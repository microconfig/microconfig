package io.microconfig.core.properties.io.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.DOUBLE_QUOTED;

/**
 * A '>' block scalar folded by YAML rules and kept the way microconfig keeps YAML values: as a YAML fragment.
 * Plain when it reads back as the same string, double-quoted otherwise, e.g. '"a: b\n"'.
 */
final class FoldedScalar {
    private FoldedScalar() {
    }

    static String toYamlValue(String chomping, List<String> lines) {
        String text = fold(chomping, lines);
        return readsBackAsPlain(text) ? text : doubleQuoted(text);
    }

    private static String fold(String chomping, List<String> lines) {
        String content = lines.stream()
                .map(line -> line.isEmpty() ? line : " " + line)
                .collect(joining("\n"));
        return new Yaml().load(">" + chomping + "\n" + content + "\n");
    }

    private static boolean readsBackAsPlain(String text) {
        if (text.isEmpty() || text.contains("\n") || !text.equals(text.trim())) return false;
        try {
            Map<?, ?> document = new Yaml().load("value: " + text);
            return text.equals(document.get("value"));
        } catch (YAMLException | ClassCastException e) {
            return false;
        }
    }

    private static String doubleQuoted(String text) {
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(DOUBLE_QUOTED);
        options.setSplitLines(false);
        return new Yaml(options).dump(text).trim();
    }
}
