package io.microconfig.domain.impl.properties.io.yaml;

import java.util.Map;

public interface YamlTree {
    String toYaml(Map<String, String> flatProperties);
}
