package io.microconfig.core.properties.resolvers.expression.impl

import org.springframework.core.convert.TypeDescriptor
import org.springframework.expression.EvaluationContext
import org.springframework.expression.ExpressionParser
import org.springframework.expression.TypeConverter
import org.springframework.expression.spel.SpelEvaluationException
import org.springframework.expression.spel.SpelMessage
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.expression.spel.support.StandardTypeConverter
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class ExpressionEvaluator(
    private val parser: ExpressionParser,
    private val context: EvaluationContext,
    private val functionNames: String,
) {
    fun evaluate(value: String): String =
        try {
            requireNotNull(parser.parseExpression(value).getValue(context, String::class.java)) {
                "Expression evaluated to null"
            }
        } catch (error: SpelEvaluationException) {
            throw withHint(error)
        }

    private fun withHint(error: SpelEvaluationException): RuntimeException =
        when (error.messageCode) {
            SpelMessage.TYPE_NOT_FOUND,
            SpelMessage.CONSTRUCTOR_NOT_FOUND ->
                IllegalArgumentException(
                    "${error.message}. Java types can't be used in expressions: T(...) and new aren't supported",
                    error,
                )

            SpelMessage.FUNCTION_NOT_DEFINED ->
                IllegalArgumentException(
                    "${error.message}. ${replacementOf(error.inserts?.getOrNull(0))}Available functions: $functionNames",
                    error,
                )

            else -> error
        }

    private fun replacementOf(function: Any?): String =
        when {
            function == "getenv" -> "Use \${env@NAME} placeholder instead. "
            function == "getProperty" -> "Use \${system@name} placeholder instead. "
            function.toString().startsWith("read") -> "Files can't be read in expressions. "
            else -> ""
        }

    companion object {
        fun withFunctionsFrom(vararg classes: Class<*>): ExpressionEvaluator {
            val functions =
                classes.flatMap { it.methods.toList() }.filter { Modifier.isStatic(it.modifiers) }
            return ExpressionEvaluator(
                SpelExpressionParser(),
                contextWith(functions),
                functions.map { "#${it.name}" }.distinct().sorted().joinToString(", "),
            )
        }

        private fun contextWith(functions: List<Method>): EvaluationContext {
            val context =
                SimpleEvaluationContext.forReadOnlyDataBinding()
                    .withMethodResolvers(ValueMethodResolver())
                    .withTypeConverter(stringConverter())
                    .build()
            functions.forEach { context.setVariable(it.name, it) }
            return context
        }

        private fun stringConverter(): TypeConverter = object : TypeConverter {
            private val delegate = StandardTypeConverter()

            override fun canConvert(sourceType: TypeDescriptor?, targetType: TypeDescriptor): Boolean =
                delegate.canConvert(sourceType, targetType)

            override fun convertValue(value: Any?, sourceType: TypeDescriptor?, targetType: TypeDescriptor): Any? =
                if (targetType.type == String::class.java &&
                    (value is String || value is Number || value is Boolean || value is Char)
                ) value.toString()
                else delegate.convertValue(value, sourceType, targetType)
        }
    }
}
