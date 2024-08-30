/*
 * OutputStreamViaAwsS3 Copyright 2020, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * OutputStreamViaAwsS3 writes to a file in an S3 bucket, then redirects user to download that file.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2020-12-08
 */
public class OutputStreamViaAwsS3 extends BufferedOutputStream {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  private OutputStreamFromHttpResponseViaAwsS3 parent;
  private String characterEncoding;
  private String fullLocalFileName;

  /**
   * The constructor.
   *
   * @param tParent the OutputStreamFromHttpResponseViaAwsS3 that created this
   */
  public OutputStreamViaAwsS3(
      OutputStreamFromHttpResponseViaAwsS3 tParent, String tCharacterEncoding) throws IOException {

    // make the superclass's BufferedOutputStream from an OutputStream
    super(new FileOutputStream(tParent.localDir + tParent.fileName + tParent.extension));
    parent = tParent;
    characterEncoding = tCharacterEncoding;
    fullLocalFileName = tParent.localDir + tParent.fileName + tParent.extension;
  }

  /**
   * This is useful for OutputStream types that support fileName if you want to change the download
   * fileName before the call to getOutputStream().
   */
  public void setFileName(String tFileName) {
    parent.setFileName(tFileName); // used in fullAwsUrl below
  }

  /**
   * This overwrites the super's close() method, closing the local buffered FileOutputStream,
   * copying the file to AWS, and redirecting the user to it.
   */
  @Override
  public void close() throws IOException {

    // close the local Buffered FileOutputStream
    super.close();

    // get and apply the fileTypeInfo
    // 2020-12-07 this is the section that was inline but now uses a static method
    Object fileTypeInfo[] =
        OutputStreamFromHttpResponse.getFileTypeInfo(
            parent.request, parent.fileType, parent.extension);
    String contentType = (String) fileTypeInfo[0];
    HashMap headerMap = (HashMap) fileTypeInfo[1];
    boolean genericCompressed =
        ((Boolean) fileTypeInfo[2]).booleanValue(); // true for generic compressed files, e.g., .zip
    boolean otherCompressed =
        ((Boolean) fileTypeInfo[3])
            .booleanValue(); // true for app specific compressed (but not audio/ image/ video)

    // copy to AWS bucket
    // tell Aws about other file attributes when file accessed as from web site
    String fullAwsUrl = EDStatic.awsS3OutputBucketUrl + parent.fileName + parent.extension;
    SSR.uploadFileToAwsS3(
        EDStatic.awsS3OutputTransferManager, fullLocalFileName, fullAwsUrl, contentType);

    // EDStatic.awsS3OutputClient.putObject(objectRequest, localPath);

    // set "file" settings for AWS file
    // do all the things associated with committing to writing data to the outputStream
    // setContentType(mimeType)
    // User won't want hassle of saving file (changing fileType, ...).

    //        response.setContentType(contentType);
    //        Iterator it = headerMap.keySet().iterator();
    //        while (it.hasNext()) {
    //            String key = (String)it.next();
    //            response.setHeader(key, (String)headerMap.get(key));
    //        }

    //        //set the characterEncoding
    //        if (characterEncoding != null && characterEncoding.length() > 0)
    //            response.setCharacterEncoding(characterEncoding);

    //        //specify the file's name  (this encourages showing File Save As dialog box in user's
    // browser)
    //        if (showFileSaveAs(genericCompressed, fileType, extension))
    //            response.setHeader("Content-Disposition","attachment;filename=" +
    //                fileName + extension);

    // redirect user to file in AWS bucket
    if (verbose) {
      // String2.log("OutputStreamFromHttpResponse charEncoding=" + characterEncoding +
      //    ", encoding=" + usingCompression + ", " + parent.fileName + ", " + parent.extension);

      // log header information
      // Enumeration en = request.getHeaderNames();
      // while (en.hasMoreElements()) {
      //    String name = en.nextElement().toString();
      //    String2.log("  request header " + name + "=" + request.getHeader(name));
      // }
    }
    parent.response.sendRedirect(fullAwsUrl);
  }
}
