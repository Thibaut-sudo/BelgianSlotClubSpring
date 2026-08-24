package org.example.belgianslotclubspring.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteRateLimiterTest {

    @Test
    void allowsBurstThenRejects() {
        WriteRateLimiter limiter = new WriteRateLimiter();
        for (int i = 0; i < WriteRateLimiter.MAX_PER_WINDOW; i++) {
            assertTrue(limiter.tryAcquire("1.2.3.4"));
        }
        assertFalse(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("9.9.9.9"));
    }
}
