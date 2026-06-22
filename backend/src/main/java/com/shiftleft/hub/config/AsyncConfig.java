package com.shiftleft.hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * Provides a virtual-thread-backed executor for AI chat requests.
     *
     * @return the chat executor service
     */
    @Bean("chatExecutor")
    public ExecutorService chatExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Dedicated executor for KCS event listeners.
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

    /**
     * Dedicated executor for the document ETL pipeline
     * (DocumentEventListener). P-10: previously shared the
     * kcsTaskExecutor, so a 4-document upload burst could starve
     * KCS drafting (and vice-versa). The ETL holds a thread for
     * parse + chunk + embed + save, so it gets its own pool sized
     * for that workload.
     *
     * @return the configured task executor
     */
    @Bean(name = "documentEtlExecutor")
    public Executor documentEtlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("doc-etl-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
