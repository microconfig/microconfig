package io.microconfig.core.properties.resolvers

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.properties.PropertiesText
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

interface RecursiveResolver : Resolver {
    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String {
        if (findStatementIn(value).getOrNull() == null) return value
        // The legacy string API has no value-format parameter. Decode only at this boundary.
        val encoded = PropertiesText.isPropertiesText(sourceOfValue)
        val result = StringBuilder(if (encoded) PropertiesText.unescape(value) else value)
        resolveStatements(result, sourceOfValue, root)
        return if (encoded) PropertiesText.escapeValue(result.toString()) else result.toString()
    }

    private fun resolveStatements(
        value: StringBuilder,
        source: DeclaringComponent?,
        root: DeclaringComponent?,
    ) {
        while (true) {
            val statement = findStatementIn(value).getOrNull() ?: return
            value.replace(
                statement.startIndex,
                statement.endIndex,
                statement.resolveFor(source, root),
            )
        }
    }

    fun findStatementIn(line: CharSequence): Optional<Statement>

    interface Statement {
        val startIndex: Int
        val endIndex: Int
        val isPlainText: Boolean
            get() = true

        fun resolveFor(sourceOfValue: DeclaringComponent?, root: DeclaringComponent?): String

        fun resolveFor(
            sourceOfValue: DeclaringComponent?,
            root: DeclaringComponent?,
            targetFormat: ValueFormat,
        ): String = resolveFor(sourceOfValue, root)
    }
}
