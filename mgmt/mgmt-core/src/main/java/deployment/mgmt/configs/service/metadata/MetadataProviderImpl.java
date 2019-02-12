package deployment.mgmt.configs.service.metadata;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import io.microconfig.utils.FileUtils;
import io.microconfig.utils.IoUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static io.microconfig.utils.FileUtils.write;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class MetadataProviderImpl implements MetadataProvider {
    private static final String LAST_ERROR_ST = "lastError.mgmt";

    private final DeployFileStructure deployFileStructure;

    @Override
    public Optional<Long> lastPid(String service) {
        return of(deployFileStructure.service().getPidFile(service))
                .filter(File::exists)
                .map(IoUtils::readFirstLine)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf);
    }

    @Override
    public void storePid(String service, long pid) {
        write(deployFileStructure.service().getPidFile(service), String.valueOf(pid));
    }

    @Override
    public void deletePid(String service) {
        FileUtils.delete(deployFileStructure.service().getPidFile(service));
    }

    @Override
    public boolean isLastRunSucceed(String service) {
        return !deployFileStructure.process().getProcessFile(service, LAST_ERROR_ST).exists();
    }

    @Override
    public void onRunFailed(String service, Exception e) {
        File file = deployFileStructure.process().createProcessFile(service, LAST_ERROR_ST);

        try (PrintWriter writer = new PrintWriter(file)) {
            e.printStackTrace(writer);
        } catch (IOException ignore) {
        }
    }

    @Override
    public void updateLastRunStatus(String service, boolean success) {
        deployFileStructure.process().removeProcessFile(service, LAST_ERROR_ST);

        if (!success) {
            deployFileStructure.process().createProcessFile(service, LAST_ERROR_ST);
        }
    }
}