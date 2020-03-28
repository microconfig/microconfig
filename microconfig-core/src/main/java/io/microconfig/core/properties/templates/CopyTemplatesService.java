package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.FileUtils.copyPermissions;
import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.Logger.info;

@RequiredArgsConstructor
public class CopyTemplatesService {
    private final TemplatePattern templatePattern;

    public CopyTemplatesService() {
        this(defaultPattern());
    }

    public void copyTemplates(DeclaringComponent currentComponent,
                              File serviceDestinationDir,
                              Map<String, Property> componentProperties,
                              Resolver resolver) {
        findTemplateDefinitionsFrom(componentProperties.values()).forEach(def -> {
            try {
                def.resolveAndCopy(resolver, currentComponent, serviceDestinationDir);
            } catch (RuntimeException e) {
                error("Template error: " + def + ", component: " + currentComponent, e);
            }
        });
    }

    //todo exception handling
    private Collection<TemplateDefinition> findTemplateDefinitionsFrom(Collection<Property> componentProperties) {
        Map<String, TemplateDefinition> templateByName = new LinkedHashMap<>();

        componentProperties.forEach(p -> {
            String key = p.getKey();
            String value = p.getValue();
            if (!templatePattern.startsWithTemplatePrefix(key)) return;

            if (key.endsWith(templatePattern.getFromFileSuffix())) {
                getOrCreate(key, templateByName).setFromFile(value.trim());
            } else if (key.endsWith(templatePattern.getToFileSuffix())) {
                getOrCreate(key, templateByName).setToFile(value.trim());
            }
        });

        return templateByName.values();
    }

    private TemplateDefinition getOrCreate(String key, Map<String, TemplateDefinition> templates) {
        return templates.computeIfAbsent(templatePattern.extractTemplateName(key), TemplateDefinition::new);
    }

    @RequiredArgsConstructor
    private class TemplateDefinition {
        private final String name;

        private String fromFile;
        private String toFile;

        private void resolveAndCopy(Resolver resolver, DeclaringComponent currentComponent, File destinationDir) {
            checkCorrectness();

            File fromFile = absoluteFromFile(destinationDir);
            File toFile = absoluteToFile(destinationDir);

            Template template = toTemplate(fromFile);

            String content = template.resolvePlaceholdersBy(resolver, currentComponent, templatePattern.getPattern());
            write(toFile, content);
            copyPermissions(fromFile.toPath(), toFile.toPath());

            info("Copied template: " + fromFile + " -> " + toFile);
        }

        private void checkCorrectness() {
            if (!isCorrect()) {
                throw new IllegalStateException("Incomplete template def: " + this);
            }
        }

        private boolean isCorrect() {
            return fromFile != null && toFile != null;
        }

        private File absoluteFromFile(File serviceDir) {
            File path = new File(fromFile);
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException("Using relative path for template '" + fromFile + "' for component '" + serviceDir.getName() + "'. "
                        + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
            }
            return path;
        }

        private File absoluteToFile(File serviceDir) {
            File path = new File(toFile);
            return path.isAbsolute() ? path : new File(serviceDir, path.getPath());
        }

        private Template toTemplate(File fromFile) {
            return new Template(fromFile);
        }

        public void setFromFile(String fromFile) {
            this.fromFile = fromFile;
            if (this.toFile == null) {
                setToFile(new File(fromFile).getName());
            }
        }

        public void setToFile(String toFile) {
            this.toFile = toFile;
        }

        @Override
        public String toString() {
            return "templateName: '" + name + "', file: '" + fromFile + "' -> '" + toFile + "'";
        }
    }
}