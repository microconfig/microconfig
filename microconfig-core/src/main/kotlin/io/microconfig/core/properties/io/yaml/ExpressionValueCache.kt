package io.microconfig.core.properties.io.yaml

internal typealias ExpressionResultCache = (expression: String, evaluate: () -> String) -> String

internal fun interface ExpressionValueCache {
    fun resolve(scalarStart: Int, expressionStart: Int, expression: String, evaluate: () -> String): String
}
