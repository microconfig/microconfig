package io.microconfig.commands;

import static deployment.util.Logger.announce;
import static java.lang.System.currentTimeMillis;

public class LogTimeCommandDecorator {
    public static Command measure(Command command, String name) {
        return context -> {
            long t = currentTimeMillis();
            command.execute(context);
            announce("Executed " + name + " command in " + (currentTimeMillis() - t) + "ms");
        };
    }
}