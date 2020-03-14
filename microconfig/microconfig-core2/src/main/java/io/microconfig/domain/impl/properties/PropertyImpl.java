package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.Resolver;
import io.microconfig.domain.Resolver.Statement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var";

    private final String key;
    @With(PRIVATE)
    private final String value;
    private final boolean temp;

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        return null;
    }

    public static Property property(String key, String value) {
        return new PropertyImpl(key, value, false);
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return null;
    }

    public static boolean containsYamlProperties(List<Property> properties) {
        return true;
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    @Override
    public Property resolveBy(Resolver resolver) {
        StringBuilder result = new StringBuilder(value);

        while (true) {
            Optional<Statement> optionalStatement = resolver.findStatementIn(result);
            if (!optionalStatement.isPresent()) break;

            Statement statement = optionalStatement.get();
            String resolved = statement.resolve();
            result.replace(statement.getStartIndex(), statement.getEndIndex(), resolved);
        }

        return withValue(result.toString());
    }
}