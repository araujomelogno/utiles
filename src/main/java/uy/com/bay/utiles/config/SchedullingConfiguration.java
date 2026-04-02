package uy.com.bay.utiles.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedullingConfiguration {
	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
		s.setPoolSize(5);

		s.setThreadNamePrefix("sched-");
		return s;
	}

	@Bean(name = "alchemerExecutor")
	public Executor alchemerExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//		executor.setCorePoolSize(8);
//		executor.setMaxPoolSize(14);
		executor.setCorePoolSize(6);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("alchemer-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}