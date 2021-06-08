/* 
 * EDDGridLon0360 Copyright 2021, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
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

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.Subscriptions;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.text.MessageFormat;
import java.util.ArrayList;

/** 
 * This class creates an EDDGrid with longitude values in the range 0 to 360
 * from the enclosed dataset with ascending longitude values, at least some
 * of which are &lt; 0.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2021-05-28
 */
public class EDDGridLon0360 extends EDDGrid { 

    private EDDGrid childDataset;
    //If childDataset is a fromErddap dataset from this ERDDAP, 
    //childDataset will be kept as null and always refreshed from 
    //erddap.gridDatasetHashMap.get(localChildDatasetID).
    private Erddap erddap; 
    private String localChildDatasetID;

    private int slonim180, slonim1, sloni0, sloni179; //source lon indices
    //corresponding destination lon indices, after the 2 parts are reordered
    //i.e., where are sloni in the destination lon array?
    //If dloni0/179 are -1, there are no values in that range in this dataset.
    private int dlonim180, dlonim1, dloni0, dloni179;

    //dInsert179/180 are used if there is a big gap between lon179 and lon180 (e.g., 120...-120(->240))
    //!!! Need to insert lots of missing values (at average spacing)
    //!!! so if user requests stride >1, user will always get a reasonable set of lon values
    //!!! and an appropriate string of missing values in the lon gap.
    //If the 'i'ndex values are -1, the system is not active for this dataset.
    private int insert179i = -1, insert180i = -1; 

    /**
     * This constructs an EDDGridLon0360 based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridLon0360"&gt; 
     *    having just been read.  
     * @return an EDDGridLon0360.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridLon0360 fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

        if (verbose) String2.log("\n*** constructing EDDGridLon0360(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 

        //data to be obtained while reading xml
        EDDGrid tChildDataset = null;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        boolean tAccessibleViaWMS = true;
        boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        int tReloadEveryNMinutes = Integer.MAX_VALUE; //unusual 
        int tUpdateEveryNMillis  = Integer.MAX_VALUE; //unusual 
        int tnThreads = -1; //interpret invalid values (like -1) as EDStatic.nGridThreads
        boolean tDimensionValuesInMemory = true;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();

        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if (localTags.equals("<dataset>")) {
                if ("false".equals(xmlReader.attributeValue("active"))) {
                    //skip it - read to </dataset>
                    if (verbose) String2.log("  skipping datasetID=" + xmlReader.attributeValue("datasetID") + 
                        " because active=\"false\".");
                    while (xmlReader.stackSize() != startOfTagsN + 1 ||
                           !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
                        xmlReader.nextTag();
                        //String2.log("  skippping tags: " + xmlReader.allTags());
                    }

                } else {
                    if (tChildDataset == null) {
                        EDD edd = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
                        if (edd instanceof EDDGrid) {
                            tChildDataset = (EDDGrid)edd;
                        } else {
                            throw new RuntimeException("Datasets.xml error: " +
                                "The dataset defined in an " +
                                "EDDGridLon0360 must be a subclass of EDDGrid.");
                        }
                    } else {
                        throw new RuntimeException("Datasets.xml error: " +
                            "There can be only one <dataset> defined within an " +
                            "EDDGridLon0360 <dataset>.");
                    }
                }

            } 
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<updateEveryNMillis>")) {}
            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis = String2.parseInt(content); 
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<accessibleViaWMS>")) {}
            else if (localTags.equals("</accessibleViaWMS>")) tAccessibleViaWMS = String2.parseBoolean(content);
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content);
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
            else if (localTags.equals( "<nThreads>")) {}
            else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content); 
            else if (localTags.equals( "<dimensionValuesInMemory>")) {}
            else if (localTags.equals("</dimensionValuesInMemory>")) tDimensionValuesInMemory = String2.parseBoolean(content);

            else xmlReader.unexpectedTagException();
        }

        //make the main dataset based on the information gathered
        return new EDDGridLon0360(erddap, tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS, tAccessibleViaFiles,
            tOnChange, tFgdcFile, tIso19115File, tDefaultDataQuery, tDefaultGraphQuery,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tChildDataset, tnThreads, tDimensionValuesInMemory);

    }

    /**
     * The constructor.
     * The axisVariables must be the same and in the same
     * order for each dataVariable.
     *
     * @param erddap if known in this context, else null
     * @param tDatasetID
     * @param tAccessibleTo is a space separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: )
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tChildDataset
     * @throws Throwable if trouble
     */
    public EDDGridLon0360(Erddap tErddap, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, 
        boolean tAccessibleViaWMS, boolean tAccessibleViaFiles,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        EDDGrid oChildDataset, int tnThreads, boolean tDimensionValuesInMemory) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridLon0360 " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridLon0360(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridLon0360"; 
        datasetID = tDatasetID;
        Test.ensureFileNameSafe(datasetID, "datasetID");
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
        nThreads = tnThreads; //interpret invalid values (like -1) as EDStatic.nGridThreads
        dimensionValuesInMemory = tDimensionValuesInMemory; 

        if (oChildDataset == null)
            throw new RuntimeException(errorInMethod + "childDataset=null!");
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
        //String2.log(">> accessibleViaFiles " + EDStatic.filesActive + " " + tAccessibleViaFiles + " " + tChildDataset.accessibleViaFiles);
        accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles && tChildDataset.accessibleViaFiles;

        //UNUSUAL: if valid value not specified, copy from childDataset
        setReloadEveryNMinutes(tReloadEveryNMinutes < Integer.MAX_VALUE?
            tReloadEveryNMinutes : 
            tChildDataset.getReloadEveryNMinutes());
        setUpdateEveryNMillis( tUpdateEveryNMillis  < Integer.MAX_VALUE?
            tUpdateEveryNMillis :
            tChildDataset.updateEveryNMillis);

        //make/copy the local globalAttributes
        localSourceUrl           = tChildDataset.localSourceUrl();
        sourceGlobalAttributes   = (Attributes)tChildDataset.sourceGlobalAttributes().clone();
        addGlobalAttributes      = (Attributes)tChildDataset.addGlobalAttributes().clone();
        combinedGlobalAttributes = (Attributes)tChildDataset.combinedGlobalAttributes().clone();
        combinedGlobalAttributes.set("title", 
             combinedGlobalAttributes.getString("title") + ", Lon0360");

        //make/copy the local axisVariables
        int nAv = tChildDataset.axisVariables.length;
        axisVariables = new EDVGridAxis[nAv];
        System.arraycopy(tChildDataset.axisVariables, 0, axisVariables, 0, nAv);
        lonIndex   = tChildDataset.lonIndex;
        latIndex   = tChildDataset.latIndex;
        altIndex   = tChildDataset.altIndex;
        depthIndex = tChildDataset.depthIndex;
        timeIndex  = tChildDataset.timeIndex;
        if (lonIndex < 0)
            throw new RuntimeException(errorInMethod + 
                "The child dataset doesn't have a longitude axis variable.");

        //lonIndex not rightmost isn't tested. So throw exception and ask admin for sample.
        if (lonIndex < nAv - 1)
            throw new RuntimeException(errorInMethod + 
                "The longitude dimension isn't the rightmost dimension. This is untested. " +
                "Please send a sample file to bob.simons@noaa.gov .");

        //make/copy the local dataVariables
        int nDv = tChildDataset.dataVariables.length;
        dataVariables = new EDV[nDv];
        System.arraycopy(tChildDataset.dataVariables, 0, dataVariables, 0, nDv);

        //figure out the newLonValues
        EDVLonGridAxis childLon = (EDVLonGridAxis)axisVariables[lonIndex];
        if (!childLon.isAscending())
            throw new RuntimeException(errorInMethod + 
                "The child longitude axis has descending values!");
        if (childLon.destinationMinDouble() >= 0)
            throw new RuntimeException(errorInMethod + 
                "The child longitude axis has no values <0 (min=" + 
                childLon.destinationMinDouble() + ")!");
        PrimitiveArray childLonValues = childLon.destinationValues();
        int nChildLonValues = childLonValues.size();

// source            slonim180,  slonim1  ||   sloni0,   sloni179  ||
// old: [ignored] ||      -180,       -1  ||        0, 90,    179  || [ignored]
// new:                   -180, -90,  -1  ||        0, 90,    179 
// dest:                                       dloni0,   dloni179  || dlonim180  dlonim1
        //all of the searches use EXACT math 
        PAOne clvPAOne = new PAOne(childLonValues);

        if (childLonValues.getDouble(0) >= 0 )
            throw new RuntimeException(errorInMethod + 
                "The child dataset is not suitable for EDDGridLon0360, because the longitude axis has no values <0 (min=" + 
                childLonValues.getDouble(0) + "!");
        slonim180 = childLonValues.binaryFindFirstGE(0,        nChildLonValues - 1, clvPAOne.setDouble(-180)); //first index >=-180
        slonim1   = childLonValues.binaryFindLastLE(slonim180, nChildLonValues - 1, clvPAOne.setDouble(0));    //last index <=0

        if (childLonValues.getDouble(nChildLonValues - 1) > 0) {
            sloni0   = childLonValues.binaryFindFirstGE(slonim1, nChildLonValues - 1, clvPAOne.setDouble(  0)); //first index >=0
            sloni179 = childLonValues.binaryFindLastLE( sloni0,  nChildLonValues - 1, clvPAOne.setDouble(180)); //last index <=180
        } else {
            sloni0   = -1; //no values >=0
            sloni179 = -1;
        }        
        //if [slonim1]=[sloni0]=0, then slonim1--, so dataset has [0]=0 (rather than [n-1]=360)
        if (slonim1 == sloni0) 
            slonim1--;
        if (childLonValues.getDouble(slonim180) == -180 && //there is a value=-180
            sloni179 >= 0 &&                               //and if there is a sloni179
            childLonValues.getDouble(sloni179) == 180)     //with value == 180
            sloni179--;  //-180 is a duplicate of 180, so ignore it        

        //start making newLonValues
        PrimitiveArray newLonValues = sloni0 == -1? 
            childLonValues.subset(0, 1, -1) : //size=0           
            childLonValues.subset(sloni0, 1, sloni179); //always a new backing array

        //set dloni: where are sloni after source values are rearranged to dest values
        dloni0   = sloni0 == -1? -1 : 0;
        dloni179 = sloni0 == -1? -1 : sloni179 - sloni0;

        if (sloni0 >= 0) {
            //create dInsert if there's a big gap
            double spacing = childLon.averageSpacing(); //avg is more reliable than any single value
            double lon179 = newLonValues.getDouble(newLonValues.size() - 1);
            double lon180 = childLonValues.getDouble(0);
            //e.g., gap of 2.1*spacing --floor--> 2 -> insert 1 value
            int insertN = Math2.roundToInt(Math.floor((lon180 - lon179) / spacing)) - 1;
            if (insertN >= 1) {  //enough space to insert 1+ regularly spaced lon values

                throw new RuntimeException(
                    "This dataset needs to have missing values inserted near lon=180, but the code to do that is untested.\n" +
                    "Please email a sample file from this dataset to bob.simons@noaa.gov so he run the tests.");

                /* 
                insert179i = newLonValues.size();
                for (int i = 1; i <= insertN; i++) {
                    double td = Math2.niceDouble(lon179 + i * spacing, 12);
                    if (Math.abs(180 - td) < 1e-8)  //be extra careful to catch 180
                        td = 180;
                    newLonValues.addDouble(td);
                }
                insert180i = newLonValues.size() - 1;
                if (verbose) String2.log(
                    "  'insert' is active: lon[" + insert179i + "]=" + newLonValues.getDouble(insert179i) +
                                         " lon[" + insert180i + "]=" + newLonValues.getDouble(insert180i));
                */
            }
        }

        //finish making newLonValues: append the m180 ... m1 lon values
        dlonim180 = newLonValues.size();
        PrimitiveArray tLonValues = childLonValues.subset(slonim180, 1, slonim1); //always a new backing array
        tLonValues.addOffsetScale(360, 1);  //-180 ... -1 -> 180 ... 359
        newLonValues.append(tLonValues);
        dlonim1 = newLonValues.size() - 1;

        //make the new lon axis
        EDVLonGridAxis newEDVLon = new EDVLonGridAxis(tDatasetID, EDV.LON_NAME,
            new Attributes(childLon.combinedAttributes()), new Attributes(),
            newLonValues);
        axisVariables[lonIndex] = newEDVLon; 

        //ensure the setup is valid
        ensureValid();

        //If the original childDataset is a FromErddap, try to subscribe to the remote dataset.
        //Don't use tChildDataset. It may be the local EDDGrid that the fromErddap points to.
        if (oChildDataset instanceof FromErddap) 
            tryToSubscribeToChildFromErddap(oChildDataset);

        //finally
        long cTime = System.currentTimeMillis() - constructionStartMillis;
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() +
                 "slonim180=" + slonim180 + " slonim1=" + slonim1 + 
                " sloni0=" + sloni0 + " sloni179=" + sloni179 + "\n" + 
                 "dlonim180=" + dlonim180 + " dlonim1=" + dlonim1 + 
                " insert179i=" + insert179i + " insert180i=" + insert180i + 
                " dloni0=" + dloni0 + " dloni179=" + dloni179 + "\n": "") + 
            "localChildDatasetID=" + localChildDatasetID + "\n" +
            "\n*** EDDGridLon0360 " + datasetID + " constructor finished. TIME=" + 
            cTime + "ms" + (cTime >= 600000? "  (>10m!)" : cTime >= 10000? "  (>10s!)" : "") + "\n"); 

        //very last thing: saveDimensionValuesInFile
        if (!dimensionValuesInMemory)
            saveDimensionValuesInFile();

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
     * This returns a list of childDatasetIDs.
     * Most dataset types don't have any children. A few, like
     * EDDGridSideBySide do, so they overwrite this method to return the IDs.
     *
     * @return a new list of childDatasetIDs. 
     */
    public StringArray childDatasetIDs() {
        StringArray sa = new StringArray();
        try {
            sa.add(childDataset == null? localChildDatasetID : childDataset.datasetID());
        } catch (Exception e) {
            String2.log("Error caught in edd.childDatasetIDs(): " + MustBe.throwableToString(e));
        }
        return sa;
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException( 
            "Error: EDDGridLon0360 doesn't support method=\"sibling\".");
    }

    /** 
     * This returns a fileTable
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     *
     * @param nextPath is the partial path (with trailing slash) to be appended 
     *   onto the local fileDir (or wherever files are, even url).
     * @return null if trouble,
     *   or Object[3]
     *   [0] is a sorted table with file "Name" (String), "Last modified" (long millis), 
     *     "Size" (long), and "Description" (String, but usually no content),
     *   [1] is a sorted String[] with the short names of directories that are 1 level lower, and
     *   [2] is the local directory corresponding to this (or null, if not a local dir).
     */
    public Object[] accessibleViaFilesFileTable(String nextPath) {
        if (!accessibleViaFiles)
            return null;
        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = getChildDataset();
        return tChildDataset.accessibleViaFilesFileTable(nextPath);
    }

    /**
     * This converts a relativeFileName into a full localFileName (which may be a url).
     * 
     * @param relativeFileName (for most EDDTypes, just offset by fileDir)
     * @return full localFileName or null if any error (including, file isn't in
     *    list of valid files for this dataset)
     */
     public String accessibleViaFilesGetLocal(String relativeFileName) {
         if (!accessibleViaFiles)
             return null;
        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = getChildDataset();
        return tChildDataset.accessibleViaFilesGetLocal(relativeFileName);
     }


    /**
     * This does the actual incremental update of this dataset 
     * (i.e., for real time datasets).
     * EDDGridFromDap's version deals with the leftmost axis growing.
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
                
        boolean changed = tChildDataset.lowUpdate(msg, startUpdateMillis);
        //catch and deal with time axis changes: by far the most common
        if (changed && timeIndex >= 0) {
            axisVariables[timeIndex] = tChildDataset.axisVariables[timeIndex];
            combinedGlobalAttributes().set("time_coverage_start", axisVariables[timeIndex].destinationMinString());
            combinedGlobalAttributes().set("time_coverage_end",   axisVariables[timeIndex].destinationMaxString());
        }

        return changed;
    }

    /** 
     * This gets data (not yet standardized) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDirTable If EDDGridFromFiles, this MAY be the dirTable, else null. 
     * @param tFileTable If EDDGridFromFiles, this MAY be the fileTable, else null. 
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
    public PrimitiveArray[] getSourceData(Table tDirTable, Table tFileTable,
        EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        //Get childDataset or localChildDataset. Work with stable local reference.
        //This isn't ideal (better if GridDataAccessor had stable reference to childDataset, 
        //but it needs this dataset), but it is pretty good.
        EDDGrid tChildDataset = getChildDataset();
        tDirTable  = tChildDataset.getDirTable();
        tFileTable = tChildDataset.getFileTable();

        //get the request lon start/stride/stop
        int li3 = lonIndex * 3;
        int rStart  = tConstraints.get(li3 + 0);
        int rStride = tConstraints.get(li3 + 1);
        int rStop   = tConstraints.get(li3 + 2);
        int nAV = axisVariables.length;
        int tnDV = tDataVariables.length;

        //find exact stop (so tests below are true tests)
        rStop -= (rStop - rStart) % rStride;

        //map:
        //dest is: [dloni0...dloni179] [insert179...insert0,] dlonim180...dlonim1,  
        //becomes  [     0...     179] [insert179...insert0,]       180...359,       

        //really easy: all the requested lons are from the new left: <=dloni179
        if (dloni0 != -1 && rStop <= dloni179) {
            //1 request, and lon values are already correct
            if (debugMode) String2.log(">> getSourceData: all from new left: " + rStart + ":" + rStride + ":" + rStop);
            IntArray newConstraints = (IntArray)tConstraints.subset(0, 1, tConstraints.size() - 1);
            newConstraints.set(li3 + 0, rStart - (dloni0 - sloni0)); 
            newConstraints.set(li3 + 2, rStop  - (dloni0 - sloni0));            
            return tChildDataset.getSourceData(tDirTable, tFileTable, tDataVariables, newConstraints);
        }

        //easy: all the requested lons are from the new right: >=dlonim180
        if (rStart >= dlonim180) {
            //1 request, but need to adjust lon values
            if (debugMode) String2.log(">> getSourceData: all from new right: " + rStart + ":" + rStride + ":" + rStop);
            IntArray newConstraints = (IntArray)tConstraints.subset(0, 1, tConstraints.size() - 1);
            newConstraints.set(li3 + 0, rStart - (dlonim180 - slonim180));   
            newConstraints.set(li3 + 2, rStop  - (dlonim180 - slonim180)); 
            PrimitiveArray results[] = tChildDataset.getSourceData(tDirTable, tFileTable, 
                tDataVariables, newConstraints);
            results[lonIndex] = (PrimitiveArray)results[lonIndex].clone();
            results[lonIndex].addOffsetScale(360, 1);
            return results;
        }

        //map:
        //dest is: [dloni0...dloni179] [insert179...insert0,] dlonim180...dlonim1,  
        //becomes  [     0...     179] [insert179...insert0,]       180...359,       

        //Hard: need to make 2 requests and merge them.
        //If this is a big request, this takes a lot of memory for the *short* time 
        //  when merging in memory.
        if (debugMode) String2.log(">> getSourceData: request from left and right: " + rStart + ":" + rStride + ":" + rStop); 

        //find request for new left half
        IntArray newConstraints = (IntArray)tConstraints.subset(0, 1, tConstraints.size() - 1);
        PrimitiveArray newLeftResults[] = null;
        if (rStart <= dloni179) {
            //some values are from sloni0 to sloni179
            newConstraints.set(li3 + 0, rStart + sloni0); 
            newConstraints.set(li3 + 2, sloni179);
            newLeftResults = tChildDataset.getSourceData(tDirTable, tFileTable, tDataVariables, newConstraints);
            newLeftResults[lonIndex] = (PrimitiveArray)newLeftResults[lonIndex].clone();
            newLeftResults[lonIndex].addOffsetScale(-360, 1);
            if (debugMode) String2.log(">> got newLeftResults from source lon[" + 
                (rStart + slonim180) + ":" + rStride + ":" + slonim1 + "]");
        }

        int insertNLon = 0;
        /*
        //find out if 'insert' is active and relevant (i.e., some values are requested)
        boolean insertActive = insert179i >= 0; //basic system is active
        if (insertActive && (rStop < insert179i || rStart > insert180i))
            insertActive = false;  //shouldn't happen because should have been dealt with above
        int firstInserti = -1;
        if (insertActive) {
            //Find first loni >= insert179i which is among actually requested lon values (given rStride).
            firstInserti = Math.max(insert179i, rStart); 
            if (rStride > 1) {
                int mod = (firstInserti - rStart) % rStride;
                if (mod > 0)
                    firstInserti += rStride - mod;
            }
            if (firstInserti > insert180i) {
                firstInserti = -1;
                insertActive = false; //stride walks over (skips) all inserted lons
            }
        }
        int lastInserti = -1;
        if (insertActive) {
            lastInserti = Math.min(rStop, insert180i);
            lastInserti -= (lastInserti - rStart) % rStride; //perhaps last=first
            insertNLon = OpendapHelper.calculateNValues(firstInserti, rStride, lastInserti);
            if (debugMode) String2.log(">> will insert lon[" + 
                firstInserti + ":" + rStride + ":" + lastInserti + "]");
        }
        */

        //find request for new right half 
        PrimitiveArray newRightResults[] = null;
        if (rStop >= dlonim180) {
            //Find first loni >=dlonim180 which is among actually requested lon values (given rStride).
            int floni = dlonim180; 
            if (rStride > 1) {
                int mod = (floni - rStart) % rStride;
                if (mod > 0)
                    floni += rStride - mod;
            }

            //some values are from source lonim180 to lonim1
            int tStart = floni - (dlonim180 - slonim180);
            int tStop  = rStop - (dlonim180 - slonim180);
            newConstraints.set(li3 + 0, tStart); 
            newConstraints.set(li3 + 2, tStop);            
            newRightResults = tChildDataset.getSourceData(tDirTable, tFileTable, 
                tDataVariables, newConstraints);
            if (debugMode) String2.log(">> got newRightResults from source lon[" + 
                tStart + ":" + rStride + ":" + tStop + "]");
        }

        //map:
        //dest is: [dloni0...dloni179] [insert179...insert0,] dlonim180...dlonim1,  
        //becomes  [     0...     179] [insert179...insert0,]       180...359,       

        //now build results[] by reading chunks alternately from the 3 sources 
        //what is chunk size from left request, from insert, and from right request?
        int chunkLeft   = newLeftResults == null? 0 : 1;
        int chunkInsert = insertNLon <= 0?    0 : 1;
        int chunkRight  = newRightResults   == null? 0 : 1;
        //what is total nValues for dataVariables
        int nValues = 1;
        PrimitiveArray results[] = new PrimitiveArray[nAV + tnDV];
        for (int avi = 0; avi < nAV; avi++) {
            int avi3 = avi * 3;
            int avStart  = tConstraints.get(avi3 + 0);
            int avStride = tConstraints.get(avi3 + 1);
            int avStop   = tConstraints.get(avi3 + 2);

            results[avi] = axisVariables[avi].sourceValues().subset(  //source=dest
                avStart, avStride, avStop);
            int avN = results[avi].size();

            if (avi > lonIndex) {
                chunkLeft   *= avN;
                chunkInsert *= avN;
                chunkRight  *= avN; 
            }
            if (avi == lonIndex) {
                if (newLeftResults != null)    chunkLeft   *= newLeftResults[avi].size();
                if (insertNLon >  0)           chunkInsert *= insertNLon;
                if (newRightResults   != null) chunkRight  *= newRightResults[avi].size();
            }
            //all axes:
            nValues *= avN;
        }
       
        //and make the results data variables
        for (int tdv = 0; tdv < tnDV; tdv++) 
            results[nAV + tdv] = PrimitiveArray.factory(
                tDataVariables[tdv].sourceDataPAType(), nValues, false);

        //dest is: dlonim180...dlonim1, [insert179i...insert180i,]  [dloni0...dloni179]
        //becomes      -180...-1,       [insert179i...insert180i,]  [     0...     179]

        //copy the dataVariables values into place
        int poLeft = 0;
        int poRight = 0;
        int nextDest = 0;
        while (nextDest < nValues) {
            for (int tdv = 0; tdv < tnDV; tdv++) {
                if (newLeftResults != null) 
                    results[nAV + tdv].addFromPA(newLeftResults[nAV + tdv], poLeft, chunkLeft);
                if (chunkInsert > 0)    
                    results[nAV + tdv].addNDoubles(chunkInsert, 
                        tDataVariables[tdv].safeDestinationMissingValue());
                if (newRightResults != null) 
                    results[nAV + tdv].addFromPA(newRightResults[  nAV + tdv], poRight, chunkRight);
            }
            poLeft   += chunkLeft;   //may be 0
            poRight  += chunkRight;  //may be 0
            nextDest += chunkLeft + chunkInsert + chunkRight;

            if (debugMode && nextDest >= nValues) {
                if (newLeftResults != null) 
                    Test.ensureEqual(newLeftResults[nAV].size(),  poLeft,  "poLeft");
                if (newRightResults   != null)    
                    Test.ensureEqual(newRightResults[nAV].size(), poRight, "poRight");
                Test.ensureEqual(results[nAV].size(), nValues, "nValues");
            }
        }
        return results;
    }


    /** 
     * This runs generateDatasetsXmlFromErddapCatalog.
     * 
     * @param oPublicSourceUrl  A complete URL of an ERDDAP (ending with "/erddap/"). 
     * @param datasetIDRegex  E.g., ".*" for all dataset Names.
     * @return a String with the results
     */
    public static String generateDatasetsXmlFromErddapCatalog( 
        String oPublicSourceUrl, String datasetIDRegex) throws Throwable {

        String2.log("*** EDDGridLon0360.generateDatasetsXmlFromErddapCatalog(" +
            oPublicSourceUrl + ")");

        if (oPublicSourceUrl == null)
            throw new RuntimeException(String2.ERROR + ": sourceUrl is null.");
        if (!oPublicSourceUrl.endsWith("/erddap/"))
            throw new RuntimeException(String2.ERROR + ": sourceUrl doesn't end with '/erddap/'.");

        //get table with datasetID minLon, maxLon
        String query = oPublicSourceUrl + "tabledap/allDatasets.csv" + 
            "?datasetID,title,griddap,minLongitude,maxLongitude" +
            "&dataStructure=" + SSR.minimalPercentEncode("\"grid\"") + 
            "&minLongitude%3C0" +
            (String2.isSomething(datasetIDRegex)? 
                "&datasetID=" + SSR.minimalPercentEncode("~\"" + datasetIDRegex + "\""): "") +
            "&orderBy(%22datasetID%22)";
        BufferedReader br = null;
        try {
            br = SSR.getBufferedUrlReader(query);
        } catch (Throwable t) {
            if (t.toString().indexOf("no data") >= 0)
                return "<!-- No griddap datasets at that ERDDAP match that regex\n" +
                       "     and have minLongitude values &lt;0. -->\n";
            throw t;
        }
        Table table = new Table();
        table.readASCII(query, 
            br, "", "", 0, 2, "", null, null, null, null, false); //simplify
        PrimitiveArray datasetIDPA = table.findColumn("datasetID");
        PrimitiveArray titlePA     = table.findColumn("title");
        PrimitiveArray minLonPA    = table.findColumn("minLongitude");
        PrimitiveArray maxLonPA    = table.findColumn("maxLongitude");
        PrimitiveArray griddapPA   = table.findColumn("griddap");
        StringBuilder sb = new StringBuilder();
        int nRows = table.nRows();
        for (int row = 0; row < nRows; row++) {
            if (datasetIDPA.getString(row).equals("etopo360"))
                continue;
sb.append(
"<dataset type=\"EDDGridLon0360\" datasetID=\"" + 
    datasetIDPA.getString(row) + "_Lon0360\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"" + 
    datasetIDPA.getString(row) + "_Lon0360Child\">\n" +
"        <!-- " + XML.encodeAsXML(titlePA.getString(row)) + "\n" +
"             minLon=" + minLonPA.getString(row) + " maxLon=" + maxLonPA.getString(row) + " -->\n" +
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
    public static void testGenerateDatasetsXmlFromErddapCatalog() throws Throwable {

        String2.log("\n*** EDDGridLon0360.testGenerateDatasetsXmlFromErddapCatalog() ***\n");
        testVerboseOn();
        String url = "http://coastwatch.pfeg.noaa.gov/erddap/"; //purposefully http:// to test if ERDDAP will promote to https://
        //erdMH1's are -179 to 179, so will be in results
        //others are ~0 to 360 so won't be in results
        //               -180 to 180    -180.0 to -110
        String regex = "(erdMH1chlamday|erdVHNchlamday|erdMWchlamday|erdMHsstnmday)";

        String results = generateDatasetsXmlFromErddapCatalog(url, regex) + "\n"; 

        String expected =
"<dataset type=\"EDDGridLon0360\" datasetID=\"erdMH1chlamday_Lon0360\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMH1chlamday_Lon0360Child\">\n" +
"        <!-- Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)\n" +
"             minLon=-179.9792 maxLon=179.9792 -->\n" +
"        <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMH1chlamday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n" +
"<dataset type=\"EDDGridLon0360\" datasetID=\"erdVHNchlamday_Lon0360\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdVHNchlamday_Lon0360Child\">\n" +
"        <!-- Chlorophyll a, North Pacific, NOAA VIIRS, 750m resolution, 2015-present (Monthly Composite)\n" +
"             minLon=-180.03375 maxLon=-110.00625 -->\n" +
"        <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridLon0360FromErddapCatalog", url, regex}, 
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, 
            "Unexpected results from GenerateDatasetsXml.doIt. results=\n" + results);

    }

    /**
     * This tests a dataset that is initially all LT0.
     *
     * @throws Throwable if trouble
     */
    public static void testLT0() throws Throwable {

        String2.log("\n****************** EDDGridLon0360.testLT0() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;

      try {

        EDDGrid eddGrid;
        try {
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_erdVHNchlamday_Lon0360");       
        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\n" + MustBe.throwableToString(t));
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_erdVHNchlamday_Lon0360");       
        }

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_LT0_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 68];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 11985];\n" +
"  Float64 longitude[longitude = 9333];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chla[time = 68][altitude = 1][latitude = 11985][longitude = 9333];\n" +
"    MAPS:\n" +
"      Float64 time[time = 68];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 11985];\n" +
"      Float64 longitude[longitude = 9333];\n" +
"  } chla;\n" +
"} test_erdVHNchlamday_Lon0360;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_LT0_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 180.00375, 249.99375;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected =
"    Float64 geospatial_lon_max 249.99375;\n" +
"    Float64 geospatial_lon_min 180.00375;\n" +
"    Float64 geospatial_lon_resolution 0.007500000000000001;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting 180.00375;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //lon values
        userDapQuery = "longitude[0:1000:9332]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_LT0_lon", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 180.00375, 187.50375, 195.00375, 202.50375, 210.00375, 217.50375, 225.00375, 232.50375, 240.00375, 247.50375, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire lon range
        userDapQuery = "chla[(2015-03-16)][][(89.77152):5500:(-0.10875)][(180.00375):4500:(249.99375)]"; //[-179.99625:4500:-110.00625]
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_LT0_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
//from same request from cw erddap: erdVHNchlamday  
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(89.77125):5500:(-0.10875)%5D%5B(-179.99625):4500:(-110.00625)%5D
// but add 360 to the lon values to get the values below
"time,altitude,latitude,longitude,chla\n" +
"UTC,m,degrees_north,degrees_east,mg m^-3\n" +
"2015-03-16T00:00:00Z,0.0,89.77125,180.00375,NaN\n" +
"2015-03-16T00:00:00Z,0.0,89.77125,213.75375,NaN\n" +
"2015-03-16T00:00:00Z,0.0,89.77125,247.50375,NaN\n" +
"2015-03-16T00:00:00Z,0.0,48.52125,180.00375,NaN\n" +
"2015-03-16T00:00:00Z,0.0,48.52125,213.75375,0.29340988\n" +
"2015-03-16T00:00:00Z,0.0,48.52125,247.50375,NaN\n" +
"2015-03-16T00:00:00Z,0.0,7.27125,180.00375,NaN\n" +
"2015-03-16T00:00:00Z,0.0,7.27125,213.75375,0.08209898\n" +
"2015-03-16T00:00:00Z,0.0,7.27125,247.50375,0.12582141\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon subset  
        userDapQuery = "chla[(2015-03-16)][][(10):500:(0)][(200):2000:(240)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_LT0_2", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(10):500:(0)%5D%5B(-160):2000:(-120)%5D
//but add 360 to get lon values below
"time,altitude,latitude,longitude,chla\n" +
"UTC,m,degrees_north,degrees_east,mg m^-3\n" +
"2015-03-16T00:00:00Z,0.0,10.00125,199.99875,0.03348998\n" +
"2015-03-16T00:00:00Z,0.0,10.00125,214.99875,0.06315609\n" +
"2015-03-16T00:00:00Z,0.0,10.00125,229.99875,0.06712352\n" +
"2015-03-16T00:00:00Z,0.0,6.25125,199.99875,0.089674756\n" +
"2015-03-16T00:00:00Z,0.0,6.25125,214.99875,0.12793668\n" +
"2015-03-16T00:00:00Z,0.0,6.25125,229.99875,0.15159287\n" +
"2015-03-16T00:00:00Z,0.0,2.50125,199.99875,0.16047283\n" +
"2015-03-16T00:00:00Z,0.0,2.50125,214.99875,0.15874723\n" +
"2015-03-16T00:00:00Z,0.0,2.50125,229.99875,0.12635218\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image
        userDapQuery = "chla[(2015-03-16)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_NEPacificNowLon0360", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test of /files/ system for fromErddap in local host dataset
        results = SSR.getUrlResponseStringUnchanged(
            "http://localhost:8080/cwexperimental/files/test_erdVHNchlamday_Lon0360/");
        expected = "VHN2015060&#x5f;2015090&#x5f;chla&#x2e;nc"; 
        Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

        String2.log("\n*** EDDGridLon0360.testLT0() finished.");
      } catch (Throwable t) {
          String2.pressEnterToContinue("\n" +
              MustBe.throwableToString(t));
      }
    }

    /**
     * This tests a dataset that is initially -180.018 to 180.018 (min and max beyond bounds are dropped by this dataset)
     *
     * @throws Throwable if trouble
     */
    public static void testPM181() throws Throwable {

        String2.log("\n****************** EDDGridLon0360.testPM181() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;

        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_nesdisVHNchlaWeekly_Lon0360");       
 /* */
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_PM181_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 371];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 4788];\n" +
"  Float64 longitude[longitude = 9600];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlor_a[time = 371][altitude = 1][latitude = 4788][longitude = 9600];\n" +
"    MAPS:\n" +
"      Float64 time[time = 371];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 4788];\n" +
"      Float64 longitude[longitude = 9600];\n" +
"  } chlor_a;\n" +
"} test_nesdisVHNchlaWeekly_Lon0360;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_PM181_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        String2.log(results);
        expected = 
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.018749999999981976, 359.98125;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected =
"    Float64 geospatial_lon_max 359.98125;\n" +
"    Float64 geospatial_lon_min 0.018749999999981976;\n" +
"    Float64 geospatial_lon_resolution 0.037500000000000006;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting 0.018749999999981976;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //lon values
        //this tests entire range
        userDapQuery = "longitude[(0.01875):1000:(359.98125)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM181_lon", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 0.018749999999981976, 37.51874999999998, 75.01874999999998, 112.51874999999998, 150.01874999999998, 187.51874999999998, 225.01874999999998, 262.51874999999995, 300.01874999999995, 337.51874999999995, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon values near 180
        userDapQuery = "longitude[(179.93):1:(180.07)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM181_lonNear0", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 179.94374999999997, 179.98125, 180.01874999999998, 180.05624999999998, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire new lon range
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(27.0188):1:(26.99)][(0.98):1900:(359.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM181_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-05T12:00:00Z):1:(2021-03-05T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(0.98):1900:(179.98)%5D
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-05T12:00:00Z):1:(2021-03-05T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(-145.25625):1900:(-0.98)%5D
        expected =     //corresponding PM180 lons: -145.25625, -74.00625, -2.75625
"time,altitude,latitude,longitude,chlor_a\n" +
"UTC,m,degrees_north,degrees_east,mg m^-3\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,0.9937499999999819,NaN\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,72.24374999999998,NaN\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,143.49374999999998,0.16770716\n" +

"2021-03-05T12:00:00Z,0.0,27.01875,214.74374999999998,0.087049656\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,285.99375,0.062160246\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,357.24375,NaN\n" +

"2021-03-05T12:00:00Z,0.0,26.98125,0.9937499999999819,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,72.24374999999998,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,143.49374999999998,0.16694945\n" +

"2021-03-05T12:00:00Z,0.0,26.98125,214.74374999999998,0.08402522\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,285.99375,0.0637603\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,357.24375,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //0-180 subset
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(27.0188):1:(26.99)][(0.98):1900:(179.9)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM181_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-05T12:00:00Z):1:(2021-03-05T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(0.98):1900:(179.98)%5D
        expected =    
"time,altitude,latitude,longitude,chlor_a\n" +
"UTC,m,degrees_north,degrees_east,mg m^-3\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,0.9937499999999819,NaN\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,72.24374999999998,NaN\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,143.49374999999998,0.16770716\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,0.9937499999999819,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,72.24374999999998,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,143.49374999999998,0.16694945\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //0-181 subset with not relevant spillover  //yes, it is correctly handled by "all from new left"
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(27.0188):1:(26.99)][(0.98):1900:(181)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM181_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-05T12:00:00Z):1:(2021-03-05T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(0.98):1900:(179.98)%5D
        expected =    
"time,altitude,latitude,longitude,chlor_a\n" +
"UTC,m,degrees_north,degrees_east,mg m^-3\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,0.9937499999999819,NaN\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,72.24374999999998,NaN\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,143.49374999999998,0.16770716\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,0.9937499999999819,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,72.24374999999998,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,143.49374999999998,0.16694945\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //180-360 subset
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(27.0188):1:(26.99)][(214.74):1900:(359.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM181_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/nesdisVHNchlaWeekly.htmlTable?chlor_a%5B(2021-03-05T12:00:00Z):1:(2021-03-05T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B(27.018749999999996):1:(26.99)%5D%5B(-145.26):1900:(-0.98)%5D
        expected =     //corresponding PM180 lons: -145.25625, -74.00625, -2.75625
"time,altitude,latitude,longitude,chlor_a\n" +
"UTC,m,degrees_north,degrees_east,mg m^-3\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,214.74374999999998,0.087049656\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,285.99375,0.062160246\n" +
"2021-03-05T12:00:00Z,0.0,27.01875,357.24375,NaN\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,214.74374999999998,0.08402522\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,285.99375,0.0637603\n" +
"2021-03-05T12:00:00Z,0.0,26.98125,357.24375,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image entire world
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_entireWorld_PM181", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);


        //test image subset near 180
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(71):(-61)][(120):(245)]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_subsetNear180_PM181", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test image just new left
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(41):(-61)][(10):(175)]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_justNewLeft_PM181", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test image just new right
        userDapQuery = "chlor_a[(2021-03-05T12:00:00Z)][][(41):(-61)][(190):(355)]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_justNewRight_PM181", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

//SOURCE is ultimately a remote THREDDS so no source files
//        String2.log("\n* Test getting /files/ from local host erddap...");
//        results = SSR.getUrlResponseStringUnchanged(
//            "http://localhost:8080/cwexperimental/files/erdRWdhws1day_Lon0360/");
//        expected = "PH2001244&#x5f;2001273&#x5f;dhw&#x2e;nc"; //"PH2001244_2001273_dhw.nc";
//        Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
        /*  */
        String2.log("\n*** EDDGridLon0360.testPM181 finished.");
    }


    /**
     * This tests a dataset that is initially -180 to 180.
     *
     * @throws Throwable if trouble
     */
    public static void testPM180() throws Throwable {

        String2.log("\n****************** EDDGridLon0360.testPM180() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;

        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_etopo180_Lon0360");       

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_PM180_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"Dataset {\n" +
"  Float64 latitude[latitude = 10801];\n" +
"  Float64 longitude[longitude = 21600];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 altitude[latitude = 10801][longitude = 21600];\n" +
"    MAPS:\n" +
"      Float64 latitude[latitude = 10801];\n" +
"      Float64 longitude[longitude = 21600];\n" +
"  } altitude;\n" +
"} test_etopo180_Lon0360;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_PM180_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        String2.log(results);
        expected = 
" longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 359.98333333333335;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected =
"    Float64 geospatial_lon_max 359.98333333333335;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.016666666666666666;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //all lon values near 0
        userDapQuery = "longitude[(179.94):(180.06)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_lonNear0", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 179.93333333333334, 179.95, 179.96666666666664, 179.98333333333335, 180.0, 180.01666666666668, 180.03333333333333, 180.05, 180.06666666666666, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //this tests correct jump across lon 180
        userDapQuery = "longitude[(179.95):3:(180.05)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_lon_jumpa", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 179.95, 180.0, 180.05, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //this tests correct jump across lon 180
        userDapQuery = "longitude[(179.966):3:(180.03)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_lon_jumpb", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 179.96666666666664, 180.01666666666668, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //this tests correct jump across lon 180
        userDapQuery = "longitude[(179.983):3:(180.04)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180__jumpc", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 179.98333333333335, 180.03333333333333, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //this tests correct jump across lon 180
        userDapQuery = "longitude[(179.999):3:(180.05)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180__jumpd", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, 180.0, 180.05, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire new lon range
        userDapQuery = "altitude[(40):60:(41)][(0):6600:(359.99)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(0):6600:(180.0)%5D
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(-140):6600:(0)%5D
"latitude,longitude,altitude\n" +
"degrees_north,degrees_east,m\n" +
"40.0,0.0,1\n" +
"40.0,110.0,1374\n" +
"40.0,220.0,-4514\n" +
"40.0,330.0,-1486\n" +
"41.0,0.0,521\n" +
"41.0,110.0,1310\n" +
"41.0,220.0,-4518\n" +
"41.0,330.0,-1998\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, just <180
        userDapQuery = "altitude[(40):60:(41)][(0):6600:(120)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_2", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(0):6600:(180.0)%5D
"latitude,longitude,altitude\n" +
"degrees_north,degrees_east,m\n" +
"40.0,0.0,1\n" +
"40.0,110.0,1374\n" +
"41.0,0.0,521\n" +
"41.0,110.0,1310\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, just > 180 
        userDapQuery = "altitude[(40):60:(41)][(220):6600:(359.99)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_3", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(-140):6600:(0)%5D
"latitude,longitude,altitude\n" +
"degrees_north,degrees_east,m\n" +
"40.0,220.0,-4514\n" +
"40.0,330.0,-1486\n" +
"41.0,220.0,-4518\n" +
"41.0,330.0,-1998\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //bang on 180 
        userDapQuery = "altitude[(40):60:(41)][(0):5400:(359.99)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_PM180_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(0):5400:(180.0)%5D
// https://coastwatch.pfeg.noaa.gov/erddap/griddap/etopo180.htmlTable?altitude%5B(40):60:(41)%5D%5B(-180):5400:(0)%5D
"latitude,longitude,altitude\n" +
"degrees_north,degrees_east,m\n" +
"40.0,0.0,1\n" +
"40.0,90.0,795\n" +
"40.0,180.0,-5241\n" +
"40.0,270.0,183\n" +
"41.0,0.0,521\n" +
"41.0,90.0,1177\n" +
"41.0,180.0,-5644\n" +
"41.0,270.0,215\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test full image
        userDapQuery = "altitude[][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_PM180", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test new left image
        userDapQuery = "altitude[][(0):(170)]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_PM180left", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test new image
        userDapQuery = "altitude[][(190):(359.99)]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_PM180right", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);



        String2.log("\n*** EDDGridLon0360.testPM180 finished.");

    }

    /**
     * This tests a dataset that is initially Insert.
     *
     * @throws Throwable if trouble
     */
    public static void testInsert() throws Throwable {

        String2.log("\n****************** EDDGridLon0360.testInsert() *****************\n");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = true;
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;
        EDDGrid eddGrid;

        //test notApplicable (dataset maxLon already <180)
        try {
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, "notApplicable_Lon0360");       
            throw new RuntimeException("shouldn't get here");
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            if (msg.indexOf("Error in EDDGridLon0360(notApplicable_Lon0360) constructor:\n" +
                "The child longitude axis has no values <0 (min=90.0)!") < 0)
                throw t;
        }

        //testInsert
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_insert_Lon0360");       

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_Insert_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 2656];\n" +  //changes
"  Float32 latitude[latitude = 90];\n" +
"  Float32 longitude[longitude = 180];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 air[time = 2656][latitude = 90][longitude = 180];\n" +
"    MAPS:\n" +
"      Float64 time[time = 2656];\n" +
"      Float32 latitude[latitude = 90];\n" +
"      Float32 longitude[longitude = 180];\n" +
"  } air;\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_Insert_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        String2.log(results);
        expected = 
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 1.0, 359.0;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected =
"    Float64 geospatial_lon_max 179.975;\n" +
"    Float64 geospatial_lon_min -180.0;\n" +
"    Float64 geospatial_lon_resolution 0.025;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting -180.0;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //lon values
        //this tests correct jump across lon 0
        userDapQuery = "longitude[(-180):2057:(179.975)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_lon", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -180.0, -128.575, -77.14999999999998, -25.725, 25.7, 77.125, 128.55, 179.975, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon values near 0
        userDapQuery = "longitude[(-.075):1:(.075)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_lonNear0", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -0.075, -0.05, -0.025, 0.0, 0.025, 0.05, 0.075, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire new lon range
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-180):2057:(179.975)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
//erdMBsstdmday natively 120...320(-40)
//verified by cw erddap  lon (128.55):2057:(180)  and (180):2057:(320)
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMBsstdmday.htmlTable?sst[(2008-04-15):1:(2008-04-15)][(0.0):1:(0.0)][(0):1:(0)][(128.55):2057:(180)]
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-180.0,26.41\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.0,179.975,26.31\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-180.0,26.4708\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,128.55,30.36\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,179.975,26.515\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, left+insert+right sections 
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(135)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_4", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,128.55,30.36\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just left
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(-70)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1L", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-77.14999999999998,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just left, 1 point
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(-120)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1Lb", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-128.575,26.7495\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just insert   //insert values are between -40 and 120
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-25.725):2057:(27)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1I", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,25.7,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just insert, 1 point
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-25.725):2057:(-20)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1Ib", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-25.725,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just right
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(128.55):2057:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1R", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.0,179.975,26.31\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,128.55,30.36\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,179.975,26.515\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just right, 1 point
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(128.55):2057:(135)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_1Rb", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,128.55,30.36\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, left + insert
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-180):2057:(27)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_2", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-180.0,26.41\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-180.0,26.4708\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,25.7,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, insert + right
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(25.7):2057:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_3", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.0,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.0,179.975,26.31\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,128.55,30.36\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,179.975,26.515\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, left + right  (jump over insert)
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):" + (2057*5) + ":(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_Insert_3", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,0.0,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.0,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.100000000000001,128.55,30.36\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image
        userDapQuery = "sst[(2008-04-16T00:00:00Z)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_Insert", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        String2.log("\n*** EDDGridLon0360.testInsert finished.");
        debugMode = oDebugMode;
    }


    /** This tests badFilesFlag.
     */
    public static void testBadFilesFlag() throws Throwable {
        String2.log("\n*** EDDGridLon0360.testBadFilesFlag()\n" +
            "This test requires that test_erdPHsstamday_Lon0360 be loaded in the local ERDDAP.");

        String datasetID      = "test_erdPHsstamday_Lon0360";
        String childDatasetID = "test_erdPHsstamday_Lon0360Child";
        String request = "http://localhost:8080/cwexperimental/griddap/" + datasetID + ".csvp?time";
        String results, expected;

        //set badFileFlag (to delete the badFiles.nc file and reload the dataset)
        String2.writeToFile(EDStatic.fullBadFilesFlagDirectory + datasetID, "doesn't matter");

        //wait 10 seconds and test that all times are present
        String2.log("I set the badFileFlag. Now I'm waiting 20 seconds while localhost ERDDAP reloads the dataset.");
        Math2.sleep(20000); //sometimes computer is super slow

        //test that all times are present
        results = SSR.getUrlResponseStringUnchanged(request);
        String fullExpected = 
"time (UTC)\n" +
"1981-09-16T00:00:00Z\n" +
"1981-10-16T12:00:00Z\n";
        Test.ensureEqual(results, fullExpected, "results=\n" + results);
        
        //mark a file bad
        EDDGridLon0360 edd = (EDDGridLon0360)oneFromDatasetsXml(null, datasetID); 
        EDDGridFromFiles childEdd = (EDDGridFromFiles)edd.getChildDataset(); 
        Table fileTable = childEdd.getFileTable(); // throws Throwable
        int dirIndex    = fileTable.getColumn(EDDGridFromFiles.FT_DIR_INDEX_COL).getInt(0);
        String fileName = fileTable.getColumn(EDDGridFromFiles.FT_FILE_LIST_COL).getString(0);
        long lastMod    = fileTable.getColumn(EDDGridFromFiles.FT_LAST_MOD_COL ).getLong(0);
        childEdd.addBadFileToTableOnDisk(dirIndex, fileName, lastMod, "for EDDGridLon0360.testBadFilesFlag()");

        //set regular flag
        String2.writeToFile(EDStatic.fullResetFlagDirectory + datasetID, "doesn't matter");

        //wait 10 seconds and test that that time point is gone
        String2.log("I marked a file as bad. Now I'm waiting 20 seconds while localhost ERDDAP reloads the dataset.");
        Math2.sleep(20000);  //sometimes computer is super slow
        results = SSR.getUrlResponseStringUnchanged(request);
        expected = 
"time (UTC)\n" +
"1981-10-16T12:00:00Z\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //set badFileFlag (to delete the badFiles.nc file and reload the dataset)
        String2.writeToFile(EDStatic.fullBadFilesFlagDirectory + datasetID, "doesn't matter");

        //wait 10 seconds and test that all times are present
        String2.log("I set the badFileFlag. Now I'm waiting 10 seconds while localhost ERDDAP reloads the dataset.");
        Math2.sleep(10000);
        results = SSR.getUrlResponseStringUnchanged(request);
        Test.ensureEqual(results, fullExpected, "results=\n" + results);
    }

    /** This tests hardFlag.
     */
    public static void testHardFlag() throws Throwable {
        String2.log("\n*** EDDGridLon0360.testHardFlag()\n" +
            "This test requires hawaii_d90f_20ee_c4cb and hawaii_d90f_20ee_c4cb_Lon0360\n" +
            "be loaded in the local ERDDAP.");

        //set hardFlag
        String startTime = Calendar2.getCurrentISODateTimeStringLocalTZ();
        Math2.sleep(1000);
        String2.writeToFile(EDStatic.fullHardFlagDirectory + "hawaii_d90f_20ee_c4cb_Lon0360", "test");
        String2.log("I just set a hardFlag for hawaii_d90f_20ee_c4cb_Lon0360.\n" +
            "Now I'm waiting 10 seconds.");
        Math2.sleep(10000);
        //flush the log file
        String tIndex = SSR.getUrlResponseStringUnchanged("http://localhost:8080/cwexperimental/status.html");
        Math2.sleep(2000);
        //read the log file
        String tLog = String2.readFromFile(EDStatic.fullLogsDirectory + "log.txt")[1];
        String expected = // ***
          "deleting cached dataset info for datasetID=hawaii_d90f_20ee_c4cb_Lon0360Child\n" +
"\\*\\*\\* unloading datasetID=hawaii_d90f_20ee_c4cb_Lon0360\n" +
"\\*\\*\\* deleting cached dataset info for datasetID=hawaii_d90f_20ee_c4cb_Lon0360\n" +
"\n" +
"\\*\\*\\* RunLoadDatasets is starting a new hardFlag LoadDatasets thread at (..........T..............)\n" +
"\n" +
"\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\n" +
"LoadDatasets.run EDStatic.developmentMode=true ..........T..............\n" +
"  datasetsRegex=\\(hawaii_d90f_20ee_c4cb_Lon0360\\) inputStream=null majorLoad=false";
        
        int po = Math.max(0, tLog.lastIndexOf(expected.substring(0, 77)));
        int po2 = Math.min(po + expected.length(), tLog.indexOf("majorLoad=false", po) + 15);
        String tResults = tLog.substring(po, po + expected.length());
        String2.log("tResults=\"\n" + tResults + "\n\"\n");
        Test.testLinesMatch(tResults, expected, "tResults and expected don't match!");
        
        //so far so good, tResults matches expected
        int po3 = tResults.indexOf("thread at ");
        String reloadTime = tResults.substring(po3 + 10, po3 + 35);
        String2.log(" startTime=" + startTime + "\n" +
                    "reloadTime=" + reloadTime);
        Test.ensureTrue(startTime.compareTo(reloadTime) < 0, "startTime is after reloadTime?!");

        //test that child was successfully constructed after that
        int po4 = tLog.indexOf(
            "*** EDDGridFromErddap hawaii_d90f_20ee_c4cb_Lon0360Child constructor finished. TIME=",
            po);
        Test.ensureTrue(po4 > po, "po4=" + po4 + " isn't greater than po=" + po + " !");

        //test that parent was successfully constructed after that
        int po5 = tLog.indexOf(
            "*** EDDGridLon0360 hawaii_d90f_20ee_c4cb_Lon0360 constructor finished. TIME=",
            po4);
        Test.ensureTrue(po5 > po4, "po5=" + po5 + " isn't greater than po4=" + po4 + " !");
    }

   
    /**
     * This tests the /files/ "files" system.
     * This requires local_erdVHNchlamday_Lon0360 in the localhost ERDDAP.
     */
    public static void testFiles() throws Throwable {

        String2.log("\n*** EDDGridLon0360.testFiles()\n");
        String tDir = EDStatic.fullTestCacheDirectory;
        String dapQuery, tName, start, query, results, expected;
        int po;

        try {
            //get /files/datasetID/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"VHN2002182_2002212_chla.nc.gz,1332025888000,17339003,\n" +
"VHN2002213_2002243_chla.nc.gz,1332026460000,18217295,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //get /files/datasetID/
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/");
            Test.ensureTrue(results.indexOf("VHN2002182&#x5f;2002212&#x5f;chla&#x2e;nc") > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">17339003<")                               > 0, "results=\n" + results);

            //get /files/datasetID/subdir/.csv

            //download a file in root
            results = String2.annotatedString(SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/VHN2002182_2002212_chla.nc.gz").substring(0, 50));
            expected = 
"[31][139][8][8] [26]eO[0][3]VHN2002182_2002212_chla.nc[0][228][216][127][140]#[231]][199]q[223][229][238]r?[end]"; 
            Test.ensureEqual(results, expected, "results=\n" + results);

            //download a file in subdir

            //try to download a non-existent dataset
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent directory
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/local_erdVHNchlamday_Lon0360/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file in existant subdir

 

        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error. This test requires local_erdVHNchlamday_Lon0360 in the localhost ERDDAP.", t); 
        } 
    }



    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 3;
        String msg = "\n^^^ EDDGridLon0360.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {

                } else {
                    if (test ==  0) testGenerateDatasetsXmlFromErddapCatalog(); 
                    if (test ==  1) testLT0();  //this also tests /files/ working for fromErddap cw ERDDAP dataset
                    if (test ==  2) testPM181();
                    if (test ==  3) testPM180();
                    //if (test ==  4) testInsert(); //needs a test dataset
                    //if (test ==  5) testHardFlag(); //if fails, try again
                    //if (test ==  6) testBadFilesFlag(); 
                    //if (test ==  7) testFiles();

                    //note that I have NO TEST of dataset where lon isn't the rightmost dimension.
                    //so there is a test for that in the constructor, 
                    //which asks admin to send me an example for testing.
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }


}
