package io.microconfig.environments.filebased;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnvironmentParserSelectorImplTest {
    private EnvironmentParser json = mock(EnvironmentParser.class);
    private EnvironmentParser yaml = mock(EnvironmentParser.class);

    private EnvironmentParserSelector selector = new EnvironmentParserSelectorImpl(json, yaml);

    @Test
    void testSelectParser() {
        assertEquals(json, selector.selectParser(new File("envs/prod.json")));
        assertEquals(yaml, selector.selectParser(new File("envs/prod.yaml")));
        assertThrows(IllegalArgumentException.class, () -> selector.selectParser(new File("envs/prod.txt")));
    }

    @Test
    void testSupportedFormats() {
        assertEquals(asList(".json", ".yaml"), selector.supportedFormats());
    }
}