/* 
 * EDDTableFromPostDatabase Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

import java.util.BitSet;
import java.util.HashMap;    //isn't synchronized, but after construction it is just read, so doesn't need to be
import java.util.HashSet;    //isn't synchronized, but after construction it is just read, so doesn't need to be

/** 
 * This is a special-purpose class to handle data from the POST database.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2009-09-03
 */
public class EDDTableFromPostDatabase extends EDDTableFromDatabase { 

    
    /** 
     * set by the constructor. static so various datasets all use same/latest info. 
     * private since this info mustn't leak out.
     * <br>The hashmap holds userName -&gt; roleHashset.
     */
    private static HashMap userHashMap; 


    /**
     * This constructs an EDDTableFromPostDatabase based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromPostDatabase"&gt; 
     *    having just been read.  
     * @return an EDDTableFromPostDatabase.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromPostDatabase fromXml(SimpleXMLReader xmlReader) throws Throwable {
        return (EDDTableFromPostDatabase)lowFromXml(xmlReader, "Post");
    }

    /**
     * The constructor. See EDDTableFromDatabase for details.
     */
    public EDDTableFromPostDatabase(String tDatasetID, String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tDataSourceName,
        String tLocalSourceUrl, String tDriverName, 
        String tConnectionProperties[],
        String tCatalogName, String tSchemaName, String tTableName,
        String tColumnNameQuotes, String tOrderBy[],
        boolean tSourceNeedsExpandedFP_EQ
        ) throws Throwable {

        super(tDatasetID, tAccessibleTo, tOnChange, tFgdcFile, tIso19115File, 
            tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes,
            tDataVariables, tReloadEveryNMinutes,
            tDataSourceName,
            tLocalSourceUrl, tDriverName, tConnectionProperties,
            tCatalogName, tSchemaName, tTableName, tColumnNameQuotes, tOrderBy,
            tSourceNeedsExpandedFP_EQ);
        
        if (verbose) String2.log(
            "\n*** starting POST specific part of EDDTableFromPostDatabase " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();

        userHashMap = makeUserHashMap(dataSourceName, dataSource,
            localSourceUrl, driverName, connectionProperties,
            tCatalogName, tSchemaName);

        if (verbose) String2.log(
            "\n*** EDDTableFromPostDatabase " + datasetID + " constructor finished. TIME=" + 
                (System.currentTimeMillis() - constructionStartMillis) + "\n");
    }

    /**
     * Given the database information, this makes userHashMap.
     *
     * <p>This also calls EDStatic.setPostUserHashMap(postUserHashMap)
     * (a different hashmap so that post users have access to the post datasets).
     *
     * @param dataSource If null, DriverManager will be used to make the connection.
     *   If not null, DataSource will be used to get a connection from the pool.
     * @throws Throwable if trouble
     */
    public static HashMap makeUserHashMap(String dataSourceName, DataSource dataSource,
            String localSourceUrl, String driverName, String connectionProperties[], 
            String catalogName, String schemaName) throws Throwable {

        if (verbose) String2.log("EDDTableFromPostDatabase.makeUserHashMap");

        Test.ensureTrue(EDStatic.PostUserTableName.length()        > 0, "'PostUserTableName' wasn't specified in setup.xml.");
        Test.ensureTrue(EDStatic.PostRoleTableName.length()        > 0, "'PostRoleTableName' wasn't specified in setup.xml.");
        Test.ensureTrue(EDStatic.PostNameColumnName.length()       > 0, "'PostNameColumnName' wasn't specified in setup.xml.");
        Test.ensureTrue(EDStatic.PostPasswordColumnName.length()   > 0, "'PostPasswordColumnName' wasn't specified in setup.xml.");
        Test.ensureTrue(EDStatic.PostRoleColumnName.length()       > 0, "'PostRoleColumnName' wasn't specified in setup.xml.");
        Test.ensureTrue(EDStatic.PostDatePublicColumnName.length() > 0, "'PostDatePublicColumnName' wasn't specified in setup.xml.");

        //make userHashMap:  name -> roleHashset 
        HashMap finishedUserHashMap = null;
        //open a new connection
        Connection connection = makeConnection(dataSourceName, dataSource,
            localSourceUrl, driverName, connectionProperties);        
        try {

            //get catalogSeparator
            DatabaseMetaData meta = connection.getMetaData();
            String catalogSeparator = meta.getCatalogSeparator();

            String catSchemaName = 
                (catalogName.equals("")? "" : catalogName + catalogSeparator) + 
                (schemaName.equals( "")? "" : schemaName  + ".");

            //get the user table's data
            Table table = new Table();
            String query = "SELECT * FROM " + catSchemaName + EDStatic.PostUserTableName;
            PreparedStatement statement = connection.prepareStatement(query);
            if (reallyVerbose) String2.log(statement.toString());
            ResultSet rs = statement.executeQuery();
            table.readSqlResultSet(rs);
            statement.close();
            //String2.log(table.saveAsCsvASCIIString());
            int nameColumn     = table.findColumnNumber(EDStatic.PostNameColumnName);
            int passwordColumn = table.findColumnNumber(EDStatic.PostPasswordColumnName);
            String notFoundIn = "\" not found in userTable=\"" + EDStatic.PostUserTableName + 
                "\" in POST database:\n" +
                table.getColumnNamesCSSVString();
            if (nameColumn < 0)
                throw new RuntimeException("nameColumnName=\"" + EDStatic.PostNameColumnName + notFoundIn);
            if (passwordColumn < 0)
                throw new RuntimeException("passwordColumnName=\"" + EDStatic.PostPasswordColumnName + notFoundIn);
            PrimitiveArray namePa     = table.getColumn(nameColumn);
            PrimitiveArray passwordPa = table.getColumn(passwordColumn);


            //store the user table's data in userHashMap: name -> roleHashset 
            if (verbose) String2.log(
                "\n*************\n" +
                "Issue #18: Invalid User or Role\n\n" +
                "Issue #18a: Invalid user and/or password\n\n" +
                "Invalid User: The user's name.length must be >0. The password.length must be 32.\n\n");
            HashMap tUserHashMap = new HashMap();
            //prepare postUserHashMap   name -> object[2]: [0]=md5Password [1]=String[]{"post"}
            HashMap postUserHashMap = new HashMap();
            int nUsers = table.nRows();
            int nValidUsers = 0, nInvalidUsers = 0;
            for (int row = 0; row < nUsers; row++) {
                String name      = namePa.getString(row);
                String password  = passwordPa.getString(row);
//if (verbose) // && name.equals("ERDUSER"))
//    String2.log("name=" + name + " " + password);
//else if (verbose) String2.log("name=" + name);
                if (name == null || name.length() == 0 || 
                    !name.equals(String2.justPrintable(name)) ||
                    password == null || password.length() != 32 ||  //md5 always 32 chars
                    !password.equals(String2.justPrintable(password))) {
                    if (verbose) String2.log("invalidUser#" + nInvalidUsers + 
                          " name=\"" + name +
                        "\" password.length=" + (password == null? 0 : password.length()));
                    nInvalidUsers++;
                    continue;
                }
                nValidUsers++;  //though there may be duplicates!
                tUserHashMap.put(name, new HashSet());
                postUserHashMap.put(name, new Object[]{
                    password.toLowerCase(), //they are MD5'd, so ensure lowercase.
                    new String[]{"post"}});  
            }
            //tUserHashMap and postUserHashMap are put in place below, when all is almost finished.
                
            //get the role table's data
            table = new Table();
            boolean fakeIt = false; //was true when role values here were inconsistent with role in surgery and detection tables
            if (fakeIt) {
                table.addColumn(EDStatic.PostNameColumnName, new StringArray());
                table.addColumn(EDStatic.PostRoleColumnName, new StringArray());
                table.getColumn(0).addString("ROBICHAUD_DAVID");
                table.getColumn(0).addString("WELCH_DAVID");
                table.getColumn(1).addString("ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER");
                table.getColumn(1).addString("WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE");
            } else {
                query = "SELECT * FROM " + catSchemaName + EDStatic.PostRoleTableName;
                statement = connection.prepareStatement(query);
                rs = statement.executeQuery();
                table.readSqlResultSet(rs);
                statement.close();
            }
            //String2.log(table.saveAsCsvASCIIString());
            nameColumn = table.findColumnNumber(EDStatic.PostNameColumnName);
            int roleColumn = table.findColumnNumber(EDStatic.PostRoleColumnName);
            notFoundIn = "\" not found in roleTable=\"" + EDStatic.PostRoleTableName + 
                "\" in POST database:\n" +
                table.getColumnNamesCSSVString();
            if (nameColumn < 0)
                throw new RuntimeException("nameColumnName=\"" + EDStatic.PostNameColumnName + notFoundIn); 
            if (roleColumn < 0)
                throw new RuntimeException("roleColumnName=\"" + EDStatic.PostRoleColumnName + notFoundIn);
            namePa                = table.getColumn(nameColumn);
            PrimitiveArray rolePa = table.getColumn(roleColumn);

            //store the role table's data in tUserHashMap: name -> roleHashset 
            if (verbose) String2.log(
                "Issue #18b: Invalid role and/or user\n\n" +
                "Invalid role: The user's name.length and role.length must be >0. \n" +
                "  And the name must be in the name/password table.\n");
            int nRoles = table.nRows();
            int nValidRoles = 0, nInvalidRoles = 0;
            for (int row = 0; row < nRoles; row++) {
                String name = namePa.getString(row);
                String role = rolePa.getString(row);
                if (name == null || name.length() == 0 ||
                    !name.equals(String2.justPrintable(name)) ||
                    role == null || role.length() == 0 ||
                    !role.equals(String2.justPrintable(role))) {
                    if (verbose) String2.log("invalidRole#" + nInvalidRoles + 
                          " name=\"" + name +
                        "\" role=\"" + role + "\"");
                    nInvalidRoles++;
                    continue;
                }
                HashSet roleHashSet = (HashSet)tUserHashMap.get(name);
                if (roleHashSet == null) {
                    if (verbose) String2.log("invalidRole#" + nInvalidRoles + 
                          " name=\"" + name +
                        "\" doesn't exist for role=\"" + role + "\"");
                    nInvalidRoles++;
                } else {
                    nValidRoles++; //not exact; there may be duplicates
                    roleHashSet.add(role);
                }
            }
//TestPost
boolean addTestPostUser = false;
            if (addTestPostUser) { 
                HashSet roleHashSet = (HashSet)tUserHashMap.get("ROBICHAUD_DAVID");
                if (roleHashSet == null) {
                    String2.log("! user=TestPost wasn't created because user=ROBICHAUD_DAVID wasn't found.");
                } else {
                    String2.log("! user=TestPost was created.");
                    tUserHashMap.put("TestPost", roleHashSet);
                    postUserHashMap.put("TestPost", new Object[]{
                        "530BCF61CA0263A3840A587B3761D94A".toLowerCase(), //MD5 of password
                        new String[]{"post"}});
                }
            }

            //all is well; put userHashMaps in place
            finishedUserHashMap = tUserHashMap; //put in place is atomic 
            EDStatic.setPostUserHashMap(postUserHashMap);
            
            if (verbose) 
                String2.log(
                    //don't display connectionProperties because of password
                  "\nPOST: nValidUsers=" + nValidUsers + " nInvalidUsers=" + nInvalidUsers + 
                  "\n      nValidRoles=" + nValidRoles + " nInvalidRoles=" + nInvalidRoles); 
            
        } finally {
            connection.close();
        }
        return finishedUserHashMap;
    }

    /**
     * This returns a suggested fileName (no dir or extension).
     * It doesn't add a random number, so will return the same results 
     * if the inputs are the same.
     *
     * @param loggedInAs is only used for POST datasets (which override EDD.suggestFileName)
     *    since loggedInAs is used by POST for row-by-row authorization
     * @param userDapQuery
     * @param fileTypeName
     * @return a suggested fileName (no dir or extension)
     * @throws Exception if trouble (in practice, it shouldn't)
     */
    public String suggestFileName(String loggedInAs, String userDapQuery, String fileTypeName) {

        //decode userDapQuery to a canonical form to avoid slight differences in percent-encoding 
        try {
            userDapQuery = SSR.percentDecode(userDapQuery);
        } catch (Exception e) {
            //shouldn't happen
        }

        //include fileTypeName in hash so, e.g., different sized .png 
        //  have different file names
        String name = datasetID + "_" + 
            String2.md5Hex12(loggedInAs + userDapQuery + fileTypeName); 
        //String2.log("%% POST suggestFileName=" + name + "\n  for loggedInAs=" + loggedInAs + " from query=" + userDapQuery + "\n  from type=" + fileTypeName);
        return name;
    }

    /** 
     * This returns the name of the file in datasetDir()
     * which has all of the distinct data combinations for the current subsetVariables.
     * The file is deleted by setSubsetVariablesCSV(), which is called whenever
     * the dataset is reloaded.
     * See also EDDTable, which deletes these files.
     *
     * @param loggedInAs POST datasets overwrite this method and use loggedInAs 
     *    since data for each loggedInAs is different; others don't use this
     * @throws Exception if trouble
     */
    public String subsetVariablesFileName(String loggedInAs) throws Exception {
        return (loggedInAs == null? "" : String2.encodeFileNameSafe(loggedInAs) + "_") + 
            SUBSET_FILENAME; 
    }

    /** 
     * This returns the name of the file in datasetDir()
     * which has all of the distinct values for the current subsetVariables.
     * The file is deleted by setSubsetVariablesCSV(), which is called whenever
     * the dataset is reloaded.
     * See also EDDTable, which deletes these files.
     *
     * @param loggedInAs POST datasets overwrite this method and use loggedInAs 
     *    since data for each loggedInAs is different; others don't use this
     * @throws Exception if trouble
     */
    public String distinctSubsetVariablesFileName(String loggedInAs) throws Exception {
        return (loggedInAs == null? "" : String2.encodeFileNameSafe(loggedInAs) + "_") + 
            DISTINCT_SUBSET_FILENAME; 
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
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        if (reallyVerbose) String2.log("EDDTableFromPostDatabase query=" + userDapQuery);

        userDapQuery = reviseDapQueryForPost(loggedInAs, userDapQuery);

        //then get the data
        super.getDataForDapQuery(loggedInAs, requestUrl, userDapQuery, tableWriter);
    }

    /** 
     * This is used by getDataForDapQuery for this class and EDDTableFromPostNcFiles
     * to ensure userDapQuery includes PostRoleColumnName and PostDatePublicColumnName.
     *
     * @param loggedInAs (may be null)
     * @return the revised userDapQuery
     * @throws Throwable
     */
    public static String reviseDapQueryForPost(String loggedInAs, String userDapQuery) 
        throws Throwable {

        if (loggedInAs == null || !loggedInAs.equals(EDStatic.loggedInAsSuperuser)) {
            //ensure userDapQuery includes PostRoleColumnName and PostDatePublicColumnName
            String parts[] = getUserQueryParts(userDapQuery); //decoded.  always at least 1 part (may be "")
            if (parts[0].equals("") || parts[0].startsWith("&")) {
                //all variables are already selected
            } else {
                //there are some csv column names
                //add PostRoleColumnName and PostDatePublicColumnName if not present
                String colNames[] = String2.split(parts[0], ',');
                if (String2.indexOf(colNames, EDStatic.PostDatePublicColumnName) < 0)
                    parts[0] += "," + EDStatic.PostDatePublicColumnName;
                if (loggedInAs != null && //if it is null, don't neet to get role info, just date_public
                    String2.indexOf(colNames, EDStatic.PostRoleColumnName) < 0)
                    parts[0] += "," + EDStatic.PostRoleColumnName;
                userDapQuery = String2.toSVString(parts, "&", false);

                if (reallyVerbose) String2.log("reviseDapQueryForPost modifiedQuery=" + userDapQuery);
            }
        }
        return userDapQuery;
    }

    /**
     * getDataForDapQuery always calls this right before standardizeResultsTable.
     * EDDTableFromPostDatabase uses this to remove data not accessible to this user.
     */
    public void preStandardizeResultsTable(String loggedInAs, Table table) {
        staticPreStandardizeResultsTable(loggedInAs, table);
    }

    /**
     * getDataForDapQuery always calls this right before standardizeResultsTable.
     * EDDTableFromPostDatabase uses this to remove data not accessible to this user.
     * <br>This can be static because userHashMap is static.
     * <br>If userHashMap hasn't been set, a new empty one is created (i.e., no valid users).
     */
    public static void staticPreStandardizeResultsTable(String loggedInAs, Table table) {

        //needed for EDDTableCopy: if loggedInAsSuperuser, keep all data
        if (loggedInAs != null && loggedInAs.equals(EDStatic.loggedInAsSuperuser)) {
            if (verbose) String2.log("loggedInAsSuperuser, so not restricting access to POST data.");
            return;
        }

        HashSet roleHashSet = null;
        if (loggedInAs == null) {
        } else {
            //get the user's roleHashSet
            if (userHashMap == null) {
                if (verbose) 
                    String2.log("\n!!!EDDTableFromPostDatabase: there is no userHashMap!\n");
            } else {
                roleHashSet = (HashSet)userHashMap.get(loggedInAs); //name -> roleHashset 
                if (verbose && roleHashSet == null) 
                    String2.log("loggedInAs=" + loggedInAs + " not found in POST userHashMap.");
            }
        }
        if (reallyVerbose) String2.log("preStandardizeResultsTable loggedInAs=" + loggedInAs + 
            "\nhasRoles=" + (roleHashSet==null? "null" : roleHashSet.toString()));

        String notFoundIn = "\" not found in roleTable=\"" + EDStatic.PostRoleTableName + 
            "\" in POST database:\n" +
            table.getColumnNamesCSSVString();

        //get the role column from the table if loggedInAs != null
        StringArray rolePa = null;
        if (loggedInAs != null) {
            int roleColumn = table.findColumnNumber(EDStatic.PostRoleColumnName);
            if (roleColumn < 0)
                throw new RuntimeException("roleColumnName=\"" + EDStatic.PostRoleColumnName + notFoundIn); 
            rolePa = (StringArray)table.getColumn(roleColumn);
        }

        //always get the public_date column from the table
        int datePublicColumn = table.findColumnNumber(EDStatic.PostDatePublicColumnName);
        if (datePublicColumn < 0)
            throw new RuntimeException("datePublicColumnName=\"" + EDStatic.PostDatePublicColumnName + notFoundIn); 
        DoubleArray datePublicPa = (DoubleArray)table.getColumn(datePublicColumn);

        //go through the table's rows; keep or don't
        double nowSeconds = System.currentTimeMillis() / 1000;
        int nRows = table.nRows();
        BitSet keepBitSet = new BitSet(nRows); //initially all false
        for (int row = 0; row < nRows; row++) {
            double datePublic = datePublicPa.get(row);
            boolean keep = datePublic < nowSeconds;
            if (!keep && roleHashSet != null && rolePa != null) {
                keep = roleHashSet.contains(rolePa.get(row));
            }
            //if (reallyVerbose) String2.log("  keep=" + keep + 
            //    " " + Calendar2.epochSecondsToIsoStringT(datePublic) +
            //    (datePublic < nowSeconds? "<" : ">") + 
            //    "now  role=" + (rolePa == null? "" : rolePa.get(row))); 
            if (keep) keepBitSet.set(row);
        }
        table.justKeep(keepBitSet);

        if (reallyVerbose) String2.log("  oldNRows=" + nRows + " newNRows=" + table.nRows());
    }


    /**
     * This tests the post surgery3 dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testPostSurg3() throws Throwable {
        String2.log("\n*** testPostSurg3");
        testVerboseOn();
        long eTime;
        int po;
        String tQuery, tName, results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {
            EDDTableFromPostDatabase tedd = (EDDTableFromPostDatabase)oneFromDatasetXml("postSurg3"); 
 
            //das
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_ps3_Data", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Attributes {\n" +
" s {\n" +
"  unique_tag_id {\n" +
"    String description \"Unique identifier for a tagging.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Unique Tag Identifier\";\n" +
"  }\n" +
"  pi {\n" +
"    String description \"Firstname and Lastname of PI.  All capital letters.  Example: CEDAR CHITTENDEN\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Principal Investigator\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    String axis \"X\";\n" +
"    String description \"The longitude of release back into nature.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    String axis \"Y\";\n" +
"    String description \"The latitude of release back into nature.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +
"    String description \"The time of the release back into nature.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Release Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  activation_time {\n" +
"    String description \"Date/time of the activation, or \\\"turning on\\\" of the tag\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Activation Time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  anaesthetic_conc_recirc {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Anaesthetic Concentration in Recirculation\";\n" +
"    String units \"ppm\";\n" +
"  }\n" +
"  anaesthetic_conc_surgery {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Anaesthetic Concentration in Surgery\";\n" +
"    String units \"ppm\";\n" +
"  }\n" +
"  buffer_conc_anaesthetic {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Buffer Concentration in Anaesthetic\";\n" +
"    String units \"ppm\";\n" +
"  }\n" +
"  buffer_conc_recirc {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Buffer Concentration in Recirculation\";\n" +
"    String units \"ppm\";\n" +
"  }\n" +
"  buffer_conc_surgery {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Buffer Concentration in Surgery\";\n" +
"    String units \"ppm\";\n" +
"  }\n" +
"  channel {\n" +
"    String description \"A, B, C or D\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Channel Heard On Post\";\n" +
"  }\n" +
"  code_label {\n" +
"    String description \"Manufacturer's specification for a tag code.  Encompasses bin and sync.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Code Label\";\n" +
"  }\n" +
"  comments {\n" +
"    String description \"Comments on the surgery, e.g. \\\"tight fit; bleeding\\\"\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Comments\";\n" +
"  }\n" +
"  common_name {\n" +
"    String description \"Common name of the species of the animal\";\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Common Name\";\n" +
"  }\n" +
"  date_public {\n" +
"    String description \"The date the data becomes publicly available.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date Public\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  delay_max {\n" +
"    String description \"Maximum delay between end of previous transmission and beginning of subsequent transmission from a tag\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Delay Maximum\";\n" +
"    String units \"ms\";\n" +
"  }\n" +
"  delay_min {\n" +
"    String description \"Minimum delay between end of previous transmission and beginning of subsequent transmission from a tag\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Delay Minimum\";\n" +
"    String units \"ms\";\n" +
"  }\n" +
"  delay_start {\n" +
"    String description \"Time between the activation of the tag and its first transmission\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Delay Start\";\n" +
"    String units \"days\";\n" +
"  }\n" +
"  delay_type {\n" +
"    String description \"Type of delay interval between adjacent transmissions from a tag (RANDOM or FIXED time)\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Delay Type\";\n" +
"  }\n" +
"  dna_sampled {\n" +
"    String description \"Whether or not a DNA sample was taken (0=false, 1=true)\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"DNA Sampled\";\n" +
"  }\n" +
"  est_tag_life {\n" +
"    String description \"Estimated battery life of a tag, entered in days, starting on the date of activation (activation_time)\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Estimated Tag Life\";\n" +
"    String units \"days\";\n" +
"  }\n" +
"  fork_length {\n" +
"    String description \"Length measured from tip of nose to fork section of the tail fin\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Fork Length\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
"  frequency {\n" +
"    String description \"Frequency of acoustic transmission from a tag\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Frequency\";\n" +
"    String units \"kHz\";\n" +
"  }\n" +
"  implant_type {\n" +
"    String description \"Examples: GASTRIC, INTERNAL, NOT ENTERED, EXTERNAL\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Device Implant Type\";\n" +
"  }\n" +
"  length_hood {\n" +
"    String description \"Hood length of a fish\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Hood Length\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
"  length_pcl {\n" +
"    String description \"Pre-caudal length, measured from the tip of the nose to the beginning of the caudal fin\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"PCL Length\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
"  length_standard {\n" +
"    String description \"Standard length of a fish\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Standard Length\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
"  project {\n" +
"    String description \"Project under which the surgery was done\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Project\";\n" +
"  }\n" +
"  provenance {\n" +
"    String description \"Origin of the animal, e.g. Wild, Hatchery, Unknown, N/A\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Provenance\";\n" +
"  }\n" +
"  role {\n" +
"    String description \"The name of a role that a user must have to access this data before date_public.  By default, it is a concatenation of pi last name, '_', pi first name, '_', scientific name, '_', stock.  Example:  CHITTENDEN_CEDAR_ONCORHYNCHUSKISUTCH_CHEHALIS.\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Accessible-To Role\";\n" +
"  }\n" +
"  scientific_name {\n" +
"    String description \"Scientific name of the species of the animal\";\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Scientific Name\";\n" +
"  }\n" +
"  sedative_conc_surgery {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Sedative Concentration in Surgery\";\n" +
"    String units \"ppm\";\n" +
"  }\n" +
"  stock {\n" +
"    String description \"Stock is a grouping of animals for management purposes.  There is no clear-cut scientific definition, but the concept is used for defining a discreet population of animals.  It's a management term.  A stock is sometimes defined by genetics, sometimes by location (e.g. the northern or southern groups).  It describes a population that is isolated from other animals.  Example: CHEHALIS. This doesn't use a controlled vocabulary.\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Stock\";\n" +
"  }\n" +
"  surgery_id {\n" +
"    String description \"Unique identifier for a surgery (from the surgery.surgery table)\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Surgery ID\";\n" +
"  }\n" +
"  surgery_location {\n" +
"    String description \"Description of the geographical location where the surgery was performed (not to be confused with the release location)\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Surgery Location\";\n" +
"  }\n" +
"  surgery_longitude {\n" +
"    String description \"Longitude where the surgery was conducted\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Surgery Longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  surgery_latitude {\n" +
"    String description \"Latitude where the surgery was conducted\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Surgery Latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  surgery_time {\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Surgery Time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  tagger {\n" +
"    String description \"Name of the person who performed the surgery\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Tagger\";\n" +
"  }\n" +
"  treatment_type {\n" +
"    String description \"Type of treatment provided to the animal (not a controlled vocabulary)\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Treatment Type\";\n" +
"  }\n" +
"  water_temp {\n" +
"    String description \"Temperature of water during surgery\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Water Temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  weight {\n" +
"    String description \"Weight at time of surgery\";\n" +
"    String ioos_category \"Biology\";\n" +
"    String long_name \"Weight\";\n" +
"    String units \"grams\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today + " (source database)\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/postSurg3.das\";\n" +
"    String infoUrl \"http://www.postprogram.org/\";\n" +
"    String institution \"POST\";\n" +
"    String license \"DISCLAIMER\n" +
"\n" +
"The POST data is a best effort, work-in-progress.\n" +
"Data will change as corrections are made and new data are added.\n" +
"We will continue to work with our principal investigators to improve\n" +
"the quality of the data.  Also, a few tags are not available for\n" +
"viewing or downloading because they have more detection data than\n" +
"ERDDAP (or your browser) can handle. If you are interested in the\n" +
"complete, unfiltered POST data, please contact the POST Database\n" +
"Manager at postdata at vanaqua dot org.\n" +
"THIS DATA IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n" +
"\"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT\n" +
"LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS\n" +
"FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n" +
"\n" +
"LICENSE\n" +
"\n" +
"By using POST data, I agree that, in any publication or presentation of any sort based wholly or in part on such data, I will:\n" +
"\n" +
"1.	Acknowledge the use of POST in one of the following prescribed forms:\n" +
"\n" +
"For POST website:\n" +
"\n" +
"Pacific Ocean Shelf Tracking (POST) Program. [date accessed] http://www.postprogram.org\n" +
"\n" +
"For data used:\n" +
"\n" +
"Principal Investigator.  Project.  [Query value1]; [Query value2]; [Query value...n].  Retrieved [date accessed] from http://www.postprogram.org\n" +
"\n" +
"Example:\n" +
"\n" +
"David Welch. Kintama Research.  common_name=CHINOOK, stock=\\\"Columbia River\\\", time>=\\\"2004-01-01\\\", time<\\\"2005-01-01\\\". Retrieved Jan. 1, 2008 from http://www.postprogram.org.\n" +
"\n" +
"2.	For information purposes, provide to post@vanaqua.org the full citation of any publication I make (printed or electronic) that cites POST.\n" +
"\n" +
"Data ownership\n" +
"\n" +
"Principal Investigators providing data to POST retain primary ownership of their data.  As such, POST does not take responsibility for the quality of such data or the use that people may make of them.\n" +
"\n" +
"All metadata from Principal Investigators, when first uploaded to the POST system, are restricted to the investigator's sole access for one year unless the Principal Investigator chooses earlier release.\n" +
"\n" +
"Data users\n" +
"\n" +
"Data users are defined as parties providing data and parties accessing data. Data Users agree to the policy and conditions of data access as adopted by the POST Management Board on December 11, 2008 or as subsequently amended.\n" +
"\n" +
"The Pacific Ocean Shelf Tracking (POST) Project Data Access Policy is available for review at:  http://www.postprogram.org/files/POST_Data_Access_Policy.pdf\n" +
"\n" +
"Data users should email any questions concerning POST data or tools (e.g. maps) to post@vanaqua.org\n" +
"\n" +
"Data use\n" +
"\n" +
"All data are either permanently public, or temporarily restricted.\n" +
"\n" +
"In this dataset, for rows of data where the date_public is in the future, access to this data is restricted to users who are logged in and have the appropriate role.  Such data may only be used in accordance with the terms specified by the owner of the data. Contact the owner for details.\n" +
"\n" +
"In this dataset, for rows of data where the date_public is in the past, the data is publicly available and may be used freely; although for scientific papers, you must reference the source of the data.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String sourceUrl \"(source database)\";\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"This dataset has animal tagging surgery records from the Pacific Ocean \n" +
"Shelf Tracking project (POST).  POST is a research tool for \n" +
"tracking the movement and estimated survival of marine animals along \n" +
"the West Coast of North America, using acoustic transmitters implanted \n" +
"in animals and a series of receivers running in lines across the \n" +
"continental shelf.  It is one of fourteen field projects of the \n" +
"Census of Marine Life assessing the distribution, diversity and \n" +
"abundance of marine organisms internationally.  (V3)\n" +
"\n" +
"By accessing this dataset, you agree to the License (terms of use) \n" +
"in the dataset's metadata.\";\n" +
"    String title \"POST Surgeries\";\n" +
"  }\n" +
"}\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
  
            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_ps3_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String unique_tag_id;\n" +
"    String pi;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    Float64 activation_time;\n" +
"    Float64 anaesthetic_conc_recirc;\n" +
"    Float64 anaesthetic_conc_surgery;\n" +
"    Float64 buffer_conc_anaesthetic;\n" +
"    Float64 buffer_conc_recirc;\n" +
"    Float64 buffer_conc_surgery;\n" +
"    String channel;\n" +
"    String code_label;\n" +
"    String comments;\n" +
"    String common_name;\n" +
"    Float64 date_public;\n" +
"    Int32 delay_max;\n" +
"    Int32 delay_min;\n" +
"    Int32 delay_start;\n" +
"    String delay_type;\n" +
"    Byte dna_sampled;\n" +
"    Float64 est_tag_life;\n" +
"    Float64 fork_length;\n" +
"    Float64 frequency;\n" +
"    String implant_type;\n" +
"    Float64 length_hood;\n" +
"    Float64 length_pcl;\n" +
"    Float64 length_standard;\n" +
"    String project;\n" +
"    String provenance;\n" +
"    String role;\n" +
"    String scientific_name;\n" +
"    Float64 sedative_conc_surgery;\n" +
"    String stock;\n" +
"    Int32 surgery_id;\n" +
"    String surgery_location;\n" +
"    Float64 surgery_longitude;\n" +
"    Float64 surgery_latitude;\n" +
"    Float64 surgery_time;\n" +
"    String tagger;\n" +
"    String treatment_type;\n" +
"    Float64 water_temp;\n" +
"    Float64 weight;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //test 1 role, public data   not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1role", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, activation_time, anaesthetic_conc_recirc, anaesthetic_conc_surgery, buffer_conc_anaesthetic, buffer_conc_recirc, buffer_conc_surgery, channel, code_label, comments, common_name, date_public, delay_max, delay_min, delay_start, delay_type, dna_sampled, est_tag_life, fork_length, frequency, implant_type, length_hood, length_pcl, length_standard, project, provenance, role, scientific_name, sedative_conc_surgery, stock, surgery_id, surgery_location, surgery_longitude, surgery_latitude, surgery_time, tagger, treatment_type, water_temp, weight\n" +
", , degrees_east, degrees_north, UTC, UTC, ppm, ppm, ppm, ppm, ppm, , , , , UTC, ms, ms, days, , , days, mm, kHz, , mm, mm, mm, , , , , ppm, , , , degrees_east, degrees_north, UTC, , , degree_C, grams\n" +
"19835, DAVID WELCH, -121.97974, 49.07988, 2007-05-16T22:00:00Z, 2007-05-13T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, A, A69-1105-AE, , \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, 90, 30, 0, RANDOM, 0, 2055.0, 204.0, 69.0, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, 1.0, CULTUS LAKE, 7991, INCH CREEK HATCHERY, -122.16152, 49.17132, 2007-05-13T07:00:00Z, ADRIAN LADOUCEUR, SHADE, 6.7, NaN\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            String2.log("*** testPostSurg3 1role FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, public data   loggedIn for other data
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleLIOD", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //same expected
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
            String2.log("*** testPostSurg3 1roleLIOD FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test 1 role, private data, not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            try {
                tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleP", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                throw new Exception("Shouldn't get here.");
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception("Should have been THERE_IS_NO_DATA: " +
                        MustBe.throwableToString(t));
            }
            String2.log("*** testPostSurg3 1roleP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, wrong log in
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            try {
                tName = tedd.makeNewFileForDapQuery(null, "WELCH_DAVID", tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleWP", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                throw new Exception("Shouldn't get here.");
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception("Should have been THERE_IS_NO_DATA: " +
                        MustBe.throwableToString(t));
            }
            String2.log("*** testPostSurg3 1roleWP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, public data, logged in
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, activation_time, anaesthetic_conc_recirc, anaesthetic_conc_surgery, buffer_conc_anaesthetic, buffer_conc_recirc, buffer_conc_surgery, channel, code_label, comments, common_name, date_public, delay_max, delay_min, delay_start, delay_type, dna_sampled, est_tag_life, fork_length, frequency, implant_type, length_hood, length_pcl, length_standard, project, provenance, role, scientific_name, sedative_conc_surgery, stock, surgery_id, surgery_location, surgery_longitude, surgery_latitude, surgery_time, tagger, treatment_type, water_temp, weight\n" +
", , degrees_east, degrees_north, UTC, UTC, ppm, ppm, ppm, ppm, ppm, , , , , UTC, ms, ms, days, , , days, mm, kHz, , mm, mm, mm, , , , , ppm, , , , degrees_east, degrees_north, UTC, , , degree_C, grams\n" +
"23669, DAVID ROBICHAUD, -122.816286671098, 49.2235994918324, 2008-08-24T14:50:00Z, 2008-08-23T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, Missing L pec fin, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 850.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7992, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.816286671098, 49.2235994918324, 2008-08-23T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n" +
"23670, DAVID ROBICHAUD, -122.816286671098, 49.2235994918324, 2008-08-24T14:50:00Z, 2008-08-23T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, def in lower lobe of caudal fin, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 1070.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7993, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.816286671098, 49.2235994918324, 2008-08-23T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n" +
"23671, DAVID ROBICHAUD, -122.788286819502, 49.2229398886514, 2008-08-30T15:56:00Z, 2008-08-30T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, small scrape near incision, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 715.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7994, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.788286819502, 49.2229398886514, 2008-08-30T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testPostSurg3 1roleL FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test public data + logged in
            eTime = System.currentTimeMillis();
            tQuery = "";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1rolePDLI", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, activation_time, anaesthetic_conc_recirc, anaesthetic_conc_surgery, buffer_conc_anaesthetic, buffer_conc_recirc, buffer_conc_surgery, channel, code_label, comments, common_name, date_public, delay_max, delay_min, delay_start, delay_type, dna_sampled, est_tag_life, fork_length, frequency, implant_type, length_hood, length_pcl, length_standard, project, provenance, role, scientific_name, sedative_conc_surgery, stock, surgery_id, surgery_location, surgery_longitude, surgery_latitude, surgery_time, tagger, treatment_type, water_temp, weight\n" +
", , degrees_east, degrees_north, UTC, UTC, ppm, ppm, ppm, ppm, ppm, , , , , UTC, ms, ms, days, , , days, mm, kHz, , mm, mm, mm, , , , , ppm, , , , degrees_east, degrees_north, UTC, , , degree_C, grams\n" +
"19835, DAVID WELCH, -121.97974, 49.07988, 2007-05-16T22:00:00Z, 2007-05-13T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, A, A69-1105-AE, , \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, 90, 30, 0, RANDOM, 0, 2055.0, 204.0, 69.0, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, 1.0, CULTUS LAKE, 7991, INCH CREEK HATCHERY, -122.16152, 49.17132, 2007-05-13T07:00:00Z, ADRIAN LADOUCEUR, SHADE, 6.7, NaN\n" +
"23669, DAVID ROBICHAUD, -122.816286671098, 49.2235994918324, 2008-08-24T14:50:00Z, 2008-08-23T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, Missing L pec fin, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 850.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7992, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.816286671098, 49.2235994918324, 2008-08-23T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n" +
"23670, DAVID ROBICHAUD, -122.816286671098, 49.2235994918324, 2008-08-24T14:50:00Z, 2008-08-23T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, def in lower lobe of caudal fin, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 1070.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7993, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.816286671098, 49.2235994918324, 2008-08-23T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testPostSurg3 1rolePDLI FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test public data all have lat/lon/time?
            eTime = System.currentTimeMillis();
            tQuery = "longitude,latitude,time&time>=2007-05-01&time<2007-09-01";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_LLT", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log("results=\n" + results.substring(0, Math.min(results.length(), 10000)));
            po = results.indexOf("NaN");
            String2.log("\nresults.length()=" + results.length() + "  NaN po=" + po);
            Test.ensureEqual(po, -1, "\nresults=\n" + results.substring(0, Math.min(results.length(), 10000)));
            String2.log("*** testPostSurg3 LLT FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Unexpected EDDTableFromPostDatabase.testPostSurg3 error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * This tests the post detection3 dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testPostDet3() throws Throwable {
        String2.log("\n*** testPostDet3");
        testVerboseOn();
        long eTime;
        String tQuery, tName, results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {
            EDDTableFromPostDatabase tedd = (EDDTableFromPostDatabase)oneFromDatasetXml("postDet3"); 
/* */
            //das
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_pd3_Data", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Attributes {\n" +
" s {\n" +
"  unique_tag_id {\n" +
"    String description \"Unique identifier for a tagging.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Unique Tag Identifier\";\n" +
"  }\n" +
"  pi {\n" +
"    String description \"Firstname and Lastname of PI.  All capital letters.  Example: CEDAR CHITTENDEN\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Principal Investigator\";\n" +
"  }\n" +
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
"    String long_name \"Detection Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  bottom_depth {\n" +
"    String description \"Depth (meters, positive=down) of the bottom of the body of water where the receiver was deployed\";\n" +
"    String ioos_category \"Bathymetry\";\n" +
"    String long_name \"Bottom Depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  common_name {\n" +
"    String description \"Common name of the species of the animal\";\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Common Name\";\n" +
"  }\n" +
"  date_public {\n" +
"    String description \"The date the data becomes publicly available.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Date Public\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  position_on_subarray {\n" +
"    String description \"Position of an individual receiver with respect to the other receivers in a line.  Not a controlled vocabulary, but usually an integer from 1 (closest to shore) to n (furthest from short).\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Position on Subarray\";\n" +
"  }\n" +
"  project {\n" +
"    String description \"Project under which the surgery was done\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Project\";\n" +
"  }\n" +
"  riser_height {\n" +
"    String description \"Vertical distance of the receiver from its anchor, which lies at the bottom at depth bottom_depth\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Riser Height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  role {\n" +
"    String description \"The name of a role that a user must have to access this data before date_public.  By default, it is a concatenation of pi last name, '_', pi first name, '_', scientific name, '_', stock.  Example:  CHITTENDEN_CEDAR_ONCORHYNCHUSKISUTCH_CHEHALIS.\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Accessible-To Role\";\n" +
"  }\n" +
"  scientific_name {\n" +
"    String description \"Scientific name of the species of the animal\";\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Scientific Name\";\n" +
"  }\n" +
"  stock {\n" +
"    String description \"Stock is a grouping of animals for management purposes.  There is no clear-cut scientific definition, but the concept is used for defining a discreet population of animals.  It's a management term.  A stock is sometimes defined by genetics, sometimes by location (e.g. the northern or southern groups).  It describes a population that is isolated from other animals.  Example: CHEHALIS.  This doesn't use a controlled vocabulary.\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Stock\";\n" +
"  }\n" +
"  surgery_time {\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Surgery Time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  surgery_location {\n" +
"    String description \"Description of the geographical location where the surgery was performed (not to be confused with the release location)\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Surgery Location\";\n" +
"  }\n" +
"  tagger {\n" +
"    String description \"Name of the person who performed the surgery\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Tagger\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today + " (source database)\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/postDet3.das\";\n" +
"    String infoUrl \"http://www.postprogram.org/\";\n" +
"    String institution \"POST\";\n" +
"    String license \"DISCLAIMER\n" +
"\n" +
"The POST data is a best effort, work-in-progress.\n" +
"Data will change as corrections are made and new data are added.\n" +
"We will continue to work with our principal investigators to improve\n" +
"the quality of the data.  Also, a few tags are not available for\n" +
"viewing or downloading because they have more detection data than\n" +
"ERDDAP (or your browser) can handle. If you are interested in the\n" +
"complete, unfiltered POST data, please contact the POST Database\n" +
"Manager at postdata at vanaqua dot org.\n" +
"THIS DATA IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n" +
"\"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT\n" +
"LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS\n" +
"FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n" +
"\n" +
"LICENSE\n" +
"By using POST data, I agree that, in any publication or presentation of any sort based wholly or in part on such data, I will:\n" +
"\n" +
"1.	Acknowledge the use of POST in one of the following prescribed forms:\n" +
"\n" +
"For POST website:\n" +
"\n" +
"Pacific Ocean Shelf Tracking (POST) Program. [date accessed] http://www.postprogram.org\n" +
"\n" +
"For data used:\n" +
"\n" +
"Principal Investigator.  Project.  [Query value1]; [Query value2]; [Query value...n].  Retrieved [date accessed] from http://www.postprogram.org\n" +
"\n" +
"Example:\n" +
"\n" +
"David Welch. Kintama Research.  common_name=CHINOOK, stock=\\\"Columbia River\\\", time>=\\\"2004-01-01\\\", time<\\\"2005-01-01\\\". Retrieved Jan. 1, 2008 from http://www.postprogram.org.\n" +
"\n" +
"2.	For information purposes, provide to post@vanaqua.org the full citation of any publication I make (printed or electronic) that cites POST.\n" +
"\n" +
"Data ownership\n" +
"\n" +
"Principal Investigators providing data to POST retain primary ownership of their data.  As such, POST does not take responsibility for the quality of such data or the use that people may make of them.\n" +
"\n" +
"All metadata from Principal Investigators, when first uploaded to the POST system, are restricted to the investigator's sole access for one year unless the Principal Investigator chooses earlier release.\n" +
"\n" +
"Data users\n" +
"\n" +
"Data users are defined as parties providing data and parties accessing data. Data Users agree to the policy and conditions of data access as adopted by the POST Management Board on December 11, 2008 or as subsequently amended.\n" +
"\n" +
"The Pacific Ocean Shelf Tracking (POST) Project Data Access Policy is available for review at:  http://www.postprogram.org/files/POST_Data_Access_Policy.pdf\n" +
"\n" +
"Data users should email any questions concerning POST data or tools (e.g. maps) to post@vanaqua.org\n" +
"\n" +
"Data use\n" +
"\n" +
"All data are either permanently public, or temporarily restricted.\n" +
"\n" +
"In this dataset, for rows of data where the date_public is in the future, access to this data is restricted to users who are logged in and have the appropriate role.  Such data may only be used in accordance with the terms specified by the owner of the data. Contact the owner for details.\n" +
"\n" +
"In this dataset, for rows of data where the date_public is in the past, the data is publicly available and may be used freely; although for scientific papers, you must reference the source of the data.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String sourceUrl \"(source database)\";\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"This dataset has tag detection records from the Pacific Ocean \n" +
"Shelf Tracking project (POST).  POST is a research tool for \n" +
"tracking the movement and estimated survival of marine animals along \n" +
"the West Coast of North America, using acoustic transmitters implanted \n" +
"in animals and a series of receivers running in lines across the \n" +
"continental shelf.  It is one of fourteen field projects of the \n" +
"Census of Marine Life assessing the distribution, diversity and \n" +
"abundance of marine organisms internationally.  (V3)\n" +
"\n" +
"By accessing this dataset, you agree to the License (terms of use) \n" +
"in the dataset's metadata.\";\n" +
"    String title \"POST Tag Detections\";\n" +
"  }\n" +
"}\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
  
            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_pd3_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String unique_tag_id;\n" +
"    String pi;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    Float64 bottom_depth;\n" +
"    String common_name;\n" +
"    Float64 date_public;\n" +
"    String position_on_subarray;\n" +
"    String project;\n" +
"    Float64 riser_height;\n" +
"    String role;\n" +
"    String scientific_name;\n" +
"    String stock;\n" +
"    Float64 surgery_time;\n" +
"    String surgery_location;\n" +
"    String tagger;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //test 1 tag, public data   not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&unique_tag_id=\"19835\"";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1tag", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//the row from the surgery table is
//"19835, DAVID WELCH, -121.97974, 49.07988, 2007-05-16T22:00:00Z, 2007-05-13T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, A, A69-1105-AE, , /"SOCKEYE, KOKANEE/", 2008-09-29T23:29:37Z, 90, 30, 0, RANDOM, 0, 2055.0, 204.0, 69.0, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, 1.0, CULTUS LAKE, 7991, INCH CREEK HATCHERY, -122.16152, 49.17132, 2007-05-13T07:00:00Z, ADRIAN LADOUCEUR, SHADE, 6.7, NaN\n";
            expected =   
"unique_tag_id, pi, longitude, latitude, time, bottom_depth, common_name, date_public, position_on_subarray, project, riser_height, role, scientific_name, stock, surgery_time, surgery_location, tagger\n" +
", , degrees_east, degrees_north, UTC, m, , UTC, , , m, , , , UTC, , \n" +
"19835, DAVID WELCH, -121.97974, 49.07988, 2007-05-16T22:00:00Z, NaN, \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, , KINTAMA RESEARCH, NaN, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, CULTUS LAKE, 2007-05-13T07:00:00Z, INCH CREEK HATCHERY, ADRIAN LADOUCEUR\n" +
"19835, DAVID WELCH, -122.59574, 49.20158, 2007-05-20T23:34:31Z, 1.3, \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, , KINTAMA RESEARCH, NaN, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, CULTUS LAKE, 2007-05-13T07:00:00Z, INCH CREEK HATCHERY, ADRIAN LADOUCEUR\n" +
"19835, DAVID WELCH, -122.59574, 49.20158, 2007-05-20T23:35:49Z, 1.3, \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, , KINTAMA RESEARCH, NaN, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, CULTUS LAKE, 2007-05-13T07:00:00Z, INCH CREEK HATCHERY, ADRIAN LADOUCEUR\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testPostDet3 1tag FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test 1 role, public data   not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1role", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //expected = same
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testPostDet3 1role FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, public data   loggedIn for other data
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleLIOD", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //same expected
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testPostDet3 1roleLIOD FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test 1 role, private data, not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            try {
                tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleP", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                throw new Exception("Shouldn't get here.");
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception("Should have been THERE_IS_NO_DATA: " +
                        MustBe.throwableToString(t));
            }
            String2.log("*** testPostDet3 1roleP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, wrong log in
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            try {
                tName = tedd.makeNewFileForDapQuery(null, "WELCH_DAVID", tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleWP", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                throw new Exception("Shouldn't get here.");
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception("Should have been THERE_IS_NO_DATA: " +
                        MustBe.throwableToString(t));
            }
            String2.log("*** testPostDet3 1roleWP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, private data, logged in
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
String2.log("\n" + results.substring(0, 4000) + "\n");
            expected =   
"unique_tag_id, pi, longitude, latitude, time, bottom_depth, common_name, date_public, position_on_subarray, project, riser_height, role, scientific_name, stock, surgery_time, surgery_location, tagger\n" +
", , degrees_east, degrees_north, UTC, m, , UTC, , , m, , , , UTC, , \n" +
"23669, DAVID ROBICHAUD, -122.816287004559, 49.2235095396314, 2008-08-24T03:17:40Z, 3.5, WHITE STURGEON, 2010-01-23T21:25:29Z, 1, LGL LIMITED, NaN, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, Lower Fraser, 2008-08-23T07:00:00Z, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, Lucia Ferreira\n" +
"23669, DAVID ROBICHAUD, -122.816287004559, 49.2235095396314, 2008-08-24T03:23:59Z, 3.5, WHITE STURGEON, 2010-01-23T21:25:29Z, 1, LGL LIMITED, NaN, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, Lower Fraser, 2008-08-23T07:00:00Z, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, Lucia Ferreira\n" +
"23669, DAVID ROBICHAUD, -122.816287004559, 49.2235095396314, 2008-08-24T03:29:33Z, 3.5, WHITE STURGEON, 2010-01-23T21:25:29Z, 1, LGL LIMITED, NaN, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, Lower Fraser, 2008-08-23T07:00:00Z, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, Lucia Ferreira\n"; 
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results.substring(0, 4000));
            String2.log("*** testPostDet3 1roleL FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

/* */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Unexpected EDDTableFromPostDatabase.testPostDet3 error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
    }


    /** This tests getting all of the data for one tag (like how EDDTableCopyPost does it). 
     * This is to try to resolve the problem of 2010-04-08.
     * For testPostTag, you have to change datasets2.xml to create separate
     * <dataset type="EDDTableFromPostDatabase" datasetID="testPostDet3">
     */
    public static void testPostTag()  throws Throwable {
        String2.log("\n*** testPostTag");
        testVerboseOn();
        long eTime;
        String tQuery, tName, results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {
            EDDTableFromPostDatabase tedd = (EDDTableFromPostDatabase)oneFromDatasetXml(
                "testPostDet3"); 

            tName = tedd.makeNewFileForDapQuery(null, null, 
                //fails:
                //"&PI=\"BARRY BEREJIKIAN\"&scientific_name=\"ONCORHYNCHUS MYKISS\"" +
                //"&stock=\"BIG BEEF CREEK\"&unique_tag_id=\"1039490\"", 

                //fails:
                "&unique_tag_id=\"1039490\"", 

                //succeeds 
                //"unique_tag_id,PI,longitude,latitude&unique_tag_id=\"1039490\"",

                //succeeds 
                //"unique_tag_id,PI,longitude,latitude,time,bottom_depth,common_name,date_public" +
                ////",position_on_subarray,project,riser_height,role,scientific_name,serial_number" +
                //",stock,surgery_time,surgery_location,tagger" +
                //"&unique_tag_id=\"1039490\"",

                //succeeds
                //"unique_tag_id,PI,longitude,latitude,time,bottom_depth,common_name,date_public" +
                //",position_on_subarray,project" +
                ////,riser_height,role,scientific_name,serial_number" +
                //",stock,surgery_time,surgery_location,tagger" +
                //"&unique_tag_id=\"1039490\"",

                //succeeds  !!!
                //"unique_tag_id,PI,longitude,latitude,time,bottom_depth,common_name,date_public" +
                //",position_on_subarray,project" +
                ////,riser_height
                //",role,scientific_name,serial_number" +
                //",stock,surgery_time,surgery_location,tagger" +
                //"&unique_tag_id=\"1039490\"",

                EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_pd3_tag", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"unique_tag_id, PI, longitude, latitude, time, bottom_depth, common_name, date_public, position_on_subarray, project, riser_height, role, scientific_name, serial_number, stock, surgery_time, surgery_location, tagger\n" +
", , degrees_east, degrees_north, UTC, m, , UTC, , , m, , , , , UTC, , \n" +
"1039490, BARRY BEREJIKIAN, NaN, NaN, 2007-04-21T18:10:00Z, 0.0, STEELHEAD, 2010-01-31T07:05:11Z, , NOAA|NOAA FISHERIES, NaN, BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_BIGBEEFCREEK, ONCORHYNCHUS MYKISS, 1039490, BIG BEEF CREEK, , NOT ENTERED, ";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Unexpected EDDTableFromPostDatabase.testPostTag error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * This tests the testPostSurgery3 dataset (direct test of database, not the local copy).
     *
     * @throws Throwable if trouble
     */
    public static void testPostSurg3Direct() throws Throwable {
        String2.log("\n*** testPostSurg3");
        testVerboseOn();
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, tQuery, results, expected;
        Table table;
        try {
            EDDTableFromDatabase tedd = (EDDTableFromDatabase)oneFromDatasetXml("testPostSurg3"); 

            tName = tedd.makeNewFileForDapQuery(null, null, "&PI=\"DAVID WELCH\"", 
                dir, tedd.className() + "_ps3_Data", ".nc"); 
            table = new Table();
            table.readFlatNc(dir + tName, null, 1);
            table.removeRows(5, Math.max(5, table.nRows()));
            results = table.dataToCSVString();
            expected = 
"unique_tag_id,PI,longitude,latitude,time,activation_time,channel,code_label,comments,common_name,date_public,delay_max,delay_min,delay_start,delay_type,dna_sampled,est_tag_life,frequency,implant_type,project,provenance,role,scientific_name,stock,surgery_id,surgery_location,surgery_longitude,surgery_latitude,surgery_time,tag_id_code,tagger,treatment_type,water_temp\n" +
"1000_A69-1204_1030201,DAVID WELCH,-121.0529,49.8615,1.1165265E9,1.1152512E9,D,A69-1204,ADIPOSE FIN CLIPPED,COHO,1.222730913E9,90,30,0,RANDOM,0,,,INTERNAL,KINTAMA RESEARCH,HATCHERY,WELCH_DAVID_ONCORHYNCHUS_KISUTCH_SPIUSCREEK,ONCORHYNCHUS KISUTCH,SPIUS CREEK,1148,SPIUS CREEK HATCHERY,-121.0253,50.1415,1.116288E9,1000,MELINDA JACOBS,NOT ENTERED,7.5\n" +
"1000_A69-1204_1032813,DAVID WELCH,-115.93472,46.12972,1.1471112E9,1.1447136E9,D,A69-1204,VERY TIGHT FIT,CHINOOK,1.222730938E9,90,30,0,RANDOM,0,486.4,,INTERNAL,KINTAMA RESEARCH,HATCHERY,WELCH_DAVID_ONCORHYNCHUS_TSHAWYTSCHA_DWORSHAK,ONCORHYNCHUS TSHAWYTSCHA,DWORSHAK,3808,KOOSKIA NATIONAL FISH HATCHERY,-115.93472,46.12972,1.1447136E9,1000,ADRIAN LADOUCEUR,ROR,12.9\n" +
"1001_A69-1204_1030202,DAVID WELCH,-121.0529,49.8615,1.1165265E9,1.1152512E9,D,A69-1204,\"ADDED 10 ML MS222,ADIPOSE FIN CLIPPED\",COHO,1.222730913E9,90,30,0,RANDOM,0,,,INTERNAL,KINTAMA RESEARCH,HATCHERY,WELCH_DAVID_ONCORHYNCHUS_KISUTCH_SPIUSCREEK,ONCORHYNCHUS KISUTCH,SPIUS CREEK,1149,SPIUS CREEK HATCHERY,-121.0253,50.1415,1.116288E9,1001,MELINDA JACOBS,NOT ENTERED,7.6\n" +
"1001_A69-1204_1032814,DAVID WELCH,-115.93472,46.12972,1.1471112E9,1.1447136E9,D,A69-1204,,CHINOOK,1.222730938E9,90,30,0,RANDOM,0,486.4,,INTERNAL,KINTAMA RESEARCH,HATCHERY,WELCH_DAVID_ONCORHYNCHUS_TSHAWYTSCHA_DWORSHAK,ONCORHYNCHUS TSHAWYTSCHA,DWORSHAK,3809,KOOSKIA NATIONAL FISH HATCHERY,-115.93472,46.12972,1.1447136E9,1001,ADRIAN LADOUCEUR,ROR,12.8\n" +
"1001_A69-1204_9174D,DAVID WELCH,-120.850361111111,50.1459722222222,1.0825668E9,1.0816416E9,D,A69-1204,,CHINOOK,1.222730904E9,90,30,0,RANDOM,0,,,INTERNAL,KINTAMA RESEARCH,HATCHERY,WELCH_DAVID_ONCORHYNCHUS_TSHAWYTSCHA_NICOLARIVER,ONCORHYNCHUS TSHAWYTSCHA,NICOLA RIVER,152,SPIUS CREEK HATCHERY,-121.0253,50.1415,1.0818144E9,1001,MELINDA JACOBS,NOT ENTERED,7.1\n";

            Test.ensureEqual(results, expected, "results=\n" + results);

            String tags[] = new String[]{
"1000_A69-1204_1030201", "1000_A69-1204_1032813", "1001_A69-1204_1030202", 
"1001_A69-1204_1032814", "1001_A69-1204_9174D"};
            for (int tag = 0; tag < tags.length; tag++) {
                tName = tedd.makeNewFileForDapQuery(null, null, 
                    "&unique_tag_id=\"" + tags[tag] + "\"", 
                    dir, tedd.className() + "_ps3_tag" + tag, ".nc"); 
                Table table2 = new Table();
                table2.readFlatNc(dir + tName, null, 1);
                results = table.dataToCSVString();

                Table table1 = (Table)table.clone();
                table1.removeRows(0, tag);
                table1.removeRows(tag + 1, table1.nRows());
                expected = table.dataToCSVString();

                Test.ensureEqual(results, expected, "tag=" + tag + " results=\n" + results);
            }

        } catch (Throwable t) {
            String2.getStringFromSystemIn(
                "\n*** Unexpected EDDTableFromDatabase.testPostSurg3 error:\n" +
                MustBe.throwableToString(t) + 
                "Press ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromPostDatabase.test() *****************\n");

        //tests usually run
        testPostSurg3Direct();
        testPostSurg3();
        testPostDet3();

    }


}

