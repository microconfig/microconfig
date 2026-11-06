package io.microconfig.core.properties.resolvers.expression;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.ResolveException;
import io.microconfig.core.properties.resolvers.RecursiveResolver;
import io.microconfig.core.properties.resolvers.expression.functions.CustomStringApi;
import io.microconfig.core.properties.resolvers.expression.functions.CustomSystemApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.microconfig.core.properties.io.properties.PropertiesText.escape;
import static io.microconfig.core.properties.io.properties.PropertiesText.isPropertiesText;
import static io.microconfig.core.properties.io.properties.PropertiesText.unescape;
import static io.microconfig.core.properties.resolvers.expression.ExpressionEvaluator.withFunctionsFrom;
import static java.util.regex.Pattern.compile;

@RequiredArgsConstructor
public class ExpressionResolver implements RecursiveResolver {
    private static final Pattern EXPRESSION_PATTERN = compile("#\\{(?<value>[^{]+?)}");
    private final ExpressionEvaluator evaluator = withFunctionsFrom(CustomStringApi.class, CustomSystemApi.class);

    public static Stream<MatchResult> expressionsIn(CharSequence line) {
        return EXPRESSION_PATTERN.matcher(line).results();
    }

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return expressionsIn(line).findFirst().map(this::toExpression);
    }

    private Expression toExpression(MatchResult matcher) {
        return new Expression(matcher.group(1), matcher.start(), matcher.end());
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
                String expression = isPropertiesText(component) ? unescape(value) : value;
                String result = evaluator.evaluate(expression);
                return isPropertiesText(component) ? escape(result) : result;
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
