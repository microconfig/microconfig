package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeFilter;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.getExtension;
import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Arrays.asList;

public class ConfigTypeFilters {
    public static ConfigTypeFilter eachConfigType() {
        return types -> types;
    }

    public static ConfigTypeFilter configTypeWithName(String... name) {
        Set<String> types = new HashSet<>(asList(name));
        return filerTypes(type -> types.contains(type.getType()), types); //todo throw exception if type is not supported
    }

    public static ConfigTypeFilter configType(StandardConfigType... standard) {
        return configTypeWithName(
                Stream.of(standard)
                        .map(ConfigType::getType)
                        .toArray(String[]::new)
        );
    }

    public static ConfigTypeFilter configTypeFromExtensionOf(File file) {
        String ext = getExtension(file);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
        }
        return filerTypes(type -> type.getSourceExtensions().contains(ext), ext);
    }

    private static ConfigTypeFilter filerTypes(Predicate<ConfigType> predicate, Object typeDescription) {
        return types -> {
            List<ConfigType> result = filter(types, predicate);
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Unsupported config type '" + typeDescription + "'." +
                        " Configured types: " + forEach(types, ConfigType::getType));
            }
            return result;
        };
    }
}