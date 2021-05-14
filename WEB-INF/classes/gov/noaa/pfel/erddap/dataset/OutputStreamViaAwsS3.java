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
import java.nio.file.FileSystem;
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

    private BufferedOutputStream localFileBOS = null;
    private OutputStreamFromHttpResponseViaAwsS3 parent;

    /**
     * The constructor.
     *
     * @param tParent the OutputStreamFromHttpResponseViaAwsS3 that created this
     */
    public OutputStreamViaAwsS3(OutputStreamFromHttpResponseViaAwsS3 tParent) throws IOException {

        //make the superclass's BufferedOutputStream from a FileOutputStream
        super(new FileOutputStream(
            tParent.cacheDir + tParent.fileName + tParent.extension));  //duplicated below
        parent = tParent;
    }

       
    /**
     * This overwrites the super's close() method, closing the buffered FileOutputStream,
     * copying the file to AWS, and redirecting the user to it.
     */
    public void close() throws IOException {

        //close the Buffered FileOutputStream
        super.close(); 
/*

        //get and apply the fileTypeInfo
        //2020-12-07 this is the section that was inline but now uses the static methods above
        Object fileTypeInfo[] = getFileTypeInfo(request, fileType, extension);
        String  contentType       =  (String) fileTypeInfo[0];
        HashMap headerMap         =  (HashMap)fileTypeInfo[1];
        boolean genericCompressed = ((Boolean)fileTypeInfo[2]).booleanValue();  //true for generic compressed files, e.g., .zip
        boolean otherCompressed   = ((Boolean)fileTypeInfo[3]).booleanValue();  //true for app specific compressed (but not audio/ image/ video)
        
        //copy to AWS bucket
        //see https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html
        String localName = tParent.cacheDir + tParent.fileName + tParent.extension; //duplicated above
        Path path = FileSystems.getDefault().getPath(localName);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(EDStatic.awsS3OutputBucket)
            .key(tParent.fileName + tParent.extension)
            .contentLength(File2.length(localName))
            .contentType(contentType)
            //
            .build();

        EDStatic.awsS3OutputClient.putObject(objectRequest, localPath); 

        //set "file" settings for AWS file
        //do all the things associated with committing to writing data to the outputStream
        //setContentType(mimeType)
        //User won't want hassle of saving file (changing fileType, ...).



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


        //redirect user to file in AWS bucket

        if (verbose) {
            String2.log("OutputStreamFromHttpResponse charEncoding=" + characterEncoding + 
                ", encoding=" + usingCompression + ", " + fileName + ", " + extension);

            //log header information
            //Enumeration en = request.getHeaderNames();
            //while (en.hasMoreElements()) {
            //    String name = en.nextElement().toString();
            //    String2.log("  request header " + name + "=" + request.getHeader(name));
            //}
        }
*/
            
    }

}



