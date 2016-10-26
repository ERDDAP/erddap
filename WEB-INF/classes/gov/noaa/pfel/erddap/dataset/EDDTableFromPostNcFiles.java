/* 
 * EDDTableFromNcPostFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.HashMap;
import java.util.List;

/**
 * Get netcdf-X.X.XX.jar from 
 * http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/index.html
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents a table of data from a collection of n-dimensional (1,2,3,4,...) .nc data files.
 * The dimensions are e.g., time,depth,lat,lon.
 * <br>[still true???]In a given file, there are multiple values of the outermost dimension (e.g., time), 
 * but THERE MUST BE JUST ONE value for the other dimensions (e.g., depth, lat, and lon).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-13
 */
public class EDDTableFromPostNcFiles extends EDDTableFromNcFiles { 

    /** 
     * The constructor calls the super constructor and loads the user and role table data
     * (to control data access). 
     *
     * <p>After construction, EDDTableCopyPost will call setSourceEdd
     *
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115File This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     */
    public EDDTableFromPostNcFiles(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows) 
        throws Throwable {

        super(tDatasetID, tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, 
            tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows);
        className = "EDDTableFromPostNcFiles";

    }


    /**
     * This returns a suggested fileName (no dir or extension).
     * It doesn't add a random number, so will return the same results 
     * if the inputs are the same.
     *
     * @param loggedInAs is only used for POST datasets (which overwrite EDD.suggestFileName)
     *    since loggedInAs is used by POST for row-by-row authorization
     * @param userDapQuery
     * @param fileTypeName
     * @return a suggested fileName (no dir or extension)
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
     * (see https://en.wikipedia.org/wiki/SQL_injection) by using
     * preparedStatements (so String values are properly escaped and
     * numeric values are assured to be numeric values).
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

        userDapQuery = EDDTableFromPostDatabase.reviseDapQueryForPost(loggedInAs, userDapQuery);

        //then get the data
        super.getDataForDapQuery(loggedInAs, requestUrl, userDapQuery, tableWriter);
    }

    /**
     * getDataForDapQuery always calls this right before standardizeResultsTable.
     * EDDTableFromPostNcFiles uses this to remove data not accessible to this user.
     */
    public void preStandardizeResultsTable(String loggedInAs, Table table) {
        EDDTableFromPostDatabase.staticPreStandardizeResultsTable(loggedInAs, table);
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        //usually run
        //see test in EDDTableCopyPost
        //...

        //not usually run
        //test24Hours();  //requires special set up
    }
}

