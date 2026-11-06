package io.microconfig.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlFlowValuesTest {
    @TempDir
    Path root;

    @BeforeEach
    void createEnvironment() throws IOException {
        Files.createDirectories(root.resolve("envs"));
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [target]\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello, world", "left[right]", "left{right}", "a: b", "🙂, cat"})
    void composedYamlStringsStaySingleFlowValues(String text) throws IOException {
        writeComponent("source", "yaml", "value: '" + text + "'\n");
        writeComponent("target", "yaml", "items: [${source@value}${missing@value:}]\n"
                + "mapping: [{value: ${source@value}${missing@value:}}]\n");

        assertEquals(Map.of("items", List.of(text), "mapping", List.of(Map.of("value", text))), build("target"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello, world", "left[right]", "left{right}", "🙂, cat"})
    void propertiesTextStaysSingleFlowValue(String text) throws IOException {
        writeComponent("source", "properties", "value=" + text + "\n");
        writeComponent("target", "yaml", "items: [${source@value}]\n");

        assertEquals(Map.of("items", List.of(text)), build("target"));
    }

    @Test
    void keepsNativeTypesAlongsideComputedFlowText() throws IOException {
        writeComponent("target", "yaml", "items: [#{2 + 3}, #{true}, #{'hello, world'}, [one, two]]\n");

        assertEquals(List.of(5, true, "hello, world", List.of("one", "two")), build("target").get("items"));
    }

    private void writeComponent(String component, String extension, String text) throws IOException {
        Path file = root.resolve("components/" + component + "/application." + extension);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private Map<?, ?> build(String component) throws IOException {
        Path destination = root.resolve("build");
        new MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", List.of(), List.of());
        return new Yaml().load(Files.readString(destination.resolve(component + "/application.yaml")));
    }
}
