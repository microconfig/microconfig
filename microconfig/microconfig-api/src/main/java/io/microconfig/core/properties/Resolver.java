package io.microconfig.core.properties;

public interface Resolver {
    String resolve(String value, DeclaringComponent sourceOfValue, DeclaringComponent root);
}