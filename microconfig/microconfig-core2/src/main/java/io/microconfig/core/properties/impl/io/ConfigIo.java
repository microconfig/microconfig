package io.microconfig.core.properties.impl.io;

import java.io.File;

public interface ConfigIo {
    ConfigReader readFrom(File file);

    ConfigWriter writeTo(File file);
}