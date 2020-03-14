package io.microconfig.io.formats.yaml;

import java.util.Map;

public interface YamlTree {
    String toYaml(Map<String, String> flatProperties);
}
