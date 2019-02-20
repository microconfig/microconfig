package io.microconfig.properties.serializer;

import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import static io.microconfig.properties.serializer.PropertiesSerializerImpl.OutputFormat.PROPERTIES;
import static io.microconfig.utils.FileUtils.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor
public class PropertiesSerializerImpl implements PropertySerializer {
    private final File componentsDir;
    private final String fileName;
    private final OutputFormat outputFormat = PROPERTIES;

    @Override
    public Optional<File> serialize(String component, Collection<Property> properties) {
        File file = pathFor(component);
        delete(file);
        if (containsOnlySystemProperties(properties)) return empty();

        write(file, outputFormat.serialize(properties));
        return of(file);
    }

    @Override
    public File pathFor(String component) {
        return new File(componentsDir, component + "/" + fileName);
    }

    private boolean containsOnlySystemProperties(Collection<Property> properties) {
        return properties.stream().allMatch(p -> p.getSource().isSystem());
    }

    public enum OutputFormat {
        PROPERTIES {
            @Override
            String serialize(Collection<Property> properties) {
                return properties.stream()
                        .filter(p -> !p.isTemp())
                        .map(Property::toString)
                        .collect(joining(LINE_SEPARATOR));
            }
        };

        abstract String serialize(Collection<Property> properties);
    }
}