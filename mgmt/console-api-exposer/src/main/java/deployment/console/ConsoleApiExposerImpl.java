package deployment.console;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static io.microconfig.utils.ConsoleColor.green;
import static io.microconfig.utils.ConsoleColor.yellow;
import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.StringUtils.toLowerHyphen;
import static java.lang.System.arraycopy;
import static java.lang.System.exit;
import static java.util.Arrays.copyOfRange;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ConsoleApiExposerImpl implements ConsoleApiExposer {
    private final List<?> services;

    public static ConsoleApiExposer expose(Object... services) {
        return new ConsoleApiExposerImpl(List.of(services));
    }

    @Override
    public void invoke(String[] command) {
        if (command.length > 0) {
            for (Object service : services) {
                Method method = chooseMethod(service.getClass(), command);
                if (method == null) continue;

                invoke(service, method, command);
                return;
            }
        }

        printHelpAndExit();
    }

    @Override
    public Stream<String> getCommandNames() {
        return allMethodsInfo(false);
    }

    private Method chooseMethod(Class<?> clazz, String[] command) {
        String methodName = command[0].replace("-", "").replace("_", "");

        return allMethods(clazz)
                .filter(m -> m.getName().equalsIgnoreCase(methodName))
                .min(comparing(m -> Math.abs(m.getParameterCount() - (command.length - 1))))
                .orElse(null);
    }

    private void invoke(Object service, Method method, String[] args) {
        try {
            method.setAccessible(true);

            if (method.getParameterCount() == 0) {
                method.invoke(service);
                return;
            }

            invokeWithParams(service, method, copyOfRange(args, 1, args.length));
        } catch (IllegalAccessException | IllegalArgumentException e) {
            error("Exception during execution: " + (toLowerHyphen(method.getName()) + " " + argsInfo(method)) + ", " + e.getMessage());
        } catch (InvocationTargetException e) {
            error(e.getCause());
        } finally {
            if (isErrorOccurred()) {
                exit(-1);
            }
        }
    }

    private void invokeWithParams(Object service, Method method, String[] args) throws IllegalAccessException, InvocationTargetException {
        if (method.getParameterCount() == 1) {
            Class<?> argType = method.getParameterTypes()[0];
            if (argType == boolean.class) {
                method.invoke(service, Boolean.valueOf(args[0]));
            } else if (argType.isArray()) {
                method.invoke(service, (Object) args);
            } else {
                method.invoke(service, (Object[]) args);
            }
        } else if (method.getParameterTypes()[method.getParameterCount() - 1].isArray()) {
            Object[] params = copyOfRange(args, 0, method.getParameterCount() - 1);
            Object[] lastArraysParam = copyOfRange(args, method.getParameterCount() - 1, args.length);

            Object[] result = new Object[method.getParameterCount()];
            arraycopy(params, 0, result, 0, params.length);
            result[result.length - 1] = lastArraysParam;
            method.invoke(service, result);
        } else {
            method.invoke(service, (Object[]) args);
        }
    }

    private Stream<Method> allMethods(Class<?> clazz) {
        Stream<Class<?>> s = clazz.isInterface() ? of(clazz) : of(clazz.getInterfaces());
        return s.flatMap(c -> of(c.getMethods()));
    }

    private Stream<String> allMethodsInfo(boolean withParams) {
        return services.stream()
                .flatMap(s -> allMethods(s instanceof Class ? (Class<?>) s : s.getClass()))
                .filter(m -> !m.isAnnotationPresent(Hidden.class))
                .sorted(comparing(m -> m.isAnnotationPresent(ConsoleOrder.class) ? m.getAnnotation(ConsoleOrder.class).value() : 100))
                .map(m -> toLowerHyphen(m.getName()) + (withParams ? (" " + argsInfo(m)) : ""));
    }

    private String argsInfo(Method m) {
        return m.getParameters().length == 0 ? "" : of(m.getParameters())
                .map(p -> p.isAnnotationPresent(ConsoleParam.class) ? consoleParamInfo(p.getAnnotation(ConsoleParam.class)) : p.getName())
                .map(arg -> "${" + arg + "}")
                .collect(joining(" "));
    }

    private String consoleParamInfo(ConsoleParam consoleParam) {
        return (consoleParam.optional() ? yellow("@Optional ") : "")
                + (!consoleParam.defaultValue().isEmpty() ? yellow("@Default(\"" + consoleParam.defaultValue() + "\") ") : "")
                + consoleParam.value();
    }

    private void printHelpAndExit() {
        announce("available commands: ");
        List<String> methodInfo = allMethodsInfo(true).collect(toList());
        for (int i = 0; i < methodInfo.size(); ++i) {
            int number = i + 1;
            info(green((number < 10 ? " " + number : number) + ") ") + methodInfo.get(i));
        }

        exit(-1);
    }
}