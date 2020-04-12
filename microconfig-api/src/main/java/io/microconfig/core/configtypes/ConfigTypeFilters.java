package io.microconfig.core.configtypes;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.microconfig.utils.CollectionUtils.setOf;
import static io.microconfig.utils.FileUtils.getExtension;
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
                throw noConfigTypesProvidedException();
            }
            return asList(types);
        };
    }

    public static ConfigTypeFilter configTypeWithName(String... name) {
        Set<String> names = setOf(name);
        if (names.isEmpty()) {
            throw noConfigTypesProvidedException();
        }
        return types -> {
            validateNames(names, types);
            return filter(types, type -> names.contains(type.getName()));
        };
    }

    public static ConfigTypeFilter configTypeWithExtensionOf(File file) {
        String ext = getExtension(file);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
        }
        return types -> types.stream()
                .filter(t -> t.getSourceExtensions().contains(ext))
                .findFirst()
                .map(Collections::singletonList)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported config extension '" + ext + "'"));
    }

    private static void validateNames(Set<String> names, List<ConfigType> supportedTypes) {
        Set<String> supportedNames = supportedTypes.stream().map(ConfigType::getName).collect(toSet());
        List<String> unsupportedNames = filter(names, n -> !supportedNames.contains(n));
        if (!unsupportedNames.isEmpty()) {
            throw new IllegalArgumentException("Unsupported config types: " + unsupportedNames + " Configured types: " + supportedNames);
        }
    }

    private static IllegalArgumentException noConfigTypesProvidedException() {
        return new IllegalArgumentException("No config types provided");
    }
}