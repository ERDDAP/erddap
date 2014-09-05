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
    private OutputStream outputStream;

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
    }


    /**
     * A variant of outputStream() for when the contentLength isn't known.
     */
    public OutputStream outputStream(String characterEncoding) throws Throwable {
        return outputStream(characterEncoding, -1);
    }

        
    /**
     * This returns the OutputStream.
     * If called repeatedly, this returns the same outputStream.
     *
     * @param characterEncoding e.g., "" (for none specified), "UTF-8", or "" (for DAP).
     *     This parameter only matters the first time this method is called.
     *     This only matters for some subclasses.
     * @param contentLength the number of bytes that will be sent (or -1 if not known).
     *     Currently, this causes Firefox to freeze, so this method ignores it.
     * @return outputStream
     * @throws Throwable if trouble
     */
    public OutputStream outputStream(String characterEncoding, long contentLength) 
        throws Throwable {

        if (outputStream != null) 
            return outputStream;

        //do all the things associated with committing to writing data to the outputStream
        //setContentType(mimeType)
        //User won't want hassle of saving file (changing fileType, ...).

        //see mime type list at http://www.webmaster-toolkit.com/mime-types.shtml
        // or http://html.megalink.com/programmer/pltut/plMimeTypes.html

        if (extension.equals(".asc")) { 
            //There are a couple of fileNameTypes that lead to .asc.
            //If DODS, ...
            if (fileType.equals(".asc"))
                response.setHeader("content-description", "dods_data"); //DAP 2.0, 7.1.1
            response.setContentType("text/plain"); 

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

        } else if (extension.equals(".dods")) {
            //see dods.servlet.DODSServlet.doGetDODS for example
            response.setContentType("application/octet-stream");
            response.setHeader("content-description", "dods_data"); //DAP 2.0, 7.1.1

        } else if (extension.equals(".gif")) {
            response.setContentType("image/gif");                                 

        } else if (extension.equals(".html")) { 
            response.setContentType(HTML_MIME_TYPE); 

        } else if (extension.equals(".jar")) { 
            response.setContentType("application/x-java-archive");  

        } else if (extension.equals(".jpg") ||
                   extension.equals(".jpeg")) {
            response.setContentType("image/jpeg");                                 

        } else if (extension.equals(".js")) { 
            response.setContentType("application/x-javascript"); 

        } else if (extension.equals(".json")) { 
            response.setContentType(fileType.equals(".jsonText")? 
                "text/plain" :       //ESRI Geoservices REST uses this
                "application/json"); //http://dret.net/biblio/reference/rfc4627

        } else if (extension.equals(".kml")) {
            //see http://earth.google.com/kml/kml_tut.html
            //which lists both of these content types (in different places)
            //application/keyhole is used by the pydap example that works
            //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
            //response.setContentType("application/vnd.google-earth.kml+xml"); 
            //Opera says handling program is "Opera"!  So I manually added this mime type to Opera.
            response.setContentType(KML_MIME_TYPE); 

        } else if (extension.equals(".pdf")) {
            response.setContentType("application/pdf"); 

        } else if (extension.equals(".png")) {
            response.setContentType("image/png"); 

        } else if (extension.equals(".rss")) {
            response.setContentType("application/rss+xml"); 

        } else if (extension.equals(".tsv")) {
            response.setContentType("text/tab-separated-values"); 
         
        } else if (extension.equals(".txt")) {
            //ODV uses .txt but doesn't have a distinct mime type (as far as I can tell) see 2010-06-15 notes
            response.setContentType("text/plain"); 

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

        } else if (extension.equals(".xml")) { 
            //special case for SOS (see EDDTable.sosResponseFormats): 
            //"text/xml;schema=\"ioos/0.6.1\"", "application/ioos+xml;version=0.6.1",
            response.setContentType(
                fileType.startsWith("custom:") ?  
                    fileType.substring(7) :
                fileType.indexOf("ioos") >= 0 ||           //IOOS SOS types
                fileType.equals("text/xml; subtype=\"om/1.0.0\"")?    //Oostethys SOS type
                    fileType :   //the SOS mime type
                    "text/xml"); //text/plain allows viewing in browser

        } else if (extension.equals(".zip")) { 
            response.setContentType("application/zip");  

        } else { //.mat, .nc, .war
            response.setContentType("application/x-download");  //or "application/octet" ?
            //how specify file name in popup window that user is shown? see below
            //see http://forum.java.sun.com/thread.jspa?threadID=696263&messageID=4043287
        }

        //set the characterEncoding
        if (characterEncoding != null && characterEncoding.length() > 0)
            response.setCharacterEncoding(characterEncoding);

        //specify contentLength if known
//???!!! using this causes firefox to freeze (unknown reason)
//        if (contentLength >= 0 && contentLength < Integer.MAX_VALUE) {
//String2.log("response.setContentLength(" + contentLength + ");");
//            response.setContentLength((int)contentLength);
//String2.log("response.setContentLength finished");
//String2.log("");
//        }

        //specify the file's name  (this may force show File Save As dialog box in user's browser)
        if (extension.equals(".csv")  || 
            extension.equals(".jar")  || 
            extension.equals(".js")   || 
             fileType.equals(".json") || //not .jsonText
            extension.equals(".kml")  || 
            extension.equals(".mat")  || 
            extension.equals(".nc")   ||
             fileType.equals(".odvTxt") ||  //don't force Save As for other .txt, but do for .odvTxt
            extension.equals(".pdf")  ||
            extension.equals(".tif")  ||
            extension.equals(".tsv")  ||
            extension.equals(".war")  ||
            extension.equals(".xml")  ||
            extension.equals(".zip")) {
            response.setHeader("Content-Disposition","attachment;filename=" + 
                fileName + extension);
        }

        //Compress the output stream if user request says it is allowed.
        //See http://www.websiteoptimization.com/speed/tweak/compress/  (gone?!)
        //and http://betterexplained.com/articles/how-to-optimize-your-site-with-gzip-compression/
//see similar code in Browser.java and ErdObis.java
        //This makes sending raw file (even ASCII) as efficient as sending a zipped file
        //   and user doesn't have to unzip the file.
        // /* not tested yet. test after other things are working. test them individually
        //Accept-Encoding should be a csv list of acceptable encodings.
        //DAP 2.0 section 6.2.1 says compress, gzip, and deflate are possible.
        //See http://httpd.apache.org/docs/1.3/mod/mod_mime.html
        //  which says that x-gzip=gzip and x-compress=compress
        String acceptEncoding = request.getHeader("accept-encoding"); //case-insensitive
        acceptEncoding = acceptEncoding == null? "" : acceptEncoding.toLowerCase();
        //???does out need to be in BufferedOutputStream; or just buffered if not compressed???
        String useEncoding = "";
        if (extension.equals(".gif") || 
            extension.equals(".jpg") || extension.equals(".jpeg") || 
            extension.equals(".png") || extension.equals(".tif") ||
            extension.equals(".jar") || extension.equals(".war") || extension.equals(".zip")) {
            //no compression  (since already compressed)
            //DODSServlet says:
            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");
            outputStream = response.getOutputStream(); //after all setHeader
        //ZipOutputStream too finicky.  outputStream.closeEntry() MUST be called at end or it fails
        //} else if (acceptEncoding.indexOf("compress") >= 0) {
        //    useEncoding = "compress";
        //    response.setHeader("Content-Encoding", "compress");
        //    outputStream = new ZipOutputStream(response.getOutputStream());
        //    ((ZipOutputStream)outputStream).putNextEntry(new ZipEntry(fileName + extension));
        } else if (acceptEncoding.indexOf("gzip") >= 0) { 
            useEncoding = "gzip";
            response.setHeader("Content-Encoding", "gzip");
            outputStream = new GZIPOutputStream(response.getOutputStream());
        } else if (acceptEncoding.indexOf("deflate") >= 0) {
            useEncoding = "deflate";
            response.setHeader("Content-Encoding", "deflate");
            outputStream = new DeflaterOutputStream(response.getOutputStream());
        } else /**/ { 
            //no compression  (see DODSServlet comments above (for .gif))
            response.setHeader("Content-Encoding", "");
            outputStream = response.getOutputStream(); //after all setHeader
        }

        if (verbose) {
            String2.log("OutputStreamFromHttpResponse " + characterEncoding + 
                ", " + useEncoding + ", " + fileName + ", " + extension);

            /* //log header information
            Enumeration en = request.getHeaderNames();
            while (en.hasMoreElements()) {
                String name = en.nextElement().toString();
                String2.log("  request header " + name + "=" + request.getHeader(name));
            }
            */
        }

        return outputStream;
    }

    //See http://httpd.apache.org/docs/1.3/mod/mod_mime.html
    //  which says that x-gzip=gzip and x-compress=compress
    //This is also repeated in messages2.xml in 
    //\"compress\", \"x-compress\" no longer supported because of difficulties with addEntry, closeEntry
    public static String acceptEncodingHtml(String tErddapUrl) {
        return 
        "<b><a name=\"compression\">Requesting Compressed Files</a></b>\n" + 
        "    <br>" + EDStatic.ProgramName + " doesn't offer results stored in compressed (e.g., .zip or .gzip) files.\n" +
        "    <br>Instead, " + EDStatic.ProgramName + " looks for\n" +
        "      <a href=\"http://betterexplained.com/articles/how-to-optimize-your-site-with-gzip-compression/\">accept-encoding" +
                    EDStatic.externalLinkHtml(tErddapUrl) + "</a>\n" +
        "      in the HTTP GET request header sent\n" +
        "    <br>by the client.  If a supported compression type (\"gzip\", \"x-gzip\", or \"deflate\") is found" +
        "    <br>in the accept-encoding list, " + EDStatic.ProgramName + " includes \"content-encoding\" in the HTTP response\n" +
        "    <br>header and compresses the data as it transmits it.\n" +
        "    <br>It is up to the client program to look for \"content-encoding\" and decompress the data.\n" +
        "    <br>Browsers and OPeNDAP clients do this by default. They request compressed data and\n" +
        "    <br>decompress the returned data automatically.\n" +
        "    <br>Other clients (e.g., Java programs) have to do this explicitly.\n"; 
    }


}



