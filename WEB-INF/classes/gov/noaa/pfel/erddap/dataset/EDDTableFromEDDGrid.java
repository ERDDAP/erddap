/* 
 * EDDTableFromEDDGrid Copyright 2013, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
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
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;


/** 
 * This class creates an EDDTable from an EDDGrid.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2013-03-08
 */
public class EDDTableFromEDDGrid extends EDDTable{ 

    public final static int DEFAULT_MAX_AXIS0 = 10; 
    protected int maxAxis0; //if <=0, no limit 

    //If childDataset is a fromErddap dataset from this ERDDAP, 
    //childDataset will be kept as null and always refreshed from 
    //erddap.gridDatasetHashMap.get(localChildDatasetID).
    protected EDDGrid childDataset;
    private Erddap erddap; 
    private String localChildDatasetID;

    /**
     * This constructs an EDDTableFromEDDGrid based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromEDDGrid"&gt; 
     *    having just been read.  
     * @return an EDDTableFromEDDGrid.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromEDDGrid fromXml(Erddap tErddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromEDDGrid(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        EDDGrid tChildDataset = null;
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
                    if (tChildDataset == null) {
                        String tType = xmlReader.attributeValue("type");
                        if (tType == null || !tType.startsWith("EDDGrid"))
                            throw new SimpleException(
                                "type=\"" + tType + "\" is not allowed for the dataset within the EDDTableFromEDDGrid. " +
                                "The type MUST start with \"EDDGrid\".");
                        tChildDataset = (EDDGrid)EDD.fromXml(tErddap, tType, xmlReader);
                    } else {
                        throw new RuntimeException("Datasets.xml error: " +
                            "There can be only one <dataset> defined within an " +
                            "EDDGridFromEDDTableo <dataset>.");
                    }
                }

            } else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
//updateEveryNMillis isn't supported (ever?). Rely on EDDGrid's update system.
//            else if (localTags.equals( "<updateEveryNMillis>")) {}
//            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis = String2.parseInt(content); 
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

        return new EDDTableFromEDDGrid(tErddap, tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
            tReloadEveryNMinutes, //tUpdateEveryNMillis, 
            tChildDataset);
    }

    /**
     * The constructor.
     *
     * @throws Throwable if trouble
     */
    public EDDTableFromEDDGrid(Erddap tErddap, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery,
        Attributes tAddGlobalAttributes,
        int tReloadEveryNMinutes, //int tUpdateEveryNMillis, 
        EDDGrid oChildDataset) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromEDDGrid " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromEDDGrid(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromEDDGrid"; 
        datasetID = tDatasetID;
        Test.ensureFileNameSafe(datasetID, "datasetID");
        setAccessibleTo(tAccessibleTo); 
        setGraphsAccessibleTo(tGraphsAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        //setUpdateEveryNMillis(tUpdateEveryNMillis); //not supported yet (ever?). Rely on EDDGrid's update system.

        if (oChildDataset == null)
            throw new SimpleException(errorInMethod + "The child EDDGrid dataset is missing.");
        if (datasetID.equals(oChildDataset.datasetID()))
            throw new RuntimeException(errorInMethod + 
                "This dataset's datasetID must not be the same as the child's datasetID.");

        //is oChildDataset a fromErddap from this erddap?
        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = null;
        if (tErddap != null && oChildDataset instanceof EDDGridFromErddap) {
            EDDGridFromErddap tFromErddap = (EDDGridFromErddap)oChildDataset;
            String tSourceUrl = tFromErddap.getPublicSourceErddapUrl();
            if (EDStatic.urlIsThisComputer(tSourceUrl)) { 
                String lcdid = File2.getNameNoExtension(tSourceUrl);
                if (datasetID.equals(lcdid))
                    throw new RuntimeException(errorInMethod + 
                        "This dataset's datasetID must not be the same as the localChild's datasetID.");
                EDDGrid lcd = tErddap.gridDatasetHashMap.get(lcdid);
                if (lcd != null) {
                    //yes, use child dataset from this erddap
                    //The local dataset will be re-gotten from erddap.gridDatasetHashMap
                    //  whenever it is needed.
                    if (reallyVerbose) String2.log("  found/using localChildDatasetID=" + lcdid);
                    tChildDataset = lcd;
                    erddap = tErddap;
                    localChildDatasetID = lcdid;
                }
            }
        }
        if (tChildDataset == null) {
            //no, use the oChildDataset supplied to this constructor
            childDataset  = oChildDataset; //set instance variable
            tChildDataset = oChildDataset;        
        }
        //for rest of constructor, use temporary, stable tChildDataset reference.

        //global attributes
        localSourceUrl           = tChildDataset.localSourceUrl;
        sourceGlobalAttributes   = tChildDataset.combinedGlobalAttributes;
        addGlobalAttributes      = tAddGlobalAttributes == null? new Attributes() : tAddGlobalAttributes;
        //cdm_data_type=TimeSeries would be nice, but there is no timeseries_id variable
        addGlobalAttributes.add("cdm_data_type", 
            (tChildDataset.lonIndex() >= 0 && tChildDataset.latIndex() >= 0)? "Point" : "Other");
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("\"null\"");
        
        maxAxis0 = combinedGlobalAttributes.getInt("maxAxis0");
        if (maxAxis0 == Integer.MAX_VALUE)
            maxAxis0 = DEFAULT_MAX_AXIS0;        

        //specify what sourceCanConstrain
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //axisVars yes, dataVars no; best to have it doublecheck
        sourceCanConstrainStringData  = CONSTRAIN_NO; 
        sourceCanConstrainStringRegex = ""; 

        //quickRestart is handled by contained tChildDataset

        //dataVariables  (axisVars in reverse order, then dataVars)
        int tChildDatasetNAV = tChildDataset.axisVariables.length;
        int tChildDatasetNDV = tChildDataset.dataVariables.length;
        dataVariables = new EDV[tChildDatasetNAV + tChildDatasetNDV];
        for (int dv = 0; dv < dataVariables.length; dv++) {
            //variables in this class just see the destination name/type/... of the tChildDataset variable
            EDV gridVar = dv < tChildDatasetNAV?  tChildDataset.axisVariables[tChildDatasetNAV - 1 - dv] :
                                                  tChildDataset.dataVariables[dv - tChildDatasetNAV];
            String tSourceName     = gridVar.destinationName();     
            Attributes tSourceAtts = gridVar.combinedAttributes(); 
            Attributes tAddAtts    = new Attributes();
            String tDataType       = gridVar.destinationDataType();
            double tMin            = gridVar.destinationMin(); 
            double tMax            = gridVar.destinationMax();
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
            } else if (tSourceName.equals(EDV.TIME_NAME)) {                
                tAddAtts.add("data_min", "" + tMin); //data_min/max have priority                
                tAddAtts.add("data_max", "" + tMax); //tMin tMax are epochSeconds    
                newVar = new EDVTime(tSourceName, tSourceAtts, tAddAtts, 
                    tDataType); //this constructor gets source / sets destination actual_range
                timeIndex = dv;
            //currently, there is no EDVTimeStampGridAxis
            } else newVar = new EDV(tSourceName, "", tSourceAtts, tAddAtts, tDataType, tMin, tMax);

            dataVariables[dv] = newVar;
        }

        //ensure the setup is valid
        ensureValid();

        //If the child is a FromErddap, try to subscribe to the remote dataset.
        if (tChildDataset instanceof FromErddap) 
            tryToSubscribeToChildFromErddap(tChildDataset);

        //finally
        if (verbose) String2.log(
            (debugMode? "\n" + toString() : "") +
            "\n*** EDDTableFromEDDGrid " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "ms\n"); 

    }

    /**
     * This returns the childDataset (if not null) or the localChildDataset.
     */
    public EDDGrid getChildDataset() {
        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = childDataset;
        if (tChildDataset == null) {
            tChildDataset = erddap.gridDatasetHashMap.get(localChildDatasetID);
            if (tChildDataset == null) {
                EDD.requestReloadASAP(localChildDatasetID);
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(underlying local datasetID=" + localChildDatasetID + " not found)"); 
            }
        }
        return tChildDataset;
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

        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = getChildDataset();

        //update the internal childDataset
        return tChildDataset.lowUpdate(msg, startUpdateMillis);
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

        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = getChildDataset();
        //String2.log("\n>> getSourceData tChildDataset type=" + tChildDataset.className);

        //parse the userDapQuery into a DESTINATION query
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        //parseUserDapQuery, not getSourceQueryFromDapQuery.  Get DESTINATION resultsVars+constraints.
        parseUserDapQuery(userDapQuery,  
            resultsVariables,  //doesn't catch constraintVariables that aren't in users requested results
            constraintVariables, constraintOps, constraintValues, false); //repair
            //Non-regex EDVTimeStamp constraintValues will be returned as epochSeconds

        //*** generate the gridDapQuery

        //find (non-regex) min and max of constraints on axis variables 
        //work backwards since deleting some
        EDVGridAxis childDatasetAV[] = tChildDataset.axisVariables();
        EDV         childDatasetDV[] = tChildDataset.dataVariables();
        int childDatasetNAV = childDatasetAV.length;
        int childDatasetNDV = childDatasetDV.length;
        //min and max desired axis destination values
        double avMin[] = new double[childDatasetNAV];  
        double avMax[] = new double[childDatasetNAV];
        for (int av = 0; av < childDatasetNAV; av++) {
            avMin[av] = childDatasetAV[av].destinationMin();  //time is epochSeconds
            avMax[av] = childDatasetAV[av].destinationMax();
        }
        boolean hasAvConstraints = false; //only true if constraints are more than av min max
        StringArray constraintsDvNames = new StringArray();  //unique
        for (int c = 0; c < constraintVariables.size(); c++) { 
            String conVar = constraintVariables.get(c);
            String conOp  = constraintOps.get(c);
            String conVal = constraintValues.get(c);
            int av = String2.indexOf(tChildDataset.axisVariableDestinationNames(), conVar);
            if (av < 0) {
                if (constraintsDvNames.indexOf(conVar) < 0)  //if not already in the list
                    constraintsDvNames.add(conVar);
                continue;
            }
            EDVGridAxis edvga = childDatasetAV[av];
            if (conOp.equals(PrimitiveArray.REGEX_OP)) {
                //FUTURE: this could be improved to find the range of matching axis values
            } else {
                boolean avIsTimeStamp = edvga instanceof EDVTimeStampGridAxis;
                double oldAvMin = avMin[av];
                double oldAvMax = avMax[av];
                double conValD = String2.parseDouble(conVal);
                boolean passed = true; //look for some aspect that doesn't pass
                if (!conOp.equals("!=") && Double.isNaN(conValD)) {
                    passed = false;
                } else {
                    // > >= < <= (and =somethingOutOfRange) were tested in EDDTable.parseUserDapQuery
                    if (conOp.equals("=")) {
                        int si = edvga.destinationToClosestSourceIndex(conValD);
                        if (si < 0)
                            passed = false;
                        else {
                            double destVal = edvga.destinationValue(si).getDouble(0);
                            passed = avIsTimeStamp?
                                conValD == destVal : //exact
                                Math2.almostEqual(5, conValD, destVal); //fuzzy, biased towards passing                                    
                            avMin[av] = Math.max(avMin[av], conValD);
                            avMax[av] = Math.min(avMax[av], conValD);
                        }
                    } else if (conOp.equals("!=")) {
                        //not very useful
                    } 
                }
                if (!passed) 
                    throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (" + 
                        MessageFormat.format(EDStatic.queryErrorNeverTrue, 
                            conVar + conOp + conValD) + 
                        ")");

                if (oldAvMin != avMin[av] || oldAvMax != avMax[av])
                    hasAvConstraints = true;
            }               
        }

        //generate the griddap avConstraints
        String avConstraints[] = new String[childDatasetNAV];
        StringBuilder allAvConstraints = new StringBuilder();
        for (int av = 0; av < childDatasetNAV; av++) { 
            EDVGridAxis edvga = childDatasetAV[av];
            //are the constraints impossible?
            if (avMin[av] > avMax[av])
                throw new SimpleException(MustBe.THERE_IS_NO_DATA + " " + 
                    MessageFormat.format(EDStatic.queryErrorNeverTrue, 
                        edvga.destinationName() + ">=" + avMin[av] + " and " +
                        edvga.destinationName() + "<=" + avMax[av]));            
            if (edvga.isAscending())
                avConstraints[av] = "[(" + avMin[av] + "):(" + avMax[av] + ")]";
            else
                avConstraints[av] = "[(" + avMax[av] + "):(" + avMin[av] + ")]";
            allAvConstraints.append(avConstraints[av]);
        }
        if (debugMode) String2.log(">>allAvConstraints=" + allAvConstraints);

        //does resultsVariables include any axis or data variables?
        StringArray resultsAvNames = new StringArray();
        StringArray resultsDvNames = new StringArray();
        for (int rv = 0; rv < resultsVariables.size(); rv++) {
            String rvName = resultsVariables.get(rv);
            if (String2.indexOf(tChildDataset.axisVariableDestinationNames(), rvName) >= 0) 
                resultsAvNames.add(rvName);
            if (String2.indexOf(tChildDataset.dataVariableDestinationNames(), rvName) >= 0) 
                resultsDvNames.add(rvName);
        }

        
        StringBuilder gridDapQuery = new StringBuilder();
        String gridRequestUrl = "/griddap/" + tChildDataset.datasetID() + ".dods"; //after EDStatic.baseUrl, before '?'

        if (resultsDvNames.size() > 0 || constraintsDvNames.size() > 0) {
            //handle a request for 1+ data variables (in results or constraint variables)
            //e.g. lat,lon,time,sst&sst>37    or lat,lon,time&sst>37
            for (int dvi = 0; dvi < resultsDvNames.size(); dvi++) {  //the names are unique
                if (gridDapQuery.length() > 0)
                    gridDapQuery.append(',');
                gridDapQuery.append(resultsDvNames.get(dvi) + allAvConstraints);
            }
            for (int dvi = 0; dvi < constraintsDvNames.size(); dvi++) { //the names are unique
                if (resultsDvNames.indexOf(constraintsDvNames.get(dvi)) < 0) {
                    if (gridDapQuery.length() > 0)
                        gridDapQuery.append(',');
                    gridDapQuery.append(constraintsDvNames.get(dvi) + allAvConstraints);
                }
            }
            if (debugMode) String2.log(">>nDvNames > 0, gridDapQuery=" + gridDapQuery);
            GridDataAccessor gda = new GridDataAccessor(tChildDataset, gridRequestUrl, 
                gridDapQuery.toString(), true, //rowMajor
                false); //convertToNaN (would be true, but TableWriterSeparatedValue will do it)

            //test maxAxis0
            if (maxAxis0 >= 1) {
                int nAxis0 = gda.totalIndex().shape()[0];
                if (maxAxis0 < nAxis0) {
                    String ax0Name = tChildDataset.axisVariableDestinationNames()[0];
                    throw new SimpleException(MustBe.OutOfMemoryError + 
                        ": Your request for data from " + nAxis0 + " axis[0] (" + ax0Name +
                        ") values exceeds the maximum allowed for this dataset (" + 
                        maxAxis0 + "). Please add tighter constraints on the " +
                        ax0Name + " variable.");      
                }
            }

            EDV queryDV[] = gda.dataVariables();
            int nQueryDV = queryDV.length;
            EDV sourceTableVars[] = new EDV[childDatasetNAV + nQueryDV];
            for (int av = 0; av < childDatasetNAV; av++) 
                sourceTableVars[av] = dataVariables[childDatasetNAV - av - 1];
            for (int dv = 0; dv < nQueryDV; dv++) 
                sourceTableVars[childDatasetNAV + dv] = findDataVariableByDestinationName(
                    queryDV[dv].destinationName()); 

            //make a table to hold a chunk of the results            
            int chunkNRows = EDStatic.partialRequestMaxCells / (childDatasetNAV + nQueryDV);
            Table tTable = makeEmptySourceTable(sourceTableVars, chunkNRows); //source table, but source here is tChildDataset's destination
            PrimitiveArray paAr[] = new PrimitiveArray[tTable.nColumns()];
            for (int col = 0; col < tTable.nColumns(); col++)
                paAr[col] = tTable.getColumn(col);

            //walk through it, periodically saving to tableWriter
            int cumNRows = 0;
            while (gda.increment()) {
                for (int av = 0; av < childDatasetNAV; av++) 
                    //FUTURE: switch to pa.addFromPA(otherPA, otherIndex, nValues);
                    paAr[av].addDouble(gda.getAxisValueAsDouble(av));  
                for (int dv = 0; dv < nQueryDV; dv++) 
                    //FUTURE: switch to pa.addFromPA(otherPA, otherIndex, nValues);
                    paAr[childDatasetNAV + dv].addDouble(gda.getDataValueAsDouble(dv));  
                if (++cumNRows >= chunkNRows) {
                    if (debugMode) String2.log(tTable.dataToString(5));
                    if (Thread.currentThread().isInterrupted())
                        throw new SimpleException("EDDTableFromEDDGrid.getDataForDapQuery" + 
                            EDStatic.caughtInterrupted);      

                    standardizeResultsTable(requestUrl, //applies all constraints
                        userDapQuery, tTable); 
                    tableWriter.writeSome(tTable);
                    tTable = makeEmptySourceTable(sourceTableVars, chunkNRows); 
                    for (int col = 0; col < tTable.nColumns(); col++)
                        paAr[col] = tTable.getColumn(col);
                    cumNRows = 0;
                    if (tableWriter.noMoreDataPlease) {
                        tableWriter.logCaughtNoMoreDataPlease(datasetID);
                        break;
                    }
                }
            }
            gda.releaseResources();

            //finish
            if (tTable.nRows() > 0) {
                standardizeResultsTable(requestUrl,  //applies all constraints
                    userDapQuery, tTable); 
                tableWriter.writeSome(tTable);
            }
            tableWriter.finish();

        } else if (resultsAvNames.size() >= 1) {
            //handle a request for multiple axis variables (no data variables anywhere)
            //gather the active edvga 
            int nActiveEdvga = resultsAvNames.size();
            EDVGridAxis activeEdvga[] = new EDVGridAxis[nActiveEdvga];
            for (int aav = 0; aav < nActiveEdvga; aav++) {
                int av = String2.indexOf(tChildDataset.axisVariableDestinationNames(), 
                    resultsAvNames.get(aav));
                activeEdvga[aav] = childDatasetAV[av];
                if (aav > 0)
                    gridDapQuery.append(',');
                gridDapQuery.append(resultsAvNames.get(aav) + avConstraints[av]);
            }

            //make modifiedUserDapQuery, resultsAvNames + just constraints on results axis vars
            StringBuilder modifiedUserDapQuery = new StringBuilder(resultsAvNames.toCSVString()); 
            for (int c = 0; c < constraintVariables.size(); c++) { 
                String conVar = constraintVariables.get(c);
                String conOp  = constraintOps.get(c);
                String conVal = constraintValues.get(c);
                int rv = resultsAvNames.indexOf(conVar);
                if (rv < 0) 
                    continue;
                int av = String2.indexOf(tChildDataset.axisVariableDestinationNames(), conVar);
                if (av < 0) 
                    continue;
                modifiedUserDapQuery.append("&" + conVar + conOp + 
                    (conOp.equals(PrimitiveArray.REGEX_OP)? String2.toJson(conVal) : conVal));
            }            
            if (debugMode) String2.log(">>resultsAvNames.size() > 1, gridDapQuery=" + gridDapQuery + 
                "\n  modifiedUserDapQuery=" + modifiedUserDapQuery);

            //parse the gridDapQuery
            StringArray tDestinationNames = new StringArray();
            IntArray tConstraints = new IntArray(); //start,stop,stride for each tDestName
            tChildDataset.parseAxisDapQuery(gridDapQuery.toString(), tDestinationNames, 
                tConstraints, false);

            //figure out the shape and make the NDimensionalIndex
            int ndShape[] = new int[nActiveEdvga]; //an array with the size of each active av
            for (int aav = 0; aav < nActiveEdvga; aav++) 
                ndShape[aav] = tConstraints.get(aav*3 + 2) - tConstraints.get(aav*3 + 0) + 1;
            NDimensionalIndex ndIndex = new NDimensionalIndex(ndShape);

            //make a table to hold a chunk of the results
            int chunkNRows = EDStatic.partialRequestMaxCells / nActiveEdvga;
            Table tTable = new Table(); //source table, but source here is tChildDataset's destination
            PrimitiveArray paAr[] = new PrimitiveArray[nActiveEdvga];
            for (int aav = 0; aav < nActiveEdvga; aav++) {
                //this class only sees tChildDataset dest type and destName
                paAr[aav] = PrimitiveArray.factory(activeEdvga[aav].destinationDataTypeClass(), 
                    chunkNRows, false);
                tTable.addColumn(activeEdvga[aav].destinationName(), paAr[aav]); 
            }

            //walk through it, periodically saving to tableWriter
            int cumNRows = 0;
            int current[] = ndIndex.getCurrent();
            while (ndIndex.increment()) {
                for (int aav = 0; aav < nActiveEdvga; aav++) 
                    paAr[aav].addDouble(activeEdvga[aav].destinationDouble( 
                        tConstraints.get(aav*3 + 0) + current[aav]));  //baseIndex + offsetIndex
                if (++cumNRows >= chunkNRows) {
                    if (Thread.currentThread().isInterrupted())
                        throw new SimpleException("EDDTableFromDatabase.getDataForDapQuery" + 
                            EDStatic.caughtInterrupted);
        
                    standardizeResultsTable(requestUrl, //applies all constraints
                        modifiedUserDapQuery.toString(), tTable); 
                    tableWriter.writeSome(tTable);
                    //??? no need for make a new table, colOrder may have changed, but PaAr hasn't
                    tTable.removeAllRows();  
                    cumNRows = 0;
                    if (tableWriter.noMoreDataPlease) {
                        tableWriter.logCaughtNoMoreDataPlease(datasetID);
                        break;
                    }
                }
            }

            //finish
            if (tTable.nRows() > 0) {
                standardizeResultsTable(requestUrl,  //applies all constraints
                    modifiedUserDapQuery.toString(), tTable); 
                tableWriter.writeSome(tTable);
            }
            tableWriter.finish();
            
        } else {        
            //if get here, no results variables?! error should have been thrown before
            throw new SimpleException(EDStatic.queryError + " No results variables?!"); 
        }
    }



    /**
     */
    public static void testBasic() throws Throwable {
        String2.log("\nEDDTableFromEDDGrid.testBasic()");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = false; //normally false.  Set it to true if need help.
        String results, query, tName, expected, expected2;
        String id = "erdMBsstdmday_AsATable";
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, id);
        String dir = EDStatic.fullTestCacheDirectory;

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "1", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 120.0, 320.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -45.0, 65.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.2030768e+9, 1.2056688e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sst {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Point\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"NASA GSFC (G. Feldman)\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_type \"institution\";\n" +
"    String creator_url \"https://www.pfeg.noaa.gov\";\n" +
"    String date_created \"2008-04-04\";\n" +
"    String date_issued \"2008-04-04\";\n" +
"    Float64 Easternmost_Easting 320.0;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 65.0;\n" +
"    Float64 geospatial_lat_min -45.0;\n" +
"    Float64 geospatial_lat_resolution 0.025;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 320.0;\n" +
"    Float64 geospatial_lon_min 120.0;\n" +
"    Float64 geospatial_lon_resolution 0.025;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NASA GSFC (G. Feldman)";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      

expected2 = 
   "String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MB_sstd_las.html\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"altitude, aqua, coast, coastwatch, data, day, daytime, degrees, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, imaging, MBsstd, moderate, modis, national, noaa, node, npp, ocean, oceans, orbiting, pacific, partnership, polar, polar-orbiting, resolution, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, wcn, west\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Int32 maxAxis0 1;\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 65.0;\n" +
"    String origin \"NASA GSFC (G. Feldman)\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String publisher_email \"erd.data@noaa.gov\";\n" +
"    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String publisher_type \"institution\";\n" +
"    String publisher_url \"https://www.pfeg.noaa.gov\";\n" +
"    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/cw_html/OceanColor_NRT_MODIS_Aqua.html .\";\n" +
"    String satellite \"Aqua\";\n" +
"    String sensor \"MODIS\";\n" +
"    String source \"satellite observation: Aqua, MODIS\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -45.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"NOTE: This dataset is the tabular version of a gridded dataset which is also\n" +
"available in this ERDDAP (see datasetID=erdMBsstdmday). Most people, most\n" +
"of the time, will prefer to use the original gridded version of this dataset.\n" +
"This dataset presents all of the axisVariables and dataVariables from the\n" +
"gridded dataset as columns in a huge database-like table of data.\n" +
"You can query this dataset as you would any other tabular dataset in ERDDAP;\n" +
"however, if you query any of the original dataVariables, you must constrain\n" +
"what was axisVariable[0] (usually \\\"time\\\") so that data for no more than 10\n" +
"axisVariable[0] values are searched.\n" +
"\n" +
"---\n" +
"NOAA CoastWatch provides SST data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  Currently, only daytime imagery is supported.\";\n" +
"    String testOutOfDate \"now-60days\";\n" +
"    String time_coverage_end \"2008-03-16T12:00:00Z\";\n" +
"    String time_coverage_start \"2008-02-15T12:00:00Z\";\n" +
"    String title \"SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite), (As A Table)\";\n" +
"    Float64 Westernmost_Easting 120.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf("String infoUrl ");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo), expected2, "results=\n" + results);      

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "2", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 altitude;\n" +
"    Float64 time;\n" +
"    Float32 sst;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query 1 axis
        query = "latitude&latitude>20&latitude<=20.1" +
            "&longitude=200"; //longitude constraint is ignored (since it's valid)
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "1axis", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"latitude\n" +
"degrees_north\n" +
"20.025\n" +
"20.05\n" +
"20.075\n" +
"20.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis =min    lon is 120 - 320
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude=120", 
            dir, tedd.className() + "1axisMin", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"longitude\n" +
"degrees_east\n" +
"120.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <=min 
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude<=120", 
            dir, tedd.className() + "1axisLEMin", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"longitude\n" +
"degrees_east\n" +
"120.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <min fails but not immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude<120", 
                dir, tedd.className() + "1axisLMin", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      

        //query 1 axis <min fails immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude<-119.99", 
                dir, tedd.className() + "1axisLMin2", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude<-119.99 is outside of the variable's actual_range: 120.0 to 320.0)", 
            "results=\n" + results);      


        //query 1 axis =max
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude=320", 
            dir, tedd.className() + "1axisMax", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"longitude\n" +
"degrees_east\n" +
"320.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis >=max 
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude>=320", 
            dir, tedd.className() + "1axisGEMax", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"longitude\n" +
"degrees_east\n" +
"320.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis >max fails but not immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude>320", 
                dir, tedd.className() + "1axisGMax", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      

        //query 1 axis >max fails immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude>320.1", 
                dir, tedd.className() + "1axisGMin2", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude>320.1 is outside of the variable's actual_range: 120.0 to 320.0)", 
            "results=\n" + results);      


        //query time axis =min
        String timeMin = "2008-02-15T12:00:00Z";
        tName = tedd.makeNewFileForDapQuery(null, null, "time&time=" + timeMin, 
            dir, tedd.className() + "timeAxisMin", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"time\n" +
"UTC\n" +
"2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query time axis <=min 
        tName = tedd.makeNewFileForDapQuery(null, null, "time&time<=" + timeMin, 
            dir, tedd.className() + "timeAxisLEMin", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"time\n" +
"UTC\n" +
"2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query time axis <min fails but not immediately   (< is tested as <=)
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "time&time<2008-02-15T12:00:00Z", 
                dir, tedd.className() + "timeAxisLMin", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      

        //query time axis <=min-1day fails immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "time&time<=2008-02-14T12", 
                dir, tedd.className() + "timeAxisLMin2", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(time<=2008-02-14T12:00:00Z is outside of the variable's actual_range: 2008-02-15T12:00:00Z to 2008-03-16T12:00:00Z)", 
            "results=\n" + results);      


        //query time axis >max fails
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "time&time>2008-03-16T12", 
                dir, tedd.className() + "timeAxisGMax", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      



        //query error   
        results = "";
        try {
            query = "latitude&latitude>20&latitude<=20.5" +
                "&longitude=201.04"; //invalid:  in middle of range but no match
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "1axisInvalid", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (longitude=201.04 will never be true.)";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
            query = "latitude&latitude>20&latitude<=20.5" +
                "&longitude=119"; //invalid:  out of actual_range
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "1axisInvalid", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
            "Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude=119 is outside of the variable's actual_range: 120.0 to 320.0)";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 2 axes
        query = "longitude,latitude&latitude>20&latitude<=20.1" +
                "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""; //time constraint is ignored (since it's valid)
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "2axes", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"215.0,20.025\n" +
"215.0,20.05\n" +
"215.0,20.075\n" +
"215.0,20.1\n" +
"215.025,20.025\n" +
"215.025,20.05\n" +
"215.025,20.075\n" +
"215.025,20.1\n" +
"215.05,20.025\n" +
"215.05,20.05\n" +
"215.05,20.075\n" +
"215.05,20.1\n" +
"215.07500000000002,20.025\n" +
"215.07500000000002,20.05\n" +
"215.07500000000002,20.075\n" +
"215.07500000000002,20.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query all axes  (different order)
        query = "latitude,longitude,altitude,time&latitude>20&latitude<=20.1" +
                "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "allaxes", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"20.025,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.025,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.025,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        String2.log("Here Now!");
        try {
            query = "latitude,longitude,altitude,time,chlorophyll&latitude>20&latitude<=20.1" +
                "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""; 
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "1axisInvalid", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
            "Caught: com.cohort.util.SimpleException: Query error: Unrecognized variable=\"chlorophyll\".";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query av and dv, with time constraint
        query = "latitude,longitude,altitude,time,sst&latitude>20&latitude<=20.1" +
                "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "allaxes", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"latitude,longitude,altitude,time,sst\n" +
"degrees_north,degrees_east,m,UTC,degree_C\n" +
"20.025,215.0,0.0,2008-02-15T12:00:00Z,22.4018\n" +
"20.025,215.025,0.0,2008-02-15T12:00:00Z,22.6585\n" +
"20.025,215.05,0.0,2008-02-15T12:00:00Z,22.5025\n" +
"20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.542\n" +
"20.05,215.0,0.0,2008-02-15T12:00:00Z,22.005\n" +
"20.05,215.025,0.0,2008-02-15T12:00:00Z,22.8965\n" +
"20.05,215.05,0.0,2008-02-15T12:00:00Z,22.555\n" +
"20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4825\n" +
"20.075,215.0,0.0,2008-02-15T12:00:00Z,21.7341\n" +
"20.075,215.025,0.0,2008-02-15T12:00:00Z,22.7635\n" +
"20.075,215.05,0.0,2008-02-15T12:00:00Z,22.389\n" +
"20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4805\n" +
"20.1,215.0,0.0,2008-02-15T12:00:00Z,22.5244\n" +
"20.1,215.025,0.0,2008-02-15T12:00:00Z,22.1972\n" +
"20.1,215.05,0.0,2008-02-15T12:00:00Z,22.81\n" +
"20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.6944\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //av+dv query with dv and av constraints
        query = //"latitude,longitude,altitude,time,sst&sst>37&time=\"2008-02-15T12\""); 
                  "latitude,longitude,altitude,time,sst&sst%3E37&time=%222008-02-15T12%22"; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "test17", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"latitude,longitude,altitude,time,sst\n" +
"degrees_north,degrees_east,m,UTC,degree_C\n" +
"-15.175,125.05,0.0,2008-02-15T12:00:00Z,37.17\n" +
"-14.95,124.0,0.0,2008-02-15T12:00:00Z,37.875\n" +
"-14.475,136.825,0.0,2008-02-15T12:00:00Z,38.79\n" +
"-14.45,136.8,0.0,2008-02-15T12:00:00Z,39.055\n" +
"-14.35,123.325,0.0,2008-02-15T12:00:00Z,37.33\n" +
"-14.325,123.325,0.0,2008-02-15T12:00:00Z,37.33\n" +
"-14.3,123.375,0.0,2008-02-15T12:00:00Z,37.73\n" +
"-8.55,164.425,0.0,2008-02-15T12:00:00Z,37.325\n" +
"-7.1,131.075,0.0,2008-02-15T12:00:00Z,39.805\n" +
"-7.1,131.1,0.0,2008-02-15T12:00:00Z,40.775\n" +
"-7.1,131.125,0.0,2008-02-15T12:00:00Z,40.775\n" +
"-7.075,131.025,0.0,2008-02-15T12:00:00Z,37.16\n" +
"-7.075,131.05,0.0,2008-02-15T12:00:00Z,37.935\n" +
"-0.425,314.875,0.0,2008-02-15T12:00:00Z,37.575\n" +
"-0.425,314.90000000000003,0.0,2008-02-15T12:00:00Z,37.575\n" +
"0.175,137.375,0.0,2008-02-15T12:00:00Z,37.18\n" +
"0.225,137.375,0.0,2008-02-15T12:00:00Z,37.665\n" +
"5.35,129.75,0.0,2008-02-15T12:00:00Z,38.46\n" +
"5.35,129.775,0.0,2008-02-15T12:00:00Z,37.54\n";
           
        //av query with dv and av constraints
        query = //"latitude,longitude,altitude,time&latitude>0&sst>37&time=\"2008-02-15T12\""); 
                  "latitude,longitude,altitude,time&latitude%3E0&sst%3E37&time=%222008-02-15T12%22"; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "test18", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"0.175,137.375,0.0,2008-02-15T12:00:00Z\n" +
"0.225,137.375,0.0,2008-02-15T12:00:00Z\n" +
"5.35,129.75,0.0,2008-02-15T12:00:00Z\n" +
"5.35,129.775,0.0,2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //dv query with dv and av constraint
        query = //"sst&sst>40&time=\"2008-02-15T12\""); 
                  "sst&sst%3E40&time=%222008-02-15T12%22"; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "test19", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"sst\n" +
"degree_C\n" +
"40.775\n" +
"40.775\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
           query = //"latitude,longitude,altitude,time&latitude>0&sst>37"); 
                     "latitude,longitude,altitude,time&latitude%3E0&sst%3E37"; 
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "maxAxis0Error", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
            "Caught: com.cohort.util.SimpleException: Out Of Memory Error: Your request for data from " +
            "2 axis[0] (time) values exceeds the maximum allowed for this dataset (1). " +
            "Please add tighter constraints on the time variable.";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        debugMode = oDebugMode;
    }

    /**
     */
    public static void testInErddap() throws Throwable {
        String2.log("\nEDDTableFromEDDGrid.testInErddap()");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = false; //normally false.  Set it to true if need help.
        String results, query, expected, expected2;
        String id = "erdMBsstdmday_AsATable";
        //this needs to test the dataset in ERDDAP, so child is local
        String baseQuery = "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable";
        String dir = EDStatic.fullTestCacheDirectory;


        //das
        results = SSR.getUrlResponseStringNewline(baseQuery + ".das");
        expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 120.0, 320.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -45.0, 65.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.2030768e+9, 1.2056688e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sst {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Point\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"NASA GSFC (G. Feldman)\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_type \"institution\";\n" +
"    String creator_url \"https://www.pfeg.noaa.gov\";\n" +
"    String date_created \"2008-04-04\";\n" +
"    String date_issued \"2008-04-04\";\n" +
"    Float64 Easternmost_Easting 320.0;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 65.0;\n" +
"    Float64 geospatial_lat_min -45.0;\n" +
"    Float64 geospatial_lat_resolution 0.025;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 320.0;\n" +
"    Float64 geospatial_lon_min 120.0;\n" +
"    Float64 geospatial_lon_resolution 0.025;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NASA GSFC (G. Feldman)";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      

expected2 = 
   "String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MB_sstd_las.html\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"altitude, aqua, coast, coastwatch, data, day, daytime, degrees, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, imaging, MBsstd, moderate, modis, national, noaa, node, npp, ocean, oceans, orbiting, pacific, partnership, polar, polar-orbiting, resolution, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, wcn, west\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Int32 maxAxis0 1;\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 65.0;\n" +
"    String origin \"NASA GSFC (G. Feldman)\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String publisher_email \"erd.data@noaa.gov\";\n" +
"    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String publisher_type \"institution\";\n" +
"    String publisher_url \"https://www.pfeg.noaa.gov\";\n" +
"    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/cw_html/OceanColor_NRT_MODIS_Aqua.html .\";\n" +
"    String satellite \"Aqua\";\n" +
"    String sensor \"MODIS\";\n" +
"    String source \"satellite observation: Aqua, MODIS\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -45.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"NOTE: This dataset is the tabular version of a gridded dataset which is also\n" +
"available in this ERDDAP (see datasetID=erdMBsstdmday). Most people, most\n" +
"of the time, will prefer to use the original gridded version of this dataset.\n" +
"This dataset presents all of the axisVariables and dataVariables from the\n" +
"gridded dataset as columns in a huge database-like table of data.\n" +
"You can query this dataset as you would any other tabular dataset in ERDDAP;\n" +
"however, if you query any of the original dataVariables, you must constrain\n" +
"what was axisVariable[0] (usually \\\"time\\\") so that data for no more than 10\n" +
"axisVariable[0] values are searched.\n" +
"\n" +
"---\n" +
"NOAA CoastWatch provides SST data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  Currently, only daytime imagery is supported.\";\n" +
"    String testOutOfDate \"now-60days\";\n" +
"    String time_coverage_end \"2008-03-16T12:00:00Z\";\n" +
"    String time_coverage_start \"2008-02-15T12:00:00Z\";\n" +
"    String title \"SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite), (As A Table)\";\n" +
"    Float64 Westernmost_Easting 120.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf("String infoUrl ");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo), expected2, "results=\n" + results);      

        //das
        results = SSR.getUrlResponseStringNewline(baseQuery + ".dds");
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 altitude;\n" +
"    Float64 time;\n" +
"    Float32 sst;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query 1 axis
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"latitude&latitude>20&latitude<=20.1" +
              "latitude&latitude%3E20&latitude%3C=20.1" +
            "&longitude=200"); //longitude constraint is ignored (since it's valid)
        expected = 
"latitude\n" +
"degrees_north\n" +
"20.025\n" +
"20.05\n" +
"20.075\n" +
"20.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis =min    lon is 120 - 320
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            "longitude&longitude=120"); 
        expected = 
"longitude\n" +
"degrees_east\n" +
"120.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <=min 
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"longitude&longitude<=120"); 
            "longitude&longitude%3C=120"); 
        expected = 
"longitude\n" +
"degrees_east\n" +
"120.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <min fails immediately
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"longitude&longitude<120"); 
                "longitude&longitude%3C120"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString(); //MustBe.throwableToString(t);
        }
        Test.ensureEqual(results, 
            //wanted: "com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3C120\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n" +
"})",
            "results=\n" + results);      

        //query 1 axis <min fails immediately and differently
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"longitude&longitude<-119.99"); 
                "longitude&longitude%3C-119.99"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            //"com.cohort.util.SimpleException: Your query produced no matching results. " +
            //"(longitude<-119.99 is outside of the variable's actual_range: 120.0 to 320.0)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3C-119.99\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (longitude<-119.99 is outside of the variable's actual_range: 120.0 to 320.0)\";\n" +
"})",
            "results=\n" + results);      


        //query 1 axis =max
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            "longitude&longitude=320"); 
        expected = 
"longitude\n" +
"degrees_east\n" +
"320.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis >=max 
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"longitude&longitude>=320"); 
            "longitude&longitude%3E=320"); 
        expected = 
"longitude\n" +
"degrees_east\n" +
"320.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis >max fails but not immediately
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"longitude&longitude>320"); 
                "longitude&longitude%3E320"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3E320\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n" +
"})",
            "results=\n" + results);      

        //query 1 axis >max fails immediately
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"longitude&longitude>320.1"); 
                "longitude&longitude%3E320.1"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            //"(longitude>320.1 is outside of the variable's actual_range: 120.0 to 320.0)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3E320.1\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (longitude>320.1 is outside of the variable's actual_range: 120.0 to 320.0)\";\n" +
"})",
            "results=\n" + results);      


        //query time axis =min
        String timeMin = "2008-02-15T12:00:00Z";
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            "time&time=" + timeMin); 
        expected = 
"time\n" +
"UTC\n" +
"2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query time axis <=min 
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"time&time<=" + timeMin); 
            "time&time%3C=" + timeMin); 
        expected = 
"time\n" +
"UTC\n" +
"2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query time axis <min fails but not immediately   (< is tested as <=)
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"time&time<2008-02-15T12:00:00Z"); 
                "time&time%3C2008-02-15T12:00:00Z"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?time&time%3C2008-02-15T12:00:00Z\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n" +
"})",
            "results=\n" + results);      

        //query time axis <=min-1day fails immediately
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"time&time<=2008-02-14T12"); 
                "time&time%3C=2008-02-14T12"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            //"(time<=2008-02-14T12:00:00Z is outside of the variable's actual_range: 2008-02-15T12:00:00Z to 2008-03-16T12:00:00Z)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?time&time%3C=2008-02-14T12\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (time<=2008-02-14T12:00:00Z is outside of the variable's actual_range: 2008-02-15T12:00:00Z to 2008-03-16T12:00:00Z)\";\n" +
"})",
            "results=\n" + results);      


        //query time axis >max fails
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"time&time>2008-03-16T12"); 
                "time&time%3E2008-03-16T12"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        Test.ensureEqual(results, 
            //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?time&time%3E2008-03-16T12\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n" +
"})",
            "results=\n" + results);      



        //query error   
        results = "";
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"latitude&latitude>20&latitude<=20.5" +
                "latitude&latitude%3E20&latitude%3C=20.5" +
                "&longitude=201.04"); //invalid:  in middle of range but no match
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. (longitude=201.04 will never be true.)";
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude&latitude%3E20&latitude%3C=20.5&longitude=201.04\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (longitude=201.04 will never be true.)\";\n" +
"})";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"latitude&latitude>20&latitude<=20.5" +
                "latitude&latitude%3E20&latitude%3C=20.5" +
                "&longitude=119"); //invalid:  out of actual_range
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
            //"Caught: com.cohort.util.SimpleException: Your query produced no matching results. " +
            //"(longitude=119 is outside of the variable's actual_range: 120.0 to 320.0)";
"Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude&latitude%3E20&latitude%3C=20.5&longitude=119\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. (longitude=119 is outside of the variable's actual_range: 120.0 to 320.0)\";\n" +
"})";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 2 axes
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"longitude,latitude&latitude>20&latitude<=20.1" +
            "longitude,latitude&latitude%3E20&latitude%3C=20.1" +
            //"&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""); //time constraint is ignored (since it's valid)
            "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22"); //time constraint is ignored (since it's valid)
        expected = 
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"215.0,20.025\n" +
"215.0,20.05\n" +
"215.0,20.075\n" +
"215.0,20.1\n" +
"215.025,20.025\n" +
"215.025,20.05\n" +
"215.025,20.075\n" +
"215.025,20.1\n" +
"215.05,20.025\n" +
"215.05,20.05\n" +
"215.05,20.075\n" +
"215.05,20.1\n" +
"215.07500000000002,20.025\n" +
"215.07500000000002,20.05\n" +
"215.07500000000002,20.075\n" +
"215.07500000000002,20.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query all axes  (different order)
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"latitude,longitude,altitude,time&latitude>20&latitude<=20.1" +
            "latitude,longitude,altitude,time&latitude%3E20&latitude%3C=20.1" +
            //"&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""); 
            "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22"); 
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"20.025,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.025,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.025,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.0,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.025,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.05,0.0,2008-02-15T12:00:00Z\n" +
"20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        String2.log("Here 2 Now!");
        try {
            results = "";
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"latitude,longitude,altitude,time,zztop&latitude>20&latitude<=20.1" +
                "latitude,longitude,altitude,time,zztop&latitude%3E20&latitude%3C=20.1" +
                //"&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""); 
                "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
            //"Caught: com.cohort.util.SimpleException: Query error: Unrecognized variable=\"zztop\"";
"Caught: java.io.IOException: HTTP status code=400 for URL: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude,longitude,altitude,time,zztop&latitude%3E20&latitude%3C=20.1&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22\n" +
"(Error {\n" +
"    code=400;\n" +
"    message=\"Bad Request: Query error: Unrecognized variable=\\\"zztop\\\".\";\n" +
"})";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query av and dv, with time constraint
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"latitude,longitude,altitude,time,sst&latitude>20&latitude<=20.1" +
            "latitude,longitude,altitude,time,sst&latitude%3E20&latitude%3C=20.1" +
            //"&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""); 
            "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22"); 
        expected = 
"latitude,longitude,altitude,time,sst\n" +
"degrees_north,degrees_east,m,UTC,degree_C\n" +
"20.025,215.0,0.0,2008-02-15T12:00:00Z,22.4018\n" +
"20.025,215.025,0.0,2008-02-15T12:00:00Z,22.6585\n" +
"20.025,215.05,0.0,2008-02-15T12:00:00Z,22.5025\n" +
"20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.542\n" +
"20.05,215.0,0.0,2008-02-15T12:00:00Z,22.005\n" +
"20.05,215.025,0.0,2008-02-15T12:00:00Z,22.8965\n" +
"20.05,215.05,0.0,2008-02-15T12:00:00Z,22.555\n" +
"20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4825\n" +
"20.075,215.0,0.0,2008-02-15T12:00:00Z,21.7341\n" +
"20.075,215.025,0.0,2008-02-15T12:00:00Z,22.7635\n" +
"20.075,215.05,0.0,2008-02-15T12:00:00Z,22.389\n" +
"20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4805\n" +
"20.1,215.0,0.0,2008-02-15T12:00:00Z,22.5244\n" +
"20.1,215.025,0.0,2008-02-15T12:00:00Z,22.1972\n" +
"20.1,215.05,0.0,2008-02-15T12:00:00Z,22.81\n" +
"20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.6944\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //av+dv query with dv and av constraints
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"latitude,longitude,altitude,time,sst&sst>37&time=\"2008-02-15T12\""); 
            "latitude,longitude,altitude,time,sst&sst%3E37&time=%222008-02-15T12%22"); 
        expected = 
"latitude,longitude,altitude,time,sst\n" +
"degrees_north,degrees_east,m,UTC,degree_C\n" +
"-15.175,125.05,0.0,2008-02-15T12:00:00Z,37.17\n" +
"-14.95,124.0,0.0,2008-02-15T12:00:00Z,37.875\n" +
"-14.475,136.825,0.0,2008-02-15T12:00:00Z,38.79\n" +
"-14.45,136.8,0.0,2008-02-15T12:00:00Z,39.055\n" +
"-14.35,123.325,0.0,2008-02-15T12:00:00Z,37.33\n" +
"-14.325,123.325,0.0,2008-02-15T12:00:00Z,37.33\n" +
"-14.3,123.375,0.0,2008-02-15T12:00:00Z,37.73\n" +
"-8.55,164.425,0.0,2008-02-15T12:00:00Z,37.325\n" +
"-7.1,131.075,0.0,2008-02-15T12:00:00Z,39.805\n" +
"-7.1,131.1,0.0,2008-02-15T12:00:00Z,40.775\n" +
"-7.1,131.125,0.0,2008-02-15T12:00:00Z,40.775\n" +
"-7.075,131.025,0.0,2008-02-15T12:00:00Z,37.16\n" +
"-7.075,131.05,0.0,2008-02-15T12:00:00Z,37.935\n" +
"-0.425,314.875,0.0,2008-02-15T12:00:00Z,37.575\n" +
"-0.425,314.90000000000003,0.0,2008-02-15T12:00:00Z,37.575\n" +
"0.175,137.375,0.0,2008-02-15T12:00:00Z,37.18\n" +
"0.225,137.375,0.0,2008-02-15T12:00:00Z,37.665\n" +
"5.35,129.75,0.0,2008-02-15T12:00:00Z,38.46\n" +
"5.35,129.775,0.0,2008-02-15T12:00:00Z,37.54\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //av query with dv and av constraints
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"latitude,longitude,altitude,time&latitude>0&sst>37&time=\"2008-02-15T12\""); 
            "latitude,longitude,altitude,time&latitude%3E0&sst%3E37&time=%222008-02-15T12%22"); 
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"0.175,137.375,0.0,2008-02-15T12:00:00Z\n" +
"0.225,137.375,0.0,2008-02-15T12:00:00Z\n" +
"5.35,129.75,0.0,2008-02-15T12:00:00Z\n" +
"5.35,129.775,0.0,2008-02-15T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //dv query with dv and av constraint
        results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
            //"sst&sst>40&time=\"2008-02-15T12\""); 
            "sst&sst%3E40&time=%222008-02-15T12%22"); 
        expected = 
"sst\n" +
"degree_C\n" +
"40.775\n" +
"40.775\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //query error   
        results = "";
        try {
            results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" +
                //"latitude,longitude,altitude,time&latitude>0&sst>37"); 
                  "latitude,longitude,altitude,time&latitude%3E0&sst%3E37"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = "Caught: " + t.toString();
        }
        expected = 
"Caught: java.io.IOException: HTTP status code=413 for URL: " +
    "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude,longitude,altitude,time&latitude%3E0&sst%3E37\n" +
"(Error {\n" +
"    code=413;\n" +
"    message=\"Payload Too Large: Out Of Memory Error: Your request for data from 2 axis[0] (time) " +
    "values exceeds the maximum allowed for this dataset (1). Please add tighter constraints on the time variable.\";\n" +
"})";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        debugMode = oDebugMode;
    }



    /** 
     * This runs generateDatasetsXmlFromErddapCatalog.
     * 
     * @param oPublicSourceUrl  A complete URL of an ERDDAP (ending with "/erddap/"). 
     * @param datasetIDRegex  E.g., ".*" for all dataset Names.
     * @return a String with the results
     */
    public static String generateDatasetsXml( 
        String oPublicSourceUrl, String datasetIDRegex, int tMaxAxis0) throws Throwable {

        String2.log("*** EDDTableFromEDDGrid.generateDatasetsXmlFromErddapCatalog(" +
            oPublicSourceUrl + ")");
        if (tMaxAxis0 <= -1 || tMaxAxis0 == Integer.MAX_VALUE)
            tMaxAxis0 = DEFAULT_MAX_AXIS0;

        if (oPublicSourceUrl == null)
            throw new RuntimeException(String2.ERROR + ": 'localSourceUrl' is null.");
        if (!oPublicSourceUrl.endsWith("/erddap/") &&
            !oPublicSourceUrl.endsWith("/cwexperimental/"))
            throw new RuntimeException(String2.ERROR + ": 'localSourceUrl' doesn't end with '/erddap/'.");

        //get table with datasetID minLon, maxLon
        String query = oPublicSourceUrl + "tabledap/allDatasets.csv" + 
            "?datasetID,title,summary,griddap" +
            "&dataStructure=" + SSR.minimalPercentEncode("\"grid\"") + 
            (String2.isSomething(datasetIDRegex)? 
                "&datasetID=" + SSR.minimalPercentEncode("~\"" + datasetIDRegex + "\""): "") +
            "&orderBy(%22datasetID%22)";
        BufferedReader lines = null;
        try {
            lines = SSR.getBufferedUrlReader(query);
        } catch (Throwable t) {
            if (t.toString().indexOf("no data") >= 0)
                return "<!-- No griddap datasets at that ERDDAP match that regex. -->\n";
            throw t;
        }
        Table table = new Table();
        table.readASCII(query, lines, 
            0, 2, "", null, null, null, null, false); //simplify
        //String2.log(table.dataToString());
        PrimitiveArray datasetIDPA = table.findColumn("datasetID");
        PrimitiveArray titlePA     = table.findColumn("title");
        PrimitiveArray summaryPA   = table.findColumn("summary");
        PrimitiveArray griddapPA   = table.findColumn("griddap");
        StringBuilder sb = new StringBuilder();
        int nRows = table.nRows();
        boolean sIsSomething = String2.isSomething(EDStatic.EDDTableFromEDDGridSummary);
        for (int row = 0; row < nRows; row++) {
            String tDatasetID = datasetIDPA.getString(row);
            String tTitle   = titlePA.getString(row) + ", (As A Table)";
            String tSummary = 
                (sIsSomething && tMaxAxis0 > 0?
                    MessageFormat.format(EDStatic.EDDTableFromEDDGridSummary, tDatasetID, tMaxAxis0) + "\n" : 
                    "") +
                summaryPA.getString(row);
            if (sIsSomething)
                 
sb.append(
"<dataset type=\"EDDTableFromEDDGrid\" datasetID=\"" + tDatasetID + "_AsATable\" active=\"true\">\n" +
"    <addAttributes>\n" +
"        <att name=\"maxAxis0\" type=\"int\">" + tMaxAxis0 + "</att>\n" +
"        <att name=\"title\">" + XML.encodeAsXML(tTitle) + "</att>\n" +
"        <att name=\"summary\">" + XML.encodeAsXML(tSummary) + "</att>\n" +
"    </addAttributes>\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"" + 
    datasetIDPA.getString(row) + "_AsATableChild\">\n" +
//set updateEveryNMillis?  frequent if local ERDDAP, less frequent if remote?
"        <sourceUrl>" + XML.encodeAsXML(griddapPA.getString(row)) + "</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n\n");
        }   
        return sb.toString();
    }
   

    /**
     * This tests generateDatasetsXmlFromErddapCatalog.
     *
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDTableFromEDDGrid.testGenerateDatasetsXml() ***\n");
        testVerboseOn();
        String url = "http://localhost:8080/cwexperimental/";
        //others are good, different test cases
        String regex = "(erdMBsstdmday|jplMURSST41|zztop)";

        String results = generateDatasetsXml(url, regex, Integer.MAX_VALUE) + "\n"; 

        String expected =
"<dataset type=\"EDDTableFromEDDGrid\" datasetID=\"erdMBsstdmday_AsATable\" active=\"true\">\n" +
"    <addAttributes>\n" +
"        <att name=\"maxAxis0\" type=\"int\">10</att>\n" +
"        <att name=\"title\">SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite), (As A Table)</att>\n" +
"        <att name=\"summary\">NOTE: This dataset is the tabular version of a gridded dataset which is also\n" +
"available in this ERDDAP (see datasetID=erdMBsstdmday). Most people, most\n" +
"of the time, will prefer to use the original gridded version of this dataset.\n" +
"This dataset presents all of the axisVariables and dataVariables from the\n" +
"gridded dataset as columns in a huge database-like table of data.\n" +
"You can query this dataset as you would any other tabular dataset in ERDDAP;\n" +
"however, if you query any of the original dataVariables, you must constrain\n" +
"what was axisVariable[0] (usually &quot;time&quot;) so that data for no more than 10\n" +
"axisVariable[0] values are searched.\n" +
"\n" +
"---\n" +
"NOAA CoastWatch provides SST data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  Currently, only daytime imagery is supported.</att>\n" +
"    </addAttributes>\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMBsstdmday_AsATableChild\">\n" +
"        <sourceUrl>http://localhost:8080/cwexperimental/griddap/erdMBsstdmday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n" +
"<dataset type=\"EDDTableFromEDDGrid\" datasetID=\"jplMURSST41_AsATable\" active=\"true\">\n" +
"    <addAttributes>\n" +
"        <att name=\"maxAxis0\" type=\"int\">10</att>\n" +
"        <att name=\"title\">Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global, 0.01&#xb0;, 2002-present, Daily, (As A Table)</att>\n" +
"        <att name=\"summary\">NOTE: This dataset is the tabular version of a gridded dataset which is also\n" +
"available in this ERDDAP (see datasetID=jplMURSST41). Most people, most\n" +
"of the time, will prefer to use the original gridded version of this dataset.\n" +
"This dataset presents all of the axisVariables and dataVariables from the\n" +
"gridded dataset as columns in a huge database-like table of data.\n" +
"You can query this dataset as you would any other tabular dataset in ERDDAP;\n" +
"however, if you query any of the original dataVariables, you must constrain\n" +
"what was axisVariable[0] (usually &quot;time&quot;) so that data for no more than 10\n" +
"axisVariable[0] values are searched.\n" +
"\n" +
"---\n" +
"This is a merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL). This daily, global, Multi-scale, Ultra-high Resolution (MUR) Sea Surface Temperature (SST) 1-km data set, Version 4.1, is produced at JPL under the NASA MEaSUREs program. For details, see https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1 . This dataset is part of the Group for High-Resolution Sea Surface Temperature (GHRSST) project. The data for the most recent 7 days is usually revised everyday.  The data for other days is sometimes revised.</att>\n" +
"    </addAttributes>\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"jplMURSST41_AsATableChild\">\n" +
"        <sourceUrl>http://localhost:8080/cwexperimental/griddap/jplMURSST41</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromEDDGrid", url, regex, ""}, 
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, 
            "Unexpected results from GenerateDatasetsXml.doIt. results=\n" + results);

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromEDDGrid.test() *****************\n");
        testVerboseOn();

/* for releases, this line should have open/close comment */
        testGenerateDatasetsXml();
        testInErddap();
        testBasic();
    }

}
