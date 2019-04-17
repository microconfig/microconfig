package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static deployment.mgmt.utils.ZipUtils.unzip;
import static io.microconfig.utils.Logger.announce;

@RequiredArgsConstructor
public class ExtractServiceImpl implements ExtractService {
    private final ClasspathService classpathService;

    @Override
    public void unzipArtifactIfNeeded(String service, ProcessProperties processProperties) {
        String extractToDir = processProperties.getMavenSettings().getExtractToDir();
        if (extractToDir == null) return;

        List<File> files = classpathService.classpathFor(service).current().onlyServiceArtifacts().asFiles();
        announce("Extracting " + service + " artifact to " + extractToDir);
        unzip(files.get(0), new File(extractToDir));
    }
}
