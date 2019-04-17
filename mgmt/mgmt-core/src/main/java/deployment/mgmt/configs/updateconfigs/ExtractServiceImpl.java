package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static deployment.mgmt.utils.ZipUtils.unzip;
import static io.microconfig.utils.Logger.error;

@RequiredArgsConstructor
public class ExtractServiceImpl implements ExtractService {
    private final ClasspathService classpathService;

    @Override
    public void unzipArtifactIfNeeded(String service, ProcessProperties processProperties) {
        String extractToDir = processProperties.getMavenSettings().getExtractToDir();
        if (extractToDir == null) return;

        List<File> files = classpathService.classpathFor(service).current().asFiles();
        if (files.size() != 1) {
            error("Mgmt can unzip only a single artifact. Remove 'extractTo' setting or configure using a single artifact.");
        }
        unzip(new File(extractToDir));
    }

}
