package io.microconfig.core;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.ResolveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesYamlBoundariesTest {
    @TempDir
    Path root;

    @BeforeEach
    void createEnvironment() throws IOException {
        Files.createDirectories(root.resolve("envs"));
        Files.writeString(root.resolve("envs/dev.yaml"), "portOffset: 1\n");
    }

    @Test
    void resolveGroupChildrenBeforeSerializingYaml() throws IOException {
        writeComponent("source", PROPERTIES,
                "db.value=#{\"abc\"}\n"
                        + "db.regex=#{#findGroup('\\\\d+', 'a123')}\n"
                        + "db.path=#{'C:\\\\temp'}\n");
        writeComponent("target", PROPERTIES, "value=${source@db.*}\n");

        String fragment = readProperties(build("target")).getProperty("value");

        assertEquals(Map.of("value", "abc", "regex", 123, "path", "C:\\temp"), readYaml(fragment));
    }

    @Test
    void keepMultilineTextStartingWithDashInGroup() throws IOException {
        writeComponent("source", PROPERTIES, "db.value=- first\\nsecond\n");
        writeComponent("target", PROPERTIES, "value=${source@db.*}\n");

        String fragment = readProperties(build("target")).getProperty("value");

        assertEquals("- first\nsecond", readYaml(fragment).get("value"));
    }

    @Test
    void resolveGroupChildrenWithRootOverrides() throws IOException {
        writeComponent("source", PROPERTIES, "setting=default\ndb.value=${this@setting}\n");
        writeComponent("target", PROPERTIES, "setting=override\nvalue=${source@db.*}\n");

        String fragment = readProperties(build("target")).getProperty("value");

        assertEquals("override", readYaml(fragment).get("value"));
    }

    @Test
    void detectCyclesThroughGroupChildren() throws IOException {
        writeComponent("source", PROPERTIES, "db.value=${source@db.*}\n");
        writeComponent("target", PROPERTIES, "value=${source@db.*}\n");

        assertThrows(ResolveException.class, () -> build("target"));
    }

    @Test
    void decodePropertiesExpressionBeforeEvaluation() throws IOException {
        writeComponent("source", PROPERTIES,
                "path=#{'C:\\\\temp'}\nregex=#{#findGroup('\\\\d+', 'a123')}\n");

        Properties result = readProperties(build("source"));

        assertEquals("C:\\temp", result.getProperty("path"));
        assertEquals("123", result.getProperty("regex"));
    }

    @Test
    void insertPropertiesTextNormallyInsideExpression() throws IOException {
        writeComponent("source", YAML, "path: C:\\temp\\new\n");
        writeComponent("bridge", PROPERTIES, "path=${source@path}\n");
        writeComponent("target", PROPERTIES,
                "direct=#{'${source@path}'.length()}\nviaBridge=#{'${bridge@path}'.length()}\n");

        Properties result = readProperties(build("target"));

        assertEquals("11", result.getProperty("direct"));
        assertEquals("11", result.getProperty("viaBridge"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "#{${missing@value:'abc'}.length()}",
            "#{${source@value}.length()}",
            "#{'${source@value}'.length()}",
            "#{#{${source@value}.length()} + 0}"
    })
    void evaluateYamlStringArgumentsBeforeSerializingExpressionResult(String expression) throws IOException {
        writeComponent("source", YAML, "value: 'abc'\n");
        writeComponent("target", YAML,
                "value: " + expression + "\nitems: [{value: " + expression + "}]\n");

        assertEquals(Map.of("value", 3, "items", List.of(Map.of("value", 3))), readYaml(build("target")));
    }

    @Test
    void keepExpressionResultTypesIndependentOfTheirStringArguments() throws IOException {
        writeComponent("source", YAML, "value: '123'\n");
        writeComponent("target", YAML,
                "number: #{${source@value}.length()}\n"
                        + "flag: #{${source@value} == '123'}\n"
                        + "text: \"#{${source@value}.length()}\"\n"
                        + "composed: prefix #{${source@value}.length()} #{2 + 3}\n"
                        + "second: #{2 + 3} #{${source@value}.length()}\n");

        assertEquals(Map.of("number", 3, "flag", true, "text", "3", "composed", "prefix 3 5", "second", "5 3"),
                readYaml(build("target")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#{${source@value}.length()}", "#{'${source@value}'.length()}", "#{\"${source@value}\".length()}"})
    void preserveQuoteCharactersInYamlExpressionArguments(String expression) throws IOException {
        writeComponent("source", YAML, "value: 'O''Reilly \"book\"'\n");
        writeComponent("target", YAML, "value: " + expression + "\n");

        assertEquals("O'Reilly \"book\"".length(), readYaml(build("target")).get("value"));
    }

    @Test
    void keepHashSuffixInResolvedValues() throws IOException {
        writeComponent("target", YAML, "value: #{2 + 3} # keep\n");

        assertEquals("5 # keep", readYaml(build("target")).get("value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "true", "a: b"})
    void resolvePlaceholdersProducedByExpressionsAsYamlValues(String text) throws IOException {
        writeComponent("source", YAML, "value: '" + text + "'\n");
        String expression = "#{'%c%csource@value%c'.formatted(36,123,125)}";
        writeComponent("target", YAML, "value: " + expression + "\nitems: [" + expression + "]\n");

        assertEquals(Map.of("value", text, "items", List.of(text)), readYaml(build("target")));
    }

    @Test
    void keepYamlStringTypeAfterEmptyPrefix() throws IOException {
        writeComponent("source", YAML, "value: '123'\n");
        writeComponent("target", YAML, "value: ${missing@value:}${source@value}\n");

        assertEquals("123", readYaml(build("target")).get("value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\#ff0000", "b\\: c", "\\\"quoted\\\"", "null", "- first\\nsecond", "\\ leading", "C:\\\\temp"})
    void quotePlainPropertiesTextInsertedIntoYaml(String value) throws IOException {
        writeComponent("source", PROPERTIES, "value=" + value + "\n");
        writeComponent("target", YAML, "value: ${source@value}\n");

        Object result = readYaml(build("target")).get("value");

        assertEquals(readProperties("value=" + value).getProperty("value"), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\#ff0000", "b\\: c", "\\\"quoted\\\""})
    void quoteTheWholeComposedYamlScalar(String value) throws IOException {
        writeComponent("source", PROPERTIES, "value=" + value + "\n");
        writeComponent("target", YAML, "value: prefix ${source@value} suffix\n");

        Object result = readYaml(build("target")).get("value");

        assertEquals("prefix " + readProperties("value=" + value).getProperty("value") + " suffix", result);
    }

    @Test
    void preserveQuotesAndBackslashesInsideQuotedYamlScalar() throws IOException {
        writeComponent("source", PROPERTIES, "value=\\\"C:\\\\temp\\\"\n");
        writeComponent("target", YAML, "value: \"prefix ${source@value} suffix\"\n");

        Object result = readYaml(build("target")).get("value");

        assertEquals("prefix \"C:\\temp\" suffix", result);
    }

    @Test
    void preserveQuotesProducedByExpressionInQuotedYaml() throws IOException {
        writeComponent("target", YAML, "value: \"prefix #{'a' + '\\\"' + 'b'} suffix\"\n");

        assertEquals("prefix a\"b suffix", readYaml(build("target")).get("value"));
    }

    @Test
    void keepYamlTemporaryValuesAsPlainText() throws IOException {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n");
        writeComponent("target", YAML, "#var alias: ${source@value}\nvalue: ${this@alias}\n");

        assertEquals("#ff0000", readYaml(build("target")).get("value"));
    }

    @Test
    void keepEmptyExpressionAndDefaultAsStrings() throws IOException {
        writeComponent("target", YAML, "expression: #{''}\ndefault: ${missing@value:}\n");

        assertEquals(Map.of("expression", "", "default", ""), readYaml(build("target")));
    }

    @Test
    void keepNativeYamlCollectionsAndQuotedStrings() throws IOException {
        writeComponent("source", YAML, "list:\n  - one\n  - two\ntext: 'a: b'\n");
        writeComponent("target", YAML, "list: ${source@list}\ntext: ${source@text}\n");

        assertEquals(Map.of("list", List.of("one", "two"), "text", "a: b"), readYaml(build("target")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "true", "null", "1.5", "yes", "010", "#tag", "a: b"})
    void preserveTheTypeOfReferencedYamlStrings(String value) throws IOException {
        writeComponent("source", YAML, "value: '" + value + "'\n");
        writeComponent("target", YAML, "value: ${source@value}\n");

        assertEquals(value, readYaml(build("target")).get("value"));
    }

    @Test
    void keepReferencedYamlStringsInsideCollections() throws IOException {
        writeComponent("source", YAML, "color: '#tag'\nnumber: '123'\nflag: 'true'\ntext: 'a: b'\n");
        writeComponent("target", YAML,
                "block:\n  - ${source@color}\n  - ${source@number}\n  - ${source@flag}\n  - ${source@text}\n"
                        + "flow: [${source@color}, ${source@number}, ${source@flag}, ${source@text}]\n");

        List<String> expected = List.of("#tag", "123", "true", "a: b");
        assertEquals(Map.of("block", expected, "flow", expected), readYaml(build("target")));
    }

    @Test
    void keepQuotedStringTypeWhenComposedWithEmptyDefault() throws IOException {
        writeComponent("source", YAML, "value: '123'\n");
        writeComponent("target", YAML, "value: ${source@value}${missing@value:}\n");

        assertEquals("123", readYaml(build("target")).get("value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"quoted\"", "\"left\" + \"right\"", "'quoted'", "123", "#tag", "[a, b]", "- item"})
    void preserveLiteralBlockContentsInProperties(String value) throws IOException {
        writeComponent("source", YAML, "value: |-\n  " + value + "\n");
        writeComponent("target", PROPERTIES, "value=${source@value}\n");

        assertEquals(value, readProperties(build("target")).getProperty("value"));
    }

    @Test
    void preserveLiteralBlockContentsInYamlAndLists() throws IOException {
        writeComponent("source", YAML, "code: |-\n  \"left\" + \"right\"\nnumber: |-\n  123\n");
        writeComponent("target", YAML,
                "code: ${source@code}\nnumber: ${source@number}\nlist: [${source@code}, ${source@number}]\n");

        String code = "\"left\" + \"right\"";
        assertEquals(Map.of("code", code, "number", "123", "list", List.of(code, "123")), readYaml(build("target")));
        assertEquals(Map.of("code", code, "number", "123"), readYaml(build("source")));
    }

    @Test
    void keepLiteralBlockStyleInEnvironmentOverrides() throws IOException {
        writeComponent("source", YAML, "+dev:\n  value: |-\n    \"quoted\"\n");
        writeComponent("target", PROPERTIES, "value=${source@value}\n");

        assertEquals("\"quoted\"", readProperties(build("target")).getProperty("value"));
    }

    @Test
    void preserveLiteralTextInsideGroupFragments() throws IOException {
        writeComponent("source", YAML, "db:\n  code: |-\n    \"left\" + \"right\"\n  number: |-\n    123\n");
        writeComponent("target", PROPERTIES, "value=${source@db.*}\n");

        String fragment = readProperties(build("target")).getProperty("value");

        assertEquals(Map.of("code", "\"left\" + \"right\"", "number", "123"), readYaml(fragment));
    }

    @Test
    void distinguishLiteralQuotesFromYamlQuotesInProperties() throws IOException {
        writeComponent("source", YAML, "quoted: '123'\nliteral: |-\n  \"123\"\n");
        writeComponent("target", PROPERTIES, "quoted=${source@quoted}\nliteral=${source@literal}\n");

        Properties result = readProperties(build("target"));

        assertEquals("123", result.getProperty("quoted"));
        assertEquals("\"123\"", result.getProperty("literal"));
    }

    @Test
    void preserveYamlStructuresInDefaultValues() throws IOException {
        writeComponent("target", YAML,
                "list: ${missing@value:[a, b]}\nmap: ${missing@value:{a: b}}\n"
                        + "text: \"${missing@value:[a, b]}\"\nnumber: ${missing@value:'123'}\n");

        assertEquals(Map.of("list", List.of("a", "b"), "map", Map.of("a", "b"),
                "text", "[a, b]", "number", "123"), readYaml(build("target")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\\\temp\\\\new", "\\\"quoted\\\"", "\\#ff0000", "b\\: c", "O'Reilly", "first\\nsecond", "123", "true"})
    void preservePropertiesTextInQuotedCollectionElements(String value) throws IOException {
        writeComponent("source", PROPERTIES, "value=" + value + "\n");
        writeComponent("target", YAML,
                "flow: [\"${source@value}\"]\nblock:\n  - \"${source@value}\"\n"
                        + "single: ['${source@value}']\n");

        String expected = readProperties("value=" + value).getProperty("value");
        assertEquals(Map.of("flow", List.of(expected), "block", List.of(expected), "single", List.of(expected)),
                readYaml(build("target")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\\\temp\\\\new", "\\\"quoted\\\"", "\\#ff0000", "b\\: c", "first\\nsecond"})
    void quotePropertiesTextInsideNestedCollections(String value) throws IOException {
        writeComponent("source", PROPERTIES, "value=" + value + "\n");
        writeComponent("target", YAML,
                "flow: [{value: ${source@value}}]\nblock:\n  - value: ${source@value}\n");

        List<Map<String, String>> expected = List.of(Map.of("value", readProperties("value=" + value).getProperty("value")));
        assertEquals(Map.of("flow", expected, "block", expected), readYaml(build("target")));
    }

    @Test
    void resolveExpressionsAndPreserveNativeTypesInsideCollections() throws IOException {
        writeComponent("source", PROPERTIES, "path=C:\\\\temp\\\\new\n");
        writeComponent("target", YAML,
                "values: [#{2 * 3}, #{'${source@path}'.length()}, true, null, '123']\n");

        assertEquals(java.util.Arrays.asList(6, 11, true, null, "123"), readYaml(build("target")).get("values"));
    }

    @Test
    void preserveUnicodeOffsetsAndComposedScalarTextInCollections() throws IOException {
        writeComponent("source", PROPERTIES, "path=C:\\\\temp\\\\new\ntext=\\#ff0000\n");
        writeComponent("target", YAML,
                "values: [\"🙂 ${source@path}\", prefix ${source@text} suffix, {nested: \"🐈 ${source@text}\"}]\n");

        assertEquals(List.of("🙂 C:\\temp\\new", "prefix #ff0000 suffix", Map.of("nested", "🐈 #ff0000")),
                readYaml(build("target")).get("values"));
    }

    @Test
    void preserveBlockScalarContentsInsideCollections() throws IOException {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n");
        writeComponent("target", YAML,
                "values:\n  - |-\n    first ${source@value}\n    second\n"
                        + "  - >-\n    first ${source@value}\n    second\n  - last\n");

        assertEquals(List.of("first #ff0000\nsecond", "first #ff0000 second", "last"),
                readYaml(build("target")).get("values"));
    }

    @Test
    void preserveExplicitBlockIndentationAndFollowingKeys() throws IOException {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n");
        writeComponent("target", YAML,
                "items:\n  - value: |2-\n      prefix ${source@value}\n    next: ok\n");

        assertEquals(List.of(Map.of("value", "prefix #ff0000", "next", "ok")),
                readYaml(build("target")).get("items"));
    }

    @Test
    void preserveEmojiInDefaultsAndResolvePlainBlockExpressions() throws IOException {
        writeComponent("source", PROPERTIES, "path=C:\\\\temp\\\\new\n");
        writeComponent("target", YAML,
                "values:\n  - ${missing@value:🐈}\n  - #{'${source@path}'.length()}\n  - \"🙂 ${source@path}\"\n");

        assertEquals(List.of("🐈", 11, "🙂 C:\\temp\\new"), readYaml(build("target")).get("values"));
    }

    @Test
    void preserveTagsAnchorsAndDynamicAliasesInCollections() throws IOException {
        writeComponent("source", PROPERTIES, "name=ref\nvalue=123\n");
        writeComponent("target", YAML,
                "values: [&${source@name} !!str ${source@value}, *${source@name}]\n");

        assertEquals(List.of("123", "123"), readYaml(build("target")).get("values"));
    }

    @Test
    void resolveLastBlockScalarWithoutTrailingLineBreak() throws IOException {
        writeComponent("source", PROPERTIES, "value=\\#ff0000\n");
        writeComponent("target", YAML, "items:\n  - |-\n    ${source@value}");

        assertEquals(List.of("#ff0000"), readYaml(build("target")).get("items"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " # keep"})
    void preserveStandaloneYamlListSplices(String suffix) throws IOException {
        writeComponent("source", YAML, "items:\n  - second\n  - third\n");
        writeComponent("target", YAML, "items:\n  - first\n  ${source@items}" + suffix + "\n");

        assertEquals(List.of("first", "second", "third"), readYaml(build("target")).get("items"));
    }

    @Test
    void distinguishFlowElementsFromStandaloneListSplices() throws IOException {
        writeComponent("text", PROPERTIES, "color=\\#ff0000\n");
        writeComponent("source", YAML, "items:\n  - second\n  - third\n");
        writeComponent("target", YAML,
                "items:\n  - nested: [\n      ${text@color}\n    ]\n  ${source@items}\n");

        assertEquals(List.of(Map.of("nested", List.of("#ff0000")), "second", "third"),
                readYaml(build("target")).get("items"));
    }

    private void writeComponent(String component, ConfigFormat format, String text) throws IOException {
        Path file = root.resolve("components/" + component + "/application" + format.extension());
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    private String build(String component) throws IOException {
        Files.writeString(root.resolve("envs/dev.yaml"),
                "portOffset: 1\ngroup:\n  components: [" + component + "]\n");
        Path destination = root.resolve("build");
        new MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", List.of(), List.of());

        Path yaml = destination.resolve(component + "/application.yaml");
        return Files.readString(Files.exists(yaml) ? yaml : destination.resolve(component + "/application.properties"));
    }

    private Properties readProperties(String text) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(text));
        return properties;
    }

    private Map<?, ?> readYaml(String text) {
        return new Yaml().load(text);
    }
}
