package io.microconfig.domain.impl.properties.repository.graph;

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
        return "Folder for component '" + notFoundComponent + "' doesn't exist. " +
                "Path to component: " + join(" -> ", path);
    }
}
