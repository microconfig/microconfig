package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.templates.TemplateContentPostProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.Logger.info;

@RequiredArgsConstructor
public class TemplateDefinition {
    private final String templateType;
    @Getter
    private final String templateName;

    private final TemplatePattern templatePattern;
    private final TemplateContentPostProcessor templateContentPostProcessor;

    public TemplateDefinition(String templateType, String templateName, TemplatePattern pattern) {
        this(templateType, templateName, pattern, new MustacheTemplateProcessor());
    }

    private File fromFile;
    private File toFile;

    public void resolveAndCopy(Resolver resolver, TypedProperties properties) {
        DeclaringComponent currentComponent = properties.getDeclaringComponent();
        toTemplate()
                .resolveBy(resolver, currentComponent)
                .postProcessContent(templateContentPostProcessor, templateType, properties)
                .copyTo(destinationFileFor(currentComponent, resolver));
        info("Copied '" + currentComponent.getComponent() + "' template ../" + fromFile.getParentFile().getName() + "/" + fromFile.getName() + " -> " + toFile);
    }

    private Template toTemplate() {
        if (!isCorrect()) {
            throw new IllegalStateException("Incomplete template def: " + this);
        }
        return new Template(templateName, getTemplateFile(), templatePattern.getPlaceholderPattern());
    }

    private boolean isCorrect() {
        return fromFile != null && toFile != null;
    }

    private File getTemplateFile() {
        if (!fromFile.isAbsolute()) {
            throw new IllegalArgumentException("Using relative path for template '" + fromFile + "'. "
                    + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
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
        this.toFile = new File(toFile);
    }

    @Override
    public String toString() {
        return "templateName: '" + templateName + "', file: '" + fromFile + "' -> '" + toFile + "'";
    }
}