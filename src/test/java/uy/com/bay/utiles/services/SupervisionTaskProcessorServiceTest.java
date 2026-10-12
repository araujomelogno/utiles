package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;

class SupervisionTaskProcessorServiceTest {

	private SupervisionTask task() {
		SupervisionTask task = new SupervisionTask();
		task.setId(1399L);
		task.setFileName("recording.m4a");
		task.setAudioContent(new byte[] { 1, 2, 3 });
		return task;
	}

	@Test
	void unknownDurationMarksTaskAsErrorWithoutTranscribing() throws Exception {
		SupervisionTaskRepository repository = mock(SupervisionTaskRepository.class);
		SupervisionTask task = task();
		when(repository.findById(task.getId())).thenReturn(Optional.of(task));
		AudioDurationService durations = mock(AudioDurationService.class);
		when(durations.getDuration(any(File.class))).thenThrow(new IOException("No duration available"));
		OpenAiService openAi = mock(OpenAiService.class);
		new SupervisionTaskProcessorService(repository, openAi, mock(ChatClient.Builder.class), durations)
				.processSingleTask(task.getId());
		assertEquals(Status.ERROR, task.getStatus());
		verifyNoInteractions(openAi);
		verify(repository, times(2)).save(task);
	}

	@Test
	void savesFractionalDurationFromSharedReader() throws Exception {
		SupervisionTaskRepository repository = mock(SupervisionTaskRepository.class);
		SupervisionTask task = task();
		when(repository.findById(task.getId())).thenReturn(Optional.of(task));
		AudioDurationService durations = mock(AudioDurationService.class);
		when(durations.getDuration(any(File.class))).thenReturn(1703.04);
		OpenAiService openAi = mock(OpenAiService.class);
		when(openAi.transcribeAudioTotal(any())).thenReturn("{\"segments\":[]}");
		ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_DEEP_STUBS);
		when(builder.build().prompt().user(anyString()).call().content()).thenReturn("{}");
		new SupervisionTaskProcessorService(repository, openAi, builder, durations).processSingleTask(task.getId());
		assertEquals(1703.04, task.getTotalAudioDuration());
		assertEquals(Status.DONE, task.getStatus());
	}
}
