package io.microconfig.core.configtypes.impl;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConfigTypeRepositoryTest {
    @Test
    void standardTypesRepo() {
        assertEquals(
                asList(StandardConfigType.values()),
                new StandardConfigTypeRepository().getConfigTypes()
        );
    }
}