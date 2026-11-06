package io.microconfig.core.properties.resolvers.expression.functions;

/**
 * The only System functions available in expressions: the rest of System (exit, load, setProperty, ...) must not be callable from configs.
 */
public class CustomSystemApi {
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static String lineSeparator() {
        return System.lineSeparator();
    }
}
