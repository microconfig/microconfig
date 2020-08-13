package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.templates.TemplateContentPostProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;
import static io.microconfig.utils.FileUtils.copyPermissions;
import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.StringUtils.addOffsets;
import static java.util.regex.Matcher.quoteReplacement;

@RequiredArgsConstructor
class Template {
    private final String templateName;
    private final File source;
    private final Pattern pattern;
    @With
    @Getter
    private final String content;

    Template(String templateName, File source, Pattern pattern) {
        if (!source.exists() || !source.isFile()) {
            throw new IllegalStateException("Missing template file: " + source);
        }
        this.templateName = templateName;
        this.source = source;
        this.pattern = pattern;
        this.content = readFully(source);
    }

    public Template resolveBy(Resolver resolver, DeclaringComponent currentComponent) {
        Matcher m = pattern.matcher(content.replace("${this@templateName}", templateName));
        if (!m.find()) return this;

        StringBuffer result = new StringBuffer();
        do {
            doResolve(m, result, resolver, currentComponent);
        } while (m.find());
        m.appendTail(result);
        String content = result.toString();
        return withContent(content);
    }

    public Template postProcessContent(TemplateContentPostProcessor postProcessor,
                                       String templateType, TypedProperties properties) {
        return withContent(
                postProcessor.process(templateType, source, content, properties)
        );
    }

    public void copyTo(File destinationFile) {
        write(destinationFile, content);
        copyPermissions(source.toPath(), destinationFile.toPath());
    }

    private void doResolve(Matcher m, StringBuffer result, Resolver resolver, DeclaringComponent currentComponent) {
        if (m.group("escaped") != null) {
            m.appendReplacement(result, quoteReplacement(m.group("placeholder")));
            return;
        }

        String resolved = resolve(m.group(), currentComponent, resolver);
        if (resolved == null) return;

        String finalValue = addOffsetForMultiLineValue(resolved, m);
        m.appendReplacement(result, quoteReplacement(finalValue));
    }

    public static boolean isValidPlaceholder(String value) {
        return findPlaceholderIn(value).isPresent();
    }

    private String resolve(String placeholder, DeclaringComponent currentComponent, Resolver resolver) {
        if (isValidPlaceholder(placeholder)) {
            return doResolve(placeholder, resolver, currentComponent);
        }

        String newFormat = "${this@" + placeholder.substring("${".length());
        if (!isValidPlaceholder(newFormat)) return null;
        try {
            return doResolve(newFormat, resolver, currentComponent);
        } catch (RuntimeException e) {
            return null;//todo warn
        }
    }

    private String doResolve(String placeholder, Resolver resolver, DeclaringComponent currentComponent) {
        return resolver.resolve(placeholder, currentComponent, currentComponent);
    }

    private String addOffsetForMultiLineValue(String value, Matcher m) {
        int lineBeginIndex = content.lastIndexOf('\n', m.start());
        int placeholderOffset = m.start() - lineBeginIndex - 1;
        return value.replace("\n", addOffsets("\n", placeholderOffset));
    }
}