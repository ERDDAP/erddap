/* 
 * OutputStreamFromHttpResponseViaAwsS3 Copyright 2020, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;

import gov.noaa.pfel.erddap.util.EDStatic;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * OutputStreamFromHttpResponseViaAwsS3 provides an OutputStream upon request.
 * This is like OutputStreamFromHttpResponse, but creates a file in an S3 bucket, 
 * then redirects user to the download the file.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2020-12-08
 */
public class OutputStreamFromHttpResponseViaAwsS3 implements OutputStreamSource {

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected String cacheDir;
    protected String fileName;
    protected String fileType;
    protected String extension;

    protected String usingCompression = ""; //not yet set
    protected OutputStream outputStream;
    protected boolean hasRangeRequest;

    /**
     * The constructor.
     *
     * @param tRequest  information is extracted from the header of the request
     * @param tResponse the outputStream is created from/for the response
     * @param tCacheDir the directory that will hold the file temporarily
     * @param tFileName without the directory or extension.
     *    This is only used for fileExtensions that encourage the client to 
     *    save the contents in a file (e.g., .csv).
     * @param tFileType The ERDDAP extension e.g., .esriAscii.
     *    In a few cases, more than one fileType (e.g., .asc and .esriAscii) 
     *    convert to the same actual extension (i.e., tExtension) (e.g., .asc).
     * @param tExtension the actual standard web file type extension 
     *    (called fileTypeExtension in ERDDAP, e.g., .asc) for the output
     */
    public OutputStreamFromHttpResponseViaAwsS3(HttpServletRequest tRequest,
        HttpServletResponse tResponse, String tCacheDir, String tFileName, String tFileType, 
        String tExtension) {

        request = tRequest;
        response = tResponse;
        cacheDir = File2.addSlash(tCacheDir);
        fileName = tFileName;
        fileType = tFileType;
        extension = tExtension;

//EEEK! How should this handle range requests?  Best to disallow range requests??
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
     * @return a buffered outputStream.  If outputStream has already been created,
     *   the same one is returned.
     * @throws Throwable if trouble
     */
    public OutputStream outputStream(String characterEncoding, long tLength) 
        throws Throwable {

        if (outputStream != null) 
            return outputStream;

        //make an OutpuStreamViaAwsS3 which has special close() method
        return outputStream = new OutputStreamViaAwsS3(this);
    }

    /** 
     * After ouputStream() has been called, this indicates the encoding (compression)
     * being used for an OutputStreamFromHttpResponse (gzip, deflate) 
     * or "identity" if no compression.
     */
    public String usingCompression() {
//!!! deal with this
        return usingCompression;
    }

    /**
     * This returns the outputStream if it has already been created (else null).
     */
    public OutputStream existingOutputStream() {
        return outputStream;
    }

}



