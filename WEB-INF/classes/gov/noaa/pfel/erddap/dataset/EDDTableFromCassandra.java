/* 
 * EDDTableFromCassandra Copyright 2014, NOAA.
 * See the LICENSE.txt file in this file's directory.
 * 
 * <p>Working with C*: 
 * http://www.datastax.com/documentation/cassandra/2.0/cassandra/gettingStartedCassandraIntro.html
 * and
 * http://www.datastax.com/documentation/cql/3.1/cql/cql_using/start_cql_win_t.html
 * Bob's C* bin dir: cd "C:\Program Files\DataStax Community\apache-cassandra\bin"
 * Startup cqlsh: cqlsh          
 * For Bob, Cassandra is at localhost:9160 -> 127.0.0.1:9160 
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
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

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

//See notes ErddapReleaseChecklist.txt about cassandra Java driver and dependencies.
//  http://www.datastax.com/documentation/developer/java-driver/2.0/java-driver/reference/settingUpJavaProgEnv_r.html?scroll=settingUpJavaProgEnv_r__dependencies-list
//  is for cassandra-driver-core-2.0.1.jar. (Mine is newer.)
//See setup.html Credits for information about dependencies. 
//  Recommended for C* Java 2.0 driver:
//  netty 3.8, guava-16.0.1.jar, metrics-core-3.0.2.jar, 
//  slf4j-api-1.7.5.jar  (I use the one in netcdf-all.jar),
//  net.jpountz.lz4
//  snappy-java....jar
import com.datastax.driver.core.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
//import java.util.Properties;

/** 
 * This class represents a table of data from Cassandra.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2014-11-03
 */
public class EDDTableFromCassandra extends EDDTable{ 

    //see getSession
    private static ConcurrentHashMap<String, Session> sessionsMap = 
        new ConcurrentHashMap(); 
    private static ConcurrentHashMap<String, PreparedStatement> statementMap = 
        new ConcurrentHashMap(); 
    public static String LIST = "!!!LIST!!!";

    /** set by the constructor */
    private Session session;
    private int protocolVersion = -1;
    protected String keyspace; 
    protected String tableName;
    protected int nPartitionKeys;
    protected String partitionKeyNames[];     //source names
    protected String partitionKeyFrom[];      //null or name of timestamp var it is derived from
    protected String partitionKeyPrecision[]; //null or precision of timestamp var
    protected String partitionKeyFixedValue[];//null, or the fixed value (plain number or string in quotes)
    protected EDV partitionKeyEDV[];          //edv of each partitionKey
    protected final static String PartitionKeysDistinctTableName = 
        "PartitionKeysDistinctTable.nc";
    protected HashSet clusterColumnSourceNames;
    protected HashSet indexColumnSourceNames;
    protected double maxRequestFraction = 1; //>0..1; 1 until subsetVarTable has been made
    protected String partitionKeyRelatedVariables; //CSSV for error message

    //Double quotes, see 
    //http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/escape_char_r.html
    protected String columnNameQuotes = "";  // empty string (default) or "
    protected boolean isListDV[]; //true if this dataVariable is a list dataType, e.g., doubleList

    //public static String testUser = "postgres";
    //public static String testUrl = "jdbc:postgresql://127.0.0.1:5432/mydatabase";
    //public static String testDriver = "org.postgresql.Driver";

    /**
     * This constructs an EDDTableFromCassandra based on the information in an .xml file.
     *
     * <p>Unusual: the <dataType> for the dataVariables include the regular dataTypes plus
     * list variables: booleanList, 
     * byteList, charList, shortList, intList, longList, floatList, doubleList, StringList.
     * When a list variable is in the results, each row of source data will be expanded
     * to size(list) rows of data in ERDDAP; scalars in the source data will be
     * duplicated size(list) times.
     * If the results contain more than one list variable, all lists on a given 
     * row of data MUST have the same size and MUST be "parallel" lists, i.e.,
     * a[0], b[0], c[0], ... MUST all be related, and 
     * a[1], b[1], c[1], ... MUST all be related, ...
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromCassandra"&gt; 
     *    having just been read.  
     * @return an EDDTableFromCassandra.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromCassandra fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromCassandra(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
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
        String tColumnNameQuotes = ""; //default
        StringArray tConnectionProperties = new StringArray();
        boolean tSourceNeedsExpandedFP_EQ = true;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("  tags=" + tags + content);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<connectionProperty>")) tConnectionProperties.add(xmlReader.attributeValue("name"));
            else if (localTags.equals("</connectionProperty>")) tConnectionProperties.add(content); 
            else if (localTags.equals( "<keyspace>")) {}
            else if (localTags.equals("</keyspace>")) tKeyspace = content; 
            else if (localTags.equals( "<tableName>")) {}
            else if (localTags.equals("</tableName>")) tTableName = content; 
            else if (localTags.equals( "<partitionKeySourceNames>")) {}
            else if (localTags.equals("</partitionKeySourceNames>")) tPartitionKeySourceNames = content; 
            else if (localTags.equals( "<clusterColumnSourceNames>")) {}
            else if (localTags.equals("</clusterColumnSourceNames>")) tClusterColumnSourceNames = content; 
            else if (localTags.equals( "<indexColumnSourceNames>")) {}
            else if (localTags.equals("</indexColumnSourceNames>")) tIndexColumnSourceNames = content; 
            else if (localTags.equals( "<maxRequestFraction>")) {}
            else if (localTags.equals("</maxRequestFraction>")) tMaxRequestFraction = String2.parseDouble(content); 
            else if (localTags.equals( "<columnNameQuotes>")) {}
            else if (localTags.equals("</columnNameQuotes>")) tColumnNameQuotes = content; 
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromCassandra(tDatasetID, tAccessibleTo, 
                tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery, 
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, 
                tLocalSourceUrl,
                tConnectionProperties.toArray(),
                tKeyspace, tTableName, 
                tPartitionKeySourceNames, tClusterColumnSourceNames, 
                tIndexColumnSourceNames,
                tMaxRequestFraction, tColumnNameQuotes,
                tSourceNeedsExpandedFP_EQ);
    }



    /**
     * The constructor. See general documentation in EDDTable.java and 
     * specific documentation in setupDatasetsXml.html.
     * 
     * @throws Throwable if trouble
     */
    public EDDTableFromCassandra(String tDatasetID, String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl, String tConnectionProperties[],
        String tKeyspace, String tTableName, 
        String tPartitionKeySourceNames, String tClusterColumnSourceNames,
        String tIndexColumnSourceNames,
        double tMaxRequestFraction, String tColumnNameQuotes,
        boolean tSourceNeedsExpandedFP_EQ
        ) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromCassandra " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromCassandra(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromCassandra"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        Test.ensureNotNothing(tLocalSourceUrl, "'sourceUrl' wasn't defined.");
        localSourceUrl = tLocalSourceUrl;
        publicSourceUrl = "(Cassandra)"; //not tLocalSourceUrl; keep it private
        addGlobalAttributes.set("sourceUrl", publicSourceUrl);  

        //connectionProperties may have secret (username and password)!
        //So use then throw away.
        if (tConnectionProperties == null) tConnectionProperties = new String[0];
        Test.ensureTrue(!Math2.odd(tConnectionProperties.length), 
            "connectionProperties.length must be an even number.");
        ConcurrentHashMap<String,String> connectionProperties = new ConcurrentHashMap();
        for (int i = 0; i < tConnectionProperties.length; i += 2) {
            String tKey   = tConnectionProperties[i];
            String tValue = tConnectionProperties[i+1];
            if (String2.isSomething(tKey) && tValue != null)
                connectionProperties.put(tKey, tValue);  //<String,String>
        }
        Test.ensureNotNothing(tKeyspace, "'keyspace' wasn't defined.");
        keyspace = tKeyspace;
        Test.ensureNotNothing(tTableName, "'tableName' wasn't defined.");
        tableName = tTableName;

        Test.ensureNotNothing(tPartitionKeySourceNames, "'partitionKeySourceNames' wasn't defined.");
        partitionKeyNames = String2.split(tPartitionKeySourceNames, ',');  //they are trimmed
        nPartitionKeys = partitionKeyNames.length;
        partitionKeyFrom       = new String[nPartitionKeys];  //all nulls
        partitionKeyPrecision  = new String[nPartitionKeys];  //all nulls
        partitionKeyFixedValue = new String[nPartitionKeys];  //all nulls
        for (int i = 0; i < nPartitionKeys; i++) {
            //timestamp derived from another timestamp?  date/sampletime/1970-01-01Z
            String sar[] = String2.split(partitionKeyNames[i], '/'); //they are trimmed
            if (sar.length == 1) {
                //fixed value? deviceid=1007  or deviceid="CA107"
                sar = String2.split(partitionKeyNames[i], '='); //they are trimmed
                if (sar.length == 1) {
                    sar[0] = String2.canonical(sar[0]);
                } else if (sar.length == 2) {
                    partitionKeyNames[i]      = String2.canonical(sar[0]);
                    partitionKeyFixedValue[i] = sar[1];
                }
            } else if (sar.length == 3) {
                partitionKeyNames[i]     = String2.canonical(sar[0]);
                partitionKeyFrom[i]      = String2.canonical(sar[1]);
                partitionKeyPrecision[i] = String2.canonical(sar[2]);
            } else {
                throw new RuntimeException(String2.ERROR + 
                ": Invalid '/' usage in partitionKeys for \"" + partitionKeyNames[i] + "\".");
            }
        }

        clusterColumnSourceNames = new HashSet();
        if (String2.isSomething(tClusterColumnSourceNames)) {
            String sar[] = String2.split(tClusterColumnSourceNames, ',');  //they are trimmed
            for (String s: sar) 
                clusterColumnSourceNames.add(String2.canonical(s));
        }

        indexColumnSourceNames = new HashSet();
        if (String2.isSomething(tIndexColumnSourceNames)) {
            String sar[] = String2.split(tIndexColumnSourceNames, ',');  //they are trimmed
            for (String s: sar) 
                indexColumnSourceNames.add(String2.canonical(s));
        }

        //don't set maxRequestFraction until after subsetVariableTable has been made
        Test.ensureBetween(tMaxRequestFraction, 
            1e-10, 1, "Invalid maxRequestFraction");

        columnNameQuotes = tColumnNameQuotes;
        Test.ensureTrue(
            "\"".equals(columnNameQuotes) ||
              "".equals(columnNameQuotes), 
            "<columnNameQuotes> must be \" or an empty string (the default).");

        //cql can support everything except != and regex constraints
        //PARTIAL because CQL treats > like >=, and < like <=
        //  and because constraints on list variables are non-sensical until expanded in ERDDAP.
        sourceNeedsExpandedFP_EQ      = tSourceNeedsExpandedFP_EQ;
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL;
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; 
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP;
      
        //set global attributes
        sourceGlobalAttributes = new Attributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //create dataVariables[]
        int ndv = tDataVariables.length;
        dataVariables = new EDV[ndv];
        isListDV = new boolean[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = (String)tDataVariables[dv][3];
            //deal with <dataType>'s that are lists: byteList, doubleList, ...
            if (tSourceType.endsWith("List")) {
                isListDV[dv] = true;
                tSourceType = tSourceType.substring(0, tSourceType.length() - 4);
            }
            Attributes tSourceAtt = new Attributes();
            //if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType=" + tSourceType);

            if (EDV.LON_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLon(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN); 
                lonIndex = dv;
            } else if (EDV.LAT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLat(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN); 
                latIndex = dv;
            } else if (EDV.ALT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVAlt(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN);
                altIndex = dv;
            } else if (EDV.DEPTH_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVDepth(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN);
                depthIndex = dv;
            } else if (EDV.TIME_NAME.equals(tDestName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                dataVariables[dv] = new EDVTime(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType); //this constructor gets source / sets destination actual_range
                timeIndex = dv;
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //this constructor gets source / sets destination actual_range
            } else {
                dataVariables[dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); 
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            }
        }

        //make/get the session (ensure it is createable)
        session = getSession(localSourceUrl, connectionProperties);
        protocolVersion = session.getCluster().getConfiguration().getProtocolOptions().getProtocolVersion();

        //make the primaryKey distinct table
        StringBuilder dapVars = new StringBuilder();
        StringBuilder cassVars = new StringBuilder();
        StringBuilder dapConstraints = new StringBuilder();
        StringBuilder cassConstraints = new StringBuilder();
        int resultsDVI[] = new int[nPartitionKeys];
        EDV rvToResultsEDV[] = new EDV[nPartitionKeys];
        File2.makeDirectory(datasetDir()); 
        TableWriterAll twa = new TableWriterAll(datasetDir(), "tPKDistinct");
        partitionKeyEDV = new EDV[nPartitionKeys];
        StringArray pkRelated = new StringArray();
        for (int pki = 0; pki < nPartitionKeys; pki++) {
            String tSourceName = partitionKeyNames[pki];
            resultsDVI[pki] = String2.indexOf(dataVariableSourceNames(), tSourceName);
            rvToResultsEDV[pki] = dataVariables[resultsDVI[pki]];
            if (resultsDVI[pki] < 0)
                throw new RuntimeException(String2.ERROR + ": sourceName=" + tSourceName + 
                    " not found in sourceNames=" + 
                    String2.toCSSVString(dataVariableSourceNames()));
            EDV edv = dataVariables[resultsDVI[pki]];
            partitionKeyEDV[pki] = edv;
            pkRelated.add(edv.destinationName());
            dapVars.append((pki == 0? "" : ",") + edv.destinationName());
            cassVars.append((pki == 0? "SELECT DISTINCT " : ",") + 
                columnNameQuotes + tSourceName + columnNameQuotes);

            String pkfv = partitionKeyFixedValue[pki];
            if (pkfv != null) {
                //constrain dap, don't constrain Cassandra (it refuses)
                dapConstraints.append("&" + edv.destinationName() + "=" + pkfv);
            }

            int dvi = String2.indexOf(dataVariableSourceNames(), partitionKeyFrom[pki]);
            if (dvi >= 0)
                pkRelated.add(dataVariables[dvi].destinationName());
        }
        partitionKeyRelatedVariables = pkRelated.toString();
        Table table = makeEmptySourceTable(rvToResultsEDV, 1024); 
        String dapQuery = dapVars.toString() + dapConstraints.toString() + "&distinct()";
        String cassQuery = cassVars.toString() + 
            " FROM " + keyspace + "." + tableName + cassConstraints.toString();
        if (verbose) String2.log(
            "* PrimaryKeys DAP  query=" + dapQuery + "\n" +
            "* PrimaryKeys Cass query=" + cassQuery);
        SimpleStatement statement = new SimpleStatement(cassQuery);
        table = getDataForCassandraQuery(
            EDStatic.loggedInAsSuperuser, "irrelevant", dapQuery, 
            resultsDVI, rvToResultsEDV, session, statement, 
            table, twa, new int[4]);
        if (twa.noMoreDataPlease) 
            throw new RuntimeException(
                "Too many primary keys?! TableWriterAll said NoMoreDataPlease.");
        preStandardizeResultsTable(EDStatic.loggedInAsSuperuser, table); 
        if (table.nRows() > 0) {
            standardizeResultsTable("irrelevant", dapQuery, table);
            twa.writeSome(table);
        }
        twa.finish();
        Table cumTable = twa.cumulativeTable();
        cumTable.leftToRightSortIgnoreCase(nPartitionKeys); //useful: now in sorted order
        String2.log(PartitionKeysDistinctTableName + " nRows=" + cumTable.nRows());
        //String2.log(cumTable.dataToCSVString());
        cumTable.saveAsFlatNc(datasetDir() + PartitionKeysDistinctTableName, 
            "row", false); //convertToFakeMissingValues
        cumTable = null;
        twa.releaseResources();        

        //gather ERDDAP sos information?
        //assume time column is indexed? so C* can return min/max efficiently

        //ensure the setup is valid
        ensureValid();

        //set after subsetVariablesTable has been made 
        maxRequestFraction = tMaxRequestFraction;

        if (verbose) 
            String2.log(
                (reallyVerbose? "\n" + toString() : "") +
                "\n*** EDDTableFromCassandra " + datasetID + " constructor finished. TIME=" + 
                (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
    }

    /**
     * This gets/makes a session for the specified url.
     * Unlike database sessions, a Cassandra session is thread-safe and very robust 
     * (e.g., transparent failover), so it can be used by multiple users for multiple datasets.
     * See claims at
     * http://www.datastax.com/documentation/developer/java-driver/2.1/common/drivers/introduction/introArchOverview_c.html
     * See the Four Simple Rules for using Cassandra clusters and sessions
     * http://www.datastax.com/documentation/developer/java-driver/2.1/java-driver/fourSimpleRules.html
     * <br>1) "Use one cluster instance per (physical) cluster (per application lifetime)
     * <br>2) Use at most one session instance per keyspace, or use a single 
     *   Session and explicitely specify the keyspace in your queries
     * <br>3) If you execute a statement more than once, consider using a prepared statement
     * <br>4) You can reduce the number of network roundtrips and also have atomic
     *   operations by using batches"
     * Because of #2, ERDDAP just creates one session per url.  
     * The sessionMap's key=url, value=session.
     */
    public static Session getSession(String url, 
        ConcurrentHashMap<String,String> connectionProperties) throws Throwable {

        Session session = sessionsMap.get(url);
        if (session != null)
            return session;

        if (verbose) String2.log("EDDTableFromCassandra.getSession");
        long tTime = System.currentTimeMillis();
        //see http://www.datastax.com/documentation/developer/java-driver/2.1/java-driver/fourSimpleRules.html
        Cluster.Builder builder = Cluster.builder().addContactPoint(url);
        
        //options
        //Note that any tag with no value causes nothing to be done.
        //http://www.datastax.com/drivers/java/2.1/com/datastax/driver/core/Cluster.Builder.html
        String opt, ts;
        int ti;
        boolean tb;

        //directly set options

        opt = "compression";
        if (verbose) String2.log("  " + opt + " default=NONE"); //NONE
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts)) {
            builder.withCompression(ProtocolOptions.Compression.valueOf(ts.toUpperCase()));
            if (verbose) String2.log("  " + opt + "  set to " + ts.toUpperCase());
        }

        opt = "credentials";
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts)) {
            ts = ts.trim();
            int po = ts.indexOf('/'); //first instance
            if (po > 0) 
                builder.withCredentials(ts.substring(0, po), ts.substring(po + 1));
            else throw new SimpleException(
                "ERROR: Invalid connectionProperty value for name=credentials. " +
                "Expected: \"name/password\" (that's a literal '/').");
            if (verbose) String2.log("  " + opt + " was set.");
        }        

        opt = "metrics";
        if (verbose) String2.log("  " + opt + " default=true"); 
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts) && !String2.parseBoolean(ts)) { //unusual
            builder.withoutMetrics(); //set it to false
            if (verbose) String2.log("  " + opt + "  set to false");
        }

        opt = "port"; 
        if (verbose) String2.log("  " + opt + " default=9042");
        ti = String2.parseInt(connectionProperties.get(opt));
        if (ti >= 0 && ti < Integer.MAX_VALUE) {
            builder.withPort(ti); //unusual
            if (verbose) String2.log("  " + opt + "  set to " + ti);
        }

        opt = "ssl";
        if (verbose) String2.log("  " + opt + " default=false"); 
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts) && String2.parseBoolean(ts)) { //unusual
            builder.withSSL();
            if (verbose) String2.log("  " + opt + "  set to true");
        }



        //QueryOptions
        QueryOptions queryOpt = new QueryOptions(); //has defaults

        opt = "consistencyLevel";
        if (verbose) String2.log("  " + opt + " default=" +  
            queryOpt.getConsistencyLevel()); //ONE
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts)) {
            queryOpt.setConsistencyLevel(ConsistencyLevel.valueOf(ts.toUpperCase()));
            if (verbose) String2.log("  " + opt + "  set to " + ts.toUpperCase());
        }

        opt = "fetchSize";
        if (verbose) String2.log("  " + opt + " default=" +  
            queryOpt.getFetchSize()); //5000
        ti = String2.parseInt(connectionProperties.get(opt));
        if (ti >= 10 && ti < Integer.MAX_VALUE) {
            queryOpt.setFetchSize(ti);
            if (verbose) String2.log("  " + opt + "  set to " + ti);
        }

        opt = "serialConsistencyLevel";
        if (verbose) String2.log("  " + opt + " default=" +  
            queryOpt.getSerialConsistencyLevel()); //SERIAL
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts)) {
            queryOpt.setSerialConsistencyLevel(ConsistencyLevel.valueOf(ts.toUpperCase()));
            if (verbose) String2.log("  " + opt + "  set to " + ts.toUpperCase());
        }

        builder = builder.withQueryOptions(queryOpt);

        //socketOptions
        SocketOptions socOpt = new SocketOptions(); //has defaults

        opt = "connectTimeoutMillis";
        if (verbose) String2.log("  " + opt + " default=" +  
            socOpt.getConnectTimeoutMillis()); //5000
        ti = String2.parseInt(connectionProperties.get(opt));
        if (ti >= 1000 && ti < Integer.MAX_VALUE) {
            socOpt.setConnectTimeoutMillis(ti);
            if (verbose) String2.log("  " + opt + "  set to " + ti + " " + socOpt.getConnectTimeoutMillis());
        }

        opt = "keepAlive";
        if (verbose) String2.log("  " + opt + " default=" + 
            socOpt.getKeepAlive()); //null
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts)) {
            tb = String2.parseBoolean(ts);
            socOpt.setKeepAlive(tb);
            if (verbose) String2.log("  " + opt + "  set to " + tb);
        }

        opt = "readTimeoutMillis";
        if (verbose) String2.log("  " + opt + " default=" + 
            socOpt.getReadTimeoutMillis()); //12000
        ti = String2.parseInt(connectionProperties.get(opt));
        if (ti >= 1000 && ti < Integer.MAX_VALUE) 
            ti = 120000;  //ERDDAP changes Cassandra's default if the dataset doesn't
        socOpt.setReadTimeoutMillis(ti);
        if (verbose) String2.log("  " + opt + "  set to " + ti);

        opt = "receiveBufferSize";
        if (verbose) String2.log("  " + opt + " default=" + 
            socOpt.getReceiveBufferSize()); //null
        ti = String2.parseInt(connectionProperties.get(opt));
        if (ti >= 10000 && ti < Integer.MAX_VALUE) {
            socOpt.setReceiveBufferSize(ti);
            if (verbose) String2.log("  " + opt + "  set to " + ti);
        }

        opt = "soLinger";
        if (verbose) String2.log("  " + opt + " default=" + 
            socOpt.getSoLinger()); //null
        ti = String2.parseInt(connectionProperties.get(opt));
        if (ti >= 1 && ti < Integer.MAX_VALUE) {
            socOpt.setSoLinger(ti);
            if (verbose) String2.log("  " + opt + "  set to " + ti);
        }

        opt = "tcpNoDelay";
        if (verbose) String2.log("  " + opt + " default=" + 
            socOpt.getTcpNoDelay()); //null
        ts = connectionProperties.get(opt);
        if (String2.isSomething(ts)) {
            tb = String2.parseBoolean(ts);
            socOpt.setTcpNoDelay(tb);
            if (verbose) String2.log("  " + opt + "  set to " + tb);
        }

        builder = builder.withSocketOptions(socOpt);

        //build the cluster
        Cluster cluster = builder.build();
        if (verbose) {
            Metadata metadata = cluster.getMetadata();
            String2.log("clusterName=" + metadata.getClusterName());
            for (Host host : metadata.getAllHosts()) 
                String2.log("datacenter=" + host.getDatacenter() +
                    " host=" + host.getAddress() + 
                    " rack=" + host.getRack());            
        }

        session = cluster.connect();
        sessionsMap.put(url, session);
        if (verbose) String2.log("  Success! time=" + 
            (System.currentTimeMillis() - tTime)); 
        return session;
    }

    /**
     * This shuts down all of this program's connections to Cassandra 
     * clusters and sessions.
     */
    public static void shutdown() {
        String2.log("starting EDDTableFromCassandra.shutdown()");
        long time = System.currentTimeMillis();
        try {
            if (sessionsMap == null) 
                return;
            Session sessions[] = sessionsMap.values().toArray(new Session[0]);
            for (Session session: sessions) {
                try {
                    session.getCluster().close(); //or are they all the same cluster?
                } catch (Throwable t) {
                    String2.log(MustBe.throwableToString(t));
                }
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }

        //Aid in garbage collection. Prevent other threads from initiating clusters and sessions.
        sessionsMap = null; 
        String2.log("EDDTableFromCassandra.shutdown() finished in " + 
            (System.currentTimeMillis() - time) + "ms");
    }


    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * <p>See CQL SELECT documentation
     * http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/select_r.html
     *
     * <p>The method prevents CQL Injection Vulnerability
     * (see http://en.wikipedia.org/wiki/SQL_injection) by using
     * QueryBuilder (so String values are properly escaped and
     * numbers are assured to be numbers). See
     * http://www.datastax.com/documentation/developer/java-driver/2.0/pdf/javaDriver20.pdf
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {
       
        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            //timeStamp constraints other than regex are epochSeconds
            constraintVariables, constraintOps, constraintValues); 

        //apply constraints to PartitionKeysDistinctTable
        Table pkdTable = new Table();
        pkdTable.readFlatNc(datasetDir() + PartitionKeysDistinctTableName,
            partitionKeyNames, 0);
        int oPkdTableNRows = pkdTable.nRows();
        BitSet pkdKeep = new BitSet(); 
        pkdKeep.set(0, oPkdTableNRows); //all true
        boolean allConstraintsHandled = true;
        int nCon = constraintVariables.size();
        BitSet conKeep = new BitSet(nCon); //all false
        for (int cv = 0; cv < nCon; cv++) {
            String cVar = constraintVariables.get(cv); //sourceName
            String cOp  = constraintOps.get(cv);
            String cVal = constraintValues.get(cv);
            double cValD = String2.parseDouble(cVal);
            int dv = String2.indexOf(dataVariableSourceNames, cVar);
            boolean isNumericEDV = dataVariables[dv].sourceDataTypeClass() != String.class;             

            //for WHERE below, just keep constraints applicable to:
            //  clusterColumnSourceNames (ops: = > >= < <= ) or
            //  indexColumnSourceNames (ops: = )
            conKeep.set(cv, 
                (clusterColumnSourceNames.contains(cVar) &&
                  !cOp.equals("!=") && 
                  !cOp.equals(PrimitiveArray.REGEX_OP) &&
                  !(isNumericEDV && !Math2.isFinite(cValD))) || //don't constrain numeric cols with NaN 
                (indexColumnSourceNames.contains(cVar) &&
                  cOp.equals("=") && //secondary index column only allow '=' constraints
                  !(isNumericEDV && !Math2.isFinite(cValD)))); //don't constrain numeric cols with NaN 

            //Is this a constraint directly on a partitionKey?
            int pkin = String2.indexOf(partitionKeyNames, cVar); //Names!
            if (pkin < 0) //no, it isn't
                allConstraintsHandled = false;

            //is it a special timestamp var?
            //(fixed value partition variables already constrained during 
            //  creation of partitionKeyDistinctTable)
            int pkif = String2.indexOf(partitionKeyFrom, cVar); //From!
            if (pkif >= 0 && !cOp.equals(PrimitiveArray.REGEX_OP) && 
                Math2.isFinite(cValD)) {
                //cVal is epoch seconds
                String origCon = cVar + cOp + cVal;
                cVar = partitionKeyNames[pkif];
                cValD = Calendar2.isoStringToEpochSeconds(
                    Calendar2.epochSecondsToLimitedIsoStringT(
                        partitionKeyPrecision[pkif], cValD, ""));
                cVal = "" + cValD;
                if (reallyVerbose) String2.log("timestamp conversion from " +
                    origCon + " to " + cVar + cOp + cVal);
            }
            
            //try to apply it (even != and regex_op can be applied here)
            int tNRows = pkdTable.tryToApplyConstraint(-1, cVar, cOp, cVal, pkdKeep);
            //String2.log("After " + cVar + cOp + cVal + " nRows=" + tNRows);
            if (tNRows == 0) 
                throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                    " (no matching partition key values)");
        }

        //compact the pkdTable (needs a lot of memory and will be in memory for a long time)
        pkdTable.justKeep(pkdKeep);
        int pkdTableNRows = pkdTable.nRows();
        //diagnostics printed below
        double fraction = pkdTableNRows / (double)oPkdTableNRows;
        if (fraction > maxRequestFraction) 
            throw new SimpleException("You are requesting too much data. " +
                "Please further constrain one or more of these variables: " +
                partitionKeyRelatedVariables +
                ". (" + pkdTableNRows + "/" + oPkdTableNRows + "=" + fraction + " > " + 
                maxRequestFraction + ")");

        //compact constraints
        constraintVariables.justKeep(conKeep);
        constraintOps.justKeep(conKeep);
        constraintValues.justKeep(conKeep);
        nCon = constraintVariables.size();

        //distinct? orderBy?  
        //ERDDAP handles them because Cassandra can only handle some cases. 
        //http://planetcassandra.org/blog/composite-keys-in-apache-cassandra/
        //says, to use "ORDER BY in queries on a table, then you will have to use 
        //  composite-key in that table and that composite key must include the 
        //  field that you wish to sort on. You have to decide which fields 
        //  you wish to sort on when you design your data model, not when you
        //  formulate queries."
        //FOR NOW, have ERDDAP TableWriters handle them, not Cassandra

        //build the statement
        //only BoundStatement lets me bind values one at a time
        //http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/select_r.html
        StringBuilder query = new StringBuilder();  
        int nRv = resultsVariables.size();
        boolean allRvAreInPkdTable = true;
        for (int rv = 0; rv < nRv; rv++) {
            //no danger of cql injection since query has been parsed 
            //  so resultsVariables must be known sourceNames
            //Note that I tried to use '?' for resultsVariables, but never got it to work: wierd results.
            //Quotes around colNames avoid trouble when colName is a CQL reserved word.
            query.append((rv == 0? "SELECT " : ", ") + 
                columnNameQuotes + resultsVariables.get(rv) + columnNameQuotes); 
            if (allRvAreInPkdTable && //no sense in looking
                pkdTable.findColumnNumber(resultsVariables.get(rv)) < 0)
                allRvAreInPkdTable = false;
        }

        //are we done?  allConstraintsHandled and all resultsVars are in pkdTable
        if (allConstraintsHandled && allRvAreInPkdTable) {
            if (verbose) String2.log("Request handled by partitionKeyDistinctTable.");
            preStandardizeResultsTable(loggedInAs, pkdTable); 
            standardizeResultsTable(requestUrl, userDapQuery, pkdTable);
            tableWriter.writeSome(pkdTable);
            tableWriter.finish();
            return;
        }

        //Lack of quotes around table names means they can't be CQL reserved words.
        //(If do quote in future, quote individual parts.)
        query.append(" FROM " + keyspace + "." + tableName);

        //add partitionKey constraints to query:
        //1 constraint per partitionKeyName (all are simple: name=value)
        // and gather pa's from the pkdTable
        PrimitiveArray pkdPA[] = new PrimitiveArray[nPartitionKeys];
        for (int pki = 0; pki < nPartitionKeys; pki++) {    
            //again, no danger of cql injection since query has been parsed and
            //  constraintVariables must be known sourceNames
            //Double quotes around colNames avoid trouble when colName is a CQL 
            //  reserved word or has odd characters.
            //http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/escape_char_r.html
            query.append((pki == 0? " WHERE " : " AND ") +
                columnNameQuotes + partitionKeyNames[pki] + columnNameQuotes + 
                " = ?"); //? is the place holder for a value
            pkdPA[pki] = pkdTable.getColumn(pki);
        }

        //add constraints on clusterColumnSourceNames
        EDV conEDV[] = new EDV[nCon];
        for (int cv = 0; cv < nCon; cv++) {    
            String conVar = constraintVariables.get(cv);
            int dv = String2.indexOf(dataVariableSourceNames(), conVar);
            conEDV[cv] = dataVariables[dv];

            //again, no danger of cql injection since query has been parsed and
            //  constraintVariables must be known sourceNames
            //Double quotes around colNames avoid trouble when colName is a CQL 
            //  reserved word or has odd characters.
            //http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/escape_char_r.html
            query.append(" AND " +
                columnNameQuotes + conVar + columnNameQuotes + " " + 
                constraintOps.get(cv) + " ?"); //? is the place holder for a value
        }

        //LIMIT?
        //  The default row LIMIT is 10000. We want them all!
        //  Some documentation says this limits the number of columns.
        //  Some documentation says this limits the number of rows.
        //  I think it is the number of columns in a column family (e.g., 1 partition key)
        //    which are like rows in a database.
        //  http://stackoverflow.com/questions/25567518/cassandra-cql3-select-statement-without-limit
        //  Asks ~ Do I have to use a huge LIMIT to get all the rows?
        //  Answers: This is a common misconception. There is only a default 10000
        //    row limit in cqlsh the interactive shell. The server and protocol 
        //    do not have a default or maximum number of rows that can be returned. 
        //    There is a timeout though which will stop running queries to protect
        //    users from running malformed queries which could cause system instability.
        //2014-11-19 I removed: LIMIT 2000000000, which caused problems at ONC

        //ALLOW FILTERING causes Cassandra to allow some requests it might not
        //  normally allow because of "performance unpredictability".
        //http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/select_r.html
        query.append(" ALLOW FILTERING;"); 
        if (verbose)
            String2.log("statement as text: " + query.toString());

        //Cassandra doesn't like preparing the same query more than once.
        // "WARNING: Re-preparing already prepared query SELECT deviceid, sampletime, ctext
        //  FROM bobKeyspace.bobTable WHERE deviceid = ? AND date = ? ALLOW FILTERING;. 
        //  Please note that preparing the same query more than once is generally an 
        //  anti-pattern and will likely affect performance. Consider preparing the 
        //  statement only once."
        //  And see https://datastax-oss.atlassian.net/browse/JAVA-236
        //So ERDDAP caches session+query->PreparedStatement.
        //  I can't solve the problem further (cache is lost when ERDDAP is restarted)
        //  without great effort.  I'm not even sure it is possible (de/serialize?).
        //  Restarting creates new sessions. Maybe that solves the problem.
        String queryString = query.toString();         
        //PreparedStatements should be unique for a given session.
        //It is the session that prepares the statement.
        //queryString includes the keyspace.tablename.
        //Session unique for a given localSourceUrl.
        String tKey = localSourceUrl + "\n" + queryString;
        PreparedStatement preparedStatement = statementMap.get(tKey);
        if (preparedStatement == null) {
            preparedStatement = session.prepare(queryString);
            statementMap.put(tKey, preparedStatement);
        }
        //preparedStatement.toString() is useless

        //gather the dataVariables[i] of each resultsVaraible
        int resultsDVI[] = new int[nRv];  //dataVariables[i] (DVI) for each resultsVariable
        EDV rvToResultsEDV[] = new EDV[nRv];
        for (int rv = 0; rv < nRv; rv++) {
            String tName = resultsVariables.get(rv); //a sourceName
            resultsDVI[rv] = String2.indexOf(dataVariableSourceNames, tName);
            rvToResultsEDV[rv] = dataVariables[resultsDVI[rv]];
        }
        //triggerNRows + 1000 since lists expand, so hard to catch exactly
        int triggerNRows = EDStatic.partialRequestMaxCells / nRv;
        Table table = makeEmptySourceTable(rvToResultsEDV, triggerNRows + 1000); 

        //make a call to Cassandra for each row in pkdTable 
        //(each relevant distinct combination of partitionKey values)
        int stats[] = new int[4]; //all 0's
        for (int pkdRow = 0; pkdRow < pkdTableNRows; pkdRow++) { //chunks will be in sorted order, yea!

            //Make the BoundStatement
            //***!!! This method avoids CQL/SQL Injection Vulnerability !!!***
            //(see http://en.wikipedia.org/wiki/SQL_injection) by using
            //preparedStatements (so String values are properly escaped and
            //numbers are assured to be numbers).
            //*** Plus, the statement is reused many times (so Prepared is recommended).
            BoundStatement boundStatement = new BoundStatement(preparedStatement);

            //assign values to nPartitionKeys constraints then nCon constraints
            for (int i = 0; i < nPartitionKeys + nCon; i++) { 
                boolean usePK = i < nPartitionKeys;
                int coni = i - nPartitionKeys; //which con to use: only used if not !usePK

                EDV edv = usePK? partitionKeyEDV[i] : conEDV[coni];
                PrimitiveArray pa = usePK? pkdPA[i] : null;
                Class tClass = edv.sourceDataTypeClass();
                String conVal = usePK? null : constraintValues.get(coni);

                //handle special cases first
                if (edv instanceof EDVTimeStamp) {
                    boundStatement.setDate(i, //partition key value won't be nan/null                
                        new Date(Math.round(
                            (usePK? pa.getDouble(pkdRow) : String2.parseDouble(conVal)) 
                            * 1000))); //round to nearest milli
                } else if (edv.isBoolean()) {
                    boundStatement.setBool(i, 
                        (usePK? pa.getInt(pkdRow) == 1 : String2.parseBoolean(conVal)));
                } else if (tClass == double.class) {
                    boundStatement.setDouble(i, 
                        (usePK? pa.getDouble(pkdRow) : String2.parseDouble(conVal)));
                } else if (tClass == float.class) {
                    boundStatement.setFloat(i, 
                        (usePK? pa.getFloat(pkdRow) : String2.parseFloat(conVal)));
                } else if (tClass == int.class ||    
                         tClass == short.class || 
                         tClass == byte.class) {
                    boundStatement.setInt(i, 
                        (usePK? pa.getInt(pkdRow) : String2.parseInt(conVal))); 
                } else {
                    String val = usePK? pa.getString(pkdRow) : conVal;
                    if (tClass == String.class)   
                        boundStatement.setString(i, val);
                    else if (tClass == long.class)
                        boundStatement.setLong(  i, String2.parseLong(val));
                    else if (tClass == char.class)
                        boundStatement.setString(i, 
                            val.length() == 0? "\u0000" : val.substring(0, 1)); //FFFF??? 
                    else throw new RuntimeException(
                        "Unexpected dataType=" + edv.sourceDataType() + 
                        "for var=" + edv.destinationName() + ".");            
                }
            }
            //boundStatement.toString() is useless

            //get the data
            table = getDataForCassandraQuery(loggedInAs, requestUrl, userDapQuery,
                resultsDVI, rvToResultsEDV, session, boundStatement, 
                table, tableWriter, stats);
            if (tableWriter.noMoreDataPlease) 
                break;
        }

        //write any data in table
        //C* doesn't seem to have resultSet.close, statement.close(), ...
        //(In any case, gc should close them.)
        preStandardizeResultsTable(loggedInAs, table); 
        if (table.nRows() > 0) {
            //String2.log("preStandardize=\n" + table.dataToCSVString());
            standardizeResultsTable(requestUrl, userDapQuery, table);
            stats[3] += table.nRows();
            tableWriter.writeSome(table); //ok if 0 rows
        }
        if (verbose) String2.log("* Cassandra stats: partitionKeyTable: " +
            pkdTableNRows + "/" + oPkdTableNRows + "=" + fraction + " <= " + 
                maxRequestFraction +
            " nCassRows=" + stats[1] + " nErddapRows=" + stats[2] + 
            " nRowsToUser=" + stats[3]);
        tableWriter.finish();
    }


    /** 
     * This executes the query statement and may write some data to the tablewriter. 
     * This doesn't call tableWriter.finish();
     *
     * @param resultsDVI dataVariables[i] (DVI) for each resultsVariable
     * @param stats is int[4]. stats[0]++; stats[1]+=nRows; stats[2]+=nExpandedRows; 
     *    stats[3]+=nRowsAfterStandardize
     * @return the same or a different table (usually with some results rows)
     */
    public Table getDataForCassandraQuery(
        String loggedInAs, String requestUrl, String userDapQuery, 
        int resultsDVI[], EDV rvToResultsEDV[],
        Session session, Statement statement, 
        Table table, TableWriter tableWriter, int[] stats) throws Throwable {

        //statement.toString() is useless

        //execute the statement
        ResultSet rs = session.execute(statement);
        ColumnDefinitions columnDef = rs.getColumnDefinitions();
        int nColumnDef = columnDef.size();
        stats[0]++;

        //connect result set columns to table columns
        int nRv = resultsDVI.length;
        int rvToRsCol[]= new int[nRv]; //stored as 0..
        DataType rvToCassDataType[] = new DataType[nRv];
        for (int rv = 0; rv < nRv; rv++) {
            //find corresponding resultSet column (may not be 1:1) and other info
            ////stored as 0..   -1 if not found
            String sn = rvToResultsEDV[rv].sourceName();
            rvToRsCol[rv] = columnDef.getIndexOf(sn); 
            if (rvToRsCol[rv] < 0) {
                StringArray tsa = new StringArray();
                for (int i = 0; i < nColumnDef; i++) 
                    tsa.add(columnDef.getName(i));
                throw new SimpleException(MustBe.InternalError + 
                    ": sourceName=" + sn + " not in Cassandra resultsSet columns=\"" + 
                    tsa.toString() + "\".");
            }
            rvToCassDataType[rv] = columnDef.getType(rvToRsCol[rv]);
        }
        int triggerNRows = EDStatic.partialRequestMaxCells / nRv;
        PrimitiveArray paArray[] = new PrimitiveArray[nRv];
        for (int rv = 0; rv < nRv; rv++) 
            paArray[rv] = table.getColumn(rv);

        //process the resultSet rows of data
        Row row;
        int maxNRows = -1;
        while ((row = rs.one()) != null) {
            stats[1]++;
            int listSizeDVI = -1;
            int listSize = -1; //initially unknown 
            for (int rv = 0; rv < nRv; rv++) {
                int rsCol = rvToRsCol[rv];
                EDV edv = rvToResultsEDV[rv];
                PrimitiveArray pa = paArray[rv];
                if (rsCol == -1 || row.isNull(rsCol)) { //not in resultSet or isNull
                    pa.addString("");
                    maxNRows = Math.max(maxNRows, pa.size());
                    continue;
                }
                Class tClass = edv.sourceDataTypeClass();
                if (isListDV[resultsDVI[rv]]) {            
                    int tListSize = -1;
                    if (edv.isBoolean()) { //special case
                        List<Boolean> list = row.getList(rsCol, Boolean.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addInt(list.get(i)? 1 : 0);
                    } else if (edv instanceof EDVTimeStamp) { //zulu millis -> epoch seconds
                        List<Date> list = row.getList(rsCol, Date.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addDouble(list.get(i).getTime() / 1000.0); 
                    } else if (tClass == String.class) {
                        //This doesn't support lists of maps/sets/lists. 
                        List<String> list = row.getList(rsCol, String.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addString(list.get(i)); 
                    } else if (tClass == double.class) {
                        List<Double> list = row.getList(rsCol, Double.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addDouble(list.get(i)); 
                    } else if (tClass == float.class) {
                        List<Float> list = row.getList(rsCol, Float.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addFloat(list.get(i)); 
                    } else if (tClass == long.class) {
                        List<Long> list = row.getList(rsCol, Long.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addLong(list.get(i)); 
                    } else { //erddap byte, short, int
                        List<Integer> list = row.getList(rsCol, Integer.class);
                        tListSize = list.size();
                        for (int i = 0; i < tListSize; i++)
                            pa.addInt(list.get(i)); 
                    }

                    //ensure valid
                    if (listSize == -1) {
                        listSizeDVI = resultsDVI[rv];
                        listSize = tListSize;
                    } else if (listSize != tListSize) {
                        String2.log("This resultSet row has different list sizes=\n" + 
                            row.toString());
                        throw new RuntimeException("Source data error: on one row, " +
                            "two list variables have lists of different sizes (" +
                            edv.destinationName() + "=" + tListSize + " != " + 
                            dataVariableDestinationNames[listSizeDVI] + "=" + listSize + ").");
                    }
                } else {
                    if (edv.isBoolean()) { //special case
                        pa.addInt(row.getBool(rsCol)? 1 : 0);
                    } else if (edv instanceof EDVTimeStamp) { //zulu millis -> epoch seconds
                        pa.addDouble(row.getDate(rsCol).getTime() / 1000.0); 
                    } else if (tClass == String.class) {
                        //getString doesn't return the String form of any type
                        //https://datastax-oss.atlassian.net/browse/JAVA-135
                        Object value = rvToCassDataType[rv].
                            deserialize(row.getBytesUnsafe(rsCol), protocolVersion);
                        pa.addString(value.toString()); 
                    } else if (tClass == double.class) {
                        pa.addDouble(row.getDouble(rsCol)); 
                    } else if (tClass == float.class) {
                        pa.addFloat(row.getFloat(rsCol)); 
                    } else if (tClass == long.class) {
                        pa.addLong(row.getLong(rsCol)); 
                    } else { //erddap byte, short, int
                        pa.addInt(row.getInt(rsCol)); 
                    }
                }
                maxNRows = Math.max(maxNRows, pa.size());
            }
            stats[2] += Math.max(1, listSize);

            //expand scalars and missing values to fill maxNRows
            for (int rv = 0; rv < nRv; rv++) {
                PrimitiveArray pa = paArray[rv];
                int n = maxNRows - pa.size();
                if (n > 0) {
                    Class tClass = pa.elementClass();
                    if (tClass == String.class ||
                        tClass == long.class) {
                        pa.addNStrings(n, pa.getString(pa.size() - 1)); 
                    } else if (tClass == double.class || 
                               tClass == float.class) {
                        pa.addNDoubles(n, pa.getDouble(pa.size() - 1)); 
                    } else {
                        pa.addNInts(n, pa.getInt(pa.size() - 1)); 
                    }
                }
            }

            //standardize a chunk and write to tableWriter.writeSome ?
            if (maxNRows >= triggerNRows) {
                //String2.log(table.toString("rows",5));
                preStandardizeResultsTable(loggedInAs, table); 
                if (table.nRows() > 0) {
                    standardizeResultsTable(requestUrl, userDapQuery, table); //changes sourceNames to destinationNames
                    stats[3] += table.nRows();
                    tableWriter.writeSome(table); //okay if 0 rows
                }

                //triggerNRows + 1000 since lists expand, so hard to catch exactly
                table = makeEmptySourceTable(rvToResultsEDV, triggerNRows + 1000);
                for (int rv = 0; rv < nRv; rv++) 
                    paArray[rv] = table.getColumn(rv);
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
        //this base version does nothing
    }
  


    /** 
     * This generates a datasets.xml entry for an EDDTableFromCassandra.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * The dataVariable sourceNames are always in sorted order. That's the
     * order that 
     *
     * @param url the Cassandra URL, e.g., 127.0.0.1   (assumed port=9160)
     * @param connectionProperties  see description for class constructor
     * @param keyspace the keyspace name 
     *    or use "!!!LIST!!!" to get a list of keyspaces.
     * @param tableName 
     *    or use "!!!LIST!!!" to get the metadata for all tableNames in the keyspace.
     * @param tReloadEveryNMinutes  e.g., DEFAULT_RELOAD_EVERY_N_MINUTES (10080) for weekly
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble. 
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String url, String tConnectionProperties[], String keyspace, String tableName,
        int tReloadEveryNMinutes,
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes)
        throws Throwable {

        String2.log("EDDTableFromCassandra.generateDatasetsXml keyspace=" + keyspace);         
        if (tReloadEveryNMinutes < suggestReloadEveryNMinutesMin ||
            tReloadEveryNMinutes > suggestReloadEveryNMinutesMax)
            tReloadEveryNMinutes = 1440; //not the usual DEFAULT_RELOAD_EVERY_N_MINUTES;

        //Querying a system table
        //http://www.datastax.com/documentation/cql/3.1/cql/cql_using/use_query_system_c.html        

        if (tConnectionProperties == null) 
            tConnectionProperties = new String[0];
        Test.ensureTrue(!Math2.odd(tConnectionProperties.length), 
            "connectionProperties.length must be an even number.");
        ConcurrentHashMap<String,String> conProp = new ConcurrentHashMap();
        for (int i = 0; i < tConnectionProperties.length; i += 2) {
            String tKey   = tConnectionProperties[i];
            String tValue = tConnectionProperties[i+1];
            if (String2.isSomething(tKey) && tValue != null)
                conProp.put(tKey, tValue);  //<String,String>
        }


        //make/get the session (and hold local reference)
        Session session = getSession(url, conProp);
        //int protocolVersion = session.getCluster().getConfiguration().getProtocolOptions().getProtocolVersion();

        //* just get a list of keyspaces
        Metadata clusterMetadata = session.getCluster().getMetadata();
        if (keyspace.equals(LIST)) {
            if (verbose) String2.log("getting keyspace list");
            return String2.toNewlineString(String2.toStringArray(
                clusterMetadata.getKeyspaces().toArray()));
        }

        //* just get info for all tables in keyspace
        KeyspaceMetadata km = clusterMetadata.getKeyspace(keyspace);
        if (km == null) 
            throw new RuntimeException("No metadata for keyspace=" + keyspace);
        if (tableName.equals(LIST)) {
            if (verbose) String2.log("getting tableName list");
            return km.exportAsString();
        }

        //* generateDatasetsXml for one Cassandra table 
        //partition key  (not a Set, because order is important)
        TableMetadata tm = km.getTable(tableName);
        StringArray partitionKeySA = new StringArray(); //sourceNames
        StringArray subsetVariablesSourceNameSA = new StringArray();
        List<ColumnMetadata> pk = tm.getPartitionKey();
        for (int i = 0; i < pk.size(); i++) {
            partitionKeySA.add(pk.get(i).getName()); 
            subsetVariablesSourceNameSA.add(pk.get(i).getName()); 
        }

        //clusterColumn and indexColumn (could be accumulated as a Set)
        StringArray clusterColumnSA = new StringArray(); //souceNames
        List<ColumnMetadata> cc = tm.getClusteringColumns();
        for (int i = 0; i < cc.size(); i++) 
            clusterColumnSA.add(cc.get(i).getName());

        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        List<ColumnMetadata> cmList = tm.getColumns();
        boolean isList[] = new boolean[cmList.size()];
        StringArray indexColumnSA = new StringArray(); //souceNames
        for (int col = 0; col < cmList.size(); col++) {

            ColumnMetadata cm = cmList.get(col);
            String sourceName = cm.getName();
            DataType cassType = cm.getType();

            if (cm.isStatic()) {
                //static columns are DISTINCT able and have few values, 
                //  so they are good for subsetVariables
                //(but they aren't constrainable)
                if (subsetVariablesSourceNameSA.indexOf(sourceName) < 0)
                    subsetVariablesSourceNameSA.add(sourceName);
            }
            
            if (cm.getIndex() != null) {
                //indexed columns are only constrainable with '=')
                if (indexColumnSA.indexOf(sourceName) < 0)
                    indexColumnSA.add(sourceName);
            }
            
            //Cass identifiers are [a-zA-Z0-9_]*
            String destName = 
                ("0123456789".indexOf(sourceName.charAt(0)) >= 0 ? "_" : "") +
                sourceName;
            if (sourceName.equals("lat")) destName = EDV.LAT_NAME;
            if (sourceName.equals("lon")) destName = EDV.LON_NAME;

            PrimitiveArray pa = null;
            Attributes sourceAtts = new Attributes();
            //lie, to trigger catching LLAT
            if (     destName.equals(EDV.LON_NAME))   sourceAtts.add("units", EDV.LON_UNITS);
            else if (destName.equals(EDV.LAT_NAME))   sourceAtts.add("units", EDV.LAT_UNITS);
            else if (destName.equals(EDV.ALT_NAME))   sourceAtts.add("units", EDV.ALT_UNITS);
            else if (destName.equals(EDV.DEPTH_NAME)) sourceAtts.add("units", EDV.DEPTH_UNITS);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                null, //no source global attributes
                sourceAtts, sourceName, true, true); //addColorBarMinMax, tryToFindLLAT
            //but make it real here, and undo the lie
            if (     destName.equals(EDV.LON_NAME))   {
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
            //time units already done above for all timestamp vars

            isList[col] = cassType.asJavaClass() == List.class;
            //String2.log(sourceName + " isList=" + isList[col] + " javaClass=" + cassType.asJavaClass());
            if (isList[col])
                cassType = cassType.getTypeArguments().get(0); //the element type

            if      (cassType == DataType.cboolean())   pa = new ByteArray();
            else if (cassType == DataType.cint())       pa = new IntArray();
            else if (cassType == DataType.bigint() ||
                     cassType == DataType.counter() ||
                     cassType == DataType.varint())     pa = new LongArray();
            else if (cassType == DataType.cfloat())     pa = new FloatArray();
            else if (cassType == DataType.cdouble() ||
                     cassType == DataType.decimal())    pa = new DoubleArray();
            else if (cassType == DataType.timestamp()) {pa = new DoubleArray();
                addAtts.add("ioos_category", "Time");
                addAtts.add("units", "seconds since 1970-01-01T00:00:00Z");
            } else                                      pa = new StringArray(); //everything else

            dataSourceTable.addColumn(col, sourceName, pa, sourceAtts);

            dataAddTable.addColumn(col, destName, pa, addAtts);
        }

        //subsetVariables source->dest name
        StringArray subsetVariablesDestNameSA = new StringArray(
            subsetVariablesSourceNameSA.size(), true);
        //String2.log(">> subsetVarSourceNames=" + subsetVariablesSourceNameSA.toString());
        for (int sv = 0; sv < subsetVariablesSourceNameSA.size(); sv++) {
            subsetVariablesDestNameSA.set(sv,
                dataAddTable.getColumnName(
                    dataSourceTable.findColumnNumber(subsetVariablesSourceNameSA.get(sv))));
        }
        //String2.log(">> subsetVarDestNames=" + subsetVariablesDestNameSA.toString());

        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) 
            externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) 
            externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) 
            externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) 
            externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", 
            "(local Cassandra)");
        //String2.log(">> ext subsetVariables=" + externalAddGlobalAttributes.getString("subsetVariables"));
        externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", 
            subsetVariablesDestNameSA.toString());
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataSourceTable, dataAddTable)? "Point" : "Other",
                "cassandra/" + keyspace + "/" + tableName, //fake file dir.  Cass identifiers are [a-zA-Z0-9_]*
                externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        
        //write the information
        StringBuilder sb = new StringBuilder();
        sb.append(
            directionsForGenerateDatasetsXml() +
            " *** Since Cassandra tables don't have any metadata, you must add metadata\n" +
            "   below, notably 'units' for each of the dataVariables.\n" +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromCassandra\" datasetID=\"cass" + 
                //Cass identifiers are [a-zA-Z0-9_]*
                ( keyspace.startsWith("_")? "" : "_") + keyspace + 
                (tableName.startsWith("_")? "" : "_") + tableName +
                "\" active=\"true\">\n" +
            "    <sourceUrl>" + url + "</sourceUrl>\n");
        for (int i = 0; i < tConnectionProperties.length; i += 2) 
            sb.append(
                "    <connectionProperty name=\"" + XML.encodeAsXML(tConnectionProperties[i]) + "\">" + 
                    XML.encodeAsXML(tConnectionProperties[i+1]) + "</connectionProperty>\n");
        sb.append(
            "    <keyspace>" + keyspace + "</keyspace>\n" +  //safe since Cass identifiers are [a-zA-Z0-9_]*
            "    <tableName>" + tableName + "</tableName>\n" +  //safe
            "    <partitionKeySourceNames>" + partitionKeySA.toString() + "</partitionKeySourceNames>\n" + 
            "    <clusterColumnSourceNames>" + clusterColumnSA.toString() + "</clusterColumnSourceNames>\n" + 
            "    <indexColumnSourceNames>" + indexColumnSA.toString() + "</indexColumnSourceNames>\n" + 
            "    <maxRequestFraction>1</maxRequestFraction>\n" + 
            "    <columnNameQuotes></columnNameQuotes>\n" + //default = empty string
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, false, false)); 
        sb.append(
            "</dataset>\n" +
            "\n");

        //convert boolean var dataType from byte to boolean
        String2.replaceAll(sb, "<dataType>byte", "<dataType>boolean"); 

        //convert lists to List <dataType>'s    e.g., float -> floatList
        for (int col = 0; col < isList.length; col++) {
            if (isList[col]) {
                String find = "<sourceName>" + dataSourceTable.getColumnName(col) + "</sourceName>";
                int po = sb.indexOf(find);                    
                if (po < 0)
                    throw new RuntimeException(
                        "Internal ERROR: \"" + find + "\" not found in sb=\n" +
                        sb.toString());
                po = sb.indexOf("</dataType>", po + find.length());
                if (po < 0)
                    throw new RuntimeException(
                        "Internal ERROR: \"" + find + "\" + </dataType> not found in sb=\n" +
                        sb.toString());
                sb.insert(po, "List");
            }
        }
        
        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }


    /**
     * This tests generateDatasetsXml.
     * 
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDTableFromCassandra.testGenerateDatasetsXml");
        testVerboseOn();
        String url="127.0.0.1";  //:9160
        String props[] = {};
        String keyspace = "bobKeyspace";
        String tableName = "bobTable";
        int tReloadEveryNMinutes = -1;
        String tInfoUrl = "http://www.oceannetworks.ca/";
        String tInstitution = "Ocean Networks Canada";
        String tSummary = "The summary for Bob's great Cassandra test data.";
        String tTitle = "The Title for Bob's Cassandra Test Data";
        //addGlobalAtts.
        String results, expected;

        //get the list of keyspaces
        results = generateDatasetsXml(url, props, LIST, "", 
            tReloadEveryNMinutes, tInfoUrl, tInstitution, 
            tSummary, tTitle, new Attributes());
expected = 
"CREATE KEYSPACE \"OpsCenter\" WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;\n" +
"CREATE KEYSPACE bobkeyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2' } AND DURABLE_WRITES = true;\n" +
"CREATE KEYSPACE system_traces WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2' } AND DURABLE_WRITES = true;\n" +
"CREATE KEYSPACE system WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.LocalStrategy' } AND DURABLE_WRITES = true;\n" +
"CREATE KEYSPACE demodb WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.NetworkTopologyStrategy', 'datacenter1': '3' } AND DURABLE_WRITES = true;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //get the metadata for all tables in a keyspace
        results = generateDatasetsXml(url, props, keyspace, LIST, 
            tReloadEveryNMinutes, tInfoUrl, tInstitution, 
            tSummary, tTitle, new Attributes());
expected = 
"CREATE KEYSPACE bobkeyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '2' } AND DURABLE_WRITES = true;\n" +
"\n" +
"CREATE TABLE bobkeyspace.bobtable (\n" +
"    deviceid int,\n" +
"    date timestamp,\n" +
"    sampletime timestamp,\n" +
"    cascii ascii,\n" +
"    cboolean boolean,\n" +
"    cbyte int,\n" +
"    cdecimal double,\n" +
"    cdouble double,\n" +
"    cfloat float,\n" +
"    cint int,\n" +
"    clong bigint,\n" +
"    cmap map<text, double>,\n" +
"    cset set<text>,\n" +
"    cshort int,\n" +
"    ctext text,\n" +
"    cvarchar text,\n" +
"    depth list<float>,\n" +
"    u list<float>,\n" +
"    v list<float>,\n" +
"    w list<float>,\n" +
"    PRIMARY KEY ((deviceid, date), sampletime)\n" +
") WITH read_repair_chance = 0.0\n" +
"   AND dclocal_read_repair_chance = 0.1\n" +
"   AND replicate_on_write = true\n" +
"   AND gc_grace_seconds = 864000\n" +
"   AND bloom_filter_fp_chance = 0.01\n" +
"   AND caching = 'KEYS_ONLY'\n" +
"   AND comment = ''\n" +
"   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy' }\n" +
"   AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n" +
"   AND default_time_to_live = 0\n" +
"   AND speculative_retry = '99.0PERCENTILE'\n" +
"   AND index_interval = 128;\n" +
"CREATE INDEX ctext_index ON bobkeyspace.bobtable (ctext);\n" +
"\n" +
"CREATE TABLE bobkeyspace.statictest (\n" +
"    deviceid int,\n" +
"    date timestamp,\n" +
"    sampletime timestamp,\n" +
"    depth list<float>,\n" +
"    lat float static,\n" +
"    lon float static,\n" +
"    u list<float>,\n" +
"    v list<float>,\n" +
"    PRIMARY KEY ((deviceid, date), sampletime)\n" +
") WITH read_repair_chance = 0.0\n" +
"   AND dclocal_read_repair_chance = 0.1\n" +
"   AND replicate_on_write = true\n" +
"   AND gc_grace_seconds = 864000\n" +
"   AND bloom_filter_fp_chance = 0.01\n" +
"   AND caching = 'KEYS_ONLY'\n" +
"   AND comment = ''\n" +
"   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy' }\n" +
"   AND compression = { 'sstable_compression' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n" +
"   AND default_time_to_live = 0\n" +
"   AND speculative_retry = '99.0PERCENTILE'\n" +
"   AND index_interval = 128;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //generate the datasets.xml for one table
        results = generateDatasetsXml(url, props, keyspace, tableName, 
            tReloadEveryNMinutes, tInfoUrl, tInstitution, 
            tSummary, tTitle, new Attributes());
expected = 
"<!--\n" +
" DISCLAIMER:\n" +
"   The chunk of datasets.xml made by GenerageDatasetsXml isn't perfect.\n" +
"   YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
"   GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
"   correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
"   THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
"\n" +
" DIRECTIONS:\n" +
" * Read about this type of dataset in\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, depth, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n" +
" *** Since Cassandra tables don't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromCassandra\" datasetID=\"cass_bobKeyspace_bobTable\" active=\"true\">\n" +
"    <sourceUrl>127.0.0.1</sourceUrl>\n" +
"    <keyspace>bobKeyspace</keyspace>\n" +
"    <tableName>bobTable</tableName>\n" +
"    <partitionKeySourceNames>deviceid, date</partitionKeySourceNames>\n" +
"    <clusterColumnSourceNames>sampletime</clusterColumnSourceNames>\n" +
"    <indexColumnSourceNames>ctext</indexColumnSourceNames>\n" +
"    <maxRequestFraction>1</maxRequestFraction>\n" + 
"    <columnNameQuotes></columnNameQuotes>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">Ocean Networks Canada</att>\n" +
"        <att name=\"creator_url\">http://www.oceannetworks.ca/</att>\n" +
"        <att name=\"infoUrl\">http://www.oceannetworks.ca/</att>\n" +
"        <att name=\"institution\">Ocean Networks Canada</att>\n" +
"        <att name=\"keywords\">bob, bobtable, canada, cascii, cassandra, cboolean, cbyte, cdecimal, cdouble, cfloat, cint, clong, cmap, cset, cshort, ctext, currents, cvarchar, data, date, depth, deviceid, networks, ocean, sampletime, test, time, title, u, v, velocity, vertical, w</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local Cassandra)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"subsetVariables\">deviceid, date</att>\n" +
"        <att name=\"summary\">The summary for Bob&#39;s great Cassandra test data.</att>\n" +
"        <att name=\"title\">The Title for Bob&#39;s Cassandra Test Data (bobTable)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>deviceid</sourceName>\n" +
"        <destinationName>deviceid</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Deviceid</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>date</sourceName>\n" +
"        <destinationName>date</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Date</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sampletime</sourceName>\n" +
"        <destinationName>sampletime</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Sampletime</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cascii</sourceName>\n" +
"        <destinationName>cascii</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cascii</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cboolean</sourceName>\n" +
"        <destinationName>cboolean</destinationName>\n" +
"        <dataType>boolean</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cboolean</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cbyte</sourceName>\n" +
"        <destinationName>cbyte</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cbyte</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cdecimal</sourceName>\n" +
"        <destinationName>cdecimal</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cdecimal</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cdouble</sourceName>\n" +
"        <destinationName>cdouble</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cdouble</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cfloat</sourceName>\n" +
"        <destinationName>cfloat</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cfloat</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cint</sourceName>\n" +
"        <destinationName>cint</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cint</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>clong</sourceName>\n" +
"        <destinationName>clong</destinationName>\n" +
"        <dataType>long</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Clong</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cmap</sourceName>\n" +
"        <destinationName>cmap</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cmap</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cset</sourceName>\n" +
"        <destinationName>cset</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cset</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cshort</sourceName>\n" +
"        <destinationName>cshort</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cshort</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ctext</sourceName>\n" +
"        <destinationName>ctext</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Ctext</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>cvarchar</sourceName>\n" +
"        <destinationName>cvarchar</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Cvarchar</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>depth</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">OceanDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>u</sourceName>\n" +
"        <destinationName>u</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">U</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>v</sourceName>\n" +
"        <destinationName>v</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">V</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>w</sourceName>\n" +
"        <destinationName>w</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Vertical Velocity</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
            //String2.log(results);
            Test.ensureEqual(results, expected, "results=\n" + results);
            //String2.pressEnterToContinue(); 

            
        //generate the datasets.xml for a table with static columns
        results = generateDatasetsXml(url, props, keyspace, "staticTest", 
            tReloadEveryNMinutes, tInfoUrl, tInstitution, 
            tSummary, "Cassandra Static Test", new Attributes());
        expected = 
"<!--\n" +
" DISCLAIMER:\n" +
"   The chunk of datasets.xml made by GenerageDatasetsXml isn't perfect.\n" +
"   YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
"   GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
"   correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
"   THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
"\n" +
" DIRECTIONS:\n" +
" * Read about this type of dataset in\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, depth, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n" +
" *** Since Cassandra tables don't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromCassandra\" datasetID=\"cass_bobKeyspace_staticTest\" active=\"true\">\n" +
"    <sourceUrl>127.0.0.1</sourceUrl>\n" +
"    <keyspace>bobKeyspace</keyspace>\n" +
"    <tableName>staticTest</tableName>\n" +
"    <partitionKeySourceNames>deviceid, date</partitionKeySourceNames>\n" +
"    <clusterColumnSourceNames>sampletime</clusterColumnSourceNames>\n" +
"    <indexColumnSourceNames></indexColumnSourceNames>\n" +
"    <maxRequestFraction>1</maxRequestFraction>\n" +
"    <columnNameQuotes></columnNameQuotes>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">Ocean Networks Canada</att>\n" +
"        <att name=\"creator_url\">http://www.oceannetworks.ca/</att>\n" +
"        <att name=\"infoUrl\">http://www.oceannetworks.ca/</att>\n" +
"        <att name=\"institution\">Ocean Networks Canada</att>\n" +
"        <att name=\"keywords\">canada, cassandra, data, date, depth, deviceid, latitude, longitude, networks, ocean, sampletime, static, test, time, u, v</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local Cassandra)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"subsetVariables\">deviceid, date, latitude, longitude</att>\n" +
"        <att name=\"summary\">The summary for Bob&#39;s great Cassandra test data.</att>\n" +
"        <att name=\"title\">Cassandra Static Test</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>deviceid</sourceName>\n" +
"        <destinationName>deviceid</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Deviceid</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>date</sourceName>\n" +
"        <destinationName>date</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Date</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sampletime</sourceName>\n" +
"        <destinationName>sampletime</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Sampletime</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>depth</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">OceanDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>u</sourceName>\n" +
"        <destinationName>u</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">U</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>v</sourceName>\n" +
"        <destinationName>v</destinationName>\n" +
"        <dataType>floatList</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">V</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n\n";
            //String2.log(results);
            Test.ensureEqual(results, expected, "results=\n" + results);

    }

    /**
     * This performs basic tests of the local Cassandra database.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean pauseBetweenTests) throws Throwable {
        String2.log("\n*** EDDTableFromCassandra.testBasic");
        testVerboseOn();
        long cumTime = 0;
        String query;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String tDatasetID = "cass_bobKeyspace_bobTable";

        try {
            EDDTableFromCassandra tedd = (EDDTableFromCassandra)oneFromDatasetXml(tDatasetID); 
            cumTime = System.currentTimeMillis();
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nDataset constructed.\n" +
                    "Paused to allow you to check the connectionProperty's."); 
/* */
            tName = tedd.makeNewFileForDapQuery(null, null, "", 
                dir, tedd.className() + "_Basic", ".dds"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Int32 deviceid;\n" +
"    Float64 date;\n" +
"    Float64 sampletime;\n" +
"    String cascii;\n" +
"    Byte cboolean;\n" +
"    Int32 cbyte;\n" +
"    Float64 cdecimal;\n" +
"    Float64 cdouble;\n" +
"    Float32 cfloat;\n" +
"    Int32 cint;\n" +
"    Float64 clong;\n" +
"    String cmap;\n" +
"    String cset;\n" +
"    Int32 cshort;\n" +
"    String ctext;\n" +
"    String cvarchar;\n" +
"    Float32 depth;\n" +
"    Float32 u;\n" +
"    Float32 v;\n" +
"    Float32 w;\n" +
"  } s;\n" +
"} s;\n";
              //String2.log("\n>> .das results=\n" + results);
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
  
            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", 
                dir, 
                tedd.className() + "_Basic", ".das"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"Attributes {\n" +
" s {\n" +
"  deviceid {\n" +
"    Int32 actual_range 1001, 1009;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Deviceid\";\n" +
"  }\n" +
"  date {\n" +
"    Float64 actual_range 1.4148e+9, 1.4154912e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sampletime {\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Sampletime\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  cascii {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cascii\";\n" +
"  }\n" +
"  cboolean {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cboolean\";\n" +
"  }\n" +
"  cbyte {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cbyte\";\n" +
"  }\n" +
"  cdecimal {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cdecimal\";\n" +
"  }\n" +
"  cdouble {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cdouble\";\n" +
"  }\n" +
"  cfloat {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cfloat\";\n" +
"  }\n" +
"  cint {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cint\";\n" +
"  }\n" +
"  clong {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Clong\";\n" +
"  }\n" +
"  cmap {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cmap\";\n" +
"  }\n" +
"  cset {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cset\";\n" +
"  }\n" +
"  cshort {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cshort\";\n" +
"  }\n" +
"  ctext {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Ctext\";\n" +
"  }\n" +
"  cvarchar {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cvarchar\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 8000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"OceanDepth\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  u {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"U\";\n" +
"  }\n" +
"  v {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"V\";\n" +
"  }\n" +
"  w {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"W\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Other\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_name \"Ocean Networks Canada\";\n" +
"    String creator_url \"http://www.oceannetworks.ca/\";\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results);
    
//    \"2014-11-15T15:05:05Z (Cassandra)
//2014-11-15T15:05:05Z http://127.0.0.1:8080/cwexperimental/tabledap/cass_bobKeyspace_bobTable.das\";
expected = 
    "String infoUrl \"http://www.oceannetworks.ca/\";\n" +
"    String institution \"Ocean Networks Canada\";\n" +
"    String keywords \"bob, canada, cascii, cboolean, cbyte, cdecimal, cdouble, cfloat, cint, clong, cmap, cset, cshort, ctext, cvarchar, data, date, depth, deviceid, networks, ocean, sampletime, test, time\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(Cassandra)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"deviceid, date\";\n" +
"    String summary \"The summary for Bob's Cassandra test data.\";\n" +
"    String title \"Bob's Cassandra Test Data\";\n" +
"  }\n" +
"}\n";
            po = results.indexOf(expected.substring(0, 14));
            if (po < 0) String2.log("results=\n" + results);            
            Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

            //all     
            query = "";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_all", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,10.1,-0.11,-0.12,-0.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,20.1,0.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,30.1,0.11,0.12,0.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,cvarchar2,10.2,-2.11,-2.12,-2.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,cvarchar2,20.2,0.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,cvarchar2,30.2,0.11,2.12,2.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,cvarchar3,10.3,-3.11,-3.12,-3.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,cvarchar3,20.3,0.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,cvarchar3,30.3,3.11,3.12,3.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,10.1,-0.11,-0.12,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,20.1,0.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,30.1,0.11,0.12,0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,10.2,-0.11,-99.0,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,20.2,-99.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,-99.0,0.11,0.12,-99.0\n" +
"1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,cvarchar7,10.7,-7.11,-7.12,-7.13\n" +
"1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,cvarchar7,20.7,0.0,NaN,0.0\n" +
"1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,cvarchar7,30.7,7.11,7.12,7.13\n" +
"1008,2014-11-08T00:00:00Z,2014-11-08T01:02:03Z,ascii8,0,8,8.00001,8.001,8.1,8000000,8000000000000,\"{map81=8.1, map82=8.2, map83=8.3, map84=8.4}\",\"[set81, set82, set83, set84, set85]\",8000,text8,cvarchar8,10.8,-8.11,-8.12,-8.13\n" +
"1008,2014-11-08T00:00:00Z,2014-11-08T01:02:03Z,ascii8,0,8,8.00001,8.001,8.1,8000000,8000000000000,\"{map81=8.1, map82=8.2, map83=8.3, map84=8.4}\",\"[set81, set82, set83, set84, set85]\",8000,text8,cvarchar8,20.8,0.0,NaN,0.0\n" +
"1008,2014-11-08T00:00:00Z,2014-11-08T01:02:03Z,ascii8,0,8,8.00001,8.001,8.1,8000000,8000000000000,\"{map81=8.1, map82=8.2, map83=8.3, map84=8.4}\",\"[set81, set82, set83, set84, set85]\",8000,text8,cvarchar8,30.8,8.11,8.12,8.13\n" +
"1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,NaN,NaN,NaN,NaN,NaN,NaN,,,NaN,,,NaN,NaN,NaN,NaN\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: all\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset   test sampletime ">=" handled correctly
            query = "deviceid,sampletime,cmap&deviceid=1001&sampletime>=2014-11-01T03:02:03Z";
            tName = tedd.makeNewFileForDapQuery(null, null, query, 
                dir, tedd.className() + "_subset1", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cmap\n" +
",UTC,\n" +
"1001,2014-11-01T03:02:03Z,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\"\n" +
"1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n" +
"1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset   test sampletime \">=\" handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset   test sampletime ">" handled correctly
            query = "deviceid,sampletime,cmap&deviceid=1001&sampletime>2014-11-01T03:02:03Z";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_subset2", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cmap\n" +
",UTC,\n" +
"1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n" +
"1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset   test sampletime \">\" handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset   test secondary index: ctext '=' handled correctly
            //so erddap tells Cass to handle this constraint
            query = "deviceid,sampletime,ctext&ctext=\"text1\"";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_subset2", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,ctext\n" +
",UTC,\n" +
"1001,2014-11-01T01:02:03Z,text1\n" +
"1001,2014-11-02T01:02:03Z,text1\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset   test secondary index: ctext '=' handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset  with code changes to allow any constraint on secondary index:
            //  proves ctext '>=' not allowed
            //so this tests that erddap handles the constraint
            query = "deviceid,sampletime,ctext&ctext>=\"text3\"";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_subset2", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,ctext\n" +
",UTC,\n" +
"1001,2014-11-01T03:02:03Z,text3\n" +
"1007,2014-11-07T01:02:03Z,text7\n" +
"1008,2014-11-08T01:02:03Z,text8\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset  test secondary index: ctext '>=' handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //distinct()   subsetVariables
            query = "deviceid,cascii&deviceid=1001&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_distinct", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,cascii\n" +
",\n" +
"1001,\n" +
"1001,ascii1\n" +
"1001,ascii2\n" +
"1001,ascii3\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: distinct()\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //orderBy()   subsetVariables
            query = "deviceid,sampletime,cascii&deviceid=1001&orderBy(\"cascii\")";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_distinct", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cascii\n" +
",UTC,\n" +
"1001,2014-11-02T02:02:03Z,\n" +
"1001,2014-11-01T01:02:03Z,ascii1\n" +
"1001,2014-11-02T01:02:03Z,ascii1\n" +
"1001,2014-11-01T02:02:03Z,ascii2\n" +
"1001,2014-11-01T03:02:03Z,ascii3\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: orderBy()\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //just keys   deviceid
            query = "deviceid,date&deviceid=1001";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_justkeys", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date\n" +
",UTC\n" +
"1001,2014-11-01T00:00:00Z\n" +
"1001,2014-11-02T00:00:00Z\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: just keys   deviceid\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //no matching data (no matching keys)
            try {
                query = "deviceid,sampletime&sampletime<2013-01-01";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "_nodata1", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = t.toString(); 
                String2.log(msg);
                Test.ensureEqual(msg, 
                    "com.cohort.util.SimpleException: Your query produced no matching results. " +
                    "(no matching partition key values)", "");
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: no matching data (no matching keys)\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset cint=NaN
            query = "&cint=NaN";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_intNaN", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,10.2,-0.11,-99.0,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,20.2,-99.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,-99.0,0.11,0.12,-99.0\n" +
"1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,NaN,NaN,NaN,NaN,NaN,NaN,,,NaN,,,NaN,NaN,NaN,NaN\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset cfloat=NaN
            query = "&cfloat=NaN";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_floatNaN", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,10.2,-0.11,-99.0,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,20.2,-99.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,-99.0,0.11,0.12,-99.0\n" +
"1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,NaN,NaN,NaN,NaN,NaN,NaN,,,NaN,,,NaN,NaN,NaN,NaN\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset cboolean=NaN
            query = "&cboolean=NaN";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_booleanNaN", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1009,2014-11-09T00:00:00Z,2014-11-09T01:02:03Z,,NaN,NaN,NaN,NaN,NaN,NaN,NaN,,,NaN,,,NaN,NaN,NaN,NaN\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset cboolean=1     
            query = "&cboolean=1";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_boolean1", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,10.2,-0.11,-99.0,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,20.2,-99.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,-99.0,0.11,0.12,-99.0\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset regex on set
            query = "&cset=~\".*set73.*\"";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_set73", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,cvarchar7,10.7,-7.11,-7.12,-7.13\n" +
"1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,cvarchar7,20.7,0.0,NaN,0.0\n" +
"1007,2014-11-07T00:00:00Z,2014-11-07T01:02:03Z,ascii7,0,7,7.00001,7.001,7.1,7000000,7000000000000,\"{map71=7.1, map72=7.2, map73=7.3, map74=7.4}\",\"[set71, set72, set73, set74, set75]\",7000,text7,cvarchar7,30.7,7.11,7.12,7.13\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //no matching data (sampletime)
            try {
                query = "&deviceid=1001&sampletime<2014-01-01";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "_nodata2", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t); 
                String2.log(msg);
                if (msg.indexOf("Your query produced no matching results.") < 0)
                    String2.pressEnterToContinue("Unexpected error."); 
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: no matching data (sampletime)\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //request a subset of response vars 
            //(Unable to duplicate reported error: my Cass doesn't have 
            //  enough data to trigger partial write to TableWriter)
            //error from ONC for query=
            //rdiadcp_AllSensors_23065.nc?time,depthBins,eastward_seawater_velocity
            //&deviceid>=23065&time>=2014-05-15T02:00:00Z&time<=2014-05-16T00:00:05Z
            query = "sampletime,depth,u" +
            "&deviceid=1001&sampletime>=2014-11-01&sampletime<=2014-11-01T03";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_dup", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"sampletime,depth,u\n" +
"UTC,m,\n" +
"2014-11-01T01:02:03Z,10.1,-0.11\n" +
"2014-11-01T01:02:03Z,20.1,0.0\n" +
"2014-11-01T01:02:03Z,30.1,0.11\n" +
"2014-11-01T02:02:03Z,10.2,-2.11\n" +
"2014-11-01T02:02:03Z,20.2,0.0\n" +
"2014-11-01T02:02:03Z,30.2,0.11\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: just keys   deviceid\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //no matching data (erddap)
            try {
                query = "&deviceid>1001&cascii=\"zztop\"";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "nodata3", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t); 
                String2.log(msg);
                if (msg.indexOf("Your query produced no matching results.") < 0)
                    String2.pressEnterToContinue("Unexpected error."); 
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: no matching data (erddap)\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //finished 
            //cum time ~313, impressive: ~30 subqueries and a lot of time spent
            //  logging to screen.
            String2.log("\n* EDDTableFromCassandra.testBasic finished successfully. time=" + 
                (System.currentTimeMillis() - cumTime));

            /* */
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromCassandra.testBasic error:"); 
        }
    }

    /**
     * This tests maxRequestFraction.
     *
     * @throws Throwable if trouble
     */
    public static void testMaxRequestFraction(boolean pauseBetweenTests)
        throws Throwable {
        String2.log("\n*** EDDTableFromCassandra.testMaxRequestFraction");
        testVerboseOn();
        long cumTime = 0;
        String query = null;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String tDatasetID = "cassTestFraction";

        try {
            EDDTableFromCassandra tedd = (EDDTableFromCassandra)oneFromDatasetXml(tDatasetID); 
            cumTime = System.currentTimeMillis();
            if (pauseBetweenTests)
                String2.pressEnterToContinue("\nDataset constructed."); 

            //all    
            try {
                query = "&deviceid>1000&cascii=\"zztop\"";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "frac", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t); 
                String2.log(msg);
                if (msg.indexOf("You are requesting too much data. " +
                    "Please further constrain one or more of these variables: " +
                    "deviceid, date, sampletime. (5/5=1.0 > 0.55)") < 0)
                    String2.pressEnterToContinue("Unexpected error."); 
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: all\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //still too much     
            try {
                query = "&deviceid>1001&cascii=\"zztop\"";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "frac2", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t); 
                String2.log(msg);
                if (msg.indexOf("You are requesting too much data. " +
                    "Please further constrain one or more of these variables: " +
                    "deviceid, date, sampletime. (3/5=0.6 > 0.55)") < 0)
                    String2.pressEnterToContinue("Unexpected error."); 
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: all\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset  2/5  0.4 is okay
            query = "deviceid,sampletime,cascii&deviceid=1001&sampletime>=2014-11-01T03:02:03Z";
            tName = tedd.makeNewFileForDapQuery(null, null, query, 
                dir, tedd.className() + "_frac3", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cascii\n" +
",UTC,\n" +
"1001,2014-11-01T03:02:03Z,ascii3\n" +
"1001,2014-11-02T01:02:03Z,ascii1\n" +
"1001,2014-11-02T02:02:03Z,\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset   test sampletime \">=\" handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //finished 
            String2.log("\n* EDDTableFromCassandra.testMaxRequestFraction finished successfully. time=" + 
                (System.currentTimeMillis() - cumTime));

            /* */
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromCassandra.testMaxRequestFraction error."); 
        }
    }

    /**
     * This is like testBasic, but on a dataset that is restricted to 1 device (1001).
     *
     * @throws Throwable if trouble
     */
    public static void testCass1Device(boolean pauseBetweenTests) throws Throwable {
        String2.log("\n*** EDDTableFromCassandra.testCass1Device");
        testVerboseOn();
        long cumTime = 0;
        String query;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String tDatasetID = "cass1Device";

        try {
            EDDTableFromCassandra tedd = (EDDTableFromCassandra)oneFromDatasetXml(tDatasetID); 
            cumTime = System.currentTimeMillis();
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nDataset constructed.\n" +
                    "Paused to allow you to check the connectionProperty's."); 

            tName = tedd.makeNewFileForDapQuery(null, null, "", 
                dir, tedd.className() + "_Basic", ".dds"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Int32 deviceid;\n" +
"    Float64 date;\n" +
"    Float64 sampletime;\n" +
"    String cascii;\n" +
"    Byte cboolean;\n" +
"    Int32 cbyte;\n" +
"    Float64 cdecimal;\n" +
"    Float64 cdouble;\n" +
"    Float32 cfloat;\n" +
"    Int32 cint;\n" +
"    Float64 clong;\n" +
"    String cmap;\n" +
"    String cset;\n" +
"    Int32 cshort;\n" +
"    String ctext;\n" +
"    String cvarchar;\n" +
"    Float32 depth;\n" +
"    Float32 u;\n" +
"    Float32 v;\n" +
"    Float32 w;\n" +
"  } s;\n" +
"} s;\n";
              //String2.log("\n>> .das results=\n" + results);
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
  
            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", 
                dir, 
                tedd.className() + "_Basic", ".das"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"Attributes {\n" +
" s {\n" +
"  deviceid {\n" +
"    Int32 actual_range 1001, 1001;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Deviceid\";\n" +
"  }\n" +
"  date {\n" +
"    Float64 actual_range 1.4148e+9, 1.4148864e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sampletime {\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Sampletime\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  cascii {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cascii\";\n" +
"  }\n" +
"  cboolean {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cboolean\";\n" +
"  }\n" +
"  cbyte {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cbyte\";\n" +
"  }\n" +
"  cdecimal {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cdecimal\";\n" +
"  }\n" +
"  cdouble {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cdouble\";\n" +
"  }\n" +
"  cfloat {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cfloat\";\n" +
"  }\n" +
"  cint {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cint\";\n" +
"  }\n" +
"  clong {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Clong\";\n" +
"  }\n" +
"  cmap {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cmap\";\n" +
"  }\n" +
"  cset {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cset\";\n" +
"  }\n" +
"  cshort {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cshort\";\n" +
"  }\n" +
"  ctext {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Ctext\";\n" +
"  }\n" +
"  cvarchar {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cvarchar\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 8000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"OceanDepth\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  u {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"U\";\n" +
"  }\n" +
"  v {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"V\";\n" +
"  }\n" +
"  w {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"W\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Other\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_name \"Ocean Networks Canada\";\n" +
"    String creator_url \"http://www.oceannetworks.ca/\";\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results);
    
//    \"2014-11-15T15:05:05Z (Cassandra)
//2014-11-15T15:05:05Z http://127.0.0.1:8080/cwexperimental/tabledap/cass_bobKeyspace_bobTable.das\";
expected = 
    "String infoUrl \"http://www.oceannetworks.ca/\";\n" +
"    String institution \"Ocean Networks Canada\";\n" +
"    String keywords \"bob, canada, cascii, cboolean, cbyte, cdecimal, cdouble, cfloat, cint, clong, cmap, cset, cshort, ctext, cvarchar, data, date, depth, deviceid, networks, ocean, sampletime, test, time\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(Cassandra)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"deviceid, date\";\n" +
"    String summary \"The summary for Bob's Cassandra test data.\";\n" +
"    String title \"Bob's Cassandra Test Data\";\n" +
"  }\n" +
"}\n";
            po = results.indexOf(expected.substring(0, 14));
            if (po < 0) String2.log("results=\n" + results);            
            Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

            //all     
            query = "";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_all", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,sampletime,cascii,cboolean,cbyte,cdecimal,cdouble,cfloat,cint,clong,cmap,cset,cshort,ctext,cvarchar,depth,u,v,w\n" +
",UTC,UTC,,,,,,,,,,,,,,m,,,\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,10.1,-0.11,-0.12,-0.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,20.1,0.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,30.1,0.11,0.12,0.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,cvarchar2,10.2,-2.11,-2.12,-2.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,cvarchar2,20.2,0.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:03Z,ascii2,0,2,2.00001,2.001,2.1,2000000,2000000000000,\"{map21=2.1, map22=2.2, map23=2.3, map24=2.4}\",\"[set21, set22, set23, set24, set25]\",2000,text2,cvarchar2,30.2,0.11,2.12,2.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,cvarchar3,10.3,-3.11,-3.12,-3.13\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,cvarchar3,20.3,0.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:02:03Z,ascii3,0,3,3.00001,3.001,3.1,3000000,3000000000000,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\",\"[set31, set32, set33, set34, set35]\",3000,text3,cvarchar3,30.3,3.11,3.12,3.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,10.1,-0.11,-0.12,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,20.1,0.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,ascii1,0,1,1.00001,1.001,1.1,1000000,1000000000000,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\",\"[set11, set12, set13, set14, set15]\",1000,text1,cvarchar1,30.1,0.11,0.12,0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,10.2,-0.11,-99.0,-0.13\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,20.2,-99.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T02:02:03Z,,1,NaN,NaN,NaN,NaN,NaN,NaN,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\",\"[, set11, set13, set14, set15]\",NaN,,,-99.0,0.11,0.12,-99.0\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: all\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset   test sampletime ">=" handled correctly
            query = "deviceid,sampletime,cmap&sampletime>=2014-11-01T03:02:03Z";
            tName = tedd.makeNewFileForDapQuery(null, null, query, 
                dir, tedd.className() + "_subset1", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cmap\n" +
",UTC,\n" +
"1001,2014-11-01T03:02:03Z,\"{map31=3.1, map32=3.2, map33=3.3, map34=3.4}\"\n" +
"1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n" +
"1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset   test sampletime \">=\" handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //subset   test sampletime ">" handled correctly
            query = "deviceid,sampletime,cmap&sampletime>2014-11-01T03:02:03Z";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_subset2", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cmap\n" +
",UTC,\n" +
"1001,2014-11-02T01:02:03Z,\"{map11=1.1, map12=1.2, map13=1.3, map14=1.4}\"\n" +
"1001,2014-11-02T02:02:03Z,\"{=1.2, map11=-99.0, map13=1.3, map14=1.4}\"\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: subset   test sampletime \">\" handled correctly\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //distinct()   subsetVariables
            query = "deviceid,cascii&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_distinct", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,cascii\n" +
",\n" +
"1001,\n" +
"1001,ascii1\n" +
"1001,ascii2\n" +
"1001,ascii3\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: distinct()\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //orderBy()   subsetVariables
            query = "deviceid,sampletime,cascii&orderBy(\"cascii\")";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_distinct", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,sampletime,cascii\n" +
",UTC,\n" +
"1001,2014-11-02T02:02:03Z,\n" +
"1001,2014-11-01T01:02:03Z,ascii1\n" +
"1001,2014-11-02T01:02:03Z,ascii1\n" +
"1001,2014-11-01T02:02:03Z,ascii2\n" +
"1001,2014-11-01T03:02:03Z,ascii3\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: orderBy()\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //just keys   deviceid
            query = "deviceid,date";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_justkeys", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date\n" +
",UTC\n" +
"1001,2014-11-01T00:00:00Z\n" +
"1001,2014-11-02T00:00:00Z\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: just keys   deviceid\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //no matching data (no matching keys)
            try {
                query = "deviceid,sampletime&sampletime<2013-01-01";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "_nodata1", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = t.toString(); 
                String2.log(msg);
                Test.ensureEqual(msg, 
                    "com.cohort.util.SimpleException: Your query produced no matching results. " +
                    "(no matching partition key values)", "");
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: no matching data (no matching keys)\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //no matching data (sampletime)
            try {
                query = "&sampletime<2014-01-01";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "_nodata2", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t); 
                String2.log(msg);
                if (msg.indexOf("Your query produced no matching results.") < 0)
                    String2.pressEnterToContinue("Unexpected error."); 
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: no matching data (sampletime)\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //no matching data (erddap)
            try {
                query = "&cascii=\"zztop\"";
                tName = tedd.makeNewFileForDapQuery(null, null, query,
                    dir, tedd.className() + "nodata3", ".csv"); 
                results = new String((new ByteArray(dir + tName)).toArray());
                expected = "Shouldn't get here";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t) {
                String msg = MustBe.throwableToString(t); 
                String2.log(msg);
                if (msg.indexOf("Your query produced no matching results.") < 0)
                    String2.pressEnterToContinue("Unexpected error."); 
            }
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: no matching data (erddap)\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //finished 
            //cum time ~313, impressive: ~30 subqueries and a lot of time spent
            //  logging to screen.
            String2.log("\n* EDDTableFromCassandra.testCass1Device finished successfully. time=" + 
                (System.currentTimeMillis() - cumTime));

            /* */
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromCassandra.testCass1Device error."); 
        }
    }

    /**
     * This is like testBasic, but on a dataset with 2 static columns.
     *
     * @throws Throwable if trouble
     */
    public static void testStatic(boolean pauseBetweenTests) throws Throwable {
        String2.log("\n*** EDDTableFromCassandra.testStatic");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = true;
        long cumTime = 0;
        String query;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String tDatasetID = "cass_bobKeyspace_staticTest";

        try {
            EDDTableFromCassandra tedd = (EDDTableFromCassandra)oneFromDatasetXml(tDatasetID); 
            cumTime = System.currentTimeMillis();
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nDataset constructed.\n" +
                    "Paused to allow you to check the connectionProperty's."); 

            //.dds
            tName = tedd.makeNewFileForDapQuery(null, null, "", 
                dir, tedd.className() + "_Basic", ".dds"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Int32 deviceid;\n" +
"    Float64 date;\n" +
"    Float64 time;\n" +
"    Float32 depth;\n" +
"    Float32 latitude;\n" +
"    Float32 longitude;\n" +
"    Float32 u;\n" +
"    Float32 v;\n" +
"  } s;\n" +
"} s;\n";
              //String2.log("\n>> .das results=\n" + results);
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
  
            //.das 
            tName = tedd.makeNewFileForDapQuery(null, null, "", 
                dir, 
                tedd.className() + "_Basic", ".das"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"Attributes {\n" +
" s {\n" +
"  deviceid {\n" +
"    Int32 actual_range 1001, 1001;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Deviceid\";\n" +
"  }\n" +
"  date {\n" +
"    Float64 actual_range 1.4148e+9, 1.4148864e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Sample Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 8000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"OceanDepth\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 33.0, 34.0;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -124.0, -123.0;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  u {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"U\";\n" +
"  }\n" +
"  v {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"V\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_name \"Ocean Networks Canada\";\n" +
"    String creator_url \"http://www.oceannetworks.ca/\";\n" +
"    Float64 Easternmost_Easting -123.0;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 34.0;\n" +
"    Float64 geospatial_lat_min 33.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -123.0;\n" +
"    Float64 geospatial_lon_min -124.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history "; // "2014-12-10T19:51:59Z (Cassandra)";\n" +
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results);
    
//    \"2014-11-15T15:05:05Z (Cassandra)
//2014-11-15T15:05:05Z http://127.0.0.1:8080/cwexperimental/tabledap/cass_bobKeyspace_bobTable.das\";
expected = 
   "String infoUrl \"http://www.oceannetworks.ca/\";\n" +
"    String institution \"Ocean Networks Canada\";\n" +
"    String keywords \"canada, cassandra, date, depth, deviceid, networks, ocean, sampletime, static, test, time\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 34.0;\n" +
"    String sourceUrl \"(Cassandra)\";\n" +
"    Float64 Southernmost_Northing 33.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"deviceid, date, latitude, longitude\";\n" +
"    String summary \"The summary for Bob's Cassandra test data.\";\n" +
"    String title \"Cassandra Static Test\";\n" +
"    Float64 Westernmost_Easting -124.0;\n" +
"  }\n" +
"}\n";
            po = results.indexOf(expected.substring(0, 14));
            if (po < 0) String2.log("results=\n" + results);            
            Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

            //all     
            query = "";
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "_staticAll", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
//This shows that lat and lon just have different values for each combinations of the
//partition key (deviceid+date).
"deviceid,date,time,depth,latitude,longitude,u,v\n" +
",UTC,UTC,m,degrees_north,degrees_east,,\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,10.1,33.0,-123.0,-0.11,-0.12\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,20.1,33.0,-123.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T01:02:03Z,30.1,33.0,-123.0,0.11,0.12\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:02Z,10.1,33.0,-123.0,-0.21,-0.22\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:02Z,20.1,33.0,-123.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T02:02:02Z,30.1,33.0,-123.0,0.21,0.22\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:03:03Z,10.1,33.0,-123.0,-0.31,-0.32\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:03:03Z,20.1,33.0,-123.0,0.0,0.0\n" +
"1001,2014-11-01T00:00:00Z,2014-11-01T03:03:03Z,30.1,33.0,-123.0,0.31,0.32\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,10.1,34.0,-124.0,-0.41,-0.42\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,20.1,34.0,-124.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,30.1,34.0,-124.0,0.41,0.42\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: all\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //distinct()   subsetVariables
            query = "deviceid,date,latitude,longitude&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, query,
                dir, tedd.className() + "_staticDistinct", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
//diagnostic messages show that ERDDAP got this data from the subset file.
"deviceid,date,latitude,longitude\n" +
",UTC,degrees_north,degrees_east\n" +
"1001,2014-11-01T00:00:00Z,33.0,-123.0\n" +
"1001,2014-11-02T00:00:00Z,34.0,-124.0\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: distinct()\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //static variables are NOT constrainable by Cassandra (even '=' queries)
            //so ERDDAP handles it
            query = "&latitude=34";
            tName = tedd.makeNewFileForDapQuery(null, null, query, 
                dir, tedd.className() + "_staticCon1", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,time,depth,latitude,longitude,u,v\n" +
",UTC,UTC,m,degrees_north,degrees_east,,\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,10.1,34.0,-124.0,-0.41,-0.42\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,20.1,34.0,-124.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,30.1,34.0,-124.0,0.41,0.42\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: showed that static variables are NOT constrainable by Cassandra\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 

            //static variables are NOT constrainable by Cassandra (even '>' queries)
            //so ERDDAP handles it
            query = "&latitude>33.5";
            tName = tedd.makeNewFileForDapQuery(null, null, query, 
                dir, tedd.className() + "_staticCon2", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected =  
"deviceid,date,time,depth,latitude,longitude,u,v\n" +
",UTC,UTC,m,degrees_north,degrees_east,,\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,10.1,34.0,-124.0,-0.41,-0.42\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,20.1,34.0,-124.0,0.0,0.0\n" +
"1001,2014-11-02T00:00:00Z,2014-11-02T01:02:03Z,30.1,34.0,-124.0,0.41,0.42\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            if (pauseBetweenTests)
                String2.pressEnterToContinue(
                    "\nTest: showed that static variables are NOT constrainable by Cassandra\n" +
                    "query=" + query + "\n" +
                    "Paused to allow you to check the stats."); 


            //finished 
            String2.log("\n* EDDTableFromCassandra.testStatic finished successfully. time=" + 
                (System.currentTimeMillis() - cumTime));

            /* */
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromCassandra.testStatic error."); 
        }
        debugMode = oDebugMode;
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromCassandra.test() *****************\n");

        //tests usually run       
        /* */
        testGenerateDatasetsXml();
        testBasic(false); //pauseBetweenTests
        testMaxRequestFraction(false);
        testCass1Device(false); //pauseBetweenTests
        testStatic(false); //pauseBetweenTests
        /* */
    }


}

