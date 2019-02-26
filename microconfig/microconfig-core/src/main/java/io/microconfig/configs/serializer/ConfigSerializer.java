package io.microconfig.configs.serializer;

import io.microconfig.configs.Property;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

public interface ConfigSerializer {
    Optional<File> serialize(String component, Collection<Property> properties);

    File configDestination(String component);
}
