package io.microconfig.commands;

import java.util.stream.Stream;

public interface Command {
    void execute(CommandContext context);

    static Command composite(Command... commands) {
        return context -> Stream.of(commands).forEach(c -> c.execute(context));
    }
}