package io.microconfig.core.resolvers.placeholder;

import io.microconfig.core.configtypes.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Optional;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Integer.max;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static lombok.AccessLevel.PRIVATE;

@With(PRIVATE)
@RequiredArgsConstructor
class PlaceholderBorders {
    private final StringBuilder line;

    @Getter
    private final int startIndex;
    private final int configTypeEndIndex;
    private final int envIndex;
    private final int valueIndex;
    private final int defaultValueIndex;
    @Getter
    private final int endIndex;

    public static Optional<PlaceholderBorders> findPlaceholderIn(CharSequence line) {
        StringBuilder sb = line instanceof StringBuilder ? (StringBuilder) line : new StringBuilder(line);
        return new PlaceholderBorders(sb).searchOpenSign();
    }

    private PlaceholderBorders(StringBuilder line) {
        this(line, -1, -1, -1, -1, -1, -1);
    }

    private Optional<PlaceholderBorders> searchOpenSign() {
        int index = line.indexOf("${", startIndex);
        if (index >= 0) {
            return new PlaceholderBorders(line)
                    .withStartIndex(index)
                    .parseComponentName();
        }

        return empty();
    }

    private Optional<PlaceholderBorders> parseComponentName() {
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

        return empty();
    }

    private Optional<PlaceholderBorders> parseEnvName() {
        for (int i = envIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ']' && i + 1 < line.length() && line.charAt(i + 1) == '@') {
                return withValueIndex(i + 2).parseValue();
            }
            if (notAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return empty();
    }

    private Optional<PlaceholderBorders> parseValue() {
        for (int i = valueIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':') {
                return withDefaultValueIndex(i + 1).parseDefaultValue();
            }
            if (c == '}') {
                return of(withEndIndex(i + 1));
            }
            if (notAllowedSymbol(c) && c != '/' && c != '\\') {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return empty();
    }

    private Optional<PlaceholderBorders> parseDefaultValue() {
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
                    return of(withEndIndex(closeBracketLastIndex + 1));
                }
            }
        }

        return closeBracketLastIndex < 0 ? empty() : of(withEndIndex(closeBracketLastIndex + 1));
    }

    private boolean notAllowedSymbol(char c) {
        return !isLetterOrDigit(c) && c != '.' && c != '_' && c != '-';
    }

    private String getConfigType(ConfigType contextConfigType) {
        return configTypeEndIndex < 0 ? contextConfigType.getName() : line.substring(startIndex + 2, configTypeEndIndex + 1);
    }

    private String getComponent() {
        return line.substring(max(startIndex + 2, configTypeEndIndex + 3), envIndex < 0 ? valueIndex - 1 : envIndex - 1);
    }

    private String getEnvironment(String contextEnv) {
        return envIndex < 0 ? contextEnv : line.subSequence(envIndex, valueIndex - 2).toString();
    }

    private String getValue() {
        return line.substring(valueIndex, defaultValueIndex < 0 ? endIndex - 1 : defaultValueIndex - 1);
    }

    private String getDefaultValue() {
        return defaultValueIndex < 0 ? null : line.substring(defaultValueIndex, endIndex - 1);
    }

    Placeholder toPlaceholder(ConfigType contextConfigType, String contextEnv) {
        return new Placeholder(
                getConfigType(contextConfigType),
                getComponent(),
                getEnvironment(contextEnv),
                getValue(),
                getDefaultValue()
        );
    }

    @Override
    public String toString() {
        return startIndex < 0 ? "" : line.substring(startIndex, endIndex);
    }
}