package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static io.microconfig.utils.CollectionUtils.setOf;

@Getter
@RequiredArgsConstructor
public enum StandardConfigType implements ConfigType {
    APPLICATION("app", setOf(".properties", ".yaml"), "application"),
    PROCESS("process", setOf(".process", ".proc"), "process"),
    DEPLOY("deploy", setOf("deploy"), "deploy"),
    HELM("helm", setOf(".helm"), "values"),
    ENV("env", setOf("env"), "env"),
    SECRET("secret", setOf("secret"), "secret");

    private final String type;
    private final Set<String> sourceExtensions;
    private final String resultFileName;
}