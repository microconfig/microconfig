package deployment.mgmt.process.stop;

import deployment.mgmt.configs.service.metadata.MetadataProvider;
import lombok.RequiredArgsConstructor;
import mgmt.utils.ProcessUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.System.currentTimeMillis;
import static mgmt.utils.ProcessUtil.waitTermination;

@RequiredArgsConstructor
public class StopHandle {
    private static final int WAIT_SEC = 20;

    private final String service;
    private final ProcessHandle processHandle;
    private final long stopTime = currentTimeMillis();

    public static Optional<StopHandle> create(String service, MetadataProvider metadataProvider) {
        return metadataProvider.lastPid(service)
                .flatMap(ProcessHandle::of)
                .filter(ProcessUtil::belongsToCurrentUser)
                .map(process -> new StopHandle(service, process));
    }

    //todo2 process.stop.exec
    public void stop() {
        info("Stopping " + service);

        Deque<ProcessHandle> allProcess = new ArrayDeque<>();
        collectChildProcess(processHandle, allProcess);
        allProcess.forEach(this::doStop);
    }

    private void collectChildProcess(ProcessHandle processHandle, Deque<ProcessHandle> destination) {
        destination.addFirst(processHandle);
        processHandle.children().forEach(c -> collectChildProcess(c, destination));
    }

    private void doStop(ProcessHandle processHandle) {
        processHandle.destroy();

        if (!processHandle.isAlive()) {
            logStopped(processHandle);
            return;
        }

        info("Waiting at max " + WAIT_SEC + " sec " + service + " to stop");
        waitTermination(processHandle, stopTime, WAIT_SEC);
        if (!processHandle.isAlive()) {
            logStopped(processHandle);
            return;
        }

        processHandle.destroyForcibly();
        waitTermination(processHandle, stopTime, WAIT_SEC);

        if (processHandle.isAlive()) {
            error("Can't kill service " + service);
        } else {
            warn("Destroyed forcibly " + service);
        }
    }

    private void logStopped(ProcessHandle processHandle) {
        announce("Stopped " + service + " [" + processHandle.pid() + "]" + " in " + secAfter(stopTime));
    }
}