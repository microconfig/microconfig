package io.microconfig.configs.serializer;

import io.microconfig.configs.Property;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

public interface ConfigSerializer {
    /**
     * @param component  - component name
     * @param properties - component's properties to serialize
     * @return - path to output file or empty() if properties is empty
     */
    Optional<File> serialize(String component, Collection<Property> properties);

    File configDestination(String component);
}
