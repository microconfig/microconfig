package deployment.configs.command;

import deployment.configs.properties.Property;

import java.io.File;
import java.util.Map;

public interface PropertiesPostProcessor {
    void process(File serviceDir, String serviceName, Map<String, Property> properties);

    static PropertiesPostProcessor emptyPostProcessor() {
        return (dir, name, props) -> {
        };
    }
}