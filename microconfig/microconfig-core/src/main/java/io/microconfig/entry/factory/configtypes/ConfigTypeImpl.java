package io.microconfig.entry.factory.configtypes;

import io.microconfig.entry.factory.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static java.util.Collections.singleton;

@Getter
@RequiredArgsConstructor
public class ConfigTypeImpl implements ConfigType {
    private final String type;
    private final Set<String> sourceExtensions;
    private final String resultFileName;

    public static ConfigType byName(String name) {
        return byNameAndExtensions(name, singleton("." + name), name);
    }

    public static ConfigType byNameAndExtensions(String name, Set<String> sourceExtensions, String resultFileName) {
        sourceExtensions.stream()
                .filter(ext -> !ext.startsWith("."))
                .findFirst()
                .ifPresent(ext -> {
                    throw new IllegalArgumentException("File extension must start with '.'. Bad extension:" + ext);
                });

        return new ConfigTypeImpl(name, sourceExtensions, resultFileName);
    }
}