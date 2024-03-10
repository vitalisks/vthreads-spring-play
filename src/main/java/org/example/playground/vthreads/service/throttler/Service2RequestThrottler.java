package org.example.playground.vthreads.service.throttler;

import org.example.playground.vthreads.config.properties.Service2Configuration;
import org.example.playground.vthreads.service.client.ServiceConsumer;
import org.example.playground.vthreads.dto.ResponseModel.Service2Model;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
@EnableConfigurationProperties(Service2Configuration.class)
public class Service2RequestThrottler extends Throttler<Service2Model> {
    public Service2RequestThrottler(
            ServiceConsumer<Service2Model> serviceConsumer,
            Service2Configuration configuration,
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
