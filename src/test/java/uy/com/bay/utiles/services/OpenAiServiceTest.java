package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.dto.AudioFile;

class OpenAiServiceTest {

	@TempDir
	Path directory;

	@Test
	void longM4aIsRemuxedInJavaAndOffsetsIncludeSilenceWithoutApiDuration() throws Exception {
		Path input = JavaAudioFixtures.longSilence(directory.resolve("long.m4a"));
		AtomicInteger uploads = new AtomicInteger();
		var service = new OpenAiService(new ObjectMapper(), new AudioDurationService()) {
			@Override
			protected String callApi(byte[] bytes, String filename) {
				assertEquals("chunk.m4a", filename);
				uploads.incrementAndGet();
				// Duration is intentionally absent; speech ends before the silent tail of each chunk.
				return "{\"text\":\"part\",\"segments\":[{\"start\":0,\"end\":1}]}";
			}
		};
		try (var processes = mockConstruction(ProcessBuilder.class)) {
			String result = service.transcribeAudioTotal(new AudioFile("long.m4a",
					new ByteArrayInputStream(Files.readAllBytes(input))));
			var response = new ObjectMapper().readTree(result);
			assertEquals(5, uploads.get());
			assertEquals(1703.04, response.path("duration").asDouble(), 0.000001);
			assertEquals(400, response.path("segments").get(1).path("start").asDouble(), 0.000001);
			assertEquals(401, response.path("segments").get(1).path("end").asDouble(), 0.000001);
			assertEquals(5, response.path("segments").size());
			assertTrue(processes.constructed().isEmpty());
		}
	}

	@Test
	void unknownDurationDoesNotUploadAudio() throws Exception {
		AudioDurationService durations = mock(AudioDurationService.class);
		when(durations.getDuration(any(File.class))).thenThrow(new IOException("No duration available"));
		OpenAiService service = spy(new OpenAiService(new ObjectMapper(), durations));
		assertThrows(IOException.class, () -> service.transcribeAudioTotal(
				new AudioFile("recording.m4a", new ByteArrayInputStream(new byte[] { 1, 2, 3 }))));
		verify(service, never()).callApi(any(), any());
	}

	@Test
	void shortAudioIsUploadedWithoutConversion() throws Exception {
		AudioDurationService durations = mock(AudioDurationService.class);
		when(durations.getDuration(any(File.class))).thenReturn(1200.0);
		OpenAiService service = spy(new OpenAiService(new ObjectMapper(), durations));
		byte[] bytes = { 1, 2, 3 };
		doReturn("transcription").when(service).callApi(bytes, "recording.m4a");
		assertEquals("transcription", service.transcribeAudioTotal(
				new AudioFile("recording.m4a", new ByteArrayInputStream(bytes))));
		verify(service).callApi(bytes, "recording.m4a");
	}
}
