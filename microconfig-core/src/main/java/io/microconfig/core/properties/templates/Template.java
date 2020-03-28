package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;
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
import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.Logger.warn;
import static io.microconfig.utils.StringUtils.addOffsets;
import static java.util.regex.Matcher.quoteReplacement;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
class Template {
    private final File source;
    private final Pattern pattern;
    @With(PRIVATE)
    @Getter
    private final String content;

     Template(File source, Pattern pattern) {
        if (!source.exists() || !source.isFile()) {
            throw new IllegalStateException("Missing template file: " + this);
        }
        this.source = source;
        this.pattern = pattern;
        this.content = readFully(source);
    }

    public Template resolveBy(Resolver resolver, DeclaringComponent currentComponent) {
        Matcher m = pattern.matcher(content);
        if (!m.find()) return this;

        StringBuffer result = new StringBuffer();
        do {
            doResolve(m, result, resolver, currentComponent);
        } while (m.find());
        m.appendTail(result);
        return withContent(result.toString());
    }

    public void copyTo(File destinationFile) {
        write(destinationFile, content);
        copyPermissions(source.toPath(), destinationFile.toPath());
        info("Copied template: " + source + " -> " + destinationFile);
    }

    private void doResolve(Matcher m, StringBuffer result, Resolver resolver, DeclaringComponent currentComponent) {
        if (m.group("escaped") != null) {
            m.appendReplacement(result, quoteReplacement(m.group("placeholder")));
            return;
        }

        String placeholder = normalize(m.group());
        if (placeholder == null) return;
        String resolved = resolve(placeholder, currentComponent, resolver);

        String finalValue = addOffsetForMultiLineValue(resolved, m);
        m.appendReplacement(result, quoteReplacement(finalValue));
    }

    private String normalize(String placeholder) {
        if (isValidPlaceholder(placeholder)) return placeholder;

        String newFormat = "${this@" + placeholder.substring("${".length());
        if (isValidPlaceholder(newFormat)) return newFormat;
        return null;
    }

    public static boolean isValidPlaceholder(String value) {
        return findPlaceholderIn(value).isPresent();
    }

    private String resolve(String placeholder, DeclaringComponent currentComponent, Resolver resolver) {
        try {
            return doResolve(placeholder, resolver, currentComponent);
        } catch (RuntimeException e) {
            warn("Template placeholder error: " + e.getMessage()); //todo test
            return null;
        }
    }

    private String doResolve(String placeholder, Resolver resolver, DeclaringComponent currentComponent) {
        return resolver.resolve(placeholder, currentComponent, currentComponent);
    }

    private String addOffsetForMultiLineValue(String value, Matcher m) {
        int lineBeginIndex = content.lastIndexOf("\n", m.start());
        int placeholderOffset = m.start() - lineBeginIndex - 1;
        return value.replace("\n", addOffsets("\n", placeholderOffset));
    }
}