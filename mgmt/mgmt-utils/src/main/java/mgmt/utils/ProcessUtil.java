package mgmt.utils;

import io.microconfig.utils.OsUtil;

import java.io.*;
import java.util.Optional;
import java.util.Set;

import static io.microconfig.utils.TimeUtils.calcSecFrom;
import static java.util.Optional.of;
import static mgmt.utils.ByteReaderUtils.copyWithFlush;
import static mgmt.utils.ThreadUtils.sleepSec;

public class ProcessUtil {
    public static void waitTermination(ProcessHandle processHandle, long startTime, int waitSec) {
        while (calcSecFrom(startTime) < waitSec && processHandle.isAlive()) {
            sleepSec(1);
        }
    }

    public static void waitTerminationOrLogMarker(ProcessHandle processHandle, File log, Set<String> logMarkers, long startTime, int startTimeoutInSec) {
        new LogMessageWaiter(processHandle, log, logMarkers, startTime, startTimeoutInSec).await();
    }

    public static Process startProcess(ProcessBuilder processBuilder) {
        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int startAndWait(ProcessBuilder processBuilder) {
        try {
            return startProcess(processBuilder).waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void executeScript(String script) {
        startAndWait(new ProcessBuilder("sh", "-c", script).inheritIO());
    }

    public static String executeAndReadOutput(String... commands) {
        return executeAndReadOutput(processBuilderToRead(commands));
    }

    public static void executeAndReadOutput(OutputStream outputDestination, String... commands) {
        executeAndReadOutput(outputDestination, processBuilderToRead(commands));
    }

    private static ProcessBuilder processBuilderToRead(String[] commands) {
        return new ProcessBuilder(commands).redirectErrorStream(true);
    }

    public static String executeAndReadOutput(ProcessBuilder processBuilder) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        executeAndReadOutput(stream, processBuilder);
        return stream.toString();
    }

    public static void executeAndReadOutput(OutputStream outputDestination, ProcessBuilder processBuilder) {
        try {
            Process process = processBuilder.start();
            try (InputStream inputStream = process.getInputStream()) {
                copyWithFlush(inputStream, outputDestination);
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static boolean belongsToCurrentUser(ProcessHandle processHandle) {
        return belongsToCurrentUser(of(processHandle));
    }

    public static boolean belongsToCurrentUser(Optional<ProcessHandle> processHandle) {
        return processHandle
                .filter(ProcessHandle::isAlive)
                .map(ProcessHandle::info)
                .flatMap(ProcessHandle.Info::user)
                .filter(OsUtil.currentUser()::equals)
                .isPresent();
    }

    public static String currentJavaPath() {
        return ProcessHandle.current().info().command().orElseThrow();
    }
}