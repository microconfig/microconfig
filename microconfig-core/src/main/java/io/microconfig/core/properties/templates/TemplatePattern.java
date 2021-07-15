package io.microconfig.core.properties.templates;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;

@With
@Getter
@Builder
public class TemplatePattern {
    public static final Pattern DEFAULT_PLACEHOLDER_PATTERN =
            compile("(?<escaped>\\\\)?" +
                    "(?<placeholder>\\$\\{(?<name>.+?)" +
                    "(?::(?<defuvalue>.*?))??})"
            );

    private final List<String> templatePrefixes;
    private final Pattern placeholderPattern;

    public static TemplatePattern defaultPattern() {
        return TemplatePattern.builder()
                .templatePrefixes(asList(
                        "microconfig.template.",
                        "microconfig.mustache.",
                        "microconfig.file.",
                        "mc.template.",
                        "mc.mustache.",
                        "mc.file."
                )).placeholderPattern(DEFAULT_PLACEHOLDER_PATTERN)
                .build();
    }

    public boolean startsWithTemplatePrefix(String key) {
        return templatePrefixes.stream().anyMatch(key::startsWith);
    }

    public String extractTemplateType(String str) {
        int templateTypeStartIndex = str.indexOf('.') + 1;
        int secondDot = str.indexOf('.', templateTypeStartIndex);
        return str.substring(templateTypeStartIndex, secondDot);
    }

    public String extractTemplateName(String str) {
        return templatePrefixes.stream()
                .filter(str::startsWith)
                .map(prefix -> str.replaceFirst(prefix, ""))
                .map(key -> key.split("\\.")[0])
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Incorrect template " + str));
    }
}