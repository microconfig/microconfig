package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateDefinitionTest {
    @Test
    void binaryTemplateWithoutSourceIsReportedAsIncomplete() {
        TemplateDefinition definition = new TemplateDefinition("file", "bin", defaultPattern());
        definition.setToFile("out.bin");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> definition.resolve(resolver(), properties()));
        assertTrue(e.getMessage().contains("Template error: templateName: 'bin'"));
        assertTrue(e.getMessage().contains("Incomplete template def"));
    }

    private Resolver resolver() {
        Resolver resolver = mock(Resolver.class);
        when(resolver.resolve(anyString(), any(), any())).thenReturn("result");
        return resolver;
    }

    private TypedProperties properties() {
        TypedProperties properties = mock(TypedProperties.class);
        when(properties.getDeclaringComponent()).thenReturn(mock(DeclaringComponent.class));
        return properties;
    }
}
