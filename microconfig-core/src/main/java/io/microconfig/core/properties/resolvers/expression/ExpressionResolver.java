package io.microconfig.core.properties.resolvers.expression;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.ResolveException;
import io.microconfig.core.properties.resolvers.RecursiveResolver;
import io.microconfig.core.properties.resolvers.expression.functions.CustomStringApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.core.properties.resolvers.expression.ExpressionEvaluator.withFunctionsFrom;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;

@RequiredArgsConstructor
public class ExpressionResolver implements RecursiveResolver {
    private final Pattern expressionPattern = compile("#\\{(?<value>[^{]+?)}");
    private final ExpressionEvaluator evaluator = withFunctionsFrom(CustomStringApi.class, System.class);

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        Matcher expressionMatcher = expressionPattern.matcher(line);
        return expressionMatcher.find() ? of(toExpression(expressionMatcher)) : empty();
    }

    private Expression toExpression(Matcher matcher) {
        return new Expression(matcher.group("value"), matcher.start(), matcher.end());
    }

    @RequiredArgsConstructor
    private class Expression implements Statement {
        private final String value;
        @Getter
        private final int startIndex;
        @Getter
        private final int endIndex;

        @Override
        public String resolveFor(DeclaringComponent component, DeclaringComponent root) {
            try {
                return evaluator.evaluate(value);
            } catch (RuntimeException e) {
                throw new ResolveException(component, root, "Can't evaluate " + this, e);
            }
        }

        @Override
        public String toString() {
            return "#{" + value + "}";
        }
    }
}