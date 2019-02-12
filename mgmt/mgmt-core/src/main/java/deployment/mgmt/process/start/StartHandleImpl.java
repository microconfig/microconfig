package deployment.mgmt.process.start;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.metadata.MetadataProvider;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import static deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl.emptyProperties;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.TimeUtils.calcSecFrom;
import static java.lang.System.currentTimeMillis;
import static mgmt.utils.ProcessUtil.*;
import static mgmt.utils.ThreadUtils.sleepSec;

@RequiredArgsConstructor
public class StartHandleImpl implements StartHandle {
    private static final int INFINITE_WAIT = Integer.MAX_VALUE;

    @Getter
    private final String serviceName;
    private final ProcessBuilder processBuilder;
    private final ProcessProperties processProperties;
    private final DeployFileStructure deployFileStructure;
    private final MetadataProvider metadataProvider;

    private Process process;
    private long startTime;
    @Getter
    private Exception exception;

    static StartHandle errorResult(String service, RuntimeException e) {
        StartHandleImpl result = new StartHandleImpl(service, null, emptyProperties(), null, null);
        result.exception = e;
        return result;
    }

    @Override
    public void executedCmdLine() {
        try {
            startTime = currentTimeMillis();

            if (exception != null) return;

            metadataProvider.deletePid(serviceName);
            process = startProcess(processBuilder);
            announce("Executed cmd line for " + serviceName);

            if (processProperties.writePid()) {
                metadataProvider.storePid(serviceName, process.pid());
            }
        } catch (RuntimeException e) {
            this.exception = e;
            metadataProvider.onRunFailed(serviceName, e);
        }
    }

    @Override
    public boolean awaitStartAndGetStatus() {
        if (exception != null) return false;

        try {
            int startTimeoutInSec = getStartTimeout();
            Optional<Long> lastPid = metadataProvider.lastPid(serviceName);//we reread pid, if process stores pid by itself
            if (lastPid.isPresent()) {
                waitStatusByPid(lastPid.orElseThrow(), startTimeoutInSec);
            } else if (!processProperties.writePid()) {
                simpleSleep(startTimeoutInSec);
            }

            boolean success = success(process);
            metadataProvider.updateLastRunStatus(serviceName, success);
            return success;
        } catch (RuntimeException e) {
            this.exception = e;
            metadataProvider.onRunFailed(serviceName, e);
            return false;
        }
    }

    private void waitStatusByPid(long lastPid, int startTimeoutInSec) {
        Optional<ProcessHandle> processHandle = ProcessHandle.of(lastPid);
        if (!processHandle.isPresent()) return;
        logWaiting(startTimeoutInSec);

        Set<String> logMarkers = processProperties.getHealthCheckSettings().getLogMarkers();
        String logFileName = processProperties.getLogFileName(serviceName);
        if (startTimeoutInSec != INFINITE_WAIT && !logMarkers.isEmpty() && logFileName != null) {
            File logFile = deployFileStructure.logs().getLogFile(serviceName, logFileName);
            waitTerminationOrLogMarker(processHandle.get(), logFile, logMarkers, startTime, startTimeoutInSec);
            return;
        }

        waitTermination(processHandle.get(), startTime, startTimeoutInSec);
    }

    //todo
    private void simpleSleep(int timeoutInSec) {
        int sleepTime = Math.min(15, timeoutReminder(timeoutInSec));
        info("Executing simple sleep for " + serviceName + " " + sleepTime + " sec, cause pid file hasn't been created");
        sleepSec(sleepTime);
    }

    private boolean success(Process process) {
        if (processProperties.isPatcher() || processProperties.isWebapp()) return process.exitValue() == 0;
        if (processProperties.isTask()) return process.isAlive() || process.exitValue() == 0;
        if (processProperties.writePid()) return process.isAlive();

        return metadataProvider.lastPid(serviceName)
                .flatMap(ProcessHandle::of)
                .map(ProcessHandle::isAlive)
                .orElse(false);
    }

    private int getStartTimeout() {
        return processProperties.isPatcher() || processProperties.isWebapp() ? INFINITE_WAIT : processProperties.getStartWaitSec();
    }

    private void logWaiting(int startTimeoutInSec) {
        if (timeoutReminder(startTimeoutInSec) <= 0) return;

        if (startTimeoutInSec == INFINITE_WAIT) {
            info("Waiting for " + serviceName + " execution");
        } else {
            info("Waiting for " + serviceName + " status at max " + startTimeoutInSec + " sec");
        }
    }

    private int timeoutReminder(int startTimeoutInSec) {
        return startTimeoutInSec - calcSecFrom(startTime);
    }

    @Override
    public ProcessProperties getProcessProperties() {
        return processProperties;
    }
}