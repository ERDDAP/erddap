/* 
 * OutputStreamFromHttpResponse Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;

import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * OutputStreamFromHttpResponse provides an OutputStream upon request.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-01
 */
public class OutputStreamFromHttpResponse implements OutputStreamSource {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    public static String HTML_MIME_TYPE = "text/html";
    public static String KML_MIME_TYPE  = "application/vnd.google-earth.kml+xml";

    private HttpServletRequest request;
    private HttpServletResponse response;
    private String fileName;
    private String fileType;
    private String extension;
    private String usingCompression = ""; //not yet set
    private OutputStream outputStream;
    private boolean hasRangeRequest;

    /**
     * The constructor.
     *
     * @param tRequest  information is extracted from the header of the request
     * @param tResponse the outputStream is created from/for the response
     * @param tFileName without the directory or extension.
     *    This is only used for fileExtensions that encourage the client to 
     *    save the contents in a file (e.g., .csv).
     * @param tFileType The ERDDAP extension e.g., .esriAscii.
     *    In a few cases, more than one fileType (e.g., .asc and .esriAscii) 
     *    convert to the same actual extension (i.e., tExtension) (e.g., .asc).
     * @param tExtension the actual standard web file type extension 
     *    (called fileTypeExtension in ERDDAP, e.g., .asc) for the output
     */
    public OutputStreamFromHttpResponse(HttpServletRequest tRequest,
        HttpServletResponse tResponse, String tFileName, String tFileType, 
        String tExtension) {

        request = tRequest;
        response = tResponse;
        fileName = tFileName;
        fileType = tFileType;
        extension = tExtension;

        hasRangeRequest = request.getHeader("Range") != null; 
    }


    public OutputStream outputStream(String characterEncoding) throws Throwable {
        return outputStream(characterEncoding, -1);
    }
   
    /**
     * This returns the OutputStream.
     * If called repeatedly, this returns the same outputStream.
     *
     * <p>This never calls httpServletResponse.setContentLength 
     * ("Content-Length") because the length isn't as expected (the file's length) 
     * if I compress the response or if I send a Byte Range response.
     * An incorrectly specified Content-Length screws things up --  
     *   Firefox and Chrome wait 20 seconds before giving up.  
     * See it esp with full page reload: Ctrl Reload in Chrome.
     * See it with Performance recording in Firefox:
     *   In both cases .html loads quickly 
     *   but related resources take 20 seconds (almost exactly).  
     * There is no need to set Content-Length (except when dealing with Byte Ranges --
     * see the 2nd Erddap.doTransfer()).
     * NOTE: if the request has a Range request (hasRangeRequest), this won't
     * ever compress the response.
     * See usage of hasRangeRequest in this class.
     *
     * @param characterEncoding e.g., "" (for none specified), String2.UTF_8, or "" (for DAP).
     *     This parameter only matters the first time this method is called.
     *     This only matters for some subclasses.
     * @param tLength The length of the entire file for the response, if known 
     *     (else -1).  Currently, this is not used.
     * @return outputStream
     * @throws Throwable if trouble
     */
    public OutputStream outputStream(String characterEncoding, long tLength) 
        throws Throwable {

        if (outputStream != null) 
            return outputStream;

        //do all the things associated with committing to writing data to the outputStream
        //setContentType(mimeType)
        //User won't want hassle of saving file (changing fileType, ...).

        //see mime type list at http://www.webmaster-toolkit.com/mime-types.shtml
        // or http://html.megalink.com/programmer/pltut/plMimeTypes.html
        // or http://www.freeformatter.com/mime-types-list.html#mime-types-list
        String extensionLC = extension.toLowerCase();
        boolean genericCompressed = false;  //true for generic compressed files, e.g., .zip
        boolean otherCompressed = false;    //true for app specific compressed (but not audio/ image/ video)

        if (extension.equals(".3gp")) {
            response.setContentType("video/3gpp");  

        } else if (extension.equals(".7z")) {
            response.setContentType("application/x-7z-compressed"); 
            genericCompressed = true;

        } else if (extension.equals(".ai")) {
            response.setContentType("application/postscript"); 

        } else if (extension.equals(".aif") ||
                   extension.equals(".aiff") ||
                   extension.equals(".aifc")) {
            response.setContentType("audio/x-aiff"); 

        } else if (extension.equals(".asc")) { 
            //There are a couple of fileNameTypes that lead to .asc.
            //If DODS, ...
            if (fileType.equals(".asc"))
                response.setHeader("content-description", "dods_data"); //DAP 2.0, 7.1.1
            else if (fileType.equals(".timeGaps"))
                response.setHeader("content-description", "time_gap_information");
            response.setContentType("text/plain"); 

        } else if (extension.equals(".au")) {
            response.setContentType("audio/basic"); 

        } else if (extension.equals(".bin")) {
            response.setContentType("application/octet-stream"); 

        } else if (extension.equals(".bmp")) {
            response.setContentType("image/bmp"); 

        } else if (extension.equals(".bz")) {
            response.setContentType("application/x-bzip"); 
            genericCompressed = true;

        } else if (extension.equals(".bz2")) {
            response.setContentType("application/x-bzip2"); 
            genericCompressed = true;

        } else if (extension.equals(".chm")) {
            response.setContentType("application/vnd.ms-htmlhelp"); 

        } else if (extension.equals(".css")) {
            response.setContentType("text/css"); 

        } else if (extension.equals(".csv")) {
            response.setContentType("text/csv"); 

        } else if (extension.equals(".das")) {
            response.setContentType("text/plain");
            response.setHeader("content-description", "dods_das"); //DAP 2.0, 7.1.1  ???!!!DConnect has 'c' 'd', BUT spec has 'C' 'D'
            
        } else if (extension.equals(".dds")) {
            response.setContentType("text/plain");
            response.setHeader("content-description", "dods_dds"); //DAP 2.0, 7.1.1

        } else if (extension.equals(".der")) {
            response.setContentType("application/x-x509-ca-cert"); 
         
        } else if (extension.equals(".doc")) {
            response.setContentType("application/msword"); 

        } else if (extension.equals(".docx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"); 
            otherCompressed = true;

        } else if (extension.equals(".dods")) {
            //see dods.servlet.DODSServlet.doGetDODS for example
            response.setContentType("application/octet-stream");
            response.setHeader("content-description", "dods_data"); //DAP 2.0, 7.1.1

        } else if (extension.equals(".dotx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.template"); 

        } else if (extension.equals(".f4v")) {
            response.setContentType("video/x-f4v");                                 

        } else if (extension.equals(".flac")) {
            response.setContentType("audio/flac");     

        } else if (extension.equals(".flv")) {
            response.setContentType("video/x-flv");                                 

        } else if (extension.equals(".gif")) {
            response.setContentType("image/gif");                                 

        } else if (extension.equals(".gtar")) {
            response.setContentType("application/x-gtar");                                 
            genericCompressed = true;

        } else if (extension.equals(".gz")) {
            response.setContentType("application/x-gzip");                                 
            genericCompressed = true;

        } else if (extension.equals(".gzip")) {
            response.setContentType("application/x-gzip");                                 
            genericCompressed = true;

        } else if (extension.equals(".h261")) {
            response.setContentType("video/h261");                                 

        } else if (extension.equals(".h263")) {
            response.setContentType("video/h263");                                 

        } else if (extension.equals(".h264")) {
            response.setContentType("video/h264");                                 

        } else if (extension.equals(".hdf")) { 
            response.setContentType("application/x-hdf"); 

        } else if (extension.equals(".html")) { //ERDDAP always writes as String2.UTF_8
            response.setContentType(HTML_MIME_TYPE); 

        } else if (extension.equals(".ief")) { 
            response.setContentType("image/ief"); 

        } else if (extension.equals(".jar")) { 
            response.setContentType("application/java-archive");  
            otherCompressed = true;

        } else if (extension.equals(".jpg") ||
                   extension.equals(".jpeg")) {
            response.setContentType("image/jpeg");                                 

        } else if (extension.equals(".jpgv")) {
            response.setContentType("video/jpeg");                                 

        } else if (extension.equals(".js")) { 
            response.setContentType("application/x-javascript"); 

        } else if (extension.equals(".json")) { 
            response.setContentType(
                fileType.equals(".jsonp")?  //psuedo fileType
                    "application/javascript" : //see https://stackoverflow.com/questions/477816/what-is-the-correct-json-content-type
                fileType.equals(".jsonText")? 
                    "text/plain" : //ESRI Geoservices REST uses this
                    "application/json"); //http://dret.net/biblio/reference/rfc4627

        } else if (extension.equals(".jsonl")) { 
            response.setContentType(
                fileType.equals(".jsonp")?  //psuedo fileType
                    "application/javascript" : //see https://stackoverflow.com/questions/477816/what-is-the-correct-json-content-type
                    "application/x-jsonlines");  //no definitive answer. https://github.com/wardi/jsonlines/issues/9  I like x-jsonlines because it is descriptive.

        } else if (extension.equals(".kml")) {
            //see https://developers.google.com/kml/documentation/kml_tut
            //which lists both of these content types (in different places)
            //application/keyhole is used by the pydap example that works
            //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
            //response.setContentType("application/vnd.google-earth.kml+xml"); 
            //Opera says handling program is "Opera"!  So I manually added this mime type to Opera.
            response.setContentType(KML_MIME_TYPE); 

        } else if (extension.equals(".kmz")) {
            response.setContentType("application/vnd.google-earth.kmz"); 
            otherCompressed = true;

        } else if (extension.equals(".latex")) {
            response.setContentType("application/x-latex"); 

        } else if (extension.equals(".lha")) {
            response.setContentType("application/lha"); 
            genericCompressed = true;

        } else if (extension.equals(".lzh")) {
            response.setContentType("application/x-lzh"); 
            genericCompressed = true;

        } else if (extension.equals(".lzma")) {
            response.setContentType("application/x-lzma"); 
            genericCompressed = true;

        } else if (extension.equals(".lzx")) {
            response.setContentType("application/x-lzx"); 
            genericCompressed = true;

        } else if (extension.equals(".log")) {
            response.setContentType("text/plain"); 

        } else if (extension.equals(".m21")) {
            response.setContentType("application/mp21"); 

        } else if (extension.equals(".mdb")) {
            response.setContentType("application/x-msaccess"); 

        } else if (extension.equals(".mid")) {
            response.setContentType("audio/midi"); 

        } else if (extension.equals(".mov")) {
            response.setContentType("video/quicktime"); 

        } else if (extension.equals(".movie")) {
            response.setContentType("video/x-sgi-movie"); 

        } else if (extension.equals(".mpeg")) {
            response.setContentType("video/mpeg"); 

        } else if (extension.equals(".mpg")) {
            response.setContentType("audio/mpeg"); 

        } else if (extension.equals(".mpga")) {
            response.setContentType("audio/mpeg"); 

        } else if (extension.equals(".mp3")) {
            response.setContentType("audio/mpeg3"); 

        } else if (extension.equals(".mp4a")) {
            response.setContentType("audio/mp4"); 

        } else if (extension.equals(".mp4")) {
            response.setContentType("video/mp4"); 

        } else if (extension.equals(".mpp")) {
            response.setContentType("application/vnd.ms-project"); 

        } else if (extension.equals(".nc")) {
            response.setContentType("application/x-netcdf"); 

        } else if (extension.equals(".odb")) {
            response.setContentType("application/vnd.oasis.opendocument.database"); 

        } else if (extension.equals(".odc")) {
            response.setContentType("application/vnd.oasis.opendocument.chart"); 

        } else if (extension.equals(".otc")) {
            response.setContentType("application/vnd.oasis.opendocument.chart-template");

        } else if (extension.equals(".odf")) {
            response.setContentType("application/vnd.oasis.opendocument.formula"); 

        } else if (extension.equals(".odg")) {
            response.setContentType("application/vnd.oasis.opendocument.graphics"); 

        } else if (extension.equals(".odi")) {
            response.setContentType("application/vnd.oasis.opendocument.image"); 

        } else if (extension.equals(".odp")) {
            response.setContentType("application/vnd.oasis.opendocument.presentation"); 

        } else if (extension.equals(".ods")) {
            response.setContentType("application/vnd.oasis.opendocument.spreadsheet"); 
            //response.setContentType("application/oda"); 

        } else if (extension.equals(".odt")) {
            response.setContentType("application/vnd.oasis.opendocument.text"); 

        } else if (extension.equals(".oga")) {
            response.setContentType("audio/ogg"); 

        } else if (extension.equals(".ogg")) {
            response.setContentType("audio/ogg"); 

        } else if (extension.equals(".ogv")) {
            response.setContentType("video/ogg"); 

        } else if (extension.equals(".ogx")) {
            response.setContentType("application/ogg"); 
            otherCompressed = true;

        } else if (extension.equals(".onetoc")) {
            response.setContentType("application/onenote"); 

        } else if (extension.equals(".opf")) {
            response.setContentType("application/oebps-package+xm"); 

        } else if (extension.equals(".otf")) {
            response.setContentType("application/x-font-otf"); 

        } else if (extension.equals(".otg")) {
            response.setContentType("application/vnd.oasis.opendocument.graphics-template"); 

        } else if (extension.equals(".oth")) {
            response.setContentType("application/vnd.oasis.opendocument.text-web"); 

        } else if (extension.equals(".oti")) {
            response.setContentType("application/vnd.oasis.opendocument.image-template"); 

        } else if (extension.equals(".otp")) {
            response.setContentType("application/vnd.oasis.opendocument.presentation-template"); 

        } else if (extension.equals(".ots")) {
            response.setContentType("application/vnd.oasis.opendocument.spreadsheet-template"); 

        } else if (extension.equals(".pbm")) {
            response.setContentType("image/x-portable-bitmap"); 

        } else if (extension.equals(".pcx")) {
            response.setContentType("image/x-pcx"); 

        } else if (extension.equals(".pdf")) {
            response.setContentType("application/pdf"); 

        } else if (extension.equals(".pfr")) {
            response.setContentType("application/font-tdpfr"); 

        } else if (extension.equals(".pgm")) {
            response.setContentType("image/x-portable-graymap"); 

        } else if (extension.equals(".pic") ||
                   extension.equals(".pict")) {
            response.setContentType("image/pict"); 

        } else if (extension.equals(".png")) {
            response.setContentType("image/png"); 

        } else if (extension.equals(".pnm")) {
            response.setContentType("image/x-portable-anymap"); 

        } else if (extension.equals(".pot")) {
            response.setContentType("application/mspowerpoint"); 

        } else if (extension.equals(".ppm")) {
            response.setContentType("image/x-portable-pixmap"); 

        } else if (extension.equals(".potx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.template"); 

        } else if (extension.equals(".ppsm")) {
            response.setContentType("application/vnd.ms-powerpoint.slideshow.macroenabled.12"); 

        } else if (extension.equals(".ppsx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.slideshow"); 

        } else if (extension.equals(".ppt")) {
            response.setContentType("application/mspowerpoint"); 

        } else if (extension.equals(".pptm")) {
            response.setContentType("application/vnd.ms-powerpoint.presentation.macroenabled.12"); 

        } else if (extension.equals(".pptx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation"); 

        } else if (extension.equals(".psd")) {
            response.setContentType("image/vnd.adobe.photoshop"); 

        } else if (extension.equals(".pub")) {
            response.setContentType("application/x-mspublisher"); 

        } else if (extension.equals(".qt")) {
            response.setContentType("video/quicktime"); 

        } else if (extension.equals(".py")) {
            response.setContentType("text/x-script.phyton"); 

        } else if (extension.equals(".pyc")) {
            response.setContentType("application/x-bytecode.python"); 

        } else if (extension.equals(".qt")) {
            response.setContentType("video/quicktime"); 

        } else if (extension.equals(".ra")) {
            response.setContentType("audio/x-realaudio"); 

        } else if (extension.equals(".ram")) {
            response.setContentType("audio/x-pn-realaudio"); 

        } else if (extension.equals(".rar")) {
            response.setContentType("application/x-rar-compressed"); 
            otherCompressed = true;

        } else if (extension.equals(".rgb")) {
            response.setContentType("image/x-rgb"); 

        } else if (extension.equals(".rm")) {
            response.setContentType("application/vnd.rn-realmedia"); 
            otherCompressed = true;

        } else if (extension.equals(".rq")) {
            response.setContentType("application/sparql-query"); 

        } else if (extension.equals(".rt")) {
            response.setContentType("text/richtext"); 

        } else if (extension.equals(".rtf")) {
            response.setContentType("application/rtf"); 

        } else if (extension.equals(".rtx")) {
            response.setContentType("text/richtext"); 

        } else if (extension.equals(".rss")) {
            response.setContentType("application/rss+xml"); 

        } else if (extension.equals(".sbml")) {
            response.setContentType("application/sbml+xml"); 

        } else if (extension.equals(".scd")) {
            response.setContentType("application/x-msschedule"); 

        } else if (extension.equals(".sgml")) {
            response.setContentType("text/sgml"); 

        } else if (extension.equals(".sldm")) {
            response.setContentType("application/vnd.ms-powerpoint.slide.macroenabled.12"); 

        } else if (extension.equals(".sldx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.slide"); 

        } else if (extension.equals(".srx")) {
            response.setContentType("application/sparql-results+xml"); 

        } else if (extension.equals(".ssml")) {
            response.setContentType("application/ssml+xml"); 

        } else if (extension.equals(".stc")) {
            response.setContentType("application/vnd.sun.xml.calc.template"); 

        } else if (extension.equals(".std")) {
            response.setContentType("application/vnd.sun.xml.draw.template"); 

        } else if (extension.equals(".sti")) {
            response.setContentType("application/vnd.sun.xml.impress.template"); 

        } else if (extension.equals(".stw")) {
            response.setContentType("application/vnd.sun.xml.writer.template"); 

        } else if (extension.equals(".svg")) {
            response.setContentType("image/svg+xml"); 

        } else if (extension.equals(".sxc")) {
            response.setContentType("application/vnd.sun.xml.calc"); 

        } else if (extension.equals(".sxd")) {
            response.setContentType("application/vnd.sun.xml.draw"); 

        } else if (extension.equals(".sxi")) {
            response.setContentType("application/vnd.sun.xml.impress"); 

        } else if (extension.equals(".sxm")) {
            response.setContentType("application/vnd.sun.xml.math"); 

        } else if (extension.equals(".sxw")) {
            response.setContentType("application/vnd.sun.xml.writer"); 

        } else if (extension.equals(".sxg")) {
            response.setContentType("application/vnd.sun.xml.writer.global"); 

        } else if (extension.equals(".tar")) {
            response.setContentType("application/x-tar"); 
            genericCompressed = true;
         
        } else if (extension.equals(".tcl")) {
            response.setContentType("application/x-tcl"); 
         
        } else if (extension.equals(".tei")) {
            response.setContentType("application/tei+xml"); 
         
        } else if (extension.equals(".tex")) {
            response.setContentType("application/x-tex"); 
         
        } else if (extension.equals(".tfm")) {
            response.setContentType("application/x-tex-tfm"); 
         
        } else if (extension.equals(".tif") ||
                   extension.equals(".tiff")) {
            response.setContentType("image/tiff"); 
         
        } else if (extension.equals(".tsv")) {
            response.setContentType("text/tab-separated-values"); 
         
        } else if (extension.equals(".ttf")) {
            response.setContentType("application/x-font-ttf"); 
         
        } else if (extension.equals(".ttl")) {
            response.setContentType("text/turtle"); 
         
        } else if (extension.equals(".txt")) {
            //ODV uses .txt but doesn't have a distinct mime type (as far as I can tell) see 2010-06-15 notes
            response.setContentType("text/plain"); 

        } else if (extension.equals(".uoml")) {
            response.setContentType("application/vnd.uoml+xml"); 
         
        } else if (extension.equals(".ufd")) {
            response.setContentType("application/vnd.ufdl"); 
         
        } else if (extension.equals(".vcf")) {
            response.setContentType("text/x-vcard"); 
         
        } else if (extension.equals(".vcs")) {
            response.setContentType("text/x-vcalendar"); 
         
        } else if (extension.equals(".vsd")) {
            response.setContentType("application/vnd.visio"); 
         
        } else if (extension.equals(".vxml")) {
            response.setContentType("application/voicexml+xml"); 
         
        } else if (extension.equals(".war")) {
            response.setContentType("application/zip.war"); 
            genericCompressed = true;
            
        } else if (extension.equals(".wav") || 
                   extension.equals(".wave")) {
            response.setContentType("audio/wav"); //I've seen audio/x-wav
         
        } else if (extension.equals(".weba")) {
            response.setContentType("audio/webm"); 

        } else if (extension.equals(".webm")) {
            response.setContentType("video/webm"); 

        } else if (extension.equals(".webp")) {
            response.setContentType("image/webp"); 
         
        } else if (extension.equals(".wm")) {
            response.setContentType("video/x-ms-wm"); 
         
        } else if (extension.equals(".wma")) {
            response.setContentType("audio/x-ms-wma"); 
         
        } else if (extension.equals(".wmv")) {
            response.setContentType("video/x-ms-wmv"); 
         
        } else if (extension.equals(".wps")) {
            response.setContentType("application/vnd.ms-works"); 
         
        } else if (extension.equals(".wri")) {
            response.setContentType("application/x-mswrite"); 
         
        } else if (extension.equals(".wsdl")) {
            response.setContentType("application/wsdl+xml"); 
         
        } else if (extension.equals(".xbm")) {
            response.setContentType("image/x-xbitmap"); 
         
        } else if (extension.equals(".xhtml")) { 
            //PROBLEM: MS Internet Explorer doesn't display .xhtml files.
            //It just shows endless series of "Save As..." dialog boxes.
            //"application/xhtml+xml" is proper mime type, 
            //  see http://keystonewebsites.com/articles/mime_type.php
            //Lots of web sites serve xhtml successfully using this
            //  e.g., in firefox, see Tools : Page Info for 
            //  http://www.w3.org/MarkUp/Forms/2003/xforms-for-html-authors
            //see http://www.w3.org/TR/xhtml1/#guidelines
            //But they do something else.
            //
            //I did:
            //"<p>XHTML and Internet Explorer - Attempts to view .xhtml files in Internet Explorer on Windows XP \n" +
            //"<br>often leads to an endless series of \"Save As...\" dialog boxes. The problem seems to be that \n" +
            //"<br>Windows XP has no registry entry for the standard XHTML mime type: application/xhtml+xml.\n" +
            //"<br>See <a href=\"http://www.peterprovost.org/archive/2004/10/22/2003.aspx\">this blog</a> for a possible solution.\n" +
            //But that is not a good solution for ERDDAP clients (too risky).
            //
            //SOLUTION from http://www.ibm.com/developerworks/xml/library/x-tipapachexhtml/index.html
            //if request is from Internet Explorer, use mime type text/html.
            String userAgent = request.getHeader("user-agent"); //case-insensitive
            if (userAgent != null && userAgent.indexOf("MSIE") >= 0) {
                 response.setContentType("text/html"); //the hack
                 if (verbose) String2.log(".xhtml request from user-agent=MSIE: using mime=text/html");
            } else response.setContentType("application/xhtml+xml"); //the right thing for everyone else

        } else if (extension.equals(".xif")) {
            response.setContentType("image/vnd.xiff"); 
         
        } else if (extension.equals(".xls")) {
            response.setContentType("application/vnd.ms-excel"); 

        } else if (extension.equals(".xlsb")) {
            response.setContentType("application/vnd.ms-excel.sheet.binary.macroenabled.12"); 

        } else if (extension.equals(".xlsm")) {
            response.setContentType("application/vnd.ms-excel.sheet.macroenabled.12"); 

        } else if (extension.equals(".xlsx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); 

        } else if (extension.equals(".xltm")) {
            response.setContentType("application/vnd.ms-excel.template.macroenabled.12"); 

        } else if (extension.equals(".xltx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.template"); 

        } else if (extension.equals(".xml")) { 
            //special case for SOS (see EDDTable.sosResponseFormats): 
            //"text/xml;schema=\"ioos/0.6.1\"", "application/ioos+xml;version=0.6.1",
            response.setContentType(
                fileType.startsWith("custom:") ?  
                    fileType.substring(7) :
                fileType.indexOf("ioos") >= 0 ||           //IOOS SOS types
                fileType.equals("text/xml; subtype=\"om/1.0.0\"")?    //Oostethys SOS type
                    fileType :   //the SOS mime type
                    //"text/xml"); //text/plain allows viewing in browser
                    "application/xml"); //official

        } else if (extension.equals(".xpm")) {
            response.setContentType("image/x-xpixmap"); 
         
        } else if (extension.equals(".xslt")) {
            response.setContentType("application/xslt+xml"); 
         
        } else if (extension.equals(".xwd")) {
            response.setContentType("image/x-xwindowdump"); 
         
        } else if (extensionLC.equals(".z")) { 
            response.setContentType("application/x-compress");  
            genericCompressed = true;

        } else if (extension.equals(".zip")) { 
            response.setContentType("application/zip");  
            genericCompressed = true;

        } else { //.mat
            response.setContentType("application/x-download");  //or "application/octet" ?
            //how specify file name in popup window that user is shown? see below
            //see http://forum.java.sun.com/thread.jspa?threadID=696263&messageID=4043287
        }

        //set the characterEncoding
        if (characterEncoding != null && characterEncoding.length() > 0)
            response.setCharacterEncoding(characterEncoding);

        //specify the file's name  (this may force show File Save As dialog box in user's browser)
        
        if (genericCompressed ||         //include all genericCompressed types
            extension.equals(".csv")  || 
            extension.equals(".itx")  || 
            extension.equals(".js")   || 
             fileType.equals(".json") || //not .jsonText
            extension.equals(".jsonl") || 
            extension.equals(".kml")  || 
            extension.equals(".mat")  || 
            extension.equals(".nc")   ||
             fileType.equals(".odvTxt") ||  //don't force Save As for other .txt, but do for .odvTxt
            extension.equals(".pdf")  ||
            extension.equals(".tif")  ||
            extension.equals(".tsv")  ||
            extension.equals(".xml")) {
            response.setHeader("Content-Disposition","attachment;filename=" + 
                fileName + extension);
        }

        //Compress the output stream if user request says it is allowed.
        //See http://www.websiteoptimization.com/speed/tweak/compress/  (gone?!)
        //and http://betterexplained.com/articles/how-to-optimize-your-site-with-gzip-compression/
//see similar code in Browser.java
        //This makes sending raw file (even ASCII) as efficient as sending a zipped file
        //   and user doesn't have to unzip the file.
        //Accept-Encoding should be a csv list of acceptable encodings.
        //DAP 2.0 section 6.2.1 says compress, gzip, and deflate are possible.
        //See http://httpd.apache.org/docs/1.3/mod/mod_mime.html
        //  which says that x-gzip=gzip and x-compress=compress
        String acceptEncoding = request.getHeader("accept-encoding"); //case-insensitive
        acceptEncoding = acceptEncoding == null? "" : acceptEncoding.toLowerCase();
        String tContentType = response.getContentType(); //as set above, or null
        if (tContentType == null)
            tContentType = "";

        //responses that I won't compress
        if (hasRangeRequest ||
            genericCompressed || //include all genericCompressed types
            otherCompressed ||   //include all otherCompressed types
            //already compressed audio, image, video files
            tContentType.indexOf("audio/") >= 0 ||
            tContentType.indexOf("image/") >= 0 ||
            tContentType.indexOf("video/") >= 0) {

            //no compression  (since already compressed)
            //DODSServlet says:
            //  This should probably be set to "plain" but this works, the
            //  C++ clients don't barf as they would if I sent "plain" AND
            //  the C++ don't expect compressed data if I do this...
            //But other sources say to use "identity"
            //  e.g., https://en.wikipedia.org/wiki/HTTP_compression
            usingCompression = "identity";
            response.setHeader("Content-Encoding", usingCompression);
            //Currently, never set Content-Length. But Erddap.doTransfer() sometimes does.
            //if (!hasRangeRequest && tLength > 0) 
            //    response.setContentLengthLong(tLength);
            outputStream = response.getOutputStream(); //after all setHeader

        //ZipOutputStream too finicky.  outputStream.closeEntry() MUST be called at end or it fails
        //} else if (acceptEncoding.indexOf("compress") >= 0) {
        //    usingCompression = "compress";
        //    response.setHeader("Content-Encoding", usingCompression);
        //    outputStream = new ZipOutputStream(response.getOutputStream());
        //    ((ZipOutputStream)outputStream).putNextEntry(new ZipEntry(fileName + extension));

        } else if (acceptEncoding.indexOf("gzip") >= 0) { 
            usingCompression = "gzip";
            response.setHeader("Content-Encoding", usingCompression);
            outputStream = new GZIPOutputStream(response.getOutputStream());
       
        //"deflate" is troublesome. Don't support it? Apache just supports gzip. But it hasn't been trouble.
        //see https://en.wikipedia.org/wiki/HTTP_compression
        } else if (acceptEncoding.indexOf("deflate") >= 0) {
            usingCompression = "deflate";
            response.setHeader("Content-Encoding", usingCompression);
            outputStream = new DeflaterOutputStream(response.getOutputStream());

        } else /**/ { 
            //no compression  (see DODSServlet comments above (for .gif))
            usingCompression = "identity";
            response.setHeader("Content-Encoding", usingCompression);
            //Currently, never set Content-Length. But Erddap.doTransfer() sometimes does.
            //if (tLength > 0) 
            //    response.setContentLengthLong(tLength);
            outputStream = response.getOutputStream(); //after all setHeader
        }

        if (verbose) {
            String2.log("OutputStreamFromHttpResponse " + characterEncoding + 
                ", encoding=" + usingCompression + ", " + fileName + ", " + extension);

            /* //log header information
            Enumeration en = request.getHeaderNames();
            while (en.hasMoreElements()) {
                String name = en.nextElement().toString();
                String2.log("  request header " + name + "=" + request.getHeader(name));
            }
            */
        }

        //Buffering:
        //HttpServletResponse.getBufferSize() returns the buffer size.
        //HttpServletResponse.setBufferSize() sets the buffer size.
        //HttpServletResponse.getOutputStream() returns a buffered stream/socket.
        //In Tomcat the socketBuffer setting specifies the default buffer size (default=9000)
        //I'm just sticking with the default.
        return outputStream; 
    }

    /** 
     * After ouputStream() has been called, this indicates the encoding (compression)
     * being used for an OutputStreamFromHttpResponse (gzip, deflate) 
     * or "identity" if no compression.
     */
    public String usingCompression() {
        return usingCompression;
    }

    /**
     * This returns the outputStream if it has already been created (else null).
     */
    public OutputStream existingOutputStream() {
        return outputStream;
    }


    //See http://httpd.apache.org/docs/1.3/mod/mod_mime.html
    //  which says that x-gzip=gzip and x-compress=compress
    //This is also repeated in messages2.xml in 
    //\"compress\", \"x-compress\" no longer supported because of difficulties with addEntry, closeEntry
    public static String acceptEncodingHtml(String tErddapUrl) {
        return 
        "<strong><a class=\"selfLink\" id=\"compression\" href=\"#compression\" rel=\"bookmark\">Requesting Compressed Files</a></strong>\n" + 
        "    <br>" + EDStatic.ProgramName + " doesn't offer results stored in compressed (e.g., .zip or .gzip) files.\n" +
        "    Instead, " + EDStatic.ProgramName + " looks for\n" +
        "      <a href=\"http://betterexplained.com/articles/how-to-optimize-your-site-with-gzip-compression/\">accept-encoding" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
        "      in the HTTP GET request header sent\n" +
        "    by the client.  If a supported compression type (\"gzip\", \"x-gzip\", or \"deflate\") is found" +
        "    in the accept-encoding list, " + EDStatic.ProgramName + " includes \"content-encoding\" in the HTTP response\n" +
        "    header and compresses the data as it transmits it.\n" +
        "    It is up to the client program to look for \"content-encoding\" and decompress the data.\n" +
        "    Browsers and OPeNDAP clients do this by default. They request compressed data and\n" +
        "    decompress the returned data automatically.\n" +
        "    Other clients (e.g., Java programs) have to do this explicitly.\n"; 
    }


}



