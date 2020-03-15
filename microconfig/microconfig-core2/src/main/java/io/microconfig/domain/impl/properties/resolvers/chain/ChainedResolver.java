package io.microconfig.domain.impl.properties.resolvers.chain;

import io.microconfig.domain.StatementResolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.findFirst;
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
        return findFirst(resolvers, r -> r.findStatementIn(line));
    }
}