package io.microconfig.core.resolvers.expression;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.resolvers.RecursiveResolver.Statement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ParseException;

import static io.microconfig.core.resolvers.expression.ExpressionEvaluator.withFunctionsFrom;

@RequiredArgsConstructor
public class Expression implements Statement {
    private static final ExpressionEvaluator evaluator = withFunctionsFrom(PredefinedFunctions.class);

    private final String value;
    @Getter
    private final int startIndex;
    @Getter
    private final int endIndex;

    @Override
    public String resolveFor(ConfigType _2, String _1) {
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