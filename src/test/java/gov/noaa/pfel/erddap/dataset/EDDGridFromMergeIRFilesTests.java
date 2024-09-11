package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridFromMergeIRFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This tests generateDatasetsXml. */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {

    String2.log("\n*** EDDGridFromMergeIRFiles.testGenerateDatasetsXml");

    String dataDir =
        Path.of(EDDGridFromMergeIRFilesTests.class.getResource("/largeFiles/mergeIR/").toURI())
            .toString();
    String results =
        EDDGridFromMergeIRFiles.generateDatasetsXml(
                dataDir, "merg_[0-9]{10}_4km-pixel\\.gz", -1, "")
            + "\n"; // cacheFromUrl

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDGridFromMergeIRFiles",
                  dataDir,
                  "merg_[0-9]{10}_4km-pixel\\.gz",
                  "-1",
                  ""
                }, // default reloadEvery, cacheFromUrl
                false); // doIt loop?
    Test.ensureEqual(
        gdxResults,
        results,
        "Unexpected results from GenerateDatasetsXml.doIt. "
            + gdxResults.length()
            + " "
            + results.length());

    String expected =
        "<dataset type=\"EDDGridFromMergeIRFiles\" datasetID=\"mergeIR\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + File2.addSlash(dataDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>merg_[0-9]{10}_4km-pixel\\.gz</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_name\">Bob Joyce</att>\n"
            + "        <att name=\"creator_email\">robert.joyce@noaa.gov</att>\n"
            + "        <att name=\"creator_url\">https://www.cpc.ncep.noaa.gov/</att>\n"
            + "        <att name=\"drawLandMask\">under</att>\n"
            + "        <att name=\"infoUrl\">https://www.cpc.ncep.noaa.gov/products/global_precip/html/README</att>\n"
            + "        <att name=\"institution\">NOAA NWS NCEP CPC</att>\n"
            + "        <att name=\"keywords\">4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"summary\">The Climate Prediction Center/NCEP/NWS is now making available\n"
            + "globally-merged (60N-60S) pixel-resolution IR brightness\n"
            + "temperature data (equivalent blackbody temps), merged from all\n"
            + "available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n"
            + "GMS).  The availability of data from METEOSAT-7, which is\n"
            + "located at 57E at the present time, yields a unique opportunity\n"
            + "for total global (60N-60S) coverage.</att>\n"
            + "        <att name=\"title\">NCEP/CPC 4km Global (60N - 60S) IR Dataset</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"delta_t\">0000-00-00 00:30:00</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>latitude</sourceName>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>longitude</sourceName>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ir</sourceName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"int\">170</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"int\">330</att>\n"
            + "            <att name=\"ioos_cateory\">Heat Flux</att>\n"
            + "            <att name=\"long_name\">IR Brightness Temperature</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">"
            + EDDGridFromMergeIRFiles.IR_MV
            + "</att>\n"
            + "            <att name=\"standard_name\">brightness_temperature</att>\n"
            + "            <att name=\"units\">degreeK</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>flux</sourceName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">500.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_cateory\">Heat Flux</att>\n"
            + "            <att name=\"long_name\">Flux</att>\n"
            + "            <att name=\"missing_value\" type=\"double\">"
            + EDDGridFromMergeIRFiles.FLUX_MV
            + "</att>\n"
            + "            <att name=\"standard_name\">surface_upwelling_shortwave_flux</att>\n"
            + "            <att name=\"units\">W/m^2</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(
        results,
        expected,
        "results.length="
            + results.length()
            + " expected.length="
            + expected.length()
            + "\nresults=\n"
            + results);

    // ensure it is ready-to-use by making a dataset from it
    String tDatasetID = "mergeIR";
    // EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDGridFromMergeIRFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "datasetID");
    Test.ensureEqual(edd.className(), "EDDGridFromMergeIRFiles", "className");
    Test.ensureEqual(edd.title(), "NCEP/CPC 4km Global (60N - 60S) IR Dataset", "title");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "ir, flux",
        "dataVariableDestinationNames");

    String2.log("\nEDDGridFromMergeIRFiles.testGenerateDatasetsXml passed the test.");
  }

  /** This tests this class. */
  @org.junit.jupiter.api.Test
  void testMergeIR() throws Throwable {
    // String2.log("\n*** EDDGridFromMergeIRFiles.testMergeIRgz\n");
    // testVerboseOn();
    int language = 0;
    // String2.log(NcHelper.ncdump(String2.unitTestDataDir +
    // "mergeIR/merg_20150101_4km-pixel", "-h"));
    EDDGridFromMergeIRFiles.deleteCachedDatasetInfo("mergeIR");
    EDDGridFromMergeIRFiles.deleteCachedDatasetInfo("mergeIRZ");
    EDDGridFromMergeIRFiles.deleteCachedDatasetInfo("mergeIRgz");
    EDDGrid edd = (EDDGrid) EDDTestDataset.getmergeIR();
    EDDGrid eddZ = (EDDGrid) EDDTestDataset.getmergeIRZ();
    EDDGrid eddgz = (EDDGrid) EDDTestDataset.getmergeIRgz();
    String dir = EDStatic.fullTestCacheDirectory;
    String tName, results, expected, dapQuery;
    int po;

    // .dds
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 4];\n"
            + "  Float32 latitude[latitude = 3298];\n"
            + "  Float32 longitude[longitude = 9896];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int16 ir[time = 4][latitude = 3298][longitude = 9896];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 4];\n"
            + "      Float32 latitude[latitude = 3298];\n"
            + "      Float32 longitude[longitude = 9896];\n"
            + "  } ir;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 flux[time = 4][latitude = 3298][longitude = 9896];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 4];\n"
            + "      Float32 latitude[latitude = 3298];\n"
            + "      Float32 longitude[longitude = 9896];\n"
            + "  } flux;\n"
            + "} mergeIR;\n";
    // uncompressed
    tName =
        edd.makeNewFileForDapQuery(language, null, null, "", dir, edd.className() + "_", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Z
    expected = String2.replaceAll(expected, "mergeIR;", "mergeIRZ;");
    tName =
        eddZ.makeNewFileForDapQuery(language, null, null, "", dir, eddZ.className() + "_Z", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "Z results=\n" + results);

    // gz
    expected = String2.replaceAll(expected, "mergeIRZ;", "mergeIRgz;");
    tName =
        eddgz.makeNewFileForDapQuery(
            language, null, null, "", dir, eddgz.className() + "_gz", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "gz results=\n" + results);

    // *** .das
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.4200704e+9, 1.4200758e+9;\n"
            + "    String axis \"T\";\n"
            + "    String delta_t \"0000-00-00 00:30:00\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range -59.982, 59.97713;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range 0.0182, 359.9695;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  ir {\n"
            + "    Int32 colorBarMaximum 330;\n"
            + "    Int32 colorBarMinimum 170;\n"
            + "    String ioos_category \"Heat Flux\";\n"
            + "    String long_name \"IR Brightness Temperature\";\n"
            + "    Int16 missing_value "
            + EDDGridFromMergeIRFiles.IR_MV
            + ";\n"
            + "    String standard_name \"brightness_temperature\";\n"
            + "    String units \"degreeK\";\n"
            + "  }\n"
            + "  flux {\n"
            + "    Float64 colorBarMaximum 500.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Heat Flux\";\n"
            + "    String long_name \"Flux\";\n"
            + "    Float64 missing_value "
            + EDDGridFromMergeIRFiles.FLUX_MV
            + ";\n"
            + "    String standard_name \"surface_upwelling_shortwave_flux\";\n"
            + "    String units \"W/m^2\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"robert.joyce@noaa.gov\";\n"
            + "    String creator_name \"Bob Joyce\";\n"
            + "    String creator_type \"person\";\n"
            + "    String creator_url \"https://www.cpc.ncep.noaa.gov/\";\n"
            + "    Float64 Easternmost_Easting 359.9695;\n"
            + "    Float64 geospatial_lat_max 59.97713;\n"
            + "    Float64 geospatial_lat_min -59.982;\n"
            + "    Float64 geospatial_lat_resolution 0.03638432817713073;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 359.9695;\n"
            + "    Float64 geospatial_lon_min 0.0182;\n"
            + "    Float64 geospatial_lon_resolution 0.03637708943911066;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"";

    // 2015-03-20T17:28:57Z (local files)\n" +
    // "2015-03-20T17:28:57Z
    String expected2 =
        "/erddap/griddap/mergeIR.das\";\n"
            + "    String infoUrl \"https://www.cpc.ncep.noaa.gov/products/global_precip/html/README\";\n"
            + "    String institution \"NOAA NWS NCEP CPC\";\n"
            + "    String keywords \"4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 59.97713;\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing -59.982;\n"
            + "    String summary \"The Climate Prediction Center/NCEP/NWS is now making available\n"
            + "globally-merged (60N-60S) pixel-resolution IR brightness\n"
            + "temperature data (equivalent blackbody temps), merged from all\n"
            + "available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n"
            + "GMS).  The availability of data from METEOSAT-7, which is\n"
            + "located at 57E at the present time, yields a unique opportunity\n"
            + "for total global (60N-60S) coverage.\";\n"
            + "    String time_coverage_end \"2015-01-01T01:30:00Z\";\n"
            + "    String time_coverage_start \"2015-01-01T00:00:00Z\";\n"
            + "    String title \"NCEP/CPC 4km Global (60N - 60S) IR Dataset\";\n"
            + "    Float64 Westernmost_Easting 0.0182;\n"
            + "  }\n"
            + "}\n";
    // uncompressed
    tName =
        edd.makeNewFileForDapQuery(language, null, null, "", dir, edd.className() + "_", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    po = results.indexOf(expected2.substring(0, 20));
    Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

    // Z
    expected2 = String2.replaceAll(expected2, "mergeIR.das", "mergeIRZ.das");
    tName =
        eddZ.makeNewFileForDapQuery(language, null, null, "", dir, eddZ.className() + "_Z", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results.substring(0, expected.length()), expected, "Z results=\n" + results);

    po = results.indexOf(expected2.substring(0, 20));
    Test.ensureEqual(results.substring(po), expected2, "Z results=\n" + results);

    // gz
    expected2 = String2.replaceAll(expected2, "mergeIRZ.das", "mergeIRgz.das");
    tName =
        eddgz.makeNewFileForDapQuery(
            language, null, null, "", dir, eddgz.className() + "_gz", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results.substring(0, expected.length()), expected, "gz results=\n" + results);

    po = results.indexOf(expected2.substring(0, 20));
    Test.ensureEqual(results.substring(po), expected2, "gz results=\n" + results);

    // *** data
    dapQuery = "ir[0][0:1000:3200][0:1000:9800],flux[0][0:1000:3200][0:1000:9800]";
    expected =
        "time,latitude,longitude,ir,flux\n"
            + "UTC,degrees_north,degrees_east,degreeK,W/m^2\n"
            + "2015-01-01T00:00:00Z,-59.982,0.0182,237,154.4\n"
            + "2015-01-01T00:00:00Z,-59.982,36.396515,273,236.1\n"
            + "2015-01-01T00:00:00Z,-59.982,72.77347,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,109.15042,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,145.52737,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,181.90433,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,218.28128,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,254.65823,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,291.0352,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-59.982,327.41214,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-23.597416,0.0182,271,230.4\n"
            + "2015-01-01T00:00:00Z,-23.597416,36.396515,296,312.5\n"
            + "2015-01-01T00:00:00Z,-23.597416,72.77347,285,273.3\n"
            + "2015-01-01T00:00:00Z,-23.597416,109.15042,283,266.7\n"
            + "2015-01-01T00:00:00Z,-23.597416,145.52737,NaN,NaN\n"
            + "2015-01-01T00:00:00Z,-23.597416,181.90433,293,301.3\n"
            + "2015-01-01T00:00:00Z,-23.597416,218.28128,294,305.0\n"
            + "2015-01-01T00:00:00Z,-23.597416,254.65823,242,163.4\n"
            + "2015-01-01T00:00:00Z,-23.597416,291.0352,294,305.0\n"
            + "2015-01-01T00:00:00Z,-23.597416,327.41214,284,270.0\n"
            + "2015-01-01T00:00:00Z,12.786415,0.0182,283,266.7\n"
            + "2015-01-01T00:00:00Z,12.786415,36.396515,208,114.2\n"
            + "2015-01-01T00:00:00Z,12.786415,72.77347,290,290.5\n"
            + "2015-01-01T00:00:00Z,12.786415,109.15042,295,308.7\n"
            + "2015-01-01T00:00:00Z,12.786415,145.52737,294,305.0\n"
            + "2015-01-01T00:00:00Z,12.786415,181.90433,282,263.5\n"
            + "2015-01-01T00:00:00Z,12.786415,218.28128,296,312.5\n"
            + "2015-01-01T00:00:00Z,12.786415,254.65823,288,283.5\n"
            + "2015-01-01T00:00:00Z,12.786415,291.0352,245,169.1\n"
            + "2015-01-01T00:00:00Z,12.786415,327.41214,264,211.7\n"
            + "2015-01-01T00:00:00Z,49.170914,0.0182,277,247.9\n"
            + "2015-01-01T00:00:00Z,49.170914,36.396515,269,224.9\n"
            + "2015-01-01T00:00:00Z,49.170914,72.77347,276,244.9\n"
            + "2015-01-01T00:00:00Z,49.170914,109.15042,275,241.9\n"
            + "2015-01-01T00:00:00Z,49.170914,145.52737,279,254.0\n"
            + "2015-01-01T00:00:00Z,49.170914,181.90433,264,211.7\n"
            + "2015-01-01T00:00:00Z,49.170914,218.28128,262,206.7\n"
            + "2015-01-01T00:00:00Z,49.170914,254.65823,281,260.3\n"
            + "2015-01-01T00:00:00Z,49.170914,291.0352,282,263.5\n"
            + "2015-01-01T00:00:00Z,49.170914,327.41214,250,179.3\n";
    // uncompressed
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, edd.className() + "_", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Z
    tName =
        eddZ.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, eddZ.className() + "_Z", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "Z results=\n" + results);

    // gz
    tName =
        eddgz.makeNewFileForDapQuery(
            language, null, null, dapQuery, dir, eddgz.className() + "_gz", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    Test.ensureEqual(results, expected, "gz results=\n" + results);

    String2.log("\n*** EDDGridFromMergeIRFiles.testMergeIR() finished successfully");
  }
}
