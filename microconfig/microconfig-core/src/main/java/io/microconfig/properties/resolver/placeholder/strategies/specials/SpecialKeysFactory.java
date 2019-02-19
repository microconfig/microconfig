package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.properties.resolver.placeholder.strategies.SpecialPropertyResolverStrategy.SpecialProperty;

import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class SpecialKeysFactory {
    public Map<String, SpecialProperty> specialPropertiesByKeys() {
        return asList(new IpProperty())
                .stream()
                .collect(toMap(SpecialProperty::key, identity()));
    }

    public Set<String> keyNames() {
        return specialPropertiesByKeys().keySet();
    }
}
