package org.example.playground.vthreads.service.throttler;

import org.example.playground.vthreads.service.client.ServiceConsumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThrottlerTest {
    private static final int THROTTLED_ELEMENTS = 5;
    private static final Duration WAIT_DURATION = Duration.ofSeconds(5);
    @Mock
    private TestConsumer testConsumer;
    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    private Throttler<String> consumerThrottler;

    @BeforeEach
    void setUp() {
        consumerThrottler = new Throttler<>(testConsumer, THROTTLED_ELEMENTS, WAIT_DURATION, executorService);
    }

    @Test
    void parallelTaskAdd() {
        IntStream.rangeClosed(1, 100)
                 .parallel()
                 .forEach(index -> consumerThrottler.submitSearchTask("" + index, (a, b) -> {
                 }));

        assertThat(consumerThrottler.getQueryQueue()).hasSize(100);
        assertThat(consumerThrottler.getQueryQueue())
                .containsExactlyInAnyOrder(
                        IntStream.rangeClosed(1, 100)
                                 .mapToObj(Integer::toString)
                                 .toArray(String[]::new)
                );
    }

    @Test
    void throttlingBarrierReached() {
        final var batchCount = 10;
        IntStream.rangeClosed(1, THROTTLED_ELEMENTS * 10)
                 .parallel()
                 .forEach(index -> consumerThrottler.submitSearchTask("" + index, (a, b) -> {
                 }));
        doReturn("abc").when(testConsumer).retrieveByQuery(anyList());

        consumerThrottler.throttlerStart();

        Awaitility.await()
                  .atMost(org.awaitility.Duration.ONE_SECOND)
                  .untilAsserted(() -> {
                      verify(testConsumer, times(batchCount)).retrieveByQuery(assertArg(params -> {
                          assertThat(params).hasSize(THROTTLED_ELEMENTS);
                      }));
                  });
    }

    @Test
    void throttlingBarrierWithTimeoutReached() {
        consumerThrottler.submitSearchTask("a", (a, b) -> {
        });
        doReturn("abc").when(testConsumer).retrieveByQuery(anyList());

        consumerThrottler.throttlerStart();

        Awaitility.await()
                  .atLeast(new org.awaitility.Duration(WAIT_DURATION.getSeconds(), TimeUnit.SECONDS))
                  .untilAsserted(() -> {
                      verify(testConsumer).retrieveByQuery(assertArg(params -> {
                          assertThat(params).containsExactlyInAnyOrder(new String[]{"a"});
                      }));
                  });
    }

    @Test
    void consumerRetriesExecuted() {
        consumerThrottler.submitSearchTask("a", (a, b) -> {
        });
        doThrow(new RuntimeException("failed"))
                .doThrow(new RuntimeException("failed"))
                .doReturn("abc")
                .when(testConsumer).retrieveByQuery(anyList());

        consumerThrottler.throttlerStart();

        Awaitility.await()
                  .atLeast(new org.awaitility.Duration(WAIT_DURATION.getSeconds(), TimeUnit.SECONDS))
                  .untilAsserted(() -> {
                      verify(testConsumer, times(3)).retrieveByQuery(assertArg(params -> {
                          assertThat(params).containsExactlyInAnyOrder(new String[]{"a"});
                      }));
                  });
    }

    @Test
    void failedResponseReturnsNull() {
        final var responseValue = new AtomicReference<String>();
        final var responseKey = new AtomicReference<String>();
        consumerThrottler.submitSearchTask("a", (key, response) -> {
            responseKey.set(key);
            responseValue.set(response);
        });
        doThrow(new RuntimeException("failed"))
                .when(testConsumer).retrieveByQuery(anyList());

        consumerThrottler.throttlerStart();

        Awaitility.await()
                  .atLeast(new org.awaitility.Duration(WAIT_DURATION.getSeconds(), TimeUnit.SECONDS))
                  .untilAsserted(() -> {
                          assertThat(responseKey.get()).isEqualTo("a");
                          assertThat(responseValue.get()).isNull();
                  });
    }

    static abstract class TestConsumer implements ServiceConsumer<String> {
    }


}