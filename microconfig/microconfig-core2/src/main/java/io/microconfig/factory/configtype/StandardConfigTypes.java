package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.factory.configtype.ConfigTypeImpl.byName;
import static io.microconfig.factory.configtype.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public enum StandardConfigTypes {
    APPLICATION(byNameAndExtensions("app", setOf(".properties", ".yaml"), "application")),
    PROCESS(byNameAndExtensions("process", setOf(".process", ".proc"), "process")),
    DEPLOY(byName("deploy")),
    ENV(byName("env")),
    SECRET(byName("secret"));

    @Getter
    private final ConfigType type;

    public static ConfigTypeProvider asProvider() {
        return () -> of(StandardConfigTypes.values())
                .map(StandardConfigTypes::getType)
                .collect(toList());
    }
}