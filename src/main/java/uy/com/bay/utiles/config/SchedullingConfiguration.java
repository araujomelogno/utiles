package uy.com.bay.utiles.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedullingConfiguration {
	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
		s.setPoolSize(2);
		
		s.setThreadNamePrefix("sched-");
		return s;
	}
}