package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class DeclaringComponentImpl implements DeclaringComponent {
    private final String configType;
    private final String component;
    private final String environment;

    @Override
    public String toString() {
        return component + "[" + environment + "]";
    }
}