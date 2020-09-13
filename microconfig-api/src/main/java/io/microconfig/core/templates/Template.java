package io.microconfig.core.templates;

import java.io.File;

public interface Template {
    String getFileName();
    String getContent();
    File getDestination();
    File getSource();
}
