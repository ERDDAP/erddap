/* 
 * EDDTableFromHyraxFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.ShortArray;
import com.cohort.array.LongArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.*;
import org.joda.time.format.*;

/** 
 * This class represents a directory tree with a collection of n-dimensional (1,2,3,4,...) 
 * opendap files, each of which is flattened into a table.
 * For example, http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/M09/pentad_19870903_v11l35flk.nc.gz.html
 * (the three dimensions there are e.g., time,lat,lon).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-08
 */
public class EDDTableFromHyraxFiles extends EDDTableFromFiles { 

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;

    /** Format for hyrax date time, e.g., 03-Apr-2009 17:15, 2009-01-03 12:03:02 */
    protected DateTimeFormatter[] dateTimeFormatters;
    protected Pattern[] patterns;

    //info from last getFileNames
    private StringArray lastFileDirs;
    //these three are have matching values for each file:
    private ShortArray  lastFileDirIndex; 
    private StringArray lastFileName; 
    private LongArray   lastFileLastMod; 

    private String[] sourceAxisNames;

    /** 
     * The constructor just calls the super constructor. 
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     */
    public EDDTableFromHyraxFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, 
        Attributes tAddGlobalAttributes,
        double tAltMetersPerSourceUnit, 
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ) 
        throws Throwable {

        super("EDDTableFromHyraxFiles", false, tDatasetID, tAccessibleTo, tOnChange, 
            tAddGlobalAttributes, tAltMetersPerSourceUnit, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ);

    }

    /**
     * This gets the file names from Hyrax catalog directory URL.
     * This overrides the superclass version of this method.
     *
     * @param fileDir for this version of this method, fileDir is the
     *  url of the base web page
     *  e.g., http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/
     *
     * @returns an array with a list of full file names 
     * @throws Throwable if trouble
     */
    public String[] getFileNames(String fileDir, String fileNameRegex, boolean recursive) throws Throwable {
        if (reallyVerbose) String2.log("EDDTableFromHyraxFiles getFileNames");
        //clear the stored file info
        lastFileDirs     = new StringArray();
        lastFileDirIndex = new ShortArray(); 
        lastFileName     = new StringArray(); 
        lastFileLastMod  = new LongArray(); 

        //This is used by superclass' constructor, so must be made here, instead of where defined
        //Format for date time in hyrax .html lists of files, e.g., 2009-04-23 18:56:56, was 03-Apr-2009 17:15 
        //timeZone doesn't matter as long as consistent for a given instance
        if (dateTimeFormatters == null) {
            dateTimeFormatters = new DateTimeFormatter[3];
            dateTimeFormatters[0] = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(DateTimeZone.UTC);
            dateTimeFormatters[1] = DateTimeFormat.forPattern("yyyy-MM-ddTHH:mm:ss").withZone(DateTimeZone.UTC);
            dateTimeFormatters[2] = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm").withZone(DateTimeZone.UTC);

            patterns = new Pattern[3];
            patterns[0] = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"); //yyyy-MM-dd HH:mm:ss
            patterns[1] = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"); //yyyy-MM-dd HH:mm:ss
            patterns[2] = Pattern.compile("\\d{2}-[a-zA-Z]{3}-\\d{4} \\d{2}:\\d{2}"); //dd-MMM-yyyy HH:mm
        }

        //call the recursive method
        getFileNameInfo(fileDir, fileNameRegex, recursive);

        //assemble the fileNames
        int n = lastFileDirIndex.size();
        String tNames[] = new String[n];
        for (int i = 0; i < n; i++) 
            tNames[i] = lastFileDirs.array[lastFileDirIndex.array[i]] + lastFileName.array[i];
        return tNames;
    }

    /**
     * This does the work for getFileNames.
     * This calls itself recursively, adding into to fileNameInfo as it is found.
     *
     * @param url the url of the current web page (with a hyrax catalog)
     * @throws Throwable if trouble, e.g., if url doesn't respond
     */
    private void getFileNameInfo(String url, String fileNameRegex, boolean recursive) throws Throwable {
        if (reallyVerbose) String2.log("\ngetFileNameInfo url=" + url + " regex=" + fileNameRegex);
        String response = SSR.getUrlResponseString(url);
        String responseLC = response.toLowerCase();
        int nMatchers = patterns.length;
        int firstMatcher = 0;
        int lastMatcher = nMatchers - 1;

        //skip header line and parent directory
        int po = responseLC.indexOf("parent directory");  //Lower Case
        if (po < 0 ) {
            if (reallyVerbose) String2.log("  \"parent directory\" not found.");
            return;
        }
        po += 18;

        //endPre
        int endPre = responseLC.indexOf("</pre>", po); //Lower Case
        if (endPre < 0) 
            endPre = response.length();

        //go through file,dir listings
        while (true) {
            //<A HREF="http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/">ANO001/</a> 2009-04-05T17:15:00  673K
            po = responseLC.indexOf("<a href=\"", po);      //Lower Case
            int po2 = responseLC.indexOf("\">", po + 9);    //Lower Case
            int po3 = responseLC.indexOf("</a>", po2 + 2);  //Lower Case
            if (po < 0 || po2 < 0 || po3 < 0 || po > endPre) {
                if (reallyVerbose) String2.log("  no more href's found.");
                return;  //no more href's
            }
            po += 9;
            int po4 = po3 + 4;

            String tUrl = response.substring(po, po2);
            if (!tUrl.startsWith("http")) //it's a filename relative to this dir
                tUrl = File2.getDirectory(url) + tUrl;
            if (reallyVerbose) String2.log("  found url=" + tUrl);
            if (tUrl.endsWith("/") ||
                tUrl.endsWith("/contents.html")) {
                //it's a directory
                if (recursive)
                    getFileNameInfo(tUrl, fileNameRegex, recursive);

            } else if (tUrl.endsWith(".html")) {

                //it's a web page (perhaps a DAP .html page)
                tUrl = tUrl.substring(0, tUrl.length() - 5);
                String tDir = File2.getDirectory(tUrl);
                String tName = File2.getNameAndExtension(tUrl);
                if (tName.matches(fileNameRegex)) {
                    //it's a match!
                    long tLastMod = 0;
                    String sourceTime = "";
                    
                    //search for date   (must be before next "<a href")
                    int nextHrefPo = responseLC.indexOf("<a href=\"", po);      //Lower Case
                    if (nextHrefPo < 0) 
                        nextHrefPo = endPre;
                    String responseChunk = response.substring(po4, nextHrefPo);
                    for (int m = firstMatcher; m <= lastMatcher; m++) {
                        Matcher matcher = patterns[m].matcher(responseChunk); //search a limited-length string
                        if (matcher.find()) {
                            try {
                                sourceTime = response.substring(po4 + matcher.start(), po4 + matcher.end());
                                tLastMod = dateTimeFormatters[m].parseMillis(sourceTime);
                                firstMatcher = m; //after a success, just use that matcher
                                lastMatcher = m;
                                break;
                            } catch (Throwable t) {
                                if (verbose) String2.log("getFileName error while parsing sourceTime=\"" + 
                                    sourceTime + "\"\n" + MustBe.throwableToString(t));
                            }
                        }
                    }
                    if (reallyVerbose) String2.log("    MatchedRegex=true!  sourceTime=\"" + 
                        sourceTime + "\" -> lastMod=" + tLastMod);

                    //only keep if it has a valid sourceTime/lastMod
                    if (tLastMod > 0) {
                        if (verbose) String2.log("    keep " + tDir + tName);

                        //add to file list
                        int tDirIndex = lastFileDirs.indexOf(tDir);
                        if (tDirIndex == -1) {
                            tDirIndex = lastFileDirs.size();
                            lastFileDirs.add(tDir);
                        }
                        lastFileDirIndex.add((short)tDirIndex);
                        lastFileName.add(tName); 
                        lastFileLastMod.add(tLastMod);
                    }
                } else {
                    if (reallyVerbose) String2.log("    MatchedRegex=false");
                }

            }
            po = po4;
        }
    }

    /**
     * This overrides the superclass's version and returns info for files/urls from last getFileNames.
     *
     * @return the time (millis since the start of the Unix epoch) 
     *    the file was last modified 
     *    (or 0 if trouble)
     */
    public long getFileLastModified(String tDir, String tName) {
        int dirIndex = lastFileDirs.indexOf(tDir);
        if (dirIndex < 0) {
            if (verbose) String2.log("EDDTableFromHyraxFiles.getFileLastModified can't find dir=" + tDir);
            return 0;
        }

        //linear search through fileNames
        int n = lastFileDirIndex.size();
        for (int i = 0; i < n; i++) {
            if (lastFileDirIndex.array[i] == dirIndex &&
                lastFileName.array[i].equals(tName)) 
                return lastFileLastMod.array[i];
        }
        if (verbose) String2.log("EDDTableFromHyraxFiles.getFileLastModified can't find file=" + tName );
        return 0;
    }


    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     *
     */
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        if (reallyVerbose) String2.log("getSourceDataFromFile " + fileDir + fileName);
        DConnect dConnect = new DConnect(fileDir + fileName, acceptDeflate, 1, 1);
        DAS das = null;
        DDS dds = null;
        Table table = new Table();
        if (getMetadata) {
            if (das == null)
                das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
            OpendapHelper.getAttributes(das, "GLOBAL", table.globalAttributes());
        }

        //first time: get axisVariables, ensure all vars use same sourceAxisNames
        if (sourceAxisNames == null) {
            if (reallyVerbose) String2.log("one time: getSourceAxisNames");
            if (dds == null)
                dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
            for (int v = 0; v < sourceDataNames.size(); v++) {
                //find a multidimensional var
                String tSourceAxisNames[] = null;
                String tName = sourceDataNames.get(v);
                BaseType baseType = dds.getVariable(tName);
                if (baseType instanceof DGrid) {
                    //dGrid has main dArray + dimensions
                    DGrid dGrid = (DGrid)baseType;
                    int nEl = dGrid.elementCount(true);
                    tSourceAxisNames = new String[nEl - 1];
                    for (int el = 1; el < nEl; el++) //1..
                        tSourceAxisNames[el - 1] = dGrid.getVar(el).getName();
                } else if (baseType instanceof DArray) {
                    //dArray is usually 1 dim, but may be multidimensional
                    DArray dArray = (DArray)baseType;
                    int nDim = dArray.numDimensions();
                    if (nDim > 1) {
                        tSourceAxisNames = new String[nDim];
                        for (int d = 0; d < nDim; d++) {//0..
                            tSourceAxisNames[d] = dArray.getDimension(d).getName();
                            if (tSourceAxisNames[d] == null || tSourceAxisNames[d].equals(""))
                                tSourceAxisNames[d] = "" + dArray.getDimension(d).getSize();
                        }
                    }
                }

                //ok?
                if (tSourceAxisNames == null) {
                    //this var isn't multidimensional
                } else if (sourceAxisNames == null) {
                    //first multidimensional var; use its sourceAxisNames
                    sourceAxisNames = tSourceAxisNames; 
                } else {
                    //verify same
                    Test.ensureEqual(tSourceAxisNames, sourceAxisNames, 
                        "The dimensions for all multidimensional variables must be the same (" +
                        tName + " is different)."); 

                }
            }
            if (sourceAxisNames == null)
                throw new RuntimeException("Error: no multidimensional variables in initial data request.");
            if (!sourceAxisNames[0].equals(sortedColumnSourceName))
                throw new RuntimeException("Error: sortedColumnSourceName must be axisVariable[0]'s name.");
        }

        //if don't have to get actual data, just get all values of axis0, min,max of other axes, and mv for others
        int tNDim = -1;
        if (!mustGetData) {
            if (dds == null)
                dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

            //get all the axis values at once
            PrimitiveArray axisPa[] = OpendapHelper.getAxisValues(dConnect, sourceAxisNames, "");

            //go through the sourceDataNames
            for (int dv = 0; dv < sourceDataNames.size(); dv++) {
                String tName = sourceDataNames.get(dv);
                BaseType baseType;
                try {
                    baseType = dds.getVariable(tName);
                } catch (Throwable t) {
                    if (verbose) String2.log("variable=" + tName + " not found in this file.");
                    continue;
                }
                Attributes atts = new Attributes();
                if (getMetadata)
                    OpendapHelper.getAttributes(das, tName, atts);
                PrimitiveArray pa;
                int po = String2.indexOf(sourceAxisNames, tName);
                if (po >= 0) {
                    pa = axisPa[po];
                } else if (tName.equals(sortedColumnSourceName)) {
                    //sortedColumn    get all values
                    pa = OpendapHelper.getPrimitiveArray(dConnect, "?" + tName);
                } else if (baseType instanceof DGrid) {
                    //multidimensional   don't get any values
                    pa = PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[dv]), 2, false);
                    pa.addString(pa.minValue());
                    pa.addString(pa.maxValue());
                } else if (baseType instanceof DArray) {                   
                    DArray dArray = (DArray)baseType;
                    if (dArray.numDimensions() == 1) {
                        //if 1 dimension, get min and max
                        //This didn't work: tSize was 0!
                        //int tSize = dArray.getDimension(0).getSize();
                        //String query = "?" + tName + "[0:" + (tSize - 1) + ":" + (tSize - 1) + "]";
                        //so get all values
                        String query = "?" + tName;
                        if (reallyVerbose) String2.log("  query=" + query);
                        pa = OpendapHelper.getPrimitiveArray(dConnect, query);
                    } else {
                        //if multidimensional   don't get any values
                        pa = PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[dv]), 2, false);
                        pa.addString(pa.minValue());
                        pa.addString(pa.maxValue());
                    }
                } else {
                    String2.log("Ignoring variable=" + tName + " because of unexpected baseType=" + baseType);
                    continue;
                }
                //String pas = pa.toString();
                //String2.log("\n" + tName + "=" + pas.substring(0, Math.min(pas.length(), 60)));
                table.addColumn(table.nColumns(), tName, pa, atts);
            }
            table.makeColumnsSameSize(); //deals with axes with 1 value, or var not in this file
            //String2.log(table.toString("row", 5));            
            return table;        
        }

        //need to get the requested data
        //calculate firstRow, lastRow of leftmost axis from minSorted, maxSorted
        int firstRow = 0;
        int lastRow = -1; 
        if (sortedSpacing >= 0 && !Double.isNaN(minSorted)) {
            //min/maxSorted is active       
            //get axis0 values
            PrimitiveArray pa = OpendapHelper.getPrimitiveArray(dConnect, "?" + sourceAxisNames[0]);
            firstRow = pa.binaryFindClosest(minSorted);
            lastRow = minSorted == maxSorted? firstRow : pa.binaryFindClosest(maxSorted);
            if (reallyVerbose) String2.log("  binaryFindClosest n=" + pa.size() + 
                " reqMin=" + minSorted + " firstRow=" + firstRow + 
                " reqMax=" + maxSorted + " lastRow=" + lastRow);
        } else {
            if (reallyVerbose) String2.log("  getting all rows since sortedSpacing=" + 
                sortedSpacing + " and minSorted=" + minSorted);
        }

        //dds is always needed for stuff below
        if (dds == null)
            dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        //build String form of the constraint
        String axis0Constraint = lastRow == -1? "" : "[" + firstRow + ":" + lastRow + "]";
        StringBuilder constraintSB = new StringBuilder();
        if (lastRow != -1) {
            constraintSB.append(axis0Constraint);
            for (int av = 1; av < sourceAxisNames.length; av++) {
                //dap doesn't allow requesting all via [], so get each dim's size
                //String2.log("  dds.getVar " + sourceAxisNames[av]);
                BaseType bt = dds.getVariable(sourceAxisNames[av]);
                DArray dArray = (DArray)bt;
                constraintSB.append("[0:" + (dArray.getDimension(0).getSize()-1) + "]");
            }
        }
        String constraint = constraintSB.toString();

        //get non-axis variables
        for (int dv = 0; dv < sourceDataNames.size(); dv++) {
            //???why not get all the dataVariables at once?
            //thredds has (and other servers may have) limits to the size of a given request
            //so breaking into parts avoids the problem.
            //Also, I check if each var is in this file. Var missing from a given file is allowed.
            String tName = sourceDataNames.get(dv);
            if (String2.indexOf(sourceAxisNames, tName) >= 0)
                continue;

            //ensure the variable is in this file
            try {
                BaseType baseType = dds.getVariable(tName);
            } catch (Throwable t) {
                if (verbose) String2.log("variable=" + tName + " not found.");
                continue;
            }

            //get the data
            PrimitiveArray pa[] = OpendapHelper.getPrimitiveArrays(dConnect, 
                "?" + tName + constraint);

            if (table.nColumns() == 0) {
                //first var: make axis columns
                int shape[] = new int[sourceAxisNames.length];
                for (int av = 0; av < sourceAxisNames.length; av++) {
                    String aName = sourceAxisNames[av];
                    Attributes atts = new Attributes();
                    if (getMetadata)
                        OpendapHelper.getAttributes(das, aName, atts);
                    shape[av] = pa[av+1].size();
                    table.addColumn(av, aName, 
                        PrimitiveArray.factory(pa[av+1].elementClass(), 128, false), atts);
                }
                //expand axis values into results table via NDimensionalIndex
                NDimensionalIndex ndIndex = new NDimensionalIndex(shape);
                int current[] = ndIndex.getCurrent();
                while (ndIndex.increment()) {
                    for (int av = 0; av < sourceAxisNames.length; av++) 
                        table.getColumn(av).addDouble(pa[av+1].getDouble(current[av]));
                }
            }  //subsequent vars: I could check that axis values are consistent

            //store dataVar column
            if (pa[0].size() != table.nRows()) 
                throw new RuntimeException("Data source error: nValues returned for " + tName + 
                    " wasn't expectedNValues=" + table.nRows());
            Attributes atts = new Attributes();
            if (getMetadata)
                OpendapHelper.getAttributes(das, tName, atts);
            table.addColumn(table.nColumns(), tName, pa[0], atts);
        }
        if (table.nColumns() > 0)
            return table;

        //no data vars? create a table with columns for sourceAxisNames only
        PrimitiveArray axisPa[] = OpendapHelper.getAxisValues(dConnect, sourceAxisNames, axis0Constraint);
        int shape[] = new int[sourceAxisNames.length];
        for (int av = 0; av < sourceAxisNames.length; av++) {
            String aName = sourceAxisNames[av];
            Attributes atts = new Attributes();
            if (getMetadata)
                OpendapHelper.getAttributes(das, aName, atts);
            shape[av] = axisPa[av].size();
            table.addColumn(av, aName, 
                PrimitiveArray.factory(axisPa[av].elementClass(), 128, false), atts);
        }
        //expand axis values into results table via NDimensionalIndex
        NDimensionalIndex ndIndex = new NDimensionalIndex(shape);
        int current[] = ndIndex.getCurrent();
        while (ndIndex.increment()) {
            for (int av = 0; av < sourceAxisNames.length; av++) 
                table.getColumn(av).addDouble(axisPa[av].getDouble(current[av]));
        }
        return table;

    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromHyraxFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalDirUrl the locally useful starting (parent) directory with a 
     *    Hyrax sub-catalog for searching for files
     *   e.g., http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *   e.g, "pentad.*\\.nc\\.gz"
     * @param oneFileDapUrl  the locally useful url for one file, without ending .das or .html
     *   e.g., http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/M09/pentad_19870908_v11l35flk.nc.gz
     * @param tReloadEveryNMinutes
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @throws Throwable if trouble
     */
    public static String generateDatasetsXml(String tLocalDirUrl, 
        String tFileNameRegex, String oneFileDapUrl, int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, Attributes externalAddGlobalAttributes) 
        throws Throwable {

        String2.log("EDDTableFromHyraxFiles.generateDatasetsXml" +
            "\n  tLocalDirUrl=" + tLocalDirUrl + 
            "\n  oneFileDapUrl=" + oneFileDapUrl);
        String tPublicDirUrl = convertToPublicSourceUrl(tLocalDirUrl);

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        DConnect dConnect = new DConnect(oneFileDapUrl, acceptDeflate, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);;
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        //variables
        Enumeration en = dds.getVariables();
        while (en.hasMoreElements()) {
            BaseType baseType = (BaseType)en.nextElement();
            String varName = baseType.getName();
            Attributes sourceAtts = new Attributes();
            OpendapHelper.getAttributes(das, varName, sourceAtts);
            PrimitiveVector pv = null; //for determining data type
            if (baseType instanceof DGrid) {   //for multidim vars
                DGrid dGrid = (DGrid)baseType;
                BaseType bt0 = dGrid.getVar(0); //holds the data
                pv = bt0 instanceof DArray? ((DArray)bt0).getPrimitiveVector() : bt0.newPrimitiveVector();
            } else if (baseType instanceof DArray) {  //for the dimension vars
                DArray dArray = (DArray)baseType;
                pv = dArray.getPrimitiveVector();
            } else {
                if (verbose) String2.log("  baseType=" + baseType.toString() + " isn't supported yet.\n");
            }
            if (pv != null) {
                dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, 
                    PrimitiveArray.factory(OpendapHelper.getElementClass(pv), 2, false),
                    sourceAtts);
                dataAddTable.addColumn(dataAddTable.nColumns(), varName, 
                    PrimitiveArray.factory(OpendapHelper.getElementClass(pv), 2, false),
                    makeReadyToUseAddVariableAttributesForDatasetsXml(
                        sourceAtts, varName, true)); //addColorBarMinMax

                //if a variable has timeUnits, files are likely sorted by time
                //and no harm if files aren't sorted that way
                if (tSortedColumnSourceName.length() == 0 && 
                    EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                    tSortedColumnSourceName = varName;
            }
        }

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //global attributes
        OpendapHelper.getAttributes(das, "GLOBAL", dataSourceTable.globalAttributes());
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", tPublicDirUrl);
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataAddTable)? "Point" : "Other",
                tLocalDirUrl, externalAddGlobalAttributes));

        //write the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName);
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromHyraxFiles\" datasetID=\"" + 
                suggestDatasetID(tPublicDirUrl + tFileNameRegex) + 
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <fileDir>" + tLocalDirUrl + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + tFileNameRegex + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + tPreExtractRegex + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + tPostExtractRegex + "</postExtractRegex>\n" +
            "    <extractRegex>" + tExtractRegex + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
            "    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToCatchLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, true, false));
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
"http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/", 
"pentad.*\\.nc\\.gz",
"http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1987/M09/pentad_19870908_v11l35flk.nc.gz", 
2880,
"", "", "", "", //extract
"time", "time", new Attributes());

String expected = 
directionsForGenerateDatasetsXml() +
"zz\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "zz???", "");
            Test.ensureEqual(edd.title(), "???", "");
            Test.ensureEqual(String2.toCSVString(edd.dataVariableDestinationNames()), 
                "???", "");


        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testNasa(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromHyraxFiles.testNasa() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;


        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);


        try {
        String id = "nmspWcosTemp";
        if (deleteCachedInfo) {
            File2.delete(datasetInfoDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + BADFILE_TABLE_FILENAME);
        }
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        try {
        String2.log("\n****************** EDDTableFromHyraxFiles testWcosTemp das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //*** test getting dds for entire dataset
        try{
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //*** test make data files
        String2.log("\n****************** EDDTableFromHyraxFiles.testWcosTemp make DATA FILES\n");       

        //.csv    for one lat,lon,time
        try {
        userDapQuery = "station&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_stationList", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //.csv    for one lat,lon,time, many depths (from different files)      via lon > <
        userDapQuery = "&station=\"ANO001\"&time=1122592440";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);   
//one depth, by hand via DAP form:   (note that depth is already negative!)
//???
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        /* */
    }


    public static void testGetSize() throws Throwable {
        try {
            DConnect dConnect = new DConnect(
"???http://edac-dap.northerngulfinstitute.org/dods-bin/nph-dods/WCOS/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc", 
                  acceptDeflate, 1, 1);
            DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
            BaseType bt = dds.getVariable("Time");
            DArray dArray = (DArray)bt;
            String2.log("nDim=" + dArray.numDimensions());
            DArrayDimension dim = dArray.getDimension(0);
            int n = dim.getSize();
            String2.log("n=" + n);
            Test.ensureTrue(n > 10, "n=" + n);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    public static void testGetAxisValues() throws Throwable {
        try {
            DConnect dConnect = new DConnect(
"???http://edac-dap.northerngulfinstitute.org/dods-bin/nph-dods/WCOS/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc",       
                acceptDeflate, 1, 1);
            PrimitiveArray pa[] = OpendapHelper.getAxisValues(dConnect, 
                new String[]{"Time","Latitude","Longitude","Depth"}, "[11:15]");
            for (int i = 0; i < pa.length; i++)
                String2.log("pa[" + i + "]=" + pa[i].toString());
            Test.ensureEqual(pa[0].toString(), "1.1189748E9, 1.11897504E9, 1.11897528E9, 1.11897552E9, 1.11897576E9", "");
            Test.ensureEqual(pa[1].toString(), "37.13015", "");      //lat
            Test.ensureEqual(pa[2].toString(), "-122.361253", "");   //lon
            Test.ensureEqual(pa[3].toString(), "-0.0", "");

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedInfo) throws Throwable {

        if (true)
            throw new SimpleException("THIS IS INACTIVE");

        //usually run
        testGenerateDatasetsXml();
        testGetSize();
        testGetAxisValues();
        testNasa(deleteCachedInfo);  

        //not usually run

    }
}

