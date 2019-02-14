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
    private static final String DEFAULT_SYSTEM_PROPERTIES_PREFIX = "SYSTEM@";
    private static final String DEFAULT_ENV_PREFIX = "ENV@";
    private static final String DEFAULT_PATTERN = "(?<escaped>\\\\)?(?<placeholder>\\$\\{(?<name>.+?)(?::(?<defvalue>.*?))??})";

    private final String templatePrefix;
    private final String fromFileSuffix;
    private final String toFileSuffix;
    private final String systemPropPrefix;
    private final String envPropPrefix;
    private final Pattern pattern;

    public static TemplatePattern defaultPattern() {
        return TemplatePattern.builder()
                .templatePrefix(DEFAULT_TEMPLATE_PREFIX)
                .fromFileSuffix(DEFAULT_FROM_FILE_SUFFIX)
                .toFileSuffix(DEFAULT_TO_FILE_SUFFIX)
                .systemPropPrefix(DEFAULT_SYSTEM_PROPERTIES_PREFIX)
                .envPropPrefix(DEFAULT_ENV_PREFIX)
                .pattern(Pattern.compile(DEFAULT_PATTERN))
                .build();
    }
}