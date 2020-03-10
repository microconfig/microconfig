package io.microconfig.domain.impl.helpers;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeFilter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class ConfigTypeFilters {
    public static ConfigTypeFilter withName(String... name) {
        List<String> types = Arrays.asList(name);
        return getConfigType(t -> types.contains(t.getType()), Arrays.toString(name));
    }

    public static ConfigTypeFilter fromFileExtension(File file) {
        Supplier<String> getExtension = () -> {
            int extIndex = file.getName().lastIndexOf('.');
            if (extIndex < 0) {
                throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
            }
            return file.getName().substring(extIndex);
        };
        String ext = getExtension.get();
        return getConfigType(t -> t.getSourceExtensions().contains(ext), file.getName());
    }

    private static ConfigTypeFilter getConfigType(Predicate<ConfigType> predicate, String typeDescription) {
        return types -> {
            List<ConfigType> result = types.stream()
                    .filter(predicate)
                    .collect(toList());
            if (result.isEmpty()) {
                throw new IllegalArgumentException("Unsupported config type for '" + typeDescription + "'." +
                        " Configured types: " + types.stream().map(ConfigType::getType).collect(toList()));
            }
            return result;
        };
    }
}