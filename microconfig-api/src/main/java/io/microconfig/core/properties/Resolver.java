package io.microconfig.core.properties;

public interface Resolver {
    String resolve(String value, DeclaringComponent sourceOfValue, DeclaringComponent root);

    default String resolve(Property property, DeclaringComponent root) {
        return resolve(property.getValue(), property.getDeclaringComponent(), root);
    }
}
