package deployment.util;

import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class CacheFactory implements InvocationHandler {
    private final ConcurrentMap<Key, Object> cache = new ConcurrentHashMap<>(512, 0.75f);
    private final Object delegate;

    public CacheFactory(Object delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cache(T delegate) {
        return (T) Proxy.newProxyInstance(delegate.getClass().getClassLoader(), delegate.getClass().getInterfaces(), new CacheFactory(delegate));
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
        if (e.getCause() != null) {
            if (e.getCause() instanceof RuntimeException) {
                throw ((RuntimeException) e.getCause());
            }
            if (e.getCause() instanceof Error) {
                throw ((Error) e.getCause());
            }
            throw new RuntimeException(e.getCause());
        }
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