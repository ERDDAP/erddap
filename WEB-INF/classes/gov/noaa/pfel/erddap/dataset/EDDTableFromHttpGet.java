/* 
 * EDDTableFromHttpGet Copyright 2016, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
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

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** 
 * This class represents a table of data from a collection of jsonlCSV data files
 * which are created by HTTP GET calls to ERDDAP.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2016-06-14
 */
public class EDDTableFromHttpGet extends EDDTableFromFiles { 

    //special column names   //DON'T EVER CHANGE ANY OF THESE!!!
    public final static String TIMESTAMP = "timestamp"; //epic seconds as double
    public final static String AUTHOR    = "author";    //String
    public final static String COMMAND   = "command";   //byte
    public final static String SPECIAL_VAR_NAMES[] = {TIMESTAMP,                    AUTHOR,   COMMAND};
    public final static String SPECIAL_VAR_TYPES[] = {"double",                    "String", "byte"};
    public final static String SPECIAL_VAR_UNITS[] = {Calendar2.SECONDS_SINCE_1970, null,     null};
//COMMAND needs CF FLAG attributes 0=insert, 1=delete
    public final static byte INSERT_COMMAND = 0;
    public final static byte DELETE_COMMAND = 1;

    public final static String  NUMERIC_TIMESTAMP_REGEX = "numericTimestamp\":(\\d\\.\\d{2,12}E9),?\\n";
    public final static Pattern NUMERIC_TIMESTAMP_PATTERN = Pattern.compile(NUMERIC_TIMESTAMP_REGEX);

    protected String         columnNames[];  //all, not just NEC
    protected String         columnUnits[];
    protected Class          columnClasses[];
    protected PrimitiveArray columnMvFv[];

    protected long lastSaveDirTableFileTableBadFiles = 0; //System.currentTimeMillis
    //insertOrDelete calls saveDirTableFileTable if &gt;=5 seconds since last save
    //This works quite well because 
    //  infrequently changed datasets (&gt; every 5 seconds) will save the fileTable to disk every time there is a change,
    //  but frequently changed datasets will just save fileTable every 5 seconds
    //  (and shouldn't ever be much more than 5 seconds out-of-date,
    //  except for weird case where the changes stop arriving for a while).
    //The big issue/danger here is:
    //  if there are frequent changes, 
    //  and a new .insert leads to a new max or min for a variable, 
    //  that change to the fileTable will be lost 
    //  if the dataset is reloaded (using fileTable info from disk)
    //  if the fileTable info on disk is out-of-date.
    //  It is only at that handoff that very recent fileTable changes can be lost.
    //  Having a smaller value here minimizes that risk,
    //  but at the cost of slowing down .insert and .delete.
    //A better solution: find a way for fileTableInMemory to be 
    //  passed directly to the new EDDTableFromHttpGet when the dataset is reloaded
    //  or to force the old version of the dataset to save fileTable info to disk
    //  right before reading it for new dataset.
    public static long saveDirTableFileTableBadFilesEveryMS = 5000; 

    /**
     * EDDTableFromHttpGet DOESN'T SUPPORT UNPACKWHAT OPTIONS (other than 0).
     * This returns the default value for standardizeWhat for this subclass.
     * See Attributes.unpackVariable for options.
     * The default was chosen to mimic the subclass' behavior from
     * before support for standardizeWhat options was added.
     *
     */
    public int defaultStandardizeWhat() {return DEFAULT_STANDARDIZEWHAT; } 
    public static int DEFAULT_STANDARDIZEWHAT = 0;

    /**
     * This extracts the numericTimestamp from the results.
     * @return the numericTimestamp (else NaN if trouble).
     */
    public static double extractTimestamp(String results) {
         Matcher matcher = NUMERIC_TIMESTAMP_PATTERN.matcher(results);
         if (matcher.find()) 
             return String2.parseDouble(matcher.group(1));
         return Double.NaN;
    }

    /**
     * For testing, insert/delete results will match this.
     */
    public static String resultsRegex(int nRows) {
        return 
            "\\{\n" +
            "\"status\":\"success\",\n" +
            "\"nRowsReceived\":" + nRows + ",\n" +
            "\"stringTimestamp\":\"....-..-..T..:..:..\\....Z\",\n" +
            "\"numericTimestamp\":.\\..{2,12}E9\n" +
            "\\}\n";
    }


    /** 
     * The constructor. 
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
        boolean tAccessibleViaFiles, boolean tRemoveMVRows, 
        int tStandardizeWhat, int tNThreads, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex) 
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
            tSortedColumnSourceName, 
            tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, 
            tFileTableInMemory,
            tAccessibleViaFiles,
            tRemoveMVRows, tStandardizeWhat, 
            tNThreads, tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex);

        //standardizeWhat must be 0 or absent
        String msg = String2.ERROR + 
            " in EDDTableFromHttpGet constructor for datasetID=" + datasetID + ": ";
        if (standardizeWhat != 0)
            throw new RuntimeException(msg + "'standardizeWhat' MUST be 0.");

        //cacheFromUrl must be null
        msg = String2.ERROR + 
            " in EDDTableFromHttpGet constructor for datasetID=" + datasetID + ": ";
        if (cacheFromUrl != null)
            throw new RuntimeException(msg + "'cacheFromUrl' MUST be null.");

        //columnNameForExtract can't be used
        if (String2.isSomething(columnNameForExtract))
            throw new RuntimeException(msg + 
                "'columnNameForExtract' can't be used for EDDTableFromHttpGet datasets.");

        //sortedColumnSourceName must be ""
        if (!"".equals(sortedColumnSourceName))
            throw new RuntimeException(msg + 
                "'sortedColumnSourceName' MUST be nothing.");

        //Check last 3 var SPECIAL_VAR_NAMES: TIMESTAMP, AUTHOR, COMMAND
        for (int i = 0; i < 3; i++) {
            int which = String2.indexOf(dataVariableSourceNames, SPECIAL_VAR_NAMES[i]);
            if (which < 0)
                throw new SimpleException(msg +
                    "One of the variables must have the name \"" +
                    SPECIAL_VAR_NAMES[which] + "\".");

            if (!SPECIAL_VAR_TYPES[i].equals(dataVariables[which].sourceDataType()))
                throw new SimpleException(msg +
                    "Variable=" + SPECIAL_VAR_NAMES[i] + 
                    " must have dataType=" + SPECIAL_VAR_TYPES[i] + 
                    ", not \"" + dataVariables[which].sourceDataType() + "\".");

            if (SPECIAL_VAR_UNITS[i] != null &&
                !SPECIAL_VAR_UNITS[i].equals(dataVariables[which].units()))
                throw new SimpleException(msg +
                    "Variable=" + SPECIAL_VAR_NAMES[i] + 
                    " must have units=" + SPECIAL_VAR_UNITS[i] + 
                    ", not \"" + dataVariables[which].units() + "\".");
        }

        int nDV = dataVariables.length;
        columnNames   = new String[nDV];
        columnUnits   = new String[nDV];
        columnClasses = new Class[nDV];
        columnMvFv    = new PrimitiveArray[nDV];
        for (int dvi = 0; dvi < nDV; dvi++) {
            EDV edv = dataVariables[dvi];
            String sourceName = edv.sourceName();
            String destName = edv.destinationName();         
            columnNames[dvi] = sourceName;
            columnUnits[dvi] = edv.addAttributes().getString("units"); //here, "source" units are in addAttributes!
            columnClasses[dvi] = edv.sourceDataTypeClass();

            // No char variables
            if (columnClasses[dvi] == char.class)
                throw new SimpleException(msg + "No dataVariable can have dataType=char.");

            //! column sourceNames must equal destinationNames (unless "=something")
            if (!sourceName.startsWith("=") &&
                !sourceName.equals(destName))
                throw new SimpleException(msg +
                    "Every variable sourceName and destinationName must match (" +
                    sourceName + " != " + destName + ").");

            //columnMvFv
            if (columnClasses[dvi] == String.class) {
                StringArray tsa = new StringArray(2, false);
                if (edv.stringMissingValue().length() > 0) tsa.add(edv.stringMissingValue());
                if (edv.stringFillValue().length()    > 0) tsa.add(edv.stringFillValue());
                columnMvFv[dvi] = tsa.size() == 0? null : tsa;
            } else if (columnClasses[dvi] == long.class) {
                StringArray tsa = new StringArray(2, false);
                String ts = edv.combinedAttributes().getString("missing_value");
                if (ts != null) tsa.add(ts);
                ts = edv.combinedAttributes().getString("_FillValue");
                if (ts != null) tsa.add(ts);
                columnMvFv[dvi] = tsa.size() == 0? null : tsa;
            } else {
                DoubleArray tda = new DoubleArray(2, false);
                if (!Double.isNaN(edv.sourceMissingValue())) tda.add(edv.sourceMissingValue());
                if (!Double.isNaN(edv.sourceFillValue()   )) tda.add(edv.sourceFillValue());
                columnMvFv[dvi] = tda.size() == 0? null : tda;
            }
        }

        if (verbose) String2.log("*** EDDTableFromHttpGet constructor for datasetID=" +
            datasetID + " finished successfully.");
    }

    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     *
     * @param sourceDataNames must be specified (not null or size=0)
     * @param sourceDataTypes must be specified (not null or size=0)
     * @throws Throwable if too much data.
     *  This won't throw an exception if no data.
     */
    public Table lowGetSourceDataFromFile(String tFileDir, String tFileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, 
        double maxSorted, //maxSorted is adulterated if time and close to NOW. Can't use for timestamp.
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        boolean process = true;
        double timestampSeconds = Double.MAX_VALUE; //i.e., don't constrain timestamp
        if (sourceConVars != null) {
            for (int i = 0; i < sourceConVars.size(); i++) {
                String scVar = sourceConVars.get(i);
                if (TIMESTAMP.equals(scVar)) {
                    if (sourceConOps.get(i).indexOf('<') < 0) {  //timestamp= or >= or leads to no processing
                        process = false;
                        break;
                    } else {
                        double td = String2.parseDouble(sourceConValues.get(i));
                        if (Double.isNaN(td))
                            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (" + TIMESTAMP + " constraint)");
                        timestampSeconds = Math.min(timestampSeconds - (sourceConOps.get(i).equals("<")?1:0), td);                     
                    }
                } else if (AUTHOR.equals(scVar) ||
                    COMMAND.equals(scVar)) {
                    process = false;
                    break;
                }
            }
        }              

        return readFile(tFileDir + tFileName, sourceDataNames, sourceDataTypes, 
            httpGetRequiredVariableNames, process, timestampSeconds); 
    }


    /**
     * This gets the data from one file and processes it (edits and deletes are applied)
     * up to and including rows with the specified timestampSeconds value.
     *
     * @param sourceDataNames must be fully specified (not null or length=0)
     * @param sourceDataClasses must be fully specified (not null or length=0)
     * @param tRequiredVariableNames are the dataset's requiredVariableNames, e.g., stationID, time
     * @param process If true, the log is processed.  If false, the raw data is returned.
     * @param timestampSeconds the maximum timestampSeconds to be kept if process=true. 
     *   Use Double.MAX_VALUE or Double.NaN to keep all rows.
     * @return the processed data table from one file.
     *   Char vars are stored as shorts in the file, but returned as chars here.
     */
    public static Table readFile(String fullFileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        String[] tRequiredVariableNames, boolean process, double timestampSeconds) throws Throwable {

        //String2.log(">> EDDTableFromHttpGet.readFile process=" + process + " timestampSeconds=" + timestampSeconds);
        //String2.directReadFrom88591File(fullFileName)); 

        //read needed columns of the file (so UPDATE's and DELETE's can be processed)
        Table table = new Table();
        //synchronized: don't read from file during a file write in insertOrDelete
        synchronized (fullFileName) {  //fullFileName is canonical: from ArrayString, so good to synchronize on
            //but probably not too bad if in middle of write
            table.readJsonlCSV(fullFileName, sourceDataNames, sourceDataTypes, false);
        }
        //String2.log(">> table in " + fullFileName + " :\n" + table.dataToString());
        //table.saveAsDDS(System.out, "s");

        //gather info about the table
        int nCols = table.nColumns();
        int nRows = table.nRows();
        PrimitiveArray pas[] = new PrimitiveArray[nCols];
        for (int col = 0; col < nCols; col++) 
            pas[col] = table.getColumn(col);
        int timestampColi = table.findColumnNumber(TIMESTAMP);
        int commandColi   = table.findColumnNumber(COMMAND);
        if (timestampColi < 0 ||
            commandColi   < 0) 
            throw new SimpleException(
                String2.ERROR + " while reading " + fullFileName + ": " +
                "columnName=" + (timestampColi < 1? TIMESTAMP : COMMAND) + 
                " not found in " + table.getColumnNamesCSVString() + ".");

        if (timestampSeconds < Double.MAX_VALUE) {  //NaN -> false
            //remove rows with timestamp > requested timestamp (whether process or not)
 
            //timestampPA should be sorted in ascending order (perhaps with ties)
            //BUT if computer's clock is rolled back, there is possibility of out-of-order.
            //OR if different threads doing changes processed at different speeds could finish out-of-order.
            //so sort by timestampColi first
            table.ascendingSort(new int[]{timestampColi});
            //String2.log(">>  timestamp values=" + table.getColumn(timestampColi));

            int row = table.getColumn(timestampColi).binaryFindFirstGE(0, nRows - 1, 
                timestampSeconds + 0.0005); // 1/2 milli later to avoid rounding problems, and because I just want > (not GE)
            //String2.log("  timestamp constraint removed " + (nRows-row) + " rows. Now nRows=" + row);
            if (row < nRows) {
                table.removeRows(row, nRows);  //exclusive
                nRows = row;
            }
        }

        if (!process || nRows == 0)
            return table;

        //process the file
        //sort based on requiredVariableNames+timestamp  (e.g., stationID, time, timestamp)
        //Except for revisions, the data should already be in this order or very close to it.
        int nRCN = tRequiredVariableNames.length;
        int sortBy[] = new int[nRCN + 1];
        for (int i = 0; i < nRCN; i++) {
            sortBy[i] = table.findColumnNumber(tRequiredVariableNames[i]);
            if (sortBy[i] < 0) 
                throw new SimpleException(
                    String2.ERROR + " while reading " + fullFileName + ": " +
                    "columnName=" + tRequiredVariableNames[i] + 
                    " not found in " + table.getColumnNamesCSVString() + ".");
        }
        sortBy[nRCN] = timestampColi;
        //I am confident that sorting by timestamp will be correct/exact.
        //Although the numbers are 0.001, if bruised (0.000999999999),
        //  they should be bruised the same way on different rows.
        //If that is incorrect, then a solution is:
        //  multiply time and timestamp by 1000 and round to integer before sorting
        //  then divide by 1000 after justKeep() below.
        table.ascendingSort(sortBy);

        //Just keep last row of each group of rows where HttpGetRequiredVariables are same.
        //But if that row is DELETE, then don't keep it either.
        BitSet justKeep = new BitSet(nRows);  //all false
        for (int row = 0; row < nRows; row++) { //look at row and row+1
            if (row < nRows - 1) {
                boolean allSame = true;           //not considering timestamp
                for (int i = 0; i < nRCN; i++) {  //not considering timestamp
                    if (pas[sortBy[i]].compare(row, row + 1) != 0) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame)   //all requiredVariableNames are same as next row
                    continue;  //don't keep this row                
            }

            //this row is last of a group
            //if last command is DELETE, then delete this row
            if (pas[commandColi].getInt(row) == DELETE_COMMAND) 
                continue; //don't keep this row

            //else keep this row
            justKeep.set(row);
        }
        table.justKeep(justKeep);      

        return table;
    }


    /**
     * This parses the httpGetDirectoryStructure specification.
     *
     * @param specification e.g, stationID/10years/7days
     * @param dsSourceName will be filled, with [i] = a var sourceName or "".
     *    sourceNames haven't been tested to see if they are in the dataset.
     * @param dsN          will be filled, with [i] = the number of Calendar items, or -1
     * @param dsCalendar   will be filled, with [i] = the e.g., Calendar.MONTH, or -1
     * @throws RuntimeException if trouble
     */ 
    public static void parseHttpGetDirectoryStructure(String specification, 
         StringArray dsSourceName, IntArray dsN, IntArray dsCalendar) {

         dsSourceName.clear();
         dsN.clear();
         dsCalendar.clear();
         String parts[] = String2.split(specification, '/');
         Pattern pattern = Pattern.compile("(\\d+)([A-Za-z]+)");
         for (int p = 0; p < parts.length; p++) {
             Matcher matcher = pattern.matcher(parts[p]);
             boolean isNUnits = matcher.matches(); //e.g., 5days
             int cal = -1;
             if (isNUnits) { //well, probably isNUnits
                 //e.g., 5days
                 // but it may be a column name which matches the pattern accidentally,
                 // so try/catch
                 try {
                     String units = matcher.group(2);
                     cal = Calendar2.unitsToConstant(units); //throws exception
                     if (cal == Calendar.WEEK_OF_YEAR)
                         throw new RuntimeException(String2.ERROR + " parsing httpGetDirectoryStructure: " +
                             "units=" + units + " is invalid.");
                 } catch (Exception e) {
                     isNUnits = false;
                     String2.log("Treating httpGetDirectoryStructure part#" + p +"=" + parts[p] + 
                         " as a columnName (" + e.toString() + ").");
                 }
             }
                
            if (isNUnits) {
                 dsSourceName.add("");
                 dsN.add(         String2.parseInt(matcher.group(1)));
                 dsCalendar.add(  cal);
             } else {
                 dsSourceName.add(parts[p]);
                 dsN.add(         -1);
                 dsCalendar.add(  -1);
             }
         }
         dsSourceName.trimToSize();
         dsN.trimToSize();
         dsCalendar.trimToSize();
     }


    /**
     * This figures out the name of the relevant file (which may or 
     * may not exist.
     *
     * @param startDir with trailing slash
     * @param tDirStructureColumnNames For each part, the variable's source column name
     *   or "" if not used for this part.
     *   Any column names here should be in requiredVariableNames.
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
     *   It is usually a requiredVariable, but not always.
     *   It is an error if it is needed here, but timeEpSec is NaN.
     * @return the full file dir+name, starting with startDir. The file may not 
     *   exist yet.
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
                        String2.ERROR + " in httpGetDirectoryStructure part#" + i + 
                        ": column=" + tDirStructureColumnNames.get(i) + 
                        " isn't in columnNames=" + String2.toCSSVString(tColumnNames) + 
                        ".");
                //data value of "" is a valid value. It will be converted to something.
                String tp = String2.encodeFileNameSafe(tColumnValues[sni].getString(row)); 
                if (i < nParts - 1)
                    dirSB.append(tp);
                nameSB.append(tp);                

            } else {
                //Find the time part. Truncate to n'th precision. 
                //e.g., 17 seconds to 5seconds precision is 15 seconds.
                //(MONTH is 0-based, so that works correctly as is.)
                if (timeEpSec >= 253402300800.0 || //10000-01-01
                    timeEpSec <= -377711769600.0) //-10000-01-01
                    throw new SimpleException(
                        String2.ERROR + " in httpGetDirectoryStructure part#" + i + 
                        ": invalid time value (timeEpSec=" + timeEpSec + ")!");
                //need a new gc for each part since gc is modified
                GregorianCalendar gc = Calendar2.epochSecondsToGc(timeEpSec); 
                int n = tDirStructureNs.get(i);
                gc.set(cal, (gc.get(cal) / n) * n);
                //Get the ISO 8601 date/time string just to that precision/field.
                String s = Calendar2.formatAsISODateTimeT3Z(gc); //to millis 
                int nChar = s.length();
                if      (cal == Calendar.YEAR)        nChar = 4;
                else if (cal == Calendar.MONTH)       nChar = 7;
                else if (cal == Calendar.DATE)        nChar = 10;
                else if (cal == Calendar.HOUR_OF_DAY) nChar = 13;
                else if (cal == Calendar.MINUTE)      nChar = 16;
                else if (cal == Calendar.SECOND)      nChar = 19;
                else nChar--; //to millis precision
                String tp = s.substring(0, nChar);  
                tp = String2.replaceAll(tp, ':', '-'); //make fileNameSafe
                if (i < nParts - 1)
                    dirSB.append(tp);
                nameSB.append(tp);                
            }
        }

        return dirSB.toString() + nameSB.toString() + ".jsonl";
    }

    /** 
     * This is the non-static insertOrDelete method which calls the
     * static insertOrDelete method.
     *
     * @return the response String
     * @throws Throwable if any kind of trouble
     */
    public String insertOrDelete(byte command, String userDapQuery) throws Throwable {

        if (cacheFromUrl != null) {
            throw new SimpleException(
                "For EDDTableFromHttpGet datasets, if cacheFromUrl is active, " +
                ".insert and .delete are not allowed for this local dataset " +
                "because any changes would be lost the next time the local dataset " +
                "checks for files from the remote dataset.");
        }

        Table tDirTable  = dirTable;  //succeeds if fileTableInMemory (which it should always be)
        Table tFileTable = fileTable;
        if (tDirTable == null) 
            tDirTable  = tryToLoadDirFileTable(datasetDir() +  DIR_TABLE_FILENAME); //may be null
        if (tFileTable == null) 
            tFileTable = tryToLoadDirFileTable(datasetDir() + FILE_TABLE_FILENAME); //may be null
        if (tDirTable == null || tFileTable == null) {
            requestReloadASAP();
            throw new SimpleException("dirTable and/or fileTable are null!");
        }

        String response = insertOrDelete(fileDir, 
            httpGetDirectoryStructureColumnNames, 
            httpGetDirectoryStructureNs, 
            httpGetDirectoryStructureCalendars,
            httpGetKeys,
            combinedGlobalAttributes,
            columnNames, columnUnits, columnClasses, columnMvFv,
            httpGetRequiredVariableNames,
            command, userDapQuery,
            tDirTable, tFileTable);  

//do more with badFileMap?

        //do slow / background thing first:
        //save dirTableFileTable to disk?
        //there is always a change to fileTable and min max (e.g., timeStamp)
        long tTime = System.currentTimeMillis();
        if (!fileTableInMemory ||  
            tTime - lastSaveDirTableFileTableBadFiles >= saveDirTableFileTableBadFilesEveryMS) { 
            saveDirTableFileTableBadFiles(standardizeWhat, tDirTable, tFileTable,  //throws Throwable
                null); //null so ignore badFilesMap
            lastSaveDirTableFileTableBadFiles = tTime;
        }

        //then faster things 
        Table tMinMaxTable = makeMinMaxTable((StringArray)(tDirTable.getColumn(0)), tFileTable);

        //then, change secondary parts of instance variables
        //e.g., update all variable destinationMinMax
        updateDestinationMinMax(tMinMaxTable);

        //then put in place as quickly/atomically as possible
        minMaxTable = tMinMaxTable; //swap into place quickly
        if (fileTableInMemory) {  //it will always be true
            //quickly swap into place
            dirTable  = tDirTable;
            fileTable = tFileTable; 
        }

        return response;
    }


    /** 
     * This is used to add insert or delete commands into a data file of this dataset. 
     * This is EDDTableFromHttpGet overwriting the default implementation.
     *
     * <p>The key should be author_secret. So keys are specific to specific people/actors.
     * The author will be kept and added to the 'author' column in the dataset.
     *
     * <p>INSERT works like SQL's INSERT and UPDATE.
     * Effectively, if the info matches existing values of sortColumnSourceNames,
     * the previous data is updated/overwritten. Otherwise, it is inserted.
     * (In reality, the info is just added to the log.)
     *
     * <p>DELETE works like SQL's DELETE, but just enters the deletion info in the log.
     * The original info isn't actually deleted.
     * 
     * <p>Timings on my pathetic laptop, with Win 7 (2018-06-27):
     * <br>nRows/insert  ms/insert
     * <br>1             1.0025
     * <br>10            1.835
     * <br>100           2.9075
     * <br>1000          12.69 (but sometimes longer)
     * <br>So clearly, you can push vastly more data in if you do it in batches.
     * <br>The limit is probably Tomcat query length, which you can change (default is 8KB?) 
     * 
     * <p>Having this static method do the actual work makes the system easier to test.
     *
     * @param tDirStructureColumnNames the column names for the parts of the 
     *   dir and file names. All of these names must be in requiredVariableNames.
     * @param keys the valid values of author= (to authenticate the author)
     * @param tGlobalAttributes  used when creating a new file
     * @param columnNames the names of ALL of the dataset's source variables,
     *   in the dataset's order,
     *   including timestamp, author, or command.
     *   The time variable, if any, must be named time.
     *   For a given dataset, this must not change over time.
     * @param columnUnits any of them may be null or "".
     *   All timestamp columns (in the general sense) should have UDUNITS 
     *   String time units (e.g., "yyyy-MM-dd'T'HH:mm:ss'Z'") 
     *   or numeric time units (e.g., "days since 1985-01-01").
     *   For INSERT and DELETE calls, the time values must be in that format
     *   (you can't revert to ISO 8601 format as with data requests in the rest of ERDDAP).
     * @param columnClasses the Java types (e.g., double.class, long.class, char.class, String.class).
     *   The missing values are the default missing values for PrimitiveArrays.
     *   All timestamp columns (in the general sense) MUST be double.class.
     * @param columnMvFv a PrimitiveArray of any suitable type
     *   (all are used via pa.indexOf(String)).  
     *   with the missing_value and/or _FillValue for each column (or null for each).
     *   If mv or fv is the PrimitiveArray standard missing_value, 
     *   it needn't be included in this PA.
     * @param requiredVariableNames the variable names which identify a unique row.
     *   All requiredVariableNames MUST be in columnNames.
     *   Insert requests MUST have all of the requiredVariableNames and usually 
     *     have all columnNames. Missing columns will get (standard PrimitiveArray) 
     *     missing values.
     *   Delete requests MUST have all of the requiredVariableNames and, in addition,
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
        Attributes tGlobalAttributes,
        String columnNames[], String columnUnits[], Class columnClasses[], PrimitiveArray columnMvFv[],
        String requiredVariableNames[],
        byte command, String userDapQuery,
        Table dirTable, Table fileTable) throws Throwable {

        double timestampSeconds = System.currentTimeMillis() / 1000.0;
        if (dirTable == null || fileTable == null) { //ensure both or neither
            dirTable = null;
            fileTable = null;
        }

        //store things in data structures paralleling columnNames (i.e., [col])
        int nColumns = columnNames.length;
        if (nColumns == 0)
            throw new SimpleException(String2.ERROR + ": columnNames not specified.");
        PrimitiveArray columnValues[] = new PrimitiveArray[nColumns];
        boolean columnIsFixed[]  = new boolean[nColumns];
        boolean columnIsString[] = new boolean[nColumns];
        boolean columnIsLong[]   = new boolean[nColumns];
        int timeColumn = -1;         
        String timeFormat = null;               //used if time variable is string
        double timeBaseAndFactor[] = null;      //used if time variable is numeric
        int timestampColumn = -1;
        int authorColumn = -1;
        int commandColumn = -1;
        for (int col = 0; col < nColumns; col++) {
            columnIsFixed[ col] = columnNames[col].charAt(0) == '=';
            columnIsString[col] = columnClasses[col] == String.class; //char treated as numeric
            columnIsLong[  col] = columnClasses[col] == long.class;

            if (!String2.isSomething(columnUnits[col]))
                columnUnits[col] = "";

            if (columnNames[col].equals(EDV.TIME_NAME)) {
                timeColumn = col;
                if (columnIsString[col]) {
                    //string times
                    if (!Calendar2.isStringTimeUnits(columnUnits[col])) {
                        String2.log("columnUnits[" + col + "]=" + columnUnits[col]);
                        throw new SimpleException(EDStatic.queryError + 
                            "Invalid units for the string time variable. " +
                            "Units MUST specify the format of the time values.");
                    }
                    timeFormat = columnUnits[col];
                } else {
                    //numeric times
                    timeBaseAndFactor = Calendar2.getTimeBaseAndFactor(
                        columnUnits[col]); //throws RuntimeException if trouble
                }
            } else if (columnNames[col].equals(TIMESTAMP)) { timestampColumn = col;
            } else if (columnNames[col].equals(AUTHOR))    { authorColumn    = col;
            } else if (columnNames[col].equals(COMMAND))   { commandColumn   = col;
            }
        }
        columnValues[timestampColumn] = new DoubleArray(new double[]{timestampSeconds});
        columnValues[commandColumn]   = new ByteArray(  new byte[]{command});

        //parse the userDapQuery's parts. Ensure it is valid. 
        String parts[] = String2.split(userDapQuery, '&');
        int nParts = parts.length;
        String author = null; //the part before '_'
        int arraySize = -1; //until an array is found
        BitSet requiredVariablesFound = new BitSet();
        for (int p = 0; p < nParts; p++) {
            int eqPo = parts[p].indexOf('=');
            if (eqPo <= 0 || //no '=' or no name
                "<>~!".indexOf(parts[p].charAt(eqPo-1)) >= 0) // <= >= != ~=
                throw new SimpleException(EDStatic.queryError + 
                    "The \"" + parts[p] + "\" parameter isn't in the form name=value.");
            String tName  = parts[p].substring(0, eqPo); //names should be varNames so not percent encoded
            String tValue = SSR.percentDecode(parts[p].substring(eqPo + 1));            
            //String2.log(">> tName=" + tName + " tValue=" + String2.annotatedString(tValue));

            //catch and verify author=
            if (tName.equals(AUTHOR)) {
                if (tValue.startsWith("\"") && tValue.endsWith("\""))
                    tValue = String2.fromJson(tValue);

                if (p != nParts - 1)
                    throw new SimpleException(EDStatic.queryError + 
                        "author= must be the last parameter.");
                if (!keys.contains(tValue))  //this tests validity of author_key (since checked when created)
                    throw new SimpleException(EDStatic.queryError + 
                        "Invalid author_key.");
                int po = Math.max(0, tValue.indexOf('_')); 
                author = tValue.substring(0, po); 
                columnValues[authorColumn] = new StringArray(new String[]{author});

            } else { 
                //is it a requiredVariable?
                int whichRC = String2.indexOf(requiredVariableNames, tName);
                if (whichRC >= 0)
                    requiredVariablesFound.set(whichRC);

                //whichColumn? 
                int whichCol = String2.indexOf(columnNames, tName);
                if (whichCol < 0) {
                    String2.log("columnNames=" + String2.toCSSVString(columnNames));
                    throw new SimpleException(EDStatic.queryError + 
                        "Unknown variable name=" + tName);
                } else if (whichCol == timestampColumn) {
                    throw new SimpleException(EDStatic.queryError + 
                        "An .insert or .delete request must not include " + TIMESTAMP + " as a parameter.");
                } else if (whichCol == commandColumn) {
                    throw new SimpleException(EDStatic.queryError + 
                        "An .insert or .delete request must not include " + COMMAND + " as a parameter.");
                }

                if (columnValues[whichCol] != null) 
                    throw new SimpleException(EDStatic.queryError + 
                        "There are two parameters with variable name=" + tName + "."); 

                //get the values
                if (tValue.startsWith("[") &&
                    tValue.endsWith(  "]")) {
                    //deal with array of values: name=[valuesCSV]

                    StringArray sa = new StringArray(StringArray.arrayFromCSV(
                        tValue.substring(1, tValue.length() - 1), ",", false)); //trim?
                    columnValues[whichCol] = PrimitiveArray.factory(columnClasses[whichCol], sa); //does nothing if desired class is String
                    //if (columnClasses[whichCol] == char.class || columnClasses[whichCol] == String.class) String2.log(">> writing var=" + tName + " pa=" + columnValues[whichCol]);
                    if (arraySize < 0)
                        arraySize = columnValues[whichCol].size();
                    else if (arraySize != columnValues[whichCol].size())
                        throw new SimpleException(EDStatic.queryError + 
                            "Different parameters with arrays have different sizes: " +
                            arraySize + "!=" + columnValues[whichCol].size() + ".");

                } else {
                    //deal with single value: name=value
                    if (tValue.startsWith("\"") && tValue.endsWith("\""))
                        tValue = String2.fromJson(tValue);
                    StringArray sa = new StringArray(
                        //do it this way to deal with quotes, special chars, etc.
                        StringArray.arrayFromCSV(tValue, ",", false)); //trim?  
                    if (sa.size() > 1)
                        throw new SimpleException(EDStatic.queryError + 
                            "One value (not " + sa.size() +
                            ") expected for columnName=" + tName + ". (missing [ ] ?)");
                    if (sa.size() == 0)
                        sa.add("");
                    columnValues[whichCol] = PrimitiveArray.factory(columnClasses[whichCol], sa); //does nothing if desired class is String

                    //if (columnClasses[whichCol] == String.class &&
                    //    (tValue.length() < 2 || 
                    //     tValue.charAt(0) != '"' ||
                    //     tValue.charAt(tValue.length() - 1) != '"'))
                    //    throw new SimpleException(EDStatic.queryError + 
                    //        "The String value for columnName=" + tName + 
                    //        " must start and end with \"'s.");
                }

                //ensure required var has valid value
                if (whichRC >= 0) {
                    PrimitiveArray pa = columnValues[whichCol];
                    if (pa.size() == 0 || pa.getString(0).length() == 0) //string="" number=NaN
                        throw new SimpleException(EDStatic.queryError + 
                            "requiredVariable=" + tName + " must have a valid value.");
                }
            }
        }

        //ensure required parameters were specified 
        if (author == null)
            throw new SimpleException(EDStatic.queryError + 
                "author= was not specified.");
        int notFound = requiredVariablesFound.nextClearBit(0);
        if (notFound < requiredVariableNames.length)
            throw new SimpleException(EDStatic.queryError + 
                "requiredVariableName=" + requiredVariableNames[notFound] + 
                " wasn't specified.");

        //make all columnValues the same size
        int maxSize = Math.max(1, arraySize);
        for (int col = 0; col < nColumns; col++) {
            PrimitiveArray pa = columnValues[col]; 

            //If this var wasn't in the command, so use mv's
            if (pa == null) 
                pa = columnValues[col] = PrimitiveArray.factory(columnClasses[col],
                    maxSize, ""); //if strings, "" is already UTF-8

            //duplicate scalar n=maxSize times
            if (pa.size() == 1 && maxSize > 1) 
                columnValues[col] = PrimitiveArray.factory(columnClasses[col],
                    maxSize, pa.getString(0));
        }

        //figure out the fullFileName for each row
        StringArray fullFileNames = new StringArray(maxSize, false);
        for (int row = 0; row < maxSize; row++) { 
            //figure out the epochSeconds time value
            double tTime = 
                timeColumn < 0? Double.NaN :                           //no time column
                timeBaseAndFactor == null? Calendar2.parseToEpochSeconds(
                    columnValues[timeColumn].getString(row), timeFormat) : 
                Calendar2.unitsSinceToEpochSeconds(                    //numeric time
                    timeBaseAndFactor[0], timeBaseAndFactor[1], 
                    columnValues[timeColumn].getDouble(row));

            fullFileNames.add(whichFile(startDir, 
                tDirStructureColumnNames, tDirStructureNs, tDirStructureCalendars,
                columnNames, columnValues, row, tTime)); 
        }

        //EVERYTHING SHOULD BE VALIDATED BY NOW. NO ERRORS AFTER HERE!
        //append each input row to the appropriate file
        int row = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        String  columnMinString[] = new String[nColumns];
        String  columnMaxString[] = new String[nColumns];
        long    columnMinLong[]   = new long[nColumns];
        long    columnMaxLong[]   = new long[nColumns];
        double  columnMinDouble[] = new double[nColumns];
        double  columnMaxDouble[] = new double[nColumns];
        boolean columnHasNaN[]    = new boolean[nColumns];
        while (row < maxSize) {
            //figure out which file
            //EFFICIENT: Code below handles all rows that use this fullFileName.               
            String fullFileName = fullFileNames.get(row); //it is canonical
            //String2.log(">> writing to " + fullFileName);

            //figure out which rows go to this fullFileName
            int startRow = row++;
            while (row < maxSize && 
                fullFileNames.get(row).equals(fullFileName))
                row++;
            int stopRow = row; //1 past end

            //connect info for this file
            baos.reset();
            boolean fileIsNew = false;

            try {

                if (!File2.isFile(fullFileName)) {
                    //if file doesn't exist, create it
                    fileIsNew = true; //first
                    File2.makeDirectory(File2.getDirectory(fullFileName)); //throws exception if trouble
                }
                Writer writer = new BufferedWriter(new OutputStreamWriter(baos, String2.UTF_8));  

                if (fileIsNew) {
                    //write the column names to the writer
                    boolean somethingWritten = false;
                    for (int col = 0; col < nColumns; col++) {
                        if (!columnIsFixed[col]) {
                            writer.write(somethingWritten? ',' : '[');
                            writer.write(String2.toJson(columnNames[col]));
                            somethingWritten = true;
                        }
                    }                        
                    writer.write("]\n");
                }

                //write the data to the writer
                for (int tRow = startRow; tRow < stopRow; tRow++) {
                    boolean somethingWritten = false;
                    for (int col = 0; col < nColumns; col++) {
                        if (!columnIsFixed[col]) {
                            writer.write(somethingWritten? ',' : '[');
                            writer.write(columnValues[col].getJsonString(tRow));
                            somethingWritten = true;
                        }
                    }
                    writer.write("]\n");
                }


                //prepare to write everything to the file
                writer.flush(); //should do nothing because already done 
                byte bar[] = baos.toByteArray();

                //As much as possible has been done ahead of time
                //  so write info to file is 1 blast
                //synchronized is ESSENTIAL: avoid problems with 2+ threads
                //  writing or reading same file at same time
                //synchronized is ESSENTIAL: fullFileName is canonical
                //  (since from StringArray) so same object in different threads
                //There were rare problems when writing to file with 4+ threads
                //  before switching to this system of full prep, then full write.
                synchronized (fullFileName) { //it is canonical, so synchronizing on it works across threads
                    //No buffering
                    BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(fullFileName, !fileIsNew)); //append?  
                    try {
                        fos.write(bar, 0, bar.length);  //entire write in 1 low level command
                        fos.close(); //explicitly now, not by finalize() at some time in future
                    } catch (Exception e) {
                        try {fos.close();} catch (Exception e2) {}
                        String2.log(String2.ERROR + 
                            " in EDDTableFromHttpGet while writing to " + fullFileName + ":\n" +
                            MustBe.throwableToString(e));
                        throw e;
                    }
                }

                //adjust min/max in fileTable if .insert
                //(only .insert because only it adds values (and .delete only has required variables))
                if (fileTable != null && command == INSERT_COMMAND) {

                    //prepare to calculate statistics
                    Arrays.fill(columnMinString, "\uFFFF"); 
                    Arrays.fill(columnMaxString, "\u0000");
                    Arrays.fill(columnMinLong,   Long.MAX_VALUE);   
                    Arrays.fill(columnMaxLong,   Long.MIN_VALUE);   
                    Arrays.fill(columnMinDouble, Double.MAX_VALUE);     
                    Arrays.fill(columnMaxDouble, -Double.MAX_VALUE);     
                    Arrays.fill(columnHasNaN,    false);   

                    //calculate statistics
                    for (int tRow = startRow; tRow < stopRow; tRow++) {
                        for (int col = 0; col < nColumns; col++) {
                            if (columnIsFixed[col]) {
                                //do nothing
                            } else if (columnIsString[col]) {
                                String s = columnValues[col].getString(tRow); 
                                if (s.length() == 0 ||
                                    (columnMvFv[col] != null && columnMvFv[col].indexOf(s) >= 0)) 
                                    columnHasNaN[col] = true;
                                else {
                                    if (s.compareTo(columnMinString[col]) < 0) columnMinString[col] = s;
                                    if (s.compareTo(columnMaxString[col]) > 0) columnMaxString[col] = s;
                                }
                            } else if (columnIsLong[col]) {
                                long d = columnValues[col].getLong(tRow); 
                                if (d == Long.MAX_VALUE ||
                                    (columnMvFv[col] != null && 
                                     columnMvFv[col].indexOf(columnValues[col].getString(tRow)) >= 0)) 
                                    columnHasNaN[col] = true;
                                else {
                                    if (d < columnMinLong[col]) columnMinLong[col] = d;
                                    if (d > columnMaxLong[col]) columnMaxLong[col] = d;
                                }
                            } else {
                                double d = columnValues[col].getDouble(tRow); 
                                if (Double.isNaN(d) ||
                                    (columnMvFv[col] != null && 
                                     columnMvFv[col].indexOf(columnValues[col].getString(tRow)) >= 0)) 
                                    columnHasNaN[col] = true;
                                else {
                                    if (d < columnMinDouble[col]) columnMinDouble[col] = d;
                                    if (d > columnMaxDouble[col]) columnMaxDouble[col] = d;
                                }
                            }
                        }
                    }

                    //save statistics to fileTable
                    synchronized (fileTable) {
                        String fileDir  = File2.getDirectory(fullFileName);
                        String fileName = File2.getNameAndExtension(fullFileName);

                        //which row in dirTable?
                        int dirTableRow = ((StringArray)(dirTable.getColumn(0))).indexOf(fileDir);
                        if (dirTableRow < 0) {
                            dirTableRow = dirTable.getColumn(0).size();
                            dirTable.getColumn(0).addString(fileDir);
                        }

                        //which row in the fileTable?
                        int fileTableRow = 0;
                        ShortArray fileTableDirPA   = (ShortArray)( fileTable.getColumn(FT_DIR_INDEX_COL));
                        StringArray fileTableNamePA = (StringArray)(fileTable.getColumn(FT_FILE_LIST_COL));
                        int fileTableNRows = fileTable.nRows();
                        while (fileTableRow < fileTableNRows &&
                               (fileTableDirPA.get(fileTableRow) != dirTableRow ||
                                !fileTableNamePA.get(fileTableRow).equals(fileName))) {
                            fileTableRow++;
                        }

                        if (fileTableRow == fileTableNRows) {
                            //add row to fileTable
                            fileTableDirPA.addInt(dirTableRow);
                            fileTableNamePA.add(fileName);
                            fileTable.getColumn(FT_LAST_MOD_COL).addLong(0); //will be updated below
                            fileTable.getColumn(FT_SIZE_COL).addLong(0);     //will be updated below 
                            fileTable.getColumn(FT_SORTED_SPACING_COL).addDouble(1); //irrelevant
                            for (int col = 0; col < nColumns; col++) {
                                int baseFTC = dv0 + col * 3; //first of 3 File Table Columns (min, max, hasNaN) for this col
                                if (columnIsFixed[col]) {
                                    fileTable.getColumn(baseFTC  ).addString(columnNames[col].substring(1)); //???
                                    fileTable.getColumn(baseFTC+1).addString(columnNames[col].substring(1));
                                } else if (columnIsString[col]) {
                                    fileTable.getColumn(baseFTC  ).addString(columnMinString[col]); 
                                    fileTable.getColumn(baseFTC+1).addString(columnMaxString[col]); 
                                } else if (columnIsLong[col]) {
                                    fileTable.getColumn(baseFTC  ).addLong(columnMinLong[col]); 
                                    fileTable.getColumn(baseFTC+1).addLong(columnMaxLong[col]); 
                                } else {
                                    fileTable.getColumn(baseFTC  ).addDouble(columnMinDouble[col]);
                                    fileTable.getColumn(baseFTC+1).addDouble(columnMaxDouble[col]);
                                }
                                fileTable.getColumn(baseFTC+2).addInt(columnHasNaN[col]? 1 : 0);   
                            }

                        } else {
                            //adjust current row:
                            //dir unchanged
                            //name unchanged
                            //lastMod will be updated below
                            //size be updated below 
                            //spacing unchanged/irrelevant
                            for (int col = 0; col < nColumns; col++) {
                                int baseFTC = dv0 + col * 3; //first of 3 File Table Columns (min, max, hasNaN) for this col
                                PrimitiveArray minColPA = fileTable.getColumn(baseFTC  );
                                PrimitiveArray maxColPA = fileTable.getColumn(baseFTC+1);
                                if (columnIsFixed[col]) {
                                    //already has fixed value
                                } else if (columnIsString[col]) {
                                    String tt = columnMinString[col];
                                    if (!tt.equals("\uFFFF")) { //has data
                                        if (tt.compareTo(minColPA.getString(fileTableRow)) < 0)
                                                         minColPA.setString(fileTableRow, tt); 
                                        tt = columnMaxString[col];
                                        if (tt.compareTo(maxColPA.getString(fileTableRow)) > 0)
                                                         maxColPA.setString(fileTableRow, tt); 
                                    }
                                } else if (columnIsLong[col]) {
                                    long tt = columnMinLong[col];
                                    if (tt != Long.MAX_VALUE) { //has data
                                        if (tt < minColPA.getLong(fileTableRow))
                                                 minColPA.setLong(fileTableRow, tt); 
                                        if (tt > maxColPA.getLong(fileTableRow))
                                                 maxColPA.setLong(fileTableRow, tt); 
                                    }
                                } else {
                                    double tt = columnMinDouble[col];
                                    if (!Double.isNaN(tt)) { //has data
                                        if (tt < minColPA.getDouble(fileTableRow))
                                                 minColPA.setDouble(fileTableRow, tt); 
                                        if (tt > maxColPA.getDouble(fileTableRow))
                                                 maxColPA.setDouble(fileTableRow, tt); 
                                    }
                                }
                                if (columnHasNaN[col])
                                    fileTable.getColumn(baseFTC+2).setInt(fileTableRow, 1);   
                            }
                        }  
                        
                        //update file's lastMod and size
                        long tLastMod = -1;
                        long tLength = -1; 
                        try {
                            File file = new File(fullFileName);
                            tLastMod = file.lastModified();
                            tLength = file.length();
                        } catch (Exception e) {
                            String2.log(String2.ERROR + 
                                " in EDDTableFromHttpGet while getting lastModified and length of " + 
                                fullFileName);
                        }
                        fileTable.getColumn(FT_LAST_MOD_COL).setLong(fileTableRow, tLastMod);
                        fileTable.getColumn(FT_SIZE_COL    ).setLong(fileTableRow, tLength);
                    } //end synchronized(fileTable)
                } 


            } catch (Throwable t) {
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
        return "{\n" +
            "\"status\":\"success\",\n" +
            "\"nRowsReceived\":" + maxSize + ",\n" +
            "\"stringTimestamp\":\"" + Calendar2.epochSecondsToIsoStringT3Z(timestampSeconds) + "\",\n" +
            "\"numericTimestamp\":" + timestampSeconds + "\n" +
            "}\n"; 
    }


    /**
     * This tests the static methods in this class.
     * @param hammer 
     *     If hammer&lt;0, this just runs the static tests 1 time.
     *     If hammer&gt;0, this doesn't delete existing files and  
     *       makes 10000 insertions with data values =hammer.
     *     If hammer==0, this prints the hammer'd data file -- raw, no processing.
     */
    public static void testStatic() throws Throwable {
        String2.log("\n*** EDDTableFromHttpGet.testStatic");
        String results, expected;
        int hammer = String2.parseInt(String2.getStringFromSystemIn(
            "Enter -1 for static tests, 1... for a hammer test, 0 for results of hammer test?"));
        if (hammer == Integer.MAX_VALUE) hammer = -1;

        //test parseDirectoryStructure
        StringArray dsColumnName = new StringArray();
        IntArray    dsN          = new IntArray();
        IntArray    dsCalendar   = new IntArray();
        parseHttpGetDirectoryStructure("5years/3MonTH/4DayS/5hr/6min/7sec/100millis/stationID", 
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
            "2016-06-21T14-15-14_2016-06-21T14-15-16.700_46088.jsonl", "");

        //set up 
        String startDir = "/data/httpGet/";
        if (hammer < 0)
            File2.deleteAllFiles(startDir, true, true); //recursive, deleteEmptySubdirectories
        parseHttpGetDirectoryStructure("stationID/2months", dsColumnName, dsN, dsCalendar);
        HashSet<String> keys = new HashSet();
        keys.add("bsimons_aSecret");
        Attributes tGlobalAttributes = new Attributes()
            .add("Conventions", "CF-1.6, COARDS, ACDD-1.3")
            .add("creator_name", "Bob Simons")
            .add("title", "Test EDDTableFromHttpGet");
        String columnNames[] = {          "stationID",   "time",                 
            "aByte",      "aChar",        "aShort",      "anInt",                 
            "aFloat",     "aDouble",      "aString",     "=123",
            TIMESTAMP,    AUTHOR,         COMMAND};
        StringArray columnNamesSA = new StringArray(columnNames);
        String columnUnits[] = {          "",           "minutes since 1980-01-01", 
            "",           "",             "m",          "days since 1985-01-01", 
            "degree_C",   EDV.TIME_UNITS, null,         "m.s-1",
            Calendar2.SECONDS_SINCE_1970, null, null};
        Class columnClasses[] = {         String.class, double.class,               
            byte.class,   char.class,     short.class,  int.class, 
            float.class,  double.class,   String.class, int.class,
            double.class, String.class,   byte.class}; 
        int nCol = columnNames.length;
        PrimitiveArray columnMvFv[] = new PrimitiveArray[nCol];               
        String columnTypes[] = new String[nCol];
        for (int col = 0; col < nCol; col++) 
            columnTypes[col] = PrimitiveArray.elementClassToString(columnClasses[col]);
        String requiredVariableNames[] = {"stationID","time"};

        Table table;

        //***  hammer the system with inserts
        if (hammer > 0) {
            long time = System.currentTimeMillis();
            int n = 2000;
            String tHammer = "" + hammer;
            if (hammer >= 10) 
                tHammer = "[" + 
                    PrimitiveArray.factory(int.class, hammer, "" + hammer).toCSVString() +
                    "]";
            String2.log(">> tHammer=" + tHammer);
            for (int i = 0; i < n; i++) {
                //error will throw exception
                results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                    keys, tGlobalAttributes,
                    columnNames, columnUnits, columnClasses, columnMvFv,
                    requiredVariableNames,
                    INSERT_COMMAND, 
                    "stationID=\"46088\"&time=" + hammer + "&aByte=" + tHammer + 
                    "&aChar=" + (char)(65) +
                    "&aShort=" + tHammer + "&anInt=" + tHammer + "&aFloat=" + tHammer + 
                    "&aDouble=" + tHammer + "&aString=" + tHammer + 
                    "&author=bsimons_aSecret",
                    null, null); //Table dirTable, Table fileTable
                if (i == 0)
                    String2.log(">> results=" + results);
            }
            String2.log("\n*** hammer(" + hammer + ") n=" + n + " finished successfully. Avg time=" +
                ((System.currentTimeMillis() - time) / (n + 0.0)) + "ms");
            return;
        }

        //***  read the hammer data
        if (hammer == 0) {
            //test the read time
            String name = startDir + "46088/46088_1980-01.jsonl";
            table = readFile(name, columnNamesSA, columnTypes, requiredVariableNames, 
                true, Double.NaN); 

            File2.copy(name, name + ".txt");
            SSR.displayInBrowser("file://" + name + ".txt");
            return;
        }

        //****** from here on are the non-hammer, static tests
        //*** test insertOrDelete
        String2.log("\n>> insertOrDelete #1: insert 1 row");
        results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
            keys, tGlobalAttributes,
            columnNames, columnUnits, columnClasses, columnMvFv,  
            requiredVariableNames,
            INSERT_COMMAND, 
            "stationID=\"46088\"&time=3.3&aByte=17.1&aChar=g" +
            "&aShort=30000.1&anInt=2&aFloat=1.23" +
            "&aDouble=1.2345678901234&aString=\"abcdefghijkl\"" + //string is nBytes long
            "&author=bsimons_aSecret",
            null, null); //Table dirTable, Table fileTable
        Test.repeatedlyTestLinesMatch(results, resultsRegex(1), "results=" + results);
        double timestamp1 = extractTimestamp(results);
        String2.log(">> results=" + results + ">> timestamp1=" + timestamp1);

        //read the data
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, Double.NaN);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl," + timestamp1 + ",bsimons,0\n";
        Test.ensureEqual(results, expected, "results=" + results);        

        Math2.sleep(1000);

        //*** add 2 rows via array
        String2.log("\n>> insertOrDelete #2: insert 2 rows via array");
        results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
            keys, tGlobalAttributes,
            columnNames, columnUnits, columnClasses, columnMvFv,  
            requiredVariableNames, INSERT_COMMAND, 
            "stationID=\"46088\"&time=[4.4,5.5]&aByte=[18.2,18.8]&aChar=[\"\u20AC\",\" \"]" +  // unicode char
            "&aShort=[30002.2,30003.3]&anInt=3&aFloat=[1.45,1.67]" +
            "&aDouble=[1.3,1.4]&aString=[\" s\n\tÃ\u20AC123\",\" \\n\\u20AC \"]" + //string is nBytes long, unicode char
            "&author=bsimons_aSecret",
            null, null); //Table dirTable, Table fileTable
        Test.repeatedlyTestLinesMatch(results, resultsRegex(2), "results=" + results);
        double timestamp2 = extractTimestamp(results);
        String2.log(">> results=" + results + ">> timestamp2=" + timestamp2);

        //read the data
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, 
            System.currentTimeMillis()/1000.0);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl," + timestamp1 + ",bsimons,0\n" +
"46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\"," + timestamp2 + ",bsimons,0\n" +
"46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \"," + timestamp2 + ",bsimons,0\n"; 
        Test.ensureEqual(results, expected, "results=" + results);        

        //read the data with timestamp from first insert
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, timestamp1);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl," + timestamp1 + ",bsimons,0\n";
        Test.ensureEqual(results, expected, "results=" + results);        


        //*** change all values in a row
        String2.log("\n>> insertOrDelete #3: change all values in a row");
        results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
            keys, tGlobalAttributes,
            columnNames, columnUnits, columnClasses, columnMvFv,  
            requiredVariableNames,
            INSERT_COMMAND, 
            "stationID=46088&time=3.3&aByte=19.9&aChar=¼" +  //stationID not in quotes    //the character itself
            "&aShort=30009.9&anInt=9&aFloat=1.99" +
            "&aDouble=1.999&aString=\"\"" + //empty string
            "&author=\"bsimons_aSecret\"",  //author in quotes
            null, null); //Table dirTable, Table fileTable
        Test.repeatedlyTestLinesMatch(results, resultsRegex(1), "results=" + results);
        double timestamp3 = extractTimestamp(results);
        String2.log(">> results=" + results + ">> timestamp3=" + timestamp3);

        //read the data
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, 
            System.currentTimeMillis()/1000.0);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,3.3,20,\\u00bc,30010,9,1.99,1.999,," + timestamp3 + ",bsimons,0\n" +
"46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\"," + timestamp2 + ",bsimons,0\n" +
"46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \"," + timestamp2 + ",bsimons,0\n";
        Test.ensureEqual(results, expected, "results=" + results);        


        //*** change values in a row but only specify a few
        String2.log("\n>> insertOrDelete #4: change a few values in a row");
        results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
            keys, tGlobalAttributes,
            columnNames, columnUnits, columnClasses, columnMvFv,  
            requiredVariableNames,
            INSERT_COMMAND, 
            "stationID=\"46088\"&time=3.3&aByte=29.9&aChar=\" \"" + 
            "&author=bsimons_aSecret",
            null, null); //Table dirTable, Table fileTable
        Test.repeatedlyTestLinesMatch(results, resultsRegex(1), "results=" + results);
        double timestamp4 = extractTimestamp(results);
        String2.log(">> results=" + results + ">> timestamp3=" + timestamp4);

        //read the data
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, 
            System.currentTimeMillis()/1000.0);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,3.3,30,\" \",,,,,," + timestamp4 + ",bsimons,0\n" +   //only low byt kept
"46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\"," + timestamp2 + ",bsimons,0\n" +
"46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \"," + timestamp2 + ",bsimons,0\n";
        Test.ensureEqual(results, expected, "results=" + results);        

        //*** delete a row
        String2.log("\n>> insertOrDelete #4: delete a row");
        results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
            keys, tGlobalAttributes,
            columnNames, columnUnits, columnClasses, columnMvFv,  
            requiredVariableNames,
            DELETE_COMMAND, 
            "stationID=\"46088\"&time=3.3" +
            "&author=bsimons_aSecret",
            null, null); //Table dirTable, Table fileTable
        Test.repeatedlyTestLinesMatch(results, resultsRegex(1), "results=" + results);
        double timestamp5 = extractTimestamp(results);
        String2.log(">> results=" + results + ">> timestamp3=" + timestamp4);

        //read the data
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, 
            System.currentTimeMillis()/1000.0);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,4.4,18,\\u20ac,30002,3,1.45,1.3,\" s\\n\\t\\u00c3\\u20ac123\"," + timestamp2 + ",bsimons,0\n" +
"46088,5.5,19,\" \",30003,3,1.67,1.4,\" \\n\\u20ac \"," + timestamp2 + ",bsimons,0\n";
        Test.ensureEqual(results, expected, "results=" + results);        

        //read the data with timestamp from first insert
        table = readFile(startDir + "46088/46088_1980-01.jsonl", 
            columnNamesSA, columnTypes, requiredVariableNames, true, timestamp1);
        results = table.dataToString();
        expected = 
"stationID,time,aByte,aChar,aShort,anInt,aFloat,aDouble,aString,timestamp,author,command\n" +
"46088,3.3,17,g,30000,2,1.23,1.2345678901234,abcdefghijkl," + timestamp1 + ",bsimons,0\n";
        Test.ensureEqual(results, expected, "results=" + results);        

        //*** test errors
        String2.log("\n>> insertOrDelete #5: expected errors");

        results = "invalid author";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A" +     
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&aDouble=1.999&aString=\"a\"" +
                "&author=zzsimons_aSecret",  //zz
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: Invalid author_key.", "");

        results = "author not last";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A" +     
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&author=bsimons_aSecret" +  
                "&aDouble=1.999&aString=\"a\"",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: author= must be the last parameter.", "");

        results = "invalid secret";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A" +     
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecretzz",  //zz
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: Invalid author_key.", "");

        results = "invalid var name";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A" +     
                "&aShort=30009.9&anInt=9&aFloatzz=1.99" + //zz
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecret",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: Query error: Unknown variable name=aFloatzz", "");

        results = "missing required var";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "time=3.3&aByte=19.9&aChar=A" +     //no stationID
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecret",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: requiredVariableName=stationID wasn't specified.", "");

        results = "invalid required var value";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"\"&time=3.3&aByte=19.9&aChar=A" +     //  ""
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecret",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: requiredVariable=stationID must have a valid value.", "");

        results = "invalid time value";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=1e14&aByte=19.9&aChar=A" +     //time is invalid
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecret",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: " +
            "ERROR in httpGetDirectoryStructure part#1: invalid time value (timeEpSec=6.0000003155328E15)!", "");

        results = "different array sizes";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A" +     
                "&aShort=30009.9&anInt=[9,10]&aFloat=[1.99,2.99,3.99]" +  //2 and 3
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecret",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: Different parameters with arrays have different sizes: 2!=3.", "");

        results = "2 vars with same name";
        try {
            results = insertOrDelete(startDir, dsColumnName, dsN, dsCalendar,
                keys, tGlobalAttributes,
                columnNames, columnUnits, columnClasses, columnMvFv,  
                requiredVariableNames,
                INSERT_COMMAND, 
                "stationID=\"46088\"&time=3.3&aByte=19.9&aChar=A" +     
                "&aShort=30009.9&anInt=9&aFloat=1.99" +
                "&aShort=30009.9" +  //duplicate
                "&aDouble=1.999&aString=\"a\"" + 
                "&author=bsimons_aSecret",
                null, null); //Table dirTable, Table fileTable
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "com.cohort.util.SimpleException: Query error: There are two parameters with variable name=aShort.", "");


     }



    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromHttpGet.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param sampleFileName the full file name of one of the files in the collection
     * @param tHttpGetRequiredVariables
     * @param tHttpGetDirectoryStructure
     * @param tHttpGetKeys
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
        String tFileDir, String sampleFileName,
        String tHttpGetRequiredVariables, String tHttpGetDirectoryStructure,
        String tHttpGetKeys,
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDTableFromHttpGet.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + 
            "\nsampleFileName=" + sampleFileName);

        String tFileNameRegex = ".*\\.jsonl";
        int tReloadEveryNMinutes = 1440;

        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        //tSortedColumnSourceName = String2.isSomething(tSortedColumnSourceName)?
        //    tSortedColumnSourceName.trim() : "";
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; 
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        dataSourceTable.readJsonlCSV(sampleFileName, null, null, true); //read all and simplify        
        //EDDTableFromHttpGet doesn't support standardizeWhat.
        int tnCol = dataSourceTable.nColumns();

        //3 required columns: TIMESTAMP, AUTHOR, COMMAND
        for (int i = 0; i < 3; i++) {
            int which = dataSourceTable.findColumnNumber(SPECIAL_VAR_NAMES[i]);
            if (which < 0)
                throw new SimpleException(
                    "One of the variables must have the name \"" +
                    SPECIAL_VAR_NAMES[which] + "\".");
        }        

        Table dataAddTable = new Table();
        double maxTimeES = Double.NaN;
        for (int c = 0; c < tnCol; c++) {
            String colName = dataSourceTable.getColumnName(c);
            PrimitiveArray sourcePA = dataSourceTable.getColumn(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            Attributes destAtts = new Attributes();
            PrimitiveArray destPA;
            //String2.log(">> colName=" + colName + " sourceClass=" + sourcePA.elementClassString());
            if (colName.equals("time")) {
                if (sourcePA.elementClass() == String.class) {
                    String tFormat = Calendar2.suggestDateTimeFormat(
                        (StringArray)sourcePA, true); //evenIfPurelyNumeric?   true since String data
                    destAtts.add("units", 
                        tFormat.length() > 0? tFormat : 
                        "yyyy-MM-dd'T'HH:mm:ss'Z'"); //default, so valid, so var name remains 'time'
                    destPA = new StringArray(sourcePA);
                } else {
                    destAtts.add("units", Calendar2.SECONDS_SINCE_1970);  //a guess
                    destPA = new DoubleArray(sourcePA);
                }
            } else if (colName.equals("latitude")) {
                destAtts.add("units", "degrees_north");
                destPA = new DoubleArray(sourcePA);
            } else if (colName.equals("longitude")) {
                destAtts.add("units", "degrees_east");
                destPA = new DoubleArray(sourcePA);
            } else if (colName.equals("depth")) {
                destAtts.add("units", "m");
                destPA = new DoubleArray(sourcePA);
            } else if (colName.equals("altitude")) {
                destAtts.add("units", "m");
                destPA = new DoubleArray(sourcePA);
            } else if (colName.equals("timestamp")) {
                destAtts.add("units", Calendar2.SECONDS_SINCE_1970);
                destAtts.add("time_precision", "1970-01-01T00:00:00.000Z");
                destPA = new DoubleArray(sourcePA);
            } else if (colName.equals("author")) {
                destAtts.add("ioos_category", "Identifier");
                destPA = new StringArray(sourcePA);
            } else if (colName.equals("command")) {
                destAtts.add("flag_values", new byte[]{0, 1});
                destAtts.add("flag_meanings", "insert delete");
                destAtts.add("ioos_category", "Other");
                destPA = new ByteArray(sourcePA);
            } else if (sourcePA.elementClass() == String.class) {
                destPA = new StringArray(sourcePA);
            } else {  //non-StringArray
                destAtts.add("units", "_placeholder");
                destPA = (PrimitiveArray)(sourcePA.clone());
            }

            if (destPA.elementClass() != String.class) 
                destAtts.add("missing_value", 
                    PrimitiveArray.factory(destPA.elementClass(), 1,
                        "" + destPA.missingValue()));
            
            //String2.log(">> in  colName= " + colName + " type=" + sourcePA.elementClassString() + " units=" + destAtts.get("units"));
            destAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), sourceAtts, destAtts, colName, 
                destPA.elementClass() != String.class, //tryToAddStandardName
                destPA.elementClass() != String.class, //addColorBarMinMax
                false); //tryToFindLLAT
            //String2.log(">> out colName= " + colName + " units=" + destAtts.get("units"));

            if ("_placeholder".equals(destAtts.getString("units")))
                destAtts.add("units", "???");
            dataAddTable.addColumn(c, colName, destPA, destAtts);

            //add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(colName, destPA, sourceAtts, destAtts);

        }
        //String2.log(">> SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
        //String2.log(">> DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (String2.isSomething(tHttpGetRequiredVariables))  externalAddGlobalAttributes.add(HTTP_GET_REQUIRED_VARIABLES, tHttpGetRequiredVariables);
        if (String2.isSomething(tHttpGetDirectoryStructure)) externalAddGlobalAttributes.add(HTTP_GET_DIRECTORY_STRUCTURE, tHttpGetDirectoryStructure);
        if (String2.isSomething(tHttpGetKeys))               externalAddGlobalAttributes.add(HTTP_GET_KEYS, tHttpGetKeys);
        if (String2.isSomething(tInfoUrl))                   externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (String2.isSomething(tInstitution))               externalAddGlobalAttributes.add("institution", tInstitution);
        if (String2.isSomething(tSummary))                   externalAddGlobalAttributes.add("summary",     tSummary);
        if (String2.isSomething(tTitle))                     externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
        
        //after dataVariables known, add global attributes in the dataAddTable
        Attributes addGlobalAtts = dataAddTable.globalAttributes();
        addGlobalAtts.set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        
        if (String2.isSomething(tHttpGetRequiredVariables))  {
            StringArray sa = StringArray.fromCSV(tHttpGetRequiredVariables);
            if (sa.size() > 0)
                addGlobalAtts.add("subsetVariables", sa.get(0));
        } else {
            addGlobalAtts.add(HTTP_GET_REQUIRED_VARIABLES,  "??? e.g., stationID, time");
        }
        if (!String2.isSomething(tHttpGetDirectoryStructure)) 
            addGlobalAtts.add(HTTP_GET_DIRECTORY_STRUCTURE, "??? e.g., stationID/2months");
        if (!String2.isSomething(tHttpGetKeys)) 
            addGlobalAtts.add(HTTP_GET_KEYS, "??? a CSV list of author_key");

        addGlobalAtts.add("testOutOfDate", "now-1day");

        //write the information
        StringBuilder sb = new StringBuilder();
        sb.append(
            "<!-- NOTE! Since JSON Lines CSV files have no metadata, you MUST edit the chunk\n" +
            "  of datasets.xml below to add all of the metadata (especially \"units\"). -->\n" +
            "<dataset type=\"EDDTableFromHttpGet\" datasetID=\"" + 
                suggestDatasetID(tFileDir +  //dirs can't be made public
                    String2.replaceAll(tFileNameRegex, '\\', '|') + //so escape chars not treated as subdirs
                    "_EDDTableFromHttpGet") +  //so different dataset types -> different md5
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>-1</updateEveryNMillis>\n" +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            //"    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            //"    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            //"    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            //"    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName></sortedColumnSourceName>\n" +  //always nothing
            "    <sortFilesBySourceNames>" +
                (String2.isSomething(tHttpGetRequiredVariables)? XML.encodeAsXML(tHttpGetRequiredVariables) : "???") +
                "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" + //safer. good for all except super frequent updates
            "    <accessibleViaFiles>true</accessibleViaFiles>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", 
            true, false)); //includeDataType, questionDestinationName
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }

    /**
     * testGenerateDatasetsXml.
     * This doesn't test suggestTestOutOfDate, except that for old data
     * it doesn't suggest anything.
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();
        String dataDir = "/u00/data/points/testFromHttpGet/";
        String sampleFile = dataDir + "testFromHttpGet.jsonl";

        try {
            String results = generateDatasetsXml(
                dataDir, sampleFile, 
                "stationID, time",
                "stationID/2months",
                "JohnSmith_JohnSmithKey, HOBOLogger_HOBOLoggerKey, QCScript59_QCScript59Key",
                "https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html",
                "NOAA NMFS SWFSC ERD",
                "This is my great summary.",
                "My Great Title",
                null) + "\n";

            String2.log(results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromHttpGet",
                dataDir, sampleFile, 
                "stationID, time",
                "stationID/2months",
                "JohnSmith_JohnSmithKey, HOBOLogger_HOBOLoggerKey, QCScript59_QCScript59Key",
                "https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html",
                "NOAA NMFS SWFSC ERD",
                "This is my great summary.",
                "My Great Title"}, 
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<!-- NOTE! Since JSON Lines CSV files have no metadata, you MUST edit the chunk\n" +
"  of datasets.xml below to add all of the metadata (especially \"units\"). -->\n" +
"<dataset type=\"EDDTableFromHttpGet\" datasetID=\"testFromHttpGet_25bf_9033_586b\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>-1</updateEveryNMillis>\n" +
"    <fileDir>/u00/data/points/testFromHttpGet/</fileDir>\n" +
"    <fileNameRegex>.*\\.jsonl</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <sortedColumnSourceName></sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>stationID, time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>true</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" +
"        <att name=\"httpGetDirectoryStructure\">stationID/2months</att>\n" +
"        <att name=\"httpGetKeys\">JohnSmith_JohnSmithKey, HOBOLogger_HOBOLoggerKey, QCScript59_QCScript59Key</att>\n" +
"        <att name=\"httpGetRequiredVariables\">stationID, time</att>\n" +
"        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html</att>\n" +
"        <att name=\"institution\">NOAA NMFS SWFSC ERD</att>\n" +
"        <att name=\"keywords\">air, airTemp, author, center, command, data, erd, fisheries, great, identifier, marine, national, nmfs, noaa, science, service, southwest, station, stationID, swfsc, temperature, time, timestamp, title, water, waterTemp</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"subsetVariables\">stationID</att>\n" +
"        <att name=\"summary\">This is my great summary. NOAA National Marine Fisheries Service (NMFS) Southwest Fisheries Science Center (SWFSC) ERD data from a local source.</att>\n" +
"        <att name=\"testOutOfDate\">now-1day</att>\n" +
"        <att name=\"title\">My Great Title</att>\n" +
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
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss&#39;Z&#39;</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>airTemp</sourceName>\n" +
"        <destinationName>airTemp</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Air Temp</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>waterTemp</sourceName>\n" +
"        <destinationName>waterTemp</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Water Temp</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>timestamp</sourceName>\n" +
"        <destinationName>timestamp</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Timestamp</att>\n" +
"            <att name=\"missing_value\" type=\"double\">NaN</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00.000Z</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>author</sourceName>\n" +
"        <destinationName>author</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Author</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>command</sourceName>\n" +
"        <destinationName>command</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"flag_meanings\">insert delete</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Command</att>\n" +
"            <att name=\"missing_value\" type=\"byte\">127</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n\n\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            String tDatasetID = "testFromHttpGet_25bf_9033_586b";
            EDD.deleteCachedDatasetInfo(tDatasetID);
            //delete the data files (but not the seed data file)
            File2.deleteAllFiles(dataDir + "station1", true, true);
            File2.deleteAllFiles(dataDir + "station2", true, true);

            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), tDatasetID, "");
            Test.ensureEqual(edd.title(), "My Great Title", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "stationID, time, airTemp, waterTemp, timestamp, author, command",
                "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }

    }



    /**
     * This does basic tests of this class.
     * Note that ü in utf-8 is \xC3\xBC or [195][188]
     * Note that Euro is \\u20ac (and low byte is #172 is \\u00ac -- I worked to encode as '?')
     *
     * @throws Throwable if trouble
     */
    public static void testBasic() throws Throwable {
        String2.log("\n****************** EDDTableFromHttpGet.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String dataDir = "/u00/data/points/testFromHttpGet/";
        String dir = EDStatic.fullTestCacheDirectory;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = true;

        String id = "testFromHttpGet"; 
        deleteCachedDatasetInfo(id);
        //delete the data files (but not the seed data file)
        File2.deleteAllFiles(dataDir + "station1", true, true);
        File2.deleteAllFiles(dataDir + "station2", true, true);
        File2.delete(dataDir + "station1");
        File2.delete(dataDir + "station2");
        //String2.pressEnterToContinue();

        EDDTableFromHttpGet eddTable = (EDDTableFromHttpGet)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** EDDTableFromHttpGet.testBasic  test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  stationID {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.529946e+9, 1.529946e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 10.2, 10.2;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -150.3, -150.3;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  airTemp {\n" +
"    Float32 actual_range 14.2, 14.2;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temp\";\n" +
"    Float32 missing_value NaN;\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  waterTemp {\n" +
"    Float32 actual_range 12.2, 12.2;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Water Temp\";\n" +
"    Float32 missing_value NaN;\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  timestamp {\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Timestamp\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00.000Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  author {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Author\";\n" +
"  }\n" +
"  command {\n" +
"    Byte actual_range 0, 0;\n" +
"    String flag_meanings \"insert delete\";\n" +
"    Byte flag_values 0, 1;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Command\";\n" +
"    Byte missing_value 127;\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"stationID, latitude, longitude\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_type \"institution\";\n" +
"    String creator_url \"https://www.pfeg.noaa.gov\";\n" +
"    Float64 Easternmost_Easting -150.3;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 10.2;\n" +
"    Float64 geospatial_lat_min 10.2;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -150.3;\n" +
"    Float64 geospatial_lon_min -150.3;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =
"    String httpGetDirectoryStructure \"stationID/2months\";\n" +
"    String httpGetRequiredVariables \"stationID, time\";\n" +
"    String infoUrl \"https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"air, airTemp, author, center, command, data, erd, fisheries, great, identifier, latitude, longitude, marine, national, nmfs, noaa, science, service, southwest, station, stationID, swfsc, temperature, time, timestamp, title, water, waterTemp\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 10.2;\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 10.2;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String subsetVariables \"stationID, longitude, latitude\";\n" +
"    String summary \"This is my great summary. NOAA National Marine Fisheries Service (NMFS) Southwest Fisheries Science Center (SWFSC) ERD data from a local source.\";\n" +
"    String testOutOfDate \"now-1day\";\n" +
"    String time_coverage_end \"2018-06-25T17:00:00Z\";\n" +
"    String time_coverage_start \"2018-06-25T17:00:00Z\";\n" +
"    String title \"My Great Title\";\n" +
"    Float64 Westernmost_Easting -150.3;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            eddTable.className() + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String stationID;\n" +
"    Float64 time;\n" +
"    Float64 latitude;\n" +
"    Float64 longitude;\n" +
"    Float32 airTemp;\n" +
"    Float32 waterTemp;\n" +
"    Float64 timestamp;\n" +
"    String author;\n" +
"    Byte command;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n*** EDDTableFromHttpGet.testBasic make DATA FILES\n");       

        //.csv  all data (just the seed data)
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n" +
",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n" +
"myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z,me,0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //initial file table
        Table tFileTable = eddTable.tryToLoadDirFileTable(datasetDir(id) + FILE_TABLE_FILENAME); 
        results = tFileTable.dataToString();
        String2.log(results);
        expected = 
"dirIndex,fileName,lastMod,size,sortedSpacing,stationID_min_,stationID_max_,stationID_hasNaN_,time_min_,time_max_,time_hasNaN_,x3d10x2e2_min_,x3d10x2e2_max_,x3d10x2e2_hasNaN_,x3dx2d150x2e3_min_,x3dx2d150x2e3_max_,x3dx2d150x2e3_hasNaN_,airTemp_min_,airTemp_max_,airTemp_hasNaN_,waterTemp_min_,waterTemp_max_,waterTemp_hasNaN_,timestamp_min_,timestamp_max_,timestamp_hasNaN_,author_min_,author_max_,author_hasNaN_,command_min_,command_max_,command_hasNaN_\n" +
"0,testFromHttpGet.jsonl,1530629894000,144,-1.0,myStation,myStation,0,2018-06-25T17:00:00Z,2018-06-25T17:00:00Z,0,,,,,,,14.2,14.2,0,12.2,12.2,0,0.0,0.0,0,me,me,0,0,0,0\n";
        Test.repeatedlyTestLinesMatch(results, expected, "");

        //push a bunch of data into the dataset: 2 stations, 2 time periods
        for (int i = 0; i < 4; i++) {
            //apr
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station1&time=2016-04-29T0" + i + ":00:00Z" +
                "&airTemp=10." + i + 
                "&waterTemp=11." + i + "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_insert1_" + i, ".insert"); 
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station2&time=2016-04-29T0" + i + ":00:00Z" +
                "&airTemp=12." + i + 
                "&waterTemp=13." + i + "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_insert2_" + i, ".insert");

            //may
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station1&time=2016-05-29T0" + i + ":00:00Z" +
                "&airTemp=14." + i + 
                "&waterTemp=15." + i + "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_insert3_" + i, ".insert"); 
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station2&time=2016-05-29T0" + i + ":00:00Z" +
                "&airTemp=16." + i + 
                "&waterTemp=17." + i + "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_insert4_" + i, ".insert");
        }

        //look at last response file
        results = String2.directReadFrom88591File(dir + tName);
        String2.log(results);
        expected = 
"\\{\n" +
"\"status\":\"success\",\n" +
"\"nRowsReceived\":1,\n" +
"\"stringTimestamp\":\"" + today + "\\d{2}:\\d{2}\\.\\d{3}Z\",\n" +
"\"numericTimestamp\":1\\.\\d{10,12}+E9\n" +  //possible but unlikely that millis=0 by chance; if so, try again
"\\}\n";
        Test.repeatedlyTestLinesMatch(results, expected, "");

        tFileTable = eddTable.tryToLoadDirFileTable(datasetDir(id) + FILE_TABLE_FILENAME); 
        results = tFileTable.dataToString();
        String2.log(results);
        expected = 
"dirIndex,fileName,lastMod,size,sortedSpacing,stationID_min_,stationID_max_,stationID_hasNaN_,time_min_,time_max_,time_hasNaN_,x3d10x2e2_min_,x3d10x2e2_max_,x3d10x2e2_hasNaN_,x3dx2d150x2e3_min_,x3dx2d150x2e3_max_,x3dx2d150x2e3_hasNaN_,airTemp_min_,airTemp_max_,airTemp_hasNaN_,waterTemp_min_,waterTemp_max_,waterTemp_hasNaN_,timestamp_min_,timestamp_max_,timestamp_hasNaN_,author_min_,author_max_,author_hasNaN_,command_min_,command_max_,command_hasNaN_\n" +
"0,testFromHttpGet.jsonl,1530629894000,144,-1.0,myStation,myStation,0,2018-06-25T17:00:00Z,2018-06-25T17:00:00Z,0,,,,,,,14.2,14.2,0,12.2,12.2,0,0.0,0.0,0,me,me,0,0,0,0\n" +
"1,station1_2016-03.jsonl,15\\d+,37.,1.0,station1,station1,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,10.0,10.3,0,11.0,11.3,0,1.5\\d+E9,1.5\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n" +
"2,station2_2016-03.jsonl,15\\d+,37.,1.0,station2,station2,0,2016-04-29T00:00:00Z,2016-04-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,12.0,12.3,0,13.0,13.3,0,1.5\\d+E9,1.5\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n" +
"1,station1_2016-05.jsonl,15\\d+,37.,1.0,station1,station1,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,14.0,14.3,0,15.0,15.3,0,1.5\\d+E9,1.5\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n" +
"2,station2_2016-05.jsonl,15\\d+,37.,1.0,station2,station2,0,2016-05-29T00:00:00Z,2016-05-29T03:00:00Z,0,10.2,10.2,0,-150.3,-150.3,0,16.0,16.3,0,17.0,17.3,0,1.5\\d+E9,1.5\\d+E9,0,JohnSmith,JohnSmith,0,0,0,0\n";
        Test.repeatedlyTestLinesMatch(results, expected, "");


        //.csv  all data 
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all2", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        String2.log(results);
        long versioningTime = System.currentTimeMillis();
        Math2.sleep(1);
        String versioningExpected =
"stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n" +
",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n" +
"myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z,me,0\n" +
"station1,2016-04-29T00:00:00Z,10.2,-150.3,10.0,11.0," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-04-29T01:00:00Z,10.2,-150.3,10.1,11.1," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-04-29T00:00:00Z,10.2,-150.3,12.0,13.0," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-04-29T01:00:00Z,10.2,-150.3,12.1,13.1," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-05-29T00:00:00Z,10.2,-150.3,14.0,15.0," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-05-29T01:00:00Z,10.2,-150.3,14.1,15.1," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-05-29T00:00:00Z,10.2,-150.3,16.0,17.0," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-05-29T01:00:00Z,10.2,-150.3,16.1,17.1," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n" +
"station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3," + today + "\\d{2}:\\d{2}\\.\\d{3}Z,JohnSmith,0\n";
        Test.repeatedlyTestLinesMatch(results, versioningExpected, "\nresults=\n" + results);


        //overwrite and delete a bunch of data
        for (int i = 0; i < 2; i++) {
            //overwrite the first 2
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station1&time=2016-04-29T0" + i + ":00:00Z" +
                "&airTemp=20." + i + 
                "&waterTemp=21." + i + "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_insert5_" + i, ".insert"); 
            //delete the first 2
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station2&time=2016-04-29T0" + i + ":00:00Z" +
                "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_delete6_" + i, ".delete");

            //overwrite the first 2
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station1&time=2016-05-29T0" + i + ":00:00Z" +
                "&airTemp=22." + i + 
                "&waterTemp=23." + i + "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_insert7_" + i, ".insert"); 
            //delete the first 2
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "stationID=station2&time=2016-05-29T0" + i + ":00:00Z" +
                "&author=JohnSmith_JohnSmithKey", 
                dir, eddTable.className() + "_delete8_" + i, ".delete");
        }

        //.csv  all data (with processing)
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all3", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"stationID,time,latitude,longitude,airTemp,waterTemp,timestamp,author,command\n" +
",UTC,degrees_north,degrees_east,degree_C,degree_C,UTC,,\n" +
"myStation,2018-06-25T17:00:00Z,10.2,-150.3,14.2,12.2,1970-01-01T00:00:00.000Z,me,0\n" +
"station1,2016-04-29T00:00:00Z,10.2,-150.3,20.0,21.0,.{24},JohnSmith,0\n" +  //changed
"station1,2016-04-29T01:00:00Z,10.2,-150.3,20.1,21.1,.{24},JohnSmith,0\n" +
"station1,2016-04-29T02:00:00Z,10.2,-150.3,10.2,11.2,.{24},JohnSmith,0\n" +
"station1,2016-04-29T03:00:00Z,10.2,-150.3,10.3,11.3,.{24},JohnSmith,0\n" +
"station2,2016-04-29T02:00:00Z,10.2,-150.3,12.2,13.2,.{24},JohnSmith,0\n" + //hours 00 01 deleted
"station2,2016-04-29T03:00:00Z,10.2,-150.3,12.3,13.3,.{24},JohnSmith,0\n" +
"station1,2016-05-29T00:00:00Z,10.2,-150.3,22.0,23.0,.{24},JohnSmith,0\n" + //changed
"station1,2016-05-29T01:00:00Z,10.2,-150.3,22.1,23.1,.{24},JohnSmith,0\n" +
"station1,2016-05-29T02:00:00Z,10.2,-150.3,14.2,15.2,.{24},JohnSmith,0\n" +
"station1,2016-05-29T03:00:00Z,10.2,-150.3,14.3,15.3,.{24},JohnSmith,0\n" +
"station2,2016-05-29T02:00:00Z,10.2,-150.3,16.2,17.2,.{24},JohnSmith,0\n" + //hours 00 01 deleted
"station2,2016-05-29T03:00:00Z,10.2,-150.3,16.3,17.3,.{24},JohnSmith,0\n";
        Test.repeatedlyTestLinesMatch(results, expected, "\nresults=\n" + results);

        //similar: versioning as of now
        userDapQuery = "&timestamp<=" + (System.currentTimeMillis()/1000.0);    //as millis
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all3b", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        Test.repeatedlyTestLinesMatch(results, expected, "\nresults=\n" + results);

        //similar: versioning as of now
        userDapQuery = "&timestamp<=" + 
            Calendar2.epochSecondsToIsoStringT3Z(System.currentTimeMillis()/1000.0);    //as ISO
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all3c", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        Test.repeatedlyTestLinesMatch(results, expected, "\nresults=\n" + results);

        //raw read a data file 
        results = String2.directReadFromUtf8File(
            "/u00/data/points/testFromHttpGet/station2/station2_2016-05.jsonl");
        //String2.log(results);
        expected = 
"\\[\"stationID\",\"time\",\"airTemp\",\"waterTemp\",\"timestamp\",\"author\",\"command\"\\]\n" +
"\\[\"station2\",\"2016-05-29T00:00:00Z\",16,17,1.5\\d+E9,\"JohnSmith\",0\\]\n" +
"\\[\"station2\",\"2016-05-29T01:00:00Z\",16.1,17.1,1.5\\d+E9,\"JohnSmith\",0\\]\n" +
"\\[\"station2\",\"2016-05-29T02:00:00Z\",16.2,17.2,1.5\\d+E9,\"JohnSmith\",0\\]\n" +
"\\[\"station2\",\"2016-05-29T03:00:00Z\",16.3,17.3,1.5\\d+E9,\"JohnSmith\",0\\]\n" +
"\\[\"station2\",\"2016-05-29T00:00:00Z\",null,null,1.5\\d+E9,\"JohnSmith\",1\\]\n" +
"\\[\"station2\",\"2016-05-29T01:00:00Z\",null,null,1.5\\d+E9,\"JohnSmith\",1\\]\n";
        Test.repeatedlyTestLinesMatch(results, expected, "\nresults=\n" + results);


        //.csv  (versioning: as of previous time)
        userDapQuery = "&timestamp<=" + (versioningTime/1000.0);
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddTable.className() + "_all5", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        Test.repeatedlyTestLinesMatch(results, versioningExpected, "\nresults=\n" + results);

        
        /* */
        reallyVerbose = oReallyVerbose;

    }



    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
/* for releases, this line should have open/close comment */

//FUTURE: command=2=addIfNew: adds a row if it is a new combination of
//  requiredVariables (after processing the jsonl file)
//  E.g., Use addIfNew to "add" all of the data from NdbcMet last5days file.

        testStatic(); 
        testGenerateDatasetsXml();
        testBasic(); 
        /* */
    }
}

