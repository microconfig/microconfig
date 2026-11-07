package io.microconfig.core.properties.io.yaml

internal fun interface ExpressionValueCache {
    fun resolve(scalarStart: Int, expressionStart: Int, expression: String, evaluate: () -> String): String
}
