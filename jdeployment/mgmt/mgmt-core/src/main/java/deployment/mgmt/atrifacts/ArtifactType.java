package deployment.mgmt.atrifacts;

public enum ArtifactType {
    JAR,
    POM,
    GZ;

    public String extension() {
        return "." + name().toLowerCase();
    }
}
