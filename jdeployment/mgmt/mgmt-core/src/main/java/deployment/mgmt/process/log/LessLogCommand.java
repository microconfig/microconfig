package deployment.mgmt.process.log;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.filestructure.ServiceLogDirs;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.util.FileUtils.writeExecutable;
import static deployment.util.Logger.announce;
import static deployment.util.Logger.warn;
import static deployment.util.StringUtils.isEmpty;

@RequiredArgsConstructor
public class LessLogCommand implements LogCommand {
    private final PropertyService propertyService;
    private final DeployFileStructure deployFileStructure;

    @Override
    public void log(String service, String logFileName) {
        File logFile = findLogFile(service, logFileName);

        if (!logFile.exists()) {
            warn("Log file " + logFile + " doesn't exist.");
            return;
        }

        String command = "less -R+F " + logFile.getAbsolutePath();
        announce(command);
        writeExecutable(deployFileStructure.deploy().getPostMgmtScriptFile(), command);
    }

    private File findLogFile(String service, String log) {
        if (!isEmpty(log)) return deployFileStructure.logs().getLogFile(service, log);

        ServiceLogDirs logs = deployFileStructure.logs();

        String logFileName = propertyService.getProcessProperties(service).getLogFileName(service);
        File serviceLog = logs.getLogFile(service, logFileName);
        if (serviceLog.exists()) return serviceLog;

        File[] files = logs.getLogDir(service).listFiles(
                f -> f.getName().endsWith(".log") && service.contains(f.getName().replace(".log", ""))
        );
        if (files.length > 0) {
            return files[0];
        }

        File out = logs.getLogFile(service, "out.log");
        if (out.exists()) return out;

        return serviceLog;
    }
}