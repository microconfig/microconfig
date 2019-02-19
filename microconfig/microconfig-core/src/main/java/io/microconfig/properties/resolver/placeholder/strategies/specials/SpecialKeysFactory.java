package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.properties.resolver.placeholder.strategies.SpecialResolverStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SpecialKeysFactory {
    public List<SpecialResolverStrategy.SpecialKey> specialKeys() {
        return new ArrayList<>();
    }

    public Set<String> keyNames() {
        return Collections.emptySet();
    }
}
