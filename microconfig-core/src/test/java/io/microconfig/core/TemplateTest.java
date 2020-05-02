package io.microconfig.core;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.templates.TemplatesService.resolveTemplatesBy;

public class TemplateTest {
    Microconfig microconfig = searchConfigsIn(classpathFile("templates"));

    @Test
    void testMustache() {
        microconfig.environments().getOrCreateByName("dev")
                .getOrCreateComponentWithName("mustache")
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .forEachComponent(resolveTemplatesBy(microconfig.resolver()));
    }
}
