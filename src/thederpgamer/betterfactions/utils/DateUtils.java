package thederpgamer.betterfactions.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/07/2021]
 */
public class DateUtils {

    public static int getAgeDays(Date date) {
        Date current = new Date(System.currentTimeMillis());
        long difference = Math.abs(current.getTime() - date.getTime());
        return (int) (difference / (1000 * 60 * 60 * 24));
    }

    public static int getAgeDays(long time) {
        return getAgeDays(new Date(time));
    }

    public static String getTimeFormatted(String format) {
        return (new SimpleDateFormat(format)).format(new Date()) + " ";
    }

    public static String getTimeFormatted() {
        return getTimeFormatted("MM/dd/yyyy '-' hh:mm:ss z");
    }
}
