package io.microconfig.core.properties;

public interface Resolver {
    String resolve(CharSequence line, String env, String configType);
}