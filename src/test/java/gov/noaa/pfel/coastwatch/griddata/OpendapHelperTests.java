package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.Attributes;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.nio.file.Path;
import opendap.dap.DAS;
import opendap.dap.DConnect2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import tags.TagDisabledThredds;
import testDataset.Initialization;

class OpendapHelperTests {

  @TempDir private static Path TEMP_DIR;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This tests getting attibutes, notably the DODS_strlen attribute. */
  @org.junit.jupiter.api.Test
  @TagDisabledThredds
  void testGetAttributes() throws Throwable {
    String url =
        "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
    String2.log("\n* OpendapHelper.testGetAttributes\n" + url);
    try (DConnect2 dConnect = new DConnect2(url, true)) {
      DAS das = dConnect.getDAS();
      Attributes atts = new Attributes();
      OpendapHelper.getAttributes(das, "flag", atts);

      String results = atts.toString();
      String expected = // the DODS_ attributes are from an attribute that is a container.
          "    A=Units added\n"
              + "    B=Data out of range\n"
              + "    C=Non-sequential time\n"
              + "    D=Failed T>=Tw>=Td\n"
              + "    DODS_dimName=f_string\n"
              + "    DODS_strlen=13i\n"
              + "    E=True wind error\n"
              + "    F=Velocity unrealistic\n"
              + "    G=Value > 4 s. d. from climatology\n"
              + "    H=Discontinuity\n"
              + "    I=Interesting feature\n"
              + "    J=Erroneous\n"
              + "    K=Suspect - visual\n"
              + "    L=Ocean platform over land\n"
              + "    long_name=quality control flags\n"
              + "    M=Instrument malfunction\n"
              + "    N=In Port\n"
              + "    O=Multiple original units\n"
              + "    P=Movement uncertain\n"
              + "    Q=Pre-flagged as suspect\n"
              + "    R=Interpolated data\n"
              + "    S=Spike - visual\n"
              + "    T=Time duplicate\n"
              + "    U=Suspect - statistial\n"
              + "    V=Spike - statistical\n"
              + "    X=Step - statistical\n"
              + "    Y=Suspect between X-flags\n"
              + "    Z=Good data\n";
      Test.ensureEqual(results, expected, "results=" + results);
    }
  }

  /** This tests parseStartStrideStop and throws exception if trouble. */
  @org.junit.jupiter.api.Test
  void testParseStartStrideStop() {

    // test calculateNValues
    Test.ensureEqual(OpendapHelper.calculateNValues(1, 1, 3), 3, "");
    Test.ensureEqual(OpendapHelper.calculateNValues(1, 2, 3), 2, "");
    Test.ensureEqual(OpendapHelper.calculateNValues(1, 2, 4), 2, "");
    try {
      OpendapHelper.calculateNValues(4, 2, 3);
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: start=4 must be less than or equal to stop=3",
          "");
    }
    try {
      OpendapHelper.calculateNValues(3, 0, 5);
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(), "java.lang.RuntimeException: stride=0 must be greater than 0", "");
    }
  }

  public static String dds(String fileName) throws Exception {
    String sar[] = String2.splitNoTrim(NcHelper.readCDL(fileName), '\n');
    int n = sar.length;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      String trimS = sar[i].trim();
      if (trimS.length() > 0 && !trimS.startsWith(":")) sb.append(sar[i] + "\n");
      sar[i] = null;
    }
    return sb.toString();
  }

  /**
   * Test allDapToNc.
   *
   * @param whichTests -1 for all, or 0.. for specific ones
   */
  @org.junit.jupiter.api.Test
  @TagDisabledThredds
  void testAllDapToNc() throws Throwable {
    // tests from nodc template examples https://www.ncei.noaa.gov/netcdf-templates
    String2.log("\n*** OpendapHelper.testAllDapToNc()");
    String dir = OpendapHelperTests.class.getResource("/data/nodcTemplates/").getPath();
    // 2023-02-15 This method hadn't been run since 2020 because tdsUrl often
    // stalled, so I had commented it out.
    // tdsUrl was
    // "https://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/";
    // //+e.g., point/KachemakBay.nc";
    String tdsUrl = "https://www.ncei.noaa.gov/thredds-ocean/dodsC/example/v1.0/"; // +e.g.,
    // point/KachemakBay.nc";
    String fileName;
    String url, results, expected;

    // this tests numeric scalars, and numeric and String 1D arrays
    fileName = "pointKachemakBay.nc";
    url = tdsUrl + "point/KachemakBay.nc";
    OpendapHelper.allDapToNc(url, dir + fileName);
    results = dds(dir + fileName);
    // String2.log(results);
    // expected = "zztop";
    // Test.ensureEqual(results, expected, "");

    // this tests numeric and String scalars, and numeric 1D arrays
    fileName = "timeSeriesBodegaMarineLabBuoy.nc";
    url = tdsUrl + "timeSeries/BodegaMarineLabBuoy.nc";
    OpendapHelper.allDapToNc(url, dir + fileName);
    results = dds(dir + fileName);
    expected =
        "netcdf "
            + dir
            + "timeSeriesBodegaMarineLabBuoy.nc {\n"
            + "  dimensions:\n"
            + "    time = 63242;\n"
            + "    string1 = 1;\n"
            + "    station_name_strlen = 17;\n"
            + "  variables:\n"
            + "    double time(time=63242);\n"
            + "    float lat;\n"
            + "    float lon;\n"
            + "    double alt;\n"
            + "    char station_name(string1=1, station_name_strlen=17);\n"
            + "    double temperature(time=63242);\n"
            + "    double salinity(time=63242);\n"
            + "    double density(time=63242);\n"
            + "    double conductivity(time=63242);\n"
            + "    int platform1;\n"
            + "    int temperature_qc(time=63242);\n"
            + "    int salinity_qc(time=63242);\n"
            + "    int density_qc(time=63242);\n"
            + "    int conductivity_qc(time=63242);\n"
            + "    int instrument1;\n"
            + "    int instrument2;\n"
            + "    double ht_wgs84;\n"
            + "    double ht_mllw;\n"
            + "    int crs;\n"
            + "  // global attributes:\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // this tests numeric scalars, and grids
    fileName = "trajectoryAoml_tsg.nc";
    url = tdsUrl + "trajectory/aoml_tsg.nc";
    OpendapHelper.allDapToNc(url, dir + fileName);
    results = dds(dir + fileName);
    // String2.log(results);
    expected =
        "netcdf "
            + dir
            + "trajectoryAoml_tsg.nc {\n"
            + "  dimensions:\n"
            + "    trajectory = 1;\n"
            + "    obs = 2880;\n"
            + "  variables:\n"
            + "    int trajectory(trajectory=1);\n"
            + "    int time(trajectory=1, obs=2880);\n"
            + "    double lat(trajectory=1, obs=2880);\n"
            + "    double lon(trajectory=1, obs=2880);\n"
            + "    double intp(trajectory=1, obs=2880);\n"
            + "    double sal(trajectory=1, obs=2880);\n"
            + "    double cond(trajectory=1, obs=2880);\n"
            + "    double ext(trajectory=1, obs=2880);\n"
            + "    double sst(trajectory=1, obs=2880);\n"
            + "    byte plt(trajectory=1);\n"
            + "    byte tsg(trajectory=1);\n"
            + "    byte tmsr(trajectory=1);\n"
            + "    byte sstr(trajectory=1);\n"
            + "    byte flag_a(trajectory=1, obs=2880);\n"
            + "    byte flag_b(trajectory=1, obs=2880);\n"
            + "    byte flag_c(trajectory=1, obs=2880);\n"
            + "    byte flag_d(trajectory=1, obs=2880);\n"
            + "    byte flag_e(trajectory=1, obs=2880);\n"
            + "    byte flag_f(trajectory=1, obs=2880);\n"
            + "    byte flag_g(trajectory=1, obs=2880);\n"
            + "    byte flag_h(trajectory=1, obs=2880);\n"
            + "    byte flag_i(trajectory=1, obs=2880);\n"
            + "    byte flag_j(trajectory=1, obs=2880);\n"
            + "    byte flag_k(trajectory=1, obs=2880);\n"
            + "    byte flag_l(trajectory=1, obs=2880);\n"
            + "    byte crs(trajectory=1);\n"
            + "  // global attributes:\n"
            + "}\n";
    Test.ensureEqual(results, expected, "");

    // this tests numeric scalars, and byte/numeric arrays
    fileName = "trajectoryJason2_satelliteAltimeter.nc";
    url = tdsUrl + "trajectory/jason2_satelliteAltimeter.nc";
    OpendapHelper.allDapToNc(url, dir + fileName);
    results = dds(dir + fileName);
    // String2.log(results);
    expected =
        "netcdf "
            + dir
            + "trajectoryJason2_satelliteAltimeter.nc {\n"
            + "  dimensions:\n"
            + "    trajectory = 1;\n"
            + "    obs = 3;\n"
            + "    meas_ind = 20;\n"
            + "  variables:\n"
            + "    double time(trajectory=1, obs=3);\n"
            + "    byte meas_ind(trajectory=1, meas_ind=20);\n"
            + "    int lat(trajectory=1, obs=3);\n"
            + "    int lon(trajectory=1, obs=3);\n"
            + "    byte surface_type(trajectory=1, obs=3);\n"
            + "    byte orb_state_flag_rest(trajectory=1, obs=3);\n"
            + "    byte ecmwf_meteo_map_avail(trajectory=1, obs=3);\n"
            + "    byte interp_flag_meteo(trajectory=1, obs=3);\n"
            + "    int alt(trajectory=1, obs=3);\n"
            + "    byte range_numval_ku(trajectory=1, obs=3);\n"
            + "    short model_wet_tropo_corr(trajectory=1, obs=3);\n"
            + "    byte atmos_corr_sig0_ku(trajectory=1, obs=3);\n"
            + "    short tb_187(trajectory=1, obs=3);\n"
            + "    short rad_water_vapor(trajectory=1, obs=3);\n"
            + "    short ssha(trajectory=1, obs=3);\n"
            + "  // global attributes:\n"
            + "}\n";
    Test.ensureEqual(results, expected, "");

    /*
     * if (whichTests == -1 || whichTests == 4) {
     * //JDAP fails to read/parse the .dds:
     * //Exception in thread "main" com.cohort.util.SimpleException: Error while
     * getting DDS from https://data.nodc.noaa.gov/thredds/dodsC/testdata/ne
     * //tCDFTemplateExamples/profile/wodObservedLevels.nc.dds .
     * //
     * //Parse Error on token: String
     * //In the dataset descriptor object:
     * //Expected a variable declaration (e.g., Int32 i;).
     * // at
     * gov.noaa.pfel.coastwatch.griddata.OpendapHelper.allDapToNc(OpendapHelper.java
     * :1239)
     * // at
     * gov.noaa.pfel.coastwatch.griddata.OpendapHelper.testAllDapToNc(OpendapHelper.
     * java:1716)
     * // at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:741)
     * //this tests numeric scalars, and numeric and string arrays
     * fileName = "profileWodObservedLevels.nc";
     * url = tdsUrl + "profile/wodObservedLevels.nc";
     * allDapToNc(url, dir + fileName);
     * results = NcHelper.ncdump(dir + fileName, "-h");
     * String2.log(results);
     * //expected = "zztop";
     * //Test.ensureEqual(results, expected, "");
     * }
     */
    // this tests numeric scalars, and numeric arrays
    fileName = "timeSeriesProfileUsgs_internal_wave_timeSeries.nc";
    url = tdsUrl + "timeSeriesProfile/usgs_internal_wave_timeSeries.nc";
    OpendapHelper.allDapToNc(url, dir + fileName);
    results = dds(dir + fileName);
    // String2.log(results);
    expected =
        "netcdf "
            + dir
            + "timeSeriesProfileUsgs_internal_wave_timeSeries.nc {\n"
            + "  dimensions:\n"
            + "    station = 1;\n"
            + "    time = 38990;\n"
            + "    z = 5;\n"
            + "  variables:\n"
            + "    int station_id(station=1);\n"
            + "    double time(time=38990);\n"
            + "    double z(z=5);\n"
            + "    double lon(station=1);\n"
            + "    double lat(station=1);\n"
            + "    double T_20(station=1, time=38990, z=5);\n"
            + "    double C_51(station=1, time=38990, z=5);\n"
            + "    double S_40(station=1, time=38990, z=5);\n"
            + "    double STH_71(station=1, time=38990, z=5);\n"
            + "    int instrument_1(station=1, z=5);\n"
            + "    int instrument_2(station=1);\n"
            + "    int platform;\n"
            + "    int crs;\n"
            + "  // global attributes:\n"
            + "}\n";
    Test.ensureEqual(results, expected, "");

    // currently no trajectoryProfile example

    // currently no swath example

  }
}
