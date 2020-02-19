package io.microconfig.properties.io.ioservice.yaml;

import java.util.Map;

public interface YamlTree {
    String toYaml(Map<String, String> flatProperties);
}
