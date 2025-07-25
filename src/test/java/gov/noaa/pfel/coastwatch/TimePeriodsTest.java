package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.collect.ImmutableList;
import java.util.GregorianCalendar;

class TimePeriodsTest {

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** TimePeriods.basicTest");

    Test.ensureEqual(TimePeriods.OPTIONS.size(), TimePeriods.N_HOURS.size(), "");

    // closestTimePeriod
    ImmutableList<String> tOptions =
        ImmutableList.of(
            "pass", "1 day", "3 day", "10 day", "14 day", "1 month", "3 month", "1 year", "10 year",
            "all");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod("pass", tOptions)), "pass", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod("1 observation", tOptions)), "pass", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod(TimePeriods._25HOUR_OPTION, tOptions)),
        "1 day",
        "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod("8 day", tOptions)), "10 day", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod("monthly", tOptions)), "1 month", "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod("5 year", tOptions)), "1 year", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod("20 year", tOptions)), "10 year", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod("Nate", tOptions)),
        "1 day",
        ""); // not an option

    // closestTimePeriod
    tOptions =
        ImmutableList.of(
            "pass", "1 day", "3 day", "10 day", "14 day", "1 month", "3 month", "1 year", "10 year",
            "all");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod(0, tOptions)), "pass", "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod(24, tOptions)), "1 day", "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod(25, tOptions)), "1 day", "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod(8 * 24, tOptions)), "10 day", "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod(31 * 24, tOptions)), "1 month", "");
    Test.ensureEqual(tOptions.get(TimePeriods.closestTimePeriod(366 * 24, tOptions)), "1 year", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod(10 * 365 * 24, tOptions)), "10 year", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod(25 * 365 * 24, tOptions)), "all", "");
    Test.ensureEqual(
        tOptions.get(TimePeriods.closestTimePeriod("Nate", tOptions)),
        "1 day",
        ""); // not an option

    // getTitles
    String tOptions2[] = {"pass", "3 day"};

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
  }
}
