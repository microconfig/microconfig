package io.microconfig.core.properties.templates;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.properties.io.yaml.YamlTreeImpl;
import io.microconfig.core.templates.TemplateContentPostProcessor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

import static io.microconfig.utils.Logger.info;

public class MustacheTemplateProcessor implements TemplateContentPostProcessor {
    private static final String MUSTACHE = "mustache";

    @Override
    public String process(String templateName, File source, String content, TypedProperties properties) {
        if (!isMustacheTemplate(source, templateName)) return content;

        info("Using mustache template for " + properties.getDeclaringComponent().getComponent() + "/" + source.getName());
        return compile(source, content).execute(toYaml(properties));
    }

    private boolean isMustacheTemplate(File source, String templateName) {
        return source.getName().endsWith("." + MUSTACHE) || templateName.contains(MUSTACHE);
    }

    private Template compile(File currentTemplate, String source) {
        return Mustache.compiler()
                .withLoader(templateLoader(currentTemplate))
                .compile(source);
    }

    private Mustache.TemplateLoader templateLoader(File currentTemplate) {
        return fileName -> {
            File newTemplate = new File(fileName);
            File fullPath = newTemplate.isAbsolute() ? newTemplate : new File(currentTemplate.getParent(), fileName);
            return new FileReader(fullPath);
        };
    }

    private Map<String, Object> toYaml(TypedProperties properties) {
        String text = new YamlTreeImpl().toYaml(properties.getPropertiesAsKeyValue());
        return new Yaml().load(text);
    }
}