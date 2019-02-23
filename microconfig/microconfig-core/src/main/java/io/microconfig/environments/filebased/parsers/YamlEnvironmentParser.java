package io.microconfig.environments.filebased.parsers;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class YamlEnvironmentParser extends AbstractEnvironmentParser {
    @Override
    protected Map<String, Object> toMap(String content) {
        return new Yaml().load(content);
    }
}