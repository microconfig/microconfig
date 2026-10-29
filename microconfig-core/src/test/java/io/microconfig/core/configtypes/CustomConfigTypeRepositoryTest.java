package io.microconfig.core.configtypes;

import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.configtypes.CustomConfigTypeRepository.findDescriptorIn;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomConfigTypeRepositoryTest {
    @Test
    void loadCustomTypeWithDefaultExtension() {
        ConfigTypeRepository customRepo = findDescriptorIn(classpathFile("configTypes/defaultExtensions"), new DumpedFsReader());

        List<ConfigType> types = customRepo.getConfigTypes();

        assertEquals(1, types.size());
        ConfigType type = types.get(0);
        assertEquals("custom", type.getName());
        assertEquals("result", type.getResultFileName());
        assertEquals(Set.of(".custom"), type.getSourceExtensions());
    }

    @Test
    void loadCustomTypeWithoutAttributes() {
        ConfigTypeRepository customRepo = findDescriptorIn(classpathFile("configTypes/withoutAttributes"), new DumpedFsReader());

        List<ConfigType> types = customRepo.getConfigTypes();

        assertEquals(1, types.size());
        ConfigType type = types.get(0);
        assertEquals("custom", type.getName());
        assertEquals("custom", type.getResultFileName());
        assertEquals(Set.of(".custom"), type.getSourceExtensions());
    }

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
