package io.microconfig.domain;

import java.util.Optional;

public interface StatementResolver {
    default String resolveRecursively(CharSequence line) {
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

    Optional<Statement> findStatementIn(CharSequence line);

    interface Statement {
        String resolve();

        int getStartIndex();

        int getEndIndex();
    }
}