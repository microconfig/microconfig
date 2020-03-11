package io.microconfig.utils;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.reflect.Proxy.newProxyInstance;

@RequiredArgsConstructor
public class CacheProxy implements InvocationHandler {
    private final ConcurrentMap<Key, Object> cache = new ConcurrentHashMap<>(256);
    private final Object delegate;

    @SuppressWarnings("unchecked")
    public static <T> T cache(T delegate) {
        return (T) newProxyInstance(
                delegate.getClass().getClassLoader(),
                delegate.getClass().getInterfaces(),
                new CacheProxy(delegate)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return cache.computeIfAbsent(new Key(method, args), k -> {
            try {
                return method.invoke(delegate, k.args);
            } catch (Exception e) {
                handleExceptionCause(e);
                throw new RuntimeException(e);
            }
        });
    }

    private void handleExceptionCause(Exception e) {
        Throwable cause = e.getCause();
        if (cause == null) return;

        if (cause instanceof RuntimeException) {
            throw ((RuntimeException) cause);
        }
        if (cause instanceof Error) {
            throw ((Error) cause);
        }
        throw new RuntimeException(cause);
    }

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static class Key {
        private final Method method;
        private final Object[] args;
    }
}