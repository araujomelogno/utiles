package uy.com.bay.utiles.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;

import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.ClippedTrack;

/** Remuxes complete audio samples into standalone M4A files without decoding. */
final class Mp4AudioChunks {

	static final int MAX_UPLOAD_BYTES = 6 * 1024 * 1024;
	private static final int MAX_SAMPLE_BYTES = 5 * 1024 * 1024;
	private static final double MAX_DURATION_SECONDS = 400;

	@FunctionalInterface
	interface Consumer {
		void accept(byte[] bytes, double offset, double duration) throws Exception;
	}

	private Mp4AudioChunks() {
	}

	static void process(File input, Consumer consumer) throws Exception {
		try (var audio = Mp4AudioFile.open(input)) {
			Track track = audio.track();
			long timescale = track.getTrackMetaData().getTimescale();
			long[] durations = track.getSampleDurations();
			long offsetTicks = 0;
			int start = 0;
			while (start < durations.length) {
				int end = start;
				long ticks = 0;
				long size = 0;
				while (end < durations.length) {
					long sampleSize = track.getSamples().get(end).getSize();
					long sampleTicks = durations[end];
					if (sampleTicks <= 0 || sampleSize <= 0) {
						throw new IOException("MP4 contains an audio sample with invalid size or duration");
					}
					if (size + sampleSize > MAX_SAMPLE_BYTES
							|| (double) (ticks + sampleTicks) / timescale > MAX_DURATION_SECONDS) {
						break;
					}
					ticks += sampleTicks;
					size += sampleSize;
					end++;
				}
				if (end == start) {
					throw new IOException("A single MP4 audio sample exceeds the upload limits");
				}
				byte[] bytes = remux(track, start, end);
				while (bytes.length > MAX_UPLOAD_BYTES && end - start > 1) {
					end = start + (end - start) / 2;
					bytes = remux(track, start, end);
				}
				if (bytes.length > MAX_UPLOAD_BYTES) {
					throw new IOException("MP4 audio chunk exceeds the upload size limit");
				}
				ticks = 0;
				for (int i = start; i < end; i++) {
					ticks += durations[i];
				}
				consumer.accept(bytes, (double) offsetTicks / timescale, (double) ticks / timescale);
				offsetTicks += ticks;
				start = end;
			}
		}
	}

	private static byte[] remux(Track track, int start, int end) throws IOException {
		Movie movie = new Movie();
		movie.addTrack(new ClippedTrack(track, start, end));
		var container = new DefaultMp4Builder().build(movie);
		try (var output = new ByteArrayOutputStream(); var channel = Channels.newChannel(output)) {
			container.writeContainer(channel);
			return output.toByteArray();
		}
	}
}
