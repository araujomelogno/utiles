package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jaudiotagger.audio.AudioFileIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudioDurationServiceTest {

	@TempDir
	Path directory;

	@Test
	void readsM4aWithEofSizedMdatWithoutJaudiotaggerOrNativeProcesses() throws Exception {
		Path audio = JavaAudioFixtures.copySilence(directory.resolve("recording with spaces.m4a"));
		byte[] original = Files.readAllBytes(audio);
		// This is the real failure mode of jaudiotagger 2.0.1, not a mocked exception.
		assertThrows(Exception.class, () -> AudioFileIO.read(audio.toFile()));
		try (var reader = mockStatic(AudioFileIO.class);
				var processes = mockConstruction(ProcessBuilder.class)) {
			assertEquals(1.064, new AudioDurationService().getDuration(audio.toFile()), 0.001);
			reader.verifyNoInteractions();
			assertTrue(processes.constructed().isEmpty());
		}
		assertArrayEquals(original, Files.readAllBytes(audio), "The original audio must remain unchanged");
	}

	@Test
	void detectsMp4ContentEvenWithoutAnM4aExtension() throws Exception {
		Path audio = JavaAudioFixtures.copySilence(directory.resolve("recording.tmp"));
		assertEquals(1.064, new AudioDurationService().getDuration(audio.toFile()), 0.001);
	}

	@Test
	void supportsExtended64BitMdatHeader() throws Exception {
		Path audio = JavaAudioFixtures.extendedMdat(directory.resolve("extended.m4a"));
		assertThrows(Exception.class, () -> AudioFileIO.read(audio.toFile()));
		assertEquals(1.064, new AudioDurationService().getDuration(audio.toFile()), 0.001);
	}

	@Test
	void keepsJaudiotaggerForOtherAudioFormats() throws Exception {
		File audio = Files.write(directory.resolve("audio.mp3"), new byte[] { 1, 2, 3 }).toFile();
		try (var reader = mockStatic(AudioFileIO.class)) {
			var taggedFile = mock(org.jaudiotagger.audio.AudioFile.class, RETURNS_DEEP_STUBS);
			when(taggedFile.getAudioHeader().getTrackLength()).thenReturn(42);
			reader.when(() -> AudioFileIO.read(audio)).thenReturn(taggedFile);
			assertEquals(42, new AudioDurationService().getDuration(audio));
		}
	}

	@Test
	void rejectsTruncatedM4aInsteadOfReturningZero() throws Exception {
		Path audio = JavaAudioFixtures.extendedMdat(directory.resolve("truncated.m4a"));
		byte[] bytes = Files.readAllBytes(audio);
		Files.write(audio, java.util.Arrays.copyOf(bytes, bytes.length - 10));
		assertThrows(IOException.class, () -> new AudioDurationService().getDuration(audio.toFile()));
	}

	@Test
	void zeroFromOtherAudioFormatsIsAnError() throws Exception {
		File audio = Files.write(directory.resolve("audio.mp3"), new byte[] { 1, 2, 3 }).toFile();
		try (var reader = mockStatic(AudioFileIO.class)) {
			var taggedFile = mock(org.jaudiotagger.audio.AudioFile.class, RETURNS_DEEP_STUBS);
			when(taggedFile.getAudioHeader().getTrackLength()).thenReturn(0);
			reader.when(() -> AudioFileIO.read(audio)).thenReturn(taggedFile);
			assertThrows(IOException.class, () -> new AudioDurationService().getDuration(audio));
		}
	}
}
