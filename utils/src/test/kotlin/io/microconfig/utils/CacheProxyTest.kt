package io.microconfig.utils

import io.microconfig.utils.CacheProxy.cache
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheProxyTest {
    @Test
    fun preserveReflexiveIdentityForCachedObjects() {
        val cached = cache(Callable { "value" })
        assertTrue(cached == cached)
        assertEquals(cached.hashCode(), cached.hashCode())
    }

    @Test
    fun testCache() {
        val executionCount: AtomicInteger = AtomicInteger()
        val task: BiFunction<Int, String, Int> =
            BiFunction<Int, String, Int> { i, s -> executionCount.incrementAndGet() }
        val cached: BiFunction<Int, String, Int> = cache(task)
        assertEquals(1, cached.apply(1, "2"))
        assertEquals(1, cached.apply(1, "2"))
        assertEquals(2, cached.apply(2, "2"))
        assertEquals(3, cached.apply(2, "3"))
        assertEquals(2, cached.apply(2, "2"))
        assertEquals(1, cached.apply(1, "2"))
    }

    @Test
    fun returnNullWithoutCaching() {
        val executionCount = AtomicInteger()
        val task =
            Callable<String?> {
                executionCount.incrementAndGet()
                null
            }
        val cached = cache(task)
        assertNull(cached.call())
        assertNull(cached.call())
        assertEquals(2, executionCount.get())
    }

    @Test
    fun testThrows() {
        run {
            val r: Runnable = Runnable {
                throw IllegalStateException()
            }
            assertThrows(IllegalStateException::class.java, { cache(r).run() })
        }
        run {
            val r: Runnable = Runnable {
                throw Error()
            }
            assertThrows(Error::class.java, { cache(r).run() })
        }
        run {
            val r: Callable<*> =
                Callable<Any> {
                    throw IOException()
                }
            assertThrows(RuntimeException::class.java, { cache(r).call() })
        }
    }
}
