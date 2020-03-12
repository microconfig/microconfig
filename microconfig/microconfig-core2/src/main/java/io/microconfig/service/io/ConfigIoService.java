package io.microconfig.service.io;

import java.io.File;

public interface ConfigIoService {
    ConfigReader read(File file);

    ConfigWriter writeTo(File file);
}