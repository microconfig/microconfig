package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConfigTypeTest {

    @Test
    void standardTypesRepo() {
        ConfigTypeRepository standardTypes = StandardConfigType.asRepository();
        Set<String> types = standardTypes.getConfigTypes().stream().map(ConfigType::getType).collect(toSet());

        Set<String> expectedTypes = setOf("app", "process", "deploy", "helm", "env", "secret");
        assertEquals(expectedTypes, types);
    }

}