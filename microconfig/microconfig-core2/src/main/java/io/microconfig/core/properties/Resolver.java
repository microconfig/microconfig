package io.microconfig.core.properties;

public interface Resolver {
    String resolve(CharSequence value,
                   ComponentWitsEnv currentComponent,
                   ComponentWitsEnv rootComponent,
                   String configType);
}