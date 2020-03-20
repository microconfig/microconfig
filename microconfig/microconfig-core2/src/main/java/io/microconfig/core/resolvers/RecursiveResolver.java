package io.microconfig.core.resolvers;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.Resolver;

import java.util.Optional;

public interface RecursiveResolver extends Resolver {
    default String resolve(CharSequence value, String envContext, ConfigType configType) {
        StringBuilder result = new StringBuilder(value);
        while (true) {
            Optional<Statement> optionalStatement = findStatementIn(result);
            if (!optionalStatement.isPresent()) break;

            Statement statement = optionalStatement.get();
            String resolved = statement.resolve(envContext, configType);
            result.replace(statement.getStartIndex(), statement.getEndIndex(), resolved);
        }
        return result.toString();
    }

    Optional<Statement> findStatementIn(CharSequence line);

    interface Statement {
        String resolve(String env, ConfigType configType);

        int getStartIndex();

        int getEndIndex();
    }
}