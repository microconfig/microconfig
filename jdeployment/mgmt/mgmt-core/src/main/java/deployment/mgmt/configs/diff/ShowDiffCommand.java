package deployment.mgmt.configs.diff;

public interface ShowDiffCommand {
    void showPropDiff(String... service);

    void showClasspathDiff(String... services);

    void printProperties(String name);
}
