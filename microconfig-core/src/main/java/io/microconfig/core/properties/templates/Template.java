package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void copyTo(File destinationFile){
        write(destinationFile, content);
        copyPermissions(source.toPath(), destinationFile.toPath());
        info("Copied template: " + source + " -> " + destinationFile);
    }

    private void doResolve(Matcher m, StringBuffer result, Resolver resolver, DeclaringComponent currentComponent) {
        if (m.group("escaped") != null) {
            m.appendReplacement(result, quoteReplacement(m.group("placeholder")));
            return;
        }

        String placeholder = m.group();
        String value = resolve(placeholder, currentComponent, resolver);
        if (value == null) return;

        String finalValue = addOffsetForMultiLineValue(m, value);
        m.appendReplacement(result, quoteReplacement(finalValue));
    }

    private String addOffsetForMultiLineValue(Matcher m, String value) {
        int lineBeginIndex = content.lastIndexOf("\n", m.start());
        int placeholderOffset = m.start() - lineBeginIndex - 1;
        return value.replace("\n", addOffsets("\n", placeholderOffset));
    }

    private String resolve(String placeholder, DeclaringComponent currentComponent, Resolver resolver) {
        boolean microconfigFormatPlaceholder = isValidPlaceholder(placeholder);
        if (!microconfigFormatPlaceholder) {
            String newFormat = "${this@" + placeholder.substring("${".length());
            if (!isValidPlaceholder(newFormat)) return null;
            placeholder = newFormat;
        }

        try {
            return doResolve(placeholder, resolver, currentComponent);
        } catch (RuntimeException e) {
            if (microconfigFormatPlaceholder) {
                warn("Template placeholder error: " + e.getMessage());
            }
            return null;
        }
    }

    public static boolean isValidPlaceholder(String value) {
//        return Placeholder.parse(value).isValid();
        return false;
    }

    private String doResolve(String placeholder, Resolver resolver, DeclaringComponent currentComponent) {
        return resolver.resolve(placeholder, currentComponent, currentComponent);
    }
}