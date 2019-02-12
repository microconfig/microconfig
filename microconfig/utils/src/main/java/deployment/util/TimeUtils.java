package deployment.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static deployment.util.Logger.warn;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class TimeUtils {
    public static int calcSecFrom(long startTimeMs) {
        return (int) MILLISECONDS.toSeconds(calcMsFrom(startTimeMs));
    }

    private static long calcMsFrom(long startTimeMs) {
        return currentTimeMillis() - startTimeMs;
    }

    public static String secAfter(long startTimeMs) {
        return calcSecFrom(startTimeMs) + " sec";
    }

    public static String msAfter(long startTimeMs) {
        return calcMsFrom(startTimeMs) + " ms";
    }

    public static String formatTimeAfter(long startTime) {
        int sec = calcSecFrom(startTime);
        int[] times = {1, 60, 3600, 3600 * 24};
        String[] timeUnit = {"s", "m", "h", "d"};
        IntFunction<String> format = i -> {
            int full = sec / times[i];
            String formatted = full + timeUnit[i];
            if (i == 0) return formatted;

            return formatted + " " + (sec % times[i] / times[i - 1]) + timeUnit[i - 1];
        };
        for (int i = 0; i < times.length - 1; i++) {
            if (sec < times[i + 1]) return format.apply(i);
        }

        return format.apply(times.length - 1);
    }

    public static Supplier<Integer> percentProgress(int eventCount) {
        AtomicInteger counter = new AtomicInteger();
        return () -> min(100, counter.incrementAndGet() * 100 / eventCount);
    }

    public static <T> T printLongTime(Supplier<T> supplier, String log) {
        long t = currentTimeMillis();
        T result = supplier.get();
        int sec = calcSecFrom(t);
        String message = log + " in " + sec + " sec ";

        if (sec > 5) {
            warn(message);
        }
        return result;
    }
}