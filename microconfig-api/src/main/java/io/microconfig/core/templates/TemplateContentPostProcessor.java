package io.microconfig.core.templates;

import io.microconfig.core.properties.TypedProperties;

import java.io.File;

public interface TemplateContentPostProcessor {
    String process(File source, String content, TypedProperties properties);

    static TemplateContentPostProcessor empty() {
        return (_1, content, _3) -> content;
    }
}
