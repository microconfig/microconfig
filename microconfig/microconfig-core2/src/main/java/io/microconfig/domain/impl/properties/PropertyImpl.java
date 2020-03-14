package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.Resolver;
import io.microconfig.domain.Resolver.Statement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var";

    private final String key;
    @With
    private final String value;
    private final boolean temp;

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        return null;
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
            Optional<Statement> statement = resolver.findStatementIn(result);
            if (!statement.isPresent()) break;

            Statement s = statement.get();
            String resolved = s.resolve();
            result.replace(s.getStartIndex(), s.getEndIndex() + 1, resolved);
        }

        return withValue(result.toString());
    }
}