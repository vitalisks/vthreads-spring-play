package org.example.playground.vthreads.service.throttler;

import java.util.function.BiConsumer;

public interface ThrottlerResponseCallback<U> extends BiConsumer<String, U> {
    default void runCallback(String key, U value) {
        accept(key, value);
    }
}
