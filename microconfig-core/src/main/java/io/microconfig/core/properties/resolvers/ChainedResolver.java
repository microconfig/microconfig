package io.microconfig.core.properties.resolvers;

import io.microconfig.core.properties.Resolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.utils.StreamUtils.findFirstResult;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class ChainedResolver implements RecursiveResolver {
    public final List<RecursiveResolver> resolvers;

    public static Resolver chainOf(RecursiveResolver... resolvers) {
        return new ChainedResolver(asList(resolvers));
    }

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return findFirstResult(resolvers, r -> r.findStatementIn(line));
    }
}