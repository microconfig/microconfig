package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
@EqualsAndHashCode
public class OverrideProperty implements Property {
    private static final String ENV_VAR_VALUE = "@var.";

    @Getter
    private final String environment;
    private final Property delegate;

    public static boolean isOverrideProperty(String line) {
        return line.startsWith("@");
    }

    public static Property overrideProperty(String key, String value, ConfigFormat configFormat, DeclaringComponent source) {
        boolean isVar = key.contains(ENV_VAR_VALUE);
        int offset = key.indexOf('.');
        String envName = extractEnv(key, offset, isVar);
        String adjustedKey = key.substring(offset + 1);
        Property delegate = new PropertyImpl(adjustedKey, value, isVar, configFormat, source);
        return new OverrideProperty(envName, delegate);
    }

    private static String extractEnv(String key, int offset, boolean isVar) {
        if (key.startsWith(ENV_VAR_VALUE)) return null;
        return key.substring(1, isVar ? key.lastIndexOf('@') : offset);
    }

    @Override
    public String getKey() {
        return delegate.getKey();
    }

    @Override
    public String getValue() {
        return delegate.getValue();
    }

    @Override
    public boolean isVar() {
        return delegate.isVar();
    }

    public boolean multiLineVar() {
        return isVar() && environment == null;
    }

    @Override
    public ConfigFormat getConfigFormat() {
        return delegate.getConfigFormat();
    }

    @Override
    public DeclaringComponent getDeclaringComponent() {
        return delegate.getDeclaringComponent();
    }

    @Override
    public Property resolveBy(Resolver resolver, DeclaringComponent root) {
        return delegate.resolveBy(resolver, root);
    }
}
