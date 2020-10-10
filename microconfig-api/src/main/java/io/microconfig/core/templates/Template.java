package io.microconfig.core.templates;

import java.io.File;

public interface Template {
    File getSource();

    File getDestination();

    String getFileName();

    String getContent();
}