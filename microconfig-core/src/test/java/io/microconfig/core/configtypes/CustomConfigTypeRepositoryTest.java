package io.microconfig.core.configtypes;

import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.core.configtypes.CustomConfigTypeRepository.findDescriptorIn;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomConfigTypeRepositoryTest {
    @Test
    void loadCustomFilesFromFile() {
        ConfigTypeRepository customRepo = findDescriptorIn(classpathFile("configTypes"), new DumpedFsReader());

        List<ConfigType> expected = new StandardConfigTypeRepository().getConfigTypes();
        List<ConfigType> actual = customRepo.getConfigTypes();

        checkConfigTypes(expected, actual);
    }

    @Test
    void loadTypesUsingIncludeFromFile() {
        ConfigTypeRepository customRepo = findDescriptorIn(classpathFile("configTypes/withIncludeAndDefault"), new DumpedFsReader());

        List<ConfigType> expected = new ArrayList<>();
        expected.add(byNameAndExtensions("custom", new HashSet<>(singletonList(".cst")), "result"));
        expected.addAll(new StandardConfigTypeRepository().getConfigTypes());
        List<ConfigType> actual = customRepo.getConfigTypes();

        checkConfigTypes(expected, actual);
    }

    private void checkConfigTypes(List<ConfigType> expected, List<ConfigType> actual) {
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