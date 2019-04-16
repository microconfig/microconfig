package io.microconfig.entry.factory.configtypes;

import io.microconfig.entry.factory.ConfigType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTypeFileProviderTest {
    private ConfigTypeFileProvider configTypeFileProvider = new ConfigTypeFileProvider();

    @Test
    void testParse() {
        List<ConfigType> actual = configTypeFileProvider.getConfigTypes(classpathFile("configTypes"));
        List<ConfigType> expected = StandardConfigTypes.asProvider().getConfigTypes(null);
        assertEquals(actual.size(), expected.size());

        for (int i = 0; i < expected.size(); i++) {
            ConfigType e = expected.get(i);
            ConfigType a = actual.get(i);
            assertEquals(e.getType(), a.getType());
            assertEquals(e.getResultFileName(), a.getResultFileName());
            assertEquals(e.getSourceExtensions(), a.getSourceExtensions());
        }
    }
}