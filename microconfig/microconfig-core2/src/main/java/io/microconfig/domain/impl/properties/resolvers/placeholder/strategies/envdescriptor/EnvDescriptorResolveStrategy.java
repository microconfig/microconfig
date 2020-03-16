package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor;

import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.environments.repository.EnvironmentException;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.PlaceholderSource;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.domain.impl.properties.PropertyImpl.tempProperty;
import static io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.PlaceholderSource.ENV_SOURCE;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class EnvDescriptorResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;
    private final Map<String, EnvProperty> propertyByKey;

    @Override
    public Optional<Property> resolve(Placeholder p) {
        EnvProperty envProperty = propertyByKey.get(p.getValue());
        if (envProperty == null) return empty();

        Environment environment = getEnvironment(p.getEnvironment());
        if (environment == null) return empty();

        return envProperty.value(p.getComponent(), p.getComponent(), environment)//todo
                .map(value -> tempProperty(p.getValue(), value, p.getEnvironment(), new PlaceholderSource(p.getComponent(), ENV_SOURCE)));
    }

    private Environment getEnvironment(String environment) {
        try {
            return environmentRepository.getByName(environment);
        } catch (EnvironmentException e) {
            return null;
        }
    }
}