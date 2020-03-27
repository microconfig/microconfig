package io.microconfig.core.resolvers;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Resolver;

import java.util.Optional;

public interface RecursiveResolver extends Resolver {
    @Override
    default String resolve(String value, DeclaringComponent sourceOfValue, DeclaringComponent root) {
        StringBuilder result = new StringBuilder(value);
        while (true) {
            Optional<Statement> optionalStatement = findStatementIn(result);
            if (!optionalStatement.isPresent()) break;

            Statement statement = optionalStatement.get();
            String resolved = statement.resolveFor(sourceOfValue, root);
            result.replace(statement.getStartIndex(), statement.getEndIndex(), resolved);
        }
        return result.toString();
    }

    Optional<Statement> findStatementIn(CharSequence line);

    interface Statement {
        int getStartIndex();

        int getEndIndex();

        String resolveFor(DeclaringComponent sourceOfValue, DeclaringComponent root);
    }
}