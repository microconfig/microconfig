package io.microconfig.core.configtypes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static io.microconfig.utils.CollectionUtils.setOf;

@Getter
@RequiredArgsConstructor
public enum StandardConfigType implements ConfigType {
    APPLICATION("app", setOf(".properties", ".yaml", ".yml"), "application"),
    PROCESS("process", setOf(".process", ".proc"), "process"),
    HELM("helm", setOf(".helm"), "values"),
    DEPLOY("deploy"),
    K8S("k8s"),
    ENV("env"),
    SECRET("secret"),
    LOG4J("log4j"),
    LOG4J2("log4j2");

    StandardConfigType(String name) {
        this(name, setOf("." + name), name);
    }

    private final String name;
    private final Set<String> sourceExtensions;
    private final String resultFileName;
    private final String resultFileExtension = null;
}