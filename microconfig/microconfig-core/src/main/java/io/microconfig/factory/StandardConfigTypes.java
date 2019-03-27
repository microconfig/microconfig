package io.microconfig.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.factory.ConfigType.byNameAndTypes;
import static io.microconfig.factory.ConfigType.extensionAsName;

@Getter
@RequiredArgsConstructor
public enum StandardConfigTypes {
    APPLICATION(byNameAndTypes("application", ".properties", ".yaml")),
    PROCESS(byNameAndTypes("process", ".process", ".proc")),
    DEPLOY(byNameAndTypes("deploy", ".deploy")),
    ENV(extensionAsName("env")),
    SECRET(extensionAsName("secret")),
    LOG4j(extensionAsName("log4j")),
    LOG4J2(extensionAsName("log4j2"));

    private final ConfigType configType;

    public ConfigType type() {
        return configType;
    }
}