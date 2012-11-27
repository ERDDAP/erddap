/* 
 * EDDGridFromDap Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
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
import com.cohort.util.ResourceBundle2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
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
import ucar.ma2.*;  //only Array is needed; all other ucar is for testing netcdf-java

/** 
 * This class represents a grid dataset from an opendap DAP source.
 *
 * <p>Note that THREDDS has a default limit of 500MB for opendap responses.
 * See http://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.0/UpgradingTo4.0.html
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDDGridFromDap extends EDDGrid { 

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;


    /**
     * This constructs an EDDGridFromDap based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromDap"&gt; 
     *    having just been read.  
     * @return an EDDGridFromDap.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridFromDap fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDGridFromDap(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        double tAltitudeMetersPerSourceUnit = 1; 
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        ArrayList tAxisVariables = new ArrayList();
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        String tLocalSourceUrl = null;

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
            if (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) {}
            else if (localTags.equals("</altitudeMetersPerSourceUnit>")) 
                tAltitudeMetersPerSourceUnit = String2.parseDouble(content); 
            else if (localTags.equals( "<axisVariable>")) tAxisVariables.add(getSDAVVariableFromXml(xmlReader));           
            else if (localTags.equals( "<dataVariable>")) tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 

            //onChange
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) 
                tOnChange.add(content); 

            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 

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

        return new EDDGridFromDap(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tGlobalAttributes,
            tAltitudeMetersPerSourceUnit,
            ttAxisVariables,
            ttDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl);
    }

    /**
     * The constructor.
     * The axisVariables must be the same and in the same
     * order for each dataVariable.
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
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
     * @param tAddGlobalAttributes are global attributes which will
     *   be added to (and take precedence over) the data source's global attributes.
     *   This may be null if you have nothing to add.
     *   The combined global attributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line).
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of "[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tAltMetersPerSourceUnit the factor needed to convert the source
     *    alt values to/from meters above sea level.
     *    If there is no altitude variable, this is irrelevant.
     * @param tAxisVariables is an Object[nAxisVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" -
     *        a category from EDV.ioosCategories, 
     *        although they are added automatically for lon, lat, alt, and time). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>If there are longitude, latitude, altitude, or time variables,
     *        they must have that name as the destinationName (or sourceName) 
     *        to be identified as such.
     *    <br>Or, use tAxisVariables=null if the axis variables need no addAttributes
     *        and the longitude,latitude,altitude,time variables (if present) 
     *        all have their correct names in the source.
     *    <br>The order of variables you define must match the
     *       order in the source.
     *    <br>A time variable must have "units" specified in addAttributes (read first)
     *       or sourceAttributes.  "units" must be 
     *       a udunits string (containing " since ")
     *        describing how to interpret numbers 
     *        (e.g., "seconds since 1970-01-01T00:00:00Z").
     * @param tDataVariables is an Object[nDataVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" 
     *        - a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>All dataVariables must share the same axis variables.
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data (use Integer.MAX_VALUE for never).
     * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
     * @throws Throwable if trouble
     */
    public EDDGridFromDap(
        String tDatasetID, String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File,
        Attributes tAddGlobalAttributes,
        double tAltMetersPerSourceUnit, 
        Object tAxisVariables[][],
        Object tDataVariables[][],
        int tReloadEveryNMinutes,
        String tLocalSourceUrl) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridFromDap " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridFromDap(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridFromDap"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));
        localSourceUrl = tLocalSourceUrl;
        setReloadEveryNMinutes(tReloadEveryNMinutes);

        //quickRestart
        Attributes quickRestartAttributes = null;       
        if (EDStatic.quickRestart && 
            EDStatic.initialLoadDatasets() && 
            File2.isFile(quickRestartFullFileName())) {
            //try to do quick initialLoadDatasets()
            //If this fails anytime during construction, the dataset will be loaded 
            //  during the next major loadDatasets,
            //  which is good because it allows quick loading of other datasets to continue.
            //This will fail (good) if dataset has changed significantly and
            //  quickRestart file has outdated information.
            quickRestartAttributes = NcHelper.readAttributesFromNc(quickRestartFullFileName());

            if (verbose)
                String2.log("  using info from quickRestartFile");

            //set creationTimeMillis to time of previous creation, so next time
            //to be reloaded will be same as if ERDDAP hadn't been restarted.
            creationTimeMillis = quickRestartAttributes.getLong("creationTimeMillis");
        }

        //open the connection to the opendap source
        //Design decision: this doesn't use ucar.nc2.dt.GridDataSet 
        //  because GridDataSet determines axes via _CoordinateAxisType (or similar) metadata
        //  which most datasets we use don't have yet.
        //  One could certainly write another class that did use ucar.nc2.dt.GridDataSet.
        DConnect dConnect = null;
        if (quickRestartAttributes == null)
            dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);

        //DAS
        byte dasBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(localSourceUrl + ".das") : //has timeout and descriptive error 
            ((ByteArray)quickRestartAttributes.get("dasBytes")).toArray();
        DAS das = new DAS();
        das.parse(new ByteArrayInputStream(dasBytes));

        //DDS
        byte ddsBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(localSourceUrl + ".dds") : //has timeout and descriptive error 
            ((ByteArray)quickRestartAttributes.get("ddsBytes")).toArray();
        DDS dds = new DDS();
        dds.parse(new ByteArrayInputStream(ddsBytes));

        //get global attributes
        sourceGlobalAttributes = new Attributes();
        OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");
        if (combinedGlobalAttributes.getString("cdm_data_type") == null)
            combinedGlobalAttributes.add("cdm_data_type", "Grid");

        //create dataVariables[]
        dataVariables = new EDV[tDataVariables.length];
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            String tDataSourceName = (String)tDataVariables[dv][0];
            String tDataDestName   = (String)tDataVariables[dv][1];
            Attributes tDataSourceAttributes = new Attributes();

            OpendapHelper.getAttributes(das, tDataSourceName, tDataSourceAttributes);

            //get the variable
            BaseType bt = dds.getVariable(tDataSourceName);  //throws Throwable if not found
            DArray mainDArray;
            if (bt instanceof DGrid) 
                mainDArray = (DArray)((DGrid)bt).getVar(0); //first element is always main array
            else if (bt instanceof DArray) 
                mainDArray = (DArray)bt;
            else throw new RuntimeException("dataVariable=" + tDataSourceName + " must be a DGrid or a DArray (" + 
                bt.toString() + ").");

            //look at the dimensions
            PrimitiveVector pv = mainDArray.getPrimitiveVector(); //just gets the data type
            //if (reallyVerbose) String2.log(tDataSourceName + " pv=" + pv);
            String dvSourceDataType = PrimitiveArray.elementClassToString( 
                OpendapHelper.getElementClass(pv));
            int numDimensions = mainDArray.numDimensions();
            if (dv == 0) {
                axisVariables = new EDVGridAxis[numDimensions];
            } else {
                Test.ensureEqual(numDimensions, axisVariables.length,
                    errorInMethod + "nDimensions was different for " +
                    "dataVariable#0=" + axisVariables[0].destinationName() + " and " +
                    "dataVariable#" + dv + "=" + tDataSourceName + ".");
            }
            for (int av = 0; av < numDimensions; av++) {

                DArrayDimension dad = mainDArray.getDimension(av);
                String tSourceAxisName = dad.getName();

               //ensure this dimension's name is the same as for the other dataVariables
                //(or as specified in tAxisVariables()[0])
                if (tAxisVariables == null) {
                    if (dv > 0)
                        Test.ensureEqual(tSourceAxisName, axisVariables[av].sourceName(),
                            errorInMethod + "Observed dimension name doesn't equal " +
                            "expected dimension name for dimension #" + av + 
                            " dataVariable#" + dv + "=" + tDataSourceName + 
                            " (compared to dataVariable#0).");
                } else {
                    Test.ensureEqual(tSourceAxisName, (String)tAxisVariables[av][0],
                        errorInMethod + "Observed dimension name doesn't equal " +
                        "expected dimension name for dimension #" + av + 
                        " dataVariable#" + dv + "=" + tDataSourceName + ".");
                    }
 
                //if dv!=0, nothing new to do, so continue
                if (dv != 0) 
                    continue;

                //do dv==0 things: create axisVariables[av]
                Attributes tSourceAttributes = new Attributes();
                PrimitiveArray tSourceValues = null;
                try {
                    dds.getVariable(tSourceAxisName); //throws NoSuchVariableException
                    OpendapHelper.getAttributes(das, tSourceAxisName, tSourceAttributes);
                    tSourceValues = quickRestartAttributes == null?
                        OpendapHelper.getPrimitiveArray(dConnect, "?" + tSourceAxisName) :
                        quickRestartAttributes.get(
                            "sourceValues_" + String2.encodeVariableNameSafe(tSourceAxisName));
                    if (reallyVerbose) {
                        int nsv = tSourceValues.size();
                        String2.log("    " + tSourceAxisName + 
                            " source values #0=" + tSourceValues.getString(0) +
                            " #" + (nsv-1) + "=" + tSourceValues.getString(nsv - 1));
                    }
                } catch (NoSuchVariableException nsve) {
                    //this occurs if no corresponding variable; ignore it
                    //make tSourceValues 0..dimensionSize-1
                    int dadSize1 = dad.getSize() - 1;
                    tSourceValues = av > 0 && dadSize1 < 32000? 
                        new ShortArray(0, dadSize1) :
                        new IntArray(0, dadSize1);
                    tSourceAttributes.add("units", "count"); //"count" is udunits;  "index" isn't, but better?
                    if (reallyVerbose) String2.log("    " + tSourceAxisName + 
                        " not found.  So made from indices 0 - " + dadSize1);
                } //but other exceptions aren't caught

                Attributes tAddAttributes = tAxisVariables == null?
                    new Attributes() : (Attributes)tAxisVariables[av][2];
                String tDestinationAxisName = tAxisVariables == null? null : 
                    (String)tAxisVariables[av][1];
                if (tDestinationAxisName == null || tDestinationAxisName.trim().length() == 0)
                    tDestinationAxisName = tSourceAxisName;

                //String2.log(tSourceAxisName + " pa=" + tSourceValues);             
                //is this the lon axis?
                if (EDV.LON_NAME.equals(tDestinationAxisName)) {
                    lonIndex = av;
                    axisVariables[av] = new EDVLonGridAxis(tSourceAxisName, 
                        tSourceAttributes, tAddAttributes, tSourceValues);

                //is this the lat axis?
                } else if (EDV.LAT_NAME.equals(tDestinationAxisName)) {
                    latIndex = av;
                    axisVariables[av] = new EDVLatGridAxis(tSourceAxisName, 
                        tSourceAttributes, tAddAttributes, tSourceValues);

                //is this the alt axis?
                } else if (EDV.ALT_NAME.equals(tDestinationAxisName)) {
                    altIndex = av;
                    axisVariables[av] = new EDVAltGridAxis(tSourceAxisName, 
                        tSourceAttributes, tAddAttributes, tSourceValues,
                        tAltMetersPerSourceUnit);

                //is this the time axis?
                } else if (EDV.TIME_NAME.equals(tDestinationAxisName)) {
                    timeIndex = av;
                    axisVariables[av] = new EDVTimeGridAxis(tSourceAxisName, 
                        tSourceAttributes, tAddAttributes, tSourceValues);

                //it is some other axis variable
                } else {
                    axisVariables[av] = new EDVGridAxis(
                        tSourceAxisName, tDestinationAxisName,
                        tSourceAttributes, tAddAttributes, tSourceValues);
                    axisVariables[av].setActualRangeFromDestinationMinMax();
                }
            }

            //create the EDVGridData
            dataVariables[dv] = new EDV(
                tDataSourceName, tDataDestName, 
                tDataSourceAttributes, (Attributes)tDataVariables[dv][2],
                dvSourceDataType, 
                Double.NaN, Double.NaN);  //hard to get min and max
            dataVariables[dv].extractAndSetActualRange();

        }

        //ensure the setup is valid
        ensureValid();

        //save quickRestart info
        if (quickRestartAttributes == null) { //i.e., there is new info
            try {
                quickRestartAttributes = new Attributes();
                quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
                quickRestartAttributes.set("dasBytes", new ByteArray(dasBytes));
                quickRestartAttributes.set("ddsBytes", new ByteArray(ddsBytes));
                for (int av = 0; av < axisVariables.length; av++) {
                    quickRestartAttributes.set(
                        "sourceValues_" + 
                            String2.encodeVariableNameSafe(axisVariables[av].sourceName()),
                        axisVariables[av].sourceValues());
                }
                File2.makeDirectory(File2.getDirectory(quickRestartFullFileName()));
                NcHelper.writeAttributesToNc(quickRestartFullFileName(), 
                    quickRestartAttributes);
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridFromDap " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @param tLocalSourceUrl
     * @param ensureAxisValuesAreEqual If Integer.MAX_VALUE, no axis sourceValue tests are performed. 
     *    If 0, this tests if sourceValues for axis-variable #0+ are same.
     *    If 1, this tests if sourceValues for axis-variable #1+ are same.
     *    (This is useful if the, for example, lat and lon values vary slightly and you 
     *    are willing to accept the initial values as the correct values.)
     *    Actually, the tests are always done but this determines whether
     *    the error is just logged or whether it throws an exception.
     * @param shareInfo if true, this ensures that the sibling's 
     *    axis and data variables are basically the same as this datasets,
     *    and then makes the new dataset point to the this instance's data structures
     *    to save memory. (AxisVariable #0 isn't duplicated.)
     *    Saving memory is important if there are 1000's of siblings in ERDDAP.
     * @return EDDGrid
     * @throws Throwable if trouble  (e.g., try to shareInfo, but datasets not similar)
     */
    public EDDGrid sibling(String tLocalSourceUrl, int ensureAxisValuesAreEqual, boolean shareInfo) throws Throwable {
        if (verbose) String2.log("EDDGridFromDap.sibling " + tLocalSourceUrl);

        int nAv = axisVariables.length;
        Object tAxisVariables[][] = new Object[nAv][3];
        for (int av = 0; av < nAv; av++) {
            tAxisVariables[av][0] = axisVariables[av].sourceName();
            tAxisVariables[av][1] = axisVariables[av].destinationName();
            tAxisVariables[av][2] = axisVariables[av].addAttributes();
        }
        //String2.getStringFromSystemIn("\nsibling axis0 addAtts=\n" + axisVariables[0].addAttributes());

        int nDv = dataVariables.length;
        Object tDataVariables[][] = new Object[nDv][3];
        for (int dv = 0; dv < nDv; dv++) {
            tDataVariables[dv][0] = dataVariables[dv].sourceName();
            tDataVariables[dv][1] = dataVariables[dv].destinationName();
            tDataVariables[dv][2] = dataVariables[dv].addAttributes();
        }
 
        //need a unique datasetID for sibling 
        //  so cached .das .dds axis values are stored separately.
        //So make tDatasetID by putting md5Hex12 in the middle of original datasetID
        //  so beginning and ending for tDatasetID are same as original.
        int po = datasetID.length() / 2; 
        String tDatasetID = datasetID.substring(0, po) +
            "_" + String2.md5Hex12(tLocalSourceUrl) + "_" +
            datasetID.substring(po);

        //make the sibling
        EDDGridFromDap newEDDGrid = new EDDGridFromDap(
            tDatasetID, 
            String2.toSSVString(accessibleTo),
            shareInfo? onChange : (StringArray)onChange.clone(), "", "", 
            addGlobalAttributes,
            altIndex < 0? 1 : ((EDVAltGridAxis)axisVariables[altIndex]).metersPerSourceUnit(),  
            tAxisVariables,
            tDataVariables,
            getReloadEveryNMinutes(),
            tLocalSourceUrl);

        //if shareInfo, point to same internal data
        if (shareInfo) {

            //ensure similar
            boolean testAV0 = false;
            String results = similar(newEDDGrid, ensureAxisValuesAreEqual, testAV0); 
            if (results.length() > 0)
                throw new SimpleException("Error in EDDGrid.sibling: " + results);

            //shareInfo
            for (int av = 1; av < nAv; av++) //not av0
                newEDDGrid.axisVariables()[av] = axisVariables[av];
            newEDDGrid.dataVariables = dataVariables;

            //shareInfo  (the EDDGrid variables)
            newEDDGrid.axisVariableSourceNames      = axisVariableSourceNames(); //() makes the array
            newEDDGrid.axisVariableDestinationNames = axisVariableDestinationNames();

            //shareInfo  (the EDD variables)
            newEDDGrid.dataVariableSourceNames      = dataVariableSourceNames();
            newEDDGrid.dataVariableDestinationNames = dataVariableDestinationNames();
            newEDDGrid.title                        = title();
            newEDDGrid.summary                      = summary();
            newEDDGrid.institution                  = institution();
            newEDDGrid.infoUrl                      = infoUrl();
            newEDDGrid.cdmDataType                  = cdmDataType();
            newEDDGrid.searchBytes                  = searchBytes();
            //not sourceUrl, which will be different
            newEDDGrid.sourceGlobalAttributes       = sourceGlobalAttributes();
            newEDDGrid.addGlobalAttributes          = addGlobalAttributes();
            newEDDGrid.combinedGlobalAttributes     = combinedGlobalAttributes();

        }

        return newEDDGrid;
    }

    /** 
     * This gets source data (not yet converted to destination data) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDataVariables
     * @param tConstraints
     * @return a PrimitiveArray[] where the first axisVariables.length elements
     *   are the axisValues and the next tDataVariables.length elements
     *   are the dataValues.
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble
     */
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        //build String form of the constraint
        //String errorInMethod = "Error in EDDGridFromDap.getSourceData for " + datasetID + ": "; 
        String constraint = buildDapArrayQuery(tConstraints);

        DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
        PrimitiveArray results[] = new PrimitiveArray[axisVariables.length + tDataVariables.length];
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            //???why not get all the dataVariables at once?
            //thredds has (and other servers may have) limits to the size of a given request
            //so breaking into parts avoids the problem.

            //get the data
            PrimitiveArray pa[] = null;
            try {
                pa = OpendapHelper.getPrimitiveArrays(dConnect, 
                    "?" + tDataVariables[dv].sourceName() + constraint);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

                //if too much data, rethrow t
                String tToString = t.toString();
                if (tToString.indexOf(Math2.memoryTooMuchData) >= 0)
                    throw t;

                requestReloadASAP(); 
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + EDStatic.errorFromDataSource + t.toString() + ")", 
                    t); 
            }

            if (pa.length == 1) {
                //it's a DArray
                if (dv == 0) {
                    //GridDataAccessor compares observed and expected axis values
                    int av3 = 0;
                    for (int av = 0; av < axisVariables.length; av++) {
                        results[av] = axisVariables[av].sourceValues().subset(
                            tConstraints.get(av3), 
                            tConstraints.get(av3 + 1), 
                            tConstraints.get(av3 + 2));
                        av3 += 3;
                    }
                }

            } else if (pa.length == axisVariables.length + 1) {
                //it's a DGrid;  test the axes
                if (dv == 0) {
                    //GridDataAccessor compares observed and expected axis values
                    for (int av = 0; av < axisVariables.length; av++) {
                        results[av] = pa[av + 1];
                    }
                } else if (pa.length != 1) {
                    for (int av = 0; av < axisVariables.length; av++) {
                        String tError = results[av].almostEqual(pa[av + 1]); 
                        if (tError.length() > 0) {
                            requestReloadASAP(); 
                            throw new WaitThenTryAgainException(
                                EDStatic.waitThenTryAgain +
                                "\nDetails: The axis values for dataVariable=0,axis=" + av +  
                                ")\ndon't equal the axis values for dataVariable=" + dv + ",axis=" + av + ".\n" +
                                tError);
                        }
                    }
                }

            } else {
                requestReloadASAP(); 
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\nDetails: An unexpected data structure was returned from the source (size observed=" + 
                    pa.length + ", expected=" + (axisVariables.length + 1) + ").");
            }

            //store the grid data 
            results[axisVariables.length + dv] = pa[0];
        }
        return results;
    }

    /** 
     * This does its best to generate a clean, ready-to-use datasets.xml entry 
     * for an EDDGridFromDap.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>If this fails because no Grid or Array variables found, it automatically calls
     * EDDTableFromDapSequence.generateDatasetsXml to see if that works.
     *
     * @param writeDirections
     * @param tLocalSourceUrl the base url for the dataset (no extension), e.g., 
     *   "http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day"
     * @param das  The das for the tLocalSourceUrl, or null.
     * @param dds  The dds for the tLocalSourceUrl, or null.
     * @param dimensionNames If not null, only the variables that use these
     *   dimensions, in this order, will be loaded.   
     *   If it is null, the vars with the most dimensions (found first, if tie) will be loaded.
     * @param tReloadEveryNMinutes  must be a valid value, e.g., 1440 for once per day. 
     *    Use, e.g., 1000000000, for never reload.
     * @param externalAddGlobalAttributes globalAttributes gleaned from external 
     *    sources, e.g., a THREDDS catalog.xml file.
     *    These have priority over other sourceGlobalAttributes.
     *    Okay to use null if none.
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(boolean writeDirections, 
        String tLocalSourceUrl, DAS das, DDS dds, String dimensionNames[], 
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDGridFromDap.generateDatasetsXml\n  tLocalSourceUrl=" + tLocalSourceUrl);
        String dimensionNamesCsv = String2.toCSSVString(dimensionNames);
        if (dimensionNames != null) String2.log("  dimensionNames=" + dimensionNamesCsv);
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);

        //get DConnect
        DConnect dConnect = new DConnect(tLocalSourceUrl, acceptDeflate, 1, 1);
        try {
            if (das == null)
                das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DAS from " + tLocalSourceUrl + ".das .\n" +
                t.getMessage());
        }
        try {
            if (dds == null) 
                dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DDS from " + tLocalSourceUrl + ".dds .\n" +
                t.getMessage());
        }

        //create tables to hold info
        Table axisSourceTable = new Table();  
        Table dataSourceTable = new Table();  
        Table axisAddTable = new Table();  
        Table dataAddTable = new Table();  

        //read through the variables[]
        HashSet dimensionNameCsvsFound = new HashSet();
        Enumeration vars = dds.getVariables();
        StringBuilder results = new StringBuilder();
        //if dimensionName!=null, this notes if a var with another dimension combo was found
        boolean otherComboFound = false; 
        String dimensionNamesInBrackets = null;
        if (writeDirections) 
            results.append(directionsForGenerateDatasetsXml() + "-->\n\n");
        NEXT_VAR:
        while (vars.hasMoreElements()) {
            BaseType bt = (BaseType)vars.nextElement();
            String dName = bt.getName();

            //ensure it is a DGrid or DArray
            DArray mainDArray;
            if (bt instanceof DGrid) 
                mainDArray = (DArray)((DGrid)bt).getVariables().nextElement(); //first element is always main array
            else if (bt instanceof DArray) 
                mainDArray = (DArray)bt;
            else continue;

            //if it's a coordinate variable, skip it
            int numDimensions = mainDArray.numDimensions();
            if (numDimensions == 1 && 
                mainDArray.getDimension(0).getName().equals(dName))
                continue; 

            //reduce numDimensions by 1 if String var
            PrimitiveVector pv = mainDArray.getPrimitiveVector(); //just gets the data type
            String dvSourceDataType = PrimitiveArray.elementClassToString( 
                OpendapHelper.getElementClass(pv));           
            if (dvSourceDataType.equals("String"))
                numDimensions--;

            //skip if numDimensions == 0
            if (numDimensions == 0)
                continue;
            //skip if combo is 1D bnds=bounds info
            if (numDimensions == 1 && mainDArray.getDimension(0).getName().equals("bnds")) 
                continue;
            //skip if combo is 2D bnds=bounds info
            if (numDimensions == 2 && mainDArray.getDimension(1).getName().equals("bnds"))
                continue;

            //if dimensionNames wasn't specified, use this method call to just look for dimensionName combos
            if (dimensionNames == null) {
                //has this combo of dimensionNames been seen?
                String tDimensionNames[] = new String[numDimensions];
                for (int av = 0; av < numDimensions; av++) 
                    tDimensionNames[av] = mainDArray.getDimension(av).getName();
                String dimCsv = String2.toCSSVString(tDimensionNames);
                boolean alreadyExisted = !dimensionNameCsvsFound.add(dimCsv);
                if (reallyVerbose) String2.log(
                    "  var=" + String2.left(dName, 12) + 
                    String2.left(" dims=\"" + dimCsv + "\"", 50) + 
                    " alreadyExisted=" + alreadyExisted);
                if (!alreadyExisted) {
                    //It shouldn't fail. But if it does, keep going.
                    try {
                        results.append(generateDatasetsXml(false, //writeDirections handled above
                            tLocalSourceUrl, das, dds, tDimensionNames, tReloadEveryNMinutes,
                            externalAddGlobalAttributes));
                    } catch (Throwable t) {
                        String2.log("ERROR: Unexpected error in generateDatasetsXml for " + dimCsv + 
                            "\nfor tLocalSourceUrl=" + tLocalSourceUrl + "\n" +
                            MustBe.throwableToString(t));
                    }
                }
                continue;
            }

            //if dimensionNames was specified, ensure current dimension names match it
            if (dimensionNames.length != numDimensions) {
                otherComboFound = true;
                continue NEXT_VAR;
            }
            for (int av = 0; av < numDimensions; av++) {
                DArrayDimension dad = mainDArray.getDimension(av);
                if (!dimensionNames[av].equals(dad.getName())) {
                    otherComboFound = true;
                    continue NEXT_VAR;
                }
            }
            //and if all ok, it falls through and continues

            //first data variable found? create axis tables
            if (axisSourceTable.nColumns() == 0) {
                StringBuilder namesInBrackets = new StringBuilder();
                for (int av = 0; av < numDimensions; av++) {
                    DArrayDimension dad = mainDArray.getDimension(av);
                    String aName = dad.getName();
                    Attributes sourceAtts = new Attributes();
                    try {
                        OpendapHelper.getAttributes(das, aName, sourceAtts);
                    } catch (Throwable t) {
                        //e.g., ignore exception for dimension without corresponding coordinate variable
                    }
                    Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                        sourceAtts, aName, false, true); //addColorBarMinMax, tryToFindLLAT
                    axisSourceTable.addColumn(axisSourceTable.nColumns(), aName, new DoubleArray(), sourceAtts); //type doesn't matter here
                    axisAddTable.addColumn(   axisAddTable.nColumns(),    aName, new DoubleArray(), addAtts);    //type doesn't matter here

                    //accumulate namesInBrackets
                    namesInBrackets.append("[" + 
                        suggestDestinationName(aName, sourceAtts.getString("units"), 
                            sourceAtts.getFloat("scale_factor"), true) +  //lookForLLAT
                        "]");
                }
                dimensionNamesInBrackets = namesInBrackets.toString();
            }

            //add the data variable to dataAddTable
            Attributes sourceAtts = new Attributes();
            OpendapHelper.getAttributes(das, dName, sourceAtts);
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                sourceAtts, dName, true, false); //addColorBarMinMax, tryToFindLLAT
            if (tLocalSourceUrl.indexOf("ncep") >= 0 &&
                tLocalSourceUrl.indexOf("reanalysis") >= 0)
                addAtts.add("drawLandMask", "under");

            dataSourceTable.addColumn(dataSourceTable.nColumns(), dName, new DoubleArray(), sourceAtts); //type doesn't matter here
            dataAddTable.addColumn(   dataAddTable.nColumns(),    dName, new DoubleArray(), addAtts);    //type doesn't matter here
        }

        //*** after data variables known, improve global attributes in axisAddTable
        OpendapHelper.getAttributes(das, "GLOBAL", axisSourceTable.globalAttributes());
        axisAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                axisSourceTable.globalAttributes(), 
                "Grid",  //another cdm type could be better; this is ok
                tLocalSourceUrl, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));


        //if dimensionNames wasn't specified, this is controller, so were're done
        if (dimensionNames == null) {
            //success?
            if (dimensionNameCsvsFound.size() > 0)
                return results.toString();

            try {
                //see if it is a DAP sequence dataset
                if (verbose) String2.log("!!! No Grid or Array variables found, " +
                    "so ERDDAP will check if the dataset is a DAP sequence ...");
                return EDDTableFromDapSequence.generateDatasetsXml(tLocalSourceUrl, 
                    tReloadEveryNMinutes, externalAddGlobalAttributes);

            } catch (Throwable t) {
                //if EDDTableFromDapSequece throws exception, then throw exception (below) for original problem
            }
        }


        //ensure that variables with the dimensionNames were found
        if (axisAddTable.nColumns() == 0 || dataAddTable.nColumns() == 0) {
            throw new SimpleException("No Grid or Array variables with dimensions=\"" + 
                dimensionNamesCsv + "\" were found for " + tLocalSourceUrl + ".dds.");
        }

        //***Here down, we know dimensionNames != null

        //if otherComboFound, add dimensionNameInBrackets to title and use to make datasetID
        String tDatasetID = suggestDatasetID(tPublicSourceUrl); 
        if (otherComboFound) {
            
            //change title
            String tTitle = axisAddTable.globalAttributes().getString("title");
            if (tTitle == null) 
                tTitle = axisSourceTable.globalAttributes().getString("title");
            axisAddTable.globalAttributes().set("title", tTitle + " " + dimensionNamesInBrackets);

            //change tDatasetID
            tDatasetID = suggestDatasetID(tPublicSourceUrl + "?" + dimensionNamesInBrackets); //don't change this! else id's will change in various erddaps
        }

        //write the information
        results.append(
            "<dataset type=\"EDDGridFromDap\" datasetID=\"" + tDatasetID + "\" active=\"true\">\n" +
            "    <sourceUrl>" + tLocalSourceUrl + "</sourceUrl>\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            //altitude is only relevant if var is already called "altitude".
            //(new GenerateDatasetsXml doesn't change depth to altitude) so units probably already meters up
            "    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n");  
        results.append(writeAttsForDatasetsXml(false, axisSourceTable.globalAttributes(), "    "));
        results.append(writeAttsForDatasetsXml(true,  axisAddTable.globalAttributes(),    "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        results.append(writeVariablesForDatasetsXml(axisSourceTable, axisAddTable, "axisVariable", false, true,  false));
        results.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", false, false, false));
        results.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return results.toString();
    }

    /** This calls generateDatasetsXml within try/catch so very safe. 
     * Only completely successful xml will be added to results.
     *
     * @param tLocalSourceUrl 
     * @param tReloadEveryNMinutes
     * @param externalAddGlobalAttributes
     * @param results to capture the results
     * @param summary captures the summary of what was done.
     * @param indent a string of spaces to be used to indent info added to summary
     * @param datasetSuccessTimes an int[String2.DistributionSize] to capture successful
     *    generateDatasetXml times
     * @param datasetFailureTimes an int[String2.DistributionSize] to capture unsuccessful
     *    generateDatasetXml times
     */
    public static void safelyGenerateDatasetsXml(String tLocalSourceUrl, 
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes,
        Writer results, StringBuilder summary, String indent, 
        int datasetSuccessTimes[], int datasetFailureTimes[]) {

        long time = System.currentTimeMillis();
        try {
            //append to results  (it should succeed completely, or fail)
            results.write(generateDatasetsXml(false, tLocalSourceUrl, 
                null, null, null,
                tReloadEveryNMinutes, externalAddGlobalAttributes));
            time = System.currentTimeMillis() - time;
            String2.distribute(time, datasetSuccessTimes);
            String ts = indent + tLocalSourceUrl + "  (" + time + " ms)\n";
            summary.append(ts);
            System.out.print(ts);

        } catch (Throwable t) {
            String2.log(String2.ERROR + " in safelyGenerateDatasetsXml\n" +
                "  for tLocalSourceUrl=" + tLocalSourceUrl + "\n" +
                MustBe.throwableToString(t));
            time = System.currentTimeMillis() - time;
            String2.distribute(time, datasetFailureTimes);
            String ts = indent + tLocalSourceUrl + "  (" + time + " ms)\n" +
                 indent + "  " + String2.ERROR + ": " + 
                    String2.replaceAll(MustBe.getShortErrorMessage(t), "\n", "\n  " + indent) + 
                    "\n";
            summary.append(ts);
            System.out.print(ts);
        } 
    }

    /**
     * This is the low-level method which generates datasets.xml for 
     * multiple datasets from a THREDDS server.
     * <br>This calls itself recursively, adding into to fileNameInfo as it is found.
     * <br>If there is trouble (e.g., an exception), this catches it and returns.
     * <br>http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html

     * <p>Unsolved problem: this does nothing for detecting groups of files/URLs 
     * that should be aggregated.
     *
     * @param tLocalSourceUrl the tLocalSourceUrl of the current Thredds xml catalog (which usually includes /catalog/), e.g.,
     *    http://thredds1.pfeg.noaa.gov/thredds/catalog/catalog.xml 
     *    http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml
     *    (note that comparable .html is at
     *    http://thredds1.pfeg.noaa.gov/thredds/Satellite/aggregsatMH/chla/catalog.html ).
     * 
     *    <p> If /catalog/ isn't in the tLocalSourceUrl, it will be added, 
     *    <br> e.g., http://thredds1.pfeg.noaa.gov/thredds/catalog.xml 
     *    <br> becomes http://thredds1.pfeg.noaa.gov/thredds/catalog/catalog.xml 
     * @param datasetNameRegex  The lowest level name of the dataset must match this, 
     *    e.g., ".*" for all dataset Names.
     * @param recursive
     * @param parentName (currently not used) the &lt;name&gt; of the parent dataset in the parent catalog, 
     *     e.g., "NCEP MODEL DATA" or "" or null
     * @param tReloadEveryNMinutes
     * @param justURLs true=just write the matching URLs; false=generateDatasetsXml for each URL.
     * @param results captures all of the results. It isn't closed when the method is done.
     * @param summary captures the summary of what was done.
     * @param catalogXmlTimes an int[String2.DistributionSize] to capture catalogXml processing times
     *     (including children)
     * @param datasetSuccessTimes an int[String2.DistributionSize] to capture successful
     *    generateDatasetXml times
     * @param datasetFailureTimes an int[String2.DistributionSize] to capture unsuccessful
     *    generateDatasetXml times
     * @param alreadyDone captures already processed catalog.xml tLocalSourceUrls and datasetIDs,
     *    so there is no duplication
     */
    public static void recursivelyGenerateDatasetsXmlFromThreddsCatalog(String tLocalSourceUrl, 
        String datasetNameRegex, boolean recursive, String parentName, int tReloadEveryNMinutes, 
        boolean justURLs, Writer results, StringBuilder summary,
        int catalogXmlTimes[], int datasetSuccessTimes[], int datasetFailureTimes[],
        HashSet alreadyDone) {

        if (reallyVerbose) String2.log(
            "\n<<< recursivelyGenerateDatasetsXmlFromThreddsCatalogs  regex=" + datasetNameRegex +
            "\n  tLocalSourceUrl=" + tLocalSourceUrl);
        long time = System.currentTimeMillis();
        summary.append(  "\nStarting " + tLocalSourceUrl + "\n");
        System.out.print("\nStarting " + tLocalSourceUrl + "\n");
        if (!alreadyDone.add(tLocalSourceUrl)) {
            String ts = "Catalog.xml already done!\n";
            summary.append(ts);
            System.out.print(ts);
            return;
        }
        if (parentName == null)
            parentName = "";
        HashMap opendapServices = new HashMap();
        String parentServiceName = "";
        int datasetCount = 0;

        //for debugging/testing, this lets Bob limit the number of datasets processed during any call
        int datasetLimit = Integer.MAX_VALUE;  //normally Integer.MAX_VALUE, sometimes less when debugging

        try {
            String datasetTags[] = {
                "<catalog><dataset>",
                "<catalog><dataset><dataset>",
                "<catalog><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset><dataset>"};
            int nDatasetTags = datasetTags.length;
            String endDatasetTags[] = {
                "<catalog></dataset>",
                "<catalog><dataset></dataset>",
                "<catalog><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset></dataset>",
                "<catalog><dataset><dataset><dataset><dataset><dataset><dataset><dataset></dataset>"};
            String indent[] = {
                "  ", 
                "    ", 
                "      ", 
                "        ",
                "          ",
                "            ",
                "              ",
                "                "};

            int catPo = tLocalSourceUrl.indexOf( "/catalog/");
            if (catPo < 0) {
                if (verbose) String2.log("  WARNING: '/catalog/' not found in" +
                    //e.g., http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/
                    //        ncep.reanalysis.dailyavgs/surface/catalog.xml
                    "\n    tLocalSourceUrl=" + tLocalSourceUrl);
                int tPod = tLocalSourceUrl.indexOf("/thredds/dodsC/");
                int tPo  = tLocalSourceUrl.indexOf("/thredds/");
                if (tPod > 0)
                    tLocalSourceUrl = tLocalSourceUrl.substring(0, tPo + 9) + 
                        "catalog/" + tLocalSourceUrl.substring(tPo + 15);
                else if (tPo > 0)
                    tLocalSourceUrl = tLocalSourceUrl.substring(0, tPo + 9) + 
                        "catalog/" + tLocalSourceUrl.substring(tPo + 9);
                else 
                    tLocalSourceUrl = File2.getDirectory(tLocalSourceUrl) + 
                        "catalog/" + File2.getNameAndExtension(tLocalSourceUrl);
                //e.g., http://www.esrl.noaa.gov/psd/thredds/catalog/Datasets/
                //        ncep.reanalysis.dailyavgs/surface/catalog.xml
                if (verbose) String2.log("    so trying tLocalSourceUrl=" + tLocalSourceUrl);
                catPo = tLocalSourceUrl.indexOf( "/catalog/");
            }
            String catalogBase = tLocalSourceUrl.substring(0, catPo + 9); //ends in "/catalog/";
            if (reallyVerbose) String2.log("  catalogBase=" + catalogBase);

            //I could get inputStream from tLocalSourceUrl, 
            //  but then (via recursion) perhaps lots of streams open for a long time.
            //I think better to get the entire response (succeed or fail *now*).
            byte bytes[] = SSR.getUrlResponseBytes(tLocalSourceUrl);
            SimpleXMLReader xmlReader = new SimpleXMLReader(new ByteArrayInputStream(bytes));
            //e.g., threddsName=thredds
            String threddsName = File2.getNameAndExtension(tLocalSourceUrl.substring(0, catPo)); 
            int ssPo = tLocalSourceUrl.indexOf("//");
            if (ssPo < 0) 
                throw new SimpleException("'//' not found in tLocalSourceUrl=" + tLocalSourceUrl);
            int sPo = tLocalSourceUrl.indexOf('/', ssPo + 2);
            if (sPo < 0) 
                throw new SimpleException("'/' not found in tLocalSourceUrl=" + tLocalSourceUrl);
            //e.g., threddsBase=http://www.esrl.noaa.gov
            String threddsBase = tLocalSourceUrl.substring(0, sPo);
            if (reallyVerbose) String2.log("  threddsBase=" + threddsBase);
            String defaultService = null;
            String defaultDefaultService = "/" + threddsName + "/dodsC/";
            String name[] = new String[nDatasetTags];
            String urlPath[] = new String[nDatasetTags];
            Attributes extraGlobalAtts[] = new Attributes[nDatasetTags];
            String service[] = new String[nDatasetTags];
            String lastDocumentationType = null;
            int cLevel = -1;
            while (true) {
                xmlReader.nextTag();
                String tags = xmlReader.allTags();
                int whichDatasetTag    = String2.indexOf(datasetTags, tags);
                int whichEndDatasetTag = String2.indexOf(endDatasetTags, tags);
                //String2.log("  tags=" + tags);

                //catch <catalogRef>
                //<catalog><dataset><dataset><dataset>
                //  <catalogRef xlink:href="Satellite/aggregsatMH/chla/catalog.xml" xlink:title="Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality" name=""/>
                if (recursive && tags.endsWith("<catalogRef>")) {
                    String href = xmlReader.attributeValue("xlink:href");
                    if (href != null) {
                        if (!href.startsWith("http")) {  //if not a complete url
                            if (href.startsWith("/" + threddsName + "/catalog/"))
                                href = threddsBase + href;
                            else if (href.startsWith("./")) 
                                href = File2.getDirectory(tLocalSourceUrl) + href.substring(2);
                            else if (!href.startsWith("/")) 
                                href = File2.getDirectory(tLocalSourceUrl) + href;
                            else href = catalogBase + href.substring(1); //href starts with /
                        }
                        recursivelyGenerateDatasetsXmlFromThreddsCatalog(href,
                            datasetNameRegex, recursive, 
                            cLevel < 0? "" : name[cLevel],  //cLevel=-1 was parentName, now ""
                            tReloadEveryNMinutes, justURLs, results,
                            summary, catalogXmlTimes,
                            datasetSuccessTimes, datasetFailureTimes, alreadyDone);
                    }

                //catch services  (this supports 1 or 2 levels) 
                //<catalog name="Oceanwatch THREDDS Data Server" version="1.0.1">
                //  <service name="all" serviceType="Compound" base="">
                //    <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>
                //see http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/6F782D2B9E0DE28D6B06A1ADCDC7A363.xml
                } else if (tags.endsWith("<service><service>")) {
                    String tServiceType = xmlReader.attributeValue("serviceType");
                    String tServiceName = xmlReader.attributeValue("name");
                    String tServiceBase = xmlReader.attributeValue("base");
                    if ("OPENDAP".equals(tServiceType) &&
                        tServiceName != null && tServiceName.length() > 0 &&
                        tServiceBase != null && tServiceBase.length() > 0) {

                        opendapServices.put(tServiceName, tServiceBase);
                        opendapServices.put(parentServiceName, tServiceBase);
                        if (defaultService == null) defaultService = tServiceBase;
                        if (reallyVerbose) 
                            String2.log(
                                "  storing service name=" + tServiceName      + " base=" + tServiceBase + "\n" +
                                "  storing service name=" + parentServiceName + " base=" + tServiceBase);
                    }

                } else if (tags.endsWith("<service></service>")) {
                    //do nothing, but needed to distinguish from </service> below

                } else if (tags.endsWith("<service>")) {
                    String tServiceType = xmlReader.attributeValue("serviceType");
                    String tServiceName = xmlReader.attributeValue("name");
                    String tServiceBase = xmlReader.attributeValue("base");
                    parentServiceName = tServiceName;
                    if ("OPENDAP".equals(tServiceType) &&  
                        tServiceName != null && tServiceName.length() > 0 &&
                        tServiceBase != null && tServiceBase.length() > 0) {

                        opendapServices.put(tServiceName, tServiceBase);
                        if (defaultService == null) defaultService = tServiceBase;
                        if (reallyVerbose) 
                            String2.log("  storing service name=" + tServiceName + " base=" + tServiceBase);
                    }
                    
                } else if (tags.endsWith("</service>")) {
                    //clear parentServiceName
                    parentServiceName = "";


                //is this dataset service an opendap service?
                //e.g., <catalog><dataset><dataset><serviceName>localOPeNDAP</serviceName>
                //If two or more (!), the last one is used.
                } else if (tags.endsWith("</serviceName>")) {  
                    String tServiceName = xmlReader.content();
                    String tServiceBase = (String)opendapServices.get(tServiceName);
                    if (tServiceBase != null) {
                        service[Math.max(0, cLevel)] = tServiceBase;
                        if (reallyVerbose) 
                            String2.log("  cLevel=" + cLevel + " using serviceName=" + 
                                tServiceName + " base=" + tServiceBase);
                    }

                //catch <dataset>
                //  <dataset name="Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality">
                //    <dataset name="5-day" ID="satellite/MH/chla/5day" urlPath="satellite/MH/chla/5day">
                } else if (whichDatasetTag >= 0) {
                    cLevel = whichDatasetTag;
                    name[cLevel] = xmlReader.attributeValue("name");
                    if (name[cLevel] == null)
                        name[cLevel] = "";
                    //String2.log("  <dataset> cLevel=" + cLevel + " name=" + name[cLevel]);
                    urlPath[cLevel] = xmlReader.attributeValue("urlPath");
                    extraGlobalAtts[cLevel] = new Attributes();   //see "inherited" below
                    service[cLevel] = cLevel == 0? 
                        (defaultService == null? defaultDefaultService : defaultService) :
                        (String)service[cLevel - 1];
                    String ts = indent[cLevel] + 
                        (name[cLevel].length() == 0? "(No Name)" : name[cLevel]) + 
                        "\n";
                    summary.append(ts);
                    System.out.print(ts);


                //catch </dataset> and create a dataset
                //  <dataset name="Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality">
                //    <dataset name="5-day" ID="satellite/MH/chla/5day" urlPath="satellite/MH/chla/5day">
                } else if (whichEndDatasetTag >= 0) {
                    if (datasetCount++ >= datasetLimit) {
                        xmlReader.close();
                        break;
                    }
                    cLevel = whichEndDatasetTag;  //it should be already
                    String tUrlPath = urlPath[cLevel];
                    if (!name[cLevel].matches(datasetNameRegex)) {
                        //skip this dataset
                        if (verbose)
                            String2.log("regex=" + datasetNameRegex + " rejectedName=" + name[cLevel]);
                    } else if (tUrlPath != null) {
                        //try to make this dataset
                        if (tUrlPath.startsWith("http")) { }
                        else if (service[cLevel].startsWith("http")) 
                             tUrlPath = service[cLevel] + tUrlPath;
                        else tUrlPath = threddsBase + service[cLevel] + tUrlPath;

                        //is there a port number in the url that can be removed?
                        String port = String2.extractRegex(tUrlPath, ":\\d{4}/", 0);
                        if (port != null) {
                            //there is a port number
                            //String2.log("!!!found port=" + port); Math2.sleep(1000);
                            String ttUrlPath = String2.replaceAll(tUrlPath, port, "/");
                            try {
                                String dds = SSR.getUncompressedUrlResponseString(ttUrlPath + ".dds");
                                if (dds.startsWith("Dataset {")) {
                                    //it can be removed
                                    //String2.log("!!!port=" + port + " was removed"); Math2.sleep(1000);
                                    tUrlPath = ttUrlPath;
                                }
                            } catch (Throwable t) {
                                //it can't be removed
                                //String2.log(t.toString()); Math2.sleep(1000);
                            }
                        }

                        //accumulate tTitle from the parent names (but not if duplicate info or too long)
                        String tTitle = name[cLevel];
                        for (int tcLevel = cLevel - 1; tcLevel >= 0; tcLevel--) {
                            String tParentName = cLevel <= 0? parentName : name[tcLevel];
                            if (tParentName.length() == 0 ||      //no info
                                tTitle.toLowerCase().indexOf(tParentName.toLowerCase()) >= 0) //duplicate info
                                continue;
                            if (tParentName.length() + 2 + tTitle.length() > 80)  //too long
                                break;
                            tTitle = tParentName + ", " + tTitle; 
                        }

                        //suggest title
                        tTitle = removeExtensionsFromTitle(tTitle); //e.g., .grib
                        tTitle = String2.replaceAll(tTitle, '/', ' ');
                        tTitle = String2.replaceAll(tTitle, '_', ' ');
                        extraGlobalAtts[cLevel].add("title", tTitle);
                        //if (reallyVerbose) String2.log("  extraGlobalAtts[" + cLevel + "] title=" + tTitle);

                        //suffixForTitle  (otherwise, too many datasets have same title)
                        //name[cLevel] e.g. Thredds currentLevel name: "5-day" or "sal001", sometimes long
                        String sft = removeExtensionsFromTitle(name[cLevel]); 
                        //sft = String2.replaceAll(sft, '/', ' ');
                        //sft = String2.replaceAll(sft, '_', ' ');
                        extraGlobalAtts[cLevel].add("suffixForTitle", sft);

                        if (alreadyDone.add(tUrlPath)) {
                            String tName = File2.getNameAndExtension(tUrlPath);
                            if (tName.startsWith("catalog") && tName.endsWith(".html")) {
                                //UAF clean catalog has these when no valid dataset in directory
                                String ts = indent[cLevel] + "  Skipping UAF catalog.html dataset: " + tUrlPath + "\n";
                                summary.append(ts);
                                System.out.print(ts);
                            } else if (justURLs) {
                                results.write(tUrlPath + "\n");
                            } else {
                                safelyGenerateDatasetsXml(tUrlPath, 
                                    tReloadEveryNMinutes, extraGlobalAtts[cLevel], 
                                    results, summary, indent[cLevel] + "  ", 
                                    datasetSuccessTimes, datasetFailureTimes);
                            }
                        } else {
                            String ts = indent[cLevel] + "  Dataset already done: " + tUrlPath + "\n";
                            summary.append(ts);
                            System.out.print(ts);
                        }
                    }
                    cLevel--;  //result may be -1


                //catch metadata
                //see 
                //  <dataset name="5-day" ID="satellite/MH/chla/5day" urlPath="satellite/MH/chla/5day">
                //  <metadata inherited="true">
                //    <authority>gov.noaa.pfel.coastwatch</authority>
                //    <dataType>GRID</dataType>
                //    <dataFormat>NetCDF</dataFormat>
                //    <documentation type="Summary">....</documentation>
                //    <documentation xlink:href="http://coastwatch.pfel.noaa.gov/infog/MH_chla_las.html" xlink:title="Dataset Summary"/>
                //    <creator>
                //      <name>NASA GSFC (G. Feldman)</name>
                //      <contact url="" email=""/>
                //    </creator>
                //    <publisher>
                //      <name>NOAA CoastWatch, West Coast Node</name>
                //      <contact url="http://coastwatch.pfel.noaa.gov" email="dave.foley@noaa.gov"/>
                //        </publisher>
                } else if (cLevel >= 0 && tags.endsWith("<metadata>")) {
                    if (cLevel > 0 && "true".equals(xmlReader.attributeValue("inherited")))
                        extraGlobalAtts[cLevel] = (Attributes)extraGlobalAtts[cLevel - 1].clone();
                } else if (cLevel >= 0 && tags.endsWith("<metadata></authority>")) {
                    extraGlobalAtts[cLevel].add(    "authority", xmlReader.content());

                } else if (cLevel >= 0 && tags.endsWith("<metadata><creator></name>")) {
                    extraGlobalAtts[cLevel].add(    "creator_name", xmlReader.content());

                } else if (cLevel >= 0 && tags.endsWith("<metadata><creator><contact>")) {
                    extraGlobalAtts[cLevel].add(    "creator_email", xmlReader.attributeValue("email"));
                    extraGlobalAtts[cLevel].add(    "creator_url",   xmlReader.attributeValue("url"));

                } else if (cLevel >= 0 && tags.endsWith("<metadata></date>")) {
                    extraGlobalAtts[cLevel].add(    "date", xmlReader.content());

                } else if (cLevel >= 0 && tags.endsWith("<metadata><documentation>")) {
                    //all types should be lowercase, sometimes they aren't
                    lastDocumentationType = xmlReader.attributeValue("type");
                    if (lastDocumentationType != null)
                        lastDocumentationType = lastDocumentationType.toLowerCase();

                    //deal with xlinks
                    String xHref  = xmlReader.attributeValue("xlink:href");
                    String xTitle = xmlReader.attributeValue("xlink:title");
                    if (xHref  != null && xHref.length()  > 0 &&
                        xTitle != null && xTitle.length() > 0) {
                        //String2.log("  xHref=" + xHref + "\n  xTitle=" + xTitle);
                        if (xTitle.equals("Dataset Summary"))
                            xTitle = "infoUrl";
                        xTitle = String2.replaceAll(xTitle, '.', '_');
                        xTitle = String2.replaceAll(xTitle, '-', '_');
                        xTitle = String2.modifyToBeFileNameSafe(xTitle);
                        extraGlobalAtts[cLevel].add(xTitle, xHref);
                        //store first .html found as infoUrl
                        if (xHref.endsWith(".html") &&
                            extraGlobalAtts[cLevel].getString("infoUrl") == null)
                            extraGlobalAtts[cLevel].add("infoUrl", xHref);
                    }

                } else if (cLevel >= 0 && tags.endsWith("<metadata></documentation>")) {
                    if (lastDocumentationType != null && lastDocumentationType.length() > 0) {
                        if (lastDocumentationType.equals("rights"))
                            lastDocumentationType = "license";
                        extraGlobalAtts[cLevel].add(
                            lastDocumentationType, 
                            xmlReader.content());
                    }

                } else if (cLevel >= 0 && tags.endsWith("<metadata></keyword>")) {
                    extraGlobalAtts[cLevel].add(    "keywords", xmlReader.content());

                } else if (cLevel >= 0 && tags.endsWith("<metadata></project>")) {
                    extraGlobalAtts[cLevel].add(    "project", xmlReader.content());

                } else if (cLevel >= 0 && tags.endsWith("<metadata><publisher></name>")) {
                    extraGlobalAtts[cLevel].add(    "publisher_name", xmlReader.content());

                } else if (cLevel >= 0 && tags.endsWith("<metadata><publisher><contact>")) {
                    extraGlobalAtts[cLevel].add(    "publisher_email", xmlReader.attributeValue("email"));
                    extraGlobalAtts[cLevel].add(    "publisher_url",   xmlReader.attributeValue("url"));

                //done
                } else if (tags.equals("</catalog>")) {
                    xmlReader.close();
                    break;
                }
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            String ts = "  " + String2.ERROR + ": " + 
                String2.replaceAll(MustBe.throwableToShortString(t), "\n", "\n  ") +
                "\n";
            summary.append(ts);
            System.out.print(ts);
        }
        if (reallyVerbose) String2.log(
            "\n>>> leaving recursivelyGenerateDatasetsXmlFromThreddsCatalogs\n" +
            "  tLocalSourceUrl=" + tLocalSourceUrl);
        time = System.currentTimeMillis() - time;
        String2.distribute(time, catalogXmlTimes);
        String ts = "Finished " + tLocalSourceUrl + "  (" + 
            Calendar2.elapsedTimeString(time) + ")\n\n";
        summary.append(ts);
        System.out.print(ts);
    }

    /**
     * This gets matching datasetURLs from a thredds catalog.
     * 
     * @param startUrl http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml
     * @param datasetNameRegex e.g. ".*\.nc"
     * @param recursive

     */
    public static String[] getUrlsFromThreddsCatalog(String startUrl, 
        String datasetNameRegex, boolean recursive) {

        if (verbose) String2.log("getUrlsFromThreddsCatalog regex=" + datasetNameRegex);
        Writer results = new StringWriter();
        StringBuilder summary = new StringBuilder();
        int catalogXmlTimes[]     = new int[String2.DistributionSize];
        int datasetSuccessTimes[] = new int[String2.DistributionSize]; //irrelevant; none created
        int datasetFailureTimes[] = new int[String2.DistributionSize]; //irrelevant; none created

        recursivelyGenerateDatasetsXmlFromThreddsCatalog(
            startUrl, datasetNameRegex, recursive, "", 1440, true, //justURLs
            results, summary, catalogXmlTimes, 
            datasetSuccessTimes, datasetFailureTimes, new HashSet());

        //make the resultsArray
        String resultsString = results.toString();
        if (resultsString.length() > 0) //remove the trailing \n
            resultsString = resultsString.substring(0, resultsString.length() - 1);        
        String resultsArray[] = resultsString.length() == 0?
            new String[0] :
            String2.split(resultsString, '\n');

        //log the results and summary 
        if (verbose)
            String2.log("\n*** getUrlsFromThreddsCatalog finished. nUrls=" + resultsArray.length);
        if (reallyVerbose) {
            String2.log("\nRESULTS\n");
            String2.log(resultsString);
            String2.log("\nSUMMARY\n");
            String2.replaceAll(summary, "--", "- - "); //so no end-of-comment
            String2.log(XML.encodeAsXML(summary.toString()));
        }

        //log the statistics
        if (verbose) {
            String2.log(XML.encodeAsXML("\n" + 
                "catalog.xml Processing Times (including children)\n" +
                String2.getDistributionStatistics(catalogXmlTimes) + "\n\n"));
        }        

        return resultsArray;
    }

 
    /**
     * This tests getUrlsFromThreddsCatalog.
     *
     */
    public static void testGetUrlsFromThreddsCatalog() throws Throwable {
        String results[], expected[];


        //test for problem with threddsBaseUrl  found 2011-11  
        results = getUrlsFromThreddsCatalog(
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/catalog.xml", 
            "air\\.sig995\\.20[0-9]{2}\\.nc", false);
        expected = new String[]{
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2000.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2001.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2002.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2003.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2004.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2005.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2006.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2007.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2008.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2009.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2010.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2011.nc",
            "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/ncep.reanalysis.dailyavgs/surface/air.sig995.2012.nc"};
        Test.ensureEqual(results, expected, "results0=" + String2.toNewlineString(results));

        //test no regex
        results = getUrlsFromThreddsCatalog(
            "http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml", 
            ".*", true);
        expected = new String[]{
            "http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day",
            "http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day",
            "http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/mday"};
        Test.ensureEqual(results, expected, "results1=" + String2.toNewlineString(results));

        //test regex
        results = getUrlsFromThreddsCatalog(
            "http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml", 
            "8.*", true);
        expected = new String[]{
            "http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day"};
        Test.ensureEqual(results, expected, "results2=" + String2.toNewlineString(results));



    
    }


    /** 
     * This runs generateDatasetsXmlFromThreddsCatalog.
     * 
     * @param oResultsFileName  If null, the procedure calls SSR.displayInBrowser.
     * @param oLocalSourceUrl  A complete URL of a thredds catalog xml file in the form
     *    it needs to be called local to this computer (e.g., perhaps numeric ip). 
     * @param datasetNameRegex  The lowest level name of the dataset must match this, 
     *    e.g., ".*" for all dataset Names.
     * @param tReloadEveryNMinutes
     */
    public static void generateDatasetsXmlFromThreddsCatalog(
        String oResultsFileName, 
        String oLocalSourceUrl, String datasetNameRegex, int tReloadEveryNMinutes) throws Throwable {

        if (oLocalSourceUrl == null)
            throw new RuntimeException("'localSourceUrl' is null.");
        runGenerateDatasetsXmlFromThreddsCatalog(oResultsFileName,
             oLocalSourceUrl, datasetNameRegex, tReloadEveryNMinutes);
    }


    /** 
     * This tests generateDatasetsXmlFromThreddsCatalogs.
     * 
     */
    public static void testGenerateDatasetsXmlFromThreddsCatalog() 
        throws Throwable {

        runGenerateDatasetsXmlFromThreddsCatalog(null, null,
             ".*", 10080);
    }


    /** 
     * This is a low level method used by generate.... and testGenerate... 
     * to run generateDatasetsXmlFromThreddsCatalogs.
     * 
     * @param oResultsFileName  If null, the procedure creates
     *    F:/u00/cwatch/erddap2/cache/_test/datasets.xml
     *    F:/u00/cwatch/erddap2/logs/log.txt or  F:/u00/cwatch/erddap2/cache/_test/datasets.xml.log.txt
     *    and calls SSR.displayInBrowser (so they are displayed in EditPlus).
     * @param oLocalSourceUrl  A complete URL of a thredds catalog xml file. If null, a standard test will be done.  
     * @param datasetNameRegex  The lowest level name of the dataset must match this, 
     *    e.g., ".*" for all dataset Names.
     * @param tReloadEveryNMinutes  e.g., weekly=10080
     */
    public static void runGenerateDatasetsXmlFromThreddsCatalog(
        String oResultsFileName,
        String oLocalSourceUrl, String datasetNameRegex, int tReloadEveryNMinutes) 
        throws Throwable {

        String resultsFileName = oResultsFileName == null?
            EDStatic.fullTestCacheDirectory + "datasets.xml" :
            oResultsFileName;

        //create a logFile if none active
        String oLogFileName = String2.logFileName();
        String logFileName = oLogFileName;
        if (oLogFileName == null) {
            logFileName = resultsFileName + ".log.txt";
            String2.setupLog(false, false, logFileName, false, false, 1000000000);
        }
        
        Writer results = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(resultsFileName), "ISO-8859-1"));
        results.write(directionsForGenerateDatasetsXml() + "-->\n\n");
        StringBuilder summary = new StringBuilder();
        int catalogXmlTimes[]     = new int[String2.DistributionSize];
        int datasetSuccessTimes[] = new int[String2.DistributionSize];
        int datasetFailureTimes[] = new int[String2.DistributionSize];

        recursivelyGenerateDatasetsXmlFromThreddsCatalog(
            oLocalSourceUrl == null? 
                "http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml" :
                oLocalSourceUrl, 
            datasetNameRegex, true, "", tReloadEveryNMinutes, false, //justURLs
            results, summary, catalogXmlTimes, 
            datasetSuccessTimes, datasetFailureTimes, new HashSet());

        //write summary as an XML comment
        results.append("\n\n<!-- *********************** SUMMARY ******************************\n");
        String2.replaceAll(summary, "--", "- - "); //so no end-of-comment
        results.append(XML.encodeAsXML(summary.toString()));
        results.append(XML.encodeAsXML("\n" + 
            "catalog.xml Processing Times (including children)\n" +
            String2.getDistributionStatistics(catalogXmlTimes) + "\n" +
            "generateDatasetXml Success Times\n" +
            String2.getDistributionStatistics(datasetSuccessTimes) + "\n" +
            "generateDatasetXml Failure Times\n" +
            String2.getDistributionStatistics(datasetFailureTimes) + "\n"));
        results.append(
            "END OF SUMMARY -->\n\n");

        results.close();

        if (oLogFileName == null)
            EDStatic.returnLoggingToSystemOut(); 

        if (oLocalSourceUrl != null) {
            SSR.displayInBrowser(resultsFileName);
            SSR.displayInBrowser(logFileName);
            return;
        }

        try {
        String resultsAr[] = String2.readFromFile(resultsFileName);
        String expected = 
//there are several datasets in the resultsAr, but this is the one that changes least frequently (monthly)
"<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_7c78_3321_3466\" active=\"true\">\n" +
"    <sourceUrl>http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/mday</sourceUrl>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"cols\" type=\"int\">8640</att>\n" +
"        <att name=\"composite\">true</att>\n" +
"        <att name=\"contributor_name\">NASA GSFC (OBPG)</att>\n" +
"        <att name=\"contributor_role\">Source of level 2 data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n" +
"        <att name=\"cwhdf_version\">3.4</att>\n" +
"        <att name=\"date_created\">2012-10-12Z</att>\n" +  //changes
"        <att name=\"date_issued\">2012-10-12Z</att>\n" + //changes
"        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n" +
"        <att name=\"et_affine\" type=\"doubleList\">0.0 0.041676313961565174 0.04167148975575877 0.0 0.0 -90.0</att>\n" +
"        <att name=\"gctp_datum\" type=\"int\">12</att>\n" +
"        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n" +
"        <att name=\"gctp_sys\" type=\"int\">0</att>\n" +
"        <att name=\"gctp_zone\" type=\"int\">0</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"double\">0.041676313961565174</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"double\">0.04167148975575877</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">up</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">NASA GSFC (OBPG)\n";

/*"2010-01-08T00:51:12Z NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD</att>\n" +
"        <att name=\"id\">LMHchlaSmday_20091216120000</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">90.0</att>\n" +
"        <att name=\"origin\">NASA GSFC (G. Feldman)</att>\n" +
"        <att name=\"pass_date\" type=\"intList\">14549 14550 14551 14552 14553 14554 14555 14556 14557 14558 14559 14560 14561 14562 14563 14564 14565 14566 14567 14568 14569 14570 14571 14572 14573 14574 14575 14576 14577 14578</att>\n" +
"        <att name=\"polygon_latitude\" type=\"doubleList\">-90.0 90.0 90.0 -90.0 -90.0</att>\n" +
"        <att name=\"polygon_longitude\" type=\"doubleList\">0.0 0.0 360.0 360.0 0.0</att>\n" +
"        <att name=\"processing_level\">3</att>\n" +
"        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"projection\">geographic</att>\n" +
"        <att name=\"projection_type\">mapped</att>\n" +
"        <att name=\"references\">Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n" +
"        <att name=\"rows\" type=\"int\">4320</att>\n" +
"        <att name=\"satellite\">Aqua</att>\n" +
"        <att name=\"sensor\">MODIS</att>\n" +
"        <att name=\"source\">satellite observation: Aqua, MODIS</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">-90.0</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n" +
"        <att name=\"start_time\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n" +
"        <att name=\"summary\">NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#039;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.</att>\n" +
"        <att name=\"time_coverage_end\">2010-01-01T00:00:00Z</att>\n" +  //changes
"        <att name=\"time_coverage_start\">2009-12-01T00:00:00Z</att>\n" + 
"        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"authority\">gov.noaa.pfel.coastwatch</att>\n" +
"        <att name=\"creator_name\">NASA GSFC (G. Feldman)</att>\n" +
*/

//*** this is a different dataset!
String expected2 = 
"        <att name=\"infoUrl\">http://coastwatch.pfel.noaa.gov/infog/MH_chla_las.html</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">1-day,\n" +
"Oceans &gt; Ocean Chemistry &gt; Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0, CWHDF</att>\n" +
"        <att name=\"Oceanwatch_Live_Access_Server\">http://oceanwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"original_institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"publisher_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"publisher_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"publisher_url\">http://coastwatch.pfel.noaa.gov</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"summary\">NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#039;s Aqua Spacecraft. Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft. This is Science Quality data.</att>\n" +
"        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality (1-day)</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.330344E9 1.330344E9</att>\n" + //changes
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"long_name\">Centered Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">up</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"positive\">up</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">-90.0 90.0</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">4</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">4</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>MHchla</sourceName>\n" +
"        <destinationName>MHchla</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n";
/*
"            <att name=\"actual_range\" type=\"floatList\">0.01 63.92</att>\n" + //changes
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"long_name\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"numberOfObservations\" type=\"int\">15412342</att>\n" +  //changes
"            <att name=\"percentCoverage\" type=\"double\">0.4129249721364883</att>\n" +
"            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" +
"            <att name=\"units\">mg m-3</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Ocean Color</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n";
*/
        int po = resultsAr[1].indexOf(expected.substring(0, 80));
        if (po < 0) String2.log("results=\n" + resultsAr[1]);
        Test.ensureEqual(resultsAr[1].substring(po, po + expected.length()), expected, 
            "results=\n" + resultsAr[1]);

        po = resultsAr[1].indexOf(expected2.substring(0, 80));
        if (po < 0) String2.log("results=\n" + resultsAr[1]);
        Test.ensureEqual(resultsAr[1].substring(po, po + expected2.length()), expected2, 
            "results=\n" + resultsAr[1]);

        String2.log("\ntestGenerateDatasetsXmlFromThreddsCatalog passed the test.");

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml on " + EDStatic.erddapUrl + //in tests, always non-https url
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /** 
     * Bob uses this for testing in individual GEO IDE UAF Thredds catalog.
     * See original catalog http://ferret.pmel.noaa.gov/thredds/geoideCatalog.html
     * See the clean catalog at http://ferret.pmel.noaa.gov/geoide/geoIDECleanCatalog.html
     */
    public static void testUAFSubThreddsCatalog(int which) throws Throwable {
        String hrefs[] = {
//0
    "http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml", //erd
    "http://thredds1.pfeg.noaa.gov/thredds/catalog/catalog.xml",  //erd
    //2012-06-14: test of new clean catalog from catalog cleaner
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogV2beta.xml",
    "http://data1.gfdl.noaa.gov:8380/thredds/ipcc/all_ipcc.xml",
    "http://cwcgom.aoml.noaa.gov/thredds/catalog.xml",
//5
    "http://osmc.noaa.gov:8180/thredds/catalog.xml",
    "http://oceanwatch.pfeg.noaa.gov/thredds/catalog.xml", //does this cause problems? unable to generate thread for remote dataset...
    "http://opendap.co-ops.nos.noaa.gov/thredds/catalog.xml", //needs aggregation, needs better summary
    "http://www.ngdc.noaa.gov/thredds/catalog.xml", //needs aggregation, has fgdc_ metadata
    "http://data.nodc.noaa.gov/thredds/catalog.xml", //needs aggregation, has lat lon as data variables (not converted to latitude longitude)
//10
    "http://nomads.ncdc.noaa.gov/thredds/catalog.xml",
    "http://edac-dap.northerngulfinstitute.org/thredds/catalog/ncom/region1/catalog.xml", //has no summary or title, but has comment="..." that caused trouble
    "http://edac-dap.northerngulfinstitute.org/thredds/catalog/ncom/region5/catalog.xml", //file not found
    "http://edac-dap.northerngulfinstitute.org/thredds/catalog/ncom/region6/catalog.xml", //file not found
    "http://edac-dap.northerngulfinstitute.org/thredds/catalog/ncom/region7/catalog.xml", 
//15
    "http://edac-dap.northerngulfinstitute.org/thredds/catalog/ncom/region10/catalog.xml", //file not found
    "http://edac-dap2.northerngulfinstitute.org/thredds/catalog/rtofs3d/catalog.xml", //needs aggregation
    "http://edac-dap2.northerngulfinstitute.org/thredds/catalog/rtofs/catalog.xml",
    "http://coastwatch.noaa.gov/thredds/catalog.xml", //has cwhdfToNc files!
    "http://dods.ndbc.noaa.gov/thredds/catalog/data/catalog.xml", //ndbc needs aggregation
//20
    "http://www.aoos.org", //a website; better: opendap catalog: http://penguin.sfos.uaf.edu/opendap/
    //137. was '/opendap/', but links are all '/thredds/' and '/thredds/' works too.  
    //lots of .txt and .png files! no dap datasets?
    //says all_services but that's not true
    "http://137.229.40.88/thredds/catalog.xml", 
    "http://www.nanoos.org", //website;  I can't find a thredds
    "http://agate.coas.oregonstate.edu:8080/thredds/catalog.xml",
    //website; see #25 thredds
    //opendap is http://bmlsc.ucdavis.edu:8080/opendap/data/contents.html
    //erddap is http://bmlsc.ucdavis.edu:8080/erddap/
    "http://www.cencoos.org",  
//25
    "http://cencoos.org:8080/thredds/catalog.xml", //currently just test files
    "http://www.sccoos.org",  //website; couldn't find thredds
    "http://ourocean.jpl.nasa.gov:8080/thredds/catalog.xml", //needs aggregation
    "http://www.pacioos.org", //website; #29 is pacioos thredds
    "http://oos.soest.hawaii.edu/thredds/catalog.xml", //needs aggregation
//30
    "http://www.glos.us", //website; #31 is pacioos thredds
    "http://michigan.glin.net:8080/thredds/catalog.xml", //needs aggregation
    "http://www.neracoos.org", //website; see subsequent thredds from http://www.neracoos.org/data/thredds.html
    "http://rocky.umeoce.maine.edu:8080/thredds/catalog.xml", //needs aggregation; some aggregated: has time and runtime
    "http://coast-enviro.er.usgs.gov/thredds/catalog.xml",
//35
    "http://geoport.whoi.edu:8081/thredds/catalog.xml",
    "http://blackburn.whoi.edu:8080/thredds/catalog.xml", //no response
    "http://www.smast.umassd.edu:8080/thredds/catalog.xml",  //I think aggregated
    "http://www.jcoot.unh.edu/thredds/catalog.xml", //needs aggregation
    "http://www.macoora.org",  //website; couldn't find thredds
//40
    "http://colossus.dl.stevens-tech.edu:8080/thredds/catalog.xml", //needs aggregation
    "http://tashtego.marine.rutgers.edu:8080/thredds/catalog.xml", //aggregated by year
    "http://aqua.smast.umassd.edu:8080/thredds/catalog.xml",
    "http://www.secoora.org", //website; #44 is secoora thredds?
    "http://omglnx1.meas.ncsu.edu:8080/thredds/catalog.xml", //needs aggregation
//45
    "http://www.caricoos.org", //website; #46 is caricoos thredds
    "http://dm1.caricoos.org/thredds/catalog.xml", //needs aggregation
    "http://www.gcoos.org", //website; #48 is gcoos thredds
    "http://csanady.tamu.edu:8080/thredds/catalog.xml", 
    "http://ferret.pmel.noaa.gov/thredds/dodsC/data/PMEL/WOA05nc/catalog.xml",  //separate small test of institution

//50="clean catalog" http://ferret.pmel.noaa.gov/geoide/geoIDECleanCatalog.html 
    //(v1.0.1  as of 2010-02-18 and still on 2010-03-03, 
    //2010-04-07 still v1.0.1, but #58-61 added)
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalog.xml",

    //51.. PMEL the  subcatalogs      //noaa_pmel  645 datasets, all loaded
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/6F782D2B9E0DE28D6B06A1ADCDC7A363.xml", 
    //52: noaa_gfdl  IPCC data,  GenerateDatasetsXml is slow, 3 unable to get .das
    //getting data slow, graph takes several minutes;  //59+2etopo datasets, all loaded //lots of grids that use different dimensions   
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/6647A86EF2F13CA51E4DC35F99B75003.xml", 
    //53: noaa_aoml GenerateDatasetsXml all ok;  8+2etopo datasets, all loaded
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/CE93007F7DA55F6B44C9D9BE68CBE2BC.xml", 
    //54: noaa_osmc OCO GenerateDatasetsXml all ok, 6+2etopo datasets, all loaded
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/00A30DB93CB98DEDA3CA8BC79E39A4DA.xml", 
//55
    //55: PFEG/PFEL! 293+2etopo datasets, GenerateDatasetsXml all ok;  293+2 datasets, all loaded
    // !!! *** If copying datasets to datasetsUAF.xml, 
    //         you *MUST* change http://thredds1 references to numeric IP !!!
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/BE6AA0E0E6206204D859DD539710B756.xml", 
    //56: NGDC GenerateDatasetsXml had one error (couldn't get .das),  8+2etopo datasets, 1 failed (see below)
    //  has "x 1000" and "* 10" variables and string scale_factor
    //  5505_1a5c_cdd8 can't load because variable ... not found, but var changes if I retry
    //     http://www.ngdc.noaa.gov/thredds/dodsC/stec-aggregation
    //     I think thredds is generating the dataset.
    // 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/4BE9976011C103BC2F14E2BFA2B12176.xml", 
    //57: ecowatch 2012-10-17 fails
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/2CF2728013B3968CD6B635FAC7631CB7.xml",
    //58: ocean NOMADS   2012-10-17 fails
    //  2010-04-26  7 datasets
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/ncom_best_time_series.xml",
    //59: 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/C9649B46B7DCFDA43C6F59478475A353.xml",
    //60: 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/fukushima_best_time_series.xml",    
    //61: 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/nws_updated.xml",
    //62: carbontracker 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/carbontracker.xml",
    //63: 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/ncom_best_time_series.xml",
    //64: 2012-10-17 ok
    "http://ferret.pmel.noaa.gov/geoide/catalog/ncep.reanalysis.dailyavgs.xml",
    //skip national coastwatch
    //"http://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalogs/noaa_coastwatch_aggregations.xml",
    };

        generateDatasetsXmlFromThreddsCatalog(null, 
            hrefs[which], ".*", 10080);  //10080=7days (so many datasets!)   was 2880 = 2 days
    }

    /**
     * This gets the file names from Hyrax catalog directory URL.
     *
     * @param startUrl the url of the current web page (with a hyrax catalog) e.g.,
            "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/contents.html"
     * @param fileNameRegex e.g.,
            "pentad.*flk\\.nc\\.gz"
     * @param recursive
     * @returns a String[] with a list of full URLs of the children (may be new String[0]) 
     * @throws Throwable if trouble
     */
    public static String[] getUrlsFromHyraxCatalog(String startUrl, String fileNameRegex, 
        boolean recursive) throws Throwable {
        if (verbose) String2.log("getUrlsFromHyraxCatalog regex=" + fileNameRegex);

        //call the recursive method
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        addToHyraxUrlList(startUrl, fileNameRegex, recursive, childUrls, lastModified);

        return childUrls.toArray();
    }

    /**
     * This does the work for getHyraxUrls.
     * This calls itself recursively, adding into to fileNameInfo as it is found.
     *
     * @param url the url of the current web page (with a hyrax catalog) e.g.,
            "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/contents.html"
     * @param fileNameRegex e.g.,
            "pentad.*flk\\.nc\\.gz"
     * @param childUrls  new children will be added to this
     * @param lastModified the lastModified time (secondsSinceEpoch, NaN if not available)
     * @return true if completely successful (no access errors, all URLs found)
     * @throws Throwable if trouble, e.g., if url doesn't respond
     */
    public static boolean addToHyraxUrlList(String url, String fileNameRegex, boolean recursive,
        StringArray childUrls, DoubleArray lastModified) throws Throwable {

        if (reallyVerbose) String2.log("\ngetHyraxUrlInfo childUrls.size=" + childUrls.size() + 
            "\n  url=" + url); 
        boolean completelySuccessful = true;  //but any child can set it to false
        String response;
        try {
            response = SSR.getUrlResponseString(url);
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return false;
        }
        String responseLC = response.toLowerCase();
        String urlDir = File2.getDirectory(url);

        //skip header line and parent directory
        int po = responseLC.indexOf("parent directory");  //Lower Case
        if (po < 0 ) {
            if (reallyVerbose) String2.log("ERROR: \"parent directory\" not found in Hyrax response.");
            return false;
        }
        po += 18;

        //endPre
        int endPre = responseLC.indexOf("</pre>", po); //Lower Case
        if (endPre < 0) 
            endPre = response.length();

        //go through file,dir listings
        boolean diagnosticMode = false;
        while (true) {

            //EXAMPLE http://data.nodc.noaa.gov/opendap/wod/monthly/  No longer available

            //EXAMPLE http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/M07
            //(reformatted: look for tags, not formatting
            /*   <tr>
                   <td align="left"><b><a href="month_19870701_v11l35flk.nc.gz.html">month_19870701_v11l35flk.nc.gz</a></b></td>
                   <td align="center" nowrap="nowrap">2007-04-04T07:00:00</td>
                   <td align="right">4807310</td>
                   <td align="center">
                      <table>
                      <tr>
                        <td><a href="month_19870701_v11l35flk.nc.gz.ddx">ddx</a>&nbsp;</td>
                        <td><a href="month_19870701_v11l35flk.nc.gz.dds">dds</a>&nbsp;</td>
                      </table>  //will exist if <table> exists
                   </td>
                   <td align="center"><a href="/opendap/webstart/viewers?dapService=/opendap/hyrax&amp;datasetID=/allData/ccmp/L3.5a/monthly/flk/1987/M07/month_19870701_v11l35flk.nc.gz">viewers</a></td>
                 </tr>  //may or may not exist
                 <tr>   //may or may not exist
                   //the next row...
               </table>
            */ 

            //find beginRow and nextRow
            int beginRow = responseLC.indexOf("<tr", po);      //Lower Case
            if (beginRow < 0 || beginRow > endPre)
                return completelySuccessful;
            int endRow = responseLC.indexOf("<tr", beginRow + 3);      //Lower Case
            if (endRow < 0 || endRow > endPre)
                endRow = endPre;

            //if <table> in the middle, skip table 
            int tablePo = responseLC.indexOf("<table", beginRow + 3);
            if (tablePo > 0 && tablePo < endRow) {
                int endTablePo = responseLC.indexOf("</table", tablePo + 6);
                if (endTablePo < 0 || endTablePo > endPre)
                    endTablePo = endPre;

                //find <tr after </table>
                endRow = responseLC.indexOf("<tr", endTablePo + 7);      //Lower Case
                if (endRow < 0 || endRow > endPre)
                    endRow = endPre;
            }
            String thisRow   = response.substring(beginRow, endRow);
            String thisRowLC = responseLC.substring(beginRow, endRow);
            if (diagnosticMode) 
                String2.log("<<<beginRow=" + beginRow + " endRow=" + endRow + "\n" + 
                    thisRow + "\n>>>");

            //look for .das   href="wod_013459339O.nc.das">das<     
            int dasPo = thisRowLC.indexOf(".das\">das<");
            if (diagnosticMode) 
                String2.log("    .das " + (dasPo < 0? "not " : "") + "found");
            if (dasPo > 0) {
                int quotePo = thisRow.lastIndexOf('"', dasPo);
                if (quotePo < 0) {
                    String2.log("ERROR: invalid .das reference:\n  " + thisRow);
                    po = endRow;
                    continue;
                }
                String fileName = thisRow.substring(quotePo + 1, dasPo);
                if (diagnosticMode) 
                    String2.log("    filename=" + fileName + 
                        (fileName.matches(fileNameRegex)? " does" : " doesn't") + 
                        " match " + fileNameRegex);
                if (fileName.matches(fileNameRegex)) {

                    //get lastModified time   >2011-06-30T04:43:09<
                    String stime = String2.extractRegex(thisRow,
                        ">\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}<", 0);
                    double dtime = Calendar2.safeIsoStringToEpochSeconds(
                        stime == null? "" : stime.substring(1, stime.length() - 1));

                    childUrls.add(urlDir + fileName);
                    lastModified.add(dtime);
                    //String2.log("  file=" + fileName + "   " + stime);
                    po = endRow;
                    continue;
                }
            } 

            if (recursive) {
                //look for   href="199703-199705/contents.html"     
                int conPo = thisRowLC.indexOf("/contents.html\"");
                if (conPo > 0) {
                    int quotePo = thisRow.lastIndexOf('"', conPo);
                    if (quotePo < 0) {
                        String2.log("ERROR: invalid contents.html reference:\n  " + thisRow);
                        po = endRow;
                        continue;
                    }
                    boolean tSuccessful = addToHyraxUrlList(
                        urlDir + thisRow.substring(quotePo + 1, conPo + 14),
                        fileNameRegex, recursive, childUrls, lastModified);
                    if (!tSuccessful)
                        completelySuccessful = false;
                    po = endRow;
                    continue;
                }
            }
            po = endRow;
        }
    }

    /**
     */
    public static void testGetUrlsFromHyraxCatalog() throws Throwable {
        String2.log("\n*** EDDGridAggregateExistingDimension.testGetUrlsFromHyraxCatalog()\n");

        try {


        String results[] = getUrlsFromHyraxCatalog(
            //before 2011-05-18, was 
            //"http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/contents.html", 
            "http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html",
            "month.*flk\\.nc\\.gz", true);
        String expected[] = new String[]{
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz",
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz"}; 
        Test.ensureEqual(results, expected, "results=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue...");
        }


    }

    /** 
     * This is for use by Bob at ERD -- others don't need it.
     * This generates a rough draft of the datasets.xml entry for an EDDGridFromDap
     * for the datasets served by ERD's Thredds server.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param search1 e.g., Satellite/aggregsat
     * @param search2 e.g., satellite
     * @throws Throwable if trouble
     */
    public static String generateErdThreddsDatasetXml(String search1, String search2) 
        throws Throwable {

        String2.log("EDDGridFromDap.generateErdThreddsDatasetXml");

        //read DataSet.properties
        ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");

        //read the main catalog
        String baseUrl = "http://oceanwatch.pfeg.noaa.gov/thredds/";
        String mainCat = SSR.getUrlResponseString(baseUrl + "catalog.html");
        int mainCatPo = 0;
        int mainCount = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(directionsForGenerateDatasetsXml());

        while (true) {
            //search for and extract from... 
            //<a href="Satellite/aggregsatMH/chla/catalog.html"><tt>Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality/</tt></a></td>
            String mainSearch = "<a href=\"" + search1;
            mainCatPo = mainCat.indexOf(mainSearch, mainCatPo + 1);
            if (mainCatPo < 0) // || mainCount++ >= 2) 
                break;

            int q1 = mainCatPo + 8;
            int q2 = mainCat.indexOf('"', q1 + 1);
            int tt1 = mainCat.indexOf("<tt>", mainCatPo);
            int tt2 = mainCat.indexOf("/</tt>", mainCatPo);
            String subCatUrl = mainCat.substring(q1 + 1, q2);
            String twoLetter = mainCat.substring(mainCatPo + mainSearch.length(), mainCatPo + mainSearch.length() + 2);
            String fourLetter = mainCat.substring(mainCatPo + mainSearch.length() + 3, mainCatPo + mainSearch.length() + 7);
            String title = mainCat.substring(tt1 + 4, tt2);
            String longName = EDV.suggestLongName("", "",
                dataSetRB2.getString(twoLetter + fourLetter + "StandardName", ""));
            String2.log(twoLetter + fourLetter + " = " + title);
       
            //read the sub catalog
            String subCat = SSR.getUrlResponseString(baseUrl + subCatUrl);
            int subCatPo = 0;

            while (true) {
                //search for and extract from... 
                //?dataset=satellite/MH/chla/5day">
                String subSearch = "?dataset=" + search2 + "/" + twoLetter + "/" + fourLetter + "/";
                subCatPo = subCat.indexOf(subSearch, subCatPo + 1);
                if (subCatPo < 0) 
                    break;
                int sq = subCat.indexOf('"', subCatPo);
                Test.ensureTrue(sq >= 0, "subSearch close quote not found.");
                String timePeriod = subCat.substring(subCatPo + subSearch.length(), sq);
                String reload = title.indexOf("Science Quality") >= 0? "10080" : //weekly
                    timePeriod.equals("hday")? "60" : "360"; //hourly or 6hourly
                int tpLength = timePeriod.length();
                String niceTimePeriod = 
                    timePeriod.equals("hday")? "Hourly" :
                    timePeriod.equals("mday")? "Monthly Composite" :
                        timePeriod.substring(0, tpLength - 3) + " Day Composite";
                String2.log("  " + timePeriod + " => " + niceTimePeriod);

                sb.append(
"    <dataset type=\"EDDGridFromDap\" datasetID=\"erd" + twoLetter + fourLetter + timePeriod + "\">\n" +
"        <!-- outside of ERD, use http://oceanwatch.pfeg.noaa.gov -->\n" +
"        <sourceUrl>http://192.168.31.18/thredds/dodsC/satellite/" + twoLetter + 
                "/" + fourLetter + "/" + timePeriod + "</sourceUrl>\n" +
"        <reloadEveryNMinutes>" + reload + "</reloadEveryNMinutes>\n" +
"        <addAttributes> \n" +
"            <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov/infog/" + 
                twoLetter + "_" + fourLetter + "_las.html</att>\n" +
"            <att name=\"title\">" + title + " (" + niceTimePeriod + ")</att>\n" +
"            <att name=\"cwhdf_version\" />\n" +
"            <att name=\"cols\" />  \n" +
"            <att name=\"et_affine\" />\n" +
"            <att name=\"gctp_datum\" />\n" +
"            <att name=\"gctp_parm\" />\n" +
"            <att name=\"gctp_sys\" />\n" +
"            <att name=\"gctp_zone\" />\n" +
"            <att name=\"id\" />\n" +
"            <att name=\"pass_date\" />\n" +
"            <att name=\"polygon_latitude\" />\n" +
"            <att name=\"polygon_longitude\" />\n" +
"            <att name=\"rows\" />\n" +
"            <att name=\"start_time\" />\n" +
"            <att name=\"time_coverage_end\" />  \n" +
"            <att name=\"time_coverage_start\" />\n" +
"        </addAttributes>\n" +
"        <longitudeSourceName>lon</longitudeSourceName>\n" +
"        <latitudeSourceName>lat</latitudeSourceName>\n" +
"        <altitudeSourceName>altitude</altitudeSourceName>\n" +
"        <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n" +
"        <timeSourceName>time</timeSourceName>\n" +
"        <timeSourceFormat></timeSourceFormat> \n" +
"        <dataVariable>\n" +
"            <sourceName>" + twoLetter + fourLetter + "</sourceName>\n" +
"            <destinationName>???" + fourLetter + "</destinationName>\n" +
"            <addAttributes> \n" +
"                <att name=\"ioos_category\">???Temperature</att>\n" +
                    (longName.length() > 0? 
"                <att name=\"long_name\">" + longName + "</att>\n" : "") +
"                <att name=\"actual_range\" /> \n" +
"                <att name=\"numberOfObservations\" /> \n" +
"                <att name=\"percentCoverage\" />\n" +
"            </addAttributes>\n" +
"        </dataVariable>\n" +
"    </dataset>\n" +
"\n");
            }
        }
        return sb.toString();
    }



    public static void testForCarleton() throws Throwable {
        //test for Charles Carleton   .nc request failed; others ok
        testVerboseOn();
        EDDGrid gridDataset = (EDDGridFromDap)oneFromDatasetXml("NCOM_Region7_2D"); //should work
        String tName = gridDataset.makeNewFileForDapQuery(null, null,
            "surf_el[(2008-06-12T00:00:00):1:(2008-06-12T00:00:00)][(10.0):100:(65.0)][(-150.0):100:(-100.0)]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Carleton", ".nc"); 
        String results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        String expected = 
"time =\n" +
"  {1.2132288E9}\n" +
"latitude =\n" +
"  {10.0, 22.5, 35.0, 47.5, 60.0}\n" +
"longitude =\n" +
"  {-150.0, -137.5, -125.0, -112.5, -100.0}\n" +
"surf_el =\n" +
"  {\n" +
"    {\n" +
"      {0.314, 0.203, 0.505, 0.495, 0.317},\n" +
"      {0.646, 0.6, 0.621, 0.547, -30.0},\n" +
"      {0.326, 0.487, 0.589, -30.0, -30.0},\n" +
"      {-0.34400000000000003, -0.044, 0.318, -30.0, -30.0},\n" +
"      {-30.0, -30.0, -30.0, -30.0, -30.0}\n" +
"    }\n" +
"  }\n" +
"}\n";
        Test.ensureTrue(results.endsWith(expected), "RESULTS=\n" + results);
    }


    public static void testForDave() throws Throwable {
        testVerboseOn();

        //tests for Dave    works, but datasets not always active
        //the cwAM datasets are gone; see active noaa_coastwatch_... datasets
        String tid = "cwAMchlaD1";  
        //String tid = "cwAMchlaD61";
        //String tid = "cwAMchlaG";
        //String tid = "cwAMchlaAnG";
        EDDGrid gridDataset = (EDDGridFromDap)oneFromDatasetXml(tid);

        String2.log("\n\n***** DDS");
        String tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_" + tid, ".dds"); 
        String2.log(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));

        String2.log("\n\n***** DAS ");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_" + tid, ".das"); 
        String2.log(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));

        String2.log("\n\n***** NCDUMP ");
        tName = gridDataset.makeNewFileForDapQuery(null, null, 
            "chlor_a[(2008-03-29T12:00:00):1:(2008-03-29T12:00:00)][(0.0):1:(0.0)][(16.995124378128825):100:(31.00905181853181)][(-99.01235553141787):100:(-78.99636386525192)]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_" + tid, ".nc"); 
        String2.log(NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true));        

        String2.log("\n\n***** PNG ");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "chlor_a[0][][][]&.colorBar=Rainbow|C|Log|.04|10|", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_" + tid + "_Map", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    }


    public static void testBasic1() throws Throwable {
        String2.log("\n*** EDDGridFromDap.testBasic1\n");
        testVerboseOn();
        EDDGridFromDap gridDataset;
        String name, tName, axisDapQuery, results, expected, error;
        int tPo;
        String userDapQuery  = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
        String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]"; 
        String mapDapQuery   = "chlorophyll[200][][(29):(50)][(225):(247)]"; //stride irrelevant 
        StringArray destinationNames = new StringArray();
        IntArray constraints = new IntArray();

        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdMHchla8day"); //should work


/* */   //just comment out to work on some other test

        //test regex for integer >=0 
        Test.ensureTrue("0".matches("[0-9]+"), "");
        Test.ensureTrue("123".matches("[0-9]+"), "");
        Test.ensureTrue(!"-1".matches("[0-9]+"), "");
        Test.ensureTrue(!"2.3".matches("[0-9]+"), "");

        //test that bad metadata was removed
        //String2.log(
        //    "\n\naddAtt=" + gridDataset.addGlobalAttributes() +
        //    "\n\ncombinedAtt=" + gridDataset.combinedGlobalAttributes());
        Test.ensureEqual(gridDataset.combinedGlobalAttributes().getString("et_affine"), null, "");
        Test.ensureEqual(gridDataset.dataVariables()[0].combinedAttributes().getString("percentCoverage"), null, "");

        //test getUserQueryParts      decoded
        Test.ensureEqual(getUserQueryParts(null), new String[]{""}, "");
        Test.ensureEqual(getUserQueryParts(""), new String[]{""}, "");
        Test.ensureEqual(getUserQueryParts("  "), new String[]{"  "}, "");
        Test.ensureEqual(getUserQueryParts("ab%3dc"), new String[]{"ab=c"}, "");  
        Test.ensureEqual(getUserQueryParts("a&b&c"), new String[]{"a", "b", "c"}, "");
        Test.ensureEqual(getUserQueryParts("&&"), new String[]{"", "", ""}, "");
        Test.ensureEqual(getUserQueryParts("a&b=R%26D"), new String[]{"a", "b=R&D"}, "");  //& visible
        Test.ensureEqual(getUserQueryParts("a%26b=\"R%26D\""), new String[]{"a", "b=\"R&D\""}, ""); //& encoded
        Test.ensureEqual(getUserQueryParts("a%26b=\"R%26D\"%26c"), new String[]{"a", "b=\"R&D\"", "c"}, ""); //& encoded
        Test.ensureEqual(getUserQueryParts("a%26b%3dR-D"), new String[]{"a", "b=R-D"}, ""); 

        //*** test getUserQueryParts (decoded) with invalid queries
        error = "";
        try {
            getUserQueryParts("a%26b=\"R%26D");  //decoded.  unclosed "
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Query error: A closing doublequote is missing.", 
            "error=" + error);


        //*** test parseQuery with invalid queries
        error = "";
        try {
            gridDataset.parseDataDapQuery("zztop", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Error: destinationVariableName=zztop wasn't found.", 
            "error=" + error);

        error = "";
        try {
            gridDataset.parseDataDapQuery("chlorophyll[][][]", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Query error: For variable=chlorophyll axis#3=longitude: \"[\" was expected at position=17.", 
            "error=" + error);

        error = "";
        try {
            gridDataset.parseDataDapQuery("chlorophyll[(2007-02-06)[][(29):10:(50)][(225):10:(247)]", 
                destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],  //last # changes frequently
            "SimpleException: Query error: For variable=chlorophyll axis#0=time Constraint=\"[(2007-02-06)[]\": Stop=\"\" is invalid.  It must be an integer between 0 and 465.", 
            "error=" + error);

        error = "";
        try {
            gridDataset.parseAxisDapQuery("zztop", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Error: variableName=zztop wasn't found.", 
            "error=" + error);

        error = "";
        try {
            gridDataset.parseAxisDapQuery("latitude,chlorophyll", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Query error: A griddap axis variable query can't include a data variable (chlorophyll).", 
            "error=" + error);

        error = "";
        try {
            gridDataset.parseDataDapQuery("chlorophyll,latitude", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Query error: A griddap data variable query can't include an axis variable (latitude).", 
            "error=" + error);

        error = "";
        try {
            gridDataset.parseAxisDapQuery("latitude[", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0],
            "SimpleException: Query error: For variable=latitude axis#2=latitude: \"]\" was not found after position=8.", 
            "error=" + error);

        //test error message from dataset that doesn't load
        //EDDGrid tGrid = (EDDGrid)oneFromDatasetXml("erdAGtanm3day"); //should fail
        //String2.log("tGrid==null = " + (tGrid == null));
        //if (true) System.exit(0);

        //*** test valid parseDataDapQuery
        String iso = Calendar2.epochSecondsToIsoStringT(1.0260864E9);
        Test.ensureTrue(!gridDataset.isAxisDapQuery(userDapQuery), "");
        gridDataset.parseDataDapQuery(userDapQuery, destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "chlorophyll", "");
        //pre 2009-09-09 was different. Based on other changes: Dave must have regridded the dataset
        Test.ensureEqual(constraints.toString(), "206, 1, 206, 0, 1, 0, 2855, 10, 3359, 5399, 10, 5927", "");

        String tDapQuery  = "chlorophyll[(2007-02-06)][][(29):10:(50)][last:1:last]"; //test last
        gridDataset.parseDataDapQuery(tDapQuery, destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "chlorophyll", "");
        Test.ensureEqual(constraints.toString(), "206, 1, 206, 0, 1, 0, 2855, 10, 3359, 8639, 1, 8639", "");

        tDapQuery  = "chlorophyll[(2007-02-06T12:00:00)][][(29):10:(50)][last]"; //test colons
        gridDataset.parseDataDapQuery(tDapQuery, destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "chlorophyll", "");
        Test.ensureEqual(constraints.toString(), "206, 1, 206, 0, 1, 0, 2855, 10, 3359, 8639, 1, 8639", "");

        Test.ensureTrue(gridDataset.isAxisDapQuery("time"), "");
        gridDataset.parseAxisDapQuery("time", destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "time", "");        
        //Test.ensureEqual(constraints.toString(), "0, 1, 331", ""); //this will increase once in a while

        Test.ensureTrue(gridDataset.isAxisDapQuery("time["), "");
        gridDataset.parseAxisDapQuery("time[(2007-02-06)]", destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "time", "");
        Test.ensureEqual(constraints.toString(), "206, 1, 206", "");

        gridDataset.parseAxisDapQuery("longitude[ last : 1 : last ]", destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "longitude", "");
        Test.ensureEqual(constraints.toString(), "8639, 1, 8639", "");

        gridDataset.parseAxisDapQuery("longitude[ last ]", destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toString(), "longitude", "");
        Test.ensureEqual(constraints.toString(), "8639, 1, 8639", "");

        gridDataset.parseAxisDapQuery("longitude[ last - 20]", destinationNames, constraints, false);
        Test.ensureEqual(constraints.toString(), "8619, 1, 8619", "");

        gridDataset.parseAxisDapQuery("longitude[last+-20]", destinationNames, constraints, false);
        Test.ensureEqual(constraints.toString(), "8619, 1, 8619", "");

        gridDataset.parseAxisDapQuery("time[20:10:(2007-02-06)],altitude,longitude[last]", destinationNames, constraints, false);
        Test.ensureEqual(destinationNames.toNewlineString(), "time\naltitude\nlongitude\n", "");
        Test.ensureEqual(constraints.toString(), "20, 10, 206, 0, 1, 0, 8639, 1, 8639", "");

        //lon: incr=0.04166667   n=8640
        gridDataset.parseAxisDapQuery("longitude[(last-0.4166)]", destinationNames, constraints, false);
        Test.ensureEqual(constraints.toString(), "8629, 1, 8629", "");

        gridDataset.parseAxisDapQuery("longitude[(last+-0.4166)]", destinationNames, constraints, false);
        Test.ensureEqual(constraints.toString(), "8629, 1, 8629", "");

        //time: incr=8days  n=272    16days=16*86400=1382400
        gridDataset.parseAxisDapQuery("time[(last-1382400)]", destinationNames, constraints, false);
        //Test.ensureEqual(constraints.toString(), "329, 1, 329", ""); //changes sometimes

        gridDataset.parseAxisDapQuery("time[(last+-1382400)]", destinationNames, constraints, false);
        //Test.ensureEqual(constraints.toString(), "329, 1, 329", ""); //changes sometimes

        error = "";
        try {
            gridDataset.parseAxisDapQuery("latitude[last-2.0]", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureTrue(error.indexOf("SimpleException: Query error: The +/- index value in Start=last-2.0 isn't an integer.") >= 0, "error=" + error);

        error = "";
        try {
            gridDataset.parseAxisDapQuery("latitude[(last-2.0a)]", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureTrue(error.indexOf("SimpleException: Query error: The +/- value in Start=(last-2.0a) isn't valid.") >= 0, "error=" + error);

        error = "";
        try {
            gridDataset.parseAxisDapQuery("latitude[(last*2)]", destinationNames, constraints, false);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureTrue(error.indexOf("SimpleException: Query error: Unexpected character after \"last\" in Start=(last*2).") >= 0, "error=" + error);

        //***test some edvga things
        EDVGridAxis edvga = gridDataset.axisVariables()[0];
        Test.ensureEqual(edvga.isEvenlySpaced(), false, "");
        //Test.ensureEqual(edvga.averageSpacing(), 700858.0060422961, "");  //changes sometimes
        //Test.ensureEqual(edvga.spacingDescription(), "8 days 02:46:30 (uneven)", ""); //changes sometimes

        edvga = gridDataset.axisVariables()[1];
        Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
        Test.ensureEqual(edvga.averageSpacing(), Double.NaN, "");
        Test.ensureEqual(edvga.spacingDescription(), "(just one value)", "");

        edvga = gridDataset.axisVariables()[2];
        Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
        Test.ensureEqual(edvga.averageSpacing(), 0.041676313961565174, "");  //not ideal, but true
        Test.ensureEqual(edvga.spacingDescription(), "0.04167631 (even)", "");



        //*** test dapInstructions
        //StringWriter sw = new StringWriter();
        //writeGeneralDapHtmlDocument(EDStatic.erddapUrl, sw);  //for testing, always non-https url
        //results = sw.toString();
        //String2.log(results);
        //expected = "Requests for Gridded Data in ";
        //Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        //expected = "In ERDDAP, time variables always have the name \"" + EDV.TIME_NAME + "\"";
        //Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);


        //*** test getting das for entire dataset
        String2.log("\n****************** EDDGridFromDap test entire dataset\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
//"Attributes {\n" +
//"  time {\n" +
//"    Float64 actual_range 1.1886912e+9, 1.1886912e+9;\n"; //this will change sometimes
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);


        expected =   //test that _FillValue and missing_value are as in sourceAtts
        //but complicated, because that's the value my Grid class uses.
"  chlorophyll {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Concentration Of Chlorophyll In Sea Water\";\n" +
"    Float32 missing_value -9999999.0;\n" +    
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m-3\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);


        //*** test getting dds for entire dataset
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 466];\n" +   //466 will change sometimes
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 4320];\n" +
"  Float64 longitude[longitude = 8640];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[time = 466][altitude = 1][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 466];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 4320];\n" +
"      Float64 longitude[longitude = 8640];\n" +
"  } chlorophyll;\n" +
"} erdMHchla8day;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** test DAP data access form
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Entire", ".html"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        //*** test getting das for 1 variable     das isn't affected by userDapQuery
        String2.log("\n****************** EDDGridFromDap test 1 variable\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "chlorophyll", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_1Variable", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected =            
//"Attributes {\n" +
//"  time {\n" +
//"    Float64 actual_range 1.17072e+9, 1.17072e+9;\n" + //changes sometimes
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);


        //*** test getting dds for 1 variable
        tName = gridDataset.makeNewFileForDapQuery(null, null, "chlorophyll", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_1Variable", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +   //466 will change sometimes
"      Float32 chlorophyll[time = 466][altitude = 1][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 466];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 4320];\n" +
"      Float64 longitude[longitude = 8640];\n" +
"  } chlorophyll;\n" +
"} erdMHchla8day;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


    }


    public static void testBasic2() throws Throwable {
        String2.log("\n*** EDDGridFromDap.testBasic2\n");
        testVerboseOn();
        EDDGridFromDap gridDataset;
        String name, tName, axisDapQuery, results, expected, error;
        int tPo;
        String userDapQuery  = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
        String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]"; 
        String mapDapQuery   = "chlorophyll[200][][(29):(50)][(225):(247)]"; //stride irrelevant 
        StringArray destinationNames = new StringArray();
        IntArray constraints = new IntArray();

        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdMHchla8day"); //should work
        //just comment out to work on some other test


        //********************************************** test getting axis data

        //.asc
        String2.log("\n*** EDDGridFromDap test get .ASC axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Axis", ".asc"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 3];\n" +
"  Float64 longitude[longitude = 1];\n" +
"} erdMHchla8day;\n" +
"---------------------------------------------\n" +
"Data:\n" +
"time[3]\n" +
"1.0260864E9, 1.0960704E9, 1.1661408E9\n" +
"longitude[1]\n" +
"360.0\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);
 
        //.csv
        String2.log("\n*** EDDGridFromDap test get .csv axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Axis", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time,longitude\n" +
"UTC,degrees_east\n" +
"2002-07-08T00:00:00Z,360.0\n" +
"2004-09-25T00:00:00Z,NaN\n" +
"2006-12-15T00:00:00Z,NaN\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.csvp
        String2.log("\n*** EDDGridFromDap test get .csvp axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Axis", ".csvp"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time (UTC),longitude (degrees_east)\n" +
"2002-07-08T00:00:00Z,360.0\n" +
"2004-09-25T00:00:00Z,NaN\n" +
"2006-12-15T00:00:00Z,NaN\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.csv  test of gridName.axisName notation
        String2.log("\n*** EDDGridFromDap test get .CSV axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "chlorophyll.time[0:100:200],chlorophyll.longitude[last]", 
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_AxisG.A", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time,longitude\n" +
"UTC,degrees_east\n" +
"2002-07-08T00:00:00Z,360.0\n" +
"2004-09-25T00:00:00Z,NaN\n" +
"2006-12-15T00:00:00Z,NaN\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.das     which disregards userDapQuery
        String2.log("\n*** EDDGridFromDap test get .DAS axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Axis", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        expected =  //see OpendapHelper.EOL definition for comments
//"Attributes {[10]\n" + 
//"  time {[10]\n" +
//"    Float64 actual_range 1.17072e+9, 1.17072e+9;[10]\n" + //this will change sometimes
"    String axis \"T\";[10]\n" +
"    Int32 fraction_digits 0;[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Centered Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n" +
"  altitude {[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  NC_GLOBAL {[10]\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";[10]\n" +
"    String cdm_data_type \"Grid\";[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

        //.dds
        String2.log("\n*** EDDGridFromDap test get .DDS axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Axis", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Float64 time[time = 3];[10]\n" +
"  Float64 longitude[longitude = 1];[10]\n" +
"} erdMHchla8day;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.dods
        String2.log("\n*** EDDGridFromDap test get .DODS axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_Axis", ".dods"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Float64 time[time = 3];[10]\n" +
"  Float64 longitude[longitude = 1];[10]\n" +
"} erdMHchla8day;[10]\n" +
"[10]\n" +
"Data:[10]\n" +
"[0][0][0][3][0][0][0][3]A[206][8221]k[0][0][0][0]A[208]U-@[0][0][0]A[209]`y`[0][0][0][0][0][0][1][0][0][0][1]@v[8364][0][0][0][0][0][end]";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.json
        String2.log("\n*** EDDGridFromDap test get .JSON axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"longitude\"],\n" +
"    \"columnTypes\": [\"String\", \"double\"],\n" +
"    \"columnUnits\": [\"UTC\", \"degrees_east\"],\n" +
"    \"rows\": [\n" +
"      [\"2002-07-08T00:00:00Z\", 360.0],\n" +
"      [\"2004-09-25T00:00:00Z\", null],\n" +
"      [\"2006-12-15T00:00:00Z\", null]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //.json with jsonp
        String2.log("\n*** EDDGridFromDap test get .JSON axis data (with jsonp)\n");
        String jsonp = "Some encoded {}\n() ! text";
        tName = gridDataset.makeNewFileForDapQuery(null, null, 
            "time[0:100:200],longitude[last]" + "&.jsonp=" + SSR.percentEncode(jsonp), 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = jsonp + "(" +
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"longitude\"],\n" +
"    \"columnTypes\": [\"String\", \"double\"],\n" +
"    \"columnUnits\": [\"UTC\", \"degrees_east\"],\n" +
"    \"rows\": [\n" +
"      [\"2002-07-08T00:00:00Z\", 360.0],\n" +
"      [\"2004-09-25T00:00:00Z\", null],\n" +
"      [\"2006-12-15T00:00:00Z\", null]\n" +
"    ]\n" +
"  }\n" +
"}\n" +
")";
        Test.ensureEqual(results, expected, "results=\n" + results);
/* */
        //.mat
        //octave> load('c:/temp/griddap/EDDGridFromDap_Axis.mat');
        //octave> erdMHchla8day
        String matlabAxisQuery = "time[0:100:200],longitude[last]"; 
        String2.log("\n*** EDDGridFromDap test get .MAT axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, matlabAxisQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".mat"); 
        String2.log(".mat test file is " + EDStatic.fullTestCacheDirectory + tName);
        results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
        String2.log(results);
        expected = 
"4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
"69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
"20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
"6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
//"2c 20 43 72 65 61 74 65   64 20 6f 6e 3a 20 54 75   , Created on: Tu |\n" +
//"65 20 4f 63 74 20 31 34   20 30 38 3a 35 36 3a 35   e Oct 14 08:56:5 |\n" +
//"34 20 32 30 30 38 20 20   20 20 20 20 20 20 20 20   4 2008           |\n" +
"20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
"00 00 00 0e 00 00 01 18   00 00 00 06 00 00 00 08                    |\n" +
"00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0d                    |\n" +
"65 72 64 4d 48 63 68 6c   61 38 64 61 79 00 00 00   erdMHchla8day    |\n" +
"00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 40                  @ |\n" +
"74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 18   41 ce 94 6b 00 00 00 00           A  k     |\n" +
"41 d0 55 2d 40 00 00 00   41 d1 60 79 60 00 00 00   A U-@   A `y`    |\n" +
"00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 08   40 76 80 00 00 00 00 00           @v       |\n";
        Test.ensureEqual(
            results.substring(0, 71 * 4) + results.substring(71 * 7), //remove the creation dateTime
            expected, "RESULTS(" + EDStatic.fullTestCacheDirectory + tName + ")=\n" + results);
/* */
        //.nc
        String2.log("\n*** EDDGridFromDap test get .nc axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory  + tName, true);
        expected = 
"netcdf EDDGridFromDap_Axis.nc {\n" +
" dimensions:\n" +
"   time = 3;\n" +   // (has coord.var)\n" +    //changed when switched to netcdf-java 4.0, 2009-02-23
"   longitude = 1;\n" +   // (has coord.var)\n" +
" variables:\n" +
"   double time(time=3);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.0260864E9, 1.1661408E9; // double\n" + //up-to-date
"     :axis = \"T\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Centered Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   double longitude(longitude=1);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = 360.0, 360.0; // double\n" +
"     :axis = \"X\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"\n" +
" :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
" :cdm_data_type = \"Grid\";\n" +
" :composite = \"true\";\n" +
" :contributor_name = \"NASA GSFC (OBPG)\";\n" +
" :contributor_role = \"Source of level 2 data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        expected = 
" :infoUrl = \"http://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n" +
" :institution = \"NOAA CoastWatch, West Coast Node\";\n" +
" :keywords = \"8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :naming_authority = \"gov.noaa.pfel.coastwatch\";\n" +
" :origin = \"NASA GSFC (OBPG)\";\n" +
" :processing_level = \"3\";\n" +
" :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
" :projection = \"geographic\";\n" +
" :projection_type = \"mapped\";\n" +
" :references = \"Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n" +
" :satellite = \"Aqua\";\n" +
" :sensor = \"MODIS\";\n" +
" :source = \"satellite observation: Aqua, MODIS\";\n" +
" :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\";\n" +
" :time_coverage_end = \"2006-12-15T00:00:00Z\";\n" +
" :time_coverage_start = \"2002-07-08T00:00:00Z\";\n" +
" :title = \"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\";\n" +
" :Westernmost_Easting = 360.0; // double\n" +
" data:\n" +
"time =\n" +
"  {1.0260864E9, 1.0960704E9, 1.1661408E9}\n" +
"longitude =\n" +
"  {360.0}\n" +
"}\n";
        tPo = results.indexOf(" :infoUrl");
        Test.ensureEqual(results.substring(tPo), expected, "RESULTS=\n" + results);
           

        //.ncHeader
        String2.log("\n*** EDDGridFromDap test get .NCHEADER axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".ncHeader"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"netcdf EDDGridFromDap_Axis.nc {\n" +
" dimensions:\n" +
"   time = 3;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23
"   longitude = 1;\n" +   // (has coord.var)\n" +
" variables:\n" +
"   double time(time=3);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.0260864E9, 1.1661408E9; // double\n" +  //up-to-date
"     :axis = \"T\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Centered Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   double longitude(longitude=1);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = 360.0, 360.0; // double\n" +
"     :axis = \"X\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"\n" +
" :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        expected = 
" :title = \"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\";\n" +
" :Westernmost_Easting = 360.0; // double\n" +
" data:\n" +
"}\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

         //.tsv
        String2.log("\n*** EDDGridFromDap test get .TSV axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".tsv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time\tlongitude\n" +
"UTC\tdegrees_east\n" +
"2002-07-08T00:00:00Z\t360.0\n" +
"2004-09-25T00:00:00Z\tNaN\n" +
"2006-12-15T00:00:00Z\tNaN\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);


         //.tsvp
        String2.log("\n*** EDDGridFromDap test get .tsv axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".tsvp"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time (UTC)\tlongitude (degrees_east)\n" +
"2002-07-08T00:00:00Z\t360.0\n" +
"2004-09-25T00:00:00Z\tNaN\n" +
"2006-12-15T00:00:00Z\tNaN\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.xhtml   latitude
        String2.log("\n*** EDDGridFromDap test get .XHTML axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "latitude[0:10:40],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_LatAxis", ".xhtml"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>EDDGridFromDap_LatAxis</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>latitude</th>\n" +
"<th>longitude</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_north</th>\n" +
"<th>degrees_east</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-90.0</td>\n" +
"<td align=\"right\">360.0</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-89.58323686038435</td>\n" +
"<td></td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-89.16647372076869</td>\n" +
"<td></td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-88.74971058115304</td>\n" +
"<td></td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-88.3329474415374</td>\n" +
"<td></td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.htmlTable   latitude
        String2.log("\n*** EDDGridFromDap test get .htmlTable axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "latitude[0:10:40],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_LatAxis", ".htmlTable"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
EDStatic.startHeadHtml(EDStatic.erddapUrl((String)null), "EDDGridFromDap_LatAxis") + "\n" +
"</head>\n" +
EDStatic.startBodyHtml(null) + "\n" +
"&nbsp;\n" +
"<form action=\"\">\n" +
"<input type=\"button\" value=\"Back\" onClick=\"history.go(-1);return true;\">\n" +
"</form>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>latitude\n" +
"<th>longitude\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_north\n" +
"<th>degrees_east\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-90.0\n" +
"<td align=\"right\">360.0\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-89.58323686038435\n" +
"<td>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-89.16647372076869\n" +
"<td>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-88.74971058115304\n" +
"<td>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-88.3329474415374\n" +
"<td>&nbsp;\n" +
"</tr>\n" +
"</table>\n" +
EDStatic.endBodyHtml(EDStatic.erddapUrl((String)null)) + "\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.xhtml   time
        String2.log("\n*** EDDGridFromDap test get .XHTML axis data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "time[0:100:200],longitude[last]", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Axis", ".xhtml"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"<tr>\n" +
"<th>time</th>\n" +
"<th>longitude</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>degrees_east</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">2002-07-08T00:00:00Z</td>\n" +
"<td align=\"right\">360.0</td>\n" +
"</tr>\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);
        expected = 
"<tr>\n" +
"<td nowrap=\"nowrap\">2006-12-15T00:00:00Z</td>\n" +
"<td></td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);


    }


    public static void testBasic3() throws Throwable {
        String2.log("\n*** EDDGridFromDap.testBasic3\n");
        testVerboseOn();
        EDDGridFromDap gridDataset;
        String name, tName, axisDapQuery, results, expected, error;
        int tPo;
        String userDapQuery  = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
        String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]"; 
        String mapDapQuery   = "chlorophyll[200][][(29):(50)][(225):(247)]"; //stride irrelevant 
        StringArray destinationNames = new StringArray();
        IntArray constraints = new IntArray();

        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdMHchla8day"); //should work
        //just comment out to work on some other test

        //******************************************** test GridDataRandomAccessor
        //set up GridDataRandomAccessor
        GridDataAccessor gda = new GridDataAccessor(gridDataset, "", userDapQuery, true, true); //rowMajor toNaN
        GridDataRandomAccessor gdra = new GridDataRandomAccessor(gda);
        //maka a new rowMajor gda and test if same data
        gda = new GridDataAccessor(gridDataset, "", userDapQuery, true, true); //rowMajor toNaN
        int current[] = gda.totalIndex().getCurrent(); //the internal object that changes
        int count = 0;
        while (gda.increment()) {
            //String2.log(String2.toCSSVString(current));  //to prove that access is rowMajor
            Test.ensureEqual(gda.getDataValueAsDouble(0), 
                gdra.getDataValueAsDouble(current, 0), "count=" + count);
            count++;
        }
        String2.log("Test of GridDataRandomAccess rowMajor succeeded. count=" + count);
        //maka a new columnMajor gda and test if same data
        gda = new GridDataAccessor(gridDataset, "", userDapQuery, false, true); //rowMajor toNaN
        current = gda.totalIndex().getCurrent(); //the internal object that changes
        count = 0;
        while (gda.increment()) {
            //String2.log(String2.toCSSVString(current)); //to prove that access is columnMajor
            Test.ensureEqual(gda.getDataValueAsDouble(0), 
                gdra.getDataValueAsDouble(current, 0), "count=" + count);
            count++;
        }
        String2.log("Test of GridDataRandomAccess columnMajor succeeded. count=" + count);


        //********************************************** test getting grid data
        //.asc
        String2.log("\n*** EDDGridFromDap test get .ASC data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".asc"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log("\n.asc results=\n" + results);
        expected = 
"Dataset {[10]\n" +
"  GRID {[10]\n" +
"    ARRAY:[10]\n" +
"      Float32 chlorophyll[time = 1][altitude = 1][latitude = 51][longitude = 53];[10]\n" +
"    MAPS:[10]\n" +
"      Float64 time[time = 1];[10]\n" +
"      Float64 altitude[altitude = 1];[10]\n" +
"      Float64 latitude[latitude = 51];[10]\n" +
"      Float64 longitude[longitude = 53];[10]\n" +
"  } chlorophyll;[10]\n" +
"} erdMHchla8day;[10]\n" +
"---------------------------------------------[10]\n" +
"chlorophyll.chlorophyll[1][1][51][53][10]\n" +
//missing values are sourceMissingValue
//"[0][0][0], -9999999.0, -9999999.0, 0.099, 0.118, -9999999.0, 0.091, -9999999.0,"; //pre 2010-10-26
//"[0][0][0], -9999999.0, -9999999.0, 0.10655, 0.12478, -9999999.0, 0.09398, -9999999.0, 0.08919, 0.09892,"; //pre 2012-08-17
"[0][0][0], -9999999.0, -9999999.0, 0.11093, 0.12439, -9999999.0, 0.09554, -9999999.0, 0.09044, 0.10009,";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        expected = "[0][0][42], -9999999.0,";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);
        expected = 
"chlorophyll.time[1][10]\n" +
"1.17072E9[10]\n" +
"[10]\n" +
"chlorophyll.altitude[1][10]\n" +
"0.0[10]\n" +
"[10]\n" +
"chlorophyll.latitude[51][10]\n" +
"28.985876360268577, 29.40263949988423, 29.81940263949987, 30.236165779115524, 30.652928918731178, 31.06969205834683, 31.486455197962485, 31.90321833757814, 32.31998147719379, 32.73674461680943, 33.153507756425086, 33.57027089604074, 33.98703403565639, 34.40379717527205, 34.8205603148877, 35.237323454503354, 35.65408659411899, 36.07084973373465, 36.4876128733503, 36.904376012965955, 37.32113915258161, 37.73790229219726, 38.1546654318129, 38.571428571428555, 38.98819171104421, 39.40495485065986, 39.821717990275516, 40.23848112989117, 40.655244269506824, 41.07200740912248, 41.48877054873813, 41.905533688353785, 42.32229682796944, 42.73905996758509, 43.155823107200746, 43.57258624681637, 43.989349386432025, 44.40611252604768, 44.82287566566333, 45.239638805278986, 45.65640194489464, 46.07316508451029, 46.48992822412595, 46.9066913637416, 47.323454503357254, 47.74021764297291, 48.15698078258856, 48.573743922204216, 48.99050706181987, 49.407270201435495, 49.82403334105115[10]\n" +
"[10]\n" +
"chlorophyll.longitude[53][10]\n" +
"224.98437319134158, 225.40108808889917, 225.81780298645677, 226.23451788401434, 226.65123278157193, 227.06794767912953, 227.4846625766871, 227.9013774742447, 228.3180923718023, 228.73480726935986, 229.15152216691746, 229.56823706447506, 229.98495196203262, 230.40166685959022, 230.81838175714782, 231.2350966547054, 231.65181155226298, 232.06852644982058, 232.48524134737815, 232.90195624493575, 233.31867114249334, 233.7353860400509, 234.1521009376085, 234.5688158351661, 234.98553073272367, 235.40224563028127, 235.81896052783887, 236.23567542539644, 236.65239032295403, 237.06910522051163, 237.48582011806923, 237.9025350156268, 238.3192499131844, 238.735964810742, 239.15267970829956, 239.56939460585716, 239.98610950341475, 240.40282440097232, 240.81953929852992, 241.23625419608751, 241.65296909364508, 242.06968399120268, 242.48639888876028, 242.90311378631785, 243.31982868387544, 243.73654358143304, 244.1532584789906, 244.5699733765482, 244.9866882741058, 245.40340317166337, 245.82011806922097, 246.23683296677856, 246.65354786433613[10]\n[end]";
        int po7 = results.indexOf("chlorophyll.time");
        Test.ensureEqual(results.substring(po7), expected, "RESULTS=\n" + results);

        //.csv
        String2.log("\n*** EDDGridFromDap test get .CSV data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected =  //missing values are "NaN"
/*pre 2010-10-26 was:
"time, altitude, latitude, longitude, chlorophyll\n" +
"UTC, m, degrees_north, degrees_east, mg m-3\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n"; 
pre 2012-08-17 was 
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09398\n"; */
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09554\n";

        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        expected = //"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.37\n"; //pre 2010-10-26
                   //"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n"; //pre 2012-08-17
                     "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

        //.csv   test gridName.gridName notation
        String2.log("\n*** EDDGridFromDap test get .CSV data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, "chlorophyll." + userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_DotNotation", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
/*pre 2010-10-26 was:
"time, altitude, latitude, longitude, chlorophyll\n" +
"UTC, m, degrees_north, degrees_east, mg m-3\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n";
//pre 2012-08-17 was 
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09398\n"; */
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09554\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        expected = //"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.37\n"; //pre 2010-10-26
                   //"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n"; //pre 2012-08-17
                     "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

        //.das
        String2.log("\n*** EDDGridFromDap test get .DAS data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        expected = 
//"Attributes {[10]\n" +
//"  time {[10]\n" +
//"    Float64 actual_range 1.17072e+9, 1.17072e+9;[10]\n" + //changes sometimes
"    String axis \"T\";[10]\n" +
"    Int32 fraction_digits 0;[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Centered Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n" +
"  altitude {[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  NC_GLOBAL {[10]\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";[10]\n" +
"    String cdm_data_type \"Grid\";[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

        //.dds
        String2.log("\n*** EDDGridFromDap test get .DDS data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  GRID {[10]\n" +
"    ARRAY:[10]\n" +
"      Float32 chlorophyll[time = 1][altitude = 1][latitude = 51][longitude = 53];[10]\n" +
"    MAPS:[10]\n" +
"      Float64 time[time = 1];[10]\n" +
"      Float64 altitude[altitude = 1];[10]\n" +
"      Float64 latitude[latitude = 51];[10]\n" +
"      Float64 longitude[longitude = 53];[10]\n" +
"  } chlorophyll;[10]\n" +
"} erdMHchla8day;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);

        //.dods
        String2.log("\n*** EDDGridFromDap test get .DODS data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".dods"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  GRID {[10]\n" +
"    ARRAY:[10]\n" +
"      Float32 chlorophyll[time = 1][altitude = 1][latitude = 51][longitude = 53];[10]\n" +
"    MAPS:[10]\n" +
"      Float64 time[time = 1];[10]\n" +
"      Float64 altitude[altitude = 1];[10]\n" +
"      Float64 latitude[latitude = 51];[10]\n" +
"      Float64 longitude[longitude = 53];[10]\n" +
"  } chlorophyll;[10]\n" +
"} erdMHchla8day;[10]\n" +
"[10]\n" +
"Data:[10]\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        results = String2.annotatedString(results);
        expected = 
"8222][231]~Sc.@n[8217]=8[206]{[176]@n[376][8217][243]I[8221]2@n[172][232][173][196][172][179]@n[186]>h?[197]5@n[199][8221]\"[186][221][183]@n" +
"[212][233][221]5[246]8[end][end]";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

        //.esriAscii 
        String2.log("\n*** EDDGridFromDap test get .esriAscii data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Map", //must be Map because .asc already used
            ".esriAscii"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = //note that lon values have been shifted from 225 to -135
"ncols 53\n" +
"nrows 51\n" +
"xllcenter -135.01562680865842\n" +
"yllcenter 28.985876360268577\n" +
"cellsize 0.4167631396156514\n" +
"nodata_value -9999999\n" +
"-9999999 -9999999 -9999999 -9999999 -9999999 -9999999 -9999999 -9999999 -9999999";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        expected = //last row
//"0.204 0.239 0.26 0.252 0.274 0.289 0.367 0.37 0.65 0.531 -9999999 -9999999 1.141\n"; //pre 2010-10-26
//"0.27878 0.31141 0.32663 0.41135 0.40628 0.65426 0.4827 -9999999 -9999999 1.16268\n"; //pre 2010-08-17
"0.28787 0.31865 0.33447 0.43293 0.43297 0.68101 0.48409 -9999999 -9999999 1.20716\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

        //.json
        String2.log("\n*** EDDGridFromDap test get .JSON data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = //missing values are "null"

"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"altitude\", \"latitude\", \"longitude\", \"chlorophyll\"],\n" +
"    \"columnTypes\": [\"String\", \"double\", \"double\", \"double\", \"float\"],\n" +
"    \"columnUnits\": [\"UTC\", \"m\", \"degrees_north\", \"degrees_east\", \"mg m-3\"],\n" +
"    \"rows\": [\n" +
"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 224.98437319134158, null],\n" +
"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 225.40108808889917, null],\n" +
//pre 2010-10-26, was
//"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 225.81780298645677, 0.099],\n" +
//"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 226.23451788401434, 0.118],\n"; 
//pre 2012-08-17 was:
//"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 225.81780298645677, 0.10655],\n" +
//"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 226.23451788401434, 0.12478],\n"; */
"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 225.81780298645677, 0.11093],\n" +
"      [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577, 226.23451788401434, 0.12439],\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        expected = 
"      [\"2007-02-06T00:00:00Z\", 0.0, 49.82403334105115, 246.23683296677856, null],\n" +
"      [\"2007-02-06T00:00:00Z\", 0.0, 49.82403334105115, 246.65354786433613, null]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);
/* */
        //.mat
        //octave> load('c:/temp/griddap/EDDGridFromDap_Data.mat');
        //octave> erdMHchla8day
        String2.log("\n*** EDDGridFromDap test get .MAT data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".mat"); 
        results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);

        //String2.log(results);
        expected = 
"4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
"69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
"20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
"6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
"20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
"00 00 00 0e 00 00 2f 98   00 00 00 06 00 00 00 08         /          |\n" +
"00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0d                    |\n" +
"65 72 64 4d 48 63 68 6c   61 38 64 61 79 00 00 00   erdMHchla8day    |\n" +
"00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 a0                    |\n" +
"74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"61 6c 74 69 74 75 64 65   00 00 00 00 00 00 00 00   altitude         |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"6c 61 74 69 74 75 64 65   00 00 00 00 00 00 00 00   latitude         |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"63 68 6c 6f 72 6f 70 68   79 6c 6c 00 00 00 00 00   chlorophyll      |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 08   41 d1 71 f1 40 00 00 00           A q @    |\n" +
"00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 08   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 0e 00 00 01 c8   00 00 00 06 00 00 00 08                    |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 33 00 00 00 01   00 00 00 01 00 00 00 00      3             |\n" +
"00 00 00 09 00 00 01 98   40 3c fc 62 64 a5 40 c8           @< bd @  |\n" +
"40 3d 67 13 61 dc 14 a0   40 3d d1 c4 5f 12 e8 74   @=g a   @=  _  t |\n" +
"40 3e 3c 75 5c 49 bc 4c   40 3e a7 26 59 80 90 24   @><u\\I L@> &Y  $ |\n" +
"40 3f 11 d7 56 b7 63 fc   40 3f 7c 88 53 ee 37 d4   @?  V c @?| S 7  |\n" +
"40 3f e7 39 51 25 0b ac   40 40 28 f5 27 2d ef c2   @? 9Q%  @@( '-   |\n" +
"40 40 5e 4d a5 c9 59 ac   40 40 93 a6 24 64 c3 98   @@^M  Y @@  $d   |\n" +
"40 40 c8 fe a3 00 2d 84   40 40 fe 57 21 9b 97 70   @@    - @@ W!  p |";
        results = results.substring(0, 71 * 4) + results.substring(71 * 7); //remove the creation dateTime
        results = results.substring(0, Math.min(results.length(), expected.length())); //remove the creation dateTime
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);
/* */
        //.nc
        String2.log("\n*** EDDGridFromDap test get .NC data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory  + tName, true);
        expected = //changed a little ("// (has coord.var)") when switched to netcdf-java 4.0, 2009-02-23
"netcdf EDDGridFromDap_Data.nc {\n" +
" dimensions:\n" +
"   time = 1;\n" +
"   altitude = 1;\n" +
"   latitude = 51;\n" +
"   longitude = 53;\n" +
" variables:\n" +
"   double time(time=1);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.17072E9, 1.17072E9; // double\n" +
"     :axis = \"T\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Centered Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   double altitude(altitude=1);\n" +
"     :_CoordinateAxisType = \"Height\";\n" +
"     :_CoordinateZisPositive = \"up\";\n" +
"     :actual_range = 0.0, 0.0; // double\n" +
"     :axis = \"Z\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Altitude\";\n" +
"     :positive = \"up\";\n" +
"     :standard_name = \"altitude\";\n" +
"     :units = \"m\";\n" +
"   double latitude(latitude=51);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 28.985876360268577, 49.82403334105115; // double\n" +
"     :axis = \"Y\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   double longitude(longitude=53);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = 224.98437319134158, 246.65354786433613; // double\n" +
"     :axis = \"X\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float chlorophyll(time=1, altitude=1, latitude=51, longitude=53);\n" +
"     :_FillValue = -9999999.0f; // float\n" +
"     :colorBarMaximum = 30.0; // double\n" +
"     :colorBarMinimum = 0.03; // double\n" +
"     :colorBarScale = \"Log\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 2; // int\n" +
"     :ioos_category = \"Ocean Color\";\n" +
"     :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n" +
"     :missing_value = -9999999.0f; // float\n" +
"     :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" +
"     :units = \"mg m-3\";\n" +
"\n" +
" :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
" :cdm_data_type = \"Grid\";\n" +
" :composite = \"true\";\n" +
" :contributor_name = \"NASA GSFC (OBPG)\";\n" +
" :contributor_role = \"Source of level 2 data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        expected = //note original missing values
" :infoUrl = \"http://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n" +
" :institution = \"NOAA CoastWatch, West Coast Node\";\n" +
" :keywords = \"8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :naming_authority = \"gov.noaa.pfel.coastwatch\";\n" +
" :Northernmost_Northing = 49.82403334105115; // double\n" +
" :origin = \"NASA GSFC (OBPG)\";\n" +
" :processing_level = \"3\";\n" +
" :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
" :projection = \"geographic\";\n" +
" :projection_type = \"mapped\";\n" +
" :references = \"Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n" +
" :satellite = \"Aqua\";\n" +
" :sensor = \"MODIS\";\n" +
" :source = \"satellite observation: Aqua, MODIS\";\n" +
" :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
" :Southernmost_Northing = 28.985876360268577; // double\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\";\n" +
" :time_coverage_end = \"2007-02-06T00:00:00Z\";\n" +
" :time_coverage_start = \"2007-02-06T00:00:00Z\";\n" +
" :title = \"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\";\n" +
" :Westernmost_Easting = 224.98437319134158; // double\n" +
" data:\n" +
"time =\n" +
"  {1.17072E9}\n" +
"altitude =\n" +
"  {0.0}\n" +
"latitude =\n" +
"  {28.985876360268577, 29.40263949988423, 29.81940263949987, 30.236165779115524, 30.652928918731178, 31.06969205834683, 31.486455197962485, 31.90321833757814, 32.31998147719379, 32.73674461680943, 33.153507756425086, 33.57027089604074, 33.98703403565639, 34.40379717527205, 34.8205603148877, 35.237323454503354, 35.65408659411899, 36.07084973373465, 36.4876128733503, 36.904376012965955, 37.32113915258161, 37.73790229219726, 38.1546654318129, 38.571428571428555, 38.98819171104421, 39.40495485065986, 39.821717990275516, 40.23848112989117, 40.655244269506824, 41.07200740912248, 41.48877054873813, 41.905533688353785, 42.32229682796944, 42.73905996758509, 43.155823107200746, 43.57258624681637, 43.989349386432025, 44.40611252604768, 44.82287566566333, 45.239638805278986, 45.65640194489464, 46.07316508451029, 46.48992822412595, 46.9066913637416, 47.323454503357254, 47.74021764297291, 48.15698078258856, 48.573743922204216, 48.99050706181987, 49.407270201435495, 49.82403334105115}\n" +
"longitude =\n" +
"  {224.98437319134158, 225.40108808889917, 225.81780298645677, 226.23451788401434, 226.65123278157193, 227.06794767912953, 227.4846625766871, 227.9013774742447, 228.3180923718023, 228.73480726935986, 229.15152216691746, 229.56823706447506, 229.98495196203262, 230.40166685959022, 230.81838175714782, 231.2350966547054, 231.65181155226298, 232.06852644982058, 232.48524134737815, 232.90195624493575, 233.31867114249334, 233.7353860400509, 234.1521009376085, 234.5688158351661, 234.98553073272367, 235.40224563028127, 235.81896052783887, 236.23567542539644, 236.65239032295403, 237.06910522051163, 237.48582011806923, 237.9025350156268, 238.3192499131844, 238.735964810742, 239.15267970829956, 239.56939460585716, 239.98610950341475, 240.40282440097232, 240.81953929852992, 241.23625419608751, 241.65296909364508, 242.06968399120268, 242.48639888876028, 242.90311378631785, 243.31982868387544, 243.73654358143304, 244.1532584789906, 244.5699733765482, 244.9866882741058, 245.40340317166337, 245.82011806922097, 246.23683296677856, 246.65354786433613}\n" +
"chlorophyll =\n" +
"  {\n" +
"    {\n" +
"      {\n" +
//pre 2010-10-26 was 
//"        {-9999999.0, -9999999.0, 0.099, 0.118, -9999999.0, 0.091, -9999999.0, 0.088, 0.085, 0.088, -9999999.0, 0.098, -9999999.0, 0.076, -9999999.0, 0.07, 0.071, -9999999.0, -9999999.0, -9999999.0, 0.078, -9999999.0, 0.09, 0.084, -9999999.0, -9999999.0, 0.098, -9999999.0, 0.079, 0.076, 0.085, -9999999.0, 0.086, 0.127, 0.199, 0.167, 0.191, 0.133, 0.14, 0.173, 0.204, 0.239, 0.26, 0.252, 0.274, 0.289, 0.367, 0.37, 0.65, 0.531, -9999999.0, -9999999.0, 1.141},\n";
//pre 2012-08-17 was
//"        {-9999999.0, -9999999.0, 0.10655, 0.12478, -9999999.0, 0.09398, -9999999.0, 0.08919, 0.09892, 0.10007, -9999999.0, 0.09986, -9999999.0, 0.07119, -9999999.0, 0.08288, 0.08163, -9999999.0, -9999999.0, -9999999.0, 0.08319, -9999999.0, 0.09706, 0.08309, -9999999.0, -9999999.0, 0.0996, -9999999.0, 0.08962, 0.08329, 0.09101, -9999999.0, 0.08679, 0.13689, 0.21315, 0.18729, 0.21642, 0.15069, 0.15123, 0.18849, 0.22975, 0.27075, 0.29062, 0.27878, 0.31141, 0.32663, 0.41135, 0.40628, 0.65426, 0.4827, -9999999.0, -9999999.0, 1.16268},\n";
  "        {-9999999.0, -9999999.0, 0.11093, 0.12439, -9999999.0, 0.09554, -9999999.0, 0.09044, 0.10009, 0.10116, -9999999.0, 0.10095, -9999999.0, 0.07243, -9999999.0, 0.08363, 0.08291, -9999999.0, -9999999.0, -9999999.0, 0.08885, -9999999.0, 0.09632, 0.0909, -9999999.0, -9999999.0, 0.09725, -9999999.0, 0.09978, 0.09462, 0.09905, -9999999.0, 0.09937, 0.12816, 0.20255, 0.17595, 0.20562, 0.14333, 0.15073, 0.18803, 0.22673, 0.27252, 0.29005, 0.28787, 0.31865, 0.33447, 0.43293, 0.43297, 0.68101, 0.48409, -9999999.0, -9999999.0, 1.20716},\n";
        tPo = results.indexOf(" :infoUrl");
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "RESULTS=\n" + results);


        //.ncHeader
        String2.log("\n*** EDDGridFromDap test get .NCHEADER data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".ncHeader"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //if (true) System.exit(1);
        expected = 
"netcdf EDDGridFromDap_Data.nc {\n" +
" dimensions:\n" +
"   time = 1;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23
"   altitude = 1;\n" +   // (has coord.var)\n" +
"   latitude = 51;\n" +   // (has coord.var)\n" +
"   longitude = 53;\n" +   // (has coord.var)\n" +
" variables:\n" +
"   double time(time=1);\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);

        expected =  //test that sourceMissingValue is intact
         //(but complicated because that is the mv I use in Grid)
         //test that actual_range has been removed
"   float chlorophyll(time=1, altitude=1, latitude=51, longitude=53);\n" +
"     :_FillValue = -9999999.0f; // float\n" +
"     :colorBarMaximum = 30.0; // double\n" +
"     :colorBarMinimum = 0.03; // double\n" +
"     :colorBarScale = \"Log\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 2; // int\n" +
"     :ioos_category = \"Ocean Color\";\n" +
"     :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n" +
"     :missing_value = -9999999.0f; // float\n" +
"     :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" +
"     :units = \"mg m-3\";\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

        expected = 
" :title = \"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\";\n" +
" :Westernmost_Easting = 224.98437319134158; // double\n" +  //note updated value
" data:\n" +
"}\n";
        int po10 = results.indexOf(" :title");
        Test.ensureEqual(results.substring(po10), expected, "RESULTS=\n" + results);

        //.tsv
        String2.log("\n*** EDDGridFromDap test get .TSV data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".tsv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time\taltitude\tlatitude\tlongitude\tchlorophyll\n" +
"UTC\tm\tdegrees_north\tdegrees_east\tmg m-3\n" +
"2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t224.98437319134158\tNaN\n" +
"2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.40108808889917\tNaN\n" +
// pre 2010-10-26 was
//"2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.81780298645677\t0.099\n" +
//"2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t226.23451788401434\t0.118\n";
//pre 2012-08-17 was
//"2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.81780298645677\t0.10655\n" +
//"2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t226.23451788401434\t0.12478\n";
  "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.81780298645677\t0.11093\n" +
  "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t226.23451788401434\t0.12439\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        expected = 
//pre 2010-10-26 was  
//"2007-02-06T00:00:00Z\t0.0\t49.407270201435495\t232.06852644982058\t0.37\n";   
//pre 2012-08-17 was
//"2007-02-06T00:00:00Z\t0.0\t49.407270201435495\t232.06852644982058\t0.58877\n";
  "2007-02-06T00:00:00Z\t0.0\t49.407270201435495\t232.06852644982058\t0.56545\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

        //.xhtml
        String2.log("\n*** EDDGridFromDap test get .XHTMLTABLE data\n");
        tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Data", ".xhtml"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
        expected = 
"<th>time</th>\n" +
"<th>altitude</th>\n" +
"<th>latitude</th>\n" +
"<th>longitude</th>\n" +
"<th>chlorophyll</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>m</th>\n" +
"<th>degrees_north</th>\n" +
"<th>degrees_east</th>\n" +
"<th>mg m-3</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">2007-02-06T00:00:00Z</td>\n" +
"<td align=\"right\">0.0</td>\n" +
"<td align=\"right\">28.985876360268577</td>\n" +
"<td align=\"right\">224.98437319134158</td>\n" +
"<td></td>\n" +   //missing value is ""
"</tr>";
        int po11 = results.indexOf("<th>time</th>");
        Test.ensureEqual(results.substring(po11, po11 + expected.length()), expected, "RESULTS=\n" + results.substring(0, 1000));
        expected = 
"<tr>\n" +
"<td nowrap=\"nowrap\">2007-02-06T00:00:00Z</td>\n" +
"<td align=\"right\">0.0</td>\n" +
"<td align=\"right\">49.82403334105115</td>\n" +
"<td align=\"right\">246.65354786433613</td>\n" +
"<td></td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, 
            "RESULTS=\n" + results.substring(results.length() - expected.length()));

        //test " in attributes
        try {
            EDDGrid tedg = (EDDGridFromDap)oneFromDatasetXml("erdSGchla8day");
            String2.log("\n***raw references=" + tedg.addGlobalAttributes.getString("references"));
            tName = tedg.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedg.className() + "Quotes", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            int po9 = results.indexOf("The 4th Pacific");
            String2.log("*** in results: " + results.substring(po9 - 10, po9 + 15));
            expected = " Proceedings of \"\"The 4th Pacific";
            Test.ensureTrue(results.indexOf(expected) < 0, "\nresults=\n" + results);
            expected = " Proceedings of \\\"The 4th Pacific";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }


        //test loading other datasets
        oneFromDatasetXml("erdAGssta1day");
        //oneFromDatasetXml("erdAGssta8day"); 
        //oneFromDatasetXml("erdAGsstamday"); 
        //oneFromDatasetXml("erdPHssta1day"); 
        //oneFromDatasetXml("erdPHssta8day");
        //oneFromDatasetXml("erdPHsstamday"); 
        // */

    }


    public static void testGraphics() throws Throwable {
        testVerboseOn();
        EDDGridFromDap gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdMHchla8day"); 
        String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]"; 
        String mapDapQuery   = "chlorophyll[200][][(29):(45)][(225):(247)]"; //stride irrelevant 
        String tName, results;
boolean testAll = true;

        //*** test getting graphs
        String2.log("\n****************** EDDGridFromDap test get graphs\n");

        //test graph .png
        if (testAll || false) {
            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.size=128|240&.font=0.75", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphTiny", 
                ".largePng"); //to show it's irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphS", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Graph", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphL", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.size=1700|1800&.font=3", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphHuge", 
                ".smallPng"); //to show it's irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

        //test graph .pdf
        if (testAll || false) {
            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphS", ".smallPdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Graph", ".pdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphL", ".largePdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

        //test legend= options
        if (testAll || false) {
            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.legend=Off&.trim=10", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphLegendOff", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphLegendOnlySmall", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphLegendOnlyMed", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphLegendOnlyLarge", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, graphDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_GraphTransparentLegendOnly", ".transparentPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }


        //*** test getting colored surface graph
        //String2.log("\n****************** EDDGridFromDap test get colored surface graph\n");
        //not working yet    time axis is hard to work with
        //String tempDapQuery = "chlorophyll[0:10:20][][(29):1:(50)][(225):1:(225)]";
        //tName = gridDataset.makeNewFileForDapQuery(null, null, tempDapQuery, 
        //    EDStatic.fullTestCacheDirectory, gridDataset.className() + "_CSGraph", ".png"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


        //*** test getting map .png
        if (testAll || false) {
            String2.log("\n****************** EDDGridFromDap test get maps\n");

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery + "&.size=120|280&.font=0.75", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapTiny", 
                ".largePng"); //to show it's irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapS", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Map", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapL", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery + "&.size=1700|1800&.font=3", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapHuge", 
                ".smallPng"); //to show it's irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }


        //test getting map transparentPng
        if (testAll || false) {

            //yes, stretched.  that's what query is asking for
            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery + "&.size=300|400", 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapTPSmall", ".transparentPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapTP", ".transparentPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

        //test map pdf
        if (testAll || false) {
            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapS", ".smallPdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Map", ".pdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_MapL", ".largePdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

        //test map kml
        if (testAll || false) {
            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Map", ".kml"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log("results=\n" + results);
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }


        //test .geotif
        if (testAll || false) {
            tName = gridDataset.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_Map", ".geotif"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            EDDGrid agssta8 = (EDDGridFromDap)oneFromDatasetXml("erdAGssta8day"); //should work
//        tName = agssta8.makeNewFileForDapQuery(null, null, dataIndexExample, 
//            EDStatic.fullTestCacheDirectory, agssta8.className() + "std1", ".csv"); 
//        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//        String2.log(results);

// currently not an option (after changes to kml in late 2009)
//        //animated map   (works, just not usually an active test)
//        if (true) {
//            tName = agssta8.makeNewFileForDapQuery(null, null, "sst[200:2:220][][(29):10:(50)][(225):10:(247)]",
//                EDStatic.fullTestCacheDirectory, agssta8.className() + "_Animated", ".kml"); 
//            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//            String2.log("results=\n" + results);
//            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
//        }

            //this is cool looking (but you have to double click on file in windows explorer to see it)
            tName = agssta8.makeNewFileForDapQuery(null, null, EDStatic.EDDGridMapExample, 
                EDStatic.fullTestCacheDirectory, agssta8.className() + "_ExampleMap", ".geotif"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName); //doesn't display it or show error message
        }
 
        //give graphs some time to be displayed (other test() may delete the files)
        Math2.sleep(2000);
    }


    public static void testOpendap() throws Throwable {
        try {
            String2.log("\n****************** EDDGridFromDap test opendap\n" +
                "!!!THIS READS DATA FROM SERVER RUNNING ON COASTWATCH: erdMHchla8day on " + 
                EDStatic.erddapUrl + "!!!"); //in tests, always non-https url
            testVerboseOn();
            String results, expected, tName;
            int tPo;
            String userDapQuery  = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
            String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]"; 
            String mapDapQuery   = "chlorophyll[200][][(29):(50)][(225):(247)]"; //stride irrelevant 
            StringArray destinationNames = new StringArray();
            IntArray constraints = new IntArray();

            //get das and dds
            String threddsUrl = "http://192.168.31.18/thredds/dodsC/satellite/MH/chla/8day";
            String erddapUrl  = EDStatic.erddapUrl + "/griddap/erdMHchla8day"; //in tests, always non-https url
            DConnect threddsConnect = new DConnect(threddsUrl, true, 1, 1);
            DConnect erddapConnect  = new DConnect(erddapUrl,  true, 1, 1); //in tests, always non-https url
            DAS das = erddapConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
            DDS dds = erddapConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
            PrimitiveArray tpas[], epas[];

            //get global attributes
            Attributes attributes = new Attributes();
            OpendapHelper.getAttributes(das, "GLOBAL", attributes);
            Test.ensureEqual(attributes.getString("contributor_name"), "NASA GSFC (OBPG)", "");
            Test.ensureEqual(attributes.getString("keywords"), "8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn", "");

            //get attributes for a dimension 
            attributes.clear();
            OpendapHelper.getAttributes(das, "latitude", attributes);
            Test.ensureEqual(attributes.getString("coordsys"), "geographic", "");
            Test.ensureEqual(attributes.get("fraction_digits"), new IntArray(new int[]{4}), ""); //test if stored in correct form

            //get attributes for grid variable
            attributes.clear();
            OpendapHelper.getAttributes(das, "chlorophyll", attributes);
            Test.ensureEqual(attributes.getString("standard_name"), "concentration_of_chlorophyll_in_sea_water", "");
            Test.ensureEqual(attributes.getString("units"), "mg m-3", "");

            //test get dimension data - all
            String threddsQuery = "lat";
            String erddapQuery  = "latitude";
            String2.log("\nFrom thredds:\n" + String2.annotatedString(new String(
                gov.noaa.pfel.coastwatch.util.SSR.getUrlResponseBytes(threddsUrl + ".asc?" + threddsQuery))));
            String2.log("\nFrom erddap:\n" + String2.annotatedString(new String(
                gov.noaa.pfel.coastwatch.util.SSR.getUrlResponseBytes(erddapUrl + ".asc?" + erddapQuery)))); //in tests, always non-https url
            tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsQuery);
            epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + erddapQuery);
            Test.ensureEqual(tpas[0].size(), 4320, ""); 
            Test.ensureEqual(epas[0].size(), 4320, "");
            Test.ensureEqual(tpas[0].getDouble(0), -90, ""); 
            Test.ensureEqual(epas[0].getDouble(0), -90, "");
            Test.ensureEqual(tpas[0].getDouble(3000), 35.02894188469553, ""); 
            Test.ensureEqual(epas[0].getDouble(3000), 35.02894188469553, ""); 

            //test get dimension data - part
            threddsQuery = "lat[10:2:20]";
            erddapQuery  = "latitude[10:2:20]";
            String2.log("\nFrom thredds:\n" + String2.annotatedString(new String(
                gov.noaa.pfel.coastwatch.util.SSR.getUrlResponseBytes(threddsUrl + ".asc?" + threddsQuery))));
            String2.log("\nFrom erddap:\n" + String2.annotatedString(new String(
                gov.noaa.pfel.coastwatch.util.SSR.getUrlResponseBytes(erddapUrl + ".asc?" + erddapQuery)))); //in tests, always non-https url
            tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsQuery);
            epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + erddapQuery);
            Test.ensureEqual(tpas[0].toString(), "-89.58323686038435, -89.49988423246121, -89.4165316045381, -89.33317897661496, -89.24982634869183, -89.16647372076869", ""); 
            Test.ensureEqual(epas[0].toString(), "-89.58323686038435, -89.49988423246121, -89.4165316045381, -89.33317897661496, -89.24982634869183, -89.16647372076869", ""); 

            //get grid data
            //chlorophyll[177][0][2080:20:2500][4500:20:4940]
            String threddsUserDapQuery = "MHchla[177][0][2080:2:2082][4940]";
            String griddapUserDapQuery = "chlorophyll[177][0][2080:2:2082][4940]";
            String2.log("\nFrom thredds:\n" + String2.annotatedString(new String(
                gov.noaa.pfel.coastwatch.util.SSR.getUrlResponseBytes(threddsUrl + ".asc?" + threddsUserDapQuery))));
            String2.log("\nFrom erddap:\n" + String2.annotatedString(new String(
                gov.noaa.pfel.coastwatch.util.SSR.getUrlResponseBytes(erddapUrl + ".asc?" + griddapUserDapQuery)))); //in tests, always non-https url

            //corresponding time varies, so just make sure they match
            tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsUserDapQuery);
            epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + griddapUserDapQuery);
            Test.ensureEqual(epas[1], tpas[1], ""); //time
            Test.ensureEqual(epas[2], tpas[2], ""); //alt
            Test.ensureEqual(epas[3], tpas[3], ""); //lat
            Test.ensureEqual(epas[4], tpas[4], ""); //lon
            Test.ensureEqual(epas[0], tpas[0], ""); //data
            String tTime = Calendar2.epochSecondsToIsoStringT(tpas[1].getDouble(0));
            float tData1 = tpas[0].getFloat(0);
            float tData2 = tpas[0].getFloat(1);

            //*** test that EDDGridFromDAP works via netcdf-java library
            String2.log("\n****************** EDDGridFromDap test netcdf-java\n");
            NetcdfFile nc = NetcdfDataset.openFile(EDStatic.erddapUrl + "/griddap/erdMHchla8day", null); //in tests, always non-https url
            try {
                results = nc.toString();
                results = NcHelper.decodeNcDump(results); //added with switch to netcdf-java 4.0
                String tUrl = String2.replaceAll(EDStatic.erddapUrl, "http:", "dods:"); //in tests, always non-https url
                expected = 
"netcdf " + tUrl + "/griddap/erdMHchla8day {\n" +
" dimensions:\n" +
"   time = 466;\n" +   // (has coord.var)\n" +  //changes sometimes
"   altitude = 1;\n" +   // (has coord.var)\n" +
"   latitude = 4320;\n" +   // (has coord.var)\n" +
"   longitude = 8640;\n" +   // (has coord.var)\n" +
" variables:\n" +
"   double time(time=466);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.0260864E9, 1.3519872E9; // double\n" +  //2nd value changes sometimes
"     :axis = \"T\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Centered Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   double altitude(altitude=1);\n" +
"     :_CoordinateAxisType = \"Height\";\n" +
"     :_CoordinateZisPositive = \"up\";\n" +
"     :actual_range = 0.0, 0.0; // double\n" +
"     :axis = \"Z\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Altitude\";\n" +
"     :positive = \"up\";\n" +
"     :standard_name = \"altitude\";\n" +
"     :units = \"m\";\n" +
"   double latitude(latitude=4320);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = -90.0, 90.0; // double\n" +
"     :axis = \"Y\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   double longitude(longitude=8640);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = 0.0, 360.0; // double\n" +
"     :axis = \"X\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float chlorophyll(time=466, altitude=1, latitude=4320, longitude=8640);\n" +
"     :_CoordinateAxes = \"time altitude latitude longitude \";\n" +
"     :_FillValue = -9999999.0f; // float\n" +
"     :colorBarMaximum = 30.0; // double\n" +
"     :colorBarMinimum = 0.03; // double\n" +
"     :colorBarScale = \"Log\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 2; // int\n" +
"     :ioos_category = \"Ocean Color\";\n" +
"     :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n" +
"     :missing_value = -9999999.0f; // float\n" +
"     :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" +
"     :units = \"mg m-3\";\n" +
"\n" +
" :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
" :cdm_data_type = \"Grid\";\n" +
" :composite = \"true\";\n" +
" :contributor_name = \"NASA GSFC (OBPG)\";\n" +
" :contributor_role = \"Source of level 2 data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
" :date_created = \"2012-11-11Z\";\n" + //changes
" :date_issued = \"2012-11-11Z\";\n" + //changes
" :Easternmost_Easting = 360.0; // double\n" +
" :geospatial_lat_max = 90.0; // double\n" +
" :geospatial_lat_min = -90.0; // double\n" +
" :geospatial_lat_resolution = 0.041676313961565174; // double\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = 360.0; // double\n" +
" :geospatial_lon_min = 0.0; // double\n" +
" :geospatial_lon_resolution = 0.04167148975575877; // double\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_max = 0.0; // double\n" +
" :geospatial_vertical_min = 0.0; // double\n" +
" :geospatial_vertical_positive = \"up\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"NASA GSFC (OBPG)\n" +  //important test   re netcdf 4.0
"20";
                Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

                expected = 
" :satellite = \"Aqua\";\n" +
" :sensor = \"MODIS\";\n" +
" :source = \"satellite observation: Aqua, MODIS\";\n" +
" :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
" :Southernmost_Northing = -90.0; // double\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\";\n" +
" :time_coverage_end = \"2012-11-04T00:00:00Z\";\n" + //changes
" :time_coverage_start = \"2002-07-08T00:00:00Z\";\n" +
" :title = \"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\";\n" +
" :Westernmost_Easting = 0.0; // double\n" +
"}\n";
                Test.ensureEqual(results.substring(results.indexOf(" :satellite =")), expected, "RESULTS=\n" + results);

                attributes.clear();
                NcHelper.getGlobalAttributes(nc, attributes);
                Test.ensureEqual(attributes.getString("contributor_name"), "NASA GSFC (OBPG)", "");
                Test.ensureEqual(attributes.getString("keywords"), "8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn", 
                    "found=" + attributes.getString("keywords"));

                //get attributes for a dimension 
                Variable ncLat = nc.findVariable("latitude");
                attributes.clear();
                NcHelper.getVariableAttributes(ncLat, attributes);
                Test.ensureEqual(attributes.getString("coordsys"), "geographic", "");
                Test.ensureEqual(attributes.get("fraction_digits"), new IntArray(new int[]{4}), ""); //test if stored in correct form

                //get attributes for grid variable
                Variable ncChl = nc.findVariable("chlorophyll");
                attributes.clear();
                NcHelper.getVariableAttributes(ncChl, attributes);
                Test.ensureEqual(attributes.getString("standard_name"), "concentration_of_chlorophyll_in_sea_water", "");
                Test.ensureEqual(attributes.getString("units"), "mg m-3", "");

                //test get dimension data - all
                PrimitiveArray pa = NcHelper.getPrimitiveArray(ncLat);
                Test.ensureEqual(pa.elementClass(), double.class, "");
                Test.ensureEqual(pa.size(), 4320, "");
                Test.ensureEqual(pa.getDouble(0), -90, "");
                Test.ensureEqual(pa.getDouble(4319), 90, ""); 

                //test get dimension data - part
                pa = NcHelper.getPrimitiveArray(ncLat, 10, 20);
                Test.ensureEqual(pa.elementClass(), double.class, "");
                Test.ensureEqual(pa.size(), 11, "");
                Test.ensureEqual(pa.getDouble(0), -89.58323686038435, "");
                Test.ensureEqual(pa.getDouble(10), -89.16647372076869, ""); 

                //get grid data
                pa = NcHelper.get4DValues(ncChl, 4500, 2080, 0, 170, 190); //x,y,z,t1,t2
                Test.ensureEqual(pa.elementClass(), float.class, "");
                String2.log("pa=" + pa);
                Test.ensureEqual(pa.size(), 21, "");
                //pre 2010-10-26 was 0.113f
                //pre 2012-08-17 was 0.12906f
                Test.ensureEqual(pa.getFloat(0), 0.13295f, "");
                Test.ensureEqual(pa.getFloat(1), -9999999.0f, "");

            } finally {
                nc.close();
            }


            //*** test that EDDGridFromDap can treat itself as a datasource
            String2.log("\n*** EDDGridFromDap test can treat itself as a datasource\n");
            ArrayList tDataVariables = new ArrayList();

            EDDGrid eddGrid2 = new EDDGridFromDap(
                "erddapChlorophyll", //String tDatasetID, 
                null,
                null, null, null,
                null,  
                1, //tAltMetersPerSourceUnit
                null,  
                new Object[][] {
                    { //dataVariables[dvIndex][0=sourceName, 1=destName, 2=addAttributes]
                        "chlorophyll", null, null}},
                60, //int tReloadEveryNMinutes,
                erddapUrl); //sourceUrl); //in tests, always non-https url

            //.xhtml
            tName = eddGrid2.makeNewFileForDapQuery(null, null, griddapUserDapQuery, 
                EDStatic.fullTestCacheDirectory, eddGrid2.className() + "_Itself", ".xhtml"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>EDDGridFromDap_Itself</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time</th>\n" +
"<th>altitude</th>\n" +
"<th>latitude</th>\n" +
"<th>longitude</th>\n" +
"<th>chlorophyll</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>m</th>\n" +
"<th>degrees_north</th>\n" +
"<th>degrees_east</th>\n" +
"<th>mg m-3</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">" + tTime + "Z</td>\n" +
"<td align=\"right\">0.0</td>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-3.3132669599444426</td>\n" +
"<td align=\"right\">205.8571593934483</td>\n" +
"<td" + (tData1 == -9999999.0f? ">" : " align=\"right\">" + tData1) + "</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">" + tTime + "Z</td>\n" +
"<td align=\"right\">0.0</td>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-3.229914332021309</td>\n" +
"<td align=\"right\">205.8571593934483</td>\n" +
"<td" + (tData2 == -9999999.0f? ">" : " align=\"right\">" + tData2) + "</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);


            //check error...
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl + //in tests, always non-https url
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }


    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();
        //don't test local dataset because of dns/numericIP problems
        //this dataset is good test because it has several dimension combos
        String url = "http://data1.gfdl.noaa.gov:8380/thredds3/dodsC/ipcc_ar4_CM2.0_R1_20C3M-0_monthly_atmos_18610101-20001231";
        String2.log("\n*** EDDGridFromDap.testGenerateDatasetsXml");

String expected1 = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_gfdl_ef1f_43bf_0c26\" active=\"true\">\n" +
"    <sourceUrl>http://data1.gfdl.noaa.gov:8380/thredds3/dodsC/ipcc_ar4_CM2.0_R1_20C3M-0_monthly_atmos_18610101-20001231</sourceUrl>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"cmor_version\" type=\"float\">1.2</att>\n" +
"        <att name=\"comment\">GFDL experiment name = CM2Q-d2_1861-2000-AllForc_h1. PCMDI experiment name = 20C3M (run1). Initial conditions for this experiment were taken from 1 January of year 1 of the 1860 control model experiment named CM2Q_Control-1860_d2. Several forcing agents varied during the 140 year duration of the CM2Q-d2_1861-2000-AllForc_h1 experiment in a manner based upon observations and reconstructions for the late 19th and 20th centuries. The time varying forcing agents were atmospheric CO2, CH4, N2O, halons, tropospheric and stratospheric O3, anthropogenic tropospheric sulfates, black and organic carbon, volcanic aerosols, solar irradiance, and the distribution of land cover types. The direct effect of tropospheric aerosols is calculated by the model, but not the indirect effects.</att>\n" +
"        <att name=\"contact\">GFDL.Climate.Model.Info@noaa.gov</att>\n" +
"        <att name=\"Conventions\">CF-1.0</att>\n" +
"        <att name=\"experiment_id\">climate of the 20th Century experiment (20C3M)</att>\n" +
"        <att name=\"gfdl_experiment_name\">CM2Q-d2_1861-2000-AllForc_h1</att>\n" +
"        <att name=\"history\">Fri Jul 13 18:00:20 2007: ncks -O -d time,0,1199 /vftmp/757914.1.ic1.a//archive/pcmdi/data1/gfdl_cm2_0/CM2Q-d2_1861-2000-AllForc_h1/pp/atmos/ts/monthly/cl_A1.186101-200012.nc /vftmp/757914.1.ic1.a//archive/pcmdi/data1/gfdl_cm2_0/CM2Q-d2_1861-2000-AllForc_h1/pp/atmos/ts/monthly/cl_A1.186101-196012.nc\n" +
"input/atmos.186101-200012.cl.nc  At 12:12:21 on 07/11/2007, CMOR rewrote data to comply with CF standards and IPCC Fourth Assessment and US CCSP Projects requirements</att>\n" +
"        <att name=\"institution\">NOAA GFDL (US Dept of Commerce / NOAA / Geophysical Fluid Dynamics Laboratory, Princeton, NJ, USA)</att>\n" +
"        <att name=\"project_id\">IPCC Fourth Assessment and US CCSP Projects</att>\n" +
"        <att name=\"realization\" type=\"int\">1</att>\n" +
"        <att name=\"references\">The GFDL Data Portal (http://nomads.gfdl.noaa.gov/) provides access to NOAA/GFDL&#039;s publicly available model input and output data sets. From this web site one can view and download data sets and documentation, including those related to the GFDL CM2.0 model experiments run for the IPCC&#039;s 4th Assessment Report and the US CCSP.</att>\n" +
"        <att name=\"source\">GFDL_CM2.0 (2004): atmosphere: AM2 (am2p13, N45L24); ocean: OM3 (mom4p0_om3p4, tripolar360x200L50); sea ice: SIS; land: LM2; infrastructure: FMS J release</att>\n" +
"        <att name=\"table_id\">Table A1 (20 September 2004)</att>\n" +
"        <att name=\"title\">GFDL CM2.0, 20C3M (run 1) climate of the 20th Century experiment (20C3M) output for IPCC AR4 and US CCSP</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_email\">GFDL.Climate.Model.Info@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA GFDL</att>\n" +
"        <att name=\"creator_url\">http://data1.gfdl.noaa.gov/nomads/forms/deccen/</att>\n" +
"        <att name=\"infoUrl\">http://data1.gfdl.noaa.gov:8380/thredds3/dodsC/ipcc_ar4_CM2.0_R1_20C3M-0_monthly_atmos_18610101-20001231.html</att>\n" +
"        <att name=\"institution\">NOAA GFDL</att>\n" +
"        <att name=\"keywords\">20c3m, 20th, ar4, ccsp, century, climate, cm2.0, coefficient, coordinate, experiment, gfdl, hybrid, ipcc, layer, noaa, output, run, sigma</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">CF-1.6, COARDS, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"original_institution\">NOAA GFDL (US Dept of Commerce / NOAA / Geophysical Fluid Dynamics Laboratory, Princeton, NJ, USA)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">GFDL experiment name = CM2Q-d2_1861-2000-AllForc_h1. PCMDI experiment name = 20C3M (run1). Initial conditions for this experiment were taken from 1 January of year 1 of the 1860 control model experiment named CM2Q_Control-1860_d2. Several forcing agents varied during the 140 year duration of the CM2Q-d2_1861-2000-AllForc_h1 experiment in a manner based upon observations and reconstructions for the late 19th and 20th centuries. The time varying forcing agents were atmospheric CO2, CH4, N2O, halons, tropospheric and stratospheric O3, anthropogenic tropospheric sulfates, black and organic carbon, volcanic aerosols, solar irradiance, and the distribution of land cover types. The direct effect of tropospheric aerosols is calculated by the model, but not the indirect effects.</att>\n" +
"        <att name=\"title\">GFDL CM2.0, 20C3M (run 1) climate of the 20th Century experiment (20C3M) output for IPCC AR4 and US CCSP [lev]</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>lev</sourceName>\n" +
"        <destinationName>lev</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"bounds\">lev_bnds</att>\n" +
"            <att name=\"formula\">p(n,k,j,i) = a(k)*p0 + b(k)*ps(n,j,i)</att>\n" +
"            <att name=\"formula_terms\">p0 a b ps</att>\n" +
"            <att name=\"long_name\">hybrid sigma pressure coordinate</att>\n" +
"            <att name=\"positive\">down</att>\n" +
"            <att name=\"standard_name\">atmosphere_hybrid_sigma_pressure_coordinate</att>\n" +
"            <att name=\"units\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>a</sourceName>\n" +
"        <destinationName>a</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">hybrid sigma coordinate A coefficient for layer</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>b</sourceName>\n" +
"        <destinationName>b</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">hybrid sigma coordinate B coefficient for layer</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_gfdl_87e1_bdad_247d\" active=\"true\">\n";

        try {
            String results = generateDatasetsXml(true, url, 
                null, null, null, 10080, null);
            
            Test.ensureEqual(results.substring(0, expected1.length()), expected1, 
                "results=\n" + results);

            //int po = results.indexOf(expected2.substring(0, 40));
            //Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDGridFromDap",
                url, "10080"},
                false); //doIt loop?
            String gdxResults = String2.getClipboardString();
            Test.ensureEqual(gdxResults, results, 
                "Unexpected results from GenerateDatasetsXml.doIt.");

            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);   //only returns the first dataset defined in results
            Test.ensureEqual(edd.datasetID(), "noaa_gfdl_ef1f_43bf_0c26", "");
            Test.ensureEqual(edd.title(), 
                "GFDL CM2.0, 20C3M (run 1) climate of the 20th Century experiment (20C3M) output for IPCC AR4 and US CCSP [lev]", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "a, b", "");


        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    
    }


        //*** test standard examples
// idExample         = "erdAGssta8day";
// dimensionExample  = "latitude[0:10:100]";
// noHyperExample    = "sst";
// dataIndexExample  = "sst[656][0][0:100:1500][0:100:3600]";
// dataValueExample  = "sst[(1192924800)][0][(-75):100:(75)][(0):100:(360)]";
// dataTimeExample   = "sst[(2007-10-21T00:00:00)][0][(-75):100:(75)][(0):100:(360)]";
// graphExample      = "sst[(2007-07-01):(2007-10-21)][0][(29)][(225)]";
// mapExample        = "sst[(2007-10-21)][0][(-75):(75)][(0):(180)]";  

    
    public static void testScaleAddOffset() throws Throwable {
        //tests of scale_factor/scaleFactor and add_offset/addOffset
        //and tests of _FillValue with no missing_value
        testVerboseOn();
        int tPo;
        try {
            EDDGrid amsr = (EDDGridFromDap)oneFromDatasetXml("ncdcOisst2AmsrAgg"); //should work
            String tName = amsr.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, amsr.className() + "amsr", ".das"); 
            String results = new String((new ByteArray(
                EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            String expected = 
"sst {\n" +
"    Float32 _FillValue -9.99;\n" +      //note affected by scaleFactor
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Daily Sea Surface Temperature\";\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"degree_C\";\n" +
"    Float32 valid_max 45.0;\n" +
"    Float32 valid_min -3.0;\n" +
"  }\n" +
"  anom {\n" +
"    Float32 _FillValue -9.99;\n" +
"    Float64 colorBarMaximum 3.0;\n" +
"    Float64 colorBarMinimum -3.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Daily Sea Surface Temperature Anomalies\";\n" +
"    String standard_name \"surface_temperature_anomaly\";\n" +
"    String units \"degree_C\";\n" +
"    Float32 valid_max 12.0;\n" +
"    Float32 valid_min -12.0;\n" +
"  }\n" +
"  err {\n" +
"    Float32 _FillValue -9.99;\n" +
"    Float64 colorBarMaximum 0.6;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"WhiteRedBlack\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Estimated Error Standard Deviation of Analyzed_SST\";\n" +
"    String standard_name \"error_standard_deviation\";\n" +
"    String units \"degree_C\";\n" +
"    Float32 valid_max 10.0;\n" +
"    Float32 valid_min 0.0;\n" +
"  }\n" +
"  ice {\n" +
"    Float32 _FillValue -9.99;\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"BlackBlueWhite\";\n" +
"    String ioos_category \"Ice Distribution\";\n" +
"    String long_name \"Sea Ice Concentration\";\n" +
"    String standard_name \"sea_ice_area_fraction\";\n" +
"    String units \"fraction\";\n" +
"    Float32 valid_max 1.0;\n" +
"    Float32 valid_min 0.0;\n" +
"  }"; 
            tPo = results.indexOf("sst {");
            String tResults = results.substring(tPo, tPo + expected.length());
            Test.ensureEqual(tResults, expected, "tresults=\n" + tResults);

            //test mv as -9.99  (adjusted by scaleFactor)
            String amsrq = "sst[0][0][0:200:600][0:200:600]";
            tName = amsr.makeNewFileForDapQuery(null, null, amsrq, 
                EDStatic.fullTestCacheDirectory, amsr.className() + "amsr", ".asc"); 
            results = new String((new ByteArray(
                EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sst[time = 1][altitude = 1][latitude = 4][longitude = 4];\n" + //note Float32
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float32 altitude[altitude = 1];\n" +
"      Float32 latitude[latitude = 4];\n" +
"      Float32 longitude[longitude = 4];\n" +
"  } sst;\n" +
"} ncdcOisst2AmsrAgg;\n" +
"---------------------------------------------\n" +
"sst.sst[1][1][4][4]\n" +
"[0][0][0], -9.99, -9.99, -9.99, -9.99\n" +
"[0][0][1], 11.63, 14.87, 12.19, 17.34\n" +
"[0][0][2], -9.99, -9.99, 29.55, 28.77\n" +
"[0][0][3], 9.43, -9.99, -9.99, -9.99\n" +
"\n" +
"sst.time[1]\n" +
"1.0228896E9\n" +
"\n" +
"sst.altitude[1]\n" +
"0.0\n" +
"\n" +
"sst.latitude[4]\n" +
"-89.875, -39.875, 10.125, 60.125\n" +
"\n" +
"sst.longitude[4]\n" +
"0.125, 50.125, 100.125, 150.125\n"; 
            Test.ensureEqual(results, expected, "results=\n" + results);

            //test mv as NaN (adjusted to -9.99 by scaleFactor, then converted to NaN (then null))
            tName = amsr.makeNewFileForDapQuery(null, null, amsrq, 
                EDStatic.fullTestCacheDirectory, amsr.className() + "amsr", ".json"); 
            results = new String((new ByteArray(
                EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"altitude\", \"latitude\", \"longitude\", \"sst\"],\n" +
"    \"columnTypes\": [\"String\", \"float\", \"float\", \"float\", \"float\"],\n" +
"    \"columnUnits\": [\"UTC\", \"m\", \"degrees_north\", \"degrees_east\", \"degree_C\"],\n" +
"    \"rows\": [\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -89.875, 0.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -89.875, 50.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -89.875, 100.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -89.875, 150.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -39.875, 0.125, 11.63],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -39.875, 50.125, 14.87],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -39.875, 100.125, 12.19],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, -39.875, 150.125, 17.34],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 10.125, 0.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 10.125, 50.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 10.125, 100.125, 29.55],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 10.125, 150.125, 28.77],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 60.125, 0.125, 9.43],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 60.125, 50.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 60.125, 100.125, null],\n" +
"      [\"2002-06-01T00:00:00Z\", 0.0, 60.125, 150.125, null]\n" +
"    ]\n" +
"  }\n" +
"}\n"; 
            Test.ensureEqual(results, expected, "results=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Error accessing ncdc dataset.\n" +
                "Press ^C to stop or Enter to continue..."); 
        }

    }     

    public static void testOneTime() throws Throwable {

        //gridDataset = (EDDGridFromDap)oneFromDatasetXml("pmelOscar"); 
        //if (true) System.exit(0);

        //one time stuff
        //gridDataset = (EDDGridFromDap)oneFromDatasetXml("ncdcOisstAmsrAgg"); 
        //tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
        //    gridDataset.className() + "ncdc", ".das"); 
        //String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);

        //soda
        EDDGrid gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdSoda202d"); //should work
        String tName = gridDataset.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_soda202d", ".das"); 
        String results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        String sodaq = "time[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, sodaq, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_soda202dqt", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        sodaq =
"temp[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
//+ ",salt[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]" 
//+ ",u[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]" 
//+ ",v[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]" 
//+ ",w[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]" 
//+ ",utrans[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]" 
//+ ",vtrans[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]" 
//+ ",CFC11[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
;
//The error: 	GridDataAccessor.increment: partialResults[0] was not as expected. 
//The other primitiveArray has a different value #0 (1008374400 != 623)
        tName = gridDataset.makeNewFileForDapQuery(null, null, sodaq, 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_soda202dq", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);


        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdSoda202s"); //should work
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_soda202s", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdSoda203d"); //should work
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_soda203d", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdSoda203s"); //should work
        tName = gridDataset.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_soda203s", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

    }

    /**
     * THIS TEST IS NOT ACTIVE BECAUSE DAPPER IS NO LONGER ACTIVE.
     * This does important tests with pmelOscar (which has descending lat axis values AND
     * wierd rage: longitude is 20 .. 419!!!).
     *
     * @throws Throwable if trouble
     */
    public static void testPmelOscar(boolean doGraphicsTests) throws Throwable {
        String2.log("\n*** test for pmelOscar");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        try {
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("pmelOscar"); 
        EDVGridAxis edvga;

        //***test some edvga things
        //time commented out: it changes too often
        //EDVGridAxis edvga = eddGrid.axisVariables()[0];  
        //Test.ensureEqual(edvga.isEvenlySpaced(), false, "");
        //Test.ensureEqual(edvga.averageSpacing(), 438329.1311754685, "");  //changes sometimes
        //Test.ensureEqual(edvga.spacingDescription(), "5 days 01:45:29 (uneven)", "");

        edvga = eddGrid.axisVariables()[1];
        Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
        Test.ensureEqual(edvga.averageSpacing(), Double.NaN, "");
        Test.ensureEqual(edvga.spacingDescription(), "(just one value)", "");

        edvga = eddGrid.axisVariables()[2];
        Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
        Test.ensureEqual(edvga.averageSpacing(), -1, "");  
        Test.ensureEqual(edvga.spacingDescription(), "-1.0 (even)", "");

        //time
        results = eddGrid.axisVariables[0].sliderCsvValues();
        expected = "\"1992-10-21\", \"1992-11-21\", \"1992-12-21\", \"1993-01-21\",";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);
        //last values and sliderNCsvValues changes frequently

        //alt
        results = eddGrid.axisVariables[1].sliderCsvValues();
        expected = "-15";
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureEqual(eddGrid.axisVariables[1].sliderNCsvValues(), 1, "");

        //lat
        results = eddGrid.axisVariables[2].sliderCsvValues();
        expected = 
"69.5, 68.5, 67.5, 66.5, 65.5, 64.5, 63.5, 62.5, 61.5, 60.5, 59.5, 58.5, 57.5, 56.5, " +
"55.5, 54.5, 53.5, 52.5, 51.5, 50.5, 49.5, 48.5, 47.5, 46.5, 45.5, 44.5, 43.5, 42.5, " +
"41.5, 40.5, 39.5, 38.5, 37.5, 36.5, 35.5, 34.5, 33.5, 32.5, 31.5, 30.5, 29.5, 28.5, " +
"27.5, 26.5, 25.5, 24.5, 23.5, 22.5, 21.5, 20.5, 19.5, 18.5, 17.5, 16.5, 15.5, 14.5, " +
"13.5, 12.5, 11.5, 10.5, 9.5, 8.5, 7.5, 6.5, 5.5, 4.5, 3.5, 2.5, 1.5, 0.5, -0.5, -1.5, " +
"-2.5, -3.5, -4.5, -5.5, -6.5, -7.5, -8.5, -9.5, -10.5, -11.5, -12.5, -13.5, -14.5, -15.5, " +
"-16.5, -17.5, -18.5, -19.5, -20.5, -21.5, -22.5, -23.5, -24.5, -25.5, -26.5, -27.5, -28.5, " +
"-29.5, -30.5, -31.5, -32.5, -33.5, -34.5, -35.5, -36.5, -37.5, -38.5, -39.5, -40.5, -41.5, " +
"-42.5, -43.5, -44.5, -45.5, -46.5, -47.5, -48.5, -49.5, -50.5, -51.5, -52.5, -53.5, -54.5, " +
"-55.5, -56.5, -57.5, -58.5, -59.5, -60.5, -61.5, -62.5, -63.5, -64.5, -65.5, -66.5, -67.5, -68.5, -69.5"; 
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureEqual(eddGrid.axisVariables[2].sliderNCsvValues(), 140, "");

        //lon
        results = eddGrid.axisVariables[3].sliderCsvValues();
        expected = "20.5, 22.5, 24.5, 26.5,";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);
        expected = "414.5, 416.5, 418.5, 419.5";
        Test.ensureEqual(results.substring(results.length() - expected.length()), 
            expected, "results=\n" + results);
        Test.ensureEqual(eddGrid.axisVariables[3].sliderNCsvValues(), 201, "");


        //.das     das isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String lastTime = "" +  eddGrid.axisVariables[eddGrid.timeIndex].lastDestinationValue();
        String lastTimeString = ((EDVTimeGridAxis)eddGrid.axisVariables[eddGrid.timeIndex]).destinationMaxString();
        lastTime = String2.replaceAll(lastTime, "E", "e+");
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 7.196256e+8, " + lastTime + ";\n" + //stop time changes periodically
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float32 actual_range -15.0, -15.0;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 69.5, -69.5;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 20.5, 419.5;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  u {\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String generic_name \"u\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Ocean Surface Zonal Currents\";\n" +
"    Float32 missing_value NaN;\n" +
"    String name \"u\";\n" +
"    String standard_name \"eastward_sea_water_velocity\";\n" +
"    String units \"m s-1\";\n" +
"  }\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

        expected = 
"    String DATASUBTYPE \"unfiltered\";\n" +
"    String DATATYPE \"5-Day Interval\";\n" +
"    String date \"27-Apr-2007\";\n" +
"    String description \"Sea Surface Velocity\";\n" +
"    Float64 Easternmost_Easting 419.5;\n" +
"    Float64 geospatial_lat_max 69.5;\n" +
"    Float64 geospatial_lat_min -69.5;\n" +
"    Float64 geospatial_lat_resolution 1.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 419.5;\n" +
"    Float64 geospatial_lon_min 20.5;\n" +
"    Float64 geospatial_lon_resolution 1.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max -15.0;\n" +
"    Float64 geospatial_vertical_min -15.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        int tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) 
            String2.log("results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);
        
//        + " http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always non-https url
//            "/griddap/pmelOscar.das\";\n" +
expected = 
    "String infoUrl \"http://www.oscar.noaa.gov/\";\n" +
"    String institution \"NOAA PMEL\";\n" +
"    String keywords \"Oceans > Ocean Circulation > Ocean Currents,\n" +
"analyses, anomaly, circulation, current, currents, eastward, eastward_sea_water_velocity, meridional, noaa, northward, northward_sea_water_velocity, ocean, oceans, oscar, pmel, real, real time, sea, seawater, surface, time, velocity, water, zonal\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 69.5;\n" +
"    String reference1 \"Bonjean F. and G.S.E. Lagerloef, 2002, \\\"Diagnostic model and analysis of the surface currents in the tropical Pacific ocean\\\", J. Phys. Oceanogr., 32, 2,938-2,954\";\n" +
"    String source \"Gary Lagerloef, ESR (lager@esr.org) and Fabrice Bonjean (bonjean@esr.org)\";\n" +
"    String sourceUrl \"http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\";\n" +
"    Float64 Southernmost_Northing -69.5;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"This project is developing a processing system and data center to provide operational ocean surface velocity fields from satellite altimeter and vector wind data. The method to derive surface currents with satellite altimeter and scatterometer data is the outcome of several years NASA sponsored research. The project will transition that capability to operational oceanographic applications. The end product is velocity maps updated daily, with a goal for eventual 2-day maximum delay from time of satellite measurement. Grid resolution is 100 km for the basin scale, and finer resolution in the vicinity of the Pacific Islands.\";\n" +
"    String time_coverage_end \"" + lastTimeString + "\";\n" +
"    String time_coverage_start \"1992-10-21T00:00:00Z\";\n" +
"    String title \"OSCAR - Ocean Surface Current Analyses, Real-Time\";\n" +
"    String VARIABLE \"Ocean Surface Currents\";\n" +
"    Float32 version 2006.0;\n" +
"    Float64 Westernmost_Easting 20.5;\n" +
"  }\n" +
"}\n";
        tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) 
            String2.log("results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);


        //.dds     dds isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        int tnTime = eddGrid.axisVariables[eddGrid.timeIndex].sourceValues().size();
        expected = 
"Dataset {\n" +
"  Float64 time[time = " + tnTime + "];\n" +    //changes periodically
"  Float32 altitude[altitude = 1];\n" +
"  Float32 latitude[latitude = 140];\n" +
"  Float32 longitude[longitude = 400];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 u[time = " + tnTime + "][altitude = 1][latitude = 140][longitude = 400];\n" +
"    MAPS:\n" +
"      Float64 time[time = " + tnTime + "];\n" +
"      Float32 altitude[altitude = 1];\n" +
"      Float32 latitude[latitude = 140];\n" +
"      Float32 longitude[longitude = 400];\n" +
"  } u;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 v[time = " + tnTime + "][altitude = 1][latitude = 140][longitude = 400];\n" +
"    MAPS:\n" +
"      Float64 time[time = " + tnTime + "];\n" +
"      Float32 altitude[altitude = 1];\n" +
"      Float32 latitude[latitude = 140];\n" +
"      Float32 longitude[longitude = 400];\n" +
"  } v;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 u_anom[time = " + tnTime + "][altitude = 1][latitude = 140][longitude = 400];\n" +
"    MAPS:\n" +
"      Float64 time[time = " + tnTime + "];\n" +
"      Float32 altitude[altitude = 1];\n" +
"      Float32 latitude[latitude = 140];\n" +
"      Float32 longitude[longitude = 400];\n" +
"  } u_anom;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 v_anom[time = " + tnTime + "][altitude = 1][latitude = 140][longitude = 400];\n" +
"    MAPS:\n" +
"      Float64 time[time = " + tnTime + "];\n" +
"      Float32 altitude[altitude = 1];\n" +
"      Float32 latitude[latitude = 140];\n" +
"      Float32 longitude[longitude = 400];\n" +
"  } v_anom;\n" +
"} pmelOscar;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.nc lat values subset (subset correct? actual_range correct?)
        userDapQuery = "latitude[(69.5):10:(-69.5)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "Lat", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        String2.log(results);
        expected = //note actual range is low to high
"netcdf EDDGridFromDapLat.nc {\n" +
" dimensions:\n" +
"   latitude = 14;\n" +   // (has coord.var)\n" +  //changed when switched to netcdf-java 4.0, 2009-02-23
" variables:\n" +
"   float latitude(latitude=14);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = -60.5f, 69.5f; // float\n" +
"     :axis = \"Y\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"\n" +
" :ANOM_MEAN_PERIOD = \"1993-01-01 to 2006-12-31\";\n" +
" :cdm_data_type = \"Grid\";\n" +
" :company = \"Earth & Space Research, Seattle, WA\";\n" +
" :contact = \"Fabrice Bonjean (bonjean@esr.org) or John T. Gunn (gunn@esr.org)\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n";

        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        expected = //note geospatial_lat_min max;  note that internal " are not slashed, but that is ncDump's problem
" :DATASUBTYPE = \"unfiltered\";\n" +
" :DATATYPE = \"5-Day Interval\";\n" +
" :date = \"27-Apr-2007\";\n" +
" :description = \"Sea Surface Velocity\";\n" +
" :geospatial_lat_max = 69.5f; // float\n" +
" :geospatial_lat_min = -60.5f; // float\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :history = \"";
        tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) 
            String2.log("results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);

//note geospatial_lat_min max;  note that internal " are not slashed, but that is ncDump's problem
//today + " http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always non-https url

expected = 
    "/griddap/pmelOscar.nc?latitude[(69.5):10:(-69.5)]\";\n" +
" :infoUrl = \"http://www.oscar.noaa.gov/\";\n" +
" :institution = \"NOAA PMEL\";\n" +
" :keywords = \"Oceans > Ocean Circulation > Ocean Currents,\n" +
"analyses, anomaly, circulation, current, currents, eastward, eastward_sea_water_velocity, meridional, noaa, northward, northward_sea_water_velocity, ocean, oceans, oscar, pmel, real, real time, sea, seawater, surface, time, velocity, water, zonal\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :Northernmost_Northing = 69.5f; // float\n" +
" :reference1 = \"Bonjean F. and G.S.E. Lagerloef, 2002, \\\"Diagnostic model and analysis of the surface currents in the tropical Pacific ocean\\\", J. Phys. Oceanogr., 32, 2,938-2,954\";\n" +
" :source = \"Gary Lagerloef, ESR (lager@esr.org) and Fabrice Bonjean (bonjean@esr.org)\";\n" +
" :sourceUrl = \"http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\";\n" +
" :Southernmost_Northing = -60.5f; // float\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :summary = \"This project is developing a processing system and data center to provide operational ocean surface velocity fields from satellite altimeter and vector wind data. The method to derive surface currents with satellite altimeter and scatterometer data is the outcome of several years NASA sponsored research. The project will transition that capability to operational oceanographic applications. The end product is velocity maps updated daily, with a goal for eventual 2-day maximum delay from time of satellite measurement. Grid resolution is 100 km for the basin scale, and finer resolution in the vicinity of the Pacific Islands.\";\n" +
" :title = \"OSCAR - Ocean Surface Current Analyses, Real-Time\";\n" +
" :VARIABLE = \"Ocean Surface Currents\";\n" +
" :version = 2006.0f; // float\n" +
" data:\n" +
"latitude =\n" +
"  {69.5, 59.5, 49.5, 39.5, 29.5, 19.5, 9.5, -0.5, -10.5, -20.5, -30.5, -40.5, -50.5, -60.5}\n" +
"}\n";
        tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) 
            String2.log("results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);

        //.csv data 
        userDapQuery = "u[0][0][(69.5):10:(-69.5)][0]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = 
"time,altitude,latitude,longitude,u\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"1992-10-21T00:00:00Z,-15.0,69.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,59.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,49.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,39.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,29.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,19.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,9.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,-0.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,-10.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,-20.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,-30.5,20.5,NaN\n" +
"1992-10-21T00:00:00Z,-15.0,-40.5,20.5,0.08152205\n" +
"1992-10-21T00:00:00Z,-15.0,-50.5,20.5,0.17953366\n" +
"1992-10-21T00:00:00Z,-15.0,-60.5,20.5,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String mapDapQuery = 
            "u[(2008-08-06T00:00:00Z)][][][]" +
            "&.draw=surface&.vars=longitude|latitude|u&.colorBar=|C|Linear|||&.land=under";
        
        tName = eddGrid.makeNewFileForDapQuery(null, null, mapDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_Map", ".png"); 
        if (doGraphicsTests) SSR.displayInBrowser(
            "file://" + EDStatic.fullTestCacheDirectory + tName);

        String transparentQuery = 
            "u[(2008-08-06T00:00:00Z)][][][]" +
            "&.draw=surface&.vars=longitude|latitude|u&.colorBar=|C|Linear|||&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, transparentQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_Transparent", ".transparentPng"); 
        if (doGraphicsTests) SSR.displayInBrowser(
            "file://" + EDStatic.fullTestCacheDirectory + tName);

        String query180 = 
            "u[(2008-08-06T00:00:00Z)][][][0:(179)]" +
            "&.draw=surface&.vars=longitude|latitude|u&.colorBar=|C|Linear|||&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, query180, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_Map", ".kml"); 
        if (doGraphicsTests) SSR.displayInBrowser(
            "file://" + EDStatic.fullTestCacheDirectory + tName);

//currently doesn't work, either ERDDAP needs to rearrange lat and lon values        
//  or GeotiffWritter needs to accept descending axis values.
//        tName = eddGrid.makeNewFileForDapQuery(null, null, query180, EDStatic.fullTestCacheDirectory, 
//            eddGrid.className() + "_Map", ".geotif"); 
//        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "u[(2008-08-06T00:00:00Z)][][0:50:(last)][0:50:(179)]", 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_Map", ".esriAscii"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
//It seems to toggle frequently between these similar results ...
//changed slightly 2008-09-07! and 2008-09-24  2008-10-09 2008-10-13 2008-11-11

        expected= 
"ncols 4\n" +
"nrows 3\n" +
"xllcenter 20.5\n" +
"yllcenter -30.5\n" +
"cellsize 50.0\n" +
"nodata_value -9999999\n" +
"-9999999 -9999999 -9999999 -9999999\n";
//"-9999999 0.085097924 0.032853972 0.16006929\n" +
//"-9999999 -0.009836693 -9999999 -0.047798216\n";

//and infinite variants of last 2 lines:
//"-9999999 0.08509792 0.032853972 0.16006929\n" +
//"-9999999 -0.009836693 -9999999 -0.047798216\n";

//"-9999999 0.085097924 0.03285397 0.16006929\n" +
//"-9999999 -0.009836693 -9999999 -0.047798216\n";

//"-9999999 0.085097924 0.032853976 0.1600693\n" +
//"-9999999 -0.009836693 -9999999 -0.047798216\n";

//"-9999999 0.08509793 0.032853976 0.1600693\n" +
//"-9999999 -0.009836693 -9999999 -0.047798216\n";

//"-9999999 0.08509792 0.032853976 0.1600693\n" +
//"-9999999 -0.009836693 -9999999 -0.047798216\n";

//but on 2009-02-04 it changed to 
//-9999999 0.07428373 -9999999 0.18457247
//-9999999 0.0044357306 -9999999 -0.05290417

        //I gave up testing: it changes too often.
        //Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        //Test.ensureTrue(results.indexOf(" 0.085097") > 0, "\nresults=\n" + results);
        //Test.ensureTrue(results.indexOf(" 0.032853") > 0, "\nresults=\n" + results);
        //Test.ensureTrue(results.indexOf(" 0.160069") > 0, "\nresults=\n" + results);
        //Test.ensureTrue(results.indexOf("-0.00983669") > 0, "\nresults=\n" + results);
        //Test.ensureTrue(results.indexOf("-0.0477982") > 0, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error accessing " + EDStatic.erddapUrl + //in tests, always non-https url
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * This does some tests for Ellyn (not usually run).
     *
     * @throws Throwable if trouble
     */
    public static void testForEllyn() throws Throwable {
        String2.log("\n*** test for Ellyn");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";

        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("mb-7201adc"); 

        //.das     das isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0673667e+9, 1.0747107e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 epic_code 624;\n" +
"    String FORTRAN_format \"F10.2\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float32 actual_range -9.452215, -0.9522152;\n" +
"    String axis \"Z\";\n" +
"    Float64 bin_size 0.5;\n" +
"    Float64 blanking_distance 0.4399999976158142;\n" +
"    Int32 epic_code 3;\n" +
"    String FORTRAN_format \"F10.2\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String NOTE \"Depth values were calculated using Surface.exe output\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"m\";\n" +
"    Float64 xducer_offset_from_bottom 1.2599999904632568;\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 33.6496, 33.6496;\n" +
"    String axis \"Y\";\n" +
"    Int32 epic_code 500;\n" +
"    String FORTRAN_format \"F10.2\";\n" +
"    String generic_name \"lat\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String name \"LAT\";\n" +
"    String standard_name \"latitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -78.7894, -78.7894;\n" +
"    String axis \"X\";\n" +
"    Int32 epic_code 502;\n" +
"    String FORTRAN_format \"f10.4\";\n" +
"    String generic_name \"lon\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String name \"LON\";\n" +
"    String standard_name \"longitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  u_1205 {\n" +
"    Float32 _FillValue 1.0E35;\n" +
"    Float64 bins_questioned 17.0, 18.0, 19.0;\n" +
"    Float64 bins_std_dev_threshold_for_fill 2.0, 0.5, 0.0;\n" +
"    Int32 epic_code 1205;\n" +
"    String generic_name \"u\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Eastward Velocity (cm/s)\";\n" +
"    Float32 maximum 15.260121, 15.359408, 17.115356, 20.168367, 22.383686, 25.54994, 28.047798, 29.958483, 31.26962, 32.359756, 33.601105, 34.743168, 37.12874, 37.92423, 38.83359, 40.78402, 38.88334, 35.099434;\n" +
"    Float32 minimum -20.984797, -21.533491, -22.015848, -21.91846, -24.096342, -23.670574, -23.147821, -23.790667, -24.070864, -23.311993, -23.055223, -23.374365, -22.824059, -22.077686, -22.545044, -23.29182, -23.85412, -22.895117;\n" +
"    String name \"u\";\n" +
"    String NOTE \"Questionable data in bins likely contaminated by side-lobe surface reflection set to FillValue_\";\n" +
"    Float64 sensor_depth 10.452199935913086;\n" +
"    String sensor_type \"RD Instruments ADCP\";\n" +
"    Float64 serial_number 159.0;\n" +
"    String standard_name \"eastward_current_velocity\";\n" +
"    String units \"cm/s\";\n" +
"    Float64 valid_range -1000.0, 1000.0;\n" +
"  }\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        expected = 
"  PGd_1203 {\n" +
"    Float32 _FillValue 1.0E35;\n" +
"    Int32 epic_code 1203;\n" +
"    String generic_name \"PGd\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Percent Good Pings\";\n" +
"    Float32 maximum 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0;\n" +
"    Float32 minimum 94.25, 50.0, 50.0, 50.0, 50.0, 50.0, 39.5, 34.5, 32.75, 32.5, 32.25, 32.5, 32.0, 31.75, 31.5, 31.75, 27.0, 41.25;\n" +
"    String name \"PGd\";\n" +
"    Float64 sensor_depth 10.452199935913086;\n" +
"    String sensor_type \"RD Instruments ADCP\";\n" +
"    Float64 serial_number 159.0;\n" +
"    String standard_name \"none\";\n" +
"    String units \"counts\";\n" +
"    Float64 valid_range 0.0, 100.0;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    Int32 ADCP_serial_number 159;\n" +
"    Int32 beam_angle 20;\n" +
"    String beam_pattern \"convex\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    Int32 COMPOSITE 0;\n" +
"    String Conventions \"CF-1.6\";\n" +
"    String COORD_SYSTEM \"GEOGRAPHIC\";\n" +
"    String CREATION_DATE \"30-Aug-2006 11:56:52\";\n" +
"    String DATA_CMNT \"additional information\";\n" +
"    String DATA_ORIGIN \"USGS WHFS Sed Trans Group\";\n" +
"    String DATA_SUBTYPE \"MOORED\";\n" +
"    String DATA_TYPE \"ADCP\";\n" +
"    String DELTA_T \"900\";\n" +
"    String Deployment_date \"28-Oct-2003\";\n" +
"    Int32 DEPTH_CONST 0;\n" +
"    String DESCRIPT \"Site 1 ADCP\";\n" +
"    Int32 DRIFTER 0;\n" +
"    Int32 ending_water_layer 5;\n" +
"    Int32 error_velocity_threshold 2000;\n" +
"    String EXPERIMENT \"Myrtle Beach\";\n" +
"    Int32 false_target_reject_values 255, 255;\n" +
"    Float64 FILL_FLAG 1.0;\n" +
"    Float32 firmware_version 16.21;\n" +
"    Int32 frequency 1200;\n" +
"    Float64 geospatial_vertical_max 9.452215;\n" +
"    Float64 geospatial_vertical_min 0.9522152;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"histories combined, fill_vals removed from coordinate vars.:WATER_DEPTH related attributes corrected.:Depth subsampled by dolly.m.:Min and max attributes on u, v, and z recalculated using recalculate_adcp_minmax_vals.m on 24-Mar-2006 10:27:10 by C. Sullivan, WHSC; Questionable velocity data in bins likely contaminated by side-lobe surface reflection set to FillValue_ by clean_adcp_bins_std.m V 1.1 on 02-Dec-2005 10:12:05 by C. Sullivan, USGS WHSC; Extra variables deleted by dolly.m.:Written to an EPIC standard data file by adcp2ep.m (version 1.1):Transformed to earth coordinates by runbm2g.m:Bins were trimmed by trimBins.m using 94% of the RDI surface output.:Transformed to earth coordinates by runbm2g.m:Bins were trimmed by trimBins.m using 94% of the RDI surface output.:Ensembles recorded pre and post deployment were trimmed by goodends.m.:The data were filtered using rdi quality control factors in runmask.m.:Converted to netCDF via MATLAB by rdi2cdf.m 3.0 10-Jan-2003\n" +
today + " http://coast-enviro.er.usgs.gov/thredds/dodsC/DATAFILES/MYRTLEBEACH/7201adc-a.nc\n" +
today + " " + EDStatic.erddapUrl + //in tests, always non-https url
            "/griddap/mb-7201adc.das\";\n" +
"    String infoUrl \"http://stellwagen.er.usgs.gov/myrtlebeach.html\";\n" +
"    Float32 inst_depth 10.4522;\n" +
"    String inst_depth_note \"inst_depth = (water_depth - inst_height); nominal depth below the surface\";\n" +
"    Float32 inst_height 1.26;\n" +
"    String inst_height_note \"height in meters above bottom: accurate for tripod mounted intstruments\";\n" +
"    String INST_TYPE \"RD Instruments ADCP\";\n" +
"    String institution \"USGS/CMGP\";\n" +
"    String janus \"4 Beam\";\n" +
"    Float64 latitude 33.6496;\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 longitude -78.78939819335938;\n" +
"    Float64 magnetic_variation -8.22;\n" +
"    Int32 minmax_percent_good 0, 100;\n" +
"    String MOORING \"7201\";\n" +
"    String orientation \"UP\";\n" +
"    Int32 pings_per_ensemble 300;\n" +
"    Int32 POS_CONST 0;\n" +
"    Float32 pred_accuracy 0.4;\n" +
"    String PROJECT \"WHFC\";\n" +
"    String Recovery_date \"21-Jan-2004\";\n" +
"    String sourceUrl \"http://coast-enviro.er.usgs.gov/thredds/dodsC/DATAFILES/MYRTLEBEACH/7201adc-a.nc\";\n" +
"    String standard_name_vocabulary \"EPIC, CF-1.0\";\n" +
"    String start_time \"28-Oct-2003 18:45:00\";\n" +
"    Int32 starting_water_layer 1;\n" +
"    String stop_time \"21-Jan-2004 18:45:00\";\n" +
"    String summary \"velocity data from the ADCP on mooring 720\";\n" +
"    Float32 time_between_ping_groups 1.0;\n" +
"    String title \"South Carolina Coastal Erosion Study -adcp7201\";\n" +
"    String transform \"EARTH\";\n" +
"    Int32 transmit_pulse_length_cm 49;\n" +
"    Int32 valid_correlation_range 64, 255;\n" +
"    String VAR_DESC \"u:v:w:Werr:AGC:PGd:hght:Tx\";\n" +
"    Float64 VAR_FILL 1.0000000409184788e+35;\n" +
"    Float32 WATER_DEPTH 11.7122;\n" +
"    String WATER_DEPTH_NOTE \"from ADCP: (m) \";\n" +
"    String WATER_MASS \"?\";\n" +
"  }\n" +
"}\n";
        int tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) 
            String2.log("results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);


        //.dds     dds isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Float64 time[time = 8161];\n" +
"  Float32 altitude[altitude = 18];\n" +
"  Float32 latitude[latitude = 1];\n" +
"  Float32 longitude[longitude = 1];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 u_1205[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 8161];\n" +
"      Float32 altitude[altitude = 18];\n" +
"      Float32 latitude[latitude = 1];\n" +
"      Float32 longitude[longitude = 1];\n" +
"  } u_1205;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 v_1206[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 8161];\n" +
"      Float32 altitude[altitude = 18];\n" +
"      Float32 latitude[latitude = 1];\n" +
"      Float32 longitude[longitude = 1];\n" +
"  } v_1206;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 w_1204[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 8161];\n" +
"      Float32 altitude[altitude = 18];\n" +
"      Float32 latitude[latitude = 1];\n" +
"      Float32 longitude[longitude = 1];\n" +
"  } w_1204;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Werr_1201[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 8161];\n" +
"      Float32 altitude[altitude = 18];\n" +
"      Float32 latitude[latitude = 1];\n" +
"      Float32 longitude[longitude = 1];\n" +
"  } Werr_1201;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 AGC_1202[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 8161];\n" +
"      Float32 altitude[altitude = 18];\n" +
"      Float32 latitude[latitude = 1];\n" +
"      Float32 longitude[longitude = 1];\n" +
"  } AGC_1202;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 PGd_1203[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 8161];\n" +
"      Float32 altitude[altitude = 18];\n" +
"      Float32 latitude[latitude = 1];\n" +
"      Float32 longitude[longitude = 1];\n" +
"  } PGd_1203;\n" +
"} mb-7201adc;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   altitude values
        userDapQuery = "altitude"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "Alt", ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = 
"altitude\n" +
"m\n" +
"-9.452215\n" +
"-8.952215\n" +
"-8.452215\n" +
"-7.952215\n" +
"-7.452215\n" +
"-6.952215\n" +
"-6.452215\n" +
"-5.952215\n" +
"-5.452215\n" +
"-4.952215\n" +
"-4.452215\n" +
"-3.9522152\n" +
"-3.4522152\n" +
"-2.9522152\n" +
"-2.4522152\n" +
"-1.9522152\n" +
"-1.4522152\n" +
"-0.9522152\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv data values
        userDapQuery = "u_1205[0][0:17][0][0]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = 
"time, altitude, latitude, longitude, u_1205\n" +
"UTC, m, degrees_north, degrees_east, cm/s\n" +
"2003-10-28T18:45:00Z, -9.452215, 33.6496, -78.7894, 4.3488593\n" +
"2003-10-28T18:45:00Z, -8.952215, 33.6496, -78.7894, 5.9566345\n" +
"2003-10-28T18:45:00Z, -8.452215, 33.6496, -78.7894, 6.794666\n" +
"2003-10-28T18:45:00Z, -7.952215, 33.6496, -78.7894, 8.219185\n" +
"2003-10-28T18:45:00Z, -7.452215, 33.6496, -78.7894, 8.064997\n" +
"2003-10-28T18:45:00Z, -6.952215, 33.6496, -78.7894, 8.445462\n" +
"2003-10-28T18:45:00Z, -6.452215, 33.6496, -78.7894, 9.1305\n" +
"2003-10-28T18:45:00Z, -5.952215, 33.6496, -78.7894, 8.146257\n" +
"2003-10-28T18:45:00Z, -5.452215, 33.6496, -78.7894, 10.3772335\n" +
"2003-10-28T18:45:00Z, -4.952215, 33.6496, -78.7894, 9.835191\n" +
"2003-10-28T18:45:00Z, -4.452215, 33.6496, -78.7894, 8.704758\n" +
"2003-10-28T18:45:00Z, -3.9522152, 33.6496, -78.7894, 6.0494637\n" +
"2003-10-28T18:45:00Z, -3.4522152, 33.6496, -78.7894, 4.7303987\n" +
"2003-10-28T18:45:00Z, -2.9522152, 33.6496, -78.7894, 3.260833\n" +
"2003-10-28T18:45:00Z, -2.4522152, 33.6496, -78.7894, 1.9206865\n" +
"2003-10-28T18:45:00Z, -1.9522152, 33.6496, -78.7894, 1.6506728\n" +
"2003-10-28T18:45:00Z, -1.4522152, 33.6496, -78.7894, 1.1185195\n" +
"2003-10-28T18:45:00Z, -0.9522152, 33.6496, -78.7894, NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


    }

    /** 
     * This tests that the query parser is properly dealing with altitude 
     * metersPerSourceUnit = -1 (which makes ascending values into descending).
     * Originally, Dave found a specific problem related to .mat files that had 
     * already been fixed but not released. 
     */
    public static void testMetersPerSourceUnit() throws Throwable {
        testVerboseOn();                                        //soda 2.2.4
        EDDGrid gridDataset = (EDDGridFromDap)oneFromDatasetXml("hawaii_d90f_20ee_c4cb"); 
        String query = 
            //Dave had (-500):(-5.01) which succeeded for .htmlTable but failed for .mat 
            //but I had already fixed/cleaned up erddap's handling of descending axis vars 
            //(including altitude axes with negative metersPerSourceUnit
            //so reformed request with (-500):(-5.01) passes .htmlTable and .mat
            "temp[(2001-12-15T00:00:00)][(-5.01):(-500)][(23.1)][(185.2)]," + 
            "salt[(2001-12-15T00:00:00)][(-5.01):(-500)][(23.1)][(185.2)]," + 
               "u[(2001-12-15T00:00:00)][(-5.01):(-500)][(23.1)][(185.2)]," + 
               "v[(2001-12-15T00:00:00)][(-5.01):(-500)][(23.1)][(185.2)]," + 
               "w[(2001-12-15T00:00:00)][(-5.01):(-500)][(23.1)][(185.2)]";

        String tName = gridDataset.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_soda224", ".htmlTable"); //was ok
        String results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String expected = 
EDStatic.startHeadHtml(EDStatic.erddapUrl((String)null), "EDDGridFromDap_soda224") + "\n" +
"</head>\n" +
EDStatic.startBodyHtml(null) + "\n" +
"&nbsp;\n" +
"<form action=\"\">\n" +
"<input type=\"button\" value=\"Back\" onClick=\"history.go(-1);return true;\">\n" +
"</form>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time\n" +
"<th>altitude\n" +
"<th>latitude\n" +
"<th>longitude\n" +
"<th>temp\n" +
"<th>salt\n" +
"<th>u\n" +
"<th>v\n" +
"<th>w\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>m\n" +
"<th>degrees_north\n" +
"<th>degrees_east\n" +
"<th>degree_C\n" +
"<th>PSU\n" +
"<th>m s-1\n" +
"<th>m s-1\n" +
"<th>m s-1\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>2001-12-15T00:00:00Z\n" +
"<td nowrap align=\"right\">-5.0\n" +
"<td align=\"right\">23.25\n" +
"<td align=\"right\">185.25\n" +
"<td align=\"right\">26.7815\n" +
"<td align=\"right\">35.205196\n" +
"<td nowrap align=\"right\">-0.16983111\n" +
"<td align=\"right\">0.11358413\n" +
"<td nowrap align=\"right\">2.099171E-10\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>2001-12-15T00:00:00Z\n" +
"<td nowrap align=\"right\">-15.0\n" +
"<td align=\"right\">23.25\n" +
"<td align=\"right\">185.25\n" +
"<td align=\"right\">26.77543\n" +
"<td align=\"right\">35.205135\n" +
"<td nowrap align=\"right\">-0.15841055\n" +
"<td align=\"right\">0.11168823\n" +
"<td nowrap align=\"right\">-6.394319E-7\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>2001-12-15T00:00:00Z\n" +
"<td nowrap align=\"right\">-25.0\n" +
"<td align=\"right\">23.25\n" +
"<td align=\"right\">185.25\n" +
"<td align=\"right\">26.774588\n" +
"<td align=\"right\">35.205017\n" +
"<td nowrap align=\"right\">-0.15311892\n" +
"<td align=\"right\">0.10998611\n" +
"<td nowrap align=\"right\">-1.3381572E-6\n" +
"</tr>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
     
        tName = gridDataset.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_soda224", ".mat");  //threw an exception
        results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
        expected = 
"4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
"69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
"20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
"6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
//"2c 20 43 72 65 61 74 65   64 20 6f 6e 3a 20 57 65   , Created on: We |\n" +
//"64 20 44 65 63 20 31 34   20 30 39 3a 30 38 3a 30   d Dec 14 09:08:0 |\n" +
//"32 20 32 30 31 31 20 20   20 20 20 20 20 20 20 20   2 2011           |\n" +
"20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
"00 00 00 0e 00 00 05 d0   00 00 00 06 00 00 00 08                    |\n" +
"00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 15                    |\n" +
"68 61 77 61 69 69 5f 64   39 30 66 5f 32 30 65 65   hawaii_d90f_20ee |\n" +
"5f 63 34 63 62 00 00 00   00 04 00 05 00 00 00 20   _c4cb            |\n" +
"00 00 00 01 00 00 01 20   74 69 6d 65 00 00 00 00           time     |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   61 6c 74 69 74 75 64 65           altitude |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   6c 61 74 69 74 75 64 65           latitude |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   6c 6f 6e 67 69 74 75 64           longitud |\n" +
"65 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00   e                |\n" +
"00 00 00 00 00 00 00 00   74 65 6d 70 00 00 00 00           temp     |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   73 61 6c 74 00 00 00 00           salt     |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   75 00 00 00 00 00 00 00           u        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   76 00 00 00 00 00 00 00           v        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   77 00 00 00 00 00 00 00           w        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 00 00 00 00 00   00 00 00 0e 00 00 00 38                  8 |\n" +
"00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 08                    |\n" +
"41 ce 0d 49 40 00 00 00   00 00 00 0e 00 00 00 c8   A  I@            |\n" +
"00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 13 00 00 00 01                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 98                    |\n" +
"c0 14 00 00 00 00 00 00   c0 2e 00 00 00 00 00 00            .       |\n" +
"c0 39 00 00 00 00 00 00   c0 41 80 00 00 00 00 00    9       A       |\n" +
"c0 47 00 00 00 00 00 00   c0 4c 80 00 00 00 00 00    G       L       |\n" +
"c0 51 80 00 00 00 00 00   c0 54 80 00 00 00 00 00    Q       T       |\n" +
"c0 58 00 00 00 00 00 00   c0 5c 00 00 00 00 00 00    X       \\       |\n" +
"c0 60 20 00 00 00 00 00   c0 62 80 00 00 00 00 00    `       b       |\n" +
"c0 65 60 00 00 00 00 00   c0 68 a0 00 00 00 00 00    e`      h       |\n" +
"c0 6c a0 00 00 00 00 00   c0 70 c0 00 00 00 00 00    l       p       |\n" +
"c0 73 d0 00 00 00 00 00   c0 77 d0 00 00 00 00 00    s       w       |\n" +
"c0 7d 10 00 00 00 00 00   00 00 00 0e 00 00 00 38    }             8 |\n" +
"00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 08                    |\n" +
"40 37 40 00 00 00 00 00   00 00 00 0e 00 00 00 38   @7@            8 |\n" +
"00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 08                    |\n" +
"40 67 28 00 00 00 00 00   00 00 00 0e 00 00 00 88   @g(              |\n" +
"00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 4c   41 d6 40 83 41 d6 34 15          LA @ A 4  |\n" +
"41 d6 32 5b 41 d6 30 7f   41 d6 1d d9 41 d5 ff 8f   A 2[A 0 A   A    |\n" +
"41 d2 f0 89 41 c9 67 76   41 bd fa 78 41 b3 48 12   A   A gvA  xA H  |\n" +
"41 ab 5f 71 41 a3 67 76   41 9b 4a e6 41 91 de 8e   A _qA gvA J A    |\n" +
"41 86 eb e9 41 71 78 64   41 53 c4 80 41 32 55 8b   A   AqxdAS  A2U  |\n" +
"41 10 98 26 00 00 00 00   00 00 00 0e 00 00 00 88   A  &             |\n" +
"00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 4c   42 0c d2 1f 42 0c d2 0f          LB   B    |\n" +
"42 0c d1 f0 42 0c d1 d0   42 0c d1 6e 42 0c d0 70   B   B   B  nB  p |\n" +
"42 0c b3 32 42 0c 5d 0d   42 0c 0b 17 42 0b d7 38   B  2B ] B   B  8 |\n" +
"42 0b cb 34 42 0b db ba   42 0b fc 25 42 0b fe 8b   B  4B   B  %B    |\n" +
"42 0b c7 32 42 0b 41 83   42 0a 8c 40 42 09 bf e2   B  2B A B  @B    |\n" +
"42 09 47 9b 00 00 00 00   00 00 00 0e 00 00 00 88   B G              |\n" +
"00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 4c   be 2d e8 35 be 22 36 60          L - 5 \"6` |\n" +
"be 1c cb 35 be 19 83 74   be 18 45 db be 19 84 17      5   t  E      |\n" +
"be 1e 2a a2 be 21 e7 7e   be 23 8d 81 be 20 8d 8e     *  ! ~ #       |\n" +
"be 19 8b 40 be 10 ab 11   be 06 d6 cd bd f7 80 30      @           0 |\n" +
"bd de e1 53 bd c5 00 a8   bd ab 5c 7e bd 92 cf 27      S      \\~   ' |\n" +
"bd 71 15 b9 00 00 00 00   00 00 00 0e 00 00 00 88    q               |\n" +
"00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 4c   3d e8 9e cc 3d e4 bc cc          L=   =    |\n" +
"3d e1 40 66 3d dd 4d ff   3d d5 1e e8 3d ce 17 d4   = @f= M =   =    |\n" +
"3d cd ad 86 3d d5 41 f0   3d d8 e2 e0 3d d0 e4 d3   =   = A =   =    |\n" +
"3d bc e2 86 3d a2 20 e2   3d 84 f9 40 3d 4c 1f fa   =   =   =  @=L   |\n" +
"3d 0c fe b9 3c ab 92 e4   3c 35 b6 0f 3b a7 b9 78   =   <   <5  ;  x |\n" +
"3b 40 c9 df 00 00 00 00   00 00 00 0e 00 00 00 88   ;@               |\n" +
"00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 4c   2f 66 ce 69 b5 2b a5 6d          L/f i + m |\n" +
"b5 b3 9a bb b6 09 19 de   b6 38 c0 1c b6 77 29 9a            8   w)  |\n" +
"b6 96 a8 25 b6 b0 67 d7   b6 c8 ed 82 b6 e3 fd 89      %  g          |\n" +
"b7 00 9b a8 b7 0f a6 fd   b7 1e ac 0e b7 2d 56 2e                -V. |\n" +
"b7 3a e2 f3 b7 45 ef e6   b7 4d 39 fb b7 50 88 d0    :   E   M9  P   |\n" +
"b7 51 15 7f 00 00 00 00    Q                                         |\n";
        Test.ensureEqual(
            results.substring(0, 71 * 4) + results.substring(71 * 7), //remove the creation dateTime
            expected, "results=\n" + results);    

        tName = gridDataset.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "_soda224", ".asc"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 temp[time = 1][altitude = 19][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float64 altitude[altitude = 19];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"  } temp;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 salt[time = 1][altitude = 19][latitude = 1][longitude = 1];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float64 altitude[altitude = 19];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"  } salt;";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);    


        //***** tests that should fail
        query = //same as above, but altitude values are reversed
            "temp[(2001-12-15T00:00:00)][(-500):(-5.01)][(23.1)][(185.2)]," + 
            "salt[(2001-12-15T00:00:00)][(-500):(-5.01)][(23.1)][(185.2)]," + 
               "u[(2001-12-15T00:00:00)][(-500):(-5.01)][(23.1)][(185.2)]," + 
               "v[(2001-12-15T00:00:00)][(-500):(-5.01)][(23.1)][(185.2)]," + 
               "w[(2001-12-15T00:00:00)][(-500):(-5.01)][(23.1)][(185.2)]";
        expected = "SimpleException: Query error: For variable=temp axis#1=altitude Constraint=\"[(-500):(-5.01)]\": StartIndex=18 is less than StopIndex=0.";
        for (int i = 0; i < dataFileTypeNames.length; i++) {
            String fileType = dataFileTypeNames[i];
            if (String2.indexOf(new String[]{
                ".das", ".dds", ".fgdc", ".graph", ".help", ".html", ".iso19115"}, fileType) >= 0)
                continue;

            results = "";
            try {
                tName = gridDataset.makeNewFileForDapQuery(null, null, query,
                    EDStatic.fullTestCacheDirectory, 
                    gridDataset.className() + "_soda224", fileType); 
            } catch (Throwable t) {
                results = MustBe.throwableToString(t);
            }
            Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
                expected, "fileType=" + fileType + " results=\n" + results);
        }

    }

    /** This tests sliderCsvValues. */
    public static void testSliderCsv() throws Throwable {
        testVerboseOn();
        String name, tName, results, expected;
        EDDGridFromDap gridDataset;
        
        //test erdBAssta5day
        gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdBAssta5day"); 

        //time
        results = gridDataset.axisVariables[0].sliderCsvValues();
        //end dates and sliderNCsvValues changes frequently
        expected = "\"2002-07-06T12:00:00Z\", \"2002-07-21T12:00:00Z\", \"2002-08-05T12:00:00Z\", \"2002-08-20T12:00:00Z\",";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);

        //alt
        results = gridDataset.axisVariables[1].sliderCsvValues();
        expected = "0";
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureEqual(gridDataset.axisVariables[1].sliderNCsvValues(), 1, "");

        //lat
        results = gridDataset.axisVariables[2].sliderCsvValues();
        expected = "-75, -74, -73, -72, -71, -70, -69, -68, -67, -66,";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);
        expected = "64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75";
        Test.ensureEqual(results.substring(results.length() - expected.length()), 
            expected, "results=\n" + results);
        Test.ensureEqual(gridDataset.axisVariables[2].sliderNCsvValues(), 151, "");

        //lon
        results = gridDataset.axisVariables[3].sliderCsvValues();
        expected = "0, 2, 4, 6, 8, 10, 12, 14, 16, 18,";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results=\n" + results);
        expected = "350, 352, 354, 356, 358, 360";
        Test.ensureEqual(results.substring(results.length() - expected.length()), 
            expected, "results=\n" + results);
        Test.ensureEqual(gridDataset.axisVariables[3].sliderNCsvValues(), 181, "");

        //*** SEE also the oscar tests
    }


    /** This tests saveAsKml. */
    public static void testKml() throws Throwable {
        testVerboseOn();
        try {

        EDDGridFromDap gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdBAssta5day"); 
        String name, tName, results, expected;

        //overall kml
        tName = gridDataset.makeNewFileForDapQuery(null, null, 
            "sst[(2008-11-01T12:00:00Z)][][][]",
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_testKml", ".kml"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
"<Document>\n" +
"  <name>SST, Blended, Global, EXPERIMENTAL (5 Day Composite)</name>\n" +
"  <description><![CDATA[Time: 2008-11-01T12:00:00Z<br />\n" +
"Data courtesy of NOAA CoastWatch, West Coast Node<br />\n" +
"<a href=\"" + EDStatic.erddapUrl + //in tests, always non-https url
            "/griddap/erdBAssta5day.html?sst\">Download data from this dataset.</a><br />\n" +
"    ]]></description>\n" +
"  <Region>\n" +
"    <Lod><minLodPixels>2</minLodPixels></Lod>\n" +
"    <LatLonAltBox>\n" +
"      <west>0.0</west>\n" +
"      <east>360.0</east>\n" +
"      <south>-75.0</south>\n" +
"      <north>75.0</north>\n" +
"    </LatLonAltBox>\n" +
"  </Region>\n" +
"  <NetworkLink>\n" +
"    <name>1_0_0_0</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>0.0</west>\n" +
"        <east>180.0</east>\n" +
"        <south>-75.0</south>\n" +
"        <north>4.163336E-15</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
    "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(-75.0):(4.163336E-15)][(0.0):(180.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <NetworkLink>\n" +
"    <name>1_0_0_1</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>0.0</west>\n" +
"        <east>180.0</east>\n" +
"        <south>4.163336E-15</south>\n" +
"        <north>75.0</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
     "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(4.163336E-15):(75.0)][(0.0):(180.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <NetworkLink>\n" +
"    <name>1_0_0_2</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>-180.0</west>\n" +
"        <east>0.0</east>\n" +
"        <south>-75.0</south>\n" +
"        <north>4.163336E-15</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
          "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(-75.0):(4.163336E-15)][(180.0):(360.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <NetworkLink>\n" +
"    <name>1_0_0_3</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>-180.0</west>\n" +
"        <east>0.0</east>\n" +
"        <south>4.163336E-15</south>\n" +
"        <north>75.0</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
     "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(4.163336E-15):(75.0)][(180.0):(360.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <GroundOverlay>\n" +
"    <drawOrder>1</drawOrder>\n" +
"    <Icon>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
    "/griddap/erdBAssta5day.transparentPng?sst[(2008-11-01T12:00:00Z)][0][(-75.0):4:(75.0)][(0.0):4:(360.0)]</href>\n" +
"    </Icon>\n" +
"    <LatLonBox>\n" +
"      <west>0.0</west>\n" +
"      <east>360.0</east>\n" +
"      <south>-75.0</south>\n" +
"      <north>75.0</north>\n" +
"    </LatLonBox>\n" +
"  </GroundOverlay>\n" +
"  <ScreenOverlay id=\"Logo\">\n" +
"    <description>" + EDStatic.erddapUrl + //in tests, always non-https url
        "</description>\n" +
"    <name>Logo</name>\n" +
"    <Icon><href>" + EDStatic.imageDirUrl + //in tests, always non-https url
      "nlogo.gif</href></Icon>\n" +
"    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
"    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
"    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" +
"  </ScreenOverlay>\n" +
"</Document>\n" +
"</kml>\n";
        Test.ensureEqual(results, expected, "results=\n" + results);    

        //a quadrant
        tName = gridDataset.makeNewFileForDapQuery(null, null, 
            "sst[(2008-11-01T12:00:00Z)][0][(-75.0):(4.163336E-15)][(180.0):(360.0)]",
            EDStatic.fullTestCacheDirectory, gridDataset.className() + "_testKml2", ".kml"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
"<Document>\n" +
"  <name>2_1_0</name>\n" +
"  <Region>\n" +
"    <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"    <LatLonAltBox>\n" +
"      <west>-180.0</west>\n" +
"      <east>0.0</east>\n" +
"      <south>-75.0</south>\n" +
"      <north>4.163336E-15</north>\n" +
"    </LatLonAltBox>\n" +
"  </Region>\n" +
"  <NetworkLink>\n" +
"    <name>2_1_0_0</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>-180.0</west>\n" +
"        <east>-90.0</east>\n" +
"        <south>-75.0</south>\n" +
"        <north>-37.5</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
   "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(-75.0):(-37.5)][(180.0):(270.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <NetworkLink>\n" +
"    <name>2_1_0_1</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>-180.0</west>\n" +
"        <east>-90.0</east>\n" +
"        <south>-37.5</south>\n" +
"        <north>4.163336E-15</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
    "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(-37.5):(4.163336E-15)][(180.0):(270.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <NetworkLink>\n" +
"    <name>2_1_0_2</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>-90.0</west>\n" +
"        <east>0.0</east>\n" +
"        <south>-75.0</south>\n" +
"        <north>-37.5</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
      "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(-75.0):(-37.5)][(270.0):(360.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <NetworkLink>\n" +
"    <name>2_1_0_3</name>\n" +
"    <Region>\n" +
"      <Lod><minLodPixels>256</minLodPixels></Lod>\n" +
"      <LatLonAltBox>\n" +
"        <west>-90.0</west>\n" +
"        <east>0.0</east>\n" +
"        <south>-37.5</south>\n" +
"        <north>4.163336E-15</north>\n" +
"      </LatLonAltBox>\n" +
"    </Region>\n" +
"    <Link>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
     "/griddap/erdBAssta5day.kml?sst[(2008-11-01T12:00:00Z)][0][(-37.5):(4.163336E-15)][(270.0):(360.0)]</href>\n" +
"      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
"    </Link>\n" +
"  </NetworkLink>\n" +
"  <GroundOverlay>\n" +
"    <drawOrder>2</drawOrder>\n" +
"    <Icon>\n" +
"      <href>" + EDStatic.erddapUrl + //in tests, always non-https url
      "/griddap/erdBAssta5day.transparentPng?sst[(2008-11-01T12:00:00Z)][0][(-75.0):2:(4.163336E-15)][(180.0):2:(360.0)]</href>\n" +
"    </Icon>\n" +
"    <LatLonBox>\n" +
"      <west>-180.0</west>\n" +
"      <east>0.0</east>\n" +
"      <south>-75.0</south>\n" +
"      <north>4.163336E-15</north>\n" +
"    </LatLonBox>\n" +
"  </GroundOverlay>\n" +
"</Document>\n" +
"</kml>\n";
        Test.ensureEqual(results, expected, "results=\n" + results);    


        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError accessing erdBAssta5day at " + EDStatic.erddapUrl + //in tests, always non-https url
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }


    /** This tests non-nc-"Grid" data variable (dimensions don't have axis/coordinate variable). 
     */
    public static void testNoAxisVariable() throws Throwable {


        String2.log("\n*** testNoAxisVariable\n" +
            "!!!!!!  This test is inactive because the test dataset disappeared.");

        /*
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z"

        try{
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testNoAxisVariable"); 

        //.das     das isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.075464e+9, 1.075464e+9;\n" +
"    String axis \"T\";\n" +
"    String calendar \"standard\";\n" +
"    String field \"time, scalar, series\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"averaged time since initialization\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  eta_rho {\n" +
"    Int16 actual_range 0, 641;\n" +
"    String ioos_category \"Location\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  xi_rho {\n" +
"    Int16 actual_range 0, 225;\n" +
"    String ioos_category \"Location\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  zeta {\n" +
"    String coordinates \"lat_rho lon_rho\";\n" +
"    String field \"free-surface, scalar, series\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"averaged free-surface\";\n" +
"    String time \"ocean_time\";\n" +
"    String units \"meter\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String avg_base \"nep4_avg\";\n" +
"    String bry_file \"/wrkdir/kate/NEP4_djd/NEP4_bry_CCSM_2000-2004.nc\";\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

        expected = 
"String tiling \"003x016\";\n" +
"    String time_coverage_end \"2004-01-30T12:00:00Z\";\n" +
"    String time_coverage_start \"2004-01-30T12:00:00Z\";\n" +
"    String title \"ROMS/TOMS 3.0 - Northeast Pacific 10km Grid (NEP4)\";\n" +
"    String type \"ROMS/TOMS averages file\";\n" +
"    String var_info \"External/varinfo.dat\";\n" +
"  }\n" +
"}\n";
        int tpo = results.indexOf("String tiling ");
        if (tpo < 0) String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(tpo), expected, 
            "results=\n" + results);


        //.dds     dds isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Float64 time[time = 1];\n" +
"  Int16 eta_rho[eta_rho = 642];\n" +
"  Int16 xi_rho[xi_rho = 226];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 zeta[time = 1][eta_rho = 642][xi_rho = 226];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Int16 eta_rho[eta_rho = 642];\n" +
"      Int16 xi_rho[xi_rho = 226];\n" +
"  } zeta;\n" +
"} testNoAxisVariable;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv   an index values
        userDapQuery = "xi_rho[0:2:5]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "XiRho", ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"xi_rho\n" +
"count\n" +
"0\n" +
"2\n" +
"4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv data values
        userDapQuery = "zeta[0][0:2][0:2]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_NAV", ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
//from source
//  http://edac-dap2.northerngulfinstitute.org/thredds/dodsC/roms/al_roms/nep4_004.nc.ascii?zeta[0:1:0][0:1:2][0:1:2]
//Dataset {
//    Float32 zeta[ocean_time = 1][eta_rho = 3][xi_rho = 3];
//} roms/al_roms/nep4_004.nc;
//---------------------------------------------
//zeta[1][3][3]
//[0][0], 0.6276099, 0.6276099, 0.6215845
//[0][1], 0.6276099, 0.6276089, 0.6215834
//[0][2], 0.6242121, 0.62421095, 0.6181293

"time, eta_rho, xi_rho, zeta\n" +
"UTC, count, count, meter\n" +
"2004-01-30T12:00:00Z, 0, 0, 0.6276099\n" +
"2004-01-30T12:00:00Z, 0, 1, 0.6276099\n" +
"2004-01-30T12:00:00Z, 0, 2, 0.6215845\n" +
"2004-01-30T12:00:00Z, 1, 0, 0.6276099\n" +
"2004-01-30T12:00:00Z, 1, 1, 0.6276089\n" +
"2004-01-30T12:00:00Z, 1, 2, 0.6215834\n" +
"2004-01-30T12:00:00Z, 2, 0, 0.6242121\n" +
"2004-01-30T12:00:00Z, 2, 1, 0.62421095\n" +
"2004-01-30T12:00:00Z, 2, 2, 0.6181293\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //analogous query
        userDapQuery = "zeta[0][(0):(2)][(0):(2)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_NAV2", ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError in EDDGridFromDap.testNoAxisVariable." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        */
    }
     

    /** This tests a climatology time problem.   */
    public static void testClimatologyTime() throws Throwable {
        String2.log("\n*** testClimatologyTime");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;

        try {
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("ncdcOwClm9505"); 
        userDapQuery = "u[(1995-12-30)][][(22)][(225)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_clim", ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,altitude,latitude,longitude,u\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"1995-12-30T00:00:00Z,10.0,22.0,225.0,-6.749089\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }


    /** This tests accessibleTo. */
    public static void testAccessibleTo() throws Throwable {
        testVerboseOn();
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("erdBAssta5day"); 
        String roleNull[] = null;
        String roleNone[] = new String[0];
        String roleBob[] = new String[]{"bob"};
        String roleBMT[] = new String[]{"bob", "mike", "tom"};

        //test accessible = null
        eddGrid.setAccessibleTo(null);
        Test.ensureTrue(eddGrid.isAccessibleTo(roleNull), "");
        Test.ensureTrue(eddGrid.isAccessibleTo(roleNone), "");
        Test.ensureTrue(eddGrid.isAccessibleTo(roleBob), "");
        Test.ensureTrue(eddGrid.isAccessibleTo(roleBMT), "");

        //test accessible = ""
        eddGrid.setAccessibleTo("");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleNull), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleNone), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleBob), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleBMT), "");

        //test accessible = "john, tom"
        eddGrid.setAccessibleTo("john, tom");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleNull), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleNone), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleBob), "");
        Test.ensureTrue(eddGrid.isAccessibleTo(roleBMT), "");

        //test accessible = "albert, stan"
        eddGrid.setAccessibleTo("albert, stan");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleNull), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleNone), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleBob), "");
        Test.ensureTrue(!eddGrid.isAccessibleTo(roleBMT), "");
    }

    /** 
     * The request size will be ~22MB/timePoint.
     * If partialRequestMaxBytes=100000000  (10^8),
     * <br>Small request (e.g., 1,2,3,4) will be handled with one source query.
     * <br>Large request (e.g., 6+, depending on setup.xml partialRequestMaxBytes)
     * <br>  will be handled with multiple source queries (one per timePoint).
     *
     * <p>Note that THREDDS has a default limit of 500MB for opendap responses.
     * See http://www.unidata.ucar.edu/projects/THREDDS/tech/tds4.0/UpgradingTo4.0.html
     * partialRequestMaxBytes=10^8 stays well under that.
     * 
     */
    public static void testBigRequest(int nTimePoints) throws Throwable {
        testVerboseOn();
        String2.log("\n*** EDDGridFromDap.testbigRequest  partialRequestMaxBytes=" +
            EDStatic.partialRequestMaxBytes + 
            "\n nTimePoints=" + nTimePoints +
            " estimated nPartialRequests=" + 
            Math2.hiDiv(nTimePoints * 22000000, EDStatic.partialRequestMaxBytes));
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("erdBAssta5day"); 
        String dir = EDStatic.fullTestCacheDirectory;
        String tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "sst[0:" + (nTimePoints - 1) + "][][][]", 
            dir, eddGrid.className() + "_testBigRequest", ".nc"); 
        String2.log("done. size=" + File2.length(dir + tName));

        //for each time point, test that values are same from erddap tiny request or .nc file
        NetcdfFile ncFile = null; 
        try {
            ncFile = NcHelper.openFile(dir + tName);
            Variable ncVariable = ncFile.findVariable("sst");
            if (ncVariable == null)
                throw new RuntimeException("sst not found in " + dir + tName);
            int xIndex = 600, yIndex = 650, zIndex = 0;

            for (int tp = 0; tp < nTimePoints; tp++) {
                String2.log("\ntp=" + tp);

                //from new nc file:
                PrimitiveArray pa = NcHelper.get4DValues(ncVariable, 
                    xIndex, yIndex, zIndex, tp, tp);  //x,y,z,t1,t2
                String2.log("\nfrom ncFile, sst=" + pa.getFloat(0));

                //from opendap:
                String testName = eddGrid.makeNewFileForDapQuery(null, null, 
                    "sst[" + tp + ":" + tp + "][" + zIndex + "][" + yIndex + "][" + xIndex + "]", 
                    dir, eddGrid.className() + "_testBigRequestDap", ".csv"); 
                String dapResult[] = String2.readFromFile(dir + testName);
                Test.ensureEqual(dapResult[0], "", "Error reading " + dir + testName);
                String2.log("\nfrom opendap:\n" + dapResult[1]);

                //compare
                int cpo = dapResult[1].lastIndexOf(",");
                String ncTest = pa.getFloat(0) == -9999999? "NaN" : "" + pa.getFloat(0);
                String dapTest = dapResult[1].substring(cpo + 1, dapResult[1].length() - 1);
                String2.log("\ntp=" + tp + 
                            "\n   ncTest=" + ncTest +
                            "\n  dapTest=" + dapTest + "\n");
                Test.ensureEqual(ncTest, dapTest, "sst values don't match!");
            }
 
            //always
            ncFile.close();

        } catch (Throwable t) {
            //ensure:
            if (ncFile != null)
                ncFile.close();
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using testBigRequest." +
                "\nPress ^C to stop or Enter to continue..."); 
        } 

    }

    /** Test speed of Data Access Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedDAF() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDGridFromDap gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdMHchla8day"); 
        String fileName = EDStatic.fullTestCacheDirectory + "gridTestSpeedDAF.txt";
        Writer writer = new FileWriter(fileName);
        gridDataset.writeDapHtmlForm(null, "", writer);

        //time it DAF
        String2.log("start timing"); 
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)  //1000 so it dominates program run time
            gridDataset.writeDapHtmlForm(null, "", writer);
        String2.log("EDDGridFromDap.testSpeedDAF time per .html = " +
            ((System.currentTimeMillis() - time) / 1000.0) + "ms (avg=6.14ms)\n" +
            "  outputFileName=" + fileName);
        EDD.testVerbose(true);
    }

    /** Test speed of Make A Graph Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedMAG() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDGridFromDap gridDataset = (EDDGridFromDap)oneFromDatasetXml("erdMHchla8day"); 
        String fileName = EDStatic.fullTestCacheDirectory + "gridTestSpeedMAG.txt";
        OutputStreamSource oss = new OutputStreamSourceSimple(new FileOutputStream(fileName));
        gridDataset.respondToGraphQuery(null, null, "", "", oss, null, null, null);

        //time it 
        String2.log("start timing");
        long time2 = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) //1000 so it dominates program run time
            gridDataset.respondToGraphQuery(null, null, "", "", oss,
                EDStatic.fullTestCacheDirectory, "testSpeedMAG.txt", ".graph");
        String2.log("EDDGridFromDap.testSpeedMAG time per .graph = " +
            ((System.currentTimeMillis() - time2) / 1000.0) + "ms (avg=18ms)\n" +
            "  outputFileName=" + fileName);
        EDD.testVerbose(true);
    }

    /** Test quick restart */
    public static void testQuickRestart() throws Throwable {
        String2.log("\nEDDGridFromDap.testQuickRestart");
        String tDatasetID = "erdBAssta5day";

        //regular load dataset
        File2.delete(quickRestartFullFileName(tDatasetID)); //force regular load
        long time1 = System.currentTimeMillis();
        EDD edd1 = oneFromDatasetXml(tDatasetID);
        String searchString1 = String2.utf8ToString(edd1.searchBytes());
        time1 = System.currentTimeMillis() - time1;

        //try to load from quickRestartFile
        EDStatic.memoryUseLoadDatasetsSB.setLength(0); //so EDStatic.initialLoadDatasets() will be true
        Test.ensureTrue(EDStatic.initialLoadDatasets(), "");
        long time2 = System.currentTimeMillis();
        EDD edd2 = oneFromDatasetXml(tDatasetID);
        String searchString2 = String2.utf8ToString(edd2.searchBytes());
        time2 = System.currentTimeMillis() - time2;

        String2.log(
            "  regular load dataset       time=" + time1 + "\n" +
            "  quick restart load dataset time=" + time2);
        Test.ensureEqual(searchString1, searchString2, "");
    }


    /** Test getting a geotiff from a dataset with descending lat values.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testDescendingAxisGeotif() throws Throwable {
        String2.log("\n*** EDDGridFromDap.testDescendingAxisGeotif");
        EDD.testVerbose(false);
        EDDGridFromDap gridDataset = (EDDGridFromDap)oneFromDatasetXml("usgsCeCrm10"); 
        String tName = gridDataset.makeNewFileForDapQuery(null, null, "topo[][]", 
            EDStatic.fullTestCacheDirectory, "descendingAxisGeotif", ".geotif"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    }

    /** 
     * This tests addToHyraxUrlList.
     */
    public static void testAddToHyraxUrlList() throws Throwable {
        String2.log("\n*** testAddToHyraxUrlList");

        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        addToHyraxUrlList(
            "http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/", //startUrl, 
            "month_[0-9]{8}_v11l35flk\\.nc\\.gz", //fileNameRegex, 
            true, //recursive, 
            childUrls, lastModified);

        String results = childUrls.toNewlineString();
        String expected = 
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870701_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870801_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870901_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871001_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871101_v11l35flk.nc.gz\n" +
"http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871201_v11l35flk.nc.gz\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        results = lastModified.toString();
        expected = "1.336609915E9, 1.336785444E9, 1.336673639E9, 1.336196561E9, 1.336881763E9, 1.336705731E9";
        Test.ensureEqual(results, expected, "results=\n" + results);

    }

    public static void testNetcdfJava() throws Throwable {
        //open as a NetcdfDataset, not a NetcdfFile as above
        String url = "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day";
        //String url = EDStatic.erddapUrl + "/griddap/erdMHchla8day"; //in tests, always non-https url

        NetcdfDataset nc = NetcdfDataset.openDataset(url);
        String results, expected;
        Attributes attributes = new Attributes();
        try {
            results = nc.toString();
            results = NcHelper.decodeNcDump(results); //added with switch to netcdf-java 4.0
            String tUrl = String2.replaceAll(EDStatic.erddapUrl, "http:", "dods:"); //in tests, always non-https url
            expected = 
"netcdf dods" + url.substring(4) + " {\n" +
" dimensions:\n" +
"   time = 466;\n" +
"   altitude = 1;\n" +
"   latitude = 4320;\n" +
"   longitude = 8640;\n" +
" variables:\n" +
"   float chlorophyll(time=466, altitude=1, latitude=4320, longitude=8640);\n" +
"     :_CoordinateAxes = \"time altitude latitude longitude \";\n" +
"     :_FillValue = -9999999.0f; // float\n" +
"     :colorBarMaximum = 30.0; // double\n" +
"     :colorBarMinimum = 0.03; // double\n" +
"     :colorBarScale = \"Log\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 2; // int\n" +
"     :ioos_category = \"Ocean Color\";\n" +
"     :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n" +
"     :missing_value = -9999999.0f; // float\n" +
"     :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" +
"     :units = \"mg m-3\";\n" +
"   double time(time=466);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.0260864E9, 1.3519872E9; // double\n" +  //2nd number changes
"     :axis = \"T\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Centered Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   double altitude(altitude=1);\n" +
"     :_CoordinateAxisType = \"Height\";\n" +
"     :_CoordinateZisPositive = \"up\";\n" +
"     :actual_range = 0.0, 0.0; // double\n" +
"     :axis = \"Z\";\n" +
"     :fraction_digits = 0; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Altitude\";\n" +
"     :positive = \"up\";\n" +
"     :standard_name = \"altitude\";\n" +
"     :units = \"m\";\n" +
"   double latitude(latitude=4320);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = -90.0, 90.0; // double\n" +
"     :axis = \"Y\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   double longitude(longitude=8640);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = 0.0, 360.0; // double\n" +
"     :axis = \"X\";\n" +
"     :coordsys = \"geographic\";\n" +
"     :fraction_digits = 4; // int\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :point_spacing = \"even\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"\n" +
" :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
" :cdm_data_type = \"Grid\";\n" +
" :composite = \"true\";\n" +
" :contributor_name = \"NASA GSFC (OBPG)\";\n" +
" :contributor_role = \"Source of level 2 data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
" :date_created = \"2012-11-11Z\";\n" +  //changes
" :date_issued = \"2012-11-11Z\";\n" +   //changes
" :Easternmost_Easting = 360.0; // double\n" +
" :geospatial_lat_max = 90.0; // double\n" +
" :geospatial_lat_min = -90.0; // double\n" +
" :geospatial_lat_resolution = 0.041676313961565174; // double\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = 360.0; // double\n" +
" :geospatial_lon_min = 0.0; // double\n" +
" :geospatial_lon_resolution = 0.04167148975575877; // double\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_max = 0.0; // double\n" +
" :geospatial_vertical_min = 0.0; // double\n" +
" :geospatial_vertical_positive = \"up\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"NASA GSFC (OBPG)";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

            expected = 
" :satellite = \"Aqua\";\n" +
" :sensor = \"MODIS\";\n" +
" :source = \"satellite observation: Aqua, MODIS\";\n" +
" :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
" :Southernmost_Northing = -90.0; // double\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\";\n" +
" :time_coverage_end = \"2012-11-04T00:00:00Z\";\n" + //changes
" :time_coverage_start = \"2002-07-08T00:00:00Z\";\n" +
" :title = \"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\";\n" +
" :Westernmost_Easting = 0.0; // double\n" +
"}\n";
            Test.ensureEqual(results.substring(results.indexOf(" :satellite =")), expected, "RESULTS=\n" + results);

            attributes.clear();
            NcHelper.getGlobalAttributes(nc, attributes);
            Test.ensureEqual(attributes.getString("contributor_name"), "NASA GSFC (OBPG)", "");
            Test.ensureEqual(attributes.getString("keywords"), "8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn", 
                "found=" + attributes.getString("keywords"));

            //get attributes for a dimension 
            Variable ncLat = nc.findVariable("latitude");
            attributes.clear();
            NcHelper.getVariableAttributes(ncLat, attributes);
            Test.ensureEqual(attributes.getString("coordsys"), "geographic", "");
            Test.ensureEqual(attributes.get("fraction_digits"), new IntArray(new int[]{4}), ""); //test if stored in correct form

            //get attributes for grid variable
            Variable ncChl = nc.findVariable("chlorophyll");
            attributes.clear();
            NcHelper.getVariableAttributes(ncChl, attributes);
            Test.ensureEqual(attributes.getString("standard_name"), "concentration_of_chlorophyll_in_sea_water", "");
            Test.ensureEqual(attributes.getString("units"), "mg m-3", "");

            //test get dimension data - all
            PrimitiveArray pa = NcHelper.getPrimitiveArray(ncLat);
            Test.ensureEqual(pa.elementClass(), double.class, "");
            Test.ensureEqual(pa.size(), 4320, "");
            Test.ensureEqual(pa.getDouble(0), -90, "");
            Test.ensureEqual(pa.getDouble(4319), 90, ""); 

            //test get dimension data - part
            pa = NcHelper.getPrimitiveArray(ncLat, 10, 20);
            Test.ensureEqual(pa.elementClass(), double.class, "");
            Test.ensureEqual(pa.size(), 11, "");
            Test.ensureEqual(pa.getDouble(0), -89.58323686038435, "");
            Test.ensureEqual(pa.getDouble(10), -89.16647372076869, ""); 

            //get grid data
            pa = NcHelper.get4DValues(ncChl, 4500, 2080, 0, 170, 190); //x,y,z,t1,t2
            Test.ensureEqual(pa.elementClass(), float.class, "");
            String2.log("pa=" + pa);
            Test.ensureEqual(pa.size(), 21, "");
            //pre 2010-10-26 was 0.113f
            //pre 2012-08-17 was 0.12906f
            Test.ensureEqual(pa.getFloat(0), 0.13295f, "");
            Test.ensureEqual(pa.getFloat(1), Float.NaN, ""); //!!! NetcdfFile returns -9999999.0f

        } finally {
            nc.close();
        }

    /*
        //open as a NetcdfDataset, not a NetcdfFile as above
        nc = NetcdfDataset.openDataset("http://beach.mbari.org:8180/erddap/griddap/erdRyanSST"); //in tests, always non-https url
        String results, expected;
        Attributes attributes = new Attributes();
        try {
            results = nc.toString();
            results = NcHelper.decodeNcDump(results); //added with switch to netcdf-java 4.0
            String tUrl = String2.replaceAll(EDStatic.erddapUrl, "http:", "dods:"); //in tests, always non-https url
            expected = 
"zztop\n";
            Test.ensureEqual(results.substring(results.indexOf(" :satellite =")), expected, "RESULTS=\n" + results);


        } finally {
            nc.close();
        }

        */
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean doGraphicsTests) throws Throwable {

        String2.log("\n****************** EDDGridFromDap.test() *****************\n");

/* */
        // standard tests 
        testBasic1();
        testBasic2();
        testBasic3();
        testAccessibleTo();
        if (doGraphicsTests) testGraphics();
        testOpendap();
        testScaleAddOffset();
        //testPmelOscar(doGraphicsTests); DAPPER IS NO LONGER ACTIVE!
        testGenerateDatasetsXml();
        testGenerateDatasetsXmlFromThreddsCatalog();
        testGetUrlsFromThreddsCatalog();
        testGetUrlsFromHyraxCatalog();
        testAddToHyraxUrlList();
        testMetersPerSourceUnit();
        testSliderCsv();
        testKml();
        testNoAxisVariable();
        testClimatologyTime();
        testBigRequest(2); //if partialRequestMaxBytes is 10^8, this will be handled in 1 partial request
        testBigRequest(4); //if partialRequestMaxBytes is 10^8, this will be handled in 1 partial request
        testBigRequest(6); //use 6 partial requests  (time axis is now driver for multiple requests)
        testSpeedDAF();
        testSpeedMAG();
        testQuickRestart();
        testNetcdfJava();
        //testDescendingAxisGeotif(); //not working yet
        /* */

        //not regularly done
        //testForCarleton();
        //testForDave();
        //testForEllyn();
        //testOneTime();
        //testBigRequest(96); //96=~2070000000 Bytes
        //testBigRequest(110); //should fail -- too much data

        String2.log("\n*** EDDGridFromDap.test finished.");

    }



}
