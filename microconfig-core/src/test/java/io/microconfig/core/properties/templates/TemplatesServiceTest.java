package io.microconfig.core.properties.templates;

import io.microconfig.core.MicroconfigRunner;
import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.templates.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.io.properties.PropertiesText.escape;
import static io.microconfig.core.properties.serializers.PropertySerializers.asConfigResult;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplatesServiceTest {
    @TempDir
    Path root;

    @BeforeEach
    void createEnvironment() throws IOException {
        Files.createDirectories(root.resolve("envs"));
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [target]\n");
    }

    @ParameterizedTest
    @MethodSource("formatsAndNotations")
    void decodeTemplatePathsBeforeParsing(ConfigFormat format, String notation, String type) throws IOException {
        String directory = format == YAML ? "dir # files" : "dir\\files";
        Path source = root.resolve(directory).resolve("template.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "hello");
        String sourcePath = "${this@configRoot}/" + sourceText(directory, format) + "/template.txt";
        String destinationPath = "output " + sourceText(directory, format) + "/${this@name}.txt";
        writeComponent(format, templateDefinition(format, notation, type, sourcePath, destinationPath));

        List<Template> templates = buildTemplates();

        assertEquals(notation.equals("brackets") ? 2 : 1, templates.size());
        File expectedDestination = destinationFile("output " + directory + "/target.txt").toFile();
        for (Template template : templates) {
            assertEquals(source.toFile().getCanonicalFile(), template.getSource().getCanonicalFile());
            assertEquals(expectedDestination.getCanonicalFile(), template.getDestination().getCanonicalFile());
            assertEquals("hello", template.getContent());
        }
        assertEquals("hello", Files.readString(expectedDestination.toPath()));
    }

    @Test
    void keepQuotesInLiteralYamlDestination() throws IOException {
        Files.writeString(root.resolve("template.txt"), "hello");
        writeComponent(YAML, "mc.template.example.fromFile: ${this@configRoot}/template.txt\n"
                + "mc.template.example.toFile: |-\n  'result.txt'\n");

        List<Template> templates = buildTemplates();

        assertEquals("'result.txt'", templates.get(0).getFileName());
        assertEquals("hello", Files.readString(destinationFile("'result.txt'")));
    }

    @Test
    void decodePropertiesPathEscapedDuringPlaceholderResolution() throws IOException {
        Path source = root.resolve("dir\\files/template.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "hello");
        writeComponent("pathSource", YAML, "path: |-\n  " + source + "\n");
        writeComponent(PROPERTIES, "mc.template.example.fromFile=${pathSource@path}\n"
                + "mc.template.example.toFile=result.txt\n");

        List<Template> templates = buildTemplates();

        assertEquals(source.toFile().getCanonicalFile(), templates.get(0).getSource().getCanonicalFile());
        assertEquals("hello", Files.readString(destinationFile("result.txt")));
    }

    private String sourceText(String text, ConfigFormat format) {
        return format == PROPERTIES ? escape(text) : text;
    }

    private String templateDefinition(ConfigFormat format, String notation, String type, String from, String to) {
        String separator = format == YAML ? ": " : "=";
        String prefix = "mc." + type + ".";
        return switch (notation) {
            case "fromTo" -> prefix + "example.fromFile" + separator + from + "\n"
                    + prefix + "example.toFile" + separator + to + "\n";
            case "arrow" -> prefix + "example" + separator + from + " -> " + to + "\n";
            case "brackets" -> prefix + "[first,second]" + separator + from + " -> " + to + "\n";
            default -> throw new IllegalArgumentException(notation);
        };
    }

    private void writeComponent(ConfigFormat format, String content) throws IOException {
        writeComponent("target", format, content);
    }

    private void writeComponent(String component, ConfigFormat format, String content) throws IOException {
        Path file = root.resolve("components/" + component + "/application" + format.extension());
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private List<Template> buildTemplates() {
        MicroconfigRunner runner = new MicroconfigRunner(root.toFile(), root.resolve("build").toFile());
        Properties properties = runner.buildProperties("dev", List.of(), List.of());
        properties.save(runner.toFiles());
        return properties.save(asConfigResult()).stream()
                .flatMap(result -> result.getTemplates().stream())
                .toList();
    }

    private Path destinationFile(String relativePath) {
        return root.resolve("build/target").resolve(relativePath);
    }

    private static Stream<Arguments> formatsAndNotations() {
        return Stream.of(YAML, PROPERTIES)
                .flatMap(format -> Stream.of("fromTo", "arrow", "brackets")
                        .flatMap(notation -> Stream.of("template", "mustache", "file")
                                .map(type -> Arguments.of(format, notation, type))));
    }
}
