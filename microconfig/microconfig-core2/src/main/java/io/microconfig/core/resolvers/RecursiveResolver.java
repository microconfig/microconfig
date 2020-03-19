package io.microconfig.core.resolvers;

import io.microconfig.core.properties.Resolver;

import java.util.Optional;

public interface RecursiveResolver extends Resolver {
    default String resolve(CharSequence line, String env, String configType) {
        StringBuilder result = new StringBuilder(line);
        while (true) {
            Optional<Statement> optionalStatement = findStatementIn(result);
            if (!optionalStatement.isPresent()) break;

            Statement statement = optionalStatement.get();
            String resolved = statement.resolve(env, configType);
            result.replace(statement.getStartIndex(), statement.getEndIndex(), resolved);
        }
        return result.toString();
    }

    Optional<Statement> findStatementIn(CharSequence line);

    interface Statement {
        String resolve(String env, String configType);

        int getStartIndex();

        int getEndIndex();
    }
}