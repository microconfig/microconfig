package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Environments;
import io.microconfig.domain.Property;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.configTypeWithName;
import static lombok.AccessLevel.PACKAGE;

@RequiredArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder {
    private static final String SELF_REFERENCE = "this";

    private final String configType;
    private final String component;
    private final String environment;
    private final String value;
    private final String defaultValue;

    public String resolve(Environments environments) {
        return environments.getOrCreateWithName(environment)
                .findComponentWithName(component, false)
                .getPropertiesFor(configTypeWithName(configType))
                .getPropertyWithKey(value)
                .map(Property::getValue)
                .orElseGet(defaultValue());
    }

    private Supplier<String> defaultValue() {
        return ()-> {
            if (defaultValue != null) return defaultValue;
            throw new PropertyResolveException("can't resolve " + toString());
        };
    }

    @Override
    public String toString() {
        return "${" +
                configType + "::" +
                component +
                "[" + environment + "]" +
                "@" +
                value +
                (defaultValue == null ? "" : ":" + defaultValue) +
                "}";
    }
}