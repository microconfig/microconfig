package io.microconfig.core;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.TypedProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.serializers.PropertySerializers.asString;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectiveValueBoundariesTest {
    @TempDir
    Path root;

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("yamlScalarHeaders")
    void keepDirectivesInsideYamlSequenceScalars(String marker, String key) throws IOException {
        String header = key.isEmpty() ? "  - " + marker : "  - " + key + ": " + marker;
        String indent = key.isEmpty() ? "    " : "      ";
        String content = "items:\n" + header + "\n"
                + indent + ".var host: literal\n"
                + indent + ".var without-delimiter\n"
                + indent + "#var legacy: literal\n"
                + indent + "#include missing-literal-component\n"
                + indent + "#@Ignore\n"
                + indent + "echo done\n"
                + ".var outside: real\n"
                + "result: ${this@host:none}\n"
                + "actual: ${this@outside:none}\n";

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));

        Map<?, ?> output = new Yaml().load(properties.save(asString()));
        List<?> items = assertInstanceOf(List.class, output.get("items"));
        Object item = items.get(0);
        Object scalar = key.isEmpty() ? item : assertInstanceOf(Map.class, item).values().iterator().next();
        String text = assertInstanceOf(String.class, scalar);
        assertTrue(text.contains(".var host: literal"));
        assertTrue(text.contains(".var without-delimiter"));
        assertTrue(text.contains("#include missing-literal-component"));
        assertTrue(text.contains("#@Ignore"));
        assertTrue(text.contains("echo done"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"a: | # quoted text\"", "'a: | # quoted text'", "prefix |"})
    void keepRealDirectivesAfterTextThatIsNotAScalarHeader(String text) throws IOException {
        String content = "items:\n  - text: " + text + "\n"
                + "      .var outside: real\n"
                + "result: ${this@outside:none}\n";

        assertEquals("real", build(content, YAML).getPropertiesAsKeyValue().get("result"));
    }

    @Test
    void keepPlaceholderDefaultsOutOfScalarHeaderParsing() throws IOException {
        String content = "text:${this@missing: | # default text}\n"
                + "  .var outside: real\n"
                + "result: ${this@outside:none}\n";

        assertEquals("real", build(content, YAML).getPropertiesAsKeyValue().get("result"));
    }

    @Test
    void detectTheEndOfAnAutoIndentedLiteral() throws IOException {
        String content = "template: |\n"
                + "    .var host: literal\n"
                + "    echo done\n"
                + "  .var outside: real\n"
                + "result: ${this@host:none}\n"
                + "actual: ${this@outside:none}\n";

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));
        assertEquals(".var host: literal" + LINES_SEPARATOR + "echo done",
                properties.getPropertiesAsKeyValue().get("template"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void respectExplicitScalarIndentation(int indentation) throws IOException {
        for (boolean mapping : List.of(false, true)) {
            String header = mapping ? "  - text: |" : "  - |";
            String indent = " ".repeat((mapping ? 4 : 2) + indentation);
            String content = "items:\n" + header + indentation + "\n"
                    + indent + ".var host: literal\n"
                    + indent + "echo done\n"
                    + "result: ${this@host:none}\n";

            TypedProperties properties = build(content, YAML);
            assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
            assertTrue(properties.save(asString()).contains(".var host: literal"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"|", "|-", "|+", ">", ">-", ">+"})
    void readMappingScalarsUsingTheSameHeadersAsSequenceScalars(String header) throws IOException {
        String content = "template: " + header + "\n"
                + "  .var host: literal\n"
                + "  #var legacy: literal\n"
                + "  echo done\n"
                + ".var outside: real\n"
                + "result: ${this@host:none}\n"
                + "actual: ${this@outside:none}\n";

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));
        Map<?, ?> output = new Yaml().load(properties.save(asString()));
        String template = assertInstanceOf(String.class, output.get("template"));
        assertTrue(template.contains(".var host: literal"));
        assertTrue(template.contains("#var legacy: literal"));
        assertTrue(template.contains("echo done"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"&script |", "!!str |", "&script !!str >-"})
    void rejectStandaloneNodePropertiesRatherThanDroppingTheirMeaning(String header) {
        String content = "template: " + header + "\n  literal\ncopy: *script\n";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> build(content, YAML));
        assertTrue(error.getMessage().contains("anchors and tags"));
    }

    @Test
    void readScalarHeadersWithLongCommentsAndUnicodeSeparators() throws IOException {
        String content = "template: | #" + " ".repeat(10000) + "\u2028\n"
                + "  .var host: literal\n"
                + "  echo done\n"
                + "result: ${this@host:none}\n";

        assertEquals("none", build(content, YAML).getPropertiesAsKeyValue().get("result"));
    }

    @ParameterizedTest(name = "{2}, env block={3}: {0}")
    @MethodSource("flowCollections")
    void keepDirectivesOutOfFlowCollectionValues(String value, Object expected, String directive, boolean envBlock) throws IOException {
        String block = "section:\n  values:\n"
                + value.lines().map(line -> "    " + line + "\n").collect(java.util.stream.Collectors.joining())
                + "  " + directive + " host: local\n";
        String content = "answer: ${this@host:none}\n" + (envBlock
                ? "+dev:\n" + block.lines().map(line -> "  " + line + "\n").collect(java.util.stream.Collectors.joining())
                : block);

        TypedProperties properties = build(content, YAML);
        assertEquals("local", properties.getPropertiesAsKeyValue().get("answer"));
        Object collection = new Yaml().load(properties.getPropertiesAsKeyValue().get("section.values"));
        assertEquals(expected, collection);
        if (envBlock) assertEquals("none", build(content, YAML, "prod").getPropertiesAsKeyValue().get("answer"));
    }

    @Test
    void keepOneLineFlowCollectionsAsCollectionsAfterDirectives() throws IOException {
        String content = "section:\n  values:\n    [one, two]\n  .var host: local\nanswer: ${this@host:none}\n";

        Map<?, ?> output = new Yaml().load(build(content, YAML).save(asString()));
        Map<?, ?> section = assertInstanceOf(Map.class, output.get("section"));
        assertEquals(List.of("one", "two"), section.get("values"));
        assertEquals("local", output.get("answer"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[one,\n  two]", "{one: 1,\n  two: 2}"})
    void keepMultilineScalarTextDistinctFromFlowCollections(String text) throws IOException {
        String content = "text: |-\n" + text.lines().map(line -> "  " + line + "\n")
                .collect(java.util.stream.Collectors.joining());

        Map<?, ?> output = new Yaml().load(build(content, YAML).save(asString()));
        assertInstanceOf(String.class, output.get("text"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ".var:\n  .var:\n", ".var:\n  .var:\n    x: 1\n",
            "+dev:\n  .var:\n    .var:\n", "datasource:\n  .var:\n"
    })
    void rejectNestedVarBlocksEvenWhenEmpty(String content) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> build(content, YAML));
        assertTrue(error.getMessage().contains("must be at the top level"));
    }

    @Test
    void allowEmptyVarBlocksAtTheirSupportedLevels() throws IOException {
        TypedProperties properties = build("a: 1\n.var:\n+dev:\n  .var:\n", YAML);

        assertEquals(Map.of("a", "1"), properties.getPropertiesAsKeyValue());
    }

    @ParameterizedTest(name = "{0} backslashes, {2}, continuation={3}")
    @MethodSource("propertiesContinuations")
    void distinguishEscapedBackslashesFromPropertiesContinuations(int slashes, boolean continued, String directive, boolean alreadyContinued) throws IOException {
        String prefix = alreadyContinued ? "path=first\\\n  next" : "path=C:";
        String content = prefix + "\\".repeat(slashes) + "\n"
                + directive + " host=outside\n"
                + "result=${this@host:none}\n";

        TypedProperties properties = build(content, PROPERTIES);
        assertEquals(continued ? "none" : "outside", properties.getPropertiesAsKeyValue().get("result"));
        String path = (alreadyContinued ? "first\\" + LINES_SEPARATOR + "next" : "C:") + "\\".repeat(slashes);
        if (continued) path += LINES_SEPARATOR + directive + " host=outside";
        assertEquals(path, properties.getPropertiesAsKeyValue().get("path"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"first\\  \n", "first\\\n\n", "first\\\n   \n"})
    void terminatePropertiesValuesBeforeTheNextRealDirective(String value) throws IOException {
        String content = "path=" + value + ".var host=outside\nresult=${this@host:none}\n";
        java.util.Properties expected = new java.util.Properties();
        expected.load(new StringReader(content));

        TypedProperties properties = build(content, PROPERTIES);
        assertEquals("outside", properties.getPropertiesAsKeyValue().get("result"));
        java.util.Properties actual = new java.util.Properties();
        actual.load(new StringReader(properties.save(asString())));
        assertEquals(expected.getProperty("path"), actual.getProperty("path"));
        assertEquals("outside", actual.getProperty("result"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"path=first\\", "path=first\\\\\\", "path=first\\\n  last\\"})
    void finishPropertiesContinuationsAtEndOfFile(String content) throws IOException {
        java.util.Properties expected = new java.util.Properties();
        expected.load(new StringReader(content));

        TypedProperties properties = build(content, PROPERTIES);
        java.util.Properties actual = new java.util.Properties();
        actual.load(new StringReader(properties.save(asString())));
        assertEquals(expected.getProperty("path"), actual.getProperty("path"));
    }

    private TypedProperties build(String content, ConfigFormat format) throws IOException {
        return build(content, format, "dev");
    }

    private TypedProperties build(String content, ConfigFormat format, String environment) throws IOException {
        Files.createDirectories(root.resolve("envs"));
        Path component = Files.createDirectories(root.resolve("components/app"));
        Files.writeString(component.resolve("application" + format.extension()), content);
        Microconfig microconfig = searchConfigsIn(root.toFile());
        return microconfig.environments().getOrCreateByName(environment)
                .findComponentWithName("app")
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .first();
    }

    private static Stream<Arguments> flowCollections() {
        return Stream.of(
                        new Object[]{"[one, two]", List.of("one", "two")},
                        new Object[]{"[one,\n  two]", List.of("one", "two")},
                        new Object[]{"[one,\n# comment\n  two]", List.of("one", "two")},
                        new Object[]{"[it's, okay]", List.of("it's", "okay")},
                        new Object[]{"[\"😀😀😀😀\", two]", List.of("😀😀😀😀", "two")},
                        new Object[]{"[\"first\n  .var literalOnly: text\n  last\"]", List.of("first .var literalOnly: text last")},
                        new Object[]{"{one: 1, two: 2}", Map.of("one", 1, "two", 2)},
                        new Object[]{"{one: 1,\n  two: 2}", Map.of("one", 1, "two", 2)},
                        new Object[]{"[\"${this@missing:one}\", # comment\n  two]", List.of("one", "two")}
                )
                .flatMap(pair -> Stream.of(".var", "#var")
                        .flatMap(directive -> Stream.of(false, true)
                                .map(envBlock -> Arguments.of(pair[0], pair[1], directive, envBlock))));
    }

    private static Stream<Arguments> yamlScalarHeaders() {
        return Stream.of("|", "|-", "|+", ">", ">-", ">+", "|2", "|2-", "|-2", ">2+", ">+2", "| # literal",
                        "&script |", "!!str |", "&script !!str |", "!!str &script >-")
                .flatMap(marker -> Stream.of("", "text", "\"text:key\"", "&key text", "!!str text")
                        .map(key -> Arguments.of(marker, key)));
    }

    private static Stream<Arguments> propertiesContinuations() {
        return Stream.of(new Object[]{0, false}, new Object[]{1, true}, new Object[]{2, false},
                        new Object[]{3, true}, new Object[]{4, false}, new Object[]{5, true}, new Object[]{6, false})
                .flatMap(pair -> Stream.of(".var", "#var")
                        .flatMap(directive -> Stream.of(false, true)
                                .map(alreadyContinued -> Arguments.of(pair[0], pair[1], directive, alreadyContinued))));
    }
}
