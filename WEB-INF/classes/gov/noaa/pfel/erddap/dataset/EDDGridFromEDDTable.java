/* 
 * EDDGridFromEDDTable Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
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

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

/** 
 * This class represents a grid dataset from an EDDTable source.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2015-01-27
 */
public class EDDGridFromEDDTable extends EDDGrid { 

    protected EDDTable eddTable;

    protected int gapThreshold;
    final static int defaultGapThreshold = 1000;
    //for precision of axis value matching 
    final static int floatPrecision = 5;
    final static int doublePrecision = 9;
    final static int fullPrecision = 16; //nominal, used to symbolize fullPrecision
    protected int avPrecision[];

    /**
     * This constructs an EDDGridFromEDDTable based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromEDDTable"&gt; 
     *    having just been read.  
     * @return an EDDGridFromEDDTable.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridFromEDDTable fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDGridFromEDDTable(xmlReader)...");
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
        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        int tUpdateEveryNMillis = 0;
        String tLocalSourceUrl = null;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        EDDTable tEDDTable = null;
        int tGapThreshold = defaultGapThreshold;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();

        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            //String2.log(">>  tags=" + tags + content);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);
            //String2.log(">>  localTags=" + localTags + content);

            //try to make the tag names as consistent, descriptive and readable as possible
            if (localTags.equals("<dataset>")) {
                if ("false".equals(xmlReader.attributeValue("active"))) {
                    //skip it - read to </dataset>
                    if (verbose) String2.log("  skipping " + xmlReader.attributeValue("datasetID") + 
                        " because active=\"false\".");
                    while (xmlReader.stackSize() != startOfTagsN + 1 ||
                           !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
                        xmlReader.nextTag();
                        //String2.log("  skippping tags: " + xmlReader.allTags());
                    }

                } else {
                    String tType = xmlReader.attributeValue("type");
                    if (tType == null || !tType.startsWith("EDDTable"))
                        throw new SimpleException(
                            "type=\"" + tType + "\" is not allowed for the dataset within the EDDGridFromEDDTable. " +
                            "The type MUST start with \"EDDTable\".");
                    String tableDatasetID = xmlReader.attributeValue("datasetID");
                    if (tDatasetID == null || tableDatasetID == null || 
                        tDatasetID.equals(tableDatasetID))
                        throw new SimpleException(
                            "The inner eddTable datasetID must be different from the " +
                            "outer EDDGridFromEDDTable datasetID.");
                    tEDDTable = (EDDTable)EDD.fromXml(erddap, tType, xmlReader);
                }

            } else if (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
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
            else if (localTags.equals( "<updateEveryNMillis>")) {}
            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis = String2.parseInt(content); 
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
            else if (localTags.equals( "<gapThreshold>")) {}
            else if (localTags.equals("</gapThreshold>")) tGapThreshold = String2.parseInt(content); 

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

        return new EDDGridFromEDDTable(tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
            ttAxisVariables,
            ttDataVariables,
            tReloadEveryNMinutes, tUpdateEveryNMillis, tGapThreshold, tEDDTable);
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
     *  (recommended: [A-Za-z][A-Za-z0-9_]* )
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
     *    Use -1 to have ERDDAP suggest the value based on how recent the last time value is.
     * @param tGapThreshold
     * @param tEDDTable the source EDDTable.
     *    It must have a different datasetID.
     *    The destination information from the eddTable 
     *    becomes the source information for the eddGrid.
     * @throws Throwable if trouble
     */
    public EDDGridFromEDDTable(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery,
        Attributes tAddGlobalAttributes,
        Object tAxisVariables[][],
        Object tDataVariables[][],
        int tReloadEveryNMinutes, int tUpdateEveryNMillis, int tGapThreshold,
        EDDTable tEDDTable)
        throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridFromEDDTable " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridFromEDDTable(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridFromEDDTable"; 
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
        addGlobalAttributes = tAddGlobalAttributes == null? 
            new Attributes() : tAddGlobalAttributes;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        setUpdateEveryNMillis(tUpdateEveryNMillis);
        gapThreshold = tGapThreshold == Integer.MAX_VALUE? defaultGapThreshold : 
            tGapThreshold;
        //The destination information from the eddTable 
        //becomes the source information for the eddGrid.
        eddTable = tEDDTable;

        //quickRestart is handled by contained eddGrid

        //get global attributes
        sourceGlobalAttributes = eddTable.combinedGlobalAttributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("\"null\"");
        if (combinedGlobalAttributes.getString("cdm_data_type") == null)
            combinedGlobalAttributes.add("cdm_data_type", "Grid");

        //create axisVariables[]
        int nav = tAxisVariables.length;
        axisVariables = new EDVGridAxis[nav];
        //for precision of axis value matching 
        avPrecision = new int[nav]; //initially all 0, i.e., not set
        for (int av = 0; av < nav; av++) {
            String tSourceName = (String)tAxisVariables[av][0];
            String tDestName   = (String)tAxisVariables[av][1];
            if (tDestName == null || tDestName.length() == 0)
                tDestName = tSourceName;
            EDV sourceEdv = eddTable.findDataVariableByDestinationName(tSourceName); 
            Attributes tSourceAtts = sourceEdv.combinedAttributes();
            Attributes tAddAtts = (Attributes)tAxisVariables[av][2];
            String msg = errorInMethod + "For axisVariable[" + av + 
                "] destinationName=" + tDestName + ": ";

            //get sourceValues from addAttributes!
//but how update as time goes on???
            PrimitiveArray tSourceValues = tAddAtts.remove("axisValues");
            if (tSourceValues == null) {
                String avsss = "axisValuesStartStrideStop";
                PrimitiveArray sss = tAddAtts.remove(avsss);
                if (sss == null)
                    throw new RuntimeException(msg +
                        ": attribute=\"axisValues\" wasn't specified.");
                if (sss instanceof StringArray ||
                    sss instanceof CharArray)
                    throw new RuntimeException(msg + 
                        avsss + "'s type must be a numeric type, not String.");
                if (sss.size() != 3)
                    throw new RuntimeException(msg + 
                        avsss + " size=" + sss.size() +
                        ". It must be 3.");
                double tStart  = sss.getDouble(0);
                double tStride = sss.getDouble(1);
                double tStop   = sss.getDouble(2);
                if (Double.isNaN(tStart) || Double.isNaN(tStride) ||
                    Double.isNaN(tStop)  ||
                    tStart > tStop ||
                    tStride <= 0 || tStride > tStop - tStart ||
                    ((tStop - tStart)/(double)tStride > 1e7))
                    throw new RuntimeException(msg + 
                        "Invalid " + avsss + ": start=" + tStart + ", stride=" + tStride + 
                            ", stop=" + tStop);
                int n = Math2.roundToInt((tStop - tStart)/(double)tStride) + 3; //2 extra
                tSourceValues = PrimitiveArray.factory(sss.elementClass(), n, false);
                for (int i = 0; i < n; i++) {
                    double d = tStart + i * tStride; //more accurate than repeatedly add
                    if (d > tStop)
                        break;
                    tSourceValues.addDouble(d);
                }
            }

            //make the axisVariable
            axisVariables[av] = makeAxisVariable(av, tSourceName, tDestName,
                tSourceAtts, tAddAtts, tSourceValues);

            //ensure axis is ascending sorted
            if (!axisVariables[av].isAscending()) //source values
                throw new RuntimeException(msg + 
                    "The sourceValues must be ascending!");

            //precision  for matching axis values
            int tPrecision = axisVariables[av].combinedAttributes().getInt("precision");
            if (axisVariables[av] instanceof EDVTimeStampGridAxis) {
                avPrecision[av] = fullPrecision; //always compare times at full precision
            } else if (tSourceValues instanceof FloatArray) {
                avPrecision[av] = tPrecision == Integer.MAX_VALUE? 
                    floatPrecision : tPrecision;
            } else if (tSourceValues instanceof DoubleArray) {
                avPrecision[av] = tPrecision == Integer.MAX_VALUE? 
                    doublePrecision : tPrecision;
            } else { //all int types
                avPrecision[av] = fullPrecision;
            }
        }

        //create dataVariables[]
        dataVariables = new EDV[tDataVariables.length];
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName   = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.length() == 0)
                tDestName = tSourceName;
            EDV sourceEdv = eddTable.findDataVariableByDestinationName(tSourceName); 
            String dvSourceDataType = sourceEdv.destinationDataType();
            Attributes tSourceAtts = sourceEdv.combinedAttributes();
            Attributes tAddAtts = (Attributes)tDataVariables[dv][2];

            //create the EDV dataVariable
            if (tDestName.equals(EDV.TIME_NAME))
                throw new RuntimeException(errorInMethod +
                    "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
            else if (EDVTime.hasTimeUnits(tSourceAtts, tAddAtts)) 
                dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName,
                    tSourceAtts, tAddAtts, dvSourceDataType);  
            else dataVariables[dv] = new EDV(
                tSourceName, tDestName, 
                tSourceAtts, tAddAtts, dvSourceDataType, 
                Double.NaN, Double.NaN);  //hard to get min and max
            dataVariables[dv].extractAndSetActualRange();
        }

        //ensure the setup is valid
        ensureValid();

        //If the child is a FromErddap, try to subscribe to the remote dataset.
        if (eddTable instanceof FromErddap) 
            tryToSubscribeToChildFromErddap(eddTable);

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridFromEDDTable " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException("Error: " + 
            "EDDGridFromEDDTable doesn't support method=\"sibling\".");
    }

    /**
     * This does the actual incremental update of this dataset 
     * (i.e., for real time datasets).
     * 
     * <p>Concurrency issue: The changes here are first prepared and 
     * then applied as quickly as possible (but not atomically!).
     * There is a chance that another thread will get inconsistent information
     * (from some things updated and some things not yet updated).
     * But I don't want to synchronize all activities of this class.
     *
     * @param msg the start of a log message, e.g., "update(thisDatasetID): ".
     * @param startUpdateMillis the currentTimeMillis at the start of this update.
     * @return true if a change was made
     * @throws Throwable if serious trouble. 
     *   For simple failures, this writes info to log.txt but doesn't throw an exception.
     *   If the dataset has changed in a serious / incompatible way and needs a full
     *     reload, this throws WaitThenTryAgainException 
     *     (usually, catcher calls LoadDatasets.tryToUnload(...) and EDD.requestReloadASAP(tDatasetID))..
     *   If the changes needed are probably fine but are too extensive to deal with here, 
     *     this calls EDD.requestReloadASAP(tDatasetID) and returns without doing anything.
     */
    public boolean lowUpdate(String msg, long startUpdateMillis) throws Throwable {
//but somehow, this should update the outer time axis in this eddGrid.

        //update the internal eddTable
        return eddTable.lowUpdate(msg, startUpdateMillis);
    }


    /** 
     * This gets source data (not yet converted to destination data) from the data 
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

        //loggedInAs isn't known here, but EDDTable uses it for row-by-row 
        //authentication. null = Not good, but okay/safe, since it just denies access. 
        String loggedInAs = null; 

        //create eddTable query from constraints
        int nav = axisVariables.length;
        int ndv = tDataVariables.length;
        int nCols = nav + ndv;

        //create the results[] PAs (filled with missing values) to hold the results
        PrimitiveArray results[] = new PrimitiveArray[nCols];
        int[] start   = new int[nav];
        int[] stride  = new int[nav];
        int[] stop    = new int[nav];
        int[] nEachAV = new int[nav]; //for this request
        //This is for calculating arbitrary position within dataVariable results arrays.
        //This is similar to the digit in 100's place in an integer having a value of 100.
        //It reflects the size of the request as if all axes had stride=1.
        //(It is the amount of data that would/will be transferred, related to each axis.)
        int[] resultAvValue = new int[nav]; 
        //This is for gap sizes.
        //This is similar to the digit in 100's place in an integer having a value of 100.
        //It reflects the size of the request as if all axes had stride=1.
        //(It is the amount of data that would/will be transferred, related to each axis.)
        int[] gapAvValue = new int[nav]; 
        int[] fullNEachAV = new int[nav]; //full axis sizes for this dataset
        Arrays.fill(resultAvValue, 1);
        Arrays.fill(gapAvValue,    1);
        //If the user's request is huge, this request will be a small sub-request. 
        //It is constrained by partialRequestMaxCells. So an int for nv will be fine.
        int willKeepNRows = 1; //the number of rows that will be kept
        //create results PAs for axisVariables, filled with correct source values
        for (int av = 0; av < nav; av++) {
            start[av]  = tConstraints.get(av*3); 
            stride[av] = tConstraints.get(av*3 + 1); 
            stop[av]   = tConstraints.get(av*3 + 2);
            PrimitiveArray sourceValues = axisVariables[av].sourceValues();
            int tFullSize = sourceValues.size();
            fullNEachAV[av] = tFullSize;
            results[av] = sourceValues.subset(
                start[av], stride[av], stop[av]);
            int tSize = results[av].size();
            nEachAV[av] = tSize;
            willKeepNRows *= tSize;
            for (int av2 = 0; av2 < av; av2++) {
                resultAvValue[av2] *= tSize; //using actual stride
                gapAvValue[av2] *= (stop[av] - start[av] + 1); //as if stride=1
            }
        }
        //create results PAs for dataVariables, full-sized, filled with missing values
        for (int dv = 0; dv < ndv; dv++) {
            EDV edv = tDataVariables[dv];
            double smv = edv.safeDestinationMissingValue();
            results[nav + dv] = PrimitiveArray.factory(
                edv.sourceDataTypeClass(), willKeepNRows, 
                Double.isNaN(smv)? "" : "" + smv); //filled with missing values
        }

        //Think of source table as having all rows in order.
        //Think of axes left to right as, e.g., nav=3 sst[0=time][1=lat][2=lon]
        //If an axis' request stride times the gapAvValue of that dimension is big,
        //  the data requested may be 
        //  1 or few rows, then a big gap (lots of rows), then
        //  1 or few rows, then a big gap (lots of rows), etc.
        //With just one big request, all the data (including the gaps)
        //  would be requested then thrown away below.
        //So go through axes right to left, looking for rightmost gap > gapThreshold.
        //That defines outerAxisVars (on left) and innerAxisVars (on right).
        //Then, I'll make subrequests with single values of outerAxisVars
        //  and the full range of innerAxisVars.
        //COMPLICATED CODE!  Great potential for off-by-one errors!
        int nInnerAxes = 0; //to start: will it be just 0?  It may end up 0 .. nav.
        int gap = 0;
        while (nInnerAxes < nav) {
            //if (stride-1) * gapAvValue < gapThreshold, continue
            //(stride=1 -> nSkip=0, so use stride-1 to find nSkipped)
            gap = (stride[(nav-1) - nInnerAxes]-1) * gapAvValue[(nav-1) - nInnerAxes];
            if (debugMode) String2.log("nInner=" + nInnerAxes + 
                " gap=" + gap + " = ((stride=" + stride[(nav-1) - nInnerAxes] + 
                ")-1) * gapAvValue=" + gapAvValue[(nav-1) - nInnerAxes]);
            if (gap >= gapThreshold)
                break;
            nInnerAxes++;
        }
        int nOuterAxes = nav - nInnerAxes;

        //make NDimensionalIndex for outerAxisVars
        int shape[];
        if (nOuterAxes == 0) {
            shape = new int[]{1};            
        } else {
            shape = new int[nOuterAxes];
            for (int av = 0; av < nOuterAxes; av++)
                shape[av] = results[av].size();
        }
        //The outerNDIndex has the leftmost/outerAxis
        //It will walk us through the individual values.
        NDimensionalIndex outerNDIndex = new NDimensionalIndex(shape);
        int outerNDCurrent[] = outerNDIndex.getCurrent(); //it will be repeatedly updated
        if (verbose) String2.log(
            "* nOuterAxes=" + nOuterAxes + " of " + nav + 
            " nOuterRequests=" + outerNDIndex.size());
        if (reallyVerbose) String2.log( 
            "  axis sizes: result=" + String2.toCSSVString(nEachAV) +
            " dataset=" + String2.toCSSVString(fullNEachAV) +
            "\n  strides=" + String2.toCSSVString(stride) +
            " gapAvValues=" + String2.toCSSVString(gapAvValue) +
            (nOuterAxes == 0? "" :
                ("\n  gap=" + gap + "=((stride=" + stride[(nav-1) - nInnerAxes] + 
                 ")-1) * gapAvValue=" + gapAvValue[(nav-1) - nInnerAxes] +
                 " >= gapThreshold=" + gapThreshold)));

        //make a subrequest for each combination of outerAxes values
        TableWriterAll twa = new TableWriterAll(null, null, //metadata not kept
            cacheDirectory(), 
            suggestFileName(loggedInAs, datasetID, ".twa")); //twa adds a random number
        twa.ignoreFinish = true;
        int qn = 0;
        outerLoop:
        while (outerNDIndex.increment()) { //updates outerNDCurrent

            //build the eddTable query for this subset of full request
            //axisVariable sourceNames
            StringBuilder querySB = new StringBuilder(
                String2.toSVString(axisVariableSourceNames(), ",", false)); //don't include trailing separator
            //requested dataVariable sourceNames
            for (int dv = 0; dv < ndv; dv++) 
                querySB.append("," + tDataVariables[dv].sourceName());
            //outer constraints     COMPLICATED!
            for (int av = 0; av < nOuterAxes; av++) {
                EDVGridAxis edvga = axisVariables[av];
                String sn = edvga.sourceName();
                PrimitiveArray sv = edvga.sourceValues();
                querySB.append( //just request 1 value for outerAxes
                    "&" + sn + "=" + 
                    sv.getDouble(start[av] + outerNDCurrent[av] * stride[av]));
            }
            //inner constraints   
            for (int iav = 0; iav < nInnerAxes; iav++) {
                int av = nOuterAxes + iav;
                EDVGridAxis edvga = axisVariables[av];
                String sn = edvga.sourceName();
                PrimitiveArray sv = edvga.sourceValues();
                querySB.append(//always request the full range of innerAxes
                    "&" + sn + ">=" + sv.getDouble(start[av]) + 
                    //unfortunately, no way to use the stride value in the constraint
                    "&" + sn + "<=" + sv.getDouble(stop[av]));
            }
            String query = querySB.toString();
            if (verbose) String2.log("query#" + qn++ + " for eddTable=" + query);

            //get the tabular results
            String requestUrl = "/erddap/tabledap/" + datasetID + 
                ".twa"; //I think irrelevant, since not getting metadata from source
            try {
                if (eddTable.handleViaFixedOrSubsetVariables(loggedInAs, requestUrl, query, twa)) {}
                else             eddTable.getDataForDapQuery(loggedInAs, requestUrl, query, twa);  
            } catch (Throwable t) {
                String msg = t.toString();
                if (msg == null || msg.indexOf(MustBe.THERE_IS_NO_DATA) < 0) 
                    throw t;
            }
        }
        twa.ignoreFinish = false;
        twa.finish();

        //go through the tabular results in TableWriterAll
        PrimitiveArray twaPA[] = new PrimitiveArray[nCols];
        DataInputStream twaDIS[] = new DataInputStream[nCols];
        for (int col = 0; col < nCols; col++) {
            twaPA[col] = twa.columnEmptyPA(col);
            twaDIS[col] = twa.dataInputStream(col);
        }

        long nRows = twa.nRows();
        int oAxisIndex[] = new int[nav]; //all 0's
        int axisIndex[]  = new int[nav]; //all 0's
        int dataIndex = 0; //index for the data results[] PAs
        int nMatches = 0;
        rowLoop: 
        for (long row = 0; row < nRows; row++) {
            //read all of the twa values for this row
            for (int col = 0; col < nCols; col++) {
                twaPA[col].clear();
                twaPA[col].readDis(twaDIS[col], 1); //read 1 value
            }

            //see if this row matches a desired combo of axis values 
            dataIndex = 0; //index for the data results[] PAs
            System.arraycopy(axisIndex, 0, oAxisIndex, 0, nav);
            for (int av = 0; av < nav; av++) {

                double avDouble = twaPA[av].getDouble(0);                
                if (debugMode) String2.log("row=" + row + " av=" + av + " value=" + avDouble);
                int navPA = results[av].size();
                int insertAt;
                int precision = avPrecision[av];
                int oIndex = oAxisIndex[av];
                if (precision == fullPrecision) {
                    if (results[av].getDouble(oIndex) == avDouble) //same as last row?
                        insertAt = oIndex;
                    else
                        insertAt = results[av].binarySearch(0, navPA - 1, avDouble);
                } else {
                    if (Math2.almostEqual(precision, results[av].getDouble(oIndex), avDouble)) {
                        insertAt = oIndex;
                    } else {
                        insertAt = results[av].binaryFindFirstGAE(0, navPA - 1, avDouble,
                            precision);
                        if (insertAt >= navPA || //not close
                            !Math2.almostEqual(precision, avDouble, results[av].getDouble(insertAt)))
                            insertAt = -1;
                    }
                }
                if (insertAt >= 0) {
                    if (debugMode) String2.log("  matched index=" + insertAt);
                    axisIndex[av] = insertAt;
                    dataIndex += insertAt * resultAvValue[av];
                } else {
                    continue rowLoop;
                }
            }

            //all axes have a match! so copy data values into results[]
            nMatches++;
            if (debugMode) 
                String2.log("  axisIndex=" + String2.toCSSVString(axisIndex) + 
                    " dataIndex=" + dataIndex);
            for (int dv = 0; dv < ndv; dv++) {
                int col = nav + dv;
                results[col].setFromPA(dataIndex, twaPA[col], 0);
            }
        }

        //release twa resources
        for (int col = 0; col < nCols; col++) 
            twaDIS[col].close();
        twa.releaseResources();

        String2.log("EDDGridFromEDDTable nMatches=" + nMatches + 
            " out of TableWriterAll nRows=" + nRows);
        return results;
    }

    /** 
     * This creates a rough draft of a datasets.xml entry for an EDDGridFromEDDTable.
     * The XML MUST then be edited by hand and added to the datasets.xml file.
     *
     * @param eddTableID the datasetID of the underlying EDDTable dataset.
     * @param tReloadEveryNMinutes  E.g., 1440 for once per day. 
     *    Use, e.g., 1000000000, for never reload.  -1 for the default.
     * @param externalAddGlobalAttributes globalAttributes gleaned from external 
     *    sources, e.g., a THREDDS catalog.xml file.
     *    These have priority over other sourceGlobalAttributes.
     *    Okay to use null if none.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String eddTableID, 
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes) 
        throws Throwable {

        String2.log("\n*** EDDGridFromEDDTable.generateDatasetsXml" +
            "\neddTableID=" + eddTableID + 
            " reloadEveryNMinutes=" + tReloadEveryNMinutes +
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        Table sourceAxisTable = new Table();  
        Table sourceDataTable = new Table();  
        Table addAxisTable = new Table();
        Table addDataTable = new Table();

        //get the eddTable
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, eddTableID);
        Attributes sourceGAtts = sourceDataTable.globalAttributes();
        Attributes destGAtts   = addDataTable.globalAttributes();
        if (externalAddGlobalAttributes != null)
            destGAtts.set(externalAddGlobalAttributes);
        sourceGAtts.set(eddTable.combinedGlobalAttributes());
        destGAtts.add("cdm_data_type", "Grid");
        if (sourceGAtts.get("featureType") != null)
            destGAtts.set("featureType", "null");

        int ndv = eddTable.dataVariables().length;
        for (int dv = 0; dv < ndv; dv++) {
            EDV sourceEdv = eddTable.dataVariables()[dv];
            String destName = sourceEdv.destinationName();
            Class tClass = sourceEdv.destinationDataTypeClass();
            Attributes sourceAtts = sourceEdv.combinedAttributes();
            Attributes destAtts = new Attributes();
            if (destName.equals(EDV.TIME_NAME) ||
                destName.equals(EDV.ALT_NAME) ||
                destName.equals(EDV.DEPTH_NAME) ||
                sourceAtts.get("instance_dimension") != null ||
                sourceAtts.get("sample_dimension") != null) {
                //make it an axisVariable
                sourceAxisTable.addColumn(sourceAxisTable.nColumns(), destName,
                    PrimitiveArray.factory(tClass, 1, false), sourceAtts);
                addAxisTable.addColumn(addAxisTable.nColumns(), destName,
                    PrimitiveArray.factory(tClass, 1, false), destAtts);
                if (sourceAtts.get("cf_role") != null)
                    destAtts.set("cf_role", "null");
                if (sourceAtts.get("instance_dimension") != null)
                    destAtts.set("instance_dimension", "null");
                if (sourceAtts.get("sample_dimension") != null)
                    destAtts.set("sample_dimension", "null");
            } else {
                //leave it as a dataVariable
                sourceDataTable.addColumn(sourceDataTable.nColumns(), destName,
                    PrimitiveArray.factory(tClass, 1, false), sourceAtts);
                addDataTable.addColumn(addDataTable.nColumns(), destName,
                    makeDestPAForGDX(PrimitiveArray.factory(tClass, 1, false), sourceAtts), 
                    destAtts);
            }
        }

        //write the information
        StringBuilder results = new StringBuilder(directionsForGenerateDatasetsXml());
        results.append(
            "-->\n\n");
        results.append(
            "<dataset type=\"EDDGridFromEDDTable\" datasetID=\"" + 
                suggestDatasetID(eddTableID + "/EDDGridFromEDDTable") + 
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + 
            (tReloadEveryNMinutes < 0 || tReloadEveryNMinutes == Integer.MAX_VALUE? 
                DEFAULT_RELOAD_EVERY_N_MINUTES : tReloadEveryNMinutes) + 
                "</reloadEveryNMinutes>\n" +
            "    <gapThreshold>" + defaultGapThreshold + "</gapThreshold>\n");
        results.append(writeAttsForDatasetsXml(false, sourceDataTable.globalAttributes(), "    "));
        results.append(writeAttsForDatasetsXml(true,  addDataTable.globalAttributes(),    "    "));
        results.append("\n" +
            "    <!-- *** If appropriate:\n" +
            "      * Change some of the <dataVariables> to be <axisVariables>.\n" +
            "      * Insert them here in the correct order.\n" +
            "      * For each one, add to its <addAttributes> one of:\n" +
            "        <att name=\"axisValues\" type=\"doubleList\">a CSV list of values</att>\n" +
            "        <att name=\"axisValuesStartStrideStop\" type=\"doubleList\">startValue, strideValue, stopValue</att>\n" +
            "      * For each one, if defaults aren't suitable, add\n" +
            "        <att name=\"precision\" type=\"int\">totalNumberOfDigitsToMatch</att>\n" +
            "    -->\n\n");
        results.append(writeVariablesForDatasetsXml(sourceAxisTable, addAxisTable, 
            "axisVariable",        //assume LLAT already identified
            false, false)); //includeDataType, questionDestinationName
        results.append("\n");
        results.append(writeVariablesForDatasetsXml(sourceDataTable, addDataTable, 
            "dataVariable", 
            false, false)); //includeDataType, questionDestinationName
        results.append("\n" +
            "    <!-- *** Insert the entire <dataset> chunk for " + eddTableID + " here.\n" +
            "       If the original dataset will be accessible to users, change the\n" +
            "       datasetID here so they aren't the same. -->\n" +
            "    <dataset ... > ... </dataset>\n" +
            "\n" +
            "</dataset>\n" +
            "\n");

        String2.log("\n*** generateDatasetsXml finished successfully.\n");
        return results.toString();
    }

    /** testGenerateDatasetsXml */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();
        String2.log("\n*** EDDGridFromEDDTable.testGenerateDatasetsXml");
        String tid = "erdNph";

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromEDDTable\" datasetID=\"erdNph_c2e8_7f71_e246\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <gapThreshold>1000</gapThreshold>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"defaultDataQuery\">time,year,month,longitude,latitude,area,maxSLP</att>\n" +
"        <att name=\"defaultGraphQuery\">longitude,latitude,month&amp;.draw=markers</att>\n" +
"        <att name=\"Easternmost_Easting\" type=\"double\">233.5</att>\n" +
"        <att name=\"featureType\">Point</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">39.3</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">23.3</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">233.5</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">201.3</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"id\">erdNph</att>\n" +
"        <att name=\"infoUrl\">http://onlinelibrary.wiley.com/doi/10.1002/grl.50100/abstract</att>\n" +
"        <att name=\"institution\">NOAA ERD</att>\n" +
"        <att name=\"keywords\">area, areal, california, ccs, center, centered, contour, current, data, extent, high, hpa, level, maximum, month, north, nph, pacific, pressure, sea, system, time, year</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">39.3</att>\n" +
"        <att name=\"references\">Schroeder, Isaac D., Bryan A. Black, William J. Sydeman, Steven J. Bograd, Elliott L. Hazen, Jarrod A. Santora, and Brian K. Wells. &quot;The North Pacific High and wintertime pre-conditioning of California current productivity&quot;, Geophys. Res. Letters, VOL. 40, 541-546, doi:10.1002/grl.50100, 2013</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">23.3</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"subsetVariables\">time, year, month</att>\n" +
"        <att name=\"summary\">Variations in large-scale atmospheric forcing influence upwelling dynamics and ecosystem productivity in the California Current System (CCS). In this paper, we characterize interannual variability of the North Pacific High over 40 years and investigate how variation in its amplitude and position affect upwelling and biology. We develop a winter upwelling &quot;pre-conditioning&quot; index and demonstrate its utility to understanding biological processes. Variation in the winter NPH can be well described by its areal extent and maximum pressure, which in turn is predictive of winter upwelling. Our winter pre-conditioning index explained 64&#37; of the variation in biological responses (fish and seabirds). Understanding characteristics of the NPH in winter is therefore critical to predicting biological responses in the CCS.</att>\n" +
"        <att name=\"time_coverage_end\">2014-01-16</att>\n" +
"        <att name=\"time_coverage_start\">1967-01-16</att>\n" +
"        <att name=\"title\">North Pacific High, 1967 - present</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">201.3</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"featureType\">null</att>\n" +
"    </addAttributes>\n" +
"\n" +
"    <!-- *** If appropriate:\n" +
"      * Change some of the <dataVariables> to be <axisVariables>.\n" +
"      * Insert them here in the correct order.\n" +
"      * For each one, add to its <addAttributes> one of:\n" +
"        <att name=\"axisValues\" type=\"doubleList\">a CSV list of values</att>\n" +
"        <att name=\"axisValuesStartStrideStop\" type=\"doubleList\">startValue, strideValue, stopValue</att>\n" +
"      * For each one, if defaults aren't suitable, add\n" +
"        <att name=\"precision\" type=\"int\">totalNumberOfDigitsToMatch</att>\n" +
"    -->\n" +
"\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">-9.33984E7 1.3898304E9</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Centered Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"time_precision\">1970-01-01</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"\n" +
"    <dataVariable>\n" +
"        <sourceName>year</sourceName>\n" +
"        <destinationName>year</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"shortList\">1967 2014</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Year</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>month</sourceName>\n" +
"        <destinationName>month</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"byteList\">1 12</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Month (1 - 12)</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">201.3 233.5</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude of the Center of the NPH</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">23.3 39.3</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude of the Center of the NPH</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>area</sourceName>\n" +
"        <destinationName>area</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 7810500.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Areal Extent of the 1020 hPa Contour</att>\n" +
"            <att name=\"units\">km2</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>maxSLP</sourceName>\n" +
"        <destinationName>maxSLP</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">1016.7 1033.3</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Maximum Sea Level Pressure</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"\n" +
"    <!-- *** Insert the entire <dataset> chunk for erdNph here.\n" +
"       If the original dataset will be accessible to users, change the\n" +
"       datasetID here so they aren't the same. -->\n" +
"    <dataset ... > ... </dataset>\n" +
"\n" +
"</dataset>\n" +
"\n\n";

        try {
            String results = generateDatasetsXml(tid, -1, null) + "\n";
            
            Test.ensureEqual(results, expected, "results=\n" + results);

            //int po = results.indexOf(expected2.substring(0, 40));
            //Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{
                "-verbose", "EDDGridFromEDDTable", tid, "-1"},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, expected,
                "Unexpected results from GenerateDatasetsXml.doIt.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }
    
    }


    /** testBasic */    
    public static void testBasic() throws Throwable {
        String2.log("\n*** EDDGridFromEDDTable.testBasic\n");
        testVerboseOn();
        String name, tName, query, results, expected, error;
        String testDir = EDStatic.fullTestCacheDirectory;
        int po;
        EDDGrid edd = (EDDGrid)oneFromDatasetsXml(null, "testGridFromTable"); //should work

        //get dds
        String2.log("\nget .dds\n");
        tName = edd.makeNewFileForDapQuery(null, null, "", testDir, 
            edd.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 66201];\n" +
"  Float64 latitude[latitude = 1];\n" +
"  Float64 longitude[longitude = 1];\n" +
"  Float64 depth[depth = 19];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int32 DataQuality[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } DataQuality;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte DataQuality_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } DataQuality_flag;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Eastward[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Eastward;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte Eastward_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Eastward_flag;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 ErrorVelocity[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } ErrorVelocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte ErrorVelocity_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } ErrorVelocity_flag;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int32 Intensity[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Intensity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte Intensity_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Intensity_flag;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Northward[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Northward;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte Northward_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Northward_flag;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Upwards[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Upwards;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte Upwards_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n" +
"    MAPS:\n" +
"      Float64 time[time = 66201];\n" +
"      Float64 latitude[latitude = 1];\n" +
"      Float64 longitude[longitude = 1];\n" +
"      Float64 depth[depth = 19];\n" +
"  } Upwards_flag;\n" +
"} testGridFromTable;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //get das
        String2.log("\nget .das\n");
        tName = edd.makeNewFileForDapQuery(null, null, "", testDir, 
            edd.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.10184436e+9, 1.10978836e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 34.04017, 34.04017;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -120.31121, -120.31121;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range -1.8, 16.2;\n" +
"    String axis \"Z\";\n" +
"    String description \"Relative to Mean Sea Level (MSL)\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  DataQuality {\n" +
"    Int32 _FillValue 9999;\n" +
"    Int32 actual_range 0, 100;\n" +
"    String description \"A post-processing quantitative data quality indicator.  Specifically, RDI percent-good #4, in earth coordinates (percentage of successful 4-beam transformations), expressed as a percentage, 50, 80, 95, etc.\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Data Quality\";\n" +
"    String units \"%\";\n" +
"  }\n" +
"  DataQuality_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Data Quality Flag\";\n" +
"  }\n" +
"  Eastward {\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range -0.966, 1.051;\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String description \"True eastward velocity measurements. Negative values represent westward velocities.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Eastward Current\";\n" +
"    String standard_name \"eastward_sea_water_velocity\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  Eastward_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Eastward Flag\";\n" +
"    String standard_name \"eastward_sea_water_velocity status_flag\";\n" +
"  }\n" +
"  ErrorVelocity {\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range -1.073, 1.122;\n" +
"    String description \"The difference of two vertical velocities, each measured by an opposing pair of ADCP beams.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Error Velocity\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  ErrorVelocity_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Error Velocity Flag\";\n" +
"  }\n" +
"  Intensity {\n" +
"    Int32 _FillValue 9999;\n" +
"    Int32 actual_range 53, 228;\n" +
"    String description \"ADCP echo intensity (or backscatter), in RDI counts.  This value represents the average of all 4 beams, rounded to the nearest whole number.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Intensity\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  Intensity_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Intensity Flag\";\n" +
"  }\n" +
"  Northward {\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range -1.072, 1.588;\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String description \"True northward velocity measurements. Negative values represent southward velocities.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Northward Current\";\n" +
"    String standard_name \"northward_sea_water_velocity\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  Northward_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Northward Flag\";\n" +
"    String standard_name \"northward_sea_water_velocity status_flag\";\n" +
"  }\n" +
"  Upwards {\n" +
"    Float64 _FillValue 9999.0;\n" +
"    Float64 actual_range -0.405, 0.406;\n" +
"    String description \"True upwards velocity measurements.  Negative values represent downward velocities.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Upward Current\";\n" +
"    String standard_name \"upward_sea_water_velocity\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  Upwards_flag {\n" +
"    Byte actual_range 0, 9;\n" +
"    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Upwards Flag\";\n" +
"    String standard_name \"upward_sea_water_velocity\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -120.31121;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 34.04017;\n" +
"    Float64 geospatial_lat_min 34.04017;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -120.31121;\n" +
"    Float64 geospatial_lon_min -120.31121;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Created by the NCDDC PISCO ADCP Profile to converter on 2009/00/11 15:00 CST.\n";
       Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

//"2015-01-30T17:06:49Z https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\n" +
//"2015-01-30T17:06:49Z http://localhost:8080/cwexperimental/griddap/testGridFromTable.das\";\n" +
expected=
    "String infoUrl \"https://www.ncddc.noaa.gov/activities/wcos\";\n" +
"    String institution \"NOAA NMSP\";\n" +
"    String keywords \"Oceans > Ocean Circulation > Ocean Currents,\n" +
"adcp, atmosphere, circulation, coast, current, currents, data, depth, eastward, eastward_sea_water_velocity, eastward_sea_water_velocity status_flag, error, flag, height, identifier, intensity, nmsp, noaa, northward, northward_sea_water_velocity, northward_sea_water_velocity status_flag, observing, ocean, oceans, quality, sea, seawater, station, status, system, time, upward, upward_sea_water_velocity, upwards, velocity, water, wcos, west, west coast\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 34.04017;\n" +
"    String sourceUrl \"https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\";\n" +
"    Float64 Southernmost_Northing 34.04017;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"longitude, latitude\";\n" +
"    String summary \"The West Coast Observing System (WCOS) project provides access to temperature and currents data collected at four of the five National Marine Sanctuary sites, including Olympic Coast, Gulf of the Farallones, Monterey Bay, and Channel Islands. A semi-automated end-to-end data management system transports and transforms the data from source to archive, making the data acessible for discovery, access and analysis from multiple Internet points of entry.\n" +
"\n" +
"The stations (and their code names) are San Miguel North (BAY), Santa Rosa North (BEA), Cuyler Harbor (CUY), Pelican/Prisoners Area (PEL), San Miguel South (SMS), Santa Rosa South (SRS), Valley Anch (VAL).\";\n" +
"    String time_coverage_end \"2005-03-02T18:32:40Z\";\n" +
"    String time_coverage_start \"2004-11-30T19:52:40Z\";\n" +
"    String title \"West Coast Observing System (WCOS) ADCP Currents Data\";\n" +
"    String Version \"2\";\n" +
"    Float64 Westernmost_Easting -120.31121;\n" +
"  }\n" +
"}\n";
        po = results.indexOf("String infoUrl");
        Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

        //get data subset -- big gap in axis#0
        //look in logs to see 
//* nOuterAxes=1 of 4 nOuterRequests=2
//  axis sizes: result=2, 1, 1, 19 dataset=66201, 1, 1, 19
//  strides=10, 1, 1, 1 gapAvValues=19, 19, 19, 1
//  gap=171=((stride=10)-1) * gapAvValue=19 >= gapThreshold=15
        String2.log("\nget .csv\n");
        tName = edd.makeNewFileForDapQuery(null, null, 
            "Eastward[10:10:20][][][],Eastward_flag[10:10:20][][][]," +
            "Northward[10:10:20][][][],Northward_flag[10:10:20][][][]", 
            testDir, edd.className() + "_gap0", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
/* from source eddTable
station,longitude,latitude,time,depth,Eastward,Eastward_flag,Northward,Northward_flag
,degrees_east,degrees_north,UTC,m,m s-1,,m s-1,
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,16.2,NaN,9,NaN,9
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,15.2,0.001,0,0.06,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,14.2,-0.002,0,0.043,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,13.2,-0.037,0,0.023,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,12.2,-0.029,0,0.055,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,11.2,-0.014,0,0.052,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,10.2,-0.023,0,0.068,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,9.2,-0.039,0,0.046,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,8.2,-0.043,0,0.063,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,7.2,-0.03,0,0.062,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,6.2,-0.032,0,0.075,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,5.2,-0.038,0,0.061,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,4.2,-0.059,0,0.075,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,3.2,-0.062,0,0.102,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,2.2,-0.059,0,0.101,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,1.2,-0.069,0,0.079,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,0.2,NaN,9,NaN,9
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,-0.8,NaN,9,NaN,9
BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,-1.8,NaN,9,NaN,9
*/
        expected = 
"time,latitude,longitude,depth,Eastward,Eastward_flag,Northward,Northward_flag\n" +
"UTC,degrees_north,degrees_east,m,m s-1,,m s-1,\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,-0.8,NaN,9,NaN,9\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,0.19999999999999996,NaN,9,NaN,9\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,1.2,-0.069,0,0.079,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,2.2,-0.059,0,0.101,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,3.2,-0.062,0,0.102,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,4.2,-0.059,0,0.075,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,5.2,-0.038,0,0.061,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,6.2,-0.032,0,0.075,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,7.2,-0.03,0,0.062,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,8.2,-0.043,0,0.063,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,9.2,-0.039,0,0.046,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,10.2,-0.023,0,0.068,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,11.2,-0.014,0,0.052,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,12.2,-0.029,0,0.055,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,13.2,-0.037,0,0.023,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,14.2,-0.002,0,0.043,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,15.2,0.001,0,0.06,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,16.2,NaN,9,NaN,9\n" +
/* from source eddTable
station,longitude,latitude,time,depth,Eastward,Eastward_flag,Northward,Northward_flag
,degrees_east,degrees_north,UTC,m,m s-1,,m s-1,
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,16.2,NaN,9,NaN,9
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,15.2,0.034,0,0.047,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,14.2,0.033,0,0.037,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,13.2,0.007,0,0.051,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,12.2,-0.003,0,0.037,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,11.2,-0.016,0,0.042,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,10.2,-0.016,0,0.055,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,9.2,-0.028,0,0.041,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,8.2,-0.028,0,0.093,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,7.2,-0.057,0,0.061,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,6.2,-0.06,0,0.064,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,5.2,-0.077,0,0.081,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,4.2,-0.139,0,0.086,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,3.2,-0.105,0,0.102,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,2.2,-0.122,0,0.1,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,1.2,-0.02,0,-0.065,0
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,0.2,NaN,9,NaN,9
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,-0.8,NaN,9,NaN,9
BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,-1.8,NaN,9,NaN,9
*/
"2004-11-30T20:32:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,-0.8,NaN,9,NaN,9\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,0.19999999999999996,NaN,9,NaN,9\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,1.2,-0.02,0,-0.065,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,2.2,-0.122,0,0.1,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,3.2,-0.105,0,0.102,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,4.2,-0.139,0,0.086,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,5.2,-0.077,0,0.081,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,6.2,-0.06,0,0.064,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,7.2,-0.057,0,0.061,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,8.2,-0.028,0,0.093,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,9.2,-0.028,0,0.041,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,10.2,-0.016,0,0.055,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,11.2,-0.016,0,0.042,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,12.2,-0.003,0,0.037,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,13.2,0.007,0,0.051,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,14.2,0.033,0,0.037,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,15.2,0.034,0,0.047,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,16.2,NaN,9,NaN,9\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        String2.log("\n*** EDDGridFromEDDTable.testBasic finished successfully\n");

        //get data subset -- big gap in axis#3
        //look in log to see: 
//* nOuterAxes=4 of 4 nOuterRequests=4
//  axis sizes: result=2, 1, 1, 2 dataset=66201, 1, 1, 19
//  strides=10, 1, 1, 17 gapAvValues=18, 18, 18, 1
//  gap=16=((stride=17)-1) * gapAvValue=1 >= gapThreshold=15
        String2.log("\nget .csv\n");
        tName = edd.makeNewFileForDapQuery(null, null, 
            "Eastward[10:10:20][][][0:17:17],Eastward_flag[10:10:20][][][0:17:17]," +
            "Northward[10:10:20][][][0:17:17],Northward_flag[10:10:20][][][0:17:17]", 
            testDir, edd.className() + "_gap3", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
expected=
"time,latitude,longitude,depth,Eastward,Eastward_flag,Northward,Northward_flag\n" +
"UTC,degrees_north,degrees_east,m,m s-1,,m s-1,\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,15.2,0.001,0,0.06,0\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n" +
"2004-11-30T20:32:40Z,34.04017,-120.31121,15.2,0.034,0,0.047,0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        String2.log("\n*** EDDGridFromEDDTable.testBasic finished successfully\n");


        //get data subset -- no gap
        //look in log to see: 
//* nOuterAxes=0 of 4 nOuterRequests=1
//  axis sizes: result=1, 1, 1, 8 dataset=66201, 1, 1, 19
//  strides=1, 1, 1, 1 gapAvValues=8, 8, 8, 1
        String2.log("\nget .csv\n");
        tName = edd.makeNewFileForDapQuery(null, null, 
            "Eastward[10][][][2:9],Eastward_flag[10][][][2:9]," +
            "Northward[10][][][2:9],Northward_flag[10][][][2:9]", 
            testDir, edd.className() + "_nogap", ".csv"); 
        results = new String((new ByteArray(testDir + tName)).toArray());
expected=
"time,latitude,longitude,depth,Eastward,Eastward_flag,Northward,Northward_flag\n" +
"UTC,degrees_north,degrees_east,m,m s-1,,m s-1,\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,0.19999999999999996,NaN,9,NaN,9\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,1.2,-0.069,0,0.079,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,2.2,-0.059,0,0.101,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,3.2,-0.062,0,0.102,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,4.2,-0.059,0,0.075,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,5.2,-0.038,0,0.061,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,6.2,-0.032,0,0.075,0\n" +
"2004-11-30T20:12:40Z,34.04017,-120.31121,7.2,-0.03,0,0.062,0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        String2.log("\n*** EDDGridFromEDDTable.testBasic finished successfully\n");
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        String2.log("\n****************** EDDGridFromEDDTable.test() *****************\n");
        // standard tests 
/* for releases, this line should have open/close comment */
        testGenerateDatasetsXml(); 
        testBasic();
        /* */

        String2.log("\n*** EDDGridFromEDDTable.test finished.");
    }

}
