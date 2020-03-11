package io.microconfig.core.properties.io.ioservice;

import java.io.File;

public interface ConfigIoService {
    ConfigReader read(File file);

    ConfigWriter writeTo(File file);
}