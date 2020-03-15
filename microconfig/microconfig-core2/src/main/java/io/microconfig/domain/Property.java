package io.microconfig.domain;

public interface Property {
    String getKey();

    String getValue();

    PropertySource getSource();

    boolean isTemp();

    Property resolveBy(StatementResolver resolver);
}