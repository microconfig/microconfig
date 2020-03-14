package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Environments;
import io.microconfig.domain.Property;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.configTypeWithName;
import static lombok.AccessLevel.PACKAGE;

@RequiredArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder {
    private static final String SELF_REFERENCE = "this";

    @Getter
    private final int startIndex;
    @Getter
    private final int endIndex;

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
                .orElse(defaultValue);
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