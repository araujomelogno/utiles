package uy.com.bay.utiles.services;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.container.mp4.MovieCreator;

/** Owns the sample data source, which mp4parser's Track.close() does not close. */
final class Mp4AudioFile implements AutoCloseable {

	private final FileRandomAccessSourceImpl source;
	private final Path normalizedFile;
	private final Track track;

	private Mp4AudioFile(FileRandomAccessSourceImpl source, Path normalizedFile, Track track) {
		this.source = source;
		this.normalizedFile = normalizedFile;
		this.track = track;
	}

	static boolean isMp4(File file) throws IOException {
		String name = file.getName().toLowerCase(Locale.ROOT);
		if (name.endsWith(".m4a") || name.endsWith(".mp4")) {
			return true;
		}
		try (var stream = Files.newInputStream(file.toPath())) {
			byte[] header = stream.readNBytes(8);
			return header.length == 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
		}
	}

	static Mp4AudioFile open(File file) throws IOException {
		Path normalized = normalizeEndOfFileBox(file.toPath());
		Path input = normalized == null ? file.toPath() : normalized;
		FileRandomAccessSourceImpl source = null;
		try {
			source = new FileRandomAccessSourceImpl(new RandomAccessFile(input.toFile(), "r"));
			try (var channel = FileChannel.open(input, StandardOpenOption.READ)) {
				var movie = MovieCreator.build(channel, source, file.getName());
				Track audio = movie.getTracks().stream().filter(track -> "soun".equals(track.getHandler()))
						.findFirst().orElseThrow(() -> new IOException("MP4 contains no audio track"));
				if (audio.getTrackMetaData().getTimescale() <= 0 || audio.getDuration() <= 0
						|| audio.getSampleDurations().length != audio.getSamples().size()) {
					throw new IOException("MP4 audio track has invalid timing or sample tables");
				}
				return new Mp4AudioFile(source, normalized, audio);
			}
		} catch (IOException | RuntimeException e) {
			if (source != null) {
				source.close();
			}
			if (normalized != null) {
				Files.deleteIfExists(normalized);
			}
			throw new IOException("Cannot read M4A/MP4 audio: " + e.getMessage(), e);
		}
	}

	Track track() {
		return track;
	}

	double duration() {
		return (double) track.getDuration() / track.getTrackMetaData().getTimescale();
	}

	/** Size 0 is a valid box extending to EOF, but mp4parser requires an explicit size. */
	private static Path normalizeEndOfFileBox(Path input) throws IOException {
		try (var file = new RandomAccessFile(input.toFile(), "r")) {
			long length = file.length();
			long offset = 0;
			while (offset < length) {
				if (length - offset < 8) {
					throw new IOException("Truncated MP4 box header");
				}
				file.seek(offset);
				long size = Integer.toUnsignedLong(file.readInt());
				file.readInt(); // box type
				long headerSize = 8;
				if (size == 1) {
					if (length - offset < 16) {
						throw new IOException("Truncated extended MP4 box header");
					}
					size = file.readLong();
					headerSize = 16;
				} else if (size == 0) {
					size = length - offset;
					if (size > 0xffffffffL) {
						throw new IOException("EOF-sized MP4 box exceeds 4 GiB");
					}
					Path copy = Files.createTempFile("audio_mp4_", ".m4a");
					try {
						Files.copy(input, copy, StandardCopyOption.REPLACE_EXISTING);
						try (var output = FileChannel.open(copy, StandardOpenOption.WRITE)) {
							ByteBuffer header = ByteBuffer.allocate(4).putInt((int) size).flip();
							output.position(offset);
							while (header.hasRemaining()) {
								output.write(header);
							}
						}
						return copy;
					} catch (IOException e) {
						Files.deleteIfExists(copy);
						throw e;
					}
				}
				if (size < headerSize || size > length - offset) {
					throw new IOException("Invalid or truncated MP4 box at offset " + offset);
				}
				offset += size;
			}
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		try {
			source.close();
		} finally {
			if (normalizedFile != null) {
				Files.deleteIfExists(normalizedFile);
			}
		}
	}
}
