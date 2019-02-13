package io.microconfig.utils;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class CacheHandler implements InvocationHandler {
    private final ConcurrentMap<Key, Object> cache = new ConcurrentHashMap<>(512);
    private final Object delegate;

    @SuppressWarnings("unchecked")
    public static <T> T cache(T delegate) {
        return (T) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                delegate.getClass().getInterfaces(),
                new CacheHandler(delegate)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return cache.computeIfAbsent(new Key(method, args), key1 -> {
            try {
                return method.invoke(delegate, args);
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

    @EqualsAndHashCode
    private static class Key {
        private final Method method;
        private final List<Object> args;

        private Key(Method method, Object[] args) {
            this.method = method;
            this.args = args != null && args.length > 0 ? asList(args) : emptyList();
        }

        @Override
        public String toString() {
            return args.get(0).toString();
        }
    }
}