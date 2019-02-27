package io.microconfig.commands;

import static java.util.stream.Stream.of;

public interface Command {
    void execute(CommandContext context);

    static Command composite(Command... commands) {
        return context -> of(commands).forEach(c -> c.execute(context));
    }
}