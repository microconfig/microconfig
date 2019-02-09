package deployment.console;

import java.util.stream.Stream;

public interface ConsoleApiExposer {
    void invoke(String[] command);

    Stream<String> getCommandNames();
}