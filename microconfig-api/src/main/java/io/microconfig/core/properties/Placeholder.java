package io.microconfig.core.properties;

public interface Placeholder {
    String getRootComponent();
    String getComponent();
    String getKey();
    String getEnvironment();
    String getConfigType();
    String getDefaultValue();

    Property resolveUsing(PlaceholderResolveStrategy strategy);

    DeclaringComponent getReferencedComponent();

    boolean isSelfReferenced();

    boolean referencedTo(DeclaringComponent c);

    Placeholder overrideBy(DeclaringComponent c);
}