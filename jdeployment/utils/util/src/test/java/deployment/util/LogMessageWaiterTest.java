package deployment.util;

import org.junit.Test;

public class LogMessageWaiterTest {
    @Test
    public void doAppend() {
        LogMessageWaiter waiter = new LogMessageWaiter(null, null, null, System.currentTimeMillis(), 1);
        StringBuilder logContent = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            waiter.doAppend(logContent, i + "|", 4);
            System.out.println();
        }
    }
}