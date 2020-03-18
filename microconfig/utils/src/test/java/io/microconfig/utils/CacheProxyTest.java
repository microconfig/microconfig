package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.microconfig.utils.CacheProxy.cache;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheProxyTest {
    @Test
    void testCache() {
        AtomicInteger executionCount = new AtomicInteger();
        BiFunction<Integer, String, Integer> task = (i, s) -> executionCount.incrementAndGet();
        BiFunction<Integer, String, Integer> cached = cache(task);
        assertEquals(1, cached.apply(1, "2"));
        assertEquals(1, cached.apply(1, "2"));
        assertEquals(2, cached.apply(2, "2"));
        assertEquals(3, cached.apply(2, "3"));
        assertEquals(2, cached.apply(2, "2"));
        assertEquals(1, cached.apply(1, "2"));
    }
}