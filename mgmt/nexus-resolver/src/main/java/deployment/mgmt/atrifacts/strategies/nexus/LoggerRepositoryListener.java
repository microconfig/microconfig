package deployment.mgmt.atrifacts.strategies.nexus;

import deployment.util.FileLogger;
import lombok.RequiredArgsConstructor;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;

@RequiredArgsConstructor
public class LoggerRepositoryListener implements RepositoryListener {
    private final FileLogger logger;


    public void artifactDeployed(RepositoryEvent event) {
        logger.info("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    public void artifactDeploying(RepositoryEvent event) {
        logger.info("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    public void artifactDescriptorInvalid(RepositoryEvent event) {
        logger.info("Invalid artifact descriptor for " + event.getArtifact() + ": "
                + event.getException().getMessage());
    }

    public void artifactDescriptorMissing(RepositoryEvent event) {
        logger.info("Missing artifact descriptor for " + event.getArtifact());
    }

    public void artifactInstalled(RepositoryEvent event) {
        logger.info("Installed " + event.getArtifact() + " to " + event.getFile());
    }

    public void artifactInstalling(RepositoryEvent event) {
        logger.info("Installing " + event.getArtifact() + " to " + event.getFile());
    }

    public void artifactResolved(RepositoryEvent event) {
        logger.info("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactDownloading(RepositoryEvent event) {
        logger.info("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactDownloaded(RepositoryEvent event) {
        logger.info("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
    }

    public void artifactResolving(RepositoryEvent event) {
        logger.info("Resolving artifact " + event.getArtifact());
    }

    public void metadataDeployed(RepositoryEvent event) {
        logger.info("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    public void metadataDeploying(RepositoryEvent event) {
        logger.info("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    public void metadataInstalled(RepositoryEvent event) {
        logger.info("Installed " + event.getMetadata() + " to " + event.getFile());
    }

    public void metadataInstalling(RepositoryEvent event) {
        logger.info("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    public void metadataInvalid(RepositoryEvent event) {
        logger.info("Invalid metadata " + event.getMetadata());
    }

    public void metadataResolved(RepositoryEvent event) {
        logger.info("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    public void metadataResolving(RepositoryEvent event) {
        logger.info("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
    }

    @Override
    public void metadataDownloaded(RepositoryEvent event) {
    }
}
