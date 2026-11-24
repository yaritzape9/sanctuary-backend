package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.dto.PanicResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyStoreTest {

    private static final String KEY = "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";

    private final AtomicInteger sends = new AtomicInteger();

    private Supplier<PanicResult> sending(PanicResult result) {
        return () -> {
            sends.incrementAndGet();
            return result;
        };
    }

    /** A clock the test can move forward. */
    private static class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-23T12:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    @Test
    void sameKey_sendsOnce_andReplaysTheFirstResult() {
        IdempotencyStore store = new IdempotencyStore();

        PanicResult first = store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(1, 1)));
        PanicResult retry = store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(2, 0)));

        assertThat(sends.get()).isEqualTo(1);
        assertThat(retry).isEqualTo(first);
    }

    @Test
    void sameKey_fromAnotherUserOrRoute_sendsSeparately() {
        IdempotencyStore store = new IdempotencyStore();

        store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(1, 0)));
        store.runOnce("trigger", "user-2", KEY, sending(new PanicResult(1, 0)));
        store.runOnce("safe", "user-1", KEY, sending(new PanicResult(1, 0)));

        assertThat(sends.get()).isEqualTo(3);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uuid", "3f2a9c1e5b7d4e8a9c217d4e5f6a8b90", KEY + "x"})
    void missingOrMalformedKey_neverBlocks_andIsNotDeduplicated(String key) {
        IdempotencyStore store = new IdempotencyStore();

        store.runOnce("trigger", "user-1", key, sending(new PanicResult(1, 0)));
        store.runOnce("trigger", "user-1", key, sending(new PanicResult(1, 0)));

        assertThat(sends.get()).isEqualTo(2);
    }

    @Test
    void failedResult_isNotRemembered_soTheRetryTriesAgain() {
        IdempotencyStore store = new IdempotencyStore();

        store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(0, 2)));
        PanicResult retry = store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(2, 0)));

        assertThat(sends.get()).isEqualTo(2);
        assertThat(retry.status()).isEqualTo("sent");
    }

    @Test
    void exception_isNotRemembered_soTheRetryTriesAgain() {
        IdempotencyStore store = new IdempotencyStore();

        assertThatThrownBy(() -> store.runOnce("trigger", "user-1", KEY, () -> {
            throw new IllegalStateException("database down");
        })).isInstanceOf(IllegalStateException.class);
        store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(1, 0)));

        assertThat(sends.get()).isEqualTo(1);
    }

    @Test
    void key_expiresAfterFiveMinutes() {
        MovableClock clock = new MovableClock();
        IdempotencyStore store = new IdempotencyStore(clock);

        store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(1, 0)));
        clock.advance(IdempotencyStore.TTL.minusSeconds(1));
        store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(1, 0)));
        assertThat(sends.get()).isEqualTo(1);

        clock.advance(Duration.ofSeconds(2));
        store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(1, 0)));
        assertThat(sends.get()).isEqualTo(2);
    }

    @Test
    void repeatWhileFirstIsStillSending_waitsForTheSameResult() throws Exception {
        IdempotencyStore store = new IdempotencyStore();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch letFirstFinish = new CountDownLatch(1);

        CompletableFuture<PanicResult> first = CompletableFuture.supplyAsync(() ->
            store.runOnce("trigger", "user-1", KEY, () -> {
                sends.incrementAndGet();
                firstStarted.countDown();
                await(letFirstFinish);
                return new PanicResult(3, 0);
            }));
        assertThat(firstStarted.await(5, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<PanicResult> retry = CompletableFuture.supplyAsync(() ->
            store.runOnce("trigger", "user-1", KEY, sending(new PanicResult(9, 9))));
        letFirstFinish.countDown();

        assertThat(retry.get(5, TimeUnit.SECONDS)).isEqualTo(new PanicResult(3, 0));
        assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(new PanicResult(3, 0));
        assertThat(sends.get()).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
