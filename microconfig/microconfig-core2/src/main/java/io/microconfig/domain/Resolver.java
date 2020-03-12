package io.microconfig.domain;

public interface Resolver {
    Expression parse(CharSequence value);

    interface Expression {
        int getStartIndex();

        int getEndIndex();

        String resolve();
    }
}