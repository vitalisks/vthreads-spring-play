package org.example.playground.vthreads.service;

import org.example.playground.vthreads.dto.ResponseModel;
import org.example.playground.vthreads.dto.ResponseModel.Service2Model;
import org.example.playground.vthreads.dto.ResponseModel.Service1Model;
import org.example.playground.vthreads.dto.ResponseModel.Service3Model;
import org.example.playground.vthreads.dto.params.AggregationParams;
import org.example.playground.vthreads.service.throttler.Service2RequestThrottler;
import org.example.playground.vthreads.service.throttler.Service1RequestThrottler;
import org.example.playground.vthreads.service.throttler.Service3RequestThrottler;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {
    @Mock
    private Service2RequestThrottler service2RequestThrottler;
    @Mock
    private Service3RequestThrottler service3RequestThrottler;
    @Mock
    private Service1RequestThrottler service1RequestThrottler;

    @Spy
    private Duration serviceCallTimeout =
            Duration.ofSeconds(2);

    @InjectMocks
    private AggregationService aggregationService;

    @Test
    void serviceExecutionForEmptyParameters() {
        var expectedResponse = ResponseModel.builder()
                                            .build();
        assertThat(aggregationService.aggregate(new AggregationParams(null, null, null))).isEqualTo(expectedResponse);
        verify(service2RequestThrottler,
                never()).submitSearchTask(anyString(),
                any());
        verify(service3RequestThrottler,
                never()).submitSearchTask(anyString(),
                any());
        verify(service1RequestThrottler,
                never()).submitSearchTask(anyString(),
                any());


        assertThat(aggregationService.aggregate(new AggregationParams(emptyList(), emptyList(), emptyList()))).isEqualTo(expectedResponse);
        verify(service2RequestThrottler,
                never()).submitSearchTask(anyString(),
                any());
        verify(service3RequestThrottler,
                never()).submitSearchTask(anyString(),
                any());
        verify(service1RequestThrottler,
                never()).submitSearchTask(anyString(),
                any());
    }

    @Test
    void responseCallbackPopulation() {
        doAnswer(createAnswer(Service2Model::new,
                1.23))
                .when(service2RequestThrottler)
                .submitSearchTask(anyString(), any());
        doAnswer(createAnswer(Service3Model::new,
                "status"))
                .when(service3RequestThrottler)
                .submitSearchTask(anyString(), any());
        doAnswer(createAnswer(Service1Model::new,
                singletonList("box")))
                .when(service1RequestThrottler)
                .submitSearchTask(anyString(), any());

        assertThat(aggregationService.aggregate(new AggregationParams(singletonList("p"), singletonList("t"), singletonList("s"))))
                .isEqualTo(ResponseModel
                        .builder()
                        .service1(new Service1Model(Map.of("s", singletonList("box"))))
                        .service2(new Service2Model(Map.of("p"
                                , 1.23)))
                        .service3(new Service3Model(Map.of(
                                "t", "status")))
                        .build()
                );
    }

    <T, U extends Map<String, T>> Answer<?> createAnswer(Supplier<U> responseSupplier, T value) {
        return args -> {
            var query = args.<String>getArgument(0);
            var callback =
                    args.<BiConsumer<String, U>>getArgument(1);
            var response = responseSupplier.get();
            response.put(query, value);
            callback.accept(query, response);
            return null;
        };
    }

    @Test
    void nullValuesWhenResponseTimeOut() {
        var service2 = new Service2Model();
        service2.put("p1", null);
        service2.put("p2", null);

        var service3 = new Service3Model();
        service3.put("t", null);

        var service1 = new Service1Model();
        service1.put("s", null);

        assertThat(aggregationService.aggregate(new AggregationParams(List.of("p1", "p2"), singletonList("t"), singletonList("s"))))
                .isEqualTo(ResponseModel
                        .builder()
                        .service1(service1)
                        .service2(service2)
                        .service3(service3).build()
                );

        verify(service1RequestThrottler).submitSearchTask(eq("s"), any());
        verify(service3RequestThrottler).submitSearchTask(eq("t"), any());
        verify(service2RequestThrottler).submitSearchTask(eq("p1"), any());
        verify(service2RequestThrottler).submitSearchTask(eq("p2"), any());
    }

    @Test
    void partialResponsesReceivedWithTimeout() {
        var service3 = new Service3Model();
        service3.put("t1", "status");
        service3.put("t2", null);
        doAnswer(args -> {
            var query = args.<String>getArgument(0);
            var callback = args.<BiConsumer<String,
                    Service3Model>>getArgument(1);
            var response = new Service3Model();
            response.put(query, "status");
            callback.accept(query, response);
            return null;
        }).when(service3RequestThrottler)
          .submitSearchTask(eq("t1"), any());
        doNothing().when(service3RequestThrottler)
                   .submitSearchTask(eq("t2"), any());

        assertThat(aggregationService.aggregate(new AggregationParams(emptyList(), List.of("t1", "t2"), emptyList()))).isEqualTo(ResponseModel
                .builder().service3(service3).build());
    }


    @RepeatedTest(5)
    void responseAwaited() {
        try (var backgroundExecutor =
                     Executors.newSingleThreadExecutor()) {
            var service3 = new Service3Model();
            service3.put("t1", "status");
            doAnswer(answer3WithDelay(backgroundExecutor, serviceCallTimeout.minusMillis(100)))
                    .when(service3RequestThrottler)
                    .submitSearchTask(eq("t1"), any());

            assertThat(aggregationService.aggregate(new AggregationParams(emptyList(), List.of("t1"), emptyList()))).isEqualTo(ResponseModel
                    .builder().service3(service3).build());
        }
    }

    @RepeatedTest(5)
    void responseAwaitTimedOut() {
        try (var backgroundExecutor =
                     Executors.newSingleThreadExecutor()) {
            var service3 = new Service3Model();
            service3.put("t1", null);
            doAnswer(answer3WithDelay(backgroundExecutor, serviceCallTimeout.plusMillis(100)))
                    .when(service3RequestThrottler)
                    .submitSearchTask(eq("t1"), any());

            assertThat(aggregationService.aggregate(new AggregationParams(emptyList(), List.of("t1"), emptyList()))).isEqualTo(ResponseModel
                    .builder().service3(service3).build());
        }
    }

    Answer<Object> answer3WithDelay(ExecutorService executorService, Duration answerDelay) {
        return args -> {
            var query = args.<String>getArgument(0);
            var callback = args.<BiConsumer<String,
                    Service3Model>>getArgument(1);
            executorService.submit(() -> {
                var response = new Service3Model();
                response.put(query, "status");
                try {
                    Thread.sleep(answerDelay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                callback.accept(query, response);
            });
            return null;
        };
    }

}