package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagMissingDataset;
import testDataset.Initialization;

class EDDTableFromCassandraTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests generateDatasetsXml.
   *
   * @param version to identify Cassandra v2 or v3
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  @TagMissingDataset
  void testGenerateDatasetsXml(int version) throws Throwable {
    // String2.log("\n*** EDDTableFromCassandra.testGenerateDatasetsXml");
    // testVerboseOn();
    String url = "127.0.0.1"; // implied: v3 :9042, v2 :9160
    String props[] = {};
    String keyspace = "bobKeyspace";
    String tableName = "bobTable";
    int tReloadEveryNMinutes = -1;
    String tInfoUrl = "http://www.oceannetworks.ca/";
    String tInstitution = "Ocean Networks Canada";
    String tSummary = "The summary for Bob's great Cassandra test data.";
    String tTitle = "The Title for Bob's Cassandra Test Data";
    // addGlobalAtts.
    String results, expected;

    try {
      // get the list of keyspaces
      // Cassandra not running? see directions up top of this file
      results =
          EDDTableFromCassandra.generateDatasetsXml(
              url,
              props,
              EDDTableFromCassandra.LIST,
              "",
              tReloadEveryNMinutes,
              tInfoUrl,
              tInstitution,
              tSummary,
              tTitle,
              new Attributes());
      expected =
          "CREATE KEYSPACE bobkeyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2' } AND DURABLE_WRITES = true;\n"
              + "CREATE KEYSPACE system_traces WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2' } AND DURABLE_WRITES = true;\n"
              + "CREATE KEYSPACE system WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.LocalStrategy' } AND DURABLE_WRITES = true;\n"
              + (version == 2
                  ? ""
                  : "CREATE KEYSPACE system_distributed WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '3' } AND DURABLE_WRITES = true;\n"
                      + "CREATE KEYSPACE system_schema WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.LocalStrategy' } AND DURABLE_WRITES = true;\n"
                      + "CREATE KEYSPACE system_auth WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;\n");
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get the metadata for all tables in a keyspace
      results =
          EDDTableFromCassandra.generateDatasetsXml(
              url,
              props,
              keyspace,
              EDDTableFromCassandra.LIST,
              tReloadEveryNMinutes,
              tInfoUrl,
              tInstitution,
              tSummary,
              tTitle,
              new Attributes());
      expected =
          "CREATE KEYSPACE bobkeyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2' } AND DURABLE_WRITES = true;\n"
              + "\n"
              + "CREATE TABLE bobkeyspace.bobtable (\n"
              + "    deviceid int,\n"
              + "    date timestamp,\n"
              + "    sampletime timestamp,\n"
              + "    cascii ascii,\n"
              + "    cboolean boolean,\n"
              + "    cbyte int,\n"
              + "    cdecimal double,\n"
              + "    cdouble double,\n"
              + "    cfloat float,\n"
              + "    cint int,\n"
              + "    clong bigint,\n"
              + "    cmap map<text, double>,\n"
              + "    cset set<text>,\n"
              + "    cshort int,\n"
              + "    ctext text,\n"
              + "    cvarchar text,\n"
              + "    depth list<float>,\n"
              + "    u list<float>,\n"
              + "    v list<float>,\n"
              + "    w list<float>,\n"
              + "    PRIMARY KEY ((deviceid, date), sampletime)\n"
              + ") WITH CLUSTERING ORDER BY (sampletime ASC)\n"
              + "    AND read_repair_chance = 0.0\n"
              + "    AND dclocal_read_repair_chance = 0.1\n"
              + "    AND gc_grace_seconds = 864000\n"
              + "    AND bloom_filter_fp_chance = 0.01\n"
              + "    AND caching = { 'keys' : 'ALL', 'rows_per_partition' : 'NONE' }\n"
              + "    AND comment = ''\n"
              + "    AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold' : 32, 'min_threshold' : 4 }\n"
              + (version == 2
                  ? "    AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n"
                  : "    AND compression = { 'chunk_length_in_kb' : 64, 'class' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n")
              + "    AND default_time_to_live = 0\n"
              + (version == 2
                  ? "    AND speculative_retry = '99.0PERCENTILE'\n"
                  : "    AND speculative_retry = '99PERCENTILE'\n")
              + "    AND min_index_interval = 128\n"
              + "    AND max_index_interval = 2048\n"
              + (version == 2 ? "" : "    AND crc_check_chance = 1.0\n" + "    AND cdc = false\n")
              + "    AND memtable_flush_period_in_ms = 0;\n"
              + "\n"
              + "CREATE INDEX ctext_index ON bobkeyspace.bobtable (ctext);\n"
              + "\n"
              + "CREATE TABLE bobkeyspace.statictest (\n"
              + "    deviceid int,\n"
              + "    date timestamp,\n"
              + "    sampletime timestamp,\n"
              + "    depth list<float>,\n"
              + "    lat float static,\n"
              + "    lon float static,\n"
              + "    u list<float>,\n"
              + "    v list<float>,\n"
              + "    PRIMARY KEY ((deviceid, date), sampletime)\n"
              + ") WITH CLUSTERING ORDER BY (sampletime ASC)\n"
              + "    AND read_repair_chance = 0.0\n"
              + "    AND dclocal_read_repair_chance = 0.1\n"
              + "    AND gc_grace_seconds = 864000\n"
              + "    AND bloom_filter_fp_chance = 0.01\n"
              + "    AND caching = { 'keys' : 'ALL', 'rows_per_partition' : 'NONE' }\n"
              + "    AND comment = ''\n"
              + "    AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold' : 32, 'min_threshold' : 4 }\n"
              + (version == 2
                  ? "    AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n"
                  : "    AND compression = { 'chunk_length_in_kb' : 64, 'class' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n")
              + "    AND default_time_to_live = 0\n"
              + (version == 2
                  ? "    AND speculative_retry = '99.0PERCENTILE'\n"
                  : "    AND speculative_retry = '99PERCENTILE'\n")
              + "    AND min_index_interval = 128\n"
              + "    AND max_index_interval = 2048\n"
              + (version == 2 ? "" : "    AND crc_check_chance = 1.0\n" + "    AND cdc = false\n")
              + "    AND memtable_flush_period_in_ms = 0;\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // generate the datasets.xml for one table
      results =
          EDDTableFromCassandra.generateDatasetsXml(
              url,
              props,
              keyspace,
              tableName,
              tReloadEveryNMinutes,
              tInfoUrl,
              tInstitution,
              tSummary,
              tTitle,
              new Attributes());
      expected =
          "<!-- NOTE! Since Cassandra tables don't have any metadata, you must add metadata\n"
              + "  below, notably 'units' for each of the dataVariables. -->\n"
              + "<dataset type=\"EDDTableFromCassandra\" datasetID=\"cass_bobKeyspace_bobTable\" active=\"true\">\n"
              + "    <sourceUrl>127.0.0.1</sourceUrl>\n"
              + "    <keyspace>bobKeyspace</keyspace>\n"
              + "    <tableName>bobTable</tableName>\n"
              + "    <partitionKeySourceNames>deviceid, date</partitionKeySourceNames>\n"
              + "    <clusterColumnSourceNames>sampletime</clusterColumnSourceNames>\n"
              + "    <indexColumnSourceNames></indexColumnSourceNames>\n"
              + // !!! 2016-04 ERDDAP can't detect indexes in C*
              // v3
              "    <maxRequestFraction>1</maxRequestFraction>\n"
              + "    <columnNameQuotes></columnNameQuotes>\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <!-- sourceAttributes>\n"
              + "    </sourceAttributes -->\n"
              + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
              + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
              + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
              + "    -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"cdm_data_type\">Other</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
              + "        <att name=\"creator_name\">Ocean Networks Canada</att>\n"
              + "        <att name=\"creator_url\">http://www.oceannetworks.ca/</att>\n"
              + "        <att name=\"infoUrl\">http://www.oceannetworks.ca/</att>\n"
              + "        <att name=\"institution\">Ocean Networks Canada</att>\n"
              + "        <att name=\"keywords\">bob, bobtable, canada, cascii, cassandra, cboolean, cbyte, cdecimal, cdouble, cfloat, cint, clong, cmap, cset, cshort, ctext, currents, cvarchar, data, date, depth, deviceid, networks, ocean, sampletime, test, time, title, u, v, velocity, vertical, w</att>\n"
              + "        <att name=\"license\">[standard]</att>\n"
              + "        <att name=\"sourceUrl\">(local Cassandra)</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
              + "        <att name=\"subsetVariables\">deviceid, time</att>\n"
              + "        <att name=\"summary\">The summary for Bob&#39;s great Cassandra test data.</att>\n"
              + "        <att name=\"title\">The Title for Bob&#39;s Cassandra Test Data (bobTable)</att>\n"
              + "    </addAttributes>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>deviceid</sourceName>\n"
              + "        <destinationName>deviceid</destinationName>\n"
              + "        <dataType>int</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Deviceid</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>date</sourceName>\n"
              + "        <destinationName>time</destinationName>\n"
              + // this is not the best time var, but no way for ERDDAP
              // to know
              "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"long_name\">Date</att>\n"
              + "            <att name=\"source_name\">date</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>sampletime</sourceName>\n"
              + "        <destinationName>sampletime</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"long_name\">Sampletime</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cascii</sourceName>\n"
              + "        <destinationName>cascii</destinationName>\n"
              + "        <dataType>String</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cascii</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cboolean</sourceName>\n"
              + "        <destinationName>cboolean</destinationName>\n"
              + "        <dataType>boolean</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cboolean</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cbyte</sourceName>\n"
              + "        <destinationName>cbyte</destinationName>\n"
              + "        <dataType>int</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cbyte</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cdecimal</sourceName>\n"
              + "        <destinationName>cdecimal</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cdecimal</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cdouble</sourceName>\n"
              + "        <destinationName>cdouble</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cdouble</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cfloat</sourceName>\n"
              + "        <destinationName>cfloat</destinationName>\n"
              + "        <dataType>float</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cfloat</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cint</sourceName>\n"
              + "        <destinationName>cint</destinationName>\n"
              + "        <dataType>int</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cint</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>clong</sourceName>\n"
              + "        <destinationName>clong</destinationName>\n"
              + "        <dataType>long</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"long\">9223372036854775807</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Clong</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cmap</sourceName>\n"
              + "        <destinationName>cmap</destinationName>\n"
              + "        <dataType>String</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cmap</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cset</sourceName>\n"
              + "        <destinationName>cset</destinationName>\n"
              + "        <dataType>String</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cset</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cshort</sourceName>\n"
              + "        <destinationName>cshort</destinationName>\n"
              + "        <dataType>int</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cshort</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>ctext</sourceName>\n"
              + "        <destinationName>ctext</destinationName>\n"
              + "        <dataType>String</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Ctext</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>cvarchar</sourceName>\n"
              + "        <destinationName>cvarchar</destinationName>\n"
              + "        <dataType>String</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Cvarchar</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>depth</sourceName>\n"
              + "        <destinationName>depth</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
              + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Depth</att>\n"
              + "            <att name=\"standard_name\">depth</att>\n"
              + "            <att name=\"units\">m</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>u</sourceName>\n"
              + "        <destinationName>u</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">U</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>v</sourceName>\n"
              + "        <destinationName>v</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">V</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>w</sourceName>\n"
              + "        <destinationName>w</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Currents</att>\n"
              + "            <att name=\"long_name\">Vertical Velocity</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>\n"
              + "\n";
      // String2.log(results);
      Test.ensureEqual(results, expected, "results=\n" + results);
      // String2.pressEnterToContinue();

      // generate the datasets.xml for a table with static columns
      results =
          EDDTableFromCassandra.generateDatasetsXml(
              url,
              props,
              keyspace,
              "staticTest",
              tReloadEveryNMinutes,
              tInfoUrl,
              tInstitution,
              tSummary,
              "Cassandra Static Test",
              new Attributes());
      expected =
          "<!-- NOTE! Since Cassandra tables don't have any metadata, you must add metadata\n"
              + "  below, notably 'units' for each of the dataVariables. -->\n"
              + "<dataset type=\"EDDTableFromCassandra\" datasetID=\"cass_bobKeyspace_staticTest\" active=\"true\">\n"
              + "    <sourceUrl>127.0.0.1</sourceUrl>\n"
              + "    <keyspace>bobKeyspace</keyspace>\n"
              + "    <tableName>staticTest</tableName>\n"
              + "    <partitionKeySourceNames>deviceid, date</partitionKeySourceNames>\n"
              + "    <clusterColumnSourceNames>sampletime</clusterColumnSourceNames>\n"
              + "    <indexColumnSourceNames></indexColumnSourceNames>\n"
              + "    <maxRequestFraction>1</maxRequestFraction>\n"
              + "    <columnNameQuotes></columnNameQuotes>\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <!-- sourceAttributes>\n"
              + "    </sourceAttributes -->\n"
              + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
              + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
              + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
              + "    -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"cdm_data_type\">Point</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
              + "        <att name=\"creator_name\">Ocean Networks Canada</att>\n"
              + "        <att name=\"creator_url\">http://www.oceannetworks.ca/</att>\n"
              + "        <att name=\"infoUrl\">http://www.oceannetworks.ca/</att>\n"
              + "        <att name=\"institution\">Ocean Networks Canada</att>\n"
              + "        <att name=\"keywords\">canada, cassandra, data, date, depth, deviceid, latitude, longitude, networks, ocean, sampletime, static, test, time, u, v</att>\n"
              + "        <att name=\"license\">[standard]</att>\n"
              + "        <att name=\"sourceUrl\">(local Cassandra)</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
              + "        <att name=\"subsetVariables\">deviceid, time, latitude, longitude</att>\n"
              + "        <att name=\"summary\">The summary for Bob&#39;s great Cassandra test data.</att>\n"
              + "        <att name=\"title\">Cassandra Static Test</att>\n"
              + "    </addAttributes>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>deviceid</sourceName>\n"
              + "        <destinationName>deviceid</destinationName>\n"
              + "        <dataType>int</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Deviceid</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>date</sourceName>\n"
              + "        <destinationName>time</destinationName>\n"
              + // not the best choice, but no way for ERDDAP to know
              "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"long_name\">Date</att>\n"
              + "            <att name=\"source_name\">date</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>sampletime</sourceName>\n"
              + "        <destinationName>sampletime</destinationName>\n"
              + "        <dataType>double</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"long_name\">Sampletime</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>depth</sourceName>\n"
              + "        <destinationName>depth</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
              + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Depth</att>\n"
              + "            <att name=\"standard_name\">depth</att>\n"
              + "            <att name=\"units\">m</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>lat</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "        <dataType>float</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Latitude</att>\n"
              + "            <att name=\"standard_name\">latitude</att>\n"
              + "            <att name=\"units\">degrees_north</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>lon</sourceName>\n"
              + "        <destinationName>longitude</destinationName>\n"
              + "        <dataType>float</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Longitude</att>\n"
              + "            <att name=\"standard_name\">longitude</att>\n"
              + "            <att name=\"units\">degrees_east</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>u</sourceName>\n"
              + "        <destinationName>u</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">U</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>v</sourceName>\n"
              + "        <destinationName>v</destinationName>\n"
              + "        <dataType>floatList</dataType>\n"
              + "        <!-- sourceAttributes>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">V</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>\n\n";
      // String2.log(results);
      Test.ensureEqual(results, expected, "results=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException("This test requires Cassandra running on Bob's laptop.", t);
    }
  }

  /**
   * This performs basic tests of the local Cassandra database.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testBasic() throws Throwable {
    // String2.log("\n*** EDDTableFromCassandra.testBasic");
    // testVerboseOn();
    int language = 0;
    long cumTime = 0;
    String query;
    String dir = EDStatic.fullTestCacheDirectory;
    String tName, results, expected;
    int po;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String tDatasetID = "cass_bobKeyspace_bobTable";

    try {
      EDDTableFromCassandra tedd =
          (EDDTableFromCassandra) EDDTableFromCassandra.oneFromDatasetsXml(null, tDatasetID);
      cumTime = System.currentTimeMillis();
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nDataset constructed.\n" +
      // "Paused to allow you to check the connectionProperty's.");
      /* */
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "", dir, tedd.className() + "_Basic", ".dds");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "Dataset {\n"
              + "  Sequence {\n"
              + "    Int32 deviceid;\n"
              + "    Float64 date;\n"
              + "    Float64 sampletime;\n"
              + "    String cascii;\n"
              + "    Byte cboolean;\n"
              + "    Int32 cbyte;\n"
              + "    Float64 cdecimal;\n"
              + "    Float64 cdouble;\n"
              + "    Float32 cfloat;\n"
              + "    Int32 cint;\n"
              + "    Float64 clong;\n"
              + "    String cmap;\n"
              + "    String cset;\n"
              + "    Int32 cshort;\n"
              + "    String ctext;\n"
              + "    String cvarchar;\n"
              + "    Float32 depth;\n"
              + "    Float32 u;\n"
              + "    Float32 v;\n"
              + "    Float32 w;\n"
              + "  } s;\n"
              + "} s;\n";
      // String2.log("\n>> .das results=\n" + results);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .dds
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "", dir, tedd.className() + "_Basic", ".das");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "Attributes {\n"
              + " s {\n"
              + "  deviceid {\n"
              + "    Int32 _FillValue 2147483647;\n"
              + "    Int32 actual_range 1001, 1009;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Deviceid\";\n"
              + "  }\n"
              + "  date {\n"
              + "    Float64 actual_range 1.4148e+9, 1.4155776e+9;\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Date\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  sampletime {\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Sampletime\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  cascii {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cascii\";\n"
              + "  }\n"
              + "  cboolean {\n"
              + "    Byte _FillValue 127;\n"
              + "    String _Unsigned \"false\";\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cboolean\";\n"
              + "  }\n"
              + "  cbyte {\n"
              + "    Int32 _FillValue 2147483647;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cbyte\";\n"
              + "  }\n"
              + "  cdecimal {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cdecimal\";\n"
              + "  }\n"
              + "  cdouble {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cdouble\";\n"
              + "  }\n"
              + "  cfloat {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cfloat\";\n"
              + "  }\n"
              + "  cint {\n"
              + "    Int32 _FillValue 2147483647;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cint\";\n"
              + "  }\n"
              + "  clong {\n"
              + "    Float64 _FillValue 9223372036854775807;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Clong\";\n"
              + "  }\n"
              + "  cmap {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cmap\";\n"
              + "  }\n"
              + "  cset {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cset\";\n"
              + "  }\n"
              + "  cshort {\n"
              + "    Int32 _FillValue 2147483647;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cshort\";\n"
              + "  }\n"
              + "  ctext {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Ctext\";\n"
              + "  }\n"
              + "  cvarchar {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cvarchar\";\n"
              + "  }\n"
              + "  depth {\n"
              + "    String _CoordinateAxisType \"Height\";\n"
              + "    String _CoordinateZisPositive \"down\";\n"
              + "    String axis \"Z\";\n"
              + "    Float64 colorBarMaximum 8000.0;\n"
              + "    Float64 colorBarMinimum -8000.0;\n"
              + "    String colorBarPalette \"TopographyDepth\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Depth\";\n"
              + "    String positive \"down\";\n"
              + "    String standard_name \"depth\";\n"
              + "    String units \"m\";\n"
              + "  }\n"
              + "  u {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"U\";\n"
              + "  }\n"
              + "  v {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"V\";\n"
              + "  }\n"
              + "  w {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"W\";\n"
              + "  }\n"
              + " }\n"
              + "  NC_GLOBAL {\n"
              + "    String cdm_data_type \"Other\";\n"
              + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + "    String creator_name \"Ocean Networks Canada\";\n"
              + "    String creator_url \"http://www.oceannetworks.ca/\";\n"
              + "    String geospatial_vertical_positive \"down\";\n"
              + "    String geospatial_vertical_units \"m\";\n"
              + "    String history";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

      // \"2014-11-15T15:05:05Z (Cassandra)
      // 2014-11-15T15:05:05Z
      // http://localhost:8080/cwexperimental/tabledap/cass_bobKeyspace_bobTable.das\";
      expected =
          "String infoUrl \"http://www.oceannetworks.ca/\";\n"
              + "    String institution \"Ocean Networks Canada\";\n"
              + "    String keywords \"bob, canada, cascii, cboolean, cbyte, cdecimal, cdouble, cfloat, cint, clong, cmap, cset, cshort, ctext, cvarchar, data, date, depth, deviceid, networks, ocean, sampletime, test, time\";\n"
              + "    String license \"The data may be used and redistributed for free but is not intended\n"
              + "for legal use, since it may contain inaccuracies. Neither the data\n"
              + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
              + "of their employees or contractors, makes any warranty, express or\n"
              + "implied, including warranties of merchantability and fitness for a\n"
              + "particular purpose, or assumes any legal liability for the accuracy,\n"
              + "completeness, or usefulness, of this information.\";\n"
              + "    String sourceUrl \"(Cassandra)\";\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
              + "    String subsetVariables \"deviceid, date\";\n"
              + "    String summary \"The summary for Bob's Cassandra test data.\";\n"
              + "    String title \"Bob's Cassandra Test Data\";\n"
              + "  }\n"
              + "}\n";
      po = results.indexOf(expected.substring(0, 14));
      if (po < 0) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

      // all
      query = "";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_all", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,10.1,-0.11,-0.12,-0.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,20.1,0.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,30.1,0.11,0.12,0.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,varchar2,10.2,-2.11,-2.12,-2.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,varchar2,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,varchar2,30.2,2.11,2.12,2.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,varchar3,10.3,-3.11,-3.12,-3.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,varchar3,20.3,0.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,varchar3,30.3,3.11,3.12,3.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,10.1,-0.11,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,20.1,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,30.1,0.11,0.12,0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,10.2,-99.0,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,-99.0,0.11,0.12,-99.0\n"
              + "1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,varchar7,10.7,-7.11,-7.12,-7.13\n"
              + "1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,varchar7,20.7,0.0,NaN,0.0\n"
              + "1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,varchar7,30.7,7.11,7.12,7.13\n"
              + "1008,2014-11-08T00:00:00Z,2014-11-08T01:02:03Z,ascii8,0,8,8.00001,8.001,8.1,8000000,8000000000000,\"{map81=8.1, map82=8.2, map83=8.3, map84=8.4}\",\"[set81, set82, set83, set84, set85]\",8000,text8,varchar8,10.8,-8.11,-8.12,-8.13\n"
              + "1008,2014-11-08T00:00:00Z,2014-11-08T01:02:03Z,ascii8,0,8,8.00001,8.001,8.1,8000000,8000000000000,\"{map81=8.1, map82=8.2, map83=8.3, map84=8.4}\",\"[set81, set82, set83, set84, set85]\",8000,text8,varchar8,20.8,0.0,NaN,0.0\n"
              + "1008,2014-11-08T00:00:00Z,2014-11-08T01:02:03Z,ascii8,0,8,8.00001,8.001,8.1,8000000,8000000000000,\"{map81=8.1, map82=8.2, map83=8.3, map84=8.4}\",\"[set81, set82, set83, set84, set85]\",8000,text8,varchar8,30.8,8.11,8.12,8.13\n"
              + "1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,-99,NaN,NaN,NaN,-99,-99,,,-99,,,NaN,NaN,NaN,NaN\n";

      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: all\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset test sampletime ">=" handled correctly
      query = "deviceid,sampletime,cmap&deviceid=1001&sampletime>=2014-11-01T03:02:03Z";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_subset1", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cmap\n"
              + ",UTC,\n"
              + "1001,2014-11-01T03:02:03Z,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\"\n"
              + "1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n"
              + "1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test sampletime \">=\" handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset test sampletime ">" handled correctly
      query = "deviceid,sampletime,cmap&deviceid=1001&sampletime>2014-11-01T03:02:03Z";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_subset2", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cmap\n"
              + ",UTC,\n"
              + "1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n"
              + "1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test sampletime \">\" handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset test secondary index: ctext '=' handled correctly
      // so erddap tells Cass to handle this constraint
      query = "deviceid,sampletime,ctext&ctext=\"text1\"";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_subset2", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,ctext\n"
              + ",UTC,\n"
              + "1001,2014-11-01T01:02:03Z,text1\n"
              + "1001,2014-11-02T01:02:03Z,text1\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test secondary index: ctext '=' handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset with code changes to allow any constraint on secondary index:
      // proves ctext '>=' not allowed
      // so this tests that erddap handles the constraint
      query = "deviceid,sampletime,ctext&ctext>=\"text3\"";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_subset2", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,ctext\n"
              + ",UTC,\n"
              + "1001,2014-11-01T03:02:03Z,text3\n"
              + "1007,2014-11-07T01:02:03Z,text7\n"
              + "1008,2014-11-08T01:02:03Z,text8\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test secondary index: ctext '>=' handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // distinct() subsetVariables
      query = "deviceid,cascii&deviceid=1001&distinct()";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_distinct", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,cascii\n"
              + ",\n"
              + "1001,\n"
              + "1001,ascii1\n"
              + "1001,ascii2\n"
              + "1001,ascii3\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: distinct()\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // orderBy() subsetVariables
      query = "deviceid,sampletime,cascii&deviceid=1001&orderBy(\"cascii\")";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_distinct", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cascii\n"
              + ",UTC,\n"
              + "1001,2014-11-02T02:02:03Z,\n"
              + "1001,2014-11-01T01:02:03Z,ascii1\n"
              + "1001,2014-11-02T01:02:03Z,ascii1\n"
              + "1001,2014-11-01T02:02:03Z,ascii2\n"
              + "1001,2014-11-01T03:02:03Z,ascii3\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: orderBy()\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // just keys deviceid
      query = "deviceid,date&deviceid=1001";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_justkeys", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date\n"
              + ",UTC\n"
              + "1001,2014-11-01T00:00:00Z\n"
              + "1001,2014-11-02T00:00:00Z\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: just keys deviceid\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // no matching data (no matching keys)
      try {
        query = "deviceid,sampletime&sampletime<2013-01-01";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "_nodata1", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = t.toString();
        String2.log(msg);
        Test.ensureEqual(
            msg,
            "com.cohort.util.SimpleException: Your query produced no matching results. "
                + "(no matching partition key values)",
            "");
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: no matching data (no matching keys)\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset cint=-99
      query = "&cint=-99";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_intNaN", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,10.2,-99.0,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,-99.0,0.11,0.12,-99.0\n"
              + "1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,-99,NaN,NaN,NaN,-99,-99,,,-99,,,NaN,NaN,NaN,NaN\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset cfloat=NaN
      query = "&cfloat=NaN";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_floatNaN", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,10.2,-99.0,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,-99.0,0.11,0.12,-99.0\n"
              + "1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,-99,NaN,NaN,NaN,-99,-99,,,-99,,,NaN,NaN,NaN,NaN\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset cboolean=NaN
      query = "&cboolean=NaN";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_booleanNaN", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,-99,NaN,NaN,NaN,-99,-99,,,-99,,,NaN,NaN,NaN,NaN\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset cboolean=1
      query = "&cboolean=1";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_boolean1", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,10.2,-99.0,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,-99.0,0.11,0.12,-99.0\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset regex on set
      query = "&cset=~\".*set73.*\"";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_set73", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,varchar7,10.7,-7.11,-7.12,-7.13\n"
              + "1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,varchar7,20.7,0.0,NaN,0.0\n"
              + "1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,varchar7,30.7,7.11,7.12,7.13\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // no matching data (sampletime)
      try {
        query = "&deviceid=1001&sampletime<2014-01-01";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "_nodata2", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        String2.log(msg);
        if (msg.indexOf("Your query produced no matching results.") < 0)
          throw new RuntimeException("Unexpected error.", t);
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: no matching data (sampletime)\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // request a subset of response vars
      // (Unable to duplicate reported error: my Cass doesn't have
      // enough data to trigger partial write to TableWriter)
      // error from ONC for query=
      // rdiadcp_AllSensors_23065.nc?time,depthBins,eastward_seawater_velocity
      // &deviceid>=23065&time>=2014-05-15T02:00:00Z&time<=2014-05-16T00:00:05Z
      query =
          "sampletime,depth,u" + "&deviceid=1001&sampletime>=2014-11-01&sampletime<=2014-11-01T03";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_dup", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "sampletime,depth,u\n"
              + "UTC,m,\n"
              + "2014-11-01T01:02:03Z,10.1,-0.11\n"
              + "2014-11-01T01:02:03Z,20.1,0.0\n"
              + "2014-11-01T01:02:03Z,30.1,0.11\n"
              + "2014-11-01T02:02:03Z,10.2,-2.11\n"
              + "2014-11-01T02:02:03Z,20.2,0.0\n"
              + "2014-11-01T02:02:03Z,30.2,2.11\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: just keys deviceid\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // no matching data (erddap)
      try {
        query = "&deviceid>1001&cascii=\"zztop\"";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "nodata3", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        String2.log(msg);
        if (msg.indexOf("Your query produced no matching results.") < 0)
          throw new RuntimeException("Unexpected error.", t);
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: no matching data (erddap)\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // finished
      // cum time ~313, impressive: ~30 subqueries and a lot of time spent
      // logging to screen.
      String2.log(
          "\n* EDDTableFromCassandra.testBasic finished successfully. time="
              + (System.currentTimeMillis() - cumTime)
              + "ms");

      /* */
    } catch (Throwable t) {
      throw new RuntimeException("This test requires Cassandra running on Bob's laptop.", t);
    }
  }

  /**
   * This tests maxRequestFraction.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testMaxRequestFraction() throws Throwable {
    // String2.log("\n*** EDDTableFromCassandra.testMaxRequestFraction");
    // testVerboseOn();
    int language = 0;
    long cumTime = 0;
    String query = null;
    String dir = EDStatic.fullTestCacheDirectory;
    String tName, results, expected;
    int po;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String tDatasetID = "cassTestFraction";

    try {
      EDDTableFromCassandra tedd =
          (EDDTableFromCassandra) EDDTableFromCassandra.oneFromDatasetsXml(null, tDatasetID);
      cumTime = System.currentTimeMillis();
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue("\nDataset constructed.");

      // all
      try {
        query = "&deviceid>1000&cascii=\"zztop\"";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "frac", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        String2.log(msg);
        if (msg.indexOf(
                "You are requesting too much data. "
                    + "Please further constrain one or more of these variables: "
                    + "deviceid, date, sampletime. (5/5=1.0 > 0.55)")
            < 0) throw new RuntimeException("Unexpected error.", t);
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: all\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // still too much
      try {
        query = "&deviceid>1001&cascii=\"zztop\"";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "frac2", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        String2.log(msg);
        if (msg.indexOf(
                "You are requesting too much data. "
                    + "Please further constrain one or more of these variables: "
                    + "deviceid, date, sampletime. (3/5=0.6 > 0.55)")
            < 0) throw new RuntimeException("Unexpected error.", t);
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: all\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset 2/5 0.4 is okay
      query = "deviceid,sampletime,cascii&deviceid=1001&sampletime>=2014-11-01T03:02:03Z";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_frac3", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cascii\n"
              + ",UTC,\n"
              + "1001,2014-11-01T03:02:03Z,ascii3\n"
              + "1001,2014-11-02T01:02:03Z,ascii1\n"
              + "1001,2014-11-02T02:02:03Z,\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test sampletime \">=\" handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // finished
      String2.log(
          "\n* EDDTableFromCassandra.testMaxRequestFraction finished successfully. time="
              + (System.currentTimeMillis() - cumTime)
              + "ms");

      /* */
    } catch (Throwable t) {
      throw new RuntimeException("This test requires Cassandra running on Bob's laptop.", t);
    }
  }

  /**
   * This is like testBasic, but on a dataset that is restricted to 1 device (1001).
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testCass1Device() throws Throwable {
    // String2.log("\n*** EDDTableFromCassandra.testCass1Device");
    // testVerboseOn();
    int language = 0;
    long cumTime = 0;
    String query;
    String dir = EDStatic.fullTestCacheDirectory;
    String tName, results, expected;
    int po;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String tDatasetID = "cass1Device";

    try {
      EDDTableFromCassandra tedd =
          (EDDTableFromCassandra) EDDTableFromCassandra.oneFromDatasetsXml(null, tDatasetID);
      cumTime = System.currentTimeMillis();
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nDataset constructed.\n" +
      // "Paused to allow you to check the connectionProperty's.");

      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "", dir, tedd.className() + "_Basic", ".dds");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "Dataset {\n"
              + "  Sequence {\n"
              + "    Int32 deviceid;\n"
              + "    Float64 date;\n"
              + "    Float64 sampletime;\n"
              + "    String cascii;\n"
              + "    Byte cboolean;\n"
              + "    Int32 cbyte;\n"
              + "    Float64 cdecimal;\n"
              + "    Float64 cdouble;\n"
              + "    Float32 cfloat;\n"
              + "    Int32 cint;\n"
              + "    Float64 clong;\n"
              + "    String cmap;\n"
              + "    String cset;\n"
              + "    Int32 cshort;\n"
              + "    String ctext;\n"
              + "    String cvarchar;\n"
              + "    Float32 depth;\n"
              + "    Float32 u;\n"
              + "    Float32 v;\n"
              + "    Float32 w;\n"
              + "  } s;\n"
              + "} s;\n";
      // String2.log("\n>> .das results=\n" + results);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .dds
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "", dir, tedd.className() + "_Basic", ".das");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "Attributes {\n"
              + " s {\n"
              + "  deviceid {\n"
              + "    Int32 actual_range 1001, 1001;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Deviceid\";\n"
              + "  }\n"
              + "  date {\n"
              + "    Float64 actual_range 1.4148e+9, 1.4148864e+9;\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Date\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  sampletime {\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Sampletime\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  cascii {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cascii\";\n"
              + "  }\n"
              + "  cboolean {\n"
              + "    String _Unsigned \"false\";\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cboolean\";\n"
              + "  }\n"
              + "  cbyte {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cbyte\";\n"
              + "  }\n"
              + "  cdecimal {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cdecimal\";\n"
              + "  }\n"
              + "  cdouble {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cdouble\";\n"
              + "  }\n"
              + "  cfloat {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cfloat\";\n"
              + "  }\n"
              + "  cint {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cint\";\n"
              + "  }\n"
              + "  clong {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Clong\";\n"
              + "  }\n"
              + "  cmap {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cmap\";\n"
              + "  }\n"
              + "  cset {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cset\";\n"
              + "  }\n"
              + "  cshort {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cshort\";\n"
              + "  }\n"
              + "  ctext {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Ctext\";\n"
              + "  }\n"
              + "  cvarchar {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Cvarchar\";\n"
              + "  }\n"
              + "  depth {\n"
              + "    String _CoordinateAxisType \"Height\";\n"
              + "    String _CoordinateZisPositive \"down\";\n"
              + "    String axis \"Z\";\n"
              + "    Float64 colorBarMaximum 8000.0;\n"
              + "    Float64 colorBarMinimum -8000.0;\n"
              + "    String colorBarPalette \"TopographyDepth\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Depth\";\n"
              + "    String positive \"down\";\n"
              + "    String standard_name \"depth\";\n"
              + "    String units \"m\";\n"
              + "  }\n"
              + "  u {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"U\";\n"
              + "  }\n"
              + "  v {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"V\";\n"
              + "  }\n"
              + "  w {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"W\";\n"
              + "  }\n"
              + " }\n"
              + "  NC_GLOBAL {\n"
              + "    String cdm_data_type \"Other\";\n"
              + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + "    String creator_name \"Ocean Networks Canada\";\n"
              + "    String creator_url \"http://www.oceannetworks.ca/\";\n"
              + "    String geospatial_vertical_positive \"down\";\n"
              + "    String geospatial_vertical_units \"m\";\n"
              + "    String history";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

      // \"2014-11-15T15:05:05Z (Cassandra)
      // 2014-11-15T15:05:05Z
      // http://localhost:8080/cwexperimental/tabledap/cass_bobKeyspace_bobTable.das\";
      expected =
          "String infoUrl \"http://www.oceannetworks.ca/\";\n"
              + "    String institution \"Ocean Networks Canada\";\n"
              + "    String keywords \"bob, canada, cascii, cboolean, cbyte, cdecimal, cdouble, cfloat, cint, clong, cmap, cset, cshort, ctext, cvarchar, data, date, depth, deviceid, networks, ocean, sampletime, test, time\";\n"
              + "    String license \"The data may be used and redistributed for free but is not intended\n"
              + "for legal use, since it may contain inaccuracies. Neither the data\n"
              + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
              + "of their employees or contractors, makes any warranty, express or\n"
              + "implied, including warranties of merchantability and fitness for a\n"
              + "particular purpose, or assumes any legal liability for the accuracy,\n"
              + "completeness, or usefulness, of this information.\";\n"
              + "    String sourceUrl \"(Cassandra)\";\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
              + "    String subsetVariables \"deviceid, date\";\n"
              + "    String summary \"The summary for Bob's Cassandra test data.\";\n"
              + "    String title \"Bob's Cassandra Test Data\";\n"
              + "  }\n"
              + "}\n";
      po = results.indexOf(expected.substring(0, 14));
      if (po < 0) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

      // all
      query = "";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_all", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n"
              + ",UTC,UTC,,,,,,,,,,,,,,m,,,\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,10.1,-0.11,-0.12,-0.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,20.1,0.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,30.1,0.11,0.12,0.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,varchar2,10.2,-2.11,-2.12,-2.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,varchar2,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,varchar2,30.2,2.11,2.12,2.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,varchar3,10.3,-3.11,-3.12,-3.13\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,varchar3,20.3,0.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,varchar3,30.3,3.11,3.12,3.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,10.1,-0.11,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,20.1,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,varchar1,30.1,0.11,0.12,0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,10.2,-99.0,-0.12,-0.13\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,20.2,0.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,-99,NaN,NaN,NaN,-99,-99,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",-99,,,-99.0,0.11,0.12,-99.0\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: all\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset test sampletime ">=" handled correctly
      query = "deviceid,sampletime,cmap&sampletime>=2014-11-01T03:02:03Z";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_subset1", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cmap\n"
              + ",UTC,\n"
              + "1001,2014-11-01T03:02:03Z,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\"\n"
              + "1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n"
              + "1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test sampletime \">=\" handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // subset test sampletime ">" handled correctly
      query = "deviceid,sampletime,cmap&sampletime>2014-11-01T03:02:03Z";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_subset2", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cmap\n"
              + ",UTC,\n"
              + "1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n"
              + "1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: subset test sampletime \">\" handled correctly\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // distinct() subsetVariables
      query = "deviceid,cascii&distinct()";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_distinct", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,cascii\n"
              + ",\n"
              + "1001,\n"
              + "1001,ascii1\n"
              + "1001,ascii2\n"
              + "1001,ascii3\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: distinct()\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // orderBy() subsetVariables
      query = "deviceid,sampletime,cascii&orderBy(\"cascii\")";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_distinct", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,sampletime,cascii\n"
              + ",UTC,\n"
              + "1001,2014-11-02T02:02:03Z,\n"
              + "1001,2014-11-01T01:02:03Z,ascii1\n"
              + "1001,2014-11-02T01:02:03Z,ascii1\n"
              + "1001,2014-11-01T02:02:03Z,ascii2\n"
              + "1001,2014-11-01T03:02:03Z,ascii3\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: orderBy()\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // just keys deviceid
      query = "deviceid,date";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_justkeys", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date\n"
              + ",UTC\n"
              + "1001,2014-11-01T00:00:00Z\n"
              + "1001,2014-11-02T00:00:00Z\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: just keys deviceid\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // no matching data (no matching keys)
      try {
        query = "deviceid,sampletime&sampletime<2013-01-01";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "_nodata1", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = t.toString();
        String2.log(msg);
        Test.ensureEqual(
            msg,
            "com.cohort.util.SimpleException: Your query produced no matching results. "
                + "(no matching partition key values)",
            "");
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: no matching data (no matching keys)\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // no matching data (sampletime)
      try {
        query = "&sampletime<2014-01-01";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "_nodata2", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        String2.log(msg);
        if (msg.indexOf("Your query produced no matching results.") < 0)
          throw new RuntimeException("Unexpected error.", t);
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: no matching data (sampletime)\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // no matching data (erddap)
      try {
        query = "&cascii=\"zztop\"";
        tName =
            tedd.makeNewFileForDapQuery(
                language, null, null, query, dir, tedd.className() + "nodata3", ".csv");
        results = File2.directReadFrom88591File(dir + tName);
        expected = "Shouldn't get here";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      } catch (Throwable t) {
        String msg = MustBe.throwableToString(t);
        String2.log(msg);
        if (msg.indexOf("Your query produced no matching results.") < 0)
          throw new RuntimeException("Unexpected error.", t);
      }
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: no matching data (erddap)\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // finished
      // cum time ~313, impressive: ~30 subqueries and a lot of time spent
      // logging to screen.
      String2.log(
          "\n* EDDTableFromCassandra.testCass1Device finished successfully. time="
              + (System.currentTimeMillis() - cumTime)
              + "ms");

      /* */
    } catch (Throwable t) {
      throw new RuntimeException("This test requires Cassandra running on Bob's laptop.", t);
    }
  }

  /**
   * This is like testBasic, but on a dataset with 2 static columns.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testStatic() throws Throwable {
    // String2.log("\n*** EDDTableFromCassandra.testStatic");
    // testVerboseOn();
    int language = 0;
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    long cumTime = 0;
    String query;
    String dir = EDStatic.fullTestCacheDirectory;
    String tName, results, expected;
    int po;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String tDatasetID = "cass_bobKeyspace_staticTest";

    try {
      EDDTableFromCassandra tedd =
          (EDDTableFromCassandra) EDDTableFromCassandra.oneFromDatasetsXml(null, tDatasetID);
      cumTime = System.currentTimeMillis();
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nDataset constructed.\n" +
      // "Paused to allow you to check the connectionProperty's.");

      // .dds
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "", dir, tedd.className() + "_Basic", ".dds");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "Dataset {\n"
              + "  Sequence {\n"
              + "    Int32 deviceid;\n"
              + "    Float64 date;\n"
              + "    Float64 time;\n"
              + "    Float32 depth;\n"
              + "    Float32 latitude;\n"
              + "    Float32 longitude;\n"
              + "    Float32 u;\n"
              + "    Float32 v;\n"
              + "  } s;\n"
              + "} s;\n";
      // String2.log("\n>> .das results=\n" + results);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .das
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, "", dir, tedd.className() + "_Basic", ".das");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "Attributes {\n"
              + " s {\n"
              + "  deviceid {\n"
              + "    Int32 actual_range 1001, 1001;\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Deviceid\";\n"
              + "  }\n"
              + "  date {\n"
              + "    Float64 actual_range 1.4148e+9, 1.4148864e+9;\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Date\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  time {\n"
              + "    String _CoordinateAxisType \"Time\";\n"
              + "    String axis \"T\";\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Sample Time\";\n"
              + "    String standard_name \"time\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  depth {\n"
              + "    String _CoordinateAxisType \"Height\";\n"
              + "    String _CoordinateZisPositive \"down\";\n"
              + "    String axis \"Z\";\n"
              + "    Float64 colorBarMaximum 8000.0;\n"
              + "    Float64 colorBarMinimum -8000.0;\n"
              + "    String colorBarPalette \"TopographyDepth\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Depth\";\n"
              + "    String positive \"down\";\n"
              + "    String standard_name \"depth\";\n"
              + "    String units \"m\";\n"
              + "  }\n"
              + "  latitude {\n"
              + "    String _CoordinateAxisType \"Lat\";\n"
              + "    Float32 actual_range 33.0, 34.0;\n"
              + "    String axis \"Y\";\n"
              + "    Float64 colorBarMaximum 90.0;\n"
              + "    Float64 colorBarMinimum -90.0;\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Latitude\";\n"
              + "    String standard_name \"latitude\";\n"
              + "    String units \"degrees_north\";\n"
              + "  }\n"
              + "  longitude {\n"
              + "    String _CoordinateAxisType \"Lon\";\n"
              + "    Float32 actual_range -124.0, -123.0;\n"
              + "    String axis \"X\";\n"
              + "    Float64 colorBarMaximum 180.0;\n"
              + "    Float64 colorBarMinimum -180.0;\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Longitude\";\n"
              + "    String standard_name \"longitude\";\n"
              + "    String units \"degrees_east\";\n"
              + "  }\n"
              + "  u {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"U\";\n"
              + "  }\n"
              + "  v {\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"V\";\n"
              + "  }\n"
              + " }\n"
              + "  NC_GLOBAL {\n"
              + "    String cdm_data_type \"Point\";\n"
              + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + "    String creator_name \"Ocean Networks Canada\";\n"
              + "    String creator_url \"http://www.oceannetworks.ca/\";\n"
              + "    Float64 Easternmost_Easting -123.0;\n"
              + "    String featureType \"Point\";\n"
              + "    Float64 geospatial_lat_max 34.0;\n"
              + "    Float64 geospatial_lat_min 33.0;\n"
              + "    String geospatial_lat_units \"degrees_north\";\n"
              + "    Float64 geospatial_lon_max -123.0;\n"
              + "    Float64 geospatial_lon_min -124.0;\n"
              + "    String geospatial_lon_units \"degrees_east\";\n"
              + "    String geospatial_vertical_positive \"down\";\n"
              + "    String geospatial_vertical_units \"m\";\n"
              + "    String history "; // "2014-12-10T19:51:59Z (Cassandra)";\n" +
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

      // \"2014-11-15T15:05:05Z (Cassandra)
      // 2014-11-15T15:05:05Z
      // http://localhost:8080/cwexperimental/tabledap/cass_bobKeyspace_bobTable.das\";
      expected =
          "String infoUrl \"http://www.oceannetworks.ca/\";\n"
              + "    String institution \"Ocean Networks Canada\";\n"
              + "    String keywords \"canada, cassandra, date, depth, deviceid, networks, ocean, sampletime, static, test, time\";\n"
              + "    String license \"The data may be used and redistributed for free but is not intended\n"
              + "for legal use, since it may contain inaccuracies. Neither the data\n"
              + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
              + "of their employees or contractors, makes any warranty, express or\n"
              + "implied, including warranties of merchantability and fitness for a\n"
              + "particular purpose, or assumes any legal liability for the accuracy,\n"
              + "completeness, or usefulness, of this information.\";\n"
              + "    Float64 Northernmost_Northing 34.0;\n"
              + "    String sourceUrl \"(Cassandra)\";\n"
              + "    Float64 Southernmost_Northing 33.0;\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
              + "    String subsetVariables \"deviceid, date, latitude, longitude\";\n"
              + "    String summary \"The summary for Bob's Cassandra test data.\";\n"
              + "    String title \"Cassandra Static Test\";\n"
              + "    Float64 Westernmost_Easting -124.0;\n"
              + "  }\n"
              + "}\n";
      po = results.indexOf(expected.substring(0, 14));
      if (po < 0) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

      // all
      query = "";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_staticAll", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          // This shows that lat and lon just have different values for each combination
          // of the
          // partition key (deviceid+date).
          "deviceid,date,time,depth,latitude,longitude,u,v\n"
              + ",UTC,UTC,m,degrees_north,degrees_east,,\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,10.1,33.0,-123.0,-0.11,-0.12\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,20.1,33.0,-123.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,30.1,33.0,-123.0,0.11,0.12\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,10.1,33.0,-123.0,-0.11,-0.12\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,20.1,33.0,-123.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,30.1,33.0,-123.0,0.11,0.12\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:03:03Z,10.1,33.0,-123.0,-0.31,-0.32\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:03:03Z,20.1,33.0,-123.0,0.0,0.0\n"
              + "1001,2014-11-01T00:00:00Z,2014-11-01T03:03:03Z,30.1,33.0,-123.0,0.31,0.32\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,10.1,34.0,-124.0,-0.41,-0.42\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,20.1,34.0,-124.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,30.1,34.0,-124.0,0.41,0.42\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: all\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // distinct() subsetVariables
      query = "deviceid,date,latitude,longitude&distinct()";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_staticDistinct", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          // diagnostic messages show that ERDDAP got this data from the subset file.
          "deviceid,date,latitude,longitude\n"
              + ",UTC,degrees_north,degrees_east\n"
              + "1001,2014-11-01T00:00:00Z,33.0,-123.0\n"
              + "1001,2014-11-02T00:00:00Z,34.0,-124.0\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: distinct()\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // static variables are NOT constrainable by Cassandra (even '=' queries)
      // so ERDDAP handles it
      query = "&latitude=34";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_staticCon1", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,time,depth,latitude,longitude,u,v\n"
              + ",UTC,UTC,m,degrees_north,degrees_east,,\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,10.1,34.0,-124.0,-0.41,-0.42\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,20.1,34.0,-124.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,30.1,34.0,-124.0,0.41,0.42\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: showed that static variables are NOT constrainable by Cassandra\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // static variables are NOT constrainable by Cassandra (even '>' queries)
      // so ERDDAP handles it
      query = "&latitude>33.5";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "_staticCon2", ".csv");
      results = File2.directReadFrom88591File(dir + tName);
      expected =
          "deviceid,date,time,depth,latitude,longitude,u,v\n"
              + ",UTC,UTC,m,degrees_north,degrees_east,,\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,10.1,34.0,-124.0,-0.41,-0.42\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,20.1,34.0,-124.0,0.0,0.0\n"
              + "1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,30.1,34.0,-124.0,0.41,0.42\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
      // if (pauseBetweenTests)
      // String2.pressEnterToContinue(
      // "\nTest: showed that static variables are NOT constrainable by Cassandra\n" +
      // "query=" + query + "\n" +
      // "Paused to allow you to check the stats.");

      // finished
      String2.log(
          "\n* EDDTableFromCassandra.testStatic finished successfully. time="
              + (System.currentTimeMillis() - cumTime)
              + "ms");

      /* */
    } catch (Throwable t) {
      throw new RuntimeException("This test requires Cassandra running on Bob's laptop.", t);
    }
    // debugMode = oDebugMode;
  }
}
