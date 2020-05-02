package io.microconfig.templates.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.templates.TemplateContentPostProcessor;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

public class MustacheTemplateProcessor implements TemplateContentPostProcessor {
    @Override
    public String process(File source, String content, TypedProperties properties) {
        if (!source.getName().endsWith(".mustache")) return content;

        Mustache mustache = compile(content);

        Writer writer = mustache.execute(new StringWriter(), properties.getPropertiesAsKeyValue());
        return writer.toString();
    }

    private Mustache compile(String source) {
        return new DefaultMustacheFactory()
                .compile(new StringReader(source), "");
    }
}