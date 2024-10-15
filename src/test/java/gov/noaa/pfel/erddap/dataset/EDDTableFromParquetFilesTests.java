package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromParquetFilesTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {

    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromParquetFilesTests.class.getResource("/data/parquet/").toURI())
                .toString());
    String fileNameRegex = ".*\\.parquet";
    String results =
        EDDTableFromParquetFiles.generateDatasetsXml(
                dataDir,
                fileNameRegex,
                "",
                1440,
                "",
                "",
                "",
                "",
                "Year",
                "",
                "",
                "",
                "",
                -1,
                null, // defaultStandardizeWhat
                null)
            + "\n";

    String2.log(results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromParquetFiles",
                  dataDir,
                  fileNameRegex,
                  "",
                  "1440",
                  "",
                  "",
                  "",
                  "",
                  "Year",
                  "",
                  "",
                  "",
                  "",
                  "-1",
                  ""
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");
    String tDatasetID =
        EDDTableFromParquetFiles.suggestDatasetID(
            dataDir + String2.replaceAll(fileNameRegex, '\\', '|') + "EDDTableFromParquetFiles");
    String expected =
        "<!-- NOTE! Since Parquet files have no metadata, you MUST edit the chunk\n"
            + "  of datasets.xml below to add all of the metadata (especially \"units\"). -->\n"
            + "<dataset type=\"EDDTableFromParquetFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + dataDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + fileNameRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <sortFilesBySourceNames>Year</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"infoUrl\">???</att>\n"
            + "        <att name=\"institution\">???</att>\n"
            + "        <att name=\"keywords\">amount, array, array-data, attribution, AttributionSources, class, collection, comprehensive, consumed, ConsumedBySectorType, context, correlation, data, DataCollection, DataReliability, distribution, DistributionType, flow, flowable, FlowAmount, FlowType, FlowUUID, geographical, GeographicalCorrelation, large, local, Location, LocationSystem, max, measureof, MeasureofSpread, meta, MetaSources, min, name, produced, ProducedBySectorType, quality, reliability, sector, SectorConsumedBy, SectorProducedBy, SectorSourceName, source, SourceName, sources, spread, stewardship, system, technological, TechnologicalCorrelation, temporal, TemporalCorrelation, time, type, unit, uuid, year</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">Class, SectorConsumedBy, SectorSourceName, Context, LocationSystem, FlowType, Year, MeasureofSpread, Spread, DistributionType, Min, Max, DataReliability, TemporalCorrelation, GeographicalCorrelation, TechnologicalCorrelation, DataCollection, ProducedBySectorType, ConsumedBySectorType</att>\n"
            + "        <att name=\"summary\">Data from a local source.</att>\n"
            + "        <att name=\"title\">Data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Flowable</sourceName>\n"
            + "        <destinationName>Flowable</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Flowable</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Class</sourceName>\n"
            + "        <destinationName>Class</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Class</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SectorProducedBy</sourceName>\n"
            + "        <destinationName>SectorProducedBy</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Sector Produced By</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SectorConsumedBy</sourceName>\n"
            + "        <destinationName>SectorConsumedBy</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Sector Consumed By</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SectorSourceName</sourceName>\n"
            + "        <destinationName>SectorSourceName</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Sector Source Name</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Context</sourceName>\n"
            + "        <destinationName>Context</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Context</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Location</sourceName>\n"
            + "        <destinationName>Location</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LocationSystem</sourceName>\n"
            + "        <destinationName>LocationSystem</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Location System</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FlowAmount</sourceName>\n"
            + "        <destinationName>FlowAmount</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Flow Amount</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Unit</sourceName>\n"
            + "        <destinationName>Unit</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Unit</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FlowType</sourceName>\n"
            + "        <destinationName>FlowType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Flow Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Year</sourceName>\n"
            + "        <destinationName>Year</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Year</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>MeasureofSpread</sourceName>\n"
            + "        <destinationName>MeasureofSpread</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Measureof Spread</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Spread</sourceName>\n"
            + "        <destinationName>Spread</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Spread</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DistributionType</sourceName>\n"
            + "        <destinationName>DistributionType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Distribution Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Min</sourceName>\n"
            + "        <destinationName>Min</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Min</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Max</sourceName>\n"
            + "        <destinationName>Max</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Max</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DataReliability</sourceName>\n"
            + "        <destinationName>DataReliability</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Data Reliability</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TemporalCorrelation</sourceName>\n"
            + "        <destinationName>TemporalCorrelation</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Temporal Correlation</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>GeographicalCorrelation</sourceName>\n"
            + "        <destinationName>GeographicalCorrelation</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Geographical Correlation</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TechnologicalCorrelation</sourceName>\n"
            + "        <destinationName>TechnologicalCorrelation</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Technological Correlation</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DataCollection</sourceName>\n"
            + "        <destinationName>DataCollection</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Data Collection</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>MetaSources</sourceName>\n"
            + "        <destinationName>MetaSources</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Meta Sources</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FlowUUID</sourceName>\n"
            + "        <destinationName>FlowUUID</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Flow UUID</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ProducedBySectorType</sourceName>\n"
            + "        <destinationName>ProducedBySectorType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Produced By Sector Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ConsumedBySectorType</sourceName>\n"
            + "        <destinationName>ConsumedBySectorType</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Consumed By Sector Type</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>AttributionSources</sourceName>\n"
            + "        <destinationName>AttributionSources</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Attribution Sources</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SourceName</sourceName>\n"
            + "        <destinationName>SourceName</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Source Name</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This does basic tests of this class.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
    String tName, results, tResults, expected, userDapQuery;
    String dir = EDStatic.fullTestCacheDirectory;
    int language = 0;

    String id = "testParquet";
    if (deleteCachedDatasetInfo) EDDTableFromParquetFiles.deleteCachedDatasetInfo(id);

    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestParquet();

    // *** test getting das for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  Flowable {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Flowable\";\n"
            + "  }\n"
            + "  Class {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Class\";\n"
            + "  }\n"
            + "  SectorProducedBy {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Sector Produced By\";\n"
            + "  }\n"
            + "  SectorConsumedBy {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Sector Consumed By\";\n"
            + "  }\n"
            + "  SectorSourceName {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Sector Source Name\";\n"
            + "  }\n"
            + "  Context {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Context\";\n"
            + "  }\n"
            + "  Location {\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Location\";\n"
            + "  }\n"
            + "  LocationSystem {\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Location System\";\n"
            + "  }\n"
            + "  FlowAmount {\n"
            + "    Float64 actual_range 2.1096225766143105e-5, 1.7147320234846729e+12;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Flow Amount\";\n"
            + "  }\n"
            + "  Unit {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Unit\";\n"
            + "  }\n"
            + "  FlowType {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Flow Type\";\n"
            + "  }\n"
            + "  Year {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Int16 actual_range 2012, 2020;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Year\";\n"
            + "  }\n"
            + "  MeasureofSpread {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Measureof Spread\";\n"
            + "  }\n"
            + "  Spread {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Spread\";\n"
            + "  }\n"
            + "  DistributionType {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Distribution Type\";\n"
            + "  }\n"
            + "  Min {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Min\";\n"
            + "  }\n"
            + "  Max {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Max\";\n"
            + "  }\n"
            + "  DataReliability {\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Data Reliability\";\n"
            + "  }\n"
            + "  TemporalCorrelation {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Temporal Correlation\";\n"
            + "  }\n"
            + "  GeographicalCorrelation {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Geographical Correlation\";\n"
            + "  }\n"
            + "  TechnologicalCorrelation {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Technological Correlation\";\n"
            + "  }\n"
            + "  DataCollection {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data Collection\";\n"
            + "  }\n"
            + "  MetaSources {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Meta Sources\";\n"
            + "  }\n"
            + "  FlowUUID {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Flow UUID\";\n"
            + "  }\n"
            + "  ProducedBySectorType {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Produced By Sector Type\";\n"
            + "  }\n"
            + "  ConsumedBySectorType {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Consumed By Sector Type\";\n"
            + "  }\n"
            + "  AttributionSources {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Attribution Sources\";\n"
            + "  }\n"
            + "  SourceName {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Source Name\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Other\";\n"
            + "    String Conventions \"COARDS, CF-1.10, ACDD-1.3\";\n"
            + "    String history";

    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "    String infoUrl \"???\";\n"
            + "    String institution \"???\";\n"
            + "    String keywords \"amount, array, array-data, attribution, AttributionSources, class, collection, comprehensive, consumed, ConsumedBySectorType, context, correlation, data, DataCollection, DataReliability, distribution, DistributionType, flow, flowable, FlowAmount, FlowType, FlowUUID, geographical, GeographicalCorrelation, large, local, Location, LocationSystem, max, measureof, MeasureofSpread, meta, MetaSources, min, name, produced, ProducedBySectorType, quality, reliability, sector, SectorConsumedBy, SectorProducedBy, SectorSourceName, source, SourceName, sources, spread, stewardship, system, technological, TechnologicalCorrelation, temporal, TemporalCorrelation, time, type, unit, uuid, year\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"Class, SectorConsumedBy, SectorSourceName, Context, LocationSystem, FlowType, Year, MeasureofSpread, Spread, DistributionType, Min, Max, DataReliability, TemporalCorrelation, GeographicalCorrelation, TechnologicalCorrelation, DataCollection, ProducedBySectorType, ConsumedBySectorType\";\n"
            + "    String summary \"Data from a local source.\";\n"
            + "    String title \"Data from a local source.\";\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String Flowable;\n"
            + "    String Class;\n"
            + "    String SectorProducedBy;\n"
            + "    String SectorConsumedBy;\n"
            + "    String SectorSourceName;\n"
            + "    String Context;\n"
            + "    String Location;\n"
            + "    String LocationSystem;\n"
            + "    Float64 FlowAmount;\n"
            + "    String Unit;\n"
            + "    String FlowType;\n"
            + "    Int16 Year;\n"
            + "    String MeasureofSpread;\n"
            + "    String Spread;\n"
            + "    String DistributionType;\n"
            + "    String Min;\n"
            + "    String Max;\n"
            + "    String DataReliability;\n"
            + "    String TemporalCorrelation;\n"
            + "    String GeographicalCorrelation;\n"
            + "    String TechnologicalCorrelation;\n"
            + "    String DataCollection;\n"
            + "    String MetaSources;\n"
            + "    String FlowUUID;\n"
            + "    String ProducedBySectorType;\n"
            + "    String ConsumedBySectorType;\n"
            + "    String AttributionSources;\n"
            + "    String SourceName;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv all data
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Flowable,Class,SectorProducedBy,SectorConsumedBy,SectorSourceName,Context,Location,LocationSystem,FlowAmount,Unit,FlowType,Year,MeasureofSpread,Spread,DistributionType,Min,Max,DataReliability,TemporalCorrelation,GeographicalCorrelation,TechnologicalCorrelation,DataCollection,MetaSources,FlowUUID,ProducedBySectorType,ConsumedBySectorType,AttributionSources,SourceName\n"
            + ",,,,,,,,,,,,,,,,,,,,,,,,,,,\n"
            + "Carbon dioxide,Chemicals,311,,NAICS_2012_Code,emission/air,00000,FIPS,8.117607800971414E7,kg,ELEMENTARY_FLOW,2012,,,,,,,,,,,EPA_GHGI_T_2_1.carbonate_use,b6f010fb-a764-3063-af2d-bcb8309a97b7,,,BEA_Detail_Use_PRO_BeforeRedef,\n"
            + "Carbon dioxide,Chemicals,313,,NAICS_2012_Code,emission/air,00000,FIPS,3.497831003670609E7,kg,ELEMENTARY_FLOW,2012,,,,,,,,,,,EPA_GHGI_T_2_1.carbonate_use,b6f010fb-a764-3063-af2d-bcb8309a97b7,,,BEA_Detail_Use_PRO_BeforeRedef,\n"
            + "Carbon dioxide,Chemicals,316,,NAICS_2012_Code,emission/air,00000,FIPS,329984.05695005745,kg,ELEMENTARY_FLOW,2012,,,,,,,,,,,EPA_GHGI_T_2_1.carbonate_use,b6f010fb-a764-3063-af2d-bcb8309a97b7,,,BEA_Detail_Use_PRO_BeforeRedef,\n"
            + "Carbon dioxide,Chemicals,321,,NAICS_2012_Code,emission/air,00000,FIPS,1.102146750213192E8,kg,ELEMENTARY_FLOW,2012,,,,,,,,,,,EPA_GHGI_T_2_1.carbonate_use,b6f010fb-a764-3063-af2d-bcb8309a97b7,,,BEA_Detail_Use_PRO_BeforeRedef,\n";

    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "\nresults=\n" + results.substring(0, expected.length()));

    // .csv subset
    userDapQuery = "Year,Flowable,FlowAmount&Flowable=\"Methane\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1Flowable", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Year,Flowable,FlowAmount\n"
            + ",,\n"
            + "2012,Methane,6.08E8\n"
            + "2012,Methane,2.72E8\n"
            + "2012,Methane,2.896E9\n"
            + "2012,Methane,7.72E8\n"
            + "2012,Methane,4.744E9\n"
            + "2012,Methane,484848.48484848486\n"
            + "2012,Methane,2969696.9696969697\n"
            + "2012,Methane,2969696.9696969697\n"
            + "2012,Methane,1575757.5757575757\n"
            + "2012,Methane,1562691.131498471\n"
            + "2012,Methane,3951070.3363914373\n"
            + "2012,Methane,2486238.5321100918\n"
            + "2012,Methane,4000000.0\n"
            + "2012,Methane,4000000.0\n"
            + "2012,Methane,1.6E7\n"
            + "2012,Methane,5494620.529223612\n"
            + "2012,Methane,2307647.5719685955\n"
            + "2012,Methane,104681.59348647862\n"
            + "2012,Methane,93050.30532131433\n"
            + "2012,Methane,3315580.190384384\n"
            + "2012,Methane,3429087.841908664\n"
            + "2012,Methane,3429087.841908664\n"
            + "2012,Methane,1.1272683455838054E7\n"
            + "2012,Methane,3506928.545607905\n"
            + "2012,Methane,899385.4681286903\n"
            + "2012,Methane,1489336.0645860948\n"
            + "2012,Methane,657910.5916375468\n"
            + "2012,Methane,221311.91046428957\n"
            + "2012,Methane,325445.80425662186\n"
            + "2012,Methane,1143807.5611282391\n"
            + "2012,Methane,4.630943472415085E7\n"
            + "2012,Methane,1.764E9\n"
            + "2012,Methane,3.2E7\n"
            + "2012,Methane,8000000.0\n"
            + "2012,Methane,4.312E9\n"
            + "2012,Methane,9.68E8\n"
            + "2012,Methane,1.34E9\n"
            + "2012,Methane,1148040.1684483318\n"
            + "2012,Methane,72562.358276644\n"
            + "2012,Methane,62196.307094266274\n"
            + "2012,Methane,10366.051182377712\n"
            + "2012,Methane,12957.56397797214\n"
            + "2012,Methane,1513443.4726271462\n"
            + "2012,Methane,85519.92225461613\n"
            + "2012,Methane,2456754.130223518\n"
            + "2012,Methane,2436022.0278587625\n"
            + "2012,Methane,189180.43407839327\n"
            + "2012,Methane,5923.457818501551\n"
            + "2012,Methane,740.4322273126938\n"
            + "2012,Methane,740.4322273126938\n"
            + "2012,Methane,740.4322273126938\n"
            + "2012,Methane,740.4322273126938\n"
            + "2012,Methane,740.4322273126938\n"
            + "2012,Methane,740.4322273126938\n"
            + "2012,Methane,2591.512795594428\n"
            + "2012,Methane,3.650042136765904E7\n"
            + "2012,Methane,1371048.6568759966\n"
            + "2012,Methane,6128529.975464964\n"
            + "2012,Methane,490509.1895149141\n"
            + "2012,Methane,261147.93612533895\n"
            + "2012,Methane,25158.180174751433\n"
            + "2012,Methane,27041.27749322085\n"
            + "2012,Methane,4519.433564326604\n"
            + "2012,Methane,317490.20789394394\n"
            + "2012,Methane,481771.61795721605\n"
            + "2012,Methane,236667.67098523653\n"
            + "2012,Methane,1883.0973184694185\n"
            + "2012,Methane,7638.99092925825\n"
            + "2012,Methane,545231.9975896354\n"
            + "2012,Methane,787247.6649593251\n"
            + "2012,Methane,754218.1379933715\n"
            + "2012,Methane,78638.14401928292\n"
            + "2012,Methane,14763.482976800242\n"
            + "2012,Methane,7381.741488400121\n"
            + "2012,Methane,3540.222958722507\n"
            + "2012,Methane,301.29557095510694\n"
            + "2012,Methane,1054.5344983428743\n"
            + "2012,Methane,45420.307321482374\n"
            + "2012,Methane,139951.79270864718\n"
            + "2012,Methane,136863.51310635734\n"
            + "2012,Methane,2228833.986140404\n"
            + "2012,Methane,1034874.9623380536\n"
            + "2012,Methane,52576.07713166616\n"
            + "2012,Methane,61991.56372401326\n"
            + "2012,Methane,47454.05242542935\n"
            + "2012,Methane,47378.72853269057\n"
            + "2012,Methane,101762.57909008737\n"
            + "2012,Methane,6176.559204579693\n"
            + "2012,Methane,8285.628201265441\n"
            + "2012,Methane,5122.024706236818\n"
            + "2012,Methane,1657.1256402530882\n"
            + "2012,Methane,9189.514914130763\n"
            + "2012,Methane,11072.61223260018\n"
            + "2012,Methane,979.2106056040976\n"
            + "2012,Methane,3088.2796022898465\n"
            + "2012,Methane,301.29557095510694\n"
            + "2012,Methane,8134.980415787888\n"
            + "2012,Methane,10319.373305212413\n"
            + "2012,Methane,117595.30371395088\n"
            + "2012,Methane,104989.08715581534\n"
            + "2012,Methane,4064.0936963541426\n"
            + "2012,Methane,22314.88483275932\n"
            + "2012,Methane,3424.3752441502497\n"
            + "2012,Methane,3424.3752441502497\n"
            + "2012,Methane,15014.56837819725\n"
            + "2012,Methane,22126.732346817\n"
            + "2012,Methane,1618.111379103964\n"
            + "2012,Methane,19831.272018320677\n"
            + "2012,Methane,3574.8972329041067\n"
            + "2012,Methane,3424.3752441502497\n"
            + "2012,Methane,5983.249052965821\n"
            + "2012,Methane,3424.3752441502497\n"
            + "2012,Methane,8353.970375839071\n"
            + "2012,Methane,12154.650591873964";
    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "\nresults=\n" + results.substring(0, expected.length()));

    // Could add additional test queries
  }
}
