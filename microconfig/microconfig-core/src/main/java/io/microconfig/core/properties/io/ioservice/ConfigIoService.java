package io.microconfig.core.properties.io.ioservice;

import java.io.File;

public interface ConfigIoService {
    ConfigReader readFor(File file);

    ConfigWriter writeTo(File file);
}