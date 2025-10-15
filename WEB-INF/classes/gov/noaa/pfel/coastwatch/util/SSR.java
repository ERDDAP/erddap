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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.utils.builder.SdkBuilder;

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
  public static final ReentrantLock emailLock = new ReentrantLock();

  static {
    HttpURLConnection.setFollowRedirects(true); // it's a static method!
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
   * This is a variant of shell() for cShell command lines.
   *
   * @param commandLine the command line to be executed (for example, "myprogram <filename>")
   * @param timeOutSeconds (use 0 for no timeout)
   * @return an ArrayList of Strings with the output from the program (or null if there is a fatal
   *     error)
   * @throws Exception if exitStatus of cmd is not 0 (or other fatal error)
   * @see #shell
   */
  public static List<String> cShell(String commandLine, int timeOutSeconds) throws Exception {
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
    // not File2.getDecompressedBufferedInputStream(). Read file as is.
    try (ZipInputStream in = new ZipInputStream(File2.getBufferedInputStream(fullZipName))) {
      // create a buffer for reading the files
      byte[] buf = new byte[4096];

      // unzip the files
      ZipEntry entry = in.getNextEntry();
      while (entry != null) {

        // isDirectory?
        String name = entry.getName();
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
          try (OutputStream out = new BufferedOutputStream(new FileOutputStream(baseDir + name))) {

            // transfer bytes from the .zip file to the output file
            // in.read reads from current zipEntry
            byte[] buffer = new byte[8192]; // best if smaller than java buffered...stream size
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, buf.length)) > 0) {
              out.write(buffer, 0, bytesRead);
            }
          }
          // close the output file
          if (resultingFullFileNames != null) resultingFullFileNames.add(baseDir + name);
        }

        // close this entry
        in.closeEntry();

        // get the next entry
        entry = in.getNextEntry();
      }
    }
    // close the input file
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
    try (ZipOutputStream out =
        new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipDirName)))) {

      // create a buffer for reading the files
      byte[] buf = new byte[4096];

      // compress the files
      for (String dirName : dirNames) {
        // if directory, get all file names
        ArrayList<String> al = new ArrayList<>();
        if (File2.isDirectory(dirName)) {
          RegexFilenameFilter.recursiveFullNameList(al, dirName, ".*", false); // directoriesToo
        } else {
          al.add(dirName);
        }

        for (String s : al) {
          // not File2.getDecompressedBufferedInputStream(). Read files as is.
          try (InputStream in = File2.getBufferedInputStream(s)) {

            // add ZIP entry to output stream
            String tName =
                includeDirectoryInfo
                    ? s.substring(removeDirPrefix.length())
                    : // already validated above
                    File2.getNameAndExtension(s);
            out.putNextEntry(new ZipEntry(tName));

            // transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
              out.write(buf, 0, len);
            }

            // complete the entry
            out.closeEntry();
          }
        }
      }
    }
    // close the ZIP file
    if (verbose) String2.log("  zip done. TIME=" + (System.currentTimeMillis() - tTime) + "ms\n");
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
  @SuppressWarnings("JavaUtilDate") // Date is needed for MimeMessage
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
    return String2.replaceAll(URLEncoder.encode(query, StandardCharsets.UTF_8), "+", "%20");
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
    return URLDecoder.decode(query, StandardCharsets.UTF_8);
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
    SdkBuilder<?, S3AsyncClient> builder;
    AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();
    if (EDStatic.config.useAwsAnonymous) {
      credentialsProvider = AnonymousCredentialsProvider.create();
    }
    if (EDStatic.config.useAwsCrt) {
      builder =
          S3AsyncClient.crtBuilder()
              .credentialsProvider(credentialsProvider)
              .region(Region.of(region))
              .targetThroughputInGbps(20.0) // ??? make a separate setting?
              .minimumPartSizeInBytes((long) (8 * Math2.BytesPerMB));
    } else {
      builder =
          S3AsyncClient.builder()
              .credentialsProvider(credentialsProvider)
              .region(Region.of(region));
    }
    return S3TransferManager.builder().s3Client(builder.build()).build();
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
      try (S3TransferManager tm = buildS3TransferManager(bro[1]); ) {
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

    URL turl = URI.create(urlString).toURL();
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
          turl = URI.create(location).toURL();
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
  public static List<String> getUrlResponseArrayList(String urlString) throws Exception {
    try {
      if (!String2.isUrl(urlString)) return File2.readLinesFromFile(urlString, File2.UTF_8, 1);

      long time = System.currentTimeMillis();
      try (BufferedReader in = getBufferedUrlReader(urlString)) {
        ArrayList<String> sa = new ArrayList<>();
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
      }
    } catch (Exception e) {
      String msg = e.toString();
      if (String2.isSomething(msg) && msg.indexOf(urlString) >= 0) throw e;
      throw new IOException(String2.ERROR + " from url=" + urlString + " : " + e, e);
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
        try (BufferedReader in = getBufferedUrlReader(urlString)) {
          String s;
          while ((s = in.readLine()) != null) {
            sb.append(s);
            sb.append('\n');
          }
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
        throw new IOException(String2.ERROR + " from url=" + urlString + " : " + e, e);
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
      try (in) {
        int got;
        while ((got = in.read(buffer)) >= 0) { // -1 if end-of-stream
          sb.append(buffer, 0, got);
          if (sb.length() >= maxChars) {
            sb.setLength(maxChars);
            sb.append("...");
            break;
          }
        }
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
      throw new IOException(String2.ERROR + " from url=" + urlString + " : " + e, e);
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
      try (BufferedInputStream is = getUrlBufferedInputStream(urlString)) {
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
      }
    } catch (Exception e) {
      String msg = e.toString();
      if (String2.isSomething(msg) && msg.indexOf(urlString) >= 0) throw e;
      throw new Exception(String2.ERROR + " from url=" + urlString + " : " + e, e);
    }
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
      String tdir = File2.getWebInfParentDirectory() + "WEB-INF/temp/";
      // make it, because Git doesn't track empty dirs
      File2.makeDirectory(tdir);
      // then set it if successful
      tempDirectory = tdir;
    }

    return tempDirectory;
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
    URL url = URI.create(urlString).toURL();
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
    try (Writer writer =
        File2.getBufferedWriterUtf8(new BufferedOutputStream(con.getOutputStream()))) {
      writer.write(content);
      writer.flush();
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
        || source.startsWith("ftp://")
        || source.startsWith("s3://")) { // untested. presumably anonymous
      // URL
      try (BufferedInputStream in =
          (BufferedInputStream)
              getUrlConnBufferedInputStream(
                  source, // throws Exception   //handles AWS S3
                  120000,
                  true,
                  false,
                  firstByte,
                  lastByte,
                  handleS3ViaSDK)[1]) {
        // throws Exception   //handles AWS S3
        // timeOutMillis, requestCompression, touchMode, ...

        // adjust firstByte,lastByte
        long newLastByte = lastByte - (firstByte > 0 && lastByte >= 0 ? firstByte : 0);
        return File2.copy(in, out, 0, newLastByte); // the adjusted range
      } catch (Exception e) {
        String2.log(
            String2.ERROR + " in SSR.copy(source=" + source + ")\n" + MustBe.throwableToString(e));
        return false;
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
