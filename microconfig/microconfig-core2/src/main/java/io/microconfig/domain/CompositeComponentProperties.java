package io.microconfig.domain;

import java.util.List;
import java.util.Optional;

public interface CompositeComponentProperties {
    List<ComponentProperties> asList();

    CompositeComponentProperties resolveBy(StatementResolver resolver);

    List<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> List<T> save(PropertySerializer<T> serializer);
}