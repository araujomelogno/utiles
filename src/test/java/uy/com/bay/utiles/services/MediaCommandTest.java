package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs({ OS.LINUX, OS.MAC })
class MediaCommandTest {

	@TempDir
	Path directory;

	private List<String> script(String content) throws IOException {
		Path script = directory.resolve("media tool.sh");
		Files.writeString(script, content);
		return List.of("/bin/sh", script.toString());
	}

	@Test
	void capturesOutput() throws Exception {
		assertEquals("1703.040000", MediaCommand.run(script("printf '1703.040000\\n'"), Duration.ofSeconds(5)));
	}

	@Test
	void rejectsNonzeroExitEvenWithDurationOutput() throws Exception {
		IOException error = assertThrows(IOException.class,
				() -> MediaCommand.run(script("printf '1703.04\\n'; exit 3"), Duration.ofSeconds(5)));
		assertTrue(error.getMessage().contains("code 3"));
	}

	@Test
	void stopsHungProcess() throws Exception {
		Path pidFile = directory.resolve("pid");
		List<String> command = script("echo $$ > '" + pidFile + "'\nexec sleep 60");
		assertThrows(IOException.class, () -> MediaCommand.run(command, Duration.ofMillis(200)));
		long pid = Long.parseLong(Files.readString(pidFile).trim());
		ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
		if (handle != null) {
			handle.onExit().get(5, java.util.concurrent.TimeUnit.SECONDS);
			assertFalse(handle.isAlive());
		}
	}

	@Test
	void preservesInterruption() throws Exception {
		List<String> command = script("exec sleep 60");
		Thread.currentThread().interrupt();
		try {
			assertThrows(IOException.class, () -> MediaCommand.run(command, Duration.ofSeconds(5)));
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}
}
