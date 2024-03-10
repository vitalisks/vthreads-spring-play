package org.example.playground.vthreads.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class BaseConfiguration {
    @Bean
    ExecutorService baseRequestExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
