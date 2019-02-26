package io.microconfig.configs.serializer;

import io.microconfig.configs.Property;

import java.io.File;
import java.util.Collection;

public interface FilenameGenerator {
    File fileFor(String component, Collection<Property> properties);
}
