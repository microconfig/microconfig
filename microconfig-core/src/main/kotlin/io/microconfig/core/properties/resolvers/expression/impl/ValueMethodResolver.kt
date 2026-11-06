package io.microconfig.core.properties.resolvers.expression.impl

import org.springframework.core.convert.TypeDescriptor
import org.springframework.expression.AccessException
import org.springframework.expression.EvaluationContext
import org.springframework.expression.MethodExecutor
import org.springframework.expression.MethodResolver
import org.springframework.expression.spel.support.DataBindingMethodResolver

internal class ValueMethodResolver : MethodResolver {
    private val delegate = DataBindingMethodResolver.forInstanceMethodInvocation()

    override fun resolve(
        context: EvaluationContext,
        target: Any,
        name: String,
        argumentTypes: List<TypeDescriptor>,
    ): MethodExecutor? {
        if (isReflection(target))
            throw AccessException(
                "Methods of ${target.javaClass.name} can't be called in expressions"
            )
        return delegate.resolve(context, target, name, argumentTypes)
    }

    private fun isReflection(target: Any): Boolean =
        target is Class<*> ||
            target is ClassLoader ||
            target is Module ||
            target.javaClass.name.startsWith("java.lang.reflect.") ||
            target.javaClass.name.startsWith("java.lang.invoke.")
}
