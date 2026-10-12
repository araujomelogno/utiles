package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.dto.AudioFile;

class OpenAiServiceTest {

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
