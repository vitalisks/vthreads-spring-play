package org.example.playground.vthreads.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

@Data
@Accessors(chain = true)
public class ThrottlingConfiguration {
    @NotNull
    private Duration maxWait = Duration.ofSeconds(5);

    @Min(1)
    private int throttleElements = 5;
}
