package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertyRepository;

import java.util.List;

public class FileSystemPropertyRepository implements PropertyRepository {
    @Override
    public List<Property> getProperties(String componentAlias, String componentType, String environment, ConfigType configType) {
        return null;
    }
}
