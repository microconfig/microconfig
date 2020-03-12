package io.microconfig.service.io.factory;

import io.microconfig.service.io.ConfigIoService;
import io.microconfig.service.io.FileSystemIo;
import io.microconfig.service.io.Io;
import io.microconfig.service.io.properties.PropertiesConfigIoService;
import io.microconfig.service.io.selector.ConfigFormatDetectorImpl;
import io.microconfig.service.io.selector.ConfigIoServiceSelector;
import io.microconfig.service.io.yaml.YamlConfigIoService;

import static io.microconfig.utils.CacheProxy.cache;

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
