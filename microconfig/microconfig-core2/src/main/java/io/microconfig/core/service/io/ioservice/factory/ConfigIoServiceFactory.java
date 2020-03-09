package io.microconfig.core.service.io.ioservice.factory;

import io.microconfig.core.service.io.ioservice.ConfigIoService;
import io.microconfig.core.service.io.ioservice.properties.PropertiesConfigIoService;
import io.microconfig.core.service.io.ioservice.selector.ConfigFormatDetectorImpl;
import io.microconfig.core.service.io.ioservice.selector.ConfigIoServiceSelector;
import io.microconfig.core.service.io.ioservice.yaml.YamlConfigIoService;
import io.microconfig.utils.reader.FsIo;
import io.microconfig.utils.reader.Io;

import static io.microconfig.utils.CacheHandler.cache;

public class ConfigIoServiceFactory {
    private static final ConfigIoService CONFIG_IO_SERVICE = newConfigIoService(new FsIo());

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
