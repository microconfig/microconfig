package deployment.util;

import org.junit.Test;

import static deployment.util.TimeUtils.formatTimeAfter;
import static java.lang.System.currentTimeMillis;

public class TimeUtilsTestIT {
    @Test
    public void testFormatTimeAfter() {
//        String sec = formatTimeAfter(currentTimeMillis() - (9 * 1000));
//        System.out.println(sec);
//
//        String min = formatTimeAfter(currentTimeMillis() - (14 * 60 * 1010));
//        System.out.println(min);

        String h = formatTimeAfter(currentTimeMillis() - (5 * 3600 * 1010));
        System.out.println(h);

        String d = formatTimeAfter(currentTimeMillis() - (10 * 25 * 3600 * 1000 ));
        System.out.println(d);
    }
}