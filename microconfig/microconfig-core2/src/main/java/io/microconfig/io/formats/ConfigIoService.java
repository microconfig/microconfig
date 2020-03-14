package io.microconfig.io.formats;

import java.io.File;

public interface ConfigIoService {
    ConfigReader read(File file);

    ConfigWriter writeTo(File file);
}