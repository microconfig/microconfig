package io.microconfig.core.properties.serializers;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.io.properties.PropertiesSyntax.escapeInsertedText;
import static io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDiffTest {
    @TempDir
    Path dir;

    @Test
    void noDiffAfterRebuildWithTheSameYamlValues() {
        File file = build("application.yaml", List.of(
                prop("multiline", "first" + LINES_SEPARATOR + "second", YAML),
                prop("indented", "  a" + LINES_SEPARATOR + "b", YAML),
                prop("backslashes", "C:\\temp\\new", YAML),
                prop("shellContinuation", "docker run \\" + LINES_SEPARATOR + "  --rm img", YAML),
                prop("simple", "x", YAML)
        ));

        assertFalse(diffFile(file).exists());
    }

    // .properties values as they reach the writer: written in the source, or inserted from YAML and escaped by the resolver
    @Test
    void noDiffAfterRebuildWithTheSamePropertiesValues() {
        File file = build("application.properties", List.of(
                prop("multiline", "first" + LINES_SEPARATOR + "second", PROPERTIES),
                prop("indented", "  a", PROPERTIES),
                prop("insertedFromYaml", escapeInsertedText("C:\\temp\\new" + LINES_SEPARATOR + "docker run \\"), PROPERTIES),
                prop("escapedInSource", "a\\\\b", PROPERTIES),
                prop("continuation", "1\\" + LINES_SEPARATOR + "2", PROPERTIES),
                prop("simple", "x", PROPERTIES)
        ));

        assertFalse(diffFile(file).exists());
    }

    @Test
    void storeDiffOfChangedValues() throws IOException {
        File file = build("application.properties", List.of(prop("multiline", "first" + LINES_SEPARATOR + "second", PROPERTIES)));
        new ConfigDiff().storeDiffFor(file, List.of(prop("multiline", "first" + LINES_SEPARATOR + "changed", PROPERTIES)));

        assertTrue(Files.readString(diffFile(file).toPath()).contains("first\\nsecond -> first\\nchanged"));
    }

    private File build(String fileName, List<Property> properties) {
        File file = dir.resolve(fileName).toFile();
        configIo().writeTo(file).write(properties);
        new ConfigDiff().storeDiffFor(file, properties);
        return file;
    }

    private File diffFile(File file) {
        return new File(file.getParentFile(), "diff-" + file.getName());
    }

    private Property prop(String key, String value, ConfigFormat format) {
        return property(key, value, format, null);
    }
}
