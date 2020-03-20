package io.microconfig.core.resolvers;

import io.microconfig.core.properties.ComponentWitsEnv;
import io.microconfig.core.properties.Resolver;

import java.util.Optional;

public interface RecursiveResolver extends Resolver {
    @Override
    default String resolve(CharSequence value,
                           ComponentWitsEnv currentComponent,
                           ComponentWitsEnv rootComponent,
                           String configType) {
        StringBuilder result = new StringBuilder(value);
        while (true) {
            Optional<Statement> optionalStatement = findStatementIn(result);
            if (!optionalStatement.isPresent()) break;

            Statement statement = optionalStatement.get();
            String resolved = statement.resolveFor(currentComponent, rootComponent, configType);
            result.replace(statement.getStartIndex(), statement.getEndIndex(), resolved);
        }
        return result.toString();
    }

    Optional<Statement> findStatementIn(CharSequence line);

    interface Statement {
        String resolveFor(ComponentWitsEnv currentComponent,
                          ComponentWitsEnv rootComponent,
                          String configType);

        int getStartIndex();

        int getEndIndex();
    }
}