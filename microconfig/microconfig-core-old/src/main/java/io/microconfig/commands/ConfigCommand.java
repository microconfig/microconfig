package io.microconfig.commands;

import static java.util.stream.Stream.of;

public interface ConfigCommand {
    void execute(ComponentsToProcess context);

    static ConfigCommand composite(ConfigCommand... configCommands) {
        return context -> of(configCommands).forEach(c -> c.execute(context));
    }
}