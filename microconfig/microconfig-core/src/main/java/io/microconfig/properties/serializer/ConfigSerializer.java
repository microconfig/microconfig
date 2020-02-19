package io.microconfig.properties.serializer;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

public interface ConfigSerializer {
    Optional<File> serialize(String component, String env, Collection<Property> properties);

    File configDestination(String component, String env, Collection<Property> properties);
}
