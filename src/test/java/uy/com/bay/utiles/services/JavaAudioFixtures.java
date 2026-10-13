package uy.com.bay.utiles.services;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mp4parser.boxes.sampleentry.SampleEntry;
import org.mp4parser.muxer.AbstractTrack;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Sample;
import org.mp4parser.muxer.TrackMetaData;
import org.mp4parser.muxer.builder.DefaultMp4Builder;

/** Builds real AAC M4A fixtures entirely in Java from a small packaged silence recording. */
final class JavaAudioFixtures {

	static Path copySilence(Path target) throws IOException {
		try (var input = JavaAudioFixtures.class.getResourceAsStream("/audio/silence.m4a")) {
			Files.copy(input, target);
		}
		return target;
	}

	static Path extendedMdat(Path target) throws IOException {
		byte[] original;
		try (var input = JavaAudioFixtures.class.getResourceAsStream("/audio/silence.m4a")) {
			original = input.readAllBytes();
		}
		ByteBuffer source = ByteBuffer.wrap(original);
		int offset = 0;
		while (source.getInt(offset) != 0) {
			offset += source.getInt(offset);
		}
		// Insert the 64-bit size into the final mdat, and adjust the preceding stco table.
		ByteBuffer result = ByteBuffer.allocate(original.length + 8);
		result.put(original, 0, offset).putInt(1).putInt(0x6d646174).putLong(original.length - offset + 8);
		result.put(original, offset + 8, original.length - offset - 8);
		for (int i = 4; i < offset - 12; i++) {
			if (result.getInt(i) == 0x7374636f) {
				int count = result.getInt(i + 8);
				for (int j = 0; j < count; j++) {
					int location = i + 12 + j * 4;
					result.putInt(location, result.getInt(location) + 8);
				}
				break;
			}
		}
		Files.write(target, result.array());
		return target;
	}

	static Path longSilence(Path target) throws IOException {
		Path seed = copySilence(target.resolveSibling("seed.m4a"));
		try (var audio = Mp4AudioFile.open(seed.toFile())) {
			var seedTrack = audio.track();
			// 26610 AAC samples at 1024 ticks / 16000 Hz = exactly 1703.04 seconds.
			long[] durations = new long[26610];
			Arrays.fill(durations, 1024);
			List<Sample> samples = Collections.nCopies(durations.length, seedTrack.getSamples().get(2));
			var repeated = new AbstractTrack("silence") {
				@Override
				public List<SampleEntry> getSampleEntries() { return seedTrack.getSampleEntries(); }
				@Override
				public long[] getSampleDurations() { return durations; }
				@Override
				public TrackMetaData getTrackMetaData() { return seedTrack.getTrackMetaData(); }
				@Override
				public String getHandler() { return "soun"; }
				@Override
				public List<Sample> getSamples() { return samples; }
				@Override
				public void close() { }
			};
			Movie movie = new Movie();
			movie.addTrack(repeated);
			try (var output = FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
				new DefaultMp4Builder().build(movie).writeContainer(output);
			}
		}
		return target;
	}
}
