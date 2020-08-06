package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.templates.TemplateDefinition;
import io.microconfig.core.properties.templates.TemplatePattern;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.Files.list;

@RequiredArgsConstructor
public class ArrowNotationParser implements TemplateDefinitionParser {
    private final TemplatePattern templatePattern;
    private final List<TemplateDefinition> templates = new ArrayList<>();

    @Override
    public void add(String key, String value) {
        if (!key.endsWith(templatePattern.extractTemplateName(key))) return;

        String[] split = value.trim().split(" -> ");
        if (split.length != 2) return;

        process(key, split[0], split[1]);
    }

    @Override
    public Collection<TemplateDefinition> getDefinitions() {
        return templates;
    }

    private void process(String key, String from, String to) {
        if (from.endsWith("/*")) {
            processWithAsterisk(key, from, to);
        } else {
            addTemplateDefinition(key, templatePattern.extractTemplateName(key), from, to);
        }
    }

    private void processWithAsterisk(String key, String from, String to) {
        String fromDir = from.substring(0, from.length() - 2);
        try (Stream<Path> list = list(new File(fromDir).toPath())) {
            list.forEach(path ->
                    addTemplateDefinition(key, templatePattern.extractTemplateName(key) + "[" + path.getFileName() + "]",
                            path.toString(), to + "/" + path.getFileName()));
        } catch (IOException e) {
            throw new RuntimeException("Can't list files in dir " + from);
        }
    }

    private void addTemplateDefinition(String key, String name, String from, String to) {
        TemplateDefinition templateDefinition = new TemplateDefinition(templatePattern.extractTemplateType(key), name, templatePattern);
        templateDefinition.setFromFile(from);
        templateDefinition.setToFile(to);
        templates.add(templateDefinition);
    }
}
