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

    @ParameterizedTest(name = "{0}, quote={1}, env block={2}")
    @MethodSource("quotedValueLayouts")
    void keepDirectivesInsideMultilineQuotedValues(String layout, String quote, boolean envBlock) throws IOException {
        String escaped = quote.equals("\"") ? "escaped \\\"quote\\\"" : "it''s quoted";
        String scalar = quote + "😀first\u2028continued\n" + escaped + "\n"
                + ".var host: literal\n.var without-delimiter\n#var legacy: literal\n"
                + "#include missing-quoted-component\n#@Ignore\n.var tail: closing" + quote;
        String node = switch (layout) {
            case "flow sequence" -> "[" + scalar.replace("\n", "\n  ") + "]";
            case "block sequence" -> "- " + scalar.replace("\n", "\n  ");
            case "mapping in sequence" -> "- message: " + scalar.replace("\n", "\n    ");
            default -> "{message: " + scalar.replace("\n", "\n  ") + "}";
        };
        String block = "text:\n" + node.lines().map(line -> "  " + line + "\n")
                .collect(java.util.stream.Collectors.joining()) + ".var outside: real\n";
        String content = "result: ${this@host:none}\ntail: ${this@tail:none}\nlegacy: ${this@legacy:none}\nactual: ${this@outside:none}\n"
                + (envBlock ? ".var outside: base\n+dev:\n" + block.lines().map(line -> "  " + line + "\n")
                .collect(java.util.stream.Collectors.joining()) : block);

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("none", properties.getPropertiesAsKeyValue().get("tail"));
        assertEquals("none", properties.getPropertiesAsKeyValue().get("legacy"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));
        Object parsed = new Yaml().load(properties.getPropertiesAsKeyValue().get("text"));
        String text;
        if (parsed instanceof Map<?, ?> map) text = (String) map.get("message");
        else {
            Object first = assertInstanceOf(List.class, parsed).get(0);
            text = first instanceof Map<?, ?> map ? (String) map.get("message") : (String) first;
        }
        assertTrue(text.contains(".var host: literal"));
        assertTrue(text.contains(".var without-delimiter"));
        assertTrue(text.contains("#include missing-quoted-component"));
        assertTrue(text.contains("#@Ignore"));
        assertTrue(text.contains(".var tail: closing"));
        if (envBlock) {
            Map<String, String> production = build(content, YAML, "prod").getPropertiesAsKeyValue();
            assertEquals("none", production.get("result"));
            assertEquals("base", production.get("actual"));
        }
    }

    @Test
    void keepQuotesInsideBlockScalarsSeparateFromQuotedScalars() throws IOException {
        String content = "items:\n"
                + "  - |\n    \"unclosed literal quote\n    .var host: literal\n"
                + "  - \"next\n    fake: |\n      literal text\n    .var quoted: literal\n    last\"\n"
                + ".var outside: real\n"
                + "result: ${this@host:none}\nquoted: ${this@quoted:none}\nactual: ${this@outside:none}\n";

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("none", properties.getPropertiesAsKeyValue().get("quoted"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));
        Map<?, ?> output = new Yaml().load(properties.save(asString()));
        List<?> items = assertInstanceOf(List.class, output.get("items"));
        assertTrue(((String) items.get(0)).contains("\"unclosed literal quote\n.var host: literal"));
        assertTrue(((String) items.get(1)).contains(".var quoted: literal"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "- "})
    void keepScalarHeaderLookalikesInsideFlowQuotes(String sequence) throws IOException {
        String content = "text:\n  " + sequence + "[\"first: |\n"
                + "      .var host: literal\n      last\"]\n"
                + ".var outside: real\nresult: ${this@host:none}\nactual: ${this@outside:none}\n";

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));
        List<?> parsed = new Yaml().load(properties.getPropertiesAsKeyValue().get("text"));
        Object scalar = sequence.isEmpty() ? parsed.get(0) : ((List<?>) parsed.get(0)).get(0);
        assertEquals("first: | .var host: literal last", scalar);
    }

    @Test
    void keepQuotedDirectivesAfterUnresolvedFlowPlaceholders() throws IOException {
        String content = "text:\n  [${this@missing:${this@alsoMissing:first}}, \"next\n"
                + "    .var host: literal\n    #include missing-quoted-component\n    last\"]\n"
                + ".var outside: real\nresult: ${this@host:none}\nactual: ${this@outside:none}\n";

        TypedProperties properties = build(content, YAML);
        assertEquals("none", properties.getPropertiesAsKeyValue().get("result"));
        assertEquals("real", properties.getPropertiesAsKeyValue().get("actual"));
        assertEquals(List.of("first", "next .var host: literal #include missing-quoted-component last"),
                new Yaml().load(properties.getPropertiesAsKeyValue().get("text")));
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

    private static Stream<Arguments> quotedValueLayouts() {
        return Stream.of("flow sequence", "block sequence", "mapping in sequence", "flow mapping")
                .flatMap(layout -> Stream.of("\"", "'").flatMap(quote -> Stream.of(false, true)
                        .map(envBlock -> Arguments.of(layout, quote, envBlock))));
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
