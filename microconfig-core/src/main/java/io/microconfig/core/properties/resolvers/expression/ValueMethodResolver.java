package io.microconfig.core.properties.resolvers.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.DataBindingMethodResolver;

import java.util.List;

/**
 * Instance methods of values, like 'a-b'.split('-'), but not of reflection objects:
 * a registered #function is a java.lang.reflect.Method, so #base64.getDeclaringClass() or #base64.invoke(...) must not be reachable.
 */
class ValueMethodResolver implements MethodResolver {
    private final MethodResolver delegate = DataBindingMethodResolver.forInstanceMethodInvocation();

    @Override
    public MethodExecutor resolve(EvaluationContext context, Object target, String name,
                                  List<TypeDescriptor> argumentTypes) throws AccessException {
        if (isReflection(target)) {
            throw new AccessException("Methods of " + target.getClass().getName() + " can't be called in expressions");
        }
        return delegate.resolve(context, target, name, argumentTypes);
    }

    private boolean isReflection(Object target) {
        if (target instanceof Class || target instanceof ClassLoader || target instanceof Module) return true;

        String type = target.getClass().getName();
        return type.startsWith("java.lang.reflect.") || type.startsWith("java.lang.invoke.");
    }
}
