package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.DAS;
import dods.dap.DConnect;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import tags.TagIncompleteTest;
import tags.TagThredds;

class OpendapHelperTests {

  @TempDir private static Path TEMP_DIR;

  /** This tests getting attibutes, notably the DODS_strlen attribute. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGetAttributes() throws Throwable {
    String url =
        "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
    String2.log("\n* OpendapHelper.testGetAttributes\n" + url);
    DConnect dConnect = new DConnect(url, true, 1, 1);
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
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

  /** This tests dapToNc DArray. */
  @org.junit.jupiter.api.Test
  // TODO- look into out of memory. It happens within dods.dap, it's unclear what version is here.
  //   Maybe get a new version.
  @TagIncompleteTest
  void testDapToNcDArray() throws Throwable {
    String2.log("\n\n*** OpendapHelper.testDapToNcDArray()");
    String fileName, expected, results;
    String today = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);

    fileName = TEMP_DIR.toAbsolutePath() + "/testDapToNcDArray.nc";
    String dArrayUrl =
        "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
    OpendapHelper.dapToNc(
        dArrayUrl,
        // note that request for zztop is ignored (because not found)
        new String[] {"zztop", "time", "lat", "lon", "PL_HD", "flag"},
        null, // projection
        fileName,
        false); // jplMode
    results = NcHelper.ncdump(fileName, ""); // printData
    expected =
        "netcdf testDapToNcDArray.nc {\n"
            + "  dimensions:\n"
            + "    time = 144;\n"
            + "    flag_strlen = 13;\n"
            + "  variables:\n"
            + "    int time(time=144);\n"
            + "      :actual_range = 16870896, 16871039; // int\n"
            + "      :data_interval = 60; // int\n"
            + "      :long_name = \"time\";\n"
            + "      :observation_type = \"calculated\";\n"
            + "      :original_units = \"hhmmss UTC\";\n"
            + "      :qcindex = 1; // int\n"
            + "      :units = \"minutes since 1-1-1980 00:00 UTC\";\n"
            + "\n"
            + "    float lat(time=144);\n"
            + "      :actual_range = 44.6f, 44.75f; // float\n"
            + "      :average_center = \"time at end of period\";\n"
            + "      :average_length = 60S; // short\n"
            + "      :average_method = \"average\";\n"
            + "      :data_precision = -9999.0f; // float\n"
            + "      :instrument = \"unknown\";\n"
            + "      :long_name = \"latitude\";\n"
            + "      :observation_type = \"measured\";\n"
            + "      :original_units = \"degrees (+N)\";\n"
            + "      :qcindex = 2; // int\n"
            + "      :sampling_rate = 1.0f; // float\n"
            + "      :units = \"degrees (+N)\";\n"
            + "\n"
            + "    float lon(time=144);\n"
            + "      :actual_range = 235.82f, 235.95f; // float\n"
            + "      :average_center = \"time at end of period\";\n"
            + "      :average_length = 60S; // short\n"
            + "      :average_method = \"average\";\n"
            + "      :data_precision = -9999.0f; // float\n"
            + "      :instrument = \"unknown\";\n"
            + "      :long_name = \"longitude\";\n"
            + "      :observation_type = \"measured\";\n"
            + "      :original_units = \"degrees (-W/+E)\";\n"
            + "      :qcindex = 3; // int\n"
            + "      :sampling_rate = 1.0f; // float\n"
            + "      :units = \"degrees (+E)\";\n"
            + "\n"
            + "    float PL_HD(time=144);\n"
            + "      :actual_range = 37.89f, 355.17f; // float\n"
            + "      :average_center = \"time at end of period\";\n"
            + "      :average_length = 60S; // short\n"
            + "      :average_method = \"average\";\n"
            + "      :data_precision = -9999.0f; // float\n"
            + "      :instrument = \"unknown\";\n"
            + "      :long_name = \"platform heading\";\n"
            + "      :missing_value = -9999.0f; // float\n"
            + "      :observation_type = \"calculated\";\n"
            + "      :original_units = \"degrees (clockwise towards true north)\";\n"
            + "      :qcindex = 4; // int\n"
            + "      :sampling_rate = 1.0f; // float\n"
            + "      :special_value = -8888.0f; // float\n"
            + "      :units = \"degrees (clockwise towards true north)\";\n"
            + "\n"
            + "    char flag(time=144, flag_strlen=13);\n"
            + "      :A = \"Units added\";\n"
            + "      :B = \"Data out of range\";\n"
            + "      :C = \"Non-sequential time\";\n"
            + "      :D = \"Failed T>=Tw>=Td\";\n"
            + "      :DODS_dimName = \"f_string\";\n"
            + "      :DODS_strlen = 13; // int\n"
            + "      :E = \"True wind error\";\n"
            + "      :F = \"Velocity unrealistic\";\n"
            + "      :G = \"Value > 4 s. d. from climatology\";\n"
            + "      :H = \"Discontinuity\";\n"
            + "      :I = \"Interesting feature\";\n"
            + "      :J = \"Erroneous\";\n"
            + "      :K = \"Suspect - visual\";\n"
            + "      :L = \"Ocean platform over land\";\n"
            + "      :long_name = \"quality control flags\";\n"
            + "      :M = \"Instrument malfunction\";\n"
            + "      :N = \"In Port\";\n"
            + "      :O = \"Multiple original units\";\n"
            + "      :P = \"Movement uncertain\";\n"
            + "      :Q = \"Pre-flagged as suspect\";\n"
            + "      :R = \"Interpolated data\";\n"
            + "      :S = \"Spike - visual\";\n"
            + "      :T = \"Time duplicate\";\n"
            + "      :U = \"Suspect - statistial\";\n"
            + "      :V = \"Spike - statistical\";\n"
            + "      :X = \"Step - statistical\";\n"
            + "      :Y = \"Suspect between X-flags\";\n"
            + "      :Z = \"Good data\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :contact_email = \"samos@coaps.fsu.edu\";\n"
            + "  :contact_info = \"Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840, USA\";\n"
            + "  :Cruise_id = \"Cruise_id undefined for now\";\n"
            + "  :Data_modification_date = \"02/07/2012 10:03:37 EST\";\n"
            + "  :data_provider = \"Timothy Salisbury\";\n"
            + "  :elev = 0S; // short\n"
            + "  :end_date_time = \"2012/01/28 -- 23:59  UTC\";\n"
            + "  :EXPOCODE = \"EXPOCODE undefined for now\";\n"
            + "  :facility = \"NOAA\";\n"
            + "  :fsu_version = \"300\";\n"
            + "  :ID = \"WTEP\";\n"
            + "  :IMO = \"009270335\";\n"
            + "  :Metadata_modification_date = \"02/07/2012 10:03:37 EST\";\n"
            + "  :platform = \"SCS\";\n"
            + "  :platform_version = \"4.0\";\n"
            + "  :receipt_order = \"01\";\n"
            + "  :site = \"OSCAR DYSON\";\n"
            + "  :start_date_time = \"2012/01/28 -- 21:36  UTC\";\n"
            + "  :title = \"OSCAR DYSON Meteorological Data\";\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {16870896, 16870897, 16870898, 16870899, 16870900, 16870901, 16870902, 16870903, 16870904, 16870905, 16870906, 16870907, 16870908, 16870909, 16870910, 16870911, 16870912, 16870913, 16870914, 16870915, 16870916, 16870917, 16870918, 16870919, 16870920, 16870921, 16870922, 16870923, 16870924, 16870925, 16870926, 16870927, 16870928, 16870929, 16870930, 16870931, 16870932, 16870933, 16870934, 16870935, 16870936, 16870937, 16870938, 16870939, 16870940, 16870941, 16870942, 16870943, 16870944, 16870945, 16870946, 16870947, 16870948, 16870949, 16870950, 16870951, 16870952, 16870953, 16870954, 16870955, 16870956, 16870957, 16870958, 16870959, 16870960, 16870961, 16870962, 16870963, 16870964, 16870965, 16870966, 16870967, 16870968, 16870969, 16870970, 16870971, 16870972, 16870973, 16870974, 16870975, 16870976, 16870977, 16870978, 16870979, 16870980, 16870981, 16870982, 16870983, 16870984, 16870985, 16870986, 16870987, 16870988, 16870989, 16870990, 16870991, 16870992, 16870993, 16870994, 16870995, 16870996, 16870997, 16870998, 16870999, 16871000, 16871001, 16871002, 16871003, 16871004, 16871005, 16871006, 16871007, 16871008, 16871009, 16871010, 16871011, 16871012, 16871013, 16871014, 16871015, 16871016, 16871017, 16871018, 16871019, 16871020, 16871021, 16871022, 16871023, 16871024, 16871025, 16871026, 16871027, 16871028, 16871029, 16871030, 16871031, 16871032, 16871033, 16871034, 16871035, 16871036, 16871037, 16871038, 16871039}\n"
            + "    lat = \n"
            + "      {44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.62, 44.62, 44.62, 44.62, 44.62, 44.62, 44.62, 44.61, 44.61, 44.61, 44.61, 44.61, 44.61, 44.61, 44.61, 44.6, 44.6, 44.6, 44.6, 44.6, 44.6, 44.61, 44.61, 44.61, 44.61, 44.62, 44.62, 44.62, 44.62, 44.63, 44.63, 44.63, 44.64, 44.64, 44.64, 44.65, 44.65, 44.65, 44.66, 44.66, 44.66, 44.67, 44.67, 44.67, 44.68, 44.68, 44.68, 44.69, 44.69, 44.69, 44.7, 44.7, 44.7, 44.71, 44.71, 44.71, 44.72, 44.72, 44.72, 44.73, 44.73, 44.73, 44.73, 44.74, 44.74, 44.74, 44.75}\n"
            + "    lon = \n"
            + "      {235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.94, 235.94, 235.94, 235.94, 235.94, 235.94, 235.93, 235.93, 235.93, 235.92, 235.92, 235.92, 235.91, 235.91, 235.91, 235.9, 235.9, 235.9, 235.89, 235.89, 235.88, 235.88, 235.88, 235.88, 235.88, 235.88, 235.87, 235.87, 235.87, 235.87, 235.87, 235.87, 235.87, 235.86, 235.86, 235.86, 235.86, 235.86, 235.86, 235.86, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.85, 235.84, 235.84, 235.84, 235.84, 235.84, 235.84, 235.83, 235.83, 235.83, 235.83, 235.83, 235.82, 235.82, 235.82, 235.82, 235.82, 235.82, 235.82}\n"
            + "    PL_HD = \n"
            + "      {75.53, 75.57, 75.97, 76.0, 75.81, 75.58, 75.99, 75.98, 75.77, 75.61, 75.72, 75.75, 75.93, 75.96, 76.01, 75.64, 75.65, 75.94, 75.93, 76.12, 76.65, 76.42, 76.25, 75.81, 76.5, 76.09, 76.35, 76.0, 76.16, 76.36, 76.43, 75.99, 75.93, 76.41, 75.85, 76.07, 76.15, 76.33, 76.7, 76.37, 76.58, 76.89, 77.14, 76.81, 74.73, 75.24, 74.52, 81.04, 80.64, 73.21, 63.34, 37.89, 347.02, 309.93, 290.99, 285.0, 279.38, 276.45, 270.26, 266.33, 266.49, 266.08, 263.59, 261.41, 259.05, 259.82, 260.35, 262.78, 258.73, 249.71, 246.52, 245.78, 246.16, 245.88, 243.52, 231.62, 223.09, 221.08, 221.01, 221.08, 220.81, 223.64, 234.12, 239.55, 241.08, 242.09, 242.04, 242.33, 242.06, 242.22, 242.11, 242.3, 242.07, 247.35, 285.6, 287.02, 287.96, 288.37, 321.32, 344.82, 346.91, 344.78, 347.95, 344.75, 344.66, 344.78, 344.7, 344.76, 343.89, 336.73, 334.01, 340.23, 344.76, 348.25, 348.74, 348.63, 351.97, 344.55, 343.77, 343.71, 347.04, 349.06, 349.45, 349.79, 349.66, 349.7, 349.74, 344.2, 343.22, 341.79, 339.11, 334.12, 334.47, 334.62, 334.7, 334.66, 327.06, 335.74, 348.25, 351.05, 355.17, 343.66, 346.85, 347.28}\n"
            + "    flag =   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZEZZSZZZZ\",   \"ZZZZZEZZSZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\",   \"ZZZZZZZZZZZZZ\"\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=" + results);
    File2.delete(fileName);

    // test subset
    try {
      String2.log("\n* testDapToNcDArray Subset");
      fileName = TEMP_DIR.toAbsolutePath() + "/testDapToNcDArraySubset.nc";
      System.out.println(fileName);
      String dArraySubsetUrl =
          "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
      OpendapHelper.dapToNc(
          dArraySubsetUrl,
          new String[] {"zztop", "time", "lat", "lon", "PL_HD", "flag"},
          "[0:10:99]", // projection
          fileName,
          false); // jplMode
      results = NcHelper.ncdump(fileName, ""); // printData
      expected =
          "netcdf testDapToNcDArraySubset.nc {\n"
              + " dimensions:\n"
              + "   time = 10;\n"
              + " variables:\n"
              + "   int time(time=10);\n"
              + "     :actual_range = 16870896, 16871039; // int\n"
              + "     :data_interval = 60; // int\n"
              + "     :long_name = \"time\";\n"
              + "     :observation_type = \"calculated\";\n"
              + "     :original_units = \"hhmmss UTC\";\n"
              + "     :qcindex = 1; // int\n"
              + "     :units = \"minutes since 1-1-1980 00:00 UTC\";\n"
              + "   float lat(time=10);\n"
              + "     :actual_range = 44.6f, 44.75f; // float\n"
              + "     :average_center = \"time at end of period\";\n"
              + "     :average_length = 60S; // short\n"
              + "     :average_method = \"average\";\n"
              + "     :data_precision = -9999.0f; // float\n"
              + "     :instrument = \"unknown\";\n"
              + "     :long_name = \"latitude\";\n"
              + "     :observation_type = \"measured\";\n"
              + "     :original_units = \"degrees (+N)\";\n"
              + "     :qcindex = 2; // int\n"
              + "     :sampling_rate = 1.0f; // float\n"
              + "     :units = \"degrees (+N)\";\n"
              + "   float lon(time=10);\n"
              + "     :actual_range = 235.82f, 235.95f; // float\n"
              + "     :average_center = \"time at end of period\";\n"
              + "     :average_length = 60S; // short\n"
              + "     :average_method = \"average\";\n"
              + "     :data_precision = -9999.0f; // float\n"
              + "     :instrument = \"unknown\";\n"
              + "     :long_name = \"longitude\";\n"
              + "     :observation_type = \"measured\";\n"
              + "     :original_units = \"degrees (-W/+E)\";\n"
              + "     :qcindex = 3; // int\n"
              + "     :sampling_rate = 1.0f; // float\n"
              + "     :units = \"degrees (+E)\";\n"
              + "   float PL_HD(time=10);\n"
              + "     :actual_range = 37.89f, 355.17f; // float\n"
              + "     :average_center = \"time at end of period\";\n"
              + "     :average_length = 60S; // short\n"
              + "     :average_method = \"average\";\n"
              + "     :data_precision = -9999.0f; // float\n"
              + "     :instrument = \"unknown\";\n"
              + "     :long_name = \"platform heading\";\n"
              + "     :missing_value = -9999.0f; // float\n"
              + "     :observation_type = \"calculated\";\n"
              + "     :original_units = \"degrees (clockwise towards true north)\";\n"
              + "     :qcindex = 4; // int\n"
              + "     :sampling_rate = 1.0f; // float\n"
              + "     :special_value = -8888.0f; // float\n"
              + "     :units = \"degrees (clockwise towards true north)\";\n"
              + "\n"
              + " :contact_email = \"samos@coaps.fsu.edu\";\n"
              + " :contact_info = \"Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840, USA\";\n"
              + " :Cruise_id = \"Cruise_id undefined for now\";\n"
              + " :Data_modification_date = \"02/07/2012 10:03:37 EST\";\n"
              + " :data_provider = \"Timothy Salisbury\";\n"
              + " :elev = 0S; // short\n"
              + " :end_date_time = \"2012/01/28 -- 23:59  UTC\";\n"
              + " :EXPOCODE = \"EXPOCODE undefined for now\";\n"
              + " :facility = \"NOAA\";\n"
              + " :fsu_version = \"300\";\n"
              + " :ID = \"WTEP\";\n"
              + " :IMO = \"009270335\";\n"
              + " :Metadata_modification_date = \"02/07/2012 10:03:37 EST\";\n"
              + " :platform = \"SCS\";\n"
              + " :platform_version = \"4.0\";\n"
              + " :receipt_order = \"01\";\n"
              + " :site = \"OSCAR DYSON\";\n"
              + " :start_date_time = \"2012/01/28 -- 21:36  UTC\";\n"
              + " :title = \"OSCAR DYSON Meteorological Data\";\n"
              + " data:\n"
              + "time =\n"
              + "  {16870896, 16870906, 16870916, 16870926, 16870936, 16870946, 16870956, 16870966, 16870976, 16870986}\n"
              + "lat =\n"
              + "  {44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.62, 44.61}\n"
              + "lon =\n"
              + "  {235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.94, 235.91}\n"
              + "PL_HD =\n"
              + "  {75.53, 75.72, 76.65, 76.43, 76.58, 63.34, 266.49, 246.52, 220.81, 242.11}\n"
              + "}\n";
      /*
       * from
       * https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/
       * WTEP_20120128v30001.nc.ascii?time[0:10:99],lat[0:10:99],lon[0:10:99],PL_HD[0:
       * 10:99]
       * time[10] 16870896, 16870906, 16870916, 16870926, 16870936, 16870946,
       * 16870956, 16870966, 16870976, 16870986
       * lat[10] 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.63, 44.62, 44.61
       * lon[10] 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95, 235.95,
       * 235.94, 235.91
       * PL_HD[10] 75.53, 75.72, 76.65, 76.43, 76.58, 63.34, 266.49, 246.52, 220.81,
       * 242.11
       */
      Test.ensureEqual(results, expected, "results=" + results);
      File2.delete(fileName);
      if (true) throw new RuntimeException("shouldn't get here");
    } catch (OutOfMemoryError oome) {
      Test.knownProblem(
          "THREDDS OutOfMemoryError. I reported it to John Caron.",
          "2012-03-02 A TDS problem. I reported it to John Caron:\n"
              + MustBe.throwableToString(oome));
      // OpendapHelper.getPrimitiveArrays ?flag[0:10:99]
      // Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
      // at
      // dods.dap.BaseTypePrimitiveVector.setLength(BaseTypePrimitiveVector.java:69)
      // at dods.dap.DVector.deserialize(DVector.java:221)
      // at dods.dap.DataDDS.readData(DataDDS.java:75)
      // at dods.dap.DConnect.getDataFromUrl(DConnect.java:523)
      // at dods.dap.DConnect.getData(DConnect.java:450)
      // at dods.dap.DConnect.getData(DConnect.java:633)
      // at
      // gov.noaa.pfel.coastwatch.griddata.OpendapHelper.getPrimitiveArrays(OpendapHelper.java:458)
      // at
      // gov.noaa.pfel.coastwatch.griddata.OpendapHelper.dapToNc(OpendapHelper.java:1398)
      // at
      // gov.noaa.pfel.coastwatch.griddata.OpendapHelper.testDapToNcDArray(OpendapHelper.java:1628)
      // at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:723)
    } catch (Throwable t) {
      Test.knownProblem(
          "\nOutOfMememoryError from TDS bug was expected (but 404 Not Found/ 'Connection cannont be read' is also common)."
              + "\n(server timed out 2013-10-24)",
          t);
    }

    // test DArray error caused by history having different dimensions
    String2.log("\n*** test DArray error cause by history having different dimensions");
    try {
      OpendapHelper.dapToNc(
          dArrayUrl,
          new String[] {"zztop", "time", "lat", "lon", "PL_HD", "history"},
          null, // projection
          fileName,
          false); // jplMode
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      results = t.toString();
      expected =
          "java.lang.RuntimeException: ERROR in OpendapHelper.dapToNc\n"
              + "  url=https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc\n"
              + "  varNames=zztop,time,lat,lon,PL_HD,history  projection=null\n"
              + "  file=C:/programs/_tomcat/webapps/cwexperimental/WEB-INF/temp/testDapToNcDArraySubset.nc\n"
              + "var=history has different dimensions than previous vars.";
      if (results.indexOf("java.net.ConnectException: Connection timed out: connect") >= 0)
        String2.pressEnterToContinue(
            MustBe.throwableToString(t)
                + "\nurl="
                + dArrayUrl
                + "\n(The server timed out 2013-10-24.)");
      else if (results.startsWith("dods.dap.DODSException: Connection cannot be opened"))
        String2.pressEnterToContinue(
            MustBe.throwableToString(t)
                + "\nurl="
                + dArrayUrl
                + "\n(The connection can't be opened 2019-11-25.)");
      else Test.ensureEqual(results, expected, "results=" + results);
    }

    String2.log("\n*** OpendapHelper.testDapToNcDArray finished.");
  }

  /** This tests parseStartStrideStop and throws exception if trouble. */
  @org.junit.jupiter.api.Test
  void testParseStartStrideStop() {

    Test.ensureEqual(String2.toCSSVString(OpendapHelper.parseStartStrideStop(null)), "", "");
    Test.ensureEqual(String2.toCSSVString(OpendapHelper.parseStartStrideStop("")), "", "");
    Test.ensureEqual(
        String2.toCSSVString(OpendapHelper.parseStartStrideStop("[6:7:8]")), "6, 7, 8", "");
    Test.ensureEqual(
        String2.toCSSVString(OpendapHelper.parseStartStrideStop("[5][3:4][6:7:8]")),
        "5, 1, 5, 3, 1, 4, 6, 7, 8",
        "");
    try {
      OpendapHelper.parseStartStrideStop("a");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"a\": '[' expected at projection position #0",
          "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[\": End ']' not found.",
          "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[5");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[5\": End ']' not found.",
          "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[5:t]");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(), "java.lang.NumberFormatException: For input string: \"t\"", "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[-1]");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[-1]\": Negative number=-1 at projection position #1",
          "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[0:1:2:3]");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(), "java.lang.NumberFormatException: For input string: \"2:3\"", "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[4:3]");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[4:3]\": start=4 must be less than or equal to stop=3",
          "");
    }
    try {
      OpendapHelper.parseStartStrideStop("[4:2:3]");
      Test.ensureEqual(0, 1, "");
    } catch (Throwable t) {
      Test.ensureEqual(
          t.toString(),
          "java.lang.RuntimeException: ERROR parsing OPENDAP constraint=\"[4:2:3]\": start=4 must be less than or equal to stop=3",
          "");
    }

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

  /**
   * Test allDapToNc.
   *
   * @param whichTests -1 for all, or 0.. for specific ones
   */
  @org.junit.jupiter.api.Test
  @TagThredds
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
    results = NcHelper.dds(dir + fileName);
    // String2.log(results);
    // expected = "zztop";
    // Test.ensureEqual(results, expected, "");

    // this tests numeric and String scalars, and numeric 1D arrays
    fileName = "timeSeriesBodegaMarineLabBuoy.nc";
    url = tdsUrl + "timeSeries/BodegaMarineLabBuoy.nc";
    OpendapHelper.allDapToNc(url, dir + fileName);
    results = NcHelper.dds(dir + fileName);
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
    results = NcHelper.dds(dir + fileName);
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
    results = NcHelper.dds(dir + fileName);
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
    results = NcHelper.dds(dir + fileName);
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
