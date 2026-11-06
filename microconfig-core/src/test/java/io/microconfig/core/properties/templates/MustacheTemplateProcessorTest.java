package io.microconfig.core.properties.templates;

import io.microconfig.core.Microconfig;
import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.TypedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MustacheTemplateProcessorTest {
    @TempDir
    Path root;

    @BeforeEach
    void createEnvironment() throws IOException {
        Files.createDirectories(root.resolve("envs"));
        Files.writeString(root.resolve("envs/dev.yaml"), "portOffset: 1\n");
    }

    @Test
    void renderYamlPathReferencedByPropertiesAsPlainText() throws IOException {
        writeComponent("source", YAML, "path: C:\\temp\\new\n");
        writeComponent("target", PROPERTIES, "path=${source@path}\n");

        String result = render("target", "{{{path}}}");

        assertEquals("C:\\temp\\new", result);
    }

    @Test
    void decodePropertiesEscapesBeforeRendering() throws IOException {
        writeComponent("target", PROPERTIES,
                "path=C:\\\\temp\\\\new\n"
                        + "line=first\\nsecond\n"
                        + "color=\\#ff0000\n"
                        + "quoted=\\\"quoted\\\"\n"
                        + "unicode=\\u0041\n");

        String result = render("target", "{{{path}}}|{{{line}}}|{{{color}}}|{{{quoted}}}|{{unicode}}");

        assertEquals("C:\\temp\\new|first\nsecond|#ff0000|\"quoted\"|A", result);
    }

    @Test
    void preserveLiteralYamlBlockContents() throws IOException {
        writeComponent("target", YAML, "code: |-\n  \"left\" + \"right\"\n");

        String result = render("target", "{{{code}}}");

        assertEquals("\"left\" + \"right\"", result);
    }

    @Test
    void keepTemporaryPropertiesAvailableToTemplates() throws IOException {
        writeComponent("target", PROPERTIES, "#var path=C:\\\\temp\\\\new\n");

        String result = render("target", "{{{path}}}");

        assertEquals("C:\\temp\\new", result);
    }

    @Test
    void renderYamlTemporaryPropertiesAsPlainText() throws IOException {
        writeComponent("target", YAML, "#var color: #ff0000\n");

        String result = render("target", "{{{color}}}");

        assertEquals("#ff0000", result);
    }

    private void writeComponent(String component, ConfigFormat format, String text) throws IOException {
        Path file = root.resolve("components/" + component + "/application" + format.extension());
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private String render(String component, String template) {
        TypedProperties properties = resolveProperties(component);
        return new MustacheTemplateProcessor().process("mustache", new File("template.mustache"), template, properties);
    }

    private TypedProperties resolveProperties(String component) {
        Microconfig microconfig = searchConfigsIn(root.toFile());
        return microconfig.environments().getOrCreateByName("dev")
                .findComponentWithName(component)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver()).first();
    }
}
