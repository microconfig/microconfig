package io.microconfig.core.properties.serializers;

import lombok.Data;

@Data
public class ConfigResult {
    private final String fileName;
    private final String configType;
    private final String content;
}