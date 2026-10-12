package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.time.Duration;

import org.jaudiotagger.audio.AudioFileIO;
import org.junit.jupiter.api.Test;

class AudioDurationServiceTest {

	private final File audio = new File("audio with spaces.m4a");

	@Test
	void usesJaudiotaggerWhenDurationIsAvailable() throws Exception {
		try (var reader = mockStatic(AudioFileIO.class); var commands = mockStatic(MediaCommand.class)) {
			var taggedFile = mock(org.jaudiotagger.audio.AudioFile.class, RETURNS_DEEP_STUBS);
			when(taggedFile.getAudioHeader().getTrackLength()).thenReturn(42);
			reader.when(() -> AudioFileIO.read(audio)).thenReturn(taggedFile);
			assertEquals(42, new AudioDurationService("ffprobe").getDuration(audio));
			commands.verifyNoInteractions();
		}
	}

	@Test
	void fallsBackToFfprobeForM4aAndPreservesFractionalSeconds() throws Exception {
		try (var reader = mockStatic(AudioFileIO.class); var commands = mockStatic(MediaCommand.class)) {
			reader.when(() -> AudioFileIO.read(audio)).thenThrow(new IOException("invalid mdat"));
			List<String> expected = List.of("/opt/media tools/ffprobe", "-v", "error", "-show_entries",
					"format=duration", "-of", "default=noprint_wrappers=1:nokey=1", audio.getAbsolutePath());
			commands.when(() -> MediaCommand.run(expected, Duration.ofSeconds(30))).thenReturn("1703.040000");
			assertEquals(1703.04, new AudioDurationService("/opt/media tools/ffprobe").getDuration(audio), 0.000001);
			commands.verify(() -> MediaCommand.run(expected, Duration.ofSeconds(30)));
		}
	}

	@Test
	void zeroFromJaudiotaggerFallsBackToFfprobe() throws Exception {
		try (var reader = mockStatic(AudioFileIO.class); var commands = mockStatic(MediaCommand.class)) {
			var taggedFile = mock(org.jaudiotagger.audio.AudioFile.class, RETURNS_DEEP_STUBS);
			when(taggedFile.getAudioHeader().getTrackLength()).thenReturn(0);
			reader.when(() -> AudioFileIO.read(audio)).thenReturn(taggedFile);
			commands.when(() -> MediaCommand.run(any(), any())).thenReturn("1.5");
			assertEquals(1.5, new AudioDurationService("ffprobe").getDuration(audio));
		}
	}

	@Test
	void invalidProbeDurationsNeverBecomeZero() throws Exception {
		try (var reader = mockStatic(AudioFileIO.class); var commands = mockStatic(MediaCommand.class)) {
			reader.when(() -> AudioFileIO.read(audio)).thenThrow(new IOException("invalid mdat"));
			for (String output : List.of("N/A", "", "0", "-1", "NaN", "Infinity")) {
				commands.when(() -> MediaCommand.run(any(), any())).thenReturn(output);
				assertThrows(IOException.class, () -> new AudioDurationService("ffprobe").getDuration(audio), output);
			}
		}
	}

	@Test
	void missingFfprobeReportsBothFailures() throws Exception {
		try (var reader = mockStatic(AudioFileIO.class); var commands = mockStatic(MediaCommand.class)) {
			IOException readerFailure = new IOException("invalid mdat");
			reader.when(() -> AudioFileIO.read(audio)).thenThrow(readerFailure);
			commands.when(() -> MediaCommand.run(any(), any())).thenThrow(new IOException("No such file"));
			IOException error = assertThrows(IOException.class, () -> new AudioDurationService("ffprobe").getDuration(audio));
			assertTrue(error.getMessage().contains("ffprobe"));
			assertSame(readerFailure, error.getCause().getSuppressed()[0]);
		}
	}
}
