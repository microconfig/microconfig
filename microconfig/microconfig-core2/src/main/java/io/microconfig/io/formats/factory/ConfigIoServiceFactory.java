package io.microconfig.io.formats.factory;

import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.properties.PropertiesConfigIoService;
import io.microconfig.io.formats.selector.ConfigFormatDetectorImpl;
import io.microconfig.io.formats.selector.ConfigIoServiceSelector;
import io.microconfig.io.formats.yaml.YamlConfigIoService;
import io.microconfig.io.io.DumpedFsReader;
import io.microconfig.io.io.FsReader;

import static io.microconfig.io.CacheProxy.cache;

public class ConfigIoServiceFactory {
    private static final ConfigIoService configIo = newConfigIoService(new DumpedFsReader());

    public static ConfigIoService configIo() {
        return configIo;
    }

    public static ConfigIoService newConfigIoService(FsReader fsReader) {
        return new ConfigIoServiceSelector(
                cache(new ConfigFormatDetectorImpl(fsReader)),
                new YamlConfigIoService(fsReader),
                new PropertiesConfigIoService(fsReader)
        );
    }
}
