package edu.vt.ranhuo.localmsg.support;

import java.time.Duration;
import java.time.Instant;

public class TimeUtils {
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static boolean isTimeout(long timestamp, Duration timeout) {
        return currentTimeMillis() - timestamp > timeout.toMillis();
    }

    public static long durationFromNow(Duration duration) {
        return Instant.now().plus(duration).toEpochMilli();
    }
}
