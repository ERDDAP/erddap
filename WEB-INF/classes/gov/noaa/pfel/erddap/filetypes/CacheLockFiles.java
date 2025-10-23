package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.OutputStreamFromHttpResponse;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public abstract class CacheLockFiles extends FileTypeInterface {
  private boolean isNcTypeHeader;

  public CacheLockFiles(boolean isNcTypeHeader) {
    this.isNcTypeHeader = isNcTypeHeader;
  }

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    writeToStream(requestInfo, false);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    writeToStream(requestInfo, true);
  }

  private void writeToStream(DapRequestInfo requestInfo, boolean isGrid) throws Throwable {
    // *** get the data and write to a tableWriter
    EDD edd = requestInfo.edd();
    String fileName = requestInfo.fileName();

    // *** make a file (then copy it to outputStream)
    // If update system active or real_time=true, don't cache anything.  Make all files unique.
    if (edd.getUpdateEveryNMillis() > 0 || edd.realTime()) {
      fileName += "_U" + System.currentTimeMillis(); // useful because it identifies time of request
      requestInfo.outputStream().setFileName(fileName);
    }

    // nc files are handled this way because .ncHeader .ncCFHeader, .ncCFMAHeader
    //  need to call NcHelper.ncdump(aRealFile, "-h").
    String fileTypeExtension =
        edd.fileTypeExtension(requestInfo.language(), requestInfo.fileTypeName());
    String fullName = requestInfo.dir() + fileName + fileTypeExtension;
    // Normally, this is cacheDirectory and it already exists,
    //  but my testing environment (2+ things running) may have removed it.
    File2.makeDirectory(requestInfo.dir());
    String cacheFullName =
        String2.canonical(isNcTypeHeader ? requestInfo.dir() + fileName + ".nc" : fullName);

    int random = Math2.random(Integer.MAX_VALUE);

    // thread-safe creation of the file
    // (If there are almost simultaneous requests for the same one, only one thread will make it.)
    ReentrantLock lock = String2.canonicalLock(cacheFullName);
    if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
      throw new TimeoutException("Timeout waiting for lock on EDDTable .ncHeader cacheFullName.");
    try {

      if (File2.isFile(cacheFullName)) { // don't 'touch()'; files for latest data will change
        if (EDD.verbose) String2.log("  reusing cached " + cacheFullName);
      } else {
        if (isGrid) {
          generateGridFile(requestInfo, cacheFullName);
        } else {
          generateTableFile(requestInfo, cacheFullName);
        }
      }
    } finally {
      lock.unlock();
    }

    if (isNcTypeHeader) {
      // thread-safe creation of the file
      // (If there are almost simultaneous requests for the same one, only one thread will make it.)
      fullName = String2.canonical(fullName);
      ReentrantLock lock2 = String2.canonicalLock(fullName);
      if (!lock2.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
        throw new TimeoutException("Timeout waiting for lock on EDDTable ncXHeader fullName.");
      try {

        if (!File2.isFile(fullName)) {
          String error =
              File2.writeToFileUtf8(
                  fullName + random,
                  NcHelper.ncdump(
                      cacheFullName,
                      "-h")); // !!!this doesn't do anything to internal " in a String attribute
          // value.
          if (error.length() == 0) {
            File2.rename(fullName + random, fullName); // make available in an instant
            File2.isFile(
                fullName,
                5); // for possible waiting thread, wait till file is visible via operating system
          } else {
            throw new RuntimeException(error);
          }
        }
      } finally {
        lock2.unlock();
      }
    }

    // copy file to ...
    if (EDStatic.config.awsS3OutputBucketUrl == null) {

      // copy file to outputStream
      // (I delayed getting actual outputStream as long as possible.)
      try (OutputStream out =
          requestInfo
              .outputStream()
              .outputStream(
                  isNcTypeHeader
                      ? File2.UTF_8
                      : requestInfo.fileTypeName().equals(".kml") ? File2.UTF_8 : "")) {
        if (!File2.copy(fullName, out)) {
          // outputStream contentType already set,
          // so I can't go back to html and display error message
          // note than the message is thrown if user cancels the transmission; so don't email to me
          throw new SimpleException(String2.ERROR + " while transmitting file.");
        }
      }
      // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
    } else {

      // copy file to AWS and redirect user
      String contentType =
          OutputStreamFromHttpResponse.getFileContentType(
              requestInfo.fileTypeName(), fileTypeExtension);
      String fullAwsUrl =
          EDStatic.config.awsS3OutputBucketUrl + File2.getNameAndExtension(fullName);
      SSR.uploadFileToAwsS3(
          EDStatic.config.awsS3OutputTransferManager, fullName, fullAwsUrl, contentType);
      requestInfo.response().sendRedirect(fullAwsUrl);
    }
  }

  protected abstract void generateTableFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable;

  protected abstract void generateGridFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable;
}
