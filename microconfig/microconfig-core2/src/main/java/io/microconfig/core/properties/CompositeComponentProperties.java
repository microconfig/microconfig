package io.microconfig.core.properties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CompositeComponentProperties {
    List<ComponentProperties> asList();

    CompositeComponentProperties resolveBy(StatementResolver resolver);

    CompositeComponentProperties withoutTempValues();

    List<Property> getProperties();

    Map<String, String> propertiesAsKeyValue();

    Optional<Property> getPropertyWithKey(String key);

    <T> List<T> save(PropertySerializer<T> serializer);
}