/* 
 * TestBrowsers Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.util.SSR;
import tags.TagExternalERDDAP;
import gov.noaa.pfel.coastwatch.griddata.*;

/**
 * This is a test of the CoastWatch browsers when they are installed.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com)
 *         2006-11-22
 */
public class TestBrowsers {

  String publicBaseUrl = "https://coastwatch.pfeg.noaa.gov/coastwatch/";
  String experimentalBaseUrl = "https://coastwatch.pfeg.noaa.gov/cwexperimental/";

  /**
   * This tests the ERD West Coast CoastWatch Browser's bathymetry data options.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testBathymetry(String url) throws Exception {
    Grid.verbose = true;

    String2.log("\n*** TestBrowsers.testBathymetry  \nurl=" + url);
    String reference, response, error, find;

    // HTTP GET an .nc grid file
    String testName = SSR.getTempDirectory() + "TestBrowsersTestBathymetry";
    String ncName = testName + ".nc";
    Grid grid = new Grid();

    // lon -180 160; a simple test within the native range of the data
    // This section has circular tests; used as basis for other tests.
    // Bath in cwbrowser180 looks right for this range.
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-180&maxLon=160&minLat=-80&maxLat=80&nLon=35&nLat=17&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 35, "");
    Test.ensureEqual(grid.lon[0], -180, "");
    Test.ensureEqual(grid.lon[34], 160, "");
    Test.ensureEqual(grid.lat.length, 17, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.lat[16], 80, "");
    Test.ensureEqual(grid.getData(0, 0), 2, ""); // -180 was -1 in original etopo2
    Test.ensureEqual(grid.getData(2, 0), 2, ""); // -160 was -1
    Test.ensureEqual(grid.getData(17, 0), 1932, ""); // -10 was 1925
    Test.ensureEqual(grid.getData(18, 0), 2352, ""); // 0 was 2351
    Test.ensureEqual(grid.getData(19, 0), 2768, ""); // 10 was 2761
    Test.ensureEqual(grid.getData(34, 0), 654, ""); // 160 was 723

    Test.ensureEqual(grid.getData(0, 16), -1521, ""); // -180 was -1519
    Test.ensureEqual(grid.getData(2, 16), -3201, ""); // -160 was -3192
    Test.ensureEqual(grid.getData(17, 16), -105, ""); // -10 was -104
    Test.ensureEqual(grid.getData(18, 16), -2593, ""); // 0 was -2591
    Test.ensureEqual(grid.getData(19, 16), -481, ""); // 10 was -481
    Test.ensureEqual(grid.getData(34, 16), -2211, ""); // 160 was -2211

    Test.ensureEqual(grid.nValidPoints, 35 * 17, "");
    Test.ensureEqual(grid.minData, -6297, ""); // was -6119
    Test.ensureEqual(grid.maxData, 5530, "");

    // -180 180 //hard part: catch 180
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-180&maxLon=180&minLat=-80&maxLat=80&nLon=37&nLat=17&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 37, "");
    Test.ensureEqual(grid.lon[0], -180, "");
    Test.ensureEqual(grid.lon[36], 180, "");
    Test.ensureEqual(grid.lat.length, 17, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.lat[16], 80, "");
    Test.ensureEqual(grid.getData(0, 0), 2, ""); // -180 was -1
    Test.ensureEqual(grid.getData(2, 0), 2, ""); // -160 was -1
    Test.ensureEqual(grid.getData(17, 0), 1932, ""); // -10 was 1925
    Test.ensureEqual(grid.getData(18, 0), 2352, ""); // 0 was 2351
    Test.ensureEqual(grid.getData(19, 0), 2768, ""); // 10 was 2761
    Test.ensureEqual(grid.getData(34, 0), 654, ""); // 160 was 723
    Test.ensureEqual(grid.getData(36, 0), 2, ""); // 180 was -1

    Test.ensureEqual(grid.getData(0, 16), -1521, ""); // -180 was -1519
    Test.ensureEqual(grid.getData(2, 16), -3201, ""); // -160 was -3192
    Test.ensureEqual(grid.getData(17, 16), -105, ""); // -10 was -104
    Test.ensureEqual(grid.getData(18, 16), -2593, ""); // 0 was -2591
    Test.ensureEqual(grid.getData(19, 16), -481, ""); // 10 was -481
    Test.ensureEqual(grid.getData(34, 16), -2211, ""); // 160 was -2211
    Test.ensureEqual(grid.getData(36, 16), -1521, ""); // 180 was -1519

    // lon 0 360 //hard parts, 180 to 359, 360
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=0&maxLon=360&minLat=-80&maxLat=80&nLon=37&nLat=17&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 37, "");
    Test.ensureEqual(grid.lon[0], -0, "");
    Test.ensureEqual(grid.lon[36], 360, "");
    Test.ensureEqual(grid.lat.length, 17, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.lat[16], 80, "");
    Test.ensureEqual(grid.getData(0, 0), 2352, ""); // 0 was 2351
    Test.ensureEqual(grid.getData(1, 0), 2768, ""); // 10 was 2761
    Test.ensureEqual(grid.getData(16, 0), 654, ""); // 160 was 723
    Test.ensureEqual(grid.getData(18, 0), 2, ""); // 180 was -1
    Test.ensureEqual(grid.getData(20, 0), 2, ""); // -160 20 was -1
    Test.ensureEqual(grid.getData(35, 0), 1932, ""); // -10 350 was 1925
    Test.ensureEqual(grid.getData(36, 0), 2352, ""); // 0 360 was 2351

    Test.ensureEqual(grid.getData(0, 16), -2593, ""); // 0 was -2591
    Test.ensureEqual(grid.getData(1, 16), -481, ""); // 10 was -481
    Test.ensureEqual(grid.getData(16, 16), -2211, ""); // 160 was -2211
    Test.ensureEqual(grid.getData(18, 16), -1521, ""); // 180 was -1519
    Test.ensureEqual(grid.getData(20, 16), -3201, ""); // -160 20 was -3192
    Test.ensureEqual(grid.getData(35, 16), -105, ""); // -10 350 was -104
    Test.ensureEqual(grid.getData(36, 16), -2593, ""); // -10 360 was -2591

    // 1 point
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-10&maxLon=-10&minLat=-80&maxLat=-80&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 1, "");
    Test.ensureEqual(grid.lon[0], -10, "");
    Test.ensureEqual(grid.lat.length, 1, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.getData(0, 0), 1932, ""); // -10

    // 1 point, but approx lon and lat
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-10.002&maxLon=-10.001&minLat=-80.002&maxLat=-80.001&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 1, "");
    Test.ensureEqual(grid.lon[0], -10, "");
    Test.ensureEqual(grid.lat.length, 1, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.getData(0, 0), 1932, ""); // -10

    // 2x2 points, approx lon and lat
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-10.01&maxLon=-9.97&minLat=-80.01&maxLat=-79.97&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 2, "");
    Test.ensureEqual((float) grid.lon[0], -10f, "");
    Test.ensureEqual((float) grid.lon[1], -9.9666666666f, "");
    Test.ensureEqual(grid.lat.length, 2, "");
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[1], -79.966666666f, "");
    Test.ensureEqual(grid.getData(0, 0), 1932, ""); // same as above
    Test.ensureEqual(grid.getData(0, 1), 1942, "");
    Test.ensureEqual(grid.getData(1, 0), 1938, "");
    Test.ensureEqual(grid.getData(1, 1), 1950, "");

    // 2x2 points
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-10&maxLon=-9&minLat=-80&maxLat=-79&nLon=2&nLat=2&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 2, "");
    Test.ensureEqual((float) grid.lon[0], -10f, "");
    Test.ensureEqual((float) grid.lon[1], -9f, "");
    Test.ensureEqual(grid.lat.length, 2, "");
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[1], -79f, "");
    Test.ensureEqual(grid.getData(0, 0), 1932, ""); // same as above
    Test.ensureEqual(grid.getData(0, 1), 1898, "");
    Test.ensureEqual(grid.getData(1, 0), 2016, "");
    Test.ensureEqual(grid.getData(1, 1), 2018, "");

    // 2x2 points, approx lon and lat
    File2.delete(ncName);
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-10.01&maxLon=-9.01&minLat=-80.01&maxLat=-79.01&fileType=.nc",
        ncName, true);
    grid.clear();
    grid.readNetCDF(ncName, null);
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 31, "");
    Test.ensureEqual((float) grid.lon[0], -10f, "");
    Test.ensureEqual((float) grid.lon[30], -9f, "");
    Test.ensureEqual(grid.lat.length, 31, "");
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[30], -79f, "");
    Test.ensureEqual(grid.getData(0, 0), 1932, ""); // same as above
    Test.ensureEqual(grid.getData(0, 30), 1898, "");
    Test.ensureEqual(grid.getData(30, 0), 2016, "");
    Test.ensureEqual(grid.getData(30, 30), 2018, "");

    // done with ncName
    File2.delete(ncName);

    // HTTP GET an .asc bath file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=bathymetryData&minLon=-10.01&maxLon=-9.01&minLat=-80.01&maxLat=-79.01&nLon=2&nLat=2&fileType=.asc");
    error = "response=" + response;
    reference = // reference data from public browser
        "ncols 2\n" +
            "nrows 2\n" +
            "xllcenter -10.0\n" +
            "yllcenter -80.0\n" +
            "cellsize 1.0\n" +
            "nodata_value -9999999\n" +
            "1898.0 2018.0\n" + // top row
            "1932.0 2016.0\n"; // bottom row
    Test.ensureEqual(response, reference, error);

    // .grd file
    File2.delete(testName + ".grd");
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-10&maxLon=-9&minLat=-80&maxLat=-79&nLon=2&nLat=2&fileType=.grd",
        testName + ".grd", true);
    grid.clear();
    grid.readGrd(testName + ".grd");
    grid.calculateStats();
    Test.ensureEqual(grid.lon.length, 2, "");
    Test.ensureEqual((float) grid.lon[0], -10f, "");
    Test.ensureEqual((float) grid.lon[1], -9f, "");
    Test.ensureEqual(grid.lat.length, 2, "");
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[1], -79f, "");
    Test.ensureEqual(grid.getData(0, 0), 1932, ""); // same as above
    Test.ensureEqual(grid.getData(0, 1), 1898, "");
    Test.ensureEqual(grid.getData(1, 0), 2016, "");
    Test.ensureEqual(grid.getData(1, 1), 2018, "");
    File2.delete(testName + ".grd");

    // test of HTTP GET an .hdf bath file (readHDF doesn't work on my files)
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=bathymetryData&minLon=-10&maxLon=-9&minLat=-80&maxLat=-79&nLon=2&nLat=2&fileType=.hdf");
    response = String2.annotatedString(response.substring(0, 100));
    error = "response=" + response;
    reference = // reference data from public browser
        "[14][3][19][1][0][190][0][0][0][0][0][30][0][1][0][0][8][242][0][0][0]\\[2][190]" +
            "[0][3][0][0][9]N[0][0][0] [2][190][0][5][0][0][9]n[0][0][0][16][2][190][0][7][0]" +
            "[0][9]~[0][0][0][16][7][171][0][8][0][0][9][381][0][0][0][4][7][170][0][8][0][0]" +
            "[9][8217][0][0][0]<[7][173][0][9][0][0][9][206][0][0][0]![7][171][0][10]";
    // Test.ensureEqual(response, reference, error); //not a good test

    // test of HTTP GET an .mat bath file (there is no read mat)
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=bathymetryData&minLon=-10&maxLon=-9&minLat=-80&maxLat=-79&nLon=2&nLat=2&fileType=.mat");
    error = "response=" + response;
    reference = // reference data from public browser
        "MATLAB 5.0 MAT-file, Created by: gov.noaa.pfel.coastwatch.Matlab, Crea";
    Test.ensureEqual(response.substring(0, reference.length()), reference, error);

    // HTTP GET an .ncHeader of a thredds bath file
    String responseArray[] = SSR.getUrlResponseLines(
        url + "?get=bathymetryData&minLon=-10&maxLon=-9&minLat=-80&maxLat=-79&nLon=2&nLat=2&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response; // reference data from public browser
    reference = "netcdf LBAthym_x-10_X-9_y-80_Y-79_nx2_ny2.nc {\n" +
        " dimensions:\n" +
        "   time = 1;   // (has coord.var)\n" +
        "   altitude = 1;   // (has coord.var)\n" +
        "   lat = 2;   // (has coord.var)\n" +
        "   lon = 2;   // (has coord.var)\n" +
        " variables:\n" +
        "   double time(time=1);\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Place Holder for Time\";\n" +
        "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "     :standard_name = \"time\";\n" +
        "     :axis = \"T\";\n" +
        "     :_CoordinateAxisType = \"Time\";\n" +
        "     :calendar = \"none\";\n" +
        "   double altitude(altitude=1);\n" +
        "     :actual_range = 0.0, 0.0; // double\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Altitude\";\n" +
        "     :positive = \"up\";\n" +
        "     :standard_name = \"altitude\";\n" +
        "     :units = \"m\";\n" +
        "     :axis = \"Z\";\n" +
        "     :_CoordinateAxisType = \"Height\";\n" +
        "     :_CoordinateZisPositive = \"up\";\n" +
        "   double lat(lat=2);\n" +
        "     :_CoordinateAxisType = \"Lat\";\n" +
        "     :actual_range = -80.0, -79.0; // double\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 6; // int\n" +
        "     :long_name = \"Latitude\";\n" +
        "     :point_spacing = \"even\";\n" +
        "     :standard_name = \"latitude\";\n" +
        "     :units = \"degrees_north\";\n" +
        "     :axis = \"Y\";\n" +
        "   double lon(lon=2);\n" +
        "     :_CoordinateAxisType = \"Lon\";\n" +
        "     :actual_range = -10.0, -9.0; // double\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 6; // int\n" +
        "     :long_name = \"Longitude\";\n" +
        "     :point_spacing = \"even\";\n" +
        "     :standard_name = \"longitude\";\n" +
        "     :units = \"degrees_east\";\n" +
        "     :axis = \"X\";\n" +
        "   float BAthym(time=1, altitude=1, lat=2, lon=2);\n" +
        "     :_FillValue = -9999999.0f; // float\n" +
        "     :actual_range = 1898.0f, 2018.0f; // float\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Bathymetry, ETOPO2v2, 0.033333 degrees, Global\";\n" +
        "     :missing_value = -9999999.0f; // float\n" +
        "     :numberOfObservations = 4; // int\n" +
        "     :percentCoverage = 1.0; // double\n" +
        "     :standard_name = \"sea_floor_depth_below_sea_level\";\n" +
        "     :units = \"m\";\n" +
        "\n" +
        " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
        " :cdm_data_type = \"Grid\";\n" +
        " :cols = 2; // int\n" +
        " :contributor_name = \"NOAA NGDC ETOPO2v2\";\n" +
        " :contributor_role = \"Source of level 3 data.\";\n" +
        " :Conventions = \"CF-1.6\";\n" +
        " :creator_email = \"dave.foley@noaa.gov\";\n" +
        " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
        " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
        " :cwhdf_version = \"3.4\";\n" +
        " :data_source = \"NOAA NGDC ETOPO2v2\";\n";
    Test.ensureEqual(response.substring(0, reference.length()), reference, "response=" + response);

    Test.ensureTrue(String2.indexOf(responseArray, " :id = \"SampledFromETOPO2v2g_MSB.raw\";") > 0,
        "response=" + response);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_end") < 0, "response=" + response);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_start") < 0, "response=" + response);
    Test.ensureTrue(String2.indexOf(responseArray, " :summary = \"" +
        "NOAA NGDC provides the ETOPO2v2 bathymetry dataset. It is a compila" +
        "tion of several datasets.\";") > 0, "response=" + response);
    Test.ensureTrue(String2.indexOf(responseArray, " :title = \"Bathymetry, ETOPO2v2, 0.033333 degrees, Global\";") > 0,
        "response=" + response);

    // test of HTTP GET an .tif bath file (there is no readTif())
    response = SSR.getUrlResponseStringUnchanged(
        // !!!there was trouble with nLon=2 and nLat=2 but more dense file seems fine
        url + "?get=bathymetryData&minLon=-10&maxLon=-9&minLat=-80&maxLat=-79&fileType=.tif");
    error = "response=" + response;
    response = String2.annotatedString(response.substring(0, Math.min(response.length(), 100)));
    reference = // reference data from public browser
        "MM[0]*[0][0][3][338][1][14][14][21]\"\")99@QMWeahur|[352][352][65533][3" +
            "82][382][164][178][178][181][194][194][7][14][21][27]\")/9@GMT[ahory[8364][8224][" +
            "65533][8221][382][164][171][178][181][188][194][198][14][27][21]\"/,6GGM[[akor[83" +
            "64][8364][8224][8221][8212][382][168][168][174][188][188][191][204][204][11][27]" +
            "[end]";
    Test.ensureEqual(response, reference, String2.annotatedString(error));

    // HTTP GET an .xyz bath file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=bathymetryData&minLon=-10.01&maxLon=-9.01&minLat=-80.01&maxLat=-79.01&nLon=2&nLat=2&fileType=.xyz");
    error = "response=" + response;
    reference = // reference data from public browser
        "-10\t-80\t1932.0\n" +
            "-9\t-80\t2016.0\n" +
            "-10\t-79\t1898.0\n" +
            "-9\t-79\t2018.0\n";
    Test.ensureEqual(response, reference, error);

  }

  /**
   * This tests the ERD West Coast CoastWatch Browser's "Edit: Grid Data" screen.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testGridScreen(String url) throws Exception {

    String2.log("\n*** TestBrowsers.testGridScreen  \nurl=" + url);
    String reference, response, error, find;

    // HTTP GET an .asc grid file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.asc");
    error = "response=" + response;
    reference = // reference data from public browser
        "ncols 121\n" +
            "nrows 113\n" +
            "xllcenter -135.0\n" +
            "yllcenter 22.0\n" +
            "cellsize 0.25\n" +
            "nodata_value -9999999\n" +
            "1.03625 2.31134 2.50077 2.20995 2.01745";
    Test.ensureEqual(response.substring(0, reference.length()), reference, error);

    // HTTP GET an .asc grid file for approximate centeredTime
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=~2006-11-21T11:12:13&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.asc");
    error = "response=" + response;
    reference = // reference data from public browser
        "ncols 121\n" +
            "nrows 113\n" +
            "xllcenter -135.0\n" +
            "yllcenter 22.0\n" +
            "cellsize 0.25\n" +
            "nodata_value -9999999\n" +
            "1.03625 2.31134 2.50077 2.20995 2.01745";
    Test.ensureEqual(response.substring(0, reference.length()), reference, error);

    // HTTP GET an .asc grid file same as above but with equivalent "endDate"
    // instead of "centeredTime"!
    // note that end date is (beginning of) last date
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&endDate=2006-11-21&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.asc");
    error = "response=" + response;
    reference = // reference data from public browser
        "ncols 121\n" +
            "nrows 113\n" +
            "xllcenter -135.0\n" +
            "yllcenter 22.0\n" +
            "cellsize 0.25\n" +
            "nodata_value -9999999\n" +
            "1.03625 2.31134 2.50077 2.20995 2.01745";
    Test.ensureEqual(response.substring(0, reference.length()), reference, error);

    // HTTP GET the same .asc grid file using the oldStyle ~endDate instead of
    // centeredTime
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&endDate=~2006-11-21T02:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.asc");
    error = "response=" + response;
    reference = // reference data from public browser
        "ncols 121\n" +
            "nrows 113\n" +
            "xllcenter -135.0\n" +
            "yllcenter 22.0\n" +
            "cellsize 0.25\n" +
            "nodata_value -9999999\n" +
            "1.03625 2.31134 2.50077 2.20995 2.01745";
    Test.ensureEqual(response.substring(0, reference.length()), reference, error);

    Grid grid = new Grid();
    String tempFileName = SSR.getTempDirectory() + "testGridScreen";

    // test of HTTP GET an .grd grid file
    File2.delete(tempFileName + ".grd");
    SSR.downloadFile( // throws Exception
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.grd",
        tempFileName + ".grd", true);
    grid.readGrd(tempFileName + ".grd");
    Test.ensureEqual(grid.lon.length, 121, "");
    Test.ensureEqual(grid.lat.length, 113, "");
    Test.ensureEqual(grid.lon[0], -135, "");
    Test.ensureEqual(grid.lon[grid.lon.length - 1], -105, "");
    Test.ensureEqual(grid.lat[0], 22, "");
    Test.ensureEqual(grid.lat[grid.lat.length - 1], 50, "");
    Test.ensureEqual((float) grid.getData(0, 0), Float.NaN, "");
    Test.ensureEqual((float) grid.getData(6, 0), -6.04076f, "");
    Test.ensureEqual((float) grid.getData(7, 0), -6.12819f, "");

    // test of HTTP GET an .hdf grid file (readHDF doesn't work on my files)
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.hdf");
    response = String2.annotatedString(response.substring(0, 100));
    error = "response=" + response;
    reference = // reference data from public browser
        "[14][3][19][1][0][216][0][0][0][0][0][30][0][1][0][0][10]\n" +
            "*[0][0][0]\\[2][190][0][3][0][0][10]\n" +
            "[8224][0][1][171]H[2][190][0][5][0][1][181][206][0][0][3][200][2][190][0][7][0][1][185][8211][0][0][3][710][7][171][0][8][0][1][189][30][0][0][0][4][7][170][0][8][0][1][189]\"[0][0][0]<[7][173][0][9][0][1][189]^[0][0][0]![7][171][0][10]\n"
            +
            "[0][1][end]";
    Test.ensureEqual(response, reference, error);

    // test of HTTP GET an .mat grid file (there is no read mat)
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-18T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.mat");
    error = "response=" + response.substring(0, 100);
    reference = // reference data from public browser
        "MATLAB 5.0 MAT-file, Created by: gov.noaa.pfel.coastwatch.Matlab, Crea";
    Test.ensureEqual(response.substring(0, reference.length()), reference, error);

    // test of HTTP GET an .nc grid file
    File2.delete(tempFileName + ".nc");
    SSR.downloadFile( // throws Exception
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.nc",
        tempFileName + ".nc", true);
    grid.readNetCDF(tempFileName + ".nc", null);
    Test.ensureEqual(grid.lon.length, 121, "");
    Test.ensureEqual(grid.lat.length, 113, "");
    Test.ensureEqual(grid.lon[0], -135, "");
    Test.ensureEqual(grid.lon[grid.lon.length - 1], -105, "");
    Test.ensureEqual(grid.lat[0], 22, "");
    Test.ensureEqual(grid.lat[grid.lat.length - 1], 50, "");
    Test.ensureEqual((float) grid.getData(0, 0), Float.NaN, "");
    Test.ensureEqual((float) grid.getData(6, 0), -6.04076f, "");
    Test.ensureEqual((float) grid.getData(7, 0), -6.12819f, "");

    // HTTP GET an .ncHeader of a thredds grid file
    String responseArray[] = SSR.getUrlResponseLines(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response; // reference data from public browser
    reference = "netcdf TQNux10S1day_20061121120000_x-135_X-105_y22_Y50_nx2147483647_ny2147483647.nc {\n" +
        " dimensions:\n" +
        "   time = 1;   // (has coord.var)\n" +
        "   altitude = 1;   // (has coord.var)\n" +
        "   lat = 113;   // (has coord.var)\n" +
        "   lon = 121;   // (has coord.var)\n" +
        " variables:\n" +
        "   double time(time=1);\n" +
        "     :actual_range = 1.1641104E9, 1.1641104E9; // double\n" + // corresponds to 2006-11-21 12:00:00
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Centered Time\";\n" +
        "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "     :standard_name = \"time\";\n" +
        "     :axis = \"T\";\n" +
        "     :_CoordinateAxisType = \"Time\";\n" +
        "   double altitude(altitude=1);\n" +
        "     :actual_range = 0.0, 0.0; // double\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Altitude\";\n" +
        "     :positive = \"up\";\n" +
        "     :standard_name = \"altitude\";\n" +
        "     :units = \"m\";\n" +
        "     :axis = \"Z\";\n" +
        "     :_CoordinateAxisType = \"Height\";\n" +
        "     :_CoordinateZisPositive = \"up\";\n" +
        "   double lat(lat=113);\n" +
        "     :_CoordinateAxisType = \"Lat\";\n" +
        "     :actual_range = 22.0, 50.0; // double\n" +
        "     :axis = \"Y\";\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 2; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Latitude\";\n" +
        "     :point_spacing = \"even\";\n" +
        "     :standard_name = \"latitude\";\n" +
        "     :units = \"degrees_north\";\n" +
        "   double lon(lon=121);\n" +
        "     :_CoordinateAxisType = \"Lon\";\n" +
        "     :actual_range = -135.0, -105.0; // double\n" +
        "     :axis = \"X\";\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 2; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Longitude\";\n" +
        "     :point_spacing = \"even\";\n" +
        "     :standard_name = \"longitude\";\n" +
        "     :units = \"degrees_east\";\n" +
        "   float QNux10(time=1, altitude=1, lat=113, lon=121);\n" +
        // " :_CoordinateAxes = \"time altitude lat lon \";\n" +
        "     :_FillValue = -9999999.0f; // float\n" +
        "     :actual_range = -7.84414f, 23.8928f; // float\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 1; // int\n" +
        "     :ioos_category = \"Wind\";\n" +
        "     :long_name = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal\";\n" +
        "     :missing_value = -9999999.0f; // float\n" +
        "     :numberOfObservations = 6239; // int\n" +
        "     :percentCoverage = 0.45630073868207416; // double\n" +
        "     :standard_name = \"x_wind\";\n" +
        "     :units = \"m s-1\";\n" +
        "\n" +
        " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
        " :cdm_data_type = \"Grid\";\n" +
        " :cols = 121; // int\n" +
        " :composite = \"true\";\n" +
        " :contributor_name = \"NASA JPL (Cal. Inst. of Technology)\";\n" +
        " :contributor_role = \"Source of level 2 data.\";\n" +
        " :Conventions = \"CF-1.6\";\n" +
        " :creator_email = \"dave.foley@noaa.gov\";\n" +
        " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
        " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
        " :cwhdf_version = \"3.4\";\n";
    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    Test.ensureTrue(String2.indexOf(responseArray, " :id = \"TQNux10S1day_20061121120000_x-135_X-105_y22_Y50\";") > 0,
        "response=" + response);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_end = \"2006-11-22T00:00:00Z\";") > 0,
        "response=" + response);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_start = \"2006-11-21T00:00:00Z\";") > 0,
        "response=" + response);
    Test.ensureTrue(
        String2.indexOf(responseArray,
            " :title = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal\";") > 0,
        "response=" + response);

    // test of HTTP GET an .tif grid file (there is no readTif())
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.tif");
    response = String2.annotatedString(response.substring(0, 100));
    error = "response=" + response;
    reference = // reference data from public browser
        "MM[0]*[0][0]4[710]HRSQOIJJHECA>FFBAHK\\`Z`hge_[402][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][end]";
    Test.ensureEqual(response, reference, error);

    // HTTP GET an .xyz grid file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.xyz");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    Test.ensureTrue(
        response.startsWith( // reference data from public browser
            "-135\t22\tNaN\n" +
                "-134.75\t22\tNaN\n" +
                "-134.5\t22\tNaN\n" +
                "-134.25\t22\tNaN\n" +
                "-134\t22\tNaN\n" +
                "-133.75\t22\tNaN\n" +
                "-133.5\t22\t-6.04076\n" +
                "-133.25\t22\t-6.12819\n" +
                "-133\t22\t-7.09104\n" +
                "-132.75\t22\t-7.33952\n" +
                "-132.5\t22\t-7.40283\n" +
                "-132.25\t22\t-5.42233\n" +
                "-132\t22\t-5.59507"),
        error);

    // HTTP GET FGDC for a grid file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=fgdc");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response + "\nnot found: ";
    find = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n";
    Test.ensureTrue(response.startsWith(find), error + find);
    find = "<title>Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal</title>";
    Test.ensureTrue(response.indexOf(find) >= 0, error + find);
    find = "     <timeperd>\n" +
        "       <timeinfo>\n" +
        "           <rngdates>\n" +
        "             <begdate>20061121</begdate>\n" +
        "             <begtime>000000Z</begtime>\n" +
        "             <enddate>20061122</enddate>\n" +
        "             <endtime>000000Z</endtime>\n" +
        "           </rngdates>\n";
    Test.ensureTrue(response.indexOf(find) >= 0, error + find);
    find = "<attraccr>Wind measurements are accurate to within 2 m/s in speed and 20 degrees in direction.</attraccr>";
    Test.ensureTrue(response.indexOf(find) >= 0, error + find);
    find = "         <srctime>\n" +
        "           <timeinfo>\n" +
        "           <rngdates>\n" +
        "             <begdate>20061121</begdate>\n" +
        "             <begtime>000000Z</begtime>\n" +
        "             <enddate>20061122</enddate>\n" +
        "             <endtime>000000Z</endtime>\n" +
        "           </rngdates>\n" +
        "         </timeinfo>\n";
    Test.ensureTrue(response.indexOf(find) >= 0, error + find);
    find = "</metadata>";
    Test.ensureTrue(response.indexOf(find) >= 0, error + find);

    // HTTP GET an .ncHeader of a local grid file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridData&dataSet=TATssta&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-130.0&minLat=22.0&maxLat=27.0&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response; // reference data from public browser
    reference = "netcdf TATsstaS1day_20061121120000_x-135_X-130_y22_Y27_nx2147483647_ny2147483647.nc {\n" +
        " dimensions:\n" +
        "   time = 1;   // (has coord.var)\n" +
        "   altitude = 1;   // (has coord.var)\n" +
        "   lat = 401;   // (has coord.var)\n" +
        "   lon = 401;   // (has coord.var)\n" +
        " variables:\n" +
        "   double time(time=1);\n" +
        "     :actual_range = 1.1641104E9, 1.1641104E9; // double\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Centered Time\";\n" +
        "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "     :standard_name = \"time\";\n" +
        "     :axis = \"T\";\n" +
        "     :_CoordinateAxisType = \"Time\";\n" +
        "   double altitude(altitude=1);\n" +
        "     :actual_range = 0.0, 0.0; // double\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :long_name = \"Altitude\";\n" +
        "     :positive = \"up\";\n" +
        "     :standard_name = \"altitude\";\n" +
        "     :units = \"m\";\n" +
        "     :axis = \"Z\";\n" +
        "     :_CoordinateAxisType = \"Height\";\n" +
        "     :_CoordinateZisPositive = \"up\";\n" +
        "   double lat(lat=401);\n" +
        "     :_CoordinateAxisType = \"Lat\";\n" +
        "     :actual_range = 22.0, 27.0; // double\n" +
        "     :axis = \"Y\";\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 4; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Latitude\";\n" +
        "     :point_spacing = \"even\";\n" +
        "     :standard_name = \"latitude\";\n" +
        "     :units = \"degrees_north\";\n" +
        "   double lon(lon=401);\n" +
        "     :_CoordinateAxisType = \"Lon\";\n" +
        "     :actual_range = -135.0, -130.0; // double\n" +
        "     :axis = \"X\";\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 4; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Longitude\";\n" +
        "     :point_spacing = \"even\";\n" +
        "     :standard_name = \"longitude\";\n" +
        "     :units = \"degrees_east\";\n" +
        "   float ATssta(time=1, altitude=1, lat=401, lon=401);\n" +
        // " :_CoordinateAxes = \"time altitude lat lon \";\n" +
        "     :_FillValue = -9999999.0f; // float\n" +
        "     :actual_range = 16.9153f, 86.2626f; // float\n" +
        "     :coordsys = \"geographic\";\n" +
        "     :fraction_digits = 1; // int\n" +
        "     :ioos_category = \"Temperature\";\n" +
        "     :long_name = \"SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night\";\n" +
        "     :missing_value = -9999999.0f; // float\n" +
        "     :numberOfObservations = 15084; // int\n" +
        "     :percentCoverage = 0.09380538678242051; // double\n" +
        "     :standard_name = \"sea_surface_temperature\";\n" +
        "     :units = \"degree_C\";\n" +
        "\n" +
        " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
        " :cdm_data_type = \"Grid\";\n" +
        " :cols = 401; // int\n" +
        " :composite = \"true\";\n" +
        " :contributor_name = \"NOAA NWS Monterey and NOAA CoastWatch\";\n" +
        " :contributor_role = \"Source of level 2 data.\";\n" +
        " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
        " :creator_email = \"dave.foley@noaa.gov\";\n" +
        " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
        " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
        " :cwhdf_version = \"3.4\";\n";
    Test.ensureEqual(response.substring(0, reference.length()), reference, "response=" + response);

    // Test.ensureTrue(String2.indexOf(responseArray, " :date_created =
    // \"2006-12-18Z\";") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, " :date_issued =
    // \"2006-12-18Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Easternmost_Easting = -130.0; // double") > 0, error);
    Test.ensureTrue(
        String2.indexOf(responseArray, " :et_affine = 0.0, 0.0125, 0.0125, 0.0, -135.0, 22.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :gctp_datum = 12; // int") > 0, error);
    Test.ensureTrue(
        String2.indexOf(responseArray,
            " :gctp_parm = 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0; // double") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :gctp_sys = 0; // int") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :gctp_zone = 0; // int") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_max = 27.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_min = 22.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_resolution = 0.0125; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_units = \"degrees_north\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_max = -130.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_min = -135.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_resolution = 0.0125; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_units = \"degrees_east\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_max = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_min = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_positive = \"up\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_units = \"m\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :history = \"NOAA NWS Monterey and NOAA CoastWatch") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, "2006-12-18T21:07:05Z NOAA
    // NESDIS CoastWatch WCRN\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :id = \"TATsstaS1day_20061121120000_x-135_X-130_y22_Y27\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :institution = \"NOAA CoastWatch, West Coast Node\";") > 0, error);
    Test.ensureTrue(
        String2.indexOf(responseArray, " :keywords = \"Oceans > Ocean Temperature > Sea Surface Temperature\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :keywords_vocabulary = \"GCMD Science Keywords\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :naming_authority = \"gov.noaa.pfel.coastwatch\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Northernmost_Northing = 27.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :origin = \"NOAA NWS Monterey and NOAA CoastWatch\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :pass_date = 13473; // int") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :polygon_latitude = 22.0, 27.0, 27.0, 22.0, 22.0; // double") > 0,
        error);
    Test.ensureTrue(
        String2.indexOf(responseArray, " :polygon_longitude = -135.0, -135.0, -130.0, -130.0, -135.0; // double") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :processing_level = \"3\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :projection = \"geographic\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :projection_type = \"mapped\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :references = \"NOAA POES satellites information: http://coastwatch.noaa.gov/poes_sst_overview.html . Processing information: https://www.ospo.noaa.gov/PSB/EPS/CW/coastwatch.html . Processing reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b.\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :rows = 401; // int") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :satellite = \"POES\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :sensor = \"AVHRR HRPT\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :source = \"satellite observation: POES, AVHRR HRPT\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Southernmost_Northing = 22.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :standard_name_vocabulary = \"CF Standard Name Table v55\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :start_time = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :summary = \"NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data is provided at high resolution (0.0125 degrees) for the North Pacific Ocean.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites.\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_end = \"2006-11-22T00:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_start = \"2006-11-21T00:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :title = \"SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Westernmost_Easting = -135.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " data:") > 0, error);

    // *** Tests of get subset from different regions
    // climatology source file is 0..360
    // CMCchlaSmday_00010516120000_x_X_y_Y_nx_ny.grd
    try {
      responseArray = SSR.getUrlResponseLines(
          url + "?get=gridData&dataSet=CMCchla&timePeriod=monthly&centeredTime=0001-05-16T12:00:00&minLon=-130.0&maxLon=-129.0&minLat=24.0&maxLat=26.0&nLon=2&nLat=3&fileType=.asc");
      response = String2.toNewlineString(responseArray);
      error = "response=" + response;
      reference = // reference data from experimental browser(!)
          "ncols 2\n" +
              "nrows 3\n" +
              "xllcenter -130.0\n" +
              "yllcenter 24.0\n" +
              "cellsize 1.0\n" +
              "nodata_value -9999999\n" +
              "0.057499997 0.057499997\n" +
              "0.0585 0.0555\n" +
              "0.0555 0.05075\n";
      Test.ensureEqual(response, reference, error);
    } catch (Exception e) {
      String2.pressEnterToContinue(MustBe.throwableToString(e) +
          "\nKnown error in TestBrowsers when getting CMCchlaSmday climatology data." +
          "\nI just haven't dealt with it.");
    }

    // **** TIME SERIES
    // HTTP GET an .asc grid time series file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridTimeSeries&dataSet=TQNux10&timePeriod=1day&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&lon=-127.26&lat=46.65&fileType=.asc");
    response = String2.toNewlineString(responseArray);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.161432E9), "2006-10-21T12:00:00Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    Test.ensureEqual(response, // reference data from public browser
        "LON\tLAT\tDEPTH\tTIME\tID\tTQNux10\n" +
            "degrees_east\tdegrees_north\tm\tseconds since 1970-01-01T00:00:00Z\t\tm s-1\n" +
            "-127.25\t46.75\t0.0\t1.161432E9\tTQNux10\t-2.1118\n" +
            "-127.25\t46.75\t0.0\t1.1615184E9\tTQNux10\t1.4521\n" +
            "-127.25\t46.75\t0.0\t1.1616048E9\tTQNux10\t4.70468\n" +
            "-127.25\t46.75\t0.0\t1.1616912E9\tTQNux10\t8.02869\n" +
            "-127.25\t46.75\t0.0\t1.1617776E9\tTQNux10\t6.75399\n" +
            "-127.25\t46.75\t0.0\t1.161864E9\tTQNux10\t5.3414\n" +
            "-127.25\t46.75\t0.0\t1.1619504E9\tTQNux10\t3.43693\n" +
            "-127.25\t46.75\t0.0\t1.1620368E9\tTQNux10\t2.30198\n" +
            "-127.25\t46.75\t0.0\t1.1621232E9\tTQNux10\t4.53238\n" +
            "-127.25\t46.75\t0.0\t1.1622096E9\tTQNux10\t-1.38027\n" +
            "-127.25\t46.75\t0.0\t1.162296E9\tTQNux10\t-4.56252\n" +
            "-127.25\t46.75\t0.0\t1.1623824E9\tTQNux10\t-7.76371\n" +
            "-127.25\t46.75\t0.0\t1.1624688E9\tTQNux10\t-11.4541\n" +
            "-127.25\t46.75\t0.0\t1.1625552E9\tTQNux10\t5.50526\n" +
            "-127.25\t46.75\t0.0\t1.1626416E9\tTQNux10\t2.4435\n" +
            "-127.25\t46.75\t0.0\t1.162728E9\tTQNux10\t8.20177\n" +
            "-127.25\t46.75\t0.0\t1.1628144E9\tTQNux10\t13.1721\n" +
            "-127.25\t46.75\t0.0\t1.1629008E9\tTQNux10\t6.31803\n" +
            "-127.25\t46.75\t0.0\t1.1629872E9\tTQNux10\t7.72192\n" +
            "-127.25\t46.75\t0.0\t1.1630736E9\tTQNux10\t5.12044\n" +
            "-127.25\t46.75\t0.0\t1.16316E9\tTQNux10\t1.96228\n" +
            "-127.25\t46.75\t0.0\t1.1632464E9\tTQNux10\t10.3302\n" +
            "-127.25\t46.75\t0.0\t1.1633328E9\tTQNux10\t12.6308\n" +
            "-127.25\t46.75\t0.0\t1.1634192E9\tTQNux10\t12.7953\n" +
            "-127.25\t46.75\t0.0\t1.1635056E9\tTQNux10\t6.48281\n" +
            "-127.25\t46.75\t0.0\t1.163592E9\tTQNux10\t3.72782\n" +
            "-127.25\t46.75\t0.0\t1.1636784E9\tTQNux10\t10.288\n" +
            "-127.25\t46.75\t0.0\t1.1637648E9\tTQNux10\t4.53542\n" +
            "-127.25\t46.75\t0.0\t1.1638512E9\tTQNux10\t-0.50557\n" +
            "-127.25\t46.75\t0.0\t1.1639376E9\tTQNux10\t-4.59427\n" +
            "-127.25\t46.75\t0.0\t1.164024E9\tTQNux10\t9.19697\n" +
            "-127.25\t46.75\t0.0\t1.1641104E9\tTQNux10\t1.52101\n",
        String2.ERROR + " getting .asc");

    // HTTP GET an .ncHeader of a grid time series file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridTimeSeries&dataSet=TQNux10&timePeriod=1day&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&lon=-127.26&lat=46.65&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    reference = "netcdf TS_TQNux10S1dayAverages_x-127.26_y46.65_t20061021120000_T20061121120000.nc {\n" +
        " dimensions:\n" +
        "   row = 32;\n" +
        "   IDStringLength = 7;\n" +
        " variables:\n" +
        "   double LON(row=32);\n" +
        "     :_CoordinateAxisType = \"Lon\";\n" +
        "     :actual_range = -127.25, -127.25; // double\n" +
        "     :axis = \"X\";\n" +
        "     :fraction_digits = 2; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Longitude\";\n" +
        "     :standard_name = \"longitude\";\n" +
        "     :units = \"degrees_east\";\n" +
        "   double LAT(row=32);\n" +
        "     :_CoordinateAxisType = \"Lat\";\n" +
        "     :actual_range = 46.75, 46.75; // double\n" +
        "     :axis = \"Y\";\n" +
        "     :fraction_digits = 2; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Latitude\";\n" +
        "     :standard_name = \"latitude\";\n" +
        "     :units = \"degrees_north\";\n" +
        "   double DEPTH(row=32);\n" +
        "     :_CoordinateAxisType = \"Height\";\n" +
        "     :_CoordinateZisPositive = \"down\";\n" +
        "     :actual_range = 0.0, 0.0; // double\n" +
        "     :axis = \"Z\";\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Depth\";\n" +
        "     :positive = \"down\";\n" +
        "     :standard_name = \"depth\";\n" +
        "     :units = \"m\";\n" +
        "   double TIME(row=32);\n" +
        "     :_CoordinateAxisType = \"Time\";\n" +
        "     :actual_range = 1.161432E9, 1.1641104E9; // double\n" +
        "     :axis = \"T\";\n" +
        "     :fraction_digits = 0; // int\n" +
        "     :ioos_category = \"Time\";\n" +
        "     :long_name = \"Centered Time of 1 day Composites\";\n" +
        "     :standard_name = \"time\";\n" +
        "     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
        "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "   char ID(row=32, IDStringLength=7);\n" +
        "   float TQNux10(row=32);\n" +
        // " :_CoordinateAxes = \"time altitude lat lon \";\n" +
        "     :actual_range = -11.4541f, 13.1721f; // float\n" +
        "     :fraction_digits = 1; // int\n" +
        "     :ioos_category = \"Wind\";\n" +
        "     :long_name = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal\";\n" +
        "     :standard_name = \"x_wind\";\n" +
        "     :units = \"m s-1\";\n" +
        "\n" +
        " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
        " :contributor_name = \"NASA JPL (Cal. Inst. of Technology)\";\n" +
        " :contributor_role = \"Source of data.\";\n" +
        " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
        " :creator_email = \"dave.foley@noaa.gov\";\n" +
        " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
        " :creator_url = \"http://coastwatch.pfel.noaa.gov\";";
    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    Test.ensureTrue(response.indexOf(" :Easternmost_Easting = -127.25; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_max = 46.75; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_min = 46.75; // double") > 0, error);
    Test.ensureEqual(response.indexOf(" :geospatial_lat_resolution"), -1, error); // res removed for time series
    Test.ensureTrue(response.indexOf(" :geospatial_lat_units = \"degrees_north\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_max = -127.25; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_min = -127.25; // double") > 0, error);
    Test.ensureEqual(response.indexOf(" :geospatial_lon_resolution"), -1, error); // res removed for time series
    Test.ensureTrue(response.indexOf(" :geospatial_lon_units = \"degrees_east\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_max = 0.0; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_min = 0.0; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_positive = \"down\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_units = \"m\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :history = \"NASA JPL (Cal. Inst. of Technology)") > 0, error);
    Test.ensureTrue(
        response.indexOf(" :id = \"TS_TQNux10S1dayAverages_x-127.26_y46.65_t20061021120000_T20061121120000\";") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :institution = \"NOAA CoastWatch, West Coast Node\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :keywords = \"Oceans > Ocean Winds > Surface Winds\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :keywords_vocabulary = \"GCMD Science Keywords\";") > 0, error);
    Test.ensureTrue(response.indexOf(
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :naming_authority = \"gov.noaa.pfel.coastwatch\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :Northernmost_Northing = 46.75; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :origin = \"NASA JPL (Cal. Inst. of Technology)\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :projection = \"geographic\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :references = \"NASA/JPL Winds: http://winds.jpl.nasa.gov/ .\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :satellite = \"QuikSCAT\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :sensor = \"SeaWinds\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :source = \"satellite observation: QuikSCAT, SeaWinds\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :Southernmost_Northing = 46.75; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :standard_name_vocabulary = \"CF Standard Name Table v55\";") > 0, error);
    Test.ensureTrue(response.indexOf(
        " :summary = \"NASA's Jet Propulsion Laboratory (JPL) distributes near real time wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :time_coverage_end = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :time_coverage_start = \"2006-10-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(
        response.indexOf(" :title = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal\";") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :Westernmost_Easting = -127.25; // double") > 0, error);

    String2.log("  TestBrowsers.testGridScreen finished successfully.");
  }

  /**
   * This tests the ERD West Coast CoastWatch Browser's "Edit: Contour Data"
   * screen.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testContourScreen(String url) throws Exception {

    String2.log("\n*** TestBrowsers.testContourScreen  \nurl=" + url);
    String responseArray[];
    String response, error, find;

    // note that the contour HTTP GET requests are tested in testGridScreen

    String2.log("  TestBrowsers.testContourScreen finished successfully.");
  }

  /**
   * This tests the ERD West Coast CoastWatch Browser's "Edit: Vector Data"
   * screen.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testVectorScreen(String url) throws Exception {

    String2.log("\n*** TestBrowsers.testVectorScreen  \nurl=" + url);
    String responseArray[];
    String response, error, find;

    // HTTP GET an .xyz vector file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridVectorData&dataSet=VTQNu10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.xyz");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    Test.ensureTrue(
        response.startsWith( // reference data from public browser
            "-135\t22\tNaN\tNaN\n" +
                "-134.75\t22\tNaN\tNaN\n" +
                "-134.5\t22\tNaN\tNaN\n" +
                "-134.25\t22\tNaN\tNaN\n" +
                "-134\t22\tNaN\tNaN\n" +
                "-133.75\t22\tNaN\tNaN\n" +
                "-133.5\t22\t-6.04076\t-2.60942\n" +
                "-133.25\t22\t-6.12819\t-2.67772\n" +
                "-133\t22\t-7.09104\t-3.15399\n" +
                "-132.75\t22\t-7.33952\t-3.07656"),
        error);

    // HTTP GET an .xyz vector file same as above but with equivalent "endDate"
    // instead of "centeredTime"!
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridVectorData&dataSet=VTQNu10&timePeriod=1day&endDate=2006-11-21&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.xyz");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    Test.ensureTrue(
        response.startsWith( // reference data from public browser
            "-135\t22\tNaN\tNaN\n" +
                "-134.75\t22\tNaN\tNaN\n" +
                "-134.5\t22\tNaN\tNaN\n" +
                "-134.25\t22\tNaN\tNaN\n" +
                "-134\t22\tNaN\tNaN\n" +
                "-133.75\t22\tNaN\tNaN\n" +
                "-133.5\t22\t-6.04076\t-2.60942\n" +
                "-133.25\t22\t-6.12819\t-2.67772\n" +
                "-133\t22\t-7.09104\t-3.15399\n" +
                "-132.75\t22\t-7.33952\t-3.07656"),
        error);

    // HTTP GET an .ncHeader of a vector file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=gridVectorData&dataSet=VTQNu10&timePeriod=1day&centeredTime=2006-11-21T12:00:00&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    String reference = // reference data from public browser
        "netcdf VTQNu10S1day_20061121120000_x-135_X-105_y22_Y50_nx121_ny113.nc {\n" +
            " dimensions:\n" +
            "   time = 1;   // (has coord.var)\n" +
            "   altitude = 1;   // (has coord.var)\n" +
            "   lat = 113;   // (has coord.var)\n" +
            "   lon = 121;   // (has coord.var)\n" +
            " variables:\n" +
            "   double time(time=1);\n" +
            "     :actual_range = 1.1641104E9, 1.1641104E9; // double\n" +
            "     :axis = \"T\";\n" +
            "     :fraction_digits = 0; // int\n" +
            "     :long_name = \"Centered Time\";\n" +
            "     :standard_name = \"time\";\n" +
            "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "     :_CoordinateAxisType = \"Time\";\n" +
            "   double altitude(altitude=1);\n" +
            "     :actual_range = 0.0, 0.0; // double\n" +
            "     :axis = \"Z\";\n" +
            "     :fraction_digits = 0; // int\n" +
            "     :long_name = \"Altitude\";\n" +
            "     :positive = \"up\";\n" +
            "     :standard_name = \"altitude\";\n" +
            "     :units = \"m\";\n" +
            "     :_CoordinateAxisType = \"Height\";\n" +
            "     :_CoordinateZisPositive = \"up\";\n" +
            "   double lat(lat=113);\n" +
            "     :_CoordinateAxisType = \"Lat\";\n" +
            "     :actual_range = 22.0, 50.0; // double\n" +
            "     :axis = \"Y\";\n" +
            "     :coordsys = \"geographic\";\n" +
            "     :fraction_digits = 2; // int\n" +
            "     :ioos_category = \"Location\";\n" +
            "     :long_name = \"Latitude\";\n" +
            "     :point_spacing = \"even\";\n" +
            "     :standard_name = \"latitude\";\n" +
            "     :units = \"degrees_north\";\n" +
            "   double lon(lon=121);\n" +
            "     :_CoordinateAxisType = \"Lon\";\n" +
            "     :actual_range = -135.0, -105.0; // double\n" +
            "     :axis = \"X\";\n" +
            "     :coordsys = \"geographic\";\n" +
            "     :fraction_digits = 2; // int\n" +
            "     :ioos_category = \"Location\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :point_spacing = \"even\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n" +
            "   float QNux10(time=1, altitude=1, lat=113, lon=121);\n" +
            // " :_CoordinateAxes = \"time altitude lat lon \";\n" +
            "     :_FillValue = -9999999.0f; // float\n" +
            "     :actual_range = -7.84414f, 23.8928f; // float\n" +
            "     :coordsys = \"geographic\";\n" +
            "     :fraction_digits = 1; // int\n" +
            "     :ioos_category = \"Wind\";\n" +
            "     :long_name = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal\";\n" +
            "     :missing_value = -9999999.0f; // float\n" +
            "     :numberOfObservations = 6239; // int\n" +
            "     :percentCoverage = 0.45630073868207416; // double\n" +
            "     :standard_name = \"x_wind\";\n" +
            "     :units = \"m s-1\";\n" +
            "   float QNuy10(time=1, altitude=1, lat=113, lon=121);\n" +
            // " :_CoordinateAxes = \"time altitude lat lon \";\n" +
            "     :_FillValue = -9999999.0f; // float\n" +
            "     :actual_range = -7.0801f, 14.7966f; // float\n" +
            "     :coordsys = \"geographic\";\n" +
            "     :fraction_digits = 1; // int\n" +
            "     :ioos_category = \"Wind\";\n" +
            "     :long_name = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Meridional\";\n" +
            "     :missing_value = -9999999.0f; // float\n" +
            "     :numberOfObservations = 6239; // int\n" +
            "     :percentCoverage = 0.45630073868207416; // double\n" +
            "     :standard_name = \"y_wind\";\n" +
            "     :units = \"m s-1\";\n" +
            "\n" +
            " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
            " :cdm_data_type = \"Grid\";\n" +
            " :cols = 121; // int\n" +
            " :composite = \"true\";\n" +
            " :contributor_name = \"NASA JPL (Cal. Inst. of Technology)\";\n" +
            " :contributor_role = \"Source of level 2 data.\";\n" +
            " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            " :creator_email = \"dave.foley@noaa.gov\";\n" +
            " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
            " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
            " :cwhdf_version = \"3.4\";\n";

    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    Test.ensureTrue(response.indexOf(" :Easternmost_Easting = -105.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :et_affine = 0.0, 0.25, 0.25, 0.0, -135.0, 22.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :gctp_datum = 12; // int\n") > 0, error);
    Test.ensureTrue(
        response.indexOf(
            " :gctp_parm = 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0; // double\n") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :gctp_sys = 0; // int\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :gctp_zone = 0; // int\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_max = 50.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_min = 22.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_resolution = 0.25; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_units = \"degrees_north\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_max = -105.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_min = -135.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_resolution = 0.25; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_units = \"degrees_east\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_max = 0.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_min = 0.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_positive = \"up\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_units = \"m\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :history = \"NASA JPL (Cal. Inst. of Technology)\n") > 0, error);
    // "2006-11-21T09:13:45Z NOAA NESDIS CoastWatch WCRN\n") > 0, error);
    // "2006-11-27T19:46:16Z NOAA NESDIS CoastWatch WCRN\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :id = \"TQNux10S1day_20061121120000_x-135_X-105_y22_Y50\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :institution = \"NOAA CoastWatch, West Coast Node\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :keywords = \"Oceans > Ocean Winds > Surface Winds\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :keywords_vocabulary = \"GCMD Science Keywords\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :naming_authority = \"gov.noaa.pfel.coastwatch\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :Northernmost_Northing = 50.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :origin = \"NASA JPL (Cal. Inst. of Technology)\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :pass_date = 13473; // int\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :polygon_latitude = 22.0, 50.0, 50.0, 22.0, 22.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :polygon_longitude = -135.0, -135.0, -105.0, -105.0, -135.0; // double\n") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :processing_level = \"3\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :projection = \"geographic\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :projection_type = \"mapped\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :references = \"NASA/JPL Winds: http://winds.jpl.nasa.gov/ .\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :rows = 113; // int\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :satellite = \"QuikSCAT\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :sensor = \"SeaWinds\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :source = \"satellite observation: QuikSCAT, SeaWinds\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :Southernmost_Northing = 22.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :standard_name_vocabulary = \"CF Standard Name Table v55\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :start_time = 0.0; // double\n") > 0, error);
    Test.ensureTrue(response.indexOf(
        " :summary = \"NASA's Jet Propulsion Laboratory (JPL) distributes near real time wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";\n") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :time_coverage_end = \"2006-11-22T00:00:00Z\";\n") > 0, error);
    Test.ensureTrue(response.indexOf(" :time_coverage_start = \"2006-11-21T00:00:00Z\";\n") > 0, error);
    Test.ensureTrue(
        response.indexOf(" :title = \"Wind, QuikSCAT SeaWinds, 0.25 degrees, Global, Near Real Time, Zonal\";\n") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :Westernmost_Easting = -135.0; // double\n") > 0, error);

    // there is no FGDC for a vector file

    // currently, there is no time series for vector data

    String2.log("  TestBrowsers.testVectorScreen finished successfully.");
  }

  /**
   * This tests the ERD West Coast CoastWatch Browser's "Edit: Station Vector"
   * screen.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testStationVectorScreen(String url) throws Exception {

    String2.log("\n*** TestBrowsers.testStationVectorScreen  \nurl=" + url);
    String responseArray[];
    String response, reference, error, find;

    // HTTP GET an .asc station vector file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0&beginTime=2006-11-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.asc");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    reference = "LON\tLAT\tDEPTH\tTIME\tID\tWSPU\tWSPV\n" +
        "degrees_east\tdegrees_north\tm\tseconds since 1970-01-01T00:00:00Z\tunitless\tm s-1\tm s-1\n" +
        "-130.36\t42.58\t0.0\t1.1641104E9\tNDBC 46002 met\t11.708333\t4.070833\n" +
        "-131.02\t46.05\t0.0\t1.1641104E9\tNDBC 46005 met\t8.054167\t2.1333334\n" +
        "-124.2\t46.2\t0.0\t1.1641104E9\tNDBC 46010 met\tNaN\tNaN\n" +
        "-120.87\t34.88\t0.0\t1.1641104E9\tNDBC 46011 met\t1.1708333\t-2.325\n" +
        "-122.88\t37.36\t0.0\t1.1641104E9\tNDBC 46012 met\t1.5041667\t-1.7125\n" +
        "-123.32\t38.23\t0.0\t1.1641104E9\tNDBC 46013 met\t1.2416667\t0.5125\n";
    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    // HTTP GET an .ncHeader of a station vector file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0&beginTime=2006-11-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    error = "response=" + response;
    reference = // reference data from public browser
        "netcdf PNBwspuS1dayAverages_x-135_X-105_y22_Y50_z0_Z0_t20061121120000_T20061121120000.nc {\n" +
            " dimensions:\n" +
            "   row = 124;\n" +
            "   IDStringLength = 14;\n" +
            " variables:\n" +
            "   double LON(row=124);\n" +
            "     :_CoordinateAxisType = \"Lon\";\n" +
            "     :actual_range = -133.94, -116.5; // double\n" +
            "     :axis = \"X\";\n" +
            "     :comment = \"The longitude of the station.\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n" +
            "   double LAT(row=124);\n" +
            "     :_CoordinateAxisType = \"Lat\";\n" +
            "     :actual_range = 32.425, 48.86; // double\n" +
            "     :axis = \"Y\";\n" +
            "     :comment = \"The latitude of the station.\";\n" +
            "     :long_name = \"Latitude\";\n" +
            "     :standard_name = \"latitude\";\n" +
            "     :units = \"degrees_north\";\n" +
            "   double DEPTH(row=124);\n" +
            "     :_CoordinateAxisType = \"Height\";\n" +
            "     :_CoordinateZisPositive = \"down\";\n" +
            "     :actual_range = 0.0, 0.0; // double\n" +
            "     :axis = \"Z\";\n" +
            "     :comment = \"The depth of the station, nominally 0 (see station information for details).\";\n" +
            "     :long_name = \"Depth\";\n" +
            "     :positive = \"down\";\n" +
            "     :standard_name = \"depth\";\n" +
            "     :units = \"m\";\n" +
            "   double TIME(row=124);\n" +
            "     :_CoordinateAxisType = \"Time\";\n" +
            "     :actual_range = 1.1641104E9, 1.1641104E9; // double\n" +
            "     :axis = \"T\";\n" +
            "     :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n"
            +
            "     :long_name = \"Centered Time of 1 day Averages\";\n" +
            "     :standard_name = \"time\";\n" +
            "     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
            "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "   char ID(row=124, IDStringLength=14);\n" +
            "     :long_name = \"Station Identifier\";\n" +
            "     :units = \"unitless\";\n" +
            "   float WSPU(row=124);\n" +
            "     :_FillValue = -9999999.0f; // float\n" +
            "     :actual_range = -5.6458335f, 11.708333f; // float\n" +
            "     :comment = \"The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
            +
            "     :long_name = \"Wind Speed, Zonal\";\n" +
            "     :missing_value = -9999999.0f; // float\n" +
            "     :standard_name = \"eastward_wind\";\n" +
            "     :units = \"m s-1\";\n" +
            "   float WSPV(row=124);\n" +
            "     :_FillValue = -9999999.0f; // float\n" +
            "     :actual_range = -4.9625f, 11.754167f; // float\n" +
            "     :comment = \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
            +
            "     :long_name = \"Wind Speed, Meridional\";\n" +
            "     :missing_value = -9999999.0f; // float\n" +
            "     :standard_name = \"northward_wind\";\n" +
            "     :units = \"m s-1\";\n" +
            "\n" +
            " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
            " :cdm_data_type = \"Station\";\n" +
            " :contributor_name = \"NOAA NDBC and Other Station Owners/Operators\";\n" +
            " :contributor_role = \"Source of data.\";\n" +
            " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            " :creator_email = \"dave.foley@noaa.gov\";\n" +
            " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
            " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n";

    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    // " :date_created = \"2006-11-27T23:49:43Z\";\n" +
    // " :date_issued = \"2006-11-27T23:49:43Z\";\n" +
    Test.ensureTrue(String2.indexOf(responseArray, " :Easternmost_Easting = -116.5; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_max = 48.86; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_min = 32.425; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_units = \"degrees_north\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_max = -116.5; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_min = -133.94; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_units = \"degrees_east\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_max = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_min = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_positive = \"down\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_units = \"m\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :history = \"NOAA NDBC") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, "2006-11-27T23:49:43Z NOAA
    // NESDIS CoastWatch WCRN\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :id = \"NDBC_31201_met\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :institution = \"NOAA CoastWatch, West Coast Node\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :keywords = \"Oceans\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :naming_authority = \"gov.noaa.pfel.coastwatch\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :NDBCMeasurementDescriptionUrl = \"https://www.ndbc.noaa.gov/measdes.shtml\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Northernmost_Northing = 48.86; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :quality = \"Automated QC checks with periodic manual QC\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :source = \"station observation\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Southernmost_Northing = 32.425; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :standard_name_vocabulary = \"CF Standard Name Table v55\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_end = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_start = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :title = \"Wind Speed, Zonal (NDBC)\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Westernmost_Easting = -133.94; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " data:") > 0, error);

    Test.ensureEqual(String2.indexOf(responseArray, " :time_coverage_resolution"), -1, error);
    // after next release, ensure not set:
    // Test.ensureEqual(String2.indexOf(responseArray, " :publisher_email"), -1,
    // error);
    // Test.ensureEqual(String2.indexOf(responseArray, " :publisher_name"), -1,
    // error);
    // Test.ensureEqual(String2.indexOf(responseArray, " :publisher_url"), -1,
    // error);

    // no FGDC for a station vector file

    // **** TIME SERIES
    // HTTP GET an .asc time series station vector file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day&minLon=-130.36&maxLon=-130.36&minLat=42.58&maxLat=42.58&minDepth=0&maxDepth=0&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.asc");
    response = String2.toNewlineString(responseArray);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.161432E9), "2006-10-21T12:00:00Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    Test.ensureEqual(response, // reference data from public browser
        "LON\tLAT\tDEPTH\tTIME\tID\tWSPU\tWSPV\n" +
            "degrees_east\tdegrees_north\tm\tseconds since 1970-01-01T00:00:00Z\tunitless\tm s-1\tm s-1\n" +
            "-130.36\t42.58\t0.0\t1.161432E9\tNDBC 46002 met\t-3.2958333\t-6.758333\n" +
            "-130.36\t42.58\t0.0\t1.1615184E9\tNDBC 46002 met\t2.2666667\t-9.608334\n" +
            "-130.36\t42.58\t0.0\t1.1616048E9\tNDBC 46002 met\t-0.07916666\t-9.95\n" +
            "-130.36\t42.58\t0.0\t1.1616912E9\tNDBC 46002 met\t3.1708333\t-8.4625\n" +
            "-130.36\t42.58\t0.0\t1.1617776E9\tNDBC 46002 met\t1.8625\t-4.9333334\n" +
            "-130.36\t42.58\t0.0\t1.161864E9\tNDBC 46002 met\t1.3833333\t0.12083333\n" +
            "-130.36\t42.58\t0.0\t1.1619504E9\tNDBC 46002 met\t1.425\t-0.6\n" +
            "-130.36\t42.58\t0.0\t1.1620368E9\tNDBC 46002 met\t0.21666667\t-2.7166667\n" +
            "-130.36\t42.58\t0.0\t1.1621232E9\tNDBC 46002 met\t1.4\t-5.65\n" +
            "-130.36\t42.58\t0.0\t1.1622096E9\tNDBC 46002 met\t-3.5958333\t-6.054167\n" +
            "-130.36\t42.58\t0.0\t1.162296E9\tNDBC 46002 met\t-7.554167\t-2.2958333\n" +
            "-130.36\t42.58\t0.0\t1.1623824E9\tNDBC 46002 met\t-5.175\t6.825\n" +
            "-130.36\t42.58\t0.0\t1.1624688E9\tNDBC 46002 met\t2.0291667\t8.079166\n" +
            "-130.36\t42.58\t0.0\t1.1625552E9\tNDBC 46002 met\t6.045833\t7.4625\n" +
            "-130.36\t42.58\t0.0\t1.1626416E9\tNDBC 46002 met\t4.8625\t8.2875\n" +
            "-130.36\t42.58\t0.0\t1.162728E9\tNDBC 46002 met\t4.4375\t6.458333\n" +
            "-130.36\t42.58\t0.0\t1.1628144E9\tNDBC 46002 met\t6.570833\t10.666667\n" +
            "-130.36\t42.58\t0.0\t1.1629008E9\tNDBC 46002 met\t6.6291666\t2.9125\n" +
            "-130.36\t42.58\t0.0\t1.1629872E9\tNDBC 46002 met\t8.613044\t-1.1217391\n" +
            "-130.36\t42.58\t0.0\t1.1630736E9\tNDBC 46002 met\t5.7958336\t-0.5916667\n" +
            "-130.36\t42.58\t0.0\t1.16316E9\tNDBC 46002 met\t8.066667\t6.2083335\n" +
            "-130.36\t42.58\t0.0\t1.1632464E9\tNDBC 46002 met\t8.2\t-3.95\n" +
            "-130.36\t42.58\t0.0\t1.1633328E9\tNDBC 46002 met\t6.5833335\t8.620833\n" +
            "-130.36\t42.58\t0.0\t1.1634192E9\tNDBC 46002 met\t8.395833\t-2.0041666\n" +
            "-130.36\t42.58\t0.0\t1.1635056E9\tNDBC 46002 met\t4.4125\t-0.59999996\n" +
            "-130.36\t42.58\t0.0\t1.163592E9\tNDBC 46002 met\t5.8916664\t12.054167\n" +
            "-130.36\t42.58\t0.0\t1.1636784E9\tNDBC 46002 met\t6.0375\t-0.23333333\n" +
            "-130.36\t42.58\t0.0\t1.1637648E9\tNDBC 46002 met\t3.95\t3.775\n" +
            "-130.36\t42.58\t0.0\t1.1638512E9\tNDBC 46002 met\t-0.4826087\t5.7434783\n" +
            "-130.36\t42.58\t0.0\t1.1639376E9\tNDBC 46002 met\t5.4083333\t8.316667\n" +
            "-130.36\t42.58\t0.0\t1.164024E9\tNDBC 46002 met\t8.395833\t5.0958333\n" +
            "-130.36\t42.58\t0.0\t1.1641104E9\tNDBC 46002 met\t11.708333\t4.070833\n",
        String2.ERROR + " getting .asc");

    // HTTP GET an .ncHeader of a time series station vector file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day&minLon=-130.36&maxLon=-130.36&minLat=42.58&maxLat=42.58&minDepth=0&maxDepth=0&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.161432E9), "2006-10-21T12:00:00Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    error = "response=" + response;
    reference = // reference data from public browser
        "netcdf PNBwspuS1dayAverages_x-130.36_X-130.36_y42.58_Y42.58_z0_Z0_t20061021120000_T20061121120000.nc {\n" +
            " dimensions:\n" +
            "   row = 32;\n" +
            "   IDStringLength = 14;\n" +
            " variables:\n" +
            "   double LON(row=32);\n" +
            "     :_CoordinateAxisType = \"Lon\";\n" +
            "     :actual_range = -130.36, -130.36; // double\n" +
            "     :axis = \"X\";\n" +
            "     :comment = \"The longitude of the station.\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n" +
            "   double LAT(row=32);\n" +
            "     :_CoordinateAxisType = \"Lat\";\n" +
            "     :actual_range = 42.58, 42.58; // double\n" +
            "     :axis = \"Y\";\n" +
            "     :comment = \"The latitude of the station.\";\n" +
            "     :long_name = \"Latitude\";\n" +
            "     :standard_name = \"latitude\";\n" +
            "     :units = \"degrees_north\";\n" +
            "   double DEPTH(row=32);\n" +
            "     :_CoordinateAxisType = \"Height\";\n" +
            "     :_CoordinateZisPositive = \"down\";\n" +
            "     :actual_range = 0.0, 0.0; // double\n" +
            "     :axis = \"Z\";\n" +
            "     :comment = \"The depth of the station, nominally 0 (see station information for details).\";\n" +
            "     :long_name = \"Depth\";\n" +
            "     :positive = \"down\";\n" +
            "     :standard_name = \"depth\";\n" +
            "     :units = \"m\";\n" +
            "   double TIME(row=32);\n" +
            "     :_CoordinateAxisType = \"Time\";\n" +
            "     :actual_range = 1.161432E9, 1.1641104E9; // double\n" +
            "     :axis = \"T\";\n" +
            "     :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n"
            +
            "     :long_name = \"Centered Time of 1 day Averages\";\n" +
            "     :standard_name = \"time\";\n" +
            "     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
            "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "   char ID(row=32, IDStringLength=14);\n" +
            "     :long_name = \"Station Identifier\";\n" +
            "     :units = \"unitless\";\n" +
            "   float WSPU(row=32);\n" +
            "     :actual_range = -7.554167f, 11.708333f; // float\n" +
            "     :comment = \"The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
            +
            "     :long_name = \"Wind Speed, Zonal\";\n" +
            "     :standard_name = \"eastward_wind\";\n" +
            "     :units = \"m s-1\";\n" +
            "   float WSPV(row=32);\n" +
            "     :actual_range = -9.95f, 12.054167f; // float\n" +
            "     :comment = \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
            +
            "     :long_name = \"Wind Speed, Meridional\";\n" +
            "     :standard_name = \"northward_wind\";\n" +
            "     :units = \"m s-1\";\n" +
            "\n" +
            " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
            " :cdm_data_type = \"Station\";\n" +
            " :contributor_name = \"NOAA NDBC and Other Station Owners/Operators\";\n" +
            " :contributor_role = \"Source of data.\";\n" +
            " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            " :creator_email = \"dave.foley@noaa.gov\";\n" +
            " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
            " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n";
    // " :date_created = \"2006-11-27T23:53:26Z\";\n" +
    // " :date_issued = \"2006-11-27T23:53:26Z\";\n" +

    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    Test.ensureTrue(response.indexOf(" :Easternmost_Easting = -130.36; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_max = 42.58; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_min = 42.58; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lat_units = \"degrees_north\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_max = -130.36; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_min = -130.36; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_lon_units = \"degrees_east\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_max = 0.0; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_min = 0.0; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_positive = \"down\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :geospatial_vertical_units = \"m\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :history = \"NOAA NDBC") > 0, error);
    // Test.ensureTrue(response.indexOf("2006-11-27T23:53:26Z NOAA NESDIS CoastWatch
    // WCRN\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :id = \"NDBC_31201_met\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :institution = \"NOAA CoastWatch, West Coast Node\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :keywords = \"Oceans\";") > 0, error);
    Test.ensureTrue(response.indexOf(
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";") > 0,
        error);
    Test.ensureTrue(response.indexOf(" :naming_authority = \"gov.noaa.pfel.coastwatch\";") > 0, error);
    Test.ensureTrue(
        response.indexOf(" :NDBCMeasurementDescriptionUrl = \"https://www.ndbc.noaa.gov/measdes.shtml\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :Northernmost_Northing = 42.58; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :quality = \"Automated QC checks with periodic manual QC\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :source = \"station observation\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :Southernmost_Northing = 42.58; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" :standard_name_vocabulary = \"CF Standard Name Table v55\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :time_coverage_end = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :time_coverage_start = \"2006-10-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :title = \"Wind Speed, Zonal (NDBC)\";") > 0, error);
    Test.ensureTrue(response.indexOf(" :Westernmost_Easting = -130.36; // double") > 0, error);
    Test.ensureTrue(response.indexOf(" data:") > 0, error);

    Test.ensureEqual(response.indexOf(" :time_coverage_resolution"), -1, error);

    String2.log("  TestBrowsers.testStationVectorScreen finished successfully.");
  }

  /**
   * This tests the ERD West Coast CoastWatch Browser's "Edit: Station" screen.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testStationScreen(String url) throws Exception {

    String2.log("\n*** start TestBrowsers.testStationScreen \nurl=" + url);
    String responseArray[];
    String response, error, find;

    // HTTP GET an .asc station file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0&beginTime=2006-11-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.asc");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    Test.ensureTrue(
        response.startsWith( // reference data from public browser
            "LON\tLAT\tDEPTH\tTIME\tID\tWTMP\n" +
                "degrees_east\tdegrees_north\tm\tseconds since 1970-01-01T00:00:00Z\tunitless\tdegree_C\n" +
                "-130.36\t42.58\t0.0\t1.1641104E9\tNDBC 46002 met\t14.391666\n" +
                "-131.02\t46.05\t0.0\t1.1641104E9\tNDBC 46005 met\t13.541667\n" +
                "-124.2\t46.2\t0.0\t1.1641104E9\tNDBC 46010 met\tNaN\n" +
                "-120.87\t34.88\t0.0\t1.1641104E9\tNDBC 46011 met\t13.766666\n" +
                "-122.88\t37.36\t0.0\t1.1641104E9\tNDBC 46012 met\t14.65\n" +
                "-123.32\t38.23\t0.0\t1.1641104E9\tNDBC 46013 met\t14.05\n"),
        error);

    // HTTP GET an .ncHeader of a station file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0&beginTime=2006-11-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    error = "response=" + response;
    String reference = // reference data from public browser
        "netcdf PNBwtmpS1dayAverages_x-135_X-105_y22_Y50_z0_Z0_t20061121120000_T20061121120000.nc {\n" +
            " dimensions:\n" +
            "   row = 124;\n" + // was "station" (and below)
            "   IDStringLength = 14;\n" +
            " variables:\n" +
            "   double LON(row=124);\n" +
            "     :_CoordinateAxisType = \"Lon\";\n" +
            "     :actual_range = -133.94, -116.5; // double\n" +
            "     :axis = \"X\";\n" +
            "     :comment = \"The longitude of the station.\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n" +
            "   double LAT(row=124);\n" +
            "     :_CoordinateAxisType = \"Lat\";\n" +
            "     :actual_range = 32.425, 48.86; // double\n" +
            "     :axis = \"Y\";\n" +
            "     :comment = \"The latitude of the station.\";\n" +
            "     :long_name = \"Latitude\";\n" +
            "     :standard_name = \"latitude\";\n" +
            "     :units = \"degrees_north\";\n" +
            "   double DEPTH(row=124);\n" +
            "     :_CoordinateAxisType = \"Height\";\n" +
            "     :_CoordinateZisPositive = \"down\";\n" +
            "     :actual_range = 0.0, 0.0; // double\n" +
            "     :axis = \"Z\";\n" +
            "     :comment = \"The depth of the station, nominally 0 (see station information for details).\";\n" +
            "     :long_name = \"Depth\";\n" +
            "     :positive = \"down\";\n" +
            "     :standard_name = \"depth\";\n" +
            "     :units = \"m\";\n" +
            "   double TIME(row=124);\n" +
            "     :_CoordinateAxisType = \"Time\";\n" +
            "     :actual_range = 1.1641104E9, 1.1641104E9; // double\n" +
            "     :axis = \"T\";\n" +
            "     :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n"
            +
            "     :long_name = \"Centered Time of 1 day Averages\";\n" +
            "     :standard_name = \"time\";\n" +
            "     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
            "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "   char ID(row=124, IDStringLength=14);\n" +
            "     :long_name = \"Station Identifier\";\n" +
            "     :units = \"unitless\";\n" +
            "   float WTMP(row=124);\n" +
            "     :_FillValue = -9999999.0f; // float\n" +
            "     :actual_range = 7.741667f, 19.170834f; // float\n" +
            "     :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
            "     :long_name = \"SST\";\n" +
            "     :missing_value = -9999999.0f; // float\n" +
            "     :standard_name = \"sea_surface_temperature\";\n" +
            "     :units = \"degree_C\";\n" +
            "\n" +
            " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
            " :cdm_data_type = \"Station\";\n" +
            " :contributor_name = \"NOAA NDBC and Other Station Owners/Operators\";\n" +
            " :contributor_role = \"Source of data.\";\n" +
            " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            " :creator_email = \"dave.foley@noaa.gov\";\n" +
            " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
            " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n";

    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    // Test.ensureTrue(String2.indexOf(responseArray, " :date_created =
    // \"2006-11-28T00:09:01Z\";") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, " :date_issued =
    // \"2006-11-28T00:09:01Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Easternmost_Easting = -116.5; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_max = 48.86; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_min = 32.425; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_units = \"degrees_north\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_max = -116.5; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_min = -133.94; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_units = \"degrees_east\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_max = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_min = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_positive = \"down\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_units = \"m\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :history = \"NOAA NDBC") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, "2006-11-28T00:09:01Z NOAA
    // NESDIS CoastWatch WCRN\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :id = \"NDBC_31201_met\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :institution = \"NOAA CoastWatch, West Coast Node\";") > 0, error);
    // lame...
    Test.ensureTrue(String2.indexOf(responseArray, " :keywords = \"Oceans\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :naming_authority = \"gov.noaa.pfel.coastwatch\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :NDBCMeasurementDescriptionUrl = \"https://www.ndbc.noaa.gov/measdes.shtml\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Northernmost_Northing = 48.86; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :quality = \"Automated QC checks with periodic manual QC\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :source = \"station observation\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Southernmost_Northing = 32.425; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :standard_name_vocabulary = \"CF Standard Name Table v55\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_end = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_start = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :title = \"SST (NDBC)\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Westernmost_Easting = -133.94; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " data:") > 0, error);

    Test.ensureEqual(String2.indexOf(responseArray, " :time_coverage_resolution"), -1, error);
    // after next release, ensure not set:
    // Test.ensureEqual(String2.indexOf(responseArray, " :publisher_email"), -1,
    // error);
    // Test.ensureEqual(String2.indexOf(responseArray, " :publisher_name"), -1,
    // error);
    // Test.ensureEqual(String2.indexOf(responseArray, " :publisher_url"), -1,
    // error);

    // no FGDC for a station file

    // **** TIME SERIES
    // HTTP GET an .asc time series station file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-130.36&maxLon=-130.36&minLat=42.58&maxLat=42.58&minDepth=0&maxDepth=0&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.asc");
    // url +
    // "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-130.36101&maxLon=-130.359&minLat=42.579002&maxLat=42.581&minDepth=0&maxDepth=0&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.asc");
    response = String2.toNewlineString(responseArray);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.161432E9), "2006-10-21T12:00:00Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    Test.ensureEqual(response, // reference data from public browser but revised to be just float data
        "LON\tLAT\tDEPTH\tTIME\tID\tWTMP\n" +
            "degrees_east\tdegrees_north\tm\tseconds since 1970-01-01T00:00:00Z\tunitless\tdegree_C\n" +
            "-130.36\t42.58\t0.0\t1.161432E9\tNDBC 46002 met\t16.2375\n" +
            "-130.36\t42.58\t0.0\t1.1615184E9\tNDBC 46002 met\t15.9375\n" +
            "-130.36\t42.58\t0.0\t1.1616048E9\tNDBC 46002 met\t15.654167\n" +
            "-130.36\t42.58\t0.0\t1.1616912E9\tNDBC 46002 met\t15.583334\n" +
            "-130.36\t42.58\t0.0\t1.1617776E9\tNDBC 46002 met\t15.6875\n" +
            "-130.36\t42.58\t0.0\t1.161864E9\tNDBC 46002 met\t15.620833\n" +
            "-130.36\t42.58\t0.0\t1.1619504E9\tNDBC 46002 met\t15.8125\n" +
            "-130.36\t42.58\t0.0\t1.1620368E9\tNDBC 46002 met\t15.845834\n" +
            "-130.36\t42.58\t0.0\t1.1621232E9\tNDBC 46002 met\t15.6875\n" +
            "-130.36\t42.58\t0.0\t1.1622096E9\tNDBC 46002 met\t15.9\n" +
            "-130.36\t42.58\t0.0\t1.162296E9\tNDBC 46002 met\t15.758333\n" +
            "-130.36\t42.58\t0.0\t1.1623824E9\tNDBC 46002 met\t15.695833\n" +
            "-130.36\t42.58\t0.0\t1.1624688E9\tNDBC 46002 met\t15.804167\n" +
            "-130.36\t42.58\t0.0\t1.1625552E9\tNDBC 46002 met\t15.645833\n" +
            "-130.36\t42.58\t0.0\t1.1626416E9\tNDBC 46002 met\t15.587501\n" +
            "-130.36\t42.58\t0.0\t1.162728E9\tNDBC 46002 met\t15.604167\n" +
            "-130.36\t42.58\t0.0\t1.1628144E9\tNDBC 46002 met\t15.700001\n" +
            "-130.36\t42.58\t0.0\t1.1629008E9\tNDBC 46002 met\t15.816667\n" +
            "-130.36\t42.58\t0.0\t1.1629872E9\tNDBC 46002 met\t15.986957\n" +
            "-130.36\t42.58\t0.0\t1.1630736E9\tNDBC 46002 met\t16.016666\n" +
            "-130.36\t42.58\t0.0\t1.16316E9\tNDBC 46002 met\t15.829166\n" +
            "-130.36\t42.58\t0.0\t1.1632464E9\tNDBC 46002 met\t15.7125\n" +
            "-130.36\t42.58\t0.0\t1.1633328E9\tNDBC 46002 met\t15.65\n" +
            "-130.36\t42.58\t0.0\t1.1634192E9\tNDBC 46002 met\t15.441667\n" +
            "-130.36\t42.58\t0.0\t1.1635056E9\tNDBC 46002 met\t15.270833\n" +
            "-130.36\t42.58\t0.0\t1.163592E9\tNDBC 46002 met\t15.3\n" +
            "-130.36\t42.58\t0.0\t1.1636784E9\tNDBC 46002 met\t15.1375\n" +
            "-130.36\t42.58\t0.0\t1.1637648E9\tNDBC 46002 met\t15.05\n" +
            "-130.36\t42.58\t0.0\t1.1638512E9\tNDBC 46002 met\t14.908695\n" +
            "-130.36\t42.58\t0.0\t1.1639376E9\tNDBC 46002 met\t14.779167\n" +
            "-130.36\t42.58\t0.0\t1.164024E9\tNDBC 46002 met\t14.5875\n" +
            "-130.36\t42.58\t0.0\t1.1641104E9\tNDBC 46002 met\t14.391666\n",
        String2.ERROR + " getting .asc");

    // HTTP GET an .ncHeader of a time series station file
    responseArray = SSR.getUrlResponseLines(
        url + "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-130.36&maxLon=-130.36&minLat=42.58&maxLat=42.58&minDepth=0&maxDepth=0&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.ncHeader");
    // url +
    // "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-130.36101&maxLon=-130.359&minLat=42.579002&maxLat=42.581&minDepth=0&maxDepth=0&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=.ncHeader");
    response = String2.toNewlineString(responseArray);
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.161432E9), "2006-10-21T12:00:00Z", "");
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1641104E9), "2006-11-21T12:00:00Z", "");
    error = "response=" + response;
    reference = // reference data from public browser
        "netcdf PNBwtmpS1dayAverages_x-130.36_X-130.36_y42.58_Y42.58_z0_Z0_t20061021120000_T20061121120000.nc {\n" +
        // "netcdf
        // PNBwtmpS1dayAverages_x-130.361_X-130.359_y42.579_Y42.581_z0_Z0_t20061021120000_T20061121120000.nc
        // {\n" +
            " dimensions:\n" +
            "   row = 32;\n" +
            "   IDStringLength = 14;\n" +
            " variables:\n" +
            "   double LON(row=32);\n" +
            "     :_CoordinateAxisType = \"Lon\";\n" +
            "     :actual_range = -130.36, -130.36; // double\n" +
            "     :axis = \"X\";\n" +
            "     :comment = \"The longitude of the station.\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n" +
            "   double LAT(row=32);\n" +
            "     :_CoordinateAxisType = \"Lat\";\n" +
            "     :actual_range = 42.58, 42.58; // double\n" +
            "     :axis = \"Y\";\n" +
            "     :comment = \"The latitude of the station.\";\n" +
            "     :long_name = \"Latitude\";\n" +
            "     :standard_name = \"latitude\";\n" +
            "     :units = \"degrees_north\";\n" +
            "   double DEPTH(row=32);\n" +
            "     :_CoordinateAxisType = \"Height\";\n" +
            "     :_CoordinateZisPositive = \"down\";\n" +
            "     :actual_range = 0.0, 0.0; // double\n" +
            "     :axis = \"Z\";\n" +
            "     :comment = \"The depth of the station, nominally 0 (see station information for details).\";\n" +
            "     :long_name = \"Depth\";\n" +
            "     :positive = \"down\";\n" +
            "     :standard_name = \"depth\";\n" +
            "     :units = \"m\";\n" +
            "   double TIME(row=32);\n" +
            "     :_CoordinateAxisType = \"Time\";\n" +
            "     :actual_range = 1.161432E9, 1.1641104E9; // double\n" +
            "     :axis = \"T\";\n" +
            "     :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n"
            +
            "     :long_name = \"Centered Time of 1 day Averages\";\n" +
            "     :standard_name = \"time\";\n" +
            "     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
            "     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "   char ID(row=32, IDStringLength=14);\n" +
            "     :long_name = \"Station Identifier\";\n" +
            "     :units = \"unitless\";\n" +
            "   float WTMP(row=32);\n" +
            "     :actual_range = 14.391666f, 16.2375f; // float\n" +
            "     :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
            "     :long_name = \"SST\";\n" +
            "     :standard_name = \"sea_surface_temperature\";\n" +
            "     :units = \"degree_C\";\n" +
            "\n" +
            " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
            " :cdm_data_type = \"Station\";\n" +
            " :contributor_name = \"NOAA NDBC and Other Station Owners/Operators\";\n" +
            " :contributor_role = \"Source of data.\";\n" +
            " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            " :creator_email = \"dave.foley@noaa.gov\";\n" +
            " :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
            " :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n";

    Test.ensureEqual(response.substring(0, reference.length()), reference, "");

    // Test.ensureTrue(String2.indexOf(responseArray, " :date_created =
    // \"2006-11-28T00:23:19Z\";") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, " :date_issued =
    // \"2006-11-28T00:23:19Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Easternmost_Easting = -130.36; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_max = 42.58; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_min = 42.58; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lat_units = \"degrees_north\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_max = -130.36; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_min = -130.36; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_lon_units = \"degrees_east\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_max = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_min = 0.0; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_positive = \"down\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :geospatial_vertical_units = \"m\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :history = \"NOAA NDBC") > 0, error);
    // Test.ensureTrue(String2.indexOf(responseArray, "2006-11-28T00:23:19Z NOAA
    // NESDIS CoastWatch WCRN\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :id = \"NDBC_31201_met\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :institution = \"NOAA CoastWatch, West Coast Node\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :keywords = \"Oceans\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :naming_authority = \"gov.noaa.pfel.coastwatch\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray,
        " :NDBCMeasurementDescriptionUrl = \"https://www.ndbc.noaa.gov/measdes.shtml\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Northernmost_Northing = 42.58; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :quality = \"Automated QC checks with periodic manual QC\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :source = \"station observation\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Southernmost_Northing = 42.58; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :standard_name_vocabulary = \"CF Standard Name Table v55\";") > 0,
        error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_end = \"2006-11-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :time_coverage_start = \"2006-10-21T12:00:00Z\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :title = \"SST (NDBC)\";") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " :Westernmost_Easting = -130.36; // double") > 0, error);
    Test.ensureTrue(String2.indexOf(responseArray, " data:") > 0, error);

    Test.ensureEqual(String2.indexOf(responseArray, " :time_coverage_resolution"), -1, error);

    String2.log("  TestBrowsers.testStationScreen finished successfully.");
  }

  /**
   * Do get=... fileType=medium.png requests and display in browser.
   *
   * @param baseUrl usually publicBaseUrl or experimentalBaseUrl
   * @param app     e.g., "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void doGraphicalGetTests(String url) throws Exception {
    String2.log("\n*** start TestBrowsers.doGraphicalGetTests \nurl=" + url);

    String testName = SSR.getTempDirectory() + "TestBrowsersGraphicalGet";

    // bathymetry
    SSR.downloadFile( // throws Exception
        url + "?get=bathymetryData&minLon=-180&maxLon=180&minLat=-90&maxLat=90&fileType=medium.png",
        testName + "0.png", true);
    Test.displayInBrowser(testName + "0.png");

    // grid
    SSR.downloadFile( // throws Exception
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00" +
            "&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=medium.png",
        testName + "1.png", true);
    Test.displayInBrowser(testName + "1.png");

    // grid time series
    SSR.downloadFile( // throws Exception
        url + "?get=gridTimeSeries&dataSet=TQNux10&timePeriod=1day" +
            "&beginTime=2006-10-21T12:00:00&endTime=2006-11-21T12:00:00&lon=-124.81&lat=38.69&fileType=medium.png",
        testName + "2.png", true);
    Test.displayInBrowser(testName + "2.png");

    // vector
    SSR.downloadFile( // throws Exception
        url + "?get=gridVectorData&dataSet=VTQNu10&timePeriod=1day&centeredTime=2006-11-21T12:00:00" +
            "&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=medium.png",
        testName + "3.png", true);
    Test.displayInBrowser(testName + "3.png");

    // station
    SSR.downloadFile( // throws Exception
        url + "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-135.0" +
            "&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0" +
            "&beginTime=2006-11-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=medium.png",
        testName + "4.png", true);
    Test.displayInBrowser(testName + "4.png");

    // station vector
    SSR.downloadFile( // throws Exception
        url + "?get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day" +
            "&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0" +
            "&beginTime=2006-11-21T12:00:00&endTime=2006-11-21T12:00:00&fileType=medium.png",
        testName + "5.png", true);
    Test.displayInBrowser(testName + "5.png");

    // transparent grid
    SSR.downloadFile( // throws Exception
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=2006-11-21T12:00:00" +
            "&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=transparent.png",
        testName + "6.png", true);
    Test.displayInBrowser(testName + "6.png");

    // delete the files
    Math2.sleep(2000);
    for (int i = 0; i < 7; i++)
      File2.delete(testName + i + ".png");

  }

  /**
   * Do graphics tests: request a certain browser set up then request the .png
   * from that screen and display in browser.
   *
   * @param baseUrl usually publicBaseUrl or experimentalBaseUrl
   * @param app     e.g., "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void doGraphicsTests(String baseUrl, String app) throws Exception {
    String2.log("\n*** start TestBrowsers.doGraphicsTests \nbaseUrl=" + baseUrl);
    String url = baseUrl + app;
    String responseArray[];
    String response, error, find;

    // *** these are tests of images shown to user in browser
    if (true) {
      // get an img of grid screen with time series
      responseArray = SSR.getUrlResponseLines(
          url + "?edit=Grid Data&gridDataSet=TQNux10&" +
              "gridTimePeriod=1+day&gridCenteredTime=2006-11-21+12:00:00&" +
              "gridTimeSeriesLon=-124.81&gridTimeSeriesLat=38.69");
      response = String2.toNewlineString(responseArray);
      int po = response.indexOf("public/TQNux10");
      Test.ensureTrue(po >= 0, String2.ERROR + ": (po) image file not found in\n" + response);
      int po2 = response.indexOf(".png", po);
      Test.ensureTrue(po2 >= 0, String2.ERROR + ": (po2) image file not found in\n" + response);
      Test.displayInBrowser(baseUrl + response.substring(po, po2 + 4));
    }

    if (true) {
      // get an img of contour screen with time series
      response = SSR.getUrlResponseStringUnchanged(
          url + "?" +
              "gridDataSet=TQNux10&" +
              "gridTimePeriod=1+day&gridCenteredTime=2006-11-21+12:00:00&" +
              "edit=Contour+Data&contourDataSet=TQNux10&" +
              "contourTimePeriod=1+day&contourCenteredTime=2006-11-21+12:00:00");
      int po = response.indexOf("public/TQNux10");
      Test.ensureTrue(po >= 0, String2.ERROR + ": (po) image file not found in\n" + response);
      int po2 = response.indexOf(".png", po);
      Test.ensureTrue(po2 >= 0, String2.ERROR + ": (po2) image file not found in\n" + response);
      Test.displayInBrowser(baseUrl + response.substring(po, po2 + 4));
    }

    if (true) {
      // get an img of vector screen with time series
      response = SSR.getUrlResponseStringUnchanged(
          url + "?" +
              "gridDataSet=TQNux10&" +
              "gridTimePeriod=1+day&gridCenteredTime=2006-11-21+12:00:00&" +
              "edit=Vector+Data&vectorDataSet=VTQNu10&" +
              "vectorTimePeriod=1+day&vectorCenteredTime=2006-11-21+12:00:00");
      // "vectorTimeSeriesLon=-124.81&vectorTimeSeriesLat=38.69&" +
      int po = response.indexOf("public/TQNux10");
      Test.ensureTrue(po >= 0, String2.ERROR + ": (po) image file not found in\n" + response);
      int po2 = response.indexOf(".png", po);
      Test.ensureTrue(po2 >= 0, String2.ERROR + ": (po2) image file not found in\n" + response);
      Test.displayInBrowser(baseUrl + response.substring(po, po2 + 4));
    }

    if (true) {
      // get an img of station vector screen with time series
      // https://coastwatch.pfeg.noaa.gov/cwexperimental/CWBrowser.jsp?
      // get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day&
      // minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0&
      // beginTime=2006-11-27T23&endTime=2006-11-27T23&fileType=.ncHeader
      response = SSR.getUrlResponseStringUnchanged(
          url + "?edit=Station Vector Data&" +
              "gridDataSet=TQNux10&" +
              "gridTimePeriod=1+day&gridCenteredTime=2006-11-21+12:00:00&" +
              "pointVectorDataSet=PVPNBwsp&pointVectorDepth=0&pointVectorTimePeriod=1+day&" +
              "pointVectorCenteredTime=2006-11-21+12:00:00&pointVectorBeginTime=2006-10-21+12:00:00&" +
              "pointVectorTimeSeries=Station+NDBC+46002+met+(42.58%B0N,+-130.36%B0E)");
      int po = response.indexOf("public/TQNux10");
      Test.ensureTrue(po >= 0, String2.ERROR + ": (po) image file not found in\n" + response);
      int po2 = response.indexOf(".png", po);
      Test.ensureTrue(po2 >= 0, String2.ERROR + ": (po2) image file not found in\n" + response);
      Test.displayInBrowser(baseUrl + response.substring(po, po2 + 4));
    }

    if (true) {
      // get an img of station screen with time series
      response = SSR.getUrlResponseStringUnchanged(
          url + "?edit=Station+Data+1&" +
              "gridDataSet=TQNux10&" +
              "gridTimePeriod=1+day&gridCenteredTime=2006-11-21+12:00:00&" +
              "pointDataSet1=PNBwtmp&pointDepth1=0&pointTimePeriod1=1+day&" +
              "pointCenteredTime1=2006-11-21+12:00:00&pointBeginTime1=2006-10-21+12:00:00&" +
              "pointTimeSeries1=Station+NDBC+46002+met+(42.58%B0N,+-130.36%B0E)");
      int po = response.indexOf("public/TQNux10");
      Test.ensureTrue(po >= 0, String2.ERROR + ": (po) image file not found in\n" + response);
      int po2 = response.indexOf(".png", po);
      Test.ensureTrue(po2 >= 0, String2.ERROR + ": (po2) image file not found in\n" + response);
      Test.displayInBrowser(baseUrl + response.substring(po, po2 + 4));
    }

  }

  /**
   * This tests the use of "latest" as the beginTime, centeredTime or endDate
   * in HTTP GET requests.
   *
   * @param url usually publicBaseUrl or experimentalBaseUrl, plus "CWBrowser.jsp"
   * @throws Exception if trouble
   */
  void testLatest(String url) throws Exception {
    String2.log("\n*** start TestBrowsers.testLatest  \nurl=" + url);

    String responseArray[], response, error, reference;

    // HTTP GET an .ncHeader of a grid file, centeredTime
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&centeredTime=latest" +
            "&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.ncHeader");
    error = "response=" + response; // reference data from public browser
    reference = " dimensions:\n" +
        "   time = 1;   // (has coord.var)\n" +
        "   altitude = 1;   // (has coord.var)\n" +
        "   lat = 113;   // (has coord.var)\n" +
        "   lon = 121;   // (has coord.var)\n" +
        " variables:\n";
    Test.ensureTrue(response.indexOf(reference) > 0, error);

    // HTTP GET an .ncHeader of a grid file, endTime
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridData&dataSet=TQNux10&timePeriod=1day&endDate=latest" +
            "&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0&fileType=.ncHeader");
    error = "response=" + response; // reference data from public browser
    reference = " dimensions:\n" +
        "   time = 1;   // (has coord.var)\n" +
        "   altitude = 1;   // (has coord.var)\n" +
        "   lat = 113;   // (has coord.var)\n" +
        "   lon = 121;   // (has coord.var)\n" +
        " variables:\n";
    Test.ensureTrue(response.indexOf(reference) > 0, error);

    // HTTP GET an .ncHeader of a grid time series file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridTimeSeries&dataSet=TQNux10&timePeriod=1day&beginTime=latest" +
            "&endTime=latest&lon=-127.26&lat=46.65&fileType=.ncHeader");
    error = "response=" + response;
    reference = "   double LON(row=1);\n" +
        "     :_CoordinateAxisType = \"Lon\";\n" +
        "     :actual_range = -127.25, -127.25; // double\n" +
        "     :axis = \"X\";\n" +
        "     :fraction_digits = 2; // int\n" +
        "     :ioos_category = \"Location\";\n" +
        "     :long_name = \"Longitude\";\n" +
        "     :standard_name = \"longitude\";\n" +
        "     :units = \"degrees_east\";\n";
    Test.ensureTrue(response.indexOf(reference) > 0, error);

    // HTTP GET an .ncHeader of a vector file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=gridVectorData&dataSet=VTQNu10&timePeriod=1day" +
            "&centeredTime=latest&minLon=-135.0&maxLon=-105.0&minLat=22.0&maxLat=50.0" +
            "&fileType=.ncHeader");
    error = "response=" + response;
    reference = // reference data from public browser
        " dimensions:\n" +
            "   time = 1;   // (has coord.var)\n" +
            "   altitude = 1;   // (has coord.var)\n" +
            "   lat = 113;   // (has coord.var)\n" +
            "   lon = 121;   // (has coord.var)\n";
    Test.ensureTrue(response.indexOf(reference) > 0, error);

    // HTTP GET an .ncHeader of a station vector file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=stationVectorData&dataSet=PVPNBwsp&timePeriod=1day&minLon=-135.0" +
            "&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0" +
            "&beginTime=latest&endTime=latest&fileType=.ncHeader");
    error = "response=" + response;
    reference = // reference data from public browser
        "     :axis = \"X\";\n" +
            "     :comment = \"The longitude of the station.\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n";
    Test.ensureTrue(response.indexOf(reference) > 0, error);

    // HTTP GET an .ncHeader of a station file
    response = SSR.getUrlResponseStringUnchanged(
        url + "?get=stationData&dataSet=PNBwtmp&timePeriod=1day&minLon=-135.0" +
            "&maxLon=-105.0&minLat=22.0&maxLat=50.0&minDepth=0&maxDepth=0" +
            "&beginTime=latest&endTime=latest&fileType=.ncHeader");
    error = "response=" + response;
    reference = // reference data from public browser
        "     :axis = \"X\";\n" +
            "     :comment = \"The longitude of the station.\";\n" +
            "     :long_name = \"Longitude\";\n" +
            "     :standard_name = \"longitude\";\n" +
            "     :units = \"degrees_east\";\n";
    Test.ensureTrue(response.indexOf(reference) > 0, error);

  }

  /** do all the browser tests */
  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testAll() throws Exception {
    String baseUrl = experimentalBaseUrl;
    String app = "CWBrowser.jsp";

    testGridScreen(baseUrl + app);
    testContourScreen(baseUrl + app);
    testVectorScreen(baseUrl + app);
    testBathymetry(baseUrl + app);
    testStationScreen(baseUrl + app);
    testStationVectorScreen(baseUrl + app);

    doGraphicsTests(baseUrl, app);
    doGraphicalGetTests(baseUrl + app);
    testLatest(baseUrl + app);
  }

}
