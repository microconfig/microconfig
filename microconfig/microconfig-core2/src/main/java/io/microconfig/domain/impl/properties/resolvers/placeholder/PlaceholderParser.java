package io.microconfig.domain.impl.properties.resolvers.placeholder;

import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Optional;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Math.max;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@With(PRIVATE)
@RequiredArgsConstructor
public class PlaceholderParser {
    private static final PlaceholderParser EMPTY = new PlaceholderParser(new StringBuilder());
    private static final Placeholder NOT_VALID = new Placeholder(-1, -1, empty(), "", "", "", empty());

    private final StringBuilder line;

    private final int startIndex;
    private final int configTypeEndIndex;
    private final int envIndex;
    private final int valueIndex;
    private final int defaultValueIndex;
    private final int endIndex;

    static PlaceholderParser parse(CharSequence sequence) {
        StringBuilder line = sequence instanceof StringBuilder ? (StringBuilder) sequence : new StringBuilder(sequence);
        return new PlaceholderParser(line).searchOpenSign();
    }

    private PlaceholderParser(StringBuilder line) {
        this(line, -1, -1, -1, -1, -1, -1);
    }

    private PlaceholderParser searchOpenSign() {
        int index = line.indexOf("${", startIndex);
        if (index >= 0) {
            return new PlaceholderParser(line)
                    .withStartIndex(index)
                    .parseComponentName();
        }

        return EMPTY;
    }

    private PlaceholderParser parseComponentName() {
        for (int i = max(startIndex + 2, configTypeEndIndex + 3); i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':' && i + 1 < line.length() && line.charAt(i + 1) == ':') {
                return withConfigTypeEndIndex(i - 1).parseComponentName();
            }
            if (c == '[') {
                return withEnvIndex(i + 1).parseEnvName();
            }
            if (c == '@') {
                return withValueIndex(i + 1).parseValue();
            }
            if (notAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return EMPTY;
    }

    private PlaceholderParser parseEnvName() {
        for (int i = envIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ']' && i + 1 < line.length() && line.charAt(i + 1) == '@') {
                return withValueIndex(i + 2).parseValue();
            }
            if (notAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return EMPTY;
    }

    private PlaceholderParser parseValue() {
        for (int i = valueIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':') {
                return withDefaultValueIndex(i + 1).parseDefaultValue();
            }
            if (c == '}') {
                return withEndIndex(i);
            }
            if (notAllowedSymbol(c) && c != '/' && c != '\\') {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return EMPTY;
    }

    private PlaceholderParser parseDefaultValue() {
        int closeBracketLastIndex = -1;
        int openBrackets = 1;
        for (int i = defaultValueIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == '{') {
                char prevChar = line.charAt(i - 1);
                if (prevChar == '$' || prevChar == '#') {
                    ++openBrackets;
                }
                continue;
            }
            if (c == '}') {
                closeBracketLastIndex = i;
                if (--openBrackets == 0) {
                    return withEndIndex(closeBracketLastIndex);
                }
            }
        }

        return closeBracketLastIndex < 0 ? EMPTY : withEndIndex(closeBracketLastIndex);
    }

    private boolean notAllowedSymbol(char c) {
        return !isLetterOrDigit(c) && c != '.' && c != '_' && c != '-';
    }

    public Placeholder toPlaceholder(String contextEnv) {
        return startIndex < 0 ? NOT_VALID : new Placeholder(
                startIndex,
                endIndex,
                getConfigType(),
                getComponent(),
                getEnvironment(contextEnv),
                getValue(),
                getDefaultValue()
        );
    }

    private Optional<String> getConfigType() {
        return ofNullable(configTypeEndIndex < 0 ? null : line.substring(startIndex + 2, configTypeEndIndex + 1));
    }

    private String getComponent() {
        return line.substring(max(startIndex + 2, configTypeEndIndex + 3), envIndex < 0 ? valueIndex - 1 : envIndex - 1);
    }

    private String getEnvironment(String contextEnv) {
        return envIndex < 0 ? contextEnv : line.subSequence(envIndex, valueIndex - 2).toString();
    }

    private String getValue() {
        return line.substring(valueIndex, defaultValueIndex < 0 ? endIndex : defaultValueIndex - 1);
    }

    private Optional<String> getDefaultValue() {
        return ofNullable(defaultValueIndex < 0 ? null : line.substring(defaultValueIndex, endIndex));
    }
}