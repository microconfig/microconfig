package io.microconfig.domain.impl.properties.resolvers.el;

import io.microconfig.domain.Resolver;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;

@RequiredArgsConstructor
public class ExpressionResolver implements Resolver {
    private static final Pattern PATTERN = compile("#\\{(?<value>[^{]+?)}");

    @Override
    public Optional<Statement> findStatementIn(CharSequence value) {
        Matcher matcher = PATTERN.matcher(value);
        return matcher.find() ?
                of(new Expression(matcher.group("value"), matcher.start(), matcher.end())) :
                empty();
    }

}