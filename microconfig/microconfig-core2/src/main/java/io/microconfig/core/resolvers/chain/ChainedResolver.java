package io.microconfig.core.resolvers.chain;

import io.microconfig.core.properties.StatementResolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.firstFirstResult;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class ChainedResolver implements StatementResolver {
    public final List<StatementResolver> resolvers;

    public static StatementResolver chainOf(StatementResolver... resolvers) {
        return new ChainedResolver(asList(resolvers));
    }

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return firstFirstResult(resolvers, r -> r.findStatementIn(line));
    }
}