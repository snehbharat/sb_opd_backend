package com.sbpl.OPD.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

/**
 * Utility class for timezone-aware date operations in the HMS application.
 * This ensures that date calculations work consistently regardless of server timezone.
 *
 * @author Rahul Kumar
 */
public class DateUtils {
    
    // Define the business timezone (change this to match your operational timezone)
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata"); // Indian Standard Time
    
    /**
     * Get the current date in the business timezone
     */
    public static LocalDate getBusinessLocalDate() {
        return ZonedDateTime.now(BUSINESS_ZONE).toLocalDate();
    }

    public static LocalDateTime getCurrentBusinessDateTime() {
        return ZonedDateTime.now(BUSINESS_ZONE).toLocalDateTime();
    }


    public static LocalDateTime[] getBusinessDayRange() {

        LocalDate today = getBusinessLocalDate();

        LocalDateTime startDateTime = today.atStartOfDay();
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        return new LocalDateTime[]{startDateTime, endDateTime};
    }
    
    /**
     * Get the start of the current business day (00:00:00) in the business timezone
     */
    public static LocalDateTime getStartOfBusinessDay() {
        LocalDate today = getBusinessLocalDate();
        return today.atStartOfDay();
    }
    
    /**
     * Get the end of the current business day (23:59:59) in the business timezone
     */
    public static LocalDateTime getEndOfBusinessDay() {
        LocalDate today = getBusinessLocalDate();
        return today.atTime(23, 59, 59);
    }
    
    /**
     * Convert a LocalDate to the start of the day in the business timezone
     */
    public static LocalDateTime getStartOfBusinessDay(LocalDate date) {
        return date.atStartOfDay();
    }
    
    /**
     * Convert a LocalDate to the end of the day (23:59:59) in the business timezone
     */
    public static LocalDateTime getEndOfBusinessDay(LocalDate date) {
        return date.atTime(23, 59, 59);
    }
    
    /**
     * Get the start and end of the current business week (7 days ago to now) in the business timezone
     */
    public static LocalDateTime[] getBusinessWeekRange() {
        LocalDate today = getBusinessLocalDate();

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.atTime(LocalTime.MAX);

        return new LocalDateTime[]{startDateTime, endDateTime};
    }
    
    /**
     * Get the business timezone
     */
    public static ZoneId getBusinessZone() {
        return BUSINESS_ZONE;
    }


    /**
     * Returns the start and end time in epoch milliseconds for the given date range in IST timezone.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>If both {@code startDate} and {@code endDate} are {@code null}, the method returns
     *     the start and end of the current day in IST.</li>
     *     <li>If dates are provided, {@code startDate} will be converted to the start of the day
     *     (00:00:00.000) and {@code endDate} will be converted to the end of the day
     *     (23:59:59.999999999).</li>
     *     <li>Date format expected: {@code yyyy-MM-dd}</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>
     * getStartAndEndDateInMilli("2026-03-01", "2026-03-05")
     * -> returns epoch millis for
     *    2026-03-01T00:00:00 IST to 2026-03-05T23:59:59.999 IST
     * </pre>
     *
     * @param startDate the start date in {@code yyyy-MM-dd} format, may be {@code null}
     * @param endDate   the end date in {@code yyyy-MM-dd} format, may be {@code null}
     * @return an array where:
     *         <ul>
     *             <li>index 0 = start time in epoch milliseconds</li>
     *             <li>index 1 = end time in epoch milliseconds</li>
     *         </ul>
     */
    public static Long[] getStartAndEndDateInMilli(String startDate, String endDate) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate start;
        LocalDate end;

        // If both null -> use today's date in IST
        if (startDate == null && endDate == null) {
            LocalDate todayIST = LocalDate.now(BUSINESS_ZONE);
            start = todayIST;
            end = todayIST;
        } else {
            assert startDate != null;
            start = LocalDate.parse(startDate, formatter);
            end = LocalDate.parse(endDate, formatter);
        }

        // Start of day and End of day
        LocalDateTime startOfDay = start.atStartOfDay();
        LocalDateTime endOfDay = end.atTime(LocalTime.MAX);

        long startMillis = startOfDay.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        long endMillis = endOfDay.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();

        return new Long[]{startMillis, endMillis};
    }

    /**
     * Returns the start and end {@link LocalDateTime} for the given date range in IST timezone.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>If both {@code startDate} and {@code endDate} are {@code null}, the method returns
     *     today's start of day and end of day in IST.</li>
     *     <li>If dates are provided, {@code startDate} is converted to start of day
     *     (00:00:00) and {@code endDate} is converted to end of day
     *     (23:59:59.999999999).</li>
     *     <li>Date format expected: {@code yyyy-MM-dd}</li>
     * </ul>
     *
     * @param startDate start date in {@code yyyy-MM-dd} format (nullable)
     * @param endDate end date in {@code yyyy-MM-dd} format (nullable)
     * @return an array where:
     *         <ul>
     *             <li>index 0 = start of day {@link LocalDateTime}</li>
     *             <li>index 1 = end of day {@link LocalDateTime}</li>
     *         </ul>
     */
    public static LocalDateTime[] getStartAndEndDateTime(String startDate, String endDate) {

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate start;
        LocalDate end;

        if (startDate == null && endDate == null) {
            LocalDate todayIST = LocalDate.now(indiaZone);
            start = todayIST;
            end = todayIST;
        } else {
            assert startDate != null;
            start = LocalDate.parse(startDate, formatter);
            end = LocalDate.parse(endDate, formatter);
        }

        LocalDateTime startOfDay = start.atStartOfDay();
        LocalDateTime endOfDay = end.atTime(23, 59, 59);

        return new LocalDateTime[]{startOfDay, endOfDay};
    }

    public static Long[] getTodayRangeInMilli() {

        LocalDate today = getBusinessLocalDate();

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return toMillisRange(startOfDay, endOfDay);
    }

    public static Long[] getWeeklyRangeInMilli() {

        LocalDate today = getBusinessLocalDate();
        LocalDate weekStart = today.minusDays(7);

        LocalDateTime startOfDay = weekStart.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return toMillisRange(startOfDay, endOfDay);
    }

    public static Long[] getMonthlyRangeInMilli() {

        LocalDate today = getBusinessLocalDate();

        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());

        LocalDateTime startOfDay = firstDay.atStartOfDay();
        LocalDateTime endOfDay = lastDay.atTime(LocalTime.MAX);

        return toMillisRange(startOfDay, endOfDay);
    }

    public static boolean isValidDate(String date) {
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static Long[] toMillisRange(LocalDateTime start, LocalDateTime end) {

        long startMillis = start.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
        long endMillis = end.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();

        return new Long[]{startMillis, endMillis};
    }

}