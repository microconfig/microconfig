package io.microconfig.core.properties.impl.io.selector;

import io.microconfig.core.properties.impl.io.ConfigIo;
import io.microconfig.core.properties.impl.io.properties.PropertiesConfigIo;
import io.microconfig.core.properties.impl.io.yaml.YamlConfigIo;
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
