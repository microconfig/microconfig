package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.factory.configtype.ConfigTypeImpl.byName;
import static io.microconfig.factory.configtype.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.CollectionUtils.setOf;
import static io.microconfig.utils.StreamUtils.map;
import static java.util.Arrays.asList;

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
        return () -> map(asList(StandardConfigTypes.values()), StandardConfigTypes::getType);
    }
}