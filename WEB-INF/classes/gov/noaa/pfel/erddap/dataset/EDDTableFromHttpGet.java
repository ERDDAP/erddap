/* 
 * EDDTableFromHttpGet Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.time.*;
import java.time.format.*;

/**
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents a table of data from a collection of 1-dimensional .nc data files
 * which are created by HTTP GET calls to ERDDAP.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2016-06-14
 */
public class EDDTableFromHttpGet extends EDDTableFromFiles { 

    //special column names   //DON'T EVER CHANGE ANY OF THESE!!!
    public final static String TIMESTAMP = "timestamp"; //epSec as double
    public final static String AUTHOR    = "author";    //String
    public final static int    AUTHOR_STRLEN = 16;      // 
    public final static String COMMAND   = "command";   //byte
    public final static byte INSERT_COMMAND = 0;
    public final static byte DELETE_COMMAND = 1;

    private HashSet<String> keys = new HashSet();
    private String[] sortedColumnSourceNames;

    //this has the parsed directoryStructure specification
    //with 1 item per directory and the last item being for the file names
    private String dirStructureColumnNames[]; //[i] has a var sourceName or ""
    private int    dirStructureNs[];          //[i] has the number of Calendar items, or -1
    private int    dirStructureCalendars[];   //[i] has the e.g., Calendar.MONTH, or -1


    /** 
     * The constructor just calls the super constructor. 
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
    public EDDTableFromHttpGet(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, 
        int tColumnNamesRow, int tFirstDataRow, String tColumnSeparator,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows) 
        throws Throwable {

        super("EDDTableFromHttpGet", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow, tColumnSeparator,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows);

    }

    /** 
     * The constructor for subclasses.
     */
    public EDDTableFromHttpGet(String tClassName, 
        String tDatasetID, String tAccessibleTo, String tGraphsAccessibleTo,
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

        super(tClassName, tDatasetID, tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows);

        //get/remove key's from global metadata
        String attName = "HttpGetKeys";
        String keyAr[] = StringArray.arrayFromCSV( //may be length=0
            combinedGlobalAttributes.getString(attName));
        sourceGlobalAttributes.remove(  attName);
        addGlobalAttributes.remove(     attName);
        combinedGlobalAttributes.remove(attName);
        //create temporary hashset
        HashSet<String> tKeys = new HashSet();
        for (int i = 0; i < keyAr.length; i++) {
            if (String2.isSomething(keyAr[i]))
                tKeys.add(String2.canonical(keyAr[i]));
        }
        if (tKeys.size() == 0)
            throw new SimpleException(String2.ERROR + " in constructor: " +
                attName + " global attribute wasn't specified.");
        //then swap into place atomically
        keys = tKeys;

        //no column sourceName can be COLUMN
//        ...
    }


    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     *
     * @throws Throwable if too much data.
     *  This won't throw an exception if no data.
     */
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        return readFile(fileDir + fileName, Double.MAX_VALUE, sortedColumnSourceNames);
    }


    /**
     * This gets the data from one file up to and including the specified timestamp value.
     */
    public static Table readFile(String fullFileName, 
        double timestampMillis,
        String[] tSortedColumns) throws Throwable {

        //read all columns of the file (so UPDATE's and DELETE's WHERE can be processed)
        Table oldTable = new Table();
        oldTable.readFlatNc(fullFileName, null, 0);  //load all vars, don't unpack
        //String2.log("  EDDTableFromHttpGet.lowGetSourceDataFromFile table.nRows=" + table.nRows());
        //table.saveAsDDS(System.out, "s");

        //make a new Table with a shallow copy of the oldTable's metadata and columns 
        int onCols = oldTable.nColumns();
        int onRows = oldTable.nRows();
        Table newTable = new Table();
        newTable.globalAttributes().add(oldTable.globalAttributes());  
        PrimitiveArray oldPAs[] = new PrimitiveArray[onCols];
        PrimitiveArray newPAs[] = new PrimitiveArray[onCols];
        DoubleArray timestampPA = null;
        ByteArray commandPA = null;
        int tCol = 0;
        for (int col = 0; col < onCols; col++) {
            PrimitiveArray pa = oldTable.getColumn(col);
            String colName = oldTable.getColumnName(col);

            //grab the TIMESTAMP and COMMAND columns
            if (colName.equals(TIMESTAMP)) {
                timestampPA = (DoubleArray)pa;
                continue;
            }
            if (colName.equals(COMMAND)) {
                commandPA = (ByteArray)pa;
                continue;
            }
            
            //insert all the others in newTable
            oldPAs[tCol] = pa;
            newPAs[tCol] = PrimitiveArray.factory(pa.elementClass(), onRows, false);
            newTable.addColumn(tCol++, //increment
                colName, newPAs[col], 
                oldTable.columnAttributes(col)); //not a clone
        }
        if (timestampPA == null ||
            commandPA   == null) 
            throw new SimpleException(
                String2.ERROR + " while reading " + fullFileName + ": " +
                "columnName=" + 
                (timestampPA == null? TIMESTAMP : commandPA) + 
                " not found.");

        //remove rows with timestamp > row's timestamp
//        if () {
//        }

        //sort based on sortedCols+timestamp

        //just keep last of each group of rows where sortedCols is same
        //  and last row is INSERT (not DELETE)
        BitSet justKeep = new BitSet();
        for (int row = 0; row < onRows; row++) {
//
        }
        newTable.justKeep(justKeep);

        

        return newTable;
    }


    /**
     * This parses the directoryStructure specification.
     *
     * @param specification e.g, stationID/10years/7days
     * @param dsSourceName will be filled, with [i] = a var sourceName or "".
     *    sourceNames haven't been tested to see if they are in the dataset.
     * @param dsN          will be filled, with [i] = the number of Calendar items, or -1
     * @param dsCalendar will be filled, with [i] = the e.g., Calendar.MONTH, or -1
     * @throws RuntimeException if trouble
     */ 
    public static void parseDirectoryStructure(String specification, 
         StringArray dsSourceName, IntArray dsN, IntArray dsCalendar) throws Exception {

         dsSourceName.clear();
         dsN.clear();
         dsCalendar.clear();
         String parts[] = String2.split(specification, '/');
         Pattern pattern = Pattern.compile("(\\d+)([A-Za-z]+)");
         for (int p = 0; p < parts.length; p++) {
             Matcher matcher = pattern.matcher(parts[p]);
             if (matcher.matches()) {
                 //e.g., 5days
                 String units = matcher.group(2);
                 int cal = Calendar2.unitsToConstant(units); //throws exception
                 if (cal == Calendar.WEEK_OF_YEAR)
                     throw new RuntimeException(String2.ERROR + " parsing directoryStructure: " +
                         "units=" + units + " is invalid.");
                 dsSourceName.add("");
                 dsN.add(         String2.parseInt(matcher.group(1)));
                 dsCalendar.add(  cal);
             } else {
                 dsSourceName.add(parts[p]);
                 dsN.add(         -1);
                 dsCalendar.add(  -1);
             }
         }
     }


    /**
     * This figures out the name of the relevant file (which may or 
     * may not exist.
     *
     * @param startDir with trailing slash
     * @param tDirStructureColumnNames For each part, the variable's source column name
     *   or "" if not used for this part.
     *   Any column names here should be in requiredColumnNames.
     * @param tDirStructureNs     For each part, the number of YEAR, MONTH, ...
     *   or -1 if not used for this part
     * @param tDirStructureCalendars  For each part, 
     *   Calendar.YEAR, MONTH, DATE, HOUR_OF_DAY, MINUTE, SECOND, MILLISECOND, 
     *   or -1 if not used for this part
     * @param tColumnNames  the source names of the columns
     * @param tColumnValues the source values associated with the tColumnNames.
     *    All should have the same size().
     *    Only values on the specified row will be used.
     * @param row the value of the rightmost array of tColumnSourceValues
     * @param timeEpSec the time value, in epoch seconds.
     *   It is usually a requiredColumn, but not always.
     *   It is an error if it is needed here, but timeEpSec is NaN.
     * @return the full file dir+name, starting with startDir.
     */
    public static String whichFile(String startDir, 
        StringArray tDirStructureColumnNames, 
        IntArray tDirStructureNs, IntArray tDirStructureCalendars,
        String tColumnNames[], PrimitiveArray tColumnValues[], int row,
        double timeEpSec) {
        
        StringBuilder dirSB = new StringBuilder(startDir);
        StringBuilder nameSB = new StringBuilder();
        int nParts = tDirStructureColumnNames.size();
        for (int i = 0; i < nParts; i++) {
            if (i > 0) {
                dirSB.append('/');
                nameSB.append('_');
            }
            int cal = tDirStructureCalendars.get(i);
            if (cal == -1) {
                //Find the var. Add its value.
                int sni = String2.indexOf(tColumnNames, tDirStructureColumnNames.get(i));
                if (sni < 0)
                    throw new SimpleException(
                        String2.ERROR + " in directoryStructure part#" + i + 
                        ": column=" + tDirStructureColumnNames.get(i) + 
                        " isn't in columnNames=" + String2.toCSSVString(tColumnNames) + 
                        ".");
                //data value of "" is a valid value. It will be converted to something.
                String tp = String2.encodeFileNameSafe(tColumnValues[sni].getString(row)); 
                if (i < nParts - 1)
                    dirSB.append(tp);
                nameSB.append(tp);                

            } else {
                //Find the time part. Round down to n'th precision. 
                //e.g., 17 seconds to 5seconds precision is 15 seconds.
                //(MONTH is 0-based, so that works correctly as is.)
                if (!Double.isFinite(timeEpSec)) 
                    throw new SimpleException(
                        String2.ERROR + " in directoryStructure part#" + i + 
                        ": time value is NaN!");
                //need a new gc for each part since gc is modified
                GregorianCalendar gc = Calendar2.epochSecondsToGc(timeEpSec); 
                int n = tDirStructureNs.get(i);
                gc.set(cal, (gc.get(cal) / n) * n);
                //Get the ISO 8601 date/time string just to that precision/field.
                String s = Calendar2.formatAsISODateTimeT3(gc); //to millis 
                int nChar = s.length();
                if      (cal == Calendar.YEAR)        nChar = 4;
                else if (cal == Calendar.MONTH)       nChar = 7;
                else if (cal == Calendar.DATE)        nChar = 10;
                else if (cal == Calendar.HOUR_OF_DAY) nChar = 13;
                else if (cal == Calendar.MINUTE)      nChar = 16;
                else if (cal == Calendar.SECOND)      nChar = 19;
                //else to millis precision
                String tp = s.substring(0, nChar);  
                tp = String2.replaceAll(tp, ':', '-'); //make fileNameSafe
                if (i < nParts - 1)
                    dirSB.append(tp);
                nameSB.append(tp);                
            }
        }

        return dirSB.toString() + nameSB.toString() + ".nc";
    }

    /** 
     * This is used to add insert or delete commands into a data file of this dataset. 
     * This is EDDTableFromHttpGet overwriting the default implementation.
     *
     * <p>The key should be author_secret. So keys are specific to specific people/actors.
     * The author will be kept and added to the 'author' column in the dataset.
     *
     * <p>INSERT works like SQL's INSERT and UPDATE.
     * If the info matches existing values of sortColumnSourceNames,
     * the previous data is updated/overwritten. Otherwise, it is inserted.
     *
     * <p>DELETE works like SQL's DELETE
     *
     * @param tDirStructureColumnNames the column names for the parts of the 
     *   dir and file names. All of these names must be in requiredColumnNames.
     * @param keys the valid values of author= (to authenticate the author)
     * @param columnNames the names of all of the dataset's source variables.
     *   This does not include timestamp, author, or command.
     *   The time variable must be named time.
     * @param columnUnits any of them may be null or "".
     *   All timestamp columns (in the general sense) should have UDUNITS 
     *   String time units (e.g., "yyyy-MM-dd'T'HH:mm:ss") 
     *   or numeric time units (e.g., "days since 1985-01-01").
     *   For INSERT and DELETE calls, the time values must be in that format
     *   (you can't revert to ISO 8601 format as with data requests in the rest of ERDDAP).
     * @param columnTypes the Java names for the types (e.g., double).
     *   The missing values are the default missing values for PrimitiveArrays.
     *   All timestamp columns MUST be doubles.
     *   'long' is not supported because .nc3 files don't support longs.
     * @param columnStringLengths -1 if not a string column.
     * @param requiredColumnNames the names which identify a unique row.
     *   RequiredColumnNames MUST all be in columnNames.
     *   Insert requests MUST have all of the requiredColumnNames and usually have all 
     *     columnNames + author. Missing columns will get (standard PrimitiveArray) 
     *     missing values.
     *   Delete requests MUST have all of the requiredColumnNames and, in addition,
     *     usually have just author. Other columns are irrelevant.
     *   This should be as minimal as possible, and always includes time:  
     *   For TimeSeries: stationID, time.
     *   For Trajectory: trajectoryID, time.
     *   For Profile: stationID, time, depth.
     *   For TimeSeriesProfile: stationID, time, depth.
     *   For TrajectoryProfile: trajectoryID, time, depth.
     * @param command INSERT_COMMAND or DELETE_COMMAND
     * @param userDapQuery the param string, still percent-encoded
     * @param dirTable  a copy of the dirTable  (changes may be made to it) or null.
     * @param fileTable a copy of the fileTable (changes may be made to it) or null.
     * @return the response string 
     * @throws Throwable if any kind of trouble
     */
    public static String insertOrDelete(String startDir, 
        StringArray tDirStructureColumnNames, 
        IntArray tDirStructureNs, IntArray tDirStructureCalendars,
        HashSet<String> keys,
        String columnNames[], String columnUnits[], String columnTypes[], 
        int columnStringLengths[], 
        String requiredColumnNames[],
        byte command, String userDapQuery,
        Table dirTable, Table fileTable) throws Throwable {

        double timestamp = System.currentTimeMillis() / 1000.0;
        if (dirTable == null || fileTable == null) { //ensure both or neither
            dirTable = null;
            fileTable = null;
        }

        //store values parallelling columnNames
        int nColumns = columnNames.length;
        PrimitiveArray columnValues[] = new PrimitiveArray[nColumns];
        Class columnClasses[] = new Class[nColumns];
        DataType columnDataTypes[] = new DataType[nColumns];
        boolean columnIsString[] = new boolean[nColumns];
        int timeColumn = -1;         
        DateTimeFormatter timeFormatter = null; //used if time variable is string
        double timeBaseAndFactor[] = null;      //used if time variable is numeric
        for (int col = 0; col < nColumns; col++) {
            if (!String2.isSomething(columnUnits[col]))
                columnUnits[col] = "";

            if (columnNames[col].equals(EDV.TIME_NAME)) {
                timeColumn = col;
                if (columnIsString[col]) {
                    if (columnUnits[col].toLowerCase().indexOf("yyyy") < 0)  //was "yy"
                        throw new SimpleException(EDStatic.queryError + 
                            "Invalid units for the string time variable. " +
                            "Units MUST specify the format of the time values.");
                    timeFormatter = DateTimeFormat.forPattern(columnUnits[col]).withZone(ZoneId.of("UTC"));
                } else { //numeric time values
                    timeBaseAndFactor = Calendar2.getTimeBaseAndFactor(
                        columnUnits[col]); //throws RuntimeException if trouble
                }
            }

            if (columnTypes[col].equals("String")) {
                columnClasses[col] = String.class;
                columnDataTypes[col] = DataType.STRING;
                columnIsString[col] = true;
                if (columnStringLengths[col] < 1 || columnStringLengths[col] > 64000)
                    throw new SimpleException(EDStatic.queryError + 
                        "Invalid string length=" + columnStringLengths[col] + 
                        " for column=" + columnNames[col] + ".");
            } else {
                columnClasses[col] = PrimitiveArray.elementStringToClass(columnTypes[col]);
                columnDataTypes[col] = NcHelper.getDataType(columnClasses[col]);
            }
        }

        //parse the userDapQuery's parts. Ensure it is valid. 
        String parts[] = String2.split(userDapQuery, '&');
        int nParts = parts.length;
        String author = null; //the part before '_'
        int arraySize = -1; //until an array is found
        BitSet requiredColumnsFound = new BitSet();
        for (int p = 0; p < nParts; p++) {
            parts[p] = SSR.percentDecode(parts[p]);
            int eqPo = parts[p].indexOf('=');
            if (eqPo <= 0 || //no '=' or no name
                "<>~!".indexOf(parts[p].charAt(eqPo-1)) >= 0) // <= >= != ~=
                throw new SimpleException(EDStatic.queryError + 
                    "The \"" + parts[p] + "\" parameter isn't in the form name=value.");
            String tName  = parts[p].substring(0, eqPo);
            String tValue = parts[p].substring(eqPo + 1);            
            if (tValue.startsWith("~")) // =~
                throw new SimpleException(EDStatic.queryError + 
                    "The \"" + parts[p] + "\" parameter isn't in the form name=value.");

            //catch and verify author=
            if (tName.equals(AUTHOR)) {
                if (author != null)
                    throw new SimpleException(EDStatic.queryError + 
                        "There are two parameters with name=author.");
                if (!keys.contains(tValue))
                    throw new SimpleException(EDStatic.queryError + 
                        "Invalid author_key.");
                if (p != nParts - 1)
                    throw new SimpleException(EDStatic.queryError + 
                        "name=author must be the last parameter.");
                int po = Math.max(0, tValue.indexOf('_'));
                author = tValue.substring(0, po); //may be ""

            } else { 
                //is it a requiredColumn?
                int whichRC = String2.indexOf(requiredColumnNames, tName);
                if (whichRC >= 0)
                    requiredColumnsFound.set(whichRC);

                //whichColumn? 
                int whichCol = String2.indexOf(columnNames, tName);
                if (whichCol < 0)
                    throw new SimpleException(EDStatic.queryError + 
                        "Unknown columnName=" + tName);
                if (columnValues[whichCol] != null) 
                    throw new SimpleException(EDStatic.queryError + 
                        "There are two parameters with columnName=" + tName + "."); 

                //get the values
                if (tValue.startsWith("[") &&
                    tValue.endsWith(  "]")) {
                    //deal with array of values: name=[valuesCSV]
                    columnValues[whichCol] = PrimitiveArray.csvFactory(
                        columnClasses[whichCol], tValue);
                    if (arraySize < 0)
                        arraySize = columnValues[whichCol].size();
                    else if (arraySize != columnValues[whichCol].size())
                        throw new SimpleException(EDStatic.queryError + 
                            "Different parameters with arrays have different sizes: " +
                            arraySize + "!=" + columnValues[whichCol].size() + ".");

                } else {
                    //deal with single value: name=value
                    columnValues[whichCol] = PrimitiveArray.csvFactory(
                        columnClasses[whichCol], tValue);

                    if (columnClasses[whichCol] == String.class &&
                        (tValue.length() < 2 || 
                         tValue.charAt(0) != '"' ||
                         tValue.charAt(tValue.length() - 1) != '"'))
                        throw new SimpleException(EDStatic.queryError + 
                            "The String value for columnName=" + tName + 
                            " must start and end with \"'s.");
                    if (columnValues[whichCol].size() != 1)
                        throw new SimpleException(EDStatic.queryError + 
                            "One value (not " + columnValues[whichCol].size() +
                            ") expected for columnName=" + tName + ". (missing [ ] ?)");
                }
            }
        }

        //ensure required parameters were specified 
        if (author == null)
            throw new SimpleException(EDStatic.queryError + 
                "author= was not specified.");
        int notFound = requiredColumnsFound.nextClearBit(0);
        if (notFound < requiredColumnNames.length)
            throw new SimpleException(EDStatic.queryError + 
                "requiredColumnName=" + requiredColumnNames[notFound] + 
                " wasn't specified.");

        //make all columnValues the same size
        //(timestamp, author, command are separate and have just 1 value)
        int maxSize = Math.max(1, arraySize);
        for (int col = 0; col < nColumns; col++) {
            PrimitiveArray pa = columnValues[col]; 
            if (pa == null) {
                //this var wasn't in the command, so use mv's
                columnValues[col] = PrimitiveArray.factory(columnClasses[col],
                    maxSize, "");
            } else if (pa.size() == 1 && maxSize > 1) {
                columnValues[col] = PrimitiveArray.factory(columnClasses[col],
                    maxSize, pa.getString(0));
            }
        }

        //figure out the fullFileName for each row
        StringArray fullFileNames = new StringArray(maxSize, false);
        for (int row = 0; row < maxSize; row++) { 
            //figure out the epochSeconds time value
            double tTime = 
                timeColumn < 0? Double.NaN :                           //no time column
                timeBaseAndFactor == null? Calendar2.toEpochSeconds(
                    columnValues[timeColumn].getString(row), timeFormatter) : 
                Calendar2.unitsSinceToEpochSeconds(                    //numeric time
                    timeBaseAndFactor[0], timeBaseAndFactor[1], 
                    columnValues[timeColumn].getDouble(row));

            fullFileNames.add(whichFile(startDir, 
                tDirStructureColumnNames, tDirStructureNs, tDirStructureCalendars,
                columnNames, columnValues, row, tTime)); 
        }

        //EVERYTHING SHOULD BE VALIDATED BY NOW. NO ERRORS AFTER HERE!
        //append each input row to the appropriate file
        Array oneTimestampArray = Array.factory(new double[]{timestamp});
//I reported to netcdf-java mailing list: this generated null pointer exception in 4.6.6:
// String tsar[] = new String[]{author};
// Array oneAuthorArray    = Array.factory(tsar); //new String[]{author});
//This works:
ArrayString.D1 oneAuthorArray = new ArrayString.D1(1);
oneAuthorArray.set(0, author);

        Array oneCommandArray   = Array.factory(new byte  []{command});
        int row = 0;
        while (row < maxSize) {
            //figure out which file
            String fullFileName = fullFileNames.get(row);

            //open the file
            NetcdfFileWriter file = null;
            boolean fileIsNew = false;
            int[] origin = new int[1];    
            try {

                Group rootGroup = null;
                Dimension rowDim = null;
                Variable vars[] =  new Variable[nColumns];
                Variable timestampVar = null;
                Variable authorVar    = null;
                Variable commandVar   = null;
                if (File2.isFile(fullFileName)) {
                    file = NetcdfFileWriter.openExisting(fullFileName);
                    rootGroup = file.addGroup(null, "");
                    rowDim = rootGroup.findDimension("row");

                    //find Variables for columnNames.   May be null, but shouldn't be.
                    StringArray columnsNotFound = new StringArray();
                    for (int col = 0; col < nColumns; col++) { 
                        vars[col] = rootGroup.findVariable(columnNames[col]); 
                        if (vars[col] == null) 
                            columnsNotFound.add(columnNames[col]);
                    }
                    timestampVar  = rootGroup.findVariable(TIMESTAMP); 
                    authorVar     = rootGroup.findVariable(AUTHOR); 
                    commandVar    = rootGroup.findVariable(COMMAND); 
                    if (timestampVar == null) columnsNotFound.add(TIMESTAMP);
                    if (authorVar    == null) columnsNotFound.add(AUTHOR);
                    if (commandVar   == null) columnsNotFound.add(COMMAND);
                    if (columnsNotFound.size() > 0)
                        throw new SimpleException(MustBe.InternalError + 
                            ": column(s)=" + columnsNotFound + 
                            " not found in " + fullFileName);

                } else {
                    //if file doesn't exist, create it
                    fileIsNew = true; //first
                    file = NetcdfFileWriter.createNew(
                        NetcdfFileWriter.Version.netcdf3, fullFileName);
                    rootGroup = file.addGroup(null, "");
                    rowDim = file.addUnlimitedDimension("row");
                    ArrayList rowDimAL = new ArrayList();
                    rowDimAL.add(rowDim);

                    //define Variables
                    for (int col = 0; col < nColumns; col++) {
                        String cName = columnNames[col];
                        String cType = columnTypes[col];
                        if (columnIsString[col]) {
                            vars[col] = file.addStringVariable(rootGroup, cName, 
                                rowDimAL, columnStringLengths[col]);
                        } else {
                            vars[col] = file.addVariable(rootGroup, cName, 
                                columnDataTypes[col], rowDimAL);
                        }
                    }
                    timestampVar = file.addVariable(rootGroup, TIMESTAMP, 
                        DataType.DOUBLE, rowDimAL);
                    authorVar = file.addStringVariable(rootGroup, AUTHOR, 
                        rowDimAL, AUTHOR_STRLEN);
                    commandVar = file.addVariable(rootGroup, COMMAND, 
                        DataType.BYTE, rowDimAL);

                    // create the file
                    file.create();
                }

                //append the series of commands that go to this fullFileName
                int startRow = row++;
                while (row < maxSize && 
                    fullFileNames.get(row).equals(fullFileName))
                    row++;
                int stopRow = row; //1 past end

                //which row in the file table?
                int fileTableRow = -1;
                if (fileTable != null) {
                    //already in fileTable?
                    //fileTableRow = ...

                    //add to fileTable
                }

                //write the data to the file
                origin[0] = rowDim.getLength();
                for (int col = 0; col < nColumns; col++) {
                    PrimitiveArray subsetPA = columnValues[col];
                    if (startRow > 0 || stopRow != maxSize)
                        subsetPA = subsetPA.subset(startRow, 1, stopRow-1); //inclusive
                    file.write(vars[col], origin, Array.factory(subsetPA.toObjectArray()));

                    //adjust min/max in fileTable
                    if (fileTable != null && command == INSERT_COMMAND) {
                        if (columnIsString[col]) {
                            //fileTableRow...   
                        } else {
                            double stats[] = subsetPA.calculateStats();
                            if (stats[PrimitiveArray.STATS_N] > 0) {  //has some non MVs
                                //fileTableRow... Math.min(  , stats[PrimitiveArray.STATS_MIN]));
                                //fileTableRow....Math.max(  , stats[PrimitiveArray.STATS_MAX]));
                            }
                            if (stats[PrimitiveArray.STATS_N] < stopRow-startRow) {
                                //fileTableRow... hasMV
                            }
                        }
                    }
                }
                Array timestampArray = oneTimestampArray;
                Array authorArray    = oneAuthorArray;
                Array commandArray   = oneCommandArray;
                if (stopRow - startRow > 1) {
                    //double timestampAr[] = new double[stopRow - startRow]; 
                    //String authorAr[]    = new String[stopRow - startRow];
                    //byte   commandAr[]   = new byte  [stopRow - startRow];
                    //Arrays.fill(timestampAr, timestamp);
                    //Arrays.fill(authorAr,    author);
                    //Arrays.fill(commandAr,   command);
                    //timestampArray = Array.factory(timestampAr);
                    //authorArray    = Array.factory(authorAr);
                    //commandArray   = Array.factory(commandAr);

                    int thisShape[] = new int[]{stopRow - startRow};
                    timestampArray = Array.factoryConstant(double.class, thisShape, new Double(timestamp));
                    authorArray    = Array.factoryConstant(String.class, thisShape, author);
                    commandArray   = Array.factoryConstant(byte.class,   thisShape, new Byte(command));
                }
                file.write(          timestampVar, origin, timestampArray);
                file.writeStringData(authorVar,    origin, authorArray);
                file.write(          commandVar,   origin, commandArray);
                
                //adjust min/max in fileTable
                if (fileTable != null && command == INSERT_COMMAND) {
                    //fileTableRow... Math.min(   , timestamp));
                    //fileTableRow....Math.max(   , timestamp));

                    //fileTableRow... Math.min(   , author));
                    //fileTableRow....Math.max(   , author));

                    //fileTableRow... Math.min(   , command));
                    //fileTableRow....Math.max(   , command));
                }

                //make it so!
                file.flush(); //force file update

                //close the file
                file.close();
                file = null;

            } catch (Throwable t) {
                if (file != null) {
                    try {file.close();} catch (Throwable t2) {}
                }
                if (fileIsNew)
                    File2.delete(fullFileName);
                String2.log(String2.ERROR + " while " +
                    (fileIsNew? "creating" : "adding to") +
                    " " + fullFileName);
                throw t;
            }
        }

        //Don't ever change any of this (except adding somthing new to the end). 
        //Clients rely on it.
        return "SUCCESS: Data received. No errors. timestamp=" + 
            Calendar2.epochSecondsToIsoStringT3(timestamp) + "Z=" +
            timestamp + " seconds since 1970-01-01T00:00:00Z.\n"; 
    }


    /**
     * This tests the static methods in this class.
     */
    public static void testStatic() throws Throwable {
        String2.log("\n*** EDDTableFromHttpGet.testStatic");
        String results, expected;

        //test parseDirectoryStructure
        StringArray dsColumnName = new StringArray();
        IntArray    dsN          = new IntArray();
        IntArray    dsCalendar   = new IntArray();
        parseDirectoryStructure("5years/3MonTH/4DayS/5hr/6min/7sec/100millis/stationID", 
            dsColumnName, dsN, dsCalendar);
        Test.ensureEqual(dsColumnName.toString(), ", , , , , , , stationID", "");
        Test.ensureEqual(dsN.toString(),          "5, 3, 4, 5, 6, 7, 100, -1", "");
        Test.ensureEqual(dsCalendar.toString(), 
            String2.toCSSVString(new int[]{Calendar.YEAR, Calendar.MONTH, Calendar.DATE, 
                Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, -1}), 
            "");

        //test whichFile
        Test.ensureEqual(
            whichFile("/ab/", dsColumnName, dsN, dsCalendar,
                new String[]{"junk", "stationID"}, //sourceNames
                                         //row0    row1
                new PrimitiveArray[]{new StringArray(new String[]{"junk0", "junk1"}),  //junk sourceValues
                                     new StringArray(new String[]{"12345", "46088"})}, //stationID sourceValues
                1, //row
                Calendar2.isoStringToEpochSeconds("2016-06-21T14:15:16.789")),
            "/ab/2015/2016-04/2016-06-20/2016-06-21T10/2016-06-21T14-12/" +
            "2016-06-21T14-15-14/2016-06-21T14-15-16.700/" +
                "2015_2016-04_2016-06-20_2016-06-21T10_2016-06-21T14-12_" +
            "2016-06-21T14-15-14_2016-06-21T14-15-16.700_46088.nc", "");

        //set up 
        String startDir = EDStatic.fullTestCacheDirectory + "/httpGet/";
        File2.deleteAllFiles(startDir, true, true); //recursive, deleteEmptySubdirectories
        parseDirectoryStructure("stationID/2months", dsColumnName, dsN, dsCalendar);
        HashSet<String> keys = new HashSet();
        keys.add("bsimons_aSecret");
        String columnNames[] = {         "stationID", "time",                 
            "aByte",     "aChar",        "aShort",    "anInt",                 
            "aFloat",    "aDouble",      "aString"};
        String columnUnits[] = {         "",          "days since 1980-01-01", 
            "",          "",             "m",         "days since 1985-01-01", 
            "degree_C",  EDV.TIME_UNITS, null};
        String columnTypes[] = {         "String",    "int",               
            "byte",      "char",         "short",     "int", 
            "float",     "double",       "String"}; 
        int columnStringLengths[] = {    5,           -1,      
            -1,          -1,             -1,          -1,
            -1,          -1,             12};
        String requiredColumnNames[] = {"stationID","time"};
        Table table;
        
        //test insertOrDelete
        results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
            keys, columnNames, columnUnits, columnTypes, columnStringLengths, 
            requiredColumnNames,
            INSERT_COMMAND, 
            "stationID=\"46088\"&time=3&aByte=17.1&aChar=g" +
            "&aShort=30000.1&anInt=2&aFloat=1.23" +
            "&aDouble=1.2345678901234&aString=\"abcdefghijklmnop\"" + //string is too long
            "&author=bsimons_aSecret",
            null, null); //Table dirTable, Table fileTable
        Test.ensureEqual(results, "zztop", "results=" + results);

        //read the data
        table = readFile(startDir + "46088/1980-01/46088_1980-01.nc", 
            Double.MAX_VALUE, requiredColumnNames);
        results = table.dataToString();
        expected = "zztop";
        Test.ensureEqual(results, expected, "results=" + results);


        
     }



    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromHttpGet.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (usually ".*\\.nc").
     * @param sampleFileName the full file name of one of the files in the collection
     * @param useDimensionsCSV If null or "", this finds the group of variables sharing the
     *    highest number of dimensions. Otherwise, it find the variables using
     *    these dimensions (plus related char variables).
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        String useDimensionsCSV, int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDTableFromHttpGet.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tFileNameRegex +
            "\nsampleFileName=" + sampleFileName +
            "\nuseDimensionsCSV=" + useDimensionsCSV + 
            " reloadEveryNMinutes=" + tReloadEveryNMinutes +
            "\nextract pre=" + tPreExtractRegex + " post=" + tPostExtractRegex + " regex=" + tExtractRegex +
            " colName=" + tColumnNameForExtract +
            "\nsortedColumn=" + tSortedColumnSourceName + 
            " sortFilesBy=" + tSortFilesBySourceNames + 
            "\ninfoUrl=" + tInfoUrl + 
            "\ninstitution=" + tInstitution +
            "\nsummary=" + tSummary +
            "\ntitle=" + tTitle +
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);

        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        String[] useDimensions = StringArray.arrayFromCSV(useDimensionsCSV);
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //show structure of sample file
        String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
        String2.log(NcHelper.dumpString(sampleFileName, false));

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        //new way
        StringArray varNames = new StringArray();
        if (useDimensions.length > 0) {
            //find the varNames
            NetcdfFile ncFile = NcHelper.openFile(sampleFileName);
            try {

                Group rootGroup = ncFile.getRootGroup();
                List rootGroupVariables = rootGroup.getVariables(); 
                for (int v = 0; v < rootGroupVariables.size(); v++) {
                    Variable var = (Variable)rootGroupVariables.get(v);
                    boolean isChar = var.getDataType() == DataType.CHAR;
                    if (var.getRank() + (isChar? -1 : 0) == useDimensions.length) {
                        boolean matches = true;
                        for (int d = 0; d < useDimensions.length; d++) {
                            if (!var.getDimension(d).getFullName().equals(useDimensions[d])) {
                                matches = false;
                                break;
                            }
                        }
                        if (matches) 
                            varNames.add(var.getFullName());
                    }
                }
                ncFile.close(); 

            } catch (Exception e) {
                //make sure ncFile is explicitly closed
                try {
                    ncFile.close(); 
                } catch (Exception e2) {
                    //don't care
                }
                String2.log(MustBe.throwableToString(e)); 
            }
            Test.ensureTrue(varNames.size() > 0, 
                "The file has no variables with dimensions: " + useDimensionsCSV);
        }

        //then read the file
        dataSourceTable.readNDNc(sampleFileName, varNames.toStringArray(), 
            null, 0, 0, true); //getMetadata
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            dataAddTable.addColumn(c, colName,
                makeDestPAForGDX(sourceAtts, dataSourceTable.getColumn(c)),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(), sourceAtts, null, 
                    colName, true, true)); //addColorBarMinMax, tryToFindLLAT

            //if a variable has timeUnits, files are likely sorted by time
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && 
                EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                tSortedColumnSourceName = colName;
        }
        //String2.log("SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
        //String2.log("DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", 
            "(" + (String2.isRemote(tFileDir)? "remote" : "local") + " files)");

        //tryToFindLLAT
        tryToFindLLAT(dataSourceTable, dataAddTable);

        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //subsetVariables
        if (dataSourceTable.globalAttributes().getString("subsetVariables") == null &&
               dataAddTable.globalAttributes().getString("subsetVariables") == null) 
            externalAddGlobalAtts.add("subsetVariables",
                suggestSubsetVariables(dataSourceTable, dataAddTable, false)); 

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = (tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : ", " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromHttpGet\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + 
            "</updateEveryNMillis>\n" +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(suggestedRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 2 params: includeDataType, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }


    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                1440,
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", "", null) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromHttpGet",
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                "1440",
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", ""},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromHttpGet\" datasetID=\"ndbcMet_5df7_b363_ad99\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>C:/u00/data/points/ndbcMet/</fileDir>\n" +
"    <fileNameRegex>.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex>^.{5}</preExtractRegex>\n" +
"    <postExtractRegex>.{7}$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>stationID</columnNameForExtract>\n" +
"    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>stationID, TIME</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"cdm_data_type\">Station</att>\n" +
"        <att name=\"contributor_name\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"contributor_role\">Source of data.</att>\n" +
//2012-07-27 "Unidata Observation Dataset v1.0" should disappear soon
"        <att name=\"Conventions\">COARDS, CF-1.4, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"date_created\">2015-07-20Z</att>\n" + //changes
"        <att name=\"date_issued\">2015-07-20Z</att>\n" +  //changes
"        <att name=\"Easternmost_Easting\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">32.501</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">32.501</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"float\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"float\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">down</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">NOAA NDBC</att>\n" +
"        <att name=\"id\">NDBC_41004_met</att>\n" +
"        <att name=\"institution\">NOAA National Data Buoy Center and Participators in Data Assembly Center.</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither NOAA, NDBC, CoastWatch, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.4, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" +
"        <att name=\"NDBCMeasurementDescriptionUrl\">http://www.ndbc.noaa.gov/measdes.shtml</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"float\">32.501</att>\n" +
"        <att name=\"project\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"quality\">Automated QC checks with periodic manual QC</att>\n" +
"        <att name=\"source\">station observation</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"float\">32.501</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">The National Data Buoy Center (NDBC) distributes meteorological data from moored buoys maintained by NDBC and others. Moored buoys are the weather sentinels of the sea. They are deployed in the coastal and offshore waters from the western Atlantic to the Pacific Ocean around Hawaii, and from the Bering Sea to the South Pacific. NDBC&#39;s moored buoys measure and transmit barometric pressure; wind direction, speed, and gust; air and sea temperature; and wave energy spectra from which significant wave height, dominant wave period, and average wave period are derived. Even the direction of wave propagation is measured on many moored buoys. \n" +
"\n" + //changes 2 places...  date is old, but this is what's in the file
"This dataset has both historical data (quality controlled, before 2011-05-01T00:00:00) and near real time data (less quality controlled, from 2011-05-01T00:00:00 on).</att>\n" +
"        <att name=\"time_coverage_end\">2015-07-20T15:00:00Z</att>\n" + //changes
"        <att name=\"time_coverage_resolution\">P1H</att>\n" +
"        <att name=\"time_coverage_start\">1978-06-27T13:00:00Z</att>\n" +
"        <att name=\"title\">NOAA NDBC Standard Meteorological</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"float\">-79.099</att>\n" +
"    </sourceAttributes -->\n" +
cdmSuggestion() +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"institution\">NOAA NDBC and Participators in Data Assembly Center.</att>\n" +
"        <att name=\"keywords\">air, air_pressure_at_sea_level, air_temperature, altitude, APD, assembly, atmosphere,\n" +
"Atmosphere &gt; Air Quality &gt; Visibility,\n" +
"Atmosphere &gt; Altitude &gt; Planetary Boundary Layer Height,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Pressure Tendency,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Water Vapor &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, ATMP, average, BAR, boundary, buoy, center, control, data, depth, dew, dew point, dew_point_temperature, DEWP, dewpoint, direction, dominant, DPD, eastward, eastward_wind, GST, gust, height, identifier, LAT, latitude, layer, level, LON, longitude, measurements, meridional, meteorological, meteorology, MWD, national, ndbc, near, noaa, northward, northward_wind, nrt, ocean, oceans,\n" +
"Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" +
"Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" +
"Oceans &gt; Ocean Waves &gt; Swells,\n" +
"Oceans &gt; Ocean Waves &gt; Wave Period,\n" +
"participators, period, planetary, point, pressure, PTDY, quality, real, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, station_id, surface, surface waves, surface_altitude, swell, swells, swh, temperature, tendency, tendency_of_air_pressure, TIDE, time, vapor, VIS, visibility, visibility_in_air, water, wave, waves, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, WSPD, WSPU, WSPV, WTMP, WVHT, zonal</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>stationID</sourceName>\n" +
"        <destinationName>stationID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIME</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">2.678004E8 1.4374044E9</att>\n" + //changes
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"comment\">Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.5E9</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DEPTH</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">down</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"comment\">The depth of the station, nominally 0 (see station information for details).</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"positive\">down</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n" +
"            <att name=\"colorBarPalette\">TopographyDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LAT</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">32.501 32.501</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"comment\">The latitude of the station.</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LON</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-79.099 -79.099</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"comment\">The longitude of the station.</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WD</sourceName>\n" +
"        <destinationName>WD</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"actual_range\" type=\"shortList\">0 359</att>\n" +
"            <att name=\"comment\">Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.</att>\n" +
"            <att name=\"long_name\">Wind Direction</att>\n" +
"            <att name=\"missing_value\" type=\"short\">32767</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"            <att name=\"units\">degrees_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPD</sourceName>\n" +
"        <destinationName>WSPD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 26.0</att>\n" +
"            <att name=\"comment\">Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.</att>\n" +
"            <att name=\"long_name\">Wind Speed</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>GST</sourceName>\n" +
"        <destinationName>GST</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 33.9</att>\n" +
"            <att name=\"comment\">Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or two-minute period. The 5 or 8 second period can be determined by payload, See the Sensor Reporting, Sampling, and Accuracy section.</att>\n" +
"            <att name=\"long_name\">Wind Gust Speed</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">wind_speed_of_gust</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WVHT</sourceName>\n" +
"        <destinationName>WVHT</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 12.53</att>\n" +
"            <att name=\"comment\">Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Height</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DPD</sourceName>\n" +
"        <destinationName>DPD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 20.0</att>\n" +
"            <att name=\"comment\">Dominant wave period (seconds) is the period with the maximum wave energy. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Period, Dominant</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>APD</sourceName>\n" +
"        <destinationName>APD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 13.1</att>\n" +
"            <att name=\"comment\">Average wave period (seconds) of all waves during the 20-minute period. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Period, Average</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>MWD</sourceName>\n" +
"        <destinationName>MWD</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"actual_range\" type=\"shortList\">0 359</att>\n" +
"            <att name=\"comment\">Mean wave direction corresponding to energy of the dominant period (DOMPD). The units are degrees from true North just like wind direction. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Direction</att>\n" +
"            <att name=\"missing_value\" type=\"short\">32767</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_to_direction</att>\n" +
"            <att name=\"units\">degrees_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>BAR</sourceName>\n" +
"        <destinationName>BAR</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">976.5 1041.5</att>\n" +
"            <att name=\"comment\">Air pressure (hPa). (&#39;PRES&#39; on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).</att>\n" +
"            <att name=\"long_name\">Air Pressure</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">air_pressure_at_sea_level</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ATMP</sourceName>\n" +
"        <destinationName>ATMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-6.1 31.7</att>\n" +
"            <att name=\"comment\">Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.</att>\n" +
"            <att name=\"long_name\">Air Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WTMP</sourceName>\n" +
"        <destinationName>WTMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-6.1 32.2</att>\n" +
"            <att name=\"comment\">Sea surface temperature (Celsius). For sensor depth, see Hull Description.</att>\n" +
"            <att name=\"long_name\">SST</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DEWP</sourceName>\n" +
"        <destinationName>DEWP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-10.6 29.1</att>\n" +
"            <att name=\"comment\">Dewpoint temperature taken at the same height as the air temperature measurement.</att>\n" +
"            <att name=\"long_name\">Dewpoint Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">dew_point_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>VIS</sourceName>\n" +
"        <destinationName>VIS</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 58.1</att>\n" +
"            <att name=\"comment\">Station visibility (km, originally statute miles). Note that buoy stations are limited to reports from 0 to 1.9 miles.</att>\n" +
"            <att name=\"long_name\">Station Visibility</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">visibility_in_air</att>\n" +
"            <att name=\"units\">km</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PTDY</sourceName>\n" +
"        <destinationName>PTDY</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-3.1 3.8</att>\n" +
"            <att name=\"comment\">Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.</att>\n" +
"            <att name=\"long_name\">Pressure Tendency</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">tendency_of_air_pressure</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">3.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-3.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIDE</sourceName>\n" +
"        <destinationName>TIDE</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"comment\">The water level in meters (originally feet) above or below Mean Lower Low Water (MLLW).</att>\n" +
"            <att name=\"long_name\">Water Level</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">surface_altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n" +
"            <att name=\"ioos_category\">Sea Level</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPU</sourceName>\n" +
"        <destinationName>WSPU</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-17.9 21.0</att>\n" +
"            <att name=\"comment\">The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.</att>\n" +
"            <att name=\"long_name\">Wind Speed, Zonal</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">eastward_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPV</sourceName>\n" +
"        <destinationName>WSPV</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-25.0 20.9</att>\n" +
"            <att name=\"comment\">The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.</att>\n" +
"            <att name=\"long_name\">Wind Speed, Meridional</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">northward_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ID</sourceName>\n" +
"        <destinationName>ID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">The station identifier.</att>\n" +
"            <att name=\"long_name\">Station Identifier</att>\n" +
"            <att name=\"standard_name\">station_id</att>\n" +
"            <att name=\"units\">unitless</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"units\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //with one small change to addAttributes:
            results = String2.replaceAll(results, 
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n",
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
                "        <att name=\"cdm_data_type\">Other</att>\n");
            String2.log(results);

            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), "ndbcMet_5df7_b363_ad99", "");
            Test.ensureEqual(edd.title(), "NOAA NDBC Standard Meteorological", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "stationID, time, depth, latitude, longitude, WD, WSPD, GST, WVHT, " +
                "DPD, APD, MWD, BAR, ATMP, WTMP, DEWP, VIS, PTDY, TIDE, WSPU, WSPV, ID", 
                "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }

    }

    /**
     * This tests the methods in this class with a 1D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromHttpGet.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String dir = EDStatic.fullTestCacheDirectory;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        String id = "erdCinpKfmSFNH";
        if (deleteCachedDatasetInfo)
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromHttpGet 1D test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station Identifier\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -120.4, -118.4;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum -118.4;\n" +
"    Float64 colorBarMinimum -120.4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 32.8, 34.05;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 34.5;\n" +
"    Float64 colorBarMinimum 32.5;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float64 actual_range 5.0, 17.0;\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 4.89024e+8, 1.183248e+9;\n" +
"    String axis \"T\";\n" +
"    Float64 colorBarMaximum 1.183248e+9;\n" +
"    Float64 colorBarMinimum 4.89024e+8;\n" +
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
"  species_name {\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Species Name\";\n" +
"  }\n" +
"  size {\n" +
"    Int16 actual_range 1, 385;\n" +
"    String ioos_category \"Biology\";\n" +
"    String long_name \"Size\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD, Channel Islands National Park, National Park Service\";\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"id, longitude, latitude\";\n" +
"    String contributor_email \"David_Kushner@nps.gov\";\n" +
"    String contributor_name \"Channel Islands National Park, National Park Service\";\n" +
"    String contributor_role \"Source of data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"Roy.Mendelssohn@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_url \"http://www.pfel.noaa.gov\";\n" +
"    String date_created \"2008-06-11T21:43:28Z\";\n" +
"    String date_issued \"2008-06-11T21:43:28Z\";\n" +
"    Float64 Easternmost_Easting -118.4;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 34.05;\n" +
"    Float64 geospatial_lat_min 32.8;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -118.4;\n" +
"    Float64 geospatial_lon_min -120.4;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 17.0;\n" +
"    Float64 geospatial_vertical_min 5.0;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Channel Islands National Park, National Park Service\n" +
"2008-06-11T21:43:28Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" + //will be SWFSC when reprocessed
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

//+ " (local files)\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
expected =
"/tabledap/erdCinpKfmSFNH.das\";\n" +
"    String infoUrl \"http://www.nps.gov/chis/naturescience/index.htm\";\n" +
"    String institution \"CINP\";\n" +
"    String keywords \"Biosphere > Aquatic Ecosystems > Coastal Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,\n" +
"aquatic, atmosphere, biology, biosphere, channel, cinp, coastal, common, depth, ecosystems, forest, frequency, habitat, height, identifier, islands, kelp, marine, monitoring, name, natural, size, species, station, taxonomy, time\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.  National Park Service Disclaimer: The National Park Service shall not be held liable for improper or incorrect use of the data described and/or contained herein. These data and related graphics are not legal documents and are not intended to be used as such. The information contained in these data is dynamic and may change over time. The data are not better than the original sources from which they were derived. It is the responsibility of the data user to use the data appropriately and consistent within the limitation of geospatial data in general and these data in particular. The related graphics are intended to aid the data user in acquiring relevant data; it is not appropriate to use the related graphics as data. The National Park Service gives no warranty, expressed or implied, as to the accuracy, reliability, or completeness of these data. It is strongly recommended that these data are directly acquired from an NPS server and not indirectly through other sources which may have changed the data in some way. Although these data have been processed successfully on computer systems at the National Park Service, no warranty expressed or implied is made regarding the utility of the data on other systems for general or scientific purposes, nor shall the act of distribution constitute any such warranty. This disclaimer applies both to individual use of the data and aggregate use with other data.\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 34.05;\n" +
"    String observationDimension \"row\";\n" + //2012-07-27 this should disappear soon
"    String project \"NOAA NMFS SWFSC ERD (http://www.pfel.noaa.gov/)\";\n" +
"    String references \"Channel Islands National Parks Inventory and Monitoring information: http://nature.nps.gov/im/units/medn . Kelp Forest Monitoring Protocols: http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 32.8;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" + 
"    String subsetVariables \"id, longitude, latitude, common_name, species_name\";\n" +
"    String summary \"This dataset has measurements of the size of selected animal species at selected locations in the Channel Islands National Park. Sampling is conducted annually between the months of May-October, so the Time data in this file is July 1 of each year (a nominal value). The size frequency measurements were taken within 10 meters of the transect line at each site.  Depths at the site vary some, but we describe the depth of the site along the transect line where that station's temperature logger is located, a typical depth for the site.\";\n" +
"    String time_coverage_end \"2007-07-01T00:00:00Z\";\n" +
"    String time_coverage_start \"1985-07-01T00:00:00Z\";\n" +
"    String title \"Channel Islands, Kelp Forest Monitoring, Size and Frequency, Natural Habitat\";\n" +
"    Float64 Westernmost_Easting -120.4;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String id;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 depth;\n" +
"    Float64 time;\n" +
"    String common_name;\n" +
"    String species_name;\n" +
"    Int16 size;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromHttpGet.test 1D make DATA FILES\n");       

        //.csv    for one lat,lon,time
        userDapQuery = "" +
            "&longitude=-119.05&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1Station", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,57\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,41\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,55\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,15\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,23\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv    for one lat,lon,time      via lon > <
        userDapQuery = "" +
            "&longitude>-119.06&longitude<=-119.04&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,57\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,41\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,55\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,15\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,23\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species
        userDapQuery = "" +
            "&time=2005-07-01&common_name=\"Red+abalone\"";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_eq", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Hare Rock),-120.35,34.05,5.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,13\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,207\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,203\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,193\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Rosa (South Point),-120.116666666667,33.8833333333333,13.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,185\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,198\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,85\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species    String !=
        userDapQuery = "" +
            "&time=2005-07-01&id!=\"San+Miguel+(Hare+Rock)\"&common_name=\"Red+abalone\"";
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_NE", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,207\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,203\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,193\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Rosa (South Point),-120.116666666667,33.8833333333333,13.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,185\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,198\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,85\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species   String > <
        userDapQuery = "" +
            "&time=2005-07-01&id>\"San+Miguel+(G\"&id<=\"San+Miguel+(I\"&common_name=\"Red+abalone\"";
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_gtlt", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Hare Rock),-120.35,34.05,5.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,13\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species     REGEX
        userDapQuery = "longitude,latitude,depth,time,id,species_name,size" + //no common_name
            "&time=2005-07-01&id=~\"(zztop|.*Hare+Rock.*)\"&common_name=\"Red+abalone\"";   //but common_name here
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_regex", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = String2.directReadFrom88591File(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,depth,time,id,species_name,size\n" +
"degrees_east,degrees_north,m,UTC,,,mm\n" +
"-120.35,34.05,5.0,2005-07-01T00:00:00Z,San Miguel (Hare Rock),Haliotis rufescens,13\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }



    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
/* */
        testStatic();
        testBasic(false); //deleteCachedDatasetInfo
        testGenerateDatasetsXml();
    }
}

