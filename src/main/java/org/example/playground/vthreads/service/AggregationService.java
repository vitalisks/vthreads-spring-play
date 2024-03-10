package org.example.playground.vthreads.service;

import org.example.playground.vthreads.dto.ResponseModel;
import org.example.playground.vthreads.dto.ResponseModel.Service2Model;
import org.example.playground.vthreads.dto.ResponseModel.Service1Model;
import org.example.playground.vthreads.dto.ResponseModel.Service3Model;
import org.example.playground.vthreads.dto.params.AggregationParams;
import org.example.playground.vthreads.service.throttler.Service2RequestThrottler;
import org.example.playground.vthreads.service.throttler.Service1RequestThrottler;
import org.example.playground.vthreads.service.throttler.Throttler;
import org.example.playground.vthreads.service.throttler.Service3RequestThrottler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;

@Slf4j
@Service
public class AggregationService {
    private final Duration serviceCallTimeout;
    private final Service1RequestThrottler service1RequestThrottler;
    private final Service2RequestThrottler service2RequestThrottler;
    private final Service3RequestThrottler service3RequestThrottler;

    public AggregationService(
            Service1RequestThrottler service1RequestThrottler,
            Service2RequestThrottler service2RequestThrottler,
            Service3RequestThrottler service3RequestThrottler,
            @Value("${org.example.playground.vthreads.callTimeout:PT20S}") Duration serviceCallTimeout
    ) {
        this.serviceCallTimeout = serviceCallTimeout;
        this.service1RequestThrottler = service1RequestThrottler;
        this.service2RequestThrottler = service2RequestThrottler;
        this.service3RequestThrottler = service3RequestThrottler;
        log.info("Initialized aggregation service with timeout {}", serviceCallTimeout);
    }

    @SneakyThrows
    public ResponseModel aggregate(AggregationParams params) {
        var paramCount = getResponseCounter(params.getService1()) +
                         getResponseCounter(params.getService3()) +
                         getResponseCounter(params.getService2());
        final var awaiter = new CountDownLatch(paramCount);
        var builder = ResponseModel.builder()
                                   .service1(getResponse(params.getService1(), service1RequestThrottler, Service1Model::new, awaiter))
                                   .service2(getResponse(params.getService2(), service2RequestThrottler, Service2Model::new, awaiter))
                                   .service3(getResponse(params.getService3(), service3RequestThrottler, Service3Model::new, awaiter));
        var awaitResult = awaiter.await(serviceCallTimeout.getSeconds(), TimeUnit.SECONDS);
        log.debug("Awaited result for response count = {}, timed out = {}", paramCount, !awaitResult);
        return builder.build();
    }

    <T, U extends Map<String, T>, R extends Throttler<U>> U getResponse(List<String> query, R throttler, Supplier<U> responseToSupplier, CountDownLatch responseCounter) {
        var responseTo = responseToSupplier.get();
        var requestKeys = Optional.ofNullable(query)
                                  .map(Set::copyOf)
                                  .orElse(emptySet());
        initResponseModel(requestKeys, responseTo);
        requestKeys.forEach(k -> {
            throttler.submitSearchTask(k, (key, response) -> {
                log.debug("Received response {} = {}", key, response);
                if (response != null) {
                    responseTo.put(key, response.get(key));
                }
                responseCounter.countDown();
            });
        });
        return responseTo.isEmpty() ? null : responseTo;
    }

    private void initResponseModel(Set<String> keys, Map<String, ?> responseModel) {
        keys.forEach(key -> responseModel.put(key, null));
    }

    int getResponseCounter(List<String> query) {
        return query != null ? Set.copyOf(query).size() : 0;
    }
}
