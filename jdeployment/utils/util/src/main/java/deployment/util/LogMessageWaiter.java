package deployment.util;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static deployment.util.ConsoleColor.yellow;
import static deployment.util.Logger.info;
import static deployment.util.Logger.logLineBreak;
import static deployment.util.LoggerUtils.oneLineInfo;
import static deployment.util.ThreadUtils.sleepSec;
import static deployment.util.TimeUtils.calcSecFrom;
import static deployment.util.TimeUtils.secAfter;

@RequiredArgsConstructor
public class LogMessageWaiter {
    private final ProcessHandle processHandle;
    private final File log;
    private final Set<String> logMarkers;

    private final long startTime;
    private final int timeoutInSec;

    public void await() {
        waitLogCreation();
        waitLogMarker();
        logLineBreak();
    }

    private void waitLogCreation() {
        while (!log.exists() && processHandle.isAlive() && !timeoutReached()) {
            oneLineInfo("Waiting log creation '" + log.getName() + "'" + timeoutInfo(startTime));
            sleepSec(2);
        }
    }

    private void waitLogMarker() {
        if (!log.exists()) return;

        StringBuilder logContent = new StringBuilder();
        int maxInMemoryLogContentLength = logMarkers.stream().mapToInt(String::length).max().orElse(1) * 5;

        try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    doAppend(logContent, line, maxInMemoryLogContentLength);

                    Optional<String> marker = logMarkers.stream()
                            .filter(s -> logContent.indexOf(s) >= 0)
                            .findFirst();

                    if (marker.isPresent()) {
                        oneLineInfo("Found log marker '" + marker.orElseThrow() + "' in '" + log.getName() + "' after " + secAfter(startTime));
                        return;
                    }

                    continue;
                }

                if (!processHandle.isAlive() || timeoutReached()) return;
                oneLineInfo("Waiting log marker in '" + log.getName() + "'" + timeoutInfo(startTime));
                sleepSec(1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void doAppend(StringBuilder logContent, String line, int maxLength) {
        logContent.append(line);

        if (logContent.length() > maxLength) {
            logContent.delete(0, logContent.length() - maxLength);
        }
    }

    private boolean timeoutReached() {
        boolean reached = calcSecFrom(startTime) > timeoutInSec;
        if (reached) {
            oneLineInfo(yellow("Ready markers have't been found. Service is still initializing..."));
        }
        return reached;
    }

    private String timeoutInfo(long startTime) {
        return " " + yellow("Timeout " + calcSecFrom(startTime) + "/" + timeoutInSec + " sec");
    }
}