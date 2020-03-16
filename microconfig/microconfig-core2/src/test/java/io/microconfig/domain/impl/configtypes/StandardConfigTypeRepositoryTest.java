package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConfigTypeRepositoryTest {
    private StandardConfigTypeRepository standardRepo = new StandardConfigTypeRepository();

    @Test
    void standardTypesRepo() {
        Collection<String> types = standardRepo.getConfigTypes().stream().map(ConfigType::getType).collect(toSet());

        Collection<String> expectedTypes = setOf("app", "process", "helm", "deploy", "env", "secret", "log4j", "log4j2");
        assertEquals(expectedTypes, types);
    }

}