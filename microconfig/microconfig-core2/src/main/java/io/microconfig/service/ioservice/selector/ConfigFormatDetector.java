package io.microconfig.service.ioservice.selector;

import io.microconfig.service.ioservice.ConfigFormat;

import java.io.File;

public interface ConfigFormatDetector {
    ConfigFormat detectConfigFormat(File file);
}