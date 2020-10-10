package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.templates.Template;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface TypedProperties {
    ConfigType getConfigType();

    DeclaringComponent getDeclaringComponent();

    TypedProperties resolveBy(Resolver resolver);

    TypedProperties withoutVars();

    TypedProperties without(Predicate<Property> excluded);

    TypedProperties withPrefix(String prefix);

    TypedProperties withTemplates(List<Template> templates);

    Map<String, Property> getPropertiesAsMap();

    Map<String, String> getPropertiesAsKeyValue();

    Collection<Property> getProperties();

    Optional<Property> getPropertyWithKey(String key);

    <T> T save(PropertySerializer<T> serializer);
}