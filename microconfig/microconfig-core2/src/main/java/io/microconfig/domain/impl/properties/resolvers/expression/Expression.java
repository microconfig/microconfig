package io.microconfig.domain.impl.properties.resolvers.expression;

import io.microconfig.domain.StatementResolver.Statement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ParseException;

import static io.microconfig.domain.impl.properties.resolvers.expression.ExpressionEvaluator.withFunctionsFrom;

@RequiredArgsConstructor
public class Expression implements Statement {
    private static final ExpressionEvaluator evaluator = withFunctionsFrom(PredefinedFunctions.class);

    private final String value;
    @Getter
    private final int startIndex;
    @Getter
    private final int endIndex;

    @Override
    public String resolve(String _1, String _2) {
        try {
            return evaluator.evaluate(value);
        } catch (EvaluationException | ParseException e) {
            throw new RuntimeException(e);//todo
        }
    }

    @Override
    public String toString() {
        return "#{" + value + "}";
    }
}