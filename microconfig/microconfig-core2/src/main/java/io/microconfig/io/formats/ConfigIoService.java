package io.microconfig.io.formats;

import java.io.File;

public interface ConfigIoService {
    ConfigReader readFrom(File file);

    ConfigWriter writeTo(File file);
}