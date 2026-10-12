package uy.com.bay.utiles.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs media tools without a shell, with bounded execution and captured diagnostics. */
final class MediaCommand {

	private MediaCommand() {
	}

	static String run(List<String> command, Duration timeout) throws IOException {
		Path output = Files.createTempFile("media_command_", ".log");
		Process process = null;
		try {
			process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
			process.getOutputStream().close();
			if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
				throw new IOException(command.getFirst() + " timed out after " + timeout.toSeconds() + " seconds");
			}
			String result;
			try (var stream = Files.newInputStream(output)) {
				result = new String(stream.readNBytes(8192), StandardCharsets.UTF_8).trim();
			}
			if (process.exitValue() != 0) {
				throw new IOException(command.getFirst() + " exited with code " + process.exitValue() + ": " + result);
			}
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while running " + command.getFirst(), e);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
			Files.deleteIfExists(output);
		}
	}
}
