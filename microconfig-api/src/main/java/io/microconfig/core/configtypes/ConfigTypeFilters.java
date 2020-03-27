package io.microconfig.core.configtypes;

import io.microconfig.utils.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.microconfig.utils.StreamUtils.filter;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

public class ConfigTypeFilters {
    public static ConfigTypeFilter eachConfigType() {
        return types -> types;
    }

    public static ConfigTypeFilter configType(ConfigType... types) {
        return __ -> {
            if (types.length == 0) {
                throwNoConfigTypesProvidedException();
            }
            return asList(types);
        };
    }

    public static ConfigTypeFilter configTypeWithName(String... name) {
        Set<String> names = new HashSet<>(asList(name));
        if (names.isEmpty()) {
            throwNoConfigTypesProvidedException();
        }
        return configTypes -> {
            validateNames(names, configTypes);
            return filter(configTypes, type -> names.contains(type.getName()));
        };
    }

    public static ConfigTypeFilter configTypeWithExtensionOf(File file) {
        String ext = FileUtils.getExtension(file);
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
        Set<String> supportedNames = supportedTypes.stream().map(ConfigType::getName).collect(toSet());
        List<String> unsupportedNames = filter(names, n -> !supportedNames.contains(n));
        if (!unsupportedNames.isEmpty()) {
            throw new IllegalArgumentException("Unsupported config types: " + unsupportedNames + " Configured types: " + supportedNames);
        }
    }

    private static void throwNoConfigTypesProvidedException() {
        throw new IllegalArgumentException("No config types provided");
    }
}