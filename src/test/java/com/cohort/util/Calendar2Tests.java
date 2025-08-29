package com.cohort.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class Calendar2Tests {
  @Test
  void limitedFormatAsISODateTimeTTest() throws Throwable {

    ZonedDateTime gc = ZonedDateTime.of(2004, 3, 7, 3, 4, 7, 16 * 1000000, ZoneOffset.UTC);
    assertEquals(
        "2004",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970"), gc));
    assertEquals(
        "2004-03",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01"), gc));
    assertEquals(
        "2004-03-07",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01"), gc));
    assertEquals(
        "2004-03-07T03Z",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01T00Z"), gc));
    assertEquals(
        "2004-03-07T03:04Z",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01T00:00Z"), gc));
    assertEquals(
        "2004-03-07T03:04:07Z",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01T00:00:00Z"), gc));
    assertEquals(
        "2004-03-07T03:04:07.0Z",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01T00:00:00.0Z"), gc));
    assertEquals(
        "2004-03-07T03:04:07.01Z",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01T00:00:00.00Z"), gc));
    assertEquals(
        "2004-03-07T03:04:07.016Z",
        Calendar2.limitedFormatAsISODateTimeT(
            Calendar2.timePrecisionToDateTimeFormatter("1970-01-01T00:00:00.000Z"), gc));
  }
}
