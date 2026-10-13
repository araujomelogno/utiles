package uy.com.bay.utiles.services;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** JDK-only entry point: dependencies live in the webapp loader, never the system loader. */
public final class WebappAudioProbe {

	public static void main(String[] args) throws Exception {
		if (ClassLoader.getSystemResource("isoparser2-default.properties") != null) {
			throw new AssertionError("Regression requires mp4parser resources to be invisible to the system loader");
		}
		var urls = new ArrayList<java.net.URL>();
		for (String path : Files.readAllLines(Path.of(args[0]))) {
			urls.add(Path.of(path).toUri().toURL());
		}
		try (var webapp = new URLClassLoader(urls.toArray(java.net.URL[]::new), ClassLoader.getPlatformClassLoader())) {
			Thread.currentThread().setContextClassLoader(webapp);
			// Verify that the old library default actually reproduces the production failure.
			var parser = webapp.loadClass("org.mp4parser.PropertyBoxParserImpl");
			try {
				parser.getConstructor(String[].class).newInstance((Object) new String[0]);
				throw new AssertionError("Expected the legacy system-loader resource lookup to fail");
			} catch (InvocationTargetException e) {
				if (!(e.getCause() instanceof NullPointerException)
						|| !e.getCause().getMessage().contains("inStream")) {
					throw e;
				}
			}
			Path directory = Path.of(args[1]);
			var fixture = webapp.loadClass("uy.com.bay.utiles.services.JavaAudioFixtures")
					.getDeclaredMethod("longSilence", Path.class);
			fixture.setAccessible(true);
			Path input = (Path) fixture.invoke(null, directory.resolve("long.m4a"));
			var durationClass = webapp.loadClass("uy.com.bay.utiles.services.AudioDurationService");
			var durationService = durationClass.getConstructor().newInstance();
			var readDuration = durationClass.getMethod("getDuration", File.class);
			double total = (double) readDuration.invoke(durationService, input.toFile());
			if (Math.abs(total - 1703.04) > 0.000001) {
				throw new AssertionError("Incorrect audio duration: " + total);
			}
			AtomicInteger count = new AtomicInteger();
			var consumerClass = webapp.loadClass("uy.com.bay.utiles.services.Mp4AudioChunks$Consumer");
			Object consumer = Proxy.newProxyInstance(webapp, new Class<?>[] { consumerClass }, (proxy, method, values) -> {
				Path chunk = Files.write(directory.resolve("chunk" + count.incrementAndGet() + ".m4a"), (byte[]) values[0]);
				double duration = (double) readDuration.invoke(durationService, chunk.toFile());
				if (Math.abs(duration - (double) values[2]) > 0.000001) {
					throw new AssertionError("Invalid chunk duration");
				}
				return null;
			});
			var process = webapp.loadClass("uy.com.bay.utiles.services.Mp4AudioChunks")
					.getDeclaredMethod("process", File.class, consumerClass);
			process.setAccessible(true);
			process.invoke(null, input.toFile(), consumer);
			if (count.get() != 5) {
				throw new AssertionError("Expected five chunks; got " + count.get());
			}
			System.out.println("WEBAPP_AUDIO_OK duration=" + total + " chunks=" + count.get());
		}
	}
}
