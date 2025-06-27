package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import tags.TagThredds;

class SaveOpendapTests {

  /** This tests the methods in this class. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void basicTest() throws Exception {
    String2.log("\n*** SaveOpendap.basicTest...");
    SaveOpendap.verbose = true;
    // String dir = "c:/temp/";
    String dir = File2.getSystemTempDirectory();
    String name;

    // test ndbc It would be better test if it actually tested the data.
    name = "SaveOpendapAsNcNDBC.nc";
    SaveOpendap.asNc(
        "https://dods.ndbc.noaa.gov/thredds/dodsC/data/stdmet/31201/31201h2005.nc", dir + name);
    String info = NcHelper.ncdump(dir + name, "-h");
    int po = info.indexOf("{");
    info = info.substring(po);
    String shouldBe =
        "{\n"
            + "  dimensions:\n"
            + "    time = UNLIMITED;   // (803 currently)\n"
            + "    latitude = 1;\n"
            + "    longitude = 1;\n"
            + "  variables:\n"
            + "    int wind_dir(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Wind Direction\";\n"
            + "      :short_name = \"wdir\";\n"
            + "      :standard_name = \"wind_from_direction\";\n"
            + "      :units = \"degrees_true\";\n"
            + "      :_FillValue = 999; // int\n"
            + "\n"
            + "    float wind_spd(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Wind Speed\";\n"
            + "      :short_name = \"wspd\";\n"
            + "      :standard_name = \"wind_speed\";\n"
            + "      :units = \"meters/second\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    float gust(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Wind Gust Speed\";\n"
            + "      :short_name = \"gst\";\n"
            + "      :standard_name = \"gust\";\n"
            + "      :units = \"meters/second\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    float wave_height(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Significant Wave Height\";\n"
            + "      :short_name = \"wvht\";\n"
            + "      :standard_name = \"significant_height_of_wave\";\n"
            + "      :units = \"meters\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    float dominant_wpd(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Dominant Wave Period\";\n"
            + "      :short_name = \"dpd\";\n"
            + "      :standard_name = \"dominant_wave_period\";\n"
            + "      :units = \"seconds\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    float average_wpd(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Average Wave Period\";\n"
            + "      :short_name = \"apd\";\n"
            + "      :standard_name = \"average_wave_period\";\n"
            + "      :units = \"seconds\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    int mean_wave_dir(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Mean Wave Direction\";\n"
            + "      :short_name = \"mwd\";\n"
            + "      :standard_name = \"mean_wave_direction\";\n"
            + "      :units = \"degrees_true\";\n"
            + "      :_FillValue = 999; // int\n"
            + "\n"
            + "    float air_pressure(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Sea Level Pressure\";\n"
            + "      :short_name = \"pres\";\n"
            + "      :standard_name = \"air_pressure_at_sea_level\";\n"
            + "      :units = \"hPa\";\n"
            + "      :_FillValue = 9999.0f; // float\n"
            + "\n"
            + "    float air_temperature(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Air Temperature\";\n"
            + "      :short_name = \"atmp\";\n"
            + "      :standard_name = \"air_temperature\";\n"
            + "      :units = \"degree_Celsius\";\n"
            + "      :_FillValue = 999.0f; // float\n"
            + "\n"
            + "    float sea_surface_temperature(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Sea Surface Temperature\";\n"
            + "      :short_name = \"wtmp\";\n"
            + "      :standard_name = \"sea_surface_temperature\";\n"
            + "      :units = \"degree_Celsius\";\n"
            + "      :_FillValue = 999.0f; // float\n"
            + "\n"
            + "    float dewpt_temperature(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Dew Point Temperature\";\n"
            + "      :short_name = \"dewp\";\n"
            + "      :standard_name = \"dew_point_temperature\";\n"
            + "      :units = \"degree_Celsius\";\n"
            + "      :_FillValue = 999.0f; // float\n"
            + "\n"
            + "    float visibility(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Visibility\";\n"
            + "      :short_name = \"vis\";\n"
            + "      :standard_name = \"visibility_in_air\";\n"
            + "      :units = \"US_statute_miles\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    float water_level(time=803, latitude=1, longitude=1);\n"
            + "      :_CoordinateAxes = \"time latitude longitude \";\n"
            + "      :long_name = \"Tide Water Level\";\n"
            + "      :short_name = \"tide\";\n"
            + "      :standard_name = \"water_level\";\n"
            + "      :units = \"feet\";\n"
            + "      :_FillValue = 99.0f; // float\n"
            + "\n"
            + "    int time(time=803);\n"
            + "      :long_name = \"Epoch Time\";\n"
            + "      :short_name = \"time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :units = \"seconds since 1970-01-01 00:00:00 UTC\";\n"
            + "      :calendar = \"gregorian\";\n"
            + // appeared with switch to netcdf-java 4.6.4
            "      :_CoordinateAxisType = \"Time\";\n"
            + "\n"
            + "    float latitude(latitude=1);\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :short_name = \"latitude\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "\n"
            + "    float longitude(longitude=1);\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :short_name = \"longitude\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :institution = \"NOAA National Data Buoy Center and Participators in Data Assembly Center\";\n"
            + "  :url = \"http://dods.ndbc.noaa.gov\";\n"
            + "  :quality = \"Automated QC checks with manual editing and comprehensive monthly QC\";\n"
            + "  :conventions = \"COARDS\";\n"
            + "  :station = \"31201\";\n"
            + "  :comment = \"Floripa, Brazil (109)\";\n"
            + "  :location = \"27.70 S 48.13 W \";\n"
            + "  :_CoordSysBuilder = \"ucar.nc2.internal.dataset.conv.CoardsConventions\";\n"
            + // 2021-01-07 changed with
            // netcdf-java 5.4.1
            // " :_CoordSysBuilder = \"ucar.nc2.dataset.conv.COARDSConvention\";\n" +
            // //2013-02-21 reappeared. 2012-07-30 disappeared
            "}\n";
    Test.ensureEqual(info, shouldBe, "info=" + info);
    File2.delete(dir + name);

    /*
     * //THIS WORKS BUT ITS TOO MUCH DATA FROM SOMEONE ELSE TO TEST ROUTINELY.
     * //test MBARI M0: it has no structures, a unlimited dimension, global
     * metadata,
     * // and 1D and 4D variables with metadata
     * //in browser, see
     * http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.html
     * name = "SaveOpendapAsNcMBARI.nc";
     * //asNc(
     * "dods://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc", dir
     * + name); //doesn't solve problem
     * asNc(
     * "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc", dir
     * + name);
     * String2.log(NcHelper.ncdump(dir + name, "-h"));
     * File2.delete(dir + name);
     */

    /*
     * doesn't work yet
     * //test an opendap sequence (see Table.testConvert)
     * name = "sequence.nc";
     * asNc(
     * "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"",
     * dir + name);
     * //String outName = testDir + "convert.nc";
     * //convert(inName, READ_OPENDAP_SEQUENCE, outName, SAVE_AS_NC, "row", false);
     * //Table table = new Table();
     * //table.readFlatNc(outName, null, 0); //standardizeWhat=0; it should be
     * already unpacked
     * //String2.log(table.toString(3));
     * //Test.ensureEqual(table.nColumns(), 2, "");
     * //Test.ensureEqual(table.nRows(), 190, "");
     * //Test.ensureEqual(table.getColumnName(0), "t0", "");
     * //Test.ensureEqual(table.getColumnName(1), "oxygen", "");
     * //Test.ensureEqual(table.columnAttributes(0).getString("long_name"),
     * "Temperature T0", "");
     * //Test.ensureEqual(table.columnAttributes(1).getString("long_name"),
     * "Oxygen", "");
     * //Test.ensureEqual(table.getDoubleData(0, 0), 12.1185, "");
     * //Test.ensureEqual(table.getDoubleData(0, 1), 12.1977, "");
     * //Test.ensureEqual(table.getDoubleData(1, 0), 6.56105, "");
     * //Test.ensureEqual(table.getDoubleData(1, 1), 6.95252, "");
     * //File2.delete(outName);
     * String2.log(NcHelper.ncdump(dir + name, ""));
     * //File2.delete(dir + name);
     */

  }
}
