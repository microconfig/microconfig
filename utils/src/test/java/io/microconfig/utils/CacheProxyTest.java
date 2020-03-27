package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.microconfig.utils.CacheProxy.cache;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void testThrows() {
        {
            Runnable r = () -> {
                throw new IllegalStateException();
            };
            assertThrows(IllegalStateException.class, () -> cache(r).run());
        }
        {
            Runnable r = () -> {
                throw new Error();
            };
            assertThrows(Error.class, () -> cache(r).run());
        }
        {
            Callable<?> r = () -> {
                throw new IOException();
            };
            assertThrows(RuntimeException.class, () -> cache(r).call());
        }
    }
}