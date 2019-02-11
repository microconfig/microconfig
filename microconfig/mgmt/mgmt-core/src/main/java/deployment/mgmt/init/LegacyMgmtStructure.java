package deployment.mgmt.init;

public interface LegacyMgmtStructure {
    boolean containsDir(String dir);

    void deleteAll();
}
