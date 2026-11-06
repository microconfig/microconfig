package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.resolvers.RecursiveResolver
import java.util.Optional

class ChainedResolver private constructor(val resolvers: List<RecursiveResolver>) :
    RecursiveResolver {
    override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
        resolvers.asSequence().map { it.findStatementIn(line) }.firstOrNull { it.isPresent }
            ?: Optional.empty()

    companion object {
        fun recursiveChainOf(vararg resolvers: RecursiveResolver): RecursiveResolver =
            ChainedResolver(resolvers.toList())
    }
}
