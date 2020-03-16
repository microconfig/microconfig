package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeRepository;
import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.domain.impl.configtypes.CustomConfigTypeRepository.findDescriptorIn;
import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomConfigTypeRepositoryTest {
    @Test
    void loadCustomFilesFromFile() {
        ConfigTypeRepository customRepo = findDescriptorIn(classpathFile("configTypes"), new DumpedFsReader());

        List<ConfigType> expected = new StandardConfigTypeRepository().getConfigTypes();
        List<ConfigType> actual = customRepo.getConfigTypes();

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            ConfigType e = expected.get(i);
            ConfigType a = actual.get(i);
            assertEquals(e.getType(), a.getType());
            assertEquals(e.getResultFileName(), a.getResultFileName());
            assertEquals(e.getSourceExtensions(), a.getSourceExtensions());
        }
    }
}