package deployment.mgmt.process.stop;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.util.ProcessUtil;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static deployment.util.Logger.announce;
import static deployment.util.Logger.warn;
import static java.lang.ProcessHandle.current;

@RequiredArgsConstructor
public class KillCommandImpl implements KillCommand {
    private final DeployFileStructure deployFileStructure;

    @Override
    public void killAllJava() {
        ProcessHandle.allProcesses()
                .filter(ProcessUtil::belongsToCurrentUser)
                .filter(this::isJava)
                .filter(this::notThisProcess)
                .forEach(this::kill);
    }

    private boolean isJava(ProcessHandle processHandle) {
        Optional<String> command = processHandle.info().commandLine();

        return command.filter(l ->
                l.contains("/bin/java")
                        || l.contains("java") && l.contains("Xmx")
        ).isPresent();
    }

    private boolean notThisProcess(ProcessHandle processHandle) {
        long currentPid = current().pid();
        return processHandle.pid() != currentPid
                && processHandle.children().noneMatch(c -> c.pid() == currentPid);
    }

    private void kill(ProcessHandle processHandle) {
        String processInfo = getInfo(processHandle);
        try {
            if (processHandle.destroyForcibly()) {
                announce("Killed: " + processInfo);
            } else {
                logFail(processInfo);
            }
        } catch (RuntimeException e) {
            logFail(processInfo);
        }
    }

    private String getInfo(ProcessHandle processHandle) {
        String info = processHandle.info().commandLine().orElse("");
        int maxLength = 190;
        return processHandle.pid() + " " + (info.length() < maxLength ? info : (info.substring(0, maxLength) + "..."));
    }

    private void logFail(String info) {
        warn("Can't kill process " + info);
    }
}
