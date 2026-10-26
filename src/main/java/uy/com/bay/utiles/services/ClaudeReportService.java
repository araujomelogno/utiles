package uy.com.bay.utiles.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.MessageCreateParams;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;

/**
 * Genera el informe de supervisión de una {@link SupervisionTask} invocando la
 * API de Claude (Anthropic). El insumo es el output de la tarea (transcripción
 * diarizada) junto con su evaluación y puntajes; la tarea debe estar en estado
 * {@link Status#DONE}. El informe se devuelve en Markdown.
 */
@Service
public class ClaudeReportService {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeReportService.class);

	private static final String PROMPT_PATH = "/prompts/informeSupervision.txt";

	/** Beta de fallback del lado del servidor ante rechazos por políticas de seguridad. */
	private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

	private final SupervisionTaskRepository supervisionTaskRepository;
	private final String apiKey;
	private final String model;
	private final String basePrompt;

	private volatile AnthropicClient client;

	public ClaudeReportService(SupervisionTaskRepository supervisionTaskRepository,
			@Value("${anthropic.api-key:}") String apiKey, @Value("${anthropic.model:claude-opus-5-5}") String model) {
		this.supervisionTaskRepository = supervisionTaskRepository;
		this.apiKey = apiKey;
		this.model = model;

		String loadedPrompt = "";
		try (InputStream inputStream = getClass().getResourceAsStream(PROMPT_PATH)) {
			if (inputStream != null) {
				loadedPrompt = new String(FileCopyUtils.copyToByteArray(inputStream), StandardCharsets.UTF_8);
			} else {
				logger.error("Prompt file not found: {}", PROMPT_PATH);
			}
		} catch (IOException e) {
			logger.error("Error loading prompt {}", PROMPT_PATH, e);
		}
		this.basePrompt = loadedPrompt;
	}

	/**
	 * Genera el informe de la tarea indicada.
	 *
	 * @throws IllegalStateException si la tarea no existe, no está en DONE, no tiene
	 *                               output, falta la API key o Claude no devuelve
	 *                               un informe utilizable
	 */
	@Transactional(readOnly = true)
	public String generateReport(Long taskId) {
		SupervisionTask task = supervisionTaskRepository.findById(taskId)
				.orElseThrow(() -> new IllegalStateException("No existe la tarea de supervisión " + taskId));
		if (task.getStatus() != Status.DONE) {
			throw new IllegalStateException("La tarea de supervisión debe estar en estado DONE para generar el informe.");
		}
		if (task.getOutput() == null || task.getOutput().isBlank()) {
			throw new IllegalStateException("La tarea de supervisión no tiene output.");
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("No está configurada la propiedad anthropic.api-key en application.properties.");
		}

		String prompt = buildPrompt(task);

		// Claude Opus 5.5 siempre piensa (thinking adaptativo); la profundidad se
		// controla con effort. "fallbacks: default" reintenta en otro modelo si la
		// solicitud es rechazada por los clasificadores de seguridad.
		MessageCreateParams params = MessageCreateParams.builder().model(model).maxTokens(16000L)
				.outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.HIGH).build())
				.addBeta(FALLBACK_BETA).putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
				.addUserMessage(prompt).build();

		BetaMessage response;
		try {
			response = getClient().beta().messages().create(params);
		} catch (AnthropicException e) {
			logger.error("Error llamando a la API de Claude para la tarea {}", taskId, e);
			throw new IllegalStateException("Error al comunicarse con la API de Claude: " + e.getMessage(), e);
		}

		BetaStopReason stopReason = response.stopReason().orElse(null);
		if (BetaStopReason.REFUSAL.equals(stopReason)) {
			throw new IllegalStateException("Claude rechazó generar el informe para esta tarea.");
		}

		String report = response.content().stream().map(BetaContentBlock::text).flatMap(Optional::stream)
				.map(BetaTextBlock::text).collect(Collectors.joining("\n")).trim();
		if (report.isEmpty()) {
			throw new IllegalStateException("Claude devolvió un informe vacío.");
		}
		if (BetaStopReason.MAX_TOKENS.equals(stopReason)) {
			logger.warn("El informe de la tarea {} se truncó por max_tokens", taskId);
			report += "\n\n_(Informe truncado: se alcanzó el límite de longitud de la respuesta.)_";
		}
		return report;
	}

	private String buildPrompt(SupervisionTask task) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		return basePrompt.replace("{{ESTUDIO}}", text(task.getAlchemerStudyName()))
				.replace("{{ARCHIVO}}", text(task.getFileName())).replace("{{ENCUESTADOR}}", text(task.getSurveyor()))
				.replace("{{TIPO}}", task.getType() != null ? task.getType().name() : "sin información")
				.replace("{{FECHA_AUDIO}}",
						task.getAudioDate() != null ? dateFormat.format(task.getAudioDate()) : "sin información")
				.replace("{{DURACION}}", String.valueOf(Math.round(task.getTotalAudioDuration())))
				.replace("{{PUNTAJE_GLOBAL}}", String.valueOf(Math.round(task.getAiScore())))
				.replace("{{COBERTURA}}", String.valueOf(Math.round(task.getScoreCobertura())))
				.replace("{{FIDELIDAD}}", String.valueOf(task.getScoreFidelidad()))
				.replace("{{NEUTRALIDAD}}", String.valueOf(task.getScoreNeutralidad()))
				.replace("{{FLUIDEZ}}", String.valueOf(task.getScoreFluidez()))
				.replace("{{ITEMS_ESPERADOS}}", text(task.getItemsEsperados()))
				.replace("{{ITEMS_ENCONTRADOS}}", text(task.getItemsEncontrados()))
				.replace("{{ITEMS_FALTANTES}}", text(task.getItemsFaltantes()))
				.replace("{{EVALUACION}}", task.getEvaluationOutput() != null ? task.getEvaluationOutput() : "")
				// El output va al final: es el texto más largo y puede contener cualquier secuencia.
				.replace("{{OUTPUT}}", task.getOutput());
	}

	private static String text(Object value) {
		return value != null && !value.toString().isBlank() ? value.toString() : "sin información";
	}

	private AnthropicClient getClient() {
		AnthropicClient result = client;
		if (result == null) {
			synchronized (this) {
				result = client;
				if (result == null) {
					result = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
					client = result;
				}
			}
		}
		return result;
	}
}
