package io.microconfig.core.properties.io.yaml;

import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.io.yaml.impl.YamlValueResolver;
import io.microconfig.core.properties.resolvers.RecursiveResolver;

import static io.microconfig.core.properties.resolvers.ChainedResolver.recursiveChainOf;

public final class YamlResolvers {
    private YamlResolvers() {
    }

    public static Resolver forValues(RecursiveResolver... resolvers) {
        return new YamlValueResolver(recursiveChainOf(resolvers));
    }
}
