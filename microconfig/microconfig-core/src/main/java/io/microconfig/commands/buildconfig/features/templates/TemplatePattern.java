package io.microconfig.commands.buildconfig.features.templates;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Wither;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

@Getter
@Wither
@Builder
public class TemplatePattern {
    private static final String DEFAULT_TEMPLATE_PREFIX = "microconfig.template.";
    private static final String DEFAULT_FROM_FILE_SUFFIX = ".fromFile";
    private static final String DEFAULT_TO_FILE_SUFFIX = ".toFile";
    private static final Pattern DEFAULT_PATTERN = compile("(?<escaped>\\\\)?(?<placeholder>\\$\\{(?<name>.+?)(?::(?<defuvalue>.*?))??})");

    private final String templatePrefix;
    private final String fromFileSuffix;
    private final String toFileSuffix;
    private final Pattern pattern;

    public static TemplatePattern defaultPattern() {
        return TemplatePattern.builder()
                .templatePrefix(DEFAULT_TEMPLATE_PREFIX)
                .fromFileSuffix(DEFAULT_FROM_FILE_SUFFIX)
                .toFileSuffix(DEFAULT_TO_FILE_SUFFIX)
                .pattern(DEFAULT_PATTERN)
                .build();
    }
}