package io.microconfig.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.templates.TemplatesService.resolveTemplatesBy;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.StringUtils.toUnixPathSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplatesTest {
    @TempDir
    File destinationDir;

    @Test
    void testMustache() {
        File root = classpathFile("templates");
        Microconfig microconfig = searchConfigsIn(root).withDestinationDir(destinationDir);
        microconfig.environments().getOrCreateByName("dev")
                .getOrCreateComponentWithName("mustache")
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .forEachComponent(resolveTemplatesBy(microconfig.resolver()));

        assertEquals(
                toUnixPathSeparator(readFully(new File(root, "components/mustache/expect.dev"))).trim(),
                toUnixPathSeparator(readFully(new File(destinationDir, "mustache/template.yaml"))).trim()
        );
    }
}
