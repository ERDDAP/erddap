/*
 * EDDTableFromCassandra Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 *
 * <p>Original 2013(?): Working with C*:
 * http://www.datastax.com/documentation/cassandra/2.0/cassandra/gettingStartedCassandraIntro.html
 * and later
 * http://www.datastax.com/documentation/cql/3.1/cql/cql_using/start_cql_win_t.html
 * 2021-05-24 When I entered data into new 2.1.22 installation, I had trouble and
 *   accidentally changed some text values from "ascii#" to just the "#".
 *   Also, I had trouble entering mv/NaN for some int columns so I used -99.

INSTALL CASSANDRA on Lenovo in 2021:
* get Cassandra from https://cassandra.apache.org/download/
  2021-05-21 got 2.1.22 from https://cassandra.apache.org/download/ because that's what ONC uses/recommends
    decompressed into \programs\apache-cassandra-2.1.22
  2019-05-15: got 3.11.4 (released 2019-02-11)
    decompressed into \programs\apache-cassandra-3.11.4
  2018: downloaded apache-cassandra-3.11.3-bin.tar.gz
    decompressed into \programs\apache-cassandra-3.11.3
* Run Cassandra from DOS window
    cd \programs\apache-cassandra-2.1.22\bin\
    cassandra.bat -f
  To stop:
    ^C
  For Bob, Cassandra is at localhost:9160
* Make a snapshot of Dell M4700 data:
  cd \programs\apache-cassandra-2.1.22\bin\
  run: cqlsh.bat
    DESCRIBE KEYSPACE bobKeyspace          //copy and paste that into text document
    COPY bobkeyspace.statictest TO '/erddapTest/cassandra/cassandra_statictest2021.txt';
    COPY bobkeyspace.bobtable TO '/erddapTest/cassandra/cassandra_bobtable2021.txt';
  If you screw up data entry and want to start over, you can delete all rows via e.g.
    TRUNCATE bobkeyspace.bobtable;
* See http://wiki.apache.org/cassandra/GettingStarted
* Recreate the keyspace and data
  cd C:\programs\apache-cassandra-3.11.8\bin
    was cd c:\Program Files\DataStax-DDC\apache-cassandra\bin\
  run: cqlsh.bat, then
    1) copy and paste /erddapTest/cassandra/cassandra_bobKeyspace.txt into shell
    2) copy and paste /erddapTest/cassandra/cassandra_statictest.txt
    3) copy and paste /erddapTest/cassandra/cassandra_bobtable.txt

RUN CASSANDRA on Lenovo in 2021:
* Start it up: cd \programs\apache-cassandra-2.1.22\bin
  For Java version changes: change JAVA_HOME in cassandra.bat, e.g.,
    set JAVA_HOME=C:\programs\jdk8u295-b10
  type: cassandra.bat -f
* Shut it down: ^C
  There is still something running in the background. Restart computer?
* CQL Shell (2.1.22): same directory, run or double click on cqlsh.bat
(It requires Python 2, so I installed it
  and changed 2 instances of "python" in cqlsh.bat to "/Python27/python".)


TEST TABLES:
2016:
create keyspace bobKeyspace with replication = {'class':'SimpleStrategy', 'replication_factor':'2'};
use bobKeyspace;
CREATE TABLE bobkeyspace.bobtable (
    deviceid int,
    date timestamp,
    sampletime timestamp,
    cascii ascii,
    cboolean boolean,
    cbyte int,
    cdecimal double,
    cdouble double,
    cfloat float,
    cint int,
    clong bigint,
    cmap map<text, double>,
    cset set<text>,
    cshort int,
    ctext text,
    cvarchar text,
    depth list<float>,
    u list<float>,
    v list<float>,
    w list<float>,
    PRIMARY KEY ((deviceid, date), sampletime)
) WITH read_repair_chance = 0.0
   AND dclocal_read_repair_chance = 0.1
   AND gc_grace_seconds = 864000
   AND bloom_filter_fp_chance = 0.01
   AND comment = ''
   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy' }
   AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.LZ4Compressor' }
   AND default_time_to_live = 0
   AND speculative_retry = '99.0PERCENTILE';
CREATE INDEX ctext_index ON bobkeyspace.bobtable (ctext);

CREATE TABLE bobkeyspace.statictest (
    deviceid int,
    date timestamp,
    sampletime timestamp,
    depth list<float>,
    lat float static,
    lon float static,
    u list<float>,
    v list<float>,
    PRIMARY KEY ((deviceid, date), sampletime)
) WITH read_repair_chance = 0.0
   AND dclocal_read_repair_chance = 0.1
   AND gc_grace_seconds = 864000
   AND bloom_filter_fp_chance = 0.01
   AND comment = ''
   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy' }
   AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.LZ4Compressor' }
   AND default_time_to_live = 0
   AND speculative_retry = '99.0PERCENTILE';

ADD DATA:
2016:
optional delete if make a mistake, e.g.,
DELETE FROM bobkeyspace.bobtable WHERE deviceid = 1001 AND date = '2014-11-01T00:00:00Z';
---
INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1001,'2014-11-01T00:00:00Z','2014-11-01T01:02:03Z','ascii1',FALSE,1,1.00001,1.001,1.1,1000000,
1000000000000,{'map11':1.1, 'map12':1.2, 'map13':1.3, 'map14':1.4},
{'set11', 'set12', 'set13', 'set14', 'set15'},1000,'text1','cvarchar1',
[10.1,20.1,30.1],[-0.11,0,0.11],[-0.12,0,0.12],[-0.13,0,0.13]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1001,'2014-11-01T00:00:00Z','2014-11-01T02:02:03Z','ascii2',FALSE,2,2.00001,2.001,2.1,2000000,
2000000000000,{'map21':2.1, 'map22':2.2, 'map23':2.3, 'map24':2.4},
{'set21', 'set22', 'set23', 'set24', 'set25'},2000,'text2','cvarchar2',
[10.2,20.2,30.2],[-2.11,0,2.11],[-2.12,0,2.12],[-2.13,0,2.13]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1001,'2014-11-01T00:00:00Z','2014-11-01T03:02:03Z','ascii3',FALSE,3,3.00001,3.001,3.1,3000000,
3000000000000,{'map31':3.1, 'map32':3.2, 'map33':3.3, 'map34':3.4},
{'set31', 'set32', 'set33', 'set34', 'set35'},3000,'text3','cvarchar3',
[10.3,20.3,30.3],[-3.11,0,3.11],[-3.12,0,3.12],[-3.13,0,3.13]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1001,'2014-11-02T00:00:00Z','2014-11-02T01:02:03Z','ascii1',FALSE,1,1.00001,1.001,1.1,1000000,
1000000000000,{'map11':1.1, 'map12':1.2, 'map13':1.3, 'map14':1.4},
{'set11', 'set12', 'set13', 'set14', 'set15'},1000,'text1','cvarchar1',
[10.1,20.1,30.1],[-0.11,0,0.11],[-0.12,0,0.12],[-0.13,0,0.13]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1001,'2014-11-02T00:00:00Z','2014-11-02T02:02:03Z',null,TRUE,null,NaN,NaN,NaN,null,
null,{'map11':-99, '':1.2, 'map13':1.3, 'map14':1.4},
{'set11', '', 'set13', 'set14', 'set15'},null,null,null,
[10.2,20.2,-99],[-99,0,0.11],[-0.12,0,0.12],[-0.13,0,-99]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1007,'2014-11-07T00:00:00Z','2014-11-07T01:02:03Z','ascii7',FALSE,7,7.00001,7.001,7.1,7000000,
7000000000000,{'map71':7.1, 'map72':7.2, 'map73':7.3, 'map74':7.4},
{'set71', 'set72', 'set73', 'set74', 'set75'},7000,'text7','cvarchar7',
[10.7,20.7,30.7],[-7.11,0,7.11],[-7.12,NaN,7.12],[-7.13,0,7.13]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1008,'2014-11-08T00:00:00Z','2014-11-08T01:02:03Z','ascii8',FALSE,8,8.00001,8.001,8.1,8000000,
8000000000000,{'map81':8.1, 'map82':8.2, 'map83':8.3, 'map84':8.4},
{'set81', 'set82', 'set83', 'set84', 'set85'},8000,'text8','cvarchar8',
[10.8,20.8,30.8],[-8.11,0,8.11],[-8.12,NaN,8.12],[-8.13,0,8.13]);

INSERT INTO bobkeyspace.bobtable
(deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,
clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w)
VALUES
(1009,'2014-11-09T00:00:00Z','2014-11-09T01:02:03Z',null,null,null,NaN,NaN,NaN,null,
null,null, null,null, null, null, null, null, null, null);

====== bobKeyspace.staticTest, lat and lon are static
//This shows that lat and lon just have different values for each combination of the
//partition key (deviceid+date).

INSERT INTO bobkeyspace.statictest (deviceid,date,lat,lon) VALUES
(1001,'2014-11-01T00:00:00Z',33.0,-123.0);
INSERT INTO bobkeyspace.statictest (deviceid,date,lat,lon) VALUES
(1001,'2014-11-02T00:00:00Z',34.0,-124.0);

INSERT INTO bobkeyspace.statictest (deviceid,date,sampletime,depth,u,v) VALUES
(1001,'2014-11-01T00:00:00Z','2014-11-01T01:02:03Z',[10.1,20.1,30.1],[-0.11,0.0,0.11],[-0.12,0.0,0.12]);
INSERT INTO bobkeyspace.statictest (deviceid,date,sampletime,depth,u,v) VALUES
(1001,'2014-11-01T00:00:00Z','2014-11-01T02:02:03Z',[10.1,20.1,30.1],[-0.11,0.0,0.11],[-0.12,0.0,0.12]);
INSERT INTO bobkeyspace.statictest (deviceid,date,sampletime,depth,u,v) VALUES
(1001,'2014-11-01T00:00:00Z','2014-11-01T03:03:03Z',[10.1,20.1,30.1],[-0.31,0.0,0.31],[-0.32,0.0,0.32]);
INSERT INTO bobkeyspace.statictest (deviceid,date,sampletime,depth,u,v) VALUES
(1001,'2014-11-02T00:00:00Z','2014-11-02T01:02:03Z',[10.1,20.1,30.1],[-0.41,0.0,0.41],[-0.42,0.0,0.42]);

 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import com.datastax.driver.core.*;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromCassandraHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// See notes ErddapReleaseChecklist.txt about cassandra Java driver and dependencies.
//
// http://www.datastax.com/documentation/developer/java-driver/2.0/java-driver/reference/settingUpJavaProgEnv_r.html?scroll=settingUpJavaProgEnv_r__dependencies-list
//  is for cassandra-driver-core-2.0.1.jar. (Mine is newer.)
// See setup.html Credits for information about dependencies.
//  Recommended/needed for C* Java driver:
//  netty.jar, guava.jar, metrics-core.jar,
//  slf4j.jar   //slf4j-simple-xxx.jar is needed in my /lib
//  lz4.jar, snappy-java.jar
// Changes to Java driver:
//  https://github.com/datastax/java-driver/tree/3.0/upgrade_guide

// see commented out usage below
// import com.codahale.metrics.jmx.JmxReporter;

/**
 * This class represents a table of data from Cassandra.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2014-11-03
 */
@SaxHandlerClass(EDDTableFromCassandraHandler.class)
public class EDDTableFromCassandra extends EDDTable {

  // see getSession
  private static ConcurrentHashMap<String, Session> sessionsMap = new ConcurrentHashMap();
  private static ConcurrentHashMap<String, PreparedStatement> statementMap =
      new ConcurrentHashMap();
  public static String LIST = "!!!LIST!!!";

  /** set by the constructor */
  private Session session;

  private ProtocolVersion protocolVersion =
      ProtocolVersion.NEWEST_SUPPORTED; // but may be changed below
  protected String keyspace;
  protected String tableName;
  protected int nPartitionKeys;
  protected String partitionKeyNames[]; // source names
  protected String partitionKeyFrom[]; // null or name of timestamp var it is derived from
  protected String partitionKeyPrecision[]; // null or precision of timestamp var
  protected String
      partitionKeyFixedValue[]; // null, or the fixed value (plain number or string in quotes)
  protected EDV partitionKeyEDV[]; // edv of each partitionKey
  protected static final String PartitionKeysDistinctTableName = "PartitionKeysDistinctTable.nc";
  protected HashSet<String> clusterColumnSourceNames;
  protected HashSet<String> indexColumnSourceNames;
  protected double maxRequestFraction = 1; // >0..1; 1 until subsetVarTable has been made
  protected String partitionKeyRelatedVariables; // CSSV for error message
  protected EDV rvToResultsEDV[]; // needed in expandPartitionKeyCSV
  protected String partitionKeyCSV; // null or csv before expansion

  // Double quotes, see
  // http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/escape_char_r.html
  protected String columnNameQuotes = ""; // empty string (default) or "
  protected boolean isListDV[]; // true if this dataVariable is a list dataType, e.g., doubleList

  // public static String testUser = "postgres";
  // public static String testUrl = "jdbc:postgresql://localhost:5432/mydatabase";
  // public static String testDriver = "org.postgresql.Driver";

  /**
   * This constructs an EDDTableFromCassandra based on the information in an .xml file.
   *
   * <p>Unusual: the <dataType> for the dataVariables include the regular dataTypes plus list
   * variables: booleanList, byteList, charList, shortList, intList, longList, floatList,
   * doubleList, StringList. When a list variable is in the results, each row of source data will be
   * expanded to size(list) rows of data in ERDDAP; scalars in the source data will be duplicated
   * size(list) times. If the results contain more than one list variable, all lists on a given row
   * of data MUST have the same size and MUST be "parallel" lists, i.e., a[0], b[0], c[0], ... MUST
   * all be related, and a[1], b[1], c[1], ... MUST all be related, ...
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromCassandra"&gt;
   *     having just been read.
   * @return an EDDTableFromCassandra. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromCassandra fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromCassandra(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    Attributes tGlobalAttributes = null;
    ArrayList tDataVariables = new ArrayList();
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    String tLocalSourceUrl = null;
    String tKeyspace = null;
    String tTableName = null;
    String tPartitionKeySourceNames = null;
    String tClusterColumnSourceNames = null;
    String tIndexColumnSourceNames = null;
    double tMaxRequestFraction = 1;
    String tColumnNameQuotes = ""; // default
    StringArray tConnectionProperties = new StringArray();
    boolean tSourceNeedsExpandedFP_EQ = true;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;
    String tPartitionKeyCSV = null;

    // process the tags
    String startOfTags = xmlReader.allTags();
    int startOfTagsN = xmlReader.stackSize();
    int startOfTagsLength = startOfTags.length();
    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      // if (reallyVerbose) String2.log("  tags=" + tags + content);
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String localTags = tags.substring(startOfTagsLength);

      // try to make the tag names as consistent, descriptive and readable as possible
      if (localTags.equals("<addAttributes>")) tGlobalAttributes = getAttributesFromXml(xmlReader);
      else if (localTags.equals("<dataVariable>"))
        tDataVariables.add(getSDADVariableFromXml(xmlReader));
      else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<sourceUrl>")) {
      } else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content;
      else if (localTags.equals("<connectionProperty>"))
        tConnectionProperties.add(xmlReader.attributeValue("name"));
      else if (localTags.equals("</connectionProperty>")) tConnectionProperties.add(content);
      else if (localTags.equals("<keyspace>")) {
      } else if (localTags.equals("</keyspace>")) tKeyspace = content;
      else if (localTags.equals("<tableName>")) {
      } else if (localTags.equals("</tableName>")) tTableName = content;
      else if (localTags.equals("<partitionKeySourceNames>")) {
      } else if (localTags.equals("</partitionKeySourceNames>")) tPartitionKeySourceNames = content;
      else if (localTags.equals("<clusterColumnSourceNames>")) {
      } else if (localTags.equals("</clusterColumnSourceNames>"))
        tClusterColumnSourceNames = content;
      else if (localTags.equals("<indexColumnSourceNames>")) {
      } else if (localTags.equals("</indexColumnSourceNames>")) tIndexColumnSourceNames = content;
      else if (localTags.equals("<maxRequestFraction>")) {
      } else if (localTags.equals("</maxRequestFraction>"))
        tMaxRequestFraction = String2.parseDouble(content);
      else if (localTags.equals("<columnNameQuotes>")) {
      } else if (localTags.equals("</columnNameQuotes>")) tColumnNameQuotes = content;
      else if (localTags.equals("<partitionKeyCSV>")) {
      } else if (localTags.equals("</partitionKeyCSV>")) tPartitionKeyCSV = content;
      else if (localTags.equals("<sourceNeedsExpandedFP_EQ>")) {
      } else if (localTags.equals("</sourceNeedsExpandedFP_EQ>"))
        tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<sosOfferingPrefix>")) {
      } else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content;
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<addVariablesWhere>")) {
      } else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content;
      else xmlReader.unexpectedTagException();
    }
    int ndv = tDataVariables.size();
    Object ttDataVariables[][] = new Object[ndv][];
    for (int i = 0; i < tDataVariables.size(); i++)
      ttDataVariables[i] = (Object[]) tDataVariables.get(i);

    return new EDDTableFromCassandra(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        ttDataVariables,
        tReloadEveryNMinutes,
        tLocalSourceUrl,
        tConnectionProperties.toArray(),
        tKeyspace,
        tTableName,
        tPartitionKeySourceNames,
        tClusterColumnSourceNames,
        tIndexColumnSourceNames,
        tPartitionKeyCSV,
        tMaxRequestFraction,
        tColumnNameQuotes,
        tSourceNeedsExpandedFP_EQ);
  }

  /**
   * The constructor. See general documentation in EDDTable.java and specific documentation in
   * setupDatasetsXml.html.
   *
   * @throws Throwable if trouble
   */
  public EDDTableFromCassandra(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      Attributes tAddGlobalAttributes,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      String tLocalSourceUrl,
      String tConnectionProperties[],
      String tKeyspace,
      String tTableName,
      String tPartitionKeySourceNames,
      String tClusterColumnSourceNames,
      String tIndexColumnSourceNames,
      String tPartitionKeyCSV,
      double tMaxRequestFraction,
      String tColumnNameQuotes,
      boolean tSourceNeedsExpandedFP_EQ)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromCassandra " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromCassandra(" + tDatasetID + ") constructor:\n";
    int language = 0; // for constructor

    // save some of the parameters
    className = "EDDTableFromCassandra";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    sosOfferingPrefix = tSosOfferingPrefix;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new Attributes();
    addGlobalAttributes = tAddGlobalAttributes;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    Test.ensureNotNothing(tLocalSourceUrl, "'sourceUrl' wasn't defined.");
    localSourceUrl = tLocalSourceUrl;
    publicSourceUrl = "(Cassandra)"; // not tLocalSourceUrl; keep it private
    addGlobalAttributes.set("sourceUrl", publicSourceUrl);
    partitionKeyCSV = String2.isSomething(tPartitionKeyCSV) ? tPartitionKeyCSV : null;

    // connectionProperties may have secret (username and password)!
    // So use then throw away.
    if (tConnectionProperties == null) tConnectionProperties = new String[0];
    Test.ensureTrue(
        !Math2.odd(tConnectionProperties.length),
        "connectionProperties.length must be an even number.");
    ConcurrentHashMap<String, String> connectionProperties = new ConcurrentHashMap();
    for (int i = 0; i < tConnectionProperties.length; i += 2) {
      String tKey = tConnectionProperties[i];
      String tValue = tConnectionProperties[i + 1];
      if (String2.isSomething(tKey) && tValue != null)
        connectionProperties.put(tKey, tValue); // <String,String>
    }
    Test.ensureNotNothing(tKeyspace, "'keyspace' wasn't defined.");
    keyspace = tKeyspace;
    Test.ensureNotNothing(tTableName, "'tableName' wasn't defined.");
    tableName = tTableName;

    Test.ensureNotNothing(tPartitionKeySourceNames, "'partitionKeySourceNames' wasn't defined.");
    partitionKeyNames = String2.split(tPartitionKeySourceNames, ','); // they are trimmed
    nPartitionKeys = partitionKeyNames.length;
    partitionKeyFrom = new String[nPartitionKeys]; // all nulls
    partitionKeyPrecision = new String[nPartitionKeys]; // all nulls
    partitionKeyFixedValue = new String[nPartitionKeys]; // all nulls
    for (int i = 0; i < nPartitionKeys; i++) {
      // timestamp derived from another timestamp?  date/sampletime/1970-01-01Z
      String sar[] = String2.split(partitionKeyNames[i], '/'); // they are trimmed
      if (sar.length == 1) {
        // fixed value? deviceid=1007  or deviceid="CA107"
        sar = String2.split(partitionKeyNames[i], '='); // they are trimmed
        if (sar.length == 1) {
          sar[0] = String2.canonical(sar[0]);
        } else if (sar.length == 2) {
          partitionKeyNames[i] = String2.canonical(sar[0]);
          partitionKeyFixedValue[i] = sar[1];
        }
      } else if (sar.length == 3) {
        partitionKeyNames[i] = String2.canonical(sar[0]);
        partitionKeyFrom[i] = String2.canonical(sar[1]);
        partitionKeyPrecision[i] = String2.canonical(sar[2]);
      } else {
        throw new RuntimeException(
            String2.ERROR
                + ": Invalid '/' usage in partitionKeys for \""
                + partitionKeyNames[i]
                + "\".");
      }
    }

    clusterColumnSourceNames = new HashSet();
    if (String2.isSomething(tClusterColumnSourceNames)) {
      String sar[] = String2.split(tClusterColumnSourceNames, ','); // they are trimmed
      for (String s : sar) clusterColumnSourceNames.add(String2.canonical(s));
    }

    indexColumnSourceNames = new HashSet();
    if (String2.isSomething(tIndexColumnSourceNames)) {
      String sar[] = String2.split(tIndexColumnSourceNames, ','); // they are trimmed
      for (String s : sar) indexColumnSourceNames.add(String2.canonical(s));
    }

    // don't set maxRequestFraction until after subsetVariableTable has been made
    Test.ensureBetween(tMaxRequestFraction, 1e-10, 1, "Invalid maxRequestFraction");

    columnNameQuotes = tColumnNameQuotes;
    Test.ensureTrue(
        "\"".equals(columnNameQuotes) || "".equals(columnNameQuotes),
        "<columnNameQuotes> must be \" or an empty string (the default).");

    // cql can support everything except != and regex constraints
    // PARTIAL because CQL treats > like >=, and < like <=
    //  and because constraints on list variables are non-sensical until expanded in ERDDAP.
    sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
    sourceCanConstrainNumericData = CONSTRAIN_PARTIAL;
    sourceCanConstrainStringData = CONSTRAIN_PARTIAL;
    sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP;

    // set global attributes
    sourceGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");

    // create dataVariables[]
    int ndv = tDataVariables.length;
    dataVariables = new EDV[ndv];
    isListDV = new boolean[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      String tSourceName = (String) tDataVariables[dv][0];
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      String tSourceType = (String) tDataVariables[dv][3];
      // deal with <dataType>'s that are lists: byteList, doubleList, ...
      if (tSourceType.endsWith("List")) {
        isListDV[dv] = true;
        tSourceType = tSourceType.substring(0, tSourceType.length() - 4);
        if (tSourceType.equals("unsignedShort")) // the xml name
        tSourceType = "char"; // the PrimitiveArray name
        else if (tSourceType.equals("string")) // the xml name
        tSourceType = "String"; // the PrimitiveArray name
      }
      Attributes tSourceAtt = new Attributes();
      // if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType="
      // + tSourceType);

      if (EDV.LON_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVLon(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        lonIndex = dv;
      } else if (EDV.LAT_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVLat(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        latIndex = dv;
      } else if (EDV.ALT_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVAlt(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        altIndex = dv;
      } else if (EDV.DEPTH_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVDepth(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        depthIndex = dv;
      } else if (EDV.TIME_NAME.equals(
          tDestName)) { // look for TIME_NAME before check hasTimeUnits (next)
        dataVariables[dv] =
            new EDVTime(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
        timeIndex = dv;
      } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
        dataVariables[dv] =
            new EDVTimeStamp(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
      } else {
        dataVariables[dv] =
            new EDV(datasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
        dataVariables[dv].setActualRangeFromDestinationMinMax();
      }
    }

    // make/get the session (ensure it is createable)
    session = getSession(localSourceUrl, connectionProperties);
    protocolVersion =
        session.getCluster().getConfiguration().getProtocolOptions().getProtocolVersion();

    // make the primaryKey distinct table
    StringBuilder dapVars = new StringBuilder();
    StringBuilder cassVars = new StringBuilder();
    StringBuilder dapConstraints = new StringBuilder();
    StringBuilder cassConstraints = new StringBuilder();
    int resultsDVI[] = new int[nPartitionKeys];
    rvToResultsEDV = new EDV[nPartitionKeys]; // make and keep this
    File2.makeDirectory(datasetDir());
    partitionKeyEDV = new EDV[nPartitionKeys];
    StringArray pkRelated = new StringArray();
    for (int pki = 0; pki < nPartitionKeys; pki++) {
      String tSourceName = partitionKeyNames[pki];
      resultsDVI[pki] = String2.indexOf(dataVariableSourceNames(), tSourceName);
      rvToResultsEDV[pki] = dataVariables[resultsDVI[pki]];
      if (resultsDVI[pki] < 0)
        throw new RuntimeException(
            String2.ERROR
                + ": sourceName="
                + tSourceName
                + " not found in sourceNames="
                + String2.toCSSVString(dataVariableSourceNames()));
      EDV edv = dataVariables[resultsDVI[pki]];
      partitionKeyEDV[pki] = edv;
      pkRelated.add(edv.destinationName());
      dapVars.append((pki == 0 ? "" : ",") + edv.destinationName());
      cassVars.append(
          (pki == 0 ? "SELECT DISTINCT " : ",")
              + columnNameQuotes
              + tSourceName
              + columnNameQuotes);

      String pkfv = partitionKeyFixedValue[pki];
      if (pkfv != null) {
        // constrain dap, don't constrain Cassandra (it refuses)
        dapConstraints.append("&" + edv.destinationName() + "=" + pkfv);
      }

      int dvi = String2.indexOf(dataVariableSourceNames(), partitionKeyFrom[pki]);
      if (dvi >= 0) pkRelated.add(dataVariables[dvi].destinationName());
    }
    partitionKeyRelatedVariables = pkRelated.toString();
    String dapQuery = dapVars.toString() + dapConstraints.toString() + "&distinct()";
    String cassQuery =
        cassVars.toString() + " FROM " + keyspace + "." + tableName + cassConstraints.toString();
    if (verbose)
      String2.log(
          "* PrimaryKeys DAP  query=" + dapQuery + "\n" + "* PrimaryKeys Cass query=" + cassQuery);
    Table cumTable;
    if (String2.isSomething(partitionKeyCSV)) {
      // do this here just to ensure expansion doesn't throw exception
      cumTable = expandPartitionKeyCSV();

    } else {
      // ask Cassandra
      TableWriterAll twa =
          new TableWriterAll(
              language,
              null,
              null, // metadata not relevant
              datasetDir(),
              "tPKDistinct");
      SimpleStatement statement = new SimpleStatement(cassQuery);
      Table table = makeEmptySourceTable(rvToResultsEDV, 1024);
      table =
          getDataForCassandraQuery(
              language,
              EDStatic.loggedInAsSuperuser,
              "irrelevant",
              dapQuery,
              resultsDVI,
              rvToResultsEDV,
              session,
              statement,
              table,
              twa,
              new int[4]);
      if (twa.noMoreDataPlease)
        throw new RuntimeException("Too many primary keys?! TableWriterAll said NoMoreDataPlease.");
      preStandardizeResultsTable(EDStatic.loggedInAsSuperuser, table);
      if (table.nRows() > 0) {
        standardizeResultsTable(0, "irrelevant", dapQuery, table);
        twa.writeSome(table);
      }
      twa.finish();
      cumTable = twa.cumulativeTable();
      twa.releaseResources();
      cumTable.leftToRightSortIgnoreCase(nPartitionKeys); // useful: now in sorted order

      // save in flatNc file
      cumTable.saveAsFlatNc(
          datasetDir() + PartitionKeysDistinctTableName,
          "row",
          false); // convertToFakeMissingValues
    }
    // cumTable is sorted and distinct
    String2.log(PartitionKeysDistinctTableName + " nRows=" + cumTable.nRows());
    if (verbose)
      String2.log(
          "first few rows of partitionKeysTable ('rows' is for info only)\n"
              + cumTable.dataToString(10));
    cumTable = null;

    // gather ERDDAP sos information?
    // assume time column is indexed? so C* can return min/max efficiently

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // set after subsetVariablesTable has been made
    maxRequestFraction = tMaxRequestFraction;

    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromCassandra "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  @Override
  public boolean knowsActualRange() {
    return false;
  } // because this gets info from Cassandra

  /**
   * Expand partitionKeyCSV. This uses class variables: partitionKeyCSV and rvToResultsEDV.
   *
   * @return the expanded primaryKey table
   * @throws Exception if trouble
   */
  protected Table expandPartitionKeyCSV() throws Exception {

    Test.ensureNotNull(partitionKeyCSV, "partitionKeyCSV is null. Shouldn't get here.");
    Test.ensureNotNull(rvToResultsEDV, "rvToResultsEDV is null. Shouldn't get here.");

    // partitionKeyCSV specified in datasets.xml
    //  deviceid,date
    //  1001,times(2016-01-05T07:00:00Z,60,now-1minute)
    //  1007,2014-11-07T00:00:00Z           //1.4153184E9
    Table table = new Table();
    table.readASCII(
        "<partitionKeyCSV>",
        new BufferedReader(new StringReader(partitionKeyCSV)),
        "",
        "",
        0,
        1,
        ",",
        null,
        null,
        null,
        null,
        false); // simplify
    if (debugMode) {
      String2.log(">> <partitionKeyCSV> as initially parsed:");
      String2.log(table.dataToString());
    }

    // make cumTable
    Table cumTable = makeEmptySourceTable(rvToResultsEDV, 1024);
    // ensure correct/expected columns
    Test.ensureEqual(
        table.getColumnNamesCSVString(),
        cumTable.getColumnNamesCSVString(),
        "The <partitionKeyCSV> column names must match the required column names.");

    // transfer data to cumTable, expanding as needed
    String errMsg =
        "In <partitionKeyCSV>: Invalid times(startTimeString, strideSeconds, stopTimeString) data: ";
    int tnRows = table.nRows();
    int tnCols = table.nColumns();
    for (int row = 0; row < tnRows; row++) {
      for (int col = 0; col < tnCols; col++) {
        PrimitiveArray pa = cumTable.getColumn(col);
        String s = table.getStringData(col, row);
        if (s.startsWith("times(") && s.endsWith(")")) {
          String parts[] = String2.split(s.substring(6, s.length() - 1), ',');
          if (parts.length != 3) throw new RuntimeException(errMsg + s);
          double epSecStart = Calendar2.safeIsoStringToEpochSeconds(parts[0]);
          double strideSec = String2.parseDouble(parts[1]);
          double epSecStop =
              parts[2].toLowerCase().startsWith("now")
                  ? Calendar2.safeNowStringToEpochSeconds(parts[2], Double.NaN)
                  : Calendar2.safeIsoStringToEpochSeconds(parts[2]);
          if (!Double.isFinite(epSecStart)
              || !Double.isFinite(epSecStop)
              || !Double.isFinite(strideSec)
              || epSecStart > epSecStop
              || strideSec <= 0) throw new RuntimeException(errMsg + s);
          for (int ti = 0; ti < 10000000; ti++) {
            // do it this way to minimize rounding errors
            double d = epSecStart + ti * strideSec;
            if (d > epSecStop) break;
            pa.addDouble(d);
          }
        } else if (s.startsWith("time(") && s.endsWith(")")) {
          double d = Calendar2.safeIsoStringToEpochSeconds(s.substring(5, s.length() - 1));
          if (!Double.isFinite(d)) throw new RuntimeException(errMsg + s);
          pa.addDouble(d);
        } else {
          pa.addString(s); // converts to correct type
        }
      }

      // expand non-expanded columns
      cumTable.ensureColumnsAreSameSize_LastValue();
    }
    return cumTable;
  }

  /**
   * This gets/makes a session for the specified url. Unlike database sessions, a Cassandra session
   * is thread-safe and very robust (e.g., transparent failover), so it can be used by multiple
   * users for multiple datasets. See claims at
   * http://www.datastax.com/documentation/developer/java-driver/2.1/common/drivers/introduction/introArchOverview_c.html
   * See the Four Simple Rules for using Cassandra clusters and sessions
   * http://www.datastax.com/documentation/developer/java-driver/2.1/java-driver/fourSimpleRules.html
   * <br>
   * 1) "Use one cluster instance per (physical) cluster (per application lifetime) <br>
   * 2) Use at most one session instance per keyspace, or use a single Session and explicitely
   * specify the keyspace in your queries <br>
   * 3) If you execute a statement more than once, consider using a prepared statement <br>
   * 4) You can reduce the number of network roundtrips and also have atomic operations by using
   * batches" Because of #2, ERDDAP just creates one session per url. The sessionMap's key=url,
   * value=session.
   */
  public static Session getSession(
      String url, ConcurrentHashMap<String, String> connectionProperties) throws Throwable {

    Session session = sessionsMap.get(url);
    if (session != null) return session;

    if (verbose) String2.log("EDDTableFromCassandra.getSession");
    long tTime = System.currentTimeMillis();
    // see
    // http://www.datastax.com/documentation/developer/java-driver/2.1/java-driver/fourSimpleRules.html
    Cluster.Builder builder = Cluster.builder().addContactPoint(url);

    // options
    // Note that any tag with no value causes nothing to be done.
    // http://www.datastax.com/drivers/java/2.1/com/datastax/driver/core/Cluster.Builder.html
    String opt, ts;
    int ti;
    boolean tb;

    // directly set options

    opt = "compression";
    if (verbose) String2.log("  " + opt + " default=NONE"); // NONE
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts)) {
      builder.withCompression(ProtocolOptions.Compression.valueOf(ts.toUpperCase()));
      if (verbose) String2.log("  " + opt + "  set to " + ts.toUpperCase());
    }

    opt = "credentials";
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts)) {
      ts = ts.trim();
      int po = ts.indexOf('/'); // first instance
      if (po > 0) builder.withCredentials(ts.substring(0, po), ts.substring(po + 1));
      else
        throw new SimpleException(
            "ERROR: Invalid connectionProperty value for name=credentials. "
                + "Expected: \"name/password\" (that's a literal '/').");
      if (verbose) String2.log("  " + opt + " was set.");
    }

    // 2021-01-25 to deal with java.lang.NoClassDefFoundError: com/codahale/metrics/JmxReporter
    // metrics is now always false.
    // https://docs.datastax.com/en/developer/java-driver/3.5/manual/metrics/#metrics-4-compatibility
    opt = "metrics";
    // if (verbose) String2.log("  " + opt + " default=true");
    ts = connectionProperties.get(opt);
    // if (String2.isSomething(ts) && !String2.parseBoolean(ts)) { //unusual
    builder.withoutMetrics(); // set it to false
    if (verbose) String2.log("  " + opt + "  set to false (always)");
    // } else {
    //    builder.withoutJMXReporting();
    //    JmxReporter reporter = JmxReporter.forRegistry(builder.getMetrics().getRegistry())
    //        .inDomain(builder.getClusterName() + "-metrics")
    //        .build();
    //    reporter.start();
    // }

    opt = "port";
    if (verbose) String2.log("  " + opt + " default=9042");
    ti = String2.parseInt(connectionProperties.get(opt));
    if (ti >= 0 && ti < Integer.MAX_VALUE) {
      builder.withPort(ti); // unusual
      if (verbose) String2.log("  " + opt + "  set to " + ti);
    }

    opt = "ssl";
    if (verbose) String2.log("  " + opt + " default=false");
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts) && String2.parseBoolean(ts)) { // unusual
      builder.withSSL();
      if (verbose) String2.log("  " + opt + "  set to true");
    }

    // QueryOptions
    QueryOptions queryOpt = new QueryOptions(); // has defaults

    opt = "consistencyLevel";
    if (verbose) String2.log("  " + opt + " default=" + queryOpt.getConsistencyLevel()); // ONE
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts)) {
      queryOpt.setConsistencyLevel(ConsistencyLevel.valueOf(ts.toUpperCase()));
      if (verbose) String2.log("  " + opt + "  set to " + ts.toUpperCase());
    }

    opt = "fetchSize";
    if (verbose) String2.log("  " + opt + " default=" + queryOpt.getFetchSize()); // 5000
    ti = String2.parseInt(connectionProperties.get(opt));
    if (ti >= 10 && ti < Integer.MAX_VALUE) {
      queryOpt.setFetchSize(ti);
      if (verbose) String2.log("  " + opt + "  set to " + ti);
    }

    opt = "serialConsistencyLevel";
    if (verbose)
      String2.log("  " + opt + " default=" + queryOpt.getSerialConsistencyLevel()); // SERIAL
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts)) {
      queryOpt.setSerialConsistencyLevel(ConsistencyLevel.valueOf(ts.toUpperCase()));
      if (verbose) String2.log("  " + opt + "  set to " + ts.toUpperCase());
    }

    builder = builder.withQueryOptions(queryOpt);

    // socketOptions
    SocketOptions socOpt = new SocketOptions(); // has defaults

    opt = "connectTimeoutMillis";
    if (verbose) String2.log("  " + opt + " default=" + socOpt.getConnectTimeoutMillis()); // 5000
    ti = String2.parseInt(connectionProperties.get(opt));
    if (ti >= 1000 && ti < Integer.MAX_VALUE) {
      socOpt.setConnectTimeoutMillis(ti);
      if (verbose)
        String2.log("  " + opt + "  set to " + ti + " " + socOpt.getConnectTimeoutMillis());
    }

    opt = "keepAlive";
    if (verbose) String2.log("  " + opt + " default=" + socOpt.getKeepAlive()); // null
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts)) {
      tb = String2.parseBoolean(ts);
      socOpt.setKeepAlive(tb);
      if (verbose) String2.log("  " + opt + "  set to " + tb);
    }

    opt = "readTimeoutMillis";
    if (verbose) String2.log("  " + opt + " default=" + socOpt.getReadTimeoutMillis()); // 12000
    ti = String2.parseInt(connectionProperties.get(opt));
    if (ti >= 1000 && ti < Integer.MAX_VALUE)
      ti = 120000; // ERDDAP changes Cassandra's default if the dataset doesn't
    socOpt.setReadTimeoutMillis(ti);
    if (verbose) String2.log("  " + opt + "  set to " + ti);

    opt = "receiveBufferSize";
    if (verbose) String2.log("  " + opt + " default=" + socOpt.getReceiveBufferSize()); // null
    ti = String2.parseInt(connectionProperties.get(opt));
    if (ti >= 10000 && ti < Integer.MAX_VALUE) {
      socOpt.setReceiveBufferSize(ti);
      if (verbose) String2.log("  " + opt + "  set to " + ti);
    }

    opt = "soLinger";
    if (verbose) String2.log("  " + opt + " default=" + socOpt.getSoLinger()); // null
    ti = String2.parseInt(connectionProperties.get(opt));
    if (ti >= 1 && ti < Integer.MAX_VALUE) {
      socOpt.setSoLinger(ti);
      if (verbose) String2.log("  " + opt + "  set to " + ti);
    }

    opt = "tcpNoDelay";
    if (verbose) String2.log("  " + opt + " default=" + socOpt.getTcpNoDelay()); // null
    ts = connectionProperties.get(opt);
    if (String2.isSomething(ts)) {
      tb = String2.parseBoolean(ts);
      socOpt.setTcpNoDelay(tb);
      if (verbose) String2.log("  " + opt + "  set to " + tb);
    }

    builder = builder.withSocketOptions(socOpt);

    // build the cluster
    Cluster cluster = builder.build();
    if (verbose) {
      Metadata metadata = cluster.getMetadata();
      String2.log("clusterName=" + metadata.getClusterName());
      for (Host host : metadata.getAllHosts())
        String2.log(
            "datacenter="
                + host.getDatacenter()
                + " host="
                + host.getEndPoint().resolve().getAddress()
                + " rack="
                + host.getRack());
    }

    session = cluster.connect();
    sessionsMap.put(url, session);
    if (verbose) String2.log("  Success! time=" + (System.currentTimeMillis() - tTime) + "ms");
    return session;
  }

  /** This shuts down all of this program's connections to Cassandra clusters and sessions. */
  public static void shutdown() {
    String2.log("starting EDDTableFromCassandra.shutdown()");
    long time = System.currentTimeMillis();
    try {
      if (sessionsMap == null) return;
      Session sessions[] = sessionsMap.values().toArray(new Session[0]);
      for (Session session : sessions) {
        try {
          session.getCluster().close(); // or are they all the same cluster?
        } catch (Throwable t) {
          String2.log(MustBe.throwableToString(t));
        }
      }
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
    }

    // Aid in garbage collection. Prevent other threads from initiating clusters and sessions.
    sessionsMap = null;
    String2.log(
        "EDDTableFromCassandra.shutdown() finished in "
            + (System.currentTimeMillis() - time)
            + "ms");
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * <p>See CQL SELECT documentation
   * http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/select_r.html
   *
   * <p>The method prevents CQL Injection Vulnerability (see
   * https://en.wikipedia.org/wiki/SQL_injection) by using QueryBuilder (so String values are
   * properly escaped and numbers are assured to be numbers). See
   * http://www.datastax.com/documentation/developer/java-driver/2.0/pdf/javaDriver20.pdf
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    // get the sourceDapQuery (a query that the source can handle)
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    getSourceQueryFromDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        // timeStamp constraints other than regex are epochSeconds
        constraintVariables,
        constraintOps,
        constraintValues);

    // apply constraints to PartitionKeysDistinctTable
    Table pkdTable;
    if (partitionKeyCSV == null) {
      pkdTable = new Table();
      pkdTable.readFlatNc(
          datasetDir() + PartitionKeysDistinctTableName, partitionKeyNames, 0); // standardizeWhat=0
    } else {
      pkdTable = expandPartitionKeyCSV();
    }
    int oPkdTableNRows = pkdTable.nRows();
    BitSet pkdKeep = new BitSet();
    pkdKeep.set(0, oPkdTableNRows); // all true
    boolean allConstraintsHandled = true;
    int nCon = constraintVariables.size();
    BitSet conKeep = new BitSet(nCon); // all false
    for (int cv = 0; cv < nCon; cv++) {
      String cVar = constraintVariables.get(cv); // sourceName
      String cOp = constraintOps.get(cv);
      String cVal = constraintValues.get(cv);
      double cValD = String2.parseDouble(cVal);
      int dv = String2.indexOf(dataVariableSourceNames, cVar);
      boolean isNumericEDV = dataVariables[dv].sourceDataPAType() != PAType.STRING;

      // for WHERE below, just keep constraints applicable to:
      //  clusterColumnSourceNames (ops: = > >= < <= ) or
      //  indexColumnSourceNames (ops: = )
      conKeep.set(
          cv,
          (clusterColumnSourceNames.contains(cVar)
                  && !cOp.equals("!=")
                  && !cOp.equals(PrimitiveArray.REGEX_OP)
                  && !(isNumericEDV && !Double.isFinite(cValD)))
              || // don't constrain numeric cols with NaN
              (indexColumnSourceNames.contains(cVar)
                  && cOp.equals("=")
                  && // secondary index column only allow '=' constraints
                  !(isNumericEDV
                      && !Double.isFinite(cValD)))); // don't constrain numeric cols with NaN

      // Is this a constraint directly on a partitionKey?
      int pkin = String2.indexOf(partitionKeyNames, cVar); // Names!
      if (pkin < 0) // no, it isn't
      allConstraintsHandled = false;

      // is it a special timestamp var?
      // (fixed value partition variables already constrained during
      //  creation of partitionKeyDistinctTable)
      int pkif = String2.indexOf(partitionKeyFrom, cVar); // From!
      if (pkif >= 0 && !cOp.equals(PrimitiveArray.REGEX_OP) && Double.isFinite(cValD)) {
        // cVal is epoch seconds
        String origCon = cVar + cOp + cVal;
        cVar = partitionKeyNames[pkif];
        cValD =
            Calendar2.isoStringToEpochSeconds(
                Calendar2.epochSecondsToLimitedIsoStringT(partitionKeyPrecision[pkif], cValD, ""));
        cVal = "" + cValD;
        if (reallyVerbose)
          String2.log("timestamp conversion from " + origCon + " to " + cVar + cOp + cVal);
      }

      // try to apply it (even != and regex_op can be applied here)
      int tNRows = pkdTable.tryToApplyConstraint(-1, cVar, cOp, cVal, pkdKeep);
      // String2.log("After " + cVar + cOp + cVal + " nRows=" + tNRows);
      if (tNRows == 0)
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (no matching partition key values)");
    }

    // compact the pkdTable (needs a lot of memory and will be in memory for a long time)
    pkdTable.justKeep(pkdKeep);
    int pkdTableNRows = pkdTable.nRows();
    // diagnostics printed below
    double fraction = pkdTableNRows / (double) oPkdTableNRows;
    if (fraction > maxRequestFraction)
      throw new SimpleException(
          "You are requesting too much data. "
              + "Please further constrain one or more of these variables: "
              + partitionKeyRelatedVariables
              + ". ("
              + pkdTableNRows
              + "/"
              + oPkdTableNRows
              + "="
              + fraction
              + " > "
              + maxRequestFraction
              + ")");

    // compact constraints
    constraintVariables.justKeep(conKeep);
    constraintOps.justKeep(conKeep);
    constraintValues.justKeep(conKeep);
    nCon = constraintVariables.size();

    // distinct? orderBy?
    // ERDDAP handles them because Cassandra can only handle some cases.
    // http://planetcassandra.org/blog/composite-keys-in-apache-cassandra/
    // says, to use "ORDER BY in queries on a table, then you will have to use
    //  composite-key in that table and that composite key must include the
    //  field that you wish to sort on. You have to decide which fields
    //  you wish to sort on when you design your data model, not when you
    //  formulate queries."
    // FOR NOW, have ERDDAP TableWriters handle them, not Cassandra

    // build the statement
    // only BoundStatement lets me bind values one at a time
    // http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/select_r.html
    StringBuilder query = new StringBuilder();
    int nRv = resultsVariables.size();
    boolean allRvAreInPkdTable = true;
    for (int rv = 0; rv < nRv; rv++) {
      // no danger of cql injection since query has been parsed
      //  so resultsVariables must be known sourceNames
      // Note that I tried to use '?' for resultsVariables, but never got it to work: wierd results.
      // Quotes around colNames avoid trouble when colName is a CQL reserved word.
      query.append(
          (rv == 0 ? "SELECT " : ", ")
              + columnNameQuotes
              + resultsVariables.get(rv)
              + columnNameQuotes);
      if (allRvAreInPkdTable
          && // no sense in looking
          pkdTable.findColumnNumber(resultsVariables.get(rv)) < 0) allRvAreInPkdTable = false;
    }

    // are we done?  allConstraintsHandled and all resultsVars are in pkdTable
    if (allConstraintsHandled && allRvAreInPkdTable) {
      if (verbose) String2.log("Request handled by partitionKeyDistinctTable.");
      preStandardizeResultsTable(loggedInAs, pkdTable);
      standardizeResultsTable(language, requestUrl, userDapQuery, pkdTable);
      tableWriter.writeSome(pkdTable);
      tableWriter.finish();
      return;
    }

    // Lack of quotes around table names means they can't be CQL reserved words.
    // (If do quote in future, quote individual parts.)
    query.append(" FROM " + keyspace + "." + tableName);

    // add partitionKey constraints to query:
    // 1 constraint per partitionKeyName (all are simple: name=value)
    // and gather pa's from the pkdTable
    PrimitiveArray pkdPA[] = new PrimitiveArray[nPartitionKeys];
    for (int pki = 0; pki < nPartitionKeys; pki++) {
      // again, no danger of cql injection since query has been parsed and
      //  constraintVariables must be known sourceNames
      // Double quotes around colNames avoid trouble when colName is a CQL
      //  reserved word or has odd characters.
      // http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/escape_char_r.html
      query.append(
          (pki == 0 ? " WHERE " : " AND ")
              + columnNameQuotes
              + partitionKeyNames[pki]
              + columnNameQuotes
              + " = ?"); // ? is the place holder for a value
      pkdPA[pki] = pkdTable.getColumn(pki);
    }

    // add constraints on clusterColumnSourceNames
    EDV conEDV[] = new EDV[nCon];
    for (int cv = 0; cv < nCon; cv++) {
      String conVar = constraintVariables.get(cv);
      int dv = String2.indexOf(dataVariableSourceNames(), conVar);
      conEDV[cv] = dataVariables[dv];

      // again, no danger of cql injection since query has been parsed and
      //  constraintVariables must be known sourceNames
      // Double quotes around colNames avoid trouble when colName is a CQL
      //  reserved word or has odd characters.
      // http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/escape_char_r.html
      query.append(
          " AND "
              + columnNameQuotes
              + conVar
              + columnNameQuotes
              + " "
              + constraintOps.get(cv)
              + " ?"); // ? is the place holder for a value
    }

    // LIMIT?
    //  The default row LIMIT is 10000. We want them all!
    //  Some documentation says this limits the number of columns.
    //  Some documentation says this limits the number of rows.
    //  I think it is the number of columns in a column family (e.g., 1 partition key)
    //    which are like rows in a database.
    //  https://stackoverflow.com/questions/25567518/cassandra-cql3-select-statement-without-limit
    //  Asks ~ Do I have to use a huge LIMIT to get all the rows?
    //  Answers: This is a common misconception. There is only a default 10000
    //    row limit in cqlsh the interactive shell. The server and protocol
    //    do not have a default or maximum number of rows that can be returned.
    //    There is a timeout though which will stop running queries to protect
    //    users from running malformed queries which could cause system instability.
    // 2014-11-19 I removed: LIMIT 2000000000, which caused problems at ONC

    // ALLOW FILTERING causes Cassandra to allow some requests it might not
    //  normally allow because of "performance unpredictability".
    // http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/select_r.html
    query.append(" ALLOW FILTERING;");
    if (verbose) String2.log("statement as text: " + query.toString());

    // Cassandra doesn't like preparing the same query more than once.
    // "WARNING: Re-preparing already prepared query SELECT deviceid, sampletime, ctext
    //  FROM bobKeyspace.bobTable WHERE deviceid = ? AND date = ? ALLOW FILTERING;.
    //  Please note that preparing the same query more than once is generally an
    //  anti-pattern and will likely affect performance. Consider preparing the
    //  statement only once."
    //  And see https://datastax-oss.atlassian.net/browse/JAVA-236
    // So ERDDAP caches session+query->PreparedStatement.
    //  I can't solve the problem further (cache is lost when ERDDAP is restarted)
    //  without great effort.  I'm not even sure it is possible (de/serialize?).
    //  Restarting creates new sessions. Maybe that solves the problem.
    String queryString = query.toString();
    // PreparedStatements should be unique for a given session.
    // It is the session that prepares the statement.
    // queryString includes the keyspace.tablename.
    // Session unique for a given localSourceUrl.
    String tKey = localSourceUrl + "\n" + queryString;
    PreparedStatement preparedStatement = statementMap.get(tKey);
    if (preparedStatement == null) {
      preparedStatement = session.prepare(queryString);
      statementMap.put(tKey, preparedStatement);
    }
    // preparedStatement.toString() is useless

    // gather the dataVariables[i] of each resultsVaraible
    int resultsDVI[] = new int[nRv]; // dataVariables[i] (DVI) for each resultsVariable
    EDV rvToResultsEDV[] = new EDV[nRv];
    for (int rv = 0; rv < nRv; rv++) {
      String tName = resultsVariables.get(rv); // a sourceName
      resultsDVI[rv] = String2.indexOf(dataVariableSourceNames, tName);
      rvToResultsEDV[rv] = dataVariables[resultsDVI[rv]];
    }
    // triggerNRows + 1000 since lists expand, so hard to catch exactly
    int triggerNRows = EDStatic.partialRequestMaxCells / nRv;
    Table table = makeEmptySourceTable(rvToResultsEDV, triggerNRows + 1000);

    // make a call to Cassandra for each row in pkdTable
    // (each relevant distinct combination of partitionKey values)
    int stats[] = new int[4]; // all 0's
    for (int pkdRow = 0; pkdRow < pkdTableNRows; pkdRow++) { // chunks will be in sorted order, yea!

      if (Thread.currentThread().isInterrupted())
        throw new SimpleException(
            "EDDTableFromCassandra.getDataForDapQuery" + EDStatic.caughtInterruptedAr[0]);

      // Make the BoundStatement
      // ***!!! This method avoids CQL/SQL Injection Vulnerability !!!***
      // (see https://en.wikipedia.org/wiki/SQL_injection) by using
      // preparedStatements (so String values are properly escaped and
      // numbers are assured to be numbers).
      // *** Plus, the statement is reused many times (so Prepared is recommended).
      BoundStatement boundStatement = new BoundStatement(preparedStatement);

      // assign values to nPartitionKeys constraints then nCon constraints
      StringBuilder requestSB =
          reallyVerbose ? new StringBuilder(">> statement: pkdRow=" + pkdRow + ", ") : null;
      for (int i = 0; i < nPartitionKeys + nCon; i++) {
        boolean usePK = i < nPartitionKeys;
        int coni = i - nPartitionKeys; // which con to use: only used if not !usePK

        EDV edv = usePK ? partitionKeyEDV[i] : conEDV[coni];
        PrimitiveArray pa = usePK ? pkdPA[i] : null;
        PAType tPAType = edv.sourceDataPAType();
        String conVal = usePK ? null : constraintValues.get(coni);
        if (requestSB != null)
          requestSB.append(
              edv.sourceName() + " is " + (usePK ? pa.getDouble(pkdRow) : conVal) + ", ");

        // handle special cases first
        if (edv instanceof EDVTimeStamp) {
          boundStatement.setTimestamp(
              i, // partition key value won't be nan/null
              new Date(
                  Math.round(
                      (usePK ? pa.getDouble(pkdRow) : String2.parseDouble(conVal))
                          * 1000))); // round to nearest milli

        } else if (edv.isBoolean()) {
          boundStatement.setBool(
              i, (usePK ? pa.getInt(pkdRow) == 1 : String2.parseBoolean(conVal)));
        } else if (tPAType == PAType.DOUBLE
            || tPAType == PAType.ULONG) { // trouble: loss of precision
          boundStatement.setDouble(i, (usePK ? pa.getDouble(pkdRow) : String2.parseDouble(conVal)));
        } else if (tPAType == PAType.FLOAT) {
          boundStatement.setFloat(i, (usePK ? pa.getFloat(pkdRow) : String2.parseFloat(conVal)));
        } else if (tPAType == PAType.LONG || tPAType == PAType.UINT) { // ???
          boundStatement.setLong(i, (usePK ? pa.getLong(pkdRow) : String2.parseLong(conVal)));
        } else if (tPAType == PAType.INT
            || tPAType == PAType.SHORT
            || tPAType == PAType.USHORT
            || // ???
            tPAType == PAType.BYTE
            || tPAType == PAType.UBYTE) { // ???
          boundStatement.setInt(i, (usePK ? pa.getInt(pkdRow) : String2.parseInt(conVal)));
        } else {
          String val = usePK ? pa.getString(pkdRow) : conVal;
          if (tPAType == PAType.STRING) boundStatement.setString(i, val);
          else if (tPAType == PAType.CHAR)
            boundStatement.setString(
                i, val.length() == 0 ? "\u0000" : val.substring(0, 1)); // FFFF???
          else
            throw new RuntimeException(
                "Unexpected dataType="
                    + edv.sourceDataType()
                    + "for var="
                    + edv.destinationName()
                    + ".");
        }
      }
      // boundStatement.toString() is useless
      if (requestSB != null) String2.log(requestSB.toString());

      // get the data
      // FUTURE: I think this could be parallelized. See EDDTableFromFiles.
      table =
          getDataForCassandraQuery(
              language,
              loggedInAs,
              requestUrl,
              userDapQuery,
              resultsDVI,
              rvToResultsEDV,
              session,
              boundStatement,
              table,
              tableWriter,
              stats);
      if (tableWriter.noMoreDataPlease) break;
    }

    // write any data remaining in table
    // C* doesn't seem to have resultSet.close, statement.close(), ...
    // (In any case, gc should close them.)
    if (!tableWriter.noMoreDataPlease) {
      preStandardizeResultsTable(loggedInAs, table);
      if (table.nRows() > 0) {
        // String2.log("preStandardize=\n" + table.dataToString());
        standardizeResultsTable(language, requestUrl, userDapQuery, table);
        stats[3] += table.nRows();
        tableWriter.writeSome(table); // ok if 0 rows
      }
    }
    if (verbose)
      String2.log(
          "* Cassandra stats: partitionKeyTable: "
              + pkdTableNRows
              + "/"
              + oPkdTableNRows
              + "="
              + fraction
              + " <= "
              + maxRequestFraction
              + " nCassRows="
              + stats[1]
              + " nErddapRows="
              + stats[2]
              + " nRowsToUser="
              + stats[3]);
    tableWriter.finish();
  }

  /**
   * This executes the query statement and may write some data to the tablewriter. This doesn't call
   * tableWriter.finish();
   *
   * @param language the index of the selected language
   * @param resultsDVI dataVariables[i] (DVI) for each resultsVariable
   * @param table May have some not-yet-tableWritten data when coming in. May have some
   *     not-yet-tableWritten data when returning.
   * @param stats is int[4]. stats[0]++; stats[1]+=nRows; stats[2]+=nExpandedRows;
   *     stats[3]+=nRowsAfterStandardize
   * @return the same or a different table (usually with some results rows)
   */
  public Table getDataForCassandraQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      int resultsDVI[],
      EDV rvToResultsEDV[],
      Session session,
      Statement statement,
      Table table,
      TableWriter tableWriter,
      int[] stats)
      throws Throwable {

    // statement.toString() is useless

    // execute the statement
    ResultSet rs = session.execute(statement);
    ColumnDefinitions columnDef = rs.getColumnDefinitions();
    int nColumnDef = columnDef.size();
    stats[0]++;

    // connect result set columns to table columns
    int nRv = resultsDVI.length;
    int rvToRsCol[] = new int[nRv]; // stored as 0..
    DataType rvToCassDataType[] = new DataType[nRv];
    TypeCodec rvToTypeCodec[] = new TypeCodec[nRv];
    for (int rv = 0; rv < nRv; rv++) {
      // find corresponding resultSet column (may not be 1:1) and other info
      // stored as 0..   -1 if not found
      String sn = rvToResultsEDV[rv].sourceName();
      rvToRsCol[rv] = columnDef.getIndexOf(sn);
      if (rvToRsCol[rv] < 0) {
        StringArray tsa = new StringArray();
        for (int i = 0; i < nColumnDef; i++) tsa.add(columnDef.getName(i));
        throw new SimpleException(
            MustBe.InternalError
                + ": sourceName="
                + sn
                + " not in Cassandra resultsSet columns=\""
                + tsa.toString()
                + "\".");
      }
      rvToCassDataType[rv] = columnDef.getType(rvToRsCol[rv]);

      if (rvToResultsEDV[rv].sourceDataPAType() == PAType.STRING)
        rvToTypeCodec[rv] = CodecRegistry.DEFAULT_INSTANCE.codecFor(rvToCassDataType[rv]);
    }
    int triggerNRows = EDStatic.partialRequestMaxCells / nRv;
    PrimitiveArray paArray[] = new PrimitiveArray[nRv];
    for (int rv = 0; rv < nRv; rv++) paArray[rv] = table.getColumn(rv);

    // process the resultSet rows of data
    int maxNRows = -1;
    boolean toStringErrorShown = false;
    // while ((row = rs.one()) != null) {   //2016-06-20 not working. returns last row repeatedly
    // So use their code from fetchMoreResults() to the solve problem
    //  and improve performance by prefetching results.
    // see
    // https://docs.datastax.com/en/drivers/java/3.0/com/datastax/driver/core/ResultSet.html#one--
    Iterator<Row> iter = rs.iterator();
    while (iter.hasNext()) {
      if (rs.getAvailableWithoutFetching() == 100 && !rs.isFullyFetched()) rs.fetchMoreResults();
      Row row = iter.next();

      stats[1]++;
      int listSizeDVI = -1;
      int listSize = -1; // initially unknown
      for (int rv = 0; rv < nRv; rv++) {
        int rsCol = rvToRsCol[rv];
        EDV edv = rvToResultsEDV[rv];
        PrimitiveArray pa = paArray[rv];
        if (rsCol == -1 || row.isNull(rsCol)) { // not in resultSet or isNull
          pa.addString("");
          maxNRows = Math.max(maxNRows, pa.size());
          continue;
        }
        PAType tPAType = edv.sourceDataPAType();
        if (isListDV[resultsDVI[rv]]) {
          int tListSize = -1;
          if (edv.isBoolean()) { // special case
            List<Boolean> list = row.getList(rsCol, Boolean.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addInt(list.get(i) ? 1 : 0);
          } else if (edv instanceof EDVTimeStamp) { // zulu millis -> epoch seconds
            List<Date> list = row.getList(rsCol, Date.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addDouble(list.get(i).getTime() / 1000.0);
          } else if (tPAType == PAType.STRING) {
            // This doesn't support lists of maps/sets/lists.
            List<String> list = row.getList(rsCol, String.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addString(list.get(i));
          } else if (tPAType == PAType.DOUBLE) {
            List<Double> list = row.getList(rsCol, Double.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addDouble(list.get(i));
          } else if (tPAType == PAType.FLOAT) {
            List<Float> list = row.getList(rsCol, Float.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addFloat(list.get(i));
          } else if (tPAType == PAType.LONG) {
            List<Long> list = row.getList(rsCol, Long.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addLong(list.get(i));
          } else if (tPAType == PAType.INT || tPAType == PAType.SHORT || tPAType == PAType.BYTE) {
            List<Integer> list = row.getList(rsCol, Integer.class);
            tListSize = list.size();
            for (int i = 0; i < tListSize; i++) pa.addInt(list.get(i));
          } else { // PAType.UINT, PAType.USHORT, PAType.UBYTE
            // I think C* doesn't support unsigned data types,
            // so no variable in ERDDAP should be an unsigned type
            throw new RuntimeException("Unexpected PAType=" + tPAType);
          }

          // ensure valid
          if (listSize == -1) {
            listSizeDVI = resultsDVI[rv];
            listSize = tListSize;
          } else if (listSize != tListSize) {
            String2.log("This resultSet row has different list sizes=\n" + row.toString());
            throw new RuntimeException(
                "Source data error: on one row, "
                    + "two list variables have lists of different sizes ("
                    + edv.destinationName()
                    + "="
                    + tListSize
                    + " != "
                    + dataVariableDestinationNames[listSizeDVI]
                    + "="
                    + listSize
                    + ").");
          }
        } else {
          if (edv.isBoolean()) { // special case
            pa.addInt(row.getBool(rsCol) ? 1 : 0);
          } else if (edv instanceof EDVTimeStamp) { // zulu millis -> epoch seconds
            pa.addDouble(row.getTimestamp(rsCol).getTime() / 1000.0);
          } else if (tPAType == PAType.STRING) {
            // v2: getString doesn't return the String form of any type
            // https://datastax-oss.atlassian.net/browse/JAVA-135
            // Object value = rvToCassDataType[rv].
            //    deserialize(row.getBytesUnsafe(rsCol), protocolVersion);
            // pa.addString(value.toString());

            // v3:
            // https://datastax.github.io/java-driver/upgrade_guide/
            String s = "[?]";
            try {
              TypeCodec codec = rvToTypeCodec[rv];
              if (codec != null) {
                java.nio.ByteBuffer bytes = row.getBytesUnsafe(rsCol);
                s = bytes == null ? "" : codec.deserialize(bytes, protocolVersion).toString();
              }

            } catch (Throwable t) {
              if (!toStringErrorShown) {
                String2.log("First toString error:\n" + MustBe.throwableToString(t));
                toStringErrorShown = true;
              }
            }
            pa.addString(s);
          } else if (tPAType == PAType.DOUBLE) {
            pa.addDouble(row.getDouble(rsCol));
          } else if (tPAType == PAType.FLOAT) {
            pa.addFloat(row.getFloat(rsCol));
          } else if (tPAType == PAType.LONG) {
            pa.addLong(row.getLong(rsCol));
          } else if (tPAType == PAType.INT || tPAType == PAType.SHORT || tPAType == PAType.BYTE) {
            pa.addInt(row.getInt(rsCol));
          } else { // PAType.UINT, PAType.USHORT, PAType.UBYTE
            // I think C* doesn't support unsigned data types,
            // so no variable in ERDDAP should be an unsigned type
            throw new RuntimeException("Unexpected PAType=" + tPAType);
          }
        }
        maxNRows = Math.max(maxNRows, pa.size());
      }
      stats[2] += Math.max(1, listSize);

      // expand scalars and missing values to fill maxNRows
      for (int rv = 0; rv < nRv; rv++) {
        PrimitiveArray pa = paArray[rv];
        int n = maxNRows - pa.size();
        if (n > 0) {
          PAType tPAType = pa.elementType();
          if (tPAType == PAType.STRING || tPAType == PAType.LONG) {
            pa.addNStrings(n, pa.getString(pa.size() - 1));
          } else if (tPAType == PAType.DOUBLE || tPAType == PAType.FLOAT) {
            pa.addNDoubles(n, pa.getDouble(pa.size() - 1));
          } else {
            pa.addNInts(n, pa.getInt(pa.size() - 1));
          }
        }
      }

      // standardize a chunk and write to tableWriter.writeSome ?
      if (maxNRows >= triggerNRows) {
        // String2.log(table.toString("rows",5));
        preStandardizeResultsTable(loggedInAs, table);
        if (table.nRows() > 0) {
          standardizeResultsTable(
              language, requestUrl, userDapQuery, table); // changes sourceNames to destinationNames
          stats[3] += table.nRows();
          tableWriter.writeSome(table); // okay if 0 rows
        }

        // triggerNRows + 1000 since lists expand, so hard to know exactly
        maxNRows = -1;
        table = makeEmptySourceTable(rvToResultsEDV, triggerNRows + 1000);
        for (int rv = 0; rv < nRv; rv++) paArray[rv] = table.getColumn(rv);
        if (tableWriter.noMoreDataPlease) {
          tableWriter.logCaughtNoMoreDataPlease(datasetID);
          break;
        }
      }
    }
    return table;
  }

  /**
   * getDataForDapQuery always calls this right before standardizeResultsTable.
   * EDDTableFromPostDatabase uses this to remove data not accessible to this user.
   */
  public void preStandardizeResultsTable(String loggedInAs, Table table) {
    // this base version does nothing
  }

  /**
   * This generates a datasets.xml entry for an EDDTableFromCassandra. The XML can then be edited by
   * hand and added to the datasets.xml file.
   *
   * <p>The dataVariable sourceNames are always in sorted order. That's the order that
   *
   * @param url the Cassandra URL, e.g., #.#.#.# or localhost (assumed port=9160)
   * @param connectionProperties see description for class constructor
   * @param keyspace the keyspace name or use "!!!LIST!!!" to get a list of keyspaces.
   * @param tableName or use "!!!LIST!!!" to get the metadata for all tableNames in the keyspace.
   * @param tReloadEveryNMinutes e.g., DEFAULT_RELOAD_EVERY_N_MINUTES (10080) for weekly
   * @param tInfoUrl or "" if in externalAddGlobalAttributes or if not available
   * @param tInstitution or "" if in externalAddGlobalAttributes or if not available
   * @param tSummary or "" if in externalAddGlobalAttributes or if not available
   * @param tTitle or "" if in externalAddGlobalAttributes or if not available
   * @param externalAddGlobalAttributes These attributes are given priority. Use null in none
   *     available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble. If no trouble, then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String url,
      String tConnectionProperties[],
      String keyspace,
      String tableName,
      int tReloadEveryNMinutes,
      String tInfoUrl,
      String tInstitution,
      String tSummary,
      String tTitle,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromCassandra.generateDatasetsXml"
            + "\nurl="
            + url
            + "\nconnectionProperties="
            + String2.toCSVString(tConnectionProperties)
            + "\nkeyspace="
            + keyspace
            + " tableName="
            + tableName
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\ninfoUrl="
            + tInfoUrl
            + "\ninstitution="
            + tInstitution
            + "\nsummary="
            + tSummary
            + "\ntitle="
            + tTitle
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);

    if (tReloadEveryNMinutes < suggestReloadEveryNMinutesMin
        || tReloadEveryNMinutes > suggestReloadEveryNMinutesMax)
      tReloadEveryNMinutes = 1440; // not the usual DEFAULT_RELOAD_EVERY_N_MINUTES;

    // Querying a system table
    // http://www.datastax.com/documentation/cql/3.1/cql/cql_using/use_query_system_c.html

    if (tConnectionProperties == null) tConnectionProperties = new String[0];
    Test.ensureTrue(
        !Math2.odd(tConnectionProperties.length),
        "connectionProperties.length must be an even number.");
    ConcurrentHashMap<String, String> conProp = new ConcurrentHashMap();
    for (int i = 0; i < tConnectionProperties.length; i += 2) {
      String tKey = tConnectionProperties[i];
      String tValue = tConnectionProperties[i + 1];
      if (String2.isSomething(tKey) && tValue != null) conProp.put(tKey, tValue); // <String,String>
    }

    // make/get the session (and hold local reference)
    // For line below, I got com.datastax.driver.core.exceptions.NoHostAvailableException
    // Solution: Make sure Cassandra is running.
    Session session = getSession(url, conProp);
    // int protocolVersion =
    // session.getCluster().getConfiguration().getProtocolOptions().getProtocolVersion();

    // * just get a list of keyspaces
    Metadata clusterMetadata = session.getCluster().getMetadata();
    if (keyspace.equals(LIST)) {
      if (verbose) String2.log("getting keyspace list");
      return String2.toNewlineString(
          String2.toStringArray(clusterMetadata.getKeyspaces().toArray()));
    }

    // * just get info for all tables in keyspace
    KeyspaceMetadata km = clusterMetadata.getKeyspace(keyspace);
    if (km == null) throw new RuntimeException("No metadata for keyspace=" + keyspace);
    if (tableName.equals(LIST)) {
      if (verbose) String2.log("getting tableName list");
      return km.exportAsString();
    }

    // * generateDatasetsXml for one Cassandra table
    // partition key  (not a Set, because order is important)
    TableMetadata tm = km.getTable(tableName);
    StringArray partitionKeySA = new StringArray(); // sourceNames
    StringArray subsetVariablesSourceNameSA = new StringArray();
    List<ColumnMetadata> pk = tm.getPartitionKey();
    for (int i = 0; i < pk.size(); i++) {
      partitionKeySA.add(pk.get(i).getName());
      subsetVariablesSourceNameSA.add(pk.get(i).getName());
    }

    // clusterColumn and indexColumn (could be accumulated as a Set)
    StringArray clusterColumnSA = new StringArray(); // sourceNames
    List<ColumnMetadata> cc = tm.getClusteringColumns();
    for (int i = 0; i < cc.size(); i++) clusterColumnSA.add(cc.get(i).getName());

    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();
    List<ColumnMetadata> cmList = tm.getColumns();
    boolean isList[] = new boolean[cmList.size()];
    StringArray indexColumnSA = new StringArray(); // sourceNames
    for (int col = 0; col < cmList.size(); col++) {

      ColumnMetadata cm = cmList.get(col);
      String sourceName = cm.getName();
      DataType cassType = cm.getType();

      if (cm.isStatic()) {
        // static columns are DISTINCT able and have few values,
        //  so they are good for subsetVariables
        // (but they aren't constrainable)
        if (subsetVariablesSourceNameSA.indexOf(sourceName) < 0)
          subsetVariablesSourceNameSA.add(sourceName);
      }

      // 2016-04-07 Removed because no more .getIndex
      //  because column <-> index is no longer 1:1.
      //  see https://datastax-oss.atlassian.net/browse/JAVA-1008
      // if (cm.getIndex() != null) {
      //    //indexed columns are only constrainable with '=')
      //    if (indexColumnSA.indexOf(sourceName) < 0)
      //        indexColumnSA.add(sourceName);
      // }

      // Cass identifiers are [a-zA-Z0-9_]*
      String destName = ("0123456789".indexOf(sourceName.charAt(0)) >= 0 ? "_" : "") + sourceName;
      if (sourceName.equals("lat")) destName = EDV.LAT_NAME;
      if (sourceName.equals("lon")) destName = EDV.LON_NAME;

      PrimitiveArray sourcePA = null;
      // https://stackoverflow.com/questions/34160748/upgrading-calls-to-datastax-java-apis-that-are-gone-in-3
      isList[col] = cassType.getName() == DataType.Name.LIST;
      // String2.log(sourceName + " isList=" + isList[col] + " javaClass=" +
      // cassType.asJavaClass());
      if (isList[col]) cassType = cassType.getTypeArguments().get(0); // the element type

      Attributes sourceAtts = new Attributes();
      Attributes addAtts = new Attributes();
      boolean isTimestamp = false;
      if (cassType == DataType.cboolean()) sourcePA = new ByteArray();
      else if (cassType == DataType.cint()) sourcePA = new IntArray();
      else if (cassType == DataType.bigint()
          || cassType == DataType.counter()
          || cassType == DataType.varint()) sourcePA = new LongArray();
      else if (cassType == DataType.cfloat()) sourcePA = new FloatArray();
      else if (cassType == DataType.cdouble() || cassType == DataType.decimal())
        sourcePA = new DoubleArray();
      else if (cassType == DataType.timestamp()) {
        sourcePA = new DoubleArray();
        isTimestamp = true;
        addAtts.add("ioos_category", "Time");
        addAtts.add("units", "seconds since 1970-01-01T00:00:00Z");
      } else sourcePA = new StringArray(); // everything else

      PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);

      // lie, to trigger catching LLAT
      if (destName.equals(EDV.LON_NAME)) sourceAtts.add("units", EDV.LON_UNITS);
      else if (destName.equals(EDV.LAT_NAME)) sourceAtts.add("units", EDV.LAT_UNITS);
      else if (destName.equals(EDV.ALT_NAME)) sourceAtts.add("units", EDV.ALT_UNITS);
      else if (destName.equals(EDV.DEPTH_NAME)) sourceAtts.add("units", EDV.DEPTH_UNITS);
      addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              null, // no source global attributes
              sourceAtts,
              addAtts,
              sourceName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT

      // but make it real here, and undo the lie
      if (destName.equals(EDV.LON_NAME)) {
        addAtts.add("units", EDV.LON_UNITS);
        sourceAtts.remove("units");
      } else if (destName.equals(EDV.LAT_NAME)) {
        addAtts.add("units", EDV.LAT_UNITS);
        sourceAtts.remove("units");
      } else if (destName.equals(EDV.ALT_NAME)) {
        addAtts.add("units", EDV.ALT_UNITS);
        sourceAtts.remove("units");
      } else if (destName.equals(EDV.DEPTH_NAME)) {
        addAtts.add("units", EDV.DEPTH_UNITS);
        sourceAtts.remove("units");
      }
      // time units already done above for all timestamp vars

      dataSourceTable.addColumn(col, sourceName, sourcePA, sourceAtts);
      dataAddTable.addColumn(col, destName, destPA, addAtts);

      // add missing_value and/or _FillValue if needed
      // but for Cassandra, I think no data, so no way to see mv's
      addMvFvAttsIfNeeded(destName, destPA, sourceAtts, addAtts);
    }

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable);

    // subsetVariables source->dest name
    StringArray subsetVariablesDestNameSA =
        new StringArray(subsetVariablesSourceNameSA.size(), true);
    // String2.log(">> subsetVarSourceNames=" + subsetVariablesSourceNameSA.toString());
    for (int sv = 0; sv < subsetVariablesSourceNameSA.size(); sv++) {
      subsetVariablesDestNameSA.set(
          sv,
          dataAddTable.getColumnName(
              dataSourceTable.findColumnNumber(subsetVariablesSourceNameSA.get(sv))));
    }
    // String2.log(">> subsetVarDestNames=" + subsetVariablesDestNameSA.toString());

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    if (tInfoUrl != null && tInfoUrl.length() > 0)
      externalAddGlobalAttributes.add("infoUrl", tInfoUrl);
    if (tInstitution != null && tInstitution.length() > 0)
      externalAddGlobalAttributes.add("institution", tInstitution);
    if (tSummary != null && tSummary.length() > 0)
      externalAddGlobalAttributes.add("summary", tSummary);
    if (tTitle != null && tTitle.length() > 0) externalAddGlobalAttributes.add("title", tTitle);
    externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local Cassandra)");
    // String2.log(">> ext subsetVariables=" +
    // externalAddGlobalAttributes.getString("subsetVariables"));
    externalAddGlobalAttributes.setIfNotAlreadySet(
        "subsetVariables", subsetVariablesDestNameSA.toString());
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                // another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable) ? "Point" : "Other",
                "cassandra/"
                    + keyspace
                    + "/"
                    + tableName, // fake file dir.  Cass identifiers are [a-zA-Z0-9_]*
                externalAddGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));

    // don't suggestSubsetVariables() since no real sourceTable data

    // write the information
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<!-- NOTE! Since Cassandra tables don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromCassandra\" datasetID=\"cass"
            +
            // Cass identifiers are [a-zA-Z0-9_]*
            (keyspace.startsWith("_") ? "" : "_")
            + XML.encodeAsXML(keyspace)
            + (tableName.startsWith("_") ? "" : "_")
            + XML.encodeAsXML(tableName)
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + url
            + "</sourceUrl>\n");
    for (int i = 0; i < tConnectionProperties.length; i += 2)
      sb.append(
          "    <connectionProperty name=\""
              + XML.encodeAsXML(tConnectionProperties[i])
              + "\">"
              + XML.encodeAsXML(tConnectionProperties[i + 1])
              + "</connectionProperty>\n");
    sb.append(
        "    <keyspace>"
            + XML.encodeAsXML(keyspace)
            + "</keyspace>\n"
            + // safe since Cass identifiers are [a-zA-Z0-9_]*
            "    <tableName>"
            + XML.encodeAsXML(tableName)
            + "</tableName>\n"
            + // safe
            "    <partitionKeySourceNames>"
            + XML.encodeAsXML(partitionKeySA.toString())
            + "</partitionKeySourceNames>\n"
            + "    <clusterColumnSourceNames>"
            + XML.encodeAsXML(clusterColumnSA.toString())
            + "</clusterColumnSourceNames>\n"
            + "    <indexColumnSourceNames>"
            + XML.encodeAsXML(indexColumnSA.toString())
            + "</indexColumnSourceNames>\n"
            + "    <maxRequestFraction>1</maxRequestFraction>\n"
            + "    <columnNameQuotes></columnNameQuotes>\n"
            + // default = empty string
            "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n");
    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(cdmSuggestion());
    sb.append(writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));
    sb.append("</dataset>\n" + "\n");

    // convert boolean var dataType from byte to boolean
    String2.replaceAll(sb, "<dataType>byte", "<dataType>boolean");

    // convert lists to List <dataType>'s    e.g., float -> floatList
    for (int col = 0; col < isList.length; col++) {
      if (isList[col]) {
        String find = "<sourceName>" + dataSourceTable.getColumnName(col) + "</sourceName>";
        int po = sb.indexOf(find);
        if (po < 0)
          throw new RuntimeException(
              "Internal ERROR: \"" + find + "\" not found in sb=\n" + sb.toString());
        po = sb.indexOf("</dataType>", po + find.length());
        if (po < 0)
          throw new RuntimeException(
              "Internal ERROR: \"" + find + "\" + </dataType> not found in sb=\n" + sb.toString());
        sb.insert(po, "List");
      }
    }

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
