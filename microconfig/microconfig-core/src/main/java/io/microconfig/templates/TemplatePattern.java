package io.microconfig.templates;

import lombok.Builder;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
@Builder(toBuilder = true)
public class TemplatePattern {
    private static final String DEFAULT_TEMPLATE_PREFIX = "template.";
    private static final String DEFAULT_FROM_FILE_SUFFIX = ".fromFile";
    private static final String DEFAULT_TO_FILE_SUFFIX = ".toFile";
    private static final String DEFAULT_PATTERN = "(?<escaped>\\\\)?(?<placeholder>\\$\\{(?<name>.+?)(?::(?<defuvalue>.*?))??})";

    private final String templatePrefix;
    private final String fromFileSuffix;
    private final String toFileSuffix;
    private final Pattern pattern;

    public static TemplatePattern defaultPattern() {
        return TemplatePattern.builder()
                .templatePrefix(DEFAULT_TEMPLATE_PREFIX)
                .fromFileSuffix(DEFAULT_FROM_FILE_SUFFIX)
                .toFileSuffix(DEFAULT_TO_FILE_SUFFIX)
                .pattern(Pattern.compile(DEFAULT_PATTERN))
                .build();
    }
}