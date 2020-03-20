package io.microconfig.core.properties;

public interface Resolver {
    String resolve(CharSequence value,
                   ComponentWithEnv sourceOfValue,
                   ComponentWithEnv root,
                   String configType);
}