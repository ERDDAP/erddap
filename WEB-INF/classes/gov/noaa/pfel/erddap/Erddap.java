/*
 * ERDDAP Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.CompoundColorMap;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

//import org.verisign.joid.consumer.OpenIdFilter;

/**
 * ERDDAP is a Java servlet which serves gridded and tabular data
 * in common data file formats (e.g., ASCII, .dods, .mat, .nc)
 * and image file formats (e.g., .pdf and .png). 
 *
 * <p> This works like an OPeNDAP DAP-style server conforming to the
 * DAP 2.0 spec (see the Documentation section at www.opendap.org). 
 *
 * <p>The authentication method is set by the authentication tag in setup.xml.
 * See its use below and in EDStatic.
 *
 * <p>Authorization is specified by roles tags and accessibleTo tags in datasets.xml.
 * <br>If a user isn't authorized to use a dataset, then EDStatic.listPrivateDatasets 
 *    determines whether the dataset appears on lists of datasets (e.g., categorize or search).
 * <br>If a user isn't authorized to use a dataset and requests info about that
 *    dataset, EDStatic.redirectToLogin is called.
 * <br>These policies are enforced by checking edd.isAccessibleTo results from 
 *    gridDatasetHashMap and tableDatasetHashMap
 *    (notably also via gridDatasetIDs, tableDatasetIDs, allDatasetIDs).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-20
 */
public class Erddap extends HttpServlet {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not by changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /** The programmatic/computer access to Erddap services are available as 
     * all of the plainFileTypes. 
     * All plainFileTypes must be valid EDDTable.dataFileTypeNames.
     * If added a new type, also add to sendPlainTable below and
     *  "//list of plainFileTypes" for rest.html.
     */
    public static String plainFileTypes[] = {
        //no need for .csvp or .tsvp, because plainFileTypes never write units
        ".csv", ".htmlTable", ".itx", ".json", ".jsonlCSV1", ".jsonlCSV", ".jsonlKVP", 
        ".mat", ".nc", ".nccsv", ".tsv", ".xhtml"};
    public static String plainFileTypesString = String2.toCSSVString(plainFileTypes);

    //version when new file types added
    public final static String FILE_TYPES_124[] = 
        //for old remote erddaps, make .png locally so pngInfo is available
        {".csvp", ".tsvp", "odvTxt", ".png"};
    public final static String FILE_TYPES_148[] = 
        {".csv0", ".tsv0"};
    public final static String FILE_TYPES_174[] = 
        {".itx"};
    public final static String FILE_TYPES_176[] = 
        {".jsonlCSV", ".jsonlKVP", ".nccsv", ".nccsvMetadata"};
    public final static String FILE_TYPES_184[] = 
        {".dataTable", ".jsonlCSV1"};


    //General/relative width is determined by what looks good in Chrome.
    //But Firefox shows TextArea's as very wide, so leads to these values.
    public final static int dpfTFWidth = 56; //data provider form TextField width
    public final static int dpfTAWidth = 58; //data provider form TextArea width

    // ************** END OF STATIC VARIABLES *****************************

    protected RunLoadDatasets runLoadDatasets;
    public AtomicInteger totalNRequests  = new AtomicInteger();
    public String lastReportDate = "";

    /** Set by loadDatasets. */
    /** datasetHashMaps are read from many threads and written to by loadDatasets, 
     * so need to synchronize these maps.
     * grid/tableDatasetHashMap are key=datasetID value=edd.
     * [See Projects.testHashMaps() which shows that ConcurrentHashMap gives
     * me a thread-safe class without the time penalty of Collections.synchronizedMap(new HashMap()).]
     */
    public ConcurrentHashMap<String,EDDGrid>  gridDatasetHashMap  = new ConcurrentHashMap(16, 0.75f, 4); 
    public ConcurrentHashMap<String,EDDTable> tableDatasetHashMap = new ConcurrentHashMap(16, 0.75f, 4); 
    /** The RSS info: key=datasetId, value=utf8 byte[] of rss xml */
    public ConcurrentHashMap<String,byte[]> rssHashMap  = new ConcurrentHashMap(16, 0.75f, 4); 
    public ConcurrentHashMap<String,int[]> failedLogins = new ConcurrentHashMap(16, 0.75f, 4); 
    public ConcurrentHashMap<String,ConcurrentHashMap> categoryInfo = new ConcurrentHashMap(16, 0.75f, 4);  
    public long lastClearedFailedLogins = System.currentTimeMillis();


    /**
     * The constructor.
     *
     * <p> This needs to find the content/erddap directory.
     * It may be a defined environment variable ("erddapContentDirectory"),
     * but is usually a subdir of <tomcat> (e.g., usr/local/tomcat/content/erddap/).
     *
     * <p>This redirects logging messages to the log.txt file in bigParentDirectory 
     * (specified in <tomcat>/content/erddap/setup.xml) or to a CommonsLogging file.
     * This is appropriate for use as a web service. 
     *
     * @throws Throwable if trouble
     */
    public Erddap() throws Throwable {
        long constructorMillis = System.currentTimeMillis();

        //rename log.txt to preserve it so it can be analyzed if there was trouble before restart
        //In timestamp, change ':' to '.' so suitable for file names
        String timeStamp = String2.replaceAll(Calendar2.getCurrentISODateTimeStringLocal(), ":", ".");
        String newLogTxt = EDStatic.fullLogsDirectory  + "log.txt";
        String BPD = EDStatic.bigParentDirectory;
        try {
            String oldLogTxt = BPD + "log.txt";
            String logTextAr = EDStatic.fullLogsDirectory  + "logArchivedAt" + 
                               timeStamp + ".txt";
            if (File2.isFile(oldLogTxt)) {
                //pre ERDDAP version 1.15
                File2.copy(oldLogTxt, logTextAr);
                File2.delete(oldLogTxt);
            } 
            if (File2.isFile(newLogTxt))
                File2.rename(newLogTxt, logTextAr);
        } catch (Throwable t) {
            String2.log("WARNING: " + MustBe.throwableToString(t));
        }
        try {
            //rename log.txt.previous to preserve it so it can be analyzed if there was trouble before restart
            String oldLogTxtP = BPD + "log.txt.previous";
            String newLogTxtP = EDStatic.fullLogsDirectory  + "log.txt.previous";
            String logTextArP = EDStatic.fullLogsDirectory  + "logPreviousArchivedAt" + 
                                timeStamp + ".txt";
            if (File2.isFile(oldLogTxtP)) {
                //pre ERDDAP version 1.15
                File2.copy(oldLogTxtP, logTextArP);
                File2.delete(oldLogTxtP);
            }
            if (File2.isFile(newLogTxtP))
                File2.rename(newLogTxtP, logTextArP);
        } catch (Throwable t) {
            String2.log("WARNING: " + MustBe.throwableToString(t));
        }

        //open String2 log system and log to BPD/logs/log.txt
        String2.setupLog(false, false, //tLogToSystemOut, tLogToSystemErr,
            newLogTxt, true, EDStatic.logMaxSizeMB * Math2.BytesPerMB); //fileName, append, maxSize
        String2.log("\n\\\\\\\\**** Start Erddap v" + EDStatic.erddapVersion + 
            " constructor at " + timeStamp + "\n" +
            "logFile=" + String2.logFileName() + " logMaxSizeMB=" + EDStatic.logMaxSizeMB + "\n" +
            String2.standardHelpAboutMessage() + "\n" +
            "verbose=" + verbose + " reallyVerbose=" + reallyVerbose + "\n" +
            "bigParentDirectory=" + BPD + "\n" +
            "contextDirectory=" + EDStatic.contextDirectory + "\n" +
            "available fonts=" + String2.toCSSVString(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));         

        //on start up, always delete all files from fullPublicDirectory and fullCacheDirectory
        File2.deleteAllFiles(EDStatic.fullPublicDirectory, true, false);  //recursive, deleteEmptySubdirectories 
        File2.deleteAllFiles(EDStatic.fullCacheDirectory,  true, false);  //in EDStatic, was true, true, but then subdirs created
        //delete cache subdirs other than starting with "_" (i.e., the dataset dirs, not _test)
        String tFD[] = (new File(EDStatic.fullCacheDirectory)).list();
        StringBuilder rdErrors = new StringBuilder();
        for (int i = 0; i < tFD.length; i++) {
            String fd = tFD[i];
            if (fd != null && fd.length() > 0 && !fd.startsWith("_") && 
                File2.isDirectory(EDStatic.fullCacheDirectory + fd)) {
                try {
                    rdErrors.append(RegexFilenameFilter.recursiveDelete(EDStatic.fullCacheDirectory + fd));
                } catch (Throwable t) {
                    rdErrors.append("ERROR in recursiveDelete(" + EDStatic.fullCacheDirectory + fd + "):\n" + 
                        MustBe.throwableToString(t));
                }
            }
        }
        if (rdErrors.length() > 0) 
            EDStatic.email(EDStatic.emailEverythingToCsv, 
                "Unable to completely clean " + EDStatic.fullCacheDirectory, rdErrors.toString());

        //copy (not rename!) subscriptionsV1.txt to preserve it 
        try {
            String subTxt = BPD + "subscriptionsV1.txt";
            if (File2.isFile(subTxt))
                File2.copy(subTxt, BPD + "subscriptionsV1ArchivedAt" + 
                    timeStamp + ".txt");
        } catch (Throwable t) {
            String2.log("WARNING: " + MustBe.throwableToString(t));
        }

        //get rid of old "private" directory (as of 1.14, ERDDAP uses fullCacheDirectory instead)
        File2.deleteAllFiles(BPD + "private", true, true); //empty it
        File2.delete(        BPD + "private"); //delete it

        //initialize Lucene
        if (EDStatic.useLuceneSearchEngine) 
            EDStatic.initializeLucene();

        //make subscriptions
        if (EDStatic.subscriptionSystemActive) 
            EDStatic.subscriptions = new Subscriptions(
                BPD + "subscriptionsV1.txt", 48, //maxHoursPending, 
                EDStatic.preferredErddapUrl); //prefer https url                

        //copy all <contentDirectory>images/ (and subdirectories) files to imageDir (and subdirectories)
        String imageFiles[] = RegexFilenameFilter.recursiveFullNameList(
            EDStatic.contentDirectory + "images/", ".+", false);
        for (int i = 0; i < imageFiles.length; i++) {
            int tpo = imageFiles[i].indexOf("/images/");
            if (tpo < 0) tpo = imageFiles[i].indexOf("\\images\\");
            if (tpo < 0) {
                String2.log("'/images/' not found in images/ file: " + imageFiles[i]);
                continue;
            }
            String tName = imageFiles[i].substring(tpo + 8);
            if (verbose) String2.log("  copying images/ file: " + tName);
            File2.copy(EDStatic.contentDirectory + "images/" + tName,  EDStatic.imageDir + tName);
        }

        //ensure images exist and get their sizes
        Image tImage = Image2.getImage(EDStatic.imageDir + 
            EDStatic.lowResLogoImageFile, 10000, false);
        EDStatic.lowResLogoImageFileWidth   = tImage.getWidth(null);
        EDStatic.lowResLogoImageFileHeight  = tImage.getHeight(null);
        tImage = Image2.getImage(EDStatic.imageDir + EDStatic.highResLogoImageFile, 10000, false);
        EDStatic.highResLogoImageFileWidth  = tImage.getWidth(null);
        EDStatic.highResLogoImageFileHeight = tImage.getHeight(null);
        tImage = Image2.getImage(EDStatic.imageDir + EDStatic.googleEarthLogoFile, 10000, false);
        EDStatic.googleEarthLogoFileWidth   = tImage.getWidth(null);
        EDStatic.googleEarthLogoFileHeight  = tImage.getHeight(null);

        //make new catInfo with first level hashMaps
        int nCat = EDStatic.categoryAttributes.length;
        for (int cat = 0; cat < nCat; cat++) 
            categoryInfo.put(EDStatic.categoryAttributes[cat], 
                new ConcurrentHashMap(16, 0.75f, 4));

        //start RunLoadDatasets
        runLoadDatasets = new RunLoadDatasets(this);
        EDStatic.runningThreads.put("runLoadDatasets", runLoadDatasets); 
        runLoadDatasets.start(); 

        //done
        String2.log("\n\\\\\\\\**** Erddap constructor finished. TIME=" +
            (System.currentTimeMillis() - constructorMillis) + "ms");
    }

    /**
     * destroy() is called by Tomcat whenever the servlet is removed from service.
     * See example at [was http://classes.eclab.byu.edu/462/demos/PrimeSearcher.java ]
     *
     * <p> Erddap doesn't overwrite HttpServlet.init(servletConfig), but it could if need be. 
     * runLoadDatasets is created by the Erddap constructor.
     */
    public void destroy() {
        EDStatic.destroy();
    }

    /**
     * This returns a StringArray with all the datasetIDs for all of the grid datasets (unsorted).
     *
     * @return a StringArray with all the datasetIDs for all of the grid datasets (unsorted.
     */
    public StringArray gridDatasetIDs() {
        return new StringArray(gridDatasetHashMap.keys()); 
    }
    
    /**
     * This returns a StringArray with all the datasetIDs for all of the table datasets (unsorted).
     *
     * @return a StringArray with all the datasetIDs for all of the table datasets (unsorted).
     */
    public StringArray tableDatasetIDs() {
        return new StringArray(tableDatasetHashMap.keys()); 
    }
    
    /**
     * This returns a StringArray with all the datasetIDs for all of the datasets (unsorted).
     *
     * @return a StringArray with all the datasetIDs for all of the datasets (unsorted).
     */
    public StringArray allDatasetIDs() {
        StringArray sa  = new StringArray(gridDatasetHashMap.keys()); 
        sa.append(new StringArray(tableDatasetHashMap.keys()));
        return sa;
    }
   

    /**
     * This returns the category values (sortIgnoreCase) for a given category attribute.
     * 
     * @param attribute e.g., "institution"
     * @return the category values for a given category attribute (or empty StringArray if none).
     */
    public StringArray categoryInfo(String attribute) {
        ConcurrentHashMap hm = categoryInfo.get(attribute);
        if (hm == null)
            return new StringArray();
        StringArray sa  = new StringArray(hm.keys());
        sa.sortIgnoreCase();
        return sa;
    }
    
    /**
     * This returns the datasetIDs (sortIgnoreCase) for a given category value for a given category attribute.
     * 
     * @param attribute e.g., "institution"
     * @param value e.g., "NOAA_NDBC"
     * @return the datasetIDs for a given category value for a given category 
     *    attribute (or empty StringArray if none).
     */
    public StringArray categoryInfo(String attribute, String value) {
        ConcurrentHashMap hm = categoryInfo.get(attribute);
        if (hm == null)
            return new StringArray();
        ConcurrentHashMap hs = (ConcurrentHashMap)hm.get(value);
        if (hs == null)
            return new StringArray();
        StringArray sa  = new StringArray(hs.keys());
        sa.sortIgnoreCase();
        return sa;
    }

    /**
     * This responds to a "post" request from the user by extending HttpServlet's doPost
     * and passing the request to doGet.
     *
     * @param request 
     * @param response
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        doGet(request, response);
    }

    /** 
     * This responds to a "get" request from the user by extending HttpServlet's doGet.
     * Mostly, this just identifies the protocol (e.g., "tabledap") in the requestUrl
     * (right after the warName) and calls doGet&lt;Protocol&gt; to handle
     * the request. That allows Erddap to work like a DAP server, or a WCS server,
     * or a ....
     *
     * @param request
     * @param response
     * @throws ServletException, IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        long doGetTime = System.currentTimeMillis();
        int requestNumber = totalNRequests.incrementAndGet();
        String ipAddress = "NotSetYet"; //won't be null

        try {

            //get loggedInAs
            String loggedInAs = EDStatic.getLoggedInAs(request);
            {
                String tLoggedInAs = loggedInAs == null?          "no/http" : 
                    loggedInAs.equals(EDStatic.loggedInAsHttps)?  EDStatic.loggedInAsHttps : "yes";
                EDStatic.tally.add("Requester Is Logged In (since startup)", tLoggedInAs);
                EDStatic.tally.add("Requester Is Logged In (since last daily report)", tLoggedInAs);
            }

            String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
            String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl(), pre "?"
            //String2.log("requestURL=" + requestUrl); 

            //get requester's ip addresses (x-forwarded-for)
            //getRemoteHost(); returns our proxy server (never changes)
            //For privacy reasons, don't tally full individual IP address; the 4th ip number is removed.
            ipAddress = request.getHeader("x-forwarded-for");  
            if (ipAddress == null) {
                ipAddress = "";
            } else {
                //if csv, get last part
                //see https://en.wikipedia.org/wiki/X-Forwarded-For
                int cPo = ipAddress.lastIndexOf(',');
                if (cPo >= 0)
                    ipAddress = ipAddress.substring(cPo + 1);
            }
            ipAddress = ipAddress.trim();
            if (ipAddress.length() == 0)
                ipAddress = "(unknownIPAddress)";

            //get userQuery
            String userQuery = request.getQueryString(); //may be null;  leave encoded
            if (userQuery == null)
                userQuery = "";
            String2.log("{{{{#" + requestNumber + " " +
                Calendar2.getCurrentISODateTimeStringLocalTZ() + " " + 
                (loggedInAs == null? "(notLoggedIn)" : loggedInAs) + " " +
                ipAddress + " " +
                request.getMethod() + " " + 
                requestUrl + 
                (requestUrl.endsWith("login.html") && userQuery.indexOf("nonce=") >= 0?
                    "?[CONFIDENTIAL]" : 
                    EDStatic.questionQuery(userQuery)));

            //refuse request? e.g., to fend of a Denial of Service attack or an overzealous web robot
            int periodPo = ipAddress.lastIndexOf('.'); //to make #.#.#.* test below for IP v4 address
            if (periodPo < 0)
                periodPo = ipAddress.lastIndexOf(':'); //to make #:#:#:#:#:#:#:* test below for IP v6 address
            if (EDStatic.requestBlacklist != null &&
                (EDStatic.requestBlacklist.contains(ipAddress) ||
                 (periodPo >= 0 && EDStatic.requestBlacklist.contains(ipAddress.substring(0, periodPo+1) + "*")))) {
                //use full ipAddress, to help id user                //odd capitilization sorts better
                EDStatic.tally.add("Requester's IP Address (Blacklisted) (since last Major LoadDatasets)", ipAddress);
                EDStatic.tally.add("Requester's IP Address (Blacklisted) (since last daily report)", ipAddress);
                EDStatic.tally.add("Requester's IP Address (Blacklisted) (since startup)", ipAddress);
                String2.log("}}}}#" + requestNumber + " Requester is on the datasets.xml requestBlacklist.");
                if (EDStatic.slowDownTroubleMillis > 0)
                    Math2.sleep(EDStatic.slowDownTroubleMillis);
                EDStatic.lowSendError(response, HttpServletResponse.SC_FORBIDDEN, //a.k.a. Error 403
                    MessageFormat.format(EDStatic.blacklistMsg, EDStatic.adminEmail));
                return;
            }

            //tally ipAddress                                    //odd capitilization sorts better
            EDStatic.tally.add("Requester's IP Address (Allowed) (since last Major LoadDatasets)", ipAddress);
            EDStatic.tally.add("Requester's IP Address (Allowed) (since last daily report)", ipAddress);
            EDStatic.tally.add("Requester's IP Address (Allowed) (since startup)", ipAddress);

            //requestUrl should start with /erddap/
            //deal with /erddap
            //??? '\' on windows computers??? or '/' since it isn't a real directory?
            if (!requestUrl.startsWith("/" + EDStatic.warName + "/")) {
                sendRedirect(response, tErddapUrl + "/index.html");
                return;
            }
            int protocolStart = EDStatic.warName.length() + 2;            

            //get protocol (e.g., "griddap" or "tabledap")
            int protocolEnd = requestUrl.indexOf("/", protocolStart);
            if (protocolEnd < 0)
                protocolEnd = requestUrl.length();
            String protocol = requestUrl.substring(protocolStart, protocolEnd);
            String endOfRequest = requestUrl.substring(protocolStart);
            if (reallyVerbose) String2.log("  protocol=" + protocol);

            //Pass the query to the requested protocol or web page.
            //Be as restrictive as possible (so resourceNotFound can be caught below, if possible).
            if (protocol.equals("griddap") ||
                protocol.equals("tabledap")) {
                doDap(request, response, loggedInAs, protocol, protocolEnd + 1, userQuery);
            } else if (protocol.equals("files")) {
                doFiles(request, response, loggedInAs, protocolEnd + 1, userQuery);
            } else if (protocol.equals("sos")) {
                doSos(request, response, loggedInAs, protocolEnd + 1, userQuery); 
            //} else if (protocol.equals("wcs")) {
            //    doWcs(request, response, loggedInAs, protocolEnd + 1, userQuery); 
            } else if (protocol.equals("wms")) {
                doWms(request, response, loggedInAs, protocolEnd + 1, userQuery);
            } else if (endOfRequest.equals("") || endOfRequest.equals("index.htm")) {
                sendRedirect(response, tErddapUrl + "/index.html");
            } else if (protocol.startsWith("index.")) {
                doIndex(request, response, loggedInAs);

            } else if (protocol.equals("download") ||
                       protocol.equals("images") ||
                       protocol.equals("public")) {
                doTransfer(request, response, protocol, protocolEnd + 1);
            } else if (protocol.equals("metadata")) {
                doMetadata(request, response, loggedInAs, endOfRequest, userQuery);
            } else if (protocol.equals("rss")) {
                doRss(request, response, loggedInAs, protocol, protocolEnd + 1);
            } else if (endOfRequest.startsWith("search/advanced.")) {  //before test for "search"
                doAdvancedSearch(request, response, loggedInAs, protocolEnd + 1, userQuery);
            } else if (protocol.equals("search")) {
                doSearch(request, response, loggedInAs, protocol, protocolEnd + 1, userQuery);
            } else if (protocol.equals("opensearch1.1")) {
                doOpenSearch(request, response, loggedInAs, protocol, protocolEnd + 1, userQuery);
            } else if (protocol.equals("categorize")) {
                doCategorize(request, response, loggedInAs, protocol, protocolEnd + 1, userQuery);
            } else if (protocol.equals("info")) {
                doInfo(request, response, loggedInAs, protocol, protocolEnd + 1);
            } else if (endOfRequest.equals("information.html")) {
                doInformationHtml(request, response, loggedInAs);
            } else if (endOfRequest.equals("legal.html")) {
                doLegalHtml(request, response, loggedInAs);
            } else if (endOfRequest.equals("login.html") && EDStatic.authentication.length() > 0) {
                doLogin(request, response, loggedInAs);
            } else if (endOfRequest.equals("loginGoogle.html") &&  
                (EDStatic.authentication.equals("google") || EDStatic.authentication.equals("oauth2"))) {
                doLoginGoogle(request, response, loggedInAs);
            } else if (endOfRequest.equals("loginOrcid.html") &&  
                (EDStatic.authentication.equals("orcid") || EDStatic.authentication.equals("oauth2"))) {
                doLoginOrcid(request, response, loggedInAs);
            } else if (endOfRequest.equals("logout.html") && EDStatic.authentication.length() > 0) {
                doLogout(request, response, loggedInAs);
            } else if (endOfRequest.equals("rest.html")) {
                doRestHtml(request, response, loggedInAs);
            } else if (protocol.equals("rest")) {  
                doGeoServicesRest(request, response, loggedInAs, endOfRequest, userQuery);
            } else if (endOfRequest.equals("setDatasetFlag.txt")) {
                doSetDatasetFlag(ipAddress, request, response, userQuery);
            } else if (endOfRequest.equals("sitemap.xml")) {
                doSitemap(request, response);
            } else if (endOfRequest.equals("slidesorter.html")) {
                doSlideSorter(request, response, loggedInAs, userQuery);
            } else if (endOfRequest.equals("status.html")) {
                doStatus(request, response, loggedInAs);
            } else if (endOfRequest.startsWith("dataProviderForm")) {
                if (!EDStatic.dataProviderFormActive) 
                    sendResourceNotFoundError(request, response, 
                        MessageFormat.format(EDStatic.disabled, "Data Provider Form"));
                else if (endOfRequest.equals("dataProviderForm.html")) 
                    doDataProviderForm(request, response, loggedInAs);
                else if (endOfRequest.equals("dataProviderForm1.html")) 
                    doDataProviderForm1(request, response, loggedInAs, ipAddress);
                else if (endOfRequest.equals("dataProviderForm2.html")) 
                    doDataProviderForm2(request, response, loggedInAs, ipAddress);
                else if (endOfRequest.equals("dataProviderForm3.html")) 
                    doDataProviderForm3(request, response, loggedInAs, ipAddress);
                else if (endOfRequest.equals("dataProviderForm4.html")) 
                    doDataProviderForm4(request, response, loggedInAs, ipAddress);
                else if (endOfRequest.equals("dataProviderFormDone.html")) 
                    doDataProviderFormDone(request, response, loggedInAs);
                else sendResourceNotFoundError(request, response, "The first subdirectory or file in the request URL doesn't exist.");

            } else if (protocol.equals("subscriptions")) {
                doSubscriptions(request, response, loggedInAs, ipAddress, endOfRequest, 
                    protocol, protocolEnd + 1, userQuery);
            } else if (protocol.equals("convert")) {
                doConvert(request, response, loggedInAs, endOfRequest, 
                    protocolEnd + 1, userQuery);
            } else if (endOfRequest.startsWith("outOfDateDatasets.")) {
                doOutOfDateDatasets(request, response, loggedInAs, endOfRequest, userQuery);
            } else if (endOfRequest.equals("version")) {
                doVersion(request, response);
            } else if (endOfRequest.equals("version_string")) {
                doVersionString(request, response);
            } else {
                sendResourceNotFoundError(request, response, "The first subdirectory or file in the request URL doesn't exist.");
            }
            
            //tally
            EDStatic.tally.add("Protocol (since startup)", protocol);
            EDStatic.tally.add("Protocol (since last daily report)", protocol);

            long responseTime = System.currentTimeMillis() - doGetTime;
            String2.distribute(responseTime, EDStatic.responseTimesDistributionLoadDatasets);
            String2.distribute(responseTime, EDStatic.responseTimesDistribution24);
            String2.distribute(responseTime, EDStatic.responseTimesDistributionTotal);
            if (verbose) String2.log("}}}}#" + requestNumber + " SUCCESS. TIME=" + responseTime + "ms\n");

        } catch (Throwable t) {

            try {
                
                int slowdown = 0;
                if (EDStatic.isClientAbortException(t)) 
                    String2.log("#" + requestNumber + " Error: ClientAbortException");
                else slowdown = EDStatic.slowDownTroubleMillis;

                //"failure" includes clientAbort and there is no data
                long responseTime = System.currentTimeMillis() - doGetTime;
                EDStatic.tally.add("Requester's IP Address (Failed) (since last Major LoadDatasets)", ipAddress);
                EDStatic.tally.add("Requester's IP Address (Failed) (since last daily report)", ipAddress);
                EDStatic.tally.add("Requester's IP Address (Failed) (since startup)", ipAddress);
                String2.distribute(responseTime, EDStatic.failureTimesDistributionLoadDatasets);
                String2.distribute(responseTime, EDStatic.failureTimesDistribution24);
                String2.distribute(responseTime, EDStatic.failureTimesDistributionTotal);
                if (slowdown > 0) //before log FAILURE, so sendErrorCode logged info is close by
                    Math2.sleep(slowdown);
                if (verbose) String2.log("#" + requestNumber + " FAILURE. TIME=" + responseTime + "ms");

            } catch (Throwable t2) {
                String2.log("Error while handling error:\n" + MustBe.throwableToString(t2));
            }

            //if sendErrorCode fails because response.isCommitted(), it throws ServletException
            EDStatic.sendError(request, response, t); 

            if (verbose) String2.log("}}}}#" + requestNumber + " sendErrorCode done. Total TIME=" + 
                (System.currentTimeMillis() - doGetTime) + "ms\n");
        }

    }

    /** 
     * This responds to an /erddap/index.xxx request
     *
     * @param request
     * @param response
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @throws ServletException, IOException
     */
    public void doIndex(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDD.baseUrl, pre "?"

        //plain file types  
        for (int pft = 0; pft < plainFileTypes.length; pft++) { 

            //index.pft  - return a list of resources
            if (requestUrl.equals("/" + EDStatic.warName + "/index" + plainFileTypes[pft])) {

                String fileTypeName = File2.getExtension(requestUrl);
                EDStatic.tally.add("Main Resources List (since startup)", fileTypeName);
                EDStatic.tally.add("Main Resources List (since last daily report)", fileTypeName);
                Table table = new Table();
                StringArray resourceCol = new StringArray();
                StringArray urlCol = new StringArray();
                table.addColumn("Resource", resourceCol);
                table.addColumn("URL", urlCol);
                StringArray resources = new StringArray(
                    new String[] {"info", "search", "categorize", "griddap", "tabledap"});
                if (EDStatic.sosActive) resources.add("sos");
                if (EDStatic.wcsActive) resources.add("wcs");
                if (EDStatic.wmsActive) resources.add("wms");
                for (int r = 0; r < resources.size(); r++) {
                    resourceCol.add(resources.get(r));
                    urlCol.add(tErddapUrl + "/" + resources.get(r) + "/index" + fileTypeName +
                        "?" + EDStatic.defaultPIppQuery +
                        (resources.get(r).equals("search")? "&searchFor=" : ""));
                }
                sendPlainTable(loggedInAs, request, response, table, "Resources", fileTypeName);
                return;
            }
        }

        //only thing left should be erddap/index.html request
        if (!requestUrl.equals("/" + EDStatic.warName + "/index.html")) {
            sendResourceNotFoundError(request, response, "index.html expected");
            return;
        }

        //display main erddap index.html page 
        EDStatic.tally.add("Home Page (since startup)", ".html");
        EDStatic.tally.add("Home Page (since last daily report)", ".html");
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Home Page", out); 
        try {
            //set up the table
            writer.write(
                "<div class=\"wide_max_width\">" + //not standard_width
                "<table style=\"vertical-align:top; " +
                "width:100%; border:0; border-spacing:0px;\">\n" +
                "<tr>\n" +
                "<td style=\"width:60%;\" class=\"T\">\n");

            //*** left column: theShortDescription
            writer.write(EDStatic.theShortDescriptionHtml(tErddapUrl));

            //thin vertical line between text columns
            writer.write(
                "</td>\n" + 
                "<td style=\"width:1%;\">&nbsp;&nbsp;&nbsp;</td>\n" + //spacing to left of vertical line
                "<td style=\"width:1%;\" class=\"verticalLine\">&nbsp;&nbsp;&nbsp;</td>\n" + //thin vertical line + spacing to right
                "<td style=\"width:38%;\" class=\"T\">\n"); 

            //*** the right column: Get Started with ERDDAP
            writer.write(
                "<h2>" + EDStatic.getStartedHtml + "</h2>\n" +
                "<ul>");

            //display a search form
            writer.write("\n<li>");
            writer.write(getSearchFormHtml(request, loggedInAs, "<h3>", "</h3>", ""));

            //display /info link with list of all datasets
            writer.write(
                //here, just use rel=contents for the list of all datasets
                "\n<li><h3><a rel=\"contents\" href=\"" + tErddapUrl + "/info/index.html?" +
                    EDStatic.encodedDefaultPIppQuery + "\">" +
                MessageFormat.format(EDStatic.indexViewAll,  
                    //below is one of few places where number isn't converted to string 
                    //(so 1000's separator is used to format the number):
                    gridDatasetHashMap.size() + tableDatasetHashMap.size()) + //no: "" + 
                "</a></h3>\n");

            //display categorize options
            writer.write("\n<li>");
            writeCategorizeOptionsHtml1(request, loggedInAs, writer, null, true);

            //display Advanced Search option
            writer.write("\n<li><h3>" +
                MessageFormat.format(EDStatic.indexSearchWith, 
                    getAdvancedSearchLink(loggedInAs, EDStatic.defaultPIppQuery)) + 
                "</h3>\n");

            //display protocol links
            writer.write(
                "\n<li>" +
                "<h3>" + EDStatic.protocolSearchHtml + "</h3>\n" +
                EDStatic.protocolSearch2Html +
                //"<br>Click on a protocol to see a list of datasets which are available via that protocol in ERDDAP." +
                "<br>&nbsp;\n" +
                "<table class=\"erd commonBGColor\">\n" +
                "  <tr><th>" + EDStatic.indexProtocol    + "</th>" + 
                      "<th>" + EDStatic.indexDescription + "</th></tr>\n" +
                "  <tr>\n" +
                "    <td><a rel=\"bookmark\" " + 
                    "href=\"" + tErddapUrl + "/griddap/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\"" + 
                    " title=\"" + 
                    MessageFormat.format(EDStatic.protocolClick, "griddap") + "\">" +
                    MessageFormat.format(EDStatic.indexDatasets, "griddap") + "</a> </td>\n" +
                "    <td>" + EDStatic.EDDGridDapDescription + "\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/griddap/documentation.html\">" +
                    MessageFormat.format(EDStatic.indexDocumentation, "griddap") + 
                    "</a>\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><a rel=\"bookmark\" " + 
                    "href=\"" + tErddapUrl + "/tabledap/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\"" + 
                    " title=\"" + 
                    MessageFormat.format(EDStatic.protocolClick, "tabledap") + "\">" +
                    MessageFormat.format(EDStatic.indexDatasets, "tabledap") + "</a></td>\n" +
                "    <td>" + EDStatic.EDDTableDapDescription + "\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html\">" +
                    MessageFormat.format(EDStatic.indexDocumentation, "tabledap") + 
                    "</a>\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><a rel=\"bookmark\" " + 
                    "href=\"" + tErddapUrl + "/files/\"" + 
                    " title=\"" + 
                    MessageFormat.format(EDStatic.protocolClick, "files") + "\">" +
                    MessageFormat.format(EDStatic.indexDatasets, "\"files\"") + "</a></td>\n" +
                "    <td>" + EDStatic.filesDescription + " " + 
                    EDStatic.warning + " " + EDStatic.filesWarning + "\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/files/documentation.html\">" +
                    MessageFormat.format(EDStatic.indexDocumentation, "\"files\"") + 
                    "</a>\n" +
                "    </td>\n" +
                "  </tr>\n");
            if (EDStatic.sosActive) writer.write(
                "  <tr>\n" +
                "    <td><a rel=\"bookmark\" " +  
                    "href=\"" + tErddapUrl + "/sos/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\"" + 
                    " title=\"" +
                    MessageFormat.format(EDStatic.protocolClick, "SOS") + "\">" +
                    MessageFormat.format(EDStatic.indexDatasets, "SOS") + "</a></td>\n" +
                "    <td>" + EDStatic.sosDescriptionHtml + "\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/sos/documentation.html\">" +
                    MessageFormat.format(EDStatic.indexDocumentation, "SOS") + 
                    "</a>\n" +
                "    </td>\n" +
                "  </tr>\n");
            if (EDStatic.wcsActive) writer.write(
                "  <tr>\n" +
                "    <td><a rel=\"bookmark\" " + 
                    "href=\"" + tErddapUrl + "/wcs/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\"" + 
                    " title=\"" + 
                    MessageFormat.format(EDStatic.protocolClick, "WCS") + "\">" +
                    MessageFormat.format(EDStatic.indexDatasets, "WCS") + "</a></td>\n" +
                "    <td>" + EDStatic.wcsDescriptionHtml + "\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/wcs/documentation.html\">" +
                    MessageFormat.format(EDStatic.indexDocumentation, "WCS") + 
                    "</a>\n" +
                "    </td>\n" +
                "  </tr>\n");
            if (EDStatic.wmsActive) writer.write(
                "  <tr>\n" +
                "    <td><a rel=\"bookmark\" " +
                    "href=\"" + tErddapUrl + "/wms/index.html?" +
                    EDStatic.encodedDefaultPIppQuery + "\"" + 
                    " title=\"" + 
                    MessageFormat.format(EDStatic.protocolClick, "WMS") + "\">" +
                    MessageFormat.format(EDStatic.indexDatasets, "WMS") + "</a></td>\n" +
                "    <td>" + EDStatic.wmsDescriptionHtml + "\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/wms/documentation.html\">" +
                    MessageFormat.format(EDStatic.indexDocumentation, "WMS") + 
                    "</a>\n" +
                "    </td>\n" +
                "  </tr>\n");
            writer.write(
                "</table>\n" +
                "&nbsp;\n" +
                "\n");  

            //connections to OpenSearch and SRU
            writer.write(
                "<li><h3>" + EDStatic.indexDevelopersSearch + "</h3>\n" +
                "  <ul>\n" +
                "  <li><a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                    EDStatic.indexRESTfulSearch + "</a>\n" +
                "  <li><a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/allDatasets.html\">" + 
                    EDStatic.indexAllDatasetsSearch + "</a>\n" +
                "  <li><a rel=\"bookmark\" href=\"" + 
                    tErddapUrl + "/opensearch1.1/index.html\">" +
                    EDStatic.indexOpenSearch + "</a>\n" +
                "  </ul>\n" +
                "\n");

            //end of search/protocol options list
            writer.write(
                "\n</ul>\n" +
                "<p>&nbsp;<hr>\n");

            //converters
            if (EDStatic.convertersActive)
                writer.write(
                "<p><strong><a class=\"selfLink\" id=\"converters\" href=\"#converters\" rel=\"bookmark\">" + 
                    EDStatic.indexConverters + "</a></strong>\n" +
                "<br>" + EDStatic.indexDescribeConverters + "\n" +
                "<table class=\"erd commonBGColor\">\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/oceanicAtmosphericAcronyms.html\">Acronyms</a></td>\n" + 
                "    <td>" + EDStatic.convertOceanicAtmosphericAcronyms + "</td></tr>\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/fipscounty.html\">FIPS County Codes</a></td>\n" + 
                "    <td>" + EDStatic.convertFipsCounty + "</td></tr>\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/keywords.html\">Keywords</a></td>\n" + 
                "    <td>" + EDStatic.convertKeywords + "</td></tr>\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/time.html\">Time</a></td>\n" + 
                "    <td>" + EDStatic.convertTime + "</td></tr>\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/units.html\">Units</a></td>\n" + 
                "    <td>" + EDStatic.convertUnits + "</td></tr>\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/urls.html\">URLs</a></td>\n" + 
                "    <td>" + EDStatic.convertURLs + "</td></tr>\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/oceanicAtmosphericVariableNames.html\">Variable Names</a></td>\n" + 
                "    <td>" + EDStatic.convertOceanicAtmosphericVariableNames + "</td></tr>\n" +
                "</table>\n" + 
                "\n");

            //metadata
            if (EDStatic.fgdcActive || EDStatic.iso19115Active) {
                writer.write(
                    "<p><strong><a class=\"selfLink\" id=\"metadata\" href=\"#metadata\" rel=\"bookmark\">" + EDStatic.indexMetadata + "</a></strong>\n" +
                    "<br>");
                String fgdcLink = //&#8209; is a non-breaking hyphen
                    "<br><a rel=\"bookmark\" " + 
                    "href=\"" + tErddapUrl + "/" + EDStatic.fgdcXmlDirectory + 
                    "\">FGDC&nbsp;Web&nbsp;Accessible&nbsp;Folder&nbsp;(WAF)</a>\n" +
                    "with <a rel=\"help\" href=\"https://www.fgdc.gov/standards/projects/FGDC-standards-projects/metadata/base-metadata/index_html\"\n" +
                    ">FGDC&#8209;STD&#8209;001&#8209;1998" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>";
                String isoLink  =
                    "<br><a rel=\"bookmark\" " + 
                    "href=\"" + tErddapUrl + "/" + EDStatic.iso19115XmlDirectory +
                    "\">ISO&nbsp;19115&nbsp;Web&nbsp;Accessible&nbsp;Folder&nbsp;(WAF)</a>\n" +
                    "with <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Geospatial_metadata\"\n" +
                    ">ISO&nbsp;19115&#8209;2/19139" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>";
                if (EDStatic.fgdcActive && EDStatic.iso19115Active)
                    writer.write(MessageFormat.format(EDStatic.indexWAF2, 
                        fgdcLink, isoLink));
                else writer.write(MessageFormat.format(EDStatic.indexWAF1, 
                    EDStatic.fgdcActive? fgdcLink : isoLink));
                 writer.write("\n\n");
            }

            //REST services
            writer.write(
                "<p><strong><a class=\"selfLink\" id=\"services\" href=\"#services\" rel=\"bookmark\">" + EDStatic.indexServices + "</a></strong>\n" +
                "<br>" + MessageFormat.format(EDStatic.indexDescribeServices, tErddapUrl) + 
                "\n\n");

            //And
            writer.write(
                "<p><strong><a class=\"selfLink\" id=\"and\" href=\"#and\" rel=\"bookmark\">And</a></strong>\n" +
                "<table class=\"erd commonBGColor\">\n" +
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/status.html\">Status</a></td>\n" + 
                "    <td>" + EDStatic.statusHtml + "</td></tr>\n" +
                (EDStatic.outOfDateDatasetsActive?
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/outOfDateDatasets.html\">Out-Of-Date Datasets</a></td>\n" + 
                "    <td>" + EDStatic.outOfDateHtml + "</td></tr>\n" : "") +
                (EDStatic.subscriptionSystemActive?
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/subscriptions/index.html\">Subscription System</a></td>\n" + 
                "    <td>" + String2.replaceAll(EDStatic.subscription0Html, "<br>", " ") + "</td></tr>\n" : "") +
                (EDStatic.slideSorterActive?
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/slidesorter.html\">Slide Sorter</a></td>\n" + 
                "    <td>" + EDStatic.ssUsePlain + "</td></tr>\n" : "") +
                (EDStatic.dataProviderFormActive?
                "<tr><td><a rel=\"bookmark\" href=\"" + tErddapUrl + "/dataProviderForm.html\">Data Provider Form</a></td>\n" + 
                "    <td>The Data Provider Form is for people who have data and want it to be served by this ERDDAP.\n" +
                "      It collects basic information about the dataset and emails it to this ERDDAP's administrator.</td></tr>\n" : "") +
                "</table>\n\n");

            //end of table
            writer.write(
                "</td>\n</tr>\n</table>\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);   //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {

            //end of home page
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * This responds by sending out the "Information" Html page (EDStatic.theLongDescriptionHtml).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doInformationHtml(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs) throws Throwable {        

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);        
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Information", out);
        try {
            writer.write("<div class=\"standard_width\">\n");
            writer.write(EDStatic.youAreHere(loggedInAs, "Information"));
            writer.write(EDStatic.theLongDescriptionHtml(tErddapUrl));
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * This responds by sending out the legal.html page (setup.xml <legal>).
     *
     */
    public void doLegalHtml(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs) throws Throwable {        

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);        
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Legal Notices", out);
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "Legal Notices") +
                "<a rel=\"bookmark\" href=\"#disclaimers\">Disclaimers</a> | " +
                "<a rel=\"bookmark\" href=\"#privacyPolicy\">Privacy Policy</a> | " +
                "<a rel=\"bookmark\" href=\"#dataLicenses\">Data Licenses</a> | " +
                "<a rel=\"bookmark\" href=\"#contact\">Contact</a>\n" +
                "\n" +
                "<h2><a class=\"selfLink\" id=\"disclaimers\" href=\"#disclaimers\" rel=\"bookmark\">Disclaimers</a></h2>\n" +
                "\n" +
                EDStatic.standardGeneralDisclaimer + "\n\n" +
                EDStatic.legal(tErddapUrl));

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /** This is used by doLogin to add a failed login attempt to failedLogins */
    public void loginFailed(String user) {
        if (verbose) String2.log("loginFailed " + user);
        EDStatic.tally.add("Log in failed (since startup)", user);
        EDStatic.tally.add("Log in failed (since last daily report)", user);
        int ia[] = failedLogins.get(user);
        boolean wasNull = ia == null;
        if (wasNull)
            ia = new int[]{0,0};
        //update the count of recent failed logins
        ia[0]++;  
        //update the minute of the last failed login
        ia[1] = Math2.roundToInt(System.currentTimeMillis() / Calendar2.MILLIS_PER_MINUTE);  
        if (wasNull)
            failedLogins.put(user, ia);
    }

    /** This is used by doLogin when a users successfully logs in 
     *(to remove failed login attempts from failedLogins) */
    public void loginSucceeded(String user) {
        if (verbose) String2.log("loginSucceeded " + user);
        EDStatic.tally.add("Log in succeeded (since startup)", user);
        EDStatic.tally.add("Log in succeeded (since last daily report)", user);
        //erase any info about failed logins
        failedLogins.remove(user);

        //clear failedLogins ~ every ~48.3 hours (just larger than 48 hours (2880 min), 
        //  so it occurs at different times of day)
        //this prevents failedLogins from accumulating never-used-again userNames
        //at worst, someone who just failed 3 times now appears to have failed 0 times; no big deal
        //but do it after a success, not a failure, so even that is less likely
        if (lastClearedFailedLogins + 2897L * Calendar2.MILLIS_PER_MINUTE < System.currentTimeMillis()) {
            if (verbose) String2.log("clearing failedLogins (done every few days)");
            lastClearedFailedLogins = System.currentTimeMillis();
            failedLogins.clear();
        }

    }

    /** This returns the number of minutes until the user can try to log in 
     * again (0 = now, 10 is max temporarily locked out).
     */
    public int minutesUntilLoginAttempt(String user) {
        int ia[] = failedLogins.get(user);

        //no recent attempt?
        if (ia == null)
            return 0;

        //greater than 10 minutes since last attempt?
        int minutesSince = Math2.roundToInt(System.currentTimeMillis() / Calendar2.MILLIS_PER_MINUTE - ia[1]);
        int minutesToGo = Math.max(0, 10 - minutesSince);
        if (minutesToGo == 0) { 
            failedLogins.remove(user); //erase any info about failed logins
            return 0;
        }

        //allow login if <3 recent failures
        if (ia[0] < 3) {
            return 0;
        } else {
            EDStatic.tally.add("Log in attempt blocked temporarily (since startup)", user);
            EDStatic.tally.add("Log in attempt blocked temporarily (since last daily report)", user);
            if (verbose) String2.log("minutesUntilLoginAttempt=" + minutesToGo + " " + user);
            return minutesToGo;
        }
    }
      
    /**
     * This is the callback url for google authentication: loginGoogle.html .
     * This just handles verification of Google login. 
     * It doesn't display a web page or redirect to a web page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doLoginGoogle(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs) throws Throwable {

        String loginUrl = EDStatic.erddapHttpsUrl + "/login.html";

        //user is trying to log in
        String idtoken = request.getParameter("idtoken"); //from the POST'd info
        if (idtoken == null) {
            //If some human mistakenly comes to this URL, redirect them to login.html
            sendRedirect(response, loginUrl);
            return;
        }
                
        String email = null;
        try {
            //in case of error, make user not logged in
            HttpSession session = request.getSession(false); 
            if (session != null) {
                //it is stored on server.  user doesn't have access, so can't spoof it
                //  (except by guessing the sessionID number (a long) and storing a cookie with it?)
                session.removeAttribute("loggedInAs:" + EDStatic.warName); 
                session.invalidate(); //forget any related info
            }
            session = request.getSession(); //make one if one doesn't exist

            //see https://developers.google.com/identity/sign-in/web/backend-auth#verify-the-integrity-of-the-id-token
            //String2.log("idtoken=" + idtoken);  //long base64(?) encoded
            String json = SSR.postFormGetResponseString(
                //SSR.getUrlResponseStringUnchanged( //throws Exception
                "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idtoken);
            //String2.log("json=" + json); //it is as expected
            JSONTokener tokener = new JSONTokener(json);
            JSONObject jo = new JSONObject(tokener);
            //String2.log("jo=" + jo.toString()); //it is as expected
            String msg = "Invalid value in idtoken: ";
            email           = jo.optString("email");  //opt returns "" if not found
            String aud      = jo.optString("aud");
            String verified = jo.optString("email_verified");
            String expires  = jo.optString("exp"); 
            if (!EDStatic.googleClientID.equals(aud))  //ensure this is request for my server
                throw new SimpleException(msg + "unexpected aud=" + aud);
            if (email != null)
                email = email.toLowerCase(); //so case insensitive, to avoid trouble
            EDStatic.subscriptions.ensureEmailValid(email); //checks validity and emailBlacklist, throws exception
            //Don't check list of users. Allow anyone to log in.
            //if (!EDStatic.onListOfUsers(email))        //ensure it is a registered user
            //    throw new SimpleException(
            //        "That email address isn't on the list of users for this ERDDAP.\n" +
            //        "Contact " + EDStatic.adminContact() + " to ask to be added to the list.\n" +
            //        "Then try again.");
            if (!verified.equals("true"))                 //ensure verified=true 
                throw new SimpleException(msg + "verified=false");
            long expireSec = String2.parseLong(expires);  //ensure it isn't an out-of-date authentication
            if (expireSec == Long.MAX_VALUE ||  
                expireSec < System.currentTimeMillis() / 1000.0) //google's exp is an epochSeconds absolute time
                throw new SimpleException(msg + "expires=" + expires + " isn't valid.");

            //success
            session.setAttribute("loggedInAs:" + EDStatic.warName, email); 
            Math2.sleep(500); //give session changes time to take effect
            loginSucceeded(email);
            //sendRedirect(response, loginUrl + "?message=" + 
            //    SSR.minimalPercentEncode(EDStatic.loginSucceeded));
            return;

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log("Caught: " + MustBe.throwableToString(t));
            loginFailed(email == null? "(unknown)" : email); 
            //sendRedirect(response, loginUrl + "?message=" + 
            //    SSR.minimalPercentEncode(EDStatic.loginFailed + ": " + 
            //        MustBe.getShortErrorMessage(t)));
            return;
        }
    }  
    
    /**
     * This is the callback url for orcid authentication: loginOrcid.html .
     * This just handles verification of orcid login. 
     * It doesn't display a web page or redirect to a web page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doLoginOrcid(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs) throws Throwable {

        String loginUrl = EDStatic.erddapHttpsUrl + "/login.html";

        //user is trying to log in
        //documentation: https://members.orcid.org/api/integrate/orcid-sign-in
        String code = request.getParameter("code"); //from the POST'd info
        if (code == null) {
            //If some human mistakenly comes to this URL, redirect them to login.html
            sendRedirect(response, loginUrl);
            return;
        }
                
        String orcid = null;
        HttpSession session = null;
        try {
            //in case of error, make user not logged in
            session = request.getSession(false); //make one if one doesn't exist
            if (session != null) {
                //it is stored on server.  user doesn't have access, so can't spoof it
                //  (except by guessing the sessionID number (a long) and storing a cookie with it?)
                session.removeAttribute("loggedInAs:" + EDStatic.warName); 
                session.invalidate(); //forget any related info
            }
            session = request.getSession(); //make one if one doesn't exist

            //exchange the code for authentication info
            //String2.log("code=" + code);  //6 character 1-time-use code
            String json = SSR.postFormGetResponseString( //throws Exception
                "https://orcid.org/oauth/token?" +
                "client_id=" + EDStatic.orcidClientID + 
                "&client_secret=" + EDStatic.orcidClientSecret +
                "&grant_type=authorization_code" +
                "&redirect_uri=" + SSR.minimalPercentEncode(EDStatic.erddapHttpsUrl + "/loginOrcid.html") + 
                "&code=" + code);
            //example from their documentation: 
            //  {"access_token":"f5af9f51-07e6-4332-8f1a-c0c11c1e3728","token_type":"bearer",
            //   "refresh_token":"f725f747-3a65-49f6-a231-3e8944ce464d","expires_in":631138518,
            //   "scope":"/authorize","name":"Sofia Garcia","orcid":"0000-0001-2345-6789"}
            JSONTokener tokener = new JSONTokener(json);
            JSONObject jo = new JSONObject(tokener);
            String msg = "Invalid code from ORCID: ";
            String access_token  = jo.optString("access_token");  
            String token_type    = jo.optString("token_type");
            //String refresh_token = jo.optString("refresh_token");
            String expires_in    = jo.optString("expires_in");
            String scope         = jo.optString("scope");
            //String name          = jo.optString("name");
                   orcid         = jo.optString("orcid");
            String diagMsg = "Info from ORCID: " + json;
            //String2.log(diagMsg); //it is as expected
            if (access_token == null || !access_token.matches("[\\-0-9a-f]{36}")) {
                String2.log(diagMsg);
                throw new SimpleException(msg + "unexpected access_token.");
            }
            if (token_type == null || !token_type.equals("bearer")) {
                String2.log(diagMsg);
                throw new SimpleException(msg + "unexpected token_type.");
            }
            //refresh_token
            long expireSec = String2.parseLong(expires_in);  //ensure it isn't an out-of-date authentication
            if (expireSec == Long.MAX_VALUE ||
                expireSec <= 1) {  //631138518 ! I don't know what their units are. If epSec, that's 1989-12-31T20:15:18Z
                String2.log(diagMsg);
                throw new SimpleException(msg + "expires_in=" + expires_in + " isn't valid.");
            }
            if (scope == null || 
                (!scope.equals("/authenticate") &&  //what I see
                 !scope.equals("/authorize"))) {    //what their example shows
                String2.log(diagMsg);
                throw new SimpleException(msg + "unexpected scope.");
            }
            //name
            if (orcid == null || !orcid.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}")) {
                String2.log(diagMsg);
                throw new SimpleException(msg + "unexpected ORCID iD=" + orcid);
            }

            //success
            session.setAttribute("loggedInAs:" + EDStatic.warName, orcid); 
            Math2.sleep(500); //give session changes time to take effect
            loginSucceeded(orcid);
            sendRedirect(response, loginUrl + "?message=" + 
                SSR.minimalPercentEncode(EDStatic.loginSucceeded));
            return;
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            try {
                String2.log("Caught: " + MustBe.throwableToString(t));
                loginFailed(orcid == null? "(unknown)" : orcid); 
            } catch (Throwable t2) {
                String2.log("Caught t2: " + MustBe.throwableToString(t2));
            }
            sendRedirect(response, loginUrl + "?message=" + 
                SSR.minimalPercentEncode(EDStatic.loginFailed + ": " + 
                    MustBe.getShortErrorMessage(t)));
            return;
        }
    }  
    
    
    
    /**
     * This responds by prompting the user to login (e.g., login.html).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doLogin(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs) throws Throwable {

        //Special case: "loggedInAsHttps" is for using https without being logged in
        //so that https is used for erddapUrl substitutions, 
        //but &amp;loginInfo; indicates user isn't logged in.
        String tErddapUrl = EDStatic.erddapHttpsUrl;
        String loginUrl = tErddapUrl + "/login.html";
        String userQuery = request.getQueryString(); //may be null;  leave encoded
        String message = request.getParameter("message");
        String standoutMessage = message == null? "" :
            "<pre><span class=\"standoutColor\">" + 
            XML.encodeAsHTML(message) +  //encoding is important to avoid security problems (HTML injection)
            "</span></pre>\n";                   

        if (loggedInAs == null) {
            //if request was sent to http:, redirect to https:
            //hopefully headers and other info won't be lost in the redirect
            sendRedirect(response, loginUrl + EDStatic.questionQuery(userQuery));
            return;            
        }

        //*** CUSTOM
        if (EDStatic.authentication.equals("custom")) {

            //is user trying to log in?
            //use getParameter because form info should have been POST'd
            String user =     request.getParameter("user");
            String password = request.getParameter("password");
            //justPrintable is good security and makes EDStatic.loggedInAsSuperuser special
            if (user     != null) user     = String2.justPrintable(user);   
            if (password != null) password = String2.justPrintable(password);
            if (loggedInAs.equals(EDStatic.loggedInAsHttps) &&   //can't log in if already logged in                
                user != null && user.length() > 0 && password != null) {

                int minutesUntilLoginAttempt = minutesUntilLoginAttempt(user);
                if (minutesUntilLoginAttempt > 0) {
                    sendRedirect(response, loginUrl + "?message=" + 
                        SSR.minimalPercentEncode(MessageFormat.format(
                            EDStatic.loginAttemptBlocked, user, "" + minutesUntilLoginAttempt)));
                    return;
                }
                try {
                    if (EDStatic.doesPasswordMatch(user, password)) {
                        //valid login
                        HttpSession session = request.getSession(); //make one if one doesn't exist
                        //it is stored on server.  user doesn't have access, so can't spoof it
                        //  (except by guessing the sessionID number (a long) and storing a cookie with it?)
                        session.setAttribute("loggedInAs:" + EDStatic.warName, user); 
                        Math2.sleep(500); //give session changes time to take effect
                        loginSucceeded(user);
                        sendRedirect(response, loginUrl + "?message=" + 
                            SSR.minimalPercentEncode(EDStatic.loginSucceeded));
                        return;
                    } else {
                        //invalid login;  if currently logged in, logout
                        HttpSession session = request.getSession(false); //don't make one if one doesn't exist
                        if (session != null) {
                            session.removeAttribute("loggedInAs:" + EDStatic.warName);
                            session.invalidate(); //forget any related info
                            Math2.sleep(500); //give session changes time to take effect
                        }
                        loginFailed(user);
                        sendRedirect(response, loginUrl + "?message=" + 
                            SSR.minimalPercentEncode(
                            EDStatic.loginFailed + ": " + EDStatic.loginInvalid));
                        return;
                    }
                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    sendRedirect(response, loginUrl +
                        "?message=" + 
                        SSR.minimalPercentEncode(EDStatic.loginFailed + ": " + 
                            MustBe.getShortErrorMessage(t)));
                    return;
                }
            }

            //custom login.html
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, EDStatic.LogIn, out);
            try {
                writer.write("<div class=\"standard_width\">\n");
                writer.write(EDStatic.youAreHere(loggedInAs, EDStatic.LogIn));

                //show message from EDStatic.redirectToLogin (which redirects to here) or logout.html
                writer.write(standoutMessage);           

                writer.write(EDStatic.loginDescribeCustom);

                if (loggedInAs.equals(EDStatic.loggedInAsHttps)) {

                    String tProblems = String2.replaceAll(EDStatic.loginProblems, 
                        "&initialHelp;", 
                        EDStatic.loginProblemExact +
                        EDStatic.loginProblem3Times);
                    tProblems = String2.replaceAll(tProblems, "&info;", EDStatic.loginUserNameAndPassword); //it's in loginProblemExact
                    tProblems = String2.replaceAll(tProblems, "&erddapUrl;", tErddapUrl); //it's in cookies

                    //show the login form
                    writer.write(
                    "<p><strong>" + EDStatic.loginNot + "</strong>\n" +
                    EDStatic.loginPublicAccess +
                    //use POST, not GET, so that form params (password!) aren't in url (and so browser history, etc.)
                    "<form action=\"login.html\" method=\"post\" id=\"login_form\">\n" +  
                    "<p><strong>" + EDStatic.loginToLogIn + ":</strong>\n" +
                    "<table class=\"compact\">\n" +
                    "  <tr>\n" +
                    "    <td>" + EDStatic.loginUserName + ":&nbsp;</td>\n" +
                    "    <td><input type=\"text\" size=\"30\" value=\"\" name=\"user\" id=\"user\"/></td>\n" +
                    "  </tr>\n" +
                    "  <tr>\n" +
                    "    <td>" + EDStatic.loginPassword + ":&nbsp;</td>\n" + 
                    "    <td><input type=\"password\" size=\"20\" value=\"\" name=\"password\" id=\"password\" autocomplete=\"off\"/>\n" +
                    "      <input type=\"submit\" value=\"" + EDStatic.LogIn + "\"/></td>\n" +
                    "  </tr>\n" +
                    "</table>\n" +
                    "</form>\n" +
                    "\n" +
                    tProblems);

                } else {
                    //tell user he is logged in
                    writer.write("<p><span class=\"successColor\">" + EDStatic.loginAs + 
                        " <strong>" + loggedInAs + "</strong>.</span>\n" +
                        "(<a href=\"" + EDStatic.erddapHttpsUrl + "/logout.html\">" +
                        EDStatic.logout + "</a>)\n" +
                        "<p>" + EDStatic.loginBack + "\n" +
                        String2.replaceAll(EDStatic.loginProblemsAfter, "&second;", ""));
                }
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t;
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        //*** EMAIL
        //Biggest weakness:
        //  What if BadBob makes the initial request using GoodGeorge's email address 
        //   and is able to view the invitation email in transit?
        //  a) Not likely. Most email systems (e.g., gmail) offer end-to-end encryption.
        //  b) Unless the email is deleted before reaching GoodGeorge, 
        //    at least GoodGeorge will know when someone is doing this.
        //  c) ??? Offer a link to withdraw this invitation / log out that email address.
        if (EDStatic.authentication.equals("email")) {

            int offerValidMinutes = 15;
            //Is user submitting any info (from form or email)?
            String email = request.getParameter("email");  //from form and email
            String nonce = request.getParameter("nonce");  //from email
            //justPrintable is good security and makes EDStatic.loggedInAsSuperuser special
            if (email != null) email = String2.justPrintable(email).toLowerCase();  //so case insensitive, to avoid trouble
            if (nonce != null) nonce = String2.justPrintable(nonce);
            String testEmailValid = EDStatic.subscriptions.testEmailValid(email);

            //FIRST STEP: Is user submitting is user requesting the invitation email?
            //use getParameter because form info may have been POST'd
            if (!loggedInAs.equals(EDStatic.loggedInAsHttps)) {  //already logged in!
                //fall through

            } else if (email == null) {  //not yet trying to log in
                //fall through

            } else if (testEmailValid.length() > 0) {
                sendRedirect(response, loginUrl + "?message=" + SSR.minimalPercentEncode(testEmailValid));
                return;

            } else if (!EDStatic.onListOfUsers(email)) {               
                sendRedirect(response, loginUrl + "?message=" + 
                    SSR.minimalPercentEncode(
                        "That email address isn't on the list of users for this ERDDAP.\n" +
                        "Contact " + EDStatic.adminContact() + " to ask to be added to the list.\n" +
                        "Then try again."));
                return;

            } else if (nonce == null) {

                //too many login attempts?
                int minutesUntilLoginAttempt = minutesUntilLoginAttempt(email);
                if (minutesUntilLoginAttempt > 0) {
                    sendRedirect(response, loginUrl + "?message=" + 
                        SSR.minimalPercentEncode(MessageFormat.format(
                            EDStatic.loginAttemptBlocked, email, "" + minutesUntilLoginAttempt)));
                    return;
                }

                //send invitation email
                HttpSession session = request.getSession(); //make one if one doesn't exist
                //It is stored on server and tied to user's browser with a cookie.  
                //User doesn't have access to server side info, so can't spoof it.
                //  (except by guessing the sessionID number (a long) and storing a cookie with it?)
                long expires = System.currentTimeMillis() + offerValidMinutes * Calendar2.MILLIS_PER_MINUTE;  
                String newNonce = EDStatic.nonce(email + "\n" + expires).toLowerCase(); //ensure lowerCase
                String error = EDStatic.email(email, 
                    EDStatic.DONT_LOG_THIS_EMAIL + "Invitation to log into ERDDAP.", 
                    "ERDDAP received a request to log in " + email + " .\n" +
                    "If you didn't make this request, please contact the ERDDAP administrator,\n" +
                    EDStatic.adminIndividualName + " (email: " + EDStatic.adminEmail + "), to report this abuse.\n" +
                    "\n" +
                    "To log in to ERDDAP, click on this link\n" +
                    loginUrl + "?email=" + SSR.minimalPercentEncode(email) + "&nonce=" + newNonce + "\n" +
                    "(or copy and paste it into the address field of your web browser).\n" +
                    "This invitation is valid for " + offerValidMinutes + " minutes, until " + 
                        Calendar2.epochSecondsToIsoStringTZ(expires / 1000.0) + ", and\n" +
                    "is only valid in the same browser that you used to make the login request.");
                if (error.length() == 0) {
                    session.setAttribute("loggingInAs:" + EDStatic.warName, 
                        email + "\n" + expires + "\n" + newNonce);
                    Math2.sleep(500); //give session changes time to take effect
                    sendRedirect(response, loginUrl + "?message=" +  
                        SSR.minimalPercentEncode(
                            "Okay. An invitation to log in has been emailed to " + email + " .\n" +
                            "Wait for the email. Then click the link in the email to log in."));
                    return;
                } else {  //trouble
                    session.removeAttribute("loggingInAs:" + EDStatic.warName);
                    session.invalidate(); //forget any related info
                    Math2.sleep(500); //give session changes time to take effect
                    sendRedirect(response, loginUrl + "?message=" +  
                        SSR.minimalPercentEncode(error));
                }
                return;

            } else { 
                //does nonce match info stored in session?
                HttpSession session = request.getSession(false); //make one if one doesn't exist
                String info = session == null? "" : 
                    (String)session.getAttribute("loggingInAs:" + EDStatic.warName);
                String parts[] = String2.split(info, '\n');
                if (parts == null || parts.length != 3 ||  //no loggingInAs info
                    parts[0] == null || parts[1] == null || parts[2] == null ||
                    !email.equals(parts[0].toLowerCase()) ||  //wrong email?
                    String2.parseLong(parts[1]) == Long.MAX_VALUE || //shouldn't happen
                    System.currentTimeMillis() > String2.parseLong(parts[1]) || //waited too long?
                    !nonce.toLowerCase().equals(parts[2].toLowerCase())) {  //wrong nonce?
                    //failure
                    if (session != null) {
                        session.removeAttribute("loggingInAs:" + EDStatic.warName);
                        session.invalidate(); //forget any related info
                        Math2.sleep(500); //give session changes time to take effect
                    }
                    loginFailed(email);
                    sendRedirect(response, loginUrl + "?message=" +  
                        SSR.minimalPercentEncode(EDStatic.loginFailed));
                    return;

                } else {
                    //success
                    session.removeAttribute("loggingInAs:" + EDStatic.warName);
                    session.setAttribute(   "loggedInAs:"  + EDStatic.warName, email); 
                    Math2.sleep(500); //give session changes time to take effect
                    loginSucceeded(email);
                    sendRedirect(response, loginUrl + "?message=" + 
                        SSR.minimalPercentEncode(EDStatic.loginSucceeded));
                    return;
                }
            }

            //email login.html
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, EDStatic.LogIn, out);
            try {
                writer.write("<div class=\"standard_width\">\n");
                writer.write(EDStatic.youAreHere(loggedInAs, EDStatic.LogIn));

                //show message from EDStatic.redirectToLogin (which redirects to here) or logout.html
                writer.write(standoutMessage);           

                writer.write(EDStatic.loginDescribeEmail);

                if (loggedInAs.equals(EDStatic.loggedInAsHttps)) {

                    //show the login form
                    String tProblems = String2.replaceAll(EDStatic.loginProblems, 
                        "&initialHelp;", 
                        EDStatic.loginProblemSameBrowser + 
                        EDStatic.loginProblemExpire +
                        EDStatic.loginProblem3Times);
                    tProblems = String2.replaceAll(tProblems, "&offerValidMinutes;", "" + offerValidMinutes); //it's in expire
                    tProblems = String2.replaceAll(tProblems, "&erddapUrl;", tErddapUrl); //it's in cookies

                    writer.write(
                    "<p><strong>" + EDStatic.loginNot + "</strong>\n" +
                    EDStatic.loginPublicAccess +
                    //use POST, not GET, so that form params (password!) aren't in url (and so browser history, etc.)
                    "\n" +  
                    "<p><strong>" + EDStatic.loginToLogIn + ":</strong>\n" +
                    "<form action=\"login.html\" method=\"post\" id=\"login_form\">" +
                    "<table class=\"compact\">\n" +
                    "  <tr>\n" +
                    "    <td>" + EDStatic.loginYourEmailAddress + ":&nbsp;</td>\n" +
                    "    <td><input type=\"text\" size=\"60\" value=\"\" name=\"email\" id=\"email\"/>\n" +
                    "      <input type=\"submit\" value=\"" + EDStatic.LogIn + "\"/></td>\n" +
                    "  </tr>\n" +
                    "</table></form>\n" +
                    "\n" +
                    tProblems); 

                } else {
                    //tell user he is logged in
                    writer.write("<p><span class=\"successColor\">" + EDStatic.loginAs + 
                        " <strong>" + loggedInAs + "</strong>.</span>\n" +
                        "(<a href=\"" + EDStatic.erddapHttpsUrl + "/logout.html\">" +
                        EDStatic.logout + "</a>)\n" +
                        "<p>" + EDStatic.loginBack + "\n" +
                        String2.replaceAll(EDStatic.loginProblemsAfter, "&second;", ""));
                }
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t;
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }


        //*** google, orcid, oauth2
        boolean isGoogle = EDStatic.authentication.equals("google");
        boolean isOrcid  = EDStatic.authentication.equals("orcid");
        boolean isOauth2 = EDStatic.authentication.equals("oauth2");
        if (isGoogle || isOrcid || isOauth2) {

            //Google login.html
            //  https://developers.google.com/identity/sign-in/web/
            //ORCID 
            //  https://members.orcid.org/api/oauth/presenting-oauth
            //  https://members.orcid.org/sites/default/files/connect-button.txt
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, EDStatic.LogIn, 
                (isGoogle || isOauth2? 
                    "<meta name=\"google-signin-client_id\" " + 
                    "content=\"" + EDStatic.googleClientID + "\">\n" + 
                    "<script src=\"https://apis.google.com/js/platform.js\" async defer></script>\n" : ""), 
                out);

            try {
                writer.write("<div class=\"standard_width\">\n");
                writer.write(EDStatic.youAreHere(loggedInAs, EDStatic.LogIn));

                //show message from EDStatic.redirectToLogin (which redirects to here) or logout.html
                writer.write(standoutMessage);           

                writer.write(
                    isGoogle? EDStatic.loginDescribeGoogle :
                    isOrcid?  EDStatic.loginDescribeOrcid  :
                              EDStatic.loginDescribeOauth2);

                if (loggedInAs.equals(EDStatic.loggedInAsHttps)) {

                    //login page for google/orcid/oauth2
                    String tProblems = String2.replaceAll(EDStatic.loginProblems, 
                        "&initialHelp;", 
                        isGoogle? EDStatic.loginProblemGoogleAgain :
                        isOrcid?  EDStatic.loginProblemOrcidAgain  :
                                  EDStatic.loginProblemOauth2Again);
                    tProblems = String2.replaceAll(tProblems, "&erddapUrl;", tErddapUrl); //it's in cookies

                    //show the login button
                    HtmlWidgets widgets = new HtmlWidgets(false, //tHtmlTooltips, 
                        EDStatic.imageDir);
                    writer.write(
                    (isGoogle || isOauth2? 
                        "<script>\n" +
                        "  function onSignIn(googleUser) {\n" +
                        "    var id_token = googleUser.getAuthResponse().id_token;\n" +
                        "    var xhr = new XMLHttpRequest();\n" +
                        //loginGoogle.html just handles setting session info.
                        //It isn't a web page and the user isn't redirected there.
                        "    xhr.open('POST', '" + EDStatic.erddapHttpsUrl + "/loginGoogle.html');\n" +
                        "    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');\n" +
                        "    xhr.onload = function() {\n" +
                        "      console.log('Signed in as: ' + xhr.responseText);\n" +                    
                        "    };\n" +
                        "    xhr.send('idtoken=' + id_token);\n" +
                        "  }\n" +
                        "</script>\n" : "") +
                    "\n" +
                    "<p><strong>" + EDStatic.loginNot + "</strong>\n" +
                    EDStatic.loginPublicAccess +
                    "<p>" + EDStatic.loginToLogIn + ":\n" +
                    "<ul>\n" +
                    (isGoogle || isOauth2?
                        "<li>" + EDStatic.loginGoogleSignIn + "\n" +
                        "  <div id=\"gSignInButton\" class=\"g-signin2\" data-onsuccess=\"onSignIn\"></div>\n" +
                        "  <br>" + EDStatic.loginGoogleSignIn2 + "\n" +
                            widgets.htmlButton("button", "loginToERDDAP", "", "", 
                                EDStatic.loginErddap, 
                                "onclick='window.location.assign(\"" + 
                                loginUrl + "\")'") +
                            "\n<br>&nbsp;\n" : "") + //don't say succeeded. It only succeeds if user successfully signed into Google.
                    (isOrcid || isOauth2?
                        //link to orcid web page to enter user's orcid and request authorization
                        //Orcid web page then redirects user to redirect_uri (loginOrcid.html) with one-time-use 6-digit code
                        "<li>" + (isOauth2? EDStatic.orComma + " " : "") +
                            "<a rel=\"help\" href=\"https://orcid.org/oauth/authorize?" +
                            "client_id=" + EDStatic.orcidClientID +      //and add & to start of next line
                            "&response_type=code" +
                            "&scope=/authenticate" +
                            "&redirect_uri=" + SSR.minimalPercentEncode(EDStatic.erddapHttpsUrl + "/loginOrcid.html") + "\" \n" +
                        "  ><img style=\"vertical-align:middle;\" src=\"images/orcid_24x24.png\" alt=\"ORCID iD icon\"/>&nbsp;" +
                            EDStatic.loginOrcidSignIn + "</a>\n" +
                        "  <br>&nbsp;\n" : "") +                    
                    "</ul>\n" +
                    tProblems + "\n" +
                    "<p><strong><a class=\"selfLink\" id=\"scripts\" href=\"#scripts\" rel=\"bookmark\">Accessing Private Datasets via Scripts</a></strong>\n" +
                    "<p>For instructions on logging into ERDDAP and accessing private datasets via scripts, see\n" +
                    "<br><a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/AccessToPrivateDatasets.html\">Access to Private Datasets in ERDDAP</a>.\n" +
                    "\n");

                } else {
                    //tell user he is logged in
                    writer.write("<p><span class=\"successColor\">" + EDStatic.loginAs + 
                        " <strong>" + loggedInAs + "</strong>.</span>\n" +
                        "(<a href=\"" + EDStatic.erddapHttpsUrl + "/logout.html\">" +
                        EDStatic.logout + "</a>)\n" +
                        "<p>" + EDStatic.loginBack + "\n" +
                        String2.replaceAll(EDStatic.loginProblemsAfter, "&second;", 
                            isOrcid || isOauth2? EDStatic.loginProblemOrcidAgain : ""));
                }
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t;
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        } 

        //*** Other
        //alternative: lowSendError(response, HttpServletResponse.SC_UNAUTHORIZED, 
        //    EDStatic.loginCanNot);
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, EDStatic.LogIn, out);
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, EDStatic.LogIn) +
                standoutMessage +
                "<p><span class=\"highlightColor\">" + EDStatic.loginCanNot + "</span>\n");       
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
        return;
    }

    /**
     * This responds to a logout.html request.
     * This doesn't display a web page.
     * This does react to the request and redirect to another web page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doLogout(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs) throws Throwable {

        String tErddapUrl = EDStatic.erddapHttpsUrl;
        String loginUrl = tErddapUrl + "/login.html";

        //user wasn't logged in?
        String encodedYouWerentLoggedIn = "?message=" + 
            SSR.minimalPercentEncode(EDStatic.loginAreNot);
        if (loggedInAs == null || loggedInAs.equals(EDStatic.loggedInAsHttps)) {
            //user wasn't logged in
            sendRedirect(response, loginUrl + encodedYouWerentLoggedIn);
            return;
        }

        try {       
            //user was logged in
            //remove the info I put in the session
            HttpSession session = request.getSession(false); //false = don't make a session if none currently
            if (session != null) { //should always be !null
                session.removeAttribute("loggedInAs:" + EDStatic.warName);
                session.invalidate(); //forget any related info
                Math2.sleep(500); //give session changes time to take effect
                EDStatic.tally.add("Log out (since startup)", "success");
                EDStatic.tally.add("Log out (since last daily report)", "success");
            }
            String encodedSuccessMessage = "?message=" + SSR.minimalPercentEncode(
                EDStatic.logoutSuccess);

            //*** CUSTOM, EMAIL, ORCID logout
            if (EDStatic.authentication.equals("custom") ||
                EDStatic.authentication.equals("email") ||
                EDStatic.authentication.equals("orcid")) {
                sendRedirect(response, loginUrl + encodedSuccessMessage);
                return;
            }

            //*** GOOGLE and OAUTH2 (act as if user used google)
            if (EDStatic.authentication.equals("google") || 
                EDStatic.authentication.equals("oauth2")) {

                //send user to web page that signs out then redirects to login.html
                //see https://developers.google.com/identity/sign-in/web/
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, EDStatic.LogOut, 
                    "<meta name=\"google-signin-client_id\" content=\"" + EDStatic.googleClientID + "\">\n",
                    out);

                try {
                    HtmlWidgets widgets = new HtmlWidgets(false, //tHtmlTooltips, 
                        EDStatic.imageDir);
                    writer.write(
                        "<div class=\"standard_width\">\n" +
                        EDStatic.youAreHere(loggedInAs, EDStatic.LogOut) +
                        //"Logging out and redirecting back to login.html.\n" +
                        //Sequence of events here was very difficult to set up.
                        //Javascript scripts are executed in order of appearance.
                        //solution to gapi.auth2 is undefined:
                        //https://stackoverflow.com/questions/29815870/typeerror-gapi-auth2-undefined
                        "<script>\n" +
                        "  function initAuth2() {\n" +
                        "    console.log('in initAuth2()');\n" +
                        "    gapi.load('auth2', function() {\n" +
                        "      gapi.auth2.init();\n" +
                        "    });\n" +
                        "  }\n" +
                        "</script>\n" +
                        "<script src=\"https://apis.google.com/js/platform.js?onload=initAuth2\"></script>\n" + //I removed async and defer so fetched and run immediately
                        "<script>\n" +
                        "  function signOut() {\n" +       
                        "    console.log('in mySignOff()');\n" +
                        "    try {\n" +
                        "      var auth2 = gapi.auth2.getAuthInstance();\n" +
                        "      auth2.signOut().then(function () {\n" +
                        "        console.log('User signed out.');\n" +
                        "        });\n" +
                        "    } catch (soError) {}\n" +
                        "    window.location.assign(\"" + 
                            loginUrl + encodedSuccessMessage + "\");\n" +
                        "  }\n" +
                        //"</script>\n" +
                        //"<script>\n" +
                        //"  onload = mySignOff;\n" +
                        "</script>\n" +
                        EDStatic.loginPartwayAs + " <strong>" + loggedInAs + "</strong>.\n" +
                        widgets.htmlButton("button", "logout", "", 
                            EDStatic.LogOut, EDStatic.LogOut, 
                            "onclick=\"signOut();\""));
                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(t));
                    throw t;
                } finally {
                    writer.write("</div>\n");
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }
                return;
            }

            //*** Other    (shouldn't get here)
            sendRedirect(response, loginUrl + encodedYouWerentLoggedIn);
            return;

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            sendRedirect(response, loginUrl + 
                "?message=" + SSR.minimalPercentEncode(MustBe.getShortErrorMessage(t)));
            return;
        }
    }

    /**
     * This shows the start of the forms for data providers to fill out.
     * Note: default URL length (actually, the whole header) is 8KB. 
     * That should be plenty. For longer, see tomcat settings:
     * https://serverfault.com/questions/56691/whats-the-maximum-url-length-in-tomcat
     *
     */
    public void doDataProviderForm(HttpServletRequest request, HttpServletResponse response,
        String tLoggedInAs) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(tLoggedInAs);
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(tLoggedInAs, "Data Provider Form", out);

        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(tLoggedInAs, "Data Provider Form"));

//begin text
writer.write(
"This Data Provider Form is for people who have data and want it to be served by this ERDDAP.\n" +
"The overview of the process is:\n" +
"<ol>\n" +
"<li>You fill out a 4-part form. When you finish a part,\n" +
"  the information you just entered is\n"+
"  sent to the administrator of this ERDDAP (<kbd>" +
    XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"<li>The ERDDAP administrator will contact you to figure out the best way to\n" +
"  transfer the data and to work out other details.\n" +
"  &nbsp;\n" +
"</ol>\n" +
"\n" +
"<h2>Things You Need To Know</h2>\n" +
"<strong>The Goal</strong>\n" +
"<br>The goal of this form is to help you create a description of your dataset\n" +
"  (\"metadata\"), so that someone who knows nothing about this dataset will be\n" +
"  able to find the dataset when they search for this type of dataset,\n" +
"  understand the dataset, use the data properly, and give you credit\n" +
"  for having created the dataset.\n" +
"<p><strong>Don't skim.</strong>\n" +
"<br>Please work your way down this document, reading everything carefully. Don't skim.\n" +
"  If you skip over some important point, it will just be twice as much\n" +
"  work for you (and me) later.\n" +
"<p><strong>Do your best.</strong>\n" +
"<br>This is the first pass at creating metadata for this dataset.\n" +
"  The ERDDAP administrator will edit this and work with you to improve it.\n" +
"  It is even easy to change the metadata after the dataset is in ERDDAP.\n" +
"  But, it is better to get this right the first time.\n" + 
"  Please focus, read carefully, be patient, be diligent, and do your best.\n" +
"<p><strong>Helpful Hint Icons</strong> " + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Success! As you see, when you move/hover your mouse over" +
    "<br>these question mark icons, you will see helpful hints" +
    "<br>and term definitions.") + "\n" +
"<br>If you move/hover your mouse over these question mark icons,\n" +
"  you will see helpful hints and term definitions.\n" +
"  Try it now with the icon above.\n" +
"  There are lots of these icons on this web page and in ERDDAP in general.\n" +
"  The information they contain is essential for filling out this form properly.\n" +
"<p><strong>How much time will this take?</strong>\n" +
"<br>It takes an hour to do a really good job with this form,\n" +
"  less if you have all the needed information handy.\n" +
"<p><strong>Running out of time?</strong>\n" +
"<br>If you run out of time before you finish all 4 parts, just leave the\n" +
"  tab open in your browser so you can come back and finish it later.\n" +
"  There is no time limit for finishing all 4 parts.\n" +
"<p><strong>Large Number of Datasets?</strong>\n" +
"<br>If you have structured information (for example, ISO 19115 files) for a large\n" +
"  number of datasets, there are probably ways that we can work together to\n" +
"  (semi-)automate the process of getting the datasets into ERDDAP.\n" +
"  Please email the administrator of this ERDDAP\n" +
"  (<kbd>" + XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>)\n" + 
"  to discuss the options.\n" +
"<p><strong>Need help?</strong>\n" +
"<br>If you have questions or need help while filling out this form,\n" +
"  please send an email to the administrator of this ERDDAP\n" +
"  (<kbd>" + XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"  <br>&nbsp;\n" +
"\n" +

"<p><a rel=\"bookmark\" href=\"" + tErddapUrl + "/dataProviderForm1.html\"\n" +
"><span style=\"font-size:xx-large; line-height:130%;\"><strong>Click Here for Part 1 (of 4) of the\n" +
"<br>Data Provider Form</strong></span></a>\n");

        
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
        return;
    }

    /**
     * This is part 1 of the Data Provider Form -- The Data.
     */
    public void doDataProviderForm1(HttpServletRequest request, HttpServletResponse response,
        String tLoggedInAs, String ipAddress) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(tLoggedInAs);
        OutputStream out = null;
        Writer writer = null;

        try {

            //getParameters
            //gridded options
            String griddedOptions[] = {"(No, I have tabular data.)", 
                ".bufr files", ".grib files",  ".hdf files", ".mat files",
                ".nc files",
                "(let's talk)"};
            int griddedOption = Math.max(0, String2.indexOf(griddedOptions, 
                request.getParameter("griddedOption")));

            //tabular options
            String tabularOptions[] = {"(No, I have gridded data.)",                 
                ".csv files", "database", "Excel files", 
                ".hdf files", ".mat files", ".nc files", 
                "(let's talk)"};
            int tabularOption = Math.max(0, String2.indexOf(tabularOptions, 
                request.getParameter("tabularOption")));

            //frequency options
            String frequencyOptions[] = {
                "never", 
                "rarely", 
                "yearly",
                "monthly",
                "daily",
                "hourly",
                "every minute",
                "(irregularly)",
                "(let's talk)"};
            int frequencyOption = String2.indexOf(frequencyOptions, 
                request.getParameter("frequencyOption"));

            String 
                tYourName         = request.getParameter("yourName"),
                tEmailAddress     = request.getParameter("emailAddress"),
                tTimestamp        = request.getParameter("timestamp");                

            //validate (same order as form)
            StringBuilder errorMsgSB = new StringBuilder();
            tYourName     = HtmlWidgets.validateIsSomethingNotTooLong(
                "Your Name",     "", tYourName,     50, errorMsgSB);
            tEmailAddress = HtmlWidgets.validateIsSomethingNotTooLong(
                "Email Address", "", tEmailAddress, 50, errorMsgSB);
            tTimestamp    = HtmlWidgets.validateIsSomethingNotTooLong(
                "Timestamp", Calendar2.getCurrentISODateTimeStringLocalTZ(), 
                tTimestamp, 26, errorMsgSB);
            if (griddedOption == 0 && tabularOption == 0) 
                errorMsgSB.append( 
                "<br>&bull; Error: Please specify (below) how your gridded or tabular data is stored.\n");
            frequencyOption = HtmlWidgets.validate0ToMax(
                "Frequency", 0, frequencyOption, frequencyOptions.length - 1, errorMsgSB);
            if (errorMsgSB.length() > 0)
                errorMsgSB.insert(0, 
                    "<br>Please fix these problems, then 'Submit' this part of the form again.\n");

            String fromInfo = tYourName  + " <" + tEmailAddress + "> at " + tTimestamp;

            //if this is a submission, 
            boolean isSubmission = "Submit".equals(request.getParameter("Submit"));
            if (isSubmission && errorMsgSB.length() == 0) {
                //convert the info into pseudo datasets.xml
                String content = 
                    "Data Provider Form - Part 1\n" + //important! Bob's erd.data gmail filter looks for 'Data Provider Form'
                    "  from " + fromInfo + "\n" +
                    "  ipAddress=" + ipAddress + "\n" +
                    "\n" +
                    "griddedOption=" + griddedOptions[griddedOption] + "\n" +
                    "tabularOption=" + tabularOptions[tabularOption] + "\n" +
                    "frequencyOption=" + frequencyOptions[frequencyOption] + "\n" +
                    "\n";

                //log the content to /logs/dataProviderForm.log
                String error = String2.appendFile(
                    EDStatic.fullLogsDirectory + "dataProviderForm.log",
                    "*** " + content);
                if (error.length() > 0)
                    String2.log(String2.ERROR + 
                        " while writing to logs/dataProviderForm.log:\n" +
                        error);
                //email the content to the admin
                EDStatic.email(EDStatic.adminEmail, 
                    "Data Provider Form - Part 1, from " + fromInfo,
                    content);

                //redirect to part 2
                sendRedirect(response, tErddapUrl + "/dataProviderForm2.html?" +
                    "yourName=" + SSR.minimalPercentEncode(tYourName) +
                    "&emailAddress=" + SSR.minimalPercentEncode(tEmailAddress) +
                    "&timestamp=" + SSR.minimalPercentEncode(tTimestamp));
    
                return;
            }

            //write the HTML
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(tLoggedInAs, "Data Provider Form - Part 1", out);
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(tLoggedInAs, "Data Provider Form - Part 1"));

            //begin form
            String formName = "f1";
            HtmlWidgets widgets = new HtmlWidgets(false, //style, false=not htmlTooltips
                EDStatic.imageDirUrl(tLoggedInAs)); 
            widgets.enterTextSubmitsForm = false; 
            writer.write(widgets.beginForm(formName, 
                //this could be POST to deal with lots of text. 
                //but better to change tomcat settings (above) and keep ease of use
                "GET", tErddapUrl + "/dataProviderForm1.html", "") + "\n");

            //hidden fields
            writer.write(
                widgets.hidden("timestamp", XML.encodeAsHTML(tTimestamp)) + 
                "\n");

//begin text
writer.write(
"This is part 1 (of 4) of the Data Provider Form.\n" +
"<br>Need help? Send an email to the administrator of this ERDDAP (<kbd>" + 
    XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"<br>&nbsp;\n" +
"\n");

//error message?
if (isSubmission && errorMsgSB.length() > 0) 
writer.write("<span class=\"warningColor\">" + errorMsgSB.toString() + "</span> " + 
    "<br>&nbsp;\n");

//Contact Info
writer.write(
"<h2>Your Contact Information</h2>\n" +
"This will be used by the ERDDAP administrator to contact you.\n" +
"This won't go in the dataset's metadata or be made public.\n" +
"<p>What is your name? " +
widgets.textField("yourName", "", //tooltip
    30, 50, tYourName, "") +
"  <br>What is your email address? " +
widgets.textField("emailAddress", "", //tooltip
    30, 50, tEmailAddress, "") +
"  <br>This dataset submission's timestamp is " + tTimestamp + ".\n" + 
"\n");

//The Data
writer.write(
"<h2>The Data</h2>\n" +
"ERDDAP deals with a dataset in one of two ways: as gridded data or as tabular data.\n" +

//Gridded Data
"<p><strong>Gridded Data</strong>\n" +
"<br>ERDDAP can serve data from various types of data files\n" +
"(and from OPeNDAP servers like Hyrax, THREDDS, GrADS, ERDDAP) that contain multi-dimensional\n" +
"gridded data, for example, Level 3 sea surface temperature data (from a satellite) with\n" +
"three dimensions: [time][latitude][longitude].\n" +
"<p>The data for a gridded dataset can be stored in one file or many files\n" +
"(typically with one time point per file).\n" +
"<p>If your dataset is already served via an OPeNDAP server,\n" +
"skip this form and just email the dataset's OPeNDAP URL\n" +
"to the administrator of this ERDDAP \n" +
"  (<kbd>" + XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"<p>How is your gridded data stored?\n" +
widgets.select("griddedOption", "", 1, griddedOptions, griddedOption, "") +
"\n" +

//Tabular Data
"<p><strong>Tabular Data</strong>\n" +
"<br>ERDDAP can also serve data that can be <i>represented</i> as a single, database-like\n" +
"table, where there is a column for each type of data and a row for each observation.\n" +
"This includes:\n" +
"<ul>\n" +
"<li><a class=\"selfLink\" id=\"databases\" href=\"#databases\" rel=\"bookmark\">Data</a> that is currently stored in a relational database.\n" +
"  <br>Please read the information about the\n" +
"  <a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#EDDTableFromDatabase\">EDDTableFromDatabase</a>\n" +
"  dataset type in ERDDAP, especially the initial ~2 screens of information which\n" +
"  talk about the need to create a denormalized table. That may sound crazy. Please\n" +
"  trust enough to read the rationale for this.\n" +
"  <br>&nbsp;\n" +
"<li>All <i>in situ</i> data.\n" +
"  <br>Examples: a time series from an instrument or several similar instruments,\n" +
"  profile data from a CTD or a group of CTD's,\n" +
"  or data collected during a ship's cruise (the similar cuises over several years).\n" +
"  <br>&nbsp;\n" +
"<li>Non-geospatial data that can be represented as a table of data.\n" +
"  <br>Examples: data from a laboratory experiment, genetic sequence data,\n" +
"  or a list of bibliographic references.\n" +
"  <br>&nbsp;\n" +
"<li>Collections of other types of files (for example, image or audio files).\n" +
"  <br>ERDDAP can present the file names in a table and let users\n" +
"  view or download the files.\n" +
"</ul>\n" +
"The data for a tabular dataset can be stored in one file or many files\n" +
"(typically with data for one station, one glider, one animal, or one cruise per file).\n" +
"We recommend making one dataset with all of the data that is very similar,\n" +
"and not a lot of separate datasets.\n" +
"For example, you might make one dataset with data from a group of moored buoys,\n" +
"a group of gliders, a group of animals, or a group of cruises (for example, annually on one line).\n" +
"<p>How is your tabular data stored?\n" +
widgets.select("tabularOption", "", 1, tabularOptions, tabularOption, "") +
"\n" +

//Frequency Of Changes
"<p><strong>Frequency of Changes</strong>\n" +
"<br>Some datasets get new data frequently. Some datasets will never be changed.\n" +
"<br>How often will this data be changed?\n" +
widgets.select("frequencyOption", "", 1, frequencyOptions, frequencyOption, "") +
"<br>&nbsp;\n" +
"\n");

//Submit
writer.write(
"<h2>Finished with part 1?</h2>\n" + 
"Click\n" + 
widgets.button("submit", "Submit", "", "Submit", "") +
"to send this information to the ERDDAP administrator and move on to part 2 (of 4).\n" +
"\n");

//end form
writer.write(widgets.endForm());        

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            if (writer != null)
                writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            if (writer != null) {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
        }
        return;
    }

    /**
     * This is Data Provider Form 2
     */
    public void doDataProviderForm2(HttpServletRequest request, HttpServletResponse response,
        String tLoggedInAs, String ipAddress) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(tLoggedInAs);
        OutputStream out = null;
        Writer writer = null;

        try {

            //get parameters
            //cdm_data_type options
            String cdmDataTypes[] = {
                "Grid",
                "Point", 
                "Profile", 
                "TimeSeries",
                "TimeSeriesProfile",
                "Trajectory",
                "TrajectoryProfile",
                "Other"};
            int defaultCdmDataType = String2.indexOf(cdmDataTypes, "Other");
            int tCdmDataType = String2.indexOf(cdmDataTypes, 
                request.getParameter("cdm_data_type"));
            String cdmDataTypeHelp =                 
"CDM is Unidata's Common Data Model, a way of categorizing datasets" +
"<br>based on the geometry of the dataset. Pick the cdm_data_type which" +
"<br>is most appropriate:" +
"<br>&bull; Use <kbd>Grid</kbd> for all gridded datasets." +
"<br>&bull; Use <kbd>Point</kbd> for a dataset with unrelated points." +
"<br>&nbsp;&nbsp;Example: whale sightings, or stranded marine mammal sightings." +
"<br>&bull; Use <kbd>Profile</kbd> for data from multiple depths at one or more longitude," +
"<br>&nbsp;&nbsp;latitude locations." +
"<br>&nbsp;&nbsp;Example: CTD's if not associated with a TimeSeries or Trajectory." +
"<br>&bull; Use <kbd>TimeSeries</kbd> for data from a set of stations with fixed longitude," +
"<br>&nbsp;&nbsp;latitude(,altitude)." +
"<br>&nbsp;&nbsp;Examples: moored buoys, or stations." +
"<br>&bull; Use <kbd>TimeSeriesProfile</kbd> for profiles from a set of stations." +
"<br>&nbsp;&nbsp;Examples: stations with CTD's." +
"<br>&bull; Use <kbd>Trajectory</kbd> for data from a set of longitude,latitude(,altitude)" +
"<br>&nbsp;&nbsp;paths called trajectories." +
"<br>&nbsp;&nbsp;Examples: ships, surface gliders, or tagged animals." +
"<br>&bull; Use <kbd>TrajectoryProfile</kbd> for profiles along trajectories." +
"<br>&nbsp;&nbsp;Examples: ships + CTD's, or profiling gliders." +
"<br>&bull; Use <kbd>Other</kbd> if the dataset doesn't have latitude,longitude data or if" +
"<br>&nbsp;&nbsp;no other type is appropriate." +
"<br>&nbsp;&nbsp;Examples: laboratory analyses, or fish landings by port name (if no lat,lon).";

            String creatorTypes[] = {
                "person",
                "group", 
                "institution", 
                "position"};
            int tCreatorType = String2.indexOf(creatorTypes, 
                request.getParameter("creator_type"));

            String 
                //required
                tYourName         = request.getParameter("yourName"),
                tEmailAddress     = request.getParameter("emailAddress"),
                tTimestamp        = request.getParameter("timestamp"),
                tTitle            = request.getParameter("title"),
                tSummary          = request.getParameter("summary"),
                tCreatorName      = request.getParameter("creator_name"),
                tCreatorEmail     = request.getParameter("creator_email"),
                tInstitution      = request.getParameter("institution"),
                tInfoUrl          = request.getParameter("infoUrl"),
                tLicense          = request.getParameter("license"),
                //optional
                tHistory          = request.getParameter("history"),
                tAcknowledgement  = request.getParameter("acknowledgement"),
                tID               = request.getParameter("id"),
                tNamingAuthority  = request.getParameter("naming_authority"),
                tProductVersion   = request.getParameter("product_version"),
                tReferences       = request.getParameter("references"),
                tComment          = request.getParameter("comment");

            //validate (same order as form) 
            StringBuilder errorMsgSB = new StringBuilder();
            tYourName     = HtmlWidgets.validateIsSomethingNotTooLong(
                "Your Name",     "?", tYourName,     50, null);
            tEmailAddress = HtmlWidgets.validateIsSomethingNotTooLong(
                "Email Address", "?", tEmailAddress, 50, null);
            tTimestamp    = HtmlWidgets.validateIsSomethingNotTooLong(
                "Timestamp", Calendar2.getCurrentISODateTimeStringLocalTZ() + "?", 
                tTimestamp, 26, null);

            //required
            tTitle          = HtmlWidgets.validateIsSomethingNotTooLong("title",         "", tTitle,             80, errorMsgSB);
            tSummary        = HtmlWidgets.validateIsSomethingNotTooLong("summary",       "", tSummary,          500, errorMsgSB);
            tCreatorName    = HtmlWidgets.validateIsSomethingNotTooLong("creator_name",  "", tCreatorName,       80, errorMsgSB);
            tCreatorType    = HtmlWidgets.validate0ToMax("creator_type", 0, 
                tCreatorType, creatorTypes.length - 1, errorMsgSB);
            tCreatorEmail   = HtmlWidgets.validateIsSomethingNotTooLong("creator_email", "", tCreatorEmail,      80, errorMsgSB);
            tInstitution    = HtmlWidgets.validateIsSomethingNotTooLong("institution",   "", tInstitution,       80, errorMsgSB);
            tInfoUrl        = HtmlWidgets.validateIsSomethingNotTooLong("infoUrl",       "", tInfoUrl,          200, errorMsgSB);
            tLicense        = HtmlWidgets.validateIsSomethingNotTooLong("license", "[standard]", tLicense,      500, errorMsgSB);
            tCdmDataType    = HtmlWidgets.validate0ToMax("cdm_data_type", defaultCdmDataType, 
                tCdmDataType, cdmDataTypes.length - 1, errorMsgSB);
            //optional
            tAcknowledgement= HtmlWidgets.validateNotTooLong("acknowledgement",          "", tAcknowledgement,  350, errorMsgSB);
            tHistory        = HtmlWidgets.validateNotTooLong("history",                  "", tHistory,          500, errorMsgSB);
            tID             = HtmlWidgets.validateNotTooLong("id",                       "", tID,                80, errorMsgSB);
            tNamingAuthority= HtmlWidgets.validateNotTooLong("naming_authority",         "", tNamingAuthority,  160, errorMsgSB);
            tProductVersion = HtmlWidgets.validateNotTooLong("product_version",          "", tProductVersion,    40, errorMsgSB);
            tReferences     = HtmlWidgets.validateNotTooLong("references",               "", tReferences,       500, errorMsgSB);
            tComment        = HtmlWidgets.validateNotTooLong("comment",                  "", tComment,          350, errorMsgSB);
            if (errorMsgSB.length() > 0)
                errorMsgSB.insert(0, 
                    "<br>Please fix these problems, then 'Submit' this part of the form again.\n");

            String fromInfo = tYourName  + " <" + tEmailAddress + "> at " + tTimestamp;

            //if this is a submission, 
            boolean isSubmission = "Submit".equals(request.getParameter("Submit"));
            if (isSubmission && errorMsgSB.length() == 0) {
                //convert the info into pseudo datasets.xml
                String tcdmType = cdmDataTypes[tCdmDataType]; //Grid, Point, Profile, TimeSeries, TimeSeriesProfile, Trajectory, TrajectoryProfile, Other
                HashSet<String> keywordHS = new HashSet();
                EDD.chopUpAndAdd(String2.replaceAll(tInstitution, '/', ' '), keywordHS);
                EDD.chopUpAndAdd(String2.replaceAll(tTitle,       '/', ' '), keywordHS);
                EDD.cleanSuggestedKeywords(keywordHS);
                StringArray tKeywords = new StringArray(keywordHS.iterator()); 
                tKeywords.sortIgnoreCase();
                String content = 
"Data Provider Form - Part 2\n" + //important! Bob's erd.data gmail filter looks for this
"  from " + fromInfo + "\n" +
"  ipAddress=" + ipAddress + "\n" +
"\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgement\">" + XML.encodeAsXML(tAcknowledgement)           + "</att>\n" +
"        <att name=\"cdm_data_type\">"   + XML.encodeAsXML(tcdmType) + "</att>\n" +
(tcdmType.indexOf("TimeSeries") >= 0? 
"        <att name=\"cdm_timeseries_variables\">???</att>\n" : "") +
(tcdmType.indexOf("Trajectory") >= 0? 
"        <att name=\"cdm_trajectory_variables\">???</att>\n" : "") +
(tcdmType.indexOf("Profile") >= 0? 
"        <att name=\"cdm_profile_variables\">???</att>\n" : "") +
"        <att name=\"subsetVariables\">???</att>\n" +
"        <att name=\"comment\">"         + XML.encodeAsXML(tComment)                   + "</att>\n" +
"        <att name=\"Conventions\">ACDD-1.3, COARDS, CF-1.6</att>\n" +
"        <att name=\"creator_name\">"    + XML.encodeAsXML(tCreatorName)               + "</att>\n" +
"        <att name=\"creator_type\">"    + XML.encodeAsXML(creatorTypes[tCreatorType]) + "</att>\n" +
"        <att name=\"creator_email\">"   + XML.encodeAsXML(tCreatorEmail)              + "</att>\n" +
"        <att name=\"creator_url\">"     + XML.encodeAsXML(tInfoUrl) /*yes, infoUrl*/  + "</att>\n" +
"        <att name=\"history\">"         + XML.encodeAsXML(tHistory)                   + "</att>\n" +
"        <att name=\"id\">"              + XML.encodeAsXML(tID)                        + "</att>\n" +
"        <att name=\"infoUrl\">"         + XML.encodeAsXML(tInfoUrl)                   + "</att>\n" +
"        <att name=\"institution\">"     + XML.encodeAsXML(tInstitution)               + "</att>\n" +
"        <att name=\"keywords\">"        + XML.encodeAsXML(tKeywords.toString())       + "</att>\n" +
"        <att name=\"license\">"         + XML.encodeAsXML(tLicense)                   + "</att>\n" +
"        <att name=\"naming_authority\">"+ XML.encodeAsXML(tNamingAuthority)           + "</att>\n" +
"        <att name=\"product_version\">" + XML.encodeAsXML(tProductVersion)            + "</att>\n" +
"        <att name=\"references\">"      + XML.encodeAsXML(tReferences)                + "</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"summary\">"         + XML.encodeAsXML(tSummary)                   + "</att>\n" +
"        <att name=\"title\">"           + XML.encodeAsXML(tTitle)                     + "</att>\n" +
"    </addAttributes>\n" +
"\n";

                //log the content to /logs/dataProviderForm.log
                String error = String2.appendFile(
                    EDStatic.fullLogsDirectory + "dataProviderForm.log",
                    "*** " + content);
                if (error.length() > 0)
                    String2.log(String2.ERROR + 
                        " while writing to logs/dataProviderForm.log:\n" +
                        error);
                //email the content to the admin
                EDStatic.email(EDStatic.adminEmail, 
                    "Data Provider Form - Part 2, from " + fromInfo,
                    content);

                //redirect to part 3
                sendRedirect(response, tErddapUrl + "/dataProviderForm3.html?" +
                    "yourName=" + SSR.minimalPercentEncode(tYourName) +
                    "&emailAddress=" + SSR.minimalPercentEncode(tEmailAddress) +
                    "&timestamp=" + SSR.minimalPercentEncode(tTimestamp));
               
                return;
            }

            //write the HTML
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(tLoggedInAs, "Data Provider Form - Part 2", out);
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(tLoggedInAs, "Data Provider Form - Part 2"));

            //begin form
            String formName = "f1";
            HtmlWidgets widgets = new HtmlWidgets(false, //style, false=not htmlTooltips
                EDStatic.imageDirUrl(tLoggedInAs)); 
            widgets.enterTextSubmitsForm = false; 
            writer.write(widgets.beginForm(formName, 
                //this could be POST to deal with lots of text. 
                //but better to change tomcat settings (above) and keep ease of use
                "GET", tErddapUrl + "/dataProviderForm2.html", "") + "\n");
            
            //hidden fields
            writer.write(
                widgets.hidden("yourName",     XML.encodeAsHTML(tYourName)) +
                widgets.hidden("emailAddress", XML.encodeAsHTML(tEmailAddress)) +
                widgets.hidden("timestamp",    XML.encodeAsHTML(tTimestamp)) + 
                "\n");

//begin text
writer.write(
"This is part 2 (of 4) of the Data Provider Form\n" +
"<br>from " + XML.encodeAsHTML(fromInfo) + ".\n" +
"<br>Need help? Send an email to the administrator of this ERDDAP (<kbd>" + 
    XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"<br>&nbsp;\n" +
"\n");

//error message?
if (isSubmission && errorMsgSB.length() > 0) 
writer.write("<span class=\"warningColor\">" + errorMsgSB.toString() + "</span> " + 
    "<br>&nbsp;\n");

//Global Metadata
writer.write(
"<h2>Global Metadata</h2>\n" +
"Global metadata is information about the entire dataset. It is a set of\n" +
"<kbd>attribute=value</kbd> pairs, for example,\n" +
"<br><kbd>title=Spray Gliders, Scripps Institution of Oceanography</kbd>\n" +
"<p>.nc files &mdash; If your data is in .nc files that already have some metadata,\n" +
"just provide the information below for attributes that aren't in your files\n" +
"or where you want to change the attribute's value.\n" +
"\n");

writer.write(
"<br>" + widgets.beginTable("class=\"compact\"") +
//Required
"<tr>\n" +
"  <td colspan=\"3\"><strong>Required</strong>\n" + 
"</tr>\n" +
"<tr>\n" +
"  <td>title\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
     "This is a short (&lt;=80 characters) description of the dataset. For example," + 
"    <br><kbd>Spray Gliders, Scripps Institution of Oceanography</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("title", "", dpfTFWidth, 140, tTitle, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>summary\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is a paragraph describing the dataset.  (&lt;=500 characters)" + 
    "<br>The summary should answer these questions:" +
    "<br>&bull; Who created the dataset?" +
    "<br>&bull; What information was collected?" +
    "<br>&bull; When was the data collected?" +
    "<br>&bull; Where was it collected?" +
    "<br>&bull; Why was it collected?" +
    "<br>&bull; How was it collected?") + "&nbsp;\n" +
"  <td><textarea name=\"summary\" cols=\"" + dpfTAWidth + "\" rows=\"6\" maxlength=\"500\" wrap=\"soft\">" +
XML.encodeAsHTML(tSummary) + //encoding is important for security
"</textarea>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>creator_name\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is the name of the primary person, group, institution," +
    "<br>or position that created the data. For example," + 
    "<br><kbd>John Smith</kbd>") + "&nbsp;" +
"  <td>\n" + 
widgets.textField("creator_name", "", 40, 80, tCreatorName, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>creator_type\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This identifies the creator_name (above) as a person," +
    "<br>group, institution, or position.") + "&nbsp;" + 
"  <td>\n" + 
widgets.select("creator_type", "", 1, creatorTypes, tCreatorType, "") +
"</tr>\n" +
"<tr>\n" +
"  <td>creator_email\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is the best contact email address for the creator of this data." +
    "<br>Use your judgment &mdash; the creator_email might be for a" +
    "<br>different entity than the creator_name." + 
    "<br>For example, <kbd>your.name@yourOrganization.org</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("creator_email", "", 40, 60, tCreatorEmail, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>institution\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is the short/abbreviated form of the name of the primary" +
    "<br>organization that created the data. For example," + 
    "<br><kbd>NOAA NMFS SWFSC</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("institution", "", 40, 120, tInstitution, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>infoUrl\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is a URL with information about this dataset." +
    "<br>For example, <kbd>http://spray.ucsd.edu</kbd>" + 
    "<br>If there is no URL related to the dataset, provide" +
    "<br>a URL for the group or organization.") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("infoUrl", "", dpfTFWidth, 200, tInfoUrl, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>license\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is the license and disclaimer for use of this data." +
    "<br>ERDDAP has a standard license, which you can use via <kbd>[standard]</kbd>" +
    "<br>You can either add to that or replace it. (&lt;=500 characters)" +
    "<br>The text of the standard license is:" +
    "<br><kbd>" + String2.replaceAll(EDStatic.standardLicense, "\n", "<br>") +
    "</kbd>") + "&nbsp;\n" +
"  <td><textarea name=\"license\" cols=\"" + dpfTAWidth + "\" rows=\"6\" maxlength=\"500\" wrap=\"soft\" >" +
XML.encodeAsHTML(tLicense) +  //encoding is important for security
"</textarea>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>cdm_data_type\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, cdmDataTypeHelp) + "&nbsp;\n" + 
"  <td>\n" + 
widgets.select("cdm_data_type", "", 1, cdmDataTypes, tCdmDataType, "") +
"</tr>\n" +
"<tr>\n" +
"  <td>&nbsp;\n" +
"  <td>&nbsp;\n" +
"  <td>&nbsp;\n" +
"</tr>\n" +
//Optional
"<tr>\n" +
"  <td><strong>Optional</strong>\n" +
"  <td>&nbsp;\n" +
"  <td>(Please provide the information if it is available for your dataset.)\n" + 
"</tr>\n" +
"<tr>\n" +
"  <td>acknowledgement\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is the place to acknowledge various types of support for" +
    "<br>the project that produced this data. (&lt;=350 characters) For example," +
    "<br><kbd>This project received additional funding from the NOAA" +
    "<br>Climate and Global Change Program.</kbd>") + "&nbsp;\n" +
"  <td><textarea name=\"acknowledgement\" cols=\"" + dpfTAWidth + "\" rows=\"4\" maxlength=\"350\" wrap=\"soft\" >" +
XML.encodeAsHTML(tAcknowledgement) +  //encoding is important for security
"</textarea>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>history\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is a list of the actions (one per line) which led to the creation of this data." +
    "<br>Ideally, each line includes a timestamp and a description of the action. (&lt;=500 characters) For example," + 
    "<br><kbd>Datafiles are downloaded ASAP from https://oceandata.sci.gsfc.nasa.gov/MODISA/L3SMI/ to NOAA NMFS SWFSC ERD." +
    "<br>NOAA NMFS SWFSC ERD (erd.data@noaa.gov) uses NCML to add the time dimension and slightly modify the metadata.</kbd>") + "&nbsp;\n" +
"  <td><textarea name=\"history\" cols=\"" + dpfTAWidth + "\" rows=\"6\" maxlength=\"500\" wrap=\"soft\" >" +
XML.encodeAsHTML(tHistory) +  //encoding is important for security
"</textarea>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>id\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is an identifier for the dataset, as provided by" +
    "<br>its naming authority. The combination of \"naming authority\"" +
    "<br>and the \"id\" should be globally unique, but the id can be" +
    "<br>globally unique by itself also. IDs can be URLs, URNs, DOIs," +
    "<br>meaningful text strings, a local key, or any other unique" +
    "<br>string of characters. The id should not include white space" +
    "<br>characters." +
    "<br>For example, <kbd>CMC0.2deg-CMC-L4-GLOB-v2.0</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("id", "", 40, 80, tID, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>naming_authority\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is the organization that provided the id (above) for the dataset." +
    "<br>The naming authority should be uniquely specified by this attribute." +
    "<br>We recommend using reverse-DNS naming for the naming authority;" +
    "<br>URIs are also acceptable." +
    "<br>For example, <kbd>org.ghrsst</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("naming_authority", "", dpfTFWidth, 160, tNamingAuthority, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>product_version\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is the version identifier of this data. For example, if you" +
    "<br>plan to add new data yearly, you might use the year as the version identifier." +
    "<br>For example, <kbd>2014</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("product_version", "", 20, 80, tProductVersion, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>references\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is one or more published or web-based references" +
    "<br>that describe the data or methods used to produce it. URL's and" +
    "<br>DOI's are recommend. (&lt;=500 characters) For example,\n" +
    "<br><kbd>Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a" +
    "<br>algorithms for oligotrophic oceans: A novel approach" +
    "<br>based on three-band reflectance difference, J. Geophys." +
    "<br>Res., 117, C01011, doi:10.1029/2011JC007395.</kbd>") + "&nbsp;\n" +
"  <td><textarea name=\"references\" cols=\"" + dpfTAWidth + "\" rows=\"6\" maxlength=\"500\" wrap=\"soft\" >" +
XML.encodeAsHTML(tReferences) +  //encoding is important for security
"</textarea>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>comment\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is miscellaneous information about the data, not" +
    "<br>captured elsewhere. (&lt;=350 characters) For example," +
    "<br><kbd>No animals were harmed during the collection of this data.</kbd>") + "&nbsp;\n" +
"  <td><textarea name=\"comment\" cols=\"" + dpfTAWidth + "\" rows=\"4\" maxlength=\"350\" wrap=\"soft\" >" +
XML.encodeAsHTML(tComment) +  //encoding is important for security
"</textarea>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>&nbsp;\n" +
"  <td>&nbsp;\n" +
"  <td>&nbsp;\n" +
"</tr>\n" +
"</table>\n" +
"\n");

//Submit
writer.write(
"<h2>Finished with part 2?</h2>\n" + 
"Click\n" + 
widgets.button("submit", "Submit", "", "Submit", "") +
"to send this information to the ERDDAP administrator and move on to part 3 (of 4).\n" +
"\n");
        
//end form
writer.write(widgets.endForm());        

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            if (writer != null)
                writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            if (writer != null) {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
        }
        return;
    }

    /**
     * This shows the form for data providers to fill out.
     * Note: default URL length (actually, the whole header) is 8KB. 
     * That should be plenty. For longer, see tomcat settings:
     * https://serverfault.com/questions/56691/whats-the-maximum-url-length-in-tomcat
     *
     */
    public void doDataProviderForm3(HttpServletRequest request, HttpServletResponse response,
        String tLoggedInAs, String ipAddress) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(tLoggedInAs);
        OutputStream out = null;
        Writer writer = null;

        try {

            //get parameters
            //dataType  (pretend char doesn't exist?)
            String groupOptions[] = {
                "first", "second", "third", "fourth", "fifth"};
            int tGroup = Math.max(0, String2.indexOf(groupOptions,
                request.getParameter("group")));             
            
            String dataTypeOptions[] = {"(unknown)", 
                "String", "boolean", "byte",  "short", "int", "long", "float", "double"};
            String dataTypeHelp = 
                "This is the data type and precision of this variable." +
                "<br>If the data file uses a specific type (for example, in a .nc file)," +
                "<br>&nbsp;&nbsp;specify that type here." +
                "<br>If the data file doesn't use a specific type (for example, in a .csv file)," +
                "<br>&nbsp;&nbsp;specify the type that should be used in ERDDAP." +
                "<br>&bull; Use <kbd>(unknown)</kbd> if you don't know." +
                "<br>&bull; <kbd>String</kbd> is a series of characters." +
                "<br>&nbsp;&nbsp;(For databases, ERDDAP treats all non-numeric data types as Strings.)" +
                "<br>&bull; <kbd>boolean</kbd> is either true or false. ERDDAP will convert these to bytes, 1 or 0." +
                "<br>&bull; <kbd>byte</kbd> is an 8 bit signed integer, +/-127" +
                "<br>&bull; <kbd>short</kbd> is a 16 bit signed integer, +/-32,767" +
                "<br>&bull; <kbd>int</kbd> is a 32 bit signed integer, +/-2,147,483,647" +
                "<br>&bull; <kbd>long</kbd> is a 64 bit signed integer, +/- ~1e19" +
                "<br>&bull; <kbd>float</kbd> is a 32 bit floating point number (up to 7 significant digits)" +
                "<br>&bull; <kbd>double</kbd> is a 64 bit floating point number (up to 17 significant digits)";

            int ioosUnknown = String2.indexOf(EDV.IOOS_CATEGORIES, "Unknown");
            String ioosCategoryHelp = 
                "Pick the ioos_category which is most appropriate for this variable." +
                "<br>&bull; Use <kbd>Location</kbd> for place names and for longitude, latitude," +
                "<br>&nbsp;&nbsp;altitude, and depth." +
                "<br>&bull; Use <kbd>Time</kbd> for date/time." +
                "<br>&bull; Use <kbd>Taxonomy</kbd> for species names." +
                "<br>&bull; Use <kbd>Identifier</kbd> for cruise names, ship names, line names," +
                "<br>&nbsp;&nbsp;station names, equipment types, serial numbers, etc." +
                "<br>&bull; Use <kbd>Ocean Color</kbd> for chlorophyll." +
                "<br>&bull; Use <kbd>Other</kbd> if no other category in the list is close." +
                "<br>&bull; Use <kbd>Unknown</kbd> if you really don't know.";

            String 
                tYourName         = request.getParameter("yourName"),
                tEmailAddress     = request.getParameter("emailAddress"),
                tTimestamp        = request.getParameter("timestamp");

            //variable attributes
            int nVars = 10;
            String                
                tSourceName[]      = new String[nVars+1],
                tDestinationName[] = new String[nVars+1],
                tLongName[]        = new String[nVars+1],
                tUnits[]           = new String[nVars+1],
                tRangeMin[]        = new String[nVars+1],
                tRangeMax[]        = new String[nVars+1],
                tStandardName[]    = new String[nVars+1],
                tFillValue[]       = new String[nVars+1],
                tComment[]         = new String[nVars+1];
            int tDataType[]        = new int[   nVars+1],
                tIoosCategory[]    = new int[   nVars+1];

            for (int var = 1; var <= nVars; var++) {     
                //String
                tSourceName[var]      = request.getParameter("sourceName" + var);
                tDestinationName[var] = request.getParameter("destinationName" + var);
                tLongName[var]        = request.getParameter("long_name" + var);
                tUnits[var]           = request.getParameter("units" + var);
                tRangeMin[var]        = request.getParameter("rangeMin" + var);
                tRangeMax[var]        = request.getParameter("rangeMax" + var);
                tStandardName[var]    = request.getParameter("standard_name" + var);
                tFillValue[var]       = request.getParameter("FillValue" + var); //note that field name lacks leading '_'
                tComment[var]         = request.getParameter("comment" + var); //note that field name lacks leading '_'

                //int
                tDataType[var]        = Math.max(0, String2.indexOf(dataTypeOptions,
                                            request.getParameter("dataType" + var)));
                tIoosCategory[var]    = String2.indexOf(EDV.IOOS_CATEGORIES,
                                            request.getParameter("ioos_category" + var));
                if (tIoosCategory[var] < 0)
                    tIoosCategory[var] = ioosUnknown;
            }

            //validate (same order as form)
            StringBuilder errorMsgSB = new StringBuilder();
            tYourName     = HtmlWidgets.validateIsSomethingNotTooLong(
                "Your Name",     "?", tYourName,     50, null);
            tEmailAddress = HtmlWidgets.validateIsSomethingNotTooLong(
                "Email Address", "?", tEmailAddress, 50, null);
            tTimestamp    = HtmlWidgets.validateIsSomethingNotTooLong(
                "Timestamp", Calendar2.getCurrentISODateTimeStringLocalTZ() + "?", 
                tTimestamp, 26, null);

            for (int var = 1; var <= nVars; var++) {     
                tSourceName[var] = var == 1?
                    HtmlWidgets.validateIsSomethingNotTooLong(
                        "sourceName #" + var, "", tSourceName[var], 50, errorMsgSB) :
                    HtmlWidgets.validateNotTooLong(
                        "sourceName #" + var, "", tSourceName[var], 50, errorMsgSB);
                tDestinationName[var] = HtmlWidgets.validateNotTooLong(
                    "destinationName #" + var, "", tDestinationName[var], 50, errorMsgSB);
                tLongName[var] = HtmlWidgets.validateNotTooLong(
                    "long_name #" + var, "", tLongName[var], 80, errorMsgSB);
                tStandardName[var] = HtmlWidgets.validateNotTooLong(
                    "standard_name #" + var, "", tStandardName[var], 80, errorMsgSB);
                tFillValue[var] = HtmlWidgets.validateNotTooLong(
                    "_FillValue #" + var, "", tFillValue[var], 20, errorMsgSB);
                tUnits[var] = HtmlWidgets.validateNotTooLong(
                    "units #" + var, "", tUnits[var], 40, errorMsgSB);
                tRangeMin[var] = HtmlWidgets.validateNotTooLong(
                    "range min #" + var, "", tRangeMin[var], 15, errorMsgSB);
                tRangeMax[var] = HtmlWidgets.validateNotTooLong(
                    "range max #" + var, "", tRangeMax[var], 15, errorMsgSB);
                tComment[var] = HtmlWidgets.validateNotTooLong(
                    "comment #" + var, "", tComment[var], 160, errorMsgSB);
            }
            if (errorMsgSB.length() > 0)
                errorMsgSB.insert(0, 
                    "<br>Please fix these problems, then 'Submit' this part of the form again.\n");

            String fromInfo = tYourName  + " <" + tEmailAddress + "> at " + tTimestamp;

            //if this is a submission, 
            boolean isSubmission = "Submit".equals(request.getParameter("Submit"));
            if (isSubmission && errorMsgSB.length() == 0) {
                //convert the info into pseudo datasets.xml
                StringBuilder content = new StringBuilder();
                content.append(
                    "Data Provider Form - Part 3\n" + //important! Bob's erd.data gmail filter looks for this
                    "  from " + fromInfo + "\n" +
                    "  ipAddress=" + ipAddress + "\n" +
                    "\n" +
                    "groupOf10=" + groupOptions[tGroup] + "\n" +
                        "\n");

                for (int var = 1; var <= nVars; var++) 
                    content.append(   
"    <dataVariable>\n" +
"        <sourceName>"      + XML.encodeAsXML(tSourceName[var])                + "</sourceName>\n" +
"        <destinationName>" + XML.encodeAsXML(tDestinationName[var])           + "</destinationName>\n" +
"        <dataType>"        + XML.encodeAsXML(dataTypeOptions[tDataType[var]]) + "</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">"
                                          + XML.encodeAsXML(tRangeMin[var])    + "</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">"
                                          + XML.encodeAsXML(tRangeMax[var])    + "</att>\n" +
"            <att name=\"comment\">"      + XML.encodeAsXML(tComment[var])     + "</att>\n" +
"            <att name=\"_FillValue\" type=\"" + 
dataTypeOptions[tDataType[var]] + "\">"   + XML.encodeAsXML(tFillValue[var])   + "</att>\n" +
"            <att name=\"ioos_category\">"+ XML.encodeAsXML(EDV.IOOS_CATEGORIES[tIoosCategory[var]]) + "</att>\n" +
"            <att name=\"long_name\">"    + XML.encodeAsXML(tLongName[var])    + "</att>\n" +
"            <att name=\"standard_name\">"+ XML.encodeAsXML(tStandardName[var])+ "</att>\n" +
"            <att name=\"units\">"        + XML.encodeAsXML(tUnits[var])       + "</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n");
                content.append(
                        "</dataset>\n" +
                        "\n");

                //log the content to /logs/dataProviderForm.log
                String error = String2.appendFile(
                    EDStatic.fullLogsDirectory + "dataProviderForm.log",
                    "*** " + content.toString());
                if (error.length() > 0)
                    String2.log(String2.ERROR + 
                        " while writing to logs/dataProviderForm.log:\n" +
                        error);
                //email the content to the admin
                EDStatic.email(EDStatic.adminEmail, 
                    "Data Provider Form - Part 3, from " + fromInfo,
                    content.toString());

                //redirect to part 4
                sendRedirect(response, tErddapUrl + "/dataProviderForm4.html?" +
                    "yourName=" + SSR.minimalPercentEncode(tYourName) +
                    "&emailAddress=" + SSR.minimalPercentEncode(tEmailAddress) +
                    "&timestamp=" + SSR.minimalPercentEncode(tTimestamp));
                
                return;
            }

            //write the HTML
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(tLoggedInAs, "Data Provider Form - Part 3", out);
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(tLoggedInAs, "Data Provider Form - Part 3"));

            //begin form
            String formName = "f1";
            HtmlWidgets widgets = new HtmlWidgets(false, //style, false=not htmlTooltips
                EDStatic.imageDirUrl(tLoggedInAs)); 
            widgets.enterTextSubmitsForm = false; 
            writer.write(widgets.beginForm(formName, 
                //this could be POST to deal with lots of text. 
                //but better to change tomcat settings (above) and keep ease of use
                "GET", tErddapUrl + "/dataProviderForm3.html", "") + "\n");

            //hidden fields
            writer.write(
                widgets.hidden("yourName",     XML.encodeAsHTML(tYourName)) +
                widgets.hidden("emailAddress", XML.encodeAsHTML(tEmailAddress)) +
                widgets.hidden("timestamp",    XML.encodeAsHTML(tTimestamp)) + 
                "\n");

//begin text
writer.write(
"This is part 3 (of 4) of the Data Provider Form\n" +
"<br>from " + XML.encodeAsHTML(fromInfo) + ".\n" +
"<br>Need help? Send an email to the administrator of this ERDDAP (<kbd>" + 
    XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"<br>&nbsp;\n" +
"\n");

//error message?
if (isSubmission && errorMsgSB.length() > 0) 
writer.write("<span class=\"warningColor\">" + errorMsgSB.toString() + "</span> " + 
    "<br>&nbsp;\n");

//Variable Metadata
writer.write(
"<h2>Variable Metadata</h2>\n" +
"Variable metadata is information that is specific to a given variable within\n" +
"the dataset. It is a set of <kbd>attribute=value</kbd> pairs, for example,\n" +
"<kbd>units=degree_C</kbd> .\n" +
"\n" +
"<p><strong>Fewer Or More Than 10 Variables</strong>\n" + //n variables
"<br>There are slots on this form for 10 variables.\n" +
"<br>If your dataset has 10 or fewer variables, just use the slots that you need.\n" +
"<br>If your dataset has more than 10 variables, then for each group of 10 variables:\n" +
"<ol>\n" +
"<li>Identify the group: This is the\n" +
widgets.select("group", "", 1, groupOptions, tGroup, "") +
"  group of 10 variables.\n" +
"<li>Fill out the form below for that group of 10 variables.\n" +
"<li>Click \"I'm finished!\" below to submit the information for that group of 10 variables.\n" +
"<li>If this isn't the last group, then on the next web page (for Part 4 of this form),\n" +
"  press your browser's Back button so that you can fill out this part of the form\n" +
"  (Part 3) for the next group of 10 variables.\n" +
"</ol>\n" +
"\n" +
"<p><strong>.nc Files</strong>\n" +
"<br>If your data is in .nc files that already have some metadata,\n" +
"just provide the information below for attributes that aren't in your files\n" +
"or where you want to change the attribute's value.\n" +
"\n");
        
//a table for each variable
for (int var = 1; var <= nVars; var++) 
writer.write(
widgets.beginTable("class=\"compact\"") +
"<tr>\n" +
"  <td colspan=3>&nbsp;<br><strong>Variable #" + var + "</strong></td>\n" +
"</tr>\n" +
"<tr>\n" +
"  <td>sourceName\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is the name of this variable currently used by the data source." +
    "<br>For example, <kbd>wt</kbd>" +
    "<br>This is case-sensitive.") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("sourceName" + var, "", 20, 60, tSourceName[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>destinationName\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: You can specify a new, different name for this variable." +
    "<br>This new name is the one that will be shown to users in ERDDAP." +
    "<br>For example, <kbd>waterTemp</kbd>" +
    "<br>This is case-sensitive." +
    "<br>This MUST start with a letter (A-Z, a-z) and MUST be followed by\n" +
    "<br>0 or more characters (A-Z, a-z, 0-9, and _)." +
    "<br>&bull; Use <kbd>latitude</kbd> for the main latitude variable." +
    "<br>&bull; Use <kbd>longitude</kbd> for the main longitude variable." +
    "<br>&bull; Use <kbd>altitude</kbd> if the variable measures height above sea level." +
    "<br>&bull; Use <kbd>depth</kbd> if the variable measures distance below sea level." +
    "<br>&bull; Use <kbd>time</kbd> for the main date/time variable." +
    "<br>&bull; Otherwise, it is up to you. If you want to use the sourceName\n" +
    "<br>&nbsp;&nbsp;as the destinationName, leave this blank.") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("destinationName" + var, "", 20, 60, tDestinationName[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>long_name\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "This is a longer, written-out version of the destinationName." +
    "<br>For example, <kbd>Water Temperature</kbd>" +
    "<br>Among other uses, it will be used as an axis title on graphs." +
    "<br>Capitalize each word in the long_name." +
    "<br>Don't include the units. (ERDDAP will add units when creating" +
    "<br>&nbsp;&nbsp;an axis title.)") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("long_name" + var, "", 40, 100, tLongName[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>standard_name\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is the name from the CF Standard Name Table" +
    "<br>&nbsp;&nbsp;which is most appropriate for this variable.\n" + 
    "<br>For example, <kbd>sea_water_temperature</kbd>." +
    "<br>Some common standard_names are listed in the dropdown list at right." +
    "<br>If you don't already know, or if no CF Standard Name is appropriate," +
    "<br>&nbsp;&nbsp;just leave this blank. We'll fill it in.") + "&nbsp;\n" +
"  <td>\n" + 
widgets.comboBox(formName, "standard_name" + var, "", 40, 120, tStandardName[var], 
    EDStatic.commonStandardNames, "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>dataType\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, dataTypeHelp) + "&nbsp;\n" + 
"  <td>\n" +
widgets.select("dataType" + var, "", 1, 
    dataTypeOptions, tDataType[var], "") +
"</tr>\n" +
"<tr>\n" +
"  <td>_FillValue\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "For numeric variables, this is the value that is used in the" +
    "<br>data file to indicate a missing value for this variable.\n" +
    "<br>For example, <kbd>-999</kbd> ." +
    "<br>If the _FillValue is NaN, use <kbd>NaN</kbd> .\n" +
    "<br>For String variables, leave this blank.") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("FillValue" + var, //note that field name lacks leading '_'
    "", 10, 30, tFillValue[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>units\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "These are the units of this variable. For example, <kbd>degree_C</kbd>" + 
    "<br>This is <strong>required</strong> for numeric variables, but not used for most String variables." +
    "<br>&bull; For temperature, use <kbd>degree_C</kbd> or <kbd>degree_F</kbd> ." +
    "<br>&bull; For counts of things, use <kbd>count</kbd> ." +
    "<br>&bull; For latitude variables, use <kbd>degrees_north</kbd> ." +
    "<br>&bull; For longitude variables, use <kbd>degrees_east</kbd> ." +
    "<br>&bull; For String date/time variables, paste a sample date/time value here." +
    "<br>&nbsp;&nbsp;We'll convert it to UDUnits." +
    "<br>&bull; For numeric date/time variables, describe the values as <kbd><i>units</i> since <i>basetime</i></kbd>," +
    "<br>&nbsp;&nbsp;for example, <kbd>days since 2010-01-01</kbd>" +
    "<br>&bull; For all other variables, use UDUNITs unit names if you know them;" +
    "<br>&nbsp;&nbsp;otherwise, use whatever units you already know.") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("units" + var, "", 20, 80, tUnits[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>range\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "For numeric variables, this specifies the typical range of values." +
    "<br>For example, <kbd>minimum=32.0</kbd> and <kbd>maximum=37.0</kbd> ." + 
    "<br>The range should include about 98% of the values." +
    "<br>These should be round numbers. This isn't precise.\n" +
    "<br>If you don't know the typical range of values, leave this blank." +
    "<br>For String variables, leave this blank.") + "&nbsp;\n" +
"  <td>minimum =\n" + 
widgets.textField("rangeMin" + var, "", 10, 30, tRangeMin[var], "") + 
"  &nbsp;&nbsp;maximum =\n" + 
widgets.textField("rangeMax" + var, "", 10, 30, tRangeMax[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>ioos_category\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, ioosCategoryHelp) + "&nbsp;\n" + 
"  <td>\n" + 
widgets.select("ioos_category" + var, "", 1, 
    EDV.IOOS_CATEGORIES, tIoosCategory[var], "") +
"</tr>\n" +
"<tr>\n" +
"  <td>comment\n" + 
"  <td>&nbsp;" + EDStatic.htmlTooltipImage(tLoggedInAs, 
    "Optional: This is miscellaneous information about this variable, not captured" +
    "<br>elsewhere. For example," +
    "<br><kbd>This is the difference between today's SST and the previous day's SST.</kbd>") + "&nbsp;\n" +
"  <td>\n" + 
widgets.textField("comment" + var, "", dpfTFWidth, 250, tComment[var], "") + 
"</tr>\n" +
"<tr>\n" +
"  <td>&nbsp;\n" +
"  <td>&nbsp;\n" +
"  <td>&nbsp;\n" +
"</tr>\n" +
"</table>\n" + //end of variable's table
"\n");

//Submit
writer.write(
"<h2>Finished with part 3?</h2>\n" + 
"Click\n" + 
widgets.button("submit", "Submit", "", "Submit", "") +
"to send this information to the ERDDAP administrator and move on to part 4 (of 4).\n" +
"\n");

//end form
writer.write(widgets.endForm());        

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            if (writer != null)
                writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            if (writer != null) {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
        }
        return;
    }

    /**
     * This is Data Provider Form 4
     *
     */
    public void doDataProviderForm4(HttpServletRequest request, HttpServletResponse response,
        String tLoggedInAs, String ipAddress) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(tLoggedInAs);
        OutputStream out = null;
        Writer writer = null;

        try {

            //get parameters
            String 
                tYourName         = request.getParameter("yourName"),
                tEmailAddress     = request.getParameter("emailAddress"),
                tTimestamp        = request.getParameter("timestamp"),
                tOtherComments    = request.getParameter("otherComments");

            //validate (same order as form)
            StringBuilder errorMsgSB = new StringBuilder();
            tYourName     = HtmlWidgets.validateIsSomethingNotTooLong(
                "Your Name",     "?", tYourName,     50, null);
            tEmailAddress = HtmlWidgets.validateIsSomethingNotTooLong(
                "Email Address", "?", tEmailAddress, 50, null);
            tTimestamp    = HtmlWidgets.validateIsSomethingNotTooLong(
                "Timestamp", Calendar2.getCurrentISODateTimeStringLocalTZ() + "?", 
                tTimestamp, 26, null);
            tOtherComments = HtmlWidgets.validateNotNullNotTooLong(
                "Other Comments", "", tOtherComments, 500, errorMsgSB);
            if (errorMsgSB.length() > 0)
                errorMsgSB.insert(0, 
                    "<br>Please fix these problems, then 'Submit' this part of the form again.\n");

            String fromInfo = tYourName  + " <" + tEmailAddress + "> at " + tTimestamp;

            //if this is a submission, 
            boolean isSubmission = "Submit".equals(request.getParameter("Submit"));
            if (isSubmission && errorMsgSB.length() == 0) {
                //convert the info into pseudo datasets.xml
                String content = 
                    "Data Provider Form - Part 4\n" + //important! Bob's erd.data gmail filter looks for this
                    "  from " + fromInfo + "\n" +
                    "  ipAddress=" + ipAddress + "\n" +
                    "\n" +
                    "Other comments:\n" + 
                    tOtherComments + "\n\n";

                //log the content to /logs/dataProviderForm.log
                String error = String2.appendFile(
                    EDStatic.fullLogsDirectory + "dataProviderForm.log",
                    "*** " + content);
                if (error.length() > 0)
                    String2.log(String2.ERROR + 
                        " while writing to logs/dataProviderForm.log:\n" +
                        error);
                //email the content to the admin
                EDStatic.email(EDStatic.adminEmail, 
                    "Data Provider Form - Part 4, from " + fromInfo,
                    content);

                //redirect to Done
                sendRedirect(response, tErddapUrl + "/dataProviderFormDone.html?" +
                    "yourName=" + SSR.minimalPercentEncode(tYourName) +
                    "&emailAddress=" + SSR.minimalPercentEncode(tEmailAddress) +
                    "&timestamp=" + SSR.minimalPercentEncode(tTimestamp));

                return;
            }

            //write the HTML
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(tLoggedInAs, "Data Provider Form - Part 4", out);
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(tLoggedInAs, "Data Provider Form - Part 4"));

            //begin form
            String formName = "f1";
            HtmlWidgets widgets = new HtmlWidgets(false, //style, false=not htmlTooltips
                EDStatic.imageDirUrl(tLoggedInAs)); 
            widgets.enterTextSubmitsForm = false; 
            writer.write(widgets.beginForm(formName, 
                //this could be POST to deal with lots of text. 
                //but better to change tomcat settings (above) and keep ease of use
                "GET", tErddapUrl + "/dataProviderForm4.html", "") + "\n");

            //hidden fields
            writer.write(
                widgets.hidden("yourName",     XML.encodeAsHTML(tYourName)) +
                widgets.hidden("emailAddress", XML.encodeAsHTML(tEmailAddress)) +
                widgets.hidden("timestamp",    XML.encodeAsHTML(tTimestamp)) + 
                "\n");

//begin text
writer.write(
"This is part 4 (of 4) of the Data Provider Form\n" +
"<br>from " + XML.encodeAsHTML(fromInfo) + ".\n" +
"<br>Need help? Send an email to the administrator of this ERDDAP (<kbd>" + 
    XML.encodeAsHTML(SSR.getSafeEmailAddress(EDStatic.adminEmail)) + "</kbd>).\n" +
"<br>&nbsp;\n" +
"\n");

//error message?
if (isSubmission && errorMsgSB.length() > 0) 
writer.write("<span class=\"warningColor\">" + errorMsgSB.toString() + "</span> " + 
    "<br>&nbsp;\n");

//other comments
writer.write(
"<h2>Other Comments</h2>\n" + 
"Optional: If there are other things you think the ERDDAP administrator\n" +
"should know about this dataset, please add them here.\n" +
"This won't go in the dataset's metadata or be made public. (&lt;500 characters)\n" +
"<br><textarea name=\"otherComments\" cols=\"" + dpfTAWidth + "\" rows=\"6\" maxlength=\"500\" wrap=\"soft\">" +
XML.encodeAsHTML(tOtherComments) +  //encoding is important for security
"</textarea>\n" +
"<br>&nbsp;\n" +
"\n");

//Submit
writer.write(
"<h2>Finished with part 4?</h2>\n" + 
"Click\n" + 
widgets.button("submit", "Submit", "", "Submit", "") +
"to send this information to the ERDDAP administrator and move on to the \"You're done!\" page.\n" +
"\n");

//end form
writer.write(widgets.endForm());        

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            if (writer != null)
                writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            if (writer != null) {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
        }
        return;
    }

    /**
     * This is Data Provider Form Done
     */
    public void doDataProviderFormDone(HttpServletRequest request, HttpServletResponse response,
        String tLoggedInAs) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(tLoggedInAs);
        OutputStream out = null;
        Writer writer = null;

        try {

            //global attributes (and contact info)
            String 
                tYourName         = request.getParameter("yourName"),
                tEmailAddress     = request.getParameter("emailAddress"),
                tTimestamp        = request.getParameter("timestamp");

            tYourName     = HtmlWidgets.validateIsSomethingNotTooLong(
                "Your Name",     "?", tYourName,     50, null);
            tEmailAddress = HtmlWidgets.validateIsSomethingNotTooLong(
                "Email Address", "?", tEmailAddress, 50, null);
            tTimestamp    = HtmlWidgets.validateIsSomethingNotTooLong(
                "Timestamp", Calendar2.getCurrentISODateTimeStringLocalTZ() + "?", 
                tTimestamp, 26, null);

            //write the HTML
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(tLoggedInAs, "Data Provider Form - Done", out);
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(tLoggedInAs, "Data Provider Form - Done"));

//begin text
writer.write(
"<h2>You're done! Congratulations! Thank you!</h2>\n" +
"The ERDDAP administrator will email you soon to figure out the best way transfer\n" +
"  the data and to work out other details.\n" +
"  This dataset submission's timestamp is " + tTimestamp + ".\n" + 
"<p>You can <a rel=\"bookmark\" href=\"" + tErddapUrl + "/dataProviderForm1.html?" +
    "yourName=" + SSR.minimalPercentEncode(tYourName) +
    "&amp;emailAddress=" + SSR.minimalPercentEncode(tEmailAddress) + "\">submit another dataset</a>\n" +
"or go back to the <a rel=\"bookmark\" href=\"" + tErddapUrl + 
    "/index.html\">ERDDAP home page</a>.\n");

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            if (writer != null) 
                writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            if (writer != null) {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
        }
        return;
    }



    /**
     * This responds to a request for status.html.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doStatus(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Status", out);
        try {
            int nGridDatasets = gridDatasetHashMap.size();
            int nTableDatasets = tableDatasetHashMap.size();
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "Status") +
                "<pre>");
            StringBuilder sb = new StringBuilder();
            EDStatic.addIntroStatistics(sb);

            //append number of active threads
            String traces = MustBe.allStackTraces(true, true);
            int po = traces.indexOf('\n');
            if (po > 0)
                sb.append(traces.substring(0, po + 1));

            sb.append(Math2.memoryString() + " " + Math2.xmxMemoryString() + "\n\n");
            EDStatic.addCommonStatistics(sb);
            sb.append(traces);
            writer.write(XML.encodeAsHTML(sb.toString()));
            writer.write("</pre>\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {

            //end of status.html
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);

            //as a convenience to admins, viewing status.html calls String2.flushLog()
            String2.flushLog();
        }

    }

    /**
     * This responds by sending out the "RESTful Web Services" information Html page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doRestHtml(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "RESTful Web Services", out);
        try {
            String htmlQueryUrl = tErddapUrl + "/search/index.html?" +
                EDStatic.encodedDefaultPIppQuery + "&#x26;searchFor=temperature";
            String jsonQueryUrl = tErddapUrl + "/search/index.json?" +
                EDStatic.encodedDefaultPIppQuery + "&#x26;searchFor=temperature";
            String htmlQueryUrlWithSpaces = htmlQueryUrl + "%20wind%20speed";
            String griddapExample  = tErddapUrl + "/griddap/" + EDStatic.EDDGridIdExample;
            String tabledapExample = tErddapUrl + "/tabledap/" + EDStatic.EDDTableIdExample;
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "RESTful Web Services") +
                "<h2 style=\"text-align:center;\"><a class=\"selfLink\" id=\"WebService\" href=\"#WebService\" rel=\"bookmark\">Accessing ERDDAP's RESTful Web Services</a></h2>\n" +
                "ERDDAP is both:\n" +
                "<ul>\n" +
                "<li><a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Web_application\">A web application" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> \n" +
                "  &ndash; a web page with a form that humans with browsers can use\n" +
                "  (in this case, to get data, graphs, or information about datasets).\n" +
                "  <br>&nbsp;\n" +
                "<li><a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Web_service\">A RESTful web service" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> \n" +
                "  &ndash; a URL that computer programs can use\n" +
                "  (in this case, to get data, graphs, and information about datasets).\n" +
                "</ul>\n" +
                "For every ERDDAP web page with a form that you as a human with a browser can use, there is a\n" +
                "<br>corresponding ERDDAP web service that is designed to be easy for computer programs and scripts to use.\n" +
                "For example, humans can use this URL to do a Full Text Search for interesting datasets:\n" +
                "<br><a href=\"" + htmlQueryUrl + "\">" + htmlQueryUrl + "</a>\n" +
                "<br>By changing the file extension in the URL from .html to .json (or .csv, or .htmlTable, or .jsonlCSV1, or .xhtml, ...):\n" +
                "<br><a href=\"" + jsonQueryUrl + "\">" + jsonQueryUrl + "</a>\n" +
                "<br>we get a URL that a computer program or JavaScript script can use to get the same\n" +
                "information in a more computer-program-friendly format like\n" +
                "<a rel=\"help\" href=\"https://www.json.org/\">JSON" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "\n" +
                "<h3><a class=\"selfLink\" id=\"BuildThings\" href=\"#BuildThings\" rel=\"bookmark\">Build Things on Top of ERDDAP</a></h3>\n" +
                "There are many features in ERDDAP that can be used by computer programs or scripts\n" +
                "that you write. You can use them to build other web applications or web services on\n" +
                "top of ERDDAP, making ERDDAP do most of the work!\n" +
                "So if you have an idea for a better interface to the data that ERDDAP serves or a web\n" +
                "page that needs an easy way to access data, we encourage you to build your own\n" +
                "web application, web service, or web page and use ERDDAP as the foundation.\n" +
                "Your system can get data, graphs, and other information from ERD's ERDDAP or from\n" +
                "other ERDDAP installations, or you can \n" +
                //setup always from coastwatch's erddap 
                "  <a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/setup.html\">set up your own ERDDAP server</a>,\n" + 
                "  which can be\n" +
                "publicly accessible or just privately accessible.\n" +
                "\n" +
                //requests
                "<h3><a class=\"selfLink\" id=\"requests\" href=\"#requests\" rel=\"bookmark\">RESTful URL Requests</a></h3>\n" +
                "Requests for user-interface information from ERDDAP (for example, search results)\n" +
                "use the web's universal standard for requests:\n" +
                "<a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Uniform_Resource_Locator\">URLs" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "sent via\n" +
                "<a href=\"https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3\">HTTP GET" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "This is the same mechanism that your browser uses when you fill out a form\n" +
                "on a web page and click on <kbd>Submit</kbd>.\n" +
                "To use HTTP GET, you generate a specially formed URL (which may include a query)\n" +
                "and send it with HTTP GET.  You can form these URLs by hand and enter them in\n" +
                "the address text field of your browser (for example,\n" +
                "<br><a href=\"" + jsonQueryUrl + "\">" + jsonQueryUrl + "</a>)\n" +
                "<br>Or, you can write a computer program or web page script to create a URL, send it,\n" +
                "and get the response.  URLs via HTTP GET were chosen because\n" +
                "<ul>\n" +
                "<li> They are simple to use.\n" +
                "<li> They work well.\n" +
                "<li> They are universally supported (in browsers, computer languages, operating system\n" +
                "  tools, etc).\n" +
                "<li> They are a foundation of\n" +
                "  <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Representational_State_Transfer\">Representational State Transfer (REST)" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> and\n" +
                "  <a rel=\"help\" href=\"https://www.crummy.com/writing/RESTful-Web-Services/\">Resource Oriented Architecture (ROA)" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "<li> They facilitate using the World Wide Web as a big distributed application,\n" +
                "  for example via\n" +
                "  <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Mashup_%28web_application_hybrid%29\">mashups" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> and\n" +
                "  <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Ajax_%28programming%29\">AJAX applications" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "<li> They are <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Stateless_protocol\">stateless" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>,\n" +
                "  as is ERDDAP, which makes the system simpler.\n" +
                "<li> A URL completely define a given request, so you can bookmark it in your browser,\n" +
                "  write it in your notes, email it to a friend, etc.\n" +
                "</ul>\n" +
                "\n" +
                "<h3><a class=\"selfLink\" id=\"PercentEncode\" href=\"#PercentEncode\" rel=\"bookmark\">Percent Encoding</a></h3>\n" +
                "In URLs, some characters are not allowed (for example, spaces) and other characters\n" +
                "have special meanings (for example, '&amp;' separates key=value pairs in a query).\n" +
                "When you fill out a form on a web page and click on Submit, your browser automatically\n" +
                "<a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encodes" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "  the special characters in the URL (for example, space becomes %20), for example,\n" +
                "<br><a href=\"" + htmlQueryUrlWithSpaces + "\">" + htmlQueryUrlWithSpaces + "</a>\n" +
                "<br>But if your computer program or script generates the URLs, it probably needs to do the percent\n" +
                "encoding itself.  If so, then probably all characters other than A-Za-z0-9_-!.~'()*\n" +
                "in the query's values (the parts after the '=' signs) need to be encoded\n" +
                "as %HH, where HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n" +
                "Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n" +
                "(ask a programmer for help). Programming languages have tools to do this (for example, see Java's\n" +
                "<a rel=\"help\" href=\"https://docs.oracle.com/javase/8/docs/api/index.html?java/net/URLEncoder.html\">java.net.URLEncoder" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> and JavaScript's\n" +
                "<a rel=\"help\" href=\"https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent\">encodeURIComponent()" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>) and there are\n" +
                "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com/\">websites that percent encode/decode for you" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "\n" +  

                //responses
                "<h3><a class=\"selfLink\" id=\"responses\" href=\"#responses\" rel=\"bookmark\">Response File Types</a></h3>\n" +
                "Although humans using browsers want to receive user-interface results (for example,\n" +
                "search results) as HTML documents, computer programs often prefer to get results in\n" +
                "simple, easily parsed, less verbose documents.  ERDDAP can return user-interface\n" +
                "results as a table of data in these common, computer-program friendly, file types:\n" +
                "<ul>\n" + //list of plainFileTypes
                "<li>.csv - a comma-separated ASCII text table.\n" +
                    "(<a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Comma-separated_values\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.htmlTable - an .html web page with the data in a table.\n" +
                    "(<a rel=\"help\" href=\"https://www.w3schools.com/html/html_tables.asp\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.itx - an Igor Text File with a wave for each column of data.\n" +
                    "(<a rel=\"help\" href=\"https://www.wavemetrics.net/doc/igorman/II-09%20Data%20Import%20Export.pdf\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.json - a table-like JSON file.\n" +
                    "(<a rel=\"help\" href=\"https://www.json.org/\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> or\n" +
                    "<a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html#json\">ERDDAP-specific info</a>)\n" +
                "<li>.jsonlCSV1 - a \"Better than CSV\" JSON Lines file with column names on the first line.\n" +
                    "(<a rel=\"help\" href=\"http://jsonlines.org/\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.jsonlCSV - a \"Better than CSV\" JSON Lines file with no column names.\n" +
                    "(<a rel=\"help\" href=\"http://jsonlines.org/\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.jsonlKVP - a JSON Lines file with Key:Value pairs.\n" +
                    "(<a rel=\"help\" href=\"http://jsonlines.org/\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.mat - a MATLAB binary file.\n" +
                    "(<a rel=\"help\" href=\"https://www.mathworks.com/\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.nc - a flat, table-like, NetCDF-3 binary file.\n" +
                    "(<a rel=\"help\" href=\"https://www.unidata.ucar.edu/software/netcdf/\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.nccsv - a flat, table-like, NetCDF-like, ASCII CSV file.\n" +
                    "(<a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.tsv - a tab-separated ASCII text table.\n" +
                    "(<a rel=\"help\" href=\"http://jkorpela.fi/TSV.html\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "<li>.xhtml - an XHTML (XML) file with the data in a table.\n" +
                    "(<a rel=\"help\" href=\"https://www.w3schools.com/html/html_tables.asp\">more&nbsp;info" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>)\n" +
                "</ul>\n" +
                "In every results table format (except .jsonlKVP, where column names are on every row):\n" +
                "<ul>\n" +
                "<li>Each column has a column name and one type of information.\n" +
                "<li>The first row of the table has the column names.\n" +
                "<li>Subsequent rows have the information you requested.\n" +
                "</ul>\n" +
                "<p>The content in these plain file types is also slightly different from the .html\n" +
                "response &mdash; it is intentionally bare-boned so that it is easier for a computer\n" +
                "program to work with.\n" +
                "\n" +
                "<p><a class=\"selfLink\" id=\"DataStructure\" href=\"#DataStructure\" rel=\"bookmark\">A Consistent Data Structure for the Responses</a>\n" +
                "<br>All of the user-interface services described on this page can return a table of\n" +
                "data in any of the common file formats listed above. Hopefully, you can write\n" +
                "just one procedure to parse a table of data in one of the formats. Then you can\n" +
                "re-use that procedure to parse the response from any of these services.  This\n" +
                "should make it easier to deal with ERDDAP.\n" +
                "\n" +
                //csvIssues
                "<p><a class=\"selfLink\" id=\"csvIssues\" href=\"#csvIssues\" rel=\"bookmark\">.csv</a> and .tsv Details<ul>\n" +
                "<li>If a datum in a .csv file has internal double quotes or commas, ERDDAP follows the\n" +
                "  .csv specification strictly: it puts double quotes around the datum and doubles\n" +
                "  the internal double quotes.\n" +
                "<li>Special characters in a .csv or .tsv file are encoded like\n" +
                "  <a rel=\"help\" href=\"https://www.json.org/\">JSON" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "  backslash-encoded\n" +
                "  characters: \\n (newline), \\\\ (backslash), \\f (formfeed), \\t (tab), \\r (carriage return)\n" +
                "  or with the \\u<i>hhhh</i> syntax.\n" +
                "</ul>\n" +
                "\n" +
                //jsonp
                "<p><a class=\"selfLink\" id=\"jsonp\" href=\"#jsonp\" rel=\"bookmark\">jsonp</a>\n" +
                "<br>Requests for .json files may now include an optional" +
                "  <a href=\"https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/\">jsonp" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> request by\n" +
                "adding \"&amp;.jsonp=<i>functionName</i>\" to the end of the query.  Basically, this tells\n" +
                "ERDDAP to add \"<i>functionName</i>(\" to the beginning of the response and \")\" to the\n" +
                "end of the response. The first character of <i>functionName</i> must be an ISO 8859 letter or \"_\".\n" +
                "Each optional subsequent character must be an ISO 8859 letter, \"_\", a digit, or \".\".\n" +
                "If originally there was no query, leave off the \"&amp;\" in your query.\n" +
                "\n" + 

                "<p>griddap and tabledap Offer Different File Types\n" +
                "<br>The file types listed above are file types ERDDAP can use to respond to\n" +
                "user-interface types of requests (for example, search requests). ERDDAP supports\n" +
                "a different set of file types for scientific data (for example, satellite and buoy\n" +
                "data) requests (see the \n" +
                "  <a rel=\"help\" href=\"" + tErddapUrl + "/griddap/documentation.html#fileType\">griddap</a> and\n" +
                "  <a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html#fileType\">tabledap</a>\n" +
                "  documentation).\n" +
                "\n" +

                //compression
                EDStatic.acceptEncodingHtml("h3", tErddapUrl) +
                "\n" +

                //accessUrls
                "<h3><a class=\"selfLink\" id=\"accessUrls\" href=\"#accessUrls\" rel=\"bookmark\">Access URLs for ERDDAP's Services</a></h3>\n" +
                "ERDDAP has these URL access points for computer programs:\n" +
                "<ul>\n" +
                "<li>To get the list of the <strong>main resource access URLs</strong>, use\n" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/index", "") +
                "  <br>&nbsp;\n" +
                "<li>To get the current list of <strong>all datasets</strong>, use\n" + 
                "  <br>" + plainLinkExamples(tErddapUrl, 
                    "/info/index", EDStatic.encodedAllPIppQuery) + 
                "  <br>&nbsp;\n" +
                "<li>To get <strong>metadata</strong> for a specific data set\n" +
                "  (the list of variables and their attributes), use\n" + 
                "  <br>" + tErddapUrl + "/info/<i>datasetID</i>/index<i>.fileType</i>\n" +
                "  <br>for example,\n" + 
                "  <br>" + plainLinkExamples(tErddapUrl, 
                    "/info/" + EDStatic.EDDGridIdExample + "/index", "") + 
                "  <br>&nbsp;\n" +
                "<li>To get the results of <strong>full text searches</strong> for datasets\n" +
                "  (using \"searchFor=wind%20speed\" as the example), use\n" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/search/index", 
                    EDStatic.encodedDefaultPIppQuery + 
                    "&amp;searchFor=wind%20speed") +
                "  <br>(Your program or script may need to \n" +
                "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent-encode" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "    the value in the query.)\n" +
                "  <br>&nbsp;\n" +
                "  <br>Or, use the\n" +
                "    <a rel=\"bookmark\" href=\"" + tErddapUrl + "/opensearch1.1/index.html\">OpenSearch 1.1</a>\n" +
                "    standard to do a full text search for datasets.\n" +
                "  <br>&nbsp;\n" +
                "<li>To get the results of <strong>advanced searches</strong> for datasets\n" +
                "  (using \"searchFor=wind%20speed\" as the example), use\n" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/search/advanced", 
                    EDStatic.encodedDefaultPIppQuery + 
                    "&amp;searchFor=wind%20speed") +
                "  <br>But experiment with\n" +
                "    <a href=\"" + tErddapUrl + "/search/advanced.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\">" + EDStatic.advancedSearch + "</a>\n" +
                "    in a browser to figure out all of the optional parameters.\n" +
                "  (Your program or script may need to \n" +
                "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent-encode" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "    the value in the query.)\n" +
                "  <br>&nbsp;\n" +
                "<li>To get the list of <strong>categoryAttributes</strong>\n" +
                "  (for example, institution, long_name, standard_name), use\n" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/categorize/index", 
                    EDStatic.encodedDefaultPIppQuery) + 
                "  <br>&nbsp;\n" +
                "<li>To get the list of <strong>categories for a specific categoryAttribute</strong>\n" +
                "  (using \"standard_name\" as the example), use\n" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/categorize/standard_name/index", 
                    EDStatic.encodedDefaultPIppQuery) + 
                "  <br>&nbsp;\n" +
                "<li>To get the list of <strong>datasets in a specific category</strong>\n" +
                "  (using \"standard_name=time\" as the example), use\n" +
                "  <br>" +  plainLinkExamples(tErddapUrl, "/categorize/standard_name/time/index", 
                    EDStatic.encodedDefaultPIppQuery));  
            int tDasIndex = String2.indexOf(EDDTable.dataFileTypeNames, ".das");
            int tDdsIndex = String2.indexOf(EDDTable.dataFileTypeNames, ".dds");
            writer.write(
                "  <br>&nbsp;\n" +
                "<li>To get the current list of <strong>all datasets available via a specific protocol</strong>,\n" +
                "  <ul>\n" +
                "  <li>For griddap: use\n<br>" +
                    plainLinkExamples(tErddapUrl, "/griddap/index", 
                    EDStatic.encodedAllPIppQuery) + 
                "  <li>For tabledap: use\n<br>" +
                    plainLinkExamples(tErddapUrl, "/tabledap/index", 
                    EDStatic.encodedAllPIppQuery)); 
            if (EDStatic.sosActive) writer.write(
                "  <li>For SOS: use\n<br>" +
                    plainLinkExamples(tErddapUrl, "/sos/index", 
                    EDStatic.encodedAllPIppQuery)); 
            if (EDStatic.wcsActive) writer.write(
                "  <li>For WCS: use\n<br>" +
                    plainLinkExamples(tErddapUrl, "/wcs/index", 
                    EDStatic.encodedAllPIppQuery)); 
            if (EDStatic.wmsActive) writer.write(
                "  <li>For WMS: use\n<br>" +
                    plainLinkExamples(tErddapUrl, "/wms/index", 
                    EDStatic.encodedAllPIppQuery));
            writer.write(
                "  <br>&nbsp;\n" +
                "  </ul>\n" +
                "<li><a class=\"selfLink\" id=\"GriddapAndTabledap\" href=\"#GriddapAndTabledap\" rel=\"bookmark\">Griddap and tabledap</a> have many web services that you can use.\n" +
                "  <ul>\n" +
                "  <li>The Data Access Forms are just simple web pages to generate URLs which\n" +
                "    request <strong>data</strong> (for example, satellite and buoy data).  The data can be in any of\n" +
                "    several common file formats. Your program can generate these URLs directly.\n" +
                "    For more information, see the\n" +
                "    <a rel=\"help\" href=\"" + tErddapUrl + "/griddap/documentation.html\">griddap documentation</a> and\n" +
                "    <a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html\">tabledap documentation</a>.\n" +
                "    <br>&nbsp;\n" +
                "  <li>The Make A Graph pages are just simple web pages to generate URLs which\n" +
                "    request <strong>graphs</strong> of a subset of the data.  The graphs can be in any of several\n" +
                "    common file formats.  Your program can generate these URLs directly. For\n" +
                "    more information, see the\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/griddap/documentation.html\">griddap documentation</a> and\n" +
                "      <a rel=\"help\" href=\"" + tErddapUrl + "/tabledap/documentation.html\">tabledap documentation</a>.\n" +
                "    <br>&nbsp;\n" +
                "  <li>To get a <strong>dataset's structure</strong>, including variable names and data types,\n" +
                "    use a standard OPeNDAP\n" +
                "      <a rel=\"help\" href=\"" + XML.encodeAsHTMLAttribute(EDDTable.dataFileTypeInfo[tDdsIndex]) + "\">.dds" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "      resquest. For example,\n" + 
                "    <br><a href=\"" + griddapExample  + ".dds\">" + griddapExample  + ".dds</a> (gridded data) or\n" +
                "    <br><a href=\"" + tabledapExample + ".dds\">" + tabledapExample + ".dds</a> (tabular data).\n" +
                "    <br>&nbsp;\n" +
                "  <li>To get a <strong>dataset's metadata</strong>, use a standard OPeNDAP\n" +
                "      <a rel=\"help\" href=\"" + XML.encodeAsHTMLAttribute(EDDTable.dataFileTypeInfo[tDasIndex]) + "\">.das" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "      resquest.\n" +
                "    For example,\n" + 
                "    <br><a href=\"" + griddapExample  + ".das\">" + griddapExample  + ".das</a> (gridded data) or\n" +
                "    <br><a href=\"" + tabledapExample + ".das\">" + tabledapExample + ".das</a> (tabular data).\n" +
                "    <br>&nbsp;\n" +
                "  <li>ERDDAP has a special tabular dataset called\n" +
                "      <a href=\"" + tErddapUrl + "/tabledap/allDatasets.html\"><strong>allDatasets</strong></a>\n" +
                "    which has data about all of the datasets currently available in\n" +
                "    this ERDDAP.  There is a row for each dataset. There are columns with\n" +
                "    different types of information (for example, datasetID, title, summary,\n" +
                "    institution, license, Data Access Form URL, Make A Graph URL).\n" +
                "    Because this is a tabledap dataset,\n" +
                "    you can use a tabledap data request to request\n" +
                "    specific columns and rows which match the constraints, and you can\n" +
                "    get the response in whichever response file type you prefer,\n" +
                "    for example, .html, .xhtml, .csv, .json, .jsonlCSV1, .jsonlCSV, or .jsonlKVP.\n" +
                "    <br>&nbsp;\n" +
                "  </ul>\n");
            if (EDStatic.sosActive || EDStatic.wcsActive || EDStatic.wmsActive) {
                writer.write(
                "<li><a class=\"selfLink\" id=\"OtherProtocols\" href=\"#OtherProtocols\" rel=\"bookmark\"\n" +
                ">ERDDAP's other protocols</a> also have web services that you can use.\n" +
                "  See\n" +
                "  <ul>\n");
                if (EDStatic.sosActive) writer.write(
                    "    <li><a rel=\"help\" href=\"" + tErddapUrl + "/sos/documentation.html\">ERDDAP's SOS documentation</a>\n");
                if (EDStatic.wcsActive) writer.write(
                     "   <li><a rel=\"help\" href=\"" + tErddapUrl + "/wcs/documentation.html\">ERDDAP's WCS documentation</a>\n");
                if (EDStatic.wmsActive) writer.write(
                    "    <li><a rel=\"help\" href=\"" + tErddapUrl + "/wms/documentation.html\">ERDDAP's WMS documentation</a>\n");
                writer.write(
                    "    <br>&nbsp;\n" +
                    "    </ul>\n");
            }
            writer.write(
                "<li><a class=\"selfLink\" id=\"subscriptions\" href=\"#subscriptions\" rel=\"bookmark\">ERDDAP</a> offers \n" +
                "    <a rel=\"help\" href=\"" + tErddapUrl + "/information.html#subscriptions\">RSS subscriptions</a>,\n" +
                "    so that your computer program can find out if a\n" +
                "  dataset has changed.\n" +
                "  <br>&nbsp;\n");
            if (EDStatic.subscriptionSystemActive) writer.write(
                "<li>ERDDAP offers \n" +
                "    <a rel=\"help\" href=\"" + tErddapUrl + "/information.html#subscriptions\">email/URL subscriptions</a>,\n" +
                "    which notify your computer program\n" +
                "  whenever a dataset changes.\n" +
                "  <br>&nbsp;\n");
            writer.write(
                "<li>ERDDAP offers several converters as web pages and as web services:\n" +
                (EDStatic.convertersActive?
                  "  <ul>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/oceanicAtmosphericAcronyms.html#computerProgram\">" + EDStatic.convertOceanicAtmosphericAcronyms + "</a>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/oceanicAtmosphericVariableNames.html#computerProgram\">" + EDStatic.convertOceanicAtmosphericVariableNames + "</a>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/fipscounty.html#computerProgram\">" + EDStatic.convertFipsCounty + "</a>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/keywords.html#computerProgram\">" + EDStatic.convertKeywords + "</a>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/time.html#computerProgram\">" + EDStatic.convertTime + "</a>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/units.html#computerProgram\">" + EDStatic.convertUnits + "</a>\n" +
                  "  <li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/urls.html#computerProgram\">" + EDStatic.convertURLs + "</a>\n" +
                  "    <br>&nbsp;\n" +
                  "  </ul>\n" :
                  "<br> (" + MessageFormat.format(EDStatic.disabled, "convert") + ")\n<br>&nbsp;\n"));
            if (EDStatic.outOfDateDatasetsActive) writer.write(
                "<li>ERDDAP has a system to keep track of\n" +
                "    <a rel=\"help\" href=\"" + tErddapUrl + "/outOfDateDatasets.html\">Out-Of-Date Datasets</a>.\n" +
                "    See the Options at the bottom of that web page.\n" +
                "  <br>&nbsp;\n");
            writer.write(
                "</ul>\n" +
                "If you have suggestions for additional links, contact <kbd>bob dot simons at noaa dot gov</kbd>.\n");

            //JavaPrograms
            writer.write(
                "<h2><a class=\"selfLink\" id=\"JavaPrograms\" href=\"#JavaPrograms\" rel=\"bookmark\">Using ERDDAP as a Data Source within Your Java Program</a></h2>\n" +
                "As described above, since Java programs can access data available on the web, you can\n" +
                "write a Java program that accesses data from any publicly accessible ERDDAP installation.\n" +
                "\n" +
                "<p>Or, since ERDDAP is an all-open source program, you can also set up your own copy of\n" +
                "ERDDAP on your own server (publicly accessible or not) to serve your own data. Your Java\n" +
                "programs can get data from that copy of ERDDAP. See\n" +
                //setup.html always from coastwatch's erddap
                "  <a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/setup.html\">Set Up Your Own ERDDAP</a>.\n");

            //login
            writer.write(
                "<h2><a class=\"selfLink\" id=\"login\" href=\"#login\" rel=\"bookmark\">Log in to access private datasets.</a></h2>\n" +
                "Many ERDDAP installations don't have authentication enabled and thus\n" +
                "don't provide any way for users to login, nor do they have any private datasets.\n" +
                "<p>Some ERDDAP installations do have authentication enabled.\n" +
                "Currently, ERDDAP only supports authentication via Google-managed email accounts,\n" +
                "which includes email accounts at NOAA and many universities.\n" +
                "If an ERDDAP has authentication enabled, anyone with a Google-managed email account\n" +
                "can log in, but they will only have access to the private datasets\n" +
                "that the ERDDAP administrator has explicitly authorized them to access.\n" +
                "For instructions on logging into ERDDAP from a browser or via a script, see\n" +
                "<a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/AccessToPrivateDatasets.html\">Access to Private Datasets in ERDDAP</a>.\n" +
                "\n");

            //erddap version
            writer.write(
                "<h2><a class=\"selfLink\" id=\"version\" href=\"#version\" rel=\"bookmark\">ERDDAP Version</a></h2>\n" +
                "If you want to use a new feature on a remote ERDDAP, you can find out if the new\n" +
                "feature is available by sending a request to determine the ERDDAP's version\n" +
                "number, for example,\n" +
                "<br><a href=\"" + tErddapUrl + "/version\">" + tErddapUrl + "/version</a>" +
                "<br>ERDDAP will send a text response with the ERDDAP version number of that ERDDAP.\n" +
                "For example:\n" +
                "<kbd>ERDDAP_version=" + EDStatic.erddapVersion + "</kbd>\n" +
                "<br>If you get an <kbd>HTTP 404 Not-Found</kbd> error message, treat the ERDDAP as version\n" +
                "1.22 or lower.\n" +
                "<p>Or, you can request the version_string, which may have additional information.\n" +
                "For example,\n" +
                "<br><a href=\"" + tErddapUrl + "/version_string\">" + tErddapUrl + "/version_string</a>" +
                "<br>ERDDAP will send a text response with the ERDDAP version_string of that ERDDAP.\n" +
                "It will be a floating point number (the version number)\n" +
                "with an optional suffix of '_' plus additional ASCII text (no spaces or control characters).\n" +
                "For example:\n" +
                "<kbd>ERDDAP_version_string=1.82_JohnsFork</kbd>\n" +
                "<br>If you get an <kbd>HTTP 404 Not-Found</kbd> error message, treat the ERDDAP as version\n" +
                "1.80 or lower.\n" +
                "\n");

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * This responds by sending out the sitemap.xml file.
     * <br>See https://www.sitemaps.org/protocol.php
     * <br>This uses the startupDate as the lastmod date.
     * <br>This uses changefreq=monthly. Datasets may change in small ways (e.g., near-real-time data), 
     *   but that doesn't affect the metadata that search engines are interested in.
     */
    public void doSitemap(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        //always use plain EDStatic.erddapUrl
        String pre = 
            "<url>\n" +
            "<loc>" + EDStatic.erddapUrl + "/";
        String basicPost =
            "</loc>\n" +
            "<lastmod>" + EDStatic.startupLocalDateTime.substring(0,10) + "</lastmod>\n" +
            "<changefreq>monthly</changefreq>\n" +
            "<priority>";
        //highPriority
        String postHigh = basicPost + "0.7</priority>\n" +  
            "</url>\n" +
            "\n";
        //medPriority
        String postMed = basicPost + "0.5</priority>\n" +  //0.5 is the default
            "</url>\n" +
            "\n";
        //lowPriority
        String postLow = basicPost + "0.3</priority>\n" +
            "</url>\n" +
            "\n";

        //beginning
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "sitemap", ".xml", ".xml");
        Writer writer = new BufferedWriter(new OutputStreamWriter(outSource.outputStream(String2.UTF_8), String2.UTF_8));
        try {
            writer.write(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                //this is their simple example from https://www.google.com/sitemaps/protocol.html
                //note http in their example despite https being available
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                //this is what they recommend to validate it, but it doesn't validate for me ('urlset' not defined)
                //"<urlset xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n" +
                //"    xsi:schemaLocation=\"https://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
                //"    url=\"https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\"\n" +
                //"    xmlns=\"https://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                "\n");

            //write the individual urls
            //don't include the admin pages that all link to ERD's erddap
            //don't include setDatasetFlag.txt, setup.html, setupDatasetsXml.html, status.html, 
            writer.write(pre); writer.write("categorize/index.html");             writer.write(postMed);
            if (EDStatic.convertersActive) {
                writer.write(pre); writer.write("convert/index.html");                    writer.write(postMed);
                writer.write(pre); writer.write("convert/oceanicAtmosphericAcronyms.html");     writer.write(postHigh);
                writer.write(pre); writer.write("convert/oceanicAtmosphericVariableNames.html");writer.write(postHigh);
                writer.write(pre); writer.write("convert/fipscounty.html");               writer.write(postHigh);
                writer.write(pre); writer.write("convert/keywords.html");                 writer.write(postHigh);
                writer.write(pre); writer.write("convert/time.html");                     writer.write(postHigh);
                writer.write(pre); writer.write("convert/units.html");                    writer.write(postHigh);
            }
            //Don't include /files. We don't want search engines downloading all the files.
            writer.write(pre); writer.write("griddap/documentation.html");        writer.write(postHigh);
            writer.write(pre); writer.write("griddap/index.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh);
            writer.write(pre); writer.write("images/embed.html");                 writer.write(postHigh);
            //writer.write(pre); writer.write("images/gadgets/GoogleGadgets.html"); writer.write(postHigh);
            writer.write(pre); writer.write("index.html");                        writer.write(postHigh);
            writer.write(pre); writer.write("info/index.html?" + 
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh); 
            writer.write(pre); writer.write("information.html");                  writer.write(postHigh);
            if (EDStatic.fgdcActive) {
            writer.write(pre); writer.write(EDStatic.fgdcXmlDirectory);           writer.write(postLow);}
            if (EDStatic.iso19115Active) {
            writer.write(pre); writer.write(EDStatic.iso19115XmlDirectory);       writer.write(postLow);}
            writer.write(pre); writer.write("legal.html");                        writer.write(postHigh);
            writer.write(pre); writer.write("rest.html");                         writer.write(postHigh);
            writer.write(pre); writer.write("search/advanced.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh); 
            writer.write(pre); writer.write("search/index.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh); 
            if (EDStatic.slideSorterActive) {
            writer.write(pre); writer.write("slidesorter.html");                  writer.write(postHigh); 
            }
            if (EDStatic.sosActive) {
            writer.write(pre); writer.write("sos/documentation.html");            writer.write(postHigh);
            writer.write(pre); writer.write("sos/index.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh);
            }
            if (EDStatic.subscriptionSystemActive) {
            writer.write(pre); writer.write("subscriptions/index.html");          writer.write(postHigh); 
            writer.write(pre); writer.write("subscriptions/add.html");            writer.write(postMed); 
            writer.write(pre); writer.write("subscriptions/validate.html");       writer.write(postMed);
            writer.write(pre); writer.write("subscriptions/list.html");           writer.write(postMed); 
            writer.write(pre); writer.write("subscriptions/remove.html");         writer.write(postMed);
            }
            writer.write(pre); writer.write("tabledap/documentation.html");       writer.write(postHigh);
            writer.write(pre); writer.write("tabledap/index.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh);
            if (EDStatic.wcsActive) {
            writer.write(pre); writer.write("wcs/documentation.html");            writer.write(postHigh);
            writer.write(pre); writer.write("wcs/index.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh);
            }
            if (EDStatic.wmsActive) {
            writer.write(pre); writer.write("wms/documentation.html");            writer.write(postHigh);
            writer.write(pre); writer.write("wms/index.html?" +
                EDStatic.encodedAllPIppQuery);                                    writer.write(postHigh);
            }

            //special links only for ERD's erddap
            if (EDStatic.baseUrl.equals("http://coastwatch.pfeg.noaa.gov") || 
                EDStatic.baseUrl.equals("https://coastwatch.pfeg.noaa.gov")) {
                writer.write(pre); writer.write("download/grids.html");                 writer.write(postHigh);
                writer.write(pre); writer.write("download/setup.html");                 writer.write(postHigh);
                writer.write(pre); writer.write("download/setupDatasetsXml.html");      writer.write(postHigh);
            }

            //write the dataset .html, .subset, .graph, wms, wcs, sos, ... urls
            //Don't include /files. We don't want search engines downloading all the files.
            StringArray sa = gridDatasetIDs();
            sa.sortIgnoreCase();
            int n = sa.size();
            String gPre = pre + "griddap/";
            String iPre = pre + "info/";
            String tPre = pre + "tabledap/";
            String sPre = pre + "sos/";
            String cPre = pre + "wcs/";
            String mPre = pre + "wms/";
            for (int i = 0; i < n; i++) {
                //don't include index/datasetID, .das, .dds; better that people go to .html or .graph
                String dsi = sa.get(i);
                writer.write(gPre); writer.write(dsi); writer.write(".html");        writer.write(postMed);    
                writer.write(iPre); writer.write(dsi); writer.write("/index.html");  writer.write(postLow);
                //EDDGrid doesn't do SOS
                EDDGrid eddg = gridDatasetHashMap.get(dsi);
                if (eddg != null) {
                    if (eddg.accessibleViaMAG().length() == 0) {
                        writer.write(gPre); writer.write(dsi); writer.write(".graph");       writer.write(postMed);
                    }
                    if (eddg.accessibleViaWCS().length() == 0) {
                        writer.write(cPre); writer.write(dsi); writer.write("/index.html");  writer.write(postLow);
                    }
                    if (eddg.accessibleViaWMS().length() == 0) {
                        writer.write(mPre); writer.write(dsi); writer.write("/index.html");  writer.write(postLow);
                    }
                }
            }

            sa = tableDatasetIDs();
            sa.sortIgnoreCase(); 
            n = sa.size();
            for (int i = 0; i < n; i++) {
                String dsi = sa.get(i);
                writer.write(tPre); writer.write(dsi); writer.write(".html");        writer.write(postMed);
                writer.write(iPre); writer.write(dsi); writer.write("/index.html");  writer.write(postLow);
                //EDDTable currently don't do wms or wcs
                EDD edd = tableDatasetHashMap.get(dsi);
                if (edd != null) {
                    if (edd.accessibleViaMAG().length() == 0) {
                        writer.write(tPre); writer.write(dsi); writer.write(".graph");       writer.write(postMed);
                    }
                    if (edd.accessibleViaSubset().length() == 0) {
                        writer.write(tPre); writer.write(dsi); writer.write(".subset");      writer.write(postMed);
                    }
                    if (edd.accessibleViaSOS().length() == 0) {
                        writer.write(sPre); writer.write(dsi); writer.write("/index.html");  writer.write(postLow);
                    }
                }
            }

            //write the category urls
            for (int ca1 = 0; ca1 < EDStatic.categoryAttributes.length; ca1++) {
                String ca1InURL = EDStatic.categoryAttributesInURLs[ca1];
                StringArray cats = categoryInfo(ca1InURL);
                int nCats = cats.size();
                String catPre = pre + "categorize/" + ca1InURL + "/";
                writer.write(catPre); writer.write("index.html"); writer.write(postMed);
                for (int ca2 = 0; ca2 < nCats; ca2++) {
                    writer.write(catPre); writer.write(cats.get(ca2)); writer.write("/index.html"); writer.write(postMed);
                }
            }

            //end
            writer.write(
                "</urlset>\n"); 
        } finally {
            writer.close(); //it flushes
        }
    }

    /**
     * This is used to generate examples for the plainFileTypes in the method above.
     * 
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @param relativeUrl without the fileType, e.g., "/griddap/index"  (no "?" or "?query" at end)
     * @param query  after the "?",  already HTML encoded, e.g., "searchfor=temperature" or "".
     * @return a string with a series of html links to information about the plainFileTypes
     */
    protected String plainLinkExamples(String tErddapUrl,
        String relativeUrl, String query) throws Throwable {

        StringBuilder sb = new StringBuilder();
        int n = plainFileTypes.length;
        for (int pft = 0; pft < n; pft++) {
            sb.append(
                "    <a href=\"" + tErddapUrl + relativeUrl + 
                    plainFileTypes[pft] + EDStatic.questionQuery(query) + "\">" + 
                plainFileTypes[pft] + "</a>");
            if (pft <= n - 3) sb.append(",\n");
            if (pft == n - 2) sb.append(", or\n");
            if (pft == n - 1) sb.append(".\n");
        }
        return sb.toString();
    }


    /**
     * Process a grid or table OPeNDAP DAP-style request.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    ("griddap" or "tabledap") in the requestUrl
     * @param userDapQuery  post "?".  Still percentEncoded.  May be "".  May not be null.
     */
    public void doDap(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs,
        String protocol, int datasetIDStartsAt, String userDapQuery) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);       
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String fileTypeName = "";
        boolean hasDatasetID = datasetIDStartsAt < requestUrl.length();
        String endOfRequestUrl = hasDatasetID? requestUrl.substring(datasetIDStartsAt) : "";

        //respond to a documentation.html request
        if (endOfRequestUrl.equals("documentation.html")) {

            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, protocol + " Documentation", out); 
            try {
                writer.write("<div class=\"standard_width\">\n");
                writer.write(EDStatic.youAreHere(loggedInAs, protocol, "Documentation"));
                if (protocol.equals("griddap"))       
                    EDDGrid.writeGeneralDapHtmlInstructions(tErddapUrl, writer, true);
                else if (protocol.equals("tabledap")) 
                    EDDTable.writeGeneralDapHtmlInstructions(tErddapUrl, writer, true);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t;
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        //first, always set the standard DAP response header info
        EDStatic.standardDapHeader(response);

        //redirect to index.html
        if (endOfRequestUrl.equals("") ||
            endOfRequestUrl.equals("index.htm")) {
            sendRedirect(response, tErddapUrl + "/" + protocol + "/index.html?" +
                EDStatic.passThroughPIppQueryPage1(request));  
            return;
        }      
        
        //respond to a version request (see opendap spec section 7.2.5)
        if (endOfRequestUrl.equals("version") ||
            endOfRequestUrl.startsWith("version.") ||
            endOfRequestUrl.endsWith(".ver")) {

            //write version response 
            //DAP 2.0 7.1.1 says version requests DON'T include content-description header.
            OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                request, response, "version", //fileName is not used
                ".txt", ".txt");
            OutputStream out = outSource.outputStream(String2.ISO_8859_1);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.ISO_8859_1));
            try {
                writer.write( 
                    "Core Version: "   + EDStatic.dapVersion    + OpendapHelper.EOL + //see EOL definition for comments
                    "Server Version: " + EDStatic.serverVersion + OpendapHelper.EOL +
                    "ERDDAP_version: " + EDStatic.erddapVersion + OpendapHelper.EOL); 

                //essential
                writer.flush();
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            } finally {
                writer.close(); 
            }

            return;
        }

        //respond to a help request  (see opendap spec section 7.2.6)
        //Note that lack of fileType (which opendap spec says should lead to help) 
        //  is handled elsewhere with error message and help 
        //  (which seems appropriate and mimics other dap servers)
        if (endOfRequestUrl.equals("help") ||
            endOfRequestUrl.startsWith("help.") ||                
            endOfRequestUrl.endsWith(".help")) {

            //write help response 
            //DAP 2.0 7.1.1 says help requests DON'T include content-description header.
            OutputStreamSource outputStreamSource = 
                new OutputStreamFromHttpResponse(request, response, 
                    "help", ".html", ".html");
            OutputStream out = outputStreamSource.outputStream(String2.ISO_8859_1);
            Writer writer = new BufferedWriter(new OutputStreamWriter(
                //DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
                out, String2.ISO_8859_1));
            try {
                writer.write(EDStatic.startHeadHtml(tErddapUrl, protocol + " Help"));
                writer.write("\n</head>\n");
                writer.write(EDStatic.startBodyHtml(loggedInAs));
                writer.write("\n");
                writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)));     
                writer.write("<div class=\"standard_width\">\n");
                writer.write(EDStatic.youAreHere(loggedInAs, protocol, "Help"));
                writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
                try {
                    if (protocol.equals("griddap")) 
                        EDDGrid.writeGeneralDapHtmlInstructions(tErddapUrl, writer, true); //true=complete
                    if (protocol.equals("tabledap")) 
                        EDDTable.writeGeneralDapHtmlInstructions(tErddapUrl, writer, true); //true=complete

                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(t));
                }
                writer.write("</div>\n");
            } catch (Exception e) {
                EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(e));
                throw e; 
            } finally {
                writer.write(EDStatic.endBodyHtml(tErddapUrl));
                writer.write("</html>");           
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                writer.close(); 
            }

            return;
        }

        //remove nextPath..., e.g., after first / in datasetID.fileType/nextPath/subDir
        String nextPath = ""; // "" is valid reference to baseUrl of .files
        int slashPoNP = hasDatasetID? endOfRequestUrl.indexOf('/') : -1;
        if (slashPoNP >= 0) {
            nextPath = endOfRequestUrl.substring(slashPoNP + 1); //no leading /
            endOfRequestUrl = endOfRequestUrl.substring(0, slashPoNP);

            //currently no nextPath options
            sendResourceNotFoundError(request, response, 
                "No options after '/' in request URL.");
            return;
        }
        //String2.log(">>nextPath=" + nextPath + " endOfRequestUrl=" + endOfRequestUrl);

        //get the datasetID and requested fileType
        int dotPo = endOfRequestUrl.lastIndexOf('.');
        if (dotPo < 0) {
            //no fileType
            if (endOfRequestUrl.equals(""))
                endOfRequestUrl = "index";
            sendRedirect(response, 
                tErddapUrl + "/" + protocol + "/" + endOfRequestUrl + ".html" +
                (endOfRequestUrl.equals("index")? 
                    "?" + EDStatic.passThroughPIppQueryPage1(request) : ""));  
            return;               
            //before 2012-01-19 was
            //throw new SimpleException("URL error: " +
            //    "No file type (e.g., .html) was specified after the datasetID.");
        }

        String id = endOfRequestUrl.substring(0, dotPo);
        fileTypeName = endOfRequestUrl.substring(dotPo);
        if (reallyVerbose) String2.log("  id=" + id + "\n  fileTypeName=" + fileTypeName);

        //respond to xxx/index request
        //show list of 'protocol'-supported datasets in .html file
        if (id.equals("index") && nextPath.length() == 0) {
            sendDatasetList(request, response, loggedInAs, protocol, fileTypeName);
            return;
        }

        //get the dataset
        EDD dataset = protocol.equals("griddap")?  gridDatasetHashMap.get(id) : 
                      protocol.equals("tabledap")? tableDatasetHashMap.get(id):
                      null;
        if (dataset == null) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unknownDatasetID, id));
            return;
        }
        if (!dataset.isAccessibleTo(EDStatic.getRoles(loggedInAs))) { //listPrivateDatasets doesn't apply
            //exception is graphsAccessibleTo=public
            if (dataset.graphsAccessibleToPublic()) {
                if (dataset.graphsAccessibleTo_fileTypeNamesContains(fileTypeName)) {
                    //fall through to get graphics
                } else {
                    EDStatic.sendHttpUnauthorizedError(loggedInAs, response, id, true);
                    return;
                }
            } else {
                EDStatic.sendHttpUnauthorizedError(loggedInAs, response, id, false);
                return;
            }
        }
        if (fileTypeName.equals(".graph") && dataset.accessibleViaMAG().length() > 0) {
            sendResourceNotFoundError(request, response, dataset.accessibleViaMAG());
            return;
        }
        if (fileTypeName.equals(".subset") && dataset.accessibleViaSubset().length() > 0) {
            sendResourceNotFoundError(request, response, dataset.accessibleViaSubset());
            return;
        }

        EDStatic.tally.add(protocol + " DatasetID (since startup)", id);
        EDStatic.tally.add(protocol + " DatasetID (since last daily report)", id);
        EDStatic.tally.add(protocol + " File Type (since startup)", fileTypeName);
        EDStatic.tally.add(protocol + " File Type (since last daily report)", fileTypeName);

        String fileName = dataset.suggestFileName(loggedInAs, userDapQuery, 
            //e.g., .ncHeader -> .nc, so same .nc file can be used for both responses
            fileTypeName.endsWith("Header")? fileTypeName.substring(0, fileTypeName.length() - 6) : fileTypeName);
        String extension = dataset.fileTypeExtension(fileTypeName); //e.g., .ncCF returns .nc
        if (reallyVerbose) String2.log("  fileName=" + fileName + "\n  extension=" + extension);
        if (fileTypeName.equals(".subset")) {
            String tValue = userDapQuery.length() == 0? 
                "initial request" : "subsequent request";
            EDStatic.tally.add(".subset (since startup)", tValue);
            EDStatic.tally.add(".subset (since last daily report)", tValue);
            EDStatic.tally.add(".subset DatasetID (since startup)", id);
            EDStatic.tally.add(".subset DatasetID (since last daily report)", id);
        }

        //jsonp
        String jsonp = null;
        if (fileTypeName.equals(".geoJson") ||
            fileTypeName.startsWith(".json") || //e.g., .json .jsonlCSV .jsonlKVP
            fileTypeName.equals(".ncoJson")) {  
            //did query include &.jsonp= ?
            String parts[] = Table.getDapQueryParts(userDapQuery); //decoded
            jsonp = String2.stringStartsWith(parts, ".jsonp="); //may be null
            if (jsonp != null) {
                jsonp = jsonp.substring(7);
                if (!fileTypeName.equals(".geoJson") && 
                    !fileTypeName.equals(".json") && //e.g., .jsonlCSV .jsonlKVP
                    !fileTypeName.equals(".ncoJson")) 
                    throw new SimpleException(EDStatic.errorJsonpNotAllowed);
                if (!String2.isJsonpNameSafe(jsonp))
                    throw new SimpleException(EDStatic.errorJsonpFunctionName);
            }
        }

        //if EDDGridFromErddap or EDDTableFromErddap, forward request
        //Note that .html and .graph are handled locally so links on web pages 
        //  are for this server and the responses can be handled quickly.
        if (dataset instanceof FromErddap) {
            FromErddap fromErddap = (FromErddap)dataset;
            int sourceVersion = fromErddap.intSourceErddapVersion();
            //some requests are handled locally...
            //more complicated test for new v184/200 orderBy features
            boolean newOrderBy = false;
            if (sourceVersion < 184 && userDapQuery.indexOf("orderBy") >= 0) {
                String parts[] = String2.splitNoTrim(userDapQuery, '&');
                for (int p = 1; p < parts.length; p++) { //1 because 0 is varList
                    if (userDapQuery.indexOf("orderByMean") >= 0 ||
                        (parts[p].startsWith("orderBy") && parts[p].indexOf('/') > 0)) {
                        newOrderBy = true;
                        break;
                    }
                }
            }
            if (newOrderBy ||
                fromErddap.redirect() == false ||
                fileTypeName.equals(".das") || 
                fileTypeName.equals(".dds") || 
                fileTypeName.equals(".html") || 
                fileTypeName.equals(".graph") ||
                fileTypeName.endsWith("ngInfo") ||  //pngInfo EDD.readPngInfo makes local file in all cases
                fileTypeName.endsWith("dfInfo") ||  //pdfInfo
                (sourceVersion < 124 && String2.indexOf(FILE_TYPES_124, fileTypeName) >= 0) ||
                (sourceVersion < 148 && String2.indexOf(FILE_TYPES_148, fileTypeName) >= 0) ||
                (sourceVersion < 174 && String2.indexOf(FILE_TYPES_174, fileTypeName) >= 0) ||
                (sourceVersion < 176 && String2.indexOf(FILE_TYPES_176, fileTypeName) >= 0) ||
                (sourceVersion < 182 && jsonp != null) ||
                (sourceVersion < 184 && String2.indexOf(FILE_TYPES_184, fileTypeName) >= 0) ||
                fileTypeName.equals(".subset")) { 
                //handle locally
            } else {
                //redirect the request
                String tUrl = fromErddap.getPublicSourceErddapUrl() + fileTypeName;
                String tqs = EDStatic.questionQuery(request.getQueryString());  //still encoded
                sendRedirect(response, tUrl + tqs);  
                return;
            }
        }

        String cacheDir = dataset.cacheDirectory(); //it is created by EDD.ensureValid
        OutputStreamSource outputStreamSource = new OutputStreamFromHttpResponse(
            request, response, fileName, 
            jsonp == null? fileTypeName : ".jsonp",  //.jsonp pseudo fileTypeName to get correct mime type
            extension);

        //*** tell the dataset to send the data
        try {
            //give the dataset the opportunity to update (DAP)
            dataset.update();

            //respond to the request
            dataset.respondToDapQuery(request, response,
                loggedInAs, requestUrl, userDapQuery, 
                outputStreamSource, 
                cacheDir, fileName, fileTypeName);            

        } catch (WaitThenTryAgainException wttae) {
            String2.log("!!ERDDAP caught WaitThenTryAgainException");

            //unload the dataset and set flag to reload it
            LoadDatasets.tryToUnload(this, id, new StringArray(), true); //needToUpdateLucene
            EDD.requestReloadASAP(id);
            //This is imperfect, but not bad. Worst case: dataset is unloaded
            //and reloaded 2+ times in quick succession when only once was needed.

            //is response committed?
            if (response.isCommitted()) {
                String2.log("but the response is already committed. So rethrowing the error.");
                throw wttae;
            }

            //wait up to 30 seconds for dataset to reload (tested below via dataset2!=dataset)
            //This also slows down the client (esp. if a script) and buys time for erddap.
            int waitSeconds = 30;
            for (int sec = 0; sec < waitSeconds; sec++) {
                //sleep for a second
                Math2.sleep(1000); 

                //has the dataset finished reloading?
                EDD dataset2 = protocol.equals("griddap")? 
                    gridDatasetHashMap.get(id) : 
                    tableDatasetHashMap.get(id);
                if (dataset2 != null && dataset != dataset2) { //yes, simplistic !=,  not !equals
                    //yes! ask dataset2 to respond to the query

                    try {
                        //note that this will fail if the previous response is already committed
                        dataset2.respondToDapQuery(request, response, loggedInAs,
                            requestUrl, userDapQuery, outputStreamSource, 
                            dataset2.cacheDirectory(), fileName, //dir is created by EDD.ensureValid
                            fileTypeName);
                        String2.log("!!ERDDAP successfully used dataset2 to respond to the request.");
                        break; //success! jump out of for(sec) loop
                    } catch (Throwable t) {
                        String2.log("!!!!ERDDAP caught Exception while handling WaitThenTryAgainException:\n" +
                            MustBe.throwableToString(t));
                        throw wttae; //throw original error
                    }
                }

                //if the dataset didn't reload after waitSeconds, throw the original error
                if (sec == waitSeconds - 1)
                    throw wttae;
            }
        }

        //2018-05-23 now this is to make doubly sure the outputStream is closed.
        try {
            OutputStream out = outputStreamSource.existingOutputStream();
            if (out != null) {
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                out.close();  //often already closed; closing again does nothing
            }
        } catch (Exception e2) {
            String2.log(MustBe.throwableToString(e2));
        } //essential, to end compression  //hard to put in finally {}
    }

    /**
     * Process a /files/ request for an accessibleViaFiles dataset.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    ("griddap" or "tabledap") in the requestUrl
     * @param userDapQuery  post "?".  Still percentEncoded.  May be "".  May not be null.
     */
    public void doFiles(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, int datasetIDStartsAt, String userDapQuery) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);       
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String fullRequestUrl = EDStatic.baseUrl(loggedInAs) + requestUrl;
        String roles[] = EDStatic.getRoles(loggedInAs);
        //String2.log(">>fullRequestUrl=" + fullRequestUrl);

        if (datasetIDStartsAt >= requestUrl.length()) {
            //dir of accessibleViaFiles datasets
            if (!fullRequestUrl.endsWith("/")) { //required for table.directoryListing below
                sendRedirect(response, fullRequestUrl + "/");  
                return;               
            }

            StringArray subDirNames = new StringArray();
            StringArray subDirDes = new StringArray();
            StringArray ids = gridDatasetIDs();
            int nids = ids.size();
            for (int ti = 0; ti < nids; ti++) {
                EDD edd = gridDatasetHashMap.get(ids.get(ti));
                if (edd != null && //in case just deleted
                    edd.accessibleViaFilesDir().length() > 0 &&
                    edd.isAccessibleTo(roles)) { // /files/, so graphsAccessibleToPublic is irrelevant
                    subDirNames.add(edd.datasetID());
                    subDirDes.add(edd.title());
                }
            }
            ids = tableDatasetIDs();
            nids = ids.size();
            for (int ti = 0; ti < nids; ti++) {
                EDD edd = tableDatasetHashMap.get(ids.get(ti));
                if (edd != null && //in case just deleted
                    edd.accessibleViaFilesDir().length() > 0 &&
                    edd.isAccessibleTo(roles)) { // /files/, so graphsAccessibleToPublic is irrelevant
                    subDirNames.add(edd.datasetID());
                    subDirDes.add(edd.title());
                }
            }
            //make columns: "Name" (String), "Last modified" (long), 
            //  "Size" (long), and "Description" (String)
            Table table = new Table();
            table.addColumn("Name",          new StringArray(new String[]{"documentation.html"}));
            table.addColumn("Last modified", new LongArray(new long[]{EDStatic.startupMillis}));
            table.addColumn("Size",          new LongArray(new long[]{Long.MAX_VALUE}));            
            table.addColumn("Description",   new StringArray(new String[]{"Documentation for ERDDAP's \"files\" system."}));
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, "Browse Source Files", out);
            try {
                writer.write(
                    "<div class=\"standard_width\">\n" +
                    EDStatic.youAreHere(loggedInAs, "files") + 
                    EDStatic.filesDescription + 
                    "\n<br><span class=\"warningColor\">" + EDStatic.warning + "</span> " +
                    EDStatic.filesWarning + "\n" +
                    "(<a rel=\"help\" href=\"" + tErddapUrl + "/files/documentation.html\">" + 
                        MessageFormat.format(EDStatic.indexDocumentation, "\"files\"") + "</a>" +
                    ", including <a rel=\"help\" href=\"" + tErddapUrl + 
                      "/files/documentation.html#HowCanIWorkWithTheseFiles\">\"How can I work with these files?\"</a>)\n" +
                    "<br>&nbsp;\n");
                writer.flush();
                writer.write(
                    table.directoryListing(
                        null, fullRequestUrl, userDapQuery, //may have sort instructions
                        EDStatic.imageDirUrl(loggedInAs) + "fileIcons/",
                        EDStatic.imageDirUrl(loggedInAs) + EDStatic.questionMarkImageFile,
                        true, subDirNames, subDirDes)); //addParentDir
            } catch (Exception e) {
                EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(e));
                throw e; 
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }

            //tally
            EDStatic.tally.add("files browse DatasetID (since startup)", "");
            EDStatic.tally.add("files browse DatasetID (since last daily report)", "");
            return;
        }

        //get the datasetID          percentDecode because there can be spaces (%20) in dir and file names
        String endOfRequestUrl = SSR.percentDecode(requestUrl.substring(datasetIDStartsAt));
        //remove nextPath, e.g., after first / in datasetID/someDir/someSubDir
        String id = endOfRequestUrl;
        String nextPath = ""; 
        int slashPoNP = endOfRequestUrl.indexOf('/');
        if (slashPoNP >= 0) {
            id = endOfRequestUrl.substring(0, slashPoNP);
            nextPath = endOfRequestUrl.substring(slashPoNP + 1); //no leading /

            //nextPath should already have only forward / 
            nextPath = String2.replaceAll(nextPath, '\\', '/');

            //beware malformed nextPath, e.g., internal /../
            if (File2.addSlash("/" + nextPath + "/").indexOf("/../") >= 0) 
                throw new SimpleException(EDStatic.queryError + "/../ not allowed!");
        } else {
            //no slash
            //is it documentation.html?
            if (id.equals("documentation.html")) {
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, "ERDDAP \"files\" Documentation", out);
                try {
                    writer.write("<div class=\"standard_width\">\n");
                    writer.write(EDStatic.youAreHere(loggedInAs, "files", "Documentation"));
                    writer.write(EDStatic.filesDocumentation(tErddapUrl));
                } catch (Exception e) {
                    EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(e));
                    throw e; 
                } finally {
                    writer.write("\n" +
                        "</div>\n");
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }
                return;

            } else {
                //presumably it is a datasetID without trailing slash, so add it and redirect
                if (!fullRequestUrl.endsWith("/")) { //required for table.directoryListing below
                    sendRedirect(response, fullRequestUrl + "/");  
                    return;               
                }
            }
        }
        //String2.log(">>nextPath=" + nextPath + " endOfRequestUrl=" + endOfRequestUrl);

        //get the dataset
        EDD edd = gridDatasetHashMap.get(id);
        if (edd == null)
            edd = tableDatasetHashMap.get(id);
        if (edd == null) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unknownDatasetID, id));
            return;
        }
        String fileDir    = edd.accessibleViaFilesDir();
        String fileRegex  = edd.accessibleViaFilesRegex();
        boolean recursive = edd.accessibleViaFilesRecursive();

        if (fileDir.length() == 0) {
            if (verbose) String2.log(EDStatic.resourceNotFound + " accessibleViaFilesDir=\"\"");
            sendResourceNotFoundError(request, response, "This dataset is not accessible via /files/ .");
            return;
        }
        if (!edd.isAccessibleTo(roles)) { 
            // /files/ access: all requests are data requests
            //listPrivateDatasets and graphsAccessibleToPublic don't apply
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, id, 
                edd.graphsAccessibleToPublic());
            return;
        }

        //if nextPath has a subdir, ensure dataset is recursive
        if (nextPath.indexOf('/') >= 0 && !recursive) {
            sendResourceNotFoundError(request, response, "The subdirectory doesn't exist.");
            return;
        }

        //catch pseudo filename that is just an extension 
        //  (hence RESTful request for filenames)
        String localFullName = fileDir + nextPath;
        String justExtension = File2.getNameAndExtension(localFullName);
        int tWhich = String2.indexOf(plainFileTypes, justExtension);
        if (tWhich >= 0) {
            //The "fileName" is just one of the plainFileType extensions, e.g., .csv.
            //Remove justExtension from localFullName and nextPath.
            localFullName = localFullName.substring(0, localFullName.length() - justExtension.length());
            nextPath = nextPath.substring(0, nextPath.length() - justExtension.length());
            //request will be handled below

        } else {
            justExtension = "";
        }

        //get the accessibleViaFilesFileTable
        //Formatted like 
        //FileVisitorDNLS.oneStep(tDirectoriesToo=false, last_mod is LongArray,
        //and size is LongArray of epochMillis)
        //with valid files (or null if unavailable or any trouble).
        //This is a copy of any internal data, so contents can be modified.
        Table fileTable = edd.accessibleViaFilesFileTable();
        if (fileTable == null) {
            sendResourceNotFoundError(request, response, 
                "File info for this dataset is currently unavailable.");
            return;
        }
        int fileTableRow;
        int fileTableNRows = fileTable.nRows();
        StringArray dirSA  = (StringArray)fileTable.getColumn(0);
        StringArray nameSA = (StringArray)fileTable.getColumn(1);

        String localDir = File2.getDirectory(localFullName); //where I access source
        String webDir = File2.getDirectory(fullRequestUrl);  //what user as apparent location
        String nameAndExt = File2.getNameAndExtension(localFullName);
        String ext = File2.getExtension(nameAndExt);

        //is it a file in the fileTable?
        if (nameAndExt.length() > 0) {
            fileTableRow = 0;
            while (fileTableRow < fileTableNRows && 
                (!localDir.equals(   dirSA.get(fileTableRow)) || 
                 !nameAndExt.equals(nameSA.get(fileTableRow))))
                fileTableRow++;
            if (fileTableRow < fileTableNRows) {
                OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                    request, response, File2.getNameNoExtension(nameAndExt), ext, ext); 
                OutputStream outputStream = 
                    outSource.outputStream("", File2.length(localFullName));
                doTransfer(request, response, localDir, webDir, nameAndExt, 
                    outputStream, outSource.usingCompression());

                //tally
                EDStatic.tally.add("files download DatasetID (since startup)", id);
                EDStatic.tally.add("files download DatasetID (since last daily report)", id);
                return;
            }
        }

        //reduce fileTable to just files in that dir
        boolean addedSlash = false;
        if (nameAndExt.length() > 0) {
            //perhaps user forgot trailing slash
            localDir += nameAndExt + "/";
            webDir += nameAndExt + "/";
            nameAndExt = "";
            addedSlash = true;
        }
        int localDirLength = localDir.length();
        HashSet subdirHash = new HashSet(); //catch all subdirectories
        BitSet keep = new BitSet(fileTableNRows);  //all false
        for (int row = 0; row < fileTableNRows; row++) {
            String tDir = dirSA.get(row);
            if (tDir.startsWith(localDir)) {
                if (tDir.length() == localDirLength) {
                    keep.set(row);
                } else {
                    //add next-level directory name
                    subdirHash.add(tDir.substring(localDirLength, tDir.indexOf('/', localDirLength))); 
                }
            }
        }
        fileTable.removeColumn(0); //directory
        fileTable.justKeep(keep);
        fileTableNRows = fileTable.nRows();
        //yes, there's a match
        if (addedSlash) {
            sendRedirect(response, fullRequestUrl + "/");  
            return;               
        }

        //handle justExtension request  e.g., datasetID/.csv[?constraintExpression]
        /*if (justExtension.length() > 0) {
            //tally it

            //make a EDDTableFromAccessibleViaFiles

            //tell it to handle the request

            //make the list of file info
            Table table = FileVisitorDNLS.oneStep(localFullName, fileRegex, 
                edd.accessibleViaFilesRecursive(), true); //dirsToo
            Test.ensureEqual(table.getColumnNamesCSVString(), 
                "directory,name,lastModified,size", 
                "Unexpected columnNames");
            //apply constraints

            //return results as justExtension fileType


            return;
        }*/

        //show directory index
        //make columns: "Name" (String), "Last modified" (long), 
        //  "Size" (long), and "Description" (String)        
        StringArray subDirs = new StringArray((String[])(subdirHash.toArray(new String[0])));
        fileTable.setColumnName(0, "Name");
        fileTable.setColumnName(1, "Last modified");
        fileTable.setColumnName(2, "Size");            
        fileTable.addColumn("Description", new StringArray(fileTableNRows, true));
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "files/" + id + "/" + nextPath, out);
        try {
            writer.write("<div class=\"standard_width\">\n");
            writer.write(
                nextPath.length() == 0? EDStatic.youAreHere(loggedInAs, "files", id) :
                "\n<h1>" + EDStatic.erddapHref(tErddapUrl) +
                "\n &gt; <a rel=\"contents\" href=\"" + 
                    XML.encodeAsHTMLAttribute(EDStatic.protocolUrl(tErddapUrl, "files")) +
                    "\">files</a>" +
                "\n &gt; <a rel=\"contents\" href=\"" + 
                    XML.encodeAsHTMLAttribute(
                        EDStatic.erddapUrl(loggedInAs) + "/files/" + id + "/") + 
                    "\">" + id + "</a>" +  
                "\n &gt; " + XML.encodeAsXML(nextPath) + 
                "</h1>\n");
            writer.write(EDStatic.filesDescription + "\n");
            if (!(edd instanceof EDDTableFromFileNames))
                writer.write(
                    "<br><span class=\"warningColor\">" + EDStatic.warning + "</span> " + 
                    EDStatic.filesWarning + "\n");
            writer.write(" (<a rel=\"help\" href=\"" + tErddapUrl + "/files/documentation.html\">" + 
                MessageFormat.format(EDStatic.indexDocumentation, "\"files\"") + "</a>" +
                ", including <a rel=\"help\" href=\"" + tErddapUrl + 
                  "/files/documentation.html#HowCanIWorkWithTheseFiles\">\"How can I work with these files?\"</a>)\n" +
                "<br>&nbsp;\n");
            edd.writeHtmlDatasetInfo(loggedInAs, writer, true, true, false, true, "", "");
            writer.write("<br>");  //causes nice spacing between datasetInfo and file table
            writer.flush();
            writer.write(
                fileTable.directoryListing(
                    localDir, fullRequestUrl, userDapQuery, //may have sort instructions
                    EDStatic.imageDirUrl(loggedInAs) + "fileIcons/",
                    EDStatic.imageDirUrl(loggedInAs) + EDStatic.questionMarkImageFile,
                    true, subDirs, null));  //addParentDir                
        } catch (Exception e) {
            EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(e));
            throw e; 
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }

        //tally
        EDStatic.tally.add("files browse DatasetID (since startup)", id);
        EDStatic.tally.add("files browse DatasetID (since last daily report)", id);

    }


    /**
     * This sends the list of griddap, tabledap, sos, wcs, or wms datasets
     *
     * @param request
     * @param response
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param protocol   must be "griddap", "tabledap", "sos", "wcs", or "wms"
     * @param fileTypeName e.g., .html or .json
     * throws Throwable if trouble
     */
    public void sendDatasetList(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String protocol, String fileTypeName) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl(), pre "?"

        //ensure valid fileTypeName
        int pft = String2.indexOf(plainFileTypes, fileTypeName);
        if (pft < 0 && !fileTypeName.equals(".html")) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unsupportedFileType, fileTypeName));
            return;
        }

        //ensure query has simplistically valid page= itemsPerPage=
        if (!Arrays.equals(
                EDStatic.getRawRequestedPIpp(request),
                EDStatic.getRequestedPIpp(request))) {
            sendRedirect(response, 
                EDStatic.baseUrl(loggedInAs) + requestUrl + "?" +
                EDStatic.passThroughJsonpQuery(request) +
                EDStatic.passThroughPIppQuery(request));  //should be nothing else in query
            return;
        }      

        //gather the datasetIDs and descriptions
        String roles[] = EDStatic.getRoles(loggedInAs);
        StringArray ids;
        StringArray titles;
        String description;  
        if      (protocol.equals("griddap")) {
            StringArray tids = gridDatasetIDs();
            int ntids = tids.size();
            titles = new StringArray(ntids, false);
            ids    = new StringArray(ntids, false);
            for (int ti = 0; ti < ntids; ti++) {
                EDD edd = gridDatasetHashMap.get(tids.get(ti));
                if (edd != null && //if just deleted
                    (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles)) ||
                        edd.graphsAccessibleToPublic()) { //griddap requests may be graphics requests {
                    titles.add(edd.title());
                    ids.add(   edd.datasetID());
                }
            }
            description = EDStatic.EDDGridDapDescription;
        } else if (protocol.equals("tabledap")) {
            StringArray tids = tableDatasetIDs();
            int ntids = tids.size();
            titles = new StringArray(ntids, false);
            ids    = new StringArray(ntids, false);
            for (int ti = 0; ti < ntids; ti++) {
                EDD edd = tableDatasetHashMap.get(tids.get(ti));
                if (edd != null && //if just deleted
                    (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles)) ||
                        edd.graphsAccessibleToPublic()) { //tabledap requests may be graphics requests 
                    titles.add(edd.title());
                    ids.add(   edd.datasetID());
                }
            }
            description = EDStatic.EDDTableDapDescription;
        } else if (EDStatic.sosActive && protocol.equals("sos")) {
            StringArray tids = tableDatasetIDs();
            int ntids = tids.size();
            titles = new StringArray(ntids, false);
            ids    = new StringArray(ntids, false);
            for (int ti = 0; ti < ntids; ti++) {
                EDD edd = tableDatasetHashMap.get(tids.get(ti));
                if (edd != null && //if just deleted
                    edd.accessibleViaSOS().length() == 0 &&
                    (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles))) {
                    //no edd.graphsAccessibleToPublic() since sos requests are all data requests
                    titles.add(edd.title());
                    ids.add(   edd.datasetID());
                }
            }
            description = EDStatic.sosDescriptionHtml +
               "\nFor details, see the 'S'OS links below.";
        } else if (EDStatic.wcsActive && protocol.equals("wcs")) {
            StringArray tids = gridDatasetIDs();
            int ntids = tids.size();
            titles = new StringArray(ntids, false);
            ids    = new StringArray(ntids, false);
            for (int ti = 0; ti < ntids; ti++) {
                EDD edd = gridDatasetHashMap.get(tids.get(ti));
                if (edd != null && //if just deleted
                    edd.accessibleViaWCS().length() == 0 &&
                    (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles))) {
                    //no edd.graphsAccessibleToPublic() since wcs requests are all data requests
                    titles.add(edd.title());
                    ids.add(   edd.datasetID());
                }
            }
            description = EDStatic.wcsDescriptionHtml;
        } else if (EDStatic.wmsActive && protocol.equals("wms")) {
            StringArray tids = gridDatasetIDs();
            int ntids = tids.size();
            titles = new StringArray(ntids, false);
            ids    = new StringArray(ntids, false);
            for (int ti = 0; ti < ntids; ti++) {
                EDD edd = gridDatasetHashMap.get(tids.get(ti));
                if (edd != null && //if just deleted
                    edd.accessibleViaWMS().length() == 0 &&
                    (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles) ||
                        edd.graphsAccessibleToPublic())) { //all wms requests are graphics requests
                    titles.add(edd.title());
                    ids.add(   edd.datasetID());
                }
            }
            description = EDStatic.wmsDescriptionHtml;
        } else {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unknownProtocol, protocol));
            return;
        }

        //sortByTitle
        Table table = new Table();
        table.addColumn("title", titles); 
        table.addColumn("id",    ids);
        table.leftToRightSortIgnoreCase(2);
        titles = null;
        table = null;

        //calculate Page ItemsPerPage  (part of: list by protocol)
        int nMatches = ids.size();
        int pIpp[] = EDStatic.calculatePIpp(request, nMatches);
        int page         = pIpp[0]; //will be 1... 
        int itemsPerPage = pIpp[1]; //will be 1...
        int startIndex   = pIpp[2]; //will be 0...
        int lastPage     = pIpp[3]; //will be 1...

        //reduce datasetIDs to ones on requested page
        //IMPORTANT!!! For this to work correctly, datasetIDs must be
        //  accessibleTo loggedInAs (or EDStatic.listPrivateDatasets)
        //  and in final sorted order.   
        //  (True here)
        //Order of removal: more efficient to remove items at end, then items at beginning.
        if (startIndex + itemsPerPage < nMatches) 
            ids.removeRange(startIndex + itemsPerPage, nMatches);
        ids.removeRange(0, Math.min(startIndex, nMatches));

        //if non-null, error will be String[2]
        String error[] = null;
        if (nMatches == 0) {
            error = new String[] {
                MessageFormat.format(EDStatic.noDatasetWith, "protocol=\"" + protocol + "\""),
                ""};
        } else if (page > lastPage) {
            error = EDStatic.noPage(page, lastPage);
        }
        
        //clean up description
        String uProtocol = protocol.equals("sos") || protocol.equals("wcs") || 
                           protocol.equals("wms")? 
            protocol.toUpperCase() : protocol;
        //description = String2.replaceAll(description, '\n', ' '); //remove inherent breaks
        //description =
        //    String2.noLongLinesAtSpace(description, 90, "<br>");

        //you can't use noLongLinesAtSpace for fear of  "<a <br>href..."
        if (protocol.equals("tabledap"))
            description += "\n" + 
                MessageFormat.format(EDStatic.tabledapVideoIntro, tErddapUrl) +
                "\n";
        if (!protocol.equals("sos"))
            description += "\n" + 
                MessageFormat.format(EDStatic.seeProtocolDocumentation,
                    tErddapUrl, protocol, uProtocol) +
                "\n";

        //handle plainFileTypes   
        boolean sortByTitle = false;  //sorted above
        if (pft >= 0) {
            if (error != null) 
                throw new SimpleException(error[0] + " " + error[1]);

            //make the plain table with the dataset list
            table = makePlainDatasetTable(loggedInAs, ids, sortByTitle, fileTypeName);  
            sendPlainTable(loggedInAs, request, response, table, protocol, fileTypeName);
            return;
        }


        //make the html table with the dataset list
        table = makeHtmlDatasetTable(loggedInAs, ids, sortByTitle);  

        //display start of web page
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, 
            MessageFormat.format(EDStatic.listOfDatasets, uProtocol),
            out); 
        try {
            String refine = EDStatic.orRefineSearchWith + 
                getAdvancedSearchLink(loggedInAs, 
                    EDStatic.passThroughPIppQueryPage1(request) + 
                    "&protocol=" + uProtocol);
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, uProtocol) +
                description);

                /*getYouAreHereTable(
                    EDStatic.youAreHere(loggedInAs, uProtocol) +
                    description +
                    "<h2>" + 
                        MessageFormat.format(EDStatic.listOfDatasets, uProtocol) + 
                        "</h2>\n",
                    //Or, View All Datasets
                    "&nbsp;\n" +
                    "<br>" + getSearchFormHtml(request, loggedInAs, EDStatic.orComma, ":\n<br>", "") +
                    "<br>" + getCategoryLinksHtml(request, tErddapUrl) +
                    "<br>&nbsp;\n" +
                    "<br>" + refine);
                */

            if (error == null) {
                String nMatchingHtml = EDStatic.nMatchingDatasetsHtml(
                    nMatches, page, lastPage, false, //=alphabetical
                    EDStatic.baseUrl(loggedInAs) + requestUrl + 
                    EDStatic.questionQuery(request.getQueryString()));

                writer.write(
                    "<p>" + nMatchingHtml + "\n" +
                    "<span class=\"N\">(" + refine + ")</span>\n");

                table.saveAsHtmlTable(writer, "commonBGColor nowrap", null, 1, false, -1, false, 
                    false); //allowWrap

                if (lastPage > 1)
                    writer.write("\n<p>" + nMatchingHtml);

                //list plain file types
                writer.write(
                    "\n" +
                    "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                    plainFileTypesString + //not links, which would be indexed by search engines
                    ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                        EDStatic.restfulViaService + "</a>.\n");
            } else {
                writer.write("<p><span class=\"warningColor\">" + 
                    XML.encodeAsHTML(error[0] + " " + error[1]) + "</span>\n");
            } 

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }


    /**
     * Process a SOS request.
     * This SOS service is intended to simulate the 
     * 52N SOS server (the OGC reference implementation) 
     *   http://sensorweb.demo.52north.org/52nSOSv3.2.1/sos 
     * and the IOOS DIF SOS services (datasetID=ndbcSOS...).
     *   e.g., ndbcSosWind https://sdf.ndbc.noaa.gov/sos/ .
     * For IOOS DIF schemas, see https://ioos.github.io/sos-dif/dif/welcome.html .
     * O&amp;M document(?) says that query names are case insensitive, but query values are case sensitive.
     * Background info: https://www.opengeospatial.org/standards/sos
     *
     * <p>This assumes request was for /erddap/sos.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    ("sos") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     *   This has name=value pairs. The name is case-insensitive. The value is case-sensitive.
     *   This must include service="SOS", request=[aValidValue like GetCapabilities].
     */
    public void doSos(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, int datasetIDStartsAt, String userQuery) throws Throwable {

        if (!EDStatic.sosActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "SOS"));
        }
/*
This isn't finished!   Reference server (ndbcSOS) is in flux and ...
Interesting IOOS DIF info c:/programs/sos/EncodingIOOSv0.6.0Observations.doc
Spec questions? Ask Jeff DLb (author of WMS spec!): Jeff.deLaBeaujardiere@noaa.gov 
*/

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);

        //catch other responses outside of try/catch  (so errors handled in doGet)
        if (endOfRequestUrl.equals("") || endOfRequestUrl.equals("index.htm")) {
            sendRedirect(response, tErddapUrl + "/sos/index.html?" + 
                EDStatic.passThroughPIppQueryPage1(request));
            return;
        }

        //list the SOS datasets
        if (endOfRequestUrl.startsWith("index.")) {
            sendDatasetList(request, response, loggedInAs, "sos", endOfRequestUrl.substring(5)); 
            return;
        }        

        //SOS documentation web page
        if (endOfRequestUrl.equals("documentation.html")) {
            doSosDocumentation(request, response, loggedInAs);
            return;
        }       

        //request should be e.g., /sos/cwwcNdbc/[EDDTable.sosServer]?service=SOS&request=GetCapabilities
        String urlEndParts[] = String2.split(endOfRequestUrl, '/');
        String tDatasetID = urlEndParts.length > 0? urlEndParts[0] : "";
        String part1 = urlEndParts.length > 1? urlEndParts[1] : "";
        EDDTable eddTable = tableDatasetHashMap.get(tDatasetID);
        if (eddTable == null) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unknownDatasetID, tDatasetID));
            return;
        }

        //give the dataset the opportunity to update (SOS)
        try {
            eddTable.update();
        } catch (WaitThenTryAgainException e) {
            //unload the dataset and set flag to reload it
            LoadDatasets.tryToUnload(this, tDatasetID, new StringArray(), true); //needToUpdateLucene
            EDD.requestReloadASAP(tDatasetID);
            throw e;
        }

        //check loggedInAs
        String roles[] = EDStatic.getRoles(loggedInAs);
        if (!eddTable.isAccessibleTo(roles)) { 
            //SOS access: all requests are data requests
            //listPrivateDatasets and graphAccessibleToPublic don't apply
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID, 
                eddTable.graphsAccessibleToPublic());
            return;
        }

        //check accessibleViaSOS
        if (eddTable.accessibleViaSOS().length() > 0) {
            sendResourceNotFoundError(request, response, eddTable.accessibleViaSOS());
            return;
        }

        //write /sos/[datasetID]/index.html
        if (part1.equals("index.html") && urlEndParts.length == 2) {
//tally other things?
            EDStatic.tally.add("SOS index.html (since last daily report)", tDatasetID);
            EDStatic.tally.add("SOS index.html (since startup)", tDatasetID);
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, XML.encodeAsHTML(eddTable.title()) + " - SOS", out);
            try {
                writer.write("<div class=\"standard_width\">\n");
                eddTable.sosDatasetHtml(loggedInAs, writer);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t; 
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        //write /sos/[datasetID]/phenomenaDictionary.xml
        if (part1.equals(EDDTable.sosPhenomenaDictionaryUrl) && urlEndParts.length == 2) {
            OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                request, response, "sos_" + eddTable.datasetID() + "_phenomenaDictionary", ".xml", ".xml");
            OutputStream out = outSource.outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                eddTable.sosPhenomenaDictionary(writer);
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            } finally {
                writer.close(); 
            }
            return;
        }


        //ensure it is a SOS server request
        if (!part1.equals(EDDTable.sosServer) && urlEndParts.length == 2) {
            sendResourceNotFoundError(request, response, "not a SOS request");
            return;
        }

        //No! Don't redirect! datasetID may be different so station and observedProperty names
        //  will be different.
        //if eddTable instanceof EDDTableFromErddap, redirect the request
        /*if (eddTable instanceof EDDTableFromErddap && userQuery != null) {
            EDDTableFromErddap fromErddap = (EDDTableFromErddap)eddTable;
            if (fromErddap.redirect()) {
                //https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle
                String tUrl = fromErddap.getNextLocalSourceErddapUrl();
                tUrl = String2.replaceAll(tUrl, "/tabledap/", "/sos/") + "/" + EDDTable.sosServer + 
                    "?" + userQuery;
                if (verbose) String2.log("redirected to " + tUrl);
                sendRedirect(response, tUrl);  
                return;
            }
        }*/

        //note that isAccessibleTo(loggedInAs) and accessibleViaSOS are checked above
        try {

            //parse SOS service userQuery  
            HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, 
                true); //true=names toLowerCase

            //if service= is present, it must be service=SOS     //technically, it is required
            String tService = queryMap.get("service"); 
            if (tService != null && !tService.equals("SOS")) 
                //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                throw new SimpleException(EDStatic.queryError + "service='" + tService + "' must be 'SOS'."); 

            //deal with the various request= options
            String tRequest = queryMap.get("request");
            if (tRequest == null)
                tRequest = "";

            if (tRequest.equals("GetCapabilities")) {
                //e.g., ?service=SOS&request=GetCapabilities
                OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                    request, response, "sos_" + eddTable.datasetID() + "_capabilities", ".xml", ".xml");
                OutputStream out = outSource.outputStream(String2.UTF_8);
                Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
                try {
                    eddTable.sosGetCapabilities(queryMap, writer, loggedInAs); 
                    writer.flush();
                    if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                } finally {
                    writer.close(); 
                }
                return;

            } else if (tRequest.equals("DescribeSensor")) {
                //The url might be something like
                //https://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor&service=SOS
                //  &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
                //  &procedure=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0

                //version is not required. If present, it must be valid.
                String version = queryMap.get("version");  //map keys are lowercase
                if (version == null || !version.equals(EDDTable.sosVersion))
                    //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                    throw new SimpleException(EDStatic.queryError + "version='" + version + 
                        "' must be '" + EDDTable.sosVersion + "'."); 

                //outputFormat is not required. If present, it must be valid.
                //not different name and values than GetObservation responseFormat
                String outputFormat = queryMap.get("outputformat");  //map keys are lowercase
                if (outputFormat == null || !outputFormat.equals(EDDTable.sosDSOutputFormat))
                    //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                    throw new SimpleException(EDStatic.queryError + "outputFormat='" + outputFormat + 
                        "' must be '" + SSR.minimalPercentEncode(EDDTable.sosDSOutputFormat) + "'."); 

                //procedure=fullSensorID is required   (in getCapabilities, procedures are sensors)
                String procedure = queryMap.get("procedure");  //map keys are lowercase
                if (procedure == null)
                //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                    throw new SimpleException(EDStatic.queryError + "procedure=''.  Please specify a procedure."); 
                String sensorGmlNameStart = eddTable.getSosGmlNameStart("sensor");
                String shortName = procedure.startsWith(sensorGmlNameStart)?
                    procedure.substring(sensorGmlNameStart.length()) : procedure;
                //int cpo = platform.indexOf(":");  //now platform  or platform:sensor
                //String sensor = "";
                //if (cpo >= 0) {
                //    sensor = platform.substring(cpo + 1);
                //    platform = platform.substring(0, cpo);
                //}         
                if (!shortName.equals(eddTable.datasetID()) &&    //all
                    eddTable.sosOfferings.indexOf(shortName) < 0) //1 station
                    //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                    throw new SimpleException(EDStatic.queryError + "procedure=" + procedure +
                        " isn't a valid long or short sensor name."); 
                //if ((!sensor.equals(eddTable.datasetID()) &&    //all
                //     String2.indexOf(eddTable.dataVariableDestinationNames(), sensor) < 0) || //1 variable
                //        sensor.equals(EDV.LON_NAME) ||
                //        sensor.equals(EDV.LAT_NAME) ||
                //        sensor.equals(EDV.ALT_NAME) ||
                //        sensor.equals(EDV.TIME_NAME) ||
                //        sensor.equals(eddTable.dataVariableDestinationNames()[eddTable.sosOfferingIndex])) 
                //    this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                //    throw new SimpleException(EDStatic.queryError + "procedure=" + procedure + " isn't valid because \"" +
                //        sensor + "\" isn't valid sensor name."); 

                //all is well. do it.
                String fileName = "sosSensor_" + eddTable.datasetID() + "_" + shortName;
                OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                    request, response, fileName, ".xml", ".xml");
                OutputStream out = outSource.outputStream(String2.UTF_8);
                Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
                try {
                    eddTable.sosDescribeSensor(loggedInAs, shortName, writer);
                    writer.flush();
                    if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                } finally {
                    writer.close(); 
                } 
                return;

            } else if (tRequest.equals("GetObservation")) {
                String responseFormat = queryMap.get("responseformat");  //map keys are lowercase
                String fileTypeName = EDDTable.sosResponseFormatToFileTypeName(responseFormat);
                if (fileTypeName == null)
                    //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                    throw new SimpleException(EDStatic.queryError + 
                        "responseFormat=" + responseFormat + " is invalid."); 

                String responseMode = queryMap.get("responsemode");  //map keys are lowercase
                if (responseMode == null)
                    responseMode = "inline";
                String extension = null;
                if (EDDTable.isIoosSosXmlResponseFormat(responseFormat) || //throws exception if invalid format
                    EDDTable.isOostethysSosXmlResponseFormat(responseFormat) ||
                    responseMode.equals("out-of-band")) { //xml response with link to tabledap

                    extension = ".xml";
                } else {
                    int po = String2.indexOf(EDDTable.dataFileTypeNames, fileTypeName);
                    if (po >= 0) 
                        extension = EDDTable.dataFileTypeExtensions[po];
                    else {
                        po = String2.indexOf(EDDTable.imageFileTypeNames, fileTypeName);
                        extension = EDDTable.imageFileTypeExtensions[po];
                    }
                }
            
                String dir = eddTable.cacheDirectory();
                String fileName = "sos_" + eddTable.suggestFileName(loggedInAs, userQuery, responseFormat);
                OutputStreamSource oss = 
                    new OutputStreamFromHttpResponse(request, response, 
                        fileName, fileTypeName, extension);
                eddTable.sosGetObservation(userQuery, loggedInAs, oss, dir, fileName); //it calls out.close()
                return;

            } else {
                //this format EDStatic.queryError + "xxx=" is parsed by Erddap section "deal with SOS error"
                throw new SimpleException(EDStatic.queryError + "request=" + tRequest + " is not supported."); 
            }

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            //deal with SOS error

            //catch errors after the response has begun
            if (response.isCommitted()) 
                throw t; //rethrown exception (will be handled in doGet try/catch)

            OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                request, response, "ExceptionReport", //fileName is not used
                ".xml", ".xml");
            OutputStream out = outSource.outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                //for now, mimic oostethys  (ndbcSOS often doesn't throw exceptions)
                //exceptionCode options are from OGC 06-121r3  section 8
                //  the locator is the name of the relevant request parameter
    //* OperationNotSupported  Request is for an operation that is not supported by this server
    //* MissingParameterValue  Operation request does not include a parameter value, and this server did not declare a default value for that parameter
    //* InvalidParameterValue  Operation request contains an invalid parameter value a
    //* VersionNegotiationFailed  List of versions in "AcceptVersions" parameter value in GetCapabilities operation request did not include any version supported by this server
    //* InvalidUpdateSequence  Value of (optional) updateSequence parameter in GetCapabilities operation request is greater than current value of service metadata updateSequence number
    //* OptionNotSupported  Request is for an option that is not supported by this server
    //* NoApplicableCode   No other exceptionCode specified by this service and server applies to this exception
                String error = MustBe.getShortErrorMessage(t);
                String exCode = "NoApplicableCode";  //default
                String locator = null;               //default

                //catch InvalidParameterValue 
                //Look for EDStatic.queryError + "xxx="
                String qe = EDStatic.queryError;
                int qepo = error.indexOf(qe);
                int epo = error.indexOf('=');
                if (qepo >= 0 && epo > qepo && epo - qepo < 17 + 20) {
                    exCode  = "InvalidParameterValue";
                    locator = error.substring(qepo + qe.length(), epo);
                } 

                writer.write(
                    "<?xml version=\"1.0\"?>\n" +
                    "<ExceptionReport xmlns=\"http://www.opengis.net/ows\" \n" +
                    "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n" +
                    "  xsi:schemaLocation=\"http://www.opengis.net/ows http://schemas.opengis.net/ows/1.0.0/owsExceptionReport.xsd\" \n" +
                    "  version=\"1.0.0\" language=\"en\">\n" +
                    "  <Exception exceptionCode=\"" + exCode + "\" " +
                        (locator == null? "" : "locator=\"" + locator + "\" ") +
                        ">\n" +
                    "    <ExceptionText>" + XML.encodeAsHTML(error) + "</ExceptionText>\n" +
                    "  </Exception>\n" +
                    "</ExceptionReport>\n");
                String2.log(String2.ERROR + " message sent to user: " + error);
                String2.log(MustBe.throwableToString(t));

                //essential
                writer.flush();
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            } finally {
                writer.close(); 
            }

        }
    }


    /**
     * This responds by sending out ERDDAP's "SOS Documentation" Html page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doSosDocumentation(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs) throws Throwable {

        if (!EDStatic.sosActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "SOS"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "SOS Documentation", out);
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "Sensor Observation Service (SOS)") +
                "\n" +
                "<h2>Overview</h2>\n" +
                "In addition to making data available via \n" +
                  "<a rel=\"bookmark\" href=\"" + tErddapUrl + "/griddap/index.html?" + 
                      EDStatic.encodedDefaultPIppQuery + "\">gridddap</a> and \n" +
                  "<a rel=\"bookmark\" href=\"" + tErddapUrl + "/tabledap/index.html?" + 
                      EDStatic.encodedDefaultPIppQuery + "\">tabledap</a>, \n" + 
                  "ERDDAP makes some datasets\n" +
                "<br>available via ERDDAP's Sensor Observation Service (SOS) web service.\n" +
                "\n" +
                "<p>" + 
                String2.replaceAll(EDStatic.sosLongDescriptionHtml, "&erddapUrl;", tErddapUrl) + 
                "<p>See the\n" +
                "<a rel=\"bookmark\" href=\"" + tErddapUrl + "/sos/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\">list of datasets available via SOS</a>\n" +
                "at this ERDDAP installation.\n" +
                "<br>The SOS web pages listed there for each dataset have further documentation and sample requests.\n" +
                "\n");

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }

    }


    /**
     * Process a WCS request.
     * This WCS service is intended to simulate the THREDDS WCS service (version 1.0.0).
     * See https://www.unidata.ucar.edu/software/thredds/current/tds/reference/WCS.html
     * O&M document(?) says that query names are case insensitive, but query values are case sensitive.
     * Background info: https://www.opengeospatial.org/standards/wcs
     *
     * <p>This assumes request was for /erddap/wcs.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    ("wcs") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doWcs(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, int datasetIDStartsAt, String userQuery) throws Throwable {

        if (!EDStatic.wcsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WCS"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);

        //catch other responses outside of try/catch  (so errors handled in doGet)
        if (endOfRequestUrl.equals("") || endOfRequestUrl.equals("index.htm")) {
            sendRedirect(response, tErddapUrl + "/wcs/index.html?" + 
                EDStatic.passThroughPIppQueryPage1(request));
            return;
        }

        //list the WCS datasets
        if (endOfRequestUrl.startsWith("index.")) {
            sendDatasetList(request, response, loggedInAs, "wcs", endOfRequestUrl.substring(5)); 
            return;
        }        

        //WCS documentation web page
        if (endOfRequestUrl.equals("documentation.html")) {
            doWcsDocumentation(request, response, loggedInAs);
            return;
        }       

        //endOfRequestUrl should be erdMHchla8day/[EDDGrid.wcsServer]
        String urlEndParts[] = String2.split(endOfRequestUrl, '/');
        String tDatasetID = urlEndParts.length > 0? urlEndParts[0] : "";
        String part1 = urlEndParts.length > 1? urlEndParts[1] : "";
        EDDGrid eddGrid = gridDatasetHashMap.get(tDatasetID);
        if (eddGrid == null) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unknownDatasetID, tDatasetID));
            return;
        }

        //check loggedInAs
        String roles[] = EDStatic.getRoles(loggedInAs);
        if (!eddGrid.isAccessibleTo(roles)) {
            //WCS access: all requests are data requests
            //listPrivateDatasets and graphsAccessibleToPublic don't apply
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID, 
                eddGrid.graphsAccessibleToPublic());
            return;
        }

        //check accessibleViaWCS
        if (eddGrid.accessibleViaWCS().length() >  0) {
            sendResourceNotFoundError(request, response, eddGrid.accessibleViaWCS());
            return;
        }

        //give the dataset the opportunity to update  (WCS)
        try {
            eddGrid.update();
        } catch (WaitThenTryAgainException e) {
            //unload the dataset and set flag to reload it
            LoadDatasets.tryToUnload(this, tDatasetID, new StringArray(), true); //needToUpdateLucene
            EDD.requestReloadASAP(tDatasetID);
            throw e;
        }

        //write /wcs/[datasetID]/index.html
        if (part1.equals("index.html") && urlEndParts.length == 2) {
//tally other things?
            EDStatic.tally.add("WCS index.html (since last daily report)", tDatasetID);
            EDStatic.tally.add("WCS index.html (since startup)", tDatasetID);
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, XML.encodeAsHTML(eddGrid.title()) + " - WCS", out);
            try {
                writer.write("<div class=\"standard_width\">\n");
                eddGrid.wcsDatasetHtml(loggedInAs, writer);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t; 
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        //ensure it is a SOS server request
        if (!part1.equals(EDDGrid.wcsServer) && urlEndParts.length == 2) {
            sendResourceNotFoundError(request, response, "not a WCS request");
            return;
        }

        //if eddGrid instanceof EDDGridFromErddap, redirect the request
        if (eddGrid instanceof EDDGridFromErddap && userQuery != null) {
            EDDGridFromErddap fromErddap = (EDDGridFromErddap)eddGrid;
            if (fromErddap.redirect()) {
                //https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day
                String tUrl = fromErddap.getPublicSourceErddapUrl();
                sendRedirect(response, String2.replaceAll(tUrl, "/griddap/", "/wcs/") + 
                    "/" + EDDGrid.wcsServer + "?" + userQuery);
                return;
            }
        }

        try {

            //parse userQuery  
            HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=names toLowerCase

            //if service= is present, it must be service=WCS     //technically, it is required
            String tService = queryMap.get("service"); 
            if (tService != null && !tService.equals("WCS"))
                throw new SimpleException(EDStatic.queryError + "service='" + tService + "' must be 'WCS'."); 

            //deal with the various request= options
            String tRequest = queryMap.get("request"); //test .toLowerCase() 
            if (tRequest == null)
                tRequest = "";

            String tVersion = queryMap.get("version");   //test .toLowerCase() 
            String tCoverage = queryMap.get("coverage"); //test .toLowerCase() 

            if (tRequest.equals("GetCapabilities")) {  
                //e.g., ?service=WCS&request=GetCapabilities
                OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                    request, response, "wcs_" + eddGrid.datasetID() + "_capabilities", 
                    ".xml", ".xml");
                OutputStream out = outSource.outputStream(String2.UTF_8);
                Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
                try {
                    eddGrid.wcsGetCapabilities(loggedInAs, tVersion, writer); 
                    writer.flush();
                    if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                } finally {
                    writer.close(); 
                }
                return;
                
            } else if (tRequest.equals("DescribeCoverage")) { 
                //e.g., ?service=WCS&request=DescribeCoverage
                OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                    request, response, "wcs_" + eddGrid.datasetID()+ "_" + tCoverage, 
                    ".xml", ".xml");
                OutputStream out = outSource.outputStream(String2.UTF_8);
                Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
                try {
                    eddGrid.wcsDescribeCoverage(loggedInAs, tVersion, tCoverage, writer);
                    writer.flush();
                    if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                } finally {
                    writer.close(); 
                }
                return;

            } else if (tRequest.equals("GetCoverage")) {
                //e.g., ?service=WCS&request=GetCoverage
                //format
                String requestFormat = queryMap.get("format"); //test name.toLowerCase()
                String tRequestFormats[]  = EDDGrid.wcsRequestFormats100;  //version100? wcsRequestFormats100  : wcsRequestFormats112;
                String tResponseFormats[] = EDDGrid.wcsResponseFormats100; //version100? wcsResponseFormats100 : wcsResponseFormats112;
                int fi = String2.caseInsensitiveIndexOf(tRequestFormats, requestFormat);
                if (fi < 0)
                    throw new SimpleException(EDStatic.queryError + "format=" + requestFormat + " isn't supported."); 
                String erddapFormat = tResponseFormats[fi];
                int efe = String2.indexOf(EDDGrid.dataFileTypeNames, erddapFormat);
                String fileExtension;
                if (efe >= 0) {
                    fileExtension = EDDGrid.dataFileTypeExtensions[efe];
                } else {
                    efe = String2.indexOf(EDDGrid.imageFileTypeNames, erddapFormat);
                    if (efe >= 0) {
                        fileExtension = EDDGrid.imageFileTypeExtensions[efe];
                    } else {
                        throw new SimpleException(EDStatic.queryError + "format=" + requestFormat + " isn't supported!"); //slightly different
                    }
                }                   

                OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                    request, response, 
                    "wcs_" + eddGrid.datasetID() + "_" + tCoverage + "_" +
                        String2.md5Hex12(userQuery), //datasetID is already in file name
                    erddapFormat, fileExtension);
                eddGrid.wcsGetCoverage(loggedInAs, userQuery, outSource);
                return;

            } else {
                throw new SimpleException(EDStatic.queryError + "request='" + tRequest + "' is not supported."); 
            }

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            //deal with the WCS error

            //catch errors after the response has begun
            if (response.isCommitted()) 
                throw t; //rethrown exception (will be handled in doGet try/catch)

            OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                request, response, "error", //fileName is not used
                ".xml", ".xml");
            OutputStream out = outSource.outputStream(String2.UTF_8);
            Writer writer = new OutputStreamWriter(out, String2.UTF_8);
            try {
                //???needs work, see Annex A of 1.0.0 specification
                //this is based on mapserver's exception  (thredds doesn't have xmlns...)
                String error = MustBe.getShortErrorMessage(t);
                writer.write(
                    "<ServiceExceptionReport\n" +
                    "  xmlns=\"http://www.opengis.net/ogc\"\n" +
                    "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n" +     //wms??? really???
                    "  xsi:schemaLocation=\"http://www.opengis.net/ogc http://schemas.opengeospatial.net/wms/1.1.1/OGC-exception.xsd\">\n" +
                    //there are others codes, see Table A.1; I don't differentiate.
                    "  <ServiceException code='InvalidParameterValue'>\n" + 
                    error + "\n" +
                    "  </ServiceException>\n" +
                    "</ServiceExceptionReport>\n");
                String2.log(String2.ERROR + " message sent to user: " + error);
                String2.log(MustBe.throwableToString(t));

                //essential
                writer.flush();
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            } finally {
                writer.close(); 
            }
        }
    }


    /**
     * This responds by sending out "ERDDAP's WCS Documentation" Html page.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doWcsDocumentation(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs) throws Throwable {

        if (!EDStatic.wcsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WCS"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "WCS Documentation", out);
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "Web Coverage Service (WCS)") +
                "\n" +
                "<h2>Overview</h2>\n" +
                "In addition to making data available via \n" +
                "<a rel=\"bookmark\" href=\"" + tErddapUrl + "/griddap/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\">gridddap</a> and \n" +
                "<a rel=\"bookmark\" href=\"" + tErddapUrl + "/tabledap/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\">tabledap</a>,\n" + 
                "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.\n" +
                "\n" +
                "<p>See the\n" +
                "<a rel=\"bookmark\" href=\"" + tErddapUrl + "/wcs/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\">list of datasets available via WCS</a>\n" +
                "at this ERDDAP installation.\n" +
                "\n" +
                "<p>" + String2.replaceAll(EDStatic.wcsLongDescriptionHtml, "&erddapUrl;", tErddapUrl) + "\n" +
                "\n" +
                "<p>WCS clients send HTTP POST or GET requests (specially formed URLs) to the WCS service and get XML responses.\n" +
                "See this <a rel=\"bookmark\" href=\"https://en.wikipedia.org/wiki/Web_Coverage_Service#WCS_Implementations\" \n" +
                    ">list of WCS clients (and servers)" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" + 
                "</ul>\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }

    }


    /**
     * Direct a WMS request to proper handler.
     *
     * <p>This assumes request was for /erddap/wms
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    ("wms") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doWms(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, int datasetIDStartsAt, String userQuery) throws Throwable {

        if (!EDStatic.wmsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WMS"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);
        int slashPo = endOfRequestUrl.indexOf('/'); //between datasetID/endEnd
        if (slashPo < 0) slashPo = endOfRequestUrl.length();
        String tDatasetID = endOfRequestUrl.substring(0, slashPo);
        String endEnd = slashPo >= endOfRequestUrl.length()? "" : 
            endOfRequestUrl.substring(slashPo + 1);

        //catch other responses outside of try/catch  (so errors handled in doGet)
        if (endOfRequestUrl.equals("") || endOfRequestUrl.equals("index.htm")) {
            sendRedirect(response, tErddapUrl + "/wms/index.html?" +
                EDStatic.encodedPassThroughPIppQueryPage1(request));
            return;
        }
        if (endEnd.length() == 0 && endOfRequestUrl.startsWith("index.")) {
            sendDatasetList(request, response, loggedInAs, "wms", endOfRequestUrl.substring(5)); 
            return;
        }
        if (endOfRequestUrl.equals("documentation.html")) {
            doWmsDocumentation(request, response, loggedInAs);
            return;
        }

//these 3 are demos.  Remove them (and links to them)?  add update()?
        if (endOfRequestUrl.equals("demo110.html")) { 
            doWmsDemo(request, response, loggedInAs, "1.1.0", EDStatic.wmsSampleDatasetID);
            return;
        }
        if (endOfRequestUrl.equals("demo111.html")) { 
            doWmsDemo(request, response, loggedInAs, "1.1.1", EDStatic.wmsSampleDatasetID);
            return;
        }
        if (endOfRequestUrl.equals("demo130.html")) { 
            doWmsDemo(request, response, loggedInAs, "1.3.0", EDStatic.wmsSampleDatasetID);
            return;
        }

        //if (endOfRequestUrl.equals(EDD.WMS_SERVER)) {
        //    doWmsRequest(request, response, loggedInAs, "", userQuery); //all datasets
        //    return;
        //}
        
        //for a specific dataset
        EDDGrid eddGrid = gridDatasetHashMap.get(tDatasetID);
        if (eddGrid != null) {
            if (!eddGrid.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
                !eddGrid.graphsAccessibleToPublic()) { 
                //WMS access: all requests are graphics requests
                //listPrivateDatasets doesn't apply
                EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID, 
                    false);
                return;
            }

            //request is for /wms/datasetID/...
            if (endEnd.equals("") || endEnd.equals("index.htm")) {
                sendRedirect(response, tErddapUrl + "/wms/index.html?" +
                    EDStatic.passThroughPIppQueryPage1(request));
                return;
            }

            //give the dataset the opportunity to update  (WMS)
            try {
                eddGrid.update();
            } catch (WaitThenTryAgainException e) {
                //unload the dataset and set flag to reload it
                LoadDatasets.tryToUnload(this, tDatasetID, new StringArray(), true); //needToUpdateLucene
                EDD.requestReloadASAP(tDatasetID);
                throw e;
            }

            if (endEnd.equals("index.html")) {
                doWmsDemo(request, response, loggedInAs, "1.3.0", tDatasetID);
                return;
            }

            if (endEnd.equals(EDD.WMS_SERVER)) {
                //if eddGrid instanceof EDDGridFromErddap, redirect the request
                if (eddGrid instanceof EDDGridFromErddap) {
                    EDDGridFromErddap fe = (EDDGridFromErddap)eddGrid;
                    if (fe.redirect() &&
                        //earlier versions of wms work ~differently
                        fe.sourceErddapVersion() >= 1.23 && 
                        userQuery != null &&
                        //erddap versions before 1.82 handled wms v1.3.0 differently
                        (userQuery.toLowerCase().indexOf("&version=1.1.") >= 0 || fe.sourceErddapVersion() >= 1.82)) {
                        //https://coastwatch.pfeg.noaa.gov/erddap/wms/erdMHchla8day/request?
                        //EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=erdMHchla8day
                        //%3Achlorophyll&TIME=2010-07-24T00%3A00%3A00Z&ELEVATION=0.0
                        //&TRANSPARENT=true&BGCOLOR=0x808080&FORMAT=image%2Fpng&SERVICE=WMS
                        //&REQUEST=GetMap&STYLES=&BBOX=307.2,-90,460.8,63.6&WIDTH=256&HEIGHT=256
                        //tUrl  e.g. https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMhchla8day
                        String tUrl = fe.getPublicSourceErddapUrl();
                        String sourceDatasetID = File2.getNameNoExtension(tUrl);
                        //!this is good but imperfect because fe.datasetID %3A may be part of some other part of the request
                        //handle percent encoded or not
                        String tQuery = String2.replaceAll(userQuery, fe.datasetID() + "%3A", sourceDatasetID + "%3A");
                        tQuery =        String2.replaceAll(tQuery,    fe.datasetID() + ":",   sourceDatasetID + ":");
                        sendRedirect(response, String2.replaceAll(tUrl, "/griddap/", "/wms/") + "/" + EDD.WMS_SERVER + 
                            "?" + tQuery);
                        return;
                    }
                }

                doWmsRequest(request, response, loggedInAs, tDatasetID, userQuery); 
                return;
            }

            //error
            sendResourceNotFoundError(request, response, "unmatched WMS request");
            return;
        } 

        //error
        sendResourceNotFoundError(request, response, "unmatched WMS request (2)");
    }

    /**
     * This handles a request for the /wms/request or /wms/datasetID/request -- a real WMS service request.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param tDatasetID   an EDDGrid datasetID 
     * @param userQuery post '?', still percentEncoded, may be null.
     */
    public void doWmsRequest(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String tDatasetID, String userQuery) throws Throwable {

        if (!EDStatic.wmsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WMS"));
            return;
        }

        try {

            //parse userQuery  e.g., ?service=WMS&request=GetCapabilities
            HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=names toLowerCase

            //must be service=WMS     but I don't require it
            String tService = queryMap.get("service");
            //if (tService == null || !tService.equals("WMS"))
            //    throw new SimpleException(EDStatic.queryError + "service='" + tService + "' must be 'WMS'."); 

            //deal with different request=
            String tRequest = queryMap.get("request");
            if (tRequest == null)
                tRequest = "";

            //e.g., ?service=WMS&request=GetCapabilities
            if (tRequest.equals("GetCapabilities")) {
                doWmsGetCapabilities(request, response, loggedInAs, tDatasetID, queryMap); 
                return;
            }
            
            if (tRequest.equals("GetMap")) {
                doWmsGetMap(request, response, loggedInAs, queryMap); 
                return;
            }

            //if (tRequest.equals("GetFeatureInfo")) { //optional, not yet supported

            throw new SimpleException(EDStatic.queryError + "request='" + tRequest + "' isn't supported."); 

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            String2.log("  doWms caught Exception:\n" + MustBe.throwableToString(t));

            //catch errors after the response has begun
            if (response.isCommitted()) 
                throw t; //rethrown exception (will be handled in doGet try/catch)

            //send out WMS XML error
            OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                request, response, "error", //fileName is not used
                ".xml", ".xml");
            OutputStream out = outSource.outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                //see WMS 1.3.0 spec, section H.2
                String error = MustBe.getShortErrorMessage(t);
                writer.write(
    "<?xml version='1.0' encoding=\"UTF-8\"?>\n" +
    "<ServiceExceptionReport version=\"1.3.0\"\n" +
    "  xmlns=\"http://www.opengis.net/ogc\"\n" +
    "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n" +
    "  xsi:schemaLocation=\"http://www.opengis.net/ogc http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd\">\n" +
    "  <ServiceException" + // code=\"InvalidUpdateSequence\"    ???list of codes
    //security: encodeAsXml important to prevent xml injection
    ">" + XML.encodeAsXML(error) + "</ServiceException>\n" + 
    "</ServiceExceptionReport>\n");
                String2.log(String2.ERROR + " message sent to user: " + error);
                String2.log(MustBe.throwableToString(t));

                //essential
                writer.flush();
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            } finally {
                writer.close(); 
            }
        }
    }


    /**
     * This responds by sending out the WMS html documentation page (long description).
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     */
    public void doWmsDocumentation(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs) throws Throwable {
       
        if (!EDStatic.wmsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WMS"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String e0 = tErddapUrl + "/wms/" + EDStatic.wmsSampleDatasetID + "/" + EDD.WMS_SERVER + "?"; 
        String ec = "service=WMS&#x26;request=GetCapabilities&#x26;version=";
        String e1 = "service=WMS&#x26;version="; 
        String e2 = "&#x26;request=GetMap&#x26;bbox=";// + EDStatic.wmsSampleBBox + "&#x26;"; //needs c or s
        //this section of code is in 2 places
        int bbox[] = String2.toIntArray(String2.split(EDStatic.wmsSampleBBox110, ',')); //extract info from 110 version
        int tHeight = Math2.roundToInt(((bbox[3] - bbox[1]) * 360) / Math.max(1, bbox[2] - bbox[0]));
        tHeight = Math2.minMaxDef(10, 600, 180, tHeight);
        String e2b = "rs=EPSG:4326&#x26;width=360&#x26;height=" + tHeight + 
            "&#x26;bgcolor=0x808080&#x26;layers=";
        //Land,erdBAssta5day:sst,Coastlines,LakesAndRivers,Nations,States
        String e3 = EDStatic.wmsSampleDatasetID + EDD.WMS_SEPARATOR + EDStatic.wmsSampleVariable;
        String e4 = "&#x26;styles=&#x26;format=image/png";
        String et = "&#x26;transparent=TRUE";
        String st = String2.isSomething(EDStatic.wmsSampleTime)?
            XML.encodeAsHTMLAttribute("&time=" + EDStatic.wmsSampleTime) : "";

        String tWmsGetCapabilities110    = e0 + ec + "1.1.0";
        String tWmsGetCapabilities111    = e0 + ec + "1.1.1";
        String tWmsGetCapabilities130    = e0 + ec + "1.3.0";
        String tWmsOpaqueExample110      = e0 + e1 + "1.1.0" + e2 + EDStatic.wmsSampleBBox110 + st + "&#x26;s" + e2b + "Land," + e3 + ",Coastlines,Nations" + e4;
        String tWmsOpaqueExample111      = e0 + e1 + "1.1.1" + e2 + EDStatic.wmsSampleBBox110 + st + "&#x26;s" + e2b + "Land," + e3 + ",Coastlines,Nations" + e4;
        String tWmsOpaqueExample130      = e0 + e1 + "1.3.0" + e2 + EDStatic.wmsSampleBBox130 + st + "&#x26;c" + e2b + "Land," + e3 + ",Coastlines,Nations" + e4;
        String tWmsTransparentExample110 = e0 + e1 + "1.1.0" + e2 + EDStatic.wmsSampleBBox110 + st + "&#x26;s" + e2b + e3 + e4 + et;
        String tWmsTransparentExample111 = e0 + e1 + "1.1.1" + e2 + EDStatic.wmsSampleBBox110 + st + "&#x26;s" + e2b + e3 + e4 + et;
        String tWmsTransparentExample130 = e0 + e1 + "1.3.0" + e2 + EDStatic.wmsSampleBBox130 + st + "&#x26;c" + e2b + e3 + e4 + et;

        //What is WMS?   (generic) 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "WMS Documentation", out);
        try {
            String likeThis = "<a href=\"" + tErddapUrl + "/wms/" + EDStatic.wmsSampleDatasetID + 
                     "/index.html\">like this</a>";
            String makeAGraphRef = "<a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/images/embed.html\">" +
                EDStatic.mag + "</a>\n";
            String datasetListRef = 
                "  See the <a rel=\"bookmark\" href=\"" + tErddapUrl + 
                "/wms/index.html?" + EDStatic.encodedDefaultPIppQuery + 
                "\">list of datasets available via WMS</a> at this ERDDAP installation.\n";
            String makeAGraphListRef =
                "  See the <a rel=\"contents\" href=\"" + tErddapUrl + 
                "/info/index.html?" + EDStatic.encodedDefaultPIppQuery + 
                "\">list of datasets with Make A Graph</a> at this ERDDAP installation.\n";

            writer.write(
                //see almost identical documentation at ...
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "wms", "Documentation") +
                String2.replaceAll(EDStatic.wmsLongDescriptionHtml, "&erddapUrl;", tErddapUrl) + "\n" +
                datasetListRef +
                //"<p>\n" +
                "<h2>Three Ways to Make Maps with WMS</h2>\n" +
                "<ol>\n" +
                "<li> <strong>In theory, anyone can download, install, and use WMS client software.</strong>\n" +
                "  <br>Some clients are: \n" +
                "    <a rel=\"bookmark\" href=\"https://www.esri.com/software/arcgis/\">ArcGIS" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> and\n" +
                "    <a rel=\"bookmark\" href=\"http://udig.refractions.net//\">uDig" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>. \n" +
                "  To make these work, you would install the software on your computer.\n" +
                "  Then, you would enter the URL of the WMS service into the client.\n" +
                //arcGis required WMS 1.1.1 (1.1.0 and 1.3.0 didn't work)
                "  For example, in ArcGIS (not yet fully working because it doesn't handle time!), use\n" +
                "  \"Arc Catalog : Add Service : Arc Catalog Servers Folder : GIS Servers : Add WMS Server\".\n" +
                "  In ERDDAP, each dataset has its own WMS service, which is located at\n" +
                "  <br>" + tErddapUrl + "/wms/<i>datasetID</i>/" + EDD.WMS_SERVER + "?\n" +  
                "  <br>For example: <strong>" + e0 + "</strong>\n" +  
                "  <br>(Some WMS client programs don't want the <strong>?</strong> at the end of that URL.)\n" +
                datasetListRef +
                "  <p><strong>In practice,</strong> we haven't found any WMS clients that properly handle dimensions\n" +
                "  other than longitude and latitude (e.g., time), a feature which is specified by the WMS\n" +
                "  specification and which is utilized by most datasets in ERDDAP's WMS servers.\n" +
                "  You may find that using a dataset's " + makeAGraphRef + 
                "     form and selecting the .kml file type\n" +
                "  (an OGC standard) to load images into <a rel=\"bookmark\" href=\"https://www.google.com/earth/\">Google Earth" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> provides\n" +            
                "    a good (non-WMS) map client.\n" +
                makeAGraphListRef +
                "<li> <strong>Web page authors can embed a WMS client in a web page.</strong>\n" +
                "  <br>For example, ERDDAP uses \n" +
                "    <a rel=\"bookmark\" href=\"https://leafletjs.com\">Leaflet" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>, \n" +  
                "    which is a very versatile WMS client, for the WMS\n" +
                "  page for each ERDDAP dataset \n" +
                "    (" + likeThis + ").\n" +  
                datasetListRef +
                "  Leaflet doesn't automatically deal with dimensions other than longitude and latitude\n" +            
                "  (e.g., time), so you will have to write JavaScript (or other scripting code) to do that.\n" +
                "  (Adventurous JavaScript programmers can look at the Source Code from a web page " + likeThis + ".)\n" + 
                "  <p>Another commonly used JavaScript WMS client is\n" +
                "    <a rel=\"bookmark\" href=\"https://openlayers.org/\">OpenLayers" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "<li> <strong>A person with a browser or a computer program can generate special WMS URLs.</strong>\n" +
                "  <br>For example:\n" +
                "  <ul>\n" +
                "  <li>To get an image with a map with an opaque background:\n" +
                "    <br><a href=\"" + tWmsOpaqueExample130 + "\">" + 
                    String2.replaceAll(tWmsOpaqueExample130, "&", "<wbr>&") + "</a>\n" +
                "  <li>To get an image with a map with a transparent background:\n" +
                "    <br><a href=\"" + tWmsTransparentExample130 + "\">" + 
                    String2.replaceAll(tWmsTransparentExample130, "&", "<wbr>&") + "</a>\n" +
                "  </ul>\n" +
                datasetListRef +
                "  <strong>See the details below.</strong>\n" +
                "  <p><strong>In practice, it is easier, more versatile,\n" +
                "  and more efficient to use a dataset's\n" +
                "    " + makeAGraphRef + " web page</strong>\n" +
                "  than to use WMS for this purpose.\n" +
                makeAGraphListRef +
                "</ol>\n" +
                "\n");

            //GetCapabilities
            writer.write(
                "<h2><a class=\"selfLink\" id=\"GetCapabilities\" href=\"#GetCapabilities\" rel=\"bookmark\">Forming GetCapabilities URLs</a></h2>\n" +
                "A GetCapabilities request returns an XML document which provides background information\n" +
                "  about the service and basic information about all of the data available from this\n" +
                "  service. For this dataset, for WMS version 1.3.0, use\n" + 
                "  <br><a href=\"" + tWmsGetCapabilities130 + "\">\n" + 
                                     tWmsGetCapabilities130 + "</a>\n" +
                "  <p>The supported parameters for a GetCapabilities request are:\n" +
                "<table class=\"erd commonBGColor nowrap\" style=\"width:100%;\">\n" +
                "  <tr>\n" +
                "    <th><i>name=value</i><sup>*</sup></th>\n" +
                "    <th>Description</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>service=WMS</td>\n" +
                "    <td>Required.</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>version=<i>version</i></td>\n" +
                "    <td>Currently, ERDDAP's WMS supports \"1.1.0\", \"1.1.1\", and \"1.3.0\".\n" +
                "      <br>This parameter is optional. The default is \"1.3.0\".</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>request=GetCapabilities</td>\n" +
                "    <td>Required.</td>\n" +
                "  </tr>\n" +
                "  </table>\n" +
                "  <sup>*</sup> Parameter names are case-insensitive.\n" +
                "  <br>Parameter values are case sensitive and must be\n" +
                "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>:\n" +
                "   all characters in query values other than A-Za-z0-9_-!.~'()* must be encoded as %HH, where\n" +
                "   HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n" +
                "   Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n" +
                "   (ask a programmer for help). There are\n" +
                    "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com/\">websites that percent encode/decode for you" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "  <br>The parameters may be in any order in the URL, separated by '&amp;' .\n" +
                "  <br>&nbsp;\n" +
                "\n");

            //getMap
            writer.write(
                "<h2><a class=\"selfLink\" id=\"GetMap\" href=\"#GetMap\" rel=\"bookmark\">Forming GetMap URLs</a></h2>\n" +
                "  A person with a browser or a computer program can generate a special URL to request a map.\n" + 
                "  The URL must be in the form\n" +
                "  <br>" + tErddapUrl + "/wms/<i>datasetID</i>/" + EDD.WMS_SERVER + "?<i>query</i> " +
                "  <br>The query for a WMS GetMap request consists of several <i>parameterName=value</i>, separated by '&amp;'.\n" +
                "  For example,\n" +
                "  <br><a href=\"" + tWmsOpaqueExample130 + "\">" + 
                  String2.replaceAll(tWmsOpaqueExample130, "&", "<wbr>&") + "</a>\n" +
                "  <br>The <a class=\"selfLink\" id=\"parameters\" href=\"#parameters\" rel=\"bookmark\">parameter</a> options for the GetMap request are:\n" +
                "  <br>&nbsp;\n" + //necessary for the blank line before the table (not <p>)
                "<table class=\"erd commonBGColor\" style=\"width:100%; \">\n" +
                "  <tr>\n" +
                "    <th><i>name=value</i><sup>*</sup></th>\n" +
                "    <th>Description</th>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td class=\"N\">service=WMS</td>\n" +
                "    <td>Required.</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>version=<i>version</i></td>\n" +
                "    <td>Request version.\n" +
                "      Currently, ERDDAP's WMS supports \"1.1.0\", \"1.1.1\", and \"1.3.0\".  Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>request=GetMap</td>\n" +
                "    <td>Request name.  Required.</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>layers=<i>layer_list</i></td>\n" +
                "    <td>Comma-separated list of one or more map layers.\n" +
                "        Layers are drawn in the order they occur in the list.\n" +
                "        Currently in ERDDAP's WMS, the layer names from datasets are named <i>datasetID</i>" + 
                    EDD.WMS_SEPARATOR + "<i>variableName</i> .\n" +
                "        In ERDDAP's WMS, there are five layers not based on ERDDAP datasets:\n" +
                "        <ul>\n" +
                "        <li> \"Land\" may be drawn BEFORE (as an under layer) or AFTER (as a land mask) layers from grid datasets.\n" +
                "        <li> \"Coastlines\" usually should be drawn AFTER layers from grid datasets.\n" +  
                "        <li> \"LakesAndRivers\" draws lakes and rivers. This usually should be drawn AFTER layers from grid datasets.\n" +
                "        <li> \"Nations\" draws national political boundaries. This usually should be drawn AFTER layers from grid datasets.\n" +
                "        <li> \"States\" draws state political boundaries. This usually should be drawn AFTER layers from grid datasets.\n" +
                "        </ul>\n" +                
                "        Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>styles=<i>style_list</i></td>\n" +
                "    <td>Comma-separated list of one rendering style per requested layer.\n" +
                "      Currently in ERDDAP's WMS, the only style offered for each layer is the default style,\n" +
                "      which is specified via \"\" (nothing).\n" +
                "      For example, if you request 3 layers, you can use \"styles=,,\".\n" +
                "      Or, even easier, you can request the default style for all layers via \"styles=\".\n" + 
                "      Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td class=\"N\">1.1.0: srs=<i>namespace:identifier</i>" +
                           "<br>1.1.1: srs=<i>namespace:identifier</i>" +
                           "<br>1.3.0: crs=<i>namespace:identifier</i></td>\n" +
                "    <td>Coordinate reference system.\n" +
                "        <br>Currently in ERDDAP's WMS 1.1.0, the only valid SRS is EPSG:4326.\n" +
                "        <br>Currently in ERDDAP's WMS 1.1.1, the only valid SRS is EPSG:4326.\n" +
                "        <br>Currently in ERDDAP's WMS 1.3.0, the only valid CRS's are CRS:84 and EPSG:4326,\n" +
                "        <br>Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>bbox=<i>4commaSeparatedValues</i></td>\n" +
                "    <td>Bounding box corners in SRS/CRS units.\n" +
                "      For version=1.3.0 with CRS=EPSG:4326, the 4 values are: minLat,minLon,maxLat,maxLon.\n" +
                "      For all other situations, the 4 values are: minLon,minLat,maxLon,maxLat.\n" +
                "      (The reverse order! Yes, it's bizarre. Welcome to the world of OGC!)\n" +
                "      ERDDAP supports requests within the dataset's longitude (perhaps 0 to 360, perhaps -180 to 180)\n" +
                "      and latitude range. Most WMS clients assume longitude values are in the range -180 to 180.\n" +
                "      If ERDDAP offers a variant of a dataset with longitude -180 to 180, use it for WMS requests.\n" +
                "      Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>width=<i>output_width</i></td>\n" +
                "    <td>Width in pixels of map picture. Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>height=<i>output_height</i></td>\n" +
                "    <td>Height in pixels of map picture. Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>format=<i>output_format</i></td>\n" +
                "    <td>Output format of map.  Currently in ERDDAP's WMS, only image/png is valid.\n" +
                "      Required.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>transparent=<i>TRUE|FALSE</i></td>\n" +
                "    <td>Background transparency of map.  Optional (default=FALSE).\n" +
                "      If TRUE, any part of the image using the BGColor will be made transparent.\n" +      
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>bgcolor=<i>color_value</i></td>\n" +
                "    <td>Hexadecimal 0xRRGGBB color value for the background color. Optional (default=0xFFFFFF, white).\n" +
                "      If transparent=true, we recommend bgcolor=0x808080 (gray), since white is in some color palettes.\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>exceptions=<i>exception_format</i></td>\n" +
                "    <td>The format for WMS exception responses.  Optional.\n" +
                "      <br>Currently, ERDDAP's WMS 1.1.0 and 1.1.1 supports\n" +
                "          \"application/vnd.ogc.se_xml\" (the default),\n" +
                "      <br>\"application/vnd.ogc.se_blank\" (a blank image) and\n" +
                "          \"application/vnd.ogc.se_inimage\" (the error in an image).\n" +
                "      <br>Currently, ERDDAP's WMS 1.3.0 supports \"XML\" (the default),\n" +
                "         \"BLANK\" (a blank image), and\n" +
                "      <br>\"INIMAGE\" (the error in an image).\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>time=<i>time</i></td>\n" +
                "    <td>Time value of layer desired, specified in ISO8601 format: yyyy-MM-ddTHH:mm:ssZ .\n" +
                "      Currently in ERDDAP's WMS, you can only specify one time value per request.\n" +
                "      <br>In ERDDAP's WMS, the value nearest to the value you specify (if between min and max) will be used.\n" +
                "      <br>In ERDDAP's WMS, the default value is the last value in the dataset's 1D time array.\n" +
                "      <br>In ERDDAP's WMS, \"current\" is interpreted as the last available time (recent or not).\n" +
                "      <br>Optional (in ERDDAP's WMS, the default is the last value, whether it is recent or not).\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>elevation=<i>elevation</i></td>\n" +
                "    <td>Elevation of layer desired.\n" +
                "      Currently in ERDDAP's WMS, you can only specify one elevation value per request.\n" +
                "      <br>In ERDDAP's WMS, this is used for the altitude or depth (converted to altitude) dimension (if any).\n" +
                "      (in meters, positive=up)\n" +
                "      <br>In ERDDAP's WMS, the value nearest to the value you specify (if between min and max) will be used.\n" +
                "      <br>Optional (in ERDDAP's WMS, the default value is the last value in the dataset's 1D altitude or depth array).\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>dim_<i>name</i>=<i>value</i></td>\n" + //see WMS 1.3.0 spec section C.3.5
                "    <td>Value of other dimensions as appropriate.\n" +
                "      Currently in ERDDAP's WMS, you can only specify one value per dimension per request.\n" +
                "      <br>In ERDDAP's WMS, this is used for the non-time, non-altitude, non-depth dimensions.\n" +
                "      <br>The name of a dimension will be \"dim_\" plus the dataset's name for the dimension, for example \"dim_model\".\n" +
                "      <br>In ERDDAP's WMS, the value nearest to the value you specify (if between min and max) will be used.\n" +
                "      <br>Optional (in ERDDAP's WMS, the default value is the last value in the dimension's 1D array).\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "</table>\n" +
                //WMS 1.3.0 spec section 6.8.1
                "  <sup>*</sup> Parameter names are case-insensitive.\n" +
                "  <br>Parameter values are case sensitive and must be\n" +
                "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>:\n" +
                "  all characters in query values other than A-Za-z0-9_-!.~'()* must be encoded as %HH, where\n" +
                "  HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n" +
                "  Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n" +
                "   (ask a programmer for help). There are\n" +
                    "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com/\">websites that percent encode/decode for you" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "  <br>The parameters may be in any order in the URL, separated by '&amp;' .\n" +
                "<p>(Revised from Table 8 of the WMS 1.3.0 specification)\n" +
                "\n");

            //notes
            writer.write(
                "<h3><a class=\"selfLink\" id=\"notes\" href=\"#notes\" rel=\"bookmark\">Notes</a></h3>\n" +
                "<ul>\n" +     
                //These WMS requirements are also in setupDatasetXml.
                "<li><a class=\"selfLink\" id=\"DatasetRequirements\" href=\"#DatasetRequirements\" rel=\"bookmark\"><strong>Dataset Requirements:</strong></a>\n" +
                "  The main requirements for a variable to be accessible via\n" +
                "  ERDDAP's WMS server are:\n" +
                "  <ul>\n" +
                "  <li>The dataset must be an EDDGrid... dataset.\n" +
                "  <li>The data variable MUST be a gridded variable.\n" +
                "  <li>The data variable MUST have longitude and latitude axis variables. (Other axis variables\n" +
                "    are OPTIONAL.)\n" +
                "  <li>There MUST be some longitude values between -180 and 180.\n" +
                "  <li>The\n" +
                "    <a rel=\"help\" \n" +
                "    href=\"https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#colorBar\" \n" +
                "    >colorBarMinimum and colorBarMaximum attributes</a>\n" +
                "     MUST be specified. (Other color bar\n" +
                "    attributes are OPTIONAL.)\n" +
                "  </ul>\n" +
                "<li><strong>Grid data layers:</strong> In ERDDAP's WMS, all data variables in grid datasets that use\n" +
                "  longitude and latitude dimensions are available via WMS.\n" +
                "  Each such variable is available as a WMS layer, with the name <i>datasetID</i>" + 
                    EDD.WMS_SEPARATOR + "<i>variableName</i>.\n" +
                "  Each such layer is transparent (i.e., data values are represented as a range of colors\n" +
                "  and missing values are represented by transparent pixels).\n" +
                "<li><strong>Table data layers:</strong> Currently in ERDDAP's WMS, data variables in table datasets are\n" +
                "  not available via WMS.\n" +
                "<li><strong>Dimensions:</strong> A consequence of the WMS design is that the TIME, ELEVATION, and other \n" +
                "  dimension values that you specify in a GetMap request apply to all of the layers.\n" +
                "  There is no way to specify different values for different layers.\n" +
                //"<li><strong>Longitude:</strong> The supported CRS values only support longitude values from -180 to 180.\n" +
                //"   But some ERDDAP datasets have longitude values 0 to 360.\n" +
                //"   Currently in ERDDAP's WMS, those datasets are only available from longitude 0 to 180 in WMS.\n" +
                "<li><strong>Strict?</strong> The table above specifies how a client should form a GetMap request.\n" +
                "  In practice, ERDDAP's WMS tries to be as lenient as possible when processing GetMap\n" +
                "  requests, since many current clients don't follow the specification. However, if you\n" +
                "  are forming GetMap URLs, we encourage you to try to follow the specification.\n" +
                "<li><strong>Why are there separate WMS servers for each dataset?</strong> Because the GetCapabilities\n" +
                "  document lists all values of all dimensions for each dataset, the information for each\n" +
                "  dataset can be voluminous (easily 300 KB). If all the datasets (currently ~300 at the)\n" +
                "  ERDDAP main site were to be included in one WMS, the resulting GetCapabilities document\n" +
                "  would be huge (~90 MB) which would take a long time to download (causing many people\n" +
                "  think something was wrong and give up) and would overwhelm most client software.\n" +
                //"   However, a WMS server with all of this ERDDAP's datasets does exist.  You can access it at\n" +
                //"   <br>" + tErddapUrl + "/wms/" + EDD.WMS_SERVER + "?\n" + 
                "</ul>\n");

            writer.write(
                //1.3.0 examples
                "<h2><a class=\"selfLink\" id=\"examples\" href=\"#examples\" rel=\"bookmark\">Examples</a></h2>\n" +
                "<p>ERDDAP is compatible with the current <strong>WMS 1.3.0</strong> standard.\n" +
                "<table class=\"erd commonBGColor\" style=\"width:100%; \">\n" +
                "  <tr>\n" +
                "    <td><strong> GetCapabilities </strong></td>\n" +
                "    <td><a href=\"" + tWmsGetCapabilities130 + "\">" + 
                    String2.replaceAll(tWmsGetCapabilities130, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong> GetMap </strong><br> (opaque) </td>\n" +
                "    <td><a href=\"" + tWmsOpaqueExample130 + "\">" + 
                    String2.replaceAll(tWmsOpaqueExample130, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong> GetMap </strong><br> (transparent) </td>\n" +
                "    <td><a href=\"" + tWmsTransparentExample130 + "\">" + 
                    String2.replaceAll(tWmsTransparentExample130, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td class=\"N\"><strong> In <a rel=\"bookmark\" href=\"https://leafletjs.com\">Leaflet" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> </strong></td> \n" +
                "    <td><a href=\"" + tErddapUrl + "/wms/demo130.html\">Demo (WMS 1.3.0)</a></td>\n" +  
                "  </tr>\n" +
                "</table>\n" +
                "\n" +

                //1.1.1 examples
                "<br>&nbsp;\n" +
                "<p><a class=\"selfLink\" id=\"examples111\" href=\"#examples111\" rel=\"bookmark\">ERDDAP</a> is also compatible with the older\n" +
                "<strong>WMS 1.1.1</strong> standard, which may be needed when working with older client software.\n" +
                "<table class=\"erd commonBGColor\" style=\"width:100%; \">\n" +
                "  <tr>\n" +
                "    <td><strong> GetCapabilities </strong></td>\n" +
                "    <td><a href=\"" + tWmsGetCapabilities111 + "\">" + 
                    String2.replaceAll(tWmsGetCapabilities111, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong> GetMap </strong><br> (opaque) </td>\n" +
                "    <td><a href=\"" + tWmsOpaqueExample111 + "\">" + 
                    String2.replaceAll(tWmsOpaqueExample111, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong> GetMap </strong><br> (transparent) </td>\n" +
                "    <td><a href=\"" + tWmsTransparentExample111 + "\">" + 
                    String2.replaceAll(tWmsTransparentExample111, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td class=\"N\"><strong> In <a rel=\"bookmark\" href=\"https://leafletjs.com\">Leaflet" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> </strong></td> \n" +
                "    <td><a href=\"" + tErddapUrl + "/wms/demo111.html\">Demo (WMS 1.1.1)</a></td>\n" +  
                "  </tr>\n" +
                "</table>\n" +
                "\n" +

                //1.1.0 examples
                "<br>&nbsp;\n" +
                "<p><a class=\"selfLink\" id=\"examples110\" href=\"#examples110\" rel=\"bookmark\">ERDDAP</a> is also compatible with the older\n" +
                "<strong>WMS 1.1.0</strong> standard, which may be needed when working with older client software.\n" +
                "<table class=\"erd commonBGColor\" style=\"width:100%; \">\n" +
                "  <tr>\n" +
                "    <td><strong> GetCapabilities </strong></td>\n" +
                "    <td><a href=\"" + tWmsGetCapabilities110 + "\">" + 
                    String2.replaceAll(tWmsGetCapabilities110, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong> GetMap </strong><br> (opaque) </td>\n" +
                "    <td><a href=\"" + tWmsOpaqueExample110 + "\">" + 
                    String2.replaceAll(tWmsOpaqueExample110, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td><strong> GetMap </strong><br> (transparent) </td>\n" +
                "    <td><a href=\"" + tWmsTransparentExample110 + "\">" + 
                    String2.replaceAll(tWmsTransparentExample110, "&", "<wbr>&") + "</a></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td class=\"N\"><strong> In <a rel=\"bookmark\" href=\"https://leafletjs.com\">Leaflet" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> </strong></td> \n" +
                "    <td><a href=\"" + tErddapUrl + "/wms/demo110.html\">Demo (WMS 1.1.0)</a></td>\n" +  
                "  </tr>\n" +
                "</table>\n" +
                "\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Respond to WMS GetMap request for doWms.
     *
     * <p>If the request is from one dataset's wms and it's an EDDGridFromErddap, redirect to remote erddap.
     *  Currently, all requests are from one dataset's wms.
     *
     * <p>Similarly, if request if from one dataset's wms, 
     *   this method can cache results in separate dataset directories.
     *   (Which is good, because dataset's cache is emptied when dataset reloaded.)
     *   Otherwise, it uses EDStatic.fullWmsCacheDirectory.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param queryMap has name=value from the url query string.
     *    names are toLowerCase. values are original values.
     */
    public void doWmsGetMap(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, HashMap<String, String> queryMap) throws Throwable {

        if (!EDStatic.wmsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WMS"));
            return;
        }

        String userQuery = request.getQueryString(); //post "?", still encoded, may be null
        if (userQuery == null)
            userQuery = "";
        String fileName = "wms_" + String2.md5Hex12(userQuery); //no extension

        int width = -1, height = -1, bgColori = 0xFFFFFF;
        String format = null, fileTypeName = null, exceptions = null;
        boolean transparent = false;
        OutputStreamSource outputStreamSource = null;
        OutputStream outputStream = null;
        try {

            //find mainDatasetID  (if request is from one dataset's wms)
            //Currently, all requests are from one dataset's wms.
            // https://coastwatch.pfeg.noaa.gov/erddap/wms/erdBAssta5day/EDD.WMS_SERVER?service=.....
            String[] requestParts = String2.split(request.getRequestURI(), '/');  //post EDD.baseUrl, pre "?"
            int wmsPart = String2.indexOf(requestParts, "wms");
            String mainDatasetID = null;
            if (wmsPart >= 0 && wmsPart == requestParts.length - 3) { //it exists, and there are two more parts
                mainDatasetID = requestParts[wmsPart + 1];
                EDDGrid eddGrid = gridDatasetHashMap.get(mainDatasetID);
                if (eddGrid == null) {
                    mainDatasetID = null; //something else is going on, e.g., wms for all dataset's together
                } else if (!eddGrid.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
                           !eddGrid.graphsAccessibleToPublic()) { 
                    //WMS: all requests are graphics requests
                    //listPrivateDatasets doesn't apply
                    EDStatic.sendHttpUnauthorizedError(loggedInAs, response, mainDatasetID,
                        false);
                    return;
                } else if (eddGrid instanceof EDDGridFromErddap) {
                    EDDGridFromErddap fromErddap = (EDDGridFromErddap)eddGrid;
                    if (fromErddap.redirect() && 
                        //earlier versions of wms work ~differently
                        fromErddap.sourceErddapVersion() >= 1.23) {
                        //Redirect to remote erddap if request is from one dataset's wms and it's an EDDGridFromErddap.
                        //tUrl e.g., https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdBAssta5day
                        String tUrl = fromErddap.getPublicSourceErddapUrl();
                        int gPo = tUrl.indexOf("/griddap/");
                        if (gPo >= 0) {
                            String rDatasetID = tUrl.substring(gPo + "/griddap/".length()); //rDatasetID is at end of tUrl
                            tUrl = String2.replaceAll(tUrl, "/griddap/", "/wms/");
                            StringBuilder etUrl = new StringBuilder(tUrl + "/" + EDD.WMS_SERVER + "?");

                            //change request's layers=mainDatasetID:var,mainDatasetID:var 
                            //              to layers=rDatasetID:var,rDatasetID:var
                            if (userQuery != null) {    
                                String qParts[] = Table.getDapQueryParts(userQuery); //decoded.  always at least 1 part (may be "")
                                for (int qpi = 0; qpi < qParts.length; qpi++) {
                                    if (qpi > 0) etUrl.append('&');

                                    //this use of replaceAll is perhaps not perfect but very close...
                                    //percentDecode parts, so EDD.WMS_SEPARATOR and other chars won't be encoded
                                    String part = qParts[qpi]; 
                                    int epo = part.indexOf('=');
                                    if (epo >= 0) {
                                        part = String2.replaceAll(part, 
                                            "=" + mainDatasetID + EDD.WMS_SEPARATOR, 
                                            "=" + rDatasetID    + EDD.WMS_SEPARATOR);
                                        part = String2.replaceAll(part, 
                                            "," + mainDatasetID + EDD.WMS_SEPARATOR, 
                                            "," + rDatasetID    + EDD.WMS_SEPARATOR);
                                        //encodedKey = encodedValue
                                        etUrl.append(SSR.minimalPercentEncode(part.substring(0, epo)) + "=" +
                                                     SSR.minimalPercentEncode(part.substring(epo + 1)));
                                    } else {
                                        etUrl.append(qParts[qpi]); //SSR.minimalPercentEncode?
                                    }
                                }
                            }
                            sendRedirect(response, etUrl.toString()); 
                            return;
                        } else {
                            String2.log(EDStatic.errorInternal + "\"/griddap/\" should have been in " +
                                "EDDGridFromErddap.getNextLocalSourceErddapUrl()=" + tUrl + " .");
                            //fall through
                        }
                    } //else fall through
                }     //else fall through
            }
            EDStatic.tally.add("WMS doWmsGetMap (since last daily report)", mainDatasetID);
            EDStatic.tally.add("WMS doWmsGetMap (since startup)", mainDatasetID);
            if (mainDatasetID != null) 
                fileName = mainDatasetID + "_" + fileName;
            String cacheDir = mainDatasetID == null? EDStatic.fullWmsCacheDirectory :
                EDD.cacheDirectory(mainDatasetID);            
            if (reallyVerbose) String2.log("doWmsGetMap cacheDir=" + cacheDir);

            //*** get required values   see wms spec, section 7.3.2   queryMap names are toLowerCase
            String tVersion     = queryMap.get("version");
            if (tVersion == null)
                tVersion = "1.3.0";
            if (!tVersion.equals("1.1.0") && !tVersion.equals("1.1.1") && 
                !tVersion.equals("1.3.0"))
                throw new SimpleException(EDStatic.queryError + "VERSION=" + tVersion + 
                    " must be '1.1.0', '1.1.1', or '1.3.0'.");
            boolean v130 = tVersion.equals("1.3.0"); //future: or higher

            String layersCsv    = queryMap.get("layers");
            String stylesCsv    = queryMap.get("styles");
            String crs          = queryMap.get("crs");
            if (crs == null) 
                crs             = queryMap.get("srs");
            String bboxCsv      = queryMap.get("bbox");
            width               = String2.parseInt(queryMap.get("width"));
            height              = String2.parseInt(queryMap.get("height"));
            format              = queryMap.get("format");

            //optional values
            String tTransparent = queryMap.get("transparent"); 
            String tBgColor     = queryMap.get("bgcolor");
            exceptions          = queryMap.get("exceptions");
            //+ dimensions   time=, elevation=, ...=  handled below

            //*** validate parameters
            transparent = tTransparent == null? false : 
                String2.parseBoolean(tTransparent);  //e.g., "false"

            bgColori = tBgColor == null || tBgColor.length() != 8 || !tBgColor.startsWith("0x")? 
                0xFFFFFF :
                String2.parseInt(tBgColor); //e.g., "0xFFFFFF"
            if (bgColori == Integer.MAX_VALUE)
                bgColori = 0xFFFFFF;


            //*** throw exceptions related to throwing exceptions 
            //(until exception, width, height, and format are valid, fall back to XML format)

            //convert exceptions to latest format
            String oExceptions = exceptions;
            if (exceptions == null)
                exceptions = "XML";
            if      (exceptions.equals("application/vnd.ogc.se_xml"))     exceptions = "XML";
            else if (exceptions.equals("application/vnd.ogc.se_blank"))   exceptions = "BLANK";
            else if (exceptions.equals("application/vnd.ogc.se_inimage")) exceptions = "INIMAGE";
            if (!exceptions.equals("XML") && 
                !exceptions.equals("BLANK") &&
                !exceptions.equals("INIMAGE")) {
                exceptions = "XML"; //fall back
                if (tVersion.equals("1.1.0") || tVersion.equals("1.1.1"))
                    throw new SimpleException(EDStatic.queryError + "EXCEPTIONS=" + oExceptions + 
                        " must be one of 'application/vnd.ogc.se_xml', 'application/vnd.ogc.se_blank', " +
                        "or 'application/vnd.ogc.se_inimage'.");  
                else //1.3.0+
                    throw new SimpleException(EDStatic.queryError + "EXCEPTIONS=" + oExceptions + 
                        " must be one of 'XML', 'BLANK', or 'INIMAGE'.");  
            }

            if (width < 2 || width > EDD.WMS_MAX_WIDTH) {
                exceptions = "XML"; //fall back
                throw new SimpleException(EDStatic.queryError + "WIDTH=" + width + 
                    " must be between 2 and " + EDD.WMS_MAX_WIDTH + ".");
            }
            if (height < 2 || height > EDD.WMS_MAX_HEIGHT) {
                exceptions = "XML"; //fall back
                throw new SimpleException(EDStatic.queryError + "HEIGHT=" + height + 
                    " must be between 2 and " + EDD.WMS_MAX_HEIGHT + ".");
            }
            if (format == null || !format.toLowerCase().equals("image/png")) {
                exceptions = "XML"; //fall back
                throw new SimpleException(EDStatic.queryError + "FORMAT=" + format +
                    " must be image/png.");
            }
            format = format.toLowerCase();
            fileTypeName = ".png"; 
            String extension = fileTypeName;  //here, not in other situations

            //*** throw Warnings/Exceptions for other params?   (try to be lenient)
            //layers
            String layers[];
            if (layersCsv == null) {
                layers = new String[]{""};
                //it is required and so should be an Exception, 
                //but http://mapserver.refractions.net/phpwms/phpwms-cvs/ (?) doesn't send it sometimes,
                //so treat null as all defaults
                String2.log("WARNING: In the WMS query, LAYERS wasn't specified: " + userQuery);
            } else {
                layers = String2.split(layersCsv, ',');
            }
            if (layers.length > EDD.WMS_MAX_LAYERS)
                throw new SimpleException(EDStatic.queryError + "the number of LAYERS=" + layers.length +
                    " must not be more than " + EDD.WMS_MAX_LAYERS + "."); //should be 1.., but allow 0
            //layers.length is at least 1, but it may be ""

            //Styles,  see WMS 1.3.0 section 7.2.4.6.5 and 7.3.3.4
            if (stylesCsv == null) {
                stylesCsv = "";
                //it is required and so should be an Exception, 
                //but http://mapserver.refractions.net/phpwms/phpwms-cvs/ doesn't send it,
                //so treat null as all defaults
                String2.log("WARNING: In the WMS query, STYLES wasn't specified: " + userQuery);
            }
            if (stylesCsv.length() == 0) //shorthand for all defaults
                stylesCsv = String2.makeString(',', layers.length - 1);
            String styles[] = String2.split(stylesCsv, ',');
            if (layers.length != styles.length)
                throw new SimpleException(EDStatic.queryError + "the number of STYLES=" + styles.length +
                    " must equal the number of LAYERS=" + layers.length + ".");

            //CRS or SRS must be present  
            if (crs == null || crs.length() == 0)   //be lenient: default to CRS:84
                crs = "CRS:84";
            if (!crs.equals("CRS:84") && !crs.equals("EPSG:4326")) 
                throw new SimpleException(EDStatic.queryError + 
                    (tVersion.equals("1.1.0") || 
                     tVersion.equals("1.1.1")? 
                    "SRS=" + crs + " must be EPSG:4326." :
                    "SRS=" + crs + " must be EPSG:4326 or CRS:84."));

            //For 1.1.x: BBOX = minx,miny,maxx,maxy         latLonOrder=false
            //For 1.3.0:  (search google for "crs:84 epsg:4326 latitude longitude order")
            //  or to get really confused, see [was http://www.ogcnetwork.net/axisorder ]
            //  for CRS:84    BBOX = minx,miny,maxx,maxy    latLonOrder=false
            //  for EPSG:4326 BBOX = miny,minx,maxy,maxx    latLonOrder=true
            boolean latLonOrder = v130 && crs.equals("EPSG:4326");

            if (bboxCsv == null || bboxCsv.length() == 0)
                throw new SimpleException(EDStatic.queryError + "BBOX must be specified.");
                //bboxCsv = "-180,-90,180,90";  //be lenient, default to full range
            double bbox[] = String2.toDoubleArray(String2.split(bboxCsv, ','));
            if (bbox.length != 4)
                throw new SimpleException(EDStatic.queryError + 
                    "BBOX length=" + bbox.length + " must be 4.");
            double minx = bbox[latLonOrder? 1 : 0];
            double miny = bbox[latLonOrder? 0 : 1];
            double maxx = bbox[latLonOrder? 3 : 2];
            double maxy = bbox[latLonOrder? 2 : 3];
            if (!Double.isFinite(minx) || !Double.isFinite(miny) ||
                !Double.isFinite(maxx) || !Double.isFinite(maxy))
                throw new SimpleException(EDStatic.queryError + 
                    "invalid number in BBOX=" + bboxCsv + ".");
            if (minx >= maxx)
                throw new SimpleException(EDStatic.queryError + 
                    "BBOX minx=" + minx + " must be < maxx=" + maxx + ".");
            if (miny >= maxy)
                throw new SimpleException(EDStatic.queryError + 
                    "BBOX miny=" + miny + " must be < maxy=" + maxy + ".");


            //if request is for JUST a transparent, non-data layer, use a _wms/... cache 
            //  so files can be shared by many datasets and no number of files in dataset dir is reduced
            boolean isNonDataLayer = false;
            if (transparent &&
                (layersCsv.equals("Land") || 
                 layersCsv.equals("LandMask") || 
                 layersCsv.equals("Coastlines") || 
                 layersCsv.equals("LakesAndRivers") || 
                 layersCsv.equals("Nations") ||
                 layersCsv.equals("States"))) {

                isNonDataLayer = true;
                //Land/LandMask not distinguished below, so consolidate images
                if (layersCsv.equals("LandMask"))
                    layersCsv = "Land"; 
                cacheDir = EDStatic.fullWmsCacheDirectory + layersCsv + "/"; 
                fileName = layersCsv + "_" + 
                    String2.md5Hex12(bboxCsv + "w" + width + "h" + height);

            }

            //is the image in the cache?
            if (File2.isFile(cacheDir + fileName + extension)) { 
                //touch nonDataLayer files, since they don't change
                if (isNonDataLayer)
                    File2.touch(cacheDir + fileName + extension);

                //write out the image
                outputStreamSource = new OutputStreamFromHttpResponse(request, response, 
                    fileName, fileTypeName, extension);
                outputStream = outputStreamSource.outputStream("");
                doTransfer(request, response, cacheDir, "_wms/", 
                    fileName + extension, 
                    outputStream, outputStreamSource.usingCompression()); 
                return;
            }
            

            //*** params are basically ok; try to make the map
            //make the image
            BufferedImage bufferedImage = new BufferedImage(width, height, 
                BufferedImage.TYPE_INT_ARGB); //I need opacity "A"
            Graphics g = bufferedImage.getGraphics(); 
            Graphics2D g2 = (Graphics2D)g;
            Color bgColor = new Color(0xFF000000 | bgColori); //0xFF000000 makes it opaque
            g.setColor(bgColor);    
            g.fillRect(0, 0, width, height);  

            //add the layers
            String roles[] = EDStatic.getRoles(loggedInAs);
            LAYER:
            for (int layeri = 0; layeri < layers.length; layeri++) {

                //***deal with non-data layers
                if (layers[layeri].equals(""))
                    continue; 
                if (layers[layeri].equals("Land") || 
                    layers[layeri].equals("LandMask") || 
                    layers[layeri].equals("Coastlines") || 
                    layers[layeri].equals("LakesAndRivers") || 
                    layers[layeri].equals("Nations") ||
                    layers[layeri].equals("States")) {
                    SgtMap.makeCleanMap(minx, maxx, miny, maxy, 
                        false,
                        null, 1, 1, 0, null,
                        layers[layeri].equals("Land") || 
                        layers[layeri].equals("LandMask"), //no need to draw it twice; no distinction here
                        layers[layeri].equals("Coastlines"), 
                        layers[layeri].equals("LakesAndRivers")? 
                            SgtMap.STROKE_LAKES_AND_RIVERS : //stroke (not fill) so, e.g., Great Lakes temp data not obscured by lakeColor
                            SgtMap.NO_LAKES_AND_RIVERS,
                        layers[layeri].equals("Nations"), 
                        layers[layeri].equals("States"),
                        g2, width, height,
                        0, 0, width, height);  
                    //String2.log("WMS layeri="+ layeri + " request was for a non-data layer=" + layers[layeri]);
                    continue;
                }

                //*** deal with grid data
                int spo = layers[layeri].indexOf(EDD.WMS_SEPARATOR);
                if (spo <= 0 || spo >= layers[layeri].length() - 1)
                    throw new SimpleException(EDStatic.queryError + "LAYER=" + layers[layeri] + 
                        " is invalid (invalid separator position).");
                String datasetID = layers[layeri].substring(0, spo);
                String destVar = layers[layeri].substring(spo + 1);
                EDDGrid eddGrid = gridDatasetHashMap.get(datasetID);
                if (eddGrid == null)
                    throw new SimpleException(EDStatic.queryError + "LAYER=" + layers[layeri] + 
                        " is invalid (dataset not found).");
                if (!eddGrid.isAccessibleTo(roles) &&
                    !eddGrid.graphsAccessibleToPublic()) {
                    //WMS: all requests are graphics requests
                    //listPrivateDatasets doesn't apply
                    EDStatic.sendHttpUnauthorizedError(loggedInAs, response, datasetID,
                        false);
                    return;
                }
                if (eddGrid.accessibleViaWMS().length() > 0)
                    throw new SimpleException(EDStatic.queryError + "LAYER=" + layers[layeri] + 
                        " is invalid (not accessible via WMS).");
                int dvi = String2.indexOf(eddGrid.dataVariableDestinationNames(), destVar);
                if (dvi < 0)
                    throw new SimpleException(EDStatic.queryError + "LAYER=" + layers[layeri] + 
                        " is invalid (variable not found).");
                EDV tDataVariable = eddGrid.dataVariables()[dvi];
                if (!tDataVariable.hasColorBarMinMax())
                    throw new SimpleException(EDStatic.queryError + "LAYER=" + layers[layeri] + 
                        " is invalid (variable doesn't have valid colorBarMinimum/Maximum).");

                //style  (currently just the default)
                if (!styles[layeri].equals("") && 
                    !styles[layeri].toLowerCase().equals("default")) { //nonstandard?  but allow it
                    throw new SimpleException(EDStatic.queryError + "for LAYER=" + layers[layeri] + 
                        ", STYLE=" + styles[layeri] + " is invalid (must be \"\").");
                }

                //get other dimension info
                EDVGridAxis ava[] = eddGrid.axisVariables();
                StringBuilder tQuery = new StringBuilder(destVar);
                for (int avi = 0; avi < ava.length; avi++) {
                    EDVGridAxis av = ava[avi];
                    if (avi == eddGrid.lonIndex()) {
                        if (maxx <= av.destinationMin() ||
                            minx >= av.destinationMax()) {
                            if (reallyVerbose) String2.log("  layer=" + layeri + 
                                " rejected because request is out of lon range.");
                            continue LAYER;
                        }
                        int first = av.destinationToClosestSourceIndex(minx);
                        int last = av.destinationToClosestSourceIndex(maxx);
                        if (first > last) {int ti = first; first = last; last = ti;}
                        int stride = DataHelper.findStride(last - first + 1, width);
                        tQuery.append("[" + first + ":" + stride + ":" + last + "]");
                        continue;
                    }

                    if (avi == eddGrid.latIndex()) {
                        if (maxy <= av.destinationMin() ||
                            miny >= av.destinationMax()) {
                            if (reallyVerbose) String2.log("  layer=" + layeri + 
                                " rejected because request is out of lat range.");
                            continue LAYER;
                        }
                        int first = av.destinationToClosestSourceIndex(miny);
                        int last = av.destinationToClosestSourceIndex(maxy);
                        if (first > last) {int ti = first; first = last; last = ti;}
                        int stride = DataHelper.findStride(last - first + 1, height);
                        tQuery.append("[" + first + ":" + stride + ":" + last + "]");
                        continue;
                    }

                    //all other axes
                    String tAvName = 
                        avi == eddGrid.altIndex()? "elevation" :
                        avi == eddGrid.depthIndex()? "elevation" :  //convert depth to elevation
                        avi == eddGrid.timeIndex()? "time" : 
                        "dim_" + ava[avi].destinationName().toLowerCase(); //make it case-insensitive for queryMap.get
                    String tValueS = queryMap.get(tAvName);
                    if (tValueS == null || 
                        (avi == eddGrid.timeIndex() && tValueS.toLowerCase().equals("current")))
                        //default is always the last value
                        tQuery.append("[" + (ava[avi].sourceValues().size() - 1) + "]");
                    else {
                        double tValueD = av.destinationToDouble(tValueS); //needed in particular for iso time -> epoch seconds
                        if (avi == eddGrid.depthIndex())
                            tValueD = -tValueD;
                        if (Double.isNaN(tValueD) ||
                            tValueD < av.destinationCoarseMin() ||
                            tValueD > av.destinationCoarseMax()) {
                            if (reallyVerbose) String2.log("  layer=" + layeri + 
                                " rejected because tValueD=" + tValueD + 
                                " for " + tAvName);
                            continue LAYER;
                        }
                        int first = av.destinationToClosestSourceIndex(tValueD);
                        tQuery.append("[" + first + "]");
                    }
                }

                //get the data
                GridDataAccessor gda = new GridDataAccessor(
                    eddGrid, 
                    "/" + EDStatic.warName + "/griddap/" + datasetID + ".dods", tQuery.toString(), 
                    false, //Grid needs column-major order
                    true); //convertToNaN
                long requestNL = gda.totalIndex().size();
                EDStatic.ensureArraySizeOkay(requestNL, "doWmsGetMap");
                int nBytesPerElement = 8;
                int requestN = (int)requestNL; //safe since checked by ensureArraySizeOkay above
                EDStatic.ensureMemoryAvailable(requestNL * nBytesPerElement, "doWmsGetMap"); 
                Grid grid = new Grid();
                grid.data = new double[requestN];
                int po = 0;
                while (gda.increment()) 
                    grid.data[po++] = gda.getDataValueAsDouble(0);
                grid.lon = gda.axisValues(eddGrid.lonIndex()).toDoubleArray();
                grid.lat = gda.axisValues(eddGrid.latIndex()).toDoubleArray(); 
                gda = null; //free up memory if possible

                //make the palette
                //I checked hasColorBarMinMax above.
                //Note that EDV checks validity of values.
                double minData = tDataVariable.combinedAttributes().getDouble("colorBarMinimum"); 
                double maxData = tDataVariable.combinedAttributes().getDouble("colorBarMaximum"); 
                String palette = tDataVariable.combinedAttributes().getString("colorBarPalette"); 
                if (String2.indexOf(EDStatic.palettes, palette) < 0)
                    palette = Math2.almostEqual(3, -minData, maxData)? "BlueWhiteRed" : "Rainbow"; 
                int nSections = tDataVariable.combinedAttributes().getInt("colorBarNSections"); 
                if (nSections > 100)
                    nSections = -1;
                boolean paletteContinuous = String2.parseBoolean( //defaults to true
                    tDataVariable.combinedAttributes().getString("colorBarContinuous")); 
                String scale = tDataVariable.combinedAttributes().getString("colorBarScale"); 
                if (String2.indexOf(EDV.VALID_SCALES, scale) < 0)
                    scale = "Linear";
                String cptFullName = CompoundColorMap.makeCPT(EDStatic.fullPaletteDirectory, 
                    palette, scale, minData, maxData, nSections, paletteContinuous, 
                    EDStatic.fullCptCacheDirectory);

                //draw the data on the map
                //for now, just cartesian  -- BEWARE: it may be stretched!
                SgtMap.makeCleanMap( 
                    minx, maxx, miny, maxy, 
                    false,
                    grid, 1, 1, 0, cptFullName, 
                    false, false, SgtMap.NO_LAKES_AND_RIVERS, false, false,
                    g2, width, height,
                    0, 0, width, height); 

            }

            //save image as file in cache dir
            //(It saves as temp file, then renames if ok.)
            SgtUtil.saveAsTransparentPng(bufferedImage, 
                transparent? bgColor : null, 
                cacheDir + fileName); 

            //copy image from file to client
            if (reallyVerbose) String2.log("  image created. copying to client: " + fileName + extension);
            outputStreamSource = new OutputStreamFromHttpResponse(request, response, 
                fileName, fileTypeName, extension);
            outputStream = outputStreamSource.outputStream("");
            doTransfer(request, response, cacheDir, "_wms/", 
                fileName + extension, 
                outputStream, outputStreamSource.usingCompression()); 

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            //deal with the WMS error  
            //exceptions in this block fall to error handling in doWms
            
            //catch errors after the response has begun
            if (response.isCommitted()) 
                throw t; //rethrown exception (will be handled in doGet try/catch)

            if (exceptions == null)
                exceptions = "XML";

            //send INIMAGE or BLANK response   
            //see wms 1.3.0 spec sections 6.9, 6.10 and 7.3.3.11
            if ((width > 0 && width <= EDD.WMS_MAX_WIDTH &&
                 height > 0 && height <= EDD.WMS_MAX_HEIGHT &&
                 format != null) &&
                (format.equals("image/png")) &&
                (exceptions.equals("INIMAGE") || exceptions.equals("BLANK"))) {

                //since handled here 
                String msg = MustBe.getShortErrorMessage(t);
                String2.log(String2.ERROR + " message sent to user in image: " + msg);
                String2.log(MustBe.throwableToString(t));

                //make image
                BufferedImage bufferedImage = new BufferedImage(width, height, 
                    BufferedImage.TYPE_INT_ARGB); //I need opacity "A"
                Graphics g = bufferedImage.getGraphics(); 
                Color bgColor = new Color(0xFF000000 | bgColori); //0xFF000000 makes it opaque
                g.setColor(bgColor);
                g.fillRect(0, 0, width, height);  

                //write exception in image   (if not THERE_IS_NO_DATA)
                if (exceptions.equals("INIMAGE") &&
                    msg.indexOf(MustBe.THERE_IS_NO_DATA) < 0) {
                    int tHeight = 12; //pixels high
                    msg = String2.noLongLines(msg, (width * 10 / 6) / tHeight, "    ");
                    String lines[] = msg.split("\\n"); //not String2.split which trims
                    g.setColor(Color.black);
                    g.setFont(new Font(EDStatic.fontFamily, Font.PLAIN, tHeight));
                    int ty = tHeight * 2;
                    for (int i = 0; i < lines.length; i++) {
                        g.drawString(lines[i], tHeight, ty);
                        ty += tHeight + 2;
                    }                    
                } //else BLANK

                //send image to client  (don't cache it)
                
                //if (format.equals("image/png")) { //currently, just .png
                    fileTypeName = ".png";
                //}
                String extension = fileTypeName;  //here, not in other situations
                if (outputStreamSource == null)
                    outputStreamSource = 
                        new OutputStreamFromHttpResponse(request, response, 
                            fileName, fileTypeName, extension);
                if (outputStream == null)
                    outputStream = outputStreamSource.outputStream("");
                SgtUtil.saveAsTransparentPng(bufferedImage, 
                    transparent? bgColor : null, 
                    outputStream); 

                return;
            } 
            
            //fall back to XML Exception in doWMS   rethrow t, so it is caught by doWms XML exception handler
            throw t;
        }

    }

    /**
     * Respond to WMS GetCapabilities request for doWms.
     * To become a Layer, a grid variable must use evenly-spaced longitude and latitude variables.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param tDatasetID  a specific dataset
     * @param queryMap should have lowercase'd names
     */
    public void doWmsGetCapabilities(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String tDatasetID, HashMap<String, String> queryMap) throws Throwable {

        if (!EDStatic.wmsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WMS"));
            return;
        }

        //make sure version is unspecified (latest), 1.1.0, 1.1.1, or 1.3.0.
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String tVersion = queryMap.get("version");
        if (tVersion == null)
            tVersion = "1.3.0";
        if (!tVersion.equals("1.1.0") &&
            !tVersion.equals("1.1.1") &&
            !tVersion.equals("1.3.0"))
            throw new SimpleException("In an ERDDAP WMS getCapabilities query, VERSION=" + tVersion + " is not supported.\n");
        String qm = tVersion.equals("1.1.0") || 
                    tVersion.equals("1.1.1")? "" : "?";  //default for 1.3.0+
        String sc = tVersion.equals("1.1.0") || 
                    tVersion.equals("1.1.1")? "S" : "C";  //default for 1.3.0+
        EDStatic.tally.add("WMS doWmsGetCapabilities (since last daily report)", tDatasetID);
        EDStatic.tally.add("WMS doWmsGetCapabilities (since startup)", tDatasetID);

        //*** describe a Layer for each wms-able data variable in each grid dataset
        //Elements must occur in proper sequence
        boolean firstDataset = true;
        boolean pm180 = true;
        String roles[] = EDStatic.getRoles(loggedInAs);
        EDDGrid eddGrid = gridDatasetHashMap.get(tDatasetID);
        if (eddGrid == null) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.notAvailable, tDatasetID));
            return;
        }
        if (!eddGrid.isAccessibleTo(roles) &&
            !eddGrid.graphsAccessibleToPublic()) {
            //WMS: all requests are graphics requests
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID,
                false);
            return;
        }
        if (eddGrid.accessibleViaWMS().length() > 0) {
            sendResourceNotFoundError(request, response, eddGrid.accessibleViaWMS());
            return;
        }
        int loni = eddGrid.lonIndex();
        int lati = eddGrid.latIndex();
        EDVGridAxis avs[] = eddGrid.axisVariables();


        //return capabilities xml
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "Capabilities", ".xml", ".xml");
        OutputStream out = outSource.outputStream(String2.UTF_8);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
        try {
            String wmsUrl = tErddapUrl + "/wms/" + tDatasetID + "/" + EDD.WMS_SERVER;
            //see the WMS 1.1.0, 1.1.1, and 1.3.0 specification for details 
            //This based example in Annex H.
            if (tVersion.equals("1.1.0"))
                writer.write(
    "<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
    "<!DOCTYPE WMT_MS_Capabilities SYSTEM\n" +
    "  \"http://schemas.opengis.net/wms/1.1.0/capabilities_1_1_0.dtd\" \n" +
    " [\n" +
    " <!ELEMENT VendorSpecificCapabilities EMPTY>\n" +
    " ]>  <!-- end of DOCTYPE declaration -->\n" +
    "<WMT_MS_Capabilities version=\"1.1.0\">\n" +
    "  <Service>\n" +
    "    <Name>GetMap</Name>\n");  
            else if (tVersion.equals("1.1.1"))
                writer.write(
    "<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
    "<!DOCTYPE WMT_MS_Capabilities SYSTEM\n" +
    "  \"http://schemas.opengis.net/wms/1.1.1/capabilities_1_1_1.dtd\" \n" +
    " [\n" +
    " <!ELEMENT VendorSpecificCapabilities EMPTY>\n" +
    " ]>  <!-- end of DOCTYPE declaration -->\n" +
    "<WMT_MS_Capabilities version=\"1.1.1\">\n" +
    "  <Service>\n" +
    "    <Name>OGC:WMS</Name>\n");  
            else if (tVersion.equals("1.3.0"))
                writer.write(
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    //not yet supported: optional updatesequence parameter
    "<WMS_Capabilities version=\"1.3.0\" xmlns=\"http://www.opengis.net/wms\"\n" +
    "    xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n" +
    "    xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n" +
    "    xsi:schemaLocation=\"http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd\">\n" +
    "  <Service>\n" +
    "    <Name>WMS</Name>\n");

            writer.write(
    "    <Title>" + XML.encodeAsXML("WMS for " + eddGrid.title()) + "</Title>\n" +
    "    <Abstract>" + XML.encodeAsXML(eddGrid.summary()) + "</Abstract>\n" + 
    "    <KeywordList>\n");
            String keywords[] = eddGrid.keywords();
            for (int i = 0; i < keywords.length; i++) 
                writer.write(
    "      <Keyword>" + XML.encodeAsXML(keywords[i]) + "</Keyword>\n");
            writer.write(
    "    </KeywordList>\n" +
    "    <!-- Top-level address of service -->\n" +  //spec annex H has "... or service provider"
    "    <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\" xlink:type=\"simple\"\n" +
    "       xlink:href=\"" + wmsUrl + qm + "\" />\n" +
    "    <ContactInformation>\n" +
    "      <ContactPersonPrimary>\n" +
    "        <ContactPerson>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</ContactPerson>\n" +
    "        <ContactOrganization>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</ContactOrganization>\n" +
    "      </ContactPersonPrimary>\n" +
    "      <ContactPosition>" + XML.encodeAsXML(EDStatic.adminPosition) + "</ContactPosition>\n" +
    "      <ContactAddress>\n" +
    "        <AddressType>postal</AddressType>\n" +
    "        <Address>" + XML.encodeAsXML(EDStatic.adminAddress) + "</Address>\n" +
    "        <City>" + XML.encodeAsXML(EDStatic.adminCity) + "</City>\n" +
    "        <StateOrProvince>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</StateOrProvince>\n" +
    "        <PostCode>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</PostCode>\n" +
    "        <Country>" + XML.encodeAsXML(EDStatic.adminCountry) + "</Country>\n" +
    "      </ContactAddress>\n" +
    "      <ContactVoiceTelephone>" + XML.encodeAsXML(EDStatic.adminPhone) + "</ContactVoiceTelephone>\n" +
    "      <ContactElectronicMailAddress>" + XML.encodeAsXML(EDStatic.adminEmail) + "</ContactElectronicMailAddress>\n" +
    "    </ContactInformation>\n" +
    "    <Fees>" + XML.encodeAsXML(eddGrid.fees()) + "</Fees>\n" +
    "    <AccessConstraints>" + XML.encodeAsXML(eddGrid.accessConstraints()) + "</AccessConstraints>\n" +

            (tVersion.equals("1.1.0") || 
             tVersion.equals("1.1.1")? "" :
    "    <LayerLimit>" + EDD.WMS_MAX_LAYERS + "</LayerLimit>\n" +  
    "    <MaxWidth>" + EDD.WMS_MAX_WIDTH + "</MaxWidth>\n" +  
    "    <MaxHeight>" + EDD.WMS_MAX_HEIGHT + "</MaxHeight>\n") +  

    "  </Service>\n");

            //Capability
            writer.write(
    "  <Capability>\n" +
    "    <Request>\n" +
    "      <GetCapabilities>\n" +
    "        <Format>" + (tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? 
                "application/vnd.ogc.wms_xml" : 
                "text/xml") + 
              "</Format>\n" +
    "        <DCPType>\n" +
    "          <HTTP>\n" +
    "            <Get>\n" +
    "              <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\" \n" +
    "                xlink:type=\"simple\" \n" +
    "                xlink:href=\"" + wmsUrl + qm + "\" />\n" +
    "            </Get>\n" +
    "          </HTTP>\n" +
    "        </DCPType>\n" +
    "      </GetCapabilities>\n" +
    "      <GetMap>\n" +
    "        <Format>image/png</Format>\n" +
    //"        <Format>image/jpeg</Format>\n" +
    "        <DCPType>\n" +
    "          <HTTP>\n" +
    "            <Get>\n" +
    "              <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\" \n" +
    "                xlink:type=\"simple\" \n" +
    "                xlink:href=\"" + wmsUrl + qm + "\" />\n" +
    "            </Get>\n" +
    "          </HTTP>\n" +
    "        </DCPType>\n" +
    "      </GetMap>\n" +
    /* GetFeatureInfo is optional; not currently supported.  (1.1.0, 1.1.1 and 1.3.0 vary)
    "      <GetFeatureInfo>\n" +
    "        <Format>text/xml</Format>\n" +
    "        <Format>text/plain</Format>\n" +
    "        <Format>text/html</Format>\n" +
    "        <DCPType>\n" +
    "          <HTTP>\n" +
    "            <Get>\n" +
    "              <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\" \n" +
    "                xlink:type=\"simple\" \n" +
    "                xlink:href=\"" + wmsUrl + qm + "\" />\n" +
    "            </Get>\n" +
    "          </HTTP>\n" +
    "        </DCPType>\n" +
    "      </GetFeatureInfo>\n" +
    */
    "    </Request>\n" +
    "    <Exception>\n");
    if (tVersion.equals("1.1.0") || tVersion.equals("1.1.1")) 
        writer.write(
    "      <Format>application/vnd.ogc.se_xml</Format>\n" +
    "      <Format>application/vnd.ogc.se_inimage</Format>\n" +  
    "      <Format>application/vnd.ogc.se_blank</Format>\n" +
    "    </Exception>\n");
    else writer.write(
    "      <Format>XML</Format>\n" +
    "      <Format>INIMAGE</Format>\n" +  
    "      <Format>BLANK</Format>\n" +
    "    </Exception>\n");

    if (tVersion.equals("1.1.0") || tVersion.equals("1.1.1"))
        writer.write(
    "    <VendorSpecificCapabilities />\n");

    //*** start the outer layer
    writer.write(
    "    <Layer>\n" + 
    "      <Title>" + XML.encodeAsXML(eddGrid.title()) + "</Title>\n");
    //?Authority
    //?huge bounding box?
    //CRS   both CRS:84 and EPSG:4326 are +-180, +-90;     ???other CRSs?
    //(tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? "" : "      <CRS>CRS:84</CRS>\n") +
    //"      <" + sc + "RS>EPSG:4326</" + sc + "RS>\n" +


            //EEEEK!!!! CRS:84 and EPSG:4326 want lon -180 to 180, but many erddap datasets are 0 to 360.
            //That seems to be ok.   But still limit x to -180 to 360.
            //pre 2009-02-11 was limit x to +/-180.
            double safeMinX = Math.max(-180, avs[loni].destinationMin());
            double safeMinY = Math.max( -90, avs[lati].destinationMin());
            double safeMaxX = Math.min( 360, avs[loni].destinationMax());
            double safeMaxY = Math.min(  90, avs[lati].destinationMax());

            //*** firstDataset, describe the LandMask non-data layer 
            if (firstDataset) {
                firstDataset = false;
                pm180 = safeMaxX < 181; //crude
                addWmsNonDataLayer(writer, tVersion, 0, 0, pm180); 
            }

            //Layer for the dataset
            //Elements are in order of elements described in spec.
            writer.write(
       "      <Layer>\n" +
       "        <Title>" + XML.encodeAsXML(eddGrid.title()) + "</Title>\n" +

            //?optional Abstract and KeywordList

            //Style: WMS 1.3.0 section 7.2.4.6.5 says "If only a single style is available, 
            //that style is known as the "default" style and need not be advertised by the server."
            //See also 7.3.3.4.
            //I'll go with that. It's simple.
            //???separate out different palettes?
       //"      <Style>\n" +
       //         //example: Default, Transparent    features use specific colors, e.g., LightBlue, Brown
       //"        <Name>Transparent</Name>\n" +
       //"        <Title>Transparent</Title>\n" +
       //"      </Style>\n" +

       //CRS   both CRS:84 and EPSG:4326 are +-180, +-90;     ???other CRSs?

       (tVersion.equals("1.1.0")? "        <SRS>EPSG:4326</SRS>\n" : // >1? space separate them
        tVersion.equals("1.1.1")? "        <SRS>EPSG:4326</SRS>\n" : // >1? use separate tags
            "        <CRS>CRS:84</CRS>\n" +
            "        <CRS>EPSG:4326</CRS>\n") + //1.3.0

       (tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? 
       "        <LatLonBoundingBox " +
                   "minx=\"" + safeMinX + "\" " +
                   "miny=\"" + safeMinY + "\" " +
                   "maxx=\"" + safeMaxX + "\" " +
                   "maxy=\"" + safeMaxY + "\" " +
                   "/>\n" :
       "        <EX_GeographicBoundingBox>\n" + //1.3.0
                   //EEEEK!!!! CRS:84 and EPSG:4326 want lon -180 to 180, but many erddap datasets are 0 to 360.
                   //That seems to be ok.   But still limit x to -180 to 360.
                   //pre 2009-02-11 was limit x to +/-180.
       "          <westBoundLongitude>" + safeMinX + "</westBoundLongitude>\n" +
       "          <eastBoundLongitude>" + safeMaxX + "</eastBoundLongitude>\n" +
       "          <southBoundLatitude>" + safeMinY + "</southBoundLatitude>\n" +
       "          <northBoundLatitude>" + safeMaxY + "</northBoundLatitude>\n" +
       "        </EX_GeographicBoundingBox>\n") +

       "        <BoundingBox " + sc + "RS=\"EPSG:4326\" " +
                "minx=\"" + safeMinX + "\" " +
                "miny=\"" + safeMinY + "\" " +
                "maxx=\"" + safeMaxX + "\" " +
                "maxy=\"" + safeMaxY + "\" " +
                (avs[loni].isEvenlySpaced()? "resx=\"" + Math.abs(avs[loni].averageSpacing()) + "\" " : "") +
                (avs[lati].isEvenlySpaced()? "resy=\"" + Math.abs(avs[lati].averageSpacing()) + "\" " : "") +
                "/>\n");

            //???AuthorityURL

            //?optional MinScaleDenominator and MaxScaleDenominator

            //for 1.1.0 and 1.1.1, make a <Dimension> for each non-lat lon dimension
            // so all <Dimension> elements are together
            if (tVersion.equals("1.1.0") || tVersion.equals("1.1.1")) {
                for (int avi = 0; avi < avs.length; avi++) {
                    if (avi == loni || avi == lati)
                        continue;
                    EDVGridAxis av = avs[avi];
                    String avName = av.destinationName();
                    String avUnits = av.units() == null? "" : av.units(); //"" is required by spec if not known (C.2)
                    //required by spec (C.2)
                    if (avi == eddGrid.timeIndex()) {
                        avName = "time";      
                        avUnits = "ISO8601"; 
                    } else if (avi == eddGrid.altIndex() || avi == eddGrid.depthIndex())  {
                        avName = "elevation"; 
                        //???is CRS:88 the most appropriate  (see spec 6.7.5 and B.6)
                        //"EPSG:5030" means "meters above the WGS84 ellipsoid."
                        avUnits = "EPSG:5030"; //here just 1.1.0 or 1.1.1
                    } else if (EDStatic.units_standard.equals("UDUNITS")) {
                        //convert other udnits to ucum   (this is in WMS GetCapabilities)
                        avUnits = EDUnits.safeUdunitsToUcum(avUnits);
                    }

                    writer.write(
           "        <Dimension name=\"" + avName + "\" " +
                        "units=\"" + avUnits + "\" />\n");
                }
            }


            //the values for each non-lat lon dimension   
            //  for 1.3.0, make a <Dimension>
            //  for 1.1.0 and 1.1.1, make a <Extent> 
            for (int avi = 0; avi < avs.length; avi++) {
                if (avi == loni || avi == lati)
                    continue;
                EDVGridAxis av = avs[avi];
                String avName = av.destinationName();
                String avUnits = av.units() == null? "" : av.units(); //"" is required by spec if not known (C.2)
                String unitSymbol = "";
                String defaultValue = av.destinationToString(av.lastDestinationValue());
                //required by spec (C.2)
                if (avi == eddGrid.timeIndex()) {
                    avName = "time";      
                    avUnits = "ISO8601"; 
                } else if (avi == eddGrid.altIndex() || avi == eddGrid.depthIndex())  {
                    avName = "elevation"; 
                    //???is CRS:88 the most appropriate  (see spec 6.7.5 and B.6)
                    //"EPSG:5030" means "meters above the WGS84 ellipsoid."
                    avUnits = tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? "EPSG:5030" : "CRS:88"; 
                    unitSymbol = "unitSymbol=\"m\" "; 
                    defaultValue = av.destinationToString(
                        (avi == eddGrid.depthIndex()? -1 : 1) * av.lastDestinationValue());
                } else if (EDStatic.units_standard.equals("UDUNITS")) {
                    //convert other udnits to ucum (this is in WMS GetCapabilites)
                    avUnits = EDUnits.safeUdunitsToUcum(avUnits);
                }

                if (tVersion.equals("1.1.0")) writer.write(
       "        <Extent name=\"" + avName + "\" ");
    //???nearestValue is important --- validator doesn't like it!!! should be allowed in 1.1.0!!!
    //It is described in OGC 01-047r2, section C.3
    //  but if I look in 1.1.0 GetCapabilities DTD from http://schemas.opengis.net/wms/1.1.0/capabilities_1_1_0.dtd
    //  and look at definition of Extent, there is no mention of multipleValues or nearestValue.
    //2008-08-22 I sent email to revisions@opengis.org asking about it
    //                    "multipleValues=\"0\" " +  //don't allow request for multiple values    
    //                    "nearestValue=\"1\" ");   //do find nearest value                      

                else if (tVersion.equals("1.1.1")) writer.write(
       "        <Extent name=\"" + avName + "\" " +
                    "multipleValues=\"0\" " +  //don't allow request for multiple values    
                    "nearestValue=\"1\" ");   //do find nearest value                      

                else writer.write( //1.3.0+
       "        <Dimension name=\"" + avName + "\" " +
                    "units=\"" + avUnits + "\" " +
                    unitSymbol +
                    "multipleValues=\"0\" " +  //don't allow request for multiple values    
                    "nearestValue=\"1\" ");   //do find nearest value                       

                writer.write(
                    "default=\"" + defaultValue +  "\" " + //default is last value
                    //!!!currently, no support for "current" since grid av doesn't have that info to identify if relevant
                    //???or just always use last value is "current"???
                    ">");

                 //extent value(s)
                 if (avi == eddGrid.depthIndex()) {
                     //convert depth to elevation
                     PrimitiveArray elevValues = (PrimitiveArray)av.destinationValues().clone();
                     elevValues.scaleAddOffset(-1, 0);
                     if (elevValues.size() > 2 && av.isEvenlySpaced()) {
                         //min/max/spacing     
                         writer.write(elevValues.getString(0) + "/" + 
                           elevValues.getString(elevValues.size() - 1) + "/" + 
                           Math.abs(av.averageSpacing()));
                     } else { 
                         //1 or many (not evenly spaced)
                         writer.write(elevValues.toCSVString()); 
                     }

                 } else if (avi != eddGrid.timeIndex() && 
                            av.sourceValues().size() > 2 && av.isEvenlySpaced()) {
                     //non-time min/max/spacing     
                     writer.write(av.destinationMinString() + "/" + 
                         av.destinationMaxString() + "/" + Math.abs(av.averageSpacing()));
                     //time min/max/spacing (time always done via iso strings)
                     //!!??? For time, express averageSpacing as ISO time interval, e.g., P1D
                     //Forming them is a little complex, so defer doing it.
                 } else {
                     //csv values   (times as iso8601)
                     // !!!For time, if lots of values (e.g., 10^6), this is SLOW (e.g., 30 seconds)!!!
                     writer.write(av.destinationStringValues().toCSVString());
                 }

                if (tVersion.equals("1.1.0") || tVersion.equals("1.1.1"))
                    writer.write("</Extent>\n");
                else //1.3.0+
                    writer.write("</Dimension>\n");
            }

            //?optional MetadataURL   needs to be in standard format (e.g., fgdc)

            writer.write(
       "        <Attribution>\n" +
       "          <Title>" + XML.encodeAsXML(eddGrid.institution()) + "</Title>\n" +
       "          <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n" +
       "            xlink:type=\"simple\"\n" +
       "            xlink:href=\"" + XML.encodeAsXML(eddGrid.infoUrl()) + "\" />\n" +
                //LogoURL
       "        </Attribution>\n");

            //?optional Identifier and AuthorityURL
            //?optional FeatureListURL
            //?optional DataURL (tied to a MIME type)
            //?optional LegendURL

    /*
            //gather all of the av destinationStringValues
            StringArray avDsv[] = new StringArray[avs.length];
            for (int avi = 0; avi < avs.length; avi++) {
                if (avi == loni || avi == lati)
                    continue;
                // !!!For time, if lots of values (e.g., 10^6), this is SLOW (e.g., 30 seconds)!!!
                avDsv[avi] = avs[avi].destinationStringValues();
            }

    */

            //an inner Layer for each dataVariable
            String dvNames[] = eddGrid.dataVariableDestinationNames();
            for (int dvi = 0; dvi < dvNames.length; dvi++) {
                if (!eddGrid.dataVariables()[dvi].hasColorBarMinMax())
                    continue;
                writer.write(
       "        <Layer opaque=\"1\" >\n" + //see 7.2.4.7.1  use opaque for grid data, non for table data
       "          <Name>" + XML.encodeAsXML(tDatasetID + EDD.WMS_SEPARATOR + dvNames[dvi]) + "</Name>\n" +
       "          <Title>" + XML.encodeAsXML(eddGrid.title() + " - " + dvNames[dvi]) + "</Title>\n");
    /*

                //make a sublayer for each index combination  !!!???          
                NDimensionalIndex ndi = new NDimensionalIndex( shape[]);
                int current[] = ndi.getCurrent();
                StringBuilder dims = new StringBuilder();
                while (ndi.increment()) {
                    //make the dims string, e.g., !time:2006-08-23T12:00:00Z!elevation:0
                    dims.setLength(0);
                    for (int avi = 0; avi < avs.length; avi++) {
                        if (avi == loni || avi == lati)
                            continue;
                        // !!!For time, if lots of values (e.g., 10^6), this is SLOW (e.g., 30 seconds)!!!
                        dims.append(EDD.WMS_SEPARATOR +  ...currentavDsv[avi] = avs[avi].destinationStringValues();
                    }
                    writer.write
                        "<Layer opaque=\"1\" >\n" + //see 7.2.4.7.1  use opaque for grid data, non for table data
                        "  <Name>" + XML.encodeAsXML(tDatasetID + EDD.WMS_SEPARATOR + dvNames[dvi]) + dims + "</Name>\n" +
                        "  <Title>" + XML.encodeAsXML(eddGrid.title() + " - " + dvNames[dvi]) + dims + "</Title>\n");
                        "        </Layer>\n");

                }

    */
                writer.write(
       "        </Layer>\n");
            }

            //end of the dataset's layer
            writer.write(
       "      </Layer>\n");        
            

            //*** describe the non-data layers   Land, Coastlines, LakesAndRivers, Nations, States
            addWmsNonDataLayer(writer, tVersion, 0, 4, pm180); 

            //*** end of the outer layer
            writer.write(
            "    </Layer>\n");        

            writer.write(
           "  </Capability>\n" +
           (tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? 
                "</WMT_MS_Capabilities>\n" : 
                "</WMS_Capabilities>\n"));

            //essential
            writer.flush();
            if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
        } finally {
            writer.close(); 
        }
    }


    /** 
     * Add a non-data layer to the writer's GetCapabilities:  
     *  0=Land/LandMask, 1=Coastlines, 2=LakesAndRivers, 3=Nations, 4=States
     */
    private static void addWmsNonDataLayer(Writer writer, String tVersion, 
        int first, int last, boolean pm180) throws Throwable {

        //Elements must occur in proper sequence
        String firstName = first == last && first == 0? "Land" : "LandMask";
        String names[]  = {firstName, "Coastlines", "LakesAndRivers",   "Nations",             "States"};
        String titles[] = {firstName, "Coastlines", "Lakes and Rivers", "National Boundaries", "State Boundaries"};
        String sc = tVersion.equals("1.1.0") || 
                    tVersion.equals("1.1.1")? "S" : "C";  //default for 1.3.0+
        double safeMinX = pm180? -180 : 0;
        double safeMaxX = pm180?  180 : 360;

        for (int layeri = first; layeri <= last; layeri++) {
            writer.write(
"      <Layer" +
     (tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? "" : 
         " opaque=\"" + (layeri == 0? 1 : 0) + "\"") + //see 7.2.4.7.1  use opaque for coverages
     ">\n" + 
"        <Name>"  +  names[layeri] + "</Name>\n" +
"        <Title>" + titles[layeri] + "</Title>\n" +
//?optional Abstract and KeywordList
//don't have to define style if just one

//CRS   both CRS:84 and EPSG:4326 are +-180, +-90;     ???other CRSs?
(tVersion.equals("1.1.0")? "        <SRS>EPSG:4326</SRS>\n" : // >1? space separate them
 tVersion.equals("1.1.1")? "        <SRS>EPSG:4326</SRS>\n" : // >1? use separate tags
     "        <CRS>CRS:84</CRS>\n" +
     "        <CRS>EPSG:4326</CRS>\n") +

(tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? 
"        <LatLonBoundingBox minx=\"" + safeMinX + "\" miny=\"-90\" maxx=\"" + safeMaxX + "\" maxy=\"90\" />\n" :
//1.3.0
"        <EX_GeographicBoundingBox>\n" + 
"          <westBoundLongitude>" + safeMinX + "</westBoundLongitude>\n" +
"          <eastBoundLongitude>" + safeMaxX + "</eastBoundLongitude>\n" +
"          <southBoundLatitude>-90</southBoundLatitude>\n" +
"          <northBoundLatitude>90</northBoundLatitude>\n" +
"        </EX_GeographicBoundingBox>\n") +

"        <BoundingBox " + sc + "RS=\"EPSG:4326\" minx=\"" + safeMinX + 
      "\" miny=\"-90\" maxx=\"" + safeMaxX + "\" maxy=\"90\" />\n" +

//???AuthorityURL
//?optional MinScaleDenominator and MaxScaleDenominator
//?optional MetadataURL   needs to be in standard format (e.g., fgdc)

"        <Attribution>\n" +
"          <Title>" + XML.encodeAsXML(layeri < 2? "NOAA NOS GSHHS" : "pscoast in GMT") + "</Title> \n" +
"          <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\" \n" +
"            xlink:type=\"simple\" \n" +
"            xlink:href=\"" + 
    (layeri < 2? "https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html" : 
                "https://gmt.soest.hawaii.edu/") + 
    "\" />\n" +
         //LogoURL
"        </Attribution>\n" +

//?optional Identifier and AuthorityURL
//?optional FeatureListURL
//?optional DataURL (tied to a MIME type)
//?optional LegendURL

"      </Layer>\n");        
        }
    }


    /**
     * This responds by sending out the /wms/datasetID/index.html (or 111 or 130) page
     * which uses Leaflet as the WMS client.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param tVersion the WMS version to use: "1.1.0", "1.1.1" or "1.3.0"
     * @param tDatasetID  currently must be an EDDGrid datasetID, e.g., erdBAssta5day   
     */
    public void doWmsDemo(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String tVersion, String tDatasetID) throws Throwable {

        if (!EDStatic.wmsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "WMS"));
            return;
        }
        boolean wmsClientActive = EDStatic.wmsClientActive;

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        if (!tVersion.equals("1.1.0") &&
            !tVersion.equals("1.1.1") &&
            !tVersion.equals("1.3.0"))
            throw new SimpleException("WMS version=" + tVersion + " must be " +
                "1.1.0, 1.1.1, or 1.3.0.");            
        EDStatic.tally.add("WMS doWmsDemo (since last daily report)", tDatasetID);
        EDStatic.tally.add("WMS doWmsDemo (since startup)", tDatasetID);

        String tWmsSampleBBox = tVersion.equals("1.3.0")? 
            EDStatic.wmsSampleBBox130 : EDStatic.wmsSampleBBox110;
        String csrs = tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? "srs" : "crs";
        String exceptions = tVersion.equals("1.1.0") || tVersion.equals("1.1.1")? 
            "" :  //default is ok for 1.1.0 and 1.1.1
            "exceptions:'INIMAGE', "; 

        EDDGrid eddGrid = gridDatasetHashMap.get(tDatasetID);
        if (eddGrid == null) {
            sendResourceNotFoundError(request, response, 
                "datasetID=" + tDatasetID + " is currently unavailable.");
            return;
        }
        if (!eddGrid.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
            !eddGrid.graphsAccessibleToPublic()) { 
            //WMS: all requests are graphics requests
            //listPrivateDatasets doesn't apply
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID,
                false);
            return;
        }
        int loni = eddGrid.lonIndex();
        int lati = eddGrid.latIndex();
        int alti = eddGrid.altIndex();
        int depthi = eddGrid.depthIndex();
        int timei = eddGrid.timeIndex();
        if (loni < 0 || lati < 0) 
            throw new SimpleException("datasetID=" + tDatasetID + 
                " doesn't have longitude and latitude dimensions.");            
        if (eddGrid.accessibleViaWMS().length() > 0)
            throw new SimpleException(eddGrid.accessibleViaWMS());            

        EDVGridAxis gaa[] = eddGrid.axisVariables();
        EDV dva[] = eddGrid.dataVariables();
        String options[][] = new String[gaa.length][];
        String tgaNames[] = new String[gaa.length];
        boolean hasNonLatLonAxes = false;
        for (int gai = 0; gai < gaa.length; gai++) {
            if (gai == loni || gai == lati)
                continue;
            hasNonLatLonAxes = true;
            PrimitiveArray tpa;
            if (gai == timei) {
                // !!!For time, if lots of values (e.g., 10^6), this is SLOW!!!
                tpa = gaa[gai].destinationStringValues(); //ISO 8601
            } else {
                tpa = (PrimitiveArray)gaa[gai].destinationValues().clone();
                if (gai == depthi) 
                    tpa.scaleAddOffset(-1, 0); //convert depth to elevation
                tpa.sort(); //people want + button to increase values and - button to decrease
            }
            options[gai] = tpa.toStringArray();
            tgaNames[gai] = 
                gai == alti?   "elevation" :
                gai == depthi? "elevation" : //convert to elevation
                gai == timei?  "time" : 
                "dim_" + gaa[gai].destinationName();
        }
        String baseUrl = tErddapUrl + "/wms/" + tDatasetID;
        String requestUrl = baseUrl + "/" + EDD.WMS_SERVER;
      
        StringArray varNamesWCB = new StringArray();
        for (int var = 0; var < eddGrid.dataVariables().length; var++) {
            EDV edv = eddGrid.dataVariables()[var];
            if (edv.hasColorBarMinMax())
                varNamesWCB.add(edv.destinationName());
        }
        boolean thisWmsClientActive = wmsClientActive && 
            varNamesWCB.size() > 0;

        double minX = gaa[loni].destinationMin();
        double maxX = gaa[loni].destinationMax();
        double minY = gaa[lati].destinationMin();
        double maxY = gaa[lati].destinationMax();
        double xRange = Math.abs(maxX - minX);
        double yRange = Math.abs(maxY - minY);
        //https://leafletjs.com/examples/zoom-levels/
        //zoom 0 is whole earth, zoom 1 is 2x2 images, zoom 2 is 4x4 images, ...
        //so each level is 2x the previous level
        double frac = Math.max( //the larger edge
            Math.max(xRange, 0.1) / 360.0,
            Math.max(yRange, 0.1) / 180.0);
        double tFrac = 1;
        int zoom = 0;
        while (tFrac / 2 > frac) {
            zoom++;
            tFrac /= 2;
        }
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        boolean pm180 = centerX < 90;
        StringBuilder scripts = new StringBuilder();
        if (thisWmsClientActive) {
scripts.append(
// Leaflet example: https://leafletjs.com/examples/wms/wms.html
// my sample:  /projects/leaflet/sample.html
"<script>\n" +
"  var map = L.map('map', {\n" +
"    center: [" + centerY + ", " + centerX + "],\n" +  //lat, lon
"    crs: L.CRS.EPSG4326,\n" +
"    zoom: " + zoom + "\n" +
"  });\n" +
"\n" +
"  var basemaps = {\n");

for (int v = 0; v < varNamesWCB.size(); v++) {
scripts.append(
"    " + varNamesWCB.get(v) + ": L.tileLayer.wms(\n" +
"      '" + requestUrl + "?', {\n" +
"      attribution: '" + eddGrid.institution() + "',\n" +
"      bgcolor: '0x808080',\n" +
"      crs: L.CRS.EPSG4326,\n" +
"      format: 'image/png',\n" +
"      layers: '" + tDatasetID + ":" + varNamesWCB.get(v) + "',\n" +
"      styles: '',\n");
  for (int gai = 0; gai < gaa.length; gai++) {
      if (tgaNames[gai] == null)
          continue;
      scripts.append(
"      " + tgaNames[gai] + ": '" + options[gai][options[gai].length - 1] + "',\n"); 
  }
scripts.append(
"      transparent: true,\n" +
"      version: '" + tVersion + "'})" + (v < varNamesWCB.size() - 1? "," : "") + "\n");
}

scripts.append(
"  }\n" +
"\n" +
"  var overlays = {\n");

StringArray olNames = StringArray.fromCSV(
    "Land, Coastlines, LakesAndRivers" +
    (EDStatic.politicalBoundariesActive? ", Nations, States" : ""));
for (int i = 0; i < olNames.size(); i++) 
scripts.append(
"    " + olNames.get(i) + ": L.tileLayer.wms(\n" +
"      '" + requestUrl + "?', {\n" +
"      bgcolor: '0x808080',\n" +
"      crs: L.CRS.EPSG4326,\n" +
"      format: 'image/png',\n" +
"      layers: '" + olNames.get(i) + "',\n" +
"      styles: '',\n" +
"      transparent: true,\n" +
"      version: '" + tVersion + "'})" + (i < olNames.size() - 1? "," : "") + "\n");

scripts.append(
"  };\n" +
"\n" +
"  L.control.layers(basemaps, overlays, {}).addTo(map);\n" +
"\n" +  //the default visible layers:
"  basemaps." + varNamesWCB.get(0) + ".addTo(map);\n" +
"  overlays.Coastlines.addTo(map);\n" +
"  overlays.LakesAndRivers.addTo(map);\n" +
(EDStatic.politicalBoundariesActive? 
"  overlays.Nations.addTo(map);\n" : "") +
"</script>\n");
    }  

        //*** html head
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        try {
            writer.write(EDStatic.startHeadHtml(tErddapUrl, eddGrid.title() + " - WMS"));
            writer.write("\n" + eddGrid.rssHeadLink());
            if (thisWmsClientActive)
                writer.write(HtmlWidgets.leafletHead(tErddapUrl));
            writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
            writer.write(
                "</head>\n");

            //*** html body
            String makeAGraphRef = "<a href=\"" + tErddapUrl + "/griddap/" + tDatasetID + ".graph\">" +
                EDStatic.mag + "</a>";
            writer.write(
                EDStatic.startBodyHtml(loggedInAs) + "\n" +
                HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)) +
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "wms", tDatasetID));
            String queryString = request.getQueryString();
            if (queryString == null)
                queryString = "";
            eddGrid.writeHtmlDatasetInfo(loggedInAs, writer, true, true, true, true, 
                queryString, "");
            if (!wmsClientActive) {
                writer.write("\n<p><span class=\"warningColor\">" +
                    MessageFormat.format(EDStatic.noXxxBecause, "Leaflet", 
                        MessageFormat.format(EDStatic.noXxxNotActive, "Leaflet")) + "</span>\n\n");
            } else if (!thisWmsClientActive) {
                writer.write("\n<p><span class=\"warningColor\">" +
                    MessageFormat.format(EDStatic.noXxx, "Leaflet") + "</span>\n\n");
            } else {
                //write all the leaflet stuff
                writer.write(HtmlWidgets.ifJavaScriptDisabled + "\n");
                HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs));
                writer.write(
                    "<br>" +
                    String2.replaceAll( //these are actually Leaflet instructions
                        String2.replaceAll(EDStatic.wmsInstructions, "&wmsVersion;", tVersion),
                        "&erddapUrl;", tErddapUrl));
                StringBuilder tAxisConstraintsSB = new StringBuilder();
                if (hasNonLatLonAxes) {
                    widgets.formName = "f1";
                    writer.write("\n" +
                        "<form name=\"f1\" action=\"" + HtmlWidgets.JAVASCRIPT_VOID + "\">\n" + //no action
                        "<table class=\"compact\">\n");

                    //a select widget for each axis (but not for lon or lat)               
                    for (int gai = 0; gai < gaa.length; gai++) {
                        if (gai == loni || gai == lati) {
                            tAxisConstraintsSB.append("[]");
                            continue;
                        }
                        int nOptions = options[gai].length;
                        int nOptionsM1 = nOptions - 1;
                        writer.write(   
                        "  <tr>\n" +
                        "    <td>" + tgaNames[gai] + ":&nbsp;</td>\n" + //2012-12-28 was gaa[gai].destinationName()
                        "    <td>");

                        if (nOptions <= 1) {
                            //one value: display it
                            writer.write(options[gai][0]); //numeric or time so don't need XML.encodeAsHTML

                        } else {
                            //many values: select
                            writer.write(widgets.select(
                                tgaNames[gai], "",  //tooltip
                                widgets.BUTTONS_0n + widgets.BUTTONS_1 + 
                                    (options[gai].length <  110? 0 : widgets.BUTTONS_100) +
                                    (options[gai].length < 1100? 0 : widgets.BUTTONS_1000),  
                                options[gai], 
                                options[gai].length - 1, //numeric or time so don't need XML.encodeAsHTML
                                "onChange='for (key in basemaps) basemaps[key].setParams({" + tgaNames[gai] + ": this.value});'", 
                                false, // encodeSpaces, 
                                " sel.onchange();")); // buttonJS
                        }
                        tAxisConstraintsSB.append("[(" + options[gai][options[gai].length - 1]+ ")]"); //this works with depthi and all other axes

                        writer.write(
                        "    </td>\n" +
                        "  </tr>\n");
                    } //end of gai loop
                    //System.out.println(">> tAxisConstraints=" + tAxisConstraintsSB.toString());

                    writer.write(
                        "</table>\n" +
                        "</form>\n");
                } else {
                    writer.write("&nbsp;(none for this dataset)\n<br>");
                }

                //the div for the map
                writer.write(
                    "&nbsp;\n" + //necessary for the blank line before div (not <p>)
                    "<div id=\"map\" style=\"width:600px; height:400px;\" ></div>\n");

                //legend for each data var with colorbar info
                String tAxisConstraints = SSR.minimalPercentEncode(tAxisConstraintsSB.toString());
                for (int v = 0; v < varNamesWCB.size(); v++) {
                    writer.write("<p><img src=\"" + XML.encodeAsHTMLAttribute(tErddapUrl + 
                            "/griddap/" + tDatasetID + ".png?" + varNamesWCB.get(v) + 
                            tAxisConstraints + "&.legend=Only") +
                        "\" alt=\"The " + varNamesWCB.get(v) + " legend.\" title=\"The " + 
                        varNamesWCB.get(v) + " legend. This colorbar is always relevant for " +
                        varNamesWCB.get(v) + ", even if the other settings don't match.\">\n");
                }
            }

            //flush
            writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better

            //*** What is WMS? 
            String e0 = tErddapUrl + "/wms/" + EDStatic.wmsSampleDatasetID + "/" + EDD.WMS_SERVER + "?";
            String ec = "service=WMS&#x26;request=GetCapabilities&#x26;version=";
            String e1 = "service=WMS&#x26;version="; 
            //this section of code is in 2 places
            int bbox[] = String2.toIntArray(String2.split(EDStatic.wmsSampleBBox110, ',')); //extract info from 110 version
            int tHeight = Math2.roundToInt(((bbox[3] - bbox[1]) * 360) / Math.max(1, bbox[2] - bbox[0]));
            tHeight = Math2.minMaxDef(10, 600, 180, tHeight);
            String e2 = "&#x26;request=GetMap&#x26;bbox=" + tWmsSampleBBox +
                        "&#x26;" + csrs + "=EPSG:4326&#x26;width=360&#x26;height=" + tHeight + 
                        "&#x26;bgcolor=0x808080&#x26;layers=";
            //Land,erdBAssta5day:sst,Coastlines,LakesAndRivers,Nations,States
            String e3 = EDStatic.wmsSampleDatasetID + EDD.WMS_SEPARATOR + EDStatic.wmsSampleVariable;
            String e4 = "&#x26;styles=&#x26;format=image/png";
            String et = "&#x26;transparent=TRUE";

            String tWmsOpaqueExample      = e0 + e1 + tVersion + e2 + "Land," + e3 + ",Coastlines,Nations" + e4;
            String tWmsTransparentExample = e0 + e1 + tVersion + e2 +           e3 + e4 + et;
            String datasetListRef = 
                "  See the\n" +
                "  <a href=\"" + tErddapUrl + "/wms/index.html?" + 
                    EDStatic.encodedDefaultPIppQuery + "\">list \n" +
                "    of datasets available via WMS</a> at this ERDDAP installation.\n";
            String makeAGraphListRef =
                "  See the\n" +
                "    <a rel=\"contents\" href=\"" + tErddapUrl + "/info/index.html?" +
                    EDStatic.encodedDefaultPIppQuery + "\">list \n" +
                "      of datasets with Make A Graph</a> at this ERDDAP installation.\n";

            //What is WMS?   (for tDatasetID) 
            //!!!see the almost identical documentation above
            String wmsUrl = tErddapUrl + "/wms/" + tDatasetID + "/" + EDD.WMS_SERVER + "?";
            String capUrl = wmsUrl + "service=WMS&#x26;request=GetCapabilities&#x26;version=" + tVersion;
            writer.write(
                "<h2><a class=\"selfLink\" id=\"description\" href=\"#description\" rel=\"bookmark\">What</a> is WMS?</h2>\n" +
                String2.replaceAll(EDStatic.wmsLongDescriptionHtml, "&erddapUrl;", tErddapUrl) + "\n" +
                datasetListRef +
                "\n" +
                "<h2>Three Ways to Make Maps with WMS</h2>\n" +
                "<ol>\n" +
                "<li> <strong>In theory, anyone can download, install, and use WMS client software.</strong>\n" +
                "  <br>Some clients are: \n" +
                "    <a href=\"https://www.esri.com/software/arcgis/\">ArcGIS" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> and\n" +
                "    <a href=\"http://udig.refractions.net//\">uDig" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>. \n" +
                "  To make a client work, you would install the software on your computer.\n" +
                "  Then, you would enter the URL of the WMS service into the client.\n" +
                "  For example, in ArcGIS (not yet fully working because it doesn't handle time!), use\n" +
                "  <br>\"Arc Catalog : Add Service : Arc Catalog Servers Folder : GIS Servers : Add WMS Server\".\n" +
                "  In ERDDAP, this dataset has its own WMS service, which is located at\n" +
                "  <br>" + wmsUrl + "\n" +  
                "  <br>(Some WMS client programs don't want the <strong>?</strong> at the end of that URL.)\n" +
                datasetListRef +
                "  <p><strong>In practice,</strong> we haven't found any WMS clients that properly handle dimensions\n" +
                "  other than longitude and latitude (e.g., time), a feature which is specified by the WMS\n" +
                "  specification and which is utilized by most datasets in ERDDAP's WMS servers.\n" +
                "  You may find that using\n" +
                makeAGraphRef + "\n" +
                "    and selecting the .kml file type (an OGC\n" +
                "  standard) to load images into <a href=\"https://www.google.com/earth/\">Google Earth" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a> provides\n" +            
                "     a good (non-WMS) map client.\n" +
                makeAGraphListRef +
                "<li> <strong>Web page authors can embed a WMS client in a web page.</strong>\n" +
                "  <br>For the map above, ERDDAP is using \n" +
                "    <a rel=\"bookmark\" href=\"https://leafletjs.com\">Leaflet" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>, which is a very versatile WMS client.\n" +
                "  Leaflet doesn't automatically deal with dimensions other than longitude and latitude\n" +            
                "  (e.g., time), so you will have to write JavaScript (or other scripting code) to do that.\n" +
                "  (Adventurous JavaScript programmers can look at the Souce Code for this web page.)\n" + 
                "  Another commonly used JavaScript WMS client is\n" +
                "    <a rel=\"bookmark\" href=\"https://openlayers.org/\">OpenLayers" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "  <br>&nbsp;\n" +
                "<li> <strong>A person with a browser or a computer program can generate special WMS URLs.</strong>\n" +
                "  <br>For example,\n" +
                "  <ul>\n" +
                "  <li>To get the Capabilities XML file, use\n" +
                "    <br><a href=\"" + capUrl + "\">" + capUrl + "</a>\n" +
                "  <li>To get an image file with a map with an opaque background, use\n" +
                "    <br><a href=\"" + tWmsOpaqueExample + "\">" + 
                                       tWmsOpaqueExample + "</a>\n" +
                "  <li>To get an image file with a map with a transparent background, use\n" +
                "    <br><a href=\"" + tWmsTransparentExample + "\">" + 
                                       tWmsTransparentExample + "</a>\n" +
                "  </ul>\n" +
                datasetListRef +
                "  <br><strong>For more information about generating WMS URLs, see \n" +
                "    <a rel=\"help\" href=\"" +tErddapUrl + "/wms/documentation.html\">ERDDAP's WMS Documentation</a> .</strong>\n" +
                "  <p><strong>In practice, it is easier, more versatile,\n" +
                "    and more efficient to use this dataset's\n" +
                "    " + makeAGraphRef + " web page</strong>\n" +
                "  than to use WMS for this purpose.\n" +
                makeAGraphListRef +
                "</ol>\n" +
                "\n");
           

            writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
            writer.write(scripts.toString());

            writer.write("</div>\n");
        } catch (Exception e) {
            EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(e));
            throw e; 
        } finally {
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Deal with /metadata/fgdc/xml/datasetID_fgdc.xml requests (or iso19115) 
     * (or shorter requests).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequest  starting with "metadata"
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doMetadata(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String endOfRequest, String userQuery) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String fileIconsDir = EDStatic.imageDirUrl(loggedInAs) + "fileIcons/";
        String questionMarkUrl = EDStatic.imageDirUrl(loggedInAs) + EDStatic.questionMarkImageFile;

        String urlParts[] = String2.split(
            endOfRequest.endsWith("/")? 
                endOfRequest.substring(0, endOfRequest.length() - 1) : //so no empty part at end
                endOfRequest, 
            '/');
        int nUrlParts = urlParts.length;

        //make a table for use below
        StringArray namePA        = new StringArray();
        LongArray   modifiedPA    = new LongArray();
        LongArray   sizePA        = new LongArray();
        StringArray descriptionPA = new StringArray();
        Table table = new Table();
        table.addColumn("Name",          namePA);
        table.addColumn("Last modified", modifiedPA);
        table.addColumn("Size",          sizePA);
        table.addColumn("Description",   descriptionPA);
        StringArray dirNames = new StringArray();
        String startTallySinceStartup     = "Metadata requests (since startup)";
        String startTallySinceDailyReport = "Metadata requests (since last daily report)";
        String failed = "Failed: ";
        String startFailureLog = "  Metadata request=" + endOfRequest + "\n" +
            "    ";  //add reason here


        // metadata
        if (!urlParts[0].equals("metadata")) {  //it should
            String reason = failed + "urlParts[0] wasn't 'metadata'.";
            if (verbose) String2.log(startFailureLog +     reason);
            EDStatic.tally.add(startTallySinceStartup,     reason);
            EDStatic.tally.add(startTallySinceDailyReport, reason);
            if (verbose) String2.log(EDStatic.resourceNotFound + " " + reason);
            sendResourceNotFoundError(request, response, reason);
            return;
        }

        if (nUrlParts == 1) {
            if (!endOfRequest.endsWith("/")) {
                sendRedirect(response, tErddapUrl + "/" + endOfRequest + "/");
                return;
            }

            //show the directory
            EDStatic.tally.add(startTallySinceStartup,     endOfRequest);
            EDStatic.tally.add(startTallySinceDailyReport, endOfRequest);

            dirNames.add("fgdc");
            dirNames.add("iso19115");

            String title = "Index of " + tErddapUrl + "/" + endOfRequest;
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, title, out); 
            try {
                writer.write(
                    "<div class=\"standard_width\">\n" +
                    "<h1>" + title + "</h1>\n");
                writer.write(
                    table.directoryListing(
                        null, tErddapUrl + "/" + endOfRequest, userQuery, 
                        fileIconsDir, questionMarkUrl, 
                        true, dirNames, null));  
            } catch (Exception e) {
                EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(e));
                throw e; 
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }


        // metadata/fgdc or iso19115
        if (!(urlParts[1].equals("fgdc") ||
              urlParts[1].equals("iso19115"))) {  
            String reason = failed + "urlParts[1] wasn't 'fgdc' or 'iso19115'.";
            if (verbose) String2.log(startFailureLog +     reason);
            EDStatic.tally.add(startTallySinceStartup,     reason);
            EDStatic.tally.add(startTallySinceDailyReport, reason);
            if (verbose) String2.log(EDStatic.resourceNotFound + " " + reason);
            sendResourceNotFoundError(request, response, reason);
            return;
        }
        boolean isFgdc = urlParts[1].equals("fgdc");
        String suffix  = isFgdc? EDD.fgdcSuffix : 
                                 EDD.iso19115Suffix;

        if (nUrlParts == 2) {
            if (!endOfRequest.endsWith("/")) {
                sendRedirect(response, tErddapUrl + "/" + endOfRequest + "/");
                return;
            }

            //show the directory
            EDStatic.tally.add(startTallySinceStartup,     endOfRequest);
            EDStatic.tally.add(startTallySinceDailyReport, endOfRequest);

            dirNames.add("xml");

            String title = "Index of " + tErddapUrl + "/" + endOfRequest;
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, title, out); 
            try {
                writer.write(
                    "<div class=\"standard_width\">\n" +
                    "<h1>" + title + "</h1>\n");
                writer.write(
                    table.directoryListing(
                        null, tErddapUrl + "/" + endOfRequest, userQuery, 
                        fileIconsDir, questionMarkUrl, true, dirNames, null));  
            } catch (Exception e) {
                EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(e));
                throw e; 
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        // metadata/fgdc/xml (or iso19115)
        if (!urlParts[2].equals("xml")) {  
            String reason = failed + "urlParts[2] wasn't 'xml'.";
            if (verbose) String2.log(startFailureLog +     reason);
            EDStatic.tally.add(startTallySinceStartup,     reason);
            EDStatic.tally.add(startTallySinceDailyReport, reason);
            if (verbose) String2.log(EDStatic.resourceNotFound + " " + reason);
            sendResourceNotFoundError(request, response, reason);
            return;
        }

        if (nUrlParts == 3) {
            if (!endOfRequest.endsWith("/")) {
                sendRedirect(response, tErddapUrl + "/" + endOfRequest + "/");
                return;
            }

            //show the directory: list the fgdc or iso19115 datasets
            EDStatic.tally.add(startTallySinceStartup,     endOfRequest);
            EDStatic.tally.add(startTallySinceDailyReport, endOfRequest);

            StringArray tIDs = allDatasetIDs();
            for (int ds = 0; ds < tIDs.size(); ds++) {
                String tDatasetID = tIDs.get(ds);
                EDD edd = gridDatasetHashMap.get(tDatasetID);
                if (edd == null) {
                    edd = tableDatasetHashMap.get(tDatasetID);
                    if (edd == null) 
                        continue;
                }
                //ensure accessibleTo and accessibleVia
                if (!edd.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
                    !edd.graphsAccessibleToPublic()) // /metadata requests
                    continue;
                if ((isFgdc? edd.accessibleViaFGDC() :
                             edd.accessibleViaISO19115()).length() > 0)
                    continue;

                //add this dataset
                String tFileName = tDatasetID + suffix + ".xml";                
                namePA.add(tFileName);
                modifiedPA.add(File2.getLastModified(EDD.datasetDir(tDatasetID) + tFileName));
                sizePA.add(    File2.length(         EDD.datasetDir(tDatasetID) + tFileName));
                descriptionPA.add(edd.title());                                               
            }

            OutputStream out = getHtmlOutputStream(request, response);
            String title = "Index of " + tErddapUrl + "/" + endOfRequest;
            Writer writer = getHtmlWriter(loggedInAs, title, out); 
            writer.write(
                "<div class=\"standard_width\">\n" +
                "<h1>" + title + "</h1>\n");
            writer.write(
                table.directoryListing(
                    null, tErddapUrl + "/" + endOfRequest, userQuery, 
                    fileIconsDir, questionMarkUrl, true, dirNames, null));  
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
            return;
        }

        // metadata/fgdc/xml/[datasetID]_fgdc.xml   (or iso19115)
        if (nUrlParts == 4) {
            String fileName = urlParts[3];
            String reason = failed; 
            if (fileName.endsWith(suffix + ".xml")) {
                String tDatasetID = fileName.substring(0, fileName.length() - (suffix.length() + 4));
                String tDir = EDD.datasetDir(tDatasetID);
                EDD edd = gridDatasetHashMap.get(tDatasetID);
                if (edd == null) 
                    edd = tableDatasetHashMap.get(tDatasetID);
                //ensure accessibleTo and accessibleVia
                if (edd == null) {
                    //reasons are just for Tally, so DON'T TRANSLATE THEM
                    reason += "The dataset wasn't available.";
                } else if (!edd.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
                           !edd.graphsAccessibleToPublic()) { //metadata requests
                    reason += "The user wasn't authorized.";
                } else if (isFgdc && edd.accessibleViaFGDC().length() > 0) {
                    reason += "The dataset wasn't accessibleViaFGDC.";
                } else if (!isFgdc && edd.accessibleViaISO19115().length() > 0) {
                    reason += "The dataset wasn't accessibleViaISO19115.";
                } else if (!File2.isFile(tDir + fileName)) {
                    reason += "The file didn't exist.";
                } else {
                    //valid request
                    EDStatic.tally.add(startTallySinceStartup,     "Succeeded: " + urlParts[1]);
                    EDStatic.tally.add(startTallySinceDailyReport, "Succeeded: " + urlParts[1]);
                    OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                        request, response, 
                        fileName.substring(0, fileName.length() - 4), //remove .xml
                        (isFgdc? ".fgdc" : ".iso19115"), ".xml");
                    OutputStream outputStream = 
                        outSource.outputStream(String2.UTF_8, File2.length(tDir + fileName));
                    doTransfer(request, response, tDir, 
                        tErddapUrl + "/" + File2.getDirectory(endOfRequest), fileName,
                        outputStream, outSource.usingCompression()); 
                    return;
                }
            } else {
                reason += "The requested file didn't end with " + suffix + ".xml.";
            }

            //any failures with nUrlParts==4 end up here
            if (verbose) String2.log(startFailureLog +     reason);
            EDStatic.tally.add(startTallySinceStartup,     reason);
            EDStatic.tally.add(startTallySinceDailyReport, reason);
            if (verbose) String2.log(EDStatic.resourceNotFound + " " + reason);
            sendResourceNotFoundError(request, response, reason);
            return;
        }

        //nUrlParts >= 5
        String reason = failed + "nUrlParts >= 5.";
        if (verbose) String2.log(startFailureLog +     reason);
        EDStatic.tally.add(startTallySinceStartup,     reason);
        EDStatic.tally.add(startTallySinceDailyReport, reason);
        if (verbose) String2.log(EDStatic.resourceNotFound + " " + reason);
        sendResourceNotFoundError(request, response, reason);
    }

    /**
     * This sends an error message for doGeoServicesRest.
     */
    public void sendGeoServicesRestError(HttpServletRequest request, 
        HttpServletResponse response, boolean fParamIsJson, int httpErrorNumber, 
        String message, String details) throws Throwable {

        //json
        if (fParamIsJson) {

            Writer writer = getJsonWriter(request, response, "error", ".jsonText");
            try {
                writer.write(
"{\n" +
"  \"error\" :\n" +
"  {\n" +
"    \"code\" : " + httpErrorNumber + ",\n" +
"    \"message\" : " + String2.toJson(message) + ",\n" +
"    \"details\" : [" + String2.toJson(details) + "]\n" +
"  }\n" +
"}\n");
            } finally {
                writer.close(); //it calls writer.flush then out.close();  
            }
            return;
        }

        //sendResourceNotFoundError
        if (httpErrorNumber == HttpServletResponse.SC_NOT_FOUND) {  //404
            sendResourceNotFoundError(request, response, message + " (" + details + ")");
            return;
        }

        //send http error
        EDStatic.lowSendError(response, httpErrorNumber, message + " (" + details + ")");
    }

    /**
     * Deal with /rest or /rest/... via ESRI GeoServices REST Specification v1.0.
     * https://www.esri.com/library/whitepapers/pdfs/geoservices-rest-spec.pdf 
     * A sample server is https://sampleserver3.arcgisonline.com/ArcGIS/rest/services
     * Only call this method if protocol="rest".
     * 
     * <p>When I checked on 2013-06-12, 
     * https://www.esri.com/industries/landing-pages/geoservices/geoservices states
     * "Use of the GeoServices REST Specification is subject to the current Open Web Foundation Agreement."
     * http://www.openwebfoundation.org/legal/the-owf-1-0-agreements
     * [in spec is http://openwebfoundation.org/legal/agreement/
     *  was http://www.openwebfoundation.org/announcements/introducingtheopenwebfoundationagreement]
     * "The Open Web Foundation Agreement itself establishes the copyright and 
     * patent rights for a specification, ensuring that downstream consumers 
     * may freely implement and reuse the licensed specification without seeking 
     * further permission."
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequest  starting with "rest"
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doGeoServicesRest(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String endOfRequest, String userQuery) throws Throwable {

        if (!EDStatic.geoServicesRestActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "GeoServices REST"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String erddapRestServices = "/" + EDStatic.warName + "/rest/services";  //ESRI uses relative URLs
        String roles[] = EDStatic.getRoles(loggedInAs);
        String teor = String2.replaceAll(endOfRequest, "//", "/"); //bypasses a common ArcGIS problem
        if (teor.endsWith("/"))
            teor = teor.substring(0, teor.length() - 1); //so no empty part at end
        String urlParts[] = String2.split(teor, '/');  
        int nUrlParts = urlParts.length;
        
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=names toLowerCase
        String fParam = queryMap.get("f");  //e.g., json or JSON
        fParam = fParam == null? "" : fParam.toLowerCase();
        boolean defaultFIsHtml = true;  //ESRI servers default to Html
        boolean defaultFIsJson = false;
        boolean fParamIsJson = (fParam.length() == 0 && defaultFIsJson) || fParam.equals("json");
        boolean fParamIsHtml = (fParam.length() == 0 && defaultFIsHtml) || fParam.equals("html");
        String prettyParam = queryMap.get("pretty"); //e.g., true
        String breadCrumbs = 
            "<h2>" + //sample server has "&nbsp;<br/><strong>\n" +
            "<a href=\"" + tErddapUrl + "\">ERDDAP</a>\n" +
            "&gt; <a rel=\"contents\" href=\"" + erddapRestServices + "?f=html\">GeoServices REST Home</a>\n";
        String endBreadCrumbs = "</h2>\n"; //sample server has "</strong>\n" +

        int esriCurrentVersion = 10; //see a sample server
        String UnableToCompleteOperation = "Unable to complete operation."; //from ESRI
        String UnsupportedMediaType = "Unsupported Media Type"; //HTTP 415 error
        String InvalidURL = "Invalid URL"; //from ESRI
        String InvalidParam = "Invalid Parameter"; //from ESRI
        String InvalidFParam = "Invalid f Parameter: Must be html" + 
            (defaultFIsHtml? " (the default)" : "") +
            " or json" + 
            (defaultFIsJson? " (the default)." : ".");

        //String startTallySinceStartup     = "Rest requests (since startup)";
        //String startTallySinceDailyReport = "Rest requests (since last daily report)";
        //EDStatic.tally.add(startTallySinceStartup,     reason);
        //EDStatic.tally.add(startTallySinceDailyReport, reason);

        //*** urlParts[0]="rest"

        //ensure urlParts[0]="rest"
        if (nUrlParts < 1 || !"rest".equals(urlParts[0])) {
            //this shouldn't happen because this method should only be called if protocol=rest
            sendResourceNotFoundError(request, response, "/rest/services was expected.");
            return;
        }

        //just "/rest"
        if (nUrlParts == 1) {
            if (fParamIsJson) //sampleserver is strict for json
                sendGeoServicesRestError(request, response, 
                    fParamIsJson, HttpServletResponse.SC_NOT_FOUND, 
                    UnableToCompleteOperation, InvalidURL);
            else //sampleserver redirects to /rest/services
                sendRedirect(response, erddapRestServices +
                    (fParam.length() == 0? "" : "?f=" + fParam));
            return;
        }

        //*** urlParts[1]="services"

        //ensure urlParts[1]="services"
        if (!"services".equals(urlParts[1])) {
            if (fParamIsJson) //sampleserver is strict for json
                sendGeoServicesRestError(request, response, 
                    fParamIsJson, HttpServletResponse.SC_NOT_FOUND, 
                    UnableToCompleteOperation, InvalidURL);
            else //sampleserver redirects to /rest/services
                sendRedirect(response, erddapRestServices +
                    (fParam.length() == 0? "" : "?f=" + fParam));
            return;
        }

        //just "/rest/services"
        if (nUrlParts == 2) {

            // find supported datasets (accessibleViaGeoServicesRest = "");
            StringArray ids;
            StringArray tids = gridDatasetIDs();
            int ntids = tids.size();
            ids    = new StringArray(ntids, false);
            for (int ti = 0; ti < ntids; ti++) {
                EDD edd = gridDatasetHashMap.get(tids.get(ti));
                if (edd != null && //if just deleted
                    edd.accessibleViaGeoServicesRest().length() == 0 &&
                    (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles))
                    //ESRI REST: treat as if all requests are data requests                        
                    ) {
                    ids.add(edd.datasetID());
                }
            }
            ids.sortIgnoreCase();
            int nids = ids.size();

            if (fParamIsJson) {
                Writer writer = getJsonWriter(request, response, 
                    "rest_services", ".jsonText");
                try {
                    writer.write(
"{ \"specVersion\" : 1.0,\n" +
"  \"currentVersion\" : " + esriCurrentVersion + ",\n" +
"  \"folders\" : [\n");
                    for (int i = 0; i < nids; i++) 
                        writer.write(
"    " + String2.toJson(ids.get(i)) + (i == nids - 1? "" : ",") + "\n");
                    writer.write(
"  ],\n" + 
"  \"services\" : [\n" +
//"    {\"name\" : \"Geometry\", \"type\" : \"GeometryServer\"}\n" +
"  ]\n" +
"}\n");
                } finally {
                    writer.close(); //it calls writer.flush then out.close();  
                }

            } else if (fParamIsHtml) {  
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, 
                    "Folder: /", out); 
                try {
                writer.write(
//?? "<div class=\"standard_width\">\n" +
//esri has different header
breadCrumbs + endBreadCrumbs + 
"\n" +
//ESRI documentation: https://resources.arcgis.com/en/help/main/10.1/index.html#/Making_a_user_connection_to_ArcGIS_Server_in_ArcGIS_for_Desktop/01540000047m000000/\
//which ESRI makes freely reusable under the Open Web Foundation Agreement
"<p>" + String2.replaceAll(EDStatic.geoServicesDescription, "&erddapUrl;", tErddapUrl) +
"\n" +
//then mimic ESRI servers  (except for <div>)
"<h2>Folder: /</h2>\n" +
"<strong>Current Version: </strong>" + (float)esriCurrentVersion + "<br/>\n" +
"<br/>\n" +
//"<strong>View Footprints In: </strong>\n" +
//"&nbsp;&nbsp;<a href=\"" + erddapRestServices + "?f=kmz\">Google Earth</a><br/>\n" +
//"<br/>\n" +
"<strong>Folders:</strong> <br/>\n" +
//"<br/>\n" + //excessive
"<ul id='folderList'>\n");
                for (int i = 0; i < nids; i++) 
                    writer.write(                          //no ?f=...
"<li><a rel=\"chapter\" href=\"" + erddapRestServices + "/" + ids.get(i) + "\">" + ids.get(i) + "</a></li>\n");
                writer.write(
"</ul>\n" +
//"<strong>Services:</strong> <br/>\n" +
//"<br/>\n" +
//"<ul id='serviceList'>\n" +
//"<li><a href=\"" + erddapRestServices + "/Geometry/GeometryServer\">Geometry</a> (GeometryServer)</li>\n" +
//"</ul><br/>\n" +
"<strong>Supported Interfaces: </strong>\n" + //their links have / before ?, but I think it causes problems
"&nbsp;&nbsp;<a target=\"_blank\" rel=\"alternate\" href=\"" + erddapRestServices + "?f=json&amp;pretty=true\">REST</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" rel=\"alternate\" href=\"https://sampleserver3.arcgisonline.com/ArcGIS/services?wsdl\">SOAP</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" href=\"" + erddapRestServices + "?f=sitemap\">Sitemap</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" href=\"" + erddapRestServices + "?f=geositemap\">Geo Sitemap</a>\n" +
"<br/>\n");
                } catch (Exception e) {
                    EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(e));
                    throw e; 
                } finally {
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }

            } else {
                sendGeoServicesRestError(request, response, 
                    fParamIsJson, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    UnsupportedMediaType, InvalidFParam);
            }
            return;
        }

        //*** urlParts[2]= datasetID

        //ensure urlParts[2]=valid datasetID
        String tDatasetID = urlParts[2];
        EDDGrid tEddGrid = gridDatasetHashMap.get(tDatasetID);
        if (tEddGrid == null) {
            sendResourceNotFoundError(request, response, "no such dataset");
            return;
        }
        if (!tEddGrid.isAccessibleTo(EDStatic.getRoles(loggedInAs))) { //authorization (very important)
            //ESRI REST: all requests are data requests
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID,
                tEddGrid.graphsAccessibleToPublic());
            return;
        }
        if (tEddGrid.accessibleViaGeoServicesRest().length() > 0) {
            sendResourceNotFoundError(request, response, tEddGrid.accessibleViaGeoServicesRest());
            return;
        }

        String relativeUrl = erddapRestServices + "/" + tDatasetID;
        breadCrumbs +=
            "&gt; <a rel=\"chapter\" href=\"" + relativeUrl + "?f=html\">" + tDatasetID + "</a>\n";
        EDV tDataVariables[] = tEddGrid.dataVariables();
        EDVLonGridAxis  tEdvLon  = (EDVLonGridAxis)(tEddGrid.axisVariables()[tEddGrid.lonIndex()]); //must exist
        EDVLatGridAxis  tEdvLat  = (EDVLatGridAxis)(tEddGrid.axisVariables()[tEddGrid.latIndex()]); //must exist
        EDVTimeGridAxis tEdvTime = (EDVTimeGridAxis)(tEddGrid.timeIndex() < 0? null : tEddGrid.axisVariables()[tEddGrid.timeIndex()]); //optional

        //just "/rest/services/[datasetID]"
        if (nUrlParts == 3) {

            if (fParamIsJson) {
                Writer writer = getJsonWriter(request, response, 
                    "rest_services_" + tDatasetID, ".jsonText");
                try {
                    writer.write(
"{ \"currentVersion\" : " + esriCurrentVersion + ",\n" +
"  \"folders\" : [],\n" + //esri acts like /[destName] isn't a valid folder!
"  \"services\" : [\n");
                
                    for (int dv = 0; dv < tDataVariables.length; dv++) 
                        writer.write(
"    {\"name\" : \"" + tDatasetID + "/" + tDataVariables[dv].destinationName() + "\", \"type\" : \"ImageServer\"}" +
                            (dv < tDataVariables.length - 1? "," : "") +
                            "\n");
                    writer.write(
"  ]\n" +
"}\n");
                } finally {
                    writer.close(); //it calls writer.flush then out.close();  
                }

            } else if (fParamIsHtml) {  
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, //"Folder: " + 
                    tDatasetID, out); 
                try {
                    writer.write(
//?? "<div class=\"standard_width\">\n" +
//mimic ESRI servers  (except for <div>)
breadCrumbs + endBreadCrumbs + 
"\n" +
"<h2>Folder: " + tDatasetID + "</h2>\n" +
"<strong>Current Version: </strong>" + (float)esriCurrentVersion + "<br/>\n" +
"<br/>\n" +
//"<strong>View Footprints In: </strong>\n" +
//"&nbsp;&nbsp;<a rel=\"alternate\" href=\"" + relativeUrl + "?f=kmz\">Google Earth</a><br/>\n" +
//"<br/>\n" +
"<strong>Services:</strong> <br/>\n" +
//"<br/>\n" +
"<ul id='serviceList'>\n"); 
                    for (int dv = 0; dv < tDataVariables.length; dv++) {
                        if (tDataVariables[dv].hasColorBarMinMax()) 
                            writer.write(
"<li><a rel=\"contents\" href=\"" + 
                       relativeUrl + "/" + tDataVariables[dv].destinationName() + "/ImageServer\">" + 
                        tDatasetID + "/" + tDataVariables[dv].destinationName() + "</a> (ImageServer)</li>\n");
                    }
                    writer.write(
"</ul>\n" +
"<strong>Supported Interfaces: </strong>\n" + //sample servers have / before ?'s, but it's trouble
"&nbsp;&nbsp;<a target=\"_blank\" rel=\"alternate\" href=\"" + relativeUrl + "?f=json&amp;pretty=true\">REST</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" rel=\"alternate\" href=\"https://sampleserver3.arcgisonline.com/ArcGIS/services/Portland/?wsdl\">SOAP</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" href=\"" + relativeUrl + "?f=sitemap\">Sitemap</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" href=\"" + relativeUrl + "?f=geositemap\">Geo Sitemap</a>\n" +
"<br/>\n");
                } finally {
                    endHtmlWriter(out, writer, tErddapUrl, false);  //would be better to call writer.close() here
                }

            } else {
                sendGeoServicesRestError(request, response, 
                    fParamIsJson, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    UnsupportedMediaType, InvalidFParam);
            }
            return;
        }

        //*** urlParts[3]= variable tDestName

        //ensure urlParts[3]=valid variable tDestName
        String tDestName = urlParts[3];
        int tDvi = String2.indexOf(tEddGrid.dataVariableDestinationNames(), tDestName);
        if (tDvi < 0 ||
            !tDataVariables[tDvi].hasColorBarMinMax()) { //must have colorBarMin/Max
            sendResourceNotFoundError(request, response, "This variable doesn't have predefined colorBarMin/Max.");
            return;
        }
        EDV tEdv = tDataVariables[tDvi];

        //just "/rest/services/[tDatasetID]/[tDestName]"
        if (nUrlParts == 4) {
            if (verbose) String2.log(EDStatic.resourceNotFound + " nParts=" + nUrlParts + " !=4");
            sendResourceNotFoundError(request, response, "nQueryParts!=4");
            return;
        }


        //*** urlParts[4]=ImageServer

        //ensure urlParts[4]=ImageServer
        if (!urlParts[4].equals("ImageServer")) {
            if (verbose) String2.log(EDStatic.resourceNotFound + " ImageServer expected");
            sendResourceNotFoundError(request, response, "ImageServer expected");
            return;
        }

        relativeUrl = erddapRestServices + "/" + tDatasetID + "/" + tDestName + "/ImageServer";
        breadCrumbs +=
            "&gt; <a rel=\"contents\" href=\"" + relativeUrl + "?f=html\">" + tDestName + " (ImageServer)</a>\n";
        String serviceDataType = "altitude".equals(tEdv.combinedAttributes().getString("standard_name"))? 
            "esriImageServiceDataTypeElevation" : 
            "esriImageServiceDataTypeProcessed";
        String pixelType = PrimitiveArray.classToEsriPixelType(tEdv.destinationDataTypeClass());
        String spatialReference = "GEOGCS[\"unnamed\",DATUM[\"WGS_1984\"," +  //found on sample server
            "SPHEROID[\"WGS 84\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0]," +
            "UNIT[\"degree\",0.0174532925199433]]";
        String tLicense = tEddGrid.combinedGlobalAttributes().getString("license");
        if (tLicense == null) 
            tLicense = "";  //suitable for json and html

        //just "/rest/services/[tDatasetID]/[tDestName]/ImageServer"
        if (nUrlParts == 5) {

            if (fParamIsJson) {
                Writer writer = getJsonWriter(request, response, 
                    "rest_services_" + tDatasetID + "_" + tDestName, ".jsonText");
                try {
                    writer.write(
"{\n" +
"  \"serviceDescription\" : " + String2.toJson(tEddGrid.title() + "\n" + tEddGrid.summary()) + ", \n" +
"  \"name\" : \"" + tDatasetID + "_" + tDestName + "\", \n" +  //???sample server name is a new 1-piece name, no slashes
"  \"description\" : " + String2.toJson(tEddGrid.title() + "\n" + tEddGrid.summary()) + ", \n" +
"  \"extent\" : {\n" +
//!!!??? I need to deal with lon 0 - 360
"    \"xmin\" : " + tEdvLon.destinationMinString() + ", \n" +
"    \"ymin\" : " + tEdvLat.destinationMinString() + ", \n" +
"    \"xmax\" : " + tEdvLon.destinationMaxString() + ", \n" +
"    \"ymax\" : " + tEdvLat.destinationMaxString() + ", \n" +
"    \"spatialReference\" : {\n" + 
//"      \"wkt\" : " + String2.toJson(spatialReference) + "\n" + //their server
"      \"wkid\" : 4326\n" + //spec 
"    }\n" +
"  }, \n" +
(tEdvTime == null? "" :
  "  \"timeInfo\" : {\"timeExtent\" : [" + 
  Math.round(tEdvTime.destinationMin() * 1000) + "," +
  Math.round(tEdvTime.destinationMax() * 1000) + "]},\n") +  //"timeReference" : null
"  \"pixelSizeX\" : " + (tEdvLon.averageSpacing()) + ", \n" +
"  \"pixelSizeY\" : " + (tEdvLat.averageSpacing()) + ", \n" +
"  \"bandCount\" : 1, \n" +
"  \"pixelType\" : \"" + pixelType + "\", \n" +  
"  \"minPixelSize\" : 0, \n" +
"  \"maxPixelSize\" : 0, \n" +
"  \"copyrightText\" : " + String2.toJson(tLicense) + ", \n" +
"  \"serviceDataType\" : \"" + serviceDataType + "\", \n" +
//"  \"minValues\" : [\n" +
//"    0, \n" +
//"    0, \n" +
//"    0\n" +
//"  ], \n" +
//"  \"maxValues\" : [\n" +
//"    254, \n" +
//"    254, \n" +
//"    254\n" +
//"  ], \n" +
//"  \"meanValues\" : [\n" +
//"    136.94026147211, \n" +
//"    139.542743660379, \n" +
//"    131.186539925398\n" +
//"  ], \n" +
//"  \"stdvValues\" : [\n" +
//"    44.975869054346, \n" +
//"    42.4256509647694, \n" +
//"    40.0998618910186\n" +
//"  ], \n" +

//I don't understand: ObjectID and Fields seem to be always same: x=x
"  \"objectIdField\" : \"OBJECTID\", \n" +
"  \"fields\" : [\n" +
"    {\n" +
"      \"name\" : \"OBJECTID\", \n" +
"      \"type\" : \"esriFieldTypeOID\", \n" +
"      \"alias\" : \"OBJECTID\"}, \n" +
"    {\n" +
"      \"name\" : \"Shape\", \n" +
"      \"type\" : \"esriFieldTypeGeometry\", \n" +
"      \"alias\" : \"Shape\"}, \n" +
"    {\n" +
"      \"name\" : \"Name\", \n" +
"      \"type\" : \"esriFieldTypeString\", \n" +
"      \"alias\" : \"Name\"}, \n" +
"    {\n" +
"      \"name\" : \"MinPS\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"MinPS\"}, \n" +
"    {\n" +
"      \"name\" : \"MaxPS\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"MaxPS\"}, \n" +
"    {\n" +
"      \"name\" : \"LowPS\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"LowPS\"}, \n" +
"    {\n" +
"      \"name\" : \"HighPS\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"HighPS\"}, \n" +
"    {\n" +
"      \"name\" : \"Category\", \n" +
"      \"type\" : \"esriFieldTypeInteger\", \n" +
"      \"alias\" : \"Category\"}, \n" +
"    {\n" +
"      \"name\" : \"Tag\", \n" +
"      \"type\" : \"esriFieldTypeString\", \n" +
"      \"alias\" : \"Tag\"}, \n" +
"    {\n" +
"      \"name\" : \"GroupName\", \n" +
"      \"type\" : \"esriFieldTypeString\", \n" +
"      \"alias\" : \"GroupName\"}, \n" +
"    {\n" +
"      \"name\" : \"ProductName\", \n" +
"      \"type\" : \"esriFieldTypeString\", \n" +
"      \"alias\" : \"ProductName\"}, \n" +
"    {\n" +
"      \"name\" : \"CenterX\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"CenterX\"}, \n" +
"    {\n" +
"      \"name\" : \"CenterY\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"CenterY\"}, \n" +
"    {\n" +
"      \"name\" : \"ZOrder\", \n" +
"      \"type\" : \"esriFieldTypeInteger\", \n" +
"      \"alias\" : \"ZOrder\"}, \n" +
"    {\n" +
"      \"name\" : \"SOrder\", \n" +
"      \"type\" : \"esriFieldTypeInteger\", \n" +
"      \"alias\" : \"SOrder\"}, \n" +
"    {\n" +
"      \"name\" : \"StereoID\", \n" +
"      \"type\" : \"esriFieldTypeString\", \n" +
"      \"alias\" : \"StereoID\"}, \n" +
"    {\n" +
"      \"name\" : \"Shape_Length\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"Shape_Length\"}, \n" +
"    {\n" +
"      \"name\" : \"Shape_Area\", \n" +
"      \"type\" : \"esriFieldTypeDouble\", \n" +
"      \"alias\" : \"Shape_Area\"}\n" +
"  ]\n" +
"}\n");
                } finally {
                    writer.close(); //it calls writer.flush then out.close();  
                }

            } else if (fParamIsHtml) {  

                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, //"Folder: " + 
                    tDatasetID + "/" + tDestName, out); 
                try {
                    writer.write(
// ??"<div class=\"standard_width\">\n" +
//mimic ESRI servers  (except for <div>)
breadCrumbs + endBreadCrumbs + 
"\n" +
"<h2>" + tDatasetID + "/" + tDestName + " (ImageServer)</h2>\n" +
"<strong>View In: </strong>\n" +
"&nbsp;&nbsp;<a rel=\"contents\" href=\"" + relativeUrl + "?f=lyr&amp;v=" + (float)esriCurrentVersion + "\">ArcMap</a>\n" +
//"&nbsp;&nbsp;<a rel=\"alternate\" href=\"" + relativeUrl + "/kml/image.kmz\">Google Earth</a>\n" +
//"&nbsp;&nbsp;<a rel=\"alternate\" href=\"" + relativeUrl + "?f=jsapi\" target=\"_blank\">ArcGIS JavaScript</a>\n" +
//"&nbsp;&nbsp;<a href=\"https://www.arcgis.com/home/webmap/viewer.html?url=http%3a%2f%2fsampleserver3.arcgisonline.com%2fArcGIS%2frest%2fservices%2fPortland%2fAerial%2fImageServer&source=sd\" target=\"_blank\">ArcGIS.com Map" +
//                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
"<br/><br/>\n" +
//"<strong>View Footprint In: </strong>\n" +
//"&nbsp;&nbsp;<a rel=\"alternate\" href=\"" + relativeUrl + "?f=kmz\">Google Earth" +
//                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
//"<br/><br/>\n" +
"<strong>Service Description:</strong> " + XML.encodeAsHTML(tEddGrid.title()) + "<br/>"
                               + XML.encodeAsHTML(tEddGrid.summary()) + "<br/>\n" +
"<br/>\n" +
"<strong>Name:</strong> " + tDatasetID + "_" + tDestName + "<br/>\n" +  //???sample server name is a new 1-piece name, no slashes
"<br/>\n" +
"<strong>Description:</strong> " + XML.encodeAsHTML(tEddGrid.title()) + "<br/>" 
                       + XML.encodeAsHTML(tEddGrid.summary()) + "<br/>\n" +
"<br/>\n" +
"<strong>Extent:</strong> <br/>\n" +
"<ul>\n" +  
//!!!??? I need to deal with lon 0 - 360
//??? sample server doesn't use any <li> !!!
"  <li>XMin: " + tEdvLon.destinationMinString() + "<br/>\n" +    
"  <li>YMin: " + tEdvLat.destinationMinString() + "<br/>\n" +
"  <li>XMax: " + tEdvLon.destinationMaxString() + "<br/>\n" +
"  <li>YMax: " + tEdvLat.destinationMaxString() + "<br/>\n" +
"  <li>Spatial Reference: " + XML.encodeAsHTML(spatialReference) + "<br/>\n" +
"</ul>\n" +
"<strong>Time Info:</strong> <br/>\n" +
"<ul>\n" + 
//??? sample server doesn't use <li> !!!
"  <li>TimeExtent: " + 
    (tEdvTime == null? "null" : "[" + 
    Calendar2.formatAsEsri(Calendar2.epochSecondsToGc(tEdvTime.destinationMin())) + ", " + 
    Calendar2.formatAsEsri(Calendar2.epochSecondsToGc(tEdvTime.destinationMax())) + "]") +
"<br/>\n" +   
"</ul>\n" +
"<strong>Pixel Size X:</strong> " + (tEdvLon.averageSpacing()) + "<br/>\n" +
"<br/>\n" +
"<strong>Pixel Size Y:</strong> " + (tEdvLat.averageSpacing()) + "<br/>\n" +
"<br/>\n" +
"<strong>Band Count:</strong> 1<br/>\n" +
"<br/>\n" +
"<strong>Pixel Type:</strong> " + pixelType + "<br/>\n" +  
"<br/>\n" +
"<strong>Min Pixel Size:</strong> 0<br/>\n" +
"<br/>\n" +
"<strong>Maximum Pixel Size:</strong> 0<br/>\n" +
"<br/>\n" +
"<strong>Copyright Text:</strong> " + XML.encodeAsHTML(tLicense) + "<br/>\n" +
"<br/>\n" +
"<strong>Service Data Type:</strong> " + serviceDataType + "<br/>\n" +  
"<br/>\n" +
//"<strong>Min Values: </strong>0<br/>\n" +
//"<br/>\n" +
//"<strong>Max Values: </strong>254<br/>\n" +
//"<br/>\n" +
//"<strong>Mean Values: </strong>136.94026147211<br/>\n" +
//"<br/>\n" +
//"<strong>Standard Deviation Values: </strong>44.975869054346<br/>\n" +
//"<br/>\n" +

//I don't understand: ObjectID and Fields seem to be always same: x=x
"<strong>Object ID Field:</strong> OBJECTID<br/>\n" +
"<br/>\n" +
"<strong>Fields:</strong>\n" +
"<ul>\n" +
"  <li>OBJECTID <i>(Type: esriFieldTypeOID, Alias: OBJECTID)</i></li>\n" +
"  <li>Shape <i>(Type: esriFieldTypeGeometry, Alias: Shape)</i></li>\n" +
"  <li>Name <i>(Type: esriFieldTypeString, Alias: Name)</i></li>\n" +
"  <li>MinPS <i>(Type: esriFieldTypeDouble, Alias: MinPS)</i></li>\n" +
"  <li>MaxPS <i>(Type: esriFieldTypeDouble, Alias: MaxPS)</i></li>\n" +
"  <li>LowPS <i>(Type: esriFieldTypeDouble, Alias: LowPS)</i></li>\n" +
"  <li>HighPS <i>(Type: esriFieldTypeDouble, Alias: HighPS)</i></li>\n" +
"  <li>Category <i>(Type: esriFieldTypeInteger, Alias: Category)</i></li>\n" +
"  <li>Tag <i>(Type: esriFieldTypeString, Alias: Tag)</i></li>\n" +
"  <li>GroupName <i>(Type: esriFieldTypeString, Alias: GroupName)</i></li>\n" +
"  <li>ProductName <i>(Type: esriFieldTypeString, Alias: ProductName)</i></li>\n" +
"  <li>CenterX <i>(Type: esriFieldTypeDouble, Alias: CenterX)</i></li>\n" +
"  <li>CenterY <i>(Type: esriFieldTypeDouble, Alias: CenterY)</i></li>\n" +
"  <li>ZOrder <i>(Type: esriFieldTypeInteger, Alias: ZOrder)</i></li>\n" +
"  <li>SOrder <i>(Type: esriFieldTypeInteger, Alias: SOrder)</i></li>\n" +
"  <li>StereoID <i>(Type: esriFieldTypeString, Alias: StereoID)</i></li>\n" +
"  <li>Shape_Length <i>(Type: esriFieldTypeDouble, Alias: Shape_Length)</i></li>\n" +
"  <li>Shape_Area <i>(Type: esriFieldTypeDouble, Alias: Shape_Area)</i></li>\n" +
"</ul>\n" +
"<strong>Supported Interfaces: </strong>\n" +  //sample server doesn't encode & !!!???
"&nbsp;&nbsp;<a target=\"_blank\" rel=\"alternate\" href=\"" + relativeUrl + "?f=json&amp;pretty=true\">REST</a>\n" +
//"&nbsp;&nbsp;<a target=\"_blank\" rel=\"alternate\" href=\"" + relativeUrl + "?wsdl\">SOAP</a>\n" +
"<br/><br/>\n" +
"<strong>Supported Operations: </strong>\n" +
"&nbsp;&nbsp;<a rel=\"contents\" href=\"" + relativeUrl + "/exportImage?bbox=" +
    tEdvLon.destinationMinString() + "," +
    tEdvLat.destinationMinString() + "," +
    tEdvLon.destinationMaxString() + "," +
    tEdvLat.destinationMaxString() + "\">Export Image</a>\n" +
"&nbsp;&nbsp;<a rel=\"alternate\" href=\"" + relativeUrl + "/query\">Query</a>\n" +
"&nbsp;&nbsp;<a rel=\"alternate\" href=\"" + relativeUrl + "/identify\">Identify</a>\n" +
"<br/>\n");
                } catch (Exception e) {
                    EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(e));
                    throw e; 
                } finally {
                    endHtmlWriter(out, writer, tErddapUrl, false); //better to call writer.close() here
                }

            } else {
                sendGeoServicesRestError(request, response, 
                    fParamIsJson, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    UnsupportedMediaType, InvalidFParam);
            }
            return;
        }


        //*** urlParts[5]=(exportImage|query|identify)

        //ensure urlParts[5]=exportImage
        if (urlParts[5].equals("exportImage")) {
            String actualDir = tEddGrid.cacheDirectory();

            if (nUrlParts == 6) {

                //bbox
                String bboxParam = queryMap.get("bbox");  
                double xMin = tEdvLon.destinationMin();
                double yMin = tEdvLat.destinationMin();
                double xMax = tEdvLon.destinationMax();
                double yMax = tEdvLat.destinationMax();
                if (bboxParam != null && bboxParam.length() > 0) {
                    //use specified bbox and ensure all valid
                    String bboxParts[] = String2.split(bboxParam, ',');
                    if (bboxParts.length != 4) {
                        sendGeoServicesRestError(request, response, 
                            fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                            InvalidParam, "bbox must be bbox=<xmin>,<ymin>,<xmax>,<ymax>");
                        return;
                    }
                    xMin = String2.parseDouble(bboxParts[0]);
                    yMin = String2.parseDouble(bboxParts[1]);
                    xMax = String2.parseDouble(bboxParts[2]);
                    yMax = String2.parseDouble(bboxParts[3]);
                    if (!Double.isFinite(xMin) || !Double.isFinite(yMin) || 
                        !Double.isFinite(xMax) || !Double.isFinite(yMax) || 
                        xMin >= xMax || yMin >= yMax) { //allow "=" ? 
                        sendGeoServicesRestError(request, response, 
                            fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                            InvalidParam, "Invalid bbox value(s)");
                        return;
                    }
                }

                //size
                String sizeParam = queryMap.get("size");  
                double xSize = 400;  //default in specification
                double ySize = 400;
                if (sizeParam != null && sizeParam.length() > 0) {
                    //use specified size and ensure all valid
                    String sizeParts[] = String2.split(sizeParam, ',');
                    if (sizeParts.length != 2) {
                        sendGeoServicesRestError(request, response, 
                            fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                            InvalidParam, "size must be size=<width>,<height>");
                        return;
                    }
                    xSize = String2.parseInt(sizeParts[0]);
                    ySize = String2.parseInt(sizeParts[1]);
                    if (xSize == Integer.MAX_VALUE || ySize == Integer.MAX_VALUE || 
                        xSize <= 0 || ySize <= 0) { 
                        sendGeoServicesRestError(request, response, 
                            fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                            InvalidParam, "Invalid size value(s)");
                        return;
                    }
                }

                //imageSR
                //bboxSR

                //time
                String centeredIsoTime = null;
                if (tEdvTime == null) {  
                    //no time variable, so ignore user-specified time= (if any)
                } else {
                    String timeParam = queryMap.get("time");  
                    double tEpochSeconds = tEdvTime.destinationMax();  //spec doesn't say default
                    if (timeParam != null && timeParam.length() > 0) {
                        //use specified time and ensure all valid
                        String timeParts[] = String2.split(timeParam, ',');
                        if (timeParts.length == 1) {
                            tEpochSeconds = String2.parseDouble(timeParts[0]) / 1000.0; //millis -> seconds
                        } else if (timeParts.length == 2) {
                            double tMinTime = String2.parseDouble(timeParts[0]);
                            double tMaxTime = String2.parseDouble(timeParts[1]);
                            if (!Double.isFinite(tMinTime))
                                tMinTime = tEdvTime.destinationMin(); //spec says "infinity"; I interpret as destMin/Max
                            if (!Double.isFinite(tMaxTime))
                                tMaxTime = tEdvTime.destinationMax();
                            tEpochSeconds = (tMinTime + tMaxTime) / 2000.0; //2 to average
                        } else {
                            sendGeoServicesRestError(request, response, 
                                fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                                InvalidParam, "time must be time=<timeInstant> or time=<startTime>,<endTime>");
                            return;
                        }
                        if (!Double.isFinite(tEpochSeconds) || 
                            tEpochSeconds <= tEdvTime.destinationCoarseMin() ||
                            tEpochSeconds >= tEdvTime.destinationCoarseMax()) { 
                            sendGeoServicesRestError(request, response, 
                                fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                                InvalidParam, "Invalid time value(s)");
                            return;
                        }
                    }

                    //find closest index (so canonical request), then epochSeconds, then ISO (so readable)
                    centeredIsoTime = tEdvTime.destinationToString(
                        tEdvTime.destinationDouble(
                        tEdvTime.destinationToClosestSourceIndex(tEpochSeconds)));
                }

                //format
                String formatParam = queryMap.get("format");
                String fileTypeName = ".transparentPng";
                String fileExtension = ".png";
                if (formatParam == null ||  //spec-defined default is jpgpng
                    ("||jpgpng|png|png8|png24|jpg|bmp|gif|").indexOf("|" + formatParam + "|") >= 0) {
                    //already fileExtension = ".png";   //valid but unsupported -> png  ???
                } else if (formatParam.equals("tiff")) {
                    fileTypeName = ".geotif";
                    fileExtension = ".tif";

                    //ERDDAP geotif requirement: lon must be all below or all above 180
                    if (xMin < 180 && xMax > 180) {
                        sendGeoServicesRestError(request, response, 
                            fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                            InvalidParam, "For format=tiff, the bbox longitude min and max can't span longitude=180.");
                        return;
                    }
                } else {
                    sendGeoServicesRestError(request, response, 
                        fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                        InvalidParam, "Format must be format=(jpgpng|png|png8|png24|jpg|bmp|gif|tiff)");
                    return;
                }

                //pixelType

                //noData

                //interpolation

                //compressionQuality

                //bandIds   
                String bandIdsParam = queryMap.get("bandIds");  
                if (bandIdsParam != null && !bandIdsParam.equals("0")) {
                    //ERDDAP is set up for 1 band per dataset/destName, so only "0" is valid request
                    sendGeoServicesRestError(request, response, 
                        fParamIsJson, HttpServletResponse.SC_BAD_REQUEST,
                        InvalidParam, "BandIds must be bandIds=0");
                    return;
                }

                //mosaicRule
                //renderingRule

                //make the image
                String virtualFileName = null;
                if (fParam.length() == 0 || fParamIsJson || fParam.equals("image")) {

                    //generate the userDapQuery          %7C=|
                    StringBuilder iQuery = new StringBuilder(tDestName);
                    EDVGridAxis tAxisVariables[] = tEddGrid.axisVariables();
                    int nav = tAxisVariables.length;
                    for (int avi = 0; avi < nav; avi++) {
                        iQuery.append('[');
                        //EDVGridAxis ega = tAxisVariables[avi];
                        if (avi == tEddGrid.lonIndex()) {
                            iQuery.append("(" + xMin + "):(" + xMax + ")");
                        } else if (avi == tEddGrid.latIndex()) {
                            if (tAxisVariables[avi].isAscending())
                                iQuery.append("(" + yMin + "):(" + yMax + ")");
                            else 
                                iQuery.append("(" + yMax + "):(" + yMin + ")");
                        } else if (avi == tEddGrid.timeIndex()) {
                            iQuery.append("(" + centeredIsoTime + ")");
                        } else {
                            iQuery.append("[0]"); //??? temporary lame cop out!
                        }
                        iQuery.append(']');
                    }                            
                    iQuery.append("&.draw=surface&.vars=longitude%7Clatitude%7C" + tDestName); 
                    iQuery.append("&.size=" + xSize + "%7C" + ySize); // |
                    String imageQuery = iQuery.toString();
                    if (verbose) String2.log("  exportImage query=" + imageQuery);

                    //generate the file name (no extension)
                    virtualFileName = tEddGrid.suggestFileName(loggedInAs, imageQuery, fileTypeName);

                    //create the image file if it doesn't exist  
                    if (File2.isFile(actualDir + virtualFileName + fileExtension)) {
                        if (verbose) String2.log("  reusing imageFile=" + 
                            actualDir + virtualFileName + fileExtension);
                    } else {
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(actualDir + virtualFileName + fileExtension));
                        OutputStreamSource oss = new OutputStreamSourceSimple(out);

                        try { //most exceptions written to image.  some throw throwable.
                            tEddGrid.saveAsImage(loggedInAs, relativeUrl, imageQuery, 
                                actualDir, virtualFileName, oss, fileTypeName); 
                            out.close(); 
                        } catch (Throwable t) {
                            sendGeoServicesRestError(request, response, 
                                fParamIsJson, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                UnableToCompleteOperation, t.toString());
                            return;

                        }
                    }
                }

                //f
                if (fParam.length() == 0 || fParamIsJson) {  //default
                    Writer writer = getJsonWriter(request, response, 
                        "rest_services_" + tDatasetID + "_" + tDestName, ".jsonText");
                    try {
                        writer.write(
                        "{\n" +
                        "  \"href\" : \"" + tErddapUrl + 
                          relativeUrl.substring(EDStatic.warName.length() + 1) +
                          "/exportImage/" + virtualFileName + fileExtension + "\"\n" +
                        "  \"width\" : \"" + xSize + "\"\n" +
                        "  \"height\" : \"" + ySize + "\"\n" +
                        "  \"extent\" : {\n" +
                        "    \"xmin\" : " + xMin + ", \"ymin\" : " + yMin + ", " +
                            "\"xmax\" : " + xMax + ", \"ymax\" : " + yMax + ",\n" +
                        "    \"spatialReference\" : {\"wkid\" : 4326}\n" +
                        "  }\n" +
                        "}\n");
                    } finally {
                        writer.close(); //it calls writer.flush then out.close();  
                    }

                } else if (fParam.equals("image")) {
                    OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                        request, response, 
                        virtualFileName, fileTypeName, fileExtension);
                    OutputStream out = outSource.outputStream("");
                    doTransfer(request, response, actualDir, relativeUrl, 
                        virtualFileName + fileExtension, 
                        out, outSource.usingCompression());

                //} else if (fParam.equals("kmz")) {
                //    ...

                } else {
                    sendGeoServicesRestError(request, response, 
                        fParamIsJson, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        UnsupportedMediaType, InvalidFParam);
                }            
                return;
                //end of nUrlParts == 6;

            } else if (nUrlParts == 7) {
                //it's a request for an image file
                String tFileName = urlParts[6];
                if (File2.isFile(actualDir + tFileName)) {
                    //transfer
                    String fileExtension = File2.getExtension(tFileName);
                    String fileTypeName = fileExtension.equals(".tif")? ".geoTif" :
                                          fileExtension.equals(".png")? ".transparentPng" :
                                          fileExtension;
                    OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                        request, response, 
                        File2.getNameNoExtension(tFileName), fileTypeName, fileExtension);
                    OutputStream out = outSource.outputStream("");
                    doTransfer(request, response, actualDir, relativeUrl, 
                        tFileName, out, outSource.usingCompression());

                } else {
                    if (verbose) String2.log(EDStatic.resourceNotFound + 
                        " !isFile " + actualDir + tFileName);
                    sendResourceNotFoundError(request, response, "file doesn't exist");
                    return;
                }
                return;

            } else {
                if (verbose) String2.log(EDStatic.resourceNotFound + 
                    " nParts=" + nUrlParts + " !=7");
                sendResourceNotFoundError(request, response, "incorrect nParts");
                return;
            } 
            //end of /exportImage[/fileName]

        } else if (urlParts[5].equals("query")) {      
            //...
            return;

        } else if (urlParts[5].equals("identify")) {
            //...
            return;

        } else { //unsupported parts[5]
            if (verbose) String2.log(EDStatic.resourceNotFound + 
                " unknown [5]=" + urlParts[5]);
            sendResourceNotFoundError(request, response, "");
            return;
        }

    }

    /** 
     * This responds to a user's requst for a file in the (pseudo)'protocol' (e.g., images) 
     * directory.
     * This works with files in subdirectories of 'protocol'.
     * <p>The problem is that web.xml transfers all requests to [url]/erddap/*
     * to this servlet. There is no way to allow requests for files in e.g. /images
     * to be handled by Tomcat. So handle them here by doing a simple file transfer.
     *
     * @param protocol here is 'download', 'images', or 'public'
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (e.g., "images") in the requestUrl
     * @throws Throwable if trouble
     */
    public static void doTransfer(HttpServletRequest request, HttpServletResponse response,
        String protocol, int datasetIDStartsAt) throws Throwable {

        String requestUrl = request.getRequestURI();  // e.g., /erddap/images/QuestionMark.jpg
        //be extra certain to avoid the security problem Local File Inclusion
        //see https://www.netsparker.com/web-vulnerability-scanner/vulnerabilities/local-file-inclusion/
        if (requestUrl.indexOf("/../") >= 0 || 
            !String2.isPrintable(requestUrl) ||
            requestUrl.indexOf("%0") >= 0) {  //percent-encoded ASCII char <16, e.g., %00
            throw new SimpleException(EDStatic.queryError + "Some characters are never allowed in requests.");
        }
        String dir = EDStatic.contextDirectory + protocol + "/";
        String fileNameAndExt = requestUrl.length() <= datasetIDStartsAt? "" : 
            requestUrl.substring(datasetIDStartsAt);

        String ext = File2.getExtension(fileNameAndExt);
        String fileName = fileNameAndExt.substring(0, fileNameAndExt.length() - ext.length()); 
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, fileName, ext, ext); 
        //characterEncoding not relevant for binary files
        String charEncoding = 
            ext.equals(".asc") || ext.equals(".csv") || 
            ext.equals(".htm") || ext.equals(".html") || 
            ext.equals(".js")  || ext.equals(".json") || ext.equals(".kml") || 
            ext.equals(".pdf") || ext.equals(".tsv") || 
            ext.equals(".txt") || ext.equals(".xml")? 
            String2.UTF_8 : //an assumption, the most universal solution
            "";

        //Set expires header for things that don't change often.
        //See "High Performance Web Sites" Steve Souders, Ch 3.
        if (protocol.equals("images")) { 
            //&& fileName.indexOf('/') == -1) {   //file not in a subdirectory
            //&& (ext.equals(".gif") || ext.equals(".jpg") || ext.equals(".js") || ext.equals(".png"))) {
            
            GregorianCalendar gc = Calendar2.newGCalendarZulu();
            int nDays = 7; //one week gets most of benefit and few problems
            gc.add(Calendar2.DATE, nDays); 
            String expires = Calendar2.formatAsRFC822GMT(gc);
            if (reallyVerbose) String2.log("  setting expires=" + expires + " header");
            response.setHeader("Cache-Control", "PUBLIC, max-age=" + 
                (nDays * Calendar2.SECONDS_PER_DAY) + ", must-revalidate");
            response.setHeader("Expires", expires);
        }
         
        OutputStream outputStream = outSource.outputStream(charEncoding, 
            File2.length(dir + fileNameAndExt));
        doTransfer(request, response, dir, protocol + "/", fileNameAndExt, 
            outputStream, outSource.usingCompression());
    }

    /** 
     * This is the lower level version of doTransfer.
     *
     * @param localDir the actual hard disk directory (or url dir), ending in '/'
     * @param webDir the apparent directory, ending in '/' (e.g., "public/"),
     *    for error message only
     * @param fileNameAndExt e.g., wms_29847362839.png
     *    (although it can be e.g., subdir/wms_29847362839.png)
     * @param outputStream  If no exception thrown (or will be), this closes the outputStream
     * @param usingCompression The type of encoding (compression) being used 
     *  (gzip, deflate) or "identity" if no compression.
     * @throws Throwable if trouble
     */
    public static void doTransfer(HttpServletRequest request, HttpServletResponse response,
            String localDir, String webDir, String fileNameAndExt, 
            OutputStream outputStream, String usingCompression) throws Throwable {

        String msg = "doTransfer " + localDir + fileNameAndExt + 
            "\n  compression=" + usingCompression;

        long fileSize = -1;
        long first = 0;
        long last = -1;

        boolean isRemote = String2.isRemote(localDir + fileNameAndExt);
        if (!isRemote) {
            fileSize = File2.length(localDir + fileNameAndExt); //it checks isFile
            if (fileSize < 0) {
                String2.log(msg);
                sendResourceNotFoundError(request, response, "file doesn't exist");
                return;
            } else {
                last = fileSize - 1;
            }
        }

        //Is this a byte range request?    "Range: bytes=0-1023"
        //https://en.wikipedia.org/wiki/Byte_serving
        //https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
        String range = request.getHeader("Range"); 
        msg += ", fileSize=" + fileSize + 
            (range == null? "" : ", Range request=" + String2.annotatedString(range));
        String extLC = File2.getExtension(fileNameAndExt).toLowerCase();
        boolean rangeRequestAllowed = String2.indexOf(EDStatic.extensionsNoRangeRequests, extLC) < 0;
        if (range != null) {
            if (!rangeRequestAllowed) {
                String2.log(msg);
                throw new SimpleException(
                    EDStatic.REQUESTED_RANGE_NOT_SATISFIABLE + //sendErrorCode looks for this 
                    "Don't try to connect to .nc or .hdf files on ERDDAP's /files/ system as if they were local files. " +
                    "It is horribly inefficient and often causes other problems. Instead: " +
                    "a) Use (OPeN)DAP client software to connect to ERDDAP's DAP services for this dataset " +
                    "(which have /griddap/ or /tabledap/ in the URL). That's what DAP is for." +
                    "b) Use the dataset's Data Access Form to request a subset of data. " +
                    "c) If you need the entire file or repeated access over a long period of time, " +
                    "use curl, wget, or your browser to download the entire file, " +
                    "then access the data from your local copy of the file.");
            }
            if (!"identity".equals(usingCompression)) {
                String2.log(msg);
                throw new SimpleException(
                    EDStatic.REQUESTED_RANGE_NOT_SATISFIABLE + //sendErrorCode looks for this 
                    "ERDDAP set Content-Encoding=" + usingCompression + 
                    " when it should have used \"identity\".");
            }
            Pattern p = Pattern.compile("bytes=([0-9]+)-(|[0-9]+)");
            Matcher m = p.matcher(range);
            if (m.matches()) {
                first = String2.parseLong(m.group(1));
                if (m.group(2).equals("")) //<audio> makes requests like this
                     last = fileSize >= 0? fileSize - 1 : -1;
                else last = String2.parseLong(m.group(2));
            } else {
                String2.log(msg);
                throw new SimpleException(
                    EDStatic.REQUESTED_RANGE_NOT_SATISFIABLE + //sendErrorCode looks for this
                    "The Range header must use the format: \"bytes=first-[last]\". (last is optional)");
            }
            if (first < 0 || 
                first == Long.MAX_VALUE || 
                last == Long.MAX_VALUE ||
                (last>= 0 && first > last) ||
                (fileSize >= 0 && last >= fileSize)) {
                String2.log(msg);
                throw new SimpleException(
                    EDStatic.REQUESTED_RANGE_NOT_SATISFIABLE + //sendErrorCode looks for this 
                    "Invalid Range requested: first=" + first + ", last=" + last);
            }

            //status must be set before content is sent. Assume transfer will be successful.
            response.setStatus(206); //206=SC_PARTIAL_CONTENT successfully sent
            String value = "bytes " + first + "-";
            if (last >= 0) {
                value += last + "/" + (fileSize >= 0? "" + fileSize : "*");
                response.setHeader("Content-Range", value);
                //see https://stackoverflow.com/questions/5052635/what-is-relation-between-content-length-and-byte-ranges-in-http-1-1
                response.setContentLengthLong(1 + last - first);         
                //response.setHeader("Content-Length", "" + (1 + last - first));         
            }
            response.setHeader("Content-Range", value);
            msg += ", set Content-Range=" + value;
        } else {
            if (rangeRequestAllowed) {
                //offer to Accept-ranges
                response.setHeader("Accept-ranges", "bytes");
            }
            //Only set Content-Length if usingCompression = identity
            //  Was: DON'T SET Content-Length!  
            //See comments re: hasRangeRequest in OutputStreamFromHttpResponse.
            if (fileSize >= 0 && "identity".equals(usingCompression)) {
                //response.setHeader("Content-Length", "" + fileSize);         
                response.setContentLengthLong(fileSize);         
                msg += ", set Content-Length=" + fileSize;
            }            
        }
        if (verbose) 
            String2.log(msg);

        //it's good that result is boolean: for security, don't return localDir name in error message
        try {
            boolean ok = SSR.copy(localDir + fileNameAndExt, outputStream, first, last);   //handles file or URL destination
            if (!ok) {
                if (!verbose)
                    String2.log(msg); //if wasn't logged above
                throw new SimpleException(
                    (range == null? "" : EDStatic.REQUESTED_RANGE_NOT_SATISFIABLE) +//sendErrorCode looks for this 
                    String2.ERROR + " during transfer.");
            }
        } finally {
            try {outputStream.close();} catch (Exception e) {} //downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
        }
    }

    /** 
     * This responds to a user's requst for an rss feed.
     * Now (Dec 2017, v1.81), this does check that the user has access to the dataset.
     *
     * @param protocol here is always 'rss'
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *   in the requestUrl
     */
    public void doRss(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String protocol, int datasetIDStartsAt) throws Throwable {

        String requestUrl = request.getRequestURI();  // /erddap/images/QuestionMark.jpg
        String nameAndExt = requestUrl.length() <= datasetIDStartsAt? "" : 
            requestUrl.substring(datasetIDStartsAt); //should be <datasetID>.rss
        if (!nameAndExt.endsWith(".rss")) {
            sendResourceNotFoundError(request, response, "Invalid name. Extension must be .rss.");
            return;
        }
        String tDatasetID = nameAndExt.substring(0, nameAndExt.length() - 4);
        EDStatic.tally.add("RSS (since last daily report)", tDatasetID);
        EDStatic.tally.add("RSS (since startup)", tDatasetID);

        //does the dataset exist?
        EDD edd = gridDatasetHashMap.get(tDatasetID);
        if (edd == null) {
            edd = tableDatasetHashMap.get(tDatasetID);
            if (edd == null) {
                //It would be better to change the rss to "dataset not available?".
                //But without edd, I can't tell if it is a private dataset.
                //so just send resourceNotFound
                //Not good: if edd is private, anyone can find out when it isn't available.
                sendResourceNotFoundError(request, response, EDStatic.rssNo);
            return;
            }
        }

        //check loggedInAs
        String roles[] = EDStatic.getRoles(loggedInAs);
        boolean gatp = edd.graphsAccessibleToPublic(); //treat rss like a graph
        if (gatp || edd.isAccessibleTo(roles)) {
            //okay
        } else {
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID, gatp);
            return;
        }

        //get rss content
        byte rssAr[] = tDatasetID.length() == 0? null : rssHashMap.get(tDatasetID);
        if (tDatasetID.equals(EDDTableFromAllDatasets.DATASET_ID) || rssAr == null) {
            sendResourceNotFoundError(request, response, EDStatic.rssNo);
            return;
        }

        //Okay. This is going to work!
        //substitute &erddapUrl;
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String s = String2.utf8BytesToString(rssAr);
        s = String2.replaceAll(s, "&erddapUrl;", tErddapUrl);
        rssAr = String2.stringToUtf8Bytes(s);

        //reply to request
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, tDatasetID, "custom:application/rss+xml", ".rss"); 
        OutputStream outputStream = outSource.outputStream(String2.UTF_8); 
        try {
            outputStream.write(rssAr);
        } finally {
            outputStream.close();
        }
    }


    /**
     * This responds to a setDatasetFlag.txt request.
     *
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doSetDatasetFlag(String ipAddress, 
        HttpServletRequest request, HttpServletResponse response, 
        String userQuery) throws Throwable {
        //see also EDD.flagUrl()

        //generate text response
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "setDatasetFlag", ".txt", ".txt");
        OutputStream out = outSource.outputStream(String2.UTF_8);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out)); 
        try {
            //look at the request
            HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //false so names are case insensitive
            String datasetID = queryMap.get("datasetid"); //lowercase name
            String flagKey = queryMap.get("flagkey"); //lowercase name
            //isFileNameSafe is doubly useful: it ensures datasetID could be a dataseID, 
            //  and it ensures file of this name can be created
            String message;
            int delaySeconds = 5; //slow down brute force attack or trying to guess flagKey
            if (String2.isSomething(datasetID)) datasetID = datasetID.trim();
            if (String2.isSomething(flagKey))   flagKey   = flagKey.trim();            

            if (!String2.isSomething(datasetID) ||
                !String2.isSomething(flagKey)) {
                message = String2.ERROR + ": Incomplete request.";
            } else if (!String2.isFileNameSafe(datasetID)) {
                message = String2.ERROR + ": Invalid datasetID.";
            } else if (!EDD.flagKey(datasetID).equals(flagKey)) {
                message = String2.ERROR + ": Invalid flagKey.";
            } else {
                //It's ok if it isn't an existing edd.  An inactive dataset is a valid one to flag.
                //And ok of it isn't even in datasets.xml.  Unknown files are removed.
                EDStatic.tally.add("SetDatasetFlag (since startup)", datasetID);
                EDStatic.tally.add("SetDatasetFlag (since last daily report)", datasetID);
                String2.writeToFile(EDStatic.fullResetFlagDirectory + datasetID, datasetID);
                message = "SUCCESS: The flag has been set.";
                delaySeconds = 0;
            }

            String sf = message.startsWith(String2.ERROR)? "Failed" : "Succeeded";
            EDStatic.tally.add("SetDatasetFlag " + sf + ", IP Address (since last daily report)", ipAddress);
            EDStatic.tally.add("SetDatasetFlag " + sf + ", IP Address (since startup)", ipAddress);

            Math2.sleep(delaySeconds * 1000);
            writer.write(message);
            if (verbose) String2.log(message + " setDatasetFlag(" + ipAddress + ")");

        } finally {
            writer.close(); //it calls writer.flush then out.close();  
        }
    }


    /**
     * This responds to a version request.
     *
     * @throws Throwable if trouble
     */
    public void doVersion(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //see also EDD.flagUrl()

        //generate text response
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "version", ".txt", ".txt");
        OutputStream out = outSource.outputStream(String2.UTF_8);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out)); 
        try {
            String ev = EDStatic.erddapVersion;
            int po = ev.indexOf('_');
            if (po >= 0)
                ev = ev.substring(0, po);
            writer.write("ERDDAP_version=" + ev + "\n");
        } finally {
            writer.close(); //it calls writer.flush then out.close();  
        }
    }

    /**
     * This responds to a version_string request.
     *
     * @throws Throwable if trouble
     */
    public void doVersionString(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //see also EDD.flagUrl()

        //generate text response
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "version_string", ".txt", ".txt");
        OutputStream out = outSource.outputStream(String2.UTF_8);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out)); 
        try {
            writer.write("ERDDAP_version_string=" + EDStatic.erddapVersion + "\n");
        } finally {
            writer.close(); //it calls writer.flush then out.close();  
        }
    }


    /**
     * This responds to a outOfDateDatasets.fileType request.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doOutOfDateDatasets(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String endOfRequest, String userQuery) throws Throwable {

        if (!EDStatic.outOfDateDatasetsActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "Out-Of-Date Datasets"));
            return;
        }

        //constants 
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        int refreshEveryNMinutes = Math2.roundToInt(EDStatic.loadDatasetsMinMillis / 60000.0);

        //parse endOfRequest
        String start = "outOfDateDatasets.";
        if (!endOfRequest.startsWith(start))
            throw new SimpleException(
                "An outOfDateDatasets request must start with \"" + start + "\".");
        String fileType = endOfRequest.substring(start.length() - 1);
        boolean isPlainType = false;
        if (fileType.equals(".html")) {
        } else if (String2.indexOf(plainFileTypes, fileType) >= 0) {
            isPlainType = true;
        } else {
            throw new SimpleException(
                "The fileType must be one of " + plainFileTypesString + ".");
        }

        //get allDatasets
        EDDTableFromAllDatasets allDatasets =  
            (EDDTableFromAllDatasets)tableDatasetHashMap.get(EDDTableFromAllDatasets.DATASET_ID);
        if (allDatasets == null)
            throw new SimpleException(
                "outOfDateDatasets is currently not available.");

        //parse userQuery
        StringArray resultsVars = new StringArray();
        StringArray conNames = new StringArray(); 
        StringArray conOps   = new StringArray(); 
        StringArray conVals  = new StringArray();
        allDatasets.parseUserDapQuery(userQuery, 
            resultsVars, conNames, conOps, conVals, false); //repair

        //get the table from allDatasets
        String currentTimeZulu = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
        Table table = allDatasets.makeDatasetTable(loggedInAs);
        BitSet keep = new BitSet();
        keep.set(0, table.nRows());
        table.justKeepColumns(new String[]{"outOfDate", "testOutOfDate", "maxTime", "datasetID", "title"}, "");
        int oodCol = 0;
        int toodCol = 1;
        int mtCol = 2;
        int idCol = 3;
        int tCol = 4;
        table.applyConstraint(idCol, "outOfDate", "!=", "NaN", keep);
        table.tryToApplyConstraints(idCol, conNames, conOps, conVals, keep);
        table.justKeep(keep);
        table.sort(new int[]{oodCol, idCol}, new boolean[]{false, true});
        //don't apply resultsVars

        if (isPlainType) {
            if (table.nRows() == 0)
                throw new SimpleException(MustBe.THERE_IS_NO_DATA);
            sendPlainTable(loggedInAs, request, response, table, "outOfDateDatasets", fileType);
            return;
        } 

        //generate html response
        String shortTitle = "Out-Of-Date Datasets";
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, shortTitle, 
            "<meta http-equiv=\"refresh\" content=\"" + (refreshEveryNMinutes * 60) + "\" >", 
            out); 
        try {
            //make some changes to table
            int nRows = table.nRows();
            StringArray outOfDateSA     = new StringArray(nRows, false);
            FloatArray  outOfDateFA     = (FloatArray)table.getColumn(oodCol);
            StringArray testOutOfDateSA = (StringArray)table.getColumn(toodCol);
            StringArray datasetIDSA     = (StringArray)table.getColumn(idCol);
            StringArray titleSA         = (StringArray)table.getColumn(tCol);

            for (int row = 0; row < nRows; row++) {
                float ood = outOfDateFA.get(row);
                outOfDateSA.add("<span style=\"color:#" + 
                    (ood < -2?  "7000e0" : //purple5 (purple4=8000ff) (color names from cohort)
                     ood < -1?  "a040ff" : //purple3 
                     ood < 0?   "c080ff" : //purple2 
                     ood < 0.5? "00d000" : //green5
                     ood < 1?   "40ff40" : //green3 
                     ood < 1.5? "ff9030" : //orange3
                     ood < 2?   "ff7000" : //orange4  (orange5 is e06000)
                     ood < 5?   "ff4040" : //red3
                     ood < 10?  "e00000" : //red5
                                "a00000") +//red7
                    "\">&#x2588;&#x2588;&#x2588;</span> " +
                    outOfDateFA.getString(row));
                testOutOfDateSA.set(row, XML.encodeAsHTML(testOutOfDateSA.get(row)));
                datasetIDSA.set(    row, XML.encodeAsHTML(datasetIDSA.get(    row)));

                //break long titles into multiple lines
                String tTitle = titleSA.get(row);
                tTitle = String2.noLongLines(tTitle, EDStatic.TITLE_DOT_LENGTH, ""); //insert newlines
                tTitle = XML.encodeAsHTML(tTitle); //newlines intact
                tTitle = String2.replaceAll(tTitle, "\n", "<br>");
                titleSA.set(row, tTitle);
            }
            table.setColumn(oodCol, outOfDateSA);

            //write html response
            writer.write("<div class=\"standard_width\">");
            writer.write(EDStatic.youAreHere(loggedInAs, shortTitle)); 
            writer.write(XML.encodeAsHTML(EDStatic.advc_outOfDate));
            writer.write(
                "\n<p>");
            if (table.nRows() == 0) {
                writer.write("[" +
                    MessageFormat.format(EDStatic.nMatching, "0") + " " +
                    EDStatic.advn_outOfDate + "]");
            } else {
                writer.write(
                    MessageFormat.format(EDStatic.nMatching, "" + table.nRows()) + " " +
                    MessageFormat.format(EDStatic.generatedAt, 
                        "<span class=\"N\">" + currentTimeZulu + "</span>") + 
                    "\n<br>");
                table.saveAsHtmlTable(writer, 
                    "commonBGColor", null, 1, //other classes, bgColor, border, 
                    false, mtCol, false, //writeUnits, timeColumn, needEncodingAsHtml,
                    false); //allowWrap
            }

            //autoRefresh message
            writer.write("<p>" + 
                MessageFormat.format(EDStatic.generatedAt, 
                    "<span class=\"N\">" + currentTimeZulu + "</span>") + 
                "\n<br>" +
                MessageFormat.format(EDStatic.autoRefresh, 
                    "" + refreshEveryNMinutes) + 
                "\n");

            //addConstraints
            writer.write(
                "<h3>" + EDStatic.options + "</h3>\n" +
                XML.encodeAsHTML(EDStatic.addConstraints) +
                "<br><a rel=\"bookmark\" href=\"" + tErddapUrl + "/" + start + 
                    "html?&amp;outOfDate%3E=0.5\">"  + tErddapUrl + "/" + start +
                    "html?&amp;outOfDate&gt;=0.5</a> .\n" +
                String2.replaceAll(EDStatic.percentEncode, "&erddapUrl;", tErddapUrl));

            //list plain file types
            writer.write(
                "\n" +
                "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                plainFileTypesString + //not links, which would be indexed by search engines
                ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                    EDStatic.restfulViaService + "</a>.\n");

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t;
        } finally {

            writer.write("</div>\n"); 
            endHtmlWriter(out, writer, tErddapUrl, false);
        }        
    }


    
    /**
     * This responds to a slidesorter.html request.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doSlideSorter(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.slideSorterActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "SlideSorter"));
            return;
        }

        //FUTURE: when submit(), identify the slide acted upon
        //and move it to forefront (zlevel=highest).

        //constants 
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String formName = "f1";
        String dFormName = "document." + formName;
        int border = 20;
        int gap = 10;
        String gapPx = gap + "px";
        int defaultContentWidth = 360;
        String bgColor = "#ffffff";  //before ERDDAP v2: was "#d7dcdd" here; ERDDAP was "#ccccff";
        int connTimeout = 120000; //ms
        String ssBePatientAlt = "alt=\"" + EDStatic.ssBePatient + "\" ";

        //DON'T use GET-style params, use POST-style (request.getParameter)  

        //get info from document
        int nSlides = String2.parseInt(request.getParameter("nSlides"));
        int scrollX = String2.parseInt(request.getParameter("scrollX"));
        int scrollY = String2.parseInt(request.getParameter("scrollY"));
        if (nSlides < 0 || nSlides > 250)   nSlides = 250;   //for all of these, consider mv-> MAX_VALUE
        if (scrollX < 0 || scrollX > 10000) scrollX = 0; 
        if (scrollY < 0 || scrollY > 10000) scrollY = 0; 

        //generate html response
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Slide Sorter", out); 
        try {
            writer.write(HtmlWidgets.dragDropScript(EDStatic.imageDirUrl(loggedInAs)));
            writer.write(EDStatic.youAreHereWithHelp(loggedInAs, "Slide Sorter", 
                "<div class=\"standard_max_width\">" + EDStatic.ssInstructionsHtml +
                "</div>")); 
            writer.write(HtmlWidgets.ifJavaScriptDisabled + "\n");

            //begin form
            HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
            widgets.enterTextSubmitsForm = false; 
            writer.write(widgets.beginForm(formName, "POST", //POST, not GET, because there may be lots of text   >1000 char
                tErddapUrl + "/slidesorter.html", "") + "\n");

            //gather slide title, url, x, y
            int newSlide = 0;
            int maxY = 150; //guess at header height
            StringBuilder addToJavaScript = new StringBuilder();
            StringBuilder otherSetDhtml = new StringBuilder();
            for (int oldSlide = 0; oldSlide <= nSlides; oldSlide++) { //yes <=
                String tTitle = oldSlide == nSlides? "" : request.getParameter("title" + oldSlide);
                String tUrl   = oldSlide == nSlides? "" : request.getParameter("url" + oldSlide);
                int tX = String2.parseInt(request.getParameter("x" + oldSlide));
                int tY = String2.parseInt(request.getParameter("y" + oldSlide));
                int tSize = String2.parseInt(request.getParameter("size" + oldSlide));
                if (reallyVerbose) String2.log("  found oldSlide=" + oldSlide + 
                    " title=\"" + tTitle + "\" url=\"" + tUrl + "\" x=" + tX + " y=" + tY);
                tTitle = tTitle == null? "" : tTitle.trim();
                tUrl   = tUrl == null? "" : tUrl.trim();
                String lcUrl = tUrl.toLowerCase();
                if (lcUrl.length() > 0 &&
                    !lcUrl.startsWith("file://") &&
                    !lcUrl.startsWith("ftp://") &&
                    !lcUrl.startsWith("http://") &&
                    !lcUrl.startsWith("https://") &&  
                    !lcUrl.startsWith("sftp://") &&   //??? will never work?
                    !lcUrl.startsWith("smb://")) {
                    tUrl = "http://" + tUrl;
                    lcUrl = tUrl.toLowerCase();
                }

                //delete this slide if it just has default info
                if (oldSlide < nSlides && 
                    tTitle.length() == 0 &&
                    tUrl.length() == 0)
                    continue;

                //clean up oldSlide's info
                //clean up tSize below
                if (tX < 0 || tX > 3000) tX = 0;
                if (tY < 0 || tY > 20000) tY = maxY;

                //pick apart tUrl
                int qPo = tUrl.indexOf('?');
                if (qPo < 0) qPo = tUrl.length();
                String preQ = tUrl.substring(0, qPo);
                String qAndPost = tUrl.substring(qPo);
                String lcQAndPost = qAndPost.toLowerCase();
                String ext = File2.getExtension(preQ);
                String lcExt = ext.toLowerCase();
                String preExt = preQ.substring(0, preQ.length() - ext.length());
                String pngExts[] = {".smallPng", ".png", ".largePng"};
                int pngSize = String2.indexOf(pngExts, ext);

                //create the slide's content
                String content = ""; //will be html
                int contentWidth = defaultContentWidth;  //default, in px    same size as erddap .png
                int contentHeight = 20;  //default (ok for 1 line of text) in px
                String contentCellStyle = "";

                String dataUrl = null;
                if (tUrl.length() == 0) {
                    if (tSize < 0 || tSize > 2) tSize = 1;
                    contentWidth = defaultContentWidth;
                    contentHeight = 20;                        
                    content = String2.ERROR + ": No URL has been specified.\n";

                } else if (lcUrl.startsWith("file://")) {
                    //local file on client's computer
                    //file:// doesn't work in iframe because of security restrictions:
                    //  so server doesn't look at a user's local file.
                    if (tSize < 0 || tSize > 2) tSize = 1;
                    contentWidth = defaultContentWidth;
                    contentHeight = 20;                        
                    content = String2.ERROR + ": 'file://' URL's aren't supported (for security reasons).\n";
                
                } else if ((tUrl.indexOf("/tabledap/") > 0 || tUrl.indexOf("/griddap/") > 0) &&
                    (ext.equals(".graph") || pngSize >= 0)) {
                    //Make A Graph
                    //change non-.png file type to .png
                    if (tSize < 0 || tSize > 2) {
                        //if size not specified, try to use pngSize; else tSize=1
                        tSize = pngSize >= 0? pngSize : 1;
                    }
                    ext = pngExts[tSize];
                    tUrl = preExt + pngExts[tSize] + qAndPost;

                    contentWidth  = EDStatic.imageWidths[tSize];
                    contentHeight = EDStatic.imageHeights[tSize];
                    content = "<img src=\"" + XML.encodeAsHTMLAttribute(tUrl) +  
                        "\" width=\"" + contentWidth + "\" height=\"" + contentHeight + 
                        "\" " + ssBePatientAlt + ">";
                    dataUrl = preExt + ".graph" + qAndPost;

                } else {
                    //all other types
                    if (tSize < 0 || tSize > 2) tSize = 1;

                    /* slideSorter used to contact the url!  That was a security risk. 
                       Don't allow users to tell ERDDAP what URLs to get info from!
                       Don't load user-specified images! (There was a Java bug related to this.)
                    */
                    if (lcExt.equals(".gif") ||
                        lcExt.equals(".png") ||
                        lcExt.equals(".jpeg") ||
                        lcExt.equals(".jpg")) {
                        //give all images a fixed square size  
                        contentWidth  = EDStatic.imageWidths[tSize];
                        contentHeight = contentWidth;
                    } else {
                        //everything else is html content
                        //sizes: small, wide, wide&high
                        contentWidth = EDStatic.imageWidths[tSize == 0? 1 : 2];
                        contentHeight = EDStatic.imageWidths[tSize == 2? 2 : tSize] * 3 / 4; //yes, widths; make wide
                    }
                    content = "<iframe src=\"" + XML.encodeAsHTMLAttribute(tUrl) + "\" " +
                        "style=\"width:" + contentWidth + "; height:" + contentHeight + "; " + 
                        "background:#FFFFFF;\" " +
                        ">Your browser does not support inline frames.</iframe>";

                }

                //write it all
                contentWidth = Math.max(150, contentWidth); //150 so enough for urlTextField+Submit
                int tW = contentWidth + 2 * border;
                writer.write(widgets.hidden("x" + newSlide, "" + tX));
                writer.write(widgets.hidden("y" + newSlide, "" + tY));
                writer.write(widgets.hidden("w" + newSlide, "" + tW));
                //writer.write(widgets.hidden("h" + newSlide, "" + tH));
                writer.write(widgets.hidden("size" + newSlide, "" + tSize));
                writer.write(

                    "<div id=\"div" + newSlide + "\" \n" +
                        "style=\"position:absolute; left:" + tX + "px; top:" + tY + "px; " + //no \n
                        "width:" + tW + "px; " +
                        "border:1px solid #555555; background:" + bgColor + "; overflow:hidden;\"> \n\n" +
                    //top border of gadget
                    "<table class=\"compact\" style=\"background-color:" + bgColor + ";\">\n" +
                    "<tr><td style=\"width:" + border + "px; height:" + border + "px;\"></td>\n" +
                    "  <td style=\"text-align:right\">\n\n");

                if (oldSlide < nSlides) {
                    //table for buttons
                    writer.write(  //width=20 makes it as narrow as possible
                        "  <table class=\"compact\" style=\"width:20px;\">\n" +
                        "  <tr>\n\n");

                    //data button
                    if (dataUrl != null) 
                        writer.write(
                        "   <td><img src=\"" + EDStatic.imageDirUrl(loggedInAs) + "data.gif\" alt=\"data\" \n" +
                        "      title=\"Edit the image or download the data in a new browser window.\" \n" +
                        "      style=\"cursor:default;\" \n" +  //cursor:hand doesn't work in Firefox
                        "      onClick=\"window.open('" + XML.encodeAsHTMLAttribute(dataUrl) + "');\" ></td>\n\n"); //open a new window 

                    //resize button
                    writer.write(
                        "   <td><img src=\"" + EDStatic.imageDirUrl(loggedInAs) + "resize.gif\" alt=\"s\" \n" +
                        "      title=\"Change between small/medium/large image sizes.\" \n" +
                        "      style=\"cursor:default;\" \n" +
                        "      onClick=\"" + dFormName + ".size" + newSlide + ".value='" + 
                               (tSize == 2? 0 : tSize + 1) + "'; \n" +
                        "        setHidden(); " + dFormName + ".submit();\"></td>\n\n");

                    //end button's table
                    writer.write(
                        "  </tr>\n" +
                        "  </table>\n\n");

                    //end slide top/center cell; start top/right 
                    writer.write(
                        "  </td>\n" +
                        "  <td style=\"width:" + border + "px; height:" + border + "px; text-align:right\">\n");
                }

                //delete button
                if (oldSlide < nSlides) 
                    writer.write(
                    "    <img src=\"" + EDStatic.imageDirUrl(loggedInAs) + "x.gif\" alt=\"x\" \n" +
                    "      title=\"Delete this slide.\" \n" +
                    "      style=\"cursor:default; width:" + border + "px; height:" + border + "px;\"\n" +
                    "      onClick=\"if (confirm('Delete this slide?')) {\n" +
                    "        " + dFormName + ".title" + newSlide + ".value=''; " + 
                                 dFormName + ".url" + newSlide + ".value=''; \n" +
                    "        setHidden(); " + dFormName + ".submit();}\">\n\n");
                writer.write(
                    "  </td>\n" +
                    "</tr>\n\n");

                //Add a Slide
                if (oldSlide == nSlides) 
                    writer.write(
                    "<tr><td>&nbsp;</td>\n" +
                    "  <td class=\"N\"><strong>Add a Slide</strong></td>\n" +
                    "</tr>\n\n");

                //gap
                writer.write(
                    "<tr><td style=\"height:" + gapPx + "\" colspan=\"2\"></td>\n" +
                    "  </tr>\n\n");

                //title text field
                String tPrompt = oldSlide == nSlides? "Title: " : "";
                int tWidth = contentWidth - 7*tPrompt.length() - 6;  // /7px=avg char width   6=border
                writer.write(
                    "<tr><td>&nbsp;</td>\n" +
                    "  <td class=\"N\">" + //no \n
                    "<strong>" + tPrompt + "</strong>");
                writer.write(widgets.textField("title" + newSlide, 
                    "Enter a title for the slide.", 
                    -1, //(contentWidth / 8) - tPrompt.length(),  // /8px=avg bold char width 
                    255, tTitle, 
                    "style=\"width:" + tWidth + "px; background:" + bgColor + "; font-weight:bold;\""));
                writer.write(
                    "</td>\n" + //no space before /td
                    "</tr>\n\n");

                //gap
                writer.write(
                    "<tr><td style=\"height:" + gapPx + "\" colspan=\"2\"></td>\n" +
                    "  </tr>\n\n");

                //content cell
                if (oldSlide < nSlides)
                    writer.write(
                    "<tr><td>&nbsp;</td>\n" +
                    "  <td id=\"cell" + newSlide + 
                        "\" style=\"vertical-align:top; width:" + contentWidth + 
                        "; height:" + contentHeight + ";\" " +
                        contentCellStyle +
                        " >" + //no \n
                    content +
                    "</td>\n" + //no space before /td
                    "</tr>\n\n");

                //gap
                if (oldSlide < nSlides)
                    writer.write(
                    "<tr><td style=\"height:" + gapPx + "\" colspan=\"2\"></td>\n" +
                    "  </tr>\n\n");

                //url text field
                tPrompt = oldSlide == nSlides? "URL:   " : ""; //3 sp make it's length() longer
                tWidth = contentWidth - 7*(tPrompt.length() + 10) - 6;  // /7px=avg char width   //10 for submit  //6=border
                writer.write(
                    "<tr><td>&nbsp;</td>\n" +
                    "  <td class=\"N\">" + //no \n
                    "<strong>" + tPrompt + "</strong>");
                writer.write(widgets.textField("url" + newSlide, 
                    "Enter a URL for the slide from ERDDAP's Make-A-Graph (or any URL).", 
                    -1, //(contentWidth / 7) - (tPrompt.length()-10),  // /7px=avg char width   10 for submit
                    1000, tUrl, 
                    "style=\"width:" + tWidth + "px; background:" + bgColor + "\""));
                //submit button (same row as URL text field)
                writer.write(widgets.button("button", "submit" + newSlide, 
                    "Click to submit the information on this page to the server.",
                    "Submit",  //button label
                    "style=\"cursor:default;\" onClick=\"setHidden(); " + dFormName + ".submit();\""));
                writer.write(
                    "</td>\n" + //no space before /td
                    "</tr>\n\n");

                //bottom border of gadget
                writer.write(
                    "<tr><td style=\"width:" + border + "px; height:" + border + "px;\" colspan=\"2\"></td></tr>\n" +
                    "</table>\n" +
                    "</div> \n" +
                    "\n");

                maxY = Math.max(maxY, tY + contentHeight + 3 * gap + 6 * border);  //5= 2borders, 1 title, 1 url, 2 dbl gap
                newSlide++;
            }
            writer.write(widgets.hidden("nSlides", "" + newSlide));
            //not important to save scrollXY, but important to have a place for setHidden to store changes
            writer.write(widgets.hidden("scrollX", "" + scrollX)); 
            writer.write(widgets.hidden("scrollY", "" + scrollY));

            //JavaScript
            //setHidden is called by widgets before submit() so position info is stored
            writer.write(
                "<script>\n" +
                "<!--\n" +
                "function setHidden() { \n" 
                //+ "alert('x0='+ dd.elements.div0.x);"
                );
            for (int i = 0; i < newSlide; i++) 
                writer.write(
                    "try {" +
                    dFormName + ".x" + i + ".value=dd.elements.div" + i + ".x; " +    
                    dFormName + ".y" + i + ".value=dd.elements.div" + i + ".y; " +  
                    dFormName + ".w" + i + ".value=dd.elements.div" + i + ".w; " + 
                    //dFormName + ".h+ " + i + ".value=dd.elements.div" + i + ".h; " +
                    "\n} catch (ex) {if (typeof(console) != 'undefined') console.log(ex.toString());}\n");
            writer.write(
                "try {" +
                dFormName + ".scrollX.value=dd.getScrollX(); " +    
                dFormName + ".scrollY.value=dd.getScrollY(); " +
                "\n} catch (ex) {if (typeof(console) != 'undefined') console.log(ex.toString());}\n" +
                "}\n");
            writer.write(
                "//-->\n" +
                "</script> \n");  

            //make space in document for slides, before end matter
            int nP = (maxY + 30) / 30;  // /30px = avg height of <p>&nbsp;  +30=round up
            for (int i = 0; i < nP; i++) 
                writer.write("<p>&nbsp;\n");
            writer.write("<div class=\"standard_max_width\">\n"); 
            writer.write("<p>");
            writer.write(widgets.button("button", "submit" + newSlide, 
                "Click to submit the information on this page to the server.",
                "Submit",  //button label
                "style=\"cursor:default;\" onClick=\"setHidden(); " + dFormName + ".submit();\""));
            writer.write("<a class=\"selfLink\" id=\"instructions\" href=\"#instructions\" rel=\"bookmark\">&nbsp;</a><p>");
            writer.write(EDStatic.ssInstructionsHtml);
            writer.write("</div>\n"); 

            //end form
            writer.write(widgets.endForm());        
            writer.write("<div class=\"standard_max_width\">\n"); 

            //write the end stuff / set up drag'n'drop
            writer.write(
                "<script>\n" +
                "<!--\n" +
                "SET_DHTML(CURSOR_MOVE"); //the default cursor for the div's
            for (int i = 0; i < newSlide; i++) 
                writer.write(",\"div" + i + "\""); 
            writer.write(otherSetDhtml.toString() + ");\n");
            for (int i = 0; i < newSlide; i++) 
                writer.write("dd.elements.div" + i + ".setZ(" + i + "); \n");
            writer.write(
                "window.scrollTo(" + scrollX + "," + scrollY + ");\n" +
                addToJavaScript.toString() +
                "//-->\n" +
                "</script>\n");

            //alternatives
            writer.write("\n<hr>\n" +
                "<h2><a class=\"selfLink\" id=\"alternatives\" href=\"#alternatives\" rel=\"bookmark\">Alternatives to Slide Sorter</a></h2>\n" +
                "<ul>\n" +
                "<li>Web page authors can \n" +
                "  <a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/images/embed.html\">embed a graph with the latest data in a web page</a> \n" +
                "  using HTML &lt;img&gt; tags.\n" +
                //"<li>Anyone can use or make \n" +
                //"  <a rel=\"help\" href=\"https://coastwatch.pfeg.noaa.gov/erddap/images/gadgets/GoogleGadgets.html\">Google " +
                //  "Gadgets</a> to display graphs of the latest data.\n" +
                "</ul>\n" +
                "\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of slidesorter
            writer.write("</div>\n"); 
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
        
    }

    /**
     * This responds to a full text search request.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "search") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doSearch(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String protocol, int datasetIDStartsAt, String userQuery) throws Throwable {
        
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String fileTypeName = "";
        String searchFor = "";
        /* retired 2017-10-27 String youAreHereTable =             
            //EDStatic.youAreHere(loggedInAs, protocol);
            getYouAreHereTable(
                EDStatic.youAreHere(loggedInAs, protocol),
                //Or, View All Datasets
                //"<br>Or, <a href=\"" + tErddapUrl + "/info/index.html" +
                //    EDStatic.encodedPassThroughPIppQueryPage1(request) + "\">" +
                //EDStatic.viewAllDatasetsHtml + "</a>\n" +
                ////Or, search by category
                //"<p>" + getCategoryLinksHtml(request, tErddapUrl) +
                ////Use <p> below if other options are enabled.
                EDStatic.orRefineSearchWith + 
                    getAdvancedSearchLink(loggedInAs, userQuery));  //??? how ensure it has PIppQuery?
            */
        String preText = "<span style=\"font-size:150%;\"><strong>";
        String postText = ":&nbsp;</strong></span>\n<br>";

        try {
            
            //respond to search.html request
            String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
                requestUrl.substring(datasetIDStartsAt);

            //get the 'searchFor' value
            searchFor = request.getParameter("searchFor");
            searchFor = searchFor == null? "" : searchFor.trim();

            //redirect to index.html
            if (endOfRequestUrl.equals("") ||
                endOfRequestUrl.equals("index.htm")) {
                sendRedirect(response, tErddapUrl + "/" + protocol + "/index.html?" +
                    EDStatic.passThroughPIppQueryPage1(request) +
                    (searchFor.length() == 0? "" : "&searchFor=" + SSR.minimalPercentEncode(searchFor)));
                return;
            }           
            
            //ensure query has simplistically valid page= itemsPerPage=
            if (!Arrays.equals(
                    EDStatic.getRawRequestedPIpp(request),
                    EDStatic.getRequestedPIpp(request))) {
                sendRedirect(response, 
                    EDStatic.baseUrl(loggedInAs) + request.getRequestURI() + "?" +
                    EDStatic.passThroughJsonpQuery(request) +
                    EDStatic.passThroughPIppQuery(request) + 
                    (searchFor.length() == 0? "" : "&searchFor=" + SSR.minimalPercentEncode(searchFor)));
                return;
            }      

            fileTypeName = File2.getExtension(endOfRequestUrl); //eg ".html"
            boolean toHtml = fileTypeName.equals(".html");
            if (reallyVerbose) String2.log("  searchFor=" + searchFor + 
                "\n  fileTypeName=" + fileTypeName);
            EDStatic.tally.add("Search For (since startup)", searchFor);
            EDStatic.tally.add("Search For (since last daily report)", searchFor);
            EDStatic.tally.add("Search File Type (since startup)", fileTypeName);
            EDStatic.tally.add("Search File Type (since last daily report)", fileTypeName);

            if (endOfRequestUrl.equals("index.html")) {
                if (searchFor.length() == 0) 
                    throw new Exception("show index"); //show form below
                //else handle just below here
            } else if (endsWithPlainFileType(endOfRequestUrl, "index")) {
                if (searchFor.length() == 0) {
                    sendResourceNotFoundError(request, response, //or SC_NO_CONTENT error?
                        MessageFormat.format(EDStatic.searchWithQuery, fileTypeName));
                    return;
                }
                //else handle just below here
            } else { //usually unsupported fileType
                sendResourceNotFoundError(request, response, "unsupported endOfRequestUrl");
                return;
            }

            //do the search (also, it ensures user has right to know dataset exists)
            //(result may be size=0)
            StringArray datasetIDs = getSearchDatasetIDs(loggedInAs, 
                allDatasetIDs(), searchFor);
            int nMatches = datasetIDs.size();

            //calculate Page ItemsPerPage (part of: full text search)
            int pIpp[] = EDStatic.calculatePIpp(request, nMatches);
            int page         = pIpp[0]; //will be 1... 
            int itemsPerPage = pIpp[1]; //will be 1...
            int startIndex   = pIpp[2]; //will be 0...
            int lastPage     = pIpp[3]; //will be 1...

            //reduce datasetIDs to ones on requested page
            //IMPORTANT!!! For this to work correctly, datasetIDs must be
            //  accessibleTo loggedInAs (or EDStatic.listPrivateDatasets)
            //  and in final sorted order.
            //  (True here)
            //Order of removal: more efficient to remove items at end, then items at beginning.
            if (startIndex + itemsPerPage < nMatches) 
                datasetIDs.removeRange(startIndex + itemsPerPage, nMatches);
            datasetIDs.removeRange(0, Math.min(startIndex, nMatches));

            int datasetIDSize = datasetIDs.size();  //may be 0        

            //error messages
            String error[] = null;
            if (nMatches == 0) {
                error = EDStatic.noSearchMatch(searchFor);
            } else if (page > lastPage) {
                error = EDStatic.noPage(page, lastPage);
            }


            //show the results as an .html file 
            boolean sortByTitle = false; //sorted above
            if (fileTypeName.equals(".html")) { 
                //display start of web page
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, EDStatic.searchTitle, out); 
                try {
                    //you are here    Search
                    writer.write(
                        "<div class=\"standard_width\">\n" +
                        EDStatic.youAreHere(loggedInAs, protocol));
                        //youAreHereTable);

                    //display the search form
                    writer.write(getSearchFormHtml(request, loggedInAs, 
                        preText, postText, searchFor) +
                        "&nbsp;" +
                        "<br>");

                    //display datasets
//                    writer.write(
//                        "<br>&nbsp;\n" +
//                        "<h2>" + EDStatic.resultsOfSearchFor + " <kbd>" + 
                        //encodeAsHTML(searchFor) is essential -- to prevent Cross-site-scripting security vulnerability
                        //(which allows hacker to insert his javascript into pages returned by server)
                        //See Tomcat (Definitive Guide) pg 147
//                        XML.encodeAsHTML(searchFor) + "</kbd></h2>\n");  
                    if (error == null) {

                        Table table = makeHtmlDatasetTable(loggedInAs, datasetIDs, sortByTitle);

                        String nMatchingHtml = EDStatic.nMatchingDatasetsHtml(
                            nMatches, page, lastPage, true, //=most relevant first
                            EDStatic.baseUrl(loggedInAs) + requestUrl + 
                            EDStatic.questionQuery(request.getQueryString()));
                                
                        writer.write(
                            nMatchingHtml + "\n" +
                            "<span class=\"N\">(" + EDStatic.orRefineSearchWith + 
                                getAdvancedSearchLink(loggedInAs, userQuery) + ")</span>\n" +
                            "<br>&nbsp;\n"); //necessary for the blank line before the table (not <p>)

                        table.saveAsHtmlTable(writer, "commonBGColor", null, 1, false, -1, 
                            false, false); //don't encodeAsHTML the cell's contents, !allowWrap

                        if (lastPage > 1)
                            writer.write("\n<p>" + nMatchingHtml);

                        //list plain file types
                        writer.write(
                            "\n" +
                            "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                            plainFileTypesString + //not links, which would be indexed by search engines
                            ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                                EDStatic.restfulViaService + "</a>.\n");
                            
                    } else {
                        //error
                        writer.write(
                            "<strong>" + XML.encodeAsHTML(error[0]) + "</strong>\n" +
                            "<br>" + XML.encodeAsHTML(error[1]) + "\n");
                    }


                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(t));
                    throw t; 
                } finally {

                    //end of document
                    writer.write("</div>\n");
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }
                return;
            }

            //show the results in other file types
            if (error != null)
                throw new SimpleException(error[0] + " " + error[1]);

            Table table = makePlainDatasetTable(loggedInAs, datasetIDs, sortByTitle, fileTypeName);
            sendPlainTable(loggedInAs, request, response, table, protocol, fileTypeName);
            return;

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            //deal with search error (or just need empty .html searchForm)
            OutputStream out = null;
            Writer writer = null;

            //catch errors after the response has begun
            if (response.isCommitted()) 
                throw t; //rethrown exception (will be handled in doGet try/catch)

            if (String2.indexOf(plainFileTypes, fileTypeName) >= 0) 
                //for plainFileTypes, rethrow the error
                throw t;

            //make html page with [error message and] search form
            String error = MustBe.getShortErrorMessage(t);
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(loggedInAs, EDStatic.searchTitle, out);
            try {
                //you are here      Search
                writer.write(
                    "<div class=\"standard_width\">\n" +
                    EDStatic.youAreHere(loggedInAs, protocol));
                    //youAreHereTable);

                //write (error and) search form
                if (error.indexOf("show index") < 0) 
                    writeErrorHtml(writer, request, error);
                writer.write(getSearchFormHtml(request, loggedInAs, 
                    "<span style=\"font-size:150%;\"><strong>", ":&nbsp;</strong></span>",
                    searchFor));
                String2.log(String2.ERROR + " message sent to user: " + error);
                String2.log(MustBe.throwableToString(t));

                //writer.write(
                //    "<p>&nbsp;<hr>\n" +
                //    String2.replaceAll(EDStatic.restfulSearchService, "&erddapUrl;", tErddapUrl) +
                //    "\n");

            } catch (Throwable t2) {
                EDStatic.rethrowClientAbortException(t2);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t2));
                throw t2; 
            } finally {
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }
    }

    /**
     * This responds to OpenSearch requests.
     * https://github.com/dewitt/opensearch#what-is-opensearch
     * (Bob has a copy of the specification in F:/programs/opensearch/1.1.htm )
     *
     * <p>A sample OpenSearch website is https://www.nature.com/opensearch/
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param protocol  Currently, just "opensearch1.1" is supported. Others may be added in future.
     * @param pageNameStartsAt is the position right after the / at the end of the protocol
     *    (always "search") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doOpenSearch(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String protocol, int pageNameStartsAt, String userQuery) throws Throwable {
        
        String tErddapUrl     = EDStatic.erddapUrl(loggedInAs);
        String requestUrl     = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String descriptionUrl = tErddapUrl + "/" + protocol + "/description.xml";
        String serviceWord    = "search";
        String serviceUrl     = tErddapUrl + "/" + protocol + "/" + serviceWord;
        String tImageDirUrl   = loggedInAs == null? EDStatic.imageDirUrl : EDStatic.imageDirHttpsUrl;
        String niceProtocol   = "OpenSearch 1.1";

        String endOfRequestUrl = pageNameStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(pageNameStartsAt);
        String xmlThisRequest = XML.encodeAsHTMLAttribute(serviceUrl + EDStatic.questionQuery(userQuery));

        //search standard_names for a good exampleSearchTerm
        String exampleSearchTerm = "datasetID";   //default    latitude?
        int snpo = String2.indexOf(EDStatic.categoryAttributes, "standard_name");
        if (snpo >= 0) {  //"standard_name" is a categoryAttribute
            String tryTerms[] = {"temperature", "wind", "salinity", "pressure", 
                "chlorophyll", "sea", "water", "atmosphere", "air"};
            StringArray sNames = categoryInfo("standard_name");
            for (int tt = 0; tt < tryTerms.length; tt++) {
                int ttpo[] = {0, 0}; //start/result   [0]=index, [1]=po
                if (sNames.indexWith(tryTerms[tt], ttpo)[0] >= 0) {
                    exampleSearchTerm = tryTerms[tt];
                    break;
                }
            }
        }
        String sampleUrl = serviceUrl + "?" +
            EDStatic.encodedDefaultPIppQuery + "&#x26;searchTerms=" + exampleSearchTerm;

        //*** respond to /index.html
        if (endOfRequestUrl.equals("index.html")) {
            //display start of web page
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, niceProtocol, out); 
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, niceProtocol) +
                "<p><a rel=\"bookmark\" href=\"https://github.com/dewitt/opensearch#what-is-opensearch\">" + niceProtocol + "" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
                "is a standard way for search engines at other websites to search this\n" +
                "ERDDAP for interesting datasets.\n" +
                "<ul>\n" +
                "<li>To set up your website's search engine to search for datasets via " + 
                    niceProtocol + ",\n" +
                "  point your " + niceProtocol + " client to the " + niceProtocol + 
                    " service description document at\n" +
                "  <br><a href=\"" + descriptionUrl + "\">" + 
                                     descriptionUrl + "</a>\n" +
                "  <br>&nbsp;\n" +
                //"<li>The " + niceProtocol + " search service is at\n" +
                //"  <br><a href=\"" + serviceUrl + "\">" + 
                //                     serviceUrl + "</a>\n" +
                //"  <br>&nbsp;\n" +
                "<li>A sample " + niceProtocol + " request for an Atom response is\n" +
                "  <br><a href=\"" + sampleUrl + "&#x26;format=atom\">" + 
                                     sampleUrl + "&#x26;format=atom</a>\n" +
                "  <br>&nbsp;\n" +
                "<li>A sample " + niceProtocol + " request for an RSS response is\n" +
                "  <br><a href=\"" + sampleUrl + "&#x26;format=rss\">" + 
                                     sampleUrl + "&#x26;format=rss</a>\n" +
                "</ul>\n" +
                "\n" +
                "<p>If you aren't setting up a website that uses " + niceProtocol + ", please use ERDDAP's regular\n" +
                "search options on the right hand side of\n" +
                "  <a rel=\"start\" href=\"" + tErddapUrl + "/index.html\">ERDDAP's home page</a>.\n" +
                "Developers of computer programs and JavaScripted web pages can access ERDDAP's regular\n" +
                "search options as\n" +
                "  <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">RESTful services</a>.\n" +
                "</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
            return;
        }      

        //*** respond to /description.xml 
        if (endOfRequestUrl.equals("description.xml")) {
            //extract the unique single keywords from EDStatic.keywords,
            //and make them space separated
            //??? or do this from all keywords of all datasets?
            String tKeywords = EDStatic.keywords.toLowerCase();
            tKeywords = String2.replaceAll(tKeywords, '>', ' ');
            tKeywords = String2.replaceAll(tKeywords, '|', ' ');
            tKeywords = String2.replaceAll(tKeywords, '\"', ' ');
            StringArray tKeywordsSA = StringArray.wordsAndQuotedPhrases(tKeywords);
            tKeywordsSA = (StringArray)tKeywordsSA.makeIndices(new IntArray()); //unique words
            tKeywords = String2.toSVString(tKeywordsSA.toArray(), " ", false);

            OutputStream out = (new OutputStreamFromHttpResponse(
                request, response, "OpenSearchDescription", 
                    "custom:application/opensearchdescription+xml", ".xml")).
                outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                String template = "?searchTerms={searchTerms}&#x26;page={startPage?}" +
                                  "&#x26;itemsPerPage={count?}";
                writer.write(  
//items are in the order they are described in OpenSearch specification
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//see xmlns example at https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md#namespace
"<OpenSearchDescription xmlns=\"http://a9.com/-/spec/opensearch/1.1/\">\n" +  //A URL, not a document
"  <ShortName>ERDDAP</ShortName>\n" +  //<=16 chars
"  <Description>The ERDDAP at " + XML.encodeAsXML(EDStatic.adminInstitution) + //<=1024 chars
" is a data server that gives you a simple, consistent way to download subsets of " +
"scientific datasets in common file formats and make graphs and maps.</Description>\n" + 
"  <Url type=\"application/atom+xml\"\n" +
"       template=\"" + serviceUrl + template + "&#x26;format=atom\"/>\n" +
"  <Url type=\"application/rss+xml\"\n" +
"       template=\"" + serviceUrl + template + "&#x26;format=rss\"/>\n" +
"  <Contact>" + XML.encodeAsXML(EDStatic.adminEmail) + "</Contact>\n" +
"  <Tags>" + XML.encodeAsXML(tKeywords) + "</Tags>\n" +
"  <LongName>ERDDAP" + //<=48 characters   
XML.encodeAsXML(EDStatic.adminInstitution.length() <= 38? " at " + EDStatic.adminInstitution : "") +
"</LongName>\n" +   
"  <Image height=\"" + EDStatic.googleEarthLogoFileHeight  + "\" " + //preferred image first
      "width=\"" + EDStatic.googleEarthLogoFileWidth   + "\">" + //type=\"image/png\" is optional
  XML.encodeAsXML(
    tImageDirUrl + EDStatic.googleEarthLogoFile)       + "</Image>\n" +
"  <Image height=\"" + EDStatic.highResLogoImageFileHeight + "\" " +
      "width=\"" + EDStatic.highResLogoImageFileWidth  + "\">" + 
  XML.encodeAsXML(
    tImageDirUrl + EDStatic.highResLogoImageFile)      + "</Image>\n" +
"  <Image height=\"" + EDStatic.lowResLogoImageFileHeight  + "\" " +
      "width=\"" + EDStatic.lowResLogoImageFileWidth   + "\">" + 
  XML.encodeAsXML(
    tImageDirUrl + EDStatic.lowResLogoImageFile)       + "</Image>\n" +
//???need more and better examples
"  <Query role=\"example\" searchTerms=\"" + XML.encodeAsXML(exampleSearchTerm) + "\" />\n" +
"  <Developer>Bob Simons (bob.simons at noaa.gov)</Developer>\n" + //<=64 chars
"  <Attribution>" +  //credit for search results    <=256 chars
XML.encodeAsXML(String2.noLongerThanDots(EDStatic.adminInstitution, 256)) + "</Attribution>\n" +
"  <SyndicationRight>" + (loggedInAs == null? "open" : "private") + "</SyndicationRight>\n" +
"  <AdultContent>false</AdultContent>\n" +
"  <Language>en-us</Language>\n" +  //language could change if messages.xml is translated
"  <InputEncoding>UTF-8</InputEncoding>\n" +
"  <OutputEncoding>UTF-8</OutputEncoding>\n" +
"</OpenSearchDescription>\n");
            } finally {
                writer.close(); //it calls writer.flush then out.close();         
            }
            return;
        }

        //redirect anything other than /search to /index.html
        //???or page not found?
        if (!endOfRequestUrl.equals(serviceWord)) {
            sendRedirect(response, tErddapUrl + "/" + protocol + "/index.html");
            return;
        }                    

        //*** handle /search request 
        //get the 'searchTerms' value
        String searchTerms = request.getParameter("searchTerms");
        searchTerms = searchTerms == null? "" : searchTerms.trim();
        EDStatic.tally.add("OpenSearch searchTerms (since startup)", searchTerms);
        EDStatic.tally.add("OpenSearch searchTerms (since last daily report)", searchTerms);
        String xmlSearchTerms = XML.encodeAsXML(searchTerms);
        String pctSearchTerms = SSR.minimalPercentEncode(searchTerms);
        String xmlPctSearchTerms = XML.encodeAsXML(SSR.minimalPercentEncode(searchTerms));

        //format  
        String format = request.getParameter("format");
        format = format == null? "" : format.trim();
        if (!format.equals("atom"))
            format = "rss"; //default

        if (reallyVerbose) String2.log("  OpenSearch format=" + format + " terms=" + searchTerms);

        //do the search  (also, it ensures user has right to know dataset exists) 
        //nMatches may be 0
        StringArray datasetIDs = getSearchDatasetIDs(loggedInAs, 
            allDatasetIDs(), searchTerms);
        int nMatches = datasetIDs.size();

        //calculate Page ItemsPerPage (part of: openSearch)
        int pIpp[] = EDStatic.calculatePIpp(request, nMatches);
        int page         = pIpp[0]; //will be 1... 
        int itemsPerPage = pIpp[1]; //will be 1...
        int startIndex   = pIpp[2]; //will be 0...
        int lastPage     = pIpp[3]; //will be 1...

        //reduce datasetIDs to ones on requested page
        //IMPORTANT!!! For this to work correctly, datasetIDs must be 
        //  accessibleTo loggedInAs (or EDStatic.listPrivateDatasets)
        //  and in final sorted order.
        //  (True here)
        //Order of removal: more efficient to remove items at end, then items at beginning.
        if (startIndex + itemsPerPage < nMatches) 
            datasetIDs.removeRange(startIndex + itemsPerPage, nMatches);
        datasetIDs.removeRange(0, Math.min(startIndex, nMatches));

        int datasetIDSize = datasetIDs.size();  //may be 0        

        //error messages
        String title0 = "";
        String msg0 = "";
        if (nMatches == 0) {
            String sar[] = EDStatic.noSearchMatch(searchTerms);
            title0 = sar[0];
            msg0   = sar[1];
        } else if (page > lastPage) {
            String sar[] = EDStatic.noPage(page, lastPage);
            title0 = sar[0];
            msg0   = sar[1];
        }


        //*** return results as Atom 
        // see https://www.ietf.org/rfc/rfc4287.txt
        // which I have in f:/projects/atom
        if (format.equals("atom")) { 
            OutputStreamSource outSource = new OutputStreamFromHttpResponse(
                request, response, "OpenSearchResults", 
                    "custom:application/atom+xml", ".xml"); 
            OutputStream out = outSource.outputStream(String2.UTF_8); 
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                long lastMajorLoadMillis = runLoadDatasets.lastMajorLoadDatasetsStopTimeMillis;
                if (lastMajorLoadMillis == 0)
                    lastMajorLoadMillis = runLoadDatasets.lastMajorLoadDatasetsStartTimeMillis;
                String thisRequestNoPage = serviceUrl + "?searchTerms=" + pctSearchTerms +
                    "&format=atom&itemsPerPage=" + itemsPerPage + "&page=";

                writer.write(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<feed xmlns=\"https://www.w3.org/2005/Atom\" \n" +
                "      xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\">\n" + //it's a URI. Don't change it.
                "  <title>ERDDAP Search: " + xmlSearchTerms + "</title> \n" +
                "  <link href=\"" + xmlThisRequest + "\"/>\n" +
                "  <updated>" + Calendar2.millisToIsoStringTZ(lastMajorLoadMillis) +
                      "</updated>\n" +
                "  <author> \n" +
                "    <name>"  + XML.encodeAsXML(EDStatic.adminIndividualName) + "</name>\n" +
                "    <email>" + XML.encodeAsXML(EDStatic.adminEmail) + "</email>\n" +
                "  </author> \n" +
                //id shouldn't change if server's url changes or if served by another erddap.
                //But I'm just using a simpler system which is universally unique: the request url
                //It is okay that response to this request may change in time. It is still the same request.
                "  <id>" + xmlThisRequest + "</id>\n" +
                "  <opensearch:totalResults>" + nMatches + "</opensearch:totalResults>\n" +
                "  <opensearch:startIndex>" + (startIndex + 1) + "</opensearch:startIndex>\n" +
                "  <opensearch:itemsPerPage>" + itemsPerPage + "</opensearch:itemsPerPage>\n" +
                "  <opensearch:Query role=\"request\" searchTerms=\"" + 
                    xmlSearchTerms + "\" startPage=\"" + page + "\" />\n" +
                //"  <link rel=\"alternate\" href=\"http://example.com/New+York+History?pw=3\" type=\"text/html\"/>\n" +
                "  <link rel=\"first\" href=\"" +
                    XML.encodeAsXML(thisRequestNoPage) + 1          + "\" type=\"application/atom+xml\"/>\n" +
                (page == 1? "" :
                "  <link rel=\"previous\" href=\"" +
                    XML.encodeAsXML(thisRequestNoPage) + (page - 1) + "\" type=\"application/atom+xml\"/>\n") +
                "  <link rel=\"self\" href=\"" +
                    XML.encodeAsXML(thisRequestNoPage) + page       + "\" type=\"application/atom+xml\"/>\n" +
                (page >= lastPage? "" :
                "  <link rel=\"next\" href=\"" +
                    XML.encodeAsXML(thisRequestNoPage) + (page + 1) + "\" type=\"application/atom+xml\"/>\n") +
                "  <link rel=\"last\" href=\"" +
                    XML.encodeAsXML(thisRequestNoPage) + lastPage   + "\" type=\"application/atom+xml\"/>\n");
                //"  <link rel=\"search\" type=\"application/opensearchdescription+xml\" href=\"http://example.com/opensearchdescription.xml\"/>\n" +

                //0 results, see Best Practices at website?
                if (datasetIDSize == 0) {
                    writer.write(
                        "  <entry>\n" +
                        "    <title>" + XML.encodeAsXML(title0) + "</title>\n" +
                        "    <link href=\"" + tErddapUrl + "/" + protocol + "/index.html\"/>\n" +
                        "    <id>" + String2.ERROR + "</id>\n" +
                        "    <updated>" + Calendar2.millisToIsoStringTZ(lastMajorLoadMillis) +
                            "</updated>\n" +
                        "    <content type=\"text\">" + XML.encodeAsXML(msg0) + "</content>\n" +
                        "  </entry>\n");
                }

                for (int row = 0; row < datasetIDSize; row++) {
                    String tDatasetID = datasetIDs.get(row);
                    EDD edd = gridDatasetHashMap.get(tDatasetID);
                    if (edd == null) {
                        edd = tableDatasetHashMap.get(tDatasetID);
                        if (edd == null) {
                            String2.log("  OpenSearch Warning: datasetID=" + tDatasetID + " not found!");
                            continue;
                        }
                    }
                    writer.write(
                        "  <entry>\n" +
                        "    <title>" + XML.encodeAsXML(edd.title()) + "</title>\n" +
                        "    <link href=\"" + tErddapUrl + "/" + edd.dapProtocol() + "/" + tDatasetID + ".html\"/>\n" +
                        //<id> shouldn't change if server's url changes or if served by another erddap.
                        //But I'm just using a simpler system which is universally unique: the dataset's url
                        //It is okay that the dataset will change in time. It is still the same dataset.
                        "    <id>" + tErddapUrl + "/" + edd.dapProtocol() + "/" + tDatasetID + ".html</id>\n" +
                        "    <updated>" + Calendar2.millisToIsoStringTZ(edd.creationTimeMillis()) +
                            "</updated>\n" +
                        "    <content type=\"text\">\n" + XML.encodeAsXML(edd.extendedSummary()) +                           
                        "    </content>\n" +
                        "  </entry>\n");
                }

                writer.write(
                    "</feed>\n");
            } finally {
                writer.close(); //it calls writer.flush then out.close();         
            }
            return;
        }


        //*** else return results as RSS 
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "OpenSearchResults", "custom:application/rss+xml", ".xml"); 
        OutputStream out = outSource.outputStream(String2.UTF_8); 
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
        try {
            writer.write(
            //see https://cyber.harvard.edu/rss/examples/rss2sample.xml which is simpler
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<rss version=\"2.0\" \n" +
            "  xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\"\n" +  //it's a URI. Don't change it.
            "  xmlns=\"http://backend.userland.com/rss2\">\n" + //no longer needed? Or change to ...?
            "  <channel>\n" +
            "    <title>ERDDAP Search: " + xmlSearchTerms + "</title>\n" +
            "    <link>" + xmlThisRequest + "</link>\n" +
            "    <description>ERDDAP Search results for \"" + 
                xmlSearchTerms + "\" at " + tErddapUrl + "</description>\n" +
            "    <opensearch:totalResults>" + nMatches + "</opensearch:totalResults>\n" +
            "    <opensearch:startIndex>" + (startIndex + 1) + "</opensearch:startIndex>\n" +
            "    <opensearch:itemsPerPage>" + itemsPerPage + "</opensearch:itemsPerPage>\n" +
            "    <atom:link rel=\"search\" type=\"application/opensearchdescription+xml\"" +
                 " href=\"" + serviceUrl + "\"/>\n" +
            "    <opensearch:Query role=\"request\" searchTerms=\"" + 
                xmlPctSearchTerms + "\" startPage=\"" + page + "\"/>\n");
            
            //0 results, see Best Practices at website?
            if (datasetIDSize == 0) {
                writer.write(
                    "    <item>\n" +
                    "      <title>" + XML.encodeAsXML(title0) + "</title>\n" +
                    "      <link>" + tErddapUrl + "/" + protocol + "/index.html</link>\n" +
                    "      <description>" + XML.encodeAsXML(msg0) + "</description>\n" +
                    "    </item>\n");
            }

            for (int row = 0; row < datasetIDSize; row++) {
                String tDatasetID = datasetIDs.get(row);
                EDD edd = gridDatasetHashMap.get(tDatasetID);
                if (edd == null) {
                    edd = tableDatasetHashMap.get(tDatasetID);
                    if (edd == null) {
                        String2.log("  OpenSearch Warning: datasetID=" + tDatasetID + " not found!");
                        continue;
                    }
                }
                writer.write(
                    "    <item>\n" +
                    "      <title>" + XML.encodeAsXML(edd.title()) + "</title>\n" +
                    "      <link>" + tErddapUrl + "/" + edd.dapProtocol() + "/" + tDatasetID + ".html</link>\n" +
                    "      <description>\n" + XML.encodeAsXML(edd.extendedSummary()) + 
                          "</description>\n" +
                    "    </item>\n");
            }

            writer.write(
                "  </channel>\n" +
                "</rss>\n");
        } finally {
            writer.close(); //it calls writer.flush then out.close();         
        }
    }

    /**
     * This writes the link to Advanced Search page.
     *
     * @param loggedInAs
     * @param paramString the param string (after "?")
     *    (starting point for advanced search, already percent encoded, but not XML/HTML encoded) 
     *    (or "" or null)
     *    but should at least have page= and itemsPerPage= (PIppQuery).
     */
    public String getAdvancedSearchLink(String loggedInAs, String paramString) throws Throwable {

        return 
            "<span class=\"N\"><a href=\"" + 
                XML.encodeAsHTMLAttribute(EDStatic.erddapUrl(loggedInAs) + "/search/advanced.html" +
                EDStatic.questionQuery(paramString)) +
            "\">" + String2.replaceAll(EDStatic.advancedSearch, " ", "&nbsp;") + "</a>&nbsp;" +
            EDStatic.htmlTooltipImage(loggedInAs, 
                "<div class=\"narrow_max_width\">" + EDStatic.advancedSearchTooltip +
                "</div>") + 
            "</span>";
    }


    /**
     * This responds to a advanced search request: erddap/search/advanced.html, 
     * and other extensions.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "search") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     */
    public void doAdvancedSearch(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, int datasetIDStartsAt, String userQuery) throws Throwable {
        
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String fileTypeName = "";
        String catAtts[]       = EDStatic.categoryAttributes;
        String catAttsInURLs[] = EDStatic.categoryAttributesInURLs;
        int    nCatAtts = catAtts.length;
        String ANY = "(ANY)";  //don't translate so consistent on all erddaps?
        String searchFor = "";
          
        //respond to /search/advanced.xxx request
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt); //e.g., advanced.json

        //ensure url is valid  
        if (!endOfRequestUrl.equals("advanced.html") &&
            !endsWithPlainFileType(endOfRequestUrl, "advanced")) {
            //unsupported fileType
            if (verbose) String2.log(EDStatic.resourceNotFound + " !advanced");
            sendResourceNotFoundError(request, response, "");
            return;
        }

        //get the parameters, e.g., the 'searchFor' value
        //parameters are "" if unused (not null)
        searchFor = request.getParameter("searchFor");
        searchFor = searchFor == null? "" : searchFor.trim();

        //ensure query has page=   
        //(different from similar places because there may be many other params)
        if (request.getParameter("page") == null) {
            if (userQuery == null)
                userQuery = "";
            if (request.getParameter("itemsPerPage") == null) 
                userQuery = "itemsPerPage=" + EDStatic.defaultItemsPerPage + 
                    (userQuery.length() == 0? "" : "&" + userQuery);
            userQuery = "page=1&" + userQuery;
            sendRedirect(response, 
                EDStatic.baseUrl(loggedInAs) + request.getRequestURI() + "?" + 
                    userQuery);
            return;
        }              
        int pipp[] = EDStatic.getRequestedPIpp(request);

        EDStatic.tally.add("Advanced Search, Search For (since startup)", searchFor);
        EDStatic.tally.add("Advanced Search, Search For (since last daily report)", searchFor);

        //boundingBox
        double minLon = String2.parseDouble(request.getParameter("minLon"));
        double maxLon = String2.parseDouble(request.getParameter("maxLon"));
        double minLat = String2.parseDouble(request.getParameter("minLat"));
        double maxLat = String2.parseDouble(request.getParameter("maxLat"));
        if (!Double.isNaN(minLon) && !Double.isNaN(maxLon) && minLon > maxLon) {
            double td = minLon; minLon = maxLon; maxLon = td; }
        if (!Double.isNaN(minLat) && !Double.isNaN(maxLat) && minLat > maxLat) {
            double td = minLat; minLat = maxLat; maxLat = td; }
        boolean llc = Double.isFinite(minLon) || Double.isFinite(maxLon) ||
                      Double.isFinite(minLat) || Double.isFinite(maxLat);
        EDStatic.tally.add("Advanced Search with Lat Lon Constraints (since startup)", "" + llc);
        EDStatic.tally.add("Advanced Search with Lat Lon Constraints (since last daily report)", "" + llc);

        String minTimeParam = request.getParameter("minTime");
        String maxTimeParam = request.getParameter("maxTime");
        if (minTimeParam == null) minTimeParam = "";
        if (maxTimeParam == null) maxTimeParam = "";
        double minTimeD = 
            minTimeParam.toLowerCase().startsWith("now")?
                Calendar2.safeNowStringToEpochSeconds(minTimeParam, Double.NaN) :
            String2.isNumber(minTimeParam)?
                String2.parseDouble(minTimeParam) : 
                Calendar2.safeIsoStringToEpochSeconds(minTimeParam);
        double maxTimeD = 
            maxTimeParam.toLowerCase().startsWith("now")?
                Calendar2.safeNowStringToEpochSeconds(maxTimeParam, Double.NaN) :
            String2.isNumber(maxTimeParam)?
                String2.parseDouble(maxTimeParam) :
                Calendar2.safeIsoStringToEpochSeconds(maxTimeParam);
        if (!Double.isNaN(minTimeD) && !Double.isNaN(maxTimeD) && minTimeD > maxTimeD) {
            String ts = minTimeParam; minTimeParam = maxTimeParam; maxTimeParam = ts;
            double td = minTimeD;     minTimeD = maxTimeD;         maxTimeD = td; 
        }
        String minTime  = Calendar2.safeEpochSecondsToIsoStringTZ(minTimeD, "");
        String maxTime  = Calendar2.safeEpochSecondsToIsoStringTZ(maxTimeD, "");
        if (minTime.length() == 0)
            minTimeParam = ""; //show some error msg if user supplied a constraint?
        if (maxTime.length() == 0)
            maxTimeParam = ""; //show some error msg if user supplied a constraint?
        boolean tc = Double.isFinite(minTimeD) || Double.isFinite(maxTimeD);
        EDStatic.tally.add("Advanced Search with Time Constraints (since startup)", "" + tc);
        EDStatic.tally.add("Advanced Search with Time Constraints (since last daily report)", "" + tc);

        //categories
        String catSAs[][] = new String[nCatAtts][];
        int whichCatSAIndex[] = new int[nCatAtts];
        for (int ca = 0; ca < nCatAtts; ca++) {
            //get user cat params and validate them (so items on form match items used for search)
            StringArray tsa = categoryInfo(catAtts[ca]);
            tsa.atInsert(0, ANY);
            catSAs[ca] = tsa.toArray();    
            String tParam = request.getParameter(catAttsInURLs[ca]);
            whichCatSAIndex[ca] = 
                (tParam == null || tParam.equals(""))? 0 :
                    Math.max(0, String2.indexOf(catSAs[ca], tParam));
            if (whichCatSAIndex[ca] > 0) {
                EDStatic.tally.add("Advanced Search with Category Constraints (since startup)", 
                    catAttsInURLs[ca] + " = " + tParam);
                EDStatic.tally.add("Advanced Search with Category Constraints (since last daily report)", 
                    catAttsInURLs[ca] + " = " + tParam);
            }
        }

        //protocol
        StringBuilder protocolTooltip = new StringBuilder(
            EDStatic.protocolSearch2Html +
            "\n<p><strong>griddap</strong> - "  + EDStatic.EDDGridDapDescription +
            "\n<p><strong>tabledap</strong> - " + EDStatic.EDDTableDapDescription);
        StringArray protocols = new StringArray();
        protocols.add(ANY);
        protocols.add("griddap");
        protocols.add("tabledap");
        if (EDStatic.wmsActive) {
            protocols.add("WMS");
            protocolTooltip.append("\n<p><strong>WMS</strong> - " + EDStatic.wmsDescriptionHtml);
        }
        if (EDStatic.wcsActive) {
            protocols.add("WCS");
            protocolTooltip.append("\n<p><strong>WCS</strong> - " + EDStatic.wcsDescriptionHtml);
        }
        if (EDStatic.sosActive) {
            protocols.add("SOS");
            protocolTooltip.append("\n<p><strong>SOS</strong> - " + EDStatic.sosDescriptionHtml);
        }
        String tProt = request.getParameter("protocol");
        int whichProtocol = Math.max(0, protocols.indexOf(tProt)); 
        if (whichProtocol > 0) {
            EDStatic.tally.add("Advanced Search with Category Constraints (since startup)", 
                "protocol = " + tProt);
            EDStatic.tally.add("Advanced Search with Category Constraints (since last daily report)", 
                "protocol = " + tProt);
        }


        //get fileTypeName
        fileTypeName = File2.getExtension(endOfRequestUrl); //eg ".html"
        boolean toHtml = fileTypeName.equals(".html");
        if (reallyVerbose) String2.log("Advanced Search   fileTypeName=" + fileTypeName +
            "\n  searchFor=" + searchFor + 
            "\n  whichCatSAString=" + whichCatSAIndex.toString());
        EDStatic.tally.add("Advanced Search, .fileType (since startup)", fileTypeName);
        EDStatic.tally.add("Advanced Search, .fileType (since last daily report)", fileTypeName);

        //*** if .html request, show the form 
        OutputStream out = null;
        Writer writer = null; 
        if (toHtml) { 
            //display start of web page
            out = getHtmlOutputStream(request, response);
            writer = getHtmlWriter(loggedInAs, EDStatic.advancedSearch, out); 
            try {
                HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
                widgets.htmlTooltips = true;
                widgets.enterTextSubmitsForm = true; 

                //display the advanced search form
                String formName = "f1";
                writer.write(
                    "<div class=\"standard_width\">\n" +
                    EDStatic.youAreHere(loggedInAs, EDStatic.advancedSearch + " " +
                        EDStatic.htmlTooltipImage(loggedInAs, 
                            "<div class=\"narrow_max_width\">" + EDStatic.advancedSearchTooltip +
                            "</div>")) + 
                    "\n\n" +
                    EDStatic.advancedSearchDirections + "\n" +
                    HtmlWidgets.ifJavaScriptDisabled + "\n" +
                    widgets.beginForm(formName, "GET",
                        tErddapUrl + "/search/advanced.html", "") + "\n");

                //pipp
                writer.write(widgets.hidden("page", "1")); //new search always resets to page 1
                writer.write(widgets.hidden("itemsPerPage", "" + pipp[1]));

                //full text search...
                writer.write(
                    "<p><strong>" + EDStatic.searchFullTextHtml + "</strong>\n" + 
                    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.searchHintsTooltip) + "\n" +
                    "<br>" +
                    widgets.textField("searchFor", EDStatic.searchTip, 70, 255, searchFor, "") + "\n");

                //categorize      
                //a table with a row for each attribute
                writer.write(
                    "&nbsp;\n" + //necessary for the blank line before the form (not <p>)
                    widgets.beginTable("class=\"compact nowrap\"") +
                    "<tr>\n" +
                    "  <td colspan=\"2\"><strong>" + EDStatic.categoryTitleHtml + "</strong>\n" +
                    EDStatic.htmlTooltipImage(loggedInAs, 
                        "<div class=\"narrow_max_width\">" + EDStatic.advancedSearchCategoryTooltip +
                        "</div>") +
                    "  </td>\n" +
                    "</tr>\n" +
                    "<tr>\n" +
                    "  <td class=\"N\" style=\"width:20%;\">protocol \n" +
                    EDStatic.htmlTooltipImage(loggedInAs, 
                        "<div class=\"standard_max_width\">" + protocolTooltip.toString() +
                        "</div>") + "\n" +
                    "  </td>\n" +
                    "  <td style=\"width:80%;\">&nbsp;=&nbsp;" +
                    widgets.select("protocol", "", 1, protocols.toArray(), whichProtocol, "") + 
                    "  </td>\n" +
                    "</tr>\n");                
                for (int ca = 0; ca < nCatAtts; ca++) {
                    if (catSAs[ca].length == 1)
                        continue;
                    //left column: attribute;   right column: values
                    writer.write(
                        "<tr>\n" +
                        "  <td class=\"N\">" + catAttsInURLs[ca] + "</td>\n" +
                        "  <td>&nbsp;=&nbsp;" + 
                        widgets.select(catAttsInURLs[ca], "", 1, catSAs[ca], whichCatSAIndex[ca], "") +
                        "  </td>\n" +
                        "</tr>\n");
                }

                //bounding box...
                String mapTooltip    = EDStatic.advancedSearchMapTooltip;
                String lonTooltip    = mapTooltip + EDStatic.advancedSearchLonTooltip;
                String timeTooltip   = EDStatic.advancedSearchTimeTooltip;
                String twoClickMap[] = HtmlWidgets.myTwoClickMap540Big(formName, 
                    widgets.imageDirUrl + "world540Big.png", null); //debugInBrowser

                writer.write(
                    //blank row
                    "<tr>\n" +
                    "  <td colspan=\"2\">&nbsp;</td>\n" +
                    "</tr>\n" +

                    //lon lat time ranges
                    "<tr>\n" +
                    "  <td colspan=\"2\"><strong>" + 
                        EDStatic.advancedSearchBounds + "</strong>\n" +
                    EDStatic.htmlTooltipImage(loggedInAs, 
                        "<div class=\"standard_max_width\">" + 
                        EDStatic.advancedSearchRangeTooltip +
                        "<p>" + lonTooltip +
                        "</div>") +
                    "  </td>\n" +
                    "</tr>\n" +

                    //max lat
                    "<tr>\n" +
                    "  <td class=\"N\">" + EDStatic.advancedSearchMaxLat + "</td>\n" +
                    "  <td>&nbsp;=&nbsp;" + 
                    "    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                        widgets.textField("maxLat", 
                            EDStatic.advancedSearchMaxLat + " (-90 to 90)<p>" + mapTooltip, 
                            8, 12, (Double.isNaN(maxLat)? "" : "" + maxLat), "") + 
                    "    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                    "    </td>\n" +
                    "</tr>\n" +

                    //min max lon
                    "<tr>\n" +
                    "  <td class=\"N\">" + EDStatic.advancedSearchMinMaxLon + "</td>\n" +
                    "  <td>&nbsp;=&nbsp;" + 
                        widgets.textField("minLon", 
                            "<div class=\"standard_max_width\">" + 
                            EDStatic.advancedSearchMinLon + "<p>" + lonTooltip +
                            "</div>", 
                            8, 12, 
                            (Double.isNaN(minLon)? "" : "" + minLon), "") + "\n" +
                        widgets.textField("maxLon", 
                            "<div class=\"standard_max_width\">" + 
                            EDStatic.advancedSearchMaxLon + "<p>" + lonTooltip +
                            "</div>", 
                            8, 12, 
                            (Double.isNaN(maxLon)? "" : "" + maxLon), "") +
                        "</td>\n" +
                    "</tr>\n" +

                    //min lat
                    "<tr>\n" +
                    "  <td class=\"N\">" + EDStatic.advancedSearchMinLat + "</td>\n" +
                    "  <td>&nbsp;=&nbsp;" + 
                    "    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                        widgets.textField("minLat", 
                            EDStatic.advancedSearchMinLat + " (-90 to 90)<p>" + mapTooltip, 8, 12, 
                            (Double.isNaN(minLat)? "" : "" + minLat), "") +
                    "    &nbsp;\n" +
                        widgets.htmlButton("button", "", "", 
                            EDStatic.advancedSearchClearHelp,
                            EDStatic.advancedSearchClear,
                            "onClick='f1.minLon.value=\"\"; f1.maxLon.value=\"\"; " +
                                     "f1.minLat.value=\"\"; f1.maxLat.value=\"\"; " +
                                     "((document.all)? document.all.rubberBand : " +
                                         "document.getElementById(\"rubberBand\"))." +
                                         "style.visibility=\"hidden\";'") +
                    "    </td>\n" +
                    "</tr>\n" +
                     
                    //world map 
                    "<tr>\n" +
                    "  <td colspan=\"2\" class=\"N\">&nbsp;&nbsp;&nbsp;&nbsp;" + twoClickMap[0] + 
                        EDStatic.htmlTooltipImage(loggedInAs, lonTooltip) + 
                        twoClickMap[1] + 
                        "</td>\n" +
                    "</tr>\n" +

                    //blank row
                    "<tr>\n" +
                    "  <td colspan=\"2\">&nbsp;</td>\n" +
                    "</tr>\n" +

                    //time
                    "<tr>\n" +
                    "  <td class=\"N\">" + EDStatic.advancedSearchMinTime + "</td>\n" +
                    "  <td>&nbsp;=&nbsp;" + widgets.textField("minTime", 
                        EDStatic.advancedSearchMinTime + "<p>" + timeTooltip, 27, 40, minTimeParam, "") + 
                        "</td>\n" +
                    "</tr>\n" +
                    "<tr>\n" +
                    "  <td class=\"N\">" + EDStatic.advancedSearchMaxTime + "</td>\n" +
                    "  <td>&nbsp;=&nbsp;" + widgets.textField("maxTime", 
                        EDStatic.advancedSearchMaxTime + "<p>" + timeTooltip, 27, 40, maxTimeParam, "") + 
                        "</td>\n" +
                    "</tr>\n" +

                    //end table
                    "</table>\n\n" +

                    //submit button 
                    "<p>" +
                    widgets.htmlButton("submit", null, null, EDStatic.searchClickTip, 
                        "<span style=\"font-size:large;\"><strong>" + EDStatic.searchButton + "</strong></span>", "") + "\n" +

                    //end form
                    widgets.endForm() + "\n" +
                    twoClickMap[2]);
                writer.flush();

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
                throw t; 
            } //otherwise there is more to the document below...
        }

        //*** do the search
        StringArray matchingDatasetIDs = null;

        //test protocol first...
        if (whichProtocol > 0) {
            String protocol = protocols.get(whichProtocol);
            if (protocol.equals("griddap")) {
                matchingDatasetIDs = gridDatasetIDs();   
            } else if (protocol.equals("tabledap")) {
                matchingDatasetIDs = tableDatasetIDs();  
            } else {
                matchingDatasetIDs = allDatasetIDs();    
                boolean testWMS = protocol.equals("WMS");
                boolean testWCS = protocol.equals("WCS");
                boolean testSOS = protocol.equals("SOS");
                int dsn = matchingDatasetIDs.size();
                BitSet keep = new BitSet(dsn);
                keep.set(0, dsn, true);  //so look for a reason not to keep it
                for (int dsi = 0; dsi < dsn; dsi++) {
                    String tDatasetID = matchingDatasetIDs.get(dsi);
                    EDD edd = gridDatasetHashMap.get(tDatasetID);
                    if (edd == null) 
                        edd = tableDatasetHashMap.get(tDatasetID);
                    if (edd == null) {keep.clear(dsi); //e.g., just removed
                    } else if (testWMS) {if (edd.accessibleViaWMS().length() > 0) keep.clear(dsi); 
                    } else if (testWCS) {if (edd.accessibleViaWCS().length() > 0) keep.clear(dsi); 
                    } else if (testSOS) {if (edd.accessibleViaSOS().length() > 0) keep.clear(dsi); 
                    }
                }
                matchingDatasetIDs.justKeep(keep);
                matchingDatasetIDs.sort(); //must be plain sort()
            }

            //category search needs plain sort(), not sortIgnoreCase()
            matchingDatasetIDs.sort(); 
            //String2.log("  after protocol=" + protocol + ", nMatching=" + matchingDatasetIDs.size()); 
        }            


        //test category...
        for (int ca = 0; ca < nCatAtts; ca++) {
            if (whichCatSAIndex[ca] > 0) {
                StringArray tMatching = categoryInfo(catAtts[ca], catSAs[ca][whichCatSAIndex[ca]]);
                tMatching.sort(); //must be plain sort()
                if  (matchingDatasetIDs == null) {
                     matchingDatasetIDs = tMatching;
                } else {
                    matchingDatasetIDs.inCommon(tMatching); 
                }
            //String2.log("  after " + catAttsInURLs[ca] + ", nMatching=" + matchingDatasetIDs.size()); 
            }
        }

        //test bounding box...
        boolean testLon  = !Double.isNaN(minLon  ) || !Double.isNaN(maxLon  );
        boolean testLat  = !Double.isNaN(minLat  ) || !Double.isNaN(maxLat  );
        boolean testTime = !Double.isNaN(minTimeD) || !Double.isNaN(maxTimeD);
        if (testLon || testLat || testTime) {
            if (matchingDatasetIDs == null)
                matchingDatasetIDs = allDatasetIDs();
            int dsn = matchingDatasetIDs.size();
            BitSet keep = new BitSet(dsn);
            keep.set(0, dsn, true);  //so look for a reason not to keep it
            for (int dsi = 0; dsi < dsn; dsi++) {
                String tDatasetID = matchingDatasetIDs.get(dsi);
                EDDGrid eddg = gridDatasetHashMap.get(tDatasetID);
                EDV lonEdv = null, latEdv = null, timeEdv = null;
                if (eddg == null) {
                    EDDTable eddt = tableDatasetHashMap.get(tDatasetID);
                    if (eddt != null) {
                        if (eddt.lonIndex( ) >= 0) lonEdv  = eddt.dataVariables()[eddt.lonIndex()];
                        if (eddt.latIndex( ) >= 0) latEdv  = eddt.dataVariables()[eddt.latIndex()];
                        if (eddt.timeIndex() >= 0) timeEdv = eddt.dataVariables()[eddt.timeIndex()];
                    }
                } else {
                        if (eddg.lonIndex( ) >= 0) lonEdv  = eddg.axisVariables()[eddg.lonIndex()];
                        if (eddg.latIndex( ) >= 0) latEdv  = eddg.axisVariables()[eddg.latIndex()];
                        if (eddg.timeIndex() >= 0) timeEdv = eddg.axisVariables()[eddg.timeIndex()];
                }

                //testLon
                if (testLon) {
                    if (lonEdv == null) {
                        keep.clear(dsi);
                    } else {
                        if (!Double.isNaN(minLon)) {
                            if (Double.isNaN(lonEdv.destinationMax()) ||
                                minLon > lonEdv.destinationMax()) {
                                keep.clear(dsi);
                            }
                        }
                        if (!Double.isNaN(maxLon)) {
                            if (Double.isNaN(lonEdv.destinationMin()) ||
                                maxLon < lonEdv.destinationMin()) {
                                keep.clear(dsi);
                            }
                        }
                    }
                }

                //testLat
                if (testLat) {
                    if (latEdv == null) {
                        keep.clear(dsi);
                    } else {
                        if (!Double.isNaN(minLat)) {
                            if (Double.isNaN(latEdv.destinationMax()) ||
                                minLat > latEdv.destinationMax()) {
                                keep.clear(dsi);
                            }
                        }
                        if (!Double.isNaN(maxLat)) {
                            if (Double.isNaN(latEdv.destinationMin()) ||
                                maxLat < latEdv.destinationMin()) {
                                keep.clear(dsi);
                            }
                        }
                    }
                }

                //testTime
                if (testTime) {
                    if (timeEdv == null) {
                        keep.clear(dsi);
                    } else {
                        if (!Double.isNaN(minTimeD)) {
                            if (Double.isNaN(timeEdv.destinationMax())) {
                                //test is ambiguous, since destMax=NaN may mean current time
                            } else if (minTimeD > timeEdv.destinationMax()) {
                                keep.clear(dsi);
                            }
                        }
                        if (!Double.isNaN(maxTimeD)) {
                            if (Double.isNaN(timeEdv.destinationMin()) ||
                                maxTimeD < timeEdv.destinationMin()) {
                                keep.clear(dsi);
                            }
                        }
                    }
                }
            }
            matchingDatasetIDs.justKeep(keep);
            //String2.log("  after boundingBox, nMatching=" + matchingDatasetIDs.size()); 
        }
            
        //do text search last, since it is the most time-consuming
        //  and since it sorts the results by relevance
        //IMPORTANT: this step ensures that datasets are in sorted order
        //  (so reducing by page and itemsPerPage below works with correct items in correct order)
        //  AND also ensures user has right to know dataset exists
        if (searchFor.length() > 0) {
            //do the full text search (sorts best to worst match)
            if (matchingDatasetIDs == null)
                matchingDatasetIDs = allDatasetIDs();
            matchingDatasetIDs = getSearchDatasetIDs(loggedInAs, matchingDatasetIDs, 
                searchFor);

        } else {
            //sortByTitle
            if (matchingDatasetIDs != null) 
                matchingDatasetIDs = sortByTitle(loggedInAs, matchingDatasetIDs, 
                    true); //search: this is a metadata request
        }

        Table resultsTable = null;
        boolean searchPerformed = matchingDatasetIDs != null;        
        int nMatches = 0, page = 0, itemsPerPage = 0,  //revised below   
            startIndex = 0, lastPage = 0;      

        if (searchPerformed) {
            //calculate Page ItemsPerPage
            nMatches = matchingDatasetIDs.size();
            int pIpp[] = EDStatic.calculatePIpp(request, nMatches);
            page         = pIpp[0]; //will be 1... 
            itemsPerPage = pIpp[1]; //will be 1...
            startIndex   = pIpp[2]; //will be 0...
            lastPage     = pIpp[3]; //will be 1...

            //reduce datasetIDs to ones on requested page
            //more efficient to remove items at end, then items at beginning
            if (startIndex + itemsPerPage < nMatches) 
                matchingDatasetIDs.removeRange(startIndex + itemsPerPage, nMatches);
            matchingDatasetIDs.removeRange(0, Math.min(startIndex, nMatches));

            //if non-null, error will be String[2]
            /*String error[] = null;
            if (nMatches == 0) {
                error = new String[] {
                MessageFormat.format(EDStatic.noDatasetWith, "protocol=\"" + protocol + "\""),
                    ""};
            } else if (page > lastPage) {
                error = EDStatic.noPage(page, lastPage);
            }*/


            //make the resultsTable 
            boolean sortByTitle = false;  //already put in appropriate order above
            if (toHtml)
                 resultsTable = makeHtmlDatasetTable( loggedInAs, matchingDatasetIDs, sortByTitle);  
            else resultsTable = makePlainDatasetTable(loggedInAs, matchingDatasetIDs, sortByTitle, fileTypeName);  
        }


        //*** show the .html results
        if (toHtml) { 
            try {
                //display datasets
                writer.write(
                    //"<br>&nbsp;\n" +
                    "<hr>\n" +
                    "<h2>" + EDStatic.advancedSearchResults + "</h2>\n");  
                if (searchPerformed) {
                    if (resultsTable.nRows() == 0) {
                         writer.write("<strong>" + XML.encodeAsHTML(MustBe.THERE_IS_NO_DATA) + "</strong>\n" +
                             (searchFor.length() > 0? "<br>" + EDStatic.searchSpelling + "\n" : "") +
                             "<br>" + EDStatic.advancedSearchFewerCriteria + "\n" +
                             "</div>\n"); //which controls width
                    } else {

                        String nMatchingHtml = EDStatic.nMatchingDatasetsHtml(
                            nMatches, page, lastPage, 
                            searchFor.length() > 0 && !searchFor.equals("all"),  //true=most relevant first
                            EDStatic.baseUrl(loggedInAs) + requestUrl + 
                            EDStatic.questionQuery(request.getQueryString()));
                            
                        writer.write(nMatchingHtml +
                            "<br>&nbsp;\n"); 

                        resultsTable.saveAsHtmlTable(writer, "commonBGColor", null, 1, false, -1, 
                            false, false); //don't encodeAsHTML the cell's contents, !allowWrap

                        if (lastPage > 1)
                            writer.write("\n<p>" + nMatchingHtml);

                        //list plain file types
                        writer.write(
                            "\n" +
                            "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                            plainFileTypesString + //not links, which would be indexed by search engines
                            ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                                EDStatic.restfulViaService + "</a>.\n");
                    }
                } else {
                    writer.write(
                        MessageFormat.format(EDStatic.advancedSearchNoCriteria,
                            EDStatic.searchButton, tErddapUrl, "" + pipp[1])); 
                }

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t;
            } finally {
                writer.write("</div>\n");  //which controls width
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        //return non-html file types 
        if (endsWithPlainFileType(endOfRequestUrl, "advanced")) {
            if (searchPerformed) {
                //show the results in other file types
                sendPlainTable(loggedInAs, request, response, resultsTable, 
                    "AdvancedSearch", fileTypeName);
                return;
            } else {
                throw new SimpleException(EDStatic.queryError + 
                    MessageFormat.format(EDStatic.advancedSearchWithCriteria, fileTypeName));
            }
        }

    }

    /**
     * This gets the HTML for a table with (usually) YouAreHere on the left 
     * and other things on the right.
     */
    public static String getYouAreHereTable(String leftSide, String rightSide) 
        throws Throwable {

        //begin table
        StringBuilder sb = new StringBuilder(
            "<table class=\"compact\" style=\"width:100%; border-spacing:2px;\">\n" +
            "<tr>\n" +
            "<td class=\"B\" style=\"width:90%;\">");

        //you are here
        sb.append(leftSide);                   
        sb.append(
            "</td>\n" +
            "<td style=\"white-space:nowrap; width:10%;\">");

        //rightside
        sb.append(rightSide);

        //end table
        sb.append(
            "</td>\n" +
            "</tr>\n" +
            "</table>\n");

        return sb.toString();
    }


    /**
     * This generates a results table in response to a searchFor string.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This is used to determine if the user has the right to know if a given
     *    dataset exists.  (But dataset will be matched if EDStatic.listPrivateDatasets.)  
     * @param tDatasetIDs  The datasets to be considered (usually allDatasetIDs()). 
     *    The order (sorted or not) is irrelevant.
     * @param searchFor the Google-like string of search terms.
     * @param toHtml if true, this returns a table with values suited
     *    to display via HTML. If false, the table has plain text values
     * @param fileTypeName the file type name (e.g., .htmlTable) to be used
     *    for the info links.
     * @return a table with the results.
     *    It may have 0 rows.
     * @throws Throwable, notably ClientAbortException
     */
    public Table getSearchTable(String loggedInAs, StringArray tDatasetIDs,
        String searchFor, boolean toHtml, String fileTypeName) throws Throwable {

        tDatasetIDs = getSearchDatasetIDs(loggedInAs, tDatasetIDs, searchFor);

        boolean sortByTitle = false; //already sorted by search 
        return toHtml? 
            makeHtmlDatasetTable( loggedInAs, tDatasetIDs, sortByTitle) : 
            makePlainDatasetTable(loggedInAs, tDatasetIDs, sortByTitle, fileTypeName);
    }

    /**
     * This finds the datasets that match a searchFor string.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This is used to determine if the user has the right to know if a given
     *    dataset exists.  (But dataset will be matched if EDStatic.listPrivateDatasets.)  
     * @param tDatasetIDs  The datasets to be considered (usually allDatasetIDs()). 
     *    The order (sorted or not) is irrelevant.
     * @param searchFor the Google-like string of search terms.
     *    Special cases: "" and "all" return all datasets that loggedInAs has a right 
     *    to know exist, sorted by title.
     * @return a StringArray with the matching datasetIDs, in order best to worst.
     * @throws Throwable, notably ClientAbortException
     */
    public StringArray getSearchDatasetIDs(String loggedInAs, StringArray tDatasetIDs,
        String searchFor) throws Throwable {

        //*** respond to search request
        StringArray searchWords = StringArray.wordsAndQuotedPhrases(searchFor.toLowerCase());
        int nSearchWords = searchWords.size();

        //special cases: "" and "all"
        if (nSearchWords == 0 ||
            (nSearchWords == 1 && searchWords.get(0).equals("all"))) 
            return sortByTitle(loggedInAs, tDatasetIDs, 
                true); //search: this is a metadata request 
        
        //gather the matching datasets
        Table table = new Table();
        IntArray    rankPa  = new IntArray();
        StringArray titlePa = new StringArray();
        StringArray idPa    = new StringArray();
        //order added is important, because it uses leftToRightSort below
        table.addColumn("rank",  rankPa); 
        table.addColumn("title", titlePa); 
        table.addColumn("id",    idPa);

        //do the search; populate the results table 
        String roles[] = EDStatic.getRoles(loggedInAs);
        int nDatasetsSearched = 0;
        long tTime = System.currentTimeMillis();
        if (nSearchWords > 0) {
            int ntDatasetIDs = tDatasetIDs.size();

            //try to get luceneIndexSearcher
            //if fail, go back to original search
            Object object2[] = EDStatic.useLuceneSearchEngine?
                EDStatic.luceneIndexSearcher() : new Object[]{null, null};                   
            IndexSearcher indexSearcher = (IndexSearcher)object2[0];
            String datasetIDFieldCache[] = (String[])object2[1];

            if (indexSearcher != null && datasetIDFieldCache != null) { 
                //useLuceneSearchEngine=true and searcher is valid
                //do the searches with the LUCENE searchEngine
                //??? future: allow "title:..." searches
                try {
                    BooleanQuery booleanQuery = new BooleanQuery();
                    boolean allNegative = true;
                    boolean booleanQueryHasTerms = false;
                    for (int w = 0; w < nSearchWords; w++) {

                        //create the BooleanQuery for Lucene
                        //see https://lucene.apache.org/java/3_5_0/queryparsersyntax.html
                        String sw = searchWords.get(w);

                        //excluded term?
                        BooleanClause.Occur occur;
                        if (sw.charAt(0) == '-') {
                            occur = BooleanClause.Occur.MUST_NOT;
                            sw = sw.substring(1);
                        } else {
                            occur = BooleanClause.Occur.MUST;
                            allNegative = false;
                        }

                        //remove enclosing double quotes
                        if (sw.length() >= 2 && 
                            sw.charAt(0) == '\"' && sw.charAt(sw.length() - 1) == '\"') 
                            sw = String2.replaceAll(sw.substring(1, sw.length() - 1), "\"\"", "\"");

                        //escape special chars
                        StringBuilder sb2 = new StringBuilder();
                        for (int i2 = 0; i2 < sw.length(); i2++) {
                            if (EDStatic.luceneSpecialCharacters.indexOf(sw.charAt(i2)) >= 0)
                                sb2.append('\\');
                            sb2.append(sw.charAt(i2));
                        }
                        sw = sb2.toString();

                        //initial parsing (is it a Term or a Phrase?)
                        //use queryParser to parse each part of the pre-parsed query
                        //(using same Analyzer here as in IndexWriter
                        //was emphasized at https://darksleep.com/lucene/ )
                        Query tQuery = EDStatic.luceneParseQuery(sw);
                        //String2.log("sw#" + w + "=" + sw + 
                        //    " initialQuery=" + 
                        //    (tQuery == null? "null" : tQuery.getClass().getName()) +
                        //    " allNegative=" + allNegative);
                        if (tQuery == null) {
                            continue;  //it is possible, e.g., search for a stop word
                        } else if (tQuery instanceof TermQuery) { //single word
                            char lastChar = sw.length() == 0? ' ' : sw.charAt(sw.length() - 1);
                            if (Character.isLetterOrDigit(lastChar) || 
                                lastChar == '.' || lastChar == '_') //lucene treats as part of single word
                                sw += "*"; //match original search: allow longer variants (wind finds windspeed)
                            //else punctuation  //things like http://* fail, but http* succeeds
                        } else {
                            sw = "\"" + sw + "\""; //treat as phrase
                        }

                        //real parsing
                        tQuery = EDStatic.luceneParseQuery(sw);

                        if (tQuery == null)
                            continue; //shouldn't happen
                        booleanQueryHasTerms = true;
                        booleanQuery.add(tQuery, occur);

                        //boost score if it is also in title
                        if (occur == BooleanClause.Occur.MUST) { //if it isn't in 'text', it won't be in title
                            tQuery = EDStatic.luceneParseQuery("title:" + sw);
                            if (tQuery != null) {
                                booleanQueryHasTerms = true;
                                booleanQuery.add(tQuery, BooleanClause.Occur.SHOULD);
                            } //if tQuery couldn't be parsed, it is fine to just drop it
                        }                           
                    }

                    //special case: add "all" to an allNegative query, e.g., "-sst -NODC" 
                    //(for which Lucene says no matches)
                    if (reallyVerbose) String2.log("allNegative=" + allNegative);
                    if (allNegative) {
                        if (booleanQueryHasTerms) {
                            booleanQuery.add(
                                new TermQuery(new Term(EDStatic.luceneDefaultField, "all")),
                                BooleanClause.Occur.MUST);
                        } else {
                            //no terms. So return all datasetsIDs, sorted by title
                            return sortByTitle(loggedInAs, tDatasetIDs,
                                true); //search: this is a metadata request
                        }
                    }
                    //now, booleanQuery must have terms
                    if (reallyVerbose) String2.log("booleanQuery=" + booleanQuery.toString());

                    //make a hashSet of tDatasetIDs (so seachable quickly)
                    HashSet hashSet = new HashSet(Math2.roundToInt(1.4 * tDatasetIDs.size()));
                    for (int i = 0; i < tDatasetIDs.size(); i++)
                        hashSet.add(tDatasetIDs.get(i));

                    //do the lucene search
                    long luceneTime = System.currentTimeMillis();
                    TopDocs hits = indexSearcher.search(booleanQuery, 
                        indexSearcher.maxDoc()); //max n search results
                    ScoreDoc scoreDocs[] = hits.scoreDocs;
                    int nHits = scoreDocs.length;
                    if (reallyVerbose) 
                        String2.log("  luceneQuery nMatches=" + nHits + 
                            " time=" + (System.currentTimeMillis() - luceneTime) + "ms");
                    for (int i = 0; i < nHits; i++) {
                        //was (without luceneDatasetIDFieldCache)
                        //Document hitDoc = indexSearcher.doc(hits.scoreDocs[i].doc);
                        //String tDatasetID = hitDoc.get("datasetID"); 

                        //with luceneDatasetIDFieldCache
                        String tDatasetID = datasetIDFieldCache[scoreDocs[i].doc]; //doc#
                        //String2.log("hit#" + i + ": datasetID=" + tDatasetID);

                        //ensure tDatasetID is in tDatasetIDs (e.g., just grid datasets)
                        if (!hashSet.contains(tDatasetID))
                            continue;
                        
                        //ensure user is allowed to know this dataset exists
                        EDD edd = gridDatasetHashMap.get(tDatasetID);
                        if (edd == null)
                            edd = tableDatasetHashMap.get(tDatasetID);
                        if (edd == null)  //just deleted?
                            continue;                         
                        if (EDStatic.listPrivateDatasets || //list all datasets, private or not
                            edd.isAccessibleTo(EDStatic.getRoles(loggedInAs)) || //accessibleTo
                            edd.graphsAccessibleToPublic()) {  //graphsAccessibleToPublic
                            //add penalty for DEPRECATED
                            int penalty = edd.title().indexOf("DEPRECATED") >= 0? nHits : 0;
                            rankPa.add(i + penalty);
                            titlePa.add(edd.title());
                            idPa.add(tDatasetID);
                        }
                    }

                //it needs to be sorted because of DEPRECATION penalty
                table.leftToRightSort(3);

                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    throw new SimpleException(EDStatic.searchNotAvailable, t);
                }

            } else {
                //do the searches with the ORIGINAL searchEngine
                //prepare the byte[]s
                boolean isNegative[]  = new boolean[nSearchWords];
                byte searchWordsB[][] = new byte[   nSearchWords][];
                int  jumpB[][] = new int[nSearchWords][];
                for (int w = 0; w < nSearchWords; w++) {
                    String sw = searchWords.get(w);
                    isNegative[w] = sw.charAt(0) == '-';  // -NCDC  -"my phrase"
                    if (isNegative[w]) 
                        sw = sw.substring(1); 

                    //remove enclosing double quotes
                    if (sw.length() >= 2 && 
                        sw.charAt(0) == '\"' && sw.charAt(sw.length() - 1) == '\"')
                        sw = String2.replaceAll(sw.substring(1, sw.length() - 1), "\"\"", "\"");

                    searchWordsB[w] = String2.stringToUtf8Bytes(sw);
                    jumpB[w] = String2.makeJumpTable(searchWordsB[w]);
                }

                for (int i = 0; i < ntDatasetIDs; i++) {
                    String tId = tDatasetIDs.get(i);
                    EDD edd = gridDatasetHashMap.get(tId);
                    if (edd == null)
                        edd = tableDatasetHashMap.get(tId);
                    if (edd == null)  //just deleted?
                        continue;
                    if (!EDStatic.listPrivateDatasets && 
                        !edd.isAccessibleTo(roles) &&
                        !edd.graphsAccessibleToPublic()) //search for datasets is always a metadata request
                        continue;
                    nDatasetsSearched++;
                    int rank = edd.searchRank(isNegative, searchWordsB, jumpB);           
                    if (rank < Integer.MAX_VALUE) {
                        // /10 makes rank less sensitive to exact char positions
                        // so more likely to be tied,
                        // so similar datasets are more likely to sort by title
                        rankPa.add(rank / 10); 
                        titlePa.add(edd.title());
                        idPa.add(tId);
                    }
                }

                //sort
                table.leftToRightSort(3);
            }
        }
        if (verbose) {
            tTime = System.currentTimeMillis() - tTime;
            String2.log("Erddap.search(" + EDStatic.searchEngine + ") " +
                //"searchFor=" + searchFor + "\n" +
                //"searchWords=" + searchWords.toString() + "\n" +
                //"nDatasetsSearched=" + nDatasetsSearched + 
                " nWords=" + nSearchWords + " nMatches=" + rankPa.size() +
                " totalTime=" + tTime + "ms"); 
                //" avgTime=" + (tTime / Math.max(1, nDatasetsSearched*nSearchWords)));
        }

        return idPa;
    }

    /**
     * This sorts the datasetIDs by the datasets' titles.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This is used to determine if the user has the right to know if a given
     *    dataset exists. (But dataset will be matched if EDStatic.listPrivateDatasets.)  
     * @param tDatasetIDs  The datasets to be considered (usually allDatasetIDs()). 
     *    The order (sorted or not) is irrelevant.
     * @param graphOrMetadataRequest use true if this is for a graph or metadata request
     *    so that graphsAccessibleToPublic() will be used.
     * @return a StringArray with the matching datasetIDs, sorted by title.
     */
    public StringArray sortByTitle(String loggedInAs, StringArray tDatasetIDs,
        boolean graphOrMetadataRequest) {

        String roles[] = EDStatic.getRoles(loggedInAs);
        Table table = new Table();
        int n = tDatasetIDs.size();
        StringArray titlePa = new StringArray(n, false);
        StringArray idPa    = new StringArray(n, false);
        table.addColumn("title", titlePa); 
        table.addColumn("id", idPa);
        for (int ds = 0; ds < n; ds++) {
            String tID = tDatasetIDs.get(ds);
            EDD edd = gridDatasetHashMap.get(tID);
            if (edd == null)
                edd = tableDatasetHashMap.get(tID);
            if (edd == null)  //just deleted?
                continue;
            if (EDStatic.listPrivateDatasets || edd.isAccessibleTo(roles) ||
                (graphOrMetadataRequest && edd.graphsAccessibleToPublic())) {
                titlePa.add(edd.title());
                idPa.add(tID);
            }
        }
        table.leftToRightSortIgnoreCase(2);
        return idPa;
    }

    /**
     * Process a categorize request:    erddap/categorize/{attribute}/{categoryName}/index.html
     * e.g., erddap/categorize/ioos_category/temperature/index.html
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (here always "categorize") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doCategorize(HttpServletRequest request, HttpServletResponse response,
        String loggedInAs, String protocol, int datasetIDStartsAt, String userQuery) throws Throwable {
        
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String fileTypeName = "";
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);  
        String gap = "&nbsp;&nbsp;&nbsp;&nbsp;";

        //ensure query has simplistically valid page= itemsPerPage=
        if (!Arrays.equals(
                EDStatic.getRawRequestedPIpp(request),
                EDStatic.getRequestedPIpp(request))) {
            sendRedirect(response, 
                EDStatic.baseUrl(loggedInAs) + requestUrl + "?" +
                EDStatic.passThroughJsonpQuery(request) +
                EDStatic.passThroughPIppQuery(request));  //should be nothing else in query
            return;
        }      

        //parse endOfRequestUrl into parts
        String parts[] = String2.split(endOfRequestUrl, '/');
        String attributeInURL = parts.length < 1? "" : parts[0];
        int whichAttribute = String2.indexOf(EDStatic.categoryAttributesInURLs, attributeInURL);
        if (reallyVerbose) String2.log("  attributeInURL=" + attributeInURL + " which=" + whichAttribute);
        String attribute = whichAttribute < 0? "" : EDStatic.categoryAttributes[whichAttribute];

        String categoryName = parts.length < 2? "" : parts[1];
        if (reallyVerbose) String2.log("  categoryName=" + categoryName);

        //if {attribute}/index.html and there is only 1 categoryName
        //  redirect to {attribute}/{categoryName}/index.html
        if (whichAttribute >= 0 && parts.length == 2 && categoryName.equals("index.html")) {
            String values[] = categoryInfo(attribute).toArray();
            if (values.length == 1) {
                sendRedirect(response, tErddapUrl + "/" + protocol + "/" + 
                    attributeInURL + "/" + values[0] + "/index.html?" +
                    EDStatic.passThroughPIppQueryPage1(request));
                return;
            }
        }


        //generate the youAreHereTable
        String advancedQuery = "";
        if (parts.length == 3 && parts[2].equals("index.html"))
            advancedQuery = parts[0] + "=" + SSR.minimalPercentEncode(parts[1]);
        String refine = ""; //"&nbsp;<br>&nbsp;";
        if (advancedQuery.length() > 0)
            refine = 
                //"&nbsp;\n" +
                ////Or, View All Datasets
                //"<br>Or, <a href=\"" + tErddapUrl + "/info/index.html?" +
                //    EDStatic.encodedPassThroughPIppQueryPage1(request) + "\">" +
                //EDStatic.viewAllDatasetsHtml + "</a>\n" +
                ////Or, search text
                //"<p>" + getSearchFormHtml(request, loggedInAs, EDStatic.orComma, ":\n<br>", "") +
                //Use <p> below if other options above are enabled.
                "<span class=\"N\">(" + EDStatic.orRefineSearchWith + 
                    getAdvancedSearchLink(loggedInAs, 
                        EDStatic.passThroughPIppQueryPage1(request) + 
                        "&" + advancedQuery) + 
                ")</span>\n";

        String youAreHere = EDStatic.youAreHere(loggedInAs, EDStatic.categoryTitleHtml);
        //String youAreHereTable = 
        //    getYouAreHereTable(youAreHere, refine) +
        //    "\n" + HtmlWidgets.ifJavaScriptDisabled + "\n";

        //*** attribute string should be e.g., ioos_category
        fileTypeName = File2.getExtension(endOfRequestUrl);
        if (whichAttribute < 0) {
            //*** deal with invalid attribute string

            //redirect to index.html
            if (attributeInURL.equals("") ||
                attributeInURL.equals("index.htm")) {
                sendRedirect(response, tErddapUrl + "/" + protocol + "/index.html?" +
                    EDStatic.passThroughPIppQueryPage1(request));
                return;
            }   
            
            //return table of categoryAttributes
            if (String2.indexOf(plainFileTypes, fileTypeName) >= 0) {
                //plainFileType
                if (attributeInURL.equals("index" + fileTypeName)) {
                    //respond to categorize/index.xxx
                    //display list of categoryAttributes in plainFileType file
                    Table table = categorizeOptionsTable(request, tErddapUrl, fileTypeName);
                    sendPlainTable(loggedInAs, request, response, table, protocol, fileTypeName);
                } else {
                    if (verbose) String2.log(EDStatic.resourceNotFound + " not index" + fileTypeName);
                    sendResourceNotFoundError(request, response, "");
                    return;
                }
            } else { 
                //respond to categorize/index.html or errors: unknown attribute, unknown fileTypeName 
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, "Categorize", out); 
                try {
                    //you are here  Categorize    
                    writer.write(
                        "<div class=\"standard_width\">\n" +
                        youAreHere);
                        //youAreHereTable);

                    if (!attributeInURL.equals("index.html")) 
                        writeErrorHtml(writer, request, 
                            "categoryAttribute=\"" + XML.encodeAsHTML(attributeInURL) + "\" is not an option.");

                    writeCategorizeOptionsHtml1(request, loggedInAs, writer, null, false);

                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(t));
                    throw t; 
                } finally {
                    writer.write("</div>\n");
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }
            }
            return;
        }   
        //attribute is valid
        if (reallyVerbose) String2.log("  attribute=" + attribute + " is valid.");


        //*** categoryName string should be e.g., Location
        //*** deal with index.xxx and invalid categoryName
        StringArray catDats = categoryInfo(attribute, categoryName); //returns datasetIDs
        if (catDats.size() == 0) {

            //redirect to index.html
            if (categoryName.equals("") ||
                categoryName.equals("index.htm")) {
                sendRedirect(response, tErddapUrl + "/" + protocol + "/" + 
                    attributeInURL + "/index.html?" +
                    EDStatic.passThroughPIppQueryPage1(request));
                return;
            }   
            
            //redirect to lowercase?
            if (parts.length >= 2) {
                catDats = categoryInfo(attribute, categoryName.toLowerCase());
                if (catDats.size() > 0) {
                    parts[1] = parts[1].toLowerCase();
                    sendRedirect(response, tErddapUrl + "/" + protocol + "/" +
                        String2.toSVString(parts, "/", false) + "?" +
                        EDStatic.passThroughJsonpQuery(request) +
                        EDStatic.passThroughPIppQueryPage1(request)); 
                    return;
                }   
            }

            //return table of categoryNames
            //Always return all.  page= and itemsPerPage don't apply to this.
            //!!! That's trouble for UAF, because there could be 10^6 options and 
            //   browsers (and Mac users) will freak out.
            if (String2.indexOf(plainFileTypes, fileTypeName) >= 0) {
                //plainFileType
                if (categoryName.equals("index" + fileTypeName)) {
                    //respond to categorize/attribute/index.xxx
                    //display list of categoryNames in plainFileType file
                    sendCategoryPftOptionsTable(request, response, loggedInAs, 
                        attribute, attributeInURL, fileTypeName);
                } else {
                    if (verbose) String2.log(EDStatic.resourceNotFound + " category not index" + fileTypeName);
                    sendResourceNotFoundError(request, response, "");
                    return;
                }
            } else { 
                //respond to categorize/index.html or errors: 
                //  unknown attribute, unknown fileTypeName 
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, "Categorize", out); 
                try {
                    writer.write(
                        "<div class=\"standard_width\">\n" +
                        youAreHere);
                        //youAreHereTable);
                    if (!categoryName.equals("index.html")) {
                        writeErrorHtml(writer, request, 
                            MessageFormat.format(EDStatic.categoryNotAnOption,
                                attributeInURL, categoryName));
                        writer.write("<hr>\n");
                    }
                    writer.write(
                        "<p>");
                    //    "<table class=\"compact nowrap\">\n" +
                    //    "<tr>\n" +
                    //    "<td class=\"T\">\n");
                    writeCategorizeOptionsHtml1(request, loggedInAs, writer, 
                        attributeInURL, false);
                    writer.write(
                        "<p>");
                    //    "</td>\n" +
                    //    "<td>" + gap + "</td>\n" +
                    //    "<td class=\"T\">\n");
                    writeCategoryOptionsHtml2(request, loggedInAs, writer, 
                        attribute, attributeInURL, categoryName);
                    //writer.write(
                    //    "</td>\n" +
                    //    "</tr>\n" +
                    //    "</table>\n");

                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(t));
                    throw t; 
                } finally {
                    writer.write("</div>\n");
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }
            }
            return;
        }           
        //categoryName is valid
        if (reallyVerbose) String2.log("  categoryName=" + categoryName + " is valid.");

        //*** attribute (e.g., ioos_category) and categoryName (e.g., Location) are valid
        //endOfRequestUrl3 should be index.xxx or {categoryName}.xxx
        String part2 = parts.length < 3? "" : parts[2];

        //redirect categorize/{attribute}/{categoryName}/index.htm request index.html
        if (part2.equals("") ||
            part2.equals("index.htm")) {
            sendRedirect(response, tErddapUrl + "/" + protocol + "/" + 
                attributeInURL + "/" + categoryName + "/index.html?" +
                    EDStatic.passThroughPIppQueryPage1(request));
            return;
        }   

        //sort catDats by title
        catDats = sortByTitle(loggedInAs, catDats,
            true); //category search: this is a metadata request

        //calculate Page ItemsPerPage  (part of: categorize)
        int nMatches = catDats.size();
        int pIpp[] = EDStatic.calculatePIpp(request, nMatches);
        int page         = pIpp[0]; //will be 1... 
        int itemsPerPage = pIpp[1]; //will be 1...
        int startIndex   = pIpp[2]; //will be 0...
        int lastPage     = pIpp[3]; //will be 1...

        //reduce datasetIDs to ones on requested page
        //IMPORTANT!!! For this to work correctly, datasetIDs must be
        //  accessibleTo loggedInAs (or EDStatic.listPrivateDatasets)
        //  and in final sorted order.   
        //  (true here)
        //Order of removal: more efficient to remove items at end, then items at beginning.
        if (startIndex + itemsPerPage < nMatches) 
            catDats.removeRange(startIndex + itemsPerPage, nMatches);
        catDats.removeRange(0, Math.min(startIndex, nMatches));

        //*** respond to categorize/{attributeInURL}/{categoryName}/index.fileTypeName request
        EDStatic.tally.add("Categorize Attribute (since startup)", attributeInURL);
        EDStatic.tally.add("Categorize Attribute (since last daily report)", attributeInURL);
        EDStatic.tally.add("Categorize Attribute = Value (since startup)", attributeInURL + " = " + categoryName);
        EDStatic.tally.add("Categorize Attribute = Value (since last daily report)", attributeInURL + " = " + categoryName);
        EDStatic.tally.add("Categorize File Type (since startup)", fileTypeName);
        EDStatic.tally.add("Categorize File Type (since last daily report)", fileTypeName);
        boolean sortByTitle = false;
        if (endsWithPlainFileType(part2, "index")) {
            //show the results as plain file type
            Table table = makePlainDatasetTable(loggedInAs, catDats, sortByTitle, fileTypeName);
            sendPlainTable(loggedInAs, request, response, table, 
                attributeInURL + "_" + categoryName, fileTypeName);
            return;
        }

        //respond to categorize/{attributeInURL}/{categoryName}/index.html request
        if (part2.equals("index.html")) {
            //make a table of the datasets
            Table table = makeHtmlDatasetTable(loggedInAs, catDats, sortByTitle);

            //display start of web page
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, "Categorize", out); 
            try {
                writer.write(
                    "<div class=\"standard_width\">\n" +
                    youAreHere);
                    //youAreHereTable);

                //write categorizeOptions
                writer.write(
                    "<p>");
                //    "<table class=\"compact nowrap\">\n" +
                //    "<tr>\n" +
                //    "<td class=\"T\">\n");
                writeCategorizeOptionsHtml1(request, loggedInAs, writer, attributeInURL, false);
                writer.write(
                    "<p>");
                //    "</td>\n" +
                //    "<td>" + gap + "</td>\n" +
                //    "<td class=\"T\">\n");
                    
                //write categoryOptions
                writeCategoryOptionsHtml2(request, loggedInAs, writer, 
                    attribute, attributeInURL, categoryName);
                //writer.write(
                //    "</td>\n" +
                //    "</tr>\n" +
                //    "</table>\n");

                String nMatchingHtml = EDStatic.nMatchingDatasetsHtml(
                    nMatches, page, lastPage, false, //=alphabetical
                    EDStatic.baseUrl(loggedInAs) + requestUrl + 
                    EDStatic.questionQuery(userQuery));

                //display datasets
                writer.write("<h3>3) " + EDStatic.resultsOfSearchFor + "\n" +
                    "<span class=\"N\"><kbd>" + attributeInURL + 
                    " = " + categoryName + "</kbd></span>" + 
                    "</h3>\n" +
                    nMatchingHtml + "\n" +
                    //"<br>&nbsp;\n" +

                    //"<br><strong>" + EDStatic.pickADataset + ":</strong>\n" +
                    refine +

                    //"<br>" + EDStatic.nMatchingDatasetsHtml(nMatches, page, lastPage, 
                    //    false,  //=alphabetical
                    //    EDStatic.baseUrl(loggedInAs) + requestUrl + 
                    //    EDStatic.questionQuery(request.getQueryString())) +
                    "<br>&nbsp;\n"); //necessary for the blank line before the table (not <p>)

                table.saveAsHtmlTable(writer, "commonBGColor", null, 1, false, -1, false, false);        

                if (lastPage > 1)
                    writer.write("\n<p>" + nMatchingHtml);

                //list plain file types
                writer.write(
                    "\n" +
                    "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                    plainFileTypesString + //not links, which would be indexed by search engines
                    ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                        EDStatic.restfulViaService + "</a>.\n");

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t; 
            } finally {

                //end of document
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        if (verbose) String2.log(EDStatic.resourceNotFound + " end of doCategorize");
        sendResourceNotFoundError(request, response, "");
    }

    /**
     * Process an info request: erddap/info/[{datasetID}/index.xxx]
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "info") in the requestUrl
     * @throws Throwable if trouble
     */
    public void doInfo(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String protocol, int datasetIDStartsAt) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);
        String fileTypeName = File2.getExtension(endOfRequestUrl);

        if (requestUrl.equals("/" + EDStatic.warName + "/info") ||
            requestUrl.equals("/" + EDStatic.warName + "/info/") ||
            requestUrl.equals("/" + EDStatic.warName + "/info/index.htm")) {
            sendRedirect(response, tErddapUrl + "/info/index.html?" +
                EDStatic.passThroughPIppQueryPage1(request));
            return;
        }

        String parts[] = String2.split(endOfRequestUrl, '/');
        int nParts = parts.length;
        if (nParts == 0 || !parts[nParts - 1].startsWith("index.")) {
            StringArray sa = new StringArray(parts);
            sa.add("index.html");
            parts = sa.toArray();
            nParts = parts.length;
            //now last part is "index...."
        }
        fileTypeName = File2.getExtension(endOfRequestUrl);        
        boolean endsWithPlainFileType = endsWithPlainFileType(parts[nParts - 1], "index");
        if (!endsWithPlainFileType && !fileTypeName.equals(".html")) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.unsupportedFileType, fileTypeName));
            return;
        }
        EDStatic.tally.add("Info File Type (since startup)", fileTypeName);
        EDStatic.tally.add("Info File Type (since last daily report)", fileTypeName);
        if (nParts < 2) {
            //*** info/index.xxx    view all datasets 

            //ensure query has simplistically valid page= itemsPerPage=
            if (!Arrays.equals(
                    EDStatic.getRawRequestedPIpp(request),
                    EDStatic.getRequestedPIpp(request))) {
                sendRedirect(response, 
                    EDStatic.baseUrl(loggedInAs) + request.getRequestURI() + "?" +
                    EDStatic.passThroughJsonpQuery(request) +
                    EDStatic.passThroughPIppQuery(request)); 
                return;
            }      

            //get the datasetIDs
            //(sortByTitle ensures user has right to know dataset exists)
            StringArray tIDs = sortByTitle(loggedInAs, allDatasetIDs(), 
                true); //info: this is a metadata request 
            int nDatasets = tIDs.size();
            EDStatic.tally.add("Info (since startup)", "View All Datasets");
            EDStatic.tally.add("Info (since last daily report)", "View All Datasets");

            //calculate Page ItemsPerPage and remove other tIDs  (part of: View All Datasets)
            int pIpp[] = EDStatic.calculatePIpp(request, nDatasets);
            int page         = pIpp[0]; //will be 1... 
            int itemsPerPage = pIpp[1]; //will be 1...
            int startIndex   = pIpp[2]; //will be 0...
            int lastPage     = pIpp[3]; //will be 1...

            //reduce tIDs to ones on requested page
            //IMPORTANT!!! For this to work correctly, datasetIDs must be 
            //  accessibleTo loggedInAs (or EDStatic.listPrivateDatasets)
            //  and in final sorted order.
            //  (True here)
            //Order of removal: more efficient to remove items at end, then items at beginning.
            if (startIndex + itemsPerPage < nDatasets) 
                tIDs.removeRange(startIndex + itemsPerPage, nDatasets);
            tIDs.removeRange(0, Math.min(startIndex, nDatasets));

            //if non-null, error will be String[2]
            String error[] = null;
            if (nDatasets == 0) {
                error = new String[] {
                    MustBe.THERE_IS_NO_DATA,
                    ""};
            } else if (page > lastPage) {
                error = EDStatic.noPage(page, lastPage);
            }

            boolean sortByTitle = false; //already sorted above
            if (fileTypeName.equals(".html")) {
                //make the table with the dataset list
                Table table = makeHtmlDatasetTable(loggedInAs, tIDs, sortByTitle);

                //display start of web page
                OutputStream out = getHtmlOutputStream(request, response);
                Writer writer = getHtmlWriter(loggedInAs, 
                    MessageFormat.format(EDStatic.listOfDatasets, EDStatic.listAll),
                    out); 
                try {
                    //you are here  View All Datasets
                    String secondLine = error == null?
                        "" : //"<h2>" + EDStatic.pickADataset + "</h2>\n" :
                        "&nbsp;<br><strong>" + XML.encodeAsHTML(error[0]) +"</strong>\n" +
                              "<br>"    + XML.encodeAsHTML(error[1]) +"\n" +
                              "<br>&nbsp;\n";

                    String nMatchingHtml = table.nRows() == 0? "" :
                        EDStatic.nMatchingDatasetsHtml(
                            nDatasets, page, lastPage, false,  //=alphabetical
                            EDStatic.baseUrl(loggedInAs) + requestUrl + 
                            EDStatic.questionQuery(request.getQueryString())) + 
                        "<br>&nbsp;\n";

                    writer.write(                        
                        "<div class=\"standard_width\">\n" +

                        //getYouAreHereTable(
                        EDStatic.youAreHere(loggedInAs, 
                            MessageFormat.format(EDStatic.listOfDatasets, EDStatic.listAll)) +
                        secondLine +
                        nMatchingHtml);

                        /*//Or, search text
                        "&nbsp;\n" +
                        "<br>" + getSearchFormHtml(request, loggedInAs, EDStatic.orComma, ":\n<br>", "") +
                        //Or, by category
                        "<p>" + getCategoryLinksHtml(request, tErddapUrl) +
                        //Or,
                        "<p>" + EDStatic.orSearchWith + 
                            getAdvancedSearchLink(loggedInAs, 
                                EDStatic.passThroughPIppQueryPage1(request)))); 
                        */                  

                    if (table.nRows() > 0) {
                      
                        //show the table of all datasets 
                        table.saveAsHtmlTable(writer, "commonBGColor", null, 
                            1, false, -1, false, false);        

                        if (lastPage > 1)
                            writer.write("\n<p>" + nMatchingHtml);

                        //list plain file types
                        writer.write(
                            "\n" +
                            "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                            plainFileTypesString + //not links, which would be indexed by search engines
                            ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                                EDStatic.restfulViaService + "</a>.\n");
                    }


                    //jsonld
                    if (EDStatic.jsonldActive) { //&& isSchemaDotOrgEnabled()){
                        try {
                            writer.flush(); //so content above is sent to user ASAP while this content is created
                            String roles[] = EDStatic.getRoles(loggedInAs);
                            ArrayList<EDD> datasets = new ArrayList<EDD>();
                            for (int i = 0; i < tIDs.size(); i++) {
                                String tId = tIDs.get(i);
                                boolean isAllDatasets = tId.equals(EDDTableFromAllDatasets.DATASET_ID);
                                if (isAllDatasets)
                                    continue;
                                EDD edd = gridDatasetHashMap.get(tId);
                                if (edd == null)
                                    edd = tableDatasetHashMap.get(tId);
                                if (edd == null)  //if just deleted
                                    continue;
                                boolean isAccessible = edd.isAccessibleTo(roles);
                                boolean graphsAccessible = isAccessible || edd.graphsAccessibleToPublic();
                                if (!EDStatic.listPrivateDatasets && !isAccessible && !graphsAccessible)
                                    continue;
                                datasets.add(edd);
                            }
                            //javascript version: writer.write(EDStatic.theSchemaDotOrgDataCatalog(datasets.toArray(new EDD[datasets.size()])));                
                            //java version:
                            theSchemaDotOrgDataCatalog(writer, datasets.toArray(new EDD[datasets.size()]));
                        } catch (Exception e) {
                            EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                            String2.log(
                                "Caught ERROR while writing jsonld for all datasets:\n" +
                                MustBe.throwableToString(e));
                        }
                    }

                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write(EDStatic.htmlForException(t));
                    throw t; 
                } finally {

                    //end of document
                    writer.write("</div>\n");
                    endHtmlWriter(out, writer, tErddapUrl, false);
                }
            } else {
                if (error != null)
                    throw new SimpleException(error[0] + " " + error[1]);

                Table table = makePlainDatasetTable(loggedInAs, tIDs, sortByTitle, fileTypeName);
                sendPlainTable(loggedInAs, request, response, table, protocol, fileTypeName);
            }
            return;
        }
        if (nParts > 2) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.infoRequestForm, EDStatic.warName));
            return;
        }
        String tID = parts[0];
        EDD edd = gridDatasetHashMap.get(tID);
        if (edd == null)
            edd = tableDatasetHashMap.get(tID);
        if (edd == null) { 
            sendResourceNotFoundError(request, response,
                MessageFormat.format(EDStatic.unknownDatasetID, tID));
            return;
        }
        if (!edd.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
            !edd.graphsAccessibleToPublic()) { 
            // /info/ request: all requests are graphics|metadata requests
            //listPrivateDatasets doesn't apply
            EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tID, 
                false);
            return;
        }

        //request is valid -- make the table
        EDStatic.tally.add("Info (since startup)", tID);
        EDStatic.tally.add("Info (since last daily report)", tID);
        Table table = new Table();
        StringArray rowTypeSA = new StringArray();
        StringArray variableNameSA = new StringArray();
        StringArray attributeNameSA = new StringArray();
        StringArray javaTypeSA = new StringArray();
        StringArray valueSA = new StringArray();
        table.addColumn("Row Type", rowTypeSA);
        table.addColumn("Variable Name", variableNameSA);
        table.addColumn("Attribute Name", attributeNameSA);
        table.addColumn("Data Type", javaTypeSA);
        table.addColumn("Value", valueSA);

        //global attribute rows
        Attributes atts = edd.combinedGlobalAttributes();
        String names[] = atts.getNames();
        int nAtts = names.length;
        for (int i = 0; i < nAtts; i++) {
            rowTypeSA.add("attribute");
            variableNameSA.add("NC_GLOBAL");
            attributeNameSA.add(names[i]);
            PrimitiveArray value = atts.get(names[i]);
            javaTypeSA.add(value.elementClassString());
            valueSA.add(Attributes.valueToNcString(value));
        }

        //dimensions
        String axisNamesCsv = "";
        if (edd instanceof EDDGrid) {
            EDDGrid eddGrid = (EDDGrid)edd;
            int nDims = eddGrid.axisVariables().length;
            axisNamesCsv = String2.toCSSVString(eddGrid.axisVariableDestinationNames());
            for (int dim = 0; dim < nDims; dim++) {
                //dimension row
                EDVGridAxis edv = eddGrid.axisVariables()[dim];
                rowTypeSA.add("dimension");
                variableNameSA.add(edv.destinationName());
                attributeNameSA.add("");
                javaTypeSA.add(edv.destinationDataType());
                int tSize = edv.sourceValues().size();
                double avgSp = edv.averageSpacing(); //may be negative
                if (tSize == 1) {
                    double dValue = edv.firstDestinationValue();
                    valueSA.add(  //for now, don't translate these, so consistent in all ERDDAPs
                        "nValues=1, onlyValue=" + 
                        (Double.isNaN(dValue)? "NaN" : edv.destinationToString(dValue))); //want "NaN", not ""
                } else {
                    valueSA.add(
                        "nValues=" + tSize + 
                        ", evenlySpaced=" + (edv.isEvenlySpaced()? "true" : "false") +
                        ", averageSpacing=" + 
                        (edv instanceof EDVTimeGridAxis? 
                            Calendar2.elapsedTimeString(Math.rint(avgSp) * 1000) : 
                            avgSp)
                        );
                }

                //attribute rows
                atts = edv.combinedAttributes();
                names = atts.getNames();
                nAtts = names.length;
                for (int i = 0; i < nAtts; i++) {
                    rowTypeSA.add("attribute");
                    variableNameSA.add(edv.destinationName());
                    attributeNameSA.add(names[i]);
                    PrimitiveArray value = atts.get(names[i]);
                    javaTypeSA.add(value.elementClassString());
                    valueSA.add(Attributes.valueToNcString(value));
                }
            }
        }

        //data variables
        int nVars = edd.dataVariables().length;
        for (int var = 0; var < nVars; var++) {
            //data variable row
            EDV edv = edd.dataVariables()[var];
            rowTypeSA.add("variable");
            variableNameSA.add(edv.destinationName());
            attributeNameSA.add("");
            javaTypeSA.add(edv.destinationDataType());
            valueSA.add(axisNamesCsv);

            //attribute rows
            atts = edv.combinedAttributes();
            names = atts.getNames();
            nAtts = names.length;
            for (int i = 0; i < nAtts; i++) {
                rowTypeSA.add("attribute");
                variableNameSA.add(edv.destinationName());
                attributeNameSA.add(names[i]);
                PrimitiveArray value = atts.get(names[i]);
                javaTypeSA.add(value.elementClassString());
                valueSA.add(Attributes.valueToNcString(value));
            }
        }

        //write the file
        if (endsWithPlainFileType) {
            sendPlainTable(loggedInAs, request, response, table, parts[0] + "_info", fileTypeName);
            return;
        }

        //respond to index.html request
        if (parts[1].equals("index.html")) {
            //display start of web page
            OutputStream out = getHtmlOutputStream(request, response);
            Writer writer = getHtmlWriter(loggedInAs, 
                MessageFormat.format(EDStatic.infoAboutFrom, edd.title(), edd.institution()), 
                out); 
            try {
                writer.write("<div class=\"wide_max_width\">\n");  //not standard_width
                writer.write(EDStatic.youAreHere(loggedInAs, protocol, parts[0]));

                //display a table with the one dataset
                StringArray sa = new StringArray();
                sa.add(parts[0]);
                boolean sortByTitle = true;
                Table dsTable = makeHtmlDatasetTable(loggedInAs, sa, sortByTitle);
                dsTable.saveAsHtmlTable(writer, "commonBGColor", null, 1, false, -1, false, false);        

                //html format the valueSA values
                String externalLinkHtml = EDStatic.externalLinkHtml(tErddapUrl);
                for (int i = 0; i < valueSA.size(); i++) {                    
                    String s = valueSA.get(i);
                    if (String2.isUrl(s)) {
                        //display as a link
                        boolean isLocal = s.startsWith(EDStatic.baseUrl);
                        s = XML.encodeAsHTMLAttribute(s);
                        valueSA.set(i, "<a href=\"" + s + "\">" + s + 
                            (isLocal? "" : externalLinkHtml) + "</a>");
                    } else if (String2.isEmailAddress(s)) {
                        //to improve security, convert "@" to " at "
                        s = XML.encodeAsHTMLAttribute(String2.replaceAll(s, "@", " at "));
                        valueSA.set(i, s);
                    } else {
                        valueSA.set(i, XML.encodeAsPreHTML(s, 10000));  //???
                    }
                }

                //display the info table
                writer.write("<h2>" + EDStatic.infoTableTitleHtml + "</h2>");

                //******** custom table writer (to change color on "variable" rows)
                writer.write(
                    "<table class=\"erd commonBGColor\">\n"); 

                //write the column names   
                writer.write("<tr>\n");
                int nColumns = table.nColumns();
                for (int col = 0; col < nColumns; col++) 
                    writer.write("<th>" + table.getColumnName(col) + "</th>\n");
                writer.write("</tr>\n");

                //write the data
                int nRows = table.nRows();
                for (int row = 0; row < nRows; row++) {
                    String s = table.getStringData(0, row);
                    if (s.equals("variable") || s.equals("dimension"))
                         writer.write("<tr class=\"highlightBGColor\">\n"); 
                    else writer.write("<tr>\n"); 
                    for (int col = 0; col < nColumns; col++) {
                        writer.write("<td>"); 
                        s = table.getStringData(col, row);
                        writer.write(s.length() == 0? "&nbsp;" : s); 
                        writer.write("</td>\n");
                    }
                    writer.write("</tr>\n");
                }

                //close the table
                writer.write("</table>\n");

                //list plain file types
                writer.write(
                    "\n" +
                    "<p>" + EDStatic.restfulInformationFormats + " \n(" +
                    plainFileTypesString + //not links, which would be indexed by search engines
                    ") <a rel=\"help\" href=\"" + tErddapUrl + "/rest.html\">" + 
                        EDStatic.restfulViaService + "</a>.\n");

                //jsonld
                if (EDStatic.jsonldActive) { //javascript: && EDStatic.isSchemaDotOrgEnabled()) {
                    try {
                        String tId = parts[0];
                        boolean isAllDatasets = tId.equals(EDDTableFromAllDatasets.DATASET_ID);
                        if (!isAllDatasets) {
                            //javascript version: writer.write(EDStatic.theSchemaDotOrgDataset(edd));
                            //java version:
                            theSchemaDotOrgDataset(writer, edd);
                        }
                    } catch (Exception e) {
                        EDStatic.rethrowClientAbortException(e);  //first thing in catch{}
                        String2.log(
                            "Caught ERROR while writing jsonld for " + edd.datasetID() + ":\n" +
                            MustBe.throwableToString(e));
                    }
                }

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                writer.write(EDStatic.htmlForException(t));
                throw t; 
            } finally {

                //end of document
                writer.write("</div>\n");
                endHtmlWriter(out, writer, tErddapUrl, false);
            }
            return;
        }

        if (verbose) String2.log(EDStatic.resourceNotFound + " end of doInfo");
        sendResourceNotFoundError(request, response, "");
    }

    /**
     * Return an object representation of the DataCatalog.
     * This hard-coded Java version mimics the method of the same name in 
     * &lt;jsonld&gt; in messages.xml. See https://schema.org/ class definitions.
     * See Google intro: https://developers.google.com/search/docs/guides/intro-structured-data
     * See Google test: https://search.google.com/structured-data/testing-tool/u/0/
     * @throws IOException if trouble
     */
    public static void theSchemaDotOrgDataCatalog(Writer writer, EDD datasets[]) 
            throws IOException {
        String baseUrl = EDStatic.preferredErddapUrl;
writer.write(
"<script type=\"application/ld+json\">\n" +
"{\n" +
"  \"@context\": \"http://schema.org\",\n" +
"  \"@type\": \"DataCatalog\",\n" +
"  \"name\": " + String2.toJson65536("ERDDAP Data Server at " + EDStatic.adminInstitution) +",\n" +
"  \"url\": " + String2.toJson65536(baseUrl) + ",\n" +
"  \"publisher\": {\n" +
"    \"@type\": \"Organization\",\n" +
"    \"name\": " + String2.toJson65536(EDStatic.adminInstitution) + ",\n" +
"    \"address\": {\n" +
"      \"@type\": \"PostalAddress\",\n" +
"      \"addressCountry\": " + String2.toJson65536(EDStatic.adminCountry) + ",\n" +
"      \"addressLocality\": " + String2.toJson65536(EDStatic.adminAddress + ", " +
        EDStatic.adminCity) + ",\n" +
"      \"addressRegion\": " + String2.toJson65536(EDStatic.adminStateOrProvince) + ",\n" +
"      \"postalCode\": " + String2.toJson65536(EDStatic.adminPostalCode) + "\n" +
"    },\n" +
"    \"telephone\": " + String2.toJson65536(EDStatic.adminPhone) + ",\n" +
"    \"email\": " + String2.toJson65536(EDStatic.adminEmail) + ",\n" +
"    \"sameAs\": " + String2.toJson65536(EDStatic.adminInstitutionUrl) + "\n" +
"  },\n" +
"  \"fileFormat\": [\n" +
"    \"application/geo+json\",\n" +
"    \"application/json\",\n" +
"    \"text/csv\"\n" +
"  ],\n" +
"  \"isAccessibleForFree\": \"True\",\n" +
"  \"dataset\": [\n");

for (int i=0; i < datasets.length; i++) {
writer.write(
"    {\n" +
"      \"@type\": \"Dataset\",\n" +
"      \"name\": " + String2.toJson65536(datasets[i].title()) + ",\n" +
"      \"sameAs\": " + String2.toJson65536(baseUrl + "/info/" + datasets[i].datasetID() + "/index.html") + "\n" +
"    }" + (i == datasets.length - 1? "" : ",") + "\n");
}

writer.write(
"  ]\n" +
"}\n" +
"</script>\n");

    }

    /** 
     * This gets creator or publisher info for jsonld, based on ACDD attribute info.
     *
     * @param tType is "creator" or "publisher"
     */
    private static String getJsonldInfo(String tType, Attributes gatts) {
        String s = "person".equals(gatts.getString(tType + "_type"))? 
            "Person" : "Organization";

        StringBuilder sb = new StringBuilder(
            ",\n" +
            "  " + String2.toJson65536(tType) + ": {\n" +
            "    \"@type\": " + String2.toJson65536(s) + ",\n" +
            "    \"name\": " + String2.toJson65536(gatts.getString(tType + "_name")));
        if (String2.isSomething(s = gatts.getString(tType + "_email")))
            sb.append(
            ",\n" +
            "    \"email\": " + String2.toJson65536(s));
        if (String2.isSomething(s = gatts.getString(tType + "_url")))
            sb.append(
            ",\n" +
            "    \"sameAs\": " + String2.toJson65536(s));
        sb.append(
            "\n" +
            "  }");

        return sb.toString();
    }

    /**
     * Return an jsonld object representation of the Dataset.
     * This hard-coded Java version mimics the method of the same name in 
     * &lt;jsonld&gt; in messages.xml. See https://schema.org/ class definitions.
     * See Google intro: https://developers.google.com/search/docs/guides/intro-structured-data
     * See Google test: https://search.google.com/structured-data/testing-tool/u/0/
     * @throws IOException if trouble
     */
    public static void theSchemaDotOrgDataset(Writer writer, EDD edd) throws IOException {
        String baseUrl = EDStatic.preferredErddapUrl;
        Attributes gatts = edd.combinedGlobalAttributes();
        String ts;

        writer.write(
            "<script type=\"application/ld+json\">\n" +
            "{\n" +
            "  \"@context\": \"http://schema.org\",\n" + //for now, leave as http://
            "  \"@type\": \"Dataset\",\n" +
            "  \"name\": " + String2.toJson65536(edd.title()) + ",\n" +
            "  \"headline\": " + String2.toJson65536(edd.datasetID()) + ",\n");

        //add everything not used elsewhere into description
        String names[] = gatts.getNames();
        StringBuilder sb = new StringBuilder(edd.summary());
        for (int j = 0; j < names.length; j++) {
            String tName = names[j];
            if (tName.startsWith("creator_") ||
                tName.startsWith("publisher_") ||
                tName.equals("date_created") ||
                tName.equals("date_issued") ||
                tName.equals("date_modified") ||
                tName.equals("keywords") ||
                tName.equals("license") ||
                tName.equals("product_version") ||
                tName.equals("summary") ||
                tName.equals("title"))
                continue;
            sb.append("\n" + tName + "=" + gatts.getString(tName));
        } 
        writer.write(
            "  \"description\": " + String2.toJson65536(sb.toString()) + ",\n" +
            "  \"url\": " + String2.toJson65536(baseUrl + "/" + 
                (edd instanceof EDDGrid? "griddap" : "tabledap") + "/" +
                edd.datasetID() + ".html") + ",\n" +
            "  \"includedInDataCatalog\": {\n" +
            "    \"@type\": \"DataCatalog\",\n" +
            "    \"name\": " + String2.toJson65536("ERDDAP Data Server at " + 
                EDStatic.adminInstitution) + ",\n" +
            "    \"sameAs\": " + String2.toJson65536(baseUrl) + "\n" +
            "  },\n");

        writer.write(
            "  \"keywords\": [\n");
        String keywords[] = edd.keywords();
        for (int i = 0; i < keywords.length; i++)
            writer.write(
            "    " + String2.toJson65536(keywords[i]) +
                (i < keywords.length - 1? "," : "") + "\n");
        writer.write(
            "  ],\n");

        if (String2.isSomething(gatts.getString("license")))
            writer.write(
            "  \"license\": " + String2.toJson65536(gatts.getString("license")) + ",\n");

        //variableMeasured
        String temporalCoverage = "";
        ArrayList<EDV> edv = new ArrayList();
        EDV arr[];
        int nAxisVariables = 0;
        if (edd instanceof EDDGrid) {  //axisVars first so lat/lon/timeIndex are correct
            arr = ((EDDGrid)edd).axisVariables();
            nAxisVariables = arr.length;
            for (int j=0; j<arr.length; j++) 
                edv.add(arr[j]);
        }
        arr = edd.dataVariables();
        for (int j=0; j<arr.length; j++) 
            edv.add(arr[j]);
        writer.write(
                "  \"variableMeasured\": [\n");
        for (int i=0; i < edv.size(); i++) {
            Attributes atts = edv.get(i).combinedAttributes();
            writer.write(
                (i == 0? "" : ",\n") +
                "    {\n" +
                "      \"@type\": \"PropertyValue\",\n" +
                "      \"name\": " + String2.toJson65536(edv.get(i).destinationName()) + ",\n" +
                "      \"alternateName\": " + String2.toJson65536(edv.get(i).longName()) + ",\n" +
                "      \"description\": " + String2.toJson65536(edv.get(i).longName()) + ",\n" +
                "      \"valueReference\": [\n" +  //or append to description?
                "        {\n" +
                "          \"@type\": \"PropertyValue\",\n" +
                "          \"name\": \"axisOrDataVariable\",\n" +
                "          \"value\": \"" + (i < nAxisVariables? "axis" : "data") + "\"\n" +
                "        }");  // ,\n pending

            //add everything not used into valueReference
            names = atts.getNames();
            boolean somethingWritten = false;
            for (int j = 0; j < names.length; j++) {
                String tName = names[j];
                if (tName.equals("actual_range") ||
                    tName.equals("units"))
                    continue;
                PrimitiveArray pa = atts.get(tName);
                writer.write(
                ",\n" +
                "        {\n" +
                "          \"@type\": \"PropertyValue\",\n" +
                "          \"name\": " + String2.toJson65536(tName) + ",\n" +
                "          \"value\": " + 
                    (pa instanceof StringArray ||
                     pa instanceof CharArray? 
                        String2.toJson65536(pa.getString(0)) : 
                        String2.toJson(pa.getNiceDouble(0))) + "\n" +
                "        }"); //\n pending
                somethingWritten = true;
            }
            writer.write(
                "\n" + 
                "      ]");

            if (EDV.TIME_NAME.equals(edv.get(i).destinationName())) {
                String maxValue = edv.get(i).destinationMaxString();
                if (maxValue.length() > 0) 
                    writer.write(
                    ",\n" +
                    "      \"maxValue\": " + String2.toJson65536(maxValue));

                String minValue = edv.get(i).destinationMinString();
                if (minValue.length() > 0) {
                    writer.write(
                    ",\n" +
                    "      \"minValue\": " + String2.toJson65536(minValue));

                    if (maxValue.length() > 0) 
                        temporalCoverage = 
                    ",\n" +
                    "  \"temporalCoverage\": " + String2.toJson65536(minValue+"/"+maxValue);
                }
     
                writer.write(
                    ",\n" +
                    "      \"propertyID\": \"time\"");

             } else {
                double maxValue = edv.get(i).destinationMax();
                if (!Double.isNaN(maxValue)) 
                    writer.write(
                    ",\n" +
                    "      \"maxValue\": " + String2.toJson(maxValue));
     
                double minValue = edv.get(i).destinationMin();
                if (!Double.isNaN(minValue)) 
                    writer.write(
                    ",\n" +
                    "      \"minValue\": " + String2.toJson(minValue));
     
                String stdName = atts.getString("standard_name");
                if (String2.isSomething(stdName))
                    writer.write(
                    ",\n" +
                    "      \"propertyID\": " + String2.toJson65536(stdName));
     
                if (String2.isSomething(edv.get(i).units())) 
                    writer.write(
                    ",\n" +
                    "      \"unitText\": " + String2.toJson65536(edv.get(i).units()));
            }
            writer.write(
                "\n" +
                "    }"); 
        }
        writer.write(
        "\n" +
        "  ]");
        //from now on ,\n is pending

        if (String2.isSomething(gatts.getString("creator_name")))
            writer.write(getJsonldInfo("creator", gatts));

        if (String2.isSomething(gatts.getString("publisher_name")))
            writer.write(getJsonldInfo("publisher", gatts));

        ts = gatts.getString("date_created");
        if (String2.isSomething(ts) && ts.matches("[0-9]{4}-[0-9]{2}.*"))
            writer.write(
            ",\n" +
            "  \"dateCreated\": " + String2.toJson65536(ts));

        ts = gatts.getString("date_issued");
        if (String2.isSomething(ts) && ts.matches("[0-9]{4}-[0-9]{2}.*"))
            writer.write(
            ",\n" +
            "  \"datePublished\": " + String2.toJson65536(ts));

        ts = gatts.getString("date_modified");
        if (String2.isSomething(ts) && ts.matches("[0-9]{4}-[0-9]{2}.*"))
            writer.write(
            ",\n" +
            "  \"dateModified\": " + String2.toJson65536(ts));

        //identifier from doiUrl/doi or (reversedDomain?/)datasetID
        String na = gatts.getString("naming_authority");
        String id = gatts.getString("id");
        if (String2.isSomething(na) && String2.isSomething(id) && 
            id.indexOf("doi.org") < 0) { //some datasets have a placeholder id
            writer.write(
            ",\n" +
            "  \"identifier\": " + String2.toJson65536(na + "/" + id));
        } else {
            writer.write(
            ",\n" +
            "  \"identifier\": " + String2.toJson65536(edd.datasetID()));
        }

        ts = gatts.getString("product_version");
        if (String2.isSomething(ts))
            writer.write(
            ",\n" +
            "  \"version\": " + String2.toJson65536(ts));

        //temporalCoverage
        writer.write(temporalCoverage);

        //spatialCoverage
        int ilat, ilon;
        if (edd instanceof EDDGrid) {
            ilat = ((EDDGrid)edd).latIndex();
            ilon = ((EDDGrid)edd).lonIndex();
        } else {
            ilat = ((EDDTable)edd).latIndex();
            ilon = ((EDDTable)edd).lonIndex();
        }
        if (ilat >= 0 && ilon >= 0) {
            double west  = edv.get(ilon).destinationMin();
            double east  = edv.get(ilon).destinationMax();
            double south = edv.get(ilat).destinationMin();
            double north = edv.get(ilat).destinationMax();
            if (!Double.isNaN(west)  && !Double.isNaN(east) && 
                !Double.isNaN(north) && !Double.isNaN(south)) {
                if (west >= 180) {
                     west -= 360; east -= 360;
                }
                if (west < 180 && east > 180) { //span date line?
                     west = -180; east = 180;
                }
                //https://schema.org/GeoShape doesn't actually specify the order
                //see https://github.com/schemaorg/schemaorg/issues/1538
                writer.write(
                ",\n" +
                "  \"spatialCoverage\": {\n" +
                "    \"@type\": \"Place\",\n" +
                "    \"geo\": {\n" +
                "      \"@type\": \"GeoShape\",\n" +
                "      \"box\": \"" + String2.toJson(south) + " " + 
                                      String2.toJson(west) + " " + 
                                      String2.toJson(north) + " " + 
                                      String2.toJson(east) + "\"\n" +
                "    }\n" +
                "  }");
            }
        }

        writer.write(
            "\n" +
            "}\n" +
            "</script>\n");

    }

    /**
     * Process erddap/subscriptions/index.html
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param ipAddress the requestor's ipAddress
     * @param endOfRequest e.g., subscriptions/add.html
     * @param protocol is always subscriptions
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "subscriptions") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doSubscriptions(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String ipAddress,
        String endOfRequest, String protocol, int datasetIDStartsAt, String userQuery) throws Throwable {

        if (!EDStatic.subscriptionSystemActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "subscriptions"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);

        if (endOfRequest.equals("subscriptions") ||
            endOfRequest.equals("subscriptions/")) {
            sendRedirect(response, tErddapUrl + "/" + Subscriptions.INDEX_HTML);
            return;
        }

        EDStatic.tally.add("Subscriptions (since startup)", endOfRequest);
        EDStatic.tally.add("Subscriptions (since last daily report)", endOfRequest);

        if (endOfRequest.equals(Subscriptions.INDEX_HTML)) {
            //fall through
        } else if (endOfRequest.equals(Subscriptions.ADD_HTML)) {
            doAddSubscription(request, response, loggedInAs, ipAddress, protocol, datasetIDStartsAt, userQuery);
            return;
        } else if (endOfRequest.equals(Subscriptions.LIST_HTML)) {
            doListSubscriptions(request, response, loggedInAs, ipAddress, protocol, datasetIDStartsAt, userQuery);
            return;
        } else if (endOfRequest.equals(Subscriptions.REMOVE_HTML)) {
            doRemoveSubscription(request, response, loggedInAs, protocol, datasetIDStartsAt, userQuery);
            return;
        } else if (endOfRequest.equals(Subscriptions.VALIDATE_HTML)) {
            doValidateSubscription(request, response, loggedInAs, protocol, datasetIDStartsAt, userQuery);
            return;
        } else {
            if (verbose) String2.log(EDStatic.resourceNotFound + " end of Subscriptions");
            sendResourceNotFoundError(request, response, "");
            return;
        }

        //display start of web page
        if (reallyVerbose) String2.log("doSubscriptions");
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, EDStatic.subscriptionsTitle, out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, protocol) +
                EDStatic.subscription0Html + 
                MessageFormat.format(EDStatic.subscription1Html, tErddapUrl) + "\n");
            writer.write(
                "<p><strong>" + EDStatic.options + ":</strong>\n" +
                "<ul>\n" +
                "<li> <a rel=\"bookmark\" href=\"" + tErddapUrl + "/" + Subscriptions.ADD_HTML      + "\">" + EDStatic.subscriptionAdd      + "</a>\n" +
                "<li> <a rel=\"bookmark\" href=\"" + tErddapUrl + "/" + Subscriptions.VALIDATE_HTML + "\">" + EDStatic.subscriptionValidate + "</a>\n" +
                "<li> <a rel=\"bookmark\" href=\"" + tErddapUrl + "/" + Subscriptions.LIST_HTML     + "\">" + EDStatic.subscriptionList     + "</a>\n" +
                "<li> <a rel=\"bookmark\" href=\"" + tErddapUrl + "/" + Subscriptions.REMOVE_HTML   + "\">" + EDStatic.subscriptionRemove   + "</a>\n" +
                "</ul>\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }


    /** 
     * This html is used at the bottom of many doXxxSubscription web pages. 
     *
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @param tEmail  the user's email address (or "")
     */
    private String requestSubscriptionListHtml(String tErddapUrl, String tEmail) {
        return 
            "<br>&nbsp;\n" +
            "<p><strong>Or, you can request an email with a\n" +
            "<a class=\"N\" rel=\"bookmark\" href=\"" + 
                XML.encodeAsHTMLAttribute(tErddapUrl + "/" + Subscriptions.LIST_HTML + 
                (tEmail.length() > 0? "?email=" + tEmail : "")) +
            "\">list of your valid and pending subscriptions</a>.</strong>\n";
    }

           
    /**
     * Process erddap/subscriptions/add.html.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param ipAddress the requestor's ip address
     * @param protocol is always subscriptions
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "info") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doAddSubscription(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String ipAddress, String protocol, int datasetIDStartsAt, 
        String userQuery) throws Throwable {

        if (!EDStatic.subscriptionSystemActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "subscriptions"));
            return;
        }

        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=lowercase keys
        String tDatasetID = queryMap.get("datasetid"); 
        String tEmail     = queryMap.get("email");
        String tAction    = queryMap.get("action");
        if (tDatasetID == null) tDatasetID = "";
        if (tEmail     == null) tEmail     = "";
        if (tAction    == null) tAction    = "";
        boolean tEmailIfAlreadyValid = String2.parseBoolean(queryMap.get("emailifalreadyvalid")); //default=true 
        boolean tShowErrors = userQuery == null || userQuery.length() == 0? false :
            String2.parseBoolean(queryMap.get("showerrors")); //default=true

        //validate params
        String trouble = "";
        if (tDatasetID.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDUnspecified + "</span>\n";
        } else if (tDatasetID.length() > Subscriptions.DATASETID_LENGTH) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDTooLong + "</span>\n";
            tDatasetID = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else if (!String2.isFileNameSafe(tDatasetID)) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDInvalid + "</span>\n";
            tDatasetID = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else if (tDatasetID.equals(EDDTableFromAllDatasets.DATASET_ID)) {
            trouble += "<li><span class=\"warningColor\">'allDatasets' doesn't accept subscriptions.</span>\n";
            tDatasetID = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else {
            EDD edd = gridDatasetHashMap.get(tDatasetID);
            if (edd == null) 
                edd = tableDatasetHashMap.get(tDatasetID);
            if (edd == null) {
                trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDInvalid + "</span>\n";
                tDatasetID = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
            } else if (!edd.isAccessibleTo(EDStatic.getRoles(loggedInAs)) &&
                       !edd.graphsAccessibleToPublic()) { 
                //subscription: all requests are graphics|metadata requests
                //listPrivateDatasets doesn't apply
                EDStatic.sendHttpUnauthorizedError(loggedInAs, response, tDatasetID,
                    false);
                return;
            }
        }

        if (tEmail.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionEmailUnspecified + "</span>\n";
        } else if (tEmail.length() > Subscriptions.EMAIL_LENGTH) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionEmailTooLong + "</span>\n";
            tEmail = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else if (!String2.isEmailAddress(tEmail) ||
                   tEmail.startsWith("your.name") || tEmail.startsWith("your.email")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionEmailInvalid + "</span>\n";
            tEmail = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        if (tAction.length() == 0) {
            //no action is fine
        } else if (tAction.length() > Subscriptions.ACTION_LENGTH) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionUrlTooLong + "</span>\n";
            tAction = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else if (tAction.length() <= 10 || 
            !(tAction.startsWith("http://") || tAction.startsWith("https://")) ||
//??? Make it so ERDDAP admin must also okay subscription requests (so admin can screen out malicious requests)?
             //this isn't allowed because a remote user could use it to gain access to other services on this server
             EDStatic.urlIsLocalhost(tAction) ||
             //this isn't allowed because a remote user could use it to gain access to other services on other local servers
             tAction.startsWith("http://192.168.") || 
             tAction.startsWith("https://192.168.")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionUrlInvalid + "</span>\n";
            tAction = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else if (tAction.indexOf('<') >= 0 || tAction.indexOf('>') >= 0) {  //prevent e.g., <script>
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionUrlInvalid + "</span>\n";
            tAction = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        //display start of web page
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Add a Subscription", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, protocol, "add") +
                EDStatic.subscription0Html +
                MessageFormat.format(EDStatic.subscription1Html, tErddapUrl) + "\n" +
                MessageFormat.format(EDStatic.subscription2Html, tErddapUrl) + "\n");

            if (trouble.length() > 0) {
                if (tShowErrors) 
                    writer.write("<p><span class=\"warningColor\">" +
                    EDStatic.subscriptionAddError + "</span>\n" +
                    "<ul>\n" +
                    trouble + "\n" +
                    "</ul>\n");
            } else {
                //try to add 
                try {
                    int row = EDStatic.subscriptions.add(tDatasetID, tEmail, tAction);
                    if (tEmailIfAlreadyValid || 
                        EDStatic.subscriptions.readStatus(row) == Subscriptions.STATUS_PENDING) {
                        String invitation = EDStatic.subscriptions.getInvitation(ipAddress, row);
                        String tError = EDStatic.email(tEmail, "Subscription Invitation", invitation);
                        if (tError.length() > 0)
                            throw new SimpleException(tError);

                        //tally
                        EDStatic.tally.add("Subscriptions (since startup)", "Add successful");
                        EDStatic.tally.add("Subscriptions (since last daily report)", "Add successful");
                    }
                    writer.write(EDStatic.subscriptionAddSuccess + "\n");
                } catch (Throwable t) {
                    EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                    writer.write("<p><span class=\"warningColor\">" +
                        EDStatic.subscriptionAddError + "\n<br>" + 
                        XML.encodeAsHTML(MustBe.getShortErrorMessage(t)) + "</span>\n");
                    String2.log("Subscription Add Exception:\n" + MustBe.throwableToString(t)); //log stack trace, too

                    //tally
                    EDStatic.tally.add("Subscriptions (since startup)", "Add unsuccessful");
                    EDStatic.tally.add("Subscriptions (since last daily report)", "Add unsuccessful");
                }
            }

            //show the form
            String urlTT = EDStatic.subscriptionUrlHtml;
            writer.write(
                widgets.beginForm("addSub", "GET", tErddapUrl + "/" + Subscriptions.ADD_HTML, "") +
                MessageFormat.format(EDStatic.subscriptionAddHtml, tErddapUrl) + "\n" +
                widgets.beginTable("class=\"compact nowrap\"") +
                "<tr>\n" +
                "  <td>The datasetID:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("datasetID", 
                    "For example, " + EDStatic.EDDGridIdExample,
                    30, Subscriptions.DATASETID_LENGTH, tDatasetID, 
                    "") + " (required)</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>Your email address:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("email", "", 
                    53, Subscriptions.EMAIL_LENGTH, tEmail, 
                    "") + " (required)</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>The URL/action:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("action", urlTT,
                    53, Subscriptions.ACTION_LENGTH, tAction, "") + "\n" +
                "    " + EDStatic.htmlTooltipImage(loggedInAs, urlTT) +
                "  (optional)</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td colspan=\"2\">" + widgets.button("submit", null, 
                    EDStatic.clickToSubmit, "Submit", "") + "\n" +
                "    <br>" + EDStatic.subscriptionAdd2 + "\n" +
                "  </td>\n" +
                "</tr>\n" +
                widgets.endTable() +  
                widgets.endForm() +
                EDStatic.subscriptionAbuse + "\n");

            //link to list of subscriptions
            writer.write(requestSubscriptionListHtml(tErddapUrl, tEmail));
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/subscriptions/list.html
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param ipAddress the requestor's ip address
     * @param protocol is always subscriptions
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "info") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doListSubscriptions(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String ipAddress, String protocol, int datasetIDStartsAt, String userQuery) 
        throws Throwable {

        if (!EDStatic.subscriptionSystemActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "subscriptions"));
            return;
        }

        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=names toLowerCase
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //process the query
        String tEmail = queryMap.get("email");
        if (tEmail == null) tEmail = "";
        String trouble = "";
        if (tEmail.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionEmailUnspecified + "</span>\n";
        } else if (tEmail.length() > Subscriptions.EMAIL_LENGTH) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionEmailTooLong + "</span>\n";
            tEmail = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        } else if (!String2.isEmailAddress(tEmail) ||
                   tEmail.startsWith("your.name") || tEmail.startsWith("your.email")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionEmailInvalid + "</span>\n";
            tEmail = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        //display start of web page
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "List Subscriptions", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, protocol, "list") +
                EDStatic.subscription0Html +
                MessageFormat.format(EDStatic.subscription1Html, tErddapUrl) + "\n");

            if (userQuery != null && userQuery.length() > 0) {
                if (trouble.length() > 0) {
                    writer.write("<p><span class=\"warningColor\">" +
                        EDStatic.subscriptionListError + "</span>\n" + 
                        "<ul>\n" +
                        trouble + "\n" +
                        "</ul>\n");
                } else {
                    //try to list the subscriptions
                    try {
                        String tList = EDStatic.subscriptions.listSubscriptions(ipAddress, tEmail);
                        String tError = EDStatic.email(tEmail, "Subscriptions List", tList);
                        if (tError.length() > 0)
                            throw new SimpleException(tError);

                        writer.write(EDStatic.subscriptionListSuccess + "\n");
                        //end of document
                        writer.write("</div>\n");
                        endHtmlWriter(out, writer, tErddapUrl, false);

                        //tally
                        EDStatic.tally.add("Subscriptions (since startup)", "List successful");
                        EDStatic.tally.add("Subscriptions (since last daily report)", "List successful");
                        return;
                    } catch (Throwable t) {
                        EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                        writer.write("<p><span class=\"warningColor\">" +
                            EDStatic.subscriptionListError + "\n" + 
                            "<br>" + XML.encodeAsHTML(MustBe.getShortErrorMessage(t)) + "</span>\n");
                        String2.log("Subscription list Exception:\n" + MustBe.throwableToString(t)); //log the details

                        //tally
                        EDStatic.tally.add("Subscriptions (since startup)", "List unsuccessful");
                        EDStatic.tally.add("Subscriptions (since last daily report)", "List unsuccessful");
                    }
                }
            }

            //show the form
            writer.write(
                widgets.beginForm("listSub", "GET", tErddapUrl + "/" + Subscriptions.LIST_HTML, "") +
                MessageFormat.format(EDStatic.subscriptionListHtml, tErddapUrl) + "\n" +
                widgets.beginTable("class=\"compact\"") +
                "<tr>\n" +
                "  <td>Your email address:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("email", "", 
                    60, Subscriptions.EMAIL_LENGTH, tEmail, 
                    "") + "</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td colspan=\"2\">" + widgets.button("submit", null, 
                    EDStatic.clickToSubmit, "Submit", "") + "</td>\n" +
                "</tr>\n" +
                widgets.endTable() +  
                widgets.endForm() +
                EDStatic.subscriptionAbuse + "\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/subscriptions/validate.html
     *
     * @param protocol is always subscriptions
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "info") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doValidateSubscription(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String protocol, int datasetIDStartsAt, String userQuery) throws Throwable {

        if (!EDStatic.subscriptionSystemActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "subscriptions"));
            return;
        }

        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=names toLowerCase
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //process the query
        String tSubscriptionID = queryMap.get("subscriptionid"); //lowercase since case insensitive
        String tKey            = queryMap.get("key");
        if (tSubscriptionID == null) tSubscriptionID = "";
        if (tKey            == null) tKey            = "";
        String trouble = "";
        if (tSubscriptionID.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDUnspecified + "</span>\n";
        } else if (!tSubscriptionID.matches("[0-9]{1,10}")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDInvalid + "</span>\n";
            tSubscriptionID = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        if (tKey.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionKeyUnspecified + "</span>\n";
        } else if (!tKey.matches("[0-9]{1,10}")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionKeyInvalid + "</span>\n";
            tKey = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        //display start of web page
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Validate a Subscription", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, protocol, "validate") +
                EDStatic.subscription0Html +
                MessageFormat.format(EDStatic.subscription1Html, tErddapUrl) + "\n");

            if (userQuery != null && userQuery.length() > 0) {
                if (trouble.length() > 0) {
                    writer.write("<p><span class=\"warningColor\">" +
                        EDStatic.subscriptionValidateError + "</span>\n" +
                        "<ul>\n" +
                        trouble + "\n" +
                        "</ul>\n");
                } else {
                    //try to validate 
                    try {
                        String message = EDStatic.subscriptions.validate(
                            String2.parseInt(tSubscriptionID), String2.parseInt(tKey));
                        if (message.length() > 0) {
                            writer.write("<p><span class=\"warningColor\">" +
                                EDStatic.subscriptionValidateError + "\n" +
                                "<br>" + message + "</span>\n");

                        } else {
                            writer.write(EDStatic.subscriptionValidateSuccess + "\n");

                            //tally
                            EDStatic.tally.add("Subscriptions (since startup)", "Validate successful");
                            EDStatic.tally.add("Subscriptions (since last daily report)", "Validate successful");
                        }
                    } catch (Throwable t) {
                        EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                        writer.write("<p><span class=\"warningColor\">" +
                            EDStatic.subscriptionValidateError + "\n" +
                            "<br>" + XML.encodeAsHTML(MustBe.getShortErrorMessage(t)) + "</span>\n");
                        String2.log("Subscription validate Exception:\n" + MustBe.throwableToString(t));

                        //tally
                        EDStatic.tally.add("Subscriptions (since startup)", "Validate unsuccessful");
                        EDStatic.tally.add("Subscriptions (since last daily report)", "Validate unsuccessful");
                    }
                }
            }

            //show the form
            writer.write(
                widgets.beginForm("validateSub", "GET", 
                    tErddapUrl + "/" + Subscriptions.VALIDATE_HTML, "") +
                MessageFormat.format(EDStatic.subscriptionValidateHtml, tErddapUrl) + "\n" +
                widgets.beginTable("class=\"compact\"") +
                "<tr>\n" +
                "  <td>The subscriptionID:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("subscriptionID", "", 
                    15, 15, tSubscriptionID, "") + "</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>The key:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("key", "", 
                    15, 15, tKey, "") + "</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td colspan=\"2\">" + widgets.button("submit", null, 
                    EDStatic.clickToSubmit, "Submit", "") + "</td>\n" +
                "</tr>\n" +
                widgets.endTable() +  
                widgets.endForm());        

            //link to list of subscriptions
            writer.write(requestSubscriptionListHtml(tErddapUrl, ""));
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }


    /**
     * Process erddap/subscriptions/remove.html
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param protocol is always subscriptions
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "info") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doRemoveSubscription(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String protocol, int datasetIDStartsAt, String userQuery) 
        throws Throwable {

        if (!EDStatic.subscriptionSystemActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "subscriptions"));
            return;
        }

        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, true); //true=names toLowerCase
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //process the query
        String tSubscriptionID = queryMap.get("subscriptionid"); //lowercase since case insensitive
        String tKey            = queryMap.get("key");
        if (tSubscriptionID == null) tSubscriptionID = "";
        if (tKey            == null) tKey            = "";
        String trouble = "";
        if (tSubscriptionID.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDUnspecified + "</span>\n";
        } else if (!tSubscriptionID.matches("[0-9]{1,10}")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionIDInvalid + "</span>\n";
            tSubscriptionID = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        if (tKey.length() == 0) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionKeyUnspecified + "</span>\n";
        } else if (!tKey.matches("[0-9]{1,10}")) {
            trouble += "<li><span class=\"warningColor\">" + EDStatic.subscriptionKeyInvalid + "</span>\n";
            tKey = ""; //Security: if it was bad, don't show it in form (could be malicious java script)
        }

        //display start of web page
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Remove a Subscription", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, protocol, "remove") +
                EDStatic.subscription0Html +
                MessageFormat.format(EDStatic.subscription1Html, tErddapUrl) + "\n");

            if (userQuery != null && userQuery.length() > 0) {
                if (trouble.length() > 0) {
                    writer.write("<p><span class=\"warningColor\">" +
                        EDStatic.subscriptionRemoveError + "</span>\n" +
                        "<ul>\n" +
                        trouble + "\n" +
                        "</ul>\n");
                } else {
                    //try to remove 
                    try {
                        String message = EDStatic.subscriptions.remove(
                            String2.parseInt(tSubscriptionID), String2.parseInt(tKey));
                        if (message.length() > 0) 
                            writer.write("<p><span class=\"warningColor\">" +
                                EDStatic.subscriptionRemoveError + "\n" +
                                "<br>" + message + "</span>\n");
                        else writer.write(EDStatic.subscriptionRemoveSuccess + "\n");

                        //tally
                        EDStatic.tally.add("Subscriptions (since startup)", "Remove successful");
                        EDStatic.tally.add("Subscriptions (since last daily report)", "Remove successful");
                    } catch (Throwable t) {
                        EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                        writer.write("<p><span class=\"warningColor\">" +
                            EDStatic.subscriptionRemoveError + "\n" +
                            "<br>" + XML.encodeAsHTML(MustBe.getShortErrorMessage(t)) + "</span>\n");
                        String2.log("Subscription remove Exception:\n" + MustBe.throwableToString(t)); //log the details

                        //tally
                        EDStatic.tally.add("Subscriptions (since startup)", "Remove unsuccessful");
                        EDStatic.tally.add("Subscriptions (since last daily report)", "Remove unsuccessful");
                    }
                }
            }

            //show the form
            writer.write(
                widgets.beginForm("removeSub", "GET", 
                    tErddapUrl + "/" + Subscriptions.REMOVE_HTML, "") +
                MessageFormat.format(EDStatic.subscriptionRemoveHtml, tErddapUrl) + "\n" +
                widgets.beginTable("class=\"compact\"") +
                "<tr>\n" +
                "  <td>The subscriptionID:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("subscriptionID", "", 
                    15, 15, tSubscriptionID, "") + "</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td>The key:&nbsp;</td>\n" +
                "  <td>" + widgets.textField("key", "", 
                    15, 15, tKey, "") + "</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "  <td colspan=\"2\">" + widgets.button("submit", null, 
                    EDStatic.clickToSubmit, "Submit", "") + "</td>\n" +
                "</tr>\n" +
                widgets.endTable() +  
                widgets.endForm());        

            //link to list of subscriptions
            writer.write(requestSubscriptionListHtml(tErddapUrl, "") +
                "<br>" + EDStatic.subscriptionRemove2 + "\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }   


    /**
     * Process erddap/convert/index.html
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequest e.g., convert/time.html
     * @param datasetIDStartsAt is the position right after the / at the end of the protocol
     *    (always "convert") in the requestUrl
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvert(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, 
        String endOfRequest, int datasetIDStartsAt, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = request.getRequestURI();  //post EDStatic.baseUrl, pre "?"
        String endOfRequestUrl = datasetIDStartsAt >= requestUrl.length()? "" : 
            requestUrl.substring(datasetIDStartsAt);       

        if (endOfRequest.equals("convert") ||
            endOfRequest.equals("convert/")) {
            sendRedirect(response, tErddapUrl + "/convert/index.html");
            return;
        }

        EDStatic.tally.add("Convert (since startup)", endOfRequest);
        EDStatic.tally.add("Convert (since last daily report)", endOfRequest);
        String fileTypeName = File2.getExtension(requestUrl);
        int pft = String2.indexOf(plainFileTypes, fileTypeName);

        if (endOfRequestUrl.equals("index.html")) {
            //fall through

        //FIPS County
        } else if (endOfRequestUrl.equals("fipscounty.html") ||
                   endOfRequestUrl.equals("fipscounty.txt")) {
            doConvertFipsCounty(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else if (endOfRequestUrl.startsWith("fipscounty.") && pft >= 0) {
            try {
                sendPlainTable(loggedInAs, request, response, 
                    EDStatic.fipsCountyTable(), "FipsCountyCodes", fileTypeName);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                throw new SimpleException("The FIPS county service is not available on this ERDDAP.", t);
            }
            return;

        //OceanicAtmospheric Acronyms
        } else if (endOfRequestUrl.equals("oceanicAtmosphericAcronyms.html") ||
                   endOfRequestUrl.equals("oceanicAtmosphericAcronyms.txt")) {
            doConvertOceanicAtmosphericAcronyms(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else if (endOfRequestUrl.startsWith("oceanicAtmosphericAcronyms.") && pft >= 0) {
            try {
                sendPlainTable(loggedInAs, request, response, 
                    EDStatic.oceanicAtmosphericAcronymsTable(), "OceanicAtmosphericAcronyms", fileTypeName);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                throw new SimpleException("The oceanic/atmospheric acronyms service is not available on this ERDDAP.", t);
            }
            return;

        //OceanicAtmospheric VariableNames
        } else if (endOfRequestUrl.equals("oceanicAtmosphericVariableNames.html") ||
                   endOfRequestUrl.equals("oceanicAtmosphericVariableNames.txt")) {
            doConvertOceanicAtmosphericVariableNames(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else if (endOfRequestUrl.startsWith("oceanicAtmosphericVariableNames.") && pft >= 0) {
            try {
                sendPlainTable(loggedInAs, request, response, 
                    EDStatic.oceanicAtmosphericVariableNamesTable(), "OceanicAtmosphericVariableNames", fileTypeName);
            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
                throw new SimpleException("The oceanic/atmospheric variable names service is not available on this ERDDAP.", t);
            }
            return;

        } else if (endOfRequestUrl.equals("keywords.html") ||
                   endOfRequestUrl.equals("keywords.txt")) {
            doConvertKeywords(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else if (endOfRequestUrl.startsWith("keywordsCf.") && pft >= 0) {
            sendPlainTable(loggedInAs, request, response, 
                EDStatic.keywordsCfTable(), "keywordsCf", fileTypeName);
            return;
        } else if (endOfRequestUrl.startsWith("keywordsCfToGcmd.") && pft >= 0) {
            sendPlainTable(loggedInAs, request, response, 
                EDStatic.keywordsCfToGcmdTable(), "keywordsCfToGcmd", fileTypeName);
            return;
        } else if (endOfRequestUrl.startsWith("keywordsGcmd.") && pft >= 0) {
            sendPlainTable(loggedInAs, request, response, 
                EDStatic.keywordsGcmdTable(), "keywordsGcmd", fileTypeName);
            return;
        } else if (endOfRequestUrl.equals("time.html") ||
                   endOfRequestUrl.equals("time.txt")) {
            doConvertTime(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else if (endOfRequestUrl.equals("units.html") ||
                   endOfRequestUrl.equals("units.txt")) {
            doConvertUnits(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else if (endOfRequestUrl.equals("urls.html") ||
                   endOfRequestUrl.equals("urls.txt")) {
            doConvertURLs(request, response, loggedInAs, endOfRequestUrl, userQuery);
            return;
        } else {
            if (verbose) String2.log(EDStatic.resourceNotFound + " end of convert");
            sendResourceNotFoundError(request, response, "");
            return;
        }

        //display start of web page
        if (reallyVerbose) String2.log("doConvert");
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">" +
                EDStatic.youAreHere(loggedInAs, "convert") +
                EDStatic.convertHtml + "\n" +
                //"<p>Options:\n" +
                "<ul>\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/oceanicAtmosphericAcronyms.html\"><strong>Acronyms</strong></a> - " + 
                    EDStatic.convertOceanicAtmosphericAcronyms + "\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/fipscounty.html\"><strong>FIPS County Codes</strong></a> - " + 
                    EDStatic.convertFipsCounty + "\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/keywords.html\"><strong>Keywords</strong></a> - " + 
                    EDStatic.convertKeywords + "\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/time.html\"><strong>Time</strong></a> - " + 
                    EDStatic.convertTime + "\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/units.html\"><strong>Units</strong></a> - " + 
                    EDStatic.convertUnits + "\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/urls.html\"><strong>URLs</strong></a> - " + 
                    EDStatic.convertURLs + "\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + "/convert/oceanicAtmosphericVariableNames.html\"><strong>Variable Names</strong></a> - " + 
                    EDStatic.convertOceanicAtmosphericVariableNames + "\n" +
                "</ul>\n");
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/convert/fipscounty.html and fipscounty.txt.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   fipscounty.html or fipscounty.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertFipsCounty(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        String defaultCode   = "06053";
        String defaultCounty = "CA, Monterey";
        String queryCode     = queryMap.get("code"); 
        String queryCounty   = queryMap.get("county");
        if (queryCode   == null) queryCode = "";
        if (queryCounty == null) queryCounty = "";
        String answerCode    = "";
        String answerCounty  = "";
        String codeTooltip   = "The 5-digit FIPS county code, for example, \"" + defaultCode + "\".";
        //String countyTooltip = "The county name, for example, \"" + defaultCounty + "\".";
        String countyTooltip = "Select a county name.";

        //only 0 or 1 of toCode,toCounty will be true (not both)
        boolean toCounty = queryCode.length() > 0; 
        boolean toCode = !toCounty && queryCounty.length() > 0;

        //a query either succeeds (and sets all answer...) 
        //  or fails (doesn't change answer... and sets tError)

        //process queryCounty
        String tError = null;
        Table fipsTable = null;
        try {
            fipsTable = EDStatic.fipsCountyTable();
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log(MustBe.throwableToString(t));
            throw new SimpleException("The FIPS county service is not available on this ERDDAP.", t);
        }
        if (toCode) {
            //process code=,   a toCode query
            int po = ((StringArray)(fipsTable.getColumn(1))).indexOf(queryCounty);
            if (po < 0) {
                tError = "county=\"" + queryCounty + 
                    "\" isn't an exact match of a FIPS county name.";
            } else {
                //success
                answerCounty = queryCounty;
                answerCode   = fipsTable.getColumn(0).getString(po);
            }

        } else if (toCounty) {        
            //process county=,   a toCounty query            
            int po = ((StringArray)(fipsTable.getColumn(0))).indexOf(queryCode);
            if (po < 0) {
                tError = "code=\"" + queryCode + 
                    "\" isn't an exact match of a 5-digit, FIPS county code.";
            } else {
                //success
                answerCode   = queryCode;
                answerCounty = fipsTable.getColumn(1).getString(po);
            }

        } else {
            //no query. use the default values...
        }

        //do the .txt response
        if (endOfRequestUrl.equals("fipscounty.txt")) {

            //throw exception?
            if (tError == null && !toCode && !toCounty)
                tError = "You must specify a code= or county= parameter (for example \"?code=" + 
                defaultCode + "\") at the end of the URL.";
            if (tError != null) 
                throw new SimpleException(tError);

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertFipsCounty", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                if (toCode) 
                    writer.write(answerCode);
                else if (toCounty) 
                    writer.write(answerCounty);            
                
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            } finally {
                writer.close(); 
            }
            return;
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert FIPS County", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">" +
                EDStatic.youAreHere(loggedInAs, "convert", "FIPS County") +
                "<h2>" + EDStatic.convertFipsCounty + "</h2>\n" +
                EDStatic.convertFipsCountyIntro + "\n");

     
            //Convert from Code to County
            writer.write(
                HtmlWidgets.ifJavaScriptDisabled + "\n" +
                widgets.beginForm("getCounty", "GET", tErddapUrl + "/convert/fipscounty.html", "") +
                "<strong>Convert from</strong>\n" + 
                widgets.textField("code", codeTooltip, 6, 10, 
                    answerCode.length() > 0? answerCode :
                    queryCode.length()  > 0? queryCode  : defaultCode, 
                    "") + 
                "\n<strong> to a county name. </strong>&nbsp;&nbsp;" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "<strong>Convert</strong>",
                    "") + 
                "\n");

            if (toCounty) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerCode) + " = " + 
                        XML.encodeAsHTML(answerCounty) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //Convert from County to Code
            String selectedCounty = 
                answerCounty.length() > 0? answerCounty :
                queryCounty.length()  > 0? queryCounty  : defaultCounty;
            String options[] = fipsTable.getColumn(1).toStringArray();
            writer.write(
                "<br>" + 
                widgets.beginForm("getCode", "GET", tErddapUrl + "/convert/fipscounty.html", "") +
                "<strong>Convert from</strong>\n" + 
                //widgets.textField("county", countyTooltip, 35, 50, selectedCounty, "") + 
                widgets.select("county", countyTooltip, 1, options,
                    String2.indexOf(options, selectedCounty), 
                    "onchange=\"this.form.submit();\"") +
                "\n<strong> to a 5-digit FIPS code. </strong>&nbsp;&nbsp;" +
                //widgets.button("submit", null, "", 
                //    "Convert",
                //    "") + 
                "\n");

            if (toCode) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerCounty) + " = " + 
                        XML.encodeAsHTML(answerCode) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //reset the form
            writer.write(
                "<p><a rel=\"bookmark\" href=\"" + tErddapUrl + 
                    "/convert/fipscounty.html\">" + EDStatic.resetTheForm + "</a>\n" +
                "<p>Or, <a rel=\"help\" href=\"#computerProgram\">bypass this web page</a>\n" +
                "  and do FIPS county conversions from within a computer program, script, or web page.\n");

            //get the entire list
            writer.write(
                "<p>Or, view/download the entire FIPS county list in these file types:" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/convert/fipscounty", ""));

            //notes  
            writer.write(EDStatic.convertFipsCountyNotes);

            //Info about .txt fips service option   
            writer.write(EDStatic.convertFipsCountyService);

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/convert/oceanicAtmosphericAcronyms.html and oceanicAtmosphericAcronyms.txt.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   oceanicAtmosphericAcronyms.html or oceanicAtmosphericAcronyms.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertOceanicAtmosphericAcronyms(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        String defaultAcronym  = "NOAA";
        String defaultFullName = "National Oceanic and Atmospheric Administration";
        String queryAcronym    = queryMap.get("acronym"); 
        String queryFullName   = queryMap.get("fullName");
        if (queryAcronym   == null) queryAcronym = "";
        if (queryFullName == null) queryFullName = "";
        String answerAcronym    = "";
        String answerFullName  = "";
        String acronymTooltip   = "The acronym, for example, \"" + defaultAcronym + "\".";
        //String fullNameTooltip = "The full name, for example, \"" + defaultFullName + "\".";
        String fullNameTooltip = "Select a full name.";

        //only 0 or 1 of toAcronym,toFullName will be true (not both)
        boolean toFullName = queryAcronym.length() > 0; 
        boolean toAcronym = !toFullName && queryFullName.length() > 0;

        //a query either succeeds (and sets all answer...) 
        //  or fails (doesn't change answer... and sets tError)

        //process queryFullName
        String tError = null;
        Table oaTable = null;
        try {
            oaTable = EDStatic.oceanicAtmosphericAcronymsTable();
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log(MustBe.throwableToString(t));
            throw new SimpleException("The oceanic/atmospheric acronym service is not available on this ERDDAP.", t);
        }
        StringArray acronymSA  = (StringArray)(oaTable.getColumn(0));
        StringArray fullNameSA = (StringArray)(oaTable.getColumn(1));
        if (toAcronym) {
            //process acronym=,   a toAcronym query
            int po = fullNameSA.indexOf(queryFullName);
            if (po < 0) {
                tError = "fullName=\"" + queryFullName + 
                    "\" isn't an exact match of an oceanic/atmospheric acronym.";
            } else {
                //success
                answerFullName = queryFullName;
                answerAcronym  = acronymSA.get(po);
            }

        } else if (toFullName) {        
            //process fullName=,   a toFullName query            
            int po = acronymSA.indexOf(queryAcronym);
            if (po < 0) {
                tError = "acronym=\"" + queryAcronym + 
                    "\" isn't an exact match of an oceanic/atmospheric full name.";
            } else {
                //success
                answerAcronym   = queryAcronym;
                answerFullName = fullNameSA.get(po);
            }

        } else {
            //no query. use the default values...
        }

        //do the .txt response
        if (endOfRequestUrl.equals("oceanicAtmosphericAcronyms.txt")) {

            //throw exception?
            if (tError == null && !toAcronym && !toFullName)
                tError = "You must specify a acronym= or fullName= parameter (for example \"?acronym=" + 
                defaultAcronym + "\") at the end of the URL.";
            if (tError != null) 
                throw new SimpleException(tError);

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertOceanicAtmosphericAcronym", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {

                if (toAcronym) 
                    writer.write(answerAcronym);
                else if (toFullName) 
                    writer.write(answerFullName);            
                
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                return;
            } finally {
                writer.close(); 
            }
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert Oceanic/Atmospheric Acronyms", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">" +
                EDStatic.youAreHere(loggedInAs, "convert", "Oceanic/Atmospheric Acronyms") +
                "<h2>" + EDStatic.convertOceanicAtmosphericAcronyms + "</h2>\n" +
                EDStatic.convertOceanicAtmosphericAcronymsIntro + "\n");

     
            //Convert from Acronym to FullName
            writer.write(
                HtmlWidgets.ifJavaScriptDisabled + "\n" +
                widgets.beginForm("getFullName", "GET", tErddapUrl + "/convert/oceanicAtmosphericAcronyms.html", "") +
                "<strong>Convert from</strong>\n" + 
                widgets.textField("acronym", acronymTooltip, 20, 40, 
                    answerAcronym.length() > 0? answerAcronym :
                    queryAcronym.length()  > 0? queryAcronym  : defaultAcronym, 
                    "") + 
                "\n<strong> to a full name. </strong>&nbsp;&nbsp;" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "<strong>Convert</strong>",
                    "") + 
                "\n");

            if (toFullName) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerAcronym) + " = " + 
                        XML.encodeAsHTML(answerFullName) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //Convert from FullName to Acronym
            String selectedFullName = 
                answerFullName.length() > 0? answerFullName :
                queryFullName.length()  > 0? queryFullName  : defaultFullName;
            String options[] = fullNameSA.toStringArray();
            Arrays.sort(options, new StringComparatorIgnoreCase());
            writer.write(
                "<br>" +
                widgets.beginForm("getAcronym", "GET", tErddapUrl + "/convert/oceanicAtmosphericAcronyms.html", "") +
                "<strong>Convert from</strong>\n" + 
                //widgets.textField("fullName", fullNameTooltip, 35, 70, selectedFullName, "") + 
                "<br>" +
                widgets.select("fullName", fullNameTooltip, 1, options,
                    String2.indexOf(options, selectedFullName), 
                    "onchange=\"this.form.submit();\"") +
                "\n" +
                "<br><strong>to an acronym.</strong>" +
                //widgets.button("submit", null, "", 
                //    "Convert",
                //    "") + 
                "\n");

            if (toAcronym) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerFullName) + " = " + 
                        XML.encodeAsHTML(answerAcronym) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //reset the form
            writer.write(
                "<p><a rel=\"bookmark\" href=\"" + tErddapUrl + 
                    "/convert/oceanicAtmosphericAcronyms.html\">" + EDStatic.resetTheForm + "</a>\n" +
                "<p>Or, <a rel=\"help\" href=\"#computerProgram\">bypass this web page</a>\n" +
                "  and do oceanic/atmospheric acronym conversions from within a computer program, script, or web page.\n");

            //get the entire list
            writer.write(
                "<p>Or, view/download the entire oceanic/atmospheric acronyms list in these file types:" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/convert/oceanicAtmosphericAcronyms", ""));

            //notes  
            writer.write(EDStatic.convertOceanicAtmosphericAcronymsNotes);

            //Info about .txt fips service option  
            writer.write(EDStatic.convertOceanicAtmosphericAcronymsService);

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/convert/oceanicAtmosphericVariableNames.html and oceanicAtmosphericVariableNames.txt.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   oceanicAtmosphericVariableNames.html or oceanicAtmosphericVariableNames.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertOceanicAtmosphericVariableNames(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        String defaultVariableName   = "sst";
        String defaultFullName = "Sea Surface Temperature";
        String queryVariableName     = queryMap.get("variableName"); 
        String queryFullName   = queryMap.get("fullName");
        if (queryVariableName   == null) queryVariableName = "";
        if (queryFullName == null) queryFullName = "";
        String answerVariableName    = "";
        String answerFullName  = "";
        String variableNameTooltip   = "The oceanic/atmospheric variable name, for example, \"" + defaultVariableName + "\".";
        //String fullNameTooltip = "The full name, for example, \"" + defaultFullName + "\".";
        String fullNameTooltip = "Select a full name.";

        //only 0 or 1 of toVariableName,toFullName will be true (not both)
        boolean toFullName = queryVariableName.length() > 0; 
        boolean toVariableName = !toFullName && queryFullName.length() > 0;

        //a query either succeeds (and sets all answer...) 
        //  or fails (doesn't change answer... and sets tError)

        //process queryFullName
        String tError = null;
        Table oaTable = null;
        try {
            oaTable = EDStatic.oceanicAtmosphericVariableNamesTable();
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            String2.log(MustBe.throwableToString(t));
            throw new SimpleException("The oceanic/atmospheric variable name service is not available on this ERDDAP.", t);
        }
        StringArray variableNameSA  = (StringArray)(oaTable.getColumn(0));
        StringArray fullNameSA      = (StringArray)(oaTable.getColumn(1));
        if (toVariableName) {
            //process variableName=,   a toVariableName query
            int po = fullNameSA.indexOf(queryFullName);
            if (po < 0) {
                tError = "fullName=\"" + queryFullName + 
                    "\" isn't an exact match of an oceanic/atmospheric full name.";
            } else {
                //success
                answerFullName = queryFullName;
                answerVariableName = variableNameSA.get(po);
            }

        } else if (toFullName) {        
            //process fullName=,   a toFullName query            
            int po = variableNameSA.indexOf(queryVariableName);
            if (po < 0) {
                tError = "variableName=\"" + queryVariableName + 
                    "\" isn't an exact match of an oceanic/atmospheric variable name.";
            } else {
                //success
                answerVariableName   = queryVariableName;
                answerFullName = fullNameSA.get(po);
            }

        } else {
            //no query. use the default values...
        }

        //do the .txt response
        if (endOfRequestUrl.equals("oceanicAtmosphericVariableNames.txt")) {

            //throw exception?
            if (tError == null && !toVariableName && !toFullName)
                tError = "You must specify a variableName= or fullName= parameter (for example \"?variableName=" + 
                defaultVariableName + "\") at the end of the URL.";
            if (tError != null) 
                throw new SimpleException(tError);

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertOceanicAtmosphericVariableName", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                if (toVariableName) 
                    writer.write(answerVariableName);
                else if (toFullName) 
                    writer.write(answerFullName);            
                
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                return;
            } finally {
                writer.close(); 
            }
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert Oceanic/Atmospheric Variable Names", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">" +
                EDStatic.youAreHere(loggedInAs, "convert", "Oceanic/Atmospheric Variable Names") +
                "<h2>" + EDStatic.convertOceanicAtmosphericVariableNames + "</h2>\n" +
                EDStatic.convertOceanicAtmosphericVariableNamesIntro + "\n");

     
            //Convert from VariableName to FullName
            writer.write(
                HtmlWidgets.ifJavaScriptDisabled + "\n" +
                widgets.beginForm("getFullName", "GET", tErddapUrl + "/convert/oceanicAtmosphericVariableNames.html", "") +
                "<strong>Convert from</strong>\n" + 
                widgets.textField("variableName", variableNameTooltip, 20, 40, 
                    answerVariableName.length() > 0? answerVariableName :
                    queryVariableName.length()  > 0? queryVariableName  : defaultVariableName, 
                    "") + 
                "\n<strong> to a fullName name. </strong>&nbsp;&nbsp;" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "<strong>Convert</strong>",
                    "") + 
                "\n");

            if (toFullName) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerVariableName) + " = " + 
                        XML.encodeAsHTML(answerFullName) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //Convert from FullName to VariableName
            String selectedFullName = 
                answerFullName.length() > 0? answerFullName :
                queryFullName.length()  > 0? queryFullName  : defaultFullName;
            String options[] = fullNameSA.toStringArray();
            Arrays.sort(options, new StringComparatorIgnoreCase());
            writer.write(
                "<br>&nbsp;\n" +  //necessary for the blank line before start of form (not <p>)
                widgets.beginForm("getVariableName", "GET", tErddapUrl + "/convert/oceanicAtmosphericVariableNames.html", "") +
                "<strong>Convert from</strong>\n" + 
                //widgets.textField("fullName", fullNameTooltip, 35, 70, selectedFullName, "") + 
                widgets.select("fullName", fullNameTooltip, 1, options,
                    String2.indexOf(options, selectedFullName), 
                    "onchange=\"this.form.submit();\"") +
                "\n<strong> to a variable name. </strong>&nbsp;&nbsp;" +
                //widgets.button("submit", null, "", 
                //    "Convert",
                //    "") + 
                "\n");

            if (toVariableName) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerFullName) + " = " + 
                        XML.encodeAsHTML(answerVariableName) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //reset the form
            writer.write(
                "<p><a rel=\"bookmark\" href=\"" + tErddapUrl + 
                    "/convert/oceanicAtmosphericVariableNames.html\">" + EDStatic.resetTheForm + "</a>\n" +
                "<p>Or, <a rel=\"help\" href=\"#computerProgram\">bypass this web page</a>\n" +
                "  and do oceanic/atmospheric variable name conversions from within a computer program, script, or web page.\n");

            //get the entire list
            writer.write(
                "<p>Or, view/download the entire oceanic/atmospheric variable names list in these file types:" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/convert/oceanicAtmosphericVariableNames", ""));

            //notes  
            writer.write(EDStatic.convertOceanicAtmosphericVariableNamesNotes);

            //Info about .txt service option   
            writer.write(EDStatic.convertOceanicAtmosphericVariableNamesService);

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }


    
    /**
     * Process erddap/convert/keywords.html [and ???.txt].
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   keywords.html or keywords.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertKeywords(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        String defaultCF   = "";
        String defaultGCMD = "";
        String queryCF     = queryMap.get("cf"); 
        String queryGCMD   = queryMap.get("gcmd");
        if (queryCF   == null) queryCF = "";
        if (queryGCMD == null) queryGCMD = "";
        String answerCF    = "";
        String answerGCMD  = "";

        //only 0 or 1 of toCF,toGCMD will be true (not both)
        boolean toGCMD = queryCF.length() > 0; 
        boolean toCF = !toGCMD && queryGCMD.length() > 0;

        //a query either succeeds (and sets all answer...) 
        //  or fails (doesn't change answer... and sets tError)

        //process queryGCMD
        String tError = null;
        if (toCF) {
            //process cf=,   a toCF query
            String ansar[] = CfToFromGcmd.gcmdToCf(queryGCMD);
            if (ansar.length == 0) {
                tError = "gcmd=\"" + queryGCMD + 
                    "\" has no corresponding CF Standard Names.";
            } else {
                //success
                answerGCMD = queryGCMD;
                answerCF   = String2.toNewlineString(ansar);
            }

        } else if (toGCMD) {        
            //process gcmd=,   a toGCMD query            
            String ansar[] = CfToFromGcmd.cfToGcmd(queryCF);
            if (ansar.length == 0) {
                tError = "cf=\"" + queryCF + 
                    "\" has no corresponding GCMD Science Keywords.";
            } else {
                //success
                answerCF   = queryCF;
                answerGCMD = String2.toNewlineString(ansar);
            }

        } else {
            //no query. use the default values...
        }

        //do the .txt response
        if (endOfRequestUrl.equals("keywords.txt")) {

            //throw exception?
            if (tError == null && !toCF && !toGCMD)
                tError = "You must specify a cf= or gcmd= parameter (for example \"?cf=" + 
                defaultCF + "\") at the end of the URL.";
            if (tError != null) 
                throw new SimpleException(tError);

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertKeywords", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                if (toCF) 
                    writer.write(answerCF);
                else if (toGCMD) 
                    writer.write(answerGCMD);            
                
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                return;
            } finally {
                writer.close(); 
            }
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert Keywords", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">" +
                EDStatic.youAreHere(loggedInAs, "convert", "keywords") +
                "<h2>" + EDStatic.convertKeywords + "</h2>\n" +
                EDStatic.convertKeywordsIntro + "\n");
     
            //Convert from CF to GCMD
            String selectedCF = queryCF.length() > 0? queryCF : defaultCF;
            writer.write(
                HtmlWidgets.ifJavaScriptDisabled + "\n" +
                widgets.beginForm("getGCMD", "GET", tErddapUrl + "/convert/keywords.html", "") +
                "<strong>Convert the CF Standard Name</strong>\n" + 
                "<br>" +
                widgets.select("cf", EDStatic.convertKeywordsCfTooltip, 1, 
                    CfToFromGcmd.cfNames,
                    String2.indexOf(CfToFromGcmd.cfNames, selectedCF), 
                    "onchange=\"this.form.submit();\"") +
                "\n<br><strong>into GCMD Science Keywords.</strong>" +
                //widgets.button("submit", null, "", "Convert", "") + 
                "\n");

            if (toGCMD) {
                String tAnswerGCMD = 
                              String2.replaceAll(answerGCMD, ">", "&gt;");
                tAnswerGCMD = String2.replaceAll(tAnswerGCMD, "\n", "\n<br>");
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        answerCF + " =<br>" + 
                        tAnswerGCMD + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //Convert from GCMD to CF
            String selectedGCMD = queryGCMD.length() > 0? queryGCMD : defaultGCMD;
            writer.write(
                "<br>" + 
                widgets.beginForm("getCF", "GET", tErddapUrl + "/convert/keywords.html", "") +
                "<strong>Convert the GCMD Science Keyword</strong>\n" + 
                "<br>" +
                widgets.select("gcmd", EDStatic.convertKeywordsGcmdTooltip, 1,
                    CfToFromGcmd.gcmdKeywords,
                    String2.indexOf(CfToFromGcmd.gcmdKeywords, selectedGCMD), 
                    "onchange=\"this.form.submit();\"") +
                "\n<br><strong>into CF Standard Names.</strong>" +
                //widgets.button("submit", null, "", "Convert", "") + 
                "\n");

            if (toCF) {
                String tAnswerCF = String2.replaceAll(answerCF, "\n", "\n<br>");
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        answerGCMD + " =<br>" + 
                        tAnswerCF + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write(widgets.endForm() + "\n");

            //reset the form
            writer.write(
                "<p><strong>Other Options</strong>\n" +
                "<ul>\n" +
                "<li><a rel=\"bookmark\" href=\"" + tErddapUrl + 
                    "/convert/keywords.html\">" + EDStatic.resetTheForm + "</a>\n" +
                "  <br>&nbsp;\n" +

                "<li><a rel=\"help\" href=\"#computerProgram\">Bypass this web page</a>\n" +
                "  and do keyword conversions from within a computer program, script, or web page.\n" +
                "  <br>&nbsp;\n" +

                //get the entire CF or GCMD list
                "<li>View/download a file which has all of the CF to GCMD conversion information:" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/convert/keywordsCfToGcmd", "") +
                "  <br>The GCMD to CF conversion information can be derived from this." +
                "  <br>&nbsp;\n" +

                "<li>View/download the entire CF Standard Names list in these file types:" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/convert/keywordsCf", "") +
                "  <br>Source: <a rel=\"bookmark\" href=\"http://cfconventions.org/Data/cf-standard-names/18/build/cf-standard-name-table.html\">Version 18, dated 22 July 2011" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "  <br>&nbsp;\n" +

                "<li>View/download the entire GCMD Science Keywords list in these file types:" +
                "  <br>" + plainLinkExamples(tErddapUrl, "/convert/keywordsGcmd", "") +
                "  <br>Source: <a rel=\"bookmark\" href=\"https://wiki.earthdata.nasa.gov/display/CMR/GCMD+Keyword+Access\">the version dated 2008-02-05" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>.\n" +
                "    The requested citation is<kbd>\n" +
                "  <br>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester,\n" +
                "  <br>H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau,\n" +
                "  <br>M. Holland, T. Northcutt, R. A. Restrepo, 2007 . NASA/Global Change\n" +
                "  <br>Master Directory (GCMD) Earth Science Keywords. Version 6.0.0.0.0 </kbd>\n" +

                "</ul>\n" +
                "\n");

            //notes
            writer.write(EDStatic.convertKeywordsNotes);

            //Info about .txt time service option   
            writer.write(EDStatic.convertKeywordsService);

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/convert/time.html and time.txt.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   time.html or time.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertTime(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        //defaultIsoTime, defaultN and defaultUnits are also used in messages.xml convertTimeService
        String defaultIsoTime   = "1985-01-02T00:00:00Z";
        String defaultOtherTime = "1/2/1985 00:00:00";
        String defaultN         = "473472000";
        String defaultUnits     = EDV.TIME_UNITS; //"seconds since 1970-01-01T00:00:00Z";
        String defaultOtherUnits= "Sec SINCE 1/1/1970";
        String queryStringTime  = queryMap.get("stringTime"); 
        String queryIsoTime     = queryMap.get("isoTime"); 
        String queryN           = queryMap.get("n");
        String queryUnits       = queryMap.get("units");
        if (queryIsoTime    == null) queryIsoTime    = "";
        if (queryStringTime == null) queryStringTime = "";
        if (queryN          == null) queryN          = "";
        if (queryUnits      == null) queryUnits      = "";
        String answerIsoTime = "";
        String answerFormat  = "";
        String answerN       = "";
        String answerUnits   = "";
        String numberTooltip = 
            "<div class=\"narrow_max_width\">" + 
            MessageFormat.format(EDStatic.convertTimeNumberTooltip, defaultN) +
            "</div>";
        String stringTimeTooltip = 
            "<div class=\"narrow_max_width\">" + 
            MessageFormat.format(EDStatic.convertTimeStringTimeTooltip, defaultIsoTime) +
            "</div>";
        String unitsTooltip = 
            "<div class=\"narrow_max_width\">" +
            EDStatic.convertTimeUnitsTooltip + 
            "</div>";


        //only 0 or 1 of these will be true
        boolean cleanString = queryStringTime.length() > 0 &&
                              queryIsoTime.length() == 0 &&
                              queryN.length() == 0 &&
                              queryUnits.length() == 0;

        boolean cleanUnits =  queryStringTime.length() == 0 &&
                              queryIsoTime.length() == 0 &&
                              queryN.length() == 0 &&
                              queryUnits.length() > 0;

        boolean toNumeric =   ((queryStringTime.length() > 0 &&
                                queryUnits.length() > 0) || //required to differentiate from cleanString
                               queryIsoTime.length() > 0) &&
                              queryN.length() == 0;

        boolean toString =    queryStringTime.length() == 0 &&
                              queryIsoTime.length() == 0 &&
                              queryN.length() > 0;
                              //queryUnits.length() > 0; not required
        
        if ((toNumeric || toString) && queryUnits.length() == 0)
            queryUnits = defaultUnits;

        String tError = null;
        if (queryStringTime.length() > 0 &&
               queryIsoTime.length() > 0)
            tError = EDStatic.convertTimeTwoTimeError;

        //a query either succeeds (and sets all answer...) 
        //  or fails (doesn't change answer... and sets tError)

        //process queryUnits
        double tbf[] = null;
        String unitsError = null;
        try {
            int sincePo = queryUnits.toLowerCase().indexOf(" since ");
            if (sincePo <= 0) {
                unitsError = EDStatic.convertTimeNoSinceError;
            } else {
                answerUnits = Calendar2.cleanUpNumericTimeUnits(queryUnits);
                tbf = Calendar2.getTimeBaseAndFactor(answerUnits);
            }
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            answerUnits = "";
            unitsError = EDStatic.convertTimeUnitsError; 
        }

        //do the calculation
        double epochSeconds; //will be valid if no error
        if (tError == null) {
            if (cleanString) {
                answerFormat  = Calendar2.tryToFindFormat(queryStringTime);
                answerIsoTime = Calendar2.tryToIsoString(queryStringTime);
                if (answerIsoTime.length() == 0)
                    tError = EDStatic.convertTimeStringFormatError;

            } else if (cleanUnits) {
                //answerUnits already set
                if (unitsError != null)
                    tError = unitsError;

            } else if (toNumeric) {
                if (unitsError != null) {
                    tError = unitsError;
                } else if (queryStringTime.length() > 0) {
                    //process stringTime=,   a toNumeric query
                    queryIsoTime = Calendar2.tryToIsoString(queryStringTime);
                    if (queryIsoTime.length() == 0)
                        tError = EDStatic.convertTimeStringFormatError;
                }

                if (tError == null) {
                    //process isoTime=,   a toNumeric query
                    epochSeconds = Calendar2.safeIsoStringToEpochSeconds(queryIsoTime); 
                    if (Double.isNaN(epochSeconds)) {
                        tError = EDStatic.convertTimeIsoFormatError;
                    } else {
                        //success
                        answerIsoTime = queryIsoTime;
                        double tN = Calendar2.epochSecondsToUnitsSince(tbf[0], tbf[1], epochSeconds);
                        answerN = tN == Math2.roundToLong(tN)? 
                            "" + Math2.roundToLong(tN) : //so no .0 at end
                            "" + tN;
                    }
                }

            } else if (toString) {        
                //process n=,   a toString query            
                double tN = String2.parseDouble(queryN);
                if (Double.isNaN(tN)) {
                    tError = EDStatic.convertTimeNumberError;
                } else if (unitsError != null) {
                    tError = unitsError;
                } else {
                    //success
                    epochSeconds = Calendar2.unitsSinceToEpochSeconds(tbf[0], tbf[1], tN);
                    answerIsoTime = Calendar2.safeEpochSecondsToIsoStringTZ(epochSeconds, "");
                    //String2.log(">> toString answerIsoTime=" + answerIsoTime + " epochSeconds=" + epochSeconds + " " +
                    //    tbf[0] + ", " + tbf[1] + ", " + tN);
                    if (answerIsoTime.length() == 0)
                        tError = EDStatic.convertTimeNumericTimeError; 
                    else answerN = tN == Math2.roundToLong(tN)? 
                        "" + Math2.roundToLong(tN) : //so no .0 at end
                        "" + tN;
                }

            } else {
                //no query. use the default values...
            }
        }

        //do the .txt response
        if (endOfRequestUrl.equals("time.txt")) {

            //throw exception?
            if (tError == null && !cleanString && !cleanUnits && !toNumeric && !toString)
                tError = EDStatic.convertTimeParametersError;
            if (tError != null) 
                throw new SimpleException(tError);

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertTime", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                if (cleanString)     writer.write(answerIsoTime);
                else if (cleanUnits) writer.write(answerUnits);
                else if (toNumeric)  writer.write(answerN); 
                else if (toString)   writer.write(answerIsoTime);            
                            
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                return;
            } finally {
                writer.close(); 
            }
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert Time", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">" +
                EDStatic.youAreHere(loggedInAs, "convert", "Time") +
                "<h2>" + EDStatic.convertTime + "</h2>\n" +
                EDStatic.convertTimeIntro + "\n");

     
            //Convert from a String Time to Numeric Time n units
            writer.write(
                "<br>" +
                widgets.beginForm("toNumeric", "GET", tErddapUrl + "/convert/time.html", "") +                
                "<strong>Convert from a string time</strong>\n" +
                widgets.textField("stringTime", 
                    stringTimeTooltip, 22, 40, 
                    queryStringTime.length() > 0? queryStringTime :
                    queryIsoTime.length()    > 0? queryIsoTime  : 
                    answerIsoTime.length()   > 0? answerIsoTime :
                    defaultIsoTime, 
                    "") + 
                "<br><strong>into a numeric time using units =</strong>\n" +
                widgets.textField("units", unitsTooltip, 37, 60, 
                    queryUnits.length()  > 0? queryUnits  :
                    answerUnits.length() > 0? answerUnits :
                    defaultUnits, 
                    "") + "." +
                "\n" +
                "<br>" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "Convert",
                    "") + 
                "\n");

            if (toNumeric) {
                writer.write(tError == null?
                    "<br><span class=\"N successColor\">" + 
                        XML.encodeAsHTML(queryStringTime + " = " +
                                         answerIsoTime + " = " + 
                                         answerN + " " + 
                                         answerUnits) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write("<br>" + widgets.endForm() + "\n");

            //Convert from n units to an ISO String Time
            writer.write(
                "<br>" +
                widgets.beginForm("toString", "GET", tErddapUrl + "/convert/time.html", "") +                                
                "<strong>Convert from a numeric time</strong>\n" +
                "<br>" + 
                widgets.textField("n", 
                    numberTooltip, 15, 30, 
                    queryN.length()  > 0? queryN  : 
                    answerN.length() > 0? answerN :
                    defaultN, 
                    "") + 
                "\n" + 
                widgets.textField("units", unitsTooltip,
                    37, 60, 
                    queryUnits.length()  > 0? queryUnits  :
                    answerUnits.length() > 0? answerUnits :
                    defaultUnits, 
                    "") + 
                "\n" +
                "<br><strong>into a string time.</strong>\n" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "Convert",
                    "") + 
                "\n");

            if (toString) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(answerN) + " " + 
                        XML.encodeAsHTML(answerUnits) + " = " + 
                        XML.encodeAsHTML(answerIsoTime) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");                
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write("<br>" + widgets.endForm() + "\n");

            //clean a String Time
            writer.write(
                "<br>" +
                widgets.beginForm("cleanString", "GET", tErddapUrl + "/convert/time.html", "") +
                "<strong>Convert any common string time</strong>\n" +
                widgets.textField("stringTime", 
                    stringTimeTooltip, 22, 40, 
                    queryStringTime.length() > 0? queryStringTime :
                    defaultOtherTime, 
                    "") + 
                "\n" +
                "<br><strong>into an ISO 8601 string time.</strong>\n" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "Convert",
                    "") + 
                "\n");

            if (cleanString) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" + 
                        XML.encodeAsHTML(queryStringTime + " @ " + answerFormat + " = " + answerIsoTime) + 
                        "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write("<br>" + widgets.endForm() + "\n");

            //clean units
            writer.write(
                "<br>" +
                widgets.beginForm("cleanUnits", "GET", tErddapUrl + "/convert/time.html", "") +
                "<strong>Convert a UDUNITS-like time units string</strong>\n" +
                widgets.textField("units", 
                    unitsTooltip, 37, 60, 
                    queryUnits.length()  > 0? queryUnits :
                    defaultOtherUnits, 
                    "") + 
                "\n" +
                "<br><strong>into a proper UDUNITS time units string.</strong>\n" +
                widgets.htmlButton("submit", null, "Convert", "", 
                    "Convert",
                    "") + 
                "\n");

            if (cleanUnits) {
                writer.write(tError == null?
                    "<br><span class=\"successColor\">" +                       
                        XML.encodeAsHTML(queryUnits + " = " + answerUnits) + "</span>\n" :
                    "<br><span class=\"warningColor\">" + XML.encodeAsHTML(tError) + "</span>\n");
            } else {
                writer.write("<br>&nbsp;\n");
            }

            writer.write("<br>" + widgets.endForm() + "\n");


            //reset the form
            writer.write(
                "<br><a rel=\"bookmark\" href=\"" + tErddapUrl + 
                    "/convert/time.html\">" + EDStatic.resetTheForm + "</a>\n" +
                "<p>" + EDStatic.convertTimeBypass + "\n");

            //notes  
            writer.write(EDStatic.convertTimeNotes);

            //Info about .txt time service option  
            writer.write(EDStatic.convertTimeService);

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }


    /**
     * Process erddap/convert/units.html and units.txt.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   units.html or units.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertUnits(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        String tStandardizeUdunits = queryMap.get("STANDARDIZE_UDUNITS"); 
        String tUdunits = queryMap.get("UDUNITS"); 
        String tUcum    = queryMap.get("UCUM");
        tStandardizeUdunits = tStandardizeUdunits == null? "" : tStandardizeUdunits.trim();
        tUdunits            = tUdunits            == null? "" : tUdunits.trim();
        tUcum               = tUcum               == null? "" : tUcum.trim();
        String rStandardizeUdunits = "";
        String rUcum = "";
        String rUdunits = "";
        if (tStandardizeUdunits.length() > 0) {
            tUdunits = "";
            tUcum = "";
            rStandardizeUdunits = EDUnits.safeStandardizeUdunits(tStandardizeUdunits);
            rUcum    = EDUnits.safeUdunitsToUcum(tStandardizeUdunits);
            rUdunits = rStandardizeUdunits;
        } else if (tUdunits.length() > 0) {
            tStandardizeUdunits = "";
            tUcum = "";
            rUcum    = EDUnits.safeUdunitsToUcum(tUdunits);
        } else if (tUcum.length() > 0) {
            tStandardizeUdunits = "";
            tUdunits = "";
            rUdunits = EDUnits.ucumToUdunits(tUcum);
        }

        //do the .txt response
        if (endOfRequestUrl.equals("units.txt")) {

            //throw exception?
            if (tStandardizeUdunits.length() == 0 && tUdunits.length() == 0 && tUcum.length() == 0) {
                throw new SimpleException(EDStatic.queryError + 
                    "Missing parameter (STANDARDIZE_UDUNITS, UDUNITS or UCUM).");
            }

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertUnits", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                writer.write(
                    tStandardizeUdunits.length() > 0? rStandardizeUdunits :
                    tUdunits.length()            > 0? rUcum : 
                                                      rUdunits);
                
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                return;
            } finally {
                writer.close(); 
            }
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert Units", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "convert", "units") +
                "<h2>" + EDStatic.convertUnits + "</h2>\n" +
                EDStatic.convertUnitsIntro);
     
            //convert udunits
            writer.write(
                "<br>" + //necessary for the blank line before start of form (not <p>)
                widgets.beginForm("getUcum", "GET", tErddapUrl + "/convert/units.html", "") +
                "<strong>Convert from UDUNITS to UCUM</strong>\n" +
                "<br>UDUNITS:\n" + 
                widgets.textField("UDUNITS", 
                    "For example, degC/meter",
                    40, 100, 
                    tUdunits.length()            > 0? tUdunits : 
                    rStandardizeUdunits.length() > 0? rStandardizeUdunits : 
                    rUdunits.length()            > 0? rUdunits :
                                                      "degC/meter", 
                    "") + 
                " " +
                widgets.htmlButton("submit", null, "Convert to UCUM", "", 
                    "<strong>Convert to UCUM</strong>",
                    "") + 
                "\n");

            if (tUdunits.length() == 0) 
                writer.write(
                    "<br>&nbsp;\n");
            else 
                writer.write(
                    "<br><span class=\"successColor\">UDUNITS \"" + XML.encodeAsHTML(tUdunits) + 
                        "\" &rarr; UCUM \"" + XML.encodeAsHTML(rUcum) + "\"</span>\n");

            writer.write(
                widgets.endForm() +
                "\n");

            //convert udunits
            writer.write(
                "<br>" +
                widgets.beginForm("getUdunits", "GET", tErddapUrl + "/convert/units.html", "") +
                "<strong>Convert from UCUM to UDUNITS</strong>\n" +
                "<br>UCUM:\n" + 
                widgets.textField("UCUM", 
                    "For example, Cel.m-1",
                    40, 100, 
                    tUcum.length() > 0? tUcum : 
                    rUcum.length() > 0? rUcum : 
                                        "Cel.m-1", 
                    "") + 
                " " +
                widgets.htmlButton("submit", null, "Convert to UDUNITS", "", 
                    "<strong>Convert to UDUNITS</strong>",
                    "") + 
                "\n");

            if (tUcum.length() == 0) 
                writer.write(
                    "<br>&nbsp;\n");
            else 
                writer.write(
                    "<br><span class=\"successColor\">UCUM \"" + XML.encodeAsHTML(tUcum) + 
                        "\" &rarr; UDUNITS \"" + XML.encodeAsHTML(rUdunits) + "\"</span>\n");            

            writer.write(widgets.endForm());
            writer.write('\n');

            //standardize udunits
            writer.write(
                "<br>" + //necessary for the blank line before start of form (not <p>)
                widgets.beginForm("standarizeUdunits", "GET", tErddapUrl + "/convert/units.html", "") +
                "<strong>Or, Standardize UDUNITS</strong>\n" +
                "<br>UDUNITS:\n" + 
                widgets.textField("STANDARDIZE_UDUNITS", 
                    "For example, deg_C/meter",
                    40, 100, 
                    tStandardizeUdunits.length() > 0? tStandardizeUdunits : 
                    rUdunits.length()            > 0? rUdunits :
                    tUdunits.length()            > 0? tUdunits : 
                                                      "degC/meter", 
                    "") + 
                " " +
                widgets.htmlButton("submit", null, "Standardize UDUNITS", "", 
                    "<strong>Standardize UDUNITS</strong>",
                    "") + 
                "\n");

            if (tStandardizeUdunits.length() == 0) 
                writer.write(
                    "<br>&nbsp;\n");
            else 
                writer.write(
                    "<br><span class=\"successColor\">UDUNITS \"" + XML.encodeAsHTML(tStandardizeUdunits) + 
                        "\" &rarr; UDUNITS \"" + XML.encodeAsHTML(rStandardizeUdunits) + "\"</span>\n");

            writer.write(
                widgets.endForm() +
                "\n");

            //notes 
            writer.write(
                "<br>On this ERDDAP, most/all datasets use " + EDStatic.units_standard + ".\n" +
                "<br>In tabledap requests, you can \n" +
                  "<a href=\"#unitsFilter\">request UDUNITS or UCUM</a>.\n");

            writer.write(EDStatic.convertUnitsNotes);
            writer.write('\n');

            //Info about service / .txt option   
            writer.write(EDStatic.convertUnitsService);
            writer.write('\n');


            //info about syntax differences 
            writer.write(EDStatic.convertUnitsComparison);
            writer.write('\n');

            //info about tabledap unitsFilter &units("UCUM") 
            writer.write(EDStatic.convertUnitsFilter);

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {

            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
    }

    /**
     * Process erddap/convert/urls.html and urls.txt.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param endOfRequestUrl   urls.html or urls.txt
     * @param userQuery  post "?", still percentEncoded, may be null.
     * @throws Throwable if trouble
     */
    public void doConvertURLs(HttpServletRequest request, HttpServletResponse response, 
        String loggedInAs, String endOfRequestUrl, String userQuery) throws Throwable {

        //first thing
        if (!EDStatic.convertersActive) {
            sendResourceNotFoundError(request, response, 
                MessageFormat.format(EDStatic.disabled, "convert"));
            return;
        }

        //parse the userQuery
        HashMap<String, String> queryMap = EDD.userQueryHashMap(userQuery, false); //true=lowercase keys
        String tText = queryMap.get("text"); 
        if (tText == null)
            tText = "";
        String rText = tText.length() == 0? "" : EDStatic.updateUrls(tText);
        String sample = "For more information, see ftp://eclipse.ncdc.noaa.gov/pub/OI-daily/daily-sst.pdf";

        //do the .txt response
        if (endOfRequestUrl.equals("urls.txt")) {

            //throw exception?
            if (rText.length() == 0) {
                throw new SimpleException(EDStatic.queryError + 
                    "Missing parameter (text).");
            }

            //respond to a valid request
            OutputStream out = (new OutputStreamFromHttpResponse(request, response, 
                "ConvertURLs", ".txt", ".txt")).outputStream(String2.UTF_8);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));
            try {
                writer.write(rText);
                
                writer.flush(); //essential
                if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
                return;
            } finally {
                writer.close(); 
            }
        }

        //do the .html response
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true; 
        OutputStream out = getHtmlOutputStream(request, response);
        Writer writer = getHtmlWriter(loggedInAs, "Convert URLs", out); 
        try {
            writer.write(
                "<div class=\"standard_width\">\n" +
                EDStatic.youAreHere(loggedInAs, "convert", "URLs") +
                "<h2>" + EDStatic.convertURLs + "</h2>\n" +
                EDStatic.convertURLsIntro);
     
            //convert
            writer.write(
                "<p>" +
                widgets.beginForm("convertURLs", "GET", tErddapUrl + "/convert/urls.html", "") +
                "<strong>" + EDStatic.convertURLs + "</strong>\n" +
                "<br>" + 
                "<textarea name=\"text\" cols=\"80\" rows=\"6\" maxlength=\"1000\" wrap=\"soft\">" +
                XML.encodeAsHTML(tText.length() > 0? tText : sample) +
                "</textarea>\n" +
                "<br>" + widgets.htmlButton("submit", null, "Convert", "", 
                    "<strong>Convert</strong>",
                    "") + 
                "\n");

            if (rText.length() == 0) 
                writer.write(
                    "<br>&nbsp;\n");
            else writer.write(
                "<br><span class=\"successColor\">" +
                XML.encodeAsHTML(rText) + "</span>\n");

            writer.write(
                widgets.endForm() +
                "\n");

            //notes 
            writer.write(EDStatic.convertURLsNotes);
            writer.write('\n');

            //Info about service / .txt option 
            writer.write(EDStatic.convertURLsService);
            writer.write('\n');

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}
            writer.write(EDStatic.htmlForException(t));
            throw t; 
        } finally {
            //end of document
            writer.write("</div>\n");
            endHtmlWriter(out, writer, tErddapUrl, false);
        }
  
    }


    /**
     * This indicates if the string 's' equals 'start' (e.g., "index") 
     * plus one of the plain file types.
     */
    protected static boolean endsWithPlainFileType(String s, String start) {
        for (int pft = 0; pft < plainFileTypes.length; pft++) { 
            if (s.equals(start + plainFileTypes[pft]))
                return true;
        }
        return false;
    }

    /**
     * Get a writer for a json file.
     *
     * @param request
     * @param response
     * @param fileName  without the extension, e.g., "error"
     * @param fileType ".json" (contentType=application/json) or ".jsonText" (contentType=text/plain)
     * @return a BufferedWriter
     * @throws Throwable if trouble
     */
    public static Writer getJsonWriter(HttpServletRequest request, HttpServletResponse response, 
        String fileName, String fileType) throws Throwable {

        OutputStreamSource outputStreamSource = 
            new OutputStreamFromHttpResponse(request, response, 
                fileName, fileType, ".json");
        return new BufferedWriter(new OutputStreamWriter(
            outputStreamSource.outputStream(String2.UTF_8), String2.UTF_8));
    }
    
    /**
     * Get an outputStream for an html file
     *
     * @param request
     * @param response
     * @return an outputStream
     * @throws Throwable if trouble
     */
    public static OutputStream getHtmlOutputStream(HttpServletRequest request, HttpServletResponse response) 
        throws Throwable {

        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, "index", ".html", ".html");
        return outSource.outputStream(String2.UTF_8);
    }

    /**
     * Get a writer for an html file and write up to and including the startHtmlBody
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param addToTitle   a string, not yet XML encoded
     * @param out
     * @return writer
     * @throws Throwable if trouble
     */
    Writer getHtmlWriter(String loggedInAs, String addToTitle, OutputStream out) throws Throwable {
        return getHtmlWriter(loggedInAs, addToTitle, "", out);
    }

    /**
     * Get a writer for an html file and write up to and including the startHtmlBody
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in)
     * @param addToTitle   a string, not yet XML encoded
     * @param addToHead  additional info for the &lt;head&gt;
     * @param out
     * @return writer
     * @throws Throwable if trouble
     */
    Writer getHtmlWriter(String loggedInAs, String addToTitle, String addToHead, 
        OutputStream out) throws Throwable {

        Writer writer = new BufferedWriter(new OutputStreamWriter(out, String2.UTF_8));

        //write the information for this protocol (dataset list table and instructions)
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        writer.write(EDStatic.startHeadHtml(tErddapUrl, addToTitle));
        if (String2.isSomething(addToHead))
            writer.write("\n" + addToHead);
        writer.write("\n</head>\n");
        writer.write(EDStatic.startBodyHtml(loggedInAs));
        writer.write("\n");
        writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)));
        writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
        return writer;
    }

    /**
     * Write the end of the standard html doc to writer.
     *
     * @param out
     * @param writer
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @param forceWriteDiagnostics
     * @throws Throwable if trouble
     */
    void endHtmlWriter(OutputStream out, Writer writer, String tErddapUrl,
        boolean forceWriteDiagnostics) throws Throwable {

        try {
            //end of document
            writer.write(EDStatic.endBodyHtml(tErddapUrl));
            writer.write("\n</html>\n");

            //essential
            writer.flush();
            if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
        } finally {
            writer.close();         
        }
    }


    /**
     * This writes the error (if not null or "") to the html writer.
     *
     * @param writer
     * @param request
     * @param error plain text, will be html-encoded here
     * @throws Throwable if trouble
     */
    void writeErrorHtml(Writer writer, HttpServletRequest request, String error) throws Throwable {
        if (error == null || error.length() == 0) 
            return;
        int colonPo = error.indexOf(": ");
        if (colonPo >= 0 && colonPo < error.length() - 5)
            error = error.substring(colonPo + 2);
        String query = SSR.percentDecode(request.getQueryString()); //percentDecode returns "" instead of null
        String requestUrl = request.getRequestURI();
        if (requestUrl == null) 
            requestUrl = "";
        if (requestUrl.startsWith("/"))
            requestUrl = requestUrl.substring(1);
        //encodeAsPreHTML(error) is essential -- to prevent Cross-site-scripting security vulnerability
        //(which allows hacker to insert his javascript into pages returned by server)
        //See Tomcat (Definitive Guide) pg 147
        error = XML.encodeAsPreHTML(error, 100);
        int brPo = error.indexOf("<br> at ");
        if (brPo < 0) 
            brPo = error.indexOf("<br>at ");
        if (brPo < 0) 
            brPo = error.length();
        writer.write(
            "<span class=\"warningColor\">" + 
            EDStatic.errorTitle + ": " + error.substring(0, brPo) + "</span>" + 
                error.substring(brPo) + 
            "<br>&nbsp;");
    }


    /**
     * This sends the HTTP resource NOT_FOUND error.
     * This always also sends the error to String2.log.
     *
     * @param message  use "" if nothing specific.
     */
    public static void sendResourceNotFoundError(HttpServletRequest request, 
        HttpServletResponse response, String message) {

        EDStatic.lowSendError(response, HttpServletResponse.SC_NOT_FOUND, message);
    }


    /** 
     * This gets the html for the search form.
     *
     * @param request
     * @param loggedInAs
     * @param pretext e.g., &lt;h2&gt;   Or use "" for none.
     * @param posttext e.g., &lt;/h2&gt;   Or use "" for none.
     * @param searchFor the default text to be searched for
     * @throws Throwable if trouble
     */
    public static String getSearchFormHtml(HttpServletRequest request, String loggedInAs,  
        String pretext, String posttext, String searchFor) throws Throwable {
     
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        widgets.enterTextSubmitsForm = true;
        StringBuilder sb = new StringBuilder();
        sb.append(widgets.beginForm("search", "GET", tErddapUrl + "/search/index.html", ""));
        sb.append(pretext + EDStatic.searchDoFullTextHtml + posttext);
        int pipp[] = EDStatic.getRequestedPIpp(request);
        sb.append(widgets.hidden("page", "1")); //new search always resets to page 1
        sb.append(widgets.hidden("itemsPerPage", "" + pipp[1]));
        if (searchFor == null)
            searchFor = "";
        widgets.htmlTooltips = false;
        sb.append(widgets.textField("searchFor", EDStatic.searchTip, 40, 255, searchFor, ""));
        widgets.htmlTooltips = true;
        sb.append(EDStatic.htmlTooltipImage(loggedInAs, EDStatic.searchHintsTooltip));
        widgets.htmlTooltips = false;
        sb.append(widgets.htmlButton("submit", null, null, EDStatic.searchClickTip, 
            EDStatic.searchButton, ""));
        widgets.htmlTooltips = true;
        sb.append("\n");
        sb.append(widgets.endForm());        
        return sb.toString();
    }


    /** 
     * This returns a table with categorize options.
     *
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @param fileTypeName .html or a plainFileType e.g., .htmlTable
     * @return a table with categorize options.
     * @throws Throwable if trouble
     */
    public Table categorizeOptionsTable(HttpServletRequest request, 
        String tErddapUrl, String fileTypeName) throws Throwable {

        Table table = new Table();
        StringArray csa = new StringArray();
        table.addColumn("Categorize", csa);
        if (fileTypeName.equals(".html")) {
            //1 column: links
            for (int cat = 0; cat < EDStatic.categoryAttributesInURLs.length; cat++) {
                csa.add("<a href=\"" + tErddapUrl + "/categorize/" + 
                    EDStatic.categoryAttributesInURLs[cat] + "/index.html?" +
                    EDStatic.encodedPassThroughPIppQueryPage1(request) + "\">" + 
                    EDStatic.categoryAttributesInURLs[cat] + "</a>");
            }
        } else {
            //2 columns: categorize, url
            StringArray usa = new StringArray();
            table.addColumn("URL", usa);
            for (int cat = 0; cat < EDStatic.categoryAttributesInURLs.length; cat++) {
                csa.add(EDStatic.categoryAttributesInURLs[cat]);
                usa.add(tErddapUrl + "/categorize/" + EDStatic.categoryAttributesInURLs[cat] + 
                    "/index" + fileTypeName + "?" +
                    EDStatic.passThroughPIppQueryPage1(request)); 
            }
        }
        return table;
    }

    /**
     * This writes a simple categorize options list (with &lt;br&gt;, for use on right-hand
     * side of getYouAreHereTable).
     *
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @return the html with the category links
     */
    public String getCategoryLinksHtml(HttpServletRequest request, String tErddapUrl) 
        throws Throwable {
        
        Table catTable = categorizeOptionsTable(request, tErddapUrl, ".html");
        int cn = catTable.nRows();
        StringBuilder sb = new StringBuilder(EDStatic.orComma + EDStatic.categoryTitleHtml + ":");
        int charCount = 0;
        for (int row = 0; row < cn; row++) {
            if (row % 4 == 0)
                sb.append("\n<br>");
            sb.append(catTable.getStringData(0, row) + 
                (row < cn - 1? ", \n" : "\n"));
        }
        return sb.toString();
    }

    /**
     * This writes the categorize options table
     *
     * @param request
     * @param loggedInAs
     * @param writer
     * @param attributeInURL e.g., institution   (it may be null or invalid)
     * @param homePage
     */
    public void writeCategorizeOptionsHtml1(HttpServletRequest request, 
        String loggedInAs, Writer writer, 
        String attributeInURL, boolean homePage) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        if (homePage) {
            Table table = categorizeOptionsTable(request, tErddapUrl, ".html");
            int n = table.nRows();
            writer.write(
                "<h3>" + EDStatic.categoryTitleHtml + "</h3>\n" +
                String2.replaceAll(EDStatic.category1Html, "<br>", "") + 
                "\n(");
            for (int row = 0; row < n; row++) 
                writer.write(table.getStringData(0, row) + (row < n - 1? ", \n" : ""));
            writer.write(") " + EDStatic.category2Html + "\n" +
                EDStatic.category3Html + "\n");
            return;
        }

        //categorize page
        writer.write(
            //"<h3>" + EDStatic.categoryTitleHtml + "</h3>\n" +
            "<h3>1) " + EDStatic.categoryPickAttribute + "&nbsp;" + 
            EDStatic.htmlTooltipImage(loggedInAs, 
                EDStatic.category1Html + " " + EDStatic.category2Html) +
            "</h3>\n");
        String attsInURLs[] = EDStatic.categoryAttributesInURLs;
        HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
        writer.write(widgets.select("cat1", "", Math.min(attsInURLs.length, 12),
            attsInURLs, String2.indexOf(attsInURLs, attributeInURL), 
            "onchange=\"window.location='" + tErddapUrl + "/categorize/' + " +
                "this.options[this.selectedIndex].text + '/index.html?" +
                EDStatic.encodedPassThroughPIppQueryPage1(request) +
                "';\""));
        writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better

    }

    /** 
     * This writes the html with the category options to the writer (in a table with lots of columns).
     *
     * @param request
     * @param loggedInAs
     * @param writer
     * @param attribute must be valid  (e.g., ioos_category)
     * @param attributeInURL must be valid
     * @param value may be null or invalid (e.g., Location)
     * @throws Throwable if trouble
     */
    public void writeCategoryOptionsHtml2(HttpServletRequest request, 
        String loggedInAs, Writer writer, 
        String attribute, String attributeInURL, String value) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String values[] = categoryInfo(attribute).toArray();
        writer.write(
            "<h3>2) " + 
            MessageFormat.format(EDStatic.categorySearchHtml, attributeInURL) +
            ":&nbsp;" +
            EDStatic.htmlTooltipImage(loggedInAs, EDStatic.categoryClickHtml) +
            "</h3>\n");
        if (values.length == 0) {
            writer.write(MustBe.THERE_IS_NO_DATA);
        } else {
            HtmlWidgets widgets = new HtmlWidgets(true, EDStatic.imageDirUrl(loggedInAs)); //true=htmlTooltips
            writer.write(widgets.select("cat2", "", Math.min(values.length, 12),
                values, String2.indexOf(values, value), 
                "onchange=\"window.location='" + 
                    tErddapUrl + "/categorize/" + attributeInURL + "/' + " +
                    "this.options[this.selectedIndex].text + '/index.html?" +
                    EDStatic.encodedPassThroughPIppQueryPage1(request) +
                    "';\""));
        }
        writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
    }


    /** 
     * This sends a response: a table with two columns (Category, URL).
     *
     * @param request
     * @param response
     * @param loggedInAs  the name of the logged-in user (or null if not logged-in)
     * @param attribute must be valid  (e.g., ioos_category)
     * @param attributeInURL must be valid  (e.g., ioos_category)
     * @param fileTypeName a plainFileType, e.g., .htmlTable
     * @throws Throwable if trouble
     */
    public void sendCategoryPftOptionsTable(HttpServletRequest request, 
        HttpServletResponse response, String loggedInAs, String attribute, 
        String attributeInURL, String fileTypeName) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        StringArray cats = categoryInfo(attribute); //already safe
        int nCats = cats.size();

        //make the table
        Table table = new Table();
        StringArray catCol = new StringArray();
        StringArray urlCol = new StringArray();
        table.addColumn("Category", catCol);
        table.addColumn("URL", urlCol);
        String pipp1 = EDStatic.passThroughPIppQueryPage1(request);
        for (int i = 0; i < nCats; i++) {
            String cat = cats.get(i); //e.g., Temperature    already safe
            catCol.add(cat);
            urlCol.add(tErddapUrl + "/categorize/" + attributeInURL + 
                "/" + cat + "/index" + fileTypeName + "?" + pipp1); 
        }

        //send it  
        sendPlainTable(loggedInAs, request, response, table, attributeInURL, fileTypeName);
    }


    /**
     * Given a list of datasetIDs, this makes a sorted table of the datasets info.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This is used to ensure that the user sees only datasets they have a 
     *    right to know exist.  But this has almost always already been done.
     * @param datasetIDs the id's of the datasets (e.g., "pmelTao") that should be put into the table
     * @param sortByTitle if true, rows will be sorted by title.
     *    If false, they are left in order of datasetIDs.
     * @param fileTypeName the file type name (e.g., ".htmlTable") to use for info links
     * @return table a table with plain text information about the datasets
     */
    public Table makePlainDatasetTable(String loggedInAs, 
        StringArray datasetIDs, boolean sortByTitle, String fileTypeName) {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String roles[] = EDStatic.getRoles(loggedInAs);
        boolean isLoggedIn = loggedInAs != null && !loggedInAs.equals(EDStatic.loggedInAsHttps);
        Table table = new Table();
        StringArray gdCol = new StringArray();
        StringArray subCol = new StringArray();
        StringArray tdCol = new StringArray();
        StringArray magCol = new StringArray();
        StringArray sosCol = new StringArray();
        StringArray wcsCol = new StringArray();
        StringArray wmsCol = new StringArray();
        StringArray filesCol = new StringArray();
        StringArray accessCol = new StringArray();
        StringArray titleCol = new StringArray();
        StringArray summaryCol = new StringArray();
        StringArray fgdcCol = new StringArray();
        StringArray iso19115Col = new StringArray();
        StringArray infoCol = new StringArray();
        StringArray backgroundCol = new StringArray();
        StringArray rssCol = new StringArray();
        StringArray emailCol = new StringArray();
        StringArray institutionCol = new StringArray();
        StringArray idCol = new StringArray();  //useful for java programs

        // !!! DON'T TRANSLATE THESE, SO CONSISTENT FOR ALL ERDDAPs 
        table.addColumn("griddap", gdCol);  //just protocol name
        table.addColumn("Subset", subCol);
        table.addColumn("tabledap", tdCol);
        table.addColumn("Make A Graph", magCol);
        if (EDStatic.sosActive) table.addColumn("sos", sosCol);
        if (EDStatic.wcsActive) table.addColumn("wcs", wcsCol);
        if (EDStatic.wmsActive) table.addColumn("wms", wmsCol);
        if (EDStatic.filesActive) table.addColumn("files", filesCol);
        if (EDStatic.authentication.length() > 0)
            table.addColumn("Accessible", accessCol);
        int sortOn = table.addColumn("Title", titleCol);
        table.addColumn("Summary", summaryCol);
        if (EDStatic.fgdcActive)     table.addColumn("FGDC", fgdcCol);
        if (EDStatic.iso19115Active) table.addColumn("ISO 19115", iso19115Col);
        table.addColumn("Info", infoCol);
        table.addColumn("Background Info", backgroundCol);
        table.addColumn("RSS", rssCol);
        if (EDStatic.subscriptionSystemActive) table.addColumn("Email", emailCol);
        table.addColumn("Institution", institutionCol);
        table.addColumn("Dataset ID", idCol);
        for (int i = 0; i < datasetIDs.size(); i++) {
            String tId = datasetIDs.get(i);
            EDD edd = gridDatasetHashMap.get(tId);
            if (edd == null) 
                edd = tableDatasetHashMap.get(tId);
            if (edd == null) //perhaps just deleted
                continue;
            boolean isAllDatasets = tId.equals(EDDTableFromAllDatasets.DATASET_ID);
            boolean isAccessible = edd.isAccessibleTo(roles);
            boolean graphsAccessible = isAccessible || edd.graphsAccessibleToPublic();
            if (!EDStatic.listPrivateDatasets && !isAccessible && !graphsAccessible)
                continue;

            //just show things (URLs, info) that user has access to 
            String daps = tErddapUrl + "/" + edd.dapProtocol() + "/" + tId; //without an extension, so easy to add
            gdCol.add(isAccessible && edd instanceof EDDGrid? daps : "");
            subCol.add(isAccessible && edd.accessibleViaSubset().length() == 0? 
                daps + ".subset" : "");
            tdCol.add(isAccessible && edd instanceof EDDTable? daps : "");
            magCol.add(graphsAccessible && edd.accessibleViaMAG().length() == 0? //graphs
                daps + ".graph" : "");
            sosCol.add(isAccessible && edd.accessibleViaSOS().length() == 0? 
                tErddapUrl + "/sos/" + tId + "/" + EDDTable.sosServer : "");
            wcsCol.add(isAccessible && edd.accessibleViaWCS().length() == 0? 
                tErddapUrl + "/wcs/" + tId + "/" + EDDGrid.wcsServer : "");
            wmsCol.add(graphsAccessible && edd.accessibleViaWMS().length() == 0? //graphs
                tErddapUrl + "/wms/" + tId + "/" + EDD.WMS_SERVER : "");
            filesCol.add(isAccessible && edd.accessibleViaFilesDir().length() > 0? 
                tErddapUrl + "/files/" + tId + "/" : "");
            accessCol.add(edd.getAccessibleTo() == null? "public" :
                isAccessible? "yes" : 
                graphsAccessible? "graphs" : 
                isLoggedIn? "no" : "log in");
            //only title, summary, institution, id are always accessible if !listPrivateDatasets
            titleCol.add(edd.title());
            summaryCol.add(edd.extendedSummary());
            fgdcCol.add(graphsAccessible && edd.accessibleViaFGDC().length() == 0? 
                tErddapUrl + "/" + EDStatic.fgdcXmlDirectory     + 
                    edd.datasetID() + EDD.fgdcSuffix     + ".xml" : "");
            iso19115Col.add(graphsAccessible && edd.accessibleViaISO19115().length() == 0? 
                tErddapUrl + "/" + EDStatic.iso19115XmlDirectory + 
                    edd.datasetID() + EDD.iso19115Suffix + ".xml" : "");
            infoCol.add(graphsAccessible? tErddapUrl + "/info/" + edd.datasetID() + "/index" + fileTypeName : "");
            backgroundCol.add(graphsAccessible? edd.infoUrl() : "");
            rssCol.add(graphsAccessible && !isAllDatasets?  
                EDStatic.erddapUrl + "/rss/" + edd.datasetID()+ ".rss" : ""); //never https url
            emailCol.add(graphsAccessible && EDStatic.subscriptionSystemActive && !isAllDatasets?
                tErddapUrl + "/" + Subscriptions.ADD_HTML + 
                    "?datasetID=" + edd.datasetID()+ "&showErrors=false&email=" : 
                "");
            institutionCol.add(edd.institution());
            idCol.add(tId);
        }
        if (sortByTitle)
            table.sortIgnoreCase(new int[]{sortOn}, new boolean[]{true});
        return table;
    }

    /**
     * Given a list of datasetIDs, this makes a sorted table of the datasets info.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This is used to ensure that the user sees only datasets they have a 
     *    right to know exist.  But this has almost always already been done.
     * @param datasetIDs the id's of the datasets (e.g., "pmelTao") that should be put into the table
     * @param sortByTitle if true, rows will be sorted by title.
     *    If false, they are left in order of datasetIDs.
     * @return table a table with html-formatted information about the datasets
     */
    public Table makeHtmlDatasetTable(String loggedInAs,
        StringArray datasetIDs, boolean sortByTitle) {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String roles[] = EDStatic.getRoles(loggedInAs);
        boolean isLoggedIn = loggedInAs != null && !loggedInAs.equals(EDStatic.loggedInAsHttps);
        Table table = new Table();
        StringArray gdCol = new StringArray();
        StringArray subCol = new StringArray();
        StringArray tdCol = new StringArray();
        StringArray magCol = new StringArray();
        StringArray sosCol = new StringArray();
        StringArray wcsCol = new StringArray();
        StringArray wmsCol = new StringArray();
        StringArray filesCol = new StringArray();
        StringArray accessCol = new StringArray();
        StringArray plainTitleCol = new StringArray(); //for sorting
        StringArray titleCol = new StringArray();
        StringArray summaryCol = new StringArray();
        StringArray infoCol = new StringArray();
        StringArray backgroundCol = new StringArray();
        StringArray rssCol = new StringArray();  
        StringArray emailCol = new StringArray(); 
        StringArray institutionCol = new StringArray();
        StringArray idCol = new StringArray();  //useful for java programs
        table.addColumn("Grid<br>DAP<br>Data", gdCol);
        table.addColumn("Sub-<br>set", subCol);
        table.addColumn("Table<br>DAP<br>Data", tdCol);
        table.addColumn("Make<br>A<br>Graph", magCol);
        if (EDStatic.sosActive) table.addColumn("S<br>O<br>S", sosCol);
        if (EDStatic.wcsActive) table.addColumn("W<br>C<br>S", wcsCol);
        if (EDStatic.wmsActive) table.addColumn("W<br>M<br>S", wmsCol);
        if (EDStatic.filesActive) table.addColumn("Source<br>Data<br>Files", filesCol);
        String accessTip = EDStatic.dtAccessible;
        if (isLoggedIn)
            accessTip += EDStatic.dtAccessibleYes + //"You are logged in and ...
                (EDStatic.listPrivateDatasets? EDStatic.dtAccessibleNo : "");   //"You are logged in and ...
        if (EDStatic.authentication.length() > 0 && !isLoggedIn && //this erddap supports logging in
            EDStatic.listPrivateDatasets) 
            accessTip += EDStatic.dtAccessibleLogIn;
        accessTip += EDStatic.dtAccessibleGraphs;
        if (EDStatic.authentication.length() > 0)
            table.addColumn("Acces-<br>sible<br>" + EDStatic.htmlTooltipImage(loggedInAs, accessTip),
                accessCol);
        String loginHref = EDStatic.authentication.length() == 0? "no" :
            "<a rel=\"bookmark\" href=\"" + EDStatic.erddapHttpsUrl + "/login.html\" " +
            "title=\"" + EDStatic.dtLogIn + "\">log in</a>";
        table.addColumn("Title", titleCol);
        int sortOn = table.addColumn("Plain Title", plainTitleCol); 
        table.addColumn("Sum-<br>mary", summaryCol);
        table.addColumn(
            (EDStatic.fgdcActive?     "FGDC,<br>" : "") +
            (EDStatic.iso19115Active? "ISO,<br>"  : "") +
            (EDStatic.fgdcActive || EDStatic.iso19115Active? "Metadata" : "Meta-<br>data"), 
            infoCol);
        table.addColumn("Back-<br>ground<br>Info", backgroundCol);
        table.addColumn("RSS", rssCol);
        if (EDStatic.subscriptionSystemActive) table.addColumn("E<br>mail", emailCol);
        table.addColumn("Institution", institutionCol);
        table.addColumn("Dataset ID", idCol);
        String externalLinkHtml = EDStatic.externalLinkHtml(tErddapUrl);
        for (int i = 0; i < datasetIDs.size(); i++) {
            String tId = datasetIDs.get(i);
            EDD edd = gridDatasetHashMap.get(tId);
            if (edd == null)
                edd = tableDatasetHashMap.get(tId);
            if (edd == null)  //if just deleted
                continue; 
            boolean isAllDatasets = tId.equals(EDDTableFromAllDatasets.DATASET_ID);
            boolean isAccessible = edd.isAccessibleTo(roles);
            boolean graphsAccessible = isAccessible || edd.graphsAccessibleToPublic();
            if (!EDStatic.listPrivateDatasets && !isAccessible && !graphsAccessible)
                continue;

            //just show things (URLs, info) user has access to
            String daps = "&nbsp;<a rel=\"chapter\" " +
                "href=\"" + tErddapUrl + "/" + edd.dapProtocol() + "/" + tId + ".html\" " +
                "title=\"" + EDStatic.dtDAF1 + " " + edd.dapProtocol() + " " + EDStatic.dtDAF2 + "\" " +
                ">data</a>&nbsp;"; 
            gdCol.add(isAccessible && edd instanceof EDDGrid?  daps : "&nbsp;"); 
            subCol.add(isAccessible && edd.accessibleViaSubset().length() == 0? 
                " &nbsp;<a rel=\"chapter\" " +
                    "href=\"" + tErddapUrl + "/tabledap/" + tId + ".subset\" " +
                    "title=\"" + EDStatic.dtSubset + "\" " +
                    ">set</a>" : 
                "&nbsp;");
            tdCol.add(isAccessible && edd instanceof EDDTable? daps : "&nbsp;");
            magCol.add(graphsAccessible && edd.accessibleViaMAG().length() == 0? //graphs
                " &nbsp;<a rel=\"chapter\" " +
                    "href=\"" + tErddapUrl + "/" + edd.dapProtocol() + 
                    "/" + tId + ".graph\" " +
                    "title=\"" + EDStatic.dtMAG + "\" " +
                    ">graph</a>" : 
                "&nbsp;");
            sosCol.add(isAccessible && edd.accessibleViaSOS().length() == 0? 
                "&nbsp;<a rel=\"chapter\" " +
                    "href=\"" + tErddapUrl + "/sos/" + tId + "/index.html\" " +
                    "title=\"" + EDStatic.dtSOS + "\" >" +
                    "S</a>&nbsp;" : 
                "&nbsp;");
            wcsCol.add(isAccessible && edd.accessibleViaWCS().length() == 0? 
                "&nbsp;<a rel=\"chapter\" " +
                    "href=\"" + tErddapUrl + "/wcs/" + tId + "/index.html\" " +
                    "title=\"" + EDStatic.dtWCS + "\" >" +
                    "C</a>&nbsp;" : 
                "&nbsp;");
            wmsCol.add(graphsAccessible && edd.accessibleViaWMS().length() == 0? //graphs
                "&nbsp;<a rel=\"chapter\" " +
                    "href=\"" + tErddapUrl + "/wms/" + tId + "/index.html\" " +
                    "title=\"" + EDStatic.dtWMS + "\" >" +
                    "M</a>&nbsp;" : 
                "&nbsp;");
            filesCol.add(isAccessible && edd.accessibleViaFilesDir().length() > 0? 
                "&nbsp;&nbsp;<a rel=\"chapter\" " +
                    "href=\"" + tErddapUrl + "/files/" + tId + "/\" " +
                    "title=\"" + EDStatic.dtFiles + "\" >" +
                    "files</a>&nbsp;" : 
                "&nbsp;");
            accessCol.add(edd.getAccessibleTo() == null? "public" :
                isAccessible? "yes" : 
                graphsAccessible? "graphs" : 
                isLoggedIn? "no" : loginHref);
            //only title, summary, institution, id are always accessible if !listPrivateDatasets
            String tTitle = edd.title();
            plainTitleCol.add(tTitle);
            String ttTitle = String2.noLongLines(tTitle, EDStatic.TITLE_DOT_LENGTH, ""); //insert newlines
            ttTitle = XML.encodeAsHTML(ttTitle); //newlines intact
            ttTitle = String2.replaceAll(ttTitle, "\n", "<br>");
            titleCol.add(ttTitle);

            summaryCol.add("&nbsp;&nbsp;&nbsp;" + EDStatic.htmlTooltipImage(loggedInAs, 
                "<div class=\"standard_max_width\">" + XML.encodeAsPreHTML(edd.extendedSummary()) +
                "</div>"));
            infoCol.add(!graphsAccessible? "" : 
                "\n&nbsp;" +
                //fgdc
                (edd.accessibleViaFGDC().length() > 0?  
                    (EDStatic.fgdcActive? "&nbsp;&nbsp;&nbsp;" : "") :
                    "&nbsp;<a rel=\"chapter\" " +
                        "href=\"" + tErddapUrl + "/" + EDStatic.fgdcXmlDirectory + 
                        edd.datasetID() + EDD.fgdcSuffix + ".xml\" " + 
                        "title=\"" + 
                        XML.encodeAsHTMLAttribute(MessageFormat.format(EDStatic.metadataDownload, "FGDC")) + 
                        "\" >F</a>") +
                "\n" +
                //iso
                (edd.accessibleViaISO19115().length() > 0? 
                    (EDStatic.iso19115Active? "&nbsp;&nbsp;&nbsp;&nbsp;" : "") :
                    "&nbsp;<a rel=\"chapter\" " +
                        "href=\"" + tErddapUrl + "/" + EDStatic.iso19115XmlDirectory +
                        edd.datasetID() + EDD.iso19115Suffix + ".xml\" " + 
                    "title=\"" + 
                        XML.encodeAsHTMLAttribute(
                            MessageFormat.format(EDStatic.metadataDownload, "ISO 19115-2/19139")) + 
                        "\" >&nbsp;I&nbsp;</a>") +
                //dataset metadata
                "\n&nbsp;" +
                "<a rel=\"chapter\" " +
                "href=\"" + tErddapUrl + "/info/" + edd.datasetID() + 
                    "/index.html\" " + //here, always .html
                "title=\"" + EDStatic.clickInfo + "\" >M</a>" +
                "\n&nbsp;");
            backgroundCol.add(!graphsAccessible? "" : 
                "<a rel=\"bookmark\" " +
                "href=\"" + XML.encodeAsHTML(edd.infoUrl()) + "\" " +
                "title=\"" + EDStatic.clickBackgroundInfo + "\" >background" +
                    (edd.infoUrl().startsWith(EDStatic.baseUrl)? "" : externalLinkHtml) + 
                    "</a>");
            rssCol.add(graphsAccessible && !isAllDatasets? 
                edd.rssHref(loggedInAs) : "&nbsp;");
            emailCol.add(graphsAccessible && EDStatic.subscriptionSystemActive &&
                !isAllDatasets? 
                edd.emailHref(loggedInAs) : "&nbsp;");
            String tInstitution = edd.institution();
            if (tInstitution.length() > 20) 
                institutionCol.add(
                    "<table class=\"compact nowrap\" style=\"width:100%;\">\n" +
                    "<tr>\n" +
                    "  <td>" + 
                        XML.encodeAsHTML(tInstitution.substring(0, 17)) + "...</td>\n" +
                    "  <td class=\"R\">&nbsp;" +
                        EDStatic.htmlTooltipImage(loggedInAs, 
                            "<div class=\"standard_max_width\">" + 
                            XML.encodeAsPreHTML(tInstitution) +
                            "</div>") +
                        "</td>\n" +
                    "</tr>\n" +
                    "</table>\n");
                    //XML.encodeAsHTML(tInstitution.substring(0, 15)) + " ... " +
                    //EDStatic.htmlTooltipImage(loggedInAs, 
                    //    XML.encodeAsPreHTML(tInstitution, 82)));
            else institutionCol.add(XML.encodeAsHTML(tInstitution));
            idCol.add(tId);
        }
        if (sortByTitle) 
            table.sortIgnoreCase(new int[]{sortOn}, new boolean[]{true});
        table.removeColumn(sortOn); //in any case, remove the plainTitle column
        return table;
    }

    /**
     * This writes the plain (non-html) table as a plainFileType response.
     *
     * @param fileName e.g., Time
     * @param fileTypeName e.g., .htmlTable
     */
    void sendPlainTable(String loggedInAs, HttpServletRequest request, HttpServletResponse response, 
        Table table, String fileName, String fileTypeName) throws Throwable {

        //handle in-common json stuff
        String jsonp = null;
        if (fileTypeName.equals(".geoJson") || //well, no geoJson support here
            fileTypeName.startsWith(".json") || 
            fileTypeName.startsWith(".ncoJson")) {  //well, no ncoJson support here
            //did query include &.jsonp= ?
            String parts[] = Table.getDapQueryParts(request.getQueryString()); //decoded
            jsonp = String2.stringStartsWith(parts, ".jsonp="); //may be null
            if (jsonp != null) {
                jsonp = jsonp.substring(7);
                if (!fileTypeName.equals(".geoJson") && 
                    !fileTypeName.equals(".json") &&  //e.g., .jsonlCSV .jsonlKVP
                    !fileTypeName.equals(".ncoJson")) 
                    throw new SimpleException(EDStatic.errorJsonpNotAllowed);
                if (!String2.isJsonpNameSafe(jsonp))
                    throw new SimpleException(EDStatic.errorJsonpFunctionName);
            }
        }

        //get outSource
        int po = String2.indexOf(EDDTable.dataFileTypeNames, fileTypeName);
        String fileTypeExtension = EDDTable.dataFileTypeExtensions[po];
        OutputStreamSource outSource = new OutputStreamFromHttpResponse(
            request, response, fileName, 
            jsonp == null? fileTypeName : ".jsonp",  //.jsonp pseudo fileTypeName to get correct mime type
            fileTypeExtension); 

        //different fileTypes
        if (fileTypeName.equals(".htmlTable")) {
            TableWriterHtmlTable.writeAllAndFinish(null, null, loggedInAs, table, outSource, 
                true, fileName, false,
                "", "", true, false, -1); //pre, post, encodeAsHTML, writeUnits

        } else if (fileTypeName.equals(".json")) {
            TableWriterJson.writeAllAndFinish(null, null, table, outSource, jsonp, false); //writeUnits

        } else if (fileTypeName.equals(".jsonlCSV1")) {
            TableWriterJsonl.writeAllAndFinish(null, null, table, outSource, true,  false, jsonp); //writeColNames, writeKVP

        } else if (fileTypeName.equals(".jsonlCSV")) {
            TableWriterJsonl.writeAllAndFinish(null, null, table, outSource, false, false, jsonp); //writeColNames, writeKVP

        } else if (fileTypeName.equals(".jsonlKVP")) {
            TableWriterJsonl.writeAllAndFinish(null, null, table, outSource, false, true, jsonp);  //writeColNames, writeKVP

        } else if (fileTypeName.equals(".csv")) {
            TableWriterSeparatedValue.writeAllAndFinish(null, null, table, outSource,
                ",", true, true, '0', "NaN"); //separator, quoted, writeColumnNames, writeUnits

        } else if (fileTypeName.equals(".itx")) {

            table.saveAsIgor(new BufferedWriter(new OutputStreamWriter(
                outSource.outputStream(Table.IgorCharset)))); 

        } else if (fileTypeName.equals(".mat")) {
            //avoid troublesome var names (e.g., with spaces)
            int nColumns = table.nColumns();
            for (int col = 0; col < nColumns; col++) 
                table.setColumnName(col, 
                    String2.modifyToBeFileNameSafe(table.getColumnName(col)));

            //??? use goofy standard structure name (nice that it's always the same);
            //  could use fileName but often long
            table.saveAsMatlab(outSource.outputStream(""), "response");  

        } else if (fileTypeName.equals(".nc")) {
            //avoid troublesome var names (e.g., with spaces)
            int nColumns = table.nColumns();
            for (int col = 0; col < nColumns; col++) 
                table.setColumnName(col, 
                    String2.modifyToBeFileNameSafe(table.getColumnName(col)));

            //This is different from other formats (which stream the results to the user),
            //since a file must be created before it can be sent.
            //Append a random# to fileName to deal with different responses 
            //for almost simultaneous requests
            //(e.g., all Advanced Search requests have fileName=AdvancedSearch)
            String ncFileName = fileName + "_" + Math2.random(Integer.MAX_VALUE) + ".nc";
            table.saveAsFlatNc(EDStatic.fullPlainFileNcCacheDirectory + ncFileName, 
                "row", false); //convertToFakeMissingValues          
            OutputStream out = outSource.outputStream("");
            doTransfer(request, response, EDStatic.fullPlainFileNcCacheDirectory, 
                "_plainFileNc/", //dir that appears to users (but it doesn't normally)
                ncFileName, out, outSource.usingCompression()); 
            //if simpleDelete fails, cache cleaning will delete it later
            File2.simpleDelete(EDStatic.fullPlainFileNcCacheDirectory + ncFileName); 

        } else if (fileTypeName.equals(".nccsv")) {
            TableWriterNccsv.writeAllAndFinish(null, null, table, outSource); 

        } else if (fileTypeName.equals(".tsv")) {
            TableWriterSeparatedValue.writeAllAndFinish(null, null, table, outSource, 
                "\t", false, true, '0', "NaN"); //separator, quoted, writeColumnNames, writeUnits

        } else if (fileTypeName.equals(".xhtml")) {
            TableWriterHtmlTable.writeAllAndFinish(null, null, 
                loggedInAs, table, outSource, true, fileName, true,
                "", "", true, false, -1); //pre, post, encodeAsHTML, writeUnits

        } else {
            throw new SimpleException(
                MessageFormat.format(EDStatic.unsupportedFileType, fileTypeName));
        }

        //essential
        OutputStream out = outSource.existingOutputStream(); 
        if (out != null) {
            if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
            out.close(); 
        }
    }

    public static void sendRedirect(HttpServletResponse response, String url) 
        throws Exception {
        url = SSR.fixPercentEncodedUrl(url); //crude insurance for new percentEncoding requirements
        if (verbose) String2.log("redirected to " + url);
        response.sendRedirect(url);
    }

    /**
     * This makes a erddapContent.zip file with the [tomcat]/content/erddap files for distribution.
     *
     * @param removeDir e.g., "c:/programs/_tomcat/samples/"     
     * @param destinationDir  e.g., "c:/backup/"
     */
    public static void makeErddapContentZip(String removeDir, String destinationDir) throws Throwable {
        String2.log("*** makeErddapContentZip dir=" + destinationDir);
        String baseDir = removeDir + "content/erddap/";
        SSR.zip(destinationDir + "erddapContent.zip", 
            new String[]{
                baseDir + "datasets.xml",
                baseDir + "setup.xml",
                baseDir + "images/erddapStart2.css"},
            10, removeDir);
    }

    /**
     * This is an attempt to assist Tomcat/Java in shutting down erddap.
     * Tomcat/Java will call this; no one else should.
     * Java calls this when an object is no longer used, just before garbage collection. 
     * 
     */
    protected void finalize() throws Throwable {
        try {  //extra assistance/insurance
            EDStatic.destroy();   //but Tomcat should call ERDDAP.destroy, which calls EDStatic.destroy().
        } catch (Throwable t) {
        }
        super.finalize();
    }

    /**
     * This is used by Bob to do simple tests of the basic Erddap services 
     * from the ERDDAP at EDStatic.erddapUrl. It assumes Bob's test datasets are available.
     *
     */
    public static void testBasic() throws Throwable {
        Erddap.verbose = true;
        Erddap.reallyVerbose = true;
        EDD.testVerboseOn();
        String results, expected;
        String2.log("\n*** Erddap.testBasic");
        int po;

        try {
            //home page
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl); //redirects to index.html
            expected = "The small effort to set up ERDDAP brings many benefits.";
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/"); //redirects to index.html
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.html"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            
            //test version info  (opendap spec section 7.2.5)
            //"version" instead of datasetID
            expected = 
                "Core Version: DAP/2.0\n" +
                "Server Version: dods/3.7\n" +
                "ERDDAP_version: " + EDStatic.erddapVersion + "\n";
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version");
            Test.ensureEqual(results, expected, "results=\n" + results);
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version");
            Test.ensureEqual(results, expected, "results=\n" + results);

            //"version.txt"
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version.txt");
            Test.ensureEqual(results, expected, "results=\n" + results);
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version.txt");
            Test.ensureEqual(results, expected, "results=\n" + results);

            //".ver"
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/etopo180.ver");
            Test.ensureEqual(results, expected, "results=\n" + results);
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.ver");
            Test.ensureEqual(results, expected, "results=\n" + results);


            //help
            expected = "griddap to Request Data and Graphs from Gridded Datasets";
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/help"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/documentation.html"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.help"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);


            expected = "tabledap to Request Data and Graphs from Tabular Datasets";
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/help"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/documentation.html"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.help"); 
            Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

            //error 404
            results = "";
            try {
                SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/gibberish"); 
            } catch (Throwable t) {
                results = t.toString();
            }
            Test.ensureTrue(results.indexOf("java.io.FileNotFoundException") >= 0, "results=\n" + results);

            //info    list all datasets
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
                EDStatic.defaultPIppQuery); 
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.csv?" +
                EDStatic.defaultPIppQuery); 
            Test.ensureTrue(results.indexOf("</html>") < 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.html"); 
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.tsv"); 
            Test.ensureTrue(results.indexOf("\t") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

            //search    
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
                EDStatic.defaultPIppQuery);
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Do a Full Text Search for Datasets") >= 0, "results=\n" + results);
            //index.otherFileType must have ?searchFor=...

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
                EDStatic.defaultPIppQuery + "&searchFor=all");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\n") >= 0,
                "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">GLOBEC NEP Rosette Bottle Data (2002)") >= 0,
                "results=\n" + results);            
           
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.htmlTable?" +
                EDStatic.defaultPIppQuery + "&searchFor=all");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\n") >= 0,
                "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
                "results=\n" + results);            
           
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\n") > 0,
                "results=\n" + results);

            //.json
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
            expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Accessible\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n" +
"    \"rows\": [\n" +
"      [\"\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n" +
"      [\"\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"],\n" +
"      [\"\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //.json with jsonp
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel&.jsonp=fnName");
            expected = 
"fnName({\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Accessible\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n" +
"    \"rows\": [\n" +
"      [\"\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n" +
"      [\"\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"],\n" +
"      [\"\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n" +
"    ]\n" +
"  }\n" +
"}\n" +
")";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //and read the header to see the mime type
            results = String2.toNewlineString(
                SSR.dosShell("curl -i \"" + EDStatic.erddapUrl + "/search/index.json?" +
                    EDStatic.defaultPIppQuery + "&searchFor=tao+pmel&.jsonp=fnName\"", 
                    120).toArray());
            po = results.indexOf("HTTP");
            results = results.substring(po);
            po = results.indexOf("chunked");
            results = results.substring(0, po + 7);
            expected = 
"HTTP/1.1 200 \n" +
"Content-Encoding: identity\n" +
"Content-Type: application/javascript;charset=UTF-8\n" +
"Transfer-Encoding: chunked";
            Test.ensureEqual(results, expected, "results=\n" + results);



            //.jsonlCSV1
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV1?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
            expected = 
"[\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Accessible\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"]\n" +
"[\"\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n" +
"[\"\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"]\n" +
"[\"\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rlPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //.jsonlCSV
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
            expected = 
"[\"\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/cwexperimental/files/pmelTaoDySst/\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n" +
"[\"\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rPmelTaoDySst\"]\n" +
"[\"\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"\", \"\", \"public\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/cwexperimental/info/rlPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //.jsonlKVP
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlKVP?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
            expected = 
"{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/cwexperimental/tabledap/pmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/cwexperimental/files/pmelTaoDySst/\", \"Accessible\":\"public\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"FGDC\":\"http://localhost:8080/cwexperimental/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/cwexperimental/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/cwexperimental/rss/pmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySst\"}\n" +
"{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/cwexperimental/tabledap/rPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"\", \"Accessible\":\"public\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"FGDC\":\"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rPmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/cwexperimental/info/rPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/cwexperimental/rss/rPmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rPmelTaoDySst\"}\n" +
"{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/cwexperimental/tabledap/rlPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"\", \"Accessible\":\"public\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\\n\\ncdm_data_type = TimeSeries\\nVARIABLES:\\narray\\nstation\\nwmo_platform_code\\nlongitude (Nominal Longitude, degrees_east)\\nlatitude (Nominal Latitude, degrees_north)\\ntime (Centered Time, seconds since 1970-01-01T00:00:00Z)\\ndepth (m)\\nT_25 (Sea Surface Temperature, degree_C)\\nQT_5025 (Sea Surface Temperature Quality)\\nST_6025 (Sea Surface Temperature Source)\\n\", \"FGDC\":\"http://localhost:8080/cwexperimental/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/cwexperimental/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/cwexperimental/info/rlPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/cwexperimental/rss/rlPmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rlPmelTaoDySst\"}\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.tsv?" +
                EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
            Test.ensureTrue(results.indexOf("\tTAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\t") > 0,
                "results=\n" + results);


            //categorize
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/index.html");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">standard_name\n") >= 0,
                "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/index.json");
            Test.ensureEqual(results, 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
"    \"columnTypes\": [\"String\", \"String\"],\n" +
"    \"rows\": [\n" +
"      [\"cdm_data_type\", \"http://localhost:8080/cwexperimental/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"institution\", \"http://localhost:8080/cwexperimental/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"ioos_category\", \"http://localhost:8080/cwexperimental/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"keywords\", \"http://localhost:8080/cwexperimental/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"long_name\", \"http://localhost:8080/cwexperimental/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"standard_name\", \"http://localhost:8080/cwexperimental/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"variableName\", \"http://localhost:8080/cwexperimental/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n" +
"    ]\n" +
"  }\n" +
"}\n", 
                "results=\n" + results);

            //json with jsonp 
            String jsonp = "myFunctionName";
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/index.json?.jsonp=" + SSR.percentEncode(jsonp));
            Test.ensureEqual(results, 
jsonp + "(" +
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
"    \"columnTypes\": [\"String\", \"String\"],\n" +
"    \"rows\": [\n" +
"      [\"cdm_data_type\", \"http://localhost:8080/cwexperimental/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"institution\", \"http://localhost:8080/cwexperimental/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"ioos_category\", \"http://localhost:8080/cwexperimental/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"keywords\", \"http://localhost:8080/cwexperimental/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"long_name\", \"http://localhost:8080/cwexperimental/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"standard_name\", \"http://localhost:8080/cwexperimental/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"variableName\", \"http://localhost:8080/cwexperimental/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n" +
"    ]\n" +
"  }\n" +
"}\n" +
")", 
                "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/standard_name/index.html");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">sea_water_temperature\n") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/standard_name/index.json");
            Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"sea_water_temperature\"") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/institution/index.html");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">ioos_category\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">noaa_coastwatch_west_coast_node\n") >= 0, 
                "results=\n" + results);
            
            results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/institution/index.tsv"));
            Test.ensureTrue(results.indexOf("Category[9]URL[10]") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                "noaa_coastwatch_west_coast_node[9]http://localhost:8080/cwexperimental/categorize/institution/noaa_coastwatch_west_coast_node/index.tsv?page=1&itemsPerPage=1000[10]") >= 0, 
                "results=\n" + results);
            
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/standard_name/sea_water_temperature/index.html");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">erdGlobecBottle\n") >= 0,
                "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                "/categorize/standard_name/sea_water_temperature/index.json");
            expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", " +
                (EDStatic.sosActive? "\"sos\", " : "") +
                (EDStatic.wcsActive? "\"wcs\", " : "") +
                (EDStatic.wmsActive? "\"wms\", " : "") + 
                (EDStatic.filesActive? "\"files\", " : "") + 
                (EDStatic.authentication.length() > 0? "\"Accessible\", " : "") +
                "\"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", " +
                (EDStatic.subscriptionSystemActive? "\"Email\", " : "") +
                "\"Institution\", \"Dataset ID\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", " +
                (EDStatic.sosActive? "\"String\", " : "") +
                (EDStatic.wcsActive? "\"String\", " : "") +
                (EDStatic.wmsActive? "\"String\", " : "") +
                (EDStatic.filesActive? "\"String\", " : "") +
                (EDStatic.authentication.length() > 0? "\"String\", " : "") +
                "\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", " +
                (EDStatic.subscriptionSystemActive? "\"String\", " : "") +
                "\"String\", \"String\"],\n" +
"    \"rows\": [\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            expected =            
"http://localhost:8080/cwexperimental/tabledap/erdGlobecBottle.subset\", " +                
"\"http://localhost:8080/cwexperimental/tabledap/erdGlobecBottle\", " +
"\"http://localhost:8080/cwexperimental/tabledap/erdGlobecBottle.graph\", " + 
                (EDStatic.sosActive? "\"\", " : "") + //currently, it isn't made available via sos
                (EDStatic.wcsActive? "\"\", " : "") +
                (EDStatic.wmsActive? "\"\", " : "") +
                (EDStatic.filesActive? "\"http://localhost:8080/cwexperimental/files/erdGlobecBottle/\", " : "") +
                (EDStatic.authentication.length() > 0? "\"public\", " : "") +
                "\"GLOBEC NEP Rosette Bottle Data (2002)\", \"GLOBEC (GLOBal " +
                "Ocean ECosystems Dynamics) NEP (Northeast Pacific)\\nRosette Bottle Data from " +
                "New Horizon Cruise (NH0207: 1-19 August 2002).\\nNotes:\\nPhysical data " +
                "processed by Jane Fleischbein (OSU).\\nChlorophyll readings done by " +
                "Leah Feinberg (OSU).\\nNutrient analysis done by Burke Hales (OSU).\\n" +
                "Sal00 - salinity calculated from primary sensors (C0,T0).\\n" +
                "Sal11 - salinity calculated from secondary sensors (C1,T1).\\n" +
                "secondary sensor pair was used in final processing of CTD data for\\n" +
                "most stations because the primary had more noise and spikes. The\\n" +
                "primary pair were used for cast #9, 24, 48, 111 and 150 due to\\n" +
                "multiple spikes or offsets in the secondary pair.\\n" +
                "Nutrient samples were collected from most bottles; all nutrient data\\n" +
                "developed from samples frozen during the cruise and analyzed ashore;\\n" +
                "data developed by Burke Hales (OSU).\\n" +
                "Operation Detection Limits for Nutrient Concentrations\\n" +
                "Nutrient  Range         Mean    Variable         Units\\n" +
                "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\\n" +
                "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\\n" +
                "Si        0.13-0.24     0.16    Silicate         micromoles per liter\\n" +
                "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\\n" +
                "Dates and Times are UTC.\\n\\n" +
                "For more information, see https://www.bco-dmo.org/dataset/2452\\n\\n" +
                //was "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\\n\\n" +
                "Inquiries about how to access this data should be directed to\\n" +
                "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\\n\\n" +
                "cdm_data_type = TrajectoryProfile\\n" +
                "VARIABLES:\\ncruise_id\\n... (24 more variables)\\n\", " +
                "\"http://localhost:8080/cwexperimental/metadata/fgdc/xml/erdGlobecBottle_fgdc.xml\", " + 
                "\"http://localhost:8080/cwexperimental/metadata/iso19115/xml/erdGlobecBottle_iso19115.xml\", " +
                "\"http://localhost:8080/cwexperimental/info/erdGlobecBottle/index.json\", " +
                "\"http://www.globec.org/\", " +
                "\"http://localhost:8080/cwexperimental/rss/erdGlobecBottle.rss\", " +
                (EDStatic.subscriptionSystemActive? 
                    "\"http://localhost:8080/cwexperimental/subscriptions/add.html?datasetID=erdGlobecBottle&showErrors=false&email=\", " :
                    "") +
                "\"GLOBEC\", \"erdGlobecBottle\"],";
            po = results.indexOf("http://localhost:8080/cwexperimental/tabledap/erdGlobecBottle");
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

            //griddap
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.html?" + 
                EDStatic.defaultPIppQuery);            
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("List of griddap Datasets") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                ">SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)\n") >= 0,
                "results=\n" + results);
            Test.ensureTrue(results.indexOf(">erdMHchla8day\n") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.json?" + 
                EDStatic.defaultPIppQuery + "");
            Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                "\"SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite)\"") >= 0,
                "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.html");            
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("(Centered Time, UTC)") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("chlorophyll") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.graph");            
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("chlorophyll") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0, "results=\n" + results);


            //tabledap
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.html?" + 
                EDStatic.defaultPIppQuery);
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("List of tabledap Datasets") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
                "results=\n" + results);            
            Test.ensureTrue(results.indexOf(">erdGlobecBottle\n") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.json?" + 
                EDStatic.defaultPIppQuery);
            Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("\"GLOBEC NEP Rosette Bottle Data (2002)\"") >= 0,
                "results=\n" + results);            
            Test.ensureTrue(results.indexOf("\"erdGlobecBottle\"") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.html");            
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.graph");            
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Filled Square") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0, "results=\n" + results);

            //files
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/");
            Test.ensureTrue(results.indexOf("ERDDAP's \"files\" system lets you browse a virtual file system and download source data files.") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("cwwcNDBCMet") >= 0, "results=\n" + results);            
            Test.ensureTrue(results.indexOf("directories") >= 0, "results=\n" + results);            
            Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/cwwcNDBCMet/");
            Test.ensureTrue(results.indexOf("NDBC Standard Meteorological Buoy Data") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Make a graph") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("NDBC&#x5f;41004&#x5f;met&#x2e;nc") >= 0, "results=\n" + results);            
            Test.ensureTrue(results.indexOf("directory") >= 0, "results=\n" + results);            
            Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

            String localName = EDStatic.fullTestCacheDirectory + "NDBC_41004_met.nc";
            File2.delete(localName);
            SSR.downloadFile( //throws Exception if trouble
                EDStatic.erddapUrl + "/files/cwwcNDBCMet/NDBC_41004_met.nc",
                localName, true); //tryToUseCompression
            Test.ensureTrue(File2.isFile(localName), 
                "/files download failed. Not found: localName=" + localName);
            File2.delete(localName);

            //sos
            if (EDStatic.sosActive) {
                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" + 
                    EDStatic.defaultPIppQuery);
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("List of SOS Datasets") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">Title") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">RSS") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">NDBC Standard Meteorological Buoy Data") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf(">cwwcNDBCMet") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.json?" + 
                    EDStatic.defaultPIppQuery);
                Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"NDBC Standard Meteorological Buoy Data\"") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf("\"cwwcNDBCMet\"") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/documentation.html");            
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(
                    "available via ERDDAP's Sensor Observation Service (SOS) web service.") >= 0, 
                    "results=\n" + results);

                String sosUrl = EDStatic.erddapUrl + "/sos/cwwcNDBCMet/" + EDDTable.sosServer;
                results = SSR.getUrlResponseStringUnchanged(sosUrl + "?service=SOS&request=GetCapabilities");            
                Test.ensureTrue(results.indexOf("<ows:ServiceIdentification>") >= 0, "results=\n" + results);            
                Test.ensureTrue(results.indexOf("<ows:Get xlink:href=\"" + sosUrl + "?\"/>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("</Capabilities>") >= 0, "results=\n" + results);
            } else {
                results = "Shouldn't get here.";
                try {
                    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" + 
                        EDStatic.defaultPIppQuery);
                } catch (Throwable t) {
                    results = MustBe.throwableToString(t);
                }
expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/sos/index.html?page=1&itemsPerPage=1000\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: The \\\"SOS\\\" system has been disabled on this ERDDAP.\";\n" +
"})\n";
                Test.ensureEqual(results.substring(0, expected.length()), expected,
                    "results=\n" + results);            
            }


            //wcs
            if (EDStatic.wcsActive) {
                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" + 
                    EDStatic.defaultPIppQuery);
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Datasets Which Can Be Accessed via WCS") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">Title</th>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">RSS</th>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)</td>") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf(">erdMHchla8day<") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.json?" + 
                    EDStatic.defaultPIppQuery);
                Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\"") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/documentation.html");            
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(
                    "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.") >= 0, 
                    "results=\n" + results);

                String wcsUrl = EDStatic.erddapUrl + "/wcs/erdMHchla8day/" + EDDGrid.wcsServer;
                results = SSR.getUrlResponseStringUnchanged(wcsUrl + "?service=WCS&request=GetCapabilities");            
                Test.ensureTrue(results.indexOf("<CoverageOfferingBrief>") >= 0, "results=\n" + results);            
                Test.ensureTrue(results.indexOf("<lonLatEnvelope srsName") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("</WCS_Capabilities>") >= 0, "results=\n" + results);
            } else {
                //wcs is inactive
                results = "Shouldn't get here.";
                try {
                    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" + 
                        EDStatic.defaultPIppQuery);
                } catch (Throwable t) {
                    results = MustBe.throwableToString(t);
                }
                Test.ensureTrue(results.indexOf("java.io.FileNotFoundException: http://localhost:8080/cwexperimental/wcs/index.html?page=1&itemsPerPage=1000") >= 0, "results=\n" + results);            
            }

            //wms
            if (EDStatic.wmsActive) {
                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" + 
                    EDStatic.defaultPIppQuery);
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("List of WMS Datasets") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf(">Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\n") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf(">erdMHchla8day\n") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.json?" + 
                    EDStatic.defaultPIppQuery);
                Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("\"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\"") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/documentation.html");            
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("display of registered and superimposed map-like views") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/erdMHchla8day/index.html");            
                Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)") >= 0,
                    "results=\n" + results);            
                Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("on-the-fly by ERDDAP's") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("altitude") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);
            } else {
                results = "Shouldn't get here.";
                try {
                    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" + 
                        EDStatic.defaultPIppQuery);
                } catch (Throwable t) {
                    results = MustBe.throwableToString(t);
                }
                Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);            
            }

//            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
//                "/categorize/standard_name/index.html");
//            Test.ensureTrue(results.indexOf(">sea_water_temperature<") >= 0,
//                "results=\n" + results);

            //validate the various GetCapabilities documents
/* NOT ACTIVE
            String s = https://xmlvalidation.com/   ".../xml/validate/?lang=en" +
                "&url=" + EDStatic.erddapUrl + "/wms/" + EDD.WMS_SERVER + "?service=WMS&" +
                "request=GetCapabilities&version=";
            SSR.displayInBrowser(s + "1.1.0");
            SSR.displayInBrowser(s + "1.1.1");
            SSR.displayInBrowser(s + "1.3.0");
*/

            //more information
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/information.html");
            Test.ensureTrue(results.indexOf(
                "ERDDAP a solution to everyone's data distribution / data access problems?") >= 0,
                "results=\n" + results);

            //subscriptions
            if (EDStatic.subscriptionSystemActive) {
                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/subscriptions/index.html");
                Test.ensureTrue(results.indexOf("Add a new subscription") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Validate a subscription") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("List your subscriptions") >= 0, "results=\n" + results);
                Test.ensureTrue(results.indexOf("Remove a subscription") >= 0, "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/subscriptions/add.html");
                Test.ensureTrue(results.indexOf(
                    "To add a (another) subscription, please fill out this form:") >= 0, 
                    "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/subscriptions/validate.html");
                Test.ensureTrue(results.indexOf(
                    "To validate a (another) subscription, please fill out this form:") >= 0, 
                    "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/subscriptions/list.html");
                Test.ensureTrue(results.indexOf(
                    "To request an email with a list of your subscriptions, please fill out this form:") >= 0, 
                    "results=\n" + results);

                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/subscriptions/remove.html");
                Test.ensureTrue(results.indexOf(
                    "To remove a (another) subscription, please fill out this form:") >= 0, 
                    "results=\n" + results);
            } else {
                results = "Shouldn't get here.";
                try {
                    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                        "/subscriptions/index.html");
                } catch (Throwable t) {
                    results = MustBe.throwableToString(t);
                }
                Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);            
            }


            //slideSorter
            if (EDStatic.slideSorterActive) {
                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/slidesorter.html");
                Test.ensureTrue(results.indexOf(
                    "Your slides will be lost when you close this browser window, unless you:") >= 0, 
                    "results=\n" + results);
            } else {
                results = "Shouldn't get here.";
                try {
                    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                        "/slidesorter.html");
                } catch (Throwable t) {
                    results = MustBe.throwableToString(t);
                }
                Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0, "results=\n" + results);            
            }


            //NOT ACTIVE - google Gadgets (always at coastwatch)
            //results = SSR.getUrlResponseStringUnchanged(
            //    "https://coastwatch.pfeg.noaa.gov/erddap/images/gadgets/GoogleGadgets.html");
            //Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            //Test.ensureTrue(results.indexOf(
            //    "Google Gadgets with Graphs or Maps") >= 0, 
            //    "results=\n" + results);
            //Test.ensureTrue(results.indexOf(
            //    "are self-contained chunks of web content") >= 0, 
            //    "results=\n" + results);


            //embed a graph  (always at coastwatch)
            results = SSR.getUrlResponseStringUnchanged(
                "https://coastwatch.pfeg.noaa.gov/erddap/images/embed.html");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                "Embed a Graph in a Web Page") >= 0, 
                "results=\n" + results);

            //Computer Programs            
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/rest.html");
            Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(
                "ERDDAP's RESTful Web Services") >= 0,
                "results=\n" + results);

            //list of services
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.csv");
            expected = 
"Resource,URL\n" +
"info,http://localhost:8080/cwexperimental/info/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
"search,http://localhost:8080/cwexperimental/search/index.csv?" + EDStatic.defaultPIppQuery + "&searchFor=\n" +
"categorize,http://localhost:8080/cwexperimental/categorize/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
"griddap,http://localhost:8080/cwexperimental/griddap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
"tabledap,http://localhost:8080/cwexperimental/tabledap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
(EDStatic.sosActive? "sos,http://localhost:8080/cwexperimental/sos/index.csv?" + EDStatic.defaultPIppQuery + "\n" : "") +
(EDStatic.wcsActive? "wcs,http://localhost:8080/cwexperimental/wcs/index.csv?" + EDStatic.defaultPIppQuery + "\n" : "") +
(EDStatic.wmsActive? "wms,http://localhost:8080/cwexperimental/wms/index.csv?" + EDStatic.defaultPIppQuery + "\n" : "");
//subscriptions?
//converters?
            Test.ensureEqual(results, expected, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.htmlTable?" + 
                EDStatic.defaultPIppQuery);
            expected = 
EDStatic.startHeadHtml(EDStatic.erddapUrl((String)null), "Resources") + "\n" +
"</head>\n" +
EDStatic.startBodyHtml(null) + 
"&nbsp;<br>\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>Resource\n" +
"<th>URL\n" +
"</tr>\n" +
"<tr>\n" +
"<td>info\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;info&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:8080/cwexperimental/info/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>search\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;search&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000&#x26;searchFor&#x3d;\">http://localhost:8080/cwexperimental/search/index.htmlTable?page=1&amp;itemsPerPage=1000&amp;searchFor=</a>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>categorize\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;categorize&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:8080/cwexperimental/categorize/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>griddap\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;griddap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:8080/cwexperimental/griddap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>tabledap\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:8080/cwexperimental/tabledap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n" +
"</tr>\n" +
(EDStatic.sosActive?
"<tr>\n" +
"<td>sos\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;sos&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:8080/cwexperimental/sos/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n" +
"</tr>\n" : "") +
"<tr>\n" +
"<td>wms\n" +
"<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;cwexperimental&#x2f;wms&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:8080/cwexperimental/wms/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n" +
"</tr>\n" +
"</table>\n" +
EDStatic.endBodyHtml(EDStatic.erddapUrl((String)null)) + "\n" +
"</html>\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.json");
            expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"Resource\", \"URL\"],\n" +
"    \"columnTypes\": [\"String\", \"String\"],\n" +
"    \"rows\": [\n" +
"      [\"info\", \"http://localhost:8080/cwexperimental/info/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"search\", \"http://localhost:8080/cwexperimental/search/index.json?page=1&itemsPerPage=1000&searchFor=\"],\n" +
"      [\"categorize\", \"http://localhost:8080/cwexperimental/categorize/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"griddap\", \"http://localhost:8080/cwexperimental/griddap/index.json?page=1&itemsPerPage=1000\"],\n" +
"      [\"tabledap\", \"http://localhost:8080/cwexperimental/tabledap/index.json?page=1&itemsPerPage=1000\"]"            + (EDStatic.sosActive || EDStatic.wcsActive || EDStatic.wmsActive? "," : "") + "\n" +
(EDStatic.sosActive? "      [\"sos\", \"http://localhost:8080/cwexperimental/sos/index.json?page=1&itemsPerPage=1000\"]" + (EDStatic.wcsActive || EDStatic.wmsActive? "," : "") + "\n" : "") +
(EDStatic.wcsActive? "      [\"wcs\", \"http://localhost:8080/cwexperimental/wcs/index.json?page=1&itemsPerPage=1000\"]" + (EDStatic.wmsActive? "," : "") + "\n" : "") +
(EDStatic.wmsActive? "      [\"wms\", \"http://localhost:8080/cwexperimental/wms/index.json?page=1&itemsPerPage=1000\"]\n" : "") +
//subscriptions?
"    ]\n" +
"  }\n" +
"}\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.tsv"));
            expected = 
"Resource[9]URL[10]\n" +
"info[9]http://localhost:8080/cwexperimental/info/index.tsv?page=1&itemsPerPage=1000[10]\n" +
"search[9]http://localhost:8080/cwexperimental/search/index.tsv?page=1&itemsPerPage=1000&searchFor=[10]\n" +
"categorize[9]http://localhost:8080/cwexperimental/categorize/index.tsv?page=1&itemsPerPage=1000[10]\n" +
"griddap[9]http://localhost:8080/cwexperimental/griddap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
"tabledap[9]http://localhost:8080/cwexperimental/tabledap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
(EDStatic.sosActive? "sos[9]http://localhost:8080/cwexperimental/sos/index.tsv?page=1&itemsPerPage=1000[10]\n" : "") +
(EDStatic.wcsActive? "wcs[9]http://localhost:8080/cwexperimental/wcs/index.tsv?page=1&itemsPerPage=1000[10]\n" : "") +
(EDStatic.wmsActive? "wms[9]http://localhost:8080/cwexperimental/wms/index.tsv?page=1&itemsPerPage=1000[10]\n" : "") +
"[end]";
            Test.ensureEqual(results, expected, "results=\n" + results);

            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.xhtml");
            expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>Resources</title>\n" +
"  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/cwexperimental/images/erddap2.css\" />\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>Resource</th>\n" +
"<th>URL</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>info</td>\n" +
"<td>http://localhost:8080/cwexperimental/info/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>search</td>\n" +
"<td>http://localhost:8080/cwexperimental/search/index.xhtml?page=1&amp;itemsPerPage=1000&amp;searchFor=</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>categorize</td>\n" +
"<td>http://localhost:8080/cwexperimental/categorize/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>griddap</td>\n" +
"<td>http://localhost:8080/cwexperimental/griddap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>tabledap</td>\n" +
"<td>http://localhost:8080/cwexperimental/tabledap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" +
(EDStatic.sosActive?
"<tr>\n" +
"<td>sos</td>\n" +
"<td>http://localhost:8080/cwexperimental/sos/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" : "") +
(EDStatic.wcsActive?
"<tr>\n" +
"<td>wcs</td>\n" +
"<td>http://localhost:8080/cwexperimental/wcs/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" : "") +
(EDStatic.wmsActive? 
"<tr>\n" +
"<td>wms</td>\n" +
"<td>http://localhost:8080/cwexperimental/wms/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
"</tr>\n" : "") +
"</table>\n" +
"</body>\n" +
"</html>\n";
            Test.ensureEqual(results, expected, "results=\n" + results);


        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl); 
        }
    }

    /**
     * This test the json-ld responses from the ERDDAP at EDStatic.erddapUrl. 
     * It assumes jplMURSST41 datasets is available.
     *
     */
    public static void testJsonld() throws Throwable {
        Erddap.verbose = true;
        Erddap.reallyVerbose = true;
        EDD.testVerboseOn();
        String results, expected;
        String2.log("\n*** Erddap.testJsonld");
        int po;

        try {
            //info    list all datasets
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
                EDStatic.defaultPIppQuery); 

            //json-ld all datasets
            expected = 
"<script type=\"application/ld+json\">\n" +
"{\n" +
"  \"@context\": \"http://schema.org\",\n" +
"  \"@type\": \"DataCatalog\",\n" +
"  \"name\": \"ERDDAP Data Server at NOAA NMFS SWFSC Environmental Research Division\",\n" +
"  \"url\": \"https://localhost:8443/cwexperimental\",\n" +
"  \"publisher\": {\n" +
"    \"@type\": \"Organization\",\n" +
"    \"name\": \"NOAA NMFS SWFSC Environmental Research Division\",\n" +
"    \"address\": {\n" +
"      \"@type\": \"PostalAddress\",\n" +
"      \"addressCountry\": \"USA\",\n" +
"      \"addressLocality\": \"99 Pacific St, Suite 255A, Monterey\",\n" +
"      \"addressRegion\": \"CA\",\n" +
"      \"postalCode\": \"93940\"\n" +
"    },\n" +
"    \"telephone\": \"+1 831-420-3666\",\n" +
"    \"email\": \"bob.simons@noaa.gov\",\n" +
"    \"sameAs\": \"https://swfsc.noaa.gov/textblock.aspx?Division=ERD&id=1315&ParentMenuId=200\"\n" +
"  },\n" +
"  \"fileFormat\": [\n" +
"    \"application/geo+json\",\n" +
"    \"application/json\",\n" +
"    \"text/csv\"\n" +
"  ],\n" +
"  \"isAccessibleForFree\": \"True\",\n" +
"  \"dataset\": [\n" +
"    {\n" +
"      \"@type\": \"Dataset\",\n" +
"      \"name\": \"";
            po = Math.max(0, results.indexOf(expected.substring(0, 80)));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

expected = 
"    {\n" +
"      \"@type\": \"Dataset\",\n" +
"      \"name\": \"JPL MUR SST Images\",\n" +
"      \"sameAs\": \"https://localhost:8443/cwexperimental/info/testFileNames/index.html\"\n" +
"    }";
            po = Math.max(0, results.indexOf(expected.substring(0, 80)));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

            //json-ld 1 dataset
            results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/jplMURSST41/index.html"); 
expected = 
"<script type=\"application/ld+json\">\n" +
"{\n" +
"  \"@context\": \"http://schema.org\",\n" +
"  \"@type\": \"Dataset\",\n" +
"  \"name\": \"Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global, 0.01�, 2002-present, Daily\",\n" +
"  \"headline\": \"jplMURSST41\",\n" +
"  \"description\": \"This is a merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysi" +
"s product from Jet Propulsion Laboratory (JPL). This daily, global, Multi-scale, Ultra-high Resolution (MUR) Se" +
"a Surface Temperature (SST) 1-km data set, Version 4.1, is produced at JPL under the NASA MEaSUREs program. For" +
" details, see https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1 . This dataset is part of the Group for " +
"High-Resolution Sea Surface Temperature (GHRSST) project. The data for the most recent 7 days is usually revise" +
"d everyday.  The data for other days is sometimes revised.\\nacknowledgement=Please acknowledge the use of these " +
"data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.\\ncd" +
"m_data_type=Grid\\ncomment=MUR = \\\"Multi-scale Ultra-high Resolution\\\"\\nConventions=CF-1.6, COARDS, ACDD-1.3\\nEa" +
"sternmost_Easting=180.0\\nfile_quality_level=1\\ngds_version_id=2.0\\ngeospatial_lat_max=89.99\\ngeospatial_lat_min" +
"=-89.99\\ngeospatial_lat_resolution=0.01\\ngeospatial_lat_units=degrees_north\\ngeospatial_lon_max=180.0\\ngeospati" +
"al_lon_min=-179.99\\ngeospatial_lon_resolution=0.01\\ngeospatial_lon_units=degrees_east\\nhistory=created at nomin" +
"al 4-day latency; replaced nrt (1-day latency) version.\\nData is downloaded daily from " +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/ " +
"to NOAA NMFS SWFSC ERD by erd.data@noaa.gov .\\nThe data fo" +
"r the most recent 7 days is usually revised everyday. The data for other days is sometimes revised.\\nid=MUR-JPL" +
"-L4-GLOB-v04.1\\ninfoUrl=https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\\ninstitution=NASA JPL\\nkeyword" +
"s_vocabulary=GCMD Science Keywords\\nnaming_authority=org.ghrsst\\nnetcdf_version_id=4.1\\nNorthernmost_Northing=8" +
"9.99\\nplatform=Terra, Aqua, GCOM-W, NOAA-19, MetOp-A, Buoys/Ships\\nprocessing_level=L4\\nproject=NASA Making Ear" +
"th Science Data Records for Use in Research Environments (MEaSUREs) Program\\nreferences=https://podaac.jpl.nasa" +
".gov/Multi-scale_Ultra-high_Resolution_MUR-SST\\nsensor=MODIS, AMSR2, AVHRR, in-situ\\nsource=MODIS_T-JPL, MODIS_" +
"A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\\nsourceUrl=(local files" +
")\\nSouthernmost_Northing=-89.99\\nspatial_resolution=0.01 degrees\\nstandard_name_vocabulary=CF Standard Name Tab" +
//                                                end date changes   
"le v55\\ntestOutOfDate=now-3days\\ntime_coverage_end=2018-08-07T09:00:00Z\\ntime_coverage_start=2002-06-01T09:00:0" +
"0Z\\nWesternmost_Easting=-179.99\",\n" +
"  \"url\": \"https://localhost:8443/cwexperimental/griddap/jplMURSST41.html\",\n" +
"  \"includedInDataCatalog\": {\n" +
"    \"@type\": \"DataCatalog\",\n" +
"    \"name\": \"ERDDAP Data Server at NOAA NMFS SWFSC Environmental Research Division\",\n" +
"    \"sameAs\": \"https://localhost:8443/cwexperimental\"\n" +
"  },\n" +
"  \"keywords\": [\n" +
"    \"analysed\",\n" +
"    \"analysed_sst\",\n" +
"    \"analysis\",\n" +
"    \"analysis_error\",\n" +
"    \"area\",\n" +
"    \"binary\",\n" +
"    \"composite\",\n" +
"    \"daily\",\n" +
"    \"data\",\n" +
"    \"day\",\n" +
"    \"deviation\",\n" +
"    \"distribution\",\n" +
"    \"Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature\",\n" +
"    \"error\",\n" +
"    \"estimated\",\n" +
"    \"field\",\n" +
"    \"final\",\n" +
"    \"foundation\",\n" +
"    \"fraction\",\n" +
"    \"ghrsst\",\n" +
"    \"high\",\n" +
"    \"ice\",\n" +
"    \"ice distribution\",\n" +
"    \"identifier\",\n" +
"    \"jet\",\n" +
"    \"jpl\",\n" +
"    \"laboratory\",\n" +
"    \"land\",\n" +
"    \"land_binary_mask\",\n" +
"    \"mask\",\n" +
"    \"multi\",\n" +
"    \"multi-scale\",\n" +
"    \"mur\",\n" +
"    \"nasa\",\n" +
"    \"ocean\",\n" +
"    \"oceans\",\n" +
"    \"product\",\n" +
"    \"propulsion\",\n" +
"    \"resolution\",\n" +
"    \"scale\",\n" +
"    \"sea\",\n" +
"    \"sea ice area fraction\",\n" +
"    \"sea/land\",\n" +
"    \"sea_ice_fraction\",\n" +
"    \"sea_surface_foundation_temperature\",\n" +
"    \"sst\",\n" +
"    \"standard\",\n" +
"    \"statistics\",\n" +
"    \"surface\",\n" +
"    \"temperature\",\n" +
"    \"time\",\n" +
"    \"ultra\",\n" +
"    \"ultra-high\"\n" +
"  ],\n" +
"  \"license\": \"These data are available free of charge under the JPL PO.DAAC data policy.\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\",\n" +
"  \"variableMeasured\": [\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"time\",\n" +
"      \"alternateName\": \"reference time of sst field\",\n" +
"      \"description\": \"reference time of sst field\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"axis\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_CoordinateAxisType\",\n" +
"          \"value\": \"Time\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axis\",\n" +
"          \"value\": \"T\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"comment\",\n" +
"          \"value\": \"Nominal time of analyzed fields\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Time\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"reference time of sst field\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"standard_name\",\n" +
"          \"value\": \"time\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"time_origin\",\n" +
"          \"value\": \"01-JAN-1970 00:00:00\"\n" +
"        }\n" +
"      ],\n" +
"      \"maxValue\": \"2018-08-07T09:00:00Z\",\n" +  //changes
"      \"minValue\": \"2002-06-01T09:00:00Z\",\n" +
"      \"propertyID\": \"time\"\n" +
"    },\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"latitude\",\n" +
"      \"alternateName\": \"Latitude\",\n" +
"      \"description\": \"Latitude\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"axis\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_CoordinateAxisType\",\n" +
"          \"value\": \"Lat\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axis\",\n" +
"          \"value\": \"Y\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Location\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"Latitude\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"standard_name\",\n" +
"          \"value\": \"latitude\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_max\",\n" +
"          \"value\": 90\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_min\",\n" +
"          \"value\": -90\n" +
"        }\n" +
"      ],\n" +
"      \"maxValue\": 89.99,\n" +
"      \"minValue\": -89.99,\n" +
"      \"propertyID\": \"latitude\",\n" +
"      \"unitText\": \"degrees_north\"\n" +
"    },\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"longitude\",\n" +
"      \"alternateName\": \"Longitude\",\n" +
"      \"description\": \"Longitude\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"axis\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_CoordinateAxisType\",\n" +
"          \"value\": \"Lon\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axis\",\n" +
"          \"value\": \"X\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Location\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"Longitude\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"standard_name\",\n" +
"          \"value\": \"longitude\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_max\",\n" +
"          \"value\": 180\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_min\",\n" +
"          \"value\": -180\n" +
"        }\n" +
"      ],\n" +
"      \"maxValue\": 180,\n" +
"      \"minValue\": -179.99,\n" +
"      \"propertyID\": \"longitude\",\n" +
"      \"unitText\": \"degrees_east\"\n" +
"    },\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"analysed_sst\",\n" +
"      \"alternateName\": \"Analysed Sea Surface Temperature\",\n" +
"      \"description\": \"Analysed Sea Surface Temperature\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"data\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_FillValue\",\n" +
"          \"value\": -7.768000000000001\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMaximum\",\n" +
"          \"value\": 32\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMinimum\",\n" +
"          \"value\": 0\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"comment\",\n" +
"          \"value\": \"Interim near-real-time (nrt) version using Multi-Resolution Variational Analysis (MRVA) method for interpolation; to be replaced by Final version\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Temperature\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"Analysed Sea Surface Temperature\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"source\",\n" +
"          \"value\": \"MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"standard_name\",\n" +
"          \"value\": \"sea_surface_foundation_temperature\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_max\",\n" +
"          \"value\": null\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_min\",\n" +
"          \"value\": -7.767000000000003\n" +
"        }\n" +
"      ],\n" +
"      \"propertyID\": \"sea_surface_foundation_temperature\",\n" +
"      \"unitText\": \"degree_C\"\n" +
"    },\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"analysis_error\",\n" +
"      \"alternateName\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
"      \"description\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"data\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_FillValue\",\n" +
"          \"value\": -327.68\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMaximum\",\n" +
"          \"value\": 5\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMinimum\",\n" +
"          \"value\": 0\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Statistics\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"Estimated Error Standard Deviation of analysed_sst\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_max\",\n" +
"          \"value\": null\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_min\",\n" +
"          \"value\": 0\n" +
"        }\n" +
"      ],\n" +
"      \"unitText\": \"degree_C\"\n" +
"    },\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"mask\",\n" +
"      \"alternateName\": \"Sea/Land Field Composite Mask\",\n" +
"      \"description\": \"Sea/Land Field Composite Mask\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"data\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_FillValue\",\n" +
"          \"value\": -128\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMaximum\",\n" +
"          \"value\": 20\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMinimum\",\n" +
"          \"value\": 0\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"comment\",\n" +
"          \"value\": \"mask can be used to further filter the data.\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"flag_masks\",\n" +
"          \"value\": 1\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"flag_meanings\",\n" +
"          \"value\": \"1=open-sea, 2=land, 5=open-lake, 9=open-sea with ice in the grid, 13=open-lake with ice in the grid\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"flag_values\",\n" +
"          \"value\": 1\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Identifier\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"Sea/Land Field Composite Mask\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"source\",\n" +
"          \"value\": \"GMT \\\"grdlandmask\\\", ice flag from sea_ice_fraction data\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"standard_name\",\n" +
"          \"value\": \"land_binary_mask\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_max\",\n" +
"          \"value\": 31\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_min\",\n" +
"          \"value\": 1\n" +
"        }\n" +
"      ],\n" +
"      \"propertyID\": \"land_binary_mask\"\n" +
"    },\n" +
"    {\n" +
"      \"@type\": \"PropertyValue\",\n" +
"      \"name\": \"sea_ice_fraction\",\n" +
"      \"alternateName\": \"Sea Ice Area Fraction\",\n" +
"      \"description\": \"Sea Ice Area Fraction\",\n" +
"      \"valueReference\": [\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"axisOrDataVariable\",\n" +
"          \"value\": \"data\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"_FillValue\",\n" +
"          \"value\": -1.28\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMaximum\",\n" +
"          \"value\": 1\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"colorBarMinimum\",\n" +
"          \"value\": 0\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"comment\",\n" +
"          \"value\": \"ice data interpolated by a nearest neighbor approach.\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"ioos_category\",\n" +
"          \"value\": \"Ice Distribution\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"long_name\",\n" +
"          \"value\": \"Sea Ice Area Fraction\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"source\",\n" +
"          \"value\": \"EUMETSAT OSI-SAF, copyright EUMETSAT\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"standard_name\",\n" +
"          \"value\": \"sea_ice_area_fraction\"\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_max\",\n" +
"          \"value\": 1\n" +
"        },\n" +
"        {\n" +
"          \"@type\": \"PropertyValue\",\n" +
"          \"name\": \"valid_min\",\n" +
"          \"value\": 0\n" +
"        }\n" +
"      ],\n" +
"      \"propertyID\": \"sea_ice_area_fraction\",\n" +
"      \"unitText\": \"1\"\n" +
"    }\n" +
"  ],\n" +
"  \"creator\": {\n" +
"    \"@type\": \"Organization\",\n" +
"    \"name\": \"JPL MUR SST project\",\n" +
"    \"email\": \"ghrsst@podaac.jpl.nasa.gov\",\n" +
"    \"sameAs\": \"https://mur.jpl.nasa.gov\"\n" +
"  },\n" +
"  \"publisher\": {\n" +
"    \"@type\": \"Organization\",\n" +
"    \"name\": \"GHRSST Project Office\",\n" +
"    \"email\": \"ghrsst-po@nceo.ac.uk\",\n" +
"    \"sameAs\": \"https://www.ghrsst.org\"\n" +
"  },\n" +
"  \"dateCreated\": \"2018-08-08T02:38:31Z\",\n" +  //changes
"  \"identifier\": \"org.ghrsst/MUR-JPL-L4-GLOB-v04.1\",\n" +
"  \"version\": \"04.1nrt\",\n" +
"  \"temporalCoverage\": \"2002-06-01T09:00:00Z/2018-08-07T09:00:00Z\",\n" + //end date changes
"  \"spatialCoverage\": {\n" +
"    \"@type\": \"Place\",\n" +
"    \"geo\": {\n" +
"      \"@type\": \"GeoShape\",\n" +
"      \"box\": \"-89.99 -179.99 89.99 180\"\n" +
"    }\n" +
"  }\n" +
"}\n" +
"</script>\n";
            po = Math.max(0, results.indexOf(expected.substring(0, 80)));
            Test.ensureEqual(results.substring(po, Math.min(results.length(), po + expected.length())), 
                expected, "results=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl); 
        }
    }

    /** This repeatedly gets the info/index.html web page and ensures it is without error. 
     *  It is best to run this when many datasets are loaded. 
     *  For a harder test: run this in 4 threads simultaneously. */
    public static void testHammerGetDatasets() throws Throwable {
        Erddap.verbose = true;
        Erddap.reallyVerbose = true;
        EDD.testVerboseOn();
        String results, expected;
        String2.log("\n*** Erddap.testHammerGetDatasets");
        int count = -5; //let it warm up
        long sumTime = 0;

        try {
            while (true) {
                if (count == 0) sumTime = 0;
                sumTime -= System.currentTimeMillis();
                //if uncompressed, it is 1Thread=280 4Threads=900ms
                results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                    "/info/index.html?" + EDStatic.defaultPIppQuery); 
                //if compressed, it is 1Thread=1575 4=Threads=5000ms
                //results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + 
                //    "/info/index.html?" + EDStatic.defaultPIppQuery); 
                sumTime += System.currentTimeMillis();
                count++;
                if (count > 0) String2.log("count=" + count + " AvgTime=" + (sumTime / count));
                expected = "List of All Datasets";
                Test.ensureTrue(results.indexOf(expected) >= 0, 
                    "results=\n" + results.substring(0, Math.min(results.length(), 5000)));
                expected = "dataset(s)";
                Test.ensureTrue(results.indexOf(expected) >= 0,
                    "results=\n" + results.substring(0, Math.min(results.length(), 5000)));
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }
    }

    /**
     * This is used by Bob to do simple tests of the basic Erddap services 
     * from the ERDDAP at EDStatic.erddapUrl. It assumes Bob's test datasets are available.
     *
     */
    public static void test() throws Throwable {
/* for releases, this line should have open/close comment */
        testBasic();
        testJsonld();
    }

}



