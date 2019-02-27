package io.microconfig.configs.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static io.microconfig.configs.types.ConfigType.byNameAndTypes;
import static io.microconfig.configs.types.ConfigType.extensionAsName;


@Getter
@RequiredArgsConstructor
public enum StandardConfigType {
    SERVICE(byNameAndTypes("service", ".properties", ".yaml")),
    PROCESS(byNameAndTypes("process", ".process", ".proc")),
    DEPLOY(byNameAndTypes("deploy", ".deploy")),
    ENV(extensionAsName("env")),
    SECRET(extensionAsName("secret")),
    LOG4j(extensionAsName("log4j")),
    LOG4J2(extensionAsName("log4j2"));

    private final ConfigType configType;

    public String getResultFileName() {
        return configType.getResultFileName();
    }

    public Set<String> getConfigExtensions() {
        return configType.getConfigExtensions();
    }

    public ConfigType type() {
        return configType;
    }
}