package uy.com.bay.utiles.services;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AudioDurationService {

	private static final Logger logger = LoggerFactory.getLogger(AudioDurationService.class);
	private final String ffprobeExecutable;

	public AudioDurationService(@Value("${audio.ffprobe.path:ffprobe}") String ffprobeExecutable) {
		this.ffprobeExecutable = ffprobeExecutable;
	}

	public double getDuration(File file) throws IOException {
		Exception readerFailure;
		try {
			double duration = AudioFileIO.read(file).getAudioHeader().getTrackLength();
			if (Double.isFinite(duration) && duration > 0) {
				return duration;
			}
			readerFailure = new IOException("jaudiotagger returned an invalid audio duration: " + duration);
		} catch (Exception e) {
			readerFailure = e;
		}
		logger.debug("Could not read audio duration with jaudiotagger; trying ffprobe: {}", readerFailure.getMessage());
		try {
			String output = MediaCommand.run(List.of(ffprobeExecutable, "-v", "error", "-show_entries",
					"format=duration", "-of", "default=noprint_wrappers=1:nokey=1", file.getAbsolutePath()),
					Duration.ofSeconds(30));
			double duration;
			try {
				duration = Double.parseDouble(output);
			} catch (NumberFormatException e) {
				throw new IOException("ffprobe did not return a numeric audio duration: " + output, e);
			}
			if (!Double.isFinite(duration) || duration <= 0) {
				throw new IOException("ffprobe returned an invalid audio duration: " + output);
			}
			return duration;
		} catch (IOException e) {
			e.addSuppressed(readerFailure);
			throw new IOException("Unable to determine audio duration. Ensure ffprobe is installed and the audio is readable.", e);
		}
	}
}
