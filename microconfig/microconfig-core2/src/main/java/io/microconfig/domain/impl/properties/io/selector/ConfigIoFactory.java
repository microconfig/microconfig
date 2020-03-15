package io.microconfig.domain.impl.properties.io.selector;

import io.microconfig.domain.impl.properties.io.ConfigIo;
import io.microconfig.domain.impl.properties.io.properties.PropertiesConfigIo;
import io.microconfig.domain.impl.properties.io.yaml.YamlConfigIo;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;

import static io.microconfig.utils.CacheProxy.cache;

public class ConfigIoFactory {
    private static final ConfigIo configIo = newConfigIo(new DumpedFsReader());

    public static ConfigIo configIo() {
        return configIo;
    }

    public static ConfigIo newConfigIo(FsReader fsReader) {
        return new ConfigIoSelector(
                cache(new ConfigFormatDetectorImpl(fsReader)),
                new YamlConfigIo(fsReader),
                new PropertiesConfigIo(fsReader)
        );
    }
}
