package io.microconfig.core.properties.resolvers.expression;

import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.stream.Stream.of;
import static org.springframework.expression.spel.support.SimpleEvaluationContext.forReadOnlyDataBinding;

@RequiredArgsConstructor
class ExpressionEvaluator {
    private final ExpressionParser parser;
    private final EvaluationContext context;

    public static ExpressionEvaluator withFunctionsFrom(Class<?>... functionClasses) {
//        EvaluationContext context = forReadOnlyDataBinding()
//                .withInstanceMethods()
//                .build();
        EvaluationContext context = new StandardEvaluationContext();

        of(functionClasses)
                .flatMap(c -> of(c.getMethods()))
                .filter(m -> isStatic(m.getModifiers()))
                .forEach(m -> context.setVariable(m.getName(), m));

        return new ExpressionEvaluator(new SpelExpressionParser(), context);
    }

    public String evaluate(String value) {
        return parser.parseExpression(value).getValue(context, String.class);
    }
}