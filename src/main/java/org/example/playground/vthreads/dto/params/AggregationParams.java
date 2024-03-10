package org.example.playground.vthreads.dto.params;

import jakarta.validation.constraints.AssertTrue;
import lombok.Value;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Value
public class AggregationParams {
    List<String> service1;
    List<String> service2;
    List<String> service3;

    @AssertTrue
    public boolean getIsAtLeastOneParameterProvided() {
        return Stream.of(service1, service3, service2)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(StringUtils::hasText);
    }
}
