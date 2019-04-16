package io.microconfig.factory.configtypes;

import io.microconfig.factory.ConfigType;
import io.microconfig.factory.ConfigsTypeProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.factory.configtypes.ConfigTypeImpl.byName;
import static io.microconfig.factory.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@Getter
@RequiredArgsConstructor
public enum StandardConfigTypes {
    APPLICATION(byNameAndExtensions("app", setOf(".properties", ".yaml"), "application")),
    PROCESS(byNameAndExtensions("process", setOf(".process", ".proc"), "process")),
    DEPLOY(byName("deploy")),
    ENV(byName("env")),
    SECRET(byName("secret")),
    LOG4j(byName("log4j")),
    LOG4J2(byName("log4j2"));

    private final ConfigType type;

    public static ConfigsTypeProvider asProvider() {
        return (File ignore) -> of(StandardConfigTypes.values())
                .map(StandardConfigTypes::getType)
                .collect(toList());
    }
}