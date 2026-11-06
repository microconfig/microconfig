package io.microconfig.core.properties.io.yaml;

import org.yaml.snakeyaml.Yaml;

import java.util.List;

import static io.microconfig.core.properties.io.yaml.YamlScalars.doubleQuoted;
import static io.microconfig.core.properties.io.yaml.YamlScalars.readsBackAsString;
import static java.util.stream.Collectors.joining;

/**
 * A '>' block scalar folded by YAML rules and kept the way microconfig keeps YAML values: as a YAML fragment.
 * Plain when it reads back as the same string, double-quoted otherwise, e.g. '"a: b\n"'.
 */
final class FoldedScalar {
    private FoldedScalar() {
    }

    static String toYamlValue(String chomping, List<String> lines) {
        String text = fold(chomping, lines);
        return readsBackAsString(text) ? text : doubleQuoted(text);
    }

    private static String fold(String chomping, List<String> lines) {
        String content = lines.stream()
                .map(line -> line.isEmpty() ? line : " " + line)
                .collect(joining("\n"));
        return new Yaml().load(">" + chomping + "\n" + content + "\n");
    }

}
