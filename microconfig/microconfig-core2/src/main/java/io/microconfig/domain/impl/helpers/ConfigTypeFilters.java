package io.microconfig.domain.impl.helpers;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeFilter;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static io.microconfig.utils.FileUtils.getExtension;
import static io.microconfig.utils.StreamUtils.map;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class ConfigTypeFilters {
    public static ConfigTypeFilter eachConfigType() {
        return types -> types;
    }

    public static ConfigTypeFilter configTypeWithName(String... name) {
        Set<String> types = new HashSet<>(asList(name));
        return filter(type -> types.contains(type.getType()), Arrays.toString(name));
    }

    public static ConfigTypeFilter configTypeFromExtensionOf(File file) {
        String ext = getExtension(file);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
        }
        return filter(type -> type.getSourceExtensions().contains(ext), file.getName());
    }

    private static ConfigTypeFilter filter(Predicate<ConfigType> predicate, String typeDescription) {
        return types -> {
            List<ConfigType> result = types.stream()
                    .filter(predicate)
                    .collect(toList());
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Unsupported config type for '" + typeDescription + "'." +
                        " Configured types: " + map(types, ConfigType::getType));
            }
            return result;
        };
    }
}