/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import com.google.common.collect.ImmutableList;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** File2 has useful static methods for working with files. */
public class File2 {

  //    public final static String  CHARSET = "charset";       //the name  of the charset att
  public static final String ISO_8859_1 =
      "ISO-8859-1"; // the value of the charset att and usable in Java code
  public static final String ISO_8859_1_LC = ISO_8859_1.toLowerCase();
  public static final Charset ISO_8859_1_CHARSET = StandardCharsets.ISO_8859_1;
  public static final String ENCODING = "_Encoding"; // the name  of the _Encoding att
  public static final String UTF_8 =
      "UTF-8"; // a   value of the _Encoding att and usable in Java code
  public static final String UTF_8_LC = UTF_8.toLowerCase();
  public static final Charset UTF_8_CHARSET = StandardCharsets.UTF_8;
  public static final String UNABLE_TO_DELETE = "unable to delete"; // so consistent in log.txt

  private static String webInfParentDirectory;

  // define the file types for the purpose of assigning icon in Table.directoryListing
  // compressed and image ext from wikipedia
  // many ext from http://www.fileinfo.com/filetypes/common
  public static final ImmutableList<String> BINARY_EXT =
      ImmutableList.of(
          ".accdb",
          ".bin",
          ".bufr",
          ".cab",
          ".cdf",
          ".cer",
          ".class",
          ".cpi",
          ".csr",
          ".db",
          ".dbf",
          ".dll",
          ".dmp",
          ".drv",
          ".dwg",
          ".dxf",
          ".fnt",
          ".fon",
          ".grb",
          ".grib",
          ".grib2",
          ".ini",
          ".keychain",
          ".lnk",
          ".mat",
          ".mdb",
          ".mim",
          ".nc",
          ".otf",
          ".pdb",
          ".prf",
          ".sys",
          ".ttf");
  public static final ImmutableList<String> COMPRESSED_EXT =
      ImmutableList.of(
          ".7z",
          ".a",
          ".ace",
          ".afa",
          ".alz",
          ".apk",
          ".ar",
          ".arc",
          ".arj",
          ".ba",
          ".bh",
          ".bz2",
          ".cab",
          ".cfs",
          ".cpio",
          ".dar",
          ".dd",
          ".deb",
          ".dgc",
          ".dmg",
          ".f",
          ".gca",
          ".gho",
          ".gz",
          ".gzip",
          ".ha",
          ".hki",
          ".hqx",
          ".infl",
          ".iso",
          ".j",
          ".jar",
          ".kgb",
          ".kmz",
          ".lbr",
          ".lha",
          ".lz",
          ".lzh",
          ".lzma",
          ".lzo",
          ".lzx",
          ".mar",
          ".msi",
          ".partimg",
          ".paq6",
          ".paq7",
          ".paq8",
          ".pea",
          ".pim",
          ".pit",
          ".pkg",
          ".qda",
          ".rar",
          ".rk",
          ".rpm",
          ".rz",
          ".s7z",
          ".sda",
          ".sea",
          ".sen",
          ".sfark",
          ".sfx",
          ".shar",
          ".sit",
          ".sitx",
          ".sqx",
          ".tar",
          ".tbz2",
          ".tgz",
          ".tlz",
          ".toast",
          ".torrent",
          ".uca",
          ".uha",
          ".uue",
          ".vcd",
          ".war",
          ".wim",
          ".xar",
          ".xp3",
          ".xz",
          ".yz1",
          ".z",
          ".zip",
          ".zipx",
          ".zoo");
  public static final ImmutableList<String> IMAGE_EXT =
      ImmutableList.of(
          ".ai",
          ".bmp",
          ".cgm",
          ".draw",
          ".drw",
          ".gif",
          ".ico",
          ".jfif",
          ".jpeg",
          ".jpg",
          ".pbm",
          ".pgm",
          ".png",
          ".pnm",
          ".ppm",
          ".pspimage",
          ".raw",
          ".svg",
          ".thm",
          ".tif",
          ".tiff",
          ".webp",
          ".yuv");
  public static final ImmutableList<String> COMPRESSED_IMAGE_EXT =
      ImmutableList.of( // done quickly, the obvious compressed
          ".gif", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".webp");
  public static final ImmutableList<String> LAYOUT_EXT =
      ImmutableList.of(
          ".doc", ".docx", ".indd", ".key", ".pct", ".pps", ".ppt", ".pptx", ".psd", ".qxd", ".qxp",
          ".rels", ".rtf", ".wpd", ".wps", ".xlr", ".xls", ".xlsx");
  public static final ImmutableList<String> MOVIE_EXT =
      ImmutableList.of(
          ".3g2", ".3gp", ".asf", ".asx", ".avi", ".fla", ".flv", ".mov", ".mp4", ".mpg", ".ogv",
          ".rm", ".swf", ".vob", ".wmv", ".webm");
  public static final ImmutableList<String> PDF_EXT = ImmutableList.of(".pdf");
  public static final ImmutableList<String> PS_EXT = ImmutableList.of(".eps", ".ps");
  public static final ImmutableList<String> SCRIPT_EXT =
      ImmutableList.of( // or executable
          ".app", ".asp", ".bat", ".cgi", ".com", ".csh", ".exe", ".gadget", ".js", ".jsp", ".ksh",
          ".php", ".pif", ".pl", ".py", ".sh", ".tcsh", ".vb", ".wsf");
  public static final ImmutableList<String> SOUND_EXT =
      ImmutableList.of(
          ".aif", ".aiff", ".aifc", ".au", ".flac", ".iff", ".m3u", ".m4a", ".mid", ".mp3", ".mpa",
          ".ogg", ".wav", ".wave", ".wma");
  public static final ImmutableList<String> COMPRESSED_SOUND_EXT =
      ImmutableList.of( // done quickly, the obvious compressed
          ".flac", ".iff", ".m3u", ".m4a", ".mp3", ".mpa", ".ogg", ".wma");
  public static final ImmutableList<String> TEXT_EXT =
      ImmutableList.of(
          ".asc", ".c", ".cpp", ".cs", ".csv", ".das", ".dat", ".dds", ".java", ".json", ".log",
          ".m", ".sdf", ".sql", ".tsv", ".txt", ".vcf");
  public static final ImmutableList<String> WORLD_EXT =
      ImmutableList.of(".css", ".htm", ".html", ".xhtml");
  public static final ImmutableList<String> XML_EXT =
      ImmutableList.of(".dtd", ".gpx", ".kml", ".xml", ".rss");

  /**
   * The image file names in ERDDAP's /images/ for the different file types. These may change
   * periodically. These parallel ICON_ALT.
   */
  public static final ImmutableList<String> ICON_FILENAME =
      ImmutableList.of(
          "generic.gif",
          "index.gif",
          "binary.gif",
          "compressed.gif",
          "image2.gif",
          "layout.gif",
          "movie.gif",
          "pdf.gif",
          "ps.gif",
          "script.gif",
          "sound1.gif",
          "text.gif",
          "world1.gif",
          "xml.gif");

  /**
   * The 3 character string used for <img alt=...> for the different file types. I hope these won't
   * change (other than adding new types). These are from Apache directory listings. These parallel
   * ICON_FILENAME
   */
  public static final ImmutableList<String> ICON_ALT =
      ImmutableList.of(
          "UNK", "IDX", "BIN", "ZIP", "IMG", "DOC", "MOV", "PDF", "PS", "EXE", "SND", "TXT", "WWW",
          "XML");

  /**
   * These are the extensions of file types that netcdf-java makes additional index files for. This
   * is not a perfect list. I think GRIB and BUFR files can have other or no extension.
   */
  public static final ImmutableList<String> NETCDF_INDEX_EXT =
      ImmutableList.of(".grb", ".grib", ".grib2", ".bufr");

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  private static String tempDirectory; // lazy creation by getSystemTempDirectory

  private static ConcurrentHashMap<String, S3Client> s3ClientMap =
      new ConcurrentHashMap<String, S3Client>();

  public static String getClassPath() {
    String find = "/com/cohort/util/String2.class";
    // use this.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
    String classPath = String2.class.getResource(find).getFile();
    int po = classPath.indexOf(find);
    classPath = classPath.substring(0, po + 1);

    // on windows, remove the troublesome leading "/"
    if (String2.OSIsWindows
        && classPath.length() > 2
        && classPath.charAt(0) == '/'
        && classPath.charAt(2) == ':') classPath = classPath.substring(1);

    // classPath is a URL! so spaces are encoded as %20 on Windows!
    // UTF-8: see https://en.wikipedia.org/wiki/Percent-encoding#Current_standard
    try {
      classPath = URLDecoder.decode(classPath, StandardCharsets.UTF_8);
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
    }

    return classPath;
  }

  /**
   * This returns the directory that is the tomcat application's root (with forward slashes and a
   * trailing slash, e.g., c:/programs/_tomcat/webapps/cwexperimental/). Tomcat calls this the
   * ContextDirectory. This only works if these classes are installed underneath Tomcat (with
   * "WEB-INF/" the start of things to be removed from classPath).
   *
   * @return the parent directory of WEB-INF (with / separator and / at the end) or "ERROR if
   *     trouble
   * @throws RuntimeException if trouble
   */
  private static String lookupWebInfParentDirectory() {
    String classPath = getClassPath();
    int po = classPath.indexOf("/WEB-INF/");
    if (po < 0)
      throw new RuntimeException(
          String2.ERROR + ": '/WEB-INF/' not found in classPath=" + classPath);
    classPath = classPath.substring(0, po + 1);
    Path path;
    if (classPath.startsWith("file:/")) {
      path = Paths.get(URI.create(classPath));
    } else {
      path = Paths.get(classPath);
    }
    return path.toAbsolutePath().toString().replace("\\", "/") + "/";
  }

  public static String getWebInfParentDirectory() {
    if (webInfParentDirectory == null) {
      webInfParentDirectory = lookupWebInfParentDirectory();
    }
    return webInfParentDirectory;
  }

  public static void setWebInfParentDirectory(String webInfParentDir) {
    webInfParentDirectory = webInfParentDir.replace("\\", "/");
  }

  /**
   * Access a classpath resource via a filesystem path. NOTE: this will not work unless resource is
   * exploded.
   *
   * @param resourcePath Classpath of resource.
   * @return Filesystem path.
   * @throws URISyntaxException Could not create URI.
   */
  public static String accessResourceFile(String resourcePath) throws URISyntaxException {
    if (resourcePath.startsWith("file:/")) {
      return Paths.get(new URI(resourcePath)).toString();
    } else {
      return Paths.get(resourcePath).toString();
    }
  }

  /**
   * This indicates if the named file is indeed an existing local file. AWS S3 files don't count as
   * local here. If dir="", it just says it isn't a file.
   *
   * @param fullName the full name of the file
   * @return true if the file exists
   */
  public static boolean isFile(String fullName) {
    try {
      // String2.log("File2.isFile: " + fullName);
      File file = new File(fullName);
      return file.isFile();
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.isFile(" + fullName + ")", e));
      return false;
    }
  }

  /**
   * This indicates if the named file is an existing AWS S3 file and you have access to it.
   *
   * @param awsUrl the URL of the file
   * @return true if the file exists
   */
  public static boolean isAwsS3File(String awsUrl) {
    return length(awsUrl) >= 0;
  }

  //    "tl;dr; It's faster to list objects with prefix being the full key path, than to use HEAD to
  // find out of a object is in an S3 bucket."
  //    says https://www.peterbe.com/plog/fastest-way-to-find-out-if-a-file-exists-in-s3
  // So use FileVisitorDNLS.isAwsS3File().
  // And I never got the code below to work correctly
  /*    try {
          String bro[] = String2.parseAwsS3Url(awsUrl);
          if (bro == null) {
              return false;
          } else {
              //I think deleteMarker means "marked for deletion"
              HeadObjectResponse response = getS3Client(bro[1]).headObject(HeadObjectRequest.builder().bucket(bro[0]).key(bro[2]).build());
              if (response == null) {
                  String2.log("response=null");
                  return false;
              }
              Boolean marker = response.deleteMarker();
              if (marker == null) { //it isn't marked for deletion
                  String2.log("marker=null");
                  return true;
              }
              return !marker.booleanValue();
          }
      } catch (NoSuchKeyException nske) { //object doesn't exist
          return false;
      } catch (Exception e) { //unexpected errors
          if (verbose) String2.log(MustBe.throwable("File2.isAwsS3File(" + awsUrl + ")", e));
          return false;
      }
  }
  */

  /**
   * This returns an index into ICON_FILENAME and ICON_ALT suitable for the fileName. The fallback
   * is "UNK".
   *
   * @param fileName
   * @return an index into ICON_FILENAME and ICON_ALT suitable for the fileName.
   */
  public static int whichIcon(String fileName) {
    String fileNameLC = fileName.toLowerCase();
    String extLC = getExtension(fileNameLC);

    if (fileNameLC.equals("index.html") || fileNameLC.equals("index.htm"))
      return ICON_ALT.indexOf("IDX");
    if (BINARY_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("BIN");
    if (COMPRESSED_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("ZIP");
    if (IMAGE_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("IMG");
    if (LAYOUT_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("DOC");
    if (MOVIE_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("MOV");
    if (PDF_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("PDF");
    if (PS_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("PS ");
    if (SCRIPT_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("EXE");
    if (SOUND_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("SND");
    if (TEXT_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("TXT");
    if (WORLD_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("WWW");
    if (XML_EXT.indexOf(extLC) >= 0) return ICON_ALT.indexOf("XML");

    return ICON_ALT.indexOf("UNK");
  }

  public static boolean isCompressedExtension(String ext) {
    if (!String2.isSomething(ext)) return false;
    ext = ext.toLowerCase();
    return COMPRESSED_EXT.indexOf(ext) >= 0
        || COMPRESSED_IMAGE_EXT.indexOf(ext) >= 0
        || MOVIE_EXT.indexOf(ext) >= 0
        || COMPRESSED_SOUND_EXT.indexOf(ext) >= 0;
  }

  /**
   * For newly created files, this tries a few times to wait for the file to be accessible via the
   * operating system.
   *
   * @param fullName the full name of the file
   * @param nTimes the number of times to try (e.g., 5) with 200ms sleep between tries
   * @return true if the file exists
   */
  public static boolean isFile(String fullName, int nTimes) {
    try {
      // String2.log("File2.isFile: " + fullName);
      File file = new File(fullName);
      boolean b = false;
      nTimes = Math.max(1, nTimes);

      for (int i = 0; i < nTimes; i++) {
        b = file.isFile();
        if (b) {
          return true;
        } else if (i < nTimes - 1) {
          Math2.sleep(200);
        }
      }
      return b;
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.isFile(" + fullName + ")", e));
      return false;
    }
  }

  /**
   * This indicates if the named directory is indeed an existing directory.
   *
   * @param dir the full name of the directory
   * @return true if the file exists
   */
  public static boolean isDirectory(String dir) {
    try {
      // String2.log("File2.isFile: " + dir);
      File d = new File(dir);
      return d.isDirectory();
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.isDirectory(" + dir + ")", e));
      return false;
    }
  }

  /**
   * This deletes the specified file or directory (must be empty).
   *
   * @param fullName the full name of the file
   * @return true if the file existed and was successfully deleted; otherwise returns false. This
   *     won't throw an exception.
   */
  public static boolean delete(String fullName) {
    // This can have problems if another thread is reading the file, so try repeatedly.
    // Unlike other places, this is often part of delete/rename,
    //  so we want to know when it is done ASAP.
    int maxAttempts = String2.OSIsWindows ? 11 : 4;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        File file = new File(fullName);
        if (!file.exists())
          return attempt > 1; // if attempt=1, nothing to delete; if attempt>1, there was something
        // I think Linux deletes no matter what.
        // I think Windows won't delete file if in use by another thread or pending action(?).
        boolean result = file.delete();
        file = null; // don't hang on to it
        if (result) {
          // take it at its word (file has been or will soon be deleted)
          // In Windows, file may continue to be isFile() for a short time.
          return true;
        } else {
          // 2009-02-16 I had wierd problems on Windows with File2.delete not deleting when it
          // should be able to.
          // 2011-02-18 problem comes and goes (varies from run to run), but still around.
          // Windows often sets result=false on first attempt in some of my unit tests.
          // Notably, when creating cwwcNDBCMet on local erddap.
          // I note (from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4045014)
          //  that Win (95?) won't delete an open file, but solaris will.
          //  But I think the files I'm working with have been closed.
          // Solution? call Math2.gc instead of Math2.sleep
          if (attempt == maxAttempts) {
            String2.log(
                String2.ERROR
                    + ": File2.delete was "
                    + UNABLE_TO_DELETE
                    + " "
                    + fullName
                    + "\n"
                    + MustBe.getStackTrace());
            return result;
          }
          String2.log(
              "WARNING #"
                  + attempt
                  + ": File2.delete is having trouble. It will try again to delete "
                  + fullName
              // + "\n" + MustBe.stackTrace()
              );
          if (attempt % 4 == 1)
            // The problem might be that something needs to be gc'd.
            Math2.gcAndWait(
                "File2.delete (before retry)"); // By experiment, gc works better than sleep.
          else Math2.sleep(1000);
        }

      } catch (Exception e) {
        if (verbose) String2.log(MustBe.throwable("File2.delete(" + fullName + ")", e));
        return false;
      }
    }
    return false; // won't get here
  }

  /**
   * This just tries once to delete the file or directory (must be empty).
   *
   * @param fullName the full name of the file
   * @return true if the file existed and was successfully deleted; otherwise returns false.
   */
  public static boolean simpleDelete(String fullName) {
    // This can have problems if another thread is reading the file, so try repeatedly.
    // Unlike other places, this is often part of delete/rename,
    //  so we want to know when it is done ASAP.
    try {
      File file = new File(fullName);
      if (!file.exists()) return false; // it didn't exist
      return file.delete();
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.simpleDelete(" + fullName + ")", e));
      return false;
    }
  }

  /**
   * This deletes all files in the specified directory (not subdirectories). If the dir isn't a
   * directory, nothing happens.
   *
   * @param dir the full name of the directory
   * @return the return value from the underlying deleteIfOld
   */
  public static int deleteAllFiles(String dir) {
    return deleteIfOld(dir, Long.MAX_VALUE, false, false);
  }

  /**
   * This deletes all files in the specified directory. See also
   * gov.noaa.pfel.coastwatch.util.RegexFilenameFilter.recursiveDelete(). If the dir isn't a
   * directory, nothing happens.
   *
   * @param dir the full name of the directory
   * @param recursive if true, subdirectories are searched, too
   * @param deleteEmptySubdirectories this is only used if recursive is true
   * @return the return value from the underlying deleteIfOld
   */
  public static int deleteAllFiles(
      String dir, boolean recursive, boolean deleteEmptySubdirectories) {
    return deleteIfOld(dir, Long.MAX_VALUE, recursive, deleteEmptySubdirectories);
  }

  /**
   * This deletes the files in the specified directory if the last modified time is older than the
   * specified time.
   *
   * @param dir the full name of the main directory
   * @param time System.currentTimeMillis of oldest file to be kept. Files will a smaller
   *     lastModified will be deleted.
   * @param recursive if true, subdirectories are searched, too
   * @param deleteEmptySubdirectories this is only used if recursive is true
   * @return number of files that remain (or -1 if trouble). This won't throw an exception if
   *     trouble.
   */
  public static int deleteIfOld(
      String dir, long time, boolean recursive, boolean deleteEmptySubdirectories) {
    try {
      String msg = String2.ERROR + ": File2.deleteIfOld is " + UNABLE_TO_DELETE + " ";
      File file = new File(dir);

      // make sure it is an existing directory
      if (!file.isDirectory()) {
        String2.log(String2.ERROR + " in File2.deleteIfOld: dir=" + dir + " isn't a directory.");
        return -1;
      }

      // go through the files and delete old ones
      File files[] = file.listFiles();
      // String2.log(">> File2.deleteIfOld dir=" + dir + " nFiles=" + files.length);
      int nRemain = 0;
      int nDir = 0;
      for (int i = 0; i < files.length; i++) {
        // String2.log(">> File2.deleteIfOld files[" + i + "]=" + files[i].getAbsolutePath());
        try {
          if (files[i].isFile()) {
            if (files[i].lastModified() < time) {
              if (!files[i].delete()) {
                // unable to delete
                String2.log(msg + files[i].getCanonicalPath());
                nRemain = -1;
              }
            } else if (nRemain != -1) { // once nRemain is -1, it isn't changed
              nRemain++;
            }
          } else if (recursive && files[i].isDirectory()) {
            nDir++;
            int tnRemain =
                deleteIfOld(files[i].getAbsolutePath(), time, recursive, deleteEmptySubdirectories);
            // String2.log(">> File2.deleteIfOld might delete this dir. tnRemain=" + tnRemain);
            if (tnRemain == -1) nRemain = -1;
            else {
              if (nRemain != -1) // once nRemain is -1, it isn't changed
              nRemain += tnRemain;
              if (tnRemain == 0 && deleteEmptySubdirectories) {
                files[i].delete();
              }
            }
          }
        } catch (Exception e) {
          try {
            nRemain = -1;
            String2.log(msg + files[i].getCanonicalPath());
          } catch (Exception e2) {
          }
        }
      }
      int nDeleted = files.length - nDir + nRemain;
      if (nDir != 0 || nDeleted != 0 || nRemain != 0)
        String2.log(
            "File2.deleteIfOld("
                + String2.left(dir, 55)
                + (time == Long.MAX_VALUE
                    ? ""
                    : ", " + Calendar2.safeEpochSecondsToIsoStringTZ(time / 1000.0, "" + time))
                + ") nDir="
                + String2.right("" + nDir, 4)
                + " nDeleted="
                + String2.right("" + nDeleted, 4)
                + " nRemain="
                + String2.right(nRemain < 0 ? String2.ERROR : "" + nRemain, 4));
      return nRemain;
    } catch (Exception e3) {
      String2.log(
          MustBe.throwable(String2.ERROR + " in File2.deleteIfOld(" + dir + ", " + time + ")", e3));
      return -1;
    }
  }

  /**
   * This deletes an AWS file.
   *
   * @param awsUrl
   * @return true if the file existed and was successfully deleted; otherwise returns false.
   */
  public static boolean deleteAwsS3File(String awsUrl) {
    // from
    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html#delete-object
    String bro[] = String2.parseAwsS3Url(awsUrl);
    if (bro == null) return false;
    try {
      getS3Client(bro[1])
          .deleteObject(DeleteObjectRequest.builder().bucket(bro[0]).key(bro[2]).build());
      return true;
    } catch (Exception e) {
      String2.log("Caught exception while deleting " + awsUrl + " : " + e.toString());
      return false;
    }
    // even with response, no easy way to determine if successfull
    // DeleteObjectResponse response = getS3Client(bro[1]).deleteObject(deleteObjectRequest);
  }

  /**
   * This renames the specified file. If the dir+newName file already exists, it will be deleted.
   *
   * @param dir the directory containing the file (with a trailing slash)
   * @param oldName the old name of the file
   * @param newName the new name of the file
   * @throws RuntimeException if trouble
   */
  public static void rename(String dir, String oldName, String newName) throws RuntimeException {
    rename(dir + oldName, dir + newName);
  }

  /**
   * This renames the specified file. If the fullNewName file already exists, it will be deleted
   * before the renaming. The files must be in the same directory.
   *
   * @param fullOldName the complete old name of the file
   * @param fullNewName the complete new name of the file
   * @throws RuntimeException if trouble
   */
  public static void rename(String fullOldName, String fullNewName) throws RuntimeException {
    File oldFile = new File(fullOldName);
    if (!oldFile.isFile())
      throw new RuntimeException(
          "Unable to rename\n"
              + fullOldName
              + " to\n"
              + fullNewName
              + "\nbecause source file doesn't exist.");

    // delete any existing file with destination name
    File newFile = new File(fullNewName);
    if (newFile.isFile()) {
      // It may try a few times.
      // Since we know file exists, !delete really means it couldn't be deleted; take result at its
      // word.
      if (!delete(fullNewName))
        throw new RuntimeException(
            "Unable to rename\n"
                + fullOldName
                + " to\n"
                + fullNewName
                + "\nbecause "
                + UNABLE_TO_DELETE
                + " an existing file with destinationName.");

      // In Windows, file may be isFile() for a short time. Give it time to delete.
      if (String2.OSIsWindows)
        Math2.sleep(Math2.shortSleep); // if Windows: encourage successful file deletion
    }
    // rename
    if (oldFile.renameTo(newFile)) return;

    // failed? give it a second try. This fixed a problem in a test on Windows.
    // The problem might be that something needs to be gc'd.
    Math2.gcAndWait("File2.rename (before retry)");
    if (oldFile.renameTo(newFile)) return;

    // This is a bad idea, but its better than datasets failing to load.
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      String2.log("Was sleeping to allow file handles time to free, but got interrupted.");
    }
    Math2.gcAndWait("File2.rename (before retry)");

    if (oldFile.renameTo(newFile)) return;

    if (!oldFile.canWrite()) {
      throw new RuntimeException(
          "Unable to rename\n"
              + fullOldName
              + " to\n"
              + fullNewName
              + "\nbecause the source file is not writable.");
    }

    throw new RuntimeException("Unable to rename\n" + fullOldName + " to\n" + fullNewName);
  }

  /**
   * This is like rename(), but won't throw an exception if trouble.
   *
   * @param fullOldName the complete old name of the file
   * @param fullNewName the complete new name of the file
   */
  public static void safeRename(String fullOldName, String fullNewName) {
    try {
      rename(fullOldName, fullNewName);
    } catch (Throwable t) {
    }
  }

  /**
   * This renames fullOldName to fullNewName if fullNewName doesn't exist. <br>
   * If fullNewName does exist, this just deletes fullOldName (and doesn't touch() fullNewName. <br>
   * The files must be in the same directory. <br>
   * This is used when more than one thread may be creating the same fullNewName with same content
   * (and where last step is rename tempFile to newName).
   *
   * @param fullOldName the complete old name of the file
   * @param fullNewName the complete new name of the file
   * @throws RuntimeException if trouble
   */
  public static void renameIfNewDoesntExist(String fullOldName, String fullNewName)
      throws RuntimeException {
    if (isFile(fullNewName))
      // in these cases, fullNewName may be in use, so rename will fail because can't delete
      delete(fullOldName);
    else rename(fullOldName, fullNewName);
  }

  /**
   * If the directory or file exists, this changes its lastModification date/time to the current
   * date and time. (The name comes from the Unix "touch" program.)
   *
   * @param fullName the full name of the file
   * @return true if the directory or file exists and if the modification was successful
   */
  public static boolean touch(String fullName) {
    return touch(fullName, 0);
  }

  /**
   * If the directory or file exists, this changes its lastModification date/time to the current
   * date and time minus millisInPast. (The name comes from the Unix "touch" program.)
   *
   * @param fullName the full name of the file
   * @param millisInPast
   * @return true if the directory or file exists and if the modification was successful
   */
  public static boolean touch(String fullName, long millisInPast) {
    try {
      File file = new File(fullName);
      // The Java documentation for setLastModified doesn't state
      // if the method returns false if the file doesn't exist
      // or if the method creates a 0 byte file (as does Unix's touch).
      // But tests show that it returns false if !exists, so no need to test.
      // if (!file.exists()) return false;
      return file.setLastModified(System.currentTimeMillis() - millisInPast);
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.touch(" + fullName + ")", e));
      return false;
    }
  }

  /**
   * If the directory or file exists, this changes its lastModification date/time to millis
   *
   * @param fullName the full name of the file
   * @param millis
   * @return true if the directory or file exists and if the modification was successful
   */
  public static boolean setLastModified(String fullName, long millis) {
    try {
      File file = new File(fullName);
      // The Java documentation for setLastModified doesn't state
      // if the method returns false if the file doesn't exist
      // or if the method creates a 0 byte file (as does Unix's touch).
      // But tests show that it returns false if !exists, so no need to test.
      // if (!file.exists()) return false;
      return file.setLastModified(millis);
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.setLastModified(" + fullName + ")", e));
      return false;
    }
  }

  /**
   * This returns the length of the named file (or -1 if trouble).
   *
   * @param fullName the full name of the file or AWS S3 object
   * @return the length of the named file (or -1 if trouble).
   */
  public static long length(String fullName) {
    try {
      String bro[] = String2.parseAwsS3Url(fullName);
      if (bro == null) {
        // String2.log("File2.isFile: " + fullName);
        File file = new File(fullName);
        if (!file.isFile()) return -1;
        return file.length();
      } else {
        // isAwsS3Url
        return getS3Client(bro[1])
            .headObject(HeadObjectRequest.builder().bucket(bro[0]).key(bro[2]).build())
            .contentLength()
            .longValue();
      }
    } catch (NoSuchKeyException nske) { // if aws key/object doesn't exist
      return -1;
    } catch (Exception e) {
      if (verbose) String2.log(MustBe.throwable("File2.length(" + fullName + ")", e));
      return -1;
    }
  }

  /**
   * Get the number of millis since the start of the Unix epoch when the file was last modified.
   *
   * @param fullName the full name of the file
   * @return the time (millis since the start of the Unix epoch) the file was last modified (or 0 if
   *     trouble)
   */
  public static long getLastModified(String fullName) {
    try {
      String bro[] = String2.parseAwsS3Url(fullName);
      if (bro == null) {
        File file = new File(fullName);
        return file.lastModified();
      } else {
        // isAwsS3Url
        return getS3Client(bro[1])
            .headObject(HeadObjectRequest.builder().bucket(bro[0]).key(bro[2]).build())
            .lastModified()
            .toEpochMilli();
      }

    } catch (Exception e) {
      // pause and try again
      try {
        // The problem might be that something needs to be gc'd.
        Math2.gcAndWait(
            "File2.getLastModified (before retry)"); // if trouble getting lastModified: gc
        // encourages success
        File file = new File(fullName);
        return file.lastModified();
      } catch (Exception e2) {
        if (verbose) String2.log(MustBe.throwable("File2.getLastModified(" + fullName + ")", e2));
        return 0;
      }
    }
  }

  /** This returns the name of the oldest file in the list. Error if fullNames.length == 0. */
  public static String getOldest(String fullNames[]) {
    int ti = 0;
    long tm = getLastModified(fullNames[0]);
    for (int i = 1; i < fullNames.length; i++) {
      long ttm = getLastModified(fullNames[i]);
      if (ttm != 0 && ttm < tm) {
        ti = i;
        tm = ttm;
      }
    }
    return fullNames[ti];
  }

  /** This returns the name of the youngest file in the list. Error if fullNames.length == 0. */
  public static String getYoungest(String fullNames[]) {
    int ti = 0;
    long tm = getLastModified(fullNames[0]);
    for (int i = 1; i < fullNames.length; i++) {
      long ttm = getLastModified(fullNames[i]);
      if (ttm != 0 && ttm > tm) {
        ti = i;
        tm = ttm;
      }
    }
    return fullNames[ti];
  }

  /**
   * This returns the protocol+domain (without a trailing slash) from a URL.
   *
   * @param url a full or partial URL.
   * @return the protocol+domain (without a trailing slash). If no protocol in URL, that's okay.
   *     null returns null. "" returns "".
   */
  public static String getProtocolDomain(String url) {
    if (url == null) return url;

    int urlLength = url.length();
    int po = url.indexOf('/');
    if (po < 0) return url;
    if (po == urlLength - 1 || po > 7)
      // protocol names are short e.g., http:// .  Perhaps it's coastwatch.pfeg.noaa.gov/
      return url.substring(0, po);
    if (url.charAt(po + 1) == '/') {
      if (po == urlLength - 2)
        // e.g., http://
        return url;
      // e.g., http://a.com/
      po = url.indexOf('/', po + 2);
      return po >= 0 ? url.substring(0, po) : url;
    } else {
      // e.g., www.a.com/...
      return url.substring(0, po);
    }
  }

  /**
   * This returns the directory info (with a trailing slash) from the fullName).
   *
   * @param fullName the full name of the file. It can have forward or backslashes.
   * @return the directory (or "" if none; 2020-01-10 was currentDirectory)
   */
  public static String getDirectory(String fullName) {
    int po = fullName.lastIndexOf('/');
    if (po < 0) po = fullName.lastIndexOf('\\');
    return po > 0 ? fullName.substring(0, po + 1) : "";
  }

  /**
   * This returns the current directory (with the proper separator at the end).
   *
   * @return the current directory (with the proper separator at the end)
   */
  public static String getCurrentDirectory() {
    String dir = System.getProperty("user.dir");

    if (!dir.endsWith(File.separator)) dir += File.separator;

    return dir;
  }

  /**
   * This removes the directory info (if any) from the fullName, and so returns just the name and
   * extension.
   *
   * @param fullName the full name of the file. It can have forward or backslashes.
   * @return the name and extension of the file (may be "")
   */
  public static String getNameAndExtension(String fullName) {
    int po = fullName.lastIndexOf('/');
    if (po >= 0) return fullName.substring(po + 1);

    po = fullName.lastIndexOf('\\');
    return po >= 0 ? fullName.substring(po + 1) : fullName;
  }

  /**
   * This returns just the extension from the file's name (the last "." and anything after, e.g.,
   * ".asc").
   *
   * @param fullName the full name or just name of the file. It can have forward or backslashes.
   * @return the extension of the file (perhaps "")
   */
  public static String getExtension(String fullName) {
    String name = getNameAndExtension(fullName);
    int po = name.lastIndexOf('.');
    return po >= 0 ? name.substring(po) : "";
  }

  /**
   * This removes the extension (if there is one) from the file's name (the last "." and anything
   * after, e.g., ".asc").
   *
   * @param fullName the full name or just name of the file. It can have forward or backslashes.
   * @return the extension of the file (perhaps "")
   */
  public static String removeExtension(String fullName) {
    String ext = getExtension(fullName);
    return fullName.substring(0, fullName.length() - ext.length());
  }

  /**
   * This replaces the existing extension (if any) with ext.
   *
   * @param fullName the full name or just name of the file. It can have forward or backslashes.
   * @param ext the new extension (e.g., ".das")
   * @return the fullName with the new ext
   */
  public static String forceExtension(String fullName, String ext) {
    String oldExt = getExtension(fullName);
    return fullName.substring(0, fullName.length() - oldExt.length()) + ext;
  }

  /**
   * This removes the directory info (if any) and extension (after the last ".", if any) from the
   * fullName, and so returns just the name.
   *
   * @param fullName the full name of the file. It can have forward or backslashes.
   * @return the name of the file
   */
  public static String getNameNoExtension(String fullName) {
    String name = getNameAndExtension(fullName);
    String extension = getExtension(fullName);
    return name.substring(0, name.length() - extension.length());
  }

  /**
   * This returns true if the file is compressed and decompressible via getDecompressedInputStream.
   *
   * @param ext The file's extension, e.g., .gz
   */
  public static boolean isDecompressible(String ext) {

    // this exactly parallels getDecompressedInputStream
    return ext.equals(".Z")
        || (ext.indexOf('z') >= 0
            && (ext.equals(".tgz")
                || ext.equals(".gz")
                || // includes .tar.gz
                ext.equals(".gzip")
                || // includes .tar.gzip
                ext.equals(".zip")
                || ext.equals(".bz2")));
  }

  /**
   * This gets a new S3Client for the specified region.
   *
   * @param region
   * @return an S3Client
   */
  public static S3Client getS3Client(String region) {
    // This code is based on
    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html#list-object
    //  was v1.1 https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingObjectKeysUsingJava.html
    // Yes, S3Client is thread safe and reuse is encouraged.
    //  Notably, the same ssl connection is reused if requests close together in time (so more
    // efficient).
    //  see https://github.com/aws/aws-sdk-cpp/issues/266
    S3Client s3Client = s3ClientMap.get(region);
    if (s3Client == null) {
      // I'm not being super careful with concurrency here because 2 initial
      //  simultaneous calls for the same region are unlikely and it doesn't
      //  matter if 2 are made initially (and only 1 kept ultimately).
      // Presumably, this throws exceptions if trouble (eg unknown/invalid region).
      // Presumably, the s3Client is smart enough to fix itself if there is trouble
      //  (or there's nothing to fix since it's just a specification).
      s3Client =
          S3Client.builder()
              // was .credentials(ProfileCredentialsProvider.create()) //now it uses default
              // credentials
              .region(Region.of(region))
              .build();
      s3ClientMap.put(region, s3Client);
    }
    return s3Client;
  }

  /**
   * This is a variant of getBufferedInputStream that gets the entire file (firstByte=0 and
   * lastByte=-1).
   */
  public static BufferedInputStream getBufferedInputStream(String fullFileName) throws Exception {
    return getBufferedInputStream(fullFileName, 0, -1);
  }

  /**
   * This gets a (not decompressed), buffered InputStream from a file or S3 URL. If the file is
   * compressed, it is assumed to be the only file (entry) in the archive.
   *
   * @param fullFileName The full file name or S3 URL.
   * @param firstByte usually 0, but &gt;=0 for a byte range request
   * @param lastByte usually -1 (last available), but a specific number (inclusive) for a byte range
   *     request.
   * @return a buffered InputStream from a file or S3 URL, ready to read firstByte.
   * @throws Exception if trouble
   */
  public static BufferedInputStream getBufferedInputStream(
      String fullFileName, long firstByte, long lastByte) throws Exception {
    String ext = getExtension(fullFileName); // if e.g., .tar.gz, this returns .gz

    // is it an AWS S3 object?
    String bro[] = String2.parseAwsS3Url(fullFileName); // [bucket, region, objectKey]
    InputStream is = null;
    if (bro == null) {
      // no, it's a regular file
      is = new FileInputStream(fullFileName);
      skipFully(is, firstByte);
    } else {
      // yes, it's an AWS S3 object. Get it.
      // example:
      // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html#download-object
      // example:
      // https://www.javacodemonk.com/aws-java-sdk-2-s3-file-upload-download-spring-boot-c1a3e072
      // This code may throw SdkClientException.
      GetObjectRequest.Builder builder = GetObjectRequest.builder().bucket(bro[0]).key(bro[2]);
      if (firstByte > 0 || lastByte != -1)
        // yes 'bytes=', not space. see
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
        builder.range("bytes=" + firstByte + "-" + (lastByte < 0 ? "" : "" + lastByte));
      is = getS3Client(bro[1]).getObject(builder.build());
    }

    // Buffer it.  Recommended by https://commons.apache.org/proper/commons-compress/examples.html
    // 1MB buffer makes no difference
    return new BufferedInputStream(is);
  }

  /**
   * This gets a decompressed, buffered InputStream from a file or S3 URL. If the file is
   * compressed, it is assumed to be the only file (entry) in the archive.
   *
   * @param fullFileName The full file name. If it ends in .tgz, .tar.gz, .tar.gzip, .gz, .gzip,
   *     .zip, or .bz2, this returns a decompressed, buffered InputStream.
   * @return a decompressed, buffered InputStream from a file.
   * @throws Exception if trouble
   */
  public static InputStream getDecompressedBufferedInputStream(String fullFileName, InputStream is)
      throws Exception {
    String ext = getExtension(fullFileName); // if e.g., .tar.gz, this returns .gz

    // !!!!! IF CHANGE SUPPORTED COMPRESSION TYPES, CHANGE isDecompressible ABOVE !!!

    // handle .Z (capital Z) specially first
    if (ext.equals(".Z")) {
      try {
        return new ZCompressorInputStream(is);
      } catch (Exception e) {
        is.close();
        throw e;
      }
    }

    // everything caught below has a z in ext
    if (ext.indexOf('z') < 0) return is;

    if (ext.equals(".tgz")
        || fullFileName.endsWith(".tar.gz")
        || fullFileName.endsWith(".tar.gzip")) {
      // modified from
      // https://stackoverflow.com/questions/7128171/how-to-compress-decompress-tar-gz-files-in-java
      GzipCompressorInputStream gzipIn = null;
      TarArchiveInputStream tarIn = null;
      try {
        gzipIn = new GzipCompressorInputStream(is);
        tarIn = new TarArchiveInputStream(gzipIn);
        TarArchiveEntry entry = tarIn.getNextTarEntry();
        while (entry != null && entry.isDirectory()) entry = tarIn.getNextTarEntry();
        if (entry == null)
          throw new IOException(
              String2.ERROR + " while reading " + fullFileName + ": no file found in archive.");
        is = tarIn;
      } catch (Exception e) {
        if (tarIn != null) tarIn.close();
        else if (gzipIn != null) gzipIn.close();
        else is.close();
        throw e;
      }

    } else if (ext.equals(".gz") || ext.equals(".gzip")) {
      try {
        is = new GzipCompressorInputStream(is);
      } catch (Exception e) {
        is.close();
        throw e;
      }

    } else if (ext.equals(".zip")) {
      ZipInputStream zis = null;
      try {
        zis = new ZipInputStream(is);
        ZipEntry entry = zis.getNextEntry();
        while (entry != null && entry.isDirectory()) entry = zis.getNextEntry();
        if (entry == null)
          throw new IOException(
              String2.ERROR + " while reading " + fullFileName + ": no file found in archive.");
        is = zis;
      } catch (Exception e) {
        if (zis != null) zis.close();
        else is.close();
        throw e;
      }

    } else if (ext.equals(".bz2")) {
      try {
        is = new BZip2CompressorInputStream(is);
      } catch (Exception e) {
        is.close();
        throw e;
      }
    }
    // .7z is possible but different and harder

    // !!!!! IF CHANGE SUPPORTED COMPRESSION TYPES, CHANGE isDecompressible ABOVE !!!

    return is;
  }

  /**
   * This gets a decompressed, buffered InputStream from a file or S3 URL. If the file is
   * compressed, it is assumed to be the only file (entry) in the archive.
   *
   * @param fullFileName The full file name. If it ends in .tgz, .tar.gz, .tar.gzip, .gz, .gzip,
   *     .zip, or .bz2, this returns a decompressed, buffered InputStream.
   * @return a decompressed, buffered InputStream from a file.
   * @throws Exception if trouble
   */
  public static InputStream getDecompressedBufferedInputStream(String fullFileName)
      throws Exception {
    InputStream is = getBufferedInputStream(fullFileName); // starting point before decompression
    return getDecompressedBufferedInputStream(fullFileName, is);
  }

  /**
   * This gets a decompressed, buffered InputStream from a file or S3 URL. If the file is
   * compressed, it is assumed to be the only file (entry) in the archive.
   *
   * @param resourceFile The full file name. If it ends in .tgz, .tar.gz, .tar.gzip, .gz, .gzip,
   *     .zip, or .bz2, this returns a decompressed, buffered InputStream.
   * @return a decompressed, buffered InputStream from a file.
   * @throws Exception if trouble
   */
  public static InputStream getDecompressedBufferedInputStream(URL resourceFile) throws Exception {
    return getDecompressedBufferedInputStream(resourceFile.getFile(), resourceFile.openStream());
  }

  public static void decompressAllFiles(String sourceFullName, String destDir) throws IOException {
    String ext = getExtension(sourceFullName); // if e.g., .tar.gz, this returns .gz
    // !!!!! IF CHANGE SUPPORTED COMPRESSION TYPES, CHANGE isDecompressible ABOVE
    // !!!

    int bufferSize = 1024;

    // handle .Z (capital Z) specially first
    // This assumes Z files contain only 1 file.
    if (ext.equals(".Z")) {
      FileOutputStream out = null;
      ZCompressorInputStream zIn = null;
      try {
        out = new FileOutputStream(destDir);
        zIn =
            new ZCompressorInputStream(
                new BufferedInputStream(new FileInputStream(sourceFullName)));
        final byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = zIn.read(buffer))) {
          out.write(buffer, 0, n);
        }
      } catch (Exception e) {
        throw e;
      } finally {
        if (out != null) {
          out.close();
        }
        if (zIn != null) {
          zIn.close();
        }
      }
    }

    // everything caught below has a z in ext
    if (ext.indexOf('z') < 0) {
      return;
    }

    if (ext.equals(".tgz")
        || sourceFullName.endsWith(".tar.gz")
        || sourceFullName.endsWith(".tar.gzip")) {
      // This can actually have multiple files.
      GzipCompressorInputStream gzipIn = null;
      TarArchiveInputStream tarIn = null;
      try {
        gzipIn =
            new GzipCompressorInputStream(
                new BufferedInputStream(new FileInputStream(sourceFullName)));
        tarIn = new TarArchiveInputStream(gzipIn);
        ArchiveEntry entry;
        while ((entry = tarIn.getNextEntry()) != null) {
          if (entry.isDirectory()) {
            File f = newFile(destDir, entry.getName());
            boolean created = f.mkdir();
            if (!created) {
              String2.log(
                  "Unable to create directory '%s', during extraction of archive contents.\n"
                      + f.getAbsolutePath());
            }
          } else {
            int count;
            byte data[] = new byte[bufferSize];
            FileOutputStream fos = new FileOutputStream(newFile(destDir, entry.getName()), false);
            try (BufferedOutputStream dest = new BufferedOutputStream(fos, bufferSize)) {
              while ((count = tarIn.read(data, 0, bufferSize)) != -1) {
                dest.write(data, 0, count);
              }
            }
          }
        }

      } catch (Exception e) {
        if (tarIn != null) tarIn.close();
        else if (gzipIn != null) gzipIn.close();
        throw e;
      } finally {
        if (gzipIn != null) {
          gzipIn.close();
        }
        if (tarIn != null) {
          tarIn.close();
        }
      }

    } else if (ext.equals(".gz") || ext.equals(".gzip")) {
      try (GZIPInputStream gzipInputStream =
          new GZIPInputStream(new FileInputStream(sourceFullName))) {
        File outputFile = new File(destDir, getFileNameWithoutExtension(sourceFullName));
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
          byte[] buffer = new byte[1024];
          int len;
          while ((len = gzipInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, len);
          }
        }
      } catch (IOException e) {
        throw e;
      }
    } else if (ext.equals(".zip")) {
      // This can actually have multiple files.
      byte[] buffer = new byte[bufferSize];
      ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFullName));
      try {
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
          File newFile = newFile(destDir, zipEntry.getName());
          if (zipEntry.isDirectory()) {
            if (!newFile.isDirectory() && !newFile.mkdirs()) {
              throw new IOException("Failed to create directory " + newFile);
            }
          } else {
            // fix for Windows-created archives
            File parent = newFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
              throw new IOException("Failed to create directory " + parent);
            }

            // write file content
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
            fos.close();
          }
          zipEntry = zis.getNextEntry();
        }
      } catch (Exception e) {
        throw e;
      } finally {
        zis.closeEntry();
        zis.close();
      }

    } else if (ext.equals(".bz2")) {
      OutputStream out = null;
      BZip2CompressorInputStream bzIn = null;

      try {
        out = Files.newOutputStream(Paths.get(destDir));
        bzIn =
            new BZip2CompressorInputStream(
                new BufferedInputStream(Files.newInputStream(Paths.get(sourceFullName))));
        final byte[] buffer = new byte[bufferSize];
        int n = 0;
        while (-1 != (n = bzIn.read(buffer))) {
          out.write(buffer, 0, n);
        }
      } finally {
        if (out != null) {
          out.close();
        }
        if (bzIn != null) {
          bzIn.close();
        }
      }
    }
    // .7z is possible but different and harder

    // !!!!! IF CHANGE SUPPORTED COMPRESSION TYPES, CHANGE isDecompressible ABOVE
    // !!!

  }

  private static String getFileNameWithoutExtension(String filePath) {
    int lastIndexOfDot = filePath.lastIndexOf(".");
    if (lastIndexOfDot > 0) {
      return filePath.substring(0, lastIndexOfDot);
    } else {
      return filePath;
    }
  }

  private static File newFile(String destinationDir, String name) throws IOException {
    Path destBase = Paths.get(destinationDir);
    Path namePath = Paths.get(name);

    if (namePath.getName(0).equals(destBase.getName(destBase.getNameCount() - 1))) {
      namePath = namePath.subpath(1, namePath.getNameCount());
    }
    File destFile = new File(destBase.resolve(namePath).toString());

    String destDirPath = destBase.toAbsolutePath().toString();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + name);
    }

    return destFile;
  }

  public static BufferedReader getDecompressedBufferedFileReader88591(String fullFileName)
      throws Exception {
    return getDecompressedBufferedFileReader(fullFileName, ISO_8859_1);
  }

  public static BufferedReader getDecompressedBufferedFileReaderUtf8(String fullFileName)
      throws Exception {
    return getDecompressedBufferedFileReader(fullFileName, UTF_8);
  }

  /**
   * This creates a buffered, decompressed (e.g., from .gz file) FileReader.
   *
   * @param fullFileName the full file name
   * @return a buffered FileReader
   * @throws IOException if trouble
   */
  public static BufferedReader getDecompressedBufferedFileReader(
      String fullFileName, String charset) throws Exception {

    InputStream is = getDecompressedBufferedInputStream(fullFileName);
    try {
      return new BufferedReader(
          new InputStreamReader(
              is,
              String2.isSomething(charset)
                  ? charset
                  : ISO_8859_1)); // invalid charset throws exception
    } catch (Exception e) {
      try {
        if (is != null) is.close();
      } catch (Exception e2) {
      }
      throw e;
    }
  }

  /** This calls directReadFromFile with charset= UTF_8 */
  public static String directReadFromUtf8File(String fileName) throws Exception {
    return directReadFromFile(fileName, UTF_8);
  }

  /** This calls directReadFromFile with charset= ISO_8859_1 */
  public static String directReadFrom88591File(String fileName) throws Exception {
    return directReadFromFile(fileName, ISO_8859_1);
  }

  /**
   * This reads the bytes of the file with the specified charset. This does not alter the characters
   * (e.g., the line endings).
   *
   * <p>This method is generally appropriate for small and medium-sized files. For very large files
   * or files that need additional processing, it may be better to write a custom method to read the
   * file line-by-line, processing as it goes.
   *
   * @param fileName is the (usually canonical) path (dir+name) for the file
   * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
   * @return a String with the decoded contents of the file.
   * @throws Exception if trouble
   */
  public static String directReadFromFile(String fileName, String charset) throws Exception {

    // declare the BufferedReader variable
    // declare the results variable: String results[] = {"", ""};
    // BufferedReader and results are declared outside try/catch so
    // that they can be accessed from within either try/catch block.
    long time = System.currentTimeMillis();
    BufferedReader br = getDecompressedBufferedFileReader(fileName, charset);
    StringBuilder sb = new StringBuilder(8192);
    try {

      // get the text from the file
      char buffer[] = new char[8192];
      int nRead;
      while ((nRead = br.read(buffer)) >= 0) // -1 = end-of-file
      sb.append(buffer, 0, nRead);
      return sb.toString();
    } finally {
      try {
        br.close();
      } catch (Exception e) {
      }
    }
  }

  /**
   * This is a variant of readFromFile that uses the ISO_8859_1 character set and 3 tries (1 second
   * apart) to read the file.
   */
  public static String[] readFromFile88591(String fileName) {
    return readFromFile(fileName, ISO_8859_1, 3);
  }

  /**
   * This is a variant of readFromFile that uses the UTF_8 character set and 3 tries (1 second
   * apart) to read the file.
   */
  public static String[] readFromFileUtf8(String fileName) {
    return readFromFile(fileName, UTF_8, 3);
  }

  /**
   * This is a variant of readFromFile that uses the specified character set and 3 tries (1 second
   * apart) to read the file.
   */
  public static String[] readFromFile(String fileName, String charset) {
    return readFromFile(fileName, charset, 3);
  }

  /**
   * This reads the text contents of the specified text file.
   *
   * <p>This method uses try/catch to ensure that all possible exceptions are caught and returned as
   * the error String (throwable.toString()).
   *
   * <p>This method is generally appropriate for small and medium-sized files. For very large files
   * or files that need additional processing, it may be better to write a custom method to read the
   * file line-by-line, processing as it goes.
   *
   * @param fileName is the (usually canonical) path (dir+name) for the file
   * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
   * @param maxAttempt e.g. 3 (the tries are 1 second apart)
   * @return a String array with two strings. Using a String array gets around Java's limitation of
   *     only returning one value from a method. String #0 is an error String (or "" if no error).
   *     String #1 has the contents of the file as one big string (with any end-of-line characters
   *     converted to \n). If the error String is not "", String #1 may not have all the contents of
   *     the file. ***This ensures that the last character in the file (if any) is \n. This behavior
   *     varies from other implementations of readFromFile.
   */
  public static String[] readFromFile(String fileName, String charset, int maxAttempt) {

    // declare the BufferedReader variable
    // declare the results variable: String results[] = {"", ""};
    // BufferedReader and results are declared outside try/catch so
    // that they can be accessed from within either try/catch block.
    long time = System.currentTimeMillis();
    BufferedReader br = null;
    String results[] = {"", ""};
    int errorIndex = 0;
    int contentsIndex = 1;

    try {
      // open the file
      // To deal with problems in multithreaded apps
      // (when deleting and renaming files, for an instant no file with that name exists),
      maxAttempt = Math.max(1, maxAttempt);
      for (int attempt = 1; attempt <= maxAttempt; attempt++) {
        try {
          br = getDecompressedBufferedFileReader(fileName, charset);
        } catch (Exception e) {
          if (attempt == maxAttempt) {
            String2.log(String2.ERROR + ": File2.readFromFile was unable to read " + fileName);
            throw e;
          } else {
            String2.log(
                "WARNING #"
                    + attempt
                    + ": File2.readFromFile is having trouble. It will try again to read "
                    + fileName);
            if (attempt == 1)
              Math2.gcAndWait(
                  "File2.readFromFile (before retry)"); // trouble! Give OS/Java a time and gc to
            // deal with trouble
            else Math2.sleep(1000);
          }
        }
      }

      // get the text from the file
      // This uses bufferedReader.readLine() to repeatedly
      // read lines from the file and thus can handle various
      // end-of-line characters.
      // The lines (with \n added at the end) are added to a
      // StringBuilder.
      StringBuilder sb = new StringBuilder(8192);
      String s = br.readLine();
      while (s != null) { // null = end-of-file
        sb.append(s);
        sb.append('\n');
        s = br.readLine();
      }

      // save the contents as results[1]
      results[contentsIndex] = sb.toString();

    } catch (Exception e) {
      results[errorIndex] = MustBe.throwable("fileName=" + fileName, e);
    } finally {
      try {
        if (br != null) br.close();
      } catch (Exception e2) {
      }
    }

    // return results
    // log("  File2.readFromFile " + fileName + " time=" +
    //    (System.currentTimeMillis() - time) + "ms");
    return results;
  }

  /**
   * This is like the other readFromFile, but returns ArrayList of Strings and throws Exception is
   * trouble. The strings in the ArrayList are not canonical! So this is useful for reading,
   * processing, and throwing away.
   *
   * <p>This method is generally appropriate for small and medium-sized files. For very large files
   * or files that need additional processing, it may be more efficient to write a custom method to
   * read the file line-by-line, processing as it goes.
   *
   * @param fileName is the (usually canonical) path (dir+name) for the file
   * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
   * @param maxAttempt e.g. 3 (the tries are 1 second apart)
   * @return ArrayList with the lines from the file
   * @throws Exception if trouble
   */
  public static ArrayList<String> readLinesFromFile(String fileName, String charset, int maxAttempt)
      throws Exception {

    long time = System.currentTimeMillis();
    BufferedReader bufferedReader = null;
    try {
      for (int i = 0; i < maxAttempt; i++) {
        try {
          bufferedReader = getDecompressedBufferedFileReader(fileName, charset);
          break; // success
        } catch (RuntimeException e) {
          if (i == maxAttempt - 1) throw e;
          Math2.sleep(100);
        }
      }
      ArrayList<String> al = new ArrayList();
      String s = bufferedReader.readLine();
      while (s != null) { // null = end-of-file
        al.add(s);
        s = bufferedReader.readLine();
      }
      return al;
    } finally {
      if (bufferedReader != null) bufferedReader.close();
    }
  }

  /**
   * This is like the other readFromFile, but returns ArrayList of Strings and throws Exception is
   * trouble. The strings in the ArrayList are not canonical! So this is useful for reading,
   * processing, and throwing away.
   *
   * <p>This method is generally appropriate for small and medium-sized files. For very large files
   * or files that need additional processing, it may be more efficient to write a custom method to
   * read the file line-by-line, processing as it goes.
   *
   * @param resourceFile URL of the file to be read
   * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
   * @param maxAttempt e.g. 3 (the tries are 1 second apart)
   * @return ArrayList with the lines from the file
   * @throws Exception if trouble
   */
  public static ArrayList<String> readLinesFromFile(
      URL resourceFile, String charset, int maxAttempt) throws Exception {

    long time = System.currentTimeMillis();
    BufferedReader bufferedReader = null;
    try {
      for (int i = 0; i < maxAttempt; i++) {
        try {
          InputStream is = getDecompressedBufferedInputStream(resourceFile);
          bufferedReader = new BufferedReader(new InputStreamReader(is, charset));
          break; // success
        } catch (RuntimeException e) {
          if (i == maxAttempt - 1) throw e;
          Math2.sleep(100);
        }
      }
      ArrayList<String> al = new ArrayList();
      String s = bufferedReader.readLine();
      while (s != null) { // null = end-of-file
        al.add(s);
        s = bufferedReader.readLine();
      }
      return al;
    } finally {
      if (bufferedReader != null) bufferedReader.close();
    }
  }

  /*
  Here is a skeleton for more direct control of reading text from a file:
      BufferedReader bufferedReader = getDecompressedBufferedFileReader(fileName, charset);
      try {
          String s;
          while ((s = bufferedReader.readLine()) != null) { //null = end-of-file
              //do something with s
              //for example, split at whitespace: String fields[] = s.split("\\s+"); //s = whitespace regex
              }

          bufferedReader.close();
      } catch (Exception e) {
          System.err.println(error + "while reading file '" + filename + "':\n" + e);
          e.printStackTrace(System.err);
          bufferedReader.close();
      }
  */

  /**
   * Creating a buffered FileWriter this way helps me check that charset is set. (Instead of the
   * default charset used by "new FileWriter()").
   */
  public static BufferedWriter getBufferedFileWriter88591(String fullFileName) throws IOException {
    return getBufferedFileWriter(fullFileName, ISO_8859_1_CHARSET);
  }

  /**
   * Creating a buffered FileWriter this way helps me check that charset is set. (Instead of the
   * default charset used by "new FileWriter()").
   */
  public static BufferedWriter getBufferedFileWriterUtf8(String fullFileName) throws IOException {
    return getBufferedFileWriter(fullFileName, UTF_8_CHARSET);
  }

  /**
   * Creating a buffered FileWriter this way helps me check that charset is set. (Instead of the
   * default charset used by "new File(outputStream)").
   *
   * @param charset Must not be "" or null.
   * @return the BufferedWriter
   */
  public static BufferedWriter getBufferedFileWriter(String fullFileName, String charset)
      throws IOException {
    return getBufferedFileWriter(fullFileName, Charset.forName(charset));
  }

  /**
   * Creating a buffered FileWriter this way helps me check that charset is set (instead of using
   * the default charset).
   *
   * @param fullFileName Must be a local file, not AWS S3 object.
   * @param charset Must not be "" or null.
   * @return the BufferedWriter
   */
  public static BufferedWriter getBufferedFileWriter(String fullFileName, Charset charset)
      throws IOException {
    return getBufferedWriter(new FileOutputStream(fullFileName), charset);
  }

  /**
   * Creating a buffered outputStreamWriter this way helps me check that charset is set. (Instead of
   * the default charset used by "new OutputStreamWriter(outputStream)").
   *
   * @return the BufferedWriter
   */
  public static BufferedWriter getBufferedWriter88591(OutputStream os) {
    return getBufferedWriter(os, ISO_8859_1_CHARSET);
  }

  /**
   * Creating a buffered outputStreamWriter this way helps me check that charset is set. (Instead of
   * the default charset used by "new OutputStreamWriter(outputStream)").
   *
   * @return the BufferedWriter
   */
  public static BufferedWriter getBufferedWriterUtf8(OutputStream os) {
    return getBufferedWriter(os, UTF_8_CHARSET);
  }

  /**
   * Creating a buffered outputStreamWriter this way helps me check that charset is set. (Instead of
   * the default charset used by "new OutputStreamWriter(outputStream)").
   *
   * @param os the OutputStream
   * @param charset Must not be "" or null.
   * @return the BufferedWriter
   */
  public static BufferedWriter getBufferedWriter(OutputStream os, String charset)
      throws UnsupportedEncodingException {
    return getBufferedWriter(os, Charset.forName(charset));
  }

  /**
   * Creating a buffered outputStreamWriter this way helps me check that charset is set. (Instead of
   * the default charset used by "new OutputStreamWriter(outputStream)").
   *
   * @param os the OutputStream
   * @param charset Must not be null.
   * @return the BufferedWriter
   */
  public static BufferedWriter getBufferedWriter(OutputStream os, Charset charset) {
    return new BufferedWriter(new OutputStreamWriter(os, charset));
  }

  /**
   * This saves some text in a file named fileName. This uses the default character encoding
   * (ISO-8859-1).
   *
   * <p>This method uses try/catch to ensure that all possible exceptions are caught and returned as
   * the error String (throwable.toString()).
   *
   * <p>This method is generally appropriate for small and medium-sized files. For very large files
   * or files that need additional processing, it may be more efficient to write a custom method to
   * read the file line-by-line, processing as it goes.
   *
   * @param fileName is the (usually canonical) path (dir+name) for the file
   * @param contents has the text that will be written to the file. contents must use \n as the
   *     end-of-line marker. Currently, this method purposely does not convert \n to the
   *     operating-system-appropriate end-of-line characters when writing to the file (see
   *     lineSeparator).
   * @param charset e.g., File2.ISO_8859_1 or File2.UTF_8
   * @return an error message (or "" if no error).
   */
  public static String writeToFile(String fileName, String contents, String charset) {
    return lowWriteToFile(fileName, contents, charset, "\n", false);
  }

  public static String writeToFile88591(String fileName, String contents) {
    return lowWriteToFile(fileName, contents, ISO_8859_1, "\n", false);
  }

  public static String writeToFileUtf8(String fileName, String contents) {
    return lowWriteToFile(fileName, contents, UTF_8, "\n", false);
  }

  /**
   * This is like writeToFile, but it appends the text if the file already exists. If the file
   * doesn't exist, it makes a new file.
   */
  public static String appendFile(String fileName, String contents, String charset) {
    return lowWriteToFile(fileName, contents, charset, "\n", true);
  }

  public static String appendFile88591(String fileName, String contents) {
    return lowWriteToFile(fileName, contents, ISO_8859_1, "\n", true);
  }

  public static String appendFileUtf8(String fileName, String contents) {
    return lowWriteToFile(fileName, contents, UTF_8, "\n", true);
  }

  /**
   * This provides services to writeToFile and appendFile. If there is an error and !append, the
   * partial file is deleted.
   *
   * @param fileName is the (usually canonical) path (dir+name) for the file
   * @param contents has the text that will be written to the file. contents must use \n as the
   *     end-of-line marker. Currently, this method purposely does not convert \n to the
   *     operating-system-appropriate end-of-line characters when writing to the file (see
   *     lineSeparator).
   * @param charset e.g., UTF-8; or null or "" for the default (ISO-8859-1)
   * @param lineSeparator is the desired lineSeparator for the outgoing file.
   * @param append if you want to append any existing fileName; otherwise any existing file is
   *     deleted first.
   * @return an error message (or "" if no error).
   */
  private static String lowWriteToFile(
      String fileName, String contents, String charset, String lineSeparator, boolean append) {

    // bufferedWriter and error are declared outside try/catch so
    // that they can be accessed from within either try/catch block.
    BufferedWriter bufferedWriter = null;
    String error = "";

    try {
      // open the file
      // This uses a BufferedWriter wrapped around a FileWriter
      // to write the information to the file.
      if (charset == null || charset.length() == 0) charset = ISO_8859_1;
      bufferedWriter = getBufferedWriter(new FileOutputStream(fileName, append), charset);

      // convert \n to operating-system-specific lineSeparator
      if (!lineSeparator.equals("\n")) contents = String2.replaceAll(contents, "\n", lineSeparator);
      // since the first String is a regex, you can use "[\\n]" too

      // write the text to the file
      bufferedWriter.write(contents);

      // test speed
      // int start = 0;
      // while (start < contents.length()) {
      //    bufferedWriter.write(contents.substring(start, Math.min(start+39, contents.length())));
      //    start += 39;
      // }

    } catch (Exception e) {
      error = e.toString();
    }

    // make sure bufferedWriter is closed
    try {
      if (bufferedWriter != null) bufferedWriter.close();
    } catch (Exception e) {
      if (error.length() == 0) error = e.toString();
      // else ignore the error (the first one is more important)
    }

    // and delete partial file if error and not appending
    if (error.length() > 0 && !append) delete(fileName);

    return error;
  }

  /**
   * This generates a hex dump of the first nBytes of the file.
   *
   * @param fullFileName
   * @param nBytes
   * @return a String with a hex dump of the first nBytes of the file.
   * @throws Exception if trouble
   */
  public static String hexDump(String fullFileName, int nBytes) throws Exception {
    InputStream fis = getDecompressedBufferedInputStream(fullFileName);
    try {
      nBytes = Math.min(nBytes, Math2.narrowToInt(length(fullFileName))); // max 2GB
      byte ba[] = new byte[nBytes];
      int bytesRead = 0;
      while (bytesRead < nBytes) bytesRead += fis.read(ba, bytesRead, nBytes - bytesRead);
      return String2.hexDump(ba);
    } finally {
      fis.close();
    }
  }

  /**
   * This returns the byte# at which the two files are different (or -1 if same).
   *
   * @param fullFileName1
   * @param fullFileName2
   * @return byte# at which the two files are different (or -1 if same).
   */
  public static long whereDifferent(String fullFileName1, String fullFileName2) {

    long length1 = length(fullFileName1);
    long length2 = length(fullFileName2);
    long length = Math.min(length1, length2);
    InputStream bis1 = null, bis2 = null;
    long po = 0;
    try {
      bis1 = getDecompressedBufferedInputStream(fullFileName1);
      bis2 = getDecompressedBufferedInputStream(fullFileName2);
      for (po = 0; po < length; po++) {
        if (bis1.read() != bis2.read()) break;
      }
    } catch (Exception e) {
      String2.log(
          String2.ERROR
              + " in whereDifferent(\n1:"
              + fullFileName1
              + "\n2:"
              + fullFileName2
              + "\n"
              + MustBe.throwableToString(e));
    }
    try {
      if (bis1 != null) bis1.close();
    } catch (Exception e) {
    }
    try {
      if (bis2 != null) bis2.close();
    } catch (Exception e) {
    }

    if (po < length) return po;
    if (length1 != length2) return length;
    return -1;
  }

  /**
   * This makes a directory (and any necessary parent directories) (if it doesn't already exist).
   *
   * @param name
   * @throws RuntimeException if unable to comply.
   */
  public static void makeDirectory(String name) throws RuntimeException {
    File dir = new File(name);
    if (dir.isFile()) {
      throw new RuntimeException(
          "Unable to make directory=" + name + ". There is a file by that name!");
    } else if (!dir.isDirectory()) {
      if (!dir.mkdirs()) throw new RuntimeException("Unable to make directory=" + name + ".");
    }
  }

  /** A variant that copies the entire source. */
  public static boolean copy(String source, String destination) {
    return copy(source, destination, 0, -1);
  }

  /**
   * This makes a copy of a file. !!!If the source might be remote, use SSR.downloadFile(source,
   * dest, false) instead.
   *
   * @param source the full file name of the source file. If compressed, this doesn't decompress!
   * @param destination the full file name of the destination file. If the directory doesn't exist,
   *     it will be created. It is closed at the end.
   * @param first the first byte to be transferred (0..)
   * @param last the last byte to be transferred (inclusive), or -1 to transfer to the end.
   * @return true if successful. If not successful, the destination file won't exist.
   */
  public static boolean copy(String source, String destination, long first, long last) {

    if (source.equals(destination)) return false;

    // make dir
    try {
      File dir = new File(getDirectory(destination));
      if (!dir.isDirectory()) dir.mkdirs();
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      return false;
    }

    // regular file
    OutputStream out = null;
    boolean success = false;
    try {
      out = new BufferedOutputStream(new FileOutputStream(destination));
      success = copy(source, out, first, last);
    } catch (Exception e) {
      String2.log(String2.ERROR + " in File2.copy source=" + source + "\n" + e.toString());
    }
    try {
      if (out != null) out.close();
    } catch (Exception e) {
    }

    if (!success) delete(destination);

    return success;
  }

  /** A variant that copies the entire source. */
  public static boolean copy(String source, OutputStream out) {
    return copy(source, out, 0, -1);
  }

  /**
   * This is like copy(), but decompresses if the source is compressed
   *
   * @param source the full file name of the source file. If compressed, this does decompress!
   * @param destination the full file name of the destination file. If the directory doesn't exist,
   *     it will be created. It is closed at the end.
   * @return true if successful. If not successful, the destination file won't exist.
   */
  public static boolean decompress(String source, String destination) {

    if (source.equals(destination)) return false;
    InputStream in = null;
    OutputStream out = null;
    boolean success = false;
    try {
      File dir = new File(getDirectory(destination));
      if (!dir.isDirectory()) dir.mkdirs();
      in = getDecompressedBufferedInputStream(source);
      out = new BufferedOutputStream(new FileOutputStream(destination));
      success = copy(in, out, 0, -1);
    } catch (Exception e) {
      String2.log(String2.ERROR + " in File2.copy source=" + source + "\n" + e.toString());
    }
    try {
      if (in != null) in.close();
    } catch (Exception e) {
    }
    try {
      if (out != null) out.close();
    } catch (Exception e) {
    }

    if (!success) delete(destination);

    return success;
  }

  /**
   * This makes a copy of a file to an outputStream.
   *
   * @param source the full file name of the source. If compressed, this doesn't decompress!
   * @param out Best if buffered. It is flushed, but not closed, at the end
   * @param first the first byte to be transferred (0..)
   * @param last the last byte to be transferred (inclusive), or -1 to transfer to the end.
   * @return true if successful.
   */
  public static boolean copy(String source, OutputStream out, long first, long last) {

    InputStream in = null;
    try {
      in =
          getBufferedInputStream(
              source); // not getDecompressedBufferedInputStream(). Read file as is.
      return copy(in, out, first, last);
    } catch (Exception e) {
      String2.log(MustBe.throwable(String2.ERROR + " in File2.copy.", e));
      return false;
    } finally {
      try {
        if (in != null) in.close();
      } catch (Exception e2) {
      }
    }
  }

  /** A variant that copies the entire source. */
  public static boolean copy(InputStream in, OutputStream out) {
    return copy(in, out, 0, -1);
  }

  /**
   * This copies a range from an inputStream to an outputStream.
   *
   * @param in Best if buffered. At the end, this is NOT closed.
   * @param out Best if buffered. It is flushed, but not closed, at the end
   * @param first the first byte to be transferred (0..)
   * @param last the last byte to be transferred (inclusive), or -1 to transfer to the end.
   * @return true if successful.
   */
  public static boolean copy(InputStream in, OutputStream out, long first, long last) {

    int bufferSize = 32768;
    byte buffer[] = new byte[bufferSize];
    long remain = last < 0 ? Long.MAX_VALUE : 1 + last - first;
    try {
      skipFully(in, first);
      int nRead;
      while ((nRead = in.read(buffer, 0, (int) Math.min(remain, bufferSize)))
          >= 0) { // 0 shouldn't happen. -1=end of file
        out.write(buffer, 0, nRead);
        remain -= nRead;
        // String2.log(">> nRead=" + nRead + " remain=" + remain);
        if (remain == 0) break;
      }
      out.flush(); // always flush
      if (last >= 0 && nRead == -1) throw new IOException("Unexpected end-of-file.");
      // String2.log(">> found eof");
      return true;
    } catch (Exception e) {
      String2.log(
          MustBe.throwable(
              String2.ERROR
                  + " in File2.copy at first="
                  + first
                  + ", last="
                  + last
                  + ", remain="
                  + (last < 0 ? "?" : "" + remain)
                  + ".",
              e));
      return false;
    }
  }

  /**
   * This fully skips the specified number of bytes from the inputstream (unlike InputStream.skip,
   * which may not skip all of the bytes).
   *
   * @param inputStream Best if buffered.
   * @param nToSkip the number of bytes to be read
   * @throws IOException if trouble
   */
  public static void skipFully(InputStream inputStream, long nToSkip) throws IOException {

    long remain = nToSkip;
    int zeroCount = 0; // consecutive
    while (remain > 0) {
      long skipped = inputStream.skip(remain); // may not skip all requested
      if (skipped == 0) {
        if (++zeroCount == 3) throw new IOException("Unable to skip within the inputStream.");
      } else {
        zeroCount = 0;
        remain -= skipped;
      }
    }
  }

  /**
   * This reads the specified number of bytes from the inputstream (unlike InputStream.read, which
   * may not read all of the bytes).
   *
   * @param inputStream Best if buffered.
   * @param byteArray
   * @param offset the first position of byteArray to be written to
   * @param length the number of bytes to be read
   * @throws Exception if trouble
   */
  public static void readFully(InputStream inputStream, byte[] byteArray, int offset, int length)
      throws Exception {

    int po = offset;
    int remain = length;
    while (remain > 0) {
      int read = inputStream.read(byteArray, po, remain);
      po += read;
      remain -= read;
    }
  }

  /**
   * This creates, reads, and returns a byte array of the specified length.
   *
   * @param inputStream Best if buffered.
   * @param length the number of bytes to be read
   * @throws Exception if trouble
   */
  public static byte[] readFully(InputStream inputStream, int length) throws Exception {

    byte[] byteArray = new byte[length];
    readFully(inputStream, byteArray, 0, length);
    return byteArray;
  }

  /**
   * This returns a temporary directory (with forward slashes and a trailing slash, e.g.,
   * c:/Users/erd.data/AppData/Local/Temp/).
   *
   * @return the temporary directory
   * @throws Exception if trouble
   */
  public static String getSystemTempDirectory() throws Exception {
    if (tempDirectory == null) {
      File file = File.createTempFile("File2.getSystemTempDirectory", ".tmp");
      tempDirectory = file.getCanonicalPath();
      tempDirectory = String2.replaceAll(tempDirectory, "\\", "/");
      // String2.log("tempDir=" + tempDirectory);
      int po = tempDirectory.lastIndexOf('/');
      tempDirectory = tempDirectory.substring(0, po + 1);
      file.delete();
    }

    return tempDirectory;
  }

  /**
   * This converts (if not already done) tDir to use trailing / and with / separator.
   *
   * @param tDir The directory, with or without trailing / . With / or \\ .
   * @return The directory with trailing / and with / separator.
   */
  public static String forwardSlashDir(String tDir) {
    StringBuilder sb = new StringBuilder(tDir);
    String2.replaceAll(sb, '\\', '/');
    if (sb.length() == 0 || sb.charAt(tDir.length() - 1) != '/') sb.append('/');
    return sb.toString();
  }

  /**
   * This adds a slash (matching the other slashes in the dir) to the end of the dir (if one isn't
   * there already).
   *
   * @param dir with or without a slash at the end.
   * @return dir with a slash (matching the other slashes) at the end. If dir is null or "", this
   *     does nothing.
   */
  public static String addSlash(String dir) {
    if (dir == null || dir.length() == 0 || "\\/".indexOf(dir.charAt(dir.length() - 1)) >= 0)
      return dir;
    int po = dir.indexOf('\\');
    if (po < 0) po = dir.indexOf('/');
    char slash = po < 0 ? '/' : dir.charAt(po);
    return dir + slash;
  }

  /**
   * This removes the slash at the end of the dir (if there is one).
   *
   * @param dir with or without a slash at the end
   * @return dir with a slash (matching the other slashes) at the end
   */
  public static String removeSlash(String dir) {
    if (dir.length() == 0 || "\\/".indexOf(dir.charAt(dir.length() - 1)) < 0) return dir;
    return dir.substring(0, dir.length() - 1);
  }

  /**
   * This reads a file line by line (with any common end-of-line characters), does a simple (not
   * regex) search and replace on each line, and saves the lines in another file (with
   * String2.lineSeparator's).
   *
   * @param fullInFileName the full name of the input file (may be externally compressed)
   * @param fullOutFileName the full name of the output file (if same as fullInFileName,
   *     fullInFileName will be renamed +.original)
   * @param charset e.g., File2.UTF_8.
   * @param search a plain text string to search for
   * @param replace a plain text string to replace any instances of 'search'
   * @throws Exception if any trouble
   */
  public static void simpleSearchAndReplace(
      String fullInFileName, String fullOutFileName, String charset, String search, String replace)
      throws Exception {

    String2.log(
        "simpleSearchAndReplace in="
            + fullInFileName
            + " out="
            + fullOutFileName
            + " charset="
            + charset
            + " search="
            + search
            + " replace="
            + replace);
    String tOutFileName = fullOutFileName + Math2.random(Integer.MAX_VALUE);
    BufferedReader bufferedReader = getDecompressedBufferedFileReader(fullInFileName, charset);
    try {
      BufferedWriter bufferedWriter = getBufferedFileWriter(tOutFileName, charset);
      try {

        // convert the text, line by line
        // This uses bufferedReader.readLine() to repeatedly
        // read lines from the file and thus can handle various
        // end-of-line characters.
        String s = bufferedReader.readLine();
        while (s != null) { // null = end-of-file
          bufferedWriter.write(String2.replaceAll(s, search, replace));
          bufferedWriter.write(String2.lineSeparator);
          s = bufferedReader.readLine();
        }

        bufferedReader.close();
        bufferedReader = null;
        bufferedWriter.close();
        bufferedWriter = null;

        if (fullInFileName.equals(fullOutFileName))
          rename(fullInFileName, fullInFileName + ".original");
        rename(tOutFileName, fullOutFileName);
        if (fullInFileName.equals(fullOutFileName)) delete(fullInFileName + ".original");

      } catch (Exception e) {
        try {
          if (bufferedWriter != null) {
            bufferedWriter.close();
            bufferedWriter = null;
          }
        } catch (Exception e2) {
        }
        try {
          if (bufferedReader != null) {
            bufferedReader.close();
            bufferedReader = null;
          }
        } catch (Exception e2) {
        }
        delete(tOutFileName);
        throw e;
      }
    } catch (Exception e3) {
      try {
        if (bufferedReader != null) bufferedReader.close();
      } catch (Exception e4) {
      }
      delete(tOutFileName);
      throw e3;
    }
  }

  /**
   * This reads an ISO_8859_1 file line by line (with any common end-of-line characters), does a
   * regex search and replace on each line, and saves the lines in another file (with
   * String2.lineSeparator's).
   *
   * @param fullInFileName the full name of the input file (may be externally compressed)
   * @param fullOutFileName the full name of the output file
   * @param charset e.g., File2.UTF_8
   * @param search a regex to search for
   * @param replace a plain text string to replace any instances of 'search'
   * @throws Exception if any trouble
   */
  public static void regexSearchAndReplace(
      String fullInFileName, String fullOutFileName, String charset, String search, String replace)
      throws Exception {

    BufferedReader bufferedReader = getDecompressedBufferedFileReader(fullInFileName, charset);
    try {
      BufferedWriter bufferedWriter = getBufferedFileWriter(fullOutFileName, charset);
      try {
        // get the text from the file
        // This uses bufferedReader.readLine() to repeatedly
        // read lines from the file and thus can handle various
        // end-of-line characters.
        String s = bufferedReader.readLine();
        while (s != null) { // null = end-of-file
          bufferedWriter.write(s.replaceAll(search, replace));
          bufferedWriter.write(String2.lineSeparator);
          s = bufferedReader.readLine();
        }
      } finally {
        bufferedWriter.close();
      }
    } finally {
      bufferedReader.close();
    }
  }
}
