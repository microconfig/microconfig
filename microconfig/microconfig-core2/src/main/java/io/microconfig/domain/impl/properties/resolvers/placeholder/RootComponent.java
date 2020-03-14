package io.microconfig.domain.impl.properties.resolvers.placeholder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class RootComponent {
    private final String componentName;
    private final String environment;

    @Override
    public String toString() {
        return componentName + "[" + environment + "]";
    }
}
