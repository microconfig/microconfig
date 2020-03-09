package io.microconfig.core.domain;

import java.io.File;

public interface PropertiesSerializer {
    File toFile();

    String asString();
}