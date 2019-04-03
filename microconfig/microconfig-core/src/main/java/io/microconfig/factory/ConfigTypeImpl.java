package io.microconfig.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Set;

import static java.util.Collections.singleton;

@ToString
@Getter
@RequiredArgsConstructor
public class ConfigTypeImpl implements ConfigType {
    private final String name;
    private final String resultFileName;
    private final Set<String> configExtensions;

    public static ConfigType byNameAndTypes(String name, String resultFileName, Set<String> configExtensions) {
        configExtensions.stream()
                .filter(ext -> !ext.startsWith("."))
                .findFirst()
                .ifPresent(ext -> {
                    throw new IllegalStateException("File extension must start with '.'. Bad extension:" + ext);
                });

        return new ConfigTypeImpl(name, resultFileName, configExtensions);
    }

    public static ConfigType byName(String name) {
        return byNameAndTypes(name, name, singleton("." + name));
    }
}