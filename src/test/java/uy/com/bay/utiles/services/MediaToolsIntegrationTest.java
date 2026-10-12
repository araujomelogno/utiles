package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import javax.sound.sampled.AudioSystem;

import org.jaudiotagger.audio.AudioFileIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.dto.AudioFile;

/** Opt in with -Daudio.media.integration=true on a machine with FFmpeg and ffprobe. */
@EnabledIfSystemProperty(named = "audio.media.integration", matches = "true")
class MediaToolsIntegrationTest {

	@TempDir
	Path directory;

	@Test
	void longM4aUsesRealFfprobeAndUploadsValidWavChunks() throws Exception {
		Path input = directory.resolve("recording with spaces.m4a");
		MediaCommand.run(List.of("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i",
				"anullsrc=r=16000:cl=mono", "-t", "1703.04", "-c:a", "aac", input.toString()),
				Duration.ofMinutes(2));
		AudioDurationService durations = new AudioDurationService("ffprobe");
		class RecordingService extends OpenAiService {
			int uploads;
			RecordingService() {
				super(new ObjectMapper(), durations);
			}
			@Override
			protected String callApi(byte[] bytes, String filename) {
				assertTrue(filename.endsWith(".wav"));
				assertEquals("RIFF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
				try (var audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) {
					double duration = audio.getFrameLength() / audio.getFormat().getFrameRate();
					assertTrue(duration > 0 && duration < 1200);
					uploads++;
					return "{\"duration\":" + duration + ",\"text\":\"part\",\"segments\":[{\"start\":0,\"end\":"
							+ duration + "}]}";
				} catch (Exception e) {
					throw new AssertionError("Uploaded chunk is not a readable WAV", e);
				}
			}
		}
		// Reproduce the old reader's failure while exercising real native tools.
		try (var reader = mockStatic(AudioFileIO.class)) {
			reader.when(() -> AudioFileIO.read(any(File.class))).thenThrow(new IOException("invalid mdat"));
			assertEquals(1703.04, durations.getDuration(input.toFile()), 0.1);
			RecordingService service = new RecordingService();
			String result = service.transcribeAudioTotal(new AudioFile(input.getFileName().toString(),
					new ByteArrayInputStream(Files.readAllBytes(input))));
			var response = new ObjectMapper().readTree(result);
			assertTrue(service.uploads > 1);
			assertEquals(service.uploads, response.path("segments").size());
			assertEquals(1703.04, response.path("duration").asDouble(), 0.2);
			assertTrue(response.path("segments").get(1).path("start").asDouble() > 0);
		}
	}
}
