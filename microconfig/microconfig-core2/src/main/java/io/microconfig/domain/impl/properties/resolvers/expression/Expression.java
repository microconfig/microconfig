package io.microconfig.domain.impl.properties.resolvers.expression;

import io.microconfig.domain.Resolver.Statement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@RequiredArgsConstructor
public class Expression implements Statement {
    private static final ExpressionParser parser = new SpelExpressionParser();

    private final String value;
    @Getter
    private final int startIndex;
    @Getter
    private final int endIndex;

    @Override
    public String resolve() {
        try {
            return parser.parseExpression(value).getValue(String.class);
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException(e);//todo
        }
    }

    @Override
    public String toString() {
        return "#{" + value + "}";
    }
}
