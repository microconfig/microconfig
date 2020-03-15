package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import lombok.Value;

import java.util.List;
import java.util.Set;

import static io.microconfig.io.StreamUtils.filter;
import static java.util.Collections.singleton;

@Value
public class ConfigTypeImpl implements ConfigType {
    private final String type;
    private final Set<String> sourceExtensions;
    private final String resultFileName;

    public static ConfigType byName(String name) {
        return byNameAndExtensions(name, singleton('.' + name), name);
    }

    public static ConfigType byNameAndExtensions(String name, Set<String> sourceExtensions, String resultFileName) {
        List<String> badExtensions = filter(sourceExtensions, ext -> !ext.startsWith("."));
        if (!badExtensions.isEmpty()) {
            throw new IllegalArgumentException("File extension must start with '.'. Current: " + badExtensions);
        }

        return new ConfigTypeImpl(name, sourceExtensions, resultFileName);
    }
}