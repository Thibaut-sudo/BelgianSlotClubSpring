package org.example.belgianslotclubspring.configs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les écritures (forum, marketplace) par clé (IP) sans Redis.
 */
public final class WriteRateLimiter {

    public static final int MAX_PER_WINDOW = 8;
    public static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            key = "unknown";
        }
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < cutoff) {
                times.pollFirst();
            }
            if (times.size() >= MAX_PER_WINDOW) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
