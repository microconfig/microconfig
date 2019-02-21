package io.microconfig.properties.serializer;

import io.microconfig.properties.Property;
import io.microconfig.properties.io.ConfigIo;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import static io.microconfig.utils.FileUtils.delete;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class PropertiesSerializerImpl implements PropertySerializer {
    private final File componentsDir;
    private final String fileName;
    private final ConfigIo configIo;

    @Override
    public Optional<File> serialize(String component, Collection<Property> properties) {
        File file = pathFor(component);
        delete(file);
        if (containsOnlySystemProperties(properties)) return empty();

        configIo.write(file, properties);
        return of(file);
    }

    @Override
    public File pathFor(String component) {
        return new File(componentsDir, component + "/" + fileName);
    }

    private boolean containsOnlySystemProperties(Collection<Property> properties) {
        return properties.stream().allMatch(p -> p.getSource().isSystem());
    }
}