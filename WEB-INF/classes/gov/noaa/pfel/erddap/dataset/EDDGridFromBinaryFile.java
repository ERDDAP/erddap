/* 
 * NOT FINISHED AND NOT ACTIVE.  EDDGridFromBinaryFile Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.ArrayList;

/** 
 * THIS IS NOT FINISHED AND NOT ACTIVE.
 * IF THIS IS REVIVED, MAKE IT A SUBCLASS OF EDDGridFromFiles.
 * This class represents a grid dataset from a binary file (e.g., the Etopo1g bathymetry data).
 * Ideally, this would be quite general. Currently, it is pretty tied to the Etopo1g
 * data.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2008-02-20
 */
public class EDDGridFromBinaryFile extends EDDGrid { 


    protected String fileName;

    /**
     * NOT FINISHED AND NOT ACTIVE. This constructs an EDDGridFromBinaryFile based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromBinaryFile"&gt; 
     *    having just been read.  
     * @return an EDDGridFromBinaryFile.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridFromBinaryFile fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {
        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDGridFromBinaryFile(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        boolean tAccessibleViaWMS = true;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        ArrayList tAxisVariables = new ArrayList();
        ArrayList tDataVariables = new ArrayList();
        IntArray tAxisLengths;
        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        String tFileName = null;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
/*
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

//fullRefDirectory + etopo2FileName,
//            -180, 180, //these settings are specific for the ETOPO1 file
//            -90, 90,  
//            21601, 10801,
//and read data as int.class

            //try to make the tag names as consistent, descriptive and readable as possible
            if (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<fileName>")) {}
            else if (localTags.equals("</fileName>")) tFileName = content; 
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<axesLengths>"))  {}
            else if (localTags.equals("</axesLengths>"))  tAxesLengths = new IntArray(content); 
            else if (localTags.equals( "<axisVariable>")) tAxisVariables.add(getSDAVVariableFromXml(xmlReader));           
            else if (localTags.equals( "<dataVariable>")) tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<accessibleViaWMS>")) {}
            else if (localTags.equals("</accessibleViaWMS>")) tAccessibleViaWMS = String2.parseBoolean(content);
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
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

        int nav = tAxisVariables.size();
        Object ttAxisVariables[][] = nav == 0? null : new Object[nav][];
        for (int i = 0; i < tAxisVariables.size(); i++)
            ttAxisVariables[i] = (Object[])tAxisVariables.get(i);

        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);
*/
        return new EDDGridFromBinaryFile(tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS, 
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
            new Object[1][3], new Object[1][3], //ttAxisVariables, ttDataVariables,
            tReloadEveryNMinutes,
            tFileName);
    }

    /**
     * THIS IS NOT FINISHED AND NOT ACTIVE. The constructor.
     * The axisVariables must be the same and in the same
     * order for each dataVariable.
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
     *
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
     * @throws Throwable if trouble
     */
    public EDDGridFromBinaryFile(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery,
        Attributes tAddGlobalAttributes,
        Object tAxisVariables[][],
        Object tDataVariables[][],
        int tReloadEveryNMinutes,
        String tFileName) throws Throwable {

  /*      if (verbose) String2.log(
            "\n*** constructing EDDGridFromBinaryFile " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridFromBinaryFile(" + 
            tDatasetID + ") constructor:\n";

        //save some of the parameters
        className = "EDDGridFromBinaryFile"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        setGraphsAccessibleTo(tGraphsAccessibleTo);
        if (!tAccessibleViaWMS) 
            accessibleViaWMS = String2.canonical(
                MessageFormat.format(EDStatic.noXxx, "WMS"));
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        sourceGlobalAttributes = new Attributes();
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");
        if (combinedGlobalAttributes.getString("cdm_data_type") == null)
            combinedGlobalAttributes.add("cdm_data_type", "Grid");
        reloadEveryNMinutes = tReloadEveryNMinutes;
        fileName = tFileName;

        //create axisVariables
        axisVariables = new EDVGridAxis[tAxisVariables.length];
        for (int av = 0; av < tAxisVariables.length; av++) {
            String tSourceName = (String)tAxisVariables[av][0];
            String tDestName = (String)tAxisVariables[av][1];
            if (tDestName != null)
                tDestName = tDestName.trim();
            if (!String2.isSomething(tDestName)) 
                tDestName = tSourceName;
            Attributes tSourceAtts = new Attributes();
            Attributes tAddAtts = (Attributes)tAxisVariables[av][2];
...            PrimitiveArray tSourceValues = new DoubleArray(DataHelper.getRegularArray(5401, -90, 1/30.0))

            axisVariables[av] = makeAxisVariable(av, tSourceName, tDestName,
                tSourceAtts, tAddAtts, tSourceValues);
        }

        //create dataVariable
        Test.ensureEqual(tDataVariables.length, 1, 
            errorInMethod + "dataVariables.length must be 1."); 
        dataVariables = new EDV[1];
        String tSourceName = (String)tDataVariables[0][0];
        String tDestinationName = (String)tDataVariables[0][1];
        if (tDestinationName == null || tDestinationName.length() == 0)
            tDestinationName = tSourceName;
        Attributes tSourceAttributes = new Attributes();
        Attributes tAddAttributes = (Attributes)tDataVariables[0][2];
        if (tDestinationName.equals(EDV.TIME_NAME))
            throw new RuntimeException(errorInMethod +
                "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
        else if (EDVTime.hasTimeUnits(tSourceAttributes, tAddAttributes)) 
            dataVariables[0] = new EDVTimeStamp(tSourceName, tDestinationName,
                tSourceAttributes, tAddAttributes,
                ???int.class);  
        else dataVariables[0] = new EDV(tSourceName, tDestinationName,
            tSourceAttributes, tAddAttributes,
            ???int.class);  
        dataVariables[0].setActualRangeFromDestinationMinMax();

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridFromBinaryFile " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
*/
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException( 
            "Error: EDDGridBinaryFile doesn't support method=\"sibling\".");

    }

    /** 
     * This gets data (not yet standardized) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDataVariables EDV[] with just the requested data variables
     * @param tConstraints  int[nAxisVariables*3] 
     *   where av*3+0=startIndex, av*3+1=stride, av*3+2=stopIndex.
     *   AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
     * @return a PrimitiveArray[] where the first axisVariables.length elements
     *   are the axisValues and the next tDataVariables.length elements
     *   are the dataValues.
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {
/*
        //create the grid;  readBinary calculates stats
        //On 2011-03-14 I switched to etopo1_ice_g_i2.bin
        //  grid referenced, 16bit ints, 10801 rows by 21601 columns
        //On 2007-03-29 I switched from ETOPO2 (version 1) to ETOPO2v2g_MSB.raw (version 2).
        //  ETOPO2v2g_MSB.raw (grid centered, MSB 16 bit signed integers)
        //  5401 rows by 10801 columns.
        //Data is stored row by row, starting at 90, going down to -90,
        //with lon -180 to 180 on each row (the first and last points on each row are duplicates).
        //The data is grid centered, so the data associated with a given lon,lat represents
        //a cell which extends 1 minute N, S, E, and W of the lon, lat.
        //I verified this interpretation with Lynn.

        DoubleArray lats = new DoubleArray(axisVariables[0].sourceValues().subset(
            tConstraints[0], tConstraints[1], tConstraints[2]);
        DoubleArray lons = new DoubleArray(axisVariables[1].sourceValues().subset(
            tConstraints[3], tConstraints[4], tConstraints[5]);

        Grid grid = new Grid();
        grid.readBinary(fullRefDirectory + etopo2FileName,
            -180, 180, //these settings are specific for the ETOPO2v2g file
            -90, 90,  
            10801, 5401,
            lons.get(0), lons.get(lons.size() - 1), 
            lats.get(0), lats.get(lats.size() - 1), 
            lons.size(), lats.size());
*/
        PrimitiveArray results[] = new PrimitiveArray[3];
/*        results[0] = lats;
        results[1] = lons;
        results[2] = new grid.data;

    public void readBinary(String fullFileName, 
            double fileMinLon, double fileMaxLon, 
            double fileMinLat, double fileMaxLat,
            int fileNLonPoints, int fileNLatPoints,
            double desiredMinLon, double desiredMaxLon, 
            double desiredMinLat, double desiredMaxLat,
            int desiredNLonPoints, int desiredNLatPoints)
            throws Throwable {
        //future: this cound accept data from files with different data types, e.g., float
        //future: this cound accept data in different order (stored col-by-col, from lower left

        //ensure desired range is acceptable
        if (verbose) String2.log("Grid.readBinary");
        clear();
        String errorInMethod = "Error in Grid.readBinary(" + fullFileName + "): "; 
        if (desiredMinLon > desiredMaxLon) 
            throw new IllegalArgumentException(errorInMethod +
                "desiredMinLon (" + desiredMinLon + ") must be <= desiredMaxLon (" + desiredMaxLon + ").");
        if (desiredMinLat > desiredMaxLat) 
            throw new IllegalArgumentException(errorInMethod +
                "desiredMinLat (" + desiredMinLat + ") must be <= desiredMaxLat (" + desiredMaxLat + ").");
        long time = System.currentTimeMillis();

        //calculate file lon and values
        //file stores top row of data at start of file, but binaryFindClosest requires ascending array
        //so make array ascending, then adjust later
        double fileLonSpacing = (fileMaxLon - fileMinLon) / (fileNLonPoints - 1);
        double fileLatSpacing = (fileMaxLat - fileMinLat) / (fileNLatPoints - 1);
        double fileLon[] = DataHelper.getRegularArray(fileNLonPoints, fileMinLon, fileLonSpacing);
        double fileLat[] = DataHelper.getRegularArray(fileNLatPoints, fileMinLat, fileLatSpacing); 
        //String2.log("fileLon=" + String2.toCSSVString(fileLon).substring(0, 70));
        //String2.log("fileLat=" + String2.toCSSVString(fileLat).substring(0, 70));

        //make the desired lon and lat arrays   (n, min, spacing)
        //String2.log("  desiredNLonPoints=" + desiredNLonPoints + " desiredNLatPoints=" + desiredNLatPoints);
        double desiredLonRange = desiredMaxLon - desiredMinLon;
        double desiredLatRange = desiredMaxLat - desiredMinLat;
        lonSpacing = desiredLonRange / Math.max(1, desiredNLonPoints - 1);
        latSpacing = desiredLatRange / Math.max(1, desiredNLatPoints - 1);
        //spacing  (essentially stride)     (floor (not ceil) because it is inverse of stride)
        //String2.log("tlatSpacing=" + latSpacing + " fileLatSpacing=" + fileLatSpacing);
        lonSpacing = Math.max(1, Math.floor(lonSpacing / fileLonSpacing)) * fileLonSpacing;
        latSpacing = Math.max(1, Math.floor(latSpacing / fileLatSpacing)) * fileLatSpacing;
        if (verbose) String2.log("  will get lonSpacing=" + (float)lonSpacing + " latSpacing=" + (float)latSpacing);

        //change desired to closest file points (if lat,lon arrays extended to appropriate range)
        //THIS ASSUMES FILE LONS AND LATS ARE AT MULTIPLES OF FileLon/LatSpacing.  (no offset)
        desiredMinLon = Math2.roundToInt(desiredMinLon / lonSpacing) * lonSpacing;
        desiredMaxLon = Math2.roundToInt(desiredMaxLon / lonSpacing) * lonSpacing;
        desiredMinLat = Math2.roundToInt(desiredMinLat / latSpacing) * latSpacing;
        desiredMaxLat = Math2.roundToInt(desiredMaxLat / latSpacing) * latSpacing;
        if (verbose) String2.log("  will get" +
            " minLon=" + (float)desiredMinLon + " maxLon=" + (float)desiredMaxLon +
            " minLat=" + (float)desiredMinLat + " maxLat=" + (float)desiredMaxLat);
        
        //nLon nLat calculated as  lastLonIndex - firstLonIndex + 1
        //if (verbose) String2.log("  grid.readBinary lonSpacing=" + (float)lonSpacing + " latSpacing=" + (float)latSpacing);
        int nLon = Math2.roundToInt(desiredMaxLon / lonSpacing) - Math2.roundToInt(desiredMinLon / lonSpacing) + 1;
        int nLat = Math2.roundToInt(desiredMaxLat / latSpacing) - Math2.roundToInt(desiredMinLat / latSpacing) + 1;
        lon = DataHelper.getRegularArray(nLon, desiredMinLon, lonSpacing);
        lat = DataHelper.getRegularArray(nLat, desiredMinLat, latSpacing);
        if (verbose) String2.log("  will get nLon=" + nLon + " nLat=" + nLat);

        //find the offsets for the start of the rows closest to the desiredLon values
        int bytesPerValue = 2;
        int offsetLon[] = new int[nLon];
        for (int i = 0; i < nLon; i++) {
            double tLon = lon[i];
            while (tLon < fileMinLon) tLon += 360;
            while (tLon > fileMaxLon) tLon -= 360;
            int closestLon = Math2.binaryFindClosest(fileLon, tLon);
            offsetLon[i] = bytesPerValue * closestLon;  
            //String2.log("tLon=" + tLon + " closestLon=" + closestLon + " offset=" + offsetLon[i]);
        }

        //find the offsets for the start of the columns closest to the desiredLon values
        int offsetLat[] = new int[nLat];
        for (int i = 0; i < nLat; i++) {
            double tLat = lat[i];
            while (tLat < fileMinLat) tLat += 90;
            while (tLat > fileMaxLat) tLat -= 90;
            int closestLat = Math2.binaryFindClosest(fileLat, tLat);
            //adjust lat, since fileLat is ascending, but file stores data top row at start of file
            closestLat = fileNLatPoints - 1 - closestLat;
            offsetLat[i] = bytesPerValue * closestLat * fileNLonPoints; 
            //String2.log("tLat=" + tLat + " closestLat=" + closestLat + " offset=" + offsetLat[i]);
        }

        //open the file  (reading should be thread safe)
        RandomAccessFile raf = new RandomAccessFile(fullFileName, "r");

        //fill data array
        //lat is outer loop because file is lat major
        //and loop is backwards since stored top to bottom
        //(goal is to read basically from start to end of file)
        nValidPoints = nLon * nLat; //all points are valid
        data = new double[nValidPoints];
        minData = Double.MAX_VALUE;
        maxData = -Double.MIN_VALUE;
        for (int tLat = nLat - 1; tLat >= 0; tLat--) { 
            for (int tLon = 0; tLon < nLon; tLon++) { 
               raf.seek(offsetLat[tLat] + offsetLon[tLon]);
               double td = raf.readShort();
               setData(tLon, tLat, td);
               minData = Math.min(minData, td);
               maxData = Math.max(maxData, td);
            }
        }

        //close the file 
        raf.close();
        if (verbose) 
            String2.log("Grid.readBinary TIME=" + 
                (System.currentTimeMillis() - time) + "\n"); 
*/
        return results;
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        String2.log("\n****************** EDDGridFromBinaryFile.test() *****************\n");
        verbose = true;
        reallyVerbose = true;
        GridDataAccessor.verbose = true;
        GridDataAccessor.reallyVerbose = true;
        SgtGraph.verbose = true;
        SgtGraph.reallyVerbose = true;
        SgtMap.verbose = true;
        SgtMap.reallyVerbose = true;
        EDDGridFromDap gridDataset;
        String name, tName, axisDapQuery, userDapQuery, results, expected, error;

/*    
        gridDataset = (EDDGridFromDap)oneFromDatasetsXml(null, "erdMHchla8day"); 

        //test that bad metadata was removed
        //String2.log(
        //    "\n\naddAtt=" + gridDataset.addGlobalAttributes() +
        //    "\n\ncombinedAtt=" + gridDataset.combinedGlobalAttributes());
        Test.ensureEqual(gridDataset.combinedGlobalAttributes().getString("et_affine"), null, "");
        Test.ensureEqual(gridDataset.dataVariables()[0].combinedAttributes().getString("percentCoverage"), null, "");

        StringArray destinationNames = new StringArray();
        IntArray constraints = new IntArray();

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDGridFromBinaryFile test entire dataset\n");
        tName = gridDataset.makeNewFileForDapQuery(null, "", dir, baseName + "_Entire", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//"Attributes {\n" +
//"  time {\n" +
//"    Float64 actual_range 1.1886912e+9, 1.1886912e+9;\n"; //this will change sometimes
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);


        expected =   //test that _FillValue and missing_value are as in sourceAtt
        //but complicated, because that's the value my Grid class uses.
"  chlorophyll {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Concentration Of Chlorophyll In Sea Water\";\n" +
"    Float32 missing_value -9999999.0;\n" +    
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m-3\";\n" +
"  }\n" +
"  GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);


        //.csv
        String2.log("\n*** EDDGridFromDap test get .CSV axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, "time[0:100:1000]", dir, baseName + "_Axis", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + dir + tName);
        expected = 
"time\n" +
"UTC\n" +
"2002-07-08T00:00:00Z\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "");
        expected = 
"2005-04-24T00:00:00Z\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

*/
        String2.log("\n*** EDDGridFromBinaryFile.test finished.");

    }


}
