/* 
 * OutputStreamViaAwsS3 Copyright 2020, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;

import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * OutputStreamViaAwsS3 writes to a file in an S3 bucket, then redirects user to download that file.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2020-12-08
 */
public class OutputStreamViaAwsS3 extends BufferedOutputStream {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    private OutputStreamFromHttpResponseViaAwsS3 parent;

    /**
     * The constructor.
     *
     * @param tParent the OutputStreamFromHttpResponseViaAwsS3 that created this
     */
    public OutputStreamViaAwsS3(OutputStreamFromHttpResponseViaAwsS3 tParent) throws IOException {

        //make the superclass's BufferedOutputStream from a FileOutputStream
        super(new FileOutputStream(
            tParent.cacheDir + tParent.fileName + tParent.extension));
        parent = tParent;
    }

       
    /**
     * This overwrites the super's close() method, closing the buffered FileOutputStream
     * and telling the user to download it.
     */
    public void close() throws IOException {

        super.close();
/*
        //do all the things associated with committing to writing data to the outputStream
        //setContentType(mimeType)
        //User won't want hassle of saving file (changing fileType, ...).

        //get and apply the fileTypeInfo
        //2020-12-07 this is the section that was inline but now uses the static methods above
        Object fileTypeInfo[] = getFileTypeInfo(request, fileType, extension);
        String  contentType       =  (String) fileTypeInfo[0];
        HashMap headerMap         =  (HashMap)fileTypeInfo[1];
        boolean genericCompressed = ((Boolean)fileTypeInfo[2]).booleanValue();  //true for generic compressed files, e.g., .zip
        boolean otherCompressed   = ((Boolean)fileTypeInfo[3]).booleanValue();  //true for app specific compressed (but not audio/ image/ video)

        response.setContentType(contentType);  
        Iterator it = headerMap.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            response.setHeader(key, (String)headerMap.get(key));
        }


        //set the characterEncoding
        if (characterEncoding != null && characterEncoding.length() > 0)
            response.setCharacterEncoding(characterEncoding);

        //specify the file's name  (this encourages showing File Save As dialog box in user's browser)        
        if (showFileSaveAs(genericCompressed, fileType, extension))
            response.setHeader("Content-Disposition","attachment;filename=" + 
                fileName + extension);


        //Compress the output stream if user request says it is allowed.
        //See http://www.websiteoptimization.com/speed/tweak/compress/  (gone?!)
        //and https://betterexplained.com/articles/how-to-optimize-your-site-with-gzip-compression/
//see similar code in Browser.java
        //This makes sending raw file (even ASCII) as efficient as sending a zipped file
        //   and user doesn't have to unzip the file.
        //Accept-Encoding should be a csv list of acceptable encodings.
        //DAP 2.0 section 6.2.1 says compress, gzip, and deflate are possible.
        //See https://httpd.apache.org/docs/1.3/mod/mod_mime.html
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
            outputStream = new BufferedOutputStream(response.getOutputStream()); //after all setHeader

        //ZipOutputStream too finicky.  outputStream.closeEntry() MUST be called at end or it fails
        //} else if (acceptEncoding.indexOf("compress") >= 0) {
        //    usingCompression = "compress";
        //    response.setHeader("Content-Encoding", usingCompression);
        //    outputStream = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()));
        //    ((ZipOutputStream)outputStream).putNextEntry(new ZipEntry(fileName + extension));

        } else if (acceptEncoding.indexOf("gzip") >= 0) { 
            usingCompression = "gzip";
            response.setHeader("Content-Encoding", usingCompression);
            outputStream = new GZIPOutputStream(new BufferedOutputStream(response.getOutputStream()));
       
        //"deflate" is troublesome. Don't support it? Apache just supports gzip. But it hasn't been trouble.
        //see https://en.wikipedia.org/wiki/HTTP_compression
        } else if (acceptEncoding.indexOf("deflate") >= 0) {
            usingCompression = "deflate";
            response.setHeader("Content-Encoding", usingCompression);
            outputStream = new DeflaterOutputStream(new BufferedOutputStream(response.getOutputStream()));

        } else { 
            //no compression  (see DODSServlet comments above (for .gif))
            usingCompression = "identity";
            response.setHeader("Content-Encoding", usingCompression);
            //Currently, never set Content-Length. But Erddap.doTransfer() sometimes does.
            //if (tLength > 0) 
            //    response.setContentLengthLong(tLength);
            outputStream = new BufferedOutputStream(response.getOutputStream()); //after all setHeader
        }

        if (verbose) {
            String2.log("OutputStreamFromHttpResponse " + characterEncoding + 
                ", encoding=" + usingCompression + ", " + fileName + ", " + extension);

            //log header information
            //Enumeration en = request.getHeaderNames();
            //while (en.hasMoreElements()) {
            //    String name = en.nextElement().toString();
            //    String2.log("  request header " + name + "=" + request.getHeader(name));
            //}
            
        }

        //Buffering:
        //HttpServletResponse.getBufferSize() returns the buffer size.
        //HttpServletResponse.setBufferSize() sets the buffer size.
        //HttpServletResponse.getOutputStream() returns a buffered stream/socket.
        //In Tomcat the socketBuffer setting specifies the default buffer size (default=9000)
        //I'm just sticking with the default.
        return outputStream; 

*/
    }

}



