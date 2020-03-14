package io.microconfig.domain.impl.configtype;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.domain.impl.configtype.ConfigTypeImpl.byName;
import static io.microconfig.domain.impl.configtype.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.io.CollectionUtils.setOf;
import static io.microconfig.io.StreamUtils.forEach;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public enum StandardConfigType {
    APPLICATION(byNameAndExtensions("app", setOf(".properties", ".yaml"), "application")),
    PROCESS(byNameAndExtensions("process", setOf(".process", ".proc"), "process")),
    DEPLOY(byName("deploy")),
    ENV(byName("env")),
    SECRET(byName("secret"));

    @Getter
    private final ConfigType type;

    public static ConfigTypes asTypes() {
        return () -> forEach(asList(StandardConfigType.values()), StandardConfigType::getType);
    }
}