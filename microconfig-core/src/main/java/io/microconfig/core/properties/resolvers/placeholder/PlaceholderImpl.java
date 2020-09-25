package io.microconfig.core.properties.resolvers.placeholder;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.Placeholder;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.Property;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import static lombok.AccessLevel.PACKAGE;

@Getter
@EqualsAndHashCode(exclude = "defaultValue")
@RequiredArgsConstructor(access = PACKAGE)
public class PlaceholderImpl implements Placeholder {
    private static final String SELF_REFERENCE = "this";

    private final String configType;
    private final String rootComponent;
    @With
    private final String component;
    @With
    private final String environment;
    private final String key;
    private final String defaultValue;

    @Override
    public Property resolveUsing(PlaceholderResolveStrategy strategy) {
        return strategy.resolve(this)
                .orElseThrow(() -> new IllegalStateException("Can't resolve " + this));
    }

    @Override
    public DeclaringComponent getReferencedComponent() {
        return new DeclaringComponentImpl(configType, component, environment);
    }

    @Override
    public boolean isSelfReferenced() {
        return component.equals(SELF_REFERENCE);
    }

    @Override
    public boolean referencedTo(DeclaringComponent c) {
        return component.equals(c.getComponent()) && environment.equals(c.getEnvironment());
    }

    @Override
    public Placeholder overrideBy(DeclaringComponent c) {
        return withComponent(c.getComponent())
                .withEnvironment(c.getEnvironment());
    }

    @Override
    public String toString() {
        return "${" +
                component +
                "[" + environment + "]" +
                "@" +
                key +
                (defaultValue == null ? "" : ":" + defaultValue) +
                "}";
    }
}