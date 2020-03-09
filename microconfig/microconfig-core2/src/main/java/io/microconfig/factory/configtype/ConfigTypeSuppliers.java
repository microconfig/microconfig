package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeSupplier;

import java.io.File;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class ConfigTypeSuppliers {
    public static ConfigTypeSupplier configTypeByName(String name) {
        return getConfigType(t -> t.getType().equals(name), name);
    }

    public static ConfigTypeSupplier configTypeFromFileName(File file) {
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

    private static ConfigTypeSupplier getConfigType(Predicate<ConfigType> predicate, String name) {
        return types -> types.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> {
                    throw new IllegalStateException("Unsupported config type for '" + name + "'." +
                            " Configured types: " + types.stream().map(ConfigType::getType).collect(toList()));
                });
    }
}