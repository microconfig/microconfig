package io.microconfig.utils;

import org.junit.Test;

import static io.microconfig.utils.TimeUtils.formatTimeAfter;
import static org.junit.Assert.assertEquals;

public class TimeUtilsTestIT {
    @Test
    public void testFormatTimeAfter() {
        long time = 1550014472998L;
        System.out.println(time);
        assertEquals("5h 3m", formatTimeAfter(time - (5 * 3600 * 1010)));

        String d = formatTimeAfter(time - (10 * 25 * 3600 * 1000));
        assertEquals("10d 10h", d);
    }
}