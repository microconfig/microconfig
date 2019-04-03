package io.microconfig.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.factory.ConfigTypeImpl.byName;
import static io.microconfig.factory.ConfigTypeImpl.byNameAndTypes;
import static io.microconfig.utils.CollectionUtils.setOf;

@Getter
@RequiredArgsConstructor
public enum StandardConfigTypes {
    APPLICATION(byNameAndTypes("app", "application", setOf(".properties", ".yaml"))),
    PROCESS(byNameAndTypes("process", "process", setOf(".process", ".proc"))),
    DEPLOY(byName("deploy")),
    ENV(byName("env")),
    SECRET(byName("secret")),
    LOG4j(byName("log4j")),
    LOG4J2(byName("log4j2"));

    private final ConfigType configType;

    public ConfigType type() {
        return configType;
    }
}