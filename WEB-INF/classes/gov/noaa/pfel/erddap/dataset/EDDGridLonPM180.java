/* 
 * EDDGridLonPM180 Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
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

import java.text.MessageFormat;


/** 
 * This class creates an EDDGrid with longitude values in the range -180 to 180
 * from the enclosed dataset with ascending longitude values, at least some
 * of which are &gt; 180.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2015-08-05
 */
public class EDDGridLonPM180 extends EDDGrid { 

    private EDDGrid childDataset;
    //If childDataset is a fromErddap dataset from this ERDDAP, 
    //childDataset will be kept as null and always refreshed from 
    //erddap.gridDatasetHashMap.get(localChildDatasetID).
    private Erddap erddap; 
    private String localChildDatasetID;

    private int sloni0, sloni179, sloni180, sloni359; //source lon indices
    //corresponding destination lon indices, after the 2 parts are reordered
    //i.e., where are sloni in the destination lon array?
    //If dloni0/179 are -1, there are no values in that range in this dataset.
    private int dloni0, dloni179, dloni180, dloni359;

    //dInsert359/0 are used if there is a big gap between lon359 and lon0 (e.g., 300/-120...120)
    //!!! dInsert SYSTEM IS NOT ACTIVE: It isn't so simple:
    //!!! Need to insert lots of missing values (at average spacing)
    //!!! so if user requests stride >1, user will always get a reasonable set of lon values
    //!!! and an appropriate string of missing values in the lon gap.
    //If the 'i'ndex values are -1, the system is not active for this dataset.
    private int insert359i = -1, insert0i = -1; 

    /**
     * This constructs an EDDGridLonPM180 based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridLonPM180"&gt; 
     *    having just been read.  
     * @return an EDDGridLonPM180.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridLonPM180 fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

        if (verbose) String2.log("\n*** constructing EDDGridLonPM180(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 

        //data to be obtained while reading xml
        EDDGrid tChildDataset = null;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        boolean tAccessibleViaWMS = true;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        int tReloadEveryNMinutes = Integer.MAX_VALUE; //unusual 
        int tUpdateEveryNMillis  = Integer.MAX_VALUE; //unusual 

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
                    if (verbose) String2.log("  skipping " + xmlReader.attributeValue("datasetID") + 
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
                                "EDDGridLonPM180 must be a subclass of EDDGrid.");
                        }
                    } else {
                        throw new RuntimeException("Datasets.xml error: " +
                            "There can be only one <dataset> defined within an " +
                            "EDDGridLonPM180 <dataset>.");
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

        //make the main dataset based on the information gathered
        return new EDDGridLonPM180(erddap, tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
            tOnChange, tFgdcFile, tIso19115File, tDefaultDataQuery, tDefaultGraphQuery,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tChildDataset);

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
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tChildDataset
     * @throws Throwable if trouble
     */
    public EDDGridLonPM180(Erddap tErddap, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        EDDGrid oChildDataset) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridLonPM180 " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridLonPM180(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridLonPM180"; 
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

        if (oChildDataset == null)
            throw new RuntimeException(errorInMethod + "childDataset=null!");
        Test.ensureFileNameSafe(datasetID, "datasetID");
        if (datasetID.equals(oChildDataset.datasetID()))
            throw new RuntimeException(errorInMethod + 
                "This dataset's datasetID must not be the same as the child's datasetID.");

        //is oChildDataset a fromErddap from this erddap?
        //Get childDataset or localChildDataset. Work with stable local reference.
        EDDGrid tChildDataset = null;
        if (tErddap != null && oChildDataset instanceof EDDGridFromErddap) {
            EDDGridFromErddap tFromErddap = (EDDGridFromErddap)oChildDataset;
            String tSourceUrl = tFromErddap.getPublicSourceErddapUrl();
            if (tSourceUrl.startsWith(EDStatic.baseUrl) ||   //normally
                tSourceUrl.startsWith("http://127.0.0.1") ||
                tSourceUrl.startsWith("http://localhost")) { //happens during testing
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

        //UNUSUAL: if valid value not specified, copy from childDataset
        setReloadEveryNMinutes(tReloadEveryNMinutes < Integer.MAX_VALUE?
            tReloadEveryNMinutes : 
            tChildDataset.getReloadEveryNMinutes());
        setUpdateEveryNMillis( tUpdateEveryNMillis  < Integer.MAX_VALUE?
            tUpdateEveryNMillis :
            tChildDataset.updateEveryNMillis);

        //accessibleViaFiles if child avfDir is local
        String avfDir = tChildDataset.accessibleViaFilesDir();
        if (String2.isSomething(avfDir) &&
            (avfDir.startsWith("file://") || avfDir.startsWith("/") || avfDir.startsWith("\\"))) {
            //but not accessible if some url
            accessibleViaFilesDir       = avfDir;
            accessibleViaFilesRegex     = tChildDataset.accessibleViaFilesRegex();
            accessibleViaFilesRecursive = tChildDataset.accessibleViaFilesRecursive();
        }

        //make/copy the local globalAttributes
        localSourceUrl           = tChildDataset.localSourceUrl();
        sourceGlobalAttributes   = (Attributes)tChildDataset.sourceGlobalAttributes().clone();
        addGlobalAttributes      = (Attributes)tChildDataset.addGlobalAttributes().clone();
        combinedGlobalAttributes = (Attributes)tChildDataset.combinedGlobalAttributes().clone();
        combinedGlobalAttributes.set("title", 
             combinedGlobalAttributes.getString("title") + ", Lon+/-180");

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
        if (childLon.destinationMax() <= 180)
            throw new RuntimeException(errorInMethod + 
                "The child longitude axis has no values >180 (max=" + 
                childLon.destinationMax() + ")!");
        PrimitiveArray childLonValues = childLon.destinationValues();
        int nChildLonValues = childLonValues.size();

// old:           || sloni0, sloni179 || sloni180,  sloni359           ||
// old: [ignored] ||      0,  90, 179 ||      180, 270,  359           || [ignored]
// new:                                      -180, -90,   -1 insert359 || insert0,  0, 90, 179 
        //all of the searches use EXACT math 
        if (childLon.destinationMin() < 180) {
            sloni0   = childLonValues.binaryFindFirstGE(0,     nChildLonValues - 1,   0); //first index >=0
            sloni179 = childLonValues.binaryFindLastLE(sloni0, nChildLonValues - 1, 180); //last index <180
            if (childLonValues.getDouble(sloni179) == 180)
                sloni179--;
        } else {
            sloni0   = -1; //no values <180
            sloni179 = -1;
        }        
        sloni180 = sloni179 + 1;  //first index >=180
        if (childLonValues.getDouble(sloni180) > 360)
            throw new RuntimeException(errorInMethod + 
                "There are no child longitude values in the range 180 to 360!");
        sloni359 = childLonValues.binaryFindLastLE(sloni180, nChildLonValues - 1, 360); //last index <=360
        if (childLonValues.getDouble(sloni359) == 360 && //there is a value=360
            sloni359 > sloni180 &&  //and it isn't the only value in the range 180 to 360...
            sloni0 >= 0 &&                         //and if there is a sloni0
            childLonValues.getDouble(sloni0) == 0) //with value == 0
            sloni359--;  //360 is a duplicate of 0, so ignore it        
        PrimitiveArray newLonValues = childLonValues.subset(sloni180, 1, sloni359); //always a new backing array
        newLonValues.addOffsetScale(-360, 1);  //180 ... 359 -> -180 ... -1

        //set dloni: where are sloni after source values are rearranged to dest values
        dloni180 = 0;
        dloni359 = sloni359 - sloni180;
        if (sloni0 >= 0) {
            //dInsert SYSTEM IS NOT ACTIVE
            //create dInsert if there's a big gap
            double spacing = childLon.averageSpacing(); //avg is more reliable than any single value
            double lon359 = newLonValues.getDouble(newLonValues.size() - 1);
            double lon0   = childLonValues.getDouble(0);
            //e.g., gap of 2.1*spacing --floor--> 2 -> insert 1 value
            int insertN = Math2.roundToInt(Math.floor((lon0 - lon359) / spacing)) - 1;
            if (insertN >= 1) {  //enough space to insert 1 regularly spaced lon values

                //throw new RuntimeException(errorInMethod + 
                //    "The child longitude axis has no values >180 (max=" + 
                //    childLon.destinationMax() + ")!");

                insert359i = newLonValues.size();
                for (int i = 1; i <= insertN; i++) {
                    double td = Math2.niceDouble(lon359 + i * spacing, 12);
                    if (Math.abs(td) < 1e-8)  //be extra careful to catch 0
                        td = 0;
                    newLonValues.addDouble(td);
                }
                insert0i = newLonValues.size() - 1;
                if (verbose) String2.log(
                    "  'insert' is active: lon[" + insert359i + "]=" + newLonValues.getDouble(insert359i) +
                                         " lon[" + insert0i   + "]=" + newLonValues.getDouble(insert0i));
            }
            
            //append the 0 ... 179 lon values
            dloni0   = newLonValues.size();
            newLonValues.append(childLonValues.subset(sloni0, 1, sloni179));
            dloni179 = newLonValues.size() - 1;

        } else {
            dloni0   = -1; //no values <180
            dloni179 = -1;
        }

        //make the new lon axis
        EDVLonGridAxis newEDVLon = new EDVLonGridAxis(EDV.LON_NAME,
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
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() +
                 "dloni180=" + dloni180 + " dloni359=" + dloni359 + 
                " insert359i=" + insert359i + " insert0i=" + insert0i + 
                " dloni0=" + dloni0 + " dloni179=" + dloni179 + "\n": "") + 
            "localChildDatasetID=" + localChildDatasetID + "\n" +
            "\n*** EDDGridLonPM180 " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException( 
            "Error: EDDGridLonPM180 doesn't support method=\"sibling\".");
    }

    /** 
     * This returns a fileTable (formatted like 
     * FileVisitorDNLS.oneStep(tDirectoriesToo=false, last_mod is LongArray,
     * and size is LongArray of epochMillis)
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     */
    public Table accessibleViaFilesFileTable() {
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

        return tChildDataset.accessibleViaFilesFileTable();
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
        EDDGrid tChildDataset = childDataset;
        if (tChildDataset == null) {
            tChildDataset = erddap.gridDatasetHashMap.get(localChildDatasetID);
            if (tChildDataset == null) {
                EDD.requestReloadASAP(localChildDatasetID);
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(underlying local datasetID=" + localChildDatasetID + " not found)"); 
            }
        }
                
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
        //dest is: dloni180...dloni359, [insert359...insert0,] [dloni0...dloni179]
        //becomes      -180...-1,       [insert359...insert0,] [     0...     179]

        //really easy: all the requested lons are from the new right: >=dloni0
        if (dloni0 != -1 && rStart >= dloni0) {
            //1 request, and lon values are already correct
            IntArray newConstraints = (IntArray)tConstraints.subset(0, 1, tConstraints.size() - 1);
            newConstraints.set(li3 + 0, rStart - (dloni0 - sloni0));
            newConstraints.set(li3 + 2, rStop  - (dloni0 - sloni0));            
            return tChildDataset.getSourceData(tDataVariables, newConstraints);
        }

        //easy: all the requested lons are from the left: <=dloni359
        if (rStop <= dloni359) {
            //1 request, but need to adjust lon values
            IntArray newConstraints = (IntArray)tConstraints.subset(0, 1, tConstraints.size() - 1);
            newConstraints.set(li3 + 0, rStart + sloni180); // (sloni180 - dloni180), but dloni180 is 0
            newConstraints.set(li3 + 2, rStop  + sloni180); // (sloni180 - dloni180), but dloni180 is 0
            PrimitiveArray results[] = tChildDataset.getSourceData(tDataVariables, newConstraints);
            results[lonIndex] = (PrimitiveArray)results[lonIndex].clone();
            results[lonIndex].addOffsetScale(-360, 1);
            return results;
        }

        //map:
        //dest is: dloni180...dloni359, [insert359i...insert0i,]  [dloni0...dloni179]
        //becomes      -180...-1,       [insert359i...insert0i,]  [     0...     179]

        //Harder: need to make 0, 1, or 2 requests and merge them.
        //If this is a big request, this takes a lot of memory for the *short* time 
        //  when merging in memory.
        if (debugMode) String2.log(">> lon request= " + rStart + ":" + rStride + ":" + rStop); 

        //find request for lon 180-359: <=dloni359
        IntArray newConstraints = (IntArray)tConstraints.subset(0, 1, tConstraints.size() - 1);
        PrimitiveArray results180[] = null;
        if (rStart <= dloni359) {
            //some values are from source loni180 to loni359
            newConstraints.set(li3 + 0, rStart + sloni180); // (sloni180 - dloni180), but dloni180 is 0
            newConstraints.set(li3 + 2, sloni359);
            results180 = tChildDataset.getSourceData(tDataVariables, newConstraints);
            results180[lonIndex] = (PrimitiveArray)results180[lonIndex].clone();
            results180[lonIndex].addOffsetScale(-360, 1);
            if (debugMode)
                String2.log(">> got results180 from source lon[" + (rStart + sloni180) + ":" + rStride + ":" + sloni359 + "]");
        }

        //find out if 'insert' is active and relevant (i.e., some values are requested)
        boolean insertActive = insert359i >= 0; //basic system is active
        if (insertActive && (rStop < insert359i || rStart > insert0i))
            insertActive = false;  //shouldn't happen because should have been dealt with above
        int firstInserti = -1;
        if (insertActive) {
            //Find first loni >= insert359i which is among actually requested lon values (given rStride).
            firstInserti = Math.max(insert359i, rStart); 
            if (rStride > 1) {
                int mod = (firstInserti - rStart) % rStride;
                if (mod > 0)
                    firstInserti += rStride - mod;
            }
            if (firstInserti > insert0i) {
                firstInserti = -1;
                insertActive = false; //stride walks over (skips) all inserted lons
            }
        }
        int lastInserti = -1;
        int insertNLon = 0;
        if (insertActive) {
            lastInserti = Math.min(rStop, insert0i);
            lastInserti -= (lastInserti - rStart) % rStride; //perhaps last=first
            insertNLon = OpendapHelper.calculateNValues(firstInserti, rStride, lastInserti);
            if (debugMode)
                String2.log(">> will insert lon[" + firstInserti + ":" + rStride + ":" + lastInserti + "]");
        }

        //find request for lon 0-179: >=dloni0
        PrimitiveArray results0[] = null;
        if (rStop >= dloni0) {
            //Find first loni >=dloni0 which is among actually requested lon values (given rStride).
            int floni = dloni0; 
            if (rStride > 1) {
                int mod = (floni - rStart) % rStride;
                if (mod > 0)
                    floni += rStride - mod;
            }

            //some values are from source loni0 to loni179
            int tStart = floni - (dloni0 - sloni0);
            int tStop  = rStop - (dloni0 - sloni0);
            newConstraints.set(li3 + 0, tStart); 
            newConstraints.set(li3 + 2, tStop);            
            results0 = tChildDataset.getSourceData(tDataVariables, newConstraints);
            if (debugMode)
                String2.log(">> got results0 from source lon[" + tStart + ":" + rStride + ":" + tStop + "]");
        }

        //map:
        //dest is: dloni180...dloni359, [insert359i...insert0i,]  [dloni0...dloni179]
        //becomes      -180...-1,       [insert359i...insert0i,]  [     0...     179]

        //now build results[] by reading chunks alternately from the 3 sources 
        //what is chunk size from 180 request, from insert, and from 0 request?
        int chunk180    = results180 == null? 0 : 1;
        int chunkInsert = insertNLon <= 0?    0 : 1;
        int chunk0      = results0   == null? 0 : 1;
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
                chunk180    *= avN;
                chunkInsert *= avN;
                chunk0      *= avN; 
            }
            if (avi == lonIndex) {
                if (results180 != null) chunk180    *= results180[avi].size();
                if (insertNLon >  0)    chunkInsert *= insertNLon;
                if (results0   != null) chunk0      *= results0[avi].size();
            }
            //all axes:
            nValues *= avN;
        }
       
        //and make the results data variables
        for (int tdv = 0; tdv < tnDV; tdv++) 
            results[nAV + tdv] = PrimitiveArray.factory(
                tDataVariables[tdv].sourceDataTypeClass(), nValues, false);

        //dest is: dloni180...dloni359, [insert359i...insert0i,]  [dloni0...dloni179]
        //becomes      -180...-1,       [insert359i...insert0i,]  [     0...     179]

        //copy the dataVariables values into place
        int po180 = 0;
        int po0 = 0;
        int nextDest = 0;
        while (nextDest < nValues) {
            for (int tdv = 0; tdv < tnDV; tdv++) {
                if (results180  != null) 
                    results[nAV + tdv].addFromPA(results180[nAV + tdv], po180, chunk180);
                if (chunkInsert >  0)    
                    results[nAV + tdv].addNDoubles(chunkInsert, 
                        tDataVariables[tdv].safeDestinationMissingValue());
                if (results0    != null) 
                    results[nAV + tdv].addFromPA(results0[  nAV + tdv], po0,   chunk0);
            }
            po180 += chunk180;  //may be 0
            po0   += chunk0;    //may be 0
            nextDest += chunk180 + chunkInsert + chunk0;

            if (debugMode && nextDest >= nValues) {
                if (results180 != null) 
                    Test.ensureEqual(results180[nAV].size(), po180, "po180");
                if (results0   != null)    
                    Test.ensureEqual(results0[nAV].size(),   po0,   "po0");
                Test.ensureEqual(results[nAV].size(), nValues, "nValues");
            }
        }
        return results;
    }

    /** 
     * This runs generateDatasetsXmlFromErddapCatalog.
     * 
     * @param oLocalSourceUrl  A complete URL of an ERDDAP (ending with "/erddap/"). 
     * @param datasetNameRegex  The lowest level name of the dataset must match this, 
     *    e.g., ".*" for all dataset Names.
     * @return a String with the results
     */
    public static String generateDatasetsXmlFromErddapCatalog( 
        String oLocalSourceUrl, String datasetNameRegex) throws Throwable {

        String2.log("*** EDDGridLonPM180.generateDatasetsXmlFromErddapCatalog(" +
            oLocalSourceUrl + ")");

        if (oLocalSourceUrl == null)
            throw new RuntimeException(String2.ERROR + ": 'localSourceUrl' is null.");
        if (!oLocalSourceUrl.endsWith("/erddap/"))
            throw new RuntimeException(String2.ERROR + ": 'localSourceUrl' doesn't end with '/erddap/'.");

        //get table with datasetID minLon, maxLon
        String query = oLocalSourceUrl + "tabledap/allDatasets.csv" + 
            "?datasetID,title,griddap,minLongitude,maxLongitude" +
            "&dataStructure=" + SSR.minimalPercentEncode("\"grid\"") + 
            "&maxLongitude>180" +
            (String2.isSomething(datasetNameRegex)? 
                "&datasetID=" + SSR.minimalPercentEncode("~\"" + datasetNameRegex + "\""): "") +
            "&orderBy(\"datasetID\")";
        String lines[] = null;
        try {
            lines = SSR.getUrlResponse(query);
        } catch (Throwable t) {
            if (t.toString().indexOf("no data") >= 0)
                return "<!-- No griddap datasets at that ERDDAP match that regex\n" +
                       "     and have longitude values &gt;180. -->\n";
            throw t;
        }
        Table table = new Table();
        table.readASCII(query, lines, 0, 2, null, null, null, null, false); //simplify
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
"<dataset type=\"EDDGridLonPM180\" datasetID=\"" + 
    datasetIDPA.getString(row) + "_LonPM180\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"" + 
    datasetIDPA.getString(row) + "_LonPM180Child\">\n" +
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

        String2.log("\n*** EDDGridLonPM180.testGenerateDatasetsXmlFromErddapCatalog() ***\n");
        testVerboseOn();
        String url = "http://coastwatch.pfeg.noaa.gov/erddap/";
        //erdMH1chlamday is -179 to 179, so nothing to do, so won't be in results
        //others are good, different test cases
        String regex = "(erdMH1chlamday|erdMWchlamday|erdMHsstnmday|erdMBsstdmday|erdPHsstamday)";

        String results = generateDatasetsXmlFromErddapCatalog(url, regex) + "\n"; 

        String expected =
"<dataset type=\"EDDGridLonPM180\" datasetID=\"erdMBsstdmday_LonPM180\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMBsstdmday_LonPM180Child\">\n" +
"        <!-- SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime (Monthly Composite)\n" +
"             minLon=120.0 maxLon=320.0 -->\n" +
"        <sourceUrl>http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMBsstdmday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n" +
"<dataset type=\"EDDGridLonPM180\" datasetID=\"erdMHsstnmday_LonPM180\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMHsstnmday_LonPM180Child\">\n" +
"        <!-- SST, Aqua MODIS, NPP, Nighttime (11 microns), DEPRECATED OLDER VERSION (Monthly Composite)\n" +
"             minLon=0.0 maxLon=360.0 -->\n" +
"        <sourceUrl>http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHsstnmday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n" +
"<dataset type=\"EDDGridLonPM180\" datasetID=\"erdMWchlamday_LonPM180\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMWchlamday_LonPM180Child\">\n" +
"        <!-- Chlorophyll-a, Aqua MODIS, NPP, 0.0125&#xc2;&#xb0;, West US, EXPERIMENTAL (Monthly Composite)\n" +
"             minLon=205.0 maxLon=255.0 -->\n" +
"        <sourceUrl>http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMWchlamday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n" +
"<dataset type=\"EDDGridLonPM180\" datasetID=\"erdPHsstamday_LonPM180\" active=\"true\">\n" +
"    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdPHsstamday_LonPM180Child\">\n" +
"        <!-- SST, Pathfinder Ver 5.0, Day and Night, 4.4 km, Global, Science Quality (Monthly Composite) DEPRECATED\n" +
"             minLon=0.02197266 maxLon=359.978 -->\n" +
"        <sourceUrl>http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdPHsstamday</sourceUrl>\n" +
"    </dataset>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridLonPM180FromErddapCatalog", url, regex},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, 
            "Unexpected results from GenerateDatasetsXml.doIt. results=\n" + results);

    }

    /**
     * This tests a dataset that is initially all GT180.
     *
     * @throws Throwable if trouble
     */
    public static void testGT180() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.testGT180() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;

      try {

        EDDGrid eddGrid;
        try {
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_erdMWchlamday_LonPM180");       
        } catch (Throwable t) {
            String2.pressEnterToContinue(
                "\n" + MustBe.throwableToString(t) +
                "\nEDDGridLonPM180.testGT180() requires erdMWchlamday in the localhost ERDDAP.\n" +
                "Make it so and this will try again.");
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_erdMWchlamday_LonPM180");       
        }

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_GT180_Entire", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Float64 time[time = 2];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 2321];\n" +
"  Float64 longitude[longitude = 4001];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[time = 2][altitude = 1][latitude = 2321][longitude = 4001];\n" +
"    MAPS:\n" +
"      Float64 time[time = 2];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 2321];\n" +
"      Float64 longitude[longitude = 4001];\n" +
"  } chlorophyll;\n" +
"} test_erdMWchlamday_LonPM180;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_GT180_Entire", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -155.0, -105.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
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
"    Float64 geospatial_lon_max -105.0;\n" +
"    Float64 geospatial_lon_min -155.0;\n" +
"    Float64 geospatial_lon_resolution 0.0125;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting -155.0;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //lon values
        userDapQuery = "longitude[(-155):400:(-105)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_GT180_lon", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -155.0, -150.0, -145.0, -140.0, -135.0, -130.0, -125.0, -120.0, -115.0, -110.0, -105.0, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire lon range
        userDapQuery = "chlorophyll[(2002-08-16T12:00:00Z)][][(35):80:(36)][(-155):400:(-105)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_GT180_1", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//from same request from coastwatch erddap: erdMWchlamday  lon=205 to 255
// but I subtracted 360 from the lon values to get the values below
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-155.0,0.06277778\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-150.0,0.091307685\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-145.0,0.06966666\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-140.0,0.0655\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-135.0,0.056166667\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-130.0,0.080142856\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-125.0,0.12250001\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-120.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-115.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-110.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-105.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-155.0,0.0695\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-150.0,0.09836364\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-145.0,0.07525001\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-140.0,0.072000004\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-135.0,0.08716667\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-130.0,0.07828571\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-125.0,0.1932857\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-120.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-115.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-110.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-105.0,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon subset   (-119 will be rolled back to -120)
        userDapQuery = "chlorophyll[(2002-08-16T12:00:00Z)][][(35):80:(36)][(-145):400:(-119)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_GT180_2", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//from same request from coastwatch erddap: erdMWchlamday  lon=205 to 255
// but I subtracted 360 from the lon values to get the values below
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-145.0,0.06966666\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-140.0,0.0655\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-135.0,0.056166667\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-130.0,0.080142856\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-125.0,0.12250001\n" +
"2002-08-16T12:00:00Z,0.0,35.0,-120.0,NaN\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-145.0,0.07525001\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-140.0,0.072000004\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-135.0,0.08716667\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-130.0,0.07828571\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-125.0,0.1932857\n" +
"2002-08-16T12:00:00Z,0.0,36.0,-120.0,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image
        userDapQuery = "chlorophyll[(2002-08-16T12:00:00Z)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_GT180", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test of /files/ system for fromErddap in local host dataset
        String2.log("\n* Test getting /files/ from local host erddap...");
        results = SSR.getUrlResponseString(
            "http://localhost:8080/cwexperimental/files/test_erdMWchlamday_LonPM180/");
        expected = "MW2002182&#x5f;2002212&#x5f;chla&#x2e;nc"; //"MW2002182_2002212_chla.nc";
        Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

        String2.log("\n*** EDDGridLonPM180.testGT180() finished.");
      } catch (Throwable t) {
          String2.pressEnterToContinue("\n" +
              MustBe.throwableToString(t) + "\n" +
              "EDDGridLonPM180.test1to359() requires erdPHsstamday in the localhost ERDDAP.");
      }
    }

    /**
     * This tests a dataset that is initially 1to359.
     *
     * @throws Throwable if trouble
     */
    public static void test1to359() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.test1to359() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;

        EDDGrid eddGrid;
      try {
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "erdPHsstamday_LonPM180");       
 
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_1to359_Entire", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Float64 time[time = 340];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 4096];\n" +
"  Float64 longitude[longitude = 8192];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sst[time = 340][altitude = 1][latitude = 4096][longitude = 8192];\n" +
"    MAPS:\n" +
"      Float64 time[time = 340];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 4096];\n" +
"      Float64 longitude[longitude = 8192];\n" +
"  } sst;\n" +
"} erdPHsstamday_LonPM180;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_1to359_Entire", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        String2.log(results);
        expected = 
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -179.97804101541936, 179.97801367541936;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
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
"    Float64 geospatial_lon_max 179.97801367541936;\n" +
"    Float64 geospatial_lon_min -179.97804101541936;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting -179.97804101541936;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //lon values
        //this tests correct jump across lon 0
        userDapQuery = "longitude[(-179.97):1638:(179.97)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_1to359_lon", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -179.97804101541936, -107.99562460925162, -36.01320820308382, 35.9692355539226, 107.95165196009035, 179.9340683662581, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon values near 0
        userDapQuery = "longitude[(-.12):1:(.12)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_1to359_lonNear0", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
//verified in browser with longitude[(359.89010938167746):1:(359.978)] and longitude[(.02):1:(.11)]
"longitude, degrees_east, -0.1098906183225381, -0.06594530916123631, -0.02199999999999136, 0.02197266, 0.06591796916127457, 0.10986327832254915, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //almost entire new lon range
        userDapQuery = "sst[(1981-10-16)][][(0):22:(1)][(-179.97):1638:(179.97)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_1to359_1", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-179.97804101541936,28.425\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-107.99562460925162,22.125\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-36.01320820308382,27.15\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,35.9692355539226,NaN\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,107.95165196009035,30.9\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,179.9340683662581,28.575\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-179.97804101541936,28.35\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-107.99562460925162,22.35\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-36.01320820308382,27.525\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,35.9692355539226,NaN\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,107.95165196009035,30.15\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,179.9340683662581,28.35\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //left and right, smaller range
        userDapQuery = "sst[(1981-10-16)][][(0):22:(1)][(-108):1638:(120)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_1to359_1s", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-107.99562460925162,22.125\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-36.01320820308382,27.15\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,35.9692355539226,NaN\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,107.95165196009035,30.9\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-107.99562460925162,22.35\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-36.01320820308382,27.525\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,35.9692355539226,NaN\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,107.95165196009035,30.15\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, just <0  (ask for into positive, but it will roll back)
        userDapQuery = "sst[(1981-10-16)][][(0):22:(1)][(-179.97):1638:(10)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_1to359_2", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//verified in browser from coastwatch erdPHsstamday  lon 180.02 to 323.99
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-179.97804101541936,28.425\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-107.99562460925162,22.125\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,-36.01320820308382,27.15\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-179.97804101541936,28.35\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-107.99562460925162,22.35\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,-36.01320820308382,27.525\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, just > 0 
        userDapQuery = "sst[(1981-10-16)][][(0):22:(1)][(35.97):1638:(179.97)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_1to359_3", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//verified in browser from coastwatch erdPHsstamday lon 35.97 to 179.93
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,35.9692355539226,NaN\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,107.95165196009035,30.9\n" +
"1981-10-16T12:00:00Z,0.0,0.021972656898654463,179.9340683662581,28.575\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,35.9692355539226,NaN\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,107.95165196009035,30.15\n" +
"1981-10-16T12:00:00Z,0.0,0.9887695604395645,179.9340683662581,28.35\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image
        userDapQuery = "sst[(1981-10-16T12:00:00Z)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_1to359", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //test of /files/ system for fromErddap in local host dataset
        String2.log("\n* Test getting /files/ from local host erddap...");
        results = SSR.getUrlResponseString(
            "http://localhost:8080/cwexperimental/files/test_erdPHsstamday_LonPM180/");
        expected = "PH1981244&#x5f;1981273&#x5f;ssta&#x2e;nc"; //"PH1981244_1981273_ssta.nc";
        Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

        String2.log("\n*** EDDGridLonPM180.test1to359 finished.");

      } catch (Throwable t) {
          String2.pressEnterToContinue("\n" +
              MustBe.throwableToString(t) + "\n" +
              "EDDGridLonPM180.test1to359() requires erdPHsstamday in the localhost ERDDAP.");
      }

    }


    /**
     * This tests a dataset that is initially 0to360.
     *
     * @throws Throwable if trouble
     */
    public static void test0to360() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.test0to360() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;

        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test_erdMHsstnmday_LonPM180");       

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_0to360_Entire", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Float64 time[time = 133];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 4320];\n" +
"  Float64 longitude[longitude = 8639];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sst[time = 133][altitude = 1][latitude = 4320][longitude = 8639];\n" +
"    MAPS:\n" +
"      Float64 time[time = 133];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 4320];\n" +
"      Float64 longitude[longitude = 8639];\n" +
"  } sst;\n" +
"} test_erdMHsstnmday_LonPM180;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_0to360_Entire", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        String2.log(results);
        expected = 
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -179.97916425512213, 179.97916425512213;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
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
"    Float64 geospatial_lon_max 179.97916425512213;\n" +
"    Float64 geospatial_lon_min -179.97916425512213;\n" +
"    Float64 geospatial_lon_resolution 0.04167148975575877;\n" +
"    String geospatial_lon_units \"degrees_east\";\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

expected = 
"    Float64 Westernmost_Easting -179.97916425512213;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 30));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //lon values
        //this tests correct jump across lon 0
        userDapQuery = "longitude[(-179.98):1234:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_0to360_lon", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -179.97916425512213, -128.55654589651581, -77.13392753790947, -25.711309179303157, 25.71130917930316, 77.13392753790947, 128.5565458965158, 179.97916425512213, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon values near 0
        userDapQuery = "longitude[(-.125):1:(.125)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_0to360_lonNear0", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
//verified in browser with longitude[(359.88):1:(360)] and longitude[(0):1:(.125)]
"longitude, degrees_east, -0.12501446926728477, -0.08334297951154213, -0.04167148975574264, 0.0, 0.04167148975575877, 0.08334297951151753, 0.1250144692672763, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire new lon range
        userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(-179.98):1234:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_0to360_1", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-179.97916425512213,28.06726\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-128.55654589651581,22.61092\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-77.13392753790947,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-25.711309179303157,25.09381\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,25.71130917930316,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,77.13392753790947,29.521\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,128.5565458965158,27.90518\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,179.97916425512213,27.96184\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-179.97916425512213,28.14974\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-128.55654589651581,22.72137\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-77.13392753790947,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-25.711309179303157,25.48109\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,25.71130917930316,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,77.13392753790947,29.48155\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,128.5565458965158,27.58245\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,179.97916425512213,28.20998\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, just <0  (ask for into positive, but it will roll back)
        userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(-179.98):1234:(10)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_0to360_2", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//verified in browser from coastwatch erdMHsstnmday  lon 180.02 to 334.3
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-179.97916425512213,28.06726\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-128.55654589651581,22.61092\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-77.13392753790947,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-25.711309179303157,25.09381\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-179.97916425512213,28.14974\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-128.55654589651581,22.72137\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-77.13392753790947,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-25.711309179303157,25.48109\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, just > 0 
        userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(25.7):1234:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_0to360_3", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//verified in browser from coastwatch erdMHsstnmday lon 25.7 to 179.98
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,25.71130917930316,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,77.13392753790947,29.521\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,128.5565458965158,27.90518\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,179.97916425512213,27.96184\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,25.71130917930316,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,77.13392753790947,29.48155\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,128.5565458965158,27.58245\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,179.97916425512213,28.20998\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //values near 0 
        userDapQuery = "sst[(2007-08-16T12:00:00Z)][][(0):23:(1)][(-.125):1:(.125)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_0to360_4", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//This is an important test of no duplicate lon values (from lon 0 and 360)
//verified in browser from coastwatch erdMHsstnmday lon 359.875 to 360, and 0 to 0.125
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-0.12501446926728477,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-0.08334297951154213,24.58318\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,-0.04167148975574264,25.307896\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.0,25.46173\n" +   //360 has NaN, 0 has 25.46173!
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.04167148975575877,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.08334297951151753,25.25877\n" +
"2007-08-16T12:00:00Z,0.0,0.020838156980772737,0.1250144692672763,25.175934\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-0.12501446926728477,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-0.08334297951154213,25.42515\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,-0.04167148975574264,25.410454\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.0,25.75864\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.04167148975575877,NaN\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.08334297951151753,25.90351\n" +
"2007-08-16T12:00:00Z,0.0,0.9793933780967734,0.1250144692672763,25.788765\n";
//In this dataset, lon=360 has all NaNs!  Kooky!
//So it is really good that EDDGridLonPM180 uses values from 0, not values from 360!
//see sst[(2007-08-16):1:(2007-08-16)][(0.0):1:(0.0)][(-90):1:(90)][(360):1:(360)]
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image
        userDapQuery = "sst[(2007-08-16T12:00:00Z)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_0to360", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

//!!!??? what's with that gap near lon=0? It's from the source.
//The source has odd gaps at lon 358 to 360, see
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHsstnmday.graph?sst[(2013-09-16T00:00:00Z)][(0.0)][(-2.479741):(-0.479278)][(357.999768):(360)]&.draw=surface&.vars=longitude|latitude|sst&.colorBar=|||||
//2015-11-25 I notified Roy, but these are deprecated datasets so probably won't be fixed

        String2.log("\n*** EDDGridLonPM180.test0to360 finished.");

    }

    /**
     * This tests a dataset that is initially 120to320.
     *
     * @throws Throwable if trouble
     */
    public static void test120to320() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.test120to320() *****************\n");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = true;
        String name, tName, userDapQuery, results, expected, error;
        int po;
        String dir = EDStatic.fullTestCacheDirectory;
        EDDGrid eddGrid;

        //test notApplicable (dataset maxLon already <180)
        try {
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, "notApplicable_LonPM180");       
            throw new RuntimeException("shouldn't get here");
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            if (msg.indexOf("Error in EDDGridLonPM180(notApplicable_LonPM180) constructor:\n" +
                "The child longitude axis has no values >180 (max=179.9792)!") < 0)
                throw t;
        }

        //test120 to 320
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "erdMBsstdmday_LonPM180");       

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_120to320_Entire", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Float64 time[time = 66];\n" + //changes
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 4401];\n" +
"  Float64 longitude[longitude = 14400];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sst[time = 66][altitude = 1][latitude = 4401][longitude = 14400];\n" +  //changes
"    MAPS:\n" +
"      Float64 time[time = 66];\n" +  //changes
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 4401];\n" +
"      Float64 longitude[longitude = 14400];\n" +
"  } sst;\n" +
"} erdMBsstdmday_LonPM180;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", dir, 
            eddGrid.className() + "_120to320_Entire", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        String2.log(results);
        expected = 
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -180.0, 179.975;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 3;\n" +
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
            eddGrid.className() + "_120to320_lon", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -180.0, -128.575, -77.14999999999998, -25.725, 25.7, 77.125, 128.55, 179.975, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //lon values near 0
        userDapQuery = "longitude[(-.075):1:(.075)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_lonNear0", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        results = String2.replaceAll(results, "\n", ", ");
        expected = 
"longitude, degrees_east, -0.075, -0.05, -0.025, 0.0, 0.025, 0.05, 0.075, ";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //entire new lon range
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-180):2057:(179.975)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
//erdMBsstdmday natively 120...320(-40)
//verified by cw erddap  lon (128.55):2057:(180)  and (180):2057:(320)
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMBsstdmday.htmlTable?sst[(2008-04-15):1:(2008-04-15)][(0.0):1:(0.0)][(0):1:(0)][(128.55):2057:(180)]
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-180.0,26.41\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,179.975,26.31\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-180.0,26.4708\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,128.55,30.36\n" +
"2008-04-16T00:00:00Z,0.0,0.1,179.975,26.515\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, left+insert+right sections 
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(135)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_4", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,128.55,30.36\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just left
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(-70)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1L", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-77.14999999999998,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just left, 1 point
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(-120)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1Lb", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-128.575,26.7495\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just insert   //insert values are between -40 and 120
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-25.725):2057:(27)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1I", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,25.7,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just insert, 1 point
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-25.725):2057:(-20)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1Ib", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-25.725,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just right
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(128.55):2057:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1R", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,179.975,26.31\n" +
"2008-04-16T00:00:00Z,0.0,0.1,128.55,30.36\n" +
"2008-04-16T00:00:00Z,0.0,0.1,179.975,26.515\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset: just right, 1 point
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(128.55):2057:(135)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_1Rb", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.1,128.55,30.36\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, left + insert
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-180):2057:(27)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_2", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-180.0,26.41\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-180.0,26.4708\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-77.14999999999998,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-25.725,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,25.7,NaN\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, insert + right
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(25.7):2057:(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_3", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,179.975,26.31\n" +
"2008-04-16T00:00:00Z,0.0,0.1,25.7,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,77.125,NaN\n" +
"2008-04-16T00:00:00Z,0.0,0.1,128.55,30.36\n" +
"2008-04-16T00:00:00Z,0.0,0.1,179.975,26.515\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //subset of that, left + right  (jump over insert)
        userDapQuery = "sst[(2008-04-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):" + (2057*5) + ":(179.98)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            eddGrid.className() + "_120to320_3", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,sst\n" +
"UTC,m,degrees_north,degrees_east,degree_C\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,-128.575,26.8195\n" +
"2008-04-16T00:00:00Z,0.0,2.498002E-15,128.55,30.61\n" +
"2008-04-16T00:00:00Z,0.0,0.1,-128.575,26.7495\n" +
"2008-04-16T00:00:00Z,0.0,0.1,128.55,30.36\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test image
        userDapQuery = "sst[(2008-04-16T00:00:00Z)][][][]&.land=under";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_120to320", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);

        String2.log("\n*** EDDGridLonPM180.test120to320 finished.");
        debugMode = oDebugMode;
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.test() *****************\n");
        /* */
        testGenerateDatasetsXmlFromErddapCatalog(); 
        testGT180();  //this also tests /files/ working for fromErddap localhost dataset
        test1to359(); //this also tests /files/ working for fromNcFiles dataset
        test0to360();
        test120to320(); 

        //note that I have NO TEST of dataset where lon isn't the rightmost dimension.
        //so there is a test for that in the constructor, 
        //which asks admin to send me an example for testing.
        /*  */

        //not usually run
    }



}
