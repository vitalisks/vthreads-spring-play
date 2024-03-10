package org.example.playground.vthreads.service;

import org.example.playground.vthreads.config.properties.Service2Configuration;
import org.example.playground.vthreads.config.properties.Service1Configuration;
import org.example.playground.vthreads.config.properties.Service3Configuration;
import org.example.playground.vthreads.dto.ResponseModel;
import org.example.playground.vthreads.dto.params.AggregationParams;
import org.example.playground.vthreads.service.client.ServiceConsumer;
import org.example.playground.vthreads.service.throttler.Service2RequestThrottler;
import org.example.playground.vthreads.service.throttler.Service1RequestThrottler;
import org.example.playground.vthreads.service.throttler.Service3RequestThrottler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceConcurrencyTest {
    @Mock
    private ServiceConsumer<ResponseModel.Service2Model> service2Consumer;

    @Mock
    private ServiceConsumer<ResponseModel.Service3Model> service3Consumer;

    @Mock
    private ServiceConsumer<ResponseModel.Service1Model> service1Consumer;
    private final ExecutorService testExecutors = Executors.newFixedThreadPool(100);
    private final ExecutorService userExecutors = Executors.newFixedThreadPool(10);

    private Service1RequestThrottler service1RequestThrottler;
    private Service2RequestThrottler service2RequestThrottler;
    private Service3RequestThrottler service3RequestThrottler;

    private Service2Configuration defaultService2Configuration = new Service2Configuration();
    private AggregationService aggregationService;

    @BeforeEach
    void setUp() {
        service1RequestThrottler = new Service1RequestThrottler(service1Consumer, new Service1Configuration(), testExecutors);
        service2RequestThrottler = new Service2RequestThrottler(service2Consumer, defaultService2Configuration, testExecutors);
        service3RequestThrottler = new Service3RequestThrottler(service3Consumer, new Service3Configuration(), testExecutors);
        aggregationService = new AggregationService(
                service1RequestThrottler,
                service2RequestThrottler,
                service3RequestThrottler,
                Duration.ofSeconds(10)
        );
    }

    @RepeatedTest(20)
    void runCallbackModificationConcurrencyTests() {
        var elementCount = 200000;

        doAnswer(args -> {
            var service2Model = new ResponseModel.Service2Model();
            args.<List<String>>getArgument(0)
                .forEach(v -> service2Model.put(v, Double.parseDouble(v)));
            return service2Model;
        }).when(service2Consumer).retrieveByQuery(anyList());

        service2RequestThrottler.throttlerStart();

        var result = aggregationService.aggregate(new AggregationParams(
                emptyList(),
                IntStream.rangeClosed(1, elementCount).boxed().map(Object::toString).toList(),
                emptyList()
        ));

        assertThat(result.getService2()).hasSize(elementCount);
        assertThat(Set.copyOf(result.getService2().values())).isEqualTo(
                IntStream.rangeClosed(1, elementCount)
                        .mapToObj(Double::valueOf)
                        .collect(Collectors.toSet())
        );
        clearInvocations(service2Consumer);
    }
}
