package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.properties.PropertyImpl.parse;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

/**
 * '+env:' block overrides properties for the env, '+env1, env2:' for several envs.
 * '#var' lines or a '.var:' block inside the block declare temp properties for the same envs.
 * '.var:' block at the top level declares temp properties for all envs, like several '#var' lines.
 */
@RequiredArgsConstructor(access = PRIVATE)
@EqualsAndHashCode
public class OverrideProperty implements Property {
    private static final String ENV_PREFIX = "+";
    private static final String MULTILINE_VAR = ".var";
    private static final String MULTILINE_VAR_PREFIX = MULTILINE_VAR + ".";
    private static final String ENV_SEPARATOR = ",";

    @Getter
    private final List<String> environments;
    private final Property delegate;

    public static boolean isOverrideProperty(String key) {
        return isEnvBlock(key) || isMultilineVar(key) || key.equals(MULTILINE_VAR);
    }

    public static boolean isEnvBlock(String key) {
        return key.startsWith(ENV_PREFIX);
    }

    public static Property overrideProperty(String key, String value, ConfigFormat configFormat, DeclaringComponent source) {
        return overrideProperty(key, value, configFormat, source, false);
    }

    public static Property overrideProperty(String key, String value, ConfigFormat configFormat, DeclaringComponent source, boolean literalText) {
        int envsEnd = envsEnd(key);
        if (envsEnd == key.length()) throw invalidOverride(propertyMissing(key), key, source);

        List<String> environments = extractEnvs(key, envsEnd, source);
        String propertyKey = key.substring(envsEnd + 1);
        if (propertyKey.equals(MULTILINE_VAR)) throw invalidOverride(propertyMissing(propertyKey), key, source);

        boolean isNestedMultilineVar = isMultilineVar(propertyKey);
        String adjustedKey = isNestedMultilineVar ? propertyKey.substring(MULTILINE_VAR_PREFIX.length()) : propertyKey;
        Property delegate = new PropertyImpl(adjustedKey, value, isMultilineVar(key) || isNestedMultilineVar, configFormat, source, literalText);
        return new OverrideProperty(environments, delegate);
    }

    // '#var key: value' line inside the '+env:' block with the given header
    public static Property envVarProperty(String envBlockHeader, String varLine, ConfigFormat configFormat, DeclaringComponent source) {
        List<String> environments = extractEnvs(envBlockHeader, envsEnd(envBlockHeader), source);
        return new OverrideProperty(environments, parse(varLine, configFormat, source));
    }

    // a block without properties: '+env:' with '#var' lines only, an empty '.var:'. A malformed env list still fails
    public static boolean isEmptyBlock(String key, String value, DeclaringComponent source) {
        if (!value.isEmpty()) return false;

        int envsEnd = envsEnd(key);
        boolean isEmpty = envsEnd == key.length() || key.substring(envsEnd + 1).equals(MULTILINE_VAR);
        if (isEmpty) extractEnvs(key, envsEnd, source);
        return isEmpty;
    }

    // '.var:' block goes at the top level or directly inside '+env:', deeper it isn't a var block
    public static void checkMultilineVarPosition(String key, DeclaringComponent source) {
        String rest = withoutAllowedMultilineVar(key) + ".";
        if (rest.contains("." + MULTILINE_VAR_PREFIX) || rest.startsWith(MULTILINE_VAR_PREFIX)) {
            throw new IllegalArgumentException("'.var:' block must be at the top level or directly inside a '+env:' block, but key '"
                    + key + "' has it deeper in " + source);
        }
    }

    private static String withoutAllowedMultilineVar(String key) {
        String propertyKey = key;
        if (isEnvBlock(key)) {
            int envsEnd = envsEnd(key);
            propertyKey = envsEnd == key.length() ? "" : key.substring(envsEnd + 1);
        }
        if (propertyKey.equals(MULTILINE_VAR)) return "";
        return isMultilineVar(propertyKey) ? propertyKey.substring(MULTILINE_VAR_PREFIX.length()) : propertyKey;
    }

    private static int envsEnd(String key) {
        int dotIndex = key.indexOf('.', 1);
        return dotIndex < 0 ? key.length() : dotIndex;
    }

    private static String propertyMissing(String key) {
        return key.equals(MULTILINE_VAR) ? "must be a block of temp properties" : "must contain a property, like '+env.key'";
    }

    private static boolean isMultilineVar(String key) {
        return key.startsWith(MULTILINE_VAR_PREFIX);
    }

    // '+' goes once, before the first env: '+prod-a, prod-b'
    private static List<String> extractEnvs(String key, int envsEnd, DeclaringComponent source) {
        if (!isEnvBlock(key)) return emptyList();

        List<String> envs = stream(key.substring(ENV_PREFIX.length(), envsEnd).split(ENV_SEPARATOR, -1))
                .map(String::trim)
                .collect(toList());
        checkEnvNames(envs, key, source);
        return envs.stream().distinct().collect(toList());
    }

    // a single empty env ('+.key') is kept as it was before env lists appeared: it just never matches
    private static void checkEnvNames(List<String> envs, String key, DeclaringComponent source) {
        if (envs.stream().anyMatch(OverrideProperty::isEnvBlock)) {
            throw invalidOverride("must have '+' only once, before the first env, like '+prod-a, prod-b'", key, source);
        }
        if (envs.size() > 1 && envs.contains("")) {
            throw invalidOverride("has an empty env name in the env list", key, source);
        }
    }

    private static IllegalArgumentException invalidOverride(String problem, String key, DeclaringComponent source) {
        return new IllegalArgumentException("Override '" + key + "' " + problem + " in " + source);
    }

    public boolean isForAllEnvs() {
        return environments.isEmpty();
    }

    public boolean isOnlyFor(String env) {
        return environments.size() == 1 && environments.contains(env);
    }

    public boolean isSharedWith(String env) {
        return environments.size() > 1 && environments.contains(env);
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
    public boolean isLiteralText() {
        return delegate.isLiteralText();
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
