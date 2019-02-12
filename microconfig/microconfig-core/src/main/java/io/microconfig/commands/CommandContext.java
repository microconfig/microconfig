package io.microconfig.commands;

import lombok.Getter;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

@Getter
public class CommandContext {
    private final String env;
    private final Optional<String> componentGroup;
    private final List<String> components;

    public CommandContext(String env, List<String> components) {
        this(env, empty(), components);
    }

    public CommandContext(String env, Optional<String> componentGroup, List<String> components) {
        this.env = requireNonNull(env, "Env is null");
        this.componentGroup = requireNonNull(componentGroup, "Component group is null");
        this.components = requireNonNull(components);
    }
}