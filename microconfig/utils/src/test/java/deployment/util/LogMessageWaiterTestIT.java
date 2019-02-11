package deployment.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class LogMessageWaiterTestIT {
    public static void main(String[] args) {
        File log = new File("c:/temp/log.txt");
        log.delete();

        new Thread(() -> new LogMessageWaiter(ProcessHandle.current(),  log, Set.of("line60", "failure marker"), System.currentTimeMillis(), 10).await()).start();

        ThreadUtils.sleepSec(2);

        new Thread(() -> writeLogs(log)).start();
    }

    private static void writeLogs(File log) {
        int i = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log), 10)) {
            while (true) {
                writer.write("line" + i++);
                writer.newLine();
                ThreadUtils.sleepMs(111);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}