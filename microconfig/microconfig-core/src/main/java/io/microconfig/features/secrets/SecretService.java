package io.microconfig.features.secrets;

import io.microconfig.configs.Property;

import java.util.Map;
import java.util.Set;

public interface SecretService {
    Set<String> updateSecrets(Map<String, Property> componentProperties);
}