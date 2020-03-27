package io.microconfig.core.properties.serializer.file;

import io.microconfig.core.properties.Property;

import java.io.File;
import java.util.Collection;

public interface FilenameGenerator {
    File fileFor(String component, String env, Collection<Property> properties);
}
