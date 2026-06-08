package com.shiftleft.hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution configuration for fire-and-forget event processing.
 * <p>Enables Spring's {@code @Async} support and provides a dedicated
 * task executor for KCS drafting and other background operations.
 * Uses a ThreadPoolTaskExecutor backed by platform threads.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Dedicated executor for async event listeners.
     * Core pool of 2 threads, max 4, with a small queue for burst handling.
     *
     * @return the configured task executor
     */
    @Bean(name = "kcsTaskExecutor")
    public Executor kcsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("kcs-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
