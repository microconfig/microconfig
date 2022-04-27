package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.templates.Template;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.StringUtils.getExceptionMessage;

@RequiredArgsConstructor
public class TemplateDefinition {
    private final String templateType;
    @Getter
    private final String templateName;

    private final TemplatePattern templatePattern;
    private final TemplateContentPostProcessor templateContentPostProcessor;
    @Getter
    private File fromFile;
    @Getter
    private File toFile;

    public TemplateDefinition(String templateType, String templateName, TemplatePattern pattern) {
        this(templateType, templateName, pattern, new MustacheTemplateProcessor());
    }

    public Template resolve(Resolver resolver, TypedProperties properties) {
        if (isBinaryTemplate()) return toBinaryTemplate(resolver, properties);
        return toStringTemplate(resolver, properties);
    }

    private StringTemplate toStringTemplate(Resolver resolver,
                                            TypedProperties properties) {
        DeclaringComponent currentComponent = properties.getDeclaringComponent();
        try {
            File destinationFile = destinationFileFor(currentComponent, resolver);
            return toStringTemplate(destinationFile)
                    .resolveBy(resolver, currentComponent)
                    .postProcessContent(templateContentPostProcessor, templateType, properties);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Template error: " + this +
                            "\nComponent: " + currentComponent +
                            "\n" + getExceptionMessage(e), e
            );
        }
    }

    private Template toBinaryTemplate(Resolver resolver, TypedProperties properties) {
        DeclaringComponent currentComponent = properties.getDeclaringComponent();
        File destinationFile = destinationFileFor(currentComponent, resolver);
        return new BinaryTemplate(templateName, getTemplateFile(), destinationFile);
    }

    private boolean isBinaryTemplate() {
        return templateType.equals("file");
    }

    private StringTemplate toStringTemplate(File destination) {
        if (!isCorrect()) {
            throw new IllegalStateException("Incomplete template def: " + this);
        }
        return new StringTemplate(templateName, getTemplateFile(), destination, templatePattern.getPlaceholderPattern());
    }

    private boolean isCorrect() {
        return fromFile != null && toFile != null;
    }

    private File getTemplateFile() {
        if (!fromFile.isAbsolute()) {
            throw new IllegalArgumentException("Using relative path for template '" + fromFile + "'. "
                    + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
        }
        if (!fromFile.exists() || !fromFile.isFile()) {
            throw new IllegalStateException("Missing template file: " + fromFile);
        }
        return fromFile;
    }

    private File destinationFileFor(DeclaringComponent currentComponent, Resolver resolver) {
        return toFile.isAbsolute() ? toFile : new File(destinationDir(currentComponent, resolver), toFile.getPath());
    }

    private String destinationDir(DeclaringComponent currentComponent, Resolver resolver) {
        return resolver.resolve("${this@resultDir}", currentComponent, currentComponent);
    }

    public void setFromFile(String fromFile) {
        this.fromFile = new File(fromFile);
        if (this.toFile == null) {
            setToFile(this.fromFile.getName());
        }
    }

    public void setToFile(String toFile) {
        this.toFile = new File(toFile.replace("${templateName}", templateName));
    }

    @Override
    public String toString() {
        return "templateName: '" + templateName + "', file: '" + fromFile + "' -> '" + toFile + "'";
    }
}