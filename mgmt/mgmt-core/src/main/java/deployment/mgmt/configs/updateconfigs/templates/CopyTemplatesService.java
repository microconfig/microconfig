package deployment.mgmt.configs.updateconfigs.templates;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static mgmt.utils.FilePermissionUtils.copyPermissions;

@RequiredArgsConstructor
public class CopyTemplatesService {
    private static final String TEMPLATE_PREFIX = "mgmt.template.";
    private static final String FROM_FILE_SUFFIX = ".fromFile";
    private static final String TO_FILE_SUFFIX = ".toFile";

    private final RelativePathResolver relativePathResolver;

    public void copyTemplates(File serviceDir, Map<String, String> serviceProperties) {
        collectTemplates(serviceProperties).forEach(def -> {
            try {
                def.resolveAndCopy(serviceDir, serviceProperties);
            } catch (Exception e) {
                error("Template error " + def, e);
            }
        });
    }

    private Collection<TemplateDef> collectTemplates(Map<String, String> serviceProperties) {
        Map<String, TemplateDef> templateByName = new LinkedHashMap<>();

        serviceProperties.forEach((key, value) -> {
            if (!key.startsWith(TEMPLATE_PREFIX)) return;

            if (key.endsWith(FROM_FILE_SUFFIX)) {
                getOrCreate(key, FROM_FILE_SUFFIX, templateByName).fromFile = value;
            } else if (key.endsWith(TO_FILE_SUFFIX)) {
                getOrCreate(key, TO_FILE_SUFFIX, templateByName).toFile = value;
            }
        });

        return templateByName.values();
    }

    private TemplateDef getOrCreate(String key, String suffix, Map<String, TemplateDef> templates) {
        return templates.computeIfAbsent(extractMiddle(key, suffix), TemplateDef::new);
    }

    private String extractMiddle(String str, String suffix) {
        try {
            return str.substring(TEMPLATE_PREFIX.length(), str.length() - suffix.length());
        } catch (RuntimeException e) {
            throw new RuntimeException("Incorrect template: " + str);
        }
    }

    @RequiredArgsConstructor
    private class TemplateDef {
        private final String name;

        private String fromFile;
        private String toFile;

        private void resolveAndCopy(File serviceDir, Map<String, String> serviceProperties) {
            if (!isCorrect()) {
                warn("Incomplete template def " + this);
                return;
            }

            File fromFile = absolute(serviceDir, this.fromFile);
            File toFile = absolute(serviceDir, this.toFile);

            Template template = toTemplate(fromFile, serviceDir.getName());
            if (template == null) return;

            String content = template.resolvePlaceholders(serviceProperties);
            content = resolveSpecialsPlaceholders(content, serviceDir);

            write(toFile, content);
            copyPermissions(fromFile.toPath(), toFile.toPath());
            info("Copied template: " + fromFile + " -> " + toFile);
        }

        private String resolveSpecialsPlaceholders(String content, File serviceDir) {
            return content.replace("${serviceDir}", unixLikePath(serviceDir.getAbsolutePath()));
        }

        private File absolute(File serviceDir, String file) {
            File path = relativePathResolver.overrideRelativePath(file,
                    () -> "Overriding template path for " + serviceDir.getName() + " " + this +
                            ". Use ${this@configDir}- resolves config repo root or ${component_name@folder} - resolves config folder of component");
            return path.isAbsolute() ? path : new File(serviceDir, path.getPath());
        }

        private boolean isCorrect() {
            return fromFile != null && toFile != null;
        }

        private Template toTemplate(File fromFile, String component) {
            if (!fromFile.exists() || !fromFile.isFile()) {
                warn("Missing file to copy. " + this + ". Service: " + component);
                return null;
            }

            try {
                return new Template(readFully(fromFile));
            } catch (Exception e) {
                warn("Cannot read fromFile. " + this);
                return null;
            }
        }

        @Override
        public String toString() {
            return "template: " + name + ", file: " + fromFile + " -> " + toFile;
        }
    }
}