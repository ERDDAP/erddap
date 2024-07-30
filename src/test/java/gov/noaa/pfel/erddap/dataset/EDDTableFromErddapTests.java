package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.DAS;
import dods.dap.DConnect;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagExternalERDDAP;
import tags.TagImageComparison;
import tags.TagLocalERDDAP;
import tags.TagMissingDataset;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromErddapTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    int po;

    // test local generateDatasetsXml. In tests, always use non-https url.
    String results = EDDTableFromErddap.generateDatasetsXml(EDStatic.erddapUrl, true) + "\n";
    String2.log("results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose", "EDDTableFromErddap", EDStatic.erddapUrl, "true", "-1"
                }, // keep original names?, defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String expected =
        "<dataset type=\"EDDTableFromErddap\" datasetID=\"erdGlobecBottle\" active=\"true\">\n"
            + "    <!-- GLOBEC NEP Rosette Bottle Data (2002) -->\n"
            + "    <sourceUrl>http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle</sourceUrl>\n"
            + "</dataset>\n";
    String fragment = expected;
    String2.log("\nresults=\n" + results);
    po = results.indexOf(expected.substring(0, 70));
    try {
      Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");
    } catch (Throwable t) {
      throw new RuntimeException(
          "Unexpected error. This test requires erdGlobecBottle in localhost ERDDAP.", t);
    }

    expected =
        "<!-- Of the datasets above, the following datasets are EDDTableFromErddap's at the remote ERDDAP.\n";
    po = results.indexOf(expected.substring(0, 20));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
    try {
      Test.ensureTrue(results.indexOf("rGlobecBottle", po) > 0, "results=\n" + results);
    } catch (Throwable t) {
      throw new RuntimeException(
          "Unexpected error. This test requires rGlobecBottle in localhost ERDDAP.", t);
    }

    /*
     * //ensure it is ready-to-use by making a dataset from it
     * //NO - don't mess with existing erdGlobecBottle
     * String tDatasetID = "erdGlobecBottle";
     * EDD.deleteCachedDatasetInfo(tDatasetID);
     * EDD edd = oneFromXmlFragment(null, fragment);
     * Test.ensureEqual(edd.title(), "GLOBEC NEP Rosette Bottle Data (2002)", "");
     * Test.ensureEqual(edd.datasetID(), tDatasetID, "");
     * Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
     * "cruise_id, ship, cast, longitude, latitude, time, bottle_posn, chl_a_total, chl_a_10um, phaeo_total, phaeo_10um, sal00, sal11, temperature0, temperature1, fluor_v, xmiss_v, PO4, N_N, NO3, Si, NO2, NH4, oxygen, par"
     * ,
     * "");
     */
  }

  /** The basic tests of this class (erdGlobecBottle). */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagLocalERDDAP
  @TagImageComparison
  void testBasic(boolean tRedirect) throws Throwable {
    // String2.log("\n****************** EDDTableFromErddap.testBasic(" +
    // tRedirect + ")\n");
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    int tPo;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 10); // just 10 till 1.40 released, then 14
    String mapDapQuery = "status,testLong,sst&.draw=markers";
    String dir = EDStatic.fullTestCacheDirectory;
    String tID = tRedirect ? "rTestNccsvScalar11" : "rTestNccsvScalarNoRedirect11";
    String url = "http://localhost:8080/cwexperimental/tabledap/" + tID;
    String2.log("* This test requires datasetID=" + tID + " in the localhost ERDDAP.");

    // *** test getting das for entire dataset
    results = SSR.getUrlResponseStringUnchanged(url + ".nccsvMetadata");
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.10, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_data_type,Trajectory\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,Easternmost_Easting,-130.2576d\n"
            + "*GLOBAL*,featureType,Trajectory\n"
            + "*GLOBAL*,geospatial_lat_max,28.0003d\n"
            + "*GLOBAL*,geospatial_lat_min,27.9998d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,-130.2576d\n"
            + "*GLOBAL*,geospatial_lon_min,-132.1591d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,infoUrl,https://erddap.github.io/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,Northernmost_Northing,28.0003d\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,27.9998d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n"
            + "*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "*GLOBAL*,Westernmost_Easting,-132.1591d\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "ship,ioos_category,Identifier\n"
            + "ship,long_name,Ship\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,actual_range,2017-03-23T00:45:00Z\\n2017-03-23T23:45:00Z\n"
            + "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Time\n"
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "latitude,*DATA_TYPE*,double\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,actual_range,27.9998d,28.0003d\n"
            + "latitude,axis,Y\n"
            + "latitude,colorBarMaximum,90.0d\n"
            + "latitude,colorBarMinimum,-90.0d\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Latitude\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,units,degrees_north\n"
            + "longitude,*DATA_TYPE*,double\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,actual_range,-132.1591d,-130.2576d\n"
            + "longitude,axis,X\n"
            + "longitude,colorBarMaximum,180.0d\n"
            + "longitude,colorBarMinimum,-180.0d\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Longitude\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,actual_range,\"'\\t'\",\"'\u20ac'\"\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "status,ioos_category,Unknown\n"
            + "status,long_name,Status\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,_FillValue,127b\n"
            + "testByte,actual_range,-128b,126b\n"
            + "testByte,ioos_category,Unknown\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,_FillValue,255ub\n"
            + "testUByte,actual_range,0ub,254ub\n"
            + "testUByte,ioos_category,Unknown\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,_FillValue,9223372036854775807L\n"
            + "testLong,actual_range,-9223372036854775808L,9223372036854775806L\n"
            + "testLong,ioos_category,Unknown\n"
            + "testLong,long_name,Test of Longs\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,_FillValue,18446744073709551615uL\n"
            + "testULong,actual_range,0uL,18446744073709551614uL\n"
            + "testULong,ioos_category,Unknown\n"
            + "testULong,long_name,Test ULong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,actual_range,10.0f,10.9f\n"
            + "sst,colorBarMaximum,32.0d\n"
            + "sst,colorBarMinimum,0.0d\n"
            + "sst,ioos_category,Temperature\n"
            + "sst,long_name,Sea Surface Temperature\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .nccsv all
    userDapQuery = "";
    results = SSR.getUrlResponseStringUnchanged(url + ".nccsv");
    // String2.log(results);
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.10, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,cdm_data_type,Trajectory\n"
            + "*GLOBAL*,cdm_trajectory_variables,ship\n"
            + "*GLOBAL*,creator_email,bob.simons@noaa.gov\n"
            + "*GLOBAL*,creator_name,Bob Simons\n"
            + "*GLOBAL*,creator_type,person\n"
            + "*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n"
            + "*GLOBAL*,Easternmost_Easting,-130.2576d\n"
            + "*GLOBAL*,featureType,Trajectory\n"
            + "*GLOBAL*,geospatial_lat_max,28.0003d\n"
            + "*GLOBAL*,geospatial_lat_min,27.9998d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,-130.2576d\n"
            + "*GLOBAL*,geospatial_lon_min,-132.1591d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,history,"
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        // T17:35:08Z (local files)\\n2017-04-18T17:35:08Z
        "http://127.0.0.1:8080/cwexperimental/tabledap/"
            + (tRedirect ? "testNccsvScalar11" : tID)
            + ".nccsv\n"
            + "*GLOBAL*,infoUrl,https://erddap.github.io/NCCSV.html\n"
            + "*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n"
            + "*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n"
            + "*GLOBAL*,Northernmost_Northing,28.0003d\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,27.9998d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n"
            + "*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n"
            + "*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n"
            + "*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n"
            + "*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n"
            + "*GLOBAL*,title,NCCSV Demonstration\n"
            + "*GLOBAL*,Westernmost_Easting,-132.1591d\n"
            + "ship,*DATA_TYPE*,String\n"
            + "ship,cf_role,trajectory_id\n"
            + "ship,ioos_category,Identifier\n"
            + "ship,long_name,Ship\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Time\n"
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "latitude,*DATA_TYPE*,double\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,axis,Y\n"
            + "latitude,colorBarMaximum,90.0d\n"
            + "latitude,colorBarMinimum,-90.0d\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Latitude\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,units,degrees_north\n"
            + "longitude,*DATA_TYPE*,double\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,axis,X\n"
            + "longitude,colorBarMaximum,180.0d\n"
            + "longitude,colorBarMinimum,-180.0d\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Longitude\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,units,degrees_east\n"
            + "status,*DATA_TYPE*,char\n"
            + "status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n"
            + "status,ioos_category,Unknown\n"
            + "status,long_name,Status\n"
            + "testByte,*DATA_TYPE*,byte\n"
            + "testByte,_FillValue,127b\n"
            + "testByte,ioos_category,Unknown\n"
            + "testByte,units,\"1\"\n"
            + "testUByte,*DATA_TYPE*,ubyte\n"
            + "testUByte,_FillValue,255ub\n"
            + "testUByte,ioos_category,Unknown\n"
            + "testUByte,units,\"1\"\n"
            + "testLong,*DATA_TYPE*,long\n"
            + "testLong,_FillValue,9223372036854775807L\n"
            + "testLong,ioos_category,Unknown\n"
            + "testLong,long_name,Test of Longs\n"
            + "testLong,units,\"1\"\n"
            + "testULong,*DATA_TYPE*,ulong\n"
            + "testULong,_FillValue,18446744073709551615uL\n"
            + "testULong,ioos_category,Unknown\n"
            + "testULong,long_name,Test ULong\n"
            + "testULong,units,\"1\"\n"
            + "sst,*DATA_TYPE*,float\n"
            + "sst,colorBarMaximum,32.0d\n"
            + "sst,colorBarMinimum,0.0d\n"
            + "sst,ioos_category,Temperature\n"
            + "sst,long_name,Sea Surface Temperature\n"
            + "sst,missing_value,99.0f\n"
            + "sst,standard_name,sea_surface_temperature\n"
            + "sst,testBytes,-128b,0b,127b\n"
            + "sst,testChars,\"','\",\"'\"\"'\",\"'\u20ac'\"\n"
            + "sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n"
            + "sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n"
            + "sst,testInts,-2147483648i,0i,2147483647i\n"
            + "sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n"
            + "sst,testShorts,-32768s,0s,32767s\n"
            + "sst,testStrings,\" a\\t~\u00fc,\\n'z\"\"\u20ac\"\n"
            + "sst,testUBytes,0ub,127ub,255ub\n"
            + "sst,testUInts,0ui,2147483647ui,4294967295ui\n"
            + "sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n"
            + "sst,testUShorts,0us,32767us,65535us\n"
            + "sst,units,degree_C\n"
            + "\n"
            + "*END_METADATA*\n"
            + "ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\u00fc,,,,,\n"
            + "\" a\\t~\u00fc,\\n'z\"\"\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n"
            + "*END_DATA*\n";
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // test .png
    String baseName = "EDDTableFromErddap_GraphM_" + tRedirect;
    tName = baseName + ".png";
    SSR.downloadFile(
        url + ".png?" + mapDapQuery,
        Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + tName,
        true);
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
  } // end of testBasic

  /** This tests making a fromErddap from a fromErddap on coastwatch. */
  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testFromErddapFromErddap() throws Throwable {
    String2.log("\n*** EDDTableFromErddap.testFromErddapFromErddap");
    EDDTable edd = (EDDTableFromErddap) EDDTestDataset.gettestFromErddapFromErddap();
    // String2.log(edd.toString());
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testDegreesSignAttribute() throws Throwable {
    String2.log("\n*** EDDTableFromErddap.testDegreesSignAttribute");
    String url =
        // "http://localhost:8080/cwexperimental/tabledap/erdCalcofiSur";
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdCalcofiSur";
    DConnect dConnect = new DConnect(url, true, 1, 1);
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    String results = OpendapHelper.getDasString(das);
    // String expected = "zztop";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    EDDTable edd =
        (EDDTableFromErddap)
            EDDTableFromErddap.oneFromDatasetsXml(null, "testCalcofiSurFromErddap");
    // String2.log(edd.toString());

  }

  /** This tests dealing with remote not having ioos_category, but local requiring it. */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testTableNoIoosCat() throws Throwable {
    String2.log("\n*** EDDTableFromErddap.testTableNoIoosCat");

    // this failed because trajectory didn't have ioos_category
    // EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "testTableNoIoosCat");
    String url = "http://localhost:8080/cwexperimental/tabledap/testTableNoIoosCat";
    String results, expected;
    String query = "?&time%3E=2008-12-10T19%3A41%3A00Z"; // "?&time>2008-12-10T19:41:00Z";

    // *** test getting csv
    results = SSR.getUrlResponseStringUnchanged(url + ".csv" + query);
    expected =
        "trajectory,time,altitude,latitude,longitude,temperature,conductivity,salinity,density,pressure,dive_number\n"
            + ",UTC,m,degrees_north,degrees_east,Celsius,S m-1,1e-3,kg m-3,dbar,1\n"
            + "sg_114_003,2008-12-10T19:41:02Z,-7.02,21.238798,-157.86617,25.356133,5.337507,34.952133,1023.1982,7.065868,568\n"
            + "sg_114_003,2008-12-10T19:41:08Z,-6.39,21.238808,-157.86618,25.353163,5.337024,34.951065,1023.1983,6.4317517,568\n"
            + "sg_114_003,2008-12-10T19:41:14Z,-5.7,21.238813,-157.86618,25.352034,5.337048,34.95233,1023.1996,5.737243,568\n"
            + "sg_114_003,2008-12-10T19:41:19Z,-5.04,21.238823,-157.8662,25.354284,5.336977,34.950283,1023.1973,5.072931,568\n"
            + "sg_114_003,2008-12-10T19:41:25Z,-4.24,21.238829,-157.86621,25.353346,5.337251,34.95328,1023.1999,4.2677035,568\n"
            + "sg_114_003,2008-12-10T19:41:30Z,-3.55,21.238836,-157.86621,25.353527,5.3372197,34.953125,1023.1997,3.5731952,568\n"
            + "sg_114_003,2008-12-10T19:41:36Z,-2.65,21.238846,-157.86623,25.351152,5.336866,34.952633,1023.2001,2.6673148,568\n"
            + "sg_114_003,2008-12-10T19:41:42Z,-1.83,21.238852,-157.86624,25.355568,5.3372297,34.95217,1023.19836,1.841957,568\n"
            + "sg_114_003,2008-12-10T19:41:47Z,-1.16,21.23886,-157.86624,25.352736,5.3364573,34.948875,1023.1968,1.1675793,568\n"
            + "sg_114_003,2008-12-10T19:41:53Z,-0.8,21.238855,-157.86624,25.330637,5.30179,34.71056,1023.02356,0.8052271,568\n"
            + "sg_114_003,2008-12-10T19:41:59Z,-0.75,21.238853,-157.86624,25.2926,2.8720038,17.5902,1010.1601,0.7549004,568\n"
            + "sg_114_003,2008-12-10T19:42:04Z,-0.72,21.23885,-157.86623,25.25033,3.0869908,19.06109,1011.27466,0.7247044,568\n"
            + "sg_114_003,2008-12-10T19:42:10Z,-0.7,21.238853,-157.86624,25.225939,-3.494945,21.86908,1013.3882,0.70457375,568\n";
    String2.log(results);
    Test.ensureEqual(results, expected, "");

    // *** test getting jsonlCSV when (until they update) they don't offer it
    results = SSR.getUrlResponseStringUnchanged(url + ".jsonlCSV" + query);
    expected =
        "[\"sg_114_003\", \"2008-12-10T19:41:02Z\", -7.02, 21.238798, -157.86617, 25.356133, 5.337507, 34.952133, 1023.1982, 7.065868, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:08Z\", -6.39, 21.238808, -157.86618, 25.353163, 5.337024, 34.951065, 1023.1983, 6.4317517, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:14Z\", -5.7, 21.238813, -157.86618, 25.352034, 5.337048, 34.95233, 1023.1996, 5.737243, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:19Z\", -5.04, 21.238823, -157.8662, 25.354284, 5.336977, 34.950283, 1023.1973, 5.072931, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:25Z\", -4.24, 21.238829, -157.86621, 25.353346, 5.337251, 34.95328, 1023.1999, 4.2677035, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:30Z\", -3.55, 21.238836, -157.86621, 25.353527, 5.3372197, 34.953125, 1023.1997, 3.5731952, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:36Z\", -2.65, 21.238846, -157.86623, 25.351152, 5.336866, 34.952633, 1023.2001, 2.6673148, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:42Z\", -1.83, 21.238852, -157.86624, 25.355568, 5.3372297, 34.95217, 1023.19836, 1.841957, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:47Z\", -1.16, 21.23886, -157.86624, 25.352736, 5.3364573, 34.948875, 1023.1968, 1.1675793, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:53Z\", -0.8, 21.238855, -157.86624, 25.330637, 5.30179, 34.71056, 1023.02356, 0.8052271, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:41:59Z\", -0.75, 21.238853, -157.86624, 25.2926, 2.8720038, 17.5902, 1010.1601, 0.7549004, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:42:04Z\", -0.72, 21.23885, -157.86623, 25.25033, 3.0869908, 19.06109, 1011.27466, 0.7247044, 568]\n"
            + "[\"sg_114_003\", \"2008-12-10T19:42:10Z\", -0.7, 21.238853, -157.86624, 25.225939, -3.494945, 21.86908, 1013.3882, 0.70457375, 568]\n";
    String2.log(results);
    Test.ensureEqual(results, expected, "");
  }

  /** This tests quotes in an attribute. */
  @org.junit.jupiter.api.Test
  void testQuotes() throws Throwable {
    String2.log("\n*** EDDTableFromErddap.testQuotes");

    EDDTable edd = (EDDTableFromErddap) EDDTestDataset.gettestQuotes();
    String results = edd.defaultGraphQuery;
    String expected =
        // backslash was actual character in the string, now just encoding here
        "longitude,latitude,time&scientific_name=\"Sardinops sagax\"&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|||||";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests dataset from Kevin O'Brien's erddap: &lt;dataset type="EDDTableFromErddap"
   * datasetID="ChukchiSea_454a_037a_fcf4" active="true"&gt; where DConnect in local ERDDAP
   * complained: connection reset, but server said everything was fine. I made changes to DConnect
   * 2016-10-03 to deal with this problem.
   */
  @org.junit.jupiter.api.Test
  void testChukchiSea() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    EDDTable eddTable = (EDDTableFromErddap) EDDTestDataset.getChukchiSea_454a_037a_fcf4();

    // *** test getting das for entire dataset
    String2.log("\n*** EDDTableFromErddap.testChukchiSea das dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_Entire",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = // see OpendapHelper.EOL for comments
        "Attributes {\n"
            + " s {\n"
            + "  prof {\n"
            + "    Float64 actual_range 1.0, 1.0;\n"
            + "    String axis \"E\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Prof\";\n"
            + "    String point_spacing \"even\";\n"
            + "  }\n"
            + "  id {\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"profile id\";\n"
            + "  }\n"
            + "  cast {\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"cast number\";\n"
            + "  }\n"
            + "  cruise {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Cruise name\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_Entire",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 prof;\n"
            + "    String id;\n"
            + "    String cast;\n"
            + "    String cruise;\n"
            + "    Float64 time;\n"
            + "    Float32 longitude;\n"
            + "    Float32 lon360;\n"
            + "    Float32 latitude;\n"
            + "    Float32 depth;\n"
            + "    Float32 ocean_temperature_1;\n"
            + "    Float32 ocean_temperature_2;\n"
            + "    Float32 ocean_dissolved_oxygen_concentration_1_mLperL;\n"
            + "    Float32 ocean_dissolved_oxygen_concentration_2_mLperL;\n"
            + "    Float32 photosynthetically_active_radiation;\n"
            + "    Float32 ocean_chlorophyll_a_concentration_factoryCal;\n"
            + "    Float32 ocean_chlorophyll_fluorescence_raw;\n"
            + "    Float32 ocean_practical_salinity_1;\n"
            + "    Float32 ocean_practical_salinity_2;\n"
            + "    Float32 ocean_sigma_t;\n"
            + "    Float32 sea_water_nutrient_bottle_number;\n"
            + "    Float32 sea_water_phosphate_concentration;\n"
            + "    Float32 sea_water_silicate_concentration;\n"
            + "    Float32 sea_water_nitrate_concentration;\n"
            + "    Float32 sea_water_nitrite_concentration;\n"
            + "    Float32 sea_water_ammonium_concentration;\n"
            + "    Float32 ocean_dissolved_oxygen_concentration_1_mMperkg;\n"
            + "    Float32 ocean_dissolved_oxygen_concentration_2_mMperkg;\n"
            + "    Float32 ocean_oxygen_saturation_1;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n*** EDDTableFromErddap.testChukchiSea make DATA FILES\n");

    // .asc
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&id=%22ae1001c011%22", // "&id=\"ae1001c011\"",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_Data",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "prof,id,cast,cruise,time,longitude,lon360,latitude,depth,ocean_temperature_1,ocean_temperature_2,ocean_dissolved_oxygen_concentration_1_mLperL,ocean_dissolved_oxygen_concentration_2_mLperL,photosynthetically_active_radiation,ocean_chlorophyll_a_concentration_factoryCal,ocean_chlorophyll_fluorescence_raw,ocean_practical_salinity_1,ocean_practical_salinity_2,ocean_sigma_t,sea_water_nutrient_bottle_number,sea_water_phosphate_concentration,sea_water_silicate_concentration,sea_water_nitrate_concentration,sea_water_nitrite_concentration,sea_water_ammonium_concentration,ocean_dissolved_oxygen_concentration_1_mMperkg,ocean_dissolved_oxygen_concentration_2_mMperkg,ocean_oxygen_saturation_1\n"
            + ",,,,UTC,degrees_east,degrees_east,degrees_north,m,Degree_C,Degree_C,mL/L,mL/L,microEin cm-2 s-1,micrograms/L,volts,PSU,PSU,kg m-3,number,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,percent saturation\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,0.0,9.5301,NaN,NaN,NaN,NaN,NaN,NaN,31.4801,NaN,24.2852,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,1.0,9.5301,NaN,NaN,NaN,NaN,NaN,NaN,31.4801,NaN,24.2852,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,2.0,9.3234,NaN,NaN,NaN,NaN,NaN,NaN,31.2654,NaN,24.15,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,3.0,9.3112,NaN,NaN,NaN,NaN,NaN,NaN,31.2056,NaN,24.1052,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,4.0,9.3096,NaN,NaN,NaN,NaN,NaN,NaN,31.1971,NaN,24.0988,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,5.0,9.3091,NaN,NaN,NaN,NaN,NaN,NaN,31.177,NaN,24.0831,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,6.0,9.3095,NaN,NaN,NaN,NaN,NaN,NaN,31.1736,NaN,24.0804,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,7.0,9.3,NaN,NaN,NaN,NaN,NaN,NaN,31.1547,NaN,24.0671,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,8.0,9.277,NaN,NaN,NaN,NaN,NaN,NaN,31.1131,NaN,24.0382,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,9.0,8.9942,NaN,NaN,NaN,NaN,NaN,NaN,31.1465,NaN,24.1077,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,10.0,8.5791,NaN,NaN,NaN,NaN,NaN,NaN,31.2294,NaN,24.2349,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,11.0,8.446,NaN,NaN,NaN,NaN,NaN,NaN,31.2322,NaN,24.2567,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,12.0,8.3966,NaN,NaN,NaN,NaN,NaN,NaN,31.2179,NaN,24.2527,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,13.0,8.3742,NaN,NaN,NaN,NaN,NaN,NaN,31.2205,NaN,24.258,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,14.0,8.3406,NaN,NaN,NaN,NaN,NaN,NaN,31.2084,NaN,24.2534,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,15.0,8.218,NaN,NaN,NaN,NaN,NaN,NaN,31.2141,NaN,24.2756,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,16.0,8.0487,NaN,NaN,NaN,NaN,NaN,NaN,31.2508,NaN,24.3285,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,17.0,7.889,NaN,NaN,NaN,NaN,NaN,NaN,31.2932,NaN,24.3844,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,18.0,7.789,NaN,NaN,NaN,NaN,NaN,NaN,31.3088,NaN,24.4106,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,19.0,7.716,NaN,NaN,NaN,NaN,NaN,NaN,31.3123,NaN,24.4235,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,20.0,7.6077,NaN,NaN,NaN,NaN,NaN,NaN,31.3387,NaN,24.4592,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,21.0,7.5372,NaN,NaN,NaN,NaN,NaN,NaN,31.3458,NaN,24.4744,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,22.0,7.4847,NaN,NaN,NaN,NaN,NaN,NaN,31.3587,NaN,24.4917,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,23.0,7.4694,NaN,NaN,NaN,NaN,NaN,NaN,31.3592,NaN,24.4942,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,24.0,7.4452,NaN,NaN,NaN,NaN,NaN,NaN,31.3635,NaN,24.5008,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,25.0,7.4487,NaN,NaN,NaN,NaN,NaN,NaN,31.3765,NaN,24.5106,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests the /files/ "files" system. This requires testTableAscii and testTableFromErddap in
   * the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    String2.log("\n*** EDDTableFromErddap.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    try {
      // get /files/datasetID/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testTableFromErddap/.csv");
      expected =
          "Name,Last modified,Size,Description\n"
              + "subdir/,NaN,NaN,\n"
              + "31201_2009.csv,1576697736354,201320,\n"
              + "46026_2005.csv,1576697015884,621644,\n"
              + "46028_2005.csv,1576697015900,623250,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get /files/datasetID/
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testTableFromErddap/");
      Test.ensureTrue(results.indexOf("subdir&#x2f;") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("subdir/") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("31201&#x5f;2009&#x2e;csv") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">201320<") > 0, "results=\n" + results);

      // get /files/datasetID/subdir/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/.csv");
      expected =
          "Name,Last modified,Size,Description\n"
              + "46012_2005.csv,1576697015869,622197,\n"
              + "46012_2006.csv,1576697015884,621812,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // download a file in root
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testTableFromErddap/31201_2009.csv");
      expected =
          "This is a header line.\n"
              + "*** END OF HEADER\n"
              + "# a comment line\n"
              + "longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp\n"
              + "# a comment line\n"
              + "degrees_east, degrees_north, m, UTC, , degrees_true, m s-1, degree_C, degree_C\n"
              + "# a comment line\n"
              + "-48.13, -27.7, 0.0, 2005-04-19T00:00:00Z, 31201, NaN, NaN, NaN, 24.4\n"
              + "# a comment line\n"
              + "#-48.13, -27.7, 0.0, 2005-04-19T01:00:00Z, 31201, NaN, NaN, NaN, 24.4\n"
              + "-48.13, -27.7, 0.0, 2005-04-19T01:00:00Z, 31201, NaN, NaN, NaN, 24.4\n"
              + "-48.13, -27.7, 0.0, 2005-04-19T02:00:00Z, 31201, NaN, NaN, NaN, 24.3\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // download a file in subdir
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/46012_2005.csv");
      expected =
          "This is a header line.\n"
              + "*** END OF HEADER\n"
              + "# a comment line\n"
              + "longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp\n"
              + "# a comment line\n"
              + "degrees_east, degrees_north, m, UTC, , degrees_true, m s-1, degree_C, degree_C\n"
              + "# a comment line\n"
              + "-122.88, 37.36, 0.0, 2005-01-01T00:00:00Z, 46012, 190, 8.2, 11.8, 12.5\n"
              + "# a comment line\n"
              + "# a comment line\n"
              + "-122.88, 37.36, 0.0, 2005-01-01T01:00:00Z, 46012, 214, 8.4, 10.4, 12.5\n"
              + "-122.88, 37.36, 0.0, 2005-01-01T02:00:00Z, 46012, 210, 7.3, 9.6, 12.5\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // try to download a non-existent dataset
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent directory
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish.csv : "
              + "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
              + "http://localhost:8080/cwexperimental/files/testTableAscii/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file in existant subdir
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/gibberish.csv : "
              + "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
              + "http://localhost:8080/cwexperimental/files/testTableAscii/subdir/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException(
          "This test requires testTableAscii and testTableFromErddap in the localhost ERDDAP.\n"
              + "Unexpected error.",
          t);
    }
  }
}
