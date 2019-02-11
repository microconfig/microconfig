package deployment.mgmt.update.restarter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static deployment.util.Logger.announce;
import static deployment.util.ProcessUtil.currentJavaPath;
import static deployment.util.ProcessUtil.startAndWait;
import static java.util.Arrays.asList;

public class RestarterImpl implements Restarter {
    public static final String UPDATE = "upgrade";
    private volatile String[] registeredCommand;

    public void restart(File jar, String[] command) {
        command = chooseCommand(command);

        announce("Restarting mgmt to apply changes, args: " + Arrays.toString(command) + "\n");

        List<String> args = new ArrayList<>();
        args.add(currentJavaPath());
        args.add("-jar");
        args.add("-D" + UPDATE + "=true");
        args.add(jar.getAbsolutePath());
        if (command.length > 0) {
            args.addAll(asList(command));
        }

        System.exit(startAndWait(new ProcessBuilder(args).inheritIO()));
    }

    private String[] chooseCommand(String[] command) {
        String[] registeredCommand = this.registeredCommand;
        return registeredCommand == null ? command : registeredCommand;
    }

    @Override
    public void registerRestartCommand(String[] command) {
        this.registeredCommand = command;
    }
}