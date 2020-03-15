package io.microconfig.io.fsgraph;

import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;

import static java.lang.String.join;

@RequiredArgsConstructor
public class ComponentDoesNotExistException extends RuntimeException {
    private final String badComponentName;
    private final Deque<String> path = new ArrayDeque<>();

    public ComponentDoesNotExistException withParent(String component) {
        path.addFirst(component);
        return this;
    }

    @Override
    public String getMessage() {
        return "Folder for component '" + badComponentName + "' doesn't exist. " +
                "Path to component: " + join(" -> ", path);
    }
}
