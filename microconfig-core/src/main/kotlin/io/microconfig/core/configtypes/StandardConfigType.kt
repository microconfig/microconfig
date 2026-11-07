package io.microconfig.core.configtypes

enum class StandardConfigType(
    override val typeName: String,
    override val sourceExtensions: Set<String>,
    override val resultFileName: String,
) : ConfigType {
    APPLICATION("app", setOf(".properties", ".yaml", ".yml"), "application"),
    PROCESS("process", setOf(".process", ".proc"), "process"),
    HELM("helm", setOf(".helm"), "values"),
    DEPLOY("deploy"),
    K8S("k8s"),
    ENV("env"),
    SECRET("secret"),
    LOG4J("log4j"),
    LOG4J2("log4j2");

    constructor(name: String) : this(name, setOf(".$name"), name)
}
