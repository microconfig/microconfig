package io.microconfig.utils

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

object CacheProxy {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cache(delegate: T): T =
        Proxy.newProxyInstance(
            delegate.javaClass.classLoader,
            delegate.javaClass.interfaces,
            CachingInvocationHandler(delegate),
        ) as T
}

internal class CachingInvocationHandler(private val delegate: Any) : InvocationHandler {
    private val values = ConcurrentHashMap<Key, Any?>()

    override fun invoke(proxy: Any, method: Method, arguments: Array<out Any?>?): Any? {
        val key = Key(method, arguments?.toList().orEmpty())
        return values.computeIfAbsent(key) { invokeDelegate(method, arguments) }
    }

    private fun invokeDelegate(method: Method, arguments: Array<out Any?>?): Any? =
        try {
            method.invoke(delegate, *arguments.orEmpty())
        } catch (error: InvocationTargetException) {
            val cause = error.cause ?: error
            when (cause) {
                is RuntimeException -> throw cause
                is Error -> throw cause
                else -> throw RuntimeException(cause)
            }
        }

    private data class Key(val method: Method, val arguments: List<Any?>)
}
