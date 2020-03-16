package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeFilter;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.microconfig.utils.FileUtils.getExtension;
import static io.microconfig.utils.StreamUtils.filter;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

public class ConfigTypeFilters {
    public static ConfigTypeFilter eachConfigType() {
        return types -> types;
    }

    public static ConfigTypeFilter configType(ConfigType... types) {
        return __ -> asList(types);
    }

    public static ConfigTypeFilter configTypeWithName(String... types) {
        Set<String> names = new HashSet<>(asList(types));
        return configTypes -> {
            validateNames(names, configTypes);
            return filter(configTypes, type -> names.contains(type.getType()));
        };
    }

    public static ConfigTypeFilter configTypeWithExtension(File file) {
        String ext = getExtension(file);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
        }
        return types -> types.stream()
                .filter(t -> t.getSourceExtensions().contains(ext))
                .findFirst()
                .map(Arrays::asList)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported config extension '" + ext + "'"));
    }

    private static void validateNames(Set<String> names, List<ConfigType> supportedTypes) {
        Set<String> supportedNames = supportedTypes.stream().map(ConfigType::getType).collect(toSet());
        names.stream()
                .filter(n -> !supportedNames.contains(n))
                .findAny()
                .ifPresent(n -> {
                    throw new IllegalArgumentException("Unsupported config type '" + n + "'."
                            + " Configured types: " + supportedNames);
                });
    }
}