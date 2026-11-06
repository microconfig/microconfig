package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

/**
 * '+env:' block overrides properties for the env, '+env1, env2:' for several envs.
 * '#var' lines or a '.var:' block inside the block declare temp properties for the same envs.
 * '.var:' block declares temp properties for all envs, like several '#var' lines.
 */
@RequiredArgsConstructor(access = PRIVATE)
@EqualsAndHashCode
public class OverrideProperty implements Property {
    private static final String ENV_PREFIX = "+";
    private static final String VAR_BLOCK = ".var";
    private static final String MULTI_VAR_PREFIX = VAR_BLOCK + ".";
    private static final String ENV_SEPARATOR = ",";

    @Getter
    private final List<String> environments;
    private final Property delegate;

    public static boolean isOverrideProperty(String line) {
        return line.startsWith(ENV_PREFIX) || isMultilineVar(line);
    }

    public static Property overrideProperty(String key, String value, ConfigFormat configFormat, DeclaringComponent source) {
        return override(key, value, isMultilineVar(key), configFormat, source);
    }

    // '#var' line inside a '+env:' block
    public static Property overrideVarProperty(String key, String value, ConfigFormat configFormat, DeclaringComponent source) {
        return override(key, value, true, configFormat, source);
    }

    // '+env:' block without properties, e.g. with '#var' lines only, or an empty '.var:' block
    public static boolean isEmptyBlock(String key, String value) {
        if (!value.isEmpty()) return false;

        int envsEnd = key.indexOf('.', 1);
        return envsEnd < 0 || key.substring(envsEnd + 1).equals(VAR_BLOCK);
    }

    private static boolean isMultilineVar(String key) {
        return key.startsWith(MULTI_VAR_PREFIX);
    }

    private static Property override(String key, String value, boolean isVar, ConfigFormat configFormat, DeclaringComponent source) {
        int envsEnd = key.indexOf('.', 1);
        List<String> environments = extractEnvs(key, envsEnd, source);
        String propertyKey = key.substring(envsEnd + 1);
        boolean isVarBlock = isMultilineVar(propertyKey); // '.var:' block inside '+env:' block
        String adjustedKey = isVarBlock ? propertyKey.substring(MULTI_VAR_PREFIX.length()) : propertyKey;
        Property delegate = new PropertyImpl(adjustedKey, value, isVar || isVarBlock, configFormat, source);
        return new OverrideProperty(environments, delegate);
    }

    // '+' goes once, before the first env: '+prod-a, prod-b'
    private static List<String> extractEnvs(String key, int envsEnd, DeclaringComponent source) {
        if (isMultilineVar(key)) return emptyList();
        if (envsEnd < 0) throw invalidOverride("must contain a property, like '+env.key'", key, source);

        List<String> envs = stream(key.substring(ENV_PREFIX.length(), envsEnd).split(ENV_SEPARATOR, -1))
                .map(String::trim)
                .collect(toList());
        checkEnvNames(envs, key, source);
        return envs.stream().distinct().collect(toList());
    }

    // a single empty env ('+.key') is kept as it was before env lists appeared: it just never matches
    private static void checkEnvNames(List<String> envs, String key, DeclaringComponent source) {
        if (envs.stream().anyMatch(env -> env.startsWith(ENV_PREFIX))) {
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
