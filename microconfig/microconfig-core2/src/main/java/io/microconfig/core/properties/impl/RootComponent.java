package io.microconfig.core.properties.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RootComponent {
    private final String componentName;
    private final String environment;

    @Override
    public String toString() {
        return componentName + "[" + environment + "]";
    }
}
