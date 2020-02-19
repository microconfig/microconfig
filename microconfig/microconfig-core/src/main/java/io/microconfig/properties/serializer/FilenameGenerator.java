package io.microconfig.properties.serializer;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;

public interface FilenameGenerator {
    File fileFor(String component, String env, Collection<Property> properties);
}
