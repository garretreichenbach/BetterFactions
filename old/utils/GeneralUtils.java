package thederpgamer.betterfactions.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * GeneralUtils.java
 * <Description>
 *
 * @since 02/10/2021
 * @author TheDerpGamer
 */
public class GeneralUtils {

    /**
     * @param date1 The first date
     * @param date2 The second date
     * @return The number of days between the two dates
     */
    public static int getDaysBetween(Date date1, Date date2) {
        return (int) Math.abs(TimeUnit.DAYS.convert(date1.getTime() - date2.getTime(), TimeUnit.MILLISECONDS));
    }

    /**
     * @param date1 The first date
     * @param date2 The second date
     * @return The number of days between the two dates
     */
    public static int getDaysBetween(long date1, long date2) {
        return (int) Math.abs(TimeUnit.DAYS.convert(date1 - date2, TimeUnit.MILLISECONDS));
    }

    /**
     * @return The current date
     */
    public static Date getCurrentDate() {
        return new Date();
    }
}
