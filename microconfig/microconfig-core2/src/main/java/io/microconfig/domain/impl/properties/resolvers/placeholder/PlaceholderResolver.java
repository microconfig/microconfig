package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Resolver;

public class PlaceholderResolver implements Resolver {
    @Override
    public Placeholder parse(CharSequence value) {
        return PlaceholderParser.parse(value).toPlaceholder("dev");
    }
}