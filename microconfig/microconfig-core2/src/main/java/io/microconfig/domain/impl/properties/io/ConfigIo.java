package io.microconfig.domain.impl.properties.io;

import java.io.File;

public interface ConfigIo {
    ConfigReader readFrom(File file);

    ConfigWriter writeTo(File file);
}