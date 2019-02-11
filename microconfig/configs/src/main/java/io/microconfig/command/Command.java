package io.microconfig.command;

public interface Command {
    void execute(CommandContext context);
}
