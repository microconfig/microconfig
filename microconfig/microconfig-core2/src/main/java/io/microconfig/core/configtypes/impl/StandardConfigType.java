package io.microconfig.core.configtypes.impl;

import io.microconfig.core.configtypes.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static io.microconfig.utils.CollectionUtils.setOf;

@Getter
@RequiredArgsConstructor
public enum StandardConfigType implements ConfigType {
    APPLICATION("app", setOf(".properties", ".yaml"), "application"),
    PROCESS("process", setOf(".process", ".proc"), "process"),
    HELM("helm", setOf(".helm"), "values"),
    DEPLOY("deploy", setOf(".deploy"), "deploy"),
    ENV("env", setOf(".env"), "env"),
    SECRET("secret", setOf(".secret"), "secret"),
    LOG4J("log4j", setOf(".log4j"), "log4j"),
    LOG4J2("log4j2", setOf(".log4j2"), "log4j2");

    private final String type;
    private final Set<String> sourceExtensions;
    private final String resultFileName;
}