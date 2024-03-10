package org.example.playground.vthreads.service.throttler;

import org.example.playground.vthreads.config.properties.Service3Configuration;
import org.example.playground.vthreads.dto.ResponseModel.Service3Model;
import org.example.playground.vthreads.service.client.ServiceConsumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
@EnableConfigurationProperties(Service3Configuration.class)
public class Service3RequestThrottler extends Throttler<Service3Model> {
    public Service3RequestThrottler(
            ServiceConsumer<Service3Model> serviceConsumer,
            Service3Configuration configuration,
            ExecutorService baseRequestExecutorService
    ) {
        super(
                serviceConsumer,
                configuration.getThrottling().getThrottleElements(),
                configuration.getThrottling().getMaxWait(),
                baseRequestExecutorService
        );
    }
}
