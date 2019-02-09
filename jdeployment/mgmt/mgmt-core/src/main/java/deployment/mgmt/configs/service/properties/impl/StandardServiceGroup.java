package deployment.mgmt.configs.service.properties.impl;

import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public enum StandardServiceGroup {
    SERVICES,
    PATCHERS,
    TASKS,
    FAILED(true),
    STOPPED(true),
    CHANGED(true);

    private final boolean runtime;

    StandardServiceGroup() {
        this(false);
    }

    public String groupName() {
        return name().toLowerCase();
    }

    public boolean nameEquals(String group) {
        return name().equalsIgnoreCase(group);
    }

    public static List<StandardServiceGroup> runtimeGroups() {
        return of(values()).filter(g -> g.runtime).collect(toList());
    }
}