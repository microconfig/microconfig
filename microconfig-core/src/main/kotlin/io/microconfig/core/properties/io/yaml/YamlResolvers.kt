package io.microconfig.core.properties.io.yaml

import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.io.yaml.impl.YamlValueResolver
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.impl.ChainedResolver

object YamlResolvers {
    fun forValues(vararg resolvers: RecursiveResolver): Resolver =
        YamlValueResolver(ChainedResolver.recursiveChainOf(*resolvers))
}
