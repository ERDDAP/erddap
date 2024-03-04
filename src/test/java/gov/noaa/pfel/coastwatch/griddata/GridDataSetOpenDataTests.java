package gov.noaa.pfel.coastwatch.griddata;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import tags.TagThredds;

class GridDataSetOpenDataTests {

  @TempDir
  private static Path TEMP_DIR;

  /**
   * This does a test of getTimeSeries using a data set on West Coast
   * cwexperimental.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGetTimeSeries() throws Exception {
    String2.log("\n*** start TestBrowsers.testGetTimeSeries");
    String url = "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/GA/ssta/"; // oceanwatch

    DataHelper.verbose = true;
    GridDataSetOpendap.verbose = true;

    String tempDir = TEMP_DIR.toAbsolutePath().toString();
    Table table = null;

    GridDataSetOpendap gdst = new GridDataSetOpendap(
        "OGAssta", "GAssta", null, url,
        new String[] { "hday", "1day", "3day", "5day", "8day", "14day", "mday" },
        new int[] { 0, 24, 3 * 24, 5 * 24, 8 * 24, 14 * 24, 30 * 24, },
        "Rainbow", "Linear", "8", "32", -1, "", null, // fgdc,
        null,
        "S", 1.8, 32, "degree_F", 45, 90);

    // *** test the GridDataSetOpendap version of getTimeSeries
    // test individual makeGrid to get the values tested below in getTimeSeries
    double rLon = -128.975;
    double rLat = 36.025;

    Grid grid = gdst.makeGrid("1 day",
        "2007-12-01 12:00:00",
        rLon, rLon, rLat, rLat, 1, 1);
    Test.ensureEqual(grid.lon[0], rLon, "");
    Test.ensureEqual(grid.lat[0], rLat, "");
    Test.ensureEqual((float) grid.data[0], 16.35f, "");

    grid = gdst.makeGrid("1 day",
        "2007-12-02 12:00:00",
        rLon, rLon, rLat, rLat, 1, 1);
    Test.ensureEqual(grid.lon[0], rLon, "");
    Test.ensureEqual(grid.lat[0], rLat, "");
    Test.ensureEqual((float) grid.data[0], 16.05f, "");

    // first just test if it gets correct answers
    table = gdst.getTimeSeries(tempDir, rLon, rLat,
        "2007-12-01 12:00:00", "2007-12-20 12:00:00", "1 day");
    // String2.log("timeSeriesTable=" + table);
    Test.ensureEqual(table.nRows(), 20, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    Test.ensureEqual(table.getColumnName(0), "LON", "");
    Test.ensureEqual(table.getColumnName(1), "LAT", "");
    Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
    Test.ensureEqual(table.getColumnName(3), "TIME", "");
    Test.ensureEqual(table.getColumnName(4), "ID", "");
    Test.ensureEqual(table.getColumnName(5), "OGAssta", "");

    Test.ensureEqual(table.getColumn(0).elementTypeString(), "double", "");
    Test.ensureEqual(table.getColumn(1).elementTypeString(), "double", "");
    Test.ensureEqual(table.getColumn(2).elementTypeString(), "double", "");
    Test.ensureEqual(table.getColumn(3).elementTypeString(), "double", "");
    Test.ensureEqual(table.getColumn(4).elementTypeString(), "String", "");
    Test.ensureEqual(table.getColumn(5).elementTypeString(), "float", "");

    Test.ensureEqual(table.getDoubleData(0, 0), rLon, "");
    Test.ensureEqual(table.getDoubleData(1, 0), rLat, "");
    Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    Test.ensureEqual(table.getDoubleData(3, 0), Calendar2.isoStringToEpochSeconds("2007-12-01 12:00:00"), "");
    Test.ensureEqual(table.getStringData(4, 0), "OGAssta", "");
    Test.ensureEqual(table.getFloatData(5, 0), 16.35f, "");

    Test.ensureEqual(table.getDoubleData(0, 1), rLon, "");
    Test.ensureEqual(table.getDoubleData(1, 1), rLat, "");
    Test.ensureEqual(table.getDoubleData(2, 1), 0, "");
    Test.ensureEqual(table.getDoubleData(3, 1), Calendar2.isoStringToEpochSeconds("2007-12-02 12:00:00"), "");
    Test.ensureEqual(table.getStringData(4, 1), "OGAssta", "");
    Test.ensureEqual(table.getFloatData(5, 1), 16.05f, "");

    // HARDER TEST: data for all time does it crash? 35s first run, then 12s
    // for (int i = 30; i <= 36; i++)
    // table = gdst.getTimeSeries(tempDir, rLon, i,
    // "1980-01-01", "2099-12-31", gdst.activeTimePeriodOptions[0]);

    // *** test the GridDataSet (superclass) version of getTimeSeries
    // this is the only test of superclass' getTimeSeries
    table = null;
    table = gdst.getSuperTimeSeries(tempDir, rLon, rLat,
        "2007-12-01 12:00:00", "2007-12-20 12:00:00", "1 day");
    // String2.log("super timeSeriesTable=" + table);
    Test.ensureEqual(table.nRows(), 20, "");
    Test.ensureEqual(table.nColumns(), 6, "");
    Test.ensureEqual(table.getColumnName(0), "LON", "");
    Test.ensureEqual(table.getColumnName(1), "LAT", "");
    Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
    Test.ensureEqual(table.getColumnName(3), "TIME", "");
    Test.ensureEqual(table.getColumnName(4), "ID", "");
    Test.ensureEqual(table.getColumnName(5), "OGAssta", "");

    Test.ensureEqual(table.getDoubleData(0, 0), rLon, "");
    Test.ensureEqual(table.getDoubleData(1, 0), rLat, "");
    Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
    Test.ensureEqual(table.getDoubleData(3, 0), Calendar2.isoStringToEpochSeconds("2007-12-01 12:00:00"), "");
    Test.ensureEqual(table.getStringData(4, 0), "OGAssta", "");
    Test.ensureEqual(table.getFloatData(5, 0), 16.35f, "");

    Test.ensureEqual(table.getDoubleData(0, 1), rLon, "");
    Test.ensureEqual(table.getDoubleData(1, 1), rLat, "");
    Test.ensureEqual(table.getDoubleData(2, 1), 0, "");
    Test.ensureEqual(table.getDoubleData(3, 1), Calendar2.isoStringToEpochSeconds("2007-12-02 12:00:00"), "");
    Test.ensureEqual(table.getStringData(4, 1), "OGAssta", "");
    Test.ensureEqual(table.getFloatData(5, 1), 16.05f, "");

    // *** test of centering time periods
    // test individual makeGrid to get the values tested below in getTimeSeries
    grid = gdst.makeGrid("3 day", "2006-08-13 12:00:00", // centered time
        -130.02, -130.02, 36.02, 36.02, // close, not exact
        1, 1);
    Test.ensureEqual(grid.lon[0], -130.025, ""); // 3 day grid is offset by .025!!
    Test.ensureEqual(grid.lat[0], 36.025, "");
    Test.ensureEqual((float) grid.data[0], 19.8f, "");

    grid = gdst.makeGrid("3 day", "2006-08-08 12:00:00", // centered time
        -130.02, -130.02, 36.02, 36.02,
        1, 1);
    Test.ensureEqual(grid.lon[0], -130.025, "");
    Test.ensureEqual(grid.lat[0], 36.025, "");
    Test.ensureEqual((float) grid.data[0], 18.75f, "");

    // first just test if it gets correct answers
    table = gdst.getTimeSeries(tempDir, -130.02, 36.02,
        "2006-08-08", "2006-08-14", // begin and end centered times
        "3 day");
    // String2.log("timeSeriesTable=" + table);
    PrimitiveArray timePA = table.getColumn(3);
    PrimitiveArray dataPA = table.getColumn(5);

    // find the row corresponding to the desired time
    double seconds = Calendar2.isoStringToEpochSeconds("2006-08-13 12:00:00"); // centered time
    int row = timePA.binaryFindClosest(seconds);
    Test.ensureEqual(timePA.getDouble(row), seconds, ""); // ensure exact match 2006-08-13T23:59:59
    Test.ensureEqual(dataPA.getFloat(row), 19.8f, "");

    seconds = Calendar2.isoStringToEpochSeconds("2006-08-08 12:00:00"); // centered time
    row = timePA.binaryFindClosest(seconds);
    Test.ensureEqual(timePA.getDouble(row), seconds, ""); // ensure exact match
    Test.ensureEqual(dataPA.getFloat(row), 18.75f, "");
  }

  /**
   * This performs a simple test of this class.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void basicTest() throws Exception {
    // FileNameUtility.verbose = true;
    // FileNameUtility fnu = new
    // FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");

    String2.log("\n*** GridDataSetOpendap.basicTest");
    GridDataSetOpendap gridDataSet;

    // variables
    String internalName, sixName;
    GridDataSetOpendap.verbose = true;
    Opendap.verbose = true;

    // *********************************************************************
    internalName = "OQSux10";
    sixName = internalName.substring(1);
    String url = "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/"; // oceanwatch
    gridDataSet = new GridDataSetOpendap(
        internalName, "QSux10", null, url,
        new String[] { "1day", "3day", "4day", "8day", "14day", "mday" },
        new int[] { 24, 3 * 24, 4 * 24, 8 * 24, 14 * 24, 30 * 24 },
        "BlueWhiteRed", "Linear", "-10", "10", -1, "", null, null, "S", 1, 0, "", 1, 1);

    Grid grid = gridDataSet.makeGrid(
        "1 day", "2006-06-10 12:00:00", -135, -105, 22, 50, 300, 300);

    // set attributes
    String fileName = "TQSux10S1day_20060610_x-135_X-105_y22_Y50";
    gridDataSet.setAttributes(grid, fileName);
    grid.setStatsAttributes(false); // false -> floats

    // see if it has the expected values
    int nLat = grid.lat.length;
    int nLon = grid.lon.length;
    Test.ensureEqual(grid.globalAttributes().get("Conventions"),
        new StringArray(new String[] { "COARDS, CF-1.6, ACDD-1.3, CWHDF" }), "Conventions");
    Test.ensureEqual(grid.globalAttributes().get("title"),
        new StringArray(new String[] { "Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Science Quality, Zonal" }),
        "title");
    Test.ensureEqual(grid.globalAttributes().get("summary"), new StringArray(new String[] {
        "Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters." }),
        "summary");
    Test.ensureEqual(grid.globalAttributes().get("keywords"),
        new StringArray(new String[] { "EARTH SCIENCE > Oceans > Ocean Winds > Surface Winds" }), "keywords");
    // !!!! thredds id differs from opendap at first letter
    Test.ensureEqual(grid.globalAttributes().get("id"), new StringArray(new String[] { "TQSux10S1day" }), "id");
    Test.ensureEqual(grid.globalAttributes().get("naming_authority"),
        new StringArray(new String[] { "gov.noaa.pfeg.coastwatch" }), "naming_authority");
    Test.ensureEqual(grid.globalAttributes().get("keywords_vocabulary"),
        new StringArray(new String[] { "GCMD Science Keywords" }), "keywords_vocabulary");
    Test.ensureEqual(grid.globalAttributes().get("cdm_data_type"), new StringArray(new String[] { "Grid" }),
        "cdm_data_typ");
    Test.ensureTrue(grid.globalAttributes().getString("history").startsWith("Remote Sensing Systems, Inc."),
        "history=" + grid.globalAttributes().getString("history"));
    Test.ensureEqual(grid.globalAttributes().get("date_created"),
        new StringArray(new String[] { Calendar2.formatAsISODate(Calendar2.newGCalendarZulu()) }), "date_created");
    Test.ensureEqual(grid.globalAttributes().get("creator_name"),
        new StringArray(new String[] { "NOAA CoastWatch, West Coast Node" }), "creator_name");
    Test.ensureEqual(grid.globalAttributes().get("creator_url"),
        new StringArray(new String[] { "http://coastwatch.pfel.noaa.gov" }), "creator_url");
    Test.ensureEqual(grid.globalAttributes().get("creator_email"),
        new StringArray(new String[] { "dave.foley@noaa.gov" }), "creator_email");
    Test.ensureEqual(grid.globalAttributes().getString("institution"), "NOAA CoastWatch, West Coast Node",
        "institution=" + grid.globalAttributes().getString("institution"));
    Test.ensureEqual(grid.globalAttributes().get("project"),
        new StringArray(new String[] { "CoastWatch (http://coastwatch.noaa.gov/)" }), "project");
    Test.ensureEqual(grid.globalAttributes().get("processing_level"), new StringArray(new String[] { "3" }),
        "processing_level");
    Test.ensureEqual(grid.globalAttributes().get("acknowledgement"),
        new StringArray(new String[] { "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD" }), "acknowledgement");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lat_min"), new DoubleArray(new double[] { 22 }),
        "geospatial_lat_min");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lat_max"), new DoubleArray(new double[] { 50 }),
        "geospatial_lat_max");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lon_min"), new DoubleArray(new double[] { -135 }),
        "geospatial_lon_min");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lon_max"), new DoubleArray(new double[] { -105 }),
        "geospatial_lon_max");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lat_units"),
        new StringArray(new String[] { "degrees_north" }), "geospatial_lat_units");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lat_resolution"), new DoubleArray(new double[] { 0.125 }),
        "geospatial_lat_resolution");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lon_units"),
        new StringArray(new String[] { "degrees_east" }), "geospatial_lon_units");
    Test.ensureEqual(grid.globalAttributes().get("geospatial_lon_resolution"), new DoubleArray(new double[] { 0.125 }),
        "geospatial_lon_resolution");
    Test.ensureEqual(grid.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] { "2006-06-10T00:00:00Z" }), "time_coverage_start");
    Test.ensureEqual(grid.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] { "2006-06-11T00:00:00Z" }), "time_coverage_end");
    // Test.ensureEqual(grid.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(grid.globalAttributes().get("standard_name_vocabulary"),
        new StringArray(new String[] { "CF Standard Name Table v70" }), "standard_name_vocabulary");
    Test.ensureEqual(grid.globalAttributes().get("license"), new StringArray(new String[] {
        "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information." }),
        "license");
    Test.ensureEqual(grid.globalAttributes().get("contributor_name"),
        new StringArray(new String[] { "Remote Sensing Systems, Inc." }), "contributor_name");
    Test.ensureEqual(grid.globalAttributes().get("contributor_role"),
        new StringArray(new String[] { "Source of level 2 data." }), "contributor_role");
    Test.ensureEqual(grid.globalAttributes().get("date_issued"),
        new StringArray(new String[] { Calendar2.formatAsISODate(Calendar2.newGCalendarZulu()) }), "date_issued");
    Test.ensureEqual(grid.globalAttributes().get("references"),
        new StringArray(new String[] { "RSS Inc. Winds: http://www.remss.com/ ." }), "references");
    Test.ensureEqual(grid.globalAttributes().get("source"),
        new StringArray(new String[] { "satellite observation: QuikSCAT, SeaWinds" }), "source");
    // Google Earth
    Test.ensureEqual(grid.globalAttributes().get("Southernmost_Northing"), new DoubleArray(new double[] { 22 }),
        "southernmost");
    Test.ensureEqual(grid.globalAttributes().get("Northernmost_Northing"), new DoubleArray(new double[] { 50 }),
        "northernmost");
    Test.ensureEqual(grid.globalAttributes().get("Westernmost_Easting"), new DoubleArray(new double[] { -135 }),
        "westernmost");
    Test.ensureEqual(grid.globalAttributes().get("Easternmost_Easting"), new DoubleArray(new double[] { -105 }),
        "easternmost");

    // cwhdf attributes
    Test.ensureEqual(grid.globalAttributes().get("cwhdf_version"), new StringArray(new String[] { "3.4" }),
        "cwhdf_version"); // string
    Test.ensureEqual(grid.globalAttributes().get("satellite"), new StringArray(new String[] { "QuikSCAT" }),
        "satellite"); // string
    Test.ensureEqual(grid.globalAttributes().get("sensor"), new StringArray(new String[] { "SeaWinds" }), "sensor"); // string
    Test.ensureEqual(grid.globalAttributes().get("composite"), new StringArray(new String[] { "true" }), "composite"); // string

    Test.ensureEqual(grid.globalAttributes().get("pass_date"), new IntArray(new int[] { 13309 }), "pass_date"); // int32[nDays]
    Test.ensureEqual(grid.globalAttributes().get("start_time"), new DoubleArray(new double[] { 0 }), "start_time"); // float64[nDays]
    Test.ensureEqual(grid.globalAttributes().get("origin"),
        new StringArray(new String[] { "Remote Sensing Systems, Inc." }), "origin"); // string
    // Test.ensureEqual(grid.globalAttributes().get("history"), new StringArray(new
    // String[]{"unknown"}), "history"); //string

    Test.ensureEqual(grid.globalAttributes().get("projection_type"), new StringArray(new String[] { "mapped" }),
        "projection_type"); // string
    Test.ensureEqual(grid.globalAttributes().get("projection"), new StringArray(new String[] { "geographic" }),
        "projection"); // string
    Test.ensureEqual(grid.globalAttributes().get("gctp_sys"), new IntArray(new int[] { 0 }), "gctp_sys"); // int32
    Test.ensureEqual(grid.globalAttributes().get("gctp_zone"), new IntArray(new int[] { 0 }), "gctp_zone"); // int32
    Test.ensureEqual(grid.globalAttributes().get("gctp_parm"), new DoubleArray(new double[15]), "gctp_parm"); // float64[15
                                                                                                              // 0's]
    Test.ensureEqual(grid.globalAttributes().get("gctp_datum"), new IntArray(new int[] { 12 }), "gctp_datum");// int32
                                                                                                              // 12=WGS84

    double matrix[] = { 0, -grid.latSpacing, grid.lonSpacing, 0, grid.lon[0], grid.lat[nLat - 1] }; // up side down
    Test.ensureEqual(grid.globalAttributes().get("et_affine"), new DoubleArray(matrix), "et_affine"); // right side up

    Test.ensureEqual(grid.globalAttributes().get("rows"), new IntArray(new int[] { grid.lat.length }), "rows");// int32
                                                                                                               // number
                                                                                                               // of
                                                                                                               // rows
    Test.ensureEqual(grid.globalAttributes().get("cols"), new IntArray(new int[] { grid.lon.length }), "cols");// int32
                                                                                                               // number
                                                                                                               // of
                                                                                                               // columns
    Test.ensureEqual(grid.globalAttributes().get("polygon_latitude"), new DoubleArray(new double[] {
        grid.lat[0], grid.lat[nLat - 1], grid.lat[nLat - 1], grid.lat[0], grid.lat[0] }), "polygon_latitude");
    Test.ensureEqual(grid.globalAttributes().get("polygon_longitude"), new DoubleArray(new double[] {
        grid.lon[0], grid.lon[0], grid.lon[nLon - 1], grid.lon[nLon - 1], grid.lon[0] }), "polygon_longitude");

    // lat attributes
    Test.ensureEqual(grid.latAttributes().get("long_name"), new StringArray(new String[] { "Latitude" }),
        "lat long_name");
    Test.ensureEqual(grid.latAttributes().get("standard_name"), new StringArray(new String[] { "latitude" }),
        "lat standard_name");
    Test.ensureEqual(grid.latAttributes().get("units"), new StringArray(new String[] { "degrees_north" }), "lat units");
    Test.ensureEqual(grid.latAttributes().get("point_spacing"), new StringArray(new String[] { "even" }),
        "lat point_spacing");
    Test.ensureEqual(grid.latAttributes().get("actual_range"), new DoubleArray(new double[] { 22, 50 }),
        "lat actual_range");

    // CWHDF metadata/attributes for Latitude
    Test.ensureEqual(grid.latAttributes().get("coordsys"), new StringArray(new String[] { "geographic" }), "coordsys");// string
    Test.ensureEqual(grid.latAttributes().get("fraction_digits"), new IntArray(new int[] { 2 }), "fraction_digits"); // int32

    // lon attributes
    Test.ensureEqual(grid.lonAttributes().get("long_name"), new StringArray(new String[] { "Longitude" }),
        "lon long_name");
    Test.ensureEqual(grid.lonAttributes().get("standard_name"), new StringArray(new String[] { "longitude" }),
        "lon standard_name");
    Test.ensureEqual(grid.lonAttributes().get("units"), new StringArray(new String[] { "degrees_east" }), "lon units");
    Test.ensureEqual(grid.lonAttributes().get("point_spacing"), new StringArray(new String[] { "even" }),
        "lon point_spacing");
    Test.ensureEqual(grid.lonAttributes().get("actual_range"), new DoubleArray(new double[] { -135, -105 }),
        "lon actual_range");

    // CWHDF metadata/attributes for Longitude
    Test.ensureEqual(grid.lonAttributes().get("coordsys"), new StringArray(new String[] { "geographic" }), "coordsys"); // string
    Test.ensureEqual(grid.lonAttributes().get("fraction_digits"), new IntArray(new int[] { 2 }), "fraction_digits"); // int32

    // data attributes it's 0.125 in DataSet.properties, but files need to be
    // reprocessed to catch that
    Test.ensureEqual(grid.dataAttributes().get("long_name"),
        new StringArray(new String[] { "Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Science Quality, Zonal" }),
        "data long_name");
    Test.ensureEqual(grid.dataAttributes().get("standard_name"), new StringArray(new String[] { "x_wind" }),
        "data standard_name");
    Test.ensureEqual(grid.dataAttributes().get("units"), new StringArray(new String[] { "m s-1" }), "data units");
    Test.ensureEqual(grid.dataAttributes().get("_FillValue"), new FloatArray(new float[] { -9999999 }),
        "data _FillValue");
    Test.ensureEqual(grid.dataAttributes().get("missing_value"), new FloatArray(new float[] { -9999999 }),
        "data missing_value");
    Test.ensureEqual(grid.dataAttributes().get("numberOfObservations"), new IntArray(new int[] { 25021 }),
        "data numberOfObservations");
    Test.ensureEqual(grid.dataAttributes().get("percentCoverage"),
        new DoubleArray(new double[] { 0.46142923005993547 }), "data percentCoverage");

    // CWHDF metadata/attributes for the data: varName
    Test.ensureEqual(grid.dataAttributes().get("coordsys"), new StringArray(new String[] { "geographic" }), "coordsys"); // string
    Test.ensureEqual(grid.dataAttributes().get("fraction_digits"), new IntArray(new int[] { 1 }), "fraction_digits"); // int32
  }
}
