/* 
 * EDDTableFromThreddsFiles Copyright 2009, NOAA.
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
import com.cohort.util.String2;
import com.cohort.util.Test;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;


/** 
 * This class represents a directory tree with a collection of n-dimensional (1,2,3,4,...) 
 * opendap files, each of which is flattened into a table.
 * For example, http://biloxi-bay.ssc.hpc.msstate.edu/dods-bin/nph-dods/WCOS/nmsp/wcos/ 
 * (the four dimensions there are e.g., time,depth,lat,lon).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-06-08
 */
public class EDDTableFromThreddsFiles extends EDDTableFromFiles { 

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;

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
    public EDDTableFromThreddsFiles(String tDatasetID, String tAccessibleTo,
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

        super("EDDTableFromThreddsFiles", false, tDatasetID, tAccessibleTo, tOnChange, 
            tAddGlobalAttributes, tAltMetersPerSourceUnit, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ);

    }

    /**
     * This gets the fileNames from a thredds catalog.
     * This overrides the superclass version of this method.
     *
     * @param fileDir for this version of this method, fileDir is the
     *  url of the base catalog.xml file, e.g.,
     *  http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml
     *
     * @returns an array with a list of full file names 
     * @throws Throwable if trouble
     */
    public String[] getFileNames(String fileDir, String fileNameRegex, boolean recursive) throws Throwable {
        if (reallyVerbose) String2.log("EDDTableFromThreddsFiles getFileNames");
        //clear the stored file info
        lastFileDirs     = new StringArray();
        lastFileDirIndex = new ShortArray(); 
        lastFileName     = new StringArray(); 
        lastFileLastMod  = new LongArray(); 

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
     * @param url the url of the current Thredds catalog xml, e.g.,
     *    http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/catalog.xml
     *    which leads to unaggregated datasets (each from a file) like
          http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080429.nc(.html)
     * @throws Throwable if trouble, e.g., if url doesn't respond
     */
    private void getFileNameInfo(String url, String fileNameRegex, boolean recursive) throws Throwable {
        if (reallyVerbose) String2.log("\n<<< getFileNameInfo url=" + url + " regex=" + fileNameRegex);

        int po = url.indexOf("/thredds/");
        Test.ensureTrue(po > 0, "'/thredds/' not found in url=" + url);
        String threddsBase = url.substring(0, po);  //no /thredds/..., no trailing /
        String serviceBase = "/thredds/dodsC/"; //default
        po = url.indexOf("/thredds/catalog/");
        String catalogBase = po > 0? File2.getDirectory(url) : threddsBase + "/thredds/catalog/";
        if (reallyVerbose) String2.log("threddsBase=" + threddsBase + "\ncatalogBase=" + catalogBase);
        long time = System.currentTimeMillis();

        String names[] = new String[5];
        String datasetTags[] = {
            "won't match",
            "<catalog><dataset>",
            "<catalog><dataset><dataset>",
            "<catalog><dataset><dataset><dataset>",
            "<catalog><dataset><dataset><dataset><dataset>"};
        String endDatasetTags[] = {
            "won't match",
            "<catalog></dataset>",
            "<catalog><dataset></dataset>",
            "<catalog><dataset><dataset></dataset>",
            "<catalog><dataset><dataset><dataset></dataset>"};
        String endDateTags[] = {
            "won't match",
            "<catalog><dataset></date>",
            "<catalog><dataset><dataset></date>",
            "<catalog><dataset><dataset><dataset></date>",
            "<catalog><dataset><dataset><dataset><dataset></date>"};

        try {
            //I could get inputStream from url, but then (via recursion) perhaps lots of streams open.
            //I think better to get the entire response (succeed or fail *now*).
            byte bytes[] = SSR.getUrlResponseBytes(url);
            SimpleXMLReader xmlReader = new SimpleXMLReader(new ByteArrayInputStream(bytes));
            while (true) {
                xmlReader.nextTag();
                String tags = xmlReader.allTags();
                int whichDatasetTag    = String2.indexOf(datasetTags,    tags);
                int whichEndDatasetTag = String2.indexOf(endDatasetTags, tags);
                int whichEndDateTag    = String2.indexOf(endDateTags,    tags);

                //<catalogRef xlink:href="2008/catalog.xml" xlink:title="2008" ID="nmsp/wcos/WES001/2008" name=""/>
                if (recursive && tags.endsWith("<catalogRef>")) {
                    String href = xmlReader.attributeValue("xlink:href");
                    if (href != null) { //look for /...
                        if (!href.startsWith("http")) {  //if not a complete url
                            if (href.startsWith("/thredds/catalog/"))
                                href = threddsBase + href;
                            else href = catalogBase + href; 
                        }
                        getFileNameInfo(href, fileNameRegex, recursive);
                    }

                //<dataset name="WES001_030MTBD029R00_20080613.nc" ID="nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc" urlPath="nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc">
                //  <date type="modified">2010-01-09 21:37:24Z</date>
                //</dataset> 
                } else if (whichDatasetTag > 0) {
                    String tName = xmlReader.attributeValue("name");
                    if (tName != null && tName.matches(fileNameRegex)) {
                        names[whichDatasetTag] = tName;
                    }

                } else if (whichEndDatasetTag > 0) {
                    names[whichEndDatasetTag] = null;
                    
                } else if (whichEndDateTag > 0 && names[whichEndDateTag] != null) {
                    //"<catalog><dataset><dataset></date>"
                    String isoTime = xmlReader.content();
                    double epochSeconds = Calendar2.safeIsoStringToEpochSeconds(isoTime);
                    if (!Double.isNaN(epochSeconds)) {

                        //add to file list
                        String dodsUrl = String2.replaceAll(File2.getDirectory(url), 
                            "/thredds/catalog/", "/thredds/dodsC/");
                        if (reallyVerbose) String2.log("    found " + 
                            dodsUrl + names[whichEndDateTag] + "   " + isoTime);
                        int tDirIndex = lastFileDirs.indexOf(dodsUrl);
                        if (tDirIndex == -1) {
                            tDirIndex = lastFileDirs.size();
                            lastFileDirs.add(dodsUrl);
                        }
                        lastFileDirIndex.add((short)tDirIndex);
                        lastFileName.add(names[whichEndDateTag]); 
                        lastFileLastMod.add(Math2.roundToLong(epochSeconds * 1000));
                    }

                } else if (tags.equals("</catalog>")) {
                    xmlReader.close();
                    break;
                }
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
        if (reallyVerbose) String2.log("\n>>> leaving getFileNameInfo time=" +
            (System.currentTimeMillis() - time) + "\n" +
            "  url=" + url);
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
            if (verbose) String2.log("EDDTableFromThreddsFiles.getFileLastModified can't find dir=" + tDir);
            return 0;
        }

        //linear search through fileNames
        int n = lastFileDirIndex.size();
        for (int i = 0; i < n; i++) {
            if (lastFileDirIndex.array[i] == dirIndex &&
                lastFileName.array[i].equals(tName)) 
                return lastFileLastMod.array[i];
        }
        if (verbose) String2.log("EDDTableFromThreddsFiles.getFileLastModified can't find file=" + tName );
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
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromThreddsFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalDirUrl  the base/starting URL with a Thredds (sub-)catalog, 
     *    usually ending in catalog.xml (but sometimes other file names).
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (".*" if no extension or e.g., ".*\\.nc").
     * @param oneFileDapUrl  url for one file, without ending .das or .html
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

        String2.log("EDDTableFromThreddsFiles.generateDatasetsXml" +
            "\n  dirUrl=" + tLocalDirUrl + 
            "\n  oneFileDapUrl=" + oneFileDapUrl);
        String tPublicDirUrl = convertToPublicSourceUrl(tLocalDirUrl);

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        DConnect dConnect = new DConnect(oneFileDapUrl, acceptDeflate, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);;
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        //global metadata
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

        //variables
//!!!Isn't this simplistic??? Doesn't it assume all Grid/Arrays have same dimensions? How does this class work?
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

        //global metadata
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

        //suggest tFileNameRegex
        if (tFileNameRegex == null || tFileNameRegex.length() == 0) {
            String tExt = File2.getExtension(oneFileDapUrl);
            if (tExt == null || tExt.length() == 0)
                tFileNameRegex = ".*";
            else tFileNameRegex = ".*\\" + tExt;
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

        //write the information
        StringBuilder sb = new StringBuilder();
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName);
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\"" + 
                suggestDatasetID(tPublicDirUrl + tFileNameRegex) + "\" active=\"true\">\n" +
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
                //I could do wcos/catalog.xml but very slow because lots of files
                "http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml",
                ".*MTBD.*\\.nc",   // ADCP files have different vars and diff metadata, e.g., _FillValue
                "http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
                1440, 
                "", "_.*$", ".*", "stationID",
                "Time", "stationID Time",
                null); //externalAddGlobalAttributes

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDTableFromThreddsFiles",
                "http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml",
                ".*MTBD.*\\.nc",  
                "http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
                "1440", 
                "", "_.*$", ".*", "stationID",
                "Time", "stationID Time"},
                false); //doIt loop?
            String gdxResults = String2.getClipboardString();
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\"noaa_nodc_8fcf_be37_cbe4\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <fileDir>http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*MTBD.*\\.nc</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex>_.*$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>stationID</columnNameForExtract>\n" +
"    <sortedColumnSourceName>Time</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>stationID Time</sortFilesBySourceNames>\n" +
"    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">CF-1.4</att>\n" +
"        <att name=\"History\">created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2010/00/10 03:00 CST. Original dataset URL: </att>\n" +
"        <att name=\"Mooring_ID\">WES001</att>\n" +
"        <att name=\"Version\">2</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">CF-1.4, COARDS, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"History\">null</att>\n" +
"        <att name=\"history\">created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2010/00/10 03:00 CST. Original dataset URL: </att>\n" +
"        <att name=\"infoUrl\">http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n" +
"        <att name=\"institution\">NOAA NODC</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">CF-1.4, COARDS, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"sourceUrl\">http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">NOAA NODC data from http://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n" +
"        <att name=\"title\">WES001 2008</att>\n" +
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
"        <sourceName>yearday</sourceName>\n" +
"        <destinationName>yearday</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Day of the year</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degree_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">-9999</att>\n" +
"            <att name=\"description\">Greenwich Mean Time of each temperature measurement record,in seconds since 1970-01-01 00:00:00.000 0:00</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01 00:00:00.000 0:00</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Depth</sourceName>\n" +
"        <destinationName>Depth</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">-9999</att>\n" +
"            <att name=\"description\">Data logger measurement depth (expressed as negative altitudes), referenced to Mean Sea Level (MSL)</att>\n" +
"            <att name=\"long_name\">depth expressed as negative altitudes</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">meter</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degree_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature_flag</sourceName>\n" +
"        <destinationName>Temperature_flag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"description\">flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data</att>\n" +
"            <att name=\"long_name\">Temperature flag</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature status_flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature</sourceName>\n" +
"        <destinationName>Temperature</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"int\">9999</att>\n" +
"            <att name=\"description\">Seawater temperature</att>\n" +
"            <att name=\"long_name\">Sea Water Temperature</att>\n" +
"            <att name=\"quantity\">Temperature</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature</att>\n" +
"            <att name=\"units\">Celsius</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>yearday_flag</sourceName>\n" +
"        <destinationName>yearday_flag</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"description\">flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data</att>\n" +
"            <att name=\"long_name\">Yearday flag</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "noaa_nodc_8fcf_be37_cbe4", "");
            Test.ensureEqual(edd.title(), "WES001 2008", "");
            Test.ensureEqual(String2.toCSVString(edd.dataVariableDestinationNames()), 
                "stationID, yearday, latitude, time, Depth, longitude, Temperature_flag, " +
                "Temperature, yearday_flag", "");

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
    public static void testWcosTemp(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromThreddsFiles.testWcosTemp() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        try {

        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);


        String id = "nmspWcosTemp";
        if (deleteCachedInfo) {
            File2.delete(datasetInfoDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + BADFILE_TABLE_FILENAME);
        }
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        try {
        String2.log("\n****************** EDDTableFromThreddsFiles testWcosTemp das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  station {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -124.932, -119.66934;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 33.89511, 48.325001;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue -9999.0;\n" +
"    Float64 actual_range 1.0971834e+9, 1.27905816e+9;\n" +  //changes sometimes   see time_coverage_end below
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +  
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range -99.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    String description \"Relative to Mean Sea Level (MSL)\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Temperature {\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String quantity \"Temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  Temperature_flag {\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Temperature Flag\";\n" +
"    String standard_name \"sea_water_temperature status_flag\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeriesProfile\";\n" +
"    String cdm_profile_variables \"time\";\n" +
"    String cdm_timeseries_variables \"station, longitude, latitude\";\n" +
"    String Conventions \"COARDS, CF-1.4, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Easternmost_Easting -119.66934;\n" +
"    Float64 geospatial_lat_max 48.325001;\n" +
"    Float64 geospatial_lat_min 33.89511;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -119.66934;\n" +
"    Float64 geospatial_lon_min -124.932;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min -99.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2009/00/04 10:00 CST.\n" +
today + " http://data.nodc.noaa.gov/opendap/nmsp/wcos/\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/nmspWcosTemp.das\";\n" +
"    String infoUrl \"http://www.ncddc.noaa.gov/activities/wcos\";\n" +
"    String institution \"NOAA NMSP\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.4, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 48.325001;\n" +
"    String sourceUrl \"http://data.nodc.noaa.gov/opendap/nmsp/wcos/\";\n" +
"    Float64 Southernmost_Northing 33.89511;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"station, longitude, latitude\";\n" +
"    String summary \"The West Coast Observing System (WCOS) project provides access to temperature and currents data collected at four of the five National Marine Sanctuary sites, including Olympic Coast, Gulf of the Farallones, Monterey Bay, and Channel Islands. A semi-automated end-to-end data management system transports and transforms the data from source to archive, making the data acessible for discovery, access and analysis from multiple Internet points of entry.\n" +
"\n" +
"The stations (and their code names) are Ano Nuevo (ANO001), San Miguel North (BAY), Santa Rosa North (BEA), Big Creek (BIG001), Bodega Head (BOD001), Cape Alava 15M (CA015), Cape Alava 42M (CA042), Cape Alava 65M (CA065), Cape Alava 100M (CA100), Cannery Row (CAN001), Cape Elizabeth 15M (CE015), Cape Elizabeth 42M (CE042), Cape Elizabeth 65M (CE065), Cape Elizabeth 100M (CE100), Cuyler Harbor (CUY), Esalen (ESA001), Point Joe (JOE001), Kalaloch 15M (KL015), Kalaloch 27M (KL027), La Cruz Rock (LAC001), Lopez Rock (LOP001), Makah Bay 15M (MB015), Makah Bay 42M (MB042), Pelican/Prisoners Area (PEL), Pigeon Point (PIG001), Plaskett Rock (PLA001), Southeast Farallon Island (SEF001), San Miguel South (SMS), Santa Rosa South (SRS), Sunset Point (SUN001), Teawhit Head 15M (TH015), Teawhit Head 31M (TH031), Teawhit Head 42M (TH042), Terrace Point 7 (TPT007), Terrace Point 8 (TPT008), Valley Anch (VAL), Weston Beach (WES001).\";\n" +
"    String time_coverage_end \"2010-07-13T21:56:00Z\";\n" + //changes
"    String time_coverage_start \"2004-10-07T21:10:00Z\";\n" +
"    String title \"West Coast Observing System (WCOS) Temperature Data\";\n" +
"    String Version \"2\";\n" +
"    Float64 Westernmost_Easting -124.932;\n" +
"  }\n" +
"}\n";
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
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    Float64 altitude;\n" +
"    Float64 Temperature;\n" +
"    Byte Temperature_flag;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //*** test make data files
        String2.log("\n****************** EDDTableFromThreddsFiles.testWcosTemp make DATA FILES\n");       

        //.csv    for one lat,lon,time
        try {
        userDapQuery = "station&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_stationList", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"station\n" +
"\n" +
"ANO001\n" +
"BAYXXX\n" +
"BEAXXX\n" +
"BIG001\n" +
"BOD001\n" +
"CA015X\n" +
"CA042X\n" +
"CA065X\n" +
"CA100X\n" +
"CE015X\n" +
"CE042X\n" +
"CE065X\n" +
"CE100X\n" +
"ESA001\n" +
"JOE001\n" +
"KL015X\n" +
"KL027X\n" +
"LAC001\n" +
"LOP001\n" +
"MB015X\n" +
"MB042X\n" +
"PELXXX\n" +
"PIG001\n" +
"SEF001\n" +
"SMSXXX\n" +
"SRSXXX\n" +
"SUN001\n" +
"TH015X\n" +
"TH031X\n" +
"TH042X\n" +
"TPT007\n" +
"TPT008\n" +
"VALXXX\n" +
"WES001\n";
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
//Lat, 37.13015 Time=1122592440 depth=-12 Lon=-122.361253    
// yearday 208.968,  temp_flag 0, temp 10.66, yeardayflag  0
        expected = 
"station, longitude, latitude, time, altitude, Temperature, Temperature_flag\n" +
", degrees_east, degrees_north, UTC, m, degree_C, \n" +
"ANO001, -122.361253, 37.13015, 2005-07-28T23:14:00Z, -4.0, 12.04, 0\n" +
"ANO001, -122.361253, 37.13015, 2005-07-28T23:14:00Z, -12.0, 10.66, 0\n" +
"ANO001, -122.361253, 37.13015, 2005-07-28T23:14:00Z, -20.0, 10.51, 0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        /* */
    }

/*
Time              DArray[Time]
Depth             DArray[Depth]
Latitude          DArray[Latitude]
Longitude         DArray[Longitude]
Height            DGrid [Depth,Latitude,Longitude]
Height_flag       DGrid [Depth,Latitude,Longitude]
Pressure          DGrid [Time,Latitude,Longitude]
Pressure_flag     DGrid [Time,Latitude,Longitude]
Temperature       DGrid [Time,Latitude,Longitude]
Temperature_flag  DGrid [Time,Latitude,Longitude]
WaterDepth        DGrid [Time,Latitude,Longitude]
WaterDepth_flag   DGrid [Time,Latitude,Longitude]
YearDay           DGrid [Time,Latitude,Longitude]
YearDay_flag      DGrid [Time,Latitude,Longitude]
DataQuality       DGrid [Time,Depth,Latitude,Longitude]
DataQuality_flag  DGrid [Time,Depth,Latitude,Longitude]
Eastward          DGrid [Time,Depth,Latitude,Longitude]
Eastward_flag     DGrid [Time,Depth,Latitude,Longitude]
ErrorVelocity     DGrid [Time,Depth,Latitude,Longitude]
ErrorVelocity_flag DGrid [Time,Depth,Latitude,Longitude]
Intensity         DGrid [Time,Depth,Latitude,Longitude]
Intensity_flag    DGrid [Time,Depth,Latitude,Longitude]
Northward         DGrid [Time,Depth,Latitude,Longitude]
Northward_flag    DGrid [Time,Depth,Latitude,Longitude]
Upwards_flag      DGrid [Time,Depth,Latitude,Longitude]
Upwards           DGrid [Time,Depth,Latitude,Longitude]
*/


    public static void testGetSize() throws Throwable {
        try {
            DConnect dConnect = new DConnect(
//"http://data.nodc.noaa.gov/opendap/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc", //lon lat reversed!
//"http://edac-dap.northerngulfinstitute.org/dods-bin/nph-dods/WCOS/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc", 
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
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
//"http://data.nodc.noaa.gov/opendap/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc", //lon lat reversed!
//"http://edac-dap.northerngulfinstitute.org/dods-bin/nph-dods/WCOS/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc",       
"http://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/ANO001/2005/ANO001_021MTBD000R00_20050617.nc",
                acceptDeflate, 1, 1);
            PrimitiveArray pa[] = OpendapHelper.getAxisValues(dConnect, 
                new String[]{"Time","Latitude","Longitude","Depth"}, "[11:15]");
            for (int i = 0; i < pa.length; i++)
                String2.log("pa[" + i + "]=" + pa[i].toString());
            Test.ensureEqual(pa[0].toString(), "1.1189748E9, 1.11897504E9, 1.11897528E9, 1.11897552E9, 1.11897576E9", "");
            Test.ensureEqual(pa[1].toString(), "37.13015", "");      //lat
            Test.ensureEqual(pa[2].toString(), "-122.361253", "");   //lon
            Test.ensureEqual(pa[3].toString(), "0.0", "");

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

        //usually run
        testGetSize();
        testGetAxisValues();
        testWcosTemp(deleteCachedInfo);  
        testGenerateDatasetsXml();

        //not usually run

    }
}

