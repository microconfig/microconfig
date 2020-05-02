package io.microconfig.core.properties.templates;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.properties.io.yaml.YamlTreeImpl;
import io.microconfig.core.templates.TemplateContentPostProcessor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Map;

import static io.microconfig.utils.Logger.info;

public class MustacheTemplateProcessor implements TemplateContentPostProcessor {
    private static final String MUSTACHE = "mustache";

    @Override
    public String process(String templateName, File source, String content, TypedProperties properties) {
        if (!isMustacheTemplate(source, templateName)) return content;

        info("Using mustache template for " + properties.getDeclaringComponent().getComponent() + "/" + source.getName());
        return compile(content).execute(toYaml(properties));
    }

    private boolean isMustacheTemplate(File source, String templateName) {
        return source.getName().endsWith("." + MUSTACHE) || templateName.contains(MUSTACHE);
    }

    private Template compile(String source) {
        return Mustache.compiler().compile(source);
    }

    private Map<String, Object> toYaml(TypedProperties properties) {
        String text = new YamlTreeImpl().toYaml(properties.getPropertiesAsKeyValue());
        return new Yaml().load(text);
    }
}