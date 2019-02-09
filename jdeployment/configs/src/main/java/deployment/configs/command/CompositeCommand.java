package deployment.configs.command;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CompositeCommand implements Command {
    private final List<Command> commands;

    @Override
    public void execute(CommandContext context) {
        commands.forEach(c -> c.execute(context));
    }
}
