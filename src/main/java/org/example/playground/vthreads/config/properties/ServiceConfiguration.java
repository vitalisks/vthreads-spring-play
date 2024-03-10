package org.example.playground.vthreads.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

@Data
@Accessors(chain = true)
public class ServiceConfiguration {
    @NotEmpty
    private String baseServiceEndpoint;
    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration requestTimeout = Duration.ofSeconds(5);
    @NotNull
    private ThrottlingConfiguration throttling = new ThrottlingConfiguration();
}
