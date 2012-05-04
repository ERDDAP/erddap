/* 
 * EDD Copyright 2007, NOAA.
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
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.Boundaries;
import gov.noaa.pfel.coastwatch.sgt.GSHHS;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONTokener;

/** 
This class represents an ERDDAP Dataset (EDD) -- 
a gridded or tabular dataset 
(usually Longitude, Latitude, Altitude, and Time-referenced)
usable by ERDDAP (ERD's Distributed Access Program).

<p>Currently, ERDDAP serves these datasets via a superset of the OPeNDAP protocol
(http://www.opendap.org/), which is the recommended
IOOS DMAC (http://dmac.ocean.us/index.jsp) data transport mechanism. 
(OPeNDAP is great!)
Other ways of serving the data (e.g., WCS, WFS, and SOS) may be added in the future.
ERDDAP is structured for this and there don't seem to be any impediments.

<p>Main goal: make it easier for scientists (especially those in the IOOS realm)
to access geo- and time-referenced data from diverse remote datasources via common protocols
(e.g., OPeNDAP) and get the results in common file formats.
To achieve that, ERDDAP tries to reduce data to a few common
data structures (grids and tables) so that the data can be dealt with in a simple way.
Although OPeNDAP is great, it is a low level protocol. 
ERDDAP works as an OPeNDAP server, but it is also a higher level web service built
upon and compatible with OPeNDAP.
Some features are:
<ul>
<li> A few, simple data structures:
  Since it can be difficult for clients to deal with the infinite number of 
  dataset structures offered by OPeNDAP, 
  ERDDAP currently deals with two dataset structures: 
  gridded data (e.g., satellite data and model data) and 
  tabular data (e.g., in-situ station and trajectory data).
  Certainly, not all data can be expressed in these structures, but much of it can. 
  Tables, in particular, are very flexible data structures
  (look at the success of relational database programs).
  This makes it easier to serve the data in standard file types
  (which often just support simple data structures).   
  And this makes it easier to compare data from different sources.   
  Other data structures (e.g., projected grids) could be
  supported in the future if called for.
<li> Requests can be made in user units: 
  Although requests in ERDDAP can be made with array indices (as with OPeNDAP),
  requests can also be in user units (e.g., degrees east).
<li> Results are formatted to suit the user:
  The results can be returned in any of several common data file formats: 
  (e.g., ESRI .asc, Google Earth's .kml, .nc, .mat, comma-separated ASCII,
  and tab-separated ASCII) instead of just the original format
  or just the OPeNDAP transfer format (which has no standard file manifestation).
  These files are created on-the-fly. Since there are few internal 
  data structures, it is easy to add additional file-type drivers. 
<li> Local or remote data:
  Datasets in ERDDAP can be local (on the same computer) or remote
  (accessible via the web).
<li> Additional metadata:
  Many data sources have little or no metadata describing the data.
  ERDDAP lets (and encourages) the administrator to describe metadata which 
  will be added to datasets and their variables on-the-fly.
<li> Standardized variable names and units for longitude, latitude, altitude, and time:
  To facilitate comparisons of data from different datasets,
  the requests and results in ERDDAP use standardized space/time axis units:
  longitude is always in degrees_east; latitude is always in degrees_north;
  altitude is always in meters with positive=up; 
  time is always in seconds since 1970-01-01T00:00:00Z
  and, when formatted as a string, is formatted according to the ISO 8601 standard.
  This makes it easy to specify constraints in requests
  without having to worry about the altitude data format 
  (are positive values up or down? in meters or fathoms?)
  or time data format (a nightmarish realm of possible formats and time zones).
  This makes the results from different data sources easy to compare.
<li>Modular structure:
  ERDDAP is structured so that it is easy to add different components
  (e.g., a class to request data from an OPeNDAP 2-level sequence dataset and store
  it as a table).  The new component then gains all the features 
  and capabilities of the parent (e.g., support for OPeNDAP requests 
  and the ability to save the data in several common file formats).
<li>Data Flow:
  To save memory (it is a big issue) and make responses start sooner,
  ERDDAP processes data requests in chunks -- repeatedly getting a chunk of data
  and sending that to the client.
  For many datasources (e.g., SOS sources), this means that the first chunk of 
  data (e.g., from the first sensor) gets to the client in seconds
  instead of minutes (e.g., after data from the last sensor has been retrieved).
  From a memory standpoint, this allows numerous large requests 
  (each larger than available memory) to be handled simultaneously
  and efficiently.
<li>Is ERDDAP a solution to all our data distribution problems? Not even close.
  But hopefully ERDDAP fills some common needs that
  aren't being filled by other data servers.
</ul>  

 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public abstract class EDD { 


    /** "ERROR" is defined here (from String2.ERROR) so that it is consistent in log files. */
    public final static String ERROR = String2.ERROR; 

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /**
     * Set this to true (by calling debugMode=true in your program, 
     * not but changing the code here)
     * if you want every possible diagnostic message sent to String2.log.
     */
    public static boolean debugMode = false; 


    /** The allowed cdm_data_type's. */
    public final static String CDM_GRID = "Grid", 
        CDM_MOVINGGRID = "MovingGrid",
        CDM_OTHER = "Other", //Bob added 
        CDM_POINT = "Point", 
        CDM_PROFILE = "Profile",
        CDM_RADIALSWEEP = "RadialSweep",
        //CDM_SECTION = "Section",  //removed 2010-12, now called TrajectoryProfile
        CDM_TIMESERIES = "TimeSeries", //2011-05-17 was Station
        CDM_TIMESERIESPROFILE = "TimeSeriesProfile", 
        CDM_SWATH = "Swath",
        CDM_TRAJECTORY = "Trajectory",
        CDM_TRAJECTORYPROFILE = "TrajectoryProfile"; //added 2010-12

    /** 
     * CDM_TYPES is an array in alphabetic order. 
     * Don't rely on the positions, since new types will be added in 
     * alphabetic order.) */
    public final static String[] CDM_TYPES = {
        CDM_GRID, CDM_MOVINGGRID, CDM_OTHER, CDM_POINT, CDM_PROFILE, 
        CDM_RADIALSWEEP, CDM_TIMESERIES, CDM_TIMESERIESPROFILE, CDM_SWATH, 
        CDM_TRAJECTORY, CDM_TRAJECTORYPROFILE};


    /** 
     * LEGEND constants define the options for legend placements on graphs
     * "Bottom" is the default.
     */
    public final static String
        LEGEND_BOTTOM = "Bottom",
        LEGEND_OFF = "Off",
        LEGEND_ONLY = "Only";

    public static int DEFAULT_RELOAD_EVERY_N_MINUTES = 10080; //1 week  //The value is mentioned in datasets.xml.

    /** These are used by EDDGridFromFiles and EDDTableFromFiles for files in datasetInfoDir(). */
    public final static String DIR_TABLE_FILENAME     = "dirTable.nc";
    public final static String FILE_TABLE_FILENAME    = "fileTable.nc";
    public final static String BADFILE_TABLE_FILENAME = "badFiles.nc";
    public final static String pngInfoSuffix = "_info.json";

    //*********** END OF STATIC DECLARATIONS ***************************

    protected long creationTimeMillis = System.currentTimeMillis();

    /** The constructor must set all of these protected variables 
     * (see definitions below in their accessor methods). 
     */    
    protected String datasetID, className;

    /** 0 or more actions (starting with "http://" or "mailto:")
     * to be done whenever the dataset changes significantly (or null)
     */
    protected StringArray onChange;

    /** 
      sourceAtt are straight from the source.
      addAtt are specified by the admin and supercede sourceAttributes. 
      combinedAtt are made from sourceAtt and addAtt, then revised (e.g., remove "null" values) 
    */
    protected Attributes sourceGlobalAttributes, addGlobalAttributes, combinedGlobalAttributes;
    //dataVariables isn't a hashMap because it is nice to allow a specified order for the variables
    protected EDV[] dataVariables;
    private int reloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    /** accessibleTo is stored in sorted order.  null means accessible to anyone, even if not logged in. 
     *   length=0 means accessible to no one.
     */
    protected String[] accessibleTo = null; 
    /** The localSourceUrl actually used to get data (e.g., the url which works in the DMZ, 
     * as opposed to the publicUrl. */
    protected String localSourceUrl;
    /** The publicSourceUrl which is what appears in combinedGlobalAttributes (e.g., the url
     * which users can use outside of the DMZ). */
    protected String publicSourceUrl;


    /** These are created as needed (in the constructor) from combinedGlobalAttributes. */
    protected String id, title, summary, extendedSummaryPartB, institution, 
        infoUrl, cdmDataType;
    /** These are created as needed (in the constructor) by accessibleVia...(). */
    protected String accessibleViaMAG, accessibleViaSubset, accessibleViaSOS, 
        accessibleViaWCS, accessibleViaWMS, accessibleViaNcCF; 
    protected byte[] searchString;
    /** These are created as needed (in the constructor) from dataVariables[]. */
    protected String[] dataVariableSourceNames, dataVariableDestinationNames;
    



    /**
     * This constructs an EDDXxx based on the information in an .xml file.
     * This ignores the &lt;dataset active=.... &gt; setting.
     * All of the subclasses fromXml() methods ignore the &lt;dataset active=.... &gt; setting.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDXxx&gt; 
     *    having just been read.  
     * @return a 'type' subclass of EDD.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDD fromXml(String type, SimpleXMLReader xmlReader) throws Throwable {
        String startError = "datasets.xml error on or before line #";
        if (type == null) 
            throw new SimpleException(startError + xmlReader.lineNumber() + 
                ": Unexpected <dataset> type=" + type + ".");
        try {
            //future: classes could be added at runtime if I used reflection
            if (type.equals("EDDGridAggregateExistingDimension")) 
                return EDDGridAggregateExistingDimension.fromXml(xmlReader);
            if (type.equals("EDDGridCopy"))             return EDDGridCopy.fromXml(xmlReader);
            if (type.equals("EDDGridFromDap"))          return EDDGridFromDap.fromXml(xmlReader);
            if (type.equals("EDDGridFromErddap"))       return EDDGridFromErddap.fromXml(xmlReader);
            if (type.equals("EDDGridFromEtopo"))        return EDDGridFromEtopo.fromXml(xmlReader);
            if (type.equals("EDDGridFromNcFiles"))      return EDDGridFromNcFiles.fromXml(xmlReader);
            if (type.equals("EDDGridSideBySide"))       return EDDGridSideBySide.fromXml(xmlReader);

            if (type.equals("EDDTableCopy"))            return EDDTableCopy.fromXml(xmlReader);
            if (type.equals("EDDTableCopyPost"))        return EDDTableCopyPost.fromXml(xmlReader);
            if (type.equals("EDDTableFromAsciiServiceNOS")) return EDDTableFromAsciiServiceNOS.fromXml(xmlReader);
            //if (type.equals("EDDTableFromBMDE"))        return EDDTableFromBMDE.fromXml(xmlReader); //inactive
            if (type.equals("EDDTableFromDapSequence")) return EDDTableFromDapSequence.fromXml(xmlReader);
            if (type.equals("EDDTableFromDatabase"))    return EDDTableFromDatabase.fromXml(xmlReader);
            if (type.equals("EDDTableFromErddap"))      return EDDTableFromErddap.fromXml(xmlReader);
            //if (type.equals("EDDTableFromMWFS"))        return EDDTableFromMWFS.fromXml(xmlReader); //inactive as of 2009-01-14
            if (type.equals("EDDTableFromAsciiFiles"))  return EDDTableFromAsciiFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromHyraxFiles"))  return EDDTableFromHyraxFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromNcFiles"))     return EDDTableFromNcFiles.fromXml(xmlReader);
            //if (type.equals("EDDTableFromNOS"))         return EDDTableFromNOS.fromXml(xmlReader); //inactive 2010-09-08
            if (type.equals("EDDTableFromNWISDV"))      return EDDTableFromNWISDV.fromXml(xmlReader);
            if (type.equals("EDDTableFromOBIS"))        return EDDTableFromOBIS.fromXml(xmlReader);
            if (type.equals("EDDTableFromPostDatabase"))return EDDTableFromPostDatabase.fromXml(xmlReader);
            if (type.equals("EDDTableFromPostNcFiles")) return EDDTableFromPostNcFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromSOS"))         return EDDTableFromSOS.fromXml(xmlReader);
            if (type.equals("EDDTableFromTaoFiles"))    return EDDTableFromTaoFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromThreddsFiles"))return EDDTableFromThreddsFiles.fromXml(xmlReader);
        } catch (Throwable t) {
            throw new RuntimeException(startError + xmlReader.lineNumber() + 
                ": " + MustBe.getShortErrorMessage(t), t);
        }
        throw new Exception(startError + xmlReader.lineNumber() + 
            ": Unexpected <dataset> type=" + type + ".");
    }

    /**
     * This is used to test the xmlReader constructor in each subclass.
     * Because this uses a simple method to convert the String to bytes,
     * the xml's encoding must be ISO-8859-1 (or just use low ASCII chars).
     * This ignores the &lt;dataset active=.... &gt; setting.
     *
     * @param type the name of the subclass, e.g., EDDGridFromDap
     * @param xml a complete datasets.xml file with the information for one dataset.
     * @return the first dataset defined in the xml
     * @throws Throwable if trouble
     */
    public static EDD oneFromXml(String xml) throws Throwable {
        String2.log("\nEDD.oneFromXml...");
        SimpleXMLReader xmlReader = new SimpleXMLReader(
            new ByteArrayInputStream(String2.toByteArray(xml)), "erddapDatasets");
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            if      (tags.equals("</erddapDatasets>")) {
                xmlReader.close();
                throw new IllegalArgumentException("No <dataset> tag in xml.");
            } else if (tags.equals("<erddapDatasets><dataset>")) {
                EDD edd = fromXml(xmlReader.attributeValue("type"), xmlReader);
                xmlReader.close();
                return edd;
            } else {
                xmlReader.unexpectedTagException();
            }
        }
    }

    /** This is like oneFromXml, but the xml here is just the xml for the 
     * one dataset tag (and subtags).
     * This adds the necessary header and pre and post xml.
     */
    public static EDD oneFromXmlFragment(String xmlFragment) throws Throwable {
        String xml = 
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
            "<erddapDatasets>\n" +
            xmlFragment +
            "</erddapDatasets>\n";
        return oneFromXml(xml);
    }

    /**
     * This is used by various test procedures to get one of the datasets
     * specified in <tomcat>/content/erddap/datasets.xml.
     * This ignores the &lt;dataset active=.... &gt; setting.
     *
     * @param tDatasetID
     * @return an instance of a subclass of EDD
     * @throws Throwable if trouble
     */
    public static EDD oneFromDatasetXml(String tDatasetID) throws Throwable {
        String2.log("\nEDD.oneFromDatasetXml(" + tDatasetID + ")...");

        SimpleXMLReader xmlReader = new SimpleXMLReader(
            new FileInputStream(EDStatic.contentDirectory + 
                "datasets" + (EDStatic.developmentMode? "2" : "") + ".xml"), 
            "erddapDatasets");
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            if      (tags.equals("</erddapDatasets>")) {
                xmlReader.close();
                throw new IllegalArgumentException(tDatasetID + " not found in datasets.xml.");
            } else if (tags.equals("<erddapDatasets><dataset>")) {
                if (xmlReader.attributeValue("datasetID").equals(tDatasetID)) {
                    EDD edd = EDD.fromXml(xmlReader.attributeValue("type"), xmlReader);
                    xmlReader.close();
                    return edd;
                } else {
                    //skip to </dataset> tag
                    while (!tags.equals("<erddapDatasets></dataset>")) {
                        xmlReader.nextTag();
                        tags = xmlReader.allTags();
                    }
                }

            } else if (tags.equals("<erddapDatasets><convertToPublicSourceUrl>")) {
                String tFrom = xmlReader.attributeValue("from");
                String tTo   = xmlReader.attributeValue("to");
                int spo = EDStatic.convertToPublicSourceUrlFromSlashPo(tFrom);
                if (tFrom != null && tFrom.length() > 3 && spo == tFrom.length() - 1 && tTo != null) 
                    EDStatic.convertToPublicSourceUrl.put(tFrom, tTo);                        
            } else if (tags.equals("<erddapDatasets></convertToPublicSourceUrl>")) {
            } else if (tags.equals("<erddapDatasets><requestBlacklist>")) {
            } else if (tags.equals("<erddapDatasets></requestBlacklist>")) {
            } else if (tags.equals("<erddapDatasets><subscriptionEmailBlacklist>")) {
            } else if (tags.equals("<erddapDatasets></subscriptionEmailBlacklist>")) {
            } else if (tags.equals("<erddapDatasets><user>")) {
            } else if (tags.equals("<erddapDatasets></user>")) {
            } else {
                xmlReader.unexpectedTagException();
            }
        }
    }

    /**
     * This is commonly used by subclass constructors to set all the items
     * common to all EDDs.
     * Or, subclasses can just set these things directly.
     *
     * <p>sourceGlobalAttributes and/or addGlobalAttributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   <li> "sourceUrl" - the url (for descriptive purposes only) of the public source of the data,
     *      e.g., the basic opendap url.
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     * Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     * Special case: if addGlobalAttributes name="license", then "[standard]" in the value 
     * will be replaced by EDStatic.standardLicense.
     *
     */
    public void setup(String tDatasetID, 
        Attributes tSourceGlobalAttributes, Attributes tAddGlobalAttributes, 
        EDV[] tDataVariables,
        int tReloadEveryNMinutes) {

        //save the parameters
        datasetID = tDatasetID;
        sourceGlobalAttributes = tSourceGlobalAttributes;
        addGlobalAttributes = tAddGlobalAttributes;
        String tLicense = addGlobalAttributes.getString("license");
        if (tLicense != null)
            addGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        combinedGlobalAttributes.removeValue("null");

        dataVariables = tDataVariables;
        reloadEveryNMinutes = tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE?
            DEFAULT_RELOAD_EVERY_N_MINUTES : tReloadEveryNMinutes;

    }

    /**
     * This should be used by all subclass constructors to ensure that 
     * all of the items common to all EDDs are properly set.
     * This also does a few standard things.
     *
     * @throws Throwable if any required item isn't properly set
     */
    public void ensureValid() throws Throwable {
        //ensure valid
        String errorInMethod = "datasets.xml/EDD.ensureValid error for " + datasetID + ":\n ";

        //test that required things are set
        Test.ensureFileNameSafe(datasetID, errorInMethod + "datasetID");
        File2.makeDirectory(EDStatic.fullCacheDirectory + datasetID);
        //Don't test Test.ensureSomethingUtf8(sourceGlobalAttributes, errorInMethod + "sourceGlobalAttributes");
        //Admin can't control source and addAttributes may override offending characters.
        Test.ensureSomethingUtf8(addGlobalAttributes,     errorInMethod + "addGlobalAttributes");
        Test.ensureSomethingUtf8(combinedGlobalAttributes,errorInMethod + 
            "combinedGlobalAttributes (but probably caused by the source attributes)");
        Test.ensureSomethingUtf8(title(),                 errorInMethod + "title");
        Test.ensureSomethingUtf8(summary(),               errorInMethod + "summary");
        Test.ensureSomethingUtf8(institution(),           errorInMethod + "institution");
        Test.ensureSomethingUtf8(infoUrl(),               errorInMethod + "infoUrl");
        Test.ensureSomethingUtf8(publicSourceUrl(),       errorInMethod + "sourceUrl");
        Test.ensureSomethingUtf8(cdmDataType(),           errorInMethod + "cdm_data_type");
        Test.ensureSomethingUtf8(className(),             errorInMethod + "className");
        int cdmPo = String2.indexOf(CDM_TYPES, cdmDataType());
        if (cdmPo < 0) {
            //if cdm_data_type is just a different case, fix it
            cdmPo = String2.caseInsensitiveIndexOf(CDM_TYPES, cdmDataType());
            if (cdmPo >= 0) {
                cdmDataType = CDM_TYPES[cdmPo];
                combinedGlobalAttributes.set("cdm_data_type", cdmDataType);
            }
        }
        Test.ensureTrue(cdmPo >= 0,      
            errorInMethod + "cdm_data_type=" + cdmDataType + 
            " isn't one of the standard CDM types (" + String2.toCSVString(CDM_TYPES) + ").");
        Test.ensureTrue(dataVariables != null && dataVariables.length > 0, 
            errorInMethod + "'dataVariables' wasn't set.");
        HashSet destNames = new HashSet();
        for (int i = 0; i < dataVariables.length; i++) {
            Test.ensureNotNull(dataVariables[i], errorInMethod + "'dataVariables[" + i + "]' wasn't set.");
            String tErrorInMethod = errorInMethod + 
                "for dataVariable #" + i + "=" + dataVariables[i].destinationName() + ":\n";
            dataVariables[i].ensureValid(tErrorInMethod);
            if (!destNames.add(dataVariables[i].destinationName()))
                throw new IllegalArgumentException(tErrorInMethod + "Two variables have destinationName=" + 
                    dataVariables[i].destinationName() + ".");
        }
        //ensure these are set in the constructor (they may be "")
        extendedSummary();  //ensures that extendedSummaryPartB is constructed
        accessibleViaMAG();
        accessibleViaSubset();
        accessibleViaSOS();
        accessibleViaWCS();
        accessibleViaWMS(); 
        accessibleViaNcCF(); 
        if (this instanceof EDDTable)
            String2.log("  accessibleViaNcCF=" +
                (accessibleViaNcCF.length() == 0? "[true]" : accessibleViaNcCF));

        //String2.log("\n***** beginSearchString\n" + String2.utf8ToString(searchString()) + 
        //            "\n***** endSearchString\n");
        //reloadEveryNMinutes
    }


    /**
     * The string representation of this gridDataSet (for diagnostic purposes).
     *
     * @return the string representation of this EDD.
     */
    public String toString() {  
        //make this JSON format?
        StringBuilder sb = new StringBuilder();
        sb.append(datasetID + ": " + 
            "\ntitle=" + title() +
            "\nsummary=" + summary() +
            "\ninstitution=" + institution() +
            "\ninfoUrl=" + infoUrl() +
            "\nlocalSourceUrl=" + localSourceUrl +
            "\npublicSourceUrl=" + publicSourceUrl() +
            "\ncdm_data_type=" + cdmDataType() +
            "\nreloadEveryNMinutes=" + reloadEveryNMinutes +
            "\nonChange=" + onChange +
            "\nsourceGlobalAttributes=\n" + sourceGlobalAttributes + 
              "addGlobalAttributes=\n" + addGlobalAttributes);
        for (int i = 0; i < dataVariables.length; i++)
            sb.append("dataVariables[" + i + "]=" + dataVariables[i]);
        return sb.toString();
    }

    /**
     * This tests if the dataVariables of the other dataset are similar 
     *     (same destination data var names, same sourceDataType, same units, 
     *     same missing values).
     *
     * @param other
     * @return "" if similar (same axis and data var names,
     *    same units, same sourceDataType, same missing values) or a message if not.
     */
    public String similar(EDD other) {
        String msg = "EDD.similar: The other dataset has a different ";

        try {
            if (other == null) 
                return "EDD.similar: Previously, this dataset was (temporarily?) not available.  " +
                    "Perhaps ERDDAP was just restarted.";

            int nDv = dataVariables.length;
            if (nDv != other.dataVariables.length)
                return msg + "number of dataVariables (" + 
                    nDv + " != " + other.dataVariables.length + ")";

            for (int dv = 0; dv < nDv; dv++) {
                EDV dv1 = dataVariables[dv];
                EDV dv2 = other.dataVariables[dv];

                //destinationName
                String s1 = dv1.destinationName();
                String s2 = dv2.destinationName();
                String msg2 = " for dataVariable #" + dv + "=" + s1 + " (";
                if (!s1.equals(s2))
                    return msg + "destinationName" + msg2 +  s1 + " != " + s2 + ")";

                //sourceDataType
                s1 = dv1.sourceDataType();
                s2 = dv2.sourceDataType();
                if (!s1.equals(s2))
                    return msg + "sourceDataType" + msg2 +  s1 + " != " + s2 + ")";

                //destinationDataType
                s1 = dv1.destinationDataType();
                s2 = dv2.destinationDataType();
                if (!s1.equals(s2))
                    return msg + "destinationDataType" + msg2 +  s1 + " != " + s2 + ")";

                //units
                s1 = dv1.units();
                s2 = dv2.units();
                if (!Test.equal(s1, s2)) //may be null 
                    return msg + "units" + msg2 +  s1 + " != " + s2 + ")";

                //sourceMissingValue
                double d1 = dv1.sourceMissingValue();
                double d2 = dv2.sourceMissingValue();
                if (!Test.equal(d1, d2)) //says NaN==NaN is true
                    return msg + "sourceMissingValue" + msg2 +  d1 + " != " + d2 + ")";

                //sourceFillValue
                d1 = dv1.sourceFillValue();
                d2 = dv2.sourceFillValue();
                if (!Test.equal(d1, d2)) //says NaN==NaN is true
                    return msg + "sourceFillValue" + msg2 +  d1 + " != " + d2 + ")";
            }

            //they are similar
            return "";
        } catch (Throwable t) {
            return MustBe.throwableToShortString(t);
        }
    }

    protected static String test1Changed(String msg, String diff) {
        return diff.length() == 0? "" : msg + "\n" + diff + "\n";
    }

    protected static String test2Changed(String msg, String oldS, String newS) {
        if (oldS.equals(newS))
            return "";
        return msg + 
            "\n  old=" + oldS + ",\n" +
              "  new=" + newS + ".\n";
    }

    /**
     * This tests if 'old' is different from this in any way.
     * <br>This test is from the view of a subscriber who wants to know
     *    when a dataset has changed in any way.
     * <br>So some things like onChange and reloadEveryNMinutes are not checked.
     * <br>This only lists the first change found.
     *
     * <p>EDDGrid overwrites this to also check the axis variables.
     *
     * @param old
     * @return "" if same or message if not.
     */
    public String changed(EDD old) {
        //future: perhaps it would be nice if EDDTable changed showed new data.
        //  so it would appear in email subscription and rss.
        //  but for many datasets (e.g., ndbc met) there are huge number of buoys. so not practical.
        if (old == null)
            return "Previously, this dataset was (temporarily?) not available.  Perhaps ERDDAP was just restarted.\n";

        //check most important things first
        int nDv = dataVariables.length;
        StringBuilder diff = new StringBuilder();
        diff.append(test2Changed("The number of dataVariables changed:",
            "" + old.dataVariables.length, 
            "" + nDv));
        if (diff.length() > 0) 
            return diff.toString(); //because tests below assume nDv are same

        for (int dv = 0; dv < nDv; dv++) { 
            EDV oldDV = old.dataVariables()[dv];
            EDV newDV =     dataVariables()[dv];             
            String newName = newDV.destinationName();

            diff.append(test2Changed(
                "The destinationName for dataVariable #" + dv + " changed:",
                oldDV.destinationName(), 
                newName));

            diff.append(test2Changed(
                "The destinationDataType for dataVariable #" + dv + "=" + newName + " changed:",
                oldDV.destinationDataType(), 
                newDV.destinationDataType()));

            diff.append(test1Changed(
                "A combinedAttribute for dataVariable #" + dv + "=" + newName + " changed:",
                String2.differentLine(
                    oldDV.combinedAttributes().toString(), 
                    newDV.combinedAttributes().toString())));
        }

        //check least important things last
        diff.append(test1Changed("A combinedGlobalAttribute changed:",
            String2.differentLine(
                old.combinedGlobalAttributes().toString(), 
                    combinedGlobalAttributes().toString())));

        return diff.toString();    
    }

    /** The directory to be used for caching files for this dataset (with "/" at end).
     */
    public String cacheDirectory() {
        return EDStatic.fullCacheDirectory + datasetID + "/"; //dir is created by EDD.ensureValid
    }

    /** 
     * This returns the link tag for an HTML head section which advertises 
     * the RSS feed for this dataset.
     */
    public String rssHeadLink(String loggedInAs) {
        return 
            "<link rel=\"alternate\" type=\"application/rss+xml\" \n" +
            "  href=\"" + EDStatic.erddapUrl + //RSS always uses non-https link
                "/rss/" + datasetID + ".rss\" \n" +
            "  title=\"ERDDAP: " + title() + "\">\n";
    }

    /** 
     * This returns the a/href tag which advertises the RSS feed for this dataset.
     */
    public String rssHref(String loggedInAs) {
        return 
            "<a type=\"application/rss+xml\" " +
            "  href=\"" + EDStatic.erddapUrl + //RSS always uses a non-https link
            "/rss/" + datasetID+ ".rss\" \n" +
            "  title=\"\"><img alt=\"RSS\" align=\"bottom\" \n" +
            "    title=\"" + EDStatic.subscriptionRSS + "\" \n" +
            "    src=\"" + EDStatic.imageDirUrl(loggedInAs) + "rss.gif\" ></a>"; //no img end tag 
    }

    /** 
     * This returns the a/href tag which advertises the email subscription url for this dataset
     * (or "" if !EDStatic.subscriptionSystemActive).
     * Unlike RSS, email can use https if user is loggedIn.
     *
     * @param loggedInAs
     */
    public String emailHref(String loggedInAs) {
        if (EDStatic.subscriptionSystemActive) 
            return 
            "<a href=\"" + EDStatic.erddapUrl(loggedInAs) + "/" + Subscriptions.ADD_HTML + 
                "?datasetID=" + datasetID+ "&amp;showErrors=false&amp;email=\" \n" +
            "  title=\"\"><img alt=\"Subscribe\" align=\"bottom\" \n" +
            "    title=\"" + XML.encodeAsHTML(EDStatic.subscriptionEmail) + "\" \n" +
            "    src=\"" + EDStatic.imageDirUrl(loggedInAs) + "envelope.gif\" ></a>";
        return "&nbsp;";
    }

    /**
     * This is used by EDDXxx.fromXml to get Attributes from the e.g., datasets.xml file.
     * 
     * @param xmlReader with the (e.g.,) ...<globalAttributes> having just been read.    
     *    The subsequent tags must all be &lt;att name=\"attName\" type=\"someType\"&gt;someValue&lt;/att&gt; .
     *    <br>someType can be:
     *    <br>for single values: boolean, unsignedShort, short, int, long, float, double, or string,
     *       (these are standard XML atomic data types),
     *    <br>or for space-separated values: 
     *       booleanList, unsignedShortList, shortList, intList, longList, floatList, doubleList, or stringList,
     *       (these could be defined in an XML schema as xml Lists: space separated lists of atomic types).
     *    <br>Or, type=\"someType\" can be omitted (interpreted as 'string').
     *    <br>If type='stringList', individual values with interior whitespace or commas
     *    must be completely enclosed in double quotes with interior double
     *    quotes converted to 2 double quotes. For String values without interior whitespace or commas,
     *    you don't have to double quote the whole value. 
     *    This doesn't match the xml definition of list as applied to strings,
     *    but a stringList type could be defined as string data which has special
     *    meaning to this application.
     * @return the Attributes based on the information in an .xml file.
     *    And xmlReader will have just read (e.g.,) ...</globalAttributes>
     * @throws Throwable if trouble
     */
    public static Attributes getAttributesFromXml(SimpleXMLReader xmlReader) throws Throwable {

        //process the tags
        if (reallyVerbose) String2.log("    getAttributesFromXml...");
        Attributes tAttributes = new Attributes();
        int startOfTagsN = xmlReader.stackSize();
        String tName = null, tType =  null;
        while (true) {
            xmlReader.nextTag();
            String topTag = xmlReader.topTag();
            if (xmlReader.stackSize() == startOfTagsN) {
                if (reallyVerbose) String2.log("      leaving getAttributesFromXml");
                return tAttributes; //the </attributes> tag
            }
            if (xmlReader.stackSize() > startOfTagsN + 1) 
                xmlReader.unexpectedTagException();

            if (topTag.equals("att")) {
                tName = xmlReader.attributeValue("name");
                tType = xmlReader.attributeValue("type");

            } else if (topTag.equals("/att")) {
                String content = xmlReader.content();
                if (tName == null)
                    throw new IllegalArgumentException("datasets.xml error on line #" + xmlReader.lineNumber() +
                        ": An <att> tag doesn't have a \"name\" attribute (content=" + content + ").");
                //if (reallyVerbose) 
                //    String2.log("      tags=" + xmlReader.allTags() + 
                //        " name=" + tName + " type=" + tType + " content=" + content);
                if (tType == null) 
                    tType = "string";
                PrimitiveArray pa;
                if (content.length() == 0) {
                    //content="" interpreted as want to delete that attribute
                    pa = new StringArray();  //always make it a StringArray (to hold "null")
                    pa.addString("null"); 
                } else if (tType.equals("string") || tType.equals("String")) { //spec requests "string"; support "String" to be nice?
                    //for "string", don't want to split at commas
                    pa = new StringArray(); 
                    pa.addString(content);
                } else {
                    //for all other types, csv designation is irrelevant
                    if (tType.endsWith("List")) 
                        tType = tType.substring(0, tType.length() - 4);
                    if (tType.equals("unsignedShort")) //the xml name
                        tType = "char"; //the PrimitiveArray name
                    else if (tType.equals("string")) //the xml name
                        tType = "String"; //the PrimitiveArray name
                    pa = PrimitiveArray.ssvFactory(PrimitiveArray.elementStringToClass(tType), 
                        content); 
                }
                //if (tName.equals("_FillValue")) 
                //    String2.log("!!!!EDD attribute name=\"" + tName + "\" content=" + content + 
                //    "\n  type=" + pa.elementClassString() + " pa=" + pa.toString());
                tAttributes.add(tName, pa);
                //String2.log("????EDD _FillValue=" + tAttributes.get("_FillValue"));

            } else {
                xmlReader.unexpectedTagException();
            }
        }
    }

    /**
     * This is used by EDDXxx.fromXml to get the sourceName,destinationName,attributes information for an 
     * axisVariable or dataVariable from the e.g., datasets.xml file.
     * Unofficial: this is becoming the standard for &lt;axisVariable&gt;.
     * 
     * @param xmlReader with the ...&lt;axisVariable&gt; or ...&lt;dataVariable&gt; having just been read.    
     *    The allowed subtags are sourceName, destinationName, and addAttributes.
     * @return Object[4] [0]=sourceName, [1]=destinationName, [2]=addAttributes, 
     *    [3]=values PrimitiveArray.
     *    This doesn't check the validity of the objects. The objects may be null. 
     *    The xmlReader will have just read ...&lt;/axisVariable&gt; or ...&lt;/dataVariable&gt;.
     * @throws Throwable if trouble
     */
    public static Object[] getSDAVVariableFromXml(SimpleXMLReader xmlReader) throws Throwable {

        //process the tags
        if (reallyVerbose) String2.log("  getSDAVVariableFromXml...");
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        String tSourceName = null, tDestinationName = null;
        Attributes tAttributes = null;
        PrimitiveArray tValuesPA = null;
        while (true) {
            xmlReader.nextTag();
            String topTag = xmlReader.topTag();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("    topTag=" + topTag + " content=" + content);
            if (xmlReader.stackSize() == startOfTagsN) { //the /variable tag
                if (reallyVerbose) String2.log("    leaving getSDAVVariableFromXml" +
                    " sourceName=" + tSourceName + " destName=" + tDestinationName);
                return new Object[]{tSourceName, tDestinationName, tAttributes, tValuesPA};
            }
            if (xmlReader.stackSize() > startOfTagsN + 1) 
                xmlReader.unexpectedTagException();

            if      (topTag.equals( "sourceName")) {}
            else if (topTag.equals("/sourceName")) tSourceName = content;
            else if (topTag.equals( "destinationName")) {}
            else if (topTag.equals("/destinationName")) tDestinationName = content;
            else if (topTag.equals( "addAttributes"))
                tAttributes = getAttributesFromXml(xmlReader);
            else if (topTag.equals( "values")) {
                //always make a PA 
                String type = xmlReader.attributeValue("type");
                if (type == null) 
                    type = "";
                if (type.endsWith("List"))
                    type = type.substring(0, type.length() - 4);
                Class elementClass = PrimitiveArray.elementStringToClass(type); //throws Throwable if trouble
                double start      = String2.parseDouble(xmlReader.attributeValue("start"));
                double increment  = String2.parseDouble(xmlReader.attributeValue("increment"));
                int n             = String2.parseInt(xmlReader.attributeValue("n"));
                if (!Double.isNaN(start) && 
                    increment > 0 && //this could change to !NaN and !0
                    n > 0 && n < Integer.MAX_VALUE) {
                    //make PA with 1+ evenly spaced values
                    tValuesPA = PrimitiveArray.factory(elementClass, n, false);
                    for (int i = 0; i < n; i++) 
                        tValuesPA.addDouble(start + i * increment);
                } else {
                    //make PA with correct type, but size=0
                    tValuesPA = PrimitiveArray.factory(elementClass, 0, "");
                }
            } else if (topTag.equals("/values")) {
                if (tValuesPA.size() == 0) {
                    //make a new PA from content values 
                    tValuesPA = PrimitiveArray.csvFactory(tValuesPA.elementClass(), content);         
                }
                if (reallyVerbose) String2.log("values for sourceName=" + tSourceName + "=" + tValuesPA.toString());

            } else xmlReader.unexpectedTagException();

        }
    }

    /**
     * This is used by EDDXxx.fromXml to get the 
     * sourceName,destinationName,attributes,dataType information for an 
     * axisVariable or dataVariable from the e.g., datasets.xml file.
     * Unofficial: this is becoming the standard for &lt;dataVariable&gt;.
     * 
     * @param xmlReader with the ...&lt;axisVariable&gt; or ...&lt;dataVariable&gt; having just been read.    
     *    The allowed subtags are sourceName, destinationName, addAttributes, and dataType. 
     * @return Object[4] 0=sourceName, 1=destinationName, 2=addAttributes, 3=dataType.
     *    This doesn't check the validity of the objects. The objects may be null. 
     *    The xmlReader will have just read ...&lt;/axisVariable&gt; or ...&lt;/dataVariable&gt;
     * @throws Throwable if trouble
     */
    public static Object[] getSDADVariableFromXml(SimpleXMLReader xmlReader) throws Throwable {

        //process the tags
        if (reallyVerbose) String2.log("  getSDADVVariableFromXml...");
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        String tSourceName = null, tDestinationName = null, tDataType = null;
        Attributes tAttributes = null;
        while (true) {
            xmlReader.nextTag();
            String topTag = xmlReader.topTag();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("    topTag=" + topTag + " content=" + content);
            if (xmlReader.stackSize() == startOfTagsN) { //the /variable tag
                if (reallyVerbose) String2.log("    leaving getSDADVVariableFromXml" +
                    " sourceName=" + tSourceName + " destName=" + tDestinationName + " dataType=" + tDataType);
                return new Object[]{tSourceName, tDestinationName, tAttributes, tDataType};
            }
            if (xmlReader.stackSize() > startOfTagsN + 1) 
                xmlReader.unexpectedTagException();

            if      (topTag.equals( "sourceName")) {}
            else if (topTag.equals("/sourceName")) tSourceName = content;
            else if (topTag.equals( "destinationName")) {}
            else if (topTag.equals("/destinationName")) tDestinationName = content;
            else if (topTag.equals( "dataType")) {}
            else if (topTag.equals("/dataType")) tDataType = content;
            else if (topTag.equals( "addAttributes")) {
                tAttributes = getAttributesFromXml(xmlReader);
                //PrimitiveArray taa= tAttributes.get("_FillValue");
                //String2.log("getSDAD " + tSourceName + " _FillValue=" + taa);
            } else xmlReader.unexpectedTagException();
        }
    }

    /**
     * This sets accessibleTo.
     *
     * @param csvList a space separated value list.
     *    null indicates the dataset is accessible to anyone.
     *    "" means it is accessible to no one.
     */
    public void setAccessibleTo(String csvList) {
        if (csvList == null) {
            accessibleTo = null;  //accessible to all
            return;
        }
        
        accessibleTo = csvList.trim().length() == 0? new String[0] : //accessible to no one
            String2.split(csvList, ',');
        Arrays.sort(accessibleTo);
    }

    /**
     * This gets accessibleTo.
     *
     * @return accessibleTo 
     *    null indicates the dataset is accessible to anyone (i.e., it is public).
     *    length=0 means it is accessible to no one.
     *    length>0 means it is accessible to some roles.
     */
    public String[] getAccessibleTo() {
        return accessibleTo;
    }

    /**
     * Given a list of the current user's roles, this compares it to
     * accessibleTo to determine if this dataset is accessible to this user.
     *
     * @param roles a sorted list of the current user's roles, or null if not logged in.
     * @return true if the dataset is accessible to this user
     */
    public boolean isAccessibleTo(String roles[]) {
        String message = "";
        boolean showMessage = reallyVerbose;
        if (showMessage) message = datasetID + 
            " accessibleTo=" + String2.toSSVString(accessibleTo) + 
            "\n  user roles=" + String2.toSSVString(roles) +
            "\n  accessible="; 

        //dataset is accessible to all?
        if (accessibleTo == null) {
            if (showMessage) String2.log(message + "true");
            return true;
        }

        //i.e., user not logged in
        if (roles == null) {
            if (showMessage) String2.log(message + "false");
            return false;
        }

        //look for a match in the two sorted lists by walking along each list
        int accessibleToPo = 0;
        int rolesPo = 0;
        while (accessibleToPo < accessibleTo.length &&
               rolesPo < roles.length) {
            int diff = accessibleTo[accessibleToPo].compareTo(roles[rolesPo]);
            if (diff == 0) {
                if (showMessage) String2.log(message + "true");
                return true;
            }

            //advance the pointer for the lower string
            if (diff < 0) accessibleToPo++;
            else rolesPo++;
        }

        //we reached the end of one of the lists without finding a match
        if (showMessage) String2.log(message + "false");
        return false;
    }

    /**
     * This indicates why the dataset isn't accessible via Make A Graph
     * (or "" if it is).
     */
    public abstract String accessibleViaMAG();

    /**
     * This indicates why the dataset isn't accessible via .subset
     * (or "" if it is).
     */
    public abstract String accessibleViaSubset();

    /** 
     * This indicates why the dataset isn't accessible via SOS
     * (or "" if it is).
     */
    public abstract String accessibleViaSOS();

    /** 
     * This indicates why the dataset isn't accessible via WCS
     * (or "" if it is).
     */
    public abstract String accessibleViaWCS();

    /** 
     * This indicates why the dataset isn't accessible via .ncCF file type
     * (or "" if it is).
     * Currently, this is only for some of the Discrete Sampling Geometries cdm_data_type 
     * representations at
     * https://cf-pcmdi.llnl.gov/trac/wiki/PointObservationConventions (currently out-of-date)
     */
    public abstract String accessibleViaNcCF();

    /** 
     * This indicates why the dataset is accessible via WMS
     * (or "" if it is).
     */
    public abstract String accessibleViaWMS();

    /**
     * This returns the dapProtocol for this dataset (e.g., griddap).
     *
     * @return the dapProtocol
     */
    public abstract String dapProtocol();

    /**
     * This returns an HTML description of the dapProtocol for this dataset.
     *
     * @return the dapDescription
     */
    public abstract String dapDescription();


    /** 
     * The datasetID is a very short string identifier 
     * (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     * for this dataset, 
     * often the source of the dataset (e.g., "erd") and the source's
     * name for the dataset (e.g., "ATssta8day") combined (e.g., "erdATssta8day").  
     * <br>The datasetID must be unique, 
     *   as datasetID is used as the virtual directory for this dataset.
     * <br>This is for use in this program (it is part of the datasets name
     *   that is shown to the user) and shouldn't (if at all possible)
     *   change over time (whereas the 'title' might change). 
     * <br>This needn't match any external name for this dataset (e.g., the
     *   id used by the source, or close to it), but it is sometimes helpful for users if it does.
     * <br>It is usually &lt; 15 characters long.
     *
     * @return the datasetID
     */
    public String datasetID() {return datasetID; }

    /**
     * The directory in which information for this dataset (e.g., fileTable.nc) is stored.
     *
     */
    public String datasetInfoDir() {
        return datasetInfoDir(datasetID);
    }

    /**
     * The directory in which information for a dataset (e.g., fileTable.nc) is stored.
     *
     */
    public static String datasetInfoDir(String tDatasetID) {
        return EDStatic.fullDatasetInfoDirectory + tDatasetID + "/";
    }

    /** 
     * The className is the name of the non-abstract subclass of this EDD, e.g., EDDTableFromDapSequence.
     * 
     * @return the className
     */
    public String className() {return className; }

    /** 
     * onChange is a list of 0 or more actions (starting with "http://" or "mailto:")
     * to be done whenever the dataset changes significantly.
     * onChange may be null.
     *
     * @return the internal onChange StringArray -- don't change it!
     */
    public StringArray onChange() {return onChange; }

    /** 
     * The title is a descriptive title for this dataset, 
     * e.g., "SST Anomaly, Pathfinder Ver 5.0, Day and Night, 0.05 degrees, Global, Science Quality".  
     * It is usually &lt; 80 characters long.
     * The information is often originally from the CF global metadata for "title".
     *
     * @return the title
     */
    public String title() {
        if (title == null) 
            title = combinedGlobalAttributes.getString("title");
        return title;
    }

    /** 
     * The summary is a longer description for this dataset.  
     * It is usually &lt; 500 characters long.
     * It may have newline characters (usually at &lt;= 72 chars per line).
     * The information is often originally from the CF global metadata for "summary".
     *
     * @return the summary
     */
    public String summary() {
        if (summary == null) 
            summary = combinedGlobalAttributes.getString("summary");
        return summary; 
    }

    /** 
     * The extendedSummary is summary() plus a list of variable names, long names, and units.
     *
     * @return the extendedSummary
     */
    public String extendedSummary() {
        String tSummary = summary();
        if (extendedSummaryPartB == null) {
            String nllSummary = String2.noLongLinesAtSpace(tSummary, 100, ""); //as it will be shown 
            int nllSummaryLength = nllSummary.length();
            int nLines = 0; 
            for (int i = 0; i < nllSummaryLength; i++) {
                if (nllSummary.charAt(i) == '\n')
                    nLines++;
            }

            //standardize the blank lines
            StringBuilder sb = new StringBuilder();
            if (tSummary.endsWith("\n\n")) {     
                //do nothing
            } else if (tSummary.endsWith("\n")) {
                sb.append('\n');   nLines++;
            } else {
                sb.append("\n\n"); nLines += 2;
            }

            //add the CDM info
            sb.append("cdm_data_type = " + cdmDataType() + "\n");
            nLines++;
            //list the stationVariables, trajectoryVariables, profileVariables?

            //add the list of variables
            sb.append("VARIABLES"); 
            if (this instanceof EDDGrid) 
                sb.append(" (all of which use the dimensions " + ((EDDGrid)this).allDimString() + ")");
            sb.append(":\n");
            nLines++;
            for (int dv = 0; dv < dataVariables.length; dv++) {
                EDV edv = dataVariables[dv];
                String lName = edv.destinationName().length() == edv.longName().length()? "" : 
                    edv.longName();
                String tUnits = edv.units() == null? "" : edv.units();
                String glue = lName.length() > 0 && tUnits.length() > 0? ", " : "";
                sb.append(edv.destinationName() + 
                    (lName.length() > 0 || tUnits.length() > 0? 
                        " (" + lName + glue + tUnits + ")" : "") +
                    "\n");                

                nLines++;
                if (nLines > 30 && dv < dataVariables.length - 4) { //don't do this if just a few more dv
                    sb.append("... (" + (dataVariables.length - dv - 1) + " more variables)\n");
                    break;
                }
            }
            extendedSummaryPartB = sb.toString();  //it is important that assignment be atomic
        }
        return extendedSummaryPartB.length() == 0? tSummary :
            tSummary + extendedSummaryPartB; 
    }

    /** 
     * The institution identifies the source of the data which should receive
     * credit for the data, suitable for "Data courtesy of " in the legend on a graph,
     * e.g., NOAA NESDIS OSDPD. 
     * It is usually &lt; 20 characters long.
     * The information is often originally from the CF global metadata for "institution".
     * 
     * @return the institution
     */
    public String institution() {
        if (institution == null) 
            institution = combinedGlobalAttributes.getString(EDStatic.INSTITUTION);
        return institution; 
    }

    /** 
     * The infoUrl identifies a url with information about the dataset. 
     * The information was supplied by the constructor and is stored as 
     * global metadata for "infoUrl" (non-standard).
     * 
     * @return the infoUrl
     */
    public String infoUrl() {
        if (infoUrl == null) 
            infoUrl = combinedGlobalAttributes.getString("infoUrl");
        return infoUrl; 
    }

    /** 
     * The localSourceUrl identifies the source (usually) url actually used
     * to get data. 
     * The information was supplied by the constructor.
     * For a FromErddap, this is the URL of the dataset on the remote ERDDAP.
     * 
     * @return the localSourceUrl
     */
    public String localSourceUrl() {
        return localSourceUrl; 
    }

    /** 
     * The publicSourceUrl identifies the source (usually) url from the
     * combinedGlobalAttributes. 
     * For a FromErddap, this is the (e.g.,) opendap server that the remote ERDDAP
     * gets data from.
     * 
     * @return the publicSourceUrl
     */
    public String publicSourceUrl() {
        if (publicSourceUrl == null) 
            publicSourceUrl = combinedGlobalAttributes.getString("sourceUrl");
        return publicSourceUrl; 
    }

    /**
     * This converts a localSourceUrl into a publicSourceUrl.
     * See the description of convertToPublicSourceUrl in datasets.xml.
     * Hopefully it will be improved and allow conversions to be specified in datasets.xml.
     * For example,
     * &lt;convertToPublicSourceUrl from="http://192.168.31.18/" to="http://oceanwatch.pfeg.noaa.gov/" /&gt;
     * will cause a matching local sourceUrl (such as http://192.168.31.18/thredds/dodsC/satellite/BA/ssta/5day)
     * into a public sourceUrl (http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day). 
     *
     * @param tLocalSourceUrl
     * @return publicSourceUrl (or tLocalSourceUrl if no change specified).
     */
    public static String convertToPublicSourceUrl(String tLocalSourceUrl) {
        //look for "[something]//[something]/..."
        int slashPo2 = EDStatic.convertToPublicSourceUrlFromSlashPo(tLocalSourceUrl);
        if (slashPo2 > 0) {
            String tFrom = tLocalSourceUrl.substring(0, slashPo2 + 1);
            String tTo = (String)EDStatic.convertToPublicSourceUrl.get(tFrom);
            if (tTo != null) //there is a match
                return tTo + tLocalSourceUrl.substring(slashPo2 + 1);
        }
        return tLocalSourceUrl;
    }

    /** 
     * The cdm_data_type global attribute identifies the type of data according to the 
     * options in 
     * http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html
     * for the cdm_data_type global metadata. 
     * It must be one of "Grid", "Station", "Trajectory", "Point" 
     * (or not yet used options: "Image", "Swath"). 
     * [What if some other type???]
     * 
     * @return the cdmDataType
     */
    public String cdmDataType() {
        if (cdmDataType == null) 
            cdmDataType = combinedGlobalAttributes.getString("cdm_data_type");
        return cdmDataType; 
    }

    /** This returns the accessConstraints (e.g., for ERDDAP's SOS, WCS, WMS)
     * from combinedGlobalAttributes (checked first)
     * or EDStatic (from setup.xml). */
    public String accessConstraints() {
        String ac = combinedGlobalAttributes().getString("accessConstraints");
        if (ac != null)
            return ac;

        return getAccessibleTo() == null?
            EDStatic.accessConstraints :
            EDStatic.accessRequiresAuthorization;
    }

    /** This returns the fees (e.g., for ERDDAP's SOS, WCS, WMS) 
     * from combinedGlobalAttributes (checked first)
     * or EDStatic (from setup.xml). */
    public String fees() {
        String fees = combinedGlobalAttributes().getString("fees");
        return fees == null? EDStatic.fees : fees;
    }

    /** This returns the keywords (e.g., for ERDDAP's SOS, WCS, WMS) 
     * from combinedGlobalAttributes (checked first)
     * or EDStatic (from setup.xml).
     */
    public String[] keywords() {
        String kw = combinedGlobalAttributes().getString("keywords");
        if (kw == null)
            kw = EDStatic.keywords;
        if (kw == null || kw.length() == 0)
            return new String[0];

        //split it
        if (kw.indexOf('>') >= 0)   //gcmd keywords may be separated with '>'
            return String2.split(kw, '>');
        if (kw.indexOf('|') >= 0)  
            return String2.split(kw, '|');
        return StringArray.wordsAndQuotedPhrases(kw).toArray(); //comma or space separated
    }

    /** This returns the featureOfInterest from combinedGlobalAttributes (checked first)
     * or EDStatic (from setup.xml). */
    public String sosFeatureOfInterest() {
        String foi = combinedGlobalAttributes().getString("sosFeatureOfInterest");
        return foi == null? EDStatic.sosFeatureOfInterest : foi;
    }

    /** This returns the sosStandardNamePrefix from combinedGlobalAttributes (checked first)
     * or EDStatic (from setup.xml). */
    public String sosStandardNamePrefix() {
        String snp = combinedGlobalAttributes().getString("sosStandardNamePrefix");
        return snp == null? EDStatic.sosStandardNamePrefix : snp;
    }

    /** This returns the sosUrnBase from combinedGlobalAttributes (checked first)
     * or EDStatic (from setup.xml). */
    public String sosUrnBase() {
        String sub = combinedGlobalAttributes().getString("sosUrnBase");
        return sub == null? EDStatic.sosUrnBase : sub;
    }

    /** This returns the default value of drawLandMask (false=under, true=over)
     * for this dataset (or this ERDDAP installation as specified in setup.xml).
     * The combinedAttributes setting (if any) has priority over the setup.xml setting.
     */
    public boolean defaultDrawLandMask() {
        String dlm = combinedGlobalAttributes().getString("drawLandMask"); 
        if (dlm == null) 
            return EDStatic.drawLandMask.equals("over");
        return !dlm.equals("under"); //'over' is preferred
    }

    /** 
     * The global attributes from the source.
     *
     * @return the global attributes from the source.
     */
    public Attributes sourceGlobalAttributes() {return sourceGlobalAttributes; }

    /**
     * The global attributes which will be added to (and take precedence over) 
     * the sourceGlobalAttributes when results files are created.
     *
     * @return the global attributes which will be added to (and take precedence over) 
     * the sourceGlobal attributes when results files are created.
     */
    public Attributes addGlobalAttributes() {return addGlobalAttributes; }

    /**
     * The source+add global attributes, then tweaked (e.g., remove "null" values).
     * 
     * @return the source+add global attributes.
     */
    public Attributes combinedGlobalAttributes() {return combinedGlobalAttributes;  }

    /** 
     * This returns the data variable which has the specified source name.
     *
     * @return the specified data variable sourceName
     * @throws Throwable if not found
     */
    public EDV findDataVariableBySourceName(String tSourceName) 
        throws Throwable {

        int which = String2.indexOf(dataVariableSourceNames(), tSourceName);
        if (which < 0) throw new SimpleException(
            "Error: source variable name='" + tSourceName + "' wasn't found.");
        return dataVariables[which];
    }

    /** 
     * This returns the data variable which has the specified destination name.
     *
     * @return the specified data variable destinationName
     * @throws Throwable if not found
     */
    public EDV findDataVariableByDestinationName(String tDestinationName) 
        throws Throwable {

        int which = String2.indexOf(dataVariableDestinationNames(), tDestinationName);
        if (which < 0) throw new SimpleException(
            "Error: destination variable name='" + tDestinationName + "' wasn't found.");
        return dataVariables[which];
    }

    /**
     * This returns a list of the dataVariables' source names.
     *
     * @return a list of the dataVariables' source names.
     *    This always returns the same internal array, so don't change it!
     */
    public String[] dataVariableSourceNames() {
        if (dataVariableSourceNames == null) {
            //thread safe since this is done in constructor thread
            String tNames[] = new String[dataVariables.length];
            for (int i = 0; i < dataVariables.length; i++)
                tNames[i] = dataVariables[i].sourceName();
            dataVariableSourceNames = tNames;
        }
        return dataVariableSourceNames;
    }

    /**
     * This returns a list of the dataVariables' destination names.
     *
     * @return a list of the dataVariables' destination names.
     *    This always returns the same internal array, so don't change it!
     */
    public String[] dataVariableDestinationNames() {
        if (dataVariableDestinationNames == null) {
            //thread safe since this is done in constructor thread
            String tNames[] = new String[dataVariables.length];
            for (int i = 0; i < dataVariables.length; i++)
                tNames[i] = dataVariables[i].destinationName();
            dataVariableDestinationNames = tNames;
        }
        return dataVariableDestinationNames;
    }

    /**
     * This returns the dataVariables.
     * This is the internal data structure, so don't change it.
     *
     * @return the dataVariables.
     */
    public EDV[] dataVariables() {return dataVariables; }

    /** 
     * This returns the axis or data variable which has the specified destination name.
     * This implementation only knows about data variables, so subclasses
     * like EDDGrid that have axis variables, too, override it.
     *
     * @return the specified axis or data variable destinationName
     * @throws Throwable if not found
     */
    public EDV findVariableByDestinationName(String tDestinationName) 
        throws Throwable {
        return findDataVariableByDestinationName(tDestinationName);
    }

    /** 
     * creationTimeMillis indicates when this dataset was created.
     * 
     * @return when this dataset was created
     */
    public long creationTimeMillis() {return creationTimeMillis; }


    /** 
     * reloadEveryNMinutes indicates how often this program should check
     * for new data for this dataset by recreating this EDD, e.g., 60. 
     * 
     * @return the suggested number of minutes between refreshes
     */
    public int getReloadEveryNMinutes() {return reloadEveryNMinutes; }

    /** 
     * This sets reloadEveryNMinutes.
     * 
     * @param minutes if &lt;=0 or == Integer.MAX_VALUE,
     *    this uses DEFAULT_RELOAD_EVERY_N_MINUTES.
     */
    public void setReloadEveryNMinutes(int tReloadEveryNMinutes) {
        reloadEveryNMinutes = 
            tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE?
            DEFAULT_RELOAD_EVERY_N_MINUTES : tReloadEveryNMinutes;
    }

    /**
     * This marks this dataset so that it will be reloaded soon 
     * (but not as fast as possible -- via requestReloadASAP) 
     * by setting the creationTime to 0, making it appear as if the 
     * dataset was created long, long ago.
     * In LoadDatasets, &lt;=0 is treated as special case to force reload
     * no matter what reloadEveryNMinutes is.
     */
    public void setCreationTimeTo0() {
        creationTimeMillis = 0;
    }

    /**
     * This creates a flag file in the EDStatic.fullResetFlagDirectory
     * to mark this dataset so that it will be reloaded as soon as possible.
     *
     * When an exception is thrown and this is called, EDStatic.waitThenTryAgain
     * is usually at the start of the exception message.
     */
    public void requestReloadASAP() {
        requestReloadASAP(datasetID);
    }

    public static void requestReloadASAP(String tDatasetID) {
        String2.log("EDD.requestReloadASAP " + tDatasetID);
        String2.writeToFile(EDStatic.fullResetFlagDirectory + tDatasetID, tDatasetID);
        EDStatic.tally.add("RequestReloadASAP (since startup)", tDatasetID);
        EDStatic.tally.add("RequestReloadASAP (since last daily report)", tDatasetID);
    }



    /**
     * Given words from a text search query, this returns a ranking of this dataset.
     * Not all words need to be found, but not finding a word incurs a large 
     * penalty.
     * This is a case-insensitve search.
     * This uses a Boyer-Moore-like search (see String2.indexOf(byte[], byte[], int[])).
     *
     * @param words the words or phrases to be searched for (already lowercase)
     *    stored as byte[] via word.getBytes("UTF-8").
     * @param jump the jumpTables from String2.makeJumpTable(word).
     * @return a rating value for this dataset (lower numbers are better),
     *   or Integer.MAX_VALUE if words.length == 0 or 
     *   one of the words wasn't found.
     */
    public int searchRank(byte words[][], int jump[][]) {
        if (words.length == 0)
            return Integer.MAX_VALUE;
        int rank = 0;
        searchString(); //ensure it is available
        //int penalty = 10000;
        for (int w = 0; w < words.length; w++) {
            int po = String2.indexOf(searchString, words[w], jump[w]);

            //word not found
            if (po < 0)
                return Integer.MAX_VALUE;
            rank += po;
            //Exact penalty value doesn't really matter. Any large penalty will
            //  force rankings to be ranked by n words found first, 
            //  then the quality of those found.
            //rank += po < 0? penalty : po;  
        }
        return rank;

        //standardize to 0..1000
        //int rank = Math2.roundToInt((1000.0 * rank) / words.length * penalty);
        //if (rank >= 1000? Integer.MAX_VALUE : rank;
        //return rank;
        
        //return rank == words.length * penalty? Integer.MAX_VALUE : rank;
    }

    /**
     * This returns the flagKey (a String of digits) for the datasetID.
     *
     * @param datasetID
     * @return the flagKey (a String of digits)
     */
    public static String flagKey(String tDatasetID) {
        return Math2.reduceHashCode(
            EDStatic.erddapUrl.hashCode() ^ //always use non-https url
            tDatasetID.hashCode() ^ EDStatic.flagKeyKey.hashCode());
    }

    /**
     * This returns flag URL for the datasetID.
     *
     * @param datasetID
     * @return the url which will cause a flag to be set for a given dataset.
     */
    public static String flagUrl(String tDatasetID) {
        //see also Erddap.doSetDatasetFlag
        return EDStatic.erddapUrl + //always use non-https url
            "/setDatasetFlag.txt?datasetID=" + tDatasetID + 
            "&flagKey=" + flagKey(tDatasetID);
    }

    /**
     * This makes/returns the searchString that searchRank searches.
     * Subclasses may overwrite this.
     *
     * @return the searchString that searchRank searches.
     */
    public abstract byte[] searchString();
    
    protected StringBuilder startOfSearchString() {

        //make a string to search through
        StringBuilder sb = new StringBuilder();
        sb.append("all\n");
        sb.append("title=" + title() + "\n");
        sb.append("datasetID=" + datasetID + "\n");
        //protocol=...  is suggested in Advanced Search for text searches from protocols
        //protocol=griddap and protocol=tabledap *were* mentioned in searchHintsHtml, now commented out
        sb.append("protocol=" + dapProtocol() + "\n");  
        if (EDStatic.sosActive && accessibleViaSOS().length() == 0) 
            sb.append("protocol=SOS\n");
        if (EDStatic.wcsActive && accessibleViaWCS().length() == 0) 
            sb.append("protocol=WCS\n");
        if (accessibleViaWMS().length() == 0) 
            sb.append("protocol=WMS\n");
        for (int dv = 0; dv < dataVariables.length; dv++) {
            sb.append(dataVariables[dv].destinationName() + "\n");
            if (!dataVariables[dv].sourceName().equalsIgnoreCase(dataVariables[dv].destinationName()))
                sb.append(dataVariables[dv].sourceName() + "\n");
            if (!dataVariables[dv].longName().equalsIgnoreCase(dataVariables[dv].destinationName()))
                sb.append(dataVariables[dv].longName() + "\n");
        }
        sb.append(combinedGlobalAttributes.toString() + "\n");
        for (int dv = 0; dv < dataVariables.length; dv++) 
            sb.append(dataVariables[dv].combinedAttributes().toString() + "\n");
        sb.append("className=" + className + "\n");
        return sb;
    }

    /**
     * This returns the types of data files that this dataset can be returned as.
     * These are short descriptive names that are put in the 
     * request url after the dataset name and before the "?", e.g., ".nc". 
     *
     * @return the types of data files that this dataset can be returned as
     *   (e.g., ".nc"). 
     */
    public abstract String[] dataFileTypeNames();

    /**
     * This returns the file extensions corresponding to the dataFileTypes.
     * E.g., dataFileTypeName=".GoogleEarth" returns dataFileTypeExtension=".kml".
     *
     * @return the file extensions corresponding to the dataFileTypes 
     *   (e.g., ".nc").
     */
    public abstract String[] dataFileTypeExtensions();

    /**
     * This returns descriptions (up to 80 characters long, suitable for a tooltip)
     * corresponding to the dataFileTypes. 
     *
     * @return descriptions corresponding to the dataFileTypes.
     */
    public abstract String[] dataFileTypeDescriptions();

    /**
     * This returns an info URL corresponding to the dataFileTypes. 
     *
     * @return an info URL corresponding to the dataFileTypes (an element is "" if not not available)
     */
    public abstract String[] dataFileTypeInfo();

    /**
     * This returns the types of image files that this dataset can be returned 
     * as. These are short descriptive names that are put in the 
     * request url after the dataset name and before the "?", e.g., ".largePng". 
     *
     * @return the types of image files that this dataset can be returned as 
     *   (e.g., ".largePng").
     */
    public abstract String[] imageFileTypeNames();

    /**
     * This returns the file extensions corresponding to the imageFileTypes,
     * e.g., imageFileTypeNames=".largePng" returns imageFileTypeExtensions=".png".
     *
     * @return the file extensions corresponding to the imageFileTypes 
     *   (e.g., ".png").
     */
    public abstract String[] imageFileTypeExtensions();

    /**
     * This returns descriptions corresponding to the imageFileTypes 
     * (each is suitable for a tooltip).
     *
     * @return descriptions corresponding to the imageFileTypes.
     */
    public abstract String[] imageFileTypeDescriptions();

    /**
     * This returns an info URL corresponding to the imageFileTypes. 
     *
     * @return an info URL corresponding to the imageFileTypes.
     */
    public abstract String[] imageFileTypeInfo();

    /**
     * This returns the "[name] - [description]" for all dataFileTypes and imageFileTypes.
     *
     * @return the "[name] - [description]" for all dataFileTypes and imageFileTypes.
     */
    public abstract String[] allFileTypeOptions();
     
    /** 
     * This returns the file extension corresponding to a dataFileType
     * or imageFileType.
     *
     * @param fileTypeName (e.g., ".largePng")
     * @return the file extension corresponding to a dataFileType
     *   imageFileType (e.g., ".png").
     * @throws Throwable if not found
     */
    public String fileTypeExtension(String fileTypeName) throws Throwable {
        //if there is need for speed in the future: use hashmap
        int po = String2.indexOf(dataFileTypeNames(), fileTypeName);
        if (po >= 0)
            return dataFileTypeExtensions()[po];

        po = String2.indexOf(imageFileTypeNames(), fileTypeName);
        if (po >= 0)
            return imageFileTypeExtensions()[po];

        //The pngInfo fileTypeNames could be in regular list, 
        //  but audience is so small, and normal audience might be confused
        if (".smallPngInfo".equals(fileTypeName) ||
            ".pngInfo".equals(fileTypeName) ||
            ".largePngInfo".equals(fileTypeName) ||
            ".smallPdfInfo".equals(fileTypeName) ||
            ".pdfInfo".equals(fileTypeName) ||
            ".largePdfInfo".equals(fileTypeName))
            return ".json";

        throw new SimpleException("Error: fileType=" + fileTypeName + 
                " is not supported by this dataset.");
    }

    /**
     * This returns a suggested fileName (no dir or extension).
     * It doesn't add a random number, so will return the same results 
     * if the inputs are the same.
     *
     * @param loggedInAs is only used for POST datasets (which override EDD.suggestFileName)
     *    since loggedInAs is used by POST for row-by-row authorization
     * @param userDapQuery
     * @param fileTypeName
     * @return a suggested fileName (no dir or extension)
     * @throws Exception if trouble (in practice, it shouldn't)
     */
    public String suggestFileName(String loggedInAs, String userDapQuery, String fileTypeName) {

        //decode userDapQuery to a canonical form to avoid slight differences in percent-encoding 
        try {
            userDapQuery = SSR.percentDecode(userDapQuery);
        } catch (Exception e) {
            //shouldn't happen
        }

        //include fileTypeName in hash so, e.g., different sized .png 
        //  have different file names
        String name = datasetID + "_" + //so all files from this dataset will sort together
            String2.md5Hex12(userDapQuery + fileTypeName); 
        //String2.log("%% suggestFileName=" + name + "\n  from query=" + userDapQuery + "\n  from type=" + fileTypeName);
        return name;
    }

    /**
     * This responds to an OPeNDAP-style query.
     *
     * @param request may be null. If null, no attempt will be made to include 
     *   the loginStatus in startHtmlBody.
     * @param response may be used by .subset to redirect the response
     *   (if not .subset request, it may be null).
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userQuery the part of the user's request after the '?', 
     *   still percentEncoded, may be null.
     * @param outputStreamSource  the source of an outputStream that receives the results,
     *    usually already buffered.
     *     If all goes well, this calls out.close() at the end.
     * @param dir the directory to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.
     * @param fileTypeName the fileTypeName for the new file.
     * @throws Throwable if trouble
     */
    public abstract void respondToDapQuery(HttpServletRequest request, 
        HttpServletResponse response,
        String loggedInAs, String requestUrl, String userQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable;

    /**
     * This responds to a graph query.
     *
     * @param request may be null. If null, no attempt will be made to include 
     *   the loginStatus in startHtmlBody.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param outputStreamSource  the source of an outputStream that receives the results,
     *    usually already buffered.
     * @param dir the directory to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.
     * @param fileTypeName the fileTypeName for the new file.
     * @throws Throwable if trouble
     */
    public abstract void respondToGraphQuery(HttpServletRequest request, 
        String loggedInAs, String requestUrl, String userQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable;

    /**
     * This deletes the old file (if any) and makes a new actual file 
     * based on an OPeNDAP DAP-style query.
     *
     * @param request may be null. If null, no attempt will be made to include 
     *   the loginStatus in startHtmlBody.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param userDapQuery the part of the user's request after the '?'.
     * @param dir the directory that will hold the new file (with a trailing slash).
     * @param fileName the name for the file (no dir, no extension).
     * @param fileTypeName the fileTypeName for the new file.
     * @return fileName + fileExtension for the resulting file
     * @throws Throwable if trouble
     */
    public String makeNewFileForDapQuery(HttpServletRequest request, 
        String loggedInAs, String userDapQuery, 
        String dir, String fileName, String fileTypeName) throws Throwable {

        String fileTypeExtension = fileTypeExtension(fileTypeName);
        File2.delete(dir + fileName + fileTypeExtension);

        return lowMakeFileForDapQuery(request, null, loggedInAs, userDapQuery, 
            dir, fileName, fileTypeName);
    }

    /**
     * This reuses an existing file or makes a new actual file based on an 
     * OPeNDAP DAP-style query.
     *
     * @param request may be null. If null, no attempt will be made to include 
     *   the loginStatus in startHtmlBody.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param userDapQuery the part of the user's request after the '?'.
     * @param dir the directory that will hold the new file (with a trailing slash).
     * @param fileName the name for the file (no dir, no extension).
     * @param fileTypeName the fileTypeName for the new file.
     * @return fileName + fileExtension for the resulting file
     * @throws Throwable if trouble
     */
    public String reuseOrMakeFileForDapQuery(HttpServletRequest request, 
        String loggedInAs, String userDapQuery, 
        String dir, String fileName, String fileTypeName) throws Throwable {

        String fileTypeExtension = fileTypeExtension(fileTypeName);
        String fullName = dir + fileName + fileTypeExtension;
        if (File2.touch(fullName)) {
            if (verbose) String2.log(
                "EDD.makeFileForDapQuery reusing " + fileName + fileTypeExtension);
            return fileName + fileTypeExtension;
        }
        return lowMakeFileForDapQuery(request, null, loggedInAs, userDapQuery, 
            dir, fileName, fileTypeName);
    }

    /**
     * This makes an actual file based on an OPeNDAP DAP-style query
     * and returns its name (not including the dir, but with the extension).
     * This is mostly used for testing since Erddap uses respondToDapQuery directly.
     *
     * <p>This is a default implementation which calls respondToDapQuery.
     * Some classes overwrite this to have this be the main responder
     * (and have respondToDapQuery call this and then copy the file to outputStream).
     * But that approach isn't as good, because it requires all data be obtained and
     * then written to file before response to user can be started.
     *
     * @param request may be null. If null, no attempt will be made to include 
     *   the loginStatus in startHtmlBody.
     * @param response may be used by .subset to redirect the response
     *   (if not .subset request, it may be null).
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param userDapQuery the part of the user's request after the '?'.
     * @param dir the directory that will hold the new file (with a trailing slash).
     * @param fileName the name for the file (no dir, no extension).
     * @param fileTypeName the fileTypeName for the new file.
     * @return fileName + fileExtension
     * @throws Throwable if trouble
     */
    public String lowMakeFileForDapQuery(HttpServletRequest request, 
        HttpServletResponse response,
        String loggedInAs, String userDapQuery, 
        String dir, String fileName, String fileTypeName) throws Throwable {

        String fileTypeExtension = fileTypeExtension(fileTypeName);
        String fullName = dir + fileName + fileTypeExtension;
       
        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        OutputStreamSource outputStreamSource = new OutputStreamSourceSimple(
            (OutputStream)new BufferedOutputStream(new FileOutputStream(fullName + randomInt)));

        try {

            //send the data to the outputStream
            respondToDapQuery(request, response, loggedInAs,
                "/" + EDStatic.warName +
                (this instanceof EDDGrid? "/griddap/" :
                 this instanceof EDDTable? "/tabledap/" :
                 "/UNKNOWN/") + 
                 datasetID + fileTypeName, 
                userDapQuery, outputStreamSource, dir, fileName, fileTypeName);

            //close the outputStream
            outputStreamSource.outputStream("").close();
        } catch (Throwable t) {
            try {
                outputStreamSource.outputStream("").close();
            } catch (Throwable t2) {
                //don't care
            }
            //delete the temporary file
            File2.delete(fullName + randomInt);
            throw t;
        }

        //rename the file to the specified name
        File2.rename(fullName + randomInt, fullName);
        return fileName + fileTypeExtension;
    }

    /**
     * This writes the dataset info (id, title, institution, infoUrl, summary)
     * to an html document.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param writer
     * @param showDafLink if true, a link is shown to this dataset's Data Access Form
     * @param showSubsetLink if true, a link is shown to this dataset's .subset form
     *    (if accessibleViaSubset() is "").
     * @param showGraphLink if true, a link is shown to this dataset's Make A Graph form
     *    (if accessibleViaMAG() is "").
     * @param userDapQuery  the part of the user's request after the '?', still percentEncoded, may be null.
     * @param otherRows  additional html content
     * @throws Throwable if trouble
     */
    public void writeHtmlDatasetInfo(
        String loggedInAs, Writer writer, 
        boolean showSubsetLink, boolean showDafLink, boolean showGraphLink,
        String userDapQuery, String otherRows) 
        throws Throwable {
        //String type = this instanceof EDDGrid? "Gridded" :
        //   this instanceof EDDTable? "Tabular" : "(type???)";
        
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String tQuery = userDapQuery == null || userDapQuery.length() == 0? "" :
            //since this may be direct from user, I need to XML encode it 
            //to prevent HTML insertion security vulnerability
            //(which allows hacker to insert his javascript into pages returned by server)
            //See Tomcat (Definitive Guide) pg 147...
            "?" + XML.encodeAsHTML(userDapQuery); 
        String dapLink = "", subsetLink = "", graphLink = "";
        if (showDafLink) 
            dapLink = 
                "     | <a title=\"Click to see an OPeNDAP Data Access Form for this data set.\" \n" +
                "         href=\"" + tErddapUrl + "/" + dapProtocol() + "/" + datasetID + ".html" + 
                    tQuery + "\">Data Access Form</a>\n";
        if (showSubsetLink && accessibleViaSubset().length() == 0) 
            subsetLink = 
                "     | <a title=\"Click to see a Subset form for this data set.\" \n" +
                "         href=\"" + tErddapUrl + "/" + dapProtocol() + "/" + datasetID + ".subset" + 
                    tQuery + 
                    (tQuery.length() == 0? "" : XML.encodeAsHTML(EDDTable.DEFAULT_SUBSET_VIEWS)) + 
                    "\">Subset</a>\n";
        if (showGraphLink && accessibleViaMAG().length() == 0) 
            graphLink = 
                "     | <a title=\"Click to see a Make A Graph form for this data set.\" \n" +
                "         href=\"" + tErddapUrl + "/" + dapProtocol() + "/" + datasetID + ".graph" + 
                    tQuery + "\">Make A Graph</a>\n";
        String tSummary = extendedSummary(); 
        String tLicense = combinedGlobalAttributes().getString("license");
        boolean nonStandardLicense = tLicense != null && !tLicense.equals(EDStatic.standardLicense);
        tLicense = tLicense == null? "" :
            "    | " +
            (nonStandardLicense? "<font class=\"warningColor\">" : "") + 
            "License " + 
            (nonStandardLicense? "</font>" : "") +
            EDStatic.htmlTooltipImage(loggedInAs, XML.encodeAsPreHTML(tLicense, 100)) +  "\n";
        writer.write(
            //"<p><b>" + type + " Dataset:</b>\n" +
            "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
            "  <tr>\n" +
            "    <td nowrap valign=\"top\">Dataset Title:&nbsp;</td>\n" +
            "    <td><font class=\"standoutColor\"><big><b>" + XML.encodeAsHTML(title()) + "</b></big></font>\n" +
            "      " + emailHref(loggedInAs) + "\n" +
            "      " + rssHref(loggedInAs) + "\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap valign=\"top\">Institution:&nbsp;</td>\n" +
            "    <td>" + XML.encodeAsHTML(institution()) + "&nbsp;&nbsp;\n" +
            "    (Dataset ID: " + XML.encodeAsHTML(datasetID) + ")</td>\n" +
            "  </tr>\n" +
            otherRows +
            "  <tr>\n" +
            "    <td nowrap valign=\"top\">Information:&nbsp;</td>\n" +
            "    <td>Summary " + 
                EDStatic.htmlTooltipImage(loggedInAs, XML.encodeAsPreHTML(tSummary, 100)) +  "\n" +
            tLicense +
            "     | <a title=\"" + EDStatic.clickInfo + "\" \n" +
            "          href=\"" + tErddapUrl + "/info/" + datasetID + "/index.html\">Metadata</a>\n" +
            "     | <a title=\"" + EDStatic.clickBackgroundInfo + "\" \n" +
            "         href=\"" + XML.encodeAsHTML(infoUrl()) + "\">Background</a>\n" +
                subsetLink + "\n" +
                dapLink + "\n" +
                graphLink + "</td>\n" +
            "  </tr>\n" +
            "</table>\n");
    }

    /**
     * This returns the kml code for the screenOverlay (which is the KML code
     * which describes how/where to display the googleEarthLogoFile).
     * This is used by EDD subclasses when creating KML files.
     *
     * @return the kml code for the screenOverlay.
     */
    public String getKmlIconScreenOverlay() {
        return 
            "  <ScreenOverlay id=\"Logo\">\n" + //generic id
            "    <description>" + EDStatic.erddapUrl + //always use non-https url
                "</description>\n" +
            "    <name>Logo</name>\n" + //generic name
            "    <Icon>" +
                  "<href>" + EDStatic.imageDirUrl + //always use non-https url
                EDStatic.googleEarthLogoFile + "</href>" +
                "</Icon>\n" +
            "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
            "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
            "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" + //0=original size
            "  </ScreenOverlay>\n";
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to make
     * sure that the global attributes includes at least place holders (dummy values)
     * for the required/common global attributes.
     *
     * @param sourceAtts   must not be null
     * @param tCdmDataType can be a specific type (e.g., "Grid") or null (for the default list)
     * @param tLocalSourceUrl a real URL (starts with "http"), a fileDirectory (with or without trailing '/'), 
     *     or a fake fileDirectory (not ideal).
     */
    public static void addDummyRequiredGlobalAttributesForDatasetsXml(Attributes sourceAtts, 
        String tCdmDataType, String tLocalSourceUrl) {

        //get the readyToUseAddGlobalAttributes for suggestions
        Attributes addAtts = makeReadyToUseAddGlobalAttributesForDatasetsXml(sourceAtts, 
            tCdmDataType, tLocalSourceUrl, null);
        String aConventions  = addAtts.getString("Conventions");
        String aInfo         = addAtts.getString("infoUrl"); 
        String aIns          = addAtts.getString("institution");
        String aMConventions = addAtts.getString("Metadata_Conventions");

        String name = "cdm_data_type";
        String value = sourceAtts.getString(name);
        if (!isSomething(value))            
            sourceAtts.add(name, tCdmDataType == null? 
                "???" + String2.toSVString(CDM_TYPES, "|", false) : 
                tCdmDataType);

        name = "Conventions";
        value = sourceAtts.getString(name); 
        if (isSomething(aConventions) && !aConventions.equals(value))  
            sourceAtts.add(name, (value == null? "" : value) + "???" + aConventions);

        name = "Metadata_Conventions";
        value = sourceAtts.getString(name); 
        if (isSomething(aMConventions) && !aMConventions.equals(value))  
            sourceAtts.add(name, (value == null? "" : value) + "???" + aMConventions);

        //for all of the rest, if something already there, don't suggest anything 
        name = "infoUrl";
        value = sourceAtts.getString(name); 
        if (!isSomething(value)) 
            sourceAtts.add(name, "???" + (aInfo == null || aInfo.equals("???")? "" : aInfo));

        name = "institution";
        value = sourceAtts.getString(name); 
        if (!isSomething(value)) 
            sourceAtts.add(name, "???" + (aIns == null || aIns.equals("???")? "" : aIns));

        name = "license";
        value = sourceAtts.getString(name); 
        if (!isSomething(value)) 
            sourceAtts.add(name, "???" + addAtts.getString(name));

        name = "standard_name_vocabulary";
        value = sourceAtts.getString(name); 
        if (!isSomething(value)) 
            sourceAtts.add(name, "???" + FileNameUtility.getStandardNameVocabulary());

        name = "summary";
        value = sourceAtts.getString(name); 
        if (!isSomething(value)) 
            sourceAtts.add(name, "???" + addAtts.getString(name));

        name = "title";
        value = sourceAtts.getString(name); 
        if (!isSomething(value)) 
            sourceAtts.add(name, "???" + addAtts.getString(name));
    }

    /**
     * This is used by generateDatasetsXml to find out if a table probably
     * has longitude, latitude, and time variables.
     *
     * @param table
     * @return true if it probably does
     */
    public static boolean probablyHasLonLatTime(Table table) {
        boolean hasLon = false, hasLat = false, hasTime = false; 
        for (int col = 0; col < table.nColumns(); col++) {
            String colName = table.getColumnName(col);
            Attributes atts = table.columnAttributes(col);
            boolean hasTimeUnits = EDVTimeStamp.hasTimeUnits(atts, null);
            String units = atts.getString("units");
            if (colName.equals("lon") || colName.equals("longitude") || 
                "degrees_east".equals(units)) 
                hasLon = true;
            if (colName.equals("lat") || colName.equals("latitude") || 
                "degrees_north".equals(units)) 
                hasLat = true;
            if (colName.equals("time") || hasTimeUnits)
                hasTime = true;
        }
        return hasLon && hasLat && hasTime;
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to make
     * sure that the global attributes are present (as good as possible; no dummy values)
     * for the required/common global attributes.
     *
     * @param sourceAtts   must not be null
     * @param tCdmDataType a specific type (e.g., "Grid"); if null, the attribute won't be added
     * @param tLocalSourceUrl a real local URL (starts with "http"), a fileDirectory (with or without trailing '/'), 
     *     or a fake fileDirectory (not ideal).
     *     If an OPeNDAP url, it is without the .das, .dds, or .html extension.
     * @param externalAtts globalAtts from another source, or null.
     *    They have priority over sourceAtts.
     *    But title is special: sourceAtts title &gt; 20 characters has priority.
     *    <br>&lt;suffixForTitle&gt; is special attribute with text to be appended to title in parentheses.
     *    It is removed from addAttributes at end of method.
     */
    public static Attributes makeReadyToUseAddGlobalAttributesForDatasetsXml(Attributes sourceAtts, 
        String tCdmDataType, String tLocalSourceUrl, Attributes externalAtts) {

//TO DO: look at other metadata standards (e.g., FGDC) to find similar attributes to look for
//fgdc: http://docs.google.com/viewer?a=v&q=cache:jqwVIfleOYoJ:portal.opengeospatial.org/files/%3Fartifact_id%3D16936+%22fgdc:title%22&hl=en&gl=us&pid=bl&srcid=ADGEESjCZAzZzsRrGP0bxE3vj2qf3e7UAtL0O9C7M6Vm9JSvkuaW74nBYChLJdQagIf0X0vm-0_qgAHUanv6WqhNu59ouFV4i3-wD-nzfUBmRg4npV2wrCrc2RIJ8Q7El65RjHCZiqzU&sig=AHIEtbRqR8ld45spO4SqD7nIYV2de1FGow

        if (reallyVerbose) 
            String2.log("makeReadyToUseAddGlobalAttributesForDatasetsXml\n" +
                //"  sourceAtts=\n" + sourceAtts.toString() +
                "  sourceAtts.title=" + sourceAtts.getString("title") + "\n" +
                "  externalAtts=" +
                (externalAtts == null? "null" : "\n" + externalAtts.toString()));
        String name, value;
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
        boolean sourceUrlIsThreddsCatalogXml = 
            tPublicSourceUrl.startsWith("http") &&
            tPublicSourceUrl.indexOf("/thredds/catalog/") > 0 &&  
            tPublicSourceUrl.endsWith(".xml");

        //Use externalAtts as initial addAtts. They have priority over sourceAtts.
        Attributes addAtts = externalAtts == null? new Attributes() : (Attributes)externalAtts.clone();

        //convert fgdc_ sourceAtts to addAtts (if null)        
        //e.g. http://www.ngdc.noaa.gov/thredds/dodsC/ustec/tec/200609030400_tec.nc.das uses '_'
        //fgdc_metadata_url is fgdc metadata, so not so useful as infoUrl
        addAtts.setIfNotAlreadySet("abstract",            sourceAtts.getString("fgdc_abstract"));
        addAtts.setIfNotAlreadySet("abstract",            sourceAtts.getString("fgdc:abstract"));
        addAtts.setIfNotAlreadySet("comment",             sourceAtts.getString("fgdc_comment"));
        addAtts.setIfNotAlreadySet("comment",             sourceAtts.getString("fgdc:comment"));
        addAtts.setIfNotAlreadySet("creator_email",       sourceAtts.getString("fgdc_creator_email"));
        addAtts.setIfNotAlreadySet("creator_email",       sourceAtts.getString("fgdc:creator_email"));
        addAtts.setIfNotAlreadySet("creator_name",        sourceAtts.getString("fgdc_creator_name"));
        addAtts.setIfNotAlreadySet("creator_name",        sourceAtts.getString("fgdc:creator_name"));
        addAtts.setIfNotAlreadySet("creator_url",         sourceAtts.getString("fgdc_creator_url"));
        addAtts.setIfNotAlreadySet("creator_url",         sourceAtts.getString("fgdc:creator_url"));
        addAtts.setIfNotAlreadySet("id",                  sourceAtts.getString("fgdc_id"));
        addAtts.setIfNotAlreadySet("id",                  sourceAtts.getString("fgdc:id"));
        addAtts.setIfNotAlreadySet("infoUrl",             sourceAtts.getString("fgdc_onlink"));
        addAtts.setIfNotAlreadySet("infoUrl",             sourceAtts.getString("fgdc:onlink"));
        addAtts.setIfNotAlreadySet("institution",         sourceAtts.getString("fgdc_institution"));
        addAtts.setIfNotAlreadySet("institution",         sourceAtts.getString("fgdc:institution"));
        addAtts.setIfNotAlreadySet("keywords",            sourceAtts.getString("fgdc_keywords"));
        addAtts.setIfNotAlreadySet("keywords",            sourceAtts.getString("fgdc:keywords"));
        addAtts.setIfNotAlreadySet("keywords_vocabulary", sourceAtts.getString("fgdc_keywords_vocabulary"));
        addAtts.setIfNotAlreadySet("keywords_vocabulary", sourceAtts.getString("fgdc:keywords_vocabulary"));
        addAtts.setIfNotAlreadySet("license",             sourceAtts.getString("fgdc_license"));
        addAtts.setIfNotAlreadySet("license",             sourceAtts.getString("fgdc:license"));
        addAtts.setIfNotAlreadySet("project",             sourceAtts.getString("fgdc_project"));
        addAtts.setIfNotAlreadySet("project",             sourceAtts.getString("fgdc:project"));
        addAtts.setIfNotAlreadySet("publisher_email",     sourceAtts.getString("fgdc_publisher_email"));
        addAtts.setIfNotAlreadySet("publisher_email",     sourceAtts.getString("fgdc:publisher_email"));
        addAtts.setIfNotAlreadySet("publisher_name",      sourceAtts.getString("fgdc_publisher_name"));
        addAtts.setIfNotAlreadySet("publisher_name",      sourceAtts.getString("fgdc:publisher_name"));
        addAtts.setIfNotAlreadySet("publisher_url",       sourceAtts.getString("fgdc_publisher_url"));
        addAtts.setIfNotAlreadySet("publisher_url",       sourceAtts.getString("fgdc:publisher_url"));
        addAtts.setIfNotAlreadySet("summary",             sourceAtts.getString("fgdc_summary"));
        addAtts.setIfNotAlreadySet("summary",             sourceAtts.getString("fgdc:summary"));
        //fgdc_title see below


        //*** populate the attributes that ERDDAP uses
        //always check wrong case for first letter
        //e.g., http://aqua.smast.umassd.edu:8080/thredds/dodsC/models/PE_SHELF_ASS_2009318_1_1.nc.das

        name = "cdm_data_type";
        if (sourceAtts.getString(name) == null &&
               addAtts.getString(name) == null &&
            tCdmDataType != null) 
            addAtts.add(name, tCdmDataType);            

        //Conventions
        name = "Conventions";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
                                     value =    addAtts.getString("conventions"); //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("conventions"); //wrong case?
            if (isSomething(value)) addAtts.set(                  "conventions", "null"); //not just remove()
        }
        if (!isSomething(value)) value =    addAtts.getString("Metadata_Conventions"); 
        if (!isSomething(value)) value = sourceAtts.getString("Metadata_Conventions"); 
        if (reallyVerbose) String2.log("  old Conventions=" + value);
        value = suggestConventions(value);
        addAtts.set(name, value);  //always reset Conventions
        if (reallyVerbose) String2.log("  new Conventions=" + value);

        //always reset Metadata_Conventions (synomym for Conventions)
        addAtts.add("Metadata_Conventions", value);
         
        //history
        name = "history";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            if (!isSomething(value)) value =    addAtts.getString("History"); //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("History");
            if ( isSomething(value))                  addAtts.set("History", "null"); //not just remove()
            if (!isSomething(value)) value =    addAtts.getString("mac_history"); 
            if (!isSomething(value)) value = sourceAtts.getString("mac_history");

            if (!isSomething(value)) value =    addAtts.getString("source");
            if (!isSomething(value)) value = sourceAtts.getString("source");

            //there doesn't have to be a history attribute
            if (isSomething(value)) 
                addAtts.add(name, value);
        }


        //infoUrl
        name = "infoUrl";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (isSomething(value)) {
            //deal with ssv or csv list
            int po = value.indexOf(' ');
            if (po >= 0) value = value.substring(0, po);
            po = value.indexOf(',');
            if (po >= 0) value = value.substring(0, po);
            addAtts.add(name, value);
        }
        if (!isSomething(value)) {
            if (!isSomething(value)) value =    addAtts.getString("onlink");
            if (!isSomething(value)) value = sourceAtts.getString("onlink");
            if (!isSomething(value)) value =    addAtts.getString("Onlink");
            if (!isSomething(value)) value = sourceAtts.getString("Onlink");
            if (!isSomething(value)) value =    addAtts.getString("url");
            if (!isSomething(value)) value = sourceAtts.getString("url");
            if (!isSomething(value)) value =    addAtts.getString("URL");
            if (!isSomething(value)) value = sourceAtts.getString("URL");
            if (!isSomething(value)) value =    addAtts.getString("html_BACKGROUND");            
            if (!isSomething(value)) value = sourceAtts.getString("html_BACKGROUND");            
            if (isSomething(value)) {
                //deal with ssv or csv list
                int po = value.indexOf(' ');
                if (po >= 0) value = value.substring(0, po);
                po = value.indexOf(',');
                if (po >= 0) value = value.substring(0, po);
            }
            if (!isSomething(value)) {
                if (tPublicSourceUrl.startsWith("http")) {
                    if (sourceUrlIsThreddsCatalogXml) {  //thredds catalog.xml -> .html
                        value = tPublicSourceUrl.substring(0, tPublicSourceUrl.length() - 4) + ".html";
                    } else {
                        value = tPublicSourceUrl + 
                            ((tPublicSourceUrl.indexOf("/thredds/") > 0 ||       //most DAP servers, add .html:  THREDDS
                              tPublicSourceUrl.indexOf("/dodsC/") > 0 ||
                              tPublicSourceUrl.indexOf("dap/") > 0)? ".html" :   //  and opendap/, tabledap/, griddap/, 
                            tPublicSourceUrl.indexOf("/dapper/") > 0? ".das" : //for DAPPER, add .das (no .html)
                            ""); 
                    }
                } else { 
                    if (!isSomething(value)) value =    addAtts.getString("creator_url"); //better than "???"
                    if (!isSomething(value)) value = sourceAtts.getString("creator_url");
                    if (!isSomething(value)) value = "???";
                }
            }
            addAtts.add(name, value);
            if (reallyVerbose) String2.log("  new infoUrl=" + value);
        }
        String infoUrl = value;

        //institution
        name = "institution";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value) || value.length() > 20) {
            if (!isSomething(value)) {
                value                          =    addAtts.getString("Institution"); //wrong case?
                if (!isSomething(value)) value = sourceAtts.getString("Institution");
                if ( isSomething(value)) addAtts.set("Institution", "null"); //not just remove()
            }
            if (!isSomething(value)) value = sourceAtts.getString("origin");


            if (isSomething(value)) {
                //too long?
                if (value.length() > 20) {
                    String original = sourceAtts.getString(name);

                    //special cases
                    if (value.equals("NOAA CoastWatch, West Coast Node"))
                        value = "NOAA CoastWatch WCN";
                    else if (value.equals("NOAA National Climatic Data Center"))
                        value = "NOAA NCDC";
                    else if (value.equals("DOC/NOAA/NESDIS/NGDC > National Geophysical Data Center, " +
                        "NESDIS, NOAA, U.S. Department of Commerce"))
                        value = "NOAA NGDC";
                    else if (value.equals("DOC/NOAA/NESDIS/OSDPD > Office of Satellite Data " +
                        "Processing and Distribution, NESDIS, NOAA, U.S. Department of Commerce"))
                        value = "NOAA OSDPD";
                    else if (value.equals("USDOC/NOAA/NESDIS CoastWatch"))
                        value = "NOAA CoastWatch";
                    else if (value.equals("National Data Buoy Center"))
                        value = "NOAA NDBC";
                    else if (value.equals("Scripps Institution of Oceanography"))
                        value = "Scripps";

                    //parentheses?  keep shorter of in or out text
                    StringBuilder newValue = new StringBuilder();
                    while (value.length() > 0) {
                        int ppo1 = value.indexOf('(');
                        int ppo2 = value.indexOf(')');
                        if (ppo1 <= 0 || ppo2 <= ppo1)  //no paren
                            break;
                        String out = value.substring(0, ppo1).trim();
                        String in  = value.substring(ppo1 + 1, ppo2);
                        newValue.append(out.length() < in.length()? out : in);
                        value = value.substring(ppo2 + 1).trim();

                        if (value.startsWith(". ") ||
                            value.startsWith(", ") ||
                            value.startsWith("; ")) {
                            newValue.append(value.substring(0, 2));
                            value = value.substring(2);
                        } else if (value.startsWith("and ")) {
                            newValue.append(" " + value.substring(0, 4));
                            value = value.substring(4);
                        } else {
                            newValue.append(' ');
                        }
                    }
                    newValue.append(value); //the remainder
                    value = newValue.toString().trim();

                    if (!value.equals(original)) 
                        addAtts.set("original_institution", original);
                }
            } else {
                //find a related url
                value = tPublicSourceUrl;
                if (!isSomething(value) || !value.startsWith("http")) value = infoUrl; //includes creator_url
                if (!isSomething(value) || !value.startsWith("http")) value =    addAtts.getString("publisher_url");
                if (!isSomething(value) || !value.startsWith("http")) value = sourceAtts.getString("publisher_url");
                if (isSomething(value) && value.startsWith("http"))
                    value = suggestInstitution(value);  //now NOAA PFEL
                else value = "???";
            }

            addAtts.add(name, value);
            if (reallyVerbose) String2.log("  new institution=" + value);
        }
        String tIns = value;

        //license
        name = "license";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            if (!isSomething(value)) value =    addAtts.getString("License");  //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("License");
            if ( isSomething(value))                  addAtts.set("License", "null"); //not just remove()

            //e.g., http://michigan.glin.net:8080/thredds/dodsC/glos/glcfs/michigan/ncasf_his3d/Lake_Michigan_Forecast_Collection_3D_fmrc.ncd.html
            if (!isSomething(value)) value = sourceAtts.getString("disclaimer"); 
            if (!isSomething(value)) value = sourceAtts.getString("Disclaimer"); 
            //A lot of ex-military datasets have distribution_statement:
            //e.g., http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/region1/ncom_glb_reg1_2010013000.nc.das
            if (!isSomething(value)) value = sourceAtts.getString("distribution_statement"); 
            if (!isSomething(value)) value = "[standard]"; //if nothing else
            addAtts.add(name, value);
        }

        name = "standard_name_vocabulary";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) addAtts.add(name, FileNameUtility.getStandardNameVocabulary());

        //note suffixForTitle //e.g. Thredds currentLevel name: "5-day" or "sal001", sometimes long
        String suffixForTitle = addAtts.getString("suffixForTitle");  
        if (!isSomething(suffixForTitle))
            suffixForTitle = "";
        String suffixForTitle2 = suffixForTitle;
        suffixForTitle2 = String2.replaceAll(suffixForTitle2, '/', ' ');
        suffixForTitle2 = String2.replaceAll(suffixForTitle2, '_', ' ');
        String suffixForTitleP = isSomething(suffixForTitle)? " (" + suffixForTitle + ")" : "";

        //summary
        boolean tSummaryFromSourceUrl = false;
        name = "summary";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            //best ones first
            if (!isSomething(value)) value =    addAtts.getString("Summary");  //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("Summary");
            if ( isSomething(value))                  addAtts.add("Summary", "null"); //not just remove()

            if (!isSomething(value)) value =    addAtts.getString("abstract"); 
            if (!isSomething(value)) value = sourceAtts.getString("abstract"); 
            if (!isSomething(value)) value =    addAtts.getString("Abstract"); 
            if (!isSomething(value)) value = sourceAtts.getString("Abstract"); 
            if (!isSomething(value)) value =    addAtts.getString("description");
            if (!isSomething(value)) value = sourceAtts.getString("description");
            if (!isSomething(value)) value =    addAtts.getString("Description");
            if (!isSomething(value)) value = sourceAtts.getString("Description");
            if (!isSomething(value)) value =    addAtts.getString("comment");
            if (!isSomething(value)) value = sourceAtts.getString("comment");
            if (!isSomething(value)) value =    addAtts.getString("Comment");
            if (!isSomething(value)) value = sourceAtts.getString("Comment");
            //from title metadata?    priority to source if >20 chars
            if (!isSomething(value)) {
                value = "";
                String sTitle = sourceAtts.getString("title");
                String aTitle =    addAtts.getString("title");
                if (!isSomething(aTitle)) aTitle =     addAtts.getString("Title");          //next best aTitle
                if (!isSomething(sTitle)) sTitle =  sourceAtts.getString("Title");          

                //use both sourceTitle and addTitle (if available and not the same)
                String pre = "", post = "";
                if (isSomething(sTitle)) value = sTitle;
                if (isSomething(sTitle) && isSomething(aTitle)) {
                    if (sTitle.toLowerCase().equals(aTitle.toLowerCase())) {
                        aTitle = "";
                    } else {
                        pre = "\n(";
                        post = ")";
                    }
                }
                if (isSomething(aTitle)) 
                    value += pre + aTitle + post;

                //if suffixForTitle's text isn't already included, add it
                if (isSomething(suffixForTitle) && 
                    value.toLowerCase().indexOf(suffixForTitle.toLowerCase()) < 0 &&
                    value.toLowerCase().indexOf(suffixForTitle2.toLowerCase()) < 0)
                    value += suffixForTitleP;
            }
            //if nothing else
            if (!isSomething(value)) {
                tSummaryFromSourceUrl = true;
                value = tIns.equals("???")? "Data " : tIns + " data ";
                if (sourceUrlIsThreddsCatalogXml)  //thredds catalog.xml -> .html
                     value += "from " + tPublicSourceUrl.substring(0, tPublicSourceUrl.length() - 4) + ".html";
                else if (tPublicSourceUrl.startsWith("http"))
                     value += "from " + tPublicSourceUrl + ".das ."; 
                else value += "from a local source.";
                value += suffixForTitleP;
            }
            addAtts.add(name, value);
        }
        String tSummary = value;

        //title
        name = "title";
        {
            value = sourceAtts.getString(name); 
            if (!isSomething(value)) value = sourceAtts.getString("fgdc_title");
            if (!isSomething(value)) value = sourceAtts.getString("fgdc:title");
            if (!isSomething(value)) value =    addAtts.getString("Title");  //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("Title");
            //if ( isSomething(value))              addAtts.set("Title", "null"); //not just remove()

            //use source title and/or external addAtts title 
            value                = removeExtensionsFromTitle(value);
            String externalTitle = removeExtensionsFromTitle(addAtts.getString(name));
            //is one within the other?  set it to ""
            if (value.length() > 0 && externalTitle.length() > 0) {
                String et2 = String2.replaceAll(externalTitle.toLowerCase(), '_', ' ');
                et2        = String2.replaceAll(et2, '/', ' ');
                if (value.toLowerCase().indexOf(et2) >= 0) externalTitle = "";
                if (et2.indexOf(value.toLowerCase()) >= 0) value = "";
            }
            //choose to use one or the other or both
            if      (value.length() == 0) value = externalTitle;
            else if (externalTitle.length() == 0) value = value;
            else if (value.length() < 25 || externalTitle.length() < 25 ||
                     (value.length() + externalTitle.length()) < 77)
                value += " (" + externalTitle + ")";
            //for fromThreddsCatalog, sourceTitle + suffixForTitle is good combo
            else if (isSomething(suffixForTitle) && 
                     (value.length() + suffixForTitle.length() < 77))
                value += suffixForTitleP;
            //for fromThreddsCatalog, it won't be supplemented by suffixForTitle below 
            else value = externalTitle; //but most likely to have distinctive info

            //get from summary-like metadata? 
            if (!isSomething(value) && !tSummaryFromSourceUrl) {
                value = tSummary;
                if (isSomething(value) && value.length() > 80) 
                    value = value.substring(0, 76) + " ...";
            }

            //thredds catalog.xml?   use last two directory names
            if (!isSomething(value) && sourceUrlIsThreddsCatalogXml) {
                value = tPublicSourceUrl;
                int po = value.lastIndexOf("/");
                if (po > 0) 
                    value = value.substring(0, po);
                po = value.lastIndexOf("/");
                if (po > 0) po = value.substring(0, po).lastIndexOf("/");
                value = po > 0 && po < value.length() - 1? 
                    value.substring(po + 1) : 
                    value;
                value = String2.replaceAll(value, '/', ' ');
                value = String2.replaceAll(value, '_', ' ');
            }

            //if nothing else
            if (!isSomething(value)) {
                //use last part of url or directory, e.g.,   2010/KH20060101_ab
                //if dir, remove trailing slash
                value = !tPublicSourceUrl.startsWith("http") && tPublicSourceUrl.endsWith("/")? 
                    tPublicSourceUrl.substring(0, tPublicSourceUrl.length() - 1) : 
                    tPublicSourceUrl;
                int po = value.lastIndexOf("/");
                if (po > 0) po = value.substring(0, po).lastIndexOf("/");
                value = po > 0 && po < value.length() - 1? 
                    value.substring(po + 1) : 
                    value;
                if (value.startsWith("dodsC/"))
                    value = value.substring(6);
                value = removeExtensionsFromTitle(value); //e.g., .grib
                value = String2.replaceAll(value, '/', ' ');
                value = String2.replaceAll(value, '_', ' ');
            }

            //make sure 1st char is capitalized
            if (isSomething(value)) //it should be
                value = Character.toUpperCase(value.charAt(0)) + value.substring(1);

            //save the original_title if different
            String oTitle = sourceAtts.getString(name); 
            if (isSomething(oTitle) && value.indexOf(oTitle) < 0)
                addAtts.add("original_title", oTitle);

            //if suffixForTitle's text isn't in the title, add it
            if (isSomething(suffixForTitle) && 
                value.toLowerCase().indexOf(suffixForTitle.toLowerCase()) < 0 &&
                value.toLowerCase().indexOf(suffixForTitle2.toLowerCase()) < 0)
                value += suffixForTitleP;
            addAtts.add(name, value);
        }

        //remove atts which are already in sourceAtts
        addAtts.removeIfSame(sourceAtts);
        addAtts.remove("suffixForTitle");

        return addAtts;
    }

    /**
     * This removes common extensions from dataset titles (if present).
     *
     * @param tTitle   if !isSomething, this returns "".
     */
    public static String removeExtensionsFromTitle(String tTitle) {
        //there are some ending in .grb.nc, so do .nc first
        //similar code in EDDGridFromDap.generateDatasetsXmlFromThreddsCatalog
        if (!isSomething(tTitle))
            return "";
        if (tTitle.endsWith(".nc"))
            tTitle = tTitle.substring(0, tTitle.length() - 3);
        if (tTitle.endsWith(".cdf") || tTitle.endsWith(".cdp") || 
            tTitle.endsWith(".grb") || tTitle.endsWith(".grd"))
            tTitle = tTitle.substring(0, tTitle.length() - 4);
        if (tTitle.endsWith(".grib"))
            tTitle = tTitle.substring(0, tTitle.length() - 5);
        return tTitle;
    }


    /** This returns true if s isn't null, "", "-", or "...". */
    public static boolean isSomething(String s) {
        //Some datasets have "" for an attribute.

        //Some datasets have comment="..." ,e.g.,
        //http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/region1/ncom_glb_reg1_2010013000.nc.das
        //which then prevents title from being generated

        //some have "-", e.g.,
        //http://dm1.caricoos.org/thredds/dodsC/content/wrf_archive/wrfout_d01_2009-09-25_12_00_00.nc.das

        return !(s == null || s.equals("") || s.equals("-") || s.equals("..."));
    }



    /**
     * This is used by subclass's generateDatasetsXml methods to make
     * sure that a variable's attributes includes at least place holders (dummy values)
     * for the required/common attributes.
     *
     * @param tSourceAtts
     * @param tSourceName
     */
    public static void addDummyRequiredVariableAttributesForDatasetsXml(
        Attributes tSourceAtts, String tSourceName, boolean addColorBarMinMax) {

        //get the readyToUseAddVariableAttributes for suggestions
        Attributes tAddAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
            tSourceAtts, tSourceName, addColorBarMinMax);

        if (addColorBarMinMax) {
            if (tSourceAtts.getString("colorBarMinimum") == null) 
                      tSourceAtts.add("colorBarMinimum", 
                   tAddAtts.getDouble("colorBarMinimum"));
            if (tSourceAtts.getString("colorBarMaximum") == null) 
                      tSourceAtts.add("colorBarMaximum", 
                   tAddAtts.getDouble("colorBarMaximum"));
        }

        String names[] = {"ioos_category", "long_name", "standard_name", "units"};
        for (int i = 0; i < 4; i++) {
            String sValue = tSourceAtts.getString(names[i]);
            String aValue = tAddAtts.getString(names[i]);
            if (sValue == null) sValue = "";
            if (aValue == null) aValue = "";
            //String2.log("  name=" + names[i] + " sValue=" + sValue + " aValue=" + aValue);
            //special case: only suggest ioos_category if required
            if (i == 0 && !EDStatic.variablesMustHaveIoosCategory)
                continue;
            //special case: see if a different long_name is suggested (give admin a choice)
            //because makeReadyToUse won't change an existing long_name
            if (i == 1 && aValue.length() == 0) {
                String tStandardName = tAddAtts.getString("standard_name");
                if (tStandardName == null)
                    tStandardName = tSourceAtts.getString("standard_name");
                aValue = EDV.suggestLongName(sValue, tSourceName, tStandardName);
            }
            if (!isSomething(sValue) || 
                (isSomething(aValue) && !aValue.equals(sValue))) 
                tSourceAtts.add(names[i], sValue + "???" + (aValue.equals("???")? "" : aValue));
        }

    }

    /**
     * This is used by generateDatasetsXml methods to add as many 
     * variable attributes as possible based on source information.
     *
     * @param tSourceAtts
     * @param tSourceName   
     * @param tryToAddColorBarMinMax
     * @return tAddAdds for the variable
     */
    public static Attributes makeReadyToUseAddVariableAttributesForDatasetsXml(
        Attributes tSourceAtts, String tSourceName, boolean tryToAddColorBarMinMax) {

        String lcSourceName = tSourceName.toLowerCase();
        Attributes tAddAtts = new Attributes();
        //oXxx are values from sourceAtts
        String oLongName     = tSourceAtts.getString("long_name");
        String oStandardName = tSourceAtts.getString("standard_name");
        String oUnits        = tSourceAtts.getString("units");
        if (oLongName       == null) oLongName     = "";
        if (oStandardName   == null) oStandardName = "";
        if (oUnits          == null) oUnits        = "";
        //tXxx are current best value
        String tLongName     = oLongName;
        String tStandardName = oStandardName;
        String tUnits        = oUnits;

        //longitude, latitude, altitude, time are special cases
        String tDestName = suggestDestinationName(tSourceName, tUnits, 
            tSourceAtts.getFloat("scale_factor"), true);
        if (tDestName.equals("longitude")) {
            tLongName = "Longitude";
            tStandardName = "longitude";
            tUnits = "degrees_east";
        }
        if (tDestName.equals("latitude")) {
            tLongName = "Latitude";
            tStandardName = "latitude";
            tUnits = "degrees_north";
        }
        if (tDestName.equals("altitude")) {
            //let tLongName be set below
            tStandardName = "altitude";
        }
        if (tDestName.equals("time")) {
            //let tLongName be set below
            tStandardName = "time";
        }

        //do standard_name first, since long_name and colorBar can use it
        if (!isSomething(tStandardName)) {
            tStandardName = tSourceAtts.getString("Standard_name");  //wrong case?
            if (!isSomething(tStandardName)) tStandardName = tSourceAtts.getString("Standard_Name");  //wrong case?
            if (!isSomething(tStandardName)) tStandardName = "";  //do after checking tSourceAtts

            //from lcSourceName?  (most are NDBC names from NdbcMetStation lists)
            //!!!??? these are CF standard names, but not all datasets will be using CF standard names
                             //mostly alphabetical by tStandardName
            if (isSomething(tStandardName)) {}
            else if (lcSourceName.equals("atmp"))        tStandardName = "air_temperature"; 
            //else if (lcSourceName.equals("bar"))         tStandardName = "air_pressure_at_sea_level"; 
            else if (lcSourceName.equals("chl_a") ||
                     lcSourceName.equals("chlor_a") ||
                     lcSourceName.equals("chlormean") ||
                     lcSourceName.equals("chla"))        tStandardName = "concentration_of_chlorophyll_in_sea_water";
            else if (lcSourceName.equals("dewp"))        tStandardName = "dew_point_temperature"; 
            else if (lcSourceName.equals("wvht"))        tStandardName = "sea_surface_swell_wave_significant_height";
            else if (lcSourceName.equals("dpd"))         tStandardName = "sea_surface_swell_wave_period";
            else if (lcSourceName.equals("apd"))         tStandardName = "sea_surface_swell_wave_period"; //???
            else if (lcSourceName.equals("mwd"))         tStandardName = "sea_surface_swell_wave_to_direction";
            else if (lcSourceName.equals("wtmp") ||
                     lcSourceName.equals("water_temp"))  tStandardName = "sea_water_temperature"; //sea???
            else if (lcSourceName.equals("sst"))         tStandardName = "sea_surface_temperature";
            else if (lcSourceName.equals("current_u") ||
                     lcSourceName.equals("current_x") ||
                     lcSourceName.equals("water_u") ||
                     lcSourceName.equals("water_x"))     tStandardName = "eastward_sea_water_velocity";
            else if (lcSourceName.equals("current_v") ||
                     lcSourceName.equals("current_y") ||
                     lcSourceName.equals("water_v") ||
                     lcSourceName.equals("water_y"))     tStandardName = "northward_sea_water_velocity";
            else if (lcSourceName.equals("shum"))        tStandardName = "specific_humidity"; 
            else if (lcSourceName.equals("tide"))        tStandardName = "surface_altitude"; 
            else if (lcSourceName.equals("tauu") ||
                     lcSourceName.equals("tau_x") ||
                     lcSourceName.equals("taux"))        tStandardName = "surface_downward_eastward_stress";
            else if (lcSourceName.equals("tauv") ||
                     lcSourceName.equals("tau_y") ||
                     lcSourceName.equals("tauy"))        tStandardName = "surface_downward_northward_stress";
            else if (lcSourceName.equals("ptdy"))        tStandardName = "tendency_of_air_pressure"; 
            else if (lcSourceName.equals("vis"))         tStandardName = "visibility_in_air"; 
            else if (lcSourceName.equals("wspu") || 
                     lcSourceName.equals("uwnd") ||                      
                     lcSourceName.equals("xwnd"))        tStandardName = "eastward_wind";
            else if (lcSourceName.equals("wspv") ||
                     lcSourceName.equals("vwnd") ||                       
                     lcSourceName.equals("ywnd"))        tStandardName = "northward_wind";
            else if (lcSourceName.equals("wd"))          tStandardName = "wind_from_direction";
            else if (lcSourceName.equals("wspd"))        tStandardName = "wind_speed";
            else if (lcSourceName.equals("gst"))         tStandardName = "wind_speed_of_gust";

        }

        //colorBar  (if these are specified, WMS works and graphs in general work better)
        if (tryToAddColorBarMinMax &&
            Double.isNaN(tSourceAtts.getDouble("colorBarMinimum")) &&
            Double.isNaN(tSourceAtts.getDouble("colorBarMaximum"))) { 
           
            double tMin = Double.NaN;
            double tMax = Double.NaN;
            String colorBarScale = null;

            //assign based on standard_name first (more consistent and less extreme than valid_min/max from other sources)
            //Fortunately, the penalty for being wrong (e.g., different units than expected) is small: bad default colorBar range.
            //FUTURE: These are CF standard_names.  Add standard_names from other standards.
            if      (tStandardName.endsWith("_from_direction"           ) ||
                     tStandardName.endsWith("_to_direction"             ) ||
                     tUnits.equals("degrees_true"                       )) {tMin = 0;    tMax = 360;}
            else if (tStandardName.equals("air_pressure"                ) ||
                     tStandardName.startsWith("air_pressure_at"         )) {tMin = 970;  tMax = 1030;}
            else if (tStandardName.equals("air_pressure_anomaly"        )) {tMin = -30;  tMax =  30;}//?
            else if (tStandardName.equals("air_temperature"             ) ||
                     tStandardName.equals("air_potential_temperature"   )) {tMin = -10;  tMax = 40;}        
            else if (tStandardName.equals("concentration_of_chlorophyll_in_sea_water") ||
                     tStandardName.equals("chlorophyll_concentration_in_sea_water") ||
                     tStandardName.equals("mass_concentration_of_chlorophyll_in_sea_water"
                                                                        )) {tMin = 0.03; tMax = 30; colorBarScale = "Log";}
            else if (lcSourceName.equals("chloranomaly"                 )) {tMin = -5;   tMax = 5;}
            else if (tStandardName.equals("dew_point_temperature"       )) {tMin = 0;    tMax = 40;}
                     //fraction  e.g., "cloud_area_fraction" "sea_ice_area_fraction"
            else if (tStandardName.indexOf("fraction") >= 0             )  {tMin = 0;    tMax = 1;}
            else if (lcSourceName.indexOf("k490") >= 0                  )  {tMin = 0;    tMax = 0.5;}
            else if (tStandardName.equals("mole_concentration_of_ammonium_in_sea_water")) 
                                                                           {tMin = 0;    tMax = 5;}
            else if (tStandardName.equals("mole_concentration_of_nitrate_in_sea_water")) 
                                                                           {tMin = 0;    tMax = 50;}
            else if (tStandardName.equals("mole_concentration_of_nitrite_in_sea_water")) 
                                                                           {tMin = 0;    tMax = 1;}
            else if (tStandardName.equals("mole_concentration_of_phosphate_in_sea_water")) 
                                                                           {tMin = 0;    tMax = 4;}
            else if (tStandardName.equals("mole_concentration_of_silicate_in_sea_water")) 
                                                                           {tMin = 0;    tMax = 50;}
            else if (tStandardName.equals("net_primary_productivity_of_carbon")) 
                                                                           {tMin = 0;    tMax = 2000;}
            else if (tUnits.toLowerCase().startsWith("percent"          ) ||
                     tUnits.startsWith("%"                              )) {tMin = 0;    tMax = 100;}
            else if (tStandardName.equals("relative_humidity"           )) {tMin = 20;   tMax = 100;}                    
            else if (tStandardName.equals("sea_floor_depth_below_sea_level"
                                                                        )) {tMin = 0;    tMax = 8000;}
            else if (tStandardName.equals("sea_surface_height_above_geoid") ||
                     tStandardName.equals("sea_surface_elevation"       )) {tMin = -2;   tMax = 2;}
            else if (tStandardName.equals("sea_surface_swell_wave_period")){tMin = 0;    tMax = 20;}
            else if (tStandardName.equals("sea_surface_swell_wave_significant_height"
                                                                        )) {tMin = 0;    tMax = 10;}
            else if (tStandardName.equals("sea_surface_temperature"     ) ||
                     tStandardName.equals("sea_surface_temperature"     ) ||
                     tStandardName.equals("sea_water potential_temperature") ||
                     tStandardName.equals("sea_water_temperature"       )) {tMin = 0;    tMax = 32;}
            else if (tStandardName.equals("sea_water_density"           )) {tMin = 20;   tMax = 28;}
            else if (tStandardName.equals("sea_water_electrical_conductivity"
                                                                        )) {tMin = 30;   tMax = 40;}
            else if (tStandardName.equals("sea_water_pressure"          )) {tMin = 0;    tMax = 5000;}
            else if (tStandardName.equals("sea_water_salinity"          ) ||
                     //lcSourceName.indexOf(   "salinity") >= 0                || //!but river/bay salinity close to 0
                     tUnits.toLowerCase().equals("psu"                  )) {tMin = 32;   tMax = 37;}
            else if (tStandardName.equals("sea_water_speed"             )) {tMin = 0;    tMax = 0.5;}
            else if (tStandardName.indexOf("sea_water_x_velocity") >= 0 ||
                     tStandardName.indexOf("sea_water_y_velocity") >= 0 ||
                     tStandardName.indexOf("sea_water_velocity") > 0    )  {tMin = -0.5; tMax = 0.5;}
            else if (tStandardName.equals("specific_humidity"           ) ||
                     tStandardName.equals("surface_specific_humidity"   )) {tMin = 0;    tMax = 30;}
            else if (tStandardName.equals("surface_altitude"            )) {tMin = -5;   tMax = 5;}
            else if (tStandardName.equals("surface_downward_x_stress") ||
                     tStandardName.equals("surface_downward_y_stress") ||
                     tStandardName.equals("surface_downward_eastward_stress") ||
                     tStandardName.equals("surface_downward_northward_stress")            
                                                                        )  {tMin = -0.5; tMax = 0.5;}
            else if (tStandardName.equals("surface_temperature_anomaly" )) {tMin = -3;   tMax = 3;}
            else if (tStandardName.equals("tendency_of_air_pressure"    )) {tMin = -3;   tMax = 3;}
            else if (tStandardName.equals("eastward_wind"               ) ||
                     tStandardName.equals("northward_wind"              ) ||
                     tStandardName.equals("x_wind"                      ) ||
                     tStandardName.equals("y_wind"                      )) {tMin = -15;  tMax = 15;}
            else if (tStandardName.equals("wind_speed"                  )) {tMin = 0;    tMax = 15;}
            else if (tStandardName.equals("wind_speed_of_gust"          )) {tMin = 0;    tMax = 30;}
            else if (tStandardName.equals("visibility_in_air"           )) {tMin = 0;    tMax = 100;}

            //next best: assign based on metadata
            if (Double.isNaN(tMin)) tMin = tSourceAtts.getNiceDouble("valid_min");
            if (Double.isNaN(tMax)) tMax = tSourceAtts.getNiceDouble("valid_max");
            if (Double.isNaN(tMin)) tMin = tSourceAtts.getNiceDouble("fgdc_valid_min");
            if (Double.isNaN(tMax)) tMax = tSourceAtts.getNiceDouble("fgdc_valid_max");
            if (Double.isNaN(tMin) && Double.isNaN(tMax)) {
                PrimitiveArray pa = tSourceAtts.get("valid_range");
                if (pa != null && pa.size() == 2) {
                    tMin = pa.getNiceDouble(0);
                    tMax = pa.getNiceDouble(1);
                }
            }
            if (Double.isNaN(tMin) && Double.isNaN(tMax)) {
                PrimitiveArray pa = tSourceAtts.get("actual_range");
                if (pa != null && pa.size() == 2) {
                    double tLow  = pa.getDouble(0);
                    double tHigh = pa.getDouble(1);
                    if (!Double.isNaN(tLow) && !Double.isNaN(tHigh)) {
                        double d2[] = Math2.suggestLowHigh(tLow, tHigh);
                        tMin = d2[0];
                        tMax = d2[1];
                    }
                }
            }

            //assign if known
            if (!Double.isNaN(tMin) && !Double.isNaN(tMax)) { 
                tAddAtts.add("colorBarMinimum", tMin);
                tAddAtts.add("colorBarMaximum", tMax);
            }
            if (colorBarScale != null)
                tAddAtts.add("colorBarScale", "Log");

        }

        //long_name   (uses standardName)
        //note that this doesn't suggest
        //but addDummyRequiredVariableAttributesForDatasetsXml will suggest something
        //  even if there is an existing value (give admin a choice)
        if (!isSomething(tLongName)) {
            if (!isSomething(tLongName)) tLongName = tSourceAtts.getString("Long_name"); //wrong case?
            if (!isSomething(tLongName)) tLongName = tSourceAtts.getString("fgdc_long_name");
            //no need to set to "" since next line will always set it
            if (!isSomething(tLongName)) tLongName = EDV.suggestLongName(oLongName, tSourceName, tStandardName);
        }

        //units
        if (isSomething(tUnits)) {
            tUnits = String2.replaceAll(tUnits, "%", "percent");
        } else {
            tUnits = tSourceAtts.getString("Units");  //wrong case?
            if (!isSomething(tUnits)) tUnits = tSourceAtts.getString("fgdc_units");
            if (!isSomething(tUnits)) tUnits = "";  //do after checking tSourceAtts
        }

        //scale_factor or add_offset are strings?!
        //e.g., see fromThreddsCatalog test #56
        PrimitiveArray pa = tSourceAtts.get("add_offset");
        if (pa != null && pa.size() > 0 && pa instanceof StringArray)
            tAddAtts.add("add_offset", pa.getFloat(0));
        pa = tSourceAtts.get("scale_factor");
        if (pa != null && pa.size() > 0 && pa instanceof StringArray) 
            tAddAtts.add("scale_factor", pa.getFloat(0));

        //deal with scale_factor in tUnits  (e.g., "* 10")
        float tScaleFactor = tSourceAtts.getFloat("scale_factor");  
        if (isSomething(tUnits) && !Float.isNaN(tScaleFactor) && tScaleFactor != 0) {
            int inverse = Math2.roundToInt(1 / tScaleFactor);
            tUnits = String2.replaceAll(tUnits, "*"  + inverse, "");  //e.g., *10  
            tUnits = String2.replaceAll(tUnits, "* " + inverse, "");  //e.g., * 10  
            tUnits = String2.replaceAll(tUnits, "x"  + inverse, "");
            tUnits = String2.replaceAll(tUnits, "x " + inverse, "");
            tUnits = String2.replaceAll(tUnits, "X"  + inverse, "");
            tUnits = String2.replaceAll(tUnits, "X " + inverse, "");
            tUnits = String2.replaceAll(tUnits, "()",           "");  //e.g., (* 10)
            tUnits = tUnits.trim();  //e.g. space before "* 10"
        }


        //do ioos_category last, since it uses standard_name, long_name, and units
        if (EDStatic.variablesMustHaveIoosCategory && 
            !isSomething(tSourceAtts.getString("ioos_category"))) {
            //It is hard to be absolutely certain when assigning ioos_category.
            //Fortunately, this isn't crucial information and is used mostly for data discovery.
            //Occasional errors are okay.
            //So my goal is >98% correct (<1 out of 50 incorrect).

            /*
        "Bathymetry", 
        "Biology", //bob added
        "Bottom Character", 
        "Colored Dissolved Organic Matter", //added 2011-05-19
        "Contaminants", "Currents", //was "Surface Currents" 
        "Dissolved Nutrients", "Dissolved O2",
        "Ecology", //bob added
        "Fish Abundance", "Fish Species", 
        "Heat Flux", 
        "Hydrology", //bob added 2011-02-07
        "Ice Distribution", "Identifier", 
        LOCATION_CATEGORY,  //bob added
        "Meteorology", //bob added; use if not Temperature or Wind
        "Ocean Color", "Optical Properties",  //what is dividing line?
        "Other", //bob added
        "Pathogens", 
        "pCO2", //added 2011-05-19
        "Phytoplankton Species", //??the species name? better to use Taxonomy??
        "Pressure", //bob added
        "Productivity", //bob added
        "Quality", //bob added 2010-11-10
        "Salinity", "Sea Level", 
        "Statistics", //bob added 2010-12-24
        "Stream Flow", //added 2011-05-19
        "Surface Waves", 
        "Taxonomy", //bob added
        "Temperature",            
        TIME_CATEGORY, //bob added
        "Total Suspended Matter", //added 2011-05-19
        "Unknown", 
        "Wind", //had Wind. 2011-05-19 has "Wind Speed and Direction", but that seems unnecessarily limited
        "Zooplankton Species", //??the species name? better to use Taxonomy??
        "Zooplankton Abundance"};       
        */

            //All ioos_category tests are in lowercase.
            //Beware of tested word within other English words, e.g., "si" (silicon) is in "since".
            //Pipes allow me to test if a whole item is a specific string, e.g., "|si|".
            String lc = "|" +  
                (isSomething(tLongName)    ? tLongName.toLowerCase()     + "|" : "") +
                (isSomething(tStandardName)? tStandardName.toLowerCase() + "|" : "") +
                (isSomething(tUnits)       ? tUnits.toLowerCase()        + "|" : "") + 
                tSourceName.toLowerCase() + "|"; 

            if (lc.indexOf("bathym")       >= 0 ||
                lc.indexOf("topo")         >= 0) {
                tAddAtts.add("ioos_category", "Bathymetry");

            } else if (                
                lc.indexOf("birth")        >= 0 ||
                lc.indexOf("chorion")      >= 0 ||
                lc.indexOf("diet")         >= 0 ||
                lc.indexOf("disease")      >= 0 ||
                lc.indexOf("egg")          >= 0 ||
                lc.indexOf("food")         >= 0 ||
                lc.indexOf("larv")         >= 0 || 
                lc.indexOf("myomere")      >= 0 ||
                lc.indexOf("|sex|")        >= 0 ||
                lc.indexOf("size")         >= 0 ||                
                lc.indexOf("stage")        >= 0 ||                
                lc.indexOf("yolk")         >= 0) {
                tAddAtts.add("ioos_category", "Biology");

            } else if (
                lc.indexOf("cfc11")        >= 0 ||
                lc.indexOf("debris")       >= 0 ||
                lc.indexOf("freon")        >= 0) {
                tAddAtts.add("ioos_category", "Contaminants");

            } else if (
                lc.indexOf("ammonia")      >= 0 ||
                lc.indexOf("ammonium")     >= 0 ||
                lc.indexOf("carbonate")    >= 0 ||
                lc.indexOf("co3")          >= 0 ||
                lc.indexOf("|n_n|")        >= 0 ||
                lc.indexOf("nh3")          >= 0 ||
                lc.indexOf("nh4")          >= 0 ||
                lc.indexOf("nitrate")      >= 0 ||
                lc.indexOf("nitrite")      >= 0 ||
                lc.indexOf("no2")          >= 0 ||
                lc.indexOf("no3")          >= 0 ||
                lc.indexOf("phosphate")    >= 0 ||
                lc.indexOf("po4")          >= 0 ||
                lc.indexOf("silicate")     >= 0 ||
                lc.indexOf("|si|")         >= 0) {
                tAddAtts.add("ioos_category", "Dissolved Nutrients");

            //Sea Level before Location and Currents so tide is caught correctly
            } else if (
               (lc.indexOf("geopotential") >= 0 && lc.indexOf("height") >= 0) ||
                lc.indexOf("ssh")                   >= 0 ||
                lc.indexOf("sea_surface_elevation") >= 0 ||
                lc.indexOf("sea surface height")    >= 0 ||
                lc.indexOf("sea_surface_height")    >= 0 ||
                lc.indexOf("seaSurfaceHeight")      >= 0 ||
                lc.indexOf("tide")                  >= 0) { 
                tAddAtts.add("ioos_category", "Sea Level");

            } else if (
                lc.indexOf("current")      >= 0 ||
                lc.indexOf("water_dir")    >= 0 ||
               (lc.indexOf("water")        >= 0 && 
                   (lc.indexOf("direction")   >= 0 || 
                    lc.indexOf("speed")       >= 0 || 
                    lc.indexOf("spd")         >= 0 || 
                    lc.indexOf("vel")         >= 0 || 
                    lc.indexOf("velocity")    >= 0))) { 
                tAddAtts.add("ioos_category", "Currents");

            } else if (
                (lc.indexOf("o2") >= 0 && lc.indexOf("co2") < 0) || //no2 was caught above
                lc.indexOf("oxygen")       >= 0) { 
                tAddAtts.add("ioos_category", "Dissolved O2");

            } else if (
                lc.indexOf("predator")     >= 0 ||
                lc.indexOf("prey")         >= 0 ||
                lc.indexOf("|troph")       >= 0) {  //don't catch geostrophic
                tAddAtts.add("ioos_category", "Ecology");

            } else if (
                lc.indexOf("hflx")         >= 0 ||
               (lc.indexOf("heat") >= 0 && (lc.indexOf("flux") >= 0 || lc.indexOf("flx") >= 0)) ||
                lc.indexOf("lflx")         >= 0 ||
                lc.indexOf("sflx")         >= 0) {
                tAddAtts.add("ioos_category", "Heat Flux");

            } else if (
                lc.indexOf("|ice")         >= 0 ||
                lc.indexOf(" ice")         >= 0 || 
                lc.indexOf("snow")         >= 0) {
                tAddAtts.add("ioos_category", "Ice Distribution");

            } else if (
                lc.indexOf("|id|")         >= 0 ||
                lc.indexOf("site_id")      >= 0 ||
                lc.indexOf("station_id")   >= 0 ||
                lc.indexOf("stationid")    >= 0) { 
                tAddAtts.add("ioos_category", "Identifier");

            } else if (
                lc.indexOf("altitude")     >= 0 ||
                lc.indexOf("depth")        >= 0 || 
                lc.indexOf("geox")         >= 0 || 
                lc.indexOf("geoy")         >= 0 || 
                lc.indexOf("|lon|")        >= 0 ||
                lc.indexOf("|lon_")        >= 0 ||
                lc.indexOf("longitude")    >= 0 ||
                lc.indexOf("|lat|")        >= 0 ||
                lc.indexOf("|lat_")        >= 0 ||
                lc.indexOf("latitude")     >= 0 ||
                lc.indexOf("|x|")          >= 0 || 
                lc.indexOf("xax")          >= 0 || //x axis
                lc.indexOf("|xpos|")       >= 0 || 
                lc.indexOf("|y|")          >= 0 ||
                lc.indexOf("yax")          >= 0 || //y axis
                lc.indexOf("|ypos|")       >= 0 || 
                lc.indexOf("|z|")          >= 0 || 
                lc.indexOf("zax")          >= 0 || //z axis
                lc.indexOf("|zpos|")       >= 0 || 
                lc.indexOf("zlev")         >= 0 ||
                lc.indexOf("|srs|")        >= 0 ||
                lc.indexOf("|datum|")      >= 0 ||
                lc.indexOf("|vertdatum|")  >= 0 ||
                lc.indexOf("location")   >= 0 ||
                lc.indexOf("locality")   >= 0 ||
                lc.indexOf("|city|")       >= 0 ||
                lc.indexOf("|county|")     >= 0 ||
                lc.indexOf("|province|")   >= 0 ||
                lc.indexOf("|state|")      >= 0 ||
                lc.indexOf("|country|")    >= 0 ||
                lc.indexOf("|fips")        >= 0) {
                tAddAtts.add("ioos_category", "Location");            

            } else if (
                lc.indexOf("cldc")         >= 0 ||  //cloud cover
                lc.indexOf("cloud")        >= 0 ||
                lc.indexOf("cloud")        >= 0 ||
                lc.indexOf("dew point")    >= 0 ||
                lc.indexOf("dewp")         >= 0 ||
                lc.indexOf("dewpt")        >= 0 ||
               (lc.indexOf("front") >= 0 && lc.indexOf("probability") >= 0) ||
                lc.indexOf("humidity")     >= 0 ||
                lc.indexOf("rhum")         >= 0 ||
                lc.indexOf("|shum|")       >= 0 ||
                lc.indexOf("total electron content") >= 0 ||
                lc.indexOf("visi")         >= 0) {
                tAddAtts.add("ioos_category", "Meteorology");

            } else if (
                lc.indexOf("chlor")        >= 0 ||
                lc.indexOf("chla")         >= 0 || 
                lc.indexOf("chl_a")        >= 0 || 
                lc.indexOf("k490")         >= 0 ||
                lc.indexOf("|par|")        >= 0) {
                tAddAtts.add("ioos_category", "Ocean Color");

            } else if (
                lc.indexOf("fluor")        >= 0) {
                tAddAtts.add("ioos_category", "Optical Properties");

            } else if (
                lc.indexOf("phytoplankton") >= 0) {  //not a great test
                tAddAtts.add("ioos_category", "Phytoplankton Species");

            } else if (
                lc.indexOf("aprs")         >= 0 || //4 letter NDBC abbreviations
                lc.indexOf("ptdy")         >= 0 ||
                lc.indexOf("pressure")     >= 0 ||
                lc.indexOf("sigma")        >= 0 ||
                lc.indexOf("|mbar|")       >= 0 ||
                lc.indexOf("|hpa|")        >= 0) {
                tAddAtts.add("ioos_category", "Pressure");

            } else if (
                lc.indexOf("productivity") >= 0 || 
                lc.indexOf("prim_prod")    >= 0 || 
                lc.indexOf("primprod")     >= 0) { 
                tAddAtts.add("ioos_category", "Productivity");
            
            } else if (
                lc.indexOf("|ph|")         >= 0 ||  //borderline
                lc.indexOf("psu")          >= 0 ||
                lc.indexOf("salinity")     >= 0 ||
                lc.indexOf("conductivity") >= 0) {
                tAddAtts.add("ioos_category", "Salinity");

            //see Sea Level above

            } else if (
                lc.indexOf("awpd")         >= 0 || //4 letter NDBC abbreviations
                lc.indexOf("dwpd")         >= 0 ||
                lc.indexOf("mwvd")         >= 0 ||
                lc.indexOf("wvht")         >= 0 ||
                lc.indexOf("wave")         >= 0) { 
                tAddAtts.add("ioos_category", "Surface Waves");
            
            } else if (
                lc.indexOf("phylum")       >= 0 ||
                lc.indexOf("order")        >= 0 ||
                lc.indexOf("family")       >= 0 ||
                lc.indexOf("genus")        >= 0 ||
                lc.indexOf("genera")       >= 0 ||
                lc.indexOf("species")      >= 0 ||
                lc.indexOf("sp.")          >= 0 ||
                lc.indexOf("spp.")         >= 0 ||
                lc.indexOf("stock")        >= 0 ||
                lc.indexOf("taxa")         >= 0 ||
                lc.indexOf("commonname")   >= 0) { 
                tAddAtts.add("ioos_category", "Taxonomy");

            } else if (
                lc.indexOf("airtemp")              >= 0 ||
                lc.indexOf("air temp")             >= 0 ||
                lc.indexOf("atemp")                >= 0 ||
                lc.indexOf("atmp")                 >= 0 ||  //4 letter NDBC abbreviation
                lc.indexOf("sst")                  >= 0 ||
                lc.indexOf("temperature")          >= 0 ||
                lc.indexOf("wtmp")                 >= 0 ||  //4 letter NDBC abbreviation
                lc.indexOf("wtemp")                >= 0 || 
                //temperature units often used with other units for other purposes
                //but if alone, it means temperature
                lc.indexOf("|°c|")                 >= 0 ||
                lc.indexOf("|celsius|")            >= 0 ||
                lc.indexOf("|degree_centigrade|")  >= 0 ||
                lc.indexOf("|degree_celsius|")     >= 0 ||
                lc.indexOf("|degrees_celsius|")    >= 0 ||
                lc.indexOf("|degc|")               >= 0 ||
                lc.indexOf("|degreec|")            >= 0 ||
                lc.indexOf("|degreesc|")           >= 0 ||
                lc.indexOf("|degree_c|")           >= 0 ||
                lc.indexOf("|degrees_c|")          >= 0 ||
                lc.indexOf("|deg_c|")              >= 0 ||
                lc.indexOf("|degs_c|")             >= 0 ||
                lc.indexOf("|cel|")                >= 0 || //ucum

                lc.indexOf("|°f|")                 >= 0 ||
                lc.indexOf("|fahrenheit|")         >= 0 ||
                lc.indexOf("|degree_fahrenheit|")  >= 0 ||
                lc.indexOf("|degrees_fahrenheit|") >= 0 ||
                lc.indexOf("|degf|")               >= 0 ||
                lc.indexOf("|degreef|")            >= 0 ||
                lc.indexOf("|degreesf|")           >= 0 ||
                lc.indexOf("|degree_f|")           >= 0 ||
                lc.indexOf("|degrees_f|")          >= 0 ||
                lc.indexOf("|deg_f|")              >= 0 ||
                lc.indexOf("|degs_f|")             >= 0 ||
                lc.indexOf("|[degf]|")             >= 0 || //ucum

                lc.indexOf("|°k|")                 >= 0 ||
                lc.indexOf("|kelvin|")             >= 0 ||
                lc.indexOf("|degree_kelvin|")      >= 0 ||
                lc.indexOf("|degrees_kelvin|")     >= 0 ||
                lc.indexOf("|degk|")               >= 0 ||
                lc.indexOf("|degreek|")            >= 0 ||
                lc.indexOf("|degreesk|")           >= 0 ||
                lc.indexOf("|degree_k|")           >= 0 ||
                lc.indexOf("|degrees_k|")          >= 0 ||
                lc.indexOf("|deg_k|")              >= 0 ||
                lc.indexOf("|degs_k|")             >= 0 ||
                lc.indexOf("|k|")                  >= 0) { //ucum

                tAddAtts.add("ioos_category", "Temperature");

            } else if (
                lc.indexOf("|time")        >= 0 ||
                lc.indexOf("|year")        >= 0 ||
                lc.indexOf("|month")       >= 0 ||
                lc.indexOf("|date")        >= 0 ||
                lc.indexOf("|day")         >= 0 ||   //not "someday"
                lc.indexOf("|hour")        >= 0 ||
                lc.indexOf("|minute")      >= 0 ||
                lc.indexOf("|second|")     >= 0 ||  //e.g., but not meters/second  secondRef
                lc.indexOf("|seconds|")    >= 0 ||  //e.g., but not meters/second  secondRef
                lc.indexOf(" since 19")    >= 0 ||
                lc.indexOf(" since 20")    >= 0) { 
                tAddAtts.add("ioos_category", "Time");

            } else if (
                lc.indexOf("uwnd")         >= 0 ||
                lc.indexOf("vwnd")         >= 0 ||
                lc.indexOf("xwnd")         >= 0 ||
                lc.indexOf("ywnd")         >= 0 ||
                lc.indexOf("wdir")         >= 0 || //4 letter NDBC abbreviations
                lc.indexOf("wspd")         >= 0 ||
                lc.indexOf("wgst")         >= 0 ||
                lc.indexOf("wspu")         >= 0 ||
                lc.indexOf("wspv")         >= 0 ||
                lc.indexOf("wind")         >= 0) { 
                tAddAtts.add("ioos_category", "Wind");
            
            } else if (
                lc.indexOf("zooplankton") >= 0) {  //not a great test
                tAddAtts.add("ioos_category", "Zooplankton Species");

            } else {
                if (reallyVerbose) String2.log("    ioos_category=Unknown for " + lc);
                tAddAtts.add("ioos_category", "Unknown");
            }        
        }

        //add to addAtts if changed
        if (isSomething(tUnits)       && !tUnits.equals(oUnits))               tAddAtts.add("units",         tUnits);
        if (isSomething(tLongName)    && !tLongName.equals(oLongName))         tAddAtts.add("long_name",     tLongName);
        if (isSomething(tStandardName)&& !tStandardName.equals(oStandardName)) tAddAtts.add("standard_name", tStandardName);

        return tAddAtts;
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to suggest
     * a Conventions metadata value.    
     *
     * @param con the old Conventions value, may be "" or null.
     * @param the new Conventions value
     */
    public static String suggestConventions(String con) {
        if (con == null) 
            con = "";
        if (con.indexOf("COARDS") < 0) 
            con += ", COARDS";
        if (con.indexOf("CF") < 0) 
            con += ", CF-1.4";
        if (con.indexOf("Unidata Dataset Discovery v1.0") < 0) 
            con += ", Unidata Dataset Discovery v1.0";
        if (con.startsWith(", "))
            con = con.substring(2);
        return con;
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to suggest
     * a datasetID.    
     * <br>This seeks to be short, descriptive, and unique (so 2 datasets don't have same datasetID).
     *
     * @param tPublicSourceUrl a real URL (starts with "http", e.g., http://oceanwatch.pfeg.noaa.gov/...), 
     *     a fileDirectory (with trailing '/') or directory+fileName (may be a fileNameRegex), 
     *     or a fake fileDirectory (not ideal).
     *     <br>If an OPeNDAP url, it is without the .das, .dds, or .html extension.
     *     <br>If a fileDirectory, the two rightmost directories are important.
     * @return a suggested datasetID, e.g., noaa_pfeg#########
     */
    public static String suggestDatasetID(String tPublicSourceUrl) {
        //???alternative: use String2.modifyToBeFileNameSafe to convert end to, e.g.,
        //   satellite_MH_chla_5day.
        //But some datasetIDs would be very long and info is already in sourceUrl in original form.

        //extract from tPublicSourceUrl
        String dir = tPublicSourceUrl.indexOf('/' ) >= 0 ||
                     tPublicSourceUrl.indexOf('\\') >= 0?
            File2.getDirectory(tPublicSourceUrl) :
            tPublicSourceUrl;
        String dsi = String2.modifyToBeFileNameSafe(
            String2.toSVString(suggestInstitutionParts(dir), "_", true));
        dsi = String2.replaceAll(dsi, '-', '_');
        dsi = String2.replaceAll(dsi, '.', '_');
        return dsi + String2.md5Hex12(tPublicSourceUrl);  
        
        //return String2.md5Hex12(tPublicSourceUrl);  
    }

    /** 
     * This extracts the institution parts from a URL.
     *
     * @param tPublicSourceUrl a real URL (starts with "http", e.g., http://oceanwatch.pfeg.noaa.gov/...), 
     *     a fileDirectory (with or without trailing '/'), 
     *     or a fake fileDirectory (not ideal).
     *     <br>If a fileDirectory, the rightmost directory is used.
     * @return a String[] with the parts, e.g., noaa, pmel
     */
    public static String[] suggestInstitutionParts(String tPublicSourceUrl) {

        String tdi = tPublicSourceUrl;
        StringArray parts;
        if (tPublicSourceUrl.startsWith("http")) {

            //reduce to e.g., oceanwatch.pfeg.noaa.gov
            int po = tdi.indexOf("//");  if (po >= 0) tdi = tdi.substring(po + 2);
            po     = tdi.indexOf('/');   if (po >= 0) tdi = tdi.substring(0, po);  

            parts = StringArray.fromCSV(String2.replaceAll(tdi, '.', ','));

            //just keep one name?
            po = parts.indexOf("edu");
            if (po < 0) po = parts.indexOf("org");
            if (po < 0) po = parts.indexOf("com");
            if (po > 0)
                return new String[]{parts.get(po - 1)};

            //remove ending, e.g., .gov .edu .com .2LetterCountryCode
            if (parts.size() > 1)
                parts.remove(parts.size() - 1);

            //if ending was country code, then .gov .edu .com .net or .org may still be at end
            String last = parts.get(parts.size() - 1);
            if  (last.equals("gov") || last.equals("edu") ||
                 last.equals("com") || last.equals("org") ||
                 last.equals("net") || last.equals("mil")) 
                parts.remove(parts.size() - 1);

            //if 3+ part name (thredds1.pfeg.noaa) or first part is www, remove first part
            if (parts.size() >= 3 || parts.get(0).equals("www"))
                parts.remove(0);

            //reverse the parts
            parts.reverse();

        } else {
            //tPublicSourceUrl is a filename
            if (tdi.startsWith("/")) tdi = tdi.substring(1);   
            if (tdi.endsWith("/"))   tdi = tdi.substring(0, tdi.length() - 1);
            parts = StringArray.fromCSV(String2.replaceAll(tdi, '/', ','));

            //just keep the last part
            if (parts.size() > 1)
                parts.removeRange(0, parts.size() - 1);
        }
        return parts.toArray();
    }

    /** 
     * This extracts the institution from a URL.
     *
     * @param tPublicSourceUrl a real URL (starts with "http", e.g., http://oceanwatch.pfeg.noaa.gov/...), 
     *     a fileDirectory (with or without trailing '/'), 
     *     or a fake fileDirectory (not ideal).
     *     <br>If a fileDirectory, the two rightmost directories are important.
     * @return e.g., NOAA PFEG
     */
    public static String suggestInstitution(String tPublicSourceUrl) {
        return String2.toSVString(suggestInstitutionParts(tPublicSourceUrl), " ", false).toUpperCase();
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to write 
     * directions to datasets.xml file.
     * <br>This doesn't have the closing "-->\n\n" so users can add other comments.
     *
     * @throws Throwable if trouble
     */
    public static String directionsForGenerateDatasetsXml() throws Throwable {
        return 
"<!--\n" +
" DISCLAIMER:\n" +
"   The chunk of datasets.xml made by GenerageDatasetsXml isn't perfect.\n" +
"   YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
"   GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
"   correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
"   THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
"\n" +
" DIRECTIONS:\n" +
" * Read about this type of dataset in\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n";
//This doesn't have the closing "-->\n\n" so users can add other comments.
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to write the
     * variables to the writer in the datasets.xml format.
     *
     * <p>This suggests a destinationName (notably, longitude, latitude, time, or
     * if the sourceName can't be used as a destName.
     *
     * @param sourceTable  may be null. 
     *    <br>If present, the variables must parallel the variables in the addTable. 
     *    <br>This is used to write the source attributes as comments.
     *    <br>It is also used as a source for source "units", used to identify variables (e.g., for ioos_category) 
     * @param addTable the main table. 
     * @param variableType  e.g., axisVariable or dataVariable
     * @param sourceUnits one element per column in the table.
     *    The whole thing can be null 
     *    (it is for oldGenerateDatasetsXml methods, since the table has the source attributes), 
     *    or any element may be null.
     * @param tryToCatchLLAT if true, this tries to catch and rename variables
     *    to longitude, latitude, altitude, and time.
     *    <br>This should be true for tabular dataVariables and grid axis variables.
     *    <br>This should be false for grid data variables.
     * @param questionDestinationName if true, the destinationName is preceded by "???"
     *    if it is different from sourceName.
     * @throws Throwable if trouble
     */
    public static String writeVariablesForDatasetsXml(Table sourceTable, Table addTable,
        String variableType, boolean includeDataType, 
        boolean tryToCatchLLAT, boolean questionDestinationName) throws Throwable {

        if (sourceTable != null)
            Test.ensureEqual(sourceTable.nColumns(), addTable.nColumns(),
                "The number of columns in sourceTable and addTable isn't equal!");
        String indent = "    ";
        StringBuilder sb = new StringBuilder();
        //e.g., don't change "lon" to "longitude" if there is already a "longitude" variable
        int sLongitude  = addTable.findColumnNumber("longitude"), 
            sLatitude   = addTable.findColumnNumber("latitude"), 
            sTime       = addTable.findColumnNumber("time");

        //ensure time has proper units
        if (sTime >= 0) {
            Attributes addAtts = addTable.columnAttributes(sTime);
            String tUnits = addAtts.getString("units");  
            if (tUnits == null && sourceTable != null) 
                tUnits = sourceTable.columnAttributes(sTime).getString("units");
            String suggestDestName = suggestDestinationName("time", tUnits, Float.NaN, tryToCatchLLAT);
            if (!suggestDestName.equals("time")) {
                addTable.setColumnName(sTime, "time_");
                sTime = -1;
            }
        }

        //go through the columns
        for (int col = 0; col < addTable.nColumns(); col++) {
            String tSourceName = sourceTable == null? addTable.getColumnName(col) :
                sourceTable.getColumnName(col);
            sb.append(
                indent + "<" + variableType + ">\n" +
                indent + "    <sourceName>" + tSourceName + "</sourceName>\n");

            //make new destinationName?
            Attributes sourceAtts = sourceTable == null? new Attributes() : 
                sourceTable.columnAttributes(col);
            Attributes addAtts = addTable.columnAttributes(col);
            String tUnits = addAtts.getString("units");  
            if (tUnits == null) 
                tUnits = sourceAtts.getString("units");
            String suggestDestName = suggestDestinationName(tSourceName, tUnits, 
                sourceAtts.getFloat("scale_factor"), tryToCatchLLAT);
            String tDestName = null;
            //String2.log("col=" + col + " sourceName=" + tSourceName + " units=" + tUnits + " suggestDestName=" + suggestDestName);

            if (col == sLongitude ||
                (sLongitude < 0 && suggestDestName.equals("longitude"))) {
                //even though sourceName may be longitude, be explicit so I know it was caught
                tDestName = "longitude"; 
                //addAtts.set("long_name", "Longitude");
                sLongitude = col; //no other column will be longitude

            } else if (col == sLatitude ||
                (sLatitude < 0 && suggestDestName.equals("latitude"))) {
                //even though sourceName may be latitude, be explicit so I know it was caught
                tDestName = "latitude"; 
                //addAtts.set("long_name", "Latitude");
                sLatitude = col; //no other column will be latitude

            } else if ((col == sTime || sTime < 0) && suggestDestName.equals("time")) {
                //above test deals ensures that "time" var is either
                //  already called "time"  and has proper units
                //  or if no sourceName is "time" and this has proper units.
                //See suggestDestinationName comments regarding time.
                tDestName = "time"; 
                //addAtts.set("long_name", "Time");
                sTime = col; //no other column will be time

            } else if (sTime >= 0 && suggestDestName.equals("time")) {
                //time already assigned
                tDestName = tSourceName;
                if (tDestName.equals("time"))
                    tDestName = "time_";

            } else { //always show destName, not just if different;  was: if (!tSourceName.equals(suggestDestName)) {
                tDestName = suggestDestName;
            }

            if (questionDestinationName && !tDestName.equals(tSourceName)) {
                tDestName = "???" + tDestName;
            }

            if (tDestName != null)
                sb.append(
                    indent + "    <destinationName>" + tDestName + "</destinationName>\n");

            if (includeDataType) sb.append(
                indent + "    <dataType>" + addTable.getColumn(col).elementClassString() + "</dataType>\n");
            if (sourceTable != null)
                sb.append(writeAttsForDatasetsXml(false, sourceAtts, indent + "    "));
            sb.append    (writeAttsForDatasetsXml(true,  addAtts,    indent + "    "));
            sb.append(
                indent + "</" + variableType + ">\n");
        }
        return sb.toString();
    }

    /**
     * This is used by writeVariablesForDatasetsXml and others to
     * suggest a destination name for a variable (notably, longtitude, latitude, time,
     * but others if, e.g., the sourceName has invalid characters).
     * <br>NOTE: time is caught solely via a value units value, 
     *    e.g., units="seconds since 1970-01-01",
     * <br>NOTE: there is no assurance that timezone is Zulu!!!
     * <br>If tSourceName is "time" but units aren't "... since ..." or "???" or "", this returns "time2". 
     * <br>Thus, this only returns "time" if the units are appropriate.
     * 
     * @param tSourceName the sourceName.
     * @param tUnits the addUnits or sourceUnits (may be null)
     * @param tScaleFactor from the "scale_factor" attribute, or NaN.
     *    This is used to look for the inverse of the scale_factor in the 
     *    variable name, e.g, "* 1000", and remove it.
     * @param tryToCatchLLAT if true, this tries to catch and rename variables
     *    to longitude, latitude, altitude, and time.
     *    <br>This should be true for tabular dataVariables and grid axis variables.
     *    <br>This should be false for grid data variables.
     * @return the suggested destinationName (which may be the same).
     */
    public static String suggestDestinationName(String tSourceName, String tUnits,
        float tScaleFactor, boolean tryToCatchLLAT) {

        //remove (units) from SOS sourceNames, e.g., "name (units)"
        int po = tSourceName.indexOf(" (");
        if (po > 0)
            tSourceName = tSourceName.substring(0, po);

        String lcSourceName = tSourceName.toLowerCase();

        //just look at suggested units
        if (tUnits == null) tUnits = "";
        po = tUnits.indexOf("???");
        if (po >= 0)
            tUnits = tUnits.substring(po + 3);  

        if (tryToCatchLLAT) {
            if (tSourceName.equals("longitude") ||            
                ((lcSourceName.indexOf("lon") >= 0 ||
                  lcSourceName.equals("x") ||
                  lcSourceName.equals("xax")) &&  //must check, since uCurrent and uWind use degrees_east, too
                 (tUnits.equals("degrees_east") ||
                  tUnits.equals("degree_east") ||
                  tUnits.equals("degrees_west") || //bizarre, but sometimes used for negative degrees_east values
                  tUnits.equals("degree_west") ||
                  tUnits.equals("degrees") ||
                  tUnits.equals("degree")))) 

                return "longitude"; 
                 
            if (tSourceName.equals("latitude") ||            
                ((lcSourceName.indexOf("lat") >= 0 ||
                  lcSourceName.equals("y") ||
                  lcSourceName.equals("yax")) &&  
                 (tUnits.equals("degrees_north") ||
                  tUnits.equals("degree_north") ||
                  tUnits.equals("degrees") ||
                  tUnits.equals("degree")))) 
     
                return "latitude"; 

            if (tUnits.indexOf(" since ") > 0) {
                try {
                    Calendar2.getTimeBaseAndFactor(tUnits); //just to throw exception if trouble
                    return "time"; 
                } catch (Exception e) {
                    String2.log("Unexpected failure when checking validity of possible time var's units=\"" + 
                        tUnits + "\":\n" + 
                        MustBe.throwableToString(e));
                }
            }

            if (tSourceName.equals("time") && !tUnits.equals("")) 
                //name is time but units aren't "... since ..." or "???" or ""! 
                //so change name
                return "time2";
        }

        //make sure tSourceName will be a valid destName
        String tDestName = tSourceName; 

        //deal with scale_factor
        //do before modifyToBeFileNameSafe, which removes '*'
        if (!Float.isNaN(tScaleFactor) && tScaleFactor != 0) {
            int inverse = Math2.roundToInt(1 / tScaleFactor);
            tDestName = String2.replaceAll(tDestName, "*" + inverse, "");  //e.g., *10  
            tDestName = String2.replaceAll(tDestName, "x" + inverse, "");
            tDestName = String2.replaceAll(tDestName, "X" + inverse, "");
            tDestName = tDestName.trim();
        }

        //make a valid destName
        tDestName = String2.modifyToBeFileNameSafe(tSourceName);
        if (tDestName.equals("_")) 
            tDestName = "a";
        tDestName = String2.replaceAll(tDestName, '.', '_');
        tDestName = String2.replaceAll(tDestName, '-', '_');
        char firstCh = tDestName.charAt(0);
        if ((firstCh >= 'A' && firstCh <= 'Z') || (firstCh >= 'a' && firstCh <= 'z')) {
            //so valid variable name in Matlab and ...
        } else {
            tDestName = "a_" + tDestName;
        }
        while (tDestName.indexOf("__") >= 0)
            tDestName = String2.replaceAll(tDestName, "__", "_");

        return tDestName;
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to write the
     * cdm_data_type-related info to the writer in the datasets.xml format.
     *
     * @throws Throwable if trouble
     */
    public static String cdmSuggestion() {
        return
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n";
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to write the
     * attributes to the writer in the datasets.xml format.
     *
     * @param isAddAtts if isAddAtts, then they are written as addAttributes.
     *    If not, they are written as sourceAttributes and commented out.
     * @param tAddAtts
     * @param indent a string of spaces
     * @throws Throwable if trouble
     */
    public static String writeAttsForDatasetsXml(boolean isAddAtts, 
        Attributes tAddAtts, String indent) throws Throwable {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + 
            (isAddAtts? "<addAttributes>\n" : "<!-- sourceAttributes>\n"));
        String names[] = tAddAtts.getNames();
        for (int att = 0; att < names.length; att++) {
            PrimitiveArray attPa = tAddAtts.get(names[att]);
            sb.append(indent + "    <att name=\"" + names[att] + "\"");
            if (attPa instanceof StringArray) {
                String val = XML.encodeAsXML(attPa.getString(0));
                if (!isAddAtts)
                    //replace all "--" with "- - " so not interpreted as end of comment
                    val = String2.replaceAll(val, "--", "- - "); 
                sb.append(">" + val + "</att>\n");
            } else {
                sb.append(" type=\"" + attPa.elementClassString() + 
                    (attPa.size() > 1? "List" : "") +
                    "\">" + String2.replaceAll(attPa.toString(), ", ", " ") + "</att>\n");
            }
        }
        sb.append(indent + 
            (isAddAtts? "</addAttributes>\n" : "</sourceAttributes -->\n"));
        return sb.toString();
    }

    /**
     * This adds a line to the "history" attribute (which is created if it 
     * doesn't already exist).
     *
     * @param attributes (always a COPY of the dataset's global attributes,
     *    so you don't get multiple similar history lines of info)
     * @param text  usually one line of info
     */
    public static void addToHistory(Attributes attributes, String text) {
        String add = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10) +
            " " + text;
        String history = attributes.getString("history");
        if (history == null)
            history = add;
        else history += "\n" + add;
        attributes.set("history", history);
    }

    /**
     * This determines if a longName is substantially different from a destinationName
     * and should be shown on a Data Access Form.
     *
     * @param varName
     * @param longName
     * @return true if the longName is substantially different and should be shown.
     */
    public static boolean showLongName(String destinationName, String longName) {
        if (destinationName.length() >= 20)
            return false; //varName is already pretty long
        destinationName = String2.replaceAll(destinationName.toLowerCase(), " ", "");
        destinationName = String2.replaceAll(destinationName, "_", "");
        longName = String2.replaceAll(longName.toLowerCase(), " ", "");
        longName = String2.replaceAll(longName, "_", "");
        return !destinationName.equals(longName); //if not the same, show longName
    }

    /**
     * This returns a new, empty, badFileMap (a thead-safe map).
     */
    public ConcurrentHashMap newEmptyBadFileMap() {
        return new ConcurrentHashMap();
    }

    /** The name of the badFileMap file. */
    public String badFileMapFileName() {
        return datasetInfoDir() + BADFILE_TABLE_FILENAME;
    }

    /**
     * This reads a badFile table from disk and creates a thread-safe ConcurrentHashMap 
     * (key=dir#/fileName, value=Object[0=(Double)lastMod, 1=(String)reason]).
     * <br>If trouble, this won't throw an Exception and will return an empty badFileMap.
     * <br>If there are no bad files, there is no file.
     *
     * @return a thread-safe ConcurrentHashMap
     */
    public ConcurrentHashMap readBadFileMap() {
        ConcurrentHashMap badFilesMap = newEmptyBadFileMap();
        String fileName = badFileMapFileName();
        try {
            if (File2.isFile(fileName)) {
                Table badTable = new Table();
                badTable.readFlatNc(fileName, null, 0);  //it logs nRows=
                int nRows = badTable.nRows();
                int nColumns = badTable.nColumns();
                Test.ensureEqual(nColumns, 3, "Unexpected number of columns.");
                Test.ensureEqual(badTable.getColumnName(0), "fileName", "Unexpected column#0 name.");
                Test.ensureEqual(badTable.getColumnName(1), "lastMod",  "Unexpected column#1 name.");
                Test.ensureEqual(badTable.getColumnName(2), "reason",   "Unexpected column#2 name.");
                Test.ensureEqual(badTable.getColumn(0).elementClassString(), "String", "Unexpected column#0 type.");
                Test.ensureEqual(badTable.getColumn(1).elementClassString(), "double", "Unexpected column#1 type.");
                Test.ensureEqual(badTable.getColumn(2).elementClassString(), "String", "Unexpected column#2 type.");
                if (nRows == 0)
                    return badFilesMap;
                for (int row = 0; row < nRows; row++) 
                    badFilesMap.put(badTable.getStringData(0, row), 
                        new Object[]{new Double(badTable.getDoubleData(1, row)),
                                     badTable.getStringData(2, row)});
            }
            return badFilesMap;
        } catch (Throwable t) {
            EDStatic.email(EDStatic.emailEverythingTo, 
                "Error while reading table of badFiles",
                "Error while reading table of badFiles\n" + fileName + "\n" + 
                MustBe.throwableToString(t));  
            File2.delete(fileName);
            return newEmptyBadFileMap();
        }
    }

    /**
     * This makes a badFile table from a thread-safe ConcurrentHashMap  
     * (key=dir#/fileName, value=Object[0=(Double)lastMod, 1=(String)reason]).
     * and writes it to disk.
     * <br>This won't throw an Exception. If the file can't be written, nothing is done.
     * <br>If there are no bad files, don't call this. There will be no file.
     *
     * @param badFilesMap
     * @throws Throwable if trouble
     */
    public void writeBadFileMap(String randomFileName, ConcurrentHashMap badFilesMap) 
        throws Throwable {

        try {
            //gather the fileNames and reasons
            StringArray fileNames = new StringArray();
            DoubleArray lastMods  = new DoubleArray();
            StringArray reasons   = new StringArray();
            Object keys[] = badFilesMap.keySet().toArray();
            for (int k = 0; k < keys.length; k++) {
                Object o = badFilesMap.get(keys[k]);
                if (o != null) {
                    fileNames.add(keys[k].toString());
                    Object oar[] = (Object[])o;
                    lastMods.add(((Double)oar[0]).doubleValue());
                    reasons.add(oar[1].toString());
                }
            }

            //make and write the badFilesTable
            Table badTable = new Table();
            badTable.addColumn("fileName", fileNames);
            badTable.addColumn("lastMod",  lastMods);
            badTable.addColumn("reason",   reasons);
            badTable.saveAsFlatNc(randomFileName, "row");
            if (verbose) String2.log("Table of badFiles successfully written. nRows=" + badTable.nRows() + "\n" +
                randomFileName);
        } catch (Throwable t) {
            String subject = "Error while writing table of badFiles";
            EDStatic.email(EDStatic.emailEverythingTo, 
                subject,
                subject + "\n" + randomFileName + "\n" + 
                MustBe.throwableToString(t));  
            File2.delete(randomFileName);
            throw t;
        }
    }

    /** 
     * This adds fileName, lastMod, and reason to a badFiles map. 
     *
     * @param badFileMap
     * @param dirIndex   
     * @param fileName   the fileName, for example  AG20090109.nc
     * @param lastMod   the lastModified time of the file
     * @param reason
     */
    public void addBadFile(ConcurrentHashMap badFileMap, int dirIndex, String fileName, 
            double lastMod, String reason) {
        String2.log(datasetID + " addBadFile: " + fileName + "\n  reason=" + reason);
        badFileMap.put(dirIndex + "/" + fileName, new Object[]{new Double(lastMod), reason});
    }

    /** 
     * This reads the table of badFiles, adds fileName and reason, and writes the table of badFiles. 
     * This is used outside of the constructor, when a previously good file is found to be bad.
     * This won't throw an exception, just logs the message.
     *
     * @param dirIndex   
     * @param fileName   the fileName, for example  AG20090109.nc
     * @param lastMod   the lastModified time of the file
     * @param reason
     * @return an error string ("" if no error).
     */
    public String addBadFileToTableOnDisk(int dirIndex, String fileName, double lastMod, 
        String reason) {

        ConcurrentHashMap badFileMap = readBadFileMap();
        addBadFile(badFileMap, dirIndex, fileName, lastMod, reason);
        String badFileMapFileName = badFileMapFileName();
        int random = Math2.random(Integer.MAX_VALUE);
        try {
            writeBadFileMap(badFileMapFileName + random, badFileMap);
            File2.rename(badFileMapFileName + random, badFileMapFileName);
            return "";
        } catch (Throwable t) {
            File2.delete(badFileMapFileName + random);
            String msg = "Error: " + MustBe.throwableToString(t);
            String2.log(msg);
            return msg;
        }
    }

    /** 
     * This returns a string representation of the information in a badFileMap.
     * 
     * @param badFileMap
     * @param dirList
     * @return a string representation of the information in a badFileMap.
     *     If there are no badFiles, this returns "".
     */
    public String badFileMapToString(ConcurrentHashMap badFileMap, StringArray dirList) {

        Object keys[] = badFileMap.keySet().toArray();
        if (keys.length == 0) 
            return "";
        StringBuilder sb = new StringBuilder(
            "\n" +
            "********************************************\n" +
            "List of Bad Files for datasetID=" + datasetID + "\n\n");
        int nDir = dirList.size();
        Arrays.sort(keys);
        for (int k = 0; k < keys.length; k++) {
            Object o = badFileMap.get(keys[k]);
            String dir = File2.getDirectory(keys[k].toString());
            int dirI = dir.length() > 1 && dir.endsWith("/")?
                String2.parseInt(dir.substring(0, dir.length() - 1)) : -1;
            if (o != null && dirI >= 0 && dirI < nDir) { 
                Object oar[] = (Object[])o;
                sb.append(dirList.get(dirI) + File2.getNameAndExtension(keys[k].toString()) + "\n" +
                  oar[1].toString() + "\n\n"); //reason
            }
        }
        sb.append(
            "********************************************\n");
        return sb.toString();
    }


    /**
     * This returns list of &amp;-separated parts, in their original order, from a percent encoded userQuery.
     * This is like split(,'&amp;'), but smarter.
     * This accepts:
     * <ul>
     * <li>connecting &amp;'s already visible (within a part, 
     *     &amp;'s must be percent-encoded (should be) or within double quotes)
     * <li>connecting &amp;'s are percent encoded (they shouldn't be!) (within a part, 
     *     &amp;'s must be within double quotes).
     * </ul>
     *
     * @param userQuery the part after the '?', still percentEncoded, may be null.
     * @return a String[] with the percentDecoded parts, in their original order,
     *   without the connecting &amp;'s.
     *   This part#0 is always the varnames (or "" if none).
     *   A null or "" userQuery will return String[1] with #0=""
     * @throws Throwable if trouble (e.g., invalid percentEncoding)
     */
    public static String[] getUserQueryParts(String userQuery) throws Throwable {
        if (userQuery == null || userQuery.length() == 0)
            return new String[]{""};

        boolean stillEncoded = true;
        if (userQuery.indexOf('&') < 0) {
            //perhaps user percentEncoded everything, even the connecting &'s, so decode everything right away
            userQuery = SSR.percentDecode(userQuery);
            stillEncoded = false;
        }
        //String2.log("userQuery=" + userQuery);

        //one way or another, connecting &'s should now be visible
        userQuery += "&"; //& triggers grabbing final part
        int userQueryLength = userQuery.length();
        int start = 0;
        boolean inQuotes = false;
        StringArray parts = new StringArray(); 
        for (int po = 0; po < userQueryLength; po++) {
            char ch = userQuery.charAt(po);
            //String2.log("ch=" + ch);
            if (ch == '"') {             //what about \" within "..."?
                inQuotes = !inQuotes;
            } else if (ch == '&' && !inQuotes) {
                String part = userQuery.substring(start, po);
                parts.add(stillEncoded? SSR.percentDecode(part) : part);
                //String2.log("part=" + parts.get(parts.size() - 1));
                start = po + 1;
            }
        }
        if (inQuotes)
            throw new SimpleException("Query error: A closing doublequote is missing.");
        return parts.toArray();
    }

    /**
     * This returns a HashMap with the variable=value entries from a userQuery.
     *
     * @param userQuery the part after the '?', still percentEncoded, may be null.
     * @param namesLC if true, the names are made toLowerCase.
     * @return HashMap<String, String>  
     *   <br>The keys and values will be percentDecoded.
     *   <br>A null or "" userQuery will return an empty hashMap.
     *   <br>If a part doesn't have '=', then it doesn't generate an entry in hashmap.
     * @throws Throwable if trouble (e.g., invalid percentEncoding)
     */
    public static HashMap<String, String> userQueryHashMap(String userQuery, boolean namesLC) throws Throwable {
        HashMap<String, String> queryHash = new HashMap<String, String>();
        if (userQuery != null) {
            String tParts[] = getUserQueryParts(userQuery); //userQuery="" returns String[1]  with #0=""
            for (int i = 0; i < tParts.length; i++) {
                int po = tParts[i].indexOf('=');
                if (po > 0) {
                    //if (reallyVerbose) String2.log(tParts[i]);
                    String name = tParts[i].substring(0, po);
                    if (namesLC)
                        name = name.toLowerCase();
                    queryHash.put(name, tParts[i].substring(po + 1));
                }
            }
        }
        return queryHash;
    }

    /**
     * This builds a user query from the parts.
     *
     * @param queryParts not percentEncoded
     * @return a userQuery, &amp; separated, with percentEncoded parts,
     *    or "" if queryParts is null or length = 0.
     * @throws Throwable
     */
    public static String buildUserQuery(String queryParts[]) throws Throwable {
        if (queryParts == null || queryParts.length == 0)
            return "";

        for (int i = 0; i < queryParts.length; i++) {
            int po = queryParts[i].indexOf('=');
            if (po >= 0) 
                queryParts[i] = 
                    SSR.minimalPercentEncode(queryParts[i].substring(0, po)) + "=" +
                    SSR.minimalPercentEncode(queryParts[i].substring(po + 1));
            else 
                queryParts[i] = SSR.minimalPercentEncode(queryParts[i]);
        }
        return String2.toSVString(queryParts, "&", false);
    }

    /**
     * This returns the pngInfo file name for a request.
     *
     * @param loggedInAs
     * @param userDapQuery the same as used to make the image file (should be percent-encoded)
     * @param fileTypeName the same as used to make the image. e.g., .png or .smallPng
     * @return the canonical fileName string
     */
    public String getPngInfoFileName(String loggedInAs, String userDapQuery, String fileTypeName) {
        String tFileName = suggestFileName(loggedInAs, userDapQuery, fileTypeName + "Info");
        return String2.canonical(cacheDirectory() + tFileName + pngInfoSuffix);  
    }

    /** 
     * This writes pngInfo image information to a .json file (dictionary with entries).
     * If trouble, this logs the error to String2.log, but doesn't throw exception.
     * 
     * @param loggedInAs
     * @param userDapQuery the same as used to make the image file (should be percent-encoded)
     * @param fileTypeName the same as used to make the image. e.g., .png or .smallPng
     * @param mmal ArrayList returned by SgtMap.makeMap or SgtGraph.makeGraph
     * @param xMin  the double-value range of the graph
     * @param xMax  the double-value range of the graph
     * @param yMin  the double-value range of the graph
     * @param yMax  the double-value range of the graph
     */
    public void writePngInfo(String loggedInAs, String userDapQuery, String fileTypeName, ArrayList mmal) {

        String infoFileName = getPngInfoFileName(loggedInAs, userDapQuery, fileTypeName);
        synchronized (infoFileName) {
            if (File2.isFile(infoFileName)) {
                if (verbose) String2.log("  writePngInfo succeeded (file already existed)"); 
                return;
            }
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("{\n");
                PrimitiveArray pa;
                pa = (DoubleArray)mmal.get(7);
                sb.append(String2.toJson("graphDoubleWESN") + ": [" + pa.toJsonCsvString() + "],\n");
                pa = (IntArray)mmal.get(6);
                sb.append(String2.toJson("graphIntWESN") + ": [" + pa.toJsonCsvString() + "],\n");
                sb.append("}\n");

                String tError = String2.writeToFile(infoFileName, sb.toString(), "UTF-8"); //json always UTF-8
                if (tError.length() == 0) {
                    if (verbose) String2.log("  writePngInfo succeeded"); 
                } else {
                    String2.log(ERROR + " while writing pngInfo image information to\n" +
                        infoFileName + " :\n" +
                        tError);
                }
                if (reallyVerbose) String2.log(
                        "    userDapQuery=" + userDapQuery + "\n" +
                        "    fileTypeName=" + fileTypeName + "\n" +
                        "    infoFileName=" + infoFileName + "\n" + 
                        sb.toString());

            } catch (Throwable t) {
                String2.log(ERROR + " while writing pngInfo image information for\n" +
                    "  userDapQuery=" + userDapQuery + "\n" +
                    "  fileTypeName=" + fileTypeName + "\n" +
                    "  infoFileName=" + infoFileName + "\n" + 
                    MustBe.throwableToString(t));
            }
        }
    }

    /** 
     * This reads the info from a pngInfo file. 
     *
     * @param loggedInAs
     * @param userDapQuery the same as used to make the image file (should be percent-encoded)
     * @param fileTypeName the same as used to make the image. e.g., .png or .smallPng
     * @return Object[]: [0]=graphDoubleWESN[], [1]=graphIntWESN[]
     *   or null if trouble (e.g., file not found)
     */
    public Object[] readPngInfo(String loggedInAs, String userDapQuery, String fileTypeName) {
        String infoFileName = getPngInfoFileName(loggedInAs, userDapQuery, fileTypeName);
        try {
            if (reallyVerbose) 
                String2.log("  readPngInfo" +
                    "\n    userDapQuery=" + userDapQuery +
                    "\n    fileTypeName=" + fileTypeName +
                    "\n    infoFileName=" + infoFileName);

            //if the pngInfo file is at a remote ERDDAP, get it and store it as if created here
            if (this instanceof FromErddap &&
                !File2.isFile(infoFileName)) {
                FromErddap fe = (FromErddap)this;
                if (fe.sourceErddapVersion() > 1.22) {
                    //if this fails, the method fails since infoFile isn't in the dir anyway
                    String tUrl = fe.getLocalSourceErddapUrl() + fileTypeName + "Info" +
                        ((userDapQuery != null && userDapQuery.length() > 0)? "?" + userDapQuery : "");
                    if (verbose) String2.log("  readPngInfo is trying to make " + infoFileName + 
                        "\n  from remote ERDDAP: " + tUrl);
                    SSR.downloadFile(tUrl, infoFileName, true);                    
                } else {
                    if (reallyVerbose)
                        String2.log(
                            "readPngInfo: file not found: " + infoFileName + "\n" +
                            "  and remote ERDDAP version=" + fe.sourceErddapVersion() + " is too old.\n");
                    return null;
                }
            }

            if (!File2.isFile(infoFileName)) {
                if (reallyVerbose) String2.log("readPngInfo: file not found: " + infoFileName);
                return null;
            }

            //read the json pngInfo file
            String sa[] = String2.readFromFile(infoFileName, "UTF-8", 1);
            if (sa[0].length() > 0) 
                throw new Exception(sa[0]);
            JSONTokener jTok = new JSONTokener(sa[1]);
            JSONObject jDictionary = new JSONObject(jTok); 

            JSONArray jArray = jDictionary.getJSONArray("graphDoubleWESN");
            double graphDoubleWESN[] = new double[4];
            for (int i = 0; i < 4; i++)
                graphDoubleWESN[i] = jArray.getDouble(i);

            jArray = jDictionary.getJSONArray("graphIntWESN");
            int graphIntWESN[] = new int[4];
            for (int i = 0; i < 4; i++)
                graphIntWESN[i] = jArray.getInt(i);

            if (verbose)
                String2.log("  readPngInfo succeeded" +
                  "\n    graphDoubleWESN=" + String2.toCSVString(graphDoubleWESN) +
                  "\n    graphIntWESN="  + String2.toCSVString(graphIntWESN));

            return new Object[]{graphDoubleWESN, graphIntWESN};
        } catch (Throwable t) {
            String2.log(ERROR + " while reading pngInfo image information for \n" + 
                "  userDapQuery=" + userDapQuery + "\n" +
                "  fileTypeName=" + fileTypeName + "\n" +
                "  infoFileName=" + infoFileName + "\n" + 
                MustBe.throwableToString(t));
            return null;
        }
    }


    /**
     * This calls testDasDds(tDatasetID, true).
     */
    public static String testDasDds(String tDatasetID) throws Throwable {
        return testDasDds(tDatasetID, true);
    }

    /**
     * Return a dataset's .das and .dds
     * (usually for test purposes when setting up a dataset).
     */
    public static String testDasDds(String tDatasetID, boolean tReallyVerbose) throws Throwable {
        verbose = true;
        reallyVerbose = tReallyVerbose;
        Table.verbose = true;
        Table.reallyVerbose = reallyVerbose;
        EDV.verbose = true;
        NcHelper.verbose = true;
        OpendapHelper.verbose = true;
        String2.log("\n*** DasDds " + tDatasetID);
        String tName;
        StringBuilder results = new StringBuilder();
        if (false) {
            for (int i = 0; i < 3; i++)
                Math2.gc(200); //all should be garbage collected now
        }
        long memory = Math2.getMemoryInUse();

        EDD edd = oneFromDatasetXml(tDatasetID); 

        tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "EDD.testDasDds_" + tDatasetID, ".das"); 
        results.append("**************************** The .das for " + tDatasetID + " ****************************\n");
        results.append(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()) + "\n");

        tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "EDD.testDasDds_" + tDatasetID, ".dds"); 
        results.append("**************************** The .dds for " + tDatasetID + " ****************************\n");
        results.append(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()) + "\n");

        //memory
        if (false) {
            for (int i = 0; i < 3; i++)
                Math2.gc(200); //all should be garbage collected now
            memory = Math2.getMemoryInUse() - memory;
            String2.log("\n*** DasDds: memoryUse=" + (memory/1024) + 
                " KB\nPress CtrlBreak in console window to generate hprof heap info.");
            String2.getStringFromSystemIn("Press ^C to stop or Enter to continue..."); 
        }

        return results.toString();
    }

    /**
     * This sets verbose=true and reallyVerbose=true for this class
     * and related clases, for tests.
     *
     * @throws Throwable if trouble
     */
    public static void testVerboseOn() {
        testVerbose(true);
    }

    /**
     * This sets verbose=true and reallyVerbose=true for this class
     * and related clases, for tests.
     *
     * @throws Throwable if trouble
     */
    public static void testVerboseOff() {
        testVerbose(false);
    }

    /**
     * This sets verbose=on and reallyVerbose=on for this class
     * and related clases, for tests.
     *
     * @throws Throwable if trouble
     */
    public static void testVerbose(boolean on) {
        verbose = on;
        reallyVerbose = on;
        Boundaries.verbose = on;
        Boundaries.reallyVerbose = on;
        Calendar2.verbose = on;
        Calendar2.reallyVerbose = on;
        gov.noaa.pfel.coastwatch.pointdata.DigirHelper.verbose = on;
        gov.noaa.pfel.coastwatch.pointdata.DigirHelper.reallyVerbose = on;
        EDV.verbose = on;
        EDV.reallyVerbose = on;
        GridDataAccessor.verbose = on;
        GridDataAccessor.reallyVerbose = on;
        GSHHS.verbose = on;
        GSHHS.reallyVerbose = on;
        NcHelper.verbose = on;
        OpendapHelper.verbose = on;
        SgtGraph.verbose = on;
        SgtGraph.reallyVerbose = on;
        SgtMap.verbose = on;
        SgtMap.reallyVerbose = on;
        Table.verbose = on;
        Table.reallyVerbose = on;
        TableWriter.verbose = on;
        TableWriter.reallyVerbose = on;
        TaskThread.verbose = on;
        TaskThread.reallyVerbose = on;
    }
}
