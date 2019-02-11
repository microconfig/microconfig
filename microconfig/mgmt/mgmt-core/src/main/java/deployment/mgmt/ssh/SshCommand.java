package deployment.mgmt.ssh;

import io.microconfig.configs.environment.ComponentGroup;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface SshCommand {
    void ssh(String env, String group);

    void executeOn(String env, String group, String command);

    <T> List<T> executeOnEveryNode(String env, String command, BiFunction<ComponentGroup, String, T> outputTransformer);

    <T> List<T> executeOnEveryNode(String env, String command,
                                   Consumer<ComponentGroup> beforeUpdateListener, BiFunction<ComponentGroup, String, T> outputTransformer);
}