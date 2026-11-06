package io.microconfig.core.properties.resolvers

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Resolver
import java.util.Optional

interface RecursiveResolver : Resolver {
    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String {
        val result = StringBuilder(value)
        resolveStatements(result, sourceOfValue, root)
        return result.toString()
    }

    private fun resolveStatements(
        value: StringBuilder,
        source: DeclaringComponent?,
        root: DeclaringComponent?,
    ) {
        while (true) {
            val statement = findStatementIn(value).orElse(null) ?: return
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
    }
}
