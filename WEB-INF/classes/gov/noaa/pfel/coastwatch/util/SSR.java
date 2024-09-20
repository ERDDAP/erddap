/*
 * SSR Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.ByteArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import com.sun.mail.smtp.SMTPTransport;
import gov.noaa.pfel.erddap.util.EDStatic;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;

/**
 * This Shell Script Replacement class has static methods to facilitate using Java programs in place
 * of Unix Shell Scripts. The parameters often follow the TCSH and Unix conventions, notably:
 * counting most things as 1,2,3... (not 0,1,2...).
 *
 * <p>In general, methods assume they will be successful and throw an Exception if not. So, if error
 * occurs, it will be obvious.
 *
 * <p>Goals for SSR and for converting CoastWatch shell scripts to Java:
 *
 * <ul>
 *   <li>Make the programs easier to work with from Java programs, notably web applications.
 *   <li>Make the Java programs readable to people familiar with the shell scripts.
 *   <li>To make it easy to port from shell scripts to Java programs.
 *   <li>Do more error checking. The scripts do minimal checking and sometimes may generate
 *       incorrect or blank graphs without notifying us. This lets us know when a problem occurs.
 *   <li>Generate descriptive error messages to help us find and fix the problems.
 *   <li>Make the Java programs plug-in replacements for the scripts. To this end, each script is
 *       replaced by one Java program. The Java programs can be called from the command line (via
 *       mini-script files) with the same parameters.
 *   <li>Add features to facilitate testing. Notably, the ability to specify different directories
 *       from the command line. Also, the creation of a test file for each script so that one or all
 *       of the scripts can be tested in an automated way.
 *   <li>Generation of statistics. E.g., how many graphs were created. Again, to facilitate
 *       monitoring the system to watch for trouble. (e.g., usually x files are generated, but x-320
 *       were generated this time)
 * </ul>
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 *     <p>Changes:
 *     <ul>
 *     </ul>
 */
public class SSR {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;
  public static boolean debugMode = false;

  public static String windows7Zip = "c:\\progra~1\\7-Zip\\7z"; // on Bob's computer
  public static String erddapVersion = "2"; // vague. will be updated by EDStatic

  private static String tempDirectory; // lazy creation by getTempDirectory
  public static ReentrantLock emailLock = new ReentrantLock();

  static {
    HttpURLConnection.setFollowRedirects(true); // it's a static method!
  }

  /**
   * Returns a String which is a substring of the current string. This checks for and deals with bad
   * first and last values.
   *
   * @param s the string
   * @param first the first character to be extracted (1..)
   * @param last the last character to be extracted (1..)
   * @return the extracted String (or "")
   */
  public static String cutChar(String s, int first, int last) {
    int size = s.length();

    if (first < 1) first = 1;
    if (last > size) last = size;
    return first > last ? "" : s.substring(first - 1, last); // last is exclusive
  }

  /**
   * Returns a String which is a substring at the end of the current string, starting at
   * <tt>first</tt>. This checks for and deals with a bad first values.
   *
   * @param s the string
   * @param first the first character to be extracted (1..)
   * @return the extracted String (or "")
   */
  public static String cutChar(String s, int first) {
    return cutChar(s, first, s.length());
  }

  /**
   * This finds the first line in the specified text file which startsWith 'start'.
   *
   * @param resourceFile the URL of the file
   * @param charset e.g., File2.ISO_8859_1.
   * @param start the text which must be at the beginning of the line
   * @return the first line in the specified text file which startsWith 'start' (or null if none
   *     found).
   * @throws Exception if error opening, reading, or closing the file
   */
  public static String getFirstLineStartsWith(URL resourceFile, String charset, String start)
      throws Exception {
    try (InputStream decompressedStream = File2.getDecompressedBufferedInputStream(resourceFile);
        InputStreamReader reader = new InputStreamReader(decompressedStream, charset);
        BufferedReader bufferedReader = new BufferedReader(reader)) {
      String s;
      while ((s = bufferedReader.readLine()) != null) { // null = end-of-file
        // String2.log(s);
        if (s.startsWith(start)) return s;
      }
    }
    return null;
  }

  /**
   * This finds the first line in the specified text file which matches 'regex'.
   *
   * @param resourceFile the name of the text file
   * @param charset e.g., File2.ISO_8859_1.
   * @param regex the regex text to be matched
   * @return the first line in the specified text file which matches regex (or null if none found).
   * @throws Exception if error opening, reading, or closing the file
   */
  public static String getFirstLineMatching(URL resourceFile, String charset, String regex)
      throws Exception {
    try (InputStream decompressedStream = File2.getDecompressedBufferedInputStream(resourceFile);
        InputStreamReader reader = new InputStreamReader(decompressedStream, charset);
        BufferedReader bufferedReader = new BufferedReader(reader)) {
      String s;
      Pattern pattern = Pattern.compile(regex);
      while ((s = bufferedReader.readLine()) != null) { // null = end-of-file
        // String2.log(s);
        if (pattern.matcher(s).matches()) return s;
      }
    }
    return null;
  }

  /**
   * This runs the specified command with dosShell (if String2.OSISWindows) or cShell (otherwise).
   *
   * @param commandLine with file names specified with forward slashes
   * @param timeOutSeconds (use 0 for no timeout)
   * @return an ArrayList of Strings with the output from the program (or null if there is a fatal
   *     error)
   * @throws Exception if exitStatus of cmd is not 0 (or other fatal error)
   */
  public static ArrayList dosOrCShell(String commandLine, int timeOutSeconds) throws Exception {
    if (String2.OSIsWindows) {
      // commandLine = String2.replaceAll(commandLine, "/", "\\");
      return dosShell(commandLine, timeOutSeconds);
    } else {
      return cShell(commandLine, timeOutSeconds);
    }
  }

  /**
   * This is a variant of shell() for DOS command lines.
   *
   * @param commandLine the command line to be executed (for example, "myprogram <filename>") with
   *     backslashes
   * @param timeOutSeconds (use 0 for no timeout)
   * @return an ArrayList of Strings with the output from the program (or null if there is a fatal
   *     error)
   * @throws Exception if exitStatus of cmd is not 0 (or other fatal error)
   * @see #shell
   */
  public static ArrayList dosShell(String commandLine, int timeOutSeconds) throws Exception {
    if (verbose) String2.log("dosShell        in: " + commandLine);
    PipeToStringArray outCatcher = new PipeToStringArray();
    PipeToStringArray errCatcher = new PipeToStringArray();

    //        int exitValue = shell(String2.tokenize("cmd.exe /C " + commandLine),
    int exitValue =
        shell(new String[] {"cmd.exe", "/C", commandLine}, outCatcher, errCatcher, timeOutSeconds);

    // collect and print results (or throw exception)
    String err = errCatcher.getString();
    if (verbose || err.length() > 0 || exitValue != 0) {
      String s =
          "dosShell       cmd: "
              + commandLine
              + "\n"
              + "dosShell exitValue: "
              + exitValue
              + "\n"
              + "dosShell       err: "
              + err
              + (err.length() > 0 ? "" : "\n");
      // "dosShell       out: " + outCatcher.getString();
      if (exitValue == 0) String2.log(s);
      else throw new Exception(String2.ERROR + " in SSR.dosShell:\n" + s);
    }
    return outCatcher.getArrayList();
  }

  /**
   * This is a variant of shell() for cShell command lines.
   *
   * @param commandLine the command line to be executed (for example, "myprogram <filename>")
   * @param timeOutSeconds (use 0 for no timeout)
   * @return an ArrayList of Strings with the output from the program (or null if there is a fatal
   *     error)
   * @throws Exception if exitStatus of cmd is not 0 (or other fatal error)
   * @see #shell
   */
  public static ArrayList cShell(String commandLine, int timeOutSeconds) throws Exception {
    if (verbose) String2.log("cShell        in: " + commandLine);
    PipeToStringArray outCatcher;
    PipeToStringArray errCatcher;
    int exitValue = 0;
    try {
      outCatcher = new PipeToStringArray();
      errCatcher = new PipeToStringArray();
      exitValue =
          shell(
              new String[] {"/bin/csh", "-c", commandLine}, outCatcher, errCatcher, timeOutSeconds);
    } catch (IOException e) {
      // Try to fallback to bash.
      outCatcher = new PipeToStringArray();
      errCatcher = new PipeToStringArray();
      exitValue =
          shell(
              new String[] {"/bin/bash", "-c", commandLine},
              outCatcher,
              errCatcher,
              timeOutSeconds);
    }

    // collect and print results (or throw exception)
    String err = errCatcher.getString();
    if (verbose || err.length() > 0 || exitValue != 0) {
      String s =
          "cShell       cmd: "
              + commandLine
              + "\n"
              + "cShell exitValue: "
              + exitValue
              + "\n"
              + "cShell       err: "
              + err
              + (err.length() > 0 ? "" : "\n"); // +
      //                "cShell       out: " + outCatcher.getString();
      if (exitValue == 0) String2.log(s);
      else throw new Exception(String2.ERROR + " in SSR.cShell:\n" + s);
    }
    return outCatcher.getArrayList();
  }

  /**
   * This is a variant of shell() for cShell command lines.
   *
   * @param commandLine the command line to be executed (for example, "myprogram <filename>")
   * @param outStream an outputStream to capture the results. This does not close the outStream
   *     afterwards. (Use "null" if you don't want to capture out.)
   * @param errStream an outputStream to capture the error. This does not close the errStream
   *     afterwards. (Use "null" if you don't want to capture err.)
   * @param timeOutSeconds (use 0 for no timeout)
   * @return the exitValue
   * @throws Exception but unlike the other shell commands, this doesn't throw an exception just
   *     because exitValue != 0.
   * @see #shell
   */
  public static int cShell(
      String commandLine, OutputStream outStream, OutputStream errStream, int timeOutSeconds)
      throws Exception {
    if (verbose) String2.log("cShell        in: " + commandLine);
    ByteArrayOutputStream outBAOS = null;
    ByteArrayOutputStream errBAOS = null;
    if (outStream == null) {
      outBAOS = new ByteArrayOutputStream();
      outStream = outBAOS;
    }
    if (errStream == null) {
      errBAOS = new ByteArrayOutputStream();
      errStream = errBAOS;
    }
    PipeToOutputStream outCatcher = new PipeToOutputStream(outStream);
    PipeToOutputStream errCatcher = new PipeToOutputStream(errStream);

    // call shell()
    int exitValue =
        shell(new String[] {"/bin/csh", "-c", commandLine}, outCatcher, errCatcher, timeOutSeconds);

    // if I created the streams, close them
    if (outBAOS != null)
      try {
        outBAOS.close();
      } catch (Exception e) {
      }
    if (errBAOS != null)
      try {
        errBAOS.close();
      } catch (Exception e) {
      }

    // collect and print results (or throw exception)
    String err = errBAOS == null ? "" : errBAOS.toString();
    if (verbose || err.length() > 0 || exitValue != 0) {
      String2.log(
          "cShell       cmd: "
              + commandLine
              + "\n"
              + "cShell exitValue: "
              + exitValue
              + "\n"
              + "cShell       err: "
              + (errBAOS == null ? "[unknown]" : err));
    }

    return exitValue;
  }

  /**
   * This runs and waits for the specified cShell command line (including parameters, pipes, and
   * other redirection (e.g., >>). This is highly OS dependent -- so it is better to not call this.
   *
   * <p>If you want to ignore exitValue!=0, try/catch the call to cShell.
   *
   * @param cmd the command to be executed (for example, new String[]{"rm", "-F", "<filename>"}). If
   *     this is a system command, the first two elements should be "/bin/csh", "-c" (for cShell),
   *     "/bin/sh", "-c" ((?)for bash), "cmd.exe", "/C" (for recent Windows), "command.com", "/C"
   *     (if osName.equals("Windows 95")).
   * @param outPipe a PipeTo to catch stdOut from the process
   * @param errPipe a PipeTo to catch stdErr from the process
   * @param timeOutSeconds (use -1 for no time out)
   * @return the exitValue (Integer.MAX_VALUE indicates time out)
   * @throws Exception
   */
  public static int shell(String cmd[], PipeTo outPipe, PipeTo errPipe, int timeOutSeconds)
      throws Exception {

    // execute the command
    // why need separate /bin/sh and -c?
    //  so that params are passed to the cmd program and not to /bin/csh
    //  see http://www.mountainstorm.com/publications/javazine.html
    // what does -c do?  see Linux In a Nutshell pg 623 for a weak explanation
    if (verbose) String2.log("SSR.shell cmd[]=\n" + String2.toNewlineString(cmd));
    Process process = Runtime.getRuntime().exec(cmd);

    // capture the output
    outPipe.setInputStream(process.getInputStream());
    errPipe.setInputStream(process.getErrorStream());
    outPipe.start(); // start the threads
    errPipe.start();

    // wait till done
    int exitValue = Integer.MAX_VALUE; // = process.waitFor();
    boolean done = false;
    long time = 0;
    long timeOutMillis = timeOutSeconds * 1000L;
    while (!done) {
      try {
        exitValue = process.exitValue(); // throws exception if process not done
        done = true;
      } catch (Exception e) {
        if (timeOutSeconds > 0 && time >= timeOutMillis) {
          done = true;
          process.destroy();
        }
        Math2.sleep(60); // give the process some time to work before trying again
        time += 60;
      }
    }

    // force stream closure (needed to generate bufferedReader read null)
    process.getInputStream().close();
    if (timeOutSeconds > 0 && time >= timeOutMillis)
      errPipe.print(
          String2.ERROR
              + ": shell command ("
              + String2.toCSSVString(cmd)
              + ") timed out ("
              + timeOutSeconds
              + " s).\n");
    process.getErrorStream().close();

    return exitValue;
  }

  /**
   * This zips the contents of a directory (recursively) and puts the results in a zip file of the
   * same name.
   *
   * @param dir with or without trailing slash. Forward or backslashes are okay.
   */
  public static void zipADirectory(String dir, int timeOutSeconds) throws Exception {
    // remove trailing slash
    char slash = dir.indexOf('/') >= 0 ? '/' : '\\';
    if (dir.endsWith("/") || dir.endsWith("\\")) dir = dir.substring(0, dir.length() - 1);

    SSR.zip(dir + ".zip", new String[] {dir}, timeOutSeconds, true, File2.getDirectory(dir));
  }

  /**
   * Put the specified files in a zip file (without directory info). See
   * http://javaalmanac.com/egs/java.util.zip/CreateZip.html . If a file named zipDirName already
   * exists, it is overwritten.
   *
   * @param zipDirName the full name for the .zip file (path + name + ".zip")
   * @param dirNames the full names of the files to be put in the zip file. These can use forward or
   *     backslashes as directory separators. If a dirName is a directory, all the files in the
   *     directory (recursively) will be included.
   * @param timeOutSeconds (use -1 for no time out)
   * @throws Exception if trouble
   */
  public static void zip(String zipDirName, String dirNames[], int timeOutSeconds)
      throws Exception {

    zip(zipDirName, dirNames, timeOutSeconds, false, "");
  }

  /**
   * Put the specified files in a zip file (with some directory info). See
   * http://javaalmanac.com/egs/java.util.zip/CreateZip.html . If a file named zipDirName already
   * exists, it is overwritten.
   *
   * @param zipDirName the full name for the .zip file (path + name + ".zip")
   * @param dirNames the full names of the files to be put in the zip file. These can use forward or
   *     backslashes as directory separators. If a dirName is a directory, all the files in the
   *     directory (recursively) will be included.
   * @param timeOutSeconds (use -1 for no time out)
   * @param removeDirPrefix the prefix to be removed from the start of each dir name (ending with a
   *     slash)
   * @throws Exception if trouble
   */
  public static void zip(
      String zipDirName, String dirNames[], int timeOutSeconds, String removeDirPrefix)
      throws Exception {

    zip(zipDirName, dirNames, timeOutSeconds, true, removeDirPrefix);
  }

  /**
   * Put the specified files in a zip file. See
   * http://javaalmanac.com/egs/java.util.zip/CreateZip.html . If a file named zipDirName already
   * exists, it is overwritten.
   *
   * @param zipDirName the full name for the .zip file (path + name + ".zip") Don't include c:.
   * @param dirNames the full names of the files to be put in the zip file. Don't include c:. These
   *     can use forward or backslashes as directory separators. If a dirName is a directory, all
   *     the files in the directory (recursively) will be included.
   * @param timeOutSeconds (use -1 for no time out)
   * @param includeDirectoryInfo set this to false if you don't want any dir info stored with the
   *     files
   * @param removeDirPrefix if includeDirectoryInfo is true, this is the prefix to be removed from
   *     the start of each dir name (ending with a slash). If includeDirectoryInfo is false, this is
   *     removed.
   * @throws Exception if trouble
   */
  private static void zip(
      String zipDirName,
      String dirNames[],
      int timeOutSeconds,
      boolean includeDirectoryInfo,
      String removeDirPrefix)
      throws Exception {

    // validate
    long tTime = System.currentTimeMillis();
    if (includeDirectoryInfo) {
      // ensure slash at end of removeDirPrefix
      if ("\\/".indexOf(removeDirPrefix.charAt(removeDirPrefix.length() - 1)) < 0)
        throw new IllegalArgumentException(
            String2.ERROR + " in SSR.zip: removeDirPrefix must end with a slash.");

      // ensure dirNames start with removeDirPrefix
      for (int i = 0; i < dirNames.length; i++)
        if (!dirNames[i].startsWith(removeDirPrefix))
          throw new IllegalArgumentException(
              String2.ERROR
                  + " in SSR.zip: dirName["
                  + i
                  + "] doesn't start with "
                  + removeDirPrefix
                  + ".");
    }

    // if Linux, it is faster to use the zip utility
    // I don't know how to include just partial dir info with Linux,
    //  since I can't cd to that directory.
    if (String2.OSIsLinux && !includeDirectoryInfo) {
      // -j: don't include dir info
      if (verbose) String2.log("Using Linux's zip to make " + zipDirName);
      File2.delete(zipDirName); // delete any exiting .zip file of that name
      cShell("zip -j " + zipDirName + " " + String2.toSSVString(dirNames), timeOutSeconds);
      if (verbose) String2.log("  zip done. TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
      return;
    }

    // for all other operating systems...
    if (verbose) String2.log("Using Java's zip to make " + zipDirName);
    // create the ZIP file
    ZipOutputStream out =
        new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipDirName)));
    try {

      // create a buffer for reading the files
      byte[] buf = new byte[4096];

      // compress the files
      for (int i = 0; i < dirNames.length; i++) {
        // if directory, get all file names
        ArrayList<String> al = new ArrayList();
        if (File2.isDirectory(dirNames[i])) {
          RegexFilenameFilter.recursiveFullNameList(al, dirNames[i], ".*", false); // directoriesToo
        } else {
          al.add(dirNames[i]);
        }

        for (int i2 = 0; i2 < al.size(); i2++) {
          InputStream in =
              File2.getBufferedInputStream(
                  al.get(i2)); // not File2.getDecompressedBufferedInputStream(). Read files as is.
          try {

            // add ZIP entry to output stream
            String tName =
                includeDirectoryInfo
                    ? al.get(i2).substring(removeDirPrefix.length())
                    : // already validated above
                    File2.getNameAndExtension(al.get(i2));
            out.putNextEntry(new ZipEntry(tName));

            // transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
              out.write(buf, 0, len);
            }

            // complete the entry
            out.closeEntry();
          } finally {
            in.close();
          }
        }
      }
    } finally {
      // close the ZIP file
      out.close();
    }
    if (verbose) String2.log("  zip done. TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
  }

  /**
   * Put the specified files in a gz file (without directory info). If a file named gzipDirName
   * already exists, it is overwritten.
   *
   * @param gzipDirName the full name for the .gz file (path + name + ".gz")
   * @param dirNames the full names of the files to be put in the gz file. These can use forward or
   *     backslashes as directory separators. CURRENTLY LIMITED TO 1 FILE.
   * @param timeOutSeconds (use -1 for no time out)
   * @throws Exception if trouble
   */
  public static void gzip(String gzipDirName, String dirNames[], int timeOutSeconds)
      throws Exception {

    gzip(gzipDirName, dirNames, timeOutSeconds, false, "");
  }

  /**
   * Put the specified files in a gzip file (with some directory info). If a file named gzipDirName
   * already exists, it is overwritten.
   *
   * @param gzipDirName the full name for the .gz file (path + name + ".gz")
   * @param dirNames the full names of the files to be put in the gzip file. These can use forward
   *     or backslashes as directory separators. CURRENTLY LIMITED TO 1 FILE.
   * @param timeOutSeconds (use -1 for no time out)
   * @param removeDirPrefix the prefix to be removed from the start of each dir name (ending with a
   *     slash)
   * @throws Exception if trouble
   */
  public static void gzip(
      String gzipDirName, String dirNames[], int timeOutSeconds, String removeDirPrefix)
      throws Exception {

    gzip(gzipDirName, dirNames, timeOutSeconds, true, removeDirPrefix);
  }

  /**
   * Put the specified files in a gzip file (without directory info). If a file named gzipDirName
   * already exists, it is overwritten.
   *
   * @param gzipDirName the full name for the .zip file (path + name + ".gz")
   * @param dirNames the full names of the files to be put in the gzip file. These can use forward
   *     or backslashes as directory separators. CURRENTLY LIMITED TO 1 FILE.
   * @param timeOutSeconds (use -1 for no time out)
   * @param includeDirectoryInfo set this to false if you don't want any dir invo stored with the
   *     files
   * @param removeDirPrefix if includeDirectoryInfo is true, this is the prefix to be removed from
   *     the start of each dir name (ending with a slash). If includeDirectoryInfo is false, this is
   *     removed.
   * @throws Exception if trouble
   */
  private static void gzip(
      String gzipDirName,
      String dirNames[],
      int timeOutSeconds,
      boolean includeDirectoryInfo,
      String removeDirPrefix)
      throws Exception {

    // validate
    if (includeDirectoryInfo) {
      // ensure slash at end of removeDirPrefix
      if ("\\/".indexOf(removeDirPrefix.charAt(removeDirPrefix.length() - 1)) < 0)
        throw new IllegalArgumentException(
            String2.ERROR + " in SSR.gzip: removeDirPrefix must end with a slash.");

      // ensure dirNames start with removeDirPrefix
      for (int i = 0; i < dirNames.length; i++)
        if (!dirNames[i].startsWith(removeDirPrefix))
          throw new IllegalArgumentException(
              String2.ERROR
                  + " in SSR.zip: dirName["
                  + i
                  + "] doesn't start with "
                  + removeDirPrefix
                  + ".");
    }

    // if Linux, it is faster to use the zip utility
    // I don't know how to include just partial dir info with Linux,
    //  since I can't cd to that directory.
    /*if (String2.OSIsLinux && !includeDirectoryInfo) {
        //-j: don't include dir info
        if (verbose) String2.log("Using Linux's zip");
        File2.delete(zipDirName); //delete any exiting .zip file of that name
        cShell("zip -j " + zipDirName + " " + String2.toSSVString(dirNames),
            timeOutSeconds);
        return;
    }*/

    // for all other operating systems...
    // create the ZIP file
    long tTime = System.currentTimeMillis();
    GZIPOutputStream out =
        new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(gzipDirName)));
    try {
      // create a buffer for reading the files
      byte[] buf = new byte[4096];

      // compress the files
      for (int i = 0; i < 1; i++) { // i < dirNames.length; i++) {
        InputStream in =
            File2.getBufferedInputStream(
                dirNames[i]); // not File2.getDecompressedBufferedInputStream() Read files as is.
        try {
          // add ZIP entry to output stream
          String tName =
              includeDirectoryInfo
                  ? dirNames[i].substring(removeDirPrefix.length())
                  : // already validated above
                  File2.getNameAndExtension(dirNames[i]);
          // out.putNextEntry(new ZipEntry(tName));

          // transfer bytes from the file to the ZIP file
          int len;
          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }
          // complete the entry
          // out.closeEntry();
        } finally {
          in.close();
        }
      }
    } finally {
      // close the GZIP file
      out.close();
    }
    if (verbose) String2.log("  gzip done. TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
  }

  /**
   * This handles the common case of unzipping a zip file (in place) that contains a directory with
   * subdirectories and files.
   */
  public static void unzipADirectory(
      String fullZipName, int timeOutSeconds, StringArray resultingFullFileNames) throws Exception {

    unzip(
        fullZipName,
        File2.getDirectory(fullZipName),
        false,
        timeOutSeconds,
        resultingFullFileNames);
  }

  /**
   * Extract all of the files from a zip file to the base directory. Any existing files of the same
   * name are overwritten.
   *
   * @param fullZipName (with .zip at end)
   * @param baseDir The destination base directory (with slash at end).
   * @param ignoreZipDirectories if true, the directories (if any) of the files in the .zip file are
   *     ignored, and all files are stored in baseDir itself. If false, new directories will be
   *     created as needed.
   * @param timeOutSeconds (use -1 for no time out)
   * @param resultingFullFileNames If this isn't null, the full names of unzipped files are added to
   *     this. This method doesn't initially cleared this StringArray!
   * @throws Exception
   */
  public static void unzip(
      String fullZipName,
      String baseDir,
      boolean ignoreZipDirectories,
      int timeOutSeconds,
      StringArray resultingFullFileNames)
      throws Exception {

    // if Linux, it is faster to use the zip utility
    long tTime = System.currentTimeMillis();
    if (verbose) String2.log("Using Java's unzip on " + fullZipName);
    ZipInputStream in =
        new ZipInputStream(
            File2.getBufferedInputStream(
                fullZipName)); // not File2.getDecompressedBufferedInputStream(). Read file as is.
    try {
      // create a buffer for reading the files
      byte[] buf = new byte[4096];

      // unzip the files
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {

        // isDirectory?
        String name = entry.getName();
        if (verbose) String2.log("  unzipping " + name);
        if (entry.isDirectory()) {
          if (ignoreZipDirectories) {
          } else {
            File tDir = new File(baseDir + name);
            if (!tDir.exists()) tDir.mkdirs();
          }
        } else {
          // open an output file
          if (ignoreZipDirectories) name = File2.getNameAndExtension(name); // remove dir info
          File2.makeDirectory(File2.getDirectory(baseDir + name)); // name may incude subdir names
          OutputStream out = new BufferedOutputStream(new FileOutputStream(baseDir + name));
          try {

            // transfer bytes from the .zip file to the output file
            // in.read reads from current zipEntry
            byte[] buffer = new byte[8192]; // best if smaller than java buffered...stream size
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, buf.length)) > 0) {
              out.write(buffer, 0, bytesRead);
            }
          } finally {
            // close the output file
            out.close();
          }
          if (resultingFullFileNames != null) resultingFullFileNames.add(baseDir + name);
        }

        // close this entry
        in.closeEntry();

        // get the next entry
        entry = in.getNextEntry();
      }
    } finally {
      // close the input file
      in.close();
    }

    if (verbose) String2.log("  unzip done. TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
  }

  /**
   * Extract the ONE file from a .gz file to the base directory. An existing file of the same name
   * will be overwritten.
   *
   * @param fullGzName (with .gz at end)
   * @param baseDir (with slash at end)
   * @param ignoreGzDirectories if true, the directories (if any) of the files in the .gz file are
   *     ignored, and all files are stored in baseDir itself. If false, new directories will be
   *     created as needed. CURRENTLY, ONLY 'TRUE' IS SUPPORTED. THE FILE IS ALWAYS GIVEN THE NAME
   *     fullGzName.substring(0, fullGzName.length() - 3).
   * @param timeOutSeconds (use -1 for no time out)
   * @throws Exception
   */
  public static void unGzip(
      String fullGzName, String baseDir, boolean ignoreGzDirectories, int timeOutSeconds)
      throws Exception {

    // if Linux, it is faster to use the zip utility
    long tTime = System.currentTimeMillis();
    if (!ignoreGzDirectories)
      throw new RuntimeException("Currently, SSR.unGzip only supports ignoreGzDirectories=true!");
    /*Do this in the future?
     if (String2.OSIsLinux) {
        //-d: the directory to put the files in
        if (verbose) String2.log("Using Linux's ungz");
        cShell("ungz -o " + //-o overwrites existing files without asking
            (ignoreGzDirectories? "-j " : "") +
            fullGzName + " " +
            "-d " + baseDir.substring(0, baseDir.length() - 1),  //remove trailing slash   necessary?
            timeOutSeconds);
    } else */ {
      // use Java's gzip procedures for all other operating systems
      if (verbose) String2.log("Java's ungzip " + fullGzName);
      GZIPInputStream in =
          new GZIPInputStream(
              File2.getBufferedInputStream(
                  fullGzName)); // not File2.getDecompressedBufferedInputStream(). Read file as is.
      try {
        // create a buffer for reading the files
        byte[] buf = new byte[4096];

        //// unzip the files
        // ZipEntry entry = in.getNextEntry();
        // while (entry != null) {
        String ext = File2.getExtension(fullGzName); // should be .gz
        String name = fullGzName.substring(0, fullGzName.length() - ext.length());
        /*
        //isDirectory?
        if (entry.isDirectory()) {
            if (ignoreZipDirectories) {
            } else {
                File tDir = new File(baseDir + name);
                if (!tDir.exists())
                    tDir.mkdirs();
            }
        } else */ {
          // open an output file
          // ???do I need to make the directory???
          if (ignoreGzDirectories) name = File2.getNameAndExtension(name); // remove dir info
          OutputStream out = new BufferedOutputStream(new FileOutputStream(baseDir + name));
          try {
            // transfer bytes from the .zip file to the output file
            // in.read reads from current zipEntry
            byte[] buffer = new byte[8192]; // best if smaller than java buffered...Stream size
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, buf.length)) > 0) {
              out.write(buffer, 0, bytesRead);
            }
          } finally {
            // close the output file
            out.close();
          }
        }

        //// close this entry
        // in.closeEntry();

        //// get the next entry
        // entry = in.getNextEntry();
        // }
      } finally {
        // close the input file
        in.close();
      }
    }
    if (verbose)
      String2.log("  ungzip done. TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
  }

  /**
   * This decompresses a .tar.gz file on Bob's Windows computer, in a directory with the name from
   * the .tar.gz file.
   *
   * @throws Exception if trouble
   */
  public static void windowsDecompressTargz(
      String sourceFullName, boolean makeBaseDir, int timeOutMinutes) throws Exception {

    String sourceDir = File2.getDirectory(sourceFullName);
    String sourceTarName = File2.getNameNoExtension(sourceFullName);
    String sourceJustName = File2.getNameNoExtension(sourceTarName);

    // extract tar from .gzip
    String cmd = windows7Zip + " -y e " + sourceFullName + " -o" + sourceDir + " -r";
    long cmdTime = System.currentTimeMillis();
    dosShell(cmd, timeOutMinutes * 60);
    String2.log("  cmd time=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));

    // extract from the .tar file   //gtspp4_at199001.tar
    if (makeBaseDir) File2.makeDirectory(sourceDir + sourceJustName);
    File2.makeDirectory(sourceDir + sourceJustName + "/");
    cmd =
        windows7Zip
            + " -y x "
            + sourceDir
            + sourceTarName
            + // xtract with full dir names
            " -o"
            + sourceDir
            + (makeBaseDir ? sourceJustName + "/" : "")
            + " -r";
    cmdTime = System.currentTimeMillis();
    dosShell(cmd, timeOutMinutes * 60);
    String2.log("  cmd time=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));

    // delete the .tar file
    File2.delete(sourceDir + sourceTarName);
  }

  /**
   * Unzip oldDir + oldName.zip (a zip containing one file: oldName) and rename it to newDir +
   * newName. If newDir + newName already exists, it is File2.'touch'ed.
   *
   * @param oldDir (with slash at end)
   * @param oldName (with .zip at end)
   * @param newDir (with slash at end)
   * @param newName
   * @param timeOutSeconds (use 0 for no timeout)
   * @throws Exception
   */
  public static void unzipRename(
      String oldDir, String oldName, String newDir, String newName, int timeOutSeconds)
      throws Exception {

    // already exists?
    if (File2.touch(newDir + newName)) {
      String2.log("SSR.unzipRename is reusing " + newName);
      return;
    }

    // unzip the file
    unzip(oldDir + oldName, newDir, true, timeOutSeconds, null);

    // rename the file
    String oldNameNoZip = oldName.substring(0, oldName.length() - 4);
    if (!oldNameNoZip.equals(newName)) File2.rename(newDir, oldNameNoZip, newName);
  }

  /**
   * This opens a ZipOutputStream with one entry (with the fileName, but no data). This is not
   * wrapped in a BufferedOutputStream, since it often doesn't need to be.
   *
   * @param zipDirName the full name for the .zip file (path + name + ".zip")
   * @param fileName the file name to be put in the zip file. Your choice: with directory info or
   *     not. Use forward directory separators [I'm pretty sure].
   * @return the ZipOutputStream (or null if trouble)
   */
  public static ZipOutputStream startZipOutputStream(String zipDirName, String fileName) {

    try {
      // Create the ZIP file
      ZipOutputStream out =
          new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipDirName)));

      // Add ZIP entry to output stream.
      out.putNextEntry(new ZipEntry(fileName));

      return out;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * THIS DOES NOT YET WORK PERFECTLY (BUT CLOSE). I DIDN'T SPEND TIME TRYING. This returns the
   * PostScript code to embed an eps file in another PostScript file.
   *
   * @param left
   * @param bottom
   * @param angle the angle to rotate (degrees)
   * @param xScale
   * @param yScale
   * @param bBoxLLX the incoming file's bounding box lower left X
   * @param bBoxLLY
   * @param epsContents
   * @return the PostScript code to embed an eps file in another PostScript file.
   */
  public static String embedEPSF(
      double left,
      double bottom,
      double angle,
      double xScale,
      double yScale,
      double bBoxLLX,
      double bBoxLLY,
      String epsContents) {
    return
    // This is from PostScript Language Reference Manual 2nd ed, pg 726
    // (in "Appendix H: Encapsulated PostScript File Format - Version 3.0"
    // BeginEPSF and EndEPSF were definitions (to be put in the prolog),
    // but the def stuff is commented out here so this can be used inline
    // in case you don't have access to the prolog of the ps file you are creating.
    // "/BeginEPSF { %def\n" +                         //prepare for EPS file
    "  /b4_inc_state save def\n"
        + // save state for cleanup
        "  /dict_count countdictstack def\n"
        + "  /op_count count 1 sub def\n"
        + // count objects on op stack
        "  userdict begin\n"
        + // make userdict current dict
        "  /showpage {} def\n"
        + // redifine showpage to be null
        "  0 setgray 0 setlinecap\n"
        + "  1 setlinewidth 0 setlinejoin\n"
        + "  10 setmiterlimit [] 0 setdash newpath\n"
        + "  /languagelevel where\n"
        + // if not equal to 1 then
        "  {pop languagelevel\n"
        + // set strokeadjust and
        "  1 ne\n"
        + // overprint to their defaults
        "    {false setstrokeadjust false setoverprint\n"
        + "    } if\n"
        + "  } if\n"
        +
        // "  } bind def\n" +

        // the code that actually embeds the content
        left
        + " "
        + bottom
        + " translate\n"
        + angle
        + " rotate\n"
        + xScale
        + " "
        + yScale
        + " scale\n"
        + bBoxLLX
        + " "
        + bBoxLLY
        + " translate\n"
        + epsContents
        + "\n"
        +

        // "/EndEPSF { %def \n" +
        "  count op_count sub {pop} repeat\n"
        + "  countdictstack dict_count sub {end} repeat\n"
        + // clean up dict stack
        "  b4_inc_state restore\n"
        +
        // "} bind def\n" +
        "";
  }

  /**
   * This converts an email address with dots and 'at' signs into a safer form (to hide from spam
   * spiders) for display on the web.
   *
   * @param emailAddress (eg. bob DOT simons AT noaa DOT gov)
   * @return the safer form (eg. bob DOT simons AT noaa DOT gov)
   */
  public static String getSafeEmailAddress(String emailAddress) {
    emailAddress = String2.replaceAll(emailAddress, ".", " dot ");
    emailAddress = String2.replaceAll(emailAddress, "@", " at ");
    return emailAddress;
  }

  /**
   * This returns a string with the topNMostRequested items.
   *
   * @param printTopN
   * @param header (e.g., printTopN + " Most Requested .grd Files")
   * @param requestedFilesMap (map with key=String (e.g., fileName), value=IntObject with frequency
   *     info). If it needs to be thread-safe, use ConcurrentHashMap.
   * @return a string with the topN items in a table
   */
  public static String getTopN(int printTopN, String header, Map requestedFilesMap) {
    // printTopNMostRequested
    // many of these will be artifacts: e.g., initial default file
    StringBuilder sb = new StringBuilder();
    if (printTopN > 0 && !requestedFilesMap.isEmpty()) {
      // topN will be kept as sorted ascending, so best will be at end
      String topN[] = new String[printTopN];
      Arrays.fill(topN, "\t");
      int worst = 0;
      int nActive = 0;
      Iterator it = requestedFilesMap.keySet().iterator();
      while (it.hasNext()) {
        String key = (String) it.next();
        int value = ((IntObject) requestedFilesMap.get(key)).i;
        if (value <= 0) continue;
        if (nActive < printTopN || value > worst) {
          if (nActive < printTopN) nActive++;
          if (value < worst) worst = value;
          String ts = String2.right("" + value, 9) + "  " + key;
          int where = Arrays.binarySearch(topN, ts);
          if (where >= 0)
            // it is already in the array -- shouldn't ever happen
            sb.append(
                String2.ERROR
                    + ": SSR.getTopN wants to insert \""
                    + ts
                    + "\"\n"
                    + "at "
                    + where
                    + ", where values are\n"
                    + String2.toNewlineString(topN)
                    + "\n");
          else {
            // make 'where' positively stated
            where = -where - 2; // would be -1, but worst is always thrown out

            // 'where' may be -1 if tie for worst and file name sorts it at beginning
            if (where >= 0) {
              // open up a space (worst is always thrown out)
              System.arraycopy(topN, 1, topN, 0, where);

              // insert it
              topN[where] = ts;
            }
          }
        }
      }

      // print the most requested .grd files
      sb.append(printTopN + header + "\n");
      for (int i = printTopN - 1; i >= 0; i--) {
        if (!topN[i].equals("\t"))
          sb.append(String2.right("#" + (printTopN - i), 7) + topN[i] + "\n");
      }
    }
    return sb.toString();
  }

  /**
   * This runs a series matlab commands, ending with 'exit' or 'quit'. This is set up for Linux
   * computers and uses cShell commands.
   *
   * @param fullControlName the name of the file with the commands for Matlabs command line (one per
   *     line).
   * @param fullOutputName the name for the file that will be created to hold the Matlab output.
   * @param timeOutSeconds (use -1 for no time out)
   * @throws Exception if trouble
   */
  public static void runMatlab(String fullControlName, String fullOutputName, int timeOutSeconds)
      throws Exception {
    // this is Dave Foley's trick in his /u00/chump/matcom script
    Exception e = null;
    try {
      SSR.cShell("set OLDDISPLAY = $DISPLAY", 1);
      SSR.cShell("unsetenv DISPLAY", 1);
      SSR.cShell(
          "nohup matlab < "
              + fullControlName
              + " >! "
              + fullOutputName, // ">!" writes to a new file
          timeOutSeconds);
    } catch (Exception e2) {
      e = e2;
    }

    // The purpose of try/catch above is to ensure this gets done.
    // Problem was: DISPLAY was unset, then error occurred and DISPLAY was never reset.
    SSR.cShell("setenv DISPLAY $OLDDISPLAY", 1);

    if (e != null) throw e;
  }

  /**
   * This gets an email session and smtpTransport for sending emails with authentication. This
   * should be called after emailLock.lock().
   *
   * @param smtpHost The domain of the email server, e.g., smtp.gmail.com .
   * @param smtpPort The port number, e.g., 587.
   * @param userName For authentication
   * @param password For authentication
   * @param properties alternating list of name|property|name|property ...
   * @return Object[]{session, smtpTransport};
   * @throws Exception if trouble
   */
  public static Object[] openEmailSession(
      String smtpHost, int smtpPort, String userName, String password, String properties)
      throws Exception {

    // String2.log("SSR.sendEmail host=" + smtpHost + " port=" + smtpPort + "\n" +
    //    "  properties=" + properties + "\n" +
    //    "  userName=" + userName + " password=" + (password.length() > 0? "[present]" :
    // "[absent]"));

    String notSendingMsg = String2.ERROR + " in SSR.openEmailSession: not sending email because ";
    if (!String2.isSomething(smtpHost))
      throw new Exception(notSendingMsg + "smtpHost wasn't specified.");
    if (smtpPort < 0 || smtpPort == Integer.MAX_VALUE)
      throw new Exception(notSendingMsg + "smtpPort=" + smtpPort + " is invalid.");
    if (!String2.isSomething(userName))
      throw new Exception(notSendingMsg + "userName wasn't specified.");
    if (!String2.isSomething(password))
      throw new Exception(notSendingMsg + "password wasn't specified.");

    // make properties
    // 2022-08-18 https://docs.cloudmailin.com/outbound/examples/send_email_with_java/
    Properties props = new Properties();
    boolean useStartTLS = false;
    if (properties != null && properties.trim().length() > 0) {
      String sar[] = String2.split(properties, '|');
      int n = (sar.length / 2) * 2;
      for (int i = 0; i < n; i += 2) {
        props.setProperty(sar[i], sar[i + 1]);

        // props.setProperty("mail.smtp.starttls.enable", "true"); //ERDDAP recommends doing this as
        // a property in setup.xml when using Gmail
        if (sar[i].equals("mail.smtp.starttls.enable") && sar[i + 1].equals("true"))
          useStartTLS = true;
      }
    }
    props.setProperty("mail.smtp.host", smtpHost);
    props.setProperty("mail.smtp.port", "" + smtpPort);

    props.setProperty("mail.smtp.auth", "true");

    // make the session
    Session session =
        Session.getInstance(
            props,
            new jakarta.mail.Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
              }
            });
    if (debugMode) session.setDebug(true);

    // make the SMTPTransport
    // 2022-08-25 Now basically follow
    // https://www.tabnine.com/code/java/classes/com.sun.mail.smtp.SMTPTransport
    //  but use "smtp" when useStartTLS=true, not "smtps" which uses SSL (outdated predecessor to
    // startTLS). https://mailtrap.io/blog/starttls-ssl-tls/
    // I have only tested with useStartTLS=true.
    SMTPTransport smtpTransport =
        (SMTPTransport) session.getTransport(useStartTLS ? "smtp" : "smtps");
    if (debugMode) String2.log(">> pre connect");
    smtpTransport.connect(smtpHost, userName, password);
    if (debugMode) String2.log(">> post connect");

    return new Object[] {session, smtpTransport};
  }

  /**
   * This handles the low level part of sending one email.
   *
   * @throws Exception if trouble
   */
  public static void lowSendEmail(
      Session session,
      SMTPTransport smtpTransport,
      String fromAddress,
      String toAddresses,
      String subject,
      String content)
      throws Exception {

    String notSendingMsg = String2.ERROR + " in SSR.lowSendEmail: not sending email because ";
    if (!String2.isSomething(fromAddress))
      throw new Exception(notSendingMsg + "fromAddress wasn't specified.");
    if (!String2.isSomething(toAddresses))
      throw new Exception(notSendingMsg + "toAddresses wasn't specified.");

    // Gather the 'to' addresses
    String tAddresses[] =
        StringArray.arrayFromCSV(toAddresses, ",", true, false); // trim, keepNothing
    int nAddresses = tAddresses.length;
    if (nAddresses == 0) return;
    InternetAddress internetAddresses[] = new InternetAddress[nAddresses];
    for (int add = 0; add < nAddresses; add++)
      internetAddresses[add] = new InternetAddress(tAddresses[add]);

    // Construct the message
    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(fromAddress));
    msg.setRecipients(Message.RecipientType.TO, internetAddresses);
    msg.setSubject(subject, File2.UTF_8);
    msg.setContent(
        "<pre>" + XML.encodeAsHTML(content) + "</pre>",
        "text/html"); // thus content is 7-bit ASCII, which avoids need for extra steps to support
    // utf-8.
    msg.setHeader("X-Mailer", "msgsend"); // program that sent the email
    msg.setSentDate(new Date());
    msg.saveChanges(); // do last.  don't forget this

    smtpTransport.sendMessage(
        msg, internetAddresses); // assumes session is already connected.  Old: send(...) did
    // connect(),sendMessage(),close().
  }

  /**
   * This procedure sends a plain text email. For example,
   *
   * <pre>sendEmail("mail.server.name", 25, "joe.smith", password, "joe@smith.com", "sue@smith.com",
   *           "re: dinner", "How about at 7?");
   * </pre>
   *
   * This is thread safe.
   *
   * <p>This code uses /libs/mail.jar. The mail.jar files are available from Sun (see
   * https://www.oracle.com/technetwork/java/javamail/index.html). The software is copyrighted by
   * Sun, but Sun grants anyone the right to freely redistribute the binary .jar files. The source
   * code is also available from Sun.
   *
   * <p>See the javamail examples notably [c:/programs/]javamail-1.4.3/demo/msgsend.java . Some
   * programming information is from Java Almanac:
   * http://javaalmanac.com/egs/javax.mail/SendApp.html and
   * http://www.websina.com/bugzero/kb/sunmail-properties.html <br>
   * See also http://kickjava.com/2872.htm <br>
   * 2021-02-18
   * https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/javamail/javamail.html
   * (Glassfish-oriented)
   * https://docs.oracle.com/cd/E26576_01/doc.312/e24930/javamail.htm#GSDVG00204 <br>
   * 2022-08-18 https://docs.cloudmailin.com/outbound/examples/send_email_with_java/ <br>
   * 2022-08-27 maintain session
   * https://www.tabnine.com/code/java/classes/com.sun.mail.smtp.SMTPTransport
   *
   * <p>If this fails with "Connection refused" error, make sure McAffee "Virus Scan Console :
   * Access Protection Properties : Anti Virus Standard Protections : Prevent mass mailing worms
   * from sending mail" is un-checked.
   *
   * <p>THIS CODE IS DUPLICATED IN SSR.sendEmail and EmailThread. So if change one, then change the
   * other.
   *
   * @param smtpHost the name of the outgoing mail server
   * @param smtpPort port to be used, usually 25 or 587
   * @param userName for the mail server
   * @param password for the mail server (may be null or "" if only sending to local email account)
   * @param properties additional properties to be used by Java mail stored as pairs of Strings
   *     within a String (separated by |'s) e.g., "mail.smtp.starttls.enable|true|param2|value2".
   *     Use null or "" is there are none. See
   *     http://javamail.kenai.com/nonav/javadocs/com/sun/mail/smtp/package-summary.html
   * @param fromAddress the email address the email is coming from (usually the same as the
   *     userName)
   * @param toAddresses a comma-separated list of the email addresses the email is going to. (One
   *     email with 1+ addressees.) If all or one is null or "" or "null", it's a silent error.
   * @param subject The subject is sent with UTF-8 encoding, so any Unicode characters are
   *     (theoretically) ok.
   * @param content This plain text content is sent with as 7-bit-encoded HTML &lt;pre&gt; content,
   *     so any Unicode characters are ok.
   * @throws Exception if trouble
   */
  public static void sendEmail(
      String smtpHost,
      int smtpPort,
      String userName,
      String password,
      String properties,
      String fromAddress,
      String toAddresses,
      String subject,
      String content)
      throws Exception {

    if (!emailLock.tryLock(10, TimeUnit.SECONDS)) {
      String2.log(
          "ERROR: SSR.sendEmail failed to get emailLock to send email to="
              + toAddresses
              + " subject="
              + subject);
      return;
    }
    SMTPTransport smtpTransport = null;
    try {
      if (debugMode)
        String2.log(
            "SSR.sendEmail host="
                + smtpHost
                + " port="
                + smtpPort
                + "\n"
                + "  properties="
                + properties
                + "\n"
                + "  userName="
                + userName
                + " password="
                + (password.length() > 0 ? "[present]" : "[absent]")
                + "\n"
                + "  toAddresses="
                + toAddresses);

      // get a session and smtpTransport
      Object oar[] = openEmailSession(smtpHost, smtpPort, userName, password, properties);
      Session session = (Session) oar[0];
      smtpTransport = (SMTPTransport) oar[1];

      // sendEmail
      lowSendEmail(session, smtpTransport, fromAddress, toAddresses, subject, content);

    } catch (Exception e) {
      String2.log(
          MustBe.throwableWithMessage(
              "SSR.sendEmail", "to=" + toAddresses + " subject=" + subject, e));
    } finally {
      try {
        if (smtpTransport != null) smtpTransport.close();
      } catch (Throwable t) {
      }
      emailLock.unlock();
    }
  }

  /**
   * This mini/pseudo percent encodes query characters for examples that work in a browser. This
   * won't always work (e.g., it won't encode &amp; within parameter value).
   *
   * @param url the not yet percentEncoded url or query or parameter value
   * @return the encoded query string. If query is null, this returns "".
   * @throws Exception if trouble
   */
  public static String pseudoPercentEncode(String url) throws Exception {
    if (url == null) return "";
    StringBuilder sb = new StringBuilder();
    int tLength = url.length();
    for (int po = 0; po < tLength; po++) {
      char ch = url.charAt(po);
      if (ch < 32) sb.append("%0" + Integer.toHexString(ch).toUpperCase());
      else if (ch >= 127 || "[]<>|\"".indexOf(ch) >= 0) sb.append(percentEncode("" + ch));
      else sb.append(ch);
    }
    return sb.toString();
  }

  /**
   * This encodes all characters except A-Za-z0-9_-!.~()*. (2022-11-22 ' now percent encoded)
   * Originally, this did a more minimal encoding. Now it does proper encoding.
   *
   * @param nameOrValue not yet percentEncoded
   * @return the encoded query string. If query is null, this returns "".
   * @throws Exception if trouble
   */
  public static String minimalPercentEncode(String nameOrValue) throws Exception {
    if (nameOrValue == null) return "";
    StringBuilder sb = new StringBuilder();
    int nvLength = nameOrValue.length();
    for (int po = 0; po < nvLength; po++) {
      char ch = nameOrValue.charAt(po);
      // see https://en.wikipedia.org/wiki/Percent-encoding#Percent-encoding_unreserved_characters
      //  "URI producers are discouraged from percent-encoding unreserved characters."
      //   A-Za-z0-9_-.~   (unreserved characters)   different from java:
      // See javadocs for URI. It says
      //  encode everything but A-Za-z0-9_-!.~()*   (unreserved characters)
      //  and for details see appendix A of https://www.ietf.org/rfc/rfc2396.txt
      //    It says agree with unreserved character list in URI javadocs
      //    (unreserved = alphanum | mark)
      // JavaScript docs support that interpretation
      //
      // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent
      if (Character.isLetterOrDigit(ch) || "_-!.~()*".indexOf(ch) >= 0) sb.append(ch);
      else if (ch < 16) sb.append("%0" + Integer.toHexString(ch).toUpperCase());
      else if (ch < 127) sb.append("%" + Integer.toHexString(ch).toUpperCase());
      else sb.append(percentEncode("" + ch)); // this handles any unicode char via utf-8
    }
    return sb.toString();
  }

  /**
   * This is used by Erddap.sendRedirect to try to fix urls that are supposedly already
   * percentEncoded, but have characters that are now (with stricter Tomcat) not allowed, notably
   * [,],|,&gt;,&lt;
   *
   * @param url the whole URL (which you wouldn't do with minimalPercentEncode)
   * @return url with additional characters encoded
   */
  public static String fixPercentEncodedUrl(String url) throws Exception {
    if (url == null) return "";
    StringBuilder sb = new StringBuilder();
    int tLength = url.length();
    for (int po = 0; po < tLength; po++) {
      char ch = url.charAt(po);
      if ("|<>;@$,#[]".indexOf(ch) >= 0) sb.append("%" + Integer.toHexString(ch).toUpperCase());
      else if (ch < 127) sb.append(ch);
      else sb.append(percentEncode("" + ch)); // this handles any unicode char via utf-8
    }
    return sb.toString();
  }

  /**
   * This encodes an Http GET query by converting special characters to %HH (where H is a
   * hexadecimal digit) and by converting ' ' to '%20' (not '+' since I consider that ambiguous when
   * decoding). <br>
   * This is used on names and values just prior to contacting a url. <br>
   * See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   * Note that reserved characters only need to be percent encoded in special circumstances (not
   * always).
   *
   * @param query not yet percentEncoded
   * @return the encoded query string. If query is null, this returns "".
   * @throws Exception if trouble
   */
  public static String percentEncode(String query) throws Exception {
    if (query == null) return "";
    return String2.replaceAll(URLEncoder.encode(query, File2.UTF_8), "+", "%20");
  }

  /**
   * This decodes an Http GET query by converting all %HH (where H is a hexadecimal digit) to the
   * corresponding character and by converting '+' to ' '. This is used by a server program before
   * parsing the incoming query.
   *
   * @param query already percentEncoded
   * @return the decoded query string. If query is null, this returns "".
   * @throws Exception if trouble
   */
  public static String percentDecode(String query) throws Exception {
    if (query == null) return "";
    return URLDecoder.decode(query, File2.UTF_8);
  }

  /**
   * This just connects to (pings) the urlString but doesn't read from the input stream (usually for
   * cases where it is a big security risk). Note that some urls return an endless stream of random
   * digits, so reading a malicious inputStream would be trouble. Besides, for this purpuse, the
   * caller doesn't care what the response is.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @param timeOutMillis
   * @param handleS3ViaSDK If true (usually), this method handles AWS S3 URLs specially, via the
   *     Java S3 SDK. If false, this handles them like any other URL (which will only work for
   *     public buckets).
   * @throws Exception if trouble
   */
  public static void touchUrl(String urlString, int timeOutMillis, boolean handleS3ViaSDK)
      throws Exception {
    // tests show that getting the inputStream IS necessary (but reading it is not)
    Object[] oar =
        getUrlConnBufferedInputStream(
            urlString,
            timeOutMillis,
            false,
            false,
            0,
            -1,
            handleS3ViaSDK); // requestCompression, touchMode
    InputStream in = (InputStream) oar[1];
    // it doesn't seem necessary to read even 1 byte (if available)
    in.close();
  }

  /**
   * This gets an uncompressed inputStream from a url. [I.e., This doesn't try for compression, so
   * the inputStream won't be compressed, so this doesn't even try to decompress it.] This has a 2
   * minute timeout to initiate the connection and 10 minute read timeout.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return an InputStream from the url
   * @throws Exception if trouble
   */
  public static BufferedInputStream getUncompressedUrlBufferedInputStream(String urlString)
      throws Exception {
    return (BufferedInputStream)
        getUrlConnBufferedInputStream(urlString, 120000, false)[1]; // 2 minute timeout
  }

  /**
   * This builds an S3TransferManager
   *
   * @param region The S3 region from bro[1].
   */
  public static S3TransferManager buildS3TransferManager(String region) {
    return S3TransferManager.builder()
        .s3Client(
            S3AsyncClient.crtBuilder()
                .region(Region.of(region))
                // .credentialsProvider(credentialProvider)  //handled by default credentials
                // provider
                .targetThroughputInGbps(20.0) // ??? make a separate setting?
                .minimumPartSizeInBytes(Long.valueOf(8 * Math2.BytesPerMB))
                .build())
        .build();
  }

  /**
   * This uploads a file to AWS S3. If the file already exists, it is just touched.
   *
   * @param tm The S3TransferManager. If null, one will be temporarily created.
   * @param localFileName The full name of the local file
   * @param awsUrl Must be in the format used by String2.parseAwsS3Url.
   * @param contentType may be null
   * @throws IOException if touble
   */
  public static void uploadFileToAwsS3(
      S3TransferManager tm, String localFileName, String awsUrl, String contentType)
      throws IOException {

    String bro[] = String2.parseAwsS3Url(awsUrl); // bucket, region, object key
    if (bro == null)
      throw new IOException(
          String2.ERROR + " in uploadFileToAwsS3: incorrect format for awsUrl=" + awsUrl);

    // sample code and javadoc:
    // https://sdk.amazonaws.com/java/api/latest/index.html?software/amazon/awssdk/transfer/s3/S3TransferManager.html
    try {
      long time = System.currentTimeMillis();

      // if the file already exists, touch it and we're done
      if (File2.length(awsUrl) >= 0) {
        // ...touch(awsUrl);
        if (verbose) String2.log("uploadFileToAwsS3 file=" + awsUrl + " already exists.");
        return;
      }

      if (tm == null) tm = buildS3TransferManager(bro[1]);
      PutObjectRequest.Builder request =
          PutObjectRequest.builder()
              .bucket(bro[0])
              .key(bro[2])
              .contentLength(File2.length(localFileName));
      if (contentType != null) request.contentType(contentType);
      FileUpload upload =
          tm.uploadFile(u -> u.source(Paths.get(localFileName)).putObjectRequest(request.build()));
      upload.completionFuture().join(); // wait for completion. exception if trouble

      if (verbose)
        String2.log(
            "uploadFileToAwsS3 successfully uploaded (in "
                + (System.currentTimeMillis() - time)
                + " ms)\n"
                + "  from: "
                + localFileName
                + "\n"
                + "  to:   "
                + awsUrl);
    } catch (Exception e) {
      // File2.delete(url);
      throw new IOException(
          String2.ERROR + " in uploadFileToAwsS3 while uploading to " + awsUrl, e);
    }
  }

  /** A variant that sets attributeTo to "downloadFile". */
  public static void downloadFile(
      String urlString, String fullFileName, boolean tryToUseCompression) throws Exception {
    downloadFile("downloadFile", urlString, fullFileName, tryToUseCompression);
  }

  /**
   * This downloads a file as bytes from a Url and saves it as a temporary file, then renames it to
   * the final name if successful. If there is a failure, this deletes the parially written file. If
   * the remote file is compressed (e.g., gzip or zipped), it will stay compressed. Note that you
   * may get a error-404-file-not-found error message stored in the file.
   *
   * <p>CHARSET! This writes the bytes as-is, regardless of charset of source URL.
   *
   * @param attributeTo is used for diagnostic messages. If null, this uses "downloadFile".
   * @param urlString urlString (or file name) pointing to the information. The query MUST be
   *     already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always). <br>
   *     This can be a url or a local file (with or without file://). <br>
   *     If this is an AWS S3 URL, this will use TransferManager to do a parallel transfer of
   *     chunks.
   * @param fullFileName the full name for the file to be created. If the directory doesn't already
   *     exist, it will be created.
   * @param tryToUseCompression If true, the request indicates in-transit http compression is
   *     acceptable and the input stream will do the decompression. 'false' is safer if the file may
   *     be already compressed (e.g., .gz or .zip) because this won't try to unzip the file.
   * @throws Exception if trouble (and the temporary file will have been deleted, (but the original
   *     fullFileName, if any, will still exist).
   */
  public static void downloadFile(
      String attributeTo, String urlString, String fullFileName, boolean tryToUseCompression)
      throws Exception {

    // first, ensure destination dir exists
    File2.makeDirectory(File2.getDirectory(fullFileName));

    // if 'url' is really just a local file, use File2.copy() to copy it into place
    if (attributeTo == null) attributeTo = "downloadFile";
    if (!String2.isUrl(urlString)) {
      if (!File2.copy(urlString, fullFileName))
        throw new IOException(
            String2.ERROR
                + ": "
                + attributeTo
                + " unable to copy "
                + urlString
                + " to "
                + fullFileName);
      return;
    }

    // is it an AWS S3 URL?
    long time = System.currentTimeMillis();
    int random = Math2.random(Integer.MAX_VALUE);
    String bro[] = String2.parseAwsS3Url(urlString); // bucket, region, object key
    if (bro != null) {
      // sample code and javadoc:
      // https://sdk.amazonaws.com/java/api/latest/index.html?software/amazon/awssdk/transfer/s3/S3TransferManager.html
      try {
        S3TransferManager tm = buildS3TransferManager(bro[1]); // !!! ??? reuse these???
        FileDownload download =
            tm.downloadFile(
                d ->
                    d.getObjectRequest(g -> g.bucket(bro[0]).key(bro[2]))
                        .destination(Paths.get(fullFileName + random)));
        download.completionFuture().join(); // exception if trouble
        File2.rename(fullFileName + random, fullFileName); // exception if trouble

        if (verbose)
          String2.log(
              attributeTo
                  + " (via AWS Transfer Manager) successfully downloaded (in "
                  + (System.currentTimeMillis() - time)
                  + " ms)\n"
                  + "  from: "
                  + urlString
                  + "\n"
                  + "  to:   "
                  + fullFileName);
        return;
      } catch (Exception e) {
        File2.delete(fullFileName + random);
        throw new IOException(
            String2.ERROR
                + " in "
                + attributeTo
                + " (AWS Transfer Manager) while downloading from "
                + urlString,
            e);
      }
    }

    // download from regular URL
    InputStream in = null;
    OutputStream out = null;
    try {
      in =
          tryToUseCompression
              ? getUrlBufferedInputStream(urlString)
              : getUncompressedUrlBufferedInputStream(urlString);
      try {
        out = new BufferedOutputStream(new FileOutputStream(fullFileName + random));
        try {
          byte buffer[] = new byte[8192]; // best if smaller than java buffered..stream sizes
          int nBytes;
          while ((nBytes = in.read(buffer)) > 0) out.write(buffer, 0, nBytes);
        } finally {
          out.close();
          out = null;
        }
      } finally {
        in.close();
        in = null;
      }
      File2.rename(fullFileName + random, fullFileName); // exception if trouble
      if (verbose)
        String2.log(
            attributeTo
                + " successfully DOWNLOADED (in "
                + (System.currentTimeMillis() - time)
                + " ms)"
                + "\n  from "
                + urlString
                + "\n  to "
                + fullFileName);

    } catch (Exception e) {
      try {
        if (in != null) in.close();
      } catch (Exception e2) {
      }
      try {
        if (out != null) out.close();
      } catch (Exception e2) {
      }
      File2.delete(fullFileName + random);
      String2.log(
          String2.ERROR
              + " in "
              + attributeTo
              + " while downloading from "
              + urlString
              + " to "
              + fullFileName);
      throw new IOException(
          String2.ERROR + " in " + attributeTo + " while downloading from " + urlString, e);
    }
  }

  /**
   * This gets the inputStream from a url. This solicits and accepts gzip and deflate compressed
   * responses (not compress or x-compress as they seem harder to work with because of 'Entries').
   * And touchMode is set to false. For info on compressed responses, see:
   * http://www.websiteoptimization.com/speed/tweak/compress/ and the related test site:
   * http://www.webperformance.org/compression/ . This variant assumes requestCompression=true,
   * touchMode=false, firstByte=0, lastByte=-1, and handleS3ViaSDK=true.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @param connectTimeOutMillis the time out for opening a connection in milliseconds (use -1 to
   *     get high default, currently 10 minutes)
   * @return Object[3], [0]=UrlConnection, [1]=a (decompressed if necessary) BufferedInputStream,
   *     [2]=charset (will be valid)
   * @throws Exception if trouble
   */
  public static Object[] getUrlConnBufferedInputStream(String urlString, int connectTimeOutMillis)
      throws Exception {
    return getUrlConnBufferedInputStream(urlString, connectTimeOutMillis, true, false, 0, -1, true);
  }

  /**
   * This is a variant of getUrlConnInputStream where touchMode=false. This variant assumes
   * touchMode=false, firstByte=0, lastByte=-1, and handleS3ViaSDK=true.
   */
  public static Object[] getUrlConnBufferedInputStream(
      String urlString, int connectTimeOutMillis, boolean requestCompression) throws Exception {
    return getUrlConnBufferedInputStream(
        urlString, connectTimeOutMillis, requestCompression, false, 0, -1, true);
  }

  /** This variant assumes firstByte=0, lastByte=-1, and handleS3ViaSDK=true. */
  public static Object[] getUrlConnBufferedInputStream(
      String urlString, int connectTimeOutMillis, boolean requestCompression, boolean touchMode)
      throws Exception {
    return getUrlConnBufferedInputStream(
        urlString, connectTimeOutMillis, requestCompression, touchMode, 0, -1, true);
  }

  /**
   * This is the low level version of getUrlConnInputStream. It has the most options.
   *
   * @param urlString this may be an AWS S3 url or a regular url.
   * @param requestCompression If true, this requests in-transit http compression. This is ignored
   *     for AWS S3 URLs or if this is a byte range request (which might work -- I just didn't test
   *     it, so this plays it safe).
   * @param touchMode If true, this method doesn't pursue http to https redirects and doesn't log
   *     the info from the errorStream.
   * @param firstByte usually 0, but &gt;=0 for a byte range request
   * @param lastByte usually -1 (last available), but a specific number (inclusive) for a byte range
   *     request.
   * @param handleS3ViaSDK If true (usually), this method handles AWS S3 URLs specially, via the
   *     Java S3 SDK. If false, this handles them like any other URL (which will only work for
   *     public buckets).
   * @return [connection, inputStream, charset]. If url is an AWS S3 url, connection will be null
   *     and charset will always be File2.UTF_8. If requestCompression=true, this will be a
   *     decompressed inputStream
   */
  public static Object[] getUrlConnBufferedInputStream(
      String urlString,
      int connectTimeOutMillis,
      boolean requestCompression,
      boolean touchMode,
      long firstByte,
      long lastByte,
      boolean handleS3ViaSDK)
      throws Exception {
    if (requestCompression
        && urlString.indexOf('?') < 0
        && // no parameters
        File2.isCompressedExtension(File2.getExtension(urlString))) requestCompression = false;
    if (reallyVerbose)
      String2.log(
          "getUrlConnInputStream "
              + urlString
              + "\n  requestCompression="
              + requestCompression
              + " first="
              + firstByte
              + " last="
              + lastByte
              + " handleS3ViaSDK="
              + handleS3ViaSDK);

    // is it an AWS S3 object?
    if (handleS3ViaSDK) {
      if (reallyVerbose) String2.log("  handle via S3 SDK");
      String bro[] = String2.parseAwsS3Url(urlString); // [bucket, region, objectKey]
      if (bro != null) {
        // not File2.getDecompressedBufferedInputStream(). Read as is.
        InputStream is = File2.getBufferedInputStream(urlString, firstByte, lastByte);
        return new Object[] {
          null, is, File2.UTF_8
        }; // connection, is, charset=UTF_8 is an assumption
      }
    }

    URL turl = new URL(urlString);
    URLConnection conn = turl.openConnection();
    if (firstByte > 0 || lastByte != -1)
      // this will cause failure if server doesn't allow byte range requests
      conn.setRequestProperty(
          "Range", "bytes=" + firstByte + "-" + (lastByte == -1 ? "" : "" + lastByte));
    else if (requestCompression)
      conn.setRequestProperty("Accept-Encoding", "gzip, deflate"); // compress, x-compress, x-gzip
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 ERDDAP/" + erddapVersion);
    // String2.log("request: " + String2.toString(conn.getRequestProperties()));
    if (connectTimeOutMillis <= 0)
      connectTimeOutMillis = 10 * Calendar2.SECONDS_PER_MINUTE * 1000; // ten minutes, in ms
    conn.setConnectTimeout(connectTimeOutMillis);
    // I think setReadTimeout is any period of inactivity.
    conn.setReadTimeout(10 * Calendar2.SECONDS_PER_MINUTE * 1000); // ten minutes, in ms
    conn.connect();

    // The automatic redirect handling won't handle http to https.
    // So if error is 301, 302, 303 and there is a "location" header field: redirect
    if (!touchMode && conn instanceof HttpURLConnection httpUrlConn) {
      int code = httpUrlConn.getResponseCode();
      if (code != 200)
        String2.log(
            (reallyVerbose
                    ? ""
                    : // info was shown above, else show now ...
                    "getUrlConnInputStream "
                        + urlString
                        + " requestCompression="
                        + requestCompression
                        + "\n")
                + "  Warning: HTTP status code="
                + code
                + (code == 206 ? " (Partial Content: a response to a byte-range request)" : ""));
      if (code >= 301
          && code <= 308
          && code != 304) { // HTTP_MOVED_TEMP HTTP_MOVED_PERM HTTP_SEE_OTHER  (304 Not Modified)
        String location = conn.getHeaderField("location");
        if (String2.isSomething(location)) {
          String2.log("  redirect to " + location);
          turl = new URL(location);
          conn = turl.openConnection();
          if (firstByte > 0 || lastByte != -1)
            conn.setRequestProperty(
                "Range", "bytes=" + firstByte + "-" + (lastByte == -1 ? "" : "" + lastByte));
          else if (requestCompression)
            conn.setRequestProperty(
                "Accept-Encoding", "gzip, deflate"); // compress, x-compress, x-gzip
          conn.setRequestProperty("User-Agent", "Mozilla/5.0 ERDDAP/" + erddapVersion);
          // String2.log("request: " + String2.toString(conn.getRequestProperties()));
          conn.setConnectTimeout(connectTimeOutMillis);
          // I think setReadTimeout is any period of inactivity.
          conn.setReadTimeout(10 * Calendar2.SECONDS_PER_MINUTE * 1000); // ten minutes, in ms
          conn.connect();
          if (conn instanceof HttpURLConnection) {
            httpUrlConn = (HttpURLConnection) conn;
            code = httpUrlConn.getResponseCode();
            if (code != 200) String2.log("  Warning: after redirect, HTTP status code=" + code);
          }
        }
        // Do little here. Java throws good Exception eg, when later try getInputStream
      }
    }

    BufferedInputStream is =
        getBufferedInputStream(
            urlString, conn); // This is in SSR, not File2. This deals with compressed content.
    String charset = getCharset(urlString, conn);

    // String2.log(">>charset=" + charset);
    return new Object[] {conn, is, charset};
  }

  /**
   * Given an awsS3FileUrl, this tests if the file is private.
   *
   * @param awsS3FileUrl
   * @return true if the file is private
   */
  public static boolean awsS3FileIsPrivate(String awsS3FileUrl) {
    boolean isPrivate = false;
    try {
      // try to touchUrl as plain https (should fail for private file)
      SSR.touchUrl(awsS3FileUrl, 15000, isPrivate);
    } catch (Exception e) {
      isPrivate = true;
    }
    return isPrivate;
  }

  /**
   * This returns the BufferedInputStream from the connection, with a content decoder if needed.
   *
   * @param urlString for diagnostics only
   * @param con
   * @return the BufferedInputStream from the connection, with a content decoder if needed.
   */
  public static BufferedInputStream getBufferedInputStream(String urlString, URLConnection con)
      throws Exception {
    String encoding = con.getContentEncoding();
    try {
      BufferedInputStream is =
          new BufferedInputStream(con.getInputStream()); // this is in SSR, not File2
      // String2.log("url = " + urlString + "\n" +  //diagnostic
      //  "  headerFields=" + String2.toString(conn.getHeaderFields()));
      //    "encoding=" + encoding + "\n" +
      //    "BeginContent");
      if (encoding != null) {
        encoding = encoding.toLowerCase();
        // if (encoding.indexOf("compress") >= 0) //hard to work with later
        //    is = new ZipInputStream(is);
        // else
        if (encoding.indexOf("gzip") >= 0) is = new BufferedInputStream(new GZIPInputStream(is));
        else if (encoding.indexOf("deflate") >= 0)
          is = new BufferedInputStream(new InflaterInputStream(is));
      }

      return is;
    } catch (Exception e) {
      if (con instanceof HttpURLConnection httpUrlCon) {
        // try to read errorStream and append to e.
        int code = httpUrlCon.getResponseCode();
        if (code != 200) {
          String msg = null;
          try {
            // try to read the errorStream
            InputStream es = httpUrlCon.getErrorStream(); // will fail if no error content
            if (es != null) {
              if (encoding != null) {
                encoding = encoding.toLowerCase();
                // if (encoding.indexOf("compress") >= 0) //hard to work with later
                //    is = new ZipInputStream(is);
                // else
                if (encoding.indexOf("gzip") >= 0) es = new GZIPInputStream(es);
                else if (encoding.indexOf("deflate") >= 0) es = new InflaterInputStream(es);
              }
              String charset = getCharset(urlString, httpUrlCon);
              msg =
                  readerToString(
                      urlString, // may throw exception
                      new BufferedReader(new InputStreamReader(es, charset)),
                      1000); // maxCharacters
            }
          } catch (Throwable t) {
            String2.log(
                "Caught error while trying to read errorStream (encoding="
                    + encoding
                    + "):\n"
                    + MustBe.throwableToString(t));
          }
          String eString = e.toString();
          if (!String2.isSomething(eString)) eString = "";
          String lookFor =
              "java.io.IOException: Server returned HTTP response code: \\d\\d\\d for .*"; // "for
          // [url]"
          if (eString.matches(lookFor)) eString = eString.substring(61); // leaving "for [url]"
          throw new IOException(
              "HTTP status code="
                  + code
                  + " "
                  + eString
                  + (String2.isSomething(msg) ? "\n(" + msg.trim() + ")" : ""));
        }
      }
      throw e; // just rethrow e
    }
  }

  /**
   * This returns the charset of the response (and assumes 8859-1 if none specified).
   *
   * @param urlString is for diagnostic messages only
   */
  public static String getCharset(String urlString, URLConnection conn) {
    // typical: Content-Type: text/html; charset=utf-8
    // default charset="ISO-8859-1"
    // see https://www.w3.org/International/articles/http-charset/index
    String charset = File2.ISO_8859_1;
    String cType = conn.getContentType();
    if (String2.isSomething(cType)) {
      // isolate charset name
      cType = cType.toUpperCase(); // java names are all upper case
      int po = cType.indexOf("CHARSET=");
      if (po >= 0) {
        cType = cType.substring(po + 8);
        // is it one of the standard java charsets? (see Charset class)
        String csar[] = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16"};
        String tcType = String2.findPrefix(csar, cType, 0);
        if (tcType != null) { // successful match
          charset = tcType;
        } else {
          // is cType supported by Java?
          try {
            Charset cset = Charset.forName(cType);
            charset = cType; // no exception means it's valid
          } catch (Exception e) {
            // charset remains default
            String2.log(
                String2.ERROR
                    + ": Using ISO-8859-1 since charset="
                    + cType
                    + " for "
                    + urlString
                    + " isn't supported by Java.");
          }
        }
      }
    }
    return charset;
  }

  /**
   * For compatibility with older code. It uses a timeout of 120 seconds. This tries to use
   * compression. IN MOST CASES, it is better to use getUrlReader, since it deals with charset
   * correctly.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   */
  public static BufferedInputStream getUrlBufferedInputStream(String urlString) throws Exception {
    return (BufferedInputStream) getUrlConnBufferedInputStream(urlString, 120000)[1];
  }

  /**
   * If you need a reader, this is better than starting with getUrlInputStream since it properly
   * deals with charset. This tries to use compression.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     If urlString is an AWS S3 url, this ASSUMES charset is UTF-8. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return a buffered Url Reader. BufferedReaders can readLine().
   */
  public static BufferedReader getBufferedUrlReader(String urlString) throws Exception {
    Object[] o3 = getUrlConnBufferedInputStream(urlString, 120000);
    return new BufferedReader(new InputStreamReader((InputStream) o3[1], (String) o3[2]));
  }

  /**
   * This gets the response from a url. This is useful for short responses. This tries to use
   * compression.
   *
   * @param urlString The query MUST be already percentEncoded as needed. This can be a url or a
   *     local file (with or without file://, using UTF_8 -- reasonable assumption for URLs). <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return an ArrayList with strings from the response (one string per line of the file) (on the
   *     assumption that basically all the lines of the file are different).
   * @throws Exception if error occurs
   */
  public static ArrayList<String> getUrlResponseArrayList(String urlString) throws Exception {
    try {
      if (!String2.isUrl(urlString)) return File2.readLinesFromFile(urlString, File2.UTF_8, 1);

      long time = System.currentTimeMillis();
      BufferedReader in = getBufferedUrlReader(urlString);
      try {
        ArrayList<String> sa = new ArrayList();
        String s;
        while ((s = in.readLine()) != null) {
          sa.add(s);
        }
        if (reallyVerbose)
          String2.log(
              "  SSR.getUrlResponseArrayList "
                  + urlString
                  + " finished. TIME="
                  + (System.currentTimeMillis() - time)
                  + "ms");
        return sa;
      } finally {
        in.close();
      }
    } catch (Exception e) {
      String msg = e.toString();
      if (String2.isSomething(msg) && msg.indexOf(urlString) >= 0) throw e;
      throw new IOException(String2.ERROR + " from url=" + urlString + " : " + e.toString(), e);
    }
  }

  /**
   * This gets the response from a url. This is useful for short responses. This tries to use
   * compression.
   *
   * @param urlString The query MUST be already percentEncoded as needed. This can be a url or a
   *     local file (with or without file://). <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return a String[] with the response (one string per line of the file).
   * @throws Exception if error occurs
   */
  public static String[] getUrlResponseLines(String urlString) throws Exception {
    return getUrlResponseArrayList(urlString).toArray(new String[0]);
  }

  /**
   * This gets the response from a url as one newline-separated (no cr's) String. This is useful for
   * short responses. This tries to use compression.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return a String with the response.
   * @throws Exception if error occurs
   */
  public static String getUrlResponseStringNewline(String urlString) throws Exception {
    if (String2.isUrl(urlString)) {
      try {
        long time = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder(4096);
        BufferedReader in = getBufferedUrlReader(urlString);
        try {
          String s;
          while ((s = in.readLine()) != null) {
            sb.append(s);
            sb.append('\n');
          }
        } finally {
          in.close();
        }
        if (reallyVerbose)
          String2.log(
              "  SSR.getUrlResponseStringNewline "
                  + urlString
                  + " finished. TIME="
                  + (System.currentTimeMillis() - time)
                  + "ms");
        return sb.toString();
      } catch (Exception e) {
        String msg = e.toString();
        if (String2.isSomething(msg) && msg.indexOf(urlString) >= 0) throw e;
        throw new IOException(String2.ERROR + " from url=" + urlString + " : " + e.toString(), e);
      }
    }

    // if file -- Trouble! it assumes 8859-1 encoding.  Who uses this?
    String sar[] = File2.readFromFile(urlString, File2.ISO_8859_1, 2); // uses newline
    if (sar[0].length() > 0) throw new IOException(sar[0]);
    return sar[1];
  }

  /**
   * This gets the response from a url as a String with unchanged, native line endings. This is
   * useful for short responses. This tries to use compression.
   *
   * <p>TODO: Consider adding support for handing redirects (300 responses).
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return a String with the response.
   * @throws Exception if error occurs
   */
  public static String getUrlResponseStringUnchanged(String urlString) throws Exception {
    if (String2.isUrl(urlString)) return readerToString(urlString, getBufferedUrlReader(urlString));

    // if file -- Trouble! it assumes 8859-1 encoding. Who uses this?
    return File2.directReadFrom88591File(urlString); // throws exception
  }

  /** This variant of readerToString gets all of the content. */
  public static String readerToString(String urlString, Reader in) throws Exception {
    return readerToString(urlString, in, Integer.MAX_VALUE);
  }

  /**
   * This returns the info from the reader, unchanged.
   *
   * @param urlString for diagnostic messages only
   * @param in Usually a bufferedReader. This method always closes the reader.
   * @throws Exception if trouble
   */
  public static String readerToString(String urlString, Reader in, int maxChars) throws Exception {
    try {
      long time = System.currentTimeMillis();
      char buffer[] = new char[8192];
      StringBuilder sb = new StringBuilder(8192);
      try {
        int got;
        while ((got = in.read(buffer)) >= 0) { // -1 if end-of-stream
          sb.append(buffer, 0, got);
          if (sb.length() >= maxChars) {
            sb.setLength(maxChars);
            sb.append("...");
            break;
          }
        }
      } finally {
        in.close();
      }
      if (reallyVerbose)
        String2.log(
            "  SSR.readerToString "
                + urlString
                + " finished. TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
      return sb.toString();
    } catch (Exception e) {
      String msg = e.toString();
      if (String2.isSomething(msg) && msg.indexOf(urlString) >= 0) throw e;
      throw new IOException(String2.ERROR + " from url=" + urlString + " : " + e.toString(), e);
    }
  }

  /**
   * This gets the bytes from a url. This tries to use compression.
   *
   * @param urlString The query MUST be already percentEncoded as needed. <br>
   *     See https://en.wikipedia.org/wiki/Percent-encoding . <br>
   *     Note that reserved characters only need to be percent encoded in special circumstances (not
   *     always).
   * @return a byte[] with the response.
   * @throws Exception if error occurs
   */
  public static byte[] getUrlResponseBytes(String urlString) throws Exception {
    try {
      // String2.log(">> SSR.getUrlResponseBytes(" + urlString + ")");
      long time = System.currentTimeMillis();
      byte buffer[] = new byte[8096];
      BufferedInputStream is = getUrlBufferedInputStream(urlString);
      try {
        ByteArray ba = new ByteArray();
        int gotN;
        while ((gotN = is.read(buffer)) > 0) { // -1 = end of stream, but should block so gotN > 0
          // String2.log(">> gotN=" + gotN);
          ba.add(buffer, 0, gotN);
        }
        if (reallyVerbose)
          String2.log(
              "  SSR.getUrlResponseBytes "
                  + urlString
                  + " finished. nBytes="
                  + ba.size()
                  + " TIME="
                  + (System.currentTimeMillis() - time)
                  + "ms");
        return ba.toArray();
      } finally {
        is.close();
      }
    } catch (Exception e) {
      String msg = e.toString();
      if (String2.isSomething(msg) && msg.indexOf(urlString) >= 0) throw e;
      throw new Exception(String2.ERROR + " from url=" + urlString + " : " + e.toString(), e);
    }
  }

  /**
   * This gets the bytes from a file.
   *
   * @param fileName
   * @return a byte[] with the response.
   * @throws Exception if error occurs
   */
  public static String getFileString(String fileName) throws Exception {
    return new String(getFileBytes(fileName));
  }

  /**
   * This gets the bytes from a file.
   *
   * @param fileName If compressed file, this reads the decompressed, first file in the archive.
   * @return a byte[] with the response.
   * @throws Exception if error occurs
   */
  public static byte[] getFileBytes(String fileName) throws Exception {
    InputStream is = null;
    try {
      long time = System.currentTimeMillis();
      byte buffer[] = new byte[1024];
      is = File2.getDecompressedBufferedInputStream(fileName);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (int s; (s = is.read(buffer)) != -1; ) baos.write(buffer, 0, s);
      is.close();
      is = null;
      if (reallyVerbose)
        String2.log(
            "  SSR.getFileBytes "
                + fileName
                + " finished. TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
      return baos.toByteArray();
    } catch (Exception e) {
      // String2.log(e.toString());
      if (is != null)
        try {
          is.close();
        } catch (Throwable t) {
        }
      ;
      throw new Exception("ERROR while reading file=" + fileName + " : " + e.toString(), e);
    }
  }

  public static void genericTunnelTest(int nTimes, String baseUrl, String varName)
      throws Exception {
    // currently, GAssta hday on otter has time dimension size is 1392
    // currently, GAssta hday on oceanwatch has time dimension size is 2877
    int nTimePoints = 1000;
    System.out.println("\nSSR.genericTunnelTest(" + nTimes + ", " + baseUrl + ")");
    long elapsedTime = System.currentTimeMillis();
    java.util.Random random = new java.util.Random();

    // run the test
    for (int time = 0; time < nTimes; time++) {
      // String2.log("iteration #" + time);
      int tIndex = random.nextInt(nTimePoints);
      int xIndex = random.nextInt(52);
      int yIndex = random.nextInt(52);
      String ts =
          getUrlResponseStringUnchanged(
              baseUrl + "?" + varName + "[" + tIndex + ":1:" + tIndex + "]" + "[0:1:0]" + "["
                  + yIndex + ":1:" + yIndex + "]" + "[" + xIndex + ":1:" + xIndex + "]");
      // if (time == 0) System.out.println(ts);
    }
    System.out.println(
        "SSR.threddsTunnelTest done.  TIME=" + (System.currentTimeMillis() - elapsedTime) + "ms\n");
  }

  /**
   * This returns a directory for temporary files (with forward slashes and a trailing slash,
   * currently: <contextDirectory>WEB-INF/temp/). This only works if these classes are installed
   * underneath Tomcat (or a path with "WEB-INF/", the start of things to be removed from
   * classPath).
   *
   * @return the tempDirectory
   * @throws Exception if trouble
   */
  public static String getTempDirectory() {
    if (tempDirectory == null) {
      String tdir = EDStatic.getWebInfParentDirectory() + "WEB-INF/temp/";
      // make it, because Git doesn't track empty dirs
      File2.makeDirectory(tdir);
      // then set it if successful
      tempDirectory = tdir;
    }

    return tempDirectory;
  }

  /**
   * This is a one time method to change the names of chl2 files in chla .zip's to chla. This
   * unzips, renames, re-zips the files.
   *
   * @param zipDir the dir with the chla .zip files
   * @param emptyDir needs to be an empty temporary directory
   */
  public static void changeChl2ToChla(String zipDir, String emptyDir) {
    String2.log("SSR.changeChl2ToChla zipDir=" + zipDir + " emptyDir=" + emptyDir);

    // get the names of all the chla files in zipDir
    String names[] = RegexFilenameFilter.fullNameList(zipDir, ".+chla.+\\.zip");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // unzip to temp dir
        unzip(names[i], emptyDir, true, 10, null);

        // if internal file was already chla, delete internal file and continue
        String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
        Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
        if (tNames[0].indexOf("chla") >= 0) {
          File2.delete(emptyDir + tNames[0]);
          continue;
        }
        String2.log("changing " + tNames[0]);

        // rename internal file
        Test.ensureTrue(tNames[0].indexOf("chl2") >= 0, "tNames[0] not chl2 file!");
        String newName = String2.replaceAll(tNames[0], "chl2", "chla");
        File2.rename(emptyDir, tNames[0], newName);

        // delete old zip file
        File2.delete(names[i]);

        // make new zip file
        zip(
            names[i],
            new String[] {emptyDir + newName},
            10,
            false,
            ""); // false = don't include dir names

        // delete internal file
        File2.delete(newName);

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }

      // empty the directory
      String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
      for (int j = 0; j < tNames2.length; j++) File2.delete(emptyDir + tNames2[j]);
    }
    String2.log(
        "successfully changed " + countRenamed + " out of " + names.length + " chla .zip files.");
  }

  /**
   * This is a one time method to change the names of GH files in GA .zip's to GA. This unzips,
   * renames, re-zips the files into their correct original location.
   *
   * @param zipDir the dir with the chla .zip files
   * @param emptyDir needs to be an empty temporary directory
   */
  public static void changeGHToGA(String zipDir, String emptyDir) {
    String2.log("SSR.changeGHToGA zipDir=" + zipDir + " emptyDir=" + emptyDir);

    // get the names of all the GA files in zipDir
    String names[] = RegexFilenameFilter.fullNameList(zipDir, "GA.+\\.zip");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // unzip to temp dir
        unzip(names[i], emptyDir, true, 10, null);

        // if internal file was already GA, delete internal file and continue
        String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
        Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
        if (tNames[0].startsWith("GA")) {
          File2.delete(emptyDir + tNames[0]);
          continue;
        }
        String2.log("changing " + tNames[0]);

        // rename internal file
        Test.ensureTrue(tNames[0].startsWith("GH"), "tNames[0] not GH file!");
        String newName = "GA" + tNames[0].substring(2);
        File2.rename(emptyDir, tNames[0], newName);

        // delete old zip file
        File2.delete(names[i]);

        // make new zip file
        zip(
            names[i],
            new String[] {emptyDir + newName},
            10,
            false,
            ""); // false = don't include dir names

        // delete internal file
        File2.delete(newName);

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }

      // empty the directory
      String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
      for (int j = 0; j < tNames2.length; j++) File2.delete(emptyDir + tNames2[j]);
    }
    String2.log(
        "successfully changed " + countRenamed + " out of " + names.length + " GA .zip files.");
  }

  /**
   * This is a one time method to change the names of GA0 files in GA .zip's to GA20. This unzips,
   * renames, re-zips the files into their correct original location.
   *
   * @param zipDir the dir with the chla .zip files
   * @param emptyDir needs to be an empty temporary directory
   */
  public static void changeGA0ToGA20(String zipDir, String emptyDir) {
    String2.log("SSR.changeGA0ToGA20 zipDir=" + zipDir + " emptyDir=" + emptyDir);

    // get the names of all the GA files in zipDir
    String names[] = RegexFilenameFilter.fullNameList(zipDir, "GA.+\\.zip");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // unzip to temp dir
        unzip(names[i], emptyDir, true, 10, null);

        // if internal file was already GA, delete internal file and continue
        String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
        Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
        if (tNames[0].startsWith("GA20")) {
          File2.delete(emptyDir + tNames[0]);
          continue;
        }
        String2.log("changing " + tNames[0]);

        // rename internal file
        Test.ensureTrue(tNames[0].startsWith("GA0"), "tNames[0] not GA0 file!");
        String newName = "GA2" + tNames[0].substring(2);
        File2.rename(emptyDir, tNames[0], newName);

        // delete old zip file
        File2.delete(names[i]);

        // make new zip file
        zip(
            names[i],
            new String[] {emptyDir + newName},
            10,
            false,
            ""); // false = don't include dir names

        // delete internal file
        File2.delete(newName);

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }

      // empty the directory
      String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
      for (int j = 0; j < tNames2.length; j++) File2.delete(emptyDir + tNames2[j]);
    }
    String2.log(
        "successfully changed " + countRenamed + " out of " + names.length + " GA0 .zip files.");
  }

  /**
   * This is a one time method to enclose each of the files in a directory in its own zip file.
   *
   * @param dir
   */
  public static void zipEach(String dir) {
    String2.log("SSR.zipEach dir=" + dir);

    // get the names of all the files dir
    String names[] = RegexFilenameFilter.fullNameList(dir, ".+");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // make new zip file
        zip(
            names[i] + ".zip",
            new String[] {names[i]},
            10,
            false,
            ""); // false = don't include dir names

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }
    }
    String2.log("successfully zipped " + countRenamed + " out of " + names.length + " .zip files.");
  }

  /**
   * This POSTs the content to the url, and returns the response.
   *
   * @param urlString where the content will be sent (with no parameters)
   * @param contentType charset MUST be UTF-8. E.g., "application/x-www-form-urlencoded;
   *     charset=UTF-8" or "text/xml; charset=UTF-8"
   * @param content the content to be sent, e.g., key1=value1&key2=value2 (where keys and values are
   *     percent encoded). This method does conversion to UTF-8 bytes.
   * @return Object[3], [0]=UrlConnection, [1]=a (decompressed if necessary) InputStream,
   *     [2]=charset (will be valid)
   * @throws Exception if trouble
   */
  public static Object[] getPostInputStream(String urlString, String contentType, String content)
      throws Exception {
    // modified from https://stackoverflow.com/questions/3324717/sending-http-post-request-in-java

    // create the connection where we're going to send the file
    URL url = new URL(urlString);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    // set the appropriate HTTP parameters
    // con.setRequestProperty("Content-Length", "" + content.length()); //not required, and I'm
    // confused about pre/post encoding length
    con.setRequestProperty("Content-Type", contentType);
    boolean requestCompression = true;
    if (requestCompression
        && urlString.indexOf('?') < 0
        && // no parameters
        File2.isCompressedExtension(File2.getExtension(urlString))) requestCompression = false;
    if (requestCompression)
      con.setRequestProperty(
          "Accept-Encoding",
          "gzip, deflate"); // no compress, x-compress, since zip Entries are hard to deal with
    con.setRequestMethod("POST");
    con.setDoOutput(true);
    con.setDoInput(true);

    // send the content
    Writer writer = File2.getBufferedWriterUtf8(new BufferedOutputStream(con.getOutputStream()));
    try {
      writer.write(content);
      writer.flush();
    } finally {
      writer.close();
    }

    BufferedInputStream is = getBufferedInputStream(urlString, con); // this is in SSR, not File2
    String charset = getCharset(urlString, con);
    return new Object[] {con, is, charset};
  }

  /**
   * Submits the urlString via POST and returns the response. This assumes the response is text, not
   * a binary file.
   *
   * @param urlString a GET-like urlString with '?' and params. This method breaks it at '?' and
   *     submits via POST.
   */
  public static String postFormGetResponseString(String urlString) throws Exception {
    int po = urlString.indexOf('?');
    Object ob3[] =
        getPostInputStream(
            po < 0 ? urlString : urlString.substring(0, po),
            "application/x-www-form-urlencoded; charset=UTF-8",
            po < 0 ? "" : urlString.substring(po + 1));
    BufferedReader bufReader =
        new BufferedReader(new InputStreamReader((InputStream) ob3[1], (String) ob3[2]));
    return readerToString(urlString, bufReader);
  }

  /**
   * Copy from source to outputStream.
   *
   * @param source May be local fileName or a URL (including an AWS S3 URL).
   * @param out Best if buffered. At the end, out is flushed, but not closed
   * @param firstByte The first byte to be transferred (0..).
   * @param lastByte The last byte to be transferred, inclusive. Use -1 to transfer to the end of
   *     the file.
   * @return true if successful
   */
  public static boolean copy(
      String source, OutputStream out, long firstByte, long lastByte, boolean handleS3ViaSDK) {
    if (source.startsWith("http://")
        || source.startsWith("https://")
        || source.startsWith("ftp://")) { // untested. presumably anonymous
      // URL
      BufferedInputStream in = null;
      try {
        in =
            (BufferedInputStream)
                getUrlConnBufferedInputStream(
                    source, // throws Exception   //handles AWS S3
                    120000,
                    true,
                    false,
                    firstByte,
                    lastByte,
                    handleS3ViaSDK)[1]; // timeOutMillis, requestCompression, touchMode, ...

        // adjust firstByte,lastByte
        long newLastByte = lastByte - (firstByte > 0 && lastByte >= 0 ? firstByte : 0);
        return File2.copy(in, out, 0, newLastByte); // the adjusted range
      } catch (Exception e) {
        String2.log(
            String2.ERROR + " in SSR.copy(source=" + source + ")\n" + MustBe.throwableToString(e));
        return false;
      } finally {
        try {
          if (in != null) in.close();
        } catch (Exception e2) {
        }
      }

    } else {
      // presumably a file
      return File2.copy(source, out, firstByte, lastByte);
    }
  }

  // public static void main(String args[]) {
  // usage
  // cd /usr/local/jakarta-tomcat-5.5.4/webapps/cwexperimental/WEB-INF/
  // java -cp
  // ./classes:./lib/mail.jar:./lib/netcdf-latest.jar:./lib/slf4j-jdk14.jar:./lib/nlog4j-1.2.21.jar
  // gov.noaa.pfel.coastwatch.util.SSR /u00/data/GA/hday/grd/
  // the new files replace the old files
  // changeGA0ToGA20(args[0], "/u00/cwatch/bobtemp/");
  // }
}
