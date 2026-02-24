package gov.noaa.pfel.coastwatch;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.time.ZonedDateTime;

class TimePeriodsTest {

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** TimePeriods.basicTest");

    // endCalendarToCenteredTime(int timePeriodNHours, GregorianCalendar cal, String
    // errorInMethod)
    ZonedDateTime cal;
    cal = Calendar2.parseISODateTimeUtc("2006-09-10 11:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(0, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 11:00:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-09-10 11:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(1, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 11:00:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-09-10 11:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(25, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 22:30:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-09-10 11:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(33, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 18:30:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-09-11 00:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-10 12:00:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-09-11 00:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(3 * 24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-09 12:00:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-09-11 00:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(8 * 24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-09-07 00:00:00", "");

    cal = Calendar2.parseISODateTimeUtc("2006-11-01 00:00:00");
    cal = TimePeriods.endCalendarToCenteredTime(30 * 24, cal, "");
    Test.ensureEqual(Calendar2.formatAsISODateTimeSpace(cal), "2006-10-16 12:00:00", "");
  }
}
