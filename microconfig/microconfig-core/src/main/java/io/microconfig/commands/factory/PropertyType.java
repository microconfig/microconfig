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

    private final String extension;
    private final String resultFile;

    PropertyType(String extension) {
        this(extension, extension);
    }

    PropertyType(String resultFile, String extension) {
        this.extension = "." + extension;
        this.resultFile = resultFile + ".properties";
    }
}