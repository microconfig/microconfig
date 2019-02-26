package io.microconfig.commands.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static io.microconfig.commands.factory.ConfigType.byNameAndTypes;
import static io.microconfig.commands.factory.ConfigType.extensionAsName;


@Getter
@RequiredArgsConstructor
public enum StandardConfigTypes {
    SERVICE(byNameAndTypes("service", ".properties", ".yaml")),
    PROCESS(byNameAndTypes("process", ".proc")),
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