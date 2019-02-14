package io.microconfig.templates;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public class TemplatePattern {
    private static final String DEFAULT_TEMPLATE_PREFIX = "mgmt.template.";
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
        return new TemplatePattern(
                DEFAULT_TEMPLATE_PREFIX, DEFAULT_FROM_FILE_SUFFIX, DEFAULT_TO_FILE_SUFFIX,
                DEFAULT_SYSTEM_PROPERTIES_PREFIX, DEFAULT_ENV_PREFIX,
                Pattern.compile(DEFAULT_PATTERN)
        );
    }
}