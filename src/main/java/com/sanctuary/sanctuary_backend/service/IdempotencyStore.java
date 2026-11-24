package com.sanctuary.sanctuary_backend.service;

import com.sanctuary.sanctuary_backend.dto.PanicResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Makes a retried panic request send its texts only once. The browser sends one random
 * Idempotency-Key per button press; a repeat of that key (same user, same route) within
 * 5 minutes gets the first result back instead of texting the contacts again. A repeat
 * that arrives while the first is still sending waits for it.
 *
 * In memory, so keys are forgotten on restart (Redis replaces this in roadmap 4.6).
 * Never logged: the map holds user IDs.
 */
@Component
public class IdempotencyStore {

    static final Duration TTL = Duration.ofMinutes(5);

    private static final Pattern UUID = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private record Entry(Instant createdAt, CompletableFuture<PanicResult> result) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public IdempotencyStore() {
        this(Clock.systemUTC());
    }

    IdempotencyStore(Clock clock) {
        this.clock = clock;
    }

    public PanicResult runOnce(String route, String userId, String key, Supplier<PanicResult> send) {
        // A missing or malformed key never blocks a panic: it just isn't deduplicated
        if (key == null || !UUID.matcher(key).matches()) {
            return send.get();
        }

        Instant now = clock.instant();
        entries.values().removeIf(e -> e.createdAt().plus(TTL).isBefore(now));

        String id = route + ":" + userId + ":" + key;
        CompletableFuture<PanicResult> mine = new CompletableFuture<>();
        Entry existing = entries.putIfAbsent(id, new Entry(now, mine));
        if (existing != null) {
            return existing.result().join();
        }

        try {
            PanicResult result = send.get();
            // Nothing went out, so a retry must really try again
            if ("failed".equals(result.status())) {
                entries.remove(id);
            }
            mine.complete(result);
            return result;
        } catch (RuntimeException e) {
            entries.remove(id);
            mine.completeExceptionally(e);
            throw e;
        }
    }
}
