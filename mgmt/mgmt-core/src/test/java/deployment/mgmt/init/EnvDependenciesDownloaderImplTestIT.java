package deployment.mgmt.init;

import deployment.mgmt.factory.MgmtFactory;

public class EnvDependenciesDownloaderImplTestIT {
    public static void main(String[] args) {
        EnvDependenciesDownloader envDependenciesDownloader = new MgmtFactory().newEnvDependenciesDownloader();
        envDependenciesDownloader.downloadDependencies("cr-dev6");
    }
}