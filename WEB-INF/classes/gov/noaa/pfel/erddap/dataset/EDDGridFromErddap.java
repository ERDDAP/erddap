/* 
 * EDDGridFromErddap Copyright 2008, NOAA.
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
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
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
import gov.noaa.pfel.erddap.util.Subscriptions;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;

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
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDDGridFromErddap extends EDDGrid implements FromErddap { 

    protected double sourceErddapVersion = 1.22; //default = last version before /version service was added

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;

    protected String publicSourceErddapUrl;

    /**
     * This constructs an EDDGridFromErddap based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromErddap"&gt; 
     *    having just been read.  
     * @return an EDDGridFromErddap.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridFromErddap fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDGridFromErddap(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        int tUpdateEveryNMillis = 0;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tLocalSourceUrl = null;
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
            if      (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<updateEveryNMillis>")) {}
            else if (localTags.equals("</updateEveryNMillis>")) tUpdateEveryNMillis = String2.parseInt(content); 

            //Since this erddap can never be logged in to the remote ERDDAP, 
            //it can never get dataset info from the remote erddap dataset (which should have restricted access).
            //Plus there is no way to pass accessibleTo info between ERDDAP's (but not to users).
            //So there is currently no way to make this work. 
            //So it is disabled.
            //else if (localTags.equals( "<accessibleTo>")) {}
            //else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;

            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
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
        return new EDDGridFromErddap(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tLocalSourceUrl);
    }

    /**
     * The constructor.
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
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data (use Integer.MAX_VALUE for never).
     * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
     * @throws Throwable if trouble
     */
    public EDDGridFromErddap(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tLocalSourceUrl) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridFromErddap " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridFromErddap(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridFromErddap"; 
        datasetID = tDatasetID;
        //setAccessibleTo(tAccessibleTo);  disabled. see above.
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        localSourceUrl = tLocalSourceUrl;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        setUpdateEveryNMillis(tUpdateEveryNMillis);
        if (tLocalSourceUrl.indexOf("/tabledap/") > 0)
            throw new RuntimeException(
                "For datasetID=" + tDatasetID + 
                ", use type=\"EDDTableFromErddap\", not EDDGridFromErddap, in datasets.xml.");
        publicSourceErddapUrl = convertToPublicSourceUrl(tLocalSourceUrl);

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

            //Ensure quickRestart information is recent.
            //If too old: abandon construction, delete quickRestart file, flag dataset reloadASAP
            ensureQuickRestartInfoIsRecent(datasetID, getReloadEveryNMinutes(), 
                creationTimeMillis, quickRestartFullFileName());

        }

        //open the connection to the opendap source
        DConnect dConnect = null;
        if (quickRestartAttributes == null)
            dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);

        //setup via info.json
        //source http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla5day
        //json   http://coastwatch.pfeg.noaa.gov/erddap/info/erdMHchla5day/index.json
        String jsonUrl = String2.replaceAll(tLocalSourceUrl, "/griddap/", "/info/") + "/index.json";

        byte sourceInfoBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(jsonUrl) : //has timeout and descriptive error
            ((ByteArray)quickRestartAttributes.get("sourceInfoBytes")).toArray();          
            
        Table table = new Table();
        table.readJson(jsonUrl, 
            new String(sourceInfoBytes, "UTF-8"));  //.json should always be UTF-8

        //go through the rows of table from bottom to top
        int nRows = table.nRows();
        Attributes tSourceAttributes = new Attributes();
        ArrayList tAxisVariables = new ArrayList();
        ArrayList tDataVariables = new ArrayList();
        for (int row = nRows-1; row >= 0; row--) {  

            //"columnNames": ["Row Type", "Variable Name", "Attribute Name", "Data Type", "Value"],
            //"columnTypes": ["String", "String", "String", "String", "String"],
            //"rows": [
            //     ["attribute", "NC_GLOBAL", "acknowledgement", "String", "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD"],
            //     ["dimension", "longitude", "", "double", "nValues=8640, evenlySpaced=true, averageSpacing=0.04166667052552379"],
            //     atts...
            //     ["variable", "chlorophyll", "", "float", "time, altitude, latitude, longitude"],
            //     atts...
            String rowType  = table.getStringData(0, row);
            String varName  = table.getStringData(1, row);
            String attName  = table.getStringData(2, row);
            String dataType = table.getStringData(3, row);
            String value    = table.getStringData(4, row);

            if (rowType.equals("attribute")) {
                if (dataType.equals("String")) {
                    tSourceAttributes.add(attName, value);
                } else {
                    Class tClass = PrimitiveArray.elementStringToClass(dataType);
                    PrimitiveArray pa = PrimitiveArray.csvFactory(tClass, value);
                    tSourceAttributes.add(attName, pa);
                }

            } else if (rowType.equals("dimension")) {
                PrimitiveArray tSourceValues = quickRestartAttributes == null?
                    OpendapHelper.getPrimitiveArray(dConnect, "?" + varName) :
                    quickRestartAttributes.get(
                        "sourceValues_" + String2.encodeVariableNameSafe(varName));

                Attributes tAddAttributes = new Attributes();
                //is this the lon axis?
                if (EDV.LON_NAME.equals(varName)) {
                    tAxisVariables.add(new EDVLonGridAxis(varName, 
                        tSourceAttributes, tAddAttributes, tSourceValues));

                //is this the lat axis?
                } else if (EDV.LAT_NAME.equals(varName)) {
                    tAxisVariables.add(new EDVLatGridAxis(varName, 
                        tSourceAttributes, tAddAttributes, tSourceValues));

                //is this the alt axis?
                } else if (EDV.ALT_NAME.equals(varName)) {
                    tAxisVariables.add(new EDVAltGridAxis(varName, 
                        tSourceAttributes, tAddAttributes, tSourceValues));

                //is this the depth axis?
                } else if (EDV.DEPTH_NAME.equals(varName)) {
                    tAxisVariables.add(new EDVDepthGridAxis(varName, 
                        tSourceAttributes, tAddAttributes, tSourceValues)); 

                //is this the time axis?
                } else if (EDV.TIME_NAME.equals(varName)) {
                    tAxisVariables.add(new EDVTimeGridAxis(varName, 
                        tSourceAttributes, tAddAttributes, tSourceValues));

                //is this a timestamp axis?
                } else if (EDVTimeStampGridAxis.hasTimeUnits(tSourceAttributes, tAddAttributes)) {
                    tAxisVariables.add(new EDVTimeStampGridAxis(varName, varName,
                        tSourceAttributes, tAddAttributes, tSourceValues));

                //it is some other axis variable
                } else {
                    tAxisVariables.add(new EDVGridAxis(varName, varName,
                        tSourceAttributes, tAddAttributes, tSourceValues));
                    ((EDVGridAxis)tAxisVariables.get(tAxisVariables.size() - 1)).setActualRangeFromDestinationMinMax();
                }

                //make new tSourceAttributes
                tSourceAttributes = new Attributes();

            //a grid variable
            } else if (rowType.equals("variable")) {

                //deal with remote not having ioos_category, but this ERDDAP requiring it
                Attributes tAddAttributes = new Attributes();
                if (EDStatic.variablesMustHaveIoosCategory &&
                    tSourceAttributes.getString("ioos_category") == null) {

                    //guess ioos_category   (alternative is always assign "Unknown")
                    Attributes tAtts = EDD.makeReadyToUseAddVariableAttributesForDatasetsXml(
                        tSourceAttributes, varName, false, false); //tryToAddColorBarMinMax, tryToFindLLAT
                    tAddAttributes.add("ioos_category", tAtts.getString("ioos_category"));
                }

                EDV edv;
                if (varName.equals(EDV.TIME_NAME))
                    throw new RuntimeException(errorInMethod +
                        "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
                else if (EDVTime.hasTimeUnits(tSourceAttributes, tAddAttributes)) 
                    edv = new EDVTimeStamp(varName, varName,
                        tSourceAttributes, tAddAttributes, dataType);  
                else edv = new EDV(varName, varName, 
                    tSourceAttributes, tAddAttributes, dataType, 
                    Double.NaN, Double.NaN);  //hard to get min and max
                edv.extractAndSetActualRange();
                tDataVariables.add(edv);
   
                //make new tSourceAttributes
                tSourceAttributes = new Attributes();

            //unexpected type
            } else throw new RuntimeException("Unexpected rowType=" + rowType + ".");
        }
        if (tAxisVariables.size() == 0)
            throw new RuntimeException("No axisVariables found!");
        sourceGlobalAttributes = tSourceAttributes; //at the top of table, so collected last
        addGlobalAttributes = new Attributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        combinedGlobalAttributes.removeValue("null");

        int nav = tAxisVariables.size();
        axisVariables = new EDVGridAxis[nav]; 
        for (int av = 0; av < nav; av++) {
            //reverse the order, since read (above) from bottom to top
            axisVariables[av] = (EDVGridAxis)tAxisVariables.get(nav - av - 1); 
            String tName = axisVariables[av].destinationName();
            if      (tName.equals(EDV.LON_NAME))   lonIndex   = av;
            else if (tName.equals(EDV.LAT_NAME))   latIndex   = av;
            else if (tName.equals(EDV.ALT_NAME))   altIndex   = av;
            else if (tName.equals(EDV.DEPTH_NAME)) depthIndex = av;
            else if (tName.equals(EDV.TIME_NAME))  timeIndex  = av;
        }

        int ndv = tDataVariables.size();
        dataVariables = new EDV[ndv];
        for (int dv = 0; dv < ndv; dv++)
            //reverse the order, since read (above) from bottom to top
            dataVariables[dv] = (EDV)tDataVariables.get(ndv - dv - 1); 

        //ensure the setup is valid
        ensureValid();  //this ensures many things are set, e.g., sourceUrl

        //try to get sourceErddapVersion
        byte versionBytes[] = new byte[]{(byte)32};
        try {
            int po = localSourceUrl.indexOf("/erddap/");
            String vUrl = localSourceUrl.substring(0, po + 8) + "version";
            versionBytes = quickRestartAttributes == null?
                SSR.getUrlResponseBytes(vUrl) : //has timeout and descriptive error 
                ((ByteArray)quickRestartAttributes.get("versionBytes")).toArray();
            String response[] = String2.split(new String(versionBytes, "UTF-8"), '\n');
            if (response[0].startsWith("ERDDAP_version=")) {
                sourceErddapVersion = String2.parseDouble(response[0].substring(15));
                if (Double.isNaN(sourceErddapVersion))
                    sourceErddapVersion = 1.22;
            }
        } catch (Throwable t) {
            //leave as default: 1.22
        }

        //try to subscribe to the remote dataset
        //It's ok that this is done every time. 
        //  emailIfAlreadyValid=false so there won't be excess email confirmations 
        //  and if flagKeyKey changes, the new tFlagUrl will be sent.
        //There is analogous code in EDDTableFromErddap.
        try {
            if (String2.isSomething(EDStatic.emailSubscriptionsFrom)) {
                int gpo = tLocalSourceUrl.indexOf("/griddap/");
                String subscriptionUrl = tLocalSourceUrl.substring(0, gpo + 1) + Subscriptions.ADD_HTML + "?" +
                    "datasetID=" + File2.getNameNoExtension(tLocalSourceUrl) + 
                    "&email=" + EDStatic.emailSubscriptionsFrom +
                    "&emailIfAlreadyValid=false" + 
                    "&action=" + SSR.minimalPercentEncode(flagUrl(datasetID)); // %encode deals with & within flagUrl
                //String2.log("subscriptionUrl=" + subscriptionUrl); //don't normally display; flags are ~confidential
                SSR.touchUrl(subscriptionUrl, 60000);  
            } else {
                String2.log(
                    String2.ERROR + ": Subscribing to the remote ERDDAP dataset failed because " +
                    "emailEverythingTo wasn't specified in setup.xml.");
            }
        } catch (Throwable st) {
            String2.log(
                "\n" + String2.ERROR + ": an exception occurred while trying to subscribe to the remote ERDDAP dataset.\n" + 
                "If the subscription hasn't been set up already, you may need to\n" + 
                "use a small reloadEveryNMinutes, or have the remote ERDDAP admin add onChange.\n\n" 
                //+ MustBe.throwableToString(st) //don't display; flags are ~confidential
                );
        }

        //save quickRestart info
        if (quickRestartAttributes == null) { //i.e., there is new info
            try {
                quickRestartAttributes = new Attributes();
                quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
                quickRestartAttributes.set("sourceInfoBytes", new ByteArray(sourceInfoBytes));
                quickRestartAttributes.set("versionBytes",    new ByteArray(versionBytes));
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
            "\n*** EDDGridFromErddap " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
    }

    /**
     * This does a quick, incremental update of this dataset (i.e., deal with 
     * leftmost axis growing). This is COPIED UNCHANGED from EDDGridFromDap.
     *
     * <p>Concurrency issue #1: This avoids 2+ simultaneous updates.
     *
     * <p>Concurrency issue #2: The changes are first prepared and 
     * then applied quickly (but not atomically!).
     * There is a chance that another thread will get inconsistent information
     * (from some things updated and some things not yet updated).
     * But I don't want to synchronize all activities of this class.
     *
     * <p>See &lt;updateEveryNMillis&gt; in constructor. 
     * Note: It is pointless and counter-productive to set updateEveryNMillis 
     * to be less than a fairly reliable update time (e.g., 1000 ms).
     *
     * <p>For simple failures, this writes to log.txt but doesn't throw an exception.
     *
     * @throws Throwable if trouble. 
     * If the dataset has changed in a serious / incompatible way and needs a full
     * reload, this throws WaitThenTryAgainException 
     * (usually, catcher calls LoadDatasets.tryToUnload(...) and EDD.requestReloadASAP(tDatasetID)).
     */
    public void update() {
        //return quickly if update system isn't active for this dataset
        if (updateEveryNMillis <= 0)
            return;

        //return quickly if dataset doesn't need to be updated
        long now = System.currentTimeMillis();
        if (now - lastUpdate < updateEveryNMillis) {
            if (reallyVerbose) String2.log("update(" + datasetID + "): no need to update:  now-last=" + 
                (now - lastUpdate) + " < updateEvery=" + updateEveryNMillis);
            return;
        }

        //if another thread is currently updating this dataset, wait for it then return
        String msg = "update(" + datasetID + "): ";
        if (!updateLock.tryLock()) {
            updateLock.lock();   //block until other thread's update finishes
            updateLock.unlock(); //immediately unlock and return
            if (verbose) String2.log(msg + "waited " + (System.currentTimeMillis() - now) +
                "ms for other thread to do the update.");
            return; 
        }

        //updateLock is locked by this thread.   Do the update!
        try {
                
            //read dds
            DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
            byte ddsBytes[] = SSR.getUrlResponseBytes(localSourceUrl + ".dds");
            DDS dds = new DDS();
            dds.parse(new ByteArrayInputStream(ddsBytes));

            //has edvga[0] changed size?
            EDVGridAxis edvga = axisVariables[0];
            EDVTimeStampGridAxis edvtsga = edvga instanceof EDVTimeStampGridAxis? 
                (EDVTimeStampGridAxis)edvga : null;
            PrimitiveArray oldValues = edvga.sourceValues();
            int oldSize = oldValues.size();

            //get mainDArray
            BaseType bt = dds.getVariable(dataVariables[0].sourceName()); //throws NoSuchVariableException
            DArray mainDArray = null;
            if (bt instanceof DGrid) {
                mainDArray = (DArray)((DGrid)bt).getVar(0); //first element is always main array
            } else if (bt instanceof DArray) {
                mainDArray = (DArray)bt;
            } else { 
                String2.log(msg + String2.ERROR + ": Unexpected " + dataVariables[0].destinationName() + 
                    " source type=" + bt.getTypeName() + ".");
                return;
                //requestReloadASAP()+WaitThenTryAgain might lead to endless cycle of full reloads
            }

            //get the leftmost dimension
            DArrayDimension dad = mainDArray.getDimension(0);
            int newSize = dad.getSize();  
            if (newSize < oldSize) 
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + msg + "[" + edvga.destinationName() + "] newSize=" + newSize + 
                    " < oldSize=" + oldSize + ")"); 
            if (newSize == oldSize) {
                if (reallyVerbose) String2.log(msg + "no change to leftmost dimension");
                return;  //finally{} below sets lastUpdate = now
            }

            //newSize > oldSize, get last old value (for testing below) and new values
            PrimitiveArray newValues = null;
            if (edvga.sourceDataTypeClass() == int.class &&                  //not a perfect test
                "count".equals(edvga.sourceAttributes().getString("units"))) 
                newValues = new IntArray(oldSize - 1, newSize - 1);  //0 based
            else {
                try {
                    newValues = OpendapHelper.getPrimitiveArray(dConnect, 
                        "?" + edvga.sourceName() + "[" + (oldSize - 1) + ":" + (newSize - 1) + "]");
                } catch (NoSuchVariableException nsve) {
                    //hopefully avoided by testing for units=count and int datatype above
                    String2.log(msg + "caught NoSuchVariableException for sourceName=" + edvga.sourceName() + 
                        ". Using index numbers.");
                    newValues = new IntArray(oldSize - 1, newSize - 1);  //0 based
                } //but other exceptions aren't caught
            }

            //ensure newValues is valid
            if (newValues == null || newValues.size() < (newSize - oldSize + 1)) {
                String2.log(msg + String2.ERROR + ": Too few " + edvga.destinationName() + 
                    " values were received (got=" + (newValues == null? "null" : "" + (newValues.size() - 1)) +
                    "expected=" + (newSize - oldSize) + ").");
                return;
            }
            if (oldValues.elementClass() != newValues.elementClass())  //they're canonical, so != works
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + msg + edvga.destinationName() + " dataType changed: " +
                       " new=" + newValues.elementClassString() +
                    " != old=" + oldValues.elementClassString() + ")"); 

            //ensure last old value is unchanged 
            if (oldValues.getDouble(oldSize - 1) != newValues.getDouble(0))  //they should be exactly equal
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + msg + edvga.destinationName() + "[" + (oldSize - 1) + 
                      "] changed!  old=" + oldValues.getDouble(oldSize - 1) + 
                              " != new=" + newValues.getDouble(0)); 

            //prepare changes to update the dataset
            double newMin = oldValues.getDouble(0);
            double newMax = newValues.getDouble(newValues.size() - 1);
            if (edvtsga != null) {
                newMin = edvtsga.sourceTimeToEpochSeconds(newMin);
                newMax = edvtsga.sourceTimeToEpochSeconds(newMax);
            } else if (edvga.scaleAddOffset()) {
                newMin = newMin * edvga.scaleFactor() + edvga.addOffset();
                newMax = newMax * edvga.scaleFactor() + edvga.addOffset();
            }

            //first, calculate newAverageSpacing (destination units, will be negative if isDescending)
            double newAverageSpacing = (newMax - newMin) / (newSize - 1);

            //second, test for min>max after extractScaleAddOffset, since order may have changed
            if (newMin > newMax) { 
                double d = newMin; newMin = newMax; newMax = d;
            }

            //test isAscending  (having last old value is essential)
            String error = edvga.isAscending()? newValues.isAscending() : newValues.isDescending();
            if (error.length() > 0) 
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + edvga.destinationName() + " was " + 
                    (edvga.isAscending()? "a" : "de") +
                    "scending, but the newest values aren't (" + error + ").)"); 

            //if was isEvenlySpaced, test that new values are and have same averageSpacing
            //(having last old value is essential)
            boolean newIsEvenlySpaced = edvga.isEvenlySpaced();  //here, this is actually oldIsEvenlySpaced
            if (newIsEvenlySpaced) {  
                error = newValues.isEvenlySpaced();
                if (error.length() > 0) {
                    String2.log(msg + "changing " + edvga.destinationName() + 
                        ".isEvenlySpaced from true to false: " + error);
                    newIsEvenlySpaced = false;

                //new spacing != old spacing ?  (precision=5, but times will be exact) 
                } else if (!Math2.almostEqual(5, newAverageSpacing, edvga.averageSpacing())) {  
                    String2.log(msg + "changing " + edvga.destinationName() + 
                        ".isEvenlySpaced from true to false: newSpacing=" + newAverageSpacing +
                        " oldSpacing=" + edvga.averageSpacing());
                    newIsEvenlySpaced = false;
                }
            }

            //remove the last old value from newValues
            newValues.remove(0); 

            //ensureCapacity of oldValues (may take time)
            oldValues.ensureCapacity(newSize); //so oldValues.append below is as fast as possible

            //right before making changes, make doubly sure another thread hasn't already (IMPERFECT TEST)
            if (oldValues.size() != oldSize) {
                String2.log(msg + "changes abandoned.  " + 
                    edvga.destinationName() + ".size changed (new=" + oldValues.size() +
                    " != old=" + oldSize + ").  (By update() in another thread?)");
                return;
            }

            //Swap changes into place quickly to minimize problems.  Better if changes were atomic.
            //Order of changes is important.
            //Other threads may be affected by some values being updated before others.
            //This is an imperfect alternative to synchronizing all uses of this dataset (which is far worse).
            oldValues.append(newValues);  //should be fast, and new size set at end to minimize concurrency problems
            edvga.setDestinationMin(newMin);
            edvga.setDestinationMax(newMax);
            edvga.setIsEvenlySpaced(newIsEvenlySpaced);
            edvga.initializeAverageSpacingAndCoarseMinMax();  
            edvga.setActualRangeFromDestinationMinMax();
            if (edvga instanceof EDVTimeGridAxis) 
                combinedGlobalAttributes.set("time_coverage_end",   
                    Calendar2.epochSecondsToLimitedIsoStringT(
                        edvga.combinedAttributes().getString(EDV.TIME_PRECISION), newMax, ""));
            edvga.clearSliderCsvValues();  //do last, to force recreation next time needed

            updateCount++;
            long thisTime = System.currentTimeMillis() - now;
            cumulativeUpdateTime += thisTime;
            if (reallyVerbose)
                String2.log(msg + "succeeded.  nValuesAdded=" + newValues.size() + 
                    " time=" + thisTime + " updateCount=" + updateCount +
                    " avgTime=" + (cumulativeUpdateTime / updateCount));
        } catch (Throwable t) {
            String2.log(msg + "failed.  Unexpected " + String2.ERROR + ":\n" +
                MustBe.throwableToString(t));
        } finally {  
            lastUpdate = now;     //say dataset is now up-to-date (or at least tried)
            updateLock.unlock();  //then ensure updateLock is always unlocked
        }
    }

    /** This returns the source ERDDAP's version number, e.g., 1.22 */
    public double sourceErddapVersion() {return sourceErddapVersion;}

    /**
     * This returns the local version of the source ERDDAP's url.
     */
    public String getLocalSourceErddapUrl() {
        return localSourceUrl;
    }

    /**
     * This returns the public version of the source ERDDAP's url.
     */
    public String getPublicSourceErddapUrl() {
        return publicSourceErddapUrl;
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
        if (verbose) String2.log("EDDGridFromErddap.sibling " + tLocalSourceUrl);

        int nAv = axisVariables.length;
        int nDv = dataVariables.length;

        //need a unique datasetID for sibling 
        //  so cached .das .dds axis values are stored separately.
        //So make tDatasetID by putting md5Hex12 in the middle of original datasetID
        //  so beginning and ending for tDatasetID are same as original.
        int po = datasetID.length() / 2; 
        String tDatasetID = datasetID.substring(0, po) +
            "_" + String2.md5Hex12(tLocalSourceUrl) + "_" +
            datasetID.substring(po);

        //make the sibling
        EDDGridFromErddap newEDDGrid = new EDDGridFromErddap(
            tDatasetID, 
            String2.toSSVString(accessibleTo),
            shareInfo? onChange : (StringArray)onChange.clone(), 
            "", "", "", "", //fgdc, iso19115, defaultDataQuery, defaultGraphQuery,
            getReloadEveryNMinutes(), getUpdateEveryNMillis(), tLocalSourceUrl);

        //if shareInfo, point to same internal data
        if (shareInfo) {

            //ensure similar
            boolean testAV0 = false;
            String results = similar(newEDDGrid, ensureAxisValuesAreEqual, false); 
            if (results.length() > 0)
                throw new RuntimeException("Error in EDDGrid.sibling: " + results);

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
     * This gets data (not yet standardized) from the data 
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
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        //build String form of the constraint
        //String errorInMethod = "Error in EDDGridFromErddap.getSourceData for " + datasetID + ": "; 
        String constraint = buildDapArrayQuery(tConstraints);

        //get results one var at a time (that's how OpendapHelper is set up)
        DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
        PrimitiveArray results[] = new PrimitiveArray[axisVariables.length + tDataVariables.length];
        for (int dv = 0; dv < tDataVariables.length; dv++) {
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

                //request should be valid, so any other error is trouble with dataset
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + EDStatic.errorFromDataSource + t.toString() + ")", 
                    t); 
            }
            if (pa.length != axisVariables.length + 1) 
                throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(Details: An unexpected data structure was returned from the source.)");
            results[axisVariables.length + dv] = pa[0];
            if (dv == 0) {
                //I think GridDataAccessor compares observed and expected axis values
                for (int av = 0; av < axisVariables.length; av++) {
                    results[av] = pa[av + 1];
                }
            } else {
                for (int av = 0; av < axisVariables.length; av++) {
                    String tError = results[av].almostEqual(pa[av + 1]); 
                    if (tError.length() > 0) 
                        throw new WaitThenTryAgainException(
                            EDStatic.waitThenTryAgain +
                            "\n(Details: The axis values for dataVariable=0,axis=" + av +  
                            "\ndon't equal the axis values for dataVariable=" + dv + ",axis=" + av + ".\n" +
                            tError + ")");
                }
            }
        }
        return results;
    }

    /** 
     * This generates datasets.xml entries for all EDDGrid from a remote ERDDAP.
     * The XML can then be edited by hand (if desired) and added to the datasets.xml file.
     *
     * @param url the base url for the dataset, e.g., 
     *   "http://coastwatch.pfeg.noaa.gov/erddap"
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String url) 
        throws Throwable {

        String2.log("EDDGridFromErddap.generateDatasetsXml\n  url=" + url);

        //make the StringBuilder to hold the results and add documentation
        StringBuilder sb = new StringBuilder();
        sb.append(  //there is very similar text in EDDGridFromErddap
"<!-- Directions:\n" +
" * The ready-to-use XML below includes information for all of the EDDGrid datasets\n" +
"   at the remote ERDDAP " + XML.encodeAsXML(url) + "\n" +
"   (except for etopo180 and etopo360, which are built into every ERDDAP).\n" +
" * If you want to add all of these datasets to your ERDDAP, just paste the XML\n" +
"   into your datasets.xml file.\n" +
" * The datasetIDs listed below are not the same as the remote datasets' datasetIDs.\n" +
"   They are generated automatically from the sourceURLs in a way that ensures that they are unique.\n" +
" * !!!reloadEveryNMinutes is left as the default 10080=oncePerWeek on the assumption\n" +
"   that the remote ERDDAP will accept your ERDDAP's request to subscribe to the dataset.\n" +
"   If you don't get emails from the remote ERDDAP asking you to validate your subscription\n" +
"   requests (perhaps because the remote ERDDAP has the subscription system turned off),\n" +
"   send an email to the admin asking that s/he add onChange tags to the datasets.\n" +
"   See the EDDGridFromErddap documentation.\n" + 
" * The XML needed for EDDGridFromErddap in datasets.xml has few options.  See\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#EDDGridFromErddap .\n" +
"   If you want to alter a dataset's metadata or make other changes to a dataset,\n" +
"   use EDDGridFromDap to access the dataset instead of EDDGridFromErddap.\n" +
"-->\n");

        //get the griddap datasets in a json table
        String jsonUrl = url + "/griddap/index.json";
        String sourceInfo = SSR.getUrlResponseString(jsonUrl);
        if (reallyVerbose) String2.log(sourceInfo.substring(0, Math.min(sourceInfo.length(), 2000)));
        if (sourceInfo.indexOf("\"table\"") > 0) {
            Table table = new Table();
            table.readJson(jsonUrl, sourceInfo);   //they are sorted by title

            PrimitiveArray urlCol = table.findColumn("griddap");
            PrimitiveArray titleCol = table.findColumn("Title");
            PrimitiveArray datasetIdCol = table.findColumn("Dataset ID");

            //go through the rows of the table
            int nRows = table.nRows();
            for (int row = 0; row < nRows; row++) {
                String id = datasetIdCol.getString(row);
                if (id.equals("etopo180") || id.equals("etopo360"))
                    continue;
                //localSourceUrl isn't available (and we generally don't want it)
                String tPublicSourceUrl = urlCol.getString(row); 
                //Use unchanged tPublicSourceUrl or via suggestDatasetID?
                //I guess suggestDatasetID because it ensures a unique name for use in local ERDDAP.
                //?? Does it cause trouble to use a different datasetID here?
                String newID = suggestDatasetID(tPublicSourceUrl);
                sb.append(
"<dataset type=\"EDDGridFromErddap\" datasetID=\"" + newID + "\" active=\"true\">\n" +
"    <!-- " + XML.encodeAsXML(String2.replaceAll(titleCol.getString(row), "--", "- - ")) + " -->\n" +
"    <sourceUrl>" + XML.encodeAsXML(tPublicSourceUrl) + "</sourceUrl>\n" +
"</dataset>\n");
            }
        }

        //get the EDDGridFromErddap datasets 
        jsonUrl = url + "/search/index.json?searchFor=EDDGridFromErddap";
        sourceInfo = "";
        try {
            sourceInfo = SSR.getUrlResponseString(jsonUrl);
        } catch (Throwable t) {
            //error if remote erddap has no EDDGridFromErddap's
        }
        if (reallyVerbose) String2.log(sourceInfo.substring(0, Math.min(sourceInfo.length(), 2000)));
        PrimitiveArray datasetIdCol;
        if (sourceInfo.indexOf("\"table\"") > 0) {
            Table table = new Table();
            table.readJson(jsonUrl, sourceInfo);   //they are sorted by title
            datasetIdCol = table.findColumn("Dataset ID");
        } else {
            datasetIdCol = new StringArray();
        }

        sb.append(
            "\n<!-- Of the datasets above, the following datasets are EDDGridFromErddap's at the remote ERDDAP.\n" +
            "It would be best if you contacted the remote ERDDAP's administrator and requested the dataset XML\n" +
            "that is being using for these datasets so your ERDDAP can access the original ERDDAP source.\n" +
            "The remote EDDGridFromErddap datasets are:\n");
        if (datasetIdCol.size() == 0)
            sb.append("(none)");
        else sb.append(String2.noLongLinesAtSpace(datasetIdCol.toString(), 80, ""));
        sb.append("\n-->\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        //test local generateDatasetsXml
        try {
            String results = generateDatasetsXml(EDStatic.erddapUrl) + "\n"; //in tests, always non-https url
            String2.log("results=\n" + results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDGridFromErddap",
                EDStatic.erddapUrl},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<!-- Directions:\n" +
" * The ready-to-use XML below includes information for all of the EDDGrid datasets\n";

            Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
                expected, "results=\n" + results);

expected = 
"<dataset type=\"EDDGridFromErddap\" datasetID=\"0_0_3648_e4d8_5e3c\" active=\"true\">\n" +
"    <!-- SST, Blended, Global, EXPERIMENTAL (5 Day Composite) -->\n" +
"    <sourceUrl>http://127.0.0.1:8080/cwexperimental/griddap/erdBAssta5day</sourceUrl>\n" +
"</dataset>";

            int po = results.indexOf(expected.substring(0, 80));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

expected = 
"<!-- Of the datasets above, the following datasets are EDDGridFromErddap's at the remote ERDDAP.\n";
            po = results.indexOf(expected.substring(0, 20));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);            
            Test.ensureTrue(results.indexOf("rMHchla8day", po) > 0, 
                "results=\n" + results + 
                "\nTHIS TEST REQUIRES rMHchla8day TO BE ACTIVE ON THE localhost ERDDAP.");

            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);    
            //!!!the first dataset will vary, depending on which are currently active!!!
            Test.ensureEqual(edd.title(), "Chlorophyll-a, Aqua MODIS, NPP, DEPRECATED OLDER VERSION (8 Day Composite)", "");
            Test.ensureEqual(edd.datasetID(), "0_0_1eed_1be2_4073", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "chlorophyll", "");


        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml on " + EDStatic.erddapUrl + //in tests, always non-https url
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /** This does some basic tests. */
    public static void testBasic(boolean testLocalErddapToo) throws Throwable {
        testVerboseOn();
        EDDGridFromErddap gridDataset;
        String name, tName, axisDapQuery, query, results, expected, expected2, error;
        int tPo;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String userDapQuery  = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
        String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]"; 
        String mapDapQuery   = "chlorophyll[200][][(29):(50)][(225):(247)]"; //stride irrelevant 
        StringArray destinationNames = new StringArray();
        IntArray constraints = new IntArray();
        String localUrl = EDStatic.erddapUrl + "/griddap/rMHchla8day"; //in tests, always non-https url

        try {

            gridDataset = (EDDGridFromErddap)oneFromDatasetXml("rMHchla8day"); 

            //*** test getting das for entire dataset
            String2.log("\n****************** EDDGridFromErddap test entire dataset\n");
            tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Entire", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Attributes \\{\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0260864e\\+9, .{8,14};\n" + //changes
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
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
"  latitude \\{\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -90.0, 90.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  \\}\n" +
"  longitude \\{\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 360.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  \\}\n" +
"  chlorophyll \\{\n" +
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
"  \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"NASA GSFC \\(OBPG\\)\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" + 
"    String creator_email \"dave.foley@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_url \"http://coastwatch.pfel.noaa.gov\";\n" +
"    String date_created \"20.{8}Z\";\n" +  //changes
"    String date_issued \"20.{8}Z\";\n" +  //changes
"    Float64 Easternmost_Easting 360.0;\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -90.0;\n" +
"    Float64 geospatial_lat_resolution 0.041676313961565174;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 360.0;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.04167148975575877;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NASA GSFC \\(OBPG\\)";
//"2010-02-05T02:14:47Z NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD\n" + //changes sometimes
//today + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\n";
//today + " http://127.0.0.1:8080/cwexperimental/griddap/rMHchla8day.das\";\n" +   //This is it working locally.
//or           coastwatch ...      //what I expected/wanted.  This really appears as if remote dataset.

expected2 =
"    String infoUrl \"http://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" + 
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String origin \"NASA GSFC \\(OBPG\\)\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch \\(http://coastwatch.noaa.gov/\\)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String references \"Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n" +
"    String satellite \"Aqua\";\n" +
"    String sensor \"MODIS\";\n" +
"    String source \"satellite observation: Aqua, MODIS\";\n" +
"    String sourceUrl \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
"    Float64 Southernmost_Northing -90.0;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer \\(MODIS\\) carried aboard the spacecraft.   This is Science Quality data.\";\n" +
"    String time_coverage_end \"20.{8}T00:00:00Z\";\n" + //changes
"    String time_coverage_start \"2002-07-08T00:00:00Z\";\n" +
"    String title \"Chlorophyll-a, Aqua MODIS, NPP, DEPRECATED OLDER VERSION \\(8 Day Composite\\)\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
            try {
                tPo = results.indexOf("history \"NASA GSFC (OBPG)");
                Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
                Test.ensureLinesMatch(results.substring(0, tPo + 25), expected, 
                    "\nresults=\n" + results);

                tPo = results.indexOf("    String infoUrl ");
                Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
                Test.ensureLinesMatch(results.substring(tPo), expected2, "\nresults=\n" + results);
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                     "Press ^C to stop or Enter to continue..."); 
            }

            try {
                if (testLocalErddapToo) {
                    results = SSR.getUrlResponseString(localUrl + ".das");
                    tPo = results.indexOf("history \"NASA GSFC (OBPG)");
                    Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
                    Test.ensureLinesMatch(results.substring(0, tPo + 25), expected, 
                        "\nresults=\n" + results);

                    tPo = results.indexOf("    String infoUrl ");
                    Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
                    Test.ensureLinesMatch(results.substring(tPo), expected2, 
                        "\nresults=\n" + results);
                }
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                     "Press ^C to stop or Enter to continue..."); 
            }

            //*** test getting dds for entire dataset
            tName = gridDataset.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Entire", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset \\{\n" +
    "  Float64 time\\[time = \\d{3}\\];\n" +   //was 500, changes sometimes   (and a few places below)
    "  Float64 altitude\\[altitude = 1\\];\n" +
    "  Float64 latitude\\[latitude = 4320\\];\n" +
    "  Float64 longitude\\[longitude = 8640\\];\n" +
    "  GRID \\{\n" +
    "    ARRAY:\n" +
    "      Float32 chlorophyll\\[time = \\d{3}\\]\\[altitude = 1\\]\\[latitude = 4320\\]\\[longitude = 8640\\];\n" +
    "    MAPS:\n" +
    "      Float64 time\\[time = \\d{3}\\];\n" +
    "      Float64 altitude\\[altitude = 1\\];\n" +
    "      Float64 latitude\\[latitude = 4320\\];\n" +
    "      Float64 longitude\\[longitude = 8640\\];\n" +
    "  \\} chlorophyll;\n" +
    "\\} "; //rMHchla8day;\n";
            Test.ensureLinesMatch(results, expected + "rMHchla8day;\n", "\nresults=\n" + results);

            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".dds");
                Test.ensureLinesMatch(results, expected + "erdMHchla8day;\n", "\nresults=\n" + results);
            }

            //********************************************** test getting axis data

            //.asc
            String2.log("\n*** EDDGridFromErddap test get .ASC axis data\n");
            query = "time[0:100:200],longitude[last]";
            tName = gridDataset.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Axis", ".asc"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Float64 time[time = 3];\n" +
    "  Float64 longitude[longitude = 1];\n" +
    "} "; //r
            expected2 = "MHchla8day;\n" +
    "---------------------------------------------\n" +
    "Data:\n" +
    "time[3]\n" +
    "1.0260864E9, 1.0960704E9, 1.1661408E9\n" +
    "longitude[1]\n" +
    "360.0\n";
            Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);
     
            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".asc?" + query);
                Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
            }

            //.csv
            String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
            query = "time[0:100:200],longitude[last]";
            tName = gridDataset.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
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

            if (testLocalErddapToo) {
                try {
                    results = SSR.getUrlResponseString(localUrl + ".csv?" + query);
                    Test.ensureEqual(results, expected, "\nresults=\n" + results);
                } catch (Throwable t) {
                    String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                        "\nUnexpected error.\nPress ^C to stop or Enter to continue...");
                }
            }

            //.csv  test of gridName.axisName notation
            String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
            query = "chlorophyll.time[0:100:200],chlorophyll.longitude[last]";
            tName = gridDataset.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
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

            if (testLocalErddapToo) {
                try {
                    results = SSR.getUrlResponseString(localUrl + ".csv?" + query);
                    Test.ensureEqual(results, expected, "\nresults=\n" + results);
                } catch (Throwable t) {
                    String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                        "\nUnexpected error.\nPress ^C to stop or Enter to continue...");
                }
            }

            //.dods
            String2.log("\n*** EDDGridFromErddap test get .DODS axis data\n");
            query = "time[0:100:200],longitude[last]";
            tName = gridDataset.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Axis", ".dods"); 
            results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
            //String2.log(results);
            expected = 
    "Dataset {[10]\n" +
    "  Float64 time[time = 3];[10]\n" +
    "  Float64 longitude[longitude = 1];[10]\n" +
    "} ";  //r
            expected2 = "MHchla8day;[10]\n" +
    "[10]\n" +
    "Data:[10]\n" +
    "[0][0][0][3][0][0][0][3]A[206][8221]k[0][0][0][0]A[208]U-@[0][0][0]A[209]`y`[0][0][0][0][0][0][1][0][0][0][1]@v[8364][0][0][0][0][0][end]";
            Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);

            if (testLocalErddapToo) {
                results = String2.annotatedString(SSR.getUrlResponseString(localUrl + ".dods?" + query));
                Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
            }

            //.mat
            //octave> load('c:/temp/griddap/EDDGridFromErddap_Axis.mat');
            //octave> erdMHchla8day
            String matlabAxisQuery = "time[0:100:200],longitude[last]"; 
            String2.log("\n*** EDDGridFromErddap test get .MAT axis data\n");
            tName = gridDataset.makeNewFileForDapQuery(null, null, matlabAxisQuery, EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Axis", ".mat"); 
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
    "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0b                    |\n" +
    "72 4d 48 63 68 6c 61 38   64 61 79 00 00 00 00 00   rMHchla8day      |\n" +
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

            //.ncHeader
            String2.log("\n*** EDDGridFromErddap test get .NCHEADER axis data\n");
            query = "time[0:100:200],longitude[last]";
            tName = gridDataset.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Axis", ".ncHeader"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
//"netcdf EDDGridFromErddap_Axis.nc {\n" +
//"  dimensions:\n" +
//"    time = 3;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23   
//"    longitude = 1;\n" +   // (has coord.var)\n" +   //but won't change for testLocalErddapToo until next release
"  variables:\n" +
"    double time(time=3);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 1.0260864E9, 1.1661408E9; // double\n" +  //up-to-date
"      :axis = \"T\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Centered Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double longitude(longitude=1);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 360.0, 360.0; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 4; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n";
        tPo = results.indexOf("  variables:\n");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "RESULTS=\n" + results);
        expected2 = 
"  :title = \"Chlorophyll-a, Aqua MODIS, NPP, DEPRECATED OLDER VERSION (8 Day Composite)\";\n" +
"  :Westernmost_Easting = 360.0; // double\n" +
" data:\n" +
"}\n";
            Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

            try {
                if (testLocalErddapToo) {
                    results = SSR.getUrlResponseString(localUrl + ".ncHeader?" + query);
                    tPo = results.indexOf("  variables:\n");
                    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
                    Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, 
                        "\nresults=\n" + results);
                    Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
                }
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\n*** THIS WILL FAIL UNTIL AFTER RELEASE 1.48." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }

            //********************************************** test getting grid data
            //.csv
            String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
            tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Data", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =  //missing values are "NaN"
/* pre 2010-10-26 was 
"time, altitude, latitude, longitude, chlorophyll\n" +
"UTC, m, degrees_north, degrees_east, mg m-3\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n"; */
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
//pre 2012-08-17 was
//"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
//"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n";
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n";
            Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
            expected2 = 
//pre 2010-10-26 was  
//"2007-02-06T00:00:00Z, 0.0, 49.407270201435495, 232.06852644982058, 0.37\n";
//pre 2012-08-17 was
//"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
            Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

            if (testLocalErddapToo) {
                try {
                    results = SSR.getUrlResponseString(localUrl + ".csv?" + userDapQuery);
                    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
                    Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
                } catch (Throwable t) {
                    String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                        "\nUnexpected error.\nPress ^C to stop or Enter to continue...");
                }
            }

            //.csv   test gridName.gridName notation
            String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
            tName = gridDataset.makeNewFileForDapQuery(null, null, "chlorophyll." + userDapQuery, 
                EDStatic.fullTestCacheDirectory, gridDataset.className() + "_DotNotation", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
/* pre 2010-10-26 was 
"time, altitude, latitude, longitude, chlorophyll\n" +
"UTC, m, degrees_north, degrees_east, mg m-3\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
"2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n"; */
"time,altitude,latitude,longitude,chlorophyll\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
//pre 2012-08-17 was
//"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
//"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n";
"2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
"2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n";
            Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
            expected2 = 
//pre 2010-10-26 was  
//"2007-02-06T00:00:00Z, 0.0, 49.407270201435495, 232.06852644982058, 0.37\n";
//pre 2012-08-17 was
//"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
"2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
            Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

            if (testLocalErddapToo) {
                try {
                    results = SSR.getUrlResponseString(localUrl + ".csv" + "?chlorophyll." + userDapQuery);
                    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
                    Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
                } catch (Throwable t) {
                    String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                        "\nUnexpected error.\nPress ^C to stop or Enter to continue...");
                }
            }

            //.nc
            String2.log("\n*** EDDGridFromErddap test get .NC data\n");
            tName = gridDataset.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                gridDataset.className() + "_Data", ".nc"); 
            results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory  + tName, true);
            expected = 
"netcdf EDDGridFromErddap_Data.nc \\{\n" +
"  dimensions:\n" +
"    time = 1;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23
"    altitude = 1;\n" +   // (has coord.var)\n" +
"    latitude = 51;\n" +   // (has coord.var)\n" +
"    longitude = 53;\n" +   // (has coord.var)\n" +
"  variables:\n" +
"    double time\\(time=1\\);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 1.17072E9, 1.17072E9; // double\n" +
"      :axis = \"T\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Centered Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double altitude\\(altitude=1\\);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"up\";\n" +
"      :actual_range = 0.0, 0.0; // double\n" +
"      :axis = \"Z\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Altitude\";\n" +
"      :positive = \"up\";\n" +
"      :standard_name = \"altitude\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double latitude\\(latitude=51\\);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 28.985876360268577, 49.82403334105115; // double\n" +
"      :axis = \"Y\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 4; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double longitude\\(longitude=53\\);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 224.98437319134158, 246.65354786433613; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 4; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float chlorophyll\\(time=1, altitude=1, latitude=51, longitude=53\\);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 30.0; // double\n" +
"      :colorBarMinimum = 0.03; // double\n" +
"      :colorBarScale = \"Log\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Ocean Color\";\n" +
"      :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" +
"      :units = \"mg m-3\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :composite = \"true\";\n" +
"  :contributor_name = \"NASA GSFC \\(OBPG\\)\";\n" +
"  :contributor_role = \"Source of level 2 data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" + 
"  :creator_email = \"dave.foley@noaa.gov\";\n" +
"  :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
"  :creator_url = \"http://coastwatch.pfel.noaa.gov\";\n" +
"  :date_created = \"20.{8}Z\";\n" + //changes periodically
"  :date_issued = \"20.{8}Z\";\n" +  //changes periodically
"  :Easternmost_Easting = 246.65354786433613; // double\n" +
"  :geospatial_lat_max = 49.82403334105115; // double\n" +
"  :geospatial_lat_min = 28.985876360268577; // double\n" +
"  :geospatial_lat_resolution = 0.041676313961565174; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 246.65354786433613; // double\n" +
"  :geospatial_lon_min = 224.98437319134158; // double\n" +
"  :geospatial_lon_resolution = 0.04167148975575877; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 0.0; // double\n" +
"  :geospatial_vertical_min = 0.0; // double\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"NASA GSFC \\(OBPG\\)";
        tPo = results.indexOf(":history = \"NASA GSFC (OBPG)");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 28), expected, "RESULTS=\n" + results);

        expected = //note original missing values
"  :infoUrl = \"http://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n" +
"  :institution = \"NOAA CoastWatch, West Coast Node\";\n" +
"  :keywords = \"8-day,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"  :naming_authority = \"gov.noaa.pfel.coastwatch\";\n" +
"  :Northernmost_Northing = 49.82403334105115; // double\n" +
"  :origin = \"NASA GSFC \\(OBPG\\)\";\n" +
"  :processing_level = \"3\";\n" +
"  :project = \"CoastWatch \\(http://coastwatch.noaa.gov/\\)\";\n" +
"  :projection = \"geographic\";\n" +
"  :projection_type = \"mapped\";\n" +
"  :references = \"Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n" +
"  :satellite = \"Aqua\";\n" +
"  :sensor = \"MODIS\";\n" +
"  :source = \"satellite observation: Aqua, MODIS\";\n" +
"  :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
"  :Southernmost_Northing = 28.985876360268577; // double\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer \\(MODIS\\) carried aboard the spacecraft.   This is Science Quality data.\";\n" +
"  :time_coverage_end = \"2007-02-06T00:00:00Z\";\n" +
"  :time_coverage_start = \"2007-02-06T00:00:00Z\";\n" +
"  :title = \"Chlorophyll-a, Aqua MODIS, NPP, DEPRECATED OLDER VERSION \\(8 Day Composite\\)\";\n" +
"  :Westernmost_Easting = 224.98437319134158; // double\n" +
" data:\n" +
"time =\n" +
"  \\{1.17072E9\\}\n" +
"altitude =\n" +
"  \\{0.0\\}\n" +
"latitude =\n" +
"  \\{28.985876360268577, 29.40263949988423, 29.81940263949987, 30.236165779115524, 30.652928918731178, 31.06969205834683, 31.486455197962485, 31.90321833757814, 32.31998147719379, 32.73674461680943, 33.153507756425086, 33.57027089604074, 33.98703403565639, 34.40379717527205, 34.8205603148877, 35.237323454503354, 35.65408659411899, 36.07084973373465, 36.4876128733503, 36.904376012965955, 37.32113915258161, 37.73790229219726, 38.1546654318129, 38.571428571428555, 38.98819171104421, 39.40495485065986, 39.821717990275516, 40.23848112989117, 40.655244269506824, 41.07200740912248, 41.48877054873813, 41.905533688353785, 42.32229682796944, 42.73905996758509, 43.155823107200746, 43.57258624681637, 43.989349386432025, 44.40611252604768, 44.82287566566333, 45.239638805278986, 45.65640194489464, 46.07316508451029, 46.48992822412595, 46.9066913637416, 47.323454503357254, 47.74021764297291, 48.15698078258856, 48.573743922204216, 48.99050706181987, 49.407270201435495, 49.82403334105115\\}\n" +
"longitude =\n" +
"  \\{224.98437319134158, 225.40108808889917, 225.81780298645677, 226.23451788401434, 226.65123278157193, 227.06794767912953, 227.4846625766871, 227.9013774742447, 228.3180923718023, 228.73480726935986, 229.15152216691746, 229.56823706447506, 229.98495196203262, 230.40166685959022, 230.81838175714782, 231.2350966547054, 231.65181155226298, 232.06852644982058, 232.48524134737815, 232.90195624493575, 233.31867114249334, 233.7353860400509, 234.1521009376085, 234.5688158351661, 234.98553073272367, 235.40224563028127, 235.81896052783887, 236.23567542539644, 236.65239032295403, 237.06910522051163, 237.48582011806923, 237.9025350156268, 238.3192499131844, 238.735964810742, 239.15267970829956, 239.56939460585716, 239.98610950341475, 240.40282440097232, 240.81953929852992, 241.23625419608751, 241.65296909364508, 242.06968399120268, 242.48639888876028, 242.90311378631785, 243.31982868387544, 243.73654358143304, 244.1532584789906, 244.5699733765482, 244.9866882741058, 245.40340317166337, 245.82011806922097, 246.23683296677856, 246.65354786433613\\}\n" +
"chlorophyll =\n" +
"  \\{\n" +
"    \\{\n" +
"      \\{\n" +
//pre 2010-10-26 was 
//"        {-9999999.0, -9999999.0, 0.099, 0.118, -9999999.0, 0.091, -9999999.0, 0.088, 0.085, 0.088, -9999999.0, 0.098, -9999999.0, 0.076, -9999999.0, 0.07, 0.071, -9999999.0, -9999999.0, -9999999.0, 0.078, -9999999.0, 0.09, 0.084, -9999999.0, -9999999.0, 0.098, -9999999.0, 0.079, 0.076, 0.085, -9999999.0, 0.086, 0.127, 0.199, 0.167, 0.191, 0.133, 0.14, 0.173, 0.204, 0.239, 0.26, 0.252, 0.274, 0.289, 0.367, 0.37, 0.65, 0.531, -9999999.0, -9999999.0, 1.141},";
//pre 2012-08-17 was
//"        {-9999999.0, -9999999.0, 0.10655, 0.12478, -9999999.0, 0.09398, -9999999.0, 0.08919, 0.09892, 0.10007, -9999999.0, 0.09986, -9999999.0, 0.07119, -9999999.0, 0.08288, 0.08163, -9999999.0, -9999999.0, -9999999.0, 0.08319, -9999999.0, 0.09706, 0.08309, -9999999.0, -9999999.0, 0.0996, -9999999.0, 0.08962, 0.08329, 0.09101, -9999999.0, 0.08679, 0.13689, 0.21315, 0.18729, 0.21642, 0.15069, 0.15123, 0.18849, 0.22975, 0.27075, 0.29062, 0.27878, 0.31141, 0.32663, 0.41135, 0.40628, 0.65426, 0.4827, -9999999.0, -9999999.0, 1.16268},";
  "        \\{-9999999.0, -9999999.0, 0.11093, 0.12439, -9999999.0, 0.09554, -9999999.0, 0.09044, 0.10009, 0.10116, -9999999.0, 0.10095, -9999999.0, 0.07243, -9999999.0, 0.08363, 0.08291, -9999999.0, -9999999.0, -9999999.0, 0.08885, -9999999.0, 0.09632, 0.0909, -9999999.0, -9999999.0, 0.09725, -9999999.0, 0.09978, 0.09462, 0.09905, -9999999.0, 0.09937, 0.12816, 0.20255, 0.17595, 0.20562, 0.14333, 0.15073, 0.18803, 0.22673, 0.27252, 0.29005, 0.28787, 0.31865, 0.33447, 0.43293, 0.43297, 0.68101, 0.48409, -9999999.0, -9999999.0, 1.20716\\},"; //no \n
            tPo = results.indexOf("  :infoUrl");
            int tPo2 = results.indexOf("1.20716},", tPo + 1);
            if (tPo < 0 || tPo2 < 0)
                String2.log("tPo=" + tPo + " tPo2=" + tPo2 + " results=\n" + results);

            Test.ensureLinesMatch(results.substring(tPo, tPo2 + 9), expected, "RESULTS=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\n*** This EDDGridFromErddap test requires erdMHchla8day on coastwatch's erddap" +
                (testLocalErddapToo? "\n    AND rMHchla8day on localhost's erddap." : "") +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    public static void testDataVarOrder() throws Throwable {
        String2.log("\n*** EDDGridFromErddap.testDataVarOrder()");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testDataVarOrder"); 
        String results = String2.toCSSVString(eddGrid.dataVariableDestinationNames());
        String expected = "SST, mask, analysis_error";
        Test.ensureEqual(results, expected, "RESULTS=\n" + results);
    }


    /** This tests dealing with remote not having ioos_category, but local requiring it. */
    public static void testGridNoIoosCat() throws Throwable {
        String2.log("\n*** EDDGridFromErddap.testGridNoIoosCat");

        //not active because no test dataset
        //this failed because trajectory didn't have ioos_category
        //EDDGrid edd = (EDDGrid)oneFromDatasetXml("testGridNoIoosCat"); 

    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        String2.log("\n****************** EDDGridFromErddap.test() *****************\n");

        //standard tests 
/* */
        testBasic(false);
        testBasic(true);
        testGenerateDatasetsXml();
        testDataVarOrder();
        testGridNoIoosCat();

        //not regularly done

        String2.log("\n*** EDDGridFromErddap.test finished.");

    }



}
