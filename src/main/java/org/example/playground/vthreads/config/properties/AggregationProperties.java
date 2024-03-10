package org.example.playground.vthreads.config.properties;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Accessors(chain = true)
@Validated
@ConfigurationProperties(prefix = "org.example.playground.vthreads.services")
public class AggregationProperties {
    private ServiceConfiguration service1;
    private ServiceConfiguration service2;
    private ServiceConfiguration service3;
}
