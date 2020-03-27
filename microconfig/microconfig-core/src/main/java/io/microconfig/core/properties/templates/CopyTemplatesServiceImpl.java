//package io.microconfig.core.properties.templates;
//
//import io.microconfig.core.configtypes.ConfigType;
//import io.microconfig.core.properties.Property;
//import lombok.RequiredArgsConstructor;
//
//import java.io.File;
//import java.util.Collection;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
//import static io.microconfig.utils.FileUtils.copyPermissions;
//import static io.microconfig.utils.FileUtils.write;
//import static io.microconfig.utils.Logger.*;
//
//@RequiredArgsConstructor
//public class CopyTemplatesServiceImpl implements CopyTemplatesService {
//    private final TemplatePattern templatePattern;
//
//    public CopyTemplatesServiceImpl() {
//        this(defaultPattern());
//    }
//
//    @Override
//    public void copyTemplates(Collection<Property> properties,
//                              ConfigType configType,
//                              String componentName,
//                              String environment) {
//        findTemplateDefinitionsIn(properties).forEach(def -> {
//            try {
//                def.resolveAndCopy(configType, componentName, environment);
//            } catch (RuntimeException e) {
//                error("Template error: " + def, e);
//            }
//        });
//    }
//
//    private Collection<TemplateDefinition> findTemplateDefinitionsIn(Collection<Property> properties) {
//        Map<String, TemplateDefinition> templateByName = new LinkedHashMap<>();
//
//        properties.forEach(p -> {
//            String key = p.getKey();
//            String value = p.getValue();
//            if (!templatePattern.startsWithTemplatePrefix(key)) return;
//
//            String fromFileSuffix = templatePattern.getFromFileSuffix();
//            String toFileSuffix = templatePattern.getToFileSuffix();
//
//            if (key.endsWith(fromFileSuffix)) {
//                if (value.trim().isEmpty()) {
//                    info("Ignoring template '" + key + "' cause value is empty");
//                    return;
//                }
//                getOrCreate(key, templateByName).setFromFile(value);
//            } else if (key.endsWith(toFileSuffix)) {
//                getOrCreate(key, templateByName).setToFile(value);
//            }
//        });
//
//        return templateByName.values();
//    }
//
//    private TemplateDefinition getOrCreate(String key, Map<String, TemplateDefinition> templates) {
//        return templates.computeIfAbsent(templatePattern.extractTemplateName(key), TemplateDefinition::new);
//    }
//
//    @RequiredArgsConstructor
//    private class TemplateDefinition {
//        private final String name;
//
//        private String fromFile;
//        private String toFile;
//
//        private void resolveAndCopy(ConfigType configType, String currentComponent, String environment) {
//            if (!isCorrect()) {
//                warn("Incomplete template def " + this);
//                return;
//            }
//
//            File fromFile = absoluteFromFile(destinationDir);
//            File toFile = absoluteToFile(destinationDir);
//
//            Template template = toTemplate(fromFile, currentComponent);
//            if (template == null) return;
//
//            String content = template.resolvePlaceholders(currentComponent, propertyResolver, templatePattern.getPattern());
//            write(toFile, content);
//            copyPermissions(fromFile.toPath(), toFile.toPath());
//
//            info("Copied template: " + fromFile + " -> " + toFile);
//        }
//
//        private boolean isCorrect() {
//            return fromFile != null && toFile != null;
//        }
//
//        private File absoluteFromFile(File serviceDir) {
//            File path = new File(fromFile);
//            if (!path.isAbsolute()) {
//                throw new IllegalArgumentException("Using relative path for template '" + fromFile + "' for component '" + serviceDir.getName() + "'. "
//                        + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
//            }
//            return path;
//        }
//
//        private File absoluteToFile(File serviceDir) {
//            File path = new File(toFile);
//            return path.isAbsolute() ? path : new File(serviceDir, path.getPath());
//        }
//
//        private Template toTemplate(File fromFile, String component) {
//            if (!fromFile.exists() || !fromFile.isFile()) {
//                warn("Missing file to copy. " + this + ". Service: " + component);
//                return null;
//            }
//
//            try {
//                return new Template(fromFile);
//            } catch (RuntimeException e) {
//                warn("Cannot read fromFile. " + this);
//                return null;
//            }
//        }
//
//        public void setFromFile(String fromFile) {
//            this.fromFile = fromFile;
//            if (this.toFile == null) {
//                setToFile(new File(fromFile).getName());
//            }
//        }
//
//        public void setToFile(String toFile) {
//            this.toFile = toFile;
//        }
//
//        @Override
//        public String toString() {
//            return "templateName: '" + name + "', file: '" + fromFile + "' -> '" + toFile + "'";
//        }
//    }
//}