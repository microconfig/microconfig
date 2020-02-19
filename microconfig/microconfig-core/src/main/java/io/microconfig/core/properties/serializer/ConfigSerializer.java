package io.microconfig.core.properties.serializer;

import io.microconfig.core.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

public interface ConfigSerializer {
    Optional<File> serialize(String component, String env, Collection<Property> properties);

    File configDestination(String component, String env, Collection<Property> properties);
}
