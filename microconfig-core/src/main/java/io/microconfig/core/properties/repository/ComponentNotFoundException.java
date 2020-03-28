package io.microconfig.core.properties.repository;

import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;

import static java.lang.String.join;

@RequiredArgsConstructor
public class ComponentNotFoundException extends RuntimeException {
    private final String notFoundComponent;
    private final Deque<String> path = new ArrayDeque<>();

    public ComponentNotFoundException withParentComponent(String parent) {
        path.addFirst(parent);
        return this;
    }

    @Override
    public String getMessage() {
        return "Component '" + notFoundComponent + "' doesn't exist. " +
                "Dependency chain: " + join(" -> ", path);
    }
}