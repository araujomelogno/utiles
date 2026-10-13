package uy.com.bay.utiles.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebappAudioClassLoaderTest {

	@TempDir
	Path directory;

	@Test
	void readsAndSplitsWhenMp4parserExistsOnlyInTheWebappLoader() throws Exception {
		Path classpath = directory.resolve("webapp-classpath.txt");
		Files.write(classpath, Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
		String bootstrap = Path.of(WebappAudioProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
		Path output = directory.resolve("probe.log");
		Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-cp", bootstrap, WebappAudioProbe.class.getName(), classpath.toString(), directory.toString())
				.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Webapp classloader probe timed out");
			String log = Files.readString(output);
			assertEquals(0, process.exitValue(), log);
			assertTrue(log.contains("WEBAPP_AUDIO_OK duration=1703.04 chunks=5"), log);
		} finally {
			if (process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}
}
