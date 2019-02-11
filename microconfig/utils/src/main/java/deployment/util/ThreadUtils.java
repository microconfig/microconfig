package deployment.util;

import static java.lang.Math.max;

public class ThreadUtils {
    public static void sleepSec(long sec) {
        sleepMs(sec * 1_000);
    }

    public static void sleepMs(long ms) {
        try {
            Thread.sleep(max(0, ms));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
