package com.amazonaws.serverless.sqseventsource;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Eventual consistency helpers.
 */
@Slf4j
final class EventualConsistency {
    private EventualConsistency() {
    }

    @SneakyThrows(InterruptedException.class)
    static <T> T waitUntil(Supplier<T> supplier, Predicate<T> predicate, int timeoutInMillis, String timeoutMessage) {
        T result;

        Instant start = Instant.now();
        Instant timeout = start.plusMillis(timeoutInMillis);
        while (Instant.now().isBefore(timeout)) {
            result = supplier.get();
            if (predicate.test(result)) {
                log.info("waitUntil completed in {} ms", Duration.between(start, Instant.now()).toMillis());
                return result;
            }
            Thread.sleep(1000);
        }

        throw new RuntimeException(timeoutMessage);
    }
}
