/*
 * OutputStreamFromHttpResponse Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * OutputStreamFromHttpResponse provides an OutputStream upon request.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-01
 */
public class OutputStreamFromHttpResponse implements OutputStreamSource {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static String HTML_MIME_TYPE = "text/html";
  public static String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";

  private HttpServletRequest request;
  private HttpServletResponse response;
  private String fileName;
  private String fileType;
  private String extension;
  private String usingCompression = ""; // not yet set
  private OutputStream outputStream;
  private boolean hasRangeRequest;

  /**
   * The constructor.
   *
   * @param tRequest information is extracted from the header of the request
   * @param tResponse the outputStream is created from/for the response
   * @param tFileName without the directory or extension. This is only used for fileExtensions that
   *     encourage the client to save the contents in a file (e.g., .csv).
   * @param tFileType The ERDDAP extension e.g., .esriAscii. In a few cases, more than one fileType
   *     (e.g., .asc and .esriAscii) convert to the same actual extension (i.e., tExtension) (e.g.,
   *     .asc).
   * @param tExtension the actual standard web file type extension (called fileTypeExtension in
   *     ERDDAP, e.g., .asc) for the output
   */
  public OutputStreamFromHttpResponse(
      HttpServletRequest tRequest,
      HttpServletResponse tResponse,
      String tFileName,
      String tFileType,
      String tExtension) {

    request = tRequest;
    response = tResponse;
    fileName = tFileName;
    fileType = tFileType;
    extension = tExtension;

    hasRangeRequest = request.getHeader("Range") != null;
  }

  /**
   * This is useful for OutputStream types that support fileName if you want to change the download
   * fileName before the call to getOutputStream().
   */
  @Override
  public void setFileName(String tFileName) {
    fileName = tFileName;
  }

  /** This is like getFiletypeInfo, but just returns the contentType. */
  public static String getFileContentType(
      HttpServletRequest request, String fileType, String extension) {
    return (String) getFileTypeInfo(request, fileType, extension)[0];
  }

  /**
   * This returns info related to a fileType.
   *
   * @param request the user's request
   * @param fileType the ERDDAP fileType, e.g., .htmlTable
   * @param extension the extension of the file that will be returned to the user, e.g., .html .
   * @return an Object[] with {String contentType, HashMap headerMap, Boolean genericCompressed,
   *     Boolean otherCompressed). contentType will always be something. headerMap may be empty.
   */
  public static Object[] getFileTypeInfo(
      HttpServletRequest request, String fileType, String extension) {

    // see mime type list at http://www.webmaster-toolkit.com/mime-types.shtml
    // or http://html.megalink.com/programmer/pltut/plMimeTypes.html
    // or http://www.freeformatter.com/mime-types-list.html#mime-types-list
    String extensionLC = extension.toLowerCase();
    String contentType = null;
    HashMap headerMap = new HashMap();
    boolean genericCompressed = false; // true for generic compressed files, e.g., .zip
    boolean otherCompressed =
        false; // true for app specific compressed (but not audio/ image/ video)

    if (extension.equals(".3gp")) {
      contentType = "video/3gpp";

    } else if (extension.equals(".7z")) {
      contentType = "application/x-7z-compressed";
      genericCompressed = true;

    } else if (extension.equals(".ai")) {
      contentType = "application/postscript";

    } else if (extension.equals(".aif") || extension.equals(".aiff") || extension.equals(".aifc")) {
      contentType = "audio/x-aiff";

    } else if (extension.equals(".asc")) {
      // There are a couple of fileNameTypes that lead to .asc.
      // If DODS, ...
      if (fileType.equals(".asc"))
        headerMap.put(
            "Content-Description",
            "dods-data"); // DAP 2.0, 7.1.1  //pre 2019-03-29 was "content-description" "dods_data"
      else if (fileType.equals(".timeGaps"))
        headerMap.put(
            "Content-Description",
            "time_gap_information"); // pre 2019-03-29 was "content-description"
      contentType = "text/plain";

    } else if (extension.equals(".au")) {
      contentType = "audio/basic";

    } else if (extension.equals(".bin")) {
      contentType = "application/octet-stream";

    } else if (extension.equals(".bmp")) {
      contentType = "image/bmp";

    } else if (extension.equals(".bz")) {
      contentType = "application/x-bzip";
      genericCompressed = true;

    } else if (extension.equals(".bz2")) {
      contentType = "application/x-bzip2";
      genericCompressed = true;

    } else if (extension.equals(".chm")) {
      contentType = "application/vnd.ms-htmlhelp";

    } else if (extension.equals(".css")) {
      contentType = "text/css";

    } else if (extension.equals(".csv")) {
      contentType = "text/csv";

    } else if (extension.equals(".das")) {
      contentType = "text/plain";
      headerMap.put(
          "Content-Description",
          "dods-das"); // DAP 2.0, 7.1.1  ???!!!DConnect (that's JPL -- ignore it) has 'c' 'd', BUT
      // spec (follow the spec) and THREDDS have 'C' 'D'-- but HTTP header names
      // are case-insensitive
      // until ERDDAP v1.84, was "content-description", "dods_das": c d _ !

    } else if (extension.equals(".dds")) {
      contentType = "text/plain";
      headerMap.put("Content-Description", "dods-dds"); // DAP 2.0, 7.1.1

    } else if (extension.equals(".der")) {
      contentType = "application/x-x509-ca-cert";

    } else if (extension.equals(".doc")) {
      contentType = "application/msword";

    } else if (extension.equals(".docx")) {
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      otherCompressed = true;

    } else if (extension.equals(".dods")) {
      // see dods.servlet.DODSServlet.doGetDODS for example
      contentType = "application/octet-stream";
      headerMap.put("Content-Description", "dods-data"); // DAP 2.0, 7.1.1

    } else if (extension.equals(".dotx")) {
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.template";

    } else if (extension.equals(".f4v")) {
      contentType = "video/x-f4v";

    } else if (extension.equals(".flac")) {
      contentType = "audio/flac";

    } else if (extension.equals(".flv")) {
      contentType = "video/x-flv";

    } else if (extension.equals(".gif")) {
      contentType = "image/gif";

    } else if (extension.equals(".gtar")) {
      contentType = "application/x-gtar";
      genericCompressed = true;

    } else if (extension.equals(".gz")) {
      contentType = "application/x-gzip";
      genericCompressed = true;

    } else if (extension.equals(".gzip")) {
      contentType = "application/x-gzip";
      genericCompressed = true;

    } else if (extension.equals(".h261")) {
      contentType = "video/h261";

    } else if (extension.equals(".h263")) {
      contentType = "video/h263";

    } else if (extension.equals(".h264")) {
      contentType = "video/h264";

    } else if (extension.equals(".hdf")) {
      contentType = "application/x-hdf";

    } else if (extension.equals(".html")) { // ERDDAP always writes as File2.UTF_8
      contentType = HTML_MIME_TYPE;

    } else if (extension.equals(".ief")) {
      contentType = "image/ief";

    } else if (extension.equals(".jar")) {
      contentType = "application/java-archive";
      otherCompressed = true;

    } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
      contentType = "image/jpeg";

    } else if (extension.equals(".jpgv")) {
      contentType = "video/jpeg";

    } else if (extension.equals(".js")) {
      contentType = "application/x-javascript";

    } else if (extension.equals(".json")) {
      contentType =
          fileType.equals(".jsonp")
              ? // pseudo fileType
              "application/javascript"
              : // see
              // https://stackoverflow.com/questions/477816/what-is-the-correct-json-content-type
              fileType.equals(".jsonText")
                  ? "text/plain"
                  : // ESRI Geoservices REST uses this
                  "application/json"; // http://dret.net/biblio/reference/rfc4627

    } else if (extension.equals(".jsonl")) {
      contentType =
          fileType.equals(".jsonp")
              ? // pseudo fileType
              "application/javascript"
              : // see
              // https://stackoverflow.com/questions/477816/what-is-the-correct-json-content-type
              "application/x-jsonlines"; // no definitive answer.
      // https://github.com/wardi/jsonlines/issues/9  I like
      // x-jsonlines because it is descriptive.

    } else if (extension.equals(".kml")) {
      // see https://developers.google.com/kml/documentation/kml_tut
      // which lists both of these content types (in different places)
      // application/keyhole is used by the pydap example that works
      // http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
      // contentType = "application/vnd.google-earth.kml+xml";
      // Opera says handling program is "Opera"!  So I manually added this mime type to Opera.
      contentType = KML_MIME_TYPE;

    } else if (extension.equals(".kmz")) {
      contentType = "application/vnd.google-earth.kmz";
      otherCompressed = true;

    } else if (extension.equals(".latex")) {
      contentType = "application/x-latex";

    } else if (extension.equals(".lha")) {
      contentType = "application/lha";
      genericCompressed = true;

    } else if (extension.equals(".lzh")) {
      contentType = "application/x-lzh";
      genericCompressed = true;

    } else if (extension.equals(".lzma")) {
      contentType = "application/x-lzma";
      genericCompressed = true;

    } else if (extension.equals(".lzx")) {
      contentType = "application/x-lzx";
      genericCompressed = true;

    } else if (extension.equals(".log")) {
      contentType = "text/plain";

    } else if (extension.equals(".m21")) {
      contentType = "application/mp21";

    } else if (extension.equals(".mdb")) {
      contentType = "application/x-msaccess";

    } else if (extension.equals(".mid")) {
      contentType = "audio/midi";

    } else if (extension.equals(".mov")) {
      contentType = "video/quicktime";

    } else if (extension.equals(".movie")) {
      contentType = "video/x-sgi-movie";

    } else if (extension.equals(".mpeg")) {
      contentType = "video/mpeg";

    } else if (extension.equals(".mpg")) {
      contentType = "audio/mpeg";

    } else if (extension.equals(".mpga")) {
      contentType = "audio/mpeg";

    } else if (extension.equals(".mp3")) {
      contentType = "audio/mpeg3";

    } else if (extension.equals(".mp4a")) {
      contentType = "audio/mp4";

    } else if (extension.equals(".mp4")) {
      contentType = "video/mp4";

    } else if (extension.equals(".mpp")) {
      contentType = "application/vnd.ms-project";

    } else if (extension.equals(".nc") || extension.equals(".cdf")) {
      contentType = "application/x-netcdf";

    } else if (extension.equals(".odb")) {
      contentType = "application/vnd.oasis.opendocument.database";

    } else if (extension.equals(".odc")) {
      contentType = "application/vnd.oasis.opendocument.chart";

    } else if (extension.equals(".otc")) {
      contentType = "application/vnd.oasis.opendocument.chart-template";

    } else if (extension.equals(".odf")) {
      contentType = "application/vnd.oasis.opendocument.formula";

    } else if (extension.equals(".odg")) {
      contentType = "application/vnd.oasis.opendocument.graphics";

    } else if (extension.equals(".odi")) {
      contentType = "application/vnd.oasis.opendocument.image";

    } else if (extension.equals(".odp")) {
      contentType = "application/vnd.oasis.opendocument.presentation";

    } else if (extension.equals(".ods")) {
      contentType = "application/vnd.oasis.opendocument.spreadsheet";
      // contentType = "application/oda";

    } else if (extension.equals(".odt")) {
      contentType = "application/vnd.oasis.opendocument.text";

    } else if (extension.equals(".oga")) {
      contentType = "audio/ogg";

    } else if (extension.equals(".ogg")) {
      contentType = "audio/ogg";

    } else if (extension.equals(".ogv")) {
      contentType = "video/ogg";

    } else if (extension.equals(".ogx")) {
      contentType = "application/ogg";
      otherCompressed = true;

    } else if (extension.equals(".onetoc")) {
      contentType = "application/onenote";

    } else if (extension.equals(".opf")) {
      contentType = "application/oebps-package+xm";

    } else if (extension.equals(".otf")) {
      contentType = "application/x-font-otf";

    } else if (extension.equals(".otg")) {
      contentType = "application/vnd.oasis.opendocument.graphics-template";

    } else if (extension.equals(".oth")) {
      contentType = "application/vnd.oasis.opendocument.text-web";

    } else if (extension.equals(".oti")) {
      contentType = "application/vnd.oasis.opendocument.image-template";

    } else if (extension.equals(".otp")) {
      contentType = "application/vnd.oasis.opendocument.presentation-template";

    } else if (extension.equals(".ots")) {
      contentType = "application/vnd.oasis.opendocument.spreadsheet-template";
    } else if (extension.equals(".parquet") || extension.equals(".parquetWMeta")) {
      contentType = "application/parquet";
    } else if (extension.equals(".pbm")) {
      contentType = "image/x-portable-bitmap";

    } else if (extension.equals(".pcx")) {
      contentType = "image/x-pcx";

    } else if (extension.equals(".pdf")) {
      contentType = "application/pdf";

    } else if (extension.equals(".pfr")) {
      contentType = "application/font-tdpfr";

    } else if (extension.equals(".pgm")) {
      contentType = "image/x-portable-graymap";

    } else if (extension.equals(".pic") || extension.equals(".pict")) {
      contentType = "image/pict";

    } else if (extension.equals(".png")) {
      contentType = "image/png";

    } else if (extension.equals(".pnm")) {
      contentType = "image/x-portable-anymap";

    } else if (extension.equals(".pot")) {
      contentType = "application/mspowerpoint";

    } else if (extension.equals(".ppm")) {
      contentType = "image/x-portable-pixmap";

    } else if (extension.equals(".potx")) {
      contentType = "application/vnd.openxmlformats-officedocument.presentationml.template";

    } else if (extension.equals(".ppsm")) {
      contentType = "application/vnd.ms-powerpoint.slideshow.macroenabled.12";

    } else if (extension.equals(".ppsx")) {
      contentType = "application/vnd.openxmlformats-officedocument.presentationml.slideshow";

    } else if (extension.equals(".ppt")) {
      contentType = "application/mspowerpoint";

    } else if (extension.equals(".pptm")) {
      contentType = "application/vnd.ms-powerpoint.presentation.macroenabled.12";

    } else if (extension.equals(".pptx")) {
      contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    } else if (extension.equals(".psd")) {
      contentType = "image/vnd.adobe.photoshop";

    } else if (extension.equals(".pub")) {
      contentType = "application/x-mspublisher";

    } else if (extension.equals(".qt")) {
      contentType = "video/quicktime";

    } else if (extension.equals(".py")) {
      contentType = "text/x-script.phyton";

    } else if (extension.equals(".pyc")) {
      contentType = "application/x-bytecode.python";

    } else if (extension.equals(".qt")) {
      contentType = "video/quicktime";

    } else if (extension.equals(".ra")) {
      contentType = "audio/x-realaudio";

    } else if (extension.equals(".ram")) {
      contentType = "audio/x-pn-realaudio";

    } else if (extension.equals(".rar")) {
      contentType = "application/x-rar-compressed";
      otherCompressed = true;

    } else if (extension.equals(".rgb")) {
      contentType = "image/x-rgb";

    } else if (extension.equals(".rm")) {
      contentType = "application/vnd.rn-realmedia";
      otherCompressed = true;

    } else if (extension.equals(".rq")) {
      contentType = "application/sparql-query";

    } else if (extension.equals(".rt")) {
      contentType = "text/richtext";

    } else if (extension.equals(".rtf")) {
      contentType = "application/rtf";

    } else if (extension.equals(".rtx")) {
      contentType = "text/richtext";

    } else if (extension.equals(".rss")) {
      contentType = "application/rss+xml";

    } else if (extension.equals(".sbml")) {
      contentType = "application/sbml+xml";

    } else if (extension.equals(".scd")) {
      contentType = "application/x-msschedule";

    } else if (extension.equals(".sgml")) {
      contentType = "text/sgml";

    } else if (extension.equals(".sldm")) {
      contentType = "application/vnd.ms-powerpoint.slide.macroenabled.12";

    } else if (extension.equals(".sldx")) {
      contentType = "application/vnd.openxmlformats-officedocument.presentationml.slide";

    } else if (extension.equals(".srx")) {
      contentType = "application/sparql-results+xml";

    } else if (extension.equals(".ssml")) {
      contentType = "application/ssml+xml";

    } else if (extension.equals(".stc")) {
      contentType = "application/vnd.sun.xml.calc.template";

    } else if (extension.equals(".std")) {
      contentType = "application/vnd.sun.xml.draw.template";

    } else if (extension.equals(".sti")) {
      contentType = "application/vnd.sun.xml.impress.template";

    } else if (extension.equals(".stw")) {
      contentType = "application/vnd.sun.xml.writer.template";

    } else if (extension.equals(".svg")) {
      contentType = "image/svg+xml";

    } else if (extension.equals(".sxc")) {
      contentType = "application/vnd.sun.xml.calc";

    } else if (extension.equals(".sxd")) {
      contentType = "application/vnd.sun.xml.draw";

    } else if (extension.equals(".sxi")) {
      contentType = "application/vnd.sun.xml.impress";

    } else if (extension.equals(".sxm")) {
      contentType = "application/vnd.sun.xml.math";

    } else if (extension.equals(".sxw")) {
      contentType = "application/vnd.sun.xml.writer";

    } else if (extension.equals(".sxg")) {
      contentType = "application/vnd.sun.xml.writer.global";

    } else if (extension.equals(".tar")) {
      contentType = "application/x-tar";
      genericCompressed = true;

    } else if (extension.equals(".tcl")) {
      contentType = "application/x-tcl";

    } else if (extension.equals(".tei")) {
      contentType = "application/tei+xml";

    } else if (extension.equals(".tex")) {
      contentType = "application/x-tex";

    } else if (extension.equals(".tfm")) {
      contentType = "application/x-tex-tfm";

    } else if (extension.equals(".tif") || extension.equals(".tiff")) {
      contentType = "image/tiff";

    } else if (extension.equals(".tsv")) {
      contentType = "text/tab-separated-values";

    } else if (extension.equals(".ttf")) {
      contentType = "application/x-font-ttf";

    } else if (extension.equals(".ttl")) {
      contentType = "text/turtle";

    } else if (extension.equals(".txt")) {
      // ODV uses .txt but doesn't have a distinct mime type (as far as I can tell) see 2010-06-15
      // notes
      contentType = "text/plain";

    } else if (extension.equals(".uoml")) {
      contentType = "application/vnd.uoml+xml";

    } else if (extension.equals(".ufd")) {
      contentType = "application/vnd.ufdl";

    } else if (extension.equals(".vcf")) {
      contentType = "text/x-vcard";

    } else if (extension.equals(".vcs")) {
      contentType = "text/x-vcalendar";

    } else if (extension.equals(".vsd")) {
      contentType = "application/vnd.visio";

    } else if (extension.equals(".vxml")) {
      contentType = "application/voicexml+xml";

    } else if (extension.equals(".war")) {
      contentType = "application/zip.war";
      genericCompressed = true;

    } else if (extension.equals(".wav") || extension.equals(".wave")) {
      contentType = "audio/wav"; // I've seen audio/x-wav

    } else if (extension.equals(".weba")) {
      contentType = "audio/webm";

    } else if (extension.equals(".webm")) {
      contentType = "video/webm";

    } else if (extension.equals(".webp")) {
      contentType = "image/webp";

    } else if (extension.equals(".wm")) {
      contentType = "video/x-ms-wm";

    } else if (extension.equals(".wma")) {
      contentType = "audio/x-ms-wma";

    } else if (extension.equals(".wmv")) {
      contentType = "video/x-ms-wmv";

    } else if (extension.equals(".wps")) {
      contentType = "application/vnd.ms-works";

    } else if (extension.equals(".wri")) {
      contentType = "application/x-mswrite";

    } else if (extension.equals(".wsdl")) {
      contentType = "application/wsdl+xml";

    } else if (extension.equals(".xbm")) {
      contentType = "image/x-xbitmap";

    } else if (extension.equals(".xhtml")) {
      // PROBLEM: MS Internet Explorer doesn't display .xhtml files.
      // It just shows endless series of "Save As..." dialog boxes.
      // "application/xhtml+xml" is proper mime type,
      //  see http://keystonewebsites.com/articles/mime_type.php
      // Lots of websites serve xhtml successfully using this
      //  e.g., in firefox, see Tools : Page Info for
      //  https://www.w3.org/MarkUp/Forms/2003/xforms-for-html-authors
      // see https://www.w3.org/TR/xhtml1/#guidelines
      // But they do something else.
      //
      // I did:
      // "<p>XHTML and Internet Explorer - Attempts to view .xhtml files in Internet Explorer on
      // Windows XP \n" +
      // "<br>often leads to an endless series of \"Save As...\" dialog boxes. The problem seems to
      // be that \n" +
      // "<br>Windows XP has no registry entry for the standard XHTML mime type:
      // application/xhtml+xml.\n" +
      // "<br>See <a href=\"http://www.peterprovost.org/archive/2004/10/22/2003.aspx\">this blog</a>
      // for a possible solution.\n" +
      // But that is not a good solution for ERDDAP clients (too risky).
      //
      // SOLUTION from http://www.ibm.com/developerworks/xml/library/x-tipapachexhtml/index.html
      // if request is from Internet Explorer, use mime type text/html.
      String userAgent = request.getHeader("user-agent"); // case-insensitive
      if (userAgent != null && userAgent.indexOf("MSIE") >= 0) {
        contentType = "text/html"; // the hack
        if (verbose) String2.log(".xhtml request from user-agent=MSIE: using mime=text/html");
      } else contentType = "application/xhtml+xml"; // the right thing for everyone else

    } else if (extension.equals(".xif")) {
      contentType = "image/vnd.xiff";

    } else if (extension.equals(".xls")) {
      contentType = "application/vnd.ms-excel";

    } else if (extension.equals(".xlsb")) {
      contentType = "application/vnd.ms-excel.sheet.binary.macroenabled.12";

    } else if (extension.equals(".xlsm")) {
      contentType = "application/vnd.ms-excel.sheet.macroenabled.12";

    } else if (extension.equals(".xlsx")) {
      contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    } else if (extension.equals(".xltm")) {
      contentType = "application/vnd.ms-excel.template.macroenabled.12";

    } else if (extension.equals(".xltx")) {
      contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.template";

    } else if (extension.equals(".xml")) {
      // special case for SOS (see EDDTable.sosResponseFormats):
      // "text/xml;schema=\"ioos/0.6.1\"", "application/ioos+xml;version=0.6.1",
      contentType =
          fileType.startsWith("custom:")
              ? fileType.substring(7)
              : fileType.indexOf("ioos") >= 0
                      || // IOOS SOS types
                      fileType.equals("text/xml; subtype=\"om/1.0.0\"")
                  ? // Oostethys SOS type
                  fileType
                  : // the SOS mime type
                  // "text/xml"; //text/plain allows viewing in browser
                  "application/xml"; // official

    } else if (extension.equals(".xpm")) {
      contentType = "image/x-xpixmap";

    } else if (extension.equals(".xslt")) {
      contentType = "application/xslt+xml";

    } else if (extension.equals(".xwd")) {
      contentType = "image/x-xwindowdump";

    } else if (extensionLC.equals(".z")) {
      contentType = "application/x-compress";
      genericCompressed = true;

    } else if (extension.equals(".zip")) {
      contentType = "application/zip";
      genericCompressed = true;

    } else { // .mat
      contentType = "application/x-download"; // or "application/octet" ?
      // how specify file name in popup window that user is shown? see below
      // see http://forum.java.sun.com/thread.jspa?threadID=696263&messageID=4043287
    }
    return new Object[] {
      contentType, headerMap, Boolean.valueOf(genericCompressed), Boolean.valueOf(otherCompressed)
    };
  }

  /**
   * This determines if a response should encourage showing File Save As dialog box in user's
   * browser.
   *
   * @param fileType The ERDDAP extension e.g., .esriAscii. In a few cases, more than one fileType
   *     (e.g., .asc and .esriAscii) convert to the same actual extension (i.e., tExtension) (e.g.,
   *     .asc).
   * @param extension the actual standard web file type extension (called fileTypeExtension in
   *     ERDDAP, e.g., .asc) for the output
   */
  public static boolean showFileSaveAs(
      boolean genericCompressed, String fileType, String extension) {

    return genericCompressed
        || // include all genericCompressed types
        extension.equals(".cdf")
        || extension.equals(".csv")
        || fileType.equals(".esriAscii")
        || extension.equals(".itx")
        || extension.equals(".js")
        || fileType.equals(".json")
        || // not .jsonText
        extension.equals(".jsonl")
        || extension.equals(".kml")
        || extension.equals(".mat")
        || extension.equals(".nc")
        || fileType.equals(".odvTxt")
        || // don't force Save As for other .txt, but do for .odvTxt
        extension.equals(".parquet")
        || extension.equals(".pdf")
        || extension.equals(".tif")
        || extension.equals(".tsv")
        || extension.equals(".xml");
  }

  @Override
  public OutputStream outputStream(String characterEncoding) throws Throwable {
    return outputStream(characterEncoding, -1);
  }

  /**
   * This returns the OutputStream. If called repeatedly, this returns the same outputStream.
   *
   * <p>This never calls httpServletResponse.setContentLength ("Content-Length") because the length
   * isn't as expected (the file's length) if I compress the response or if I send a Byte Range
   * response. An incorrectly specified Content-Length screws things up -- Firefox and Chrome wait
   * 20 seconds before giving up. See it esp with full page reload: Ctrl Reload in Chrome. See it
   * with Performance recording in Firefox: In both cases .html loads quickly but related resources
   * take 20 seconds (almost exactly). There is no need to set Content-Length (except when dealing
   * with Byte Ranges -- see the 2nd Erddap.doTransfer()). NOTE: if the request has a Range request
   * (hasRangeRequest), this won't ever compress the response. See usage of hasRangeRequest in this
   * class.
   *
   * @param characterEncoding e.g., "" (for none specified), File2.UTF_8, or "" (for DAP). This
   *     parameter only matters the first time this method is called. This only matters for some
   *     subclasses.
   * @param tLength The length of the entire file for the response, if known (else -1). Currently,
   *     this is not used.
   * @return a buffered outputStream. If outputStream has already been created, the same one is
   *     returned.
   * @throws Throwable if trouble
   */
  @Override
  public OutputStream outputStream(String characterEncoding, long tLength) throws Throwable {

    if (outputStream != null) return outputStream;

    // do all the things associated with committing to writing data to the outputStream
    // setContentType(mimeType)
    // User won't want hassle of saving file (changing fileType, ...).

    // get and apply the fileTypeInfo
    // 2020-12-07 this is the section that was inline but now uses the static methods above
    Object fileTypeInfo[] = getFileTypeInfo(request, fileType, extension);
    String contentType = (String) fileTypeInfo[0];
    HashMap headerMap = (HashMap) fileTypeInfo[1];
    boolean genericCompressed =
        ((Boolean) fileTypeInfo[2]).booleanValue(); // true for generic compressed files, e.g., .zip
    boolean otherCompressed =
        ((Boolean) fileTypeInfo[3])
            .booleanValue(); // true for app specific compressed (but not audio/ image/ video)

    response.setContentType(contentType);
    Iterator it = headerMap.keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      response.setHeader(key, (String) headerMap.get(key));
    }

    // set the characterEncoding
    if (characterEncoding != null && characterEncoding.length() > 0)
      response.setCharacterEncoding(characterEncoding);

    // specify the file's name  (this encourages showing File Save As dialog box in user's browser)
    // More importantly this is what actually sets the downloaded filename (even if the browser
    // doesn't show the save as dialog).
    if (showFileSaveAs(genericCompressed, fileType, extension)) {
      response.setHeader("Content-Disposition", "attachment;filename=" + fileName + extension);
    }

    // Compress the output stream if user request says it is allowed.
    // See http://www.websiteoptimization.com/speed/tweak/compress/  (gone?!)
    // and https://betterexplained.com/articles/how-to-optimize-your-site-with-gzip-compression/
    // see similar code in Browser.java
    // This makes sending raw file (even ASCII) as efficient as sending a zipped file
    //   and user doesn't have to unzip the file.
    // Accept-Encoding should be a csv list of acceptable encodings.
    // DAP 2.0 section 6.2.1 says compress, gzip, and deflate are possible.
    // See https://httpd.apache.org/docs/1.3/mod/mod_mime.html
    //  which says that x-gzip=gzip and x-compress=compress
    String acceptEncoding = request.getHeader("accept-encoding"); // case-insensitive
    acceptEncoding = acceptEncoding == null ? "" : acceptEncoding.toLowerCase();
    String tContentType = response.getContentType(); // as set above, or null
    if (tContentType == null) tContentType = "";

    // responses that I won't compress
    if (hasRangeRequest
        || genericCompressed
        || // include all genericCompressed types
        otherCompressed
        || // include all otherCompressed types
        // already compressed audio, image, video files
        tContentType.indexOf("audio/") >= 0
        || tContentType.indexOf("image/") >= 0
        || tContentType.indexOf("video/") >= 0) {

      // no compression  (since already compressed)
      // DODSServlet says:
      //  This should probably be set to "plain" but this works, the
      //  C++ clients don't barf as they would if I sent "plain" AND
      //  the C++ don't expect compressed data if I do this...
      // But other sources say to use "identity"
      //  e.g., https://en.wikipedia.org/wiki/HTTP_compression
      usingCompression = "identity";
      response.setHeader("Content-Encoding", usingCompression);
      // Currently, never set Content-Length. But Erddap.doTransfer() sometimes does.
      // if (!hasRangeRequest && tLength > 0)
      //    response.setContentLengthLong(tLength);
      outputStream = new BufferedOutputStream(response.getOutputStream()); // after all setHeader

      // ZipOutputStream too finicky.  outputStream.closeEntry() MUST be called at end or it fails
      // } else if (acceptEncoding.indexOf("compress") >= 0) {
      //    usingCompression = "compress";
      //    response.setHeader("Content-Encoding", usingCompression);
      //    outputStream = new ZipOutputStream(new
      // BufferedOutputStream(response.getOutputStream()));
      //    ((ZipOutputStream)outputStream).putNextEntry(new ZipEntry(fileName + extension));

    } else if (acceptEncoding.indexOf("gzip") >= 0) {
      usingCompression = "gzip";
      response.setHeader("Content-Encoding", usingCompression);
      outputStream = new GZIPOutputStream(new BufferedOutputStream(response.getOutputStream()));

      // "deflate" is troublesome. Don't support it? Apache just supports gzip. But it hasn't been
      // trouble.
      // see https://en.wikipedia.org/wiki/HTTP_compression
    } else if (acceptEncoding.indexOf("deflate") >= 0) {
      usingCompression = "deflate";
      response.setHeader("Content-Encoding", usingCompression);
      outputStream = new DeflaterOutputStream(new BufferedOutputStream(response.getOutputStream()));

    } else /**/ {
      // no compression  (see DODSServlet comments above (for .gif))
      usingCompression = "identity";
      response.setHeader("Content-Encoding", usingCompression);
      // Currently, never set Content-Length. But Erddap.doTransfer() sometimes does.
      // if (tLength > 0)
      //    response.setContentLengthLong(tLength);
      outputStream = new BufferedOutputStream(response.getOutputStream()); // after all setHeader
    }

    if (verbose) {
      String2.log(
          "OutputStreamFromHttpResponse charEncoding="
              + characterEncoding
              + ", encoding="
              + usingCompression
              + ", "
              + fileName
              + ", "
              + extension);

      /* //log header information
      Enumeration en = request.getHeaderNames();
      while (en.hasMoreElements()) {
          String name = en.nextElement().toString();
          String2.log("  request header " + name + "=" + request.getHeader(name));
      }
      */
    }

    // Buffering:
    // HttpServletResponse.getBufferSize() returns the buffer size.
    // HttpServletResponse.setBufferSize() sets the buffer size.
    // HttpServletResponse.getOutputStream() returns a buffered stream/socket.
    // In Tomcat the socketBuffer setting specifies the default buffer size (default=9000)
    // I'm just sticking with the default.
    return outputStream;
  }

  /**
   * After ouputStream() has been called, this indicates the encoding (compression) being used for
   * an OutputStreamFromHttpResponse (gzip, deflate) or "identity" if no compression.
   */
  @Override
  public String usingCompression() {
    return usingCompression;
  }

  /** This returns the outputStream if it has already been created (else null). */
  @Override
  public OutputStream existingOutputStream() {
    return outputStream;
  }
}
