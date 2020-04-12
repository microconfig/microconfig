package io.microconfig.core.configtypes;

import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.configtypes.CustomConfigTypeRepository.findDescriptorIn;
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
            assertEquals(e.getName(), a.getName());
            assertEquals(e.getResultFileName(), a.getResultFileName());
            assertEquals(e.getSourceExtensions(), a.getSourceExtensions());
        }
    }
}