package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static io.microconfig.utils.CollectionUtils.setOf;

@RequiredArgsConstructor
public enum StandardConfigType implements ConfigType {
    APPLICATION("app", setOf(".properties", ".yaml"), "application"),
    PROCESS("process", setOf(".process", ".proc"), "process"),
    DEPLOY("deploy", setOf("deploy"), "deploy"),
    HELM("helm", setOf(".helm"), "values"),
    ENV("env", setOf("env"), "env"),
    SECRET("secret", setOf("secret"), "secret");

    @Getter
    private final String type;

    @Getter
    private final Set<String> sourceExtensions;

    @Getter
    private final String resultFileName;

}