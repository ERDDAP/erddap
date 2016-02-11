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

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.Enumeration;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;

/** 
 * This class creates an EDDTable from an EDDGrid.
 *
 * <p>This doesn't have generateDatasetsXml.  Use the appropriate
 * EDDGridFromX.generateDatasetsXml, then wrap it with EDDTableFromEDDGrid.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2013-03-08
 */
public class EDDTableFromEDDGrid extends EDDTable{ 

    protected EDDGrid eddGrid;

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
    public static EDDTableFromEDDGrid fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromEDDGrid(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        EDDGrid tEDDGrid = null;
        Attributes tAddGlobalAttributes = null;
        String tAccessibleTo = null;
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
                    if (tType == null || !tType.startsWith("EDDGrid"))
                        throw new SimpleException(
                            "type=\"" + tType + "\" is not allowed for the dataset within the EDDTableFromEDDGrid. " +
                            "The type MUST start with \"EDDGrid\".");
                    tEDDGrid = (EDDGrid)EDD.fromXml(erddap, tType, xmlReader);
                }

            } else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
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

        return new EDDTableFromEDDGrid(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
            tReloadEveryNMinutes, //tUpdateEveryNMillis, 
            tEDDGrid);
    }

    /**
     * The constructor.
     *
     * @throws Throwable if trouble
     */
    public EDDTableFromEDDGrid(String tDatasetID, String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery,
        Attributes tAddGlobalAttributes,
        int tReloadEveryNMinutes, //int tUpdateEveryNMillis, 
        EDDGrid tEDDGrid) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromEDDGrid " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromEDDGrid(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromEDDGrid"; 
        datasetID = tDatasetID;
        if (datasetID == null || datasetID.length() == 0)
            throw new SimpleException(errorInMethod + "The datasetID wasn't specified.");
        if (tEDDGrid == null)
            throw new SimpleException(errorInMethod + "The contained EDDGrid dataset is missing.");
        if (datasetID.equals(tEDDGrid.datasetID())) 
            throw new SimpleException(errorInMethod + "The EDDTableFromEDDGrid's datasetID " +
                "must be different from the contained EDDGrid's datasetID.");     
        eddGrid = tEDDGrid;
        localSourceUrl = eddGrid.localSourceUrl;

        setAccessibleTo(tAccessibleTo); 
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        //setUpdateEveryNMillis(tUpdateEveryNMillis); //not supported yet (ever?). Rely on EDDGrid's update system.

        //global attributes
        sourceGlobalAttributes   = eddGrid.combinedGlobalAttributes;
        addGlobalAttributes      = tAddGlobalAttributes == null? new Attributes() : tAddGlobalAttributes;
        //cdm_data_type=TimeSeries would be nice, but there is no timeseries_id variable
        addGlobalAttributes.add("cdm_data_type", 
            (eddGrid.lonIndex() >= 0 && eddGrid.latIndex() >= 0)? "Point" : "Other");
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");
        

        //specify what sourceCanConstrain
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //axisVars yes, dataVars no; best to have it doublecheck
        sourceCanConstrainStringData  = CONSTRAIN_NO; 
        sourceCanConstrainStringRegex = ""; //no    ???

        //quickRestart is handled by contained eddGrid

        //dataVariables  (axisVars in reverse order, then dataVars)
        int eddGridNAV = eddGrid.axisVariables.length;
        int eddGridNDV = eddGrid.dataVariables.length;
        dataVariables = new EDV[eddGridNAV + eddGridNDV];
        for (int dv = 0; dv < dataVariables.length; dv++) {
            //variables in this class just see the destination name/type/... of the eddGrid varaible
            EDV gridVar = dv < eddGridNAV?  eddGrid.axisVariables[eddGridNAV - 1 - dv] :
                                            eddGrid.dataVariables[dv - eddGridNAV];
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
                //tMin tMax are epochSeconds    
                tAddAtts.add("data_min", "" + tMin);
                if (debugMode) String2.log("> time tMax=" + tMax + " NOW=" + (System.currentTimeMillis()/1000.0) +
                    " abs(diff)=" + Math.abs(tMax - System.currentTimeMillis()/1000.0) +
                    " 3days=" + (3 * Calendar2.SECONDS_PER_DAY));
                newVar = new EDVTime(tSourceName, tSourceAtts, tAddAtts, 
                    tDataType); //this constructor gets source / sets destination actual_range
                //actively unset destinationMax if time is close to NOW)
                if (Math.abs(tMax - System.currentTimeMillis()/1000.0) < 3 * Calendar2.SECONDS_PER_DAY) {
                    newVar.setDestinationMax(Double.NaN);
                    newVar.setActualRangeFromDestinationMinMax();
                }
                timeIndex = dv;
            //currently, there is no EDVTimeStampGridAxis
            } else newVar = new EDV(tSourceName, "", tSourceAtts, tAddAtts, tDataType, tMin, tMax);

            dataVariables[dv] = newVar;
        }

        //ensure the setup is valid
        ensureValid();

        //If the child is a FromErddap, try to subscribe to the remote dataset.
        if (eddGrid instanceof FromErddap) 
            tryToSubscribeToChildFromErddap(eddGrid);

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromEDDGrid " + datasetID + " constructor finished. TIME=" + 
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

        //update the internal eddGrid
        return eddGrid.lowUpdate(msg, startUpdateMillis);
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
        EDVGridAxis eddGridAV[] = eddGrid.axisVariables();
        EDV         eddGridDV[] = eddGrid.dataVariables();
        int eddGridNAV = eddGridAV.length;
        int eddGridNDV = eddGridDV.length;
        //min and max desired axis destination values
        double avMin[] = new double[eddGridNAV];  
        double avMax[] = new double[eddGridNAV];
        for (int av = 0; av < eddGridNAV; av++) {
            avMin[av] = eddGridAV[av].destinationMin();  //time is epochSeconds
            avMax[av] = eddGridAV[av].destinationMax();
        }
        boolean hasAvConstraints = false; //only true if constraints are more than av min max
        StringArray constraintsDvNames = new StringArray();  //unique
        for (int c = 0; c < constraintVariables.size(); c++) { 
            String conVar = constraintVariables.get(c);
            String conOp  = constraintOps.get(c);
            String conVal = constraintValues.get(c);
            int av = String2.indexOf(eddGrid.axisVariableDestinationNames(), conVar);
            if (av < 0) {
                if (constraintsDvNames.indexOf(conVar) < 0)  //if not already in the list
                    constraintsDvNames.add(conVar);
                continue;
            }
            EDVGridAxis edvga = eddGridAV[av];
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
//??? require hasAvConstraints???

        //generate the griddap avConstraints
        String avConstraints[] = new String[eddGridNAV];
        StringBuilder allAvConstraints = new StringBuilder();
        for (int av = 0; av < eddGridNAV; av++) { 
            EDVGridAxis edvga = eddGridAV[av];
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
            if (String2.indexOf(eddGrid.axisVariableDestinationNames(), rvName) >= 0) 
                resultsAvNames.add(rvName);
            if (String2.indexOf(eddGrid.dataVariableDestinationNames(), rvName) >= 0) 
                resultsDvNames.add(rvName);
        }

        
        StringBuilder gridDapQuery = new StringBuilder();
        String gridRequestUrl = "/griddap/" + eddGrid.datasetID() + ".dods"; //after EDStatic.baseUrl, before '?'

        if (resultsDvNames.size() > 0 || constraintsDvNames.size() > 0) {
            //handle a request for 1+ data variables (in results or constraint variables)
            //e.g. lat,lon,time,sst&sst>35    or lat,lon,time&sst>35
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
            GridDataAccessor gda = new GridDataAccessor(eddGrid, gridRequestUrl, 
                gridDapQuery.toString(), true, //rowMajor
                false); //convertToNaN (would be true, but TableWriterSeparatedValue will do it)
            EDV queryDV[] = gda.dataVariables();
            int nQueryDV = queryDV.length;
            EDV sourceTableVars[] = new EDV[eddGridNAV + nQueryDV];
            for (int av = 0; av < eddGridNAV; av++) 
                sourceTableVars[av] = dataVariables[eddGridNAV - av - 1];
            for (int dv = 0; dv < nQueryDV; dv++) 
                sourceTableVars[eddGridNAV + dv] = findDataVariableByDestinationName(
                    queryDV[dv].destinationName()); 

            //make a table to hold a chunk of the results            
            int chunkNRows = EDStatic.partialRequestMaxCells / (eddGridNAV + nQueryDV);
            Table tTable = makeEmptySourceTable(sourceTableVars, chunkNRows); //source table, but source here is eddGrid's destination
            PrimitiveArray paAr[] = new PrimitiveArray[tTable.nColumns()];
            for (int col = 0; col < tTable.nColumns(); col++)
                paAr[col] = tTable.getColumn(col);

            //walk through it, periodically saving to tableWriter
            int cumNRows = 0;
            while (gda.increment()) {
                for (int av = 0; av < eddGridNAV; av++) 
                    paAr[av].addDouble(gda.getAxisValueAsDouble(av));  
                for (int dv = 0; dv < nQueryDV; dv++) 
                    paAr[eddGridNAV + dv].addDouble(gda.getDataValueAsDouble(dv));  
                if (++cumNRows >= chunkNRows) {
                    if (debugMode) String2.log(tTable.dataToCSVString(5));
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
                int av = String2.indexOf(eddGrid.axisVariableDestinationNames(), 
                    resultsAvNames.get(aav));
                activeEdvga[aav] = eddGridAV[av];
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
                int av = String2.indexOf(eddGrid.axisVariableDestinationNames(), conVar);
                if (av < 0) 
                    continue;
                modifiedUserDapQuery.append("&" + conVar + conOp + 
                    (conOp.equals(PrimitiveArray.REGEX_OP)? String2.toJson(conVal) : conVal));
            }            
            if (debugMode) 
                String2.log(">>resultsAvNames.size() > 1, gridDapQuery=" + gridDapQuery + 
                    "\n  modifiedUserDapQuery=" + modifiedUserDapQuery);

            //parse the gridDapQuery
            StringArray tDestinationNames = new StringArray();
            IntArray tConstraints = new IntArray(); //start,stop,stride for each tDestName
            eddGrid.parseAxisDapQuery(gridDapQuery.toString(), tDestinationNames, 
                tConstraints, false);

            //figure out the shape and make the NDimensionalIndex
            int ndShape[] = new int[nActiveEdvga]; //an array with the size of each active av
            for (int aav = 0; aav < nActiveEdvga; aav++) 
                ndShape[aav] = tConstraints.get(aav*3 + 2) - tConstraints.get(aav*3 + 0) + 1;
            NDimensionalIndex ndIndex = new NDimensionalIndex(ndShape);

            //make a table to hold a chunk of the results
            int chunkNRows = EDStatic.partialRequestMaxCells / nActiveEdvga;
            Table tTable = new Table(); //source table, but source here is eddGrid's destination
            PrimitiveArray paAr[] = new PrimitiveArray[nActiveEdvga];
            for (int aav = 0; aav < nActiveEdvga; aav++) {
                //this class only sees eddGrid dest type and destName
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
        String id = "testEDDTableFromEDDGrid";
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, id);
        String dir = EDStatic.fullTestCacheDirectory;


        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "1", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Attributes \\{\n" +
" s \\{\n" +
"  longitude \\{\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 360.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  \\}\n" +
"  latitude \\{\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -75.0, 75.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  \\}\n" +
"  altitude \\{\n" +
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
"  \\}\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0259568e\\+9, 1.3940208e\\+9;\n" +  
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  sst \\{\n" +
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
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Point\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_url \"http://www.pfeg.noaa.gov\";\n" +
"    String date_created \"20.{8}Z\";\n" + //changes
"    String date_issued \"20.{8}Z\";\n" +  //changes
"    Float64 Easternmost_Easting 360.0;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 75.0;\n" +
"    Float64 geospatial_lat_min -75.0;\n" +
"    Float64 geospatial_lat_resolution 0.1;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 360.0;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.1;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch";
//2013-03-10T15:13:48Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
//2013-04-04T21:43:14Z http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day
//2013-04-04T21:43:14Z http://127.0.0.1:8080/cwexperimental/tabledap/testEDDTableFromEDDGrid.das

        int tPo = results.indexOf("String history \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 73), expected, "results=\n" + results);      

expected2 = 
   "String infoUrl \"http://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"5-day,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"blended, coastwatch, day, degrees, experimental, global, noaa, ocean, oceans, sea, sea_surface_temperature, sst, surface, temperature, wcn\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 75.0;\n" +
"    String origin \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch \\(http://coastwatch.noaa.gov/\\)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String publisher_email \"erd.data@noaa.gov\";\n" +
"    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String publisher_url \"http://www.pfeg.noaa.gov\";\n" +
"    String references \"Blended SST from satellites information: This is an " + 
    "experimental product which blends satellite-derived SST data from multiple " + 
    "platforms using a weighted mean.  Weights are based on the inverse square " + 
    "of the nominal accuracy of each satellite. AMSR_E Processing information: " + 
    "http://www.ssmi.com/amsr/docs/AMSRE_V05_Updates.pdf . AMSR-E Processing " + 
    "reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT " + 
    "CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing " + 
    "Systems Internal Report. AVHRR Processing Information: " + 
    "http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing " + 
    "Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The " + 
    "development and operational application of nonlinear algorithms for the " + 
    "measurement of sea surface temperatures with the NOAA polar-orbiting " + 
    "environmental satellites. J.G.R., 103: \\(C12\\) 27999-28012, 1998. " + 
    "Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  " + 
    "Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud " + 
    "classification algorithm for the advanced very high resolution radiometer. " + 
    "J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: " + 
    "Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving " + 
    "the operational nonlinear multi-channel sea surface temperature algorithm " + 
    "coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, " + 
    "Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: " + 
    "Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. " + 
    "Validation of coastal sea and lake surface temperature measurements derived " + 
    "from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, " + 
    "No. 7, 1285-1303, 2001b. GOES Imager Processing Information: " + 
    "http://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager " + 
    "Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. " + 
    "Estimation of sea surface temperatures using GOES-8/9 radiance measurements, " + 
    "Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: " + 
    "http://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing " + 
    "reference: Not Available.\";\n" +
"    String satellite \"Aqua, GOES, POES\";\n" +
"    String sensor \"AMSR-E, MODIS, Imager, AVHRR\";\n" +
"    String source \"satellite observation: Aqua, GOES, POES, AMSR-E, MODIS, Imager, AVHRR\";\n" +
"    String sourceUrl \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day\";\n" +
"    Float64 Southernmost_Northing -75.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"NOAA OceanWatch provides a blended sea surface temperature " + 
    "\\(SST\\) products derived from both microwave and infrared sensors carried " + 
    "on multiple platforms.  The microwave instruments can measure ocean " + 
    "temperatures even in the presence of clouds, though the resolution is a bit " + 
    "coarse when considering features typical of the coastal environment.  These " + 
    "are complemented by the relatively fine measurements of infrared sensors.  The " + 
    "blended data are provided at moderate spatial resolution \\(0.1 degrees\\) " + 
    "for the Global Ocean.  Measurements are gathered by Japan's Advanced " + 
    "Microwave Scanning Radiometer \\(AMSR-E\\) instrument, a passive radiance " + 
    "sensor carried aboard NASA's Aqua spacecraft, NOAA's Advanced Very High " + 
    "Resolution Radiometer, NOAA GOES Imager, and NASA's Moderate Resolution " + 
    "Imaging Spectrometer \\(MODIS\\). THIS IS AN EXPERIMENTAL PRODUCT: " + 
    "intended strictly for scientific evaluation by professional marine scientists.\";\n" +
"    String time_coverage_end \"20.{8}T12:00:00Z\";\n" +    //changes
"    String time_coverage_start \"2002-07-06T12:00:00Z\";\n" +
"    String title \"SST, Blended, Global, EXPERIMENTAL \\(5 Day Composite\\)\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  \\}\n" +
"\\}\n";
        tPo = results.indexOf("String infoUrl ");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected2, "results=\n" + results);      

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "2", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
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
        query = "latitude&latitude>20&latitude<=20.5" +
            "&longitude=0"; //longitude constraint is ignored (since it's valid)
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "1axis", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude\n" +
"degrees_north\n" +
"20.1\n" +
"20.2\n" +
"20.3\n" +
"20.4\n" +
"20.5\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis =min
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude=0", 
            dir, tedd.className() + "1axisMin", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"longitude\n" +
"degrees_east\n" +
"0.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <=min 
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude<=0", 
            dir, tedd.className() + "1axisLEMin", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"longitude\n" +
"degrees_east\n" +
"0.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <min fails but not immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude<0", 
                dir, tedd.className() + "1axisLMin", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      

        //query 1 axis <min fails immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude<-0.01", 
                dir, tedd.className() + "1axisLMin2", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude<-0.01 is outside of the variable's actual_range: 0.0 to 360.0)", 
            "results=\n" + results);      


        //query 1 axis =max
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude=360", 
            dir, tedd.className() + "1axisMax", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"longitude\n" +
"degrees_east\n" +
"360.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis >=max 
        tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude>=360", 
            dir, tedd.className() + "1axisGEMax", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"longitude\n" +
"degrees_east\n" +
"360.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 1 axis <max fails but not immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude>360", 
                dir, tedd.className() + "1axisGMax", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      

        //query 1 axis >max fails immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "longitude&longitude>360.01", 
                dir, tedd.className() + "1axisGMin2", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude>360.01 is outside of the variable's actual_range: 0.0 to 360.0)", 
            "results=\n" + results);      


        //query time axis =min
        String timeMin = "2002-07-06T12:00:00Z";
        tName = tedd.makeNewFileForDapQuery(null, null, "time&time=" + timeMin, 
            dir, tedd.className() + "timeAxisMin", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"time\n" +
"UTC\n" +
"2002-07-06T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query time axis <=min 
        tName = tedd.makeNewFileForDapQuery(null, null, "time&time<=" + timeMin, 
            dir, tedd.className() + "timeAxisLEMin", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"time\n" +
"UTC\n" +
"2002-07-06T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query time axis <min fails but not immediately   (< is tested as <=)
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "time&time<2002-07-06T12:00:00Z", 
                dir, tedd.className() + "timeAxisLMin", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, "com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)", 
            "results=\n" + results);      

        //query time axis <min-1sec fails immediately
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "time&time<2002-07-06T11:59:59.9Z", 
                dir, tedd.className() + "timeAxisLMin2", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureLinesMatch(results, 
            "com.cohort.util.SimpleException: Your query produced no matching results. " +
            "\\(time<20.{17}Z is outside of the variable's actual_range: " +
            "2002-07-06T12:00:00Z to 20.{17}Z\\)", 
            "results=\n" + results);      


        //time axis max is NaN (e.g., ~now)
        //query time axis <max fails
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "time&time>=now-1hour", 
                dir, tedd.className() + "timeAxisGMax", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureLinesMatch(results, 
            "com.cohort.util.SimpleException: " +
            "Your query produced no matching results. " +
            "\\(time>=20.{17}Z is outside of the variable's actual_range: " +
            "2002-07-06T12:00:00Z to 20.{17}Z\\)", 
            "results=\n" + results);      



        //query error   
        results = "";
        try {
            query = "latitude&latitude>20&latitude<=20.5" +
                "&longitude=1.04"; //invalid:  in middle of range but no match
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "1axisInvalid", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        expected = "com.cohort.util.SimpleException: Your query produced no matching results. (longitude=1.04 will never be true.)";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
            query = "latitude&latitude>20&latitude<=20.5" +
                "&longitude=-1"; //invalid:  out of actual_range
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "1axisInvalid", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        expected = "com.cohort.util.SimpleException: Your query produced no matching results. " +
            "(longitude=-1 is outside of the variable's actual_range: 0.0 to 360.0)";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query 2 axes
        query = "longitude,latitude&latitude>20&latitude<=20.3" +
                "&longitude>=15&longitude<15.3&time=\"2012-01-01T12\""; //time constraint is ignored (since it's valid)
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "2axes", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"15.0,20.1\n" +
"15.0,20.2\n" +
"15.0,20.3\n" +
"15.1,20.1\n" +
"15.1,20.2\n" +
"15.1,20.3\n" +
"15.2,20.1\n" +
"15.2,20.2\n" +
"15.2,20.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query all axes  (different order)
        query = "latitude,longitude,altitude,time&latitude>20&latitude<=20.3" +
                "&longitude>=15&longitude<15.3&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "allaxes", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"20.1,15.0,0.0,2012-01-01T12:00:00Z\n" +
"20.1,15.1,0.0,2012-01-01T12:00:00Z\n" +
"20.1,15.2,0.0,2012-01-01T12:00:00Z\n" +
"20.2,15.0,0.0,2012-01-01T12:00:00Z\n" +
"20.2,15.1,0.0,2012-01-01T12:00:00Z\n" +
"20.2,15.2,0.0,2012-01-01T12:00:00Z\n" +
"20.3,15.0,0.0,2012-01-01T12:00:00Z\n" +
"20.3,15.1,0.0,2012-01-01T12:00:00Z\n" +
"20.3,15.2,0.0,2012-01-01T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //av+dv query with dv and av constraints
        query = "latitude,longitude,altitude,time,sst&sst>35&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "_dvav1", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude,longitude,altitude,time,sst\n" +
"degrees_north,degrees_east,m,UTC,degree_C\n" +
"-31.8,240.6,0.0,2012-01-01T12:00:00Z,35.1\n" +
"-31.4,241.3,0.0,2012-01-01T12:00:00Z,35.1\n" +
"-31.4,241.5,0.0,2012-01-01T12:00:00Z,35.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //av query with dv and av constraints
        query = "latitude,longitude,altitude,time&sst>35&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "_dvav2", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"-31.8,240.6,0.0,2012-01-01T12:00:00Z\n" +
"-31.4,241.3,0.0,2012-01-01T12:00:00Z\n" +
"-31.4,241.5,0.0,2012-01-01T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //dv query with dv and av constraint
        query = "sst&sst>35&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "_dvav3", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"sst\n" +
"degree_C\n" +
"35.1\n" +
"35.1\n" +
"35.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        debugMode = oDebugMode;
    }

    /**
     */
    public static void testTableFromGriddap() throws Throwable {
        String2.log("\nEDDTableFromEDDGrid.testTableFromGriddap()");
        testVerboseOn();
debugMode = false; //normally false.  Set it to true if need help.
        String results, query, tName, expected, expected2;
        String id = "testTableFromGriddap";
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, id);
        String dir = EDStatic.fullTestCacheDirectory;


        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "1", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Attributes \\{\n" +
" s \\{\n" +
"  longitude \\{\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 360.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  \\}\n" +
"  latitude \\{\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -75.0, 75.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  \\}\n" +
"  altitude \\{\n" +
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
"  \\}\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0259568e\\+9, .{5,14};\n" +  
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  sst \\{\n" +
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
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Point\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_url \"http://www.pfeg.noaa.gov\";\n" +
"    String date_created \"20.{8}Z\";\n" + //changes
"    String date_issued \"20.{8}Z\";\n" +  //changes
"    Float64 Easternmost_Easting 360.0;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 75.0;\n" +
"    Float64 geospatial_lat_min -75.0;\n" +
"    Float64 geospatial_lat_resolution 0.1;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 360.0;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.1;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch";
//2013-03-10T15:13:48Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
//2013-04-04T21:43:14Z http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day
//2013-04-04T21:43:14Z http://127.0.0.1:8080/cwexperimental/tabledap/testEDDTableFromEDDGrid.das

        int tPo = results.indexOf("String history \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 73), expected, "results=\n" + results);      

expected2 = 
   "String infoUrl \"http://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html\";\n" +
"    String institution \"NOAA NMFS SWFSC ERD\";\n" +
"    String keywords \"5-day,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"blended, coastwatch, day, degrees, experimental, global, noaa, ocean, oceans, sea, sea_surface_temperature, sst, surface, temperature, wcn\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 75.0;\n" +
"    String origin \"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch \\(http://coastwatch.noaa.gov/\\)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String publisher_email \"erd.data@noaa.gov\";\n" +
"    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String publisher_url \"http://www.pfeg.noaa.gov\";\n" +
"    String references \"Blended SST from satellites information: This is an " +
    "experimental product which blends satellite-derived SST data from multiple " +
    "platforms using a weighted mean.  Weights are based on the inverse square " +
    "of the nominal accuracy of each satellite. AMSR_E Processing information: " +
    "http://www.ssmi.com/amsr/docs/AMSRE_V05_Updates.pdf . AMSR-E Processing " +
    "reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT " +
    "CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing " +
    "Systems Internal Report. AVHRR Processing Information: " +
    "http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing " +
    "Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The " +
    "development and operational application of nonlinear algorithms for the " +
    "measurement of sea surface temperatures with the NOAA polar-orbiting " +
    "environmental satellites. J.G.R., 103: \\(C12\\) 27999-28012, 1998. " +
    "Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  " +
    "Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud " +
    "classification algorithm for the advanced very high resolution radiometer. " +
    "J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: " +
    "Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the " +
    "operational nonlinear multi-channel sea surface temperature algorithm " +
    "coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, " +
    "Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, " +
    "X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation " +
    "of coastal sea and lake surface temperature measurements derived from " +
    "NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, " +
    "1285-1303, 2001b. GOES Imager Processing Information: " +
    "http://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager " +
    "Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. " +
    "Estimation of sea surface temperatures using GOES-8/9 radiance measurements, " +
    "Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: " +
    "http://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing " +
    "reference: Not Available.\";\n" +
"    String satellite \"Aqua, GOES, POES\";\n" +
"    String sensor \"AMSR-E, MODIS, Imager, AVHRR\";\n" +
"    String source \"satellite observation: Aqua, GOES, POES, AMSR-E, MODIS, Imager, AVHRR\";\n" +
"    String sourceUrl \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day\";\n" +
"    Float64 Southernmost_Northing -75.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"NOAA OceanWatch provides a blended sea surface temperature " +
    "\\(SST\\) products derived from both microwave and infrared sensors carried " +
    "on multiple platforms.  The microwave instruments can measure ocean " +
    "temperatures even in the presence of clouds, though the resolution is a bit " +
    "coarse when considering features typical of the coastal environment.  These " +
    "are complemented by the relatively fine measurements of infrared sensors.  The " +
    "blended data are provided at moderate spatial resolution \\(0.1 degrees\\) for " +
    "the Global Ocean.  Measurements are gathered by Japan's Advanced Microwave " +
    "Scanning Radiometer \\(AMSR-E\\) instrument, a passive radiance sensor carried " +
    "aboard NASA's Aqua spacecraft, NOAA's Advanced Very High Resolution Radiometer, " +
    "NOAA GOES Imager, and NASA's Moderate Resolution Imaging Spectrometer " +
    "\\(MODIS\\). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific " +
    "evaluation by professional marine scientists.\";\n" +
"    String time_coverage_end \"20.{8}T12:00:00Z\";\n" +    //changes
"    String time_coverage_start \"2002-07-06T12:00:00Z\";\n" +
"    String title \"SST, Blended, Global, EXPERIMENTAL \\(5 Day Composite\\)\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  \\}\n" +
"\\}\n";

        tPo = results.indexOf("String infoUrl ");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected2, "results=\n" + results);      

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "2", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
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
        query = "latitude&latitude>20&latitude<=20.5" +
            "&longitude=0"; //longitude constraint is ignored (since it's valid)
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "1axis", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude\n" +
"degrees_north\n" +
"20.1\n" +
"20.2\n" +
"20.3\n" +
"20.4\n" +
"20.5\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //query error   
        results = "";
        try {
            query = "latitude&latitude>20&latitude<=20.5" +
                "&longitude=1.04"; //invalid
            tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
                tedd.className() + "1axisInvalid", ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = t.toString();
        }
        String2.log("results=\n" + results);
        expected = "com.cohort.util.SimpleException: Your query produced no matching results. (longitude=1.04 will never be true.)";
        Test.ensureEqual(results, expected, "");      

        //query 2 axes
        query = "longitude,latitude&latitude>20&latitude<=20.3" +
                "&longitude>=15&longitude<15.3&time=\"2012-01-01T12\""; //time constraint is ignored (since it's valid)
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "2axes", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"15.0,20.1\n" +
"15.0,20.2\n" +
"15.0,20.3\n" +
"15.1,20.1\n" +
"15.1,20.2\n" +
"15.1,20.3\n" +
"15.2,20.1\n" +
"15.2,20.2\n" +
"15.2,20.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      


        //query all axes  (different order)
        query = "latitude,longitude,altitude,time&latitude>20&latitude<=20.3" +
                "&longitude>=15&longitude<15.3&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "allaxes", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"20.1,15.0,0.0,2012-01-01T12:00:00Z\n" +
"20.1,15.1,0.0,2012-01-01T12:00:00Z\n" +
"20.1,15.2,0.0,2012-01-01T12:00:00Z\n" +
"20.2,15.0,0.0,2012-01-01T12:00:00Z\n" +
"20.2,15.1,0.0,2012-01-01T12:00:00Z\n" +
"20.2,15.2,0.0,2012-01-01T12:00:00Z\n" +
"20.3,15.0,0.0,2012-01-01T12:00:00Z\n" +
"20.3,15.1,0.0,2012-01-01T12:00:00Z\n" +
"20.3,15.2,0.0,2012-01-01T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //av+dv query with dv and av constraints
        query = "latitude,longitude,altitude,time,sst&sst>35&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "_dvav1", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude,longitude,altitude,time,sst\n" +
"degrees_north,degrees_east,m,UTC,degree_C\n" +
"-31.8,240.6,0.0,2012-01-01T12:00:00Z,35.1\n" +
"-31.4,241.3,0.0,2012-01-01T12:00:00Z,35.1\n" +
"-31.4,241.5,0.0,2012-01-01T12:00:00Z,35.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //av query with dv and av constraints
        query = "latitude,longitude,altitude,time&sst>35&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "_dvav2", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"latitude,longitude,altitude,time\n" +
"degrees_north,degrees_east,m,UTC\n" +
"-31.8,240.6,0.0,2012-01-01T12:00:00Z\n" +
"-31.4,241.3,0.0,2012-01-01T12:00:00Z\n" +
"-31.4,241.5,0.0,2012-01-01T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           
        //dv query with dv and av constraint
        query = "sst&sst>35&time=\"2012-01-01T12\""; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, dir, 
            tedd.className() + "_dvav3", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"sst\n" +
"degree_C\n" +
"35.1\n" +
"35.1\n" +
"35.1\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
    }

     
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromEDDGrid.test() *****************\n");
        testVerboseOn();

/* */
        //always done        
        testBasic();
        testTableFromGriddap();
    }

}
