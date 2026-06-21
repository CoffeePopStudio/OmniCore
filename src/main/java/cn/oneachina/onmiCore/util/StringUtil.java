package cn.oneachina.onmiCore.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([mhd])$");

    public static int safeParseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long safeParseLong(String s, long defaultValue) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Duration parseTime(String input) {
        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit) {
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> null;
        };
    }

    private StringUtil() {}
}
