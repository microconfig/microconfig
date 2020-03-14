package io.microconfig.core.properties.serializer.file;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ioservice.ConfigIoService;
import io.microconfig.core.properties.serializer.ConfigSerializer;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import static io.microconfig.io.FileUtils.delete;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class ToFileConfigSerializer implements ConfigSerializer {
    private final FilenameGenerator filenameGenerator;
    private final ConfigIoService configIoService;

    @Override
    public Optional<File> serialize(String component, String env, Collection<Property> properties) {
        File file = configDestination(component, env, properties);
        delete(file);

        if (properties.isEmpty()) return empty();

        configIoService.writeTo(file).write(properties);
        return of(file);
    }

    @Override
    public File configDestination(String component, String env, Collection<Property> properties) {
        return filenameGenerator.fileFor(component, env, properties);
    }
}