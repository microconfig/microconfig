package io.microconfig.core.resolvers;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.ComponentDescription;
import io.microconfig.core.properties.Resolver;

import java.util.Optional;

public interface RecursiveResolver extends Resolver {
    @Override
    default String resolve(CharSequence value,
                           ComponentDescription currentComponent,
                           ComponentDescription rootComponent,
                           ConfigType configType) {
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
        String resolveFor(ComponentDescription currentComponent,
                          ComponentDescription rootComponent,
                          ConfigType configType);

        int getStartIndex();

        int getEndIndex();
    }
}