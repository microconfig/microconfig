package io.microconfig.templates;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.utils.FilePermissionUtils.copyPermissions;
import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.StringUtils.unixLikePath;

@RequiredArgsConstructor
public class CopyTemplatesServiceImpl implements CopyTemplatesService {
    private final TemplatePattern templatePattern;
    private final RelativePathResolver relativePathResolver;

    @Override
    public void copyTemplates(File destinationDir, Map<String, String> serviceProperties) {
        collectTemplates(serviceProperties).forEach(def -> {
            try {
                def.resolveAndCopy(destinationDir, serviceProperties);
            } catch (RuntimeException e) {
                error("Template error " + def, e);
            }
        });
    }

    private Collection<TemplateDefinition> collectTemplates(Map<String, String> serviceProperties) {
        Map<String, TemplateDefinition> templateByName = new LinkedHashMap<>();

        serviceProperties.forEach((key, value) -> {
            if (!key.startsWith(templatePattern.getTemplatePrefix())) return;

            String fromFileSuffix = templatePattern.getFromFileSuffix();
            String toFileSuffix = templatePattern.getToFileSuffix();
            if (key.endsWith(fromFileSuffix)) {
                getOrCreate(key, fromFileSuffix, templateByName).fromFile = value;
            } else if (key.endsWith(toFileSuffix)) {
                getOrCreate(key, toFileSuffix, templateByName).toFile = value;
            }
        });

        return templateByName.values();
    }

    private TemplateDefinition getOrCreate(String key, String suffix, Map<String, TemplateDefinition> templates) {
        return templates.computeIfAbsent(extractMiddle(key, suffix), TemplateDefinition::new);
    }

    private String extractMiddle(String str, String suffix) {
        try {
            return str.substring(templatePattern.getTemplatePrefix().length(), str.length() - suffix.length());
        } catch (RuntimeException e) {
            throw new RuntimeException("Incorrect template: " + str);
        }
    }

    @RequiredArgsConstructor
    private class TemplateDefinition {
        private final String name;

        private String fromFile;
        private String toFile;

        private void resolveAndCopy(File destinationDir, Map<String, String> serviceProperties) {
            if (!isCorrect()) {
                warn("Incomplete template def " + this);
                return;
            }

            File fromFile = absolute(destinationDir, this.fromFile);
            File toFile = absolute(destinationDir, this.toFile);

            Template template = toTemplate(fromFile, destinationDir.getName());
            if (template == null) return;

            String content = doResolve(serviceProperties, template, destinationDir);
            write(toFile, content);
            copyPermissions(fromFile.toPath(), toFile.toPath());

            info("Copied template: " + fromFile + " -> " + toFile);
        }

        private boolean isCorrect() {
            return fromFile != null && toFile != null;
        }

        private File absolute(File serviceDir, String file) {
            File path = relativePathResolver.overrideRelativePath(file,
                    () -> "Overriding template path for " + serviceDir.getName() + " " + this +
                            ". Use ${this@configDir}- resolves config repo root or ${component_name@folder} - resolves folder of config component");
            return path.isAbsolute() ? path : new File(serviceDir, path.getPath());
        }

        private Template toTemplate(File fromFile, String component) {
            if (!fromFile.exists() || !fromFile.isFile()) {
                warn("Missing file to copy. " + this + ". Service: " + component);
                return null;
            }

            try {
                return new Template(readFully(fromFile));
            } catch (RuntimeException e) {
                warn("Cannot read fromFile. " + this);
                return null;
            }
        }

        private String doResolve(Map<String, String> serviceProperties, Template template, File serviceDir) {
            String resolved = template.resolvePlaceholders(serviceProperties, templatePattern);
            return resolveSpecialsPlaceholders(resolved, serviceDir);
        }

        private String resolveSpecialsPlaceholders(String content, File serviceDir) {
            return content.replace("${serviceDir}", unixLikePath(serviceDir.getAbsolutePath()));
        }

        @Override
        public String toString() {
            return "template: " + name + ", file: " + fromFile + " -> " + toFile;
        }
    }
}