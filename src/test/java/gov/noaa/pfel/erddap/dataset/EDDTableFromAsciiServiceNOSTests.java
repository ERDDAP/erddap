package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromAsciiServiceNOSTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests nosCoops Water Level datasets. These datasets are based on plain text responses from
   * forms listed at https://opendap.co-ops.nos.noaa.gov/axis/text.html (from "text" link at bottom
   * of https://opendap.co-ops.nos.noaa.gov/axis/ ) These datasets were hard to work with: Different
   * services support different stations (WLVDM is very diffent). Different stations support
   * different datums. The web service which indicates which station supports which datum is
   * imperfect (see above; e.g., it never mentions STND which most, not all, support). For the web
   * services, the parameters sometimes change form: nosCoopsWLHLTP uses datum 0=MLLW 1=STND, others
   * use acronym The responses are sometimes inconsistent some say UTC, some GMT some say Feet when
   * the return values are Meters (nosCoopsWLR1) some respond "Station Name", some "StationName" For
   * the soap services, there is no RESTfull (URL) access. So harder to work with. There is a time
   * limit of 30 day's worth of data Approx. when is the latest available Verified data? The Great
   * Lakes stations have completely separate services:
   * http://tidesandcurrents.noaa.gov/station_retrieve.shtml?type=Great+Lakes+Water+Level+Data
   *
   * @param idRegex is a regex indicating which datasetID's should be tested (".*" for all)
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "nosCoopsWLR1",
        "nosCoopsWLV6",
        "nosCoopsWLR60",
        "nosCoopsWLVHL",
        "nosCoopsWLTPHL",
        "nosCoopsWLTP60",
        "nosCoopsMAT",
        "nosCoopsMBP",
        "nosCoopsMC",
        "nosCoopsMRF",
        "nosCoopsMRH",
        "nosCoopsMWT",
        "nosCoopsMW",
        "nosCoopsMV",
        "nosCoopsCA"
      })
  @TagIncompleteTest // "this.stationTable" is null
  void testNosCoops(String idRegex) throws Throwable {
    // String2.log("\n******************
    // EDDTableFromAsciiServiceNOS.testNosCoopsWL\n");
    // testVerboseOn();
    int language = 0;
    // EDD.debugMode=true;
    String results, query, tName, expected;
    // (0, 11) causes all of these to end in 'T'
    String yesterday =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(1, Double.NaN)).substring(0, 11);
    String daysAgo5 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(5, Double.NaN)).substring(0, 11);
    String daysAgo10 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(10, Double.NaN)).substring(0, 11);
    String daysAgo20 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(20, Double.NaN)).substring(0, 11);
    String daysAgo30 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(30, Double.NaN)).substring(0, 11);
    String daysAgo40 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(40, Double.NaN)).substring(0, 11);
    String daysAgo70 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(70, Double.NaN)).substring(0, 11);
    String daysAgo72 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(72, Double.NaN)).substring(0, 11);
    String daysAgo90 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(90, Double.NaN)).substring(0, 11);
    String daysAgo92 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(92, Double.NaN)).substring(0, 11);

    String daysAhead5 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(-5, Double.NaN)).substring(0, 11);
    String daysAhead7 =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(-7, Double.NaN)).substring(0, 11);
    String commonProblems =
        "Small differences (e.g., a missing line) and \"Your query produced no matching results\" are common.";

    // Raw 6 minute
    if ("nosCoopsWLR6".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLR6();

      query =
          "&stationID=\"9414290\"&datum=\"MLLW\"&time>="
              + yesterday
              + "21:00&time<="
              + yesterday
              + "22:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected = // this changes every day
          // 9414290,San
          // Francisco,CA,1854-06-30T00:00:00Z,FTPC1,"NWLON,PORTS",-122.4659,37.8063," +
          // yesterday + "21:00:00Z,MLLW,1,WL,1.094,0.04,NaN,0,0,0
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,dcp,sensor,waterLevel,sigma,O,F,R,L\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,,m,m,count,,,\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:00:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:06:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:12:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:18:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:24:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:30:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:36:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:42:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:48:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:54:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "22:00:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Raw 1 minute
    if ("nosCoopsWLR1".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLR1();
      query =
          "&stationID=\"9414290\"&datum=\"MLLW\"&time>="
              + yesterday
              + "21:00&time<="
              + yesterday
              + "21:10";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // 9414290,San
          // Francisco,CA,1854-06-30T00:00:00Z,FTPC1,"NWLON,PORTS",-122.4659,37.8063,2015-02-02T21:00:00Z,MLLW,1,WL,1.097
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,dcp,sensor,waterLevel\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,,m\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:00:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:01:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:02:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:03:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:04:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:05:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:06:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:07:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:08:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:09:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + yesterday
              + "21:10:00Z,MLLW,1,WL,([\\-\\.\\d]{1,6}|NaN)\n";

      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Verified 6 minute
    if ("nosCoopsWLV6".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLV6();

      // 2012-11-14 and 2013-11-01 Have to experiment a lot to get actual data. (Gap
      // in Early 2013-10?)
      // list of stations: https://opendap.co-ops.nos.noaa.gov/stations/index.jsp
      // their example=8454000 SF=9414290 Honolulu=1612340
      String daysAgo = daysAgo40;
      query =
          "&stationID=\"9414290\"&datum=\"MLLW\"&time>="
              + daysAgo
              + "21:00"
              + "&time<="
              + daysAgo
              + "22:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected = // changes every day
          // 9414290,San
          // Francisco,CA,1854-06-30T00:00:00Z,FTPC1,"NWLON,PORTS",-122.4659,37.8063,2014-12-25T21:00:00Z,MLLW,1.841,0.048,0,0,0,0
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,waterLevel,sigma,I,F,R,L\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,m,m,,,,\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:06:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:12:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:18:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:24:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:30:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:36:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:42:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:48:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "21:54:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n"
              + "9414290,San Francisco,CA,1854-06-30T00:00:00Z,FTPC1,\"NWLON,PORTS\",-122.4659,37.8063,"
              + daysAgo
              + "22:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Verified hourly
    if ("nosCoopsWLR60".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLV60();
      // 2012-11-14 and 2013-11-01 Have to experiment a lot to get actual data. (Gap
      // in Early 2013-10?)
      // list of stations: https://opendap.co-ops.nos.noaa.gov/stations/index.jsp
      // their example=8454000 SF=9414290 Honolulu=1612340
      // 2012-11-14 was stationID=9044020 but that stopped working
      String daysAgo = daysAgo40;
      query =
          "&stationID=\"8454000\"&datum=\"MLLW\"&time>="
              + daysAgo
              + "14:00&time<="
              + daysAgo
              + "23:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,"NWLON,PORTS",-71.4011,41.8067,2014-12-25T14:00:00Z,MLLW,1.641,0.002,0,0
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,waterLevel,sigma,I,L\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,m,m,,\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "14:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "15:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "16:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "17:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "18:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "19:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "20:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "21:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "22:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "23:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),[0|1],(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Verified high low
    if ("nosCoopsWLVHL".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLVHL();
      // no recent data. always ask for daysAgo72 ... daysAgo70
      // 2012-11-14 was stationID=9044020 but that stopped working
      // 2012-11-14 and 2013-11-01 Have to experiment a lot to get actual data. (Gap
      // in Early 2013-10?)
      // list of stations: https://opendap.co-ops.nos.noaa.gov/stations/index.jsp
      // their example=8454000 SF=9414290 Honolulu=1612340
      String daysAgo = daysAgo72;
      query =
          "&stationID=\"8454000\"&datum=\"MLLW\"&time>="
              + daysAgo
              + "00:00&time<="
              + daysAgo
              + "23:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // number of lines in results varies
      String sar[] = String2.split(results, '\n');
      expected =
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,waterLevel,type,I,L\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,m,,,\n";
      for (int i = 2; i < sar.length - 1; i++)
        expected +=
            // eg
            // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,"NWLON,PORTS",-71.4006,41.8067,2014-11-23T00:30:00Z,MLLW,1.374,H,0,0
            // 2020-04-27 was -71.4006,41.8067
            "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
                + daysAgo
                + "..:..:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),.{1,2},(0|1),(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Tide Predicted High Low
    // I needed a different way to specify datum (just 0=MLLW or 1=STND)
    // so always use datum=0; don't let user specify datum
    // works:
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/highlowtidepred/plain/response.jsp?
    // unit=0&timeZone=0&metadata=yes&Submit=Submit&stationId=8454000&beginDate=20101123+00:00&endDate=20101125+00:00&datum=0
    // RESULT FORMAT IS VERY DIFFERENT so this class processes this dataset
    // specially
    if ("nosCoopsWLTPHL".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLTPHL();

      String daysAhead = daysAhead5;
      query = "&stationID=\"8454000\"&time>=" + daysAhead + "00:00&time<=" + daysAhead + "23:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // number of lines in results varies
      String sar[] = String2.split(results, '\n');
      expected =
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,waterLevel,type\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,m,\n";
      for (int i = 2; i < sar.length - 1; i++)
        expected +=
            // eg
            // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,"NWLON,PORTS",-71.4006,41.8067,2015-02-08T08:48:00Z,MLLW,-0.03
            // 2020-04-27 was -71.4006,41.8067
            "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
                + daysAhead
                + "..:..:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),.{1,2}\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Tide Predicted 6 minute
    // I needed a different way to specify datum (just 0=MLLW or 1=STND)
    // so always use datum=0; don't let user specify datum
    // And always use dataInterval=6 (minutes).
    // works:https://opendap.co-ops.nos.noaa.gov/axis/webservices/predictions/plain/response.jsp?
    // unit=0&timeZone=0&datum=0&dataInterval=6&beginDate=20101122+00:00&endDate=20101122+02:00
    // &metadata=yes&Submit=Submit&stationId=1611400
    if ("nosCoopsWLTP6".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLTP6();

      String daysAhead = daysAhead5;
      query =
          "&stationID=\"8454000\"&datum=\"MLLW\"&time>="
              + daysAhead
              + "00:00&time<="
              + daysAhead
              + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,"NWLON,PORTS",-71.4006,41.8067,2015-02-08T00:00:00Z,MLLW,0.597
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,predictedWL\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,m\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:06:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:12:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:18:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:24:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:30:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:36:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:42:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:48:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:54:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "01:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Tide Predicted 60 minute
    // I needed a different way to specify datum (just 0=MLLW or 1=STND)
    // so always use datum=0; don't let user specify datum
    // And always use dataInterval=60 (minutes).
    // works:https://opendap.co-ops.nos.noaa.gov/axis/webservices/predictions/plain/response.jsp?
    // unit=0&timeZone=0&datum=0&dataInterval=60&beginDate=20101122+00:00&endDate=20101122+02:00
    // &metadata=yes&Submit=Submit&stationId=1611400
    if ("nosCoopsWLTP60".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsWLTP60();

      String daysAhead = daysAhead5;
      query = "&stationID=\"8454000\"&time>=" + daysAhead + "00:00&time<=" + daysAhead + "10:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,"NWLON,PORTS",-71.4006,41.8067,2015-02-08T00:00:00Z,MLLW,0.597
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,datum,predictedWL\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,m\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "00:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "01:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "02:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "03:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "04:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "05:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "06:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "07:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "08:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "09:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAhead
              + "10:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Air Temperature
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/airtemperature/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMAT".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMAT();

      String daysAgo = yesterday;
      query = "&stationID=\"8454000\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4006,41.8067,2015-02-02T00:00:00Z,1,AT,-1.0,0,0,0
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,AT,X,N,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,degree_C,,,\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:00:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:06:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:12:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:18:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:24:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:30:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:36:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:42:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:48:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:54:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "01:00:00Z,1,AT,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Barometric Pressure
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/barometricpressure/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMBP".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMBP();

      String daysAgo = yesterday;
      query = "&stationID=\"8454000\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4006,41.8067,2015-02-02T00:00:00Z,1,BP,1019.5,0,0,0
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,BP,X,N,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,hPa,,,\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:00:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:06:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:12:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:18:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:24:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:30:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:36:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:42:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:48:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:54:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "01:00:00Z,1,BP,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Conductivity
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/conductivity/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMC".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMC();

      // String id = "8452660";
      // String cityLL =
      // ",Newport,RI,1930-09-11T00:00:00Z,NWPR1,\"NWLON,PORTS\",-71.3261,41.5044,";
      String id = "8447386";
      String cityLL =
          ",Fall River,MA,1955-10-28T00:00:00Z,FRVM3,\"PORTS,Global\",-71.1641,41.7043,";

      String daysAgo = daysAgo20;
      query = "&stationID=\"" + id + "\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // 8452660,Newport,RI,1930-09-11T00:00:00Z,NWPR1,"NWLON,PORTS",-71.3267,41.505,2014-12-25T00:00:00Z,1,CN,31.16,0,0,0
          // 8452660,Newport,RI,1930-09-11T00:00:00Z,NWPR1,"NWLON,PORTS",-71.3261,41.5044,2014-12-25T00:00:00Z,1,CN,31.16,0,0,0
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,CN,X,N,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,mS/cm,,,\n"
              + id
              + cityLL
              + daysAgo
              + "00:00:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:06:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:12:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:18:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:24:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:30:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:36:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:42:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:48:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "00:54:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n"
              + id
              + cityLL
              + daysAgo
              + "01:00:00Z,1,CN,([\\-\\.\\d]{1,6}|NaN),0,0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Rain Fall
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/rainfall/plain/response.jsp?timeZone=0&metadata=yes&Submit=Submit&stationId=9752619&beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMRF".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMRF();

      String daysAgo = daysAgo70; // yesterday;
      query = "&stationID=\"9752619\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "00:54";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // 9752619,"Isabel Segunda, Vieques
          // Island",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.444,18.153,2015-02-02T00:00:00Z,1,J1,0.0,0,0
          // 2019-03-19 was -65.4438, now -65-4439
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,RF,X,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,mm,,\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:00:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:06:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:12:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:18:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:24:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:30:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:36:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:42:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:48:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"
              + "9752619,\"Isabel Segunda, Vieques Island\",PR,2007-09-13T00:00:00Z,VQSP4,COASTAL,-65.4439,18.1525,"
              + daysAgo
              + "00:54:00Z,1,J1,\\d\\.\\d,0,(0|1)\n"; // +
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Relative Humidity
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/relativehumidity/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=9063063
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMRH".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMRH();

      String daysAgo = daysAgo5; // yesterday;
      query = "&stationID=\"9063063\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // 9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,2015-02-02T00:00:00Z,1,RH,91.4,0,0,0
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,RH,X,N,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,percent,,,\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:00:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:06:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:12:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:18:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:24:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:30:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:36:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:42:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:48:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "00:54:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n"
              + "9063063,Cleveland,OH,1860-01-01T00:00:00Z,CNDO1,NWLON,-81.6355,41.5409,"
              + daysAgo
              + "01:00:00Z,1,RH,\\d\\d\\.\\d,0,0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Water Temperature
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/watertemperature/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMWT".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMWT();

      // 2014-01 "yesterday" fails, so seek a specific date
      String daysAgo = "2010-10-24T"; // yesterday;
      query = "&stationID=\"8454000\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4006,41.8067,2010-10-24T00:00:00Z,1,WT,14.8,0,0,0
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,WT,X,N,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,degree_C,,,\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:00:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:06:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:12:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:18:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:24:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:30:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:36:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:42:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:48:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:54:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "01:00:00Z,1,WT,\\d{1,2}.\\d,0,0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Wind
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMW".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMW();

      String daysAgo = yesterday;
      query = "&stationID=\"8454000\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // eg
          // 8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4006,41.8067,2015-02-02T00:00:00Z,1,WS,1.86,22.72,3.2,0,0
          // 2020-04-27 was -71.4006,41.8067
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,dcp,sensor,WS,WD,WG,X,R\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,,,m s-1,degrees_true,m s-1,,\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:00:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:06:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:12:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:18:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:24:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:30:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:36:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:42:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:48:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "00:54:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n"
              + "8454000,Providence,RI,1938-06-03T00:00:00Z,FOXR1,\"NWLON,PORTS\",-71.4012,41.8071,"
              + daysAgo
              + "01:00:00Z,1,WS,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,(0|1)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Visibility
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/visibility/plain/response.jsp?
    // timeZone=0&metadata=yes&Submit=Submit&stationId=8737005
    // &beginDate=20101024+00:00&endDate=20101026+00:00
    if ("nosCoopsMV".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsMV();

      // 2014-01 "yesterday" fails, so seek a specific date
      String daysAgo = "2010-10-24T"; // yesterday;
      query = "&stationID=\"8737005\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          "stationID,stationName,state,dateEstablished,shefID,deployment,longitude,latitude,time,Vis\n"
              + ",,,UTC,,,degrees_east,degrees_north,UTC,nautical_miles\n";
      // number of lines in results varies
      String sar[] = String2.split(results, '\n');
      for (int i = 2; i < sar.length - 1; i++)
        expected +=
            // 8737005,Pinto
            // Island,AL,2009-12-15T00:00:00Z,PTOA1,PORTS,-88.0311,30.6711,2010-10-24T00:00:00Z,5.4
            // 8737005,Pinto
            // Island,AL,2009-12-15T00:00:00Z,PTOA1,PORTS,-88.031,30.6712,2010-10-24T00:00:00Z,5.4
            // 2020-04-27 was Pinto Island, not ...Visibility
            "8737005,Pinto Island Visibility,AL,2009-12-15T00:00:00Z,PTOA1,PORTS,-88.031,30.6712,"
                + daysAgo
                + "..:..:00Z,([\\-\\.\\d]{1,6}|NaN)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    // Active Currents
    // works
    // https://opendap.co-ops.nos.noaa.gov/axis/webservices/currents/plain/response.jsp?
    // metadata=yes&Submit=Submit&stationId=db0301&beginDate=20101121+00:00&endDate=20101121+01:00
    if ("nosCoopsCA".matches(idRegex)) {
      EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsCA();

      // 2014-01 "yesterday" fails, so seek a specific date
      String daysAgo = "2010-11-21T"; // yesterday;
      query = "&stationID=\"db0301\"&time>=" + daysAgo + "00:00&time<=" + daysAgo + "01:00";
      tName =
          edd.makeNewFileForDapQuery(
              language,
              null,
              null,
              query,
              EDStatic.fullTestCacheDirectory,
              edd.className() + "_" + edd.datasetID(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,2010-11-21T00:03:00Z,1.526,199.0
          "stationID,stationName,dateEstablished,longitude,latitude,time,CS,CD\n"
              + ",,UTC,degrees_east,degrees_north,UTC,knots,degrees_true\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:03:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:09:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              +
              // "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462," + daysAgo +
              // "00:15:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" + //not on
              // 2010-11-21
              "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:21:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:27:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:33:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:39:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:45:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:51:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n"
              + "db0301,Philadelphia,2003-03-25T00:00:00Z,-75.1396,39.9462,"
              + daysAgo
              + "00:57:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n";
      Test.ensureLinesMatch(results, expected, "results=\n" + results);
    }

    /*
     * //Verified Daily Mean NEVER WORKED,
     * //Their example at
     * https://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelverifieddaily/
     * plain/
     * // with stationID=9044020 works,
     * // but using it here returns ERDDAP error message (not NOS service error
     * message)
     * //
     * "Your query produced no matching results. (There are no matching stations.)"
     * if ("nosCoopsWLVDM".matches(idRegex)) {
     * try {
     * EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "nosCoopsWLVDM");
     * query = "&stationID=\"1612340\"&datum=\"MLLW\"&time>=" + daysAgo72 +
     * "00:00&time<=" + daysAgo70 + "00:00";
     * tName = edd.makeNewFileForDapQuery(language, null, null, query,
     * EDStatic.fullTestCacheDirectory,
     * edd.className() + "_" + edd.datasetID(), ".csv");
     * results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory +
     * tName);
     * expected =
     * "zztop\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     *
     * } catch (Throwable t) {
     * String2.pressEnterToContinue("\n" + MustBe.throwableToString(t) +
     * "\n*** THIS HAS NEVER WORKED." +
     * "\nResponse says \"Water Level Daily Mean Data is only available for Great Lakes stations,"
     * +
     * "\nand not for coastal stations.\" but stations table only lists coastal stations."
     * );
     * }
     * }
     */
    // There is no plain text service for the Survey Currents Survey dataset at
    // https://opendap.co-ops.nos.noaa.gov/axis/text.html

    // EDD.debugMode = false;

  }
}
