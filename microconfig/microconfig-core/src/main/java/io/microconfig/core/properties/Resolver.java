package io.microconfig.core.properties;

public interface Resolver {
    String resolve(String value, ComponentWithEnv sourceOfValue, ComponentWithEnv root);
}