/*
 * EDDTableFromDatabase Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.ULongArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.Script2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromDatabaseHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * This class represents a table of data from a database. This class avoids the SQL Injection
 * security problem (see https://en.wikipedia.org/wiki/SQL_injection).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-08
 */
@SaxHandlerClass(EDDTableFromDatabaseHandler.class)
public class EDDTableFromDatabase extends EDDTable {

  /** set by the constructor */
  protected String dataSourceName;

  protected DataSource dataSource; // null if none available
  protected String driverName;
  protected String connectionProperties[]; // may have username and password!
  protected String catalogName;
  protected String schemaName;
  protected String tableName;
  protected String columnNameQuotes = "\""; // may also be ' or empty string
  protected String orderBy[];
  protected HashMap<String, HashSet<String>> scriptNeedsColumns =
      new HashMap(); // <sourceName, otherSourceColumnNames>

  protected String catalogSeparator;

  public static String testUser = "postgres";
  public static String testUrl = "jdbc:postgresql://localhost:5432/mydatabase";
  public static String testDriver = "org.postgresql.Driver";

  /**
   * This constructs an EDDTableFromDatabase based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDatabase"&gt;
   *     having just been read.
   * @return an EDDTableFromDatabase. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromDatabase fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {
    return lowFromXml(erddap, xmlReader, "");
  }

  /**
   * This constructs an EDDTableFromDatabase based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDatabase"&gt;
   *     having just been read.
   * @param subclass "" for regular or "Post"
   * @return an EDDTableFromDatabase or one of its subclasses. When this returns, xmlReader will
   *     have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  public static EDDTableFromDatabase lowFromXml(
      Erddap erddap, SimpleXMLReader xmlReader, String subclass) throws Throwable {

    // data to be obtained (or not)
    if (verbose)
      String2.log("\n*** constructing EDDTableFrom" + subclass + "Database(xmlReader)...");
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
    String tDataSourceName = null;
    String tLocalSourceUrl = null;
    String tDriverName = null;
    String tCatalogName = "";
    String tSchemaName = "";
    String tTableName = null;
    String tColumnNameQuotes = "\""; // to be consistent with previous versions
    String tOrderBy[] = new String[0];
    StringArray tConnectionProperties = new StringArray();
    boolean tSourceNeedsExpandedFP_EQ = true;
    String tSourceCanOrderBy = "no";
    String tSourceCanDoDistinct = "no";
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;

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
      else if (localTags.equals("<altitudeMetersPerSourceUnit>"))
        throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
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
      else if (localTags.equals("<dataSourceName>")) {
      } else if (localTags.equals("</dataSourceName>")) tDataSourceName = content;
      else if (localTags.equals("<driverName>")) {
      } else if (localTags.equals("</driverName>")) tDriverName = content;
      else if (localTags.equals("<connectionProperty>"))
        tConnectionProperties.add(xmlReader.attributeValue("name"));
      else if (localTags.equals("</connectionProperty>")) tConnectionProperties.add(content);
      else if (localTags.equals("<catalogName>")) {
      } else if (localTags.equals("</catalogName>")) tCatalogName = content;
      else if (localTags.equals("<schemaName>")) {
      } else if (localTags.equals("</schemaName>")) tSchemaName = content;
      else if (localTags.equals("<tableName>")) {
      } else if (localTags.equals("</tableName>")) tTableName = content;
      else if (localTags.equals("<columnNameQuotes>")) {
      } else if (localTags.equals("</columnNameQuotes>")) tColumnNameQuotes = content;
      else if (localTags.equals("<orderBy>")) {
      } else if (localTags.equals("</orderBy>")) {
        if (content != null && content.length() > 0) tOrderBy = String2.split(content, ',');
      } else if (localTags.equals("<sourceNeedsExpandedFP_EQ>")) {
      } else if (localTags.equals("</sourceNeedsExpandedFP_EQ>"))
        tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
      else if (localTags.equals("<sourceCanOrderBy>")) {
      } else if (localTags.equals("</sourceCanOrderBy>")) tSourceCanOrderBy = content;
      else if (localTags.equals("<sourceCanDoDistinct>")) {
      } else if (localTags.equals("</sourceCanDoDistinct>")) tSourceCanDoDistinct = content;
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

    /*if (subclass.equals("Post"))
        return new EDDTableFromPostDatabase(tDatasetID,
            tAccessibleTo, tGraphsAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tGlobalAttributes,
            ttDataVariables,
            tReloadEveryNMinutes,
            tDataSourceName,
            tLocalSourceUrl, tDriverName,
            tConnectionProperties.toArray(),
            tCatalogName, tSchemaName, tTableName, tColumnNameQuotes, tOrderBy,
            tSourceNeedsExpandedFP_EQ, tSourceCanOrderBy, tSourceCanDoDistinct);
    else*/ return new EDDTableFromDatabase(
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
        tDataSourceName,
        tLocalSourceUrl,
        tDriverName,
        tConnectionProperties.toArray(),
        tCatalogName,
        tSchemaName,
        tTableName,
        tColumnNameQuotes,
        tOrderBy,
        tSourceNeedsExpandedFP_EQ,
        tSourceCanOrderBy,
        tSourceCanDoDistinct);
  }

  /**
   * The constructor.
   *
   * <p>Yes, lots of detailed information must be supplied here that is sometimes available in
   * metadata. If it is in metadata, make a subclass that extracts info from metadata and calls this
   * constructor.
   *
   * <p>Security Features - When working with databases, you need to do things as safely and
   * securely as possible to avoid allowing a malicious user to damage your database or gain access
   * to data they shouldn't have access to. ERDDAP tries to do things in a secure way, too.
   *
   * <ul>
   *   <li>We encourage you to set up ERDDAP to connect to the database as a database user that only
   *       has access to the *relevant* database(s) and only has READ privileges.
   *   <li>We encourage you to set up the connection from ERDDAP to the database so that it always
   *       uses SSL, only allows connections from one IP address and from the one ERDDAP user, and
   *       only transfers passwords in their MD5 hashed form.
   *   <li>[BAD]The database password is stored as plain text in datasets.xml (only the
   *       administrator should have READ access to this file). We haven't found a way to allow the
   *       administrator to enter the database password during ERDDAP's startup in Tomcat (which
   *       occurs without user input), so the password must be accessible in a file.
   *   <li>When in ERDDAP, the password is kept "private".
   *   <li>Requests from clients are parsed and checked for validity before generating requests for
   *       the database.
   *   <li>Requests to the database are made with PreparedStatements, to avoid SQL injection.
   *   <li>Requests to the database are submitted with executeQuery (not executeStatement) to limit
   *       requests to be read-only (so attempted SQL injection to alter the database will fail for
   *       this reason, too).
   * </ul>
   *
   * <p>It can be difficult for ERDDAP to successfully make the connection to the database. In
   * addition to properly specifying the &lt;dataset&gt; info in datasets.xml:
   *
   * <ul>
   *   <li>You need to put the appropriate database driver .jar file (for example,
   *       postgresql-8.3-603.jdbc3.jar) in [tomcat]/common/lib .
   *   <li>You need to configure the database to have an ERDDAP user (you choose the userName) who
   *       can only READ the database, not make any changes.
   *   <li>You need to configure the database to allow a connection from the ERDDAP user. Be as
   *       restrictive as you can. If this is a remote connection, it should always use SSL.
   *       Passwords should never be transmitted as plaintext (use MD5 or another means instead).
   *       <br>
   *       For example, for Postgres, you need to make a pg_hba.conf file (see
   *       http://developer.postgresql.org/pgdocs/postgres/auth-pg-hba-conf.html) with a line in the
   *       form of <br>
   *       "<tt>hostssl database user CIDR-address auth-method [auth-option]</tt>" <br>
   *       for example, <br>
   *       "<tt>hostssl myDatabase myUserId 65.219.21.0/24 md5</tt>"
   * </ul>
   *
   * @param tDatasetID is a very short string identifier (recommended: [A-Za-z][A-Za-z0-9_]* ) for
   *     this dataset. See EDD.datasetID().
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: ) to be done
   *     whenever the dataset changes significantly
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tAddGlobalAttributes are global attributes which define the data source's global
   *     attributes (since there are no source attributes). This may be null if you have nothing to
   *     add. The addGlobalAttributes must include:
   *     <ul>
   *       <li>"title" - the short (&lt; 80 characters) description of the dataset
   *       <li>"summary" - the longer description of the dataset. It may have newline characters
   *           (usually at &lt;= 72 chars per line).
   *       <li>"institution" - the source of the data (best if &lt; 50 characters so it fits in a
   *           graph's legend).
   *       <li>"infoUrl" - the url with information about this data set
   *       <li>"cdm_data_type" - one of the EDD.CDM_xxx options
   *     </ul>
   *     Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
   *     Special case: if combinedGlobalAttributes name="license", any instance of
   *     value="[standard]" will be converted to the EDStatic.standardLicense.
   * @param tDataVariables is an Object[nDataVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
   *     [3]=String the source dataType (e.g., "boolean", "int", "float", "String", ...). This is
   *     needed because databases types are not universally consistent in what how they assign JDBC
   *     data types to the database's internal data types (see
   *     http://www.onlamp.com/pub/a/onlamp/2001/09/13/aboutSQL.html?page=last). Even if they were,
   *     there are some difficult types, e.g., unsigned integer types. So you need to explicitly
   *     define the dataTypes. <br>
   *     For Date, Time, and Timestamp database types, use "double". <br>
   *     The order of variables you define doesn't have to match the order in the source.
   *     <p>If there is a time variable, either tAddAttributes (read first) or tSourceAttributes
   *     must have "units" which is either
   *     <ul>
   *       <li>a UDUunits string (containing " since ") describing how to interpret source time
   *           values (which should always be numeric since they are a dimension of a grid) (e.g.,
   *           "seconds since 1970-01-01T00:00:00").
   *       <li>a java.time.format.DateTimeFormatter string (which is compatible with
   *           java.text.SimpleDateFormat) describing how to interpret string times (e.g., the
   *           ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   *           or
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   *     </ul>
   *
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tDataSourceName the name of the dataSource (for connection pooling) defined in
   *     [tomcat]/conf/server.xml (or null or "" of not used)
   * @param tLocalSourceUrl the url needed to connect to the database (it includes the database name
   *     at the end), e.g., jdbc:postgresql://otter.pfeg.noaa.gov/posttest .
   * @param tDriverName the Java class name, e.g., org.postgresql.Driver. The file containing the
   *     class (e.g., postgresql-8.3-603.jdbc3.jar) must be in the classpath (usually in
   *     [tomcat]/common/lib or you'll get a runtime error.
   * @param tConnectionProperties is an alternating list of names and values used to make a
   *     connection to the database. <br>
   *     It may be null or length=0. <br>
   *     For example name={"user", "erdUser", "password", "thePassword", "ssl", "true",
   *     "sslfactory", "org.postgresql.ssl.NonValidatingFactory"}.
   *     <p>SSL is an important security issue. <br>
   *     Remember that userNames and password MD5's will be transmitted between ERDDAP and the
   *     database. <br>
   *     So, if the database is local, you may chose not to use SSL for the connection. But for all
   *     remote databases, it is STRONGLY RECOMMENDED that you use SSL. <br>
   *     See https://jdbc.postgresql.org/documentation/81/ssl-client.html .
   * @param tCatalogName use "" if not needed
   * @param tSchemaName use "" if not needed
   * @param tTableName
   * @param tColumnNameQuotes should be ", ', or empty string. This will be put before and after
   *     variable names in SQL statements.
   * @param tOrderBy is an array of sourceNames (use String[0] if not needed) which are used to
   *     construct an ORDER BY clause for a query. Only sourceNames which are relevant to a given
   *     query are used in the ORDER BY clause. The leftmost sourceName is most important;
   *     subsequent sourceNames are only used to break ties.
   * @param tSourceNeedsExpandedFP_EQ
   * @throws Throwable if trouble
   */
  public EDDTableFromDatabase(
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
      String tDataSourceName,
      String tLocalSourceUrl,
      String tDriverName,
      String tConnectionProperties[],
      String tCatalogName,
      String tSchemaName,
      String tTableName,
      String tColumnNameQuotes,
      String tOrderBy[],
      boolean tSourceNeedsExpandedFP_EQ,
      String tSourceCanOrderBy,
      String tSourceCanDoDistinct)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromDatabase " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromDatabase(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDTableFromDatabase";
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
    if (String2.isSomething(tDataSourceName)) {
      tLocalSourceUrl = "(using dataSource)";
      tDriverName = "(using dataSource)";
    }
    Test.ensureNotNothing(tLocalSourceUrl, "'sourceUrl' wasn't defined.");
    Test.ensureNotNothing(tDriverName, "'driverName' wasn't defined.");
    // catalog and schema may be ""
    Test.ensureNotNothing(tTableName, "'tableName' wasn't defined.");
    dataSourceName = tDataSourceName;
    if (tConnectionProperties == null) tConnectionProperties = new String[0];
    Test.ensureTrue(
        !Math2.odd(tConnectionProperties.length),
        "connectionProperties.length must be an even number.");
    publicSourceUrl = "(source database)"; // not tLocalSourceUrl; keep it private
    addGlobalAttributes.set("sourceUrl", publicSourceUrl);
    localSourceUrl = tLocalSourceUrl;
    driverName = tDriverName;
    connectionProperties = tConnectionProperties;
    catalogName = tCatalogName;
    schemaName = tSchemaName;
    tableName = tTableName;
    columnNameQuotes = tColumnNameQuotes;
    Test.ensureTrue(
        "\"".equals(columnNameQuotes)
            || "'".equals(columnNameQuotes)
            || "".equals(columnNameQuotes),
        "<columnNameQuotes> must be \", ', or an empty string.");
    orderBy = tOrderBy == null ? new String[0] : tOrderBy;
    // database will never be able to deal with all ERDDAP-supported orderBy and distinct() options,
    // so convert Yes to Partial
    sourceCanOrderBy =
        Math2.minMax(
            0,
            1, // so default=no
            getNoPartialYes(tSourceCanOrderBy));
    sourceCanDoDistinct =
        Math2.minMax(
            0,
            1, // so default=no
            getNoPartialYes(tSourceCanDoDistinct));

    // try to get the dataSource
    if (dataSourceName != null && dataSourceName.length() > 0) {
      try {
        String2.log("\nTrying to find dataSourceName=" + dataSourceName + "...");
        Context context = new InitialContext();
        dataSource = (DataSource) context.lookup(dataSourceName); // may be null
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }
      String2.log(
          "dataSourceName="
              + dataSourceName
              + (dataSource == null
                  ? " wasn't found, so connection pooling won't be used.\n"
                      + "  (Isn't this code running in an application server like Tomcat?\n"
                      + "  Did you define the resource in, e.g., [tomcat]/conf/context.xml ?)\n"
                  : " was successfully found, so connection pooling will be used.\n"));
    } else {
      String2.log("\ndataSourceName wasn't specified, so connection pooling won't be used.\n");
    }

    // sql can support everything except regex constraints
    sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
    sourceCanConstrainNumericData =
        CONSTRAIN_PARTIAL; // database won't see sourceName=script constraints, so ERDDAP applies
    // them, too
    sourceCanConstrainStringData =
        CONSTRAIN_PARTIAL; // database won't see sourceName=script constraints, so ERDDAP applies
    // them, too
    sourceCanConstrainStringRegex = "";

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
    for (int dv = 0; dv < ndv; dv++) {
      String tSourceName = (String) tDataVariables[dv][0];
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      String tSourceType = (String) tDataVariables[dv][3];
      Attributes tSourceAtt = new Attributes();
      // if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType="
      // + tSourceType);

      if (tSourceName.startsWith("="))
        scriptNeedsColumns.put(
            tSourceName,
            Script2.jexlScriptNeedsColumns(tSourceName)); // needsColumns.size() may be 0

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

    // get the connection
    // This is also an important test of ability to make a connection.
    // Failure causes dataset to fail to load!
    Connection connection =
        makeConnection(
            dataSourceName, dataSource, localSourceUrl, driverName, connectionProperties);
    try {
      DatabaseMetaData meta = connection.getMetaData();
      catalogSeparator = meta.getCatalogSeparator();

      // finally
      if (verbose) {
        String2.log(
            // don't display connectionProperties because of password
            "\ndatabase name="
                + meta.getDatabaseProductName()
                + " version="
                + meta.getDatabaseProductVersion()
                + "\ndriver name="
                + meta.getDriverName()
                + " version="
                + meta.getDriverVersion()
                + "\njdbc majorVersion="
                + meta.getJDBCMajorVersion()
                + " minorVersion="
                + meta.getJDBCMinorVersion());
      }
    } finally { // not catch{}, so trouble causes dataset to fail to load!
      try {
        connection.close();
      } catch (Throwable t) {
        String2.log(
            "Caught ERROR while closing database connection:\n" + MustBe.throwableToString(t));
      }
    }

    // Don't gather ERDDAP sos information.
    // I am guessing that requesting the min/maxTime for each station is
    //  *very* taxing for most databases.

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromDatabase "
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
  } // because this gets info from a database

  /**
   * This makes a new database Connection. If dataSource != null, it will be used to get the
   * connection; else via DriverManager. See the connectionProperties documentation for the class
   * constructor.
   */
  public static Connection makeConnection(
      String dataSourceName,
      DataSource dataSource,
      String url,
      String driverName,
      String connectionProperties[])
      throws Throwable {

    long tTime = System.currentTimeMillis();

    if (dataSource == null) {
      // get a connection via DriverManager
      if (verbose)
        String2.log("EDDTableFromDatabase.makeConnection via DriverManager + datasets.xml info");
      Class.forName(driverName); // to load the jdbc driver

      // see example (with SSL info) at https://jdbc.postgresql.org/documentation/80/connect.html
      Properties props = new Properties();
      for (int i = 0; i < connectionProperties.length; i += 2)
        props.setProperty(connectionProperties[i], connectionProperties[i + 1]);
      Connection con = DriverManager.getConnection(url, props);
      if (verbose)
        String2.log(
            "  Success! time=" + (System.currentTimeMillis() - tTime) + "ms"); // often slow!
      return con;
    } else {
      // get a connection from the connection pool via DataSource
      if (verbose)
        String2.log(
            "EDDTableFromDatabase.makeConnection from pool via DataSource + [tomcat]/conf/server.xml info");
      Connection con = dataSource.getConnection();
      if (verbose)
        String2.log(
            "  Success! time="
                + (System.currentTimeMillis() - tTime)
                + "ms"); // should be very fast
      return con;
    }
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * <p>The method avoids SQL Injection Vulnerability (see
   * https://en.wikipedia.org/wiki/SQL_injection) by using preparedStatements (so String values are
   * properly escaped and numbers are assured to be numbers).
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

    // good summary of using statements, queries, resultSets, ...
    //  https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html

    // get the sourceDapQuery (a query that the source can handle)
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    getSourceQueryFromDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues); // timeStamp constraints other than regex are epochSeconds
    // String2.log(">>resultsVars=" + resultsVariables.toString());

    // pull script variables out of resultsVariables
    HashSet<String> sourceNamesSet = new HashSet();
    BitSet keep = new BitSet();
    StringArray scriptNames = null;
    StringArray scriptTypes = null;
    HashSet<String> needOtherSourceNames = null;
    for (int v = 0; v < resultsVariables.size(); v++) {
      String sourceName = resultsVariables.get(v);
      if (sourceName.startsWith("=")) {
        // content comes from a script
        EDV edv =
            findDataVariableBySourceName(sourceName); // throws exception, but should always succeed
        if (scriptNames == null) {
          scriptNames = new StringArray();
          scriptTypes = new StringArray();
          needOtherSourceNames = new HashSet();
        }
        scriptNames.add(sourceName);
        scriptTypes.add(edv.sourceDataType());

        // later: ensure columns referenced in script are in sourceNamesSet
        needOtherSourceNames.addAll(scriptNeedsColumns.get(sourceName));
      } else {
        sourceNamesSet.add(sourceName);
        keep.set(v);
      }
    }

    // distinct? orderBy...?
    //  Database handles FIRST distinct or orderBY
    //    IF sourceCanDoDistict/OrderBy = PARTIAL or YES
    // making database do distinct seems useful (maybe it can optimize, data transfer greatly
    // reduced
    // but orderBy may be slow/hard for database (faster to do it in erddap?)
    boolean distinct = false;
    StringArray queryOrderBy = null; // the query orderBy or distinct source variable names
    int nDistinctOrOrderBy = 0;
    String[] parts = Table.getDapQueryParts(userDapQuery); // decoded.
    for (int pi = 0; pi < parts.length; pi++) {
      String p = parts[pi];
      // String2.log(">>p#" + pi + "=" + p);
      if (p.equals("distinct()")) {
        nDistinctOrOrderBy++;
        if (nDistinctOrOrderBy == 1 && sourceCanDoDistinct >= CONSTRAIN_PARTIAL) {
          distinct = true;
          // To databases, DISTINCT doesn't imply a sort order.
          // To ERDDAP,    DISTINCT does imply a sort order.
          // So if database is going to handle DISTINCT, also tell it to sort the results.
          // https://stackoverflow.com/questions/691562/does-select-distinct-imply-a-sort-of-the-results

          queryOrderBy = (StringArray) resultsVariables.clone();
          // String2.log(">>distinct() -> queryOrderBy=" + queryOrderBy.toString());
        }

      } else if (scriptNames == null
          && // only ask database to handle orderBy() if no scriptNames in original resultsVariables
          p.startsWith("orderBy")
          && // doesn't matter if orderByMax|Min|MinMax|...
          p.endsWith("\")")) {
        if (nDistinctOrOrderBy == 0 && sourceCanOrderBy >= CONSTRAIN_PARTIAL) {
          int tpo = p.indexOf("(\"");
          if (tpo < 0)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.queryErrorAr[0] + "Invalid syntax for \"" + p + "\".",
                    EDStatic.queryErrorAr[language]
                        + "Invalid syntax for \""
                        + p
                        + "\".")); // should have been caught already
          StringArray tQueryOrderBy = StringArray.fromCSV(p.substring(tpo + 2, p.length() - 2));
          // change from destNames to sourceNames
          boolean foundScript = false;
          for (int oi = 0; oi < tQueryOrderBy.size(); oi++) {
            int v = String2.indexOf(dataVariableDestinationNames(), tQueryOrderBy.get(oi));
            if (v < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.queryErrorAr[0]
                          + MessageFormat.format(
                              EDStatic.queryErrorUnknownVariableAr[0], tQueryOrderBy.get(oi)),
                      EDStatic.queryErrorAr[language]
                          + MessageFormat.format(
                              EDStatic.queryErrorUnknownVariableAr[language],
                              tQueryOrderBy.get(oi))));
            String tSourceName = dataVariableSourceNames()[v];
            tQueryOrderBy.set(oi, tSourceName);
            HashSet<String> tNeedsColumns = scriptNeedsColumns.get(tSourceName);
            if (tNeedsColumns != null
                && tNeedsColumns.size() > 0) { // actually refers to another variable
              foundScript = true;
              break;
            }
          }
          if (!foundScript) {
            // no =script variables, so we can pass this orderBy to the database
            queryOrderBy = tQueryOrderBy;
            nDistinctOrOrderBy++;
          }
        }
      }
    }

    if (scriptNames != null) {
      // this works and can be done here because getSourceQueryFromDapQuery
      // made sure all constraint variables and orderBy variables
      // were included in resultsVariables.
      resultsVariables.justKeep(keep);

      // then ensure needOtherSourceNames are in resultsVariables
      for (String sourceName : needOtherSourceNames) {
        if (sourceNamesSet.add(sourceName)) // if not already present
        resultsVariables.add(sourceName);
      }
    }

    // no need to further prune constraints

    // make the connection
    Connection connection = null;

    // ??? Use a ConnectionPool???  See JDBC API Tutorial book, pg 640; or search web.
    // For now, make a new connection each time???  I think that is excessive, but simple.
    //  see connection.close() below.

    // See javadocs for isClosed -- it isn't very useful. So do quick test:
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
    */

    // make a new connection
    try {
      connection =
          makeConnection(
              dataSourceName, dataSource, localSourceUrl, driverName, connectionProperties);
    } catch (Throwable t) {

      EDStatic.rethrowClientAbortException(t); // first thing in catch{}

      // unable to make connection
      // wait 5 seconds (slow things down if system is overwhelmed), then try again
      String msg =
          "ERROR from EDDTableFromDatabase(" + datasetID + ").getDataForDapQuery makeConnection #";
      String2.log(msg + "1=\n" + MustBe.throwableToString(t));
      Math2.sleep(5000);
      try {
        connection =
            makeConnection(
                dataSourceName, dataSource, localSourceUrl, driverName, connectionProperties);
      } catch (Throwable t2) {

        EDStatic.rethrowClientAbortException(t2); // first thing in catch{}

        // give up
        String2.log(msg + "2=\n" + MustBe.throwableToString(t2));
        throw new WaitThenTryAgainException(
            EDStatic.bilingual(
                language,
                EDStatic.waitThenTryAgainAr[0]
                    + "("
                    + EDStatic.databaseUnableToConnectAr[0]
                    + ": "
                    + t.toString()
                    + ")",
                EDStatic.waitThenTryAgainAr[language]
                    + "("
                    + EDStatic.databaseUnableToConnectAr[language]
                    + ": "
                    + t.toString()
                    + ")"));
      }
    }

    // try/catch to ensure connection and statement are closed at the end
    PreparedStatement statement = null;
    try {

      // build the sql query
      StringBuilder query = new StringBuilder();
      int nRv = resultsVariables.size();
      String distinctString = distinct ? "DISTINCT " : "";
      for (int rv = 0; rv < nRv; rv++)
        // no danger of sql injection since query has been parsed and
        //  resultsVariables must be known sourceNames
        // Note that I tried to use '?' for resultsVariables, but never got it to work: wierd
        // results.
        // Quotes around colNames avoid trouble when colName is a SQL reserved word.
        query.append(
            (rv == 0 ? "SELECT " + distinctString : ", ")
                + columnNameQuotes
                + resultsVariables.get(rv)
                + columnNameQuotes);
      // Lack of quotes around table names means they can't be SQL reserved words.
      // (If do quote in future, quote individual parts.)
      query.append(
          " FROM "
              + (catalogName.equals("") ? "" : catalogName + catalogSeparator)
              + (schemaName.equals("") ? "" : schemaName + ".")
              + tableName);

      // create orderBySB
      StringBuilder orderBySB = new StringBuilder();
      if (queryOrderBy != null) {
        // append queryOrderBy variables
        for (int ob = 0; ob < queryOrderBy.size(); ob++) {
          if (resultsVariables.indexOf(queryOrderBy.get(ob)) >= 0) { // should be
            if (orderBySB.length() > 0) orderBySB.append(", ");
            // Quotes around colNames avoid trouble when colName is a SQL reserved word.
            orderBySB.append(columnNameQuotes + queryOrderBy.get(ob) + columnNameQuotes);
          }
        }
      } else {
        // append predefined orderBy variables
        for (int ob = 0; ob < orderBy.length; ob++) {
          if (resultsVariables.indexOf(orderBy[ob]) >= 0) {
            if (orderBySB.length() > 0) orderBySB.append(", ");
            // Quotes around colNames avoid trouble when colName is a SQL reserved word.
            orderBySB.append(columnNameQuotes + orderBy[ob] + columnNameQuotes);
          }
        }
      }
      // String2.log(">>orderBySB=" + orderBySB.toString());

      // add constraints to query
      int nCv = constraintVariables.size();
      StringBuilder humanQuery = new StringBuilder(query);
      int nActiveCV = 0;
      for (int cv = 0; cv < nCv; cv++) {
        String constraintVariable = constraintVariables.get(cv);
        // if it's a fixedValue or script variable, don't ask database to constrain it
        if (constraintVariable.startsWith("=")) continue;
        nActiveCV++;
        int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
        EDV edv = dataVariables[dv];

        // sql uses "<>", not "!=";  other sql operators are the same as tableDap
        String tOp = constraintOps.get(cv);
        if (tOp.equals("!=")) tOp = "<>";

        // convert time constraints (epochSeconds) to source units
        // No need! Use Java's setTimeStamp below.

        // again, no danger of sql injection since query has been parsed and
        //  constraintVariables must be known sourceNames
        // Quotes around colNames avoid trouble when colName is a SQL reserved word.
        String ts =
            (nActiveCV == 1 ? " WHERE " : " AND ")
                + columnNameQuotes
                + constraintVariables.get(cv)
                + columnNameQuotes
                + " "
                + tOp;
        query.append(ts + " ?"); // ? is the place holder for a value
        humanQuery.append(ts + " '" + constraintValues.get(cv) + "'");
      }
      if (orderBySB.length() > 0) {
        String ts = " ORDER BY " + orderBySB.toString();
        query.append(ts);
        humanQuery.append(ts);
      }

      // fill in the '?' in the preparedStatement
      // ***!!! This method avoids SQL Injection Vulnerability !!!***
      // (see https://en.wikipedia.org/wiki/SQL_injection) by using
      // preparedStatements (so String values are properly escaped and
      // numbers are assured to be numbers).
      statement = connection.prepareStatement(query.toString());
      EDV constraintEDVs[] = new EDV[nCv];
      nActiveCV = 0;
      for (int cv = 0; cv < nCv; cv++) {
        if (constraintVariables.get(cv).startsWith("=")) continue;
        nActiveCV++; // +1 since sql uses 1..
        EDV edv = findDataVariableBySourceName(constraintVariables.get(cv));
        constraintEDVs[cv] = edv;
        PAType tPAType = edv.sourceDataPAType();
        String val = constraintValues.get(cv);
        // String2.log("cv=" + cv + " tPAType=" + tPAType);
        // ???Do I need to parse numeric val's to check for NaN (which is not allowed(?))
        if (edv instanceof EDVTimeStamp && !constraintOps.get(cv).equals(PrimitiveArray.REGEX_OP))
          statement.setTimestamp(
              nActiveCV,
              // round to nearest milli
              new Timestamp(Math.round(String2.parseDouble(val) * 1000)));
        else if (edv.isBoolean())
          statement.setBoolean(nActiveCV, String2.parseBoolean(val)); // special case
        else if (tPAType == PAType.STRING) statement.setString(nActiveCV, val);
        // for Unsigned types, go to next bigger data type.
        else if (tPAType == PAType.DOUBLE) statement.setDouble(nActiveCV, String2.parseDouble(val));
        else if (tPAType == PAType.FLOAT) statement.setFloat(nActiveCV, String2.parseFloat(val));
        else if (tPAType == PAType.LONG) statement.setLong(nActiveCV, String2.parseLong(val));
        else if (tPAType == PAType.ULONG)
          statement.setBigDecimal(nActiveCV, String2.parseBigDecimalObject(val));
        else if (tPAType == PAType.INT) statement.setInt(nActiveCV, String2.parseInt(val));
        else if (tPAType == PAType.UINT) statement.setLong(nActiveCV, String2.parseLong(val));
        else if (tPAType == PAType.SHORT)
          statement.setShort(nActiveCV, Math2.narrowToShort(String2.parseInt(val)));
        else if (tPAType == PAType.USHORT) statement.setInt(nActiveCV, String2.parseInt(val));
        else if (tPAType == PAType.BYTE)
          statement.setByte(nActiveCV, Math2.narrowToByte(String2.parseInt(val)));
        else if (tPAType == PAType.UBYTE)
          statement.setShort(nActiveCV, Math2.narrowToShort(String2.parseInt(val))); // ???
        else if (tPAType == PAType.CHAR)
          statement.setString(
              nActiveCV, val.length() == 0 ? "\u0000" : val.substring(0, 1)); // FFFF???
        else
          throw new RuntimeException(
              "Prepared statements don't support class type=" + edv.sourceDataType() + ".");
      }
      if (verbose)
        String2.log(
            "  statement=" + statement.toString() + "\n" + " statement~=" + humanQuery.toString());

      // execute the query
      ResultSet rs = statement.executeQuery();

      // make empty table with a column for each resultsVariable
      int tableColToRsCol[] = new int[nRv]; // stored as 1..
      EDV resultsEDVs[] = new EDV[nRv];
      for (int rv = 0; rv < nRv; rv++) {
        String tName = resultsVariables.get(rv); // a sourceName
        resultsEDVs[rv] = findDataVariableBySourceName(tName);

        // find corresponding resultSet column (should be 1:1) and other info
        tableColToRsCol[rv] =
            rs.findColumn(tName); // stored as 1..    throws Throwable if not found
      }
      int triggerNRows = EDStatic.partialRequestMaxCells / resultsEDVs.length;
      Table table = makeEmptySourceTable(resultsEDVs, triggerNRows);
      PrimitiveArray paArray[] = new PrimitiveArray[nRv];
      for (int rv = 0; rv < nRv; rv++) paArray[rv] = table.getColumn(rv);

      // process the resultSet rows of data
      while (true) {
        boolean hasNext = rs.next();

        if (hasNext) {
          for (int rv = 0; rv < nRv; rv++) {
            int rsCol = tableColToRsCol[rv];
            EDV edv = resultsEDVs[rv];
            PAType tPAType = edv.sourceDataPAType();
            if (debugMode) String2.log(rv + " " + rs.getString(rsCol));
            if (edv.isBoolean()) { // special case
              boolean tb = rs.getBoolean(rsCol);
              paArray[rv].addInt(rs.wasNull() ? Integer.MAX_VALUE : tb ? 1 : 0);
            } else if (edv instanceof EDVTimeStamp) {
              Timestamp tts = rs.getTimestamp(rsCol); // zulu millis -> epoch seconds
              paArray[rv].addDouble(tts == null ? Double.NaN : tts.getTime() / 1000.0);
            } else if (tPAType == PAType.STRING) {
              String ts = rs.getString(rsCol); // it may return null
              paArray[rv].addString(ts == null ? "" : ts);
            } else if (tPAType == PAType.DOUBLE) {
              double d = rs.getDouble(rsCol);
              paArray[rv].addDouble(rs.wasNull() ? Double.NaN : d);
            } else if (tPAType == PAType.FLOAT) {
              float f = rs.getFloat(rsCol);
              paArray[rv].addFloat(rs.wasNull() ? Float.NaN : f);
            } else if (tPAType == PAType.ULONG) {
              BigDecimal bd = rs.getBigDecimal(rsCol);
              ((ULongArray) paArray[rv]).add(rs.wasNull() ? null : Math2.roundToULongOrNull(bd));
            } else if (tPAType == PAType.LONG || tPAType == PAType.UINT) {
              long tl = rs.getLong(rsCol);
              if (rs.wasNull()) {
                paArray[rv].setMaxIsMV(true);
                tl = Long.MAX_VALUE;
              }
              paArray[rv].addLong(tl);
            } else {
              int ti = rs.getInt(rsCol);
              if (rs.wasNull()) {
                paArray[rv].setMaxIsMV(true);
                ti = Integer.MAX_VALUE;
              }
              paArray[rv].addInt(ti);
            }
          }
        }

        if ((paArray[0].size() > 0 && !hasNext) || paArray[0].size() >= triggerNRows) {
          if (Thread.currentThread().isInterrupted())
            throw new SimpleException(
                "EDDTableFromDatabase.getDataForDapQuery" + EDStatic.caughtInterruptedAr[0]);

          // convert script columns into data columns
          if (scriptNames != null)
            convertScriptColumnsToDataColumns(
                "", table, scriptNames, scriptTypes, scriptNeedsColumns);

          // String2.log(table.toString("rows",5));
          preStandardizeResultsTable(loggedInAs, table);
          if (table.nRows() > 0) {
            standardizeResultsTable(
                language,
                requestUrl,
                userDapQuery,
                table); // changes sourceNames to destinationNames
            tableWriter.writeSome(table); // okay if 0 rows
          }

          if (hasNext) {
            table = makeEmptySourceTable(resultsEDVs, triggerNRows);
            for (int rv = 0; rv < nRv; rv++) paArray[rv] = table.getColumn(rv);
          }
          if (tableWriter.noMoreDataPlease) {
            tableWriter.logCaughtNoMoreDataPlease(datasetID);
            break;
          }
        }
        if (!hasNext) break;
      }
      tableWriter.finish();
      statement.close();

      // last thing
      connection.close();

    } catch (Throwable t) {
      try {
        connection.close();
      } catch (Exception e) {
      }
      try {
        if (statement != null) statement.close();
      } catch (Exception e) {
      }

      EDStatic.rethrowClientAbortException(t); // first thing in catch{}

      String msg = MustBe.throwableToString(t);
      // String2.log("EDDTableFromDatabase caught:\n" + msg);

      if (msg.indexOf(MustBe.THERE_IS_NO_DATA) >= 0
          || msg.indexOf(EDStatic.caughtInterruptedAr[0]) >= 0) {
        throw t;
      } else {
        // all other errors probably from database
        throw new Throwable(EDStatic.errorFromDataSource + t.toString(), t);
      }
    }
  }

  /**
   * getDataForDapQuery always calls this right before standardizeResultsTable.
   * EDDTableFromPostDatabase uses this to remove data not accessible to this user.
   */
  public void preStandardizeResultsTable(String loggedInAs, Table table) {
    // this base version does nothing
  }

  /**
   * This gets info from/about the database.
   *
   * @param url with the databaseName at end, e.g., jdbc:postgresql://otter.pfeg.noaa.gov/posttest
   * @param driverName the Java class name, e.g., org.postgresql.Driver. The file containing the
   *     class (e.g., postgresql-8.3-603.jdbc3.jar) must be in the classpath (usually in
   *     [tomcat]/common/lib or you'll get a runtime error.
   * @param connectionProperties see description for class constructor
   * @param catalogName use "" for no catalog; use null or "null" for any catalog; use "!!!LIST!!!"
   *     to get a list of catalogNames, then exit.
   * @param schemaName use "" for no schema; use null or "null" for any schema; use "!!!LIST!!!" to
   *     get a list of schemas, then exit.
   * @param tableName use a specific table name; or use "!!!LIST!!!" to get a list of tables, then
   *     exit.
   * @param sortColumnsByName
   * @throws Throwable if trouble
   */
  public static String getDatabaseInfo(
      String url,
      String driverName,
      String connectionProperties[],
      String catalogName,
      String schemaName,
      String tableName)
      throws Throwable {

    StringBuilder sb = new StringBuilder();

    String2.log(
        "EDDTableFromDatabase.getDatabaseInfo"
            + "\n  driver="
            + driverName
            + "\n  catalog="
            + catalogName
            + " schema="
            + schemaName
            + " table="
            + tableName);
    if (catalogName != null && catalogName.equals("null")) catalogName = null;
    if (schemaName != null && schemaName.equals("null")) schemaName = null;

    // Overview of how to get table info: http://www.jguru.com/faq/view.jsp?EID=1184

    // get the connection
    Connection con =
        makeConnection(
            null,
            null, // dataSource not available in static situations
            url,
            driverName,
            connectionProperties);
    ResultSet rs = null;
    try {

      // *** basically, make a Table which has the dataset's info
      // for databases, there is no metadata, so just get the column names and data types
      Table table = new Table();

      // get databaseMetaData
      DatabaseMetaData dm = con.getMetaData();

      if (catalogName != null && catalogName.equals("!!!LIST!!!")) {
        if (verbose) String2.log("getting catalog list");
        table.readSqlResultSet(dm.getCatalogs());
        return table.saveAsCsvASCIIString();
      }

      if (schemaName != null && schemaName.equals("!!!LIST!!!")) {
        if (verbose) String2.log("getting schema list");
        table.readSqlResultSet(dm.getSchemas());
        return table.saveAsCsvASCIIString();
      }

      if (tableName.equals("!!!LIST!!!")) {
        if (verbose) String2.log("getting tables list");
        table.readSqlResultSet(dm.getTables(catalogName, schemaName, null, null));
        return table.saveAsCsvASCIIString();
      }

      // from here on, we are working with a specific table
      // get the primary keys for the table
      if (verbose) String2.log("getting primaryKey list");
      Table pkTable = new Table();
      pkTable.readSqlResultSet(dm.getPrimaryKeys(catalogName, schemaName, tableName));
      PrimitiveArray pkSA =
          pkTable.nColumns() >= 4
              ? pkTable.getColumn(3)
              : new StringArray(); // table columns are 0..

      // get the foreign keys for the table
      if (verbose) String2.log("getting foreignKey list");
      Table fkTable = new Table();
      fkTable.readSqlResultSet(dm.getImportedKeys(catalogName, schemaName, tableName));
      PrimitiveArray fkNames =
          fkTable.nColumns() >= 8
              ? fkTable.getColumn(7)
              : new StringArray(); // table columns are 0..

      // get all column types for the given catalog/schema/table
      addDummyRequiredGlobalAttributesForDatasetsXml(
          table.globalAttributes(),
          null,
          "database/"
              + // fake file name
              (catalogName == null ? "" : catalogName + "/")
              + (schemaName == null ? "" : schemaName + "/")
              + tableName
              + "/");
      rs = dm.getColumns(catalogName, schemaName, tableName, "%");
      // get dbObject metadata
      // javaDoc for dm.getColumns defines the info in each column
      // for (int i = 1; i <= 18; i++)
      //    String2.log(col + " i=" + i + " " + rs.getString(i));

      // gather/print column information
      int col = 0;
      StringArray booleanList = new StringArray(); // sourceNames
      sb.append(
          String2.left("Col", 4)
              + String2.left("Key", 4)
              + String2.left("Name", 24)
              + String2.left("java.sql.Types", 15)
              + String2.left("Java Type", 10)
              + "Remarks\n");
      while (rs.next()) {
        // see javadocs for DatabaseMetaData.getColumns for column meanings
        String sqlName = rs.getString(4);
        int sqlType = rs.getInt(5);
        String remarks = rs.getString(12);
        if (remarks == null) remarks = "";
        String key = pkSA.indexOf(sqlName) >= 0 ? "P" : "";
        int fkRow = fkNames.indexOf(sqlName);
        if (fkRow >= 0) {
          key += "F";
          remarks =
              remarks
                  + (remarks.length() > 0 ? " " : "")
                  + "[FK from "
                  + fkTable.getStringData(0, fkRow)
                  + "."
                  + fkTable.getStringData(1, fkRow)
                  + "."
                  + fkTable.getStringData(2, fkRow)
                  + "."
                  + fkTable.getStringData(3, fkRow)
                  + "]";
        }
        boolean isTime = sqlType == Types.DATE || sqlType == Types.TIMESTAMP;
        if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) booleanList.add(sqlName);

        PrimitiveArray pa = PrimitiveArray.sqlFactory(sqlType);
        sb.append(
            String2.left("" + col, 4)
                + String2.left(key, 4)
                + String2.left(sqlName, 24)
                + String2.left(
                    (sqlType == -7
                        ? "bit"
                        : sqlType == 16
                            ? "boolean"
                            : sqlType == 91
                                ? "Date"
                                : sqlType == 92
                                    ? "Time"
                                    : sqlType == 93 ? "TimeStamp" : "" + sqlType),
                    15)
                + String2.left(pa.elementTypeString(), 10)
                + (remarks == null ? "" : remarks)
                + "\n"); // remarks
        col++;
      }
    } finally {
      // free the database resources
      try {
        if (rs != null) rs.close();
      } catch (Exception e) {
      }
      try {
        if (con != null) con.close();
      } catch (Exception e) {
      }
    }

    return sb.toString();
  }

  /**
   * This generates a datasets.xml entry for an EDDTableFromDatabase. The XML can then be edited by
   * hand and added to the datasets.xml file.
   *
   * @param url with the databaseName at end, e.g., jdbc:postgresql://otter.pfeg.noaa.gov/posttest
   * @param driverName the Java class name, e.g., org.postgresql.Driver. The file containing the
   *     class (e.g., postgresql-8.3-603.jdbc3.jar) must be in the classpath (usually in
   *     [tomcat]/common/lib or you'll get a runtime error.
   * @param connectionProperties see description for class constructor
   * @param catalogName use "" for no catalog; use null or "null" for any catalog;
   * @param schemaName use "" for no schema; use null or "null" for any schema;
   * @param tableName use a specific table name
   * @param tOrderBy use null or "" for no orderBy
   * @param tReloadEveryNMinutes e.g., DEFAULT_RELOAD_EVERY_N_MINUTES (10080) for weekly
   * @param tInfoUrl or "" if in externalAddGlobalAttributes or if not available
   * @param tInstitution or "" if in externalAddGlobalAttributes or if not available
   * @param tSummary or "" if in externalAddGlobalAttributes or if not available
   * @param tTitle or "" if in externalAddGlobalAttributes or if not available
   * @param externalAddGlobalAttributes These attributes are given priority. Use null in none
   *     available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String url,
      String driverName,
      String connectionProperties[],
      String catalogName,
      String schemaName,
      String tableName,
      String tOrderBy,
      int tReloadEveryNMinutes,
      String tInfoUrl,
      String tInstitution,
      String tSummary,
      String tTitle,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDTableFromDatabase.generateDatasetsXml"
            + "\nurl="
            + url
            + "\ndriver="
            + driverName
            + "\nconnectionProperties="
            + String2.toCSVString(connectionProperties)
            + "\ncatalog="
            + catalogName
            + " schema="
            + schemaName
            + " table="
            + tableName
            + " orderBy="
            + tOrderBy
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
    if (catalogName != null && catalogName.equals("null")) catalogName = null;
    if (schemaName != null && schemaName.equals("null")) schemaName = null;

    // Overview of how to get table info: http://www.jguru.com/faq/view.jsp?EID=1184
    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes.
    // for databases, there is no metadata, so just get the column names and data types
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();
    StringArray booleanList = new StringArray();

    // get the connection
    Connection con =
        makeConnection(
            null,
            null, // dataSource not available in static situations
            url,
            driverName,
            connectionProperties);
    ResultSet rs = null;
    try {

      // get databaseMetaData
      DatabaseMetaData dm = con.getMetaData();

      // from here on, we are working with a specific table
      // get the primary keys for the table
      if (verbose) String2.log("getting primaryKey list");
      Table pkTable = new Table();
      pkTable.readSqlResultSet(dm.getPrimaryKeys(catalogName, schemaName, tableName));
      PrimitiveArray pkSA =
          pkTable.nColumns() >= 4
              ? pkTable.getColumn(3)
              : // table columns are 0..
              new StringArray();

      // get the foreign keys for the table
      if (verbose) String2.log("getting foreignKey list");
      Table fkTable = new Table();
      fkTable.readSqlResultSet(dm.getImportedKeys(catalogName, schemaName, tableName));
      PrimitiveArray fkNames =
          fkTable.nColumns() >= 8
              ? fkTable.getColumn(7)
              : // table columns are 0..
              new StringArray();

      // get all column types for the given catalog/schema/table
      rs = dm.getColumns(catalogName, schemaName, tableName, "%");
      // get dbObject metadata
      // javaDoc for dm.getColumns defines the info in each column
      // for (int i = 1; i <= 18; i++)
      //    String2.log(col + " i=" + i + " " + rs.getString(i));

      // gather/print column information
      int col = 0;
      if (verbose)
        String2.log(
            "\n"
                + String2.left("Col", 4)
                + String2.left("Key", 4)
                + String2.left("Name", 24)
                + String2.left("java.sql.Types", 15)
                + String2.left("Java Type", 10)
                + "Remarks");
      while (rs.next()) {
        // see javadocs for DatabaseMetaData.getColumns for column meanings
        String sqlName = rs.getString(4);
        int sqlType = rs.getInt(5);
        String remarks = rs.getString(12);
        if (remarks == null) remarks = "";
        String key = pkSA.indexOf(sqlName) >= 0 ? "P" : "";
        int fkRow = fkNames.indexOf(sqlName);
        if (fkRow >= 0) {
          key += "F";
          remarks =
              remarks
                  + (remarks.length() > 0 ? " " : "")
                  + "[FK from "
                  + fkTable.getStringData(0, fkRow)
                  + "."
                  + fkTable.getStringData(1, fkRow)
                  + "."
                  + fkTable.getStringData(2, fkRow)
                  + "."
                  + fkTable.getStringData(3, fkRow)
                  + "]";
        }
        boolean isTime = sqlType == Types.DATE || sqlType == Types.TIMESTAMP;
        if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) booleanList.add(sqlName);

        Attributes sourceAtts = new Attributes();
        Attributes addAtts = new Attributes();
        if (isTime) {
          addAtts.add("ioos_category", "Time");
          addAtts.add("units", "seconds since 1970-01-01T00:00:00Z"); // no "???"
        }
        PrimitiveArray sourcePA = PrimitiveArray.sqlFactory(sqlType);
        PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
        addAtts =
            makeReadyToUseAddVariableAttributesForDatasetsXml(
                null, // no source global attributes
                sourceAtts,
                addAtts,
                sqlName, // sourceAtts, addAtts, sourceName
                destPA.elementType() != PAType.STRING, // tryToAddStandardName
                destPA.elementType() != PAType.STRING, // addColorBarMinMax
                true); // tryToFindLLAT

        dataSourceTable.addColumn(col, sqlName, sourcePA, sourceAtts);
        dataAddTable.addColumn(col, sqlName.toLowerCase(), destPA, addAtts);
        if (verbose)
          String2.log(
              String2.left("" + col, 4)
                  + String2.left(key, 4)
                  + String2.left(sqlName, 24)
                  + String2.left(
                      (sqlType == -7
                          ? "bit"
                          : sqlType == 16
                              ? "boolean"
                              : sqlType == 91
                                  ? "Date"
                                  : sqlType == 92
                                      ? "Time"
                                      : sqlType == 93 ? "TimeStamp" : "" + sqlType),
                      15)
                  + String2.left(sourcePA.elementTypeString(), 10)
                  + (remarks == null ? "" : remarks)); // remarks

        // add missing_value and/or _FillValue if needed
        // But for database, no data available
        // addMvFvAttsIfNeeded(sqlName.toLowerCase(), sourcePA, sourceAtts, addAtts); //sourcePA
        // since strongly typed

        col++;
      }
    } finally {
      // free the database resources
      try {
        if (rs != null) rs.close();
      } catch (Exception e) {
      }
      try {
        if (con != null) con.close();
      } catch (Exception e) {
      }
    }

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    if (tInfoUrl != null && tInfoUrl.length() > 0)
      externalAddGlobalAttributes.add("infoUrl", tInfoUrl);
    if (tInstitution != null && tInstitution.length() > 0)
      externalAddGlobalAttributes.add("institution", tInstitution);
    if (tSummary != null && tSummary.length() > 0)
      externalAddGlobalAttributes.add("summary", tSummary);
    if (tTitle != null && tTitle.length() > 0) externalAddGlobalAttributes.add("title", tTitle);
    externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local database)");

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable); // just axisTables

    // externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
    // after dataVariables known, add global attributes in the dataAddTable
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                // another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable) ? "Point" : "Other",
                "database/"
                    + // fake file dir
                    (catalogName == null ? "" : catalogName + "/")
                    + (schemaName == null ? "" : schemaName + "/")
                    + tableName
                    + "/",
                externalAddGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));

    // don't suggestSubsetVariables, since sourceTable not available

    // sort the column names?
    // if (sortColumnsByName)
    //    dataAddTable.sortColumnsByName();

    // write the information
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<!-- NOTE! Since database tables don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromDatabase\" datasetID=\""
            + XML.encodeAsXML(
                ((catalogName != null && catalogName.length() > 0) ? catalogName + "_" : "")
                    + ((schemaName != null && schemaName.length() > 0) ? schemaName + "_" : "")
                    + tableName)
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + XML.encodeAsXML(url)
            + "</sourceUrl>\n"
            + "    <driverName>"
            + XML.encodeAsXML(driverName)
            + "</driverName>\n");
    for (int i = 0; i < connectionProperties.length; i += 2)
      sb.append(
          "    <connectionProperty name=\""
              + XML.encodeAsXML(connectionProperties[i])
              + "\">"
              + XML.encodeAsXML(connectionProperties[i + 1])
              + "</connectionProperty>\n");
    sb.append(
        "    <catalogName>"
            + XML.encodeAsXML(catalogName)
            + "</catalogName>\n"
            + "    <schemaName>"
            + XML.encodeAsXML(schemaName)
            + "</schemaName>\n"
            + "    <tableName>"
            + XML.encodeAsXML(tableName)
            + "</tableName>\n"
            + (tOrderBy == null || tOrderBy.length() == 0
                ? ""
                : "    <orderBy>" + XML.encodeAsXML(tOrderBy) + "</orderBy>\n")
            + "    <reloadEveryNMinutes>"
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
    String search = "<dataType>byte";
    for (int i = 0; i < booleanList.size(); i++) {
      int po = sb.indexOf("<sourceName>" + booleanList.get(i));
      if (po > 0) {
        int po2 = sb.indexOf(search, po);
        if (po2 > 0 && po2 - po < 160) sb.replace(po2, po2 + search.length(), "<dataType>boolean");
      }
    }

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }

  /**
   * This returns the contents of a dataset in csv form.
   *
   * @param language the index of the selected language
   * @throws Throwable if trouble
   */
  public static String getCSV(int language, String datasetID) throws Throwable {
    String dir = EDStatic.fullTestCacheDirectory;
    EDDTableFromDatabase tedd = (EDDTableFromDatabase) oneFromDatasetsXml(null, datasetID);
    String tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, "", dir, tedd.className() + "_" + datasetID + "_getCSV", ".csv");
    return File2.directReadFrom88591File(dir + tName);
  }
}
