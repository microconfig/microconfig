package io.microconfig.templates.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.templates.TemplateContentPostProcessor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class MustacheTemplateProcessor implements TemplateContentPostProcessor {
    private static final String MUSTACHE = ".mustache";

    @Override
    public String process(String templateName, File source, String content, TypedProperties properties) {
        if (!source.getName().endsWith(MUSTACHE) && !templateName.contains(MUSTACHE)) return content;

        Mustache mustache = compile(content);

        StringWriter sw = new StringWriter();
        Writer writer = mustache.execute(sw, toYaml(properties));
        return writer.toString();
    }

    private Map<String, Object> toYaml(TypedProperties properties) {
        String text = properties.getProperties()
                .stream()
                .map(p -> p.getKey() + ": " + p.getValue())
                .collect(joining("\n"));
        return new Yaml().load(text);
    }

    private Mustache compile(String source) {
        return new DefaultMustacheFactory()
                .compile(new StringReader(source), "");
    }
}