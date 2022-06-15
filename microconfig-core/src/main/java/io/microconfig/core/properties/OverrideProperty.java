package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
@EqualsAndHashCode
public class OverrideProperty implements Property {
    private static final String MULTI_VAR_PREFIX = "@var.";

    @Getter
    private final String environment;
    private final Property delegate;

    public static boolean isOverrideProperty(String line) {
        return line.startsWith("@") || line.startsWith("+");
    }

    public static Property overrideProperty(String key, String value, ConfigFormat configFormat, DeclaringComponent source) {
        boolean isVar = key.startsWith("@");
        int offset = key.indexOf('.');
        String envName = extractEnv(key, offset);
        String adjustedKey = key.substring(offset + 1);
        Property delegate = new PropertyImpl(adjustedKey, value, isVar, configFormat, source);
        return new OverrideProperty(envName, delegate);
    }

    private static String extractEnv(String key, int offset) {
        return key.startsWith(MULTI_VAR_PREFIX) ? null : key.substring(1, offset);
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
