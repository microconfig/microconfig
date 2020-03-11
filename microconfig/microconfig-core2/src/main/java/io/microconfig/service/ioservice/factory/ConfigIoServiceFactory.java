package io.microconfig.service.ioservice.factory;

import io.microconfig.service.io.FileSystemIo;
import io.microconfig.service.io.Io;
import io.microconfig.service.ioservice.ConfigIoService;
import io.microconfig.service.ioservice.properties.PropertiesConfigIoService;
import io.microconfig.service.ioservice.selector.ConfigFormatDetectorImpl;
import io.microconfig.service.ioservice.selector.ConfigIoServiceSelector;
import io.microconfig.service.ioservice.yaml.YamlConfigIoService;

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
