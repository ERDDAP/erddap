package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.GregorianCalendar;

class TimePeriodsTest {

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** TimePeriods.basicTest");

    Test.ensureEqual(TimePeriods.OPTIONS.length, TimePeriods.TITLES.length, "");
    Test.ensureEqual(TimePeriods.OPTIONS.length, TimePeriods.N_HOURS.length, "");
    Test.ensureEqual(TimePeriods.OPTIONS.length, TimePeriods.PICK_FROM.length, "");

    // test getCleanCenteredTime
    // If timePeriod is pass, the time is unchanged.
    // If timePeriod is nHour, min=0, sec=0.
    // If timePeriod is even nDays, hr=0, min=0, sec=0.
    // If timePeriod is odd nDays, hr=12, min=0, sec=0.
    // If timePeriod is even nMonths, date=1, hr=0, min=0, sec=0.
    // If timePeriod is odd nMonths, date=middle, hr=0 or 12, min=0, sec=0.
    // If timePeriod is 1 year (to nearest month), date=1, hr=0, min=0, sec=0.
    // If timePeriod is even nYears, month=0, date=1, hr=0, min=0, sec=0.
    // If timePeriod is odd nYears, month=6, date=1, hr=0, min=0, sec=0.
    // If timePeriod is "all", the time is unchanged.
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("pass", "2005-06-10 14:13:11"),
        "2005-06-10 14:13:11",
        "a");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime(TimePeriods._25HOUR_OPTION, "2005-06-10 14:13:11"),
        "2005-06-10 14:30:00",
        "b");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime(TimePeriods._33HOUR_OPTION, "2005-06-10 14:13:11"),
        "2005-06-10 14:30:00",
        "b33");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 day", "2005-06-10 14:13:11"),
        "2005-06-10 12:00:00",
        "c");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("3 day", "2005-06-10 14:13:11"),
        "2005-06-10 12:00:00",
        "d");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("4 day", "2005-06-10 14:13:11"),
        "2005-06-10 00:00:00",
        "e");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("8 day", "2005-06-10 14:13:11"),
        "2005-06-10 00:00:00",
        "f");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 month", "2005-06-10 14:13:11"),
        "2005-06-16 00:00:00",
        "g");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 month", "2005-01-10 14:13:11"),
        "2005-01-16 12:00:00",
        "g");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 month", "2005-05-09 14:13:11"),
        "2005-05-16 12:00:00",
        "h");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("3 month", "2005-06-10 14:13:11"),
        "2005-06-16 00:00:00",
        "i");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 year", "2005-06-10 14:13:11"),
        "2005-06-01 00:00:00",
        "j");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("5 year", "2005-06-10 14:13:11"),
        "2005-07-01 00:00:00",
        "k");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("10 year", "2005-06-10 14:13:11"),
        "2005-01-01 00:00:00",
        "l");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("all", "2005-06-10 14:13:11"), "2005-06-10 14:13:11", "m");

    // test getCleanCenteredTime for climatology
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("pass", "0001-06-10 14:13:11"),
        "0001-06-10 14:13:11",
        "a");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime(TimePeriods._25HOUR_OPTION, "0001-06-10 14:13:11"),
        "0001-06-10 14:30:00",
        "b");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime(TimePeriods._33HOUR_OPTION, "0001-06-10 14:13:11"),
        "0001-06-10 14:30:00",
        "b33");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 day", "0001-06-10 14:13:11"),
        "0001-06-10 12:00:00",
        "c");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("3 day", "0001-06-10 14:13:11"),
        "0001-06-10 12:00:00",
        "d");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("4 day", "0001-06-10 14:13:11"),
        "0001-06-10 00:00:00",
        "e");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("8 day", "0001-06-10 14:13:11"),
        "0001-06-10 00:00:00",
        "f");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 month", "0001-06-10 14:13:11"),
        "0001-06-16 00:00:00",
        "g");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 month", "0001-01-10 14:13:11"),
        "0001-01-16 12:00:00",
        "g");
    Test.ensureEqual(
        TimePeriods.getCleanCenteredTime("1 month", "0001-05-09 14:13:11"),
        "0001-05-16 12:00:00",
        "h");
    // Test.ensureEqual(getCleanCenteredTime("3 month", "0001-06-10 14:13:11"),
    // "0001-06-16 00:00:00", "i");
    // Test.ensureEqual(getCleanCenteredTime("1 year", "0001-06-10 14:13:11"),
    // "0001-06-01 00:00:00", "j");
    // Test.ensureEqual(getCleanCenteredTime("5 year", "0001-06-10 14:13:11"),
    // "0001-07-01 00:00:00", "k");
    // Test.ensureEqual(getCleanCenteredTime("10 year", "0001-06-10 14:13:11"),
    // "0001-01-01 00:00:00", "l");
    // Test.ensureEqual(getCleanCenteredTime("all", "0001-06-10 14:13:11"),
    // "0001-06-10 14:13:11", "m");

    // test getStartCalendar getEndCalendar
    String minT = "1971-02-03 01:00:00";
    String maxT = "2006-04-05 02:00:00";
    TimePeriods.testStartEndCalendar(
        "pass", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "a");
    TimePeriods.testStartEndCalendar(
        "1 observation", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "2005-06-16 14:13:11", "b");
    TimePeriods.testStartEndCalendar(
        TimePeriods._25HOUR_OPTION,
        "2005-06-16 10:30:00",
        "2005-06-15 22:00:00",
        "2005-06-16 23:00:00",
        "c");
    TimePeriods.testStartEndCalendar(
        TimePeriods._33HOUR_OPTION,
        "2005-06-16 10:30:00",
        "2005-06-15 18:00:00",
        "2005-06-17 03:00:00",
        "d");
    TimePeriods.testStartEndCalendar(
        "1 day", "2005-06-16 12:00:00", "2005-06-16 00:00:00", "2005-06-17 00:00:00", "e");
    TimePeriods.testStartEndCalendar(
        "3 day", "2005-06-16 12:00:00", "2005-06-15 00:00:00", "2005-06-18 00:00:00", "f");
    TimePeriods.testStartEndCalendar(
        "5 day", "2005-06-16 12:00:00", "2005-06-14 00:00:00", "2005-06-19 00:00:00", "h");
    TimePeriods.testStartEndCalendar(
        "4 day", "2005-06-16 00:00:00", "2005-06-14 00:00:00", "2005-06-18 00:00:00", "g");
    TimePeriods.testStartEndCalendar(
        "8 day", "2005-06-16 00:00:00", "2005-06-12 00:00:00", "2005-06-20 00:00:00", "i");
    TimePeriods.testStartEndCalendar(
        "10 day", "2005-06-16 00:00:00", "2005-06-11 00:00:00", "2005-06-21 00:00:00", "j");
    TimePeriods.testStartEndCalendar(
        "1 month", "2005-01-16 12:00:00", "2005-01-01 00:00:00", "2005-02-01 00:00:00", "k");
    TimePeriods.testStartEndCalendar(
        "1 month", "2005-06-16 00:00:00", "2005-06-01 00:00:00", "2005-07-01 00:00:00", "k");
    TimePeriods.testStartEndCalendar(
        "3 month", "2005-06-16 00:00:00", "2005-05-01 00:00:00", "2005-08-01 00:00:00", "m");
    TimePeriods.testStartEndCalendar(
        "1 year", "2005-06-16 00:00:00", "2004-12-01 00:00:00", "2005-12-01 00:00:00", "n");
    TimePeriods.testStartEndCalendar(
        "5 year", "2005-07-01 00:00:00", "2003-01-01 00:00:00", "2008-01-01 00:00:00", "o");
    TimePeriods.testStartEndCalendar(
        "10 year", "2005-01-01 00:00:00", "2000-01-01 00:00:00", "2010-01-01 00:00:00", "p");
    TimePeriods.testStartEndCalendar("all", "2005-06-16 14:13:11", minT, maxT, "q");

    // test getStartCalendar getEndCalendar -- for climatology
    TimePeriods.testStartEndCalendar(
        "pass", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "a");
    TimePeriods.testStartEndCalendar(
        "1 observation", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "0001-06-16 14:13:11", "b");
    TimePeriods.testStartEndCalendar(
        TimePeriods._25HOUR_OPTION,
        "0001-06-16 10:30:00",
        "0001-06-15 22:00:00",
        "0001-06-16 23:00:00",
        "c");
    TimePeriods.testStartEndCalendar(
        TimePeriods._33HOUR_OPTION,
        "0001-06-16 10:30:00",
        "0001-06-15 18:00:00",
        "0001-06-17 03:00:00",
        "d");
    TimePeriods.testStartEndCalendar(
        "1 day", "0001-06-16 12:00:00", "0001-06-16 00:00:00", "0001-06-17 00:00:00", "e");
    TimePeriods.testStartEndCalendar(
        "3 day", "0001-06-16 12:00:00", "0001-06-15 00:00:00", "0001-06-18 00:00:00", "f");
    TimePeriods.testStartEndCalendar(
        "5 day", "0001-06-16 12:00:00", "0001-06-14 00:00:00", "0001-06-19 00:00:00", "h");
    TimePeriods.testStartEndCalendar(
        "4 day", "0001-06-16 00:00:00", "0001-06-14 00:00:00", "0001-06-18 00:00:00", "g");
    TimePeriods.testStartEndCalendar(
        "8 day", "0001-06-16 00:00:00", "0001-06-12 00:00:00", "0001-06-20 00:00:00", "i");
    TimePeriods.testStartEndCalendar(
        "10 day", "0001-06-16 00:00:00", "0001-06-11 00:00:00", "0001-06-21 00:00:00", "j");
    TimePeriods.testStartEndCalendar(
        "1 month", "0001-01-16 12:00:00", "0001-01-01 00:00:00", "0001-02-01 00:00:00", "k");
    TimePeriods.testStartEndCalendar(
        "1 month", "0001-06-16 00:00:00", "0001-06-01 00:00:00", "0001-07-01 00:00:00", "k");
    // testStartEndCalendar("3 month", "0001-06-16 00:00:00", "0001-05-01 00:00:00",
    // "0001-08-01 00:00:00", "m");
    // testStartEndCalendar("1 year", "0001-06-16 00:00:00", "2004-12-01 00:00:00",
    // "0001-12-01 00:00:00", "n");
    // testStartEndCalendar("5 year", "0001-07-01 00:00:00", "2003-01-01 00:00:00",
    // "2008-01-01 00:00:00", "o");
    // testStartEndCalendar("10 year", "0001-01-01 00:00:00", "2000-01-01 00:00:00",
    // "2010-01-01 00:00:00", "p");
    // testStartEndCalendar("all", "0001-06-16 14:13:11", minT, maxT, "q");

    // getLegendTime
    Test.ensureEqual(
        TimePeriods.getLegendTime("pass", "2005-06-10 14:13:11"), "2005-06-10T14:13:11Z", "a");
    Test.ensureEqual(
        TimePeriods.getLegendTime("1 observation", "2005-06-10 14:13:11"),
        "2005-06-10T14:13:11Z",
        "a2");
    Test.ensureEqual(
        TimePeriods.getLegendTime(TimePeriods._25HOUR_OPTION, "2005-06-10 14:13:11"),
        "2005-06-10T14:30:00Z (center of 25 hours)",
        "b");
    Test.ensureEqual(
        TimePeriods.getLegendTime(TimePeriods._33HOUR_OPTION, "2005-06-10 14:13:11"),
        "2005-06-10T14:30:00Z (center of 33 hours)",
        "b2");
    Test.ensureEqual(TimePeriods.getLegendTime("1 day", "2005-06-10 14:13:11"), "2005-06-10", "c");
    Test.ensureEqual(
        TimePeriods.getLegendTime("3 day", "2005-06-10 14:13:11"),
        "2005-06-09 through 2005-06-11",
        "d");
    Test.ensureEqual(
        TimePeriods.getLegendTime("4 day", "2005-06-10 14:13:11"),
        "2005-06-08 through 2005-06-11",
        "e");
    Test.ensureEqual(
        TimePeriods.getLegendTime("8 day", "2005-06-10 14:13:11"),
        "2005-06-06 through 2005-06-13",
        "f");
    Test.ensureEqual(TimePeriods.getLegendTime("1 month", "2005-06-10 14:13:11"), "2005-06", "g");
    Test.ensureEqual(TimePeriods.getLegendTime("1 month", "2005-05-09 14:13:11"), "2005-05", "h");
    Test.ensureEqual(
        TimePeriods.getLegendTime("3 month", "2005-06-10 14:13:11"),
        "2005-05 through 2005-07",
        "i");
    Test.ensureEqual(
        TimePeriods.getLegendTime("1 year", "2005-06-10 14:13:11"), "2004-12 through 2005-11", "j");
    Test.ensureEqual(
        TimePeriods.getLegendTime("5 year", "2005-06-10 14:13:11"), "2003 through 2007", "k");
    Test.ensureEqual(
        TimePeriods.getLegendTime("10 year", "2005-06-10 14:13:11"), "2000 through 2009", "l");
    Test.ensureEqual(
        TimePeriods.getLegendTime("all", "2005-06-10 14:13:11"), "All available data", "m");

    // getLegendTime - for climatology
    Test.ensureEqual(
        TimePeriods.getLegendTime("pass", "0001-06-10 14:13:11"), "0001-06-10T14:13:11Z", "a");
    Test.ensureEqual(
        TimePeriods.getLegendTime("1 observation", "0001-06-10 14:13:11"),
        "0001-06-10T14:13:11Z",
        "a2");
    Test.ensureEqual(
        TimePeriods.getLegendTime(TimePeriods._25HOUR_OPTION, "0001-06-10 14:13:11"),
        "0001-06-10T14:30:00Z (center of 25 hours)",
        "b");
    Test.ensureEqual(
        TimePeriods.getLegendTime(TimePeriods._33HOUR_OPTION, "0001-06-10 14:13:11"),
        "0001-06-10T14:30:00Z (center of 33 hours)",
        "b2");
    Test.ensureEqual(TimePeriods.getLegendTime("1 day", "0001-06-10 14:13:11"), "0001-06-10", "c");
    Test.ensureEqual(
        TimePeriods.getLegendTime("3 day", "0001-06-10 14:13:11"),
        "0001-06-09 through 0001-06-11",
        "d");
    Test.ensureEqual(
        TimePeriods.getLegendTime("4 day", "0001-06-10 14:13:11"),
        "0001-06-08 through 0001-06-11",
        "e");
    Test.ensureEqual(
        TimePeriods.getLegendTime("8 day", "0001-06-10 14:13:11"),
        "0001-06-06 through 0001-06-13",
        "f");
    Test.ensureEqual(TimePeriods.getLegendTime("1 month", "0001-06-10 14:13:11"), "0001-06", "g");
    Test.ensureEqual(TimePeriods.getLegendTime("1 month", "0001-05-09 14:13:11"), "0001-05", "h");
    // Test.ensureEqual(getLegendTime("3 month", "0001-06-10 14:13:11"), "0001-05
    // through 0001-07", "i");
    // Test.ensureEqual(getLegendTime("1 year", "0001-06-10 14:13:11"), "2004-12
    // through 0001-11", "j");
    // Test.ensureEqual(getLegendTime("5 year", "0001-06-10 14:13:11"), "2003
    // through 2007", "k");
    // Test.ensureEqual(getLegendTime("10 year", "2005-06-10 14:13:11"), "2000
    // through 2009", "l");
    // Test.ensureEqual(getLegendTime("all", "2005-06-10 14:13:11"), "All available
    // data", "m");

    // getLegendTime
    /*
     * NOT FINISHED
     * Test.ensureEqual(getLegendTime("pass", "2005-06-10 14:13:11"),
     * "1 pass at 2005-06-10 14:13:11Z", "a");
     * Test.ensureEqual(getLegendTime("1 observation","2005-06-10 14:13:11"),
     * "1 observation at 2005-06-10 14:13:11Z", "a");
     * Test.ensureEqual(getLegendTime(_25HOUR_OPTION, "2005-06-10 14:13:11"),
     * "25 hours, centered at 2005-06-10 14:00:00Z", "b");
     * Test.ensureEqual(getLegendTime("1 day", "2005-06-10 14:13:11"),
     * "1 day, centered at 2005-06-10 12:00:00Z", "c");
     * Test.ensureEqual(getLegendTime("3 day", "2005-06-10 14:13:11"),
     * "3 days, centered at 2005-06-10 12:00:00Z", "d");
     * Test.ensureEqual(getLegendTime("4 day", "2005-06-10 14:13:11"),
     * "4 days, centered at 2005-06-10 00:00:00Z", "e");
     * Test.ensureEqual(getLegendTime("8 day", "2005-06-10 14:13:11"),
     * "8 days, centered at 2005-06-10 00:00:00Z", "f");
     * Test.ensureEqual(getLegendTime("1 month", "2005-06-10 14:13:11"),
     * "1 month, centered at 2005-06-16 00:00:00Z", "g");
     * Test.ensureEqual(getLegendTime("1 moth", "2005-05-09 14:13:11"),
     * "1 month, centered at 2005-05-09 12:00:00Z", "h");
     * Test.ensureEqual(getLegendTime("3 month", "2005-06-10 14:13:11"),
     * "3 months, centered at 2005-06-16 00:00:00Z", "i");
     * Test.ensureEqual(getLegendTime("1 year", "2005-06-10 14:13:11"),
     * "1 year, centered at 2005-06-01 00:00:00Z", "j");
     * Test.ensureEqual(getLegendTime("5 year", "2005-06-10 14:13:11"),
     * "5 years, centered at 2005-07-01 00:00:00Z", "k");
     * Test.ensureEqual(getLegendTime("10 year", "2005-06-10 14:13:11"),
     * "10 years, centered at 2005-01-01 00:00:00Z", "l");
     * Test.ensureEqual(getLegendTime("all", "2005-06-10 14:13:11"),
     * "All available", "m");
     */

    // getLegendTime
    /*
     * never finished
     * String center = "2005-06-16 14:13:11";
     * Test.ensureEqual(getLegendTime("pass", center, minT, maxT),
     * "2005-06-16 14:13:11Z", "");
     * Test.ensureEqual(getLegendTime("1 observation", center, minT, maxT),
     * "2005-06-16 14:13:11Z", "");
     * Test.ensureEqual(getLegendTime(_25HOUR_OPTION, center, minT, maxT),
     * "2005-06-15 21:30:00Z to 2005-06-16 22:30:00Z", "c");
     * Test.ensureEqual(getLegendTime(_33HOUR_OPTION, center, minT, maxT),
     * "2005-06-15 17:30:00Z to 2005-06-17 02:30:00Z", "d");
     * Test.ensureEqual(getLegendTime("1 day", center, minT, maxT),
     * "2005-06-16 00:00:00Z to 2005-06-17 00:00:00Z", "e");
     * Test.ensureEqual(getLegendTime("3 day", center, minT, maxT),
     * "2005-06-15 00:00:00Z to 2005-06-18 00:00:00Z", "f");
     * Test.ensureEqual(getLegendTime("5 day", center, minT, maxT),
     * "2005-06-14 00:00:00Z to 2005-06-19 00:00:00Z", "h");
     * Test.ensureEqual(getLegendTime("4 day", center, minT, maxT),
     * "2005-06-14 00:00:00Z to 2005-06-18 00:00:00Z", "g");
     * Test.ensureEqual(getLegendTime("8 day", center, minT, maxT),
     * "2005-06-12 00:00:00Z to 2005-06-20 00:00:00Z", "i");
     * Test.ensureEqual(getLegendTime("10 day", center, minT, maxT),
     * "2005-06-11 00:00:00Z to 2005-06-21 00:00:00Z", "j");
     * Test.ensureEqual(getLegendTime("1 month", center, minT, maxT),
     * "2005-06-01 00:00:00Z to 2005-07-01 00:00:00Z", "k");
     * Test.ensureEqual(getLegendTime("monthly", center, minT, maxT),
     * "2005-06-01 00:00:00Z to 2005-07-01 00:00:00Z", "l");
     * Test.ensureEqual(getLegendTime("3 month", center, minT, maxT),
     * "2005-05-01 00:00:00Z to 2005-08-01 00:00:00Z", "m");
     * Test.ensureEqual(getLegendTime("1 year", center, minT, maxT),
     * "2005-01-01 00:00:00Z to 2006-01-01 00:00:00Z", "n");
     * Test.ensureEqual(getLegendTime("5 year", center, minT, maxT),
     * "2003-01-01 00:00:00Z to 2008-01-01 00:00:00Z", "o");
     * Test.ensureEqual(getLegendTime("10 year", center, minT, maxT),
     * "2000-01-01 00:00:00Z to 2010-01-01 00:00:00Z", "p");
     * Test.ensureEqual(getLegendTime("all", center, minT, maxT),
     * "1971-02-03 01:00:00Z to 2006-04-05 02:00:00Z", "q");
     */

    // closestTimePeriod
    String tOptions[] = {
      "pass", "1 day", "3 day", "10 day", "14 day", "1 month", "3 month", "1 year", "10 year", "all"
    };
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod("pass", tOptions)], "pass", "");
    Test.ensureEqual(
        tOptions[TimePeriods.closestTimePeriod("1 observation", tOptions)], "pass", "");
    Test.ensureEqual(
        tOptions[TimePeriods.closestTimePeriod(TimePeriods._25HOUR_OPTION, tOptions)], "1 day", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod("8 day", tOptions)], "10 day", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod("monthly", tOptions)], "1 month", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod("5 year", tOptions)], "1 year", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod("20 year", tOptions)], "10 year", "");
    Test.ensureEqual(
        tOptions[TimePeriods.closestTimePeriod("Nate", tOptions)], "1 day", ""); // not an option

    // closestTimePeriod
    tOptions =
        new String[] {
          "pass", "1 day", "3 day", "10 day", "14 day", "1 month", "3 month", "1 year", "10 year",
          "all"
        };
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(0, tOptions)], "pass", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(24, tOptions)], "1 day", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(25, tOptions)], "1 day", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(8 * 24, tOptions)], "10 day", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(31 * 24, tOptions)], "1 month", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(366 * 24, tOptions)], "1 year", "");
    Test.ensureEqual(
        tOptions[TimePeriods.closestTimePeriod(10 * 365 * 24, tOptions)], "10 year", "");
    Test.ensureEqual(tOptions[TimePeriods.closestTimePeriod(25 * 365 * 24, tOptions)], "all", "");
    Test.ensureEqual(
        tOptions[TimePeriods.closestTimePeriod("Nate", tOptions)], "1 day", ""); // not an option

    // getTitles
    String tOptions2[] = {"pass", "3 day"};
    Test.ensureEqual(
        TimePeriods.getTitles(tOptions2),
        new String[] {
          "Specify the length of time in which you are interested.", // first one is generic
          "Get the data from one observation.",
          "Get the mean of 3 days' data."
        },
        "");

    // validateBeginDate
    Test.ensureEqual(
        TimePeriods.validateBeginTime("2003-01-05", "2004-02-03", "1 day"),
        "2003-01-05 12:00:00",
        ""); // already
    // valid
    Test.ensureEqual(
        TimePeriods.validateBeginTime("2005-01-05", "2004-02-03", "3 day"),
        "2004-01-03 00:00:00",
        ""); // one
    // month
    // back
    Test.ensureEqual(
        TimePeriods.validateBeginTime("", "2004-02-03", "1 month"),
        "2003-02-03 00:00:00",
        ""); // one
    // year
    // back

    // endCalendarToCenteredTime(int timePeriodNHours, GregorianCalendar cal, String
    // errorInMethod)
    GregorianCalendar cal;
    cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
    TimePeriods.endCalendarToCenteredTime(0, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 11:00:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
    TimePeriods.endCalendarToCenteredTime(1, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 11:00:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
    TimePeriods.endCalendarToCenteredTime(25, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 22:30:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-09-10 11:00:00");
    TimePeriods.endCalendarToCenteredTime(33, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 18:30:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-09-11 00:00:00");
    TimePeriods.endCalendarToCenteredTime(24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 12:00:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-09-11 00:00:00");
    TimePeriods.endCalendarToCenteredTime(3 * 24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 12:00:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-09-11 00:00:00");
    TimePeriods.endCalendarToCenteredTime(8 * 24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-07 00:00:00", "");

    cal = Calendar2.parseISODateTimeZulu("2006-11-01 00:00:00");
    TimePeriods.endCalendarToCenteredTime(30 * 24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-10-16 12:00:00", "");

    // centeredTimeToOldStyleEndOption(int timePeriodNHours, String centeredTime) {
    // comments about (don't) include hms are based on CWBrowser before change to
    // centered time
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(0, "2006-09-10 11:00:00"),
        "2006-09-10 11:00:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(1, "2006-09-10 11:00:00"),
        "2006-09-10 11:00:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(25, "2006-09-10 08:30:00"),
        "2006-09-10 21:00:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(33, "2006-09-10 08:30:00"),
        "2006-09-11 01:00:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(24, "2006-09-10 12:00:00"),
        "2006-09-10",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(3 * 24, "2006-09-10 12:00:00"),
        "2006-09-11",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(8 * 24, "2006-09-10 00:00:00"),
        "2006-09-13",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms
    Test.ensureEqual(
        TimePeriods.centeredTimeToOldStyleEndOption(30 * 24, "2006-09-16 00:00:00"),
        "2006-09-30",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms

    // oldStyleEndOptionToCenteredTime(int timePeriodNHours, String centeredTime) {
    // comments about (don't) include hms are based on CWBrowser before change to
    // centered time
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(0, "2006-09-10 11:00:00"),
        "2006-09-10 11:00:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(1, "2006-09-10 11:00:00"),
        "2006-09-10 11:00:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(25, "2006-09-10 21:00:00"),
        "2006-09-10 08:30:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(33, "2006-09-11 01:00:00"),
        "2006-09-10 08:30:00",
        ""); // yes, include hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(24, "2006-09-10"),
        "2006-09-10 12:00:00",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(3 * 24, "2006-09-11"),
        "2006-09-10 12:00:00",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(8 * 24, "2006-09-13"),
        "2006-09-10 00:00:00",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms
    Test.ensureEqual(
        TimePeriods.oldStyleEndOptionToCenteredTime(30 * 24, "2006-09-30"),
        "2006-09-16 00:00:00",
        ""); // last
    // day,
    // inclusive;
    // no,
    // don't
    // include
    // hms

  }
}
