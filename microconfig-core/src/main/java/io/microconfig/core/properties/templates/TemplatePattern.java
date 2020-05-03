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
    public static final Pattern DEFAULT_PATTERN =
            compile("(?<escaped>\\\\)?" +
                    "(?<placeholder>\\$\\{(?<name>.+?)" +
                    "(?::(?<defuvalue>.*?))??})"
            );

    private final List<String> templatePrefixes;
    private final String fromFileSuffix;
    private final String toFileSuffix;
    private final Pattern pattern;

    public static TemplatePattern defaultPattern() {
        return TemplatePattern.builder()
                .templatePrefixes(asList("microconfig.template.", "mc.template.", "mc.mustache."))
                .fromFileSuffix(".fromFile")
                .toFileSuffix(".toFile")
                .pattern(DEFAULT_PATTERN)
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
                .map(p -> {
                    int endIndex = str.endsWith(fromFileSuffix) ? fromFileSuffix.length() : toFileSuffix.length();
                    return str.substring(p.length(), str.length() - endIndex);
                }).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Incorrect template " + str));
    }
}