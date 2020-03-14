package io.microconfig.domain;

import java.util.Optional;

public interface Resolver {
    Optional<Statement> findStatementIn(CharSequence line);

    default String resolve(CharSequence line) {
        StringBuilder result = new StringBuilder(line);
        while (true) {
            Optional<Statement> optionalStatement = findStatementIn(result);
            if (!optionalStatement.isPresent()) break;

            Statement statement = optionalStatement.get();
            String resolved = statement.resolve();
            result.replace(statement.getStartIndex(), statement.getEndIndex(), resolved);
        }
        return result.toString();
    }

    interface Statement {
        int getStartIndex();

        int getEndIndex();

        String resolve();
    }
}