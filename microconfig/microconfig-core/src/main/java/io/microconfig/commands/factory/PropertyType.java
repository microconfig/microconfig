package io.microconfig.commands.factory;

import lombok.Getter;

@Getter
public enum PropertyType {
    SERVICE("service", "properties"),
    PROCESS("process", "proc"),
    SECRET("secret"),
    DEPENDENCIES("dependencies"),
    ENV("env"),
    LOG4j("log4j"),
    LOG4J2("log4j2"),
    SAP("sap");

    private final String resultFileName;
    private final String configExtension;

    PropertyType(String configExtension) {
        this(configExtension, configExtension);
    }

    PropertyType(String resultFileName, String configExtension) {
        this.configExtension = "." + configExtension;
        this.resultFileName = resultFileName + "." + System.getProperty("outputFormat", "yaml");
    }
}