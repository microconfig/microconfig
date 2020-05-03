package io.microconfig.core.templates;

import io.microconfig.core.properties.TypedProperties;

import java.io.File;

public interface TemplateContentPostProcessor {
    String process(String templateType,
                   File source, String templateContent,
                   TypedProperties properties);
}
