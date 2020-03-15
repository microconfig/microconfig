package io.microconfig.domain.impl.properties.io.factory;

import io.microconfig.domain.impl.properties.io.ConfigIoService;
import io.microconfig.domain.impl.properties.io.properties.PropertiesConfigIoService;
import io.microconfig.domain.impl.properties.io.selector.ConfigFormatDetectorImpl;
import io.microconfig.domain.impl.properties.io.selector.ConfigIoServiceSelector;
import io.microconfig.domain.impl.properties.io.yaml.YamlConfigIoService;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;

import static io.microconfig.utils.CacheProxy.cache;

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
