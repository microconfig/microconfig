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

    public static DeclaringComponent copyOf(DeclaringComponent c) {
        return c instanceof DeclaringComponentImpl ? c : new DeclaringComponentImpl(c.getConfigType(), c.getComponent(), c.getEnvironment());
    }

    @Override
    public String toString() {
        return component + "[" + environment + "]";
    }
}