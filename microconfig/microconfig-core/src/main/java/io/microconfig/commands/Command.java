package io.microconfig.commands;

public interface Command {
    void execute(CommandContext context);
}
