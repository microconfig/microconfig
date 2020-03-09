package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.Property;

import java.io.File;
import java.util.Collection;

public interface FilenameGenerator {
    File getFileName(String componentName, Collection<Property> properties);
}
