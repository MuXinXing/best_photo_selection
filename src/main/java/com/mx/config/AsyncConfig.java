package com.mx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(name = "classificationExecutor", destroyMethod = "shutdownNow")
    public ExecutorService classificationExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "warehouse-classify");
            thread.setDaemon(true);
            return thread;
        });
    }
}
