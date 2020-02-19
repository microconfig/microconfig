package io.microconfig.commands.buildconfig.features.templates;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.compile;

@Getter
@With
@Builder
public class TemplatePattern {
    public static final String DEFAULT_TEMPLATE_PREFIX = "microconfig.template.";
    public static final String DEFAULT_FROM_FILE_SUFFIX = ".fromFile";
    public static final String DEFAULT_TO_FILE_SUFFIX = ".toFile";
    public static final Pattern DEFAULT_PATTERN = compile("(?<escaped>\\\\)?(?<placeholder>\\$\\{(?<name>.+?)(?::(?<defuvalue>.*?))??})");

    private final List<String> templatePrefixes;
    private final String fromFileSuffix;
    private final String toFileSuffix;
    private final Pattern pattern;

    public static TemplatePattern defaultPattern() {
        return TemplatePattern.builder()
                .templatePrefixes(singletonList(DEFAULT_TEMPLATE_PREFIX))
                .fromFileSuffix(DEFAULT_FROM_FILE_SUFFIX)
                .toFileSuffix(DEFAULT_TO_FILE_SUFFIX)
                .pattern(DEFAULT_PATTERN)
                .build();
    }

    public boolean startsWithTemplatePrefix(String key) {
        return templatePrefixes.stream().anyMatch(key::startsWith);
    }

    public String extractTemplateName(String str) {
        return templatePrefixes.stream()
                .filter(str::startsWith)
                .map(p -> {
                    int endIndex = str.endsWith(fromFileSuffix) ? fromFileSuffix.length() : toFileSuffix.length();
                    return str.substring(p.length(), str.length() - endIndex);
                }).findFirst().get();

    }
}