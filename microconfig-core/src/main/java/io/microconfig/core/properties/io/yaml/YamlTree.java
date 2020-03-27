package io.microconfig.core.properties.io.yaml;

import java.util.Map;

public interface YamlTree {
    String toYaml(Map<String, String> flatProperties);
}
