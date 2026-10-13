package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Mp4AudioChunksTest {

	@TempDir
	Path directory;

	@Test
	void splitsLongM4aIntoIndependentContainersWithoutLosingSamples() throws Exception {
		Path input = JavaAudioFixtures.longSilence(directory.resolve("long recording.m4a"));
		AtomicInteger sampleIndex = new AtomicInteger();
		AtomicInteger chunks = new AtomicInteger();
		AtomicReference<Double> nextOffset = new AtomicReference<>(0.0);
		try (var original = Mp4AudioFile.open(input.toFile());
				var processes = mockConstruction(ProcessBuilder.class)) {
			Mp4AudioChunks.process(input.toFile(), (bytes, offset, duration) -> {
				assertEquals(nextOffset.get(), offset, 0.000001);
				assertTrue(duration > 0 && duration <= 400);
				assertTrue(bytes.length <= Mp4AudioChunks.MAX_UPLOAD_BYTES);
				Path output = Files.write(directory.resolve("chunk" + chunks.getAndIncrement() + ".m4a"), bytes);
				try (var chunk = Mp4AudioFile.open(output.toFile())) {
					assertEquals(duration, chunk.duration(), 0.000001);
					assertEquals("mp4a", chunk.track().getSampleEntries().getFirst().getType());
					for (var sample : chunk.track().getSamples()) {
						assertEquals(original.track().getSamples().get(sampleIndex.getAndIncrement()).asByteBuffer(),
								sample.asByteBuffer(), "AAC samples must remain byte-for-byte identical and ordered");
					}
				}
				nextOffset.set(offset + duration);
			});
			assertEquals(original.track().getSamples().size(), sampleIndex.get());
			assertEquals(1703.04, nextOffset.get(), 0.000001);
			assertEquals(5, chunks.get());
			assertTrue(processes.constructed().isEmpty());
		}
	}

	@Test
	void remuxesEofSizedMdatIntoReadableM4a() throws Exception {
		Path input = JavaAudioFixtures.copySilence(directory.resolve("small.m4a"));
		AtomicInteger chunks = new AtomicInteger();
		Mp4AudioChunks.process(input.toFile(), (bytes, offset, duration) -> {
			Path output = Files.write(directory.resolve("output.m4a"), bytes);
			try (var chunk = Mp4AudioFile.open(output.toFile())) {
				assertEquals(1.064, chunk.duration(), 0.001);
			}
			chunks.incrementAndGet();
		});
		assertEquals(1, chunks.get());
	}
}
