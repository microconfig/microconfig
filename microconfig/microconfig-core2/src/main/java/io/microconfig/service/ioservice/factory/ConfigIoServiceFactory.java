package io.microconfig.service.ioservice.factory;

import io.microconfig.service.ioservice.ConfigIoService;
import io.microconfig.service.ioservice.properties.PropertiesConfigIoService;
import io.microconfig.service.ioservice.selector.ConfigFormatDetectorImpl;
import io.microconfig.service.ioservice.selector.ConfigIoServiceSelector;
import io.microconfig.service.ioservice.yaml.YamlConfigIoService;
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
