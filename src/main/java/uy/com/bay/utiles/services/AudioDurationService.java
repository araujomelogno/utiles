package uy.com.bay.utiles.services;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFileIO;
import org.springframework.stereotype.Service;

@Service
public class AudioDurationService {

	public double getDuration(File file) throws IOException {
		try {
			double duration;
			if (Mp4AudioFile.isMp4(file)) {
				try (var audio = Mp4AudioFile.open(file)) {
					duration = audio.duration();
				}
			} else {
				duration = AudioFileIO.read(file).getAudioHeader().getTrackLength();
			}
			if (!Double.isFinite(duration) || duration <= 0) {
				throw new IOException("Invalid audio duration: " + duration);
			}
			return duration;
		} catch (Exception e) {
			throw new IOException("Unable to determine audio duration. The audio must be a readable, supported file.", e);
		}
	}
}
