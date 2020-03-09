package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

public interface ConfigTypeProvider {
    List<ConfigType> getTypes();

    default ConfigType detectConfigType(File file) {
        Supplier<String> getExtension = () -> {
            int extIndex = file.getName().lastIndexOf('.');
            if (extIndex < 0) {
                throw new IllegalArgumentException("File " + file + " doesn't have an extension. Unable to resolve component type.");
            }
            return file.getName().substring(extIndex);
        };
        String ext = getExtension.get();
        return getTypes().stream()
                .filter(t -> t.getSourceExtensions().contains(ext))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find ConfigType for extension " + ext));
    }
}