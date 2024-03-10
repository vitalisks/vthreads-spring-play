package org.example.playground.vthreads.service.throttler;

import org.example.playground.vthreads.config.properties.Service1Configuration;
import org.example.playground.vthreads.service.client.ServiceConsumer;
import org.example.playground.vthreads.dto.ResponseModel.Service1Model;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
public class Service1RequestThrottler extends Throttler<Service1Model> {
    public Service1RequestThrottler(
            ServiceConsumer<Service1Model> serviceConsumer,
            Service1Configuration configuration,
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
