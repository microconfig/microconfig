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

@RequiredArgsConstructor
public class CopyTemplatesService {
    private final TemplatePattern templatePattern;

    public CopyTemplatesService() {
        this(defaultPattern());
    }

    public void copyTemplatesOf(DeclaringComponent currentComponent,
                                Map<String, Property> componentProperties,
                                Resolver resolver,
                                File destinationDir) {
        findTemplateDefinitionsFrom(componentProperties.values()).forEach(def -> {
            try {
                def.resolveAndCopy(resolver, currentComponent, destinationDir);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Template error: " + def + ", component: " + currentComponent, e);
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
            toTemplate()
                    .resolveBy(resolver, currentComponent)
                    .copyTo(getDestinationFile(destinationDir));
        }

        private Template toTemplate() {
            if (!isCorrect()) {
                throw new IllegalStateException("Incomplete template def: " + this);
            }
            return new Template(getTemplateFile(), templatePattern.getPattern());
        }

        private boolean isCorrect() {
            return fromFile != null && toFile != null;
        }

        private File getTemplateFile() {
            File path = new File(fromFile);
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException("Using relative path for template '" + fromFile + "'. "
                        + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
            }
            return path;
        }

        private File getDestinationFile(File serviceDir) {
            File path = new File(toFile);
            return path.isAbsolute() ? path : new File(serviceDir, path.getPath());
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