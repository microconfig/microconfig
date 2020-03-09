package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.ComponentProperties;
import io.microconfig.core.domain.PropertiesSerializer;
import io.microconfig.core.domain.Property;
import io.microconfig.core.service.io.ioservice.ConfigIoService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.utils.FileUtils.delete;

@RequiredArgsConstructor
public class ComponentPropertiesImpl implements ComponentProperties {
    private final String componentName;
    private final ConfigType configType;

    private final Map<String, Property> propertyByKey;

    private final FilenameGenerator filenameGenerator;
    private final ConfigIoService configIoService;

    @Override
    public String getConfigType() {
        return configType.getName();
    }

    @Override
    public Map<String, Property> propertyByKey() {
        return propertyByKey;
    }

    @Override
    public PropertiesSerializer save() {
        return new PropertiesSerializer() {
            @Override
            public File toFile() {
                File file = filenameGenerator.getFileName(componentName, propertyByKey.values());
                delete(file);

                if (!propertyByKey.isEmpty()) {
                    configIoService.writeTo(file).write(propertyByKey.values());
                }
                return file;
            }

            @Override
            public String asString() {
                return null;
            }
        };
    }
}
