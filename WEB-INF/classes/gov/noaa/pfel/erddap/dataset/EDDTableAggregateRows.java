/* 
 * EDDTableAggregateRows Copyright 2016, NOAA.
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
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import gov.noaa.pfel.erddap.DasDds;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;


/** 
 * This class creates an EDDTable by aggregating a list of EDDTables
 * that have the same variables.
 *
 * <p>This doesn't have generateDatasetsXml.  Use the appropriate
 * EDDTableFromX.generateDatasetsXml for each child, 
 * then wrap them up with EDDTableAggregateRows.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2016-02-19
 */
public class EDDTableAggregateRows extends EDDTable{ 

    protected int nChildren;
    protected EDDTable children[];  //[c] is null if local fromErddap
    //If oChildren[c] is a fromErddap dataset from this ERDDAP, 
    //children[c] will be kept as null and always refreshed from 
    //erddap.tableDatasetHashMap.get(localChildrenID).
    private Erddap erddap; 
    private String localChildrenID[]; //[c] is null if NOT local fromErddap

    /**
     * This constructs an EDDTableAggregateRows based on the information in an .xml file.
     * 
     * @param tErddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableAggregateRows"&gt; 
     *    having just been read.  
     * @return an EDDTableAggregateRows.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableAggregateRows fromXml(Erddap tErddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableAggregateRows(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        ArrayList<EDDTable> tChildren = new ArrayList();
        Attributes tAddGlobalAttributes = null;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        int tUpdateEveryNMillis = 0;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;

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
                            "type=\"" + tType + "\" is not allowed for the dataset within the EDDTableAggregateRows. " +
                            "The type MUST start with \"EDDTable\".");
                    tChildren.add((EDDTable)EDD.fromXml(tErddap, tType, xmlReader));
                }

            } else if (localTags.equals( "<accessibleTo>")) {}
            //accessibleTo overrides any child accessibleTo
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
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
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else if (localTags.equals("<addAttributes>")) {
                tAddGlobalAttributes = getAttributesFromXml(xmlReader);
            } else {
                xmlReader.unexpectedTagException();
            }
        }

        int nChildren = tChildren.size();
        EDDTable ttChildren[] = new EDDTable[nChildren];
        for (int i = 0; i < nChildren; i++)
            ttChildren[i] = tChildren.get(i);
        
        return new EDDTableAggregateRows(tErddap, tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
            tReloadEveryNMinutes, tUpdateEveryNMillis, 
            ttChildren);
    }

    /**
     * The constructor.
     *
     * @throws Throwable if trouble
     */
    public EDDTableAggregateRows(Erddap tErddap, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery,
        Attributes tAddGlobalAttributes,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis, 
        EDDTable oChildren[]) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableAggregateRows " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableAggregateRows(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableAggregateRows"; 
        datasetID = tDatasetID;
        if (datasetID == null || datasetID.length() == 0)
            throw new RuntimeException(errorInMethod + "The datasetID wasn't specified.");
        if (oChildren == null || oChildren.length == 0)
            throw new RuntimeException(errorInMethod + "The EDDTable children datasets are missing.");
        nChildren = oChildren.length;
        children = new EDDTable[nChildren];      //the instance datasets if not local fromErddap
        localChildrenID = new String[nChildren]; //the instance datasetIDs if   local fromErddap
        EDDTable tChildren[] = new EDDTable[nChildren]; //stable local instance
        int ndv = -1;
        for (int c = 0; c < nChildren; c++) {
            if (oChildren[c] == null)
                throw new RuntimeException(errorInMethod + 
                    "The child dataset[" + c + "] is null.");
            if (datasetID.equals(oChildren[c].datasetID())) 
                throw new RuntimeException(errorInMethod + 
                    "The child dataset[" + c + "] datasetID=" + datasetID + 
                    " must be different from the EDDTableAggregateRows's datasetID.");     
            if (tErddap != null && oChildren[c] instanceof EDDTableFromErddap) {
                EDDTableFromErddap tFromErddap = (EDDTableFromErddap)oChildren[c];
                String tSourceUrl = tFromErddap.getPublicSourceErddapUrl();
                if (tSourceUrl.startsWith(EDStatic.baseUrl) ||   //normally
                    tSourceUrl.startsWith("http://127.0.0.1") ||
                    tSourceUrl.startsWith("http://localhost")) { //happens during testing
                    String lcdid = File2.getNameNoExtension(tSourceUrl);
                    if (datasetID.equals(lcdid))
                        throw new RuntimeException(errorInMethod + 
                            ": The parent datasetID must not be the same as the localChild's datasetID.");
                    EDDTable lcd = tErddap.tableDatasetHashMap.get(lcdid);
                    if (lcd != null) {
                        //yes, use child dataset from local Erddap
                        //The local dataset will be re-gotten from erddap.gridDatasetHashMap
                        //  whenever it is needed.
                        if (reallyVerbose) String2.log("  found/using localChildDatasetID=" + lcdid);
                        tChildren[c] = lcd;
                        erddap = tErddap;
                        localChildrenID[c] = lcdid;
                    }
                }
            }
            if (tChildren[c] == null) {
                //no, use the  oChildren[c] supplied to this constructor
                children[c]  = oChildren[c]; //set instance variable
                tChildren[c] = oChildren[c];        
            }
            int cndv = tChildren[c].dataVariables.length;
            if (c == 0) {
                ndv = cndv;
            } else if (ndv != cndv) {
                throw new RuntimeException(errorInMethod + 
                    "Child dataset[" + c + "] datasetID=" + datasetID + 
                    " has a diffent number of variables (" + cndv + 
                    ") than the first child (" + ndv + ").");     
            }
        }
        //for rest of constructor, use temporary, stable tChildren reference.

        setAccessibleTo(tAccessibleTo); 
        setGraphsAccessibleTo(tGraphsAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        setUpdateEveryNMillis(tUpdateEveryNMillis); 

        //get most info from child0
        EDDTable child0 = tChildren[0];

        //global attributes
        sourceGlobalAttributes   = child0.combinedGlobalAttributes;
        addGlobalAttributes      = tAddGlobalAttributes == null? 
            new Attributes() : tAddGlobalAttributes;
        combinedGlobalAttributes 
            = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");
       
        //specify what sourceCanConstrain
        //but irrelevant for this class -- 
        //  each child will parse and deal with user request its own way
        sourceCanConstrainNumericData = child0.sourceCanConstrainNumericData;
        sourceCanConstrainStringData  = child0.sourceCanConstrainStringData;
        sourceCanConstrainStringRegex = child0.sourceCanConstrainStringRegex;

        //quickRestart is handled by tChildren

        //dataVariables  (axisVars in reverse order, then dataVars)
        dataVariables = new EDV[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            //variables in this class just see the destination name/type/... of the child0 variable
            EDV childVar = child0.dataVariables[dv];
            String tSourceName     = childVar.destinationName();     
            Attributes tSourceAtts = childVar.combinedAttributes(); 
            Attributes tAddAtts    = new Attributes();
            String tDataType       = childVar.destinationDataType();
            String tUnits          = childVar.units();
            double tMV             = childVar.destinationMissingValue();
            double tFV             = childVar.destinationFillValue();
            double tMin            = childVar.destinationMin(); 
            double tMax            = childVar.destinationMax();

            //ensure all tChildren are consistent
            for (int c = 1; c < nChildren; c++) {
                EDV cEdv = tChildren[c].dataVariables[dv];
                String cName = cEdv.destinationName();
                if (!Test.equal(tSourceName, cName))
                    throw new RuntimeException(errorInMethod + 
                        "child dataset[" + c + "]=" + tChildren[c].datasetID() +
                        " variable #" + dv + " destinationName=" + cName + 
                        " should have destinationName=" + tSourceName + ".");

                String hasADifferent = errorInMethod + 
                        "child dataset[" + c + "]=" + tChildren[c].datasetID() +
                        " variable=" + tSourceName + 
                        " has a different ";
                String than = " than the same variable in child dataset[0]=" + 
                        tChildren[0].datasetID() + " ";

                String cDataType = cEdv.destinationDataType();
                if (!Test.equal(tDataType, cDataType))
                    throw new RuntimeException(hasADifferent + 
                        "dataType=" + cDataType + than +
                        "dataType=" + tDataType + ".");
                String cUnits = cEdv.units();
                if (!Test.equal(tUnits, cUnits))
                    throw new RuntimeException(hasADifferent +  
                        "units=" + cUnits + than +
                        "units=" + tUnits + ".");
                double cMV = cEdv.destinationMissingValue();
                if (!Test.equal(tMV, cMV)) //9 digits
                    throw new RuntimeException(hasADifferent +
                        "missing_value=" + cMV + than +
                        "missing_value=" + tMV + ".");
                double cFV = cEdv.destinationFillValue();
                if (!Test.equal(tFV, cFV)) //9 digits
                    throw new RuntimeException(hasADifferent +
                        "_FillValue=" + cFV + than +
                        "_FillValue=" + tFV + ".");

                //and get the minimum min and maximum max
                tMin = Math.min(tMin, cEdv.destinationMin()); //if either is NaN -> NaN
                tMax = Math.max(tMax, cEdv.destinationMax()); //if either is NaN -> NaN
            }

            //make the variable
            EDV newVar = null;
            if (tSourceName.equals(EDV.LON_NAME)) {
                newVar = new EDVLon(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                lonIndex = dv;
            } else if (tSourceName.equals(EDV.LAT_NAME)) {
                newVar = new EDVLat(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                latIndex = dv;
            } else if (tSourceName.equals(EDV.ALT_NAME)) {
                newVar = new EDVAlt(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                altIndex = dv;
            } else if (tSourceName.equals(EDV.DEPTH_NAME)) {
                newVar = new EDVDepth(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                depthIndex = dv;
            } else if (tSourceName.equals(EDV.TIME_NAME)) { //do time before timeStamp
                tAddAtts.add("data_min", "" + tMin); //data_min/max have priority                
                tAddAtts.add("data_max", "" + tMax); //tMin tMax are epochSeconds    
                newVar = new EDVTime(tSourceName, tSourceAtts, tAddAtts, 
                    tDataType); //this constructor gets source / sets destination actual_range
                timeIndex = dv;
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtts, tAddAtts)) {
                tAddAtts.add("data_min", "" + tMin); //data_min/max have priority                
                tAddAtts.add("data_max", "" + tMax); //tMin tMax are epochSeconds    
                newVar = new EDVTimeStamp(tSourceName, "", tSourceAtts, tAddAtts,
                    tDataType); //this constructor gets source / sets destination actual_range
            } else newVar = new EDV(tSourceName, "", tSourceAtts, tAddAtts, tDataType, tMin, tMax);

            newVar.setActualRangeFromDestinationMinMax();
            dataVariables[dv] = newVar;
        }

        //ensure the setup is valid
        ensureValid();

        //If the child is a FromErddap, try to subscribe to the remote dataset.
        for (int c = 0; c < nChildren; c++) {
            if (tChildren[c] instanceof FromErddap) 
                tryToSubscribeToChildFromErddap(tChildren[0]);
        }

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableAggregateRows " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
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

        //update the children
        boolean anyChange = false;
        int ndv = dataVariables.length;
        double tMin[] = new double[ndv]; Arrays.fill(tMin, Double.NaN);
        double tMax[] = new double[ndv]; Arrays.fill(tMax, Double.NaN);
        for (int c = 0; c < nChildren; c++) {
            EDDTable tChild = getChild(c);
            if (tChild.lowUpdate(msg, startUpdateMillis))
                anyChange = true;

            //just update all dataVariable's destinationMin/Max
            for (int dvi = 0; dvi < ndv; dvi++) {
                EDV cEdv = tChild.dataVariables[dvi]; 
                if (c == 0) {
                    tMin[dvi] = cEdv.destinationMin();
                    tMax[dvi] = cEdv.destinationMax();
                } else {
                    tMin[dvi] = Math.min(tMin[dvi], cEdv.destinationMin()); //if either is NaN -> NaN
                    tMax[dvi] = Math.max(tMax[dvi], cEdv.destinationMax()); //if either is NaN -> NaN
                }
            }
        }
        for (int dvi = 0; dvi < ndv; dvi++) {
            dataVariables[dvi].setDestinationMinMax(tMin[dvi], tMax[dvi]);
            dataVariables[dvi].setActualRangeFromDestinationMinMax();
        }


        return anyChange;
    }

    /** This gets the specified child dataset, whether local fromErddap or otherwise. */
    private EDDTable getChild(int c) {
        EDDTable tChild = children[c];
        if (tChild == null) {
            tChild = erddap.tableDatasetHashMap.get(localChildrenID[c]);
            if (tChild == null) {
                EDD.requestReloadASAP(localChildrenID[c]);
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(underlying local child[" + c + "] datasetID=" + localChildrenID[c] + 
                    " not found)"); 
            }
        }
        return tChild;
    }


   /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //tableWriter
        tableWriter.ignoreFinish = true;

        //pass the request to each child, and accumulate the results
        for (int c = 0; c < nChildren; c++) {
            try {
                //get data from this child. It's nice that:
                //* each child can parse and handle parts its own way
                //  e.g., sourceCanConstrainStringData
                //* each child handles standardizeResultsTable and applies constraints.
                //* each child has the same destination names and units for the same vars.
                //* this will call tableWriter.finish(), but it will be ignored
                getChild(c).getDataForDapQuery(EDStatic.loggedInAsSuperuser, 
                    requestUrl, userDapQuery, tableWriter); 

                //no more data?
                if (tableWriter.noMoreDataPlease)
                    break;

            } catch (Throwable t) {
                //no results is okay
                String msg = t.toString();
                if (msg.indexOf(MustBe.THERE_IS_NO_DATA) >= 0)
                    continue;

                //rethrow, including WaitThenTryAgainException
                throw t;
            }
        }

        //finish
        tableWriter.ignoreFinish = false;
        tableWriter.finish();
    }



    /**
     */
    public static void testBasic() throws Throwable {
        String2.log("\nEDDTableAggregateRows.testBasic()");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = false; //normally false.  Set it to true if need help.
        String results, query, tName, expected;
        //DasDds.main(new String[]{"miniNdbc410", "-verbose"});    
        String id = "miniNdbc410";
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, id);
        String dir = EDStatic.fullTestCacheDirectory;
        int tPo;

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "1", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Attributes {\n" +
" s {\n" +
"  station {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station Name\";\n" +
"  }\n" +
"  prefix {\n" +
"    Float32 actual_range 4102.0, 4103.0;\n" + //important test that it's from all children
"    String comment \"fixed value\";\n" +
"    String ioos_category \"Other\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -80.41, -75.402;\n" + //important test that it's from all children
"    String axis \"X\";\n" +
"    String comment \"The longitude of the station.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 32.28, 35.006;\n" +
"    String axis \"Y\";\n" +
"    String comment \"The latitude of the station.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.048878e+9, 1.4220504e+9;\n" +
"    String axis \"T\";\n" +
"    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  wd {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 0, 359;\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Direction\";\n" +
"    Int16 missing_value 32767;\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  wspd {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 96.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  atmp {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -5.9, 35.9;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  wtmp {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"SST\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station, prefix, longitude, latitude\";\n" +
"    String contributor_name \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"    String contributor_role \"Source of data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"dave.foley@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_url \"http://coastwatch.pfeg.noaa.gov\";\n" +
"    Float64 Easternmost_Easting -75.402;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 35.006;\n" +
"    Float64 geospatial_lat_min 32.28;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -75.402;\n" +
"    Float64 geospatial_lon_min -80.41;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NOAA NDBC\n";
//2016-02-24T16:48:35Z http://www.ndbc.noaa.gov/
//2016-02-24T16:48:35Z http://localhost:8080/cwexperimental/tabledap/miniNdbc410.das";";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      

expected = 
    "String infoUrl \"http://www.ndbc.noaa.gov/\";\n" +
"    String institution \"NOAA NDBC, CoastWatch WCN\";\n" +
"    String keywords \"keyword1, keyword2\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    String NDBCMeasurementDescriptionUrl \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
"    Float64 Northernmost_Northing 35.006;\n" +
"    String project \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"    String quality \"Automated QC checks with periodic manual QC\";\n" +
"    String source \"station observation\";\n" +
"    String sourceUrl \"http://www.ndbc.noaa.gov/\";\n" +
"    Float64 Southernmost_Northing 32.28;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"station, prefix, longitude, latitude\";\n" +
"    String summary \"miniNdbc summary\";\n" +
"    String time_coverage_end \"2015-01-23T22:00:00Z\";\n" +
"    String time_coverage_resolution \"P1H\";\n" +
"    String time_coverage_start \"2003-03-28T19:00:00Z\";\n" +
"    String title \"NDBC Standard Meteorological Buoy Data\";\n" +
"    Float64 Westernmost_Easting -80.41;\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf("String infoUrl ");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo), expected, "results=\n" + results);      

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "2", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 prefix;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query station info
        query = "prefix,station,latitude,longitude&distinct()";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "stationInfo", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"prefix,station,latitude,longitude\n" +
"m,,degrees_north,degrees_east\n" +
"4102.0,41024,33.848,-78.489\n" +
"4102.0,41025,35.006,-75.402\n" +
"4102.0,41029,32.81,-79.63\n" +
"4103.0,41033,32.28,-80.41\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query station info, lon<-80
        query = "prefix,station,latitude,longitude&distinct()&longitude<-80";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "stationInfoLT", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"prefix,station,latitude,longitude\n" +
"m,,degrees_north,degrees_east\n" +
"4103.0,41033,32.28,-80.41\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query station info, lon>-80
        query = "prefix,station,latitude,longitude&distinct()&longitude>-80";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "stationInfoGT", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"prefix,station,latitude,longitude\n" +
"m,,degrees_north,degrees_east\n" +
"4102.0,41024,33.848,-78.489\n" +
"4102.0,41025,35.006,-75.402\n" +
"4102.0,41029,32.81,-79.63\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query data
        query = "&time=2014-01-01";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "data", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
"41024,4102.0,-78.489,33.848,2014-01-01T00:00:00Z,320,2.0,10.3,NaN\n" +
"41025,4102.0,-75.402,35.006,2014-01-01T00:00:00Z,305,7.0,11.8,23.3\n" +
"41029,4102.0,-79.63,32.81,2014-01-01T00:00:00Z,340,3.0,17.4,NaN\n" +
"41033,4103.0,-80.41,32.28,2014-01-01T00:00:00Z,350,3.0,12.2,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query data, lon<-80
        query = "&time=2014-01-01&longitude<-80";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "dataLT", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
"41033,4103.0,-80.41,32.28,2014-01-01T00:00:00Z,350,3.0,12.2,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query data, lon >-80
        query = "&time=2014-01-01&longitude>-80";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "dataGT", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
"41024,4102.0,-78.489,33.848,2014-01-01T00:00:00Z,320,2.0,10.3,NaN\n" +
"41025,4102.0,-75.402,35.006,2014-01-01T00:00:00Z,305,7.0,11.8,23.3\n" +
"41029,4102.0,-79.63,32.81,2014-01-01T00:00:00Z,340,3.0,17.4,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query data, prefix=4103
        query = "&time=2014-01-01&prefix=4103";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "data4103", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
"41033,4103.0,-80.41,32.28,2014-01-01T00:00:00Z,350,3.0,12.2,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query data, prefix=4102
        query = "&time=2014-01-01&prefix=4102";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "data4102", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
"41024,4102.0,-78.489,33.848,2014-01-01T00:00:00Z,320,2.0,10.3,NaN\n" +
"41025,4102.0,-75.402,35.006,2014-01-01T00:00:00Z,305,7.0,11.8,23.3\n" +
"41029,4102.0,-79.63,32.81,2014-01-01T00:00:00Z,340,3.0,17.4,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //test metadata is from child0: query data, prefix=4103
        query = "&time=2014-01-01&prefix=4103";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "data4103", ".nc"); 
        results = NcHelper.dumpString(dir + tName, true);
        expected = 
    "short wd(row=1);\n" + //1 row
"      :_FillValue = 32767S; // short\n" +
"      :actual_range = 350S, 350S; // short\n" +  //up-to-date
"      :colorBarMaximum = 360.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :comment = \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Wind Direction\";\n" +
"      :missing_value = 32767S; // short\n" +
"      :standard_name = \"wind_from_direction\";\n" +
"      :units = \"degrees_true\";\n";
        tPo = results.indexOf("short wd(");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "results=\n" + results);      

        //test metadata is from child0: query data, prefix=4102
        query = "&time=2014-01-01&prefix=4102";
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "data4102", ".nc"); 
        results = NcHelper.dumpString(dir + tName, true);
        expected = 
    "short wd(row=3);\n" +  //3 rows
"      :_FillValue = 32767S; // short\n" +
"      :actual_range = 305S, 340S; // short\n" +  //up-to-date
"      :colorBarMaximum = 360.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :comment = \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Wind Direction\";\n" +
"      :missing_value = 32767S; // short\n" +
"      :standard_name = \"wind_from_direction\";\n" +
"      :units = \"degrees_true\";\n";
        tPo = results.indexOf("short wd(");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
            query = "&longitude<-90"; //quick reject
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "errorLT90", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
            String2.log("Expected error:\n" + MustBe.throwableToString(t));
        }
        expected = "com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude<-90 is outside of the variable's actual_range: -80.41 to -75.402)";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
            query = "&wtmp=23.12345"; //look through every row of data: no matching data
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "errorWtmp", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
            String2.log("Expected error:\n" + MustBe.throwableToString(t));
        }
        expected = "com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)";
        Test.ensureEqual(results, expected, "results=\n" + results);      

           
        debugMode = oDebugMode;
        String2.log("\n*** EDDTableAggregateRows.testBasic finished successfully.");
        /* */
    }


     
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableAggregateRows.test() *****************\n");
        testVerboseOn();

/* */
        //always done        
        testBasic();
    }

}
