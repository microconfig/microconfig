package io.microconfig.io.formats.factory;

import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.FileSystemIo;
import io.microconfig.io.formats.Io;
import io.microconfig.io.formats.properties.PropertiesConfigIoService;
import io.microconfig.io.formats.selector.ConfigFormatDetectorImpl;
import io.microconfig.io.formats.selector.ConfigIoServiceSelector;
import io.microconfig.io.formats.yaml.YamlConfigIoService;

import static io.microconfig.io.CacheProxy.cache;

public class ConfigIoServiceFactory {
    private static final ConfigIoService CONFIG_IO_SERVICE = newConfigIoService(new FileSystemIo());

    public static ConfigIoService configIoService() {
        return CONFIG_IO_SERVICE;
    }

    public static ConfigIoService newConfigIoService(Io io) {
        return new ConfigIoServiceSelector(
                cache(new ConfigFormatDetectorImpl(io)),
                new YamlConfigIoService(io),
                new PropertiesConfigIoService(io)
        );
    }
}
