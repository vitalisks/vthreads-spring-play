package org.example.playground.vthreads.service.throttler;

import org.example.playground.vthreads.service.client.ServiceConsumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@Slf4j
public class Throttler<U> implements Runnable {
    private static final int DEFAULT_RETRY_COUNT = 2;
    private final ServiceConsumer<U> serviceConsumer;
    private final int throttleElements;
    private final Duration throttlingWaitTime;
    private final AtomicReference<CountDownLatch> sync;
    private final ReentrantLock lock = new ReentrantLock();

    @Getter(AccessLevel.PACKAGE)
    private final Set<String> queryQueue = new LinkedHashSet<>();
    private final Map<String, List<ThrottlerResponseCallback<U>>> callbacks = new HashMap<>();
    private final ExecutorService taskProcessingExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService requestExecutor;

    public Throttler(ServiceConsumer<U> serviceConsumer, int throttleElements, Duration throttlingWaitTime, ExecutorService requestExecutor) {

        this.throttlingWaitTime = throttlingWaitTime;
        this.throttleElements = throttleElements;
        this.sync = new AtomicReference<>(new CountDownLatch(throttleElements));
        log.info("Initialized with element throttling elements {}, throttling max wait {}", throttleElements, throttlingWaitTime);
        this.serviceConsumer = serviceConsumer;
        this.requestExecutor = requestExecutor;
    }

    public void submitSearchTask(String query, ThrottlerResponseCallback<U> callback) {
        inLockScope(
                () -> {
                    callbacks.computeIfAbsent(query, k -> new LinkedList<>())
                             .add(callback);
                    queryQueue.add(query);
                    sync.get().countDown();
                }
        );
    }

    @SneakyThrows
    List<String> pollBatchValues() {
        final var done = sync.get().await(throttlingWaitTime.getSeconds(), TimeUnit.SECONDS);
        return inLockScope(() -> {
            sync.updateAndGet(current -> new CountDownLatch(queryQueue.size() > throttleElements ? 0 : throttleElements));
            log.debug("Wait period done, executing {}, result = {}", queryQueue.size(), done);
            return fetchSingleBatch();
        });
    }

    private List<String> fetchSingleBatch() {
        if (queryQueue.isEmpty()) return emptyList();
        var iterator = queryQueue.iterator();
        return IntStream.rangeClosed(1, Math.min(throttleElements, queryQueue.size()))
                        .mapToObj(itx -> {
                            var itr = iterator.next();
                            iterator.remove();
                            return itr;
                        })
                        .toList();
    }

    private void handleResponse(List<String> list, U response) {
        inLockScope(() -> {
            try {
                list.stream()
                    .filter(Objects::nonNull)
                    .map(key -> Map.entry(key, callbacks.remove(key)))
                    .forEach(entry -> entry.getValue()
                                           .forEach(callback -> callback.accept(entry.getKey(), response))
                    );
            } catch (RuntimeException ex) {
                log.error("Error processing response: {}, {}", list, response);
            }
        });
    }

    @Override
    public void run() {
        Stream.generate(this::pollBatchValues)
              .filter(Predicate.not(List::isEmpty))
              .forEachOrdered(list ->
                      requestExecutor.submit(() -> {
                          try {
                              var response = simpleRetry(() -> serviceConsumer.retrieveByQuery(list), DEFAULT_RETRY_COUNT);
                              handleResponse(list, response);
                          } catch (Exception ex) {
                              log.error("Failed to execute request {}", ex.getMessage());
                              handleResponse(list, null);
                          }
                      })
              );
    }

    static <U> U simpleRetry(Supplier<U> caller, int retries) {
        return IntStream.rangeClosed(1, 1 + retries)
                        .mapToObj(attempt -> {
                            try {
                                return caller.get();
                            } catch (Exception ex) {
                                log.error("Error processing request", ex);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Unable to process request"));
    }

    private <T> T inLockScope(Supplier<T> concurrentOperation) {
        lock.lock();
        try {
            return concurrentOperation.get();
        } finally {
            lock.unlock();
        }
    }

    private void inLockScope(Runnable concurrentOperation) {
        lock.lock();
        try {
            concurrentOperation.run();
        } finally {
            lock.unlock();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void throttlerStart() {
        taskProcessingExecutor.execute(this);
    }
}
