package io.microconfig.core.properties;

public interface Resolver {
    String resolve(CharSequence value,
                   ComponentDescription currentComponent,
                   ComponentDescription rootComponent,
                   String configType);
}