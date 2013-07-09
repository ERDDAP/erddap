/* 
 * EDDTableFromDatabase Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
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

import java.io.FileWriter;
import java.io.StringWriter;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

/** 
 * This class represents a table of data from a database.
 * This class avoids the SQL Injection security problem 
 * (see http://en.wikipedia.org/wiki/SQL_injection).
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-08
 */
public class EDDTableFromDatabase extends EDDTable{ 


    /** set by the constructor */
    protected String dataSourceName;
    protected DataSource dataSource; //null if none available
    protected String driverName;
    protected String connectionProperties[]; //may have username and password!
    protected String catalogName;
    protected String schemaName;
    protected String tableName;
    protected String orderBy[];

    protected String catalogSeparator;


    /**
     * This constructs an EDDTableFromDatabase based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDatabase"&gt; 
     *    having just been read.  
     * @return an EDDTableFromDatabase.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromDatabase fromXml(SimpleXMLReader xmlReader) throws Throwable {
        return lowFromXml(xmlReader, "");
    }

    /**
     * This constructs an EDDTableFromDatabase based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDatabase"&gt; 
     *    having just been read.  
     * @param subclass "" for regular or "Post"
     * @return an EDDTableFromDatabase or one of its subclasses.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromDatabase lowFromXml(SimpleXMLReader xmlReader, String subclass) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFrom" + subclass + "Database(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tDataSourceName = null;
        String tLocalSourceUrl = null;
        String tDriverName = null;
        String tCatalogName = "";
        String tSchemaName = "";
        String tTableName = null;
        String tOrderBy[] = new String[0];
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
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<dataSourceName>")) {}
            else if (localTags.equals("</dataSourceName>")) tDataSourceName = content; 
            else if (localTags.equals( "<driverName>")) {}
            else if (localTags.equals("</driverName>")) tDriverName = content; 
            else if (localTags.equals( "<connectionProperty>")) tConnectionProperties.add(xmlReader.attributeValue("name"));
            else if (localTags.equals("</connectionProperty>")) tConnectionProperties.add(content); 
            else if (localTags.equals( "<catalogName>")) {}
            else if (localTags.equals("</catalogName>")) tCatalogName = content; 
            else if (localTags.equals( "<schemaName>")) {}
            else if (localTags.equals("</schemaName>")) tSchemaName = content; 
            else if (localTags.equals( "<tableName>")) {}
            else if (localTags.equals("</tableName>")) tTableName = content; 
            else if (localTags.equals( "<orderBy>")) {}
            else if (localTags.equals("</orderBy>")) {
                if (content != null && content.length() > 0)
                    tOrderBy = String2.split(content, ',');
            }
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
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

        if (subclass.equals("Post"))
            return new EDDTableFromPostDatabase(tDatasetID, tAccessibleTo, 
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery, 
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, 
                tDataSourceName,
                tLocalSourceUrl, tDriverName, 
                tConnectionProperties.toArray(),
                tCatalogName, tSchemaName, tTableName, tOrderBy,
                tSourceNeedsExpandedFP_EQ);
        else return new EDDTableFromDatabase(tDatasetID, tAccessibleTo, 
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery, 
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes, 
                tDataSourceName,
                tLocalSourceUrl, tDriverName, 
                tConnectionProperties.toArray(),
                tCatalogName, tSchemaName, tTableName, tOrderBy,
                tSourceNeedsExpandedFP_EQ);

    }

    /**
     * The constructor.
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
     *
     * <p>Security Features - When working with databases, you need to do things as safely and
     * securely as possible to avoid allowing a malicious user to 
     * damage your database or gain access to data they shouldn't have access to.
     * ERDDAP tries to do things in a secure way, too.
     * <ul>
     * <li>We encourage you to set up ERDDAP to connect to the database as a 
     *     database user that only has access to the *relevant* database(s)
     *     and only has READ privileges.
     * <li>We encourage you to set up the connection from ERDDAP 
     *    to the database so that it always uses SSL, only allows connections
     *    from one IP address and from the one ERDDAP user, and only 
     *    transfers passwords in their MD5 hashed form.
     * <li>[BAD]The database password is stored as plain text in datasets.xml
     *    (only the administrator should have READ access to this file). 
     *    We haven't found a way to allow the administrator to enter the database
     *    password during ERDDAP's startup in Tomcat (which occurs without user input),
     *    so the password must be accessible in a file.
     * <li>When in ERDDAP, the password is kept "private".
     * <li>Requests from clients are parsed and checked for validity before
     *     generating requests for the database.
     * <li>Requests to the database are made with PreparedStatements, 
     *   to avoid SQL injection.
     * <li>Requests to the database are submitted with executeQuery 
     *     (not executeStatement) to limit requests to be read-only
     *     (so attempted SQL injection to alter the database will fail for this 
     *     reason, too).
     * </ul>
     * 
     * <p>It can be difficult for ERDDAP to successfully make the connection
     * to the database. In addition to properly specifying the &lt;dataset&gt;
     * info in datasets.xml:
     * <ul>
     * <li>You need to put the appropriate database driver .jar
     *   file (for example, postgresql-8.3-603.jdbc3.jar) in [tomcat]/common/lib .
     * <li>You need to configure the database to have an ERDDAP user (you choose the userName) 
     *   who can only READ the database, not make any changes.
     * <li>You need to configure the database to allow a connection from 
     *   the ERDDAP user.  Be as restrictive as you can.
     *   If this is a remote connection, it should always use SSL. 
     *   Passwords should never be transmitted as plaintext (use MD5 or another means instead).
     *   <br>For example, for Postgres, you need to make a pg_hba.conf file
     *   (see http://developer.postgresql.org/pgdocs/postgres/auth-pg-hba-conf.html)
     *   with a line in the form of  
     *   <br>"<tt>hostssl    database  user  CIDR-address  auth-method  [auth-option]</tt>"
     *   <br>for example, 
     *   <br>"<tt>hostssl    myDatabase  myUserId  65.219.21.0/24  md5</tt>" 
     * </ul>
     *
     * @param tDatasetID is a very short string identifier 
     *   (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tAddGlobalAttributes are global attributes which define 
     *   the data source's global attributes (since there are no source attributes).
     *   This may be null if you have nothing to add.
     *   The addGlobalAttributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line). 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of value="[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tDataVariables is an Object[nDataVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=String the source dataType (e.g., "boolean", "int", "float", "String", ...). 
     *        This is needed because databases types are not universally consistent
     *        in what how they assign JDBC data types to the database's internal data types
     *        (see http://www.onlamp.com/pub/a/onlamp/2001/09/13/aboutSQL.html?page=last).
     *        Even if they were, there are some difficult types, e.g., unsigned integer types.
     *        So you need to explicitly define the dataTypes. 
     *        <br>For Date, Time, and Timestamp database types, use "String".
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <p>If there is a time variable,  
     *      either tAddAttributes (read first) or tSourceAttributes must have "units"
     *      which is either <ul>
     *      <li> a UDUunits string (containing " since ")
     *        describing how to interpret source time values 
     *        (which should always be numeric since they are a dimension of a grid)
     *        (e.g., "seconds since 1970-01-01T00:00:00").
     *      <li> a org.joda.time.format.DateTimeFormat string
     *        (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *        string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *        http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html or 
     *        http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).
     *      </ul>
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tDataSourceName the name of the dataSource (for connection pooling)
     *    defined in [tomcat]/conf/server.xml (or null or "" of not used)
     * @param tLocalSourceUrl the url needed to connect to the database
     *    (it includes the database name at the end),  e.g., 
     *   jdbc:postgresql://otter.pfeg.noaa.gov/posttest .
     * @param tDriverName the Java class name, e.g., org.postgresql.Driver.
     *    The file containing the class (e.g., postgresql-8.3-603.jdbc3.jar) 
     *    must be in the classpath (usually in [tomcat]/common/lib
     *    or you'll get a runtime error. 
     * @param tConnectionProperties is an alternating list of names and values used 
     *    to make a connection to the database.
     *    <br>It may be null or length=0.
     *    <br>For example name={"user", "erdUser", "password", "myPassword", "ssl", "true",
     *      "sslfactory", "org.postgresql.ssl.NonValidatingFactory"}.
     *    <p>SSL is an important security issue.
     *    <br>Remember that userNames and password MD5's will be transmitted
     *    between ERDDAP and the database. 
     *    <br>So, if the database is local, you may chose not to use SSL 
     *    for the connection. But for all remote databases, it is 
     *    STRONGLY RECOMMENDED that you use SSL.
     *    <br>See http://jdbc.postgresql.org/documentation/81/ssl-client.html .
     * @param tCatalogName   use "" if not needed
     * @param tSchemaName    use "" if not needed
     * @param tTableName
     * @param tOrderBy is an array of sourceNames (use String[0] if not needed)
     *    which are used to construct an ORDER BY clause for a query.
     *    Only sourceNames which are relevant to a given query are used in the
     *    ORDER BY clause.
     *    The leftmost sourceName is most important; subsequent sourceNames are only used to break ties.
     * @param tSourceNeedsExpandedFP_EQ
     * @throws Throwable if trouble
     */
    public EDDTableFromDatabase(String tDatasetID, String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tDataSourceName, 
        String tLocalSourceUrl, String tDriverName, 
        String tConnectionProperties[],
        String tCatalogName, String tSchemaName, String tTableName,
        String tOrderBy[],
        boolean tSourceNeedsExpandedFP_EQ
        ) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromDatabase " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromDatabase(" + 
            tDatasetID + ") constructor:\n";
            
        //save the some of the parameters
        className = "EDDTableFromDatabase"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        Test.ensureNotNothing(tLocalSourceUrl, "'sourceUrl' wasn't defined.");
        Test.ensureNotNothing(tDriverName, "'driverName' wasn't defined.");
        //catalog and schema may be ""
        Test.ensureNotNothing(tTableName, "'tableName' wasn't defined.");
        dataSourceName = tDataSourceName;
        if (tConnectionProperties == null) tConnectionProperties = new String[0];
        Test.ensureTrue(!Math2.odd(tConnectionProperties.length), 
            "connectionProperties.length must be an even number.");
        publicSourceUrl = "(source database)"; //not tLocalSourceUrl; keep it private
        addGlobalAttributes.set("sourceUrl", publicSourceUrl);  
        localSourceUrl = tLocalSourceUrl;
        driverName = tDriverName;
        connectionProperties = tConnectionProperties;
        catalogName = tCatalogName;
        schemaName = tSchemaName;
        tableName = tTableName;
        orderBy = tOrderBy == null? new String[0] : tOrderBy;

        //try to get the dataSource
        if (dataSourceName != null && dataSourceName.length() > 0) {
            try {
                String2.log("\nTrying to find dataSourceName=" + dataSourceName + "..."); 
                Context context = new InitialContext();
                dataSource = (DataSource)context.lookup(dataSourceName); //may be null
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
            String2.log("dataSourceName=" + dataSourceName + 
                (dataSource == null? 
                    " wasn't found, so connection pooling won't be used.\n" + 
                    "  (Isn't this code running in an application server like Tomcat?\n" +
                    "  Did you define the resource in, e.g., [tomcat]/conf/context.xml ?)\n" :

                    " was successfully found, so connection pooling will be used.\n"));
        } else {
            String2.log("\ndataSourceName wasn't specified, so connection pooling won't be used.\n"); 
        }

        //sql can support everything except regex constraints
        sourceNeedsExpandedFP_EQ      = tSourceNeedsExpandedFP_EQ;
        sourceCanConstrainNumericData = CONSTRAIN_YES;
        sourceCanConstrainStringData  = CONSTRAIN_YES; 
        sourceCanConstrainStringRegex = "";
      
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
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = (String)tDataVariables[dv][3];
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
                    tSourceAtt, tAddAtt, tSourceType);
                timeIndex = dv;
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            } else {
                dataVariables[dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            }
        }

        //get the connection
        //This is also an important test of ability to make a connection.
        Connection connection = makeConnection(dataSourceName, dataSource, 
            localSourceUrl, driverName, connectionProperties);
        try {
            DatabaseMetaData meta = connection.getMetaData();
            catalogSeparator = meta.getCatalogSeparator();

            //finally
            if (verbose) {
                String2.log(
                    //don't display connectionProperties because of password
                    "\ndatabase name=" + meta.getDatabaseProductName() +
                        " version=" + meta.getDatabaseProductVersion() +
                    "\ndriver name=" + meta.getDriverName() + 
                        " version=" + meta.getDriverVersion() +
                    "\njdbc majorVersion=" + meta.getJDBCMajorVersion() + 
                        " minorVersion=" + meta.getJDBCMinorVersion() +
                    (reallyVerbose? "\n" + toString() : "") +
                    "\n*** EDDTableFromDatabase " + datasetID + " constructor finished. TIME=" + 
                    (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
            }
        } finally {
            connection.close();
        }

        //Don't gather ERDDAP sos information.
        //I am guessing that requesting the min/maxTime for each station is 
        //  *very* taxing for most databases.

        //ensure the setup is valid
        ensureValid();

    }

    /**
     * This makes a new database Connection.
     * If dataSource != null, it will be used to get the connection; else via DriverManager.
     * See the connectionProperties documentation for the class constructor.
     */
    public static Connection makeConnection(String dataSourceName, DataSource dataSource,
        String url, String driverName, String connectionProperties[]) throws Throwable {

        long tTime = System.currentTimeMillis();

        if (dataSource == null) {
            //get a connection via DriverManager
            if (verbose) String2.log(
                "EDDTableFromDatabase.getConnection via DriverManager + datasets.xml info");
            Class.forName(driverName); //to load the jdbc driver

            //see example (with SSL info) at http://jdbc.postgresql.org/documentation/80/connect.html
            Properties props = new Properties();
            for (int i = 0; i < connectionProperties.length; i += 2) 
                props.setProperty(connectionProperties[i], connectionProperties[i + 1]);
            Connection con = DriverManager.getConnection(url, props);
            if (verbose) String2.log("  Success! time=" + 
                (System.currentTimeMillis() - tTime)); //often slow!
            return con;
        } else {
            //get a connection from the connection pool via DataSource
            if (verbose) String2.log(
                "EDDTableFromDatabase.getConnection from pool via DataSource + [tomcat]/conf/server.xml info");
            Connection con = dataSource.getConnection();
            if (verbose) String2.log("  Success! time=" + 
                (System.currentTimeMillis() - tTime)); //should be very fast
            return con;
        }
    }

    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * <p>The method avoids SQL Injection Vulnerability
     * (see http://en.wikipedia.org/wiki/SQL_injection) by using
     * preparedStatements (so String values are properly escaped and
     * numbers are assured to be numbers).
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //good summary of using statements, queries, resultSets, ...
        //  http://download.oracle.com/javase/7/docs/guide/jdbc/getstart/resultset.html

        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

        //distinct?  orderBy?  databases can handle them
        //making database do distinct seems useful (maybe it can optimize, data transfer greatly reduced
        //but orderBy may be slow/hard for database (faster to do it in erddap?)
        String[] parts = getUserQueryParts(userDapQuery); //decoded.  
        boolean distinct = false;
        boolean queryHasOrderBy = false;
        boolean queryHasOrderByMax = false;
        for (int pi = 0; pi < parts.length; pi++) {
            String p = parts[pi];
            if (p.equals("distinct()")) distinct = true;
            if (p.startsWith("orderBy(\"") && p.endsWith("\")")) 
                queryHasOrderBy = true;
            if (p.startsWith("orderByMax(\"") && p.endsWith("\")")) 
                queryHasOrderByMax = true;
        }

        //no need to further prune constraints

        try {
            //make sure the connection is valid
//??? Use a ConnectionPool???  See JDBC API Tutorial book, pg 640; or search web.
//For now, make a new connection each time???  I think that is excessive, but simple.
//  see connection.close() below.

            //See javadocs for isClosed -- it isn't very useful. So do quick test:
            /*if (connection != null && !connection.isClosed()) {
                try {
                    connection.getCatalog(); //simple test; ignore the response
                } catch (Throwable t) {
                    String2.log("The existing connection isn't working. Closing it and creating a new one...");
                    try {
                        connection.close();
                    } catch (Throwable t2) {
                    }
                    connection = null; //so new one will be created below
                }
            }

            //open a new connection?
            if (connection == null || connection.isClosed()) */
            Connection connection = makeConnection(dataSourceName, dataSource,
                localSourceUrl, driverName, connectionProperties);        
            try {

                //build the sql query
                StringBuilder query = new StringBuilder();
                int nRv = resultsVariables.size();
                String distinctString = distinct? "DISTINCT " : "";
                for (int rv = 0; rv < nRv; rv++) 
                    //no danger of sql injection since query has been parsed and
                    //  resultsVariables must be known sourceNames
                    //Note that I tried to use '?' for resultsVariables, but never got it to work: wierd results.
                    //Quotes around colNames avoid trouble when colName is a SQL reserved word.
                    query.append((rv == 0? "SELECT " + distinctString : ", ") + 
                        '"' + resultsVariables.get(rv) + '"'); 
                //Lack of quotes around table names means they can't be SQL reserved words.
                //(If do quote in future, quote individual parts.)
                query.append(" FROM " + 
                    (catalogName.equals("")? "" : catalogName + catalogSeparator) + 
                    (schemaName.equals( "")? "" : schemaName  + ".") + 
                    tableName);

                //add orderBy
                StringBuilder orderBySB = new StringBuilder();
                if (distinct || queryHasOrderBy || queryHasOrderByMax) {
                    //let TableWriterXxx handle it (probably more efficient since in memory)
                } else {
                    //append predefined orderBy variables
                    for (int ob = 0; ob < orderBy.length; ob++) {
                        if (resultsVariables.indexOf(orderBy[ob]) >= 0) {
                            if (orderBySB.length() > 0) orderBySB.append(", ");
                            //Quotes around colNames avoid trouble when colName is a SQL reserved word.
                            orderBySB.append("\"" + orderBy[ob] + "\"");
                        }
                    }
                }

                //add constraints to query  
                int nCv = constraintVariables.size();
                StringBuilder humanQuery = new StringBuilder(query);
                for (int cv = 0; cv < nCv; cv++) {    
                    String constraintVariable = constraintVariables.get(cv);
                    int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
                    EDV edv = dataVariables[dv];

                    //sql uses "<>", not "!=";  other sql operators are the same as tableDap
                    String tOp = constraintOps.get(cv);
                    if (tOp.equals("!=")) 
                        tOp = "<>"; 

                    //convert time constraints (epochSeconds) to source units
                    //No need! Use Java's setTimeStamp below.

                    //again, no danger of sql injection since query has been parsed and
                    //  constraintVariables must be known sourceNames
                    //Quotes around colNames avoid trouble when colName is a SQL reserved word.
                    String ts = (cv == 0? " WHERE \"" : " AND \"") +
                        constraintVariables.get(cv) + "\" " + 
                        tOp; 
                    query.append(ts + " ?"); //? is the place holder for a value
                    humanQuery.append(ts + " '" + constraintValues.get(cv) + "'");
                }
                if (orderBySB.length() > 0) {
                    String ts = " ORDER BY " + orderBySB.toString();
                    query.append(ts);
                    humanQuery.append(ts);
                }

                //fill in the '?' in the preparedStatement
                //***!!! This method avoids SQL Injection Vulnerability !!!***
                //(see http://en.wikipedia.org/wiki/SQL_injection) by using
                //preparedStatements (so String values are properly escaped and
                //numbers are assured to be numbers).
                PreparedStatement statement = connection.prepareStatement(query.toString());
                EDV constraintEDVs[] = new EDV[nCv];
                for (int cv = 0; cv < nCv; cv++) {
                    int tv = cv + 1; //+1 since sql uses 1..
                    EDV edv = findDataVariableBySourceName(constraintVariables.get(cv));
                    constraintEDVs[cv] = edv;
                    Class tClass = edv.sourceDataTypeClass();
                    String val = constraintValues.get(cv);
                    //String2.log("cv=" + cv + " tClass=" + PrimitiveArray.elementClassToString(tClass));
                    if (edv instanceof EDVTimeStamp &&
                        !constraintOps.get(cv).equals(PrimitiveArray.REGEX_OP)) statement.setTimestamp(tv, 
                                                      //round to nearest milli
                                                      new Timestamp(Math.round(String2.parseDouble(val)*1000)));
                    else if (edv.isBoolean())         statement.setBoolean(tv, String2.parseBoolean(val)); //special case
                    else if (tClass == String.class)  statement.setString( tv, val);
                    else if (tClass == double.class)  statement.setDouble( tv, String2.parseDouble(val));
                    else if (tClass == float.class)   statement.setFloat(  tv, String2.parseFloat(val));
                    else if (tClass == long.class)    statement.setLong(   tv, String2.parseLong(val));
                    else if (tClass == int.class)     statement.setInt(    tv, String2.parseInt(val)); //???NaN???
                    else if (tClass == short.class)   statement.setShort(  tv, Math2.narrowToShort(String2.parseInt(val))); 
                    else if (tClass == byte.class)    statement.setByte(   tv, Math2.narrowToByte(String2.parseInt(val))); 
                    else if (tClass == char.class)    statement.setString( tv, val.length() == 0? "\u0000" : val.substring(0, 1)); //FFFF??? 
                    else throw new RuntimeException("Prepared statements don't support class type=" + edv.sourceDataType() + ".");            
                }
                if (verbose) String2.log("  statement=" + statement.toString() + "\n" +
                                         " statement~=" + humanQuery.toString());

                //execute the query
                ResultSet rs = statement.executeQuery();

                //make empty table with a column for each resultsVariable
                int tableColToRsCol[]= new int[nRv]; //stored as 1..
                EDV resultsEDVs[] = new EDV[nRv];
                for (int rv = 0; rv < nRv; rv++) {
                    String tName = resultsVariables.get(rv); //a sourceName
                    resultsEDVs[rv] = findDataVariableBySourceName(tName);

                    //find corresponding resultSet column (should be 1:1) and other info
                    tableColToRsCol[rv] = rs.findColumn(tName); //stored as 1..    throws Throwable if not found
                }
                int triggerNRows = EDStatic.partialRequestMaxCells / resultsEDVs.length;
                Table table = makeEmptySourceTable(resultsEDVs, triggerNRows);
                PrimitiveArray paArray[] = new PrimitiveArray[nRv];
                for (int rv = 0; rv < nRv; rv++) 
                    paArray[rv] = table.getColumn(rv);

                //process the resultSet rows of data
                while (rs.next()) {
                    for (int rv = 0; rv < nRv; rv++) {
                        int rsCol = tableColToRsCol[rv];
                        EDV edv = resultsEDVs[rv];
                        Class tClass = edv.sourceDataTypeClass();
                        if (debugMode) String2.log(rv + " " + rs.getString(rsCol));
                        if (edv.isBoolean()) { //special case
                            boolean tb = rs.getBoolean(rsCol);
                            paArray[rv].addInt(rs.wasNull()? Integer.MAX_VALUE : tb? 1 : 0);
                        } else if (edv instanceof EDVTimeStamp) {
                            Timestamp tts = rs.getTimestamp(rsCol);         //zulu millis -> epoch seconds
                            paArray[rv].addDouble(tts == null? Double.NaN : tts.getTime() / 1000.0); 
                        } else if (tClass == String.class) {
                            String ts = rs.getString(rsCol); //it may return null
                            paArray[rv].addString(ts == null? "" : ts); 
                        } else if (tClass == double.class) {
                            double d = rs.getDouble(rsCol);
                            paArray[rv].addDouble(rs.wasNull()? Double.NaN : d); 
                        } else if (tClass == float.class) {
                            float f = rs.getFloat(rsCol);
                            paArray[rv].addFloat(rs.wasNull()? Float.NaN : f); 
                        } else if (tClass == long.class) {
                            long tl = rs.getLong(rsCol);
                            paArray[rv].addLong(rs.wasNull()? Long.MAX_VALUE : tl); 
                        } else {
                            int ti = rs.getInt(rsCol);
                            paArray[rv].addInt(rs.wasNull()? Integer.MAX_VALUE : ti); 
                        }
                    }

                    if (paArray[0].size() >= triggerNRows) {
                        //String2.log(table.toString("rows",5));
                        preStandardizeResultsTable(loggedInAs, table); 
                        if (table.nRows() > 0) {
                            standardizeResultsTable(requestUrl, userDapQuery, table); //changes sourceNames to destinationNames
                            tableWriter.writeSome(table);
                        }

                        table = makeEmptySourceTable(resultsEDVs, triggerNRows);
                        for (int rv = 0; rv < nRv; rv++) 
                            paArray[rv] = table.getColumn(rv);
                        if (tableWriter.noMoreDataPlease) {
                            tableWriter.logCaughtNoMoreDataPlease(datasetID);
                            break;
                        }
                    }
                }
                statement.close();
                preStandardizeResultsTable(loggedInAs, table); 
                standardizeResultsTable(requestUrl, userDapQuery, table);
                tableWriter.writeSome(table);
                tableWriter.finish();
            } catch (Throwable t) {
                connection.close();
                EDStatic.rethrowClientAbortException(t);  //(almost) first thing in catch{}

                //if too much data, rethrow t
                String tToString = t.toString();
                if (tToString.indexOf(Math2.memoryTooMuchData) >= 0)
                    throw t;

                throw new Throwable(EDStatic.errorFromDataSource + t.toString(), t);
            } finally {
                connection.close();
            }
        } catch (Throwable t) {
            //can't make connection!
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            requestReloadASAP();
            throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                "\n(" + EDStatic.errorFromDataSource + t.toString() + ")", 
                t); 
        }
    }

    /**
     * getDataForDapQuery always calls this right before standardizeResultsTable.
     * EDDTableFromPostDatabase uses this to remove data not accessible to this user.
     */
    public void preStandardizeResultsTable(String loggedInAs, Table table) {
        //this base version does nothing
    }
  

    /** 
     * This gets info from/about the database.
     *
     * @param url with the databaseName at end, e.g., jdbc:postgresql://otter.pfeg.noaa.gov/posttest
     * @param driverName the Java class name, e.g., org.postgresql.Driver.
     *    The file containing the class (e.g., postgresql-8.3-603.jdbc3.jar) 
     *    must be in the classpath (usually in [tomcat]/common/lib
     *    or you'll get a runtime error. 
     * @param connectionProperties  see description for class constructor
     * @param catalogName  use "" for no catalog;
     *                     use null or "null" for any catalog; 
     *                     use "!!!LIST!!!" to get a list of catalogNames, then exit.
     * @param schemaName   use "" for no schema;
     *                     use null or "null" for any schema;
     *                     use "!!!LIST!!!" to get a list of schemas, then exit.
     * @param tableName    use a specific table name; 
     *                  or use "!!!LIST!!!" to get a list of tables, then exit.
     * @param sortColumnsByName
     * @throws Throwable if trouble
     */
    public static String getDatabaseInfo(
        String url, String driverName, String connectionProperties[],
        String catalogName, String schemaName, String tableName,
        boolean sortColumnsByName, boolean justPrintTableInfo) 
        throws Throwable {

        StringBuilder sb = new StringBuilder();

        String2.log("EDDTableFromDatabase.getDatabaseInfo" +
            "\n  driver=" + driverName + 
            "\n  catalog=" + catalogName + " schema=" + schemaName + " table=" + tableName);         
        if (catalogName != null && catalogName.equals("null")) catalogName = null;
        if (schemaName  != null && schemaName.equals( "null")) schemaName  = null;

        //Overview of how to get table info: http://www.jguru.com/faq/view.jsp?EID=1184
        
        //get the connection
        Connection con = makeConnection(null, null, //dataSource not available in static situations
            url, driverName, connectionProperties);        

        //*** basically, make a Table which has the dataset's info
        //for databases, there is no metadata, so just get the column names and data types
        Table table = new Table();

        //get databaseMetaData
        DatabaseMetaData dm = con.getMetaData();

        if (catalogName != null && catalogName.equals("!!!LIST!!!")) {
            if (verbose) String2.log("getting catalog list");
            table.readSqlResultSet(dm.getCatalogs());
            con.close();
            return table.saveAsCsvASCIIString();
        }

        if (schemaName != null && schemaName.equals("!!!LIST!!!")) {
            if (verbose) String2.log("getting schema list");
            table.readSqlResultSet(dm.getSchemas());
            con.close();
            return table.saveAsCsvASCIIString();
        }

        if (tableName.equals("!!!LIST!!!")) {
            if (verbose) String2.log("getting tables list");
            table.readSqlResultSet(dm.getTables(catalogName, schemaName, null, null));
            con.close();
            return table.saveAsCsvASCIIString();
        }

        //from here on, we are working with a specific table
        //get the primary keys for the table
        if (verbose) String2.log("getting primaryKey list");
        Table pkTable = new Table();
        pkTable.readSqlResultSet(dm.getPrimaryKeys(catalogName, schemaName, tableName));
        PrimitiveArray pkSA = pkTable.nColumns() >= 4? pkTable.getColumn(3) : new StringArray();  //table columns are 0..

        //get the foreign keys for the table
        if (verbose) String2.log("getting foreignKey list");
        Table fkTable = new Table();
        fkTable.readSqlResultSet(dm.getImportedKeys(catalogName, schemaName, tableName));
        PrimitiveArray fkNames = fkTable.nColumns() >= 8 ? fkTable.getColumn(7) : new StringArray(); //table columns are 0..

        //get all column types for the given catalog/schema/table 
        addDummyRequiredGlobalAttributesForDatasetsXml(table.globalAttributes(), null,
            "database/" + //fake file name
            (catalogName == null? "" : catalogName + "/") +
            (schemaName  == null? "" : schemaName + "/") +
            tableName + "/");
        table.globalAttributes().add("subsetVariables", "???");
        ResultSet rs = dm.getColumns(catalogName, schemaName, tableName, "%");
        //get dbObject metadata
        //javaDoc for dm.getColumns defines the info in each column
        //for (int i = 1; i <= 18; i++) 
        //    String2.log(col + " i=" + i + " " + rs.getString(i));

        //gather/print column information
        int col = 0;
        StringArray booleanList = new StringArray();
        if (verbose || justPrintTableInfo) String2.log("\n" +
            String2.left("Col", 4) + 
            String2.left("Key", 4) + 
            String2.left("Name", 24) + 
            String2.left("java.sql.Types", 15) + 
            String2.left("Java Type", 10) +
            "Remarks");   
        while (rs.next()) {
            //see javadocs for DatabaseMetaData.getColumns for column meanings
            String sqlName = rs.getString(4);
            int    sqlType = rs.getInt(5);
            String remarks = rs.getString(12);
            if (remarks == null) 
                remarks = "";
            String key = pkSA.indexOf(sqlName) >= 0? "P" : "";
            int fkRow = fkNames.indexOf(sqlName);
            if (fkRow >= 0) {
                key += "F";
                remarks = remarks + 
                    (remarks.length() > 0? " " : "") +
                    "[FK from " +
                    fkTable.getStringData(0, fkRow) + "." +
                    fkTable.getStringData(1, fkRow) + "." +
                    fkTable.getStringData(2, fkRow) + "." +
                    fkTable.getStringData(3, fkRow) + "]";
            }
            boolean isTime = sqlType == Types.DATE || sqlType == Types.TIMESTAMP;
            if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) 
                booleanList.add(sqlName.toLowerCase());             

            PrimitiveArray pa = PrimitiveArray.sqlFactory(sqlType);
            Attributes atts = new Attributes();
            if (isTime) {
                atts.add("ioos_category", "???Time");
                atts.add("units", "seconds since 1970-01-01T00:00:00Z");  //no "???"
            }
            addDummyRequiredVariableAttributesForDatasetsXml(atts, sqlName, 
                !(pa instanceof StringArray));
            table.addColumn(col, sqlName.toLowerCase(), pa, atts);
            if (verbose || justPrintTableInfo) String2.log(
                String2.left("" + col, 4) + 
                String2.left(key, 4) +
                String2.left(sqlName.toLowerCase(), 24) + 
                String2.left( 
                  (sqlType == -7? "bit"  : sqlType == 16? "boolean"  : 
                   sqlType == 91? "Date" : 
                   sqlType == 92? "Time" : sqlType == 93? "TimeStamp" : ""+sqlType), 15) + 
                String2.left(pa.elementClassString(), 10) +
                (remarks == null? "" : remarks));    //remarks
            col++;
        }

        //free the database resources
        rs.close();
        con.close();
        if (justPrintTableInfo) 
            return "";

        //sort the column names?
        if (sortColumnsByName)
            table.sortColumnsByName();

        //write the information
        sb.append(directionsForGenerateDatasetsXml());
        sb.append(
            "<!-- Since the source files don't have any metadata, you must add metadata\n" +
            "     below, notably 'units' for each of the dataVariables. -->\n" +
            "<dataset type=\"EDDTableFromDatabase\" datasetID=\"???" + 
                ((catalogName != null && catalogName.length() > 0)? catalogName + "." : "") +
                (( schemaName != null &&  schemaName.length() > 0)?  schemaName + "." : "") +
                tableName + 
                "\" active=\"true\">\n" +
            "    <sourceUrl>" + url + "</sourceUrl>\n" +
            "    <driverName>" + driverName + "</driverName>\n");
        for (int i = 0; i < connectionProperties.length; i += 2) 
            sb.append(
            "    <connectionProperty name=\"" + XML.encodeAsXML(connectionProperties[i]) + "\">" + 
                XML.encodeAsXML(connectionProperties[i+1]) + "</connectionProperty>\n");
        sb.append(
            "    <catalogName>" + catalogName + "</catalogName>\n" +
            "    <schemaName>" + schemaName + "</schemaName>\n" +
            "    <tableName>" + tableName + "</tableName>\n" +
            "    <orderBy>???</orderBy>\n" +
            "    <reloadEveryNMinutes>???" + DEFAULT_RELOAD_EVERY_N_MINUTES + "</reloadEveryNMinutes>\n");
        sb.append(writeAttsForDatasetsXml(true, table.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(null, table, "dataVariable", true, true, true));
        sb.append(
            "</dataset>\n");

        //convert boolean var dataType from byte to boolean
        String search = "<dataType>byte";
        for (int i = 0; i < booleanList.size(); i++) {
            int po = sb.indexOf("<sourceName>" + booleanList.get(i));
            if (po > 0) {
                int po2 = sb.indexOf(search, po);
                if (po2 > 0 && po2 - po < 160) 
                    sb.replace(po2, po2 + search.length(), "<dataType>boolean");
            }
        }
        
        return sb.toString();
    }

    /** 
     * This generates a datasets.xml entry for an EDDTableFromDatabase.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param url with the databaseName at end, e.g., jdbc:postgresql://otter.pfeg.noaa.gov/posttest
     * @param driverName the Java class name, e.g., org.postgresql.Driver.
     *    The file containing the class (e.g., postgresql-8.3-603.jdbc3.jar) 
     *    must be in the classpath (usually in [tomcat]/common/lib
     *    or you'll get a runtime error. 
     * @param connectionProperties  see description for class constructor
     * @param catalogName  use "" for no catalog;
     *                     use null or "null" for any catalog; 
     * @param schemaName   use "" for no schema;
     *                     use null or "null" for any schema;
     * @param tableName    use a specific table name
     * @param tOrderBy     use null or "" for no orderBy
     * @param tReloadEveryNMinutes  e.g., DEFAULT_RELOAD_EVERY_N_MINUTES (10080) for weekly
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @throws Throwable if trouble
     */
    public static String generateDatasetsXml(
        String url, String driverName, String connectionProperties[],
        String catalogName, String schemaName, String tableName, 
        String tOrderBy,
        int tReloadEveryNMinutes,
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes)
        throws Throwable {


        String2.log("EDDTableFromDatabase.generateDatasetsXml" +
            "\n  driver=" + driverName + 
            "\n  catalog=" + catalogName + " schema=" + schemaName + " table=" + tableName);         
        if (catalogName != null && catalogName.equals("null")) catalogName = null;
        if (schemaName  != null && schemaName.equals( "null")) schemaName  = null;

        //Overview of how to get table info: http://www.jguru.com/faq/view.jsp?EID=1184
        
        //get the connection
        Connection con = makeConnection(null, null, //dataSource not available in static situations
            url, driverName, connectionProperties);        

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes.
        //for databases, there is no metadata, so just get the column names and data types
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        //get databaseMetaData
        DatabaseMetaData dm = con.getMetaData();

        //from here on, we are working with a specific table
        //get the primary keys for the table
        if (verbose) String2.log("getting primaryKey list");
        Table pkTable = new Table();
        pkTable.readSqlResultSet(dm.getPrimaryKeys(catalogName, schemaName, tableName));
        PrimitiveArray pkSA = pkTable.nColumns() >= 4? 
            pkTable.getColumn(3) : //table columns are 0..
            new StringArray();  

        //get the foreign keys for the table
        if (verbose) String2.log("getting foreignKey list");
        Table fkTable = new Table();
        fkTable.readSqlResultSet(dm.getImportedKeys(catalogName, schemaName, tableName));
        PrimitiveArray fkNames = fkTable.nColumns() >= 8 ? 
            fkTable.getColumn(7) : //table columns are 0..
            new StringArray();

        //get all column types for the given catalog/schema/table 
        ResultSet rs = dm.getColumns(catalogName, schemaName, tableName, "%");
        //get dbObject metadata
        //javaDoc for dm.getColumns defines the info in each column
        //for (int i = 1; i <= 18; i++) 
        //    String2.log(col + " i=" + i + " " + rs.getString(i));

        //gather/print column information
        int col = 0;
        StringArray booleanList = new StringArray();
        if (verbose) String2.log("\n" +
            String2.left("Col", 4) + 
            String2.left("Key", 4) + 
            String2.left("Name", 24) + 
            String2.left("java.sql.Types", 15) + 
            String2.left("Java Type", 10) +
            "Remarks");   
        while (rs.next()) {
            //see javadocs for DatabaseMetaData.getColumns for column meanings
            String sqlName = rs.getString(4);
            int    sqlType = rs.getInt(5);
            String remarks = rs.getString(12);
            if (remarks == null) 
                remarks = "";
            String key = pkSA.indexOf(sqlName) >= 0? "P" : "";
            int fkRow = fkNames.indexOf(sqlName);
            if (fkRow >= 0) {
                key += "F";
                remarks = remarks + 
                    (remarks.length() > 0? " " : "") +
                    "[FK from " +
                    fkTable.getStringData(0, fkRow) + "." +
                    fkTable.getStringData(1, fkRow) + "." +
                    fkTable.getStringData(2, fkRow) + "." +
                    fkTable.getStringData(3, fkRow) + "]";
            }
            boolean isTime = sqlType == Types.DATE || sqlType == Types.TIMESTAMP;
            if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) 
                booleanList.add(sqlName.toLowerCase());             

            PrimitiveArray pa = PrimitiveArray.sqlFactory(sqlType);
            Attributes sourceAtts = new Attributes();
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                sourceAtts, sqlName, true, true); //addColorBarMinMax, tryToFindLLAT
            if (isTime) {
                addAtts.add("ioos_category", "Time");
                addAtts.add("units", "seconds since 1970-01-01T00:00:00Z");  //no "???"
            }

            dataSourceTable.addColumn(col, sqlName.toLowerCase(), pa, sourceAtts);
            dataAddTable.addColumn(   col, sqlName.toLowerCase(), pa, addAtts);
            if (verbose) String2.log(
                String2.left("" + col, 4) + 
                String2.left(key, 4) +
                String2.left(sqlName.toLowerCase(), 24) + 
                String2.left( 
                  (sqlType == -7? "bit"  : sqlType == 16? "boolean"  : 
                   sqlType == 91? "Date" : 
                   sqlType == 92? "Time" : sqlType == 93? "TimeStamp" : ""+sqlType), 15) + 
                String2.left(pa.elementClassString(), 10) +
                (remarks == null? "" : remarks));    //remarks
            col++;
        }

        //free the database resources
        rs.close();
        con.close();


        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local database)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataAddTable)? "Point" : "Other",
                "database/" + //fake file dir
                    (catalogName == null? "" : catalogName + "/") +
                    (schemaName  == null? "" : schemaName + "/") +
                    tableName + "/", 
                externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        
        //sort the column names?
        //if (sortColumnsByName)
        //    dataAddTable.sortColumnsByName();

        //write the information
        StringBuilder sb = new StringBuilder();
        sb.append(
            directionsForGenerateDatasetsXml() +
            " *** Since the database doesn't have any metadata, you must add metadata\n" +
            "   below, notably 'units' for each of the dataVariables.\n" +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromDatabase\" datasetID=\"" + 
                ((catalogName != null && catalogName.length() > 0)? catalogName + "_" : "") +
                (( schemaName != null &&  schemaName.length() > 0)?  schemaName + "_" : "") +
                tableName + 
                "\" active=\"true\">\n" +
            "    <sourceUrl>" + url + "</sourceUrl>\n" +
            "    <driverName>" + driverName + "</driverName>\n");
        for (int i = 0; i < connectionProperties.length; i += 2) 
            sb.append(
                "    <connectionProperty name=\"" + XML.encodeAsXML(connectionProperties[i]) + "\">" + 
                    XML.encodeAsXML(connectionProperties[i+1]) + "</connectionProperty>\n");
        sb.append(
            "    <catalogName>" + catalogName + "</catalogName>\n" +
            "    <schemaName>" + schemaName + "</schemaName>\n" +
            "    <tableName>" + tableName + "</tableName>\n" +
            (tOrderBy == null || tOrderBy.length() == 0? "" : 
            "    <orderBy>" + tOrderBy + "</orderBy>\n") +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        //convert boolean var dataType from byte to boolean
        String search = "<dataType>byte";
        for (int i = 0; i < booleanList.size(); i++) {
            int po = sb.indexOf("<sourceName>" + booleanList.get(i));
            if (po > 0) {
                int po2 = sb.indexOf(search, po);
                if (po2 > 0 && po2 - po < 160) 
                    sb.replace(po2, po2 + search.length(), "<dataType>boolean");
            }
        }
        
        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /** This is used by Bob to test POST. */
    public static String postUrl = "jdbc:postgresql://142.22.58.155:5432/postparse"; 
//    public static String postUrl = "jdbc:postgresql://data.postprogram.org:5432/postparse";  
    public static String postDriver = "org.postgresql.Driver";
    public static String[] postProperties() throws Throwable {
        String password = String2.getPasswordFromSystemIn("Password for 'erduser'? ");
//String password = 
        return new String[]{
            "user",        "erduser", 
            "password",    password,
            "ssl",         "true",
            "sslfactory",  "org.postgresql.ssl.NonValidatingFactory"};
    }


    /**
     * This is used by Bob to print info about POST tables.
     *
     * @param catalogName use null for any (that's all POST needs)
     * @param schemaName  use a specific schemaName, or null for any
     * @param tableName   use a specific tableName, or "!!!LIST!!!" for a list of tables in the schema.
     * @throws Throwable if trouble
     */
    public static String getPostTableInfo(String catalogName, String schemaName, 
            String tableName, boolean sortColumns, boolean justPrintTableInfo) throws Throwable {
        String2.log("\n*** printPostTableInfo");
        verbose = true;

        return getDatabaseInfo(postUrl, postDriver, postProperties(),
            catalogName, schemaName, tableName, 
            sortColumns, justPrintTableInfo);
    }


    /**
     * This tests generateDatasetsXml.
     * 
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        String2.log("\n*** EDDTableFromDatabase.testGenerateDatasetsXml");
        testVerboseOn();
        String name, tName, gdiResults, results, tResults, expected, userDapQuery, tQuery;

        try {
            String tCatalogName = "";
            String tSchemaName = "erd";
            String tTableName = "detection";
            //gdiResults = getDatabaseInfo(
            //    postUrl, postDriver, 
            //    postProperties(),
            //    tCatalogName, tSchemaName, tTableName,
            //    false, false);

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDTableFromDatabase",                
                postUrl, //s1
                postDriver, //s2
                String2.toSVString(postProperties(), "|", false), //s3
                tCatalogName, tSchemaName, tTableName,  //s4,5,6
                "", //s7 orderBy csv
                "" + DEFAULT_RELOAD_EVERY_N_MINUTES, //s8 reloadEveryNMinutes  default is 10080
                "", //s9  infoUrl
                "", //s10 institution
                "", //s11 summary
                ""}, //s12 title
                false); //doIt loop?
            results = String2.getClipboardString();
            //Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");


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
"   altitude, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n" +
" *** Since the database doesn't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromDatabase\" datasetID=\"erd_detection\" active=\"true\">\n" +
"    <sourceUrl>jdbc:postgresql://142.22.58.155:5432/postparse</sourceUrl>\n" +
"    <driverName>org.postgresql.Driver</driverName>\n" +
"    <connectionProperty name=\"user\">erduser</connectionProperty>\n" +
"    <connectionProperty name=\"password\">";
          Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);

expected = 
"name=\"ssl\">true</connectionProperty>\n" +
"    <connectionProperty name=\"sslfactory\">org.postgresql.ssl.NonValidatingFactory</connectionProperty>\n" +
"    <catalogName></catalogName>\n" +
"    <schemaName>erd</schemaName>\n" +
"    <tableName>detection</tableName>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">code, common, detection, erd, identifier, name, project, surgery, tag, time, timestamp</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"sourceUrl\">(local database)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">Data from a local source.</att>\n" +
"        <att name=\"title\">Erd detection</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>common_name</sourceName>\n" +
"        <destinationName>common_name</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Common Name</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>detection_timestamp</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Detection Timestamp</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>tag_id_code</sourceName>\n" +
"        <destinationName>tag_id_code</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Tag Id Code</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>tag_sn</sourceName>\n" +
"        <destinationName>tag_sn</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Tag Sn</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
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
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
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
"        <sourceName>surgery_id</sourceName>\n" +
"        <destinationName>surgery_id</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Surgery Id</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>pi</sourceName>\n" +
"        <destinationName>pi</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Pi</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>project</sourceName>\n" +
"        <destinationName>project</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Project</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
            int po = results.indexOf("name=\"ssl\"");
            Test.ensureEqual(results.substring(po), expected, "results=" + results);


            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "erd_detection", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "common_name, time, tag_id_code, tag_sn, latitude, longitude, surgery_id, pi, project", 
                "");


        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromDatabase.testGenerateDatasetsXml error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }

    }


    /**
     * This tests the post erd detection dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testErdDetection() throws Throwable {
        String2.log("\n*** testErdDetection");
        testVerboseOn();
        long eTime;
        String tQuery;
        try {
            EDDTableFromDatabase tedd = (EDDTableFromDatabase)oneFromDatasetXml("postDet"); 
            String tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_Data", ".das"); 
            String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
            String expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  common_name {\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Common Name\";\n" +
"  }\n" +
"  pi {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Principal Investigator\";\n" +
"  }\n" +
"  project {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Project\";\n" +
"  }\n" +
"  surgery_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Surgery ID\";\n" +
"  }\n" +
"  tag_id_code {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Tag ID Code\";\n" +
"  }\n" +
"  tag_sn {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Tag Serial Number\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today + " (source database)\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/postDet.das\";\n" +
"    String infoUrl \"http://www.postcoml.org/\";\n" +
"    String institution \"POST\";\n" +
"    String keywords \"Biosphere > Aquatic Ecosystems > Coastal Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Pelagic Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Estuarine Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Rivers/Stream Habitat\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String sourceUrl \"(source database)\";\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"The dataset has tag detection records from the Pacific Ocean\n" +
"Shelf Tracking project (POST).  POST is a research tool for\n" +
"tracking the movement and estimated survival of marine animals along\n" +
"the West Coast of North America, using acoustic transmitters implanted\n" +
"in animals and a series of receivers running in lines across the\n" +
"continental shelf.  It is one of fourteen field projects of the\n" +
"Census of Marine Life assessing the distribution, diversity and\n" +
"abundance of marine organisms internationally.  (V1)\";\n" +
"    String title \"POST Tag Detections\";\n" +
"  }\n" +
"}\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
  
            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    String common_name;\n" +
"    String pi;\n" +
"    String project;\n" +
"    Int32 surgery_id;\n" +
"    String tag_id_code;\n" +
"    String tag_sn;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
/* */

            //testErdDetection1Var();        
            //SELECT DISTINCT common_name FROM erd.detection  95.7s
            //SELECT DISTINCT pi FROM erd.detection //2009-05-28: 286s     (16ms in EDDTableReplicate) 
            eTime = System.currentTimeMillis();
            tQuery = "common_name&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_Data_common", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   //time strings converted from e.g., 2004-04-16T05:16:42-07:00"  to hour=12 Z
"common_name\n" +
"\n" +
"BLACK ROCKFISH\n" +
"CHINOOK\n" +
"COHO\n" +
"CUTTHROAT\n" +
"DOLLY VARDEN\n" +
"\"SOCKEYE, KOKANEE\"\n" +
"SQUID\n" +
"STEELHEAD\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            String2.log("*** testErdDetection1Var common_name FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //.csv  one variable, distinct
            tQuery = "pi&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_peb_Data_pi", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());           
            Test.ensureTrue(results.startsWith("pi\n"), "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf("\nBARRY BEREJIKIAN\n") > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf("\nSCOTT STELTZNER\n") > 0, "\nresults=\n" + results);
            String2.log("*** testErdDetection1Var pi FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            
            //testErdDetection2Vars();       //2009-05-22: 192s    (15ms in EDDTableReplicate)
            //                               //2012-03-20  1159.3s
            //SELECT DISTINCT pi, common_name FROM erd.detection
            eTime = System.currentTimeMillis();
            tQuery = "pi,common_name&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_Data_pc", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = //this will change
"pi,common_name\n" +
",\n" +
"BARRY BEREJIKIAN,STEELHEAD\n" +
"CEDAR CHITTENDEN,COHO\n" +
"CHRIS WOOD,\"SOCKEYE, KOKANEE\"\n" +
"CHUCK BOGGS,COHO\n" +
"DAVID WELCH,CHINOOK\n" +
"DAVID WELCH,COHO\n" +
"DAVID WELCH,DOLLY VARDEN\n" +
"DAVID WELCH,\"SOCKEYE, KOKANEE\"\n" +
"DAVID WELCH,STEELHEAD\n" +
"FRED GOETZ,CHINOOK\n" +
"FRED GOETZ,CUTTHROAT\n" +
"JACK TIPPING,STEELHEAD\n" +
"JEFF MARLIAVE,BLACK ROCKFISH\n" +
"JOHN PAYNE,SQUID\n" +
"LYSE GODBOUT,\"SOCKEYE, KOKANEE\"\n" +
"MIKE MELNYCHUK,COHO\n" +
"MIKE MELNYCHUK,\"SOCKEYE, KOKANEE\"\n" +
"MIKE MELNYCHUK,STEELHEAD\n" +
"ROBERT BISON,STEELHEAD\n" +
"SCOTT STELTZNER,CHINOOK\n" +
"SCOTT STELTZNER,COHO\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            String2.log("*** testErdDetection2Vars FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //testErdDetection1Pi();         //2009-05-22: 152s     (~62ms in EDDTableReplicate)
            //                               //2012-03-20 499.06s
            //SELECT DISTINCT pi, common_name, surgery_id FROM erd.detection WHERE pi = DAVID WELCH AND common_name = COHO
            eTime = System.currentTimeMillis();
            tQuery = "pi,common_name,surgery_id&pi=\"DAVID WELCH\"&common_name=\"COHO\"&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_Data_tags", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log(results);
            expected =   //time strings converted from e.g., 2004-04-16T05:16:42-07:00"  to hour=12 Z
"pi,common_name,surgery_id\n" +
",,\n" +
"DAVID WELCH,COHO,1\n" +
"DAVID WELCH,COHO,2\n" +
"DAVID WELCH,COHO,3\n" +
"DAVID WELCH,COHO,4\n" +
"DAVID WELCH,COHO,5\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testErdDetection1Pi FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 



            //testErdDetection1Tag();        //2009-05-22: 5.7s     (15ms (with diagnostic msgs off) in EDDTableReplicate)
            //.csv   detections for one tag  //2012-03-20 49.8s
            eTime = System.currentTimeMillis();
            tQuery = "&surgery_id=4540";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_Data_detect", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   //time strings converted from e.g., 2004-04-16T05:16:42-07:00"  to hour=12 Z
//Note that all data is in triplicate! How bizarre!
"longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
"degrees_east,degrees_north,UTC,,,,,,\n" +
"-127.00323,50.55336,2004-06-19T17:15:16Z,COHO,DAVID WELCH,KINTAMA RESEARCH,4540,1784,1034575\n" +
"-127.00323,50.55336,2004-06-19T17:15:16Z,COHO,DAVID WELCH,KINTAMA RESEARCH,4540,1784,1034575\n" +
"-127.00323,50.55336,2004-06-19T17:15:16Z,COHO,DAVID WELCH,KINTAMA RESEARCH,4540,1784,1034575\n" +
"-127.00323,50.55336,2004-06-19T17:18:21Z,COHO,DAVID WELCH,KINTAMA RESEARCH,4540,1784,1034575\n" +
"-127.00323,50.55336,2004-06-19T17:18:21Z,COHO,DAVID WELCH,KINTAMA RESEARCH,4540,1784,1034575\n" +
"-127.00323,50.55336,2004-06-19T17:18:21Z,COHO,DAVID WELCH,KINTAMA RESEARCH,4540,1784,1034575\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testErdDetection1Tag FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //change internal orderBy, get results in a different order 
            //This is just for test purposes -- since I can't change datasets.xml for these tests.
            // this changed so needs work
            //String oldOrderBy[] = tedd.orderBy;
            //tedd.orderBy = new String[]{"common_name", "surgery_id", "detection_timestamp"};
            //tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
            //    tedd.className() + "_peb_Data", ".csv"); 
            //results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //expected = 
//"zztop\n";
            //Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            //tedd.orderBy = oldOrderBy; //return to original orderBy


            //testErdDetectionConstraints //2009-05-22: 8.5s
            //                            //2012-03-20  50.3s  and 76.2s
            eTime = System.currentTimeMillis();
            tQuery = "&pi=\"DAVID WELCH\"&common_name=\"CHINOOK\"&latitude>50" +
                "&surgery_id>=1201&surgery_id<1202&time>=2007-05-01T08&time<2007-05-01T09";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_constrained", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =  
//Note that all data is in triplicate! How bizarre!
"longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
"degrees_east,degrees_north,UTC,,,,,,\n" +
"-127.48843,50.78142,2007-05-01T08:43:33Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:43:33Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:43:33Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:48:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:48:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.48843,50.78142,2007-05-01T08:48:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testErdDetectionConstraints FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //2012-03-21   61.4s and 70.5s
            eTime = System.currentTimeMillis();
            tQuery = "&surgery_id=1201&time<2007-04-28"; //implied 00:00:00+0000
//I think query sent to database is correct  (when use setTimestamp):
//SELECT common_name, detection_timestamp, tag_id_code, tag_sn, latitude, longitude, surgery_id, pi, project 
//FROM erd.detection WHERE surgery_id = 1201 AND detection_timestamp < 2007-04-27 17:00:00.000000 -07:00 
//ORDER BY pi, project, surgery_id, detection_timestamp
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_peb_time", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//Problem was    (BUT NOW SOLVED BY USING setTimestamp)
//some of raw data for 1201 from Jose
// CHINOOK     | 2007-04-27 16:51:24-07 | 1054        | 1030254 |  50.7986 |  -127.4745 |       1201 | DAVID WELCH | KINTAMA RESEARCH
// CHINOOK     | 2007-04-27 16:53:48-07 | 1054        | 1030254 |  50.7986 |  -127.4745 |       1201 | DAVID WELCH | KINTAMA RESEARCH
//response should stop here   since this corresponds to 2007-04-28 00:0:00+0000
// CHINOOK     | 2007-04-27 17:01:40-07 | 1054        | 1030254 |  50.7986 |  -127.4745 |       1201 | DAVID WELCH | KINTAMA RESEARCH
//...
// CHINOOK     | 2007-04-27 18:09:17-07 | 1054        | 1030254 |  50.8036 | -127.46988 |       1201 | DAVID WELCH | KINTAMA RESEARCH
// CHINOOK     | 2007-04-27 18:13:09-07 | 1054        | 1030254 |  50.8118 | -127.46292 |       1201 | DAVID WELCH | KINTAMA RESEARCH
// CHINOOK     | 2007-04-27 18:37:36-07 | 1054        | 1030254 |  50.8036 | -127.46988 |       1201 | DAVID WELCH | KINTAMA RESEARCH
// CHINOOK     | 2007-04-27 20:24:05-07 | 1054        | 1030254 |  50.8036 | -127.46988 |       1201 | DAVID WELCH | KINTAMA RESEARCH
//response does stop here  which is 2007-04-28 00:00:00 in local time (-7)
// CHINOOK     | 2007-04-28 00:02:37-07 | 1054        | 1030254 |  50.7986 |  -127.4745 |       1201 | DAVID WELCH | KINTAMA RESEARCH
// CHINOOK     | 2007-04-28 00:11:33-07 | 1054        | 1030254 |  50.7986 |  -127.4745 |       1201 | DAVID WELCH | KINTAMA RESEARCH
// CHINOOK     | 2007-04-28 00:17:49-07 | 1054        | 1030254 |  50.7986 |  -127.4745 |       1201 | DAVID WELCH | KINTAMA RESEARCH
//but erddap gives last return value of
//CHINOOK, 2007-04-28T03:24:05Z, 1054, 1030254, 50.8036, -127.46988, 1201, DAVID WELCH, KINTAMA RESEARCH
//postgres 8.5.3 http://www.postgresql.org/docs/8.0/static/datatype-datetime.html
//All timezone-aware dates and times are stored internally in UTC. They are converted to local time in the zone specified by the timezone 
//configuration parameter before being displayed to the client.
            expected =  
//Note that all data is in triplicate! How bizarre!
"longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n" +
"degrees_east,degrees_north,UTC,,,,,,\n" +
"-127.3507,50.68383,2004-05-23T17:27:04Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.3507,50.68383,2004-05-23T17:27:04Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.3507,50.68383,2004-05-23T17:27:04Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.3507,50.68383,2004-05-23T17:49:10Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.3507,50.68383,2004-05-23T17:49:10Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.3507,50.68383,2004-05-23T17:49:10Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n" +
"-127.3507,50.68383,2004-05-23T17:50:41Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n";
                Test.ensureEqual(results.substring(0, expected.length()), 
                expected, "\nresults=\n" + results);
            String2.log("*** testTime FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 
            
            /* */

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromDatabase.testTime error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * This tests the post erd detection dataset.
     *
     * @throws Throwable if trouble
     */
     /*
    public static void testErdDetectionBoolean() throws Throwable {
        String2.log("\n*** testErdDetectionBoolean");
        testVerboseOn();
        try {
            EDDTableFromDatabase tedd = (EDDTableFromDatabase)oneFromDatasetXml("postDet"); 
            //.csv   with boolean=true test
            try {
                String tQuery = "detection_id,longitude,latitude,time,receiver_name&disabled=1";
                String tName = tedd.makeNewFileForDapQuery(null, null, tQuery, EDStatic.fullTestCacheDirectory, 
                    tedd.className() + "_peb_booleanT", ".csv"); 
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception("'No Data' exception was expected.", t); 
            }

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected EDDTableFromDatabase.testErdDetectionBoolean error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }

//???need tests of ==null or !=null

    }
    */

    /*
     * This returns the contents of a dataset in csv form.
     *
     * @throws Throwable if trouble
     */
    public static String getCSV(String datasetID) throws Throwable {
        String dir = EDStatic.fullTestCacheDirectory;
        EDDTableFromDatabase tedd = (EDDTableFromDatabase)oneFromDatasetXml(datasetID); 
        String tName = tedd.makeNewFileForDapQuery(null, null, "", 
            dir, tedd.className() + "_" + datasetID + "_getCSV", ".csv"); 
        return new String((new ByteArray(dir + tName)).toArray());
    }




    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromDatabase.test() *****************\n");

        //tests usually run
        testGenerateDatasetsXml();
        testErdDetection();

        //*** no longer active tests
        //testErdDetectionBoolean();

        //other code used sometimes
        //String2.log(getPostTableInfo(null, null, "!!!LIST!!!", true));
        //String2.log(getPostTableInfo("", "erd", "detection", false));


        //out-of-date
        //String2.log(getPostTableInfo("", "detection", "detection", false));
        //String2.log(getPostTableInfo("", "surgery", "surgery", false));
    }


}

