/* 
 * EDDTable Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
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
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.griddata.Matlab;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.sgt.CompoundColorMap;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;


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
import ucar.ma2.*;

/** 
 * This class represents a dataset where the results can be presented as a table.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-08
 */
public abstract class EDDTable extends EDD { 

    public final static String dapProtocol = "tabledap"; 
    public final static String SEQUENCE_NAME = "s"; //same for all datasets
   
    /** 
     * This is a list of all operator symbols (for my convenience in parseUserDapQuery: 
     * 2 letter ops are first). 
     * Note that all variables (numeric or String)
     * can be constrained via a ERDDAP constraint using any of these
     * operators.
     * If the source can't support the operator (e.g., 
     *    no source supports numeric =~ testing,
     *    and no source supports string &lt; &lt;= &gt; &gt;= testing),
     * ERDDAP will just get all the relevant data and do the test itself. 
     */
    public final static String OPERATORS[] = { 
        //EDDTableFromFiles.isOK relies on this order
        "!=", PrimitiveArray.REGEX_OP, "<=", ">=",   
        "=", "<", ">"}; 
    /** Partially Percent Encoded Operators ('=' is not encoded)*/
    public final static String PPE_OPERATORS[] = { 
        //EDDTableFromFiles.isOK relies on this order
        "!=", PrimitiveArray.REGEX_OP, "%3C=", "%3E=",   
        "=", "%3C", "%3E"}; 
    private final static int opDefaults[] = {
        String2.indexOf(OPERATORS, ">="),
        String2.indexOf(OPERATORS, "<=")};

    /** These parallel OPERATORS, but &gt; and &lt; operators are reversed.
        These are used when converting altitude constraints to depth constraints. */
    public final static String REVERSED_OPERATORS[] = { 
        "!=", PrimitiveArray.REGEX_OP, ">=", "<=",   
        "=", ">", "<"}; 

    /** A list of the greater-than less-than operators, which some sources can't handle for strings. */
    public final static String GTLT_OPERATORS[] = {
        "<=", ">=", "<", ">"}; 
    /** The index of REGEX_OP in OPERATORS. */
    public static int REGEX_OP_INDEX = String2.indexOf(OPERATORS, PrimitiveArray.REGEX_OP);

    /** The orderBy options. */
    public static String orderByOptions[] = {
        "orderBy", "orderByMax", "orderByMin", "orderByMinMax"};

    /** This is used in many file types as the row identifier. */
    public final static String ROW_NAME = "row";  //see also Table.ROW_NAME

    /** These are needed for EDD-required methods of the same name. */
    public final static String[] dataFileTypeNames = {  
        ".asc", ".csv", ".csvp", ".das", ".dds", 
        ".dods", ".esriCsv", ".fgdc", ".geoJson", ".graph", ".help", ".html", 
        ".htmlTable", ".iso19115", ".json", ".mat", ".nc", ".ncHeader", 
        ".ncCF", ".ncCFHeader", ".ncCFMA", ".ncCFMAHeader", ".odvTxt", 
        ".subset", ".tsv", ".tsvp", ".xhtml"};
    public final static String[] dataFileTypeExtensions = {
        ".asc", ".csv", ".csv", ".das", ".dds", 
        ".dods", ".csv", ".xml", ".json", ".html", ".html", ".html", 
        ".html", ".xml", ".json", ".mat", ".nc", ".txt",
        ".nc", ".txt", ".nc", ".txt", ".txt", 
        ".html",   ".tsv", ".tsv", ".xhtml"};
    //These all used to have " (It may take a while. Please be patient.)" at the end.
    public static String[] dataFileTypeDescriptions = {
        EDStatic.fileHelp_asc,
        EDStatic.fileHelp_csv,
        EDStatic.fileHelp_csvp,
        EDStatic.fileHelp_das,
        EDStatic.fileHelp_dds,
        EDStatic.fileHelp_dods,
        EDStatic.fileHelpTable_esriAscii,
        EDStatic.fileHelp_fgdc,
        EDStatic.fileHelp_geoJson,
        EDStatic.fileHelp_graph,
        EDStatic.fileHelpTable_help,
        EDStatic.fileHelp_html,
        EDStatic.fileHelp_htmlTable,
        EDStatic.fileHelp_iso19115,
        EDStatic.fileHelp_json,
        EDStatic.fileHelp_mat,
        EDStatic.fileHelpTable_nc,
        EDStatic.fileHelp_ncHeader,
        EDStatic.fileHelp_ncCF,
        EDStatic.fileHelp_ncCFHeader,
        EDStatic.fileHelp_ncCFMA,
        EDStatic.fileHelp_ncCFMAHeader,
        EDStatic.fileHelpTable_odvTxt,
        EDStatic.fileHelp_subset,
        EDStatic.fileHelp_tsv,
        EDStatic.fileHelp_tsvp,
        EDStatic.fileHelp_xhtml
    };
    public static String[] dataFileTypeInfo = {  //"" if not available
        "http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#ASCII_Service", //OPeNDAP ascii
        //csv: also see http://www.ietf.org/rfc/rfc4180.txt
        "http://en.wikipedia.org/wiki/Comma-separated_values", //csv was "http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm", 
        "http://en.wikipedia.org/wiki/Comma-separated_values", //csv was "http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm", 
        "http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Dataset_Attribute_Structure", //das
        "http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Dataset_Descriptor_Structure", //dds
        "http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Data_Transmission", //dods
        "http://resources.arcgis.com/content/kbase?fa=articleShow&d=27589", //.esriCsv
        "http://www.fgdc.gov/", //fgdc
        "http://wiki.geojson.org/Main_Page", //geoJSON
        "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#GraphicsCommands", //GraphicsCommands
        "http://www.opendap.org/pdf/ESE-RFC-004v1.2.pdf", //help
        "http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#WWW_Interface_Service", //html
        "http://www.w3schools.com/html/html_tables.asp", //htmlTable
        "http://en.wikipedia.org/wiki/Geospatial_metadata", //iso19115
        "http://www.json.org/", //json
        "http://www.mathworks.com/", //mat
        "http://www.unidata.ucar.edu/software/netcdf/", //nc
        "http://www.unidata.ucar.edu/software/netcdf/docs/guide_ncdump.html", //ncHeader
        "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries", //ncCF Discrete Sampling Geometries 
        "http://www.unidata.ucar.edu/software/netcdf/docs/guide_ncdump.html", //ncCFHeader
        "http://www.nodc.noaa.gov/data/formats/netcdf/", //.ncCFMA
        "http://www.unidata.ucar.edu/software/netcdf/docs/guide_ncdump.html", //ncCFMAHeader
        "http://odv.awi.de/en/documentation/", //odv
        "http://en.wikipedia.org/wiki/Faceted_search",  //subset
        "http://www.cs.tut.fi/~jkorpela/TSV.html",  //tsv
        "http://www.cs.tut.fi/~jkorpela/TSV.html",  //tsv
        "http://www.w3schools.com/html/html_tables.asp"}; //xhtml
        //"http://www.tizag.com/htmlT/tables.php" //xhtml

    public final static String[] imageFileTypeNames = {
        ".kml", ".smallPdf", ".pdf", ".largePdf", ".smallPng", ".png", ".largePng", ".transparentPng"};  
    public final static String[] imageFileTypeExtensions = {
        ".kml", ".pdf", ".pdf", ".pdf", ".png", ".png", ".png", ".png"};
    public static String[] imageFileTypeDescriptions = {
        EDStatic.fileHelpTable_kml,
        EDStatic.fileHelp_smallPdf,
        EDStatic.fileHelp_pdf,
        EDStatic.fileHelp_largePdf,
        EDStatic.fileHelp_smallPng,
        EDStatic.fileHelp_png,
        EDStatic.fileHelp_largePng,
        EDStatic.fileHelp_transparentPng
    };
    public static String[] imageFileTypeInfo = {
        "http://earth.google.com/", //kml
        "http://www.adobe.com/products/acrobat/adobepdf.html", //pdf
        "http://www.adobe.com/products/acrobat/adobepdf.html", //pdf
        "http://www.adobe.com/products/acrobat/adobepdf.html", //pdf
        "http://www.libpng.org/pub/png/", //png
        "http://www.libpng.org/pub/png/", //png
        "http://www.libpng.org/pub/png/", //png
        "http://www.libpng.org/pub/png/" //png
    };

    private static String[] allFileTypeOptions, allFileTypeNames;
    private static int defaultFileTypeOption;

    /** SOS static values. */
    public final static String sosServer = "server";
    public final static String sosVersion = "1.0.0";
    public final static String sosNetworkOfferingType = "network";
    /** sosDSOutputFormat is for DescribeSensor. Note that it needs to be XML.encodeAsXML in a url. */
    public final static String sosDSOutputFormat = "text/xml;subtype=\"sensorML/1.0.1\"";
    public final static String sosPhenomenaDictionaryUrl = "phenomenaDictionary.xml";
    /** These are the mime types allowed for SOS requestFormat. */
    public final static int sosDefaultDataResponseFormat = 9; //tsv
    public final static int sosOostethysDataResponseFormat = 2;
    public final static String sosDataResponseFormats[] = {         
        "text/xml;schema=\"ioos/0.6.1\"",      //two names for the same IOOS SOS XML format
          "application/ioos+xml;version=0.6.1", //"ioos" in the names is detected by OutputStreamFromHttpResponse
          "text/xml; subtype=\"om/1.0.0\"", //Oostethys-style  pre 2010-04-30 was application/com-xml
          "text/csv",      
        "application/json;subtype=geojson", 
          "text/html",    //no way to distinguish an html table vs html document in general
          "application/json",     
        "application/x-matlab-mat",  //??? made up, x=nonstandard
          "application/x-netcdf",       //see http://www.wussu.com/various/mimetype.htm
          "application/x-netcdf", //ncCF, see http://www.wussu.com/various/mimetype.htm
          "application/x-netcdf", //ncCFMA
          "text/tab-separated-values",     
        "application/xhtml+xml"};
    /** These are the corresponding tabledap dataFileTypeNames, not the final file extensions. */
    public final static String sosTabledapDataResponseTypes[] = { 
        //*** ADDING A FORMAT? Add support for it in sosGetObservation().
        //see isIoosSosXmlResponseFormat()
        "text/xml;schema=\"ioos/0.6.1\"",       //needed by OutputStreamFromHttpResponse
          "application/ioos+xml;version=0.6.1", //needed by OutputStreamFromHttpResponse
          //see isOosethysXmlResponseFormat()
          "text/xml; subtype=\"om/1.0.0\"", //Oostethys-style //checked for by OutputStreamFromHttpResponse, was application/com-xml
          ".csvp",      
        ".geoJson", 
          ".htmlTable", 
          ".json",     
        ".mat",     
          ".nc", 
          ".ncCF", 
          ".ncCFMA", 
          ".odvTxt",     
        ".xhtml"};
    //** And there are analogouse arrays for image formats (enable these when variables can be accessed separately)
    public final static String sosImageResponseFormats[] = {
        OutputStreamFromHttpResponse.KML_MIME_TYPE, 
        "application/pdf",
        "image/png"};
    public final static String sosTabledapImageResponseTypes[] = {
        ".kml", ".pdf", ".png"};


    //static constructor
    static {
        int nDFTN = dataFileTypeNames.length;
        int nIFTN = imageFileTypeNames.length;
        Test.ensureEqual(nDFTN, dataFileTypeDescriptions.length,
            "'dataFileTypeNames.length' not equal to 'dataFileTypeDescriptions.length'.");                                     
        Test.ensureEqual(nDFTN, dataFileTypeExtensions.length,
            "'dataFileTypeNames.length' not equal to 'dataFileTypeExtensions.length'.");                                     
        Test.ensureEqual(nDFTN, dataFileTypeInfo.length,
            "'dataFileTypeNames.length' not equal to 'dataFileTypeInfo.length'.");                                     
        Test.ensureEqual(nIFTN, imageFileTypeDescriptions.length,
            "'imageFileTypeNames.length' not equal to 'imageFileTypeDescriptions.length'.");                                     
        Test.ensureEqual(nIFTN, imageFileTypeExtensions.length,
            "'imageFileTypeNames.length' not equal to 'imageFileTypeExtensions.length'.");                                     
        Test.ensureEqual(nIFTN, imageFileTypeInfo.length,
            "'imageFileTypeNames.length' not equal to 'imageFileTypeInfo.length'.");                                     
        defaultFileTypeOption = String2.indexOf(dataFileTypeNames, ".htmlTable");

        //construct allFileTypeOptions
        allFileTypeOptions = new String[nDFTN + nIFTN];
        allFileTypeNames = new String[nDFTN + nIFTN];
        for (int i = 0; i < nDFTN; i++) {
            allFileTypeOptions[i] = dataFileTypeNames[i] + " - " + dataFileTypeDescriptions[i];
            allFileTypeNames[i] = dataFileTypeNames[i];
        }
        for (int i = 0; i < nIFTN; i++) {
            allFileTypeOptions[nDFTN + i] = imageFileTypeNames[i] + " - " + imageFileTypeDescriptions[i];
            allFileTypeNames[nDFTN + i] = imageFileTypeNames[i];
        }

        Test.ensureEqual(sosDataResponseFormats.length, sosTabledapDataResponseTypes.length,
            "'sosDataResponseFormats.length' not equal to 'sosTabledapDataResponseTypes.length'.");                                     
        Test.ensureEqual(sosImageResponseFormats.length, sosTabledapImageResponseTypes.length,
            "'sosImageResponseFormats.length' not equal to 'sosTabledapImageResponseTypes.length'.");                                     

    }


    //*********** end of static declarations ***************************

    /**
     * These specifies which variables the source can constrain
     * (either via constraints in the query or in the getSourceData method).
     * <br>For example, DAPPER lets you constrain the lon,lat,depth,time
     *   but not dataVariables
     *   (http://www.epic.noaa.gov/epic/software/dapper/dapperdocs/conventions/
     *   and see the Constraints heading).
     * <br>It is presumed that no source can handle regex's for numeric variables.
     * <p>CONSTRAIN_PARTIAL indicates the constraint will be partly handled by 
     *   getSourceData and fully handled by standarizeResultsTable.
     * The constructor must set these.
     */
    public final static int CONSTRAIN_NO = 0, CONSTRAIN_PARTIAL = 1, CONSTRAIN_YES = 2;
    protected int sourceCanConstrainNumericData = -1;  //not set
    protected int sourceCanConstrainStringData  = -1;  //not set
    /** Some sources don't handle floating point =, !=, &gt;=, &lt;= correctly and
     * need these constraints expanded to &gt;= and/or &lt;= +/-0.001 (fudge).  */
    protected boolean sourceNeedsExpandedFP_EQ = false; //but some subclasses default to true
    /** Some sources can constrain String regexes (use =~ or ~=); some can't (use ""). 
        standardizeResultsTable always (re)tests regex constraints (so =~ here acts like PARTIAL).
        See PrimitiveArray.REGEX_OP.*/
    protected String sourceCanConstrainStringRegex = ""; 

    /** If present, these dataVariables[xxxIndex] values are set by the constructor. 
     * alt and depth mustn't both be active.
     */
    protected int lonIndex = -1, latIndex = -1, altIndex = -1, depthIndex = -1, timeIndex = -1; 
    protected int profile_idIndex = -1, timeseries_idIndex = -1, trajectory_idIndex = -1; //see accessibleViaNcCF
    protected String requiredCfRequestVariables[]; //each data request must include all of these
    //a list of all profile|timeseries|trajectory variables
    //each data request must include at least one variable *not* on this list
    protected String outerCfVariables[]; 


    /** 
     * The list of dataVariable destinationNames for use by .subset and SUBSET_FILENAME
     * (or null if not yet set up or String[0] if unused).
     */
    private String subsetVariables[] = null;
    public static String DEFAULT_SUBSET_VIEWS = //viewDistinctData will default to 1000
        "&.viewDistinctMap=true"; 

    /** 
     * EDDTable.subsetVariables relies on this name ending to know which cached subset files to delete 
     * when a dataset is reloaded.
     */
    public static String SUBSET_FILENAME          = "subset.nc";
    public static String DISTINCT_SUBSET_FILENAME = "distinct.nc";
    
    /** 
     * These are parallel data structures for use by setSosOfferingTypeAndIndex(). 
     * If cdmDataType is sosCdmDataTypes[t], 
     *   then sosOfferingType is sosOfferingTypes[t] 
     *   and sosOfferingIndex is var with cf_role=sosCdmCfRoles[t],
     * !!!FOR NOW, cdm_data_type must be TimeSeries and 
     * so sosOfferingType must be Station (?Check with Derrick)
     */
    public static String sosCdmDataTypes[]     = {"TimeSeries"};
    public static String sosCdmOfferingTypes[] = {"Station"};
    public static String sosCdmCfRoles[]       = {"timeseries_id"};
    /** 
     * This is usually set by setSosOfferingTypeAndIndex(). 
     * This will be null if the dataset is not available via ERDDAP's SOS server.
     */
    public String sosOfferingType = null; 
    /** 
     * The index of the dataVariable with the sosOffering outer var (e.g. with cf_role=timeseries_id). 
     * This is usually set by setSosOfferingTypeAndIndex(). 
     * This will be -1 if the dataset is not available via ERDDAP's SOS server.
     */
    public int sosOfferingIndex = -1;

    /**
     * To make a dataset accessibleViaSOS, the constructor must set these parallel
     * PrimitiveArrays with the relevant *destination* values for each offering
     * (e.g., timeseries/trajectory/profile). 
     * <br>These will be null if the dataset is not available via ERDDAP's SOS server.
     * <br>EDDTable subclasses that have all this info available (e.g., EDDTableFromSOS)
     *   set these right before ensureValid().  ensureValid() also tries to set these
     *   for other datasets (without trying to get min/max time) 
     *   if the information is available from subsetVariablesDataTable.
     * <br>sosOfferings has the original/short offering names.  (Not sos....)
     * <br>MinTime and MaxTime are epochSeconds and can have NaNs; the others can't have NaNs.
     * <br>Consumers of the information shouldn't change the values.
     */
    public PrimitiveArray sosOfferings, sosMinTime, sosMaxTime, 
        sosMinLat, sosMaxLat, sosMinLon, sosMaxLon;

    /** 
     * This tries to set sosOfferingType and sosOfferingIndex and 
     * returns true if successful.
     * Use this near end of constructor -- it needs combinedAttributes and dataVariables[].
     *
     * @return true if sosOfferingType and sosOfferingIndex were set.
     */
    public boolean setSosOfferingTypeAndIndex() {
        //Is the cdm_data_type one of the compatible types?
        String cdmType = combinedGlobalAttributes().getString("cdm_data_type");
        int type = String2.indexOf(sosCdmDataTypes, cdmType);
        if (type < 0) {
            if (debugMode) String2.log("setSosOfferingTypeAndIndex=false.  cdm_data_type=" + 
                cdmType + " is incompatible.");
            return false;
        }
        sosOfferingType = sosCdmOfferingTypes[type];

        //look for the var with the right cf_role
        String sosCdmCfRole = sosCdmCfRoles[type];
        sosOfferingIndex = -1;
        for (int dv = 0; dv < dataVariables.length; dv++) {
            if (sosCdmCfRole.equals(dataVariables[dv].combinedAttributes().getString("cf_role"))) {
                sosOfferingIndex = dv;
                break;
            }
        }
        if (sosOfferingIndex >= 0) {
            if (debugMode) String2.log("setSosOfferingTypeAndIndex=true");
            return true;
        } else {
            sosOfferingType = null;
            if (debugMode) String2.log("setSosOfferingTypeAndIndex=false.  No var has cf_role=" + 
                sosCdmCfRole);
            return false;
        }
    }


    /**
     * This returns the dapProtocol
     *
     * @return the dapProtocol
     */
    public String dapProtocol() {return dapProtocol; }

    /**
     * This returns the dapDescription 
     *
     * @return the dapDescription
     */
    public String dapDescription() {return EDStatic.EDDTableDapDescription; }

    /**
     * This returns the longDapDescription 
     *
     * @return the longDapDescription
     */
    public static String longDapDescription(String tErddapUrl) {
        return String2.replaceAll(EDStatic.EDDTableDapLongDescription, "&erddapUrl;", tErddapUrl); 
    }

    /**
     * This MUST be used by all subclass constructors to ensure that 
     * all of the items common to all EDDTables are properly set.
     * This also does some actual work.
     *
     * @throws Throwable if any required item isn't properly set
     */
    public void ensureValid() throws Throwable {
        super.ensureValid();

        String errorInMethod = "datasets.xml/EDDTable.ensureValid error for datasetID=" + 
            datasetID + ":\n ";

        Test.ensureTrue(lonIndex < 0 || dataVariables[lonIndex] instanceof EDVLon, 
            errorInMethod + "dataVariable[lonIndex=" + lonIndex + "] isn't an EDVLon.");
        Test.ensureTrue(latIndex < 0 || dataVariables[latIndex] instanceof EDVLat, 
            errorInMethod + "dataVariable[latIndex=" + latIndex + "] isn't an EDVLat.");
        Test.ensureTrue(altIndex < 0 || dataVariables[altIndex] instanceof EDVAlt, 
            errorInMethod + "dataVariable[altIndex=" + altIndex + "] isn't an EDVAlt.");
        Test.ensureTrue(depthIndex < 0 || dataVariables[depthIndex] instanceof EDVDepth, 
            errorInMethod + "dataVariable[depthIndex=" + depthIndex + "] isn't an EDVDepth.");
        //some places depend on not having alt *and* depth
        Test.ensureTrue(altIndex <= 0 || depthIndex <= 0,
            errorInMethod + "The dataset has both an altitude and a depth variable.");
        Test.ensureTrue(timeIndex < 0 || dataVariables[timeIndex] instanceof EDVTime, 
            errorInMethod + "dataVariable[timeIndex=" + timeIndex + "] isn't an EDVTime.");

        Test.ensureTrue(sourceCanConstrainNumericData >= 0 && sourceCanConstrainNumericData <= 2, 
            errorInMethod + "sourceCanConstrainNumericData=" + sourceCanConstrainNumericData + " must be 0, 1, or 2.");
        Test.ensureTrue(sourceCanConstrainStringData >= 0 && sourceCanConstrainStringData <= 2, 
            errorInMethod + "sourceCanConstrainStringData=" + sourceCanConstrainStringData + " must be 0, 1, or 2.");
        Test.ensureTrue(sourceCanConstrainStringRegex.equals("=~") ||
            sourceCanConstrainStringRegex.equals("~=") ||
            sourceCanConstrainStringRegex.equals(""),
            errorInMethod + "sourceCanConstrainStringRegex=\"" + 
                sourceCanConstrainStringData + "\" must be \"=~\", \"~=\", or \"\".");

        //*Before* add standard metadata below...
        //Make subsetVariablesDataTable and distinctSubsetVariablesDataTable(loggedInAs=null).
        //if accessibleViaSubset() and dataset is available to public.
        //This isn't required (ERDDAP was/could be lazy), but doing it makes initial access fast
        //  and makes it thread-safe (always done in constructor's thread).
        if (accessibleViaSubset().length() == 0 && accessibleTo == null) {

            //If this throws exception, dataset initialization will fail.  That seems fair.
            Table table = distinctSubsetVariablesDataTable(null, null);
            //it calls subsetVariablesDataTable(null);        

            //now go back and set destinationMin/Max
            //see EDDTableFromDap.testSubsetVariablesRange()
            int nCol = table.nColumns();
            for (int col = 0; col < nCol; col++) {
                String colName = table.getColumnName(col);
                int which = String2.indexOf(dataVariableDestinationNames(), colName);                    
                if (which < 0) 
                    throw new SimpleException(String2.ERROR + ": column=" + colName + 
                        " in subsetVariables isn't in dataVariables=\"" +
                        String2.toCSSVString(dataVariableDestinationNames()) + "\".");
                EDV edv = dataVariables[which];
                PrimitiveArray pa = table.getColumn(col);
                //note that time is stored as doubles in distinctValues table
                //String2.log("distinct col=" + colName + " " + pa.elementClassString());
                if (pa instanceof StringArray)
                    continue;
                int nMinMax[] = pa.getNMinMaxIndex();
                if (nMinMax[0] == 0)
                    continue;
                edv.setDestinationMin(pa.getDouble(nMinMax[1]));
                edv.setDestinationMax(pa.getDouble(nMinMax[2]));
                edv.setActualRangeFromDestinationMinMax();
            }
        }

        //add standard metadata to combinedGlobalAttributes
        //(This should always be done, so shouldn't be in an optional method...)
        String destNames[] = dataVariableDestinationNames();
        //lon
        //String2.log(">> lonIndex=" + lonIndex);
        if (lonIndex >= 0) {
            combinedGlobalAttributes.add("geospatial_lon_units", EDV.LON_UNITS);
            PrimitiveArray pa = dataVariables[lonIndex].combinedAttributes().get("actual_range");
            if (pa != null) {
                combinedGlobalAttributes.add("geospatial_lon_min", pa.getNiceDouble(0));
                combinedGlobalAttributes.add("geospatial_lon_max", pa.getNiceDouble(1));
                combinedGlobalAttributes.add("Westernmost_Easting", pa.getNiceDouble(0));
                combinedGlobalAttributes.add("Easternmost_Easting", pa.getNiceDouble(1));
            }
        }

        //lat
        if (latIndex >= 0) {
            combinedGlobalAttributes.add("geospatial_lat_units", EDV.LAT_UNITS);
            PrimitiveArray pa = dataVariables[latIndex].combinedAttributes().get("actual_range");
            if (pa != null) {
                combinedGlobalAttributes.add("geospatial_lat_min", pa.getNiceDouble(0));
                combinedGlobalAttributes.add("geospatial_lat_max", pa.getNiceDouble(1));
                combinedGlobalAttributes.add("Southernmost_Northing", pa.getNiceDouble(0));
                combinedGlobalAttributes.add("Northernmost_Northing", pa.getNiceDouble(1));
            }
        }

        //alt
        if (altIndex >= 0) {
            combinedGlobalAttributes.add("geospatial_vertical_positive", "up");
            combinedGlobalAttributes.add("geospatial_vertical_units", EDV.ALT_UNITS);
            PrimitiveArray pa = dataVariables[altIndex].combinedAttributes().get("actual_range");
            if (pa != null) {
                combinedGlobalAttributes.add("geospatial_vertical_min", pa.getNiceDouble(0));
                combinedGlobalAttributes.add("geospatial_vertical_max", pa.getNiceDouble(1));
            }
        } else if (depthIndex >= 0) {
            combinedGlobalAttributes.add("geospatial_vertical_positive", "down");
            combinedGlobalAttributes.add("geospatial_vertical_units", EDV.DEPTH_UNITS);
            PrimitiveArray pa = dataVariables[depthIndex].combinedAttributes().get("actual_range");
            if (pa != null) {
                combinedGlobalAttributes.add("geospatial_vertical_min", pa.getNiceDouble(0));
                combinedGlobalAttributes.add("geospatial_vertical_max", pa.getNiceDouble(1));
            }
        }

        //time
        if (timeIndex >= 0) {
            PrimitiveArray pa = dataVariables[timeIndex].combinedAttributes().get("actual_range");
            if (pa != null) {
                double d = pa.getDouble(0);
                if (!Double.isNaN(d))
                    combinedGlobalAttributes.set("time_coverage_start", Calendar2.epochSecondsToIsoStringT(d) + "Z");
                d = pa.getDouble(1);  //will be NaN for 'present'.   Deal with this better???
                if (!Double.isNaN(d))
                    combinedGlobalAttributes.set("time_coverage_end",   Calendar2.epochSecondsToIsoStringT(d) + "Z");
            }
        }

        //set featureType from cdm_data_type (if it is a type ERDDAP supports)
        String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
        if (cdmType != null) {}
        else if (CDM_POINT.equals(cdmType) || 
            CDM_PROFILE.equals(cdmType) || 
            CDM_TRAJECTORY.equals(cdmType) || 
            CDM_TRAJECTORYPROFILE.equals(cdmType) ||
            CDM_TIMESERIES.equals(cdmType) || 
            CDM_TIMESERIESPROFILE.equals(cdmType))
            combinedGlobalAttributes.set("featureType", 
                cdmType.substring(0, 1).toLowerCase() + 
                cdmType.substring(1));

        //rethink accessibleViaSos.  
        //This generic SOS setup uses entire time range (not each offerings time range).
        //Do this *after* subsetVariablesDataTable has been created.
        if (EDStatic.sosActive && accessibleViaSOS.length() > 0) 
            genericSosSetup();

        //last: uses time_coverage metadata
        //make FGDC and ISO19115
        accessibleViaFGDC     = null; //should be null already, but make sure so files will be created now
        accessibleViaISO19115 = null;
        accessibleViaFGDC();
        accessibleViaISO19115();

        //really last: it uses accessibleViaFGDC and accessibleViaISO19115
        //make searchString  (since should have all finished/correct metadata)
        //This makes creation of searchString thread-safe (always done in constructor's thread).
        searchString();
    }


    /** 
     * The index of the longitude variable (-1 if not present).
     * @return the index of the longitude variable (-1 if not present)
     */
    public int lonIndex() {return lonIndex; }

    /** 
     * The index of the latitude variable (-1 if not present).
     * @return the index of the latitude variable (-1 if not present)
     */
    public int latIndex() {return latIndex; }

    /** 
     * The index of the altitude variable (-1 if not present).
     * @return the index of the altitude variable (-1 if not present)
     */
    public int altIndex() {return altIndex; }

    /** 
     * The index of the depth variable (-1 if not present).
     * @return the index of the depth variable (-1 if not present)
     */
    public int depthIndex() {return depthIndex; }

    /** 
     * The index of the time variable (-1 if not present).
     * @return the index of the time variable (-1 if not present)
     */
    public int timeIndex() {return timeIndex; }

    /**
     * The string representation of this tableDataSet (for diagnostic purposes).
     *
     * @return the string representation of this tableDataSet.
     */
    public String toString() {  
        //make this JSON format?
        StringBuilder sb = new StringBuilder();
        sb.append("//** EDDTable " + super.toString() +
            "\nsourceCanConstrainNumericData=" + sourceCanConstrainNumericData +
            "\nsourceCanConstrainStringData="  + sourceCanConstrainStringData +
            "\nsourceCanConstrainStringRegex=\"" + sourceCanConstrainStringRegex + "\"" +
            "\n\\**\n\n");
        return sb.toString();
    }

    /**
     * This makes the searchString (mixed case) used to create searchBytes or searchDocument.
     *
     * @return the searchString (mixed case) used to create searchBytes or searchDocument.
     */
    public String searchString() {    

        StringBuilder sb = startOfSearchString();

        String2.replaceAll(sb, "\"", ""); //no double quotes (esp around attribute values)
        String2.replaceAll(sb, "\n    ", "\n"); //occurs for all attributes
        return sb.toString();
    }



    /** 
     * This returns 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or
     * 2=CONSTRAIN_YES (completely), indicating if the source can handle 
     * all non-regex constraints on numericDataVariables
     * (either via constraints in the query or in the getSourceData method).
     *
     * <p>CONSTRAIN_NO indicates that sourceQueryFromDapQuery should remove
     * these constraints, because the source doesn't even want to see them.
     * <br>CONSTRAIN_YES indicates that standardizeResultsTable needn't
     * do the tests for these constraints.
     *
     * <p>Note that CONSTRAIN_PARTIAL is very useful. It says that
     * the source would like to see all of the constraints (e.g., 
     * from sourceQueryFromDapQuery) so that it can then further prune
     * the constraints as needed and use them or not. 
     * Then, standardizeResultsTable will test all of the constraints.
     * It is a flexible and reliable approach.
     *
     * <p>It is assumed that no source can handle regex for numeric data, so this says nothing about regex.
     *
     * @return 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or
     * 2=CONSTRAIN_YES (completely) indicating if the source can handle 
     * all non-regex constraints on numericDataVariables 
     * (ignoring regex, which it is assumed none can handle).
     */
    public int sourceCanConstrainNumericData() {
        return sourceCanConstrainNumericData;
    }

    /** 
     * This returns 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or
     * 2=CONSTRAIN_YES (completely), indicating if the source can handle 
     * all non-regex constraints on stringDataVariables
     * (either via constraints in the query or in the getSourceData method).
     *
     * <p>CONSTRAIN_NO indicates that sourceQueryFromDapQuery should remove
     * these constraints, because the source doesn't even want to see them.
     * <br>CONSTRAIN_YES indicates that standardizeResultsTable needn't
     * do the tests for these constraints.
     *
     * <p>Note that CONSTRAIN_PARTIAL is very useful. It says that
     * the source would like to see all of the constraints (e.g., 
     * from sourceQueryFromDapQuery) so that it can then further prune
     * the constraints as needed and use them or not. 
     * Then, standardizeResultsTable will test all of the constraints.
     * It is a flexible and reliable approach.
     *
     * <p>PARTIAL and YES say nothing about regex; see sourceCanConstrainStringRegex.
     * So this=YES + sourceCanConstrainStringReges="" is a valid setup.
     *
     * @return 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or
     * 2=CONSTRAIN_YES (completely) indicating if the source can handle 
     * all non-regex constraints on stringDataVariables.
     */
    public int sourceCanConstrainStringData() {
        return sourceCanConstrainStringData;
    }

    /** 
     * This returns "=~" or "~=" (indicating the operator that the source uses)
     * if the source can handle some source regex constraints, or "" if it can't.
     * <br>sourceCanConstrainStringData must be PARTIAL or YES if this isn't "".
     * <br>If this isn't "", the implication is always that the source can
     *    only handle the regex PARTIAL-ly (i.e., it is always also checked in
     *    standardizeResultsTable).
     *
     * @return "=~" or "~=" if the source can handle some source regex constraints,
     * or "" if it can't.
     */
    public String sourceCanConstrainStringRegex() {
        return sourceCanConstrainStringRegex;
    }


    /** 
     * This is a convenience for the subclass' getDataForDapQuery methods.
     * <br>This is the first step in converting a userDapQuery into a sourceDapQuery.
     * <br>The userDapQuery refers to destinationNames 
     *   and allows any constraint op on any variable in the dataset.
     * <br>The sourceDapQuery refers to sourceNames and utilizes the 
     *   sourceCanConstraintXxx settings to move some constraintVariables
     *   into resultsVariables so they can be tested by
     *   standardizeResultsTable (and by the source, or not).
     * <br>This removes constraints which can't be handled by the source 
     *   (e.g., regex for numberic source variables).
     * <br>But the subclass will almost always need to fine-tune the
     *   results from this method to PRUNE out additional constraints
     *   that the source can't handle (e.g., constraints on inner variables
     *   in an OPeNDAP sequence).
     *
     * <p>Users can do a numeric test for "=NaN" or "!=NaN" (and it works as they expect),
     * but the NaN must always be represented by "NaN" (not just an invalid number).
     *
     * @param userDapQuery  the query from the user after the '?', still percentEncoded (shouldn't be null)
     *  (e.g., var1,var2&var3%3C=40) 
     *   referring to destinationNames (e.g., LAT), not sourceNames.
     * @param resultsVariables will receive the list of variables that 
     *   it should request.
     *   <br>If a constraintVariable isn't in the original resultsVariables list
     *     that the user submitted, it will be added here.
     * @param constraintVariables will receive the constraint source variables
     *     that the source has indicated (via sourceCanConstrainNumericData and
     *     sourceCanConstrainStringData) that it would like to see.
     * @param constraintOps will receive the source operators corresponding to the 
     *     constraint variables.
     *   <br>REGEX_OP remains REGEX_OP (it isn't converted to sourceCanConstrainRegex string).
     *     It is only useful if destValuesEqualSourceValues(). 
     *   <br>For non-regex ops, if edv.scale_factor is negative, then operators 
     *      which include &lt; or &gt; will be reversed to be &gt; or &lt;.
     * @param constraintValues will receive the source values corresponding to 
     *     the constraint variables.
     *   <br>Note that EDVTimeStamp values for non-regex ops will be returned as 
     *      epoch seconds, not in the source format.
     *   <br>For non-regex ops, edv.scale_factor and edv.add_offset will have 
     *      been applied in reverse so that the constraintValues are appropriate 
     *      for the source.
     * @throws Throwable if trouble (e.g., improper format or unrecognized variable)
     */
    public void getSourceQueryFromDapQuery(String userDapQuery, 
        StringArray resultsVariables,
        StringArray constraintVariables,
        StringArray constraintOps,
        StringArray constraintValues) throws Throwable {
        if (reallyVerbose) String2.log("\nEDDTable.getSourceQueryFromDapQuery...");

        //pick the query apart
        resultsVariables.clear();
        constraintVariables.clear();
        constraintOps.clear();
        constraintValues.clear();
        parseUserDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues, //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            false);
        if (debugMode) String2.log(">>constraintValues=" + constraintValues);

        //remove any fixedValue variables from resultsVariables (source can't provide their data values)
        //work backwards since I may remove some variables
        for (int rv = resultsVariables.size() - 1; rv >= 0; rv--) {
            EDV edv = findDataVariableByDestinationName(resultsVariables.get(rv)); 
            if (edv.isFixedValue())
                resultsVariables.remove(rv);
        }

        //make source query (which the source can handle, and which gets all 
        //  the data needed to do all the constraints and make the final table)

        //Deal with each of the constraintVariables.
        //Work backwards because I often delete the current one.
        //!!!!! VERY IMPORTANT CODE. THINK IT THROUGH CAREFULLY.
        //!!!!! THE IF/ELSE STRUCTURE HERE IS EXACTLY THE SAME AS IN standardizeResultsTable.
        //!!!!! SO IF YOU MAKE A CHANGE HERE, MAKE A CHANGE THERE.
        for (int cv = constraintVariables.size() - 1; cv >= 0; cv--) { 
            String constraintVariable = constraintVariables.get(cv);
            int dv = String2.indexOf(dataVariableDestinationNames(), constraintVariable);
            EDV edv = dataVariables[dv];
            Class sourceClass = edv.sourceDataTypeClass();
            Class destClass = edv.destinationDataTypeClass(); 
            boolean isTimeStamp = edv instanceof EDVTimeStamp;

            String constraintOp = constraintOps.get(cv);
            String constraintValue = constraintValues.get(cv); //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            double constraintValueD = String2.parseDouble(constraintValue);
            //Only valid numeric constraintValue NaN is "NaN".
            //Test this because it helps discover other constraint syntax errors.
            if (destClass != String.class &&
                !constraintOp.equals(PrimitiveArray.REGEX_OP) &&
                Double.isNaN(constraintValueD)) {
                if (constraintValue.equals("NaN") || (isTimeStamp && constraintValue.equals(""))) {
                    //not an error
                } else {
                    throw new SimpleException(EDStatic.queryError +
                        MessageFormat.format(EDStatic.queryErrorConstraintNaN, constraintValue));
                }
            }
            if (reallyVerbose) String2.log("  Looking at constraint#" + cv + ": " + constraintVariable + 
                " " + constraintOp + " " + constraintValue);

            //constraintVariable is a fixedValue
            if (edv.isFixedValue()) {
                //it's a fixedValue; do constraint now
                //pass=that's nice    fail=NO DATA
                boolean tPassed = false;
                if (edv.sourceDataTypeClass() == String.class ||
                    constraintOp.equals(PrimitiveArray.REGEX_OP)) 
                    tPassed = PrimitiveArray.testValueOpValue(edv.fixedValue(),  
                        constraintOp, constraintValue);
                else if (edv.sourceDataTypeClass() == float.class) 
                    tPassed = PrimitiveArray.testValueOpValue(String2.parseFloat(edv.fixedValue()),
                        constraintOp, constraintValueD);
                else  
                    tPassed = PrimitiveArray.testValueOpValue(String2.parseDouble(edv.fixedValue()),
                        constraintOp, constraintValueD);

                if (!tPassed)
                    throw new SimpleException(MustBe.THERE_IS_NO_DATA + " " +
                        MessageFormat.format(EDStatic.noDataFixedValue, edv.destinationName(), 
                            edv.fixedValue() + constraintOp + constraintValueD));

                //It passed, so remove the constraint from list passed to source.
                constraintVariables.remove(cv);
                constraintOps.remove(cv);
                constraintValues.remove(cv);
                if (reallyVerbose) 
                    String2.log("    The fixedValue constraint passes.");

            //constraint source is String                  
            } else if (sourceClass == String.class) {
                if (sourceCanConstrainStringData == CONSTRAIN_NO ||
                    (constraintOp.equals(PrimitiveArray.REGEX_OP) && sourceCanConstrainStringRegex.length() == 0)) {
                    //remove from constraints and constrain after the fact in standardizeResultsTable
                    constraintVariables.remove(cv);
                    constraintOps.remove(cv);
                    constraintValues.remove(cv);                            
                } else {
                    //source will (partially) handle it
                    if (reallyVerbose) 
                        String2.log("    The string constraint will be (partially) handled by source.");
                }

                //if source handling is NO or PARTIAL, data is needed to do the test in standardizeResultsTable
                if (sourceCanConstrainStringData == CONSTRAIN_NO ||
                    sourceCanConstrainStringData == CONSTRAIN_PARTIAL ||
                    constraintOp.equals(PrimitiveArray.REGEX_OP)) {    //regex always (also) done in standardizeResultsTable
                    if (resultsVariables.indexOf(constraintVariable) < 0)
                        resultsVariables.add(constraintVariable);
                }

            //constraint source is numeric
            } else {  
                if (sourceCanConstrainNumericData == CONSTRAIN_NO ||
                    constraintOp.equals(PrimitiveArray.REGEX_OP)) {
                    //remove from constraints and constrain after the fact in standardizeResultsTable
                    constraintVariables.remove(cv);
                    constraintOps.remove(cv);
                    constraintValues.remove(cv);                            
                } else {
                    //source will (partially) handle it
                    if (reallyVerbose) 
                        String2.log("    The numeric constraint will be (partially) handled by source.");

                    if (sourceNeedsExpandedFP_EQ && 
                        (sourceClass == float.class || sourceClass == double.class) && 
                        !Double.isNaN(constraintValueD) &&
                        constraintValueD != Math2.roundToDouble(constraintValueD)) { //if not int

                        double fudge = 0.001;
                        if (reallyVerbose && constraintOp.indexOf('=') >= 0)
                            String2.log("    The constraint with '=' is being expanded because sourceNeedsExpandedFP_EQ=true.");
                        if (constraintOp.equals(">=")) {
                            constraintValues.set(cv, String2.genEFormat10(constraintValueD - fudge));
                        } else if (constraintOp.equals("<=")) {
                            constraintValues.set(cv, String2.genEFormat10(constraintValueD + fudge));
                        } else if (constraintOp.equals("=")) {
                            constraintValueD = Math2.roundTo(constraintValueD, 3); //fudge 0.001 -> 3
                            constraintVariables.add(cv + 1, constraintVariable);
                            constraintOps.set(cv,     ">=");
                            constraintOps.add(cv + 1, "<=");
                            constraintValues.set(cv,     String2.genEFormat10(constraintValueD - fudge));
                            constraintValues.add(cv + 1, String2.genEFormat10(constraintValueD + fudge)); 
                        } else if (constraintOp.equals("!=")) {
                            constraintValueD = Math2.roundTo(constraintValueD, 3); //fudge 0.001 -> 3
                            constraintVariables.add(cv + 1, constraintVariable);
                            constraintOps.set(cv,     "<=");
                            constraintOps.add(cv + 1, ">=");
                            constraintValues.set(cv,     String2.genEFormat10(constraintValueD - fudge));
                            constraintValues.add(cv + 1, String2.genEFormat10(constraintValueD + fudge)); 
                        }
                    }
                }

                //if source handling is NO or PARTIAL, data is needed to do the test below
                if (sourceCanConstrainNumericData == CONSTRAIN_NO ||
                    sourceCanConstrainNumericData == CONSTRAIN_PARTIAL ||
                    constraintOp.equals(PrimitiveArray.REGEX_OP)) {
                    if (resultsVariables.indexOf(constraintVariable) < 0)
                        resultsVariables.add(constraintVariable);
                }
            }
        }

        //gather info about constraints
        int nConstraints = constraintVariables.size();
        EDV constraintEdv[] = new EDV[nConstraints];
        boolean constraintVarIsString[] = new boolean[nConstraints];
        boolean constraintOpIsRegex[] = new boolean[nConstraints];
        double  constraintValD[] = new double[nConstraints];
        boolean constraintValDIsNaN[] = new boolean[nConstraints];
        for (int cv = 0; cv < nConstraints; cv++) {
            EDV edv = findDataVariableByDestinationName(constraintVariables.get(cv));
            constraintEdv[cv] = edv;
            constraintVarIsString[cv] = edv.sourceDataTypeClass() == String.class;
            constraintOpIsRegex[cv] = constraintOps.get(cv).equals(PrimitiveArray.REGEX_OP);
            constraintValD[cv] = constraintValues.getDouble(cv);
            constraintValDIsNaN[cv] = Double.isNaN(constraintValD[cv]);
        }

        //Before convert to source names and values, test for conflicting (so impossible) constraints.
        //n^2 test, but should go quickly    (tests are quick and there are never a huge number of constraints)
        for (int cv1 = 0; cv1 < constraintVariables.size(); cv1++) { 
            if (constraintVarIsString[cv1] || constraintOpIsRegex[cv1])
                continue;
            EDV edv1 = constraintEdv[cv1];
            String op1 = constraintOps.get(cv1);
            if (op1.equals(PrimitiveArray.REGEX_OP))
                continue;
            char ch1 = op1.charAt(0);
            double val1 = constraintValD[cv1];
            boolean isNaN1 = constraintValDIsNaN[cv1];

            //is this test impossible by itself?
            if (isNaN1 && (op1.equals("<") || op1.equals(">")))
                throw new SimpleException(EDStatic.queryError +
                    MessageFormat.format(EDStatic.queryErrorNeverTrue, 
                        edv1.destinationName() + op1 + val1));

            for (int cv2 = cv1 + 1; cv2 < constraintVariables.size(); cv2++) { 
                if (constraintVarIsString[cv2] || constraintOpIsRegex[cv2])
                    continue;
                EDV edv2 = constraintEdv[cv2];
                if (edv1 != edv2)  //yes, fastest to do simple test of inequality of pointers
                    continue;
 
                //we now know constraints use same, non-string var, and neither op is REGEX_OP
                //We don't have to catch every impossible query, 
                //but DON'T flag any possible queries as impossible!
                String op2 = constraintOps.get(cv2);
                char ch2 = op2.charAt(0);
                double val2 = constraintValD[cv2];
                boolean isNaN2 = constraintValDIsNaN[cv2];
                boolean possible = true;
                //deal with isNaN and !isNaN
                //deal with ops:  < <= > >= = !=
                if (isNaN1) { 
                    if (ch1 == '!') {
                        if (isNaN2) {
                            //impossible: a!=NaN & a<=|>=|=NaN
                            if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
                        }

                    } else {
                        // test1 must be a<=|>=|=NaN, which really must be a=NaN
                        if (isNaN2) {
                            //impossible: a=NaN & a!=NaN
                            if (ch2 == '!') possible = false;
                        } else {
                            //impossible: a=NaN & a<|<=|>|>=|=3
                            if (ch2 != '!') possible = false;
                        }
                    }

                } else {
                    //val1 is finite
                    if (ch1 == '<') {
                        if (isNaN2) {
                            //impossible: a<|<=2 & a<=|>=|=NaN
                            if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
                        } else {
                            //impossible: a<|<=2 & a>|>=|=3
                            if ((ch2 == '>' || ch2 == '=') && val1 < val2) possible = false;
                        }
                    } else if (ch1 == '>') { 
                        if (isNaN2) {
                            //impossible: a>|>=2 & a<=|>=|=NaN
                            if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
                        } else {
                            //impossible: a>|>=3 & a<|<=|=2
                            if ((ch2 == '<' || ch2 == '=') && val1 > val2) possible = false;
                        }
                    } else if (ch1 == '=') {
                        if (isNaN2) {
                            //impossible: a=2 & a<=|>=|=NaN
                            if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
                        } else {
                            if (ch2 == '>') {
                                //impossible: a=3 & a>|>=4 
                                if (val1 < val2) possible = false;
                            } else if (ch2 == '<') {
                                //impossible: a=3 & a<|<=2
                                if (val1 > val2) possible = false;
                            } else if (ch2 == '=') {
                                //impossible: a=2 & a=3
                                if (val1 != val2) possible = false;  //can do simple != test
                            } else if (ch2 == '!') {
                                //impossible: a=2 & a!=2
                                if (val1 == val2) possible = false;  //can do simple == test
                            }
                        }
                    }
                }
                if (!possible) throw new SimpleException(EDStatic.queryError + 
                    MessageFormat.format(EDStatic.queryErrorNeverBothTrue,
                        edv1.destinationName() + op1 + val1,
                        edv2.destinationName() + op2 + val2));
            }
        }

        //Convert resultsVariables and constraintVariables to sourceNames.
        //Do last because sourceNames may not be unique (e.g., for derived axes, e.g., "=0")
        //And cleaner and safer to do all at once.
        for (int rv = 0; rv < resultsVariables.size(); rv++) {
            EDV edv = findDataVariableByDestinationName(resultsVariables.get(rv));
            resultsVariables.set(rv, edv.sourceName());
        }
        for (int cv = 0; cv < nConstraints; cv++) {
            EDV edv = constraintEdv[cv];
            constraintVariables.set(cv, edv.sourceName());

            //and if constaintVariable isn't in resultsVariables, add it
            if (resultsVariables.indexOf(edv.sourceName()) < 0)
                resultsVariables.add(edv.sourceName());

            if (!constraintOpIsRegex[cv] &&        //regex constraints are unchanged
                !(edv instanceof EDVTimeStamp) &&  //times stay as epochSeconds
                edv.scaleAddOffset()) {            //need to apply scale and add_offset  

                //apply scale_factor and add_offset to non-regex constraintValues
                // sourceValue = (destintationValue - addOffset) / scaleFactor;
                double td = (constraintValD[cv] - edv.addOffset()) / edv.scaleFactor();
                Class tClass = edv.sourceDataTypeClass();
                if (PrimitiveArray.isIntegerType(tClass))
                     constraintValues.set(cv, "" + Math2.roundToLong(td));
                else if (tClass == float.class)  constraintValues.set(cv, "" + (float)td);
                else                             constraintValues.set(cv, "" + td);
                //String2.log(">>after scaleAddOffset applied constraintVal=" + td);

                //*if* scale_factor < 0, reverse > and < operators 
                if (edv.scaleFactor() < 0) {
                    int copi = String2.indexOf(OPERATORS, constraintOps.get(cv));
                    constraintOps.set(cv, REVERSED_OPERATORS[copi]);
                }
            }
        }

        if (reallyVerbose) String2.log("getSourceQueryFromDapQuery done");
        if (debugMode) String2.log(">>constraintValues=" + constraintValues);

    }

    /** 
     * This is a convenience for the subclass' getData methods.
     * This converts the table of data from the source OPeNDAP query into
     * the finished, standardized, results table.  If need be, it tests constraints
     * that the source couldn't test.
     *
     * <p>This always does all regex tests.
     *
     * <p>Coming into this method, the column names are sourceNames.
     *  Coming out, they are destinationNames.
     *
     * <p>Coming into this method, missing values in the table
     *  will be the original edv.sourceMissingValues or sourceFillValues.
     *  Coming out, they will be edv.destinationMissingValues or destinationFillValues.
     *
     * <p>This removes any global or column attributes from the table.
     *  This then adds a copy of the combinedGlobalAttributes,
     *  and adds a copy of each variable's combinedAttributes.
     *
     * <p>This doesn't call setActualRangeAndBoundingBox
     *   (since you need the entire table to get correct values).
     *   In fact, all the file-specific metadata is removed (e.g., actual_range) here.
     *   Currently, the only file types that need file-specific metadata are 
     *   .nc, .ncHeader, .ncCF, .ncCFHeader, .ncCFMA, and .ncCFMAHeader.
     *   (.das always shows original metadata, not subset metadata).
     *   Currently the only place the file-specific metadata is added
     *   is in TableWriterAll (which has access to all the data).
     *
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery  the OPeNDAP DAP-style query from the user, 
     *   after the '?', still percentEncoded (shouldn't be null).
     *   (e.g., var1,var2&var3&lt;40) 
     *   referring to variable's destinationNames (e.g., LAT), instead of the
     *   sourceNames.
     *   Presumably, getSourceDapQuery has ensured that this is a valid query
     *   and has tested the constraints on fixedValue axes.
     * @param table the table from the source query which will be modified extensively
     *   to become the finished standardized table.
     *   It must have all the requested source resultsVariables (using sourceNames),
     *     and variables needed for constraints that need to be handled here.
     *   If it doesn't, the variables are assumed to be all missing values.
     *   It may have additional variables.
     *   It doesn't need to already have globalAttributes or variable attributes.
     *   If nRows=0, this throws SimpleException(MustBe.THERE_IS_NO_DATA).
     * @throws Throwable if trouble (e.g., improper format or
     *    unrecognized variable).
     *    Because this may be called separately for each station, 
     *    this doesn't throw exception if no data at end; 
     *    tableWriter.finish will catch such problems.
     *    But it does throw exception if no data at beginning.
     *    
     */
    public void standardizeResultsTable(String requestUrl, String userDapQuery, 
            Table table) throws Throwable {
        if (reallyVerbose) String2.log("\nstandardizeResultsTable incoming cols=" + table.getColumnNamesCSSVString());
        //String2.log("table=\n" + table.toString());
        //String2.log("DEBUG " + MustBe.getStackTrace());
        if (table.nRows() == 0) 
            throw new SimpleException(MustBe.THERE_IS_NO_DATA);

        //pick the query apart
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        parseUserDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            false);
        
        //set the globalAttributes (this takes care of title, summary, ...)
        setResponseGlobalAttributes(requestUrl, userDapQuery, table);

        //Change all table columnNames from sourceName to destinationName.
        //Do first because sourceNames in Erddap may not be unique 
        //(e.g., for derived vars, e.g., "=0").
        //But all table columnNames are unique because they are direct from source.
        //And cleaner and safer to do all at once.
        int nRows = table.nRows();
        for (int col = table.nColumns() - 1; col >= 0; col--) { //work backward since may remove some cols

            //These are direct from source, so no danger of ambiguous fixedValue source names.
            String columnSourceName = table.getColumnName(col);
            int dv = String2.indexOf(dataVariableSourceNames(), columnSourceName);
            if (dv < 0) {
                //remove unexpected column
                if (reallyVerbose) String2.log("  removing unexpected source column=" + columnSourceName);
                table.removeColumn(col);
                continue;
            }

            //set the attributes
            //String2.log("  col=" + col + " dv=" + dv + " nRows=" + nRows);
            EDV edv = dataVariables[dv];
            table.setColumnName(col, edv.destinationName());
            table.columnAttributes(col).clear(); //remove any existing atts
            table.columnAttributes(col).set(edv.combinedAttributes()); //make a copy

            //convert source values to destination values
            //(source fill&missing values become destination fill&missing values)
            table.setColumn(col, edv.toDestination(table.getColumn(col)));
        }

        //apply the constraints and finish up
        applyConstraints(table, false,  //don't apply CONSTRAIN_YES constraints
            resultsVariables, 
            constraintVariables, constraintOps, constraintValues);

        if (reallyVerbose) String2.log("standardizeResultsTable done.");

    }


    /** 
     * This is used by standardizeResultsTable 
     * (and places that bypass standardizeResultsTable) to update the 
     * globalAtrbiutes of a response table.
     */
    public void setResponseGlobalAttributes(String requestUrl, String userDapQuery, 
        Table table) {

        //set the globalAttributes (this takes care of title, summary, ...)
        table.globalAttributes().clear(); //remove any existing atts
        table.globalAttributes().set(combinedGlobalAttributes); //make a copy

        //fix up global attributes  (always to a local COPY of global attributes)
        EDD.addToHistory(table.globalAttributes(), publicSourceUrl());
        EDD.addToHistory(table.globalAttributes(), 
            EDStatic.baseUrl + requestUrl + 
            (userDapQuery == null || userDapQuery.length() == 0? "" : "?" + userDapQuery));
    }


    /**
     * Given a table that contains the destination values (and destinationMV)
     * for the resultsVariables and constraintVariables,
     * this applies the constraints (removing some rows of the table),
     * removes non-resultsVariables variables, sets results variable attributes,
     * and unsets actual_range metadata.
     *
     *
     * @param table  which contains the destination values (and destinationMV)
     *    for the resultsVariables and constraintVariables and will be modified.
     *    If a given variable is missing, it is assumed to be all missing values.
     * @param applyAllConstraints  if true, all constraints will be applied.
     *    If false, CONSTRAIN_YES constraints are not applied.
     * @param resultsVariables destinationNames
     * @param constraintVariables  destinationNames
     * @param constraintOps   any ops are ok.
     * @param constraintValues
     * @throws 
     */
    public void applyConstraints(Table table, boolean applyAllConstraints,
        StringArray resultsVariables, 
        StringArray constraintVariables, StringArray constraintOps, StringArray constraintValues) 
        throws Throwable {

        //move the requested resultsVariables columns into place   and add metadata
        int nRows = table.nRows();
        for (int rv = 0; rv < resultsVariables.size(); rv++) {

            int dv = String2.indexOf(dataVariableDestinationNames(), resultsVariables.get(rv));
            EDV edv = dataVariables[dv];
            int col = table.findColumnNumber(edv.destinationName());
            if (edv.isFixedValue()) {
                //edv is a fixedValue
                //if the column isn't present, add it
                if (col < 0) 
                    table.addColumn(rv, resultsVariables.get(rv), 
                        PrimitiveArray.factory(edv.destinationDataTypeClass(), nRows, 
                            edv.fixedValue()),
                        new Attributes(edv.combinedAttributes())); //make a copy
            } else {
                //edv is not fixedValue, so make sure it is in the table. 
                if (col < 0) {
                    //not found, so assume all missing values
                    if (verbose) String2.log("Note: " +
                        "variable=" + edv.destinationName() + 
                        " not found in results table.");
                    col = table.nColumns();
                    double dmv = edv.safeDestinationMissingValue();
                    table.addColumn(col, edv.destinationName(), 
                        PrimitiveArray.factory(edv.destinationDataTypeClass(), nRows, 
                            (Double.isNaN(dmv)? "" : "" + dmv)),
                        new Attributes(edv.combinedAttributes()));

                } else if (col < rv) {
                    throw new SimpleException("Error: " +
                        "variable=" + edv.destinationName() + " is in two columns (" + 
                        col + " and " + rv + ") in the results table.\n" +
                        "colNames=" + String2.toCSSVString(table.getColumnNames()));
                }

                //move the column into place
                table.moveColumn(col, rv);
                table.columnAttributes(rv).set(edv.combinedAttributes()); //make a copy
            }
        }
        //String2.log("table after rearrange cols=\n" + table.toString());

        
        //deal with the constraints one-by-one
        //!!!!! VERY IMPORTANT CODE. THINK IT THROUGH CAREFULLY.
        //!!!!! THE IF/ELSE STRUCTURE HERE IS ALMOST EXACTLY THE SAME AS IN getSourceDapQuery.
        //Difference: above, only CONSTRAIN_NO items are removed from what goes to source
        //            below, CONSTRAIN_NO and CONSTRAIN_PARTIAL items are tested here
        //!!!!! SO IF YOU MAKE A CHANGE HERE, MAKE A CHANGE THERE.
        BitSet keep = new BitSet();
        keep.set(0, nRows, true); 
        if (reallyVerbose) String2.log("  nRows=" + nRows);
        for (int cv = 0; cv < constraintVariables.size(); cv++) { 
            String constraintVariable = constraintVariables.get(cv);
            String constraintOp       = constraintOps.get(cv);
            int dv = String2.indexOf(dataVariableDestinationNames(), constraintVariable);
            EDV edv = dataVariables[dv];
            Class sourceClass = edv.sourceDataTypeClass();
            Class destClass = edv.destinationDataTypeClass();

            if (applyAllConstraints) {
                //applyAllConstraints
            } else {
                //skip this constraint if the source has handled it

                //is constraintVariable a fixedValue? 
                if (edv.isFixedValue()) {
                    //it's a fixedValue axis; always numeric; test was done by getSourceDapQuery
                    continue;

                //constraint source is String
                } else if (sourceClass == String.class) {
                    if (sourceCanConstrainStringData == CONSTRAIN_NO ||
                        sourceCanConstrainStringData == CONSTRAIN_PARTIAL ||
                        constraintOp.equals(PrimitiveArray.REGEX_OP)) {  //always do all regex tests here (perhaps in addition to source)
                        //fall through to test below
                    } else {
                        //source did the test
                        continue;
                    }

                //constraint source is numeric
                } else {
                    if (sourceCanConstrainNumericData == CONSTRAIN_NO ||
                        sourceCanConstrainNumericData == CONSTRAIN_PARTIAL ||
                        constraintOp.equals(PrimitiveArray.REGEX_OP)) { //always do all regex tests here
                        //fall through to test below
                    } else {
                        //source did the test
                        continue;
                    }
                }
            }

            //The constraint needs to be tested here. Test it now.
            //Note that Time and Alt values have been converted to standardized units above.
            PrimitiveArray dataPa = table.findColumn(constraintVariable); //throws Throwable if not found
            String constraintValue = constraintValues.get(cv);
            if (reallyVerbose) String2.log("  Handling constraint #" + cv + " here: " + 
                constraintVariable + " " + constraintOp + " " + constraintValue);
            int nSwitched = 0;
            if (sourceClass != String.class) {
                nSwitched = dataPa.convertToStandardMissingValues(
                    edv.destinationFillValue(), edv.destinationMissingValue());            
                //String2.log("    nSwitched=" + nSwitched);
            }
            int nStillGood = dataPa.applyConstraint(keep, constraintOp, constraintValue);
            if (reallyVerbose) String2.log("    nStillGood=" + nStillGood);
            if (nStillGood == 0)
                break;
            if (nSwitched > 0)
                dataPa.switchNaNToFakeMissingValue(edv.safeDestinationMissingValue());            

            //if (cv == constraintVariables.size() - 1 && nStillGood > 0) String2.log(table.toString());

        }

        //discard excess columns (presumably constraintVariables not handled by source)
        table.removeColumns(resultsVariables.size(), table.nColumns());

        //remove the non-keep rows
        table.justKeep(keep);

        //unsetActualRangeAndBoundingBox  (see comments in method javadocs above)
        //(see TableWriterAllWithMetadata which adds them back)
        table.unsetActualRangeAndBoundingBox();
    }

    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     *
     * <p>This methods allows any constraint on any variable.
     * Since many data sources just support constraints on certain variables
     * (e.g., Dapper only supports axis variable constraints), the implementations
     * here must separate constraints that the server can handle and constraints 
     * that must be handled separately afterwards.
     * See getSourceQueryFromDapQuery.
     *
     * <p>This method does NOT call handleViaFixedOrSubsetVariables.
     * You will usually want to call handleViaFixedOrSubsetVariables before calling this
     * to see if the subsetVariables can handle this request 
     * (since that data is cached and so the response is fast).
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     *   Although this is included, this method does NOT check if loggedInAs has access to 
     *   this dataset. The exceptions are fromPOST datasets, where loggedInAs is
     *   used to determine access to each row of data.
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery  the OPeNDAP DAP-style  
     *   query from the user after the '?', still percentEncoded (may be null).
     *   (e.g., var1,var2&var3%3C=40) 
     *   referring to destination variable names.
     *   See parseUserDapQuery.
     * @param tableWriter
     * @throws Throwable if trouble
     */
    public abstract void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable;

    /** 
     * This is a convenience method to get all of the data for a userDapQuery in a 
     * TableWriterAllWithMetadata.
     * You can easily get the entire table (if it fits in memory) via
     * Table table = getTwawmForDapQuery(...).cumulativeTable();
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but in unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     *   Use "" if not important (e.g., for tests).
     * @param userDapQuery the part of the user's request after the '?', 
     *   still percentEncoded (shouldn't be null).
     * @return twawm will all of the data already read.
     */
    public TableWriterAllWithMetadata getTwawmForDapQuery(String loggedInAs, 
        String requestUrl, String userDapQuery) throws Throwable {

        String dir = cacheDirectory(); //dir is created by EDD.ensureValid
        String fileName = suggestFileName(loggedInAs, userDapQuery, ".twawm");
        if (reallyVerbose) String2.log("getTwawmForDapQuery " + dir + fileName);
        TableWriterAllWithMetadata twawm = new TableWriterAllWithMetadata(dir, fileName);
        TableWriter tableWriter = encloseTableWriter(dir, fileName, twawm, userDapQuery);

        if (handleViaFixedOrSubsetVariables(loggedInAs, requestUrl, userDapQuery, tableWriter)) {}
        else             getDataForDapQuery(loggedInAs, requestUrl, userDapQuery, tableWriter);  
        if (reallyVerbose) String2.log("getTwawmForDapQuery finished.");

        return twawm;
    }


    /** 
     * This is used by subclasses getDataForDapQuery to write a chunk of source table 
     * data to a tableWriter if nCells &gt; partialRequestMaxCells
     * or finish==true.
     * <br>This converts columns to expected sourceTypes (if they weren't already).
     * <br>This calls standardizeResultsTable.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery  after the '?', still percentEncoded (shouldn't be null).
     * @param table with a chunk of the total source data.
     * @param tableWriter to which the table's data will be written
     * @param finish If true, the will be written to the tableWriter.
     *    If false, the data will only be written to tableWriter if there are 
     *    a sufficient number of cells of data.
     * @return true Returns true if it wrote the table data to tableWriter 
     *    (table was changed, so should be thrown away).
     *    Returns false if the data wasn't written -- add some more rows of data and call this again.
     */
    protected boolean writeChunkToTableWriter(String requestUrl, String userDapQuery, 
        Table table, TableWriter tableWriter, boolean finish) throws Throwable {

        //write to tableWriter?
        //String2.log("writeChunkToTableWriter table.nRows=" + table.nRows());
        if (table.nRows() > 200 || //that's a small number, but user is probably eager to see some results
            (table.nRows() * (long)table.nColumns() > EDStatic.partialRequestMaxCells) || 
            finish) {

            if (table.nRows() == 0 && finish) {
                tableWriter.finish();
                return true;
            }

            //String2.log("\ntable at end of getSourceData=\n" + table.toString("row", 10));
            //convert columns to stated type
            for (int col = 0; col < table.nColumns(); col++) {
                String colName = table.getColumnName(col);
                EDV edv = findDataVariableBySourceName(colName); 
                Class shouldBeType = edv.sourceDataTypeClass();
                Class isType = table.getColumn(col).elementClass();
                if (shouldBeType != isType) {
                    //if (reallyVerbose) String2.log("  converting col=" + col + 
                    //    " from=" + PrimitiveArray.elementClassToString(isType) + 
                    //    " to="   + PrimitiveArray.elementClassToString(shouldBeType));
                    PrimitiveArray newPa = PrimitiveArray.factory(shouldBeType, 1, false);
                    newPa.append(table.getColumn(col));
                    table.setColumn(col, newPa);
                }            
            }

            //standardize the results table
            if (table.nRows() > 0) {
                standardizeResultsTable(requestUrl, userDapQuery, table); //changes sourceNames to destinationNames
                tableWriter.writeSome(table);
            }

            //done?
            if (finish)
                tableWriter.finish();

            return true;
        }

        return false;
    }

    /**
     * This returns the types of data files that this dataset can be returned as.
     * These are short descriptive names that are put in the 
     * request url after the dataset name and before the "?", e.g., ".nc". 
     *
     * @return the types of data files that this dataset can be returned as.
     */
    public String[] dataFileTypeNames() {return dataFileTypeNames; }

    /**
     * This returns the file extensions corresponding to the dataFileTypes.
     * E.g., dataFileTypeName=".htmlTable" returns dataFileTypeExtension=".html".
     *
     * @return the file extensions corresponding to the dataFileTypes.
     */
    public String[] dataFileTypeExtensions() {return dataFileTypeExtensions; }

    /**
     * This returns descriptions (up to 80 characters long, suitable for a tooltip)
     * corresponding to the dataFileTypes. 
     *
     * @return descriptions corresponding to the dataFileTypes.
     */
    public String[] dataFileTypeDescriptions() {return dataFileTypeDescriptions; }

    /**
     * This returns an info URL corresponding to the dataFileTypes. 
     *
     * @return an info URL corresponding to the dataFileTypes (an element is "" if not available).
     */
    public String[] dataFileTypeInfo() {return dataFileTypeInfo; }

    /**
     * This returns the types of image files that this dataset can be returned 
     * as. These are short descriptive names that are put in the 
     * request url after the dataset name and before the "?", e.g., ".largePng". 
     *
     * @return the types of image files that this dataset can be returned as.
     */
    public String[] imageFileTypeNames() {return imageFileTypeNames; }

    /**
     * This returns the file extensions corresponding to the imageFileTypes,
     * e.g., imageFileTypeNames=".largePng" returns imageFileTypeExtensions=".png".
     *
     * @return the file extensions corresponding to the imageFileTypes.
     */
    public String[] imageFileTypeExtensions() {return imageFileTypeExtensions; }

    /**
     * This returns descriptions corresponding to the imageFileTypes 
     * (each is suitable for a tooltip).
     *
     * @return descriptions corresponding to the imageFileTypes.
     */
    public String[] imageFileTypeDescriptions() {return imageFileTypeDescriptions; }

    /**
     * This returns an info URL corresponding to the imageFileTypes. 
     *
     * @return an info URL corresponding to the imageFileTypes.
     */
    public String[] imageFileTypeInfo() {return imageFileTypeInfo; }
    
    /**
     * This returns the "[name]" for all dataFileTypes and imageFileTypes.
     *
     * @return the "[name]" for all dataFileTypes and imageFileTypes.
     */
    public String[] allFileTypeNames() {return allFileTypeNames; }

    /**
     * This returns the "[name] - [description]" for all dataFileTypes and imageFileTypes.
     *
     * @return the "[name] - [description]" for all dataFileTypes and imageFileTypes.
     */
    public String[] allFileTypeOptions() {return allFileTypeOptions; }
     
    /**
     * This returns the number of the .htmlTable option 
     *
     * @return the number of the .htmlTable option 
     */
    public int defaultFileTypeOption() {return defaultFileTypeOption; }


    /**
     * This parses a PERCENT ENCODED OPeNDAP DAP-style query.
     * This checks the validity of the resultsVariable and constraintVariabe names.
     *
     * <p>Unofficially (e.g., for testing) the query can be already percent decoded
     * if there are no %dd in the query.
     *
     * <p>There can be a constraints on variables that
     * aren't in the user-specified results variables.
     * This procedure adds those variables to the returned resultsVariables.
     *
     * <p>If the constraintVariable is time, the value
     * can be numeric ("seconds since 1970-01-01") 
     * or String (ISO format, e.g., "1996-01-31T12:40:00", at least YYYY-MM)
     * or now(+|-)(seconds|minutes|hours|days|months|years).
     * All variants are converted to epochSeconds.
     *
     * @param userDapQuery is the opendap DAP-style query, 
     *      after the '?', still percentEncoded (shouldn't be null), e.g.,
     *      <tt>var1,var2,var3&amp;var4=value4&amp;var5%3E=value5</tt> .
     *    <br>Values for String variable constraints should be in double quotes.
     *    <br>A more specific example is 
     *      <tt>genus,species&amp;genus="Macrocystis"&amp;LAT%3C=53&amp;LAT%3C=54</tt> .
     *    <br>If no results variables are specified, all will be returned.
     *      See OPeNDAP specification, section 6.1.1.
     *    <br>Note that each constraint's left hand side must be a variable
     *      and its right hand side must be a value.
     *    <br>The valid operators (for numeric and String variables) are "=", "!=", "&lt;", "&lt;=",  
     *      "&gt;", "&gt;=", and "=~" (REGEX_OP, which looks for data values
     *      matching the regular expression on the right hand side),
     *      but in percent encoded form. 
     *      (see http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Selecting_Data:_Using_Constraint_Expressions).
     *    <br>If an &amp;-separated part is "distinct()", "orderBy("...")", 
     *      "orderByMax("...")", "orderByMin("...")", "orderByMinMax("...")", "units("...")", 
     *      it is ignored.
     *    <br>If an &amp;-separated part starts with ".", it is ignored.
     *      It can't be a variable name.
     *      &amp;.[param]=value is used to pass special values (e.g., &amp;.colorBar=...).
     * @param resultsVariables to be appended with the results variables' 
     *    destinationNames, e.g., {var1,var2,var3}.
     *    <br>If a variable is needed for constraint testing in standardizeResultsTable
     *      but is not among the user-requested resultsVariables, it won't be added 
     *      to resultsVariables.
     * @param constraintVariables to be appended with the constraint variables' 
     *    destinationNames, e.g., {var4,var5}.
     *    This method makes sure they are valid.
     *    These will not be percent encoded.
     * @param constraintOps to be appended with the constraint operators, e.g., {"=", "&gt;="}.
     *    These will not be percent encoded.
     * @param constraintValues to be appended with the constraint values, e.g., {value4,value5}.
     *    These will not be percent encoded.
     *    Non-regex EDVTimeStamp constraintValues will be returned as epochSeconds.
     * @param repair if true, this method tries to do its best repair problems (guess at intent), 
     *     not to throw exceptions 
     * @throws Throwable if invalid query
     *     (0 resultsVariables is a valid query)
     */
    public void parseUserDapQuery(String userDapQuery, 
        StringArray resultsVariables,
        StringArray constraintVariables, StringArray constraintOps, StringArray constraintValues,
        boolean repair) throws Throwable {

        //parse userDapQuery into parts
        String parts[] = getUserQueryParts(userDapQuery); //decoded.  always at least 1 part (may be "")
        resultsVariables.clear();
        constraintVariables.clear(); 
        constraintOps.clear(); 
        constraintValues.clear();

        //expand no resultsVariables (or entire sequence) into all results variables
        //look at part0 with comma-separated vars 
        if (parts[0].length() == 0 || parts[0].equals(SEQUENCE_NAME)) {
            if (debugMode) String2.log("  userDapQuery parts[0]=\"" + parts[0] + 
                "\" is expanded to request all variables.");
            for (int v = 0; v < dataVariables.length; v++) {
                resultsVariables.add(dataVariables[v].destinationName());
            }
        } else {
            String cParts[] = String2.split(parts[0], ',');
            for (int cp = 0; cp < cParts.length; cp++) {

                //request uses sequence.dataVarName notation?
                String tVar = cParts[cp].trim();
                int period = tVar.indexOf('.');
                if (period > 0 && tVar.substring(0, period).equals(SEQUENCE_NAME)) 
                    tVar = tVar.substring(period + 1);

                //is it a valid destinationName?
                int po = String2.indexOf(dataVariableDestinationNames(), tVar);
                if (po < 0) {
                    if (!repair) {
                        if (tVar.equals(SEQUENCE_NAME))
                            throw new SimpleException(EDStatic.queryError +
                                 "If " + SEQUENCE_NAME + " is requested, it must be the only requested variable.");
                        for (int op = 0; op < OPERATORS.length; op++) {
                            int opPo = tVar.indexOf(OPERATORS[op]);
                            if (opPo >= 0)
                                throw new SimpleException(
                                    EDStatic.queryError + "All constraints (including \"" +
                                    tVar.substring(0, opPo + OPERATORS[op].length()) + 
                                    "...\") must be preceded by '&'.");
                        }
                        throw new SimpleException(EDStatic.queryError +
                             "Unrecognized variable=" + tVar);
                    }
                } else {
                    //it's valid; is it a duplicate?
                    if (resultsVariables.indexOf(tVar) >= 0) {
                        if (!repair) 
                            throw new SimpleException(EDStatic.queryError +
                                "variable=" + tVar + " is listed twice in the results variables list.");
                    } else {
                        resultsVariables.add(tVar);
                    }
                }
            }
        }
        //String2.log("resultsVariables=" + resultsVariables);

        //get the constraints 
        for (int p = 1; p < parts.length; p++) {
            //deal with one constraint at a time
            String constraint = parts[p]; 
            int constraintLength = constraint.length();
            //String2.log("constraint=" + constraint);            
            
            //special case: ignore constraint starting with "." 
            //(can't be a variable name)
            //use for e.g., .colorBar=...
            if (constraint.startsWith("."))
                continue;

            //special case: server-side functions
            if (constraint.equals("distinct()") ||
                (constraint.endsWith("\")") &&
                 (constraint.startsWith("orderBy(\"") ||
                  constraint.startsWith("orderByMax(\"") ||
                  constraint.startsWith("orderByMin(\"") ||
                  constraint.startsWith("orderByMinMax(\"") ||
                  constraint.startsWith("units(\""))))
                continue;

            //look for == (common mistake, but not allowed)
            if (constraint.indexOf("==") >= 0) {
                if (repair) constraint = String2.replaceAll(constraint, "==", "=");
                else throw new SimpleException(EDStatic.queryError +
                    "Use '=' instead of '==' in constraints.");
            }

            //look for ~= (common mistake, but not allowed)
            if (constraint.indexOf("~=") >= 0) {
                if (repair) constraint = String2.replaceAll(constraint, "~=", "=~");
                else throw new SimpleException(EDStatic.queryError +
                    "Use '=~' instead of '~=' in constraints.");
            }

            //find the valid op            
            int op = 0;
            int opPo = -1;
            while (op < OPERATORS.length && 
                (opPo = constraint.indexOf(OPERATORS[op])) < 0)
                op++;
            if (opPo < 0) {
                if (repair) continue; //was IllegalArgumentException
                else throw new SimpleException(EDStatic.queryError +
                    "No operator found in constraint=\"" + constraint + "\".");
            }

            //request uses sequenceName.dataVarName notation?
            String tName = constraint.substring(0, opPo);
            int period = tName.indexOf('.');
            if (period > 0 && tName.substring(0, period).equals(SEQUENCE_NAME)) 
                tName = tName.substring(period + 1);

            //is it a valid destinationName?
            int dvi = String2.indexOf(dataVariableDestinationNames(), tName);
            if (dvi < 0) {
                if (repair) continue;
                else throw new SimpleException(EDStatic.queryError +
                    "Unrecognized constraint variable=\"" + tName + "\""
                    //+ "\nValid=" + String2.toCSSVString(dataVariableDestionNames())
                    );
            }

            EDV conEdv = dataVariables[dvi];
            constraintVariables.add(tName);
            constraintOps.add(OPERATORS[op]);
            String tValue = constraint.substring(opPo + OPERATORS[op].length());
            constraintValues.add(tValue);
            if (debugMode) String2.log(">>constraint: " + tName + OPERATORS[op] + tValue);

            //convert <time><op><isoString> to <time><op><epochSeconds>   
            if (conEdv instanceof EDVTimeStamp) {
                //this isn't precise!!!   should it be required??? or forbidden???
                if (debugMode) String2.log(">>isTimeStamp=true");
                if (tValue.startsWith("\"") && tValue.endsWith("\"")) { 
                    tValue = String2.fromJson(tValue);
                    constraintValues.set(constraintValues.size() - 1, tValue);
                }                

                //if not for regex, convert isoString to epochSeconds 
                if (OPERATORS[op] != PrimitiveArray.REGEX_OP) {
                    if (Calendar2.isIsoDate(tValue)) {
                        double valueD = repair? Calendar2.safeIsoStringToEpochSeconds(tValue) :
                            Calendar2.isoStringToEpochSeconds(tValue);
                        constraintValues.set(constraintValues.size() - 1,
                            "" + valueD);
                        if (debugMode) String2.log(">> TIME CONSTRAINT converted in parseUserDapQuery: " + tValue + " -> " + valueD);

                    } else if (tValue.startsWith("now")) {
                        if (repair)
                            constraintValues.set(constraintValues.size() - 1,
                                "" + Calendar2.safeNowStringToEpochSeconds(tValue, 
                                        (double)Math2.hiDiv(System.currentTimeMillis(), 1000)));
                        else constraintValues.set(constraintValues.size() - 1,
                                "" + Calendar2.nowStringToEpochSeconds(tValue));

                    } else {
                        //it must be a number (epochSeconds)
                        //test that value=NaN must use NaN or "", not somthing just a badly formatted number
                        double td = String2.parseDouble(tValue);
                        if (Double.isNaN(td) && !tValue.equals("NaN") && !tValue.equals("")) {
                            if (repair) {
                                tValue = "NaN";
                                constraintValues.set(constraintValues.size() - 1, tValue);
                            } else {
                                throw new SimpleException(EDStatic.queryError +
                                    "Test for missing time values must use \"NaN\" or \"\", not value=\"" + tValue + "\".");
                            }
                        } //else constraintValues already ok
                    }
                }

            } else if (conEdv.destinationDataTypeClass() == String.class) {

                //String variables must have " around constraintValues
                if ((tValue.startsWith("\"") && tValue.endsWith("\"")) || repair) {
                    //repair if needed
                    if (!tValue.startsWith("\""))
                        tValue = "\"" + tValue;
                    if (!tValue.endsWith("\""))
                        tValue = tValue + "\"";

                    //decode
                    tValue = String2.fromJson(tValue);
                    constraintValues.set(constraintValues.size() - 1, tValue);

                } else {
                    throw new SimpleException(EDStatic.queryError +
                        "For constraints of String variables, " +
                        "the right-hand-side value must be surrounded by double quotes.\n" +
                        "Bad constraint: " + constraint);
                }

            } else {
                //numeric variables

                //if op=regex, value must have "'s around it
                if (OPERATORS[op] == PrimitiveArray.REGEX_OP) {
                    if ((tValue.startsWith("\"") && tValue.endsWith("\"")) || repair) {
                        //repair if needed
                        if (!tValue.startsWith("\""))
                            tValue = "\"" + tValue;
                        if (!tValue.endsWith("\""))
                            tValue = tValue + "\"";

                        //decode
                        tValue = String2.fromJson(tValue);
                        constraintValues.set(constraintValues.size() - 1, tValue);
                    } else {
                        throw new SimpleException(EDStatic.queryError +
                            "For =~ constraints of numeric variables, " +
                            "the right-hand-side value must be surrounded by double quotes.\n" +
                            "Bad constraint: " + constraint);
                    }

                } else {
                    //if op!=regex, numbers must NOT have "'s around them
                    if (tValue.startsWith("\"") || tValue.endsWith("\"")) {
                        if (repair) {
                            if (tValue.startsWith("\""))
                                tValue = tValue.substring(1);
                            if (tValue.endsWith("\""))
                                tValue = tValue.substring(0, tValue.length() - 1);
                            constraintValues.set(constraintValues.size() - 1, tValue);
                        } else {
                            throw new SimpleException(EDStatic.queryError +
                                "For non =~ constraints of numeric variables, " +
                                "the right-hand-side value must not be surrounded by double quotes.\n" +
                                "Bad constraint: " + constraint);
                        }
                    }

                    //test of value=NaN must use "NaN", not somthing just a badly formatted number
                    double td = String2.parseDouble(tValue);
                    if (Double.isNaN(td) && !tValue.equals("NaN")) {
                        if (repair) {
                            tValue = "NaN";
                            constraintValues.set(constraintValues.size() - 1, tValue);
                        } else {
                            throw new SimpleException(EDStatic.queryError +
                                "Numeric tests of NaN must use \"NaN\", not value=\"" + tValue + "\".");
                        }
                    }
                }
            }
        }

        if (debugMode) {
            String2.log("  Output from parseUserDapQuery:" +
                "\n    resultsVariables=" + resultsVariables +
                "\n    constraintVariables=" + constraintVariables +
                "\n    constraintOps=" + constraintOps +
                "\n    constraintValues=" + constraintValues);
        }
    }

    /**
     * As a service to getDataForDapQuery implementations,  
     * given the userDapQuery (the DESTINATION (not source) request),
     * this fills in the requestedMin and requestedMax arrays 
     * [0=lon, 1=lat, 2=alt|depth, 3=time] (in destination units, may be NaN).
     * [2] will be altIndex if >=0 or depthIndex if >= 0.
     *
     * <p>If a given variable (e.g., altIndex and depthIndex) isn't defined, 
     * the requestedMin/Max will be NaN.
     *
     * @param userDapQuery  a userDapQuery (usually including longitude,latitude,
     *    altitude,time variables), after the '?', still percentEncoded (shouldn't be null).
     * @param useVariablesDestinationMinMax if true, the results are further constrained
     *    by the LLAT variable's destinationMin and destinationMax.
     * @param requestedMin will catch the minimum LLAT constraints;
     *     NaN if a given var doesn't exist or no info or constraints. 
     * @param requestedMax will catch the maximum LLAT constraints;
     *     NaN if a given var doesn't exist or no info or constraints. 
     * @throws Throwable if trouble (e.g., a requestedMin &gt; a requestedMax)
     */
    public void getRequestedDestinationMinMax(String userDapQuery, 
        boolean useVariablesDestinationMinMax,
        double requestedMin[], double requestedMax[]) throws Throwable {

        Arrays.fill(requestedMin, Double.NaN);
        Arrays.fill(requestedMax, Double.NaN);

        //parseUserDapQuery
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        parseUserDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            false);

        //try to get the LLAT variables
        EDV edv[] = new EDV[]{
            lonIndex  >= 0? dataVariables[lonIndex] : null,
            latIndex  >= 0? dataVariables[latIndex] : null,
            altIndex  >= 0? dataVariables[altIndex] : 
              depthIndex >= 0? dataVariables[depthIndex] : null,
            timeIndex >= 0? dataVariables[timeIndex] : null};

        //go through the constraints
        int nConstraints = constraintVariables.size();
        for (int constraint = 0; constraint < nConstraints; constraint++) {

            String destName = constraintVariables.get(constraint);
            int conDVI = String2.indexOf(dataVariableDestinationNames(), destName);
            if (conDVI < 0)
                throw new SimpleException(EDStatic.queryError +
                    "constraint variable=" + destName + " wasn't found.");
            String op = constraintOps.get(constraint);
            String conValue = constraintValues.get(constraint);
            double conValueD = String2.parseDouble(conValue); //ok for times: they are epochSeconds

            //constraint affects which of LLAT
            int index4 = -1;
            if      (conDVI == lonIndex)   index4 = 0;
            else if (conDVI == latIndex)   index4 = 1;
            else if (conDVI == altIndex)   index4 = 2;
            else if (conDVI == depthIndex) index4 = 2;
            else if (conDVI == timeIndex)  index4 = 3;
            //String2.log("index4:" + index4 + " " + op + " " + conValueD);

            if (index4 >= 0) {
                if (op.equals("=") || op.equals(">=") || op.equals(">")) 
                    requestedMin[index4] = Double.isNaN(requestedMin[index4])? conValueD :
                        Math.max(requestedMin[index4], conValueD); //yes, max
                if (op.equals("=") || op.equals("<=") || op.equals("<")) 
                    requestedMax[index4] = Double.isNaN(requestedMax[index4])? conValueD :
                        Math.min(requestedMax[index4], conValueD); //yes, min
            }
        }
        
        //invalid user constraints?
        for (int i = 0; i < 4; i++) {
            if (edv[i] == null) 
                continue;
            String tName = edv[i].destinationName();
            if (reallyVerbose) String2.log("  " + tName + " requested min=" + requestedMin[i] + " max=" + requestedMax[i]);
            if (!Double.isNaN(requestedMin[i]) && !Double.isNaN(requestedMax[i])) {
                if (requestedMin[i] > requestedMax[i]) {
                    String minS = i == 3? Calendar2.epochSecondsToIsoStringT(requestedMin[3]) : "" + requestedMin[i];
                    String maxS = i == 3? Calendar2.epochSecondsToIsoStringT(requestedMax[3]) : "" + requestedMax[i];
                    throw new SimpleException(EDStatic.queryError +
                        "The requested " + tName + " min=" + minS +
                        " is greater than the requested max=" + maxS + ".");
                }
            }
        }

       
        //use the variable's destinationMin, max?
        if (!useVariablesDestinationMinMax)
            return;

        for (int i = 0; i < 4; i++) {
            if (edv[i] != null) {
                double tMin = edv[i].destinationMin();
                if (Double.isNaN(tMin)) {
                } else {
                    if (Double.isNaN(requestedMin[i]))
                        requestedMin[i] = tMin;
                    else requestedMin[i] = Math.max(tMin, requestedMin[i]);
                }
                double tMax = edv[i].destinationMax();
                if (Double.isNaN(tMax)) {
                } else {
                    if (Double.isNaN(requestedMax[i]))
                        requestedMax[i] = tMax;
                    else requestedMax[i] = Math.min(tMax, requestedMax[i]);
                }
            }
        }
        if (Double.isNaN(requestedMax[3]))   
            requestedMax[3] = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu()) + 
                Calendar2.SECONDS_PER_HOUR; //now + 1 hr     
            //???is this trouble for models which predict future?  (are any models EDDTables?)

        //recheck. If invalid now, it's No Data due to variable's destinationMin/Max
        for (int i = 0; i < 4; i++) {
            if (edv[i] == null) 
                continue;
            String tName = edv[i].destinationName();
            if (reallyVerbose) String2.log("  " + tName + " requested min=" + requestedMin[i] + " max=" + requestedMax[i]);
            if (!Double.isNaN(requestedMin[i]) && !Double.isNaN(requestedMax[i])) {
                if (requestedMin[i] > requestedMax[i]) {
                    String minS = i == 3? Calendar2.epochSecondsToIsoStringT(requestedMin[3]) : "" + requestedMin[i];
                    String maxS = i == 3? Calendar2.epochSecondsToIsoStringT(requestedMax[3]) : "" + requestedMax[i];
                    throw new SimpleException(MustBe.THERE_IS_NO_DATA +
                        "\n(" + tName + " min=" + minS +
                        " is greater than max=" + maxS + ")");
                }
            }
        }

    }

    /**
     * This formats the resultsVariables and constraints as an OPeNDAP DAP-style query.
     * This does no checking of the validity of the resultsVariable or 
     * constraintVariabe names, so, e.g., axisVariables may be referred to by
     * the sourceNames or the destinationNames, so this method can be used in various ways.
     *
     * @param resultsVariables  
     * @param constraintVariables 
     * @param constraintOps   are usually the standard OPERATORS
     *     (or ~=, the non-standard regex op)
     * @param constraintValues 
     * @return the OPeNDAP DAP-style query,
     *   <tt>var1,var2,var3&amp;var4=value4&amp;var5&amp;gt;=value5</tt>.
     *   See parseUserDapQuery.
     */
    public static String formatAsDapQuery(String resultsVariables[],
        String constraintVariables[], String constraintOps[], String constraintValues[]) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultsVariables.length; i++) {
            if (i > 0) 
                sb.append(',');
            sb.append(resultsVariables[i]);
        }

        //do I need to put quotes around String constraintValues???
        for (int i = 0; i < constraintVariables.length; i++) 
            sb.append("&" + constraintVariables[i] + constraintOps[i] + 
                constraintValues[i]);  //!!!bug??? Strings should be String2.toJson() so within " "
        
        return sb.toString();
    }

    /** 
     * This is similar to formatAsDapQuery, but properly SSR.percentEncodes the parts.
     * 
     */
    public static String formatAsPercentEncodedDapQuery(String resultsVariables[],
        String constraintVariables[], String constraintOps[], String constraintValues[]) 
        throws Throwable {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultsVariables.length; i++) {
            if (i > 0) 
                sb.append(',');
            sb.append(SSR.minimalPercentEncode(resultsVariables[i]));
        }

        for (int i = 0; i < constraintVariables.length; i++) 
            sb.append("&" + 
                SSR.minimalPercentEncode(constraintVariables[i]) + 
                PPE_OPERATORS[String2.indexOf(OPERATORS, constraintOps[i])] + 
                SSR.minimalPercentEncode(constraintValues[i])); //!!!bug??? Strings should be String2.toJson() so within " "
        
        return sb.toString();
    }


    /**
     * This responds to an OPeNDAP-style query.
     *
     * @param request may be null. Currently, it isn't used.
     *   (It is passed to respondToGraphQuery, but not used).
     * @param response may be used by .subset to redirect the response
     *   (if not .subset request, it may be null).
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but in unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery the part of the user's request after the '?', 
     *   still percentEncoded (shouldn't be null).
     * @param outputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     <br>This doesn't call out.close() at the end; the caller MUST call out.close().
     * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.
     * @param fileTypeName the fileTypeName for the new file  (e.g., .largePng)
     * @throws Throwable if trouble
     */
    public void respondToDapQuery(HttpServletRequest request, 
        HttpServletResponse response,
        String loggedInAs,
        String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable {

        if (reallyVerbose) String2.log("\n//** EDDTable.respondToDapQuery..." +
            "\n  datasetID=" + datasetID +
            "\n  userDapQuery=" + userDapQuery +
            "\n  dir=" + dir +
            "\n  fileName=" + fileName +
            "\n  fileTypeName=" + fileTypeName);
        long makeTime = System.currentTimeMillis();
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //.das, .dds, and .html requests can be handled without getting data 
        if (fileTypeName.equals(".das")) {
            //.das is always the same regardless of the userDapQuery.
            //(That's what THREDDS does -- DAP 2.0 7.2.1 is vague.
            //THREDDs doesn't even object if userDapQuery is invalid.)
            Table table = makeEmptyDestinationTable(requestUrl, "", true); //as if userDapQuery was for everything

            //DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
            table.saveAsDAS(outputStreamSource.outputStream("ISO-8859-1"), 
                SEQUENCE_NAME);
            return;
        }
        if (fileTypeName.equals(".dds")) {
            Table table = makeEmptyDestinationTable(requestUrl, userDapQuery, false);
            //DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
            table.saveAsDDS(outputStreamSource.outputStream("ISO-8859-1"),
                SEQUENCE_NAME);
            return;
        }

        if (fileTypeName.equals(".fgdc")) {
            if (accessibleViaFGDC.length() == 0) {                
                File2.copy(datasetDir() + datasetID + fgdcSuffix + ".xml", 
                    outputStreamSource.outputStream("UTF-8"));
            } else {
                throw new SimpleException(accessibleViaFGDC);
            }
            return;
        }
        
        if (fileTypeName.equals(".graph")) {
            respondToGraphQuery(request, loggedInAs, requestUrl, userDapQuery, outputStreamSource,
                dir, fileName, fileTypeName);
            return;
        }
        
        if (fileTypeName.equals(".html")) {
            //first
            Table table = makeEmptyDestinationTable(requestUrl, "", true); //das is as if no constraint
            //it is important that this use outputStreamSource so stream is compressed (if possible)
            //DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible unicode
            OutputStream out = outputStreamSource.outputStream("UTF-8");
            Writer writer = new OutputStreamWriter(out, "UTF-8"); 
            writer.write(EDStatic.startHeadHtml(tErddapUrl,  
                title() + " - " + EDStatic.daf));
            writer.write("\n" + rssHeadLink(loggedInAs));
            writer.write("\n</head>\n");
            writer.write(EDStatic.startBodyHtml(loggedInAs));
            writer.write("\n");
            writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)));   //this is a link to a script
            writer.write(HtmlWidgets.dragDropScript(EDStatic.imageDirUrl(loggedInAs)));      //this is a link to a script
            writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
            try {
                writer.write(EDStatic.youAreHereWithHelp(loggedInAs, dapProtocol, 
                    EDStatic.daf, 
                    EDStatic.dafTableHtml + 
                    "<p>" + EDStatic.EDDTableDownloadDataHtml +
                    "</ol>\n" +
                    EDStatic.dafTableBypass));
                writeHtmlDatasetInfo(loggedInAs, writer, true, false, true, userDapQuery, "");
                if (userDapQuery.length() == 0) 
                    userDapQuery = defaultDataQuery(); //after writeHtmlDatasetInfo and before writeDapHtmlForm
                writeDapHtmlForm(loggedInAs, userDapQuery, writer);
                writer.write("<hr>\n");
                writer.write("<h2>" + EDStatic.dasTitle + "</h2>\n" +
                    "<pre>\n");
                table.writeDAS(writer, SEQUENCE_NAME, true); //useful so search engines find all relevant words
                writer.write("</pre>\n");
                writer.write("<br>&nbsp;\n");
                writer.write("<hr>\n");
                writeGeneralDapHtmlInstructions(tErddapUrl, writer, false); 
                if (EDStatic.displayDiagnosticInfo) 
                    EDStatic.writeDiagnosticInfoHtml(writer);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
            }
            writer.write(EDStatic.endBodyHtml(tErddapUrl));
            writer.write("\n</html>\n");
            writer.flush(); //essential
            return;
        }

        if (fileTypeName.endsWith("Info") && 
            (fileTypeName.equals(".smallPngInfo") ||
             fileTypeName.equals(".pngInfo") ||
             fileTypeName.equals(".largePngInfo") ||
             fileTypeName.equals(".smallPdfInfo") ||
             fileTypeName.equals(".pdfInfo") ||
             fileTypeName.equals(".largePdfInfo"))) {
            //try to readPngInfo (if fromErddap, this saves it to local file)
            //(if info not available, this will fail)
            String imageFileType = fileTypeName.substring(0, fileTypeName.length() - 4);
            Object[] pngInfo = readPngInfo(loggedInAs, userDapQuery, imageFileType);
            if (pngInfo == null) 
                throw new SimpleException(EDStatic.errorFileNotFoundImage);

            //ok, copy it  (and don't close the outputStream)
            File2.copy(getPngInfoFileName(loggedInAs, userDapQuery, imageFileType),
                outputStreamSource.outputStream("UTF-8"));
            return;
        }

        if (fileTypeName.equals(".iso19115")) {
            if (accessibleViaISO19115.length() == 0) {                
                File2.copy(datasetDir() + datasetID + iso19115Suffix + ".xml", 
                    outputStreamSource.outputStream("UTF-8"));
            } else {
                throw new SimpleException(accessibleViaFGDC);
            }
            return;
        }
        
        if (fileTypeName.equals(".subset")) {
            respondToSubsetQuery(request, response, loggedInAs, 
                requestUrl, userDapQuery, outputStreamSource,
                dir, fileName, fileTypeName);
            return;
        }
        

        //*** get the data and write to a tableWriter
        TableWriter tableWriter = null;
        TableWriterAllWithMetadata twawm = null;
        if (fileTypeName.equals(".asc")) 
            tableWriter = new TableWriterDodsAscii(outputStreamSource, SEQUENCE_NAME);
        else if (fileTypeName.equals(".csv")) 
            tableWriter = new TableWriterSeparatedValue(outputStreamSource, ",", true, '2', "NaN");
        else if (fileTypeName.equals(".csvp")) 
            tableWriter = new TableWriterSeparatedValue(outputStreamSource, ",", true, '(', "NaN");
        else if (fileTypeName.equals(".dods")) 
            tableWriter = new TableWriterDods(outputStreamSource, SEQUENCE_NAME);
        else if (fileTypeName.equals(".esriCsv")) 
            tableWriter = new TableWriterEsriCsv(outputStreamSource);
        else if (fileTypeName.equals(".geoJson") || 
                 fileTypeName.equals(".json")) {
            //did query include &.jsonp= ?
            String parts[] = getUserQueryParts(userDapQuery); //decoded
            String jsonp = String2.stringStartsWith(parts, ".jsonp="); //may be null
            if (jsonp != null) {
                jsonp = jsonp.substring(7);
                if (!String2.isJsonpNameSafe(jsonp))
                    throw new SimpleException(EDStatic.errorJsonpFunctionName);
            }
            if (fileTypeName.equals(".geoJson"))
                tableWriter = new TableWriterGeoJson(outputStreamSource, jsonp);
            if (fileTypeName.equals(".json"))
                tableWriter = new TableWriterJson(outputStreamSource, jsonp, true);
        } else if (fileTypeName.equals(".htmlTable")) 
            tableWriter = new TableWriterHtmlTable(loggedInAs, outputStreamSource, 
                true, fileName, false, "", "", true, true, -1);
        else if (fileTypeName.equals(".mat")) { 
            twawm = new TableWriterAllWithMetadata(dir, fileName);  //used after getDataForDapQuery below...
            tableWriter = twawm;
        } else if (fileTypeName.equals(".odvTxt")) { 
            //ensure there is longitude, latitude, time data in the request (else it is useless in ODV)
            StringArray resultsVariables = new StringArray();
            parseUserDapQuery(userDapQuery, resultsVariables,
                new StringArray(), new StringArray(), new StringArray(), false);
            if (resultsVariables.indexOf(EDV.LON_NAME)  < 0 ||
                resultsVariables.indexOf(EDV.LAT_NAME)  < 0 ||
                resultsVariables.indexOf(EDV.TIME_NAME) < 0)
                throw new SimpleException(EDStatic.errorOdvLLTTable);
            twawm = new TableWriterAllWithMetadata(dir, fileName);  //used after getDataForDapQuery below...
            tableWriter = twawm;
        } else if (fileTypeName.equals(".tsv")) 
            tableWriter = new TableWriterSeparatedValue(outputStreamSource, "\t", false, '2', "NaN");
        else if (fileTypeName.equals(".tsvp")) 
            tableWriter = new TableWriterSeparatedValue(outputStreamSource, "\t", false, '(', "NaN");
        else if (fileTypeName.equals(".xhtml")) 
            tableWriter = new TableWriterHtmlTable(loggedInAs, outputStreamSource, 
                true, fileName, true, "", "", true, true, -1);

        if (tableWriter != null) {
            tableWriter = encloseTableWriter(dir, fileName, tableWriter, userDapQuery);

            if (handleViaFixedOrSubsetVariables(loggedInAs, requestUrl, userDapQuery, tableWriter)) {}
            else             getDataForDapQuery(loggedInAs, requestUrl, userDapQuery, tableWriter);

            //special case
            if (fileTypeName.equals(".mat")) 
                saveAsMatlab(outputStreamSource, twawm, datasetID);
            else if (fileTypeName.equals(".odvTxt")) 
                saveAsODV(outputStreamSource, twawm, datasetID, publicSourceUrl(), 
                    infoUrl());
            return;
        }

        //*** make a file (then copy it to outputStream)
        //nc files are handled this way because .ncHeader .ncCFHeader, .ncCFMAHeader need to call
        //  NcHelper.dumpString(aRealFile, false). 
        String fileTypeExtension = fileTypeExtension(fileTypeName);
        String fullName = dir + fileName + fileTypeExtension;
        //Normally, this is cacheDirectory and it already exists,
        //  but my testing environment (2+ things running) may have removed it.
        File2.makeDirectory(dir);

        //does the file already exist?
        //if .ncXHeader, make sure the .nc file exists (and it is the better file to cache)
        //  [! not really! it will have a different hash/fileName]
        boolean ncXHeader = 
            fileTypeName.equals(".ncHeader") ||
            fileTypeName.equals(".ncCFHeader") ||
            fileTypeName.equals(".ncCFMAHeader");
        String cacheFullName = ncXHeader? dir + fileName + ".nc" : fullName;
        int random = Math2.random(Integer.MAX_VALUE);

        //thread-safe creation of the file 
        //(If there are almost simultaneous requests for the same one, only one thread will make it.)
        synchronized(String2.canonical(cacheFullName)) {

            if (File2.isFile(cacheFullName)) { //don't 'touch()'; files for latest data will change
                if (verbose) String2.log("  reusing cached " + cacheFullName);

            } else if (fileTypeName.equals(".nc") || 
                       fileTypeName.equals(".ncHeader")) {
                //if .ncHeader, make sure the .nc file exists 
                //(and it is the better file to cache)
                twawm = getTwawmForDapQuery(loggedInAs, requestUrl, userDapQuery);

                saveAsFlatNc(cacheFullName, twawm); //internally, it writes to temp file, then renames to cacheFullName

                File2.isFile(cacheFullName, 5); //for possible waiting thread, wait till file is visible via operating system

            } else if (fileTypeName.equals(".ncCF") ||
                       fileTypeName.equals(".ncCFHeader") ||
                       fileTypeName.equals(".ncCFMA") ||
                       fileTypeName.equals(".ncCFMAHeader")) {
                //quick reject?
                if (accessibleViaNcCF().length() > 0)
                    throw new SimpleException(accessibleViaNcCF);

                //check that query includes required variables
                if (userDapQuery == null) userDapQuery = "";
                String varNames = userDapQuery.substring(0, (userDapQuery + "&").indexOf('&'));
                if (varNames.length() == 0) {
                    //ok since all vars are requested and we know dataset has required variables
                } else {
                    //queries must include cf_role=timeseries_id var (or profile_id or trajectory_id)
                    //queries must include longitude, latitude, time (and altitude or depth if in dataset)
                    StringBuilder addVars = new StringBuilder();
                    String varList[] = StringArray.arrayFromCSV(varNames); 
                    for (int v = 0; v < requiredCfRequestVariables.length; v++) {
                        if (String2.indexOf(varList, requiredCfRequestVariables[v]) < 0)
                            addVars.append(requiredCfRequestVariables[v] + ",");
                            //throw new SimpleException(EDStatic.queryError +
                            //    ".ncCF queries for this dataset must include all of these variables: " +
                            //    String2.toCSSVString(requiredCfRequestVariables) + ".");
                    }
                    //add missing vars to beginning of userDapQuery (they're outer vars, so beginning is good)
                    userDapQuery = addVars.toString() + userDapQuery;

                    //query must include at least one non-outer variable
                    boolean ok = false;
                    for (int v = 0; v < varList.length; v++) {
                        if (String2.indexOf(outerCfVariables, varList[v]) < 0) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        throw new SimpleException(EDStatic.queryError +
                            ".ncCF and .ncCFMA queries for this dataset must at least one variable not on this list : " +
                            String2.toCSSVString(outerCfVariables) + ".");                    
                    }
                }


                //get the data
                twawm = getTwawmForDapQuery(loggedInAs, requestUrl, userDapQuery);
                //if (debug) String2.log("\n>>after twawm, globalAttributes=\n" + twawm.globalAttributes());

                //make .ncCF or .ncCFMA file
                boolean nodcMode = fileTypeName.equals(".ncCFMA") ||
                                   fileTypeName.equals(".ncCFMAHeader");
                String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
                if (CDM_POINT.equals(cdmType)) 
                    saveAsNcCF0(cacheFullName, twawm); 
                else if (CDM_TIMESERIES.equals(cdmType) ||
                    CDM_PROFILE.equals(cdmType) ||
                    CDM_TRAJECTORY.equals(cdmType))
                    saveAsNcCF1(nodcMode, cacheFullName, twawm); 
                else if (CDM_TIMESERIESPROFILE.equals(cdmType) ||
                         CDM_TRAJECTORYPROFILE.equals(cdmType))
                    saveAsNcCF2(nodcMode, cacheFullName, twawm); 
                else  //shouldn't happen, since accessibleViaNcCF checks this
                    throw new SimpleException("unexpected cdm_data_type=" + cdmType);

                File2.isFile(cacheFullName, 5); //for possible waiting thread, wait till file is visible via operating system
                
            } else {
                //all other types
                //create random file; and if error, only random file will be created
                FileOutputStream fos = new FileOutputStream(cacheFullName + random);
                OutputStreamSourceSimple osss = new OutputStreamSourceSimple(fos);
                boolean ok; 
                if (fileTypeName.equals(".kml")) {
                    ok = saveAsKml(loggedInAs, requestUrl, userDapQuery, dir, fileName, 
                        osss);

                } else if (String2.indexOf(imageFileTypeNames, fileTypeName) >= 0) {
                    //pdf and png  //do last so .kml caught above
                    ok = saveAsImage(loggedInAs, requestUrl, userDapQuery, dir, fileName, 
                        osss, fileTypeName);

                } else { 
                    fos.close();
                    File2.delete(cacheFullName + random);
                    throw new SimpleException(EDStatic.queryError +
                        MessageFormat.format(EDStatic.queryErrorFileType, fileTypeName));
                }

                fos.close();
                File2.rename(cacheFullName + random, cacheFullName); //make available in an instant
                if (!ok) //make eligible to be removed from cache in 5 minutes
                    File2.touch(cacheFullName, 
                        Math.max(0, EDStatic.cacheMillis - 5 * Calendar2.MILLIS_PER_MINUTE));

                File2.isFile(cacheFullName, 5); //for possible waiting thread, wait till file is visible via operating system
            } 
        }

        //if ncXHeader (.ncHeader, .ncCFHeader, .ncCFMAHeader), create the underlying .nc file
        if (ncXHeader) {
            //thread-safe creation of the file 
            //(If there are almost simultaneous requests for the same one, only one thread will make it.)
            synchronized(String2.canonical(fullName)) {
                if (!File2.isFile(fullName)) {
                    String error = String2.writeToFile(fullName + random, 
                        NcHelper.dumpString(cacheFullName, false)); //!!!this doesn't do anything to internal " in a String attribute value.
                    if (error.length() == 0) {
                        File2.rename(fullName + random, fullName); //make available in an instant
                        File2.isFile(fullName, 5); //for possible waiting thread, wait till file is visible via operating system
                    } else {
                        throw new RuntimeException(error);
                    }
                }
            }
        }

        //copy file to outputStream
        //(I delayed getting actual outputStream as long as possible.)
        if (!File2.copy(fullName, outputStreamSource.outputStream(
            ncXHeader? "UTF-8" : 
            fileTypeName.equals(".kml")? "UTF-8" : 
            ""))) { 
            //outputStream contentType already set,
            //so I can't go back to html and display error message
            //note than the message is thrown if user cancels the transmission; so don't email to me
            String2.log("Error while transmitting " + fileName + fileTypeExtension);
        }

        //done
        if (reallyVerbose) String2.log(
            "\n\\\\** EDDTable.respondToDapQuery finished successfully. TIME=" +
            (System.currentTimeMillis() - makeTime));

    }

    /**
     * If a part of the userDapQuery is a function (e.g., distinct(), orderBy("..."))
     * this wraps tableWriter in (e.g.) TableWriterDistinct
     * in the order they occur in userDapQuery.
     *
     * @param dir a private cache directory for storing the intermediate files
     * @param fileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     * @param tableWriter the final tableWriter 
     * @param userDapQuery
     * @return the (possibly) wrapped tableWriter
     */
    protected TableWriter encloseTableWriter(String dir, String fileName, 
        TableWriter tableWriter, String userDapQuery) throws Throwable {
        
        //work backwards through parts so tableWriters are added in correct order
        String[] parts = getUserQueryParts(userDapQuery); //decoded
        for (int part = parts.length - 1; part >= 0; part--) {
            String p = parts[part];
            if (p.equals("distinct()")) {
                tableWriter = new TableWriterDistinct(dir, fileName, tableWriter);
            } else if (p.startsWith("orderBy(\"") && p.endsWith("\")")) {
                TableWriterOrderBy twob = new TableWriterOrderBy(dir, fileName, tableWriter, 
                    p.substring(9, p.length() - 2));
                tableWriter = twob;
                //minimal test: ensure orderBy columns are valid column names
                for (int ob = 0; ob < twob.orderBy.length; ob++) {
                    if (String2.indexOf(dataVariableDestinationNames(), twob.orderBy[ob]) < 0)
                        throw new SimpleException(EDStatic.queryError +
                            "'orderBy' variable=" + twob.orderBy[ob] + " isn't in the dataset.");
                }
            } else if (p.startsWith("orderByMax(\"") && p.endsWith("\")")) {
                TableWriterOrderByMax twobm = new TableWriterOrderByMax(dir, 
                    fileName, tableWriter, p.substring(12, p.length() - 2));
                tableWriter = twobm;
                //minimal test: ensure orderBy columns are valid column names
                for (int ob = 0; ob < twobm.orderBy.length; ob++) {
                    if (String2.indexOf(dataVariableDestinationNames(), twobm.orderBy[ob]) < 0)
                        throw new SimpleException(EDStatic.queryError +
                            "'orderByMax' variable=" + twobm.orderBy[ob] + " isn't in the dataset.");
                }
            } else if (p.startsWith("orderByMin(\"") && p.endsWith("\")")) {
                TableWriterOrderByMin twobm = new TableWriterOrderByMin(dir, 
                    fileName, tableWriter, p.substring(12, p.length() - 2));
                tableWriter = twobm;
                //minimal test: ensure orderBy columns are valid column names
                for (int ob = 0; ob < twobm.orderBy.length; ob++) {
                    if (String2.indexOf(dataVariableDestinationNames(), twobm.orderBy[ob]) < 0)
                        throw new SimpleException(EDStatic.queryError +
                            "'orderByMin' variable=" + twobm.orderBy[ob] + " isn't in the dataset.");
                }
            } else if (p.startsWith("orderByMinMax(\"") && p.endsWith("\")")) {
                TableWriterOrderByMinMax twobm = new TableWriterOrderByMinMax(dir, 
                    fileName, tableWriter, p.substring(15, p.length() - 2));
                tableWriter = twobm;
                //minimal test: ensure orderBy columns are valid column names
                for (int ob = 0; ob < twobm.orderBy.length; ob++) {
                    if (String2.indexOf(dataVariableDestinationNames(), twobm.orderBy[ob]) < 0)
                        throw new SimpleException(EDStatic.queryError +
                            "'orderByMinMax' variable=" + twobm.orderBy[ob] + " isn't in the dataset.");
                }
            } else if (p.startsWith("units(\"") && p.endsWith("\")")) {
                String fromUnits = EDStatic.units_standard;
                String toUnits = p.substring(7, p.length() - 2);
                TableWriterUnits.checkFromToUnits(fromUnits, toUnits);
                if (!fromUnits.equals(toUnits)) {
                    TableWriterUnits twu = new TableWriterUnits(tableWriter, 
                        fromUnits, toUnits);
                    tableWriter = twu;
                }
            }
        }
        return tableWriter;
    }

    /**
     * This makes an empty table (with destination columns, but without rows) 
     * corresponding to the request.
     *
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
     * @param withAttributes
     * @return an empty table (with columns, but without rows) 
     *    corresponding to the request
     * @throws Throwable if trouble
     */
    protected Table makeEmptyDestinationTable(String requestUrl, String userDapQuery, 
        boolean withAttributes) throws Throwable {
        if (reallyVerbose) String2.log("  makeEmptyDestinationTable...");

        //pick the query apart
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        parseUserDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            false);

        //make the table
        //attributes are added in standardizeResultsTable
        Table table = new Table();        
        if (withAttributes) {
            setResponseGlobalAttributes(requestUrl, userDapQuery, table);

            //pre 2013-05-28 was  
            //table.globalAttributes().set(combinedGlobalAttributes); //make a copy
            ////fix up global attributes  (always to a local COPY of global attributes)
            //EDD.addToHistory(table.globalAttributes(), publicSourceUrl());
            //EDD.addToHistory(table.globalAttributes(), 
            //    EDStatic.baseUrl + requestUrl); //userDapQuery is irrelevant
        }

        //add the columns
        for (int col = 0; col < resultsVariables.size(); col++) {
            int dv = String2.indexOf(dataVariableDestinationNames(), resultsVariables.get(col));
            EDV edv = dataVariables[dv]; 
            Attributes atts = new Attributes();
            if (withAttributes) 
                atts.set(edv.combinedAttributes()); //make a copy
            table.addColumn(col, edv.destinationName(), 
                PrimitiveArray.factory(edv.destinationDataTypeClass(), 1, false),
                atts); 
        }

        //can't setActualRangeAndBoundingBox because no data
        //so remove actual_range and bounding box attributes??? 
        //  no, they are dealt with in standardizeResultsTable

        if (reallyVerbose) String2.log("  makeEmptyDestinationTable done.");
        return table;
    }


    /**
     * This makes a .kml file.
     * The userDapQuery must include the EDV.LON_NAME and EDV.LAT_NAME columns 
     * (and preferably also EDV.ALT_NAME and EDV.TIME_NAME column) in the results variables.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl 
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
     * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.
     * @param outputStreamSource
     * @return true of written ok; false if exception occurred (and written on image)
     * @throws Throwable if trouble
     */
    protected boolean saveAsKml(String loggedInAs,
        String requestUrl, String userDapQuery,
        String dir, String fileName,
        OutputStreamSource outputStreamSource) throws Throwable {

        //before any work is done, 
        //  ensure LON_NAME and LAT_NAME are among resultsVariables
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        parseUserDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            false);
        if (resultsVariables.indexOf(EDV.LON_NAME) < 0 ||
            resultsVariables.indexOf(EDV.LAT_NAME) < 0)
            throw new SimpleException(EDStatic.queryError +
                MessageFormat.format(EDStatic.queryErrorLL, ".kml"));

        //get the table with all the data
        TableWriterAllWithMetadata twawm = getTwawmForDapQuery(
            loggedInAs, requestUrl, userDapQuery);
        Table table = twawm.cumulativeTable();
        twawm.releaseResources();
        table.convertToStandardMissingValues(); //so stored as NaNs

        //double check that lon and lat were found
        int lonCol  = table.findColumnNumber(EDV.LON_NAME);
        int latCol  = table.findColumnNumber(EDV.LAT_NAME);
        int altCol  = table.findColumnNumber(EDV.ALT_NAME); 
        int timeCol = table.findColumnNumber(EDV.TIME_NAME);
        EDVTime edvTime = timeIndex < 0? null : (EDVTime)dataVariables[timeIndex];
        if (lonCol < 0 || latCol < 0)
            throw new SimpleException(EDStatic.queryError +
                MessageFormat.format(EDStatic.queryErrorLL, ".kml"));

        //remember this may be many stations one time, or one station many times, or many/many
        //sort table by lat, lon, depth, then time (if possible)
        if (altCol >= 0 && timeCol >= 0)
            table.sort(new int[]{lonCol, latCol, altCol, timeCol}, new boolean[]{true, true, true, true});
        else if (timeCol >= 0)
            table.sort(new int[]{lonCol, latCol, timeCol}, new boolean[]{true, true, true});
        else if (altCol >= 0)
            table.sort(new int[]{lonCol, latCol, altCol},  new boolean[]{true, true, true});
        else 
            table.sort(new int[]{lonCol, latCol}, new boolean[]{true, true});
        //String2.log(table.toString("row", 10));

        //get lat and lon range (needed to create icon size and ensure there is data to be plotted)
        double minLon = Double.NaN, maxLon = Double.NaN, minLat = Double.NaN, maxLat = Double.NaN;
        if (lonCol >= 0) {
            double stats[] = table.getColumn(lonCol).calculateStats();
            minLon = stats[PrimitiveArray.STATS_MIN]; 
            maxLon = stats[PrimitiveArray.STATS_MAX];
        }
        if (latCol >= 0) {
            double stats[] = table.getColumn(latCol).calculateStats();
            minLat = stats[PrimitiveArray.STATS_MIN]; 
            maxLat = stats[PrimitiveArray.STATS_MAX];
        }
        if (Double.isNaN(minLon) || Double.isNaN(minLat))
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " " +
                EDStatic.noDataNoLL);
        double lonRange = maxLon - minLon;
        double latRange = maxLat - minLat;
        double maxRange = Math.max(lonRange, latRange);

        //get time range and prep moreTime constraints

        String moreTime = "";
        double minTime = Double.NaN, maxTime = Double.NaN;
        if (timeCol >= 0) {
            //time is in the response
            double stats[] = table.getColumn(timeCol).calculateStats();
            minTime = stats[PrimitiveArray.STATS_MIN]; 
            maxTime = stats[PrimitiveArray.STATS_MAX];
            if (!Double.isNaN(minTime)) { //there are time values
                //at least a week
                double tMinTime = Math.min(minTime, maxTime - 7 * Calendar2.SECONDS_PER_DAY);
                moreTime =  // >  <
                    "&time%3E=" + 
                    Calendar2.epochSecondsToLimitedIsoStringT(
                        edvTime.time_precision(), tMinTime, "NaN") +  
                    "&time%3C=" + 
                    Calendar2.epochSecondsToLimitedIsoStringT(
                        edvTime.time_precision(), maxTime, "NaN");
            }
        } else {
            //look for time in constraints
            for (int c = 0; c < constraintVariables.size(); c++) {
                if (EDV.TIME_NAME.equals(constraintVariables.get(c))) {
                    double tTime = String2.parseDouble(constraintValues.get(c));
                    char ch1 = constraintOps.get(c).charAt(0);
                    if (ch1 == '>' || ch1 == '=') 
                        minTime = Double.isNaN(minTime)? tTime : Math.min(minTime, tTime);
                    if (ch1 == '<' || ch1 == '=')
                        maxTime = Double.isNaN(maxTime)? tTime : Math.max(maxTime, tTime);
                }
            }
            if (Math2.isFinite(minTime)) 
                moreTime = "&time%3E=" +  
                    Calendar2.epochSecondsToLimitedIsoStringT(edvTime.time_precision(),
                        minTime - 7 * Calendar2.SECONDS_PER_DAY, "NaN");
            if (Math2.isFinite(maxTime)) 
                moreTime = "&time%3C=" + 
                    Calendar2.epochSecondsToLimitedIsoStringT(edvTime.time_precision(),
                        maxTime + 7 * Calendar2.SECONDS_PER_DAY, "NaN");
        }

        //Google Earth .kml
        //(getting the outputStream was delayed until actually needed)
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            outputStreamSource.outputStream("UTF-8"), "UTF-8"));

        //collect the units
        String columnUnits[] = new String[table.nColumns()];
        boolean columnIsString[] = new boolean[table.nColumns()];
        boolean columnIsTimeStamp[] = new boolean[table.nColumns()];
        String columnTimeFormat[] = new String[table.nColumns()];
        for (int col = 0; col < table.nColumns(); col++) {
            String units = table.columnAttributes(col).getString("units");
            //test isTimeStamp before prepending " "
            columnIsTimeStamp[col] = EDV.TIME_UNITS.equals(units) || EDV.TIME_UCUM_UNITS.equals(units); 
            //String2.log("col=" + col + " name=" + table.getColumnName(col) + " units=" + units + " isTimestamp=" + columnIsTimeStamp[col]);
            units = (units == null || units.equals(EDV.UNITLESS))? "" :
                " " + units;
            columnUnits[col] = units;
            columnIsString[col] = table.getColumn(col) instanceof StringArray;
            columnTimeFormat[col] = table.columnAttributes(col).getString(EDV.time_precision);
        }

        //based on kmz example from http://www.coriolis.eu.org/cdc/google_earth.htm
        //see copy in bob's c:/programs/kml/SE-LATEST-MONTH-STA.kml
        //kml docs: http://earth.google.com/kml/kml_tags.html
        //CDATA is necessary for url's with queries
        //kml/description docs recommend \n<br />
        String courtesy = institution().length() == 0? "" : 
            MessageFormat.format(EDStatic.imageDataCourtesyOf, institution());
        double iconSize = maxRange > 90? 1.2 : maxRange > 45? 1.0 : maxRange > 20? .8 : 
            maxRange > 10? .6 : maxRange > 5 ? .5 : .4;
        writer.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document>\n" +
            //human-friendly, but descriptive, <name>
            //name is used as link title -- leads to <description> 
            "  <name>" + XML.encodeAsXML(title()) + "</name>\n" +
            //<description> appears in balloon
            "  <description><![CDATA[" +
            XML.encodeAsXML(courtesy) + "\n<br />" +
            String2.replaceAll(XML.encodeAsXML(summary()), "\n", "\n<br />") +
            //link to download this dataset
            "\n<br />" +
            "<a href=\"" + XML.encodeAsHTMLAttribute(tErddapUrl + 
                "/tabledap/" + //don't use \n for the following lines
                datasetID + ".html?" + userDapQuery) + //already percentEncoded; XML.encodeAsXML isn't ok
            "\">View/download more data from this dataset.</a>\n" +
            "    ]]></description>\n" +
            "  <open>1</open>\n" +
            "  <Style id=\"BUOY ON\">\n" +
            "    <IconStyle>\n" +
            "      <color>ff0099ff</color>\n" + //abgr   orange
            "      <scale>" + (3 * iconSize) + "</scale>\n" +
            "      <Icon>\n" +
            //see list in Google Earth by right click on icon : Properties : the icon icon
            //2011-12-15 was shaded_dot (too fuzzy), now placemark_circle
            "        <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
            "      </Icon>\n" +
            "    </IconStyle>\n" +
            "  </Style>\n" +
            "  <Style id=\"BUOY OUT\">\n" +
            "    <IconStyle>\n" +
            "      <color>ff0099ff</color>\n" +
            "      <scale>" + (2 * iconSize) + "</scale>\n" +
            "      <Icon>\n" +
            "        <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
            "      </Icon>\n" +
            "    </IconStyle>\n" +
            "    <LabelStyle><scale>0</scale></LabelStyle>\n" +
            "  </Style>\n" +
            "  <StyleMap id=\"BUOY\">\n" +
            "    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n" +
            "    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n" +
            "  </StyleMap>\n");
        
        //just one link for each station (same lat,lon/depth):
        //LON   LAT DEPTH   TIME    ID  WTMP
        //-130.36   42.58   0.0 1.1652336E9 NDBC 46002 met  13.458333174387613
        int nRows = table.nRows();  //there must be at least 1 row
        int startRow = 0;
        double startLon = table.getNiceDoubleData(lonCol, startRow);
        double startLat = table.getNiceDoubleData(latCol, startRow);
        for (int row = 1; row <= nRows; row++) { //yes, 1...n, since looking at previous row
            //look for a change in lastLon/Lat
            if (row == nRows || 
                startLon != table.getNiceDoubleData(lonCol, row) ||
                startLat != table.getNiceDoubleData(latCol, row)) {

                if (!Double.isNaN(startLon) && !Double.isNaN(startLat)) {
                                
                    //make a placemark for this station
                    double startLon180 = Math2.anglePM180(startLon); 
                    writer.write(
                        "  <Placemark>\n" +
                        "    <name>" + 
                            "Lat=" + String2.genEFormat10(startLat) +
                            ", Lon=" + String2.genEFormat10(startLon180) +
                            "</name>\n" +
                        "    <description><![CDATA[" + 
                        //kml/description docs recommend \n<br />
                        XML.encodeAsXML(title) +
                        "\n<br />" + XML.encodeAsXML(courtesy));

                    //if timeCol exists, find last row with valid time
                    //This solves problem with dapper data (last row for each station has just NaNs)
                    int displayRow = row - 1;
                    if (timeCol >= 0) {
                        while (displayRow - 1 >= startRow && 
                               Double.isNaN(table.getDoubleData(timeCol, displayRow))) //if displayRow is NaN...
                            displayRow--;
                    }

                    //display the last row of data  (better than nothing)
                    for (int col = 0; col < table.nColumns(); col++) {
                        double td = table.getNiceDoubleData(col, displayRow);
                        String ts = table.getStringData(col, displayRow);
                        //String2.log("col=" + col + " name=" + table.getColumnName(col) + " units=" + columnUnits[col] + " isTimestamp=" + columnIsTimeStamp[col]);
                        writer.write("\n<br />" + 
                            XML.encodeAsXML(table.getColumnName(col) + " = " +
                                (columnIsTimeStamp[col]? 
                                     Calendar2.epochSecondsToLimitedIsoStringT(
                                        columnTimeFormat[col], td, "") :
                                 columnIsString[col]? ts :
                                 (Double.isNaN(td)? "NaN" : ts) + columnUnits[col])));
                    }
                    writer.write(
                        "\n<br /><a href=\"" + XML.encodeAsHTMLAttribute(tErddapUrl + 
                            "/tabledap/" + //don't use \n for the following lines
                            datasetID + ".htmlTable?" + 
                                //was SSR.minimalPercentEncode   XML.encodeAsXML isn't ok
                                //ignore userDapQuery    
                                //get just this station, all variables, at least 7 days                            
                                moreTime +  //already percentEncoded
                                //some data sources like dapper don't respond to lon=startLon lat=startLat
                                "&" + EDV.LON_NAME + SSR.minimalPercentEncode(">") + (startLon - .01) +  //not startLon180
                                "&" + EDV.LON_NAME + SSR.minimalPercentEncode("<") + (startLon + .01) +  
                                "&" + EDV.LAT_NAME + SSR.minimalPercentEncode(">") + (startLat - .01) +
                                "&" + EDV.LAT_NAME + SSR.minimalPercentEncode("<") + (startLat + .01)) +
                        "\">View tabular data for this location.</a>\n" +
                        "\n<br /><a href=\"" + XML.encodeAsHTMLAttribute(tErddapUrl + "/tabledap/" + //don't use \n for the following lines
                            datasetID + ".html?" + userDapQuery) + //already percentEncoded.  XML.encodeAsXML isn't ok 
                        "\">View/download more data from this dataset.</a>\n" +
                        "]]></description>\n" +
                        "    <styleUrl>#BUOY</styleUrl>\n" +
                        "    <Point>\n" +
                        "      <coordinates>" + 
                                   startLon180 + "," +
                                   startLat + 
                                "</coordinates>\n" +
                        "    </Point>\n" +
                        "  </Placemark>\n");
                }

                //reset startRow...
                startRow = row;
                if (startRow == nRows) {
                    //assist with LookAt
                    //it has trouble with 1 point: zoom in forever
                    //  and if lon range crossing dateline
                    double tMaxRange = Math.min(90, maxRange); //90 is most you can comfortably see, and useful fudge
                    tMaxRange = Math.max(2, tMaxRange);  //now it's 2 .. 90 
                    //fudge for smaller range
                    if (tMaxRange < 45) tMaxRange *= 1.5;
                    double eyeAt = 14.0e6 * tMaxRange / 90.0; //meters, 14e6 shows whole earth (~90 deg comfortably)  
                    double lookAtX = Math2.anglePM180((minLon + maxLon) / 2);
                    double lookAtY = (minLat + maxLat) / 2;
                    if (reallyVerbose) String2.log("KML minLon=" + minLon + " maxLon=" + maxLon + 
                        " minLat=" + minLat + " maxLat=" + maxLat + 
                        "\n  maxRange=" + maxRange + " tMaxRange=" + tMaxRange +
                        "\n  lookAtX=" + lookAtX + 
                        "  lookAtY=" + lookAtY + "  eyeAt=" + eyeAt); 
                    writer.write(
                        "  <LookAt>\n" +
                        "    <longitude>" + lookAtX + "</longitude>\n" +
                        "    <latitude>" + lookAtY + "</latitude>\n" +
                        "    <range>" + eyeAt + "</range>\n" + //meters  
                        "  </LookAt>\n");
                } else {
                    startLon = table.getNiceDoubleData(lonCol, startRow);
                    startLat = table.getNiceDoubleData(latCol, startRow);
                    minLon = Double.isNaN(minLon)? startLon : 
                             Double.isNaN(startLon)? minLon : Math.min(minLon, startLon);
                    maxLon = Double.isNaN(maxLon)? startLon : 
                             Double.isNaN(startLon)? maxLon : Math.max(maxLon, startLon);
                    minLat = Double.isNaN(minLat)? startLat : 
                             Double.isNaN(startLat)? minLat : Math.min(minLat, startLat);
                    maxLat = Double.isNaN(maxLat)? startLat : 
                             Double.isNaN(startLat)? maxLat : Math.max(maxLat, startLat);
                }

            } //end processing change in lon, lat, or depth
        } //end row loop

        //end of kml file
        writer.write(
            getKmlIconScreenOverlay() +
            "  </Document>\n" +
            "</kml>\n");
        writer.flush(); //essential
        return true;

    }

    /**
     * This saves the data in the table to the outputStream as an image.
     * If table.getColumnName(0)=LON_NAME and table.getColumnName(0)=LAT_NAME,
     * this plots the data on a map.
     * Otherwise, this makes a graph with x=col(0) and y=col(1).
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
     * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header and is also used to write the [fileTypeName]Info (e.g., .pngInfo) file.
     * @param outputStreamSource
     * @param fileTypeName
     * @return true of written ok; false if exception occurred (and written on image)
     * @throws Throwable if trouble
     */
    public boolean saveAsImage(String loggedInAs, String requestUrl, String userDapQuery,
        String dir, String fileName, 
        OutputStreamSource outputStreamSource, String fileTypeName) throws Throwable {

        if (reallyVerbose) String2.log("  EDDTable.saveAsImage query=" + userDapQuery);
        long time = System.currentTimeMillis();
        if (debugMode) String2.log("saveAsImage 1");
        //imageFileTypes
        //determine the size
        int sizeIndex = 
            fileTypeName.startsWith(".small")? 0 :
            fileTypeName.startsWith(".medium")? 1 :
            fileTypeName.startsWith(".large")? 2 : 1;
        boolean pdf = fileTypeName.toLowerCase().endsWith("pdf");
        boolean png = fileTypeName.toLowerCase().endsWith("png");
        boolean transparentPng = fileTypeName.equals(".transparentPng");
        if (!pdf && !png) 
            throw new SimpleException(EDStatic.errorInternal +
                "Unexpected image type=" + fileTypeName);
        Object pdfInfo[] = null;
        BufferedImage bufferedImage = null;
        Graphics2D g2 = null;
        int imageWidth, imageHeight;
        if (pdf) {
            imageWidth  = EDStatic.pdfWidths[ sizeIndex]; 
            imageHeight = EDStatic.pdfHeights[sizeIndex];
        } else if (transparentPng) {
            imageWidth  = EDStatic.imageWidths[sizeIndex]; 
            imageHeight = imageWidth;
        } else {
            imageWidth  = EDStatic.imageWidths[sizeIndex]; 
            imageHeight = EDStatic.imageHeights[sizeIndex];
        }

        Color transparentColor = transparentPng? Color.white : null; //getBufferedImage returns white background
        String drawLegend = LEGEND_BOTTOM;  
        int trim = Integer.MAX_VALUE;
        boolean ok = true;
       
        try {
            //get the user-specified resultsVariables
            StringArray resultsVariables    = new StringArray();
            StringArray constraintVariables = new StringArray();
            StringArray constraintOps       = new StringArray();
            StringArray constraintValues    = new StringArray();
            parseUserDapQuery(userDapQuery, resultsVariables,
                constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
                false);
            if (debugMode) String2.log("saveAsImage 2");

            EDV xVar, yVar, zVar = null, tVar = null; 
            if (resultsVariables.size() < 2)
                throw new SimpleException(EDStatic.queryError +
                    MessageFormat.format(EDStatic.queryError2Var, fileTypeName));
            //if lon and lat requested, it's a map
            boolean isMap =
                resultsVariables.indexOf(EDV.LON_NAME) >= 0 &&
                resultsVariables.indexOf(EDV.LON_NAME) < 2  &&
                resultsVariables.indexOf(EDV.LAT_NAME) >= 0 &&
                resultsVariables.indexOf(EDV.LAT_NAME) < 2;
            if (isMap) {
                xVar = dataVariables[lonIndex]; 
                yVar = dataVariables[latIndex]; 
            } else {
                //xVar,yVar are 1st and 2nd request variables
                xVar = findVariableByDestinationName(resultsVariables.get(0)); 
                yVar = findVariableByDestinationName(resultsVariables.get(1)); 
                if (yVar instanceof EDVTimeStamp) { //prefer time on x axis
                    EDV edv = xVar; xVar = yVar; yVar = edv;
                }
            }
            if (resultsVariables.size() >= 3)
                zVar = findVariableByDestinationName(resultsVariables.get(2));
            if (resultsVariables.size() >= 4)
                tVar = findVariableByDestinationName(resultsVariables.get(3));

            //get the table with all the data
            //errors here will be caught below
            //drawLegend=Only: Since data is needed early on, no way to not get data if legend doesn't need it
            TableWriterAllWithMetadata twawm = getTwawmForDapQuery(
                loggedInAs, requestUrl, userDapQuery);
            Table table = twawm.cumulativeTable();
            twawm.releaseResources();
            table.convertToStandardMissingValues();
            if (debugMode) String2.log("saveAsImage 3");         

            //units
            int xColN = table.findColumnNumber(xVar.destinationName());
            int yColN = table.findColumnNumber(yVar.destinationName());
            int zColN = zVar == null? -1 : table.findColumnNumber(zVar.destinationName());
            int tColN = tVar == null? -1 : table.findColumnNumber(tVar.destinationName());
            String xUnits = xVar instanceof EDVTimeStamp? "UTC" : table.columnAttributes(xColN).getString("units");
            String yUnits = yVar instanceof EDVTimeStamp? "UTC" : table.columnAttributes(yColN).getString("units");
            String zUnits = zColN < 0? null : zVar instanceof EDVTimeStamp? "UTC" : table.columnAttributes(zColN).getString("units");
            String tUnits = tColN < 0? null : tVar instanceof EDVTimeStamp? "UTC" : table.columnAttributes(tColN).getString("units");
            xUnits = xUnits == null? "" : " (" + xUnits + ")";
            yUnits = yUnits == null? "" : " (" + yUnits + ")";
            zUnits = zUnits == null? "" : " (" + zUnits + ")";
            tUnits = tUnits == null? "" : " (" + tUnits + ")";

            //extract optional .graphicsSettings from userDapQuery
            //  xRange, yRange, color and colorbar information
            //  title2 -- a prettified constraint string 
            boolean drawLines = false;
            boolean drawLinesAndMarkers = false;
            boolean drawMarkers = true;
            boolean drawSticks  = false;
            boolean drawVectors = false;
            int markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
            int markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
            Color color = Color.black;

            //set colorBar defaults via zVar attributes
            String ts;
            ts = zVar == null? null : zVar.combinedAttributes().getString("colorBarPalette");
            String palette = ts == null? "" : ts;
            ts = zVar == null? null : zVar.combinedAttributes().getString("colorBarScale");
            String scale = ts == null? "Linear" : ts;
            double paletteMin = zVar == null? Double.NaN : zVar.combinedAttributes().getDouble("colorBarMinimum");
            double paletteMax = zVar == null? Double.NaN : zVar.combinedAttributes().getDouble("colorBarMaximum");
            int nSections = -1;
            ts = zVar == null? null : zVar.combinedAttributes().getString("colorBarContinuous");
            boolean continuous = String2.parseBoolean(ts); //defaults to true

            double xMin = Double.NaN, xMax = Double.NaN, yMin = Double.NaN, yMax = Double.NaN;
            double fontScale = 1, vectorStandard = Double.NaN;
            int drawLandAsMask = 0;  //holds the .land setting: 0=default 1=under 2=over
            StringBuilder title2 = new StringBuilder();
            String ampParts[] = getUserQueryParts(userDapQuery); //decoded.  always at least 1 part (may be "")
            for (int ap = 0; ap < ampParts.length; ap++) {
                String ampPart = ampParts[ap];
                if (debugMode) String2.log("saveAsImage 4 " + ap);

                //.colorBar defaults: palette=""|continuous=C|scale=Linear|min=NaN|max=NaN|nSections=-1
                if (ampPart.startsWith(".colorBar=")) {
                    String pParts[] = String2.split(ampPart.substring(10), '|');
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0 && pParts[0].length() > 0) palette    = pParts[0];
                    if (pParts.length > 1 && pParts[1].length() > 0) continuous = !pParts[1].toLowerCase().startsWith("d");
                    if (pParts.length > 2 && pParts[2].length() > 0) scale      = pParts[2];
                    if (pParts.length > 3 && pParts[3].length() > 0) paletteMin = String2.parseDouble(pParts[3]);
                    if (pParts.length > 4 && pParts[4].length() > 0) paletteMax = String2.parseDouble(pParts[4]);
                    if (pParts.length > 5 && pParts[5].length() > 0) nSections  = String2.parseInt(pParts[5]);
                    if (String2.indexOf(EDStatic.palettes, palette) < 0) palette   = "";
                    if (String2.indexOf(EDV.VALID_SCALES, scale) < 0)    scale     = "Linear";
                    if (nSections < 0 || nSections >= 100)               nSections = -1;
                    if (reallyVerbose)
                        String2.log(".colorBar palette=" + palette +
                            " continuous=" + continuous +
                            " scale=" + scale +
                            " min=" + paletteMin + " max=" + paletteMax +
                            " nSections=" + nSections);                                

                //.color
                } else if (ampPart.startsWith(".color=")) {
                    int iColor = String2.parseInt(ampPart.substring(7));
                    if (iColor < Integer.MAX_VALUE) {
                        color = new Color(iColor);
                        if (reallyVerbose)
                            String2.log(".color=0x" + Integer.toHexString(iColor));
                    }

                //.draw 
                } else if (ampPart.startsWith(".draw=")) {
                    String tDraw = ampPart.substring(6);
                    //make all false
                    drawLines = false;
                    drawLinesAndMarkers = false;
                    drawMarkers = false;
                    drawSticks  = false;
                    drawVectors = false;
                    //set one option to true
                    if (tDraw.equals("sticks") && zVar != null) {drawSticks = true; isMap = false;}
                    else if (isMap && tDraw.equals("vectors") && zVar != null && tVar != null) drawVectors = true;
                    else if (tDraw.equals("lines")) drawLines = true;
                    else if (tDraw.equals("linesAndMarkers")) drawLinesAndMarkers = true;
                    else drawMarkers = true; //default

                //.font
                } else if (ampPart.startsWith(".font=")) {
                    String pParts[] = String2.split(ampPart.substring(6), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0) fontScale = String2.parseDouble(pParts[0]);
                    fontScale = Double.isNaN(fontScale)? 1 : fontScale < 0.1? 0.1 : fontScale > 10? 10 : fontScale;
                    if (reallyVerbose)
                        String2.log(".font= scale=" + fontScale);

                //.land 
                } else if (ampPart.startsWith(".land=")) {
                    String gt = ampPart.substring(6);
                    if      (gt.equals("under")) drawLandAsMask = 1;
                    else if (gt.equals("over"))  drawLandAsMask = 2;
                    if (reallyVerbose)
                        String2.log(".land= drawLandAsMask=" + drawLandAsMask);

                //.legend 
                } else if (ampPart.startsWith(".legend=")) {
                    drawLegend = ampPart.substring(8);
                    if (!drawLegend.equals(LEGEND_OFF) &&
                        !drawLegend.equals(LEGEND_ONLY))
                        drawLegend = LEGEND_BOTTOM;
                    if (drawLegend.equals(LEGEND_ONLY)) {
                        transparentPng = false; //if it was transparent, it was already png=true, size=1
                        transparentColor = null;
                    }

                //.marker
                } else if (ampPart.startsWith(".marker=")) {
                    String pParts[] = String2.split(ampPart.substring(8), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0) markerType = String2.parseInt(pParts[0]);
                    if (pParts.length > 1) markerSize = String2.parseInt(pParts[1]);
                    if (markerType < 0 || markerType >= GraphDataLayer.MARKER_TYPES.length) 
                        markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
                    if (markerSize < 1 || markerSize > 50) markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
                    if (reallyVerbose)
                        String2.log(".marker= type=" + markerType + " size=" + markerSize);

                //.size
                } else if (ampPart.startsWith(".size=")) {
                    //customSize = true; not used in EDDTable
                    String pParts[] = String2.split(ampPart.substring(6), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0) {
                        int w = String2.parseInt(pParts[0]); 
                        if (w > 0 && w < 3000) imageWidth = w;
                    }
                    if (pParts.length > 1) {
                        int h = String2.parseInt(pParts[1]);
                        if (h > 0 && h < 3000) imageHeight = h;
                    }
                    if (reallyVerbose)
                        String2.log(".size=  imageWidth=" + imageWidth + " imageHeight=" + imageHeight);

                //.trim
                } else if (ampPart.startsWith(".trim=")) {
                    trim = String2.parseInt(ampPart.substring(6));
                    if (reallyVerbose)
                        String2.log(".trim " + trim);

                //.vec
                } else if (ampPart.startsWith(".vec=")) {
                    vectorStandard = String2.parseDouble(ampPart.substring(5));
                    if (reallyVerbose)
                        String2.log(".vec " + vectorStandard);

                //.xRange   (supported, but currently not created by the Make A Graph form)
                //  prefer set via xVar constratints
                } else if (ampPart.startsWith(".xRange=")) {
                    String pParts[] = String2.split(ampPart.substring(8), '|');
                    if (pParts.length > 0) xMin = String2.parseDouble(pParts[0]);
                    if (pParts.length > 1) xMax = String2.parseDouble(pParts[1]);
                    if (reallyVerbose)
                        String2.log(".xRange min=" + xMin + " max=" + xMax);

                //.yRange   (supported, but currently not created by the Make A Graph form)
                //  prefer set via yVar constratints
                } else if (ampPart.startsWith(".yRange=")) {
                    String pParts[] = String2.split(ampPart.substring(8), '|');
                    if (pParts.length > 0) yMin = String2.parseDouble(pParts[0]);
                    if (pParts.length > 1) yMax = String2.parseDouble(pParts[1]);
                    if (reallyVerbose)
                        String2.log(".yRange min=" + yMin + " max=" + yMax);
    
                //ignore any unrecognized .something 
                } else if (ampPart.startsWith(".")) {

                //don't do anything with list of vars
                } else if (ap == 0) {

                //x and yVar constraints 
                } else if (ampPart.startsWith(xVar.destinationName()) || //x,y axis ranges indicate x,y var constraints
                           ampPart.startsWith(yVar.destinationName())) {
                    //don't include in title2  (better to leave in, but space often limited)

                //constraints on other vars
                } else {
                    //add to title2
                    if (title2.length() > 0) 
                        title2.append(", ");
                    title2.append(ampPart);
                }
            }
            if (title2.length() > 0) {
                title2.insert(0, "(");
                title2.append(")");
            }
            if (debugMode) String2.log("saveAsImage 5");

            //make colorMap if needed
            CompoundColorMap colorMap = null;
            //if (drawLines || drawSticks || drawVectors) 
            //    colorMap = null;
            //if ((drawLinesAndMarkers || drawMarkers) && zVar == null) 
            //    colorMap = null;
            if (drawVectors && zVar != null && Double.isNaN(vectorStandard)) {
                double zStats[] = table.getColumn(zColN).calculateStats();
                if (zStats[PrimitiveArray.STATS_N] == 0) {
                    vectorStandard = 1;
                } else {
                    double minMax[] = Math2.suggestLowHigh(0, 
                        Math.max(Math.abs(zStats[PrimitiveArray.STATS_MIN]),
                                 Math.abs(zStats[PrimitiveArray.STATS_MAX])));
                    vectorStandard = minMax[1];
                }
            }
            if ((drawLinesAndMarkers || drawMarkers) && zVar != null && colorMap == null) {
                if ((palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) && 
                    zColN >= 0) {
                    //set missing items based on z data
                    double zStats[] = table.getColumn(zColN).calculateStats();
                    if (zStats[PrimitiveArray.STATS_N] > 0) {
                        double minMax[];
                        if (zVar instanceof EDVTimeStamp) {
                            //???I think this is too crude. Smarter code elsewhere? Or handled by compoundColorMap?
                            double r20 = 
                                (zStats[PrimitiveArray.STATS_MAX] -
                                 zStats[PrimitiveArray.STATS_MIN]) / 20;
                            minMax = new double[]{
                                zStats[PrimitiveArray.STATS_MIN] - r20,
                                zStats[PrimitiveArray.STATS_MAX] + r20};
                        } else {
                            minMax = Math2.suggestLowHigh(
                                zStats[PrimitiveArray.STATS_MIN],
                                zStats[PrimitiveArray.STATS_MAX]);
                        }

                        if (palette.length() == 0) {
                            if (minMax[1] >= minMax[0] / -2 && 
                                minMax[1] <= minMax[0] * -2) {
                                double td = Math.max(minMax[1], -minMax[0]);
                                minMax[0] = -td;
                                minMax[1] = td;
                                palette = "BlueWhiteRed";
                            } else {
                                palette = "Rainbow";
                            }
                        }
                        if (Double.isNaN(paletteMin)) 
                            paletteMin = minMax[0];
                        if (Double.isNaN(paletteMax)) 
                            paletteMax = minMax[1];
                    }                                             
                }
                if (palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
                    String2.log("Warning in EDDTable.saveAsImage: NaNs not allowed (zVar has no numeric data):" +
                        " palette=" + palette +
                        " paletteMin=" + paletteMin +
                        " paletteMax=" + paletteMax);
                } else {
                    if (reallyVerbose)
                        String2.log("create colorBar palette=" + palette +
                            " continuous=" + continuous +
                            " scale=" + scale +
                            " min=" + paletteMin + " max=" + paletteMax +
                            " nSections=" + nSections);                                
                    if (zVar instanceof EDVTimeStamp)
                        colorMap = new CompoundColorMap(
                            EDStatic.fullPaletteDirectory, palette, false, //false= data is seconds
                            paletteMin, paletteMax, nSections, 
                            continuous, EDStatic.fullCptCacheDirectory);
                    else colorMap = new CompoundColorMap(
                            EDStatic.fullPaletteDirectory, palette, scale, 
                            paletteMin, paletteMax, nSections, 
                            continuous, EDStatic.fullCptCacheDirectory);
                }
            }

            //x|yVar > >= < <= constraints are relevant to x|yMin|Max (if not already set)
            StringBuilder constraintTitle = new StringBuilder();
            for (int con = 0; con < constraintVariables.size(); con++) {
                String conVar = constraintVariables.get(con);
                String conOp  = constraintOps.get(con);
                String conVal = constraintValues.get(con);
                double conValD = String2.parseDouble(conVal); //times are epochSeconds
                boolean isX = conVar.equals(xVar.destinationName());
                boolean isY = conVar.equals(yVar.destinationName());
                if (isX || isY) {
                    boolean isG = conOp.startsWith(">");
                    boolean isL = conOp.startsWith("<");
                    if (isG || isL) {
                        if      (isX && isG && Double.isNaN(xMin)) xMin = conValD;
                        else if (isX && isL && Double.isNaN(xMax)) xMax = conValD;
                        else if (isY && isG && Double.isNaN(yMin)) yMin = conValD;
                        else if (isY && isL && Double.isNaN(yMax)) yMax = conValD;
                    }

                    //isX and isY constraints not written to legend:
                    //  axis range implies variable constraint  (e.g., lat, lon, time)

                } else {

                    //build constraintTitle for legend
                    if (constraintTitle.length() > 0) 
                        constraintTitle.append(", ");
                    EDV edv = findDataVariableByDestinationName(conVar); 
                    constraintTitle.append(conVar + conOp + 
                        ((conOp.equals(PrimitiveArray.REGEX_OP) || 
                          edv.destinationDataTypeClass() == String.class) ?
                            String2.toJson(conVal) :
                         !Math2.isFinite(conValD)?
                            "NaN" :
                         edv instanceof EDVTimeStamp? 
                            //not time_precision, since query may be more precise
                            Calendar2.epochSecondsToIsoStringT(conValD) + "Z" : 
                         conVal));
                }
            }
            if (constraintTitle.length() > 0) {
                constraintTitle.insert(0, '(');
                constraintTitle.append(')');
            }
            if (debugMode) String2.log("saveAsImage 6");

            String varTitle = "";
            if (drawLines) {
                varTitle = "";
            } else if (drawLinesAndMarkers || drawMarkers) {
                varTitle = zVar == null || colorMap == null? "" : zVar.longName() + zUnits;
            } else if (drawSticks) {
                varTitle = "x=" + yVar.destinationName() + 
                    (yUnits.equals(zUnits)? "" : yUnits) + 
                    ", y=" + zVar.destinationName() + zUnits;
            } else if (drawVectors) { 
                varTitle = "x=" + zVar.destinationName() + 
                    (zUnits.equals(tUnits)? "" : zUnits) + 
                    ", y=" + tVar.destinationName() + tUnits +
                    ", standard=" + (float)vectorStandard;
            }
            String yLabel = drawSticks? varTitle :
                yVar.longName() + yUnits;

            //make a graphDataLayer 
            GraphDataLayer graphDataLayer = new GraphDataLayer(
                -1, //which pointScreen
                xColN, yColN, zColN, tColN, zColN, //x,y,z1,z2,z3 column numbers
                drawSticks? GraphDataLayer.DRAW_STICKS :
                    drawVectors?  GraphDataLayer.DRAW_POINT_VECTORS :
                    drawLines? GraphDataLayer.DRAW_LINES :
                    drawLinesAndMarkers? GraphDataLayer.DRAW_MARKERS_AND_LINES :
                    GraphDataLayer.DRAW_MARKERS, //default
                true, false,
                xVar.longName() + xUnits, //x,yAxisTitle  for now, always std units 
                yVar.longName() + yUnits, 
                varTitle.length() > 0? varTitle : title,             
                varTitle.length() > 0? title : "",
                constraintTitle.toString(), //title2.toString(), 
                MessageFormat.format(EDStatic.imageDataCourtesyOf, institution()), 
                table, null, null,
                colorMap, color,
                markerType, markerSize,
                vectorStandard,
                GraphDataLayer.REGRESS_NONE);
            ArrayList graphDataLayers = new ArrayList();
            graphDataLayers.add(graphDataLayer);

            //setup graphics2D
            String logoImageFile;
            if (pdf) {
                logoImageFile = EDStatic.highResLogoImageFile;
                fontScale *= 1.4; //SgtMap.PDF_FONTSCALE=1.5 is too big
                //getting the outputStream was delayed as long as possible to allow errors
                //to be detected and handled before committing to sending results to client
                pdfInfo = SgtUtil.createPdf(SgtUtil.PDF_PORTRAIT, 
                    imageWidth, imageHeight, outputStreamSource.outputStream("UTF-8"));
                g2 = (Graphics2D)pdfInfo[0];
            } else {
                logoImageFile = sizeIndex <= 1? EDStatic.lowResLogoImageFile : EDStatic.highResLogoImageFile;
                fontScale *= imageWidth < 500? 1: 1.25;
                bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
                g2 = (Graphics2D)bufferedImage.getGraphics();
            }
            if (reallyVerbose) String2.log("  sizeIndex=" + sizeIndex + " pdf=" + pdf + 
                " imageWidth=" + imageWidth + " imageHeight=" + imageHeight);
            if (debugMode) String2.log("saveAsImage 7");

            if (isMap) {
                //create a map

                if (Double.isNaN(xMin) || Double.isNaN(xMax) ||
                    Double.isNaN(yMin) || Double.isNaN(yMax)) {

                    //calculate the xy axis ranges (this should be in make map!)
                    double xStats[] = table.getColumn(xColN).calculateStats();
                    double yStats[] = table.getColumn(yColN).calculateStats();
                    if (xStats[PrimitiveArray.STATS_N] == 0 ||
                        yStats[PrimitiveArray.STATS_N] == 0) 
                        throw new SimpleException(EDStatic.noDataNoLL);

                    //old way  (too tied to big round numbers like 100, 200, 300) 
                    //often had big gap on one side
                    //double xLH[] = Math2.suggestLowHigh(
                    //    xStats[PrimitiveArray.STATS_MIN], 
                    //    xStats[PrimitiveArray.STATS_MAX]);
                    //double yLH[] = Math2.suggestLowHigh(
                    //    yStats[PrimitiveArray.STATS_MIN], 
                    //    yStats[PrimitiveArray.STATS_MAX]);

                    //new way
                    double xLH[] = {
                        xStats[PrimitiveArray.STATS_MIN], 
                        xStats[PrimitiveArray.STATS_MAX]};
                    double[] sd = Math2.suggestDivisions(xLH[1] - xLH[0]);
                    xLH[0] -= sd[1]; //tight range
                    xLH[1] += sd[1];
                    if (xStats[PrimitiveArray.STATS_N] == 0) {
                        xLH[0] = xVar.destinationMin(); 
                        xLH[1] = xVar.destinationMax();
                    }

                    double yLH[] = {
                        yStats[PrimitiveArray.STATS_MIN], 
                        yStats[PrimitiveArray.STATS_MAX]};
                    sd = Math2.suggestDivisions(yLH[1] - yLH[0]);
                    yLH[0] -= sd[1]; //tight range
                    yLH[1] += sd[1];
                    if (yStats[PrimitiveArray.STATS_N] == 0) {
                        yLH[0] = yVar.destinationMin(); 
                        yLH[1] = yVar.destinationMax();
                    }

                    //ensure default range at least 1x1 degree
                    double expandBy = (1 - (xLH[1] - xLH[0])) / 2;
                    if (expandBy > 0) {
                        xLH[0] -= expandBy;
                        xLH[1] += expandBy;
                    }
                    expandBy = (1 - (yLH[1] - yLH[0])) / 2;
                    if (expandBy > 0) {
                        yLH[0] -= expandBy;
                        yLH[1] += expandBy;
                    }

                    //ensure reasonable for a map
                    if (xLH[0] < -180) xLH[0] = -180;
                    //deal with odd cases like pmelArgoAll: x<0 and >180
                    if (-xLH[0] > xLH[1]-180) { //i.e., if minX is farther below 0, then maxX is >180
                        if (xLH[1] > 180) xLH[1] = 180;
                    } else {
                        if (xLH[0] < 0)   xLH[0] = 0;
                        if (xLH[1] > 360) xLH[1] = 360;
                    }
                    if (yLH[0] < -90) yLH[0] = -90;
                    if (yLH[1] >  90) yLH[1] =  90;

                    //make square
                    if (true) {
                        double xRange = xLH[1] - xLH[0];
                        double yRange = yLH[1] - yLH[0];
                        if (xRange > yRange) {
                            double diff2 = (xRange - yRange) / 2;
                            yLH[0] = Math.max(-90, yLH[0] - diff2);
                            yLH[1] = Math.min( 90, yLH[1] + diff2);
                        } else {
                            double diff2 = (yRange - xRange) / 2;
                            //deal with odd cases like pmelArgoAll: x<0 and >180
                            if (-xLH[0] > xLH[1]-180) { //i.e., if minX is farther below 0, than maxX is >180
                                xLH[0] = Math.max(-180, xLH[0] - diff2);
                                xLH[1] = Math.min( 180, xLH[1] + diff2);
                            } else {
                                xLH[0] = Math.max(   0, xLH[0] - diff2);
                                xLH[1] = Math.min( 360, xLH[1] + diff2);
                            }
                        }
                    }

                    //set xyMin/Max
                    if (Double.isNaN(xMin) || Double.isNaN(xMax)) {
                        xMin = xLH[0]; xMax = xLH[1];
                    }
                    if (Double.isNaN(yMin) || Double.isNaN(yMax)) {
                        yMin = yLH[0]; yMax = yLH[1];
                    }
                }


                int predicted[] = SgtMap.predictGraphSize(1, imageWidth, imageHeight, 
                    xMin, xMax, yMin, yMax);
                Grid bath = transparentPng || table.nRows() == 0? null :
                    SgtMap.createTopographyGrid(
                        EDStatic.fullSgtMapTopographyCacheDirectory, 
                        xMin, xMax, yMin, yMax, 
                        predicted[0], predicted[1]);

                if (drawLandAsMask == 0) {
                    EDV edv = zVar == null? yVar : zVar;
                    drawLandAsMask = edv.drawLandMask(defaultDrawLandMask())? 2 : 1;
                }

                if (transparentPng) {
                    //fill with unusual color --> later convert to transparent
                    //Not a great approach to the problem.
                    transparentColor = new Color(0,3,1); //not common, not in grayscale, not white
                    g2.setColor(transparentColor);
                    g2.fillRect(0, 0, imageWidth, imageHeight);
                }

                ArrayList mmal = SgtMap.makeMap(transparentPng, 
                    SgtUtil.LEGEND_BELOW,
                    EDStatic.legendTitle1, EDStatic.legendTitle2,
                    EDStatic.imageDir, logoImageFile,
                    xMin, xMax, yMin, yMax, //predefined min/maxX/Y
                    drawLandAsMask == 2, //2=over
                    bath != null, //plotGridData (bathymetry)
                    bath,
                    1, 1, 0, //double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
                    drawLandAsMask == 1? SgtMap.topographyCptFullName : //1=under
                                         SgtMap.bathymetryCptFullName,  //2=over: deals better with elevation ~= 0
                    null, //SgtMap.TOPOGRAPHY_BOLD_TITLE + " (" + SgtMap.TOPOGRAPHY_UNITS + ")",
                    "",
                    "",  
                    "", //MessageFormat.format(EDStatic.imageDataCourtesyOf, SgtMap.TOPOGRAPHY_COURTESY)
                    SgtMap.FILL_LAKES_AND_RIVERS, 
                    false, null, 1, 1, 1, "", null, "", "", "", "", "", //plot contour 
                    graphDataLayers,
                    g2, 0, 0, imageWidth, imageHeight,
                    0, //no boundaryResAdjust,
                    fontScale);

                writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);


            } else {
                //create a graph

                if (transparentPng) {
                    //fill with unusual color --> later convert to transparent
                    //Not a great approach to the problem.
                    transparentColor = new Color(0,3,1); //not common, not in grayscale, not white
                    g2.setColor(transparentColor);
                    g2.fillRect(0, 0, imageWidth, imageHeight);
                }

                ArrayList mmal = EDStatic.sgtGraph.makeGraph(transparentPng,
                    xVar.longName() + xUnits, //x,yAxisTitle  for now, always std units 
                    png && drawLegend.equals(LEGEND_ONLY)? "." : yLabel, //avoid running into legend
                    SgtUtil.LEGEND_BELOW, EDStatic.legendTitle1, EDStatic.legendTitle2,
                    EDStatic.imageDir, logoImageFile,
                    xMin, xMax, yMin, yMax, 
                    xVar instanceof EDVTimeStamp, //x/yIsTimeAxis,
                    yVar instanceof EDVTimeStamp, 
                    graphDataLayers,
                    g2, 0, 0, imageWidth, imageHeight,  1, //graph imageWidth/imageHeight
                    fontScale); 

                writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);
            }
            if (debugMode) String2.log("saveAsImage 8");

            //deal with .legend and trim
            if (png && !transparentPng) {
                if (drawLegend.equals(LEGEND_OFF)) 
                    bufferedImage = SgtUtil.removeLegend(bufferedImage);
                else if (drawLegend.equals(LEGEND_ONLY)) 
                    bufferedImage = SgtUtil.extractLegend(bufferedImage);

                //do after removeLegend
                bufferedImage = SgtUtil.trimBottom(bufferedImage, trim);
            }
            if (debugMode) String2.log("saveAsImage 9");

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            ok = false;
            try {
                String msg = MustBe.getShortErrorMessage(t);
                String2.log(MustBe.throwableToString(t)); //log full message with stack trace

                if (png && drawLegend.equals(LEGEND_ONLY)) {
                    //return a transparent 1x1 pixel image
                    bufferedImage = SgtUtil.getBufferedImage(1, 1); //has white background
                    transparentColor = Color.white;

                } else {
                    //write exception info on image
                    double tFontScale = pdf? 1.25 : 1;
                    int tHeight = Math2.roundToInt(tFontScale * 12);

                    if (pdf) {
                        if (pdfInfo == null)
                            pdfInfo = SgtUtil.createPdf(SgtUtil.PDF_PORTRAIT, 
                                imageWidth, imageHeight, outputStreamSource.outputStream("UTF-8"));
                        if (g2 == null)
                            g2 = (Graphics2D)pdfInfo[0];
                    } else {
                        //make a new image (I don't think pdf can work this way -- sent as created)
                        bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
                        g2 = (Graphics2D)bufferedImage.getGraphics();
                    }
                    if (transparentPng) {
                        //don't write the message  
                        //The "right" thing to do is different in different situations.
                        //But e.g., No Data, should just be a transparent image.
                        transparentColor = Color.white;
                    } else {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setClip(0, 0, imageWidth, imageHeight); //unset in case set by sgtGraph
                        msg = String2.noLongLines(msg, (imageWidth * 10 / 6) / tHeight, "    ");
                        String lines[] = msg.split("\\n"); //not String2.split which trims
                        g2.setColor(Color.black);
                        g2.setFont(new Font(EDStatic.fontFamily, Font.PLAIN, tHeight));
                        int ty = tHeight * 2;
                        for (int i = 0; i < lines.length; i++) {
                            g2.drawString(lines[i], tHeight, ty);
                            ty += tHeight + 2;
                        }
                    }
                }
            } catch (Throwable t2) {
                EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}
                String2.log(String2.ERROR + "2 while creating error image:\n" + 
                    MustBe.throwableToString(t2)); 
                if (pdf) {
                    if (pdfInfo == null) throw t;
                } else {
                    if (bufferedImage == null) throw t;
                }
                //else fall through to close/save image below
            }
        }
        if (debugMode) String2.log("saveAsImage 9");

        //save image
        if (pdf) {
            SgtUtil.closePdf(pdfInfo);
        } else {

            //getting the outputStream was delayed as long as possible to allow errors
            //to be detected and handled before committing to sending results to client
            SgtUtil.saveAsTransparentPng(bufferedImage, transparentColor,
                outputStreamSource.outputStream("")); 
        }

        outputStreamSource.outputStream("").flush(); //safety

        if (reallyVerbose) String2.log("  EDDTable.saveAsImage done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
        return ok;
    }



    /**
     * Save the TableWriterAllWithMetadata data as a Matlab .mat file.
     * This doesn't write attributes because .mat files don't store attributes.
     * This maintains the data types (Strings become char[][]).
     * 
     * @param outputStreamSource
     * @param twawm  all the results data, with missingValues stored as destinationMissingValues
     *    or destinationFillValues  (they are converted to NaNs)
     * @param structureName the name to use for the variable which holds all of the data, 
     *    usually the dataset's internal name (datasetID).
     * @throws Throwable 
     */
    public void saveAsMatlab(OutputStreamSource outputStreamSource, 
        TableWriterAllWithMetadata twawm, String structureName) throws Throwable {
        if (reallyVerbose) String2.log("EDDTable.saveAsMatlab"); 
        long time = System.currentTimeMillis();

        //make sure there is data
        long tnRows = twawm.nRows();
        if (tnRows == 0)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA);
        if (tnRows >= Integer.MAX_VALUE)
            throw new SimpleException(Math2.memoryTooMuchData + "  " +
                MessageFormat.format(EDStatic.errorMoreThan2GB,
                    ".mat", tnRows + " " + EDStatic.rows));
        int nCols = twawm.nColumns();
        int nRows = (int)tnRows; //safe since checked above

        //calculate cumulative size of the structure
        byte structureNameInfo[] = Matlab.nameInfo(structureName); 
        NDimensionalIndex ndIndex[] = new NDimensionalIndex[nCols];
        long cumSize = //see 1-32
            16 + //for array flags
            16 + //my structure is always 2 dimensions 
            structureNameInfo.length +
            8 + //field name length (for all fields)
            8 + nCols * 32; //field names
        for (int col = 0; col < nCols; col++) {
            Class type = twawm.columnType(col);
            if (type == String.class)
                ndIndex[col] = Matlab.make2DNDIndex(nRows, twawm.columnMaxStringLength(col)); 
            else
                ndIndex[col] = Matlab.make2DNDIndex(nRows); 
            //add size of each cell
            cumSize += 8 + //type and size
                Matlab.sizeOfNDimensionalArray( //throws exception if too big for Matlab
                    "", //without column names (they're stored separately)
                    type, ndIndex[col]);
        }

        if (cumSize >= Integer.MAX_VALUE - 1000)
            throw new SimpleException(Math2.memoryTooMuchData + "  " +
                MessageFormat.format(EDStatic.errorMoreThan2GB,
                    ".mat", (cumSize / Math2.BytesPerMB) + " MB"));

        //open a dataOutputStream 
        DataOutputStream stream = new DataOutputStream(outputStreamSource.outputStream(""));

        //*** write Matlab Structure  see 1-32
        //*** THIS CODE MIMICS Table.saveAsMatlab. If make changes here, make them there, too.
        //    The code in EDDGrid.saveAsMatlab is similar, too.
        //write the header
        Matlab.writeMatlabHeader(stream);

        //write the miMatrix dataType and nBytes
        stream.writeInt(Matlab.miMATRIX);        //dataType
        stream.writeInt((int)cumSize); //safe since checked above

        //write array flags 
        stream.writeInt(Matlab.miUINT32); //dataType
        stream.writeInt(8);  //fixed nBytes of data
        stream.writeInt(Matlab.mxSTRUCT_CLASS); //array flags  
        stream.writeInt(0); //reserved; ends on 8 byte boundary

        //write structure's dimension array 
        stream.writeInt(Matlab.miINT32); //dataType
        stream.writeInt(2 * 4);  //nBytes
        //matlab docs have 2,1, octave has 1,1. 
        //Think of structure as one row of a table, where elements are entire arrays:  e.g., sst.lon sst.lat sst.sst.
        //Having multidimensions (e.g., 2 here) lets you have additional rows, e.g., sst(2).lon sst(2).lat sst(2).sst.
        //So 1,1 makes sense.
        stream.writeInt(1);  
        stream.writeInt(1);
         
        //write structure name 
        stream.write(structureNameInfo, 0, structureNameInfo.length);

        //write length for all field names (always 32)  (short form)
        stream.writeShort(4);                //nBytes
        stream.writeShort(Matlab.miINT32);   //dataType
        stream.writeInt(32);                 //32 bytes per field name

        //write the field names (each 32 bytes)
        stream.writeInt(Matlab.miINT8);   //dataType
        stream.writeInt(nCols * 32);      //32 bytes per field name
        String nulls = String2.makeString('\u0000', 32);
        for (int col = 0; col < nCols; col++) 
            stream.write(String2.toByteArray(
                String2.noLongerThan(twawm.columnName(col), 31) + nulls), 0, 32);

        //write the structure's elements (one for each col)
        //This is pretty good at conserving memory (just one column in memory at a time).
        //It would be hard to make more conservative because Strings have
        //to be written out: all first chars, all second chars, all third chars...
        for (int col = 0; col < nCols; col++) {
            PrimitiveArray pa = twawm.column(col);
            if (!(pa instanceof StringArray)) {
                //convert missing values to NaNs
                pa.convertToStandardMissingValues( 
                    twawm.columnAttributes(col).getDouble("_FillValue"),
                    twawm.columnAttributes(col).getDouble("missing_value"));
            }
            Matlab.writeNDimensionalArray(stream, "", //without column names (they're stored separately)
                pa, ndIndex[col]);
        }

        //this doesn't write attributes because .mat files don't store attributes

        stream.flush(); //essential

        if (reallyVerbose) String2.log("  EDDTable.saveAsMatlab done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
    }


    /**
     * Save this table of data as a flat netCDF .nc file (a column for each 
     * variable, all referencing one dimension) using the currently
     * available attributes.
     * <br>The data are written as separate variables, sharing a common dimension
     *   "observation", not as a Structure.
     * <br>The data values are written as their current data type 
     *   (e.g., float or int).
     * <br>This writes the lon values as they are currently in this table
     *   (e.g., +-180 or 0..360).
     * <br>This overwrites any existing file of the specified name.
     * <br>This makes an effort not to create a partial file if there is an error.
     * <br>If no exception is thrown, the file was successfully created.
     * <br>!!!The file must have at least one row, or an Exception will be thrown
     *   (nc dimensions can't be 0 length).
     * <br>!!!Missing values should be destinationMissingValues or 
     *   destinationFillValues and aren't changed.
     * 
     * @param fullName the full file name (dir+name+ext)
     * @param twawm provides access to all of the data
     * @throws Throwable if trouble (e.g., TOO_MUCH_DATA)
     */
    public void saveAsFlatNc(String fullName, TableWriterAllWithMetadata twawm) 
        throws Throwable {
        if (reallyVerbose) String2.log("  EDDTable.saveAsFlatNc " + fullName); 
        long time = System.currentTimeMillis();

        //delete any existing file
        File2.delete(fullName);

        //test for too much data
        if (twawm.nRows() >= Integer.MAX_VALUE)
            throw new SimpleException(Math2.memoryTooMuchData + "  " +
                MessageFormat.format(EDStatic.errorMoreThan2GB,
                    ".nc", twawm.nRows() + " " + EDStatic.rows));
        int nRows = (int)twawm.nRows(); //safe since checked above
        int nColumns = twawm.nColumns();
        if (nRows == 0) 
            throw new SimpleException(MustBe.THERE_IS_NO_DATA);

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fullName + randomInt, false);
        try {
            //items determined by looking at a .nc file; items written in that order 

            //define the dimensions
            Dimension dimension  = nc.addDimension(ROW_NAME, nRows);
//javadoc says: if there is an unlimited dimension, all variables that use it are in a structure
//Dimension rowDimension  = nc.addDimension("row", nRows, true, true, false); //isShared, isUnlimited, isUnknown
//String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

            //add the variables
            for (int col = 0; col < nColumns; col++) {
                Class type = twawm.columnType(col);
                String tColName = twawm.columnName(col);
                if (type == String.class) {
                    int max = Math.max(1, twawm.columnMaxStringLength(col)); //nc libs want at least 1; 0 happens if no data
                    Dimension lengthDimension = nc.addDimension(
                        tColName + NcHelper.StringLengthSuffix, max);
                    nc.addVariable(tColName, DataType.CHAR, 
                        new Dimension[]{dimension, lengthDimension}); 
                } else {
                    nc.addVariable(tColName, DataType.getType(type), new Dimension[]{dimension}); 
                }
//nc.addMemberVariable(recordStructure, nc.findVariable(tColName));
            }

//boolean bool = nc.addRecordStructure(); //creates a structure variable called "record"         
//String2.log("addRecordStructure: " + bool);
//Structure recordStructure = (Structure)nc.findVariable("record");

            //set id attribute = file name
            //currently, only the .nc saveAs types use attributes!
            Attributes globalAttributes = twawm.globalAttributes();
            if (globalAttributes.get("id") == null)
                globalAttributes.set("id", File2.getNameNoExtension(fullName));

            //set the attributes
            NcHelper.setAttributes(nc, "NC_GLOBAL", globalAttributes);
            for (int col = 0; col < nColumns; col++) 
                NcHelper.setAttributes(nc, twawm.columnName(col), twawm.columnAttributes(col));

            //leave "define" mode
            nc.create();

            //write the data
            for (int col = 0; col < nColumns; col++) {
                //missing values are already destinationMissingValues or destinationFillValues
                /*
                //old way.  getting all data for one column may use a lot of memory
                PrimitiveArray pa = twawm.column(col);
                nc.write(twawm.columnName(col), 
                    NcHelper.get1DArray(pa.toObjectArray()));
                */
                int nToGo = nRows; 
                int ncOffset = 0;
                int bufferSize = EDStatic.partialRequestMaxCells;
                PrimitiveArray pa = null;
                DataInputStream dis = twawm.dataInputStream(col);
                Class colType = twawm.columnType(col);

                while (nToGo > 0) {
                    bufferSize = Math.min(nToGo, bufferSize); //actual number to be transferred
                    //use of != below (not <) lets toObjectArray below return internal array since size=capacity
                    if (pa == null || pa.capacity() != bufferSize)
                        pa = PrimitiveArray.factory(colType, bufferSize, false);
                    pa.readDis(dis, bufferSize);                    
                    nc.write(twawm.columnName(col), 
                        colType == String.class? new int[]{ncOffset, 0} : new int[]{ncOffset},
                        NcHelper.get1DArray(pa.toObjectArray()));  
                    nToGo -= bufferSize;
                    ncOffset += bufferSize;
                    //String2.log("col=" + col + " bufferSize=" + bufferSize + " isString?" + (colType == String.class));
                }
            }

            //if close throws Throwable, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately

            //rename the file to the specified name
            File2.rename(fullName + randomInt, fullName);

            //diagnostic
            if (reallyVerbose) String2.log("  EDDTable.saveAsFlatNc done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
            //ncDump("End of Table.saveAsFlatNc", directory + name + ext, false);

        } catch (Throwable t) {
            //try to close the file
            try {
                nc.close(); //it calls flush() and doesn't like flush called separately
            } catch (Throwable t2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullName + randomInt);

            throw t;
        }

    }

    /** Ensure a conventions string includes "CF-1.6" (or already >1.6). */
    public static String ensureAtLeastCF16(String conv) {
        if (conv == null || conv.length() == 0) {
            conv = "CF-1.6";
        } else if (conv.indexOf("CF-") < 0) {
            conv += ", CF-1.6";
        } else {
            for (int i = 0; i < 6; i++)
                conv = String2.replaceAll(conv, "CF-1." + i, "CF-1.6");
        }
        return conv;
    }

    /**
     * Create a Point .ncCF file (Discrete Sampling Geometries).  Specifically:
     * <br>"Point Data" at
     * http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries
     * <br>This assumes cdm_data_type=Point. 
     *
     * <p>This also handles .ncCFMA.  No changes needed.
     * 
     * @param ncCFName the complete name of the file that will be created
     * @param twawm
     * @throws Throwable if trouble 
     */
    public void saveAsNcCF0(String ncCFName, TableWriterAllWithMetadata twawm) 
        throws Throwable {
        if (reallyVerbose) String2.log("EDDTable.convertFlatNcToNcCF0 " + ncCFName); 
        long time = System.currentTimeMillis();

        //delete any existing file
        File2.delete(ncCFName);

        //double check that this dataset supports .ncCF (but it should have been tested earlier)
        if (accessibleViaNcCF.length() > 0) 
            throw new SimpleException(accessibleViaNcCF);
        String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
        if (!CDM_POINT.equals(cdmType))   
            throw new SimpleException( //but already checked before calling this method
                "cdm_data_type for convertFlatNcToNcCF0() must be Point, " +
                "not " + cdmType + ".");

        //add metadata 
        Attributes globalAtts = twawm.globalAttributes();
        globalAtts.set("featureType", "Point");
        globalAtts.set(                            "Conventions", 
            ensureAtLeastCF16(globalAtts.getString("Conventions")));
        globalAtts.set(                            "Metadata_Conventions", 
            ensureAtLeastCF16(globalAtts.getString("Metadata_Conventions")));

        //set the variable attributes
        //section 9.1.1 says "The coordinates attribute must identify the variables 
        //  needed to geospatially position and time locate the observation. 
        //  The lat, lon and time coordinates must always exist; a vertical
        //  coordinate may exist."
        //so always include time, lat, lon, even if they use obs dimension.
        String ncColNames[] = twawm.columnNames();
        int ncNCols = ncColNames.length;
        boolean isCoordinateVar[] = new boolean[ncNCols];  //all false
        isCoordinateVar[String2.indexOf(ncColNames, "longitude")] = true;
        isCoordinateVar[String2.indexOf(ncColNames, "latitude" )] = true;
        isCoordinateVar[String2.indexOf(ncColNames, "time"     )] = true;
        String coordinates = "time latitude longitude"; //spec doesn't specify a specific order
        String proxy = combinedGlobalAttributes.getString("cdm_altitude_proxy");
        if (altIndex >= 0) {
            coordinates += " altitude";
            isCoordinateVar[String2.indexOf(ncColNames, "altitude")] = true;
        } else if (depthIndex >= 0) {
            coordinates += " depth";
            isCoordinateVar[String2.indexOf(ncColNames, "depth")] = true;
        } else if (proxy != null) {
            //it is in requiredCfRequestVariables
            coordinates += " " + proxy;
            isCoordinateVar[String2.indexOf(ncColNames, proxy)] = true; 
        }
        for (int col = 0; col < ncNCols; col++) {
            Attributes atts = twawm.columnAttributes(col);
            if (!isCoordinateVar[col])
                atts.add("coordinates", coordinates);
        }

        //save as .nc (it writes to randomInt temp file, then to fullName)
        saveAsFlatNc(ncCFName, twawm);
    }


    /**
     * Create a one-level TimeSeries, Trajectory, or Profile .ncCF or .ncCFMA file
     * (Discrete Sampling Geometries).
     * http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries
     * <br>This assumes cdm_data_type=TimeSeries|Trajectory|Profile and 
     * cdm_timeseries_variables, cdm_trajectory_variables, or 
     * cdm_profile_variables are defined correctly. 
     * 
     * @param nodcMode If true, data will be saved in .ncCFMA file conforming to
     *    NODC Templates http://www.nodc.noaa.gov/data/formats/netcdf/
     *    with Multidimensional Array Representation.
     *    If false, data will be saved in .ncCF file with 
     *    Contiguous Ragged Array Representation.
     * @param ncCFName the complete name of the file that will be created
     * @param twawm
     * @throws Throwable if trouble 
     */
    public void saveAsNcCF1(boolean nodcMode, String ncCFName, 
        TableWriterAllWithMetadata twawm) throws Throwable {
        if (reallyVerbose) String2.log("EDDTable.convertFlatNcToNcCF1 " + ncCFName); 
        long time = System.currentTimeMillis();
        String rowSizeName = "rowSize";

        //delete any existing file
        File2.delete(ncCFName);

        //double check that this dataset supports .ncCF (but it should have been tested earlier)
        if (accessibleViaNcCF.length() > 0) 
            throw new SimpleException(accessibleViaNcCF);
        String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
        String lcCdmType = cdmType == null? null : cdmType.toLowerCase();

        //ensure profile_id|timeseries_id|trajectory_id variable is defined 
        //and that cdmType is valid
        int idDVI = -1;  
        if      (CDM_PROFILE   .equals(cdmType)) idDVI = profile_idIndex;
        else if (CDM_TIMESERIES.equals(cdmType)) idDVI = timeseries_idIndex;
        else if (CDM_TRAJECTORY.equals(cdmType)) idDVI = trajectory_idIndex;
        else throw new SimpleException( //but already checked before calling this method
            "For convertFlatNcToNcCF1(), cdm_data_type must be TimeSeries, Profile, or Trajectory, " +
            "not " + cdmType + ".");
        if (idDVI < 0) //but already checked by accessibleViaNcCF
            throw new SimpleException("No variable has a cf_role=" + lcCdmType + "_id attribute.");              
        String idName = dataVariableDestinationNames()[idDVI];  //var name of ..._id var

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        NetcdfFileWriteable ncCF = null;
        try {
            //get info from twawm
            //Note: this uses "feature" as stand-in for profile|timeseries|trajectory
            String ncColNames[] = twawm.columnNames();
            //String2.log("  ncColNames=" + String2.toCSSVString(ncColNames));
            int ncNCols = twawm.nColumns();
            boolean isFeatureVar[] = new boolean[ncNCols];
            String featureVars[] = combinedGlobalAttributes.getStringsFromCSV(
                "cdm_" + lcCdmType + "_variables");
            EDV colEdv[] = new EDV[ncNCols];
            for (int col = 0; col < ncNCols; col++) {
                isFeatureVar[col] = String2.indexOf(featureVars, ncColNames[col]) >= 0;
                colEdv[col] = findVariableByDestinationName(ncColNames[col]);
            }

            //ensure profile_id|timeseries_id|trajectory_id variable is in flatNc file
            int idPo = String2.indexOf(ncColNames, idName);
            if (idPo < 0) //but already checked before calling this method
                throw new SimpleException("The .nc file must have " + idName);

            //read id PA from flatNc file
            PrimitiveArray idPA = twawm.column(idPo);
            //String2.log("  idPA=" + idPA.toString());

            //rank id rows
            //!!! this assumes already sorted by time/depth/... and will stay sorted when rearranged. 
            //  PrimitiveArray.rank promises stable sort.
            ArrayList tTable = new ArrayList();
            tTable.add(idPA);
            int rank[] = PrimitiveArray.rank(tTable, new int[]{0}, new boolean[]{true});
            tTable = null;
            //String2.log("  rank=" + String2.toCSSVString(rank));

            //find unique features (e.g., station|profile|trajectory): 
            int totalNObs = idPA.size();
            //first row has first feature (first row in CF file for each feature)
            IntArray featureFirstRow = new IntArray(); //'row' after data is sorted
            featureFirstRow.add(0);            
            int prevFirstRow = 0;                      //'row' after data is sorted
            IntArray featureNRows = new IntArray();  
            BitSet featureKeep = new BitSet(); //initially all false
            featureKeep.set(0);                        //'row' after data is sorted
            //'row' after data is sorted
            for (int row = 1; row < totalNObs; row++) {  //1 since comparing to previous row
                if (idPA.compare(rank[row - 1], rank[row]) != 0) {
                    //found a new feature
                    featureFirstRow.add(row);             //for this feature
                    featureNRows.add(row - prevFirstRow); //for the previous feature
                    featureKeep.set(row);
                    prevFirstRow = row;
                }
            }
            featureNRows.add(totalNObs - prevFirstRow);
            int maxFeatureNRows = Math2.roundToInt(
                featureNRows.calculateStats()[PrimitiveArray.STATS_MAX]);
            int nFeatures = featureFirstRow.size();
            if (reallyVerbose) String2.log("  nFeatures=" + nFeatures + 
                "\n  featureFirstRow=" + featureFirstRow.toString() +
                "\n  featureNRows="    + featureNRows.toString() +
                "\n  maxFeatureNRows=" + maxFeatureNRows);
            //conserve memory
            idPA = null;

//do a fileSize check here.  <2GB?

            //** Create the .ncCF file
            ncCF = NetcdfFileWriteable.createNew(ncCFName + randomInt, false);
            //define the dimensions
            Dimension featureDimension = ncCF.addDimension(lcCdmType, nFeatures);
            Dimension obsDimension     = ncCF.addDimension("obs", 
                nodcMode? maxFeatureNRows : totalNObs);

            //add the feature variables, then the obs variables
            for (int so = 0; so < 2; so++) {  //0=feature 1=obs
                for (int col = 0; col < ncNCols; col++) {
                    if (( isFeatureVar[col] && so == 0) || //is var in currently desired group?
                        (!isFeatureVar[col] && so == 1)) {
                        //keep going
                    } else {
                        continue;
                    }
                    String tColName = ncColNames[col];
                    Class type = colEdv[col].destinationDataTypeClass();
                    int maxStringLength = 1;  //nclib wants at least 1
                    if (type == String.class)
                        maxStringLength = Math.max(1, twawm.columnMaxStringLength(col));
                    if (type == long.class) {
                        //store longs as Strings since nc3 doesn't support longs
                        type = String.class; 
                        maxStringLength = NcHelper.LONG_MAXSTRINGLENGTH;
                    }
                    ArrayList tDims = new ArrayList();
                    if (nodcMode) {
                        if (isFeatureVar[col]) {
                            tDims.add(featureDimension);
                        } else {
                            //obsVar[feature][obs]
                            tDims.add(featureDimension);
                            tDims.add(obsDimension);
                        }
                    } else {
                        tDims.add(isFeatureVar[col]? featureDimension : obsDimension);
                    }
                    if (type == String.class) {
                        ncCF.addStringVariable(tColName, tDims, maxStringLength);
                    } else {
                        ncCF.addVariable(tColName, DataType.getType(type), tDims); 
                    }

                    //nodcMode: ensure all numeric obs variables have missing_value or _FillValue
                    //(String vars would have "", but that isn't a valid attribute)
                    if (nodcMode && so == 1 && type != String.class) {
                        Attributes atts = twawm.columnAttributes(col);
                        if (atts.getString("missing_value") == null && //getString so distinguish null and NaN
                            atts.getString("_FillValue")    == null) {
                            //"" will become native numeric MV, e.g., 127 for ByteArray
                            atts.set("_FillValue", PrimitiveArray.factory(type, 1, "")); 
                        }
                    }
                    
                }

                //at end of feature vars, add the rowSize variable
                if (!nodcMode && so == 0) {
                    ncCF.addVariable(rowSizeName, DataType.getType(int.class), 
                        new Dimension[]{featureDimension}); 
                    //String2.log("  rowSize variable added");
                }
            }
            
            //set the global attributes
            //currently, only the .nc saveAs file types use attributes!
            Attributes cdmGlobalAttributes = twawm.globalAttributes();
            String featureType = cdmType;
            cdmGlobalAttributes.remove("observationDimension"); //obsDim is for a flat file and OBSOLETE
            cdmGlobalAttributes.set("featureType", featureType);
            cdmGlobalAttributes.set("id", File2.getNameNoExtension(ncCFName)); //id attribute = file name
            cdmGlobalAttributes.set(                            "Conventions", 
                ensureAtLeastCF16(cdmGlobalAttributes.getString("Conventions")));
            cdmGlobalAttributes.set(                            "Metadata_Conventions", 
                ensureAtLeastCF16(cdmGlobalAttributes.getString("Metadata_Conventions")));
            NcHelper.setAttributes(ncCF, "NC_GLOBAL", cdmGlobalAttributes);


            //set the variable attributes
            //section 9.1.1 says "The coordinates attribute must identify the variables 
            //  needed to geospatially position and time locate the observation. 
            //  The lat, lon and time coordinates must always exist; a vertical
            //  coordinate may exist."
            //so always include time, lat, lon, even if they use obs dimension.
            boolean isCoordinateVar[] = new boolean[ncNCols];  //all false
            isCoordinateVar[String2.indexOf(ncColNames, "longitude")] = true;
            isCoordinateVar[String2.indexOf(ncColNames, "latitude" )] = true;
            isCoordinateVar[String2.indexOf(ncColNames, "time"     )] = true;
            String coordinates = "time latitude longitude"; //spec doesn't specify a specific order
            String proxy = combinedGlobalAttributes.getString("cdm_altitude_proxy");
            if (altIndex >= 0) {
                coordinates += " altitude";
                isCoordinateVar[String2.indexOf(ncColNames, "altitude")] = true;
            } else if (depthIndex >= 0) {
                coordinates += " depth";
                isCoordinateVar[String2.indexOf(ncColNames, "depth")] = true;
            } else if (proxy != null) {
                //it is in requiredCfRequestVariables
                coordinates += " " + proxy;
                isCoordinateVar[String2.indexOf(ncColNames, proxy)] = true; 
            }
            for (int col = 0; col < ncNCols; col++) {
                Attributes atts = twawm.columnAttributes(col);
                if (!isFeatureVar[col] && !isCoordinateVar[col])
                    atts.add("coordinates", coordinates);
                NcHelper.setAttributes(ncCF, ncColNames[col], atts);
            }

            //set the rowSize attributes
            if (!nodcMode) {
                NcHelper.setAttributes(ncCF, rowSizeName, new Attributes()
                    .add("ioos_category", "Identifier")
                    .add("long_name", "Number of Observations for this " + cdmType)
                    .add("sample_dimension", "obs"));  
            }

            //** leave "define" mode
            //If offset to last var is >2GB, netcdf-java library will throw an exception here.
            ncCF.create();

            //write the variables  
            //One var at a time makes it reasonably memory efficient.
            //This needs to reorder the values, which needs all values at once, so can't do in chunks.
            for (int col = 0; col < ncNCols; col++) {
                EDV tEdv = colEdv[col];
                double tSafeMV = tEdv.safeDestinationMissingValue();
                PrimitiveArray pa = twawm.column(col);
                //first re-order
                pa.reorder(rank);

                //convert LongArray to StringArray (since nc3 doesn't support longs)
                if (pa instanceof LongArray)
                    pa = new StringArray(pa);

                if (isFeatureVar[col]) {
                    //write featureVar[feature]
                    pa.justKeep(featureKeep);
                    NcHelper.write(ncCF, ncColNames[col], 0, pa, 
                        twawm.columnMaxStringLength(col)); 

                } else if (nodcMode) {
                    //write nodc obsVar[feature][obs]
                    int origin[] = {0, 0};
                    for (int feature = 0; feature < nFeatures; feature++) { 
                        //write the data for this feature
                        origin[0] = feature;
                        origin[1] = 0;
                        int firstRow = featureFirstRow.get(feature);
                        int tNRows   = featureNRows.get(feature);
                        int stopRow  = firstRow + tNRows - 1;
                        PrimitiveArray subsetPa = pa.subset(firstRow, 1, stopRow);
                        subsetPa.trimToSize(); //so toObjectArray will be underlying array
                        NcHelper.write(ncCF, ncColNames[col], 
                            origin, new int[]{1, tNRows}, subsetPa);         

                        //and write missing values
                        if (tNRows < maxFeatureNRows) {
                            origin[1] = tNRows;
                            //String2.log("  writeMVs: feature=" + feature + " origin[1]=" + origin[1] + 
                            //  " tNRows=" + tNRows + " maxFeatureNRows=" + maxFeatureNRows);
                            tNRows = maxFeatureNRows - tNRows;
                            subsetPa.clear();
                            if (tEdv.destinationDataTypeClass() == long.class)
                                subsetPa.addNStrings(tNRows, "" + tSafeMV);
                            else if (subsetPa instanceof StringArray)
                                subsetPa.addNStrings(tNRows, "");
                            else
                                subsetPa.addNDoubles(tNRows, tSafeMV);
                            NcHelper.write(ncCF, ncColNames[col], 
                                origin, new int[]{1, tNRows}, subsetPa);         
                        }
                    }

                } else {
                    //write obsVar[obs]
                    NcHelper.write(ncCF, ncColNames[col], 0, pa, 
                        twawm.columnMaxStringLength(col)); 
                }
            }

            //write the rowSize values
            if (!nodcMode)
                NcHelper.write(ncCF, rowSizeName, 0, featureNRows, 0); 

            //if close throws Throwable, it is trouble
            ncCF.close(); //it calls flush() and doesn't like flush called separately
            ncCF = null;

            //rename the file to the specified name
            File2.rename(ncCFName + randomInt, ncCFName);

            //diagnostic
            if (reallyVerbose) String2.log("  EDDTable.convertFlatNcToNcCF1 done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
            //String2.log("  .ncCF File created:\n" + NcHelper.dumpString(ncCFName, false));

        } catch (Throwable t) {
            try { 
                if (ncCF != null) {
                    ncCF.close(); 
                    //delete the partial file
                    File2.delete(ncCFName + randomInt);
                }
            } catch (Exception e) {
            }

            throw t;
        }

    }  //end of saveAsNcCF1

    /**
     * Create a two-level TimeSeriesProfile or TrajectoryProfile .ncCF or .ncCFMA file
     * (Discrete Sampling Geometries). 
     * http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries
     * <br>This assumes cdm_data_type=TimeSeriesProfile|TrajectoryProfile and 
     * cdm_timeseries_variables, cdm_trajectory_variables, and/or cdm_profile_variables 
     * are defined correctly. 
     * 
     * @param nodcMode If true, data will be saved in .ncCFMA file conforming to
     *    NODC Templates http://www.nodc.noaa.gov/data/formats/netcdf/
     *    with Multidimensional Array Representation.
     *    If false, data will be saved in .ncCF file with 
     *    Contiguous Ragged Array Representation.
     * @param ncCFName the complete name of the file that will be created
     * @param twawm
     * @throws Throwable if trouble 
     */
    public void saveAsNcCF2(boolean nodcMode, String ncCFName, 
        TableWriterAllWithMetadata twawm) throws Throwable {

        if (reallyVerbose) String2.log("EDDTable.convertFlatNcToNcCF2 " + ncCFName); 
        long time = System.currentTimeMillis();
        String rowSizeName = "rowSize";

        //delete any existing file
        File2.delete(ncCFName);

        //double check that this dataset supports .ncCF (but it should have been tested earlier)
        if (accessibleViaNcCF.length() > 0) 
            throw new SimpleException(accessibleViaNcCF);
        String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
        String lcCdmType = cdmType == null? null : cdmType.toLowerCase();

        //ensure profile_id and timeseries_id|trajectory_id variable is defined 
        //and that cdmType is valid
        int oidDVI = -1;               //'o'ther = timeseries|trajectory
        int pidDVI = profile_idIndex;  //'p'rofile
        String olcCdmName;
        if      (CDM_TIMESERIESPROFILE.equals(cdmType)) {
            oidDVI = timeseries_idIndex;    
            olcCdmName = "timeseries";   }
        else if (CDM_TRAJECTORYPROFILE.equals(cdmType)) {
            oidDVI = trajectory_idIndex; 
            olcCdmName = "trajectory";}
        else throw new SimpleException( //but already checked before calling this method
            "For convertFlatNcToNcCF2(), cdm_data_type must be TimeSeriesProfile or TrajectoryProfile, " +
            "not " + cdmType + ".");
        if (oidDVI < 0) //but already checked by accessibleViaNcCF
            throw new SimpleException("No variable has a cf_role=" + olcCdmName + "_id attribute.");              
        if (pidDVI < 0) //but already checked by accessibleViaNcCF
            throw new SimpleException("No variable has a cf_role=profile_id attribute.");              
        String oidName = dataVariableDestinationNames()[oidDVI];  //var name of ..._id var
        String pidName = dataVariableDestinationNames()[pidDVI];  
        String indexName = olcCdmName + "Index";

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        NetcdfFileWriteable ncCF = null;
        try {
            //get info from twawm
            //Note: this uses 'feature' as stand-in for timeseries|trajectory 
            String ncColNames[] = twawm.columnNames();
            //String2.log("  ncColNames=" + String2.toCSSVString(ncColNames));
            int ncNCols = twawm.nColumns();
            boolean isFeatureVar[] = new boolean[ncNCols];
            boolean isProfileVar[] = new boolean[ncNCols];
            String featureVars[] = combinedGlobalAttributes.getStringsFromCSV(
                "cdm_" + olcCdmName + "_variables");
            String profileVars[] = combinedGlobalAttributes.getStringsFromCSV(
                "cdm_profile_variables");
            EDV colEdv[] = new EDV[ncNCols];
            for (int col = 0; col < ncNCols; col++) {
                isFeatureVar[col] = String2.indexOf(featureVars, ncColNames[col]) >= 0;
                isProfileVar[col] = String2.indexOf(profileVars, ncColNames[col]) >= 0;
                colEdv[col] = findVariableByDestinationName(ncColNames[col]);
            }

            //String2.log("  isFeatureVar=" + String2.toCSSVString(isFeatureVar));
            //String2.log("  isProfileVar=" + String2.toCSSVString(isProfileVar));

            //ensure profile_id and timeseries_id|trajectory_id variable is in flatNc file
            int oidPo = String2.indexOf(ncColNames, oidName);
            int pidPo = String2.indexOf(ncColNames, pidName);
            if (oidPo < 0) //but already checked before calling this method
                throw new SimpleException("The .nc file must have " + oidName);
            if (pidPo < 0) //but already checked before calling this method
                throw new SimpleException("The .nc file must have " + pidName);

            //read oid ('feature'_id) and pid (profile_id) PA from flatNc file
            PrimitiveArray oidPA = twawm.column(oidPo);
            PrimitiveArray pidPA = twawm.column(pidPo);
            //String2.log("  oidPA=" + oidPA.toString());
            //String2.log("  pidPA=" + pidPA.toString());

            //rank oid+pid rows
            //!!! this assumes already sorted by time/depth/... and will stay sorted when re-sorted. 
            //  PrimitiveArray.rank promises stable sort.
            ArrayList tTable = new ArrayList();
            tTable.add(oidPA);
            tTable.add(pidPA);
            int rank[] = PrimitiveArray.rank(tTable, new int[]{0,1}, new boolean[]{true,true});
            tTable = null;
            int tTableNRows = oidPA.size();
            //String2.log("  rank=" + String2.toCSSVString(rank));

            //find unique features (timeseries|trajectory):
            //featureFirstRow (first row in CF file for each feature)            
            IntArray featureIndex = new IntArray();    //'feature' number for each profile
            featureIndex.add(0);                      
            IntArray profileIndexInThisFeature = new IntArray();  //0,1,2, 0,1,2,3, ...
            int tProfileIndexInThisFeature = 0; //first is #0
            profileIndexInThisFeature.add(tProfileIndexInThisFeature);
            BitSet featureKeep = new BitSet();         //initially all false
            featureKeep.set(0);                        //'row' after data is sorted
            int nFeatures = 1;
            IntArray nProfilesPerFeature = new IntArray();
            nProfilesPerFeature.add(1);                //for the initial row

            //find unique profiles:
            //profileFirstRow (first row in CF file for each profile)
            IntArray profileFirstObsRow = new IntArray(); //'row' after data is sorted
            profileFirstObsRow.add(0);                    //first row has first profile
            int prevFirstRow = 0;                      //'row' after data is sorted
            IntArray nObsPerProfile = new IntArray();  
            BitSet profileKeep = new BitSet(); //initially all false
            profileKeep.set(0);                        //'row' after data is sorted

            //'row' after data is sorted
            for (int row = 1; row < tTableNRows; row++) {  //1 since comparing to previous row
                //keeping the tests separate and using featureChanged allows 
                //   profile_id to be only unique within an 'feature' 
                //   (it doesn't have to be globally unique)
                boolean featureChanged = false;                
                if (oidPA.compare(rank[row - 1], rank[row]) != 0) {
                    //found a new feature
                    featureChanged = true;
                    featureKeep.set(row);
                    nFeatures++;
                    nProfilesPerFeature.add(0); 
                    tProfileIndexInThisFeature = -1;
                }
                if (featureChanged || pidPA.compare(rank[row - 1], rank[row]) != 0) {
                    //found a new profile
                    featureIndex.add(nFeatures - 1);      //yes, one for each profile
                    tProfileIndexInThisFeature++;
                    profileIndexInThisFeature.add(tProfileIndexInThisFeature);
                    nProfilesPerFeature.set(nFeatures - 1, tProfileIndexInThisFeature + 1);
                    profileFirstObsRow.add(row);             //for this profile
                    nObsPerProfile.add(row - prevFirstRow); //for the previous profile
                    profileKeep.set(row);
                    prevFirstRow = row;
                }
            }
            nObsPerProfile.add(tTableNRows - prevFirstRow);
            int nProfiles = profileFirstObsRow.size();
            int maxObsPerProfile = Math2.roundToInt(
                nObsPerProfile.calculateStats()[PrimitiveArray.STATS_MAX]);
            int maxProfilesPerFeature = Math2.roundToInt(
                nProfilesPerFeature.calculateStats()[PrimitiveArray.STATS_MAX]);
            if (reallyVerbose) String2.log(
                  "  nFeatures="           + nFeatures + 
                "\n  nProfilesPerFeature=" + nProfilesPerFeature.toString() +
                "\n  nProfiles="           + nProfiles + 
                "\n  featureIndex="        + featureIndex.toString() +
                "\n  profileFirstObsRow="  + profileFirstObsRow.toString() +
                "\n  nObsPerProfile="      + nObsPerProfile.toString());
            //conserve memory
            oidPA = null;
            pidPA = null;

            //** Create the .ncCF file
            ncCF = NetcdfFileWriteable.createNew(ncCFName + randomInt, false);
            //define the dimensions
            Dimension featureDimension = ncCF.addDimension(olcCdmName, nFeatures);
            Dimension profileDimension = ncCF.addDimension("profile",  
                nodcMode? maxProfilesPerFeature : nProfiles);
            Dimension obsDimension     = ncCF.addDimension("obs",
                nodcMode? maxObsPerProfile : tTableNRows);

            //add the feature variables, then profile variables, then obs variables
            for (int opo = 0; opo < 3; opo++) {  //0=feature 1=profile 2=obs
                for (int col = 0; col < ncNCols; col++) {
                    if (( isFeatureVar[col] && opo == 0) ||   //is var in currently desired group?
                        ( isProfileVar[col] && opo == 1) ||
                        (!isFeatureVar[col] && !isProfileVar[col] && opo == 2)) {
                        //keep going
                    } else {
                        continue;
                    }
                    String tColName = ncColNames[col];
                    //String2.log(" opo=" + opo + " col=" + col + " name=" + tColName);
                    ArrayList tDims = new ArrayList();
                    if (nodcMode) {
                        if (isFeatureVar[col]) {
                            //featureVar[feature]     
                            tDims.add(featureDimension);
                        } else if (isProfileVar[col]) {  
                            //profileVar[feature][profile]
                            tDims.add(featureDimension);
                            tDims.add(profileDimension);
                        } else {  
                            //obsVar[feature][profile][obs]
                            tDims.add(featureDimension);
                            tDims.add(profileDimension);
                            tDims.add(obsDimension);
                        }
                    } else {
                        if (isFeatureVar[col]) {  
                            //featureVar[feature]     
                            tDims.add(featureDimension);
                        } else if (isProfileVar[col]) {
                            //profileVar[profile]
                            tDims.add(profileDimension);
                        } else {
                            //obsVar[obs]
                            tDims.add(obsDimension);
                        }
                    }
                    Class type = colEdv[col].destinationDataTypeClass();
                    int maxStringLength = 1;
                    if (type == String.class) {
                        maxStringLength = Math.max(1, twawm.columnMaxStringLength(col)); //nclib wants at least 1
                    } else if (type == long.class) {
                        //store longs as Strings since nc3 doesn't support longs
                        type = String.class; 
                        maxStringLength = NcHelper.LONG_MAXSTRINGLENGTH;
                    }

                    if (type == String.class) {
                        ncCF.addStringVariable(tColName, tDims, maxStringLength);
                    } else {
                        ncCF.addVariable(tColName, DataType.getType(type), tDims); 
                    }

                    //nodcMode: ensure all numeric obs variables have missing_value or _FillValue
                    //(String vars would have "", but that isn't a valid attribute)
                    if (nodcMode && opo == 2 && type != String.class) {
                        Attributes atts = twawm.columnAttributes(col);
                        if (atts.getString("missing_value") == null && //getString so distinguish null and NaN
                            atts.getString("_FillValue")    == null) {
                            //"" will become native numeric MV, e.g., 127 for ByteArray
                            atts.set("_FillValue", PrimitiveArray.factory(type, 1, "")); 
                        }
                    }
                }

                //at end of profile vars, add 'feature'Index and rowSize variables
                if (!nodcMode && opo == 1) {
                    ncCF.addVariable(indexName, DataType.getType(int.class), 
                        new Dimension[]{profileDimension}); //yes, profile, since there is one for each profile
                    ncCF.addVariable(rowSizeName, DataType.getType(int.class), 
                        new Dimension[]{profileDimension}); 
                    //String2.log("  featureIndex and rowSize variable added");
                }
            }
            
            //set the global attributes
            //currently, only the .nc saveAs types use attributes!
            Attributes cdmGlobalAttributes = twawm.globalAttributes();            
            String featureType = cdmType;
            cdmGlobalAttributes.remove("observationDimension"); //obsDim is for a flat file and OBSOLETE
            cdmGlobalAttributes.set("featureType", featureType);
            cdmGlobalAttributes.set("id", File2.getNameNoExtension(ncCFName)); //id attribute = file name
            cdmGlobalAttributes.set(                            "Conventions", 
                ensureAtLeastCF16(cdmGlobalAttributes.getString("Conventions")));
            cdmGlobalAttributes.set(                            "Metadata_Conventions", 
                ensureAtLeastCF16(cdmGlobalAttributes.getString("Metadata_Conventions")));
            NcHelper.setAttributes(ncCF, "NC_GLOBAL", cdmGlobalAttributes);

            //set the variable attributes
            //section 9.1.1 says "The coordinates attribute must identify the variables 
            //  needed to geospatially position and time locate the observation. 
            //  The lat, lon and time coordinates must always exist; a vertical
            //  coordinate may exist."
            //so always include time, lat, lon, even if they use obs dimension.
            boolean isCoordinateVar[] = new boolean[ncNCols];  //all false
            isCoordinateVar[String2.indexOf(ncColNames, "longitude")] = true;
            isCoordinateVar[String2.indexOf(ncColNames, "latitude" )] = true;
            isCoordinateVar[String2.indexOf(ncColNames, "time"     )] = true;
            String coordinates = "time latitude longitude"; //spec doesn't specify a specific order
            String proxy = combinedGlobalAttributes.getString("cdm_altitude_proxy");
            if (altIndex >= 0) {
                coordinates += " altitude";
                isCoordinateVar[String2.indexOf(ncColNames, "altitude")] = true;
            } else if (depthIndex >= 0) {
                coordinates += " depth";
                isCoordinateVar[String2.indexOf(ncColNames, "depth")] = true;
            } else if (proxy != null) {
                //it is in requiredCfRequestVariables
                coordinates += " " + proxy;
                isCoordinateVar[String2.indexOf(ncColNames, proxy)] = true; 
            }
            for (int col = 0; col < ncNCols; col++) {
                Attributes atts = twawm.columnAttributes(col);
                if (!isFeatureVar[col] && !isProfileVar[col] && !isCoordinateVar[col])
                    atts.add("coordinates", coordinates);
                NcHelper.setAttributes(ncCF, ncColNames[col], atts);
            }

            if (!nodcMode) {
                //set the index attributes
                NcHelper.setAttributes(ncCF, indexName, new Attributes()
                    .add("ioos_category", "Identifier")
                    .add("long_name", "The " + olcCdmName + " to which this profile is associated.")
                    .add("instance_dimension", olcCdmName));

                //set the rowSize attributes
                NcHelper.setAttributes(ncCF, rowSizeName, new Attributes()
                    .add("ioos_category", "Identifier")
                    .add("long_name", "Number of Observations for this Profile")
                    .add("sample_dimension", "obs"));
            }

            //** leave "define" mode
            //If offset to last var is >2GB, netcdf-java library will throw an exception here.
            ncCF.create();

            //write the variables  
            //One var at a time makes it reasonably memory efficient.
            //This needs to reorder the values, which needs all values at once, so can't do in chunks.
            for (int col = 0; col < ncNCols; col++) {
                PrimitiveArray pa = twawm.column(col);
                EDV tEdv = colEdv[col];
                double tSafeMV = tEdv.safeDestinationMissingValue();

                //first re-order
                pa.reorder(rank);
                if (pa instanceof LongArray)
                    pa = new StringArray(pa);

                if (isFeatureVar[col]) {
                    //write featureVar[feature]
                    pa.justKeep(featureKeep);
                    NcHelper.write(ncCF, ncColNames[col], 0, pa, 
                        twawm.columnMaxStringLength(col));

                } else if (isProfileVar[col]) {
                    pa.justKeep(profileKeep);
                    if (nodcMode) {
                        //write nodc profileVar[feature][profile]
                        int origin[] = new int[2];
                        int firstRow;
                        int lastRow = -1;
                        for (int feature = 0; feature < nFeatures; feature++) { 
                            //write data
                            origin[0] = feature;
                            origin[1] = 0;
                            int tnProfiles = nProfilesPerFeature.get(feature);
                            firstRow = lastRow + 1;
                            lastRow = firstRow + tnProfiles - 1;
                            PrimitiveArray tpa = pa.subset(firstRow, 1, lastRow);                        
                            NcHelper.write(ncCF, ncColNames[col], 
                                origin, new int[]{1, tnProfiles}, tpa);   
                            
                            //write fill values
                            int nmv = maxProfilesPerFeature - tnProfiles;
                            if (nmv > 0) {
                                origin[1] = tnProfiles;
                                tpa.clear();
                                if (tEdv.destinationDataTypeClass() == long.class)
                                    tpa.addNStrings(nmv, "" + tSafeMV);
                                else if (tpa instanceof StringArray) 
                                    tpa.addNStrings(nmv, "");
                                else
                                    tpa.addNDoubles(nmv, tSafeMV);
                                NcHelper.write(ncCF, ncColNames[col], 
                                    origin, new int[]{1, nmv}, tpa);         
                            }
                        }
                    } else {
                        //write ragged array var[profile]
                        NcHelper.write(ncCF, ncColNames[col], 
                            new int[]{0}, new int[]{pa.size()}, pa);         
                    }

                } else if (nodcMode) {
                    //write nodc obsVar[feature][profile][obs] as MultidimensionalArray
                    int origin[] = {0, 0, 0};
                    
                    //fill with mv
                    PrimitiveArray subsetPa = PrimitiveArray.factory(
                        pa.elementClass(), maxObsPerProfile, 
                        pa.elementClass() == String.class? "" : "" + tSafeMV);
                    int shape[] = {1, 1, maxObsPerProfile};
                    for (int feature = 0; feature < nFeatures; feature++) { 
                        origin[0] = feature;
                        for (int profile = 0; profile < maxProfilesPerFeature; profile++) { 
                            origin[1] = profile;
                            NcHelper.write(ncCF, ncColNames[col],
                                origin, shape, subsetPa);
                        }
                    }

                    //write obsVar[feature][profile][obs] as MultidimensionalArray
                    for (int profile = 0; profile < nProfiles; profile++) { 
                        origin[0] = featureIndex.get(profile);  //feature#
                        origin[1] = profileIndexInThisFeature.get(profile);
                        origin[2] = 0;
                        int firstRow = profileFirstObsRow.get(profile);
                        int tNRows   = nObsPerProfile.get(profile);
                        int stopRow  = firstRow + tNRows - 1;
                        subsetPa = pa.subset(firstRow, 1, stopRow);
                        shape[2] = tNRows;
                        NcHelper.write(ncCF, ncColNames[col], origin, shape, subsetPa);         
                    }

                } else {
                    //write obsVar[obs] as Contiguous Ragged Array
                    NcHelper.write(ncCF, ncColNames[col], 0, pa, 
                        twawm.columnMaxStringLength(col));
                }
            }

            if (!nodcMode) {
                //write the featureIndex and rowSize values
                NcHelper.write(ncCF, indexName,   0, featureIndex, 0); 
                NcHelper.write(ncCF, rowSizeName, 0, nObsPerProfile, 0); 
            }

            //if close throws Throwable, it is trouble
            ncCF.close(); //it calls flush() and doesn't like flush called separately
            ncCF = null;

            //rename the file to the specified name
            File2.rename(ncCFName + randomInt, ncCFName);

            //diagnostic
            if (reallyVerbose) String2.log("  EDDTable.convertFlatNcToNcCF2 done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
            //String2.log("  .ncCF File:\n" + NcHelper.dumpString(ncCFName, false));

        } catch (Throwable t) {
            try { 
                if (ncCF != null) {
                    ncCF.close(); 
                    //delete the partial file
                    File2.delete(ncCFName + randomInt);
                }
            } catch (Exception e) {
            }

            throw t;
        } 

    }  //end of saveAsNcCF2

    /**
     * Save the TableWriterAllWithMetadata data as an ODV .tsv file.
     * This writes a few attributes.
     * <br>See the User's Guide section 16.3 etc. http://odv.awi.de/en/documentation/
     * (or Bob's c:/programs/odv/odv4Guide.pdf).
     * <br>The data must have longitude, latitude, and time columns.
     * <br>Longitude can be any values (e.g., -180 or 360).
     * 
     * @param outputStreamSource
     * @param twawm  all the results data, with missingValues stored as destinationMissingValues
     *    or destinationFillValues  (they are converted to NaNs)
     * @param tDatasetID
     * @param tPublicSourceUrl
     * @param tInfoUrl
     * @throws Throwable 
     */
    public static void saveAsODV(OutputStreamSource outputStreamSource, 
        TableWriterAll twawm, 
        String tDatasetID, String tPublicSourceUrl, String tInfoUrl) 
        throws Throwable {

        if (reallyVerbose) String2.log("EDDTable.saveAsODV"); 
        long time = System.currentTimeMillis();

        //make sure there is data
        long tnRows = twawm.nRows();
        if (tnRows == 0)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA);
        //the other params are all required by EDD, so it's a programming error if they are missing
        if (tDatasetID == null || tDatasetID.length() == 0)
            throw new SimpleException(EDStatic.errorInternal + 
                "saveAsODV error: datasetID wasn't specified.");
        if (tPublicSourceUrl == null || tPublicSourceUrl.length() == 0)
            throw new SimpleException(EDStatic.errorInternal + 
                "saveAsODV error: publicSourceUrl wasn't specified.");
        if (tInfoUrl == null || tInfoUrl.length() == 0)
            throw new SimpleException(EDStatic.errorInternal + 
                "saveAsODV error: infoUrl wasn't specified.");

        //make sure there isn't too much data before getting outputStream
        Table table = twawm.cumulativeTable(); //it checks memory usage
        twawm = null; //encourage it to be gc'd
        int nCols = table.nColumns();
        int nColsM1 = nCols - 1;
        int nRows = table.nRows();

        //ensure there is longitude, latitude, time data in the request (else it is useless in ODV)
        if (table.findColumnNumber(EDV.LON_NAME)  < 0 ||
            table.findColumnNumber(EDV.LAT_NAME)  < 0 || 
            table.findColumnNumber(EDV.TIME_NAME) < 0)
            throw new SimpleException(EDStatic.queryError +
                MessageFormat.format(EDStatic.queryErrorLLT, ".odvTxt"));

        //convert numeric missing values to NaN
        table.convertToStandardMissingValues();

        //open an OutputStream   
        OutputStreamWriter writer = new OutputStreamWriter(outputStreamSource.outputStream(
            "ISO-8859-1")); //ODV User's Guide 16.3 says ASCII (7bit), so might as well go for compatible common 8bit

        //write header
        //ODV says linebreaks can be \n or \r\n (see 2010-06-15 notes)
        String creator = tPublicSourceUrl; 
        if (creator.startsWith("(")) //(local files) or (local database)
            creator = tInfoUrl; //a required attribute
        //ODV says not to encodeAsXML (e.g., < as &lt;) (see 2010-06-15 notes), 
        //but he may have misunderstood. Encoding seems necessary.  (And affects only the creator)
        writer.write(
            "//<Creator>" + XML.encodeAsXML(creator) + "</Creator>\n" + //the way to get to the original source
            "//<CreateTime>" + Calendar2.getCurrentISODateTimeStringZulu() + "</CreateTime>\n" + //nowZ
            "//<Software>ERDDAP - Version " + EDStatic.erddapVersion + "</Software>\n" + //ERDDAP
            "//<Source>" + EDStatic.erddapUrl + "/tabledap/" + tDatasetID + ".html</Source>\n" + //Data Access Form
            //"//<SourceLastModified>???</SourceLastModified>\n" + //not available
            "//<Version>ODV Spreadsheet V4.0</Version>\n" + //of ODV Spreadsheet file
            //"//<MissingValueIndicators></MissingValueIndicators>\n" + //only use for non-empty cell, non-NaN, e.g., -9999
            "//<DataField>GeneralField</DataField>\n" + //!!! better if Ocean|Atmosphere|Land|IceSheet|SeaIce|Sediment
            "//<DataType>GeneralType</DataType>\n"); //!!! better if GeneralType|Profiles|TimeSeries|Trajectories

        //try to find required columns
        //Presence of longitude, latitude, time is checked above.
        int cruiseCol = -1;
        int stationCol = -1;
        int timeCol = table.findColumnNumber(EDV.TIME_NAME);  //but ODV requires it. see above
        //2010-07-07 email from Stephan Heckendorff says altitude (or similar) if present MUST be primaryVar
        int primaryVarCol = table.findColumnNumber(EDV.ALT_NAME); 
        if (primaryVarCol < 0) 
            primaryVarCol = table.findColumnNumber("depth"); 

        //is a column perfectly named?
        for (int col = 0; col < nCols; col++) {
            String lcColName = table.getColumnName(col).toLowerCase();
            if (lcColName.equals("cruise"))  cruiseCol = col;
            if (lcColName.equals("station")) stationCol = col;
            if (primaryVarCol < 0 && 
                (lcColName.equals("altitude") ||
                 lcColName.equals("depth")    ||
                 lcColName.equals("position") ||
                 lcColName.equals("pressure") ||
                 lcColName.equals("sigma")))
                primaryVarCol = col;
        }

        //if not found, find imperfectly named column
        if (cruiseCol == -1 || stationCol == -1) {
            //(use the first likely column)
            for (int col = 0; col < nCols; col++) {
                String lcColName = table.getColumnName(col).toLowerCase();
                if (cruiseCol == -1 && lcColName.indexOf("cruise") >= 0)   
                    cruiseCol = col;
                if (stationCol == -1 && lcColName.indexOf("station") >= 0) 
                    stationCol = col;
                if (primaryVarCol < 0 && 
                    (lcColName.indexOf("altitude") >= 0 ||
                     lcColName.indexOf("depth")    >= 0 ||
                     lcColName.indexOf("position") >= 0 ||
                     lcColName.indexOf("pressure") >= 0 ||
                     lcColName.indexOf("sigma")    >= 0))
                    primaryVarCol = col;
            }
        }

        //write column names
        //Mandatory columns that we don't have data for are always first.
        //ERDDAP hardly ever has "Bot. Depth"-info, so don't ever use it (it isn't madatory).
        writer.write("Type:METAVAR:TEXT:2");         //do these need :METAVAR:TEXT:2? or ...:1? or nothing?
        if (cruiseCol == -1)
            writer.write("\tCruise:METAVAR:TEXT:2");
        if (stationCol == -1)
            writer.write("\tStation:METAVAR:TEXT:2");
        if (timeCol == -1)
            writer.write("\tyyyy-mm-ddThh:mm:ss.sss");

        //columns from selected data
        boolean isTimeStamp[] = new boolean[nCols];
        for (int col = 0; col < nCols; col++) {

            //write tab first (since Type column comes before first table column)
            writer.write('\t');

            //prepare the units
            String colName = table.getColumnName(col);
            String colNameLC = colName.toLowerCase();
            String units = table.columnAttributes(col).getString("units");
            isTimeStamp[col] = EDV.TIME_UNITS.equals(units); //units may be null
            if (units == null || units.length() == 0) {
                units = "";
            } else {
                //ODV doesn't care about units standards. UDUNITS or UCUM are fine
                //ODV doesn't allow internal brackets; 2010-06-15 they say use parens
                units = String2.replaceAll(units, '[', '(');
                units = String2.replaceAll(units, ']', ')');
                units = " [" + units + "]";
            }

            //make ODV type  BYTE, SHORT, ... , TEXT:81
            //16.3.3 says station labels can be numeric or TEXT
            String type = "";
            PrimitiveArray pa = table.getColumn(col);
            if      (pa instanceof ByteArray)   type = "BYTE";
            else if (pa instanceof CharArray)   type = "SHORT";
            else if (pa instanceof ShortArray)  type = "SHORT";   //!
            else if (pa instanceof IntArray)    type = "INTEGER";
            else if (pa instanceof LongArray)   type = "DOUBLE";  //!
            else if (pa instanceof FloatArray)  type = "FLOAT";
            else if (pa instanceof DoubleArray) type = "DOUBLE";
            //maxStringLenth + 1 byte used to hold string length
            //since 1 byte used for string length, max length must be 255
            else if (pa instanceof StringArray) type = "TEXT:" + 
                Math.min(255, ((StringArray)pa).maxStringLength() + 1);
            else throw new SimpleException(EDStatic.errorInternal + 
                "Unexpected data type=" + pa.elementClassString() +
                " for column=" + colName + ".");

            //ODV .txt files are ASCII (7 bit) only. I could use 
            //colName = String2.modifyToBeASCII(colName);            
            //but that isn't ideal. ODV will display correct ISO-8859-1 text
            //on most 'Western' computers (where ODV is most commonly used).
            //So leave high ascii characters in place and hope for best.
            //!!! I think ERDDAP just allows A-Z a-z 0-9 _, so all are safe.

            //Ensure colName name isn't "Type", which conflicts with ODV "Type" above
            if (colName.equals("Type"))
                colName = "Type2"; //crude

            //METAVAR columns
            //Main time must have colName=yyyy-mm-ddThh:mm:ss.sss 
            //  (it's okay that ERDDAP doesn't use .sss for actual values)
            //Secondary time (only one allowed) must have colName=time_ISO8601
            if      (colName.equals(EDV.LON_NAME)) writer.write("Longitude [degrees_east]:METAVAR:" + type);
            else if (colName.equals(EDV.LAT_NAME)) writer.write("Latitude [degrees_north]:METAVAR:" + type);
            else if (col == timeCol)               writer.write("yyyy-mm-ddThh:mm:ss.sss");
            else if (isTimeStamp[col])             writer.write("time_ISO8601"); //after test col==timeCol
            else if (col == cruiseCol)             writer.write("Cruise:METAVAR:" + type);
            else if (col == stationCol)            writer.write("Station:METAVAR:" + type);
            //all string columns are METAVARs
            else if (type.startsWith("TEXT:"))     writer.write(colName + ":METAVAR:" + type);

            //it's a data variable 
            //!!! I don't support the ODV system of quality flag variables 
            else {
                //is it the one primaryVar?
                if (primaryVarCol < 0 && 
                    colNameLC.indexOf("ship") < 0 &&
                    colNameLC.indexOf("station") < 0) {
                    primaryVarCol = col;
                }
                String primary = primaryVarCol == col? ":PRIMARYVAR" : "";

                //write the column name
                writer.write(colName + units + primary + ":" + type);
            }
        }
        writer.write('\n');

        //write data
        for (int row = 0; row < nRows; row++) {
            //write mandatory columns that we don't have data for
            //Type: better if a specific type, see section 16.3.3
            //  'B' for stations with <250 samples (e.g., bottle data) 
            //  'C' for stations with >=250 samples (e.g., CTD, XBT, etc.). 
            //  '*' lets ODV make the choice.
            //  If Bot. Depth values are not available, you should leave this field empty.  (meaning '*'?)
            writer.write('*');  
            if (cruiseCol < 0)
                writer.write("\t");
            if (stationCol < 0)
                writer.write("\t");
            if (timeCol < 0)
                writer.write("\t");
            for (int col = 0; col < nCols; col++) {
                writer.write('\t');  //since Type and other columns were written above
                if (isTimeStamp[col]) {
                    //no Z at end;  ODV ignores time zone info (see 2010-06-15 notes)
                    //!!! Keep as ISO 8601, not time_precision.
                    writer.write(Calendar2.safeEpochSecondsToIsoStringT(table.getDoubleData(col, row), "")); 
                    //missing numeric will be empty cell; that's fine
                } else {
                    //See comments above about ISO-8859-1. I am choosing *not* to strip high ASCII chars here.
                    writer.write(table.getStringData(col, row)); 
                }
            }
            writer.write('\n');
        }

        //done!
        writer.flush(); //essential

        if (reallyVerbose) String2.log("  EDDTable.saveAsODV done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
    }


    /**
     * This is used by administrators to get the empirical min and max for all 
     *   non-String variables by doing a request for a time range (sometimes one time point).
     * This could be used by constructors (at the end),
     * but the results vary with different isoDateTimes,
     * and would be wasteful to do this every time a dataset is constructed 
     * if results don't change.
     *
     * @param minTime the ISO 8601 min time to check (use null or "" if no min limit)
     * @param maxTime the ISO 8601 max time to check (use null or "" if no max limit)
     * @param makeChanges if true, the discovered values are used to set 
     *    the variable's min and max values if they aren't already set
     * @param stringsToo if true, this determines if the String variables have any values
     * @throws Throwable if trouble
     */
    public void getEmpiricalMinMax(String loggedInAs, String minTime, String maxTime, 
            boolean makeChanges, boolean stringsToo) throws Throwable {
        if (verbose) String2.log("\nEDDTable.getEmpiricalMinMax for " + datasetID + ", " + 
            minTime + " to " + maxTime + " total nVars=" + dataVariables.length);
        long downloadTime = System.currentTimeMillis();

        StringBuilder query = new StringBuilder();
        int first = 0;     //change temporarily for bmde to get e.g., 0 - 100, 100 - 200, ...
        int last = dataVariables.length;  //change temporarily for bmde
        for (int dv = first; dv < last; dv++) {
            //get vars
            if (stringsToo || !dataVariables[dv].destinationDataType().equals("String"))
                query.append("," + dataVariables[dv].destinationName());
        }
        if (query.length() == 0) {
            String2.log("All variables are String variables.");
            return;
        }
        query.deleteCharAt(0); //first comma
        if (minTime != null && minTime.length() > 0)
            query.append("&time>=" + minTime);
        if (maxTime != null && maxTime.length() > 0)
            query.append("&time<=" + maxTime);

        //query
        String dir = EDStatic.fullTestCacheDirectory;
        String fileName = datasetID + Math2.random(Integer.MAX_VALUE);
        TableWriterAllWithMetadata twawm = getTwawmForDapQuery(
            loggedInAs, "", query.toString());
        Table table = twawm.cumulativeTable();
        twawm.releaseResources();
        String2.log("  downloadTime=" + (System.currentTimeMillis() - downloadTime));
        String2.log("  found nRows=" + table.nRows());
        for (int col = 0; col < table.nColumns(); col++) {
            String destName = table.getColumnName(col);
            EDV edv = findDataVariableByDestinationName(destName); //error if not found
            if (edv.destinationDataType().equals("String")) {
                String2.log("  " + destName + ": is String variable; maxStringLength found = " +
                    twawm.columnMaxStringLength(col) + "\n");
            } else {
                String tMin, tMax; 
                if (PrimitiveArray.isIntegerType(edv.destinationDataTypeClass())) {
                    tMin = "" + Math2.roundToLong(twawm.columnMinValue[col]); 
                    tMax = "" + Math2.roundToLong(twawm.columnMaxValue[col]); 
                } else {
                    tMin = "" + twawm.columnMinValue[col]; 
                    tMax = "" + twawm.columnMaxValue[col]; 
                }
                if (verbose) {
                    double loHi[] = Math2.suggestLowHigh(twawm.columnMinValue[col], twawm.columnMaxValue[col]);
                    String2.log("  " + destName + ":\n" +
                    "                <att name=\"actual_range\" type=\"" + edv.destinationDataType().toLowerCase() + 
                        "List\">" + tMin + " " + tMax + "</att>\n" +
                    "                <att name=\"colorBarMinimum\" type=\"double\">" + loHi[0] + "</att>\n" +
                    "                <att name=\"colorBarMaximum\" type=\"double\">" + loHi[1] + "</att>\n");
                }
                if (makeChanges && 
                    Double.isNaN(edv.destinationMin()) &&
                    Double.isNaN(edv.destinationMax())) {

                    edv.setDestinationMin(twawm.columnMinValue[col] * edv.scaleFactor() + edv.addOffset());
                    edv.setDestinationMax(twawm.columnMaxValue[col] * edv.scaleFactor() + edv.addOffset());
                    edv.setActualRangeFromDestinationMinMax();
                }
            }
        }
        if (verbose) String2.log("\ntotal nVars=" + dataVariables.length);
    }

    /**
     * This is used by administrator to get min,max time
     *   by doing a request for lon,lat.
     * This could be used by constructors (at the end).
     * The problem is that the results vary with different isoDateTimes,
     * and wasteful to do this every time constructed (if results don't change).
     *
     * @param lon the lon to check
     * @param lat the lat to check
     * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.
     * @throws Throwable if trouble
     */
    public void getMinMaxTime(String loggedInAs, double lon, double lat, 
            String dir, String fileName) throws Throwable {
        if (verbose) String2.log("\nEDDTable.getMinMaxTime for lon=" + lon + " lat=" + lat);

        StringBuilder query = new StringBuilder();
        for (int dv = 0; dv < dataVariables.length; dv++)
            //get all vars since different vars at different altitudes
            query.append("," + dataVariables[dv].destinationName());
        query.deleteCharAt(0); //first comma
        query.append("&" + EDV.LON_NAME + "=" + lon + "&" + EDV.LAT_NAME + "=" + lat);

        //auto get source min/max time   for that one lat,lon location
        TableWriterAllWithMetadata twawm = getTwawmForDapQuery(
            loggedInAs, "", query.toString());
        Table table = twawm.makeEmptyTable(); //no need for twawm.cumulativeTable();
        twawm.releaseResources();
        int timeCol = table.findColumnNumber(EDV.TIME_NAME);
        double tMin = twawm.columnMinValue(timeCol); 
        double tMax = twawm.columnMaxValue(timeCol);
        if (verbose) String2.log("  found time min=" + tMin + "=" +
            (Double.isNaN(tMin)? "" : Calendar2.epochSecondsToIsoStringT(tMin)) + 
            " max=" + tMax + "=" +
            (Double.isNaN(tMax)? "" : Calendar2.epochSecondsToIsoStringT(tMax)));
        if (!Double.isNaN(tMin)) dataVariables[timeIndex].setDestinationMin(tMin); //scaleFactor,addOffset not supported
        if (!Double.isNaN(tMax)) dataVariables[timeIndex].setDestinationMax(tMax);
        dataVariables[timeIndex].setActualRangeFromDestinationMinMax();
    }


    /**
     * This writes an HTML form requesting info from this dataset 
     * (like the OPeNDAP Data Access Forms, DAF).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
     * @param writer
     * @throws Throwable if trouble
     */
    public void writeDapHtmlForm(String loggedInAs,
        String userDapQuery, Writer writer) throws Throwable {
        HtmlWidgets widgets = new HtmlWidgets("", true, EDStatic.imageDirUrl(loggedInAs));

        //parse userDapQuery 
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        if (userDapQuery == null)
            userDapQuery = "";
        userDapQuery = userDapQuery.trim();
        StringArray resultsVariables = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps = new StringArray();
        StringArray constraintValues = new StringArray();
        if (userDapQuery == null)
            userDapQuery = "";
        try {
            parseUserDapQuery(userDapQuery, resultsVariables,
                constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
                true);
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log(MustBe.throwableToString(t));
            userDapQuery = ""; //as if no userDapQuery
        }

        //get the distinctOptionsTable
        String distinctOptions[][] = null;
        if (accessibleViaSubset().length() == 0) 
            distinctOptions = distinctOptionsTable(loggedInAs);

        //beginning of form
        String liClickSubmit = "\n" +
            "  <li> " + EDStatic.EDDClickOnSubmitHtml + "\n" +
            "  </ol>\n";
        writer.write("&nbsp;\n"); //necessary for the blank line before the form (not <p>)
        String formName = "form1";
        String docFormName = "document.form1";
        writer.write(HtmlWidgets.ifJavaScriptDisabled);
        writer.write(widgets.beginForm(formName, "GET", "", ""));
        writer.write(HtmlWidgets.PERCENT_ENCODE_JS);

        //begin table
        writer.write(widgets.beginTable(0, 0, "")); 

        //write the table's column names   
        String gap = "&nbsp;&nbsp;&nbsp;";
        writer.write(
            "<tr>\n" +
            "  <th nowrap align=\"left\">" + EDStatic.EDDTableVariable + " " +
            EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableTabularDatasetHtml));
        
        StringBuilder checkAll = new StringBuilder();
        StringBuilder uncheckAll = new StringBuilder();
        int nDv = dataVariables.length;
        for (int dv = 0; dv < nDv; dv++) {
            checkAll.append(  formName + ".varch" + dv + ".checked=true;");
            uncheckAll.append(formName + ".varch" + dv + ".checked=false;");
        }
        writer.write(widgets.button("button", "CheckAll", EDStatic.EDDTableCheckAllTooltip,
            EDStatic.EDDTableCheckAll,   "onclick=\"" + checkAll.toString() + "\""));
        writer.write(widgets.button("button", "UncheckAll", EDStatic.EDDTableUncheckAllTooltip,
            EDStatic.EDDTableUncheckAll, "onclick=\"" + uncheckAll.toString() + "\""));

        writer.write(
            "  &nbsp;</th>\n" +
            "  <th colspan=\"2\">" + EDStatic.EDDTableOptConstraint1Html + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableConstraintHtml) + "</th>\n" +
            "  <th colspan=\"2\">" + EDStatic.EDDTableOptConstraint2Html + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableConstraintHtml) + "</th>\n" +

            "  <th nowrap align=\"left\">" + gap + EDStatic.EDDMinimum + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableMinimumTooltip) + 
                (distinctOptions == null? "<br>&nbsp;" :
                    "<br>" + gap + "or a List of Distinct Values " + 
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.distinctValuesHtml)) +
                "</th>\n" +
            "  <th nowrap align=\"left\">" + gap + EDStatic.EDDMaximum + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableMaximumTooltip) + 
                "<br>&nbsp;" +
                "</th>\n" +
            "</tr>\n");

        //a row for each dataVariable
        String sliderFromNames[] = new String[nDv];
        String sliderToNames[] = new String[nDv];
        int sliderNThumbs[] = new int[nDv];
        String sliderUserValuesCsvs[] = new String[nDv];
        int sliderInitFromPositions[] = new int[nDv];
        int sliderInitToPositions[] = new int[nDv];
        for (int dv = 0; dv < nDv; dv++) {
            EDV edv = dataVariables[dv];
            double tMin = edv.destinationMin();
            double tMax = edv.destinationMax();
            boolean isTime = dv == timeIndex;
            boolean isTimeStamp = edv instanceof EDVTimeStamp;
            boolean isString = edv.destinationDataTypeClass() == String.class;
            String tTime_precision = isTimeStamp? ((EDVTimeStamp)edv).time_precision() : null;

            writer.write("<tr>\n");
            
            //get the extra info   
            String extra = edv.units();
            if (isTimeStamp)
                extra = "UTC"; //no longer true: "seconds since 1970-01-01..."
            if (extra == null) 
                extra = "";
            if (showLongName(edv.destinationName(), edv.longName())) 
                extra = edv.longName() + (extra.length() == 0? "" : ", " + extra);
            if (extra.length() > 0) 
                extra = " (" + extra + ")";

            //variables: checkbox varname (longName, units)
            writer.write("  <td nowrap>");
            writer.write(widgets.checkbox("varch" + dv, EDStatic.EDDTableCheckTheVariables, 
                userDapQuery.length() == 0? true : (resultsVariables.indexOf(edv.destinationName()) >= 0), 
                edv.destinationName(), 
                edv.destinationName() + extra + " " +
                    EDStatic.htmlTooltipImageEDV(loggedInAs, edv), 
                ""));
            writer.write("  &nbsp;</td>\n");

            //get default constraints
            String[] tOp = {">=", "<="};
            double[] tValD = {Double.NaN, Double.NaN};
            String[] tValS = {null, null};
            if (userDapQuery.length() > 0) {
                //get constraints from userDapQuery?
                boolean done0 = false, done1 = false;
                //find first 2 constraints on this var
                for (int con = 0; con < 2; con++) { 
                    int tConi = constraintVariables.indexOf(edv.destinationName());
                    if (tConi >= 0) {
                        String cOp = constraintOps.get(tConi); 
                        int putIn = cOp.startsWith("<") || done0? 1 : 0; //prefer 0 first
                        if (putIn == 0) done0 = true; 
                        else done1 = true;
                        tOp[putIn] = cOp;
                        tValS[putIn] = constraintValues.get(tConi);
                        tValD[putIn] = String2.parseDouble(tValS[putIn]);
                        if (isTimeStamp) {
                            //time constraint will be stored as double (then as a string)
                            tValS[putIn] = Calendar2.epochSecondsToLimitedIsoStringT(
                                tTime_precision, tValD[putIn], "");
                        }
                        constraintVariables.remove(tConi);
                        constraintOps.remove(tConi);
                        constraintValues.remove(tConi);
                    }
                }
            } else {
                if (isTime) {
                    double ttMax = tMax;
                    if (Math2.isFinite(ttMax)) {
                        //only set max request if tMax is known
                        tValD[1] = ttMax;
                        tValS[1] = Calendar2.epochSecondsToLimitedIsoStringT(
                            tTime_precision, ttMax, "");
                    }
                    tValD[0] = Calendar2.backNDays(7, ttMax); //NaN -> now
                    tValS[0] = Calendar2.epochSecondsToLimitedIsoStringT(
                        tTime_precision, tValD[0], "");
                }
            }

            //write constraints html 
            String valueWidgetName = "val" + dv + "_";
            String tTooltip = 
                isTimeStamp? EDStatic.EDDTableTimeConstraintHtml :
                isString?    EDStatic.EDDTableStringConstraintHtml :
                             EDStatic.EDDTableNumericConstraintHtml;
            if (edv.destinationMinString().length() > 0)
                tTooltip += "<br>&nbsp;<br>" + 
                    MessageFormat.format(EDStatic.rangesFromTo, 
                        edv.destinationName(),
                        edv.destinationMinString(),
                        edv.destinationMaxString().length() > 0? 
                            edv.destinationMaxString() : "(?)");
            for (int con = 0; con < 2; con++) {
                writer.write("  <td nowrap>" + (con == 0? "" : gap));
                writer.write(widgets.select("op" + dv + "_" + con, EDStatic.EDDTableSelectAnOperator, 1,
                    OPERATORS, String2.indexOf(OPERATORS, tOp[con]), ""));
                writer.write("  </td>\n");
                writer.write("  <td nowrap>");
                String tVal = tValS[con];
                if (tVal == null) {
                    tVal = "";
                } else if (tOp[con].equals(PrimitiveArray.REGEX_OP) || isString) {
                    tVal = String2.toJson(tVal); //enclose in "
                } 
                writer.write(widgets.textField(valueWidgetName + con, tTooltip,
                    20, 255, tVal, ""));
                writer.write("  </td>\n");
            }

            //distinct options   or min and max
            int whichDTCol = distinctOptions == null? -1 :
                String2.indexOf(subsetVariables, edv.destinationName());
            if (whichDTCol >= 0) {
                //show the distinct options
                writer.write( //left justified (the default) is best if the distinct values are really long
                    "  <td colspan=\"2\" nowrap>" +
                    widgets.beginTable(0, 0, "") + //table for distinctOptions
                    "  <tr>\n" +
                    "  <td nowrap>&nbsp;&nbsp;");
                String tVal0 = tValS[0];
                if (tVal0 == null) {
                    tVal0 = "";
                } else if (isString) {
                    tVal0 = String2.replaceAll(String2.toJson(tVal0), '\u00A0', ' '); //enclose in "
                } 
                int tWhich = String2.indexOf(distinctOptions[whichDTCol], tVal0);
                //String2.log("*** tVal0=" + String2.annotatedString(tVal0) + 
                //         " a disOption=" + String2.annotatedString(distinctOptions[whichDTCol][2]) +
                //         " match=" + tWhich);
                writer.write(
                    widgets.select("dis" + dv, "",
                        1, distinctOptions[whichDTCol], Math.max(0, tWhich), 
                        "onChange='" +
                          //"if (this.selectedIndex>0) {" +
                          " " + docFormName + ".op"  + dv + "_0.selectedIndex=" + String2.indexOf(OPERATORS, "=") + ";" +
                          " " + docFormName + ".val" + dv + "_0.value=this.options[this.selectedIndex].text;" +
                          //" this.selectedIndex=0;}" + //last
                          "'", 
                        true) + //encodeSpaces solves the problem with consecutive internal spaces
                    "\n" +
                    "  </td>\n");

                //+- buttons
                writer.write(
                    "  <td nowrap>" +
                    "<img src=\"" + widgets.imageDirUrl + "minus.gif\"\n" +
                    "  " + widgets.completeTooltip(EDStatic.selectPrevious) +
                    "  alt=\"-\" " + 
                    //onMouseUp works much better than onClick and onDblClick
                    "  onMouseUp=\"\n" +
                    "    var dis=" + docFormName + ".dis" + dv + ";\n;" +
                    "    if (dis.selectedIndex>0) {" +
                    "      dis.selectedIndex--;\n" +
                    "      " + docFormName + ".op"  + dv + "_0.selectedIndex=" + 
                           String2.indexOf(OPERATORS, "=") + ";" +
                    "      " + docFormName + ".val" + dv + "_0.value=" + 
                           "dis.options[dis.selectedIndex].text;" +
                    "    }\" >\n" + 
                    "  </td>\n" +
                    "  <td nowrap>" +
                    "<img src=\"" + widgets.imageDirUrl + "plus.gif\"\n" +
                    "  " + widgets.completeTooltip(EDStatic.selectNext) +
                    "  alt=\"+\" " + 
                    //onMouseUp works much better than onClick and onDblClick
                    "  onMouseUp=\"\n" +
                    "    var dis=" + docFormName + ".dis" + dv + ";\n;" +
                    "    if (dis.selectedIndex<" + (distinctOptions[whichDTCol].length - 1) + ") {" +
                    "      dis.selectedIndex++;\n" +
                    "      " + docFormName + ".op"  + dv + "_0.selectedIndex=" + 
                           String2.indexOf(OPERATORS, "=") + ";" +
                    "      " + docFormName + ".val" + dv + "_0.value=" + 
                           "dis.options[dis.selectedIndex].text;" +
                    "    }\" >\n" + 
                    "  </td>\n" +
                    "  <td nowrap>&nbsp;" +
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableSelectConstraint) + 
                    "  </tr>\n" +
                    "  </table>\n" +
                    "</td>\n");

            } else {
                //show min and max
                String mins = edv.destinationMinString();
                String maxs = edv.destinationMaxString();
                writer.write(
                    "  <td nowrap>" + gap + mins + "</td>\n" +
                    "  <td nowrap>" + gap + maxs + "</td>\n");

//            } else {
//                //empty cell
//                writer.write("  <td nowrap>&nbsp;</td>\n");
            }

            //end of row
            writer.write("</tr>\n");


            // *** and a slider for this dataVariable    (Data Access Form)
            if ((dv != lonIndex && dv != latIndex && dv != altIndex && dv != depthIndex && !isTime) ||
                !Math2.isFinite(edv.destinationMin()) ||
                (!Math2.isFinite(edv.destinationMax()) && !isTime)) {

                //no slider
                sliderNThumbs[dv] = 0;
                sliderFromNames[dv] = "";
                sliderToNames[  dv] = "";
                sliderUserValuesCsvs[dv] = "";
                sliderInitFromPositions[dv] = 0;
                sliderInitToPositions[dv]   = 0;

            } else {
                //slider
                sliderNThumbs[dv] = 2;
                sliderFromNames[dv] = formName + "." + valueWidgetName + "0";
                sliderToNames[  dv] = formName + "." + valueWidgetName + "1";
                sliderUserValuesCsvs[dv] = edv.sliderCsvValues();
                sliderInitFromPositions[dv] = edv.closestSliderPosition(tValD[0]);
                sliderInitToPositions[dv]   = edv.closestSliderPosition(tValD[1]);
                if (sliderInitFromPositions[dv] == -1) sliderInitFromPositions[dv] = 0;
                if (sliderInitToPositions[  dv] == -1) sliderInitToPositions[  dv] = EDV.SLIDER_PIXELS - 1;
                writer.write(
                    "<tr align=\"left\">\n" +
                    "  <td nowrap colspan=\"5\" align=\"left\">\n" +
                    widgets.spacer(10, 1, "align=\"left\"") +
                    widgets.dualSlider(dv, EDV.SLIDER_PIXELS - 1, "align=\"left\"") +
                    "  </td>\n" +
                    "</tr>\n");
            }

        }

        //end of table
        writer.write(widgets.endTable());

        //functions
        String queryParts[] = getUserQueryParts(userDapQuery); //decoded.  always at least 1 part (may be "")
        int nOrderBy = 5;
        writeFunctionHtml(loggedInAs, queryParts, writer, widgets, nOrderBy);

        //fileType
        writer.write("<p><b>" + EDStatic.EDDFileType + "</b>\n");
        writer.write(widgets.select("fileType", EDStatic.EDDSelectFileType, 1,
            allFileTypeOptions, defaultFileTypeOption, ""));
        writer.write(" <a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html#fileType\">more&nbsp;info</a>\n");


        //generate the javaScript
        String javaScript = 
            "var result = \"\";\n" +
            "try {\n" +
            "  var d = document;\n" +
            "  var q1 = \"\";\n" +
            "  var q2 = \"\";\n" +  //&constraints
            "  var active = {};\n" + //needed for functionJavaScript below
            "  for (var dv = 0; dv < " + nDv + "; dv++) {\n" +
            "    var tVar = eval(\"d." + formName + ".varch\" + dv);\n" +
            "    if (tVar.checked) {\n" +
            "      q1 += (q1.length==0? \"\" : \",\") + tVar.value;\n" +
            "      active[tVar.value] = 1;\n" +
            "    }\n" +
            "    var tOp  = eval(\"d." + formName + ".op\"  + dv + \"_0\");\n" +
            "    var tVal = eval(\"d." + formName + ".val\" + dv + \"_0\");\n" +
            "    if (tVal.value.length > 0) q2 += \"\\x26\" + tVar.value + tOp.options[tOp.selectedIndex].text + percentEncode(tVal.value);\n" +
            "    tOp  = eval(\"d." + formName + ".op\"  + dv + \"_1\");\n" +
            "    tVal = eval(\"d." + formName + ".val\" + dv + \"_1\");\n" +
            "    if (tVal.value.length > 0) q2 += \"\\x26\" + tVar.value + tOp.options[tOp.selectedIndex].text + percentEncode(tVal.value);\n" +
            "  }\n" +
            functionJavaScript(formName, nOrderBy) +
            "  var ft = d." + formName + ".fileType.options[d." + formName + ".fileType.selectedIndex].text;\n" +
            "  result = \"" + tErddapUrl + "/tabledap/" + datasetID + 
              "\" + ft.substring(0, ft.indexOf(\" - \")) + \"?\" + q1 + q2;\n" + //if all is well, make result
            "} catch (e) {alert(e);}\n" +  //result is in 'result'  (or "" if error)
            "d." + formName + ".tUrl.value = result;\n";

        //just generate URL
        writer.write("<br>"); 
        String genViewHtml = String2.replaceAll(EDStatic.justGenerateAndViewHtml, "&protocolName;", dapProtocol);
        writer.write(widgets.button("button", "getUrl", genViewHtml,
            EDStatic.justGenerateAndView, 
            //"class=\"skinny\" " + //only IE needs it but only IE ignores it
            "onclick='" + javaScript + "'"));
        writer.write(widgets.textField("tUrl", 
            EDStatic.justGenerateAndViewUrl,
            70, 1000, "", ""));
        writer.write("<a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html\" " +
            "title=\"tabledap documentation\">Documentation&nbsp;/&nbsp;Bypass&nbsp;this&nbsp;form</a>\n" +
            EDStatic.htmlTooltipImage(loggedInAs, genViewHtml));


        //submit
        writer.write(
            "<br>&nbsp;\n" +
            "<br>" +
            widgets.htmlButton("button", "submit1", 
                "", EDStatic.submitTooltip, "<big><b>" + EDStatic.submit + "</b></big>", 
                "onclick='" + javaScript +
                "if (result.length > 0) window.location=result;\n" + //or open a new window: window.open(result);\n" +
                "'") +
            " " + EDStatic.patientData + "\n");

        //end of form
        writer.write(widgets.endForm());
        writer.write("<br>&nbsp;\n");

        //the javascript for the sliders
        writer.write(widgets.sliderScript(sliderFromNames, sliderToNames, 
            sliderNThumbs, sliderUserValuesCsvs, 
            sliderInitFromPositions, sliderInitToPositions, EDV.SLIDER_PIXELS - 1));

        //be nice
        writer.flush(); 

    }


    /** This writes the Function options for the Make A Graph and Data Access Form web pages. */
    void writeFunctionHtml(String loggedInAs, String[] queryParts, Writer writer, HtmlWidgets widgets, 
        int nOrderBy) throws Throwable {

        String functionHtml = EDStatic.functionHtml;

        writer.write("\n&nbsp;\n"); //necessary for the blank line before the table (not <p>)
        writer.write(widgets.beginTable(0, 0, "")); 
        writer.write("<tr><td><b>" + EDStatic.functions + "</b> " + 
            EDStatic.htmlTooltipImage(loggedInAs, functionHtml) +
            "</td></tr>\n");

        //distinct function
        writer.write("<tr><td>");
        writer.write( 
            widgets.checkbox("distinct", 
                EDStatic.functionDistinctCheck,
                String2.indexOf(queryParts, "distinct()") >= 0, 
                "true", "distinct()", ""));
        writer.write(EDStatic.htmlTooltipImage(loggedInAs, EDStatic.functionDistinctHtml));
        writer.write("</td></tr>\n");

        //orderBy... function
        StringArray dvNames0 = new StringArray(dataVariableDestinationNames);
        dvNames0.add(0, "");
        String dvList0[] = dvNames0.toArray();
        String important[]= nOrderBy == 4?
            //"most", "second most", "third most", ["fourth most",] "least"};
            new String[]{EDStatic.functionOrderBySort1, 
                         EDStatic.functionOrderBySort2, 
                         EDStatic.functionOrderBySort3, 
                         EDStatic.functionOrderBySortLeast} :
            new String[]{EDStatic.functionOrderBySort1, 
                         EDStatic.functionOrderBySort2, 
                         EDStatic.functionOrderBySort3, 
                         EDStatic.functionOrderBySort4, 
                         EDStatic.functionOrderBySortLeast};
        //find first part that uses orderBy...
        String obPart = null;
        int whichOb = -1;
        for (int ob = 0; ob < orderByOptions.length; ob++) {
            obPart = String2.stringStartsWith(queryParts, orderByOptions[ob] + "(\"");
            if (obPart != null) {
                if (obPart.endsWith("\")")) {
                    whichOb = ob;
                    break;
                } else {
                    obPart = null;
                }
            }
        }        
        String orderBy[] = obPart == null? new String[0] :
            String2.split(obPart.substring(obPart.length() + 2, obPart.length() - 2), ',');
        writer.write("\n<tr><td>"); //was <td nowrap>, but allow wrapping if varNames are long
        writer.write(widgets.select("orderBy", "", 1, orderByOptions, whichOb, ""));
        writer.write(EDStatic.htmlTooltipImage(loggedInAs, EDStatic.functionOrderByHtml));
        writer.write("(\"\n");       
        for (int ob = 0; ob < nOrderBy; ob++) {
            //if (ob > 0) writer.write(",\n"); 
            writer.write(widgets.select("orderBy" + ob,                 
                MessageFormat.format(EDStatic.functionOrderBySort, important[ob]), // was + \n
                1, dvList0, 
                Math.max(0, dvNames0.indexOf(ob < orderBy.length? orderBy[ob] : "")),
                ""));
        }
        writer.write("\")\n");       
        writer.write("</td></tr>\n");

        writer.write(widgets.endTable());
    }

    /** This writes the JavaScript for the Function options for the Make A Graph 
     * and Data Access Form web pages. 
     * q1 holds the csv list of results variables.
     * q2 holds the &amp; queries.
     */
    String functionJavaScript(String formName, int nOrderBy) throws Throwable {

        return
            "\n" +
            "    if (d." + formName + ".distinct.checked) q2 += \"\\x26distinct()\";\n" +
            "\n" +
            "    var nActiveOb = 0;\n" +
            "    var ObOS = d." + formName + ".orderBy;\n" +  //the OrderBy... option Select widget
            "    var ObO = ObOS.options[ObOS.selectedIndex].text;\n" +
            "    for (var ob = 0; ob < " + nOrderBy + "; ob++) {\n" +
            "      var tOb = eval(\"d." + formName + ".orderBy\" + ob);\n" +
            "      var obVar = tOb.options[tOb.selectedIndex].text;\n" +
            "      if (obVar != \"\") {\n" +
            "        q2 += (nActiveOb++ == 0? \"\\x26\" + ObO + \"(%22\" : \",\") + obVar;\n" +
            "        if (active[obVar] === undefined) {\n" +
            "          q1 += (q1.length==0? \"\" : \",\") + obVar;\n" +
            "          active[obVar] = 1;\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "    if (nActiveOb > 0) q2 += \"%22)\";\n" +
            "\n";
    }


    /**
     * This write HTML info on forming OPeNDAP DAP-style requests for this type of dataset.
     *
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @param writer to which will be written HTML info on forming OPeNDAP 
     *    DAP-style requests for this type of dataset.
     * @param complete if false, this just writes a paragraph and shows a link
     *    to [protocol]/documentation.html
     * @throws Throwable if trouble
     */
    public static void writeGeneralDapHtmlInstructions(String tErddapUrl,
        Writer writer, boolean complete) throws Throwable {

        String dapBase = tErddapUrl + "/" + dapProtocol + "/";
        String datasetBase = dapBase + EDStatic.EDDTableIdExample;
        //all of the fullXxx examples are NOT encoded
        String fullDdsExample      = datasetBase + ".dds";
        String fullValueExample    = datasetBase + ".htmlTable?" + EDStatic.EDDTableDataValueExample;
        String fullTimeExample     = datasetBase + ".htmlTable?" + EDStatic.EDDTableDataTimeExample;
        String fullTimeCsvExample  = datasetBase + ".csv?"       + EDStatic.EDDTableDataTimeExample;
        String fullTimeMatExample  = datasetBase + ".mat?"       + EDStatic.EDDTableGraphExample;
        String fullGraphExample    = datasetBase + ".png?"       + EDStatic.EDDTableGraphExample;
        String fullGraphMAGExample = datasetBase + ".graph?"     + EDStatic.EDDTableGraphExample;
        String fullGraphDataExample= datasetBase + ".htmlTable?" + EDStatic.EDDTableGraphExample;
        String fullMapExample      = datasetBase + ".png?"       + EDStatic.EDDTableMapExample;
        String fullMapMAGExample   = datasetBase + ".graph?"     + EDStatic.EDDTableMapExample;
        String fullMapDataExample  = datasetBase + ".htmlTable?" + EDStatic.EDDTableMapExample;

        GregorianCalendar daysAgo7Gc = Calendar2.newGCalendarZulu();
        daysAgo7Gc.add(Calendar2.DATE, -7);
        String daysAgo7 = Calendar2.formatAsISODate(daysAgo7Gc); 

        writer.write(  
            "<h2><a name=\"instructions\">Using</a> tabledap to Request Data and Graphs from Tabular Datasets</h2>\n" +
            longDapDescription(tErddapUrl) +
            "<p><b>Tabledap request URLs must be in the form</b>\n" +
            "<br>&nbsp;&nbsp;&nbsp;<tt>" + dapBase + 
                "<i><a rel=\"help\" href=\"" + dapBase + "documentation.html#datasetID\">datasetID</a></i>." + 
                "<i><a rel=\"help\" href=\"" + dapBase + "documentation.html#fileType\">fileType</a></i>{?" + 
                "<i><a rel=\"help\" href=\"" + dapBase + "documentation.html#query\">query</a></i>}</tt>\n" +
            "<br>For example,\n" +
            "<br>&nbsp;&nbsp;&nbsp;<a href=\"" + 
                EDStatic.phEncode(fullTimeExample) + "\"><tt>" + 
                 XML.encodeAsHTML(fullTimeExample) + "</tt></a>\n" +
            "<br>Thus, the query is often a comma-separated list of desired variable names, followed by a collection of\n" +
            "<br>constraints (e.g., <tt><i>variable</i>&lt;<i>value</i></tt>),\n" +
            "  each preceded by '&amp;' (which is interpreted as \"AND\").\n" +
            "\n");

        if (!complete) {
            writer.write(
            "<p>For details, see the <a rel=\"help\" href=\"" + dapBase + 
                "documentation.html\">" + dapProtocol + " Documentation</a>.\n");
            return;
        }
         
        //details
        writer.write(
            "<p><b>Details:</b>\n" +
            "<ul>\n" +
            "<li>Requests must not have any internal spaces.\n"+
            "<li>Requests are case sensitive.\n" +
            "<li>{} is notation to denote an optional part of the request.\n" + 
            "  <br>&nbsp;\n" +

            //datasetID
            "<li><a name=\"datasetID\"><b>datasetID</b></a> identifies the name that ERDDAP\n" +
            "  assigned to the source web site and dataset\n" +
            "  <br>(for example, <tt>" + 
                XML.encodeAsHTML(EDStatic.EDDTableIdExample) + "</tt>). You can see a list of\n" +
            "    <a rel=\"bookmark\" href=\"" + 
                EDStatic.phEncode(tErddapUrl + "/" + dapProtocol) + 
                "/index.html\">datasetID options available via tabledap</a>.\n" +
            "  <br>&nbsp;\n" +

            //fileType
            "<li><a name=\"fileType\"><b>fileType</b></a> specifies the type of table data file that you " +
            "  want to download (for example, <tt>.htmlTable</tt>).\n" +
            "  <br>The actual extension of the resulting file may be slightly different than the fileType (for example,\n" +
            "  <br><tt>.smallPdf</tt> returns a small .pdf file).\n" +
            "  <br>The fileType options for downloading tabular data are:\n" +
            "  <br>&nbsp;\n" +
            "  <table class=\"erd\" cellspacing=\"0\">\n" + 
            "    <tr><th>Data<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
        for (int i = 0; i < dataFileTypeNames.length; i++) 
            writer.write(
                "    <tr>\n" +
                "      <td>" + dataFileTypeNames[i] + "</td>\n" +
                "      <td>" + dataFileTypeDescriptions[i] + "</td>\n" +
                "      <td>" + (dataFileTypeInfo[i].equals("")? 
                    "&nbsp;" : 
                    "<a rel=\"help\" href=\"" + XML.encodeAsHTMLAttribute(dataFileTypeInfo[i]) + "\">info</a>") + 
                "</td>\n" +
                "      <td><a href=\"" +  datasetBase + dataFileTypeNames[i] + "?" + 
                    EDStatic.phEncode(EDStatic.EDDTableDataTimeExample) + "\">example</a></td>\n" +
                "    </tr>\n");
        writer.write(
            "   </table>\n" +
            "   <br>For example, here is a complete request URL to download data formatted as an HTML table:\n" +
            "   <br>&nbsp;&nbsp;<a href=\"" + EDStatic.phEncode(fullTimeExample) + "\"><tt>" + 
                                              XML.encodeAsHTML( fullTimeExample) + "</tt></a>\n" +
            "\n" +

            //ArcGIS
            "<p><b><a rel=\"bookmark\" href=\"http://www.esri.com/software/arcgis/index.html\">ArcGIS</a>\n" +
            "  <a rel=\"help\" href=\"http://resources.arcgis.com/content/kbase?fa=articleShow&amp;d=27589\">.esriCsv</a></b>\n" +
            "  - <a name=\"ArcGIS\">ArcGIS</a> is a family of Geographical Information Systems (GIS) products from ESRI:\n" +
            "  <br>ArcView, ArcEditor, and ArcInfo.  To get data from ERDDAP into your ArcGIS program (version 9.x and below):\n" +
            "  <ol>\n" +
            "  <li>In ERDDAP, save some data (which must include longitude and latitude) in an .esriCsv file\n" +
            "    <br>(which will have the extension .csv) in the directory where you usually store ArcGIS data files.\n" +
            "    <br>In the file:\n" +
            "    <ul>\n" +
            "    <li> Column names have been changed to be 10 characters or less\n" +
            "     <br>(sometimes with A, B, C, ... at the end to make them unique).\n" +
            "    <li> <tt>longitude</tt> is renamed <tt>X</tt>. <tt>latitude</tt> is renamed <tt>Y</tt> to make them the default\n" +
            "     <br>coordinate fields.\n" +
            "    <li> Missing numeric values are written as -9999.\n" +
            "    <li> Double quotes in strings are double quoted.\n" +
            "    <li> Timestamp columns are separated into date (ISO 8601) and time (am/pm) columns.\n" +
            "    </ul>\n" +
            "  <li>In ArcMap, navigate to <tt>Tools : Add XY Data</tt> and select the file.\n" +
            "  <li>Click on <tt>Edit</tt> to open the Spatial Reference Properties dialog box.\n" +
            "  <li>Click on <tt>Select</tt> to select a coordinate system.\n" +
            "  <li>Open the <tt>World</tt> folder and select <tt>WGS_1984.prj</tt>.\n" +
            "  <li>Click a <tt>Add, Apply</tt> and <tt>OK</tt> on the Spatial Reference Properties dialog box.\n" +
            "  <li>Click <tt>OK</tt> on the Add XY Data dialog box.\n" +
            "  <li>Optional: save the data as a shapefile or into a geodatabase by right clicking\n" +
            "    <br>on the data set and choosing <tt>Data : Export Data</tt> ...\n" +
            "  </ol>\n" +
            "  <br>The points will be drawn as an Event theme in ArcMap.\n" +
            "  <br>(These instructions are a modified version of\n" +
            "    <a rel=\"help\" href=\"http://resources.arcgis.com/content/kbase?fa=articleShow&amp;d=27589\">ESRI's instructions</a>.)\n" +
            "\n" +
            //Ferret
            "  <p><b><a rel=\"bookmark\" href=\"http://www.ferret.noaa.gov/Ferret/\">Ferret</a></b> \n" +
            "    <a name=\"Ferret\">is</a> a free program for visualizing and analyzing large and complex\n" +
            "  <br><b>gridded</b> datasets.  Because tabledap's <b>tabular</b> datasets are very different\n" +
            "  <br>from gridded datasets, it is necessary to use Ferret in a very specific way to\n" +
            "  <br>avoid serious problems and misunderstandings:<ol>\n" +
            "    <li>Use the ERDDAP web pages or a program like\n" +
            "      <a rel=\"help\" href=\"#curl\">curl</a> to download a subset of a\n" +
            "      <br>dataset in a plain .nc file.\n" +
            "    <li>Open that local file (which has the data in a gridded format) in Ferret.  The data\n" +
            "      <br>isn't in an ideal format for Ferret, but you should be able to get it to work.\n" +
            "    </ol>\n" +
            "  <br>WARNING: Ferret won't like profile data (and perhaps other data), because the time\n" +
            "  <br>variable will have the same time value for all of the rows of data for a given\n" +
            "  <br>profile.  In fact, Ferret will add some number of seconds to the time values\n" +
            "  <br>to make them all unique! So treat these time values accordingly.\n" +
            "\n" +
            //IDL
            "  <p><b><a rel=\"bookmark\" href=\"http://www.ittvis.com/language/en-us/productsservices/idl.aspx/\">IDL</a></b> - \n" +
            "    <a name=\"IDL\">IDL</a> is a commercial scientific data visualization program. To get data from\n" +
            "  <br>ERDDAP into IDL, first use ERDDAP to select a subset of data and download a .nc file.\n" +
            "  <br>Then, use these\n" +
            "    <a rel=\"help\" href=\"http://www.atmos.umd.edu/~gcm/usefuldocs/hdf_netcdf/IDL_hdf-netcdf.html\">instructions</a>\n" +
            "    to import the data from the .nc file into IDL.\n" +
            "\n" +
            //json
            "  <p><b><a rel=\"help\" href=\"http://www.json.org/\">JSON .json</a></b>\n" +
            "    <a name=\"json\">files</a> are widely used to transfer data to JavaScript scripts running on web pages.\n" +
            "  <br>Tabledap will format the data in a table-like structure in the .json file.\n" +
            "\n" +
            //jsonp
            "  <p><b><a rel=\"help\" href=\"http://niryariv.wordpress.com/2009/05/05/jsonp-quickly/\">JSONP</a>\n" +
            "    (from <a href=\"http://www.json.org/\">.json</a> and\n" +
            "    <a rel=\"help\" href=\"http://wiki.geojson.org/Main_Page\">.geoJson</a>)</b> -\n" +
            "  <a name=\"jsonp\">Jsonp</a> is an easy way for a JavaScript script on a web\n" +
            "  <br>page to import and access data from ERDDAP.  Requests for .json and .geoJson files may\n" +
            "  <br>include an optional jsonp request by adding <tt>&amp;.jsonp=<i>functionName</i></tt> to the end of the query.\n" +
            "  <br>Basically, this just tells ERDDAP to add <tt><i>functionName</i>(</tt> to the beginning of the response\n" +
            "  <br>and <tt>\")\"</tt> to the end of the response.\n" +
            "  <br>The functionName must be a series of 1 or more (period-separated) words.\n" +
            "  <br>For each word, the first character of <i>functionName</i> must be an ISO 8859 letter or \"_\".\n" +
            "  <br>Each optional subsequent character must be an ISO 8859 letter, \"_\", or a digit.\n" +
            "  <br>If originally there was no query, leave off the \"&amp;\" in your query.\n" +
            "  <br>After the data download to the web page has finished, the data is accessible to the JavaScript\n" +
            "  <br>script via that JavaScript function.\n" +
            "\n" +
            //matlab
            "  <p><b><a rel=\"bookmark\" href=\"http://www.mathworks.com/products/matlab/\">MATLAB</a>\n" +
            "     <a rel=\"help\" href=\"http://www.serc.iisc.ernet.in/ComputingFacilities/software/matfile_format.pdf\">.mat</a></b>\n" +
            "   - <a name=\"matlab\">Matlab</a> users can use griddap's .mat file type to download data from within\n" +
            "  <br>MATLAB.  Here is a one line example:<pre>\n" +
                 "load(urlwrite('" + XML.encodeAsHTML(fullTimeMatExample) + "', 'test.mat'));</pre>\n" +
            "  The data will be in a MATLAB structure. The structure's name will be the datasetID\n" +
            "  <br>(for example, <tt>" + XML.encodeAsHTML(EDStatic.EDDTableIdExample) + "</tt>). \n" +
            "  <br>The structure's internal variables will be column vectors with the same names\n" +
            "  <br>as in ERDDAP \n" +
            "    (for example, use <tt>fieldnames(" + 
                XML.encodeAsHTML(EDStatic.EDDTableIdExample) + ")</tt>). \n" +
            "  <br>You can then make a scatterplot of any two columns. For example:<pre>\n" +
                XML.encodeAsHTML(EDStatic.EDDTableMatlabPlotExample) + "</pre>\n" +
            "  <p>ERDDAP stores datetime values in .mat files as \"seconds since 1970-01-01T00:00:00Z\".\n" +
            "  <br>To display one of these values as a String in Matlab, you can use, e.g.,\n" +
            "  <br><tt>datastr(cwwcNDBCMet.time(1)/86400 + 719529)</tt>\n" +
            "  <br>86400 converts ERDDAP's \"seconds since\" to Matlab's \"days since\". 719529 converts\n" +
            "  <br>ERDDAP's base time of \"1970-01-01T00:00:00Z\" to Matlab's \"0000-01-00T00:00:00Z\".\n" +
            "\n" +
            //nc
            "  <p><b><a rel=\"bookmark\" href=\"http://www.unidata.ucar.edu/software/netcdf/\">NetCDF</a>\n" +
            "    <a rel=\"help\" href=\"http://www.unidata.ucar.edu/software/netcdf/docs/netcdf/File-Format-Specification.html\">.nc</a></b>\n" +
            "     - <a name=\"nc\">Requests</a> for .nc files will always return the data in a table-like, version 3,\n" +
            "  <br>32-bit, .nc file:\n" +
            "  <ul>\n" +
            "  <li>All variables will use the file's \"row\" dimension.\n" +
            "  <li>All String variables will also have a dimension indicating the maximum number of\n" +
            "    <br>characters for that variable.\n" +
            "  </ul>\n" +
            "  <p>Don't use NetCDF-Java, NetCDF-C, NetCDF-Fortran, NetCDF-Perl, or Ferret to try to\n" +
            "  <br>access a remote ERDDAP .nc file.  It won't work.  Instead, use\n" +
            "  <a href=\"#netcdfjava\">this approach</a>.\n" +
            "\n" +
            //ncHeader
            "  <p><b>.ncHeader</b>\n" +
            "    - <a name=\"ncHeader\">Requests</a> for .ncHeader files will return the header information (text) that\n" +
            "  <br>would be generated if you used\n" +
            "    <a rel=\"help\" href=\"http://www.unidata.ucar.edu/software/netcdf/docs/guide_ncdump.html\">ncdump -h <i>fileName</i></a>\n" +
            "    on the corresponding .nc file.\n" +
            "\n" +
            //ncCF
            "  <p><a name=\"ncCF\"><b>.ncCF</b></a>\n" +
            "     - Requests for a .ncCF file will return a version 3, 32-bit,\n" +
            "  <a rel=\"bookmark\" href=\"http://www.unidata.ucar.edu/software/netcdf/\">NetCDF .nc</a>\n" +
            "  file with the\n" +
            "  <br>Contiguous Ragged Array Representation associated with the dataset's cdm_data_type,\n" +
            "  <br>as defined in the newly ratified\n" +
            "    <a rel=\"help\" href=\"http://cf-pcmdi.llnl.gov/documents/cf-conventions\">CF</a>\n" +
            "    <a rel=\"help\" href=\"http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries\">Discrete Geometries</a> conventions\n" +
            "  <br>(which were previously named \"CF Point Observation Conventions\").\n" +
            "  <ul>\n" +
            "  <li>Point - Appendix H.1 Point Data\n" +
            "  <li>TimeSeries - Appendix H.2.4 Contiguous ragged array representation of timeSeries\n" +
            "  <li>Profile - Appendix H.3.4 Contiguous ragged array representation of profiles\n" +
            "  <li>Trajectory - Appendix H.4.3 Contiguous ragged array representation of trajectories\n" +
            "  <li>TimeSeriesProfile - Appendix H.5.3 Ragged array representation of timeSeriesProfiles\n" +
            "  <li>TrajectoryProfile - Appendix H.6.3 Ragged array representation of trajectoryProfiles\n" +
            "  </ul>\n" +
            "  <br>A request will succeed only if the dataset has a cdm_data_type other than \"Other\"\n" +
            "  <br>and if the request includes at least one data variable (not just the outer, descriptive variables).\n" +
            "  <br>The file will include longitude, latitude, time, and other required descriptive variables, even if\n" +
            "  <br>you don't request them.\n" +
            "  <br>Since these are the newly ratified file formats, one can hope that they won't change again.\n" +
            "\n" +  
            //ncCFHeader
            "  <p><b>.ncCFHeader</b>\n" +
            "    - <a name=\"ncCFHeader\">Requests</a> for .ncCFHeader files will return the header information (text) that\n" +
            "  <br>would be generated if you used\n" +
            "    <a rel=\"help\" href=\"http://www.unidata.ucar.edu/software/netcdf/docs/guide_ncdump.html\">ncdump -h <i>fileName</i></a>\n" +
            "    on the corresponding .ncCF file.\n" +
            "\n" +
            //ncCFMA
            "  <p><a name=\"ncCFMA\"><b>.ncCFMA</b></a>\n" +
            "     - Requests for a .ncCFMA file will return a verion 3, 32-bit,\n" +
            "    <a rel=\"bookmark\" href=\"http://www.unidata.ucar.edu/software/netcdf/\">NetCDF .nc</a> file\n" +
            "   <br>with the Complete or Incomplete, depending on the data, Multidimensional Array Representation\n" +
            "   <br>associated with the dataset's cdm_data_type, as defined in the\n" +
            "     <a rel=\"help\" href=\"http://cf-pcmdi.llnl.gov/documents/cf-conventions\">CF</a>\n" +
            "     <a rel=\"help\" href=\"http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries\">Discrete Sampling Geometries</a>\n" +
            "   <br>conventions, which were previously named \"CF Point Observation Conventions\".\n" +
            "   <br>This is the file type used by the <a rel=\"help\" href=\"http://www.nodc.noaa.gov/data/formats/netcdf/\">NODC Templates</a>.\n" +
            "   <br>A request will succeed only if the dataset has a cdm_data_type other than \"Other\"\n" +
            "   <br>and if the request includes at least one data variable (not just the outer, descriptive variables).\n" +
            "   <br>The file will include longitude, latitude, time, and other required descriptive variables, even if\n" +
            "   <br>you don't request them.\n" +
            "\n" +  
            //ncCFMAHeader
            "  <p><b>.ncCFMAHeader</b>\n" +
            "    - <a name=\"ncCFMAHeader\">Requests</a> for .ncCFMAHeader files will return the header information (text) that\n" +
            "  <br>would be generated if you used\n" +
            "    <a rel=\"help\" href=\"http://www.unidata.ucar.edu/software/netcdf/docs/guide_ncdump.html\">ncdump -h <i>fileName</i></a>\n" +
            "    on the corresponding .ncCFMA file.\n" +
            "\n" +
            //netcdfjava
            "  <p><b><a rel=\"bookmark\" href=\"http://www.unidata.ucar.edu/software/netcdf/\">NetCDF-Java, NetCDF-C, NetCDF-Fortran, and NetCDF-Perl</a></b>\n" +
            "  <a name=\"netcdfjava\">-</a>\n" +
            "  <br>Don't try to access an ERDDAP tabledap dataset URL directly with a NetCDF library or tool\n" +
            "  <br>(by treating the tabledap dataset as an OPeNDAP dataset or by creating a URL with the .nc file extension).\n" +
            "  <br>It won't work.\n" +
            "    <ol>\n" +
            "    <li>These libraries and tools just work with OPeNDAP gridded data.\n" +
            "      <br>Tabledap offers data as OPeNDAP sequence data, which is a different data structure.\n" +
            "    <li>These libraries and tools just work with actual, static, .nc files.\n" +
            "      <br>But an ERDDAP URL with the .nc file extension is a virtual file. It doesn't actually exist.\n" +
            "    </ol>\n" +
            "  <br>Fortunately, there is a two step process that does work:\n" +
            "    <ol>\n" +
            "    <li>Use the ERDDAP web pages or a program like\n" +
            "      <a rel=\"help\" href=\"#curl\">curl</a> to download a .nc file with a subset of the dataset.\n" +
            "    <li>Open that local .nc file (which has the data in a grid format) with one of the NetCDF libraries or tools.\n" +
            "      <br>With NetCDF-Java for example, use:\n" +
            "      <br><tt>NetcdfFile nc = NetcdfFile.open(\"c:\\downloads\\theDownloadedFile.nc\");</tt>\n" +
            "      <br>or\n" +
            "      <br><tt>NetcdfDataset nc = NetcdfDataset.openDataset(\"c:\\downloads\\theDownloadedFile.nc\");</tt>\n" +
            "      <br>(NetcdfFiles are a lower level approach than NetcdfDatasets.  It is your choice.)\n" +
            "      <br>In both cases, you can then do what you want with the <tt>nc</tt> object, for example,\n" +
            "      <br>request metadata or request a subset of a variable's data as you would with any other\n" +
            "      <br>.nc file.\n" +
            "    </ol>\n" +
            "\n" +
            //odv
            "  <p><b><a rel=\"bookmark\" href=\"http://odv.awi.de/\">Ocean Data View</a> .odvTxt</b>\n" +
            "   - <a name=\"ODV\">ODV</a> users can download data in a\n" +
            "    <a rel=\"help\" href=\"http://odv.awi.de/en/documentation/\">ODV Generic Spreadsheet Format .txt file</a>\n" +
            "  <br>by requesting tabledap's .odvTxt fileType.\n" +
            "  <br>The selected data MUST include longitude, latitude, and time variables.\n" +
            "  <br>Any longitude values (0 to 360, or -180 to 180) are fine.\n" +
            "  <br>After saving the resulting file (with the extension .txt) in your computer:\n" +
            "  <ol>\n" +
            "  <li>Open ODV.\n" +
            "  <li>Use <tt>File : Open</tt>.\n" +
            "    <ul>\n" +
            "    <li>Change <tt>Files of type</tt> to <tt>Data Files (*.txt *.csv *.jos *.o4x)</tt>.\n" + 
            "    <li>Browse to select the .txt file you created in ERDDAP and click on <tt>Open</tt>.\n" +
            "      <br>The data should now be visible on a map in ODV.\n" +
            "    </ul>\n" +
            "  <li>See ODV's <tt>Help</tt> menu for more help using ODV.\n" +
            "  </ol>\n" +
            "\n" +
            //opendapLibraries
            "  <p><b><a name=\"opendapLibraries\">OPeNDAP Libraries</a></b> - Although ERDDAP is an\n" +
            "    <a rel=\"bookmark\" href=\"http://www.opendap.org/\">OPeNDAP</a>-compatible data server,\n" +
            "    you can't use\n" +
            "  <br>most OPeNDAP client libraries, including\n" +
            "  <a rel=\"bookmark\" href=\"http://www.unidata.ucar.edu/software/netcdf/\">NetCDF-Java, NetCDF-C, NetCDF-Fortran, NetCDF-Perl</a>,\n" +
            "  <br>or\n" +
            "  <a rel=\"bookmark\" href=\"http://www.ferret.noaa.gov/Ferret/\">Ferret</a>,\n" +
            "  to get data directly from an ERDDAP tabledap dataset because those libraries don't\n" +
            "  <br>support the OPeNDAP Selection constraints that tabledap datasets use for requesting\n" +
            "  <br>subsets of the dataset, nor do they support the sequence data structure in the respons.\n" +
            "  <br>(But see this <a href=\"#netcdfjava\">other approach</a> that works with NetCDF libraries.)\n" + 
            "  <br>But you can use the <a href=\"#PydapClient\">Pydap Client</a> or\n" +
            "  <a rel=\"bookmark\" href=\"http://www.opendap.org/java-DAP\">Java-DAP2</a>,\n" +
            "  because they both support Selection\n" +
            "  <br>constraints.  With both the Pydap Client and Java-DAP2, when creating the initial\n" +
            "  <br>connection to an ERDDAP table dataset, use the tabledap dataset's base URL, e.g.,\n" +
            "  <br><tt>" + datasetBase + "</tt>\n" +
            "  <ul>\n" +
            "  <li>Don't include a file extension (e.g., .nc) at the end of the dataset's name.\n" +
            "  <li>Don't specify a variable or a Selection-constraint.\n" +
            "  </ul>\n" +
            "  <br>Once you have made the connection to the dataset, you can request metadata or a\n" +
            "  <br>subset of a variable's data via a Selection-constraint.\n" +
            "\n" +
            //Pydap Client
            "  <p><b><a rel=\"bookmark\" href=\"http://pydap.org/client.html\">Pydap Client</a></b>\n" +
            "    <a name=\"PydapClient\">users</a>\n" +
            "    can access tabledap datasets via ERDDAP's standard OPeNDAP services.\n" +
            "  <br>See the\n" + 
            "    <a rel=\"help\" href=\"http://pydap.org/client.html#accessing-sequential-data/\">Pydap Client instructions for accessing sequential data</a>.\n" +
            "  <br>Note that the name of a dataset in tabledap will always be a single word, e.g., " + EDStatic.EDDTableIdExample + "\n" +
            "  <br>in the OPeNDAP dataset URL\n" +
            "  <br>" + datasetBase + "\n" +
            "  <br>and won't ever have a file extension (unlike, for example, .cdp for the sample dataset in the\n" +
            "  <br>Pydap instructions).  Also, the name of the sequence in tabledap datasets will always be \"" + SEQUENCE_NAME + "\"\n" +
            "  <br>(unlike \"location\" for the sample dataset in the Pydap instructions).\n" +
            "\n" +
            //R
            "  <p><b><a rel=\"bookmark\" href=\"http://www.r-project.org/\">R Statistical Package</a></b> -\n" +
            "    <a name=\"R\">R</a> is an open source statistical package for many operating systems.\n" +
            "  <br>In R, you can download a .csv file from ERDDAP\n" +
            "    and then import data from that .csv file\n" +
            "  <br>into an R structure (e.g., <tt>test</tt>).  For example:<pre>\n" +
            "  download.file(url=\"" + XML.encodeAsHTML(fullTimeCsvExample) + "\", destfile=\"/home/bsimons/test.csv\")\n" +
            "  test&lt;-read.csv(file=\"/home/bsimons/test.csv\")</pre>\n" +
            "\n"); 

        //imageFile Types, graphs and maps
        writer.write(
            "  <p><a name=\"imageFileTypes\"><b>Making an Image File with a Graph or Map of Tabular Data</b></a>\n" +
            "    <br>If a tabledap request URL specifies a subset of data which is suitable for making a graph or\n" +
            "    <br>a map, and the fileType is an image fileType, tabledap will return an image with a graph or map. \n" +
            "    <br>tabledap request URLs can include optional <a rel=\"help\" href=\"#GraphicsCommands\">graphics commands</a> which let you\n" +
            "    <br>customize the graph or map.\n" +
            "    <br>As with other tabledap request URLs, you can create these URLs by hand or have a computer\n" +
            "    <br>program do it. \n" +
            "    <br>Or, you can use the Make A Graph web pages, which simplify creating these URLs (see the\n" +
            "    <br>\"graph\" links in the table of <a href=\"" + 
                dapBase + "index.html\">tabledap datasets</a>). \n" +
            "\n" +
            "   <p>The fileType options for downloading images of graphs and maps of table data are:\n" +
            "  <table class=\"erd\" cellspacing=\"0\">\n" + 
            "    <tr><th>Image<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
        for (int i = 0; i < imageFileTypeNames.length; i++) 
            writer.write(
                "    <tr>\n" +
                "      <td>" + imageFileTypeNames[i] + "</td>\n" +
                "      <td>" + imageFileTypeDescriptions[i] + "</td>\n" +
                "      <td>" + 
                      (imageFileTypeInfo[i] == null || imageFileTypeInfo[i].equals("")? 
                          "&nbsp;" : "<a rel=\"help\" href=\"" +  imageFileTypeInfo[i] + "\">info</a>") + 
                      "</td>\n" + //must be mapExample below because kml doesn't work with graphExample
                "      <td><a href=\"" +  datasetBase + imageFileTypeNames[i] + "?" + 
                    EDStatic.phEncode(EDStatic.EDDTableMapExample) + "\">example</a></td>\n" +
                "    </tr>\n");
        writer.write(
            "   </table>\n" +
            "\n" +
        //size
            "  <p>Image Size - \".small\" and \".large\" were ERDDAP's original system for making\n" +
            "  <br>different-sized images. Now, for .png and .transparentPng images (not other\n" +
            "  <br>image file types), you can also use the\n" +
            "    <a rel=\"help\" href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a>\n" +
            "    parameter to request\n" +
            "  <br>an image of any size.\n" +
            "\n" +
        //transparentPng
            "  <p><a name=\"transparentPng\">.transparentPng</a> - The .transparentPng file type will make a graph or map without\n" +
            "  <br>the graph axes, landmask, or legend, and with a transparent (not opaque white)\n" +
            "  <br>background.  This file type can be used for any type of graph or map.\n" +
            "  <br>For graphs and maps, the default size is 360x360 pixels.\n" +
            "  <br>Or, you can use the <a rel=\"help\" href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a>\n" +
            "    parameter to request an image of any size.\n" + 
            "\n");

        //file type incompatibilities
        writer.write(
            "  <p><b>Incompatibilities</b>\n" +
            "  <br>Some results file types have restrictions. For example, Google Earth .kml is only\n" +
            "  <br>appropriate for results with longitude and latitude values. If a given request is\n" +
            "  <br>incompatible with the requested file type, tabledap throws an error.\n" + 
            "\n" +

        //curl
            "<p><a name=\"curl\"><b>Command Line Downloads with curl</b></a>\n" +
            "<br>If you want to download a series of files from ERDDAP, you don't have to request each file's\n" +
            "<br>ERDDAP URL in your browser, sitting and waiting for each file to download. \n" +
            "<br>If you are comfortable writing computer programs (e.g., with C, Java, Python, Matlab, r)\n" +
            "<br>you can write a program with a loop that imports all of the desired data files.\n" +
            "<br>Or, if are comfortable with command line programs (just running a program, or using bash or tcsh\n" +
            "<br>scripts in Linux or Mac OS X, or batch files in Windows), you can use curl to save results files\n" +
            "<br>from ERDDAP into files on your hard drive, without using a browser or writing a computer program.\n" +
            "<br>ERDDAP+curl is amazingly powerful and allows you to use ERDDAP in many new ways.\n" +
            "<br>On Linux or Mac OS X, curl is probably already installed as /usr/bin/curl.\n" +
            "<br>On Windows, or if your computer doesn't have curl already, you need to \n" +
            "  <a rel=\"bookmark\" href=\"http://curl.haxx.se/download.html\">download curl</a>\n" +
            "<br>and install it.  To get to a command line in Windows, use \"Start : Run\" and type in \"cmd\".\n" +
            "<br>(\"Win32 - Generic, Win32, binary (without SSL)\" worked for me on Windows XP and Windows 7.)\n" +
            "<br>Instructions for using curl are on the \n" +
                "<a rel=\"help\" href=\"http://curl.haxx.se/download.html\">curl man page</a> and in this\n" +
                "<a rel=\"help\" href=\"http://curl.haxx.se/docs/httpscripting.html\">curl tutorial</a>.\n" +
            "<br>But here is a quick tutorial related to using curl with ERDDAP:\n" +
            "<ul>\n" +
            "<li>To download and save one file, use \n" +
            "  <br><tt>curl -g \"<i>erddapUrl</i>\" -o <i>fileDir/fileName.ext</i></tt>\n" +
            "  <br>where <tt>-g</tt> disables curl's globbing feature,\n" +
            "  <br>&nbsp;&nbsp;<tt><i>erddapUrl</i></tt> is any ERDDAP URL that requests a data or image file, and\n" +
            "  <br>&nbsp;&nbsp;<tt>-o <i>fileDir/fileName.ext</i></tt> specifies the name for the file that will be created.\n" +
            "  <br>For example,\n" +
            "<pre>curl -g \"http://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet.png?time,atmp&amp;time%3E=2010-09-03T00:00:00Z&amp;time%3C=2010-09-06T00:00:00Z&amp;station=%22TAML1%22&amp;.draw=linesAndMarkers&amp;.marker=5|5&amp;.color=0x000000&amp;.colorBar=|||||\" -o NDBCatmpTAML1.png</pre>\n" +
            "  The erddapUrl must be <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>.\n" +
            "  <br>If you get the URL from your browser's address textfield, this may be already done.\n" +
            "  <br>If not, in practice, this can be very minimal percent encoding: all you usually\n" +
            "  <br>have to do is convert % into %25, &amp; into %26, \" into %22, + into %2B,\n" +
            "  <br>space into %20 (or +), &lt; into %3C, &gt; into %3E, ~ into %7E, and convert\n" +
            "  <br>all characters above #126 to their %HH form (where HH is the 2-digit hex value).\n" +
            "  <br>Unicode characters above #255 must be UTF-8 encoded and then each byte\n" +
            "  <br> must be converted to %HH form (ask a programmer for help).\n" +
            "  <br>&nbsp;\n" +
            "<li>To download and save many files in one step, use curl with the globbing feature enabled:\n" +
            "  <br><tt>curl \"<i>erddapUrl</i>\" -o <i>fileDir/fileName#1.ext</i></tt>\n" +
            "  <br>Since the globbing feature treats the characters [, ], {, and } as special, you must also\n" +
            "  <br><a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encode</a> \n" +
              "them in the erddapURL as &#37;5B, &#37;5D, &#37;7B, &#37;7D, respectively.\n" +
            "  <br>Fortunately, these are rare in tabledap URLs.\n" +
            "  <br>Then, in the erddapUrl, replace a zero-padded number (for example <tt>01</tt>) with a range\n" +
            "  <br>of values (for example, <tt>[01-05]</tt> ),\n" +
            "  <br>or replace a substring (for example <tt>TAML1</tt>) with a list of values (for example,\n" +
            "  <br><tt>{TAML1,41009,46088}</tt> ).\n" +
            "  <br>The <tt>#1</tt> within the output fileName causes the current value of the range or list\n" +
            "  <br>to be put into the output fileName.\n" +
            "  <br>For example, \n" +
            "<pre>curl \"http://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet.png?time,atmp&amp;time%3E=2010-09-03T00:00:00Z&amp;time%3C=2010-09-06T00:00:00Z&amp;station=%22{TAML1,41009,46088}%22&amp;.draw=linesAndMarkers&amp;.marker=5|5&amp;.color=0x000000&amp;.colorBar=|||||\" -o NDBCatmp#1.png</pre>\n" +
            "</ul>\n" +
            "<br>&nbsp;\n");


        //query
        writer.write(
            "<li><a name=\"query\"><b>query</b></a> is the part of the request after the \"?\".\n" +
            "  It specifies the subset of data that you want to receive.\n" +
            "  <br>In tabledap, it is an \n" +
            "  <a rel=\"bookmark\" href=\"http://www.opendap.org/\">OPeNDAP</a> " +
            "  <a rel=\"help\" href=\"http://www.opendap.org/pdf/ESE-RFC-004v1.2.pdf\">DAP</a>\n" +
            "  <a rel=\"help\" href=\"http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Constraint_Expressions\">selection constraint</a> query" +
            "  in the form: <tt>{<i>resultsVariables</i>}{<i>constraints</i>}</tt> .\n " +
            "  <br>For example,\n" +
            "  <br><tt>" + 
                XML.encodeAsHTML(EDStatic.EDDTableVariablesExample + EDStatic.EDDTableConstraintsExample) + 
                "</tt>\n" +
            "  <ul>\n" +
            "  <li><i><b>resultsVariables</b></i> is an optional comma-separated list of variables\n" +
            "        (for example,\n" +
            "    <br><tt>" + XML.encodeAsHTML(EDStatic.EDDTableVariablesExample) + "</tt>).\n" +
            "    <br>For each variable in resultsVariables, there will be a column in the \n" +
            "    <br>results table, in the same order.\n" +
            "    <br>If you don't specify any results variables, the results table will include\n" +
            "    <br>columns for all of the variables in the dataset.\n" +
            "  <li><i><b>constraints</b></i> is an optional list of constraints, each preceded by &amp;\n" +
            "    (for example,\n" +
            "    <br><tt>" + XML.encodeAsHTML(EDStatic.EDDTableConstraintsExample) + "</tt>).\n" +
            "    <ul>\n" +
            "    <li>The constraints determine which rows of data from the original table\n" +
            "      <br>table are included in the results table. \n" +
            "      <br>The constraints are applied to each row of the original table.\n" +
            "      <br>If all the constraints evaluate to <tt>true</tt> for a given row,\n" +
            "      <br>that row is included in the results table.\n" +
            "      <br>Thus, \"&amp;\" can be roughly interpreted as \"and\".\n" +
            "    <li>If you don't specify any constraints, all rows from the original table\n" +
            "      <br>will be included in the results table.\n" +
            "      <br>For the fileTypes that return information about the dataset (notably,\n" +
            "      <br>.das, .dds, and .html), but don't return actual data, it is fine not\n" +
            "      <br>to specify constraints. For example,\n" +
            "      <br><a href=\"" + EDStatic.phEncode(fullDdsExample) + "\"><tt>" + 
                                     XML.encodeAsHTML( fullDdsExample) + "</tt></a>\n" +
            "      <br>For the fileTypes that do return data, not specifying any constraints\n" +
            "      <br>may result in a very large results table.\n" + 
            "    <li>tabledap constraints are consistent with \n" +
            "      <a rel=\"bookmark\" href=\"http://www.opendap.org/\">OPeNDAP</a> " +
            "      <a rel=\"help\" href=\"http://www.opendap.org/pdf/ESE-RFC-004v1.2.pdf\">DAP</a>\n" +
            "      <a rel=\"help\" href=\"http://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Constraint_Expressions\">selection constraints</a>,\n" +
            "      <br>but with a few additional features.\n" +
            "    <li>Each constraint is in the form <tt><i>variable</i><i>operator</i><i>value</i></tt>\n" +
            "      <br>(for example, <tt>latitude&gt;45</tt>).\n" +
            "    <li>The valid operators are =, != (not equals),\n" +
            "       =~ (a <a rel=\"help\" href=\"#regularExpression\">regular expression test</a>),\n" +
            "       &lt;, &lt;=, &gt;, and &gt;= .\n" +
            "    <li>tabledap extends the OPeNDAP standard to allow any operator to be used with any data type.\n" +
            "      <br>(Standard OPeNDAP selections don't allow &lt;, &lt;=, &gt;, or &gt;= \n" +
            "         to be used with string variables\n" +
            "      <br>and don't allow =~ to be used with numeric variables.)\n" +
            "      <ul>\n" +
            "      <li>For String variables, all operators act in a case-sensitive manner.\n" +
            "      <li>For String variables, queries for =\"\" and !=\"\" should work correctly for almost all\n" +
            "        <br>datasets.\n" +
            "    <li><a name=\"QuoteStrings\">For</a> <a name=\"backslashEncoded\">all</a> constraints of String variables and for regex constraints of numeric variables,\n" +
            "        <br>the right-hand-side value MUST be enclosed in double quotes (e.g., <tt>id=\"NDBC41201\"</tt>)\n" +
            "        <br>and any internal special characters must be backslash encoded: \\ into \\\\, \" into \\\",\n" +
            "        <br>newline into \\n, and tab into \\t.\n" +
            "    <li><a name=\"PercentEncode\">For</a> all constraints, the <tt><i>variable</i><i>operator</i><i>value</i></tt>\n" +
            "        MUST be <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>.\n" +
            "        <br>If you submit the request via a browser, the browser will do the percent encoding\n" +
            "        <br>for you. If you submit the request via a computer program, then the program\n" +
            "        <br>needs to do the percent encoding.\n" +
            "        <br>In practice, this can be very minimal percent encoding: all you usually\n" +
            "        <br>have to do is convert % into %25, &amp; into %26, \" into %22, + into %2B,\n" +
            "        <br>space into %20 (or +), &lt; into %3C, &gt; into %3E, ~ into %7E, and convert\n" +
            "        <br>all characters above #126 to their %HH form (where HH is the 2-digit hex value).\n" +
            "        <br>Unicode characters above #255 must be UTF-8 encoded and then each byte\n" +
            "        <br> must be converted to %HH form (ask a programmer for help).\n" +
            "      <li><a name=\"comparingFloats\">WARNING:</a> Numeric queries involving =, !=, &lt;=, or &gt;= may not work as desired with\n" +
            "          <br>floating point numbers. For example, a search for <tt>longitude=220.2</tt> may\n" +
            "          <br>fail if the value is stored as 220.20000000000001. This problem arises because\n" +
            "          <br>floating point numbers are " +
            "            <a rel=\"help\" href=\"http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm\">not represented exactly within computers</a>.\n" +
            "          <br>When ERDDAP performs these tests, it allows for minor variations and tries\n" +
            "          <br>to avoid the problem. But it is possible that some datasets will still\n" +
            "          <br>have problems with these queries and return unexpected and incorrect results.\n" +
            "      <li><a rel=\"help\" href=\"http://en.wikipedia.org/wiki/NaN\">NaN (Not-a-Number)</a> -\n" +
            "            <a name=\"NaN\">Many</a> numeric variables have an attribute which identifies a\n" +
            "          <br>number (e.g., -99) as a missing_value or a _FillValue. When ERDDAP tests\n" +
            "          <br>constraints, it always treats these values as NaN's. So:\n" +
            "          <br>Don't create constraints like <tt>temperature!=-99</tt> .\n" +
            "          <br>Do&nbsp;&nbsp;&nbsp;&nbsp; create constraints like <tt>temperature!=NaN</tt> .\n" +
            "        <ul>\n" +
            "        <li>For numeric variables, tests of <tt><i>variable</i>=NaN</tt> (Not-a-Number) and <tt><i>variable</i>!=NaN</tt>\n" +
            "          <br>will usually work as expected.\n" +
            "          <br><b>WARNING: numeric queries with =NaN and !=NaN may not work as desired</b>\n" +
            "          <br>since many data sources don't offer native support for these queries and\n" +
            "          <br>ERDDAP can't always work around this problem.\n" +
            "          <br>For some datasets, queries with =NaN or !=NaN may fail with an error message\n" +
            "          <br>(insufficient memory or timeout) or erroneously report\n" +
            "          <br><tt>" + MustBe.THERE_IS_NO_DATA + "</tt>\n" +
            "        <li>For numeric variables, tests of <tt><i>variable</i>&lt;NaN</tt> (Not-a-Number) (or &lt;=, &gt;, &gt;=) will\n" +
            "          <br>return false for any value of the variable, even NaN.  NaN isn't a number so these\n" +
            "          <br>tests are nonsensical.\n" +
            "        </ul>\n" +
            "      <li><a name=\"regularExpression\"><i>variable</i>=~\"<i>regularExpression</i>\"</a>" +
            "          tests if the value from the variable on the left matches the \n" +
            "         <br><a rel=\"help\" href=\"http://download.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html\">regular expression</a>\n" +
            "         on the right.\n" +
            "        <ul>\n" +
            "        <li>tabledap uses the same \n" +
            "             <a rel=\"help\" href=\"http://download.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html\">regular expression syntax</a>\n" +
            "             (<a rel=\"help\" href=\"http://www.vogella.de/articles/JavaRegularExpressions/article.html\">tutorial</a>) as is used by Java.\n" +
            "        <li><b>WARNING</b> - For numeric variables, the =~ test is performed on the String representation\n" +
            "          <br>of the variable's source values.  So if the variable's source uses scale_factor\n" +
            "          <br>and/or add_offset metadata (which ERDDAP uses to unpack the data) or if the variable\n" +
            "          <br>is an altitude variable that ERDDAP has converted from a depth value, the =~ test\n" +
            "          <br>will be applied to the raw source values.  This is not a good situation, but there\n" +
            "          <br>is no easy way to deal with it. It is fine to try =~ tests of numeric variables, but\n" +
            "          <br>but be very skeptical of the results.\n" +
            "        <li><b>WARNING: queries with =~ may not work as desired</b> since many data sources\n" +
            "          <br>don't offer native support for these queries and ERDDAP can't always work\n" +
            "          <br>around this problem.\n" +
            "          <br>ERDDAP sometimes has to get the entire dataset to do the test itself.\n" +
            "          <br>For some datasets, queries with =~ may fail with an error message\n" +
            "          <br>(insufficient memory or timeout) or erroneously report\n" +
            "          <br><tt>" + MustBe.THERE_IS_NO_DATA + "</tt>\n" +
            "          <br>.\n" +
            "        </ul>\n" +
            "      </ul>\n" +
            "    <li>tabledap extends the OPeNDAP standard to allow constraints (selections) to be applied to any variable\n" +
            "      <br>in the dataset. The constraint variables don't have to be included in the resultsVariables.\n" +
            "      <br>(Standard OPeNDAP implementations usually only allow constraints to be applied\n" +
            "      <br>to certain variables. The limitations vary with different implementations.)\n" +           
            "    <li>Although tabledap extends the OPeNDAP selection standard (as noted above),\n" +
            "      <br>these extensions (notably \"=~\") sometimes aren't practical because tabledap\n" +
            "      <br>may need to download lots of extra data from the source (which takes time)\n" +
            "      <br>in order to test the constraint.\n" +
            "    <li><a name=\"timeConstraints\">tabledap</a> always stores date/time values as numbers (in seconds since 1970-01-01T00:00:00Z).\n" +
            "      <br>Here is an example of a query which includes date/time numbers:\n" +
            "      <br><a href=\"" + EDStatic.phEncode(fullValueExample) + "\"><tt>" + 
                                     XML.encodeAsHTML( fullValueExample) + "</tt></a>\n" +
            "      <br>Some fileTypes (notably, .csv, .tsv, .htmlTable, .odvTxt, and .xhtml) display\n" +
            "      <br>date/time values as \n" +
            "        <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" date/time strings</a>\n" +
            "      <br>(e.g., <tt>2002-08-03T12:30:00Z</tt>).\n" +
            (EDStatic.convertersActive? 
              "      <br>ERDDAP has a utility to\n" +
              "        <a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/time.html\">Convert\n" +
              "        a Numeric Time to/from a String Time</a>.\n" +
              "      <br>See also:\n" +
              "        <a rel=\"help\" href=\"" + tErddapUrl + "/convert/time.html#erddap\">How\n" +
              "        ERDDAP Deals with Time</a>.\n" : "") +
            "    <li>tabledap extends the OPeNDAP standard to allow you to specify time values in the ISO 8601\n" +
            "      <br>date/time format (<tt><i>YYYY-MM-DD</i>T<i>hh:mm:ssZ</i></tt>, where Z is 'Z' or a &plusmn;hh:mm offset from UTC). \n" +
            "      <br>If you omit Z (or the &plusmn;hh:mm offset), :ssZ, :mm:ssZ, or Thh:mm:ssZ from the ISO date/time\n" +
            "      <br>that you specify, the missing fields are assumed to be 0.\n" +
            "      <br>Here is an example of a query which includes ISO date/time values:\n" +
            "      <br><a href=\"" + EDStatic.phEncode(fullTimeExample) + "\"><tt>" + 
                                     XML.encodeAsHTML( fullTimeExample) + "</tt></a> .\n" +
            "    <li><a name=\"now\">tabledap</a> extends the OPeNDAP standard to allow you to specify constraints for\n" +
            "      <br>time and timestamp variables relative to <tt>now</tt>. The constraint can be simply,\n" +
            "      <br>for example, <tt>time&lt;now</tt>, but usually the constraint is in the form\n" +
            "      <br><tt>&nbsp;&nbsp;now(+| |-)<i>positiveInteger</i>(second|seconds|minute|minutes|\n" +
            "      <br>&nbsp;&nbsp;hour|hours|day|days|month|months|year|years)</tt>\n" +
            "      <br>for example, <tt>now-7days</tt>.\n" +
            "      <br>Months and years are interpreted as calendar months and years (not\n" +
            "      <br>UDUNITS-like constant values).\n" +
            "      <br>This feature is especially useful when creating a URL for an &lt;img&gt; (image)\n" +
            "      <br>tag on a web page, since it allows you to specify that the image will always\n" +
            "      <br>show, for example, the last 7 days worth of data (<tt>&amp;time&gt;now-7days</tt>).\n" +
            "      <br>Note that a '+' in the URL is percent-decoded as ' ', so you should\n" +
            "      <br>percent-encode '+' as %2B.  However, ERDDAP interprets <tt>\"now \"</tt> as <tt>\"now+\"</tt>,\n" +
            "      <br>so in practice you don't have to percent-encode it.\n" +
            "    </ul>\n" +
            "  <li><a name=\"functions\"><b>Server-side Functions</b></a> - The OPeNDAP standard supports the idea of\n" +
            "    <br>server-side functions, but doesn't define any.\n" +
            "    <br>TableDAP supports some server-side functions that modify the results table before\n" +
            "    <br>the results table is sent to you.\n" +
            "    <br>Server-side functions are OPTIONAL.  Other than 'distinct', they are rarely needed.\n" +
            "    <br>Currently, each of the functions takes the results table as input and\n" +
            "    <br>returns a table with the same columns in the same order (but the rows may be\n" +
            "    <br>be altered). If you use more than one function, they will be applied in the order\n" +
            "    <br>that they appear in the request. Most of these options appear near the bottom\n" +
            "    <br>of the Data Access Form and Make A Graph web page for each dataset.\n" + 
            "    <ul>\n" +
            "    <li><a name=\"distinct\"><tt>&amp;distinct()</tt></a>\n" +
            "      <br>If you add <tt>&amp;distinct()</tt> to the end of a query, ERDDAP will sort all of the\n" +
            "      <br>rows in the results table (starting with the first requested variable, then using the\n" +
            "      <br>second requested variable if the first variable has a tie, ...), then remove all\n" +
            "      <br>non-unique rows of data.\n" +
            "      <br>For example, the query <tt>stationType,stationID&amp;distinct()</tt> will return a\n" +
            "      <br>sorted list of stationIDs associated with each stationType.\n" +
            "      <br>In many situations, ERDDAP can return distinct() values quickly and efficiently.\n" +
            "      <br>But in some cases, ERDDAP must look through all rows of the source dataset. \n" +
            "      <br>If the data set isn't local, this may be VERY slow, may return only some of the\n" +
            "      <br>results (without an error), or may throw an error.\n" +
            "    <li><a name=\"orderBy\"><tt>&amp;orderBy(\"<i>comma-separated list of variable names</i>\")</tt></a>\n" +
            "      <br>If you add this to the end of a query, ERDDAP will sort all of the rows in the results\n" +
            "      <br>table (starting with the first variable, then using the second variable if the first\n" +
            "      <br>variable has a tie, ...).\n" +
            "      <br>Normally, the rows of data in the response table are in the order they arrived from\n" +
            "      <br>the data source.\n" +
            "      <br>orderBy allows you to request that the results table be sorted in a specific way.\n" +
            "      <br>For example, use the query\n" +
            "      <br><tt>stationID,time,temperature&amp;time&gt;" + daysAgo7 + "&amp;orderBy(\"stationID,time\")</tt>\n" +
            "      <br>to get the results sorted by stationID, then time.\n" +
            "      <br>Or use the query\n" +
            "      <br><tt>stationID,time,temperature&amp;time&gt;" + daysAgo7 + "&amp;orderBy(\"time,stationID\")</tt>\n" +
            "      <br>to get the results sorted by time first, then stationID.\n" +
            "      <br>The orderBy variables MUST be included in the list of requested variables in the\n" +
            "      <br>query as well as in the orderBy list of variables.\n" +
            "    <li><a name=\"orderByMax\"><tt>&amp;orderByMax(\"<i>comma-separated list of variable names</i>\")</tt></a>\n" +
            "      <br>If you add this to the end of a query, ERDDAP will sort all of the rows in the results\n" +
            "      <br>table (starting with the first variable, then using the second variable if the first\n" +
            "      <br>variable has a tie, ...) and then just keeps the rows where the value of the last\n" +
            "      <br>sort variable is highest (for each combination of other values).\n" +
            "      <br>For example, use the query\n" +
            "      <br><tt>stationID,time,temperature&amp;time&gt;" + daysAgo7 + "&amp;orderByMax(\"stationID,time\")</tt>\n" +
            "      <br>to get just the rows of data with each station's maximum time value (for stations\n" +
            "      <br>with data from after " + daysAgo7 + ").\n" +
            "      <br>(Without the time constraint, ERDDAP would have to look through all rows of the \n" +
            "      <br>the dataset, which might be VERY slow.)\n" +
            "      <br>The orderByMax variables MUST be included in the list of requested variables in the\n" +
            "      <br>query as well as in the orderByMax list of variables.\n" +
            "      <br>This is the closest thing in tabledap to griddap's allowing requests for the <tt>[last]</tt>\n" +
            "      <br>axis value.\n" +
            "    <li><a name=\"orderByMin\"><tt>&amp;orderByMin(\"<i>comma-separated list of variable names</i>\")</tt></a>\n" +
            "      <br>orderByMin is exactly like <a href=\"#orderByMax\">orderByMax</a>, except that it\n" +
            "      <br>returns the minimum value(s).\n" +
            "    <li><a name=\"orderByMinMax\"><tt>&amp;orderByMinMax(\"<i>comma-separated list of variable names</i>\")</tt></a>\n" +
            "      <br>orderByMinMax is like <a href=\"#orderByMax\">orderByMax</a>, except that it returns\n" +
            "      <br>two rows for every combination of the n-1 variables: one row with the minimum value,\n" +
            "      <br>and one row with the maximum value.\n" +
            "      <br>For example, use the query\n" +
            "      <br><tt>stationID,time,temperature&amp;time&gt;" + daysAgo7 + "&amp;orderByMinMax(\"stationID,time\")</tt>\n" +
            "      <br>to get just the rows of data with each station's minimum time value and each station's\n" +
            "      <br>maximum time value (for stations with data from after " + daysAgo7 + ").\n" +
            "      <br>If there is only one row of data for a given combination (e.g., stationID), there will\n" +
            "      <br>still be two rows in the output (with identical data).\n" +
            "    <li><a name=\"units\"><tt>&amp;units(\"<i>value</i>\")</tt></a>\n" +
            "      <br>If you add <tt>&amp;units(\"UDUNITS\")</tt> to the end of a query, the units will be described\n" +
            "      <br>via the\n" +
            "        <a rel=\"help\" href=\"http://www.unidata.ucar.edu/software/udunits/\">UDUNITS</a> standard (for example, <tt>degrees_C</tt>).\n" +
            "      <br>If you add <tt>&amp;units(\"UCUM\")</tt> to the end of a query, the units will be described\n" +
            "      <br>via the\n" +
            "        <a rel=\"help\" href=\"http://unitsofmeasure.org/ucum.html\">UCUM</a> standard (for example, <tt>Cel</tt>).\n" +
            "      <br>On this ERDDAP, the default for most/all datasets is " + EDStatic.units_standard + ".\n" +
            (EDStatic.convertersActive? 
            "      <br>See also ERDDAP's <a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/units.html\">units converter</a>.\n" : "") +
            "    </ul>\n" +
               
            //Graphics Commands
            "  <li><a name=\"GraphicsCommands\"><b>Graphics Commands</b></a> - <a name=\"MakeAGraph\">tabledap</a> extends the OPeNDAP standard by allowing graphics commands\n" +
            "    <br>in the query. \n" +
            "    <br>The Make A Graph web pages simplify the creation of URLs with these graphics commands\n" +
            "    <br>(see the \"graph\" links in the table of <a href=\"" + 
               dapBase + "index.html\">tabledap datasets</a>). \n" +
            "    <br>So we recommend using the Make A Graph web pages to generate URLs, and then, when\n" +
            "    <br>needed, using the information here to modify the URLs for special purposes.\n" +
            "    <br>These commands are optional.\n" +
            "    <br>If present, they must occur after the data request part of the query. \n" +
            "    <br>These commands are used by tabledap if you request an <a rel=\"bookmark\" href=\"#imageFileTypes\">image fileType</a> (.png or .pdf)\n" +
            "    <br>and are ignored if you request a data file (e.g., .asc). \n" +
            "    <br>If relevant commands are not included in your request, tabledap uses the defaults and\n" +
            "    <br>tries its best to generate a reasonable graph or map. \n" +
            "    <br>All of the commands are in the form &amp;.<i>commandName</i>=<i>value</i> . \n" +
            "    <br>If the value has sub-values, they are separated by the '|' character. \n" +
            "    <br>The commands are:\n" +
            "    <ul>\n" +
            "    <li><tt>&amp;.colorBar=<i>palette</i>|<i>continuous</i>|<i>scale</i>|<i>min</i>|<i>max</i>|<i>nSections</i></tt> \n" +
            "      <br>This specifies the settings for a color bar.  The sub-values are:\n" +
            "      <ul>\n" +
            //the standard palettes are listed in 'palettes' in Bob's messages.xml, 
            //        EDDTable.java, EDDGrid.java, and setupDatasetsXml.html
            "      <li><i>palette</i> - All ERDDAP installations support a standard set of palettes:\n" +
            "        <br>BlackBlueWhite, BlackRedWhite, BlackWhite, BlueWhiteRed, LightRainbow,\n" +
            "        <br>Ocean, OceanDepth, Rainbow, RedWhiteBlue, ReverseRainbow, Topography,\n" +
            "        <br>WhiteBlack, WhiteBlueBlack, WhiteRedBlack.\n" +
            "        <br>Some ERDDAP installations support additional options.\n" +
            "        <br>See a Make A Graph web page for a complete list.\n" +
            "        <br>The default (no value) varies based on min and max: if -1*min ~= max,\n" +
            "        <br>the default is BlueWhiteRed; otherwise, the default is Rainbow.\n" +
            "      <li><i>continuous</i> - must be either no value (the default), 'C' (for Continuous),\n" +
            "        <br>or 'D' (for Discrete). The default is different for different datasets.\n" +
            "      <li><i>scale</i> - must be either no value (the default), <tt>Linear</tt>, or <tt>Log</tt>.\n" +
            "         <br>The default is different for different datasets.\n" +
            "      <li><i>min</i> - The minimum value for the color bar.\n" +
            "        <br>The default is different for different datasets.\n" +
            "      <li><i>max</i> - The maximum value for the color bar.\n" +
            "        <br>The default is different for different datasets.\n" +
            "      <li><i>nSections</i> - The preferred number of sections (for Log color bars,\n" +
            "        <br>this is a minimum value). The default is different for different datasets.\n" +
            "      </ul>\n" +
            "      If you don't specify one of the sub-values, the default for the sub-value will be used.\n" +
            "    <li><tt>&amp;.color=<i>value</i></tt>\n" +
            "        <br>This specifies the color for data lines, markers, vectors, etc.  The value must\n" +
            "        <br>be specified as an 0xRRGGBB value (e.g., 0xFF0000 is red, 0x00FF00 is green).\n" +
            "        <br>The default is 0x000000 (black).\n" +
            "    <li><tt>&amp;.draw=<i>value</i></tt> \n" +
            "        <br>This specifies how the data will be drawn, as <tt>lines</tt>,\n" +
            "        <br><tt>linesAndMarkers</tt>, <tt>markers</tt> (default), <tt>sticks</tt>, or <tt>vectors</tt>. \n" +
            "    <li><tt>&amp;.font=<i>scaleFactor</i></tt>\n" +
            "        <br>This specifies a scale factor for the font\n" +
            "        <br>(e.g., 1.5 would make the font 1.5 times as big as normal).\n" +
            "    <li><tt>&amp;.land=<i>value</i></tt>\n" +
            "      <br>This is only relevant if the first two results variables are longitude and latitude,\n" +
            "      <br>so that the graph is a map.\n" +
            "      <br>The default is different for different datasets (under the ERDDAP administrator's\n" +
            "      <br>control via a drawLandMask in datasets.xml, or via the fallback <tt>drawLandMask</tt>\n" +
            "      <br>setting in setup.xml).\n" +
            "      <br><tt>over</tt> makes the land mask on a map visible (land appears as a uniform gray area).\n" +
            "      <br><tt>over</tt> is commonly used for purely oceanographic datasets.\n" +
            "      <br><tt>under</tt> makes the land mask invisible (topography information is displayed\n" +
            "      <br>for ocean and land areas).  <tt>under</tt> is commonly used for all other data.\n" +
            "    <li><tt>&amp;.legend=<i>value</i></tt>\n" +
            "      <br>This specifies whether the legend on .png images (not .pdf's) should be at the\n" +
            "      <br><tt>Bottom</tt> (default), <tt>Off</tt>, or <tt>Only</tt> (which returns only the legend).\n" +
            "    <li><tt>&amp;.marker=<i>markerType</i>|<i>markerSize</i></tt>\n" +
            "      <br>markerType is an integer: 0=None, 1=Plus, 2=X, 3=Dot, 4=Square,\n" +
            "      <br>5=Filled Square (default), 6=Circle, 7=Filled Circle, 8=Up Triangle,\n" +
            "      <br>9=Filled Up Triangle.\n" +
            "      <br>markerSize is an integer from 3 to 50 (default=5)\n" +
            "    <li><tt>&amp;.size=<i>width</i>|<i>height</i></tt>\n" +
            "      <br>For .png images (not .pdf's), this specifies the desired size of the image, in pixels.\n" +
            "      <br>This allows you to specify sizes other than the predefined sizes of .smallPng, .png, and\n" +
            "      <br>.largePng.\n" +
            "    <li><tt>&amp;.trim=<i>trimPixels</i></tt>\n" +
            "      <br>For .png images (not .pdf's), this tells ERDDAP to make the image shorter by removing\n" +
            "      <br>all whitespace at the bottom, except for <i>trimPixels</i>.\n" +
            "    <li>There is no <tt>&amp;.vars=</tt> command. Instead, the results variables from the main part\n" +
            "      <br>of the query are used.\n" +
            "      <br>The meaning associated with each position varies with the <tt>&amp;.draw</tt> value:\n" +
            "      <ul>\n" +
            "      <li>for lines: xAxis,yAxis\n" +
            "      <li>for linesAndMarkers: xAxis,yAxis,Color\n" +
            "      <li>for markers: xAxis,yAxis,Color\n" +
            "      <li>for sticks: xAxis,uComponent,vComponent\n" +
            "      <li>for vectors: xAxis,yAxis,uComponent,vComponent\n" +
            "      </ul>\n" +
            "      If xAxis=longitude and yAxis=latitude, you get a map; otherwise, you get a graph.\n " +
            "    <li><tt>&amp;.vec=<i>value</i></tt>\n" +
            "      <br>This specifies the data vector length (in data units) to be scaled to the size\n" +
            "      <br>of the sample vector in the legend. The default varies based on the data.\n" +
            "    <li><tt>&amp;.xRange=<i>min</i>|<i>max</i></tt>\n" +
            "      <br>This specifies the min|max for the X axis. The default varies based on the data.\n" +
            "    <li><tt>&amp;.yRange=<i>min</i>|<i>max</i></tt>\n" +
            "      <br>This specifies the min|max for the Y axis. The default varies based on the data.\n" +
            "    </ul>\n" +
            "    <br><a name=\"sampleGraphURL\">A sample graph URL is</a> \n" +
            "    <br><a href=\"" + EDStatic.phEncode(fullGraphExample) + "\">" + 
                                   XML.encodeAsHTML( fullGraphExample) + "</a>\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to .graph,\n" +
            "    <br>you can see a Make A Graph web page with that request loaded:\n" +
            "    <br><a href=\"" + EDStatic.phEncode(fullGraphMAGExample) + "\">" + 
                                   XML.encodeAsHTML( fullGraphMAGExample) + "</a>\n" +
            "    <br>That makes it easy for humans to modify an image request to make a\n" +
            "    <br>similar graph or map.\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to a data fileType\n" +
            "    <br>(e.g., .htmlTable), you can download the data that was graphed:\n" +
            "    <br><a href=\"" + EDStatic.phEncode(fullGraphDataExample) + "\">" + 
                                   XML.encodeAsHTML( fullGraphDataExample) + "</a>\n" +
            "\n" +
            "    <p>A sample map URL is \n" +
            "    <br><a href=\"" + EDStatic.phEncode(fullMapExample) + "\">" + 
                                   XML.encodeAsHTML( fullMapExample) + "</a>\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to .graph,\n" +
            "    <br>you can see a Make A Graph web page with that request loaded:\n" +
            "    <br><a href=\"" + EDStatic.phEncode(fullMapMAGExample) + "\">" + 
                                   XML.encodeAsHTML( fullMapMAGExample) + "</a>\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to a data fileType\n" +
            "    <br>(e.g., .htmlTable), you can download the data that was mapped:\n" +
            "    <br><a href=\"" + EDStatic.phEncode(fullMapDataExample) + "\">" + 
                                   XML.encodeAsHTML( fullMapDataExample) + "</a>\n" +
            "  </ul>\n" +
            "</ul>\n" +

            //other info
            "<a name=\"otherInformation\"><b>Other Information</b></a>\n" +
            "<ul>\n" +
            "<li><a name=\"dataModel\"><b>Data Model</b></a> - Each tabledap dataset can be represented as a table with one\n" +
            "<br>or more rows and one or more columns of data.\n" + 
            "  <ul>\n" +
            "  <li>Each column is also known as a \"data variable\" (or just a \"variable\").\n" +
            "    <br>Each data variable has data of one specific type.\n" +
            "    <br>The supported types are (int8, uint16, int16, int32, int64, float32, float64, and \n" +
            "    <br>String of any length).\n" +
            "    <br>Any cell in the table may have a no value (i.e., a missing value).\n" +
            "    <br>Each data variable has a name composed of a letter (A-Z, a-z) and then 0 or more\n" +
            "    <br>characters (A-Z, a-z, 0-9, _).\n" +
            "    <br>Each data variable has metadata which is a set of Key=Value pairs.\n" +
            "  <li>Each dataset has global metadata which is a set of Key=Value pairs.\n" +
            "  <li>Note about metadata: each variable's metadata and the global metadata is a set of 0\n" +
            "    <br>or more Key=Value pairs.\n" +
            "    <br>Each Key is a String consisting of a letter (A-Z, a-z) and then 0 or more other\n" +
            "    <br>characters (A-Z, a-z, 0-9, '_').\n" +
            "    <br>Each Value is either one or more numbers (of one Java type), or one or more Strings\n" +
            "    <br>of any length (using \\n as the separator).\n" +
            "    <br>&nbsp;\n" +
            "  </ul>\n" +
            "<li><a name=\"specialVariables\"><b>Special Variables</b></a>\n" +
            "  <ul>\n" +
            "  <li>In tabledap, a longitude variable (if present) always has the name \"" + EDV.LON_NAME + "\"\n" +
            "    <br>and the units \"" + EDV.LON_UNITS + "\".\n" +
            "  <li>In tabledap, a latitude variable (if present) always has the name \"" + EDV.LAT_NAME + "\"\n" +
            "    <br>and the units \"" + EDV.LAT_UNITS + "\".\n" +
            "  <li>In tabledap, an altitude variable (if present) always has the name \"" + EDV.ALT_NAME + "\"\n" +
            "    <br>and the units \"" + EDV.ALT_UNITS + "\" above sea level.\n" +
            "    <br>Locations below sea level have negative altitude values.\n" +
            "  <li>In tabledap, a depth variable (if present) always has the name \"" + EDV.DEPTH_NAME + "\"\n" + 
            "    <br>and the units \"" + EDV.DEPTH_UNITS + "\" below sea level.\n" +
            "    <br>Locations below sea level have positive depth values.\n" +
            "  <li>In tabledap, a time variable (if present) always has the name \"" + EDV.TIME_NAME + "\" and\n" +
            "    <br>the units \"" + EDV.TIME_UNITS + "\".\n" +
            "    <br>If you request data and specify a time constraint, you can specify the time as\n" +
            "    <br>a number (seconds since 1970-01-01T00:00:00Z) or as a\n" +
            "    <br>String value (e.g., \"2002-12-25T07:00:00Z\" in the UTC/GMT/Zulu time zone).\n" +
            "  <li>In tabledap, other variables can be timeStamp variables, which act like the time\n" +
            "      <br>variable but have a different name.\n" +
            "  <li>Because the longitude, latitude, altitude, depth, and time variables are specifically\n" +
            "      <br>recognized, ERDDAP is aware of the spatiotemporal features of each dataset.\n" +
            "      <br>This is useful when making images with maps or time-series, and when saving\n" +
            "      <br>data in geo-referenced file types (e.g., .geoJson and .kml).\n" +
            "      <br>&nbsp;\n" +
            "  </ul>\n" +
            "<li><a name=\"incompatibilities\"><b>Incompatibilities</b></a>\n" +
            "  <ul>\n" +
            "  <li>Constraints - Different types of data sources support different types of constraints.\n" +
            "    <br>Most data sources don't support all of the types of constraints that tabledap\n" +
            "    <br>advertises. For example, most data sources can't test <tt><i>variable=~\"RegularExpression\"</i></tt>.\n" +
            "    <br>When a data source doesn't support a given type of constraint, tabledap\n" +
            "    <br>gets extra data from the source, then does that constraint test itself.\n" +
            "    <br>Sometimes, getting the extra data takes a lot of time or causes the remote\n" +
            "    <br>server to throw an error.\n" +
            "  <li>File Types - Some results file types have restrictions.\n" +
            "    <br>For example, .kml is only appropriate for results with longitude and latitude\n" +
            "    <br>values. If a given request is incompatible with the requested file type,\n" +
            "    <br>tabledap throws an error.\n" + 
            "    <br>&nbsp;\n" +
            "  </ul>\n" +
            "<li>" + OutputStreamFromHttpResponse.acceptEncodingHtml +
            "    <br>&nbsp;\n" +
            "</ul>\n" +
            "<br>&nbsp;\n");

        writer.flush(); 

    }                   


    /**
     * This deals with requests for a Make A Graph (MakeAGraph, MAG) web page for this dataset. 
     *
     * @param request may be null. Currently, it isn't used.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery    after the '?', still percentEncoded (shouldn't be null).
     *    If the query has missing or invalid parameters, defaults will be used.
     *    If the query has irrelevant parameters, they will be ignored.
     * @param outputStreamSource  the source of an outputStream that receives the results,
     *    usually already buffered.  This doesn't call out.close at the end. The caller must!
     * @param dir the directory to use for temporary/cache files [currently, not used]
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.  [currently, not used]
     * @param fileTypeName must be .graph [currently, not used]
     * @throws Throwable if trouble
     */
    public void respondToGraphQuery(HttpServletRequest request, String loggedInAs,
        String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable {

        if (accessibleViaMAG().length() > 0)
            throw new SimpleException(accessibleViaMAG());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        if (userDapQuery == null)
            userDapQuery = "";
        if (reallyVerbose)
            String2.log("*** respondToGraphQuery");
        if (debugMode) String2.log("respondToGraphQuery 1");

        String formName = "f1"; //change JavaScript below if this changes

        //find the numeric dataVariables
        StringArray sa = new StringArray();
        StringArray nonLLSA = new StringArray();
        StringArray axisSA = new StringArray();
        StringArray nonAxisSA = new StringArray();
        EDVTime timeVar = null;
        int dvTime = -1;
        for (int dv = 0; dv < dataVariables.length; dv++) {
            if (dataVariables[dv].destinationDataTypeClass() != String.class) {
                String dn = dataVariables[dv].destinationName();
                if        (dv == lonIndex)   {axisSA.add(dn); 
                } else if (dv == latIndex)   {axisSA.add(dn); 
                } else if (dv == altIndex)   {axisSA.add(dn); 
                } else if (dv == depthIndex) {axisSA.add(dn); 
                } else if (dv == timeIndex)  {axisSA.add(dn);  dvTime = sa.size(); 
                    timeVar = (EDVTime)dataVariables[dv]; 
                } else { 
                    nonAxisSA.add(dn);
                }

                if (dv != lonIndex && dv != latIndex)
                    nonLLSA.add(dn);

                sa.add(dn);
            }
        }
        String[] nonAxis = nonAxisSA.toArray(); nonAxisSA = null;
        String[] axis = axisSA.toArray();       axisSA = null;
        String[] nonLL = nonLLSA.toArray();     nonLLSA = null;
        String[] numDvNames = sa.toArray();
        sa.add(0, "");
        String[] numDvNames0 = sa.toArray();    sa = null;
        String[] dvNames0 = new String[dataVariableDestinationNames.length + 1]; //all, including string vars
        dvNames0[0] = "";
        System.arraycopy(dataVariableDestinationNames, 0, dvNames0, 1, dataVariableDestinationNames.length);

        //gather LLAT range
        StringBuilder llatRange = new StringBuilder();
        int llat[] = {lonIndex, latIndex, 
            altIndex >= 0? altIndex : depthIndex, 
            timeIndex};
        for (int llati = 0; llati < 4; llati++) {
            int dv = llat[llati];
            if (dv < 0) 
                continue;
            EDV edv = dataVariables[dv];
            if (Double.isNaN(edv.destinationMin()) && Double.isNaN(edv.destinationMax())) {
            } else {
                String tMin = Double.isNaN(edv.destinationMin())? "(?)" : 
                    llati == 3? edv.destinationMinString() : ""+((float)edv.destinationMin());
                String tMax = Double.isNaN(edv.destinationMax())? 
                    (llati == 3? "(now?)" : "(?)") : 
                    llati == 3? edv.destinationMaxString() : 
                        ""+((float)edv.destinationMax());
                llatRange.append(edv.destinationName() + "&nbsp;=&nbsp;" + 
                    tMin + "&nbsp;" + EDStatic.magRangeTo +"&nbsp;" + tMax + 
                    (llati == 0? "&deg;E" : llati == 1? "&deg;N" : "") +
                    ", ");
            }
        }
        String otherRows = "";
        if (llatRange.length() > 0) {
            llatRange.setLength(llatRange.length() - 2); //remove last ", "
            otherRows = "  <tr><td nowrap>" + EDStatic.magRange + ":&nbsp;</td>" +
                              "<td>" + llatRange + "</td></tr>\n";
        }


        //*** write the header
        OutputStream out = outputStreamSource.outputStream("UTF-8");
        Writer writer = new OutputStreamWriter(out, "UTF-8"); 
        HtmlWidgets widgets = new HtmlWidgets("", true, EDStatic.imageDirUrl(loggedInAs));
        writer.write(EDStatic.startHeadHtml(tErddapUrl,  
            title() + " - " + EDStatic.mag));
        writer.write("\n" + rssHeadLink(loggedInAs));
        writer.write("\n</head>\n");
        writer.write(EDStatic.startBodyHtml(loggedInAs));
        writer.write("\n");
        writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs))); //this is a link to a script
        writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
        try {
            writer.write(EDStatic.youAreHereWithHelp(loggedInAs, "tabledap", 
                EDStatic.mag, 
                EDStatic.magTableHtml));
            writeHtmlDatasetInfo(loggedInAs, writer, true, true, false, userDapQuery, otherRows);
            if (userDapQuery.length() == 0) 
                userDapQuery = defaultGraphQuery(); //after writeHtmlDatasetInfo and before parseUserDapQuery
            writer.write(HtmlWidgets.ifJavaScriptDisabled + "\n");

            //make the big table
            writer.write("&nbsp;\n"); //necessary for the blank line between the table (not <p>)
            writer.write(widgets.beginTable(0, 0, "")); 
            writer.write("<tr><td align=\"left\" valign=\"top\">\n"); 

            //begin the form
            writer.write(widgets.beginForm(formName, "GET", "", ""));

            if (debugMode) String2.log("respondToGraphQuery 2");

            //parse the query to get the constraints
            StringArray resultsVariables    = new StringArray();
            StringArray constraintVariables = new StringArray();
            StringArray constraintOps       = new StringArray();
            StringArray constraintValues    = new StringArray(); 
            int conPo;
            parseUserDapQuery(userDapQuery, resultsVariables,
                constraintVariables, constraintOps, constraintValues,  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
                true);
            //String2.log("conVars=" + constraintVariables);
            //String2.log("conOps=" + constraintOps);
            //String2.log("conVals=" + constraintValues);

            //some cleaning (needed by link from Subset)
            //remove if invalid or String
            for (int rv = resultsVariables.size() - 1; rv >= 0; rv--) {
                int dv = String2.indexOf(dataVariableDestinationNames(), resultsVariables.get(rv));
                if (dv < 0 ||
                    (dv >= 0 && dataVariables[dv].destinationDataTypeClass() == String.class)) 
                    resultsVariables.remove(rv);
            }

            //parse the query so &-separated parts are handy
            String paramName, paramValue, partName, partValue, pParts[];
            String queryParts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")
            //String2.log(">>queryParts=" + String2.toCSSVString(queryParts));

            //try to setup things for zoomLatLon
            //lon/lat/Min/Max reflect active lon/lat constraints (explicit &longitude>=, or implicit (click))
            double lonMin  = Double.NaN, lonMax  = Double.NaN;
            double latMin  = Double.NaN, latMax  = Double.NaN;
            double timeMin = Double.NaN, timeMax = Double.NaN;

            //maxExtentWESN is world-like limits (not data min/max)
            //??? how know if +/-180 or 0...360, or 0...400 (that's the real problem)???
            double maxExtentWESN[] = {-180, 400, -90, 90};  


            //remove &.click &.zoom from userDapQuery (but they are still in queryParts)
            //so userDapQuery is now the same one used to generate the image the user clicked or zoomed on.
            int clickPo = userDapQuery.indexOf("&.click=");
            if (clickPo >= 0)
                userDapQuery = userDapQuery.substring(0, clickPo);
            int zoomPo = userDapQuery.indexOf("&.zoom=");
            if (zoomPo >= 0)
                userDapQuery = userDapQuery.substring(0, zoomPo);

            Object pngInfo[] = null;
            double pngInfoDoubleWESN[] = null;
            int pngInfoIntWESN[] = null;
            //if user clicked or zoomed, read pngInfo file 
            //(if available, e.g., if graph is x=lon, y=lat and image recently created)
            if ((clickPo >= 0 || zoomPo >= 0) && lonIndex >= 0 && latIndex >= 0) {
                //okay if not available 
                pngInfo = readPngInfo(loggedInAs, userDapQuery, ".png");
                if (pngInfo != null) {
                    pngInfoDoubleWESN = (double[])pngInfo[0];
                    pngInfoIntWESN = (int[])pngInfo[1];
                }
            }


            //if lon min max is known, use it to adjust maxExtentWESN[0,1]
            if (lonIndex >= 0) {
                EDV edv = dataVariables[lonIndex];
                double dMin = edv.destinationMin();
                double dMax = edv.destinationMax();
                if (!Double.isNaN(dMin) && !Double.isNaN(dMax)) {
                    if (dMax > 360) {
                        double lh[] = Math2.suggestLowHigh(Math.min(dMin, 0), dMax);
                        maxExtentWESN[0] = lh[0]; 
                        maxExtentWESN[1] = lh[1]; 
                    } else if (dMax <= 180) {    //preference to this
                        maxExtentWESN[0] = -180; //data in 0..180 put here arbitrarily
                        maxExtentWESN[1] =  180; 
                    } else {
                        double lh[] = Math2.suggestLowHigh(Math.min(dMin, 0), Math.max(dMax, 360));
                        maxExtentWESN[0] = lh[0]; 
                        maxExtentWESN[1] = lh[1]; 
                    }
                }

                if (reallyVerbose) 
                    String2.log("  maxExtentWESN=" + String2.toCSSVString(maxExtentWESN));
            }

            //note explicit longitude/longitude/time>= <= constraints
            String tpStart;
            int tpi;
            //longitude
            tpStart = "longitude>=";
            tpi = String2.lineStartsWith(queryParts, tpStart);
            if (tpi < 0) {
                tpStart = "longitude>";
                tpi = String2.lineStartsWith(queryParts, tpStart);
            }
            if (tpi >= 0) {
                double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
                //ignore dVal isNaN, but fix if out of range
                if (dVal < maxExtentWESN[0]) {
                    dVal = maxExtentWESN[0];
                    queryParts[tpi] = tpStart + dVal;
                }
                lonMin = dVal; //may be NaN
            }
            tpStart = "longitude<=";
            tpi = String2.lineStartsWith(queryParts, tpStart);
            if (tpi < 0) {
                tpStart = "longitude<";
                tpi = String2.lineStartsWith(queryParts, tpStart);
            }
            if (tpi >= 0) {
                //special: fixup maxExtentWESN[0]    //this is still imperfect and may be always
                if (lonMin == -180)
                    maxExtentWESN[1] = 180;
                double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
                //ignore dVal isNaN, but fix if out of range
                if (dVal > maxExtentWESN[1]) {
                    dVal = maxExtentWESN[1];
                    queryParts[tpi] = tpStart + dVal;
                }
                lonMax = dVal; //may be NaN
            }
            //latitude
            tpStart = "latitude>=";
            tpi = String2.lineStartsWith(queryParts, tpStart);
            if (tpi < 0) {
                tpStart = "latitude>";
                tpi = String2.lineStartsWith(queryParts, tpStart);
            }
            if (tpi >= 0) {
                double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
                //ignore dVal isNaN, but fix if out of range
                if (dVal < maxExtentWESN[2]) {
                    dVal = maxExtentWESN[2];
                    queryParts[tpi] = tpStart + dVal;
                }
                latMin = dVal; //may be NaN
            }
            tpStart = "latitude<=";
            tpi = String2.lineStartsWith(queryParts, tpStart);
            if (tpi < 0) {
                tpStart = "latitude<";
                tpi = String2.lineStartsWith(queryParts, tpStart);
            }
            if (tpi >= 0) {
                double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
                //ignore dVal isNaN, but fix if out of range
                if (dVal > maxExtentWESN[3]) {
                    dVal = maxExtentWESN[3];
                    queryParts[tpi] = tpStart + dVal;
                }
                latMax = dVal; //may be NaN
            }

            if (pngInfoDoubleWESN == null &&
                !Double.isNaN(lonMin) && !Double.isNaN(lonMax) &&
                !Double.isNaN(latMin) && !Double.isNaN(latMax))
                pngInfoDoubleWESN = new double[]{lonMin, lonMax, latMin, latMax};

            //time
            tpStart = "time>=";
            tpi = String2.lineStartsWith(queryParts, tpStart);
            if (tpi < 0) {
                tpStart = "time>";
                tpi = String2.lineStartsWith(queryParts, tpStart);
            }
            if (tpi >= 0) {
                String sVal = queryParts[tpi].substring(tpStart.length());
                timeMin = sVal.startsWith("now")? Calendar2.safeNowStringToEpochSeconds(sVal, timeMin) :
                    Calendar2.isIsoDate(sVal)?  Calendar2.isoStringToEpochSeconds(sVal) : 
                    String2.parseDouble(sVal);  //may be NaN
            }
            tpStart = "time<=";
            tpi = String2.lineStartsWith(queryParts, tpStart);
            if (tpi < 0) {
                tpStart = "time<";
                tpi = String2.lineStartsWith(queryParts, tpStart);
            }
            if (tpi >= 0) {
                String sVal = queryParts[tpi].substring(tpStart.length());
                timeMax = sVal.startsWith("now")? Calendar2.safeNowStringToEpochSeconds(sVal, timeMax) :
                    Calendar2.isIsoDate(sVal)? Calendar2.isoStringToEpochSeconds(sVal) : 
                    String2.parseDouble(sVal);  //may be NaN
            }
            if (reallyVerbose)
                String2.log("  constraint lon>=" + lonMin + " <=" + lonMax +
                    "\n  constraint lat>=" + latMin + " <=" + latMax +
                    "\n  constraint time>=" + 
                        Calendar2.safeEpochSecondsToIsoStringTZ(timeMin, "NaN") + " <=" + 
                        Calendar2.safeEpochSecondsToIsoStringTZ(timeMax, "NaN"));


            //If user clicked on map, change lon/lat/Min/Max
            partValue = String2.stringStartsWith(queryParts, partName = ".click=?"); //? indicates user clicked on map
            if (partValue != null && pngInfo != null) {
                if (reallyVerbose)
                    String2.log("  processing " + partValue);
                try {
                    String xy[] = String2.split(partValue.substring(8), ','); //e.g., 24,116
                    double clickLonLat[] = SgtUtil.xyToLonLat(
                        String2.parseInt(xy[0]), String2.parseInt(xy[1]),
                        pngInfoIntWESN, pngInfoDoubleWESN, maxExtentWESN);
                    if (clickLonLat == null) {
                        if (verbose) String2.log("  clickLonLat is null!");
                    } else {
                        if (verbose) String2.log("  click center=" + clickLonLat[0] + ", " + clickLonLat[1]);
                        
                        //get current radius, and shrink if clickLonLat is closer to maxExtent
                        double radius = Math.max(pngInfoDoubleWESN[1] - pngInfoDoubleWESN[0], 
                                                 pngInfoDoubleWESN[3] - pngInfoDoubleWESN[2]) / 2;

                        int nPlaces = lonLatNPlaces(radius);
                        clickLonLat[0] = Math2.roundTo(clickLonLat[0], nPlaces);
                        clickLonLat[1] = Math2.roundTo(clickLonLat[1], nPlaces);

                        radius = Math.min(radius, clickLonLat[0] - maxExtentWESN[0]);
                        radius = Math.min(radius, maxExtentWESN[1] - clickLonLat[0]);
                        radius = Math.min(radius, clickLonLat[1] - maxExtentWESN[2]);
                        radius = Math.min(radius, maxExtentWESN[3] - clickLonLat[1]);
                        radius = Math2.roundTo(radius, nPlaces);
                        if (verbose) String2.log("  postclick radius=" + radius);

                        //if not too close to data's limits...  success
                        if (radius >= 0.01) {
                            lonMin = clickLonLat[0] - radius;
                            lonMax = clickLonLat[0] + radius;
                            latMin = clickLonLat[1] - radius;
                            latMax = clickLonLat[1] + radius;
                        }
                    }           
                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    String2.log("Error while trying to read &.click? value.\n" + 
                        MustBe.throwableToString(t));
                }
            }

            //If user clicked on a zoom button, change lon/lat/Min/Max
            partValue = String2.stringStartsWith(queryParts, partName = ".zoom="); 
            if (partValue != null && pngInfoDoubleWESN != null) {
                if (reallyVerbose)
                    String2.log("  processing " + partValue);
                try {
                    partValue = partValue.substring(6);
                    //use lonlatMinMax from pngInfo
                    lonMin = pngInfoDoubleWESN[0];
                    lonMax = pngInfoDoubleWESN[1];
                    latMin = pngInfoDoubleWESN[2];
                    latMax = pngInfoDoubleWESN[3];
                    double lonRange = lonMax - lonMin;
                    double latRange = latMax - latMin;
                    double lonCenter = (lonMin + lonMax) / 2; 
                    double latCenter = (latMin + latMax) / 2; 
                    double cRadius = Math.max(Math.abs(lonRange), Math.abs(latRange)) / 2; 
                    double zoomOut  = cRadius * 5 / 4;
                    double zoomOut2 = cRadius * 2;
                    double zoomOut8 = cRadius * 8;
                    double zoomIn   = cRadius * 4 / 5;
                    double zoomIn2  = cRadius / 2;
                    double zoomIn8  = cRadius / 8;
                    //using nPlaces = 3 makes zoomIn/Out cleanly repeatable (lonLatNPlaces doesn't)
                    int nPlaces = 3; //lonLatNPlaces(zoomOut8); 

                    double tZoom = 
                        partValue.equals("out8")? zoomOut8 :
                        partValue.equals("out2")? zoomOut2 :
                        partValue.equals("out" )? zoomOut : 0;
                    if (tZoom != 0) {
                        //if zoom out, keep ranges intact by moving tCenter to safe place
                        double tLonCenter, tLatCenter;
                        tLonCenter = Math.max( lonCenter, maxExtentWESN[0] + tZoom);
                        tLonCenter = Math.min(tLonCenter, maxExtentWESN[1] - tZoom);
                        tLatCenter = Math.max( latCenter, maxExtentWESN[2] + tZoom);
                        tLatCenter = Math.min(tLatCenter, maxExtentWESN[3] - tZoom);
                        lonMin = Math2.roundTo(tLonCenter - tZoom, nPlaces);
                        lonMax = Math2.roundTo(tLonCenter + tZoom, nPlaces);
                        latMin = Math2.roundTo(tLatCenter - tZoom, nPlaces);
                        latMax = Math2.roundTo(tLatCenter + tZoom, nPlaces); 
                        lonMin = Math.max(maxExtentWESN[0], lonMin);
                        lonMax = Math.min(maxExtentWESN[1], lonMax);
                        latMin = Math.max(maxExtentWESN[2], latMin);
                        latMax = Math.min(maxExtentWESN[3], latMax);

                    } else {
                        tZoom = 
                            partValue.equals("in8")? zoomIn8 :
                            partValue.equals("in2")? zoomIn2 :
                            partValue.equals("in" )? zoomIn : 0;
                        if (tZoom != 0) {
                            lonMin = Math2.roundTo(lonCenter - tZoom, nPlaces);
                            lonMax = Math2.roundTo(lonCenter + tZoom, nPlaces);
                            latMin = Math2.roundTo(latCenter - tZoom, nPlaces);
                            latMax = Math2.roundTo(latCenter + tZoom, nPlaces); 
                        }
                    }

                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    String2.log("Error while trying to process &.zoom=" + partValue + "\n" + 
                        MustBe.throwableToString(t));
                }
            }



            //if lon/lat/Min/Max were changed, change constraints that are affected
            boolean wesnFound[] = new boolean[4]; //default=all false
            for (int c = 0; c < constraintVariables.size(); c++) {
                String var = constraintVariables.get(c);
                String op  = constraintOps.get(c);
                if (var.equals("longitude")) {
                    if        (op.startsWith(">")) {
                        wesnFound[0] = true; 
                        if (!Double.isNaN(lonMin)) constraintValues.set(c, String2.genEFormat6(lonMin));
                    } else if (op.startsWith("<")) {
                        wesnFound[1] = true;
                        if (!Double.isNaN(lonMax)) constraintValues.set(c, String2.genEFormat6(lonMax));
                    }
                } else if (var.equals("latitude")) {
                    if        (op.startsWith(">")) {
                        wesnFound[2] = true;
                        if (!Double.isNaN(latMin)) constraintValues.set(c, String2.genEFormat6(latMin));
                    } else if (op.startsWith("<")) {
                        wesnFound[3] = true; 
                        if (!Double.isNaN(latMax)) constraintValues.set(c, String2.genEFormat6(latMax));
                    }
                }
            }
            //if user clicked on map when no lat lon constraints, I need to add lat lon constraints
            if (!wesnFound[0] && !Double.isNaN(lonMin)) {
                constraintVariables.add("longitude");
                constraintOps.add(      ">=");
                constraintValues.add(   String2.genEFormat6(lonMin));
            }
            if (!wesnFound[1] && !Double.isNaN(lonMax)) {
                constraintVariables.add("longitude");
                constraintOps.add(      "<=");
                constraintValues.add(   String2.genEFormat6(lonMax));
            }
            if (!wesnFound[2] && !Double.isNaN(latMin)) {
                constraintVariables.add("latitude");
                constraintOps.add(      ">=");
                constraintValues.add(   String2.genEFormat6(latMin));
            }
            if (!wesnFound[3] && !Double.isNaN(latMax)) {
                constraintVariables.add("latitude");
                constraintOps.add(      "<=");
                constraintValues.add(   String2.genEFormat6(latMax));
            }

            //* write all of the distinct options arrays            
            String distinctOptions[][] = null;
            if (accessibleViaSubset().length() == 0) {
                distinctOptions = distinctOptionsTable(loggedInAs, 60); //maxChar=60
                writer.write(
                    "<script type=\"text/javascript\" language=\"JavaScript\">\n" +
                    "var distinctOptions=[\n");
                for (int sv = 0; sv < subsetVariables.length; sv++) {
                    //encodeSpaces as nbsp solves the problem with consecutive internal spaces
                    int ndo = distinctOptions[sv].length;
                    for (int doi = 0; doi < ndo; doi++)
                        distinctOptions[sv][doi] = XML.minimalEncodeSpaces(distinctOptions[sv][doi]);

                    writer.write(" [");
                    writer.write((new StringArray(distinctOptions[sv])).toJsonCsvString());
                    writer.write("]" + (sv == subsetVariables.length - 1? "" : ",") + "\n");
                }           
                writer.write(
                    "];\n" +
                    "</script>\n"); 
            }

            //** write the table
            StringBuilder graphQuery = new StringBuilder();
            StringBuilder graphQueryNoLatLon = new StringBuilder();
            StringBuilder graphQueryNoTime = new StringBuilder();
            String gap = "&nbsp;&nbsp;&nbsp;";

            //set the Graph Type    //eeek! what if only 1 var?
            StringArray drawsSA = new StringArray();
            //it is important for javascript below that first 3 options are the very similar (L L&M M)
            drawsSA.add("lines");  
            drawsSA.add("linesAndMarkers"); 
            drawsSA.add("markers");  int defaultDraw = 2;
            if (axis.length >= 1 && nonAxis.length >= 2) 
                drawsSA.add("sticks");
            if (lonIndex >= 0 && latIndex >= 0 && nonAxis.length >= 2) 
                drawsSA.add("vectors");
            String draws[] = drawsSA.toArray();
            int draw = defaultDraw;
            partValue = String2.stringStartsWith(queryParts, partName = ".draw=");
            if (partValue != null) {
                draw = String2.indexOf(draws, partValue.substring(partName.length()));
                if (draw < 0)
                    draw = defaultDraw;
            }
            boolean drawLines = draws[draw].equals("lines");
            boolean drawLinesAndMarkers = draws[draw].equals("linesAndMarkers");
            boolean drawMarkers = draws[draw].equals("markers");
            boolean drawSticks  = draws[draw].equals("sticks");
            boolean drawVectors = draws[draw].equals("vectors");
            writer.write(widgets.beginTable(0, 0, "")); 
            paramName = "draw";
            writer.write(
                "<tr>\n" +
                "  <td nowrap><b>" + EDStatic.magGraphType + ":&nbsp;</b>" + 
                "  </td>\n" +
                "  <td>\n");
            writer.write(widgets.select(paramName, "", 
                1, draws, draw, 
                "onChange='mySubmit(" +  //change->submit so form always reflects graph type
                (draw < 3? "document.f1.draw.selectedIndex<3" : //if old and new draw are <3, do send var names
                    "false") + //else don't send var names
                ");'")); 
            writer.write(
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.magGraphTypeTooltipTable) +
                "  </td>\n" +
                "</tr>\n");
            if (debugMode) String2.log("respondToGraphQuery 3");

            //heuristic: look for two adjacent nonAxis vars that have same units
            int sameUnits1 = 0; //default = first nonAxis var
            String units1 = null;
            String units2 = findDataVariableByDestinationName(nonAxis[0]).units();
            for (int sameUnits2 = 1; sameUnits2 < nonAxis.length; sameUnits2++) {
                units1 = units2;
                units2 = findDataVariableByDestinationName(nonAxis[sameUnits2]).units(); 
                if (units1 != null && units2 != null && units1.equals(units2)) {
                    sameUnits1 = sameUnits2 - 1;
                    //String2.log("useDefaultVars sameUnits1=" + sameUnits1;
                    break;
                }
            }

            //if no userDapQuery, set default variables
            boolean useDefaultVars = userDapQuery.length() == 0 || userDapQuery.startsWith("&");
            if (useDefaultVars) 
                resultsVariables.clear(); //parse() set resultsVariables to all variables

            //set draw-related things
            int nVars = -1, nonAxisPo = 0;
            String varLabel[], varHelp[], varOptions[][];
            if (reallyVerbose)
                String2.log("pre  set draw-related; resultsVariables=" + resultsVariables.toString());
            if (drawLines) {
                nVars = 2;
                varLabel = new String[]{EDStatic.magAxisX + ":", EDStatic.magAxisY + ":"};
                varHelp  = new String[]{
                    EDStatic.magAxisHelpGraphX, EDStatic.magAxisHelpGraphY};
                varOptions = new String[][]{numDvNames, numDvNames};
                if (useDefaultVars || resultsVariables.size() < 2) {
                    resultsVariables.clear();
                    if (lonIndex >= 0 && latIndex >= 0) {
                        resultsVariables.add(EDV.LON_NAME);
                        resultsVariables.add(EDV.LAT_NAME);
                    } else if (timeIndex >= 0 && nonAxis.length >= 1) {
                        resultsVariables.add(EDV.TIME_NAME);
                        resultsVariables.add(nonAxis[0]);
                    } else if (nonAxis.length >= 2) {
                        resultsVariables.add(nonAxis[0]);
                        resultsVariables.add(nonAxis[1]);
                    } else {
                        resultsVariables.add(numDvNames[0]);
                        if (numDvNames.length > 0) resultsVariables.add(numDvNames[1]);
                    }
                }
            } else if (drawLinesAndMarkers || drawMarkers) {
                nVars = 3;
                varLabel = new String[]{EDStatic.magAxisX + ":", EDStatic.magAxisY + ":", 
                    EDStatic.magAxisColor + ":"};
                varHelp  = new String[]{
                    EDStatic.magAxisHelpGraphX, EDStatic.magAxisHelpGraphY,
                    EDStatic.magAxisHelpMarkerColor};
                varOptions = new String[][]{numDvNames, numDvNames, numDvNames0};
                if (useDefaultVars || resultsVariables.size() < 2) {
                    resultsVariables.clear();
                    if (lonIndex >= 0 && latIndex >= 0 && nonAxis.length >= 1) {
                        resultsVariables.add(EDV.LON_NAME);
                        resultsVariables.add(EDV.LAT_NAME);
                        resultsVariables.add(nonAxis[0]);
                    } else if (timeIndex >= 0 && nonAxis.length >= 1) {
                        resultsVariables.add(EDV.TIME_NAME);
                        resultsVariables.add(nonAxis[0]);
                    } else if (nonAxis.length >= 2) {
                        resultsVariables.add(nonAxis[0]);
                        resultsVariables.add(nonAxis[1]);
                    } else {
                        resultsVariables.add(numDvNames[0]);
                        if (numDvNames.length > 0) resultsVariables.add(numDvNames[1]);
                    }
                }
            } else if (drawSticks) {
                nVars = 3;
                varLabel = new String[]{EDStatic.magAxisX + ":", EDStatic.magAxisStickX + ":",
                    EDStatic.magAxisStickY + ":"};
                varHelp  = new String[]{
                    EDStatic.magAxisHelpGraphX, 
                    EDStatic.magAxisHelpStickX, EDStatic.magAxisHelpStickY};
                varOptions = new String[][]{numDvNames, numDvNames, numDvNames};
                if (useDefaultVars || resultsVariables.size() < 3) {
                    //x var is an axis (prefer: time)
                    resultsVariables.clear();
                    resultsVariables.add(timeIndex >= 0? EDV.TIME_NAME : axis[0]);
                    resultsVariables.add(nonAxis[sameUnits1]);
                    resultsVariables.add(nonAxis[sameUnits1 + 1]);
                }
            } else if (drawVectors) {
                nVars = 4;
                varLabel = new String[]{EDStatic.magAxisX + ":", EDStatic.magAxisY + ":", 
                    EDStatic.magAxisVectorX + ":", EDStatic.magAxisVectorY + ":"};
                varHelp  = new String[]{
                    EDStatic.magAxisHelpMapX, EDStatic.magAxisHelpMapY,
                    EDStatic.magAxisHelpVectorX, EDStatic.magAxisHelpVectorY};
                varOptions = new String[][]{new String[]{EDV.LON_NAME}, new String[]{EDV.LAT_NAME}, nonAxis, nonAxis};
                if (useDefaultVars || resultsVariables.size() < 4) {
                    resultsVariables.clear();
                    resultsVariables.add(EDV.LON_NAME);
                    resultsVariables.add(EDV.LAT_NAME);
                    resultsVariables.add(nonAxis[sameUnits1]);
                    resultsVariables.add(nonAxis[sameUnits1 + 1]);
                }
            } else throw new SimpleException(EDStatic.errorInternal +
                "No drawXxx value was set.");
            if (debugMode) String2.log("respondToGraphQuery 4");
            if (reallyVerbose)
                String2.log("post set draw-related; resultsVariables=" + resultsVariables.toString());

            //pick variables
            for (int v = 0; v < nVars; v++) {
                String tDvNames[] = varOptions[v];
                int vi = v >= resultsVariables.size()? 0 :
                    Math.max(0, String2.indexOf(tDvNames, resultsVariables.get(v)));
                if (v >= resultsVariables.size()) 
                     resultsVariables.add(tDvNames[vi]);
                else resultsVariables.set(v, tDvNames[vi]);
                paramName = "var" + v;
                writer.write(
                    "<tr>\n" +
                    "  <td nowrap>" + varLabel[v] + "&nbsp;" +
                    "  </td>\n" +
                    "  <td>\n");
                writer.write(widgets.select(paramName, 
                    MessageFormat.format(EDStatic.magAxisVarHelp, varHelp[v]), 
                    1, tDvNames, vi, ""));
                writer.write(
                    "  </td>\n" +
                    "</tr>\n");
            }
            boolean zoomLatLon = 
                resultsVariables.get(0).equals(EDV.LON_NAME) &&
                resultsVariables.get(1).equals(EDV.LAT_NAME);

            //end of variables table
            writer.write(widgets.endTable());

            if (debugMode) String2.log("respondToGraphQuery 5");

            //*** write the constraint table
            writer.write("&nbsp;\n"); //necessary for the blank line before the table (not <p>)
            writer.write(widgets.beginTable(0, 0, "")); 
            writer.write(
                "<tr>\n" +
                "  <th nowrap align=\"left\">" + 
                    EDStatic.EDDTableConstraints + " " +
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.magConstraintHelp) +
                "    &nbsp;</th>\n" +
                "  <th nowrap align=\"center\" colspan=\"2\">" + 
                    EDStatic.EDDTableOptConstraint1Html + " " + 
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableConstraintHtml) + "</th>\n" +
                "  <th nowrap align=\"center\" colspan=\"2\">" + 
                    EDStatic.EDDTableOptConstraint2Html + " " + 
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDTableConstraintHtml) + "</th>\n" +
                "</tr>\n");

            //a row for each constrained variable
            int nConRows = 5;
            int conVar[] = new int[nConRows];
            int conOp[][] = new int[nConRows][2];
            String conVal[][] = new String[nConRows][2]; //initially nulls
            //INACTIVE: StringBuilder onloadSB = new StringBuilder();
            for (int cv = 0; cv < nConRows; cv++) {
                writer.write("<tr>\n"); 
                String tConVala = "";

                //get constraints from query
                //!!! since each constraintVar/Op/Val is removed after processing, 
                //    always work on constraintVar/Op/Val #0
                if (constraintVariables.size() > 0) { 
                    //parse() ensured that all constraintVariables and Ops are valid 
                    conVar[cv]    = String2.indexOf(dvNames0, constraintVariables.get(0));  //yes, 0 (see above)
                    boolean use1  = constraintOps.get(0).startsWith("<");
                    conOp[ cv][use1? 1 : 0] = String2.indexOf(OPERATORS, constraintOps.get(0)); 
                    conVal[cv][use1? 1 : 0] = constraintValues.get(0);    
                    constraintVariables.remove(0); //yes, 0 (see above)
                    constraintOps.remove(0); 
                    constraintValues.remove(0);
                    //another constraint for same var?
                    conPo = constraintVariables.indexOf(dvNames0[conVar[cv]]);
                    if (conPo >= 0) {
                        conOp[ cv][use1? 0 : 1] = String2.indexOf(OPERATORS, constraintOps.get(conPo)); 
                        conVal[cv][use1? 0 : 1] = constraintValues.get(conPo);    
                        constraintVariables.remove(conPo);
                        constraintOps.remove(conPo); 
                        constraintValues.remove(conPo);
                    } else {
                        conOp[ cv][use1? 0 : 1] = opDefaults[use1? 0 : 1];
                        conVal[cv][use1? 0 : 1] = null;  //still
                    }
                } else { //else blanks
                    conVar[cv] = 0;
                    conOp[cv][0] = opDefaults[0]; 
                    conOp[cv][1] = opDefaults[1];
                    conVal[cv][0] = null;  //still
                    conVal[cv][1] = null;  //still
                }
                EDV conEdv = null;
                if (conVar[cv] > 0)  // >0 since option 0=""
                    conEdv = dataVariables()[conVar[cv] - 1];

                for (int con = 0; con < 2; con++) {
                    String ab = con == 0? "a" : "b";

                    //ensure conVal[cv][con] is valid
                    if (conVal[cv][con] == null) {
                    } else if (conVal[cv][con].length() > 0) {
                        if (conEdv != null && conEdv instanceof EDVTimeStamp) { 
                            //convert numeric time (from parseUserDapQuery) to iso
                            conVal[cv][con] = ((EDVTimeStamp)conEdv).destinationToString(
                                String2.parseDouble(conVal[cv][con]));
                        } else if (dataVariables[conVar[cv] - 1].destinationDataTypeClass() == String.class) { 
                            //-1 above since option 0=""
                            //leave String constraint values intact
                        } else if (conVal[cv][con].toLowerCase().equals("nan")) { //NaN  (or clean up to "NaN")
                            conVal[cv][con] = "NaN"; 
                        } else if (Double.isNaN(String2.parseDouble(conVal[cv][con]))) { 
                            //it should be numeric, but NaN, set to null
                            conVal[cv][con] = null; 
                        //else leave as it
                        }
                    }

                    if (cv == 0 && userDapQuery.length() == 0 && timeIndex >= 0) {
                        //suggest a time constraint: last week's worth of data
                        conVar[cv] = timeIndex + 1; // +1 since option 0=""
                        conEdv = dataVariables()[conVar[cv] - 1];
                        double d = Calendar2.backNDays(-1, conEdv.destinationMax()); //coming midnight
                        if (con == 0) 
                            d = Calendar2.backNDays(7, d); 
                        conVal[cv][con] = Calendar2.epochSecondsToLimitedIsoStringT(
                            conEdv.combinedAttributes().getString(EDV.time_precision), d, "");
                        if (con == 0) timeMin = d;
                        else timeMax = d;
                    }

                    //variable list
                    if (con == 0) {
                        //make javascript
                        StringBuilder cjs = null;
                        if (distinctOptions != null) {
                            cjs = new StringBuilder(1024);
                            //The constraint variable onChange causes the distinct options row to be updated
                            cjs.append("onChange='" +
                                //make the row and select widget invisible
                                " document.body.style.cursor=\"wait\";" + //no effect in Firefox
                                " var tDisRow=document.getElementById(\"disRow" + cv + "\");" + 
                                " tDisRow.style.display=\"none\";" +  //hides/closes up the row      
                                " var tDis=document.getElementById(\"dis" + cv + "\");" + 
                                " tDis.selected=0;" +
                                " tDis.options.length=0;" +
                                " document." + formName + ".cVal" + cv + "a.value=\"\";" + 
                                " document." + formName + ".cVal" + cv + "b.value=\"\";" +
                                //is newly selected var a subsetVar?
                                " var vName=this.options[this.selectedIndex].text;" +
                                " var sv=-1;" +
                                " if (vName==\"" + subsetVariables[0] + "\") sv=0;"); 
                            for (int sv = 1; sv < subsetVariables.length; sv++) 
                                cjs.append(
                                " else if (vName==\"" + subsetVariables[sv] + "\") sv=" + sv + ";"); 
                            cjs.append(
                                //change the operator
                                " document." + formName + ".cOp"  + cv + "a.selectedIndex=" + 
                                    "(sv>=0?" + String2.indexOf(OPERATORS, "=")  + ":" +
                                                String2.indexOf(OPERATORS, ">=") + ");" +
                                " if (sv>=0) {" +
                                   //clear distinct values, populate select widget, and make tDisRow visible
                                "  var n=distinctOptions[sv].length;" +
                                "  var tDisOptions = tDis.options;" + //as few lookups as possible in loop
                                "  var dosv = distinctOptions[sv];" + //as few lookups as possible in loop
                                "  for (var i=0; i<n; i++) tDisOptions.add(new Option(dosv[i]));" + //[i]faster than add() in FF, but unique_tag_id not sorted!
                                "  tDisRow.style.display=\"\";" + //make it visible: "" defaults to "block"; don't use "block" to avoid problems in firefox
                                "}" +
                                " document.body.style.cursor=\"default\";" +  //no effect in Firefox
                                "'");
                        }

                        writer.write("  <td nowrap>");
                        writer.write(widgets.select("cVar" + cv, 
                            EDStatic.EDDTableOptConstraintVar, 
                            1, dvNames0, conVar[cv], cjs == null? "" : cjs.toString()));
                        writer.write("  &nbsp;</td>\n");
                    }

                    //make the constraintTooltip
                    //ConstraintHtml below is worded in generic way so works with number and time, too
                    String constraintTooltip = EDStatic.EDDTableStringConstraintHtml; 
                    //String2.log("cv=" + cv + " conVar[cv]=" + conVar[cv]);
                    if (conEdv != null && conEdv.destinationMinString().length() > 0) {
                        constraintTooltip += "<br>&nbsp;<br>" + 
                            MessageFormat.format(EDStatic.rangesFromTo, 
                                conEdv.destinationName(), 
                                conEdv.destinationMinString(),
                                conEdv.destinationMaxString().length() > 0? 
                                    conEdv.destinationMaxString() : "(?)");    
                    }

                    //display the op and value
                    int tOp = conOp[cv][con];
                    writer.write("  <td nowrap>" + gap);
                    writer.write(widgets.select("cOp" + cv + ab, EDStatic.EDDTableSelectAnOperator, 
                        1, OPERATORS, tOp, ""));
                    writer.write("  </td>\n");
                    String tVal = conVal[cv][con];
                    if (tVal == null)
                        tVal = "";
                    else if (tOp == REGEX_OP_INDEX || 
                        (conEdv != null && conEdv.destinationDataTypeClass() == String.class)) {
                        tVal = String2.toJson(tVal); //enclose in "
                    }
                    if (con == 0)
                        tConVala = tVal;
                    writer.write("  <td nowrap>");
                    writer.write(widgets.textField("cVal" + cv + ab, constraintTooltip,                    
                        18, 255, tVal, ""));
                    writer.write("  </td>\n");

                    //add to graphQuery
                    if (conVar[cv] > 0 && tVal.length() > 0) {
                        String tq = "&" + dvNames0[conVar[cv]] + 
                            PPE_OPERATORS[conOp[cv][con]] + SSR.minimalPercentEncode(tVal);
                        graphQuery.append(tq);
                        if (!(conEdv instanceof EDVLat) &&
                            !(conEdv instanceof EDVLon))
                            graphQueryNoLatLon.append(tq);
                        if (!(conEdv instanceof EDVTime))
                            graphQueryNoTime.append(tq);
                    }

                }

                //end of row
                writer.write("</tr>\n");

                //Display distinctOptions?
                if (distinctOptions != null) {
                    int whichSV = conEdv == null? -1 : 
                        String2.indexOf(subsetVariables, conEdv.destinationName());

                    writer.write(
                        "<tr id=\"disRow" + cv + "\" " +
                            (whichSV < 0? "style=\"display:none;\" " : "") + //hide the row?
                            ">\n" +
                        "  <td>&nbsp;</td>\n" +
                        "  <td>&nbsp;</td>\n" +
                        "  <td colspan=\"3\">\n" +
                        "    " + widgets.beginTable(0, 0, "") +
                        "    <tr>\n" +
                        "      <td>");
                    writer.write(
                        widgets.select("dis" + cv, EDStatic.EDDTableSelectConstraint,
                            1, 
                            whichSV < 0? new String[]{""} : distinctOptions[whichSV], 
                            whichSV < 0? 0 : 
                                Math.max(0, String2.indexOf(distinctOptions[whichSV], 
                                    XML.minimalEncodeSpaces(tConVala))),
                            //INACTIVE. 2 lines above were:
                            //  new String[]{""}, 0, //initially, just the 0="" element to hold place
                            //onChange pushes selected value to textfield above
                            "id=\"dis" + cv + "\" onChange='" +
                              " document." + formName + ".cVal" + cv + "a.value=this.options[this.selectedIndex].text;" +
                              "'"));
                            //not encodeSpaces=true here because all distinctOptions[] were encoded above

                    /* INACTIVE. I used to load the distinctOptions below. But it is much faster to just load the select normally. 
                        (test with cPostSurg3, unique_tag_id)
                    if (whichSV >= 0) {
                        //populate dis here (instead of above, so info only transmitted once)
                        onloadSB.append(
                            "\n" +
                            " cDis=document." + formName + ".dis" + cv + ";" + //as few lookups as possible in loop
                            " cDisOptions=cDis.options;" +                     //as few lookups as possible in loop
                            " dosv=distinctOptions[" + whichSV + "];" +        //as few lookups as possible in loop
                            " for (var i=1; i<" + distinctOptions[whichSV].length + "; i++)" +  //0 already done
                            "  cDisOptions.add(new Option(dosv[i]));\n" +  //[i]= is faster than add() in FF, but not sorted order!
                            " cDis.selectedIndex=" + (Math.max(0, String2.indexOf(distinctOptions[whichSV], tConVala))) + ";" + //Math.max causes not found -> 0=""
                            " document.getElementById(\"disRow" + cv + "\").style.display=\"\";");  //show the row                              
                    } */
                    //write buttons
                    writer.write(
                        "      </td>\n" +

                        "      <td>\n" +
                        "<img src=\"" + widgets.imageDirUrl + "minus.gif\"\n" +
                        "  " + widgets.completeTooltip(EDStatic.magItemPrevious) +
                        "  alt=\"-\" " + 
                        //onMouseUp works much better than onClick and onDblClick
                        "  onMouseUp=\"\n" +
                        "   var sel=document." + formName + ".dis" + cv + ";\n" +
                        "   if (sel.selectedIndex>1) {\n" + //don't go to item 0 (it would lose the variable selection)
                        "    document." + formName + ".cVal" + cv + "a.value=sel.options[--sel.selectedIndex].text;\n" +
                        "    mySubmit(true);\n" +
                        "   }\" >\n" + 
                        "      </td>\n" +

                        "      <td>\n" +
                        "<img src=\"" + widgets.imageDirUrl + "plus.gif\"\n" +
                        "  " + widgets.completeTooltip(EDStatic.magItemNext) +
                        "  alt=\"+\" " + 
                        //onMouseUp works much better than onClick and onDblClick
                        "  onMouseUp=\"\n" +
                        "   var sel=document." + formName + ".dis" + cv + ";\n" +
                        "   if (sel.selectedIndex<sel.length-1) {\n" + //no action if at last item
                        "    document." + formName + ".cVal" + cv + "a.value=sel.options[++sel.selectedIndex].text;\n" +
                        "    mySubmit(true);\n" +
                        "   }\" >\n" + 
                        "      </td>\n" +

                        "    </tr>\n" +
                        "    " + widgets.endTable() +
                        "  </td>\n" +
                        "</tr>\n");
                }

            }
            if (debugMode) String2.log("respondToGraphQuery 6");

            //end of constraint table
            writer.write(widgets.endTable());

            //INACTIVE. I used to load the distinctOptions here.
            //But it is much faster to just load the select normally.
            //  (test with cPostSurg3, unique_tag_id)
            //write out the onload code
            //this way, loading options is unobtrusive 
            //  (try with cPostSurg3 unique_tag_id as a constraint)
            /*if (onloadSB.length() > 0) {
                writer.write(
                    "<script type=\"text/javascript\" language=\"JavaScript\">\n" +
                    "window.onload=function(){\n" + 
                    " document.body.style.cursor=\"wait\";" + //no effect in Firefox
                    " var cDis, cDisOptions, dosv;\n");
                writer.write(
                    onloadSB.toString());
                writer.write(
                    " document.body.style.cursor=\"default\";" + //no effect in Firefox
                    "}\n" +
                    "</script>\n");
            }*/

            //*** function options
            int nOrderBy = 4; //to be narrower than Data Access Form
            writeFunctionHtml(loggedInAs, queryParts, writer, widgets, nOrderBy);
            if (String2.indexOf(queryParts, "distinct()") >= 0) {
                graphQuery.append("&distinct()");
                graphQueryNoLatLon.append("&distinct()");
                graphQueryNoTime.append("&distinct()");
            }
            for (int i = 0; i < 2; i++) {  //don't translate
                String start = i == 0? "orderBy(\"" : "orderByMax(\"";
                String obPart = String2.stringStartsWith(queryParts, start);
                if (obPart == null || !obPart.endsWith("\")")) 
                    continue;
                StringArray names = StringArray.wordsAndQuotedPhrases(
                    obPart.substring(start.length(), obPart.length() - 2));
                StringArray goodNames = new StringArray();
                for (int n = 0; n < names.size(); n++) {
                    if (String2.indexOf(dataVariableDestinationNames, names.get(n)) < 0)
                        continue;
                    goodNames.add(names.get(n));
                    if (resultsVariables.indexOf(names.get(n)) < 0)
                        resultsVariables.add(names.get(n));
                }
                if (goodNames.size() > 0) {
                    String tq = "&" + 
                        start.substring(0, start.length() - 1) + "%22" + 
                        String2.replaceAll(goodNames.toString(), " ", "") + 
                        "%22)";
                    graphQuery.append(tq);
                    graphQueryNoLatLon.append(tq);
                    graphQueryNoTime.append(tq);
                }
            }

            //add resultsVariables to graphQuery
            while (true) {
                int blank = resultsVariables.indexOf("");
                if (blank < 0)
                    break;
                resultsVariables.remove(blank);
            }
            graphQuery.insert(        0, String2.replaceAll(resultsVariables.toString(), " ", ""));
            graphQueryNoLatLon.insert(0, String2.replaceAll(resultsVariables.toString(), " ", ""));
            graphQueryNoTime.insert(  0, String2.replaceAll(resultsVariables.toString(), " ", ""));

            //*** Graph Settings
            //add draw to graphQuery
            graphQuery.append(        "&.draw=" + draws[draw]); 
            graphQueryNoLatLon.append("&.draw=" + draws[draw]); 
            graphQueryNoTime.append(  "&.draw=" + draws[draw]); 

            writer.write("&nbsp;\n"); //necessary for the blank line before the table (not <p>)
            writer.write(widgets.beginTable(0, 0, "")); 
            writer.write("  <tr><th align=\"left\" colspan=\"2\" nowrap>" +
                EDStatic.magGS + "</th></tr>\n");

            //get Marker settings
            int mType = -1, mSize = -1;
            String mSizes[] = {"3", "4", "5", "6", "7", "8", "9", "10", "11", 
                "12", "13", "14", "15", "16", "17", "18"};
            if (drawLinesAndMarkers || drawMarkers) {
                partValue = String2.stringStartsWith(queryParts, partName = ".marker=");
                if (partValue != null) {
                    pParts = String2.split(partValue.substring(partName.length()), '|');
                    if (pParts.length > 0) mType = String2.parseInt(pParts[0]);
                    if (pParts.length > 1) mSize = String2.parseInt(pParts[1]); //the literal, not the index
                    if (reallyVerbose)
                        String2.log("  .marker type=" + mType + " size=" + mSize);
                }

                //markerType
                paramName = "mType";
                if (mType < 0 || mType >= GraphDataLayer.MARKER_TYPES.length)
                    mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
                //if (varName[2].length() > 0 && 
                //    GraphDataLayer.MARKER_TYPES[mType].toLowerCase().indexOf("filled") < 0) 
                //    mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE; //needs "filled" marker type
                writer.write("  <tr>\n" +
                             "    <td nowrap>" + EDStatic.magGSMarkerType + ":&nbsp;</td>\n" +
                             "    <td nowrap>");
                writer.write(widgets.select(paramName, "", 
                    1, GraphDataLayer.MARKER_TYPES, mType, ""));

                //markerSize
                paramName = "mSize";
                mSize = String2.indexOf(mSizes, "" + mSize); //convert from literal 3.. to index in mSizes[0..]
                if (mSize < 0)
                    mSize = String2.indexOf(mSizes, "" + GraphDataLayer.MARKER_SIZE_SMALL);
                writer.write(gap + EDStatic.magGSSize + ": ");
                writer.write(widgets.select(paramName, "", 1, mSizes, mSize, ""));
                writer.write("    </td>\n" +
                             "  </tr>\n");

                //add to graphQuery
                graphQuery.append(        "&.marker=" + mType + "|" + mSizes[mSize]);
                graphQueryNoLatLon.append("&.marker=" + mType + "|" + mSizes[mSize]);
                graphQueryNoTime.append(  "&.marker=" + mType + "|" + mSizes[mSize]);
            }
            if (debugMode) String2.log("respondToGraphQuery 7");

            //color
            paramName = "colr";
            String colors[] = HtmlWidgets.PALETTE17;
            partValue = String2.stringStartsWith(queryParts, partName = ".color=0x");
            int colori = String2.indexOf(colors, 
                partValue == null? "" : partValue.substring(partName.length()));
            if (colori < 0)
                colori = String2.indexOf(colors, "000000");
            writer.write("  <tr>\n" +
                         "    <td nowrap>" + EDStatic.magGSColor + ":&nbsp;</td>\n" +
                         "    <td nowrap>");
            writer.write(widgets.color17("", paramName, "", colori, ""));
            writer.write("    </td>\n" +
                         "  </tr>\n");

            //add to graphQuery
            graphQuery.append(        "&.color=0x" + HtmlWidgets.PALETTE17[colori]);
            graphQueryNoLatLon.append("&.color=0x" + HtmlWidgets.PALETTE17[colori]);
            graphQueryNoTime.append(  "&.color=0x" + HtmlWidgets.PALETTE17[colori]);

            //color bar
            int palette = 0, continuous = 0, scale = 0, pSections = 0;
            String palMin = "", palMax = "";
            if (drawLinesAndMarkers || drawMarkers) {
                partValue = String2.stringStartsWith(queryParts, partName = ".colorBar=");
                pParts = partValue == null? new String[0] : String2.split(partValue.substring(partName.length()), '|');
                if (reallyVerbose)
                    String2.log("  .colorBar=" + String2.toCSSVString(pParts));

                //find dataVariable relevant to colorBar
                //(force change in values if this var changes?  but how know, since no state?)
                int tDataVariablePo = String2.indexOf(dataVariableDestinationNames(), 
                    resultsVariables.size() > 2? resultsVariables.get(2) : ""); //currently, only relevant representation uses #2 for "Color"
                EDV tDataVariable = tDataVariablePo >= 0? dataVariables[tDataVariablePo] : null;

                paramName = "p";
                String defaultPalette = ""; 
                palette = Math.max(0, 
                    String2.indexOf(EDStatic.palettes0, pParts.length > 0? pParts[0] : defaultPalette));
                writer.write("  <tr>\n" +
                             "    <td colspan=\"2\" nowrap>" + EDStatic.magGSColorBar + ": ");
                writer.write(widgets.select(paramName, EDStatic.magGSColorBarTooltip, 
                    1, EDStatic.palettes0, palette, ""));

                String conDisAr[] = new String[]{"", "Continuous", "Discrete"};
                paramName = "pc";
                continuous = pParts.length > 1? (pParts[1].equals("D")? 2 : pParts[1].equals("C")? 1 : 0) : 0;
                writer.write(gap + EDStatic.magGSContinuity + ": ");
                writer.write(widgets.select(paramName, EDStatic.magGSContinuityTooltip, 
                    1, conDisAr, continuous, ""));

                paramName = "ps";
                String defaultScale = "";
                scale = Math.max(0, String2.indexOf(EDV.VALID_SCALES0, pParts.length > 2? pParts[2] : defaultScale));
                writer.write(gap + EDStatic.magGSScale + ": ");
                writer.write(widgets.select(paramName, EDStatic.magGSScaleTooltip, 
                    1, EDV.VALID_SCALES0, scale, ""));
                writer.write(
                    "    </td>\n" +
                    "  </tr>\n");

                paramName = "pMin";
                String defaultMin = "";
                palMin = pParts.length > 3? pParts[3] : defaultMin;
                writer.write(
                    "  <tr>\n" +
                    "    <td colspan=\"2\" nowrap> " + gap + gap + EDStatic.magGSMin + ": ");
                writer.write(widgets.textField(paramName, EDStatic.magGSMinTooltip, 
                   10, 40, palMin, ""));

                paramName = "pMax";
                String defaultMax = "";
                palMax = pParts.length > 4? pParts[4] : defaultMax;
                writer.write(gap + EDStatic.magGSMax + ": ");
                writer.write(widgets.textField(paramName, EDStatic.magGSMaxTooltip, 
                   10, 40, palMax, ""));

                paramName = "pSec";
                pSections = Math.max(0, String2.indexOf(EDStatic.paletteSections, pParts.length > 5? pParts[5] : ""));
                writer.write(gap + EDStatic.magGSNSections + ": ");
                writer.write(widgets.select(paramName, EDStatic.magGSNSectionsTooltip, 
                    1, EDStatic.paletteSections, pSections, ""));
                writer.write("    </td>\n" +
                             "  </tr>\n");

                //add to graphQuery
                String tq = 
                    "&.colorBar=" + EDStatic.palettes0[palette] + "|" +
                    (conDisAr[continuous].length() == 0? "" : conDisAr[continuous].charAt(0)) + "|" +
                    EDV.VALID_SCALES0[scale] + "|" +
                    palMin + "|" + palMax + "|" + EDStatic.paletteSections[pSections];
                graphQuery.append(        tq);
                graphQueryNoLatLon.append(tq);
                graphQueryNoTime.append(  tq);
            }

            //drawLandAsMask/drawLandMask?
            boolean showLandOption = 
                resultsVariables.size() >= 2 &&
                resultsVariables.get(0).equals(EDV.LON_NAME) &&
                resultsVariables.get(1).equals(EDV.LAT_NAME); 
            if (showLandOption) {
                //Draw Land
                int tLand = 0;          //don't translate
                String landOptions[] = {"", "under (show topography)", "over (just show bathymetry)"};  //under/over order also affects javascript below
                partValue = String2.stringStartsWith(queryParts, partName = ".land=");
                if (partValue != null) {
                    partValue = partValue.substring(6);
                    if      (partValue.equals("under")) tLand = 1;
                    else if (partValue.equals("over"))  tLand = 2;
                }
                writer.write(
                    "<tr>\n" +
                    "  <td colSpan=\"2\" nowrap>" + EDStatic.magGSLandMask + ": \n");
                writer.write(widgets.select("land", EDStatic.magGSLandMaskTooltipTable, 
                    1, landOptions, tLand, ""));
                writer.write(
                    "  </td>\n" +
                    "</tr>\n");

                //add to graphQuery
                if (tLand > 0) {
                    String tq = "&.land=" + (tLand == 1? "under" : "over");
                    graphQuery.append(        tq);
                    graphQueryNoLatLon.append(tq);
                    graphQueryNoTime.append(  tq);
                }
            } else {
                partValue = String2.stringStartsWith(queryParts, partName = ".land=");
                if (partValue != null) {
                    //pass it through
                    graphQuery.append(        partValue);          
                    graphQueryNoLatLon.append(partValue);  
                    graphQueryNoTime.append(  partValue);  
                }
            }


            //Vector Standard 
            String vec = "";
            if (drawVectors) {
                paramName = "vec";
                vec = String2.stringStartsWith(queryParts, partName = ".vec=");
                vec = vec == null? "" : vec.substring(partName.length());
                writer.write(
                    "  <tr>\n" +
                    "    <td nowrap>" + EDStatic.magGSVectorStandard + ":&nbsp;</td>\n" +
                    "    <td nowrap>");
                writer.write(widgets.textField(paramName, 
                    EDStatic.magGSVectorStandardTooltip, 10, 20, vec, ""));
                writer.write(
                    "    </td>\n" +
                    "  </tr>\n");

                //add to graphQuery
                if (vec.length() > 0) {
                    graphQuery.append(        "&.vec=" + vec);
                    graphQueryNoLatLon.append("&.vec=" + vec);
                    graphQueryNoTime.append(  "&.vec=" + vec);
                }
            }

            //yRange
            String yRange[] = new String[]{"", ""};
            if (true) {
                paramName = "yRange";
                String tyRange = String2.stringStartsWith(queryParts, partName = ".yRange=");
                if (tyRange != null) {
                    String tyRangeAr[] = String2.split(tyRange.substring(partName.length()), '|');
                    for (int i = 0; i < 2; i++) {
                        if (tyRangeAr.length > i) {
                            double td = String2.parseDouble(tyRangeAr[i]);
                            yRange[i] = Double.isNaN(td)? "" : String2.genEFormat10(td);
                        }
                    }
                }
                writer.write(
                    "  <tr>\n" +
                    "    <td nowrap>" + EDStatic.magGSYAxisMin + ":&nbsp;</td>\n" +
                    "    <td nowrap width=\"90%\">\n");
                writer.write(widgets.textField("yRangeMin", 
                    EDStatic.magGSYRangeMinTooltip + EDStatic.magGSYRangeTooltip, 
                   10, 20, yRange[0], ""));
                writer.write("&nbsp;&nbsp;" + EDStatic.magGSYAxisMax + ":\n");
                writer.write(widgets.textField("yRangeMax", 
                    EDStatic.magGSYRangeMaxTooltip + EDStatic.magGSYRangeTooltip, 
                   10, 20, yRange[1], ""));
                writer.write(
                    "    </td>\n" +
                    "  </tr>\n");

                //add to graphQuery
                if (yRange[0].length() > 0 || yRange[1].length() > 0) {
                    String ts = "&.yRange=" + yRange[0] + "|" + yRange[1];
                    graphQuery.append(ts);
                    graphQueryNoLatLon.append(ts);
                    graphQueryNoTime.append(ts);
                }
            }

            //end of graph settings table
            writer.write(widgets.endTable());

            //make javascript function to generate query
            //q1 holds resultVar csv list
            //q2 holds &queries
            writer.write(
                HtmlWidgets.PERCENT_ENCODE_JS +
                "<script type=\"text/javascript\"> \n" +
                "function makeQuery(varsToo) { \n" +
                "  try { \n" +
                "    var d = document; \n" +
                "    var tVar, c1 = \"\", c2 = \"\", q1 = \"\", q2 = \"\"; \n" + //constraint 1|2
                "    var active = {};\n" + //needed for functionJavaScript below
                "    if (varsToo) { \n");
            //vars
            for (int v = 0; v < nVars; v++) {
                writer.write( 
                    "      tVar = d.f1.var" + v + ".options[d.f1.var" + v + ".selectedIndex].text; \n" +
                    "      if (tVar.length > 0) {\n" +
                    "        q1 += (q1.length == 0? \"\" : \",\") + tVar; \n" +
                    "        active[tVar] = 1;\n" +
                    "      }\n");
            }
            writer.write(
                    "    } \n");
            //constraints
            for (int v = 0; v < nConRows; v++) {
                writer.write( 
                    "    tVar = d.f1.cVar" + v + ".options[d.f1.cVar" + v + ".selectedIndex].text; \n" +
                    "    if (tVar.length > 0) { \n" +
                    "      c1 = d.f1.cVal" + v + "a.value; \n" +
                    "      c2 = d.f1.cVal" + v + "b.value; \n" +
                    "      if (c1.length > 0) q2 += \"\\x26\" + tVar + \n" +
                    "        d.f1.cOp" + v + "a.options[d.f1.cOp" + v + "a.selectedIndex].text + percentEncode(c1); \n" +
                    "      if (c2.length > 0) q2 += \"\\x26\" + tVar + \n" +
                    "        d.f1.cOp" + v + "b.options[d.f1.cOp" + v + "b.selectedIndex].text + percentEncode(c2); \n" +
                    "    } \n");
            }

            //functions
            writer.write(functionJavaScript(formName, nOrderBy));

            //graph settings   
            writer.write(                
                "    q2 += \"\\x26.draw=\" + d.f1.draw.options[d.f1.draw.selectedIndex].text; \n");
            if (drawLinesAndMarkers || drawMarkers) writer.write(
                "    q2 += \"\\x26.marker=\" + d.f1.mType.selectedIndex + \"|\" + \n" +
                "      d.f1.mSize.options[d.f1.mSize.selectedIndex].text; \n");
            writer.write(
                "    q2 += \"\\x26.color=0x\"; \n" +
                "    for (var rb = 0; rb < " + colors.length + "; rb++) \n" + 
                "      if (d.f1.colr[rb].checked) q2 += d.f1.colr[rb].value; \n"); //always: one will be checked
            if (drawLinesAndMarkers || drawMarkers) writer.write(
                "    var tpc = d.f1.pc.options[d.f1.pc.selectedIndex].text;\n" +
                "    q2 += \"\\x26.colorBar=\" + d.f1.p.options[d.f1.p.selectedIndex].text + \"|\" + \n" +
                "      (tpc.length > 0? tpc.charAt(0) : \"\") + \"|\" + \n" +
                "      d.f1.ps.options[d.f1.ps.selectedIndex].text + \"|\" + \n" +
                "      d.f1.pMin.value + \"|\" + d.f1.pMax.value + \"|\" + \n" +
                "      d.f1.pSec.options[d.f1.pSec.selectedIndex].text; \n");
            if (drawVectors) writer.write(
                "    if (d.f1.vec.value.length > 0) q2 += \"\\x26.vec=\" + d.f1.vec.value; \n");
            if (showLandOption) writer.write(
                "    if (d.f1.land.selectedIndex > 0)\n" +
                "      q2 += \"\\x26.land=\" + (d.f1.land.selectedIndex<=1? \"under\" : \"over\"); \n");
            if (true) writer.write(
                "    var yRMin=d.f1.yRangeMin.value; \n" +
                "    var yRMax=d.f1.yRangeMax.value; \n" +
                "    if (yRMin.length > 0 || yRMax.length > 0)\n" +
                "      q2 += \"\\x26.yRange=\" + yRMin + \"|\" + yRMax; \n");
            writer.write(
                "    return q1 + q2; \n" +
                "  } catch (e) { \n" +
                "    alert(e); \n" +
                "    return \"\"; \n" +
                "  } \n" +
                "} \n" +
                "function mySubmit(varsToo) { \n" +
                "  var q = makeQuery(varsToo); \n" +
                "  if (q.length > 0) window.location=\"" + //javascript uses length, not length()
                    tErddapUrl + "/tabledap/" + datasetID + 
                    ".graph?\" + q;\n" + 
                "} \n" +
                "</script> \n");  

            //submit
            writer.write("&nbsp;\n"); //necessary for the blank line between the table (not <p>)
            writer.write(widgets.beginTable(0, 0, "")); 
            writer.write(
                "<tr><td nowrap>");
            writer.write(widgets.htmlButton("button", "", "",
                EDStatic.magRedrawTooltip, 
                "<big><b>" + EDStatic.magRedraw + "</b></big>", 
                "onMouseUp='mySubmit(true);'")); 
            writer.write(
                " " + EDStatic.patientData + "\n" +
                "</td></tr>\n");

            //Download the Data
            writer.write(
                "<tr><td nowrap>&nbsp;<br>" + EDStatic.optional + ":" +
                "<br>" + EDStatic.magFileType + ":\n");
            paramName = "fType";
            String htmlEncodedGraphQuery = XML.encodeAsHTMLAttribute(graphQuery.toString());
            writer.write(widgets.select(paramName, EDStatic.EDDSelectFileType, 1,
                allFileTypeNames, defaultFileTypeOption, 
                "onChange='document.f1.tUrl.value=\"" + tErddapUrl + "/tabledap/" + datasetID + 
                    "\" + document.f1.fType.options[document.f1.fType.selectedIndex].text + " +
                    "\"?" + htmlEncodedGraphQuery + "\";'"));
            writer.write(" and\n");
            writer.write(widgets.button("button", "", 
                EDStatic.magDownloadTooltip + "<br>" + EDStatic.patientData,
                EDStatic.magDownload, 
                //"class=\"skinny\" " + //only IE needs it but only IE ignores it
                "onMouseUp='window.location=\"" + 
                    tErddapUrl + "/tabledap/" + datasetID + 
                    "\" + document.f1." + paramName + ".options[document.f1." + paramName + ".selectedIndex].text + \"?" + 
                    htmlEncodedGraphQuery +
                    "\";'")); //or open a new window: window.open(result);\n" +
            writer.write(
                "</td></tr>\n");

            //view the url
            writer.write(
                "<tr><td nowrap>" + EDStatic.magViewUrl + ":\n");
            String genViewHtml = String2.replaceAll(EDStatic.justGenerateAndViewGraphUrlHtml, "&protocolName;", dapProtocol);
            writer.write(widgets.textField("tUrl", genViewHtml, 
                60, 1000, 
                tErddapUrl + "/tabledap/" + datasetID + 
                    dataFileTypeNames[defaultFileTypeOption] + "?" + graphQuery.toString(), 
                ""));
            writer.write("<br>(<a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html\" " +
                "title=\"tabledap documentation\">" + EDStatic.magDocumentation + "</a>\n" +
                EDStatic.htmlTooltipImage(loggedInAs, genViewHtml) + ")\n");
            writer.write(" (<a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html#fileType\">" +
                EDStatic.EDDFileTypeInformation + "</a>)\n");

            if (debugMode) String2.log("respondToGraphQuery 8");
            writer.write(
                "</td></tr>\n" +
                "</table>\n\n");

            //end form
            writer.write(widgets.endForm());

            //end of left half of big table
            writer.write("</td>\n" +
                "<td>" + gap + "</td>\n" + //gap in center
                "<td nowrap align=\"left\" valign=\"top\">\n"); //begin right half of screen

            //*** zoomLatLon stuff
            if (zoomLatLon) {

                String gqz = "onMouseUp='window.location=\"" + tErddapUrl + "/tabledap/" + 
                    datasetID + ".graph?" + htmlEncodedGraphQuery + "&amp;.zoom=";
                String gqnll = "onMouseUp='window.location=\"" + tErddapUrl + "/tabledap/" + 
                    datasetID + ".graph?" + XML.encodeAsHTMLAttribute(graphQueryNoLatLon.toString());

                writer.write(
                    EDStatic.magZoomCenter + "\n" +
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.magZoomCenterTooltip) +
                    "<br><b>" + EDStatic.magZoom + ":&nbsp;</b>\n");

                //zoom out?
                boolean disableZoomOut = 
                    !Double.isNaN(lonMin) &&
                    !Double.isNaN(lonMax) &&
                    !Double.isNaN(latMin) &&
                    !Double.isNaN(latMax) &&
                    lonMin <= maxExtentWESN[0] && 
                    lonMax >= maxExtentWESN[1] && 
                    latMin <= maxExtentWESN[2] &&
                    latMax >= maxExtentWESN[3];

                //if zoom out, keep ranges intact by moving tCenter to safe place
                writer.write(
                widgets.button("button", "",  
                    MessageFormat.format(EDStatic.magZoomOutTooltip, "8x"),  
                    MessageFormat.format(EDStatic.magZoomOut, "8x"),  
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : gqz + "out8\";'")));

                writer.write(
                widgets.button("button", "",  
                    MessageFormat.format(EDStatic.magZoomOutTooltip, "2x"),  
                    MessageFormat.format(EDStatic.magZoomOut, "2x"),  
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : gqz + "out2\";'")));

                writer.write(
                widgets.button("button", "",  
                    MessageFormat.format(EDStatic.magZoomOutTooltip, EDStatic.magZoomALittle),  
                    MessageFormat.format(EDStatic.magZoomOut, "").trim(),  
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : gqz + "out\";'")));

                //zoom data
                writer.write(
                widgets.button("button", "",  
                    MessageFormat.format(EDStatic.magZoomOutTooltip, EDStatic.magZoomOutData),  
                    EDStatic.magZoomData,  
                    "class=\"skinny\" " + gqnll + "\";'"));

                //zoom in     
                writer.write(
                    widgets.button("button", "", 
                        MessageFormat.format(EDStatic.magZoomInTooltip, EDStatic.magZoomALittle),  
                        MessageFormat.format(EDStatic.magZoomIn, "").trim(),  
                        "class=\"skinny\" " + gqz + "in\";'"));

                writer.write(
                    widgets.button("button", "", 
                        MessageFormat.format(EDStatic.magZoomInTooltip, "2x"),  
                        MessageFormat.format(EDStatic.magZoomIn, "2x"),  
                        "class=\"skinny\" " + gqz + "in2\";'"));

                writer.write(
                    widgets.button("button", "", 
                        MessageFormat.format(EDStatic.magZoomInTooltip, "8x"),  
                        MessageFormat.format(EDStatic.magZoomIn, "8x"),  
                        "class=\"skinny\" " + gqz + "in8\";'"));

                //trailing <br>
                writer.write("<br>");
            }

            //*** zoomTime stuff
            boolean zoomTime = !Double.isNaN(timeMin) && !Double.isNaN(timeMax);
            if (zoomTime) {

                EDVTimeStamp edvTime = (EDVTimeStamp)dataVariables[timeIndex];
                double edvTimeMin = edvTime.destinationMin();  //may be NaN
                double edvTimeMax = edvTime.destinationMax();  //may be NaN
                String tTime_precision = edvTime.time_precision();
                if (!Double.isNaN(edvTimeMin) && Double.isNaN(edvTimeMax))
                    edvTimeMax = Calendar2.backNDays(-1, edvTimeMax);

                //timeMin and timeMax are not NaN
                double timeRange = timeMax - timeMin;
                if (reallyVerbose)
                    String2.log("zoomTime timeMin=" + Calendar2.epochSecondsToIsoStringT(timeMin) +
                      "  timeMax=" + Calendar2.epochSecondsToIsoStringT(timeMax) +
                    "\n  range=" + Calendar2.elapsedTimeString(timeRange * 1000) +
                    "\n  edvTimeMin=" + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, edvTimeMin, "NaN") +
                             "  max=" + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, edvTimeMax, "NaN"));

                //set idealTimeN (1..100), idealTimeUnits
                int idealTimeN = -1; //1..100
                int idealTimeUnits = -1;
                partValue = String2.stringStartsWith(queryParts, ".timeRange=");
                if (partValue != null) {
                    //try to read from url params
                    String parts[] = String2.split(partValue.substring(11), ',');
                    if (parts.length == 2) {
                        idealTimeN = String2.parseInt(parts[0]);
                        idealTimeUnits = String2.indexOf(Calendar2.IDEAL_UNITS_OPTIONS, parts[1]);
                    }
                }
                //if not set, find closest
                //if (reallyVerbose)
                //    String2.log("  idealTimeN=" + idealTimeN + " units=" + idealTimeUnits);
                if (idealTimeN < 1 || idealTimeN > 100 ||
                    idealTimeUnits < 0) {

                    idealTimeUnits = Calendar2.IDEAL_UNITS_OPTIONS.length - 1;
                    while (idealTimeUnits > 0 && 
                        timeRange < Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]) {
                        idealTimeUnits--;
                    }
                    idealTimeN = Math2.minMax(1, 100, 
                        Math2.roundToInt(timeRange / Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]));
                }
                if (reallyVerbose)
                    String2.log("  idealTimeN+Units=" + idealTimeN + " " + Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]);


                //make idealized minTime
                GregorianCalendar idMinGc = Calendar2.roundToIdealGC(timeMin, idealTimeN, idealTimeUnits);
                GregorianCalendar idMaxGc = Calendar2.newGCalendarZulu(idMinGc.getTimeInMillis());
                idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 

                String gqnt = "window.location=\"" + tErddapUrl + "/tabledap/" + 
                    datasetID + ".graph?" + XML.encodeAsHTML(graphQueryNoTime.toString());

                writer.write(
                    "<b>" + EDStatic.magTimeRange + "</b>\n");

                String timeRangeString = idealTimeN + " " + Calendar2.IDEAL_UNITS_OPTIONS[idealTimeUnits];
                String timeRangeParam = "&amp;.timeRange=" + idealTimeN + "," + 
                    Calendar2.IDEAL_UNITS_OPTIONS[idealTimeUnits];

                //n = 1..100
                writer.write(
                widgets.select("timeN", //keep using widgets even though this isn't part of f1 form
                    EDStatic.magTimeRangeTooltip, 
                    1, Calendar2.IDEAL_N_OPTIONS, idealTimeN - 1, //-1 so index=0 .. 99
                    "onChange='" + gqnt + 
                    "&amp;time%3E=" + 
                    Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMin, "") +
                    "&amp;time%3C"  + 
                    Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMax, "") +
                    "&amp;.timeRange=\" + this.options[this.selectedIndex].text + \"," + 
                    Calendar2.IDEAL_UNITS_OPTIONS[idealTimeUnits] + "\";'")); 

                //timeUnits
                writer.write(
                widgets.select("timeUnits", //keep using widgets even though this isn't part of f1 form
                    EDStatic.magTimeRangeTooltip, 
                    1, Calendar2.IDEAL_UNITS_OPTIONS, idealTimeUnits, 
                    "onChange='" + gqnt + 
                    "&amp;time%3E=" + 
                    Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMin, "") +
                    "&amp;time%3C"  + 
                    Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMax, "") +
                    "&amp;.timeRange=" + idealTimeN + ",\" + " +
                    "this.options[this.selectedIndex].text;'")); 


                //beginning time
                if (!Double.isNaN(edvTimeMin)) {

                    //make idealized beginning time
                    GregorianCalendar tidMinGc = Calendar2.roundToIdealGC(edvTimeMin, idealTimeN, idealTimeUnits);
                    //if it rounded to later time period, shift to earlier time period
                    if (tidMinGc.getTimeInMillis() / 1000 > edvTimeMin)
                        tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                    GregorianCalendar tidMaxGc = Calendar2.newGCalendarZulu(tidMinGc.getTimeInMillis());
                        tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 

                    //always show button if idealTime is different from current selection
                    double idRange = (tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis()) / 1000;
                    double ratio = (timeMax - timeMin) / idRange;                    

                    if (timeMin > edvTimeMin || ratio < 0.99 || ratio > 1.01) {
                        writer.write(
                        "&nbsp;&nbsp;\n" +
                        HtmlWidgets.htmlTooltipImage( 
                            EDStatic.imageDirUrl(loggedInAs) + "arrowLL.gif", "<<",
                            MessageFormat.format(EDStatic.magTimeRangeFirst, timeRangeString),
                            "align=\"top\" onMouseUp='" + gqnt + 
                            "&amp;time%3E=" + 
                            Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMinGc) +
                            "&amp;time%3C"  + 
                            Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMaxGc) +
                            timeRangeParam + "\";'"));
                    } else {
                        writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
                    }
                } else {
                    writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
                }

                //time to left
                if (Double.isNaN(edvTimeMin) || 
                    (!Double.isNaN(edvTimeMin) && timeMin > edvTimeMin)) {

                    //idealized (rounded) time shift
                    idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                    idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                    writer.write(
                    "&nbsp;&nbsp;\n" +
                    HtmlWidgets.htmlTooltipImage( 
                        EDStatic.imageDirUrl(loggedInAs) + "minus.gif", "-",
                        MessageFormat.format(EDStatic.magTimeRangeBack, timeRangeString),
                        "align=\"top\" onMouseUp='" + gqnt + 
                        "&amp;time%3E=" + 
                        Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMinGc) + 
                        "&amp;time%3C"  + 
                        Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMaxGc) +
                        timeRangeParam + "\";'"));
                    idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                    idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 

                } else {
                    writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
                }

                //time to right
                if (Double.isNaN(edvTimeMax) || 
                    (!Double.isNaN(edvTimeMax) && timeMax < edvTimeMax)) {

                    //idealized (rounded) time shift
                    idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                    idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);  
                    String equals = 
                        (!Double.isNaN(edvTimeMax) && 
                        idMaxGc.getTimeInMillis() >= edvTimeMax * 1000)? "=" : "";
                    writer.write(
                    "&nbsp;&nbsp;\n" +
                    HtmlWidgets.htmlTooltipImage( 
                        EDStatic.imageDirUrl(loggedInAs) + "plus.gif", "+",
                        MessageFormat.format(EDStatic.magTimeRangeForward, timeRangeString),
                        "align=\"top\" onMouseUp='" + gqnt + 
                        "&amp;time%3E=" +          
                        Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMinGc) + 
                        "&amp;time%3C"  + equals + 
                        Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMaxGc) + 
                        timeRangeParam + "\";'"));
                    idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                    idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 

                } else {
                    writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
                }

                //end time
                if (!Double.isNaN(edvTimeMax)) {
                    //make idealized end time
                    GregorianCalendar tidMaxGc = Calendar2.roundToIdealGC(edvTimeMax, idealTimeN, idealTimeUnits);
                    //if it rounded to earlier time period, shift to later time period
                    if (tidMaxGc.getTimeInMillis() / 1000 < edvTimeMax)
                        tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                    GregorianCalendar tidMinGc = Calendar2.newGCalendarZulu(tidMaxGc.getTimeInMillis());
                        tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 

                    //always show button if idealTime is different from current selection
                    double idRange = (tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis()) / 1000;
                    double ratio = (timeMax - timeMin) / idRange;                    

                    if (timeMax < edvTimeMax || ratio < 0.99 || ratio > 1.01) {
                        writer.write(
                        "&nbsp;&nbsp;\n" +
                        HtmlWidgets.htmlTooltipImage( 
                            EDStatic.imageDirUrl(loggedInAs) + "arrowRR.gif", ">>",
                            MessageFormat.format(EDStatic.magTimeRangeLast, timeRangeString),
                            "align=\"top\" onMouseUp='" + gqnt + 
                            "&amp;time%3E=" + 
                            Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMinGc) +
                            "&amp;time%3C=" + 
                            Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMaxGc) +  //yes, =
                            timeRangeParam + "\";'"));
                    } else {
                        writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
                    }
                } else {
                    writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
                }

                //trailing <br>
                writer.write("<br>");

            }


            //display the graph
            if (reallyVerbose) 
                String2.log("  graphQuery=" + graphQuery.toString());
            if (zoomLatLon) writer.write(
                "<a href=\"" + tErddapUrl + "/tabledap/" + datasetID+ ".graph?" + htmlEncodedGraphQuery + 
                "&amp;.click=\">");  //if user clicks on image, browser adds "?x,y" to url
            writer.write("<img " + (zoomLatLon? "ismap " : "") + 
                "width=\"" + EDStatic.imageWidths[1] + 
                "\" height=\"" + EDStatic.imageHeights[1] + "\" " +
                "alt=\"" + EDStatic.patientYourGraph + "\" " +
                "src=\"" + tErddapUrl + "/tabledap/" + //don't use \n for the following lines
                datasetID+ ".png?" + htmlEncodedGraphQuery + "\">"); //no img end tag
            if (zoomLatLon) 
                writer.write("</a>");

            //*** end of right half of big table
            writer.write("\n</td></tr></table>\n");

            //*** do things with graphs
            writer.write(String2.replaceAll(MessageFormat.format(
                EDStatic.doWithGraphs, tErddapUrl), "&erddapUrl;", tErddapUrl));
            writer.write("\n\n");

            //*** .das   
            writer.write(
                "<hr>\n" +
                "<h2>" + EDStatic.dasTitle + "</h2>\n" +
                "<pre>\n");
            Table table = makeEmptyDestinationTable("/tabledap/" + datasetID + ".das", "", true); //as if no constraints
            table.writeDAS(writer, SEQUENCE_NAME, true); //useful so search engines find all relevant words
            writer.write("</pre>\n");

            //*** end of document
            writer.write(
                "<br>&nbsp;\n" +
                "<hr>\n");
            writeGeneralDapHtmlInstructions(tErddapUrl, writer, false); 
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
        }
        if (EDStatic.displayDiagnosticInfo) 
            EDStatic.writeDiagnosticInfoHtml(writer);
        writer.write(EDStatic.endBodyHtml(tErddapUrl));
        writer.write("\n</html>\n");

        //essential
        writer.flush(); 
    }

    /**
     * This suggests nPlaces precision for a given lat lon radius (used in Make A Graph).
     *
     * @param radius
     * @return nPlaces precision for a given lat lon radius (used in Make A Graph).
     */
    public static int lonLatNPlaces(double radius) {
        return radius > 100? 0 : radius > 10? 1 : radius > 1? 2 : radius > 0.1? 3 : 4;
    }

    /**
     * This looks for var,op in constraintVariable,constraintOp (or returns -1).
     *
     */
    protected static int indexOf(StringArray constraintVariables, StringArray constraintOps,
        String var, String op) {
        int n = constraintVariables.size();
        for (int i = 0; i < n; i++) {
            if (constraintVariables.get(i).equals(var) &&
                constraintOps.get(i).equals(op))
                return i;
        }
        return -1;
    }

    /** 
     * This returns the name of the file in datasetDir()
     * which has all of the distinct data combinations for the current subsetVariables.
     * The file is deleted by setSubsetVariablesCSV(), which is called whenever
     * the dataset is reloaded.
     * See also EDDTable.subsetVariables(), which deletes these files.
     *
     * @param loggedInAs POST datasets overwrite this method and use loggedInAs 
     *    since data for each loggedInAs is different; others don't use this
     * @throws Exception if trouble
     */
    public String subsetVariablesFileName(String loggedInAs) throws Exception {
        return SUBSET_FILENAME; 
    }

    /** 
     * This returns the name of the file in datasetDir()
     * which has just the distinct values for the current subsetVariables.
     * The file is deleted by setSubsetVariablesCSV(), which is called whenever
     * the dataset is reloaded.
     * See also EDDTable, which deletes these files.
     *
     * @param loggedInAs POST datasets overwrite this method and use loggedInAs 
     *    since data for each loggedInAs is different; others don't use this
     * @throws Exception if trouble
     */
    public String distinctSubsetVariablesFileName(String loggedInAs) throws Exception {
        return DISTINCT_SUBSET_FILENAME; 
    }

    /**
     * Generate the Subset Variables subsetVariables .subset HTML form.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void respondToSubsetQuery(HttpServletRequest request, 
        HttpServletResponse response, 
        String loggedInAs, String requestUrl, String userQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable {

        //BOB: this is similar to Erddap.doPostSubset()
        //Here, time is like a String variable (not reduced to Year)

        //constants
        String ANY = "(ANY)";
        boolean fixedLLRange = false; //true until 2011-02-08

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String subsetUrl = tErddapUrl + "/tabledap/" + datasetID + ".subset";
        if (userQuery == null) 
            userQuery = "";

        //!!!Important: this use of subsetVariables with () and accessibleViaSubset() 
        //insures that subsetVariables has been created.
        if (subsetVariables().length == 0 ||
            accessibleViaSubset().length() > 0) 
            throw new SimpleException(accessibleViaSubset());
        String subsetVariablesCSV = String2.toSVString(subsetVariables, ",", false);

        //if present, the lastP parameter is not used for bigTable, but is used for smallTable
        String queryParts[] = getUserQueryParts(userQuery); //decoded.  userQuery="" returns String[1]  with #0=""
        String start = ".last=";
        String val = String2.stringStartsWith(queryParts, start);
        if (val != null)
            val = val.substring(start.length());
        int lastP = String2.indexOf(subsetVariables, val); //pName of last changed select option

        //parse the userQuery to find the param specified for each subsetVariable
        //  with the required String for each subsetVariable (or null)
        String param[] = new String[subsetVariables.length];
        String firstNumericSubsetVar = String2.indexOf(subsetVariables, "time") >= 0? "time" : null; 
        for (int p = 0; p < subsetVariables.length; p++) {
            String pName = subsetVariables[p];
            start = pName + "=";  //this won't catch lonLatConstraints like longitude>=123
            param[p] = String2.stringStartsWith(queryParts, start); //may be null, ...="ANY", or ...="aString"
            if (param[p] != null)
                param[p] = param[p].substring(start.length());
            //if last selection was ANY, last is irrelevant
            if (param[p] == null || param[p].equals(ANY) || param[p].equals("\"" + ANY + "\"")) {
                param[p] = null;
            } else if (param[p].length() >= 2 && 
                     param[p].charAt(0) == '"' && 
                     param[p].charAt(param[p].length() - 1) == '"') {
                //remove begin/end quotes
                param[p] = param[p].substring(1, param[p].length() - 1);
            }
            if (p == lastP && param[p] == null)
                lastP = -1;

            //firstNumericSubsetVar
            if (firstNumericSubsetVar == null && 
                !pName.equals("longitude") && !pName.equals("latitude")) {
                EDV edv = findVariableByDestinationName(pName);
                if (edv.destinationDataTypeClass() != String.class)
                    firstNumericSubsetVar = pName;
            }
        }

        //firstNumericVar (not necessarily in subsetVariables)
        String firstNumericVar = String2.indexOf(dataVariableDestinationNames(), "time") >= 0? "time" : null; 
        if (firstNumericVar == null) {
            int nv = dataVariables.length;
            for (int v = 0; v < nv; v++) {
                EDV edv = dataVariables[v];
                if (edv.destinationName().equals("longitude") ||
                    edv.destinationName().equals("latitude")  ||
                    edv.destinationDataTypeClass() == String.class)
                    continue;
                firstNumericSubsetVar = edv.destinationName();
                break;
            }
        }

        //make/read all of the subsetVariable data
        Table subsetTable = subsetVariablesDataTable(loggedInAs);

        //if either map is possible, make consistent lonLatConstraints (specifies map extent)
        boolean distinctMapIsPossible = 
            String2.indexOf(subsetVariables, "longitude") >= 0 &&
            String2.indexOf(subsetVariables, "latitude")  >= 0;
        boolean relatedMapIsPossible = 
            !distinctMapIsPossible && lonIndex >= 0 && latIndex >= 0;
        String lonLatConstraints = ""; //(specifies map extent)
        if (distinctMapIsPossible && fixedLLRange) {
            double minLon = Double.NaN, maxLon = Double.NaN, 
                   minLat = Double.NaN, maxLat = Double.NaN;  
            //Don't get raw lon lat from variable's destinationMin/Max.
            //It isn't very reliable.

            //get min/max lon lat from subsetTable
            PrimitiveArray pa = subsetTable.findColumn("longitude");
            double stats[] = pa.calculateStats();
            minLon = stats[PrimitiveArray.STATS_MIN];
            maxLon = stats[PrimitiveArray.STATS_MAX];
            
            pa = subsetTable.findColumn("latitude");
            stats = pa.calculateStats();
            minLat = stats[PrimitiveArray.STATS_MIN];
            maxLat = stats[PrimitiveArray.STATS_MAX];
            if (reallyVerbose) String2.log(
                "  minMaxLonLat from bigTable: minLon=" + minLon + " maxLon=" + maxLon + 
                " minLat=" + minLat + " maxLat=" + maxLat);

            //calculate lonLatConstraints
            double minMaxLon[] = Math2.suggestLowHigh(minLon, maxLon);
            double minMaxLat[] = Math2.suggestLowHigh(minLat, maxLat);
            if (minLon >= -180) minMaxLon[0] = Math.max(-180, minMaxLon[0]);
            if (maxLon <=  180) minMaxLon[1] = Math.min( 180, minMaxLon[1]);
            if (maxLon <=  360) minMaxLon[1] = Math.min( 360, minMaxLon[1]);                    
            double lonCenter = (minMaxLon[0] + minMaxLon[1]) / 2;
            double latCenter = (minMaxLat[0] + minMaxLat[1]) / 2;
            double range = Math.max(minMaxLon[1] - minMaxLon[0],
                Math.max(1, minMaxLat[1] - minMaxLat[0])); //at least 1 degree
            minLon = lonCenter - range / 2;
            maxLon = lonCenter + range / 2;
            minLat = Math.max(-90, latCenter - range / 2);
            maxLat = Math.min( 90, latCenter + range / 2);
            lonLatConstraints =  
                "&longitude%3E=" + minLon +  //no need to round, they should be round numbers already from suggestLowHigh
                "&longitude%3C=" + maxLon +
                "&latitude%3E="  + minLat +
                "&latitude%3C="  + maxLat;
        }

        //reduce subsetTable to "bigTable"  (as if lastP param was set to ANY)
        int nRows = subsetTable.nRows();
        BitSet keep = new BitSet(nRows);
        keep.set(0, nRows); //set all to true
        for (int p = 0; p < subsetVariables.length; p++) {
            String tParam = param[p];
            if (tParam == null || p == lastP)  //don't include lastP param in bigTable
                continue;
 
            EDV edv = findDataVariableByDestinationName(subsetVariables[p]);
            EDVTimeStamp edvTimeStamp = edv instanceof EDVTimeStamp? (EDVTimeStamp)edv : 
                null;
            String tTime_precision = edvTimeStamp == null? null : 
                edvTimeStamp.time_precision();
            PrimitiveArray pa = subsetTable.findColumn(subsetVariables[p]);
            if (edvTimeStamp == null && !(pa instanceof StringArray) && tParam.equals("NaN"))
                tParam = "";  //e.g., doubleArray.getString() for NaN returns ""
            for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
                String value = edvTimeStamp == null?
                    pa.getString(row) :
                    Calendar2.epochSecondsToLimitedIsoStringT(
                        tTime_precision, pa.getDouble(row), "NaN");
                keep.set(row, tParam.equals(value));  //tParam isn't null; pa.getString might be
            }
        }
        subsetTable.justKeep(keep); 
        nRows = subsetTable.nRows(); //valid params should always yield at least 1, but don't sometimes
        if (reallyVerbose) String2.log("  bigTable nRows=" + nRows);

        //save lastP column  in a different PrimitiveArray
        PrimitiveArray lastPPA = lastP < 0? null :
            (PrimitiveArray)(subsetTable.findColumn(subsetVariables[lastP]).clone());

        //reduce subsetTable to "smallTable" (using lastP param to reduce the table size)
        if (lastP >= 0) {
            String tParam = param[lastP];
            keep = new BitSet(nRows);
            keep.set(0, nRows); //set all to true

            EDV edv = findDataVariableByDestinationName(subsetVariables[lastP]);
            EDVTimeStamp edvTimeStamp = edv instanceof EDVTimeStamp? (EDVTimeStamp)edv : 
                null;
            String tTime_precision = edvTimeStamp == null? null : 
                edvTimeStamp.time_precision();

            PrimitiveArray pa = subsetTable.findColumn(subsetVariables[lastP]);
            if (edvTimeStamp == null && !(pa instanceof StringArray) && tParam.equals("NaN"))
                tParam = "";  //e.g., doubleArray.getString() for NaN returns ""
            for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
                String value = edvTimeStamp == null?
                    pa.getString(row) :
                    Calendar2.epochSecondsToLimitedIsoStringT(
                        tTime_precision, pa.getDouble(row), "NaN");
                //if (value.startsWith(" ")) String2.log("value=\"" + value + "\"");
                keep.set(row, tParam.equals(value));  //tParam isn't null; pa.getString might be
            }
            subsetTable.justKeep(keep); 
            nRows = subsetTable.nRows(); //valid params should always yield at least 1, but don't sometimes
            if (reallyVerbose) String2.log("  smallTable " + 
                subsetVariables[lastP] + "=\"" + tParam + "\" nRows=" + nRows);
        }

        String clickPart = String2.stringStartsWith(queryParts, ".click=?"); //browser added '?' when user clicked
        String clickLon = null, clickLat = null;
        boolean userClicked = clickPart != null;
        boolean lonAndLatSpecified = 
            String2.stringStartsWith(queryParts, "longitude=") != null &&
            String2.stringStartsWith(queryParts, "latitude=")  != null;
        if (userClicked) {
            //lonAndLatSpecified may or may not have been specified
            try {
                //&.view marks end of graphDapQuery  (at least viewDistinctMap is true)
                //see graphDapQuery and url referenced in <a href><img></a> for view[0] below
                int tpo = userQuery.indexOf("&.view");  
                Test.ensureTrue(tpo >= 0, "\"&.view\" wasn't found in the userQuery.");
                String imageQuery = userQuery.substring(0, tpo);

                //extract the &.view info from userQuery
                String viewQuery = userQuery.substring(tpo);
                tpo = viewQuery.indexOf("&.click="); //end of &.view...= info
                Test.ensureTrue(tpo >= 0, "\"&.click=\" wasn't found at end of viewQuery=" + viewQuery);
                viewQuery = viewQuery.substring(0, tpo);              

                //get pngInfo -- it should exist since user clicked on the image
                Object pngInfo[] = readPngInfo(loggedInAs, imageQuery, ".png");
                if (pngInfo == null) {
                    String2.log("Unexpected: userClicked, but no .pngInfo for tabledap/" + 
                        datasetID + ".png?" + imageQuery);
                    userClicked = false;
                } else {
                    double pngInfoDoubleWESN[] = (double[])pngInfo[0];
                    int pngInfoIntWESN[] = (int[])pngInfo[1];

                    //generate a no Lat Lon Query
                    String noLLQuery = userQuery; 
                    //remove var name list from beginning
                    tpo = noLLQuery.indexOf("&");
                    if (tpo >= 0) //it should be
                        noLLQuery = noLLQuery.substring(tpo);
                    if (fixedLLRange) {
                        //remove lonLatConstraints   
                        //(longitude will appear before lat in the query -- search for it)
                        tpo = noLLQuery.indexOf("&longitude>="); 
                        if (tpo < 0)
                            tpo = noLLQuery.indexOf("&longitude%3E="); 
                        if (tpo >= 0) //should be
                            noLLQuery = noLLQuery.substring(0, tpo); //delete everything after it
                        else if (verbose) String2.log("\"&longitude%3E=\" not found in noLLQuery=" + noLLQuery);
                    } else {
                        //no LonLatConstraint? search for &distinct()
                        tpo = noLLQuery.indexOf("&distinct()");
                        if (tpo >= 0) {
                            noLLQuery = noLLQuery.substring(0, tpo); //delete everything after it
                        }
                    }
                    //and search for lon=
                    tpo = noLLQuery.indexOf("&longitude=");
                    if (tpo >= 0) {
                        int tpo2 = noLLQuery.indexOf('&', tpo + 1);
                        noLLQuery = noLLQuery.substring(0, tpo) + 
                            (tpo2 > 0? noLLQuery.substring(tpo2) : "");
                    }
                    //and search for lat=
                    tpo = noLLQuery.indexOf("&latitude=");
                    if (tpo >= 0) {
                        int tpo2 = noLLQuery.indexOf('&', tpo + 1);
                        noLLQuery = noLLQuery.substring(0, tpo) + 
                            (tpo2 > 0? noLLQuery.substring(tpo2) : "");
                    }


                    //convert userClick x,y to clickLonLat
                    String xy[] = String2.split(clickPart.substring(8), ','); //e.g., 24,116
                    int cx = String2.parseInt(xy[0]);
                    int cy = String2.parseInt(xy[1]);
                    if (lonAndLatSpecified) {
                        if (cx < pngInfoIntWESN[0] ||
                            cx > pngInfoIntWESN[1] ||
                            cy > pngInfoIntWESN[2] ||
                            cy < pngInfoIntWESN[3]) {

                            //reset lon and lat to ANY
                            noLLQuery = subsetVariablesCSV + 
                                noLLQuery + //already percent encoded
                                viewQuery + "&distinct()";

                            if (verbose) String2.log("userClicked outside graph, so redirecting to " + 
                                subsetUrl + "?" + noLLQuery);
                            response.sendRedirect(subsetUrl + "?" + noLLQuery);
                            return;            
                        }

                    } else { //!lonAndLatSpecified
                        double clickLonLat[] = SgtUtil.xyToLonLat(
                            cx, cy, pngInfoIntWESN, pngInfoDoubleWESN, 
                            pngInfoDoubleWESN);  //use pngInfoDoubleWESN for maxExtentWESN (insist on click within visible map)
                        Test.ensureTrue(clickLonLat != null, "clickLonLat is null!");
                        if (verbose)
                            String2.log("clickLon=" + clickLonLat[0] + " lat=" + clickLonLat[1]);

                        //find closestLon and closestLat 
                        PrimitiveArray lonPa = subsetTable.findColumn("longitude");
                        PrimitiveArray latPa = subsetTable.findColumn("latitude");
                        int tn = lonPa.size();
                        String closestLon = null, closestLat = null;
                        double minDist2 = Double.MAX_VALUE;
                        for (int row = 0; row < tn; row++) {
                            double tLon = lonPa.getDouble(row);
                            double tLat = latPa.getDouble(row);
                            if (!Double.isNaN(tLon) && !Double.isNaN(tLat)) {
                                double dist2 = Math.pow(tLon - clickLonLat[0], 2) +
                                               Math.pow(tLat - clickLonLat[1], 2);
                                if (dist2 <= minDist2) { 
                                    minDist2 = dist2;
                                    closestLon = lonPa.getString(row);
                                    closestLat = latPa.getString(row);
                                }
                            }
                        }        
                        Test.ensureTrue(closestLon != null, "closestLon=null.  No valid lon, lat?");

                        String newQuery = subsetVariablesCSV + 
                            "&longitude=" + closestLon + 
                            "&latitude="  + closestLat + 
                            noLLQuery + //already percent encoded
                            viewQuery + "&distinct()";

                        if (verbose) String2.log("userClicked, so redirecting to " + 
                            subsetUrl + "?" + newQuery);
                        response.sendRedirect(subsetUrl + "?" + newQuery);
                        return;            
                    }
                }

            } catch (Exception e) {
                String2.log(String2.ERROR + " while decoding \"&.click=\":\n" + MustBe.throwableToString(e));
                userClicked = false;
            }
        }      


        //show the .html response/form
        HtmlWidgets widgets = new HtmlWidgets("", true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = outputStreamSource.outputStream("UTF-8");
        Writer writer = new OutputStreamWriter(out, "UTF-8"); 
        writer.write(EDStatic.startHeadHtml(tErddapUrl,  
            title() + " - " + EDStatic.subset));
        writer.write("\n" + rssHeadLink(loggedInAs));
        writer.write("\n</head>\n");
        writer.write(EDStatic.startBodyHtml(loggedInAs));
        writer.write("\n");
        writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)));   //this is a link to a script
        writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
        try {
            writer.write(EDStatic.youAreHereWithHelp(loggedInAs, dapProtocol, 
                EDStatic.subset, 
                EDStatic.subsetTooltip));

            StringBuilder diQuery = new StringBuilder();
            for (int qpi = 1; qpi < queryParts.length; qpi++) { //1 because part0 is var names
                String qp = queryParts[qpi];
                if (!qp.startsWith(".") &&      //lose e.g., &.viewDistinct...
                    !qp.equals("distinct()")) { //&distinct() is misleading/trouble on MAG
                    //keep it
                    int epo = qp.indexOf('=');
                    if (epo >= 0) 
                        qp = qp.substring(0, epo + 1) + 
                             SSR.minimalPercentEncode(qp.substring(epo + 1));
                    else qp = SSR.minimalPercentEncode(qp);
                    diQuery.append("&" + qp);
                }
            }
            writeHtmlDatasetInfo(loggedInAs, writer, false, true, true, diQuery.toString(), "");
            writer.write(HtmlWidgets.ifJavaScriptDisabled);
            
            //if noData/invalid request tell user and reset all
            if (nRows == 0) {
                //notably, this happens if a selected value has a leading or trailing space
                //because the <option> on the html form trims the values when submitting the form (or before).
                //but trying to fix this 2012-04-18
                String2.log("WARNING: EDDTable.respondToSubsetQuery found no matching data for\n" +
                    requestUrl + "?" + userQuery); 

                //error message?
                writer.write("<font class=\"warningColor\">" + MustBe.THERE_IS_NO_DATA + 
                    " " + EDStatic.resetTheFormWas + "</font>\n");

                //reset all
                Arrays.fill(param, ANY);
                subsetTable = subsetVariablesDataTable(loggedInAs); //reload all subset data
                lastP = -1;
                lastPPA = null;
            }
            String lastPName = lastP < 0? null : subsetVariables[lastP];

            //Select a subset
            writer.write(
                widgets.beginForm("f1", "GET", subsetUrl, "") + "\n" +
                "<p><b>" + EDStatic.subsetSelect + ":</b>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font class=\"subduedColor\">" +
                    "(" + 
                    MessageFormat.format(EDStatic.subsetNMatching, "" + subsetTable.nRows()) + 
                    ")</font>\n" +
                "  <br>" + EDStatic.subsetInstructions + "\n" +
                widgets.beginTable(0, 0, ""));  

            StringBuilder countsVariables = new StringBuilder(); 
            StringBuilder newConstraintsNoLL = new StringBuilder();
            StringBuilder newConstraints = new StringBuilder();   //for url
            StringBuilder newConstraintsNLP = new StringBuilder();   //for url, w/out lastP variable
            StringBuilder newConstraintsNLPHuman = new StringBuilder(); //for human consumption
            boolean allData = true;
            for (int p = 0; p < subsetVariables.length; p++) {
                String pName = subsetVariables[p];
                EDV edv = findDataVariableByDestinationName(pName);
                EDVTimeStamp edvTimeStamp = edv instanceof EDVTimeStamp? (EDVTimeStamp)edv : 
                    null;
                String tTime_precision = edvTimeStamp == null? null : 
                    edvTimeStamp.time_precision();

                //work on a copy
                PrimitiveArray pa = p == lastP? (PrimitiveArray)lastPPA.clone() :
                    (PrimitiveArray)(subsetTable.findColumn(pName).clone());
                if (edvTimeStamp != null) {
                    int paSize = pa.size();
                    StringArray ta = new StringArray(paSize, false);
                    for (int row = 0; row < paSize; row++) 
                        ta.add(Calendar2.epochSecondsToLimitedIsoStringT(
                            tTime_precision, pa.getDouble(row), "NaN"));
                    pa = ta;
                }
                pa.sortIgnoreCase();
                //int nBefore = pa.size();
                pa.removeDuplicates();
                //String2.log("  " + pName + " nBefore=" + nBefore + " nAfter=" + pa.size());
                pa.addString(0, ""); //place holder for ANY (but pa may be numeric so can't take it now)
                String pasa[] = pa.toStringArray();
                pasa[0] = ANY;
                //use NaN for numeric missing value
                //String2.log("pName=" + pName + " last pasa value=" + pasa[pasa.length - 1]);
                if (!(pa instanceof StringArray)) {
                    if (pasa[pasa.length - 1].length() == 0)
                        pasa[pasa.length - 1] = "NaN";
                }
                int which = param[p] == null? 0 : String2.indexOf(pasa, param[p]);
                if (which < 0)
                    which = 0;
                if (which > 0) {
                    allData = false;
                    if (countsVariables.length() > 0) 
                        countsVariables.append(',');
                    countsVariables.append(SSR.minimalPercentEncode(pName));
                    if ((pa instanceof StringArray) || edvTimeStamp != null) {
                        String tq = "&" + SSR.minimalPercentEncode(pName) + "=" + 
                            SSR.minimalPercentEncode("\"" + param[p] + "\"");
                        newConstraints.append(tq);
                        newConstraintsNoLL.append(tq);
                        if (p != lastP) {
                            newConstraintsNLP.append(tq);
                            newConstraintsNLPHuman.append(
                                (newConstraintsNLPHuman.length() > 0? " and " : "") +
                                pName + "=\"" + param[p] + "\"");
                        }
                    } else {  //numeric
                        String tq = "&" + SSR.minimalPercentEncode(pName) + "=" + param[p];
                        newConstraints.append(tq);
                        if (!pName.equals("longitude") && !pName.equals("latitude"))
                            newConstraintsNoLL.append(tq);
                        if (p != lastP) {
                            newConstraintsNLP.append(tq);
                            newConstraintsNLPHuman.append(
                                (newConstraintsNLPHuman.length() > 0? " and " : "") +
                                pName + "=" + param[p]);
                        }
                    }
                }

                //String2.log("pName=" + pName + " which=" + which);
                writer.write(
                    "<tr>\n" +
                    "  <td nowrap>&nbsp;&nbsp;&nbsp;&nbsp;" + pName + "\n" +
                    EDStatic.htmlTooltipImageEDV(loggedInAs, edv) +
                    "  </td>\n" +
                    "  <td nowrap>\n");

                if (p == lastP) 
                    writer.write(
                        "  " + widgets.beginTable(0, 0, "") + "\n" +
                        "  <tr>\n" +
                        "  <td nowrap>\n");

                writer.write(
                    "  &nbsp;=\n" + 
                    widgets.select(pName, "", 1,
                        pasa, which, "onchange='mySubmit(\"" + pName + "\");'",
                        true));  //encodeSpaces solves the problem with consecutive internal spaces

                String tUnits = edv instanceof EDVTimeStamp? null : edv.units();                
                tUnits = tUnits == null? "" : 
                    "&nbsp;<font class=\"subduedColor\">" + tUnits + 
                    "</font>&nbsp;\n";

                if (p == lastP) { 
                    writer.write(
                        "      </td>\n" +
                        "      <td>\n" +
                        "<img src=\"" + widgets.imageDirUrl + "minus.gif\"\n" +
                        "  " + widgets.completeTooltip(EDStatic.selectPrevious) +
                        "  alt=\"-\" " + 
                        //onMouseUp works much better than onClick and onDblClick
                        "  onMouseUp='\n" +
                        "   var sel=document.f1." + pName + ";\n" +
                        "   if (sel.selectedIndex>1) {\n" + //1: if go to 0=(Any), lastP is lost. Stick to individual items.
                        "    sel.selectedIndex--;\n" +
                        "    mySubmit(\"" + pName + "\");\n" +
                        "   }' >\n" + 
                        "      </td>\n" +

                        "      <td>\n" +
                        "<img src=\"" + widgets.imageDirUrl + "plus.gif\"\n" +
                        "  " + widgets.completeTooltip(EDStatic.selectNext) +
                        "  alt=\"+\" " + 
                        //onMouseUp works much better than onClick and onDblClick
                        "  onMouseUp='\n" +
                        "   var sel=document.f1." + pName + ";\n" +
                        "   if (sel.selectedIndex<sel.length-1) {\n" + //no action if at last item
                        "    sel.selectedIndex++;\n" +
                        "    mySubmit(\"" + pName + "\");\n" +
                        "   }' >\n" + 
                        "      </td>\n" +

                        "      <td>" + tUnits + "</td>\n" +

                        "    </tr>\n" +
                        "    " + widgets.endTable());
                } else {
                    writer.write(tUnits);
                }

                writer.write(

                    //write hidden last widget?
                    (p == lastP? widgets.hidden("last", pName) : "") +

                    //end of select td (or its table)
                    "      </td>\n");

                //n options
                writer.write(
                    (which == 0 || p == lastP? 
                        "  <td nowrap>&nbsp;<font class=\"subduedColor\">" + 
                            (pa.size() - 1) + " " + 
                            (pa.size() == 2? 
                                EDStatic.subsetOption + ": " + 
                                //encodeSpaces so only option displays nicely
                                XML.minimalEncodeSpaces(XML.encodeAsHTML(pa.getString(1))) : 
                                EDStatic.subsetOptions) + 
                            "</font></td>\n" : 
                        "") + 

                    "</tr>\n");
            }

            writer.write(
                "</table>\n" +
                "\n");

            //View the graph and/or data?
            writer.write(
                "<p><b>" + EDStatic.subsetView + ":</b>\n");
            //!!! If changing these, change DEFAULT_SUBSET_VIEWS above. 
            String viewParam[] = {
                "viewDistinctMap",        
                "viewRelatedMap",                                            
                "viewDistinctDataCounts", 
                "viewDistinctData",     
                "viewRelatedDataCounts",  
                "viewRelatedData"};
            String viewTitle[] = {
                EDStatic.subsetViewDistinctMap,        
                EDStatic.subsetViewRelatedMap,                                            
                EDStatic.subsetViewDistinctDataCounts, 
                EDStatic.subsetViewDistinctData,     
                EDStatic.subsetViewRelatedDataCounts,  
                EDStatic.subsetViewRelatedData};
            String viewTooltip[] = {
                EDStatic.subsetViewDistinctMapTooltip,
                EDStatic.subsetViewRelatedMapTooltip        + EDStatic.subsetWarn,
                EDStatic.subsetViewDistinctDataCountsTooltip, 
                EDStatic.subsetViewDistinctDataTooltip      + EDStatic.subsetWarn10000,     
                EDStatic.subsetViewRelatedDataCountsTooltip + EDStatic.subsetWarn,  
                EDStatic.subsetViewRelatedDataTooltip       + EDStatic.subsetWarn10000};

            boolean useCheckbox[] = {true,  true, true,  false, true, false};
            //for checkboxes, viewDefault: 0=false 1=true
            int     viewDefault[] = {   1,     0,    0,   1000,    0,    0}; 
            int     viewValue[]   = new int[viewParam.length]; //will be set below
            StringBuilder viewQuery = new StringBuilder();
            String intOptions[] =  //1 is not an option
                new String[]{"0", "10", "100", "1000", "10000", "100000"}; 
           
            for (int v = 0; v < viewParam.length; v++) {
                start = "." + viewParam[v] + "="; //e.g., &.viewDistinctMap=
                if (useCheckbox[v]) {
                    //checkboxes
                    //some or all maps not possible
                    if (v == 0 && !distinctMapIsPossible) {
                        viewValue[v] = 0;
                        continue;  //don't even show the checkbox
                    }

                    if (v == 1 && !relatedMapIsPossible) {
                        viewValue[v] = 0;
                        continue;  //don't even show the checkbox
                    }

                    //show the checkbox
                    String part = (userQuery == null || userQuery.length() == 0)?
                        start + (viewDefault[v] == 1) :                    
                        String2.stringStartsWith(queryParts, start); 
                    val = part == null? "false" : part.substring(start.length());
                    viewValue[v] = "true".equals(val) ? 1 : 0; 
                    writer.write(
                        "&nbsp;&nbsp;&nbsp;\n" + 
                        widgets.checkbox(viewParam[v], 
                            "", 
                            viewValue[v] == 1, "true", viewTitle[v], 
                            "onclick='mySubmit(null);'") +  //IE doesn't trigger onchange for checkbox
                        EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[v]));
                    if (viewValue[v] == 1)
                        viewQuery.append("&." + viewParam[v] + "=true");

                } else {

                    //select 0/10/100/1000/10000/100000
                    String part = (userQuery == null || userQuery.length() == 0)?
                        null :                    
                        String2.stringStartsWith(queryParts, start); 
                    val = part == null? null : part.substring(start.length());
                    if (val != null && val.equals("true")) //from when it was a checkbox
                        viewValue[v] = 1000;
                    else if (val != null && val.equals("false")) //from when it was a checkbox
                        viewValue[v] = 0;
                    else viewValue[v] = String2.parseInt(val); 
                    if (String2.indexOf(intOptions, "" + viewValue[v]) < 0)
                        viewValue[v] = viewDefault[v];
                    writer.write(
                        "&nbsp;&nbsp;&nbsp;\n" + 
                        viewTitle[v] + " " +
                        widgets.select(viewParam[v], "", 1,
                            intOptions, String2.indexOf(intOptions, "" + viewValue[v]),
                            "onchange='mySubmit(null);'") +  
                        EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[v]));
                    viewQuery.append("&." + viewParam[v] + "=" + viewValue[v]);
                }
            }

            //mySubmit   
            //(greatly reduces the length of the query -- just essential info --
            //  and makes query like normal tabledap request)
            writer.write(
                HtmlWidgets.PERCENT_ENCODE_JS +
                "<script type=\"text/javascript\"> \n" +
                "function mySubmit(tLast) { \n" +
                "  try { \n" +
                "    var d = document; \n" +
                "    var q = \"" + subsetVariablesCSV + "\"; \n" +
                "    var w; \n");
            for (int p = 0; p < subsetVariables.length; p++) {
                String pName = subsetVariables[p];
                String quotes = (subsetTable.getColumn(p) instanceof StringArray)? "\\\"" : "";
                writer.write(
                "    w = d.f1." + pName + ".selectedIndex; \n" +
                "    if (w > 0) q += \"\\x26" + pName + 
                    "=\" + percentEncode(\"" + quotes + "\" + d.f1." + pName + ".options[w].text + \"" + quotes + "\"); \n");
            }
            for (int v = 0; v < viewParam.length; v++) {
                String pName = viewParam[v];
                if (useCheckbox[v]) {
                    if (v == 0 && !distinctMapIsPossible) continue;
                    if (v == 1 && !relatedMapIsPossible)  continue;
                    writer.write(
                    "    if (d.f1." + pName + ".checked) q += \"\\x26." + pName + "=true\"; \n");
                } else {
                    writer.write(
                    "    q += \"\\x26." + pName + 
                        "=\" + d.f1." + pName + ".options[d.f1." + pName + ".selectedIndex].text; \n");
                }
            }            
            writer.write(    
                "    q += \"\\x26distinct()\"; \n");

            //last
            writer.write(
                "    if ((tLast == null || tLast == undefined) && d.f1.last != undefined) tLast = d.f1.last.value; \n" + //get from hidden widget?
                "    if (tLast != null && tLast != undefined) q += \"\\x26.last=\" + tLast; \n");

            //query must be something, else checkboxes reset to defaults
            writer.write( 
                "    if (q.length == 0) q += \"" + viewParam[0] + "=false\"; \n"); //javascript uses length, not length()

            //submit the query
            writer.write(
                "    window.location=\"" + subsetUrl + "?\" + q;\n" + 
                "  } catch (e) { \n" +
                "    alert(e); \n" +
                "    return \"\"; \n" +
                "  } \n" +
                "} \n" +
                "</script> \n");  

            //endForm
            writer.write(widgets.endForm() + "\n");

            //set initial focus to lastP select widget    throws error
            if (lastP >= 0) 
                writer.write(
                    "<script type=\"text/javascript\">document.f1." + subsetVariables[lastP] + ".focus();</script>\n");
            

            //*** RESULTS
            // 0 = viewDistinctMap
            int current = 0; 
            if (distinctMapIsPossible) {
                //the ordering of parts of graphDapQuery is used above to decode url if userClicked (see above)
                String graphDapQuery = 
                    "longitude,latitude" + 
                    (firstNumericSubsetVar == null? "" : "," + firstNumericSubsetVar) + 
                    newConstraints.toString() + 
                    lonLatConstraints + //map extent, e.g., lon>= <=  lat>= <=
                    "&distinct()" + 
                    "&.draw=markers&.colorBar=|D||||";
                writer.write("\n" +
                    "<a name=\"" + viewParam[current] + "\">&nbsp;</a><hr>\n" +
                    "<b>" + viewTitle[current] + "</b>\n" +
                    EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[current]) +
                    "&nbsp;&nbsp;(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".graph?" +
                        XML.encodeAsHTML(graphDapQuery) + "\">" +
                        EDStatic.subsetRefineMapDownload + "</a>)\n" +
                    "<br>");

                if (viewValue[current] == 1) {  

                    if (lonAndLatSpecified)
                        writer.write(
                            "(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".subset?" +
                            XML.encodeAsHTMLAttribute(subsetVariablesCSV + newConstraintsNoLL.toString() + 
                                viewQuery.toString() + //&.view marks end of graphDapQuery
                                "&distinct()") + 
                            "\">" +
                            MessageFormat.format(EDStatic.subsetClickResetLL, ANY) + 
                            "</a>)\n");
                    else 
                        writer.write(EDStatic.subsetClickResetClosest + "\n");
                            //EDStatic.htmlTooltipImage(loggedInAs, 
                            //    "Unfortunately, after you click, some data sources respond with" +
                            //    "<br><tt>" + MustBe.THERE_IS_NO_DATA + "</tt>" +
                            //    "<br>because they can't handle requests for specific longitude and" +
                            //    "<br>latitude values, but there will still be useful information above."));

                    writer.write(
                        "<br><a href=\"" + subsetUrl + "?" + 
                            XML.encodeAsHTMLAttribute(
                                graphDapQuery + //userClicked above needs to be able to extract exact same graphDapQuery
                                viewQuery.toString() + //&.view marks end of graphDapQuery
                                "&.click=") + 
                            "\">" +  //if user clicks on image, browser adds "?x,y" to url
                        "<img ismap " + 
                            "width=\"" + EDStatic.imageWidths[1] + 
                            "\" height=\"" + EDStatic.imageHeights[1] + "\" " +
                            "alt=\"" + XML.encodeAsHTML(viewTitle[current]) + "\" " +
                            "src=\"" + XML.encodeAsHTMLAttribute(tErddapUrl + "/tabledap/" + 
                                datasetID + ".png?" + graphDapQuery) + "\">\n" +  
                        "</a>\n");
                } else {
                    writer.write("<p><font class=\"subduedColor\">" +
                        MessageFormat.format(EDStatic.subsetViewCheck, 
                            "<tt>" + EDStatic.subsetView + " : " + viewTitle[current] + "</tt>") +
                        "</font>\n");
                }
            }

            // 1 = viewRelatedMap
            current = 1;
            if (relatedMapIsPossible) {
                String graphDapQuery = 
                    "longitude,latitude" + 
                    (firstNumericVar == null? "" : "," + firstNumericVar) + 
                    newConstraints.toString() + //?? use lonLatConstraints?
                    //not &distinct()
                    "&.draw=markers&.colorBar=|D||||";
                writer.write("\n" +
                    "<a name=\"" + viewParam[current] + "\">&nbsp;</a><hr>\n" +
                    "<b>" + viewTitle[current] + "</b>\n" +
                    EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[current]) +
                    "&nbsp;&nbsp;(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".graph?" +
                        XML.encodeAsHTMLAttribute(graphDapQuery) + "\">" +
                        EDStatic.subsetRefineMapDownload + "</a>)\n");

                if (viewValue[current] == 1) {  
                    if (allData) {
                        writer.write(
                            "\n<p><font class=\"subduedColor\">" +
                            MessageFormat.format(EDStatic.subsetViewCheck1,
                                viewTitle[current], ANY) +
                            "</font>\n");
                    } else {
                        writer.write(
                            "<br><img width=\"" + EDStatic.imageWidths[1] + 
                            "\" height=\"" + EDStatic.imageHeights[1] + "\" " +
                            "alt=\"" + EDStatic.patientYourGraph + "\" " +
                            "src=\"" + XML.encodeAsHTMLAttribute(tErddapUrl + "/tabledap/" + 
                                datasetID + ".png?" + graphDapQuery) + "\">\n"); 
                    }
                } else {
                    writer.write("<p><font class=\"subduedColor\">" +
                        MessageFormat.format(EDStatic.subsetViewCheck, 
                            "<tt>" + EDStatic.subsetView + " : " + viewTitle[current] + "</tt>") +
                        "</font>\n" +
                        String2.replaceAll(EDStatic.subsetWarn, "<br>", " ") + 
                        "\n");
                }
            }

            // 2 = viewDistinctDataCounts
            current = 2;
            writer.write("\n" +
                "<br><a name=\"" + viewParam[current] + "\">&nbsp;</a><hr>\n" +
                "<p><b>" + viewTitle[current] + "</b>\n" +
                EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[current]));
            if (viewValue[current] == 1 && lastP >= 0 && lastPPA != null) {  //lastPPA shouldn't be null
                writer.write(
                    "&nbsp;&nbsp;\n" +
                    "(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".html?" +
                    XML.encodeAsHTMLAttribute(countsVariables.toString() + newConstraintsNLP.toString() + 
                        "&distinct()") + 
                    "\">" + EDStatic.subsetRefineSubsetDownload + "</a>)\n");

                try {                    
                    //get the distinct data; work on a copy
                    int n = lastPPA.size();
                    EDV edv = findDataVariableByDestinationName(subsetVariables[lastP]);
                    PrimitiveArray varPA = (PrimitiveArray)lastPPA.clone(); 
                    Table countTable = new Table();
                    countTable.addColumn(0, lastPName, varPA, edv.combinedAttributes());

                    //sort, count, remove duplicates
                    varPA.sortIgnoreCase();
                    IntArray countPA = new IntArray(n, false);
                    countTable.addColumn(EDStatic.subsetCount, countPA);
                    int lastCount = 1;
                    countPA.add(lastCount);
                    keep = new BitSet(n);
                    keep.set(0, n); //initially, all set
                    for (int i = 1; i < n; i++) {
                        if (varPA.compare(i-1, i) == 0) {
                            keep.clear(i-1);
                            lastCount++;
                        } else {
                            lastCount = 1;
                        }
                        countPA.add(lastCount);
                    }
                    countTable.justKeep(keep);

                    //calculate percents
                    double stats[] = countPA.calculateStats();
                    double total = stats[PrimitiveArray.STATS_SUM];
                    n = countPA.size();
                    DoubleArray percentPA = new DoubleArray(n, false);
                    countTable.addColumn(EDStatic.subsetPercent, percentPA);
                    for (int i = 0; i < n; i++) 
                        percentPA.add(Math2.roundTo(countPA.get(i) * 100 / total, 2));

                    //write results
                    String countsCon = newConstraintsNLPHuman.toString();
                    writer.write("<br>" +  
                        (countsCon.length() == 0? 
                            EDStatic.subsetWhenNoConstraints : 
                            MessageFormat.format(EDStatic.subsetWhen, XML.encodeAsHTML(countsCon))) + 
                        (countsCon.length() < 40? " " : "\n<br>") +
                        MessageFormat.format(EDStatic.subsetWhenCounts,
                            EDStatic.subsetViewSelectDistinctCombos,
                            "" + countTable.nRows(), XML.encodeAsHTML(lastPName)) + 
                        "\n");
                    writer.flush(); //essential, since creating and using another writer to write the countTable
                    TableWriterHtmlTable.writeAllAndFinish(loggedInAs, countTable, 
                        new OutputStreamSourceSimple(out), 
                        false, "", false, "", "", 
                        true, false, -1);          
                    writer.write("<p>" + 
                        MessageFormat.format(EDStatic.subsetTotalCount, 
                            "" + Math2.roundToLong(total)) + 
                        "\n");
                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    String message = MustBe.getShortErrorMessage(t);
                    String2.log(String2.ERROR + ":\n" + MustBe.throwableToString(t)); //log full message with stack trace
                    writer.write( 
                        //"<p>An error occurred while getting the data:\n" +
                        "<pre>" + XML.encodeAsPreHTML(message, 120) +
                        "</pre>\n");                        
                }

            } else {
                writer.write(
                    "<p><font class=\"subduedColor\">" +
                    MessageFormat.format(EDStatic.subsetViewSelect,
                        EDStatic.subsetViewSelectDistinctCombos,
                        EDStatic.subsetView, viewTitle[current]) +
                    "</font>\n");
            }

            // 3 = viewDistinctData
            current = 3;
            writer.write("\n" +
                "<br><a name=\"" + viewParam[current] + "\">&nbsp;</a><hr>\n" +
                "<p><b>" + viewTitle[current] + "</b>\n" +
                EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[current]) +
                "&nbsp;&nbsp;(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID +
                    ".das\">" + EDStatic.subsetMetadata + "</a>)\n");
            writer.write(
                "&nbsp;&nbsp;\n" +
                "(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".html?" +
                    XML.encodeAsHTMLAttribute(subsetVariablesCSV + newConstraints.toString() + 
                        "&distinct()") + 
                    "\">" + EDStatic.subsetRefineSubsetDownload + "</a>)\n");
            int onRows = subsetTable.nRows();
            if (viewValue[current] > 0) {  
                //this is last use of local copy of subsetTable, so okay to remove rows at end
                if (onRows > viewValue[current])
                    subsetTable.removeRows(viewValue[current], onRows);
                writer.flush(); //essential, since creating and using another writer to write the subsetTable
                TableWriterHtmlTable.writeAllAndFinish(loggedInAs, subsetTable, 
                    new OutputStreamSourceSimple(out), 
                    false, "", false, "", "", 
                    true, true, -1);          
            }

            writer.write("<p>" +
                MessageFormat.format(EDStatic.subsetNVariableCombos, "" + onRows) +
                "\n");
            if (onRows > viewValue[current])
                writer.write("<br><font class=\"warningColor\">" +
                    MessageFormat.format(EDStatic.subsetShowingNRows,
                        "" + Math.min(onRows, viewValue[current]),
                        "" + (onRows - viewValue[current])) + 
                    "</font>\n");
            else writer.write(EDStatic.subsetShowingAllRows + "\n");
            writer.write("<br>" +
                MessageFormat.format(EDStatic.subsetChangeShowing, 
                    "<tt>" + EDStatic.subsetView + " : " + viewTitle[current] + "</tt>") +
                "\n");

            // 4 = viewRelatedDataCounts
            current = 4;
            writer.write("\n" +
                "<br><a name=\"" + viewParam[current] + "\">&nbsp;</a><hr>\n" +
                "<p><b>" + viewTitle[current] + "</b>\n" +
                EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[current]));
            if (viewValue[current] == 1 && lastP >= 0) {  
                writer.write(
                    "&nbsp;&nbsp;\n" +
                    "(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".html?" +
                        XML.encodeAsHTMLAttribute(newConstraintsNLP.toString()) + //no vars, not distinct
                        "\">" + EDStatic.subsetRefineSubsetDownload + "</a>)\n");

                try {                    
                    //get the raw data
                    String fullCountsQuery = lastPName + newConstraintsNLP.toString();
                    TableWriterAll twa = new TableWriterAll(cacheDirectory(), 
                        suggestFileName(loggedInAs, fullCountsQuery, ".twa"));
                    getDataForDapQuery(loggedInAs, "", fullCountsQuery, //not distinct()
                        twa); 
                    PrimitiveArray varPA = twa.column(0);
                    Table countTable = new Table();
                    EDV edv = findDataVariableByDestinationName(subsetVariables[lastP]);
                    countTable.addColumn(0, lastPName, varPA, edv.combinedAttributes());

                    //sort, count, remove duplicates
                    varPA.sortIgnoreCase();
                    int n = varPA.size();
                    IntArray countPA = new IntArray(n, false);
                    countTable.addColumn(EDStatic.subsetCount, countPA);
                    int lastCount = 1;
                    countPA.add(lastCount);
                    keep = new BitSet(n);
                    keep.set(0, n); //initially, all set
                    for (int i = 1; i < n; i++) {
                        if (varPA.compare(i-1, i) == 0) {
                            keep.clear(i-1);
                            lastCount++;
                        } else {
                            lastCount = 1;
                        }
                        countPA.add(lastCount);
                    }
                    countTable.justKeep(keep);

                    //calculate percents
                    double stats[] = countPA.calculateStats();
                    double total = stats[PrimitiveArray.STATS_SUM];
                    n = countPA.size();
                    DoubleArray percentPA = new DoubleArray(n, false);
                    countTable.addColumn(EDStatic.subsetPercent, percentPA);
                    for (int i = 0; i < n; i++) 
                        percentPA.add(Math2.roundTo(countPA.get(i) * 100 / total, 2));

                    //write results
                    String countsConNLP = newConstraintsNLPHuman.toString();
                    writer.write("<br>" +  
                        (countsConNLP.length() == 0? 
                            EDStatic.subsetWhenNoConstraints : 
                            MessageFormat.format(EDStatic.subsetWhen, XML.encodeAsHTML(countsConNLP))) + 
                        (countsConNLP.length() < 40? " " : "\n<br>") +
                        MessageFormat.format(EDStatic.subsetWhenCounts,
                            EDStatic.subsetViewSelectRelatedCounts,
                            "" + countTable.nRows(), XML.encodeAsHTML(lastPName)) + 
                        "\n");
                    writer.flush(); //essential, since creating and using another writer to write the countTable
                    TableWriterHtmlTable.writeAllAndFinish(loggedInAs, countTable, 
                        new OutputStreamSourceSimple(out), 
                        false, "", false, "", "", 
                        true, false, -1);          
                    writer.write("<p>" +
                        MessageFormat.format(EDStatic.subsetTotalCount,
                            "" + Math2.roundToLong(total)) + 
                        "\n");
                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    String message = MustBe.getShortErrorMessage(t);
                    String2.log(String2.ERROR + ":\n" + MustBe.throwableToString(t)); //log full message with stack trace
                    writer.write( 
                        //"<p>An error occurred while getting the data:\n" +
                        "<pre>" + XML.encodeAsPreHTML(message, 120) +
                        "</pre>\n");                        
                }

            } else {
                writer.write("<p><font class=\"subduedColor\">" +
                    MessageFormat.format(EDStatic.subsetViewSelect,
                        EDStatic.subsetViewSelectRelatedCounts,
                        EDStatic.subsetView, viewTitle[current]) + 
                    "</font>\n" +
                    String2.replaceAll(EDStatic.subsetWarn, "<br>", " ") + "\n");
            }

            // 5 = viewRelatedData
            current = 5;
            writer.write("\n" +
                "<br><a name=\"" + viewParam[current] + "\">&nbsp;</a><hr>\n" +
                "<p><b>" + viewTitle[current] + "</b>\n" +
                EDStatic.htmlTooltipImage(loggedInAs, viewTooltip[current]) +
                "&nbsp;&nbsp;(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID +
                    ".das\">" + EDStatic.subsetMetadata + "</a>)\n" +
                "&nbsp;&nbsp;\n" +
                "(<a href=\"" + tErddapUrl + "/tabledap/" + datasetID + ".html" +
                    (newConstraints.length() == 0? "" : 
                        "?" +
                        XML.encodeAsHTMLAttribute(newConstraints.toString())) + //no vars specified, and not distinct()
                    "\">" + EDStatic.subsetRefineSubsetDownload + "</a>)\n");
            if (viewValue[current] > 0) {  
                if (allData) {
                    writer.write(
                        "\n<p><font class=\"subduedColor\">" +
                        MessageFormat.format(EDStatic.subsetViewCheck1,
                            viewTitle[current], ANY) +
                        "</font>\n");

                } else {
                    writer.flush(); //essential, since creating and using another writer to write the subsetTable
                    try {
                        //this shows only the first viewValue[current] rows of related data
                        TableWriterHtmlTable twht = new TableWriterHtmlTable(
                            loggedInAs, new OutputStreamSourceSimple(out),
                            false, "", false, "", "", 
                            true, true, viewValue[current]);          
                        if (handleViaFixedOrSubsetVariables(loggedInAs, requestUrl,
                            newConstraints.toString(), //no vars specified, and not distinct()
                            twht)) {}
                        else getDataForDapQuery(loggedInAs, "", 
                            newConstraints.toString(), //no vars specified, and not distinct()
                            twht);  

                        //counts
                        writer.write("<p>" +
                            MessageFormat.format(EDStatic.subsetNRowsRelatedData,
                                "" + twht.totalRows()) +
                            "\n");
                        if (twht.totalRows() > viewValue[current])
                            writer.write("<br><font class=\"warningColor\">" +
                                MessageFormat.format(EDStatic.subsetShowingNRows,
                                    "" + Math.min(twht.totalRows(), viewValue[current]),
                                    "" + (twht.totalRows() - viewValue[current])) +
                                "</font>\n");
                        else writer.write(EDStatic.subsetShowingAllRows + "\n");
                        writer.write("<br>" +
                            MessageFormat.format(EDStatic.subsetChangeShowing,
                                "<tt>" + EDStatic.subsetView + " : " + viewTitle[current] + "</tt>") +
                            "\n");

                        /*
                        //(simple, but inefficient since it gets all data then writes some)
                        //(more efficient: add stopAfterNRows to TableWriter.* and all users of it,
                        //   but that would be a lot of work)
                        TableWriterAllWithMetadata twa = new TableWriterAllWithMetadata(
                            cacheDirectory(), "subsetRelatedData");          
                        if (handleViaFixedOrSubsetVariables(loggedInAs, 
                            newConstraints.toString(), //no vars specified, and not distinct()
                            twa)) {}
                        else getDataForDapQuery(loggedInAs, "", 
                            newConstraints.toString(), //no vars specified, and not distinct()
                            twa);

                        //create a table with desired number of rows (in a memory efficient way)
                        long twaNRows = twa.nRows();
                        Table relatedTable = new Table();
                        String[] columnNames = twa.columnNames();
                        for (int col = 0; col < columnNames.length; col++) 
                            relatedTable.addColumn(col, columnNames[col], 
                                twa.column(col, viewValue[current]),
                                twa.columnAttributes(col));

                        //write table to HtmlTable
                        TableWriterHtmlTable.writeAllAndFinish(loggedInAs,
                            relatedTable, new OutputStreamSourceSimple(out),
                            false, "", false, "", "", 
                            true, true, -1);          

                        //counts
                        writer.write("<p>In total, there are " + twaNRows + 
                            " rows of related data.\n");
                        if (twaNRows > viewValue[current])
                            writer.write("<br><font class=\"warningColor\">" +
                                "The first " + Math.min(twaNRows, viewValue[current]) + " rows are shown above. " +
                                "The remaining " + (twaNRows - viewValue[current]) + " rows aren't being shown.</font>\n");
                        else writer.write("All of the rows are shown above.\n");
                        writer.write(
                            "<br>To change the maximum number of rows displayed, change <tt>View : " + 
                            viewTitle[current] + "</tt> above.\n");
                        */

                    } catch (Throwable t) {
                        EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                        String message = MustBe.getShortErrorMessage(t);
                        String2.log(String2.ERROR + ":\n" + MustBe.throwableToString(t)); //log full message with stack trace
                        writer.write( 
                            //"<p>An error occurred while getting the data:\n" +
                            "<pre>" + XML.encodeAsPreHTML(message, 120) +
                            "</pre>\n");                        
                    }
                }
            } else {
                writer.write("<p><font class=\"subduedColor\">" +
                    MessageFormat.format(EDStatic.subsetViewRelatedChange,
                        "<tt>" + EDStatic.subsetView + " : " + viewTitle[current] + "</tt>") +
                    "</font>\n" +
                    String2.replaceAll(EDStatic.subsetWarn, "<br>", " ") + "\n");
            }


        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
        }
        writer.write(EDStatic.endBodyHtml(tErddapUrl));
        writer.write("\n</html>\n");
        writer.flush(); //essential
    }

    /** 
     * This returns the subsetVariables data table.
     *
     * <p>This is thread-safe because the constructor (one thread) calls 
     *     distinctSubsetVariablesDataTable() which calls this and creates .nc file.
     *
     * @param loggedInAs this only matters for POST data (where the subsetVariables table
     *    is different for each loggedInAs!)
     * @return a table with the distinct() combinations of the subsetVariables.
     *    The table will have full metadata.
     * @throws Throwable if trouble  (e.g., not accessibleViaSubset())
     */
    public Table subsetVariablesDataTable(String loggedInAs) throws Throwable {
        String subsetFileName = subsetVariablesFileName(loggedInAs);

        //[WAS]The first user to request .subset causes it to be made.
        //  Thus, first user pays the price (time).
        //[NOW] The .subset.nc file is made by the constructor. 
        //  The file may be made needlessly, but no delay for first user.

        //read subsetTable from cached file?
        Table table = null;
        if (File2.isFile(datasetDir() + subsetFileName)) {
            table = new Table();
            table.readFlatNc(datasetDir() + subsetFileName, subsetVariables, 0);
            return table;
        }

        //is this dataset accessibleViaSubset?
        if (accessibleViaSubset().length() > 0) 
            throw new SimpleException(accessibleViaSubset());

        //Make the subsetVariablesDataTable.
        //If this fails, dataset won't load.  Locally, throws exception if failure.
        //Now: since subset file is made in the constructor, failure cases dataset to not load. That's okay.
        long time = System.currentTimeMillis();
        File2.makeDirectory(datasetDir());
           
        String tSubsetVars[] = subsetVariables();
        String subsetVariablesCSV = String2.toSVString(tSubsetVars, ",", false);

        // are the combinations available in a .json or .csv file created by ERDDAP admin?
        // <contentDir>subset/datasetID.json
        String adminSubsetFileName = EDStatic.contentDirectory + "subset/" + datasetID;
        if (File2.isFile(adminSubsetFileName + ".json")) {
            //json
            if (reallyVerbose) String2.log("* " + datasetID + 
                " is making subsetVariablesDataTable(" + loggedInAs + ")\n" +
                "from file=" + adminSubsetFileName + ".json");
            table = new Table();
            table.readJson(adminSubsetFileName + ".json");  //throws Exception if trouble

            //put column names in correct order
            //and ensure all of the desired columns are present
            for (int col = 0; col < tSubsetVars.length; col++) {
                int tCol = table.findColumnNumber(tSubsetVars[col]);
                if (tCol < 0)
                    throw new SimpleException("subsetVariable=" + tSubsetVars[col] + 
                        " wasn't found in " + adminSubsetFileName + ".json .");
                if (tCol > col)
                    table.moveColumn(tCol, col);
            }
            //remove excess columns
            table.removeColumns(tSubsetVars.length, table.nColumns());

        } else if (File2.isFile(adminSubsetFileName + ".csv")) {
            //csv
            if (reallyVerbose) String2.log("* " + datasetID + 
                " is making subsetVariablesDataTable(" + loggedInAs + ")\n" +
                "from file=" + adminSubsetFileName + ".csv");
            table = new Table();
            table.readASCII(adminSubsetFileName + ".csv", 0, 1,  //throws Exception if trouble
                null, null, null, //no tests
                tSubsetVars,   //file may have additional columns, but must have these
                false); //simplify (force below)

            //force data types to be as expected
            for (int col = 0; col < tSubsetVars.length; col++) {
                EDV edv = findDataVariableByDestinationName(tSubsetVars[col]);
                Class destClass = edv.destinationDataTypeClass();
                if (edv instanceof EDVTimeStamp) {
                    PrimitiveArray oldPa = table.getColumn(col);
                    int nRows = oldPa.size();
                    DoubleArray newPa = new DoubleArray(nRows, false);
                    for (int row = 0; row < nRows; row++)
                        newPa.add(Calendar2.safeIsoStringToEpochSeconds(oldPa.getString(row)));
                    table.setColumn(col, newPa);
                } else if (destClass != String.class) {
                    PrimitiveArray newPa = PrimitiveArray.factory(destClass, 1, false);
                    newPa.append(table.getColumn(col));
                    table.setColumn(col, newPa);
                }
            }
        } else {
            //See if subsetVariables are all fixedValue variables 
            table = new Table();
            boolean justFixed = true;
            for (int sv = 0; sv < tSubsetVars.length; sv++) {
                EDV edv = findDataVariableByDestinationName(tSubsetVars[sv]);
                if (edv.isFixedValue()) {
                    table.addColumn(table.nColumns(), edv.destinationName(), 
                        PrimitiveArray.factory(edv.destinationDataTypeClass(), 1, edv.sourceName().substring(1)), 
                        new Attributes(edv.combinedAttributes()));
                } else {
                    justFixed = false;
                    break;
                }
            }

            if (justFixed) {
                if (reallyVerbose) String2.log("* " + datasetID + 
                    " made subsetVariablesDataTable(" + loggedInAs + 
                    ") from just fixedValue variables.");
            } else {
                table = null;
            }
        }

        //if the table was created from a file created by ERDDAP admin or has just fixedValue variables ...
        if (table != null) {
            //at this point, we know the admin-supplied file has just the 
            //correct columns in the correct order.

            //ensure sorted (in correct order), then remove duplicates
            int cols[] = (new IntArray(0, tSubsetVars.length - 1)).toArray();
            boolean order[] = new boolean[tSubsetVars.length];
            Arrays.fill(order, true);
            table.sortIgnoreCase(cols, order);
            table.removeDuplicates();
            table.globalAttributes().add(combinedGlobalAttributes()); 

            //save it as subset file
            table.saveAsFlatNc(datasetDir() + subsetFileName, ROW_NAME);
            if (verbose) String2.log("* " + datasetID + 
                " made subsetVariablesDataTable(" + loggedInAs + ") in time=" +
                (System.currentTimeMillis() - time));
            return table;
        }

        //avoid recursion leading to stack overflow from 
        //  repeat{getDataForDapQuery -> get subsetVariablesDataTable}
        //if subsetVariables are defined and that data is only available from an admin-made table
        //(which is absent) (and data is not available from data source).
        String stack = MustBe.getStackTrace();
        //String2.log("stack=\n" + stack);
        int count = String2.countAll(stack, 
            "at gov.noaa.pfel.erddap.dataset.EDDTable.subsetVariablesDataTable(");
        if (count >= 2) 
            throw new RuntimeException( 
                "Unable to create subsetVariablesDataTable. (in danger of stack overflow)\n" +
                "(Perhaps the required, admin-provided subset file in " +
                    "[tomcat]/content/erddap/subset/\n" +
                "wasn't found or couldn't be read.)");

        //else request the data for the subsetTable from the source
        if (reallyVerbose) String2.log("* " + datasetID + 
            " is making subsetVariablesDataTable(" + loggedInAs + 
            ") from source data...");
        String svDapQuery = subsetVariablesCSV + "&distinct()";
        //don't use getTwawmForDapQuery() since it tries to handleViaFixedOrSubsetVariables
        //  since this method is how subsetVariables gets its data!
        TableWriterAllWithMetadata twawm = new TableWriterAllWithMetadata(cacheDirectory(), subsetFileName);
        TableWriterDistinct twd = new TableWriterDistinct(cacheDirectory(), subsetFileName, twawm);
        getDataForDapQuery(loggedInAs, "", svDapQuery, twd);  
        table = twawm.cumulativeTable();
        table.convertToStandardMissingValues();

        //save it
        table.saveAsFlatNc(datasetDir() + subsetFileName, ROW_NAME);
        if (verbose) String2.log("* " + datasetID + 
            " made subsetVariablesDataTable(" + loggedInAs + ").  time=" +
            (System.currentTimeMillis() - time));


        //done
        return table;
    }

    /** 
     * This returns the distinct subsetVariables data table.
     * NOTE: the columns are unrelated!  Each column is sorted separately!
     *
     * <p>This is thread-safe because the constructor (one thread) calls 
     *     distinctSubsetVariablesDataTable() which calls this and makes .nc file.
     *
     * @param loggedInAs this only matters for POST data (where the distinct subsetVariables table
     *    is different for each loggedInAs!)
     * @param loadVars the specific destinationNames to be loaded (or null for all subsetVariables)
     * @return the table with one column for each subsetVariable.
     *    The columns are sorted separately and have DIFFERENT SIZES!
     *    So it isn't a valid table!
     *    The table will have full metadata.
     * @throws Throwable if trouble  (e.g., not accessibleViaSubset())
     */
    public Table distinctSubsetVariablesDataTable(String loggedInAs, String loadVars[]) throws Throwable {

        String fullDistinctFileName = datasetDir() + distinctSubsetVariablesFileName(loggedInAs);
        Table distinctTable = null;

        //read from cached distinct.nc file?
        if (File2.isFile(fullDistinctFileName)) {
            distinctTable = new Table();
            StringArray varNames = new StringArray();
            PrimitiveArray pas[] = NcHelper.readPAsInNc(fullDistinctFileName, loadVars, varNames);
            for (int v = 0; v < varNames.size(); v++) 
                distinctTable.addColumn(v, varNames.get(v), pas[v],
                    new Attributes(findDataVariableByDestinationName(varNames.get(v)).combinedAttributes()));
            distinctTable.globalAttributes().add(combinedGlobalAttributes());
            return distinctTable;
        }  

        //is this dataset accessibleViaSubset?
        String tSubsetVars[] = subsetVariables();
        if (tSubsetVars.length == 0 ||
            accessibleViaSubset().length() > 0) 
            throw new SimpleException(accessibleViaSubset());


        //create the table and store it in the file
        //**** NOTE: the columns are unrelated!  Each column is sorted separately!
        if (reallyVerbose) String2.log("* " + datasetID + 
            " is making distinctSubsetVariablesDataTable(" + loggedInAs + ")");
        long time = System.currentTimeMillis();

        //read the combinations table
        //this will throw exception if trouble
        //* this also ensures datasetDir() exists
        distinctTable = subsetVariablesDataTable(loggedInAs);

        //find the distinct values
        PrimitiveArray distinctPAs[] = new PrimitiveArray[tSubsetVars.length];
        for (int v = 0; v < tSubsetVars.length; v++) {
            distinctPAs[v] = distinctTable.getColumn(v);
            distinctPAs[v].sortIgnoreCase();
            distinctPAs[v].removeDuplicates(false);  
        }

        //store in file 
        int randomInt = Math2.random(Integer.MAX_VALUE);
        try {
            NcHelper.writePAsInNc(fullDistinctFileName + randomInt, 
                new StringArray(tSubsetVars), distinctPAs);
            //sway into place (atomically, in case other threads want this info)
            File2.rename(fullDistinctFileName + randomInt,
                         fullDistinctFileName);
        } catch (Throwable t) {
            //delete any partial file
            File2.delete(fullDistinctFileName + randomInt);
            throw t;
        }
        if (verbose) String2.log("* " + datasetID + 
            " made distinctSubsetVariablesDataTable(" + loggedInAs + ").  time=" +
            (System.currentTimeMillis() - time) + " (includes subsetVariablesDataTable() time)");


        
        //*** look for and print out values with \r or \n
        //do this after saving the file (so it is quickly accessible to other threads)
        StringBuilder results = new StringBuilder();
        boolean datasetIdPrinted = false;
        for (int v = 0; v < tSubsetVars.length; v++) {
            PrimitiveArray pa = distinctPAs[v];
            if (!(pa instanceof StringArray))
                continue;
            boolean columnNamePrinted = false;
            int nRows = pa.size();
            for (int row = 0; row < nRows; row++) {
                String s = pa.getString(row);
                if (s.length() == 0)
                    continue;
                if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {

                    //troubled value found!
                    if (!datasetIdPrinted) {
                        results.append(
                            "WARNING: for datasetID=" + datasetID + "\n" +
                            "(sourceUrl=" + publicSourceUrl() + "),\n" +
                            "EDDTable.distinctSubsetVariablesDataTable found carriageReturns ('\\r' below)\n" +
                            "or newlines ('\\n' below) in some of the data values.\n" + 
                            "Since this makes data searches error-prone and causes ERDDAP's .subset web\n" +
                            "page to fail, it would be great if you could remove the offending characters.\n" +
                            "Thank you for looking into this.\n\n");
                        datasetIdPrinted = true;
                    }
                    if (!columnNamePrinted) {
                        results.append("sourceName=" +
                            findDataVariableByDestinationName(tSubsetVars[v]).sourceName() + 
                            "\n");
                        columnNamePrinted = true;
                    }
                    //print the troubled value
                    //quoteIfNeeded converts carriageReturns/newlines to (char)166; //'¦'  (#166)
                    //was results.append("  " + String2.quoteIfNeeded(true, s) + "\n");
                    results.append("  " + String2.toJson(s) + "\n");
                }
            }
            if (columnNamePrinted)
                results.append('\n');
        }
        if (results.length() > 0) {
            String2.log("");
            String2.log(results.toString());
            EDStatic.email(EDStatic.emailEverythingTo, 
                "WARNING: datasetID=" + datasetID + " has carriageReturns or newlines",
                results.toString());
        }

        //add original units
        //for (int v = 0; v < nCols; v++) {
        //    EDV edv = findDataVariableByDestinationName(distinctTable.getColumnName(v));
        //    distinctTable.columnAttributes(v).set("units", edv.units());
        //}

        //just the loadVars
        if (loadVars != null) {
            for (int v = 0; v < loadVars.length; v++) 
                distinctTable.moveColumn(distinctTable.findColumnNumber(loadVars[v]), v);
            distinctTable.removeColumns(loadVars.length, distinctTable.nColumns());
        }

        return distinctTable;
    }
    
    /** 
     * This returns a table with the specified columns and 0 rows active, 
     *    suitable for catching source results. 
     * There are no attributes, since attributes are added in standardizeResultsTable.
     *
     * @param resultsDVIs  the indices of the results dataVariables.
     * @@param capacityNRows
     * @return an empty table with the specified columns and 0 rows active, 
     *    suitable for catching source results. 
     */
    public Table makeEmptySourceTable(int resultsDVIs[], int capacityNRows) {
        int nDV = resultsDVIs.length;
        EDV resultsEDVs[] = new EDV[nDV];
        for (int rv = 0; rv < nDV; rv++) 
            resultsEDVs[rv] = dataVariables[resultsDVIs[rv]];
        return makeEmptySourceTable(resultsEDVs, capacityNRows);
    }

    /** 
     * This returns a table with the specified columns and 0 rows active, 
     *    suitable for catching source results. 
     * There are no attributes, since attributes are added in standardizeResultsTable.
     *
     * @param resultsEDVs
     * @@param capacityNRows
     * @return an empty table with the specified columns and 0 rows active, 
     *    suitable for catching source results. 
     */
    public Table makeEmptySourceTable(EDV resultsEDVs[], int capacityNRows) {
        int nRv = resultsEDVs.length;
        PrimitiveArray paArray[] = new PrimitiveArray[nRv];
        Table table = new Table();
        for (int rv = 0; rv < nRv; rv++) {
            EDV edv = resultsEDVs[rv];
            table.addColumn(edv.sourceName(), 
                PrimitiveArray.factory(edv.sourceDataTypeClass(), capacityNRows, false));
        }
        //String2.log("\n>>makeEmptySourceTable cols=" + table.getColumnNamesCSVString());
        return table;
    }


    /** 
     * This is like distinctOptionsTable, but doesn't place a limit on the max length
     * of the options.
     */
    public String[][] distinctOptionsTable(String loggedInAs) throws Throwable {
        return distinctOptionsTable(loggedInAs, Integer.MAX_VALUE);
    }

    /** 
     * This is like distinctSubsetVariablesDataTable, but the values
     * are ready to be used as options in a constraint (e.g., String values are in quotes).
     *
     * @param loggedInAs null if user not logged in.
     * @param maxChar options that are longer than maxChar are removed
     *  (and if so, "[Some really long options aren't shown.]" is added at the end).
     * @return the left array has a value for each subsetVariable.
     * @throws exception if trouble (e.g., dataset isn't accessibleViaSubset)
     */
    public String[][] distinctOptionsTable(String loggedInAs, int maxChar) throws Throwable {
        //get the distinctTable and convert to distinctOptions (String[]'s with #0="")   
        Table distinctTable = distinctSubsetVariablesDataTable(loggedInAs, subsetVariables());
        String distinctOptions[][] = new String[subsetVariables.length][];
        for (int sv = 0; sv < subsetVariables.length; sv++) {
            EDV edv = findDataVariableByDestinationName(subsetVariables[sv]);
            PrimitiveArray pa = distinctTable.getColumn(sv);

            //avoid excess StringArray conversions (which canonicalize the Strings)
            if (pa instanceof StringArray) {

                StringArray sa = (StringArray)pa;
                int n = sa.size();

                //remove strings longer than maxChar
                if (maxChar > 0 && maxChar < Integer.MAX_VALUE) {
                    BitSet bitset = new BitSet(n); 
                    for (int i = 0; i < n; i++)
                        bitset.set(i, sa.get(i).length() <= maxChar);                    
                    sa.justKeep(bitset);
                    if (bitset.nextClearBit(0) < n)  //if don't keep 1 or more... (nextClearBit returns n if none)
                        sa.add(EDStatic.subsetLongNotShown);
                    n = sa.size();
                }

                //convert String variable options to JSON strings
                distinctOptions[sv] = new String[n + 1];
                distinctOptions[sv][0] = "";
                for (int i = 0; i < n; i++) { //1.. since 0 is ""
                    distinctOptions[sv][i + 1] = String2.toJson(sa.get(i));
                }

            } else if (edv instanceof EDVTimeStamp) { 

                //convert epochSeconds to iso Strings
                String tTime_precision = ((EDVTimeStamp)edv).time_precision();
                int n = pa.size();
                distinctOptions[sv] = new String[n + 1];
                distinctOptions[sv][0] = "";
                for (int i = 0; i < n; i++) { 
                    distinctOptions[sv][i + 1] = 
                        Calendar2.epochSecondsToLimitedIsoStringT(
                            tTime_precision, pa.getDouble(i), "NaN");
                }

            } else { 
                //convert numeric options to Strings
                int n = pa.size();
                distinctOptions[sv] = new String[n + 1];
                distinctOptions[sv][0] = "";
                for (int i = 0; i < n; i++)
                    distinctOptions[sv][i + 1] = pa.getString(i);

                //convert final NaN (if present) from "" to NaN
                if (distinctOptions[sv][n].length() == 0)
                    distinctOptions[sv][n] = "NaN";
            }
        }

        return distinctOptions;
    }


    /**
     * This sees if the request can be handled by just fixed values,
     * or the cached subsetVariables or distinct subsetVariables table,
     * 
     * <p>This ignores constraints like orderBy() that are handled by the 
     *    construction of the tableWriter (by making a chain or tableWriters).
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *   I think it's currently just used to add to "history" metadata.
     * @param userDapQuery the part of the user's request after the '?', 
     *    still percentEncoded, may be null.
     * @param tableWriter
     * @returns true if it was handled
     * @throws throwable e.g., if it should have been handled, but failed.
     *    Or if no data.
     */
    public boolean handleViaFixedOrSubsetVariables(String loggedInAs, String requestUrl,
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        if (userDapQuery == null)
            userDapQuery = "";

        //parse the query
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        parseUserDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues, //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            false); //don't repair

        //can query be handled by just fixed value variables?
        //If so, the response is just one row with fixed values 
        //  (if constraints don't make it 0 rows).
        boolean justFixed = true;
        Table table = new Table();
        for (int rv = 0; rv < resultsVariables.size(); rv++) {
            EDV edv = findDataVariableByDestinationName(resultsVariables.get(rv));
            if (edv.isFixedValue()) {
                table.addColumn(table.nColumns(), edv.destinationName(), 
                    PrimitiveArray.factory(edv.destinationDataTypeClass(), 1, edv.sourceName().substring(1)), 
                    new Attributes(edv.combinedAttributes()));
            } else {
                justFixed = false;
                break;
            }
        }
        if (justFixed) {
            BitSet keep = new BitSet();
            keep.set(0);
            for (int cv = 0; cv < constraintVariables.size(); cv++) {
                EDV edv = findDataVariableByDestinationName(constraintVariables.get(cv));
                if (edv.isFixedValue()) {
                    //is it true?
                    PrimitiveArray pa = PrimitiveArray.factory(edv.destinationDataTypeClass(), 
                        1, edv.sourceName().substring(1));
                    if (pa.applyConstraint(keep, constraintOps.get(cv), constraintValues.get(cv)) == 0)
                        throw new SimpleException(MustBe.THERE_IS_NO_DATA);
                } else {
                    justFixed = false;
                    break; 
                }
            }

            //succeeded?
            if (justFixed) {
                setResponseGlobalAttributes(requestUrl, userDapQuery, table);
                tableWriter.writeAllAndFinish(table);
                if (verbose) String2.log(datasetID + 
                    ".handleViaFixedOrSubsetVariables got the data from fixedValue variables.");               
                return true;
            }
        }

        //*** Handle request via subsetVariables?
        //quick check: ensure &distinct() is in the query?
        //No, assume: if request is just subsetVariables, distinct() is implied?
        //if (userDapQuery.indexOf("&distinct()") < 0)
        //    return false;
        table = null;

        //quick check: ensure subsetVariables system is active
        String tSubsetVars[] = subsetVariables();
        if (tSubsetVars.length == 0 ||
            accessibleViaSubset().length() > 0) 
            return false;

        //ensure query can be handled completely by just subsetVars
        for (int rv = 0; rv < resultsVariables.size(); rv++)
            if (String2.indexOf(tSubsetVars, resultsVariables.get(rv)) < 0)
                return false;
        for (int cv = 0; cv < constraintVariables.size(); cv++)
            if (String2.indexOf(tSubsetVars, constraintVariables.get(cv)) < 0)
                return false;

        //OK. This method will handle the request.
        long time = System.currentTimeMillis();

        //If this just involves 1 column, get distinctSubsetVariablesDataTable (it's smaller)
        //else get subsetVariablesDataTable
        boolean justOneVar = resultsVariables.size() == 1;
        if (justOneVar) {
            String varName = resultsVariables.get(0);
            for (int con = 0; con < constraintVariables.size(); con++) {
                if (!varName.equals(constraintVariables.get(con))) {
                    justOneVar = false;
                    break;
                }
            }
        }
        if (justOneVar) {
            table = distinctSubsetVariablesDataTable(loggedInAs, 
                new String[]{resultsVariables.get(0)});
        } else {
            table = subsetVariablesDataTable(loggedInAs);
        }

        //apply constraints, rearrange columns, add metadata 
        applyConstraints(table, true, //applyAllConstraints
            resultsVariables, 
            constraintVariables, constraintOps, constraintValues);
        setResponseGlobalAttributes(requestUrl, userDapQuery, table);

        //write to tableWriter
        tableWriter.writeAllAndFinish(table);
        if (verbose) String2.log(datasetID + ".handleViaFixedOrSubsetVariables got the data from the " +
            (justOneVar? "distinctSubset" : "subset") +
            " file.  time=" +
            (System.currentTimeMillis() -  time));
       
        return true;
    }


    /** 
     * This sets accessibleViaSubset and returns the array of 
     *  dataVariable destinationNames for use by .subset (or String[0] if unused).
     *
     * <p>NOTE: The first call to this is the first access to this system for this dataset.
     * So, it causes the cached subset file to be deleted.
     *
     */
    public String[] subsetVariables() {
        if (subsetVariables == null || 
            accessibleViaSubset == null) {

            String tAccessibleViaSubset = "";
            String tSubsetVariables[];
            String subsetVariablesCSV = combinedGlobalAttributes().getString("subsetVariables");
            if (subsetVariablesCSV == null || subsetVariablesCSV.length() == 0) {
                tSubsetVariables = new String[0];
            } else {            
                StringArray sa = StringArray.wordsAndQuotedPhrases(subsetVariablesCSV);
                int n = sa.size();
                tSubsetVariables = new String[n];
                for (int i = 0; i < n; i++) {
                    int po = String2.indexOf(dataVariableDestinationNames(), sa.get(i));
                    if (po >= 0)
                        tSubsetVariables[i] = sa.get(i);
                    else {
                        //tAccessibleViaSubset = 
                        throw new SimpleException(
                            "<subsetVariables> wasn't set up correctly: \"" + 
                            sa.get(i) + "\" isn't a valid variable destinationName.");
                        //String2.log(String2.ERROR + " for datasetID=" + datasetID + ": " + tAccessibleViaSubset);
                        //tSubsetVariables = new String[0];
                        //break;
                    }
                }
            }
            if (tSubsetVariables.length == 0)
                tAccessibleViaSubset = EDStatic.subsetNotSetUp;

            if ((className.equals("EDDTableFromDapSequence") ||
                 className.equals("EDDTableFromErddap")) && 
                EDStatic.quickRestart && 
                EDStatic.initialLoadDatasets() && 
                File2.isFile(quickRestartFullFileName())) {

                //don't delete subset and distinct files  (to reuse them)
                if (verbose) 
                    String2.log("  quickRestart: reusing subset and distinct files");

            } else {

                //delete the subset and distict files if they exist (to force recreation)
                if (File2.isDirectory(datasetDir())) {

                    //DELETE the cached distinct combinations file.
                    //the subsetVariables may have changed (and the dataset probably just reloaded)
                    //  so delete the file (or files for POST datasets) with all of the distinct combinations
                    //see subsetVariablesFileName());
                    RegexFilenameFilter.regexDelete(datasetDir(), 
                        ".*" + //.* handles POST files which have loggedInAs in here
                            String2.replaceAll(DISTINCT_SUBSET_FILENAME, ".", "\\."),
                        false);

                    //DELETE the new-style cached .nc subset table files in the subdir of /datasetInfo/
                    RegexFilenameFilter.regexDelete(datasetDir(), 
                        ".*" + //.* handles POST files which have loggedInAs in here
                            String2.replaceAll(SUBSET_FILENAME, ".", "\\."),
                        false);
                }
            }

            //LAST: set in an instant  [Now this is always done by constructor -- 1 thread]
            subsetVariables = tSubsetVariables; 
            accessibleViaSubset = String2.canonical(tAccessibleViaSubset);
        }

        return subsetVariables;
    }

    /**
     * This indicates why the dataset isn't accessible via .subset
     * (or "" if it is).
     */
    public String accessibleViaSubset() {
        if (accessibleViaSubset == null) 
            subsetVariables(); //it sets accessibleViaSubset, so it can be returned below

        return accessibleViaSubset;
    }

    /**
     * This is called by EDDTable.ensureValid by subclass constructors,
     * to go through the metadata of the dataVariables and gather
     * all unique sosObservedProperties.
     */
    /*protected void gatherSosObservedProperties() {
        HashSet set = new HashSet(Math2.roundToInt(1.4 * dataVariables.length));
        for (int dv = 0; dv < dataVariables.length; dv++) {
            String s = dataVariables[dv].combinedAttributes().getString("observedProperty");
            if (s != null)
               set.add(s);
        }
        sosObservedProperties = String2.toStringArray(set.toArray()); 
        Arrays.sort(sosObservedProperties);
    }*/


    /**
     * This indicates why the dataset isn't accessible via Make A Graph
     * (or "" if it is).
     */
    public String accessibleViaMAG() {
        if (accessibleViaMAG == null) {
            int nonAxisNumeric = 0;
            for (int dv = 0; dv < dataVariables.length; dv++) {
                if (dv == lonIndex ||
                    dv == latIndex ||
                    dv == altIndex ||
                    dv == depthIndex ||
                    dv == timeIndex ||
                    dataVariables[dv].destinationDataTypeClass() == String.class) 
                    continue;
                
                nonAxisNumeric++;
            }
            if (nonAxisNumeric == 0)
                accessibleViaMAG = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause2, 
                        EDStatic.mag, EDStatic.noXxxNoNonString));
            else if (lonIndex < 0 && latIndex < 0 && timeIndex < 0 && nonAxisNumeric < 2)
                accessibleViaMAG = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause2, 
                        EDStatic.mag, EDStatic.noXxxNo2NonString));
            else accessibleViaMAG = String2.canonical("");
        }
        return accessibleViaMAG;
    }

    /** 
     * This returns an error message regarding why the dataset isn't accessible via SOS 
     * (or "" if it is).
     * ???No restriction on lon +/-180 vs. 0-360?
     * ???Currently, sosOffering must be "station" 
     *  (because of assumption of 1 lat,lon in getObservations.
     */
    public String accessibleViaSOS() {
        if (accessibleViaSOS == null) {

            String cdt = combinedGlobalAttributes().getString("cdm_data_type");
            int type = String2.indexOf(sosCdmDataTypes, cdt);

            //easy tests make for a better error message
            if (!EDStatic.sosActive)
                accessibleViaSOS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "SOS", 
                        MessageFormat.format(EDStatic.noXxxNotActive, "SOS")));
            else if (type < 0) 
                accessibleViaSOS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "SOS", 
                        MessageFormat.format(EDStatic.noXxxNoCdmDataType, cdt)));
            else if (lonIndex < 0 || latIndex < 0 || timeIndex < 0)
                accessibleViaSOS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "SOS", EDStatic.noXxxNoLLT));
            else if (sosOfferingType == null ||
                     sosOfferingIndex < 0 ||
                     sosMinLon == null  || sosMaxLon == null ||
                     sosMinLat == null  || sosMaxLat == null ||
                     sosMinTime == null || sosMaxTime == null ||
                     sosOfferings == null)
                accessibleViaSOS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "SOS", EDStatic.noXxxNoMinMax));
            else accessibleViaSOS = String2.canonical("");
        }
        return accessibleViaSOS;
    }

    /**
     * If the subclass' constructor didn't setup this dataset for the SOS server,
     * EDDTable.ensureValid calls this to try to do a generic SOS setup. 
     * (This must be called *after* subsetVariablesDataTable has been created.)
     * This generic SOS setup uses entire time range (not each offerings time range).
     * If successful, this sets accessibleViaSOS to "".
     */
    public void genericSosSetup() {
/*
        if (!EDStatic.sosActive) 
            return;

        if (lonIndex >= 0 && latIndex >= 0 && timeIndex >= 0 &&
            setSosOfferingTypeAndIndex()) { //compatible cdm_data_type

            //are sosOfferingIndex, lon, lat in subsetVariablesDataTable
            //This will only be true if lon and lat are point per offering.
            String sosOfferingDestName = dataVariables[sosOfferingIndex].destinationName();
            ...

            //request distinct() sosOfferingDestName, lon, lat
            //This should succeed. So don't protect against failure. 
            //So failure causes dataset to be not loaded.
            Table table = new Table();
            ...

            //copy the results to the sos PAs
            sosOfferings = table.getColumn(0);
            sosMinLon    = table.getColumn(1);
            sosMaxLon    = sosMinLon;
            sosMinLat    = table.getColumn(2);
            sosMaxLat    = sosMinLat;
            sosMinTime   = PrimitiveArray.factory(double.class, 8, true);
            sosMaxTime   = sosMinTime;

            //success
            accessibleViaSOS = "";         
        } 
*/    }

    /** 
     * This indicates why the dataset isn't accessible via ESRI GeoServices REST
     * (or "" if it is).
     */
    public String accessibleViaGeoServicesRest() {
        if (accessibleViaGeoServicesRest == null) {

            if (!EDStatic.geoServicesRestActive)
                accessibleViaGeoServicesRest = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "GeoServicesRest", 
                        MessageFormat.format(EDStatic.noXxxNotActive, "GeoServicesRest")));
            else accessibleViaGeoServicesRest = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "GeoServicesRest", 
                        EDStatic.noXxxItsTabular));
        }
        return accessibleViaGeoServicesRest;
    }
     
    /** 
     * This indicates why the dataset isn't accessible via WCS
     * (or "" if it is).
     */
    public String accessibleViaWCS() {
        if (accessibleViaWCS == null) {

            if (!EDStatic.wcsActive)
                accessibleViaWCS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "WCS", 
                        MessageFormat.format(EDStatic.noXxxNotActive, "WCS")));
            else accessibleViaWCS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "WCS", 
                        EDStatic.noXxxItsTabular));
        }
        return accessibleViaWCS;
    }
     
    /** 
     * This indicates why the dataset isn't accessible via WMS
     * (or "" if it is).
     */
    public String accessibleViaWMS() {
        if (accessibleViaWMS == null)
            if (!EDStatic.wmsActive)
                accessibleViaWMS = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause, "WMS", 
                        MessageFormat.format(EDStatic.noXxxNotActive, "WMS")));
            else accessibleViaWMS = String2.canonical(
                MessageFormat.format(EDStatic.noXxxBecause, "WMS", 
                    EDStatic.noXxxItsTabular));
        return accessibleViaWMS;
    }
     

    /** 
     * This indicates why the dataset isn't accessible via .ncCF and .ncCFMA file type
     * (or "" if it is).
     * Currently, this is only for some of the Discrete Sampling Geometries
     * (was PointObservationConventions) cdm_data_type 
     * representations at
     * http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries
     *
     * @throws SimpleException if trouble (e.g., something set up wrong).
     */
    public String accessibleViaNcCF() {

        if (accessibleViaNcCF == null) {

            //!!! For thread safety, don't use temporary value of accessibleViaNcCF.
            //Just set it with the final value; don't change it. 
            String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
            String lcCdmType = cdmType == null? null : cdmType.toLowerCase();
            if (cdmType == null || cdmType.length() == 0) 
                throw new SimpleException(
                    "cdm_data_type must be specified in globalAttributes.");
            //if (!cdmType.equals(CDM_POINT) &&
            //    !cdmType.equals(CDM_PROFILE) &&
            //    !cdmType.equals(CDM_TIMESERIES) &&
            //    !cdmType.equals(CDM_TIMESERIESPROFILE) &&
            //    !cdmType.equals(CDM_TRAJECTORY) &&
            //    !cdmType.equals(CDM_TRAJECTORYPROFILE)) {

            String startCdm  = "cdm_data_type=" + cdmType + ", but ";
            StringArray requiredRequestVars = new StringArray();
            //a list of all cdm_profile+timeseries+trajectory_variables
            //each data request must include at least one variable *not* on this list
            StringArray outerVars = new StringArray();

            //set cdmUses  (e.g., TimeSeriesProfile uses timeseries and profile)
            String cdmLCNames[] = new String[]{"profile", "timeseries", "trajectory"};
            boolean cdmUses[] = new boolean[3];
            for (int cdmi = 0; cdmi < 3; cdmi++) 
                cdmUses[cdmi] = cdmType.toLowerCase().indexOf(cdmLCNames[cdmi]) >= 0;

            //look for the cf_role=timeseries_id (and profile_id and trajectory_id) columns
            //  (or ensure not present if wrong cdmType)
            int cdmIdIndex[] = new int[]{-1, -1, -1};
            int ndv = dataVariables.length;
            for (int dv = 0; dv < ndv; dv++) {
                String cfRole = dataVariables[dv].combinedAttributes().getString("cf_role");
                for (int cdmi = 0; cdmi < 3; cdmi++) {
                    if ((cdmLCNames[cdmi] + "_id").equals(cfRole)) {
                        if (cdmUses[cdmi]) {
                            //already set!
                            if (cdmIdIndex[cdmi] >= 0) 
                                throw new SimpleException(
                                    "cf_role=" + cdmLCNames[cdmi] + 
                                    "_id must be defined for *only* one variable (see " +
                                    dataVariables[cdmIdIndex[cdmi]].destinationName() + " and " + 
                                    dataVariables[dv].destinationName() + ").");
                            //set it
                            cdmIdIndex[cdmi] = dv;
                        } else {
                            //shouldn't be defined
                            throw new SimpleException("For cdm_data_type=" + cdmType + 
                                ", no variable should have cf_role=" + 
                                cdmLCNames[cdmi] + "_id (see " +
                                dataVariables[dv].destinationName() + ").");
                        }
                    }
                }
            }
            //ensure the required cf_role=xxx_id were set
            for (int cdmi = 0; cdmi < 3; cdmi++) {
                if (cdmUses[cdmi] && cdmIdIndex[cdmi] < 0) 
                    throw new SimpleException("For cdm_data_type=" + cdmType + 
                        ", one variable must have cf_role=" + cdmLCNames[cdmi] + "_id.");
            }
            profile_idIndex    = cdmIdIndex[0];
            timeseries_idIndex    = cdmIdIndex[1];
            trajectory_idIndex = cdmIdIndex[2]; 
            //???set old sosOfferingIndex to one of these? How is that used?

            //look for cdm_xxx_variables   (or ensure they aren't present if they should be)
            String cdmVars[][] = new String[3][];  //subarray may be size=0, won't be null
            for (int cdmi = 0; cdmi < 3; cdmi++) {
                cdmVars[cdmi] = combinedGlobalAttributes.getStringsFromCSV(
                    "cdm_" + cdmLCNames[cdmi] + "_variables");
                if (cdmVars[cdmi] == null)
                    cdmVars[cdmi] = new String[0];
                if (cdmUses[cdmi]) {
                    if (cdmVars[cdmi].length == 0) 
                        //should be set, but isn't
                        throw new SimpleException("For cdm_data_type=" + cdmType + 
                            ", the global attribute cdm_" + cdmLCNames[cdmi] +
                            "_variables must be set.");
                    for (int var = 0; var < cdmVars[cdmi].length; var++) {
                        //unknown variable
                        if (String2.indexOf(dataVariableDestinationNames(), cdmVars[cdmi][var]) < 0) 
                            throw new SimpleException( 
                                "cdm_" + cdmLCNames[cdmi] + "_variables #" + var + "=" + 
                                cdmVars[cdmi][var] + 
                                " isn't one of the data variable destinationNames.");
                    }
                } else {
                    if (cdmVars[cdmi].length > 0) 
                        //is set, but shouldn't be
                        throw new SimpleException("For cdm_data_type=" + cdmType + 
                            ", the global attribute cdm_" + cdmLCNames[cdmi] +
                            "_variables must *not* be set.");
                }
            }


            //ensure profile datasets have altitude, depth or cdm_altitude_proxy
            String proxy = combinedGlobalAttributes.getString("cdm_altitude_proxy");
            int proxyDV = String2.indexOf(dataVariableDestinationNames(), proxy);
            if (altIndex >= 0) {
                if (proxy != null && !proxy.equals("altitude"))
                    throw new SimpleException("If the dataset has an altitude variable, " +
                        "don't define the global attribute cdm_altitude_proxy.");
            } else if (depthIndex >= 0) {
                if (proxy != null && !proxy.equals("depth"))
                    throw new SimpleException("If the dataset has a depth variable, " +
                        "don't define the global attribute cdm_altitude_proxy.");  
            } else if (proxyDV >= 0) {
                //okay
                EDV proxyEDV = dataVariables[proxyDV];
                proxyEDV.addAttributes(     ).add("_CoordinateAxisType", "Height");
                proxyEDV.combinedAttributes().add("_CoordinateAxisType", "Height");
                proxyEDV.addAttributes(     ).add("axis", "Z");
                proxyEDV.combinedAttributes().add("axis", "Z");

            } else if (cdmUses[0]) {
                throw new SimpleException("For cdm_data_type=" + cdmType + 
                    ", when there is no altitude or depth variable, " +
                    "you MUST define the global attribute cdm_altitude_proxy.");
            }


            //ensure there is no overlap of cdm_xxx_variables
            //test profile and timeseries
            StringArray sa1 = new StringArray((String[])cdmVars[0].clone());
            StringArray sa2 = new StringArray((String[])cdmVars[1].clone());
            sa1.sort();
            sa2.sort();
            sa1.inCommon(sa2);
            if (sa1.size() > 0)
                throw new SimpleException(
                    "There must not be any variables common to cdm_profile_variables and " +
                    "cdm_timeseries_variables. Please fix these: " + sa1.toString() + ".");

            //test timeseries and trajectory   -- actually, they shouldn't coexist
            if (cdmVars[1].length > 0 && cdmVars[2].length > 0)
                throw new SimpleException(
                    "cdm_timeseries_variables and cdm_trajectory_variables must never both be defined.");

            //test trajectory and profile
            sa1 = new StringArray((String[])cdmVars[2].clone());
            sa2 = new StringArray((String[])cdmVars[0].clone());
            sa1.sort();
            sa2.sort();
            sa1.inCommon(sa2);
            if (sa1.size() > 0)
                throw new SimpleException(
                  "There must not be any variables common to cdm_profile_variables and " +
                  "cdm_trajectory_variables. Please fix these: " + sa1.toString() + ".");


            //check the cdmType's requirements
            if (accessibleViaNcCF != null) {
                //error message already set

            } else if (cdmType == null || cdmType.length() == 0) {
                throw new SimpleException(
                    "cdm_data_type wasn't specified in globalAttributes.");

            //Point
            } else if (cdmType.equals(CDM_POINT)) {
                //okay!

            //Profile
            } else if (cdmType.equals(CDM_PROFILE)) {
                //okay!
                requiredRequestVars.add(dataVariableDestinationNames()[profile_idIndex]);
                outerVars.add(cdmVars[0]);

            //TimeSeries
            } else if (cdmType.equals(CDM_TIMESERIES)) {
                //okay!
                requiredRequestVars.add(dataVariableDestinationNames()[timeseries_idIndex]);
                outerVars.add(cdmVars[1]);

            //Trajectory  
            } else if (cdmType.equals(CDM_TRAJECTORY)) {
                //okay!
                requiredRequestVars.add(dataVariableDestinationNames()[trajectory_idIndex]);
                outerVars.add(cdmVars[2]);

            //TimeSeriesProfile
            } else if (cdmType.equals(CDM_TIMESERIESPROFILE)) {
                //okay!
                requiredRequestVars.add(dataVariableDestinationNames()[timeseries_idIndex]);
                requiredRequestVars.add(dataVariableDestinationNames()[profile_idIndex]);
                outerVars.add(cdmVars[0]);
                outerVars.add(cdmVars[1]);

            //TrajectoryProfile  
            } else if (cdmType.equals(CDM_TRAJECTORYPROFILE)) {
                //okay!
                requiredRequestVars.add(dataVariableDestinationNames()[trajectory_idIndex]);
                requiredRequestVars.add(dataVariableDestinationNames()[profile_idIndex]);
                outerVars.add(cdmVars[0]);
                outerVars.add(cdmVars[2]);

            //the cdmType (e.g., Other) doesn't support .ncCF/.ncCFMA
            } else {
                accessibleViaNcCF = String2.canonical(
                    MessageFormat.format(EDStatic.noXxxBecause2, ".ncCF/.ncCFMA", 
                        MessageFormat.format(EDStatic.noXxxNoCdmDataType, cdmType)));
            }


            //no errors so far
            if (accessibleViaNcCF == null) {
                if (lonIndex < 0) {
                    throw new SimpleException(  
                        startCdm + "the dataset doesn't have a longitude variable.");
                } else if (latIndex < 0) {
                    throw new SimpleException(   
                        startCdm + "the dataset doesn't have a latitude variable.");
                } else if (timeIndex < 0) {
                    throw new SimpleException(   
                        startCdm + "the dataset doesn't have a time variable.");
                } else {
                    //it passes!
                    if (requiredRequestVars.indexOf("longitude") < 0)
                        requiredRequestVars.add(    "longitude");
                    if (requiredRequestVars.indexOf("latitude") < 0)
                        requiredRequestVars.add(    "latitude");
                    if (requiredRequestVars.indexOf("time") < 0)
                        requiredRequestVars.add(    "time");
                    if (altIndex >= 0) { 
                        if (requiredRequestVars.indexOf("altitude") < 0)
                            requiredRequestVars.add(    "altitude");
                    } else if (depthIndex >= 0) { 
                        if (requiredRequestVars.indexOf("depth") < 0)
                            requiredRequestVars.add(    "depth");
                    } else if (proxy != null) {
                        if (requiredRequestVars.indexOf(proxy) < 0)
                            requiredRequestVars.add(    proxy);
                    }
                    requiredCfRequestVariables = requiredRequestVars.toArray();

                    outerCfVariables = outerVars.toArray();
                    accessibleViaNcCF = String2.canonical(""); //do last
                }
            }
        }
        return accessibleViaNcCF;
    }

    /**
     * This returns the start of the SOS GML offering/procedure name, e.g.,
     * urn:ioos:sensor:noaa.nws.ndbc:41004:adcp   (but for ERDDAP, the station = the sensor)
     * urn:ioos:station:noaa.nws.ndbc:41004:
     * urn:ioos:network:noaa.nws.ndbc:all (IOOS NDBC)
     * urn:ioos:network:noaa.nws.ndbc:[datasetID] (ERDDAP)
     * (after : is (datasetID|stationID)[:varname]
     *
     * @param tSosOfferingType usually this EDDTable's sosOfferingType (e.g., Station), 
     *    but sometimes sosNetworkOfferingType ("network") or "sensor".
     * @return sosGmlNameStart    
     */
    public String getSosGmlNameStart(String tSosOfferingType) {
        return sosUrnBase() + ":" + tSosOfferingType + 
            ":" + EDStatic.sosBaseGmlName + "." + datasetID + ":"; //+ shortOffering 
    }

    /**
     * This returns the SOS capabilities xml for this dataset (see Erddap.doSos).
     * This should only be called if accessibleViaSOS is true and
     *    loggedInAs has access to this dataset 
     *    (so redirected to login, instead of getting an error here).
     *
     * <p>This seeks to mimic IOOS SOS servers like ndbcSosWind.
     * See http://sdf.ndbc.noaa.gov/sos/ .
     *
     * @param writer In the end, the writer is flushed, not closed.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     */
    public void sosGetCapabilities(Writer writer,
        String loggedInAs) throws Throwable {

        if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
            throw new SimpleException(
                MessageFormat.format(EDStatic.notAuthorized, loggedInAs, datasetID));

        if (accessibleViaSOS().length() > 0)
            throw new SimpleException(accessibleViaSOS());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String stationGmlNameStart = getSosGmlNameStart(sosOfferingType);
        String networkGmlNameStart = getSosGmlNameStart(sosNetworkOfferingType);
        String sensorGmlNameStart = getSosGmlNameStart("sensor");
        String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
        String fullPhenomenaDictionaryUrl = tErddapUrl + "/sos/" + datasetID + 
            "/" + sosPhenomenaDictionaryUrl; 
        String datasetObservedProperty = datasetID;
        String minAllTimeString = " indeterminatePosition=\"unknown\">";
        if (timeIndex >= 0) {
            EDVTime timeEdv = (EDVTime)dataVariables[timeIndex];
            if (timeEdv.destinationMinString().length() > 0)
                minAllTimeString = ">" + timeEdv.destinationMinString();
        }

        writer.write(
//???this was patterned after 
//http://sdftest.ndbc.noaa.gov/sos/
//http://sdftest.ndbc.noaa.gov/sos/server.php?request=GetCapabilities&service=SOS
//    which is c:/programs/sos/ndbc_capabilities_100430.xml
//less developed: Coops
//    http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/
//    http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/SOS?service=SOS&request=GetCapabilities
//        which is c:/programs/sos/coops_capabilities_100430.xml
//and http://www.gomoos.org/cgi-bin/sos/V1.0/oostethys_sos.cgi?service=SOS&request=GetCapabilities
//    which is C:/programs/sos/gomoos_capabilities_100430.xml
"<?xml version=\"1.0\"?>\n" +   //don't specify encoding (http does that)
"<Capabilities\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns=\"http://www.opengis.net/sos/1.0\"\n" +
"  xmlns:sos=\"http://www.opengis.net/sos/1.0\"\n" +
"  xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n" +
"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" +
"  xmlns:tml=\"http://www.opengis.net/tml\"\n" +
"  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
"  xmlns:myorg=\"http://www.myorg.org/features\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/sos/1.0 http://schemas.opengis.net/sos/1.0.0/sosAll.xsd\"\n" + 
"  version=\"" + sosVersion + "\">\n" +
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n");

        //ServiceIdentification
        writer.write(
"  <ows:ServiceIdentification>\n" +
"    <ows:Title>" + XML.encodeAsXML("SOS for " + title()) + "</ows:Title>\n" +
"    <ows:Abstract>" + XML.encodeAsXML(summary()) + "</ows:Abstract>\n" +
"    <ows:Keywords>\n");
            String keywordsSA[] = keywords();
            for (int i = 0; i < keywordsSA.length; i++)
                writer.write(
"      <ows:Keyword>" + XML.encodeAsXML(keywordsSA[i]) + "</ows:Keyword>\n");
            writer.write(
"    </ows:Keywords>\n" +
"    <ows:ServiceType codeSpace=\"http://opengeospatial.net\">OGC:SOS</ows:ServiceType>\n" +
"    <ows:ServiceTypeVersion>" + sosVersion + "</ows:ServiceTypeVersion>\n" +  
"    <ows:Fees>" + XML.encodeAsXML(fees()) + "</ows:Fees>\n" +
"    <ows:AccessConstraints>" + XML.encodeAsXML(accessConstraints()) + "</ows:AccessConstraints>\n" +
"  </ows:ServiceIdentification>\n");

        //ServiceProvider 
        writer.write(
"  <ows:ServiceProvider>\n" +
"    <ows:ProviderName>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</ows:ProviderName>\n" +
"    <ows:ProviderSite xlink:href=\"" + tErddapUrl + "\"/>\n" +
"    <ows:ServiceContact>\n" +
"      <ows:IndividualName>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</ows:IndividualName>\n" +
"      <ows:ContactInfo>\n" +
"        <ows:Phone>\n" +
"          <ows:Voice>" + XML.encodeAsXML(EDStatic.adminPhone) + "</ows:Voice>\n" +
"        </ows:Phone>\n" +
"        <ows:Address>\n" +
"          <ows:DeliveryPoint>" + XML.encodeAsXML(EDStatic.adminAddress) + "</ows:DeliveryPoint>\n" +
"          <ows:City>" + XML.encodeAsXML(EDStatic.adminCity) + "</ows:City>\n" +
"          <ows:AdministrativeArea>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</ows:AdministrativeArea>\n" +
"          <ows:PostalCode>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</ows:PostalCode>\n" +
"          <ows:Country>" + XML.encodeAsXML(EDStatic.adminCountry) + "</ows:Country>\n" +
"          <ows:ElectronicMailAddress>" + XML.encodeAsXML(EDStatic.adminEmail) + "</ows:ElectronicMailAddress>\n" +
"        </ows:Address>\n" +
"      </ows:ContactInfo>\n" +
"    </ows:ServiceContact>\n" +
"  </ows:ServiceProvider>\n");

        //OperationsMetadaata 
        writer.write(
"  <ows:OperationsMetadata>\n" +

        //GetCapabilities
"    <ows:Operation name=\"GetCapabilities\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"" + sosUrl + "\"/>\n" +
//"          <ows:Post xlink:href=\"" + sosUrl + "\"/>\n" +  //erddap supports POST???
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"service\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>SOS</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"version\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>1.0.0</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
//"      <ows:Parameter name=\"Sections\">\n" + //the sections of this document; allow request sections???
//"        <ows:AllowedValues>\n" +
//"          <ows:Value>ServiceIdentification</ows:Value>\n" +
//"          <ows:Value>ServiceProvider</ows:Value>\n" +
//"          <ows:Value>OperationsMetadata</ows:Value>\n" +
//"          <ows:Value>Contents</ows:Value>\n" +
//"          <ows:Value>All</ows:Value>\n" +
//"        </ows:AllowedValues>\n" +
//"      </ows:Parameter>\n" +
//"      <ows:Parameter name=\"AcceptVersions\">\n" +
//"        <ows:AllowedValues>\n" +
//"          <ows:Value>1.0.0</ows:Value>\n" +
//"        </ows:AllowedValues>\n" +
//"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +

        //DescribeSensor
"    <ows:Operation name=\"DescribeSensor\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"" + sosUrl + "\"/>\n" +
//"          <ows:Post xlink:href=\"" + sosUrl + "\"/>\n" +  //allow???
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"service\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>SOS</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"version\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>1.0.0</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"outputFormat\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>text/xml;subtype=\"sensorML/1.0.1\"</ows:Value>\n" + 
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
//as of 2010-05-02, GetCapabilities from http://sdftest.ndbc.noaa.gov/sos/ doesn't list DescribeSensor procedures!
"      <ows:Parameter name=\"procedure\">\n" +
"        <ows:AllowedValues>\n");

        for (int i = 0; i < sosOfferings.size(); i++)
            writer.write(
"          <ows:Value>" + sensorGmlNameStart + sosOfferings.getString(i) + "</ows:Value>\n");

        writer.write(
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +

        
        //GetObservation
"    <ows:Operation name=\"GetObservation\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"" + sosUrl + "\"/>\n" +
//"          <ows:Post xlink:href=\"" + sosUrl + "\"/>\n" +  //allow???
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"service\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>SOS</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"version\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>1.0.0</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"observedProperty\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>" + datasetObservedProperty + "</ows:Value>\n" +  
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"responseFormat\">\n" +
"        <ows:AllowedValues>\n");
        for (int i = 0; i < sosDataResponseFormats.length; i++) 
            writer.write(         
"          <ows:Value>" + sosDataResponseFormats[i] + "</ows:Value>\n");
        writer.write(
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"eventTime\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Range>\n" +
"            <ows:MinimumValue" + minAllTimeString + "</ows:MinimumValue>\n" + 
"            <ows:MaximumValue>now</ows:MaximumValue>\n" +  //???
"          </ows:Range>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"      <ows:Parameter name=\"procedure\">\n" +
"        <ows:AllowedValues>\n");

        for (int i = 0; i < sosOfferings.size(); i++)
            writer.write(
"          <ows:Value>" + sensorGmlNameStart + sosOfferings.getString(i) + "</ows:Value>\n");

        writer.write(
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
//"      <ows:Parameter name=\"resultModel\">\n" +
//"        <ows:AllowedValues>\n" +
//"          <ows:Value>om:Observation</ows:Value>\n" +
//"        </ows:AllowedValues>\n" +
//"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +
"  </ows:OperationsMetadata>\n");


        //Contents 
        writer.write(
"  <Contents>\n" +
"    <ObservationOfferingList>\n");

        //note that accessibleViaSOS() has insured
        //  sosMinLon, sosMaxLon, sosMinLat, sosMaxLat, sosMinTime, sosMaxTime,
        //  and sosOfferings are valid


        //go through the offerings, e.g. 100 stations.    -1 is for sosNetworkOfferingType [datasetID]
        int nOfferings = sosOfferings.size();
        for (int offering = -1; offering < nOfferings; offering++) { 
            String offeringType = offering == -1? sosNetworkOfferingType : sosOfferingType;
            String offeringName = offering == -1? datasetID : sosOfferings.getString(offering);
            String gmlName = (offering == -1? networkGmlNameStart : stationGmlNameStart) + offeringName; 
            //datasetID is name of 'sensor' at that station
            String sensorName = sensorGmlNameStart + offeringName; // + ":" + datasetID;

            double minLon  = Double.NaN, maxLon  = Double.NaN, 
                   minLat  = Double.NaN, maxLat  = Double.NaN, 
                   minTime = Double.NaN, maxTime = Double.NaN;
            if (offering == -1) {       
                minLon  = dataVariables[lonIndex].destinationMin();
                maxLon  = dataVariables[lonIndex].destinationMax();
                minLat  = dataVariables[latIndex].destinationMin();
                maxLat  = dataVariables[latIndex].destinationMax();
            } else {
                minLon  = sosMinLon.getNiceDouble(offering);
                maxLon  = sosMaxLon.getNiceDouble(offering);
                minLat  = sosMinLat.getNiceDouble(offering);
                maxLat  = sosMaxLat.getNiceDouble(offering);
                minTime = sosMinTime.getDouble(offering);
                maxTime = sosMaxTime.getDouble(offering);
            }
            if (Double.isNaN(minTime)) minTime = dataVariables[timeIndex].destinationMin();
            if (Double.isNaN(maxTime)) maxTime = dataVariables[timeIndex].destinationMax();

            String minTimeString = Double.isNaN(minTime)?  
                " indeterminatePosition=\"unknown\">" :
                ">" + Calendar2.epochSecondsToIsoStringT(minTime) + "Z";
            String maxTimeString = 
                Double.isNaN(maxTime)?  
                    " indeterminatePosition=\"unknown\">" : 
                System.currentTimeMillis() - maxTime < 3 * Calendar2.MILLIS_PER_DAY? //has data within last 3 days
                    " indeterminatePosition=\"now\">" : 
                    ">" + Calendar2.epochSecondsToIsoStringT(maxTime) + "Z";

            writer.write(
"      <ObservationOffering gml:id=\"" + offeringType + "-" + 
            offeringName + "\">\n" +  
"        <gml:description>" + offeringType + " " + offeringName + "</gml:description>\n" +  //a better description would be nice
"        <gml:name>" + gmlName + "</gml:name>\n" +  
"        <gml:srsName>urn:ogc:def:crs:epsg::4326</gml:srsName>\n" +  //gomoos doesn't have this
"        <gml:boundedBy>\n" +
"          <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" + //gomoos has ...EPSG:6.5:4326
"            <gml:lowerCorner>" + minLat + " " + minLon + "</gml:lowerCorner>\n" +
"            <gml:upperCorner>" + maxLat + " " + maxLon + "</gml:upperCorner>\n" +
"          </gml:Envelope>\n" +
"        </gml:boundedBy>\n" +
"        <time>\n" +
"          <gml:TimePeriod xsi:type=\"gml:TimePeriodType\">\n" +
"            <gml:beginPosition" + minTimeString + "</gml:beginPosition>\n" +
"            <gml:endPosition"   + maxTimeString + "</gml:endPosition>\n" +
"          </gml:TimePeriod>\n" +
"        </time>\n");

             if (offering == -1) {
                 //procedures are names of all stations
                 for (int offering2 = 0; offering2 < nOfferings; offering2++) 
                     writer.write(
"        <procedure xlink:href=\"" + 
                         XML.encodeAsXML(stationGmlNameStart + sosOfferings.getString(offering2)) + "\"/>\n"); 

             } else {
                 //procedure is name of sensor. 
                 writer.write(
"        <procedure xlink:href=\"" + XML.encodeAsXML(sensorName) + "\"/>\n"); 
             }

             writer.write(
"        <observedProperty xlink:href=\"" + 
                 XML.encodeAsXML(fullPhenomenaDictionaryUrl + "#" + datasetObservedProperty) + "\"/>\n");
             for (int dv = 0; dv < dataVariables.length; dv++) {
                 if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex || 
                     dv == timeIndex || dv == sosOfferingIndex) 
                     continue;
                 writer.write(
"        <observedProperty xlink:href=\"" + 
                     XML.encodeAsXML(fullPhenomenaDictionaryUrl + "#" + dataVariables[dv].destinationName()) + "\"/>\n");
             }

             writer.write(
"        <featureOfInterest xlink:href=\"" + XML.encodeAsXML(sosFeatureOfInterest()) + "\"/>\n");
             for (int rf = 0; rf < sosDataResponseFormats.length; rf++) 
                 writer.write(
"        <responseFormat>" + XML.encodeAsXML(sosDataResponseFormats[rf]) + "</responseFormat>\n");
             for (int rf = 0; rf < sosImageResponseFormats.length; rf++) 
                 writer.write(
"        <responseFormat>" + XML.encodeAsXML(sosImageResponseFormats[rf]) + "</responseFormat>\n");
             writer.write(
//???different for non-SOS-XML, e.g., csv???
"        <resultModel>om:Observation</resultModel>\n" +
//"        <responseMode>inline</responseMode>\n" +
//"        <responseMode>out-of-band</responseMode>\n" +
"      </ObservationOffering>\n");
        }

        writer.write(
"    </ObservationOfferingList>\n" +
"  </Contents>\n");

        //end of getCapabilities
        writer.write(
"</Capabilities>\n");

        //essential
        writer.flush();
    }


    /**
     * This returns the SOS phenomenaDictionary xml for this dataset, i.e., the response to 
     *   [tErddapUrl]/sos/[datasetID]/[sosPhenomenaDictionaryUrl].
     * This should only be called if accessibleViaSOS is true and
     *    loggedInAs has access to this dataset 
     *    (so redirected to login, instead of getting an error here).
     *
     * <p>This seeks to mimic IOOS SOS servers like ndbcSosWind.
     * See http://sdf.ndbc.noaa.gov/sos/ and in particular
     * http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml
     *
     * @param writer In the end, the writer is flushed, not closed.
     */
    public void sosPhenomenaDictionary(Writer writer) throws Throwable {

        //if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
        //    throw new SimpleException(
        //        MessageFormat.format(EDStatic.notAuthorized, loggedInAs, datasetID));

        //if (accessibleViaSOS().length() > 0)
        //    throw new SimpleException(accessibleViaSOS());

        //String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        //String dictUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl;

        String codeSpace = getSosGmlNameStart("phenomena");
        codeSpace = codeSpace.substring(0, codeSpace.length() - 2); //remove ::
        String destNames[] = dataVariableDestinationNames();
        int ndv = destNames.length;
        String vocab = combinedGlobalAttributes().getString("standard_name_vocabulary");
        boolean usesCfNames = vocab != null && vocab.startsWith("CF-");

        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<gml:Dictionary gml:id=\"PhenomenaDictionary0.6.1\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.2\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //2009-08-26 I changed from ../.. links to actual links for xsd's below
"  xsi:schemaLocation=\"http://www.opengis.net/swe/1.0.2 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/OGC/sweCommon/1.0.2/phenomenon.xsd http://www.opengis.net/gml/3.2 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/OGC/gml/3.2.1/gml.xsd\"\n" +
"  >\n" +
"  <gml:description>Dictionary of observed phenomena for " + datasetID + ".</gml:description>\n" +
"  <gml:identifier codeSpace=\"" + codeSpace + "\">PhenomenaDictionary</gml:identifier>\n");

        //the variables
        int count = 0;
        for (int dv = 0; dv < ndv; dv++) {
            if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                dv == timeIndex || dv == sosOfferingIndex)
                continue;
            count++;
            String standardName = dataVariables[dv].combinedAttributes().getString("standard_name");
            boolean hasSN = standardName != null && standardName.length() > 0;
            writer.write(
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"" + destNames[dv] + "\">\n" +
"      <gml:description>" + XML.encodeAsXML(dataVariables[dv].longName()) + "</gml:description>\n" +
"      <gml:identifier codeSpace=\"" + 
                (hasSN? sosStandardNamePrefix().substring(0, sosStandardNamePrefix().length() - 1) : codeSpace) + 
                "\">" + 
                (hasSN? standardName : destNames[dv]) + 
                "</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n");
        }
        
        //the composite
        writer.write(
"  <gml:definitionMember >\n" +
"    <swe:CompositePhenomenon gml:id=\"" + datasetID + "\" dimension=\"" + count + "\">\n" +
"      <gml:description>" + XML.encodeAsXML(title()) + "</gml:description>\n" +
"      <gml:identifier codeSpace=\"" + codeSpace + "\">" + datasetID + "</gml:identifier>\n");
        for (int dv = 0; dv < ndv; dv++) {
            if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                dv == timeIndex || dv == sosOfferingIndex)
                continue;
            writer.write(
"      <swe:component xlink:href=\"#" + destNames[dv] + "\"/>\n");
        }

        writer.write(
"    </swe:CompositePhenomenon>\n" +
"  </gml:definitionMember>\n" +
"</gml:Dictionary>\n");

        writer.flush();
    }


    /**
     * This responds to a sos describeSensor request.
     * This should only be called if accessibleViaSOS is true and
     *    loggedInAs has access to this dataset 
     *    (so redirected to login, instead of getting an error here).
     *
     * <p>This seeks to mimic IOOS SOS servers like 
     *   http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/get/describesensor/getstation.jsp
     *
     * @param loggedInAs
     * @param shortName the short "procedure" name (from the query, but shortened): 
     *     the network name (the datasetID, for all) or a station name (41004).
     *     It must be already checked for validity.
     * @param writer In the end, the writer is flushed, not closed.
     */
    public void sosDescribeSensor(String loggedInAs, String shortName,
        Writer writer) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;

        //get sensor info
        boolean isNetwork = shortName.equals(datasetID);
        double minTimeD = Double.NaN, maxTimeD = Double.NaN;
        String offeringType, minLon, maxLon, minLat, maxLat;
        if (isNetwork) {
            offeringType = sosNetworkOfferingType;
            EDV lonEdv = dataVariables[lonIndex];
            EDV latEdv = dataVariables[latIndex];
            minLon = lonEdv.destinationMinString(); 
            maxLon = lonEdv.destinationMaxString(); 
            minLat = latEdv.destinationMinString(); 
            maxLat = latEdv.destinationMaxString(); 
        } else {
            offeringType = sosOfferingType;
            int which = sosOfferings.indexOf(shortName);
            if (which < 0)
                //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                throw new SimpleException(EDStatic.queryError + "procedure=" + shortName + 
                    " isn't a valid sensor name."); 
            minLon = sosMinLon.getString(which); 
            maxLon = sosMaxLon.getString(which);
            minLat = sosMinLat.getString(which);
            maxLat = sosMaxLat.getString(which);
            minTimeD = sosMinTime.getDouble(which);
            maxTimeD = sosMaxTime.getDouble(which);
        }
        if (Double.isNaN(minTimeD)) minTimeD = dataVariables[timeIndex].destinationMin();
        if (Double.isNaN(maxTimeD)) maxTimeD = dataVariables[timeIndex].destinationMax();
        String minTimeString = Double.isNaN(minTimeD)?  
            " indeterminatePosition=\"unknown\">" :
            ">" + Calendar2.epochSecondsToIsoStringT(minTimeD) + "Z";
        String maxTimeString = 
            Double.isNaN(maxTimeD)?  
                " indeterminatePosition=\"unknown\">" : 
            System.currentTimeMillis() - maxTimeD < 3 * Calendar2.MILLIS_PER_DAY? //has data within last 3 days
                " indeterminatePosition=\"now\">" : 
                ">" + Calendar2.epochSecondsToIsoStringT(maxTimeD) + "Z";

        String tType = isNetwork? sosNetworkOfferingType : offeringType;
        String name2 = tType + "-" + shortName;
        String gmlName = getSosGmlNameStart(tType) + shortName;

        //based on http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/get/describesensor/getstation.jsp
        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<sml:SensorML\n" +
"  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n" +
"  version=\"1.0.1\"\n" + //the sensorML version, not the sosVersion
"  >\n" +
"  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n" +
"  <sml:member>\n" +
"    <sml:System gml:id=\"" + name2 + "\">\n" +
"      <gml:description>" + XML.encodeAsXML(title() + ", " + name2) + "</gml:description>\n" +
"      <sml:keywords>\n" +
"        <sml:KeywordList>\n"); //codeSpace=\"http://gcmd.nasa.gov/Resources/valids/archives/keyword_list.html\">\n" +
            String keywordsSA[] = keywords();
            for (int i = 0; i < keywordsSA.length; i++)
                writer.write(
"          <sml:keyword>" + XML.encodeAsXML(keywordsSA[i]) + "</sml:keyword>\n");
            writer.write(
"        </sml:KeywordList>\n" +
"      </sml:keywords>\n" +
"\n" +

//identification
"      <sml:identification>\n" +
"        <sml:IdentifierList>\n" +
"          <sml:identifier name=\"Station ID\">\n" +  //true for "network", too?
"            <sml:Term definition=\"urn:ioos:station:organization:stationID:\">\n" +
"              <sml:codeSpace xlink:href=\"" + tErddapUrl + "\" />\n" +
"              <sml:value>" + gmlName + "</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"          <sml:identifier name=\"Short Name\">\n" +
"            <sml:Term definition=\"urn:ioos:identifier:organization:shortName\">\n" +
"              <sml:value>" + shortName + "</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
//"          <sml:identifier name=\"Long Name\">\n" +
//"            <sml:Term definition=\"urn:ogc:def:identifier:OGC:longName\">\n" +
//"              <sml:value>Providence</sml:value>\n" +
//"            </sml:Term>\n" +
//"          </sml:identifier>\n" +
//"          <sml:identifier name=\"Vertical Datum MHW\">\n" +
//"            <sml:Term definition=\"urn:ioos:datum:noaa:MHW:\">\n" +
//"              <sml:value>2.464</sml:value>\n" +
//"            </sml:Term>\n" +
//"          </sml:identifier>\n" +
//           and vertical datums for other sensors
"        </sml:IdentifierList>\n" +
"      </sml:identification>\n" +
"\n" +

//classification
"      <sml:classification>\n" +
"        <sml:ClassifierList>\n");

            if (!isNetwork)
                writer.write(
"          <sml:classifier name=\"Parent Network\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:NOAA:parentNetwork\">\n" +
"              <sml:codeSpace xlink:href=\"" + tErddapUrl + "\" />\n" +
"              <sml:value>" + getSosGmlNameStart(sosNetworkOfferingType) + datasetID + "</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:classifier>\n");

            writer.write(
//"          <sml:classifier name=\"System Type Name\">\n" +
//"            <sml:Term definition=\"urn:ioos:classifier:NOAA:systemTypeName\">\n" +
//"              <sml:codeSpace xlink:href=\"http://tidesandcurrents.noaa.gov\" />\n" +
//"              <sml:value>CO-OPS Observational System</sml:value>\n" +
//"            </sml:Term>\n" +
//"          </sml:classifier>\n" +
"          <sml:classifier name=\"System Type Identifier\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:NOAA:systemTypeID\">\n" +
"              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n" +
"              <sml:value>Platform</sml:value>\n" +  //ERDDAP treats all datasets and stations as 'platforms'
"            </sml:Term>\n" +
"          </sml:classifier>\n" +
"        </sml:ClassifierList>\n" +
"      </sml:classification>\n" +
"\n" +

//time
"      <sml:validTime>\n" +
"        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n" +
"          <gml:beginPosition" + minTimeString + "</gml:beginPosition>\n" +
"          <gml:endPosition"   + maxTimeString + "</gml:endPosition>\n" +
"        </gml:TimePeriod>\n" +
"      </sml:validTime>\n" +
"\n" +

//contact    
"      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n" + //was owner
"        <sml:ResponsibleParty>\n" +
           //bob added:
"          <sml:individualName>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</sml:individualName>\n" +
"          <sml:organizationName>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</sml:organizationName>\n" +
"          <sml:contactInfo>\n" +
"            <sml:phone>\n" +
"              <sml:voice>" + XML.encodeAsXML(EDStatic.adminPhone) + "</sml:voice>\n" +
"            </sml:phone>\n" +
"            <sml:address>\n" +
"              <sml:deliveryPoint>" + XML.encodeAsXML(EDStatic.adminAddress) + "</sml:deliveryPoint>\n" +
"              <sml:city>" + XML.encodeAsXML(EDStatic.adminCity) + "</sml:city>\n" +
"              <sml:administrativeArea>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</sml:administrativeArea>\n" +
"              <sml:postalCode>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</sml:postalCode>\n" +
"              <sml:country>" + XML.encodeAsXML(EDStatic.adminCountry) + "</sml:country>\n" +
               //bob added:
"              <sml:electronicMailAddress>" + XML.encodeAsXML(EDStatic.adminEmail) + "</sml:electronicMailAddress>\n" +
"            </sml:address>\n" +
"            <sml:onlineResource xlink:href=\"" + tErddapUrl + "\" />\n" +
"          </sml:contactInfo>\n" +
"        </sml:ResponsibleParty>\n" +
"      </sml:contact>\n" +
"\n" +

//documentation
"      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n" +
"        <sml:Document>\n" +
"          <gml:description>Web page with background information from the source of this dataset</gml:description>\n" +
"          <sml:format>text/html</sml:format>\n" +
"          <sml:onlineResource xlink:href=\"" + XML.encodeAsXML(infoUrl) + "\" />\n" +
//"          <sml:onlineResource xlink:href=\"" + tErddapUrl + "/info/" + datasetID + ".html\" />\n" +
"        </sml:Document>\n" +
"      </sml:documentation>\n" +
"\n");

//            writer.write(
//"      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:objectImage\">\n" +
//"        <sml:Document>\n" +
//"          <gml:description>Photo or diagram illustrating the system (or a representative instance) described in this SensorML record</gml:description>\n" +
//"          <sml:format>image/jpg</sml:format>\n" +
//"          <sml:onlineResource xlink:href=\"http://tidesandcurrents.noaa.gov/images/photos/8454000a.jpg\" />\n" +
//"        </sml:Document>\n" +
//"      </sml:documentation>\n" +
//"\n");

//location
            if (minLon.equals(maxLon) && minLat.equals(maxLat)) 
                writer.write(
"      <sml:location>\n" +
"        <gml:Point srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"" + offeringType + "_LatLon\">\n" +
"          <gml:coordinates>" + minLat + " " + minLon + "</gml:coordinates>\n" +
"        </gml:Point>\n" +
"      </sml:location>\n" +
"\n");
//            else writer.write( 
//                //bob tried to add, but gml:Box is invalid; must be Point or _Curve.  
//                //xml validator says sml:location isn't required
//"      <sml:location>\n" +
//"        <gml:Box srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"" + offeringType + "_Box\">\n" +  //Box was "location"
//"          <gml:coordinates>" + minLat + "," + minLon + " " + maxLat + "," + maxLon + "</gml:coordinates>\n" +
//"        </gml:Box>\n" +
//"      </sml:location>\n" +
//"\n");

//components   (an sml:component for each sensor)
            writer.write(
"      <sml:components>\n" +
"        <sml:ComponentList>\n");

            //a component for each variable
/*            {
                for (int dv = 0; dv < dataVariables.length; dv++) {
                    if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                        dv == timeIndex || dv == sosOfferingIndex)
                        continue;
                    EDV edv = dataVariables[dv];
                    String destName = edv.destinationName();
                    writer.write(
"          <sml:component name=\"" + destName + " Instrument\">\n" +
"            <sml:System gml:id=\"sensor-" + name2 + "-" + destName + "\">\n" +
"              <gml:description>" + name2 + "-" + destName + " Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"" + gmlName + ":" + destName + "\" />\n" +  //A1:wspd
"              <sml:documentation xlink:href=\"" + tErddapUrl + "/info/" + datasetID + ".html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n");
                    String codeSpace = getSosGmlNameStart("phenomena");
                    String stdName = edv.combinedAttributes().getString("standard_name");
                    boolean hasSN = stdName != null && stdName.length() > 0;
                    writer.write(
"                  <sml:output name=\"" + destName + "\">\n" + //but NOS has longName
"                    <swe:Quantity definition=\"" + 
                     (hasSN? sosStandardNamePrefix() + stdName : codeSpace + destName) + 
                     "\">\n");
                    if (edv.ucumUnits() != null && edv.ucumUnits().length() > 0)
                        writer.write(
"                      <swe:uom code=\"" + edv.ucumUnits() + "\" />\n");
                    writer.write(
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n");
                }
            }
*/

           //component for all variables
           { 
               writer.write(
"          <sml:component name=\"" + datasetID + " Instrument\">\n" +
"            <sml:System gml:id=\"sensor-" + name2 + "-" + datasetID + "\">\n" +
"              <gml:description>" + name2 + " Platform</gml:description>\n" +
"              <sml:identification xlink:href=\"" + XML.encodeAsXML(gmlName) + //":" + datasetID +  //41004:cwwcNDBCMet
                   "\" />\n" +  
"              <sml:documentation xlink:href=\"" + tErddapUrl + "/info/" + datasetID + ".html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n");
                String codeSpace = getSosGmlNameStart("phenomena");
                for (int dv = 0; dv < dataVariables.length; dv++) {
                    if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                        dv == timeIndex || dv == sosOfferingIndex)
                        continue;
                    EDV edv = dataVariables[dv];
                    String stdName = edv.combinedAttributes().getString("standard_name");
                    boolean hasSN = stdName != null && stdName.length() > 0;
                    writer.write(
"                  <sml:output name=\"" + edv.destinationName() + "\">\n" + //but NOS has longName
"                    <swe:Quantity definition=\"" + 
                     (hasSN? sosStandardNamePrefix() + stdName : codeSpace + edv.destinationName()) + 
                     "\">\n");
                    if (edv.ucumUnits() != null && edv.ucumUnits().length() > 0)
                        writer.write(
"                      <swe:uom code=\"" + edv.ucumUnits() + "\" />\n");
                    writer.write(
"                    </swe:Quantity>\n" +
"                  </sml:output>\n");
                }
                writer.write(
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n");
           }

            writer.write(
"        </sml:ComponentList>\n" +
"      </sml:components>\n" +
"    </sml:System>\n" +
"  </sml:member>\n" +
"</sml:SensorML>\n");




/*   * <p>This seeks to mimic IOOS SOS servers like ndbcSosWind.
     * See http://sdf.ndbc.noaa.gov/sos/ .
     * The url might be something like
     *    http://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor&service=SOS
     *    &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
     *    &procedure=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0

        writer.write(
//???this was patterned after C:/programs/sos/ndbcSosCurrentsDescribeSensor90810.xml
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<sml:Sensor \n" +
"  xmlns:sml=\"http://www.w3.org/2001/XMLSchema\" \n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\" \n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\" \n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\" \n" + //ogc doesn't show 1.0.2
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
"<!-- This is a PROTOTYPE service.  The information in this response is NOT complete. -->\n" +
"  <sml:identification>\n" +
"    <sml:IdentifierList/>\n" +
"  </sml:identification>\n" +
"  <sml:inputs>\n" +
"    <sml:InputList/>\n" +
"  </sml:inputs>\n" +
"  <sml:outputs>\n" +
"    <sml:OutputList/>\n" +
"  </sml:outputs>\n" +
"  <sml:positions>\n" +
"    <sml:PositionList>\n" +
"      <sml:position name=\"southwestCorner\">\n" +
"        <swe:GeoLocation>\n" +
"          <swe:latitude>\n" +
"            <swe:Quantity>" + minLat + "</swe:Quantity>\n" +
"          </swe:latitude>\n" +
"          <swe:longitude>\n" +
"            <swe:Quantity>" + minLon + "</swe:Quantity>\n" +
"          </swe:longitude>\n" +
"        </swe:GeoLocation>\n" +
"      </sml:position>\n" +
"      <sml:position name=\"southeastCorner\">\n" +
"        <swe:GeoLocation>\n" +
"          <swe:latitude>\n" +
"            <swe:Quantity>" + minLat + "</swe:Quantity>\n" +
"          </swe:latitude>\n" +
"          <swe:longitude>\n" +
"            <swe:Quantity>" + maxLon + "</swe:Quantity>\n" +
"          </swe:longitude>\n" +
"        </swe:GeoLocation>\n" +
"      </sml:position>\n" +
"      <sml:position name=\"northeastCorner\">\n" +
"        <swe:GeoLocation>\n" +
"          <swe:latitude>\n" +
"            <swe:Quantity>" + maxLat + "</swe:Quantity>\n" +
"          </swe:latitude>\n" +
"          <swe:longitude>\n" +
"            <swe:Quantity>" + maxLon + "</swe:Quantity>\n" +
"          </swe:longitude>\n" +
"        </swe:GeoLocation>\n" +
"      </sml:position>\n" +
"      <sml:position name=\"northwestCorner\">\n" +
"        <swe:GeoLocation>\n" +
"          <swe:latitude>\n" +
"            <swe:Quantity>" + maxLat + "</swe:Quantity>\n" +
"          </swe:latitude>\n" +
"          <swe:longitude>\n" +
"            <swe:Quantity>" + minLon + "</swe:Quantity>\n" +
"          </swe:longitude>\n" +
"        </swe:GeoLocation>\n" +
"      </sml:position>\n" +
"    </sml:PositionList>\n" +
"  </sml:positions>\n" +
"</sml:Sensor>\n");
*/
        //essential
        writer.flush();
        
    }

    /**
     * This returns the fileTypeName (e.g., .csvp) which corresponds to the data or image responseFormat.
     *
     * @param responseFormat
     * @return returns the fileTypeName (e.g., .csvp)  which corresponds to the data or image 
     *    responseFormat (or null if no match).
     */
    public static String sosResponseFormatToFileTypeName(String responseFormat) {
        if (responseFormat == null)
            return null;
        int po = String2.indexOf(sosDataResponseFormats,  responseFormat);
        if (po >= 0)
            return sosTabledapDataResponseTypes[po];
        po = String2.indexOf(sosImageResponseFormats,  responseFormat);
        if (po >= 0)
            return sosTabledapImageResponseTypes[po];
        return null;
    }

    /**
     * This indicates if the responseFormat (from a sos GetObservation request)
     * is one of the IOOS SOS XML format response types.
     *
     * @param responseFormat
     * @throws SimpleException if trouble (e.g., not a valid responseFormat)
     */
    public static boolean isIoosSosXmlResponseFormat(String responseFormat) {
        String tabledapType = sosResponseFormatToFileTypeName(responseFormat);
        if (tabledapType == null)
            //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
            throw new SimpleException(EDStatic.queryError + "responseFormat=" + responseFormat + " is invalid."); 
        //true if valid (tested above) and "ioos" is in the name
        return responseFormat.indexOf("ioos") >= 0;            
    }

    /**
     * This indicates if the responseFormat (from a sos GetObservation request)
     * is the Oostethys SOS XML format response types.
     *
     * @param responseFormat
     * @throws SimpleException if trouble (e.g., not a valid responseFormat)
     */
    public static boolean isOostethysSosXmlResponseFormat(String responseFormat) {
        return sosDataResponseFormats[sosOostethysDataResponseFormat].equals(responseFormat);        
    }

    /**
     * This responds to a SOS query.
     * This should only be called if accessibleViaSOS is "" and
     *    loggedInAs has access to this dataset
     *    (so redirected to login, instead of getting an error here);
     *    this doesn't check.
     *
     * @param sosQuery
     * @param loggedInAs  or null if not logged in
     * @param outputStreamSource  if all goes well, this method calls out.close() at the end.
     * @param dir  the directory to use for a cache file
     * @param fileName the suggested name for a file
     * @throws Exception if trouble (e.g., invalid query parameter)
     */
    public void sosGetObservation(String sosQuery, String loggedInAs,
        OutputStreamSource outputStreamSource, 
        String dir, String fileName) throws Throwable {

        if (reallyVerbose) String2.log("\nrespondToSosQuery q=" + sosQuery);
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = "/sos/" + datasetID + "/" + sosServer;

        //??? should this check?  getDataForDapQuery doesn't
        //if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
        //    throw new SimpleException(
        //        MessageFormat.format(EDStatic.notAuthorized, loggedInAs, datasetID));

        if (accessibleViaSOS().length() > 0)
            throw new SimpleException(accessibleViaSOS());

        //parse the sosQuery
        String dapQueryAr[]   = sosQueryToDapQuery(loggedInAs, sosQuery);
        String dapQuery       = dapQueryAr[0];
        String offeringType   = dapQueryAr[1]; //"network" or e.g., "station"), 
        String offeringName   = dapQueryAr[2]; //datasetID (all) or 41004
        String responseFormat = dapQueryAr[3];
        //String responseMode   = dapQueryAr[4];
        String requestedVars  = dapQueryAr[5];
        if (reallyVerbose) String2.log("dapQueryAr=" + String2.toCSSVString(dapQueryAr));

        //find the fileTypeName (e.g., .tsvp) (sosQueryToDapQuery ensured responseFormat was valid)
        String fileTypeName = sosResponseFormatToFileTypeName(responseFormat);

        OutputStream out = null;
        if (isIoosSosXmlResponseFormat(responseFormat) ||
            isOostethysSosXmlResponseFormat(responseFormat)) {

            //*** handle the IOOS or OOSTETHYS SOS XML responses
            //get the data
            //Unfortunately, this approach precludes streaming the results.
            TableWriterAllWithMetadata twawm = getTwawmForDapQuery(
                loggedInAs, requestUrl, dapQuery); 
            if (twawm.nRows() == 0)
                throw new SimpleException(MustBe.THERE_IS_NO_DATA);

            //convert UDUNITS to UCUM units  (before direct use of twawm or get cumulativeTable)
            if (EDStatic.units_standard.equals("UDUNITS")) {
                int nColumns = twawm.nColumns();
                for (int col = 0; col < nColumns; col++) {
                    Attributes atts = twawm.columnAttributes(col);
                    String units = atts.getString("units");
                    if (units != null)
                        atts.set("units", EDUnits.safeUdunitsToUcum(units)); 
                }
            }

            //get the cumulativeTable 
            //It will be reused in sosObservationsXml.
            //Do this now, so if not enough memory, 
            //    error will be thrown before get outputStream.
            Table table = twawm.cumulativeTable(); 

            //write the results
            //all likely errors are above, so it is now ~safe to get outputstream
            out = outputStreamSource.outputStream("UTF-8");
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            if (isIoosSosXmlResponseFormat(responseFormat)) 
                sosObservationsXmlInlineIoos(offeringType, offeringName, 
                    twawm, writer, loggedInAs);
            else if (isOostethysSosXmlResponseFormat(responseFormat)) 
                sosObservationsXmlInlineOostethys(offeringType, offeringName, 
                    twawm, writer, loggedInAs);            

        } else {
            //*** handle all other file types
            //add the units function to dapQuery?
            if (!EDStatic.units_standard.equals("UCUM"))
                dapQuery += "&units(%22UCUM%22)";

            respondToDapQuery(null, null, loggedInAs, requestUrl, 
                dapQuery, outputStreamSource, dir, fileName, fileTypeName);
        }

        if (out == null)
            out = outputStreamSource.outputStream("");
        if (out instanceof ZipOutputStream) 
            ((ZipOutputStream)out).closeEntry();
        out.close();             
        return;        
    }

    /**
     * This converts a SOS GetObservation query into an OPeNDAP user query.
     * See http://www.oostethys.org/best-practices/best-practices-get .
     *
     * @param loggedInAs
     * @param sosQueryMap  
     * @return [0]=dapQuery (percent encoded), 
     *         [1]=requestOfferingType (e.g., "network" or e.g., "station"), 
     *         [2]=requestShortOfferingName (short name: e.g., all or 41004)
     *         [3]=responseFormat (in request)
     *         //[4]=responseMode (e.g., inline)
     *         [5]=requestedVars (csv of short observedProperties)
     * @throws Exception if trouble (e.g., invalid query parameter)
     */
    public String[] sosQueryToDapQuery(String loggedInAs, String sosQuery) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HashMap<String, String> sosQueryMap = userQueryHashMap(sosQuery, true);

        //parse the query and build the dapQuery
        //http://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0
        //&request=GetObservation
        //&offering=urn:ioos:station:noaa.nws.ndbc:41004:
        //or:                      network                all or datasetID
        //&observedProperty=Winds
        //&responseFormat=text/xml;schema=%22ioos/0.6.1%22
        //&eventTime=2008-08-01T00:00:00Z/2008-08-01T04:00:00Z
        //or
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        StringBuilder dapQuery = new StringBuilder("");

        //service   required
        String service = sosQueryMap.get("service"); //test name.toLowerCase()
        if (service == null || !service.equals("SOS"))
            //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
            throw new SimpleException(EDStatic.queryError + "service=" + service + " should have been \"SOS\"."); 

        //version    required
        String version = sosQueryMap.get("version"); //test name.toLowerCase()
        if (version == null || !version.equals(sosVersion))
            //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
            throw new SimpleException(EDStatic.queryError + "version=" + version + " should have been \"" + 
                sosVersion + "\"."); 

        //request    required
        String request = sosQueryMap.get("request"); //test name.toLowerCase()
        if (request == null || !request.equals("GetObservation"))
            //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
            throw new SimpleException(EDStatic.queryError + "request=" + request + " should have been \"GetObservation\"."); 

        //srsName  SOS required; erddap optional (default=4326, the only one supported)
        String srsName = sosQueryMap.get("srsname"); //test name.toLowerCase()
        if (srsName != null && !srsName.endsWith(":4326"))
            //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
            throw new SimpleException(EDStatic.queryError + "srsName=" + srsName + 
                " should have been \"urn:ogc:def:crs:epsg::4326\"."); 

        //observedProperty      (spec: 1 or more required; erddap: csv list or none (default=all vars)
        //   long (phenomenaDictionary#...) or short (datasetID or edvDestinationName)   
        //don't add vars to dapQuery now. do it later.
        String dictStart = tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl + "#";
        String obsProp = sosQueryMap.get("observedproperty"); //test name.toLowerCase()
        if (obsProp == null || obsProp.length() == 0)
            obsProp = datasetID;
        String properties[] = String2.split(obsProp, ',');
        StringArray requestedVars = new StringArray();
        for (int p = 0; p < properties.length; p++) {
            String property = properties[p];
            if (property.startsWith(dictStart))
                property = property.substring(dictStart.length());
            if (property.equals(datasetID)) {
                requestedVars.clear();
                break;
            } else {
                int dv = String2.indexOf(dataVariableDestinationNames, property);
                if (dv < 0 ||
                    dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                    dv == timeIndex || dv == sosOfferingIndex)
                    throw new SimpleException(EDStatic.queryError + "observedProperty=" + property + " is invalid."); 
                requestedVars.add(property);
            }
        }

        //offering    1, required
        String requestOffering = sosQueryMap.get("offering"); //test name.toLowerCase()
        if (requestOffering == null) requestOffering = "";
        String stationGmlNameStart = getSosGmlNameStart(sosOfferingType);
        String networkGmlName = getSosGmlNameStart(sosNetworkOfferingType) + datasetID;
        if (reallyVerbose) String2.log("stationGmlNameStart=" + stationGmlNameStart + 
            "\nnetworkGmlName=" + networkGmlName);
        String requestOfferingType, requestShortOfferingName; 
        //long or short network name
        int whichOffering = -1;
        if (requestOffering.equals(networkGmlName) || requestOffering.equals(datasetID)) {
            requestOfferingType = sosNetworkOfferingType;
            requestShortOfferingName = datasetID; 
            whichOffering = -1;

        //long or short station name
        } else {
            requestOfferingType = sosOfferingType;
            requestShortOfferingName = requestOffering;
            //make station offering short
            if (requestShortOfferingName.startsWith(stationGmlNameStart) && 
                requestShortOfferingName.lastIndexOf(':') == stationGmlNameStart.length() - 1) 
                requestShortOfferingName = requestShortOfferingName.substring(stationGmlNameStart.length()); //e.g., 41004

            whichOffering = sosOfferings.indexOf(requestShortOfferingName);
            if (whichOffering < 0) 
                //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                throw new SimpleException(EDStatic.queryError + "offering=" + requestOffering + " is invalid."); 
            dapQuery.append("&" + dataVariables[sosOfferingIndex].destinationName() + "=" +
                SSR.minimalPercentEncode("\"" + requestShortOfferingName + "\""));        
        }

        //procedure    forbidden
        String procedure = sosQueryMap.get("procedure"); //test name.toLowerCase()
        if (procedure != null)
            throw new SimpleException(EDStatic.queryError + "\"procedure\" is not supported."); 

        //result    forbidden
        String result = sosQueryMap.get("result"); //test name.toLowerCase()
        if (result != null)
            throw new SimpleException(EDStatic.queryError + "\"result\" is not supported."); 

        //responseMode  SOS & erddap optional (default="inline")
        //String responseMode = sosQueryMap.get("responsemode"); //test name.toLowerCase()
        //if (responseMode == null) 
        //    responseMode = "inline";
        //if (!responseMode.equals("inline") && !responseMode.equals("out-of-band"))
        //    throw new SimpleException(EDStatic.queryError + "responseMode=" + responseMode + 
        //        " should have been \"inline\" or \"out-of-band\"."); 

        //responseFormat
        String responseFormat = sosQueryMap.get("responseformat"); //test name.toLowerCase()
        String fileTypeName = sosResponseFormatToFileTypeName(responseFormat);
        if (fileTypeName == null)
            //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
            throw new SimpleException(EDStatic.queryError + "responseFormat=" + responseFormat + " is invalid."); 
        //if ((isIoosSosXmlResponseFormat(responseFormat) ||
        //     isOostethysSosXmlResponseFormat(responseFormat)) && 
        //    responseMode.equals("out-of-band")) 
        //    throw new SimpleException(EDStatic.queryError + "for responseFormat=" + responseFormat + 
        //        ", responseMode=" + responseMode + " must be \"inline\"."); 

        //resultModel    optional, must be om:Observation
        String resultModel = sosQueryMap.get("resultmodel"); //test name.toLowerCase()
        if (resultModel != null && !resultModel.equals("om:Observation"))
            throw new SimpleException(EDStatic.queryError + "resultModel=" + resultModel + 
                " should have been \"om:Observation\"."); 

        //eventTime   optional start or start/end               spec allow 0 or many
        String eventTime = sosQueryMap.get("eventtime"); //test name.toLowerCase()
        if (eventTime == null) {  
            //if not specified, get latest time's data (within last week)
            double maxTime = whichOffering == -1? dataVariables[timeIndex].destinationMax() : 
                sosMaxTime.getDouble(whichOffering);
            double minTime = Calendar2.backNDays(7, maxTime);
            dapQuery.append("&time>=" + Calendar2.epochSecondsToIsoStringT(minTime) + "Z" +
                "&orderByMax" + SSR.minimalPercentEncode("(\"time\")"));
        } else {
            int spo = eventTime.indexOf('/');
            if (spo < 0)
                 dapQuery.append("&time=" + eventTime);
            else {
                int lpo = eventTime.lastIndexOf('/');
                if (lpo != spo)
                    throw new SimpleException(EDStatic.queryError + "ERDDAP doesn't support '/resolution' " +
                        "in eventTime=beginTime/endTime/resolution."); 
                dapQuery.append("&time>=" + eventTime.substring(0, spo) +
                                "&time<=" + eventTime.substring(spo + 1));
            }
        }

        //featureOfInterest  optional BBOX:minLon,minLat,maxLon,maxLat
        //spec allows 0 or more, advertised or a constraint
        String foi = sosQueryMap.get("featureofinterest"); //test name.toLowerCase()
        if (foi == null) {
        } else if (foi.toLowerCase().startsWith("bbox:")) { //test.toLowerCase
            String bboxSA[] = String2.split(foi.substring(5), ',');
            if (bboxSA.length != 4)
                throw new SimpleException(
                    EDStatic.queryError + "featureOfInterest=BBOX must have 4 comma-separated values.");

            double d1 = String2.parseDouble(bboxSA[0]);
            double d2 = String2.parseDouble(bboxSA[2]);
            if (d1 == d2) { //NaN==NaN? returns false
                dapQuery.append("&longitude=" + d1);
            } else {
                if (!Double.isNaN(d1)) dapQuery.append("&longitude>=" + d1);
                if (!Double.isNaN(d2)) dapQuery.append("&longitude<=" + d2);
            }

            d1 = String2.parseDouble(bboxSA[1]);
            d2 = String2.parseDouble(bboxSA[3]);
            if (d1 == d2) { //NaN==NaN? returns false
                dapQuery.append("&latitude=" + d1);
            } else {
                if (!Double.isNaN(d1)) dapQuery.append("&latitude>=" + d1);
                if (!Double.isNaN(d2)) dapQuery.append("&latitude<=" + d2);
            }
        } else {
            throw new SimpleException(
                EDStatic.queryError + "featureOfInterest=" + foi + " is invalid. Only BBOX is allowed.");
        }

        //add dataVariables to start of dapQuery
        String requestedVarsCsv = String2.replaceAll(requestedVars.toString(), " ", "");
        if (responseFormat.equals(OutputStreamFromHttpResponse.KML_MIME_TYPE)) {

            //GoogleEarth -- always all variables

        } else if (String2.indexOf(sosImageResponseFormats, responseFormat) >= 0) {

            //Note that any requestedVars will be dv names (not the dataset name, which leads to requestedVars="")
            
            //responseFormat is an image type
            if (requestedVars.size() == 0) {
                throw new SimpleException(EDStatic.queryError + "for image responseFormats (e.g., " + responseFormat + 
                    "), observedProperty must list one or more simple observedProperties."); 

            } else if (requestedVars.size() == 1) {

                EDV edv1 = findDataVariableByDestinationName(requestedVars.get(0));
                if (edv1.destinationDataTypeClass().equals(String.class)) 
                    throw new SimpleException(EDStatic.queryError + "for image responseFormats, observedProperty=" + 
                        requestedVars.get(0) + " can't be a String phenomena."); 

                if (whichOffering == -1) {
                    //network -> map
                    dapQuery.insert(0, "longitude,latitude," + requestedVars.get(0));  
                    dapQuery.append("&.draw=markers&.marker=5|4");   //filled square|smaller
                } else {
                    //1 station, 1 var -> time series
                    dapQuery.insert(0, "time," + requestedVars.get(0));
                    dapQuery.append("&.draw=linesAndMarkers&.marker=5|4&.color=0xFF9900");
                }

            } else {
                //1 station, >=2 vars -> use first 2 vars
                EDV edv1 = findDataVariableByDestinationName(requestedVars.get(0));
                EDV edv2 = findDataVariableByDestinationName(requestedVars.get(1));
                if (edv1.destinationDataTypeClass().equals(String.class)) 
                    throw new SimpleException(EDStatic.queryError + "for image responseFormats, observedProperty=" + 
                        requestedVars.get(0) + " can't be a String phenomena."); 
                if (edv2.destinationDataTypeClass().equals(String.class)) 
                    throw new SimpleException(EDStatic.queryError + "for image responseFormats, observedProperty=" + 
                        requestedVars.get(1) + " can't be a String phenomena."); 

                if (edv1.ucumUnits() != null && edv2.ucumUnits() != null &&
                    edv1.ucumUnits().equals(edv2.ucumUnits())) {

                    //the 2 variables have the same ucumUnits
                    if (whichOffering == -1) {
                        //network -> map with vectors
                        dapQuery.insert(0, "longitude,latitude," + requestedVars.get(0) + 
                            "," + requestedVars.get(1));  
                        dapQuery.append("&.draw=vectors&.color=0xFF9900");
                        String colorBarMax = edv1.combinedAttributes().getString("colorBarMaximum"); //string avoids rounding problems
                        if (!Double.isNaN(String2.parseDouble(colorBarMax)))
                            dapQuery.append("&.vec=" + colorBarMax);
                    } else {
                        //1 station, 2 compatible var -> sticks
                        dapQuery.insert(0, "time," + requestedVars.get(0) + 
                            "," + requestedVars.get(1));
                        dapQuery.append("&.draw=sticks&.color=0xFF9900");
                    }
                } else {                     
                    //the 2 variables don't have compatible ucumUnits
                    if (whichOffering == -1) {
                        //network -> map with markers
                        dapQuery.insert(0, "longitude,latitude," + requestedVars.get(0));  
                        dapQuery.append("&.draw=markers&.marker=5|4");  //filled square
                    } else {
                        //1 station, 2 incompatible vars -> var vs. var, with time as color
                        dapQuery.insert(0, requestedVars.get(0) + "," + requestedVars.get(1) + ",time");
                        dapQuery.append("&.draw=linesAndMarkers&.marker=5|4&.color=0xFF9900");
                    }                

                //leftover?
                //dapQuery.insert(0, "time," + requestedVars.get(0));
                //dapQuery.append("&.draw=linesAndMarkers&.marker=5|3&.color=0xFF9900");
                }
            }
            
        } else {
            
            //responseFormat is a data type  (not IOOS XML; it is excluded above)
            if (requestedVars.size() > 0) {            
                //specific variables
                String llatiVars = "longitude,latitude" +
                    (altIndex >= 0? ",altitude" : depthIndex >= 0? ",depth" : "") +
                    ",time," + dataVariables[sosOfferingIndex].destinationName() + ",";
                dapQuery.insert(0, llatiVars + requestedVarsCsv);
            } //else "" gets all variables
        }

        if (reallyVerbose) 
            String2.log("sosQueryToDapQuery=" + dapQuery.toString() + 
            "\n  requestOfferingType=" + requestOfferingType + 
              " requestShortOfferingName=" + requestShortOfferingName);
        return new String[] {dapQuery.toString(), requestOfferingType, requestShortOfferingName,
            responseFormat, 
            null, //responseMode, 
            requestedVars.toString()};
    }


    /**
     * This returns a SOS *out-of-band* response.
     * See Observations and Measurements schema OGC 05-087r3 section 7.3.2.
     *
     * @param requestShortOfferingName  the short version
     * @param fileTypeName e.g., .csv  or .smallPng?
     * @param dapQuery    percent-encoded 
     * @param requestedVars csv of short observedProperties, or "" if all

     */
    public void sosObservationsXmlOutOfBand(String requestOfferingType, String requestShortOfferingName,
        String fileTypeName, String dapQuery, String requestedVars,
        Writer writer, String loggedInAs) throws Throwable {

        //if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
        //    throw new SimpleException(
        //        MessageFormat.format(EDStatic.notAuthorized, loggedInAs, datasetID));

        //if (accessibleViaSOS().length() > 0)
        //    throw new SimpleException(accessibleViaSOS());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
        String stationGmlNameStart = getSosGmlNameStart(sosOfferingType);
        String sensorGmlNameStart = getSosGmlNameStart("sensor");
        String requestedVarAr[] = requestedVars.length() == 0? new String[0] : 
            String2.split(requestedVars, ',');

        //parse the dapQuery
        String minLon = null, maxLon = null, minLat = null, maxLat = null, 
            minTime = null, maxTime = null;
        String parts[] = getUserQueryParts(dapQuery); //decoded.  always at least 1 part (may be "")
        for (int p = 0; p < parts.length; p++) {
            String part = parts[p];
            if      (part.startsWith("longitude>=")) {minLon = part.substring(11); }
            else if (part.startsWith("longitude<=")) {maxLon = part.substring(11); }
            else if (part.startsWith("longitude="))  {minLon = part.substring(10); maxLon = part.substring(10); }
            else if (part.startsWith("latitude>="))  {minLat = part.substring(10); }
            else if (part.startsWith("latitude<="))  {maxLat = part.substring(10); }
            else if (part.startsWith("latitude="))   {minLat = part.substring(9);  maxLat = part.substring(9); }
            else if (part.startsWith("time>="))      {minTime = part.substring(6); }
            else if (part.startsWith("time<="))      {maxTime = part.substring(6); }
            else if (part.startsWith("time="))       {minTime = part.substring(5); maxTime = part.substring(5); }
        }

        //find stations which match
        boolean allStations = requestShortOfferingName.equals(datasetID);
        int nSosOfferings = sosOfferings.size();
        BitSet keepBitset = new BitSet(nSosOfferings); //all clear
        if (allStations) {
            keepBitset.set(0, nSosOfferings); //all set
            if (minLon != null && maxLon != null) {  //they should both be null, or both be not null
                double minLonD = String2.parseDouble(minLon);
                double maxLonD = String2.parseDouble(maxLon);
                int offering = keepBitset.nextSetBit(0);
                while (offering >= 0) {
                    if (sosMaxLon.getDouble(offering) < minLonD ||
                        sosMinLon.getDouble(offering) > maxLonD)
                        keepBitset.clear(offering);
                    offering = keepBitset.nextSetBit(offering + 1);
                }
            }
            if (minLat != null && maxLat != null) {  //they should both be null, or both be not null
                double minLatD = String2.parseDouble(minLat);
                double maxLatD = String2.parseDouble(maxLat);
                int offering = keepBitset.nextSetBit(0);
                while (offering >= 0) {
                    if (sosMaxLat.getDouble(offering) < minLatD ||
                        sosMinLat.getDouble(offering) > maxLatD)
                        keepBitset.clear(offering);
                    offering = keepBitset.nextSetBit(offering + 1);
                }
            }
            if (minTime != null && maxTime != null) {  //they should both be null, or both be not null
                double minTimeD = String2.parseDouble(minTime);
                double maxTimeD = String2.parseDouble(maxTime);
                int offering = keepBitset.nextSetBit(0);
                double days3 = 3 * Calendar2.SECONDS_PER_DAY;
                while (offering >= 0) {
                    //time is different: min or max may be NaN
                    double stationMinTime = sosMinTime.getDouble(offering);
                    double stationMaxTime = sosMaxTime.getDouble(offering) + days3; //pretend it has 3 more days of data
                    if ((!Double.isNaN(stationMaxTime) && stationMaxTime < minTimeD) ||
                        (!Double.isNaN(stationMinTime) && stationMinTime > maxTimeD))
                        keepBitset.clear(offering);
                    offering = keepBitset.nextSetBit(offering + 1);
                }
            }
        } else {
            keepBitset.set(sosOfferings.indexOf(requestShortOfferingName));
        }
        int nValidOfferings = keepBitset.cardinality();
        if (nValidOfferings == 0)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                " (There are no valid offerings.)");
            

        //write the response
        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<om:CompositeObservation xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" + //ogc doesn't show 1.0.2
"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n" +
"  gml:id=\"" + datasetID +  //was WindsPointCollection
  "TimeSeriesObservation\">\n" + 
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <gml:description>" + datasetID + //was Winds 
  " observations at a series of times</gml:description>\n" +
"  <gml:name>" + XML.encodeAsXML(title) + //was NOAA.NWS.NDBC
  ", " + requestOfferingType + " " + requestShortOfferingName + //was  observations at station ID 41004
  "</gml:name>\n");

        if (minLon != null && minLat != null &&
            maxLon != null && maxLat != null) {
            writer.write(
"  <gml:boundedBy>\n" +
"    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"      <gml:lowerCorner>" + minLat + " " + minLon + "</gml:lowerCorner>\n" + //lat lon
"      <gml:upperCorner>" + maxLat + " " + maxLon + "</gml:upperCorner>\n" +
"    </gml:Envelope>\n" +
"  </gml:boundedBy>\n");
        }

        if (minTime != null && maxTime != null) {
            writer.write(
"  <om:samplingTime>\n" +
"    <gml:TimePeriod gml:id=\"ST\">\n" +
"      <gml:beginPosition>" + minTime + "</gml:beginPosition>\n" +
"      <gml:endPosition>"   + maxTime + "</gml:endPosition>\n" +
"    </gml:TimePeriod>\n" +
"  </om:samplingTime>\n");
        }

        //procedure 
        writer.write(
"  <om:procedure>\n" +
"    <om:Process>\n" +
"      <ioos:CompositeContext gml:id=\"SensorMetadata\"" +
//??? Jeff says recDef may change or go away
//"        recDef=\"http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/recordDefinitions/PointSensorMetadataRecordDefinition.xml\"" + 
        ">\n" +
"        <gml:valueComponents>\n" +
"          <ioos:Count name=\"NumberOf" + sosOfferingType + "s\">" + nValidOfferings + //plural=s goofy but simple
             "</ioos:Count>\n" + 
"          <ioos:ContextArray gml:id=\"" + sosOfferingType + "Array\">\n" +
"            <gml:valueComponents>\n");

        //for each station
        int count = 0;
        int offering = keepBitset.nextSetBit(0);
        while (offering >= 0) {
            //write station info
            count++;
            writer.write(
"              <ioos:CompositeContext gml:id=\"" + sosOfferingType + count + "Info\">\n" +
"                <gml:valueComponents>\n" +
//???ioos hard codes "Station"
"                  <ioos:StationName>" + sosOfferingType + " - " + sosOfferings.getString(offering) + "</ioos:StationName>\n" +
"                  <ioos:Organization>" + institution() + "</ioos:Organization>\n" +
"                  <ioos:StationId>" + stationGmlNameStart + sosOfferings.getString(offering) + "</ioos:StationId>\n");

            if (sosMinLon.getString(offering).equals(sosMaxLon.getString(offering)))
                writer.write(
//Already reported to Jeff: one lat lon for all time?! but some stations move a little!
"                  <gml:Point gml:id=\"" + sosOfferingType + count + "LatLon\">\n" +
"                    <gml:pos>" + sosMinLon.getString(offering) + " " +
                                  sosMinLat.getString(offering) + "</gml:pos>\n" +
"                  </gml:Point>\n");
            else writer.write(
"                  <gml:boundedBy>\n" + 
"                    <gml:Box srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"" + requestOfferingType + 
                        "_location\">\n" +  
"                      <gml:coordinates>" + 
                          sosMinLat.getString(offering) + "," + 
                          sosMinLon.getString(offering) + " " + 
                          sosMaxLat.getString(offering) + "," + 
                          sosMaxLon.getString(offering) + "</gml:coordinates>\n" +
"                    </gml:Box>\n" +
"                  </gml:boundedBy>\n");

            int nSosVars = dataVariables.length - 4; //Lon, Lat, Time, ID
            if (altIndex >= 0 || depthIndex >= 0) nSosVars--;
            writer.write(
//vertical crs: http://www.oostethys.org/best-practices/verticalcrs
//???Eeek. I don't keep this info     allow it to be specified in combinedGlobalAttributes???
//But it may be different for different stations (e.g., buoy vs affixed to dock)
//  or even within a dataset (ndbcMet: wind/air measurements vs water measurements)
"                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n" +
//???one vertical position!!!     for now, always missing value
"                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n" +
"                  <ioos:Count name=\"" + sosOfferingType + count + "NumberOfSensors\">" + 
                       (requestedVarAr.length == 0? 1 : requestedVarAr.length) + 
                        "</ioos:Count>\n" +
"                  <ioos:ContextArray gml:id=\"" + sosOfferingType + count + "SensorArray\">\n" +
"                    <gml:valueComponents>\n");

            //these are the sensors in the response (not the available sensors)
            if (requestedVarAr.length == 0) {
                //sensor for entire dataset
                writer.write(
"                      <ioos:CompositeContext gml:id=\"" + sosOfferingType + count + "Sensor1Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>" + sensorGmlNameStart + sosOfferings.getString(offering) + 
                             //":" + datasetID + 
                             "</ioos:SensorId>\n" +
                             //IOOS sos had <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n");
            } else {

                //sensor for each non-LLATId var
                for (int rv = 0; rv < requestedVarAr.length; rv++) {
                    writer.write(
"                      <ioos:CompositeContext gml:id=\"" + sosOfferingType + count + 
                         "Sensor" + (rv + 1) + "Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>" + sensorGmlNameStart + sosOfferings.getString(offering) + 
                             ":" + requestedVarAr[rv] + "</ioos:SensorId>\n" +
                             //IOOS sos had <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n");
                }
            }

            writer.write(
"                    </gml:valueComponents>\n" +
"                  </ioos:ContextArray>\n" +
"                </gml:valueComponents>\n" +
"              </ioos:CompositeContext>\n");

            offering = keepBitset.nextSetBit(offering + 1);
        } //end of offering loop

        writer.write(
"            </gml:valueComponents>\n" +
"          </ioos:ContextArray>\n" +
"        </gml:valueComponents>\n" +
"      </ioos:CompositeContext>\n" +
"    </om:Process>\n" +
"  </om:procedure>\n");

        writer.write(
//use a local phenomenaDictionary
//"  <om:observedProperty xlink:href=\"http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml#Winds\"/>\n" +
"  <om:observedProperty xlink:href=\"" + tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl + 
    "#" + datasetID + "\"/>\n" +
"  <om:featureOfInterest xlink:href=\"" + XML.encodeAsXML(sosFeatureOfInterest()) + "\"/>\n" +
"  <om:result xlink:href=\"" + 
    XML.encodeAsXML(tErddapUrl + "/tabledap/" + datasetID + fileTypeName + "?" + dapQuery) + 
    "\"" +
//???role? 
//    " xlink:role=\"application/xmpp\" +
//    " xsi:type=\"gml:ReferenceType\" + 
    "/>\n" +
"</om:CompositeObservation>\n");

        //essential
        writer.flush();
    }

    /**
     * This returns the IOOS SOS *inline* observations xml for this dataset (see Erddap.doSos).
     * This should only be called if accessibleViaSOS is "" and
     *    loggedInAs has access to this dataset
     *    (so redirected to login, instead of getting an error here);
     *    this doesn't check.
     *
     * <p>This seeks to mimic IOOS SOS servers like ndbcSosWind.
     * See http://sdf.ndbc.noaa.gov/sos/ .
     *
     * @param requesttOfferingType e.g., network or e.g., station
     * @param requestShortOfferingName e.g., all or 41004 or ...
     * @param tw with all of the results data.  Units should be UCUM already.
     * @param writer In the end, the writer is flushed, not closed.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     */
    public void sosObservationsXmlInlineIoos(String requestOfferingType, String requestShortOfferingName,
        TableWriterAllWithMetadata tw, Writer writer, 
        String loggedInAs) throws Throwable {

        //if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
        //    throw new SimpleException(
        //        MessageFormat.format(EDStatic.notAuthorized, loggedInAs, datasetID));

        //if (accessibleViaSOS().length() > 0)
        //    throw new SimpleException(accessibleViaSOS());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
        String stationGmlNameStart = getSosGmlNameStart(sosOfferingType);
        String sensorGmlNameStart = getSosGmlNameStart("sensor");
        String datasetObservedProperty = datasetID;

        //It's quick and easy to write out the info if cumulativeTable is available.
        //It's very cumbersome any other way.
        //It may need a lot of memory, but hopefully just for a short time (tested by caller).
        Table table = tw.cumulativeTable(); 
        table.convertToStandardMissingValues(); //so missing_values are NaNs

        //ensure lon, lat, time, id are present  (and alt if eddTable has alt)
        String idName = dataVariableDestinationNames[sosOfferingIndex];
        int tLonIndex   = table.findColumnNumber(EDV.LON_NAME);
        int tLatIndex   = table.findColumnNumber(EDV.LAT_NAME);
        int tAltIndex   = table.findColumnNumber(EDV.ALT_NAME);
        int tDepthIndex = table.findColumnNumber(EDV.DEPTH_NAME);
        int tTimeIndex  = table.findColumnNumber(EDV.TIME_NAME);
        int tIdIndex    = table.findColumnNumber(idName);
        if (tLonIndex < 0)  throw new RuntimeException(EDStatic.errorInternal + "'longitude' isn't in the response table.");
        if (tLatIndex < 0)  throw new RuntimeException(EDStatic.errorInternal + "'latitude' isn't in the response table.");
        if (altIndex >= 0 && tAltIndex < 0) //yes, first one is alt, not tAlt
                            throw new RuntimeException(EDStatic.errorInternal + "'altitude' isn't in the response table.");
        if (depthIndex >= 0 && tDepthIndex < 0) //yes, first one is depth, not tDepth
                            throw new RuntimeException(EDStatic.errorInternal + "'depth' isn't in the response table.");
        if (tTimeIndex < 0) throw new RuntimeException(EDStatic.errorInternal + "'time' isn't in the response table.");
        if (tIdIndex < 0)   throw new RuntimeException(EDStatic.errorInternal + "'" + idName + "' isn't in the response table.");

        //Ensure it is sorted by tIdIndex, tTimeIndex, [tAltIndex|tDepthIndex] (it usually is, but make sure; it is assumed below)
        int sortIndices[] = tAltIndex   >= 0? new int[]{tIdIndex, tTimeIndex, tAltIndex} : 
                            tDepthIndex >= 0? new int[]{tIdIndex, tTimeIndex, tDepthIndex} : 
                                              new int[]{tIdIndex, tTimeIndex};
        boolean sortAscending[] = new boolean[sortIndices.length];
        Arrays.fill(sortAscending, true);
        table.sort(sortIndices, sortAscending);
        PrimitiveArray tIdPA = table.getColumn(tIdIndex);
        PrimitiveArray tTimePA = table.getColumn(tTimeIndex);
        PrimitiveArray tLatPA = table.getColumn(tLatIndex);
        PrimitiveArray tLonPA = table.getColumn(tLonIndex);
        double minLon = tw.columnMinValue(tLonIndex);
        double maxLon = tw.columnMaxValue(tLonIndex);
        double minLat = tw.columnMinValue(tLatIndex);
        double maxLat = tw.columnMaxValue(tLatIndex);
        double minTime = tw.columnMinValue(tTimeIndex);
        double maxTime = tw.columnMaxValue(tTimeIndex);
        if (tLonPA instanceof FloatArray) {
            minLon = Math2.floatToDouble(minLon);
            maxLon = Math2.floatToDouble(maxLon);
        }
        if (tLatPA instanceof FloatArray) {
            minLat = Math2.floatToDouble(minLat);
            maxLat = Math2.floatToDouble(maxLat);
        }
        int nRows = tIdPA.size();
        int nCols = table.nColumns();

        //count n stations
        String prevID = "\uFFFF";
        int nStations = 0;
        for (int row = 0; row < nRows; row++) {
            String currID = tIdPA.getString(row);
            if (!currID.equals(prevID)) {
                nStations++;
                prevID = currID;
            }
        }

        //gather column names and ucumUnits
        String xmlColNames[] = new String[nCols];
        String xmlColUcumUnits[] = new String[nCols];  //ready to use
        for (int col = 0; col < nCols; col++) {
            xmlColNames[col] = XML.encodeAsXML(table.getColumnName(col));
            String u = table.columnAttributes(col).getString("units");
            if (EDV.TIME_UCUM_UNITS.equals(u)) 
                u = "UTC";
            xmlColUcumUnits[col] = u == null || u.length() == 0?
                "" : " uom=\"" + XML.encodeAsXML(u) + "\"";
        }
        int nSensors = nCols - 4 - (tAltIndex >= 0 || tDepthIndex >= 0? 1 : 0); //4=LLTI
        boolean compositeSensor = nCols == dataVariables.length;


        //write start of xml content for IOOS response
        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<om:CompositeObservation xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"" + //ogc doesn't show 1.0.2
"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n" +
"  gml:id=\"" + datasetID +  //was WindsPointCollection
  "TimeSeriesObservation\">\n" + 
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <gml:description>" + datasetID + //was Winds 
  " observations at a series of times</gml:description>\n" +
"  <gml:name>" + XML.encodeAsXML(title) + 
  ", " + requestOfferingType + " " + requestShortOfferingName + //was  observations at station ID 41004
  "</gml:name>\n" +
"  <gml:boundedBy>\n" +
"    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"      <gml:lowerCorner>" + minLat + " " + minLon + "</gml:lowerCorner>\n" + //lat lon
"      <gml:upperCorner>" + maxLat + " " + maxLon + "</gml:upperCorner>\n" +
"    </gml:Envelope>\n" +
"  </gml:boundedBy>\n" +
"  <om:samplingTime>\n" +
"    <gml:TimePeriod gml:id=\"ST\">\n" +
"      <gml:beginPosition>" + Calendar2.epochSecondsToIsoStringT(tw.columnMinValue(tTimeIndex)) + "Z</gml:beginPosition>\n" +
"      <gml:endPosition>"   + Calendar2.epochSecondsToIsoStringT(tw.columnMaxValue(tTimeIndex)) + "Z</gml:endPosition>\n" +
"    </gml:TimePeriod>\n" +
"  </om:samplingTime>\n" +
"  <om:procedure>\n" +
"    <om:Process>\n" +
"      <ioos:CompositeContext gml:id=\"SensorMetadata\"" +
//??? Jeff says recDef may change or go away
//"        recDef=\"http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/recordDefinitions/PointSensorMetadataRecordDefinition.xml\"" + 
        ">\n" +
"        <gml:valueComponents>\n" +
"          <ioos:Count name=\"NumberOf" + sosOfferingType + "s\">" + nStations + "</ioos:Count>\n" + //plural=s goofy but simple
"          <ioos:ContextArray gml:id=\"" + sosOfferingType + "Array\">\n" +
"            <gml:valueComponents>\n");

        //for each station
        //find firstStationRow for next station (then write info for previous station)
        int stationNumber = 0;
        int firstStationRow = 0;
        prevID = tIdPA.getString(0);
        for (int nextFirstStationRow = 1; nextFirstStationRow <= nRows; nextFirstStationRow++) { //1,<= because we're looking back
            String currID = nextFirstStationRow == nRows? "\uFFFF" : tIdPA.getString(nextFirstStationRow);
            if (prevID.equals(currID))
                continue;

            //a new station!
            //write station info
            stationNumber++;  //firstStationRow and prevID are changed at end of loop
            writer.write(
"              <ioos:CompositeContext gml:id=\"" + sosOfferingType + stationNumber + "Info\">\n" +
"                <gml:valueComponents>\n" +
//???ioos hard codes "Station"
"                  <ioos:StationName>" + sosOfferingType + " - " + prevID + "</ioos:StationName>\n" +
"                  <ioos:Organization>" + institution() + "</ioos:Organization>\n" +
"                  <ioos:StationId>" + stationGmlNameStart + prevID + "</ioos:StationId>\n" +
"                  <gml:Point gml:id=\"" + sosOfferingType + stationNumber + "LatLon\">\n" +
//Already reported to Jeff: one lat lon for all time?! but some stations move a little!
//And what about non-stations, e.g., trajectories?
"                    <gml:pos>" + tLatPA.getNiceDouble(firstStationRow) + " " +
                                  tLonPA.getNiceDouble(firstStationRow) + "</gml:pos>\n" +
"                  </gml:Point>\n" +
//vertical crs: http://www.oostethys.org/best-practices/verticalcrs
//???Eeek. I don't keep this info     allow it to be specified in combinedGlobalAttributes???
//But it may be different for different stations (e.g., buoy vs affixed to dock)
//  or even within a dataset (ndbcMet: wind/air measurements vs water measurements)
"                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n" +
//???one vertical position!!!     for now, always missing value
"                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n");

            if (compositeSensor) {
                //observedProperty was datasetID: platform's sensor
                writer.write(
                   //composite observedProperty treats each station as having one observedProperty 
                   //and one "sensor"
"                  <ioos:Count name=\"" + sosOfferingType + stationNumber + 
                    "NumberOfSensors\">1</ioos:Count>\n" +
"                  <ioos:ContextArray gml:id=\"" + sosOfferingType + stationNumber + "SensorArray\">\n" +
"                    <gml:valueComponents>\n" +
"                      <ioos:CompositeContext gml:id=\"" + sosOfferingType + stationNumber + "Sensor1Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>" + sensorGmlNameStart + prevID + ":" + 
                             datasetObservedProperty + "</ioos:SensorId>\n" +
                             //ioos has <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n" +
"                    </gml:valueComponents>\n" +
"                  </ioos:ContextArray>\n");
            } else {
                //observedProperty was csv variable list: separate sensors
                writer.write(
"                  <ioos:Count name=\"" + sosOfferingType + stationNumber + 
                    "NumberOfSensors\">" + nSensors + "</ioos:Count>\n" +
"                  <ioos:ContextArray gml:id=\"" + sosOfferingType + stationNumber + "SensorArray\">\n" +
"                    <gml:valueComponents>\n");
                int sensori = 0;
                for (int c = 0; c < nCols; c++) {
                    if (c == tLonIndex  || c == tLatIndex || c == tAltIndex || c == tDepthIndex ||
                        c == tTimeIndex || c == tIdIndex)
                    continue;
                    sensori++;
                    writer.write(
"                      <ioos:CompositeContext gml:id=\"" + sosOfferingType + stationNumber + 
                        "Sensor" + sensori + "Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>" + sensorGmlNameStart + prevID + ":" + 
                             table.getColumnName(c) +
                             "</ioos:SensorId>\n" +
                             //ioos has <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n");
                }
                writer.write(
"                    </gml:valueComponents>\n" +
"                  </ioos:ContextArray>\n");
            }

            writer.write(
"                </gml:valueComponents>\n" +
"              </ioos:CompositeContext>\n");

            firstStationRow = nextFirstStationRow;
            prevID = currID;
        }

        writer.write(
"            </gml:valueComponents>\n" +
"          </ioos:ContextArray>\n" +
"        </gml:valueComponents>\n" +
"      </ioos:CompositeContext>\n" +
"    </om:Process>\n" +
"  </om:procedure>\n" +

//use a local phenomenaDictionary
//"  <om:observedProperty xlink:href=\"http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml#Winds\"/>\n" +
"  <om:observedProperty xlink:href=\"" + tErddapUrl + "/sos/" + datasetID + "/" + 
    sosPhenomenaDictionaryUrl + "#" + datasetID + "\"/>\n" +
"  <om:featureOfInterest xlink:href=\"" + XML.encodeAsXML(sosFeatureOfInterest()) + "\"/>\n");


       writer.write(
"  <om:result>\n" +
//???Jeff says recordDefinitions may change or go away
"    <ioos:Composite " + //recDef=\"http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/recordDefinitions/WindsPointDataRecordDefinition.xml\" " +
  "gml:id=\"" + datasetID + "PointCollectionTimeSeriesDataObservations\">\n" +
"      <gml:valueComponents>\n" +
"        <ioos:Count name=\"NumberOfObservationsPoints\">" + nStations + "</ioos:Count>\n" + 
"        <ioos:Array gml:id=\"" + datasetID + "PointCollectionTimeSeries\">\n" +
"          <gml:valueComponents>\n");

        //for each station
        //find firstStationRow for next station (then write info for previous station)
        //and nTimes for this station
        stationNumber = 0;
        firstStationRow = 0;
        prevID = tIdPA.getString(0);
        int nTimes = 0;
        double prevTime = tTimePA.getDouble(0);
        for (int nextFirstStationRow = 1; nextFirstStationRow <= nRows; nextFirstStationRow++) { //1,<= because we're looking back
            //nTimes will be too low by 1 until nextStation found, then correct.
            double currTime = nextFirstStationRow == nRows? Double.MAX_VALUE : 
                tTimePA.getDouble(nextFirstStationRow);
            if (prevTime != currTime) {
                nTimes++;
                prevTime = currTime;
            }

            String currID = nextFirstStationRow == nRows? "\uFFFF" : 
                tIdPA.getString(nextFirstStationRow);
            if (prevID.equals(currID))
                continue;

            //a new station!
            //write station info
            stationNumber++;  //firstStationRow and prevID are changed at end of loop
            String stationN = sosOfferingType + stationNumber;
            writer.write(
"            <ioos:Composite gml:id=\"" + stationN + "TimeSeriesRecord\">\n" +
"              <gml:valueComponents>\n" +
"                <ioos:Count name=\"" + stationN + "NumberOfObservationsTimes\">" +
                    nTimes + "</ioos:Count>\n" +
"                <ioos:Array gml:id=\"" + stationN + 
                    (nTimes == nextFirstStationRow - firstStationRow ? "" : "Profile") + 
                    "TimeSeries\">\n" +
"                  <gml:valueComponents>\n");

            //rows for one station
            int nthTime = 0;
            int sTimeStartRow = firstStationRow;
            for (int sRow = firstStationRow; sRow < nextFirstStationRow; sRow++) {
                double tTimeD = tTimePA.getDouble(sRow);
                if (sRow < nextFirstStationRow -1 && tTimeD == tTimePA.getDouble(sRow + 1)) //nextTime
                    continue;

                //for this station, this is the last row with this time
                nthTime++; //this is the nth time for this station (1..)
                int nTimesThisTime = sRow - sTimeStartRow + 1;  //n rows share this time

                String stationNTN = sosOfferingType + stationNumber + "T" + nthTime;
                String tTimeS = Double.isNaN(tTimeD)?
                    " indeterminatePosition=\"unknown\">":  //???correct???
                    ">" + Calendar2.epochSecondsToIsoStringT(tTimeD) + "Z";

                writer.write(
"                    <ioos:Composite gml:id=\"" + stationNTN + 
                       (nTimesThisTime == 1? "Point" : "Profile") +
                       "\">\n" +
"                      <gml:valueComponents>\n" +
"                        <ioos:CompositeContext gml:id=\"" + 
                            stationNTN + "ObservationConditions\" " +
                           "processDef=\"#" + stationN + "Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <gml:TimeInstant gml:id=\"" + stationNTN + "Time\">\n" +
"                              <gml:timePosition" + tTimeS + "</gml:timePosition>\n" +
"                            </gml:TimeInstant>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeContext>\n");

                if (nTimesThisTime > 1) {
                    writer.write(
"                        <ioos:Count name=\"" + stationNTN + "NumberOfBinObservations\">" + 
                           nTimesThisTime + "</ioos:Count>\n" +
"                        <ioos:ValueArray gml:id=\"" + stationNTN + "ProfileObservations\">\n" +
"                          <gml:valueComponents>\n");
                }
                String tIndent = nTimesThisTime == 1? "" : "    ";

                //show the result rows that have same time
                String lastAltVal = "\u0000";
                for (int timeRow = sTimeStartRow; timeRow <= sRow; timeRow++) {
                    if (compositeSensor) {

                        if (nTimesThisTime == 1) {
                            writer.write(
"                        <ioos:CompositeValue gml:id=\"" + stationNTN + "PointObservation\" " +
                           "processDef=\"#" + stationN + "Sensor1Info\">\n" +
"                          <gml:valueComponents>\n");
                        } else if (tAltIndex < 0 && tDepthIndex < 0) {
                            //withinTimeIndex may not be tAltIndex or tDepthIndex, 
                            //so ensure there is an tAltIndex or tDepthIndex
                            throw new SimpleException("This dataset is not suitable for SOS: " +
                                "there are multiple values for one time, " + 
                                "but there is no altitude or depth variable.");
                        } else {
                            //withinTimeIndex may not be tAltIndex or tDepthIndex, 
                            //so ensure tAlt value changed
                            int tAltDepthIndex = tAltIndex >= 0? tAltIndex : tDepthIndex;
                            String val = table.getStringData(tAltDepthIndex, timeRow);
                            if (val.equals(lastAltVal))
                                throw new SimpleException(
                                    "This dataset is not suitable for SOS: " +
                                    "there are multiple values for one time, " +
                                    "but not because of the altitude or depth variable.");
                            lastAltVal = val;

                            writer.write(
"                            <ioos:CompositeValue gml:id=\"" + stationNTN + "Bin" + 
                               (timeRow - sTimeStartRow + 1) + "Obs\" processDef=\"#" + 
                               stationN + "Sensor1Info\">\n" +
"                              <gml:valueComponents>\n" +
"                                <ioos:Context name=\"" + xmlColNames[tAltDepthIndex] + "\"" + 
                                 xmlColUcumUnits[tAltDepthIndex]); 
                            writer.write(val.length() == 0?
                            " xsi:nil=\"true\" nilReason=\"unknown\"/>\n" :
                            ">" + XML.encodeAsXML(val) + "</ioos:Context>\n");
                        }


                        //write the values on this row  
                        //e.g., <ioos:Quantity name=\"WindDirection\" uom=\"deg\">229.0</ioos:Quantity>
                        //e.g., <ioos:Quantity name=\"WindVerticalVelocity\" uom=\"m/s\" xsi:nil=\"true\" nilReason=\"unknown\"/>
                        for (int col = 0; col < nCols; col++) {
                            //skip the special columns
                            if (col == tLonIndex  || col == tLatIndex || 
                                col == tAltIndex  || col == tDepthIndex || 
                                col == tTimeIndex || col == tIdIndex)
                                continue;
     
                            writer.write(tIndent + 
"                            <ioos:Quantity name=\"" + xmlColNames[col] + "\"" +
                                xmlColUcumUnits[col]);
                            String val = table.getStringData(col, timeRow);
                            writer.write(val.length() == 0?
                            " xsi:nil=\"true\" nilReason=\"unknown\"/>\n" :
                            ">" + XML.encodeAsXML(val) + "</ioos:Quantity>\n");
                        }

                        //end of observations on this row
                        writer.write(tIndent +
"                          </gml:valueComponents>\n" + tIndent +
"                        </ioos:CompositeValue>\n");

                    } else { 
                        //not composite sensor: separate sensors
                        int sensori = 0;
                        for (int col = 0; col < nCols; col++) {
                            //skip the special columns
                            if (col == tLonIndex  || col == tLatIndex || 
                                col == tAltIndex  || col == tDepthIndex || 
                                col == tTimeIndex || col == tIdIndex)
                                continue;

                            sensori++;
                            if (nTimesThisTime == 1) {
                                writer.write(
"                        <ioos:CompositeValue gml:id=\"" + stationNTN + 
                           "Sensor" + sensori + "PointObservation\" " +
                           "processDef=\"#" + stationN + "Sensor" + sensori + "Info\">\n" +
"                          <gml:valueComponents>\n");
                            } else if (tAltIndex < 0 && tDepthIndex < 0) {
                                //withinTimeIndex may not be tAltIndex or tDepthIndex, 
                                //so ensure there is an tAltIndex or tDepthIndex
                                throw new SimpleException("This dataset is not suitable for SOS: " +
                                    "there are multiple values for one time, " +
                                    "but there is no altitude variable.");
                            } else {
                                //withinTimeIndex may not be tAltIndex or tDepthIndex, 
                                //so ensure tAlt value changed
                                int tAltDepthIndex = tAltIndex >= 0? tAltIndex : tDepthIndex;
                                String val = table.getStringData(tAltDepthIndex, timeRow);
                                if (val.equals(lastAltVal))
                                    throw new SimpleException(
                                        "This dataset is not suitable for SOS: " +
                                        "there are multiple values for one time, " +
                                        "but not because of the altitude or depth variable.");
                                lastAltVal = val;

                                writer.write(
"                            <ioos:CompositeValue gml:id=\"" + stationNTN + "Bin" + 
                               (timeRow - sTimeStartRow + 1) + "Obs\" processDef=\"#" + 
                               stationN + "Sensor1Info\">\n" +
"                              <gml:valueComponents>\n" +
"                                <ioos:Context name=\"" + xmlColNames[tAltDepthIndex] + "\"" + 
                                    xmlColUcumUnits[tAltDepthIndex]); 
                                writer.write(val.length() == 0?
                            " xsi:nil=\"true\" nilReason=\"unknown\"/>\n" :
                            ">" + XML.encodeAsXML(val) + "</ioos:Context>\n");
                            }


                            //write the value on this row for this "sensor" (column)
                            //e.g., <ioos:Quantity name=\"WindDirection\" uom=\"deg\">229.0</ioos:Quantity>
                            //e.g., <ioos:Quantity name=\"WindVerticalVelocity\" uom=\"m/s\" xsi:nil=\"true\" nilReason=\"unknown\"/>    
                            writer.write(tIndent + 
"                            <ioos:Quantity name=\"" + xmlColNames[col] + "\"" + 
                                xmlColUcumUnits[col]);
                            String val = table.getStringData(col, timeRow);
                            writer.write(val.length() == 0?
                            " xsi:nil=\"true\" nilReason=\"unknown\"/>\n" :
                            ">" + XML.encodeAsXML(val) + "</ioos:Quantity>\n");

                            //end of observations on this row + this column
                            writer.write(tIndent +
"                          </gml:valueComponents>\n" + tIndent +
"                        </ioos:CompositeValue>\n");
                        } //end of col loop
                    } //end of treating cols as separate sensors
                } //end timeRow loop

                //end of observations for this time
                if (nTimesThisTime > 1) 
                    writer.write(
"                          </gml:valueComponents>\n" +
"                        </ioos:ValueArray\">\n");

                writer.write(
"                      </gml:valueComponents>\n" +
"                    </ioos:Composite>\n");

                //prepare for next time value
                sTimeStartRow = sRow + 1;
            } //end of sRow loop

            //end of station info        
            writer.write(
"                  </gml:valueComponents>\n" +
"                </ioos:Array>\n" +
"              </gml:valueComponents>\n" +
"            </ioos:Composite>\n");

            firstStationRow = nextFirstStationRow;
            prevID = currID;
        } //end of station loop

            writer.write(
"          </gml:valueComponents>\n" +
"        </ioos:Array>\n" +
"      </gml:valueComponents>\n" +
"    </ioos:Composite>\n" +
"  </om:result>\n" +
"</om:CompositeObservation>\n");


        //temporary
        //if (reallyVerbose) String2.log(table.saveAsCsvASCIIString());

        //essential
        writer.flush();
    }

    /**
     * This returns the Oostethys SOS *inline* observations xml for this dataset (see Erddap.doSos).
     * This should only be called if accessibleViaSOS is "" and
     *    loggedInAs has access to this dataset
     *    (so redirected to login, instead of getting an error here);
     *    this doesn't check.
     *
     * <p>This seeks to mimic Oostethys SOS servers like gomoosBuoy datasource
     * http://www.gomoos.org/cgi-bin/sos/oostethys_sos.cgi.
     *
     * @param requesttOfferingType e.g., network or e.g., station
     * @param requestShortOfferingName e.g., all or 41004 or ...
     * @param tw with all of the results data. Units should be UCUM already.
     * @param writer In the end, the writer is flushed, not closed.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     */
    public void sosObservationsXmlInlineOostethys(String requestOfferingType, 
        String requestShortOfferingName,
        TableWriterAllWithMetadata tw, Writer writer, 
        String loggedInAs) throws Throwable {

        //if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
        //    throw new SimpleException(
        //        MessageFormat.format(EDStatic.notAuthorized, loggedInAs, datasetID));

        //if (accessibleViaSOS().length() > 0)
        //    throw new SimpleException(accessibleViaSOS());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
        String stationGmlNameStart = getSosGmlNameStart(sosOfferingType);
        String sensorGmlNameStart = getSosGmlNameStart("sensor");
        String fullPhenomenaDictionaryUrl = tErddapUrl + "/sos/" + datasetID + 
            "/" + sosPhenomenaDictionaryUrl; 

        //It's quick and easy to write out the info if cumulativeTable is available.
        //It's very cumbersome any other way.
        //It may need a lot of memory, but hopefully just for a short time (tested by caller).
        Table table = tw.cumulativeTable(); 
        table.convertToStandardMissingValues(); //so missing_values are NaNs

        //ensure lon, lat, time, id are present in the table (and alt if eddTable has alt)
        String idName = dataVariableDestinationNames[sosOfferingIndex];
        int tLonIndex   = table.findColumnNumber(EDV.LON_NAME);
        int tLatIndex   = table.findColumnNumber(EDV.LAT_NAME);
        int tAltIndex   = table.findColumnNumber(EDV.ALT_NAME);
        int tDepthIndex = table.findColumnNumber(EDV.DEPTH_NAME);
        int tTimeIndex  = table.findColumnNumber(EDV.TIME_NAME);
        int tIdIndex    = table.findColumnNumber(idName);
        if (tLonIndex < 0)  throw new RuntimeException(EDStatic.errorInternal + "'longitude' isn't in the response table.");
        if (tLatIndex < 0)  throw new RuntimeException(EDStatic.errorInternal + "'latitude' isn't in the response table.");
        if (altIndex >= 0 && tAltIndex < 0) //yes, first one is alt, not tAlt
                            throw new RuntimeException(EDStatic.errorInternal + "'altitude' isn't in the response table.");
        if (depthIndex >= 0 && tDepthIndex < 0) //yes, first one is depth, not tDepth
                            throw new RuntimeException(EDStatic.errorInternal + "'depth' isn't in the response table.");
        if (tTimeIndex < 0) throw new RuntimeException(EDStatic.errorInternal + "'time' isn't in the response table.");
        if (tIdIndex < 0)   throw new RuntimeException(EDStatic.errorInternal + "'" + idName + "' isn't in the response table.");

        //Ensure it is sorted by tIdIndex, tTimeIndex, [tAltIndex|tDepthIndex] (it usually is, but make sure; it is assumed below)
        int sortIndices[] = tAltIndex   >= 0? new int[]{tIdIndex, tTimeIndex, tAltIndex} : 
                            tDepthIndex >= 0? new int[]{tIdIndex, tTimeIndex, tDepthIndex} : 
                                              new int[]{tIdIndex, tTimeIndex};
        boolean sortAscending[] = new boolean[sortIndices.length];
        Arrays.fill(sortAscending, true);
        table.sort(sortIndices, sortAscending);
        PrimitiveArray tIdPA    = table.getColumn(tIdIndex);
        PrimitiveArray tTimePA  = table.getColumn(tTimeIndex);
        PrimitiveArray tAltPA   = tAltIndex   >= 0? table.getColumn(tAltIndex)   : null;
        PrimitiveArray tDepthPA = tDepthIndex >= 0? table.getColumn(tDepthIndex) : null;
        PrimitiveArray tLatPA   = table.getColumn(tLatIndex);
        PrimitiveArray tLonPA   = table.getColumn(tLonIndex);
        int nRows = tIdPA.size();
        int nCols = table.nColumns();

        //count n stations
        String prevID = "\uFFFF";
        int nStations = 0;
        for (int row = 0; row < nRows; row++) {
            String currID = tIdPA.getString(row);
            if (!currID.equals(prevID)) {
                nStations++;
                prevID = currID;
            }
        }

        //gather column names and units
        String xmlColNames[] = new String[nCols];
        String xmlColUcumUnits[] = new String[nCols];  
        boolean isStringCol[] = new boolean[nCols];
        boolean isTimeStampCol[] = new boolean[nCols];
        for (int col = 0; col < nCols; col++) {
            xmlColNames[col] = XML.encodeAsXML(table.getColumnName(col));
            String u = table.columnAttributes(col).getString("units");
            isTimeStampCol[col] = EDV.TIME_UCUM_UNITS.equals(u);
            if (isTimeStampCol[col])
                u = "UTC";
            xmlColUcumUnits[col] = u == null || u.length() == 0?
                "" : XML.encodeAsXML(u);
            isStringCol[col] = table.getColumn(col) instanceof StringArray;
        }
        int nSensors = nCols - 4 - (tAltIndex >= 0 || tDepthIndex >= 0? 1 : 0); //4=LLTI
        boolean compositeSensor = nCols == dataVariables.length;
        double minLon = tw.columnMinValue(tLonIndex);
        double maxLon = tw.columnMaxValue(tLonIndex);
        double minLat = tw.columnMinValue(tLatIndex);
        double maxLat = tw.columnMaxValue(tLatIndex);
        double minTime = tw.columnMinValue(tTimeIndex);
        double maxTime = tw.columnMaxValue(tTimeIndex);
        if (tLonPA instanceof FloatArray) {
            minLon = Math2.floatToDouble(minLon);
            maxLon = Math2.floatToDouble(maxLon);
        }
        if (tLatPA instanceof FloatArray) {
            minLat = Math2.floatToDouble(minLat);
            maxLat = Math2.floatToDouble(maxLat);
        }

        //write start of xml content for Oostethys response
        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<om:Observation\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/0\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:om=\"http://www.opengis.net/om\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
//???
"  xsi:schemaLocation=\"http://www.opengis.net/om ../../../../../../ows4/schema0/swe/branches/swe-ows4-demo/om/current/commonObservation.xsd\"\n" +
"  gml:id=\"" + datasetID + "TimeSeriesObservation\">\n" + 
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <gml:description>" + datasetID + " observations at a series of times</gml:description>\n" +
"  <gml:name>" + XML.encodeAsXML(title) + 
  ", " + requestOfferingType + " " + requestShortOfferingName + 
  "</gml:name>\n" +
"  <gml:location>\n");
        if (minLon == maxLon && minLat == maxLat)
            writer.write(
"    <gml:Point gml:id=\"OBSERVATION_LOCATION\" srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
//"            <!-- use lat lon depth in deg deg m -->\n" +    //Oostethys had depth, too
"      <gml:coordinates>" + minLat + " " + minLon + "</gml:coordinates>\n" +
"    </gml:Point>\n");
        else writer.write(
"    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"      <gml:lowerCorner>" + minLat + " " + minLon + "</gml:lowerCorner>\n" + //lat lon
"      <gml:upperCorner>" + maxLat + " " + maxLon + "</gml:upperCorner>\n" +
"    </gml:Envelope>\n");
        writer.write(
"  </gml:location>\n" +
"  <om:time>\n");
        if (minTime == maxTime)
            writer.write(
            //use TimeInstant for a single measurement
"    <gml:TimeInstant gml:id=\"DATA_TIME\">\n" +
"      <gml:timePosition" + Calendar2.epochSecondsToIsoStringT(minTime) + "</gml:timePosition>\n" +
"    </gml:TimeInstant>\n");
        else writer.write(
            //use TimePeriod for a series of data 
"    <gml:TimePeriod gml:id=\"DATA_TIME\">\n" +
"      <gml:beginPosition>" + Calendar2.epochSecondsToIsoStringT(minTime) + "Z</gml:beginPosition>\n" +
"      <gml:endPosition>"   + Calendar2.epochSecondsToIsoStringT(maxTime) + "Z</gml:endPosition>\n" +
"    </gml:TimePeriod>\n");
        writer.write(
"  </om:time>\n");
        
        //Procedure
        writer.write(
"    <om:procedure xlink:href=\"" + 
            XML.encodeAsXML(sensorGmlNameStart +  
                (nStations > 1? datasetID : tIdPA.getString(0)) + ":" + 
                datasetID) + //???individual sensors?        
            "\"/>\n");

        //the property(s) measured 
        if (compositeSensor) {
            writer.write(
"    <om:observedProperty xlink:href=\"" + fullPhenomenaDictionaryUrl + "#" + datasetID + "\"/>\n");
        } else {
            for (int col = 0; col < nCols; col++) {
                if (col == tLonIndex  || col == tLatIndex || 
                    col == tAltIndex  || col == tDepthIndex ||
                    col == tTimeIndex || col == tIdIndex)
                    continue;
            writer.write(
"    <om:observedProperty xlink:href=\"" + fullPhenomenaDictionaryUrl + "#" + xmlColNames[col] + "\"/>\n");
            }
        }

        writer.write(        
//"    <!-- Feature Of Interest -->\n" +
"    <om:featureOfInterest xlink:href=\"" + XML.encodeAsXML(sosFeatureOfInterest()) + "\"/>\n" +
//"    <!-- Result Structure and Encoding -->\n" +
"    <om:resultDefinition>\n" +
"        <swe:DataBlockDefinition>\n" +
"            <swe:components name=\"" + datasetID + "DataFor" + 
        (nStations == 1? tIdPA.getString(0) : sosNetworkOfferingType) + //stationID : "network"
        "\">\n" +
"                <swe:DataRecord>\n" +
"                    <swe:field name=\"time\">\n" +
"                        <swe:Time definition=\"urn:ogc:phenomenon:time:iso8601\"/>\n" +
"                    </swe:field>\n" +
"                    <swe:field name=\"latitude\">\n" +
"                        <swe:Quantity definition=\"urn:ogc:phenomenon:latitude:wgs84\">\n" +
//???or like  <swe:uom code=\"m s-1\" /> ?
"                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n" +
"                        </swe:Quantity>\n" +
"                    </swe:field>\n" +
"                    <swe:field name=\"longitude\">\n" +
"                        <swe:Quantity definition=\"urn:ogc:phenomenon:longitude:wgs84\">\n" +
"                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n" +
"                        </swe:Quantity>\n" +
"                    </swe:field>\n");

        if (tAltIndex >= 0) {
            writer.write(
"                    <swe:field name=\"altitude\">\n" +  //gomoos had 'depth'
"                        <swe:Quantity definition=\"urn:ogc:phenomenon:altitude\">\n" + //gomoos had 'depth'
//???or like  <swe:uom code=\"m\" /> ?
"                            <swe:uom xlink:href=\"urn:ogc:unit:meter\"/>\n" +
"                        </swe:Quantity>\n" +
"                    </swe:field>\n");
        }

        if (tDepthIndex >= 0) {
            writer.write(
"                    <swe:field name=\"depth\">\n" +  //gomoos had 'depth'
"                        <swe:Quantity definition=\"urn:ogc:phenomenon:depth\">\n" + //gomoos had 'depth'
//???or like  <swe:uom code=\"m\" /> ?
"                            <swe:uom xlink:href=\"urn:ogc:unit:meter\"/>\n" +
"                        </swe:Quantity>\n" +
"                    </swe:field>\n");
        }

        for (int col = 0; col < nCols; col++) {
            if (col == tLonIndex  || col == tLatIndex || 
                col == tAltIndex  || col == tDepthIndex || 
                col == tTimeIndex || col == tIdIndex)
                continue;
            writer.write(         
"                    <swe:field name=\"" + xmlColNames[col] + "\">\n" +  
"                        <swe:Quantity definition=\"" + fullPhenomenaDictionaryUrl + "#" + 
                             xmlColNames[col] + "\">\n");
            if (xmlColUcumUnits[col].length() > 0) {
//???units!!!
//???or like  <swe:uom code=\"m.s-1\" /> ?
                //gomoos had <swe:uom xlink:href="urn:mm.def:units#psu"/>
                writer.write(
"                            <swe:uom xlink:href=\"urn:erddap.def:units#" + xmlColUcumUnits[col] + "\"/>\n");
            }
            writer.write(
"                        </swe:Quantity>\n" +
"                    </swe:field>\n");
        }
        writer.write(
"                </swe:DataRecord>\n" +
"            </swe:components>\n" +
"            <swe:encoding>\n" +
"                <swe:AsciiBlock tokenSeparator=\",\" blockSeparator=\" \" decimalSeparator=\".\"/>\n" +
"            </swe:encoding>\n" +
"        </swe:DataBlockDefinition>\n" +
"    </om:resultDefinition>\n" +
"    <om:result>");

        //rows of data
        String mvString = ""; //2009-09-21 Eric Bridger says Oostethys servers use ""
        for (int row = 0; row < nRows; row++) {
            if (row > 0)
                writer.write(' '); //blockSeparator

            //TLLA
            double tTime = tTimePA.getDouble(row);
            String tLat  = tLatPA.getString(row);
            String tLon  = tLonPA.getString(row);
            String tAlt  = tAltPA == null? null : tAltPA.getString(row);
            writer.write(
                Calendar2.safeEpochSecondsToIsoStringTZ(tTime, mvString) + "," +
                (tLat.length() == 0? mvString : tLat) + "," +
                (tLon.length() == 0? mvString : tLon) + //no end comma
                (tAlt == null? "" : tAlt.length() == 0? "," + mvString : "," + tAlt)); //no end comma

            //data columns
            //2007-07-04T00:00:00Z,43.7813,-69.8891,1,29.3161296844482 2007-07-04T00:00:00Z,43.7813,-69.8891,20,31.24924659729 ...   no end space
            for (int col = 0; col < nCols; col++) {
                if (col == tLonIndex  || col == tLatIndex || 
                    col == tAltIndex  || col == tDepthIndex ||
                    col == tTimeIndex || col == tIdIndex)
                    continue;
                writer.write(',');
                String tVal = table.getStringData(col, row);
                if (isStringCol[col])
                    writer.write(String2.toJson(tVal));            
                else if (isTimeStampCol[col])
                    writer.write(tVal.length() == 0? mvString : 
                        Calendar2.epochSecondsToIsoStringT(table.getDoubleData(col, row)) + "Z");            
                else writer.write(tVal.length() == 0? mvString : tVal);
            }
        }
        writer.write("</om:result>\n" +  //no preceding \n or space
"</om:Observation>\n");

        //essential
        writer.flush();
    }
    
/**
     * This writes the /sos/[datasetID]/index.html page to the writer.
     * <br>Currently, this just works as a SOS 1.0.0 server.
     * <br>The caller should have already checked loggedInAs and accessibleViaSOS().
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This doesn't check if eddTable is accessible to loggedInAs.
     *    The caller should do that.
     * @param writer  This doesn't write the header.
     *    <br>This starts with youAreHere.
     *    <br>This doesn't write the footer.
     *    <br>Afterwards, the writer is flushed, not closed.
     * @throws Throwable if trouble (there shouldn't be)
     */
    public void sosDatasetHtml(String loggedInAs, Writer writer) throws Throwable {
      
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);        
        String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
        String dictionaryUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl;
        int whichOffering = Math.max(0, sosOfferings.indexOf("41004")); //cheap trick for cwwcNDBCMet demo
        String shortOfferingi = sosOfferings.getString(whichOffering);
        String sensori   = getSosGmlNameStart("sensor")        + shortOfferingi; // + ":" + datasetID;
        String offeringi = getSosGmlNameStart(sosOfferingType) + shortOfferingi;
        String networkOffering = getSosGmlNameStart(sosNetworkOfferingType) + datasetID;
        EDV lonEdv  = dataVariables[lonIndex];
        EDV latEdv  = dataVariables[latIndex];
        EDV timeEdv = dataVariables[timeIndex];

        //suggest time range for offeringi
        double maxTime = sosMaxTime.getDouble(whichOffering);
        GregorianCalendar gc = Double.isNaN(maxTime)?
            Calendar2.newGCalendarZulu() :
            Calendar2.epochSecondsToGc(maxTime);
        String maxTimeS = Calendar2.formatAsISODateTimeT(gc) + "Z"; //use gc, not maxTime
        double minTime = Calendar2.backNDays(7, maxTime);
        String minTimeS = Calendar2.epochSecondsToIsoStringT(minTime) + "Z";
        double networkMinTime = Calendar2.backNDays(1, maxTime);
        String networkMinTimeS = Calendar2.epochSecondsToIsoStringT(networkMinTime) + "Z";

        //GetCapabilities
        String getCapabilitiesHtml = XML.encodeAsHTMLAttribute(
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetCapabilities");
        
        //DescribeSensor
        //http://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor&service=SOS
        // &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
        // &procedure=urn:ioos:sensor:noaa.nws.ndbc:41012:
        String describeSensorHtml = XML.encodeAsHTMLAttribute(
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=DescribeSensor" +
            "&outputFormat=" + SSR.minimalPercentEncode(sosDSOutputFormat) + 
            "&procedure=" + SSR.minimalPercentEncode(sensori));

        //find the first numeric non-LLATI var
        int fdv = String2.indexOf(dataVariableDestinationNames, "wtmp"); //cheap trick for cwwcNDBCMet demo
        if (fdv < 0) {
            for (int dv = 0; dv < dataVariables.length; dv++) {
                if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex || 
                    dv == timeIndex || dv == sosOfferingIndex || 
                    dataVariables[dv].destinationDataTypeClass() == String.class)
                    continue;
                fdv = dv;
                break;
            }        
        }
        String fdvName = fdv < 0? null : dataVariableDestinationNames[fdv];
        
        //find u,v variables (adjacent vars, same units), if possible
        int dvu = -1;
        int dvv = -1;
        for (int dv1 = dataVariables.length - 2; dv1 >= 0; dv1--) {  //working backwards works well with cwwcNDBCMet
            int dv2 = dv1 + 1;
            if (dv1 == lonIndex || dv1 == latIndex || dv1 == altIndex || dv1 == depthIndex || dv1 == timeIndex ||
                dv2 == lonIndex || dv2 == latIndex || dv2 == altIndex || dv2 == depthIndex || dv2 == timeIndex ||
                dv1 == sosOfferingIndex || dataVariables[dv1].destinationDataTypeClass() == String.class ||
                dv2 == sosOfferingIndex || dataVariables[dv2].destinationDataTypeClass() == String.class ||
                dataVariables[dv1].ucumUnits() == null ||
                dataVariables[dv2].ucumUnits() == null ||
                !dataVariables[dv1].ucumUnits().equals(dataVariables[dv2].ucumUnits()))
                continue;
            dvu = dv1;
            dvv = dv2;
            break;
        }        

        //find a(from fdv),b variables (different units), if possible
        int dva = -1;
        int dvb = -1;
        if (fdv >= 0) {
            for (int dv2 = 0; dv2 < dataVariables.length; dv2++) {  
                int dv1 = fdv;
                if (dv1 == dv2 ||
                    dv1 == lonIndex || dv1 == latIndex || dv1 == altIndex || dv1 == depthIndex || dv1 == timeIndex ||
                    dv2 == lonIndex || dv2 == latIndex || dv2 == altIndex || dv2 == depthIndex || dv2 == timeIndex ||
                    dv1 == sosOfferingIndex || dataVariables[dv1].destinationDataTypeClass() == String.class ||
                    dv2 == sosOfferingIndex || dataVariables[dv2].destinationDataTypeClass() == String.class ||
                    dataVariables[dv1].ucumUnits() == null ||
                    dataVariables[dv2].ucumUnits() == null ||
                    dataVariables[dv1].ucumUnits().equals(dataVariables[dv2].ucumUnits()))
                    continue;
                dva = dv1;
                dvb = dv2;
                break;
            }        
        }

        //GetObservatioon
        //"service=SOS&version=1.0.0&request=GetObservation" +
        //"&offering=urn:ioos:station:1.0.0.127.cwwcNDBCMet:41004:" +
        //"&observedProperty=cwwcNDBCMet" +
        //"&responseFormat=text/xml;schema=%22ioos/0.6.1%22" +
        //"&eventTime=2008-08-01T00:00:00Z/2008-08-01T04:00:00Z";
        String getObs = 
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + SSR.minimalPercentEncode(offeringi) +
            "&observedProperty=" + datasetID +
            "&eventTime=" + minTimeS + "/" + maxTimeS +
            "&responseFormat=";
        String getObservationHtml = XML.encodeAsHTML(
            getObs +
            SSR.minimalPercentEncode(sosDataResponseFormats[sosDefaultDataResponseFormat]));
        String bbox = "&featureOfInterest=BBOX:" + //show upper left quadrant
                lonEdv.destinationMinString() + "," + 
                latEdv.destinationMinString() + "," + 
                lonEdv.destinationMaxString() + "," + 
                latEdv.destinationMaxString();
        String getImageObsAllStations1Var = fdv < 0? null :
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + datasetID +
            "&observedProperty=" + fdvName +
            "&eventTime=" + minTimeS + "/" + maxTimeS +
            bbox +
            "&responseFormat=";
        String getImageObsAllStationsVector = dvu < 0? null :
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + datasetID +
            "&observedProperty=" + 
                dataVariableDestinationNames[dvu] + "," + 
                dataVariableDestinationNames[dvv] +
            "&eventTime=" + minTimeS +
            bbox +
            "&responseFormat=";
        String getImageObs1Station1Var = fdv < 0? null : 
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + SSR.minimalPercentEncode(offeringi) +
            "&observedProperty=" + fdvName +
            "&eventTime=" + minTimeS + "/" + maxTimeS +
            "&responseFormat=";
        String getImageObs1StationVector = dvu < 0? null :
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + SSR.minimalPercentEncode(offeringi) +
            "&observedProperty=" + 
                dataVariableDestinationNames[dvu] + "," + 
                dataVariableDestinationNames[dvv] +
            "&eventTime=" + minTimeS + "/" + maxTimeS +
            "&responseFormat=";
        String getImageObs1Station2Var = dva < 0? null :
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + SSR.minimalPercentEncode(offeringi) +
            "&observedProperty=" + 
                dataVariableDestinationNames[dva] + "," + 
                dataVariableDestinationNames[dvb] +
            "&eventTime=" + minTimeS + "/" + maxTimeS +
            "&responseFormat=";

        //network
        String getNetworkObservationHtml = XML.encodeAsHTMLAttribute(
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetObservation" +
            "&offering=" + SSR.minimalPercentEncode(networkOffering) +
            "&observedProperty=" + datasetID +
            "&eventTime=" + networkMinTimeS + "/" + maxTimeS +
            bbox +
            "&responseFormat=" + 
            SSR.minimalPercentEncode(sosDataResponseFormats[sosDefaultDataResponseFormat]));

        //observedPropertyOptions
        StringBuilder opoSB = new StringBuilder( 
            "the datasetID (for this dataset <tt>" + datasetID + "</tt>) (for all phenomena)\n" +
            "    <br>or a comma-separated list of simple phenomena (for this dataset, any subset of\n<br><tt>");
        int charsWritten = 0;
        for (int dv = 0; dv < dataVariables.length; dv++) {
            if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                dv == timeIndex || dv == sosOfferingIndex)
                continue;
            if (charsWritten > 0) 
                opoSB.append(",");
            String ts = dataVariableDestinationNames()[dv];
            if (charsWritten + ts.length() > 80) {
                opoSB.append("\n    <br>"); 
                charsWritten = 0; 
            }
            opoSB.append(ts);
            charsWritten += ts.length();
        }
        opoSB.append("</tt>)");
        String observedPropertyOptions = opoSB.toString();

        //*** html head
        //writer.write(EDStatic.startHeadHtml(tErddapUrl, title() + " - SOS"));
        //writer.write("\n" + rssHeadLink(loggedInAs));
        //writer.write("</head>\n");
        //writer.write(EDStatic.startBodyHtml(loggedInAs) + "\n");
        //writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)));
        //writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better

        //*** html body content
        writer.write(EDStatic.youAreHere(loggedInAs, "sos", datasetID)); //sos must be lowercase for link to work
        writeHtmlDatasetInfo(loggedInAs, writer, true, true, true, "", "");

        String makeAGraphRef = "<a href=\"" + tErddapUrl + "/griddap/" + datasetID + ".graph\">" +
            EDStatic.mag + "</a>";
        String datasetListRef = 
            "<p>See the\n" +
            "  <a href=\"" + tErddapUrl + "/sos/index.html\">list \n" +
            "    of datasets available via SOS</a> at this ERDDAP installation.\n";
        String makeAGraphListRef =
            "  <br>See the\n" +
            "    <a href=\"" + tErddapUrl + "/info/index.html" +
                "?page=1&itemsPerPage=" + EDStatic.defaultItemsPerPage + "\">list \n" +
            "      of datasets with Make A Graph</a> at this ERDDAP installation.\n";

        //*** What is SOS?   (for tDatasetID) 
        //!!!see the almost identical documentation above
        writer.write(
            "<h2><a name=\"description\">What</a> is SOS?</h2>\n" +
            EDStatic.sosLongDescriptionHtml + "\n" +
            datasetListRef + "\n" +
            "\n");


        //*** client software
        writer.write(
            "<h2><a name=\"clientSoftware\">Client Software</a></h2>" +
            "It is our impression that SOS is not intended to be used directly by humans using a browser.\n" +
            "<br>Instead, SOS seems to be intended for computer-to-computer transfer of data using SOS client\n" +
            "<br>software or SOS software libraries.\n" +
            "<ul>\n" +
            "<li>If you have SOS client software, give it this dataset's SOS server url\n" +
            "  <br><a href=\"" + sosUrl + "\">" + sosUrl + "</a>\n" +
            "  <br>and the client software will try to create the URLs and process the results for you.\n" + 
            "<li>Unfortunately, we are not yet aware of any SOS client software or libraries that can get data\n" +
            "  <br>from IOOS SOS servers (like ERDDAP's SOS server), other than ERDDAP(!) (which makes the data\n" +
            "  <br>available via OPeNDAP). \n" +
            "<li>There may be clients for\n" +
            "  <a href=\"http://www.oostethys.org/\">OOSTethys</a>-style SOS servers.\n" +
            "<li>Different SOS servers work slightly differently so it would be hard to write client software\n" +
            "  <br>that works with different types of SOS servers without customizing it for each server type.\n" +
            "<li>So the current recommendation seems to be that one should write a custom computer program\n" +
            "  <br>or script to format the requests and to parse and process the XML output from a given server.\n" +
            "  <br>Some of the information you need to write such software is below.\n" +
            "<li>For additional information, please see the\n" +
            "  <a rel=\"help\" href=\"http://www.opengeospatial.org/standards/sos\">OGC's SOS Specification</a>.\n" +
            "</ul>\n" +
            "\n");

        //*** sample SOS requests
        writer.write(
            "<h2>Sample SOS Requests for this Dataset and Details from the \n" +
            "<a rel=\"help\" href=\"http://www.opengeospatial.org/standards/sos\">SOS Specification</a></h2>\n" +
            "SOS requests are specially formed URLs with queries.\n" +
            "<br>You can create these URLs yourself or write software to do it for you.\n" +
            "<br>There are three types of SOS requests:\n" +
            "  <a rel=\"help\" href=\"#GetCapabilities\">GetCapabilities</a>,\n" +
            "  <a rel=\"help\" href=\"#DescribeSensor\">DescribeSensor</a>,\n" +
            "  <a rel=\"help\" href=\"#GetObservation\">GetObservation</a>.\n" + 
            "\n");


        //*** GetCapabilities
        writer.write(
            "<p><a name=\"GetCapabilities\"><b>GetCapabilities</b></a>\n" +
            " - A GetCapabilities request returns an XML document which provides\n" +
            "  <br>background information about the service and specific information about all of the data\n" +
            "  <br>available from this service.  For this dataset, use\n" + 
            "  <br><a href=\"" + getCapabilitiesHtml + "\">\n" + 
                getCapabilitiesHtml + "</a>\n" +
            "  <br>The GetCapabilities document refers to the " +
               "<a href=\"" + XML.encodeAsHTMLAttribute(dictionaryUrl) + "\">" + 
                  XML.encodeAsHTML(sosPhenomenaDictionaryUrl) + "</a> for this dataset.\n" +
            "\n");
            
        //getCapabilities specification
        writer.write(
            "<p>The parameters for a GetCapabilities request are:\n" +
            "<table class=\"erd commonBGColor\" cellspacing=\"4\">\n" +
            "  <tr>\n" +
            "    <th nowrap><i>name=value</i><sup>*</sup></th>\n" +
            "    <th>Description</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>service=SOS</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>version=" + sosVersion + "</td>\n" +
            "    <td>The only valid value is " + sosVersion + " . This parameter is optional.\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>request=GetCapabilities</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "</table>\n" +
            "<sup>*</sup> Parameter names are case-insensitive.\n" +
            "<br>Parameter values are case sensitive and must be\n" +
            "  <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>,\n" +
            "    which your browser normally handles for you.\n" +
            "<br>The parameters may be in any order in the URL.\n" +
            "\n");


        //*** DescribeSensor
        writer.write(
            "<p><a name=\"DescribeSensor\"><b>DescribeSensor</b></a>\n" +
            "  - A DescribeSensor request returns an XML document which provides\n" +
            "  <br>more detailed information about a specific sensor. For example,\n" + 
            "  <br><a href=\"" + describeSensorHtml + "\">\n" + 
                describeSensorHtml + "</a>\n" +
            "\n");

        //start of DescribeSensor Specification
        // &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.1%22
        // &sensorID=urn:ioos:sensor:noaa.nws.ndbc:41012:
        writer.write(
            "<p>The parameters for a DescribeSensor request are:\n" +
            "<table class=\"erd commonBGColor\" cellspacing=\"4\">\n" +
            "  <tr>\n" +
            "    <th nowrap><i>name=value</i><sup>*</sup></th>\n" +
            "    <th>Description</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>service=SOS</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>version=" + sosVersion + "</td>\n" +
            "    <td>The only valid value is " + sosVersion + " .  Required.\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>request=DescribeSensor</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>procedure=<i>sensorID</i></td>\n" +
            "    <td>See the sensorID's listed in the GetCapabilities response.\n" +
            "      <br>ERDDAP allows the long or the short form (the last part) of the sensorID.\n" +
            "      Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>outputFormat=\n" + 
            "      <br>&nbsp;&nbsp;" + 
                SSR.minimalPercentEncode(sosDSOutputFormat) + "</td>\n" +
            "    <td>The only valid value (in\n" +
            "      <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>\n" +
            "      form) is \"" + 
                SSR.minimalPercentEncode(sosDSOutputFormat) + "\".\n" +
            "      Required.\n" +
            "  </tr>\n" +
            "</table>\n" +
            "<sup>*</sup> Parameter names are case-insensitive.\n" +
            "<br>Parameter values are case sensitive and must be\n" +
            "  <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>,\n" +
            "    which your browser normally handles for you.\n" +
            "<br>The parameters may be in any order in the URL.\n" +
            "\n");


        //*** GetObservation
        writer.write(
            "<p><a name=\"GetObservation\"><b>GetObservation</b></a>\n" +
            "  - A GetObservation request specifies the subset of data that you want:\n" +
            "<ul>\n" +
            "  <li>The offering: the datasetID (for this dataset <tt>" + datasetID + "</tt>) (for all stations) or one specific station\n" +
            "    <br>(e.g., <tt>" + XML.encodeAsHTML(SSR.minimalPercentEncode(offeringi)) + "</tt>)\n" +
            "  <li>The observedProperty:\n" +
            "    <br>" + observedPropertyOptions + "\n" +
            "  <li>The time range\n" +
            "  <li>(Optional) the bounding box (BBOX:<i>minLon,minLat,maxLon,maxLat</i>)\n" +
            "  <li>The response type (e.g., text/xml;schema=\"ioos/0.6.1\" or text/csv)\n" +
            "</ul>\n" +
            "The SOS service responds with a file with the requested data. An example which returns a SOS XML file is" +
            "<br><a href=\"" + getObservationHtml + "\">\n" + 
                 getObservationHtml + "</a>\n" +
            "<br>(This example was computer-generated, so it may not work well.)\n" +
            "\n" +

            //responseFormat 
            "<p><a name=\"responseFormat\">responseFormat</a>" +
            "  - By changing the responseFormat, you can get the results in a different file formats.\n" +
            "  <br>The data file formats are:");
        for (int f = 0; f < sosDataResponseFormats.length; f++)   
            writer.write((f == 0? "\n" : ",\n") +
                (f % 4 == 2? "<br>" : "") +
                "<a href=\"" + 
                XML.encodeAsHTMLAttribute(getObs + SSR.minimalPercentEncode(sosDataResponseFormats[f])) + 
                "\">" +
                XML.encodeAsHTML(sosDataResponseFormats[f]) + "</a>");
        writer.write(".\n");
        writer.write(
            "<br>The image file formats are:\n" +
            "<ul>\n");

        //image: kml
        writer.write(
            "<li><a name=\"kml\"><b>Google Earth .kml</b></a>" +
            "  - If <tt>responseFormat=" + XML.encodeAsHTML(sosImageResponseFormats[0]) + "</tt>,\n" +
            "<br>the response will be a .kml file with all of the data (regardless of <tt>observedProperty</tt>)\n" +
            "<br>for the latest time available (within the time range you specify).\n");
        if (fdv < 0) {
            writer.write(
            "  <br>Sorry, there are no examples for this dataset.\n");
        } else {
            writer.write(
            "  <br>For example:\n" +
            "  <a href=\"" + 
                XML.encodeAsHTMLAttribute(getImageObsAllStations1Var + 
                    SSR.minimalPercentEncode(sosImageResponseFormats[0])) + 
                "\">" +
                XML.encodeAsHTML(sosImageResponseFormats[0]) + "</a>");
            writer.write(".\n" +
            "  <br>(For many datasets, requesting data for all stations will time out,\n" +
            "  <br>or fail for some other reason.)\n");
        }

        //image: all stations, 1 var
        writer.write(
            "<li><a name=\"map\"><b>Map</b></a>" +
            "  - If the request's <tt>offering</tt> is " + datasetID + " (all of the stations)\n" +
            "  <br>and the <tt>observedProperty</tt> is one simple phenomenon,\n" +
            "  <br>you will get a map showing the latest data (within the time range you request).\n");
        if (fdv < 0) {
            writer.write(
            "  <br>Sorry, there are no examples for this dataset.\n");
        } else {
            writer.write(
        "  <br>For example:");
            for (int f = 1; f < sosImageResponseFormats.length; f++)   //skip 0=kml
                writer.write((f == 1? "\n" : ",\n") +
                    "<a href=\"" + 
                    XML.encodeAsHTMLAttribute(getImageObsAllStations1Var + 
                        SSR.minimalPercentEncode(sosImageResponseFormats[f])) + 
                    "\">" +
                    XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
            writer.write(".\n" +
            "  <br>(For many datasets, requesting data for all stations will time out,\n" +
            "  <br>or fail for some other reason.)\n");
        }

        //image: all stations, 2 var: vectors
        writer.write(
            "<li><a name=\"vectors\"><b>Map with Vectors</b></a>" +
            "  - If the request's <tt>offering</tt> is " + datasetID + " (all of the stations),\n" +
            "  <br>the <tt>observedProperty</tt> lists two simple phenomenon with the same units\n" +
            "  <br>(hopefully representing the x and y components of a vector), \n" +
            "  <br>and the <tt>eventTime</tt> is a single time, you will get a map with vectors.\n" +
            "  <br>(If the <tt>eventTime</tt> is a time range, you will get multiple vectors for each station,\n" +
            "  <br>which is a mess).\n");
        if (dvu < 0)
            writer.write(
            "  <br>Sorry, there are no examples for this dataset.\n");
        else {
            writer.write(
            "  <br>For example:");
            for (int f = 1; f < sosImageResponseFormats.length; f++)   //skip 0=kml
                writer.write((f == 1? "\n" : ",\n") +
                    "<a href=\"" + 
                    XML.encodeAsHTMLAttribute(getImageObsAllStationsVector + 
                        SSR.minimalPercentEncode(sosImageResponseFormats[f])) + 
                    "\">" +
                    XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
            writer.write(".\n" +
            "  <br>(For many datasets, requesting data for all stations will timeout,\n" +
            "  <br>or fail for some other reason.)\n");
        }

        //image: 1 station, 1 var
        writer.write(
            "<li><a name=\"timeSeries\"><b>Time Series</b></a>" +
            "  - If the request's <tt>offering</tt> is a specific station\n" +
            "  <br>and the <tt>observedProperty</tt> is one simple phenomenon,\n" +
            "  <br>you will get a time series graph (with data from the time range you request).\n");
        if (fdv < 0) {
            writer.write(
            "  <br>Sorry, there are no examples for this dataset.\n");
        } else {
            writer.write(
            "  <br>For example:");
            for (int f = 1; f < sosImageResponseFormats.length; f++)   //skip 0=kml
                writer.write((f == 1? "\n" : ",\n") +
                    "<a href=\"" + 
                    XML.encodeAsHTMLAttribute(getImageObs1Station1Var + 
                        SSR.minimalPercentEncode(sosImageResponseFormats[f])) + 
                    "\">" +
                    XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
            writer.write(".\n");
        }

        //image: 1 station, scatterplot
        writer.write(
            "<li><a name=\"scatterplot\"><b>Scatter Plot / Phase Plot</b></a>" +
            "  - If the request's <tt>offering</tt> is a specific station\n" +
            "  <br>and the <tt>observedProperty</tt> lists two simple phenomenon with the different units,\n" +
            "  <br>you will get a scatter plot (with data from the time range you request).\n");
        if (dvu < 0)
            writer.write(
            "  <br>Sorry, there are no examples for this dataset.\n");
        else {
            writer.write(
            "  <br>For example:");
            for (int f = 1; f < sosImageResponseFormats.length; f++)   //skip 0=kml
                writer.write((f == 1? "\n" : ",\n") +
                    "<a href=\"" + 
                    XML.encodeAsHTMLAttribute(getImageObs1Station2Var + 
                        SSR.minimalPercentEncode(sosImageResponseFormats[f])) + 
                    "\">" +
                    XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
            writer.write(".\n");
        }

        //image: 1 station, 2 var - sticks
        writer.write(
            "<li><a name=\"sticks\"><b>Sticks</b></a>" +
            "  - If the request's <tt>offering</tt> is a specific station\n" +
            "  <br>and the <tt>observedProperty</tt> lists two simple phenomenon with the same units\n" +
            "  <br>(representing the x and y components of a vector),\n" +
            "  <br>you will get a sticks graph (with data from the time range you request).\n" +
            "  <br>Each stick is a vector associated with one time point.\n");
        if (dvu < 0)
            writer.write(
            "  <br>Sorry, there are no examples for this dataset.\n");
        else {
            writer.write(
            "  <br>For example:");
            for (int f = 1; f < sosImageResponseFormats.length; f++)   //skip 0=kml
                writer.write((f == 1? "\n" : ",\n") +
                    "<a href=\"" + 
                    XML.encodeAsHTMLAttribute(getImageObs1StationVector + 
                        SSR.minimalPercentEncode(sosImageResponseFormats[f])) + 
                    "\">" +
                    XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
            writer.write(".\n");
        }

        writer.write(
            "</ul>\n" +
            "(These examples were computer-generated, so they may not work well.)\n" +
            "\n");

        /*
        //responseMode=out-of-band
        writer.write(
            "<p><a name=\"outOfBand\">responseMode=out-of-band</a>" +
            "  - By changing the responseFormat and setting responseMode=out-of-band,\n" +
            "  <br>you can get a SOS XML file with a <b>link</b> to the results in a different file format.\n" +
            "  <br>[For now, there is not much point to using out-of-band since you might as will get the response directly (inline).]\n" +
            "  <br>The data file formats are:");
        for (int f = 2; f < sosDataResponseFormats.length; f++)   //first 2 are standard SOS xml formats
            writer.write((f <= 2? "\n" : ",\n") +
                "<a href=\"" + 
                XML.encodeAsHTMLAttribute(getObs + SSR.minimalPercentEncode(sosDataResponseFormats[f]) + 
                    "&responseMode=out-of-band") + 
                "\">" +
                XML.encodeAsHTML(sosDataResponseFormats[f]) + "</a>");
        writer.write(".\n");
        if (fdv >= 0) {
            writer.write(
            "<br>The image file formats are:");
            for (int f = 0; f < sosImageResponseFormats.length; f++)   
                writer.write((f == 0? "\n" : ",\n") +
                    "<a href=\"" + 
                    XML.encodeAsHTMLAttribute(getImageObs1Station1Var + 
                        SSR.minimalPercentEncode(sosImageResponseFormats[f]) + 
                        "&responseMode=out-of-band") + 
                    "\">" +
                    XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
            writer.write(".\n");
        }
        writer.write(
            "<br>(These examples were computer-generated, so they may not work well.)\n");
        */

        //featureOfInterest=BBOX
        writer.write(
            "<p><a name=\"bbox\">featureOfInterest=BBOX</a>" +
            "  - To get data from all stations within a rectangular bounding box,\n" +
            "  <br>you can request data for the network offering (all stations) and include a\n" +
            "  <br>minLon,minLat,maxLon,maxLat bounding box constraint in the request.\n" +
            "  <br>An example which returns a SOS XML file is" +
            "  <br><a href=\"" + getNetworkObservationHtml + "\">\n" + 
                 getNetworkObservationHtml + "</a>\n" +
            "  <br>(This example was computer-generated, so it may not work well.)\n" +
            "  <br>For remote datasets, this example may respond slowly or fail because it takes too long.\n" + 
            "\n");

            
        //start of GetObservation Specification
        writer.write(
            "<p>The parameters for a GetObservation request are:\n" +
            "<table class=\"erd commonBGColor\" cellspacing=\"4\">\n" +
            "  <tr>\n" +
            "    <th nowrap><i>name=value</i><sup>*</sup></th>\n" +
            "    <th>Description</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>service=SOS</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>version=" + sosVersion + "</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>request=GetObservation</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
        //"service=SOS&version=1.0.0&request=GetObservation" +
        //"&offering=urn:ioos:station:1.0.0.127.cwwcNDBCMet:41004:" +
        //"&observedProperty=cwwcNDBCMet" +
        //"&responseFormat=text/xml;schema=%22ioos/0.6.1%22" +
        //"&eventTime=2008-08-01T00:00:00Z/2008-08-01T04:00:00Z";
            "  <tr>\n" +
            "    <td nowrap>srsName=<i>srsName</i></td>\n" +
            "    <td>This is required by the SOS standard, but optional in ERDDAP.\n" +
            "      <br>In ERDDAP, if it is present, it must be \"urn:ogc:def:crs:epsg::4326\", the only supported SRS.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>offering=<i>offering</i></td>\n" +
            "    <td>The name of an <tt>observationOffering</tt> from GetCapabilities.\n" +
            "       <br>ERDDAP allows either the long or the short form (the datasetID or the\n" +
             "      station name at the end) of the offering name.\n" +
            "       <br>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>eventTime=<i>beginTime</i>\n" +
            "    <br>eventTime=<i>beginTime/endTime</i></td>\n" +
            "    <td>The beginTime and endTime must be in\n" +
            "      <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" format</a>,\n" +
            "      for example, <span style=\"white-space: nowrap;\">\"1985-01-02T00:00:00Z\").</span>\n" +
            "      <br>This parameter is optional for the SOS standard and ERDDAP.\n" +
            "      <br>For IOOS SOS, the default is the last value.\n" +
            "      <br>For ERDDAP, the default is the last value (but perhaps nothing if there is no data for the last week).\n" +
            "      <br>If offering=" + datasetID + ", the default returns data from all of the stations which have data for the most recent time.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>procedure=<i>procedure</i></td>\n" +
            "    <td>The SOS standard allows 0 or more comma-separated procedures.  ERDDAP currently forbids this.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>observedProperty=<i>observedProperty</i></td>\n" +
            "    <td>The SOS standard requires 1 or more comma-separated observedProperties.\n" + 
            "      <br>ERDDAP allows the long form\n" +
            "      <br>(e.g., " + tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl + "#" + datasetID + ")\n" +
            "      <br>or the short form (e.g., " + datasetID + ") of the observedProperty.\n" +
            "      <br>ERDDAP allows 0 or more comma-separated <tt>observedProperty</tt>'s from GetCapabilities (default=" + datasetID + ").\n" +
            "      <br>In ERDDAP, the options are:\n" +
            "      <br>" + observedPropertyOptions + ".\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>featureOfInterest=\n" +
            "      <br>&nbsp;&nbsp;BBOX:<i>minLon,minLat,maxLon,maxLat</i></td>\n" +
            "    <td>BBOX allows you to specify a longitude,latitude bounding box constraint.\n" +
            "      <br>This parameter is optional for the SOS standard.\n" +
            "      <br>This parameter is optional in ERDDAP and only really useful for the \"network\" offering.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>result=<i>result</i></td>\n" +
            "    <td>This is optional in the SOS standard and currently forbidden in ERDDAP.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>responseFormat=<i>mimeType</i></td>\n" +
            "    <td>In ERDDAP, this can be any one of several response formats (in\n" +
            "      <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>\n" +
            "      form,\n" +
                "  <br>which your browser normally handles for you).\n" +
            "      <br>The data responseFormats are:<br>");
        charsWritten = 0;
        for (int f = 0; f < sosDataResponseFormats.length; f++) {
            if (f > 0) writer.write(", ");
            String ts = "\"" + SSR.minimalPercentEncode(sosDataResponseFormats[f]) + "\"";
            if (charsWritten + ts.length() > 80) {
                writer.write("\n      <br>"); 
                charsWritten = 0; 
            }
            writer.write(ts);
            charsWritten += ts.length();
        }
        writer.write(
            "      <br>(The first two data responseFormat options are synonyms for the IOOS SOS XML format.)\n" +
            "      <br>The image responseFormats are: ");
        for (int f = 0; f < sosImageResponseFormats.length; f++) {
            if (f > 0) writer.write(", ");
            writer.write("\"" + SSR.minimalPercentEncode(sosImageResponseFormats[f]) + "\"");
        }
        writer.write(
            ".\n" +
            "      <br>If you specify an image responseFormat, you must specify simple observedProperties\n" +
            "        (usually 1 or 2).\n" +
            "      <br>See the <a href=\"#responseFormat\">examples</a> above.\n" +
            "      <br>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>resultModel=<i>resultModel</i></td>\n" +
            "    <td>This is optional in the SOS standard and ERDDAP.\n" +
            "      <br>If present, it must be \"om:Observation\" (the default).</td>\n" +
            "  </tr>\n" +
            //"  <tr>\n" +
            //"    <td nowrap>responseMode=<i>responseMode</i></td>\n" +
            //"    <td>This is optional in the SOS standard and ERDDAP (default=\"inline\").\n" + 
            //"      <br>Use \"inline\" to get the responseFormat directly.\n" +
            //"      <br>Use \"out-of-band\" to get a short SOS XML response with a link to the responseFormat response.</td>\n" +
            //"  </tr>\n" +
            "</table>\n" +
            "<sup>*</sup> Parameter names are case-insensitive.\n" +
            "<br>Parameter values are case sensitive and must be\n" +
            "  <a rel=\"help\" href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>,\n" +
            "  which your browser normally handles for you.\n" +
            "<br>The parameters may be in any order in the URL.\n" +
            "\n");

        

        //if (EDStatic.displayDiagnosticInfo) 
        //    EDStatic.writeDiagnosticInfoHtml(writer);
        //writer.write(EDStatic.endBodyHtml(tErddapUrl));
        //writer.write("\n</html>\n");
        writer.flush(); 
    }


    /**
     * This writes the Google Visualization Query Language documentation .html page.
     */
    /*
       Google defined the services that a Google Data Source (GDS) needs to provide.
       It is a sophisticated, SQL-like query system.
       Unfortunately, the OPeNDAP query system that ERDDAP supports is not as 
       sophisticated as the GDS system. 
       Similarly, none of the remote data sources (with the exception of databases)
       have a query system as sophisticated as GDS.
       So ERDDAP can't process a GDS request by simply converting it into an OPeNDAP request,
       or by passing the GDS request to the remote server.

       Google recognized that it would be hard for people to create servers
       that could handle the GDS requests.
       So Google kindly released Java code to simplify creating a GDS server.
       It works by having the server program (e.g., ERDDAP) feed in the entire 
       dataset in order to process each request.
       It then does all of the hard work to filter and process the request.
       This works great for the vast majority of datasets on the web,
       where the dataset is small (a few hundred rows) and can be accessed quickly (i.e., is stored locally).
       Unfortunately, most datasets in ERDDAP are large (10's or 100's of thousands of rows)
       and are accessed slowly (because they are remote).
       Downloading the entire dataset just to process one request just isn't feasible.
       
       For now, ERDDAP's compromise solution is to just accept a subset of 
       the Google Query Language commands. 
       This way, ERDDAP can pass most of the constraints on to the remote data source
       (as it always has) and let the remote data source do most of the subset selection.
       Then ERDDAP can do the final subset selection and processing and 
       return the response data table to the client.
       This is not ideal, but there has to be some compromise to allow ERDDAP's
       large, remote datasets (accessible via the OPeNDAP query system) 
       to be accessible as Google Data Sources.
        
    */

    /**
     * This extracts a CSV list of variable/column names (gVis style)
     * 
     * @param start is the index in gVisQuery of white space or the first letter
     *    of the first name
     * @param names receives the list of variable/column names.
     *     It must be empty initially.
     * @return the new value of start (or -1 if names is an error message)
     */
    private int getGVisColumnNames(String gVisQuery, int start, StringBuilder names) {
        names.setLength(0);
        int gVisQueryLength = gVisQuery.length();
        do {
            if (names.length() > 0) {
                //eat the required ',' (see matching 'while' at bottom) 
                start++;

                //add , to names
                names.append(',');
            }

            //eat any spaces
            while (gVisQuery.startsWith(" ", start)) 
                start++;

            String name;
            if (gVisQuery.startsWith("'", start)) {
                //'colName'
                int po = gVisQuery.indexOf("'", start + 1);
                if (po < 0) {
                    names.setLength(0);
                    names.append(EDStatic.queryError + "missing \"'\" after position=" + start);
                    return -1;
                }
                name = gVisQuery.substring(start + 1, po);
                start = po + 1;                        
            } else {
                //unquoted colName
                int po = start + 1;
                while (po < gVisQueryLength &&
                       !gVisQuery.startsWith(",", po) &&  
                       !gVisQuery.startsWith(" ", po))
                    po++;
                name = gVisQuery.substring(start, po);
                start = po;
            }
            //ensure the name is valid
            if (String2.indexOf(dataVariableDestinationNames, name) < 0) {
                names.setLength(0);
                names.append(EDStatic.queryError + "unrecognized column name=\""+ name + 
                    "\" at position=" + (start - name.length()));
                return -1;
            }
            names.append(name);
            while (gVisQuery.startsWith(" ", start)) 
                start++;

        } while (gVisQuery.startsWith(",", start));
        return start;
    }

    /**
     * This generates an OPeNDAP query from a Google Visualization API Query Language string.
     * <br>See Query Language reference http://code.google.com/apis/visualization/documentation/querylanguage.html
     *
     * @param tgVisQuery  the case of the key words (e.g., SELECT) is irrelevant
     * @return dapQuery
     * @throws Throwable if trouble
     *  ???or if error, return gVis-style error message string?
     */
    public String gVisQueryToDapQuery(String gVisQuery) {
        //SELECT [DISTINCT] csvRequestColumns [WHERE constraint [AND constraint]] [ORDER BY csvOrderBy] 
        //csvRequestColumns[&constraint][&distinct()][&orderBy csvOrderBy
        int gVisQueryLength = gVisQuery.length();
        String gVisQueryLC = gVisQuery.toLowerCase();
        StringBuilder dapQuery = new StringBuilder();

        //SELECT
        int start = 0;
        if (gVisQueryLC.startsWith("select ", start)) {
            start += 7;
            while (gVisQueryLC.startsWith(" ", start)) 
                start++;
            if (gVisQueryLC.startsWith("*", start)) {
                start++;
                while (gVisQueryLC.startsWith(" ", start)) 
                    start++;
            } else {
                do {
                    if (dapQuery.length() > 0) {
                        //eat the required ',' and optional white space
                        start++;
                        while (gVisQueryLC.startsWith(" ", start)) 
                            start++;
                        //add , to dapQuery
                        dapQuery.append(',');
                    }
                    String name;
                    if (gVisQueryLC.startsWith("'", start)) {
                        //'colName'
                        int po = gVisQueryLC.indexOf("'", start + 1);
                        if (po < 0)
                            return EDStatic.queryError + "missing \"'\" after position=" + start;
                        name = gVisQuery.substring(start + 1, po);
                        start = po + 1;                        
                    } else {
                        //unquoted colName
                        int po = start + 1;
                        while (po < gVisQueryLength &&
                               !gVisQueryLC.startsWith(",", po) &&  
                               !gVisQueryLC.startsWith(" ", po))
                            po++;
                        name = gVisQuery.substring(start, po);
                        start = po;
                    }
                    if (String2.indexOf(dataVariableDestinationNames, name) < 0)
                        return EDStatic.queryError + "unrecognized column name=\""+ name + 
                            "\" at position=" + (start - name.length());
                    dapQuery.append(name);
                    while (gVisQueryLC.startsWith(" ", start)) 
                        start++;

                } while (gVisQueryLC.startsWith(",", start));
            }
        }

        //WHERE
        if (gVisQueryLC.startsWith("where ", start)) {
        }

        //ORDER BY
        if (gVisQueryLC.startsWith("order by ", start)) {
        }

        return dapQuery.toString();
    }



    /** 
     * This writes the dataset's FGDC-STD-012-2002
     * "Content Standard for Digital Geospatial Metadata" 
     * (unlike EDDGrid, not "Extensions for Remote Sensing Metadata")
     * XML to the writer.
     * <br>I started with EDDGrid version, but modified using FGDC example from OBIS.
     * 
     * <p>This is usually just called by the dataset's constructor, 
     * at the end of EDDTable/Grid.ensureValid.
     * 
     * <p>See FGDC documentation at
     * http://www.fgdc.gov/standards/projects/FGDC-standards-projects/metadata/base-metadata/v2_0698.pdf
     * Bob has local copy at f:/programs/fgdc/fgdc-std-001-1998-v2_0698.pdf
     * <br>For missing String values, use "Unknown" (pg viii).
     *
     * <p>The <b>most</b> useful descriptive information 
     * (but somewhat cumbersome to navigate; use Back button, don't hunt for their back links):
     * http://www.fgdc.gov/csdgmgraphical/index.htm
     *
     * <p>FGDC Metadata validator
     * http://geolibportal.usm.maine.edu/geolib/fgdc_metadata_validator.html
     * 
     * <p>If getAccessibleTo()==null, the fgdc refers to http: ERDDAP links; otherwise,
     * it refers to https: ERDDAP links.
     *
     * @param writer a UTF-8 writer
     * @throws Throwable if trouble (e.g., no latitude and longitude variables)
     */
    protected void writeFGDC(Writer writer) throws Throwable {
        //future: support datasets with x,y (and not longitude,latitude)

        //requirements
        if (lonIndex < 0 || latIndex < 0) 
            throw new SimpleException(EDStatic.noXxxNoLL);

        String tErddapUrl = EDStatic.erddapUrl(getAccessibleTo() == null? null : "anyone");
        String datasetUrl = tErddapUrl + "/" + dapProtocol + "/" + datasetID();
        String sosUrl     = tErddapUrl + "/sos/" + datasetID() + "/" + sosServer; // "?" at end?
        String wmsUrl     = tErddapUrl + "/wms/" + datasetID() + "/" + WMS_SERVER; // "?" at end?
        String domain = EDStatic.baseUrl;
        if (domain.startsWith("http://"))
            domain = domain.substring(7);
        String eddCreationDate = String2.replaceAll(
            Calendar2.millisToIsoZuluString(creationTimeMillis()).substring(0, 10), "-", "");
        String unknown = "Unknown"; //pg viii of FGDC document

        String acknowledgement = combinedGlobalAttributes.getString("acknowledgement");
        String cdmDataType     = combinedGlobalAttributes.getString("cdm_data_type");
        String contributorName = combinedGlobalAttributes.getString("contributor_name");
        String contributorEmail= combinedGlobalAttributes.getString("contributor_email");
        String contributorRole = combinedGlobalAttributes.getString("contributor_role");
        String creatorName     = combinedGlobalAttributes.getString("creator_name");
        String creatorEmail    = combinedGlobalAttributes.getString("creator_email");
        //creatorUrl: use infoUrl
        String dateCreated     = combinedGlobalAttributes.getString("date_created");
        String dateIssued      = combinedGlobalAttributes.getString("date_issued");
        //make compact form  YYYYMMDD
        if (dateCreated != null && dateCreated.length() >= 10) 
            dateCreated = String2.replaceAll(dateCreated.substring(0, 10), "-", ""); 
        if (dateIssued  != null && dateIssued.length()  >= 10)
            dateIssued = String2.replaceAll(dateIssued.substring(0, 10), "-", ""); 
        String history         = combinedGlobalAttributes.getString("history");
        String infoUrl         = combinedGlobalAttributes.getString("infoUrl"); 
        String institution     = combinedGlobalAttributes.getString("institution");
        String keywords        = combinedGlobalAttributes.getString("keywords");
        String keywordsVocabulary = combinedGlobalAttributes.getString("keywords_vocabulary");
        if (keywords == null) { //use the crude, ERDDAP keywords
            keywords           = EDStatic.keywords;
            keywordsVocabulary = null;
        }
        String license         = combinedGlobalAttributes.getString("license");
        String project         = combinedGlobalAttributes.getString("project");
        if (project == null) 
            project = institution;
        String references      = combinedGlobalAttributes.getString("references");
        String satellite       = combinedGlobalAttributes.getString("satellite");
        String sensor          = combinedGlobalAttributes.getString("sensor");
        String sourceUrl       = publicSourceUrl();
        String standardNameVocabulary = combinedGlobalAttributes.getString("standard_name_vocabulary");

        String adminInstitution    = EDStatic.adminInstitution    == null? unknown : EDStatic.adminInstitution;
        String adminIndividualName = EDStatic.adminIndividualName == null? unknown : EDStatic.adminIndividualName;
        String adminPosition       = EDStatic.adminPosition       == null? unknown : EDStatic.adminPosition;
        String adminPhone          = EDStatic.adminPhone          == null? unknown : EDStatic.adminPhone;
        String adminAddress        = EDStatic.adminAddress        == null? unknown : EDStatic.adminAddress;
        String adminCity           = EDStatic.adminCity           == null? unknown : EDStatic.adminCity;
        String adminStateOrProvince= EDStatic.adminStateOrProvince== null? unknown : EDStatic.adminStateOrProvince;
        String adminPostalCode     = EDStatic.adminPostalCode     == null? unknown : EDStatic.adminPostalCode;
        String adminCountry        = EDStatic.adminCountry        == null? unknown : EDStatic.adminCountry;
        String adminEmail          = EDStatic.adminEmail          == null? unknown : EDStatic.adminEmail;

        //testMinimalMetadata is useful for Bob doing tests of validity of FGDC results 
        //  when a dataset has minimal metadata
        boolean testMinimalMetadata = false; //only true when testing. normally false;
        if (acknowledgement == null || testMinimalMetadata) acknowledgement = unknown;
        if (cdmDataType     == null || testMinimalMetadata) cdmDataType     = CDM_OTHER;
        if (contributorName == null || testMinimalMetadata) contributorName = unknown;
        if (contributorEmail== null || testMinimalMetadata) contributorEmail= unknown;
        if (contributorRole == null || testMinimalMetadata) contributorRole = unknown;
        if (creatorName     == null || testMinimalMetadata) creatorName     = unknown;
        if (creatorEmail    == null || testMinimalMetadata) creatorEmail    = unknown;
        if (dateCreated     == null || testMinimalMetadata) dateCreated     = unknown;
        if (dateIssued      == null || testMinimalMetadata) dateIssued      = unknown;
        if (history         == null || testMinimalMetadata) history         = unknown;
        if (infoUrl         == null || testMinimalMetadata) infoUrl         = unknown;
        if (institution     == null || testMinimalMetadata) institution     = unknown;
        if (keywords        == null || testMinimalMetadata) keywords        = unknown;
        if (keywordsVocabulary == null || testMinimalMetadata) keywordsVocabulary = unknown;
        if (license         == null || testMinimalMetadata) license         = unknown;
        if (project         == null || testMinimalMetadata) project         = unknown;
        if (references      == null || testMinimalMetadata) references      = unknown;
        if (satellite       == null || testMinimalMetadata) satellite       = unknown;
        if (sensor          == null || testMinimalMetadata) sensor          = unknown;
        if (sourceUrl       == null || testMinimalMetadata) sourceUrl       = unknown;
        if (standardNameVocabulary == null || testMinimalMetadata) standardNameVocabulary = unknown;

        //notAvailable and ...Edv
        EDVLat latEdv = (EDVLat)dataVariables[latIndex];
        EDVLon lonEdv = (EDVLon)dataVariables[lonIndex];
        EDVAlt altEdv = (altIndex < 0 || testMinimalMetadata)? null :  
            (EDVAlt)dataVariables[altIndex];
        EDVDepth depthEdv = (depthIndex < 0 || testMinimalMetadata)? null :  
            (EDVDepth)dataVariables[depthIndex];
        EDVTime timeEdv = (timeIndex < 0 || testMinimalMetadata)? null :
            (EDVTime)dataVariables[timeIndex];

        //standardNames
        StringArray standardNames = new StringArray();
        if (!testMinimalMetadata) {
            for (int v = 0; v < dataVariables.length; v++) {
                String sn = dataVariables[v].combinedAttributes().getString("standard_name");
                if (sn != null)
                    standardNames.add(sn);
            }
        }

        //adminCntinfo
String adminCntinfo = 
"            <cntinfo>\n" +
"              <cntorgp>\n" +
"                <cntorg>" + XML.encodeAsXML(adminInstitution) + "</cntorg>\n" +
"                <cntper>" + XML.encodeAsXML(adminIndividualName) + "</cntper>\n" +
"              </cntorgp>\n" +
"              <cntpos>" + XML.encodeAsXML(adminPosition) + "</cntpos>\n" +
"              <cntaddr>\n" +
"                <addrtype>Mailing and Physical Address</addrtype>\n" +
"                <address>" + XML.encodeAsXML(adminAddress) + "</address>\n" +
"                <city>" + XML.encodeAsXML(adminCity) + "</city>\n" +
"                <state>" + XML.encodeAsXML(adminStateOrProvince) + "</state>\n" +
"                <postal>" + XML.encodeAsXML(adminPostalCode) + "</postal>\n" +
"                <country>" + XML.encodeAsXML(adminCountry) + "</country>\n" +
"              </cntaddr>\n" +
"              <cntvoice>" + XML.encodeAsXML(adminPhone) + "</cntvoice>\n" +
//"              <cntfax>" + unknown + "</cntfax>\n" +
"              <cntemail>" + XML.encodeAsXML(adminEmail) + "</cntemail>\n" +
//"              <hours>" + unknown + "</hours>\n" +
"            </cntinfo>\n";


//start writing xml
        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +   //or ISO-8859-1 charset???
"<metadata xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
"xsi:noNamespaceSchemaLocation=\"http://fgdcxml.sourceforge.net/schema/fgdc-std-012-2002/fgdc-std-012-2002.xsd\" " +
//http://lab.usgin.org/groups/etl-debug-blog/fgdc-xml-schema-woes 
//talks about using this
">\n" +
"  <idinfo>\n" +
//EDDGrid has ngdc's <datasetid> here
"    <citation>\n" +
"      <citeinfo>\n" +

//origin: from project, creator_email, creator_name, infoUrl, institution
"        <origin>\n" + XML.encodeAsXML(
            (project      == unknown? "" : "Project: "     + project      + "\n") + 
            (creatorName  == unknown? "" : "Name: "        + creatorName  + "\n") + 
            (creatorEmail == unknown? "" : "Email: "       + creatorEmail + "\n") +
                                           "Institution: " + institution  + "\n"  + //always known
            (infoUrl      == unknown? "" : "InfoURL: "     + infoUrl      + "\n") +
            (sourceUrl    == unknown? "" : "Source URL: "  + sourceUrl    + "\n")) +
"        </origin>\n" +
         //EDDGrid has ngdc's <origin_cntinfo> here
"        <pubdate>" + XML.encodeAsXML(dateIssued == unknown? eddCreationDate : dateIssued) + "</pubdate>\n" +
"        <title>" + XML.encodeAsXML(title) + "</title>\n" +
"        <edition>" + unknown + "</edition>\n" +
         //geoform vocabulary http://www.fgdc.gov/csdgmgraphical/ideninfo/citat/citinfo/type.htm 
"        <geoform>tabular digital data</geoform>\n" + 
//"        <serinfo>\n" +  ERDDAP doesn't have serial info
//"          <sername>NOAA Tsunami Inundation DEMs</sername>\n" +
//"          <issue>Adak, Alaska</issue>\n" +
//"        </serinfo>\n" +

//publisher is ERDDAP,  use admin... information
"        <pubinfo>\n" +
"          <pubplace>" + XML.encodeAsXML(adminCity + ", " + adminStateOrProvince + ", " + 
             adminCountry) + "</pubplace>\n" +
"          <publish>" + XML.encodeAsXML("ERDDAP, version " + EDStatic.erddapVersion + ", at " + 
               adminInstitution) + "</publish>\n" +
//EDDGrid has ngdc's <publish_cntinfo> here
"        </pubinfo>\n");

//online resources   .html, .graph, WMS
writer.write(
"        <onlink>" + XML.encodeAsXML(datasetUrl + ".html")  + "</onlink>\n" +
(accessibleViaMAG.length() > 0? "" :
"        <onlink>" + XML.encodeAsXML(datasetUrl + ".graph") + "</onlink>\n") +
(accessibleViaSubset.length() > 0? "" :
"        <onlink>" + XML.encodeAsXML(datasetUrl + ".subset") + "</onlink>\n") +
(accessibleViaSOS.length() > 0? "" : 
"        <onlink>" + XML.encodeAsXML(sosUrl) + "</onlink>\n") +
(accessibleViaWMS.length() > 0? "" : 
"        <onlink>" + XML.encodeAsXML(wmsUrl) + "</onlink>\n"));
//EDDGrid has  <CI_OnlineResource> here

//larger work citation: project
if (project != unknown)
    writer.write(
"        <lworkcit>\n" +  
"          <citeinfo>\n" +
"            <origin>" + XML.encodeAsXML(project) + "</origin>\n" +
//"            <pubdate>20081031</pubdate>\n" +
//"            <title>NOAA Tsunami Inundation Gridding Project</title>\n" +
//"            <geoform>Raster Digital Data</geoform>\n" +
//"            <serinfo>\n" +
//"              <sername>NOAA Tsunami Inundation DEMs</sername>\n" +
//"              <issue>Named by primary coastal city, state in DEM.</issue>\n" +
//"            </serinfo>\n" +
//"            <pubinfo>\n" +
//"              <pubplace>Boulder, Colorado</pubplace>\n" +
//"              <publish>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</publish>\n" +
//"            </pubinfo>\n" +
//"            <onlink>http://www.ngdc.noaa.gov/mgg/inundation/tsunami/inundation.html</onlink>\n" +
//"            <CI_OnlineResource>\n" +
//"              <linkage>http://www.ngdc.noaa.gov/mgg/inundation/tsunami/inundation.html</linkage>\n" +
//"              <name>NOAA Tsunami Inundation Gridding Project</name>\n" +
//"              <description>Project web page.</description>\n" +
//"              <function>information</function>\n" +
//"            </CI_OnlineResource>\n" +
//"            <CI_OnlineResource>\n" +
//"              <linkage>http://www.ngdc.noaa.gov/dem/squareCellGrid/map</linkage>\n" +
//"              <name>Map Interface</name>\n" +
//"              <description>Graphic geo-spatial search tool for locating completed and planned NOAA tsunami inundation DEMs.</description>\n" +
//"              <function>search</function>\n" +
//"            </CI_OnlineResource>\n" +
//"            <CI_OnlineResource>\n" +
//"              <linkage>http://www.ngdc.noaa.gov/dem/squareCellGrid/search</linkage>\n" +
//"              <name>DEM text search tool</name>\n" +
//"              <description>Text search tool for locating completed and planned NOAA tsunami inundation DEMs.</description>\n" +
//"              <function>search</function>\n" +
//"            </CI_OnlineResource>\n" +
"          </citeinfo>\n" +
"        </lworkcit>\n");

writer.write(
"      </citeinfo>\n" +
"    </citation>\n");

//description
writer.write(
"    <descript>\n" +
"      <abstract>" + XML.encodeAsXML(summary) + "</abstract>\n" +
"      <purpose>" + unknown + "</purpose>\n" +
"      <supplinf>" + XML.encodeAsXML(infoUrl) + "</supplinf>\n" + //OBIS uses

//??? ideally from "references", but it's a blob and they want components here
/*"      <documnts>\n" +  
"        <userguid>\n" +
"          <citeinfo>\n" +
"            <origin>Kelly S. Carignan</origin>\n" +
"            <origin>Lisa A. Taylor</origin>\n" +
"            <origin>Barry W. Eakins</origin>\n" +
"            <origin>Robin R. Warnken</origin>\n" +
"            <origin>Elliot Lim</origin>\n" +
"            <origin>Pamela R. Medley</origin>\n" +
"            <origin_cntinfo>\n" +
"              <cntinfo>\n" +
"                <cntorgp>\n" +
"                  <cntorg>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</cntorg>\n" +
"                  <cntper>Lisa A. Taylor</cntper>\n" +
"                </cntorgp>\n" +
"                <cntaddr>\n" +
"                  <addrtype>Mailing and Physical Address</addrtype>\n" +
"                  <address>NOAA/NESDIS/NGDC  325 Broadway</address>\n" +
"                  <city>Boulder</city>\n" +
"                  <state>CO</state>\n" +
"                  <postal>80305-3328</postal>\n" +
"                  <country>USA</country>\n" +
"                </cntaddr>\n" +
"                <cntvoice>(303) 497-6767</cntvoice>\n" +
"                <cnttdd>(303) 497-6958</cnttdd>\n" +
"                <cntfax>(303) 497-6513</cntfax>\n" +
"                <cntemail>Lisa.A.Taylor@noaa.gov</cntemail>\n" +
"                <hours>7:30 - 5:00 Mountain</hours>\n" +
"                <cntinst>Contact Data Center</cntinst>\n" +
"              </cntinfo>\n" +
"            </origin_cntinfo>\n" +
"            <pubdate>200904</pubdate>\n" +
"            <title>Digital Elevation Model of Adak, Alaska: Procedures, Data Sources, and Analysis</title>\n" +
"            <serinfo>\n" +
"              <sername>NOAA Technical Memorandum</sername>\n" +
"              <issue>NESDIS NGDC-31</issue>\n" +
"            </serinfo>\n" +
"            <pubinfo>\n" +
"              <pubplace>Boulder, CO</pubplace>\n" +
"              <publish>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</publish>\n" +
"              <publish_cntinfo>\n" +
"                <cntinfo>\n" +
"                  <cntorgp>\n" +
"                    <cntorg>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</cntorg>\n" +
"                    <cntper>User Services</cntper>\n" +
"                  </cntorgp>\n" +
"                  <cntaddr>\n" +
"                    <addrtype>Mailing and Physical Address</addrtype>\n" +
"                    <address>NOAA/NESDIS/NGDC E/GC 325 Broadway</address>\n" +
"                    <city>Boulder</city>\n" +
"                    <state>CO</state>\n" +
"                    <postal>80305-3328</postal>\n" +
"                    <country>USA</country>\n" +
"                  </cntaddr>\n" +
"                  <cntvoice>(303) 497-6826</cntvoice>\n" +
"                  <cntfax>(303) 497-6513</cntfax>\n" +
"                  <cntemail>ngdc.info@noaa.gov</cntemail>\n" +
"                  <hours>7:30 - 5:00 Mountain</hours>\n" +
"                </cntinfo>\n" +
"              </publish_cntinfo>\n" +
"            </pubinfo>\n" +
"            <othercit>29 pages</othercit>\n" +
"            <onlink>http://www.ngdc.noaa.gov/dem/squareCellGrid/getReport/258</onlink>\n" +
"            <CI_OnlineResource>\n" +
"              <linkage>http://www.ngdc.noaa.gov/dem/squareCellGrid/getReport/258</linkage>\n" +
"              <name>Digital Elevation Model of Adak, Alaska: Procedures, Data Sources and Analysis</name>\n" +
"              <description>Report describing the development of the Adak, Alaska DEM</description>\n" +
"              <function>download</function>\n" +
"            </CI_OnlineResource>\n" +
"          </citeinfo>\n" +
"        </userguid>\n" +
"      </documnts>\n" +
*/

"    </descript>\n");

//time range
    writer.write(
"    <timeperd>\n" +
"      <timeinfo>\n" +
"        <rngdates>\n" +
"          <begdate>" + (timeEdv == null || timeEdv.destinationMinString().length() < 10? unknown :
           String2.replaceAll(timeEdv.destinationMinString().substring(0, 10), "-", "")) + 
           "</begdate>\n" +
"          <enddate>" + (timeEdv == null || timeEdv.destinationMaxString().length() < 10? unknown :
           String2.replaceAll(timeEdv.destinationMaxString().substring(0, 10), "-", "")) + 
           "</enddate>\n" +
"        </rngdates>\n" +
"      </timeinfo>\n" +
       //http://www.fgdc.gov/csdgmgraphical/ideninfo/timepd/current.htm
"      <current>ground condition</current>\n" +  //I think it means: that's what I see in the dataset
"    </timeperd>\n");

//???  crap! FGDC requires lon to be +/-180 and has no guidance for 0 - 360 datasets
//so just deal with some of the options
// and use (float) to avoid float->double bruising
//default: just the lon part already in -180 to 180.
float lonMin = (float)Math2.minMax(-180, 180, lonEdv.destinationMin());
float lonMax = (float)Math2.minMax(-180, 180, lonEdv.destinationMax());
// 0 to 360  -> -180 to 180
if (lonEdv.destinationMin() >=   0 && lonEdv.destinationMin() <= 20 &&
    lonEdv.destinationMax() >= 340) {
    lonMin = -180;
    lonMax = 180;
//all lon >=180, so shift down 360
} else if (lonEdv.destinationMin() >= 180) { 
    lonMin = (float)Math2.minMax(-180, 180, lonEdv.destinationMin() - 360);
    lonMax = (float)Math2.minMax(-180, 180, lonEdv.destinationMax() - 360);
}
writer.write(
"    <status>\n" +
"      <progress>Complete</progress>\n" +
"      <update>As needed</update>\n" +
"    </status>\n" +
"    <spdom>\n" +
"      <bounding>\n" +
"        <westbc>"  + lonMin + "</westbc>\n" +
"        <eastbc>"  + lonMax + "</eastbc>\n" +
"        <northbc>" + (float)Math2.minMax(-90, 90, latEdv.destinationMax()) + "</northbc>\n" +
"        <southbc>" + (float)Math2.minMax(-90, 90, latEdv.destinationMin()) + "</southbc>\n" +
"      </bounding>\n" +
"    </spdom>\n");

//keywords,  from global keywords
StringArray kar = StringArray.fromCSVNoBlanks(keywords);
writer.write(
"    <keywords>\n" +
"      <theme>\n" +
"        <themekt>" + 
            (keywordsVocabulary == unknown? "Uncontrolled" : XML.encodeAsXML(keywordsVocabulary)) + 
         "</themekt>\n");
for (int i = 0; i < kar.size(); i++)
    writer.write(
"        <themekey>" + XML.encodeAsXML(kar.get(i)) + "</themekey>\n");
writer.write(
"      </theme>\n");

//use standardNames as keywords
if (standardNames.size() > 0) {
    writer.write(
"      <theme>\n" +
"        <themekt>" + 
            XML.encodeAsXML(standardNameVocabulary == unknown? "Uncontrolled" :
                standardNameVocabulary) + 
         "</themekt>\n");
    for (int i = 0; i < standardNames.size(); i++)
        writer.write(
"        <themekey>" + XML.encodeAsXML(standardNames.get(i)) + "</themekey>\n");
    writer.write(
"      </theme>\n");
} 

writer.write(
"    </keywords>\n");

//Platform and Instrument Indentification: satellite, sensor
if (satellite != unknown || sensor != unknown)
    writer.write(
"    <plainsid>\n" + //long and short names the same since that's all I have
"      <missname>" + XML.encodeAsXML(project) + "</missname>\n" +
"      <platflnm>" + XML.encodeAsXML(satellite) + "</platflnm>\n" +  
"      <platfsnm>" + XML.encodeAsXML(satellite) + "</platfsnm>\n" +
"      <instflnm>" + XML.encodeAsXML(sensor) + "</instflnm>\n" +
"      <instshnm>" + XML.encodeAsXML(sensor) + "</instshnm>\n" +
"    </plainsid>\n");

//access constraints   and use constraints
writer.write(
"    <accconst>" + 
        (getAccessibleTo() == null? "None." : "Authorized users only") + 
    "</accconst>\n" +  
"    <useconst>" + XML.encodeAsXML(license) + "</useconst>\n");

//point of contact:   creatorName creatorEmail
String conOrg = institution;
String conName = creatorName;
String conPhone = unknown;
String conEmail = creatorEmail;
String conPos = unknown;
writer.write(
"    <ptcontac>\n" +
"      <cntinfo>\n" +
"        <cntorgp>\n" +
"          <cntorg>" + XML.encodeAsXML(conOrg) + "</cntorg>\n" +
"          <cntper>" + XML.encodeAsXML(conName) + "</cntper>\n" +
"        </cntorgp>\n" +
"        <cntpos>" + XML.encodeAsXML(conPos) + "</cntpos>\n" + //required
"        <cntaddr>\n" +
"          <addrtype>Mailing and Physical Address</addrtype>\n" +
"          <address>" + unknown + "</address>\n" +
"          <city>" + unknown + "</city>\n" +
"          <state>" + unknown + "</state>\n" +
"          <postal>" + unknown + "</postal>\n" +
"          <country>" + unknown + "</country>\n" +
"        </cntaddr>\n" +
"        <cntvoice>" + XML.encodeAsXML(conPhone) + "</cntvoice>\n" +
//"        <cntfax>303-497-6513</cntfax>\n" +
"        <cntemail>" + XML.encodeAsXML(conEmail) + "</cntemail>\n" +
//"        <hours>9am-5pm, M-F, Mountain Time</hours>\n" +
//"        <cntinst>Contact NGDC&apos;s Marine Geology and Geophysics Division. http://www.ngdc.noaa.gov/mgg/aboutmgg/contacts.html</cntinst>\n" +
"      </cntinfo>\n" +
"    </ptcontac>\n");


//graphical view of the data
writer.write(
"    <browse>\n" +
"      <browsen>" + XML.encodeAsXML(datasetUrl + ".graph") + "</browsen>\n" +
"      <browsed>Web page to make a customized map or graph of the data</browsed>\n" +
"      <browset>HTML</browset>\n" +
"    </browse>\n");

if (contributorName  != unknown || 
    contributorEmail != unknown ||
    contributorRole  != unknown ||
    acknowledgement  != unknown)
    writer.write(
"    <datacred>" + XML.encodeAsXML(
    (contributorName  == unknown? "" : "Contributor Name: "  + contributorName  + "\n") +
    (contributorEmail == unknown? "" : "Contributor Email: " + contributorEmail + "\n") +
    (contributorRole  == unknown? "" : "Contributor Role: "  + contributorRole  + "\n") +
    (acknowledgement  == unknown? "" : "Acknowledgement: "   + acknowledgement  + "\n")) +
"    </datacred>\n");

writer.write(
//"    <native>Microsoft Windows 2000 Version 5.2 (Build 3790) Service Pack 2; ESRI ArcCatalog 9.2.4.1420</native>\n" +

//aggregation info - if data is part of larger collection.  
//But ERDDAP encourages as much aggregation as possible for each dataset (e.g., all times).
//"    <agginfo>\n" +
//"      <conpckid>\n" +
//"        <datsetid>gov.noaa.ngdc.mgg.dem:tigp</datsetid>\n" +
//"      </conpckid>\n" +
//"    </agginfo>\n" +
"  </idinfo>\n");

//data quality
writer.write(
"  <dataqual>\n" +
"    <logic>" + unknown + "</logic>\n" +
"    <complete>" + unknown + "</complete>\n" +
"    <posacc>\n" +
"      <horizpa>\n" +
"        <horizpar>" + unknown + "</horizpar>\n" +
"      </horizpa>\n" +
"      <vertacc>\n" +
"        <vertaccr>" + unknown + "</vertaccr>\n" +
"      </vertacc>\n" +
"    </posacc>\n" +
"    <lineage>\n");

//writer.write(
//"      <srcinfo>\n" +
//"        <srccite>\n" +
//"          <citeinfo>\n" +
//"            <origin>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</origin>\n" +
//"            <origin_cntinfo>\n" +
//"              <cntinfo>\n" +
//"                <cntorgp>\n" +
//"                  <cntorg>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</cntorg>\n" +
//"                  <cntper>User Services</cntper>\n" +
//"                </cntorgp>\n" +
//"                <cntaddr>\n" +
//"                  <addrtype>Mailing and Physical Address</addrtype>\n" +
//"                  <address>NOAA/NESDIS/NGDC E/GC 325 Broadway</address>\n" +
//"                  <city>Boulder</city>\n" +
//"                  <state>CO</state>\n" +
//"                  <postal>80305-3328</postal>\n" +
//"                  <country>USA</country>\n" +
//"                </cntaddr>\n" +
//"                <cntvoice>(303) 497-6826</cntvoice>\n" +
//"                <cntfax>(303) 497-6513</cntfax>\n" +
//"                <cntemail>ngdc.info@noaa.gov</cntemail>\n" +
//"                <hours>7:30 - 5:00 Mountain</hours>\n" +
//"              </cntinfo>\n" +
//"            </origin_cntinfo>\n" +
//"            <pubdate>2008</pubdate>\n" +
//"            <title>NOS Hydrographic Surveys</title>\n" +
//"            <onlink>http://www.ngdc.noaa.gov/mgg/bathymetry/hydro.html</onlink>\n" +
//"            <CI_OnlineResource>\n" +
//"              <linkage>http://www.ngdc.noaa.gov/mgg/bathymetry/hydro.html</linkage>\n" +
//"              <name>NOS Hydrographic Survey Database</name>\n" +
//"              <description>Digital database of NOS hydrographic surveys that date back to the late 19th century.</description>\n" +
//"              <function>download</function>\n" +
//"            </CI_OnlineResource>\n" +
//"          </citeinfo>\n" +
//"        </srccite>\n" +
//"        <typesrc>online</typesrc>\n" +
//"        <srctime>\n" +
//"          <timeinfo>\n" +
//"            <rngdates>\n" +
//"              <begdate>1933</begdate>\n" +
//"              <enddate>2005</enddate>\n" +
//"            </rngdates>\n" +
//"          </timeinfo>\n" +
//"          <srccurr>ground condition</srccurr>\n" +
//"        </srctime>\n" +
//"        <srccitea>NOS Hydrographic Surveys</srccitea>\n" +
//"        <srccontr>hydrographic surveys</srccontr>\n" +
//"      </srcinfo>\n");
       //and several other other <srcinfo>

//process step:  lines from history
String historyLines[] = String2.split(history, '\n');
for (int hl = 0; hl < historyLines.length; hl++) {
    String step = historyLines[hl];
    String date = unknown;
    int spo = step.indexOf(' ');      //date must be YYYY-MM-DD.* initially
    if (spo >= 10 && step.substring(0, 10).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
        date = String2.replaceAll(step.substring(0, 10), "-", ""); //now YYYYMMDD
    } else {
        spo = 0;
    }

    writer.write(
"      <procstep>\n" +
"        <procdesc>" + XML.encodeAsXML(step.substring(spo).trim()) + "</procdesc>\n" +
"        <procdate>" + XML.encodeAsXML(date) + "</procdate>\n" +
"      </procstep>\n");
}

writer.write(
"    </lineage>\n" +
"  </dataqual>\n");

//Spatial Data Organization information  (very different for EDDTable and EDDGrid)
writer.write(
"  <spdoinfo>\n" +
     //high level description, see list at http://www.fgdc.gov/csdgmgraphical/sdorg/dirspref/dirsprm.htm
"    <direct>Point</direct>\n" +  
"    <ptvctinf>\n" +
"      <sdtstrm>\n" +
         //see http://www.fgdc.gov/csdgmgraphical/sdorg/dirspref/objinfo/pntveco/sdtstrm/sdtstyp.htm
"        <sdtstyp>" + XML.encodeAsXML(
    cdmDataType.equals(CDM_PROFILE)?                "Point" :
    cdmDataType.equals(CDM_RADIALSWEEP)?            "Node, network" : //???
    cdmDataType.equals(CDM_TIMESERIES)?             "Point" :
    cdmDataType.equals(CDM_TIMESERIESPROFILE)?      "Point" :
    cdmDataType.equals(CDM_SWATH)?                  "Node, network" : //???
    cdmDataType.equals(CDM_TRAJECTORY)?             "String" :        //???
    cdmDataType.equals(CDM_TRAJECTORYPROFILE)?      "String" :        //???
                                                    "Point") + //CDM_POINT or CDM_OTHER 
         "</sdtstyp>\n" +
"      </sdtstrm>\n" +
"    </ptvctinf>\n" +
"  </spdoinfo>\n");

//Spatial Reference Information
//since res info is never known and since "Unknown" not allowed, skip this section
/*writer.write(
"  <spref>\n" +
"    <horizsys>\n" +
"      <geograph>\n" +  
"        <latres>"  + unknown + "</latres>\n" +
"        <longres>" + unknown + "</longres>\n" +
"        <geogunit>Decimal degrees</geogunit>\n" +
"      </geograph>\n" +
"      <geodetic>\n" +
"        <horizdn>D_WGS_1984</horizdn>\n" +  //??? I'm not certain this is true for all
"        <ellips>WGS_1984</ellips>\n" +
"        <semiaxis>6378137.000000</semiaxis>\n" +
"        <denflat>298.257224</denflat>\n" +
"      </geodetic>\n" +
"    </horizsys>\n");

int depthIndex = String2.indexOf(dataVariableDestinationNames, "depth");
if (altEdv != null || depthEdv != null) {
    writer.write(
"    <vertdef>\n" +

    (altEdv == null? "" : 
"      <altsys>\n" +
"        <altdatum>" + unknown + "</altdatum>\n" +
"        <altres>" + unknown + "</altres>\n" +
"        <altunits>meters</altunits>\n" +
"        <altenc>" + unknown + "</altenc>\n" +
"      </altsys>\n") +

    (depthEdv == null? "" : 
"      <depthsys>\n" +
"        <depthdn>" + unknown + "</depthdn>\n" +
"        <depthres>" + unknown + "</depthres>\n" +
"        <depthdu>meters</depthdu>\n" +
"        <depthem>" + unknown + "</depthem>\n" +
"      </depthsys>\n") +

"    </vertdef>\n");
}

writer.write(
"  </spref>\n");
*/

//distribution information: admin...
writer.write(
"  <distinfo>\n" +
"    <distrib>\n" +
       adminCntinfo +
"    </distrib>\n" +
"    <resdesc>" + XML.encodeAsXML("ERDDAP, version " + EDStatic.erddapVersion + 
      ": get metadata; download data; make graphs and maps.") + "</resdesc>\n" +
"    <distliab>" + XML.encodeAsXML(license) + "</distliab>\n" +
"    <stdorder>\n");

//data file types
for (int ft = 0; ft < dataFileTypeNames.length; ft++)
writer.write(
"      <digform>\n" +
"        <digtinfo>\n" + //digital transfer info
"          <formname>" + dataFileTypeNames[ft] + "</formname>\n" + 
"          <formvern>1</formvern>\n" +
"          <formspec>" + XML.encodeAsXML(dataFileTypeDescriptions[ft] + " " +
              dataFileTypeInfo[ft]) + "</formspec>\n" +
//           I think file decompression technique only used if file *always* encoded.
//"          <filedec>gzip</filedec>\n" + 
"        </digtinfo>\n" +
"        <digtopt>\n" +
"          <onlinopt>\n" +
"            <computer>\n" +
"              <networka>\n" +
"                <networkr>" + XML.encodeAsXML(datasetUrl + ".html") + "</networkr>\n" +
                 //EDDGrid has <CI_OnlineResource> here
"              </networka>\n" +
"            </computer>\n" +
"          </onlinopt>\n" +
"        </digtopt>\n" +
"      </digform>\n");

//image file types
for (int ft = 0; ft < imageFileTypeNames.length; ft++)
writer.write(
"      <digform>\n" +
"        <digtinfo>\n" + //digital transfer info
"          <formname>" + imageFileTypeNames[ft] + "</formname>\n" + 
"          <formvern>1</formvern>\n" +
"          <formspec>" + XML.encodeAsXML(imageFileTypeDescriptions[ft] + " " +
              imageFileTypeInfo[ft]) + "</formspec>\n" +
//           I think file decompression technique only used if file *always* encoded.
//"          <filedec>gzip</filedec>\n" + //file decompression technique
"        </digtinfo>\n" +
"        <digtopt>\n" +
"          <onlinopt>\n" +
"            <computer>\n" +
"              <networka>\n" +
"                <networkr>" + XML.encodeAsXML(datasetUrl + ".graph") + "</networkr>\n" +
                 //EDDGrid has  <CI_OnlineResource> here
"              </networka>\n" +
"            </computer>\n" +
"          </onlinopt>\n" +
"        </digtopt>\n" +
"      </digform>\n");


writer.write(
"      <fees>None</fees>\n" +
"    </stdorder>\n" +
"  </distinfo>\n");

writer.write(
"  <metainfo>\n" +
"    <metd>" + eddCreationDate + "</metd>\n" +
"    <metc>\n" + 
    adminCntinfo +
"    </metc>\n" +  
     //EDDGrid metstdn has ": Extensions for Remote Sensing Metadata"
"    <metstdn>Content Standard for Digital Geospatial Metadata</metstdn>\n" +
"    <metstdv>FGDC-STD-012-2002</metstdv>\n" +
"    <mettc>universal time</mettc>\n" +  
     //metadata access constraints
"    <metac>" + XML.encodeAsXML(getAccessibleTo() == null? "None." : "Authorized users only") + 
    "</metac>\n" +  
     //metadata use constraints
"    <metuc>" + XML.encodeAsXML(license) + "</metuc>\n" +
"  </metainfo>\n" +
"</metadata>\n");
    }


    /** 
     * This writes the dataset's ISO 19115-2/19139 XML to the writer.
     * <br>The template is initially based on EDDGrid.writeISO19115
     * <br>modified as warranted by looking at
     * <br>http://oos.soest.hawaii.edu/pacioos/metadata/iso/PACN_FISH_TRANSECT.xml
     * <br>Made pretty via TestAll: XML.prettyXml(in, out) and other small changes
     * <br>and stored on Bob's computer as
     * <br>c:/programs/iso19115/PACN_FISH_TRANSECTpretty.xml
     * <br>I note that:
     * <br>* PACN_FISH uses an ISO resources list, whereas ncISO uses one from NGDC.
     *
     * <p>Probably better example (from Ted Habermann) is 
     * <br>from Nearshore Sensor 1 dataset from 
     * <br>http://oos.soest.hawaii.edu/thredds/idd/nss_hioos.html
     * <br>and stored on Bob's computer as F:/programs/iso19115/ns01agg.xml
     *
     * <br>See also  https://geo-ide.noaa.gov/wiki/index.php?title=NcISO#Questions_and_Answers
     *
     * <p>This is usually just called by the dataset's constructor, 
     * at the end of EDDTable/Grid.ensureValid.
     * 
     * <p>Help with schema: http://www.schemacentral.com/sc/niem21/e-gmd_contact-1.html
     * <br>List of nilReason: http://www.schemacentral.com/sc/niem21/a-gco_nilReason.html
     * 
     * @param writer a UTF-8 writer
     * @throws Throwable if trouble (e.g., no latitude and longitude variables)
     */
    public void writeISO19115(Writer writer) throws Throwable {
        //future: support datasets with x,y (and not longitude,latitude)?

        if (lonIndex < 0 || latIndex < 0) 
            throw new SimpleException(EDStatic.noXxxNoLL);

        String tErddapUrl = EDStatic.erddapUrl(getAccessibleTo() == null? null : "anyone");
        String datasetUrl = tErddapUrl + "/tabledap/" + datasetID;
        //String wcsUrl     = tErddapUrl + "/wcs/"     + datasetID() + "/" + wcsServer;  // "?" at end?
        String wmsUrl     = tErddapUrl + "/wms/"     + datasetID() + "/" + WMS_SERVER; // "?" at end?
        String domain = EDStatic.baseUrl;
        if (domain.startsWith("http://"))
            domain = domain.substring(7);
        String eddCreationDate = String2.replaceAll(
            Calendar2.millisToIsoZuluString(creationTimeMillis()), "-", "").substring(0, 8) + "Z";

        String acknowledgement = combinedGlobalAttributes.getString("acknowledgement");
        String contributorName = combinedGlobalAttributes.getString("contributor_name");
        String contributorEmail= combinedGlobalAttributes.getString("contributor_email");
        String contributorRole = combinedGlobalAttributes.getString("contributor_role");
        //creatorName assumed to be person. use institution for related organization
        String creatorName     = combinedGlobalAttributes.getString("creator_name");
        String creatorEmail    = combinedGlobalAttributes.getString("creator_email");
        //creatorUrl: use infoUrl
        String dateCreated     = combinedGlobalAttributes.getString("date_created");
        String dateIssued      = combinedGlobalAttributes.getString("date_issued");
        if (dateCreated != null && dateCreated.length() >= 10 && !dateCreated.endsWith("Z"))
            dateCreated = dateCreated.substring(0, 10) + "Z";
        if (dateIssued  != null && dateIssued.length()  >= 10 && !dateIssued.endsWith("Z"))
            dateIssued  = dateIssued.substring(0, 10)  + "Z";
        String history         = combinedGlobalAttributes.getString("history");
        String infoUrl         = combinedGlobalAttributes.getString("infoUrl"); 
        String institution     = combinedGlobalAttributes.getString("institution");
        String keywords        = combinedGlobalAttributes.getString("keywords");
        String keywordsVocabulary = combinedGlobalAttributes.getString("keywordsVocabulary");
        if (keywords == null) { //use the crude, ERDDAP keywords
            keywords           = EDStatic.keywords;
            keywordsVocabulary = null;
        }
        String license         = combinedGlobalAttributes.getString("license");
        String project         = combinedGlobalAttributes.getString("project");
        if (project == null) 
            project = institution;
        String standardNameVocabulary = combinedGlobalAttributes.getString("standard_name_vocabulary");
        String sourceUrl       = publicSourceUrl();

        //testMinimalMetadata is useful for Bob doing tests of validity of FGDC results 
        //  when a dataset has minimal metadata
        boolean testMinimalMetadata = false; //only true when testing. normally false;
        if (testMinimalMetadata) {
            acknowledgement = null;
            contributorName = null;
            contributorEmail= null;
            contributorRole = null;
            creatorName     = null;
            creatorEmail    = null;
            dateCreated     = null;
            dateIssued      = null;
            history         = null;
            //infoUrl         = null;  //ensureValid ensure that some things exist
            //institution     = null;
            keywords        = null;
            keywordsVocabulary = null;
            license         = null;
            project         = null;
            standardNameVocabulary = null;
            //sourceUrl       = null;
        }

        if (dateCreated == null) 
            dateCreated = eddCreationDate;
        EDVLat latEdv     = (EDVLat)dataVariables[latIndex];
        EDVLon lonEdv     = (EDVLon)dataVariables[lonIndex];
        EDVTime timeEdv   = timeIndex < 0 || testMinimalMetadata? null :  
            (EDVTime)dataVariables[timeIndex];
        EDVAlt altEdv     = altIndex < 0 || testMinimalMetadata? null :
            (EDVAlt)dataVariables[altIndex];
        EDVDepth depthEdv = depthIndex < 0 || testMinimalMetadata? null :
            (EDVDepth)dataVariables[depthIndex];

        String minTime = ""; //iso string with Z, may be ""
        String maxTime = "";
        if (timeEdv != null) { 
            minTime = timeEdv.destinationMinString();  //differs from EDDGrid, may be ""
            maxTime = timeEdv.destinationMaxString();
        }


        double minVert = Double.NaN; //in destination units (may be positive = up[I use] or down?! any units)
        double maxVert = Double.NaN;
        if (altEdv != null) {
            minVert = altEdv.destinationMin();
            maxVert = altEdv.destinationMax();
        } else if (depthEdv != null) {
            minVert = -depthEdv.destinationMax(); //make into alt
            maxVert = -depthEdv.destinationMin();
        }

        StringArray standardNames = new StringArray();
        if (!testMinimalMetadata) {
            for (int v = 0; v < dataVariables.length; v++) {
                String sn = dataVariables[v].combinedAttributes().getString("standard_name");
                if (sn != null)
                    standardNames.add(sn);
            }
        }

        //lon,lat Min/Max
        //??? I'm not certain but I suspect ISO requires lon to be +/-180.
        //    I don't see guidance for 0 - 360 datasets.
        //so just deal with some of the options
        // and use (float) to avoid float->double bruising
        //default: just the lon part already in -180 to 180.
        //EDDGrid doesn't allow for NaN
        float lonMin = (float)lonEdv.destinationMin();
        float lonMax = (float)lonEdv.destinationMax();
        if (!Float.isNaN(lonMin) && !Float.isNaN(lonMax)) {
            lonMin = (float)Math2.minMax(-180, 180, lonMin);
            lonMax = (float)Math2.minMax(-180, 180, lonMax);
            // 0 to 360  -> -180 to 180
            if (lonEdv.destinationMin() >=   0 && lonEdv.destinationMin() <= 20 &&
                lonEdv.destinationMax() >= 340) {
                lonMin = -180;
                lonMax = 180;
            //all lon >=180, so shift down 360
            } else if (lonEdv.destinationMin() >= 180) { 
                lonMin = (float)Math2.minMax(-180, 180, lonEdv.destinationMin() - 360);
                lonMax = (float)Math2.minMax(-180, 180, lonEdv.destinationMax() - 360);
            }
        }
        float latMin = (float)latEdv.destinationMin();
        float latMax = (float)latEdv.destinationMax();
        if (!Float.isNaN(latMin)) latMin = (float)Math2.minMax(-90, 90, latMin);
        if (!Float.isNaN(latMax)) latMax = (float)Math2.minMax(-90, 90, latMax);

//write the xml       
writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<gmi:MI_Metadata \n" +
"  xmlns=\"http://www.isotc211.org/2005/gmi\"\n" +   //eddgrid doesn't have this
"  xmlns:srv=\"http://www.isotc211.org/2005/srv\"\n" +
"  xmlns:gmx=\"http://www.isotc211.org/2005/gmx\"\n" +
"  xmlns:gsr=\"http://www.isotc211.org/2005/gsr\"\n" +
"  xmlns:gss=\"http://www.isotc211.org/2005/gss\"\n" +
"  xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
"  xmlns:gts=\"http://www.isotc211.org/2005/gts\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gco=\"http://www.isotc211.org/2005/gco\"\n" +
"  xmlns:gmd=\"http://www.isotc211.org/2005/gmd\"\n" +
"  xmlns:gmi=\"http://www.isotc211.org/2005/gmi\"\n" +
"  xsi:schemaLocation=\"http://www.isotc211.org/2005/gmi http://www.ngdc.noaa.gov/metadata/published/xsd/schema.xsd\">\n" +

"  <gmd:fileIdentifier>\n" +
"    <gco:CharacterString>" + datasetID() + "</gco:CharacterString>\n" +
"  </gmd:fileIdentifier>\n" +
"  <gmd:language>\n" +
"    <gmd:LanguageCode " +
       "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:LanguageCode\" " +
       "codeListValue=\"eng\">eng</gmd:LanguageCode>\n" +
"  </gmd:language>\n" +
"  <gmd:characterSet>\n" +
"    <gmd:MD_CharacterSetCode " +
       "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CharacterSetCode\" " +
       "codeListValue=\"UTF8\">UTF8</gmd:MD_CharacterSetCode>\n" +
"  </gmd:characterSet>\n" +
"  <gmd:hierarchyLevel>\n" +
"    <gmd:MD_ScopeCode " +
       "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" " +
       "codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n" +
"  </gmd:hierarchyLevel>\n" +
"  <gmd:hierarchyLevel>\n" +
"    <gmd:MD_ScopeCode " +
       "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" " +
       "codeListValue=\"service\">service</gmd:MD_ScopeCode>\n" +
"  </gmd:hierarchyLevel>\n");
        
//contact: use admin... ("resource provider" is last in chain responsible for metadata)
//  (or use creator...?)
writer.write(
"  <gmd:contact>\n" +  
"    <gmd:CI_ResponsibleParty>\n" +
"      <gmd:individualName>\n" +
"        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</gco:CharacterString>\n" +
"      </gmd:individualName>\n" +
"      <gmd:organisationName>\n" +
"        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</gco:CharacterString>\n" +
"      </gmd:organisationName>\n" +
"      <gmd:contactInfo>\n" +
"        <gmd:CI_Contact>\n" +
"          <gmd:phone>\n" +
"            <gmd:CI_Telephone>\n" +
"              <gmd:voice>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminPhone) + "</gco:CharacterString>\n" +
"              </gmd:voice>\n" +
"            </gmd:CI_Telephone>\n" +
"          </gmd:phone>\n" +
"          <gmd:address>\n" +
"            <gmd:CI_Address>\n" +
"              <gmd:deliveryPoint>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminAddress) + "</gco:CharacterString>\n" +
"              </gmd:deliveryPoint>\n" +
"              <gmd:city>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminCity) + "</gco:CharacterString>\n" +
"              </gmd:city>\n" +
"              <gmd:administrativeArea>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</gco:CharacterString>\n" +
"              </gmd:administrativeArea>\n" +
"              <gmd:postalCode>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</gco:CharacterString>\n" +
"              </gmd:postalCode>\n" +
"              <gmd:country>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminCountry) + "</gco:CharacterString>\n" +
"              </gmd:country>\n" +
"              <gmd:electronicMailAddress>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminEmail) + "</gco:CharacterString>\n" +
"              </gmd:electronicMailAddress>\n" +
"            </gmd:CI_Address>\n" +
"          </gmd:address>\n" +
"        </gmd:CI_Contact>\n" +
"      </gmd:contactInfo>\n" +
"      <gmd:role>\n" +
"        <gmd:CI_RoleCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" " +
         "codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n" +
"      </gmd:role>\n" +
"    </gmd:CI_ResponsibleParty>\n" +
"  </gmd:contact>\n" +  
"  <gmd:dateStamp>\n" +
"    <gco:Date>" + eddCreationDate + "</gco:Date>\n" +
"  </gmd:dateStamp>\n" +
"  <gmd:metadataStandardName>\n" + 
//Ted Haberman says 19115-2 is correct.  Part 2 has improvements for lots of 
//things, not just "Imagery and Gridded Data"
//PacN dataset had "with Biological Extensions" from NGDC, but I'm not using those.
"    <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions " +
    "for Imagery and Gridded Data</gco:CharacterString>\n" +
"  </gmd:metadataStandardName>\n" +
"  <gmd:metadataStandardVersion>\n" +
"    <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>\n" +
"  </gmd:metadataStandardVersion>\n" +

//spatialRepresentation
"  <gmd:spatialRepresentationInfo>\n" +
"    <gmd:MD_GridSpatialRepresentation>\n" +
"      <gmd:numberOfDimensions>\n" +
"        <gco:Integer>" + 
         (2 + (timeEdv == null? 0 : 1) + (altEdv == null && depthEdv == null? 0 : 1)) + 
         "</gco:Integer>\n" +  
"      </gmd:numberOfDimensions>\n" +
//longitude is "column" dimension
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" " +
               "codeListValue=\"column\">column</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//latitude is "row" dimension
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" " +
               "codeListValue=\"row\">row</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +

//altitude or depth is "vertical" dimension
(altEdv == null && depthEdv == null? "" :
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" " +
               "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n") +

//time is "temporal" dimension
(timeEdv == null? "" : 
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" " +
               "codeListValue=\"temporal\">temporal</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n") +

//cellGeometry
//??? Ted Habermann has no answer for cellGeometry for tabular data
"      <gmd:cellGeometry>\n" +
"        <gmd:MD_CellGeometryCode codeList=\"\" codeListValue=\"\" codeSpace=\"\" />\n" +
"      </gmd:cellGeometry>\n" +
"      <gmd:transformationParameterAvailability gco:nilReason=\"unknown\"/>\n" + 
"    </gmd:MD_GridSpatialRepresentation>\n" +
"  </gmd:spatialRepresentationInfo>\n");

//metadataExtensionIfo for taxomony would go here


//*** IdentificationInfo loop
int iiDataIdentification = 0;
int iiOPeNDAP = 1;
int iiWMS = 2;
for (int ii = 0; ii <= iiWMS; ii++) {  

    //currently, no tabular datasets are accessibleViaWMS
    if (ii == iiWMS && accessibleViaWMS().length() > 0)
        break;

    writer.write(
"  <gmd:identificationInfo>\n" +

(ii == iiDataIdentification?   "    <gmd:MD_DataIdentification id=\"DataIdentification\">\n" :
 ii == iiOPeNDAP?              "    <srv:SV_ServiceIdentification id=\"OPeNDAP\">\n" :
 ii == iiWMS?                  "    <srv:SV_ServiceIdentification id=\"OGC-WMS\">\n" :
                               "    <gmd:ERROR id=\"ERROR\">\n") +

"      <gmd:citation>\n" +
"        <gmd:CI_Citation>\n" +
"          <gmd:title>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(title()) + "</gco:CharacterString>\n" +
"          </gmd:title>\n" +
"          <gmd:date>\n" +
"            <gmd:CI_Date>\n" +
"              <gmd:date>\n" +
"                <gco:Date>" + XML.encodeAsXML(dateCreated) + "</gco:Date>\n" +
"              </gmd:date>\n" +
"              <gmd:dateType>\n" +
"                <gmd:CI_DateTypeCode " +
                   "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" " +
                   "codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n" +
"              </gmd:dateType>\n" +
"            </gmd:CI_Date>\n" +
"          </gmd:date>\n" + 

(dateIssued == null? "" : 
"          <gmd:date>\n" +
"            <gmd:CI_Date>\n" +
"              <gmd:date>\n" +
"                <gco:Date>" + XML.encodeAsXML(dateIssued) + "</gco:Date>\n" +
"              </gmd:date>\n" +
"              <gmd:dateType>\n" +
"                <gmd:CI_DateTypeCode " +
                   "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" " +
                   "codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n" +
"              </gmd:dateType>\n" +
"            </gmd:CI_Date>\n" +
"          </gmd:date>\n") +

//naming_authority
(ii == iiDataIdentification? 
"          <gmd:identifier>\n" +
"            <gmd:MD_Identifier>\n" +
"              <gmd:authority>\n" +
"                <gmd:CI_Citation>\n" +
"                  <gmd:title>\n" +
"                    <gco:CharacterString>" + XML.encodeAsXML(domain) + "</gco:CharacterString>\n" +
"                  </gmd:title>\n" +
"                  <gmd:date gco:nilReason=\"inapplicable\"/>\n" +
"                </gmd:CI_Citation>\n" +
"              </gmd:authority>\n" +
"              <gmd:code>\n" +  
"                <gco:CharacterString>" + XML.encodeAsXML(datasetID()) + "</gco:CharacterString>\n" +
"              </gmd:code>\n" +
"            </gmd:MD_Identifier>\n" +
"          </gmd:identifier>\n" :
"") + //other ii


//citedResponsibleParty   role=originator:   from creator_email, creator_name, infoUrl, institution
"          <gmd:citedResponsibleParty>\n" +
"            <gmd:CI_ResponsibleParty>\n" +

(creatorName == null?  
"              <gmd:individualName gco:nilReason=\"missing\"/>\n" :
"              <gmd:individualName>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(creatorName) + "</gco:CharacterString>\n" +
"              </gmd:individualName>\n") +

(institution == null?
"              <gmd:organisationName gco:nilReason=\"missing\"/>\n" :
"              <gmd:organisationName>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(institution) + "</gco:CharacterString>\n" +
"              </gmd:organisationName>\n") +

//originator: from creator_..., specify contactInfo   
"              <gmd:contactInfo>\n" +
"                <gmd:CI_Contact>\n" +

(creatorEmail == null?
"                  <gmd:address gco:nilReason=\"missing\"/>\n" :
"                  <gmd:address>\n" +
"                    <gmd:CI_Address>\n" +
"                      <gmd:electronicMailAddress>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(creatorEmail) + "</gco:CharacterString>\n" +
"                      </gmd:electronicMailAddress>\n" +
"                    </gmd:CI_Address>\n" +
"                  </gmd:address>\n") +

"                  <gmd:onlineResource>\n" +
"                    <gmd:CI_OnlineResource>\n" +
"                      <gmd:linkage>\n" +  //in ERDDAP, infoUrl is better and more reliable than creator_url
"                        <gmd:URL>" + XML.encodeAsXML(infoUrl) + "</gmd:URL>\n" +
"                      </gmd:linkage>\n" +
"                      <gmd:protocol>\n" +
"                        <gco:CharacterString>http</gco:CharacterString>\n" +
"                      </gmd:protocol>\n" +
"                      <gmd:applicationProfile>\n" +
"                        <gco:CharacterString>web browser</gco:CharacterString>\n" +
"                      </gmd:applicationProfile>\n" +
"                      <gmd:name>\n" +
"                        <gco:CharacterString>Background Information</gco:CharacterString>\n" +
"                      </gmd:name>\n" +
"                      <gmd:description><gco:CharacterString/></gmd:description>\n" +
"                      <gmd:function>\n" +
"                        <gmd:CI_OnLineFunctionCode " +
                           "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" " +
                           "codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n" +
"                      </gmd:function>\n" +
"                    </gmd:CI_OnlineResource>\n" +
"                  </gmd:onlineResource>\n" +
"                </gmd:CI_Contact>\n" +
"              </gmd:contactInfo>\n" +
"              <gmd:role>\n" +
"                <gmd:CI_RoleCode " +
                   "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" " +
                   "codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:citedResponsibleParty>\n");

//contributor_name (ncISO assumes it's a name; I assume it's an organisation), _role
if (contributorName != null || contributorRole != null)
    writer.write(
"          <gmd:citedResponsibleParty>\n" +
"            <gmd:CI_ResponsibleParty>\n" +
"              <gmd:individualName gco:nilReason=\"missing\"/>\n" +

(contributorName == null? 
"              <gmd:organisationName gco:nilReason=\"missing\"/>\n" :
"              <gmd:organisationName>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(contributorName) + "</gco:CharacterString>\n" +
"              </gmd:organisationName>\n") +

(contributorEmail == null?
"              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" :
"              <gmd:contactInfo>\n" +
"                <gmd:electronicMailAddress>\n" +
"                  <gco:CharacterString>" + XML.encodeAsXML(contributorEmail) + "</gco:CharacterString>\n" +
"                </gmd:electronicMailAddress>\n" +
"              </gmd:contactInfo>\n") +

"              <gmd:role>\n" +  
"                <gmd:CI_RoleCode " +
                   "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" " +
  //contributor isn't in the codeList. I asked that it be added. 
  // ncISO used something *not* in the list.  Isn't this a controlled vocabulary?
                   "codeListValue=\"contributor\">contributor</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:citedResponsibleParty>\n"); //end of  if (contributorName != null ...

writer.write(
"        </gmd:CI_Citation>\n" +
"      </gmd:citation>\n" +

//abstract
"      <gmd:abstract>\n" +
"        <gco:CharacterString>" + XML.encodeAsXML(summary()) + "</gco:CharacterString>\n" +
"      </gmd:abstract>\n");

//**
if (ii == iiDataIdentification) {
    writer.write(

    //credit
    (acknowledgement == null? 
"      <gmd:credit gco:nilReason=\"missing\"/>\n" :
"      <gmd:credit>\n" +
"        <gco:CharacterString>" + XML.encodeAsXML(acknowledgement) + "</gco:CharacterString>\n" +
"      </gmd:credit>\n") +

    //pointOfContact
"      <gmd:pointOfContact>\n" +
"        <gmd:CI_ResponsibleParty>\n" +

    (creatorName == null?  
"          <gmd:individualName gco:nilReason=\"missing\"/>\n" :
"          <gmd:individualName>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(creatorName) + "</gco:CharacterString>\n" +
"          </gmd:individualName>\n") +

    (institution == null?
"          <gmd:organisationName gco:nilReason=\"missing\"/>\n" :
"          <gmd:organisationName>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(institution) + "</gco:CharacterString>\n" +
"          </gmd:organisationName>\n") +

    //originator: from creator_..., specify contactInfo   
"          <gmd:contactInfo>\n" +
"            <gmd:CI_Contact>\n" +

    (creatorEmail == null?
"              <gmd:address gco:nilReason=\"missing\"/>\n" :
"              <gmd:address>\n" +
"                <gmd:CI_Address>\n" +
"                  <gmd:electronicMailAddress>\n" +
"                    <gco:CharacterString>" + XML.encodeAsXML(creatorEmail) + "</gco:CharacterString>\n" +
"                  </gmd:electronicMailAddress>\n" +
"                </gmd:CI_Address>\n" +
"              </gmd:address>\n") +

"              <gmd:onlineResource>\n" +
"                <gmd:CI_OnlineResource>\n" +
"                  <gmd:linkage>\n" +  //in ERDDAP, infoUrl is better and more reliable than creator_url
"                    <gmd:URL>" + XML.encodeAsXML(infoUrl) + "</gmd:URL>\n" +
"                  </gmd:linkage>\n" +
"                  <gmd:protocol>\n" +
"                    <gco:CharacterString>http</gco:CharacterString>\n" +
"                  </gmd:protocol>\n" +
"                  <gmd:applicationProfile>\n" +
"                    <gco:CharacterString>web browser</gco:CharacterString>\n" +
"                  </gmd:applicationProfile>\n" +
"                  <gmd:name>\n" +
"                    <gco:CharacterString>Background Information</gco:CharacterString>\n" +
"                  </gmd:name>\n" +
"                  <gmd:description><gco:CharacterString/></gmd:description>\n" +
"                  <gmd:function>\n" +
"                    <gmd:CI_OnLineFunctionCode " +
                       "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" " +
                       "codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n" +
"                  </gmd:function>\n" +
"                </gmd:CI_OnlineResource>\n" +
"              </gmd:onlineResource>\n" +
"            </gmd:CI_Contact>\n" +
"          </gmd:contactInfo>\n" +
"          <gmd:role>\n" +
"            <gmd:CI_RoleCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" " +
               "codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n" +
"          </gmd:role>\n" +
"        </gmd:CI_ResponsibleParty>\n" +
"      </gmd:pointOfContact>\n");

    //keywords, from global keywords
    if (keywords != null) {
        StringArray kar = StringArray.fromCSVNoBlanks(keywords);

        //segregate gcmdKeywords and remove cf standard_names
        StringArray gcmdKeywords = new StringArray();
        for (int i = kar.size() - 1; i >= 0; i--) {
            String kari = kar.get(i);
            if (kari.indexOf(" > ") > 0) {
                gcmdKeywords.add(kari);
                kar.remove(i);

            } else if (kari.indexOf('_') > 0) { 
                kar.remove(i);  //a multi-word CF Standard Name

            } else if (kari.length() <= LONGEST_ONE_WORD_CF_STANDARD_NAMES && //quick accept if short enough
                       String2.indexOf(ONE_WORD_CF_STANDARD_NAMES, kari) >= 0) { //few, so linear search is quick
                kar.remove(i);  //a one word CF Standard Name
            }
        }    

        //keywords not from vocabulary
        if (kar.size() > 0) {
            writer.write(
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n");

            for (int i = 0; i < kar.size(); i++)
                writer.write(
"          <gmd:keyword>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(kar.get(i)) + "</gco:CharacterString>\n" +
"          </gmd:keyword>\n");

            writer.write(
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" " +
               "codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n");
        }

        //gcmd keywords 
        if (gcmdKeywords.size() > 0) {
            writer.write(
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n");

            for (int i = 0; i < gcmdKeywords.size(); i++)
                writer.write(
"          <gmd:keyword>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(gcmdKeywords.get(i)) + "</gco:CharacterString>\n" +
"          </gmd:keyword>\n");

            writer.write(
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" " +
               "codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName>\n" +
"            <gmd:CI_Citation>\n" +
"              <gmd:title>\n" +
"                <gco:CharacterString>GCMD Science Keywords</gco:CharacterString>\n" +
"              </gmd:title>\n" +
"              <gmd:date gco:nilReason=\"unknown\"/>\n" +
"            </gmd:CI_Citation>\n" +
"          </gmd:thesaurusName>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n");
        }
    }

    //keywords (project)    
    if (project != null)
        writer.write(
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(project) + "</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" " +
               "codeListValue=\"project\">project</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n");

    //keywords - variable (CF) standard_names
    if (standardNames.size() > 0) {
        writer.write(
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n");
        for (int sn = 0; sn < standardNames.size(); sn++)
            writer.write(
"          <gmd:keyword>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(standardNames.get(sn)) + "</gco:CharacterString>\n" +
"          </gmd:keyword>\n");
        writer.write(
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" " +
               "codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName>\n" +
"            <gmd:CI_Citation>\n" +
"              <gmd:title>\n" +
"                <gco:CharacterString>" +      //if not specified, CF is a guess
                 XML.encodeAsXML(standardNameVocabulary == null? "CF" : standardNameVocabulary) + 
                "</gco:CharacterString>\n" +
"              </gmd:title>\n" +
"              <gmd:date gco:nilReason=\"unknown\"/>\n" +
"            </gmd:CI_Citation>\n" +
"          </gmd:thesaurusName>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n");
    }

    //resourceConstraints  (license)
    if (license != null) 
        writer.write(
"      <gmd:resourceConstraints>\n" +
"        <gmd:MD_LegalConstraints>\n" +
"          <gmd:useLimitation>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(license) + "</gco:CharacterString>\n" +
"          </gmd:useLimitation>\n" +
"        </gmd:MD_LegalConstraints>\n" +
"      </gmd:resourceConstraints>\n");

    //aggregationInfo (project), larger work
    if (project != null)
        writer.write(
"      <gmd:aggregationInfo>\n" +       
"        <gmd:MD_AggregateInformation>\n" +
"          <gmd:aggregateDataSetName>\n" +
"            <gmd:CI_Citation>\n" +
"              <gmd:title>\n" +               
"                <gco:CharacterString>" + XML.encodeAsXML(project) + "</gco:CharacterString>\n" +
"              </gmd:title>\n" +
"              <gmd:date gco:nilReason=\"inapplicable\"/>\n" +
"            </gmd:CI_Citation>\n" +
"          </gmd:aggregateDataSetName>\n" +
"          <gmd:associationType>\n" +
"            <gmd:DS_AssociationTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" " +
               "codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n" +
"          </gmd:associationType>\n" +
"          <gmd:initiativeType>\n" +
"            <gmd:DS_InitiativeTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" " +
               "codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n" +
"          </gmd:initiativeType>\n" +
"        </gmd:MD_AggregateInformation>\n" +
"      </gmd:aggregationInfo>\n");

    //aggregation, larger work       Unidata CDM  (? ncISO does this)
if (!CDM_OTHER.equals(cdmDataType())) {
    writer.write(
"      <gmd:aggregationInfo>\n" + 
"        <gmd:MD_AggregateInformation>\n" +
"          <gmd:aggregateDataSetIdentifier>\n" +
"            <gmd:MD_Identifier>\n" +
"              <gmd:authority>\n" +
"                <gmd:CI_Citation>\n" +
"                  <gmd:title>\n" +
"                    <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>\n" +
"                  </gmd:title>\n" +
"                  <gmd:date gco:nilReason=\"inapplicable\"/>\n" +
"                </gmd:CI_Citation>\n" +
"              </gmd:authority>\n" +
"              <gmd:code>\n" +
"                <gco:CharacterString>" + cdmDataType() + "</gco:CharacterString>\n" +
"              </gmd:code>\n" +
"            </gmd:MD_Identifier>\n" +
"          </gmd:aggregateDataSetIdentifier>\n" +
"          <gmd:associationType>\n" +
"            <gmd:DS_AssociationTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" " +
               "codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n" +
"          </gmd:associationType>\n" +
"          <gmd:initiativeType>\n" +
"            <gmd:DS_InitiativeTypeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" " +
               "codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n" +
"          </gmd:initiativeType>\n" +
"        </gmd:MD_AggregateInformation>\n" +
"      </gmd:aggregationInfo>\n");
}

    //language
writer.write(
"      <gmd:language>\n" +
"        <gco:CharacterString>eng</gco:CharacterString>\n" +
"      </gmd:language>\n" +

    //see category list http://www.schemacentral.com/sc/niem21/e-gmd_MD_TopicCategoryCode.html
    //options: climatologyMeteorologyAtmosphere(ncIso has), geoscientificInformation, elevation, oceans, ...???
    //I chose geoscientificInformation because it is the most general. Will always be true.
"      <gmd:topicCategory>\n" +   
"        <gmd:MD_TopicCategoryCode>geoscientificInformation</gmd:MD_TopicCategoryCode>\n" +
"      </gmd:topicCategory>\n" +
    //extent
"      <gmd:extent>\n");

} else if (ii == iiOPeNDAP) {
    writer.write(
"      <srv:serviceType>\n" +
"        <gco:LocalName>ERDDAP OPeNDAP</gco:LocalName>\n" +
"      </srv:serviceType>\n" +
    //extent
"      <srv:extent>\n");

} else if (ii == iiWMS) {
    writer.write(
"      <srv:serviceType>\n" +
"        <gco:LocalName>Open Geospatial Consortium Web Map Service (WMS)</gco:LocalName>\n" +
"      </srv:serviceType>\n" +
    //extent
"      <srv:extent>\n");
}

//all ii
writer.write(
"        <gmd:EX_Extent" +
    (ii == iiDataIdentification? " id=\"boundingExtent\"" : "") +
    ">\n" +
"          <gmd:geographicElement>\n" +
"            <gmd:EX_GeographicBoundingBox" +
    (ii == iiDataIdentification? " id=\"boundingGeographicBoundingBox\"" : "") +
    ">\n" +
"              <gmd:extentTypeCode>\n" +
"                <gco:Boolean>1</gco:Boolean>\n" +
"              </gmd:extentTypeCode>\n" +

(Float.isNaN(lonMin)?  //differs from EDDGrid since it may be NaN
"              <gmd:westBoundLongitude gco:nilReason=\"unknown\" />\n" :
"              <gmd:westBoundLongitude>\n" +
"                <gco:Decimal>" + lonMin + "</gco:Decimal>\n" +
"              </gmd:westBoundLongitude>\n") +

(Float.isNaN(lonMax)?
"              <gmd:eastBoundLongitude gco:nilReason=\"unknown\" />\n" :
"              <gmd:eastBoundLongitude>\n" +
"                <gco:Decimal>" + lonMax + "</gco:Decimal>\n" +
"              </gmd:eastBoundLongitude>\n") +

(Float.isNaN(latMin)?
"              <gmd:southBoundLatitude gco:nilReason=\"unknown\" />\n" :
"              <gmd:southBoundLatitude>\n" +
"                <gco:Decimal>" + latMin + "</gco:Decimal>\n" +
"              </gmd:southBoundLatitude>\n") +

(Float.isNaN(latMax)?
"              <gmd:northBoundLatitude gco:nilReason=\"unknown\" />\n" :
"              <gmd:northBoundLatitude>\n" +
"                <gco:Decimal>" + latMax + "</gco:Decimal>\n" +
"              </gmd:northBoundLatitude>\n") +

"            </gmd:EX_GeographicBoundingBox>\n" +
"          </gmd:geographicElement>\n" +

(minTime.length() == 0? "" : //differs from EDDGrid
"          <gmd:temporalElement>\n" +
"            <gmd:EX_TemporalExtent" +
    (ii == iiDataIdentification? " id=\"boundingTemporalExtent\"" : "") +
    ">\n" +
"              <gmd:extent>\n" +
"                <gml:TimePeriod gml:id=\"" +  //id is required    //ncISO has "d293"
    (ii == iiDataIdentification? "DI"  : 
     ii == iiOPeNDAP?            "OD"  :
     ii == iiWMS?                "WMS" :
    "ERROR") + 
    "_gmdExtent_timePeriod_id\">\n" + 
"                  <gml:description>seconds</gml:description>\n" +
"                  <gml:beginPosition>" + minTime + "</gml:beginPosition>\n" +

(maxTime.length() == 0? 
//now? see https://marinemetadata.org/workshops/mmiworkshop06/materials/track1/sensorml/EXAMPLES/Garmin_GPS_SensorML
"                  <gml:endPosition indeterminatePosition=\"now\" />\n" :
"                  <gml:endPosition>"   + maxTime + "</gml:endPosition>\n") +

"                </gml:TimePeriod>\n" +
"              </gmd:extent>\n" +
"            </gmd:EX_TemporalExtent>\n" +
"          </gmd:temporalElement>\n") +

(Double.isNaN(minVert) || Double.isNaN(maxVert)? "" :  
"          <gmd:verticalElement>\n" +
"            <gmd:EX_VerticalExtent>\n" +
"              <gmd:minimumValue><gco:Real>" + minVert + "</gco:Real></gmd:minimumValue>\n" +
"              <gmd:maximumValue><gco:Real>" + maxVert + "</gco:Real></gmd:maximumValue>\n" +
//!!!needs work.   info is sometimes available e.g., in coastwatch files
//http://www.schemacentral.com/sc/niem21/e-gmd_verticalCRS-1.html
"              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" + //???
"            </gmd:EX_VerticalExtent>\n" +
"          </gmd:verticalElement>\n") +

"        </gmd:EX_Extent>\n");

if (ii == iiDataIdentification) {
    writer.write(
"      </gmd:extent>\n" +
"    </gmd:MD_DataIdentification>\n");
}

if (ii == iiOPeNDAP) {
    writer.write(
"      </srv:extent>\n" +
"      <srv:couplingType>\n" +
"        <srv:SV_CouplingType " +
           "codeList=\"http://www.tc211.org/ISO19139/resources/codeList.xml#SV_CouplingType\" " +
           "codeListValue=\"tight\">tight</srv:SV_CouplingType>\n" +
"      </srv:couplingType>\n" +
"      <srv:containsOperations>\n" +
"        <srv:SV_OperationMetadata>\n" +
"          <srv:operationName>\n" +
"            <gco:CharacterString>OPeNDAPDatasetQueryAndAccess</gco:CharacterString>\n" +
"          </srv:operationName>\n" +
"          <srv:DCP gco:nilReason=\"unknown\"/>\n" +  //??? Distributed Computing Platform  
"          <srv:connectPoint>\n" +
"            <gmd:CI_OnlineResource>\n" +
"              <gmd:linkage>\n" +
"                <gmd:URL>" + XML.encodeAsXML(datasetUrl) + "</gmd:URL>\n" +
"              </gmd:linkage>\n" +
"              <gmd:name>\n" +
"                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" +
"              </gmd:name>\n" +
"              <gmd:description>\n" +
"                <gco:CharacterString>ERDDAP's tabledap OPeNDAP service. Add different extensions " +
                   "(e.g., .html, .das, .dds) for different purposes.</gco:CharacterString>\n" +
"              </gmd:description>\n" +
"              <gmd:function>\n" +
"                <gmd:CI_OnLineFunctionCode " +
                   "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" " +
                   "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n" +
"              </gmd:function>\n" +
"            </gmd:CI_OnlineResource>\n" +
"          </srv:connectPoint>\n" +
"        </srv:SV_OperationMetadata>\n" +
"      </srv:containsOperations>\n" +
"      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n" +
"    </srv:SV_ServiceIdentification>\n");
}

if (ii == iiWMS) {
    writer.write(
"      </srv:extent>\n" +
"      <srv:couplingType>\n" +
"        <srv:SV_CouplingType " +
           "codeList=\"http://www.tc211.org/ISO19139/resources/codeList.xml#SV_CouplingType\" " +
           "codeListValue=\"tight\">tight</srv:SV_CouplingType>\n" +
"      </srv:couplingType>\n" +
"      <srv:containsOperations>\n" +
"        <srv:SV_OperationMetadata>\n" +
"          <srv:operationName>\n" +
"            <gco:CharacterString>GetCapabilities</gco:CharacterString>\n" +
"          </srv:operationName>\n" +
"          <srv:DCP gco:nilReason=\"unknown\"/>\n" +  //Distributed Computing Platform  
"          <srv:connectPoint>\n" +
"            <gmd:CI_OnlineResource>\n" +
"              <gmd:linkage>\n" +
"                <gmd:URL>" + XML.encodeAsXML(wmsUrl + 
                     "?service=WMS&version=1.3.0&request=GetCapabilities") + "</gmd:URL>\n" +
"              </gmd:linkage>\n" +
"              <gmd:name>\n" +
"                <gco:CharacterString>OGC-WMS</gco:CharacterString>\n" +
"              </gmd:name>\n" +
"              <gmd:description>\n" +
"                <gco:CharacterString>Open Geospatial Consortium Web Map Service (WMS)</gco:CharacterString>\n" +
"              </gmd:description>\n" +
"              <gmd:function>\n" +
"                <gmd:CI_OnLineFunctionCode " +
                   "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" " +
                   "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n" +
"              </gmd:function>\n" +
"            </gmd:CI_OnlineResource>\n" +
"          </srv:connectPoint>\n" +
"        </srv:SV_OperationMetadata>\n" +
"      </srv:containsOperations>\n" +
"      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n" +
"    </srv:SV_ServiceIdentification>\n");
}

writer.write(
"  </gmd:identificationInfo>\n");

} //end of ii loop


//contentInfo  (dataVariables)
writer.write(
"  <gmd:contentInfo>\n" +
"    <gmi:MI_CoverageDescription>\n" +
"      <gmd:attributeDescription gco:nilReason=\"unknown\"/>\n" +  //???
       //from http://www.schemacentral.com/sc/niem21/t-gco_CodeListValue_Type.html       
"      <gmd:contentType>\n" +
"        <gmd:MD_CoverageContentTypeCode " +
           "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CoverageContentTypeCode\" " +
           "codeListValue=\"physicalMeasurement\">physicalMeasurement</gmd:MD_CoverageContentTypeCode>\n" +
"      </gmd:contentType>\n");

//dataVariables
for (int v = 0; v < dataVariables.length; v++) {
    EDV edv = dataVariables[v];
    if (edv == lonEdv || edv == latEdv || edv == altEdv || edv == depthEdv)
        continue;
    String tUnits = testMinimalMetadata? null : edv.ucumUnits();
    writer.write(
"      <gmd:dimension>\n" +  //in ncIso, a dimension???
"        <gmd:MD_Band>\n" +
"          <gmd:sequenceIdentifier>\n" +
"            <gco:MemberName>\n" +
"              <gco:aName>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(edv.destinationName()) + "</gco:CharacterString>\n" +
"              </gco:aName>\n" +
"              <gco:attributeType>\n" +
"                <gco:TypeName>\n" +
"                  <gco:aName>\n" +            //e.g., double   (java-style names seem to be okay)
"                    <gco:CharacterString>" + edv.destinationDataType() + "</gco:CharacterString>\n" +
"                  </gco:aName>\n" +
"                </gco:TypeName>\n" +
"              </gco:attributeType>\n" +
"            </gco:MemberName>\n" +
"          </gmd:sequenceIdentifier>\n" +
"          <gmd:descriptor>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(edv.longName()) + "</gco:CharacterString>\n" +
"          </gmd:descriptor>\n" +

    //???I think units is used incorrectly, see
    //http://grepcode.com/file/repo1.maven.org/maven2/org.jvnet.ogc/gml-v_3_2_1-schema/1.0.3/iso/19139/20060504/resources/uom/gmxUom.xml
    //which is really complex for derivedUnits.
    (tUnits == null? "" : 
"          <gmd:units xlink:href=\"http://unitsofmeasure.org/ucum.html#" +
             XML.encodeAsHTMLAttribute(SSR.minimalPercentEncode(edv.ucumUnits())) + "\"/>\n") +

"        </gmd:MD_Band>\n" +
"      </gmd:dimension>\n");
}
writer.write(
"    </gmi:MI_CoverageDescription>\n" +
"  </gmd:contentInfo>\n");

//distibutionInfo: erddap treats distributionInfo same as serviceInfo: use EDStatic.admin...
//   ncISO uses creator
writer.write(
"  <gmd:distributionInfo>\n" +
"    <gmd:MD_Distribution>\n" +
"      <gmd:distributor>\n" +
"        <gmd:MD_Distributor>\n" +
"          <gmd:distributorContact>\n" + //ncIso has "missing"
"            <gmd:CI_ResponsibleParty>\n" +
"              <gmd:individualName>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</gco:CharacterString>\n" +
"              </gmd:individualName>\n" +
"              <gmd:organisationName>\n" +
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</gco:CharacterString>\n" +
"              </gmd:organisationName>\n" +
"              <gmd:contactInfo>\n" +
"                <gmd:CI_Contact>\n" +
"                  <gmd:phone>\n" +
"                    <gmd:CI_Telephone>\n" +
"                      <gmd:voice>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminPhone) + "</gco:CharacterString>\n" +
"                      </gmd:voice>\n" +
"                    </gmd:CI_Telephone>\n" +
"                  </gmd:phone>\n" +
"                  <gmd:address>\n" +
"                    <gmd:CI_Address>\n" +
"                      <gmd:deliveryPoint>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminAddress) + "</gco:CharacterString>\n" +
"                      </gmd:deliveryPoint>\n" +
"                      <gmd:city>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminCity) + "</gco:CharacterString>\n" +
"                      </gmd:city>\n" +
"                      <gmd:administrativeArea>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</gco:CharacterString>\n" +
"                      </gmd:administrativeArea>\n" +
"                      <gmd:postalCode>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</gco:CharacterString>\n" +
"                      </gmd:postalCode>\n" +
"                      <gmd:country>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminCountry) + "</gco:CharacterString>\n" +
"                      </gmd:country>\n" +
"                      <gmd:electronicMailAddress>\n" +
"                        <gco:CharacterString>" + XML.encodeAsXML(EDStatic.adminEmail) + "</gco:CharacterString>\n" +
"                      </gmd:electronicMailAddress>\n" +
"                    </gmd:CI_Address>\n" +
"                  </gmd:address>\n" +
"                </gmd:CI_Contact>\n" +
"              </gmd:contactInfo>\n" +
"              <gmd:role>\n" +   //From list, "distributor" seems best here.
"                <gmd:CI_RoleCode " +
                   "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" " +
                   "codeListValue=\"distributor\">distributor</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:distributorContact>\n" +

//distributorFormats are formats (says Ted Habermann)
"          <gmd:distributorFormat>\n" +
"            <gmd:MD_Format>\n" +
"              <gmd:name>\n" +
"                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" +
"              </gmd:name>\n" + 
"              <gmd:version>\n" +  //ncIso has unknown
"                <gco:CharacterString>" + XML.encodeAsXML(EDStatic.dapVersion) + "</gco:CharacterString>\n" +
"              </gmd:version>\n" +
"            </gmd:MD_Format>\n" +
"          </gmd:distributorFormat>\n" +

//distributorTransferOptions are URLs  (says Ted Habermann)
"          <gmd:distributorTransferOptions>\n" +
"            <gmd:MD_DigitalTransferOptions>\n" +
"              <gmd:onLine>\n" +
"                <gmd:CI_OnlineResource>\n" +
"                  <gmd:linkage>\n" +  
"                    <gmd:URL>" + XML.encodeAsXML(datasetUrl) + ".html</gmd:URL>\n" +
"                  </gmd:linkage>\n" +
"                  <gmd:name>\n" +        
"                    <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" +
"                  </gmd:name>\n" +
"                  <gmd:description>\n" + 
"                    <gco:CharacterString>ERDDAP's version of the OPeNDAP .html web page for this dataset. " +
                       "Specify a subset of the dataset and download the data via OPeNDAP " +
                       "or in many different file types.</gco:CharacterString>\n" +
"                  </gmd:description>\n" +
"                  <gmd:function>\n" +
"                    <gmd:CI_OnLineFunctionCode " +
                       "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" " +
                       "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n" +
"                  </gmd:function>\n" +
"                </gmd:CI_OnlineResource>\n" +
"              </gmd:onLine>\n" +
"            </gmd:MD_DigitalTransferOptions>\n" +
"          </gmd:distributorTransferOptions>\n" +

(accessibleViaMAG().length() > 0? "" :   //from Ted Habermann's ns01agg.xml
"          <gmd:distributorTransferOptions>\n" +
"            <gmd:MD_DigitalTransferOptions>\n" +
"              <gmd:onLine>\n" +
"                <gmd:CI_OnlineResource>\n" +
"                  <gmd:linkage>\n" +  
"                    <gmd:URL>" + XML.encodeAsXML(datasetUrl) + ".graph</gmd:URL>\n" +
"                  </gmd:linkage>\n" +
"                  <gmd:name>\n" +        
"                    <gco:CharacterString>Viewer Information</gco:CharacterString>\n" +
"                  </gmd:name>\n" +
"                  <gmd:description>\n" + 
"                    <gco:CharacterString>ERDDAP's Make-A-Graph .html web page for this dataset. " +
                       "Create an image with a map or graph of a subset of the data.</gco:CharacterString>\n" +
"                  </gmd:description>\n" +
"                  <gmd:function>\n" +
"                    <gmd:CI_OnLineFunctionCode " +
                       "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" " +
                       "codeListValue=\"mapDigital\">mapDigital</gmd:CI_OnLineFunctionCode>\n" +
"                  </gmd:function>\n" +
"                </gmd:CI_OnlineResource>\n" +
"              </gmd:onLine>\n" +
"            </gmd:MD_DigitalTransferOptions>\n" +
"          </gmd:distributorTransferOptions>\n") +

"        </gmd:MD_Distributor>\n" +
"      </gmd:distributor>\n" +
"    </gmd:MD_Distribution>\n" +
"  </gmd:distributionInfo>\n");

//quality
if (history != null) 
    writer.write(
"  <gmd:dataQualityInfo>\n" +
"    <gmd:DQ_DataQuality>\n" +
"      <gmd:scope>\n" +
"        <gmd:DQ_Scope>\n" +
"          <gmd:level>\n" +
"            <gmd:MD_ScopeCode " +
               "codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" " +
               "codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n" +
"          </gmd:level>\n" +
"        </gmd:DQ_Scope>\n" +
"      </gmd:scope>\n" +
"      <gmd:lineage>\n" +
"        <gmd:LI_Lineage>\n" +
"          <gmd:statement>\n" +
"            <gco:CharacterString>" + XML.encodeAsXML(history) + "</gco:CharacterString>\n" +
"          </gmd:statement>\n" +
"        </gmd:LI_Lineage>\n" +
"      </gmd:lineage>\n" +
"    </gmd:DQ_DataQuality>\n" +
"  </gmd:dataQualityInfo>\n");

//metadata
writer.write(
"  <gmd:metadataMaintenance>\n" +
"    <gmd:MD_MaintenanceInformation>\n" +
"      <gmd:maintenanceAndUpdateFrequency gco:nilReason=\"unknown\"/>\n" +
"      <gmd:maintenanceNote>\n" +
"        <gco:CharacterString>This record was created from dataset metadata by ERDDAP Version " + 
          EDStatic.erddapVersion + "</gco:CharacterString>\n" +
"      </gmd:maintenanceNote>\n" +
"    </gmd:MD_MaintenanceInformation>\n" +
"  </gmd:metadataMaintenance>\n" +
"</gmi:MI_Metadata>\n");

    }


    /**
     * Test SOS server using cwwcNDBCMet.
     */
    public static void testSosNdbcMet() throws Throwable {
        String2.log("\n*** EDDTable.testSosNdbcMet()");
        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 
        String dir = EDStatic.fullTestCacheDirectory;
        String sosQuery, fileName, results, expected;
        java.io.StringWriter writer;
        ByteArrayOutputStream baos;
        OutputStreamSourceSimple osss;
        String fullPhenomenaDictionaryUrl = EDStatic.erddapUrl + "/sos/" + eddTable.datasetID + 
            "/" + sosPhenomenaDictionaryUrl; 


        //GetCapabilities
        String2.log("\n+++ GetCapabilities");
        writer = new java.io.StringWriter();
        eddTable.sosGetCapabilities(writer, null);
        results = writer.toString();
        expected = 
"<?xml version=\"1.0\"?>\n" +
"<Capabilities\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns=\"http://www.opengis.net/sos/1.0\"\n" +
"  xmlns:sos=\"http://www.opengis.net/sos/1.0\"\n" +
"  xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n" +
"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" +
"  xmlns:tml=\"http://www.opengis.net/tml\"\n" +
"  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
"  xmlns:myorg=\"http://www.myorg.org/features\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/sos/1.0 http://schemas.opengis.net/sos/1.0.0/sosAll.xsd\"\n" +
"  version=\"1.0.0\">\n" +
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <ows:ServiceIdentification>\n" +
"    <ows:Title>SOS for NDBC Standard Meteorological Buoy Data</ows:Title>\n" +
"    <ows:Abstract>The National Data Buoy Center (NDBC) distributes meteorological data from \n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather \n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters \n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the \n" +
"Bering Sea to the South Pacific. NDBC&#39;s moored buoys measure and transmit \n" +
"barometric pressure; wind direction, speed, and gust; air and sea \n" +
"temperature; and wave energy spectra from which significant wave height, \n" +
"dominant wave period, and average wave period are derived. Even the \n" +
"direction of wave propagation is measured on many moored buoys. \n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch, West Coast Node.\n" +
"\n" +
"This dataset has both historical data (quality controlled, before 2009-12-\n" +
"01T00:00:00) and near real time data (less quality controlled, from 2009-12-\n" +
"01T00:00:00 on).</ows:Abstract>\n" +
"    <ows:Keywords>\n" +
"      <ows:Keyword>EARTH SCIENCE</ows:Keyword>\n" +
"      <ows:Keyword>Oceans</ows:Keyword>\n" +
"    </ows:Keywords>\n" +
"    <ows:ServiceType codeSpace=\"http://opengeospatial.net\">OGC:SOS</ows:ServiceType>\n" +
"    <ows:ServiceTypeVersion>1.0.0</ows:ServiceTypeVersion>\n" +
"    <ows:Fees>NONE</ows:Fees>\n" +
"    <ows:AccessConstraints>NONE</ows:AccessConstraints>\n" +
"  </ows:ServiceIdentification>\n" +
"  <ows:ServiceProvider>\n" +
"    <ows:ProviderName>NOAA Environmental Research Division</ows:ProviderName>\n" +
"    <ows:ProviderSite xlink:href=\"http://127.0.0.1:8080/cwexperimental\"/>\n" +
"    <ows:ServiceContact>\n" +
"      <ows:IndividualName>Bob Simons</ows:IndividualName>\n" +
"      <ows:ContactInfo>\n" +
"        <ows:Phone>\n" +
"          <ows:Voice>831-658-3205</ows:Voice>\n" +
"        </ows:Phone>\n" +
"        <ows:Address>\n" +
"          <ows:DeliveryPoint>1352 Lighthouse Ave.</ows:DeliveryPoint>\n" +
"          <ows:City>Pacific Grove</ows:City>\n" +
"          <ows:AdministrativeArea>CA</ows:AdministrativeArea>\n" +
"          <ows:PostalCode>93950</ows:PostalCode>\n" +
"          <ows:Country>USA</ows:Country>\n" +
"          <ows:ElectronicMailAddress>bob.simons@noaa.gov</ows:ElectronicMailAddress>\n" +
"        </ows:Address>\n" +
"      </ows:ContactInfo>\n" +
"    </ows:ServiceContact>\n" +
"  </ows:ServiceProvider>\n" +
"  <ows:OperationsMetadata>\n" +
"    <ows:Operation name=\"GetCapabilities\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/server\"/>\n" +
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"Sections\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>ServiceIdentification</ows:Value>\n" +
"          <ows:Value>ServiceProvider</ows:Value>\n" +
"          <ows:Value>OperationsMetadata</ows:Value>\n" +
"          <ows:Value>Contents</ows:Value>\n" +
"          <ows:Value>All</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +
"    <ows:Operation name=\"GetObservation\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/server\"/>\n" +
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"observedProperty\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>cwwcNDBCMet</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +
"    <ows:Operation name=\"DescribeSensor\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/server\"/>\n" +
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"outputFormat\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>text/xml;subtype=\"sensorML/1.0.0\"</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +
"    <ows:Parameter name=\"service\">\n" +
"      <ows:AllowedValues>\n" +
"        <ows:Value>SOS</ows:Value>\n" +
"      </ows:AllowedValues>\n" +
"    </ows:Parameter>\n" +
"    <ows:Parameter name=\"version\">\n" +
"      <ows:AllowedValues>\n" +
"        <ows:Value>1.0.0</ows:Value>\n" +
"      </ows:AllowedValues>\n" +
"    </ows:Parameter>\n" +
"  </ows:OperationsMetadata>\n" +
"  <Contents>\n" +
"    <ObservationOfferingList>\n" +
"      <ObservationOffering gml:id=\"network-cwwcNDBCMet\">\n" +
"        <gml:description>network cwwcNDBCMet</gml:description>\n" +
"        <gml:name>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</gml:name>\n" +
"        <gml:srsName>urn:ogc:def:crs:epsg::4326</gml:srsName>\n" +
"        <gml:boundedBy>\n" +
"          <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"            <gml:lowerCorner>-27.7 -177.58</gml:lowerCorner>\n" +
"            <gml:upperCorner>70.4 179.02</gml:upperCorner>\n" +
"          </gml:Envelope>\n" +
"        </gml:boundedBy>\n" +
"        <time>\n" +
"          <gml:TimePeriod>\n" +
"            <gml:beginPosition>1970-02-26T20:00:00Z</gml:beginPosition>\n" +
"            <gml:endPosition>2010-01-08T20:00:00Z</gml:endPosition>\n" +
"          </gml:TimePeriod>\n" +
"        </time>\n" +
"        <procedure xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:23020:\"/>\n" +
"        <procedure xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:31201:\"/>\n" +
"        <procedure xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:32012:\"/>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, results.substring(0, 7000));

        //String2.log("\n...\n");
        //String2.log(results.substring(60000, 67000));        
        //String2.log("\n...\n");
        String2.log(results.substring(results.length() - 5000));        
        expected = 
//"      </ObservationOffering>\n" +
"      <ObservationOffering gml:id=\"Station-YRSV2\">\n" +
"        <gml:description>Station YRSV2</gml:description>\n" +
"        <gml:name>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:YRSV2:</gml:name>\n" +
"        <gml:srsName>urn:ogc:def:crs:epsg::4326</gml:srsName>\n" +
"        <gml:boundedBy>\n" +
"          <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"            <gml:lowerCorner>37.4142 -76.7125</gml:lowerCorner>\n" +
"            <gml:upperCorner>37.4142 -76.7125</gml:upperCorner>\n" +
"          </gml:Envelope>\n" +
"        </gml:boundedBy>\n" +
"        <time>\n" +
"          <gml:TimePeriod>\n" +
"            <gml:beginPosition>2007-07-10T08:00:00Z</gml:beginPosition>\n" +
"            <gml:endPosition>2010-01-08T11:00:00Z</gml:endPosition>\n" +
"          </gml:TimePeriod>\n" +
"        </time>\n" +
"        <procedure xlink:href=\"urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:YRSV2:cwwcNDBCMet\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wd\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspd\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#gst\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wvht\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dpd\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#apd\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#mwd\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#bar\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#atmp\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wtmp\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dewp\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#vis\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#ptdy\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#tide\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspu\"/>\n" +
"        <observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspv\"/>\n" +
"        <featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
"        <responseFormat>text/xml;schema=&quot;ioos/0.6.1&quot;</responseFormat>\n" +
"        <responseFormat>application/ioos+xml;version=0.6.1</responseFormat>\n" +
"        <responseFormat>application/com-xml</responseFormat>\n" +
"        <responseFormat>text/csv</responseFormat>\n" +
"        <responseFormat>application/json;subtype=geojson</responseFormat>\n" +
"        <responseFormat>text/html</responseFormat>\n" +
"        <responseFormat>application/json</responseFormat>\n" +
"        <responseFormat>application/x-matlab-mat</responseFormat>\n" +
"        <responseFormat>application/x-netcdf</responseFormat>\n" +
"        <responseFormat>text/tab-separated-values</responseFormat>\n" +
"        <responseFormat>application/xhtml+xml</responseFormat>\n" +
"        <responseFormat>application/vnd.google-earth.kml+xml</responseFormat>\n" +
"        <responseFormat>application/pdf</responseFormat>\n" +
"        <responseFormat>image/png</responseFormat>\n" +
"        <resultModel>om:Observation</resultModel>\n" +
//"        <responseMode>inline</responseMode>\n" +
//"        <responseMode>out-of-band</responseMode>\n" +
"      </ObservationOffering>\n" +
"    </ObservationOfferingList>\n" +
"  </Contents>\n" +
"</Capabilities>\n";
        int po = results.indexOf(expected.substring(0, 50));
        if (po < 0)
            String2.log(results);
        Test.ensureEqual(results.substring(po), expected, 
            results.substring(results.length() - 5000));

        //phenomenaDictionary
        String2.log("\n+++ phenomenaDictionary");
        writer = new java.io.StringWriter();        
        eddTable.sosPhenomenaDictionary(writer);
        results = writer.toString();
        //String2.log(results);        
        expected =
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<gml:Dictionary gml:id=\"PhenomenaDictionary0.6.1\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.2\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/swe/1.0.2 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/OGC/sweCommon/1.0.2/phenomenon.xsd http://www.opengis.net/gml/3.2 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/OGC/gml/3.2.1/gml.xsd\"\n" +
"  >\n" +
"  <gml:description>Dictionary of observed phenomena for cwwcNDBCMet.</gml:description>\n" +
"  <gml:identifier codeSpace=\"urn:ioos:phenomena:1.0.0.127.cwwcNDBCMet\">PhenomenaDictionary</gml:identifier>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"wd\">\n" +
"      <gml:description>Wind Direction</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">wind_from_direction</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"wspd\">\n" +
"      <gml:description>Wind Speed</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">wind_speed</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"gst\">\n" +
"      <gml:description>Wind Gust Speed</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">wind_speed_of_gust</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"wvht\">\n" +
"      <gml:description>Wave Height</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_significant_height</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"dpd\">\n" +
"      <gml:description>Wave Period, Dominant</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_period</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"apd\">\n" +
"      <gml:description>Wave Period, Average</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_period</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"mwd\">\n" +
"      <gml:description>Wave Direction</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_to_direction</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"bar\">\n" +
"      <gml:description>Air Pressure</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">air_pressure_at_sea_level</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"atmp\">\n" +
"      <gml:description>Air Temperature</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">air_temperature</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"wtmp\">\n" +
"      <gml:description>SST</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_temperature</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"dewp\">\n" +
"      <gml:description>Dewpoint Temperature</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">dew_point_temperature</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"vis\">\n" +
"      <gml:description>Station Visibility</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">visibility_in_air</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"ptdy\">\n" +
"      <gml:description>Pressure Tendency</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">tendency_of_air_pressure</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"tide\">\n" +
"      <gml:description>Water Level</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">surface_altitude</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"wspu\">\n" +
"      <gml:description>Wind Speed, Zonal</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">eastward_wind</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:Phenomenon gml:id=\"wspv\">\n" +
"      <gml:description>Wind Speed, Meridional</gml:description>\n" +
"      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">northward_wind</gml:identifier>\n" +
"    </swe:Phenomenon>\n" +
"  </gml:definitionMember>\n" +
"  <gml:definitionMember >\n" +
"    <swe:CompositePhenomenon gml:id=\"cwwcNDBCMet\" dimension=\"16\">\n" +
"      <gml:description>NDBC Standard Meteorological Buoy Data</gml:description>\n" +
"      <gml:identifier codeSpace=\"urn:ioos:phenomena:1.0.0.127.cwwcNDBCMet\">cwwcNDBCMet</gml:identifier>\n" +
"      <swe:component xlink:href=\"#wd\"/>\n" +
"      <swe:component xlink:href=\"#wspd\"/>\n" +
"      <swe:component xlink:href=\"#gst\"/>\n" +
"      <swe:component xlink:href=\"#wvht\"/>\n" +
"      <swe:component xlink:href=\"#dpd\"/>\n" +
"      <swe:component xlink:href=\"#apd\"/>\n" +
"      <swe:component xlink:href=\"#mwd\"/>\n" +
"      <swe:component xlink:href=\"#bar\"/>\n" +
"      <swe:component xlink:href=\"#atmp\"/>\n" +
"      <swe:component xlink:href=\"#wtmp\"/>\n" +
"      <swe:component xlink:href=\"#dewp\"/>\n" +
"      <swe:component xlink:href=\"#vis\"/>\n" +
"      <swe:component xlink:href=\"#ptdy\"/>\n" +
"      <swe:component xlink:href=\"#tide\"/>\n" +
"      <swe:component xlink:href=\"#wspu\"/>\n" +
"      <swe:component xlink:href=\"#wspv\"/>\n" +
"    </swe:CompositePhenomenon>\n" +
"  </gml:definitionMember>\n" +
"</gml:Dictionary>\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //DescribeSensor   all
        //http://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor
        //  &service=SOS&version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
        //  &sensorID=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0
        //stored as /programs/sos/ndbcSosCurrentsDescribeSensor90810.xml
        String2.log("\n+++ DescribeSensor all");
        writer = new java.io.StringWriter();
        eddTable.sosDescribeSensor(null, eddTable.datasetID, writer);
        results = writer.toString();
        //String2.log(results);     
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<sml:SensorML\n" +
"  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n" +
"  version=\"1.0.1\"\n" +
"  >\n" +
"  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n" +
"  <sml:member>\n" +
"    <sml:System gml:id=\"network-cwwcNDBCMet\">\n" +
"      <gml:description>NDBC Standard Meteorological Buoy Data, network-cwwcNDBCMet</gml:description>\n" +
"      <sml:keywords>\n" +
"        <sml:KeywordList>\n" +
"          <sml:keyword>EARTH SCIENCE</sml:keyword>\n" +
"          <sml:keyword>Oceans</sml:keyword>\n" +
"        </sml:KeywordList>\n" +
"      </sml:keywords>\n" +
"\n" +
"      <sml:identification>\n" +
"        <sml:IdentifierList>\n" +
"          <sml:identifier name=\"Station ID\">\n" +
"            <sml:Term definition=\"urn:ioos:identifier:NOAA:stationID:\">\n" +
"              <sml:codeSpace xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"              <sml:value>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"          <sml:identifier name=\"Short Name\">\n" +
"            <sml:Term definition=\"urn:ogc:def:identifier:OGC:shortName\">\n" +
"              <sml:value>cwwcNDBCMet</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"        </sml:IdentifierList>\n" +
"      </sml:identification>\n" +
"\n" +
"      <sml:classification>\n" +
"        <sml:ClassifierList>\n" +
"          <sml:classifier name=\"System Type Identifier\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:NOAA:systemTypeID\">\n" +
"              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n" +
"              <sml:value>Platform</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:classifier>\n" +
"        </sml:ClassifierList>\n" +
"      </sml:classification>\n" +
"\n" +
"      <sml:validTime>\n" +
"        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n" +
"          <gml:beginPosition>1970-02-26T20:00:00Z</gml:beginPosition>\n" +
"          <gml:endPosition>2010-01-08T20:00:00Z</gml:endPosition>\n" +
"        </gml:TimePeriod>\n" +
"      </sml:validTime>\n" +
"\n" +
"      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n" +
"        <sml:ResponsibleParty>\n" +
"          <sml:individualName>Bob Simons</sml:individualName>\n" +
"          <sml:organizationName>NOAA Environmental Research Division</sml:organizationName>\n" +
"          <sml:contactInfo>\n" +
"            <sml:phone>\n" +
"              <sml:voice>831-658-3205</sml:voice>\n" +
"            </sml:phone>\n" +
"            <sml:address>\n" +
"              <sml:deliveryPoint>1352 Lighthouse Ave.</sml:deliveryPoint>\n" +
"              <sml:city>Pacific Grove</sml:city>\n" +
"              <sml:administrativeArea>CA</sml:administrativeArea>\n" +
"              <sml:postalCode>93950</sml:postalCode>\n" +
"              <sml:country>USA</sml:country>\n" +
"              <sml:electronicMailAddress>bob.simons@noaa.gov</sml:electronicMailAddress>\n" +
"            </sml:address>\n" +
"            <sml:onlineResource xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"          </sml:contactInfo>\n" +
"        </sml:ResponsibleParty>\n" +
"      </sml:contact>\n" +
"\n" +
"      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n" +
"        <sml:Document>\n" +
"          <gml:description>Web page with background information from the source of this dataset</gml:description>\n" +
"          <sml:format>text/html</sml:format>\n" +
"          <sml:onlineResource xlink:href=\"http://www.ndbc.noaa.gov/\" />\n" +
"        </sml:Document>\n" +
"      </sml:documentation>\n" +
"\n" +
"      <sml:components>\n" +
"        <sml:ComponentList>\n" +
"          <sml:component name=\"wd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wd\">\n" +
"              <gml:description>network-cwwcNDBCMet-wd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wspd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wspd\">\n" +
"              <gml:description>network-cwwcNDBCMet-wspd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wspd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wspd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"gst Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-gst\">\n" +
"              <gml:description>network-cwwcNDBCMet-gst Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:gst\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"gst\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wvht Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wvht\">\n" +
"              <gml:description>network-cwwcNDBCMet-wvht Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wvht\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wvht\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"dpd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-dpd\">\n" +
"              <gml:description>network-cwwcNDBCMet-dpd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:dpd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"dpd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"apd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-apd\">\n" +
"              <gml:description>network-cwwcNDBCMet-apd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:apd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"apd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"mwd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-mwd\">\n" +
"              <gml:description>network-cwwcNDBCMet-mwd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:mwd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"mwd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"bar Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-bar\">\n" +
"              <gml:description>network-cwwcNDBCMet-bar Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:bar\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"bar\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"atmp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-atmp\">\n" +
"              <gml:description>network-cwwcNDBCMet-atmp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:atmp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"atmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wtmp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wtmp\">\n" +
"              <gml:description>network-cwwcNDBCMet-wtmp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wtmp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wtmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"dewp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-dewp\">\n" +
"              <gml:description>network-cwwcNDBCMet-dewp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:def:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:dewp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"dewp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"vis Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-vis\">\n" +
"              <gml:description>network-cwwcNDBCMet-vis Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:vis\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"vis\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n" +
"                      <swe:uom code=\"km\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"ptdy Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-ptdy\">\n" +
"              <gml:description>network-cwwcNDBCMet-ptdy Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:ptdy\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"ptdy\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"tide Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-tide\">\n" +
"              <gml:description>network-cwwcNDBCMet-tide Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:tide\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"tide\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wspu Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wspu\">\n" +
"              <gml:description>network-cwwcNDBCMet-wspu Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wspu\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wspu\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wspv Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wspv\">\n" +
"              <gml:description>network-cwwcNDBCMet-wspv Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wspv\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wspv\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"cwwcNDBCMet Instrument\">\n" +
"            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-cwwcNDBCMet\">\n" +
"              <gml:description>network-cwwcNDBCMet Platform</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:cwwcNDBCMet\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wspd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"gst\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wvht\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"dpd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"apd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"mwd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"bar\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"atmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wtmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"dewp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"vis\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n" +
"                      <swe:uom code=\"km\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"ptdy\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"tide\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wspu\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wspv\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"        </sml:ComponentList>\n" +
"      </sml:components>\n" +
"    </sml:System>\n" +
"  </sml:member>\n" +
"</sml:SensorML>\n";
        Test.ensureEqual(results, expected, "results=\n"+results);

        //DescribeSensor   41004
        String2.log("\n+++ DescribeSensor 41004");
        writer = new java.io.StringWriter();
        eddTable.sosDescribeSensor(null, "41004", writer);
        results = writer.toString();
        //String2.log(results);     
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<sml:SensorML\n" +
"  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n" +
"  version=\"1.0.1\"\n" +
"  >\n" +
"  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n" +
"  <sml:member>\n" +
"    <sml:System gml:id=\"Station-41004\">\n" +
"      <gml:description>NDBC Standard Meteorological Buoy Data, Station-41004</gml:description>\n" +
"      <sml:keywords>\n" +
"        <sml:KeywordList>\n" +
"          <sml:keyword>EARTH SCIENCE</sml:keyword>\n" +
"          <sml:keyword>Oceans</sml:keyword>\n" +
"        </sml:KeywordList>\n" +
"      </sml:keywords>\n" +
"\n" +
"      <sml:identification>\n" +
"        <sml:IdentifierList>\n" +
"          <sml:identifier name=\"Station ID\">\n" +
"            <sml:Term definition=\"urn:ioos:identifier:url:stationID\">\n" +
"              <sml:codeSpace xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"              <sml:value>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"          <sml:identifier name=\"Short Name\">\n" +
"            <sml:Term definition=\"urn:ogc:def:identifier:OGC:shortName\">\n" +
"              <sml:value>41004</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"        </sml:IdentifierList>\n" +
"      </sml:identification>\n" +
"\n" +
"      <sml:classification>\n" +
"        <sml:ClassifierList>\n" +
"          <sml:classifier name=\"Parent Network\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:url:parentNetwork\">\n" +
"              <sml:codeSpace xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"              <sml:value>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:classifier>\n" +
"          <sml:classifier name=\"System Type Identifier\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:URL:systemTypeID\">\n" +
"              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n" +
"              <sml:value>Platform</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:classifier>\n" +
"        </sml:ClassifierList>\n" +
"      </sml:classification>\n" +
"\n" +
"      <sml:validTime>\n" +
"        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n" +
"          <gml:beginPosition>1978-06-27T13:00:00Z</gml:beginPosition>\n" +
"          <gml:endPosition>2010-01-08T19:00:00Z</gml:endPosition>\n" +
"        </gml:TimePeriod>\n" +
"      </sml:validTime>\n" +
"\n" +
"      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n" +
"        <sml:ResponsibleParty>\n" +
"          <sml:individualName>Bob Simons</sml:individualName>\n" +
"          <sml:organizationName>NOAA Environmental Research Division</sml:organizationName>\n" +
"          <sml:contactInfo>\n" +
"            <sml:phone>\n" +
"              <sml:voice>831-658-3205</sml:voice>\n" +
"            </sml:phone>\n" +
"            <sml:address>\n" +
"              <sml:deliveryPoint>1352 Lighthouse Ave.</sml:deliveryPoint>\n" +
"              <sml:city>Pacific Grove</sml:city>\n" +
"              <sml:administrativeArea>CA</sml:administrativeArea>\n" +
"              <sml:postalCode>93950</sml:postalCode>\n" +
"              <sml:country>USA</sml:country>\n" +
"              <sml:electronicMailAddress>bob.simons@noaa.gov</sml:electronicMailAddress>\n" +
"            </sml:address>\n" +
"            <sml:onlineResource xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"          </sml:contactInfo>\n" +
"        </sml:ResponsibleParty>\n" +
"      </sml:contact>\n" +
"\n" +
"      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n" +
"        <sml:Document>\n" +
"          <gml:description>Web page with background information from the source of this dataset</gml:description>\n" +
"          <sml:format>text/html</sml:format>\n" +
"          <sml:onlineResource xlink:href=\"http://www.ndbc.noaa.gov/\" />\n" +
"        </sml:Document>\n" +
"      </sml:documentation>\n" +
"\n" +
"      <sml:location>\n" +
"        <gml:Point srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"Station_LatLon\">\n" +
"          <gml:coordinates>32.5 -79.09</gml:coordinates>\n" +
"        </gml:Point>\n" +
"      </sml:location>\n" +
"\n" +
"      <sml:components>\n" +
"        <sml:ComponentList>\n" +
"          <sml:component name=\"wd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wd\">\n" +
"              <gml:description>Station-41004-wd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wspd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wspd\">\n" +
"              <gml:description>Station-41004-wspd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wspd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wspd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"gst Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-gst\">\n" +
"              <gml:description>Station-41004-gst Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:gst\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"gst\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wvht Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wvht\">\n" +
"              <gml:description>Station-41004-wvht Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wvht\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wvht\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"dpd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-dpd\">\n" +
"              <gml:description>Station-41004-dpd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:dpd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"dpd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"apd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-apd\">\n" +
"              <gml:description>Station-41004-apd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:apd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"apd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"mwd Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-mwd\">\n" +
"              <gml:description>Station-41004-mwd Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:mwd\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"mwd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"bar Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-bar\">\n" +
"              <gml:description>Station-41004-bar Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:bar\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"bar\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"atmp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-atmp\">\n" +
"              <gml:description>Station-41004-atmp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:atmp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"atmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wtmp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wtmp\">\n" +
"              <gml:description>Station-41004-wtmp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wtmp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wtmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"dewp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-dewp\">\n" +
"              <gml:description>Station-41004-dewp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:dewp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"dewp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"vis Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-vis\">\n" +
"              <gml:description>Station-41004-vis Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:vis\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"vis\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n" +
"                      <swe:uom code=\"km\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"ptdy Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-ptdy\">\n" +
"              <gml:description>Station-41004-ptdy Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:ptdy\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"ptdy\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"tide Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-tide\">\n" +
"              <gml:description>Station-41004-tide Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:tide\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"tide\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wspu Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wspu\">\n" +
"              <gml:description>Station-41004-wspu Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wspu\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wspu\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"wspv Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wspv\">\n" +
"              <gml:description>Station-41004-wspv Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wspv\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wspv\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"          <sml:component name=\"cwwcNDBCMet Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-cwwcNDBCMet\">\n" +
"              <gml:description>Station-41004 Platform</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:cwwcNDBCMet\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wspd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"gst\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wvht\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"dpd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"apd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n" +
"                      <swe:uom code=\"s\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"mwd\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n" +
"                      <swe:uom code=\"degrees_true\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"bar\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"atmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wtmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"dewp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"vis\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n" +
"                      <swe:uom code=\"km\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"ptdy\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n" +
"                      <swe:uom code=\"hPa\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"tide\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n" +
"                      <swe:uom code=\"m\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wspu\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                  <sml:output name=\"wspv\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n" +
"                      <swe:uom code=\"m s-1\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"        </sml:ComponentList>\n" +
"      </sml:components>\n" +
"    </sml:System>\n" +
"  </sml:member>\n" +
"</sml:SensorML>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n"+results);

        //DescribeSensor   wtmp
        //http://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor
        //  &service=SOS&version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
        //  &sensorID=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0
        //stored as /programs/sos/ndbcSosCurrentsDescribeSensor90810.xml
        String2.log("\n+++ DescribeSensor 41004:wtmp");
        writer = new java.io.StringWriter();
        eddTable.sosDescribeSensor(null, "41004", writer);
        results = writer.toString();
        String2.log(results);     
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<sml:SensorML\n" +
"  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n" +
"  version=\"1.0.1\"\n" +
"  >\n" +
"  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n" +
"  <sml:member>\n" +
"    <sml:System gml:id=\"Station-41004-wtmp\">\n" +
"      <gml:description>NDBC Standard Meteorological Buoy Data, Station-41004-wtmp</gml:description>\n" +
"      <sml:keywords>\n" +
"        <sml:KeywordList>\n" +
"          <sml:keyword>EARTH SCIENCE</sml:keyword>\n" +
"          <sml:keyword>Oceans</sml:keyword>\n" +
"        </sml:KeywordList>\n" +
"      </sml:keywords>\n" +
"\n" +
"      <sml:identification>\n" +
"        <sml:IdentifierList>\n" +
"          <sml:identifier name=\"Station ID\">\n" +
"            <sml:Term definition=\"urn:ioos:identifier:URL:stationID\">\n" +
"              <sml:codeSpace xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"              <sml:value>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"          <sml:identifier name=\"Short Name\">\n" +
"            <sml:Term definition=\"urn:ogc:def:identifier:OGC:shortName\">\n" +
"              <sml:value>41004:wtmp</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:identifier>\n" +
"        </sml:IdentifierList>\n" +
"      </sml:identification>\n" +
"\n" +
"      <sml:classification>\n" +
"        <sml:ClassifierList>\n" +
"          <sml:classifier name=\"Parent Network\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:URL:parentNetwork\">\n" +
"              <sml:codeSpace xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"              <sml:value>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:classifier>\n" +
"          <sml:classifier name=\"System Type Identifier\">\n" +
"            <sml:Term definition=\"urn:ioos:classifier:URL:systemTypeID\">\n" +
"              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n" +
"              <sml:value>Platform</sml:value>\n" +
"            </sml:Term>\n" +
"          </sml:classifier>\n" +
"        </sml:ClassifierList>\n" +
"      </sml:classification>\n" +
"\n" +
"      <sml:validTime>\n" +
"        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n" +
"          <gml:beginPosition>1978-06-27T13:00:00Z</gml:beginPosition>\n" +
"          <gml:endPosition>2010-01-08T19:00:00Z</gml:endPosition>\n" +
"        </gml:TimePeriod>\n" +
"      </sml:validTime>\n" +
"\n" +
"      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n" +
"        <sml:ResponsibleParty>\n" +
"          <sml:individualName>Bob Simons</sml:individualName>\n" +
"          <sml:organizationName>NOAA Environmental Research Division</sml:organizationName>\n" +
"          <sml:contactInfo>\n" +
"            <sml:phone>\n" +
"              <sml:voice>831-658-3205</sml:voice>\n" +
"            </sml:phone>\n" +
"            <sml:address>\n" +
"              <sml:deliveryPoint>1352 Lighthouse Ave.</sml:deliveryPoint>\n" +
"              <sml:city>Pacific Grove</sml:city>\n" +
"              <sml:administrativeArea>CA</sml:administrativeArea>\n" +
"              <sml:postalCode>93950</sml:postalCode>\n" +
"              <sml:country>USA</sml:country>\n" +
"              <sml:electronicMailAddress>bob.simons@noaa.gov</sml:electronicMailAddress>\n" +
"            </sml:address>\n" +
"            <sml:onlineResource xlink:href=\"http://127.0.0.1:8080/cwexperimental\" />\n" +
"          </sml:contactInfo>\n" +
"        </sml:ResponsibleParty>\n" +
"      </sml:contact>\n" +
"\n" +
"      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n" +
"        <sml:Document>\n" +
"          <gml:description>Web page with background information from the source of this dataset</gml:description>\n" +
"          <sml:format>text/html</sml:format>\n" +
"          <sml:onlineResource xlink:href=\"http://www.ndbc.noaa.gov/\" />\n" +
"        </sml:Document>\n" +
"      </sml:documentation>\n" +
"\n" +
"      <sml:location>\n" +
"        <gml:Point srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"Station_LatLon\">\n" +
"          <gml:coordinates>32.5 -79.09</gml:coordinates>\n" +
"        </gml:Point>\n" +
"      </sml:location>\n" +
"\n" +
"      <sml:components>\n" +
"        <sml:ComponentList>\n" +
"          <sml:component name=\"wtmp Instrument\">\n" +
"            <sml:System gml:id=\"sensor-Station-41004-wtmp\">\n" +
"              <gml:description>Station-41004-wtmp Sensor</gml:description>\n" +
"              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wtmp\" />\n" +
"              <sml:documentation xlink:href=\"http://127.0.0.1:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n" +
"              <sml:outputs>\n" +
"                <sml:OutputList>\n" +
"                  <sml:output name=\"wtmp\">\n" +
"                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n" +
"                      <swe:uom code=\"degree_C\" />\n" +
"                    </swe:Quantity>\n" +
"                  </sml:output>\n" +
"                </sml:OutputList>\n" +
"              </sml:outputs>\n" +
"            </sml:System>\n" +
"          </sml:component>\n" +
"        </sml:ComponentList>\n" +
"      </sml:components>\n" +
"    </sml:System>\n" +
"  </sml:member>\n" +
"</sml:SensorML>\n";
        Test.ensureEqual(results, expected, "results=\n"+results);



//???!!!
//test sosQueryToDapQuery() more
//???write tests of invalid queries?
//need test of xml response for currents (depths)
//support var vs. var plot?
//need test of png of  sticks  (ensure 2 vars have same units?)

/*  */

        //*** observations for 1 station, all vars  CSV response
        String sosQuery1 = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:" +
            "&observedProperty=cwwcNDBCMet" +
            "&responseFormat=text/xml;schema=%22ioos/0.6.1%22" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
        String sosQuery1Csv = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:" + //long
            "&observedProperty=" + fullPhenomenaDictionaryUrl + "#cwwcNDBCMet" + //long
            "&responseFormat=text/csv" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
        String2.log("\n+++ GetObservations for 1 station  CSV\n" + sosQuery1);
        String dapQuery1[] = eddTable.sosQueryToDapQuery(null, sosQuery1);
        String2.log("\nsosQuery1=" + sosQuery1 + "\n\ndapQuery1=" + dapQuery1[0]);
        Test.ensureEqual(dapQuery1[0], 
            "&station=%2241004%22&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z", "");
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery1Csv, null, osss, dir, "testSos1Sta");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
//from eddTableSos.testNdbcSosWind
//"longitude, latitude, station_id, altitude, time, WindSpeed, WindDirection, WindVerticalVelocity, WindGust\n" +
//"degrees_east, degrees_north, , m, UTC, m s-1, degrees_true, m s-1, m s-1\n" +
//"-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN, 2008-08-01T00:50:00Z, 10.1, 229.0, NaN, 12.6\n" +
//"-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN, 2008-08-01T01:50:00Z, 9.3, 232.0, NaN, 11.3\n" +
//"-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN, 2008-08-01T02:50:00Z, 7.8, 237.0, NaN, 11.5\n" +
//"-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN, 2008-08-01T03:50:00Z, 8.0, 236.0, NaN, 9.3\n";

"longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
"degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n" +
"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** #1: observations for 1 station, all vars,   sos XML response
        String2.log("\n+++ GetObservations for 1 station  XML\n" + sosQuery1);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery1, null, osss, dir, "testSos1");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<om:CompositeObservation xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n" +
"  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n" +
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n" +
"  <gml:name>NDBC Standard Meteorological Buoy Data, Station 41004</gml:name>\n" +
"  <gml:boundedBy>\n" +
"    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"      <gml:lowerCorner>32.5 -79.09</gml:lowerCorner>\n" +
"      <gml:upperCorner>32.5 -79.09</gml:upperCorner>\n" +
"    </gml:Envelope>\n" +
"  </gml:boundedBy>\n" +
"  <om:samplingTime>\n" +
"    <gml:TimePeriod gml:id=\"ST\">\n" +
"      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n" +
"      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n" +
"    </gml:TimePeriod>\n" +
"  </om:samplingTime>\n" +
"  <om:procedure>\n" +
"    <om:Process>\n" +
"      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n" +
"        <gml:valueComponents>\n" +
"          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n" +
"          <ioos:ContextArray gml:id=\"StationArray\">\n" +
"            <gml:valueComponents>\n" +
"              <ioos:CompositeContext gml:id=\"Station1Info\">\n" +
"                <gml:valueComponents>\n" +
"                  <ioos:StationName>Station - 41004</ioos:StationName>\n" +
"                  <ioos:Organization>NOAA NDBC, CoastWatch WCN</ioos:Organization>\n" +
"                  <ioos:StationId>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</ioos:StationId>\n" +
"                  <gml:Point gml:id=\"Station1LatLon\">\n" +
"                    <gml:pos>32.5 -79.09</gml:pos>\n" +
"                  </gml:Point>\n" +
"                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n" +
"                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n" +
"                  <ioos:Count name=\"Station1NumberOfSensors\">1</ioos:Count>\n" +
"                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n" +
"                    <gml:valueComponents>\n" +
"                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:cwwcNDBCMet</ioos:SensorId>\n" +
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n" +
"                    </gml:valueComponents>\n" +
"                  </ioos:ContextArray>\n" +
"                </gml:valueComponents>\n" +
"              </ioos:CompositeContext>\n" +
"            </gml:valueComponents>\n" +
"          </ioos:ContextArray>\n" +
"        </gml:valueComponents>\n" +
"      </ioos:CompositeContext>\n" +
"    </om:Process>\n" +
"  </om:procedure>\n" +
"  <om:observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n" +
"  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
"  <om:result>\n" +
"    <ioos:Composite gml:id=\"cwwcNDBCMetPointCollectionTimeSeriesDataObservations\">\n" +
"      <gml:valueComponents>\n" +
"        <ioos:Count name=\"NumberOfObservationsPoints\">1</ioos:Count>\n" +
"        <ioos:Array gml:id=\"cwwcNDBCMetPointCollectionTimeSeries\">\n" +
"          <gml:valueComponents>\n" +
"            <ioos:Composite gml:id=\"Station1TimeSeriesRecord\">\n" +
"              <gml:valueComponents>\n" +
"                <ioos:Count name=\"Station1NumberOfObservationsTimes\">2</ioos:Count>\n" +
"                <ioos:Array gml:id=\"Station1TimeSeries\">\n" +
"                  <gml:valueComponents>\n" +
"                    <ioos:Composite gml:id=\"Station1T1Point\">\n" +
"                      <gml:valueComponents>\n" +
"                        <ioos:CompositeContext gml:id=\"Station1T1ObservationConditions\" processDef=\"#Station1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <gml:TimeInstant gml:id=\"Station1T1Time\">\n" +
"                              <gml:timePosition>2008-08-01T00:00:00Z</gml:timePosition>\n" +
"                            </gml:TimeInstant>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeContext>\n" +
"                        <ioos:CompositeValue gml:id=\"Station1T1PointObservation\" processDef=\"#Station1Sensor1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <ioos:Quantity name=\"wd\" uom=\"degrees_true\">225</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wspd\" uom=\"m s-1\">10.9</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"gst\" uom=\"m s-1\">14.0</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wvht\" uom=\"m\">1.66</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"dpd\" uom=\"s\">5.26</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"apd\" uom=\"s\">4.17</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"mwd\" uom=\"degrees_true\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"bar\" uom=\"hPa\">1007.6</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"dewp\" uom=\"degree_C\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"vis\" uom=\"km\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"ptdy\" uom=\"hPa\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"tide\" uom=\"m\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"wspu\" uom=\"m s-1\">7.7</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wspv\" uom=\"m s-1\">7.7</ioos:Quantity>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeValue>\n" +
"                      </gml:valueComponents>\n" +
"                    </ioos:Composite>\n" +
"                    <ioos:Composite gml:id=\"Station1T2Point\">\n" +
"                      <gml:valueComponents>\n" +
"                        <ioos:CompositeContext gml:id=\"Station1T2ObservationConditions\" processDef=\"#Station1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <gml:TimeInstant gml:id=\"Station1T2Time\">\n" +
"                              <gml:timePosition>2008-08-01T01:00:00Z</gml:timePosition>\n" +
"                            </gml:TimeInstant>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeContext>\n" +
"                        <ioos:CompositeValue gml:id=\"Station1T2PointObservation\" processDef=\"#Station1Sensor1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <ioos:Quantity name=\"wd\" uom=\"degrees_true\">229</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wspd\" uom=\"m s-1\">10.1</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"gst\" uom=\"m s-1\">12.6</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wvht\" uom=\"m\">1.68</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"dpd\" uom=\"s\">5.56</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"apd\" uom=\"s\">4.36</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"mwd\" uom=\"degrees_true\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"bar\" uom=\"hPa\">1008.0</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"dewp\" uom=\"degree_C\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"vis\" uom=\"km\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"ptdy\" uom=\"hPa\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"tide\" uom=\"m\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n" +
"                            <ioos:Quantity name=\"wspu\" uom=\"m s-1\">7.6</ioos:Quantity>\n" +
"                            <ioos:Quantity name=\"wspv\" uom=\"m s-1\">6.6</ioos:Quantity>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeValue>\n" +
"                      </gml:valueComponents>\n" +
"                    </ioos:Composite>\n" +
"                  </gml:valueComponents>\n" +
"                </ioos:Array>\n" +
"              </gml:valueComponents>\n" +
"            </ioos:Composite>\n" +
"          </gml:valueComponents>\n" +
"        </ioos:Array>\n" +
"      </gml:valueComponents>\n" +
"    </ioos:Composite>\n" +
"  </om:result>\n" +
"</om:CompositeObservation>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** #1b: observations for 1 station, all vars,    no time -> last time, CSV response
        String sosQuery1b = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:" +
            "&observedProperty=cwwcNDBCMet" +
            "&responseFormat=text/csv";
        String2.log("\n+++ 1b: GetObservations for 1 station  CSV\n" + sosQuery1b);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery1b, null, osss, dir, "testSos1b");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected =    //changes when I update ndbc
"longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
"degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n" +
"-79.09, 32.5, 2010-01-08T19:00:00Z, 41004, 300, 8.0, 9.0, 1.4, 7.0, 4.8, NaN, 1015.2, 6.6, 20.0, 2.2, NaN, -1.5, NaN, 6.9, -4.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** #2: observations for all stations, all vars, and with BBOX  (but just same 1 station) (csv)
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        String2.log("\n+++ GetObservations with BBOX (1 station)");
        String sosQuery2 = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet" +
            "&observedProperty=cwwcNDBCMet" + //short
            "&responseFormat=text/csv" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z" +
            "&featureOfInterest=BBOX:-79.10,32.4,-79.08,32.6";
        String dapQuery2[] = eddTable.sosQueryToDapQuery(null, sosQuery2);
        String2.log("\nsosQuery2=" + sosQuery2 + "\n\ndapQuery2=" + dapQuery2[0]);
        Test.ensureEqual(dapQuery2[0], 
            "&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z" +
                "&longitude>=-79.1&longitude<=-79.08&latitude>=32.4&latitude<=32.6", 
            "");
        writer = new java.io.StringWriter();
        String2.log("query: " + sosQuery2);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery2, null, osss, dir, "testSos2");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
"degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n" +
"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        
        //*** #2b:  observations for all stations, 2 vars, and with BBOX  (but just same 1 station)  (csv)
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        String2.log("\n+++ 2b: GetObservations, 2 vars, with BBOX (1 station)");
        String sosQuery2b = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet" +
            "&observedProperty=atmp," + fullPhenomenaDictionaryUrl + "#wtmp" + //short and long
            "&responseFormat=text/csv" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z" +
            "&featureOfInterest=BBOX:-79.10,32.4,-79.08,32.6";
        String dapQuery2b[] = eddTable.sosQueryToDapQuery(null, sosQuery2b);
        String2.log("\nsosQuery2b=" + sosQuery2b + "\n\ndapQuery2b=" + dapQuery2b[0]);
        Test.ensureEqual(dapQuery2b[0], 
            "longitude,latitude,time,station,atmp,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z" +
                "&longitude>=-79.1&longitude<=-79.08&latitude>=32.4&latitude<=32.6",
            "");
        writer = new java.io.StringWriter();
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery2b, null, osss, dir, "testSos2b");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"longitude, latitude, time, station, atmp, wtmp\n" +
"degrees_east, degrees_north, UTC, , degree_C, degree_C\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 27.8, 27.9\n" +
"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 27.8, 27.9\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** #2c:  observations for all stations, 2 vars, and with BBOX  (but just same 1 station) (xml)
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        String2.log("\n+++ 2c: GetObservations, 2 vars, with BBOX (1 station)");
        String sosQuery2c = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet" +
            "&observedProperty=atmp," + fullPhenomenaDictionaryUrl + "#wtmp" + //short and long
            "&responseFormat=text/xml;schema=%22ioos/0.6.1%22" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z" +
            "&featureOfInterest=BBOX:-79.10,32.4,-79.08,32.6";
        String dapQuery2c[] = eddTable.sosQueryToDapQuery(null, sosQuery2c);
        String2.log("\nsosQuery2c=" + sosQuery2c + "\n\ndapQuery2c=" + dapQuery2c[0]);
        Test.ensureEqual(dapQuery2c[0], 
            "longitude,latitude,time,station,atmp,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z" +
                "&longitude>=-79.1&longitude<=-79.08&latitude>=32.4&latitude<=32.6", 
            "");
        writer = new java.io.StringWriter();
        String2.log("query: " + sosQuery2c);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery2c, null, osss, dir, "testSos2c");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<om:CompositeObservation xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n" +
"  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n" +
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n" +
"  <gml:name>NDBC Standard Meteorological Buoy Data, network cwwcNDBCMet</gml:name>\n" +
"  <gml:boundedBy>\n" +
"    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"      <gml:lowerCorner>32.5 -79.09</gml:lowerCorner>\n" +
"      <gml:upperCorner>32.5 -79.09</gml:upperCorner>\n" +
"    </gml:Envelope>\n" +
"  </gml:boundedBy>\n" +
"  <om:samplingTime>\n" +
"    <gml:TimePeriod gml:id=\"ST\">\n" +
"      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n" +
"      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n" +
"    </gml:TimePeriod>\n" +
"  </om:samplingTime>\n" +
"  <om:procedure>\n" +
"    <om:Process>\n" +
"      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n" +
"        <gml:valueComponents>\n" +
"          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n" +
"          <ioos:ContextArray gml:id=\"StationArray\">\n" +
"            <gml:valueComponents>\n" +
"              <ioos:CompositeContext gml:id=\"Station1Info\">\n" +
"                <gml:valueComponents>\n" +
"                  <ioos:StationName>Station - 41004</ioos:StationName>\n" +
"                  <ioos:Organization>NOAA NDBC, CoastWatch WCN</ioos:Organization>\n" +
"                  <ioos:StationId>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</ioos:StationId>\n" +
"                  <gml:Point gml:id=\"Station1LatLon\">\n" +
"                    <gml:pos>32.5 -79.09</gml:pos>\n" +
"                  </gml:Point>\n" +
"                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n" +
"                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n" +
"                  <ioos:Count name=\"Station1NumberOfSensors\">2</ioos:Count>\n" +
"                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n" +
"                    <gml:valueComponents>\n" +
"                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:atmp</ioos:SensorId>\n" +
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n" +
"                      <ioos:CompositeContext gml:id=\"Station1Sensor2Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:wtmp</ioos:SensorId>\n" +
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n" +
"                    </gml:valueComponents>\n" +
"                  </ioos:ContextArray>\n" +
"                </gml:valueComponents>\n" +
"              </ioos:CompositeContext>\n" +
"            </gml:valueComponents>\n" +
"          </ioos:ContextArray>\n" +
"        </gml:valueComponents>\n" +
"      </ioos:CompositeContext>\n" +
"    </om:Process>\n" +
"  </om:procedure>\n" +
"  <om:observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n" +
"  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
"  <om:result>\n" +
"    <ioos:Composite gml:id=\"cwwcNDBCMetPointCollectionTimeSeriesDataObservations\">\n" +
"      <gml:valueComponents>\n" +
"        <ioos:Count name=\"NumberOfObservationsPoints\">1</ioos:Count>\n" +
"        <ioos:Array gml:id=\"cwwcNDBCMetPointCollectionTimeSeries\">\n" +
"          <gml:valueComponents>\n" +
"            <ioos:Composite gml:id=\"Station1TimeSeriesRecord\">\n" +
"              <gml:valueComponents>\n" +
"                <ioos:Count name=\"Station1NumberOfObservationsTimes\">2</ioos:Count>\n" +
"                <ioos:Array gml:id=\"Station1TimeSeries\">\n" +
"                  <gml:valueComponents>\n" +
"                    <ioos:Composite gml:id=\"Station1T1Point\">\n" +
"                      <gml:valueComponents>\n" +
"                        <ioos:CompositeContext gml:id=\"Station1T1ObservationConditions\" processDef=\"#Station1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <gml:TimeInstant gml:id=\"Station1T1Time\">\n" +
"                              <gml:timePosition>2008-08-01T00:00:00Z</gml:timePosition>\n" +
"                            </gml:TimeInstant>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeContext>\n" +
"                        <ioos:CompositeValue gml:id=\"Station1T1Sensor1PointObservation\" processDef=\"#Station1Sensor1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeValue>\n" +
"                        <ioos:CompositeValue gml:id=\"Station1T1Sensor2PointObservation\" processDef=\"#Station1Sensor2Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeValue>\n" +
"                      </gml:valueComponents>\n" +
"                    </ioos:Composite>\n" +
"                    <ioos:Composite gml:id=\"Station1T2Point\">\n" +
"                      <gml:valueComponents>\n" +
"                        <ioos:CompositeContext gml:id=\"Station1T2ObservationConditions\" processDef=\"#Station1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <gml:TimeInstant gml:id=\"Station1T2Time\">\n" +
"                              <gml:timePosition>2008-08-01T01:00:00Z</gml:timePosition>\n" +
"                            </gml:TimeInstant>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeContext>\n" +
"                        <ioos:CompositeValue gml:id=\"Station1T2Sensor1PointObservation\" processDef=\"#Station1Sensor1Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeValue>\n" +
"                        <ioos:CompositeValue gml:id=\"Station1T2Sensor2PointObservation\" processDef=\"#Station1Sensor2Info\">\n" +
"                          <gml:valueComponents>\n" +
"                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n" +
"                          </gml:valueComponents>\n" +
"                        </ioos:CompositeValue>\n" +
"                      </gml:valueComponents>\n" +
"                    </ioos:Composite>\n" +
"                  </gml:valueComponents>\n" +
"                </ioos:Array>\n" +
"              </gml:valueComponents>\n" +
"            </ioos:Composite>\n" +
"          </gml:valueComponents>\n" +
"        </ioos:Array>\n" +
"      </gml:valueComponents>\n" +
"    </ioos:Composite>\n" +
"  </om:result>\n" +
"</om:CompositeObservation>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        /*
        //#2d   get out-of-band
        String2.log("\n+++ 2d: GetObservations for 1 station, 2 obsProp, out-of-band xml");
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery2b + "&responseMode=out-of-band", null, osss, dir, "testSos2d");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<om:CompositeObservation xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
"  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
"  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n" +
"  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n" +
"<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
"  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n" +
"  <gml:name>NDBC Standard Meteorological Buoy Data, network cwwcNDBCMet</gml:name>\n" +
"  <gml:boundedBy>\n" +
"    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
"      <gml:lowerCorner>32.4 -79.1</gml:lowerCorner>\n" +
"      <gml:upperCorner>32.6 -79.08</gml:upperCorner>\n" +
"    </gml:Envelope>\n" +
"  </gml:boundedBy>\n" +
"  <om:samplingTime>\n" +
"    <gml:TimePeriod gml:id=\"ST\">\n" +
"      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n" +
"      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n" +
"    </gml:TimePeriod>\n" +
"  </om:samplingTime>\n" +
"  <om:procedure>\n" +
"    <om:Process>\n" +
"      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n" +
"        <gml:valueComponents>\n" +
"          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n" +
"          <ioos:ContextArray gml:id=\"StationArray\">\n" +
"            <gml:valueComponents>\n" +
"              <ioos:CompositeContext gml:id=\"Station1Info\">\n" +
"                <gml:valueComponents>\n" +
"                  <ioos:StationName>Station - 41004</ioos:StationName>\n" +
"                  <ioos:Organization>NOAA NDBC, CoastWatch WCN</ioos:Organization>\n" +
"                  <ioos:StationId>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</ioos:StationId>\n" +
"                  <gml:Point gml:id=\"Station1LatLon\">\n" +
"                    <gml:pos>-79.09 32.5</gml:pos>\n" +
"                  </gml:Point>\n" +
"                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n" +
"                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n" +
"                  <ioos:Count name=\"Station1NumberOfSensors\">2</ioos:Count>\n" +
"                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n" +
"                    <gml:valueComponents>\n" +
"                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:atmp</ioos:SensorId>\n" +
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n" +
"                      <ioos:CompositeContext gml:id=\"Station1Sensor2Info\">\n" +
"                        <gml:valueComponents>\n" +
"                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:wtmp</ioos:SensorId>\n" +
"                        </gml:valueComponents>\n" +
"                      </ioos:CompositeContext>\n" +
"                    </gml:valueComponents>\n" +
"                  </ioos:ContextArray>\n" +
"                </gml:valueComponents>\n" +
"              </ioos:CompositeContext>\n" +
"            </gml:valueComponents>\n" +
"          </ioos:ContextArray>\n" +
"        </gml:valueComponents>\n" +
"      </ioos:CompositeContext>\n" +
"    </om:Process>\n" +
"  </om:procedure>\n" +
"  <om:observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n" +
"  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
"  <om:result xlink:href=\"http://127.0.0.1:8080/cwexperimental/tabledap/cwwcNDBCMet.csv?longitude,latitude,time,station,atmp,wtmp&amp;time&gt;=2008-08-01T00:00:00Z&amp;time&lt;=2008-08-01T01:00:00Z&amp;longitude&gt;=-79.1&amp;longitude&lt;=-79.08&amp;latitude&gt;=32.4&amp;latitude&lt;=32.6\"/>\n" +
"</om:CompositeObservation>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
*/
        
        //*** #3: observations for all stations, all vars, and with BBOX  (multiple stations)
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        String sosQuery3 = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=cwwcNDBCMet" +  //short
            "&observedProperty=cwwcNDBCMet" +
            "&responseFormat=text/csv" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z" +
            "&featureOfInterest=BBOX:-79.9,32.4,-79.0,33.0";
        String dapQuery3[] = eddTable.sosQueryToDapQuery(null, sosQuery3);
        String2.log("\nsosQuery3=" + sosQuery3 + "\n\ndapQuery3=" + dapQuery3[0]);
        Test.ensureEqual(dapQuery3[0], 
            "&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z" +
                "&longitude>=-79.9&longitude<=-79.0&latitude>=32.4&latitude<=33.0", 
            "");
        String2.log("\n+++ GetObservations with BBOX (multiple stations)");
        writer = new java.io.StringWriter();
        String2.log("query: " + sosQuery3);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery3, null, osss, dir, "testSos3");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
"degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n" +
"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n" +
"-79.63, 32.81, 2008-08-01T00:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n" +
"-79.63, 32.81, 2008-08-01T01:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n" +
"-79.89, 32.68, 2008-08-01T00:00:00Z, FBIS1, 237, 6.4, 9.0, NaN, NaN, NaN, NaN, 1009.5, 28.7, NaN, 24.8, NaN, NaN, NaN, 5.4, 3.5\n" +
"-79.89, 32.68, 2008-08-01T01:00:00Z, FBIS1, 208, 1.3, 1.5, NaN, NaN, NaN, NaN, 1010.1, 27.1, NaN, 24.9, NaN, NaN, NaN, 0.6, 1.1\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** #4  all stations,  no obsProp -> all vars,   text/csv
        String sosQuery4 = //no obsProp
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=cwwcNDBCMet" + //short
            "&responseFormat=text/csv" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z" +
            "&featureOfInterest=BBOX:-79.9,32.4,-79.0,33.0";
        String dapQuery4[] = eddTable.sosQueryToDapQuery(null, sosQuery4);
        String2.log("\nsosQuery4=" + sosQuery4 + "\n\ndapQuery4=" + dapQuery4[0]);
        Test.ensureEqual(dapQuery4[0], 
            "&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z&longitude>=-79.9&longitude<=-79.0&latitude>=32.4&latitude<=33.0",
            "");
        writer = new java.io.StringWriter();
        String2.log("query: " + sosQuery4);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery4, null, osss, dir, "testSos4");
        results = baos.toString("UTF-8");
        expected = 
"longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
"degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n" +
"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n" +
"-79.63, 32.81, 2008-08-01T00:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n" +
"-79.63, 32.81, 2008-08-01T01:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n" +
"-79.89, 32.68, 2008-08-01T00:00:00Z, FBIS1, 237, 6.4, 9.0, NaN, NaN, NaN, NaN, 1009.5, 28.7, NaN, 24.8, NaN, NaN, NaN, 5.4, 3.5\n" +
"-79.89, 32.68, 2008-08-01T01:00:00Z, FBIS1, 208, 1.3, 1.5, NaN, NaN, NaN, NaN, 1010.1, 27.1, NaN, 24.9, NaN, NaN, NaN, 0.6, 1.1\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        
        //***  #5:   1 station, 1 obsProp  csv
        String sosQuery5csv = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=41004" + //short
            "&observedProperty=" + fullPhenomenaDictionaryUrl + "#wtmp" +  //long
            "&responseFormat=text/csv" +
            "&eventTime=2008-07-25T00:00:00Z/2008-08-01T00:00:00Z";
        String sosQuery5png = 
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=41004" + //short
            "&observedProperty=wtmp" + //short
            "&responseFormat=image/png" +
            "&eventTime=2008-07-25T00:00:00Z/2008-08-01T00:00:00Z";

        String dapQuery5[] = eddTable.sosQueryToDapQuery(null, sosQuery5csv);
        String2.log("\nsosQuery5csv=" + sosQuery5csv + "\n\ndapQuery5=" + dapQuery5[0]);
        Test.ensureEqual(dapQuery5[0], 
            "longitude,latitude,time,station,wtmp&station=%2241004%22" +
                "&time>=2008-07-25T00:00:00Z&time<=2008-08-01T00:00:00Z",
            "");
        String2.log("\n+++ 5csv: GetObservations for 1 station, 1 obsProp\n" + sosQuery5csv);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery5csv, null, osss, dir, "testSos5csv");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"longitude, latitude, time, station, wtmp\n" +
"degrees_east, degrees_north, UTC, , degree_C\n" +
"-79.09, 32.5, 2008-07-25T00:00:00Z, 41004, 28.0\n" +
"-79.09, 32.5, 2008-07-25T01:00:00Z, 41004, 27.9\n" +
"-79.09, 32.5, 2008-07-25T02:00:00Z, 41004, 27.8\n" +
"-79.09, 32.5, 2008-07-25T03:00:00Z, 41004, 27.6\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
"-79.09, 32.5, 2008-07-31T21:00:00Z, 41004, 28.0\n" +
"-79.09, 32.5, 2008-07-31T22:00:00Z, 41004, 28.0\n" +
"-79.09, 32.5, 2008-07-31T23:00:00Z, 41004, 27.9\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 27.9\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);
    
        //#5png   get png -> time series
        String2.log("\n+++ 5png: GetObservations for 1 station, 1 obsProp\n" + sosQuery5png);
        dapQuery5 = eddTable.sosQueryToDapQuery(null, sosQuery5png);
        String2.log("\nsosQuery5png=" + sosQuery5png + "\n\ndapQuery5=" + dapQuery5[0]);
        Test.ensureEqual(dapQuery5[0], 
            "time,wtmp&station=%2241004%22" +
                "&time>=2008-07-25T00:00:00Z&time<=2008-08-01T00:00:00Z" +
                "&.draw=linesAndMarkers&.marker=5|4&.color=0xFF9900",
            "");
        String dapQuery = eddTable.sosQueryToDapQuery(null, sosQuery5png)[0];
        fileName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_testSos5png", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + fileName);


        //*** #6   all stations,  1 obsProp, 
        String sosQuery6csv = //no obsProp
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=cwwcNDBCMet" + //short
            "&observedProperty=wtmp" +
            "&responseFormat=text/csv" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
        String dapQuery6[] = eddTable.sosQueryToDapQuery(null, sosQuery6csv);
        String2.log("\nsosQuery6csv=" + sosQuery6csv + "\n\ndapQuery6csv=" + dapQuery6[0]);
        Test.ensureEqual(dapQuery6[0], 
            "longitude,latitude,time,station,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z",
            "");
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddTable.sosGetObservation(sosQuery6csv, null, osss, dir, "testSos6csv");
        results = baos.toString("UTF-8");
        String2.log(results);        
        expected = 
"longitude, latitude, time, station, wtmp\n" +
"degrees_east, degrees_north, UTC, , degree_C\n" +
"-85.38, -19.62, 2008-08-01T00:00:00Z, 32012, NaN\n" +
"-85.38, -19.62, 2008-08-01T01:00:00Z, 32012, NaN\n" +
"-72.66, 34.68, 2008-08-01T00:00:00Z, 41001, NaN\n" +
"-72.66, 34.68, 2008-08-01T01:00:00Z, 41001, NaN\n" +
"-75.35, 32.31, 2008-08-01T00:00:00Z, 41002, NaN\n" +
"-75.35, 32.31, 2008-08-01T01:00:00Z, 41002, NaN\n" +
"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 27.9\n" +
"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 27.9\n" +
"-80.87, 31.4, 2008-08-01T00:00:00Z, 41008, 27.1\n" +
"-80.87, 31.4, 2008-08-01T01:00:00Z, 41008, 27.0\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
"-76.48, 37.23, 2008-08-01T00:00:00Z, YKTV2, 28.2\n" +
"-76.48, 37.23, 2008-08-01T01:00:00Z, YKTV2, 28.3\n" +
"-76.7125, 37.4142, 2008-08-01T00:00:00Z, YRSV2, NaN\n" +
"-76.7125, 37.4142, 2008-08-01T01:00:00Z, YRSV2, NaN\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);

        String sosQuery6png = //no obsProp
            "service=SOS&version=1.0.0&request=GetObservation" +
            "&offering=cwwcNDBCMet" + //short
            "&observedProperty=wtmp" +
            "&responseFormat=image/png" +
            "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
        dapQuery6 = eddTable.sosQueryToDapQuery(null, sosQuery6png);
        String2.log("\nsosQuery6png=" + sosQuery6png + "\n\ndapQuery6png=" + dapQuery6[0]);
        Test.ensureEqual(dapQuery6[0], 
            "longitude,latitude,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z&.draw=markers&.marker=5|4",
            "");
        fileName = eddTable.makeNewFileForDapQuery(null, null, dapQuery6[0], 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_testSos6png", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + fileName);
/*  */

    }


    /**
     * Test SOS server using ndbcSosCurrents.
     */
    public static void testSosCurrents() throws Throwable {
        try {
            String2.log("\n*** EDDTable.testSosCurrents()");
            EDDTable eddTable = (EDDTable)oneFromDatasetXml("ndbcSosCurrents"); 
            String dir = EDStatic.fullTestCacheDirectory;
            String sosQuery, fileName, results, expected;
            java.io.StringWriter writer;
            ByteArrayOutputStream baos;
            OutputStreamSourceSimple osss;

            //GetCapabilities
            String2.log("\n+++ GetCapabilities");
            writer = new java.io.StringWriter();
            eddTable.sosGetCapabilities(writer, null);
            results = writer.toString();
            String2.log(results.substring(0, 7000));        
            String2.log("\n...\n");
            String2.log(results.substring(45000, 55000));        
            String2.log("\n...\n");
            String2.log(results.substring(results.length() - 3000));        

            //*** observations for 1 station (via bbox), 2 times   
            String sosQuery1 = 
                "service=SOS&version=1.0.0&request=GetObservation" +
                "&offering=urn:ioos:network:1.0.0.127.ndbcSosCurrents:ndbcSosCurrents" +
                "&observedProperty=ndbcSosCurrents" +
                "&responseFormat=text/csv" +
                "&eventTime=2008-06-01T14:00:00Z/2008-06-01T14:30:00Z" +
                "&featureOfInterest=BBOX:-88,29.1,-87.9,29.2"; //<min_lon>,<min_lat>,<max_lon>,<max_lat>

            String2.log("\n+++ GetObservations for 1 station (via BBOX)\n" + sosQuery1);
            baos = new ByteArrayOutputStream();
            osss = new OutputStreamSourceSimple(baos);
            eddTable.sosGetObservation(sosQuery1, null, osss, dir, "testSosCurBB");
    /*from eddTableSos.testNdbcSosCurrents
    "&longitude=-87.94&latitude>=29.1&latitude<29.2&time>=2008-06-01T14:00&time<=2008-06-01T14:30", 
    "station_id, longitude, latitude, altitude, time, CurrentDirection, CurrentSpeed\n" +
    ", degrees_east, degrees_north, m, UTC, degrees_true, cm s-1\n" +
    "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -56.8, 2008-06-01T14:03:00Z, 83, 30.2\n" +
    "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -88.8, 2008-06-01T14:03:00Z, 96, 40.5\n" +
    "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -120.8, 2008-06-01T14:03:00Z, 96, 40.7\n" +
    "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -152.8, 2008-06-01T14:03:00Z, 96, 35.3\n" +
    "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -184.8, 2008-06-01T14:03:00Z, 89, 31.9\n" +
    */
            results = baos.toString("UTF-8");
            //String2.log(results);        
            expected = 
    "longitude, latitude, station_id, altitude, time, CurrentDirection, CurrentSpeed\n" +
    "degrees_east, degrees_north, , m, UTC, degrees_true, cm s-1\n" +
    "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -56.8, 2008-06-01T14:03:00Z, 83, 30.2\n" +
    "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -88.8, 2008-06-01T14:03:00Z, 96, 40.5\n" +
    "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -120.8, 2008-06-01T14:03:00Z, 96, 40.7\n" +
    "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -152.8, 2008-06-01T14:03:00Z, 96, 35.3\n" +
    "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -184.8, 2008-06-01T14:03:00Z, 89, 31.9\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

            //*** same query, but via offering=(station) instead of via bbox
            //  observations for 1 station, 2 times   
            String sosQuery2 = 
                "service=SOS&version=1.0.0&request=GetObservation" +
                "&offering=urn:ioos:Station:1.0.0.127.ndbcSosCurrents:42376:" +
                "&observedProperty=ndbcSosCurrents" +
                "&responseFormat=text/csv" +
                "&eventTime=2008-06-01T14:00:00Z/2008-06-01T14:30:00Z";

            String2.log("\n+++ GetObservations for 1 station (via offering=(station))\n" + sosQuery2);
            baos = new ByteArrayOutputStream();
            osss = new OutputStreamSourceSimple(baos);
            eddTable.sosGetObservation(sosQuery2, null, osss, dir, "testSosCurSta");
            results = baos.toString("UTF-8");
            String2.log(results);        
            //expected = same data
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);


            //*** same query, but return as sos xml
            //  observations for 1 station, 2 times   
            String sosQuery3 = 
                "service=SOS&version=1.0.0&request=GetObservation" +
                "&offering=42376" +  //and switch to short offering name
                "&observedProperty=ndbcSosCurrents" +
                "&responseFormat=" + SSR.minimalPercentEncode("text/xml;schema=\"ioos/0.6.1\"") +
                "&eventTime=2008-06-01T14:00:00Z/2008-06-01T14:30:00Z";

            String2.log("\n+++ GetObservations for 1 station (via offering=(station))\n" + sosQuery3);
            baos = new ByteArrayOutputStream();
            osss = new OutputStreamSourceSimple(baos);
            eddTable.sosGetObservation(sosQuery3, null, osss, dir, "testSosCurSta2");
            results = baos.toString("UTF-8");
            //String2.log(results);        
            expected = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<om:CompositeObservation xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
    "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
    "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
    "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
    "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
    "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n" +
    "  gml:id=\"ndbcSosCurrentsTimeSeriesObservation\">\n" +
    "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
    "  <gml:description>ndbcSosCurrents observations at a series of times</gml:description>\n" +
    "  <gml:name>Buoy Data from the NOAA NDBC SOS Server - Currents, Station 42376</gml:name>\n" +
    "  <gml:boundedBy>\n" +
    "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
    "      <gml:lowerCorner>29.16 -87.94</gml:lowerCorner>\n" +
    "      <gml:upperCorner>29.16 -87.94</gml:upperCorner>\n" +
    "    </gml:Envelope>\n" +
    "  </gml:boundedBy>\n" +
    "  <om:samplingTime>\n" +
    "    <gml:TimePeriod gml:id=\"ST\">\n" +
    "      <gml:beginPosition>2008-06-01T14:03:00Z</gml:beginPosition>\n" +
    "      <gml:endPosition>2008-06-01T14:23:00Z</gml:endPosition>\n" +
    "    </gml:TimePeriod>\n" +
    "  </om:samplingTime>\n" +
    "  <om:procedure>\n" +
    "    <om:Process>\n" +
    "      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n" +
    "        <gml:valueComponents>\n" +
    "          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n" +
    "          <ioos:ContextArray gml:id=\"StationArray\">\n" +
    "            <gml:valueComponents>\n" +
    "              <ioos:CompositeContext gml:id=\"Station1Info\">\n" +
    "                <gml:valueComponents>\n" +
    "                  <ioos:StationName>Station - urn:ioos:station:noaa.nws.ndbc:42376:</ioos:StationName>\n" +
    "                  <ioos:Organization>NOAA NDBC</ioos:Organization>\n" +
    "                  <ioos:StationId>urn:ioos:Station:1.0.0.127.ndbcSosCurrents:urn:ioos:station:noaa.nws.ndbc:42376:</ioos:StationId>\n" +
    "                  <gml:Point gml:id=\"Station1LatLon\">\n" +
    "                    <gml:pos>29.16 -87.94</gml:pos>\n" +
    "                  </gml:Point>\n" +
    "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n" +
    "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n" +
    "                  <ioos:Count name=\"Station1NumberOfSensors\">1</ioos:Count>\n" +
    "                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n" +
    "                    <gml:valueComponents>\n" +
    "                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n" +
    "                        <gml:valueComponents>\n" +
    "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.ndbcSosCurrents:urn:ioos:station:noaa.nws.ndbc:42376:ndbcSosCurrents</ioos:SensorId>\n" +
    "                        </gml:valueComponents>\n" +
    "                      </ioos:CompositeContext>\n" +
    "                    </gml:valueComponents>\n" +
    "                  </ioos:ContextArray>\n" +
    "                </gml:valueComponents>\n" +
    "              </ioos:CompositeContext>\n" +
    "            </gml:valueComponents>\n" +
    "          </ioos:ContextArray>\n" +
    "        </gml:valueComponents>\n" +
    "      </ioos:CompositeContext>\n" +
    "    </om:Process>\n" +
    "  </om:procedure>\n" +
    "  <om:observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/ndbcSosCurrents/phenomenaDictionary.xml#ndbcSosCurrents\"/>\n" +
    "  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
    "  <om:result>\n" +
    "    <ioos:Composite gml:id=\"ndbcSosCurrentsPointCollectionTimeSeriesDataObservations\">\n" +
    "      <gml:valueComponents>\n" +
    "        <ioos:Count name=\"NumberOfObservationsPoints\">1</ioos:Count>\n" +
    "        <ioos:Array gml:id=\"ndbcSosCurrentsPointCollectionTimeSeries\">\n" +
    "          <gml:valueComponents>\n" +
    "            <ioos:Composite gml:id=\"Station1TimeSeriesRecord\">\n" +
    "              <gml:valueComponents>\n" +
    "                <ioos:Count name=\"Station1NumberOfObservationsTimes\">2</ioos:Count>\n" +
    "                <ioos:Array gml:id=\"Station1ProfileTimeSeries\">\n" +
    "                  <gml:valueComponents>\n" +
    "                    <ioos:Composite gml:id=\"Station1T1Profile\">\n" +
    "                      <gml:valueComponents>\n" +
    "                        <ioos:CompositeContext gml:id=\"Station1T1ObservationConditions\" processDef=\"#Station1Info\">\n" +
    "                          <gml:valueComponents>\n" +
    "                            <gml:TimeInstant gml:id=\"Station1T1Time\">\n" +
    "                              <gml:timePosition>2008-06-01T14:03:00Z</gml:timePosition>\n" +
    "                            </gml:TimeInstant>\n" +
    "                          </gml:valueComponents>\n" +
    "                        </ioos:CompositeContext>\n" +
    "                        <ioos:Count name=\"Station1T1NumberOfBinObservations\">32</ioos:Count>\n" +
    "                        <ioos:ValueArray gml:id=\"Station1T1ProfileObservations\">\n" +
    "                          <gml:valueComponents>\n" +
    "                            <ioos:CompositeValue gml:id=\"Station1T1Bin1Obs\" processDef=\"#Station1Sensor1Info\">\n" +
    "                              <gml:valueComponents>\n" +
    "                                <ioos:Context name=\"altitude\" uom=\"m\">-1048.8</ioos:Context>\n" +
    "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">0</ioos:Quantity>\n" +
    "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">0.0</ioos:Quantity>\n" +
    "                              </gml:valueComponents>\n" +
    "                            </ioos:CompositeValue>\n" +
    "                            <ioos:CompositeValue gml:id=\"Station1T1Bin2Obs\" processDef=\"#Station1Sensor1Info\">\n" +
    "                              <gml:valueComponents>\n" +
    "                                <ioos:Context name=\"altitude\" uom=\"m\">-1016.8</ioos:Context>\n" +
    "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">0</ioos:Quantity>\n" +
    "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">0.0</ioos:Quantity>\n" +
    "                              </gml:valueComponents>\n" +
    "                            </ioos:CompositeValue>\n" +
    "                            <ioos:CompositeValue gml:id=\"Station1T1Bin3Obs\" processDef=\"#Station1Sensor1Info\">\n" +
    "                              <gml:valueComponents>\n" +
    "                                <ioos:Context name=\"altitude\" uom=\"m\">-984.8</ioos:Context>\n" +
    "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">0</ioos:Quantity>\n" +
    "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">0.0</ioos:Quantity>\n" +
    "                              </gml:valueComponents>\n" +
    "                            </ioos:CompositeValue>\n" +
    "                            <ioos:CompositeValue gml:id=\"Station1T1Bin4Obs\" processDef=\"#Station1Sensor1Info\">\n" +
    "                              <gml:valueComponents>\n" +
    "                                <ioos:Context name=\"altitude\" uom=\"m\">-952.8</ioos:Context>\n" +
    "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">75</ioos:Quantity>\n" +
    "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">4.3</ioos:Quantity>\n" +
    "                              </gml:valueComponents>\n" +
    "                            </ioos:CompositeValue>\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * Test SOS server using gomoosBuoy.
     * This caused an internal error in EDDTableFromSos. So this helped me fix the bug.
     */
    public static void testSosGomoos() throws Throwable {
        try {
            String2.log("\n*** EDDTable.testSosGomoos()");
            EDDTable eddTable = (EDDTable)oneFromDatasetXml("gomoosBuoy"); 
            String dir = EDStatic.fullTestCacheDirectory;
            String sosQuery, fileName, results, expected;
            java.io.StringWriter writer;
            ByteArrayOutputStream baos;
            OutputStreamSourceSimple osss;

            //*** observations for 1 station   
            //request that caused error:
            ///erddap2/sos/gomoosBuoy/server?service=SOS&version=1.0.0&request=GetObservation
            //&offering=urn:ioos:Station:noaa.pfeg.coastwatch.gomoosBuoy::A01
            //&observedProperty=gomoosBuoy&eventTime=2009-08-25T00:00:00Z/2009-09-01T18:23:51Z
            //&responseFormat=text/xml;schema%3D%22ioos/0.6.1%22
            String sosQuery1 = 
                "service=SOS&version=1.0.0&request=GetObservation" +
                "&offering=urn:ioos:Station:1.0.0.127.gomoosBuoy::A01" +
                "&observedProperty=gomoosBuoy" +
                "&responseFormat=text/csv" +
                "&eventTime=2009-08-25T00:00:00Z/2009-09-01T18:23:51Z";

            String2.log("\n+++ GetObservations for 1 station\n" + sosQuery1);
            baos = new ByteArrayOutputStream();
            osss = new OutputStreamSourceSimple(baos);
            eddTable.sosGetObservation(sosQuery1, null, osss, dir, "testGomoos");
            results = baos.toString("UTF-8");
            //String2.log(results);        
            expected = 
"longitude (deg{east}), latitude (deg{north}), station_id, altitude (m), time (UTC), air_temperature (Cel), chlorophyll (mg.m-3), direction_of_sea_water_velocity (deg{true}), dominant_wave_period (s), sea_level_pressure (mbar), sea_water_density (kg.m-3), sea_water_electrical_conductivity (S.m-1), sea_water_salinity ({psu}), sea_water_speed (cm.s-1), sea_water_temperature (Cel), wave_height (m), visibility_in_air (m), wind_from_direction (deg{true}), wind_gust (m.s-1), wind_speed (m.s-1)\n" +
"-70.5680705627165, 42.5221700538877, A01, 4.0, 2009-08-25T00:00:00Z, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 1.69000005722046, 3.05599999427795, 1.46200001239777\n" +
"-70.5680705627165, 42.5221700538877, A01, 3.0, 2009-08-25T00:00:00Z, 20.9699993133545, NaN, NaN, NaN, 1016.49780273438, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n" +
"-70.5680705627165, 42.5221700538877, A01, 0.0, 2009-08-25T00:00:00Z, NaN, NaN, NaN, 8.0, NaN, NaN, NaN, NaN, NaN, NaN, 1.00613415, NaN, NaN, NaN, NaN\n" +
"-70.5680705627165, 42.5221700538877, A01, -1.0, 2009-08-25T00:00:00Z, NaN, NaN, NaN, NaN, NaN, 21.1049213409424, 43.431999206543, 30.5694179534912, NaN, 21.0799999237061, NaN, NaN, NaN, NaN, NaN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results.substring(0, Math.min(5000, results.length())));
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * Test the Oostethys-style SOS data response using cwwcNDBCMet.
     */
    public static void testSosOostethys() throws Throwable {
        try {
            String2.log("\n*** EDDTable.testSosOostethys()");
            EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 
            String dir = EDStatic.fullTestCacheDirectory;
            String sosQuery, fileName, results, expected;
            java.io.StringWriter writer;
            ByteArrayOutputStream baos;
            OutputStreamSourceSimple osss;


            //*** observations for 1 station, all vars  CSV response
            String sosQuery1 = 
                "service=SOS&version=1.0.0&request=GetObservation" +
                "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet::41004" +
                "&observedProperty=cwwcNDBCMet" +
                "&responseFormat=application/com-xml" +
                "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
            String2.log("\n+++ GetObservations for 1 station \n" + sosQuery1);
            baos = new ByteArrayOutputStream();
            osss = new OutputStreamSourceSimple(baos);
            eddTable.sosGetObservation(sosQuery1, null, osss, dir, "testSos1Sta");
            results = baos.toString("UTF-8");
            String2.log(results);        
            expected = 
    //"longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
    //"degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n" +
    //"-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n" +
    //"-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n";
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<om:Observation\n" +
    "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
    "  xmlns:swe=\"http://www.opengis.net/swe/0\"\n" +
    "  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
    "  xmlns:om=\"http://www.opengis.net/om\"\n" +
    "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
    "  xsi:schemaLocation=\"http://www.opengis.net/om ../../../../../../ows4/schema0/swe/branches/swe-ows4-demo/om/current/commonObservation.xsd\"\n" +
    "  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n" +
    "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n" +
    "  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n" +
    "  <gml:name>NDBC Standard Meteorological Buoy Data, Station 41004</gml:name>\n" +
    "  <gml:location>\n" +
    "    <gml:Point gml:id=\"OBSERVATION_LOCATION\" srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
    "      <gml:coordinates>32.5 -79.09</gml:coordinates>\n" +
    "    </gml:Point>\n" +
    "  </gml:location>\n" +
    "  <om:time>\n" +
    "    <gml:TimePeriod gml:id=\"DATA_TIME\">\n" +
    "      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n" +
    "      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n" +
    "    </gml:TimePeriod>\n" +
    "  </om:time>\n" +
    "    <om:procedure xlink:href=\"urn:ioos:sensor:1.0.0.127.cwwcNDBCMet::41004:cwwcNDBCMet\"/>\n" +
    "    <om:observedProperty xlink:href=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n" +
    "    <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
    "    <om:resultDefinition>\n" +
    "        <swe:DataBlockDefinition>\n" +
    "            <swe:components name=\"cwwcNDBCMetDataFor41004\">\n" +
    "                <swe:DataRecord>\n" +
    "                    <swe:field name=\"time\">\n" +
    "                        <swe:Time definition=\"urn:ogc:phenomenon:time:iso8601\"/>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"latitude\">\n" +
    "                        <swe:Quantity definition=\"urn:ogc:phenomenon:latitude:wgs84\">\n" +
    "                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"longitude\">\n" +
    "                        <swe:Quantity definition=\"urn:ogc:phenomenon:longitude:wgs84\">\n" +
    "                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"wd\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wd\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#degrees_true\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"wspd\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspd\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"gst\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#gst\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"wvht\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wvht\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#m\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"dpd\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dpd\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#s\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"apd\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#apd\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#s\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"mwd\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#mwd\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#degrees_true\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"bar\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#bar\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#hPa\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"atmp\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#atmp\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#degree_C\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"wtmp\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wtmp\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#degree_C\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"dewp\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dewp\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#degree_C\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"vis\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#vis\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#km\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"ptdy\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#ptdy\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#hPa\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"tide\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#tide\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#m\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"wspu\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspu\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                    <swe:field name=\"wspv\">\n" +
    "                        <swe:Quantity definition=\"http://127.0.0.1:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspv\">\n" +
    "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n" +
    "                        </swe:Quantity>\n" +
    "                    </swe:field>\n" +
    "                </swe:DataRecord>\n" +
    "            </swe:components>\n" +
    "            <swe:encoding>\n" +
    "                <swe:AsciiBlock tokenSeparator=\",\" blockSeparator=\" \" decimalSeparator=\".\"/>\n" +
    "            </swe:encoding>\n" +
    "        </swe:DataBlockDefinition>\n" +
    "    </om:resultDefinition>\n" +
    "    <om:result>2008-08-01T00:00:00Z,32.5,-79.09,225,10.9,14.0,1.66,5.26,4.17,,1007.6,27.8,27.9,,,,,7.7,7.7 " +
                   "2008-08-01T01:00:00Z,32.5,-79.09,229,10.1,12.6,1.68,5.56,4.36,,1008.0,27.8,27.9,,,,,7.6,6.6</om:result>\n" +
    "</om:Observation>\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }



    /** This tests some EDDTable-specific things. */
    public static void test() throws Throwable {

        //tests of ERDDAP's SOS server are disabled
        //testSosGomoos();
        //testSosNdbcMet();
        //testSosOostethys();
    }

}
