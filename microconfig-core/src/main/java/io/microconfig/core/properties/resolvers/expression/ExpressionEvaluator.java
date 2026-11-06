package io.microconfig.core.properties.resolvers.expression;

import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.lang.reflect.Method;
import java.util.List;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.springframework.expression.spel.SpelMessage.CONSTRUCTOR_NOT_FOUND;
import static org.springframework.expression.spel.SpelMessage.FUNCTION_NOT_DEFINED;
import static org.springframework.expression.spel.SpelMessage.TYPE_NOT_FOUND;

@RequiredArgsConstructor
class ExpressionEvaluator {
    private final ExpressionParser parser;
    private final EvaluationContext context;
    private final String functionNames;

    // no type references T(...), constructors, static methods or methods of reflection objects, so an expression can't run arbitrary code;
    // instance methods of values ('a-b'.split('-')) and the registered #functions stay available
    public static ExpressionEvaluator withFunctionsFrom(Class<?>... functionClasses) {
        List<Method> functions = staticMethodsOf(functionClasses);
        EvaluationContext context = contextWith(functions);
        return new ExpressionEvaluator(new SpelExpressionParser(), context, namesOf(functions));
    }

    private static List<Method> staticMethodsOf(Class<?>... classes) {
        return of(classes)
                .flatMap(c -> of(c.getMethods()))
                .filter(m -> isStatic(m.getModifiers()))
                .collect(toList());
    }

    private static EvaluationContext contextWith(List<Method> functions) {
        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withMethodResolvers(new ValueMethodResolver())
                .build();
        functions.forEach(f -> context.setVariable(f.getName(), f));
        return context;
    }

    private static String namesOf(List<Method> functions) {
        return functions.stream()
                .map(f -> "#" + f.getName())
                .distinct()
                .sorted()
                .collect(joining(", "));
    }

    public String evaluate(String value) {
        try {
            return requireNonNull(parser.parseExpression(value).getValue(context, String.class), "Expression evaluated to null");
        } catch (SpelEvaluationException e) {
            throw withHint(e);
        }
    }

    // configs written for older versions can use Java types or functions that aren't available in expressions anymore
    private RuntimeException withHint(SpelEvaluationException e) {
        if (e.getMessageCode() == TYPE_NOT_FOUND || e.getMessageCode() == CONSTRUCTOR_NOT_FOUND) {
            return new IllegalArgumentException(e.getMessage() + ". Java types can't be used in expressions: T(...) and new aren't supported", e);
        }
        if (e.getMessageCode() == FUNCTION_NOT_DEFINED) {
            return new IllegalArgumentException(e.getMessage() + ". " + replacementOf(e.getInserts()[0]) + "Available functions: " + functionNames, e);
        }
        return e;
    }

    private String replacementOf(Object removedFunction) {
        if ("getenv".equals(removedFunction)) return "Use ${env@NAME} placeholder instead. ";
        if ("getProperty".equals(removedFunction)) return "Use ${system@name} placeholder instead. ";
        if (String.valueOf(removedFunction).startsWith("read")) return "Files can't be read in expressions. ";
        return "";
    }
}
