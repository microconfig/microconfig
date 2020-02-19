package io.microconfig.core.properties.serializer;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ioservice.ConfigIoService;
import io.microconfig.core.properties.io.ioservice.ConfigWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToFileConfigSerializerTest {
    @Mock
    private FilenameGenerator filenameGenerator;
    @Mock
    private ConfigIoService configIoService;
    private ToFileConfigSerializer toFileConfigSerializer;

    private String c1 = "c1";
    private String env = "env";
    private File expected = new File("expected");
    @Mock
    private List<Property> properties;


    @BeforeEach
    void setUp() {
        when(filenameGenerator.fileFor(c1, env, properties)).thenReturn(expected);
        toFileConfigSerializer = new ToFileConfigSerializer(filenameGenerator, configIoService);
    }

    @Test
    void testSerialize() {
        when(properties.isEmpty()).thenReturn(false);
        ConfigWriter configWriter = Mockito.mock(ConfigWriter.class);
        when(configIoService.writeTo(expected)).thenReturn(configWriter);

        Optional<File> result = toFileConfigSerializer.serialize(c1, env, properties);
        verify(configWriter).write(properties);
        assertSame(expected, result.get());
    }

    @Test
    void configDestination() {
        File result = toFileConfigSerializer.configDestination(c1, env, properties);
        assertSame(expected, result);
    }
}