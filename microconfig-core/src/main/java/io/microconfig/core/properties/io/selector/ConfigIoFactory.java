package io.microconfig.core.properties.io.selector;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.io.ConfigIo;
import io.microconfig.core.properties.io.properties.PropertiesConfigIo;
import io.microconfig.core.properties.io.yaml.YamlConfigIo;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;

import static io.microconfig.utils.CacheProxy.cache;

public class ConfigIoFactory {
    private static final ConfigIo configIo = newConfigIo(new DumpedFsReader());

    public static ConfigIo configIo() {
        return configIo;
    }

    public static ConfigIo configIo(ConfigFormat configFormat) {
        switch (configFormat) {
            case PROPERTIES:
                return new PropertiesConfigIo(new DumpedFsReader());
            case YAML:
                return new YamlConfigIo(new DumpedFsReader());
            default:
                throw new IllegalArgumentException("Unsupported config format: " + configFormat);
        }
    }

    public static ConfigIo newConfigIo(FsReader fsReader) {
        return new ConfigIoSelector(
                cache(new ConfigFormatDetectorImpl(fsReader)),
                new YamlConfigIo(fsReader),
                new PropertiesConfigIo(fsReader)
        );
    }
}
