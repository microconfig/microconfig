package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.resolvers.RecursiveResolver
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class ChainedResolver private constructor(val resolvers: List<RecursiveResolver>) :
    RecursiveResolver {
    override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
        Optional.ofNullable(resolvers.firstNotNullOfOrNull { it.findStatementIn(line).getOrNull() })

    companion object {
        fun recursiveChainOf(vararg resolvers: RecursiveResolver): RecursiveResolver =
            ChainedResolver(resolvers.toList())
    }
}
