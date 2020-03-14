package io.microconfig.domain.impl.properties.resolvers.chain;

import io.microconfig.domain.Resolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class ChainedResolver implements Resolver {
    public final List<Resolver> resolvers;

    public static Resolver chainOf(Resolver... resolvers) {
        return new ChainedResolver(asList(resolvers));
    }

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return resolvers.stream()
                .map(r -> r.findStatementIn(line))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}