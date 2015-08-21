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
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
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
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.FileVisitorSubdir;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONTokener;

import ucar.nc2.NetcdfFile;

/** 
This class represents an ERDDAP Dataset (EDD) -- 
a gridded or tabular dataset 
(usually Longitude, Latitude, Altitude/Depth, and Time-referenced)
usable by ERDDAP (ERD's Distributed Access Program).

<p>Currently, ERDDAP serves these datasets via the OPeNDAP protocol
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
<li> Standardized variable names and units for longitude, latitude, altitude/depth, and time:
  To facilitate comparisons of data from different datasets,
  the requests and results in ERDDAP use standardized space/time axis units:
  longitude is always in degrees_east; latitude is always in degrees_north;
  altitude is always in meters with positive=up; 
  depth is always in meters with positive=down; 
  time is always in seconds since 1970-01-01T00:00:00Z
  and, when formatted as a string, is formatted according to the ISO 8601 standard.
  This makes it easy to specify constraints in requests
  without having to worry about the altitude data format 
  (are positive values up or down? in meters or fathoms?)
  or time data format (a nightmarish realm of possible formats, time zones, and daylight savings).
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

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /**
     * Set this to true (by calling debugMode=true in your program, 
     * not by changing the code here)
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
     * alphabetic order.) 
     * Note that CF 1.6 section 9.4 says "The value assigned to the featureType attribute is case-insensitive". */
    public final static String[] CDM_TYPES = {
        CDM_GRID, CDM_MOVINGGRID, CDM_OTHER, CDM_POINT, CDM_PROFILE, 
        CDM_RADIALSWEEP, CDM_TIMESERIES, CDM_TIMESERIESPROFILE, CDM_SWATH, 
        CDM_TRAJECTORY, CDM_TRAJECTORYPROFILE};

    /**
     * The CF 1.6 standard featureTypes are just the point CDM_TYPES.
     * See featureType definition and table 9.1 in the CF standard.
     */
    public final static String[] CF_FEATURE_TYPES = {
        CDM_POINT, CDM_PROFILE, CDM_TIMESERIES, CDM_TIMESERIESPROFILE, 
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

    /** These are used by EDDGridFromFiles and EDDTableFromFiles for files in datasetDir(). */
    public final static String DIR_TABLE_FILENAME     = "dirTable.nc";
    public final static String FILE_TABLE_FILENAME    = "fileTable.nc";
    public final static String BADFILE_TABLE_FILENAME = "badFiles.nc";
    public final static String QUICK_RESTART_FILENAME = "quickRestart.nc";
    public final static String pngInfoSuffix = "_info.json";
    public final static String fgdcSuffix     = "_fgdc";
    public final static String iso19115Suffix = "_iso19115";

    public final static String WMS_SERVER = "request"; //last part of url for actual wms server
    public final static int WMS_MAX_LAYERS = 16; //arbitrary
    public final static int WMS_MAX_WIDTH = 4096; //arbitrary EDDGrid and EDDTable.saveAsImage now use these, too
    public final static int WMS_MAX_HEIGHT = 4096; //arbitrary
    public final static char WMS_SEPARATOR = ':'; //separates datasetID and variable name (not a valid interior char)

    public final static String ONE_WORD_CF_STANDARD_NAMES[] = {
        "altitude","cakes123","depth","geopotential","height",
        "latitude","longitude","omega","realization","region","time"};
    public final static int LONGEST_ONE_WORD_CF_STANDARD_NAMES = 12;  //characters

    public final static byte[] NOT_ORIGINAL_SEARCH_ENGINE_BYTES = String2.getUTF8Bytes(
        "In setup.xml, <searchEngine> is not 'original'.");

    public final static String KEEP_SHORT_KEYWORDS[] = {"u", "v", "w", "xi"};
    public final static String KEEP_SHORT_UC_KEYWORDS[] = {"hf", "l2", "l3", "l4", "o2", "us"};


    /** 
     * suggestReloadEveryNMinutes multiplies the original suggestion by this factor.
     * So, e.g., using 0.5 will cause suggestReloadEveryNMinutes to return
     * smaller numbers (hence more aggressive reloading).
     * Don't change this value here.  Change it in calling code as needed. 
     */
    public static double suggestReloadEveryNMinutesFactor = 1.0;
    /** 
     * This sets the minimum and maximum values that will be returned by 
     * suggestReloadEveryNMinutes.
     * Don't change this value here.  Change it in calling code as needed. 
     */
    public static int suggestReloadEveryNMinutesMin = 1;
    public static int suggestReloadEveryNMinutesMax = 2000000000;

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
    /** defaultDataQuery is used for .html if the user doesn't provide a query.
        defaultGraphQuery is used for .graph if the user doesn't provide a query.
    */
    protected String defaultDataQuery, defaultGraphQuery;


    /** These are created as needed (in the constructor) from combinedGlobalAttributes. */
    protected String id, title, summary, extendedSummaryPartB, institution, 
        infoUrl, cdmDataType;
    /** These are created as needed (in the constructor) by accessibleVia...(). */
    protected String 
        accessibleViaMAG, accessibleViaSubset, accessibleViaGeoServicesRest, 
        accessibleViaSOS, accessibleViaWCS, accessibleViaWMS, accessibleViaNcCF, 
        accessibleViaFGDC, accessibleViaISO19115; 
    protected String  accessibleViaFilesDir = ""; //default=inactive   EDStatic.filesActive must be true
    protected String  accessibleViaFilesRegex = ""; 
    protected boolean accessibleViaFilesRecursive = false;
    protected String fgdcFile, iso19115File;  //the names of pre-made, external files; or null
    protected byte[] searchBytes;
    /** These are created as needed (in the constructor) from dataVariables[]. */
    protected String[] dataVariableSourceNames, dataVariableDestinationNames;   

    /** Things related to incremental update */
    protected long lastUpdate = 0; //System.currentTimeMillis at completion of last update
    protected int updateEveryNMillis = 0; // <=0 means incremental update not active
    protected ReentrantLock updateLock = null;  //setUpdateEveryNMillis creates this if needed
    protected long cumulativeUpdateTime = 0, updateCount = 0; 

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
        String startStartError = "datasets.xml error on"; //does the error message already start with this?
        String startError      = "datasets.xml error on or before line #";
        if (type == null) 
            throw new SimpleException(startError + xmlReader.lineNumber() + 
                ": Unexpected <dataset> type=" + type + ".");
        try {
            //future: classes could be added at runtime if I used reflection
            if (type.equals("EDDGridAggregateExistingDimension")) 
                return EDDGridAggregateExistingDimension.fromXml(xmlReader);
            if (type.equals("EDDGridCopy"))             return EDDGridCopy.fromXml(xmlReader);
            if (type.equals("EDDGridFromDap"))          return EDDGridFromDap.fromXml(xmlReader);
            if (type.equals("EDDGridFromEDDTable"))     return EDDGridFromEDDTable.fromXml(xmlReader);
            if (type.equals("EDDGridFromErddap"))       return EDDGridFromErddap.fromXml(xmlReader);
            if (type.equals("EDDGridFromEtopo"))        return EDDGridFromEtopo.fromXml(xmlReader);
            if (type.equals("EDDGridFromMergeIRFiles")) return EDDGridFromMergeIRFiles.fromXml(xmlReader);
            if (type.equals("EDDGridFromNcFiles"))      return EDDGridFromNcFiles.fromXml(xmlReader);
            if (type.equals("EDDGridSideBySide"))       return EDDGridSideBySide.fromXml(xmlReader);

            if (type.equals("EDDTableCopy"))            return EDDTableCopy.fromXml(xmlReader);
            //if (type.equals("EDDTableCopyPost"))        return EDDTableCopyPost.fromXml(xmlReader); //inactive
            if (type.equals("EDDTableFromAsciiServiceNOS")) return EDDTableFromAsciiServiceNOS.fromXml(xmlReader);
            //if (type.equals("EDDTableFromBMDE"))        return EDDTableFromBMDE.fromXml(xmlReader); //inactive
            if (type.equals("EDDTableFromCassandra"))   return EDDTableFromCassandra.fromXml(xmlReader);
            if (type.equals("EDDTableFromDapSequence")) return EDDTableFromDapSequence.fromXml(xmlReader);
            if (type.equals("EDDTableFromDatabase"))    return EDDTableFromDatabase.fromXml(xmlReader);
            if (type.equals("EDDTableFromEDDGrid"))     return EDDTableFromEDDGrid.fromXml(xmlReader);
            if (type.equals("EDDTableFromErddap"))      return EDDTableFromErddap.fromXml(xmlReader);
            if (type.equals("EDDTableFromFileNames"))   return EDDTableFromFileNames.fromXml(xmlReader);
            //if (type.equals("EDDTableFromMWFS"))        return EDDTableFromMWFS.fromXml(xmlReader); //inactive as of 2009-01-14
            if (type.equals("EDDTableFromAsciiFiles"))  return EDDTableFromAsciiFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromColumnarAsciiFiles"))  return EDDTableFromColumnarAsciiFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromAwsXmlFiles")) return EDDTableFromAwsXmlFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromHyraxFiles"))  return EDDTableFromHyraxFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromNcFiles"))     return EDDTableFromNcFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromNcCFFiles"))   return EDDTableFromNcCFFiles.fromXml(xmlReader);
            //if (type.equals("EDDTableFromNOS"))         return EDDTableFromNOS.fromXml(xmlReader); //inactive 2010-09-08
            //if (type.equals("EDDTableFromNWISDV"))      return EDDTableFromNWISDV.fromXml(xmlReader); //inactive 2011-12-16
            if (type.equals("EDDTableFromOBIS"))        return EDDTableFromOBIS.fromXml(xmlReader);
            if (type.equals("EDDTableFromPostDatabase"))return EDDTableFromPostDatabase.fromXml(xmlReader);
            if (type.equals("EDDTableFromPostNcFiles")) return EDDTableFromPostNcFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromSOS"))         return EDDTableFromSOS.fromXml(xmlReader);
            if (type.equals("EDDTableFromThreddsFiles"))return EDDTableFromThreddsFiles.fromXml(xmlReader);
            if (type.equals("EDDTableFromWFSFiles"))    return EDDTableFromWFSFiles.fromXml(xmlReader);
        } catch (Throwable t) {
            String msg = MustBe.getShortErrorMessage(t);
            throw new RuntimeException(
                (msg.startsWith(startStartError)? "" : startError + xmlReader.lineNumber() + ": ") + 
                msg, t);
        }
        throw new RuntimeException(startError + xmlReader.lineNumber() + 
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
     * Special case: for combinedGlobalAttributes name="license", any instance of "[standard]"
     *   will be converted to the EDStatic.standardLicense.
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
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
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
        String errorInMethod = "datasets.xml/EDD.ensureValid error for datasetID=" + datasetID + ":\n ";

        //test that required things are set
        Test.ensureFileNameSafe(datasetID, errorInMethod + "datasetID");
        if (datasetID.indexOf('.') >= 0)
            throw new SimpleException(errorInMethod + "periods are not allowed in datasetID's.");
        datasetID = String2.canonical(datasetID); //for Lucene, useful if canonical
        //make cacheDirectory (cache cleaner in RunLoadDatasets won't remove it, 
        //  but my testing environment (2+ things running) may remove it)
        File2.makeDirectory(cacheDirectory()); 
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
        if (defaultDataQuery == null || defaultDataQuery.length() == 0) {
            //if not from <defaultDataQuery>tag, try to get from attributes
            defaultDataQuery = combinedGlobalAttributes.getString("defaultDataQuery");
        } else { 
            //make atts same as separate <defaultDataQuery> tag
            addGlobalAttributes.set("defaultDataQuery", defaultDataQuery);
            combinedGlobalAttributes.set("defaultDataQuery", defaultDataQuery);
        }
        if (defaultGraphQuery == null || defaultGraphQuery.length() == 0) {
            defaultGraphQuery = combinedGlobalAttributes.getString("defaultGraphQuery");
        } else {
            addGlobalAttributes.set("defaultGraphQuery", defaultGraphQuery);
            combinedGlobalAttributes.set("defaultGraphQuery", defaultGraphQuery);
        }
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
            " isn't one of the standard CDM types (" + String2.toCSSVString(CDM_TYPES) + ").");
        if (String2.indexOf(CF_FEATURE_TYPES, cdmDataType) >= 0)
            combinedGlobalAttributes.set("featureType", cdmDataType); //case-insensitive (see CF 1.6, section 9.4), so match ERDDAP's name
        else 
            combinedGlobalAttributes.remove("featureType"); //featureType is for point types only (table 9.1)
        Test.ensureTrue(dataVariables != null && dataVariables.length > 0, 
            errorInMethod + "'dataVariables' wasn't set.");
        for (int i = 0; i < dataVariables.length; i++) {
            Test.ensureNotNull(dataVariables[i], errorInMethod + "'dataVariables[" + i + "]' wasn't set.");
            String tErrorInMethod = errorInMethod + 
                "for dataVariable #" + i + "=" + dataVariables[i].destinationName() + ":\n";
            dataVariables[i].ensureValid(tErrorInMethod);
        }
        //ensure these are set in the constructor (they may be "")
        extendedSummary();  //ensures that extendedSummaryPartB is constructed
        accessibleViaMAG();
        accessibleViaSubset();
        accessibleViaGeoServicesRest();
        accessibleViaSOS();
        accessibleViaWCS();
        accessibleViaWMS(); 
        accessibleViaNcCF(); 
        //handle at end of EDDGrid/Table.ensureValid: 
        //  accessibleViaFGDC();  
        //  accessibleViaISO19115(); 
        if (this instanceof EDDTable)
            String2.log("  accessibleViaNcCF=" +
                (accessibleViaNcCF.length() == 0? "[true]" : accessibleViaNcCF));

        //String2.log("\n***** beginSearchString\n" + searchString() + 
        //    "\n***** endSearchString\n");
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
            " updateEveryNMillis=" + updateEveryNMillis +
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

        try {
            if (other == null) 
                return "EDSimilar: " + EDStatic.EDDChangedWasnt;

            int nDv = dataVariables.length;
            if (nDv != other.dataVariables.length)
                return EDStatic.EDDSimilarDifferentNVar + 
                    " (" + nDv + " != " + other.dataVariables.length + ")";

            for (int dv = 0; dv < nDv; dv++) {
                EDV dv1 = dataVariables[dv];
                EDV dv2 = other.dataVariables[dv];

                //destinationName
                String s1 = dv1.destinationName();
                String s2 = dv2.destinationName();
                String msg2 = "#" + dv + "=" + s1;
                if (!s1.equals(s2))
                    return MessageFormat.format(EDStatic.EDDSimilarDifferent,
                        "destinationName", msg2, "(" + s1 + " != " + s2 + ")");

                //sourceDataType
                s1 = dv1.sourceDataType();
                s2 = dv2.sourceDataType();
                if (!s1.equals(s2))
                    return MessageFormat.format(EDStatic.EDDSimilarDifferent,
                        "sourceDataType", msg2, "(" + s1 + " != " + s2 + ")");

                //destinationDataType
                s1 = dv1.destinationDataType();
                s2 = dv2.destinationDataType();
                if (!s1.equals(s2))
                    return MessageFormat.format(EDStatic.EDDSimilarDifferent,
                        "destinationDataType", msg2, "(" + s1 + " != " + s2 + ")");

                //units
                s1 = dv1.units();
                s2 = dv2.units();
                if (!Test.equal(s1, s2)) //may be null 
                    return MessageFormat.format(EDStatic.EDDSimilarDifferent,
                        "units", msg2, "(" + s1 + " != " + s2 + ")");

                //sourceMissingValue
                double d1 = dv1.sourceMissingValue();
                double d2 = dv2.sourceMissingValue();
                if (!Test.equal(d1, d2)) //says NaN==NaN is true
                    return MessageFormat.format(EDStatic.EDDSimilarDifferent,
                        "sourceMissingValue", msg2, "(" + d1 + " != " + d2 + ")");

                //sourceFillValue
                d1 = dv1.sourceFillValue();
                d2 = dv2.sourceFillValue();
                if (!Test.equal(d1, d2)) //says NaN==NaN is true
                    return MessageFormat.format(EDStatic.EDDSimilarDifferent,
                        "sourceFillValue", msg2, "(" + d1 + " != " + d2 + ")");
            }

            //they are similar
            return "";
        } catch (Throwable t) {
            return MustBe.throwableToShortString(t);
        }
    }

//    protected static String test1Changed(String msg, String diff) {
//        return diff.length() == 0? "" : msg + "\n" + diff + "\n";
//    }

//    protected static String test2Changed(String msg, String oldS, String newS) {
//        if (oldS.equals(newS))
//            return "";
//        return msg + 
//            "\n  old=" + oldS + ",\n" +
//              "  new=" + newS + ".\n";
//    }

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
            return EDStatic.EDDChangedWasnt;

        //check most important things first
        int nDv = dataVariables.length;
        StringBuilder diff = new StringBuilder();
        String oldS = "" + old.dataVariables.length;
        String newS = "" + nDv;
        if (!oldS.equals(newS)) {
            diff.append(MessageFormat.format(EDStatic.EDDChangedDifferentNVar,
                oldS, newS));
            return diff.toString(); //because tests below assume nDv are same
        }

        for (int dv = 0; dv < nDv; dv++) { 
            EDV oldDV = old.dataVariables()[dv];
            EDV newDV =     dataVariables()[dv];             
            String newName = newDV.destinationName();
            String msg2 = "#" + dv + "=" + newName;

            oldS = oldDV.destinationName();
            newS = newName;
            if (!oldS.equals(newS))
                diff.append(MessageFormat.format(EDStatic.EDDChanged2Different,
                    "destinationName", msg2, oldS, newS) + "\n");

            oldS = oldDV.destinationDataType(); 
            newS = newDV.destinationDataType();
            if (!oldS.equals(newS))
                diff.append(MessageFormat.format(EDStatic.EDDChanged2Different,
                    "destinationDataType", msg2, oldS, newS) + "\n");

            String s = String2.differentLine(
                oldDV.combinedAttributes().toString(), 
                newDV.combinedAttributes().toString());
            if (s.length() > 0)
                diff.append(MessageFormat.format(EDStatic.EDDChanged1Different,
                    "combinedAttribute", msg2, s) + "\n");
        }

        //check least important things last
        String s = String2.differentLine(
            old.combinedGlobalAttributes().toString(), 
                combinedGlobalAttributes().toString());
        if (s.length() > 0)
            diff.append(MessageFormat.format(EDStatic.EDDChangedCGADifferent,
                s) + "\n");

        return diff.toString();    
    }

    /**
     * Update rss.
     * If there is an error, this just writes error to log file and returns "". This won't throw an exception.
     *
     * @param erddap if not null, new rss doc will be put in erddap.rssHashMap
     * @param change a description of what changed 
     *    (if null or "", nothing will be done and this returns "")
     * @return the rss document
     */
    public String updateRSS(Erddap erddap, String change) {
        if (change == null || change.length() == 0)
            return "";
        try {
            //generate the rss xml
            //See general info: http://en.wikipedia.org/wiki/RSS_(file_format)
            //  background: http://www.mnot.net/rss/tutorial/
            //  rss 2.0 spec: http://cyber.law.harvard.edu/rss/rss.html
            //I chose rss 2.0 for no special reason (most modern version of that fork; I like "simple").
            //The feed programs didn't really care if just pubDate changed.
            //  They care about item titles changing.
            //  So this treats every change as a new item with a different title, 
            //    replacing the previous item.
            StringBuilder rss = new StringBuilder();
            GregorianCalendar gc = Calendar2.newGCalendarZulu();
            String pubDate = 
                "    <pubDate>" + Calendar2.formatAsRFC822GMT(gc) + "</pubDate>\n";
            String link = 
                "    <link>" + EDStatic.publicErddapUrl(getAccessibleTo() == null) +
                    "/" + dapProtocol() + "/" + datasetID();
            rss.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rss version=\"2.0\" xmlns=\"http://backend.userland.com/rss2\">\n" +
                "  <channel>\n" +
                "    <title>ERDDAP: " + XML.encodeAsXML(title()) + "</title>\n" +
                "    <description>This RSS feed changes when the dataset changes.</description>\n" +      
                link + ".html</link>\n" +
                pubDate +
                "    <item>\n" +
                "      <title>This dataset changed " + Calendar2.formatAsISODateTimeT(gc) + "Z</title>\n" +
                "  " + link + ".html</link>\n" +
                "      <description>" + XML.encodeAsXML(change) + "</description>\n" +      
                "    </item>\n" +
                "  </channel>\n" +
                "</rss>\n");

            //store the xml
            String rssString = rss.toString();
            if (erddap != null)
                erddap.rssHashMap.put(datasetID(), String2.getUTF8Bytes(rssString));
            return rssString;

        } catch (Throwable rssT) {
            String2.log(String2.ERROR + " in updateRSS for " + datasetID() + ":\n" + 
                MustBe.throwableToString(rssT));
            return "";
        }

    }


    /**
     * The directory in which information for this dataset (e.g., fileTable.nc) is stored.
     *
     */
    public String datasetDir() {
        return datasetDir(datasetID);
    }

    /**
     * The directory in which information for a dataset (e.g., fileTable.nc) is stored.
     * EDStatic.fullDatasetDirectory/[last2char]/tDatasetID/
     */
    public static String datasetDir(String tDatasetID) {
        return EDStatic.fullDatasetDirectory + 
            (tDatasetID.length() <= 2? tDatasetID : tDatasetID.substring(tDatasetID.length() - 2)) +
            "/" +
            tDatasetID + "/";
    }

    /** 
     * This deletes the specified dataset's cached dataset info.
     * No error if it doesn't exist.
     */
    public static void deleteCachedDatasetInfo(String tDatasetID) {
        String dir = datasetDir(tDatasetID);
        File2.delete(dir + DIR_TABLE_FILENAME);
        File2.delete(dir + FILE_TABLE_FILENAME);
        File2.delete(dir + BADFILE_TABLE_FILENAME);
        File2.delete(dir + QUICK_RESTART_FILENAME);
    }

    /** 
     * This deletes this dataset's cached dataset info.
     * No error if it doesn't exist.
     */
    public void deleteCachedDatasetInfo() {
        deleteCachedDatasetInfo(datasetID);
    }


    /**
     * The full name of the quick restart .nc file for this dataset.
     *
     */
    public String quickRestartFullFileName() {
        return quickRestartFullFileName(datasetID);
    }

    /**
     * The full name of the quick restart .nc file for a dataset.
     */
    public static String quickRestartFullFileName(String tDatasetID) {
        return datasetDir(tDatasetID) + QUICK_RESTART_FILENAME;
    }

    /**
     * This is called by quickRestart system in an EDD constructor
     * to ensure that the quickRestart information is recent.
     * If it is out-of-date, this deletes the specified quickRestart file, 
     * sets a flag dataset reloadASAP, and throws a RuntimeException.
     *
     * ERDDAP could go ahead with the construction (as if no quickRestart info),
     * but that would slow down the quickRestart.
     *
     * @param tDatasetID
     * @param tReloadEveryNMinutes usually from getReloadEveryNMinutes() so valid (or default)
     * @param quickRestartInfoTimeMillis usually the age of quickRestartFileName
     * @param quickRestartFileName will be deleted if too old
     */
    public static void ensureQuickRestartInfoIsRecent(String tDatasetID, 
        int tReloadEveryNMinutes,
        long quickRestartInfoTimeMillis, String quickRestartFileName) {

        long minutesOld = quickRestartInfoTimeMillis <= 0?  //see edd.setCreationTimeTo0
            Long.MAX_VALUE :
            (System.currentTimeMillis() - quickRestartInfoTimeMillis) / 60000; 
        if (minutesOld > tReloadEveryNMinutes) {
            File2.delete(quickRestartFileName);
            requestReloadASAP(tDatasetID);
            throw new SimpleException(
                "\n(This is a diagnostic message, not a real error!) The quickRestart information is too old (" + minutesOld + " > " + tReloadEveryNMinutes + " minutes).\n" +
                "So ERDDAP abandoned construction, deleted the quickRestart file, and called requestReloadASAP.\n" +
                "The dataset will be reloaded right after this major LoadDatasets finishes.");
        }
    }


    /** 
     * The directory to be used for caching files for this dataset (with "/" at end).
     * ensureValid() creates this for each dataset.
     * The cache cleaner in RunLoadDatasets won't remove it, 
     *   but my testing environment (2+ things running) may remove it.
     */
    public String cacheDirectory() {
        return cacheDirectory(datasetID);
    }

    /** The directory to be used for caching files for this dataset (with "/" at end).
     */
    public static String cacheDirectory(String tDatasetID) {

        return EDStatic.fullCacheDirectory + 
            (tDatasetID.length() <= 2? tDatasetID : tDatasetID.substring(tDatasetID.length() - 2)) +
            "/" +
            tDatasetID + "/"; //dir is created by EDD.ensureValid
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
            "<a rel=\"alternate\" type=\"application/rss+xml\" " +
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
            "<a rel=\"alternate\" \n" +
            "  href=\"" + EDStatic.erddapUrl(loggedInAs) + "/" + Subscriptions.ADD_HTML + 
                "?datasetID=" + datasetID+ "&amp;showErrors=false&amp;email=\" \n" +
            "  title=\"\"><img alt=\"Subscribe\" align=\"bottom\" \n" +
            "    title=\"" + XML.encodeAsHTMLAttribute(EDStatic.subscriptionEmail) + "\" \n" +
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
                //if (reallyVerbose) 
                //    String2.log("      tags=" + xmlReader.allTags() + 
                //        " name=" + tName + " type=" + tType + " content=" + content);
                if (!String2.isSomething(tName))
                    throw new IllegalArgumentException("datasets.xml error on line #" + xmlReader.lineNumber() +
                        ": An <att> tag doesn't have a \"name\" attribute.");
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
                //    String2.log(">>EDD attribute name=\"" + tName + "\" content=" + content + 
                //    "\n  type=" + pa.elementClassString() + " pa=" + pa.toString());
                tAttributes.add(tName, pa);
                //String2.log(">>????EDD _FillValue=" + tAttributes.get("_FillValue"));

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
        boolean showMessage = false;
        String message = showMessage? 
            datasetID + " accessibleTo=" + String2.toSSVString(accessibleTo) + 
            "\n  user roles=" + String2.toSSVString(roles) +
            "\n  accessible=" :
            ""; 

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
     * This indicates why the dataset isn't accessible via the ESRI GeoServices REST
     * specification (or "" if it is).
     */
    public abstract String accessibleViaGeoServicesRest();

    /** 
     * This indicates why the dataset isn't accessible via WCS
     * (or "" if it is).
     */
    public abstract String accessibleViaWCS();

    /** 
     * This indicates why the dataset isn't accessible via .ncCF and .ncCFMA file types
     * (or "" if it is).
     * Currently, this is only for some of the Discrete Sampling Geometries cdm_data_type 
     * representations at
     * http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#discrete-sampling-geometries
     */
    public abstract String accessibleViaNcCF();

    /** 
     * This indicates why the dataset isn't accessible via WMS
     * (or "" if it is).
     */
    public abstract String accessibleViaWMS();

    /** 
     * This indicates the base directory if the dataset is accessible via the 
     * /files/ service (or "" if it isn't available).
     */
    public String accessibleViaFilesDir() {
        return accessibleViaFilesDir;
    }

    /** 
     * This indicates the file name regex if accessibleViaFilesDir != "".
     */
    public String accessibleViaFilesRegex() {
        return accessibleViaFilesRegex;
    }

    /** 
     * If accessibleViaFiles isn't "", this indicates if subdirectories
     * are available via the /files/ service.
     */
    public boolean accessibleViaFilesRecursive() {
        return accessibleViaFilesRecursive;
    }

    /** 
     * This returns a fileTable (formatted like 
     * FileVisitorDNLS.oneStep(tDirectoriesToo=false, last_mod is LongArray,
     * and size is LongArray of epochMillis)
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     */
    public Table accessibleViaFilesFileTable() {
        return null;
    }

    /** 
     * This indicates why the dataset isn't accessible via the FGDC service
     * (or "" if it is).
     */
    public String accessibleViaFGDC() {

        if (accessibleViaFGDC == null) {

            if (EDStatic.fgdcActive) {

                //see if error while creating the FGDC file
                //(The constructor calls this, so no need to be careful about concurrency.)
                accessibleViaFGDC = String2.canonical("");
                String tmp = ".tmp";
                File2.makeDirectory(datasetDir());
                String tName = datasetDir() + datasetID + fgdcSuffix + ".xml";
                try {
                    //is a pre-made, external file available?
                    if (fgdcFile == null) {  
                        //No.  Write fgdc to temp file
                        StringWriter writer = new StringWriter(65536);  //most are ~40KB
                        writeFGDC(writer);
                        accessibleViaFGDC = String2.canonical(
                            String2.writeToFile(
                                tName + tmp, writer.toString(), "UTF-8"));

                        //then swap into place to replace old version quickly
                        if (accessibleViaFGDC.length() == 0)
                            File2.rename(tName + tmp, tName);
                        else File2.delete(tName + tmp);                

                    } else {
                        //Yes.  fgdcFile is a filename, copy the file
                        if (fgdcFile.length() > 0 && File2.isFile(fgdcFile)) {
                            if (!File2.copy(fgdcFile, tName)) 
                                throw new SimpleException(
                                    MessageFormat.format(EDStatic.errorCopyFrom, fgdcFile));
                        } else {
                            throw new SimpleException(
                                MessageFormat.format(EDStatic.errorFileNotFound, 
                                    "fgdcFile=\"" + fgdcFile + "\""));
                        }
                    }
                    
                } catch (Throwable t) {
                    String2.log(MessageFormat.format(
                        EDStatic.noXxxBecause2,
                        "FGDC", 
                        (t instanceof SimpleException?
                            MustBe.getShortErrorMessage(t) :
                            MustBe.throwableToString(t))));
                    if (accessibleViaFGDC.length() == 0)
                        accessibleViaFGDC = String2.canonical(MustBe.getShortErrorMessage(t));
                    File2.delete(tName + tmp);                
                    File2.delete(tName);                
                }  
            } else {
                accessibleViaFGDC = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause2,      "FGDC",
                        MessageFormat.format(EDStatic.noXxxNotActive, "FGDC")));
            }
        }
        return accessibleViaFGDC;
    }

    /** 
     * This indicates why the dataset isn't accessible via the ISO 19115 service
     * (or "" if it is).
     */
    public String accessibleViaISO19115() {

        if (accessibleViaISO19115 == null) {

            if (EDStatic.iso19115Active) {

                //create the ISO19115 file
                //(The constructor calls this, so no need to be careful about concurrency.)
                accessibleViaISO19115 = String2.canonical("");
                String tmp = ".tmp";
                File2.makeDirectory(datasetDir());
                String tName = datasetDir() + datasetID + iso19115Suffix + ".xml";
                try {
                    //is a pre-made, external file available?
                    if (iso19115File == null) {
                        //No.  Write iso19115 to temp file
                        StringWriter writer = new StringWriter(65536);  //most are ~40KB
                        writeISO19115(writer);
                        accessibleViaISO19115 = String2.canonical(
                            String2.writeToFile(
                                tName + tmp, writer.toString(), "UTF-8"));

                        //then swap into place to replace old version quickly
                        if (accessibleViaISO19115.length() == 0)
                            File2.rename(tName + tmp, tName);
                        else File2.delete(tName + tmp);                

                    } else {
                        //Yes.  iso19115File is a filename, copy the file
                        if (iso19115File.length() > 0 && File2.isFile(iso19115File)) {
                            if (!File2.copy(iso19115File, tName)) 
                                throw new SimpleException(
                                    MessageFormat.format(EDStatic.errorCopyFrom, iso19115File));
                        } else {
                            throw new SimpleException(
                                MessageFormat.format(EDStatic.errorFileNotFound, 
                                    "iso19115File=\"" + iso19115File + "\""));
                        }
                    }

                } catch (Throwable t) {
                    String2.log(MessageFormat.format(
                        EDStatic.noXxxBecause2,
                        "ISO 19115-2/19139", 
                        (t instanceof SimpleException?
                            MustBe.getShortErrorMessage(t) :
                            MustBe.throwableToString(t))));
                    if (accessibleViaISO19115.length() == 0)
                        accessibleViaISO19115 = String2.canonical(
                            MustBe.getShortErrorMessage(t));
                    File2.delete(tName + tmp);                
                    File2.delete(tName);                
                }
            } else {
                accessibleViaISO19115 = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause2,      "ISO 19115-2/19139",
                        MessageFormat.format(EDStatic.noXxxNotActive, "ISO 19115-2/19139")));
            }
        }
        return accessibleViaISO19115;
    }



    /** 
     * This writes the dataset's FGDC-STD-012-2002
     * "Content Standard for Digital Geospatial Metadata: Extensions for Remote Sensing Metadata"
     * XML to the writer.
     *
     * <p>This is usually just called by the dataset's constructor, 
     * at the end of EDDTable/Grid.ensureValid.
     * 
     * @param writer a UTF-8 writer
     * @throws Throwable if trouble
     */
    protected abstract void writeFGDC(Writer writer) throws Throwable;

    /** 
     * This writes the dataset's ISO 19115-2/19139 XML to the writer.
     * <br>The template is initially based on THREDDS ncIso output from
     * <br>http://oceanwatch.pfeg.noaa.gov/thredds/iso/satellite/MH/chla/8day
     * <br>(stored on Bob's computer as F:/programs/iso19115/threddsNcIsoMHchla8dayYYYYMM.xml).
     * <br>Made pretty via TestAll: XML.prettyXml(in, out);
     *
     * <p>Help with schema: http://www.schemacentral.com/sc/niem21/e-gmd_contact-1.html
     * <br>List of nilReason: http://www.schemacentral.com/sc/niem21/a-gco_nilReason.html
     * 
     * <p>This is usually just called by the dataset's constructor, 
     * at the end of EDDTable/Grid.ensureValid.
     * 
     * @param writer a UTF-8 writer
     * @throws Throwable if trouble
     */
    protected abstract void writeISO19115(Writer writer) throws Throwable;


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
     * The defaultDataQuery is used if the user requests .html with no query. 
     * 
     * @return the defaultDataQuery.  Won't be null. May be "".
     */
    public String defaultDataQuery() {
        return defaultDataQuery == null? "" : defaultDataQuery;
    }

    /** 
     * The defaultGraphQuery is used if the user requests .graph with no query. 
     * 
     * @return the defaultGraphQuery.  Won't be null. May be "".
     */
    public String defaultGraphQuery() {
        return defaultGraphQuery == null? "" : defaultGraphQuery;
    }

    /** 
     * The cdm_data_type global attribute identifies the type of data. 
     * Valid values include the CF featureType's + Grid.
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

        //split it  (this makes duplicates, so sort and remove them)
        //if (kw.indexOf('>') >= 0)   //gcmd keywords may be separated with '>'
        //    return String2.split(kw, '>');
        //if (kw.indexOf('|') >= 0)  
        //    return String2.split(kw, '|');
        //StringArray sa = StringArray.wordsAndQuotedPhrases(kw); //comma or space separated
        StringArray sa = StringArray.fromCSV(kw);
        sa.sortIgnoreCase();
        sa.removeDuplicates();
        return sa.toArray();
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
            MessageFormat.format(EDStatic.errorNotFound, 
                "sourceVariableName=" + tSourceName));
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
            MessageFormat.format(EDStatic.errorNotFound, 
                "destinationVariableName=" + tDestinationName));
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
     * updateEveryNMillis indicates how often this program should check
     * for new data for this dataset and do an incremental update, e.g., 1000. 
     * 
     * @return the number of milliseconds until dataset needs an incremental update
     */
    public int getUpdateEveryNMillis() {return updateEveryNMillis; }

    /** 
     * This sets updateEveryNMillis.
     * 
     * @param tUpdateEveryNMillis Use 0 to never update.  (&lt;=0 and Integer.MAX_VALUE are treated as 0.)
     */
    public void setUpdateEveryNMillis(int tUpdateEveryNMillis) {
        updateEveryNMillis = 
            tUpdateEveryNMillis < 1 || tUpdateEveryNMillis == Integer.MAX_VALUE? 0 : 
                tUpdateEveryNMillis;
        if (updateEveryNMillis > 0 && updateLock == null)
            updateLock = new ReentrantLock();
    }

    /**
     * This provides the framework for the updateEveryNMillis system 
     * to do a quick incremental update of this dataset (i.e., for real time datasets),
     * but leaves the class specific work to lowUpdate() (which here does nothing,
     * but which subclasses like EDDGridFromDap overwrite).
     * 
     * <p>Concurrency issue: This avoids 2+ simultaneous updates.
     *
     * <p>See &lt;updateEveryNMillis&gt; in constructor. 
     * Note: It is pointless and counter-productive to set updateEveryNMillis 
     * to be less than a fairly reliable update time (e.g., 1000 ms).
     *
     * @return true if a change was made
     * @throws Throwable if serious trouble. 
     *   For simple failures, this writes info to log.txt but doesn't throw an exception.
     *   If the dataset has changed in a serious / incompatible way and needs a full
     *     reload, this throws WaitThenTryAgainException 
     *     (usually, catcher calls LoadDatasets.tryToUnload(...) and EDD.requestReloadASAP(tDatasetID))..
     *   If the changes needed are probably fine but are too extensive to deal with here, 
     *     this calls EDD.requestReloadASAP(tDatasetID) and returns without doing anything.
     */
    public boolean update() throws Throwable {
        //return quickly if update system isn't active for this dataset
        if (updateEveryNMillis <= 0)
            return false;

        //return quickly if dataset doesn't need to be updated
        long startUpdateMillis = System.currentTimeMillis();
        if (startUpdateMillis - lastUpdate < updateEveryNMillis) {
            if (reallyVerbose) String2.log("update(" + datasetID + 
                "): no need to update:  startUpdateMillis-last=" + 
                (startUpdateMillis - lastUpdate) + " < updateEvery=" + updateEveryNMillis);
            return false;
        }

        //if another thread is currently updating this dataset, wait for it then return
        String msg = "update(" + datasetID + "): ";
        if (!updateLock.tryLock()) {
            updateLock.lock();   //block until other thread's update finishes and I get the lock
            updateLock.unlock(); //immediately unlock and return (since other thread did the update)
            if (verbose) String2.log(msg + "waited " + 
                (System.currentTimeMillis() - startUpdateMillis) +
                "ms for another thread to do the update.");
            return false; 
        }

        //updateLock is locked by this thread.   Do the update!
        try {
            return lowUpdate(msg, startUpdateMillis);

        } finally {  
            lastUpdate = startUpdateMillis;     //say dataset is now up-to-date (or at least tried)
            updateLock.unlock();  //then ensure updateLock is always unlocked
        }
    }

    /**
     * This does the actual incremental update of this dataset 
     * (i.e., for real time datasets).
     * This stub in EDD does nothing, but subclasses (like EDDGridFromDap) overwrite this
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
        return false;
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
     * <p>Normal use: if a true source error occurs while getting data
     *   (e.g., not ClientAbortException) 
     *   then edd throws a gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException.
     *   which ERDDAP will catch and then call LoadDatasets.tryToUnload and requestReloadASAP().
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
     * This is a case-insensitve search.
     * This uses a Boyer-Moore-like search (see String2.indexOf(byte[], byte[], int[])).
     *
     * @param words the words or phrases to be searched for (already lowercase)
     *    stored as byte[] via word.getBytes("UTF-8").
     * @param jump the jumpTables from String2.makeJumpTable(word).
     * @return a rating value for this dataset (lower numbers are better),
     *   or Integer.MAX_VALUE if words.length == 0 or 
     *   one of the words wasn't found or a negative search word was found.
     */
    public int searchRank(boolean isNegative[], byte words[][], int jump[][]) {
        if (words.length == 0)
            return Integer.MAX_VALUE;
        int rank = 0;
        byte tSearchBytes[] = searchBytes(); //hold on, since it may be recreated each time
        for (int w = 0; w < words.length; w++) {
            if (words[w].length == 0) //search word was removed
                continue;

            int po = String2.indexOf(tSearchBytes, words[w], jump[w]);

            //word not found
            if (isNegative[w]) {
                if (po >= 0)
                    return Integer.MAX_VALUE;
            } else {
                if (po < 0)
                    return Integer.MAX_VALUE;
            }
            rank += po;
            //Exact penalty value doesn't really matter. Any large penalty will
            //  force rankings to be ranked by n words found first, 
            //  then the quality of those found.
            //rank += po < 0? penalty : po;  
        }
        //special case of deprecated datasets
        if (title().indexOf("DEPRECATED") >= 0)
            rank += 400;
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
     * This makes/returns the searchBytes that originalSearchEngine searchRank searches.
     *
     * @return the searchBytes that searchRank searches.
     */
    public byte[] searchBytes() {

        if (searchBytes == null) {
            byte tSearchBytes[] = String2.getUTF8Bytes(searchString().toLowerCase());
            if (EDStatic.useOriginalSearchEngine)
                searchBytes = tSearchBytes; //cache it
            else return tSearchBytes;       //don't cache it (10^6 datasets?!) (uses should be rare)
            //was NOT_ORIGINAL_SEARCH_ENGINE_BYTES;
        }
        return searchBytes;
    }
    
    /**
     * This makes the searchString (mixed case) used to create searchBytes or searchDocument.
     *
     * @return the searchString (mixed case) used to create searchBytes or searchDocument.
     */
    public abstract String searchString();


    protected StringBuilder startOfSearchString() {

        //make a string to search through
        StringBuilder sb = new StringBuilder();
        sb.append("all\n");
        sb.append("title=" + title() + "\n");
        sb.append("datasetID=" + datasetID + "\n");
        //protocol=...  is suggested in Advanced Search for text searches from protocols
        //protocol=griddap and protocol=tabledap *were* mentioned in searchHintsHtml, now commented out
        sb.append("protocol=" + dapProtocol() + "\n");  
        sb.append("className=" + className + "\n");
        if (accessibleViaSOS().length()      == 0) sb.append("protocol=SOS\n");
        if (accessibleViaWCS().length()      == 0) sb.append("protocol=WCS\n");
        if (accessibleViaWMS().length()      == 0) sb.append("protocol=WMS\n");
        if (accessibleViaFGDC().length()     == 0) sb.append("service=FGDC\n");
        if (accessibleViaISO19115().length() == 0) sb.append("service=ISO19115\n");
        if (accessibleViaMAG().length()      == 0) sb.append("service=MakeAGraph\n");
        if (accessibleViaNcCF().length()     == 0) sb.append("service=NcCF\n");
        if (accessibleViaSubset().length()   == 0) sb.append("service=Subset\n");
        for (int dv = 0; dv < dataVariables.length; dv++) {
            sb.append("variableName=" + dataVariables[dv].destinationName() + "\n");
            sb.append("sourceName="   + dataVariables[dv].sourceName() + "\n");
            sb.append("long_name="    + dataVariables[dv].longName() + "\n");
        }
        sb.append(combinedGlobalAttributes.toString() + "\n");
        for (int dv = 0; dv < dataVariables.length; dv++) 
            sb.append(dataVariables[dv].combinedAttributes().toString() + "\n");
        return sb;
    }

    /**
     * This makes/returns the Document that Lucene searches.
     * Subclasses may implement this.
     *
     * @return the Document that Lucene searches.
     */
    public Document searchDocument() {

        Document doc = new Document();
        //Store specifies if the original string also needs to be stored as is
        //  (e.g., so I can retrieve datasetID field from a matched document).
        //ANALYZED breaks string into tokens;  NOT_ANALYZED treats string as 1 token.
        //"datasetID" Store.YES lets me later figure out which dataset a given document is for.
        doc.add(new Field("datasetID",                 datasetID,      Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(EDStatic.luceneDefaultField, searchString(), Field.Store.NO,  Field.Index.ANALYZED));
        
        //Do duplicate searches of title (boost=10, plus it is shorter, so scores higher), 
        //  so score from lucene and original are closer. 
        //!!! FUTURE: support separate searches within the datasets' titles
        //   If so, add support in searchEngine=original, too.
        Field field = new Field("title",               title(),        Field.Store.NO,  Field.Index.ANALYZED);
        field.setBoost(10);
        doc.add(field);
        return doc;
    }


    /* * This is like startOfSearchString but creates the start of a Document for Lucene. */
    /*protected Document startOfSearchDocument() {

        StringBuilder sb = new StringBuilder();

        field = new Field("title", title(), fs, fi);
        field.setBoost(20);
        doc.add(field);

        field = new Field("datasetID", datasetID, fs, fi);
        field.setBoost(10);
        doc.add(field);

        sb.setLength(0);
        for (int dv = 0; dv < dataVariables.length; dv++) 
            sb.append(dataVariables[dv].destinationName() + ", ");
        field = new Field("variableName", sb.toString(), fs, fi);
        field.setBoost(10);
        doc.add(field);

        sb.setLength(0);
        for (int dv = 0; dv < dataVariables.length; dv++) 
            sb.append(dataVariables[dv].sourceName() + ", ");
        field = new Field("variableSourceName", sb.toString(), fs, fi);
        field.setBoost(5);
        doc.add(field);

        sb.setLength(0);
        for (int dv = 0; dv < dataVariables.length; dv++) 
            sb.append(dataVariables[dv].longName() + ", ");
        field = new Field("variableLongName", sb.toString(), fs, fi);
        field.setBoost(5);
        doc.add(field);

        sb.setLength(0);
        sb.append(combinedGlobalAttributes.toString());
        String2.replaceAll(sb, "\"", ""); //no double quotes (esp around attribute values)
        String2.replaceAll(sb, "\n    ", "\n"); //occurs for all attributes
        field = new Field("globalAttributes", sb.toString(), fs, fi);
        field.setBoost(2);
        doc.add(field);

        sb.setLength(0);
        for (int dv = 0; dv < dataVariables.length; dv++) 
            sb.append(dataVariables[dv].combinedAttributes().toString() + "\n");
        String2.replaceAll(sb, "\"", ""); //no double quotes (esp around attribute values)
        String2.replaceAll(sb, "\n    ", "\n"); //occurs for all attributes
        field = new Field("variableAttributes", sb.toString(), fs, fi);
        field.setBoost(1);
        doc.add(field);

        //protocol=...  is suggested in Advanced Search for text searches from protocols
        //protocol=griddap and protocol=tabledap *were* mentioned in searchHintsHtml, 
        //  now commented out
        sb.setLength(0);
        sb.append(dapProtocol());  
        if (accessibleViaSOS().length()      == 0) sb.append(", SOS");
        if (accessibleViaWCS().length()      == 0) sb.append(", WCS");
        if (accessibleViaWMS().length()      == 0) sb.append(", WMS");
        field = new Field("protocol", sb.toString(), fs, fi);
        field.setBoost(1);
        doc.add(field);

        sb.setLength(0);
        if (accessibleViaFGDC().length()     == 0) sb.append("FGDC, ");
        if (accessibleViaISO19115().length() == 0) sb.append("ISO19115, ");
        if (accessibleViaMAG().length()      == 0) sb.append("MakeAGraph, ");
        if (accessibleViaNcCF().length()     == 0) sb.append("NcCF, ");
        if (accessibleViaSubset().length()   == 0) sb.append("Subset, ");
        field = new Field("service", sb.toString(), fs, fi);
        field.setBoost(1);
        doc.add(field);

        field = new Field("className", "className", fs, fi);
        field.setBoost(1);
        doc.add(field);

        field = new Field("all", "all", fs, fi);
        field.setBoost(1);
        doc.add(field);

        return doc;
    } */


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
     * E.g., dataFileTypeName=".ncCF" returns dataFileTypeExtension=".nc".
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

        if (".fgdc".equals(fileTypeName))     return datasetID + fgdcSuffix;    //without trailing .xml
        if (".iso19115".equals(fileTypeName)) return datasetID + iso19115Suffix;//without trailing .xml
        if (".ncml".equals(fileTypeName))     return datasetID + "_ncml";       //without trailing .xml

        //include fileTypeName in hash so, e.g., different sized .png 
        //  have different file names
        String name = datasetID + "_" + //so all files from this dataset will sort together
            String2.md5Hex12(userDapQuery + fileTypeName); 
        //String2.log("%% suggestFileName=" + name + "\n  from query=" + userDapQuery + "\n  from type=" + fileTypeName);
        return name;
    }

    /**
     * Given the last time value, this suggests a reloadEveryNMinutes value.
     *
     * <br>This is just a suggestion. It makes big assumptions. But for e.g., UAF,
     * any guess is probably better than none.
     * It also depends on how aggressive you want to be. 
     * This is not very aggressive.
     *
     * @param epochSecondsLastTime the last time value (of the time data, not 
     *    the time it was updated).
     *    I know this may be startTime or centeredTime of a composite.
     *    I know this may catch a recent update promptly, by chance. 
     *    This method does the best it can.
     * @return a reloadEveryNMinutes value for the dataset.
     *    Currently, 60 min is the shortest suggestion returned. 
     *    EDD.DEFAULT_RELOAD_EVERY_N_MINUTES (10080) is only returned as the default
     *    (e.g., for epochSecondsLastTime=Integer.MAX_VALUE) and is unaffected by 
     *    suggestReloadEveryNMinutesFactor.
     */
    public static int suggestReloadEveryNMinutes(double epochSecondsLastTime) {
        if (!Math2.isFinite(epochSecondsLastTime) || Math.abs(epochSecondsLastTime) > 1e18)
            return DEFAULT_RELOAD_EVERY_N_MINUTES;  //DEFAULT is only returned as the default
        double daysAgo = (System.currentTimeMillis()/1000 - epochSecondsLastTime) / 
            Calendar2.SECONDS_PER_DAY;
        int snm = 
            daysAgo < -370?  Calendar2.MINUTES_PER_30DAYS :   //1+ yr  forecast,       update monthly (might be updated)
            daysAgo < -32?   8 * Calendar2.MINUTES_PER_DAY :  //1-12 month forecast,   update every 8 days 
            daysAgo < -11?    Calendar2.MINUTES_PER_DAY :     //11-32 day forecast,    update daily
            daysAgo < -1.2?  180 :                            //1-11 day forecast,     update every 3 hours
            daysAgo < 0.5?   60 :                             //1day forcst/12hr delay,update hourly
            daysAgo < 2.5?   180 :                            //1-2.5 days delay,      update every 3 hours
            daysAgo < 8?     Calendar2.MINUTES_PER_DAY :      //2.5-8 days delay,      update daily
            daysAgo < 33?    2 * Calendar2.MINUTES_PER_DAY :  //week-month delay,      update every 2 days
            daysAgo < 63?    4 * Calendar2.MINUTES_PER_DAY :  //1-2 month delay,       update every 4 days
            daysAgo < 370?   8 * Calendar2.MINUTES_PER_DAY :  //2-12 month delay,      update every 8 days
                             Calendar2.MINUTES_PER_30DAYS;    //1+year delay,          update monthly
        snm = Math2.roundToInt(suggestReloadEveryNMinutesFactor * snm);
        snm = Math.min(suggestReloadEveryNMinutesMax, snm);
        snm = Math.max(suggestReloadEveryNMinutesMin, snm);
        return snm;
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
     * to an html document (e.g., the top of a Data Access Form).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param writer
     * @param showDafLink if true, a link is shown to this dataset's Data Access Form
     * @param showSubsetLink if true, a link is shown to this dataset's .subset form
     *    (if accessibleViaSubset() is "").
     * @param showFilesLink if true, a link is shown to this dataset's /files/ page
     *    (if accessibleViaFiles() is "").
     * @param showGraphLink if true, a link is shown to this dataset's Make A Graph form
     *    (if accessibleViaMAG() is "").
     * @param userDapQuery  the part of the user's request after the '?', still percentEncoded, may be null.
     * @param otherRows  additional html content
     * @throws Throwable if trouble
     */
    public void writeHtmlDatasetInfo(
        String loggedInAs, Writer writer, 
        boolean showSubsetLink, boolean showDafLink, boolean showFilesLink, boolean showGraphLink,
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
            XML.encodeAsHTMLAttribute("?" + userDapQuery); 
        String dapUrl = tErddapUrl + "/" + dapProtocol() + "/" + datasetID;
        String dapLink = "", subsetLink = "", graphLink = "", filesLink = "";
        if (showDafLink) 
            dapLink = 
                "     | <a rel=\"alternate\" rev=\"alternate\" " +  
                    "title=\"" + EDStatic.clickAccess + "\" \n" +
                "         href=\"" + dapUrl + ".html" + 
                    tQuery + "\">" + EDStatic.daf + "</a>\n";
        if (showSubsetLink && accessibleViaSubset().length() == 0) 
            subsetLink = 
                "     | <a rel=\"alternate\" rev=\"alternate\" " +
                    "title=\"" + EDStatic.dtSubset + "\" \n" +
                "         href=\"" + dapUrl + ".subset" + 
                    tQuery + 
                    (tQuery.length() == 0? "" : XML.encodeAsHTMLAttribute(EDDTable.DEFAULT_SUBSET_VIEWS)) + 
                    "\">" + EDStatic.subset + "</a>\n";
        if (showFilesLink && accessibleViaFilesDir().length() > 0) //> because it has sourceDir
            filesLink = 
                "     | <a rel=\"alternate\" rev=\"alternate\" " +
                    "title=\"" + 
                    XML.encodeAsHTMLAttribute(EDStatic.filesDescription +
                        (this instanceof EDDTableFromFileNames? "" : 
                            " " + EDStatic.warning + " " + EDStatic.filesWarning)) +
                    "\" \n" +
                "         href=\"" + tErddapUrl + "/files/" + datasetID + "/\">" + 
                EDStatic.EDDFiles + "</a>\n";
        if (showGraphLink && accessibleViaMAG().length() == 0) 
            graphLink = 
                "     | <a rel=\"alternate\" rev=\"alternate\" " +
                    "title=\"" + EDStatic.dtMAG + "\" \n" +
                "         href=\"" + dapUrl + ".graph" + 
                    tQuery + "\">" + EDStatic.EDDMakeAGraph + "</a>\n";
        String tSummary = extendedSummary(); 
        String tLicense = combinedGlobalAttributes().getString("license");
        boolean nonStandardLicense = tLicense != null && !tLicense.equals(EDStatic.standardLicense);
        tLicense = tLicense == null? "" :
            "    | " +
            (nonStandardLicense? "<font class=\"warningColor\">" : "") + 
            EDStatic.license + " " + 
            (nonStandardLicense? "</font>" : "") +
            //link below should have rel=\"copyright\"
            EDStatic.htmlTooltipImage(loggedInAs, XML.encodeAsPreHTML(tLicense, 100)) +  "\n";
        writer.write(
            //"<p><b>" + type + " Dataset:</b>\n" +
            "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
            "  <tr>\n" +
            "    <td nowrap valign=\"top\">" + EDStatic.EDDDatasetTitle + ":&nbsp;</td>\n" +
            "    <td><font class=\"standoutColor\"><big><b>" + XML.encodeAsHTML(title()) + "</b></big></font>\n" +
            "      " + emailHref(loggedInAs) + "\n" +
            "      " + rssHref(loggedInAs) + "\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap valign=\"top\">" + EDStatic.EDDInstitution + ":&nbsp;</td>\n" +
            "    <td>" + XML.encodeAsHTML(institution()) + "&nbsp;&nbsp;\n" +
            "    (" + EDStatic.EDDDatasetID + ": " + XML.encodeAsHTML(datasetID) + ")</td>\n" +
            "  </tr>\n" +
            otherRows + "\n" +
            "  <tr>\n" +
            "    <td nowrap valign=\"top\">" + EDStatic.EDDInformation + ":&nbsp;</td>\n" +
            "    <td>" + EDStatic.EDDSummary + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, XML.encodeAsPreHTML(tSummary, 100)) +  "\n" +
            tLicense +
            (accessibleViaFGDC.length() > 0? "" : 
                "     | <a rel=\"alternate\" rev=\"alternate\" \n" +
                "          title=\"" + EDStatic.EDDFgdcMetadata + "\" \n" +
                "          href=\"" + dapUrl + ".fgdc\">" + EDStatic.EDDFgdc + "</a>\n") +
            (accessibleViaISO19115().length() > 0? "" : 
                "     | <a rel=\"alternate\" rev=\"alternate\" \n" +
                "          title=\"" + EDStatic.EDDIso19115Metadata + "\" \n" +
                "          href=\"" + dapUrl + ".iso19115\">" + EDStatic.EDDIso19115 + "</a>\n") +
            "     | <a rel=\"alternate\" rev=\"alternate\" \n" +
            "          title=\"" + EDStatic.clickInfo + "\" \n" +
            "          href=\"" + tErddapUrl + "/info/" + datasetID + "/index.html\">" + 
                EDStatic.EDDMetadata + "</a>\n" +
            "     | <a rel=\"bookmark\" \n" +
            "          title=\"" + EDStatic.clickBackgroundInfo + "\" \n" +
            "          href=\"" + XML.encodeAsHTMLAttribute(infoUrl()) + "\">" + 
                EDStatic.EDDBackground + 
                (infoUrl().startsWith(EDStatic.baseUrl)? "" : EDStatic.externalLinkHtml(tErddapUrl)) + 
                "</a>\n" +
                subsetLink + "\n" +
                dapLink + "\n" +
                filesLink + "\n" +
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
     * NOW, makeReadyToUseAddGlobalAttributesForDatasetsXml IS RECOMMENDED OVER THIS.
     * This is used by subclass's generateDatasetsXml methods to make
     * sure that the global attributes includes at least place holders (dummy values)
     * for the required/common global attributes.
     *
     * @param sourceAtts   must not be null
     * @param tCdmDataType can be a specific type (e.g., "Grid") or null (for the default list)
     * @param tLocalSourceUrl a real URL (starts with "http"), a fileDirectory (with or without trailing '/'), 
     *     or a fake fileDirectory (not ideal).
     * @throws Exception if trouble
     */
    public static void addDummyRequiredGlobalAttributesForDatasetsXml(Attributes sourceAtts, 
        String tCdmDataType, String tLocalSourceUrl) throws Exception {

        //get the readyToUseAddGlobalAttributes for suggestions
        Attributes addAtts = makeReadyToUseAddGlobalAttributesForDatasetsXml(sourceAtts, 
            tCdmDataType, tLocalSourceUrl, null, new HashSet());
        String aConventions  = addAtts.getString("Conventions");
        String aInfo         = addAtts.getString("infoUrl"); 
        String aIns          = addAtts.getString("institution");

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
     * This is used by generateDatasetsXml to find out if a table 
     * probably has longitude, latitude, and time variables.
     *
     * @param sourceTable  This can't be null.
     * @param addTable with columns meanings that exactly parallel sourceTable
     *   (although, often different column names, e.g., lat, latitude).
     *   This can't be null.
     * @return true if it probably does
     */
    public static boolean probablyHasLonLatTime(Table sourceTable, Table addTable) {
        boolean hasLon = false, hasLat = false, hasTime = false;
        int sn = sourceTable.nColumns();
        int an =    addTable.nColumns();
        if (sn != an)
            throw new RuntimeException(
                "sourceTable nColumns=" + sn + " (" + sourceTable.getColumnNamesCSVString() + ")\n" +
                "!= addTable nColumns=" + an + " (" +    addTable.getColumnNamesCSVString() + ")");
        for (int col = 0; col < sn; col++) {
            String colName = addTable.getColumnName(col).toLowerCase();
            String units = addTable.columnAttributes(col).getString("units");
            if (units == null)
                units = sourceTable.columnAttributes(col).getString("units");
            if (colName.equals(EDV.LON_NAME) || 
                colName.equals("lon") ||
                EDV.LON_UNITS.equals(units)) 
                hasLon = true;
            else if (colName.equals(EDV.LAT_NAME) || 
                     colName.equals("lat") || 
                EDV.LAT_UNITS.equals(units)) 
                hasLat = true;
            else if (colName.equals(EDV.TIME_NAME) || Calendar2.isTimeUnits(units))
                hasTime = true;
            //String2.log(">> colName=" + colName + " units=" + units + " hasLon=" + hasLon + " hasLat=" + hasLat + " hasTime=" + hasTime);
        }
        return hasLon && hasLat && hasTime;
    }

    /**
     * This is used by generateDatasetsXml to change the 
     * names in the addTable to longitude, latitude, altitude/depth, and time,
     * if warranted.
     *
     * @param sourceTable  May be be null.
     * @param addTable with columns meanings that exactly parallel sourceTable
     *   (although, often different column names, e.g., lat, latitude).
     *   This can't be null.
     *   If a suitable column is found for LLAT, its column name is changed in addTable.
     * @return true if it found LLT (altitude/depth is ignored)
     */
    public static boolean tryToFindLLAT(Table sourceTable, Table addTable) {
        boolean hasLon = false, hasLat = false, hasAltDepth = false, hasTime = false;
        int an = addTable.nColumns();
        int sn = sourceTable == null? an : sourceTable.nColumns();
        if (sn != an)
            throw new RuntimeException(
                "sourceTable nColumns=" + sn + " (" + sourceTable.getColumnNamesCSVString() + ")\n" +
                "!= addTable nColumns=" + an + " (" +    addTable.getColumnNamesCSVString() + ")");

        //simple search for existing LLAT
        //Does it have the correct name and correct units (or units="")?
        for (int col = 0; col < sn; col++) {
            String colName = addTable.getColumnName(col);
            String colNameLC = colName.toLowerCase();
            String units = addTable.columnAttributes(col).getString("units");
            if (units == null && sourceTable != null)
                units = sourceTable.columnAttributes(col).getString("units");
            if (!hasLon && colNameLC.equals(EDV.LON_NAME)) {
                if (units == null || units.length() == 0 || 
                    String2.caseInsensitiveIndexOf(EDV.LON_UNITS_VARIANTS, units) >= 0) {
                    addTable.setColumnName(col, colNameLC);
                    if (!EDV.LON_UNITS.equals(units))
                        addTable.columnAttributes(col).set("units", EDV.LON_UNITS);
                    hasLon = true;
                } else if (colName.equals(colNameLC)) {
                    addTable.setColumnName(col, colName + "_");
                }
            } else if (!hasLat && colNameLC.equals(EDV.LAT_NAME)) {
                if (units == null || units.length() == 0 || 
                    String2.caseInsensitiveIndexOf(EDV.LAT_UNITS_VARIANTS, units) >= 0) {
                    addTable.setColumnName(col, colNameLC);
                    if (!EDV.LAT_UNITS.equals(units))
                        addTable.columnAttributes(col).set("units", EDV.LAT_UNITS);
                    hasLat = true;
                } else if (colName.equals(colNameLC)) {
                    addTable.setColumnName(col, colName + "_");
                }
            } else if (!hasAltDepth && 
                       (colNameLC.equals(EDV.ALT_NAME) || colNameLC.equals(EDV.DEPTH_NAME))) {
                if (units == null || units.length() == 0 || 
                    String2.indexOf(EDV.METERS_VARIANTS, units) >= 0) { //case sensitive
                    addTable.setColumnName(col, colNameLC);
                    if (!EDV.ALT_UNITS.equals(units))
                        addTable.columnAttributes(col).set("units", EDV.ALT_UNITS);
                    hasAltDepth = true;
                } else if (colName.equals(colNameLC)) {
                    addTable.setColumnName(col, colName + "_");
                }
            } else if (!hasTime && colNameLC.equals(EDV.TIME_NAME)) {
                if (Calendar2.isTimeUnits(units)) {
                    addTable.setColumnName(col, colNameLC);
                    hasTime = true;
                } else if (colName.equals(colNameLC)) {
                    addTable.setColumnName(col, colName + "_");
                }
            }
        }

        //search for compatible units for LLT
        for (int col = 0; col < sn; col++) {
            String colName = addTable.getColumnName(col);
            String colNameLC = colName.toLowerCase();
            String units = addTable.columnAttributes(col).getString("units");
            if (units == null && sourceTable != null)
                units = sourceTable.columnAttributes(col).getString("units");
            if (!hasLon && 
                String2.caseInsensitiveIndexOf(EDV.LON_UNITS_VARIANTS, units) >= 0) {
                addTable.setColumnName(col, EDV.LON_NAME);
                if (!EDV.LON_UNITS.equals(units))
                        addTable.columnAttributes(col).set("units", EDV.LON_UNITS);
                hasLon = true;
            } else if (!hasLat && 
                String2.caseInsensitiveIndexOf(EDV.LAT_UNITS_VARIANTS, units) >= 0) {
                addTable.setColumnName(col, EDV.LAT_NAME);
                if (!EDV.LAT_UNITS.equals(units))
                        addTable.columnAttributes(col).set("units", EDV.LAT_UNITS);
                hasLat = true;
            } else if (!hasTime && Calendar2.isTimeUnits(units)) {
                addTable.setColumnName(col, EDV.TIME_NAME);
                hasTime = true;
            }
            //String2.log(">> hasTime=" + hasTime + " col=" + addTable.getColumnName(col) + " units=" + units + " timeUnits=" + EDVTimeStamp.hasTimeUnits(units));             
        }
        return hasLon && hasLat && hasTime;
    }


    /**
     * This adds the whole phrase.toLowerCase and its parts to hashSet.
     *
     * @param phrase
     * @param hashset
     * @return the same hashSet for convenience
     */
    public static void addAllAndParts(String phrase, HashSet<String> hashSet) {
        if (phrase == null || phrase.length() == 0)
            return;
        phrase = phrase.toLowerCase();
        if (phrase.endsWith("*"))
            phrase = phrase.substring(0, phrase.length() - 1);
        if (phrase.endsWith("_"))
            phrase = phrase.substring(0, phrase.length() - 1);
        hashSet.add(phrase.trim());
        chopUpAndAdd(phrase, hashSet);
    }

    /**
     * This chops a csv list into words/phrases and adds them as is to hashSet.
     *
     * @param phrase
     * @param hashset
     * @return the same hashSet for convenience
     */
    public static HashSet<String> chopUpCsvAndAdd(String csv, HashSet<String> hashSet) {
        //String2.log("chopUpAndAdd " + phrase);
        if (csv == null || csv.length() == 0)
            return hashSet;
        String tWords[] = StringArray.arrayFromCSV(csv);
        int ntWords = tWords.length;
        for (int tw = 0; tw < ntWords; tw++) {
            String w = tWords[tw];
            if (w.endsWith("*"))
                w = w.substring(0, w.length() - 1);
            if (w.endsWith("_"))
                w = w.substring(0, w.length() - 1);
            hashSet.add(w.trim());
        }
        return hashSet;
    }


    /**
     * This chops phrase.toLowerCase into words and adds words[i] to hashSet.
     *
     * @param phrase
     * @param hashset
     * @return the same hashSet for convenience
     */
    public static HashSet<String> chopUpAndAdd(String phrase, HashSet<String> hashSet) {
        //String2.log("chopUpAndAdd " + phrase);
        //remove . at end of sentence or abbreviation, but not within number or word.word
        if (phrase == null || phrase.length() == 0)
            return hashSet;
        phrase = String2.replaceAll(phrase, ". ", " ");         //too aggressive?
        if (phrase.endsWith(".") && phrase.indexOf(' ') > 0)    //too aggressive?
            phrase = phrase.substring(0, phrase.length() - 1);
        String phraseLC = phrase.toLowerCase();
        if (phraseLC.indexOf("mixed_layer") >= 0)
            hashSet.add("mixed layer");

        char car[] = phraseLC.toCharArray();
        int nc = car.length;
        for (int c = 0; c < nc; c++) {
            //want to break things up, but not parts of equations.  
            //  keep /: mg/ml 
            //    but break up in Title and GCMD keywords before calling this method, 
            //    so Salinity/Density broken up
            //  keep ^: want to keep length^2
            //  keep .: end-of-sentence dealt with above, so now more likely part of a number or abbreviation
            //  keep -: in a number and word-word should be kept, - alone will be removed (<3 chars)
            if (car[c] == '%') {
                hashSet.add("percent");
                car[c] = ' ';
            } else if ("`~!@#$()_={}[];:'\"<>,?".indexOf(car[c]) >= 0) {
                car[c] = ' ';
            }
        }
        StringArray tWords = StringArray.wordsAndQuotedPhrases(new String(car));
        int ntWords = tWords.size();
        for (int tw = 0; tw < ntWords; tw++) {
            String s = tWords.get(tw);
            //String2.log(">> chop s=" + s);
            s = removeExtensionsFromTitle(s);
            if (s.length() > 2) {
                //String2.log("  add " + s);
                hashSet.add(s); 
            } else if (String2.indexOf(KEEP_SHORT_KEYWORDS, s) >= 0 ||
                       String2.indexOf(KEEP_SHORT_UC_KEYWORDS, s) >= 0) {
                hashSet.add(s); //uppercase will be upper-cased when written to generateDatasetsXml
            }                

            //if it contains internal '-' or '/', also: split it and add words
            if (s.indexOf('-') > 0 ||
                s.indexOf('/') > 0) {
                s = String2.replaceAll(s, '-', ' ');
                s = String2.replaceAll(s, '/', ' ');
                StringArray ttWords = StringArray.wordsAndQuotedPhrases(s);
                int nttWords = ttWords.size();
                for (int ttw = 0; ttw < nttWords; ttw++) {
                    String ts = ttWords.get(ttw);
                    if (ts.length() > 2) {
                        //String2.log("  add " + ts);
                        hashSet.add(ts); 
                    }
                }
            }
        }
        return hashSet;
    }

    /**
     * This is used by generateDatasetsXml in subclasses to suggest 
     * keywords (GCMD and others) based on variable's standard_names, long_name,
     * and ioos_category (for use by makeReadyToUseAddGlobalAttributesForDatasetsXml).
     *
     * @param dataSourceTable
     * @param dataAddTable
     * @return a HashSet of suggested keywords (may be String[0])
     */
    public static HashSet<String> suggestKeywords(Table dataSourceTable, 
        Table dataAddTable) {

        HashSet<String> keywordHashSet = new HashSet(128);

        //from the global metadata
        Attributes sourceGAtt = dataSourceTable.globalAttributes();
        Attributes addGAtt    = dataAddTable.globalAttributes();
        chopUpAndAdd(sourceGAtt.getString("institution"), keywordHashSet);
        chopUpAndAdd(   addGAtt.getString("institution"), keywordHashSet);
        chopUpAndAdd(sourceGAtt.getString("title"),       keywordHashSet);
        chopUpAndAdd(   addGAtt.getString("title"),       keywordHashSet);

        //from the data variables
        for (int addCol = 0; addCol < dataAddTable.nColumns(); addCol++) {
            //add the variable destinationName 
            String destName = dataAddTable.getColumnName(addCol);
            //String2.log(">> suggestKeywords destName=" + destName);
            String tdn = destName;
            int po = tdn.indexOf(" ("); //e.g., temp (degC)
            if (po > 0)
                tdn = tdn.substring(0, po);
            if (tdn.endsWith("*"))
                tdn = tdn.substring(0, tdn.length() - 1);
            if (tdn.endsWith("_"))
                tdn = tdn.substring(0, tdn.length() - 1);
            keywordHashSet.add(tdn.trim()); //even if short.  don't chop up.
            int sourceCol = dataSourceTable.findColumnNumber(destName);
            Attributes addAtts = dataAddTable.columnAttributes(addCol);
            Attributes sourceAtts = sourceCol >= 0? dataSourceTable.columnAttributes(sourceCol) :
                null;

            //try to find standard_name 
            String stdName = addAtts.getString("standard_name");
            if (stdName == null && sourceAtts != null)
                stdName = sourceAtts.getString("standard_name");
            if (stdName != null) {
                //get matching gcmd keywords
                String tKeywords[] = CfToFromGcmd.cfToGcmd(stdName);
                for (int i = 0; i < tKeywords.length; i++) {
                    //add the whole gcmd keyword phrase
                    String tk = tKeywords[i];
                    keywordHashSet.add(tk); 

                    //add individual words from gcmd keywords
                    chopUpAndAdd(String2.replaceAll(tk, '/', ' '), keywordHashSet);
                }

                //add whole and parts of standard_name
                if (stdName.indexOf("longitude") < 0 &&
                    stdName.indexOf("latitude") < 0 &&
                    //depth is interesting
                    //altitude is interesting
                    !stdName.equals("time")) {    //time     is in interesting longer standard_names
                    keywordHashSet.add(stdName.toLowerCase());  
                    chopUpAndAdd(stdName, keywordHashSet);  
                }
            }

            //try to find long_name 
            String longName = addAtts.getString("long_name");
            if (longName == null && sourceAtts != null)
                longName = sourceAtts.getString("long_name");
            if (longName != null) 
                chopUpAndAdd(longName, keywordHashSet);

            //try to find ioos_category 
            String ioos = addAtts.getString("ioos_category");
            if (ioos == null && sourceAtts != null)
                ioos = sourceAtts.getString("ioos_category");
            if (ioos != null) {
                //add whole and in parts
                keywordHashSet.add(ioos.toLowerCase());
                chopUpAndAdd(ioos, keywordHashSet);
            }

        }

        //common uninteresting keywords (due, for) will be removed by
        //  makeReadyToUseAddGlobalAttributesForDatasetsXml

        return keywordHashSet;
    }

    /**
     * This cleans the keywords hashset (e.g., removes common words like 'from').
     *
     * @param suggestedKeywords
     * @throws Exception if touble
     */
    public static void cleanSuggestedKeywords(HashSet<String> keywords) throws Exception {

        //look in keywords for acronyms -- expand them
        String keys[] = keywords.toArray(new String[0]);
        int n = keys.length;
        HashMap<String,String> achm = EDStatic.gdxAcronymsHashMap();
        for (int i = 0; i < n; i++) {
            //String2.log(">> 1keyword#" + i + "=" + keys[i]);
            chopUpAndAdd(achm.get(keys[i].toUpperCase()), keywords); 
        }

        if (keywords.contains("1st")) 
            keywords.add(     "first");  
        if (keywords.contains("2nd")) 
            keywords.add(     "second");  
        if (keywords.contains("3rd")) 
            keywords.add(     "third");  
        if (keywords.contains("4th")) 
            keywords.add(     "fourth");  
        if (keywords.contains("5th")) 
            keywords.add(     "fifth");  
        if (keywords.contains("6th")) 
            keywords.add(     "sixth");  
        if (keywords.contains("adcp"))  
            chopUpAndAdd(     "current currents velocity", keywords);
        if (keywords.contains("calcofi") || 
            keywords.contains("CalCOFI"))  
            chopUpAndAdd(     "California Cooperative Fisheries Investigations", keywords);
        if (keywords.contains("cloud"))  
            chopUpAndAdd(     "cloudiness", keywords);
        if (keywords.contains("cloudiness"))  
            addAllAndParts(   "cloud cover", keywords);
        if (keywords.contains("coads") || keywords.contains("icoads"))  
            chopUpAndAdd(     "coads icoads international comprehensive Ocean Atmosphere Data Set", keywords);
        if (keywords.contains("crm"))  
            addAllAndParts(   "coastal relief model", keywords);
        if (keywords.contains("ctd")) 
            chopUpAndAdd(     "conductivity temperature depth sonde", keywords);  
        if (keywords.contains("1day")) 
            keywords.add(     "daily");  
        if (keywords.contains("daily")) 
            keywords.add(     "day");  
        if (keywords.contains("daytime")) 
            chopUpAndAdd(     "day time", keywords);  
        if (keywords.contains("dem")) 
            chopUpAndAdd(     "digital elevation model", keywords);  
        if (keywords.contains("dewpoint") || 
            keywords.contains("dewpt") ||
            (keywords.contains("dew") && keywords.contains("point"))) 
            addAllAndParts(   "dew point", keywords);
        if (keywords.contains("etopo"))  
            chopUpAndAdd(     "global bathymetry topography", keywords);
        if (keywords.contains("flh")) 
            chopUpAndAdd(     "fluorescence line height", keywords);  
        if (keywords.contains("fnmoc")) 
            chopUpAndAdd(     "fleet numerical meteorology and oceanography center", keywords);  
        if (keywords.contains("ghcn"))  
            chopUpAndAdd(     "global historical climatology network", keywords);
        if (keywords.contains("ghrsst")) 
            chopUpAndAdd(     "global high resolution sea surface temperature sst", keywords);  
        if (keywords.contains("globec")) 
            chopUpAndAdd(     "Global Ocean Ecosystems Dynamics", keywords);  
        if (keywords.contains("goes")) 
            chopUpAndAdd(     "geostationary operational environmental satellite", keywords);  
        if (keywords.contains("gpcc")) 
            chopUpAndAdd(     "global precipitation climatology centre rain rainfall", keywords);  
        if (keywords.contains("gpcp")) 
            chopUpAndAdd(     "global precipitation climatology project rain rainfall", keywords);  
        if (keywords.contains("hfradar")) 
            chopUpAndAdd(     "hf high frequency radar", keywords);  
        if (keywords.contains("hf") && keywords.contains("radar")) 
            chopUpAndAdd(     "high frequency hfradar", keywords);  
        if (keywords.contains("hourly")) 
            keywords.add(     "hour");  
        if (keywords.contains("hycom")) 
            chopUpAndAdd(     "hybrid coordinate ocean model", keywords);  
        //"icoads" see coads above
        if (keywords.contains("ltm")) 
            chopUpAndAdd(     "long term mean", keywords);  
        if (keywords.contains("mday")) 
            keywords.add(     "monthly");  
        if (keywords.contains("monthly")) 
            keywords.add(     "month");  
        if (keywords.contains("mur")) 
            chopUpAndAdd(     "multi-scale ultra-high resolution", keywords);  
        if (keywords.contains("ncom")) 
            chopUpAndAdd(     "navy coastal ocean model", keywords);
        if (keywords.contains("near-real")) 
            chopUpAndAdd(     "near real time", keywords);
        if (keywords.contains("nep")) 
            chopUpAndAdd(     "north east pacific", keywords);  
        if (keywords.contains("nighttime")) 
            chopUpAndAdd(     "night time", keywords);  
        if (keywords.contains("nrt")) 
            chopUpAndAdd(     "near real time", keywords);  
        if (keywords.contains("npp")) 
            chopUpAndAdd(     "national polar orbiting partnership", keywords);  
        if (keywords.contains("nseabaltic")) {
            addAllAndParts(   "north sea", keywords);
            addAllAndParts(   "baltic sea", keywords);
        }                        
        /* for adding new terms
        if (keywords.contains("ctd")) 
            chopUpAndAdd(     "conductivity temperature depth", keywords);  
        */
        if (keywords.contains("obpg")) 
            chopUpAndAdd(     "Ocean Biology Processing Group NASA color", keywords);  
        if (keywords.contains("oceans")) 
            keywords.add(     "ocean");  
        if (keywords.contains("olr")) 
            chopUpAndAdd(     "outgoing longwave radiation", keywords);  
        if (keywords.contains("poes")) 
            chopUpAndAdd(     "polar orbiting environmental satellite", keywords);
        if (keywords.contains("precipitation")) 
            chopUpAndAdd(     "rain rainfall", keywords);  
        if (keywords.contains("rain")) 
            chopUpAndAdd(     "precipitation rainfall", keywords);  
        if (keywords.contains("rainfall")) 
            chopUpAndAdd(     "precipitation rain", keywords);  
        if (keywords.contains("real-time")) {
            addAllAndParts(   "real time", keywords);
            keywords.add(     "realtime");
        }
        if (keywords.contains("roms")) {
            chopUpAndAdd(     "regional ocean modeling system", keywords);
            keywords.add(     "model");
        }
        if (keywords.contains("rtofs")) 
            chopUpAndAdd(     "real-time ocean forecast system", keywords);  
        if (keywords.contains("smi")) 
            chopUpAndAdd(     "standard mapped image", keywords);  
        if (keywords.contains("trmm")) 
            chopUpAndAdd(     "tropical rainfall measuring mission", keywords);  
        if (keywords.contains("viirs")) 
            chopUpAndAdd(     "visible infrared imaging radiometer suite", keywords);  


        //add expanded common abbreviations and acronyms    
        //use contains() so original isn't removed
        if (keywords.contains("co2"))  
            addAllAndParts(   "carbon dioxide", keywords);
        if (keywords.contains("carbon dioxide"))  
            keywords.add(     "co2");
        if (keywords.contains("co3"))  
            keywords.add(     "carbonate");
        if (keywords.contains("carbonate"))  
            keywords.add(     "co3");
        if (keywords.contains("nh4"))
            keywords.add(     "ammonium");
        if (keywords.contains("ammonium"))
            keywords.add(     "nh4");
        if (keywords.contains("no2"))
            keywords.add(     "nitrite");
        if (keywords.contains("nitrate"))
            keywords.add(     "n02");
        if (keywords.contains("no3"))
            keywords.add(     "nitrate");
        if (keywords.contains("nitrate"))
            keywords.add(     "no3");
        if (keywords.contains("o2"))
            keywords.add(     "oxygen");
        if (keywords.contains("oxygen"))
            keywords.add(     "o2");
        if (keywords.contains("po4"))
            keywords.add(     "phosphate");
        if (keywords.contains("phosphate"))
            keywords.add(     "po4");

        if (keywords.contains("chl") || 
            keywords.contains("chla") ||
            keywords.contains("chlor") ||
            keywords.contains("chlora")) 
            keywords.add(     "chlorophyll");

        if (keywords.contains("eur"))  
            keywords.add(     "europe");
        if ((keywords.contains("hf") && keywords.contains("radar")) ||
            keywords.contains("hfradar")) { 
            keywords.add(     "hfradar");
            addAllAndParts(   "hf radar", keywords);
        }
        if (keywords.contains("hf") && keywords.contains("radio")) {
            keywords.add(     "hfradio");
            addAllAndParts(   "hf radio", keywords);
        }
        if (keywords.contains("hires")) 
            addAllAndParts(   "high resolution", keywords);
        if (keywords.contains("glob"))  
            keywords.add(     "global");  //usually
        if (keywords.contains("mod")) 
            keywords.add(     "modulus"); //usually

        if (keywords.contains("aoml") ||
            keywords.contains("coastwatch") ||
            keywords.contains("esrl") ||
            keywords.contains("gfdl") ||
            keywords.contains("glerl") ||
            keywords.contains("ncdc") ||
            keywords.contains("ncei") ||
            keywords.contains("ndbc") ||
            keywords.contains("nesdis") ||
            keywords.contains("ngdc") ||
            keywords.contains("nmfs") ||
            keywords.contains("nodc") ||
            keywords.contains("nws") ||
            keywords.contains("osdpd") ||
            keywords.contains("ospo") ||
            keywords.contains("pfeg") ||
            keywords.contains("pfel") ||
            keywords.contains("pmel")) 
            keywords.add(     "noaa");

        if (keywords.contains("obs")) 
            keywords.add(     "observations");
        if (keywords.contains("seawater")) {
            keywords.add(     "sea");
            keywords.add(     "water");
        }
        if (keywords.contains("sea") && 
            keywords.contains("water"))
            keywords.add(     "seawater");
        if (keywords.contains("stdev"))  
            addAllAndParts(   "standard deviation", keywords);  


        //name changes (both ways)
        if (keywords.contains("osdpd"))
            keywords.add(     "ospo");
        if (keywords.contains("ospo"))
            keywords.add(     "osdpd");
        if (keywords.contains("ncddc") ||
            keywords.contains("ncdc") ||
            keywords.contains("ngdc") ||
            keywords.contains("nodc"))
            keywords.add(     "ncei");

        //expand common abbreviations   usually use remove() so original is removed
        if (keywords.remove("anal")) 
            keywords.add(   "analysis");
        if (keywords.remove("ann")) 
            keywords.add(   "annual");
        if (keywords.remove("anom")) 
            keywords.add(   "anomaly");
        if (keywords.remove("atmos")) 
            keywords.add(   "atmosphere");
        if (keywords.remove("avg")) 
            keywords.add(   "average");
        if (keywords.remove("coef.") || keywords.remove("coef")) 
            keywords.add(   "coefficient");
        if (keywords.remove("climatologymeteorologyatmosphere")) {
            keywords.add(   "atmosphere");
            keywords.add(   "climatology");
            keywords.add(   "meteorology");
        }            
        if (keywords.remove("err")) 
            keywords.add(   "error");
        if (keywords.remove("geoscientificinformation")) {
            keywords.add(   "geoscientific");
            keywords.add(   "information");
        }                        
        if (keywords.remove("merid.") || keywords.remove("merid")) 
            keywords.add(   "meridional");
        if (keywords.remove("mon")) 
            keywords.add(   "monthly");
        if (keywords.remove("phos")) 
            keywords.add(   "phosphate");
        if (keywords.remove("precip")) 
            keywords.add(   "precipitation");
        if (keywords.remove("sili")) 
            keywords.add(   "silicate");
        if (keywords.remove("temp")) 
            keywords.add(   "temperature");
        if (keywords.remove("u-veloc.")) 
            keywords.add(   "u-velocity");
        if (keywords.remove("univ"))  
            keywords.add(   "university");
        if (keywords.remove("u.s"))  
            keywords.add(   "us"); //will be upper-cased when written (it's in KEEP_SHORT_UC)
        if (keywords.remove("v-veloc.") || keywords.remove("v-veloc")) 
            keywords.add(   "v-velocity");
        if (keywords.remove("veloc.")   || keywords.remove("veloc")) 
            keywords.add(   "velocity");
        if (keywords.remove("w-veloc.") || keywords.remove("w-veloc")) 
            keywords.add(   "w-velocity");
        
        //remove common uninteresting keywords >=3 chars
        keywords.remove("alt");
        keywords.remove("and");
        keywords.remove("apr");
        keywords.remove("april");
        keywords.remove("aug");
        keywords.remove("august");
        keywords.remove("dec");
        keywords.remove("december");
        keywords.remove("deg"); 
        keywords.remove("dodsc");
        keywords.remove("feb");
        keywords.remove("february");
        keywords.remove("for");
        keywords.remove("from");
        keywords.remove("gmt");
        keywords.remove("http");
        keywords.remove("jan");
        keywords.remove("january");
        keywords.remove("jul");
        keywords.remove("july");
        keywords.remove("jun");
        keywords.remove("june");
        keywords.remove("last");
        keywords.remove("location");
        keywords.remove("lon");
        keywords.remove("lat");
        keywords.remove("mar");
        keywords.remove("march");
        keywords.remove("may");
        keywords.remove("m/s");
        keywords.remove("netcdf");
        keywords.remove("nov");
        keywords.remove("november");
        keywords.remove("null");
        keywords.remove("oct");
        keywords.remove("october");
        keywords.remove("other");
        keywords.remove("prof.");  //professor, profile?
        keywords.remove("sep");
        keywords.remove("september");
        keywords.remove("the");
        keywords.remove("unknown");
        keywords.remove("uri");
        keywords.remove("url");
        keywords.remove("utc");
        keywords.remove("vars");
        keywords.remove("variables");
        keywords.remove("ver");  //usually version, but could be vertical or ...

        //always!
        keywords.add("data");

        //remove if different case
        keys = keywords.toArray(new String[0]);
        n = keys.length;
        for (int i = 0; i < n; i++) {
            //String2.log(">> 2keyword#" + i + "=" + keys[i]);
            //remove trailing * or _ 
            String k = keys[i];
            while (k.length() > 0 && "*_".indexOf(k.charAt(k.length() - 1)) >= 0) {
                keywords.remove(k);
                k = k.substring(0, k.length() - 1).trim();
                if (k.length() == 0)
                    continue;
                keywords.add(k);
            }
            if (!k.equals(k.trim())) {
                keywords.remove(k);
                k = k.trim();
                if (k.length() == 0)
                    continue;
                keywords.add(k);
            }

            String lc = k.toLowerCase();
            if (!lc.equals(k) && keywords.contains(lc))
                keywords.remove(k);
        }
    }

    static void addIfNoAddOrSourceAtt(Attributes addAtts, Attributes sourceAtts, 
        String name, String value) {
        if (!isSomething(   addAtts.getString(name)) &&
            !isSomething(sourceAtts.getString(name)))
            addAtts.add(name, value);
    }

    static void addIfNoAddOrSourceAtt(Attributes addAtts, Attributes sourceAtts, 
        String name, PrimitiveArray value) {
        if (!isSomething(   addAtts.getString(name)) &&
            !isSomething(sourceAtts.getString(name)))
            addAtts.add(name, value);
    }

    /**
     * This is used by generateDatasetsXml to expand acronyms in the proposed summary.
     *
     * @param acronym  e.g., "ICOADS"
     * @param expanded e.g., "International Comprehensive Ocean Atmosphere Data Set"
     * @return the new tSummary (or the same one if unchanged)
     */
    static String expandInSummary(String tSummary, HashSet<String> suggestedKeywords,
        String acronym, String expanded) {

        String full = expanded + " (" + acronym + ")";
        if (String2.looselyContains(tSummary, expanded)) {
            chopUpAndAdd(full, suggestedKeywords);
            return tSummary;
        }

        int po = String2.findWholeWord(tSummary, acronym);
        if (po >= 0) {  
            //that's good enough to add
            chopUpAndAdd(full, suggestedKeywords);

            //ensure it isn't in a URL    e.g., /TRMM/ or /TRMM_something/ or _TRMM_something
            //acronym likely applies, but expansion would be in bad place
            if (po > 0 && po + acronym.length() < tSummary.length() &&
                "/_.".indexOf(tSummary.charAt(po - 1)) >= 0 &&
                "/_" .indexOf(tSummary.charAt(po + acronym.length())) >= 0)
                return tSummary; //don't make the change
            tSummary = tSummary.substring(0, po) + full + 
                tSummary.substring(po + acronym.length());
            if (debugMode) String2.log(">> expandInSummary " + acronym + " -> " + full);
            return tSummary;
        }

        return tSummary;
    }


    /**
     * This is used by subclass's generateDatasetsXml methods to make
     * sure that the global attributes are present (as good as possible; no dummy values)
     * for the required/common global attributes.
     *
     * @param sourceAtts  usually not null
     * @param tCdmDataType a specific type (e.g., "Grid"); if null, the attribute won't be added
     * @param tLocalSourceUrl a real local URL (starts with "http"), a fileDirectory (with or without trailing '/'), 
     *     or a fake fileDirectory (not ideal).
     *     If an OPeNDAP url, it is without the .das, .dds, or .html extension.
     * @param externalAtts globalAtts from another source, or null.
     *    They have priority over sourceAtts.
     *    But title is special: sourceAtts title &gt; 20 characters has priority.
     *    <br>&lt;suffixForTitle&gt; is special attribute with text to be appended to title in parentheses.
     *    It is removed from addAttributes at end of method.
     * @param suggestedKeywords suggested keywords (usually from suggestKeywords)
     * @param throws Exception if trouble
     */
    public static Attributes makeReadyToUseAddGlobalAttributesForDatasetsXml(Attributes sourceAtts, 
        String tCdmDataType, String tLocalSourceUrl, Attributes externalAtts,
        HashSet<String> suggestedKeywords) throws Exception {

//TO DO: look at other metadata standards (e.g., FGDC) to find similar attributes to look for
//fgdc: http://docs.google.com/viewer?a=v&q=cache:jqwVIfleOYoJ:portal.opengeospatial.org/files/%3Fartifact_id%3D16936+%22fgdc:title%22&hl=en&gl=us&pid=bl&srcid=ADGEESjCZAzZzsRrGP0bxE3vj2qf3e7UAtL0O9C7M6Vm9JSvkuaW74nBYChLJdQagIf0X0vm-0_qgAHUanv6WqhNu59ouFV4i3-wD-nzfUBmRg4npV2wrCrc2RIJ8Q7El65RjHCZiqzU&sig=AHIEtbRqR8ld45spO4SqD7nIYV2de1FGow
  
        if (sourceAtts == null)
            sourceAtts = new Attributes();
        if (externalAtts == null)
            externalAtts = new Attributes();
        externalAtts.remove("Oceanwatch_Live_Access_Server");
        if (reallyVerbose) 
            String2.log("makeReadyToUseAddGlobalAttributesForDatasetsXml\n" +
                (debugMode? "  sourceAtts=\n" + sourceAtts.toString() : "") +
                "  sourceAtts.title=" + sourceAtts.getString("title") + "\n" +
                "  externalAtts=" +
                (externalAtts == null? "null" : "\n" + externalAtts.toString()));
        if (suggestedKeywords == null)
            suggestedKeywords = new HashSet(128);
        //String2.log("initial suggestedKeywords: " + String2.toCSSVString(suggestedKeywords));

        String name, value;
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
        if (tPublicSourceUrl == null)
            tPublicSourceUrl = "";
        boolean sourceUrlIsHyraxFile = 
            tPublicSourceUrl.startsWith("http") &&
            tPublicSourceUrl.indexOf("/opendap/") > 0 &&
            (tPublicSourceUrl.endsWith("/") || tPublicSourceUrl.endsWith("/contents.html"));
        boolean sourceUrlIsHyraxCatalog = 
            tPublicSourceUrl.startsWith("http") &&
            tPublicSourceUrl.indexOf("/opendap/") > 0 &&
            !sourceUrlIsHyraxFile;
        boolean sourceUrlIsThreddsCatalog = 
            tPublicSourceUrl.startsWith("http") &&
            tPublicSourceUrl.indexOf("/thredds/catalog/") > 0;

        String sourceUrlAsTitle = String2.replaceAll(
            //"extension" may be part of name with internal periods, 
            //  so get it but remove known file type extensions
            removeExtensionsFromTitle(File2.getNameAndExtension(tPublicSourceUrl)), 
            '_', ' ');
        sourceUrlAsTitle = String2.replaceAll(sourceUrlAsTitle, '\n', ' ');
        sourceUrlAsTitle = String2.replaceAllIgnoreCase(sourceUrlAsTitle, "Data iridl.ldeo.columbia.edu SOURCES ", "");
        sourceUrlAsTitle = String2.replaceAllIgnoreCase(sourceUrlAsTitle, "data opendap.jpl.nasa.gov opendap ", "");        
        sourceUrlAsTitle = String2.replaceAllIgnoreCase(sourceUrlAsTitle, "aggregate", "");
        sourceUrlAsTitle = String2.replaceAllIgnoreCase(sourceUrlAsTitle, "aggregation", "");
        sourceUrlAsTitle = String2.replaceAllIgnoreCase(sourceUrlAsTitle, "ghrsst", "GHRSST");
        sourceUrlAsTitle = String2.replaceAllIgnoreCase(sourceUrlAsTitle, "ncml", "");
        sourceUrlAsTitle = String2.replaceAll(sourceUrlAsTitle, "avhrr AVHRR", "AVHRR");
        sourceUrlAsTitle = String2.combineSpaces(sourceUrlAsTitle);
        int dpo = sourceUrlAsTitle.lastIndexOf(" dodsC ");
        if (dpo >= 0) 
            sourceUrlAsTitle = sourceUrlAsTitle.substring(dpo + 7);
        if ("catalog".equals(sourceUrlAsTitle))
            sourceUrlAsTitle = "";
        if (debugMode) String2.log(">> sourceUrlAsTitle=" + sourceUrlAsTitle);

        //Use externalAtts as initial addAtts. They have priority over sourceAtts.
        Attributes addAtts = externalAtts == null? new Attributes() : (Attributes)externalAtts.clone();

        //convert all fgdc_X, fgdc:X, and HDF5_GLOBAL.X metadata to X (if not already set)
        //  e.g., http://measures.gsfc.nasa.gov/thredds/dodsC/SWDB_aggregation/SWDB_L305.004/SWDB_Aggregation_L305_1997.ncml.ncml
        //and fix any bad characters in sourceAtt names.
        //  e.g. http://www.ngdc.noaa.gov/thredds/dodsC/ustec/tec/200609030400_tec.nc.das uses '_'
        //http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#idp4775248
        //  says "Variable, dimension and attribute names should begin with a letter
        //  and be composed of letters, digits, and underscores."
        //Technically, starting with _ is not allowed, but it is widely done: 
        //  e.g., _CoordinateAxes, _CoordSysBuilder
        String sourceNames[] = sourceAtts.getNames();
        String removePrefixes[] = {"fgdc_", "fgdc:", "HDF5_GLOBAL_", "HDF5_GLOBAL."};
        //fgdc_metadata_url is fgdc metadata, so not so useful as infoUrl
        HashSet toRemove = new HashSet(Arrays.asList( 
            //Enter them lowercase here. The search for them is case-insensitive.
            "cols", "columns", "cwhdf_version",
            "data_bins", "data_center", "data_maximum", "data_minimum",
            "easternmost_longitude",
            "end_day", "end_millisec", "end_time", "end_year",
            "end_orbit", "endorbitnumber", "end_orbit_number",
            "et_affine", 
            "first_index", "format", //e.g., hdf5
            "fgdc_metadata_url", "fgdc:metadata_url", 
            "gctp_datum", "gctp_parm", "gctp_sys", "gctp_zone",
            "gds_version_id", "georange", "granulepointer",
            "ice_fraction", "inputpointer",
            "intercept",
            "land_fraction",
            "local_granule_id",
            "lat%2eaxis", "lat%2ecomment", "lat%2elong_name", 
            "lat%2estandard_name", "lat%2eunits", 
            "lat%2e_fillvalue", "lat%2evalid_max", "lat%2evalid_min",
            "lat_axis", "lat_long_name", "lat_standard_name", "lat_units", 
            "latitude_resolution", 
            "latitude_step", "latitude_units",
            "latitudes",
            "lon%2eaxis", "lon%2ecomment", "lon%2elong_name", 
            "lon%2estandard_name", "lon%2eunits",
            "lon%2e_fillvalue", "lon%2evalid_max", "lon%2evalid_min",
            "lon_axis", "lon_long_name", "lon_standard_name", "lon_units", 
            "longitude_resolution",
            "longitude_step", "longitude_units",
            "longitudes",
            "map_time_range", "minimum_bin_pts",
            "northernmost_latitude",
            "number_of_columns", "number_of_lines",
            "num_l3_columns", "num_l3_rows",
            "observation_date", "operationmode", "orbitparameterspointer",
            "orbit",
            "parameter", "pass_date", "percent_rev_data_usage",
            "period", 
            "period_end_day", "period_end_year", "period_start_day", "period_start_year", 
            "polygon_latitude", "polygon_longitude",
            "qagranulepointer", "qapercentmissingdata", "qapercentoutofboundsdata",
            "range_beginning_date", "rangebeginningdate", 
            "range_beginning_time", "rangebeginningtime", 
            "range_ending_date", "rangeendingdate", 
            "range_ending_time", "rangeendingtime", 
            "rows",
            "scaling", "scaling_equation", "search_radius_km", "second_index", 
            "slope",
            "southernmost_latitude",
            "spatial_completeness_comment", 
            "spatial_completeness_definition", 
            "spatial_completeness_ratio", 
            "start_date", "start_day", "start_millisec", 
            "start_orbit", "startorbitnumber", "start_orbit_number",
            "start_time", "start_year",
            "station_latitude", "station_longitude",
            "stop_date", "stop_time",
            "stop_orbit", "stoporbitnumber", "stop_orbit_number",
            "suggested_image_scaling_applied",
            "suggested_image_scaling_maximum",
            "suggested_image_scaling_minimum",
            "suggested_image_scaling_type",
            "sw_point_latitude", "sw_point_longitude", 
            "time%2eaxis", "time%2ecomment", "time%2elong_name", 
            "time%2estandard_name", "time%2eunits",
            "time%2e_fillvalue", "time%2ecalendar",
            "time_axis", "time_end", "time_epoch", "time_long_name", 
            "time_mean_removed", "time_standard_name", "time_start", 
            "time_units",            
            "units",
            "variable_1", "variable_2", "variable_3", "variable_4", "variable_5", 
            "westernmost_longitude", 
            "wind_vector_cell_resolution", "wind_vector_source",
            "year"));
        for (int i = 0; i < sourceNames.length; i++) {
            String sn = sourceNames[i];
            String pre = String2.findPrefix(removePrefixes, sn, 0);
            if (toRemove.contains(sn.toLowerCase())) {
                addAtts.set(sn, "null");    //remove toRemove att name
                if (debugMode) String2.log(">>  useless sourceAttName=\"" + sn + "\" removed.");
            } else if (sn.startsWith("dsp_") ||  //dsp info is mostly specific to one time point
                       sn.startsWith("EquatorCrossing") || 
                       sn.startsWith("l3_actual_grid_") || 
                       sn.endsWith("_dim_0.name") || 
                       sn.endsWith("_dim_1.name") || 
                       sn.endsWith("_dim_2.name") || 
                       sn.endsWith("_dim_3.name")) {
                addAtts.set(sn, "null");   //remove it
                if (debugMode)
                    String2.log(">>  useless sourceAttName=\"" + sn + "\" removed.");
            } else if (pre == null) {
                String safeSN = String2.modifyToBeVariableNameSafe(sn);
                if (!sn.equals(safeSN)) {       //if sn isn't safe
                    addAtts.set(sn, "null");    //  neutralize bad att name
                    if (reallyVerbose)
                        String2.log("  bad     sourceAttName=\"" + sn + "\" converted to \"" + safeSN + "\".");
                    addAtts.setIfNotAlreadySet(safeSN, sourceAtts.get(sn)); 
                }
            } else {
                addAtts.set(sn, "null");   //remove full original prefixed att name
                String safeSN = sn.substring(pre.length()); //e.g., fgdc_X becomes X
                safeSN = String2.modifyToBeVariableNameSafe(safeSN);
                if (toRemove.contains(safeSN)) {
                    if (debugMode)
                        String2.log(">>  useless sourceAttName=\"" + sn + "\" removed.");
                } else {
                    if (reallyVerbose)
                        String2.log("  bad     sourceAttName=\"" + sn + "\" converted to \"" + safeSN + "\".");
                    addAtts.setIfNotAlreadySet(safeSN, sourceAtts.get(sn)); 
                }
            }
        }

        //convert atts from a podaac dataset from Earth & Space Research
        //removed above: GEORANGE, PERIOD, YEAR
        String gfrom[] = {
            "COMPANY",       "CONTACT",     "Convention",    "convention", 
            "CREATION_DATE", "DATASUBTYPE",
            "DATATYPE",      "DESCRIPTION", "REFERENCE",     "SOURCE",
            "VARIABLE",      "VERSION"};
        String gto[]   = { //if not already specified
            "institution",   "contact",     "Conventions",   "Conventions",
            "creation_date", "datasubtype",
            "datatype",      "title",       "reference",     "source",
            "variable",      "version"};
        for (int i = 0; i < gfrom.length; i++) {
            value = sourceAtts.getString(gfrom[i]);
            if (value != null)
                addAtts.set(gfrom[i], "null");
            if (isSomething(value)) {
                if (gfrom[i].equals("COMPANY") && value.endsWith(", Seattle, WA"))
                    value = value.substring(0, value.length() - 13);
                addIfNoAddOrSourceAtt(addAtts, sourceAtts, gto[i], value);
            }
        }


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
        if (!isSomething(value)) value =    addAtts.getString("conventions"); //wrong case?
        if (!isSomething(value)) value = sourceAtts.getString("conventions"); //wrong case?
        if (!isSomething(value)) value =    addAtts.getString("Metadata_Conventions"); 
        if (!isSomething(value)) value = sourceAtts.getString("Metadata_Conventions"); 
        if (reallyVerbose) String2.log("  old Conventions=" + value);
        value = suggestConventions(value);
        addAtts.set(name, value);  //always reset Conventions
        if (reallyVerbose) String2.log("  new " + name + "=" + value);

        //nullify wrong case?
        name = "conventions";
        value = addAtts.getString(name);
        if (!isSomething(value)) sourceAtts.getString(name); 
        if (isSomething(value))
            addAtts.set(name, "null"); //clear it

        //nullify old Metadata_Conventions?
        name = "Metadata_Conventions";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name); 
        if (isSomething(value)) 
            addAtts.set(name, "null"); //clear it

        //creator_email   (not required, but useful ACDD and for ISO 19115 and FGDC)
        name = "creator_email";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) value = addAtts.getString("email");
        if (!isSomething(value)) value = sourceAtts.getString("email");
        if (!isSomething(value)) value = addAtts.getString("contact_person_email");
        if (!isSomething(value)) value = sourceAtts.getString("contact_person_email");
        String creator_email = isSomething(value)? value : "";

        //creator_name    (not required, but useful ACDD and for ISO 19115 and FGDC)
        name = "creator_name";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        String creator_name = isSomething(value)? value : "";
        
        //creator_url     (not required, but useful ACDD and for ISO 19115 and FGDC)
        name = "creator_url";
        value                          =    addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) value =    addAtts.getString("related_url");
        if (!isSomething(value)) value = sourceAtts.getString("related_url");
        String creator_url = isSomething(value)? value : "";

        //note suffixForTitle //e.g. Thredds currentLevel name: "5-day" or "sal001", sometimes long
        String suffixForTitle = removeExtensionsFromTitle(addAtts.getString("suffixForTitle"));  
        if (!isSomething(suffixForTitle))
            suffixForTitle = "";
        String suffixForTitleP = isSomething(suffixForTitle)? " (" + suffixForTitle + ")" : "";

        //do early   so available for tEmailSource
        name = "summary";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        String oSummary = value;
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
            if (!isSomething(value)) value =    addAtts.getString("long_name");
            if (!isSomething(value)) value = sourceAtts.getString("long_name");
            //from title metadata?    priority to source if >20 chars
            if (!isSomething(value)) {
                value = "";
                String sTitle = sourceAtts.getString("title");
                String aTitle =    addAtts.getString("title");
                if (!isSomething(sTitle)) sTitle =  sourceAtts.getString("Title");          
                if (!isSomething(aTitle)) aTitle =     addAtts.getString("Title");   
                sTitle = removeExtensionsFromTitle(String2.whitespacesToSpace(sTitle));
                aTitle = removeExtensionsFromTitle(String2.whitespacesToSpace(aTitle));
                if (!isSomething(aTitle))
                    aTitle = sourceUrlAsTitle;

                //use both sourceTitle and addTitle (if available and not the same)
                String pre = "", post = "";
                if (isSomething(sTitle)) value = sTitle;
                if (isSomething(sTitle) && isSomething(aTitle)) {
                    if (String2.looselyContains(sTitle, aTitle)) {
                        aTitle = "";
                    } else {
                        pre = " (";
                        post = ")";
                    }
                }
                if (isSomething(aTitle) &&
                    !String2.looselyContains(value, aTitle)) 
                    value += pre + aTitle + post;
            }
            value = removeExtensionsFromTitle(value);
        }
        //if suffixForTitle's text isn't already included, add it
        if (isSomething(value)) {
            if (isSomething(suffixForTitle) &&
                !String2.looselyContains(value, suffixForTitle)) 
                value += suffixForTitleP;
        } else {
            value = suffixForTitle;
        }
        
        if (!isSomething(value))
            value = ""; //not null. useful below.

        String tValue = "";
        //add NOTE1/2/3/4/5 to summary
        for (int i = 1; i <= 5; i++) 
            if (value.length() < 800 &&
                isSomething(tValue = sourceAtts.getString("NOTE" + i)) &&
                !String2.looselyContains(value, tValue))
                value = String2.periodSpaceConcat(value, tValue);
        
        //add comment1/2/3/4/5 to summary
        for (int i = 1; i <= 5; i++) 
            if (value.length() < 800 &&
                isSomething(tValue = sourceAtts.getString("comment" + i)) &&
                !String2.looselyContains(value, tValue)) 
                value = String2.periodSpaceConcat(value, tValue);

        //remove badly escaped FF(?)
        if (isSomething(value) && value.indexOf("\\012") >= 0) {
            //change it
            value = String2.replaceAll(value, "\\\\\\\\012", "\n"); 
            value = String2.replaceAll(value, "\\\\\\012",   "\n"); 
            value = String2.replaceAll(value, "\\\\012",     "\n"); 
            value = String2.replaceAll(value, "\\012",       "\n"); 
            //There are datasets with just 012, but it's dangerous to change those.
        }
        String tSummary = isSomething(value)? value : "";


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
            if (!isSomething(value)) value =    addAtts.getString("related_url");
            if (!isSomething(value)) value = sourceAtts.getString("related_url");
            if (!isSomething(value)) value =    addAtts.getString("html_BACKGROUND");            
            if (!isSomething(value)) value = sourceAtts.getString("html_BACKGROUND");            
            if (!isSomething(value)) {
                value =                             addAtts.getString("documentation");            
                if (!isSomething(value)) value = sourceAtts.getString("documentation");            
                if (isSomething(value) && !value.startsWith("http")) 
                    value = "";  
                //else ssv or csv list dealt with below
            }
            if (!isSomething(value)) {
                value =                             addAtts.getString("references");            
                if (!isSomething(value)) value = sourceAtts.getString("references");            
                if (isSomething(value) && !value.startsWith("http")) 
                    value = "";  
                //else ssv or csv list dealt with below
            }
            if (isSomething(value)) {
                //deal with ssv or csv list
                int po = value.indexOf(' ');
                if (po >= 0) value = value.substring(0, po);
                po = value.indexOf(',');
                if (po >= 0) value = value.substring(0, po);
            }
            if (!isSomething(value)) {
                if (tPublicSourceUrl.startsWith("http")) {
                    if (tPublicSourceUrl.startsWith("http://nomads.ncep.noaa.gov") &&
                        tPublicSourceUrl.indexOf("/rtofs/") > 0) {
                        value = "http://polar.ncep.noaa.gov/global/";
                    } else if (sourceUrlIsHyraxFile) {  
                        value = File2.getDirectory(tPublicSourceUrl) + ".html";
                    } else if (sourceUrlIsHyraxCatalog) {  
                        value = File2.getDirectory(tPublicSourceUrl) + "contents.html";
                    } else if (sourceUrlIsThreddsCatalog) {  
                        value = File2.getDirectory(tPublicSourceUrl) + "catalog.html";
                    } else {
                        value = tPublicSourceUrl + 
                            ((tPublicSourceUrl.indexOf("/thredds/") > 0 ||       //most DAP servers, add .html:  THREDDS
                              tPublicSourceUrl.indexOf("/dodsC/") > 0 ||
                              tPublicSourceUrl.indexOf("dap/") > 0)? ".html" :   //  and opendap/, tabledap/, griddap/, 
                            tPublicSourceUrl.indexOf("/dapper/") > 0? ".das" : //for DAPPER, add .das (no .html)
                            ""); 
                    }
                } else { 
                    if (!isSomething(value)) value =    addAtts.getString("creator_url"); 
                    if (!isSomething(value)) value = sourceAtts.getString("creator_url");
                    if (!isSomething(value)) value = "???";
                }
            }
            if (reallyVerbose) String2.log("  new " + name + "=" + value);
        }
        //special case: fix common out-of-date URL
        if ("http://edac-dap2.northerngulfinstitute.org/ocean_nomads/NCOM_index_map.html".equals(value))
            value = "http://ecowatch.ncddc.noaa.gov/global-ncom/";
        String infoUrl = value;

        //institution
        name = "institution";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            if (!isSomething(value)) {
                                         value =    addAtts.getString("Institution"); //wrong case?
                if (!isSomething(value)) value = sourceAtts.getString("Institution");
                if ( isSomething(value)) addAtts.set("Institution", "null"); //not just remove()
            }
            if (!isSomething(value)) value = sourceAtts.getString("origin");
            if (!isSomething(value)) value = sourceAtts.getString("Originating_or_generating_Center"); //grib
            if (reallyVerbose) String2.log("  new " + name + "=" + value);
        }
        String tInstitution = isSomething(value)? value : "";
        if (tInstitution.indexOf("produced by the EUMETSAT O&SI SAF") >= 0)
            tInstitution = "EUMETSAT OSI SAF";
        if (debugMode) String2.log(">> 1 tInstitution=" + tInstitution);

        //take apart an email address (for creator_name, creator_email, tInstitution information) 
        String tContact = null;
        if (!isSomething(tContact)) tContact = addAtts.getString("contact");
        if (!isSomething(tContact)) tContact = addAtts.getString("Contact");
        if (!isSomething(tContact)) tContact = addAtts.getString("contact_person_name");
        if (!isSomething(tContact)) tContact = sourceAtts.getString("contact");
        if (!isSomething(tContact)) tContact = sourceAtts.getString("Contact");
        if (!isSomething(tContact)) tContact = sourceAtts.getString("contact_person_name");
        String tEmailSource = creator_email;
        if (!isSomething(tEmailSource) && isSomething(tContact) && tContact.indexOf('@') > 0)
            tEmailSource = tContact;
        String tAuthor = sourceAtts.getString("author");
        if (!isSomething(tEmailSource) && isSomething(tAuthor) && tAuthor.indexOf('@') > 0)
            tEmailSource = tAuthor;
        String tReference = sourceAtts.getString("reference");
        if (!isSomething(tEmailSource) && isSomething(tReference) && tReference.indexOf('@') > 0)
            tEmailSource = tReference;
        tReference = sourceAtts.getString("references");
        if (!isSomething(tEmailSource) && isSomething(tReference) && tReference.indexOf('@') > 0)
            tEmailSource = tReference;
        if (!isSomething(tEmailSource) && tSummary.indexOf('@') > 0) 
            tEmailSource = tSummary;
        if (isSomething(tEmailSource)) {
            StringArray contactParts = StringArray.wordsAndQuotedPhrases(tEmailSource);
            int nParts = contactParts.size();
            for (int parti = 0; parti < nParts; parti++) {
                if (contactParts.get(parti).indexOf('@') < 0) 
                    continue;
                String part = contactParts.get(parti);
                while (part.startsWith("(") || part.startsWith("[") || part.startsWith("<"))
                    part = part.substring(1);
                while (part.endsWith(")") || part.endsWith("]") || part.endsWith(">"))
                    part = part.substring(0, part.length() - 1);
                if (String2.isEmailAddress(part)) {
                    if (!isSomething(creator_email))
                        creator_email = part;
                    if (!isSomething(creator_name)) {
                        int po = part.indexOf('@');  //it must exist

                        //creator_name e.g., Bob Simons from bob.simons, or erd.data, rsignell
                        creator_name = part.substring(0, po); 
                        creator_name = String2.replaceAll(creator_name, '.', ' '); 
                        creator_name = String2.replaceAll(creator_name, '_', ' '); 
                        creator_name = String2.replaceAll(creator_name, '-', ' '); 
                        creator_name = String2.whitespacesToSpace(creator_name);
                        creator_name = creator_name.indexOf(' ') >= 0?
                            String2.toTitleCase(creator_name) :
                            creator_name.toUpperCase();  //it's probably an acronym
                        String lcContactName = creator_name.toLowerCase();
                        if (lcContactName.indexOf("desk") >= 0 ||
                            lcContactName.indexOf("help") >= 0 ||
                            lcContactName.indexOf("info") >= 0 ||
                            lcContactName.indexOf("service") >= 0 ||
                            lcContactName.indexOf("support") >= 0 ||
                            lcContactName.indexOf("webmaster") >= 0) 
                            creator_name = "";
                    }

                    //tInstitution from email address
                    //e.g., PODAAC JPL NASA from podaac.jpl.nasa.gov 
                    if (!isSomething(tInstitution)) {
                        int po = part.indexOf('@');  //it must exist
                        tInstitution = part.substring(po + 1);  
                        po = tInstitution.lastIndexOf('.'); //it must exist
                        //toUpperCase since probably acronym
                        tInstitution = tInstitution.substring(0, po).toUpperCase(); 
                        if (tInstitution.equals("PODAAC JPL NASA"))
                            tInstitution = "JPL NASA";
                        if (debugMode) String2.log(">> 2 tInstitution=" + tInstitution);
                    }

                    break;
                }
                parti++;
            }
        }

        //info for specific datasets/projects encountered by UAF ERDDAP
        //Since these set creator_email etc, they are powerful. Only use if essentially always true.
        {
            String lcUrl = tPublicSourceUrl.toLowerCase();
            String tIns = addAtts.getString("institution");
            if (!isSomething(tIns)) tIns = sourceAtts.getString("institution");
            if (!isSomething(tIns)) tIns = "";
            if (debugMode) String2.log(">> in  specific: creator email=" + creator_email + " name=" + creator_name + " url=" + creator_url + " institution=" + tIns);
            String taTitle =    addAtts.getString("title");
            String tsTitle = sourceAtts.getString("title");
            if (!isSomething(taTitle)) taTitle = "";
            if (!isSomething(tsTitle)) tsTitle = "";
            String lcaTitle = taTitle.toLowerCase();
            String lcsTitle = tsTitle.toLowerCase();
            //coads
            if (lcUrl.indexOf("/coads/") >= 0 ||
                lcUrl.indexOf("/icoads/") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Eric.Freeman@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA ICOADS";                   
                if (!isSomething(tInstitution))  tInstitution  = "NOAA ICOADS";                   
                if (!isSomething(creator_url))   creator_url   = "http://icoads.noaa.gov/";                   
            //crm
            } else if (lcUrl.indexOf("/crm/") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Barry.Eakins@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA NGDC";                                   
                if (!isSomething(tInstitution))  tInstitution  = "NOAA NGDC";                                   
                if (!isSomething(creator_url))   creator_url   = "http://www.ngdc.noaa.gov/mgg/coastal/crm.html";                   
            //etopo
            } else if (lcUrl.indexOf("/etopo") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Barry.Eakins@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA NGDC";                   
                if (!isSomething(tInstitution))  tInstitution  = "NOAA NGDC";                   
                if (!isSomething(creator_url))   creator_url   = "http://www.ngdc.noaa.gov/mgg/global/global.html";                   
            //gfdl cm  (climate model)
            } else if (lcUrl.indexOf(".gfdl.noaa.gov") >= 0 &&
                       (lcUrl.indexOf("_cm") >= 0 ||
                        lcUrl.indexOf("/cm") >= 0)) {
                if (!isSomething(creator_name))  creator_name  = "NOAA GFDL";                   
                if (!isSomething(tInstitution))  tInstitution  = "NOAA GFDL";                   
                if (!isSomething(creator_url))   creator_url   = "http://data1.gfdl.noaa.gov/nomads/forms/deccen/";                   
            //godas
            } else if (lcUrl.indexOf("/godas/") >= 0 ||
                       lcaTitle.indexOf("godas") >= 0 ||
                       lcsTitle.indexOf("godas") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Yan.Xue@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA NCEP";                                   
                if (!isSomething(tInstitution))  tInstitution  = "NOAA NCEP";                                   
                if (!isSomething(creator_url))   creator_url   = "http://www.cpc.ncep.noaa.gov/products/GODAS/";                   
            //NCOM 
            } else if (taTitle.indexOf("NCOM") >= 0 || //a project
                       tsTitle.indexOf("NCOM") >= 0 || 
                         lcUrl.indexOf("/ncom") >= 0) {
                if (!isSomething(creator_email)) creator_email = "frank.bub@navy.mil";                   
                if (!isSomething(creator_name))  creator_name  = "Naval Research Lab (NRL)";                   
                if (!isSomething(tInstitution))  tInstitution  = "Naval Research Lab (NRL)";
                if (!isSomething(creator_url))   creator_url   = "www7320.nrlssc.navy.mil/global_ncom/";                   
            //ncep reanalysis
            } else if (lcUrl.indexOf("ncep") >= 0 &&
                lcUrl.indexOf("reanalysis") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Wesley.Ebisuzaki@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA NCEP, NCAR";
                if (!isSomething(tInstitution))  tInstitution  = "NOAA NCEP, NCAR";
                if (!isSomething(creator_url))   creator_url   = "http://www.cpc.ncep.noaa.gov/products/wesley/reanalysis.html";                   
            //ncsu roms/toms  (several people run ROMS/TOMS at NCSU, but rhe is only email I found)
            } else if (lcUrl.indexOf("meas.ncsu.edu") >= 0 ||
                       lcaTitle.indexOf("roms/toms") >= 0 ||
                       lcsTitle.indexOf("roms/toms") >= 0) {
                if (!isSomething(creator_email)) creator_email = "rhe@ncsu.edu";    
                if (!isSomething(creator_name))  creator_name  = "Ruoying He";                 
                if (!isSomething(tInstitution))  tInstitution  = "NCSU";
            //ngdc dem
            } else if (lcUrl.indexOf("ngdc.noaa.gov") >= 0 &&
                lcUrl.indexOf("/dem/") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Barry.Eakins@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA NGDC";
                if (!isSomething(tInstitution))  tInstitution  = "NOAA NGDC";
                if (!isSomething(creator_url))   creator_url   = "http://www.ngdc.noaa.gov/mgg/dem/demportal.html";                   
            //osmc
            } else if (lcUrl.indexOf("osmc.noaa.gov") >= 0) {
                if (!isSomething(creator_email)) creator_email = "OSMC.Webmaster@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NOAA OSMC";                   
                if (!isSomething(tInstitution))  tInstitution  = "NOAA OSMC";
                if (!isSomething(creator_url))   creator_url   = "http://www.osmc.noaa.gov";                   
            //podaac ccmp flk
            } else if (lcUrl.indexOf("podaac") >= 0 &&
                       lcUrl.indexOf("/ccmp/") >= 0 &&
                       lcUrl.indexOf("/flk/") >= 0) {
                if (!isSomething(creator_email)) creator_email = "podaac@podaac.jpl.nasa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "NASA GSFC MEaSUREs, NOAA";                   
                if (!isSomething(tInstitution))  tInstitution  = "NASA GSFC, NOAA";
                if (!isSomething(creator_url))   creator_url   = "http://podaac.jpl.nasa.gov/dataset/CCMP_MEASURES_ATLAS_L4_OW_L3_0_WIND_VECTORS_FLK";                   
            //rutgers roms 
            } else if (lcUrl.indexOf("marine.rutgers.edu") >= 0 &&
                       lcUrl.indexOf("/roms/") >= 0) {
                if (!isSomething(creator_email)) creator_email = "jwilkin@marine.rutgers.edu";                   
                if (!isSomething(creator_name))  creator_name  = "Rutgers Marine";                                   
                if (!isSomething(tInstitution))  tInstitution  = "Rutgers";
                if (!isSomething(creator_url))   creator_url   = "http://marine.rutgers.edu/po/";                   

            //TRMM 
            } else if (tPublicSourceUrl.indexOf("TRMM") >= 0 &&
                       lcUrl.indexOf("nasa") >= 0) {
                if (!isSomething(creator_email)) creator_email = "Harold.F.Pierce@nasa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "Tropical Rainfall Measuring Mission (TRMM)";                                   
                if (!isSomething(tInstitution))  tInstitution  = "NASA, JAXA";
                if (!isSomething(creator_url))   creator_url   = "http://trmm.gsfc.nasa.gov/";                   
                if (!isSomething(infoUrl))       infoUrl       = "http://trmm.gsfc.nasa.gov/";                   
                if (!isSomething(tSummary))      tSummary =  
                    "The Tropical Rainfall Measuring Mission (TRMM) is a joint mission between NASA and the Japan Aerospace Exploration Agency (JAXA) designed to monitor and study tropical rainfall.";         
                
            //woa
            } else if (lcUrl.indexOf("/woa01") >= 0 ||
                       lcUrl.indexOf("/woa05") >= 0 ||
                       lcUrl.indexOf("/woa09") >= 0) {
                if (!isSomething(creator_email)) creator_email = "OCL.help@noaa.gov";                   
                if (!isSomething(creator_name))  creator_name  = "CSIRO, NOAA NODC";                   
                if (!isSomething(tInstitution))  tInstitution  = "CSIRO, NOAA NODC";                   
                if (!isSomething(creator_url))   creator_url   = 
                    "http://www.nodc.noaa.gov/OC5/WOA" +
                        (lcUrl.indexOf("/woa01") >= 0? "01/pr_woa01.html" :
                         lcUrl.indexOf("/woa05") >= 0? "05/pr_woa05.html" :
                                                       "09/pr_woa09.html");                   
            }
            if (debugMode) String2.log(">> out specific: creator email=" + creator_email + " name=" + creator_name + " url=" + creator_url + " institution=" + tIns);
        }

        if (!isSomething(creator_url) && 
            "Earth & Space Research".equals(addAtts.getString("institution")))
            creator_url = "https://www.esr.org/";

        //sources that create most/all of what distribute.  Do last.  Better than nothing.
        if (!isSomething(creator_email)) {
            String taTitle =    addAtts.getString("title");
            String tsTitle = sourceAtts.getString("title");
            if (!isSomething(taTitle)) taTitle = "";
            if (!isSomething(tsTitle)) tsTitle = "";
            String tIns = addAtts.getString("institution");
            if (!isSomething(tIns)) tIns = sourceAtts.getString("institution");
            if (!isSomething(tIns)) tIns = "";
            //cwcgom.aoml
            if (tPublicSourceUrl.startsWith("http://cwcgom.aoml.noaa.gov")) {
                if (!isSomething(creator_email)) creator_email = "Joaquin.Trinanes@noaa.gov";     
                if (!isSomething(creator_name))  creator_name  = "Joaquin Trinanes";
                if (!isSomething(tInstitution))  tInstitution  = "NOAA NESDIS CWCGOM, NOAA AOML";               
            //hycom
            } else if (tPublicSourceUrl.indexOf("hycom.org/") >= 0) {
                if (!isSomething(creator_email)) creator_email = "hycomdata@coaps.fsu.edu"; 
                if (!isSomething(creator_name))  creator_name  = "HYCOM";
                if (!isSomething(tInstitution))  tInstitution  = "HYCOM";               
                if (!isSomething(creator_url))   creator_url   = "http://hycom.org/";
            //NAVO (before NRL)
            } else if (taTitle.indexOf("NAVO") >= 0 || //an office, AKA  NAVOCEANO
                       tsTitle.indexOf("NAVO") >= 0 || 
                       tPublicSourceUrl.indexOf("/navo") >= 0 ||
                       tPublicSourceUrl.indexOf("/NAVO") >= 0) {
                if (!isSomething(creator_email)) creator_email = "CSO.navo.fct@navy.mil";                   
                if (!isSomething(creator_name))  creator_name  = "Naval Research Lab (NRL) NAVOCEANO"; 
                if (!isSomething(tInstitution))  tInstitution  = "Naval Research Lab (NRL) NAVOCEANO"; 
                if (!isSomething(creator_url))   creator_url   = "http://www.usno.navy.mil/NAVO";
            //NRL (after NAVO and NCOM)
            } else if (tIns.indexOf("Naval Research Lab") >= 0 || 
                       tIns.indexOf("NRL") >= 0 ||
                       taTitle.indexOf("NRL") >= 0 ||
                       tsTitle.indexOf("NRL") >= 0) {
                //if (!isSomething(creator_email)) creator_email = "firstname.lastname@nrlmry.navy.mil";                   
                if (!isSomething(creator_name))  creator_name  = "Naval Research Lab (NRL)";
                if (!isSomething(tInstitution))  tInstitution  = "Naval Research Lab (NRL)";
                if (!isSomething(creator_url))   creator_url   = "http://www.nrl.navy.mil/";
            //podaac
            } else if ((tPublicSourceUrl.indexOf("podaac") >= 0 ||
                        tPublicSourceUrl.indexOf("opendap-uat") >= 0) &&
                        tPublicSourceUrl.indexOf("jpl.nasa.gov") >= 0) {
                if (tInstitution.equals("NASA/GSFC OBPG")) {
                    if (!isSomething(creator_email)) creator_email = "webadmin@oceancolor.gsfc.nasa.gov"; 
                    if (!isSomething(creator_name))  creator_name  = tInstitution;
                    if (!isSomething(creator_url))   creator_url   = "http://oceancolor.gsfc.nasa.gov/cms/"; 
                } else {
                    if (!isSomething(creator_email)) creator_email = "podaac@podaac.jpl.nasa.gov"; 
                    if (!isSomething(creator_name))  creator_name  = "NASA JPL PODAAC";
                    if (!isSomething(tInstitution))  tInstitution  = "NASA JPL";
                    if (!isSomething(creator_url))   creator_url   = "http://podaac.jpl.nasa.gov/"; 
                }
            //WHOI  (Rich Signell is a good contact; unfortunately, many datasets are from other sources)
            } else if (tPublicSourceUrl.startsWith("http://geoport.whoi.edu/thredds/")) {
                if (!isSomething(creator_email)) creator_email = "rsignell@usgs.gov"; 
                if (!isSomething(creator_name))  creator_name  = "USGS, WHCMSC Sediment Transport Group";
                if (!isSomething(tInstitution))  tInstitution  = "USGS, WHCMSC Sediment Transport Group";
                //if (!isSomething(creator_url))   creator_url   = ""; 
            }
        }
        
        //institution (again)
        name = "institution";
        if (!isSomething(tInstitution)) {

            //find a related url
            value = creator_url;
            if (!isSomething(value) || !value.startsWith("http")) value = infoUrl;
            if (!isSomething(value) || !value.startsWith("http")) value =    addAtts.getString("related_url");
            if (!isSomething(value) || !value.startsWith("http")) value = sourceAtts.getString("related_url");
            if (!isSomething(value) || !value.startsWith("http")) value = tPublicSourceUrl; //could be the publisher
            if (isSomething(value) && value.startsWith("http"))
                tInstitution = suggestInstitution(value);  //now NOAA PFEL
            if (debugMode) String2.log(">> 3 tInstitution=" + tInstitution);
        }

        //fix mistakes in some datasets
        tInstitution = String2.replaceAll(tInstitution, "NOA ESRL", "NOAA ESRL");

        //use common abbreviations in tInstitution
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "NOAA CoastWatch, West Coast Node",
            "NOAA CoastWatch WCN");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "Fleet Numerical Meteorology and Oceanography Center, Monterey, CA, USA",
            "FNMOC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "Fleet Numerical Meteorology and Oceanography Center",
            "FNMOC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "Earth System Research Laboratory",
            "ESRL");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "Goddard Space Flight Center",
            "GSFC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "National Climatic Data Center",
            "NCDC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "DOC/NOAA/NESDIS/NGDC > National Geophysical Data Center",
            "NOAA NGDC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "National Geophysical Data Center",
            "NGDC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "National Oceanographic Data Center",
            "NGDC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "NOAA, U.S. Department of Commerce",
            "NOAA");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "DOC/NOAA/NESDIS/OSDPD > Office of Satellite Data Processing and Distribution",
            "NOAA OSPO"); //new name
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "USDOC/NOAA/NESDIS CoastWatch",
            "NOAA CoastWatch");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "National Data Buoy Center",
            "NDBC");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "National Weather Service",
            "NWS");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "Scripps Institution of Oceanography",
            "Scripps");
        tInstitution = String2.replaceAllIgnoreCase(tInstitution, 
            "Woods Hole Oceanographic Institution",
            "WHOI");
        if (String2.indexOf(
            new String[]{"NCDC", "NCDDC", "NDBC", "NESDIS", "NGDC", "NODC", "NWS"},
            tInstitution) >= 0)
            tInstitution = "NOAA " + tInstitution;

        if (tInstitution.length() > 20) {
            //parentheses?  keep shorter of in or out text
            StringBuilder newValue = new StringBuilder();
            while (tInstitution.length() > 0) {
                int ppo1 = tInstitution.indexOf('(');
                int ppo2 = tInstitution.indexOf(')');
                if (ppo1 <= 0 || ppo2 <= ppo1)  //no paren
                    break;
                String out = tInstitution.substring(0, ppo1).trim();
                String in  = tInstitution.substring(ppo1 + 1, ppo2);
                newValue.append(out.length() < in.length()? out : in);
                tInstitution = tInstitution.substring(ppo2 + 1).trim();

                if (tInstitution.startsWith(". ") ||
                    tInstitution.startsWith(", ") ||
                    tInstitution.startsWith("; ")) {
                    newValue.append(tInstitution.substring(0, 2));
                    tInstitution = tInstitution.substring(2);
                } else if (tInstitution.startsWith("and ")) {
                    newValue.append(" " + tInstitution.substring(0, 4));
                    tInstitution = tInstitution.substring(4);
                } else {
                    newValue.append(' ');
                }
            }
            newValue.append(tInstitution); //the remainder
            tInstitution = newValue.toString().trim();
        }
        if (debugMode) String2.log(">> 4 tInstitution=" + tInstitution);

        //creator_name again
        if (!isSomething(creator_name)) {
            if (tPublicSourceUrl.indexOf("/psd/") < 0) //dealt with below
                creator_name = tInstitution;
        }

        //history
        name = "history";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            if (!isSomething(value)) {
                                     value =    addAtts.getString("History"); //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("History");
            if ( isSomething(value))                  addAtts.set("History", "null"); //not just remove()
            }

            if (!isSomething(value)) {
                                     value =    addAtts.getString("_History"); //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("_History");
            if ( isSomething(value))                  addAtts.set("_History", "null"); //not just remove()
            }

            if (!isSomething(value)) {
                                     value =    addAtts.getString("mac_history"); 
            if (!isSomething(value)) value = sourceAtts.getString("mac_history");
            if ( isSomething(value))                  addAtts.set("mac_history", "null"); //not just remove()
            }

            if (!isSomething(value)) value =    addAtts.getString("source");
            if (!isSomething(value)) value = sourceAtts.getString("source");

            //there doesn't have to be a history attribute
            if (isSomething(value)) 
                addAtts.add(name, value);
        }

        //keywords below, after title

        //license
        name = "license";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            if (!isSomething(value)) {
                                     value =    addAtts.getString("License");  //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("License");
            if ( isSomething(value))                  addAtts.set("License", "null"); //not just remove()
            }

            //e.g., http://michigan.glin.net:8080/thredds/dodsC/glos/glcfs/michigan/ncasf_his3d/Lake_Michigan_Forecast_Collection_3D_fmrc.ncd.html
            if (!isSomething(value)) value = sourceAtts.getString("disclaimer"); 
            if (!isSomething(value)) value = sourceAtts.getString("Disclaimer"); 
            //A lot of ex-military datasets have distribution_statement:
            //e.g., http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/region1/ncom_glb_reg1_2010013000.nc.das
            if (!isSomething(value)) value = sourceAtts.getString("distribution_statement"); 
            if (!isSomething(value)) value = "[standard]"; //if nothing else
            addAtts.add(name, value);
        }

        //standard_name_vocabulary
        name = "standard_name_vocabulary";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value) || 
            //this isn't very sophisticated:
            //ensure it is the new ACDD-1.3 style "CF Standard Name Table v29"
            !value.matches("CF Standard Name Table v[0-9]{2,}")) 
            addAtts.add(name, FileNameUtility.getStandardNameVocabulary());

        //improve summary?
        if (!isSomething(tSummary) || tSummary.length() < 30) {

            value = isSomething(tInstitution)? tInstitution + " data " : "Data ";
            if (sourceUrlIsHyraxFile || sourceUrlIsHyraxCatalog ||
                sourceUrlIsThreddsCatalog)  
                 value += "from " + infoUrl;
            else if (tPublicSourceUrl.startsWith("http"))
                 value += "from " + tPublicSourceUrl + ".das ."; 
            else value += "from a local source.";
            if (isSomething(suffixForTitle) &&
                !String2.looselyContains(value, suffixForTitle) &&
                !String2.looselyContains(tSummary, suffixForTitle))
                value += suffixForTitleP;
            tSummary = String2.periodSpaceConcat(tSummary, value);
        }

        //title
        name = "title";
        {
            String sTitle = removeExtensionsFromTitle(sourceAtts.getString(name)); 
            String aTitle = removeExtensionsFromTitle(   addAtts.getString(name)); //from THREDDS catalog is complicated
            if        (!isSomething(sTitle)) { value = aTitle;
            } else if (!isSomething(aTitle)) { value = sTitle;
            } else {
                //use both
                if (sTitle.length() >= aTitle.length()) {
                    value = sTitle;
                    if (!String2.looselyContains(sTitle, aTitle) &&
                        value.length() + aTitle.length() + 3 < 77)
                        value += " (" + aTitle + ")";
                } else {
                    value = aTitle;
                    if (!String2.looselyContains(aTitle, sTitle) &&
                        value.length() + sTitle.length() + 3 < 77)
                        value += " (" + sTitle + ")";
                }
            }
            if (debugMode) String2.log(">> 1 title=" + value);
            String l3smi = "Level-3 Standard Mapped Image";
            {  //always, so Title is removed
                String                   Value =    addAtts.getString("Title");  //wrong case?
                if (!isSomething(Value)) Value = sourceAtts.getString("Title");
                if ( isSomething(Value)) {
                    addAtts.set("Title", "null"); //not just remove()
                    if (!isSomething(value)) {
                        value = Value;
                        int po = value.indexOf(l3smi); //at jpl, often the entire Title
                        if (po >= 0) {
                            if (sourceUrlAsTitle.indexOf("L3") >= 0 &&
                                sourceUrlAsTitle.indexOf("SMI") >= 0)
                                value = String2.replaceAll(value, l3smi, 
                                    sourceUrlAsTitle);
                            else value = String2.replaceAll(value, l3smi, 
                                    "L3 SMI, " + sourceUrlAsTitle);

                            if (tSummary.indexOf(l3smi) < 0) 
                                tSummary = l3smi + ". " + tSummary;
                        }
                    }
                }
            }
            if (!isSomething(value)) value =    addAtts.getString("long_name");
            if (!isSomething(value)) value = sourceAtts.getString("long_name");
            value = String2.whitespacesToSpace(value);
            value = removeExtensionsFromTitle(value);
            if (debugMode) String2.log(">> 2 title=" + value);

            //use suffixForTitle?  (see add suffixForTitle below)
            //for fromThreddsCatalog, sourceTitle + suffixForTitle is good combo
            //String2.log(">> value.length=" + value.length() + " suffix.length=" + suffixForTitle.length());
            if (!isSomething(value)) 
                value = suffixForTitle;

            //get from summary-like metadata? 
            if (!isSomething(value) && isSomething(tSummary) &&
                !tSummary.startsWith("WARNING")) {
                value = tSummary;
                if (isSomething(value) && value.length() > 80) 
                    value = value.substring(0, 60) + " ...";
            }
            if (debugMode) String2.log(">> 3 title=" + value);

            //hyrax or thredds catalog?   use last two directory names
            if (!isSomething(value) && 
                (sourceUrlIsHyraxCatalog || sourceUrlIsThreddsCatalog)) {
                value = File2.getDirectory(tPublicSourceUrl);
                int po = value.lastIndexOf("/");
                if (po > 0) po = value.substring(0, po).lastIndexOf("/");
                value = po > 0 && po < value.length() - 1? 
                    value.substring(po + 1) : 
                    value;
                value = String2.replaceAll(value, '/', ' ');
                value = String2.whitespacesToSpace(String2.replaceAll(value, '_', ' '));
            }
            if (debugMode) String2.log(">> 4 title=" + value);

            //if nothing else, use last part of url or directory, e.g., 2010/KH20060101_ab
            //if dir, remove trailing slash
            if (!isSomething(value)) {
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
                value = String2.whitespacesToSpace(String2.replaceAll(value, '_', ' '));
            }

            //add a little something?  expand common acronyms   [title]
            if (!isSomething(value))
                value = ""; //not e.g., "???"
            if (!isSomething(value) || value.length() < 40) {
                if (tPublicSourceUrl.indexOf("/crm/") >=0 &&
                    value.toLowerCase().indexOf("coastal relief model") < 0) {
                    value = String2.periodSpaceConcat("NOAA NGDC U.S. Coastal Relief Model.", value);
                } else if (tPublicSourceUrl.indexOf("/dem/") >= 0 &&
                    value.toLowerCase().indexOf("elevation") < 0) {
                    value = String2.periodSpaceConcat("Digital Elevation Model.", value);
                } else if ((value.toLowerCase().indexOf("etopo") >= 0 ||
                            tPublicSourceUrl.indexOf("etopo") >=0) &&
                           value.toLowerCase().indexOf("topography") < 0 &&
                           value.toLowerCase().indexOf("bathymetry") < 0) {
                    value = String2.periodSpaceConcat("ETOPO Global Topography and Bathymetry.", value);
                } else if (value.indexOf("MSG SST") >= 0 &&
                           !String2.looselyContains(value, "Meteosat Second Generation")) {
                    value = String2.replaceAll(value, "MSG SST", 
                            "Meteosat Second Generation (MSG) SST");
                }
            }
            if (debugMode) String2.log(">> 5 title=" + value);

            //move l3smi from title to summary?
            int po = value.indexOf(l3smi);
            if (po >= 0) {
                String cs = value.indexOf(l3smi + ", ") >= 0? ", " : "";
                if (value.indexOf("L3") >= 0 &&
                    value.indexOf("SMI") >= 0)
                     value = String2.replaceAll(value, l3smi + cs, "");
                else value = String2.replaceAll(value, l3smi + cs, "L3 SMI" + cs);

                if (!String2.looselyContains(tSummary, l3smi)) {
                    tSummary = l3smi + ". " + tSummary;
                }
            }

            //shorten the title?
            //preserve original title (add oTitle to summary below if it changes)
            String oTitle = removeExtensionsFromTitle(sourceAtts.getString("title")); 
            if (!isSomething(oTitle))
                oTitle = value;
            if (isSomething(oTitle)) 
                 oTitle = String2.replaceAllIgnoreCase(String2.whitespacesToSpace(oTitle), " , ", ", ");
            else oTitle = "";

            //special cases:
            //sourceUrl=http://data1.gfdl.noaa.gov:8380/thredds3/dodsC/ipcc_ar4_CM2.0_R2_20C3M-0_monthly_ice_tripolar_18610101-20001231
            if (value.matches("GFDL CM.{3}, 20C3M \\(run .\\) climate of the 20th Century " +
                    "experiment \\(20C3M\\) output for IPCC AR. and US CCSP") &&
                isSomething(sourceUrlAsTitle)) {
                value = sourceUrlAsTitle; 
                value = String2.replaceAll(value, "ipcc ar", "IPCC AR");
            }
            //sourceUrl=http://data1.gfdl.noaa.gov:8380/thredds3/dodsC/CM2.1U_CDFef_v1.0_r6land
            if (//            ...forecasts - CM2.1U_CDFef_V1.0                  cap V
                value.matches(         ".* - CM...U_CD..._V...") &&
                //                           CM2.1U CDFef v1.0 r6land        little v
                sourceUrlAsTitle.matches(   "CM...U CD... v.*")) {
                value = value.replaceFirst( "CM...U_CD..._V...", sourceUrlAsTitle);
            }
            //simple cases:
            value = String2.replaceAllIgnoreCase(value,
                "Archived NOAA",
                "NOAA");
            value = String2.replaceAllIgnoreCase(value,
                " climate of the 20th Century experiment (20C3M) output for IPCC AR4 and US CCSP",
                "");
            value = String2.replaceAllIgnoreCase(value,
                "CLIVAR model output prepared for GFDL Seasonal-Interannual Experimental Forecasts",
                "CLIVAR model output (for GFDL SI)");
            value = String2.replaceAllIgnoreCase(value,
                "COAWST Forecast System : USGS :",
                "USGS COAWST Forecast,");
            value = String2.replaceAllIgnoreCase(value,
                "CPC Unified Gauge-Based Analysis of Daily Precipitation over CONUS at PSD",
                "CPC Daily Precipitation over CONUS");
            value = String2.replaceAllIgnoreCase(value,
                "Daily Values using AVHRR only",
                "AVHRR only, Daily");
            value = String2.replaceAllIgnoreCase(value,
                "Model output prepared for GFDL Seasonal-Interannual experimental forecasts -",
                "GFDL Seasonal-Interannual Forecast,");
            value = String2.replaceAllIgnoreCase(value,
                "Model output prepared for GFDL Seasonal-Interannual experimental forecasts ",
                "GFDL Seasonal-Interannual Forecast, ");
            value = String2.replaceAllIgnoreCase(value,
                "GFDL SI system initialized from ensemble filter fully coupled data assimilation",
                "GFDL SI fully coupled data assimilation");
            value = String2.replaceAllIgnoreCase(value,
                "GISS Surface Temperature Analysis (GISTEMP)",
                "GISTEMP");            
            value = String2.replaceAllIgnoreCase(value,
                "GODAS: Global Ocean Data Assimilation System",
                "GODAS");
            value = String2.replaceAllIgnoreCase(value,
                "Global Ocean Data Assimilation System",
                "GODAS");             
            value = String2.replaceAllIgnoreCase(value,
                "HYbrid Coordinate Ocean Model (HYCOM): Global",
                "HYCOM, Global");
            value = String2.replaceAllIgnoreCase(value,
                "HYbrid Coordinate Ocean Model (HYCOM)",
                "HYCOM");
            value = String2.replaceAllIgnoreCase(value,
                "HYbrid Coordinate Ocean Model",
                "HYCOM");
            value = String2.replaceAllIgnoreCase(value,
                " Monthly Means appended with GPCC monitoring dataset from 2011 onwards",
                "");
            value = String2.replaceAllIgnoreCase(value,
                "NAVO NCOM Relocatable Model",
                "NAVO NCOM");
            value = String2.replaceAllIgnoreCase(value,
                "USDOC/NOAA/NESDIS COASTWATCH",
                "NOAA CoastWatch");
            value = String2.replaceAllIgnoreCase(value,
                "NOAA Merged Land-Ocean Surface Temperature Analysis",
                "NOAA Merged Land-Ocean Surface Temperature");
            value = String2.replaceAllIgnoreCase(value,
                " Percentage of Years in Climatology",
                ", Climatology % Years");
            value = String2.replaceAllIgnoreCase(value,
                " prepared for GFDL Seasonal-Interannual Experimental Forecasts Coupled Data Assimilation Experiment",
                "");
            value = String2.replaceAllIgnoreCase(value,
                " Product Suite for the Greater Caribbean Region",
                ", Caribbean");
            value = String2.replaceAllIgnoreCase(value,
                " Quality Flag = Preliminary",
                ", Preliminary");
            value = String2.replaceAllIgnoreCase(value,
                "Regional Ocean Modeling System (ROMS)",
                "ROMS");
            value = String2.replaceAllIgnoreCase(value,
                "Regional Ocean Modeling System",
                "ROMS");
            value = String2.replaceAllIgnoreCase(value,
                "ROMS ESPRESSO Real-Time Operational IS4DVAR Forecast System Version 2 (NEW)",
                "ROMS ESPRESSO IS4DVAR Forecast, v2");
            value = String2.replaceAllIgnoreCase(value, 
                "sea surface temperature", "SST");           
            value = String2.whitespacesToSpace(value);
            value = String2.replaceAllIgnoreCase(value, " , ", ", ");
            value = String2.replaceAllIgnoreCase(value, " - ", ", ");
            /*
            value = String2.replaceAllIgnoreCase(value,
                "CLIVAR model output prepared for GFDL Seasonal-Interannual Experimental Forecasts ",
                "");
            */
            if (debugMode) String2.log(">> 6 title=" + value);
            
            //add oTitle to summary if it was shortened
            if (isSomething(oTitle)) {
                if (isSomething(tSummary)) {
                    if (!oTitle.equals(value) &&  //title shortened, so store original in summary
                        !String2.looselyContains(tSummary, oTitle))
                        tSummary = String2.periodSpaceConcat(oTitle, tSummary);
                } else {
                    tSummary = oTitle;
                }
                if (debugMode) String2.log("\n>>new tSummary=" + tSummary + "\n");
            }

            //after shortening...
            if (String2.looselyContains(suffixForTitle, sourceUrlAsTitle))
                sourceUrlAsTitle = "";
            if (String2.looselyContains(sourceUrlAsTitle, suffixForTitle))
                suffixForTitle = "";

            //add suffixForTitle?  (it was considered as a sole source of title above)
            //for fromThreddsCatalog, sourceTitle + suffixForTitle is good combo
            //String2.log(">> value.length=" + value.length() + " suffix.length=" + suffixForTitle.length());
            if (isSomething(suffixForTitle) && 
                !String2.looselyContains(value, suffixForTitle) &&
                value.length() + suffixForTitleP.length() < 77)
                value += suffixForTitleP;

            //append sourceUrlAsTitle
            if (isSomething(sourceUrlAsTitle) &&
                !String2.looselyContains(value, sourceUrlAsTitle) &&
                value.length() + sourceUrlAsTitle.length() + 3 < 77)
                value = isSomething(value)? value + " (" + sourceUrlAsTitle + ")" : //e.g., 1day
                    sourceUrlAsTitle;

            //last: clean up and save it in addAtts
            value = String2.replaceAll(value, "Avhrr AVHRR", "AVHRR"); //common at jpl
            value = String2.replaceAll(value, "GHRSST GHRSST", "GHRSST"); //common at jpl
            value = String2.replaceAll(value, '\n', ' ');
            value = String2.replaceAllIgnoreCase(value, "aggregate", "");
            value = String2.replaceAllIgnoreCase(value, "aggregation", "");
            value = String2.replaceAllIgnoreCase(value, "ghrsst", "GHRSST");
            value = String2.replaceAllIgnoreCase(value, "jpl", "JPL");
            value = String2.replaceAllIgnoreCase(value, "ncml", "");
            value = String2.whitespacesToSpace(value);
            if (value.startsWith("(")) {
                value = value.substring(1);
                po = value.indexOf(')');
                if (po >= 0)
                    value = value.substring(0, po) + value.substring(po + 1);
            }
        }
        if (tPublicSourceUrl.indexOf("TRMM") >= 0 &&
            isSomething(value) &&
            !String2.looselyContains(value, "trmm") &&
            !String2.looselyContains(value, "tropical rainfall")) 
            value = "Tropical Rainfall, " + value;    
        String tTitle = value;
        if (debugMode) String2.log(">> 7 title=" + value);

        //near end: after title may have been added to summary,
        //expand common acronyms in summary
        if (isSomething(tSummary)) {
            Table tTable = EDStatic.gdxAcronymsTable();
            StringArray acronymSA  = (StringArray)(tTable.getColumn(0));
            StringArray fullNameSA = (StringArray)(tTable.getColumn(1));
            int n = acronymSA.size();
            for (int i = 0; i < acronymSA.size(); i++) 
                tSummary = expandInSummary(tSummary, suggestedKeywords,
                    acronymSA.get(i), fullNameSA.get(i)); 
        } else {
            tSummary = ""; // not ??? (for periodSpaceConcat)
        }

        //special cases
        String s;
        if (tPublicSourceUrl.indexOf("/crm/") >=0 &&
            !String2.looselyContains(tSummary, "coastal relief model")) {
            chopUpAndAdd(s = "U.S. Coastal Relief Model. ", suggestedKeywords);
            tSummary = String2.periodSpaceConcat(s, tSummary);
        }
        if (tSummary.indexOf("CRUTEM") >= 0 &&  //e.g., CRUTEM3
            !String2.looselyContains(tSummary, "Climatic Research Unit")) {
            chopUpAndAdd(s = "Climatic Research Unit CRUTEM", suggestedKeywords);
            tSummary = String2.replaceAll(tSummary, "CRUTEM", s); 
        }
        if (tPublicSourceUrl.indexOf("/dem/") >= 0 &&
            tSummary.toLowerCase().indexOf("elevation") < 0) {
            chopUpAndAdd(s = "Digital Elevation Model.", suggestedKeywords);
            tSummary = String2.periodSpaceConcat(s, tSummary);
        }
        if ((tSummary.toLowerCase().indexOf("etopo") >= 0 ||
                    tPublicSourceUrl.indexOf("etopo") >=0) &&
                  tSummary.toLowerCase().indexOf("topography") < 0 &&
                  tSummary.toLowerCase().indexOf("bathymetry") < 0) {
            chopUpAndAdd(s = "ETOPO Global Topography and Bathymetry.", suggestedKeywords);
            tSummary = String2.periodSpaceConcat(s, tSummary);
        }


        //almost last thing
        //improve creator_email, creator_name, tInstitution, creator_url
        if (isSomething(creator_name)) {
            if (creator_name.equals("RSIGNELL")) 
                 creator_name = "Rich Signell";
            else if (creator_name.equals("Esrl Psd Data")) 
                creator_name = "NOAA ESRL PSD";
            else if (creator_name.equals("PODAAC")) 
                creator_name = "PODAAC NASA JPL";
            //shorten acronyms
            creator_name = String2.replaceAll(creator_name, 
                "National Geophysical Data Center (NGDC)", "NGDC");
        }

        if (!isSomething(creator_email) ||
            !isSomething(creator_name) ||
            !isSomething(creator_url) ||
            !isSomething(tInstitution)) {

            int po1 = Math.max(0, infoUrl.indexOf("/dodsC/"));
            int po2 = Math.max(0, tPublicSourceUrl.indexOf("/dodsC/"));
            String lc = "";
            int lastLevel = 5;
            for (int i = 0; i <= lastLevel; i++) {

                //make source material to search  (highest priority/best info first)
                if (i == 0) lc += "0 " + tTitle + " " + creator_name + " " + creator_url + " ";
                if (i == 1) lc += "1 " + tInstitution + " ";
                if (i == 2) lc += "2 " + infoUrl.substring(po1) + " " +  //2nd half
                                         creator_email + " ";  //email late because e.g., @noaa.gov
                if (i == 3) lc += "3 " + tPublicSourceUrl.substring(po2) + " "; //2nd half
                int midLevel = 3; //some things only look for at i>=midLevel
                if (i == 4) lc += "4 " + infoUrl.substring(0, po1) + " "; //1st half
                if (i == 5) lc += "5 " + tPublicSourceUrl.substring(0, po2) + " "; //1st half
                //not summary - too likely to refer to subordinate contributors or data sets
                //if adding i==6? change lastLevel above
                lc = lc.toLowerCase();
                if (debugMode) String2.log(">>i=" + i + " creator_name=" + creator_name + " email=" + creator_email + " url=" + creator_url + "\n  lc=" + lc);

                //?FUTURE? could make all this info into array of strings
                //   [][0=regex to match in lc, 1=email, 2=name, 3=url]
                //and (if separate method) pass it [creator_email, creator_name, tInstitution, creator_url]
                //  so any/all of the 4 strings could be changed.

                //more specific items at top (less specific items are further below)
                if (lc.indexOf(" erd ") >= 0 || 
                    lc.indexOf("pfeg") >= 0 || lc.indexOf("pfel") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "erd.data@noaa.gov";
                    if (!isSomething(creator_name))  creator_name  = "NOAA NMFS SWFSC ERD"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NMFS SWFSC ERD";
                    if (!isSomething(creator_url))   creator_url   = "http://www.pfeg.noaa.gov";
                    break;
                } 
                if (lc.indexOf("fnmoc") >= 0 || lc.indexOf("naval oceanographic office") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "CSO.navo.fct@navy.mil";                   
                    if (!isSomething(creator_name))  creator_name  = "Naval Research Lab (NRL) NAVOCEANO"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Naval Research Lab (NRL) NAVOCEANO";
                    if (!isSomething(creator_url))   creator_url   = "http://www.usno.navy.mil/NAVO";
                    break;
                } 
                if (lc.indexOf("bluelink") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "Tracey.pitman@csiro.au";                   
                    if (!isSomething(creator_name))  creator_name  = "Bluelink"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Bluelink";
                    if (!isSomething(creator_url))   creator_url   = "http://wp.csiro.au/bluelink/";
                    break;
                } 
                if (lc.indexOf("soda") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "chepurin@umd.edu";                   
                    if (!isSomething(creator_name))  creator_name  = "SODA, UMD"; 
                    if (!isSomething(tInstitution))  tInstitution  = "SODA, UMD";
                    if (!isSomething(creator_url))   creator_url   = "http://www.atmos.umd.edu/~ocean/";
                    break;
                } 
                if (lc.indexOf("trmm") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "Harold.F.Pierce@nasa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "TRMM"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NASA, JAXA";
                    if (!isSomething(creator_url))   creator_url   = "http://trmm.gsfc.nasa.gov/";
                    break;
                } 
                if (lc.indexOf("hycom") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "forum@hycom.org";                   
                    if (!isSomething(creator_name))  creator_name  = "HYCOM"; 
                    if (!isSomething(tInstitution))  tInstitution  = "HYCOM";
                    if (!isSomething(creator_url))   creator_url   = "https://hycom.org/";
                    break;
                } 
                if (lc.indexOf("rtofs") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "avichal.mehra@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NCEP"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NCEP";
                    if (!isSomething(creator_url))   creator_url   = "http://polar.ncep.noaa.gov/global/";
                    break;
                }
                if (lc.indexOf("obpg") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "webadmin@oceancolor.gsfc.nasa.gov"; 
                    if (!isSomething(creator_name))  creator_name  = "NASA/GSFC OBPG";
                    if (!isSomething(tInstitution))  tInstitution  = "NASA/GSFC OBPG";
                    if (!isSomething(creator_url))   creator_url   = "http://oceancolor.gsfc.nasa.gov/cms/"; 
                    break;
                }
                if (lc.indexOf("hadcrut") >= 0) { //before crutem
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Hadley Centre (UK Met Office), Climatic Research Unit (University of East Anglia)"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Hadley Centre (UK Met Office), Climatic Research Unit (University of East Anglia)";                                   
                    if (!isSomething(creator_url))   creator_url   = "http://www.cru.uea.ac.uk/cru/data/temperature/";                   
                    break;
                }
                if (lc.indexOf("crutem") >= 0) { //after hadcrut
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Climatic Research Unit (University of East Anglia)"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Climatic Research Unit (University of East Anglia)";                                   
                    if (!isSomething(creator_url))   creator_url   = "http://www.cru.uea.ac.uk/cru/data/temperature/";                   
                    break;
                }
                if (lc.indexOf("@dmi.dk") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Center for Ocean and Ice, Danish Meteorological Institute"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Center for Ocean and Ice, Danish Meteorological Institute";
                    if (!isSomething(creator_url))   creator_url   = "http://ocean.dmi.dk/";
                    break;
                } 
                if (lc.indexOf("cmc") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Canadian Meteorological Centre"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Canadian Meteorological Centre";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ec.gc.ca/scitech/default.asp?lang=En&n=61B33C26-1#cmc";
                    break;
                }
                if (lc.indexOf("meteo.fr") >= 0 || lc.indexOf("meteofrance") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Meteo France"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Meteo France";
                    if (!isSomething(creator_url))   creator_url   = "http://www.meteofrance.com";
                    break;
                }
                if (lc.indexOf("ifremer") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Ifremer"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Ifremer";
                    if (!isSomething(creator_url))   creator_url   = "http://wwz.ifremer.fr/";
                    break;
                }
                if (lc.indexOf("meteosat") >= 0 || lc.indexOf("msg") >= 0) { //meteosat second generation
                    if (!isSomething(creator_email)) creator_email = "ops@eumetsat.int";                   
                    if (!isSomething(creator_name))  creator_name  = "EUMETSAT"; 
                    if (!isSomething(tInstitution))  tInstitution  = "EUMETSAT";
                    if (!isSomething(creator_url))   creator_url   = "http://www.eumetsat.int/website/home/Satellites/CurrentSatellites/Meteosat/index.html";
                    break;
                }
                if (lc.indexOf("esrl") >= 0 && lc.indexOf("psd") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "esrl.psd.data@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA ESRL PSD"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA ESRL PSD";
                    if (!isSomething(creator_url))   creator_url   = "http://www.esrl.noaa.gov/psd/";
                    break;
                }
                if (lc.indexOf("aoml") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "aoml.webmaster@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA AOMLESRL"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA AOML";
                    if (!isSomething(creator_url))   creator_url   = "http://www.aoml.noaa.gov/";
                    break;
                }
                if (lc.indexOf("glerl") >= 0 || //before coastwatch below
                    lc.indexOf("great lakes environmental research laboratory") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "cw.glerl@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA CoastWatch Great Lakes Node"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA CoastWatch Great Lakes Node";
                    if (!isSomething(creator_url))   creator_url   = "http://coastwatch.glerl.noaa.gov/";
                    break;
                }
                if (lc.indexOf("noaa") >= 0 && lc.indexOf("coastwatch") >= 0) {
                    if (lc.indexOf("noaa coastwatch wcn") >= 0) {
                        if (!isSomething(creator_email)) creator_email = "erd.data@noaa.gov";
                        if (!isSomething(creator_name))  creator_name  = "NOAA NMFS SWFSC ERD"; 
                        if (!isSomething(tInstitution))  tInstitution  = "NOAA CoastWatch WCN, NOAA NMFS SWFSC ERD";
                        if (!isSomething(creator_url))   creator_url   = "http://www.pfeg.noaa.gov";
                        break;
                    } else if (lc.indexOf("pfeg") >= 0 || lc.indexOf("pfel") >= 0) {
                        if (!isSomething(creator_email)) creator_email = "erd.data@noaa.gov";
                        if (!isSomething(creator_name))  creator_name  = "NOAA NMFS SWFSC ERD"; 
                        if (!isSomething(tInstitution))  tInstitution  = "NOAA NMFS SWFSC ERD";
                        if (!isSomething(creator_url))   creator_url   = "http://www.pfeg.noaa.gov";
                        break;
                    } else { //coastwatch
                        if (!isSomething(creator_email)) creator_email = "coastwatch.info@noaa.gov";                   
                        if (!isSomething(creator_name))  creator_name  = "NOAA CoastWatch"; 
                        if (!isSomething(tInstitution))  tInstitution  = "NOAA CoastWatch";
                        if (!isSomething(creator_url))   creator_url   = "http://coastwatch.noaa.gov/";
                        break;
                    }
                }
                if (lc.indexOf("esrl") >= 0) {
                    //catch psd that would be caught in later iteration 
                    if (tPublicSourceUrl.indexOf("/psd/") >= 0) { 
                        if (!isSomething(creator_email)) creator_email = "esrl.psd.data@noaa.gov";                   
                        if (!isSomething(creator_name))  creator_name  = "NOAA ESRL PSD"; 
                        if (!isSomething(tInstitution))  tInstitution  = "NOAA ESRL PSD";
                        if (!isSomething(creator_url))   creator_url   = "http://www.esrl.noaa.gov/psd/";
                        break;
                    } else {
                        if (!isSomething(creator_email)) creator_email = "webmaster.esrl@noaa.gov";                   
                        if (!isSomething(creator_name))  creator_name  = "NOAA ESRL"; 
                        if (!isSomething(tInstitution))  tInstitution  = "NOAA ESRL";
                        if (!isSomething(creator_url))   creator_url   = "http://www.esrl.noaa.gov/";
                        break;
                    }
                }
                if (lc.indexOf("gfdl") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "GFDL.Climate.Model.Info@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA GFDL"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA GFDL";
                    if (!isSomething(creator_url))   creator_url   = "http://www.gfdl.noaa.gov/";
                    break;
                }
                if (lc.indexOf(".oco.noaa") >= 0) { //oco are common letters, so be more specific
                    if (!isSomething(creator_email)) creator_email = "climate.observation@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA OCO"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA OCO";
                    if (!isSomething(creator_url))   creator_url   = "http://www.oco.noaa.gov/";
                    break;
                }
                if (lc.indexOf("ncddc") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "ncddcwebmaster@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NCDDC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NCDDC";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ncddc.noaa.gov/";
                    break;
                }
                if (lc.indexOf("ncep") >= 0 ||
                    lc.indexOf("national centers for environmental prediction") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NCEP"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NCEP";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ncep.noaa.gov/";
                    break;
                }
                if (lc.indexOf("noaa") >= 0 &&
                    (lc.indexOf("osdpd") >= 0 || lc.indexOf("ospo") >= 0)) {
                    if (!isSomething(creator_email)) creator_email = "SSDWebmaster@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA OSPO"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA OSPO";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ospo.noaa.gov/";
                    break;
                }
                if (lc.indexOf("noaa") >= 0 && lc.indexOf("pmel") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "pmel.info@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA PMEL"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA PMEL";
                    if (!isSomething(creator_url))   creator_url   = "http://www.pmel.noaa.gov/";
                    break;
                }
                if (lc.indexOf("ndbc") >= 0 ||
                    lc.indexOf("national data buoy center") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "webmaster.ndbc@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NDBC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NDBC";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ndbc.noaa.gov/";
                    break;
                }
                if (lc.indexOf("cmar") >= 0 && lc.indexOf("csiro") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "Enquiries@csiro.au";                   
                    if (!isSomething(creator_name))  creator_name  = "CSIRO CMAR"; 
                    if (!isSomething(tInstitution))  tInstitution  = "CSIRO CMAR";
                    if (!isSomething(creator_url))   creator_url   = "http://www.cmar.csiro.au/";
                    break;
                }
                if (lc.indexOf("glos") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA GLOS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA GLOS";
                    if (!isSomething(creator_url))   creator_url   = "http://glos.us/";
                    break;
                }
                if (lc.indexOf("pacioos") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "jimp@hawaii.edu";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA PacIOOS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA PacIOOS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.pacioos.org";
                    break;
                }
                if (lc.indexOf("neracoos") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "ebridger@gmri.org";                   
                    if (!isSomething(creator_name))  creator_name  = "NERACOOS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NERACOOS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.neracoos.org/";
                    break;
                }
                if (lc.indexOf("nanoos") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NANOOS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NANOOS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.nanoos.org/";
                    break;
                }
                if (lc.indexOf("secoora") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "vembu@secoora.org";                   
                    if (!isSomething(creator_name))  creator_name  = "SECOORA"; 
                    if (!isSomething(tInstitution))  tInstitution  = "SECOORA";
                    if (!isSomething(creator_url))   creator_url   = "http://secoora.org/";
                    break;
                }
                if (lc.indexOf("caricoos") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "caricoos@gmail.com";                   
                    if (!isSomething(creator_name))  creator_name  = "CariCOOS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "CariCOOS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.caricoos.org/";
                    break;
                }


                //medium specific  
                if (lc.indexOf("ioos") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "data.ioos@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "IOOS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "IOOS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ioos.noaa.gov/";
                    break;
                }
                if (lc.indexOf("nsidc") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "nsidc@nsidc.org";                   
                    if (!isSomething(creator_name))  creator_name  = "NSIDC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NSIDC";
                    if (!isSomething(creator_url))   creator_url   = "http://nsidc.org/";
                    break;
                }
                if (lc.indexOf("hadley") >= 0) { 
                    if (!isSomething(creator_email)) creator_email = "john.kennedy@metoffice.gov.uk";                   
                    if (!isSomething(creator_name))  creator_name  = "Met Office Hadley Centre"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Met Office Hadley Centre";                                   
                    if (!isSomething(creator_url))   creator_url   = "http://hadobs.metoffice.com/";                   
                    break;
                }
                if (lc.indexOf("podaac") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "podaac@podaac.jpl.nasa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NASA JPL PODAAC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NASA JPL PODAAC";
                    if (!isSomething(creator_url))   creator_url   = "https://podaac.jpl.nasa.gov/";
                    break;
                }
                if (lc.indexOf("scripps") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Scripps"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Scripps";
                    if (!isSomething(creator_url))   creator_url   = "https://scripps.ucsd.edu/";
                    break;
                }
                if (lc.indexOf("bco-dmo") >= 0 || lc.indexOf("bcodmo") >= 0) { //before whoi
                    if (!isSomething(creator_email)) creator_email = "info@bco-dmo.org";                   
                    if (!isSomething(creator_name))  creator_name  = "BCO-DMO"; 
                    if (!isSomething(tInstitution))  tInstitution  = "BCO-DMO";
                    if (!isSomething(creator_url))   creator_url   = "http://www.bco-dmo.org/";
                    break;
                }
                if (lc.indexOf(".udel") >= 0 || lc.indexOf("univ. delaware") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "University of Delaware"; 
                                                     tInstitution  = "University of Delaware";
                    if (!isSomething(creator_url))   creator_url   = "http://www.udel.edu/";
                    break;
                }
                if (lc.indexOf("duke") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Duke University"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Duke University";
                    if (!isSomething(creator_url))   creator_url   = "https://nicholas.duke.edu/";
                    break;
                }
                if (lc.indexOf("ncdc") >= 0 ||
                    lc.indexOf("national climatic data center") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "ncdc.webmaster@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NCDC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NCDC";
                    if (!isSomething(creator_url))   creator_url   = "http://www.ncdc.noaa.gov/";
                    break;
                }
                if (lc.indexOf("nodc") >= 0 ||
                    lc.indexOf("national oceanographic data center") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "NODC.Webmaster@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NODC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NODC";
                    if (!isSomething(creator_url))   creator_url   = "http://www.nodc.noaa.gov/";
                    break;
                }
                if (lc.indexOf("ngdc") >= 0 ||
                    lc.indexOf("national geophysical data center") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "ngdc.info@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NGDC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NGDC";
                    if (!isSomething(creator_url))   creator_url   = "https://www.ngdc.noaa.gov/";
                    break;
                }
                if (lc.indexOf("nws") >= 0 ||
                    lc.indexOf("national weather service") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NWS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NWS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.weather.gov/";
                    break;
                }
                if (lc.indexOf("jpl") >= 0) { //after podaac, before nasa
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NASA JPL"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NASA JPL";
                    if (!isSomething(creator_url))   creator_url   = "http://www.jpl.nasa.gov/";
                    break;
                }
                if (lc.indexOf("ncar") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NCAR"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NCAR";
                    if (!isSomething(creator_url))   creator_url   = "http://ncar.ucar.edu/";
                    break;
                }
                if (lc.indexOf("gsfc") >= 0) { //before nasa
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NASA GSFC"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NASA GSFC";
                    if (!isSomething(creator_url))   creator_url   = "http://www.nasa.gov/centers/goddard/home/index.html";
                    break;
                }
                if (lc.indexOf("rsmas") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "University of Miami, RSMAS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "University of Miami, RSMAS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.rsmas.miami.edu/";
                    break;
                }
                if (lc.indexOf(".dal.ca") >= 0 || lc.indexOf("dalhousie") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Dalhousie University"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Dalhousie University";
                    if (!isSomething(creator_url))   creator_url   = "http://www.dal.ca/";
                    break;
                }
                if (lc.indexOf("whoi") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "WHOI"; 
                    if (!isSomething(tInstitution))  tInstitution  = "WHOI";
                    if (!isSomething(creator_url))   creator_url   = "http://www.whoi.edu/";
                    break;
                }

                //less specific
                if (lc.indexOf("csiro") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "Enquiries@csiro.au";                   
                    if (!isSomething(creator_name))  creator_name  = "CSIRO"; 
                    if (!isSomething(tInstitution))  tInstitution  = "CSIRO";
                    if (!isSomething(creator_url))   creator_url   = "http://www.csiro.au/";
                    break;
                }
                if (lc.indexOf(".abom") >= 0 || lc.indexOf("@abom") >= 0 || 
                    lc.indexOf("/abom") >= 0 || lc.indexOf(" abom") >= 0 || 
                    lc.indexOf(".bom.gov.au") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Australian Bureau of Meteorology"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Australian Bureau of Meteorology";
                    if (!isSomething(creator_url))   creator_url   = "http://www.bom.gov.au/";
                    break;
                }
                if (lc.indexOf("nasa") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NASA"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NASA";
                    if (!isSomething(creator_url))   creator_url   = "http://www.nasa.gov/";
                    break;
                }
                if (lc.indexOf("nesdis") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "NESDIS.Data.Access@noaa.gov";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA NESDIS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA NESDIS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.nesdis.noaa.gov/";
                    break;
                }
                if (lc.indexOf("usgs") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "USGS"; 
                    if (!isSomething(tInstitution))  tInstitution  = "USGS";
                    if (!isSomething(creator_url))   creator_url   = "http://www.usgs.gov/";
                    break;
                }
                if (lc.indexOf("rutgers") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "Institute of Marine and Coastal Science, Rutgers"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Institute of Marine and Coastal Science, Rutgers";
                    if (!isSomething(creator_url))   creator_url   = "http://marine.rutgers.edu/main/";
                    break;
                }
                if (lc.indexOf("eumetsat") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "ops@eumetsat.int";                   
                    if (!isSomething(creator_name))  creator_name  = "EUMETSAT"; 
                    if (!isSomething(tInstitution))  tInstitution  = "EUMETSAT";
                    if (!isSomething(creator_url))   creator_url   = "http://www.eumetsat.int/website/home/index.html";
                    break;
                }
                if (lc.indexOf("metoffice") >= 0) { 
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "UK Met Office"; 
                    if (!isSomething(tInstitution))  tInstitution  = "UK Met Office";                                   
                    if (!isSomething(creator_url))   creator_url   = "http://www.metoffice.gov.uk/research";                   
                    break;
                }
                if (lc.indexOf("jma") >= 0) {
                    if (!isSomething(creator_email)) creator_email = "metsat@kishou.go.jp";                   
                    if (!isSomething(creator_name))  creator_name  = "Japan Meteorological Agency"; 
                    if (!isSomething(tInstitution))  tInstitution  = "Japan Meteorological Agency";
                    if (!isSomething(creator_url))   creator_url   = "http://www.jma.go.jp/en/gms/";
                    break;
                }
                if (lc.indexOf("jaxa") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "JAXA"; 
                    if (!isSomething(tInstitution))  tInstitution  = "JAXA";
                    if (!isSomething(creator_url))   creator_url   = "http://global.jaxa.jp/";
                    break;
                }

                //less specific, and only check at last level:
                if (i < midLevel) 
                    continue;
                if (lc.indexOf("noaa") >= 0) {
                    //if (!isSomething(creator_email)) creator_email = "";                   
                    if (!isSomething(creator_name))  creator_name  = "NOAA"; 
                    if (!isSomething(tInstitution))  tInstitution  = "NOAA";
                    if (!isSomething(creator_url))   creator_url   = "http://www.noaa.gov/";
                    break;
                }
            }
        }

        //keywords (after title)
        name = "keywords";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (!isSomething(value)) {
            if (!isSomething(value)) {
                                     value =    addAtts.getString("keyword");  //singular?
            if (!isSomething(value)) value = sourceAtts.getString("keyword");
            if ( isSomething(value))                  addAtts.set("keyword", "null"); //not just remove()
            }
            if (!isSomething(value)) {
                                     value =    addAtts.getString("Keywords");  //wrong case?
            if (!isSomething(value)) value = sourceAtts.getString("Keywords");
            if ( isSomething(value))                  addAtts.set("Keywords", "null");  //not just remove()
            }

            if (isSomething(value) && value.indexOf(',') < 0 && value.indexOf('>') < 0) 
                //MEASURES has space-separated "keyword"s
                value = String2.toCSSVString(String2.split(value, ' '));
        }

        //add suggestedKeywords and words from title to keywords  (and improve current keywords)
        {

            //build hashset of current keywords
            String words[] = StringArray.arrayFromCSV(value == null? "" : value);  //csv, not chop up
            for (int w = 0; w < words.length; w++) {
                if (words[w].length() > 0) {
                    if (words[w].indexOf(" > ") >= 0) { 
                        if (words[w].toLowerCase().startsWith("earth science > "))
                            words[w] = words[w].substring(16);
                        suggestedKeywords.add(words[w]);

                        //add individual words from original gcmd keywords
                        chopUpAndAdd(String2.replaceAll(words[w], '/', ' '), suggestedKeywords);
                    } else {
                        //individual words: toLowerCase avoids distinguishing based on case
                        suggestedKeywords.add(words[w].toLowerCase());
                    }
                }
            }

            //add words from institution;     definitely split at '/', e.g., NOAA/NODC
            //odd to make lowerCase but good to sort in with single words, not GCMD keywords
            chopUpAndAdd(String2.replaceAll(tInstitution, '/', ' '), suggestedKeywords);  

            //removed... interesting idea, but too aggressive. keywords should be for dataset, not distribution
            //catch server type from publicSourceUrl
            //las not distinctive; hyrax is "/opendap/"?
            //String tps = tPublicSourceUrl.toLowerCase();
            //if (tps.indexOf("/thredds/") >= 0)  suggestedKeywords.add("thredds");
            //suggestedKeywords.add("erddap");  //all datasets processed here available via erddap

            //add words and popular phrases from new title and original_title
            //'/' difficult; most likely word/word (not e.g., mg/ml) so split it
            String tt = sourceAtts.getString("title"); 
            tt = (tTitle + (isSomething(tt) && !tt.equals(tTitle)? " " + tt : "")).toLowerCase();
            chopUpAndAdd(String2.replaceAll(tt, '/', ' '), suggestedKeywords);             

            //add some phrases from title and alternate forms
            if (tt.indexOf(              "best time series") >= 0)
                suggestedKeywords.add(   "best time series");
            if (tt.indexOf(              "east coast") >= 0) 
                suggestedKeywords.add(   "east coast");
            if (tt.indexOf(              "great lakes") >= 0) 
                suggestedKeywords.add(   "great lakes");
            if (tt.indexOf(              "gulf of mexico") >= 0) 
                suggestedKeywords.add(   "gulf of mexico");
            if (tt.indexOf(              "hf radar") >= 0 ||
                tt.indexOf(              "hfradar")  >= 0) 
                suggestedKeywords.add(   "hf radar");
            if (tt.indexOf(              "hf radio") >= 0 ||
                tt.indexOf(              "hfradio") >= 0) 
                suggestedKeywords.add(   "hf radio");
            if (tt.indexOf(              "navy coastal ocean model") >= 0) 
                suggestedKeywords.add(   "ncom");  //expanded further below
            int npo = tt.indexOf("near");  //may be "near-real-time" or "near real time"
            if (npo >= 0 &&
                tt.indexOf("real", npo + 4) == npo + 5 &&
                tt.indexOf("time", npo + 4) == npo + 10) 
                suggestedKeywords.add(   "near real time");
            if (tt.indexOf(              "new york") >= 0) 
                suggestedKeywords.add(   "new york");
            if (tt.indexOf(              "north america") >= 0) 
                suggestedKeywords.add(   "north america");
            if (!suggestedKeywords.contains("near real time") &&
                (tt.indexOf("real-time") >= 0 ||         //real-time converted to "real time" below
                 tt.indexOf("real time") >= 0)) 
                suggestedKeywords.add(   "real time");  
            if (tt.indexOf(              "regional ocean model") >= 0)
                suggestedKeywords.add(   "regional ocean model");
            if (tt.indexOf(              "science quality") >= 0)
                suggestedKeywords.add(   "science quality");
            if (tt.indexOf(              "south america") >= 0) 
                suggestedKeywords.add(   "south america");
            if (tt.indexOf(              "time series") >= 0)
                suggestedKeywords.add(   "time series");
            if (tt.indexOf(              "west coast") >= 0) 
                suggestedKeywords.add(   "west coast");

            cleanSuggestedKeywords(suggestedKeywords);
            
            //build new keywords String
            StringBuilder sb = new StringBuilder("");
            String keywordSar[] = (String[])suggestedKeywords.toArray(new String[0]); 
            //they are consistently capitalized, so will sort very nicely:
            //  single words then gcmd
            Arrays.sort(keywordSar, new StringComparatorIgnoreCase()); 
            for (int w = 0; w < keywordSar.length; w++) {

                //don't save numbers
                String kw = keywordSar[w];
                boolean aNumber = true;
                for (int kwpo = 0; kwpo < kw.length(); kwpo++) {
                    char ch = kw.charAt(kwpo);
                    if (!String2.isDigit(ch) && ch != '.') {
                         aNumber = false;
                         break;
                    }
                }
                if (aNumber)
                    continue;

                if (kw.length() > 2) { //lose lots of junk and a few greek letters

                    boolean isGcmd = kw.indexOf(" > ") >= 0;
                    if (isGcmd) {
                        if (sb.length() == 0) 
                            sb.append('\n');
                        else if (sb.charAt(sb.length() - 1) == ' ') 
                            sb.setCharAt(sb.length() - 1, '\n');
                    }
                    sb.append(kw);
                    sb.append(isGcmd? ",\n" : ", "); //both are 2 char
                } else if (String2.indexOf(KEEP_SHORT_KEYWORDS, kw) >= 0) {
                    sb.append(kw);
                    sb.append(", ");
                } else if (String2.indexOf(KEEP_SHORT_UC_KEYWORDS, kw) >= 0) {
                    sb.append(kw.toUpperCase());
                    sb.append(", ");
                }
                    
            }
            //remove last 2 char separator (", " or ",\n")
            if (sb.length() >= 2)
                sb.setLength(sb.length() - 2);

            value = sb.toString();
            addAtts.add(name, value);
            if (reallyVerbose) String2.log("  new " + name + "=" + value);
        }
        boolean keywordsPartlyGcmd = value.indexOf(" > ") >= 0;

        //keywords_vocabulary
        name = "keywords_vocabulary";
        value = addAtts.getString(name);
        if (!isSomething(value)) value = sourceAtts.getString(name);
        if (keywordsPartlyGcmd) {
            String gcmdSK = "GCMD Science Keywords";
            //Some datasets use a slightly different name.  This standardizes the name.
            if ((isSomething(value) && !value.equals(gcmdSK)) ||
                !isSomething(value)) {
                value = gcmdSK;
                addAtts.add(name, value);
                if (reallyVerbose) String2.log("  new " + name + "=" + value);
            }
        }

        //finally set creator_..., infoUrl, institution, summary, title
        if (isSomething(creator_name))
            addAtts.set("creator_name", creator_name);   //removeIfSame below
        if (isSomething(creator_email))
            addAtts.set("creator_email", creator_email); //removeIfSame below
        if (isSomething(creator_url)) {
            addAtts.set("creator_url", creator_url);     //removeIfSame below
            //set infoUrl from creator_url?
            if (!isSomething(infoUrl))
                infoUrl = creator_url;
        } else {
            //set creator_url from infoUrl?
            if (isSomething(infoUrl))
                addAtts.set("creator_url", infoUrl);
        }
        //required atts: use ??? if not known (hopefully never)
        tInstitution = String2.whitespacesToSpace(tInstitution);
        //not summary
        tTitle       = String2.whitespacesToSpace(tTitle);
        tInstitution = String2.replaceAll(tInstitution, " , ", ", ");
        tSummary     = String2.replaceAll(tSummary,     " , ", ", ");
        tTitle       = String2.replaceAll(tTitle,       " , ", ", ");
        addAtts.set("infoUrl", 
            isSomething(infoUrl)? infoUrl : "???"); //hopefully never        
        addAtts.set("institution", 
            isSomething(tInstitution)? tInstitution : "???"); //hopefully never
        addAtts.set("summary", 
            isSomething(tSummary)? tSummary : "???"); //hopefully never
        addAtts.set("title", 
            isSomething(tTitle)? tTitle : "???"); //hopefully never
        if (debugMode) String2.log(">> final tInstitution=" + tInstitution + 
            "\n>> final tSummary=" + tSummary); 

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
        String exts[] = {".7z",
            ".bz",  ".bz2", ".cdf",  ".cdp",  ".dods", "_dods", //_dods exists
            ".grb", ".grd", ".grib", ".gtar", ".gz",   ".gzip",
            ".jar", ".jnl", ".lha",  ".lzh",  ".lzma", ".lzx", 
            ".mat", ".nc",  ".ncd",  ".ncml", ".new",
            ".py",  ".pyc", ".rar",  ".tar",  ".war",  
            ".xml", ".zip", ".z",    ".Z"};
        while (true) {
            int which = String2.whichSuffix(exts, tTitle, 0);
            if (which < 0) {
                //String2.log(">> removeExtensionsFromTitle -> " + tTitle);
                return tTitle;
            }
            tTitle = tTitle.substring(0, tTitle.length() - exts[which].length());
        }
    }


    /** This returns true if s isn't null, "", "-", "N/A", "...", "???", etc. */
    public static boolean isSomething(String s) {
        //Some datasets have "" for an attribute.

        //Some datasets have comment="..." ,e.g.,
        //http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/region1/ncom_glb_reg1_2010013000.nc.das
        //which then prevents title from being generated

        //some have "-", e.g.,
        //http://dm1.caricoos.org/thredds/dodsC/content/wrf_archive/wrfout_d01_2009-09-25_12_00_00.nc.das

        return !(s == null || s.trim().equals("") || s.equals("-") || s.equals("...") || 
            s.equals("?") || s.equals("???") || s.equals("N/A") || s.equals("NA"));
    }



    /**
     * NOW, makeReadyToUseAddVariableAttributesForDatasetsXml() IS RECOMMENDED OVER THIS.
     * This is used by subclass's generateDatasetsXml methods to make
     * sure that a variable's attributes includes at least place holders (dummy values)
     * for the required/common attributes.
     *
     * @param sourceAtts
     * @param tSourceName
     * @throws Exception if trouble
     */
    public static void addDummyRequiredVariableAttributesForDatasetsXml(
        Attributes sourceGlobalAtts, Attributes sourceAtts, String tSourceName, 
        boolean addColorBarMinMax) throws Exception {

        //get the readyToUseAddVariableAttributes for suggestions
        Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
            sourceGlobalAtts, sourceAtts, tSourceName, addColorBarMinMax, false);

        if (addColorBarMinMax) {
            if (sourceAtts.getString("colorBarMinimum") == null) 
                      sourceAtts.add("colorBarMinimum", 
                   addAtts.getDouble("colorBarMinimum"));
            if (sourceAtts.getString("colorBarMaximum") == null) 
                      sourceAtts.add("colorBarMaximum", 
                   addAtts.getDouble("colorBarMaximum"));
        }

        String names[] = {"ioos_category", "long_name", "standard_name", "units"};
        for (int i = 0; i < 4; i++) {
            String sValue = sourceAtts.getString(names[i]);
            String aValue = addAtts.getString(names[i]);
            if (sValue == null) sValue = "";
            if (aValue == null) aValue = "";
            //String2.log("  name=" + names[i] + " sValue=" + sValue + " aValue=" + aValue);
            //special case: only suggest ioos_category if required
            if (i == 0 && !EDStatic.variablesMustHaveIoosCategory)
                continue;
            //special case: see if a different long_name is suggested (give admin a choice)
            //because makeReadyToUse won't change an existing long_name
            if (i == 1 && aValue.length() == 0) {
                String tStandardName = addAtts.getString("standard_name");
                if (tStandardName == null)
                    tStandardName = sourceAtts.getString("standard_name");
                aValue = EDV.suggestLongName(sValue, tSourceName, tStandardName);
            }
            if (!isSomething(sValue) || 
                (isSomething(aValue) && !aValue.equals(sValue))) 
                sourceAtts.add(names[i], sValue + "???" + (aValue.equals("???")? "" : aValue));
        }

    }

    /**
     * This is used by generateDatasetsXml methods to add as many 
     * variable attributes as possible based on source information.
     *
     * @param sourceGlobalAtts the source's global attributes (may be null)
     * @param sourceAtts the source's variable attributes
     * @param tSourceName   
     * @param tryToAddColorBarMinMax
     * @param tryToFindLLAT   This tries to identify longitude, latitude, altitude/depth, 
     *    and time variables.  It is usually true for grid dataset axis variables, and 
     *    true for table dataset all variables
     * @return tAddAdds for the variable
     * @throws Exception if trouble
     */
    public static Attributes makeReadyToUseAddVariableAttributesForDatasetsXml(
        Attributes sourceGlobalAtts, Attributes sourceAtts, String tSourceName, 
        boolean tryToAddColorBarMinMax, boolean tryToFindLLAT) throws Exception {

        String value;

        //if from readXml, be brave and just use last part of the source name
        //  .../.../aws:time
        int slashPo = tSourceName.lastIndexOf('/');
        if (slashPo >= 0 && slashPo < tSourceName.length() - 1) { //not the last char
            int slashPo1 = tSourceName.indexOf('/');
            if (slashPo1 != slashPo) {
                tSourceName = tSourceName.substring(slashPo + 1);
                
                //and it probably has unnecessary prefix:
                int colonPo = tSourceName.lastIndexOf(':');
                if (colonPo >= 0 && colonPo < tSourceName.length() - 1) { //not the last char
                    tSourceName = tSourceName.substring(colonPo + 1);
                }
            }
        }

        String lcSourceName = tSourceName.toLowerCase();
        if (sourceGlobalAtts == null)
            sourceGlobalAtts = new Attributes();

        //this is what this method creates and populates
        Attributes addAtts = new Attributes();

        String sourceNames[] = sourceAtts.getNames();

        //change/remove grads_ attributes
        value = sourceAtts.getString("grads_dim"); //grads_dim removed below
        if (isSomething(value) && "xyzt".indexOf(value.charAt(0)) >= 0)
            addAtts.add("axis", "" + Character.toUpperCase(value.charAt(0)));  //e.g., axis = X

        String inaxA[] = {"in", "ax"};
        for (int i = 0; i < 2; i++) {
            String inax = inaxA[i];
            PrimitiveArray tpa = sourceAtts.get("m" + inax + "imum"); //removed below
            if (tpa == null)
                tpa = sourceAtts.get("Data%20M" + inax + "imum"); //removed below
            if (tpa == null)
                tpa = sourceAtts.get("Data_M" + inax + "imum"); //removed below
            if (tpa != null)
                addAtts.add("data_m" + inax, tpa);
        }

        //remove some attributes if they have specific values
        if ("no".equals(sourceAtts.getString("modulo"))) 
            addAtts.add("modulo", "null");
       
        //convert all fgdc_X and fgdc:X metadata to X (if not already set)
        // and fix any bad characters in sourceAtt names.
        //e.g. http://www.ngdc.noaa.gov/thredds/dodsC/ustec/tec/200609030400_tec.nc.das uses '_'
        //http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#idp4775248
        //  says "Variable, dimension and attribute names should begin with a letter
        //  and be composed of letters, digits, and underscores."
        //Technically, starting with _ is not allowed, but it is widely done: 
        //  e.g., _CoordinateAxes, _CoordSysBuilder
        String removePrefixes[] = {"fgdc_", "fgdc:", "HDF5_", "HDF5."}; //e.g., HDF5_chunksize
        String toRemove[] = {  //lowercase here. Removal is case-insensitive.
            "bounds", "_chunksize", "chunksize", 
            "_coordinateaxes", "coordinates", "coordintates",//sic //coordinate info often wrong or with sourceNames
            "data_bins", "data_center", 
            "data_maximum", "data_minimum", //see above
            "dataset", "dataset_index",
            "easternmost_longitude", 
            "end", "end_day", "end_millisec", "end_orbit", "end_time", "end_year",
            "ferret_datatype", 
            "grads_dim",   //these were changed above
            "grads_mapping", "grads_min", "grads_size", "grads_step", 
            "gridtype", 
            "infile_datatype", 
            "input_files", "input_parameters", "institution", 
            "l2_flag_names", 
            "latitude_step", "latitude_units", "length", "longitude_step", "longitude_units",
            "maximum", "minimum",  //see above
            "mission_characteristics",
            "northernmost_latitude", "number_of_columns", "number_of_lines",
            "numberofobservations", 
            "orig_file_axname", "original_units", 
            "palette_info_variable",
            "percentcoverage", 
            "period_end_day", "period_end_year", "period_start_day", "period_start_year",
            "pointwidth",
            "processing_control", "processing_time", "processing_version",
            //"product_name", "product_type", 
            "resolution", 
            "scaling_equation",
            //"software_name", "software_version", 
            "southernmost_latitude",
            "start", "start_day", "start_millisec", "start_orbit", "start_time", "start_year",
            "station_latitude", "station_longitude", "station_name",
            "suggested_image_scaling_applied", 
            //"suggested_image_scaling_maximum", "suggested_image_scaling_minimum", convert to colorbarmax|min?
            "suggested_image_scaling_type",
            "sw_point_latitude", "sw_point_longitude", 
            "westernmost_longitude"};
        for (int i = 0; i < sourceNames.length; i++) {
            String sn = sourceNames[i];
            String pre = String2.findPrefix(removePrefixes, sn, 0);
            if (String2.indexOf(toRemove, sn.toLowerCase()) >= 0) {
                addAtts.set(sn, "null");    //remove toRemove att name
                if (debugMode)
                    String2.log(">>  useless var sourceAttName=\"" + sn + "\" removed.");
            } else if (sn.startsWith("dsp_")) {
                addAtts.set(sn, "null");   //remove full original prefixed att name
                if (debugMode)
                    String2.log(">> useless var sourceAttName=\"" + sn + "\" removed.");
            } else {
                String safeSN = pre == null? sn : sn.substring(pre.length()); //e.g., fgdc_X becomes X
                safeSN = String2.modifyToBeVariableNameSafe(safeSN);
                if (String2.indexOf(toRemove, safeSN) >= 0) {
                    addAtts.set(sn, "null");     //  neutralize bad att name
                    if (debugMode)
                        String2.log(">> useless var sourceAttName=\"" + sn + "\" removed.");
                } else if (!sn.equals(safeSN)) {        //if sn isn't safe
                    addAtts.set(sn, "null");     //  neutralize bad att name
                    if (reallyVerbose)
                        String2.log("  badSourceAttName=\"" + sn + "\" converted to \"" + safeSN + "\".");
                    addAtts.setIfNotAlreadySet(safeSN, sourceAtts.get(sn)); 
                }
            }
        }

        //oXxx are values from sourceAtts
        String oLongName     = addAtts.getString("long_name");
        String oStandardName = addAtts.getString("standard_name");
        String oUnits        = addAtts.getString("units");
        String oPositive     = addAtts.getString("positive");
        if (oLongName       == null) oLongName     = sourceAtts.getString("long_name");
        if (oStandardName   == null) oStandardName = sourceAtts.getString("standard_name");
        if (oUnits          == null) oUnits        = sourceAtts.getString("units");
        if (oPositive       == null) oPositive     = sourceAtts.getString("positive");
        if (oLongName       == null) oLongName     = "";
        if (oStandardName   == null) oStandardName = "";
        if (oUnits          == null) oUnits        = "";
        if (oPositive       == null) oPositive     = "";
        //tXxx are current best value
        String tLongName     = oLongName;
        String tStandardName = oStandardName;
        String tUnits        = oUnits;
        if (tUnits.length() == 0) {
            //rtofs grads
            //sea_water_practical_salinity units = "1" in CF std names 27; I'm sticking with PSU.
            String from[] = {"degc",     "psu",  "m/s",   "m", "Presumed Salinity Units"};
            String to[]   = {"degree_C", "PSU",  "m s-1", "m", "PSU"}; 
            for (int i = 0; i < from.length; i++) {
                if (oLongName.endsWith(" (" + from[i] + ")")) {  //e.g. " (degc)"
                    tUnits = to[i];
                    tLongName = oLongName.substring(0, oLongName.length() - from[i].length() - 3);
                }
            }
        }
        if (tSourceName.equals("l3m_data") && tLongName.equals("l3m_data")) {
            //special case for some podaac datasets
            String gParam = sourceGlobalAtts.getString("Parameter");
            String gUnits = sourceGlobalAtts.getString("Units");
            if (isSomething(gParam))
                tLongName = gParam;
            if (isSomething(gUnits) && !isSomething(tUnits))
                tUnits = gUnits;
        }
        if (tSourceName.equals("l3m_qual") && tLongName.equals("l3m_qual")) {
            //special case for some podaac datasets
            String gParam = sourceGlobalAtts.getString("Parameter");
            String gUnits = sourceGlobalAtts.getString("Units");
            if (isSomething(gParam))
                tLongName = gParam + " Quality";
            if (isSomething(gUnits) && !isSomething(tUnits))
                tUnits = gUnits;
        }
        if (tLongName.length() > 0 && tLongName.equals(tLongName.toLowerCase()) &&
            tLongName.indexOf(' ') == -1 && tLongName.indexOf('_') >0) {
            //convert possible standard_name to Title Case   (also rtofs grads)
            tLongName = String2.toTitleCase(String2.replaceAll(tLongName, '_', ' '));
        }
        if (String2.caseInsensitiveIndexOf(EDV.LON_UNITS_VARIANTS, tUnits) >= 0)
            tUnits = EDV.LON_UNITS;
        if (String2.caseInsensitiveIndexOf(EDV.LAT_UNITS_VARIANTS, tUnits) >= 0)
            tUnits = EDV.LAT_UNITS;
        String tUnitsLC      = tUnits.toLowerCase();

        //add_offset instead of add_off
        PrimitiveArray oa = sourceAtts.get("add_off");
        if (oa != null &&
            !(oa instanceof StringArray) &&
            sourceAtts.get("add_offset") == null) {
            //some podaac datasets have this
            addAtts.add("add_off", "null");
            addAtts.add("add_offset", oa);
        }

        //units instead of unit
        PrimitiveArray ua = sourceAtts.get("unit");
        if (ua != null) {
            if (sourceAtts.get("units") == null &&
                addAtts.get("units") == null) 
                //some podaac datasets have this
                addAtts.add("units", ua);
            addAtts.add("unit", "null");
        }

        //git rid of redundant original_name, some podaac datasets have this
        if (isSomething(oStandardName) && 
            oStandardName.equals(sourceAtts.getString("original_name")))
            addAtts.add("original_name", "null");
        
        double tScaleFactor = sourceAtts.getDouble("scale_factor");
        double tAddOffset   = sourceAtts.getDouble("add_offset");
        if (tScaleFactor == 1 && tAddOffset == 0) {
            //remove pointless attributes
            addAtts.add("add_offset", "null");
            addAtts.add("scale_factor", "null");
            if (sourceAtts.getDouble("add_offset_err") == 0)
                addAtts.add("add_offset_err", "null");
            if (sourceAtts.getDouble("scale_factor_err") == 0)
                addAtts.add("scale_factor_err", "null");

        } else { 
            //scale_factor or add_offset are strings?! convert to float
            //e.g., see fromThreddsCatalog test #56
            PrimitiveArray pa = sourceAtts.get("add_offset");
            if (pa != null && pa.size() > 0 && pa instanceof StringArray) 
                addAtts.add("add_offset", pa.getFloat(0));
            pa = sourceAtts.get("scale_factor");
            if (pa != null && pa.size() > 0 && pa instanceof StringArray) 
                addAtts.add("scale_factor", pa.getFloat(0));
        }

        //Intercept -> add_offset, Slope -> scale_factor ?
        String Scaling = sourceAtts.getString("Scaling"); //linear
        if ("linear".equals(Scaling)) {
            //some podaac datasets have this
            double tIntercept = sourceAtts.getDouble("Intercept");
            double tSlope     = sourceAtts.getDouble("Slope");
            PrimitiveArray pa = sourceAtts.get("Slope");
            if (tIntercept == 0 && tSlope == 1) {
            } else {
                if (Double.isNaN(tAddOffset) && !Double.isNaN(tIntercept)) {
                    tAddOffset = tIntercept;
                    addAtts.add("add_offset", sourceAtts.get("Intercept")); //same type
                }
                if (Double.isNaN(tScaleFactor) && !Double.isNaN(tSlope)) {
                    tScaleFactor = tSlope;
                    addAtts.add("scale_factor", sourceAtts.get("Slope")); //same type
                }
            }
            if (sourceAtts.get("Intercept")        != null) 
                   addAtts.add("Intercept",          "null");
            if (sourceAtts.get("Scaling")          != null) 
                   addAtts.add("Scaling",            "null");
            if (sourceAtts.get("Scaling_Equation") != null) 
                   addAtts.add("ScalingEquation",    "null");
            if (sourceAtts.get("Slope")            != null) 
                   addAtts.add("Slope",              "null");
        }

        //Fill instead of _FillValue
        PrimitiveArray Fill = sourceAtts.get("Fill");
        if (Fill != null && 
            !(Fill instanceof StringArray) &&
            sourceAtts.get("_FillValue") == null &&
            sourceAtts.get("missing_value") == null) {
            //some podaac datasets have this
            addAtts.add("Fill", "null");
            addAtts.add("_FillValue", Fill);
        }


        //isMeters
        String testUnits = tUnits.toLowerCase(); 
        testUnits = String2.replaceAll(testUnits, ' ', '|');
        testUnits = String2.replaceAll(testUnits, '_', '|');
        testUnits = String2.replaceAll(testUnits, '=', '|');
        testUnits = String2.replaceAll(testUnits, ',', '|');
        testUnits = String2.replaceAll(testUnits, '/', '|');
        boolean isMeters = String2.indexOf(EDV.METERS_VARIANTS, tUnits) >= 0; // case sensitive

        //convert feet to meters
        boolean isFeet = 
            testUnits.equals("foot")               ||
            testUnits.equals("feet")               ||
            testUnits.equals("ft")                 ||
            testUnits.equals("international_foot") ||
            testUnits.equals("international_feet");
        if (isFeet && 
            (Double.isNaN(tScaleFactor) || tScaleFactor == 1) &&
            (Double.isNaN(tAddOffset)   || tAddOffset == 0)) {
            tScaleFactor = 0.3048;
            addAtts.set("scale_factor", (float)tScaleFactor); //feet usually int, so convert to float
            tUnits = "m";
            tUnitsLC = "m";
            testUnits = "m";
            isMeters = true;
            isFeet = false;
        }

        //do LLAT vars already exist? 
        String tDestName = suggestDestinationName(tSourceName, tUnits, oPositive,
            Math2.doubleToFloatNaN(tScaleFactor), tryToFindLLAT);
        if (tDestName.equals(EDV.LON_NAME)) {
            tLongName = isSomething(tLongName) && 
               !tLongName.toLowerCase().equals(EDV.LON_NAME)? tLongName : "Longitude";
            tStandardName = EDV.LON_NAME;
            tUnits = EDV.LON_UNITS;
        } else if (tDestName.equals(EDV.LAT_NAME)) {
            tLongName = isSomething(tLongName) && 
               !tLongName.toLowerCase().equals(EDV.LAT_NAME)? tLongName : "Latitude";
            tStandardName = EDV.LAT_NAME;
            tUnits = EDV.LAT_UNITS;
        } else if (tDestName.equals(EDV.ALT_NAME)) {
            //let tLongName be set below
            tStandardName = EDV.ALT_NAME;
        } else if (tDestName.equals(EDV.DEPTH_NAME)) {
            //let tLongName be set below
            tStandardName = EDV.DEPTH_NAME;
        } else if (tDestName.equals(EDV.TIME_NAME)) {
            //let tLongName be set below
            tStandardName = EDV.TIME_NAME;
        }

        //common mistakes in UAF
        if (tUnitsLC.equals("yyyy.(fractional part of year)")) {
            addAtts.add("originalUnits", "YYYY.(fractional part of year)");
            tUnits = "1";
        } else if (tUnitsLC.equals("celsius/degree")) 
            tUnits = "degree_C";
        else if (tUnitsLC.equals("kelvins")) 
            tUnits = "deg_K";
        else if (tUnitsLC.equals("°k")) 
            tUnits = "deg_K";
        else if (tUnits.equals("u M") || tUnits.equals("uM")) 
            tUnits = "umoles L-1";
        else if (tUnits.equals("degree C")) 
            tUnits = "degree_C";

        int umpo = tUnits.indexOf("(uM)");
        if (umpo >= 0) 
            tUnits = tUnits.substring(0, umpo) + "(umoles L-1)" + tUnits.substring(umpo + 4);
        if (tUnitsLC.startsWith("degrees celsius")) 
            tUnits = "degree_C" + tUnits.substring(15);

        //moles (more distinctive than looking for g for grams)
        boolean moleUnits = tUnits.indexOf("mol") >= 0 || 
            //M is used for Molar (moles/liter).  UDUNITS doesn't define it, but some datasets use it
            //M is the metric abbreviation prefix for Mega
            tUnits.indexOf("M") >= 0;   

        //gUnits is less useful, but its absence is useful     g may be from many terms
        boolean gUnits = tUnits.indexOf("g") >= 0; 

        tUnitsLC = tUnits.toLowerCase();

        if (isSomething(tStandardName)) {

            //fix some common invalid standard_names in WOA 2005
            if (     tStandardName.equals("chlorophyll"))   //avoids mol vs. g 
                     tStandardName      = "concentration_of_chlorophyll_in_sea_water";
            else if (tStandardName.equals("nitrate"))       //no g option
                     tStandardName      = "mole_concentration_of_nitrate_in_sea_water";
            else if (tStandardName.equals("nitrite"))       //no g option
                     tStandardName      = "mole_concentration_of_nitrite_in_sea_water";
            else if (tStandardName.equals("dissolved_oxygen"))      
                     tStandardName      = moleUnits?
                                          "mole_concentration_of_dissolved_molecular_oxygen_in_sea_water" :
                                          "mass_concentration_of_oxygen_in_sea_water";
            else if (tStandardName.equals("apparent_oxygen_saturation") ||
                     tStandardName.equals("percentage_oxygen_saturation"))      
                     tStandardName      = "fractional_saturation_of_oxygen_in_sea_water";
            else if (tStandardName.equals("phosphate"))      
                     tStandardName      = moleUnits?
                                          "mole_concentration_of_phosphate_in_sea_water" :
                                          "mass_concentration_of_phosphate_in_sea_water";
            else if (tStandardName.equals("salinity"))       
                     tStandardName      = "sea_water_salinity";
            else if (tStandardName.equals("silicate"))
                     tStandardName      = moleUnits?
                                          "mole_concentration_of_silicate_in_sea_water" :
                                          "mass_concentration_of_silicate_in_sea_water";    
            else if (tStandardName.equals("temperature"))  //dealt with specially below
                     tStandardName      = "";  //perhaps sea_water_temperature, perhaps air or land

            //and other common incorrect names
            else if (tStandardName.equals("eastward_sea_water_velocit")) //missing y
                     tStandardName = "eastward_sea_water_velocity";
            else if (tStandardName.equals("northward_sea_water_velocit")) //missing y
                     tStandardName = "northward_sea_water_velocity";

            else if (tStandardName.equals("grid_eastward_sea_water_velocity"))
                     tStandardName      = "eastward_sea_water_velocity";
            else if (tStandardName.equals("grid_northward_sea_water_velocity"))
                     tStandardName      = "northward_sea_water_velocity";
            else if (tStandardName.equals("ice_thickness"))       
                     tStandardName      = "sea_ice_thickness";
            else if (tStandardName.equals("ice_u_veloctiy") || //sic
                     tStandardName.equals("grid_eastward_sea_ice_velocity"))
                     tStandardName      = "sea_ice_x_velocity";
            else if (tStandardName.equals("ice_v_veloctiy") || //sic       
                     tStandardName.equals("grid_northward_sea_ice_velocity"))
                     tStandardName      = "sea_ice_y_velocity";
            else if (tStandardName.equals("net_surface_heat_flux") ||
                     tStandardName.equals("surface_heat_flux"))       
                     tStandardName      = "surface_downward_heat_flux_in_air";
            else if (tStandardName.equals("wave_direction_to"))       
                     tStandardName      = "sea_surface_wave_to_direction";
            else if (tStandardName.equals("wave_height"))       
                     tStandardName      = "sea_surface_wave_significant_height";
            else if (tStandardName.equals("wave_period"))       
                     tStandardName      = "sea_surface_swell_wave_period";

        }


        //do standard_name first, since long_name and colorBar can use it
        //    (These aren't crucial but are important. Try hard to be pretty confident.)
        String ttLongName = tLongName.toLowerCase();
        int asl = ttLongName.indexOf(" at sigma level ");
        if (asl > 0)
            ttLongName = ttLongName.substring(0, asl);
        String lc = "|" + lcSourceName + "|" + ttLongName + "|"; 
        lc = String2.replaceAll(lc, ' ', '|'); 
        lc = String2.replaceAll(lc, '_', '|'); 
        lc = String2.replaceAll(lc, '=', '|'); 
        lc = String2.replaceAll(lc, ',', '|'); 
        lc = String2.replaceAll(lc, '/', '|'); 
        String lcu = lc + 
            tStandardName.toLowerCase() + "|" +
            tUnitsLC                    + "|";
        lcu = String2.replaceAll(lcu, ' ', '|');
        lcu = String2.replaceAll(lcu, '_', '|');
        lcu = String2.replaceAll(lcu, '=', '|');
        lcu = String2.replaceAll(lcu, ',', '|');
        lcu = String2.replaceAll(lcu, '/', '|');
        if (reallyVerbose && 
            "|longitude|latitude|altitude|depth|time|".indexOf("|" + tStandardName + "|") < 0)
            String2.log("  sourceName=" + tSourceName + " lcu=" + lcu);

        //isDegreesC
        boolean isDegreesC = 
            testUnits.equals("°c")                  ||
            testUnits.equals("celsius")             ||
            testUnits.equals("degree|centigrade")   ||
            testUnits.equals("degree|celsius")      ||
            testUnits.equals("degrees|celsius")     ||
            testUnits.equals("degc")                ||
            testUnits.equals("degreec")             ||
            testUnits.equals("degreesc")            ||
            testUnits.equals("degree|c")            ||
            testUnits.equals("degrees|c")           ||
            testUnits.equals("deg|c")               ||
            testUnits.equals("degs|c")              ||
            testUnits.equals("cel")                 || //ucum
            testUnits.equals("celsius|degree")      || //special for UAF
            testUnits.endsWith("(degc)")            || //special for UAF
            testUnits.endsWith("(degc");               //special for UAF

        //isDegreesF
        boolean isDegreesF = 
            testUnits.equals("°f")                  ||
            testUnits.equals("fahrenheit")          ||
            testUnits.equals("degree|fahrenheit")   ||
            testUnits.equals("degrees|fahrenheit")  ||
            testUnits.equals("degf")                ||
            testUnits.equals("degreef")             ||
            testUnits.equals("degreesf")            ||
            testUnits.equals("degree|f")            ||
            testUnits.equals("degrees|f")           ||
            testUnits.equals("deg|f")               ||
            testUnits.equals("degs|f")              ||
            testUnits.equals("[degf]"); //ucum

        //isDegreesK
        boolean isDegreesK = 
            testUnits.equals("°k")                  ||
            testUnits.equals("kelvin")              ||
            testUnits.equals("degree|kelvin")       ||
            testUnits.equals("degrees|kelvin")      ||
            testUnits.equals("degk")                ||
            testUnits.equals("degreek")             ||
            testUnits.equals("degreesk")            ||
            testUnits.equals("degree|k")            ||
            testUnits.equals("degrees|k")           ||
            testUnits.equals("deg|k")               ||
            testUnits.equals("degs|k")              ||
            testUnits.equals("k");                     //udunits and ucum
        boolean hasTemperatureUnits = isDegreesC || isDegreesF || isDegreesK;

        if (isSomething(tStandardName)) {
            //deal with the mess that is salinity
            if (tStandardName.equals("sea_water_salinity") ||
                tStandardName.equals("sea_surface_salinity")) {
                //g/g and kg/kg are very rare
                if ("|g/g|kg/kg|g kg-1|g/kg|".indexOf("|" + tUnits + "|") >= 0) {
                    tStandardName = "sea_water_absolute_salinity";  //canonical is g/kg
                } else {
                    tStandardName = "sea_water_practical_salinity"; 
                    //Possibly changing units is very aggressive. I know.
                    //1 is CF canonical, but datasets have 1e-3, 1, psu, ...
                    //It is better to be aggressive and defy CF than have misleading/
                    //  bizarre units based on previous versions of CF standard names.
                    if (tUnitsLC.indexOf("pss") < 0)
                        tUnits = "PSU"; 
                }
            }
        } else {

            //does the lcSourceName or ttLongName equal a cfName?
            //special cases
            String tsn = String2.replaceAll(isSomething(lcSourceName)? lcSourceName : "\r", " ", "_"); //\r won't match anything
            String tln = String2.replaceAll(isSomething(ttLongName)?   ttLongName   : "\r", " ", "_");
            if (tsn.equals("lev") && tln.equals("altitude"))  tln = "\r"; //"altitude" is wrong
            int i = String2.indexOf(CfToFromGcmd.cfNames, tsn);
            if (i < 0)
                i = String2.indexOf(CfToFromGcmd.cfNames, tln);
            //String2.log(">>> CF? tsn=" + tsn + " tln=" + tln + " size=" + CfToFromGcmd.cfNames.length + " i=" + i);
            if (i >= 0)
                tStandardName = CfToFromGcmd.cfNames[i];
        }

        if (!isSomething(tStandardName)) {
            tStandardName = sourceAtts.getString("Standard_name");  //wrong case?
            if (!isSomething(tStandardName)) tStandardName = sourceAtts.getString("Standard_Name");  //wrong case?
            if (!isSomething(tStandardName) &&
               "surface_carbon_dioxide_mole_flux".equals(sourceAtts.getString("comment")))
                tStandardName = sourceAtts.getString("comment");

            if (!isSomething(tStandardName)) tStandardName = "";  //do after checking sourceAtts
            //"|" allows search for whole word

            //coads special case
            String tHistory = sourceAtts.getString("history"); //yes, for each variable
            boolean coads = isSomething(tHistory) && tHistory.indexOf("coads") >= 0;

            //from lcSourceName or lcLongName?  
            //  (some abbreviations are NDBC names from NdbcMetStation lists)
            //  (some abbreviations are from ICOADS)
            //!!!??? these are CF standard names, but not all datasets will be using CF standard names
                             //mostly alphabetical by tStandardName
            if (tUnits.indexOf("Interpolation error fields") >= 0 ||
                tUnits.indexOf("Monthly difference") >= 0 ||
                tUnits.indexOf("Number of observations") >= 0 ||
                tUnits.indexOf("Radius influence grid points") >= 0 ||
                tUnits.indexOf("Standard deviation of data") >= 0 ||
                tUnits.indexOf("Standard error of the mean") >= 0) {  
                //special case: don't assign stdName for WOA 2001 datasets 
                //with crucial info in units 

            } else if (isSomething(tStandardName)) {

            //catch sigma-theta before catch sigma
            } else if (lc.indexOf("sigma") >= 0 && 
                     lc.indexOf("theta") >= 0)      tStandardName = "sea_water_sigma_theta"; 

            //see similar CATCH STATISTICS below
            else if (
                     (lcu.indexOf("|n|") >= 0 && lcu.indexOf("degrees|n|") < 0) ||
                      (lcu.indexOf("count") >= 0 && lcu.indexOf("county") < 0) || 
                     lcu.indexOf("stddev")       >= 0 || 
                     lcu.indexOf("|sd|")         >= 0 || 
                     lcu.indexOf("|s.d.|")       >= 0 ||
                     lcu.indexOf("sigma")        >= 0 ||
                     lcu.indexOf("variance")     >= 0 ||
                     lcu.indexOf("confidence")   >= 0 ||
                     lcu.indexOf("precision")    >= 0 ||
                     lcu.indexOf("error")        >= 0 || //"interpolation error fields"
                     lcu.indexOf("number")       >= 0 || //"number of observations"
                     lcu.indexOf("|nobs|")       >= 0 || //number of observations
                     lcu.indexOf("radius|influence|grid|points") >= 0 ||
                     lcu.indexOf("standard|deviation") >= 0 ||
                     lcu.indexOf("standard|error") >= 0) {}

            //see similar CATCH QUALITY above and below        catch before others
            else if (lcu.indexOf("qc")           >= 0 || 
                     lcu.indexOf("qa")           >= 0 || 
                     (lcu.indexOf("quality") >= 0 && lcu.indexOf("science|quality") < 0) || 
                     lcu.indexOf("flag")         >= 0) {} 

            //coads special cases
            else if ((coads && lc.indexOf("|sflx|") >= 0) ||
                     lcu.indexOf("|surface|downward|sensible|heat|flux|") >= 0 ||
                     (lc.indexOf("|sensible|") >= 0 &&
                      lc.indexOf("|heat|")     >= 0 &&
                      lc.indexOf("|flux|")     >= 0))
                                                   tStandardName = "surface_downward_sensible_heat_flux";
            else if ((coads && lc.indexOf("|lflx|") >= 0) ||
                      lcu.indexOf("|surface|downward|latent|heat|flux|") >= 0 ||
                     (lc.indexOf("|latent|")   >= 0 &&
                      lc.indexOf("|heat|")     >= 0 &&
                      lc.indexOf("|flux|")     >= 0))
                                                    tStandardName = "surface_downward_latent_heat_flux";

            //oceanographic and meteorological                     
            else if (((lc.indexOf("|air") >= 0 && 
                       lc.indexOf("|temp") >= 0) ||
                      lc.indexOf("|atmp") >= 0) &&
                     lc.indexOf("diff") < 0 &&
                     hasTemperatureUnits)           tStandardName = lc.indexOf("anom") >= 0? //anomaly
                                                                     "air_temperature_anomaly" :
                                                                     lc.indexOf("rate") >= 0? "" :
                                                                     lc.indexOf("potential") >= 0? 
                                                                     "air_potential_temperature" :
                                                                     "air_temperature"; 
            else if (((lc.indexOf("|air")  >= 0 || lc.indexOf("|atmo") >= 0 || 
                       lc.indexOf("cloud") >= 0 || lc.indexOf("surface") >= 0 ||
                       lc.indexOf("tropopause") >= 0) && 
                      lc.indexOf("|press") >= 0) ||
                     //lc.indexOf("|bar|") >= 0 ||
                     lc.indexOf("|slp|") >= 0 ||
                     lc.indexOf("baromet") >= 0)    tStandardName = lc.indexOf("rate") >= 0? "" :
                                                                    lc.indexOf("|diff") >= 0? "" :
                                                                    lc.indexOf("|anom") >= 0? //anomaly
                                                                    "air_pressure_anomaly" :
                                                                    (lc.indexOf("|slp|") >= 0 ||
                                                                     lc.indexOf("surface") >= 0)?
                                                                    "surface_air_pressure" :
                                                                    "air_pressure"; 
            else if (lc.indexOf("albedo") >= 0 &&
                     (lcu.equals("percent") || lcu.equals("%") || lcu.equals("1")))  
                                                    tStandardName = 
                                                        lc.indexOf("cloud")     >= 0? "cloud_albedo" :
                                                        lc.indexOf("planetary") >= 0? "planetary_albedo" :
                                                        lc.indexOf("ice")       >= 0? "sea_ice_albedo" :
                                                        lc.indexOf("soil")      >= 0? "soil_albedo" :
                                                        "surface_albedo";
            else if ((lcu.indexOf("|sea|floor|depth|") >= 0 ||
                      lcu.indexOf("|etopo2|") >= 0 ||
                      lcu.indexOf("bathymetry") >= 0 ||
                      lcu.indexOf("|bottom|depth|") >= 0) &&
                     isMeters)
                                                    tStandardName = "sea_floor_depth";
            else if ((lc.indexOf("|land") >= 0 && lc.indexOf("mask|") >= 0)) 
                                                    tStandardName = "land_binary_mask";
            else if (lcu.indexOf("|cloud|area|fraction|") >= 0 ||
                     (lc.indexOf("cloud") >= 0 && lc.indexOf("fraction") >= 0) ||
                     (lc.indexOf("cloud") >= 0 && lc.indexOf("cover") >= 0) ||
                     lc.indexOf("|cldc|") >= 0)     tStandardName = "cloud_area_fraction"; 
            else if (lcSourceName.equals("depth"))   tStandardName = "depth"; 
            else if (lcu.indexOf("|liquid|water|content|of|surface|snow|") >= 0 ||
                     (lc.indexOf("|water|") >= 0 && 
                      lc.indexOf("|equiv") >= 0 && 
                      lc.indexOf("|snow|") >= 0 && 
                      lc.indexOf("|surface|") >= 0))  tStandardName = "liquid_water_content_of_surface_snow"; 
            else if (((lc.indexOf("dew") >= 0 && 
                       lc.indexOf("point") >= 0) ||
                      lcu.indexOf("|dew|point|temperature|") >= 0 ||
                      lc.indexOf("|dewp") >= 0) &&
                     hasTemperatureUnits)      tStandardName = lc.indexOf("rate") >= 0? "" :
                                                        "dew_point_temperature"; 

            else if (lcu.indexOf("|lwe|water|evaporation|rate|") >= 0 ||
                     (lc.indexOf("|evapo") >= 0 && 
                      lc.indexOf("|rate|") >= 0))   tStandardName = "lwe_water_evaporation_rate";
            else if (lcu.indexOf("|rainfall|rate|") >= 0 ||
                     (lc.indexOf("|rain") >= 0 && 
                      lc.indexOf("fall") >= 0 && 
                      lc.indexOf("|rate|") >= 0))   tStandardName = "rainfall_rate";

            else if ((lc.indexOf("surface") >= 0 || lc.indexOf("net") >= 0) && 
                     lc.indexOf("|heat|flux|") >= 0) {
                if (lc.indexOf("upward") >= 0) {
                    tStandardName = 
                        lc.indexOf("latent") >= 0?   "surface_upward_latent_heat_flux" : 
                        lc.indexOf("sensible") >= 0? "surface_upward_sensible_heat_flux" :
                                                     "surface_upward_heat_flux_in_air";                     
                } else {
                    tStandardName = 
                        lc.indexOf("latent") >= 0?   "surface_downward_latent_heat_flux" : 
                        lc.indexOf("sensible") >= 0? "surface_downward_sensible_heat_flux" :
                                                     "surface_downward_heat_flux_in_air";                     
                }}
            else if (lc.indexOf("momentum|flux") >= 0) {
                if (lc.indexOf("eastward") >= 0 ||
                    lc.indexOf("|uflx|") >= 0 ||
                    lc.indexOf("|u|") >= 0 ||
                    lc.indexOf("zonal") >= 0) {
                    tStandardName = "downward_eastward_momentum_flux_in_air";
                } else {
                    tStandardName = "downward_northward_momentum_flux_in_air";
                }}
            else if ((lc.indexOf("surface") >= 0 || lc.indexOf("net") >= 0) && 
                     lc.indexOf("|longwave|flux|") >= 0) 
                                                    tStandardName = lc.indexOf("upward") >= 0?
                                                        "surface_net_upward_longwave_flux" :
                                                        "surface_net_downward_longwave_flux";
            else if ((lc.indexOf("surface") >= 0 || lc.indexOf("net") >= 0) && 
                     lc.indexOf("|shortwave|flux|") >= 0) 
                                                    tStandardName = lc.indexOf("upward") >= 0?
                                                        "surface_net_upward_shortwave_flux" :
                                                        "surface_net_downward_shortwave_flux";
            else if ((lc.indexOf("wave") >= 0 && lc.indexOf("height") >= 0 &&
                      lc.indexOf("ucmp") < 0  && lc.indexOf("vcmp") < 0 && lc.indexOf("spectral") < 0) ||
                     lc.indexOf("|wvht|") >= 0)     tStandardName = lc.indexOf("swell") >= 0?
                                                                    "sea_surface_swell_wave_significant_height" :
                                                                    lc.indexOf("wind") >= 0?
                                                                    "sea_surface_wind_wave_significant_height" :
                                                                    "sea_surface_wave_significant_height";
            else if ((lc.indexOf("wave") >= 0 && lc.indexOf("period") >= 0) ||
                     lc.indexOf("|dpd|") >= 0 ||
                     lc.indexOf("|apd|") >= 0)      tStandardName = lc.indexOf("wind") >= 0?
                                                                    "sea_surface_wind_wave_period" :
                                                                    "sea_surface_swell_wave_period";                                                  
            else if ((lc.indexOf("wave") >= 0 && lc.indexOf("dir") >= 0 &&
                      lc.indexOf("ucmp") < 0  && lc.indexOf("vcmp") < 0 && lc.indexOf("spectral") < 0) ||
                     lc.indexOf("|mwd|") >= 0)      tStandardName = lc.indexOf("swell") >= 0?
                                                                    "sea_surface_swell_wave_to_direction" :
                                                                    lc.indexOf("wind") >= 0?
                                                                    "sea_surface_wind_wave_to_direction" :
                                                                    lc.indexOf("from") >= 0?
                                                                    "sea_surface_wave_from_direction" : //only 'from' option
                                                                    "sea_surface_wave_to_direction";            
            else if ((lc.indexOf("water") >= 0 && 
                      lc.indexOf("density") >= 0))  tStandardName = "sea_water_density"; 
            else if (lc.indexOf("conduct") >= 0)    tStandardName = "sea_water_electrical_conductivity"; 
            else if ((lc.indexOf("salinity") >= 0 || 
                      lc.indexOf("sss") >= 0 || //OSMC sea surface salinity
                      lc.indexOf("zsal") >= 0 || //OSMC salinity (at depths?)
                      lc.indexOf("salt") >= 0) &&
                     lc.indexOf("diffusion") < 0) {
                if (lc.indexOf("fl|") >= 0 ||
                    lc.indexOf("flx") >= 0 ||
                    lc.indexOf("flux") >= 0 ||
                    lc.indexOf("transport") >= 0) {
                    if (lc.indexOf("|x|") >= 0 || 
                        lc.indexOf("|u|") >= 0 || 
                        lc.indexOf("east") >= 0) {
                        tStandardName = "ocean_salt_x_transport"; 
                    } else if (
                        lc.indexOf("|y|") >= 0 ||  
                        lc.indexOf("|v|") >= 0 ||  
                        lc.indexOf("north") >= 0) {
                        tStandardName = "ocean_salt_y_transport"; 
                    } else if (
                        lc.indexOf("river") >= 0) {
                        tStandardName = "salt_flux_into_sea_water_from_rivers";
                    } else {
                        //no generic salt_flux
                    }
                } else {
                    if ("|g kg-1|g/kg|".indexOf("|" + tUnits + "|") >= 0) {
                        tStandardName = "sea_water_absolute_salinity"; 
                    } else {
                        tStandardName = "sea_water_practical_salinity"; 
                        if (tUnitsLC.indexOf("pss") < 0)
                            tUnits = "PSU"; //1 is CF canonical, but datasets have 1e-3, 1, psu, ...
                            //better to defy CF than have misleading bizarre units.
                    }
                }}
            else if (((lc.indexOf("water") >= 0 && 
                       lc.indexOf("temp") >= 0) ||
                     lc.indexOf("|wtmp|") >= 0 ||
                     lc.indexOf("|ztmp|") >= 0) && //OSMC temperature (at depths?)
                     lc.indexOf("diff") < 0 &&
                     hasTemperatureUnits)     tStandardName = lc.indexOf("anom") >= 0? //anomaly
                                                                    "surface_temperature_anomaly" : //no sea_water_temperature_anomaly
                                                                    lc.indexOf("rate") >= 0? "" :
                                                                    lc.indexOf("potential") >= 0? 
                                                                    "sea_water_potential_temperature" :
                                                                    "sea_water_temperature"; //sea vs river???
            else if ((lc.indexOf("sst") >= 0 ||
                      lc.indexOf("sea|surface|temp") >= 0) &&
                     hasTemperatureUnits &&
                     lc.indexOf("diff") < 0 &&
                     lc.indexOf("time") < 0)        tStandardName = lc.indexOf("gradient") >= 0? "":
                                                                    lc.indexOf("anom") >= 0? //anomaly
                                                                    "surface_temperature_anomaly" : //no sea_surface_temperature_anomaly
                                                                    lc.indexOf("land") >= 0?
                                                                    "surface_temperature" :
                                                                    "sea_surface_temperature";
            else if (lc.indexOf("wet") >= 0 && 
                     lc.indexOf("bulb") >= 0 &&
                     hasTemperatureUnits)           tStandardName = "wet_bulb_temperature"; 
            else if (lc.indexOf("wind") < 0 &&
                     ((lc.indexOf("current") >= 0 && 
                       lc.indexOf("east") >= 0) ||
                      lc.indexOf("|eastward|sea|water|velocity|") >= 0 ||
                      lc.indexOf("current|u") >= 0 ||
                      lc.indexOf("currentu") >= 0 ||
                      lc.indexOf("ucur") >= 0 ||
                      lc.indexOf("current|x") >= 0 ||
                      lc.indexOf("|wu|") >= 0 ||
                      lc.indexOf("water|u") >= 0 ||
                      lc.indexOf("wateru") >= 0 ||
                      lc.indexOf("water|x") >= 0))    tStandardName = "eastward_sea_water_velocity";
            else if (lc.indexOf("wind") < 0 &&
                     ((lc.indexOf("current") >= 0 && 
                       lc.indexOf("north") >= 0) ||
                      lc.indexOf("|northward|sea|water|velocity|") >= 0 ||
                      lc.indexOf("current|v|") >= 0 || //beware current_velocity
                      lc.indexOf("currentv|") >= 0 ||
                      lc.indexOf("vcur") >= 0 ||
                      lc.indexOf("current|y") >= 0 ||
                      lc.indexOf("|wv|") >= 0 ||
                      lc.indexOf("water|v|") >= 0 ||  //beware water_velocity
                      lc.indexOf("waterv|") >= 0 ||
                      lc.indexOf("water|y") >= 0))    tStandardName = "northward_sea_water_velocity";
            else if ((lc.indexOf("surface") >= 0 && lc.indexOf("roughness") >= 0 &&
                      isMeters))                    tStandardName = "surface_roughness_length"; 

            else if (lcSourceName.equals("par")) 
                    tStandardName = "downwelling_photosynthetic_photon_radiance_in_sea_water";
            
            else if (lcSourceName.equals("ph"))
                    tStandardName = "sea_water_ph_reported_on_total_scale";

            else if (((lc.indexOf("rel") >= 0 && 
                       lc.indexOf("hum") >= 0) ||
                      lc.indexOf("humidity") >= 0 ||
                      lc.indexOf("|rhum|") >= 0 ||
                      lc.indexOf("|rh|") >= 0) &&
                     (tUnitsLC.equals("percent") ||
                      tUnitsLC.equals("%")))         tStandardName = lc.indexOf("rate") >= 0? "" :
                                                        "relative_humidity"; 
            else if ((lc.indexOf("spec") >= 0 && 
                      lc.indexOf("hum") >= 0) ||
                     lc.indexOf("|shum|") >= 0)     tStandardName = "specific_humidity"; 
            else if (lcu.indexOf("|soil|") >= 0 && 
                     lcu.indexOf("|moisture|") >= 0)tStandardName = "soil_moisture_content";
            else if ((lc.indexOf("geopotential") >= 0 && 
                      lc.indexOf("height") >= 0))   tStandardName = "geopotential_height"; 
            else if ((lc.indexOf("surface") >= 0 && 
                      lc.indexOf("height") >= 0) ||  //sea_surface_height
                     lc.indexOf("ssh") >= 0 ||       //there are more specific, e.g. above geoid
                     lc.indexOf("surf|el") >= 0 ||       //there are more specific, e.g. above geoid
                     (lc.indexOf("|tide|") >= 0 && 
                         lc.indexOf("current") < 0 &&
                         lc.indexOf("angle") < 0 &&
                         lc.indexOf("period") < 0))
                                                    tStandardName = lc.indexOf("anom") >= 0? //anomaly
                                                                    "sea_surface_elevation_anomaly" :
                                                                    "sea_surface_height"; 
            else if (lcu.indexOf("|direction|of|sea|water|velocity|") >= 0 ||
                     (lc.indexOf("tidal") >= 0 && 
                      lc.indexOf("angle") >= 0))    tStandardName = "direction_of_sea_water_velocity";
            else if (lcSourceName.equals("omega"))  tStandardName = "omega";
            else if ((lc.indexOf("|precip") >= 0 && 
                      lc.indexOf("|rate|") >= 0))   tStandardName = "lwe_precipitation_rate";
            else if (lcu.indexOf("|water|content|of|atmosphere|layer|") >= 0 ||
                     (lc.indexOf("|precipitable|") >= 0 && 
                      lc.indexOf("|atmosphere|") >= 0 && 
                      lc.indexOf("|water|") >= 0))  tStandardName = "water_content_of_atmosphere_layer";
            else if (lcu.indexOf("|surface|downward|eastward|stress|") >= 0 ||
                     (lc.indexOf("stress") >= 0 && 
                      lc.indexOf("x") >= 0 &&
                      lc.indexOf("max") < 0) ||
                     lc.indexOf("|upstr|") >= 0 || //coads
                     lc.indexOf("|tauu|") >= 0 ||
                     lc.indexOf("|tau|u|") >= 0 ||
                     lc.indexOf("|taux|") >= 0 ||
                     lc.indexOf("|tau|x|") >= 0)    tStandardName = "surface_downward_eastward_stress";
            else if (lcu.indexOf("|surface|downward|northward|stress|") >= 0 ||
                     (lc.indexOf("stress") >= 0 && 
                      lc.indexOf("y") >= 0) ||
                     lc.indexOf("|vpstr|") >= 0 || //coads
                     lc.indexOf("|tauv|") >= 0 ||
                     lc.indexOf("|tau|v|") >= 0 ||
                     lc.indexOf("|tauy|") >= 0 ||
                     lc.indexOf("|tau|y|") >= 0)    tStandardName = "surface_downward_northward_stress";
            else if (lcu.indexOf("|tendency|of|air|pressure|") >= 0 ||
                     lc.indexOf("|ptdy|") >= 0)     tStandardName = "tendency_of_air_pressure"; 
            else if (lc.indexOf("visibility") >= 0 || 
                     lc.indexOf("|vis|") >= 0)      tStandardName = "visibility_in_air"; 
            else if ((lc.indexOf("east") >= 0 && 
                      lc.indexOf("wind") >= 0) ||
                     lc.indexOf("|u-wind|") >= 0 || 
                     lc.indexOf("|wind|u|") >= 0 || 
                     lc.indexOf("|wspu|") >= 0 || 
                     lc.indexOf("|uwnd|") >= 0 ||                      
                     lc.indexOf("u wind") >= 0 ||                      
                     lc.indexOf("|xwnd|") >= 0)     tStandardName = "eastward_wind";
            else if ((lc.indexOf("north") >= 0 && 
                      lc.indexOf("wind") >= 0) ||
                     lc.indexOf("|v-wind|") >= 0 || 
                     lc.indexOf("|wind|v|") >= 0 || 
                     lc.indexOf("|wspv|") >= 0 ||
                     lc.indexOf("|vwnd|") >= 0 ||                       
                     lc.indexOf("v wind") >= 0 ||                      
                     lc.indexOf("|ywnd|") >= 0)     tStandardName = "northward_wind";
            else if ((lc.indexOf("wind") >= 0 && 
                      lc.indexOf("dir") >= 0) ||
                     lc.indexOf("|wd|") >= 0)       tStandardName = "wind_from_direction";
            else if (lc.indexOf("gust") >= 0 ||
                     lc.indexOf("|gst|") >= 0) {
                if      (lc.indexOf("dir") >=0)      {}
                else if (lc.indexOf("time") >=0)     {}
                else                                 tStandardName = "wind_speed_of_gust";
                }
            else if ((lc.indexOf("wind") >= 0 && 
                      lc.indexOf("speed") >= 0) ||   //not wspd3, which should have a stdName but doesn't
                     lc.indexOf("|wspd|") >= 0)      tStandardName = "wind_speed";

            //chemistry
            else if (lc.indexOf("ammoni") >= 0 ||
                     lc.indexOf("|nh4|") >= 0)      tStandardName = "mole_concentration_of_ammonium_in_sea_water";
            else if (lcu.indexOf("|zooplankton|expressed|as|carbon|in|sea|water|") >= 0 ||
                     lc.indexOf("|zooplankton|carbon|content|") >= 0) //no mass option      
                                                    tStandardName = "mole_concentration_of_zooplankton_expressed_as_carbon_in_sea_water";
            else if (lc.indexOf("chlorophyll") >= 0 ||       
                     lc.indexOf("chl|a|")      >= 0 ||               
                     lc.indexOf("chlor|a|")    >= 0 ||
                     lc.indexOf("|chlora|")    >= 0 ||
                     lc.indexOf("|chlormean|") >= 0 ||              //avoids mol vs. g 
                     lc.indexOf("|chla|") >= 0)     tStandardName = lc.indexOf("anom") >= 0? "": //anomaly
                                                                    lc.indexOf("index") >= 0? "": 
                                                                    "concentration_of_chlorophyll_in_sea_water";
            //else if ((lc.indexOf("no2") >= 0 && 
            //          lc.indexOf("no3") >= 0) ||                     //catch NO2 NO3 together before separately
            //         lc.indexOf("nitrogen") >= 0)   tStandardName = ??? there is no plain nitrogen, nox or noy in_sea_water
            else if (lc.indexOf("nitrate") >= 0 ||                  //no g option
                     lc.indexOf("|no3|") >= 0)      tStandardName = "mole_concentration_of_nitrate_in_sea_water";
            else if (lc.indexOf("nitrite") >= 0 ||                   //no g option
                     lc.indexOf("|no2|") >= 0)      tStandardName = "mole_concentration_of_nitrite_in_sea_water";
            else if (lc.indexOf("|dissolved|oxygen|") >= 0)      
                                                    tStandardName = moleUnits?
                                                        "mole_concentration_of_dissolved_molecular_oxygen_in_sea_water" :
                                                        gUnits?
                                                        "mass_concentration_of_oxygen_in_sea_water" :
                                                        "volume_fraction_of_oxygen_in_sea_water";

            else if (lcu.indexOf("|fractional|saturation|of|oxygen|in|sea|water|") >= 0 ||
                     (lc.indexOf("|apparent|") >= 0 || 
                      lc.indexOf("|percent|") >= 0 || lc.indexOf('%') >= 0) &&
                     lc.indexOf("|oxygen|saturation|") >= 0)      
                                                    tStandardName = "fractional_saturation_of_oxygen_in_sea_water";
            else if (lc.indexOf("phosphate") >= 0 ||
                     lc.indexOf("|po4|") >= 0)      tStandardName = moleUnits?
                                                         "mole_concentration_of_phosphate_in_sea_water" :
                                                         "mass_concentration_of_phosphate_in_sea_water";
            else if (lc.indexOf("silicate") >= 0 ||
                     lc.indexOf("|si|") >= 0)       tStandardName = moleUnits?
                                                         "mole_concentration_of_silicate_in_sea_water" :
                                                         "mass_concentration_of_silicate_in_sea_water";

            //special fixup
            if ("temperature".equals(oStandardName) &&
                "".equals(tStandardName)) //couldn't determine if sea, air, or land
                tStandardName = "null";

            //update lcu
            lcu = lc + 
                tStandardName.toLowerCase() + "|" +
                tUnitsLC                    + "|";
            lcu = String2.replaceAll(lcu, ' ', '|');
            lcu = String2.replaceAll(lcu, '_', '|');
            lcu = String2.replaceAll(lcu, '=', '|');
            lcu = String2.replaceAll(lcu, ',', '|');
            lcu = String2.replaceAll(lcu, '/', '|');

        }
        if (reallyVerbose && 
            "|longitude|latitude|altitude|depth|time|".indexOf("|" + tStandardName + "|") < 0)
            String2.log("    tStandardName=" + tStandardName);

        //colorBar  (if these are specified, WMS works and graphs in general work better)
        //    (These are not crucial. If it's wrong, it isn't terrible.)
        if (tryToAddColorBarMinMax &&
            Double.isNaN(sourceAtts.getDouble("colorBarMinimum")) &&
            Double.isNaN(sourceAtts.getDouble("colorBarMaximum"))) { 
           
            double tMin = Double.NaN;
            double tMax = Double.NaN;
            String colorBarScale = null;

            //assign based on standard_name first (more consistent and less extreme than valid_min/max from other sources)
            //Fortunately, the penalty for being wrong (e.g., different units than expected) is small: bad default colorBar range.
            //FUTURE: These are CF standard_names.  Add standard_names from other standards.
            if (testUnits.indexOf("interpolation|error|fields") >= 0 ||
                tUnitsLC.indexOf("difference") >= 0) {
                if (testUnits.indexOf("fraction") >= 0 || 
                    (testUnits.indexOf("1") >= 0 && testUnits.indexOf("-1") < 0)) {
                    tMin = -0.1;  tMax = 0.1;
                } else if ((testUnits.indexOf("psu") >= 0 && testUnits.indexOf("psue") < 0) || //psuedo
                    testUnits.indexOf("pss") >= 0) {
                    tMin = -1;    tMax = 1;
                } else if (hasTemperatureUnits ||
                    testUnits.indexOf("percent") >= 0) {
                    tMin = -5;    tMax = 5;
                } else {
                    tMin = -10;   tMax = 10;
                }}

            else if (lcu.indexOf("stddev")       >= 0 || 
                lcu.indexOf("|sd|")         >= 0 || 
                lcu.indexOf("|s.d.|")       >= 0 ||
                lcu.indexOf("variance")     >= 0 ||
                lcu.indexOf("confidence")   >= 0 ||
                lcu.indexOf("error")        >= 0 || 
                lcu.indexOf("standard|deviation") >= 0 ||
                lcu.indexOf("standard|error") >= 0) {
                if (testUnits.indexOf("fraction") >= 0 || 
                    (testUnits.indexOf("1") >= 0 && testUnits.indexOf("-1") < 0)) {
                    tMin = 0;    tMax = 0.1;
                } else if ((testUnits.indexOf("psu") >= 0 && testUnits.indexOf("psue") < 0) || //psuedo
                    testUnits.indexOf("pss") >= 0) {
                    tMin = 0;    tMax = 1;
                } else if (hasTemperatureUnits ||
                    testUnits.indexOf("percent") >= 0) {
                    tMin = 0;    tMax = 5;
                } else {
                    tMin = 0;    tMax = 50;
                }}


            //see similar CATCH STATISTICS above and below     catch before others
            //here just catch n and count and make a crude guess at 0 to 100
            else if (lcu.indexOf("number")       >= 0 || 
                     (lcu.indexOf("count") >= 0 && lcu.indexOf("county") < 0) || 
                     lcu.indexOf("radius|influence|grid|points") >= 0)     {tMin = 0;    tMax = 100;}

            else if (lcu.indexOf("|mask|on|") >= 0 && 
                     lcu.indexOf("points|") >= 0)                          {tMin = 0;    tMax = 1.5;}
            else if (lcu.indexOf("mask") >= 0 && sourceAtts.get("actual_range") == null)
                                                                           {tMin = 0;    tMax = 127;}

            else if (lcu.indexOf("processing|param") >= 0)                 {tMin = 0;    tMax = 10;}                    

            //see similar CATCH QUALITY above and below        catch before others
            else if (lcu.indexOf("qc")           >= 0 || 
                     lcu.indexOf("qa")           >= 0 || 
                    (lcu.indexOf("quality") >= 0 && lcu.indexOf("science|quality") < 0) || 
                     lcu.indexOf("flag")         >= 0                   )  {tMin = 0;    tMax = 128;}

            //special: catch "percent" 
            else if (lcu.indexOf("percent") >= 0 ||
                     tUnits.indexOf('%') >= 0)                             {tMin = 0;    tMax = 100;}

            else if (tUnitsLC.indexOf("yyyy") >= 0                      )  {tMin = 1950; tMax = 2020;}  //special case ("fraction part of year")

            //special: catch fraction  e.g., "cloud_area_fraction" "sea_ice_area_fraction"
            else if (lcu.indexOf("fraction") >= 0) {
                if (sourceAtts.getDouble("valid_max") == 100 && 
                    sourceAtts.getDouble("scale_factor") != 0.01) {
                    //'percent' caught above, but some "fraction" are expressed as percent
                    tMin = 0;    tMax = 100;
                } else {
                    tMin = 0;    tMax = 1;
                    if (tUnits.equals("-")) {
                        tUnits = "1";
                        tUnitsLC = "1";
                    }                        
                }
              }

            //catch 0 - 360   
            else if (tStandardName.endsWith("_from_direction"           ) ||
                     tStandardName.endsWith("_to_direction"             ) ||
                     tStandardName.startsWith("direction_of_"           ) ||
                     tUnits.equals("degrees_true"                       )) {tMin = 0;    tMax = 360;}

            //catch z in Coastal Relief Model
            else if (lcSourceName.equals("z") && 
                    isMeters && 
                    "up".equals(sourceAtts.getString("positive"))) {
                tMin = -8000; tMax = 8000;
                addAtts.add("colorBarPalette", "Topography");
                }

            //catch normal things
            else if (tStandardName.equals("surface_air_pressure"        ) ||
                     tStandardName.equals("air_pressure"                ) ||
                     tStandardName.startsWith("air_pressure_at"         )) {
                if (tUnitsLC.startsWith("pa")) {  //or pascals
                    if (lc.indexOf("tropopause") >= 0) {
                        tMin = 0;  tMax = 40000;
                    } else if (lc.indexOf("high|cloud") >= 0) {
                        tMin = 25000;  tMax = 50000;
                    } else if (lc.indexOf("cloud") >= 0) {
                        tMin = 30000;  tMax = 90000;
                    } else {
                        tMin = 95000;  tMax = 105000;
                    }
                } else {
                    tMin = 950;  tMax = 1050;
                }}
            else if (tStandardName.equals("air_pressure_anomaly"        )) {
                if (tUnitsLC.equals("pa")) {
                    tMin = -3000; tMax = 3000;
                } else {
                    tMin = -30;   tMax = 30;
                }}                
            else if (tStandardName.equals("air_temperature"             ) ||
                     tStandardName.equals("air_potential_temperature"   )) {
                if (isDegreesK) {
                    if (lc.indexOf("tropopause") >= 0) {
                        tMin = 190;  tMax = 230;
                    } else {
                        tMin = 263;  tMax = 313;
                    }
                } else if (isDegreesF) {
                    tMin = 14;   tMax = 104;
                } else {
                    tMin = -10;  tMax = 40;
                }}
            else if (tStandardName.equals("air_temperature_anomaly"     )) {tMin = -10;  tMax = 10;}
            else if (tStandardName.equals("water_content_of_atmosphere_layer"))
                                                                           {tMin = 0;    tMax = 50;}
            else if (tStandardName.equals("atmosphere_cloud_condensed_water_content") ||
                     tStandardName.equals("atmosphere_cloud_ice_content") ||
                     tStandardName.equals("atmosphere_cloud_liquid_water_content)"))
                                                                           {tMin = 0;    tMax = 0.5;}

            else if (lcu.indexOf("bottom") >= 0 && 
                     lcu.indexOf("roughness") >= 0)                        {tMin = 0;    tMax = 0.1;}
            else if (tStandardName.equals("concentration_of_chlorophyll_in_sea_water") ||
                     tStandardName.equals("chlorophyll_concentration_in_sea_water") ||
                     tStandardName.equals("mass_concentration_of_chlorophyll_a_in_sea_water") ||
                     tStandardName.equals("mass_concentration_of_chlorophyll_in_sea_water"
                                                                        )) {tMin = 0.03; tMax = 30; colorBarScale = "Log";}
            else if (lcSourceName.equals("chloranomaly"                 )) {tMin = -5;   tMax = 5;}
            else if (tStandardName.equals("dew_point_temperature"       )) {
                if (isDegreesK) {
                    tMin = 273; tMax = 313;                  
                } else if (isDegreesF) {
                    tMin = 14;  tMax = 104;                  
                } else {
                    tMin = 0;   tMax = 40;
                }}                  
            else if (lcu.indexOf("dilution|of|precision") >= 0          )  {tMin = 0;    tMax = 1;}
            else if (lcu.indexOf("evaporation") >= 0                    )  {tMin = -1e-4;tMax = 1e-4;}
            else if (lcu.indexOf("|u-flux|") >= 0 ||
                     lcu.indexOf("|v-flux|") >= 0)                         {tMin = -1e6; tMax = 1e6;}
            else if (tStandardName.equals("geopotential_height")          ){tMin = -50;  tMax = 1000;}
            else if (lcSourceName.indexOf("graphics") >= 0              )  {tMin = 0;    tMax = 20;}
            else if (lcSourceName.indexOf("k490") >= 0                  )  {tMin = 0;    tMax = 0.5;}
            else if (tStandardName.equals("lagrangian_tendency_of_air_pressure")) 
                                                                           {tMin = -0.02;tMax = 0.02;}
            else if (lcSourceName.indexOf("|lifted|index|") >= 0        )  {tMin = -10;  tMax = 50;}
            else if (tStandardName.equals("ocean_mixed_layer_thickness") ||
                     tStandardName.equals("mixed_layer_depth")           ) {tMin = 0;    tMax = 100;}
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
            else if (tStandardName.equals("mole_concentration_of_zooplankton_expressed_as_carbon_in_sea_water")) 
                                                                           {tMin = 0;    tMax = 100;}
            else if (tStandardName.equals("mole_fraction_of_o3_in_air")) 
                                                                           {tMin = 0;    tMax = 30;}            
            else if (tStandardName.equals("downward_eastward_momentum_flux_in_air") ||
                     tStandardName.equals("downward_northward_momentum_flux_in_air"))
                                                                           {tMin = -1;   tMax = 1;}
            else if (lcu.indexOf("momentum|component") >= 0)               {tMin = -0.3; tMax = 0.3;}
            else if (lcu.indexOf("momentum|stress") >= 0)                  {tMin = -0.1; tMax = 0.1;}

            else if (tStandardName.equals("net_primary_productivity_of_carbon")) 
                                                                           {tMin = 0;    tMax = 2000;}
            else if (tStandardName.equals("eastward_ocean_heat_transport")) 
                                                                           {tMin = -1e-4;tMax = 1e-4;}
            else if (tStandardName.equals("northward_ocean_heat_transport")) 
                                                                           {tMin = -1e-4;tMax = 1e-4;}

            else if (tStandardName.equals("ocean_meridional_overturning_streamfunction")) 
                                                                           {tMin = 0;    tMax = 40;}            
            else if (tStandardName.equals("ocean_salt_x_transport") ||
                     tStandardName.equals("ocean_salt_y_transport") ||
                     lcu.indexOf("|salt|flux|") >= 0 ||
                     lcu.indexOf("|salinity|flux|") >= 0)                  {tMin = -2e-6;tMax = 2e-6;}
            else if (tUnitsLC.equals("okta")                             ) {tMin = 0;    tMax = 9;}
            else if (tStandardName.equals("omega"))                        {tMin = -0.1; tMax = 0.1;}
            else if (lcu.indexOf("apparent|oxygen|utilization") >= 0    )  {tMin = -1;   tMax = 1;}                    
            else if (tStandardName.equals("volume_fraction_of_oxygen_in_sea_water")
                                                                        )  {tMin = 0;    tMax = 10;}                    
            else if (tStandardName.indexOf("oxygen_in_sea_water") >= 0)    {tMin = 0;    tMax = 500;}                      
            else if (tStandardName.indexOf("water_flux_into_ocean") >= 0)  {tMin = 0;    tMax = 1e-4;}
            else if (tStandardName.equals("downwelling_photosynthetic_photon_radiance_in_sea_water") ||
                     lcSourceName.equals("par")) {
                if (tUnitsLC.equals("volt") || tUnitsLC.equals("volts"))   {tMin = 0;    tMax = 3;}
                else                  /* microEinsteins m^-2 s-1 */        {tMin = 0;    tMax = 70;}} 
            else if (tStandardName.equals("sea_water_ph_reported_on_total_scale") ||
                     lcSourceName.equals("ph"))                            {tMin = 7;    tMax = 9;}
            else if (tStandardName.indexOf("precipitation") >= 0 ||
                     tStandardName.indexOf("snowfall") >= 0 ||
                     tStandardName.indexOf("rainfall") >= 0 ||
                     tStandardName.indexOf("runoff") >= 0 ||
                     tStandardName.indexOf("graupel") >= 0) {
                if (tStandardName.indexOf("flux") >= 0 ||
                    tStandardName.indexOf("rate") >= 0) {
                    tMin = 0;    tMax = 1e-4;
                } else {
                    tMin = 0;    tMax = 1;
                }}                    
            else if (tStandardName.equals("relative_humidity"           )) {tMin = 20;   tMax = 100;}                    
            else if (lcSourceName.indexOf("667") >= 0                   )  {
                if (lcSourceName.indexOf("anom") >= 0) {
                    tMin = -0.01; tMax = 0.01;
                } else {
                    tMin = -25000.055;tMax = -25000.035;
                }}
            else if (lcu.indexOf("ripple") >= 0 && 
                     lcu.indexOf("length") >= 0)                           {tMin = 0;    tMax = 0.2;}
            else if (lcu.indexOf("ripple") >= 0 && 
                     lcu.indexOf("height") >= 0)                           {tMin = 0;    tMax = 0.02;}
            else if (tStandardName.equals("sea_floor_depth") ||
                     tStandardName.equals("sea_floor_depth_below_geoid") ||
                     tStandardName.equals("sea_floor_depth_below_sea_level") ||
                     tStandardName.equals("sea_floor_depth_below_sea_surface") ||
                     tStandardName.equals("depth_at_nodes") ||
                     tStandardName.equals("depth")) {
                tMin = 0;    tMax = 8000;
                addAtts.add("colorBarPalette", "OceanDepth");
                }                
            else if (tStandardName.equals("sea_ice_thickness"           )) {tMin = 0;   tMax = 2.5;}                    
            else if (tStandardName.equals("eastward_sea_ice_velocity")  ||
                     tStandardName.equals("northward_sea_ice_velocity") || 
                     tStandardName.equals("sea_ice_x_velocity")         ||         
                     tStandardName.equals("sea_ice_y_velocity"          )) {tMin = -0.1;tMax = 0.1;}                    
            else if (tStandardName.equals("sea_surface_height_above_geoid") ||
                     tStandardName.equals("sea_surface_height_above_reference_ellipsoid") ||
                     tStandardName.equals("sea_surface_height_above_sea_level") ||
                     tStandardName.equals("sea_surface_height") ||
                     tStandardName.equals("sea_surface_elevation") ||
                     tStandardName.equals("sea_surface_elevation_anomaly") ||
                     tStandardName.equals("water_surface_height_above_reference_datum")){
                                                                  tMin = -2;   tMax = 2;}
            else if (tStandardName.equals("sea_surface_foundation_temperature") ||
                     tStandardName.equals("sea_surface_skin_temperature") ||
                     tStandardName.equals("sea_surface_subskin_temperature") ||
                     tStandardName.equals("sea_surface_temperature"     ) ||
                     tStandardName.equals("sea_water_potential_temperature") ||
                     tStandardName.equals("sea_water_temperature"       ) ||
                     tStandardName.equals("surface_temperature_where_sea")) {
                if (isDegreesK) {
                    tMin = 273; tMax = 305;
                } else if (isDegreesF) {
                    tMin = 32;  tMax = 89;
                } else {
                    tMin = 0;   tMax = 32;
                }}                  
            else if (tStandardName.equals("sea_water_density"           ) ||
                     tStandardName.equals("sea_water_potential_density" )) {tMin = 20;   tMax = 28;}
            else if (tStandardName.equals("sea_water_electrical_conductivity"
                                                                        )) {tMin = 30;   tMax = 40;}
            else if (tStandardName.equals("sea_water_pressure_at_sea_floor"))         {tMin = 0;    tMax = 1000;}
            else if (tStandardName.equals("sea_water_pressure_at_sea_water_surface")) {tMin = 4000; tMax = 5000;}
            else if (tStandardName.equals("sea_water_pressure"          ))            {tMin = 0;    tMax = 5000;}
            else if (tStandardName.equals("sea_surface_salinity"        ) ||
                     tStandardName.equals("sea_water_salinity"          ) ||
                     tStandardName.equals("sea_water_absolute_salinity") ||
                     tStandardName.equals("sea_water_cox_salinity") ||
                     tStandardName.equals("sea_water_knudsen_salinity") ||
                     tStandardName.equals("sea_water_practical_salinity") ||
                     tStandardName.equals("sea_water_preformed_salinity") ||
                     tStandardName.equals("sea_water_reference_salinity") ||
                     tStandardName.equals("sea_water_salinity") ||
                     //lc.indexOf(   "salinity") >= 0     || //!but river/bay salinity close to 0
                     tUnitsLC.equals("psu") ||
                     tUnitsLC.equals("pss78")  || tUnitsLC.equals("ipss78") ||
                     tUnitsLC.equals("pss-78") || tUnitsLC.equals("ipss-78") || 
                     tUnitsLC.equals("pss")    || tUnitsLC.equals("ipss")) {
                 if (tUnitsLC.equals("kg/kg") || tUnitsLC.equals("g/g")) { //rare
                     tMin = 0.032;   tMax = 0.037;
                 } else {
                     tMin = 32;   tMax = 37;
                 }
          } else if (tStandardName.equals("sea_water_speed"))              {tMin = 0;    tMax = 0.5;}
            else if (tStandardName.indexOf("sea_water_x_velocity") >= 0 ||
                     tStandardName.indexOf("sea_water_y_velocity") >= 0 ||
                     tStandardName.indexOf("sea_water_velocity") > 0)      {tMin = -0.5; tMax = 0.5;}
            else if (tStandardName.indexOf("sea_water_z_velocity") >= 0 )  {tMin = -1e-7;tMax = 1e-7;}
            else if (lcu.indexOf("sediment") >= 0 && 
                     lcu.indexOf("size") >= 0)                             {tMin = 0;    tMax = 0.01;}
            else if (lcu.indexOf("sediment") >= 0 && 
                     lcu.indexOf("density") >= 0)                          {tMin = 0;    tMax = 4000;}
            else if (lcu.indexOf("sediment") >= 0 && 
                     lcu.indexOf("velocity") >= 0)                         {tMin = 0;    tMax = 0.1;}

            else if (tStandardName.equals("soil_moisture_content"       )) {tMin = 0;    tMax = 5;}
            else if (tStandardName.equals("specific_humidity"           ) || //units=1 caught above
                     tStandardName.equals("surface_specific_humidity"   )) {
                         tMin = 0;
                         tMax = (tUnitsLC.equals("kg/kg") || tUnitsLC.equals("g/g"))?
                             0.0005 : 30;
                         }
            else if (tStandardName.equals("surface_altitude"            )) {tMin = -5;   tMax = 5;}
            else if (tStandardName.equals("surface_carbon_dioxide_mole_flux"
                                                                        )) {tMin = -1e-5;tMax = 1e-5;}
            else if (tStandardName.equals("surface_downward_x_stress") ||
                     tStandardName.equals("surface_downward_y_stress") ||
                     tStandardName.equals("surface_downward_eastward_stress") ||
                     tStandardName.equals("surface_downward_northward_stress")            
                                                                        )  {tMin = -0.5; tMax = 0.5;}
            else if (tStandardName.equals("surface_roughness_length"))     {tMin = 0;   tMax = 0.001;}
            else if (tStandardName.equals("surface_snow_mass") ||
                     tStandardName.equals("liquid_water_content_of_surface_snow"))
                                                                           {tMin = 0;   tMax = 1000;}
            else if (tStandardName.equals("surface_temperature_anomaly" )) {tMin = -3;   tMax = 3;}
            else if (tStandardName.indexOf("surface_temperature") >= 0) {
                if (tStandardName.indexOf("tendency") >= 0) {
                    tMin = -2;  tMax = 2;
                } else {
                    if (isDegreesK) {
                        tMin = 263;  tMax = 313;
                    } else if (isDegreesF) {
                        tMin = 14;   tMax = 104;                                                                             
                    } else {
                        tMin = -10;  tMax = 40;
                    }
                }}                  
            else if ((lcu.indexOf("suspended") >= 0 || lcu.indexOf("flux") >= 0) && 
                     gUnits &&
                     (lcu.indexOf("sand") >= 0 || lcu.indexOf("sediment") >= 0)) 
                                                                           {tMin = 0;    tMax = 1;}
            else if (tStandardName.indexOf("tendency_") >= 0) {
                if (tStandardName.indexOf("air_pressure")         >= 0){
                    if (tUnitsLC.equals("pa")) {tMin = -300; tMax = 300;}
                    else                       {tMin = -3;   tMax = 3;}}
                else if (tStandardName.indexOf("salinity")          >= 0){tMin = -2;   tMax = 2;}
                else if (tStandardName.indexOf("air_temperature")   >= 0){tMin = -5;   tMax = 5;}
                else if (tStandardName.indexOf("water_temperature") >= 0){tMin = -1;   tMax = 1;}
                else                                                     {tMin = -5;   tMax = 5;}

            }
            else if (lc.indexOf("tidal") >= 0 && 
                     lc.indexOf("current") >= 0 &&
                     tUnits.equals("meters second-1"))                     {tMin = 0;    tMax = 0.5;}
            else if (tStandardName.indexOf("_wave") >= 0 &&
                     tStandardName.indexOf("period") >= 0)                 {tMin = 0;    tMax = 20;}
            else if (tStandardName.indexOf("wave") >= 0 &&
                     tStandardName.indexOf("significant_height") >= 0    ) {tMin = 0;    tMax = 10;}
            else if (tStandardName.equals("eastward_wind"               ) ||
                     tStandardName.equals("northward_wind"              ) ||
                     tStandardName.equals("x_wind"                      ) ||
                     tStandardName.equals("y_wind"                      )) {tMin = -15;  tMax = 15;}
            else if (tStandardName.equals("wind_speed"                  )) {tMin = 0;    tMax = 15;}
            else if (tStandardName.equals("wind_speed_of_gust"          )) {tMin = 0;    tMax = 30;}
            else if (tStandardName.equals("visibility_in_air"           )) {tMin = 0;    tMax = 100;}
            else if (tStandardName.equals("volume_fraction_of_water_in_soil")) 
                                                                           {tMin = 0;    tMax = 200;}
            else if (lc.indexOf("|ice|concentration|") >= 0)               {tMin = 0;    tMax = 1.5;}

            //general things (if specific not caught above)
            else if (isDegreesC)                                           {if (lcu.indexOf("rate") >= 0) {
                                                                              tMin = -5;  tMax = 5;
                                                                            } else {
                                                                              tMin = -10;  tMax = 40;
                                                                            }}
            else if (isDegreesF)                                           {if (lcu.indexOf("rate") >= 0) {
                                                                              tMin = -10;  tMax = 10;
                                                                            } else {
                                                                              tMin = 14;  tMax = 104;
                                                                            }}
            else if (isDegreesK)                                           {if (lcu.indexOf("rate") >= 0) {
                                                                              tMin = -5;  tMax = 5;
                                                                            } else {
                                                                              tMin = 263;  tMax = 313;
                                                                            }}
            else if (lcu.indexOf("anom") >= 0 ||
                     lcu.indexOf("diff") >= 0                           )  {tMin = -10;  tMax = 10;}
            else if (lcu.indexOf("direction") >= 0                      )  {tMin = 0;    tMax = 360;}
            else if ((lcu.indexOf("|radiative|") >= 0 ||
                      lcu.indexOf("|radiation|") >= 0 ||
                      lcu.indexOf("|shortwave|") >= 0 ||
                      lcu.indexOf("|longwave|")  >= 0 ||
                      lcu.indexOf("|solar|")     >= 0) && 
                     (lcu.indexOf("|flux|")  >= 0 ||
                      lcu.indexOf("|fluxd|") >= 0)                      )  {tMin = -500; tMax = 500;}
            else if (lcu.indexOf("|w/m^2|") >= 0                        )  {tMin = -500; tMax = 500;}
            else if (lcu.indexOf("|heat|") >= 0 && lcu.indexOf("|flux|") >= 0
                                                                        )  {tMin = -250; tMax = 250;}
            else if (tUnits.equals(EDV.LON_UNITS)                       )  {tMin = -180; tMax = 180;}
            else if (tUnits.equals(EDV.LAT_UNITS)                       )  {tMin = -90;  tMax = 90;}
            else if (tUnits.equals("radians")                           )  {tMin = -3.2; tMax = 3.2;}
            else if (tUnits.startsWith("kg")) {
                if (tUnits.endsWith("s-1")) {tMin = 0;  tMax = 1;   } 
                else                        {tMin = 0;  tMax = 200; }}    
             
            if (reallyVerbose && 
                "|longitude|latitude|altitude|depth|time|".indexOf("|" + tStandardName + "|") < 0)
                String2.log("    assigned tMin=" + tMin + " tMax=" + tMax);

            //next best: assign based on metadata
            if (Double.isNaN(tMin) || Double.isNaN(tMax)) {
                tMin = Double.NaN;
                tMax = Double.NaN;
                PrimitiveArray pa = sourceAtts.get("actual_range");
                if (pa == null || pa.size() != 2)
                    pa = sourceAtts.get("valid_range");  //often too wide
                if (pa != null && pa.size() == 2) {
                    tMin = pa.getDouble(0);
                    tMax = pa.getDouble(1);
                } 
                if (Double.isNaN(tMin)) tMin = addAtts.getNiceDouble("valid_min"); //often too wide
                if (Double.isNaN(tMax)) tMax = addAtts.getNiceDouble("valid_max");
                if (Double.isNaN(tMin)) tMin = sourceAtts.getNiceDouble("valid_min"); //often too wide
                if (Double.isNaN(tMax)) tMax = sourceAtts.getNiceDouble("valid_max");
                if (!Double.isNaN(tMin) && !Double.isNaN(tMax)) {
                    //all of theses need scale_factor and add_offset applied
                    if (Double.isNaN(tScaleFactor))
                        tScaleFactor = 1;
                    if (Double.isNaN(tAddOffset))
                        tAddOffset = 0;
                    tMin = tMin * tScaleFactor + tAddOffset;
                    tMax = tMax * tScaleFactor + tAddOffset;
                    double d2[] = Math2.suggestLowHigh(tMin, tMax);
                    tMin = d2[0];
                    tMax = d2[1];
                }
            }

            if (Double.isNaN(tMin) || Double.isNaN(tMax)) {
                PrimitiveArray pa = sourceAtts.get("unpacked_valid_range"); //often too wide
                if (pa != null && pa.size() == 2) {
                    double d2[] = Math2.suggestLowHigh(pa.getNiceDouble(0), pa.getNiceDouble(1));
                    tMin = d2[0];
                    tMax = d2[1];
                }
            }
                      

            //???better something than nothing (so WMS service is available)? 
            //most scientific measurements choose units so numbers are easy, e.g., +/-1000
            //NO! poorly chosen values will just annoy people
            //  and Make A Graph auto-determines range if none specified
            if (Double.isNaN(tMin) && Double.isNaN(tMax) && 
                isSomething(tStandardName) &&
                !tStandardName.equals("longitude") && !tStandardName.equals("latitude") && 
                !tStandardName.equals("depth")     && !tStandardName.equals("altitude") && 
                !tStandardName.equals("time"))
                if (verbose) String2.log("Note: no colorBarMin/Max for standard_name=" + tStandardName);


            //assign if known
            if (!Double.isNaN(tMin)) addAtts.add("colorBarMinimum", tMin);
            if (!Double.isNaN(tMax)) addAtts.add("colorBarMaximum", tMax);
            if (colorBarScale != null)
                addAtts.add("colorBarScale", colorBarScale);
        }

        //if colorBarMin/Max exist, ensure min < max
        double tMin = addAtts.getDouble("colorBarMinimum");
        double tMax = addAtts.getDouble("colorBarMaximum");
        if (Double.isNaN(tMin)) tMin = sourceAtts.getDouble("colorBarMinimum");
        if (Double.isNaN(tMax)) tMax = sourceAtts.getDouble("colorBarMaximum");
        if (!Double.isNaN(tMin) &&
            !Double.isNaN(tMax)) {
            if (tMin == tMax) {
                addAtts.add("colorBarMinimum", Math2.smaller(tMin));
            } else if (tMin > tMax) {
                addAtts.add("colorBarMinimum", tMax);
                addAtts.add("colorBarMaximum", tMin);
            }
        }


        //long_name   (uses standardName)
        //note that this doesn't suggest
        //but addDummyRequiredVariableAttributesForDatasetsXml will suggest something
        //  even if there is an existing value (give admin a choice)
        if (!isSomething(tLongName)) {
            if (!isSomething(tLongName)) tLongName = sourceAtts.getString("Long_name"); //wrong case?
            //no need to set to "" since next line will always set it
            if (!isSomething(tLongName)) tLongName = EDV.suggestLongName(oLongName, tSourceName, tStandardName);
        }

        //units
        if (isSomething(tUnits)) {
            if (tUnits.equals("unitless"))
                tUnits = "null";
            if (tUnits.indexOf("%Y%m%d") < 0)
                tUnits = String2.replaceAll(tUnits, "%", "percent");
        } else {
            tUnits = sourceAtts.getString("Units");  //wrong case?
            if (!isSomething(tUnits)) tUnits = "";  //do after checking sourceAtts
        }
        tUnitsLC = tUnits.toLowerCase();

        //deal with scale_factor (e.g., 0.1) and in tUnits  (e.g., "* 10")
        if (isSomething(tUnits) && !Double.isNaN(tScaleFactor) && tScaleFactor != 0) {
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
        tUnitsLC = tUnits.toLowerCase();


        //do ioos_category last, since it uses standard_name, long_name, and units
        //fix problem in some aoml datasets
        String oIoosCat = sourceAtts.getString("ioos_category");
        if (oIoosCat != null && oIoosCat.equals("ocean_color"))
                addAtts.add("ioos_category", "Ocean Color"); 
        if (EDStatic.variablesMustHaveIoosCategory && 
            !isSomething(oIoosCat)) {
            //It is hard to be absolutely certain when assigning ioos_category.
            //Fortunately, this isn't crucial information and is used mostly for data discovery.
            //Occasional errors are okay.
            //So my goal is >98% correct (<1 out of 50 incorrect).

            //See EDV.IOOS_CATEGORIES list.

            //All ioos_category tests are in lowercase.
            //Beware of tested word within other English words, e.g., "si" (silicon) is in "since".
            //Pipes allow me to test consistently for whole words, e.g., "|si|".
            ttLongName = tLongName.toLowerCase();
            asl = ttLongName.indexOf(" at sigma level ");
            if (asl > 0)
                ttLongName = ttLongName.substring(0, asl);
            lcu = "|" +  
                ttLongName                  + "|" +
                tStandardName.toLowerCase() + "|" +
                tUnitsLC                    + "|" + 
                tSourceName.toLowerCase()   + "|"; 
            lcu = String2.replaceAll(lcu, '-', '|');
            lcu = String2.replaceAll(lcu, '_', '|');
            lcu = String2.replaceAll(lcu, '=', '|');
            lcu = String2.replaceAll(lcu, ' ', '|');
            lcu = String2.replaceAll(lcu, ',', '|');
            lcu = String2.replaceAll(lcu, '/', '|');
            //String2.log(">>ioos_category " + lcSourceName + " unitsLC=" + tUnitsLC);

            //CATCH Identifier before others
            //see similar CATCH STATISTICS         
            if (sourceAtts.getString("cf_role") != null ||
                lcu.indexOf("|identifier|") >= 0 ||
                lcu.indexOf("|id|") >= 0 ||
                "profile".equals(lcSourceName) ||
                "profileid".equals(lcSourceName) ||
                "station".equals(lcSourceName) ||
                "stationid".equals(lcSourceName) ||
                "trajectory".equals(lcSourceName) ||
                "trajectoryid".equals(lcSourceName)) {
                addAtts.add("ioos_category", "Identifier"); 

            } else if (lcu.indexOf("sigma")        >= 0 &&
                lcu.indexOf("theta")        >= 0) {
                addAtts.add("ioos_category", "Physical Oceanography"); 

            //CATCH Statistics before others
            } else if (
                (lcu.indexOf("count") >= 0 && lcu.indexOf("county") < 0) || 
                //lcu.indexOf("mean")         >= 0 || //let it be the relevant ioos_category
                //lcu.indexOf("|average|")    >= 0 || // ditto
                lcu.indexOf("stddev")       >= 0 || 
                lcu.indexOf("stderr")       >= 0 || 
                lcu.indexOf("|nbounds|")    >= 0 || 
                lcu.indexOf("|bounds|")     >= 0 || 
                lcu.indexOf("|sd|")         >= 0 || 
                lcu.indexOf("|s.d.|")       >= 0 ||
                lcu.indexOf("variance")     >= 0 ||
                lcu.indexOf("confidence")   >= 0 ||
                lcu.indexOf("precision")    >= 0 ||
                lcu.indexOf("error")        >= 0 || //"interpolation error fields"
                lcu.indexOf("number")       >= 0 || //"number of observations"
                lcu.indexOf("radius|influence|grid|points") >= 0 ||
                lcu.indexOf("standard|deviation") >= 0 ||
                lcu.indexOf("standard|error") >= 0) {
                //catch statistics first    including special cases from WOA 2001 
                //See BELOW for additional statistics (last resort)
                addAtts.add("ioos_category", "Statistics");

            } else if (lcu.indexOf("sigma")        >= 0) {
                //ambiguous   statistics or pressure
                addAtts.add("ioos_category", 
                    (lcu.indexOf("coordinate") >= 0 || lcu.indexOf("level") >= 0)?
                        "Location" :
                        "Unknown"); 

            //CATCH Quality before others
            } else if (
                //see similar CATCH QUALITY above        catch before others
                lcu.indexOf("qc")           >= 0 || 
                lcu.indexOf("qa")           >= 0 || 
                lcu.indexOf("reliability")  >= 0 || 
                lcu.indexOf("uncertainty")  >= 0 || 
                (lcu.indexOf("quality") >= 0 && lcu.indexOf("science|quality") < 0) || 
                lcu.indexOf("flag")         >= 0) { 
                addAtts.add("ioos_category", "Quality");
    
            //CATCH definitely time before others
            } else if (Calendar2.isTimeUnits(tUnitsLC)) { 
                addAtts.add("ioos_category", "Time");

            //CATCH definitely temperature before others
            } else if (hasTemperatureUnits) {  
                addAtts.add("ioos_category", "Temperature");

            //CATCH definitely Location before others
            } else if (
                tDestName.equals(EDV.LON_NAME) ||
                tDestName.equals(EDV.LAT_NAME) ||
                tDestName.equals(EDV.ALT_NAME) ||
                tDestName.equals(EDV.DEPTH_NAME) ||
                String2.indexOf(EDV.LON_UNITS_VARIANTS, tUnits) >= 0 ||
                String2.indexOf(EDV.LAT_UNITS_VARIANTS, tUnits) >= 0) {
                addAtts.add("ioos_category", "Location");            

            } else if (
                lcu.indexOf("bathym")       >= 0 ||
                lcu.indexOf("topo")         >= 0 ||
                (tSourceName.equals("z") && isMeters && 
                    "up".equals(sourceAtts.getString("positive"))) ||
                tSourceName.equals("land_binary_mask")) {
                addAtts.add("ioos_category", "Bathymetry");

            } else if (                
                lcu.indexOf("|algae")       >= 0 ||
                lcu.indexOf("birth")        >= 0 ||
                lcu.indexOf("chorion")      >= 0 ||
                lcu.indexOf("diet")         >= 0 ||
                lcu.indexOf("disease")      >= 0 ||
                lcu.indexOf("egg")          >= 0 ||
                lcu.indexOf("food")         >= 0 ||
                lcu.indexOf("larv")         >= 0 || 
                lcu.indexOf("myomere")      >= 0 ||
                lcu.indexOf("|plant")       >= 0 ||
                lcu.indexOf("|sex|")        >= 0 ||
                lcu.indexOf("|spp|")        >= 0 ||
                lcu.indexOf("stage")        >= 0 ||                
                lcu.indexOf("transpir")     >= 0 ||                
                lcu.indexOf("|veg")         >= 0 ||                
                lcu.indexOf("yolk")         >= 0 
                ) {
                addAtts.add("ioos_category", "Biology");

            } else if (lcu.indexOf("|percent") >= 0 && lcu.indexOf("|cover") >= 0 &&
                lcu.indexOf("|water") < 0 &&
                lcu.indexOf("|lake")  < 0 &&
                lcu.indexOf("|land")  < 0 &&
                lcu.indexOf("|ice|")  < 0 &&
                lcu.indexOf("|snow")  < 0) {
                addAtts.add("ioos_category", "Bottom Character");

            } else if (
                (lcu.indexOf("carbon") >= 0 && lcu.indexOf("flux") >= 0) ||
                lcu.indexOf("alkalinity")          >= 0 || 
                lcu.indexOf("co2")          >= 0 || 
                lcu.indexOf("carbonate")    >= 0 ||
                lcu.indexOf("co3")          >= 0 ||
                lcu.indexOf("carbon|dioxide")>= 0) { 
                addAtts.add("ioos_category", "CO2");

            } else if (
                lcu.indexOf("cfc11")        >= 0 ||
                lcu.indexOf("debris")       >= 0 ||
                lcu.indexOf("freon")        >= 0 ||
                lcu.indexOf("ozone")        >= 0) {
                addAtts.add("ioos_category", "Contaminants");

            } else if (
                lcu.indexOf("ammonia")      >= 0 ||
                lcu.indexOf("ammonium")     >= 0 ||
                lcu.indexOf("|n|n|")        >= 0 ||
                lcu.indexOf("nh3")          >= 0 ||
                lcu.indexOf("nh4")          >= 0 ||
                lcu.indexOf("nitrate")      >= 0 ||
                lcu.indexOf("nitrite")      >= 0 ||
                lcu.indexOf("no2")          >= 0 ||
                lcu.indexOf("no3")          >= 0 ||
                lcu.indexOf("phosphate")    >= 0 ||
                lcu.indexOf("po4")          >= 0 ||
                lcu.indexOf("silicate")     >= 0 ||
                lcu.indexOf("|si|")         >= 0) {
                addAtts.add("ioos_category", "Dissolved Nutrients");

            //Sea Level before Location and Currents so tide is caught correctly
            } else if (lcu.indexOf("wind") < 0 && 
                       lcu.indexOf("wave") < 0 && //don't catch e.g., sea surface swell wave height
               ((lcu.indexOf("geopotential") >= 0 && lcu.indexOf("height") >= 0) ||
                lcu.indexOf("ssh")                   >= 0 ||
                lcu.indexOf("surf|el")               >= 0 ||
                lcu.indexOf("|sea|height|")          >= 0 ||
                lcu.indexOf("|tide|")                >= 0 ||
                (lcu.indexOf("water") >= 0 && lcu.indexOf("level") >= 0) || 
                (lcu.indexOf("|sea|") >= 0 && lcu.indexOf("surface") >= 0 &&
                    (lcu.indexOf("elevation") >= 0 || lcu.indexOf("height") >= 0)))) { 
                addAtts.add("ioos_category", "Sea Level");

            //Currents: water or air, or things measuring them (tracer)
            } else if (lcu.indexOf("wave") < 0 &&
                       lcu.indexOf("wind") < 0 &&
                       lcu.indexOf("ship") < 0 &&
                ((lcu.indexOf("ocean") >= 0 && lcu.indexOf("streamfunction") >= 0) ||
                 lcu.indexOf("momentum|component") >= 0 ||
                 lcu.indexOf("momentum|stress")    >= 0 ||
                 lcu.indexOf("|u-flux|")           >= 0 ||
                 lcu.indexOf("|v-flux|")           >= 0 ||
                 lcu.indexOf("tracer")             >= 0 ||
                 lcu.indexOf("current")            >= 0 ||
                 lcu.indexOf("water|dir")          >= 0 ||
                 lcu.indexOf("direction")          >= 0 || 
                 lcu.indexOf("speed")              >= 0 || 
                 lcu.indexOf("spd")                >= 0 || 
                 lcu.indexOf("|vel")               >= 0 ||  //not "level"
                 lcu.indexOf("velocity")           >= 0)) { 
                addAtts.add("ioos_category", "Currents");

            } else if (
                (lcu.indexOf("o2") >= 0 && lcu.indexOf("co2") < 0) || //no2 was caught above
                lcu.indexOf("oxygen")       >= 0) { 
                addAtts.add("ioos_category", "Dissolved O2");

            } else if (
                lcu.indexOf("predator")     >= 0 ||
                lcu.indexOf("prey")         >= 0 ||
                lcu.indexOf("|troph")       >= 0) {  //don't catch geostrophic
                addAtts.add("ioos_category", "Ecology");

            } else if (
               ((lcu.indexOf("heat") >= 0 || 
                 lcu.indexOf("radiative") >= 0 ||
                 lcu.indexOf("radiation") >= 0 || 
                 lcu.indexOf("solar") >= 0 ||
                 lcu.indexOf("temperature") >= 0) && 
                 (lcu.indexOf("transport") >= 0 || lcu.indexOf("flux") >= 0 || lcu.indexOf("flx") >= 0)) ||
                (lcu.indexOf("solar") >= 0 && (lcu.indexOf("irradiance") >= 0 || lcu.indexOf("reflectance") >= 0)) ||
                lcu.indexOf("shortwave")      >= 0 ||  //"heat flux" not ideal; "radiant energy"?
                lcu.indexOf("longwave")       >= 0 ||  //"heat flux" not ideal; "radiant energy"?
                lcu.indexOf("hflx")           >= 0 ||
                lcu.indexOf("lflx")           >= 0 ||
                lcu.indexOf("sflx")           >= 0 ||
                lcu.indexOf("|cape|")         >= 0 || //convective available potential energy
                lcu.indexOf("|cin|")          >= 0) { //convective inhibition
                addAtts.add("ioos_category", "Heat Flux");

            //see Hydrology below

            } else if (
                lcu.indexOf("|ice")         >= 0 ||
                lcu.indexOf("|snow")        >= 0) {
                addAtts.add("ioos_category", "Ice Distribution");

            } else if (
                lcu.indexOf("|mask|")       >= 0 ||
                lcu.indexOf("|id|")         >= 0 ||
                lcu.indexOf("site|id")      >= 0 ||
                lcu.indexOf("station|id")   >= 0 ||
                lcu.indexOf("stationid")    >= 0 ||
                lcu.indexOf("|pi|")         >= 0 ||
                lcu.indexOf("|project|")    >= 0) { 
                addAtts.add("ioos_category", "Identifier");

            //see Location below

            } else if (
                lcu.indexOf("cldc")         >= 0 ||  //cloud cover
                lcu.indexOf("cloud")        >= 0 ||
                lcu.indexOf("cloud")        >= 0 ||
                lcu.indexOf("dew point")    >= 0 ||
                lcu.indexOf("dewp")         >= 0 ||
                lcu.indexOf("evapora")      >= 0 ||
               (lcu.indexOf("front") >= 0 && lcu.indexOf("probability") >= 0) ||
                lcu.indexOf("humidity")     >= 0 ||
                lcu.indexOf("precip")       >= 0 ||  //precipitable precipitation
                lcu.indexOf("|rain")        >= 0 ||  //not "grain"
                lcu.indexOf("rhum")         >= 0 ||
                lcu.indexOf("|shum|")       >= 0 ||
                lcu.indexOf("|storm|")      >= 0 ||
                lcu.indexOf("total electron content") >= 0 ||
                lcu.indexOf("|water|condensate|") >= 0 ||
                lcu.indexOf("|water|vapor|") >= 0 ||
                lcu.indexOf("visi")         >= 0) {
                addAtts.add("ioos_category", "Meteorology");

            } else if (
                lcu.indexOf("chlor")              >= 0 ||
                lcu.indexOf("chla")               >= 0 || 
                lcu.indexOf("chl|a|")             >= 0 || 
                lcu.indexOf("k490")               >= 0 ||
                lcu.indexOf("kd490")              >= 0 ||
                lcu.indexOf("|pic|")              >= 0 ||
                lcu.indexOf("|inorganic|carbon|") >= 0 ||
                lcu.indexOf("|poc|")              >= 0 ||
                lcu.indexOf("|organic|carbon|")   >= 0 ||
                lcu.indexOf("dissolved|organic|material") >= 0) {
                addAtts.add("ioos_category", "Ocean Color");

            } else if (
                lcu.indexOf("aerosol")      >= 0 ||
                lcu.indexOf("optical")      >= 0 ||
                lcu.indexOf("albedo")       >= 0 ||
                lcu.indexOf("|rrs")         >= 0 ||
                lcu.indexOf("667")          >= 0 ||
                lcu.indexOf("fluor")        >= 0 ||
                lcu.indexOf("|par|")        >= 0 ||
                lcu.indexOf("|photosynthetically|available|radiation|") >= 0 ||
                lcu.indexOf("|photosynthetically|active|radiation|")    >= 0 ||
                lcu.indexOf("|wavelength|") >= 0 ||
                lcu.indexOf("reflectance")  >= 0 ||
                lcu.indexOf("|transmissi")  >= 0 || //vity 
                lcu.indexOf("|attenuation") >= 0 || 
                lcu.indexOf("|olr|")  >= 0 ||
                ((lcu.indexOf("|radiative|") >= 0 ||
                  lcu.indexOf("|radiation|") >= 0 ||
                  lcu.indexOf("|shortwave|") >= 0 ||
                  lcu.indexOf("|longwave|")  >= 0 ||
                  lcu.indexOf("|solar|")     >= 0) && 
                 (lcu.indexOf("|flux|")  >= 0 ||
                  lcu.indexOf("|fluxd|") >= 0))  ||
                lcu.indexOf("|w/m^2|")      >= 0 ||
                tSourceName.toLowerCase().equals("graphics")) {
                addAtts.add("ioos_category", "Optical Properties");

            //Physical Oceanography, see below

            } else if (
                //??? add/distinguish Phytoplankton Abundance ???
                lcu.indexOf("phytoplankton") >= 0) {  //not a great test
                addAtts.add("ioos_category", "Phytoplankton Species"); 

            } else if (
                lcu.indexOf("aprs")         >= 0 || //4 letter NDBC abbreviations
                lcu.indexOf("ptdy")         >= 0 ||
                lcu.indexOf("pressure")     >= 0 ||
                //lcu.indexOf("sigma")        >= 0 ||  //but caught above specially
                lcu.indexOf("|mbar|")       >= 0 ||
                lcu.indexOf("|millibar|")   >= 0 ||
                lcu.indexOf("|hpa|")        >= 0) {
                addAtts.add("ioos_category", "Pressure");

            } else if (
                lcu.indexOf("productivity") >= 0 || 
                lcu.indexOf("prim|prod")    >= 0 || 
                lcu.indexOf("primprod")     >= 0) { 
                addAtts.add("ioos_category", "Productivity");

            //see Quality above

            } else if (
                lcu.indexOf("|ph|")         >= 0 ||  //borderline
                lcu.indexOf("pss")          >= 0 ||
                (lcu.indexOf("psu")         >= 0 && lcu.indexOf("psue") < 0) || //not "psuedo"
                lcu.indexOf("salinity")     >= 0 ||
                lcu.indexOf("salt")         >= 0 ||
                lcu.indexOf("conductivity") >= 0 ||
                lcu.indexOf("|sea|water|density|") >= 0) {
                addAtts.add("ioos_category", "Salinity");

            //see Sea Level above

            } else if (
                lcu.indexOf("soil")         >= 0) { 
                addAtts.add("ioos_category", "Soils");
    

            //see Statistics above

            } else if (
                (lcu.indexOf("surf") >= 0 && lcu.indexOf("roughness") >= 0) || //surface
                lcu.indexOf("awpd")         >= 0 || //4 letter NDBC abbreviations
                lcu.indexOf("dwpd")         >= 0 ||
                lcu.indexOf("mwvd")         >= 0 ||
                lcu.indexOf("wvht")         >= 0 ||
                (lcu.indexOf("wave") >= 0 && lcu.indexOf("spectral") < 0 &&
                    lcu.indexOf("wavelength") < 0 &&
                    lcu.indexOf("short") < 0 && lcu.indexOf("long") < 0)) { 
                addAtts.add("ioos_category", "Surface Waves");
            
            } else if (
                lcu.indexOf("phylum")       >= 0 ||
                lcu.indexOf("order")        >= 0 ||
                lcu.indexOf("family")       >= 0 ||
                lcu.indexOf("genus")        >= 0 ||
                lcu.indexOf("genera")       >= 0 ||
                lcu.indexOf("species")      >= 0 ||
                lcu.indexOf("sp.")          >= 0 ||
                lcu.indexOf("spp")          >= 0 ||
                lcu.indexOf("stock")        >= 0 ||
                lcu.indexOf("taxa")         >= 0 ||
                lcu.indexOf("scientific")   >= 0 ||
                lcu.indexOf("vernacular")   >= 0 ||
                lcu.indexOf("commonname")   >= 0) { 
                addAtts.add("ioos_category", "Taxonomy");

            } else if (
                lcu.indexOf("airtemp")              >= 0 ||
                lcu.indexOf("air|temp")             >= 0 ||
                lcu.indexOf("atemp")                >= 0 || 
                lcu.indexOf("atmp")                 >= 0 ||  //4 letter NDBC abbreviation 
                lcu.indexOf("ztmp")                 >= 0 ||  
                lcu.indexOf("|degree|c|")           >= 0 ||
                lcu.indexOf("|degrees|c|")          >= 0 ||
                lcu.indexOf("heating")              >= 0 ||
                lcu.indexOf("sst")                  >= 0 ||
                lcu.indexOf("temperature")          >= 0 ||
                lcu.indexOf("wtmp")                 >= 0 ||  //4 letter NDBC abbreviation
                lcu.indexOf("wtemp")                >= 0 ||  
                lcu.indexOf("temp.")                >= 0 ||  
                //temperature units often used with other units for other purposes
                //but if alone, it means temperature
                hasTemperatureUnits) {  //also caught above before others

                addAtts.add("ioos_category", "Temperature");


            } else if (
               ((lcu.indexOf("atmosphere")     >= 0 || lcu.indexOf("air")    >= 0) &&
                (lcu.indexOf("streamfunction") >= 0 || lcu.indexOf("stress") >= 0)) ||
                lcu.indexOf("momentum|flux")>= 0 ||
                lcu.indexOf("|u-flux|")     >= 0 ||
                lcu.indexOf("|v-flux|")     >= 0 ||
                lcu.indexOf("gust")         >= 0 ||
                lcu.indexOf("uwnd")         >= 0 ||
                lcu.indexOf("vwnd")         >= 0 ||
                lcu.indexOf("xwnd")         >= 0 ||
                lcu.indexOf("ywnd")         >= 0 ||
                lcu.indexOf("wdir")         >= 0 || //4 letter NDBC abbreviations
                lcu.indexOf("wspd")         >= 0 ||
                lcu.indexOf("wgst")         >= 0 ||
                lcu.indexOf("wspu")         >= 0 ||
                lcu.indexOf("wspv")         >= 0 ||
                lcu.indexOf("wind")         >= 0) { 
                addAtts.add("ioos_category", "Wind");
           
            } else if (
                //Physical Oceanography here to catch "stress" other than wind
                //this can be very inclusive
                lcu.indexOf("stress")       >= 0 ||             
                lcu.indexOf("density")      >= 0 ||             
                lcu.indexOf("erosion")      >= 0 ||
                lcu.indexOf("|sand|")       >= 0 ||
                lcu.indexOf("sediment")     >= 0 ||
                lcu.indexOf("roughness")    >= 0 ||
                lcu.indexOf("tide")         >= 0 ||
                lcu.indexOf("tidal")        >= 0 ||
                lcu.indexOf("mixed|layer")  >= 0) {
                addAtts.add("ioos_category", "Physical Oceanography");
            
            } else if (
                lcu.indexOf("zooplankton") >= 0) {  //not a great test
                addAtts.add("ioos_category", "Zooplankton Abundance");

            //Hydrology near end, so likely to catch other categories first (e.g., Temperature)
            } else if (
                lcu.indexOf("runoff")         >= 0 ||
                lcu.indexOf("water|flux|into|ocean") >= 0 ||
                (lcu.indexOf("stream") >= 0 && lcu.indexOf("flow") >= 0) ||
                (lcu.indexOf("surface") >= 0 && lcu.indexOf("water") >= 0)) {
                addAtts.add("ioos_category", "Hydrology");

            //catch time near end
            //let other things be caught above, e.g., wind in m/s, temperature change/day
            } else if (
                //Calendar2.isTimeUnits(tUnitsLC) || //see above: caught before others
                lcu.indexOf("|age|")        >= 0 ||
                lcu.indexOf("|calendar|")   >= 0 ||
                lcu.indexOf("|date")        >= 0 ||
                lcu.indexOf("|day")         >= 0 ||   //not "someday"
                lcu.indexOf("|hour")        >= 0 ||
                lcu.indexOf("|minute")      >= 0 ||
                lcu.indexOf("|month")       >= 0 ||
                //lcu.indexOf("|s|")          >= 0 || //too likely something else   .../s
                lcu.indexOf("|second|")     >= 0 || 
                lcu.indexOf("|seconds|")    >= 0 ||
                (lcu.indexOf("|time") >= 0 && lcu.indexOf("|time-averaged") < 0) ||
                lcu.indexOf("|year")        >= 0) {
                addAtts.add("ioos_category", "Time");

            //catch Quality at end if not caught above
            } else if (
                lcu.indexOf("bits")         >= 0) { //eg flag bits
                addAtts.add("ioos_category", "Quality");

            //catch Location last   so e.g., ocean_salt_x_transport caught by Salinity
            //some Location caught above before others 
            } else if (
                lcu.indexOf("altitude")     >= 0 ||
                lcu.indexOf("elevation")    >= 0 ||
                (lcu.indexOf("depth")       >= 0 && 
                 lcu.indexOf("integral")    <  0) ||
                lcu.indexOf("geox")         >= 0 || 
                lcu.indexOf("geoy")         >= 0 || 
                (lcu.indexOf("|level|") >= 0 && 
                   (lcu.indexOf("|m|") >= 0       || lcu.indexOf("|meter|") >= 0 || 
                    lcu.indexOf("|meters|") >= 0  || lcu.indexOf("|cm|") >= 0)) ||
                (lcu.indexOf("|plev|") >= 0 && lcu.indexOf("|atm|") >= 0) ||
                lcu.indexOf("|lon|")        >= 0 ||
                lcu.indexOf("|tlon|")       >= 0 ||
                lcu.indexOf("|vlon|")       >= 0 ||
                lcu.indexOf("longitude")    >= 0 ||
                lcu.indexOf("|lat|")        >= 0 ||
                lcu.indexOf("|tlat|")       >= 0 ||
                lcu.indexOf("|vlat|")       >= 0 ||
                lcu.indexOf("latitude")     >= 0 ||
                lcu.indexOf("|x|")          >= 0 || 
                lcu.indexOf("xax")          >= 0 || //x axis
                lcu.indexOf("|xpos|")       >= 0 || 
                lcu.indexOf("|y|")          >= 0 ||
                lcu.indexOf("yax")          >= 0 || //y axis
                lcu.indexOf("|ypos|")       >= 0 || 
                lcu.indexOf("|z|")          >= 0 || 
                lcu.indexOf("zax")          >= 0 || //z axis
                lcu.indexOf("|zpos|")       >= 0 || 
                lcu.indexOf("zlev")         >= 0 ||
                lcu.indexOf("|cs|w|")       >= 0 ||
                lcu.indexOf("|eta|rho|")    >= 0 ||
                lcu.indexOf("|eta|u|")      >= 0 ||
                lcu.indexOf("|eta|v|")      >= 0 ||
                lcu.indexOf("|s|rho|")      >= 0 ||
                lcu.indexOf("|s|w|")        >= 0 ||
                lcu.indexOf("|xi|rho|")     >= 0 ||
                lcu.indexOf("|xi|u|")       >= 0 ||
                lcu.indexOf("|xi|v|")       >= 0 ||
                lcu.indexOf("|nsites|")     >= 0 || 
                lcu.indexOf("|srs|")        >= 0 ||
                lcu.indexOf("|datum|")      >= 0 ||
                lcu.indexOf("|vertdatum|")  >= 0 ||
                lcu.indexOf("location")     >= 0 ||
                lcu.indexOf("locality")     >= 0 ||
                lcu.indexOf("|region|")     >= 0 ||
                lcu.indexOf("|sites|")      >= 0 ||
                lcu.indexOf("|city|")       >= 0 ||
                lcu.indexOf("|county|")     >= 0 ||
                lcu.indexOf("|province|")   >= 0 ||
                lcu.indexOf("|state|")      >= 0 ||
                lcu.indexOf("|zip|")        >= 0 ||
                lcu.indexOf("|country|")    >= 0 ||
                lcu.indexOf("|fips")        >= 0) {
                addAtts.add("ioos_category", "Location");            

            } else if (
                //last resort statistics    (catch things not caught above)
                lcu.indexOf("|average|")    >= 0 || 
                lcu.indexOf("|mean|")       >= 0 || 
                lcu.indexOf("|nav|")        >= 0 || 
                lcu.indexOf("|ngrids|")     >= 0 || 
                lcu.indexOf("|nmodels|")    >= 0 || 
                lcu.indexOf("|nuser|")      >= 0 || 
                lcu.indexOf("|nx|")         >= 0 || 
                lcu.indexOf("|ny|")         >= 0 || 
                lcu.indexOf("|nv|")         >= 0 || 
                lcu.indexOf("|n|")          >= 0) {
                //See ABOVE for additional statistics 
                addAtts.add("ioos_category", "Statistics");

            } else {
                if (reallyVerbose || !lcu.equals("|nbnds|||nbnds|")) 
                    String2.log("    ioos_category=Unknown for " + lcu);
                addAtts.add("ioos_category", "Unknown");
            }        
        }

        //add to addAtts if changed
        if (isSomething(tUnits)       && !tUnits.equals(oUnits))               addAtts.add("units",         tUnits);
        if (isSomething(tLongName)    && !tLongName.equals(oLongName))         addAtts.add("long_name",     tLongName);
        if (isSomething(tStandardName)&& !tStandardName.equals(oStandardName)) addAtts.add("standard_name", tStandardName);

        value = addAtts.getString("units");
        if (value == null)
            value = sourceAtts.getString("units");
        if (isSomething(value)) {
            if (value.startsWith("deg-")) //some podaac datasets have this
                addAtts.set("units", "deg_" + value.substring(4));
            if (value.toLowerCase().equals("n/a"))
                addAtts.set("units", "null");
        }

        return addAtts;
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
        con = String2.replaceAll(con, "/",", ");
        if (con.indexOf("COARDS") < 0) 
            con += ", COARDS";
        if (con.indexOf("CF") < 0) 
            con += ", CF-1.6";
        else {
            con = String2.replaceAll(con, "CF-1.00","CF-1.6");
            con = String2.replaceAll(con, "CF-1.0", "CF-1.6");
            con = String2.replaceAll(con, "CF-1.1", "CF-1.6");
            con = String2.replaceAll(con, "CF-1.2", "CF-1.6");
            con = String2.replaceAll(con, "CF-1.3", "CF-1.6");
            con = String2.replaceAll(con, "CF-1.4", "CF-1.6");
            con = String2.replaceAll(con, "CF-1.5", "CF-1.6");
        }
        con = String2.replaceAll(con, "Unidata Dataset Discovery v1.0", "ACDD-1.3");
        if (con.indexOf("ACDD") < 0) 
            con += ", ACDD-1.3";
        else {
            con = String2.replaceAll(con, "ACDD-1.0", "ACDD-1.3");
            con = String2.replaceAll(con, "ACDD-1.1", "ACDD-1.3");
            con = String2.replaceAll(con, "ACDD-1.2", "ACDD-1.3");
        }
        con = String2.replaceAll(con, "CWHDF, ","");
        con = String2.replaceAll(con, ", CWHDF","");
        if (con.startsWith(", "))
            con = con.substring(2);
        if (con.endsWith(", "))
            con = con.substring(0, con.length() - 2);
        return con;
    }

    /**
     * This is used by subclass's generateDatasetsXml methods to suggest
     * a datasetID.    
     * <br>This seeks to be short, descriptive, and unique (so 2 datasets don't have same datasetID).
     *
     * @param tPublicSourceUrl a real URL (starts with "http", e.g., http://oceanwatch.pfeg.noaa.gov/...), 
     *   a fileDirectory (with trailing '/') or directory+fileName (may be a fileNameRegex), 
     *   or a fake fileDirectory (not ideal).
     *   <br>If an OPeNDAP url, it is without the .das, .dds, or .html extension.
     *   <br>If a fileDirectory, the two rightmost directories are important.
     *   <br>If you want to add additional information (e.g. dimension names or "EDDTableFromFileNames"), 
     *     add it add the end of the url.
     * @return a suggested datasetID, e.g., noaa_pfeg#########
     */
    public static String suggestDatasetID(String tPublicSourceUrl) {
        //???alternative: use String2.modifyToBeFileNameSafe to convert end to, e.g.,
        //   satellite_MH_chla_5day.
        //But some datasetIDs would be very long and info is already in sourceUrl in original form.

        //extract from tPublicSourceUrl
        //is it an Amazon AWS S3 URL?
        String dsi = String2.getAwsS3BucketName(tPublicSourceUrl);
        if (dsi == null) {
            //regular url
            String dir = tPublicSourceUrl.indexOf('/' ) >= 0 ||
                         tPublicSourceUrl.indexOf('\\') >= 0?
                File2.getDirectory(tPublicSourceUrl) :
                tPublicSourceUrl;
            dsi = String2.toSVString(suggestInstitutionParts(dir), "_", true);
        } else {
            //AWS S3 url
            dsi = "s3" + dsi + "_";
        }
        dsi = String2.modifyToBeFileNameSafe(dsi);
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
"   altitude, depth, and time variables).\n" +
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
     * @param tryToFindLLAT if true, this tries to catch and rename variables
     *    to longitude, latitude, altitude, depth, and time.
     *    <br>This should be true for tabular dataVariables and grid axis variables.
     *    <br>This should be false for grid data variables.
     * @param questionDestinationName if true, the destinationName is preceded by "???"
     *    if it is different from sourceName.
     * @throws Throwable if trouble
     */
    public static String writeVariablesForDatasetsXml(Table sourceTable, Table addTable,
        String variableType, boolean includeDataType, 
        boolean tryToFindLLAT, boolean questionDestinationName) throws Throwable {

        if (sourceTable != null)
            Test.ensureEqual(sourceTable.nColumns(), addTable.nColumns(),
                "The number of columns in sourceTable and addTable isn't equal!");
        String indent = "    ";
        StringBuilder sb = new StringBuilder();
        if (tryToFindLLAT)
            tryToFindLLAT(sourceTable, addTable);

        //e.g., don't change "lon" to "longitude" if there is already a "longitude" variable
        int sLongitude  = addTable.findColumnNumber("longitude"), 
            sLatitude   = addTable.findColumnNumber("latitude"), 
            sAltitude   = addTable.findColumnNumber("altitude"), //but just one of altitude or depth
            sDepth      = addTable.findColumnNumber("depth"),    //but just one of altitude or depth
            sTime       = addTable.findColumnNumber("time");

        //ensure time has proper units
        if (sTime >= 0) {
            Attributes addAtts = addTable.columnAttributes(sTime);
            String tUnits = addAtts.getString("units");  
            if (tUnits == null && sourceTable != null) 
                tUnits = sourceTable.columnAttributes(sTime).getString("units");
            if (!Calendar2.isTimeUnits(tUnits)) {
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
            String tPositive = addAtts.getString("positive");  
            if (tPositive == null) 
                tPositive = sourceAtts.getString("positive");
            float tScaleFactor = sourceAtts.getFloat("scale_factor");
            String suggestDestName = suggestDestinationName(tSourceName, tUnits, 
                tPositive, tScaleFactor, tryToFindLLAT); 
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

            } else if (col == sAltitude ||
                (sAltitude < 0 && sDepth < 0 && suggestDestName.equals("altitude"))) {
                tDestName = "altitude"; 
                sAltitude = col; //no other column will be altitude

            } else if (col == sDepth ||
                (sAltitude < 0 && sDepth < 0 && suggestDestName.equals("depth"))) {
                tDestName = "depth"; 
                sDepth = col; //no other column will be depth

            } else if ((col == sTime || sTime < 0) && suggestDestName.equals("time")) {
                //above test deals ensures that "time" var is either
                //  already called "time"  and has proper units
                //  or if no sourceName is "time" and this has proper units.
                //See suggestDestinationName comments regarding time.
                tDestName = "time"; 
                //addAtts.set("long_name", "Time");
                sTime = col; //no other column will be time

        //*** deal with duplicate names
            } else if (sLongitude >= 0 && suggestDestName.equals("longitude")) {
                //longitude already assigned
                suggestDestName = suggestDestinationName(tSourceName, tUnits, tPositive,
                    tScaleFactor, false); //tryToFindLLAT
                tDestName = suggestDestName;
                if (tDestName.equals("longitude"))
                    tDestName = "longitude2";

            } else if (sLatitude >= 0 && suggestDestName.equals("latitude")) {
                //latitude already assigned
                suggestDestName = suggestDestinationName(tSourceName, tUnits, tPositive,
                    tScaleFactor, false); //tryToFindLLAT
                tDestName = suggestDestName;
                if (tDestName.equals("latitude"))
                    tDestName = "latitude2";

            } else if ((sAltitude >= 0 || sDepth >= 0) && suggestDestName.equals("altitude")) {
                //altitude already assigned
                suggestDestName = suggestDestinationName(tSourceName, tUnits, tPositive,
                    tScaleFactor, false); //tryToFindLLAT
                tDestName = suggestDestName;
                if (tDestName.equals("altitude"))
                    tDestName = "altitude2";

            } else if ((sAltitude >= 0 || sDepth >= 0) && suggestDestName.equals("depth")) {
                //depth already assigned
                suggestDestName = suggestDestinationName(tSourceName, tUnits, tPositive,
                    tScaleFactor, false); //tryToFindLLAT
                tDestName = suggestDestName;
                if (tDestName.equals("depth"))
                    tDestName = "depth2";

            } else if (sTime >= 0 && suggestDestName.equals("time")) {
                //time already assigned
                suggestDestName = suggestDestinationName(tSourceName, tUnits, tPositive,
                    tScaleFactor, false); //tryToFindLLAT
                tDestName = suggestDestName;
                if (tDestName.equals("time"))
                    tDestName = "time2";

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
     * @param tPositive the value of the "positive" attribute (e.g., up or down, may be null)
     * @param tScaleFactor from the "scale_factor" attribute, or NaN.
     *    This is used to look for the inverse of the scale_factor in the 
     *    variable name, e.g, "* 1000", and remove it.
     * @param tryToFindLLAT if true, this tries to catch and rename variables
     *    to longitude, latitude, altitude, depth, and time.
     *    <br>This should be true for tabular dataVariables and grid axis variables.
     *    <br>This should be false for grid data variables.
     * @return the suggested destinationName (which may be the same).
     */
    public static String suggestDestinationName(String tSourceName, String tUnits, 
        String tPositive, float tScaleFactor, boolean tryToFindLLAT) {

        //remove (units) from SOS sourceNames, e.g., "name (units)"
        String oSourceName = tSourceName;
        int po = tSourceName.indexOf(" (");
        if (po > 0)
            tSourceName = tSourceName.substring(0, po);

        //if from readXml (e.g., .../.../aaas:time), be brave and just use last part of the name
        int slashPo = tSourceName.lastIndexOf('/');
        if (slashPo >= 0 && slashPo < tSourceName.length() - 1) { //not the last char
            int slashPo1 = tSourceName.indexOf('/');
            if (slashPo1 != slashPo) {
                tSourceName = tSourceName.substring(slashPo + 1);
 
                //and it probably has unnecessary prefix:
                int colonPo = tSourceName.lastIndexOf(':');
                if (colonPo >= 0 && colonPo < tSourceName.length() - 1) { //not the last char
                    tSourceName = tSourceName.substring(colonPo + 1);
                }
            }
        }

        String lcSourceName = tSourceName.toLowerCase();

        //just look at suggested units
        if (tUnits == null) tUnits = "";
        po = tUnits.indexOf("???");
        if (po >= 0)
            tUnits = tUnits.substring(po + 3);  
        String tUnitsLC = tUnits.toLowerCase();
        boolean unitsAreMeters = String2.indexOf(EDV.METERS_VARIANTS, tUnitsLC) >= 0; //case sensitive

        if (tPositive == null) 
            tPositive = "";
        tPositive = tPositive.toLowerCase();

        if (tryToFindLLAT) {
            if ((lcSourceName.indexOf("lon") >= 0 ||
                  lcSourceName.equals("x") ||
                  lcSourceName.equals("xax")) &&  //must check, since uCurrent and uWind use degrees_east, too
                 (String2.caseInsensitiveIndexOf(EDV.LON_UNITS_VARIANTS, tUnitsLC) >= 0 ||    
                  tUnitsLC.equals("degrees_west") || //bizarre, but sometimes used for postive or negative degrees_east values
                  tUnitsLC.equals("degree_west") ||
                  tUnitsLC.equals("degrees") ||
                  tUnitsLC.equals("degree"))) 

                return "longitude"; 
                 
            if ((lcSourceName.indexOf("lat") >= 0 ||
                  lcSourceName.equals("y") ||
                  lcSourceName.equals("yax")) &&  
                 (String2.caseInsensitiveIndexOf(EDV.LAT_UNITS_VARIANTS, tUnitsLC) >= 0 ||
                  tUnitsLC.equals("degrees") ||
                  tUnitsLC.equals("degree"))) 
     
                return "latitude"; 

            if (lcSourceName.equals("altitude") ||  //stricter than lat and lon
                (lcSourceName.equals("elevation") && unitsAreMeters) ||
                (tPositive.equals("up") && lcSourceName.indexOf("_above_ground") < 0 && unitsAreMeters))      
                return "altitude"; 

            if (lcSourceName.equals("depth") ||     //stricter than lat and lon
                (tPositive.equals("down") && unitsAreMeters)) 
     
                return "depth"; 

            if (tUnitsLC.indexOf(" since ") > 0) { //simple test; definitive test is below
                try {
                    Calendar2.getTimeBaseAndFactor(tUnits); //just to throw exception if trouble
                    return "time"; 
                } catch (Exception e) {
                    String2.log("Unexpected failure when checking validity of possible time var's units=\"" + 
                        tUnits + "\":\n" + 
                        MustBe.throwableToString(e));
                }
            }

            //see Calendar2.suggestDateTimeFormat for common Joda date time formats
            if (tUnitsLC.indexOf("yy") >= 0) 
                return "time"; 

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
        while (tDestName.length() > 0 && "*_".indexOf(tDestName.charAt(tDestName.length() - 1)) >= 0) 
            tDestName = tDestName.substring(0, tDestName.length() - 1).trim();
        if (tDestName.length() == 0)
            tDestName = "a";

        //shorten the name?
        //special case
        String seek = "aasg_";
        po = -1;
        if (tDestName.length() > 6)
            po = tDestName.substring(0, tDestName.length() - 1).lastIndexOf(seek);
        //NOT YET. Most sourceNames aren't too long. aasg is the only known exception.
        //look for last '_', but not at very end
        //  and avoid e.g., several something_quality -> quality
        //if (po < 0 && tDestName.length() > 20) 
        //    po = tDestName.substring(0, tDestName.length() - 8).lastIndexOf(seek = "_");
        if (po >= 0)
            tDestName = tDestName.substring(po + seek.length());

        //String2.log(">> suggestDestinationName orig=" + oSourceName + " tSource=" + tSourceName + " dest=" + tDestName);
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
     * @param addAtts
     * @param indent a string of spaces
     * @throws Throwable if trouble
     */
    public static String writeAttsForDatasetsXml(boolean isAddAtts, 
        Attributes addAtts, String indent) throws Throwable {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + 
            (isAddAtts? "<addAttributes>\n" : "<!-- sourceAttributes>\n"));
        String names[] = addAtts.getNames();
        for (int att = 0; att < names.length; att++) {
            PrimitiveArray attPa = addAtts.get(names[att]);
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
        String add = Calendar2.getCurrentISODateTimeStringZulu() +
            "Z " + text;
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
        return new ConcurrentHashMap(16, 0.75f, 4);
    }

    /** The name of the badFileMap file. */
    public String badFileMapFileName() {
        return datasetDir() + BADFILE_TABLE_FILENAME;
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
            String subject = "Error while reading table of badFiles";
            String content = fileName + "\n" + 
                MustBe.throwableToString(t);  
            String2.log(subject + ":\n" + content);
            EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
            File2.delete(fileName);
            return newEmptyBadFileMap();
        }
    }

    /**
     * This makes a badFile table from a thread-safe ConcurrentHashMap  
     * (key=dir#/fileName, value=Object[0=(Double)lastMod, 1=(String)reason]).
     * and writes it to disk.
     * <br>If the file can't be written, an email is sent to emailEverythingToCsv.
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
            String content = randomFileName + "\n" + 
                MustBe.throwableToString(t);  
            String2.log(subject + ":\n" + content);
            EDStatic.email(EDStatic.emailEverythingToCsv, subject, content);
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
     * @param lastMod   the lastModified time (millis) of the file 
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
     * @param lastMod   the lastModified time (millis) of the file
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
     * This is used by EDDGridFromFiles and EDDTableFromFiles to 
     * find a file from the fileTable (via linear search).
     *
     * @return the row number if it was in the fileTable (else -1)
     */     
    public static int findInFileTable(int dirIndex, String fileName, 
        Table tFileTable, ShortArray ftDirIndex, StringArray ftFileList) {

        int fileListPo = 0;
        int nFiles = ftDirIndex.size();
        while (fileListPo < nFiles) {
            if (dirIndex == ftDirIndex.get(fileListPo) && 
                fileName.equals(ftFileList.get(fileListPo))) {
                return fileListPo;
            }
            fileListPo++;
        }
        return -1;
    }

    /** 
     * This is used by EDDGridFromFiles and EDDTableFromFiles to remove
     * a file from the fileTable (via linear search).
     *
     * @return true if it was in the fileTable and thus was removed
     */     
    public static boolean removeFromFileTable(int dirIndex, String fileName, 
        Table tFileTable, ShortArray ftDirIndex, StringArray ftFileList) {

        int fileListPo = findInFileTable(dirIndex, fileName, 
            tFileTable, ftDirIndex, ftFileList);
        if (fileListPo >= 0) {
            tFileTable.removeRow(fileListPo);
            return true;
        } else {
            return false;
        }
    }

    /** 
     * This is used by EDDGridFromFiles and EDDTableFromFiles to save 
     * all the file information to disk.
     *
     * @throws Throwable if trouble
     */
    public void saveDirTableFileTableBadFiles(Table dirTable, Table fileTable, 
        ConcurrentHashMap badFileMap) throws Throwable {

        String dirTableFileName  = datasetDir() +  DIR_TABLE_FILENAME;
        String fileTableFileName = datasetDir() + FILE_TABLE_FILENAME;
        String badFilesFileName  = badFileMapFileName();
        int random = Math2.random(Integer.MAX_VALUE);

        try {
            //*** It is important that the 3 files are swapped into place as atomically as possible
            //So save all first, then rename all.
            dirTable.saveAsFlatNc(  dirTableFileName + random, "row"); //throws exceptions
            fileTable.saveAsFlatNc(fileTableFileName + random, "row"); //throws exceptions
            if (!badFileMap.isEmpty()) //only create badMapFile if there are some bad files
                writeBadFileMap(    badFilesFileName + random, badFileMap);
            //if Windows, give OS file system time to settle
            if (String2.OSIsWindows) Math2.gc(1000); //so things below go quickly
            
            //Integrity of these files is important. Rename is less likely to have error.
            if (badFileMap.isEmpty())
                File2.delete(badFilesFileName);
            else File2.rename(badFilesFileName + random, badFilesFileName);
            File2.rename(     dirTableFileName + random, dirTableFileName);
            //do fileTable last: more changes, more important
            File2.rename(    fileTableFileName + random, fileTableFileName); 
            if (reallyVerbose) String2.log("save fileTable(first 5 rows)=\n" + 
                fileTable.dataToCSVString(5));
        } catch (Throwable t) {
            String subject = String2.ERROR + 
                " while saving dirTable, fileTable, or badFiles for " + datasetID;
            String msg = MustBe.throwableToString(t);
            String2.log(subject + "\n" + msg);
            EDStatic.email(EDStatic.emailEverythingToCsv, subject, msg);

            File2.delete( dirTableFileName + random);
            File2.delete(fileTableFileName + random);
            File2.delete( badFilesFileName + random);

            throw t;
        }
    }

    /**
     * This returns a HashMap with the variable=value entries from a userQuery.
     * If any names are the same, the last name=value will be in the hashmap.
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
            String tParts[] = Table.getDapQueryParts(userQuery); //decoded.  userQuery="" returns String[1]  with #0=""
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
                    String2.log(String2.ERROR + " while writing pngInfo image information to\n" +
                        infoFileName + " :\n" +
                        tError);
                }
                if (reallyVerbose) String2.log(
                        "    userDapQuery=" + userDapQuery + "\n" +
                        "    fileTypeName=" + fileTypeName + "\n" +
                        "    infoFileName=" + infoFileName + "\n" + 
                        sb.toString());

            } catch (Throwable t) {
                String2.log(String2.ERROR + " while writing pngInfo image information for\n" +
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
        long eTime = System.currentTimeMillis();
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
                  "\n    graphDoubleWESN=" + String2.toCSSVString(graphDoubleWESN) +
                  "\n    graphIntWESN="  + String2.toCSSVString(graphIntWESN));

            return new Object[]{graphDoubleWESN, graphIntWESN};
        } catch (Throwable t) {
            String2.log(String2.ERROR + " (time=" + (System.currentTimeMillis() - eTime) + 
                ") while reading pngInfo image information for \n" + 
                "  userDapQuery=" + userDapQuery + "\n" +
                "  fileTypeName=" + fileTypeName + "\n" +
                "  infoFileName=" + infoFileName + "\n" + 
                MustBe.throwableToString(t));
            return null;
        }
    }

    /**
     * This walks through the start directory and subdirectories and tries
     * to generateDatasetsXml for groups of data files that it finds.
     * <br>This assumes that when a dataset is found, the dataset includes all 
     *   subdirectories.
     * <br>If dataset is found, sibling directories will be treated as separate datasets
     *   (e.g., dir for 1990's, dir for 2000's, dir for 2010's will be separate datasets).
     *   But they should be easy to combine by hand.
     * <br>This will only catch one type of file in a directory (e.g., 
     *   a dir with sst files and chl files will just catch one of those).
     * 
     * @return a suggested chunk of xml for all datasets it can find for use in datasets.xml 
     * @throws Throwable if trouble, e.g., startDir not found or no valid datasets were made.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXmlFromFiles(String startDir) throws Exception {
        String2.log("> EDD.generateDatasetsXmlFromFiles(" + startDir  + ")");
        StringBuilder resultsSB = new StringBuilder();
        long time = System.currentTimeMillis();

        //get list of subdirs
        //because of the way it recurses, the order is already fine for my use here:
        //  every parent directory is listed before all of its child directories.
        StringArray paths = FileVisitorSubdir.oneStep(startDir);
        int nDirs = paths.size();
        //String2.pressEnterToContinue(String2.toNewlineString(paths.toArray()));
        
        StringArray dirs = new StringArray(nDirs, false);
        for (int i = 0; i < nDirs; i++) {
            String path = paths.get(i); 
            if (File.separatorChar == '\\')
                path = String2.replaceAll(path, '\\', '/');
            path = File2.addSlash(path);
            dirs.add(path);
        }
        StringArray dirInfo = new StringArray(nDirs, true);
        Table dirTable = new Table();
        dirTable.addColumn("dir", dirs);
        dirTable.addColumn("dirInfo", dirInfo);
        BitSet dirDone = new BitSet(nDirs); //all false
        String2.log("> nDirs=" + nDirs + " elapsedTime=" +
            (System.currentTimeMillis() - time));

        //go through dirs, from high level to low level, looking for datafiles/datasets
        int nCreated = 0;
        int nGridNc = 0;
        int nTableNcCF = 0;
        int nTableNc = 0;
        int nTableAscii = 0;
        int nTableFileNames = 0;
        String skipThisDir = "> Skip this directory: ";
        String success = "> Success: ";
        String indent = "    ";
        for (int diri = 0; diri < nDirs; diri++) {
            String tDir = dirs.get(diri);
            String2.log("> dir#" + diri + " of " + nDirs + "=" + tDir);
            if (dirDone.get(diri)) {
                dirInfo.set(diri, indent + "see parent dataset");
                String2.log("> Skip this directory: already covered by a dataset in a parent dir.");
                continue;
            }

            Table fileTable = FileVisitorDNLS.oneStep(tDir, ".*", 
                false, false); //tRecursive, tDirectoriesToo
            StringArray names = (StringArray)fileTable.getColumn(FileVisitorDNLS.NAME);
            StringArray exts = new StringArray();
            int nFiles = names.size();
            if (nFiles == 0) {
                dirDone.set(diri);
                String msg = "nFiles=0";
                dirInfo.set(diri, indent + msg);
                String2.log(skipThisDir + msg);
                continue;
            }

            //tally the file's extensions
            Tally tally = new Tally();
            for (int filei = 0; filei < nFiles; filei++) {
                String tName = names.get(filei);
                String ext = File2.getExtension(tName); //may be ""
                exts.add(ext);
                if (ext.equals(".md5") || 
                    tName.toLowerCase().startsWith("readme")) { //readme or read_me
                    //don't tally .md5, readme, or others?
                } else {
                    tally.add("ext", ext);
                }
            }
            fileTable.addColumn(0, "ext", exts);
        
            //get the most common file extension
            ArrayList tallyArrayList = tally.getSortedNamesAndCounts("ext");
            if (tallyArrayList == null)
                return "";
            StringArray tallyExts = (StringArray)tallyArrayList.get(0);
            IntArray tallyCounts = (IntArray)tallyArrayList.get(1);
            if (tallyCounts.size() == 0) {
                dirDone.set(diri);
                String msg = "0 of " + nFiles + " have interesting extensions";
                dirInfo.set(diri, indent + msg);
                String2.log(skipThisDir + msg);
                continue;
            }
            String topExt = tallyExts.get(0);
            int topCount = tallyCounts.get(0);
            int sampleRow = exts.indexOf(topExt);
            String sampleName = names.get(sampleRow);
            String2.log("> topExt=" + topExt + " topCount=" + topCount + " sample=" + sampleName);
            String topOfAre = topCount + " of " + nFiles + " files are " + topExt + ": ";

            if (topCount < 4) {
                //I'm looking for collections of data files. 
                //Don't be distracted by e.g., one .txt file.
                dirDone.set(diri);
                String msg = topOfAre + "That's less than 4.";
                dirInfo.set(diri, indent + msg);
                String2.log(skipThisDir + msg);
                continue;
            }

            //try to make datasets.xml for files in this dir (and subdirs)
            int tReloadEveryNMinutes = 1440;
//If updateNMillis works, then 1440 is good. If not, then 180?

            //table in .ncCF file
            if (topExt.equals(".nc") || topExt.equals(".cdf")) {
                String featureType = null;
                try {
                    //does it have featureType metadata?
                    NetcdfFile ncFile = NcHelper.openFile(tDir + sampleName);
                    Attributes gAtts = new Attributes();
                    NcHelper.getGlobalAttributes(ncFile, gAtts);
                    featureType = gAtts.getString("featureType"); 
                    ncFile.close();
                    if (featureType == null)
                        throw new RuntimeException("No featureType, so it isn't an .ncCF file.");

                    //try to interpret as a .ncCF file
                    String xmlChunk = EDDTableFromNcCFFiles.generateDatasetsXml(
                        tDir, ".*\\" + topExt, 
                        tDir + sampleName, tReloadEveryNMinutes,
                        "", "", "", "", //extract
                        "", "", "", "", "", null); //other info
                    resultsSB.append(xmlChunk);  //recursive=true
                    for (int diri2 = diri; diri2 < nDirs; diri2++)
                        if (dirs.get(diri2).startsWith(tDir))
                            dirDone.set(diri2);
                    String msg = topOfAre + "EDDTableFromNcCFFiles/" + featureType;
                    dirInfo.set(diri, indent + msg);
                    String2.log(success + msg);
                    nTableNcCF++;
                    nCreated++;
                    continue;
                } catch (Throwable t) {
                    String2.log("> Attempt with EDDTableFromNcCFFiles (" + 
                        featureType + ") failed:\n" +
                        MustBe.throwableToString(t));
                }
            }

            //grid via netcdf-java
            if (topExt.equals(".nc") || topExt.equals(".cdf") || 
                topExt.equals(".hdf") || 
                topExt.equals(".grb") || topExt.equals(".grb2") || 
                topExt.equals(".bufr") || 
                topExt.equals("")) {  //.hdf are sometimes unidentified
                try {
                    String xmlChunk = EDDGridFromNcFiles.generateDatasetsXml(
                        tDir, ".*\\" + topExt, 
                        tDir + sampleName, 
                        tReloadEveryNMinutes, null); //externalAddGlobalAttributes
                    resultsSB.append(xmlChunk);  //recursive=true
                    for (int diri2 = diri; diri2 < nDirs; diri2++)
                        if (dirs.get(diri2).startsWith(tDir))
                            dirDone.set(diri2);
                    String msg = topOfAre + "EDDGridFromNcFiles";
                    dirInfo.set(diri, indent + msg);
                    String2.log(success + msg);
                    nGridNc++; 
                    nCreated++;
                    continue;
                } catch (Throwable t) {
                    String2.log("> Attempt with EDDGridFromNcFiles failed:\n" +
                        MustBe.throwableToString(t));
                }
            }

            //table in .nc file
            if (topExt.equals(".nc") || topExt.equals(".cdf")) {
                try {
                    String xmlChunk = EDDTableFromNcFiles.generateDatasetsXml(
                        tDir, ".*\\" + topExt, 
                        tDir + sampleName, "", tReloadEveryNMinutes,
                        "", "", "", "", //extract
                        "", "", "", "", "", "", null); //other info
                    resultsSB.append(xmlChunk);  //recursive=true
                    for (int diri2 = diri; diri2 < nDirs; diri2++)
                        if (dirs.get(diri2).startsWith(tDir))
                            dirDone.set(diri2);
                    String msg = topOfAre + "EDDTableFromNcFiles";
                    dirInfo.set(diri, indent + msg);
                    String2.log(success + msg);
                    nTableNc++; 
                    nCreated++;
                    continue;
                } catch (Throwable t) {
                    String2.log("> Attempt with EDDTableFromNcFiles failed:\n" +
                        MustBe.throwableToString(t));
                }
            } 

            //ascii table 
            if (topExt.equals(".csv") || topExt.equals(".tsv") || 
                topExt.equals(".txt")) {
                try {
                    String xmlChunk = EDDTableFromAsciiFiles.generateDatasetsXml(
                        tDir, ".*\\" + topExt, 
                        tDir + sampleName, 
                        "", 1, 2, //charset, columnNamesRow, firstDataRow, 
                        tReloadEveryNMinutes, 
                        "", "", "", "", //extract
                        "", "", "", "", "", "", null); //other info
                    resultsSB.append(xmlChunk);  //recursive=true
                    for (int diri2 = diri; diri2 < nDirs; diri2++)
                        if (dirs.get(diri2).startsWith(tDir))
                            dirDone.set(diri2);
                    String msg = topOfAre + "EDDTableFromAsciiFiles";
                    dirInfo.set(diri, indent + msg);
                    String2.log(success + msg);
                    nTableAscii++;
                    nCreated++;
                    continue;
                } catch (Throwable t) {
                    String2.log("> Attempt with EDDTableFromAscii failed:\n" +
                        MustBe.throwableToString(t));
                }
            }

            //all fail? Use EDDTableFromFileNames and serve all files (not just topExt)
            try {
                String xmlChunk = EDDTableFromFileNames.generateDatasetsXml(
                    tDir, ".*", true, //recursive 
                    tReloadEveryNMinutes, 
                    "", "", "", "", null); //other info
                resultsSB.append(xmlChunk);  //recursive=true
                for (int diri2 = diri; diri2 < nDirs; diri2++)
                    if (dirs.get(diri2).startsWith(tDir))
                        dirDone.set(diri2);
                String msg = topOfAre + "EDDTableFromFileNames";
                dirInfo.set(diri, indent + msg);
                String2.log(success + msg);
                nTableFileNames++;
                nCreated++;
                continue;
            } catch (Throwable t) {
                String2.log("> Attempt with EDDTableFromFileNames failed! Give up on this dir.\n" +
                    MustBe.throwableToString(t));
            }
        }

        String2.log("\nDirectory Tree:\n");
        String2.log(dirTable.dataToCSVString());
        String2.log("\n> *** EDD.generateDatasetsXmlFromFiles finished successfully. time=" +
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + "\n" +
            "> nDirs=" + nDirs + " nDatasetsCreated=" + nCreated + "\n" +
            "> (nGridNc=" + nGridNc + " nTablencCF=" + nTableNcCF +
            " nTableNc=" + nTableNc + " nTableAscii=" + nTableAscii +
            " nTableFileNames=" + nTableFileNames + ")\n");
        if (nCreated == 0)
            throw new RuntimeException("No datasets.xml chunks where successfully constructed."); 
        return resultsSB.toString();
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
        //Math2.gcAndWait(); Math2.gcAndWait(); //used in development, before getMemoryInUse
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
            Math2.gcAndWait(); Math2.gcAndWait(); //Used in development.  Before getMemoryInUse().
            memory = Math2.getMemoryInUse() - memory;
            String2.log("\n*** DasDds: memoryUse=" + (memory/1024) + 
                " KB\nPress CtrlBreak in console window to generate hprof heap info.");
            String2.pressEnterToContinue(); 
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
