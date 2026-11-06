package io.microconfig.core.properties.io.yaml.impl;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.FileBasedComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.resolvers.RecursiveResolver;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerImpl;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.Token;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.PropertyImpl.literalProperty;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.io.yaml.YamlScalars.*;
import static io.microconfig.core.properties.resolvers.expression.ExpressionResolver.expressionsIn;
import static io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN;
import static org.yaml.snakeyaml.tokens.Token.ID.StreamEnd;

@RequiredArgsConstructor
public class YamlValueResolver implements Resolver {
    private final RecursiveResolver delegate;

    @Override
    public String resolve(String value, DeclaringComponent sourceOfValue, DeclaringComponent root) {
        return delegate.resolve(value, sourceOfValue, root);
    }

    @Override
    public String resolve(Property property, DeclaringComponent root) {
        if (!isYamlProperty(property)) return delegate.resolve(property, root);
        return isYamlCollection(property) ? resolveCollection(property, root) : resolveScalar(property, root);
    }

    private boolean isYamlProperty(Property property) {
        return property.getDeclaringComponent() instanceof FileBasedComponent
                && ((FileBasedComponent) property.getDeclaringComponent()).isYaml();
    }

    private boolean isYamlCollection(Property property) {
        return property.getConfigFormat() == YAML && !property.isLiteralText() && isCollection(property.getValue());
    }

    private String resolveCollection(Property property, DeclaringComponent root) {
        String original = property.getValue();
        if (delegate.findStatementIn(original).isEmpty()) return original;

        String template = collectionTemplate(original);
        StringBuilder result = new StringBuilder();
        int previousEnd = 0;
        for (ScalarToken token : scalarTokens(template)) {
            int start = template.offsetByCodePoints(0, token.getStartMark().getIndex());
            int end = template.offsetByCodePoints(0, token.getEndMark().getIndex());
            result.append(delegate.resolve(original.substring(previousEnd, start), property.getDeclaringComponent(), root));
            result.append(resolveToken(original.substring(start, end), token, property, root));
            previousEnd = end;
        }
        result.append(delegate.resolve(original.substring(previousEnd), property.getDeclaringComponent(), root));
        return result.toString();
    }

    // Mask unresolved expressions while scanning structure; restore quoted and block scalars
    // before the second scan so YAML decodes their text in its original indentation context.
    private String collectionTemplate(String original) {
        String masked = maskStatements(original);
        List<ScalarToken> tokens;
        while (true) {
            try {
                tokens = scalarTokens(masked);
                break;
            } catch (MarkedYAMLException e) {
                String parseable = maskSpliceLine(original, masked, e.getContextMark());
                if (parseable == null) parseable = maskSpliceLine(original, masked, e.getProblemMark());
                if (parseable == null) throw e;
                masked = parseable;
            }
        }
        StringBuilder result = new StringBuilder(masked);
        for (ScalarToken token : tokens) {
            if (token.getStyle() == PLAIN) continue;
            int start = masked.offsetByCodePoints(0, token.getStartMark().getIndex());
            int end = masked.offsetByCodePoints(0, token.getEndMark().getIndex());
            result.replace(start, end, original.substring(start, end));
        }
        return result.toString();
    }

    private String maskStatements(String original) {
        StringBuilder masked = new StringBuilder(original);
        while (true) {
            Optional<RecursiveResolver.Statement> statement = delegate.findStatementIn(masked);
            if (statement.isEmpty()) return masked.toString();

            RecursiveResolver.Statement current = statement.get();
            for (int i = current.getStartIndex(); i < current.getEndIndex(); i++) {
                char c = masked.charAt(i);
                if (c != '\n' && c != '\r') masked.setCharAt(i, 'x');
            }
        }
    }

    // A standalone native fragment may be invalid YAML until it is expanded. Keep it
    // outside scalar tokens; valid standalone elements in flow collections stay scalars.
    private String maskSpliceLine(String original, String masked, Mark mark) {
        if (mark == null) return null;
        int offset = masked.offsetByCodePoints(0, mark.getIndex());
        int start = masked.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        int end = masked.indexOf('\n', offset);
        if (end < 0) end = masked.length();
        String line = original.substring(start, end);
        Optional<RecursiveResolver.Statement> statement = delegate.findStatementIn(line);
        if (statement.isEmpty() || !isStandaloneStatementLine(line, statement.get())) return null;

        int statementStart = start + statement.get().getStartIndex();
        if (masked.charAt(statementStart) == '#') return null;
        StringBuilder result = new StringBuilder(masked);
        result.setCharAt(statementStart, '#');
        return result.toString();
    }

    private boolean isStandaloneStatementLine(CharSequence value, RecursiveResolver.Statement statement) {
        for (int i = statement.getStartIndex() - 1; i >= 0; i--) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r') break;
            if (!Character.isWhitespace(c)) return false;
        }
        for (int i = statement.getEndIndex(); i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r') break;
            if (c == '#' && i > statement.getEndIndex()) return true;
            if (!Character.isWhitespace(c)) return false;
        }
        return true;
    }

    private List<ScalarToken> scalarTokens(String template) {
        Scanner scanner = new ScannerImpl(new StreamReader(new StringReader(template)), new LoaderOptions());
        List<ScalarToken> result = new ArrayList<>();
        while (!scanner.checkToken(StreamEnd)) {
            Token token = scanner.getToken();
            if (token instanceof ScalarToken scalar) result.add(scalar);
        }
        return result;
    }

    private String resolveToken(String original, ScalarToken token, Property collection, DeclaringComponent root) {
        if (delegate.findStatementIn(original).isEmpty()) return original;

        String key = collection.getKey();
        DeclaringComponent source = collection.getDeclaringComponent();
        return switch (token.getStyle()) {
            case PLAIN -> resolveScalar(property(key, original, YAML, source), root);
            case SINGLE_QUOTED, DOUBLE_QUOTED, JSON_SCALAR_STYLE -> resolveScalar(property(key, doubleQuoted(token.getValue()), YAML, source), root);
            case LITERAL, FOLDED -> doubleQuoted(resolveScalar(literalProperty(key, token.getValue(), YAML, source), root))
                    + trailingLineBreak(original);
        };
    }

    private String trailingLineBreak(String original) {
        if (original.endsWith("\r\n")) return "\r\n";
        if (original.endsWith("\n")) return "\n";
        return original.endsWith("\r") ? "\r" : "";
    }

    private String resolveScalar(Property property, DeclaringComponent root) {
        boolean yamlSyntax = property.getConfigFormat() == YAML && !property.isLiteralText();
        boolean quoted = yamlSyntax && isQuoted(property.getValue());
        StringBuilder result = new StringBuilder(quoted ? unquote(property.getValue()) : property.getValue());
        boolean plainText = false;
        boolean stringValue = quoted;
        while (true) {
            Optional<RecursiveResolver.Statement> statement = delegate.findStatementIn(result);
            if (statement.isEmpty()) break;

            RecursiveResolver.Statement current = statement.get();
            Optional<MatchResult> expression = expressionContaining(result, current);
            if (expression.isPresent()) {
                MatchResult match = expression.get();
                String value = resolveExpression(result.substring(match.start(), match.end()), property, root);
                if (match.start() == 0 && match.end() == result.length()) stringValue = quoted;
                result.replace(match.start(), match.end(), value);
                plainText = true;
                continue;
            }
            String value = current.resolveFor(property.getDeclaringComponent(), root);
            boolean wholeValue = current.getStartIndex() == 0 && current.getEndIndex() == result.length();
            if (wholeValue) {
                plainText = false;
                stringValue = quoted;
            }
            if (!current.isPlainText() && (quoted || !yamlSyntax || !wholeValue) && isQuoted(value)) {
                value = unquote(value);
                stringValue = true;
            }
            plainText |= current.isPlainText();
            result.replace(current.getStartIndex(), current.getEndIndex(), value);
        }
        if (!yamlSyntax) return result.toString();
        return renderScalar(result.toString(), quoted, stringValue, plainText);
    }

    private Optional<MatchResult> expressionContaining(CharSequence value, RecursiveResolver.Statement statement) {
        return expressionsIn(maskPlaceholders(value))
                .filter(expression -> expression.start() <= statement.getStartIndex()
                        && statement.getEndIndex() <= expression.end())
                .findFirst();
    }

    private String maskPlaceholders(CharSequence value) {
        StringBuilder masked = new StringBuilder(value);
        while (true) {
            var placeholder = findPlaceholderIn(masked);
            if (placeholder.isEmpty()) return masked.toString();

            for (int i = placeholder.get().getStartIndex(); i < placeholder.get().getEndIndex(); i++) {
                char c = masked.charAt(i);
                if (c != '\n' && c != '\r') masked.setCharAt(i, 'x');
            }
        }
    }

    // An expression is evaluated before its result participates in YAML scalar serialization.
    private String resolveExpression(String expression, Property property, DeclaringComponent root) {
        StringBuilder result = new StringBuilder(expression);
        while (true) {
            Optional<RecursiveResolver.Statement> statement = delegate.findStatementIn(result);
            if (statement.isEmpty()) return result.toString();

            RecursiveResolver.Statement current = statement.get();
            String value = current.resolveFor(property.getDeclaringComponent(), root);
            if (current.getStartIndex() == 0 && current.getEndIndex() == result.length()) return value;
            char quote = expressionQuoteAt(result, current.getStartIndex());
            result.replace(current.getStartIndex(), current.getEndIndex(), expressionValue(value, current, quote));
        }
    }

    private char expressionQuoteAt(CharSequence expression, int offset) {
        char quote = 0;
        for (int i = 0; i < offset; i++) {
            char c = expression.charAt(i);
            if (c == '$' && i + 1 < offset && expression.charAt(i + 1) == '{') {
                var placeholder = findPlaceholderIn(expression, i);
                if (placeholder.isPresent() && placeholder.get().getStartIndex() == i) {
                    i = placeholder.get().getEndIndex() - 1;
                    continue;
                }
            }
            if (quote == 0 && (c == '\'' || c == '"')) {
                quote = c;
            } else if (c == quote) {
                if (i + 1 < offset && expression.charAt(i + 1) == quote) i++;
                else quote = 0;
            }
        }
        return quote;
    }

    private String expressionValue(String value, RecursiveResolver.Statement statement, char quote) {
        boolean yamlString = !statement.isPlainText() && isQuoted(value);
        String text = yamlString ? unquote(value) : value;
        if (quote != 0) return text.replace(String.valueOf(quote), String.valueOf(quote).repeat(2));
        return yamlString ? "'" + text.replace("'", "''") + "'" : value;
    }

    private String renderScalar(String value, boolean quoted, boolean stringValue, boolean plainText) {
        if (quoted) return doubleQuoted(value);
        if (stringValue) return stringValue(value);
        return plainText ? fromPlainText(value) : value;
    }
}
