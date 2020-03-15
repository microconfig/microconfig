package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.domain.impl.configtypes.ConfigTypeImpl.byName;
import static io.microconfig.domain.impl.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.io.CollectionUtils.setOf;
import static io.microconfig.io.StreamUtils.forEach;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public enum StandardConfigType {
    APPLICATION(byNameAndExtensions("app", setOf(".properties", ".yaml"), "application")),
    PROCESS(byNameAndExtensions("process", setOf(".process", ".proc"), "process")),
    DEPLOY(byName("deploy")),
    ENV(byName("env")),
    SECRET(byName("secret"));

    @Getter
    private final ConfigType type;

    public static ConfigTypeRepository asRepository() {
        return () -> forEach(of(StandardConfigType.values()), StandardConfigType::getType);
    }
}