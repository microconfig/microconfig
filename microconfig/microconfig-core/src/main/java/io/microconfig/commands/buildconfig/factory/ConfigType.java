package io.microconfig.commands.buildconfig.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Stream.of;

@ToString
@Getter
@RequiredArgsConstructor
public class ConfigType {
    private final String resultFileName;
    private final Set<String> configExtensions;

    public static ConfigType byNameAndTypes(String resultFileName, String... configExtensions) {
        of(configExtensions)
                .filter(ext -> !ext.startsWith("."))
                .findFirst()
                .ifPresent(ext -> {
                    throw new IllegalStateException("File extension must start with '.'. Bad extension:" + ext);
                });

        return new ConfigType(resultFileName, new HashSet<>(asList(configExtensions)));
    }

    public static ConfigType extensionAsName(String resultFileName) {
        return byNameAndTypes(resultFileName, "." + resultFileName);
    }
}